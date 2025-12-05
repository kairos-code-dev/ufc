package com.ulalax.ufc.infrastructure.yahoo.streaming

/**
 * WebSocket 재연결 설정.
 *
 * 연결이 끊어졌을 때 자동 재연결 동작을 제어합니다.
 * 지수 백오프(exponential backoff) 전략을 사용하여 재시도 간격을 점진적으로 늘립니다.
 *
 * @property enabled 재연결 활성화 여부 (기본값: true)
 * @property maxAttempts 최대 재시도 횟수 (기본값: 5)
 * @property initialDelayMs 초기 대기 시간 (밀리초, 기본값: 1000ms = 1초)
 * @property maxDelayMs 최대 대기 시간 (밀리초, 기본값: 30000ms = 30초)
 * @property backoffMultiplier 지수 백오프 배수 (기본값: 2.0)
 */
data class ReconnectionConfig(
    val enabled: Boolean = true,
    val maxAttempts: Int = 5,
    val initialDelayMs: Long = 1000,
    val maxDelayMs: Long = 30000,
    val backoffMultiplier: Double = 2.0
) {
    init {
        require(maxAttempts > 0) { "maxAttempts must be positive" }
        require(initialDelayMs > 0) { "initialDelayMs must be positive" }
        require(maxDelayMs >= initialDelayMs) { "maxDelayMs must be >= initialDelayMs" }
        require(backoffMultiplier >= 1.0) { "backoffMultiplier must be >= 1.0" }
    }

    /**
     * 재시도 횟수에 따른 대기 시간을 계산합니다.
     *
     * 공식: min(initialDelay × (backoffMultiplier ^ attempt), maxDelay)
     *
     * @param attempt 재시도 횟수 (0부터 시작)
     * @return 대기 시간 (밀리초)
     */
    fun calculateDelay(attempt: Int): Long {
        val delay = (initialDelayMs * Math.pow(backoffMultiplier, attempt.toDouble())).toLong()
        return minOf(delay, maxDelayMs)
    }
}
