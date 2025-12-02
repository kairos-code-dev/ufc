package com.ulalax.ufc.fakes

import com.ulalax.ufc.infrastructure.ratelimit.RateLimiter
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitConfig
import com.ulalax.ufc.infrastructure.ratelimit.RateLimiterStatus

/**
 * RateLimiter의 테스트용 Fake 구현입니다.
 *
 * 이 구현은 클래식 TDD의 Fake 객체로서:
 * - 실제 Rate Limiting 로직 없이 즉시 반환 (지연 없음)
 * - 호출 추적을 통해 acquire() 호출 여부 검증
 * - 토큰 상태 관리로 내부 동작 검증
 * - 테스트 속도 향상 및 일관성 있는 동작 보장
 *
 * acquire() 호출 시 지연이 없으므로 테스트가 빠르게 진행됩니다.
 *
 * @property name RateLimiter의 이름 (로깅/디버깅용)
 * @property initialTokens 초기 토큰 개수 (기본값: 100)
 */
class FakeRateLimiter(
    private val name: String = "FAKE_RATE_LIMITER",
    private val initialTokens: Int = 100
) : RateLimiter {

    // 호출 추적을 위한 내부 상태
    private var acquireCallCount = 0
    private var totalTokensConsumed = 0
    private var currentAvailableTokens = initialTokens
    private var shouldFail = false
    private var failureException: Exception? = null

    // 각 acquire 호출의 상세 기록
    private val acquireHistory = mutableListOf<AcquireRecord>()

    /**
     * 지정된 개수의 토큰을 소비합니다.
     *
     * 이 구현은 실제 대기 없이 즉시 반환되며, 호출을 기록합니다.
     * 토큰 개수는 감소하지만 실제 지연은 없습니다 (테스트 성능 향상).
     *
     * @param tokensNeeded 소비할 토큰 개수 (기본값: 1)
     * @throws Exception shouldFail가 true인 경우 설정된 예외 발생
     */
    override suspend fun acquire(tokensNeeded: Int) {
        // 실패 조건 확인
        if (shouldFail) {
            failureException?.let { throw it }
                ?: throw RuntimeException("Fake rate limiter configured to fail")
        }

        // 호출 정보 기록
        acquireCallCount++
        totalTokensConsumed += tokensNeeded
        currentAvailableTokens = (currentAvailableTokens - tokensNeeded).coerceAtLeast(0)

        // 호출 이력에 추가
        acquireHistory.add(
            AcquireRecord(
                sequence = acquireCallCount,
                tokensRequested = tokensNeeded,
                timestampMs = System.currentTimeMillis()
            )
        )
    }

    /**
     * 현재 사용 가능한 토큰의 개수를 반환합니다.
     *
     * @return 사용 가능한 토큰의 개수
     */
    override fun getAvailableTokens(): Int = currentAvailableTokens

    /**
     * 1개 토큰을 획득하는 데 필요한 대기 시간을 반환합니다.
     *
     * Fake 구현이므로 항상 0을 반환합니다 (실제 대기 없음).
     *
     * @return 항상 0 (지연 없음)
     */
    override fun getWaitTimeMillis(): Long = 0L

    /**
     * RateLimiter의 현재 상태를 반환합니다.
     *
     * @return 현재 상태 정보
     */
    override fun getStatus(): RateLimiterStatus {
        return RateLimiterStatus(
            availableTokens = currentAvailableTokens,
            capacity = initialTokens,
            refillRate = 0,  // Fake 구현이므로 리필 없음
            isEnabled = !shouldFail,
            estimatedWaitTimeMs = 0L
        )
    }

    // ============= 테스트 헬퍼 메서드 =============

    /**
     * acquire() 메서드의 호출 횟수를 반환합니다.
     *
     * @return 호출 횟수
     */
    fun getAcquireCallCount(): Int = acquireCallCount

    /**
     * acquire()가 호출되었는지 여부를 반환합니다.
     *
     * @return 호출된 경우 true, 미호출 경우 false
     */
    fun isAcquireCalled(): Boolean = acquireCallCount > 0

    /**
     * 지금까지 소비한 토큰의 총 개수를 반환합니다.
     *
     * @return 소비한 토큰 총 개수
     */
    fun getTotalTokensConsumed(): Int = totalTokensConsumed

    /**
     * acquire() 호출의 상세 기록을 반환합니다.
     *
     * @return AcquireRecord 목록
     */
    fun getAcquireHistory(): List<AcquireRecord> = acquireHistory.toList()

    /**
     * 특정 시퀀스의 acquire 호출 정보를 조회합니다.
     *
     * @param sequence 조회할 호출 순서 (1부터 시작)
     * @return AcquireRecord 또는 null (없는 경우)
     */
    fun getAcquireRecord(sequence: Int): AcquireRecord? {
        return acquireHistory.find { it.sequence == sequence }
    }

    /**
     * 마지막 acquire() 호출의 정보를 반환합니다.
     *
     * @return 마지막 AcquireRecord 또는 null (호출이 없는 경우)
     */
    fun getLastAcquireRecord(): AcquireRecord? = acquireHistory.lastOrNull()

    /**
     * 지정된 Exception으로 실패하도록 설정합니다.
     *
     * 이 메서드 호출 후 acquire()는 지정된 Exception을 발생시킵니다.
     *
     * @param exception 발생시킬 Exception
     */
    fun setFailureWithException(exception: Exception) {
        shouldFail = true
        failureException = exception
    }

    /**
     * 일반적인 실패 조건을 설정합니다.
     *
     * 이 메서드 호출 후 acquire()는 RuntimeException을 발생시킵니다.
     */
    fun setFailure() {
        shouldFail = true
        failureException = null
    }

    /**
     * 이전에 설정한 실패 조건을 해제합니다.
     *
     * 이 메서드 호출 후 acquire()는 다시 정상적으로 작동합니다.
     */
    fun clearFailureCondition() {
        shouldFail = false
        failureException = null
    }

    /**
     * Fake 객체를 초기 상태로 리셋합니다.
     *
     * 호출 카운트, 토큰 상태, 실패 조건을 모두 초기화합니다.
     * 테스트 격리(Test Isolation)를 위해 각 테스트 후 호출하는 것을 권장합니다.
     */
    fun reset() {
        acquireCallCount = 0
        totalTokensConsumed = 0
        currentAvailableTokens = initialTokens
        shouldFail = false
        failureException = null
        acquireHistory.clear()
    }

    /**
     * 수동으로 사용 가능한 토큰 개수를 설정합니다.
     *
     * 테스트 시나리오 설정용입니다.
     *
     * @param tokens 설정할 토큰 개수
     */
    fun setAvailableTokens(tokens: Int) {
        currentAvailableTokens = tokens
    }

    /**
     * Fake 객체의 상태를 상세히 문자열로 반환합니다.
     *
     * @return 상태 정보 문자열
     */
    override fun toString(): String {
        val failureStatus = when {
            shouldFail -> "FAILING (${failureException?.message ?: "Generic failure"})"
            else -> "OK"
        }

        return "FakeRateLimiter(" +
                "name='$name', " +
                "callCount=$acquireCallCount, " +
                "availableTokens=$currentAvailableTokens, " +
                "totalConsumed=$totalTokensConsumed, " +
                "status=$failureStatus)"
    }

    /**
     * acquire() 호출의 기록을 나타내는 데이터 클래스입니다.
     *
     * @property sequence 호출 순서 (1부터 시작)
     * @property tokensRequested 요청한 토큰 개수
     * @property timestampMs 호출 시간 (밀리초)
     */
    data class AcquireRecord(
        val sequence: Int,
        val tokensRequested: Int,
        val timestampMs: Long
    ) {
        /**
         * 이전 호출 이후 경과 시간을 계산합니다.
         *
         * @param previousRecord 이전 호출 기록
         * @return 경과 시간 (밀리초)
         */
        fun getElapsedSincePrevious(previousRecord: AcquireRecord): Long {
            return this.timestampMs - previousRecord.timestampMs
        }

        /**
         * 호출 정보를 상세히 문자열로 반환합니다.
         *
         * @return 정보 문자열
         */
        override fun toString(): String {
            return "AcquireRecord(#$sequence, tokens=$tokensRequested, timestamp=$timestampMs)"
        }
    }
}
