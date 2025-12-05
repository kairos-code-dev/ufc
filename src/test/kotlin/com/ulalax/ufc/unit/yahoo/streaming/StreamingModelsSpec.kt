package com.ulalax.ufc.unit.yahoo.streaming

import com.ulalax.ufc.domain.model.streaming.MarketHours
import com.ulalax.ufc.domain.model.streaming.QuoteType
import com.ulalax.ufc.domain.model.streaming.StreamingPrice
import com.ulalax.ufc.domain.model.streaming.StreamingQuote
import com.ulalax.ufc.infrastructure.yahoo.streaming.ReconnectionConfig
import com.ulalax.ufc.infrastructure.yahoo.streaming.StreamingClientConfig
import com.ulalax.ufc.infrastructure.yahoo.streaming.internal.PricingData
import com.ulalax.ufc.unit.utils.UnitTestBase
import kotlinx.serialization.protobuf.ProtoBuf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Base64

@DisplayName("Streaming Models - Unit Test")
class StreamingModelsSpec : UnitTestBase() {

    private val protoBuf = ProtoBuf {
        encodeDefaults = false
    }

    @Test
    @DisplayName("MarketHours enum은 코드로부터 올바르게 변환된다")
    fun `MarketHours fromCode converts correctly`() = unitTest {
        // Then
        assertThat(MarketHours.fromCode(0)).isEqualTo(MarketHours.CLOSED)
        assertThat(MarketHours.fromCode(1)).isEqualTo(MarketHours.REGULAR)
        assertThat(MarketHours.fromCode(2)).isEqualTo(MarketHours.PRE_MARKET)
        assertThat(MarketHours.fromCode(3)).isEqualTo(MarketHours.POST_MARKET)
        assertThat(MarketHours.fromCode(99)).isEqualTo(MarketHours.UNKNOWN)
    }

    @Test
    @DisplayName("QuoteType enum은 코드로부터 올바르게 변환된다")
    fun `QuoteType fromCode converts correctly`() = unitTest {
        // Then
        assertThat(QuoteType.fromCode(1)).isEqualTo(QuoteType.EQUITY)
        assertThat(QuoteType.fromCode(2)).isEqualTo(QuoteType.ETF)
        assertThat(QuoteType.fromCode(5)).isEqualTo(QuoteType.OPTION)
        assertThat(QuoteType.fromCode(6)).isEqualTo(QuoteType.MUTUAL_FUND)
        assertThat(QuoteType.fromCode(8)).isEqualTo(QuoteType.INDEX)
        assertThat(QuoteType.fromCode(11)).isEqualTo(QuoteType.CRYPTOCURRENCY)
        assertThat(QuoteType.fromCode(12)).isEqualTo(QuoteType.CURRENCY)
        assertThat(QuoteType.fromCode(13)).isEqualTo(QuoteType.FUTURE)
        assertThat(QuoteType.fromCode(99)).isEqualTo(QuoteType.UNKNOWN)
    }

    @Test
    @DisplayName("ReconnectionConfig은 대기 시간을 올바르게 계산한다")
    fun `ReconnectionConfig calculates delay correctly`() = unitTest {
        // Given
        val config = ReconnectionConfig(
            initialDelayMs = 1000,
            maxDelayMs = 30000,
            backoffMultiplier = 2.0
        )

        // When & Then
        assertThat(config.calculateDelay(0)).isEqualTo(1000)   // 1초
        assertThat(config.calculateDelay(1)).isEqualTo(2000)   // 2초
        assertThat(config.calculateDelay(2)).isEqualTo(4000)   // 4초
        assertThat(config.calculateDelay(3)).isEqualTo(8000)   // 8초
        assertThat(config.calculateDelay(4)).isEqualTo(16000)  // 16초
        assertThat(config.calculateDelay(5)).isEqualTo(30000)  // 30초 (max)
        assertThat(config.calculateDelay(10)).isEqualTo(30000) // 30초 (max)
    }

    @Test
    @DisplayName("StreamingClientConfig은 기본값으로 생성된다")
    fun `StreamingClientConfig has correct defaults`() = unitTest {
        // When
        val config = StreamingClientConfig()

        // Then
        assertThat(config.webSocketUrl).isEqualTo("wss://streamer.finance.yahoo.com/?version=2")
        assertThat(config.connectTimeoutMs).isEqualTo(10000)
        assertThat(config.heartbeatIntervalMs).isEqualTo(15000)
        assertThat(config.pingTimeoutMs).isEqualTo(30000)
        assertThat(config.reconnection.enabled).isTrue()
        assertThat(config.reconnection.maxAttempts).isEqualTo(5)
        assertThat(config.eventBufferSize).isEqualTo(64)
        assertThat(config.enableLogging).isFalse()
    }

    @Test
    @DisplayName("PricingData는 Protobuf로부터 파싱된다")
    fun `PricingData deserializes from Protobuf`() = unitTest {
        // Given - 샘플 PricingData를 Protobuf로 인코딩
        val originalData = PricingData(
            id = "AAPL",
            price = 175.43f,
            time = 1701234567L,
            currency = "USD",
            exchange = "NMS",
            quoteType = 1,
            marketHours = 1,
            changePercent = 1.23f,
            dayVolume = 50000000L,
            dayHigh = 176.0f,
            dayLow = 174.0f,
            change = 2.15f,
            shortName = "Apple Inc.",
            openPrice = 175.0f,
            previousClose = 173.28f,
            bid = 175.42f,
            bidSize = 100L,
            ask = 175.44f,
            askSize = 200L
        )

        // When - Protobuf로 직렬화 후 역직렬화
        val bytes = protoBuf.encodeToByteArray(PricingData.serializer(), originalData)
        val decodedData = protoBuf.decodeFromByteArray(PricingData.serializer(), bytes)

        // Then
        assertThat(decodedData.id).isEqualTo("AAPL")
        assertThat(decodedData.price).isEqualTo(175.43f)
        assertThat(decodedData.time).isEqualTo(1701234567L)
        assertThat(decodedData.currency).isEqualTo("USD")
        assertThat(decodedData.exchange).isEqualTo("NMS")
        assertThat(decodedData.quoteType).isEqualTo(1)
        assertThat(decodedData.marketHours).isEqualTo(1)
        assertThat(decodedData.changePercent).isEqualTo(1.23f)
        assertThat(decodedData.dayVolume).isEqualTo(50000000L)
        assertThat(decodedData.shortName).isEqualTo("Apple Inc.")
    }

    @Test
    @DisplayName("StreamingPrice는 올바르게 생성된다")
    fun `StreamingPrice is constructed correctly`() = unitTest {
        // When
        val price = StreamingPrice(
            symbol = "AAPL",
            price = 175.43,
            change = 2.15,
            changePercent = 1.24,
            timestamp = 1701234567L,
            volume = 50000000L,
            bid = 175.42,
            ask = 175.44,
            marketHours = MarketHours.REGULAR
        )

        // Then
        assertThat(price.symbol).isEqualTo("AAPL")
        assertThat(price.price).isEqualTo(175.43)
        assertThat(price.change).isEqualTo(2.15)
        assertThat(price.changePercent).isEqualTo(1.24)
        assertThat(price.timestamp).isEqualTo(1701234567L)
        assertThat(price.volume).isEqualTo(50000000L)
        assertThat(price.bid).isEqualTo(175.42)
        assertThat(price.ask).isEqualTo(175.44)
        assertThat(price.marketHours).isEqualTo(MarketHours.REGULAR)
    }

    @Test
    @DisplayName("StreamingQuote는 올바르게 생성된다")
    fun `StreamingQuote is constructed correctly`() = unitTest {
        // When
        val quote = StreamingQuote(
            symbol = "AAPL",
            price = 175.43,
            change = 2.15,
            changePercent = 1.24,
            timestamp = 1701234567L,
            volume = 50000000L,
            bid = 175.42,
            ask = 175.44,
            marketHours = MarketHours.REGULAR,
            dayHigh = 176.0,
            dayLow = 174.0,
            openPrice = 175.0,
            previousClose = 173.28,
            bidSize = 100L,
            askSize = 200L,
            currency = "USD",
            exchange = "NMS",
            shortName = "Apple Inc.",
            quoteType = QuoteType.EQUITY
        )

        // Then
        assertThat(quote.symbol).isEqualTo("AAPL")
        assertThat(quote.price).isEqualTo(175.43)
        assertThat(quote.dayHigh).isEqualTo(176.0)
        assertThat(quote.dayLow).isEqualTo(174.0)
        assertThat(quote.openPrice).isEqualTo(175.0)
        assertThat(quote.previousClose).isEqualTo(173.28)
        assertThat(quote.currency).isEqualTo("USD")
        assertThat(quote.exchange).isEqualTo("NMS")
        assertThat(quote.shortName).isEqualTo("Apple Inc.")
        assertThat(quote.quoteType).isEqualTo(QuoteType.EQUITY)
    }

    @Test
    @DisplayName("StreamingPrice의 nullable 필드는 null을 허용한다")
    fun `StreamingPrice nullable fields accept null`() = unitTest {
        // When
        val price = StreamingPrice(
            symbol = "AAPL",
            price = 175.43,
            change = 2.15,
            changePercent = 1.24,
            timestamp = 1701234567L,
            volume = 50000000L,
            bid = null,
            ask = null,
            marketHours = MarketHours.REGULAR
        )

        // Then
        assertThat(price.bid).isNull()
        assertThat(price.ask).isNull()
    }

    @Test
    @DisplayName("StreamingQuote의 nullable 필드는 null을 허용한다")
    fun `StreamingQuote nullable fields accept null`() = unitTest {
        // When
        val quote = StreamingQuote(
            symbol = "AAPL",
            price = 175.43,
            change = 2.15,
            changePercent = 1.24,
            timestamp = 1701234567L,
            volume = 50000000L,
            bid = null,
            ask = null,
            marketHours = MarketHours.REGULAR,
            dayHigh = 176.0,
            dayLow = 174.0,
            openPrice = 175.0,
            previousClose = 173.28,
            bidSize = null,
            askSize = null,
            currency = "USD",
            exchange = "NMS",
            shortName = "Apple Inc.",
            quoteType = QuoteType.EQUITY
        )

        // Then
        assertThat(quote.bid).isNull()
        assertThat(quote.ask).isNull()
        assertThat(quote.bidSize).isNull()
        assertThat(quote.askSize).isNull()
    }
}
