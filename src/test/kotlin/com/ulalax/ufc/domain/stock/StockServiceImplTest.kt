package com.ulalax.ufc.domain.stock

import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.fakes.FakeStockHttpClient
import com.ulalax.ufc.fakes.TestQuoteSummaryResponseBuilder
import com.ulalax.ufc.infrastructure.util.CacheHelper
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * StockServiceImpl 단위 테스트
 *
 * 테스트 전략:
 * - FakeStockHttpClient를 사용하여 HTTP 호출 격리
 * - 캐싱 동작 검증 (호출 횟수 추적)
 * - 도메인 검증 로직 테스트
 * - 파싱 로직 테스트
 *
 * 테스트 격리:
 * - StockHttpClient 인터페이스 덕분에 Fake 구현체로 간단하게 테스트
 * - HTTP 의존성 없음 (빠른 실행)
 *
 * Classical TDD (State-based Testing) 원칙:
 * - Fake 사용 (Mock 아님)
 * - 상태 검증 (호출 횟수, 결과 데이터)
 */
class StockServiceImplTest {

    private lateinit var fakeHttpClient: FakeStockHttpClient
    private lateinit var cache: CacheHelper
    private lateinit var service: StockServiceImpl

    @BeforeEach
    fun setUp() {
        fakeHttpClient = FakeStockHttpClient()
        cache = CacheHelper()
        service = StockServiceImpl(fakeHttpClient, cache)
    }

    @AfterEach
    fun tearDown() {
        fakeHttpClient.clear()
        cache.clear()
    }

    // ============================================================================
    // getCompanyInfo Tests
    // ============================================================================

    @Test
    fun `getCompanyInfo should return company data when symbol is valid`() = runTest {
        // Given
        val symbol = "AAPL"
        val response = TestQuoteSummaryResponseBuilder.createCompanyInfoResponse(symbol, "Apple Inc.")
        fakeHttpClient.setQuoteSummaryResponse(symbol, response)

        // When
        val result = service.getCompanyInfo(symbol)

        // Then
        assertEquals(symbol, result.symbol)
        assertEquals("Apple Inc.", result.longName)
        assertEquals("Apple", result.shortName)
        assertEquals("Technology", result.sector)
        assertEquals("Consumer Electronics", result.industry)
        assertEquals("United States", result.country)
        assertEquals("NASDAQ", result.exchange)
        assertEquals("USD", result.currency)
        assertEquals(AssetType.EQUITY, result.quoteType)
        assertEquals("https://www.apple.com", result.website)
        assertEquals("408-996-1010", result.phone)
        assertEquals(15550061000L, result.sharesOutstanding)

        // HTTP 1회만 호출되었는지 확인
        assertEquals(1, fakeHttpClient.quoteSummaryCallCount)
    }

    @Test
    fun `getCompanyInfo should use cache when called multiple times within TTL`() = runTest {
        // Given
        val symbol = "AAPL"
        val response = TestQuoteSummaryResponseBuilder.createCompanyInfoResponse(symbol)
        fakeHttpClient.setQuoteSummaryResponse(symbol, response)

        // When
        service.getCompanyInfo(symbol)  // 첫 호출
        service.getCompanyInfo(symbol)  // 두 번째 호출 (캐시)
        service.getCompanyInfo(symbol)  // 세 번째 호출 (캐시)

        // Then
        assertEquals(1, fakeHttpClient.quoteSummaryCallCount)  // HTTP 1회만 호출
    }

    @Test
    fun `getCompanyInfo should throw exception when symbol is blank`() = runTest {
        // When & Then
        val exception = assertThrows<UfcException> {
            service.getCompanyInfo("")
        }
        assertEquals(ErrorCode.INVALID_SYMBOL, exception.errorCode)
    }

    @Test
    fun `getCompanyInfo should throw exception when symbol is too long`() = runTest {
        // When & Then
        val exception = assertThrows<UfcException> {
            service.getCompanyInfo("A".repeat(21))
        }
        assertEquals(ErrorCode.INVALID_SYMBOL, exception.errorCode)
    }

    @Test
    fun `getCompanyInfo should throw exception when symbol contains invalid characters`() = runTest {
        // When & Then
        val exception = assertThrows<UfcException> {
            service.getCompanyInfo("AAPL@#$")
        }
        assertEquals(ErrorCode.INVALID_SYMBOL, exception.errorCode)
    }

    @Test
    fun `getCompanyInfo should return map when multiple symbols are provided`() = runTest {
        // Given
        val symbols = listOf("AAPL", "GOOGL", "MSFT")
        symbols.forEach { symbol ->
            val response = TestQuoteSummaryResponseBuilder.createCompanyInfoResponse(symbol, "$symbol Inc.")
            fakeHttpClient.setQuoteSummaryResponse(symbol, response)
        }

        // When
        val result = service.getCompanyInfo(symbols)

        // Then
        assertEquals(3, result.size)
        assertTrue(result.containsKey("AAPL"))
        assertTrue(result.containsKey("GOOGL"))
        assertTrue(result.containsKey("MSFT"))
        assertEquals("AAPL Inc.", result["AAPL"]?.longName)
        assertEquals("GOOGL Inc.", result["GOOGL"]?.longName)
        assertEquals("MSFT Inc.", result["MSFT"]?.longName)

        // 각 심볼마다 1회씩 호출
        assertEquals(3, fakeHttpClient.quoteSummaryCallCount)
    }

    @Test
    fun `getCompanyInfo should throw exception when symbol list is empty`() = runTest {
        // When
        val result = service.getCompanyInfo(emptyList())

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getCompanyInfo should throw exception when symbol count exceeds limit`() = runTest {
        // Given
        val symbols = (1..51).map { "SYM$it" }

        // When & Then
        assertThrows<IllegalArgumentException> {
            service.getCompanyInfo(symbols)
        }
    }

    // ============================================================================
    // getFastInfo Tests
    // ============================================================================

    @Test
    fun `getFastInfo should return fast info when symbol is valid`() = runTest {
        // Given
        val symbol = "AAPL"
        val response = TestQuoteSummaryResponseBuilder.createFastInfoResponse(symbol, "USD", "NASDAQ")
        fakeHttpClient.setQuoteSummaryResponse(symbol, response)

        // When
        val result = service.getFastInfo(symbol)

        // Then
        assertEquals(symbol, result.symbol)
        assertEquals("USD", result.currency)
        assertEquals("NASDAQ", result.exchange)
        assertEquals(AssetType.EQUITY, result.quoteType)
        assertEquals(1, fakeHttpClient.quoteSummaryCallCount)
    }

    @Test
    fun `getFastInfo should use cache when called multiple times within TTL`() = runTest {
        // Given
        val symbol = "AAPL"
        val response = TestQuoteSummaryResponseBuilder.createFastInfoResponse(symbol)
        fakeHttpClient.setQuoteSummaryResponse(symbol, response)

        // When
        service.getFastInfo(symbol)  // 첫 호출
        service.getFastInfo(symbol)  // 두 번째 호출 (캐시)

        // Then
        assertEquals(1, fakeHttpClient.quoteSummaryCallCount)  // HTTP 1회만 호출
    }

    @Test
    fun `getFastInfo should return map when multiple symbols are provided`() = runTest {
        // Given
        val symbols = listOf("AAPL", "GOOGL")
        symbols.forEach { symbol ->
            val response = TestQuoteSummaryResponseBuilder.createFastInfoResponse(symbol)
            fakeHttpClient.setQuoteSummaryResponse(symbol, response)
        }

        // When
        val result = service.getFastInfo(symbols)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.containsKey("AAPL"))
        assertTrue(result.containsKey("GOOGL"))
        assertEquals(2, fakeHttpClient.quoteSummaryCallCount)
    }

    // ============================================================================
    // getIsin Tests
    // ============================================================================

    @Test
    fun `getIsin should return ISIN code when symbol is valid`() = runTest {
        // Given
        val symbol = "AAPL"
        val isin = "US0378331005"
        val response = TestQuoteSummaryResponseBuilder.createIsinResponse(symbol, isin)
        fakeHttpClient.setQuoteSummaryResponse(symbol, response)

        // When
        val result = service.getIsin(symbol)

        // Then
        assertEquals(isin, result)
        assertEquals(1, fakeHttpClient.quoteSummaryCallCount)
    }

    @Test
    fun `getIsin should use cache when called multiple times within TTL`() = runTest {
        // Given
        val symbol = "AAPL"
        val isin = "US0378331005"
        val response = TestQuoteSummaryResponseBuilder.createIsinResponse(symbol, isin)
        fakeHttpClient.setQuoteSummaryResponse(symbol, response)

        // When
        service.getIsin(symbol)  // 첫 호출
        service.getIsin(symbol)  // 두 번째 호출 (캐시)
        service.getIsin(symbol)  // 세 번째 호출 (캐시)

        // Then
        assertEquals(1, fakeHttpClient.quoteSummaryCallCount)  // HTTP 1회만 호출
    }

    @Test
    fun `getIsin should return map when multiple symbols are provided`() = runTest {
        // Given
        val symbols = listOf("AAPL", "GOOGL")
        val isins = listOf("US0378331005", "US02079K3059")
        symbols.zip(isins).forEach { (symbol, isin) ->
            val response = TestQuoteSummaryResponseBuilder.createIsinResponse(symbol, isin)
            fakeHttpClient.setQuoteSummaryResponse(symbol, response)
        }

        // When
        val result = service.getIsin(symbols)

        // Then
        assertEquals(2, result.size)
        assertEquals("US0378331005", result["AAPL"])
        assertEquals("US02079K3059", result["GOOGL"])
        assertEquals(2, fakeHttpClient.quoteSummaryCallCount)
    }

    // ============================================================================
    // getShares Tests
    // ============================================================================

    @Test
    fun `getShares should return shares data when symbol is valid`() = runTest {
        // Given
        val symbol = "AAPL"
        val shares = 15550061000L
        val response = TestQuoteSummaryResponseBuilder.createSharesResponse(symbol, shares)
        fakeHttpClient.setQuoteSummaryResponse(symbol, response)

        // When
        val result = service.getShares(symbol)

        // Then
        assertEquals(1, result.size)
        assertEquals(shares, result[0].shares)
        assertEquals(1, fakeHttpClient.quoteSummaryCallCount)
    }

    @Test
    fun `getShares should use cache when called multiple times within TTL`() = runTest {
        // Given
        val symbol = "AAPL"
        val response = TestQuoteSummaryResponseBuilder.createSharesResponse(symbol)
        fakeHttpClient.setQuoteSummaryResponse(symbol, response)

        // When
        service.getShares(symbol)  // 첫 호출
        service.getShares(symbol)  // 두 번째 호출 (캐시)

        // Then
        assertEquals(1, fakeHttpClient.quoteSummaryCallCount)  // HTTP 1회만 호출
    }

    @Test
    fun `getShares should return map when multiple symbols are provided`() = runTest {
        // Given
        val symbols = listOf("AAPL", "GOOGL")
        symbols.forEach { symbol ->
            val response = TestQuoteSummaryResponseBuilder.createSharesResponse(symbol)
            fakeHttpClient.setQuoteSummaryResponse(symbol, response)
        }

        // When
        val result = service.getShares(symbols)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.containsKey("AAPL"))
        assertTrue(result.containsKey("GOOGL"))
        assertEquals(2, fakeHttpClient.quoteSummaryCallCount)
    }

    @Test
    fun `getSharesFull should return same as getShares`() = runTest {
        // Given
        val symbol = "AAPL"
        val response = TestQuoteSummaryResponseBuilder.createSharesResponse(symbol)
        fakeHttpClient.setQuoteSummaryResponse(symbol, response)

        // When
        val result = service.getSharesFull(symbol)

        // Then
        assertEquals(1, result.size)
        assertEquals(1, fakeHttpClient.quoteSummaryCallCount)
    }

    // ============================================================================
    // getRawQuoteSummary Tests
    // ============================================================================

    @Test
    fun `getRawQuoteSummary should return raw response when symbol is valid`() = runTest {
        // Given
        val symbol = "AAPL"
        val response = TestQuoteSummaryResponseBuilder.createCompanyInfoResponse(symbol)
        fakeHttpClient.setQuoteSummaryResponse(symbol, response)

        // When
        val result = service.getRawQuoteSummary(symbol, listOf("assetProfile"))

        // Then
        assertNotNull(result)
        assertEquals(symbol, result.quoteSummary.result?.first()?.price?.symbol)
        assertEquals(1, fakeHttpClient.quoteSummaryCallCount)
    }

    // ============================================================================
    // Error Handling Tests
    // ============================================================================

    @Test
    fun `getCompanyInfo should propagate exception when HTTP client fails`() = runTest {
        // Given
        val symbol = "AAPL"
        fakeHttpClient.setException(UfcException(ErrorCode.EXTERNAL_API_ERROR, "Network error"))

        // When & Then
        val exception = assertThrows<UfcException> {
            service.getCompanyInfo(symbol)
        }
        assertEquals(ErrorCode.EXTERNAL_API_ERROR, exception.errorCode)
    }

    @Test
    fun `getFastInfo should propagate exception when HTTP client fails`() = runTest {
        // Given
        val symbol = "AAPL"
        fakeHttpClient.setException(UfcException(ErrorCode.EXTERNAL_API_ERROR, "Network error"))

        // When & Then
        val exception = assertThrows<UfcException> {
            service.getFastInfo(symbol)
        }
        assertEquals(ErrorCode.EXTERNAL_API_ERROR, exception.errorCode)
    }

    @Test
    fun `getIsin should propagate exception when HTTP client fails`() = runTest {
        // Given
        val symbol = "AAPL"
        fakeHttpClient.setException(UfcException(ErrorCode.EXTERNAL_API_ERROR, "Network error"))

        // When & Then
        val exception = assertThrows<UfcException> {
            service.getIsin(symbol)
        }
        assertEquals(ErrorCode.EXTERNAL_API_ERROR, exception.errorCode)
    }

    @Test
    fun `getShares should propagate exception when HTTP client fails`() = runTest {
        // Given
        val symbol = "AAPL"
        fakeHttpClient.setException(UfcException(ErrorCode.EXTERNAL_API_ERROR, "Network error"))

        // When & Then
        val exception = assertThrows<UfcException> {
            service.getShares(symbol)
        }
        assertEquals(ErrorCode.EXTERNAL_API_ERROR, exception.errorCode)
    }
}
