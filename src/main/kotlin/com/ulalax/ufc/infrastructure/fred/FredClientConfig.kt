package com.ulalax.ufc.infrastructure.fred

import com.ulalax.ufc.infrastructure.common.ratelimit.RateLimitConfig
import com.ulalax.ufc.infrastructure.common.ratelimit.RateLimitingSettings

/**
 * FRED 클라이언트 설정
 *
 * HTTP 연결 타임아웃, 로깅 등의 설정을 관리합니다.
 *
 * @property connectTimeoutMs 연결 타임아웃 (밀리초), 기본값: 30초
 * @property requestTimeoutMs 요청 타임아웃 (밀리초), 기본값: 60초
 * @property enableLogging HTTP 로깅 활성화 여부, 기본값: false
 * @property rateLimitConfig Rate Limit 설정 (기본값: FRED 권장 2 RPS)
 */
data class FredClientConfig(
    val connectTimeoutMs: Long = 30_000,
    val requestTimeoutMs: Long = 60_000,
    val enableLogging: Boolean = false,
    val rateLimitConfig: RateLimitConfig = RateLimitingSettings.fredDefault(),
)
