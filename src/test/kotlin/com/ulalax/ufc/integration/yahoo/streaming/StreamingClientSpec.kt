package com.ulalax.ufc.integration.yahoo.streaming

import com.ulalax.ufc.domain.model.streaming.StreamingEvent
import com.ulalax.ufc.infrastructure.yahoo.streaming.StreamingClient
import com.ulalax.ufc.infrastructure.yahoo.streaming.StreamingClientConfig
import com.ulalax.ufc.integration.utils.IntegrationTestBase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

@DisplayName("StreamingClient - Integration Test")
class StreamingClientSpec : IntegrationTestBase() {

    private lateinit var client: StreamingClient

    @AfterEach
    fun cleanUp() {
        if (::client.isInitialized) {
            client.close()
        }
    }

    @Test
    @DisplayName("WebSocket 연결을 성공적으로 수립할 수 있다")
    fun `can establish WebSocket connection`() = runBlocking {
        // Given
        val config = StreamingClientConfig(enableLogging = true)
        client = StreamingClient.create(config)

        // When
        client.connect()

        // Then
        assertThat(client.isConnected()).isTrue()

        // 연결 이벤트 확인
        withTimeout(5000) {
            val event = client.events.first { it is StreamingEvent.Connected }
            assertThat(event).isInstanceOf(StreamingEvent.Connected::class.java)
        }
    }

    @Test
    @DisplayName("단일 심볼을 구독하고 데이터를 수신할 수 있다")
    fun `can subscribe to single symbol and receive data`() = runBlocking {
        // Given
        val config = StreamingClientConfig(enableLogging = true)
        client = StreamingClient.create(config)

        // When
        client.subscribe("AAPL")

        // Then
        assertThat(client.isConnected()).isTrue()
        assertThat(client.getSubscribedSymbols()).contains("AAPL")

        // 가격 데이터 수신 대기 (최대 30초)
        withTimeout(30000) {
            val price = client.prices.first()
            assertThat(price.symbol).isEqualToIgnoringCase("AAPL")
            assertThat(price.price).isGreaterThan(0.0)
            assertThat(price.timestamp).isGreaterThan(0L)
        }
    }

    @Test
    @DisplayName("여러 심볼을 구독하고 데이터를 수신할 수 있다")
    fun `can subscribe to multiple symbols and receive data`() = runBlocking {
        // Given
        val config = StreamingClientConfig(enableLogging = true)
        client = StreamingClient.create(config)

        // When
        client.subscribe(listOf("AAPL", "GOOGL", "MSFT"))

        // Then
        assertThat(client.getSubscribedSymbols()).containsExactlyInAnyOrder("AAPL", "GOOGL", "MSFT")

        // 각 심볼의 데이터 수신 확인 (최대 30초)
        withTimeout(30000) {
            val prices = client.prices.take(3).toList()
            assertThat(prices).hasSize(3)
            assertThat(prices.map { it.symbol.uppercase() }).containsAnyOf("AAPL", "GOOGL", "MSFT")
        }
    }

    @Test
    @DisplayName("특정 심볼의 데이터만 필터링할 수 있다")
    fun `can filter data by symbol`() = runBlocking {
        // Given
        val config = StreamingClientConfig(enableLogging = true)
        client = StreamingClient.create(config)
        client.subscribe(listOf("AAPL", "GOOGL"))

        // When
        withTimeout(30000) {
            val applePrice = client.pricesBySymbol("AAPL").first()

            // Then
            assertThat(applePrice.symbol).isEqualToIgnoringCase("AAPL")
            assertThat(applePrice.price).isGreaterThan(0.0)
        }
    }

    @Test
    @DisplayName("구독을 해제할 수 있다")
    fun `can unsubscribe from symbols`() = runBlocking {
        // Given
        val config = StreamingClientConfig(enableLogging = true)
        client = StreamingClient.create(config)
        client.subscribe(listOf("AAPL", "GOOGL"))

        // When
        client.unsubscribe("AAPL")

        // Then
        assertThat(client.getSubscribedSymbols()).containsExactly("GOOGL")
    }

    @Test
    @DisplayName("모든 구독을 해제할 수 있다")
    fun `can unsubscribe all symbols`() = runBlocking {
        // Given
        val config = StreamingClientConfig(enableLogging = true)
        client = StreamingClient.create(config)
        client.subscribe(listOf("AAPL", "GOOGL", "MSFT"))

        // When
        client.unsubscribeAll()

        // Then
        assertThat(client.getSubscribedSymbols()).isEmpty()
    }

    @Test
    @DisplayName("연결을 종료하고 재연결할 수 있다")
    fun `can disconnect and reconnect`() = runBlocking {
        // Given
        val config = StreamingClientConfig(enableLogging = true)
        client = StreamingClient.create(config)
        client.connect()
        assertThat(client.isConnected()).isTrue()

        // When - 연결 종료
        client.disconnect()
        delay(1000)

        // Then
        assertThat(client.isConnected()).isFalse()

        // When - 재연결
        client.connect()

        // Then
        assertThat(client.isConnected()).isTrue()
    }

    @Test
    @DisplayName("StreamingQuote 데이터를 수신할 수 있다")
    fun `can receive StreamingQuote data`() = runBlocking {
        // Given
        val config = StreamingClientConfig(enableLogging = true)
        client = StreamingClient.create(config)

        // When
        client.subscribe("AAPL")

        // Then - Quote 데이터 수신 대기 (필수 필드가 모두 있는 경우)
        withTimeout(30000) {
            val quote = client.quotes.first()
            assertThat(quote.symbol).isEqualToIgnoringCase("AAPL")
            assertThat(quote.price).isGreaterThan(0.0)
            assertThat(quote.currency).isNotBlank()
            assertThat(quote.exchange).isNotBlank()
            assertThat(quote.shortName).isNotBlank()
        }
    }

    @Test
    @DisplayName("이벤트 스트림을 통해 연결 상태를 추적할 수 있다")
    fun `can track connection status through events`() = runBlocking {
        // Given
        val config = StreamingClientConfig(enableLogging = true)
        client = StreamingClient.create(config)

        val events = mutableListOf<StreamingEvent>()

        // When
        client.connect()

        // Then
        withTimeout(5000) {
            val connectedEvent = client.events.first { it is StreamingEvent.Connected }
            assertThat(connectedEvent).isInstanceOf(StreamingEvent.Connected::class.java)
        }
    }

    @Test
    @DisplayName("구독 업데이트 이벤트를 수신할 수 있다")
    fun `can receive subscription update events`() = runBlocking {
        // Given
        val config = StreamingClientConfig(enableLogging = true)
        client = StreamingClient.create(config)

        // When
        client.subscribe("AAPL")

        // Then
        withTimeout(5000) {
            val event = client.events.first { it is StreamingEvent.SubscriptionUpdated }
            assertThat(event).isInstanceOf(StreamingEvent.SubscriptionUpdated::class.java)

            val subscriptionEvent = event as StreamingEvent.SubscriptionUpdated
            assertThat(subscriptionEvent.symbols).contains("AAPL")
        }
    }
}
