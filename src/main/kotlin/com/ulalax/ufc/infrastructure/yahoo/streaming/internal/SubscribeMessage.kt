package com.ulalax.ufc.infrastructure.yahoo.streaming.internal

import kotlinx.serialization.Serializable

/**
 * 구독 요청 메시지.
 *
 * WebSocket을 통해 전송되는 구독 요청을 나타냅니다.
 *
 * @property subscribe 구독할 심볼 목록
 */
@Serializable
internal data class SubscribeMessage(
    val subscribe: List<String>,
)
