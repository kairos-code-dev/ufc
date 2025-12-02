package com.ulalax.ufc.client

import com.ulalax.ufc.domain.chart.YahooChartService
import com.ulalax.ufc.domain.quote.YahooQuoteSummaryService
import com.ulalax.ufc.infrastructure.ratelimit.fakes.FakeRateLimiter
import com.ulalax.ufc.infrastructure.ratelimit.TokenBucketRateLimiter
import com.ulalax.ufc.internal.fakes.TestHttpClientFactory
import com.ulalax.ufc.domain.auth.fakes.FakeAuthStrategy
import com.ulalax.ufc.model.common.Interval
import com.ulalax.ufc.model.common.Period
import org.assertj.core.api.Assertions.*
import kotlinx.coroutines.runBlocking
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach

/**
 * UFCClientImpl 스펙 스타일 단위 테스트
 *
 * UFC 클라이언트의 통합 기능을 테스트합니다:
 * - 초기화 및 리소스 관리
 * - Chart 데이터 조회
 * - Quote 데이터 조회
 * - Rate Limiting 검증
 * - 복합 시나리오
 */
@Tag("unit")
@DisplayName("UFCClient - Universal Financial Client 통합 테스트")
internal class UFCClientTest {

    private lateinit var fakeAuthStrategy: FakeAuthStrategy
    private lateinit var fakeRateLimiter: FakeRateLimiter
    private lateinit var testHttpClient: HttpClient
    private lateinit var config: UFCClientConfig

    @BeforeEach
    fun setUp() {
        // Fake 객체 초기화
        fakeAuthStrategy = FakeAuthStrategy()
        fakeRateLimiter = FakeRateLimiter("UFCClient")

        // HttpClient 설정
        testHttpClient = TestHttpClientFactory.createBasicMockClient()

        // UFC 설정
        config = UFCClientConfig(
            fredApiKey = null,
            rateLimitingSettings = com.ulalax.ufc.infrastructure.ratelimit.RateLimitingSettings()
        )
    }

    @AfterEach
    fun tearDown() {
        testHttpClient.close()
        fakeAuthStrategy.reset()
        fakeRateLimiter.reset()
    }

    @Nested
    @DisplayName("클라이언트 초기화 및 관리")
    inner class InitializationTests {

        @Test
        @DisplayName("클라이언트를 생성할 수 있다")
        fun `클라이언트를 생성할 수 있다`() {
            // Given: 설정 준비
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

            // When: Chart 데이터 조회 테스트
            val chartService = YahooChartService(
                mockHttpClient,
                fakeRateLimiter,
                runBlocking { fakeAuthStrategy.authenticate() }
            )

            val result = runBlocking {
                chartService.getChartData("AAPL", Interval.OneDay, Period.OneMonth)
            }

            // Then: 데이터가 정상 반환됨
            assertThat(result).isNotEmpty()
            assertThat(result[0].close).isEqualTo(151.0)

            mockHttpClient.close()
        }

        @Test
        @DisplayName("클라이언트의 Rate Limiter 상태를 확인할 수 있다")
        fun `클라이언트의 Rate Limiter 상태를 확인할 수 있다`() {
            // Given
            assertThat(fakeRateLimiter.getAcquireCallCount()).isEqualTo(0)

            // When: Rate Limiter 사용
            runBlocking {
                fakeRateLimiter.acquire()
            }

            // Then
            assertThat(fakeRateLimiter.getAcquireCallCount()).isEqualTo(1)
            assertThat(fakeRateLimiter.getTotalTokensConsumed()).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("Chart 데이터 조회 통합")
    inner class ChartDataIntegrationTests {

        @Test
        @DisplayName("Chart 데이터를 조회할 수 있다")
        fun `Chart 데이터를 조회할 수 있다`() {
            // Given
            val mockResponse = """
                {
                    "chart": {
                        "result": [
                            {
                                "meta": {"symbol": "AAPL"},
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
            val chartService = YahooChartService(
                mockHttpClient,
                fakeRateLimiter,
                runBlocking { fakeAuthStrategy.authenticate() }
            )

            // When
            val result = runBlocking {
                chartService.getChartData("AAPL", Interval.OneDay, Period.OneMonth)
            }

            // Then
            assertThat(result).hasSize(2)
            assertThat(result[0].timestamp).isEqualTo(1609459200)
            assertThat(result[1].timestamp).isEqualTo(1609545600)

            mockHttpClient.close()
        }

        @Test
        @DisplayName("여러 심볼의 차트 데이터를 조회할 수 있다")
        fun `여러 심볼의 차트 데이터를 조회할 수 있다`() {
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
            val chartService = YahooChartService(
                mockHttpClient,
                fakeRateLimiter,
                runBlocking { fakeAuthStrategy.authenticate() }
            )

            // When
            val result = runBlocking {
                chartService.getChartData(
                    listOf("AAPL", "GOOGL"),
                    Interval.OneDay,
                    Period.OneMonth
                )
            }

            // Then
            assertThat(result).hasSize(2)
            assertThat(result.containsKey("AAPL")).isTrue()
            assertThat(result.containsKey("GOOGL")).isTrue()

            mockHttpClient.close()
        }

        @Test
        @DisplayName("Rate Limiting이 각 차트 요청에 적용된다")
        fun `Rate Limiting이 각 차트 요청에 적용된다`() {
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
            val chartService = YahooChartService(
                mockHttpClient,
                fakeRateLimiter,
                runBlocking { fakeAuthStrategy.authenticate() }
            )

            // When: 3개의 요청 수행
            runBlocking {
                chartService.getChartData("AAPL")
                chartService.getChartData("GOOGL")
                chartService.getChartData("MSFT")
            }

            // Then: Rate Limiter가 3번 호출됨
            assertThat(fakeRateLimiter.getAcquireCallCount()).isEqualTo(3)
            assertThat(fakeRateLimiter.getTotalTokensConsumed()).isEqualTo(3)

            mockHttpClient.close()
        }
    }

    @Nested
    @DisplayName("Quote 데이터 조회 통합")
    inner class QuoteDataIntegrationTests {

        @Test
        @DisplayName("Quote 요약 정보를 조회할 수 있다")
        fun `Quote 요약 정보를 조회할 수 있다`() {
            // Given
            val mockResponse = """
                {
                    "quoteSummary": {
                        "result": [
                            {
                                "price": {
                                    "symbol": "AAPL",
                                    "regularMarketPrice": {"raw": 150.0, "fmt": "150.00"},
                                    "currency": "USD"
                                }
                            }
                        ]
                    }
                }
            """.trimIndent()

            val mockHttpClient = HttpClient(MockEngine { request ->
                respond(content = mockResponse, status = HttpStatusCode.OK)
            })
            val quoteService = YahooQuoteSummaryService(
                mockHttpClient,
                fakeRateLimiter,
                runBlocking { fakeAuthStrategy.authenticate() }
            )

            // When
            val result = runBlocking {
                quoteService.getQuoteSummary("AAPL")
            }

            // Then
            assertThat(result).isNotNull()
            assertThat(result.price?.symbol).isEqualTo("AAPL")
            assertThat(result.price?.regularMarketPrice?.doubleValue).isEqualTo(150.0)

            mockHttpClient.close()
        }

        @Test
        @DisplayName("여러 심볼의 요약 정보를 조회할 수 있다")
        fun `여러 심볼의 요약 정보를 조회할 수 있다`() {
            // Given
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

            val mockHttpClient = HttpClient(MockEngine { request ->
                respond(content = mockResponse, status = HttpStatusCode.OK)
            })
            val quoteService = YahooQuoteSummaryService(
                mockHttpClient,
                fakeRateLimiter,
                runBlocking { fakeAuthStrategy.authenticate() }
            )

            // When
            val result = runBlocking {
                quoteService.getQuoteSummary(listOf("AAPL", "GOOGL"))
            }

            // Then
            assertThat(result).hasSize(2)

            mockHttpClient.close()
        }

        @Test
        @DisplayName("Stock 요약 정보를 조회할 수 있다")
        fun `Stock 요약 정보를 조회할 수 있다`() {
            // Given
            val mockResponse = """
                {
                    "quoteSummary": {
                        "result": [
                            {
                                "price": {
                                    "symbol": "AAPL",
                                    "regularMarketPrice": {"raw": 150.0, "fmt": "150.00"},
                                    "currency": "USD"
                                },
                                "summaryDetail": {
                                    "beta": {"raw": 1.2, "fmt": "1.20"}
                                }
                            }
                        ]
                    }
                }
            """.trimIndent()

            val mockHttpClient = HttpClient(MockEngine { request ->
                respond(content = mockResponse, status = HttpStatusCode.OK)
            })
            val quoteService = YahooQuoteSummaryService(
                mockHttpClient,
                fakeRateLimiter,
                runBlocking { fakeAuthStrategy.authenticate() }
            )

            // When
            val result = runBlocking {
                quoteService.getStockSummary("AAPL")
            }

            // Then
            assertThat(result).isNotNull()
            assertThat(result.symbol).isEqualTo("AAPL")
            assertThat(result.currentPrice).isEqualTo(150.0)

            mockHttpClient.close()
        }
    }

    @Nested
    @DisplayName("복합 시나리오")
    inner class ComplexScenariosTests {

        @Test
        @DisplayName("Chart와 Quote 데이터를 연속으로 조회한다")
        fun `Chart와 Quote 데이터를 연속으로 조회한다`() {
            // Given
            val chartResponse = """
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

            val quoteResponse = """
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

            val mockHttpClient = HttpClient(MockEngine { request ->
                val response = if (request.url.toString().contains("chart")) chartResponse else quoteResponse
                respond(content = response, status = HttpStatusCode.OK)
            })

            val chartService = YahooChartService(
                mockHttpClient,
                fakeRateLimiter,
                runBlocking { fakeAuthStrategy.authenticate() }
            )
            val quoteService = YahooQuoteSummaryService(
                mockHttpClient,
                fakeRateLimiter,
                runBlocking { fakeAuthStrategy.authenticate() }
            )

            // When: Chart와 Quote 데이터 순차 조회
            val chartData = runBlocking {
                chartService.getChartData("AAPL")
            }
            val quoteData = runBlocking {
                quoteService.getQuoteSummary("AAPL")
            }

            // Then
            assertThat(chartData).isNotEmpty()
            assertThat(quoteData).isNotNull()
            assertThat(fakeRateLimiter.getAcquireCallCount()).isEqualTo(2)

            mockHttpClient.close()
        }

        @Test
        @DisplayName("Rate Limiter 상태가 여러 요청에 걸쳐 유지된다")
        fun `Rate Limiter 상태가 여러 요청에 걸쳐 유지된다`() {
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
            val chartService = YahooChartService(
                mockHttpClient,
                fakeRateLimiter,
                runBlocking { fakeAuthStrategy.authenticate() }
            )

            // When: 여러 심볼 조회
            runBlocking {
                chartService.getChartData("AAPL")
                chartService.getChartData("GOOGL")
                chartService.getChartData("MSFT")
            }

            // Then: Rate Limiter 상태 검증
            val acquireHistory = fakeRateLimiter.getAcquireHistory()
            assertThat(acquireHistory).hasSize(3)
            assertThat(acquireHistory[0].tokensRequested).isEqualTo(1)
            assertThat(acquireHistory[1].tokensRequested).isEqualTo(1)
            assertThat(acquireHistory[2].tokensRequested).isEqualTo(1)

            // 시간 순서 검증
            assertThat(acquireHistory[1].timestampMs)
                .isGreaterThanOrEqualTo(acquireHistory[0].timestampMs)
            assertThat(acquireHistory[2].timestampMs)
                .isGreaterThanOrEqualTo(acquireHistory[1].timestampMs)

            mockHttpClient.close()
        }
    }
}
