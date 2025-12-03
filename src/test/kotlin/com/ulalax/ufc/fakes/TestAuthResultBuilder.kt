package com.ulalax.ufc.fakes

import com.ulalax.ufc.infrastructure.yahoo.auth.AuthResult

/**
 * AuthResult 객체를 생성하기 위한 Builder 클래스입니다.
 *
 * 클래식 TDD의 Mother Pattern을 구현하여 테스트 픽스처 생성을 단순화합니다.
 * 각 필드를 개별적으로 커스터마이징할 수 있으며, 기본값으로 유효한 객체를 생성합니다.
 *
 * 사용 예시:
 * ```
 * // 기본값으로 생성
 * val authResult = TestAuthResultBuilder().build()
 *
 * // 커스텀 값으로 생성
 * val authResult = TestAuthResultBuilder()
 *     .withCrumb("CUSTOM_CRUMB")
 *     .withStrategy("custom_strategy")
 *     .build()
 *
 * // 만료된 토큰 생성
 * val expiredAuthResult = TestAuthResultBuilder()
 *     .withExpiredTimestamp()
 *     .build()
 * ```
 */
class TestAuthResultBuilder {

    private var crumb: String = "DEFAULT_CRUMB_TOKEN_FOR_TESTING"
    private var strategy: String = "test"
    private var timestamp: Long = System.currentTimeMillis()

    /**
     * CRUMB 토큰을 설정합니다.
     *
     * @param crumb 설정할 CRUMB 토큰
     * @return 체이닝을 위한 Builder 자신
     */
    fun withCrumb(crumb: String) = apply {
        this.crumb = crumb
    }

    /**
     * 인증 전략을 설정합니다.
     *
     * @param strategy 설정할 인증 전략
     * @return 체이닝을 위한 Builder 자신
     */
    fun withStrategy(strategy: String) = apply {
        this.strategy = strategy
    }

    /**
     * 타임스탐프를 설정합니다.
     *
     * @param timestamp 설정할 타임스탐프 (밀리초)
     * @return 체이닝을 위한 Builder 자신
     */
    fun withTimestamp(timestamp: Long) = apply {
        this.timestamp = timestamp
    }

    /**
     * 특정 시간 전의 타임스탐프를 설정합니다.
     *
     * @param millisAgo 현재로부터 몇 밀리초 이전의 시간을 설정할지
     * @return 체이닝을 위한 Builder 자신
     */
    fun withTimestampAgo(millisAgo: Long) = apply {
        this.timestamp = System.currentTimeMillis() - millisAgo
    }

    /**
     * 타임스탐프를 1시간 이전으로 설정합니다.
     *
     * AuthResult의 isValid() 메서드는 1시간 이내의 인증만 유효로 간주하므로,
     * 이 메서드를 호출하면 만료된 AuthResult가 생성됩니다.
     *
     * @return 체이닝을 위한 Builder 자신
     */
    fun withExpiredTimestamp() = apply {
        val ONE_HOUR_IN_MILLIS = 60 * 60 * 1000L
        this.timestamp = System.currentTimeMillis() - ONE_HOUR_IN_MILLIS - 1000L
    }

    /**
     * 타임스탐프를 30분 이전으로 설정합니다.
     *
     * 유효하지만 곧 만료될 예정인 AuthResult를 생성할 때 사용합니다.
     *
     * @return 체이닝을 위한 Builder 자신
     */
    fun withRecentTimestamp() = apply {
        val THIRTY_MINUTES_IN_MILLIS = 30 * 60 * 1000L
        this.timestamp = System.currentTimeMillis() - THIRTY_MINUTES_IN_MILLIS
    }

    /**
     * 지정된 길이의 CRUMB을 생성하여 설정합니다.
     *
     * 다양한 길이의 CRUMB을 테스트해야 할 때 사용합니다.
     *
     * @param length CRUMB의 길이
     * @return 체이닝을 위한 Builder 자신
     */
    fun withCrumbOfLength(length: Int) = apply {
        this.crumb = "A".repeat(length)
    }

    /**
     * BasicAuthStrategy에서 반환하는 크롬을 시뮬레이션합니다.
     *
     * @return 체이닝을 위한 Builder 자신
     */
    fun withBasicAuthStrategyDefaults() = apply {
        this.crumb = "DEFAULT_CRUMB_TOKEN_FOR_TESTING"
        this.strategy = "basic"
        this.timestamp = System.currentTimeMillis()
    }

    /**
     * FakeAuthStrategy에서 반환하는 값을 시뮬레이션합니다.
     *
     * @return 체이닝을 위한 Builder 자신
     */
    fun withFakeAuthStrategyDefaults() = apply {
        this.crumb = "FAKE_CRUMB_TOKEN_FOR_TESTING_12345"
        this.strategy = "fake"
        this.timestamp = System.currentTimeMillis()
    }

    /**
     * 현재 설정된 상태로 AuthResult를 생성합니다.
     *
     * @return 생성된 AuthResult
     */
    fun build(): AuthResult {
        return AuthResult(
            crumb = crumb,
            strategy = strategy,
            timestamp = timestamp
        )
    }

    /**
     * 현재 설정된 상태와 추가 CRUMB을 가진 AuthResult를 생성합니다.
     *
     * @param additionalCrumb 생성할 AuthResult의 개수
     * @return 생성된 AuthResult 목록
     */
    fun buildMultiple(additionalCrumb: Int): List<AuthResult> {
        return (0 until additionalCrumb).map {
            AuthResult(
                crumb = "$crumb-$it",
                strategy = strategy,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    /**
     * Builder의 현재 상태를 문자열로 반환합니다.
     *
     * @return 상태 정보 문자열
     */
    override fun toString(): String {
        return "TestAuthResultBuilder(" +
                "crumb='$crumb', " +
                "strategy='$strategy', " +
                "timestamp=$timestamp)"
    }
}

/**
 * TestAuthResultBuilder의 편의 확장 함수입니다.
 *
 * 사용 예시:
 * ```
 * val authResult = authResultBuilder {
 *     withCrumb("CUSTOM")
 *     withStrategy("extended")
 * }
 * ```
 */
fun authResultBuilder(block: TestAuthResultBuilder.() -> TestAuthResultBuilder): AuthResult {
    return block(TestAuthResultBuilder()).build()
}
