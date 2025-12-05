# Ufc Facade API 기술 명세서

> **Version**: 1.0
> **작성일**: 2025-12-05

---

## 1. 개요

### 1.1 API 목적

Ufc 클래스는 UFC 라이브러리의 **통합 진입점(Unified Entry Point)**으로, 모든 금융 데이터 소스에 대한 접근을 단일 파사드로 제공한다.

| 항목 | 값 |
|-----|---|
| 패턴 | Facade Pattern |
| 패키지 | `com.ulalax.ufc.api` |
| 주요 클래스 | Ufc, UfcConfig |
| 통합 데이터 소스 | Yahoo Finance, FRED, Business Insider |

### 1.2 제공 기능

| 기능 | 설명 |
|-----|------|
| 통합 진입점 | 모든 데이터 소스를 단일 인터페이스로 접근 |
| 설정 관리 | 각 클라이언트별 독립적인 설정 제공 |
| 리소스 관리 | AutoCloseable을 통한 자동 리소스 정리 |
| 위임 패턴 | 실제 구현은 각 클라이언트에 위임 |
| 편의 메서드 | 자주 사용되는 API를 파사드에서 직접 제공 |

### 1.3 아키텍처

```
┌─────────────────────────────────────────────────┐
│                 Ufc (Facade)                    │
│  - 통합 진입점                                   │
│  - 설정 관리                                     │
│  - 리소스 관리                                   │
└────────────┬──────────┬──────────┬──────────────┘
             │          │          │
    ┌────────▼──┐  ┌────▼────┐  ┌─▼─────────────┐
    │ Yahoo     │  │ FRED    │  │ Business      │
    │ Client    │  │ Client  │  │ Insider Client│
    └───────────┘  └─────────┘  └───────────────┘
             │
    ┌────────▼──────────┐
    │ Streaming Client  │
    └───────────────────┘
```

---

## 2. 설정

### 2.1 UfcConfig

Ufc 클라이언트 생성 시 사용되는 통합 설정 객체.

| 필드 | 타입 | 기본값 | 설명 |
|-----|------|-------|------|
| fredApiKey | String? | null | FRED API 키 (null이면 FRED 클라이언트 미생성) |
| yahooConfig | YahooClientConfig | YahooClientConfig() | Yahoo Finance 클라이언트 설정 |
| fredConfig | FredClientConfig | FredClientConfig() | FRED 클라이언트 설정 |
| businessInsiderConfig | BusinessInsiderClientConfig | BusinessInsiderClientConfig() | Business Insider 클라이언트 설정 |
| streamingConfig | StreamingClientConfig | StreamingClientConfig() | WebSocket Streaming 클라이언트 설정 |

### 2.2 YahooClientConfig

Yahoo Finance API 클라이언트 설정.

| 필드 | 타입 | 기본값 | 설명 |
|-----|------|-------|------|
| connectTimeoutMs | Long | 30000 | HTTP 연결 타임아웃 (밀리초) |
| requestTimeoutMs | Long | 60000 | HTTP 요청 타임아웃 (밀리초) |
| enableLogging | Boolean | false | HTTP 로깅 활성화 |
| rateLimitConfig | RateLimitConfig | yahooDefault() | Rate Limit 설정 (50 RPS) |

#### RateLimitConfig 기본값 (Yahoo)

| 필드 | 기본값 |
|-----|-------|
| tokensPerSecond | 50.0 |
| capacity | 100.0 |

### 2.3 FredClientConfig

FRED API 클라이언트 설정.

| 필드 | 타입 | 기본값 | 설명 |
|-----|------|-------|------|
| connectTimeoutMs | Long | 30000 | HTTP 연결 타임아웃 (밀리초) |
| requestTimeoutMs | Long | 60000 | HTTP 요청 타임아웃 (밀리초) |
| enableLogging | Boolean | false | HTTP 로깅 활성화 |
| rateLimitConfig | RateLimitConfig | fredDefault() | Rate Limit 설정 (2 RPS) |

#### RateLimitConfig 기본값 (FRED)

| 필드 | 기본값 |
|-----|-------|
| tokensPerSecond | 2.0 |
| capacity | 4.0 |

### 2.4 BusinessInsiderClientConfig

Business Insider API 클라이언트 설정.

| 필드 | 타입 | 기본값 | 설명 |
|-----|------|-------|------|
| connectTimeoutMs | Long | 30000 | HTTP 연결 타임아웃 (밀리초) |
| requestTimeoutMs | Long | 60000 | HTTP 요청 타임아웃 (밀리초) |
| enableLogging | Boolean | false | HTTP 로깅 활성화 |
| rateLimitConfig | RateLimitConfig | businessInsiderDefault() | Rate Limit 설정 (10 RPS) |

#### RateLimitConfig 기본값 (Business Insider)

| 필드 | 기본값 |
|-----|-------|
| tokensPerSecond | 10.0 |
| capacity | 20.0 |

### 2.5 StreamingClientConfig

WebSocket Streaming 클라이언트 설정.

| 필드 | 타입 | 기본값 | 설명 |
|-----|------|-------|------|
| webSocketUrl | String | "wss://streamer.finance.yahoo.com/?version=2" | WebSocket 엔드포인트 URL |
| connectTimeoutMs | Long | 10000 | 연결 타임아웃 (밀리초) |
| heartbeatIntervalMs | Long | 15000 | 하트비트 주기 (밀리초) |
| pingTimeoutMs | Long | 30000 | Ping 타임아웃 (밀리초) |
| reconnection | ReconnectionConfig | ReconnectionConfig() | 재연결 설정 |
| eventBufferSize | Int | 64 | Flow 버퍼 크기 |
| enableLogging | Boolean | false | 로깅 활성화 |

#### ReconnectionConfig

| 필드 | 타입 | 기본값 | 설명 |
|-----|------|-------|------|
| enabled | Boolean | true | 재연결 활성화 |
| maxAttempts | Int | 5 | 최대 재시도 횟수 |
| initialDelayMs | Long | 1000 | 초기 대기 시간 (밀리초) |
| maxDelayMs | Long | 30000 | 최대 대기 시간 (밀리초) |
| backoffMultiplier | Double | 2.0 | 지수 백오프 배수 |

---

## 3. 클라이언트 구조

### 3.1 Ufc 클래스 구조

```kotlin
class Ufc private constructor(
    val yahoo: YahooClient,
    val fred: FredClient?,
    val businessInsider: BusinessInsiderClient,
    val streaming: StreamingClient
) : AutoCloseable
```

| 프로퍼티 | 타입 | Nullable | 설명 |
|---------|------|----------|------|
| yahoo | YahooClient | No | Yahoo Finance 클라이언트 |
| fred | FredClient | Yes | FRED 클라이언트 (API 키 없으면 null) |
| businessInsider | BusinessInsiderClient | No | Business Insider 클라이언트 |
| streaming | StreamingClient | No | WebSocket Streaming 클라이언트 |

### 3.2 생성 메서드

#### 기본 생성

```kotlin
suspend fun create(): Ufc
```

| 항목 | 값 |
|-----|---|
| 메서드 | companion object의 create() |
| 파라미터 | 없음 |
| 반환 | Ufc 인스턴스 |
| 특징 | FRED 클라이언트는 생성되지 않음 (fred = null) |

#### 설정과 함께 생성

```kotlin
suspend fun create(config: UfcConfig): Ufc
```

| 항목 | 값 |
|-----|---|
| 메서드 | companion object의 create(config) |
| 파라미터 | config: UfcConfig |
| 반환 | Ufc 인스턴스 |
| 특징 | config.fredApiKey가 있으면 FRED 클라이언트 생성 |

### 3.3 리소스 관리

```kotlin
override fun close()
```

| 항목 | 설명 |
|-----|------|
| 인터페이스 | AutoCloseable |
| 동작 | 모든 하위 클라이언트의 close() 호출 |
| 대상 | yahoo, fred, businessInsider, streaming |
| 권장 사용법 | use 블록 사용 |

---

## 4. API 메서드 요약

### 4.1 Yahoo Finance API

Ufc 파사드는 자주 사용되는 Yahoo API를 직접 노출하여 편의성을 제공한다.

#### 4.1.1 실시간 시세 (Quote)

| 메서드 | 파라미터 | 반환 | 설명 |
|-------|---------|-----|------|
| quote | symbol: String | QuoteData | 단일 심볼 시세 조회 |
| quote | symbols: List&lt;String&gt; | List&lt;QuoteData&gt; | 다중 심볼 시세 조회 |

```kotlin
val quote = ufc.quote("AAPL")
val quotes = ufc.quote(listOf("AAPL", "GOOGL", "MSFT"))
```

#### 4.1.2 상세 정보 (QuoteSummary)

| 메서드 | 파라미터 | 반환 | 설명 |
|-------|---------|-----|------|
| quoteSummary | symbol: String, vararg modules: QuoteSummaryModule | QuoteSummaryModuleResult | 모듈별 상세 정보 조회 |

```kotlin
val summary = ufc.quoteSummary("AAPL", QuoteSummaryModule.PRICE, QuoteSummaryModule.SUMMARY_DETAIL)
```

#### 4.1.3 히스토리컬 데이터 (Chart)

| 메서드 | 파라미터 | 반환 | 설명 |
|-------|---------|-----|------|
| chart | symbol: String, interval: Interval = OneDay, period: Period = OneYear, vararg events: ChartEventType | ChartData | OHLCV 및 이벤트 데이터 조회 |

```kotlin
val chart = ufc.chart("AAPL", Interval.OneDay, Period.OneYear)
val chartWithEvents = ufc.chart("AAPL", Interval.OneDay, Period.OneYear, ChartEventType.DIV, ChartEventType.SPLIT)
```

#### 4.1.4 실적 발표 일정 (Earnings Calendar)

| 메서드 | 파라미터 | 반환 | 설명 |
|-------|---------|-----|------|
| earningsCalendar | symbol: String, limit: Int = 12, offset: Int = 0 | EarningsCalendarData | 과거 및 예정된 실적 발표 조회 |

```kotlin
val earnings = ufc.earningsCalendar("AAPL")
val moreEarnings = ufc.earningsCalendar("AAPL", limit = 20, offset = 12)
```

#### 4.1.5 재무제표 시계열 (Fundamentals Timeseries)

| 메서드 | 파라미터 | 반환 | 설명 |
|-------|---------|-----|------|
| fundamentalsTimeseries | symbol: String, types: List&lt;FundamentalsType&gt;, startDate: LocalDate? = null, endDate: LocalDate? = null | FundamentalsTimeseriesResult | 재무제표 시계열 데이터 조회 |

```kotlin
val fundamentals = ufc.fundamentalsTimeseries(
    "AAPL",
    listOf(FundamentalsType.TOTAL_REVENUE, FundamentalsType.NET_INCOME)
)
```

#### 4.1.6 심볼 검색 (Lookup)

| 메서드 | 파라미터 | 반환 | 설명 |
|-------|---------|-----|------|
| lookup | query: String, type: LookupType = ALL, count: Int = 25 | LookupResult | 심볼, 이름으로 종목 검색 |

```kotlin
val result = ufc.lookup("Apple", LookupType.EQUITY, 10)
```

#### 4.1.7 시장 정보 (Market)

| 메서드 | 파라미터 | 반환 | 설명 |
|-------|---------|-----|------|
| marketSummary | market: MarketCode | MarketSummaryResult | 시장 요약 정보 조회 |
| marketTime | market: MarketCode | MarketTimeResult | 시장 거래 시간 조회 |

```kotlin
val summary = ufc.marketSummary(MarketCode.US)
val time = ufc.marketTime(MarketCode.US)
```

#### 4.1.8 옵션 (Options)

| 메서드 | 파라미터 | 반환 | 설명 |
|-------|---------|-----|------|
| options | symbol: String, expirationDate: Long? = null | OptionsData | 옵션 체인 조회 |

```kotlin
val options = ufc.options("AAPL")
val optionsByDate = ufc.options("AAPL", expirationDate = 1735689600L)
```

#### 4.1.9 스크리너 (Screener)

| 메서드 | 파라미터 | 반환 | 설명 |
|-------|---------|-----|------|
| screener | query: ScreenerQuery, sortField: ScreenerSortField = TICKER, sortAsc: Boolean = false, size: Int = 100, offset: Int = 0 | ScreenerResult | 커스텀 쿼리로 종목 스크리닝 |
| screener | predefinedId: String, count: Int = 25, sortField: ScreenerSortField? = null, sortAsc: Boolean? = null | ScreenerResult | 사전정의 ID로 스크리닝 |
| screener | predefined: PredefinedScreener, count: Int = 25, sortField: ScreenerSortField? = null, sortAsc: Boolean? = null | ScreenerResult | 사전정의 Enum으로 스크리닝 |

```kotlin
val result1 = ufc.screener(
    ScreenerQuery.Builder().build(),
    ScreenerSortField.TICKER
)
val result2 = ufc.screener(PredefinedScreener.MOST_ACTIVE, 50)
```

#### 4.1.10 검색 (Search)

| 메서드 | 파라미터 | 반환 | 설명 |
|-------|---------|-----|------|
| search | query: String, quotesCount: Int = 8, newsCount: Int = 8, enableFuzzyQuery: Boolean = false | SearchResponse | 통합 검색 (종목 + 뉴스) |

```kotlin
val result = ufc.search("Apple", quotesCount = 10, newsCount = 5)
```

#### 4.1.11 실적일 조회 (Visualization)

| 메서드 | 파라미터 | 반환 | 설명 |
|-------|---------|-----|------|
| visualization | symbol: String, limit: Int = 12 | VisualizationEarningsCalendar | 과거 및 예정된 실적 발표일 조회 |

```kotlin
val viz = ufc.visualization("AAPL")
```

### 4.2 FRED API

FRED API는 fred 프로퍼티를 통해 접근하거나 파사드 메서드를 사용한다.

#### 4.2.1 시계열 데이터 (Series)

| 메서드 | 파라미터 | 반환 | 설명 |
|-------|---------|-----|------|
| series | seriesId: String, startDate: LocalDate? = null, endDate: LocalDate? = null, frequency: DataFrequency? = null | SeriesData | 경제 지표 시계열 조회 |

```kotlin
// 파사드 메서드 사용
val gdp = ufc.series("GDP", startDate = LocalDate.of(2020, 1, 1))

// 직접 접근
val cpi = ufc.fred?.series("CPIAUCSL")
```

**주의**: `fredApiKey`가 설정되지 않았으면 `CONFIGURATION_ERROR` 예외 발생.

#### 4.2.2 시리즈 메타데이터 (SeriesInfo)

| 메서드 | 파라미터 | 반환 | 설명 |
|-------|---------|-----|------|
| seriesInfo | seriesId: String | SeriesInfo | 시리즈 메타데이터 조회 |

```kotlin
val info = ufc.fred?.seriesInfo("GDP")
```

### 4.3 Business Insider API

Business Insider API는 businessInsider 프로퍼티 또는 파사드 메서드를 통해 접근한다.

#### 4.3.1 ISIN 검색 (Search ISIN)

| 메서드 | 파라미터 | 반환 | 설명 |
|-------|---------|-----|------|
| searchIsin | isin: String | IsinSearchResult | ISIN 코드로 심볼 검색 |

```kotlin
// 파사드 메서드 사용
val result = ufc.searchIsin("US0378331005")

// 직접 접근
val result2 = ufc.businessInsider.searchIsin("US0378331005")
```

### 4.4 WebSocket Streaming API

Streaming API는 streaming 프로퍼티를 통해 접근한다.

#### 4.4.1 연결 관리

| 메서드 | 파라미터 | 반환 | 설명 |
|-------|---------|-----|------|
| connect | 없음 | Unit | WebSocket 연결 수립 |
| disconnect | 없음 | Unit | WebSocket 연결 종료 |
| isConnected | 없음 | Boolean | 연결 상태 확인 |

```kotlin
ufc.streaming.connect()
val connected = ufc.streaming.isConnected()
ufc.streaming.disconnect()
```

#### 4.4.2 구독 관리

| 메서드 | 파라미터 | 반환 | 설명 |
|-------|---------|-----|------|
| subscribe | symbol: String | Unit | 단일 심볼 구독 |
| subscribe | symbols: List&lt;String&gt; | Unit | 다중 심볼 구독 |
| unsubscribe | symbol: String | Unit | 단일 심볼 구독 해제 |
| unsubscribe | symbols: List&lt;String&gt; | Unit | 다중 심볼 구독 해제 |
| unsubscribeAll | 없음 | Unit | 모든 구독 해제 |
| getSubscribedSymbols | 없음 | Set&lt;String&gt; | 현재 구독 목록 조회 |

```kotlin
ufc.streaming.subscribe("AAPL")
ufc.streaming.subscribe(listOf("GOOGL", "MSFT"))
val subscribed = ufc.streaming.getSubscribedSymbols()
ufc.streaming.unsubscribe("AAPL")
ufc.streaming.unsubscribeAll()
```

#### 4.4.3 데이터 수신

| 프로퍼티/메서드 | 타입 | 설명 |
|---------------|------|------|
| prices | SharedFlow&lt;StreamingPrice&gt; | 실시간 가격 스트림 (모든 심볼) |
| quotes | SharedFlow&lt;StreamingQuote&gt; | 실시간 상세 시세 스트림 (모든 심볼) |
| events | SharedFlow&lt;StreamingEvent&gt; | 연결/에러 이벤트 스트림 |
| pricesBySymbol | (symbol: String) -&gt; Flow&lt;StreamingPrice&gt; | 특정 심볼의 가격 스트림 |
| quotesBySymbol | (symbol: String) -&gt; Flow&lt;StreamingQuote&gt; | 특정 심볼의 상세 시세 스트림 |

```kotlin
// 모든 심볼의 가격 수신
ufc.streaming.prices.collect { price ->
    println("${price.symbol}: ${price.price}")
}

// 특정 심볼의 가격만 수신
ufc.streaming.pricesBySymbol("AAPL").collect { price ->
    println("AAPL: ${price.price}")
}

// 이벤트 모니터링
ufc.streaming.events.collect { event ->
    when (event) {
        is StreamingEvent.Connected -> println("Connected")
        is StreamingEvent.Disconnected -> println("Disconnected: ${event.reason}")
        is StreamingEvent.Error -> println("Error: ${event.exception}")
        else -> {}
    }
}
```

---

## 5. 예외 처리

### 5.1 공통 예외

모든 API는 `UfcException`을 발생시킨다.

```kotlin
class UfcException(
    val errorCode: ErrorCode,
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
```

### 5.2 ErrorCode 목록

#### 5.2.1 WebSocket 오류 (1000번대)

| ErrorCode | 코드 | 메시지 | 재시도 |
|-----------|------|-------|-------|
| WEBSOCKET_CONNECTION_FAILED | 1010 | WebSocket 연결에 실패했습니다. | Yes |
| WEBSOCKET_HANDSHAKE_FAILED | 1011 | WebSocket 핸드셰이크에 실패했습니다. | Yes |
| WEBSOCKET_CLOSED_BY_SERVER | 1012 | 서버가 WebSocket 연결을 종료했습니다. | Yes |
| WEBSOCKET_PROTOCOL_ERROR | 1013 | WebSocket 프로토콜 오류가 발생했습니다. | No |
| WEBSOCKET_MESSAGE_TOO_LARGE | 1014 | WebSocket 메시지가 너무 큽니다. | No |
| STREAMING_RECONNECTION_FAILED | 1020 | 재연결에 실패했습니다. | No |

#### 5.2.2 인증 오류 (2000번대)

| ErrorCode | 코드 | 메시지 | 재시도 |
|-----------|------|-------|-------|
| AUTHENTICATION_FAILED | 2001 | 인증에 실패했습니다. | No |

#### 5.2.3 Rate Limiting 오류 (3000번대)

| ErrorCode | 코드 | 메시지 | 재시도 |
|-----------|------|-------|-------|
| RATE_LIMIT_EXCEEDED | 3001 | Rate Limit을 초과했습니다. | Yes |

#### 5.2.4 데이터 오류 (4000번대)

| ErrorCode | 코드 | 메시지 | 재시도 |
|-----------|------|-------|-------|
| DATA_NOT_FOUND | 4001 | 요청한 데이터를 찾을 수 없습니다. | No |
| INVALID_SYMBOL | 4002 | 유효하지 않은 심볼입니다. | No |

#### 5.2.5 파싱 오류 (5000번대)

| ErrorCode | 코드 | 메시지 | 재시도 |
|-----------|------|-------|-------|
| JSON_PARSING_ERROR | 5001 | JSON 파싱 중 오류가 발생했습니다. | No |
| DATA_PARSING_ERROR | 5010 | 데이터 파싱 중 오류가 발생했습니다. | No |
| PROTOBUF_DECODING_ERROR | 5011 | Protobuf 디코딩 중 오류가 발생했습니다. | No |

#### 5.2.6 파라미터/구독 오류 (6000번대)

| ErrorCode | 코드 | 메시지 | 재시도 |
|-----------|------|-------|-------|
| INVALID_PARAMETER | 6001 | 유효하지 않은 파라미터입니다. | No |
| STREAMING_SUBSCRIPTION_FAILED | 6010 | 스트리밍 구독에 실패했습니다. | No |
| STREAMING_MAX_SUBSCRIPTIONS_EXCEEDED | 6011 | 최대 구독 개수를 초과했습니다. | No |

#### 5.2.7 서버 오류 (7000번대)

| ErrorCode | 코드 | 메시지 | 재시도 |
|-----------|------|-------|-------|
| EXTERNAL_API_ERROR | 7004 | 외부 API 오류가 발생했습니다. | Yes |

#### 5.2.8 설정 오류 (9000번대)

| ErrorCode | 코드 | 메시지 | 재시도 |
|-----------|------|-------|-------|
| CONFIGURATION_ERROR | 9002 | 설정 오류가 발생했습니다. | No |

### 5.3 예외 처리 예시

```kotlin
try {
    val quote = ufc.quote("AAPL")
    println("Price: ${quote.pricing?.price}")
} catch (e: UfcException) {
    when (e.errorCode) {
        ErrorCode.INVALID_SYMBOL -> println("Invalid symbol")
        ErrorCode.RATE_LIMIT_EXCEEDED -> println("Rate limit exceeded, please retry later")
        ErrorCode.NETWORK_ERROR -> println("Network error, check connectivity")
        else -> println("Error: ${e.message}")
    }
}
```

---

## 6. 사용 예시

### 6.1 기본 사용

```kotlin
suspend fun basicUsage() {
    // 기본 생성 (FRED 없음)
    Ufc.create().use { ufc ->
        // Yahoo API
        val quote = ufc.quote("AAPL")
        println("AAPL Price: ${quote.pricing?.price}")

        // Business Insider API
        val isinResult = ufc.searchIsin("US0378331005")
        println("Symbol: ${isinResult.symbol}")
    }
}
```

### 6.2 설정과 함께 사용

```kotlin
suspend fun configuredUsage() {
    val config = UfcConfig(
        fredApiKey = "your-fred-api-key",
        yahooConfig = YahooClientConfig(
            enableLogging = true,
            requestTimeoutMs = 30_000
        ),
        fredConfig = FredClientConfig(
            enableLogging = true
        )
    )

    Ufc.create(config).use { ufc ->
        // Yahoo API
        val chart = ufc.chart("AAPL", Interval.OneDay, Period.OneMonth)

        // FRED API (API 키가 있으므로 사용 가능)
        val gdp = ufc.series("GDP")

        // Business Insider API
        val isinResult = ufc.searchIsin("US0378331005")
    }
}
```

### 6.3 WebSocket Streaming 사용

```kotlin
suspend fun streamingUsage() {
    Ufc.create().use { ufc ->
        // 이벤트 모니터링
        launch {
            ufc.streaming.events.collect { event ->
                println("Event: $event")
            }
        }

        // 실시간 가격 구독
        launch {
            ufc.streaming.pricesBySymbol("AAPL").collect { price ->
                println("AAPL: ${price.price} (${price.changePercent}%)")
            }
        }

        // 구독 시작
        ufc.streaming.subscribe(listOf("AAPL", "GOOGL", "MSFT"))

        // 데이터 수신 대기
        delay(60_000)

        // 구독 해제
        ufc.streaming.unsubscribeAll()
    }
}
```

### 6.4 다중 데이터 소스 조합

```kotlin
suspend fun multiSourceUsage() {
    Ufc.create(UfcConfig(fredApiKey = "your-key")).use { ufc ->
        val symbol = "AAPL"

        // Yahoo: 실시간 시세
        val quote = ufc.quote(symbol)

        // Yahoo: 히스토리컬 데이터
        val chart = ufc.chart(symbol, Interval.OneDay, Period.OneYear)

        // Yahoo: 재무제표
        val fundamentals = ufc.fundamentalsTimeseries(
            symbol,
            listOf(FundamentalsType.TOTAL_REVENUE, FundamentalsType.NET_INCOME)
        )

        // FRED: 경제 지표
        val gdp = ufc.series("GDP")
        val unemployment = ufc.series("UNRATE")

        // Business Insider: ISIN 조회
        val isinResult = ufc.searchIsin("US0378331005")

        // 통합 분석
        analyzeStockWithEconomicData(quote, chart, fundamentals, gdp, unemployment)
    }
}
```

### 6.5 에러 처리

```kotlin
suspend fun errorHandling() {
    Ufc.create().use { ufc ->
        try {
            // FRED API 키가 없으면 에러
            val gdp = ufc.series("GDP")
        } catch (e: UfcException) {
            if (e.errorCode == ErrorCode.CONFIGURATION_ERROR) {
                println("FRED API key not configured")
                // fred 프로퍼티가 null인지 확인하여 우회
                if (ufc.fred == null) {
                    println("Using Yahoo data only")
                }
            }
        }

        try {
            // 잘못된 심볼
            val quote = ufc.quote("INVALID_SYMBOL")
        } catch (e: UfcException) {
            when (e.errorCode) {
                ErrorCode.INVALID_SYMBOL -> println("Symbol not found")
                ErrorCode.DATA_NOT_FOUND -> println("No data available")
                else -> throw e
            }
        }
    }
}
```

### 6.6 직접 접근 vs 파사드 메서드

```kotlin
suspend fun directAccessVsFacade() {
    Ufc.create().use { ufc ->
        // 파사드 메서드 사용 (권장)
        val quote1 = ufc.quote("AAPL")
        val chart1 = ufc.chart("AAPL", Interval.OneDay, Period.OneYear)

        // 직접 접근 (추가 메서드 사용 시)
        val quote2 = ufc.yahoo.quote("AAPL")
        val chart2 = ufc.yahoo.chart("AAPL", Interval.OneDay, Period.OneYear)

        // FRED는 직접 접근 필요 (seriesInfo는 파사드에 없음)
        val seriesInfo = ufc.fred?.seriesInfo("GDP")
    }
}
```

---

## 7. 참고 자료

### 7.1 설계 원칙

| 원칙 | 설명 |
|-----|------|
| Facade Pattern | 복잡한 서브시스템을 단순한 인터페이스로 추상화 |
| Delegation | 실제 구현은 각 클라이언트에 위임 |
| Configuration Object | 모든 설정을 단일 객체로 관리 |
| Resource Management | AutoCloseable로 리소스 자동 정리 |
| Fail-Fast | 잘못된 설정은 초기화 시점에 검증 |

### 7.2 Global Rate Limiters

Ufc는 내부적으로 `GlobalRateLimiters` 싱글톤을 사용하여 여러 Ufc 인스턴스 간에도 Rate Limit을 공유한다.

| 데이터 소스 | Rate Limiter 키 | 기본 RPS |
|-----------|----------------|---------|
| Yahoo Finance | "yahoo" | 50 |
| FRED | "fred" | 2 |
| Business Insider | "businessInsider" | 10 |

```kotlin
// 여러 인스턴스가 동일한 Rate Limiter 공유
val ufc1 = Ufc.create()
val ufc2 = Ufc.create()
// ufc1과 ufc2는 동일한 Yahoo Rate Limiter 사용
```

### 7.3 의존성

| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| Kotlin | 2.1.0 | 언어 |
| kotlinx.coroutines | 1.10.1 | 비동기 처리 |
| Ktor Client | 3.0.1 | HTTP 클라이언트 |
| kotlinx.serialization | 1.7.3 | JSON 직렬화 |
| kotlinx-datetime | 0.6.1 | 날짜/시간 처리 |

### 7.4 관련 문서

| 문서 | 설명 |
|-----|------|
| quote-api-spec.md | Quote API 상세 명세 |
| websocket-streaming-api-spec.md | WebSocket Streaming API 상세 명세 |
| earnings-calendar-api-spec.md | Earnings Calendar API 상세 명세 |
| fundamentals-timeseries-api-spec.md | Fundamentals Timeseries API 상세 명세 |
| lookup-api-spec.md | Lookup API 상세 명세 |
| market-api-spec.md | Market API 상세 명세 |
| options-api-spec.md | Options API 상세 명세 |
| screener-api-spec.md | Screener API 상세 명세 |
| search-api-spec.md | Search API 상세 명세 |
| visualization-api-spec.md | Visualization API 상세 명세 |

### 7.5 Best Practices

| 항목 | 권장 사항 |
|-----|---------|
| 리소스 정리 | 항상 `use` 블록 사용 |
| FRED API 키 | 환경 변수로 관리 (local.properties 또는 env) |
| 에러 처리 | ErrorCode별로 적절한 복구 전략 구현 |
| 로깅 | 개발 환경에서만 enableLogging = true |
| Rate Limit | 기본값 사용 권장, 필요 시 조정 |
| WebSocket | 장시간 연결 시 reconnection 설정 확인 |
| FRED null 체크 | fred 사용 전 null 체크 또는 series() 파사드 메서드 사용 |

### 7.6 제약사항

| 제약 | 설명 |
|-----|------|
| FRED API 키 | 선택적, 없으면 FRED 기능 사용 불가 |
| 비공식 API | Yahoo Finance는 문서화되지 않은 API |
| Rate Limit | 각 데이터 소스별 제한 존재 |
| WebSocket 연결 | 장시간 유지 시 리소스 사용 |
| 싱글톤 아님 | 여러 인스턴스 생성 가능 (Rate Limiter는 공유) |

### 7.7 용어

| 용어 | 설명 |
|-----|------|
| Facade | 복잡한 서브시스템을 단순한 인터페이스로 감싸는 디자인 패턴 |
| Delegation | 실제 작업을 다른 객체에 위임하는 패턴 |
| AutoCloseable | 자동 리소스 관리를 위한 Kotlin/Java 인터페이스 |
| Rate Limiter | API 요청 빈도를 제한하는 메커니즘 |
| Token Bucket | Rate Limiting 알고리즘의 일종 |
| Global Rate Limiter | 여러 인스턴스가 공유하는 Rate Limiter |
| Suspend Function | Kotlin 코루틴의 일시 중단 함수 |
