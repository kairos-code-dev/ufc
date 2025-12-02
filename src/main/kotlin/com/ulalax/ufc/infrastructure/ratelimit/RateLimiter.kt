package com.ulalax.ufc.infrastructure.ratelimit

import kotlinx.serialization.Serializable

/**
 * Rate Limiter 인터페이스
 *
 * 다양한 외부 API에 대한 요청 제한을 관리하기 위한 인터페이스입니다.
 * Token Bucket 알고리즘을 기반으로 동작합니다.
 *
 * 사용 예시:
 * ```
 * val rateLimiter = TokenBucketRateLimiter(config)
 *
 * // 1개 토큰 소비
 * rateLimiter.acquire()
 *
 * // 5개 토큰 소비
 * rateLimiter.acquire(5)
 *
 * // 현재 상태 확인
 * val status = rateLimiter.getStatus()
 * println("Available tokens: ${status.availableTokens}")
 * ```
 */
interface RateLimiter {
    /**
     * 지정된 개수의 토큰을 소비합니다.
     *
     * 토큰이 부족한 경우, 필요한 토큰이 충전될 때까지 대기합니다.
     * waitTimeoutMillis를 초과하면 [RateLimitException.RateLimitTimeoutException]을 발생시킵니다.
     *
     * @param tokensNeeded 소비할 토큰 개수 (기본값: 1)
     * @throws RateLimitException.RateLimitTimeoutException 대기 시간 초과 시
     */
    suspend fun acquire(tokensNeeded: Int = 1)

    /**
     * 현재 사용 가능한 토큰의 개수를 반환합니다.
     *
     * @return 사용 가능한 토큰의 정수 개수
     */
    fun getAvailableTokens(): Int

    /**
     * 1개 토큰을 획득하는 데 필요한 대기 시간을 밀리초로 반환합니다.
     *
     * 사용 가능한 토큰이 있으면 0을 반환합니다.
     *
     * @return 대기 시간 (밀리초)
     */
    fun getWaitTimeMillis(): Long

    /**
     * Rate Limiter의 현재 상태를 반환합니다.
     *
     * @return 현재 상태 정보
     */
    fun getStatus(): RateLimiterStatus
}

/**
 * Rate Limiter의 상태를 나타내는 데이터 클래스
 *
 * 현재 사용 가능한 토큰, 용량, 리필 속도 등의 정보를 포함합니다.
 *
 * @property availableTokens 현재 사용 가능한 토큰 개수
 * @property capacity 최대 토큰 용량
 * @property refillRate 초당 리필되는 토큰 개수
 * @property isEnabled Rate Limiter 활성화 여부
 * @property estimatedWaitTimeMs 1개 토큰 획득 예상 대기 시간 (밀리초)
 */
@Serializable
data class RateLimiterStatus(
    val availableTokens: Int,
    val capacity: Int,
    val refillRate: Int,
    val isEnabled: Boolean,
    val estimatedWaitTimeMs: Long
) {
    /**
     * 사용률을 백분율로 반환합니다.
     *
     * @return 사용률 (0.0 ~ 100.0)
     */
    val utilizationPercent: Double
        get() = if (capacity > 0) {
            ((capacity - availableTokens) * 100.0) / capacity
        } else {
            0.0
        }

    /**
     * 토큰이 사용 가능한지 여부를 반환합니다.
     *
     * @return 토큰 사용 가능 여부
     */
    val hasTokensAvailable: Boolean
        get() = availableTokens > 0
}
