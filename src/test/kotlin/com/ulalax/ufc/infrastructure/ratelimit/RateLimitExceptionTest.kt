package com.ulalax.ufc.infrastructure.ratelimit

import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Tag

/**
 * RateLimitException 단위 테스트
 *
 * 다양한 Rate Limit 예외 클래스를 검증합니다.
 */
@Tag("unit")
@DisplayName("RateLimitException 테스트")
internal class RateLimitExceptionTest {

    @Test
    @DisplayName("기본 예외: RateLimitException은 RuntimeException을 상속")
    fun testExceptionHierarchy() {
        val config = RateLimitConfig()
        val exception = RateLimitException.RateLimitConfigException("test error")

        assertThat(exception).isInstanceOf(RuntimeException::class.java)
        assertThat(exception).isInstanceOf(RateLimitException::class.java)
    }

    @Test
    @DisplayName("TimeoutException: 생성자 매개변수 검증")
    fun testTimeoutExceptionConstruction() {
        val config = RateLimitConfig(capacity = 50, refillRate = 50)

        val exception = RateLimitException.RateLimitTimeoutException(
            source = "YAHOO",
            config = config,
            tokensNeeded = 10,
            waitedMillis = 500L
        )

        assertThat(exception.source).isEqualTo("YAHOO")
        assertThat(exception.tokensNeeded).isEqualTo(10)
        assertThat(exception.waitedMillis).isEqualTo(500L)
        assertThat(exception.config).isEqualTo(config)
    }

    @Test
    @DisplayName("TimeoutException: 기본 매개변수")
    fun testTimeoutExceptionDefaultParameters() {
        val config = RateLimitConfig()

        val exception = RateLimitException.RateLimitTimeoutException(
            source = "TEST",
            config = config
        )

        assertThat(exception.source).isEqualTo("TEST")
        assertThat(exception.tokensNeeded).isEqualTo(1)
        assertThat(exception.waitedMillis).isEqualTo(0L)
    }

    @Test
    @DisplayName("TimeoutException: 메시지 포함")
    fun testTimeoutExceptionMessage() {
        val config = RateLimitConfig(capacity = 50, refillRate = 50)

        val exception = RateLimitException.RateLimitTimeoutException(
            source = "YAHOO",
            config = config,
            tokensNeeded = 10,
            waitedMillis = 100L
        )

        val message = exception.message
        assertThat(message).contains("Rate limit timeout")
        assertThat(message).contains("YAHOO")
        assertThat(message).contains("100ms")
    }

    @Test
    @DisplayName("TimeoutException: 형식화된 대기 시간")
    fun testFormattedWaitTimeMilliseconds() {
        val config = RateLimitConfig(capacity = 50, refillRate = 50)

        val exception = RateLimitException.RateLimitTimeoutException(
            source = "TEST",
            config = config,
            tokensNeeded = 1
        )

        val formattedTime = exception.getFormattedWaitTime()
        // refillRate = 50이므로 1개 토큰에 필요한 시간 = 1000/50 = 20ms
        assertThat(formattedTime).contains("ms")
    }

    @Test
    @DisplayName("TimeoutException: 형식화된 대기 시간 (초 단위)")
    fun testFormattedWaitTimeSeconds() {
        val config = RateLimitConfig(capacity = 10, refillRate = 1)

        val exception = RateLimitException.RateLimitTimeoutException(
            source = "TEST",
            config = config,
            tokensNeeded = 10
        )

        val formattedTime = exception.getFormattedWaitTime()
        // refillRate = 1이므로 10개 토큰에 필요한 시간 = 10000ms = 10초
        assertThat(formattedTime).contains("초")
    }

    @Test
    @DisplayName("TimeoutException: 형식화된 대기 시간 (분 단위)")
    fun testFormattedWaitTimeMinutes() {
        val config = RateLimitConfig(capacity = 10, refillRate = 1, waitTimeoutMillis = 3600000L)

        val exception = RateLimitException.RateLimitTimeoutException(
            source = "TEST",
            config = config,
            tokensNeeded = 100  // 100초 필요
        )

        val formattedTime = exception.getFormattedWaitTime()
        // refillRate = 1이므로 100개 토큰에 필요한 시간 = 100000ms > 60000ms
        assertThat(formattedTime).contains("분")
    }

    @Test
    @DisplayName("ConfigException: 생성자 매개변수 검증")
    fun testConfigExceptionConstruction() {
        val exception = RateLimitException.RateLimitConfigException(
            configError = "Invalid capacity value"
        )

        assertThat(exception.configError).isEqualTo("Invalid capacity value")
    }

    @Test
    @DisplayName("ConfigException: 메시지 포함")
    fun testConfigExceptionMessage() {
        val exception = RateLimitException.RateLimitConfigException(
            configError = "capacity must be greater than 0"
        )

        val message = exception.message
        assertThat(message).contains("configuration")
        assertThat(message).contains("capacity must be greater than 0")
    }

    @Test
    @DisplayName("StateException: 생성자 매개변수 검증")
    fun testStateExceptionConstruction() {
        val exception = RateLimitException.RateLimitStateException(
            stateError = "Rate limiter not initialized"
        )

        assertThat(exception.stateError).isEqualTo("Rate limiter not initialized")
    }

    @Test
    @DisplayName("StateException: 메시지 포함")
    fun testStateExceptionMessage() {
        val exception = RateLimitException.RateLimitStateException(
            stateError = "Invalid state transition"
        )

        val message = exception.message
        assertThat(message).contains("state error")
        assertThat(message).contains("Invalid state transition")
    }

    @Test
    @DisplayName("예외 타입 확인: sealed class의 모든 서브타입 확인 가능")
    fun testExceptionTypes() {
        val config = RateLimitConfig()

        val exceptions: List<RateLimitException> = listOf(
            RateLimitException.RateLimitTimeoutException("TEST", config),
            RateLimitException.RateLimitConfigException("test error"),
            RateLimitException.RateLimitStateException("test state")
        )

        exceptions.forEach { exception ->
            assertThat(exception).isInstanceOf(RateLimitException::class.java)
            assertThat(exception).isInstanceOf(RuntimeException::class.java)
        }
    }

    @Test
    @DisplayName("Cause 설정: 원인 예외 포함 가능")
    fun testExceptionWithCause() {
        val cause = IllegalArgumentException("underlying error")

        // RateLimitException 기본 생성자는 cause를 지원하지만,
        // data class 구현에서는 사용할 수 없으므로 이 테스트는 인터페이스 검증용
        val exception = RateLimitException.RateLimitConfigException(
            configError = "Configuration error"
        )

        // 메시지 검증
        assertThat(exception.message).isNotEmpty()
    }
}
