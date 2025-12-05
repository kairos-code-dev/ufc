package com.ulalax.ufc.domain.exception

/**
 * UFC 애플리케이션에서 사용되는 에러 코드 정의.
 *
 * 에러 코드 범위:
 * - 1000번대: 네트워크/WebSocket 오류
 * - 2000번대: 인증 오류
 * - 3000번대: Rate Limiting 오류
 * - 4000번대: 데이터 오류
 * - 5000번대: 파싱 오류
 * - 6000번대: 파라미터/구독 오류
 * - 7000번대: 서버 오류
 * - 9000번대: 기타 오류
 */
enum class ErrorCode(
    val code: Int,
    val message: String,
    val isRetryableFlag: Boolean = false
) {
    // 1000번대: 네트워크/WebSocket 오류
    WEBSOCKET_CONNECTION_FAILED(1010, "WebSocket 연결에 실패했습니다.", true),
    WEBSOCKET_HANDSHAKE_FAILED(1011, "WebSocket 핸드셰이크에 실패했습니다.", true),
    WEBSOCKET_CLOSED_BY_SERVER(1012, "서버가 WebSocket 연결을 종료했습니다.", true),
    WEBSOCKET_PROTOCOL_ERROR(1013, "WebSocket 프로토콜 오류가 발생했습니다.", false),
    WEBSOCKET_MESSAGE_TOO_LARGE(1014, "WebSocket 메시지가 너무 큽니다.", false),
    STREAMING_RECONNECTION_FAILED(1020, "재연결에 실패했습니다.", false),

    // 2000번대: 인증 오류
    AUTHENTICATION_FAILED(2001, "인증에 실패했습니다.", false),

    // 3000번대: Rate Limiting 오류
    RATE_LIMIT_EXCEEDED(3001, "Rate Limit을 초과했습니다.", true),

    // 4000번대: 데이터 오류
    DATA_NOT_FOUND(4001, "요청한 데이터를 찾을 수 없습니다.", false),
    INVALID_SYMBOL(4002, "유효하지 않은 심볼입니다.", false),

    // 5000번대: 파싱 오류
    JSON_PARSING_ERROR(5001, "JSON 파싱 중 오류가 발생했습니다.", false),
    DATA_PARSING_ERROR(5010, "데이터 파싱 중 오류가 발생했습니다.", false),
    PROTOBUF_DECODING_ERROR(5011, "Protobuf 디코딩 중 오류가 발생했습니다.", false),

    // 6000번대: 파라미터/구독 오류
    INVALID_PARAMETER(6001, "유효하지 않은 파라미터입니다.", false),
    STREAMING_SUBSCRIPTION_FAILED(6010, "스트리밍 구독에 실패했습니다.", false),
    STREAMING_MAX_SUBSCRIPTIONS_EXCEEDED(6011, "최대 구독 개수를 초과했습니다.", false),

    // 7000번대: 서버 오류
    EXTERNAL_API_ERROR(7004, "외부 API 오류가 발생했습니다.", true),

    // 9000번대: 기타 오류
    CONFIGURATION_ERROR(9002, "설정 오류가 발생했습니다.", false),
}

/**
 * ErrorCode의 재시도 가능 여부를 반환합니다.
 *
 * 일반적으로 네트워크, 타임아웃, 서버 오류는 재시도 가능합니다.
 * 인증 오류, 파라미터 오류는 재시도 불가능합니다.
 *
 * @return 재시도 가능하면 true, 불가능하면 false
 */
fun ErrorCode.isRetryable(): Boolean = this.isRetryableFlag
