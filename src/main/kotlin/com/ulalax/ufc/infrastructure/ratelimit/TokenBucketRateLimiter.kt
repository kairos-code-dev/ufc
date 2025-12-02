package com.ulalax.ufc.infrastructure.ratelimit

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.ceil

/**
 * Token Bucket 알고리즘을 기반으로 한 Rate Limiter 구현
 *
 * Token Bucket 알고리즘의 특징:
 * - 일정한 속도로 토큰이 버킷에 채워집니다 (refillRate)
 * - 최대 용량(capacity)까지만 토큰이 누적됩니다
 * - API 요청이 필요할 때 버킷에서 토큰을 꺼냅니다
 * - 토큰이 부족하면 대기합니다
 *
 * 이 구현은 Coroutine Mutex를 사용하여 스레드 안전성을 보장합니다.
 *
 * @property source API 또는 서비스 소스 (로깅/디버깅용)
 * @property config Rate Limiting 설정
 *
 * 사용 예시:
 * ```
 * // 설정
 * val config = RateLimitConfig(
 *     capacity = 50,
 *     refillRate = 50,
 *     enabled = true,
 *     waitTimeoutMillis = 60000L
 * )
 *
 * val rateLimiter = TokenBucketRateLimiter("YAHOO", config)
 *
 * // 사용
 * try {
 *     // 1개 토큰 소비
 *     rateLimiter.acquire()
 *
 *     // 5개 토큰 소비
 *     rateLimiter.acquire(5)
 *
 *     // 상태 확인
 *     val status = rateLimiter.getStatus()
 *     println("Remaining tokens: ${status.availableTokens}")
 * } catch (e: RateLimitException.RateLimitTimeoutException) {
 *     println("Rate limit exceeded")
 * }
 * ```
 */
class TokenBucketRateLimiter(
    private val source: String,
    private val config: RateLimitConfig
) : RateLimiter {

    // 현재 버킷에 있는 토큰 개수 (Double로 관리하여 정밀한 리필 계산)
    private var tokens: Double = config.capacity.toDouble()

    // 마지막으로 토큰을 리필한 시간 (밀리초)
    private var lastRefillTime: Long = System.currentTimeMillis()

    // 동시성 제어용 Mutex (Coroutine-safe)
    private val lock = Mutex()

    init {
        require(source.isNotBlank()) { "source는 빈 문자열이 될 수 없습니다" }
    }

    /**
     * 지정된 개수의 토큰을 소비합니다.
     *
     * Rate Limiter가 비활성화된 경우는 즉시 반환합니다.
     * 토큰이 부족한 경우, 필요한 토큰이 충전될 때까지 대기합니다.
     *
     * @param tokensNeeded 소비할 토큰 개수 (기본값: 1)
     * @throws RateLimitException.RateLimitTimeoutException 대기 시간 초과 시
     */
    override suspend fun acquire(tokensNeeded: Int) {
        // Rate Limiter 비활성화 상태 확인
        if (!config.enabled) {
            return
        }

        require(tokensNeeded > 0) { "tokensNeeded는 0보다 커야 합니다. (현재: $tokensNeeded)" }
        require(tokensNeeded <= config.capacity) {
            "tokensNeeded는 capacity를 초과할 수 없습니다. " +
                    "(요청: $tokensNeeded, capacity: ${config.capacity})"
        }

        val startTimeMillis = System.currentTimeMillis()

        while (true) {
            lock.withLock {
                // 토큰 리필
                refillTokens()

                // 요청한 토큰이 사용 가능한 경우
                if (tokens >= tokensNeeded) {
                    tokens -= tokensNeeded
                    return
                }
            }

            // 필요한 대기 시간 계산
            val waitTimeMs = calculateWaitTimeMs(tokensNeeded)

            // 타임아웃 확인
            val elapsedTime = System.currentTimeMillis() - startTimeMillis
            if (elapsedTime + waitTimeMs > config.waitTimeoutMillis) {
                throw RateLimitException.RateLimitTimeoutException(
                    source = source,
                    config = config,
                    tokensNeeded = tokensNeeded,
                    waitedMillis = elapsedTime
                )
            }

            // 대기 (최대 100ms 단위로 나누어 대기하여 응답성 향상)
            val delayMs = minOf(waitTimeMs.toLong(), 100L)
            delay(delayMs)
        }
    }

    /**
     * 현재 사용 가능한 토큰의 개수를 반환합니다.
     *
     * @return 사용 가능한 토큰의 정수 개수
     */
    override fun getAvailableTokens(): Int {
        // lock을 획득하여 스레드 안전성 보장
        return runBlocking {
            lock.withLock {
                refillTokens()
                tokens.toInt()
            }
        }
    }

    /**
     * 1개 토큰을 획득하는 데 필요한 대기 시간을 밀리초로 반환합니다.
     *
     * 사용 가능한 토큰이 있으면 0을 반환합니다.
     *
     * @return 대기 시간 (밀리초)
     */
    override fun getWaitTimeMillis(): Long {
        return runBlocking {
            lock.withLock {
                refillTokens()
                if (tokens >= 1.0) {
                    0L
                } else {
                    calculateWaitTimeMs(1).toLong()
                }
            }
        }
    }

    /**
     * Rate Limiter의 현재 상태를 반환합니다.
     *
     * @return 현재 상태 정보
     */
    override fun getStatus(): RateLimiterStatus {
        return RateLimiterStatus(
            availableTokens = getAvailableTokens(),
            capacity = config.capacity,
            refillRate = config.refillRate,
            isEnabled = config.enabled,
            estimatedWaitTimeMs = getWaitTimeMillis()
        )
    }

    /**
     * 마지막 리필 이후 경과 시간에 따라 토큰을 리필합니다.
     *
     * 토큰은 다음 공식에 따라 리필됩니다:
     * tokensToAdd = (현재시간 - 마지막리필시간) / 1000 * refillRate
     *
     * 리필된 토큰은 capacity를 초과할 수 없습니다.
     */
    private fun refillTokens() {
        val currentTime = System.currentTimeMillis()
        val elapsedMillis = currentTime - lastRefillTime

        if (elapsedMillis > 0) {
            // 경과 시간 동안 리필될 토큰 계산
            // refillRate는 초당 토큰 개수이므로 1000으로 나눔
            val tokensToAdd = (elapsedMillis * config.refillRate) / 1000.0
            tokens = minOf(tokens + tokensToAdd, config.capacity.toDouble())
            lastRefillTime = currentTime
        }
    }

    /**
     * 지정된 개수의 토큰을 획득하는 데 필요한 대기 시간을 계산합니다.
     *
     * 계산 공식:
     * tokensNeeded를 획득하려면 (tokensNeeded - tokens) / refillRate * 1000 밀리초 필요
     *
     * @param tokensNeeded 필요한 토큰 개수
     * @return 대기 시간 (밀리초)
     */
    private fun calculateWaitTimeMs(tokensNeeded: Int): Double {
        if (tokens >= tokensNeeded) {
            return 0.0
        }

        val tokensToWait = tokensNeeded - tokens
        return (tokensToWait / config.refillRate) * 1000.0
    }

    /**
     * 현재 Rate Limiter 상태의 상세 정보를 문자열로 반환합니다.
     *
     * @return 상태 정보 문자열
     */
    override fun toString(): String {
        val status = getStatus()
        return "TokenBucketRateLimiter(" +
                "source=$source, " +
                "availableTokens=${status.availableTokens}, " +
                "capacity=${status.capacity}, " +
                "refillRate=${status.refillRate}/sec, " +
                "enabled=${status.isEnabled}, " +
                "utilizationPercent=%.2f%%, ".format(status.utilizationPercent) +
                "estimatedWaitTimeMs=${status.estimatedWaitTimeMs}ms)"
    }
}
