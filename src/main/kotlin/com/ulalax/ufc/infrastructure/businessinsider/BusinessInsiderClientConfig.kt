package com.ulalax.ufc.infrastructure.businessinsider

import com.ulalax.ufc.infrastructure.common.ratelimit.RateLimitConfig
import com.ulalax.ufc.infrastructure.common.ratelimit.RateLimitingSettings

/**
 * Business Insider 클라이언트 설정
 *
 * @property connectTimeoutMs 연결 타임아웃 (밀리초, 기본값: 30초)
 * @property requestTimeoutMs 요청 타임아웃 (밀리초, 기본값: 60초)
 * @property enableLogging HTTP 로깅 활성화 여부 (기본값: false)
 * @property rateLimitConfig Rate Limit 설정 (기본값: Business Insider 권장 10 RPS)
 */
data class BusinessInsiderClientConfig(
    val connectTimeoutMs: Long = 30_000,
    val requestTimeoutMs: Long = 60_000,
    val enableLogging: Boolean = false,
    val rateLimitConfig: RateLimitConfig = RateLimitingSettings.businessInsiderDefault()
)
