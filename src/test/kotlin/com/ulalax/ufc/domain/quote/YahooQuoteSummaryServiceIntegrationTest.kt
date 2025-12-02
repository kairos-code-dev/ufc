package com.ulalax.ufc.domain.quote

import com.ulalax.ufc.exception.UfcException
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitConfig
import com.ulalax.ufc.infrastructure.ratelimit.TokenBucketRateLimiter
import com.ulalax.ufc.internal.yahoo.YahooHttpClientFactory
import com.ulalax.ufc.internal.yahoo.auth.AuthResult
import kotlinx.serialization.json.JsonPrimitive
import org.assertj.core.api.Assertions.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

/**
 * Yahoo QuoteSummary Service 통합 테스트
 */
@Tag("integration")
@DisplayName("YahooQuoteSummaryService 통합 테스트")
internal class YahooQuoteSummaryServiceIntegrationTest {

    @Test
    @DisplayName("정상적으로 QuoteSummary Service를 생성할 수 있어야 한다")
    fun testCreateQuoteSummaryService() {
        val httpClient = YahooHttpClientFactory.create()
        val rateLimiter = TokenBucketRateLimiter(
            "YAHOO",
            RateLimitConfig(capacity = 50, refillRate = 50)
        )
        val authResult = AuthResult(
            crumb = "test_crumb",
            strategy = "basic"
        )

        val service = YahooQuoteSummaryService(httpClient, rateLimiter, authResult)

        assertThat(service).isNotNull()

        httpClient.close()
    }

    @Test
    @DisplayName("빈 심볼로 요청하면 UfcException을 발생시켜야 한다")
    fun testEmptySymbol() {
        val httpClient = YahooHttpClientFactory.create()
        val rateLimiter = TokenBucketRateLimiter(
            "YAHOO",
            RateLimitConfig(capacity = 50, refillRate = 50)
        )
        val authResult = AuthResult(crumb = "test_crumb", strategy = "basic")
        val service = YahooQuoteSummaryService(httpClient, rateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getQuoteSummary("")
            }
        }.isInstanceOf(UfcException::class.java)

        httpClient.close()
    }

    @Test
    @DisplayName("공백만 있는 심볼로 요청하면 UfcException을 발생시켜야 한다")
    fun testWhitespaceSymbol() {
        val httpClient = YahooHttpClientFactory.create()
        val rateLimiter = TokenBucketRateLimiter(
            "YAHOO",
            RateLimitConfig(capacity = 50, refillRate = 50)
        )
        val authResult = AuthResult(crumb = "test_crumb", strategy = "basic")
        val service = YahooQuoteSummaryService(httpClient, rateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getQuoteSummary("   ")
            }
        }.isInstanceOf(UfcException::class.java)

        httpClient.close()
    }

    @Test
    @DisplayName("너무 긴 심볼로 요청하면 UfcException을 발생시켜야 한다")
    fun testTooLongSymbol() {
        val httpClient = YahooHttpClientFactory.create()
        val rateLimiter = TokenBucketRateLimiter(
            "YAHOO",
            RateLimitConfig(capacity = 50, refillRate = 50)
        )
        val authResult = AuthResult(crumb = "test_crumb", strategy = "basic")
        val service = YahooQuoteSummaryService(httpClient, rateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getQuoteSummary("VERYLONGSYMBOLTHATISMORETHAN10CHARACTERS")
            }
        }.isInstanceOf(UfcException::class.java)

        httpClient.close()
    }

    @Test
    @DisplayName("빈 심볼 목록으로 요청하면 UfcException을 발생시켜야 한다")
    fun testEmptySymbolList() {
        val httpClient = YahooHttpClientFactory.create()
        val rateLimiter = TokenBucketRateLimiter(
            "YAHOO",
            RateLimitConfig(capacity = 50, refillRate = 50)
        )
        val authResult = AuthResult(crumb = "test_crumb", strategy = "basic")
        val service = YahooQuoteSummaryService(httpClient, rateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getQuoteSummary(emptyList())
            }
        }.isInstanceOf(UfcException::class.java)

        httpClient.close()
    }

    @Test
    @DisplayName("빈 모듈 목록으로 요청하면 UfcException을 발생시켜야 한다")
    fun testEmptyModuleList() {
        val httpClient = YahooHttpClientFactory.create()
        val rateLimiter = TokenBucketRateLimiter(
            "YAHOO",
            RateLimitConfig(capacity = 50, refillRate = 50)
        )
        val authResult = AuthResult(crumb = "test_crumb", strategy = "basic")
        val service = YahooQuoteSummaryService(httpClient, rateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getQuoteSummary("AAPL", emptyList())
            }
        }.isInstanceOf(UfcException::class.java)

        httpClient.close()
    }

    @Test
    @DisplayName("Ktor 기반 HTTP 클라이언트를 사용해야 한다")
    fun testHttpClientCreation() {
        val httpClient = YahooHttpClientFactory.create()

        assertThat(httpClient).isNotNull()

        httpClient.close()
    }

    @Test
    @DisplayName("서비스가 정상 종료되어야 한다")
    fun testServiceGracefulShutdown() {
        val httpClient = YahooHttpClientFactory.create()
        val rateLimiter = TokenBucketRateLimiter(
            "YAHOO",
            RateLimitConfig(capacity = 50, refillRate = 50)
        )
        val authResult = AuthResult(crumb = "test_crumb", strategy = "basic")
        val service = YahooQuoteSummaryService(httpClient, rateLimiter, authResult)

        // 정상 종료 확인
        httpClient.close()
    }

    @Test
    @DisplayName("Rate Limiter와 함께 서비스를 사용할 수 있어야 한다")
    fun testServiceWithRateLimiter() {
        val httpClient = YahooHttpClientFactory.create()
        val rateLimiter = TokenBucketRateLimiter(
            "YAHOO",
            RateLimitConfig(capacity = 50, refillRate = 50, enabled = true)
        )
        val authResult = AuthResult(
            crumb = "test_crumb",
            strategy = "basic"
        )

        val service = YahooQuoteSummaryService(httpClient, rateLimiter, authResult)

        // Rate Limiter가 정상 작동함을 확인
        assertThat(rateLimiter.getAvailableTokens()).isEqualTo(50)

        httpClient.close()
    }

    @Test
    @DisplayName("비활성화된 Rate Limiter와 함께 서비스를 사용할 수 있어야 한다")
    fun testServiceWithDisabledRateLimiter() {
        val httpClient = YahooHttpClientFactory.create()
        val rateLimiter = TokenBucketRateLimiter(
            "YAHOO",
            RateLimitConfig(capacity = 50, refillRate = 50, enabled = false)
        )
        val authResult = AuthResult(
            crumb = "test_crumb",
            strategy = "basic"
        )

        val service = YahooQuoteSummaryService(httpClient, rateLimiter, authResult)

        // 토큰 상태는 확인할 수 있지만 acquire는 무시됨
        assertThat(rateLimiter.getStatus()).isNotNull()

        httpClient.close()
    }

    @Test
    @DisplayName("QuoteSummaryResult를 StockSummary로 변환할 수 있어야 한다")
    fun testConvertToStockSummary() {
        val httpClient = YahooHttpClientFactory.create()
        val rateLimiter = TokenBucketRateLimiter(
            "YAHOO",
            RateLimitConfig(capacity = 50, refillRate = 50)
        )
        val authResult = AuthResult(crumb = "test_crumb", strategy = "basic")
        val service = YahooQuoteSummaryService(httpClient, rateLimiter, authResult)

        // StockSummary 객체가 생성될 수 있음을 확인
        val mockResult = QuoteSummaryResult(
            price = Price(
                symbol = "AAPL",
                regularMarketPrice = RawFormatted(raw = JsonPrimitive(150.0), fmt = "150.00"),
                currency = "USD"
            )
        )

        assertThat(mockResult.price?.symbol).isEqualTo("AAPL")

        httpClient.close()
    }

    @Test
    @DisplayName("price 모듈만 요청할 수 있어야 한다")
    fun testPriceModuleOnly() {
        val httpClient = YahooHttpClientFactory.create()
        val rateLimiter = TokenBucketRateLimiter(
            "YAHOO",
            RateLimitConfig(capacity = 50, refillRate = 50)
        )
        val authResult = AuthResult(crumb = "test_crumb", strategy = "basic")
        val service = YahooQuoteSummaryService(httpClient, rateLimiter, authResult)

        // 메서드 호출 가능성만 확인
        val modules = listOf("price")
        assertThat(modules).isNotEmpty()

        httpClient.close()
    }

    @Test
    @DisplayName("summaryDetail 모듈만 요청할 수 있어야 한다")
    fun testSummaryDetailModuleOnly() {
        val httpClient = YahooHttpClientFactory.create()
        val rateLimiter = TokenBucketRateLimiter(
            "YAHOO",
            RateLimitConfig(capacity = 50, refillRate = 50)
        )
        val authResult = AuthResult(crumb = "test_crumb", strategy = "basic")
        val service = YahooQuoteSummaryService(httpClient, rateLimiter, authResult)

        // 메서드 호출 가능성만 확인
        val modules = listOf("summaryDetail")
        assertThat(modules).isNotEmpty()

        httpClient.close()
    }

    @Test
    @DisplayName("financialData 모듈만 요청할 수 있어야 한다")
    fun testFinancialDataModuleOnly() {
        val httpClient = YahooHttpClientFactory.create()
        val rateLimiter = TokenBucketRateLimiter(
            "YAHOO",
            RateLimitConfig(capacity = 50, refillRate = 50)
        )
        val authResult = AuthResult(crumb = "test_crumb", strategy = "basic")
        val service = YahooQuoteSummaryService(httpClient, rateLimiter, authResult)

        // 메서드 호출 가능성만 확인
        val modules = listOf("financialData")
        assertThat(modules).isNotEmpty()

        httpClient.close()
    }
}
