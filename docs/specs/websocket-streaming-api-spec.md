# WebSocket Streaming API 기능 명세서

> **Version**: 1.0
> **작성일**: 2025-12-05
> **대상**: UFC 프로젝트에 Yahoo Finance WebSocket 실시간 스트리밍 기능 통합

---

## 목차

- [1. API 개요](#1-api-개요)
- [2. Yahoo Finance WebSocket API 분석](#2-yahoo-finance-websocket-api-분석)
- [3. UFC 통합 설계](#3-ufc-통합-설계)
- [4. 연결 관리](#4-연결-관리)
- [5. 데이터 매핑](#5-데이터-매핑)
- [6. 에러 처리](#6-에러-처리)
- [7. 테스트 전략](#7-테스트-전략)
- [8. 구현 우선순위](#8-구현-우선순위)

---

## 1. API 개요

### 1.1 WebSocket Streaming의 목적

Yahoo Finance WebSocket Streaming API는 **실시간 주식 시세 데이터를 지속적으로 수신**하기 위한 양방향 통신 채널을 제공합니다. REST API와 달리 클라이언트가 반복적으로 요청할 필요 없이, 서버가 데이터 변경 시 자동으로 푸시합니다.

### 1.2 주요 사용 사례

1. **실시간 시세 모니터링**: 특정 종목들의 가격 변동을 실시간으로 추적
2. **실시간 체결 정보**: 거래량, 체결가, 호가 등의 실시간 업데이트
3. **다중 종목 동시 모니터링**: 포트폴리오나 관심 종목 리스트의 실시간 추적
4. **실시간 알림 시스템**: 특정 가격 도달 시 알림 트리거
5. **라이브 차트 업데이트**: 실시간 차트 데이터 갱신

### 1.3 REST API와의 차이점

| 구분 | WebSocket Streaming | REST API (chart, quoteSummary) |
|------|-------------------|-------------------------------|
| **통신 방식** | 양방향 지속 연결 | 요청-응답 단방향 |
| **데이터 전송** | 서버 푸시 (변경 시 자동 전송) | 클라이언트 폴링 (요청 시에만 응답) |
| **지연 시간** | 극히 낮음 (수백 ms 이하) | 상대적으로 높음 (수초) |
| **네트워크 효율** | 높음 (연결 유지, 변경 시만 전송) | 낮음 (매 요청마다 재연결) |
| **사용 목적** | 실시간 모니터링 | 이력 데이터, 상세 정보 조회 |
| **데이터 범위** | 현재가 중심의 실시간 데이터 | 과거 이력, 모듈별 상세 정보 |
| **다중 심볼** | 한 연결로 여러 심볼 구독 | 각 심볼마다 별도 요청 필요 |
| **리소스 사용** | 연결 유지 필요 (메모리) | 필요 시만 사용 |

### 1.4 WebSocket Streaming의 장점

- **초저지연**: 시장 데이터 변경 후 수백 ms 이내 수신
- **효율적 리소스 사용**: 변경된 데이터만 전송
- **간단한 다중 구독**: 하나의 연결로 수십~수백 개 심볼 동시 모니터링
- **자동 업데이트**: 폴링 로직 불필요

### 1.5 WebSocket Streaming의 제약사항

- **연결 유지 필요**: 장시간 연결 유지로 인한 리소스 사용
- **재연결 관리 필요**: 네트워크 끊김 시 자동 재연결 로직 필수
- **이력 데이터 없음**: 과거 데이터는 REST API로 조회 필요
- **상세 정보 제한**: 현재가/거래량 중심이며 재무제표 등은 미제공
- **비공식 API**: Yahoo의 정책 변경 가능성

---

## 2. Yahoo Finance WebSocket API 분석

### 2.1 WebSocket URL

```
wss://streamer.finance.yahoo.com/?version=2
```

**프로토콜**: WebSocket Secure (WSS)
**버전**: 2 (쿼리 파라미터로 지정)

### 2.2 연결 프로토콜 및 핸드셰이크

#### 2.2.1 연결 수립 과정

1. **WebSocket 연결 시도**
   - URL: `wss://streamer.finance.yahoo.com/?version=2`
   - 표준 WebSocket 핸드셰이크 수행
   - HTTP/1.1 Upgrade 요청

2. **연결 성공 확인**
   - HTTP 101 Switching Protocols 응답 수신
   - WebSocket 연결 활성화

3. **초기 구독 메시지 전송**
   - 연결 직후 구독할 심볼 목록 전송
   - JSON 형식의 구독 메시지

#### 2.2.2 연결 헤더 요구사항

- **User-Agent**: 브라우저 User-Agent 권장
- **Origin**: `https://finance.yahoo.com` 권장
- **Sec-WebSocket-Version**: 13 (표준)

### 2.3 구독 메시지 포맷

#### 2.3.1 구독 요청 (Subscribe)

```json
{
  "subscribe": ["AAPL", "GOOGL", "MSFT"]
}
```

**메시지 구조**:
- `subscribe`: 구독할 심볼 배열
- 심볼은 Yahoo Finance 표준 포맷 사용 (예: "BTC-USD", "^GSPC")
- 한 번에 여러 심볼 구독 가능

#### 2.3.2 구독 해제 (Unsubscribe)

```json
{
  "unsubscribe": ["AAPL"]
}
```

**메시지 구조**:
- `unsubscribe`: 구독 해제할 심볼 배열
- 개별 심볼 또는 일부 심볼만 선택적 해제 가능

#### 2.3.3 하트비트 구독 (Heartbeat)

**목적**: 연결 유지 및 구독 상태 갱신

**동작 방식**:
- 주기적으로 (15초 간격 권장) 구독 메시지 재전송
- 서버에게 연결이 활성 상태임을 알림
- 타임아웃 방지

**하트비트 메시지**:
```json
{
  "subscribe": ["AAPL", "GOOGL", "MSFT"]
}
```
- 초기 구독과 동일한 메시지 전송
- 현재 구독 중인 전체 심볼 목록 포함

### 2.4 수신 메시지 포맷

#### 2.4.1 메시지 구조

서버에서 수신하는 메시지는 다음과 같은 JSON 구조를 가집니다:

```json
{
  "message": "base64EncodedProtobufData"
}
```

**필드 설명**:
- `message`: Base64로 인코딩된 Protobuf 바이너리 데이터
- Protobuf 디코딩 후 PricingData 메시지 획득

#### 2.4.2 메시지 처리 흐름

1. **WebSocket 메시지 수신**: JSON 문자열 수신
2. **JSON 파싱**: `message` 필드 추출
3. **Base64 디코딩**: 바이너리 데이터로 변환
4. **Protobuf 파싱**: PricingData 객체로 역직렬화
5. **도메인 모델 변환**: UFC 모델로 매핑
6. **Flow 이벤트 방출**: Kotlin Flow를 통해 구독자에게 전달

### 2.5 Protobuf 메시지 구조

#### 2.5.1 PricingData 메시지 정의

Yahoo Finance는 `PricingData` Protobuf 메시지를 사용합니다:

```protobuf
syntax = "proto3";

message PricingData {
    string id = 1;                    // 심볼 (예: "AAPL")
    float price = 2;                  // 현재가
    sint64 time = 3;                  // 타임스탬프 (Unix timestamp, seconds)
    string currency = 4;              // 통화 (예: "USD")
    string exchange = 5;              // 거래소 (예: "NMS")
    int32 quote_type = 6;             // 자산 유형 코드
    int32 market_hours = 7;           // 시장 시간 상태 (장중/장외)
    float change_percent = 8;         // 등락률 (%)
    sint64 day_volume = 9;            // 당일 거래량
    float day_high = 10;              // 당일 고가
    float day_low = 11;               // 당일 저가
    float change = 12;                // 등락폭 (절대값)
    string short_name = 13;           // 짧은 이름
    sint64 expire_date = 14;          // 만료일 (옵션 전용)
    float open_price = 15;            // 시가
    float previous_close = 16;        // 전일 종가
    float strike_price = 17;          // 행사가 (옵션 전용)
    string underlying_symbol = 18;    // 기초자산 심볼 (옵션 전용)
    sint64 open_interest = 19;        // 미결제약정 (옵션 전용)
    sint64 options_type = 20;         // 옵션 타입 (Call/Put)
    sint64 mini_option = 21;          // 미니 옵션 여부
    sint64 last_size = 22;            // 최근 체결 크기
    float bid = 23;                   // 매수 호가
    sint64 bid_size = 24;             // 매수 호가 수량
    float ask = 25;                   // 매도 호가
    sint64 ask_size = 26;             // 매도 호가 수량
    sint64 price_hint = 27;           // 가격 표시 힌트 (소수점 자릿수)
    sint64 vol_24hr = 28;             // 24시간 거래량 (암호화폐 전용)
    sint64 vol_all_currencies = 29;   // 전체 통화 거래량 (암호화폐 전용)
    string from_currency = 30;        // 변환 전 통화 (환율 전용)
    string last_market = 31;          // 마지막 체결 시장
    double circulating_supply = 32;   // 유통량 (암호화폐 전용)
    double market_cap = 33;           // 시가총액
}
```

#### 2.5.2 필드별 상세 설명

**기본 정보**:
- `id`: 심볼 식별자 (구독한 심볼과 동일)
- `short_name`: 자산의 짧은 이름 (예: "Apple Inc.")
- `currency`: 가격 표시 통화 코드 (ISO 4217)
- `exchange`: 거래소 코드 (예: "NMS"=NASDAQ, "NYQ"=NYSE)

**가격 정보**:
- `price`: 실시간 현재가
- `open_price`: 당일 시가
- `previous_close`: 전일 종가
- `day_high`: 당일 최고가
- `day_low`: 당일 최저가
- `change`: 등락폭 (price - previous_close)
- `change_percent`: 등락률 백분율

**거래 정보**:
- `day_volume`: 당일 누적 거래량
- `last_size`: 최근 체결 수량
- `time`: 데이터 갱신 타임스탬프

**호가 정보**:
- `bid`: 최우선 매수 호가
- `bid_size`: 매수 호가 수량
- `ask`: 최우선 매도 호가
- `ask_size`: 매도 호가 수량

**시장 상태**:
- `market_hours`: 시장 시간 상태
  - 값 예시: 0 (장외), 1 (장중), 2 (프리마켓), 3 (포스트마켓)
- `quote_type`: 자산 유형 코드
  - 값 예시: 1 (주식), 2 (ETF), 5 (옵션), 6 (뮤추얼펀드), 8 (인덱스), 11 (암호화폐), 12 (환율)

**옵션 전용 필드**:
- `strike_price`: 행사가
- `expire_date`: 만료일 (Unix timestamp)
- `underlying_symbol`: 기초자산 심볼
- `open_interest`: 미결제약정
- `options_type`: Call(0) 또는 Put(1)
- `mini_option`: 미니 옵션 여부

**암호화폐 전용 필드**:
- `vol_24hr`: 24시간 거래량
- `vol_all_currencies`: 모든 통화 합산 거래량
- `circulating_supply`: 유통 공급량
- `market_cap`: 시가총액

**기타**:
- `price_hint`: 가격 소수점 자릿수 힌트 (10^(-price_hint)로 해석)
- `from_currency`: 환율 쌍의 기준 통화
- `last_market`: 최근 체결이 발생한 시장

#### 2.5.3 필드 존재 여부

Protobuf proto3 특성상 모든 필드는 선택적(optional)입니다:
- 값이 설정되지 않은 필드는 기본값 (0, "", 등)
- 자산 유형에 따라 일부 필드만 의미 있음
  - 주식: 기본 가격/거래량 필드 + 호가 정보
  - 옵션: 옵션 전용 필드 추가
  - 암호화폐: 암호화폐 전용 필드 추가

---

## 3. UFC 통합 설계

### 3.1 기존 아키텍처와의 통합

#### 3.1.1 아키�ecture 계층

WebSocket Streaming은 기존 UFC 아키텍처의 **인프라스트럭처 계층**에 통합됩니다:

```
ufc/
├── src/main/kotlin/com/ulalax/ufc/
│   ├── yahoo/                      # Yahoo Finance 통합 (기존)
│   │   ├── YahooClient.kt          # REST API 클라이언트
│   │   ├── YahooClientConfig.kt
│   │   ├── internal/
│   │   │   ├── auth/
│   │   │   ├── response/
│   │   │   └── YahooApiUrls.kt
│   │   └── model/                  # 공개 도메인 모델
│   │       ├── ChartData.kt
│   │       ├── QuoteSummaryTypes.kt
│   │       └── ...
│   │
│   ├── streaming/                  # WebSocket Streaming (신규)
│   │   ├── StreamingClient.kt      # WebSocket 클라이언트 (공개)
│   │   ├── StreamingClientConfig.kt
│   │   ├── internal/               # 내부 구현
│   │   │   ├── websocket/
│   │   │   │   ├── YahooWebSocket.kt
│   │   │   │   ├── WebSocketConnection.kt
│   │   │   │   ├── WebSocketState.kt
│   │   │   │   └── ReconnectionStrategy.kt
│   │   │   ├── protobuf/
│   │   │   │   ├── PricingData.kt  # Protobuf 생성 클래스
│   │   │   │   └── ProtobufDecoder.kt
│   │   │   └── response/
│   │   │       └── StreamingMessage.kt
│   │   └── model/                  # 공개 도메인 모델
│   │       ├── StreamingPrice.kt
│   │       ├── StreamingQuote.kt
│   │       ├── MarketHours.kt
│   │       └── QuoteType.kt
│   │
│   ├── common/                     # 공통 인프라 (기존)
│   │   ├── exception/
│   │   ├── http/
│   │   └── ratelimit/
│   │
│   ├── Ufc.kt                      # 최상위 클라이언트 (수정)
│   └── UfcConfig.kt                # 설정 (수정)
```

#### 3.1.2 기존 인프라 재사용

WebSocket Streaming은 다음 기존 인프라를 재사용합니다:

**재사용 가능**:
- `com.ulalax.ufc.common.exception.*`: 예외 처리 체계
- `com.ulalax.ufc.common.http.UserAgents`: User-Agent 문자열
- `com.ulalax.ufc.yahoo.internal.auth.YahooAuthenticator`: CRUMB 토큰 (필요 시)

**재사용 불가 또는 제한적**:
- `RateLimiter`: WebSocket은 연결 기반이므로 기존 토큰 버킷 방식 불필요
  - 대신 **최대 동시 연결 수 제한** 구현 필요
- `HttpClient`: REST용이므로 WebSocket은 별도 클라이언트 사용

#### 3.1.3 Ufc 최상위 클라이언트 통합

`Ufc` 클래스에 `StreamingClient` 접근 추가:

**통합 방식**:
- `Ufc.streaming` 프로퍼티로 접근
- `StreamingClient`는 독립적으로 생성/종료 가능
- `Ufc.close()` 호출 시 자동으로 WebSocket 연결 종료

**라이프사이클**:
- `Ufc` 생성 시 `StreamingClient`는 즉시 생성되지만 연결은 수립하지 않음
- 사용자가 `streaming.subscribe()` 호출 시 첫 연결 수립
- `Ufc.close()` 또는 `streaming.close()` 호출 시 연결 종료

### 3.2 네임스페이스 배치

#### 3.2.1 신규 네임스페이스: `ufc.streaming`

WebSocket Streaming 기능은 **독립적인 네임스페이스**로 분리합니다:

**선택 이유**:
- `ufc.live`보다 `ufc.streaming`이 기능을 더 명확히 표현
- 향후 다른 실시간 데이터 소스 통합 가능성 (예: 웹소켓 이외의 실시간 API)
- 기존 `ufc.yahoo`는 REST API 전용으로 유지

#### 3.2.2 공개 API 구조

사용자는 다음과 같이 접근:

```
ufc.streaming.subscribe(symbols)     // 구독 시작
ufc.streaming.unsubscribe(symbols)   // 구독 해제
ufc.streaming.prices                 // Flow<StreamingPrice> 수신
ufc.streaming.close()                // 연결 종료
```

### 3.3 Kotlin Coroutines Flow 활용 방안

#### 3.3.1 Flow 기반 설계 이유

**장점**:
- **비동기 스트림**: WebSocket의 지속적인 데이터 수신에 최적
- **백프레셔 지원**: 소비자가 처리할 수 있는 속도로 데이터 소비
- **Coroutine 통합**: 기존 UFC 비동기 패턴과 일관성
- **취소 가능**: Coroutine Scope 종료 시 자동으로 Flow 구독 취소
- **콜드 스트림 변환 가능**: SharedFlow/StateFlow로 핫 스트림 구현 가능

#### 3.3.2 Flow 타입 선택

**SharedFlow 사용**:
- 여러 구독자가 동일한 WebSocket 스트림 공유
- 새로운 구독자는 구독 시점 이후 데이터만 수신
- WebSocket 연결은 하나만 유지 (효율적)

**StateFlow 사용 고려**:
- 각 심볼별 최신 상태를 유지하려면 StateFlow 사용 가능
- Map<Symbol, StateFlow<StreamingPrice>> 형태로 제공

**권장 설계**:
- 원본 스트림: `SharedFlow<StreamingPrice>` (모든 업데이트)
- 심볼별 필터링: 사용자가 `prices.filter { it.symbol == "AAPL" }` 형태로 활용

#### 3.3.3 Flow 생성 방식

**callbackFlow 사용**:
- WebSocket 콜백 기반 이벤트를 Flow로 변환
- 연결/해제 라이프사이클 관리 용이

**channelFlow 사용 고려**:
- 복잡한 버퍼링 로직 필요 시
- 현재는 callbackFlow로 충분

### 3.4 필요한 모델 클래스 목록

#### 3.4.1 공개 도메인 모델 (ufc.streaming.model)

**StreamingPrice**:
- 역할: 실시간 가격 업데이트 이벤트
- 주요 필드:
  - symbol: String (심볼)
  - price: Double (현재가)
  - change: Double (등락폭)
  - changePercent: Double (등락률)
  - timestamp: Long (Unix timestamp)
  - volume: Long (거래량)
  - bid: Double? (매수 호가)
  - ask: Double? (매도 호가)
  - marketHours: MarketHours (시장 시간 상태)

**StreamingQuote**:
- 역할: 실시간 종합 시세 정보 (StreamingPrice보다 더 상세)
- 주요 필드:
  - StreamingPrice의 모든 필드 포함
  - dayHigh: Double (당일 고가)
  - dayLow: Double (당일 저가)
  - openPrice: Double (시가)
  - previousClose: Double (전일 종가)
  - bidSize: Long? (매수 호가 수량)
  - askSize: Long? (매도 호가 수량)
  - currency: String (통화)
  - exchange: String (거래소)
  - shortName: String (자산명)
  - quoteType: QuoteType (자산 유형)

**MarketHours** (Enum):
- 역할: 시장 시간 상태 표현
- 값:
  - PRE_MARKET (프리마켓)
  - REGULAR (정규장)
  - POST_MARKET (포스트마켓)
  - CLOSED (장외)
  - UNKNOWN (알 수 없음)

**QuoteType** (Enum):
- 역할: 자산 유형 분류
- 값:
  - EQUITY (주식)
  - ETF (상장지수펀드)
  - INDEX (지수)
  - MUTUAL_FUND (뮤추얼펀드)
  - OPTION (옵션)
  - CRYPTOCURRENCY (암호화폐)
  - CURRENCY (환율)
  - FUTURE (선물)
  - UNKNOWN (알 수 없음)

**StreamingEvent** (Sealed Class):
- 역할: 스트리밍 상태 변화 이벤트
- 하위 클래스:
  - Connected (연결 성공)
  - Disconnected (연결 해제, reason: String?)
  - Reconnecting (재연결 시도 중, attempt: Int)
  - SubscriptionUpdated (구독 변경, symbols: Set<String>)
  - Error (에러 발생, exception: UfcException)

#### 3.4.2 내부 구현 모델 (ufc.streaming.internal)

**WebSocketState** (Sealed Class):
- 역할: WebSocket 연결 상태 관리
- 하위 클래스:
  - Disconnected
  - Connecting
  - Connected (connection: WebSocketConnection)
  - Reconnecting (attempt: Int)
  - Closed

**WebSocketConnection**:
- 역할: WebSocket 연결 래퍼
- 주요 메서드:
  - send(message: String)
  - close()
  - isActive(): Boolean
- 주요 필드:
  - session: WebSocketSession (Ktor WebSocket 세션)
  - connectedAt: Instant

**StreamingMessage** (Internal Response):
- 역할: 서버 메시지 파싱 결과
- 주요 필드:
  - message: String (Base64 인코딩된 Protobuf 데이터)

**ReconnectionConfig**:
- 역할: 재연결 전략 설정
- 주요 필드:
  - enabled: Boolean (재연결 활성화 여부)
  - maxAttempts: Int (최대 재시도 횟수)
  - initialDelayMs: Long (초기 대기 시간)
  - maxDelayMs: Long (최대 대기 시간)
  - backoffMultiplier: Double (지수 백오프 배수)

#### 3.4.3 Protobuf 생성 모델 (ufc.streaming.internal.protobuf)

**PricingData**:
- 역할: Protobuf 메시지 클래스 (코드 생성 또는 수동 구현)
- 구조: 2.5.1 섹션 참조
- 구현 방식:
  - Option 1: protobuf-gradle-plugin으로 자동 생성
  - Option 2: kotlinx.serialization 기반 수동 구현

**ProtobufDecoder**:
- 역할: Base64 문자열을 PricingData로 디코딩
- 주요 메서드:
  - decode(base64String: String): PricingData

### 3.5 API 메서드 시그니처 정의

#### 3.5.1 StreamingClient (공개 API)

**생성 메서드**:
```kotlin
companion object {
    fun create(config: StreamingClientConfig = StreamingClientConfig()): StreamingClient
}
```

**구독 관리**:
```kotlin
// 심볼 구독 (단일)
suspend fun subscribe(symbol: String)

// 심볼 구독 (다중)
suspend fun subscribe(symbols: List<String>)

// 심볼 구독 해제 (단일)
suspend fun unsubscribe(symbol: String)

// 심볼 구독 해제 (다중)
suspend fun unsubscribe(symbols: List<String>)

// 전체 구독 해제
suspend fun unsubscribeAll()

// 현재 구독 중인 심볼 목록 조회
fun getSubscribedSymbols(): Set<String>
```

**데이터 수신**:
```kotlin
// 실시간 가격 스트림 (모든 구독 심볼)
val prices: SharedFlow<StreamingPrice>

// 실시간 종합 시세 스트림 (모든 구독 심볼)
val quotes: SharedFlow<StreamingQuote>

// 연결 상태 이벤트 스트림
val events: SharedFlow<StreamingEvent>

// 심볼별 필터링 헬퍼
fun pricesBySymbol(symbol: String): Flow<StreamingPrice>
fun quotesBySymbol(symbol: String): Flow<StreamingQuote>
```

**연결 제어**:
```kotlin
// 명시적 연결 (선택적, subscribe() 호출 시 자동 연결)
suspend fun connect()

// 연결 해제
suspend fun disconnect()

// 연결 상태 확인
fun isConnected(): Boolean

// 리소스 정리
override fun close()
```

#### 3.5.2 StreamingClientConfig (설정)

```kotlin
data class StreamingClientConfig(
    // WebSocket URL
    val webSocketUrl: String = "wss://streamer.finance.yahoo.com/?version=2",

    // 타임아웃 설정
    val connectTimeoutMs: Long = 10_000L,
    val heartbeatIntervalMs: Long = 15_000L,
    val pingTimeoutMs: Long = 30_000L,

    // 재연결 설정
    val reconnection: ReconnectionConfig = ReconnectionConfig(),

    // 버퍼 설정
    val eventBufferSize: Int = 64,  // Flow replay 버퍼 크기

    // 로깅
    val enableLogging: Boolean = false
)

data class ReconnectionConfig(
    val enabled: Boolean = true,
    val maxAttempts: Int = 5,
    val initialDelayMs: Long = 1_000L,
    val maxDelayMs: Long = 30_000L,
    val backoffMultiplier: Double = 2.0
)
```

#### 3.5.3 사용 예시 (명세용)

**기본 사용**:
```kotlin
Ufc.create().use { ufc ->
    ufc.streaming.subscribe("AAPL")

    ufc.streaming.prices.collect { price ->
        println("${price.symbol}: ${price.price} (${price.changePercent}%)")
    }
}
```

**다중 심볼 모니터링**:
```kotlin
ufc.streaming.subscribe(listOf("AAPL", "GOOGL", "MSFT"))

launch {
    ufc.streaming.pricesBySymbol("AAPL").collect { price ->
        // AAPL만 처리
    }
}

launch {
    ufc.streaming.events.collect { event ->
        when (event) {
            is StreamingEvent.Connected -> println("연결됨")
            is StreamingEvent.Error -> println("에러: ${event.exception}")
            else -> {}
        }
    }
}
```

**재연결 비활성화**:
```kotlin
val config = UfcConfig(
    streamingConfig = StreamingClientConfig(
        reconnection = ReconnectionConfig(enabled = false)
    )
)
Ufc.create(config).use { ufc ->
    // 재연결 없이 사용
}
```

---

## 4. 연결 관리

### 4.1 연결 수립 및 해제

#### 4.1.1 연결 수립 흐름

**자동 연결 (권장)**:
1. 사용자가 `streaming.subscribe(symbol)` 호출
2. 현재 상태가 `Disconnected`이면 자동으로 `connect()` 호출
3. WebSocket 연결 수립
4. 연결 성공 시 `StreamingEvent.Connected` 이벤트 방출
5. 구독 메시지 전송
6. 데이터 수신 시작

**명시적 연결 (선택적)**:
1. 사용자가 `streaming.connect()` 호출
2. WebSocket 연결 수립
3. 연결 성공하면 대기 상태 (구독 없음)
4. 이후 `subscribe()` 호출 시 구독 메시지만 전송

#### 4.1.2 연결 해제 흐름

**명시적 해제**:
1. 사용자가 `streaming.disconnect()` 또는 `streaming.close()` 호출
2. 모든 구독 해제 메시지 전송 (선택적)
3. WebSocket 연결 종료
4. `StreamingEvent.Disconnected` 이벤트 방출
5. 내부 상태를 `Closed`로 전환

**자동 해제**:
1. `Ufc.close()` 호출 시 `streaming.close()` 자동 호출
2. Coroutine Scope 취소 시 Flow 구독 자동 해제

#### 4.1.3 연결 상태 전이도

```
Disconnected
    ↓ connect()
Connecting
    ↓ (성공)
Connected
    ↓ (연결 끊김)
Reconnecting (attempt 1)
    ↓ (재시도)
Reconnecting (attempt 2)
    ...
    ↓ (성공)
Connected
    ↓ disconnect() / close()
Closed (최종 상태)
```

### 4.2 재연결 전략

#### 4.2.1 재연결 트리거 조건

다음 상황에서 자동 재연결 시도:
- **네트워크 연결 끊김**: TCP 연결 실패
- **WebSocket Close Frame 수신**: 서버가 연결 종료
- **타임아웃**: 일정 시간 동안 메시지 미수신 (ping/pong 실패)
- **예외 발생**: 메시지 처리 중 예외 (Protobuf 디코딩 제외)

**재연결 하지 않는 경우**:
- 사용자가 명시적으로 `disconnect()` 또는 `close()` 호출
- `ReconnectionConfig.enabled = false`
- 최대 재시도 횟수 초과
- 인증 실패 등 복구 불가능한 에러

#### 4.2.2 재연결 알고리즘

**지수 백오프 (Exponential Backoff)**:

```
대기 시간 = min(initialDelay * (backoffMultiplier ^ attempt), maxDelay)
```

**예시 (기본 설정)**:
- 1차 시도: 1초 대기
- 2차 시도: 2초 대기
- 3차 시도: 4초 대기
- 4차 시도: 8초 대기
- 5차 시도: 16초 대기
- 6차 이후: 30초 대기 (maxDelay)

**재연결 프로세스**:
1. 연결 끊김 감지
2. `StreamingEvent.Reconnecting(attempt=1)` 이벤트 방출
3. 대기 (백오프 시간)
4. 재연결 시도
5. 성공 시: `StreamingEvent.Connected` 방출 및 구독 복원
6. 실패 시: attempt 증가 후 2단계 반복
7. 최대 횟수 초과 시: `StreamingEvent.Error` 방출 및 `Closed` 상태 전환

#### 4.2.3 구독 복원

재연결 성공 시 다음 작업 수행:
1. 재연결 이전의 구독 심볼 목록 복원
2. 구독 메시지 재전송
3. 하트비트 타이머 재시작

**구독 상태 저장**:
- `Set<String>` 형태로 구독 중인 심볼 목록 관리
- 연결 끊김 시에도 구독 목록 유지
- 재연결 시 자동으로 복원

### 4.3 심볼 구독/해제 관리

#### 4.3.1 구독 관리 전략

**내부 상태**:
- `subscribedSymbols: MutableSet<String>` 형태로 관리
- Thread-safe 컬렉션 사용 (Concurrent 또는 Mutex)

**구독 추가**:
1. `subscribe(symbols)` 호출
2. 연결되지 않았다면 자동 연결
3. `subscribedSymbols`에 심볼 추가
4. 서버에 구독 메시지 전송
5. `StreamingEvent.SubscriptionUpdated` 이벤트 방출 (선택적)

**구독 해제**:
1. `unsubscribe(symbols)` 호출
2. `subscribedSymbols`에서 심볼 제거
3. 서버에 구독 해제 메시지 전송
4. `StreamingEvent.SubscriptionUpdated` 이벤트 방출 (선택적)
5. 모든 구독 해제 시 연결 유지 또는 종료 (설정 가능)

#### 4.3.2 중복 구독 처리

**중복 구독 시**:
- 이미 구독 중인 심볼 재구독 요청 시 무시
- 서버에 중복 메시지 전송하지 않음
- 멱등성 보장

**중복 해제 시**:
- 구독하지 않은 심볼 해제 요청 시 무시
- 에러 발생하지 않음

#### 4.3.3 하트비트 메커니즘

**목적**:
- 연결 활성 상태 유지
- 서버 타임아웃 방지

**구현**:
1. 연결 성공 시 타이머 시작 (15초 간격 권장)
2. 타이머 만료 시 현재 구독 목록 전체를 재전송
3. 메시지 형식은 초기 구독과 동일: `{"subscribe": ["AAPL", "GOOGL"]}`
4. 연결 종료 시 타이머 취소

**타임아웃 감지**:
- 마지막 메시지 수신 시간 추적
- `pingTimeoutMs` (기본 30초) 동안 메시지 미수신 시 재연결 트리거

---

## 5. 데이터 매핑

### 5.1 Protobuf 메시지 → UFC 도메인 모델 매핑

#### 5.1.1 PricingData → StreamingPrice 매핑

**매핑 테이블**:

| PricingData 필드 | StreamingPrice 필드 | 타입 변환 | 비고 |
|-----------------|-------------------|---------|------|
| id | symbol | String | 직접 매핑 |
| price | price | Float → Double | 직접 매핑 |
| change | change | Float → Double | 직접 매핑 |
| change_percent | changePercent | Float → Double | 직접 매핑 |
| time | timestamp | Int64 → Long | Unix timestamp (seconds) |
| day_volume | volume | Int64 → Long | 직접 매핑 |
| bid | bid | Float → Double? | nullable |
| ask | ask | Float → Double? | nullable |
| market_hours | marketHours | Int32 → MarketHours | Enum 변환 |

**변환 규칙**:
- Float → Double: 직접 캐스팅
- Int32/Int64 → Enum: 값에 따라 매핑 (아래 참조)
- 선택적 필드: Protobuf 기본값(0, "")이면 null로 변환

#### 5.1.2 PricingData → StreamingQuote 매핑

**StreamingPrice의 모든 필드 포함 + 추가 필드**:

| PricingData 필드 | StreamingQuote 필드 | 타입 변환 | 비고 |
|-----------------|-------------------|---------|------|
| day_high | dayHigh | Float → Double | 직접 매핑 |
| day_low | dayLow | Float → Double | 직접 매핑 |
| open_price | openPrice | Float → Double | 직접 매핑 |
| previous_close | previousClose | Float → Double | 직접 매핑 |
| bid_size | bidSize | Int64 → Long? | nullable |
| ask_size | askSize | Int64 → Long? | nullable |
| currency | currency | String | 직접 매핑 |
| exchange | exchange | String | 직접 매핑 |
| short_name | shortName | String | 직접 매핑 |
| quote_type | quoteType | Int32 → QuoteType | Enum 변환 |

### 5.2 타입 변환 규칙

#### 5.2.1 market_hours → MarketHours 변환

| PricingData 값 | MarketHours Enum | 설명 |
|---------------|-----------------|------|
| 0 | CLOSED | 장외 (시장 종료) |
| 1 | REGULAR | 정규 거래 시간 |
| 2 | PRE_MARKET | 프리마켓 (장 시작 전) |
| 3 | POST_MARKET | 포스트마켓 (장 종료 후) |
| 기타 | UNKNOWN | 알 수 없는 상태 |

#### 5.2.2 quote_type → QuoteType 변환

| PricingData 값 | QuoteType Enum | 설명 |
|---------------|---------------|------|
| 1 | EQUITY | 주식 |
| 2 | ETF | 상장지수펀드 |
| 5 | OPTION | 옵션 |
| 6 | MUTUAL_FUND | 뮤추얼펀드 |
| 8 | INDEX | 인덱스 |
| 11 | CRYPTOCURRENCY | 암호화폐 |
| 12 | CURRENCY | 환율 |
| 13 | FUTURE | 선물 |
| 기타 | UNKNOWN | 알 수 없는 유형 |

#### 5.2.3 Nullable 필드 처리

**Protobuf proto3 기본값 규칙**:
- 숫자 타입: 0
- 문자열: ""
- Boolean: false

**UFC 변환 규칙**:
- 숫자 필드:
  - 0이면 null로 변환 (의미 없는 데이터)
  - 0이 아니면 값 사용
- 문자열 필드:
  - 빈 문자열이면 null로 변환
  - 비어있지 않으면 값 사용

**예외**:
- `price`, `timestamp` 등 필수 필드는 null 불가
- 0 값도 유효한 경우 (예: change가 0) null 변환하지 않음

#### 5.2.4 타임스탬프 변환

**PricingData.time**:
- 형식: Unix timestamp (초 단위)
- 타입: sint64

**StreamingPrice.timestamp**:
- 형식: Unix timestamp (초 단위 유지)
- 타입: Long
- 사용: `Instant.ofEpochSecond(timestamp)`로 변환 가능

#### 5.2.5 가격 정밀도 처리

**price_hint 필드 활용**:
- `price_hint`: 소수점 자릿수를 나타내는 힌트
- 값 예시: `2` (소수점 2자리)
- 변환: 표시용으로만 사용, 내부 계산은 Double 그대로 유지

**권장 처리**:
- 가격 저장: Double 그대로 저장
- 가격 표시: `String.format("%.${priceHint}f", price)` 형태로 포맷

---

## 6. 에러 처리

### 6.1 예상되는 에러 케이스

#### 6.1.1 연결 관련 에러

**연결 실패 (Connection Failed)**:
- 원인: 네트워크 불가, DNS 실패, 방화벽 차단
- ErrorCode: `NETWORK_CONNECTION_ERROR`
- 재시도: 가능 (재연결 전략 적용)
- 처리: `StreamingEvent.Error` 방출, 재연결 시도

**연결 타임아웃 (Connection Timeout)**:
- 원인: 서버 응답 없음, 네트워크 지연
- ErrorCode: `NETWORK_TIMEOUT`
- 재시도: 가능
- 처리: 재연결 시도

**인증 실패 (Authentication Failed)**:
- 원인: CRUMB 토큰 만료 또는 누락 (필요 시)
- ErrorCode: `AUTHENTICATION_FAILED`
- 재시도: 불가능 (CRUMB 재발급 필요)
- 처리: 에러 이벤트 방출 후 연결 종료

**서버 연결 거부 (Connection Refused)**:
- 원인: 서버 점검, Rate Limit 초과
- ErrorCode: `SERVICE_UNAVAILABLE` 또는 `RATE_LIMIT_EXCEEDED`
- 재시도: 가능 (긴 백오프 적용)
- 처리: 재연결 시도 (최대 시도 후 포기)

#### 6.1.2 데이터 수신 에러

**메시지 파싱 실패 (Message Parsing Error)**:
- 원인: 잘못된 JSON 형식, Base64 디코딩 실패
- ErrorCode: `JSON_PARSING_ERROR` 또는 `DATA_PARSING_ERROR`
- 재시도: 불필요 (해당 메시지만 스킵)
- 처리: 로그 기록, 메시지 무시, 연결 유지

**Protobuf 디코딩 실패 (Protobuf Decoding Error)**:
- 원인: Protobuf 구조 불일치, 손상된 데이터
- ErrorCode: `DATA_PARSING_ERROR`
- 재시도: 불필요 (해당 메시지만 스킵)
- 처리: 로그 기록, 메시지 무시, 연결 유지

**예상치 못한 메시지 형식 (Unexpected Message Format)**:
- 원인: Yahoo API 변경, 알 수 없는 메시지 타입
- ErrorCode: `INVALID_DATA_FORMAT`
- 재시도: 불필요
- 처리: 로그 기록, 메시지 무시

#### 6.1.3 구독 관련 에러

**구독 실패 (Subscription Failed)**:
- 원인: 잘못된 심볼, 서버 거부
- ErrorCode: `INVALID_SYMBOL` 또는 `EXTERNAL_API_ERROR`
- 재시도: 불가능 (심볼 검증 필요)
- 처리: 에러 이벤트 방출, 해당 심볼 구독 목록에서 제거

**구독 타임아웃 (Subscription Timeout)**:
- 원인: 구독 요청 후 응답 없음
- ErrorCode: `NETWORK_TIMEOUT`
- 재시도: 가능
- 처리: 구독 메시지 재전송

**구독 한도 초과 (Subscription Limit Exceeded)**:
- 원인: 동시 구독 가능한 심볼 개수 초과
- ErrorCode: `RATE_LIMIT_EXCEEDED`
- 재시도: 불가능
- 처리: 에러 이벤트 방출, 사용자에게 구독 개수 조정 안내

#### 6.1.4 재연결 에러

**재연결 최대 횟수 초과 (Max Reconnection Attempts Exceeded)**:
- 원인: 지속적인 네트워크 실패
- ErrorCode: `NETWORK_UNAVAILABLE`
- 재시도: 불가능 (최대 시도 완료)
- 처리: `StreamingEvent.Error` 방출, 연결 종료

**재연결 중 사용자 취소 (User Cancellation During Reconnection)**:
- 원인: 사용자가 `disconnect()` 호출
- ErrorCode: 없음 (정상 종료)
- 재시도: 불필요
- 처리: 재연결 중단, 연결 종료

### 6.2 에러 코드 확장

기존 `ErrorCode` Enum에 WebSocket 전용 에러 추가 필요:

**신규 에러 코드 제안**:

| 에러 코드 | 코드 번호 | 메시지 | 재시도 가능 | 비고 |
|---------|---------|-------|-----------|------|
| WEBSOCKET_CONNECTION_FAILED | 1010 | WebSocket 연결에 실패했습니다. | true | 연결 실패 |
| WEBSOCKET_HANDSHAKE_FAILED | 1011 | WebSocket 핸드셰이크에 실패했습니다. | true | 핸드셰이크 실패 |
| WEBSOCKET_CLOSED_BY_SERVER | 1012 | 서버가 WebSocket 연결을 종료했습니다. | true | 서버 종료 |
| WEBSOCKET_PROTOCOL_ERROR | 1013 | WebSocket 프로토콜 오류가 발생했습니다. | false | 프로토콜 위반 |
| WEBSOCKET_MESSAGE_TOO_LARGE | 1014 | WebSocket 메시지가 너무 큽니다. | false | 메시지 크기 초과 |
| PROTOBUF_DECODING_ERROR | 5011 | Protobuf 디코딩 중 오류가 발생했습니다. | false | Protobuf 파싱 실패 |
| STREAMING_SUBSCRIPTION_FAILED | 6010 | 스트리밍 구독에 실패했습니다. | false | 구독 실패 |
| STREAMING_MAX_SUBSCRIPTIONS_EXCEEDED | 6011 | 최대 구독 개수를 초과했습니다. | false | 구독 한도 초과 |
| STREAMING_RECONNECTION_FAILED | 1020 | 재연결에 실패했습니다. | false | 재연결 최대 시도 초과 |

### 6.3 에러 처리 전략

#### 6.3.1 에러 분류 및 처리 방침

**복구 가능 에러 (Recoverable)**:
- 네트워크 일시 장애, 타임아웃
- 처리: 자동 재연결 시도
- 사용자 알림: `StreamingEvent.Reconnecting` 이벤트

**복구 불가능 에러 (Unrecoverable)**:
- 인증 실패, 프로토콜 위반, 심볼 오류
- 처리: 에러 이벤트 방출 후 연결 종료
- 사용자 알림: `StreamingEvent.Error` 이벤트

**일시적 에러 (Transient)**:
- 메시지 파싱 실패 (단일 메시지만 영향)
- 처리: 해당 메시지 무시, 연결 유지
- 사용자 알림: 로그만 기록 (선택적)

#### 6.3.2 에러 이벤트 방출

**StreamingEvent.Error 구조**:
```kotlin
data class Error(
    val exception: UfcException,
    val isFatal: Boolean  // 치명적 에러 여부 (연결 종료 필요)
) : StreamingEvent()
```

**사용자 에러 처리 예시**:
```kotlin
ufc.streaming.events.collect { event ->
    when (event) {
        is StreamingEvent.Error -> {
            if (event.isFatal) {
                println("치명적 에러 발생: ${event.exception.message}")
                // 연결 종료 처리
            } else {
                println("일시적 에러: ${event.exception.message}")
                // 로그만 기록
            }
        }
        is StreamingEvent.Reconnecting -> {
            println("재연결 시도 중... (${event.attempt}회)")
        }
        is StreamingEvent.Connected -> {
            println("연결 복구됨")
        }
        else -> {}
    }
}
```

#### 6.3.3 로깅 전략

**에러 로깅 레벨**:
- **ERROR**: 복구 불가능 에러, 재연결 최대 시도 초과
- **WARN**: 재연결 시도, 구독 실패
- **INFO**: 연결 성공, 구독 변경
- **DEBUG**: 메시지 수신, 파싱 과정

**로그 포맷 예시**:
```
[WARN] WebSocket connection lost. Attempting reconnection (1/5)...
[ERROR] Max reconnection attempts (5) exceeded. Closing connection.
[INFO] Successfully subscribed to symbols: [AAPL, GOOGL, MSFT]
[DEBUG] Received PricingData: symbol=AAPL, price=175.43
```

---

## 7. 테스트 전략

### 7.1 단위 테스트 시나리오

#### 7.1.1 모델 변환 테스트

**테스트 클래스**: `ProtobufDecoderTest`

**테스트 케이스**:
1. **유효한 Protobuf 메시지 디코딩**
   - Base64 문자열 → PricingData 변환 성공 확인
   - 모든 필드 값 검증

2. **잘못된 Base64 문자열 처리**
   - 예외 발생 확인 (DataParsingException)
   - 에러 메시지 검증

3. **PricingData → StreamingPrice 매핑**
   - 모든 필드 올바르게 매핑되는지 확인
   - Nullable 필드 처리 검증 (0 → null)

4. **PricingData → StreamingQuote 매핑**
   - StreamingPrice 필드 + 추가 필드 매핑 확인
   - QuoteType, MarketHours Enum 변환 검증

5. **Enum 변환 테스트**
   - market_hours → MarketHours (모든 값 케이스)
   - quote_type → QuoteType (모든 값 케이스)
   - 알 수 없는 값 → UNKNOWN 매핑 확인

#### 7.1.2 재연결 로직 테스트

**테스트 클래스**: `ReconnectionStrategyTest`

**테스트 케이스**:
1. **백오프 시간 계산**
   - 각 재시도 횟수별 대기 시간 검증
   - maxDelay 초과 시 제한 확인

2. **재연결 최대 횟수 제한**
   - maxAttempts 도달 시 재연결 중단 확인
   - 적절한 에러 이벤트 방출 검증

3. **재연결 비활성화 설정**
   - `enabled=false` 시 재연결 시도하지 않음 확인

4. **재연결 성공 시 구독 복원**
   - 구독 목록이 재연결 후에도 유지되는지 확인

#### 7.1.3 구독 관리 테스트

**테스트 클래스**: `SubscriptionManagerTest`

**테스트 케이스**:
1. **단일 심볼 구독**
   - 구독 목록에 추가 확인
   - 구독 메시지 전송 검증

2. **다중 심볼 구독**
   - 여러 심볼 동시 구독 확인
   - 중복 심볼 처리 (무시) 검증

3. **심볼 구독 해제**
   - 구독 목록에서 제거 확인
   - 구독 해제 메시지 전송 검증

4. **전체 구독 해제**
   - 모든 심볼 제거 확인

5. **중복 구독/해제 멱등성**
   - 이미 구독 중인 심볼 재구독 시 무시
   - 구독하지 않은 심볼 해제 시 무시

#### 7.1.4 Flow 동작 테스트

**테스트 클래스**: `StreamingFlowTest`

**테스트 케이스**:
1. **가격 데이터 Flow 방출**
   - `prices` Flow가 StreamingPrice 이벤트 방출하는지 확인
   - 여러 구독자가 동일한 데이터 수신하는지 검증 (SharedFlow)

2. **심볼별 필터링**
   - `pricesBySymbol("AAPL")` 호출 시 AAPL만 수신 확인

3. **이벤트 Flow**
   - `events` Flow가 연결/에러 이벤트 방출하는지 확인

4. **백프레셔 처리**
   - 소비자가 느린 경우 버퍼링 동작 확인

### 7.2 통합 테스트 시나리오

#### 7.2.1 실제 WebSocket 연결 테스트

**테스트 클래스**: `StreamingClientIntegrationSpec`

**테스트 케이스**:
1. **실제 서버 연결 및 구독**
   - Yahoo Finance WebSocket 서버 연결 성공
   - AAPL 구독 후 실시간 데이터 수신 확인
   - 수신된 데이터 필드 검증

2. **다중 심볼 구독**
   - AAPL, GOOGL, MSFT 동시 구독
   - 각 심볼별 데이터 수신 확인

3. **구독 해제**
   - 구독 후 일부 심볼 해제
   - 해제된 심볼 데이터 미수신 확인

4. **연결 종료 및 재연결**
   - 강제로 연결 종료 후 자동 재연결 확인
   - 구독 복원 검증

5. **장시간 연결 유지**
   - 1분 이상 연결 유지 확인
   - 하트비트 메시지 전송 검증

**테스트 제약사항**:
- 실제 Yahoo Finance API 사용 (네트워크 필요)
- 시장 시간에만 실시간 데이터 수신 가능
- 장외 시간에는 데이터 미수신 또는 제한적 수신

#### 7.2.2 에러 시나리오 통합 테스트

**테스트 클래스**: `StreamingErrorHandlingSpec`

**테스트 케이스**:
1. **잘못된 심볼 구독**
   - 존재하지 않는 심볼 구독 시 에러 처리 확인

2. **네트워크 끊김 시뮬레이션**
   - 연결 중 네트워크 차단 시 재연결 동작 확인

3. **타임아웃 시뮬레이션**
   - 서버 응답 없을 때 타임아웃 처리 확인

4. **재연결 최대 횟수 도달**
   - 재연결 반복 실패 시 최종 에러 처리 확인

#### 7.2.3 성능 테스트

**테스트 클래스**: `StreamingPerformanceSpec`

**테스트 케이스**:
1. **다중 심볼 처리 성능**
   - 50개 심볼 동시 구독
   - 초당 메시지 수신 속도 측정
   - 메모리 사용량 모니터링

2. **장시간 스트리밍**
   - 10분 이상 연결 유지
   - 메모리 누수 확인

3. **재연결 성능**
   - 재연결 소요 시간 측정
   - 구독 복원 시간 측정

#### 7.2.4 테스트 환경 구성

**테스트 실행 조건**:
- 환경 변수 또는 설정 파일로 통합 테스트 활성화
- CI/CD 환경에서는 Mock 서버 사용 고려
- 로컬 개발 시에만 실제 서버 연결 테스트

**Mock WebSocket 서버**:
- 테스트용 WebSocket 서버 구현 (선택적)
- Protobuf 메시지 생성 및 전송
- 재연결, 타임아웃 등 시나리오 시뮬레이션

---

## 8. 구현 우선순위

### 8.1 Phase 1: 핵심 기능 구현 (MVP)

**목표**: 기본적인 WebSocket 연결 및 데이터 수신

**구현 범위**:
1. **Protobuf 스키마 통합**
   - PricingData Protobuf 클래스 생성
   - ProtobufDecoder 구현

2. **WebSocket 연결 관리**
   - WebSocketConnection 구현
   - 연결/해제 기본 로직
   - WebSocketState 상태 관리

3. **기본 구독 기능**
   - subscribe(symbol) / subscribe(symbols) 구현
   - 구독 메시지 전송
   - 구독 목록 관리 (Set<String>)

4. **데이터 수신 및 Flow 방출**
   - WebSocket 메시지 수신
   - Base64 디코딩 및 Protobuf 파싱
   - PricingData → StreamingPrice 변환
   - SharedFlow<StreamingPrice> 방출

5. **기본 에러 처리**
   - 연결 실패 에러 처리
   - 메시지 파싱 실패 시 무시

**성공 기준**:
- 단일 심볼 구독 후 실시간 가격 데이터 수신
- 데이터를 Flow로 소비 가능

**예상 기간**: 1-2주

### 8.2 Phase 2: 안정성 강화

**목표**: 재연결 및 에러 처리 완성

**구현 범위**:
1. **재연결 전략 구현**
   - ReconnectionStrategy 구현
   - 지수 백오프 알고리즘
   - 최대 재시도 횟수 제한
   - 구독 복원 로직

2. **하트비트 메커니즘**
   - 주기적 구독 메시지 재전송 (15초)
   - 타임아웃 감지 (30초)

3. **포괄적 에러 처리**
   - 모든 에러 케이스 처리 (6.1 섹션 참조)
   - StreamingEvent.Error 이벤트 방출
   - 에러 코드 확장

4. **구독 해제 기능**
   - unsubscribe(symbol) / unsubscribe(symbols) 구현
   - unsubscribeAll() 구현
   - 구독 해제 메시지 전송

5. **연결 상태 이벤트**
   - StreamingEvent 구현
   - Connected, Disconnected, Reconnecting, Error 이벤트 방출
   - events Flow 제공

**성공 기준**:
- 네트워크 끊김 시 자동 재연결
- 재연결 후 구독 복원
- 모든 에러 케이스 처리

**예상 기간**: 1-2주

### 8.3 Phase 3: 고급 기능 및 최적화

**목표**: 사용성 개선 및 성능 최적화

**구현 범위**:
1. **StreamingQuote 지원**
   - PricingData → StreamingQuote 변환
   - quotes Flow 제공
   - 상세 시세 정보 제공

2. **심볼별 필터링 헬퍼**
   - pricesBySymbol(symbol) 구현
   - quotesBySymbol(symbol) 구현

3. **설정 옵션 확장**
   - StreamingClientConfig 세부 옵션
   - ReconnectionConfig 커스터마이징
   - 버퍼 크기, 타임아웃 설정

4. **Ufc 최상위 클라이언트 통합**
   - Ufc.streaming 프로퍼티 추가
   - UfcConfig에 StreamingClientConfig 통합
   - 생성/종료 라이프사이클 연동

5. **성능 최적화**
   - Flow 버퍼링 최적화
   - Protobuf 파싱 성능 개선
   - 메모리 사용량 최적화

**성공 기준**:
- 50개 이상 심볼 동시 구독 가능
- 초당 수백 개 메시지 처리
- 메모리 누수 없음

**예상 기간**: 1주

### 8.4 Phase 4: 테스트 및 문서화

**목표**: 프로덕션 준비 완료

**구현 범위**:
1. **단위 테스트 완성**
   - 7.1 섹션의 모든 테스트 케이스 구현
   - 코드 커버리지 80% 이상

2. **통합 테스트 완성**
   - 7.2 섹션의 모든 테스트 케이스 구현
   - 실제 서버 연결 테스트
   - Mock 서버 테스트 (CI/CD용)

3. **문서화**
   - README에 WebSocket Streaming 사용법 추가
   - KDoc 주석 작성
   - 사용 예시 추가

4. **샘플 애플리케이션**
   - 실시간 가격 모니터링 예제
   - 다중 심볼 대시보드 예제

**성공 기준**:
- 모든 테스트 통과
- 문서 완성
- 사용자가 쉽게 시작 가능

**예상 기간**: 1주

### 8.5 전체 일정 요약

| Phase | 목표 | 예상 기간 | 누적 기간 |
|-------|------|---------|---------|
| Phase 1 | 핵심 기능 구현 (MVP) | 1-2주 | 1-2주 |
| Phase 2 | 안정성 강화 | 1-2주 | 2-4주 |
| Phase 3 | 고급 기능 및 최적화 | 1주 | 3-5주 |
| Phase 4 | 테스트 및 문서화 | 1주 | 4-6주 |

**총 예상 기간**: 4-6주

### 8.6 우선순위 결정 기준

1. **사용자 가치**: 실시간 가격 수신이 최우선
2. **안정성**: 재연결 없이는 실용성 없음
3. **사용성**: 간단한 API로 빠르게 시작 가능해야 함
4. **확장성**: 다중 심볼 지원 필수
5. **성능**: 최적화는 기본 기능 이후

---

## 부록

### A. 참조 자료

- **yfinance live.py**: `/home/ulalax/project/kairos/yfinance/yfinance/live.py`
- **Protobuf 스키마**: `/home/ulalax/project/kairos/yfinance/yfinance/pricing.proto`
- **UFC 기존 구조**: `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/`

### B. 기술 스택

- **WebSocket 클라이언트**: Ktor Client WebSocket 플러그인
- **Protobuf 파싱**: kotlinx-serialization-protobuf 또는 protobuf-java
- **비동기 처리**: Kotlin Coroutines + Flow
- **JSON 파싱**: kotlinx.serialization (기존 UFC 사용)

### C. 의존성 추가 예상

```kotlin
// build.gradle.kts
dependencies {
    // WebSocket
    implementation("io.ktor:ktor-client-websockets:3.0.1")

    // Protobuf (Option 1: Google Protobuf)
    implementation("com.google.protobuf:protobuf-kotlin:3.24.0")

    // Protobuf (Option 2: kotlinx.serialization)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.7.3")
}
```

### D. 용어 정리

- **WebSocket**: 양방향 통신 프로토콜
- **Protobuf**: Protocol Buffers, 구조화된 데이터 직렬화 형식
- **SharedFlow**: 여러 구독자가 공유하는 핫 Flow
- **백오프 (Backoff)**: 재시도 간격을 점진적으로 늘리는 전략
- **하트비트 (Heartbeat)**: 연결 활성 상태 유지를 위한 주기적 메시지
- **멱등성 (Idempotency)**: 동일한 요청을 여러 번 수행해도 결과가 같음

---

**문서 버전**: 1.0
**최종 수정일**: 2025-12-05
