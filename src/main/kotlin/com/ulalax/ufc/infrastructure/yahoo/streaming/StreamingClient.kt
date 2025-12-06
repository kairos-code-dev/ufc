package com.ulalax.ufc.infrastructure.yahoo.streaming

import com.ulalax.ufc.domain.exception.ErrorCode
import com.ulalax.ufc.domain.exception.UfcException
import com.ulalax.ufc.domain.model.streaming.MarketHours
import com.ulalax.ufc.domain.model.streaming.QuoteType
import com.ulalax.ufc.domain.model.streaming.StreamingClientConfig
import com.ulalax.ufc.domain.model.streaming.StreamingEvent
import com.ulalax.ufc.domain.model.streaming.StreamingPrice
import com.ulalax.ufc.domain.model.streaming.StreamingQuote
import com.ulalax.ufc.infrastructure.yahoo.streaming.internal.PricingData
import com.ulalax.ufc.infrastructure.yahoo.streaming.internal.StreamingMessage
import com.ulalax.ufc.infrastructure.yahoo.streaming.internal.SubscribeMessage
import com.ulalax.ufc.infrastructure.yahoo.streaming.internal.UnsubscribeMessage
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readReason
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import org.slf4j.LoggerFactory
import java.util.Base64
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Yahoo Finance WebSocket Streaming 클라이언트.
 *
 * WebSocket을 통해 실시간 주식 시세 데이터를 수신합니다.
 * 자동 재연결, 하트비트, 구독 관리 기능을 제공합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val client = StreamingClient.create()
 * client.connect()
 *
 * // 데이터 수신
 * client.prices.collect { price ->
 *     println("${price.symbol}: ${price.price}")
 * }
 *
 * // 구독
 * client.subscribe("AAPL")
 * client.subscribe(listOf("GOOGL", "MSFT"))
 *
 * // 정리
 * client.close()
 * ```
 *
 * @property config 클라이언트 설정
 */
class StreamingClient private constructor(
    private val config: StreamingClientConfig,
) : AutoCloseable {
    private val logger = LoggerFactory.getLogger(StreamingClient::class.java)

    private val httpClient =
        HttpClient(CIO) {
            install(WebSockets)
        }

    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    private val protoBuf =
        ProtoBuf {
            encodeDefaults = false
        }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 연결 상태
    private var session: DefaultWebSocketSession? = null
    private val isConnected = AtomicBoolean(false)
    private val isClosed = AtomicBoolean(false)

    // 구독 목록
    private val subscribedSymbols = mutableSetOf<String>()

    // 재연결 관련
    private val reconnectAttempt = AtomicInteger(0)
    private var heartbeatJob: Job? = null
    private var receiveJob: Job? = null

    // Flow 이벤트
    private val _prices =
        MutableSharedFlow<StreamingPrice>(
            replay = 0,
            extraBufferCapacity = config.eventBufferSize,
        )
    private val _quotes =
        MutableSharedFlow<StreamingQuote>(
            replay = 0,
            extraBufferCapacity = config.eventBufferSize,
        )
    private val _events =
        MutableSharedFlow<StreamingEvent>(
            replay = 0,
            extraBufferCapacity = config.eventBufferSize,
        )

    /**
     * 실시간 가격 데이터 스트림.
     */
    val prices: SharedFlow<StreamingPrice> = _prices.asSharedFlow()

    /**
     * 실시간 상세 시세 데이터 스트림.
     */
    val quotes: SharedFlow<StreamingQuote> = _quotes.asSharedFlow()

    /**
     * 연결 상태 이벤트 스트림.
     */
    val events: SharedFlow<StreamingEvent> = _events.asSharedFlow()

    /**
     * 특정 심볼의 가격 데이터만 필터링한 Flow.
     *
     * @param symbol 필터링할 심볼
     * @return 해당 심볼의 가격 데이터 Flow
     */
    fun pricesBySymbol(symbol: String): Flow<StreamingPrice> =
        prices.filter { it.symbol.equals(symbol, ignoreCase = true) }

    /**
     * 특정 심볼의 상세 시세 데이터만 필터링한 Flow.
     *
     * @param symbol 필터링할 심볼
     * @return 해당 심볼의 상세 시세 데이터 Flow
     */
    fun quotesBySymbol(symbol: String): Flow<StreamingQuote> =
        quotes.filter { it.symbol.equals(symbol, ignoreCase = true) }

    /**
     * WebSocket 연결을 수립합니다.
     *
     * 이미 연결되어 있다면 아무 동작도 하지 않습니다.
     *
     * @throws UfcException 연결 실패 시
     */
    suspend fun connect() {
        if (isClosed.get()) {
            throw UfcException(ErrorCode.WEBSOCKET_CONNECTION_FAILED, "Client is closed")
        }

        if (isConnected.get()) {
            if (config.enableLogging) {
                logger.debug("Already connected")
            }
            return
        }

        try {
            if (config.enableLogging) {
                logger.info("Connecting to WebSocket: ${config.webSocketUrl}")
            }

            val newSession =
                withTimeout(config.connectTimeoutMs) {
                    httpClient.webSocketSession(config.webSocketUrl)
                }

            session = newSession
            isConnected.set(true)
            reconnectAttempt.set(0)

            // 이벤트 발송
            _events.emit(StreamingEvent.Connected)

            // 수신 작업 시작
            startReceiving()

            // 하트비트 시작
            startHeartbeat()

            // 기존 구독 복원
            if (subscribedSymbols.isNotEmpty()) {
                sendSubscribe(subscribedSymbols.toList())
            }

            if (config.enableLogging) {
                logger.info("Connected successfully")
            }
        } catch (e: TimeoutCancellationException) {
            throw UfcException(
                ErrorCode.WEBSOCKET_CONNECTION_FAILED,
                "Connection timeout after ${config.connectTimeoutMs}ms",
                e,
            )
        } catch (e: Exception) {
            throw UfcException(
                ErrorCode.WEBSOCKET_CONNECTION_FAILED,
                "Failed to connect: ${e.message}",
                e,
            )
        }
    }

    /**
     * WebSocket 연결을 종료합니다.
     *
     * 재연결하지 않습니다.
     */
    suspend fun disconnect() {
        if (!isConnected.get()) {
            return
        }

        try {
            if (config.enableLogging) {
                logger.info("Disconnecting...")
            }

            stopHeartbeat()
            receiveJob?.cancel()

            session?.close(CloseReason(CloseReason.Codes.NORMAL, "User requested disconnect"))
            session = null
            isConnected.set(false)

            _events.emit(StreamingEvent.Disconnected("User requested"))

            if (config.enableLogging) {
                logger.info("Disconnected")
            }
        } catch (e: Exception) {
            if (config.enableLogging) {
                logger.warn("Error during disconnect: ${e.message}")
            }
        }
    }

    /**
     * 연결 상태를 확인합니다.
     *
     * @return 연결되어 있으면 true, 아니면 false
     */
    fun isConnected(): Boolean = isConnected.get()

    /**
     * 심볼을 구독합니다.
     *
     * 연결되어 있지 않으면 자동으로 연결합니다.
     *
     * @param symbol 구독할 심볼
     */
    suspend fun subscribe(symbol: String) {
        subscribe(listOf(symbol))
    }

    /**
     * 여러 심볼을 구독합니다.
     *
     * 연결되어 있지 않으면 자동으로 연결합니다.
     *
     * @param symbols 구독할 심볼 목록
     */
    suspend fun subscribe(symbols: List<String>) {
        require(symbols.isNotEmpty()) { "symbols must not be empty" }

        val newSymbols = symbols.map { it.uppercase() }
        subscribedSymbols.addAll(newSymbols)

        // 자동 연결
        if (!isConnected.get()) {
            connect()
        }

        sendSubscribe(newSymbols)

        _events.emit(StreamingEvent.SubscriptionUpdated(subscribedSymbols.toSet()))
    }

    /**
     * 심볼 구독을 해제합니다.
     *
     * @param symbol 구독 해제할 심볼
     */
    suspend fun unsubscribe(symbol: String) {
        unsubscribe(listOf(symbol))
    }

    /**
     * 여러 심볼의 구독을 해제합니다.
     *
     * @param symbols 구독 해제할 심볼 목록
     */
    suspend fun unsubscribe(symbols: List<String>) {
        require(symbols.isNotEmpty()) { "symbols must not be empty" }

        val upperSymbols = symbols.map { it.uppercase() }
        subscribedSymbols.removeAll(upperSymbols.toSet())

        if (isConnected.get()) {
            sendUnsubscribe(upperSymbols)
        }

        _events.emit(StreamingEvent.SubscriptionUpdated(subscribedSymbols.toSet()))
    }

    /**
     * 모든 구독을 해제합니다.
     */
    suspend fun unsubscribeAll() {
        if (subscribedSymbols.isEmpty()) {
            return
        }

        val allSymbols = subscribedSymbols.toList()
        subscribedSymbols.clear()

        if (isConnected.get()) {
            sendUnsubscribe(allSymbols)
        }

        _events.emit(StreamingEvent.SubscriptionUpdated(emptySet()))
    }

    /**
     * 현재 구독 중인 심볼 목록을 반환합니다.
     *
     * @return 구독 중인 심볼 Set
     */
    fun getSubscribedSymbols(): Set<String> = subscribedSymbols.toSet()

    /**
     * 리소스를 정리하고 연결을 종료합니다.
     */
    override fun close() {
        if (isClosed.getAndSet(true)) {
            return
        }

        runBlocking {
            disconnect()
        }

        scope.cancel()
        httpClient.close()

        if (config.enableLogging) {
            logger.info("StreamingClient closed")
        }
    }

    // ===== Private Methods =====

    private fun startReceiving() {
        receiveJob =
            scope.launch {
                try {
                    session?.incoming?.consumeAsFlow()?.collect { frame ->
                        when (frame) {
                            is Frame.Text -> {
                                handleTextFrame(frame.readText())
                            }
                            is Frame.Close -> {
                                handleClose(frame.readReason())
                            }
                            else -> {
                                // Ignore other frame types
                            }
                        }
                    }
                } catch (e: ClosedReceiveChannelException) {
                    if (config.enableLogging) {
                        logger.debug("WebSocket channel closed")
                    }
                    handleDisconnection("Channel closed")
                } catch (e: CancellationException) {
                    // Job cancelled, do nothing
                } catch (e: Exception) {
                    if (config.enableLogging) {
                        logger.error("Error receiving WebSocket frames", e)
                    }
                    handleDisconnection("Receive error: ${e.message}")
                }
            }
    }

    private suspend fun handleTextFrame(text: String) {
        try {
            val message = json.decodeFromString<StreamingMessage>(text)
            val binaryData = Base64.getDecoder().decode(message.message)
            val pricingData = protoBuf.decodeFromByteArray(PricingData.serializer(), binaryData)

            // Domain 모델로 변환 및 방출
            convertAndEmit(pricingData)
        } catch (e: Exception) {
            if (config.enableLogging) {
                logger.warn("Failed to parse message: ${e.message}")
            }

            // 파싱 실패는 일시적 에러로 처리 (연결 유지)
            _events.emit(
                StreamingEvent.Error(
                    UfcException(ErrorCode.PROTOBUF_DECODING_ERROR, "Failed to parse message", e),
                    isFatal = false,
                ),
            )
        }
    }

    private suspend fun handleClose(reason: CloseReason?) {
        if (config.enableLogging) {
            logger.info("WebSocket closed: ${reason?.message}")
        }

        isConnected.set(false)
        _events.emit(StreamingEvent.Disconnected(reason?.message))

        // 재연결 시도
        if (!isClosed.get() && config.reconnection.enabled) {
            attemptReconnect()
        }
    }

    private suspend fun handleDisconnection(reason: String) {
        if (!isConnected.get()) {
            return
        }

        isConnected.set(false)
        stopHeartbeat()

        _events.emit(StreamingEvent.Disconnected(reason))

        // 재연결 시도
        if (!isClosed.get() && config.reconnection.enabled) {
            attemptReconnect()
        }
    }

    private suspend fun attemptReconnect() {
        val attempt = reconnectAttempt.incrementAndGet()

        if (attempt > config.reconnection.maxAttempts) {
            if (config.enableLogging) {
                logger.error("Max reconnection attempts exceeded")
            }

            _events.emit(
                StreamingEvent.Error(
                    UfcException(ErrorCode.STREAMING_RECONNECTION_FAILED, "Max attempts exceeded"),
                    isFatal = true,
                ),
            )
            return
        }

        _events.emit(StreamingEvent.Reconnecting(attempt))

        val delay = config.reconnection.calculateDelay(attempt - 1)

        if (config.enableLogging) {
            logger.info("Reconnecting in ${delay}ms (attempt $attempt/${config.reconnection.maxAttempts})")
        }

        delay(delay)

        try {
            connect()
        } catch (e: Exception) {
            if (config.enableLogging) {
                logger.warn("Reconnection attempt $attempt failed: ${e.message}")
            }

            // 다음 재시도
            attemptReconnect()
        }
    }

    private fun startHeartbeat() {
        heartbeatJob =
            scope.launch {
                while (isActive && isConnected.get()) {
                    delay(config.heartbeatIntervalMs)

                    try {
                        // 구독 메시지 재전송 (하트비트)
                        if (subscribedSymbols.isNotEmpty()) {
                            sendSubscribe(subscribedSymbols.toList())
                        }
                    } catch (e: Exception) {
                        if (config.enableLogging) {
                            logger.warn("Heartbeat failed: ${e.message}")
                        }
                    }
                }
            }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private suspend fun sendSubscribe(symbols: List<String>) {
        val message = SubscribeMessage(symbols)
        val jsonText = json.encodeToString(SubscribeMessage.serializer(), message)

        session?.send(Frame.Text(jsonText))

        if (config.enableLogging) {
            logger.debug("Sent subscribe: $symbols")
        }
    }

    private suspend fun sendUnsubscribe(symbols: List<String>) {
        val message = UnsubscribeMessage(symbols)
        val jsonText = json.encodeToString(UnsubscribeMessage.serializer(), message)

        session?.send(Frame.Text(jsonText))

        if (config.enableLogging) {
            logger.debug("Sent unsubscribe: $symbols")
        }
    }

    private suspend fun convertAndEmit(data: PricingData) {
        if (data.id.isBlank()) {
            return
        }

        // StreamingPrice 생성 및 방출
        val price =
            StreamingPrice(
                symbol = data.id,
                price = data.price.toDouble(),
                change = data.change.toDouble(),
                changePercent = data.changePercent.toDouble(),
                timestamp = data.time,
                volume = data.dayVolume,
                bid = if (data.bid > 0f) data.bid.toDouble() else null,
                ask = if (data.ask > 0f) data.ask.toDouble() else null,
                marketHours = MarketHours.fromCode(data.marketHours),
            )
        _prices.emit(price)

        // StreamingQuote 생성 및 방출 (필수 필드가 있을 때만)
        if (data.shortName.isNotBlank() && data.currency.isNotBlank() && data.exchange.isNotBlank()) {
            val quote =
                StreamingQuote(
                    symbol = data.id,
                    price = data.price.toDouble(),
                    change = data.change.toDouble(),
                    changePercent = data.changePercent.toDouble(),
                    timestamp = data.time,
                    volume = data.dayVolume,
                    bid = if (data.bid > 0f) data.bid.toDouble() else null,
                    ask = if (data.ask > 0f) data.ask.toDouble() else null,
                    marketHours = MarketHours.fromCode(data.marketHours),
                    dayHigh = data.dayHigh.toDouble(),
                    dayLow = data.dayLow.toDouble(),
                    openPrice = data.openPrice.toDouble(),
                    previousClose = data.previousClose.toDouble(),
                    bidSize = if (data.bidSize > 0L) data.bidSize else null,
                    askSize = if (data.askSize > 0L) data.askSize else null,
                    currency = data.currency,
                    exchange = data.exchange,
                    shortName = data.shortName,
                    quoteType = QuoteType.fromCode(data.quoteType),
                )
            _quotes.emit(quote)
        }
    }

    companion object {
        /**
         * StreamingClient 인스턴스를 생성합니다.
         *
         * @param config 클라이언트 설정 (기본값 사용 가능)
         * @return StreamingClient 인스턴스
         */
        fun create(config: StreamingClientConfig = StreamingClientConfig()): StreamingClient = StreamingClient(config)
    }
}
