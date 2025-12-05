# WebSocket Streaming API 기술 명세서

> **Version**: 2.0
> **작성일**: 2025-12-05

---

## 1. 개요

### 1.1 API 목적

Yahoo Finance WebSocket Streaming API를 통해 **실시간 주식 시세 데이터**를 지속적으로 수신한다.

| 항목 | 값 |
|-----|---|
| 엔드포인트 | `wss://streamer.finance.yahoo.com/?version=2` |
| 프로토콜 | WebSocket Secure (WSS) |
| 인증 | 불필요 |
| 데이터 형식 | JSON (외부), Protobuf (내부) |

### 1.2 제공 데이터

| 데이터 | 설명 |
|-------|------|
| 실시간 현재가 | 가격 변동 시 즉시 푸시 |
| 등락폭/등락률 | 전일 대비 변화량 |
| 거래량 | 당일 누적 거래량 |
| 호가 정보 | 최우선 매수/매도 호가 및 수량 |
| 당일 고가/저가 | 당일 최고/최저 가격 |
| 시장 상태 | 장중/장외/프리마켓/포스트마켓 |
| 자산 정보 | 통화, 거래소, 자산 유형 |

### 1.3 REST API와의 차이점

| 구분 | WebSocket Streaming | REST API |
|------|-------------------|----------|
| 통신 방식 | 양방향 지속 연결 | 요청-응답 단방향 |
| 데이터 전송 | 서버 푸시 (자동) | 클라이언트 폴링 (수동) |
| 지연 시간 | 수백 ms 이하 | 수초 |
| 사용 목적 | 실시간 모니터링 | 이력 조회, 상세 정보 |
| 다중 심볼 | 한 연결로 다수 구독 | 각 심볼마다 별도 요청 |

---

## 2. 데이터 소스 분석

### 2.1 WebSocket 연결

| 항목 | 값 |
|-----|---|
| URL | `wss://streamer.finance.yahoo.com/?version=2` |
| 버전 | 2 (쿼리 파라미터) |
| 핸드셰이크 | HTTP/1.1 Upgrade → 101 Switching Protocols |

### 2.2 연결 헤더

| 헤더 | 값 | 필수 |
|-----|---|------|
| User-Agent | 브라우저 User-Agent | 권장 |
| Origin | `https://finance.yahoo.com` | 권장 |
| Sec-WebSocket-Version | 13 | Yes |

### 2.3 구독 메시지 형식

#### 2.3.1 구독 요청

```json
{
  "subscribe": ["AAPL", "GOOGL", "MSFT"]
}
```

| 필드 | 타입 | 설명 |
|-----|------|------|
| subscribe | Array&lt;String&gt; | 구독할 심볼 목록 |

#### 2.3.2 구독 해제

```json
{
  "unsubscribe": ["AAPL"]
}
```

| 필드 | 타입 | 설명 |
|-----|------|------|
| unsubscribe | Array&lt;String&gt; | 구독 해제할 심볼 목록 |

#### 2.3.3 하트비트

| 항목 | 값 |
|-----|---|
| 목적 | 연결 유지 및 타임아웃 방지 |
| 주기 | 15초 권장 |
| 형식 | 구독 메시지 재전송 |

### 2.4 수신 메시지 형식

#### 2.4.1 메시지 구조

```json
{
  "message": "base64EncodedProtobufData"
}
```

| 필드 | 타입 | 설명 |
|-----|------|------|
| message | String | Base64 인코딩된 Protobuf 데이터 |

#### 2.4.2 메시지 처리 흐름

| 단계 | 작업 |
|-----|------|
| 1 | WebSocket 메시지 수신 (JSON 문자열) |
| 2 | JSON 파싱, `message` 필드 추출 |
| 3 | Base64 디코딩 (바이너리 데이터로 변환) |
| 4 | Protobuf 파싱 (PricingData 역직렬화) |
| 5 | Domain 모델 변환 (StreamingPrice/StreamingQuote) |
| 6 | Flow 이벤트 방출 |

### 2.5 Protobuf 메시지 정의

#### 2.5.1 PricingData 필드

| 필드 번호 | 필드명 | 타입 | 설명 |
|---------|-------|------|------|
| 1 | id | string | 심볼 (예: "AAPL") |
| 2 | price | float | 현재가 |
| 3 | time | sint64 | Unix timestamp (초) |
| 4 | currency | string | 통화 (예: "USD") |
| 5 | exchange | string | 거래소 (예: "NMS") |
| 6 | quote_type | int32 | 자산 유형 코드 |
| 7 | market_hours | int32 | 시장 시간 상태 |
| 8 | change_percent | float | 등락률 (%) |
| 9 | day_volume | sint64 | 당일 거래량 |
| 10 | day_high | float | 당일 고가 |
| 11 | day_low | float | 당일 저가 |
| 12 | change | float | 등락폭 |
| 13 | short_name | string | 자산명 |
| 14 | expire_date | sint64 | 만료일 (옵션) |
| 15 | open_price | float | 시가 |
| 16 | previous_close | float | 전일 종가 |
| 17 | strike_price | float | 행사가 (옵션) |
| 18 | underlying_symbol | string | 기초자산 (옵션) |
| 19 | open_interest | sint64 | 미결제약정 (옵션) |
| 20 | options_type | sint64 | Call/Put |
| 21 | mini_option | sint64 | 미니 옵션 여부 |
| 22 | last_size | sint64 | 최근 체결 수량 |
| 23 | bid | float | 매수 호가 |
| 24 | bid_size | sint64 | 매수 호가 수량 |
| 25 | ask | float | 매도 호가 |
| 26 | ask_size | sint64 | 매도 호가 수량 |
| 27 | price_hint | sint64 | 소수점 자릿수 힌트 |
| 28 | vol_24hr | sint64 | 24시간 거래량 (암호화폐) |
| 29 | vol_all_currencies | sint64 | 전체 통화 거래량 (암호화폐) |
| 30 | from_currency | string | 기준 통화 (환율) |
| 31 | last_market | string | 최근 체결 시장 |
| 32 | circulating_supply | double | 유통량 (암호화폐) |
| 33 | market_cap | double | 시가총액 |

#### 2.5.2 market_hours 코드

| 코드 | 의미 |
|-----|-----|
| 0 | CLOSED (장외) |
| 1 | REGULAR (정규장) |
| 2 | PRE_MARKET (프리마켓) |
| 3 | POST_MARKET (포스트마켓) |

#### 2.5.3 quote_type 코드

| 코드 | 의미 |
|-----|-----|
| 1 | EQUITY (주식) |
| 2 | ETF (상장지수펀드) |
| 5 | OPTION (옵션) |
| 6 | MUTUAL_FUND (뮤추얼펀드) |
| 8 | INDEX (인덱스) |
| 11 | CRYPTOCURRENCY (암호화폐) |
| 12 | CURRENCY (환율) |
| 13 | FUTURE (선물) |

---

## 3. 데이터/모델 설계

### 3.1 Domain 모델

#### StreamingPrice

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| symbol | String | No | 심볼 |
| price | Double | No | 현재가 |
| change | Double | No | 등락폭 |
| changePercent | Double | No | 등락률 (%) |
| timestamp | Long | No | Unix timestamp (초) |
| volume | Long | No | 당일 거래량 |
| bid | Double | Yes | 매수 호가 |
| ask | Double | Yes | 매도 호가 |
| marketHours | MarketHours | No | 시장 시간 상태 |

#### StreamingQuote

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| symbol | String | No | 심볼 |
| price | Double | No | 현재가 |
| change | Double | No | 등락폭 |
| changePercent | Double | No | 등락률 (%) |
| timestamp | Long | No | Unix timestamp (초) |
| volume | Long | No | 당일 거래량 |
| bid | Double | Yes | 매수 호가 |
| ask | Double | Yes | 매도 호가 |
| marketHours | MarketHours | No | 시장 시간 상태 |
| dayHigh | Double | No | 당일 고가 |
| dayLow | Double | No | 당일 저가 |
| openPrice | Double | No | 시가 |
| previousClose | Double | No | 전일 종가 |
| bidSize | Long | Yes | 매수 호가 수량 |
| askSize | Long | Yes | 매도 호가 수량 |
| currency | String | No | 통화 |
| exchange | String | No | 거래소 |
| shortName | String | No | 자산명 |
| quoteType | QuoteType | No | 자산 유형 |

#### MarketHours

```kotlin
enum class MarketHours {
    PRE_MARKET,
    REGULAR,
    POST_MARKET,
    CLOSED,
    UNKNOWN
}
```

#### QuoteType

```kotlin
enum class QuoteType {
    EQUITY,
    ETF,
    INDEX,
    MUTUAL_FUND,
    OPTION,
    CRYPTOCURRENCY,
    CURRENCY,
    FUTURE,
    UNKNOWN
}
```

#### StreamingEvent

```kotlin
sealed class StreamingEvent {
    data object Connected : StreamingEvent()
    data class Disconnected(val reason: String?) : StreamingEvent()
    data class Reconnecting(val attempt: Int) : StreamingEvent()
    data class SubscriptionUpdated(val symbols: Set<String>) : StreamingEvent()
    data class Error(val exception: UfcException, val isFatal: Boolean) : StreamingEvent()
}
```

### 3.2 Internal Request 모델

#### SubscribeMessage

| 필드 | 타입 |
|-----|------|
| subscribe | List&lt;String&gt; |

#### UnsubscribeMessage

| 필드 | 타입 |
|-----|------|
| unsubscribe | List&lt;String&gt; |

### 3.3 Internal Response 모델

#### StreamingMessage

| 필드 | 타입 | 설명 |
|-----|------|------|
| message | String | Base64 인코딩된 Protobuf 데이터 |

### 3.4 API 메서드 시그니처

#### StreamingClient

```kotlin
// 생성
companion object {
    fun create(config: StreamingClientConfig = StreamingClientConfig()): StreamingClient
}

// 구독 관리
suspend fun subscribe(symbol: String)
suspend fun subscribe(symbols: List<String>)
suspend fun unsubscribe(symbol: String)
suspend fun unsubscribe(symbols: List<String>)
suspend fun unsubscribeAll()
fun getSubscribedSymbols(): Set<String>

// 데이터 수신
val prices: SharedFlow<StreamingPrice>
val quotes: SharedFlow<StreamingQuote>
val events: SharedFlow<StreamingEvent>
fun pricesBySymbol(symbol: String): Flow<StreamingPrice>
fun quotesBySymbol(symbol: String): Flow<StreamingQuote>

// 연결 제어
suspend fun connect()
suspend fun disconnect()
fun isConnected(): Boolean
override fun close()
```

#### StreamingClientConfig

| 필드 | 타입 | 기본값 | 설명 |
|-----|------|-------|------|
| webSocketUrl | String | wss://streamer.finance.yahoo.com/?version=2 | WebSocket URL |
| connectTimeoutMs | Long | 10000 | 연결 타임아웃 (ms) |
| heartbeatIntervalMs | Long | 15000 | 하트비트 주기 (ms) |
| pingTimeoutMs | Long | 30000 | Ping 타임아웃 (ms) |
| reconnection | ReconnectionConfig | ReconnectionConfig() | 재연결 설정 |
| eventBufferSize | Int | 64 | Flow 버퍼 크기 |
| enableLogging | Boolean | false | 로깅 활성화 |

#### ReconnectionConfig

| 필드 | 타입 | 기본값 | 설명 |
|-----|------|-------|------|
| enabled | Boolean | true | 재연결 활성화 |
| maxAttempts | Int | 5 | 최대 재시도 횟수 |
| initialDelayMs | Long | 1000 | 초기 대기 시간 (ms) |
| maxDelayMs | Long | 30000 | 최대 대기 시간 (ms) |
| backoffMultiplier | Double | 2.0 | 지수 백오프 배수 |

### 3.5 필드 매핑

#### PricingData → StreamingPrice

| Protobuf 필드 | Domain 필드 | 변환 | 비고 |
|--------------|------------|------|------|
| id | symbol | String | 직접 매핑 |
| price | price | Float → Double | 직접 매핑 |
| change | change | Float → Double | 직접 매핑 |
| change_percent | changePercent | Float → Double | 직접 매핑 |
| time | timestamp | Int64 → Long | Unix timestamp |
| day_volume | volume | Int64 → Long | 직접 매핑 |
| bid | bid | Float → Double? | 0 → null |
| ask | ask | Float → Double? | 0 → null |
| market_hours | marketHours | Int32 → Enum | 코드 매핑 |

#### PricingData → StreamingQuote

| Protobuf 필드 | Domain 필드 | 변환 | 비고 |
|--------------|------------|------|------|
| (StreamingPrice 모든 필드 포함) | | | |
| day_high | dayHigh | Float → Double | 직접 매핑 |
| day_low | dayLow | Float → Double | 직접 매핑 |
| open_price | openPrice | Float → Double | 직접 매핑 |
| previous_close | previousClose | Float → Double | 직접 매핑 |
| bid_size | bidSize | Int64 → Long? | 0 → null |
| ask_size | askSize | Int64 → Long? | 0 → null |
| currency | currency | String | 직접 매핑 |
| exchange | exchange | String | 직접 매핑 |
| short_name | shortName | String | 직접 매핑 |
| quote_type | quoteType | Int32 → Enum | 코드 매핑 |

### 3.6 Nullable 변환 규칙

| 원본 타입 | 기본값 | Domain 변환 |
|---------|-------|-----------|
| float | 0.0 | 0 → null (선택적 필드만) |
| sint64/int64 | 0 | 0 → null (선택적 필드만) |
| string | "" | "" → null |
| 필수 필드 | - | null 불가 |

---

## 4. 연결 관리

### 4.1 연결 상태

| 상태 | 설명 |
|-----|------|
| Disconnected | 초기 상태, 연결 전 |
| Connecting | 연결 시도 중 |
| Connected | 연결 성공, 데이터 수신 가능 |
| Reconnecting | 재연결 시도 중 |
| Closed | 최종 종료 상태 |

### 4.2 연결 수립

| 방식 | 트리거 | 동작 |
|-----|-------|------|
| 자동 연결 | subscribe() 호출 | 연결되지 않았다면 자동 연결 |
| 명시적 연결 | connect() 호출 | 즉시 연결 수립 (구독 없음) |

### 4.3 재연결 정책

#### 4.3.1 재연결 트리거

| 조건 | 재연결 여부 |
|-----|----------|
| 네트워크 연결 끊김 | Yes |
| WebSocket Close Frame 수신 | Yes |
| Ping/Pong 타임아웃 | Yes |
| 사용자 명시적 disconnect() | No |
| enabled=false 설정 | No |
| 최대 재시도 횟수 초과 | No |
| 인증 실패 | No |

#### 4.3.2 재연결 지연 계산

```
대기 시간 = min(initialDelay × (backoffMultiplier ^ attempt), maxDelay)
```

| 재시도 | 기본 설정 대기 시간 |
|-------|-----------------|
| 1차 | 1초 |
| 2차 | 2초 |
| 3차 | 4초 |
| 4차 | 8초 |
| 5차 | 16초 |
| 6차 이후 | 30초 (maxDelay) |

### 4.4 구독 복원

| 항목 | 동작 |
|-----|------|
| 재연결 성공 시 | 이전 구독 목록 자동 복원 |
| 구독 메시지 | 전체 구독 목록 재전송 |
| 하트비트 | 타이머 재시작 |

### 4.5 타임아웃 감지

| 타임아웃 | 기본값 | 동작 |
|---------|-------|------|
| Connect Timeout | 10초 | 연결 실패 에러 |
| Ping Timeout | 30초 | 재연결 트리거 |
| Heartbeat Interval | 15초 | 구독 메시지 재전송 |

---

## 5. 예외 처리

### 5.1 ErrorCode 매핑

| 에러 케이스 | ErrorCode | 재시도 |
|-----------|-----------|-------|
| 연결 실패 | WEBSOCKET_CONNECTION_FAILED | Yes |
| 핸드셰이크 실패 | WEBSOCKET_HANDSHAKE_FAILED | Yes |
| 서버 연결 종료 | WEBSOCKET_CLOSED_BY_SERVER | Yes |
| 프로토콜 위반 | WEBSOCKET_PROTOCOL_ERROR | No |
| 메시지 크기 초과 | WEBSOCKET_MESSAGE_TOO_LARGE | No |
| JSON 파싱 실패 | JSON_PARSING_ERROR | No |
| Protobuf 디코딩 실패 | PROTOBUF_DECODING_ERROR | No |
| 잘못된 데이터 형식 | INVALID_DATA_FORMAT | No |
| 구독 실패 | STREAMING_SUBSCRIPTION_FAILED | No |
| 구독 한도 초과 | STREAMING_MAX_SUBSCRIPTIONS_EXCEEDED | No |
| 재연결 최대 횟수 초과 | STREAMING_RECONNECTION_FAILED | No |
| 연결 타임아웃 | NETWORK_TIMEOUT | Yes |
| Rate Limit | RATE_LIMIT_EXCEEDED | Yes |

### 5.2 신규 ErrorCode

| 에러 코드 | 코드 번호 | 메시지 |
|---------|---------|-------|
| WEBSOCKET_CONNECTION_FAILED | 1010 | WebSocket 연결에 실패했습니다. |
| WEBSOCKET_HANDSHAKE_FAILED | 1011 | WebSocket 핸드셰이크에 실패했습니다. |
| WEBSOCKET_CLOSED_BY_SERVER | 1012 | 서버가 WebSocket 연결을 종료했습니다. |
| WEBSOCKET_PROTOCOL_ERROR | 1013 | WebSocket 프로토콜 오류가 발생했습니다. |
| WEBSOCKET_MESSAGE_TOO_LARGE | 1014 | WebSocket 메시지가 너무 큽니다. |
| PROTOBUF_DECODING_ERROR | 5011 | Protobuf 디코딩 중 오류가 발생했습니다. |
| STREAMING_SUBSCRIPTION_FAILED | 6010 | 스트리밍 구독에 실패했습니다. |
| STREAMING_MAX_SUBSCRIPTIONS_EXCEEDED | 6011 | 최대 구독 개수를 초과했습니다. |
| STREAMING_RECONNECTION_FAILED | 1020 | 재연결에 실패했습니다. |

### 5.3 에러 분류

| 분류 | 처리 방식 |
|-----|---------|
| 복구 가능 | 자동 재연결 시도, Reconnecting 이벤트 방출 |
| 복구 불가능 | Error 이벤트 방출 후 연결 종료 |
| 일시적 | 해당 메시지 무시, 연결 유지, 로그 기록 |

### 5.4 메시지 파싱 실패 처리

| 상황 | 처리 |
|-----|------|
| JSON 파싱 실패 | 로그 기록, 메시지 무시, 연결 유지 |
| Base64 디코딩 실패 | 로그 기록, 메시지 무시, 연결 유지 |
| Protobuf 파싱 실패 | 로그 기록, 메시지 무시, 연결 유지 |

---

## 6. 참고 자료

### 6.1 API 제약사항

| 제약 | 설명 |
|-----|------|
| 비공식 API | 문서화되지 않음, 변경 가능 |
| 연결 유지 필요 | 장시간 연결로 인한 리소스 사용 |
| 이력 데이터 없음 | 과거 데이터는 REST API 사용 |
| 상세 정보 제한 | 재무제표 등 미제공 |

### 6.2 의존성

| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| io.ktor:ktor-client-websockets | 3.0.1 | WebSocket 클라이언트 |
| com.google.protobuf:protobuf-kotlin | 3.24.0+ | Protobuf 파싱 (Option 1) |
| kotlinx-serialization-protobuf | 1.7.3 | Protobuf 파싱 (Option 2) |

### 6.3 용어

| 용어 | 설명 |
|-----|------|
| WebSocket | 양방향 지속 통신 프로토콜 |
| Protobuf | Protocol Buffers, 구조화된 데이터 직렬화 |
| SharedFlow | 여러 구독자가 공유하는 핫 Flow |
| 백오프 | 재시도 간격을 점진적으로 늘리는 전략 |
| 하트비트 | 연결 유지를 위한 주기적 메시지 |
| 멱등성 | 동일 요청을 여러 번 수행해도 결과 동일 |
