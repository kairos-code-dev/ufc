# Market API 기능 명세서

## 문서 정보
- 작성일: 2025-12-05
- 버전: 1.0
- 대상 API: Yahoo Finance Market Summary & Market Time API
- 프로젝트: UFC (Unified Finance Client)

---

## 1. API 개요

### 1.1 목적
Market API는 Yahoo Finance로부터 다음 두 가지 핵심 시장 정보를 제공합니다:

1. **Market Summary (시장 요약 정보)**
   - 특정 시장의 주요 지수 및 섹터별 현재 가격, 변동폭 정보 제공
   - 예: 미국 시장의 S&P 500, 나스닥, 다우존스 지수 등
   - 예: 한국 시장의 코스피, 코스닥 지수 등

2. **Market Time (시장 시간 정보)**
   - 특정 시장의 거래 시간대 및 현재 상태 정보 제공
   - 개장/폐장 시각, 타임존, 현재 거래 상태 등

### 1.2 주요 사용 사례
- 대시보드에서 주요 시장 지수 현황 표시
- 시장 개장/폐장 상태 확인
- 거래 가능 시간 체크
- 글로벌 시장 모니터링 (미국, 한국, 일본 등)
- 시장별 타임존 정보 제공

---

## 2. Yahoo Finance Market API 분석

### 2.1 Market Summary Endpoint

#### 엔드포인트
```
GET https://query1.finance.yahoo.com/v6/finance/quote/marketSummary
```

#### 요청 파라미터

| 파라미터 | 타입 | 필수 | 설명 | 예시 값 |
|---------|------|------|------|---------|
| `market` | String | Y | 조회할 시장 코드 | "us", "kr", "jp", "gb", "de" |
| `fields` | String | N | 반환받을 필드 목록 (쉼표 구분) | "shortName,regularMarketPrice,regularMarketChange,regularMarketChangePercent" |
| `formatted` | Boolean | N | 포맷팅된 문자열 포함 여부 | true, false (기본값: false) |
| `lang` | String | N | 언어 코드 | "en-US", "ko-KR", "ja-JP" |

#### 지원 Market 코드

| Market 코드 | 설명 | 주요 지수 예시 |
|------------|------|--------------|
| `us` | 미국 시장 | ^GSPC (S&P 500), ^IXIC (NASDAQ), ^DJI (DOW) |
| `kr` | 한국 시장 | ^KS11 (KOSPI), ^KQ11 (KOSDAQ) |
| `jp` | 일본 시장 | ^N225 (NIKKEI 225) |
| `gb` | 영국 시장 | ^FTSE (FTSE 100) |
| `de` | 독일 시장 | ^GDAXI (DAX) |
| `hk` | 홍콩 시장 | ^HSI (HANG SENG) |
| `cn` | 중국 시장 | 000001.SS (SSE Composite) |
| `fr` | 프랑스 시장 | ^FCHI (CAC 40) |

#### 응답 JSON 구조

```json
{
  "marketSummaryResponse": {
    "result": [
      {
        "exchange": "NMS",
        "symbol": "^GSPC",
        "shortName": "S&P 500",
        "regularMarketPrice": 4783.45,
        "regularMarketChange": 23.45,
        "regularMarketChangePercent": 0.49,
        "regularMarketTime": 1701993600,
        "regularMarketDayHigh": 4790.12,
        "regularMarketDayLow": 4765.33,
        "regularMarketVolume": 0,
        "regularMarketPreviousClose": 4760.00,
        "currency": "USD",
        "marketState": "POST",
        "quoteType": "INDEX",
        "sourceInterval": 15,
        "exchangeTimezoneName": "America/New_York",
        "exchangeTimezoneShortName": "EST",
        "gmtOffSetMilliseconds": -18000000
      },
      {
        "exchange": "NMS",
        "symbol": "^IXIC",
        "shortName": "NASDAQ",
        "regularMarketPrice": 15095.14,
        "regularMarketChange": 85.34,
        "regularMarketChangePercent": 0.57,
        ...
      }
    ],
    "error": null
  }
}
```

#### 주요 응답 필드

| 필드명 | 타입 | Nullable | 설명 |
|-------|------|----------|------|
| `exchange` | String | N | 거래소 코드 (예: NMS, NYQ, KSC) |
| `symbol` | String | N | 지수 심볼 (예: ^GSPC, ^IXIC) |
| `shortName` | String | N | 짧은 이름 (예: S&P 500, NASDAQ) |
| `regularMarketPrice` | Double | Y | 현재가 |
| `regularMarketChange` | Double | Y | 전일 대비 변동폭 |
| `regularMarketChangePercent` | Double | Y | 전일 대비 변동률 (%) |
| `regularMarketTime` | Long | Y | 마지막 업데이트 시각 (Unix timestamp) |
| `regularMarketDayHigh` | Double | Y | 당일 최고가 |
| `regularMarketDayLow` | Double | Y | 당일 최저가 |
| `regularMarketVolume` | Long | Y | 거래량 |
| `regularMarketPreviousClose` | Double | Y | 전일 종가 |
| `currency` | String | Y | 통화 코드 (USD, KRW, JPY 등) |
| `marketState` | String | Y | 시장 상태 (PRE, REGULAR, POST, CLOSED) |
| `quoteType` | String | Y | 자산 타입 (INDEX, EQUITY 등) |
| `exchangeTimezoneName` | String | Y | 타임존 IANA 이름 |
| `exchangeTimezoneShortName` | String | Y | 타임존 약어 (EST, KST 등) |
| `gmtOffSetMilliseconds` | Long | Y | GMT 오프셋 (밀리초) |

### 2.2 Market Time Endpoint

#### 엔드포인트
```
GET https://query1.finance.yahoo.com/v6/finance/markettime
```

#### 요청 파라미터

| 파라미터 | 타입 | 필수 | 설명 | 예시 값 |
|---------|------|------|------|---------|
| `market` | String | Y | 조회할 시장 코드 | "us", "kr", "jp" |
| `formatted` | Boolean | N | 포맷팅된 문자열 포함 여부 | true, false (기본값: true) |
| `lang` | String | N | 언어 코드 | "en-US", "ko-KR" |
| `key` | String | N | API 키 (일반적으로 "finance" 고정) | "finance" |

#### 응답 JSON 구조

```json
{
  "finance": {
    "marketTimes": [
      {
        "marketTime": [
          {
            "exchange": "US",
            "market": "us_market",
            "marketState": "REGULAR",
            "open": "2025-12-05T09:30:00-05:00",
            "close": "2025-12-05T16:00:00-05:00",
            "preMarket": {
              "start": "2025-12-05T04:00:00-05:00",
              "end": "2025-12-05T09:30:00-05:00"
            },
            "postMarket": {
              "start": "2025-12-05T16:00:00-05:00",
              "end": "2025-12-05T20:00:00-05:00"
            },
            "timezone": [
              {
                "short": "EST",
                "name": "America/New_York",
                "gmtoffset": -18000000
              }
            ],
            "time": "2025-12-05T14:23:15-05:00"
          }
        ]
      }
    ],
    "error": null
  }
}
```

#### 주요 응답 필드

| 필드명 | 타입 | Nullable | 설명 |
|-------|------|----------|------|
| `exchange` | String | N | 거래소 코드 |
| `market` | String | N | 시장 식별자 |
| `marketState` | String | N | 현재 시장 상태 (PRE, REGULAR, POST, CLOSED) |
| `open` | String (ISO 8601) | N | 정규 장 개장 시각 |
| `close` | String (ISO 8601) | N | 정규 장 폐장 시각 |
| `preMarket.start` | String (ISO 8601) | Y | 프리마켓 시작 시각 |
| `preMarket.end` | String (ISO 8601) | Y | 프리마켓 종료 시각 |
| `postMarket.start` | String (ISO 8601) | Y | 애프터마켓 시작 시각 |
| `postMarket.end` | String (ISO 8601) | Y | 애프터마켓 종료 시각 |
| `timezone[0].short` | String | N | 타임존 약어 (EST, KST) |
| `timezone[0].name` | String | N | 타임존 IANA 이름 |
| `timezone[0].gmtoffset` | Long | N | GMT 오프셋 (밀리초) |
| `time` | String (ISO 8601) | Y | 현재 시각 (해당 시장 타임존) |

---

## 3. UFC 통합 설계

### 3.1 아키텍처 통합 방안

#### 기존 UFC 아키텍처 레이어
```
ufc/
├── yahoo/                    # Yahoo Finance 클라이언트 레이어
│   ├── YahooClient.kt       # 저수준 API 클라이언트
│   ├── model/               # Yahoo 도메인 모델
│   └── internal/            # 내부 구현 (response, auth 등)
```

#### Market API 추가 레이어
```
ufc/
├── yahoo/
│   ├── YahooClient.kt           # 기존: quoteSummary(), chart()
│   │                            # 추가: marketSummary(), marketTime()
│   ├── model/
│   │   ├── ChartData.kt
│   │   ├── QuoteSummaryModule.kt
│   │   └── MarketData.kt        # 신규 추가
│   └── internal/
│       ├── response/
│       │   ├── ChartDataResponse.kt
│       │   ├── QuoteSummaryResponse.kt
│       │   └── MarketResponse.kt    # 신규 추가
│       └── YahooApiUrls.kt          # MARKET_SUMMARY, MARKET_TIME 추가
```

### 3.2 네임스페이스 배치

#### YahooClient 클래스에 메서드 추가
기존 `YahooClient`는 Yahoo Finance의 모든 low-level API를 제공하는 역할을 수행합니다.
Market API도 동일한 패턴으로 `YahooClient`에 메서드를 추가합니다.

```kotlin
class YahooClient {
    // 기존 API
    suspend fun quoteSummary(symbol: String, modules: Set<QuoteSummaryModule>): QuoteSummaryModuleResult
    suspend fun chart(symbol: String, interval: Interval, period: Period): ChartData

    // 신규 Market API
    suspend fun marketSummary(market: MarketCode): MarketSummaryResult
    suspend fun marketTime(market: MarketCode): MarketTimeResult
}
```

### 3.3 필요한 모델 클래스 목록

#### 3.3.1 Public 도메인 모델 (yahoo/model/)

##### MarketData.kt
- **MarketCode (enum class)**: 지원 시장 코드 열거형
  - 역할: us, kr, jp 등 시장 코드 정의
  - 속성: code (String), description (String)

- **MarketState (enum class)**: 시장 상태 열거형
  - 역할: PRE, REGULAR, POST, CLOSED 정의
  - 속성: value (String)

- **MarketSummaryItem (data class)**: 개별 지수 요약 정보
  - 역할: 하나의 지수 정보 (예: S&P 500)
  - 속성: exchange, symbol, shortName, regularMarketPrice, regularMarketChange, regularMarketChangePercent, regularMarketTime, currency, marketState 등

- **MarketSummaryResult (data class)**: Market Summary API 응답 결과
  - 역할: 특정 시장의 모든 지수 목록
  - 속성: market (MarketCode), items (List<MarketSummaryItem>)

- **MarketTimezone (data class)**: 타임존 정보
  - 역할: 시장의 타임존 데이터
  - 속성: shortName (String), ianaName (String), gmtOffsetMillis (Long)

- **TradingHours (data class)**: 거래 시간 정보
  - 역할: 개장/폐장 시각 표현
  - 속성: start (Instant), end (Instant)

- **MarketTimeResult (data class)**: Market Time API 응답 결과
  - 역할: 시장 시간 정보
  - 속성: market (MarketCode), marketState (MarketState), open (Instant), close (Instant), preMarket (TradingHours?), postMarket (TradingHours?), timezone (MarketTimezone), currentTime (Instant)

#### 3.3.2 Internal 응답 모델 (yahoo/internal/response/)

##### MarketResponse.kt
- **MarketSummaryResponse (data class)**: Yahoo API 원본 응답
  - 역할: /v6/finance/quote/marketSummary 응답 역직렬화
  - 속성: marketSummaryResponse (MarketSummaryResponseWrapper)

- **MarketSummaryResponseWrapper (data class)**
  - 속성: result (List<MarketSummaryItemResponse>), error (ErrorResponse?)

- **MarketSummaryItemResponse (data class)**: 개별 지수 응답
  - 속성: exchange, symbol, shortName, regularMarketPrice, regularMarketChange 등 (Yahoo API 필드와 1:1 매핑)

- **MarketTimeResponse (data class)**: Yahoo API 원본 응답
  - 역할: /v6/finance/markettime 응답 역직렬화
  - 속성: finance (FinanceWrapper)

- **FinanceWrapper (data class)**
  - 속성: marketTimes (List<MarketTimeWrapper>), error (ErrorResponse?)

- **MarketTimeWrapper (data class)**
  - 속성: marketTime (List<MarketTimeItemResponse>)

- **MarketTimeItemResponse (data class)**: 시장 시간 정보 응답
  - 속성: exchange, market, marketState, open, close, preMarket, postMarket, timezone, time

- **PrePostMarketResponse (data class)**
  - 속성: start (String), end (String)

- **TimezoneResponse (data class)**
  - 속성: short (String), name (String), gmtoffset (Long)

### 3.4 API 메서드 시그니처 정의

#### YahooClient.kt에 추가될 메서드

```kotlin
/**
 * Market Summary API를 호출하여 시장 요약 정보를 조회합니다.
 *
 * @param market 조회할 시장 코드
 * @return MarketSummaryResult
 * @throws ApiException API 호출 실패 시
 */
suspend fun marketSummary(market: MarketCode): MarketSummaryResult

/**
 * Market Time API를 호출하여 시장 시간 정보를 조회합니다.
 *
 * @param market 조회할 시장 코드
 * @return MarketTimeResult
 * @throws ApiException API 호출 실패 시
 */
suspend fun marketTime(market: MarketCode): MarketTimeResult
```

#### YahooApiUrls.kt에 추가될 상수

```kotlin
object YahooApiUrls {
    // 기존 상수들...
    const val CHART = "$QUERY2/v8/finance/chart"
    const val QUOTE_SUMMARY = "$QUERY2/v10/finance/quoteSummary"

    // 신규 추가
    const val MARKET_SUMMARY = "$QUERY1/v6/finance/quote/marketSummary"
    const val MARKET_TIME = "$QUERY1/v6/finance/markettime"
}
```

---

## 4. 데이터 매핑

### 4.1 Market Summary 매핑 규칙

#### Yahoo 응답 → UFC 도메인 모델

| Yahoo 필드 | UFC 모델 필드 | 타입 변환 | Nullable 처리 |
|-----------|--------------|----------|--------------|
| `exchange` | `MarketSummaryItem.exchange` | String → String | Non-null (필수) |
| `symbol` | `MarketSummaryItem.symbol` | String → String | Non-null (필수) |
| `shortName` | `MarketSummaryItem.shortName` | String → String | Non-null (필수) |
| `regularMarketPrice` | `MarketSummaryItem.regularMarketPrice` | Double? → Double? | Nullable 유지 |
| `regularMarketChange` | `MarketSummaryItem.regularMarketChange` | Double? → Double? | Nullable 유지 |
| `regularMarketChangePercent` | `MarketSummaryItem.regularMarketChangePercent` | Double? → Double? | Nullable 유지 |
| `regularMarketTime` | `MarketSummaryItem.regularMarketTime` | Long? → Instant? | Long을 Instant로 변환 |
| `regularMarketDayHigh` | `MarketSummaryItem.regularMarketDayHigh` | Double? → Double? | Nullable 유지 |
| `regularMarketDayLow` | `MarketSummaryItem.regularMarketDayLow` | Double? → Double? | Nullable 유지 |
| `regularMarketVolume` | `MarketSummaryItem.regularMarketVolume` | Long? → Long? | Nullable 유지 |
| `regularMarketPreviousClose` | `MarketSummaryItem.regularMarketPreviousClose` | Double? → Double? | Nullable 유지 |
| `currency` | `MarketSummaryItem.currency` | String? → String? | Nullable 유지 |
| `marketState` | `MarketSummaryItem.marketState` | String? → MarketState? | String을 MarketState enum으로 변환 |
| `quoteType` | `MarketSummaryItem.quoteType` | String? → String? | Nullable 유지 |
| `exchangeTimezoneName` | `MarketSummaryItem.timezoneName` | String? → String? | Nullable 유지 |
| `exchangeTimezoneShortName` | `MarketSummaryItem.timezoneShortName` | String? → String? | Nullable 유지 |
| `gmtOffSetMilliseconds` | `MarketSummaryItem.gmtOffsetMillis` | Long? → Long? | Nullable 유지 |

### 4.2 Market Time 매핑 규칙

#### Yahoo 응답 → UFC 도메인 모델

| Yahoo 필드 | UFC 모델 필드 | 타입 변환 | Nullable 처리 |
|-----------|--------------|----------|--------------|
| `exchange` | `MarketTimeResult.exchange` | String → String | Non-null (필수) |
| `market` | `MarketTimeResult.marketIdentifier` | String → String | Non-null (필수) |
| `marketState` | `MarketTimeResult.marketState` | String → MarketState | String을 MarketState enum으로 변환 |
| `open` | `MarketTimeResult.open` | String (ISO 8601) → Instant | ISO 8601 파싱 |
| `close` | `MarketTimeResult.close` | String (ISO 8601) → Instant | ISO 8601 파싱 |
| `preMarket.start` | `MarketTimeResult.preMarket.start` | String? → Instant? | ISO 8601 파싱, null 허용 |
| `preMarket.end` | `MarketTimeResult.preMarket.end` | String? → Instant? | ISO 8601 파싱, null 허용 |
| `postMarket.start` | `MarketTimeResult.postMarket.start` | String? → Instant? | ISO 8601 파싱, null 허용 |
| `postMarket.end` | `MarketTimeResult.postMarket.end` | String? → Instant? | ISO 8601 파싱, null 허용 |
| `timezone[0].short` | `MarketTimeResult.timezone.shortName` | String → String | Non-null (필수) |
| `timezone[0].name` | `MarketTimeResult.timezone.ianaName` | String → String | Non-null (필수) |
| `timezone[0].gmtoffset` | `MarketTimeResult.timezone.gmtOffsetMillis` | Long → Long | Non-null (필수) |
| `time` | `MarketTimeResult.currentTime` | String? (ISO 8601) → Instant? | ISO 8601 파싱, null 허용 |

### 4.3 타입 변환 규칙

#### ISO 8601 문자열 → Instant
- 변환 방법: `Instant.parse(isoString)`
- 예시: `"2025-12-05T09:30:00-05:00"` → `Instant`
- 예외 처리: 파싱 실패 시 `DataParsingException` 발생

#### Unix Timestamp (Long) → Instant
- 변환 방법: `Instant.ofEpochSecond(timestamp)`
- 예시: `1701993600L` → `Instant`

#### String → MarketState enum
- 매핑 규칙:
  - `"PRE"` → `MarketState.PRE`
  - `"REGULAR"` → `MarketState.REGULAR`
  - `"POST"` → `MarketState.POST`
  - `"CLOSED"` → `MarketState.CLOSED`
  - 기타 → `MarketState.UNKNOWN` (fallback)

#### String → MarketCode enum
- 매핑 규칙:
  - `"us"` → `MarketCode.US`
  - `"kr"` → `MarketCode.KR`
  - `"jp"` → `MarketCode.JP`
  - 기타 → 지원되는 모든 시장 코드 매핑

### 4.4 Nullable 처리 전략

#### 필수 필드 (Non-null)
다음 필드는 API 응답에서 항상 존재해야 하며, 없을 경우 `DataParsingException` 발생:
- `exchange`
- `symbol`
- `shortName` (Market Summary)
- `open`, `close` (Market Time)
- `timezone` (Market Time)

#### 선택적 필드 (Nullable)
다음 필드는 Nullable로 처리:
- 모든 가격 관련 필드 (`regularMarketPrice`, `regularMarketChange` 등)
- `preMarket`, `postMarket` (시장에 따라 제공되지 않을 수 있음)
- `currentTime` (일부 응답에서 누락 가능)

#### Null 처리 로직
- Yahoo API가 필드를 제공하지 않으면 null 유지
- 빈 문자열 `""` → null 변환
- 0 또는 0.0 → 원본 값 유지 (실제 0일 수 있으므로)

---

## 5. 에러 처리

### 5.1 예상되는 에러 케이스

#### HTTP 레벨 에러
| HTTP 상태 코드 | 의미 | UFC ErrorCode | 처리 방법 |
|---------------|------|--------------|----------|
| 400 Bad Request | 잘못된 market 파라미터 | `INVALID_PARAMETER` | market 값 검증 |
| 401 Unauthorized | 인증 실패 (crumb 만료) | `AUTH_FAILED` | crumb 재획득 후 재시도 |
| 404 Not Found | 지원하지 않는 시장 | `DATA_NOT_FOUND` | 사용자에게 에러 메시지 반환 |
| 429 Too Many Requests | Rate limit 초과 | `RATE_LIMITED` | Rate limiter에서 사전 차단 |
| 500 Internal Server Error | Yahoo 서버 오류 | `EXTERNAL_API_ERROR` | 재시도 로직 (exponential backoff) |
| 503 Service Unavailable | Yahoo 서비스 점검 | `EXTERNAL_API_ERROR` | 사용자에게 에러 메시지 반환 |

#### API 응답 레벨 에러
| 에러 상황 | Yahoo 응답 | UFC ErrorCode | 처리 방법 |
|----------|-----------|--------------|----------|
| 에러 객체 존재 | `error: { code: "...", description: "..." }` | `EXTERNAL_API_ERROR` | error.description 포함하여 예외 발생 |
| 결과 없음 | `result: []` | `DATA_NOT_FOUND` | "시장 데이터를 찾을 수 없습니다" 예외 |
| 결과 null | `result: null` | `DATA_NOT_FOUND` | "시장 데이터를 찾을 수 없습니다" 예외 |
| JSON 파싱 실패 | 비정상 JSON | `DATA_PARSING_ERROR` | 원본 응답 일부 포함하여 예외 발생 |

#### 데이터 변환 레벨 에러
| 에러 상황 | 예시 | UFC ErrorCode | 처리 방법 |
|----------|------|--------------|----------|
| ISO 8601 파싱 실패 | "invalid-date" | `DATA_PARSING_ERROR` | "시간 정보 파싱 실패: {field}" 예외 |
| 필수 필드 누락 | exchange 필드 없음 | `DATA_PARSING_ERROR` | "필수 필드 누락: {field}" 예외 |
| Timezone 배열 비어있음 | `timezone: []` | `DATA_PARSING_ERROR` | "타임존 정보 없음" 예외 |
| Unix timestamp 음수 | `regularMarketTime: -1` | 무시 | null로 처리 |

### 5.2 에러 응답 구조

#### Yahoo Finance API 에러 응답 예시

##### Market Summary Error
```json
{
  "marketSummaryResponse": {
    "result": null,
    "error": {
      "code": "Bad Request",
      "description": "Invalid market parameter: xyz"
    }
  }
}
```

##### Market Time Error
```json
{
  "finance": {
    "marketTimes": null,
    "error": {
      "code": "Not Found",
      "description": "Market not found"
    }
  }
}
```

#### UFC ApiException 구조

```kotlin
throw ApiException(
    errorCode = ErrorCode.EXTERNAL_API_ERROR,
    message = "Market Summary API 에러: Invalid market parameter: xyz",
    statusCode = 400,
    metadata = mapOf(
        "market" to "xyz",
        "yahooErrorCode" to "Bad Request"
    )
)
```

### 5.3 에러 처리 우선순위

1. **Rate Limiter 사전 차단**: API 호출 전 Rate Limit 체크
2. **HTTP 상태 코드 확인**: 4xx, 5xx 에러 즉시 처리
3. **API 에러 객체 확인**: Yahoo의 error 객체 존재 여부 체크
4. **결과 유효성 검증**: result가 null이거나 empty인지 확인
5. **데이터 파싱**: JSON 역직렬화 및 타입 변환
6. **필수 필드 검증**: Non-null 필드 존재 확인

---

## 6. 테스트 전략

### 6.1 단위 테스트 시나리오

#### YahooClient.marketSummary() 단위 테스트
- **테스트 위치**: `src/test/kotlin/com/ulalax/ufc/unit/yahoo/`
- **테스트 클래스**: `MarketSummaryUnitTest.kt`

##### 테스트 시나리오
1. **정상 응답 파싱 테스트**
   - Mock JSON 응답을 주고 정상적으로 `MarketSummaryResult`로 변환되는지 확인
   - 모든 필드가 올바르게 매핑되는지 검증

2. **Nullable 필드 처리 테스트**
   - `regularMarketPrice`가 null인 경우 정상 처리 확인
   - `preMarket`, `postMarket` null 처리 확인

3. **타입 변환 테스트**
   - Unix timestamp → Instant 변환 검증
   - ISO 8601 → Instant 변환 검증
   - MarketState 문자열 → enum 변환 검증

4. **에러 응답 처리 테스트**
   - Yahoo error 객체 존재 시 ApiException 발생 확인
   - result가 empty일 때 DATA_NOT_FOUND 예외 발생 확인

5. **잘못된 JSON 처리 테스트**
   - 비정상 JSON 입력 시 DATA_PARSING_ERROR 발생 확인

#### YahooClient.marketTime() 단위 테스트
- **테스트 클래스**: `MarketTimeUnitTest.kt`

##### 테스트 시나리오
1. **정상 응답 파싱 테스트**
   - Mock JSON 응답을 주고 정상적으로 `MarketTimeResult`로 변환되는지 확인

2. **Timezone 파싱 테스트**
   - timezone 배열 첫 번째 요소 정상 추출 확인
   - timezone이 비어있을 때 에러 처리 확인

3. **Pre/Post Market 처리 테스트**
   - preMarket이 null일 때 정상 처리
   - postMarket이 null일 때 정상 처리

4. **MarketState 변환 테스트**
   - "PRE", "REGULAR", "POST", "CLOSED" → enum 변환
   - 알 수 없는 상태 → UNKNOWN 처리

### 6.2 통합 테스트 시나리오

#### Market API Integration Test
- **테스트 위치**: `src/test/kotlin/com/ulalax/ufc/integration/yahoo/`
- **테스트 클래스**: `MarketSpec.kt`
- **테스트 베이스**: `IntegrationTestBase` 상속

##### 기본 동작 테스트 그룹
```
@Nested
@DisplayName("기본 동작")
inner class BasicBehavior {
    @Test
    fun `미국 시장 요약 정보를 조회할 수 있다`()

    @Test
    fun `한국 시장 요약 정보를 조회할 수 있다`()

    @Test
    fun `일본 시장 요약 정보를 조회할 수 있다`()

    @Test
    fun `미국 시장 시간 정보를 조회할 수 있다`()

    @Test
    fun `한국 시장 시간 정보를 조회할 수 있다`()
}
```

##### 응답 데이터 스펙 테스트 그룹
```
@Nested
@DisplayName("응답 데이터 스펙")
inner class ResponseSpec {
    @Test
    fun `Market Summary 응답에는 최소 1개 이상의 지수가 포함된다`()

    @Test
    fun `각 지수에는 symbol, shortName이 포함된다`()

    @Test
    fun `Market Time 응답에는 개장/폐장 시각이 포함된다`()

    @Test
    fun `타임존 정보가 정상적으로 파싱된다`()
}
```

##### 시장별 특성 테스트 그룹
```
@Nested
@DisplayName("시장별 특성")
inner class MarketSpecificBehavior {
    @Test
    fun `미국 시장에는 S&P 500, NASDAQ, DOW 지수가 포함된다`()

    @Test
    fun `한국 시장에는 KOSPI, KOSDAQ 지수가 포함된다`()

    @Test
    fun `미국 시장은 pre/post market 정보를 제공한다`()

    @Test
    fun `한국 시장은 pre/post market 정보가 없다`()
}
```

##### 에러 케이스 테스트 그룹
```
@Nested
@DisplayName("에러 케이스")
inner class ErrorCases {
    @Test
    fun `지원하지 않는 시장 코드 조회 시 ApiException을 던진다`()

    @Test
    fun `네트워크 오류 시 적절한 예외가 발생한다`()
}
```

##### 활용 예제 테스트 그룹
```
@Nested
@DisplayName("활용 예제")
inner class UsageExamples {
    @Test
    fun `여러 시장의 요약 정보를 순차적으로 조회할 수 있다`()

    @Test
    fun `시장 개장 여부를 확인할 수 있다`()

    @Test
    fun `현재 시장 상태(PRE, REGULAR, POST, CLOSED)를 알 수 있다`()
}
```

### 6.3 테스트 더블 전략

#### Mock 데이터 준비
- **위치**: `src/test/resources/yahoo/market/`
- **파일 구조**:
  ```
  market/
  ├── marketSummary_us.json
  ├── marketSummary_kr.json
  ├── marketSummary_jp.json
  ├── marketTime_us.json
  ├── marketTime_kr.json
  ├── marketTime_error.json
  └── marketSummary_empty.json
  ```

#### ResponseRecorder 활용
- 실제 API 응답을 녹화하여 테스트에 활용
- `RecordingConfig.isRecordingEnabled = true` 설정 시 응답 저장
- 저장된 응답은 회귀 테스트에 활용

### 6.4 성능 테스트 시나리오

#### Rate Limiting 검증
```
@Test
fun `Rate Limiter가 정상적으로 동작한다`() {
    // 50개 요청을 순차적으로 보내고
    // Rate Limiter가 초당 50개 제한을 준수하는지 확인
}
```

#### 캐싱 검증 (향후 추가 시)
```
@Test
fun `동일 시장 재조회 시 캐시된 응답을 반환한다`() {
    // 첫 번째 호출 후 두 번째 호출이 캐시에서 반환되는지 확인
}
```

### 6.5 테스트 실행 방법

#### 전체 테스트 실행
```bash
./gradlew test
```

#### Market API 통합 테스트만 실행
```bash
./gradlew test --tests 'MarketSpec'
```

#### 특정 테스트 그룹 실행
```bash
./gradlew test --tests 'MarketSpec$BasicBehavior'
```

#### 단위 테스트만 실행
```bash
./gradlew test --tests '*MarketSummaryUnitTest'
./gradlew test --tests '*MarketTimeUnitTest'
```

---

## 7. 구현 체크리스트

### 7.1 모델 클래스 구현
- [ ] `MarketCode` enum class 생성
- [ ] `MarketState` enum class 생성
- [ ] `MarketSummaryItem` data class 생성
- [ ] `MarketSummaryResult` data class 생성
- [ ] `MarketTimezone` data class 생성
- [ ] `TradingHours` data class 생성
- [ ] `MarketTimeResult` data class 생성
- [ ] Internal response 클래스 생성 (`MarketResponse.kt`)

### 7.2 API 클라이언트 구현
- [ ] `YahooApiUrls`에 상수 추가 (`MARKET_SUMMARY`, `MARKET_TIME`)
- [ ] `YahooClient.marketSummary()` 메서드 구현
- [ ] `YahooClient.marketTime()` 메서드 구현
- [ ] Response → Domain 모델 변환 로직 구현
- [ ] 에러 처리 로직 구현

### 7.3 테스트 구현
- [ ] 단위 테스트 작성 (`MarketSummaryUnitTest`, `MarketTimeUnitTest`)
- [ ] 통합 테스트 작성 (`MarketSpec`)
- [ ] Mock 데이터 준비
- [ ] 에러 케이스 테스트 작성

### 7.4 문서화
- [ ] KDoc 주석 추가 (모든 public 클래스/메서드)
- [ ] README 업데이트 (Market API 사용 예제 추가)
- [ ] 통합 테스트를 API 가이드로 활용 가능하도록 작성

---

## 8. 향후 확장 가능성

### 8.1 캐싱 전략
- Market Summary: TTL 60초 (현재가 데이터)
- Market Time: TTL 3600초 (시장 시간은 자주 변경되지 않음)

### 8.2 고급 기능
- 여러 시장 동시 조회 (`marketSummary(vararg markets: MarketCode)`)
- 시장 개장 여부 헬퍼 메서드 (`isMarketOpen(market: MarketCode): Boolean`)
- 다음 개장 시각 계산 (`getNextOpenTime(market: MarketCode): Instant`)

### 8.3 다른 소스와의 통합
- Market API는 독립적으로 사용 가능하므로 별도 통합 필요 없음
- FRED API와는 관련 없음 (주식 시장 vs 거시경제 지표)

---

## 9. 참고 자료

### 9.1 yfinance 구현 참조
- 파일: `/home/ulalax/project/kairos/yfinance/yfinance/domain/market.py`
- 참고 사항:
  - yfinance는 `_parse_data()`에서 summary와 status를 동시 조회
  - yfinance는 `property` 패턴 사용, UFC는 `suspend fun` 사용
  - yfinance는 summary 결과를 exchange를 키로 하는 딕셔너리로 변환

### 9.2 Yahoo Finance API 문서
- 공식 문서 없음 (비공식 API)
- 리버스 엔지니어링을 통해 파라미터 및 응답 구조 파악

### 9.3 UFC 기존 구현 참조
- `YahooClient.quoteSummary()`: 모듈 기반 조회 패턴
- `YahooClient.chart()`: 간단한 파라미터 기반 조회 패턴
- Market API는 chart() 패턴과 유사 (단순 파라미터)

---

## 10. 주의사항

### 10.1 Yahoo Finance API 제약
- 비공식 API이므로 Yahoo의 정책 변경에 따라 동작이 달라질 수 있음
- Rate Limiting이 엄격하므로 Rate Limiter 설정 준수 필요
- Crumb 토큰이 만료되면 재획득 필요

### 10.2 데이터 품질
- 일부 시장은 데이터가 불완전할 수 있음
- Pre/Post Market 정보는 미국 시장만 제공될 가능성 높음
- 타임존 정보는 항상 제공되지만 정확도는 Yahoo에 의존

### 10.3 에러 복원력
- 네트워크 오류 발생 시 재시도 로직 고려
- Yahoo 서버 오류 시 적절한 fallback 메커니즘 필요
- 사용자에게 명확한 에러 메시지 제공

---

## 문서 버전 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|-----|------|--------|----------|
| 1.0 | 2025-12-05 | Claude | 초기 명세서 작성 |
