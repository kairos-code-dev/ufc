package com.ulalax.ufc.domain.corp

import com.ulalax.ufc.fakes.FakeCorpHttpClient
import com.ulalax.ufc.fakes.TestChartResponseBuilder
import com.ulalax.ufc.domain.common.Period
import com.ulalax.ufc.infrastructure.util.CacheHelper
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * CorpServiceImpl 단위 테스트
 *
 * 테스트 전략:
 * - FakeCorpHttpClient를 사용하여 HTTP 호출 격리
 * - 캐싱 동작 검증 (호출 횟수 추적)
 * - 파싱 로직 테스트
 *
 * 테스트 격리:
 * - CorpHttpClient 인터페이스 덕분에 Fake 구현체로 간단하게 테스트
 * - HTTP 의존성 없음 (빠른 실행)
 *
 * Classical TDD (State-based Testing) 원칙:
 * - Fake 사용 (Mock 아님)
 * - 상태 검증 (호출 횟수, 결과 데이터)
 */
class CorpServiceImplTest {

    private lateinit var fakeHttpClient: FakeCorpHttpClient
    private lateinit var cache: CacheHelper
    private lateinit var service: CorpServiceImpl

    @BeforeEach
    fun setUp() {
        fakeHttpClient = FakeCorpHttpClient()
        cache = CacheHelper()
        service = CorpServiceImpl(fakeHttpClient, cache)
    }

    @AfterEach
    fun tearDown() {
        fakeHttpClient.clear()
        cache.clear()
    }

    // ============================================================================
    // getDividends Tests
    // ============================================================================

    @Test
    fun `getDividends should return dividend history when data exists`() = runTest {
        // Given
        val symbol = "AAPL"
        val period = Period.FiveYears
        val response = TestChartResponseBuilder.createChartDataWithDividends(symbol)
        fakeHttpClient.setChartDataResponse(symbol, response)

        // When
        val result = service.getDividends(symbol, period)

        // Then
        assertEquals(symbol, result.symbol)
        assertTrue(result.dividends.isNotEmpty())
        assertEquals(1, fakeHttpClient.chartDataCallCount)
    }

    @Test
    fun `getDividends should use cache when called multiple times within TTL`() = runTest {
        // Given
        val symbol = "AAPL"
        val period = Period.FiveYears
        val response = TestChartResponseBuilder.createChartDataWithDividends(symbol)
        fakeHttpClient.setChartDataResponse(symbol, response)

        // When
        service.getDividends(symbol, period)  // 첫 호출
        service.getDividends(symbol, period)  // 두 번째 호출 (캐시)
        service.getDividends(symbol, period)  // 세 번째 호출 (캐시)

        // Then
        assertEquals(1, fakeHttpClient.chartDataCallCount)  // HTTP 1회만 호출
    }

    @Test
    fun `getDividends should return empty list when no dividend events exist`() = runTest {
        // Given
        val symbol = "AAPL"
        val period = Period.FiveYears
        val response = TestChartResponseBuilder.createEmptyChartData(symbol)
        fakeHttpClient.setChartDataResponse(symbol, response)

        // When
        val result = service.getDividends(symbol, period)

        // Then
        assertEquals(symbol, result.symbol)
        assertTrue(result.dividends.isEmpty())
    }

    // ============================================================================
    // getSplits Tests
    // ============================================================================

    @Test
    fun `getSplits should return split history when data exists`() = runTest {
        // Given
        val symbol = "AAPL"
        val period = Period.Max
        val response = TestChartResponseBuilder.createChartDataWithSplits(symbol)
        fakeHttpClient.setChartDataResponse(symbol, response)

        // When
        val result = service.getSplits(symbol, period)

        // Then
        assertEquals(symbol, result.symbol)
        assertTrue(result.splits.isNotEmpty())
        assertEquals(1, fakeHttpClient.chartDataCallCount)
    }

    @Test
    fun `getSplits should return empty list when no split events exist`() = runTest {
        // Given
        val symbol = "AAPL"
        val period = Period.Max
        val response = TestChartResponseBuilder.createEmptyChartData(symbol)
        fakeHttpClient.setChartDataResponse(symbol, response)

        // When
        val result = service.getSplits(symbol, period)

        // Then
        assertEquals(symbol, result.symbol)
        assertTrue(result.splits.isEmpty())
    }

    // ============================================================================
    // getCapitalGains Tests
    // ============================================================================

    @Test
    fun `getCapitalGains should return capital gain history when data exists`() = runTest {
        // Given
        val symbol = "SPY"
        val period = Period.FiveYears
        val response = TestChartResponseBuilder.createChartDataWithCapitalGains(symbol)
        fakeHttpClient.setChartDataResponse(symbol, response)

        // When
        val result = service.getCapitalGains(symbol, period)

        // Then
        assertEquals(symbol, result.symbol)
        assertTrue(result.capitalGains.isNotEmpty())
        assertEquals(1, fakeHttpClient.chartDataCallCount)
    }

    @Test
    fun `getCapitalGains should return empty list when no capital gain events exist`() = runTest {
        // Given
        val symbol = "SPY"
        val period = Period.FiveYears
        val response = TestChartResponseBuilder.createEmptyChartData(symbol)
        fakeHttpClient.setChartDataResponse(symbol, response)

        // When
        val result = service.getCapitalGains(symbol, period)

        // Then
        assertEquals(symbol, result.symbol)
        assertTrue(result.capitalGains.isEmpty())
    }

    // ============================================================================
    // Cache Tests
    // ============================================================================

    @Test
    fun `getCorporateAction should cache data across different method calls`() = runTest {
        // Given
        val symbol = "AAPL"
        val period = Period.FiveYears
        val response = TestChartResponseBuilder.createChartDataWithAll(symbol)
        fakeHttpClient.setChartDataResponse(symbol, response)

        // When
        service.getDividends(symbol, period)
        service.getSplits(symbol, period)
        service.getCapitalGains(symbol, period)

        // Then
        assertEquals(1, fakeHttpClient.chartDataCallCount)  // 모든 메서드가 같은 캐시 사용
    }
}
