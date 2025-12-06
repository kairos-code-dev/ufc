package com.ulalax.ufc.domain.model.streaming

import com.ulalax.ufc.domain.exception.UfcException

/**
 * 스트리밍 연결 이벤트를 나타내는 sealed class.
 *
 * WebSocket 연결 상태 변화 및 에러를 표현합니다.
 */
sealed class StreamingEvent {
    /**
     * WebSocket 연결 성공
     */
    data object Connected : StreamingEvent()

    /**
     * WebSocket 연결 종료
     *
     * @property reason 종료 사유 (없을 경우 null)
     */
    data class Disconnected(
        val reason: String?,
    ) : StreamingEvent()

    /**
     * 재연결 시도 중
     *
     * @property attempt 재시도 횟수 (1부터 시작)
     */
    data class Reconnecting(
        val attempt: Int,
    ) : StreamingEvent()

    /**
     * 구독 목록 업데이트
     *
     * @property symbols 현재 구독 중인 심볼 목록
     */
    data class SubscriptionUpdated(
        val symbols: Set<String>,
    ) : StreamingEvent()

    /**
     * 에러 발생
     *
     * @property exception 발생한 예외
     * @property isFatal 복구 불가능한 에러인지 여부 (true일 경우 연결 종료됨)
     */
    data class Error(
        val exception: UfcException,
        val isFatal: Boolean,
    ) : StreamingEvent()
}
