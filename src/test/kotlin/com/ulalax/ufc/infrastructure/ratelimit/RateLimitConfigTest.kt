package com.ulalax.ufc.infrastructure.ratelimit

import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Tag

/**
 * RateLimitConfig 단위 테스트
 *
 * 설정 검증, 기본값, 계산된 속성 등을 검증합니다.
 */
@Tag("unit")
@DisplayName("RateLimitConfig 테스트")
internal class RateLimitConfigTest {

    @Test
    @DisplayName("기본값 설정: 기본 생성자로 생성 시 기본값 적용")
    fun testDefaultValues() {
        val config = RateLimitConfig()

        assertThat(config.capacity).isEqualTo(50)
        assertThat(config.refillRate).isEqualTo(50)
        assertThat(config.enabled).isTrue()
        assertThat(config.waitTimeoutMillis).isEqualTo(60000L)
    }

    @Test
    @DisplayName("커스텀 설정: 모든 필드를 커스텀으로 설정 가능")
    fun testCustomValues() {
        val config = RateLimitConfig(
            capacity = 100,
            refillRate = 25,
            enabled = false,
            waitTimeoutMillis = 120000L
        )

        assertThat(config.capacity).isEqualTo(100)
        assertThat(config.refillRate).isEqualTo(25)
        assertThat(config.enabled).isFalse()
        assertThat(config.waitTimeoutMillis).isEqualTo(120000L)
    }

    @Test
    @DisplayName("검증: capacity가 0일 때 예외 발생")
    fun testCapacityValidation() {
        assertThatThrownBy {
            RateLimitConfig(capacity = 0)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("검증: capacity가 음수일 때 예외 발생")
    fun testCapacityNegativeValidation() {
        assertThatThrownBy {
            RateLimitConfig(capacity = -1)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("검증: refillRate가 0일 때 예외 발생")
    fun testRefillRateValidation() {
        assertThatThrownBy {
            RateLimitConfig(refillRate = 0)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("검증: refillRate가 음수일 때 예외 발생")
    fun testRefillRateNegativeValidation() {
        assertThatThrownBy {
            RateLimitConfig(refillRate = -1)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("검증: waitTimeoutMillis가 0일 때 예외 발생")
    fun testWaitTimeoutValidation() {
        assertThatThrownBy {
            RateLimitConfig(waitTimeoutMillis = 0L)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("검증: waitTimeoutMillis가 음수일 때 예외 발생")
    fun testWaitTimeoutNegativeValidation() {
        assertThatThrownBy {
            RateLimitConfig(waitTimeoutMillis = -1L)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("계산: refillPeriodSeconds 계산")
    fun testRefillPeriodSeconds() {
        // refillRate = 50이면 1개 토큰당 1/50 = 0.02초
        val config = RateLimitConfig(capacity = 50, refillRate = 50)
        assertThat(config.refillPeriodSeconds).isCloseTo(0.02, within(0.001))
    }

    @Test
    @DisplayName("계산: fullRefillSeconds 계산")
    fun testFullRefillSeconds() {
        // capacity = 50, refillRate = 50이면 50/50 = 1초
        val config = RateLimitConfig(capacity = 50, refillRate = 50)
        assertThat(config.fullRefillSeconds).isCloseTo(1.0, within(0.001))
    }

    @Test
    @DisplayName("계산: fullRefillSeconds 계산 (느린 리필)")
    fun testFullRefillSecondsSlowRefill() {
        // capacity = 100, refillRate = 10이면 100/10 = 10초
        val config = RateLimitConfig(capacity = 100, refillRate = 10)
        assertThat(config.fullRefillSeconds).isCloseTo(10.0, within(0.001))
    }

    @Test
    @DisplayName("Copy: copy()로 일부 필드만 변경")
    fun testCopy() {
        val original = RateLimitConfig(
            capacity = 50,
            refillRate = 50,
            enabled = true,
            waitTimeoutMillis = 60000L
        )

        val modified = original.copy(capacity = 100, enabled = false)

        assertThat(modified.capacity).isEqualTo(100)
        assertThat(modified.refillRate).isEqualTo(50)
        assertThat(modified.enabled).isFalse()
        assertThat(modified.waitTimeoutMillis).isEqualTo(60000L)
    }
}

/**
 * RateLimitingSettings 단위 테스트
 *
 * 여러 API 설정을 관리하는 클래스를 검증합니다.
 */
@DisplayName("RateLimitingSettings 테스트")
internal class RateLimitingSettingsTest {

    @Test
    @DisplayName("기본값 설정: 기본 생성자로 생성 시 각 API별 기본값 적용")
    fun testDefaultSettings() {
        val settings = RateLimitingSettings()

        assertThat(settings.yahoo.capacity).isEqualTo(50)
        assertThat(settings.yahoo.refillRate).isEqualTo(50)
        assertThat(settings.fred.capacity).isEqualTo(10)
        assertThat(settings.fred.refillRate).isEqualTo(10)
    }

    @Test
    @DisplayName("커스텀 설정: 각 API별 커스텀 설정 가능")
    fun testCustomSettings() {
        val yahooConfig = RateLimitConfig(capacity = 100, refillRate = 100)
        val fredConfig = RateLimitConfig(capacity = 20, refillRate = 20)

        val settings = RateLimitingSettings(yahoo = yahooConfig, fred = fredConfig)

        assertThat(settings.yahoo.capacity).isEqualTo(100)
        assertThat(settings.yahoo.refillRate).isEqualTo(100)
        assertThat(settings.fred.capacity).isEqualTo(20)
        assertThat(settings.fred.refillRate).isEqualTo(20)
    }

    @Test
    @DisplayName("Yahoo 설정: Yahoo Finance API 설정 검증")
    fun testYahooSettings() {
        val settings = RateLimitingSettings()

        assertThat(settings.yahoo.enabled).isTrue()
        assertThat(settings.yahoo.capacity).isEqualTo(50)
        assertThat(settings.yahoo.refillRate).isEqualTo(50)
    }

    @Test
    @DisplayName("FRED 설정: FRED API 설정 검증")
    fun testFredSettings() {
        val settings = RateLimitingSettings()

        assertThat(settings.fred.enabled).isTrue()
        assertThat(settings.fred.capacity).isEqualTo(10)
        assertThat(settings.fred.refillRate).isEqualTo(10)
    }

    @Test
    @DisplayName("Copy: copy()로 일부 설정만 변경")
    fun testCopy() {
        val original = RateLimitingSettings()

        val modified = original.copy(
            yahoo = original.yahoo.copy(capacity = 200)
        )

        assertThat(modified.yahoo.capacity).isEqualTo(200)
        assertThat(modified.fred.capacity).isEqualTo(10)  // 변경 없음
    }
}
