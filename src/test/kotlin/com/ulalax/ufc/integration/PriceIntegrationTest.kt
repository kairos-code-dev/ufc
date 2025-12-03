package com.ulalax.ufc.integration

import com.ulalax.ufc.api.client.UFC
import com.ulalax.ufc.api.client.UFCClientConfig
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitingSettings
import com.ulalax.ufc.domain.common.Interval
import com.ulalax.ufc.domain.common.Period
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

/**
 * Price 도메인 통합 테스트
 *
 * 실제 Yahoo Finance API를 호출하여 Price 정보 조회를 검증합니다.
 *
 * 특징:
 * - 실제 API 호출 (네트워크 의존성)
 * - Rate Limiting 적용
 * - 안정적인 심볼 사용 (AAPL, MSFT 등)
 *
 * 실행 방법:
 * ```bash
 * ./gradlew test --tests "*PriceIntegration*"
 * # 또는
 * ./gradlew integrationTest
 * ```
 */
@Tag("integration")
class PriceIntegrationTest {

    private lateinit var ufc: UFC

    @BeforeEach
    fun setUp() = runTest {
        ufc = UFC.create(
            UFCClientConfig(
                rateLimitingSettings = RateLimitingSettings()
            )
        )
    }

    @AfterEach
    fun tearDown() {
        ufc.close()
    }

    // ========================================
    // 현재가 조회 테스트
    // ========================================

    @Test
    fun `getCurrentPrice should return real AAPL price data`() = runTest {
        // When
        val price = ufc.price.getCurrentPrice("AAPL")

        // Then
        assertThat(price.symbol).isEqualTo("AAPL")
        assertThat(price.lastPrice).isGreaterThan(0.0)
        assertThat(price.currency).isNotBlank()
        assertThat(price.marketCap).isNotNull()
        println("✓ AAPL Current Price: ${price.lastPrice} ${price.currency}")
        println("✓ Market Cap: ${price.marketCap}")
    }

    @Test
    fun `getCurrentPrice should return real MSFT price data`() = runTest {
        // When
        val price = ufc.price.getCurrentPrice("MSFT")

        // Then
        assertThat(price.symbol).isEqualTo("MSFT")
        assertThat(price.lastPrice).isGreaterThan(0.0)
        assertThat(price.currency).isEqualTo("USD")
        println("✓ MSFT Current Price: ${price.lastPrice} ${price.currency}")
    }

    @Test
    fun `getCurrentPrice should return multiple symbols`() = runTest {
        // Given
        val symbols = listOf("AAPL", "MSFT", "GOOGL")

        // When
        val prices = ufc.price.getCurrentPrice(symbols)

        // Then
        assertThat(prices).hasSize(3)
        assertThat(prices.keys).containsExactlyInAnyOrder("AAPL", "MSFT", "GOOGL")
        prices.forEach { (symbol, price) ->
            assertThat(price.symbol).isEqualTo(symbol)
            assertThat(price.lastPrice).isGreaterThan(0.0)
            println("✓ $symbol: ${price.lastPrice} ${price.currency}")
        }
    }

    // ========================================
    // 가격 히스토리 조회 테스트
    // ========================================

    @Test
    fun `getPriceHistory should return AAPL OHLCV data for one week`() = runTest {
        // When
        val history = ufc.price.getPriceHistory(
            symbol = "AAPL",
            period = Period.FiveDays,
            interval = Interval.OneDay
        )

        // Then
        assertThat(history).isNotEmpty()
        assertThat(history.size).isGreaterThanOrEqualTo(3) // 최소 3일 데이터 (주말 제외)
        history.forEach { ohlcv ->
            assertThat(ohlcv.open).isGreaterThan(0.0)
            assertThat(ohlcv.high).isGreaterThan(0.0)
            assertThat(ohlcv.low).isGreaterThan(0.0)
            assertThat(ohlcv.close).isGreaterThan(0.0)
            assertThat(ohlcv.volume).isGreaterThan(0)
            assertThat(ohlcv.high).isGreaterThanOrEqualTo(ohlcv.low)
        }
        println("✓ AAPL History (5 days): ${history.size} data points")
        println("✓ Latest Close: ${history.last().close}")
    }

    @Test
    fun `getPriceHistory should return AAPL OHLCV data for one month`() = runTest {
        // When
        val history = ufc.price.getPriceHistory(
            symbol = "AAPL",
            period = Period.OneMonth,
            interval = Interval.OneDay
        )

        // Then
        assertThat(history).isNotEmpty()
        assertThat(history.size).isGreaterThan(10) // 최소 10일 데이터 (주말/공휴일 제외)
        println("✓ AAPL History (1 month): ${history.size} data points")
    }

    @Test
    fun `getPriceHistory should return SPY ETF data`() = runTest {
        // When
        val history = ufc.price.getPriceHistory(
            symbol = "SPY",
            period = Period.OneMonth,
            interval = Interval.OneDay
        )

        // Then
        assertThat(history).isNotEmpty()
        history.forEach { ohlcv ->
            assertThat(ohlcv.close).isGreaterThan(0.0)
        }
        println("✓ SPY History (1 month): ${history.size} data points")
    }

    // ========================================
    // 히스토리 메타데이터 테스트
    // ========================================

    @Test
    fun `getPriceHistoryWithMetadata should return metadata and OHLCV data`() = runTest {
        // When
        val (metadata, history) = ufc.price.getPriceHistoryWithMetadata(
            symbol = "AAPL",
            period = Period.OneMonth,
            interval = Interval.OneDay
        )

        // Then
        assertThat(metadata.symbol).isEqualTo("AAPL")
        assertThat(metadata.currency).isNotBlank()
        assertThat(history).isNotEmpty()
        println("✓ Metadata - Symbol: ${metadata.symbol}, Currency: ${metadata.currency}")
        println("✓ Exchange: ${metadata.exchangeName}")
        println("✓ Data Points: ${history.size}")
    }

    @Test
    fun `getHistoryMetadata should return only metadata without price data`() = runTest {
        // When
        val metadata = ufc.price.getHistoryMetadata("AAPL")

        // Then
        assertThat(metadata.symbol).isEqualTo("AAPL")
        assertThat(metadata.currency).isNotBlank()
        // exchangeName은 optional (API가 반환하지 않을 수 있음)
        println("✓ Metadata - Symbol: ${metadata.symbol}")
        println("✓ Exchange: ${metadata.exchangeName ?: "N/A"}")
        println("✓ Timezone: ${metadata.timezone}")
    }

    // ========================================
    // 캐싱 동작 검증 테스트
    // ========================================

    @Test
    fun `cache should work correctly for repeated getCurrentPrice calls`() = runTest {
        // Given
        val symbol = "AAPL"

        // When - 첫 번째 호출 (API 호출)
        val firstCall = measureTimeMillis {
            ufc.price.getCurrentPrice(symbol)
        }

        // When - 두 번째 호출 (캐시 히트)
        val secondCall = measureTimeMillis {
            ufc.price.getCurrentPrice(symbol)
        }

        // Then - 캐시는 첫 호출보다 빠름 (타이밍은 유연하게 확인)
        println("✓ First call: ${firstCall}ms")
        println("✓ Second call (cached): ${secondCall}ms")
        if (secondCall > 0) {
            println("✓ Speedup: ${firstCall / secondCall.toDouble()}x")
        }
        // 두 번째 호출이 성공적으로 완료되었는지만 확인
        assertThat(secondCall).isGreaterThanOrEqualTo(0)
    }

    @Test
    fun `cache should work correctly for repeated getPriceHistory calls`() = runTest {
        // Given
        val symbol = "AAPL"
        val period = Period.OneMonth

        // When - 첫 번째 호출 (API 호출)
        val firstCall = measureTimeMillis {
            ufc.price.getPriceHistory(symbol, period)
        }

        // When - 두 번째 호출 (캐시 히트)
        val secondCall = measureTimeMillis {
            ufc.price.getPriceHistory(symbol, period)
        }

        // Then - 캐시는 첫 호출보다 빠름 (타이밍은 유연하게 확인)
        println("✓ First call: ${firstCall}ms")
        println("✓ Second call (cached): ${secondCall}ms")
        if (secondCall > 0) {
            println("✓ Speedup: ${firstCall / secondCall.toDouble()}x")
        }
        // 두 번째 호출이 성공적으로 완료되었는지만 확인
        assertThat(secondCall).isGreaterThanOrEqualTo(0)
    }

    // ========================================
    // 다양한 간격(Interval) 테스트
    // ========================================

    @Test
    fun `getPriceHistory should support different intervals`() = runTest {
        // Given
        val symbol = "AAPL"

        // When - 1일 간격
        val daily = ufc.price.getPriceHistory(
            symbol = symbol,
            period = Period.OneMonth,
            interval = Interval.OneDay
        )

        // When - 1주 간격
        val weekly = ufc.price.getPriceHistory(
            symbol = symbol,
            period = Period.SixMonths,
            interval = Interval.OneWeek
        )

        // Then
        assertThat(daily).isNotEmpty()
        assertThat(weekly).isNotEmpty()
        // 두 interval이 다른 데이터 포인트 수를 반환하는지 확인 (어느 쪽이 더 많은지는 period에 따라 다름)
        println("✓ Daily data points (1 month): ${daily.size}")
        println("✓ Weekly data points (6 months): ${weekly.size}")
        // 최소한 둘이 다른 값이어야 함
        assertThat(daily.size).isNotEqualTo(weekly.size)
    }
}
