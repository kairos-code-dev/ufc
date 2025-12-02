package com.ulalax.ufc.domain.quote

import com.ulalax.ufc.exception.ApiException
import com.ulalax.ufc.exception.ErrorCode
import com.ulalax.ufc.exception.UfcException
import com.ulalax.ufc.infrastructure.ratelimit.RateLimiter
import com.ulalax.ufc.internal.yahoo.auth.AuthResult
import org.assertj.core.api.Assertions.*
import kotlinx.coroutines.runBlocking
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@Tag("unit")
@DisplayName("YahooQuoteSummaryService 테스트")
internal class YahooQuoteSummaryServiceTest {

    private val mockRateLimiter = mockk<RateLimiter> {
        coEvery { acquire() } returns Unit
    }

    private val authResult = AuthResult(
        crumb = "test_crumb_token",
        strategy = "basic"
    )

    @Test
    @DisplayName("정상적인 응답을 파싱하여 QuoteSummaryResult를 반환해야 한다")
    fun testParseValidResponse() {
        val mockResponse = """
            {
                "quoteSummary": {
                    "result": [
                        {
                            "price": {
                                "symbol": "AAPL",
                                "regularMarketPrice": {"raw": 150.0, "fmt": "150.00"},
                                "currency": "USD",
                                "exchange": "NASDAQ",
                                "regularMarketPreviousClose": {"raw": 149.5, "fmt": "149.50"}
                            },
                            "summaryDetail": {
                                "dividendRate": {"raw": 0.92, "fmt": "0.92"},
                                "dividendYield": {"raw": 0.006, "fmt": "0.60%"},
                                "averageVolume": {"raw": 50000000, "fmt": "50.00M"},
                                "beta": {"raw": 1.2, "fmt": "1.20"},
                                "trailingPE": {"raw": 25.5, "fmt": "25.50"},
                                "forwardPE": {"raw": 23.0, "fmt": "23.00"},
                                "marketCap": {"raw": 2500000000000, "fmt": "2.50T"}
                            }
                        }
                    ]
                }
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            respond(
                content = mockResponse,
                status = HttpStatusCode.OK
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }

        val service = YahooQuoteSummaryService(httpClient, mockRateLimiter, authResult)

        val result = runBlocking {
            service.getQuoteSummary("AAPL")
        }

        assertThat(result).isNotNull()
        assertThat(result.price?.symbol).isEqualTo("AAPL")
        assertThat(result.price?.regularMarketPrice?.doubleValue).isEqualTo(150.0)
    }

    @Test
    @DisplayName("빈 심볼로 요청 시 UfcException을 발생시켜야 한다")
    fun testEmptySymbol() {
        val mockEngine = MockEngine { request ->
            respond(content = "", status = HttpStatusCode.OK)
        }

        val httpClient = HttpClient(mockEngine)
        val service = YahooQuoteSummaryService(httpClient, mockRateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getQuoteSummary("")
            }
        }.isInstanceOf(UfcException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SYMBOL)
    }

    @Test
    @DisplayName("너무 긴 심볼로 요청 시 UfcException을 발생시켜야 한다")
    fun testTooLongSymbol() {
        val mockEngine = MockEngine { request ->
            respond(content = "", status = HttpStatusCode.OK)
        }

        val httpClient = HttpClient(mockEngine)
        val service = YahooQuoteSummaryService(httpClient, mockRateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getQuoteSummary("VERYLONGSYMBOLTHATEXCEEDSLIMIT")
            }
        }.isInstanceOf(UfcException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SYMBOL)
    }

    @Test
    @DisplayName("HTTP 오류 응답을 받을 경우 ApiException을 발생시켜야 한다")
    fun testHttpErrorResponse() {
        val mockEngine = MockEngine { request ->
            respond(
                content = "Not Found",
                status = HttpStatusCode.NotFound
            )
        }

        val httpClient = HttpClient(mockEngine)
        val service = YahooQuoteSummaryService(httpClient, mockRateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getQuoteSummary("AAPL")
            }
        }.isInstanceOf(ApiException::class.java)
    }

    @Test
    @DisplayName("API 에러 응답을 받을 경우 ApiException을 발생시켜야 한다")
    fun testApiErrorResponse() {
        val mockResponse = """
            {
                "quoteSummary": {
                    "error": {
                        "code": "invalid_symbol",
                        "description": "Invalid symbol"
                    }
                }
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            respond(
                content = mockResponse,
                status = HttpStatusCode.OK
            )
        }

        val httpClient = HttpClient(mockEngine)
        val service = YahooQuoteSummaryService(httpClient, mockRateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getQuoteSummary("AAPL")
            }
        }.isInstanceOf(ApiException::class.java)
    }

    @Test
    @DisplayName("빈 결과를 받을 경우 UfcException을 발생시켜야 한다")
    fun testEmptyResult() {
        val mockResponse = """
            {
                "quoteSummary": {
                    "result": []
                }
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            respond(
                content = mockResponse,
                status = HttpStatusCode.OK
            )
        }

        val httpClient = HttpClient(mockEngine)
        val service = YahooQuoteSummaryService(httpClient, mockRateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getQuoteSummary("AAPL")
            }
        }.isInstanceOf(UfcException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_NOT_FOUND)
    }

    @Test
    @DisplayName("특정 모듈을 요청할 수 있어야 한다")
    fun testSpecificModules() {
        val mockResponse = """
            {
                "quoteSummary": {
                    "result": [
                        {
                            "price": {
                                "symbol": "AAPL",
                                "regularMarketPrice": {"raw": 150.0, "fmt": "150.00"}
                            },
                            "summaryDetail": {
                                "beta": {"raw": 1.2, "fmt": "1.20"}
                            }
                        }
                    ]
                }
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            respond(
                content = mockResponse,
                status = HttpStatusCode.OK
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }

        val service = YahooQuoteSummaryService(httpClient, mockRateLimiter, authResult)

        val result = runBlocking {
            service.getQuoteSummary("AAPL", listOf("price", "summaryDetail"))
        }

        assertThat(result).isNotNull()
        assertThat(result.price?.regularMarketPrice?.doubleValue).isEqualTo(150.0)
    }

    @Test
    @DisplayName("빈 모듈 목록으로 요청 시 UfcException을 발생시켜야 한다")
    fun testEmptyModuleList() {
        val mockEngine = MockEngine { request ->
            respond(content = "", status = HttpStatusCode.OK)
        }

        val httpClient = HttpClient(mockEngine)
        val service = YahooQuoteSummaryService(httpClient, mockRateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getQuoteSummary("AAPL", emptyList())
            }
        }.isInstanceOf(UfcException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PARAMETER)
    }

    @Test
    @DisplayName("여러 심볼의 요약 정보를 조회해야 한다")
    fun testMultipleSymbols() {
        val mockResponse = """
            {
                "quoteSummary": {
                    "result": [
                        {
                            "price": {
                                "symbol": "AAPL",
                                "regularMarketPrice": {"raw": 150.0, "fmt": "150.00"}
                            }
                        }
                    ]
                }
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            respond(
                content = mockResponse,
                status = HttpStatusCode.OK
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }

        val service = YahooQuoteSummaryService(httpClient, mockRateLimiter, authResult)

        val result = runBlocking {
            service.getQuoteSummary(listOf("AAPL", "GOOGL"))
        }

        assertThat(result.size).isEqualTo(2)
    }

    @Test
    @DisplayName("빈 심볼 목록으로 요청 시 UfcException을 발생시켜야 한다")
    fun testEmptySymbolList() {
        val mockEngine = MockEngine { request ->
            respond(content = "", status = HttpStatusCode.OK)
        }

        val httpClient = HttpClient(mockEngine)
        val service = YahooQuoteSummaryService(httpClient, mockRateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getQuoteSummary(emptyList())
            }
        }.isInstanceOf(UfcException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PARAMETER)
    }

    @Test
    @DisplayName("정규화된 StockSummary를 반환해야 한다")
    fun testStockSummary() {
        val mockResponse = """
            {
                "quoteSummary": {
                    "result": [
                        {
                            "price": {
                                "symbol": "AAPL",
                                "regularMarketPrice": {"raw": 150.0, "fmt": "150.00"},
                                "currency": "USD",
                                "exchange": "NASDAQ"
                            },
                            "summaryDetail": {
                                "dividendRate": {"raw": 0.92, "fmt": "0.92"},
                                "beta": {"raw": 1.2, "fmt": "1.20"},
                                "trailingPE": {"raw": 25.5, "fmt": "25.50"},
                                "marketCap": {"raw": 2500000000000, "fmt": "2.50T"}
                            },
                            "financialData": {
                                "returnOnEquity": {"raw": 0.8, "fmt": "80.00%"},
                                "profitMargins": {"raw": 0.25, "fmt": "25.00%"}
                            }
                        }
                    ]
                }
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            respond(
                content = mockResponse,
                status = HttpStatusCode.OK
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }

        val service = YahooQuoteSummaryService(httpClient, mockRateLimiter, authResult)

        val result = runBlocking {
            service.getStockSummary("AAPL")
        }

        assertThat(result).isNotNull()
        assertThat(result.symbol).isEqualTo("AAPL")
        assertThat(result.currentPrice).isEqualTo(150.0)
        assertThat(result.currency).isEqualTo("USD")
        assertThat(result.beta).isEqualTo(1.2)
    }

    @Test
    @DisplayName("원본 QuoteSummaryResponse를 반환해야 한다")
    fun testRawQuoteSummary() {
        val mockResponse = """
            {
                "quoteSummary": {
                    "result": [
                        {
                            "price": {
                                "symbol": "AAPL",
                                "regularMarketPrice": {"raw": 150.0, "fmt": "150.00"}
                            }
                        }
                    ]
                }
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            respond(
                content = mockResponse,
                status = HttpStatusCode.OK
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }

        val service = YahooQuoteSummaryService(httpClient, mockRateLimiter, authResult)

        val result = runBlocking {
            service.getRawQuoteSummary("AAPL")
        }

        assertThat(result).isNotNull()
        assertThat(result.quoteSummary.result?.size).isEqualTo(1)
    }

    @Test
    @DisplayName("각 요청마다 rate limiter를 호출해야 한다")
    fun testRateLimiterInvocation() {
        var callCount = 0
        val countingRateLimiter = mockk<RateLimiter> {
            coEvery { acquire() } answers {
                callCount++
            }
        }

        val mockResponse = """
            {
                "quoteSummary": {
                    "result": [
                        {
                            "price": {
                                "symbol": "AAPL",
                                "regularMarketPrice": {"raw": 150.0, "fmt": "150.00"}
                            }
                        }
                    ]
                }
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            respond(content = mockResponse, status = HttpStatusCode.OK)
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }

        val service = YahooQuoteSummaryService(httpClient, countingRateLimiter, authResult)

        runBlocking {
            service.getQuoteSummary("AAPL")
        }
        assertThat(callCount).isEqualTo(1)

        runBlocking {
            service.getQuoteSummary("AAPL")
        }
        assertThat(callCount).isEqualTo(2)
    }
}
