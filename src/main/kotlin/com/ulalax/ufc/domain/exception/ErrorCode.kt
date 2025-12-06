package com.ulalax.ufc.domain.exception

/**
 * Error code definitions used in the UFC application.
 *
 * Error code ranges:
 * - 1000s: Network/WebSocket errors
 * - 2000s: Authentication errors
 * - 3000s: Rate limiting errors
 * - 4000s: Data errors
 * - 5000s: Parsing errors
 * - 6000s: Parameter/Subscription errors
 * - 7000s: Server errors
 * - 9000s: Other errors
 */
enum class ErrorCode(
    val code: Int,
    val message: String,
    val isRetryableFlag: Boolean = false
) {
    // 1000s: Network/WebSocket errors
    WEBSOCKET_CONNECTION_FAILED(1010, "WebSocket connection failed.", true),
    WEBSOCKET_HANDSHAKE_FAILED(1011, "WebSocket handshake failed.", true),
    WEBSOCKET_CLOSED_BY_SERVER(1012, "Server closed WebSocket connection.", true),
    WEBSOCKET_PROTOCOL_ERROR(1013, "WebSocket protocol error occurred.", false),
    WEBSOCKET_MESSAGE_TOO_LARGE(1014, "WebSocket message is too large.", false),
    STREAMING_RECONNECTION_FAILED(1020, "Reconnection failed.", false),

    // 2000s: Authentication errors
    AUTHENTICATION_FAILED(2001, "Authentication failed.", false),

    // 3000s: Rate limiting errors
    RATE_LIMIT_EXCEEDED(3001, "Rate limit exceeded.", true),

    // 4000s: Data errors
    DATA_NOT_FOUND(4001, "Requested data not found.", false),
    INVALID_SYMBOL(4002, "Invalid symbol.", false),

    // 5000s: Parsing errors
    JSON_PARSING_ERROR(5001, "JSON parsing error occurred.", false),
    DATA_PARSING_ERROR(5010, "Data parsing error occurred.", false),
    PROTOBUF_DECODING_ERROR(5011, "Protobuf decoding error occurred.", false),

    // 6000s: Parameter/Subscription errors
    INVALID_PARAMETER(6001, "Invalid parameter.", false),
    STREAMING_SUBSCRIPTION_FAILED(6010, "Streaming subscription failed.", false),
    STREAMING_MAX_SUBSCRIPTIONS_EXCEEDED(6011, "Maximum subscription limit exceeded.", false),

    // 7000s: Server errors
    EXTERNAL_API_ERROR(7004, "External API error occurred.", true),

    // 9000s: Other errors
    CONFIGURATION_ERROR(9002, "Configuration error occurred.", false),
}

/**
 * Returns whether the ErrorCode is retryable.
 *
 * Generally, network, timeout, and server errors are retryable.
 * Authentication and parameter errors are not retryable.
 *
 * @return true if retryable, false otherwise
 */
fun ErrorCode.isRetryable(): Boolean = this.isRetryableFlag
