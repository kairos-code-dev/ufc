package com.ulalax.ufc.infrastructure.yahoo.streaming.internal

import kotlinx.serialization.Serializable

/**
 * WebSocket으로 수신되는 메시지 구조.
 *
 * Yahoo Finance WebSocket API는 JSON 형태로 메시지를 전송하며,
 * message 필드에 Base64로 인코딩된 Protobuf 데이터가 포함됩니다.
 *
 * @property message Base64로 인코딩된 Protobuf 데이터
 */
@Serializable
internal data class StreamingMessage(
    val message: String,
)
