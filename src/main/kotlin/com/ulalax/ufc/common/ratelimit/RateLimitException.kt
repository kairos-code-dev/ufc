package com.ulalax.ufc.common.ratelimit

/**
 * Rate Limiting 관련 예외의 부모 클래스
 *
 * Rate Limiter에서 발생할 수 있는 모든 예외는 이 sealed class를 상속합니다.
 */
sealed class RateLimitException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    /**
     * Rate Limit 토큰 대기 타임아웃 예외
     *
     * 필요한 토큰을 획득하기 위해 대기할 때 waitTimeoutMillis를 초과한 경우 발생합니다.
     *
     * @property source API 또는 서비스 소스 (예: "YAHOO", "FRED")
     * @property config Rate Limit 설정
     * @property tokensNeeded 요청한 토큰 개수
     * @property waitedMillis 실제 대기한 시간 (밀리초)
     *
     * 사용 예시:
     * ```
     * try {
     *     rateLimiter.acquire(10)
     * } catch (e: RateLimitException.RateLimitTimeoutException) {
     *     logger.error("Rate limit timeout for ${e.source}")
     *     // 재시도 로직 또는 폴백 처리
     * }
     * ```
     */
    data class RateLimitTimeoutException(
        val source: String,
        val config: RateLimitConfig,
        val tokensNeeded: Int = 1,
        val waitedMillis: Long = 0L
    ) : RateLimitException(
        message = "Rate limit timeout for source '$source' after waiting ${waitedMillis}ms. " +
                "Needed tokens: $tokensNeeded, capacity: ${config.capacity}, " +
                "refill rate: ${config.refillRate} tokens/sec, " +
                "timeout: ${config.waitTimeoutMillis}ms"
    ) {
        /**
         * 예상 대기 시간을 형식화하여 반환합니다.
         */
        fun getFormattedWaitTime(): String {
            val estimatedWaitMs = (tokensNeeded * 1000.0) / config.refillRate
            return when {
                estimatedWaitMs < 1000 -> "${estimatedWaitMs.toLong()}ms"
                estimatedWaitMs < 60000 -> "%.2f초".format(estimatedWaitMs / 1000)
                else -> "%.2f분".format(estimatedWaitMs / 60000)
            }
        }
    }

    /**
     * Rate Limit 설정 검증 예외
     *
     * Rate Limit 설정 값이 유효하지 않은 경우 발생합니다.
     *
     * @property configError 설정 오류 메시지
     */
    data class RateLimitConfigException(
        val configError: String
    ) : RateLimitException(
        message = "Invalid rate limit configuration: $configError"
    )

    /**
     * Rate Limit 상태 오류 예외
     *
     * Rate Limiter의 상태가 부정합한 경우 발생합니다.
     * (예: 초기화되지 않은 상태에서 acquire 호출)
     *
     * @property stateError 상태 오류 메시지
     */
    data class RateLimitStateException(
        val stateError: String
    ) : RateLimitException(
        message = "Rate limiter state error: $stateError"
    )
}
