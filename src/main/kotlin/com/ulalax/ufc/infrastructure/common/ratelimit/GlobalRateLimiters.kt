package com.ulalax.ufc.infrastructure.common.ratelimit

import org.slf4j.LoggerFactory

/**
 * 글로벌 Rate Limiter 싱글톤 관리자
 *
 * 모든 UFC 클라이언트 인스턴스가 소스별(Yahoo, FRED, BusinessInsider) Rate Limiter를 공유합니다.
 * 이를 통해 여러 클라이언트 인스턴스가 생성되어도 Rate Limit이 배가되지 않습니다.
 *
 * ## 동작 방식
 * - **싱글톤 패턴**: 각 소스별로 단일 TokenBucketRateLimiter 인스턴스 유지
 * - **Thread-safe**: Double-checked locking + @Volatile로 동시성 제어
 * - **설정 우선순위**: 첫 호출의 설정이 적용되며, 이후 호출은 기존 인스턴스 재사용
 *
 * ## 사용 예제
 * ```kotlin
 * // 첫 번째 클라이언트 생성 (RPS 30으로 초기화)
 * val client1 = YahooClient.create(
 *     YahooClientConfig(
 *         rateLimitConfig = RateLimitConfig(capacity = 30, refillRate = 30)
 *     )
 * )
 *
 * // 두 번째 클라이언트 생성 (첫 번째 설정 재사용, RPS 30)
 * val client2 = YahooClient.create()
 *
 * // client1과 client2가 같은 Rate Limiter 공유
 * ```
 *
 * ## 제약사항
 * - **첫 호출 우선**: 첫 번째 클라이언트 생성 호출의 설정이 영구 적용
 * - **JVM 프로세스 단위**: 멀티프로세스 환경에서는 각 프로세스마다 독립적인 싱글톤
 * - **설정 변경 불가**: 초기화 후 설정 변경 불가 (테스트 환경 제외)
 *
 * @see TokenBucketRateLimiter
 * @see RateLimitingSettings
 */
object GlobalRateLimiters {
    private val logger = LoggerFactory.getLogger(GlobalRateLimiters::class.java)

    // ========================================
    // Volatile 변수: JMM visibility 보장
    // ========================================

    @Volatile
    private var yahooInstance: TokenBucketRateLimiter? = null

    @Volatile
    private var fredInstance: TokenBucketRateLimiter? = null

    @Volatile
    private var businessInsiderInstance: TokenBucketRateLimiter? = null

    // ========================================
    // Lock 객체: 소스별 독립적인 동기화
    // ========================================

    private val yahooLock = Any()
    private val fredLock = Any()
    private val businessInsiderLock = Any()

    // ========================================
    // Yahoo Rate Limiter
    // ========================================

    /**
     * Yahoo Finance API 글로벌 Rate Limiter 획득
     *
     * 첫 호출 시 제공된 config로 TokenBucketRateLimiter를 초기화하며,
     * 이후 호출은 기존 인스턴스를 재사용합니다.
     *
     * ## Thread-safety
     * Double-checked locking 패턴으로 구현:
     * 1. 빠른 경로: 이미 초기화되었으면 lock 없이 반환
     * 2. 느린 경로: 초기화되지 않았으면 lock 획득 후 재확인 및 생성
     *
     * @param config Rate Limit 설정 (기본값: Yahoo 권장 50 RPS)
     * @return Yahoo 전용 글로벌 TokenBucketRateLimiter
     */
    fun getYahooLimiter(config: RateLimitConfig = RateLimitingSettings.yahooDefault()): TokenBucketRateLimiter {
        // Fast path: 이미 초기화되었으면 즉시 반환
        yahooInstance?.let { return it }

        // Slow path: 초기화 필요
        return synchronized(yahooLock) {
            // Double-check: lock 획득 후 다시 확인
            yahooInstance ?: TokenBucketRateLimiter("Yahoo", config).also { newInstance ->
                yahooInstance = newInstance
                logger.info(
                    "Initialized global Yahoo RateLimiter: " +
                        "capacity={}, " +
                        "refillRate={} req/sec, " +
                        "enabled={}",
                    config.capacity,
                    config.refillRate,
                    config.enabled,
                )
            }
        }
    }

    // ========================================
    // FRED Rate Limiter
    // ========================================

    /**
     * FRED API 글로벌 Rate Limiter 획득
     *
     * 첫 호출 시 제공된 config로 TokenBucketRateLimiter를 초기화하며,
     * 이후 호출은 기존 인스턴스를 재사용합니다.
     *
     * @param config Rate Limit 설정 (기본값: FRED 권장 2 RPS)
     * @return FRED 전용 글로벌 TokenBucketRateLimiter
     */
    fun getFredLimiter(config: RateLimitConfig = RateLimitingSettings.fredDefault()): TokenBucketRateLimiter {
        fredInstance?.let { return it }

        return synchronized(fredLock) {
            fredInstance ?: TokenBucketRateLimiter("FRED", config).also { newInstance ->
                fredInstance = newInstance
                logger.info(
                    "Initialized global FRED RateLimiter: " +
                        "capacity={}, " +
                        "refillRate={} req/sec, " +
                        "enabled={}",
                    config.capacity,
                    config.refillRate,
                    config.enabled,
                )
            }
        }
    }

    // ========================================
    // Business Insider Rate Limiter
    // ========================================

    /**
     * Business Insider API 글로벌 Rate Limiter 획득
     *
     * 첫 호출 시 제공된 config로 TokenBucketRateLimiter를 초기화하며,
     * 이후 호출은 기존 인스턴스를 재사용합니다.
     *
     * @param config Rate Limit 설정 (기본값: Business Insider 권장 10 RPS)
     * @return Business Insider 전용 글로벌 TokenBucketRateLimiter
     */
    fun getBusinessInsiderLimiter(
        config: RateLimitConfig = RateLimitingSettings.businessInsiderDefault(),
    ): TokenBucketRateLimiter {
        businessInsiderInstance?.let { return it }

        return synchronized(businessInsiderLock) {
            businessInsiderInstance ?: TokenBucketRateLimiter("BusinessInsider", config).also { newInstance ->
                businessInsiderInstance = newInstance
                logger.info(
                    "Initialized global BusinessInsider RateLimiter: " +
                        "capacity={}, " +
                        "refillRate={} req/sec, " +
                        "enabled={}",
                    config.capacity,
                    config.refillRate,
                    config.enabled,
                )
            }
        }
    }

    // ========================================
    // 테스트 전용 Reset
    // ========================================

    /**
     * 모든 Rate Limiter 초기화 (테스트 전용)
     *
     * 주의: 이 메서드는 테스트 환경에서만 사용해야 합니다.
     * 프로덕션 코드에서 호출하면 Rate Limiting이 리셋되어 예상치 못한 동작이 발생할 수 있습니다.
     *
     * ## 사용 예시
     * ```kotlin
     * @AfterEach
     * fun cleanup() {
     *     GlobalRateLimiters.resetForTesting()
     * }
     * ```
     *
     * @see org.junit.jupiter.api.Test
     */
    @Suppress("unused")
    fun resetForTesting() {
        synchronized(yahooLock) {
            yahooInstance = null
        }
        synchronized(fredLock) {
            fredInstance = null
        }
        synchronized(businessInsiderLock) {
            businessInsiderInstance = null
        }
        logger.warn("GlobalRateLimiters reset for testing - all instances cleared")
    }
}
