package com.ulalax.ufc.domain.chart

import com.ulalax.ufc.exception.ApiException
import com.ulalax.ufc.exception.DataParsingException
import com.ulalax.ufc.exception.ErrorCode
import com.ulalax.ufc.exception.UfcException
import com.ulalax.ufc.infrastructure.ratelimit.fakes.FakeRateLimiter
import com.ulalax.ufc.internal.yahoo.auth.AuthResult
import com.ulalax.ufc.model.common.Interval
import com.ulalax.ufc.model.common.Period
import com.ulalax.ufc.internal.fakes.TestHttpClientFactory
import com.ulalax.ufc.domain.auth.fakes.FakeAuthStrategy
import org.assertj.core.api.Assertions.*
import kotlinx.coroutines.runBlocking
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach

/**
 * YahooChartService 스펙 스타일 단위 테스트
 *
 * 클래식 TDD와 Specification-based testing을 적용합니다:
 * - @Nested로 테스트 그룹화 (테스트 관점별 분류)
 * - @DisplayName으로 명확한 명세화
 * - Given-When-Then 구조
 * - Fake 객체를 통한 의존성 격리
 * - 상태 검증(State Verification)
 */
@Tag("unit")
@DisplayName("YahooChartService - Yahoo Finance 차트 데이터 조회 서비스")
internal class YahooChartServiceTest {

    private lateinit var fakeRateLimiter: FakeRateLimiter
    private lateinit var fakeAuthStrategy: FakeAuthStrategy
    private lateinit var testHttpClient: HttpClient
    private lateinit var authResult: AuthResult
    private lateinit var service: YahooChartService

    @BeforeEach
    fun setUp() {
        // Fake 객체 초기화
        fakeRateLimiter = FakeRateLimiter("ChartService")
        fakeAuthStrategy = FakeAuthStrategy()

        // AuthResult 생성
        authResult = runBlocking {
            fakeAuthStrategy.authenticate()
        }

        // HttpClient 설정 (모든 요청에 대해 200 OK 반환)
        testHttpClient = TestHttpClientFactory.createBasicMockClient()

        // 서비스 생성
        service = YahooChartService(testHttpClient, fakeRateLimiter, authResult)
    }

    @AfterEach
    fun tearDown() {
        testHttpClient.close()
        fakeAuthStrategy.reset()
        fakeRateLimiter.reset()
    }

    @Nested
    @DisplayName("getChartData() - 차트 데이터 조회")
    inner class GetChartDataTests {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("유효한 심볼로 일일(Daily) 차트 데이터를 조회한다")
            fun `유효한 심볼로 일일 차트 데이터를 조회한다`() {
                // Given: 모킹된 응답 설정
                val mockResponse = """
                    {
                        "chart": {
                            "result": [
                                {
                                    "meta": {
                                        "symbol": "AAPL",
                                        "currency": "USD"
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

                val mockHttpClient = HttpClient(MockEngine { request ->
                    respond(content = mockResponse, status = HttpStatusCode.OK)
                })
                val service = YahooChartService(mockHttpClient, fakeRateLimiter, authResult)

                // When: 차트 데이터 조회
                val result = runBlocking {
                    service.getChartData("AAPL", Interval.OneDay, Period.OneMonth)
                }

                // Then: 데이터가 반환되고 유효성 검증
                assertThat(result).isNotEmpty().hasSize(2)
                assertThat(result).allMatch { ohlcv ->
                    ohlcv.timestamp > 0L &&
                    ohlcv.open > 0.0 &&
                    ohlcv.high > 0.0 &&
                    ohlcv.low > 0.0 &&
                    ohlcv.close > 0.0 &&
                    ohlcv.volume >= 0L
                }
                // Rate Limiter가 호출되었는가?
                assertThat(fakeRateLimiter.getAcquireCallCount()).isEqualTo(1)

                mockHttpClient.close()
            }

            @Test
            @DisplayName("시간(Hourly) 간격 차트 데이터를 조회한다")
            fun `시간 간격 차트 데이터를 조회한다`() {
                // Given
                val mockResponse = """
                    {
                        "chart": {
                            "result": [
                                {
                                    "meta": {"symbol": "AAPL"},
                                    "timestamp": [1609459200, 1609462800, 1609466400],
                                    "indicators": {
                                        "quote": [
                                            {
                                                "open": [150.0, 150.5, 151.0],
                                                "high": [152.0, 152.5, 153.0],
                                                "low": [149.0, 149.5, 150.0],
                                                "close": [151.0, 151.5, 152.0],
                                                "volume": [5000000, 5500000, 6000000]
                                            }
                                        ]
                                    }
                                }
                            ]
                        }
                    }
                """.trimIndent()

                val mockHttpClient = HttpClient(MockEngine { request ->
                    respond(content = mockResponse, status = HttpStatusCode.OK)
                })
                val service = YahooChartService(mockHttpClient, fakeRateLimiter, authResult)

                // When
                val result = runBlocking {
                    service.getChartData("AAPL", Interval.OneHour, Period.FiveDays)
                }

                // Then
                assertThat(result).isNotEmpty().hasSize(3)
                // 타임스탬프가 시간 단위로 증가
                for (i in 1 until result.size) {
                    assertThat(result[i].timestamp).isGreaterThan(result[i - 1].timestamp)
                }

                mockHttpClient.close()
            }

            @Test
            @DisplayName("분(Minutes) 간격 차트 데이터를 조회한다")
            fun `분 간격 차트 데이터를 조회한다`() {
                // Given
                val mockResponse = """
                    {
                        "chart": {
                            "result": [
                                {
                                    "meta": {"symbol": "GOOGL"},
                                    "timestamp": [1609459200, 1609459260],
                                    "indicators": {
                                        "quote": [
                                            {
                                                "open": [100.0, 100.5],
                                                "high": [101.0, 101.5],
                                                "low": [99.5, 100.0],
                                                "close": [100.5, 101.0],
                                                "volume": [1000000, 1100000]
                                            }
                                        ]
                                    }
                                }
                            ]
                        }
                    }
                """.trimIndent()

                val mockHttpClient = HttpClient(MockEngine { request ->
                    respond(content = mockResponse, status = HttpStatusCode.OK)
                })
                val service = YahooChartService(mockHttpClient, fakeRateLimiter, authResult)

                // When
                val result = runBlocking {
                    service.getChartData("GOOGL", Interval.FiveMinutes, Period.OneDay)
                }

                // Then
                assertThat(result).isNotEmpty().hasSize(2)

                mockHttpClient.close()
            }

            @Test
            @DisplayName("다양한 기간(Period)을 지원한다")
            fun `다양한 기간을 지원한다`() {
                // Given: 다양한 Period
                val periods = listOf(
                    Period.OneMonth,
                    Period.ThreeMonths,
                    Period.SixMonths,
                    Period.OneYear,
                    Period.Max
                )
                val mockResponse = """
                    {
                        "chart": {
                            "result": [
                                {
                                    "meta": {"symbol": "AAPL"},
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

                val mockHttpClient = HttpClient(MockEngine { request ->
                    respond(content = mockResponse, status = HttpStatusCode.OK)
                })
                val service = YahooChartService(mockHttpClient, fakeRateLimiter, authResult)

                // When & Then: 각 Period별로 데이터 조회 성공
                for (period in periods) {
                    val result = runBlocking {
                        service.getChartData("AAPL", Interval.OneDay, period)
                    }
                    assertThat(result).isNotEmpty()
                        .withFailMessage("Period: %s에서 데이터를 조회해야 함", period)
                }

                // Rate Limiter가 기간 개수만큼 호출됨
                assertThat(fakeRateLimiter.getAcquireCallCount()).isEqualTo(periods.size)

                mockHttpClient.close()
            }
        }

        @Nested
        @DisplayName("에러 케이스")
        inner class ErrorCases {

            @Test
            @DisplayName("빈 심볼로 조회 시 UfcException을 발생시킨다")
            fun `빈 심볼로 조회 시 예외를 발생시킨다`() {
                // Given: 빈 심볼
                val symbol = ""
                val interval = Interval.OneDay
                val period = Period.OneMonth

                // When & Then: 예외 발생
                assertThatThrownBy {
                    runBlocking {
                        service.getChartData(symbol, interval, period)
                    }
                }.isInstanceOf(UfcException::class.java)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SYMBOL)
            }

            @Test
            @DisplayName("너무 긴 심볼로 조회 시 예외를 발생시킨다")
            fun `너무 긴 심볼로 조회 시 예외를 발생시킨다`() {
                // Given: 너무 긴 심볼 (10자 초과)
                val symbol = "VERYLONGSYMBOL"
                val interval = Interval.OneDay
                val period = Period.OneMonth

                // When & Then
                assertThatThrownBy {
                    runBlocking {
                        service.getChartData(symbol, interval, period)
                    }
                }.isInstanceOf(UfcException::class.java)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SYMBOL)
            }

            @Test
            @DisplayName("HTTP 오류 응답을 받을 경우 ApiException을 발생시킨다")
            fun `HTTP 오류 응답을 받을 경우 예외를 발생시킨다`() {
                // Given
                val mockHttpClient = HttpClient(MockEngine { request ->
                    respond(content = "Not Found", status = HttpStatusCode.NotFound)
                })
                val service = YahooChartService(mockHttpClient, fakeRateLimiter, authResult)

                // When & Then
                assertThatThrownBy {
                    runBlocking {
                        service.getChartData("AAPL")
                    }
                }.isInstanceOf(ApiException::class.java)

                mockHttpClient.close()
            }

            @Test
            @DisplayName("잘못된 JSON 응답을 받을 경우 DataParsingException을 발생시킨다")
            fun `잘못된 JSON 응답을 받을 경우 예외를 발생시킨다`() {
                // Given
                val mockHttpClient = HttpClient(MockEngine { request ->
                    respond(content = "{ invalid json }", status = HttpStatusCode.OK)
                })
                val service = YahooChartService(mockHttpClient, fakeRateLimiter, authResult)

                // When & Then
                assertThatThrownBy {
                    runBlocking {
                        service.getChartData("AAPL")
                    }
                }.isInstanceOf(DataParsingException::class.java)

                mockHttpClient.close()
            }

            @Test
            @DisplayName("API 에러 응답을 받을 경우 ApiException을 발생시킨다")
            fun `API 에러 응답을 받을 경우 예외를 발생시킨다`() {
                // Given
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

                val mockHttpClient = HttpClient(MockEngine { request ->
                    respond(content = mockResponse, status = HttpStatusCode.OK)
                })
                val service = YahooChartService(mockHttpClient, fakeRateLimiter, authResult)

                // When & Then
                assertThatThrownBy {
                    runBlocking {
                        service.getChartData("AAPL")
                    }
                }.isInstanceOf(ApiException::class.java)

                mockHttpClient.close()
            }

            @Test
            @DisplayName("빈 결과를 받을 경우 UfcException을 발생시킨다")
            fun `빈 결과를 받을 경우 예외를 발생시킨다`() {
                // Given
                val mockResponse = """
                    {
                        "chart": {
                            "result": []
                        }
                    }
                """.trimIndent()

                val mockHttpClient = HttpClient(MockEngine { request ->
                    respond(content = mockResponse, status = HttpStatusCode.OK)
                })
                val service = YahooChartService(mockHttpClient, fakeRateLimiter, authResult)

                // When & Then
                assertThatThrownBy {
                    runBlocking {
                        service.getChartData("AAPL")
                    }
                }.isInstanceOf(UfcException::class.java)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_NOT_FOUND)

                mockHttpClient.close()
            }

            @Test
            @DisplayName("null 값을 포함한 OHLCV 데이터를 필터링한다")
            fun `null 값을 포함한 OHLCV 데이터를 필터링한다`() {
                // Given
                val mockResponse = """
                    {
                        "chart": {
                            "result": [
                                {
                                    "meta": {"symbol": "AAPL"},
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

                val mockHttpClient = HttpClient(MockEngine { request ->
                    respond(content = mockResponse, status = HttpStatusCode.OK)
                })
                val service = YahooChartService(mockHttpClient, fakeRateLimiter, authResult)

                // When
                val result = runBlocking {
                    service.getChartData("AAPL")
                }

                // Then: null을 포함한 데이터는 필터링되어야 함
                assertThat(result).hasSize(2)
                assertThat(result[0].timestamp).isEqualTo(1609459200)
                assertThat(result[1].timestamp).isEqualTo(1609632000)

                mockHttpClient.close()
            }
        }
    }

    @Nested
    @DisplayName("다중 심볼 조회")
    inner class MultipleSymbolsTests {

        @Test
        @DisplayName("여러 심볼의 차트 데이터를 조회한다")
        fun `여러 심볼의 차트 데이터를 조회한다`() {
            // Given
            val mockResponse = """
                {
                    "chart": {
                        "result": [
                            {
                                "meta": {"symbol": "AAPL"},
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

            val mockHttpClient = HttpClient(MockEngine { request ->
                respond(content = mockResponse, status = HttpStatusCode.OK)
            })
            val service = YahooChartService(mockHttpClient, fakeRateLimiter, authResult)

            // When
            val result = runBlocking {
                service.getChartData(
                    listOf("AAPL", "GOOGL"),
                    Interval.OneDay,
                    Period.OneYear
                )
            }

            // Then
            assertThat(result.size).isEqualTo(2)
            assertThat(result.containsKey("AAPL")).isTrue()
            assertThat(result.containsKey("GOOGL")).isTrue()

            mockHttpClient.close()
        }

        @Test
        @DisplayName("빈 심볼 목록으로 요청 시 UfcException을 발생시킨다")
        fun `빈 심볼 목록으로 요청 시 예외를 발생시킨다`() {
            // Given & When & Then
            assertThatThrownBy {
                runBlocking {
                    service.getChartData(emptyList())
                }
            }.isInstanceOf(UfcException::class.java)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PARAMETER)
        }
    }

    @Nested
    @DisplayName("getRawChartData() - 원본 차트 응답 조회")
    inner class GetRawChartDataTests {

        @Test
        @DisplayName("원본 응답의 구조가 올바르다")
        fun `원본 응답의 구조가 올바르다`() {
            // Given
            val mockResponse = """
                {
                    "chart": {
                        "result": [
                            {
                                "meta": {"symbol": "AAPL", "currency": "USD"},
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

            val mockHttpClient = HttpClient(MockEngine { request ->
                respond(content = mockResponse, status = HttpStatusCode.OK)
            })
            val service = YahooChartService(mockHttpClient, fakeRateLimiter, authResult)

            // When
            val response = runBlocking {
                service.getRawChartData("AAPL")
            }

            // Then: 응답 구조 검증
            assertThat(response).isNotNull()
            assertThat(response.chart).isNotNull()
            assertThat(response.chart.result).isNotNull()
            assertThat(response.chart.result?.get(0)?.meta?.symbol).isEqualTo("AAPL")

            mockHttpClient.close()
        }
    }

    @Nested
    @DisplayName("Rate Limiting 검증")
    inner class RateLimitingTests {

        @Test
        @DisplayName("각 호출마다 Rate Limiter 토큰을 소비한다")
        fun `각 호출마다 Rate Limiter 토큰을 소비한다`() {
            // Given
            val mockResponse = """
                {
                    "chart": {
                        "result": [
                            {
                                "meta": {"symbol": "AAPL"},
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

            val mockHttpClient = HttpClient(MockEngine { request ->
                respond(content = mockResponse, status = HttpStatusCode.OK)
            })
            val service = YahooChartService(mockHttpClient, fakeRateLimiter, authResult)

            // When: 첫 번째 호출
            runBlocking {
                service.getChartData("AAPL")
            }
            assertThat(fakeRateLimiter.getAcquireCallCount()).isEqualTo(1)
            assertThat(fakeRateLimiter.getTotalTokensConsumed()).isEqualTo(1)

            // When: 두 번째 호출
            runBlocking {
                service.getChartData("AAPL")
            }

            // Then
            assertThat(fakeRateLimiter.getAcquireCallCount()).isEqualTo(2)
            assertThat(fakeRateLimiter.getTotalTokensConsumed()).isEqualTo(2)

            mockHttpClient.close()
        }
    }
}
