package com.ulalax.ufc.infrastructure.yahoo.internal.auth

/**
 * Yahoo Finance API 인증 결과를 캡슐화하는 데이터 클래스입니다.
 *
 * Crumb 토큰과 인증 메타데이터를 포함하며, 인증 유효성 검증을 지원합니다.
 * 이 클래스는 immutable이며 동시성 안전합니다.
 *
 * @property crumb Yahoo Finance API 호출에 필요한 CRUMB 토큰
 * @property strategy 사용된 인증 전략 (예: "basic")
 * @property timestamp 인증이 수행된 시간 (밀리초 단위, 에포크 기준)
 */
data class AuthResult(
    val crumb: String,
    val strategy: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 인증 결과의 유효성을 검증합니다.
     *
     * 인증은 획득 후 1시간 이내에 유효하며, 그 이후는 재인증이 필요합니다.
     * Yahoo Finance API의 CRUMB 토큰은 타임바운드 토큰으로, 시간이 경과하면
     * 유효하지 않을 수 있습니다.
     *
     * @return 인증이 유효하면 true, 만료되었으면 false
     */
    fun isValid(): Boolean {
        val ONE_HOUR_IN_MILLIS = 60 * 60 * 1000L
        val elapsedTime = System.currentTimeMillis() - timestamp
        return elapsedTime < ONE_HOUR_IN_MILLIS
    }

    /**
     * 인증 결과의 정보를 명확하게 표현합니다.
     *
     * @return 인증 정보의 문자열 표현
     */
    override fun toString(): String {
        val elapsedSeconds = (System.currentTimeMillis() - timestamp) / 1000
        val isValidStr = if (isValid()) "유효" else "만료됨"
        return "AuthResult(strategy='$strategy', crumb_length=${crumb.length}, " +
                "elapsed_sec=$elapsedSeconds, status='$isValidStr')"
    }
}
