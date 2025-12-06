package com.ulalax.ufc.infrastructure.yahoo.streaming.internal

import kotlinx.serialization.Serializable

/**
 * 구독 해제 요청 메시지.
 *
 * WebSocket을 통해 전송되는 구독 해제 요청을 나타냅니다.
 *
 * @property unsubscribe 구독 해제할 심볼 목록
 */
@Serializable
internal data class UnsubscribeMessage(
    val unsubscribe: List<String>,
)
