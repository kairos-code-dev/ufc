package com.ulalax.ufc.domain.funds

import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.fakes.FakeFundsHttpClient
import com.ulalax.ufc.fakes.TestQuoteSummaryResponseBuilder
import com.ulalax.ufc.infrastructure.util.CacheHelper
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * FundsServiceImpl 단위 테스트
 *
 * 테스트 전략:
 * - FakeFundsHttpClient를 사용하여 HTTP 호출 격리
 * - 캐싱 동작 검증 (호출 횟수 추적)
 * - 도메인 검증 로직 테스트
 * - 파싱 로직 테스트
 *
 * 테스트 격리:
 * - FundsHttpClient 인터페이스 덕분에 Fake 구현체로 간단하게 테스트
 * - HTTP 의존성 없음 (빠른 실행)
 *
 * Classical TDD (State-based Testing) 원칙:
 * - Fake 사용 (Mock 아님)
 * - 상태 검증 (호출 횟수, 결과 데이터)
 */
class FundsServiceImplTest {

    private lateinit var fakeHttpClient: FakeFundsHttpClient
    private lateinit var cache: CacheHelper
    private lateinit var service: FundsServiceImpl

    @BeforeEach
    fun setUp() {
        fakeHttpClient = FakeFundsHttpClient()
        cache = CacheHelper()
        service = FundsServiceImpl(fakeHttpClient, cache)
    }

    @AfterEach
    fun tearDown() {
        fakeHttpClient.clear()
        cache.clear()
    }

    // ============================================================================
    // getFundData Tests
    // ============================================================================

    @Test
    fun `getFundData should return fund data when symbol is valid ETF`() = runTest {
        // Given
        val symbol = "SPY"
        val response = TestQuoteSummaryResponseBuilder.createFundDataResponse(symbol, "ETF")
        fakeHttpClient.setQuoteSummaryResponse(symbol, response)

        // When
        val result = service.getFundData(symbol)

        // Then
        assertEquals(symbol, result.symbol)
        assertEquals("ETF", result.quoteType)
        assertNotNull(result.fundOverview)
        assertNotNull(result.topHoldings)

        // HTTP 1회만 호출되었는지 확인
        assertEquals(1, fakeHttpClient.quoteSummaryCallCount)
    }

    @Test
    fun `getFundData should use cache when called multiple times within TTL`() = runTest {
        // Given
        val symbol = "SPY"
        val response = TestQuoteSummaryResponseBuilder.createFundDataResponse(symbol, "ETF")
        fakeHttpClient.setQuoteSummaryResponse(symbol, response)

        // When
        service.getFundData(symbol)  // 첫 호출
        service.getFundData(symbol)  // 두 번째 호출 (캐시)
        service.getFundData(symbol)  // 세 번째 호출 (캐시)

        // Then
        assertEquals(1, fakeHttpClient.quoteSummaryCallCount)  // HTTP 1회만 호출
    }

    @Test
    fun `getFundData should throw exception when symbol is blank`() = runTest {
        // Given
        val symbol = ""

        // When & Then
        val exception = assertThrows<UfcException> {
            service.getFundData(symbol)
        }

        assertEquals(ErrorCode.INVALID_SYMBOL, exception.errorCode)
        assertTrue(exception.message!!.contains("비어있습니다"))
    }

    @Test
    fun `getFundData should throw exception when symbol is too long`() = runTest {
        // Given
        val symbol = "VERYLONGSYMBOL"

        // When & Then
        val exception = assertThrows<UfcException> {
            service.getFundData(symbol)
        }

        assertEquals(ErrorCode.INVALID_SYMBOL, exception.errorCode)
        assertTrue(exception.message!!.contains("너무 깁니다"))
    }

    @Test
    fun `getFundData should throw exception when quote type is not ETF or MUTUALFUND`() = runTest {
        // Given
        val symbol = "AAPL"
        val response = TestQuoteSummaryResponseBuilder.createFundDataResponse(symbol, "EQUITY")
        fakeHttpClient.setQuoteSummaryResponse(symbol, response)

        // When & Then
        val exception = assertThrows<UfcException> {
            service.getFundData(symbol)
        }

        assertEquals(ErrorCode.INVALID_FUND_TYPE, exception.errorCode)
        assertTrue(exception.message!!.contains("펀드가 아닙니다"))
    }

    // ============================================================================
    // getFundData (Batch) Tests
    // ============================================================================

    @Test
    fun `getFundData batch should return fund data for valid symbols`() = runTest {
        // Given
        val symbols = listOf("SPY", "AGG", "VTI")
        symbols.forEach { symbol ->
            val response = TestQuoteSummaryResponseBuilder.createFundDataResponse(symbol, "ETF")
            fakeHttpClient.setQuoteSummaryResponse(symbol, response)
        }

        // When
        val results = service.getFundData(symbols)

        // Then
        assertEquals(3, results.size)
        assertTrue(results.containsKey("SPY"))
        assertTrue(results.containsKey("AGG"))
        assertTrue(results.containsKey("VTI"))
    }

    @Test
    fun `getFundData batch should return partial results when some symbols fail`() = runTest {
        // Given
        val symbols = listOf("SPY", "INVALID", "AGG")

        // SPY와 AGG는 성공
        fakeHttpClient.setQuoteSummaryResponse(
            "SPY",
            TestQuoteSummaryResponseBuilder.createFundDataResponse("SPY", "ETF")
        )
        fakeHttpClient.setQuoteSummaryResponse(
            "AGG",
            TestQuoteSummaryResponseBuilder.createFundDataResponse("AGG", "ETF")
        )
        // INVALID는 응답 없음 (예외 발생)

        // When
        val results = service.getFundData(symbols)

        // Then
        assertEquals(2, results.size)
        assertTrue(results.containsKey("SPY"))
        assertTrue(results.containsKey("AGG"))
        assertFalse(results.containsKey("INVALID"))
    }

    @Test
    fun `getFundData batch should return empty map when symbols list is empty`() = runTest {
        // Given
        val symbols = emptyList<String>()

        // When
        val results = service.getFundData(symbols)

        // Then
        assertTrue(results.isEmpty())
        assertEquals(0, fakeHttpClient.quoteSummaryCallCount)
    }

    @Test
    fun `getFundData batch should throw exception when all symbols fail`() = runTest {
        // Given
        val symbols = listOf("INVALID1", "INVALID2")
        // 응답을 설정하지 않음 (모두 예외 발생)

        // When & Then
        val exception = assertThrows<UfcException> {
            service.getFundData(symbols)
        }

        assertEquals(ErrorCode.FUND_DATA_NOT_FOUND, exception.errorCode)
    }

    // ============================================================================
    // isFund Tests
    // ============================================================================

    @Test
    fun `isFund should return true when symbol is ETF`() = runTest {
        // Given
        val symbol = "SPY"
        val response = TestQuoteSummaryResponseBuilder.createFundDataResponse(symbol, "ETF")
        fakeHttpClient.setQuoteSummaryResponse(symbol, response)

        // When
        val result = service.isFund(symbol)

        // Then
        assertTrue(result)
    }

    @Test
    fun `isFund should return true when symbol is MUTUALFUND`() = runTest {
        // Given
        val symbol = "VTSAX"
        val response = TestQuoteSummaryResponseBuilder.createFundDataResponse(symbol, "MUTUALFUND")
        fakeHttpClient.setQuoteSummaryResponse(symbol, response)

        // When
        val result = service.isFund(symbol)

        // Then
        assertTrue(result)
    }

    @Test
    fun `isFund should return false when symbol is not a fund`() = runTest {
        // Given
        val symbol = "AAPL"
        val response = TestQuoteSummaryResponseBuilder.createFundDataResponse(symbol, "EQUITY")
        fakeHttpClient.setQuoteSummaryResponse(symbol, response)

        // When
        val result = service.isFund(symbol)

        // Then
        assertFalse(result)
    }

    @Test
    fun `isFund should return false when API call fails`() = runTest {
        // Given
        val symbol = "INVALID"
        // 응답을 설정하지 않음 (예외 발생)

        // When
        val result = service.isFund(symbol)

        // Then
        assertFalse(result)
    }

    // ============================================================================
    // getRawFundData Tests
    // ============================================================================

    @Test
    fun `getRawFundData should return raw response`() = runTest {
        // Given
        val symbol = "SPY"
        val response = TestQuoteSummaryResponseBuilder.createFundDataResponse(symbol, "ETF")
        fakeHttpClient.setQuoteSummaryResponse(symbol, response)

        // When
        val result = service.getRawFundData(symbol)

        // Then
        assertNotNull(result)
        assertNotNull(result.quoteSummary.result)
        assertEquals(1, fakeHttpClient.quoteSummaryCallCount)
    }
}
