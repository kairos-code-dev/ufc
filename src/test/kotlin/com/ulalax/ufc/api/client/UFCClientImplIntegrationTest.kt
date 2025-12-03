package com.ulalax.ufc.api.client

import com.ulalax.ufc.infrastructure.ratelimit.RateLimitConfig
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitingSettings
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

/**
 * UFCClientImpl 통합 테스트
 *
 * UFCClientImpl이 내부 서비스들과 정상적으로 통합되는지 확인합니다.
 */
@Tag("integration")
@DisplayName("UFCClientImpl 통합 테스트")
internal class UFCClientImplIntegrationTest {

    @Test
    @DisplayName("Rate Limiting 설정을 관리할 수 있어야 한다")
    fun testManageRateLimitingSettings() {
        val config = UFCClientConfig(
            fredApiKey = "test-key",
            rateLimitingSettings = RateLimitingSettings(
                yahoo = RateLimitConfig(
                    capacity = 50,
                    refillRate = 50
                ),
                fred = RateLimitConfig(
                    capacity = 10,
                    refillRate = 10
                )
            )
        )

        assertThat(config.rateLimitingSettings.yahoo.capacity).isNotEqualTo(0)
        assertThat(config.rateLimitingSettings.fred.capacity).isNotEqualTo(0)
    }

    @Test
    @DisplayName("FRED API 키를 옵션으로 설정할 수 있어야 한다")
    fun testFredApiKeyConfiguration() {
        val configWithKey = UFCClientConfig(fredApiKey = "my-key")
        val configWithoutKey = UFCClientConfig()

        assertThat(configWithKey.fredApiKey).isEqualTo("my-key")
        assertThat(configWithoutKey.fredApiKey).isNull()
    }

    @Test
    @DisplayName("기본 Rate Limiting 설정을 사용할 수 있어야 한다")
    fun testDefaultRateLimitingSettings() {
        val config = UFCClientConfig()

        assertThat(config.rateLimitingSettings.yahoo.capacity).isNotEqualTo(0)
        assertThat(config.rateLimitingSettings.yahoo.enabled).isEqualTo(true)
    }
}
