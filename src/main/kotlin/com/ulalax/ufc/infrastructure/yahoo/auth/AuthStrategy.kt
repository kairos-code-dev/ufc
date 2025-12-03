package com.ulalax.ufc.infrastructure.yahoo.auth

/**
 * Yahoo Finance API 인증 전략을 정의하는 인터페이스입니다.
 *
 * 다양한 인증 방식을 구현할 수 있도록 설계되었습니다.
 * 현재는 BasicAuthStrategy를 기본 구현으로 제공합니다.
 *
 * Strategy 패턴을 사용하여 런타임에 인증 방식을 전환할 수 있으며,
 * 향후 다른 인증 방식이 필요한 경우 쉽게 추가할 수 있습니다.
 *
 * ## 구현 시 고려사항:
 * - 모든 구현은 suspend fun이므로 장시간 작업(네트워크 I/O)을 허용합니다.
 * - 인증 실패 시 명확한 예외를 발생시켜야 합니다.
 * - 인증 결과는 AuthResult로 래핑되어 타임스탐프와 함께 저장됩니다.
 */
fun interface AuthStrategy {
    /**
     * Yahoo Finance API 인증을 수행합니다.
     *
     * 이 메서드는 suspend이므로 네트워크 I/O 대기 시간 동안 코루틴을 일시 중지합니다.
     * 구현체는 다음을 담당합니다:
     * - 인증 엔드포인트 호출
     * - 쿠키/토큰 획득 및 관리
     * - 응답 검증 및 파싱
     * - 필요한 경우 예외 발생
     *
     * @return 인증 결과를 포함하는 AuthResult 객체
     * @throws UfcException 인증 실패 시 발생
     *         - CRUMB_ACQUISITION_FAILED: CRUMB 토큰 획득 실패
     *         - AUTHENTICATION_FAILED: 기타 인증 실패
     *         - NETWORK_*: 네트워크 관련 오류
     */
    suspend fun authenticate(): AuthResult
}
