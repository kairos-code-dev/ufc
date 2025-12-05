# Market API 기술 명세서

> **Version**: 2.0
> **작성일**: 2025-12-05

---

## 1. 개요

### 1.1 API 목적

Yahoo Finance Market API를 통해 **시장 요약 정보(Market Summary)**와 **시장 시간 정보(Market Time)**를 조회한다.

| API | 엔드포인트 | HTTP 메서드 | 인증 |
|-----|-----------|------------|------|
| Market Summary | `GET https://query1.finance.yahoo.com/v6/finance/quote/marketSummary` | GET | 불필요 |
| Market Time | `GET https://query1.finance.yahoo.com/v6/finance/markettime` | GET | 불필요 |

### 1.2 제공 데이터

#### Market Summary

| 데이터 | 설명 |
|-------|------|
| 주요 지수 목록 | 특정 시장의 대표 지수 (S&P 500, KOSPI 등) |
| 현재가 | 지수의 실시간 가격 |
| 변동폭 | 전일 대비 변동폭 및 변동률 |
| 거래량 | 당일 거래량 |
| 시장 상태 | PRE, REGULAR, POST, CLOSED |
| 타임존 정보 | 시장의 타임존 및 GMT 오프셋 |

#### Market Time

| 데이터 | 설명 |
|-------|------|
| 개장/폐장 시각 | 정규 거래 시간 |
| Pre-Market | 프리마켓 거래 시간 (미국만) |
| Post-Market | 애프터마켓 거래 시간 (미국만) |
| 시장 상태 | PRE, REGULAR, POST, CLOSED |
| 타임존 정보 | IANA 이름, 약어, GMT 오프셋 |
| 현재 시각 | 해당 시장의 현재 시각 |

### 1.3 지원 시장

| Market 코드 | 설명 | 주요 지수 |
|------------|------|---------|
| us | 미국 시장 | ^GSPC (S&P 500), ^IXIC (NASDAQ), ^DJI (DOW) |
| kr | 한국 시장 | ^KS11 (KOSPI), ^KQ11 (KOSDAQ) |
| jp | 일본 시장 | ^N225 (NIKKEI 225) |
| gb | 영국 시장 | ^FTSE (FTSE 100) |
| de | 독일 시장 | ^GDAXI (DAX) |
| hk | 홍콩 시장 | ^HSI (HANG SENG) |
| cn | 중국 시장 | 000001.SS (SSE Composite) |
| fr | 프랑스 시장 | ^FCHI (CAC 40) |

---

## 2. 데이터 소스 분석

### 2.1 Market Summary API

#### 요청 파라미터

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|---------|------|-----|------|--------|
| market | String | Yes | 조회할 시장 코드 | - |
| fields | String | No | 반환받을 필드 목록 (쉼표 구분) | 전체 |
| formatted | Boolean | No | 포맷팅된 문자열 포함 | false |
| lang | String | No | 언어 코드 | en-US |

#### 응답 필드

| 필드 | 타입 | Nullable | 설명 |
|-----|------|---------|------|
| exchange | String | No | 거래소 코드 (NMS, NYQ, KSC) |
| symbol | String | No | 지수 심볼 (^GSPC, ^KS11) |
| shortName | String | No | 짧은 이름 (S&P 500, KOSPI) |
| regularMarketPrice | Double | Yes | 현재가 |
| regularMarketChange | Double | Yes | 전일 대비 변동폭 |
| regularMarketChangePercent | Double | Yes | 전일 대비 변동률 (%) |
| regularMarketTime | Long | Yes | 마지막 업데이트 (Unix timestamp) |
| regularMarketDayHigh | Double | Yes | 당일 최고가 |
| regularMarketDayLow | Double | Yes | 당일 최저가 |
| regularMarketVolume | Long | Yes | 거래량 |
| regularMarketPreviousClose | Double | Yes | 전일 종가 |
| currency | String | Yes | 통화 코드 (USD, KRW, JPY) |
| marketState | String | Yes | 시장 상태 |
| quoteType | String | Yes | 자산 타입 (INDEX, EQUITY) |
| exchangeTimezoneName | String | Yes | 타임존 IANA 이름 |
| exchangeTimezoneShortName | String | Yes | 타임존 약어 (EST, KST) |
| gmtOffSetMilliseconds | Long | Yes | GMT 오프셋 (밀리초) |

#### MarketState 값

| 값 | 설명 |
|----|------|
| PRE | 프리마켓 (개장 전) |
| REGULAR | 정규 거래 시간 |
| POST | 애프터마켓 (폐장 후) |
| CLOSED | 휴장 |

### 2.2 Market Time API

#### 요청 파라미터

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|---------|------|-----|------|--------|
| market | String | Yes | 조회할 시장 코드 | - |
| formatted | Boolean | No | 포맷팅된 문자열 포함 | true |
| lang | String | No | 언어 코드 | en-US |
| key | String | No | API 키 | finance |

#### 응답 필드

| 필드 | 타입 | Nullable | 설명 |
|-----|------|---------|------|
| exchange | String | No | 거래소 코드 |
| market | String | No | 시장 식별자 |
| marketState | String | No | 현재 시장 상태 |
| open | String (ISO 8601) | No | 정규 장 개장 시각 |
| close | String (ISO 8601) | No | 정규 장 폐장 시각 |
| preMarket.start | String (ISO 8601) | Yes | 프리마켓 시작 시각 |
| preMarket.end | String (ISO 8601) | Yes | 프리마켓 종료 시각 |
| postMarket.start | String (ISO 8601) | Yes | 애프터마켓 시작 시각 |
| postMarket.end | String (ISO 8601) | Yes | 애프터마켓 종료 시각 |
| timezone[0].short | String | No | 타임존 약어 |
| timezone[0].name | String | No | 타임존 IANA 이름 |
| timezone[0].gmtoffset | Long | No | GMT 오프셋 (밀리초) |
| time | String (ISO 8601) | Yes | 현재 시각 |

---

## 3. 데이터/모델 설계

### 3.1 Domain 모델

#### MarketCode

```kotlin
enum class MarketCode(val code: String, val description: String) {
    US("us", "United States"),
    KR("kr", "South Korea"),
    JP("jp", "Japan"),
    GB("gb", "United Kingdom"),
    DE("de", "Germany"),
    HK("hk", "Hong Kong"),
    CN("cn", "China"),
    FR("fr", "France")
}
```

#### MarketState

```kotlin
enum class MarketState(val value: String) {
    PRE("PRE"),
    REGULAR("REGULAR"),
    POST("POST"),
    CLOSED("CLOSED"),
    UNKNOWN("UNKNOWN")
}
```

#### MarketSummaryItem

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| exchange | String | No | 거래소 코드 |
| symbol | String | No | 지수 심볼 |
| shortName | String | No | 짧은 이름 |
| regularMarketPrice | Double | Yes | 현재가 |
| regularMarketChange | Double | Yes | 전일 대비 변동폭 |
| regularMarketChangePercent | Double | Yes | 전일 대비 변동률 (%) |
| regularMarketTime | Instant | Yes | 마지막 업데이트 시각 |
| regularMarketDayHigh | Double | Yes | 당일 최고가 |
| regularMarketDayLow | Double | Yes | 당일 최저가 |
| regularMarketVolume | Long | Yes | 거래량 |
| regularMarketPreviousClose | Double | Yes | 전일 종가 |
| currency | String | Yes | 통화 코드 |
| marketState | MarketState | Yes | 시장 상태 |
| quoteType | String | Yes | 자산 타입 |
| timezoneName | String | Yes | 타임존 IANA 이름 |
| timezoneShortName | String | Yes | 타임존 약어 |
| gmtOffsetMillis | Long | Yes | GMT 오프셋 (밀리초) |

#### MarketSummaryResult

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| market | MarketCode | No | 조회한 시장 |
| items | List&lt;MarketSummaryItem&gt; | No | 지수 목록 |

#### MarketTimezone

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| shortName | String | No | 타임존 약어 (EST, KST) |
| ianaName | String | No | 타임존 IANA 이름 |
| gmtOffsetMillis | Long | No | GMT 오프셋 (밀리초) |

#### TradingHours

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| start | Instant | No | 시작 시각 |
| end | Instant | No | 종료 시각 |

#### MarketTimeResult

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| market | MarketCode | No | 조회한 시장 |
| exchange | String | No | 거래소 코드 |
| marketIdentifier | String | No | 시장 식별자 |
| marketState | MarketState | No | 현재 시장 상태 |
| open | Instant | No | 정규 장 개장 시각 |
| close | Instant | No | 정규 장 폐장 시각 |
| preMarket | TradingHours | Yes | 프리마켓 거래 시간 |
| postMarket | TradingHours | Yes | 애프터마켓 거래 시간 |
| timezone | MarketTimezone | No | 타임존 정보 |
| currentTime | Instant | Yes | 현재 시각 |

### 3.2 Internal Request 모델

Market Summary와 Market Time API는 GET 요청이므로 별도 Request 모델 불필요.

### 3.3 Internal Response 모델

#### Market Summary 구조

| 클래스 | 필드 | 타입 |
|-------|-----|------|
| MarketSummaryResponse | marketSummaryResponse | MarketSummaryWrapper |
| MarketSummaryWrapper | result | List&lt;MarketSummaryItemResponse&gt;? |
| | error | ErrorResponse? |
| MarketSummaryItemResponse | exchange | String |
| | symbol | String |
| | shortName | String |
| | regularMarketPrice | Double? |
| | regularMarketChange | Double? |
| | 기타 필드... | - |
| ErrorResponse | code | String? |
| | description | String? |

#### Market Time 구조

| 클래스 | 필드 | 타입 |
|-------|-----|------|
| MarketTimeResponse | finance | FinanceWrapper |
| FinanceWrapper | marketTimes | List&lt;MarketTimeWrapper&gt;? |
| | error | ErrorResponse? |
| MarketTimeWrapper | marketTime | List&lt;MarketTimeItemResponse&gt; |
| MarketTimeItemResponse | exchange | String |
| | market | String |
| | marketState | String |
| | open | String |
| | close | String |
| | preMarket | PrePostMarketResponse? |
| | postMarket | PrePostMarketResponse? |
| | timezone | List&lt;TimezoneResponse&gt; |
| | time | String? |
| PrePostMarketResponse | start | String |
| | end | String |
| TimezoneResponse | short | String |
| | name | String |
| | gmtoffset | Long |

### 3.4 API 메서드 시그니처

#### marketSummary

```kotlin
suspend fun marketSummary(market: MarketCode): MarketSummaryResult
```

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| market | MarketCode | 조회할 시장 코드 |

| 반환 | 설명 |
|-----|------|
| MarketSummaryResult | 시장 요약 정보 |

#### marketTime

```kotlin
suspend fun marketTime(market: MarketCode): MarketTimeResult
```

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| market | MarketCode | 조회할 시장 코드 |

| 반환 | 설명 |
|-----|------|
| MarketTimeResult | 시장 시간 정보 |

### 3.5 필드 매핑

#### Market Summary 매핑

| Yahoo 필드 | Domain 필드 | 변환 |
|-----------|------------|------|
| exchange | exchange | 그대로 |
| symbol | symbol | 그대로 |
| shortName | shortName | 그대로 |
| regularMarketPrice | regularMarketPrice | Double? |
| regularMarketChange | regularMarketChange | Double? |
| regularMarketChangePercent | regularMarketChangePercent | Double? |
| regularMarketTime | regularMarketTime | Long? → Instant? |
| regularMarketDayHigh | regularMarketDayHigh | Double? |
| regularMarketDayLow | regularMarketDayLow | Double? |
| regularMarketVolume | regularMarketVolume | Long? |
| regularMarketPreviousClose | regularMarketPreviousClose | Double? |
| currency | currency | String? |
| marketState | marketState | String → MarketState |
| quoteType | quoteType | String? |
| exchangeTimezoneName | timezoneName | String? |
| exchangeTimezoneShortName | timezoneShortName | String? |
| gmtOffSetMilliseconds | gmtOffsetMillis | Long? |

#### Market Time 매핑

| Yahoo 필드 | Domain 필드 | 변환 |
|-----------|------------|------|
| exchange | exchange | 그대로 |
| market | marketIdentifier | 그대로 |
| marketState | marketState | String → MarketState |
| open | open | ISO 8601 → Instant |
| close | close | ISO 8601 → Instant |
| preMarket.start | preMarket.start | ISO 8601 → Instant? |
| preMarket.end | preMarket.end | ISO 8601 → Instant? |
| postMarket.start | postMarket.start | ISO 8601 → Instant? |
| postMarket.end | postMarket.end | ISO 8601 → Instant? |
| timezone[0].short | timezone.shortName | 그대로 |
| timezone[0].name | timezone.ianaName | 그대로 |
| timezone[0].gmtoffset | timezone.gmtOffsetMillis | Long |
| time | currentTime | ISO 8601 → Instant? |

### 3.6 타입 변환 규칙

| 변환 | 방법 | 예외 처리 |
|-----|------|----------|
| Unix timestamp → Instant | `Instant.ofEpochSecond(timestamp)` | 음수는 null 처리 |
| ISO 8601 → Instant | `Instant.parse(isoString)` | 파싱 실패 시 DATA_PARSING_ERROR |
| String → MarketState | enum 매핑 (PRE, REGULAR, POST, CLOSED) | 알 수 없는 값은 UNKNOWN |
| String → MarketCode | enum 매핑 (us, kr, jp 등) | 알 수 없는 값은 예외 |

---

## 4. 예외 처리

### 4.1 ErrorCode 매핑

#### HTTP 레벨 에러

| 상황 | HTTP 코드 | ErrorCode | 설명 |
|-----|----------|-----------|------|
| 잘못된 market 파라미터 | 400 | INVALID_PARAMETER | market 값 검증 실패 |
| 인증 실패 | 401 | AUTH_FAILED | crumb 재획득 필요 |
| 지원하지 않는 시장 | 404 | DATA_NOT_FOUND | 시장 데이터 없음 |
| Rate limit 초과 | 429 | RATE_LIMITED | 요청 제한 |
| Yahoo 서버 오류 | 500 | EXTERNAL_API_ERROR | 재시도 필요 |
| 서비스 점검 | 503 | EXTERNAL_API_ERROR | 일시적 오류 |

#### API 응답 레벨 에러

| 상황 | Yahoo 응답 | ErrorCode | 처리 |
|-----|-----------|-----------|------|
| 에러 객체 존재 | error != null | EXTERNAL_API_ERROR | error.description 포함 |
| 결과 없음 | result = [] | DATA_NOT_FOUND | 빈 결과 예외 |
| 결과 null | result = null | DATA_NOT_FOUND | 빈 결과 예외 |
| JSON 파싱 실패 | 비정상 JSON | DATA_PARSING_ERROR | 원본 응답 포함 |

#### 데이터 변환 레벨 에러

| 상황 | ErrorCode | 메시지 |
|-----|-----------|--------|
| ISO 8601 파싱 실패 | DATA_PARSING_ERROR | "시간 정보 파싱 실패: {field}" |
| 필수 필드 누락 | DATA_PARSING_ERROR | "필수 필드 누락: {field}" |
| Timezone 배열 비어있음 | DATA_PARSING_ERROR | "타임존 정보 없음" |
| Unix timestamp 음수 | - | null로 처리 (예외 아님) |

### 4.2 필수 필드 검증

#### Market Summary

| 필드 | 처리 |
|-----|------|
| exchange | 누락 시 DATA_PARSING_ERROR |
| symbol | 누락 시 DATA_PARSING_ERROR |
| shortName | 누락 시 DATA_PARSING_ERROR |
| 기타 필드 | Nullable로 처리 |

#### Market Time

| 필드 | 처리 |
|-----|------|
| exchange | 누락 시 DATA_PARSING_ERROR |
| market | 누락 시 DATA_PARSING_ERROR |
| open | 누락 시 DATA_PARSING_ERROR |
| close | 누락 시 DATA_PARSING_ERROR |
| timezone | 누락 또는 빈 배열 시 DATA_PARSING_ERROR |
| preMarket | Nullable로 처리 |
| postMarket | Nullable로 처리 |
| time | Nullable로 처리 |

### 4.3 빈 결과 처리

| 상황 | 처리 |
|-----|------|
| result = [] | DATA_NOT_FOUND 예외 발생 |
| items.isEmpty() | DATA_NOT_FOUND 예외 발생 |
| 존재하지 않는 market 코드 | DATA_NOT_FOUND 예외 발생 |

### 4.4 재시도 정책

| 에러 | 재시도 | 횟수 |
|-----|-------|-----|
| Rate Limit (429) | Yes | Rate Limiter 처리 |
| Network Error | Yes | 3회 |
| HTTP 5xx | Yes | 3회 |
| HTTP 4xx | No | - |
| JSON 파싱 실패 | No | - |

---

## 5. 참고 자료

### 5.1 API 제약사항

| 제약 | 설명 |
|-----|------|
| 비공식 API | 문서화되지 않음, 변경 가능 |
| GET 전용 | POST 불가 |
| Rate Limiting | 엄격한 요청 제한 |
| Crumb 필요 | 인증 토큰 필요 (자동 처리) |

### 5.2 데이터 품질

| 항목 | 설명 |
|-----|------|
| 실시간성 | 15분 지연 가능 (sourceInterval 참고) |
| Pre/Post Market | 미국 시장만 제공 |
| 타임존 정보 | 항상 제공되나 Yahoo에 의존 |
| 불완전한 데이터 | 일부 시장은 데이터 누락 가능 |

### 5.3 용어

| 용어 | 설명 |
|-----|------|
| Pre-Market | 정규 장 개장 전 거래 시간 |
| Post-Market | 정규 장 폐장 후 거래 시간 |
| Regular Market | 정규 거래 시간 |
| GMT Offset | 그리니치 평균시와의 시차 (밀리초) |
| IANA Timezone | 표준 타임존 데이터베이스 이름 |
