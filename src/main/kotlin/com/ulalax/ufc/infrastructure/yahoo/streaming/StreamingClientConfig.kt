package com.ulalax.ufc.infrastructure.yahoo.streaming

/**
 * WebSocket Streaming 클라이언트 설정.
 *
 * Yahoo Finance WebSocket API 연결 및 동작을 제어합니다.
 *
 * @property webSocketUrl WebSocket 엔드포인트 URL
 * @property connectTimeoutMs 연결 타임아웃 (밀리초, 기본값: 10000ms = 10초)
 * @property heartbeatIntervalMs 하트비트 주기 (밀리초, 기본값: 15000ms = 15초)
 * @property pingTimeoutMs Ping 타임아웃 (밀리초, 기본값: 30000ms = 30초)
 * @property reconnection 재연결 설정
 * @property eventBufferSize Flow 버퍼 크기 (기본값: 64)
 * @property enableLogging 로깅 활성화 여부 (기본값: false)
 */
data class StreamingClientConfig(
    val webSocketUrl: String = "wss://streamer.finance.yahoo.com/?version=2",
    val connectTimeoutMs: Long = 10000,
    val heartbeatIntervalMs: Long = 15000,
    val pingTimeoutMs: Long = 30000,
    val reconnection: ReconnectionConfig = ReconnectionConfig(),
    val eventBufferSize: Int = 64,
    val enableLogging: Boolean = false,
) {
    init {
        require(webSocketUrl.isNotBlank()) { "webSocketUrl must not be blank" }
        require(connectTimeoutMs > 0) { "connectTimeoutMs must be positive" }
        require(heartbeatIntervalMs > 0) { "heartbeatIntervalMs must be positive" }
        require(pingTimeoutMs > heartbeatIntervalMs) { "pingTimeoutMs must be > heartbeatIntervalMs" }
        require(eventBufferSize > 0) { "eventBufferSize must be positive" }
    }
}
