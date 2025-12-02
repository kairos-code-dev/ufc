package com.ulalax.ufc.domain.chart

import com.ulalax.ufc.exception.ApiException
import com.ulalax.ufc.exception.DataParsingException
import com.ulalax.ufc.exception.ErrorCode
import com.ulalax.ufc.exception.UfcException
import com.ulalax.ufc.infrastructure.ratelimit.RateLimiter
import com.ulalax.ufc.internal.yahoo.auth.AuthResult
import com.ulalax.ufc.model.common.Interval
import com.ulalax.ufc.model.common.Period
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
@DisplayName("YahooChartService 테스트")
internal class YahooChartServiceTest {

    private val mockRateLimiter = mockk<RateLimiter> {
        coEvery { acquire() } returns Unit
    }

    private val authResult = AuthResult(
        crumb = "test_crumb_token",
        strategy = "basic"
    )

    @Test
    @DisplayName("정상적인 응답을 파싱하여 OHLCVData 리스트를 반환해야 한다")
    fun testParseValidResponse() {
        // Arrange
        val mockResponse = """
            {
                "chart": {
                    "result": [
                        {
                            "meta": {
                                "symbol": "AAPL",
                                "currency": "USD",
                                "regularMarketPrice": 150.0,
                                "exchange": "NASDAQ"
                            },
                            "timestamp": [1609459200, 1609545600],
                            "indicators": {
                                "quote": [
                                    {
                                        "open": [150.0, 151.0],
                                        "high": [152.0, 153.0],
                                        "low": [149.0, 150.0],
                                        "close": [151.0, 152.0],
                                        "volume": [50000000, 60000000]
                                    }
                                ]
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

        val service = YahooChartService(httpClient, mockRateLimiter, authResult)

        // Act
        val result = runBlocking {
            service.getChartData("AAPL", Interval.OneDay, Period.OneYear)
        }

        // Assert
        assertThat(result).hasSize(2)
        assertThat(result[0].timestamp).isEqualTo(1609459200)
        assertThat(result[0].open).isEqualTo(150.0)
        assertThat(result[0].high).isEqualTo(152.0)
        assertThat(result[0].low).isEqualTo(149.0)
        assertThat(result[0].close).isEqualTo(151.0)
        assertThat(result[0].volume).isEqualTo(50000000)
    }

    @Test
    @DisplayName("빈 심볼로 요청 시 UfcException을 발생시켜야 한다")
    fun testEmptySymbol() {
        val mockEngine = MockEngine { request ->
            respond(content = "", status = HttpStatusCode.OK)
        }

        val httpClient = HttpClient(mockEngine)
        val service = YahooChartService(httpClient, mockRateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getChartData("")
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
        val service = YahooChartService(httpClient, mockRateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getChartData("VERYLONGSYMBOLTHATEXCEEDSLIMIT")
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
        val service = YahooChartService(httpClient, mockRateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getChartData("AAPL")
            }
        }.isInstanceOf(ApiException::class.java)
    }

    @Test
    @DisplayName("잘못된 JSON 응답을 받을 경우 DataParsingException을 발생시켜야 한다")
    fun testInvalidJsonResponse() {
        val mockEngine = MockEngine { request ->
            respond(
                content = "{ invalid json }",
                status = HttpStatusCode.OK
            )
        }

        val httpClient = HttpClient(mockEngine)
        val service = YahooChartService(httpClient, mockRateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getChartData("AAPL")
            }
        }.isInstanceOf(DataParsingException::class.java)
    }

    @Test
    @DisplayName("API 에러 응답을 받을 경우 ApiException을 발생시켜야 한다")
    fun testApiErrorResponse() {
        val mockResponse = """
            {
                "chart": {
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
        val service = YahooChartService(httpClient, mockRateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getChartData("AAPL")
            }
        }.isInstanceOf(ApiException::class.java)
    }

    @Test
    @DisplayName("빈 결과를 받을 경우 UfcException을 발생시켜야 한다")
    fun testEmptyResult() {
        val mockResponse = """
            {
                "chart": {
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
        val service = YahooChartService(httpClient, mockRateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getChartData("AAPL")
            }
        }.isInstanceOf(UfcException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_NOT_FOUND)
    }

    @Test
    @DisplayName("null 값을 포함한 OHLCV 데이터를 필터링해야 한다")
    fun testFilterNullValues() {
        val mockResponse = """
            {
                "chart": {
                    "result": [
                        {
                            "meta": {
                                "symbol": "AAPL"
                            },
                            "timestamp": [1609459200, 1609545600, 1609632000],
                            "indicators": {
                                "quote": [
                                    {
                                        "open": [150.0, null, 151.0],
                                        "high": [152.0, 153.0, 154.0],
                                        "low": [149.0, 150.0, 150.0],
                                        "close": [151.0, 152.0, 153.0],
                                        "volume": [50000000, 60000000, 70000000]
                                    }
                                ]
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

        val httpClient = HttpClient(mockEngine)
        val service = YahooChartService(httpClient, mockRateLimiter, authResult)

        val result = runBlocking {
            service.getChartData("AAPL")
        }

        // null을 포함한 데이터는 필터링되어야 함
        assertThat(result).hasSize(2)
        assertThat(result[0].timestamp).isEqualTo(1609459200)
        assertThat(result[1].timestamp).isEqualTo(1609632000)
    }

    @Test
    @DisplayName("여러 심볼의 차트 데이터를 조회해야 한다")
    fun testMultipleSymbols() {
        val mockResponse = """
            {
                "chart": {
                    "result": [
                        {
                            "meta": {
                                "symbol": "AAPL"
                            },
                            "timestamp": [1609459200],
                            "indicators": {
                                "quote": [
                                    {
                                        "open": [150.0],
                                        "high": [152.0],
                                        "low": [149.0],
                                        "close": [151.0],
                                        "volume": [50000000]
                                    }
                                ]
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

        val httpClient = HttpClient(mockEngine)
        val service = YahooChartService(httpClient, mockRateLimiter, authResult)

        val result = runBlocking {
            service.getChartData(
                listOf("AAPL", "GOOGL"),
                Interval.OneDay,
                Period.OneYear
            )
        }

        assertThat(result.size).isEqualTo(2)
        assertThat(result.containsKey("AAPL")).isTrue()
        assertThat(result.containsKey("GOOGL")).isTrue()
    }

    @Test
    @DisplayName("빈 심볼 목록으로 요청 시 UfcException을 발생시켜야 한다")
    fun testEmptySymbolList() {
        val mockEngine = MockEngine { request ->
            respond(content = "", status = HttpStatusCode.OK)
        }

        val httpClient = HttpClient(mockEngine)
        val service = YahooChartService(httpClient, mockRateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getChartData(emptyList())
            }
        }.isInstanceOf(UfcException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PARAMETER)
    }

    @Test
    @DisplayName("원본 ChartDataResponse를 반환해야 한다")
    fun testRawChartData() {
        val mockResponse = """
            {
                "chart": {
                    "result": [
                        {
                            "meta": {
                                "symbol": "AAPL",
                                "currency": "USD"
                            },
                            "timestamp": [1609459200],
                            "indicators": {
                                "quote": [
                                    {
                                        "open": [150.0],
                                        "high": [152.0],
                                        "low": [149.0],
                                        "close": [151.0],
                                        "volume": [50000000]
                                    }
                                ]
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

        val httpClient = HttpClient(mockEngine)
        val service = YahooChartService(httpClient, mockRateLimiter, authResult)

        val result = runBlocking {
            service.getRawChartData("AAPL")
        }

        assertThat(result).isNotNull()
        assertThat(result.chart.result?.size).isEqualTo(1)
        assertThat(result.chart.result?.get(0)?.meta?.symbol).isEqualTo("AAPL")
    }

    @Test
    @DisplayName("1분 간격 데이터를 조회할 수 있어야 한다")
    fun testOneMinuteInterval() {
        val mockResponse = """
            {
                "chart": {
                    "result": [
                        {
                            "meta": {
                                "symbol": "AAPL",
                                "dataGranularity": "1m"
                            },
                            "timestamp": [1609459200],
                            "indicators": {
                                "quote": [
                                    {
                                        "open": [150.0],
                                        "high": [152.0],
                                        "low": [149.0],
                                        "close": [151.0],
                                        "volume": [1000000]
                                    }
                                ]
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

        val httpClient = HttpClient(mockEngine)
        val service = YahooChartService(httpClient, mockRateLimiter, authResult)

        val result = runBlocking {
            service.getChartData("AAPL", Interval.OneMinute, Period.OneDay)
        }

        assertThat(result).hasSize(1)
        assertThat(result[0].timestamp).isEqualTo(1609459200)
    }

    @Test
    @DisplayName("1시간 간격 데이터를 조회할 수 있어야 한다")
    fun testOneHourInterval() {
        val mockResponse = """
            {
                "chart": {
                    "result": [
                        {
                            "meta": {
                                "symbol": "AAPL",
                                "dataGranularity": "1h"
                            },
                            "timestamp": [1609459200, 1609462800],
                            "indicators": {
                                "quote": [
                                    {
                                        "open": [150.0, 151.0],
                                        "high": [152.0, 153.0],
                                        "low": [149.0, 150.0],
                                        "close": [151.0, 152.0],
                                        "volume": [5000000, 6000000]
                                    }
                                ]
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

        val httpClient = HttpClient(mockEngine)
        val service = YahooChartService(httpClient, mockRateLimiter, authResult)

        val result = runBlocking {
            service.getChartData("AAPL", Interval.OneHour, Period.FiveDays)
        }

        assertThat(result).hasSize(2)
    }

    @Test
    @DisplayName("1년 기간 데이터를 조회할 수 있어야 한다")
    fun testOneYearPeriod() {
        val mockResponse = """
            {
                "chart": {
                    "result": [
                        {
                            "meta": {
                                "symbol": "AAPL",
                                "range": "1y"
                            },
                            "timestamp": [1609459200],
                            "indicators": {
                                "quote": [
                                    {
                                        "open": [150.0],
                                        "high": [152.0],
                                        "low": [149.0],
                                        "close": [151.0],
                                        "volume": [50000000]
                                    }
                                ]
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

        val httpClient = HttpClient(mockEngine)
        val service = YahooChartService(httpClient, mockRateLimiter, authResult)

        val result = runBlocking {
            service.getChartData("AAPL", Interval.OneDay, Period.OneYear)
        }

        assertThat(result).hasSize(1)
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
                "chart": {
                    "result": [
                        {
                            "meta": {"symbol": "AAPL"},
                            "timestamp": [1609459200],
                            "indicators": {
                                "quote": [{"open": [150.0], "high": [152.0], "low": [149.0], "close": [151.0], "volume": [50000000]}]
                            }
                        }
                    ]
                }
            }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            respond(content = mockResponse, status = HttpStatusCode.OK)
        }

        val httpClient = HttpClient(mockEngine)
        val service = YahooChartService(httpClient, countingRateLimiter, authResult)

        runBlocking {
            service.getChartData("AAPL")
        }
        assertThat(callCount).isEqualTo(1)

        runBlocking {
            service.getChartData("AAPL")
        }
        assertThat(callCount).isEqualTo(2)
    }
}
