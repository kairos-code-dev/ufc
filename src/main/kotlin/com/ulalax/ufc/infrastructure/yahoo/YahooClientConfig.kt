package com.ulalax.ufc.infrastructure.yahoo

import com.ulalax.ufc.infrastructure.common.ratelimit.RateLimitConfig
import com.ulalax.ufc.infrastructure.common.ratelimit.RateLimitingSettings

/**
 * Yahoo 클라이언트 설정
 *
 * Yahoo Finance API 클라이언트의 동작을 제어하는 설정값들을 정의합니다.
 *
 * @property connectTimeoutMs HTTP 연결 타임아웃 (밀리초)
 * @property requestTimeoutMs HTTP 요청 타임아웃 (밀리초)
 * @property enableLogging HTTP 요청/응답 로깅 활성화 여부
 * @property rateLimitConfig Rate Limit 설정 (기본값: Yahoo 권장 50 RPS)
 */
data class YahooClientConfig(
    val connectTimeoutMs: Long = 30_000,
    val requestTimeoutMs: Long = 60_000,
    val enableLogging: Boolean = false,
    val rateLimitConfig: RateLimitConfig = RateLimitingSettings.yahooDefault(),
)
