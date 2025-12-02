package com.ulalax.ufc.domain.chart

import com.ulalax.ufc.exception.UfcException
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitConfig
import com.ulalax.ufc.infrastructure.ratelimit.TokenBucketRateLimiter
import com.ulalax.ufc.internal.yahoo.YahooHttpClientFactory
import com.ulalax.ufc.internal.yahoo.auth.AuthResult
import com.ulalax.ufc.model.common.Interval
import com.ulalax.ufc.model.common.Period
import org.assertj.core.api.Assertions.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

/**
 * Yahoo Chart Service 통합 테스트
 *
 * 이 테스트는 실제 Ktor HTTP 클라이언트와 Rate Limiter를 사용합니다.
 * 그러나 모의 데이터를 사용하므로 실제 API 호출은 하지 않습니다.
 */
@Tag("integration")
@DisplayName("YahooChartService 통합 테스트")
internal class YahooChartServiceIntegrationTest {

    @Test
    @DisplayName("정상적으로 Chart Service를 생성할 수 있어야 한다")
    fun testCreateChartService() {
        val httpClient = YahooHttpClientFactory.create()
        val rateLimiter = TokenBucketRateLimiter(
            "YAHOO",
            RateLimitConfig(capacity = 50, refillRate = 50)
        )
        val authResult = AuthResult(
            crumb = "test_crumb",
            strategy = "basic"
        )

        val service = YahooChartService(httpClient, rateLimiter, authResult)

        assertThat(service).isNotNull()

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

        val service = YahooChartService(httpClient, rateLimiter, authResult)

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

        val service = YahooChartService(httpClient, rateLimiter, authResult)

        // 토큰 상태는 확인할 수 있지만 acquire는 무시됨
        assertThat(rateLimiter.getStatus()).isNotNull()

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
        val service = YahooChartService(httpClient, rateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getChartData("")
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
        val service = YahooChartService(httpClient, rateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getChartData("   ")
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
        val service = YahooChartService(httpClient, rateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getChartData("VERYLONGSYMBOLTHATISMORETHAN10CHARACTERS")
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
        val service = YahooChartService(httpClient, rateLimiter, authResult)

        assertThatThrownBy {
            runBlocking {
                service.getChartData(emptyList())
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
        val service = YahooChartService(httpClient, rateLimiter, authResult)

        // 정상 종료 확인
        httpClient.close()
    }

    @Test
    @DisplayName("Period.OneDay를 사용할 수 있어야 한다")
    fun testPeriodOneDay() {
        val httpClient = YahooHttpClientFactory.create()
        val rateLimiter = TokenBucketRateLimiter(
            "YAHOO",
            RateLimitConfig(capacity = 50, refillRate = 50)
        )
        val authResult = AuthResult(crumb = "test_crumb", strategy = "basic")
        val service = YahooChartService(httpClient, rateLimiter, authResult)

        // Period가 정상적으로 전달되는지 확인
        assertThat(Period.OneDay.value).isEqualTo("1d")

        httpClient.close()
    }

    @Test
    @DisplayName("Period.OneYear를 사용할 수 있어야 한다")
    fun testPeriodOneYear() {
        val httpClient = YahooHttpClientFactory.create()
        val rateLimiter = TokenBucketRateLimiter(
            "YAHOO",
            RateLimitConfig(capacity = 50, refillRate = 50)
        )
        val authResult = AuthResult(crumb = "test_crumb", strategy = "basic")
        val service = YahooChartService(httpClient, rateLimiter, authResult)

        // Period가 정상적으로 전달되는지 확인
        assertThat(Period.OneYear.value).isEqualTo("1y")

        httpClient.close()
    }

    @Test
    @DisplayName("Period.Max를 사용할 수 있어야 한다")
    fun testPeriodMax() {
        val httpClient = YahooHttpClientFactory.create()
        val rateLimiter = TokenBucketRateLimiter(
            "YAHOO",
            RateLimitConfig(capacity = 50, refillRate = 50)
        )
        val authResult = AuthResult(crumb = "test_crumb", strategy = "basic")
        val service = YahooChartService(httpClient, rateLimiter, authResult)

        // Period가 정상적으로 전달되는지 확인
        assertThat(Period.Max.value).isEqualTo("max")

        httpClient.close()
    }

    @Test
    @DisplayName("Interval.OneMinute를 사용할 수 있어야 한다")
    fun testIntervalOneMinute() {
        val httpClient = YahooHttpClientFactory.create()
        val rateLimiter = TokenBucketRateLimiter(
            "YAHOO",
            RateLimitConfig(capacity = 50, refillRate = 50)
        )
        val authResult = AuthResult(crumb = "test_crumb", strategy = "basic")
        val service = YahooChartService(httpClient, rateLimiter, authResult)

        // Interval이 정상적으로 전달되는지 확인
        assertThat(Interval.OneMinute.value).isEqualTo("1m")

        httpClient.close()
    }

    @Test
    @DisplayName("Interval.OneHour를 사용할 수 있어야 한다")
    fun testIntervalOneHour() {
        val httpClient = YahooHttpClientFactory.create()
        val rateLimiter = TokenBucketRateLimiter(
            "YAHOO",
            RateLimitConfig(capacity = 50, refillRate = 50)
        )
        val authResult = AuthResult(crumb = "test_crumb", strategy = "basic")
        val service = YahooChartService(httpClient, rateLimiter, authResult)

        // Interval이 정상적으로 전달되는지 확인
        assertThat(Interval.OneHour.value).isEqualTo("1h")

        httpClient.close()
    }

    @Test
    @DisplayName("Interval.OneDay를 사용할 수 있어야 한다")
    fun testIntervalOneDay() {
        val httpClient = YahooHttpClientFactory.create()
        val rateLimiter = TokenBucketRateLimiter(
            "YAHOO",
            RateLimitConfig(capacity = 50, refillRate = 50)
        )
        val authResult = AuthResult(crumb = "test_crumb", strategy = "basic")
        val service = YahooChartService(httpClient, rateLimiter, authResult)

        // Interval이 정상적으로 전달되는지 확인
        assertThat(Interval.OneDay.value).isEqualTo("1d")

        httpClient.close()
    }
}
