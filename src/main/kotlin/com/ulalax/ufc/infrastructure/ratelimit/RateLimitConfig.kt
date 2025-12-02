package com.ulalax.ufc.infrastructure.ratelimit

import kotlinx.serialization.Serializable

/**
 * Rate Limiter 설정 데이터 클래스
 *
 * 각 API별 Rate Limiting 설정을 정의합니다.
 *
 * @property capacity 최대 토큰 수 (기본값: 50)
 * @property refillRate 초당 리필 토큰 수 (기본값: 50)
 * @property enabled Rate Limiter 활성화 여부 (기본값: true)
 * @property waitTimeoutMillis 토큰 대기 타임아웃 (밀리초, 기본값: 60000)
 *
 * @throws IllegalArgumentException capacity, refillRate, waitTimeoutMillis가 0 이하인 경우
 *
 * 사용 예시:
 * ```
 * // Yahoo Finance API 설정
 * val yahooConfig = RateLimitConfig(
 *     capacity = 50,
 *     refillRate = 50,
 *     enabled = true,
 *     waitTimeoutMillis = 60000L
 * )
 *
 * // FRED API 설정 (더 엄격함)
 * val fredConfig = RateLimitConfig(
 *     capacity = 10,
 *     refillRate = 10,
 *     enabled = true,
 *     waitTimeoutMillis = 120000L
 * )
 * ```
 */
@Serializable
data class RateLimitConfig(
    val capacity: Int = 50,
    val refillRate: Int = 50,
    val enabled: Boolean = true,
    val waitTimeoutMillis: Long = 60000L
) {
    init {
        require(capacity > 0) { "capacity은 0보다 커야 합니다. (현재: $capacity)" }
        require(refillRate > 0) { "refillRate은 0보다 커야 합니다. (현재: $refillRate)" }
        require(waitTimeoutMillis > 0) { "waitTimeoutMillis는 0보다 커야 합니다. (현재: $waitTimeoutMillis)" }
    }

    /**
     * 토큰 리필까지 필요한 시간(초)을 반환합니다.
     */
    val refillPeriodSeconds: Double
        get() = if (refillRate > 0) 1.0 / refillRate else 0.0

    /**
     * 최대 용량까지 리필하는데 필요한 시간(초)을 반환합니다.
     */
    val fullRefillSeconds: Double
        get() = if (refillRate > 0) capacity.toDouble() / refillRate else 0.0
}

/**
 * Rate Limiting 설정 모음
 *
 * 각 외부 API별 Rate Limiting 설정을 관리합니다.
 *
 * @property yahoo Yahoo Finance API Rate Limiting 설정
 * @property fred FRED API Rate Limiting 설정
 */
@Serializable
data class RateLimitingSettings(
    val yahoo: RateLimitConfig = RateLimitConfig(capacity = 50, refillRate = 50),
    val fred: RateLimitConfig = RateLimitConfig(capacity = 10, refillRate = 10)
)
