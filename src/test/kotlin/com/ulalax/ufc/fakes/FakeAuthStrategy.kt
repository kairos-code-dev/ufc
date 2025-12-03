package com.ulalax.ufc.fakes

import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.infrastructure.yahoo.auth.AuthResult
import com.ulalax.ufc.infrastructure.yahoo.auth.AuthStrategy

/**
 * AuthStrategy의 테스트용 Fake 구현입니다.
 *
 * 이 구현은 클래식 TDD의 Fake 객체로서:
 * - 실제 네트워크 호출 없이 결정적인 응답 반환
 * - 호출 추적을 통해 메서드 호출 여부 검증
 * - 실패 시나리오 지원으로 예외 처리 테스트 가능
 * - 모든 테스트에서 일관성 있는 동작 보장
 *
 * @property crumbToken 반환할 고정 CRUMB 토큰 (기본값: 테스트용 토큰)
 * @property strategy 반환할 전략 이름 (기본값: "fake")
 */
class FakeAuthStrategy(
    private val crumbToken: String = "FAKE_CRUMB_TOKEN_FOR_TESTING_12345",
    private val strategy: String = "fake"
) : AuthStrategy {

    // 호출 추적을 위한 내부 상태
    private var authenticateCallCount = 0
    private var shouldFailWithErrorCode: ErrorCode? = null
    private var shouldFailWithException: Exception? = null

    /**
     * Yahoo Finance API 인증을 모의로 수행합니다.
     *
     * 이 메서드는 다음의 행동을 합니다:
     * 1. 호출 카운트 증가 (호출 추적)
     * 2. 실패 조건이 설정된 경우 예외 발생
     * 3. 설정된 CRUMB 토큰으로 AuthResult 반환
     *
     * @return 고정된 CRUMB 토큰을 포함하는 AuthResult
     * @throws UfcException shouldFailWithErrorCode가 설정된 경우
     * @throws Exception shouldFailWithException이 설정된 경우
     */
    override suspend fun authenticate(): AuthResult {
        authenticateCallCount++

        // 실패 시나리오 1: 특정 ErrorCode로 실패
        shouldFailWithErrorCode?.let {
            throw UfcException(
                errorCode = it,
                message = "Fake authentication failed with error code: ${it.name}"
            )
        }

        // 실패 시나리오 2: 특정 Exception으로 실패
        shouldFailWithException?.let {
            throw it
        }

        // 성공 시나리오: AuthResult 반환
        return AuthResult(
            crumb = crumbToken,
            strategy = strategy,
            timestamp = System.currentTimeMillis()
        )
    }

    // ============= 테스트 헬퍼 메서드 =============

    /**
     * authenticate() 메서드의 호출 횟수를 반환합니다.
     *
     * @return 호출 횟수
     */
    fun getAuthenticateCallCount(): Int = authenticateCallCount

    /**
     * authenticate()가 호출되었는지 여부를 반환합니다.
     *
     * @return 호출된 경우 true, 미호출 경우 false
     */
    fun isAuthenticateCalled(): Boolean = authenticateCallCount > 0

    /**
     * 특정 ErrorCode로 실패하도록 설정합니다.
     *
     * 이 메서드 호출 후 authenticate()는 지정된 ErrorCode로 UfcException을 발생시킵니다.
     *
     * @param errorCode 실패 시 발생시킬 ErrorCode
     */
    fun setFailureWithErrorCode(errorCode: ErrorCode) {
        this.shouldFailWithErrorCode = errorCode
        this.shouldFailWithException = null
    }

    /**
     * 특정 Exception으로 실패하도록 설정합니다.
     *
     * 이 메서드 호출 후 authenticate()는 지정된 Exception을 발생시킵니다.
     *
     * @param exception 실패 시 발생시킬 Exception
     */
    fun setFailureWithException(exception: Exception) {
        this.shouldFailWithException = exception
        this.shouldFailWithErrorCode = null
    }

    /**
     * 이전에 설정한 실패 조건을 해제합니다.
     *
     * 이 메서드 호출 후 authenticate()는 다시 성공적으로 AuthResult를 반환합니다.
     */
    fun clearFailureCondition() {
        this.shouldFailWithErrorCode = null
        this.shouldFailWithException = null
    }

    /**
     * Fake 객체를 초기 상태로 리셋합니다.
     *
     * 호출 카운트와 실패 조건을 모두 초기화합니다.
     * 테스트 격리(Test Isolation)를 위해 각 테스트 후 호출하는 것을 권장합니다.
     */
    fun reset() {
        authenticateCallCount = 0
        shouldFailWithErrorCode = null
        shouldFailWithException = null
    }

    /**
     * Fake 객체의 상태를 상세히 문자열로 반환합니다.
     *
     * @return 상태 정보 문자열
     */
    override fun toString(): String {
        val failureStatus = when {
            shouldFailWithErrorCode != null -> "ErrorCode: ${shouldFailWithErrorCode!!.name}"
            shouldFailWithException != null -> "Exception: ${shouldFailWithException!!::class.simpleName}"
            else -> "No failure"
        }

        return "FakeAuthStrategy(" +
                "crumbToken_length=${crumbToken.length}, " +
                "strategy='$strategy', " +
                "callCount=$authenticateCallCount, " +
                "failure=$failureStatus)"
    }
}
