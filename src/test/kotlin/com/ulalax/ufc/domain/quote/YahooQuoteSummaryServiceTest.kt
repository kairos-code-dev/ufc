package com.ulalax.ufc.domain.quote

import com.ulalax.ufc.exception.ApiException
import com.ulalax.ufc.exception.ErrorCode
import com.ulalax.ufc.exception.UfcException
import com.ulalax.ufc.fakes.FakeRateLimiter
import com.ulalax.ufc.internal.yahoo.auth.AuthResult
import com.ulalax.ufc.fakes.FakeAuthStrategy
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
 * YahooQuoteSummaryService 스펙 스타일 단위 테스트
 *
 * 클래식 TDD와 Specification-based testing을 적용합니다:
 * - @Nested로 테스트 그룹화
 * - @DisplayName으로 명확한 명세화
 * - Given-When-Then 구조
 * - Fake 객체를 통한 의존성 격리
 * - 상태 검증(State Verification)
 */
@Tag("unit")
@DisplayName("YahooQuoteSummaryService - Yahoo Finance 주식 요약 정보 조회 서비스")
internal class YahooQuoteSummaryServiceTest {

    private lateinit var fakeRateLimiter: FakeRateLimiter
    private lateinit var fakeAuthStrategy: FakeAuthStrategy
    private lateinit var authResult: AuthResult
    private lateinit var service: YahooQuoteSummaryService

    @BeforeEach
    fun setUp() {
        // Fake 객체 초기화
        fakeRateLimiter = FakeRateLimiter("QuoteSummaryService")
        fakeAuthStrategy = FakeAuthStrategy()

        // AuthResult 생성
        authResult = runBlocking {
            fakeAuthStrategy.authenticate()
        }
    }

    @AfterEach
    fun tearDown() {
        fakeAuthStrategy.reset()
        fakeRateLimiter.reset()
    }

    @Nested
    @DisplayName("getQuoteSummary() - 주식 요약 정보 조회")
    inner class GetQuoteSummaryTests {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("단일 심볼의 요약 정보를 조회한다")
            fun `단일 심볼의 요약 정보를 조회한다`() {
                // Given
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
                                        "beta": {"raw": 1.2, "fmt": "1.20"},
                                        "trailingPE": {"raw": 25.5, "fmt": "25.50"}
                                    }
                                }
                            ]
                        }
                    }
                """.trimIndent()

                val mockHttpClient = HttpClient(MockEngine { request ->
                    respond(content = mockResponse, status = HttpStatusCode.OK)
                })
                val service = YahooQuoteSummaryService(mockHttpClient, fakeRateLimiter, authResult)

                // When
                val result = runBlocking {
                    service.getQuoteSummary("AAPL")
                }

                // Then
                assertThat(result).isNotNull()
                assertThat(result.price?.symbol).isEqualTo("AAPL")
                assertThat(result.price?.regularMarketPrice?.doubleValue).isEqualTo(150.0)
                assertThat(fakeRateLimiter.getAcquireCallCount()).isEqualTo(1)

                mockHttpClient.close()
            }

            @Test
            @DisplayName("특정 모듈만 선택하여 조회한다")
            fun `특정 모듈만 선택하여 조회한다`() {
                // Given
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

                val mockHttpClient = HttpClient(MockEngine { request ->
                    respond(content = mockResponse, status = HttpStatusCode.OK)
                })
                val service = YahooQuoteSummaryService(mockHttpClient, fakeRateLimiter, authResult)

                // When
                val result = runBlocking {
                    service.getQuoteSummary("AAPL", listOf("price", "summaryDetail"))
                }

                // Then
                assertThat(result).isNotNull()
                assertThat(result.price?.regularMarketPrice?.doubleValue).isEqualTo(150.0)

                mockHttpClient.close()
            }

            @Test
            @DisplayName("여러 심볼의 요약 정보를 조회한다")
            fun `여러 심볼의 요약 정보를 조회한다`() {
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
                val service = YahooQuoteSummaryService(mockHttpClient, fakeRateLimiter, authResult)

                // When
                val result = runBlocking {
                    service.getQuoteSummary(listOf("AAPL", "GOOGL"))
                }

                // Then
                assertThat(result.size).isEqualTo(2)
                assertThat(fakeRateLimiter.getAcquireCallCount()).isEqualTo(2)

                mockHttpClient.close()
            }
        }

        @Nested
        @DisplayName("에러 케이스")
        inner class ErrorCases {

            @Test
            @DisplayName("빈 심볼로 조회 시 UfcException을 발생시킨다")
            fun `빈 심볼로 조회 시 예외를 발생시킨다`() {
                // Given
                val mockHttpClient = HttpClient(MockEngine { request ->
                    respond(content = "", status = HttpStatusCode.OK)
                })
                val service = YahooQuoteSummaryService(mockHttpClient, fakeRateLimiter, authResult)

                // When & Then
                assertThatThrownBy {
                    runBlocking {
                        service.getQuoteSummary("")
                    }
                }.isInstanceOf(UfcException::class.java)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SYMBOL)

                mockHttpClient.close()
            }

            @Test
            @DisplayName("너무 긴 심볼로 조회 시 UfcException을 발생시킨다")
            fun `너무 긴 심볼로 조회 시 예외를 발생시킨다`() {
                // Given
                val mockHttpClient = HttpClient(MockEngine { request ->
                    respond(content = "", status = HttpStatusCode.OK)
                })
                val service = YahooQuoteSummaryService(mockHttpClient, fakeRateLimiter, authResult)

                // When & Then
                assertThatThrownBy {
                    runBlocking {
                        service.getQuoteSummary("VERYLONGSYMBOLTHATEXCEEDSLIMIT")
                    }
                }.isInstanceOf(UfcException::class.java)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SYMBOL)

                mockHttpClient.close()
            }

            @Test
            @DisplayName("HTTP 오류 응답을 받을 경우 ApiException을 발생시킨다")
            fun `HTTP 오류 응답을 받을 경우 예외를 발생시킨다`() {
                // Given
                val mockHttpClient = HttpClient(MockEngine { request ->
                    respond(content = "Not Found", status = HttpStatusCode.NotFound)
                })
                val service = YahooQuoteSummaryService(mockHttpClient, fakeRateLimiter, authResult)

                // When & Then
                assertThatThrownBy {
                    runBlocking {
                        service.getQuoteSummary("AAPL")
                    }
                }.isInstanceOf(ApiException::class.java)

                mockHttpClient.close()
            }

            @Test
            @DisplayName("API 에러 응답을 받을 경우 ApiException을 발생시킨다")
            fun `API 에러 응답을 받을 경우 예외를 발생시킨다`() {
                // Given
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

                val mockHttpClient = HttpClient(MockEngine { request ->
                    respond(content = mockResponse, status = HttpStatusCode.OK)
                })
                val service = YahooQuoteSummaryService(mockHttpClient, fakeRateLimiter, authResult)

                // When & Then
                assertThatThrownBy {
                    runBlocking {
                        service.getQuoteSummary("AAPL")
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
                        "quoteSummary": {
                            "result": []
                        }
                    }
                """.trimIndent()

                val mockHttpClient = HttpClient(MockEngine { request ->
                    respond(content = mockResponse, status = HttpStatusCode.OK)
                })
                val service = YahooQuoteSummaryService(mockHttpClient, fakeRateLimiter, authResult)

                // When & Then
                assertThatThrownBy {
                    runBlocking {
                        service.getQuoteSummary("AAPL")
                    }
                }.isInstanceOf(UfcException::class.java)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_NOT_FOUND)

                mockHttpClient.close()
            }

            @Test
            @DisplayName("빈 모듈 목록으로 요청 시 UfcException을 발생시킨다")
            fun `빈 모듈 목록으로 요청 시 예외를 발생시킨다`() {
                // Given
                val mockHttpClient = HttpClient(MockEngine { request ->
                    respond(content = "", status = HttpStatusCode.OK)
                })
                val service = YahooQuoteSummaryService(mockHttpClient, fakeRateLimiter, authResult)

                // When & Then
                assertThatThrownBy {
                    runBlocking {
                        service.getQuoteSummary("AAPL", emptyList())
                    }
                }.isInstanceOf(UfcException::class.java)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PARAMETER)

                mockHttpClient.close()
            }

            @Test
            @DisplayName("빈 심볼 목록으로 요청 시 UfcException을 발생시킨다")
            fun `빈 심볼 목록으로 요청 시 예외를 발생시킨다`() {
                // Given
                val mockHttpClient = HttpClient(MockEngine { request ->
                    respond(content = "", status = HttpStatusCode.OK)
                })
                val service = YahooQuoteSummaryService(mockHttpClient, fakeRateLimiter, authResult)

                // When & Then
                assertThatThrownBy {
                    runBlocking {
                        service.getQuoteSummary(emptyList())
                    }
                }.isInstanceOf(UfcException::class.java)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PARAMETER)

                mockHttpClient.close()
            }
        }
    }

    @Nested
    @DisplayName("getStockSummary() - 정규화된 요약 정보 조회")
    inner class GetStockSummaryTests {

        @Test
        @DisplayName("정규화된 StockSummary를 반환한다")
        fun `정규화된 StockSummary를 반환한다`() {
            // Given
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

            val mockHttpClient = HttpClient(MockEngine { request ->
                respond(content = mockResponse, status = HttpStatusCode.OK)
            })
            val service = YahooQuoteSummaryService(mockHttpClient, fakeRateLimiter, authResult)

            // When
            val result = runBlocking {
                service.getStockSummary("AAPL")
            }

            // Then
            assertThat(result).isNotNull()
            assertThat(result.symbol).isEqualTo("AAPL")
            assertThat(result.currentPrice).isEqualTo(150.0)
            assertThat(result.currency).isEqualTo("USD")
            assertThat(result.beta).isEqualTo(1.2)

            mockHttpClient.close()
        }
    }

    @Nested
    @DisplayName("getRawQuoteSummary() - 원본 응답 조회")
    inner class GetRawQuoteSummaryTests {

        @Test
        @DisplayName("원본 QuoteSummaryResponse를 반환한다")
        fun `원본 QuoteSummaryResponse를 반환한다`() {
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
            val service = YahooQuoteSummaryService(mockHttpClient, fakeRateLimiter, authResult)

            // When
            val result = runBlocking {
                service.getRawQuoteSummary("AAPL")
            }

            // Then
            assertThat(result).isNotNull()
            assertThat(result.quoteSummary.result?.size).isEqualTo(1)

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
            val service = YahooQuoteSummaryService(mockHttpClient, fakeRateLimiter, authResult)

            // When: 첫 번째 호출
            runBlocking {
                service.getQuoteSummary("AAPL")
            }
            assertThat(fakeRateLimiter.getAcquireCallCount()).isEqualTo(1)

            // When: 두 번째 호출
            runBlocking {
                service.getQuoteSummary("AAPL")
            }

            // Then
            assertThat(fakeRateLimiter.getAcquireCallCount()).isEqualTo(2)
            assertThat(fakeRateLimiter.getTotalTokensConsumed()).isEqualTo(2)

            mockHttpClient.close()
        }
    }
}
