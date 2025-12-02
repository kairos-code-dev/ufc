package com.ulalax.ufc.infrastructure.ratelimit

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Tag

/**
 * TokenBucketRateLimiter 단위 테스트
 *
 * Token Bucket 알고리즘의 동작, 동시성 제어, 예외 처리 등을 검증합니다.
 */
@Tag("unit")
@DisplayName("TokenBucketRateLimiter 테스트")
internal class TokenBucketRateLimiterTest {

    private lateinit var rateLimiter: TokenBucketRateLimiter
    private lateinit var config: RateLimitConfig

    @BeforeEach
    fun setUp() {
        config = RateLimitConfig(
            capacity = 50,
            refillRate = 50,
            enabled = true,
            waitTimeoutMillis = 60000L
        )
        rateLimiter = TokenBucketRateLimiter("TEST", config)
    }

    @Test
    @DisplayName("초기 상태: 토큰이 최대 용량으로 설정되어야 함")
    fun testInitialTokenCount() {
        assertThat(rateLimiter.getAvailableTokens()).isEqualTo(50)
    }

    @Test
    @DisplayName("토큰 획득: 1개 토큰 소비 성공")
    fun testAcquireSingleToken() = runBlocking {
        val initialTokens = rateLimiter.getAvailableTokens()
        rateLimiter.acquire(1)
        val afterTokens = rateLimiter.getAvailableTokens()

        assertThat(afterTokens).isEqualTo(initialTokens - 1)
    }

    @Test
    @DisplayName("토큰 획득: 여러 개 토큰 소비 성공")
    fun testAcquireMultipleTokens() = runBlocking {
        val initialTokens = rateLimiter.getAvailableTokens()
        rateLimiter.acquire(10)
        val afterTokens = rateLimiter.getAvailableTokens()

        assertThat(afterTokens).isEqualTo(initialTokens - 10)
    }

    @Test
    @DisplayName("토큰 획득: 모든 토큰 소비")
    fun testAcquireAllTokens() = runBlocking {
        rateLimiter.acquire(50)
        assertThat(rateLimiter.getAvailableTokens()).isEqualTo(0)
    }

    @Test
    @DisplayName("대기 시간: 사용 가능한 토큰이 있으면 0 반환")
    fun testWaitTimeWithAvailableTokens() {
        // 초기에 토큰이 있으므로 대기 시간은 0
        assertThat(rateLimiter.getWaitTimeMillis()).isEqualTo(0L)
    }

    @Test
    @DisplayName("대기 시간: 토큰 부족 시 예상 대기 시간 계산")
    fun testWaitTimeWithInsufficientTokens() = runBlocking {
        // 모든 토큰 소비
        rateLimiter.acquire(50)

        // 1개 토큰이 필요할 때 대기 시간은 약 1000 / refillRate = 20ms
        val waitTime = rateLimiter.getWaitTimeMillis()
        assertThat(waitTime).isGreaterThan(0)
        assertThat(waitTime).isLessThanOrEqualTo(100)
    }

    @Test
    @DisplayName("상태 조회: 현재 상태 정보 반환")
    fun testGetStatus() {
        val status = rateLimiter.getStatus()

        assertThat(status.availableTokens).isEqualTo(50)
        assertThat(status.capacity).isEqualTo(50)
        assertThat(status.refillRate).isEqualTo(50)
        assertThat(status.isEnabled).isTrue()
        assertThat(status.estimatedWaitTimeMs).isEqualTo(0)
    }

    @Test
    @DisplayName("상태 조회: 사용률 계산")
    fun testUtilizationPercent() = runBlocking {
        rateLimiter.acquire(25)
        val status = rateLimiter.getStatus()

        // 25개 소비했으므로 사용률은 50%
        assertThat(status.utilizationPercent).isCloseTo(50.0, within(0.1))
    }

    @Test
    @DisplayName("토큰 리필: 시간 경과에 따른 토큰 자동 리필")
    fun testTokenRefillOverTime() = runBlocking {
        // 모든 토큰 소비
        rateLimiter.acquire(50)
        assertThat(rateLimiter.getAvailableTokens()).isEqualTo(0)

        // 1초 대기 (refillRate = 50 tokens/sec이므로 50개 리필됨)
        delay(1000)

        // 상태 확인
        val status = rateLimiter.getStatus()
        assertThat(status.availableTokens).isGreaterThanOrEqualTo(45)
    }

    @Test
    @DisplayName("토큰 리필: 최대 용량 초과 방지")
    fun testTokenCapacityLimit() = runBlocking {
        // 이미 50개가 있고, 최대 50개까지만 유지되어야 함
        val status = rateLimiter.getStatus()

        assertThat(status.availableTokens).isEqualTo(50)
        assertThat(status.availableTokens).isLessThanOrEqualTo(config.capacity)
    }

    @Test
    @DisplayName("비활성화 상태: Rate Limiter 비활성화 시 즉시 반환")
    fun testDisabledRateLimiter() = runBlocking {
        val disabledConfig = config.copy(enabled = false)
        val disabledLimiter = TokenBucketRateLimiter("DISABLED", disabledConfig)

        // 비활성화되어 있으므로 타임아웃 없이 즉시 반환
        disabledLimiter.acquire(1000)
        // 예외 발생하지 않으면 성공
    }

    @Test
    @DisplayName("예외 처리: 타임아웃 초과 시 RateLimitTimeoutException 발생")
    fun testTimeoutException() = runBlocking {
        val shortTimeoutConfig = RateLimitConfig(
            capacity = 10,
            refillRate = 1,  // 초당 1개 토큰만 리필
            enabled = true,
            waitTimeoutMillis = 100L  // 100ms 타임아웃
        )
        val limitedLimiter = TokenBucketRateLimiter("TIMEOUT_TEST", shortTimeoutConfig)

        // 모든 토큰 소비
        limitedLimiter.acquire(10)

        // 100ms 내에 10개 토큰을 획득할 수 없음 (refillRate = 1)
        assertThatThrownBy {
            runBlocking {
                limitedLimiter.acquire(10)
            }
        }.isInstanceOf(RateLimitException.RateLimitTimeoutException::class.java)
            .hasFieldOrPropertyWithValue("source", "TIMEOUT_TEST")
            .hasFieldOrPropertyWithValue("tokensNeeded", 10)
    }

    @Test
    @DisplayName("예외 처리: 잘못된 토큰 개수 검증")
    fun testInvalidTokenCount() = runBlocking {
        assertThatThrownBy {
            runBlocking {
                rateLimiter.acquire(0)
            }
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("예외 처리: 용량 초과 토큰 요청")
    fun testExcessiveTokenRequest() = runBlocking {
        assertThatThrownBy {
            runBlocking {
                rateLimiter.acquire(100)  // capacity는 50
            }
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("동시성: 여러 Coroutine에서 동시 접근")
    fun testConcurrentAccess() = runBlocking {
        // 충분한 토큰으로 시작
        val jobs = (1..5).map {
            launch {
                rateLimiter.acquire(1)
            }
        }

        jobs.forEach { it.join() }

        // 5개 토큰이 소비되어야 함
        assertThat(rateLimiter.getAvailableTokens()).isEqualTo(45)
    }

    @Test
    @DisplayName("동시성: 많은 요청 처리")
    fun testHighConcurrency() = runBlocking {
        val jobs = (1..50).map {
            launch {
                rateLimiter.acquire(1)
            }
        }

        jobs.forEach { it.join() }

        // 모든 토큰이 소비되어야 함
        assertThat(rateLimiter.getAvailableTokens()).isEqualTo(0)
    }

    @Test
    @DisplayName("동시성: 토큰 리필 중 접근")
    fun testConcurrentAccessWithRefill() = runBlocking {
        // 모든 토큰 소비
        rateLimiter.acquire(50)

        val jobs = (1..5).map {
            launch {
                delay(100)  // 토큰이 리필될 시간 제공
                rateLimiter.acquire(1)
            }
        }

        jobs.forEach { it.join() }

        // 모든 요청이 완료되어야 함 (대기 후 토큰 리필)
    }

    @Test
    @DisplayName("상태 문자열: toString() 메서드 검증")
    fun testToString() {
        val str = rateLimiter.toString()

        assertThat(str).contains("TokenBucketRateLimiter")
        assertThat(str).contains("TEST")
        assertThat(str).contains("50")  // availableTokens
    }
}
