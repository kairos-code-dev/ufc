# Chart API 기술 명세서

> **Version**: 1.0
> **작성일**: 2025-12-05

---

## 1. 개요

### 1.1 API 목적

Yahoo Finance Chart API를 통해 **히스토리컬 OHLCV(시가, 고가, 저가, 종가, 거래량) 데이터**를 조회한다.

| 항목 | 값 |
|-----|---|
| 엔드포인트 | `GET https://query2.finance.yahoo.com/v8/finance/chart/{symbol}` |
| HTTP 메서드 | GET |
| 인증 | CRUMB 토큰 필요 |
| 다중 심볼 | 미지원 (심볼당 개별 요청) |

### 1.2 Quote API와의 차이점

| 구분 | Chart API | Quote API |
|------|-----------|-----------|
| 목적 | 히스토리컬 OHLCV 시계열 데이터 | 실시간 시장 데이터 및 기본 정보 |
| 데이터 형태 | 시계열 배열 (타임스탬프별) | 단일 스냅샷 |
| 시간 범위 | 과거 데이터 (1일~최대) | 현재 시점 |
| 이벤트 데이터 | 배당/분할 이력 포함 가능 | 최근 배당 정보만 |
| 조정가 | 조정 종가(adjClose) 제공 | 미제공 |
| 사용 사례 | 차트 그리기, 백테스팅, 기술적 분석 | 실시간 모니터링, 현재가 확인 |

### 1.3 제공 데이터

| 카테고리 | 주요 필드 |
|---------|----------|
| 메타데이터 | symbol, currency, exchange, regularMarketPrice, fiftyTwoWeekHigh/Low |
| OHLCV 데이터 | timestamp, open, high, low, close, adjClose, volume |
| 배당 이벤트 | amount, date (요청 시) |
| 분할 이벤트 | numerator, denominator, splitRatio, date (요청 시) |
| 자본이득 이벤트 | amount, date (요청 시) |

### 1.4 데이터 간격(Interval)과 조회 기간(Period)

| Interval | 사용 가능한 Period | 최대 데이터 범위 |
|----------|-------------------|----------------|
| 1m, 2m, 5m, 15m, 30m, 1h | 1d, 5d, 1mo | 약 60일 |
| 1d, 5d, 1wk, 1mo, 3mo | 모든 Period | 수십 년 |

---

## 2. 데이터 소스 분석

### 2.1 요청 파라미터

| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---------|------|-----|------|------|
| symbol | String | Yes | 조회할 심볼 | "AAPL" |
| interval | String | Yes | 데이터 간격 | "1d", "1h", "5m" |
| period | String | Yes* | 조회 기간 | "1y", "1mo", "max" |
| range | String | Yes* | 조회 기간(period와 동일) | "1y", "1mo" |
| events | String | No | 이벤트 타입 (쉼표 구분) | "div,split" |
| crumb | String | Yes | CRUMB 인증 토큰 | "dFhd8fj..." |

\* period와 range는 동일한 의미이며, 둘 중 하나만 제공하면 됨

### 2.2 응답 구조

#### 정상 응답

| 경로 | 타입 | 설명 |
|-----|------|------|
| chart.result | Array | 차트 결과 배열 (단일 심볼이므로 항상 1개) |
| chart.result[0].meta | Object | 메타데이터 |
| chart.result[0].timestamp | Array&lt;Long&gt; | 타임스탬프 배열 (Unix seconds) |
| chart.result[0].indicators.quote[0] | Object | OHLCV 데이터 배열 |
| chart.result[0].indicators.adjclose[0].adjclose | Array&lt;Double&gt; | 조정 종가 배열 |
| chart.result[0].events | Object? | 이벤트 데이터 (요청 시) |
| chart.error | Object? | 에러 객체 (정상 시 null) |

#### 에러 응답

| 경로 | 타입 | 설명 |
|-----|------|------|
| chart.result | null or [] | 빈 배열 또는 null |
| chart.error.code | String? | 에러 코드 |
| chart.error.description | String? | 에러 설명 |

### 2.3 응답 필드 분류

#### 메타데이터 (ChartMeta)

| Yahoo 필드 | 타입 | Nullable | 설명 |
|-----------|------|----------|------|
| symbol | String | No | 티커 심볼 |
| currency | String | Yes | 통화 코드 (USD, EUR 등) |
| currencySymbol | String | Yes | 통화 기호 ($, € 등) |
| regularMarketPrice | Double | Yes | 현재 시장가 |
| exchange | String | Yes | 거래소 이름 (NASDAQ, NYSE 등) |
| regularMarketDayHigh | Double | Yes | 당일 최고가 |
| regularMarketDayLow | Double | Yes | 당일 최저가 |
| dataGranularity | String | Yes | 데이터 간격 (API에서 반환) |
| range | String | Yes | 조회 기간 (API에서 반환) |
| fiftyTwoWeekHigh | Double | Yes | 52주 최고가 |
| fiftyTwoWeekLow | Double | Yes | 52주 최저가 |
| sharesOutstanding | Long | Yes | 발행주식수 |
| marketCap | Long | Yes | 시가총액 |
| regularMarketVolume | Long | Yes | 당일 거래량 |
| validRanges | List&lt;String&gt; | Yes | 사용 가능한 기간 목록 |

#### OHLCV 데이터 (indicators.quote[0])

| Yahoo 필드 | 타입 | Nullable | 설명 |
|-----------|------|----------|------|
| open | List&lt;Double?&gt; | Yes | 시가 배열 |
| high | List&lt;Double?&gt; | Yes | 고가 배열 |
| low | List&lt;Double?&gt; | Yes | 저가 배열 |
| close | List&lt;Double?&gt; | Yes | 종가 배열 |
| volume | List&lt;Long?&gt; | Yes | 거래량 배열 |

#### 조정 종가 (indicators.adjclose[0])

| Yahoo 필드 | 타입 | Nullable | 설명 |
|-----------|------|----------|------|
| adjclose | List&lt;Double?&gt; | Yes | 조정 종가 배열 (배당/분할 조정됨) |

#### 이벤트 데이터 (events)

**배당 이벤트 (events.dividends)**

| Yahoo 필드 | 타입 | Nullable | 설명 |
|-----------|------|----------|------|
| amount | Double | Yes | 배당금액 |
| date | Long | Yes | 배당일 (Unix timestamp) |

**분할 이벤트 (events.splits)**

| Yahoo 필드 | 타입 | Nullable | 설명 |
|-----------|------|----------|------|
| date | Long | Yes | 분할일 (Unix timestamp) |
| numerator | Double | Yes | 분할 비율 분자 |
| denominator | Double | Yes | 분할 비율 분모 |
| splitRatio | String | Yes | 분할 비율 문자열 (예: "4:1") |

**자본이득 이벤트 (events.capitalGains)**

| Yahoo 필드 | 타입 | Nullable | 설명 |
|-----------|------|----------|------|
| amount | Double | Yes | 자본이득 금액 |
| date | Long | Yes | 발생일 (Unix timestamp) |

### 2.4 자산 유형별 필드 제공 여부

| 필드 그룹 | EQUITY | ETF | INDEX | CRYPTOCURRENCY |
|---------|--------|-----|-------|----------------|
| 메타데이터 | Yes | Yes | Yes | Yes |
| OHLCV 데이터 | Yes | Yes | Yes | Yes |
| 조정 종가 | Yes | Yes | Partial | Partial |
| 배당 이벤트 | Partial | Partial | No | No |
| 분할 이벤트 | Yes | Partial | No | Partial |
| 자본이득 이벤트 | Rare | Yes | No | No |

---

## 3. 데이터/모델 설계

### 3.1 Domain 모델

#### ChartData

최상위 응답 모델로, 요청한 이벤트와 차트 데이터를 포함한다.

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| requestedEvents | Set&lt;ChartEventType&gt; | No | 요청한 이벤트 타입 목록 |
| meta | ChartMeta | No | 메타데이터 |
| prices | List&lt;OHLCV&gt; | No | OHLCV 가격 데이터 목록 |
| events | ChartEvents | Yes | 이벤트 데이터 (요청 시에만 포함) |

**헬퍼 메서드**:
- `hasEvent(eventType: ChartEventType): Boolean` - 특정 이벤트를 요청했는지 확인
- `getDividends(): Map<String, DividendEvent>?` - 배당 이벤트 가져오기
- `getSplits(): Map<String, SplitEvent>?` - 분할 이벤트 가져오기
- `getCapitalGains(): Map<String, CapitalGainEvent>?` - 자본이득 이벤트 가져오기

#### ChartMeta

차트 메타데이터. 심볼, 통화, 현재가 등 기본 정보를 포함한다.

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| symbol | String | Yes | 티커 심볼 |
| currency | String | Yes | 통화 코드 |
| currencySymbol | String | Yes | 통화 기호 |
| regularMarketPrice | Double | Yes | 현재 시장가 |
| exchange | String | Yes | 거래소 이름 |
| regularMarketDayHigh | Double | Yes | 당일 최고가 |
| regularMarketDayLow | Double | Yes | 당일 최저가 |
| dataGranularity | String | Yes | 데이터 간격 |
| range | String | Yes | 조회 기간 |
| fiftyTwoWeekHigh | Double | Yes | 52주 최고가 |
| fiftyTwoWeekLow | Double | Yes | 52주 최저가 |
| sharesOutstanding | Long | Yes | 발행주식수 |
| marketCap | Long | Yes | 시가총액 |
| regularMarketVolume | Long | Yes | 당일 거래량 |
| validRanges | List&lt;String&gt; | Yes | 사용 가능한 기간 목록 |

#### OHLCV

단일 시점의 가격 데이터(봉). Open, High, Low, Close, Volume 정보를 포함한다.

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| timestamp | Long | No | Unix timestamp (초 단위) |
| open | Double | No | 시가 |
| high | Double | No | 고가 |
| low | Double | No | 저가 |
| close | Double | No | 종가 |
| adjClose | Double | Yes | 조정 종가 (배당/분할 조정됨) |
| volume | Long | No | 거래량 |

**헬퍼 메서드**:
- `range(): Double` - 일중 가격 변동폭 (high - low)
- `rangePercent(): Double` - 일중 변동률 (%)
- `change(): Double` - 종가 기준 변동 (close - open)
- `changePercent(): Double` - 종가 기준 변동률 (%)
- `isBullish(): Boolean` - 양봉 여부 (close > open)
- `isBearish(): Boolean` - 음봉 여부 (close < open)

#### ChartEvents

이벤트 데이터 컨테이너. 요청한 이벤트만 포함된다.

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| dividends | Map&lt;String, DividendEvent&gt; | Yes | 배당 이벤트 맵 (타임스탬프 → 이벤트) |
| splits | Map&lt;String, SplitEvent&gt; | Yes | 분할 이벤트 맵 (타임스탬프 → 이벤트) |
| capitalGains | Map&lt;String, CapitalGainEvent&gt; | Yes | 자본이득 이벤트 맵 (타임스탬프 → 이벤트) |

#### DividendEvent

배당금 이벤트.

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| amount | Double | Yes | 배당금액 (주당) |
| date | Long | Yes | 배당일 (Unix timestamp) |

#### SplitEvent

주식 분할 이벤트.

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| date | Long | Yes | 분할일 (Unix timestamp) |
| numerator | Double | Yes | 분할 비율 분자 (예: 4:1에서 4) |
| denominator | Double | Yes | 분할 비율 분모 (예: 4:1에서 1) |
| splitRatio | String | Yes | 분할 비율 문자열 (예: "4:1") |

#### CapitalGainEvent

자본이득 이벤트 (주로 ETF).

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| amount | Double | Yes | 자본이득 금액 |
| date | Long | Yes | 발생일 (Unix timestamp) |

### 3.2 Enum 타입

#### Interval

데이터 조회 간격.

| 값 | API Value | 설명 | 분 단위 |
|----|-----------|------|--------|
| OneMinute | "1m" | 1분봉 | 1 |
| TwoMinutes | "2m" | 2분봉 | 2 |
| FiveMinutes | "5m" | 5분봉 | 5 |
| FifteenMinutes | "15m" | 15분봉 | 15 |
| ThirtyMinutes | "30m" | 30분봉 | 30 |
| OneHour | "1h" | 1시간봉 | 60 |
| OneDay | "1d" | 일봉 | 1440 |
| FiveDays | "5d" | 5일봉 | 7200 |
| OneWeek | "1wk" | 주봉 | 10080 |
| OneMonth | "1mo" | 월봉 | 43200 |
| ThreeMonths | "3mo" | 3개월봉 | 129600 |

**헬퍼 함수**:
- `isIntraday(): Boolean` - 분/시간 단위 간격인지 확인
- `isDailyOrLonger(): Boolean` - 일 단위 이상 간격인지 확인
- `toKoreanString(): String` - 한글 표현 변환

#### Period

데이터 조회 기간.

| 값 | API Value | 설명 |
|----|-----------|------|
| OneDay | "1d" | 1일 |
| FiveDays | "5d" | 5일 |
| OneMonth | "1mo" | 1개월 |
| ThreeMonths | "3mo" | 3개월 |
| SixMonths | "6mo" | 6개월 |
| OneYear | "1y" | 1년 |
| TwoYears | "2y" | 2년 |
| FiveYears | "5y" | 5년 |
| TenYears | "10y" | 10년 |
| YearToDate | "ytd" | 연초부터 현재까지 |
| Max | "max" | 최대 기간 (전체 이력) |

**헬퍼 함수**:
- `toKoreanString(): String` - 한글 표현 변환

#### ChartEventType

차트 이벤트 타입.

| 값 | API Value | 설명 |
|----|-----------|------|
| DIVIDEND | "div" | 배당금 이벤트 |
| SPLIT | "split" | 주식 분할 이벤트 |
| CAPITAL_GAIN | "capitalGain" | 자본이득 이벤트 (주로 ETF) |

### 3.3 Internal Response 모델

#### 구조

| 클래스 | 필드 | 타입 |
|-------|-----|------|
| ChartDataResponse | chart | Chart |
| Chart | result | List&lt;ChartResult&gt;? |
| | error | ChartError? |
| ChartResult | meta | ChartMeta? |
| | timestamp | List&lt;Long&gt;? |
| | indicators | ChartIndicators? |
| | events | ChartEvents? |
| ChartIndicators | quote | List&lt;ChartQuote&gt;? |
| | adjclose | List&lt;ChartAdjClose&gt;? |
| ChartQuote | open, high, low, close | List&lt;Double?&gt;? |
| | volume | List&lt;Long?&gt;? |
| ChartAdjClose | adjclose | List&lt;Double?&gt;? |
| ChartEvents | dividends | Map&lt;String, DividendEvent&gt;? |
| | splits | Map&lt;String, SplitEvent&gt;? |
| | capitalGains | Map&lt;String, CapitalGainEvent&gt;? |
| ChartError | code | String? |
| | description | String? |

### 3.4 API 메서드 시그니처

#### YahooClient (Infrastructure)

```kotlin
suspend fun chart(
    symbol: String,
    interval: Interval,
    period: Period,
    vararg events: ChartEventType = emptyArray()
): ChartData
```

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|-------|------|
| symbol | String | (필수) | 티커 심볼 (예: "AAPL") |
| interval | Interval | (필수) | 데이터 간격 |
| period | Period | (필수) | 조회 기간 |
| events | vararg ChartEventType | emptyArray() | 조회할 이벤트 타입 |

**반환값**: `ChartData` - 차트 데이터 (메타, OHLCV, 이벤트 포함)

### 3.5 필드 매핑

#### Internal Response → Domain 변환

**ChartMeta 매핑**

| Internal | Domain | 변환 규칙 |
|----------|--------|----------|
| meta.symbol | meta.symbol | 그대로 |
| meta.currency | meta.currency | 그대로 |
| meta.currencySymbol | meta.currencySymbol | 그대로 |
| meta.regularMarketPrice | meta.regularMarketPrice | 그대로 |
| meta.exchange | meta.exchange | 그대로 (거래소 코드) |
| meta.fiftyTwoWeekHigh | meta.fiftyTwoWeekHigh | 그대로 |
| meta.fiftyTwoWeekLow | meta.fiftyTwoWeekLow | 그대로 |
| meta.marketCap | meta.marketCap | 그대로 |
| meta.regularMarketVolume | meta.regularMarketVolume | 그대로 |

**OHLCV 매핑**

| Internal | Domain | 변환 규칙 |
|----------|--------|----------|
| timestamp[i] | OHLCV.timestamp | 그대로 (Unix seconds) |
| indicators.quote[0].open[i] | OHLCV.open | null 제거 |
| indicators.quote[0].high[i] | OHLCV.high | null 제거 |
| indicators.quote[0].low[i] | OHLCV.low | null 제거 |
| indicators.quote[0].close[i] | OHLCV.close | null 제거 |
| indicators.adjclose[0].adjclose[i] | OHLCV.adjClose | nullable 유지 |
| indicators.quote[0].volume[i] | OHLCV.volume | null 제거 |

**이벤트 매핑**

| Internal | Domain | 변환 규칙 |
|----------|--------|----------|
| events.dividends | ChartEvents.dividends | Map&lt;String, DividendEvent&gt; 그대로 |
| events.splits | ChartEvents.splits | Map&lt;String, SplitEvent&gt; 그대로 |
| events.capitalGains | ChartEvents.capitalGains | Map&lt;String, CapitalGainEvent&gt; 그대로 |

#### 타입 변환 규칙

| Yahoo 타입 | Domain 타입 | 변환 규칙 |
|-----------|------------|----------|
| Long (timestamp) | Long | 그대로 (Unix seconds) |
| List&lt;Double?&gt; | List&lt;Double&gt; | null 값 제거 후 timestamp와 zip |
| List&lt;Long?&gt; | List&lt;Long&gt; | null 값 제거 후 timestamp와 zip |
| Map&lt;String, Event&gt; | Map&lt;String, Event&gt; | 그대로 |

#### Nullable 처리

| 상황 | 처리 |
|-----|------|
| result = null or [] | DATA_NOT_FOUND 예외 |
| timestamp = null or empty | DATA_PARSING_ERROR 예외 |
| OHLCV 배열 일부 null | 해당 인덱스 제외 (null 값 스킵) |
| 이벤트 데이터 없음 | events = null 또는 해당 필드 null |
| meta 필드 일부 null | nullable 필드는 그대로 null 유지 |

---

## 4. 예외 처리

### 4.1 ErrorCode 매핑

| 에러 케이스 | ErrorCode | HTTP Status | 설명 |
|-----------|-----------|-------------|------|
| 잘못된 심볼 | INVALID_SYMBOL | 200 | chart.error.code = "Not Found" |
| 데이터 없음 | DATA_NOT_FOUND | 200 | chart.result = null or [] |
| 잘못된 interval/period 조합 | INVALID_PARAMETER | 400 | 인트라데이 간격 + 장기 period |
| Rate Limit (429) | RATE_LIMITED | 429 | Too Many Requests |
| JSON 파싱 실패 | DATA_PARSING_ERROR | 200 | 역직렬화 실패 |
| CRUMB 획득 실패 | AUTH_FAILED | 401 | 인증 실패 |
| 네트워크 오류 | NETWORK_ERROR | - | 연결 실패 |
| HTTP 4xx | EXTERNAL_API_ERROR | 4xx | 클라이언트 오류 |
| HTTP 5xx | EXTERNAL_API_ERROR | 5xx | 서버 오류 |

### 4.2 빈 결과 처리

| 상황 | 처리 |
|-----|------|
| chart.result = null | DATA_NOT_FOUND 예외 |
| chart.result = [] | DATA_NOT_FOUND 예외 |
| chart.error.code != null | INVALID_SYMBOL 또는 EXTERNAL_API_ERROR 예외 |
| timestamp = null or empty | DATA_PARSING_ERROR 예외 |
| OHLCV 모든 값 null | DATA_PARSING_ERROR 예외 |
| 이벤트 데이터 없음 | 정상 (events = null) |

### 4.3 재시도 정책

| 에러 | 재시도 | 횟수 | 대기 시간 |
|-----|-------|-----|----------|
| Rate Limit (429) | Yes | 무제한 | Rate Limiter 처리 |
| Network Error | Yes | 3회 | Exponential backoff |
| CRUMB 실패 | Yes | 3회 | 즉시 |
| HTTP 5xx | Yes | 3회 | Exponential backoff |
| HTTP 4xx | No | - | - |
| 파싱 오류 | No | - | - |
| 잘못된 파라미터 | No | - | - |

### 4.4 데이터 검증

| 검증 항목 | 규칙 | 실패 시 처리 |
|---------|------|------------|
| timestamp 배열 크기 | > 0 | DATA_PARSING_ERROR |
| OHLCV 배열 크기 일치 | 모두 동일 | 짧은 배열 기준으로 자름 |
| OHLCV null 값 | 전체가 null 아님 | null 값 제외 |
| high >= low | 항상 true | 검증하지 않음 (API 신뢰) |
| volume >= 0 | 항상 true | 검증하지 않음 (API 신뢰) |

---

## 5. 참고 자료

### 5.1 API 제약사항

| 제약 | 설명 |
|-----|------|
| 비공식 API | 문서화되지 않음, 변경 가능 |
| 인트라데이 데이터 제한 | 분/시간 단위는 최대 60일 |
| 다중 심볼 미지원 | 심볼당 개별 요청 필요 |
| CRUMB 필수 | 인증 토큰 필요 |
| Interval/Period 조합 제약 | 일부 조합 불가 (예: 1m + 10y) |
| 장중 데이터 불완전 | 거래 중에는 마지막 봉이 미완성 상태 |

### 5.2 Interval과 Period 조합 가이드

#### 인트라데이 간격 (1m ~ 1h)

| Interval | 권장 Period | 최대 Period | 비고 |
|----------|------------|------------|-----|
| 1m, 2m, 5m | 1d, 5d | 5d | 단기 분석용 |
| 15m, 30m | 1d, 5d, 1mo | 1mo | 중기 분석용 |
| 1h | 5d, 1mo | 1mo | 시간대 분석용 |

#### 일간 이상 간격 (1d ~ 3mo)

| Interval | 권장 Period | 최대 Period | 비고 |
|----------|------------|------------|-----|
| 1d | 1mo, 3mo, 1y, 5y | max | 가장 일반적 |
| 5d | 3mo, 6mo, 1y | max | 주간 요약 |
| 1wk | 1y, 2y, 5y | max | 주봉 차트 |
| 1mo | 5y, 10y, max | max | 월봉 차트 |
| 3mo | 10y, max | max | 분기 차트 |

### 5.3 캐싱 전략

| API | TTL | 이유 |
|-----|-----|------|
| chart (인트라데이) | 60초 | 장중 데이터는 자주 변경됨 |
| chart (일봉, 장중) | 5분 | 당일 봉은 변경 가능 |
| chart (일봉, 장외) | 1시간 | 과거 데이터는 변경 없음 |
| chart (주봉 이상) | 1일 | 장기 데이터는 거의 변경 없음 |

### 5.4 거래일/비거래일 동작

| 상황 | 동작 | 예시 |
|-----|------|------|
| 거래일 (장중) | 당일 봉 포함, 미완성 | 2025-12-05 14:30 → 당일 봉의 OHLCV가 실시간 업데이트 중 |
| 거래일 (장외) | 당일 봉 포함, 완성 | 2025-12-05 20:00 → 당일 봉 완성됨 |
| 휴장일 (주말/공휴일) | 당일 봉 없음 | 2025-12-07 (일) → 마지막 거래일(12-06 금)까지만 |
| 장 시작 전 | 전일 봉까지만 | 2025-12-08 08:00 → 전일(12-05 금)까지만 |

### 5.5 사용 사례별 권장 설정

#### 차트 그리기

| 차트 종류 | Interval | Period | Events |
|----------|----------|--------|--------|
| 일중 차트 | 5m, 15m | 1d | - |
| 일봉 차트 | 1d | 1mo, 3mo, 1y | DIV, SPLIT |
| 주봉 차트 | 1wk | 1y, 2y, 5y | DIV, SPLIT |
| 월봉 차트 | 1mo | 5y, 10y, max | DIV, SPLIT |

#### 백테스팅

| 목적 | Interval | Period | Events |
|-----|----------|--------|--------|
| 단기 전략 | 1d | 1y, 2y | DIV, SPLIT |
| 장기 전략 | 1d, 1wk | 5y, 10y, max | DIV, SPLIT |
| 분봉 전략 | 1m, 5m, 15m | 5d, 1mo | - |

#### 기술적 분석

| 지표 | Interval | Period | 비고 |
|-----|----------|--------|-----|
| 이동평균 (MA) | 1d | 1y, 2y | 50일, 200일 MA 계산 |
| RSI, MACD | 1d | 6mo, 1y | 일반적인 설정 |
| 볼린저밴드 | 1d | 3mo, 6mo | 20일 기준 |
| 지지/저항선 | 1d | 1y, 2y | 과거 고점/저점 확인 |

### 5.6 용어

| 용어 | 설명 |
|-----|------|
| OHLCV | Open, High, Low, Close, Volume (시가, 고가, 저가, 종가, 거래량) |
| 조정 종가 (Adjusted Close) | 배당금 지급, 주식 분할 등을 고려하여 조정된 종가 |
| 인트라데이 (Intraday) | 일중 데이터 (분봉, 시간봉) |
| 봉 (Candle) | 특정 기간의 OHLCV 데이터를 나타내는 단위 |
| 양봉 (Bullish) | 종가가 시가보다 높은 봉 (상승) |
| 음봉 (Bearish) | 종가가 시가보다 낮은 봉 (하락) |
| 주식 분할 (Stock Split) | 주식을 더 작은 단위로 나누는 것 (예: 1주 → 4주) |
| 배당락일 (Ex-Dividend Date) | 배당을 받을 권리가 소멸되는 날 |
| 시가총액 (Market Cap) | 발행주식수 × 현재가 |
| YTD (Year-To-Date) | 연초부터 현재까지 |

### 5.7 참고 링크

- [Yahoo Finance 비공식 API 가이드](https://algotrading101.com/learn/yahoo-finance-api-guide/)
- [OHLCV 차트 분석 기초](https://www.investopedia.com/terms/o/ohlcchart.asp)
- [주식 분할의 이해](https://www.investopedia.com/terms/s/stocksplit.asp)
- [조정 종가 계산 방법](https://www.investopedia.com/terms/a/adjusted_closing_price.asp)
