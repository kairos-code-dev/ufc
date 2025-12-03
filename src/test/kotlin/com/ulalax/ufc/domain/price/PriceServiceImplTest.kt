package com.ulalax.ufc.domain.price

import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.fakes.FakePriceHttpClient
import com.ulalax.ufc.fakes.TestChartResponseBuilder
import com.ulalax.ufc.fakes.TestQuoteSummaryResponseBuilder
import com.ulalax.ufc.domain.common.Interval
import com.ulalax.ufc.domain.common.Period
import com.ulalax.ufc.infrastructure.util.CacheHelper
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

/**
 * PriceServiceImpl 단위 테스트
 *
 * 테스트 전략:
 * - FakePriceHttpClient를 사용하여 HTTP 호출 격리
 * - 캐싱 동작 검증 (호출 횟수 추적)
 * - 도메인 검증 로직 테스트
 * - 파싱 로직 테스트
 *
 * 테스트 격리:
 * - PriceHttpClient 인터페이스 덕분에 Fake 구현체로 간단하게 테스트
 * - HTTP 의존성 없음 (빠른 실행)
 */
class PriceServiceImplTest {

    private lateinit var fakeHttpClient: FakePriceHttpClient
    private lateinit var cache: CacheHelper
    private lateinit var service: PriceServiceImpl

    @BeforeEach
    fun setUp() {
        fakeHttpClient = FakePriceHttpClient()
        cache = CacheHelper()
        service = PriceServiceImpl(fakeHttpClient, cache)
    }

    @AfterEach
    fun tearDown() {
        fakeHttpClient.clear()
        cache.clear()
    }

    // ============================================================================
    // getCurrentPrice Tests
    // ============================================================================

    @Test
    fun `getCurrentPrice should return price data when symbol is valid`() = runTest {
        // Given
        val symbol = "AAPL"
        val expectedPrice = 150.0
        val response = TestQuoteSummaryResponseBuilder.createPriceResponse(symbol, expectedPrice, 145.0)
        fakeHttpClient.setQuoteSummaryResponse(symbol, response)

        // When
        val result = service.getCurrentPrice(symbol)

        // Then
        assertEquals(symbol, result.symbol)
        assertEquals(expectedPrice, result.lastPrice)
        assertEquals(expectedPrice, result.regularMarketPrice)
        assertEquals(145.0, result.previousClose)
        assertEquals("USD", result.currency)
        assertEquals("NMS", result.exchange)

        // HTTP 1회만 호출되었는지 확인
        assertEquals(1, fakeHttpClient.quoteSummaryCallCount)
    }

    @Test
    fun `getCurrentPrice should use cache when called multiple times within TTL`() = runTest {
        // Given
        val symbol = "AAPL"
        val response = TestQuoteSummaryResponseBuilder.createPriceResponse(symbol, 150.0)
        fakeHttpClient.setQuoteSummaryResponse(symbol, response)

        // When
        service.getCurrentPrice(symbol)  // 첫 호출
        service.getCurrentPrice(symbol)  // 두 번째 호출 (캐시)
        service.getCurrentPrice(symbol)  // 세 번째 호출 (캐시)

        // Then
        assertEquals(1, fakeHttpClient.quoteSummaryCallCount)  // HTTP 1회만 호출
    }

    @Test
    fun `getCurrentPrice should throw exception when symbol is blank`() = runTest {
        // When & Then
        val exception = assertThrows<UfcException> {
            service.getCurrentPrice("")
        }
        assertEquals(ErrorCode.INVALID_SYMBOL, exception.errorCode)
    }

    @Test
    fun `getCurrentPrice should throw exception when symbol is too long`() = runTest {
        // When & Then
        val exception = assertThrows<UfcException> {
            service.getCurrentPrice("A".repeat(21))
        }
        assertEquals(ErrorCode.INVALID_SYMBOL, exception.errorCode)
    }

    @Test
    fun `getCurrentPrice should throw exception when symbol contains invalid characters`() = runTest {
        // When & Then
        val exception = assertThrows<UfcException> {
            service.getCurrentPrice("AAPL@#$")
        }
        assertEquals(ErrorCode.INVALID_SYMBOL, exception.errorCode)
    }

    @Test
    fun `getCurrentPrice should return price map when multiple symbols are provided`() = runTest {
        // Given
        val symbols = listOf("AAPL", "GOOGL", "MSFT")
        symbols.forEach { symbol ->
            val response = TestQuoteSummaryResponseBuilder.createPriceResponse(symbol, 100.0 + symbol.length)
            fakeHttpClient.setQuoteSummaryResponse(symbol, response)
        }

        // When
        val result = service.getCurrentPrice(symbols)

        // Then
        assertEquals(3, result.size)
        assertTrue(result.containsKey("AAPL"))
        assertTrue(result.containsKey("GOOGL"))
        assertTrue(result.containsKey("MSFT"))

        // 각 심볼마다 1회씩 호출
        assertEquals(3, fakeHttpClient.quoteSummaryCallCount)
    }

    @Test
    fun `getCurrentPrice should throw exception when symbol list is empty`() = runTest {
        // When & Then
        assertThrows<IllegalArgumentException> {
            service.getCurrentPrice(emptyList())
        }
    }

    @Test
    fun `getCurrentPrice should throw exception when symbol count exceeds limit`() = runTest {
        // Given
        val symbols = (1..101).map { "SYM$it" }

        // When & Then
        assertThrows<IllegalArgumentException> {
            service.getCurrentPrice(symbols)
        }
    }

    // ============================================================================
    // getPriceHistory Tests
    // ============================================================================

    @Test
    fun `getPriceHistory should return OHLCV data when period and interval are valid`() = runTest {
        // Given
        val symbol = "AAPL"
        val response = TestChartResponseBuilder.createChartResponse(symbol, dataPoints = 10, basePrice = 100.0)
        fakeHttpClient.setChartResponse(symbol, response)

        // When
        val result = service.getPriceHistory(symbol, Period.OneYear, Interval.OneDay)

        // Then
        assertEquals(10, result.size)
        assertEquals(100.0, result[0].open)
        assertEquals(101.0, result[0].close)
        assertEquals(1, fakeHttpClient.chartCallCount)
    }

    @Test
    fun `getPriceHistory should use cache when called multiple times with same parameters`() = runTest {
        // Given
        val symbol = "AAPL"
        val response = TestChartResponseBuilder.createChartResponse(symbol)
        fakeHttpClient.setChartResponse(symbol, response)

        // When
        service.getPriceHistory(symbol, Period.OneYear, Interval.OneDay)  // 첫 호출
        service.getPriceHistory(symbol, Period.OneYear, Interval.OneDay)  // 두 번째 호출 (캐시)

        // Then
        assertEquals(1, fakeHttpClient.chartCallCount)  // HTTP 1회만 호출
    }

    @Test
    fun `getPriceHistory should throw exception when intraday interval exceeds period limit`() = runTest {
        // When & Then
        val exception = assertThrows<UfcException> {
            service.getPriceHistory("AAPL", Period.OneYear, Interval.OneMinute)
        }
        assertEquals(ErrorCode.INVALID_PERIOD_INTERVAL, exception.errorCode)
    }

    @Test
    fun `getPriceHistory should throw exception when hourly interval exceeds period limit`() = runTest {
        // When & Then
        val exception = assertThrows<UfcException> {
            service.getPriceHistory("AAPL", Period.OneYear, Interval.OneHour)
        }
        assertEquals(ErrorCode.INVALID_PERIOD_INTERVAL, exception.errorCode)
    }

    @Test
    fun `getPriceHistory should return OHLCV data when date range is provided`() = runTest {
        // Given
        val symbol = "AAPL"
        val start = LocalDate.of(2023, 1, 1)
        val end = LocalDate.of(2023, 12, 31)
        val response = TestChartResponseBuilder.createChartResponse(symbol)
        fakeHttpClient.setChartResponse(symbol, response)

        // When
        val result = service.getPriceHistory(symbol, start, end, Interval.OneDay)

        // Then
        assertTrue(result.isNotEmpty())
        assertEquals(1, fakeHttpClient.chartCallCount)
    }

    @Test
    fun `getPriceHistory should throw exception when start date is after end date`() = runTest {
        // Given
        val start = LocalDate.of(2023, 12, 31)
        val end = LocalDate.of(2023, 1, 1)

        // When & Then
        val exception = assertThrows<UfcException> {
            service.getPriceHistory("AAPL", start, end, Interval.OneDay)
        }
        assertEquals(ErrorCode.INVALID_DATE_RANGE, exception.errorCode)
    }

    // ============================================================================
    // getPriceHistoryWithMetadata Tests
    // ============================================================================

    @Test
    fun `getPriceHistoryWithMetadata should return metadata and OHLCV data when parameters are valid`() = runTest {
        // Given
        val symbol = "AAPL"
        val response = TestChartResponseBuilder.createChartResponse(symbol)
        fakeHttpClient.setChartResponse(symbol, response)

        // When
        val (metadata, ohlcv) = service.getPriceHistoryWithMetadata(symbol, Period.OneYear, Interval.OneDay)

        // Then
        assertEquals(symbol, metadata.symbol)
        assertEquals("USD", metadata.currency)
        assertEquals("NMS", metadata.exchangeName)
        assertTrue(ohlcv.isNotEmpty())
        assertEquals(1, fakeHttpClient.chartCallCount)
    }

    @Test
    fun `getPriceHistoryWithMetadata should use cache when called multiple times with same parameters`() = runTest {
        // Given
        val symbol = "AAPL"
        val response = TestChartResponseBuilder.createChartResponse(symbol)
        fakeHttpClient.setChartResponse(symbol, response)

        // When
        service.getPriceHistoryWithMetadata(symbol, Period.OneYear, Interval.OneDay)  // 첫 호출
        service.getPriceHistoryWithMetadata(symbol, Period.OneYear, Interval.OneDay)  // 두 번째 호출 (캐시)

        // Then
        assertEquals(1, fakeHttpClient.chartCallCount)  // HTTP 1회만 호출
    }

    // ============================================================================
    // getHistoryMetadata Tests
    // ============================================================================

    @Test
    fun `getHistoryMetadata should return chart metadata when symbol is valid`() = runTest {
        // Given
        val symbol = "AAPL"
        val response = TestChartResponseBuilder.createChartResponse(symbol)
        fakeHttpClient.setChartResponse(symbol, response)

        // When
        val result = service.getHistoryMetadata(symbol)

        // Then
        assertEquals(symbol, result.symbol)
        assertEquals("USD", result.currency)
        assertEquals("NMS", result.exchangeName)
        assertEquals("1d", result.dataGranularity)
        assertEquals(1, fakeHttpClient.chartCallCount)
    }

    // ============================================================================
    // getRawPrice and getRawPriceHistory Tests
    // ============================================================================

    @Test
    fun `getRawPrice should return raw response when symbol is valid`() = runTest {
        // Given
        val symbol = "AAPL"
        val response = TestQuoteSummaryResponseBuilder.createPriceResponse(symbol, 150.0)
        fakeHttpClient.setQuoteSummaryResponse(symbol, response)

        // When
        val result = service.getRawPrice(symbol)

        // Then
        assertNotNull(result)
        assertEquals(symbol, result.quoteSummary.result?.first()?.price?.symbol)
        assertEquals(1, fakeHttpClient.quoteSummaryCallCount)
    }

    @Test
    fun `getRawPriceHistory should return raw chart response when parameters are valid`() = runTest {
        // Given
        val symbol = "AAPL"
        val response = TestChartResponseBuilder.createChartResponse(symbol)
        fakeHttpClient.setChartResponse(symbol, response)

        // When
        val result = service.getRawPriceHistory(symbol, Period.OneYear, Interval.OneDay)

        // Then
        assertNotNull(result)
        assertEquals(symbol, result.chart.result?.first()?.meta?.symbol)
        assertEquals(1, fakeHttpClient.chartCallCount)
    }

    // ============================================================================
    // Error Handling Tests
    // ============================================================================

    @Test
    fun `getCurrentPrice should propagate exception when HTTP client fails`() = runTest {
        // Given
        val symbol = "AAPL"
        fakeHttpClient.setException(UfcException(ErrorCode.EXTERNAL_API_ERROR, "Network error"))

        // When & Then
        val exception = assertThrows<UfcException> {
            service.getCurrentPrice(symbol)
        }
        assertEquals(ErrorCode.EXTERNAL_API_ERROR, exception.errorCode)
    }

    @Test
    fun `getPriceHistory should propagate exception when HTTP client fails`() = runTest {
        // Given
        val symbol = "AAPL"
        fakeHttpClient.setException(UfcException(ErrorCode.EXTERNAL_API_ERROR, "Network error"))

        // When & Then
        val exception = assertThrows<UfcException> {
            service.getPriceHistory(symbol, Period.OneYear, Interval.OneDay)
        }
        assertEquals(ErrorCode.EXTERNAL_API_ERROR, exception.errorCode)
    }
}
