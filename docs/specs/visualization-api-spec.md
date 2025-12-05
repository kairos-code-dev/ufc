# Visualization API 기능 명세서

> **Version**: 1.0
> **작성일**: 2025-12-05
> **대상**: UFC 프로젝트에 Yahoo Finance `/v1/finance/visualization` API 통합

---

## 목차

- [1. API 개요](#1-api-개요)
- [2. Yahoo Finance Visualization API 분석](#2-yahoo-finance-visualization-api-분석)
- [3. UFC 통합 설계](#3-ufc-통합-설계)
- [4. 데이터 매핑](#4-데이터-매핑)
- [5. 에러 처리](#5-에러-처리)
- [6. 테스트 전략](#6-테스트-전략)
- [7. 구현 우선순위](#7-구현-우선순위)

---

## 1. API 개요

### 1.1 Visualization API의 목적

Visualization API는 Yahoo Finance의 `/v1/finance/visualization` 엔드포인트를 통해 **실적 발표 일정(Earnings Dates)**을 조회하기 위한 전용 API입니다.

이 API는 다음 정보를 제공합니다:
- 과거 및 미래 실적 발표 일정
- EPS 추정치 (Estimate)
- EPS 실제값 (Actual)
- 서프라이즈 비율 (Surprise %)
- 이벤트 타입 (Call, Earnings Report, Meeting)

### 1.2 주요 사용 사례

1. **실적 발표 캘린더**: 투자자가 다가오는 실적 발표를 추적
2. **과거 실적 분석**: 지난 분기/연도의 EPS 추정치와 실제값 비교
3. **서프라이즈 분석**: 실적 발표가 예상을 얼마나 초과/미달했는지 분석
4. **이벤트 기반 트레이딩**: 실적 발표 전후 매매 전략 수립

### 1.3 Chart API 및 QuoteSummary와의 차이점

| 구분 | Visualization API | Chart API | QuoteSummary API |
|------|------------------|-----------|------------------|
| **목적** | 실적 발표 일정 조회 | 가격 히스토리 및 이벤트 | 상세 금융 정보 |
| **HTTP 메서드** | POST | GET | GET |
| **데이터 구조** | 테이블 형식 (rows/columns) | 시계열 배열 | 모듈별 JSON |
| **시간 범위** | 과거/미래 실적 일정 | 과거 가격 히스토리 | 현재 상태 스냅샷 |
| **필터링** | entityIdType, query 지원 | period, interval | modules |
| **페이지네이션** | size 파라미터 (최대 100) | 없음 (전체 기간) | 없음 |
| **정렬** | sortField, sortType | timestamp 고정 | 정렬 없음 |

### 1.4 Visualization API의 특징

**장점**:
- 실적 발표 일정 전용 엔드포인트로 최적화
- 과거와 미래 데이터를 한 번에 조회 가능
- 정렬 및 필터링 기능 제공 (DESC/ASC)
- 최대 100개 데이터 조회 가능

**제약사항**:
- POST 요청만 지원 (Body에 JSON 전송)
- 복잡한 쿼리 구조 (operator, operands)
- 응답 구조가 테이블 형식 (rows/columns)으로 파싱이 복잡
- 비공식 API (문서화되지 않음)

---

## 2. Yahoo Finance Visualization API 분석

### 2.1 API 엔드포인트

```
POST https://query1.finance.yahoo.com/v1/finance/visualization
```

**베이스 URL**: `YahooApiUrls.QUERY1` (`https://query1.finance.yahoo.com`)

### 2.2 요청 구조

#### 2.2.1 HTTP 헤더

| 헤더 | 값 | 필수 | 설명 |
|------|---|------|------|
| `Content-Type` | `application/json` | Yes | JSON Body 전송 |
| `User-Agent` | `Mozilla/5.0 ...` | Yes | Yahoo Finance 요구사항 |
| `Accept` | `application/json` | Yes | - |

#### 2.2.2 URL 파라미터

| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---------|------|------|------|------|
| `lang` | String | No | 언어 설정 | `"en-US"` |
| `region` | String | No | 지역 설정 | `"US"` |

#### 2.2.3 요청 Body (JSON)

```json
{
  "size": 12,
  "query": {
    "operator": "eq",
    "operands": ["ticker", "AAPL"]
  },
  "sortField": "startdatetime",
  "sortType": "DESC",
  "entityIdType": "earnings",
  "includeFields": [
    "startdatetime",
    "timeZoneShortName",
    "epsestimate",
    "epsactual",
    "epssurprisepct",
    "eventtype"
  ]
}
```

**필드 설명**:

| 필드 | 타입 | 필수 | 설명 | 가능한 값 |
|------|------|------|------|----------|
| `size` | Int | Yes | 조회할 데이터 개수 (최대 100) | 1-100 |
| `query` | Object | Yes | 필터링 조건 | - |
| `query.operator` | String | Yes | 조건 연산자 | `"eq"` (equals) |
| `query.operands` | Array | Yes | [필드명, 값] | `["ticker", "AAPL"]` |
| `sortField` | String | Yes | 정렬 기준 필드 | `"startdatetime"` |
| `sortType` | String | Yes | 정렬 방향 | `"DESC"`, `"ASC"` |
| `entityIdType` | String | Yes | 엔티티 타입 | `"earnings"` (고정값) |
| `includeFields` | Array | Yes | 응답에 포함할 필드 목록 | 아래 참조 |

**includeFields 가능한 값**:
- `startdatetime`: 실적 발표 시작 시간
- `timeZoneShortName`: 시간대 약어 (예: "EST")
- `epsestimate`: EPS 추정치
- `epsactual`: EPS 실제값
- `epssurprisepct`: 서프라이즈 비율 (%)
- `eventtype`: 이벤트 타입 (1=Call, 2=Earnings, 11=Meeting)

### 2.3 응답 JSON 구조

#### 2.3.1 전체 응답 구조

```json
{
  "finance": {
    "result": [
      {
        "documents": [
          {
            "columns": [
              {"label": "Event Start Date", "id": "startdatetime"},
              {"label": "Timezone short name", "id": "timeZoneShortName"},
              {"label": "EPS Estimate", "id": "epsestimate"},
              {"label": "Reported EPS", "id": "epsactual"},
              {"label": "Surprise (%)", "id": "epssurprisepct"},
              {"label": "Event Type", "id": "eventtype"}
            ],
            "rows": [
              ["2024-02-01T12:00:00", "EST", 2.10, 2.18, 3.81, 2],
              ["2023-11-02T12:00:00", "EDT", 1.39, 1.46, 5.04, 2]
            ]
          }
        ]
      }
    ],
    "error": null
  }
}
```

#### 2.3.2 응답 구조 분석

**계층 구조**:
```
finance
 └─ result (Array)
     └─ documents (Array)
         ├─ columns (Array<Column>)
         │   ├─ label: 컬럼 표시 이름
         │   └─ id: 컬럼 식별자
         └─ rows (Array<Array<Any>>)
             └─ 각 row는 columns 순서대로 값 배열
```

**특징**:
- 테이블 형식: columns는 메타데이터, rows는 실제 데이터
- rows의 각 요소는 columns 순서와 1:1 매핑
- 타입 혼합: rows의 값은 String, Double, Int 등 혼합

#### 2.3.3 에러 응답 구조

```json
{
  "finance": {
    "result": [],
    "error": {
      "code": "Bad Request",
      "description": "Invalid query"
    }
  }
}
```

### 2.4 응답 필드 상세

#### 2.4.1 columns 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| `label` | String | 사용자에게 표시할 컬럼 이름 |
| `id` | String | 컬럼 식별자 (includeFields와 매칭) |

**예시**:
```json
{
  "label": "Event Start Date",
  "id": "startdatetime"
}
```

#### 2.4.2 rows 필드

rows는 2차원 배열로, 각 row는 다음 순서로 구성:

| 인덱스 | 필드명 | 타입 | 설명 | 예시 |
|-------|--------|------|------|------|
| 0 | Event Start Date | String | ISO 8601 날짜/시간 | `"2024-02-01T12:00:00"` |
| 1 | Timezone short name | String | 시간대 약어 | `"EST"` |
| 2 | EPS Estimate | Double | EPS 추정치 | `2.10` |
| 3 | Reported EPS | Double | 실제 EPS | `2.18` |
| 4 | Surprise (%) | Double | 서프라이즈 비율 | `3.81` |
| 5 | Event Type | Int | 이벤트 타입 코드 | `2` |

**Event Type 코드 매핑**:
- `1`: Call (Earnings Call, 전화 회의)
- `2`: Earnings (Earnings Report, 실적 발표)
- `11`: Meeting (Stockholders Meeting, 주주총회)

### 2.5 데이터 예시

#### 2.5.1 정상 응답 (AAPL, 12개 데이터)

```json
{
  "finance": {
    "result": [
      {
        "documents": [
          {
            "columns": [
              {"label": "Event Start Date", "id": "startdatetime"},
              {"label": "Timezone short name", "id": "timeZoneShortName"},
              {"label": "EPS Estimate", "id": "epsestimate"},
              {"label": "Reported EPS", "id": "epsactual"},
              {"label": "Surprise (%)", "id": "epssurprisepct"},
              {"label": "Event Type", "id": "eventtype"}
            ],
            "rows": [
              ["2024-05-02T16:30:00", "EDT", 1.50, null, null, 2],
              ["2024-02-01T16:30:00", "EST", 2.10, 2.18, 3.81, 2],
              ["2023-11-02T16:30:00", "EDT", 1.39, 1.46, 5.04, 2],
              ["2023-08-03T16:30:00", "EDT", 1.19, 1.26, 5.88, 2]
            ]
          }
        ]
      }
    ],
    "error": null
  }
}
```

#### 2.5.2 빈 결과 (실적 발표 없음)

```json
{
  "finance": {
    "result": [
      {
        "documents": [
          {
            "columns": [...],
            "rows": []
          }
        ]
      }
    ],
    "error": null
  }
}
```

### 2.6 yfinance에서의 활용

yfinance는 `_get_earnings_dates_using_screener()` 메서드에서 Visualization API를 활용합니다:

**주요 로직**:
1. POST 요청으로 실적 발표 데이터 조회
2. columns 배열에서 컬럼명 추출
3. rows 배열을 DataFrame으로 변환
4. Event Type 코드를 문자열로 변환 (1→Call, 2→Earnings, 11→Meeting)
5. startdatetime을 Pandas Datetime으로 변환 및 시간대 적용
6. Surprise (%), EPS Estimate, Reported EPS를 float64로 변환
7. DataFrame 인덱스를 Earnings Date로 설정

**참고**: yfinance는 2025년 여름 Yahoo가 기존 HTML 스크래핑 엔드포인트를 중단하면서 이 API로 전환했습니다.

---

## 3. UFC 통합 설계

### 3.1 기존 아키텍처와의 통합 방안

Visualization API는 기존 UFC 아키텍처의 다음 계층에 통합됩니다:

```
YahooClient (Infrastructure)
    ↓ implements
EarningsHttpClient (Domain Interface)
    ↓ used by
EarningsService (Domain Service)
    ↓ used by
StockApi (Presentation)
```

**설계 원칙**:
- Clean Architecture 유지: Infrastructure → Domain ← Presentation
- 의존성 역전: Domain은 `EarningsHttpClient` 인터페이스에만 의존
- 단일 책임: Visualization API는 실적 발표 일정 조회 전용

### 3.2 네임스페이스 배치

실적 발표 일정은 **주식 정보**의 일부이므로 `StockApi`에 배치합니다.

#### 배치 예시

```
ufc.stock.getEarningsDates(symbol)              // 기본 (12개)
ufc.stock.getEarningsDates(symbol, limit)       // 개수 지정 (최대 100)
ufc.stock.getEarningsCalendar(symbol)           // 미래 일정만
ufc.stock.getEarningsHistory(symbol)            // 과거 일정만
```

**대안 (거부됨)**: 새로운 `CorpApi`에 배치
- `ufc.corp` 네임스페이스는 배당금, 주식분할 등 **기업 행동(Corporate Actions)**을 다룸
- 실적 발표는 기업 행동이 아닌 **이벤트 일정**이므로 부적합

### 3.3 필요한 모델 클래스 목록

#### 3.3.1 Domain 계층 (Public Models)

**위치**: `/src/main/kotlin/com/ulalax/ufc/yahoo/model/`

| 클래스명 | 역할 | 주요 필드 |
|---------|------|----------|
| `EarningsDates` | 실적 발표 일정 전체를 담는 도메인 모델 | symbol, earningsDates |
| `EarningsDate` | 단일 실적 발표 정보 | earningsDate, epsEstimate, epsActual, surprisePercent, eventType |
| `EarningsEventType` | 이벤트 타입 열거형 | CALL, EARNINGS, MEETING, UNKNOWN |

**EarningsDates 구조**:
```kotlin
data class EarningsDates(
    val symbol: String,
    val earningsDates: List<EarningsDate>
)
```

**EarningsDate 구조**:
```kotlin
data class EarningsDate(
    val earningsDate: Instant,              // 실적 발표 일시 (타임존 포함)
    val timezoneShortName: String?,         // 시간대 약어 (예: "EST")
    val epsEstimate: Double?,               // EPS 추정치
    val epsActual: Double?,                 // 실제 EPS (과거 데이터만)
    val surprisePercent: Double?,           // 서프라이즈 비율 (%)
    val eventType: EarningsEventType        // 이벤트 타입
)
```

**EarningsEventType 열거형**:
```kotlin
enum class EarningsEventType(val code: Int, val displayName: String) {
    CALL(1, "Call"),
    EARNINGS(2, "Earnings"),
    MEETING(11, "Meeting"),
    UNKNOWN(-1, "Unknown");

    companion object {
        fun fromCode(code: Int): EarningsEventType
    }
}
```

#### 3.3.2 Infrastructure 계층 (Internal Response Models)

**위치**: `/src/main/kotlin/com/ulalax/ufc/yahoo/internal/response/`

| 클래스명 | 역할 |
|---------|------|
| `VisualizationResponse` | Yahoo API의 전체 응답 래퍼 |
| `VisualizationFinance` | `finance` 객체 |
| `VisualizationResult` | `result` 배열의 요소 |
| `VisualizationDocument` | `documents` 배열의 요소 |
| `VisualizationColumn` | `columns` 배열의 요소 (컬럼 메타데이터) |
| `VisualizationError` | 에러 응답 |

**VisualizationResponse 구조**:
```kotlin
@Serializable
internal data class VisualizationResponse(
    val finance: VisualizationFinance
)

@Serializable
internal data class VisualizationFinance(
    val result: List<VisualizationResult>? = null,
    val error: VisualizationError? = null
)

@Serializable
internal data class VisualizationResult(
    val documents: List<VisualizationDocument>? = null
)

@Serializable
internal data class VisualizationDocument(
    val columns: List<VisualizationColumn>,
    val rows: List<List<JsonElement>>  // 타입 혼합으로 JsonElement 사용
)

@Serializable
internal data class VisualizationColumn(
    val label: String,
    val id: String
)

@Serializable
internal data class VisualizationError(
    val code: String?,
    val description: String?
)
```

#### 3.3.3 요청 Body 모델

**위치**: `/src/main/kotlin/com/ulalax/ufc/yahoo/internal/request/`

| 클래스명 | 역할 |
|---------|------|
| `VisualizationRequest` | POST Body 전체 |
| `VisualizationQuery` | query 객체 |

**VisualizationRequest 구조**:
```kotlin
@Serializable
internal data class VisualizationRequest(
    val size: Int,
    val query: VisualizationQuery,
    val sortField: String,
    val sortType: String,
    val entityIdType: String,
    val includeFields: List<String>
)

@Serializable
internal data class VisualizationQuery(
    val operator: String,
    val operands: List<String>
)
```

### 3.4 API 메서드 시그니처 정의

#### 3.4.1 YahooClient (Infrastructure)

**위치**: `/src/main/kotlin/com/ulalax/ufc/yahoo/YahooClient.kt`

```kotlin
/**
 * Visualization API를 호출하여 실적 발표 일정을 조회합니다.
 *
 * @param symbol 조회할 심볼 (예: "AAPL")
 * @param limit 조회할 데이터 개수 (기본값: 12, 최대: 100)
 * @return VisualizationDocument (Internal Response)
 * @throws ApiException API 호출 실패 시
 */
suspend fun visualization(
    symbol: String,
    limit: Int = 12
): VisualizationDocument
```

**동작**:
1. Rate Limiting 적용
2. 요청 Body 생성 (`VisualizationRequest`)
3. POST 요청 수행
4. HTTP 상태 코드 확인
5. 응답 파싱 및 에러 처리
6. `VisualizationDocument` 반환

#### 3.4.2 EarningsService (Domain)

**위치**: `/src/main/kotlin/com/ulalax/ufc/domain/earnings/EarningsService.kt` (신규)

```kotlin
/**
 * 실적 발표 일정을 조회합니다.
 *
 * @param symbol 조회할 심볼
 * @param limit 조회할 데이터 개수
 * @return EarningsDates
 */
suspend fun getEarningsDates(symbol: String, limit: Int = 12): EarningsDates
```

**동작**:
1. 심볼 유효성 검증
2. limit 범위 검증 (1-100)
3. 캐시 확인
4. `EarningsHttpClient.fetchEarningsDates()` 호출
5. `VisualizationDocument` → `EarningsDates` 변환
6. 캐싱 및 반환

#### 3.4.3 StockApi (Presentation)

**위치**: `/src/main/kotlin/com/ulalax/ufc/api/StockApi.kt` (기존 확장)

```kotlin
/**
 * 실적 발표 일정을 조회합니다 (과거 및 미래).
 *
 * @param symbol 조회할 심볼 (예: "AAPL")
 * @param limit 조회할 데이터 개수 (기본값: 12, 최대: 100)
 * @return EarningsDates
 * @throws UfcException 조회 실패 시
 */
suspend fun getEarningsDates(symbol: String, limit: Int = 12): EarningsDates

/**
 * 미래 실적 발표 일정만 조회합니다.
 *
 * @param symbol 조회할 심볼
 * @return EarningsDates (미래 일정만 포함)
 */
suspend fun getEarningsCalendar(symbol: String): EarningsDates

/**
 * 과거 실적 발표 이력만 조회합니다.
 *
 * @param symbol 조회할 심볼
 * @return EarningsDates (과거 일정만 포함)
 */
suspend fun getEarningsHistory(symbol: String): EarningsDates
```

**동작**:
- `getEarningsDates()`: `EarningsService.getEarningsDates()` 위임
- `getEarningsCalendar()`: 전체 조회 후 미래 일정 필터링
- `getEarningsHistory()`: 전체 조회 후 과거 일정 필터링

### 3.5 캐싱 전략

| API | 캐시 키 형식 | TTL | 이유 |
|-----|-------------|-----|------|
| `getEarningsDates(symbol, limit)` | `"earnings:dates:{symbol}:{limit}"` | 1시간 | 실적 일정은 자주 변경되지 않음 |
| `getEarningsCalendar(symbol)` | `"earnings:calendar:{symbol}"` | 1시간 | - |
| `getEarningsHistory(symbol)` | `"earnings:history:{symbol}"` | 6시간 | 과거 데이터는 변경 없음 |

**캐싱 주의사항**:
- limit가 다르면 별도 캐시 (12개 vs 50개는 다른 데이터)
- 미래 일정은 1시간마다 갱신 (일정 추가/변경 가능)
- 과거 이력은 6시간 캐싱 (변경 없음)

### 3.6 에러 처리 전략

| 에러 케이스 | ErrorCode | HTTP 상태 | 처리 방법 |
|-----------|-----------|-----------|----------|
| 잘못된 심볼 | `INVALID_SYMBOL` | 200 (빈 rows) | rows 확인 후 예외 발생 |
| 실적 일정 없음 | `EARNINGS_DATES_NOT_FOUND` | 200 (빈 rows) | 빈 리스트 반환 (예외 아님) |
| limit 범위 초과 | `INVALID_PARAMETER` | - | 1-100 검증 |
| API 에러 | `EXTERNAL_API_ERROR` | 4xx/5xx | HTTP 상태 코드 기반 예외 |
| Rate Limit 초과 | `RATE_LIMITED` | 429 | Rate Limiter에서 자동 대기 |
| 파싱 오류 | `DATA_PARSING_ERROR` | 200 | JSON 역직렬화 실패 시 |

**특이사항**:
- Yahoo API는 빈 rows를 반환하는 경우 에러가 아님 (정상 응답)
- 상장폐지 종목의 경우 빈 rows 반환 가능

---

## 4. 데이터 매핑

### 4.1 Yahoo 응답 필드 → UFC 도메인 모델 매핑

#### 4.1.1 VisualizationDocument → EarningsDates

**변환 로직**:
1. columns 배열에서 각 컬럼의 인덱스 매핑 생성
2. rows 배열을 순회하며 각 row를 `EarningsDate`로 변환
3. `List<EarningsDate>`를 `EarningsDates`로 래핑

**컬럼 인덱스 매핑**:
```kotlin
// columns 배열: [{"label": "Event Start Date", "id": "startdatetime"}, ...]
// 인덱스 맵 생성: {"startdatetime": 0, "timeZoneShortName": 1, ...}
val columnIndexMap: Map<String, Int> = columns.mapIndexed { index, column ->
    column.id to index
}.toMap()
```

#### 4.1.2 Row → EarningsDate 매핑

| Yahoo 필드 ID | UFC 필드 | 타입 변환 | Nullable | 비고 |
|--------------|---------|----------|----------|------|
| `startdatetime` | `earningsDate` | String → Instant | No | ISO 8601 파싱 |
| `timeZoneShortName` | `timezoneShortName` | String → String | Yes | "EST", "EDT" 등 |
| `epsestimate` | `epsEstimate` | Double → Double | Yes | 추정치 |
| `epsactual` | `epsActual` | Double → Double | Yes | 실제값 (미래는 null) |
| `epssurprisepct` | `surprisePercent` | Double → Double | Yes | 서프라이즈 % (미래는 null) |
| `eventtype` | `eventType` | Int → EarningsEventType | No | 1/2/11 → enum |

### 4.2 타입 변환 규칙

#### 4.2.1 JsonElement → 원시 타입

rows는 `List<List<JsonElement>>`로 파싱되므로, 각 요소를 적절한 타입으로 변환:

```kotlin
// String
val startDatetime: String = row[0].jsonPrimitive.content

// Double (nullable)
val epsEstimate: Double? = row[2].jsonPrimitive.doubleOrNull

// Int
val eventType: Int = row[5].jsonPrimitive.int
```

#### 4.2.2 ISO 8601 String → Instant

```kotlin
// Yahoo: "2024-02-01T16:30:00"
// UFC: Instant

fun String.toInstant(): Instant {
    // ISO 8601 형식을 Instant로 변환
    return Instant.parse(this + "Z")  // 타임존 미포함 시 UTC로 가정
}
```

**주의사항**:
- Yahoo API는 타임존을 별도 필드(`timeZoneShortName`)로 제공
- ISO 8601 문자열 자체에는 타임존 정보가 없을 수 있음
- 타임존 적용은 사용자 측에서 처리 (예: `Instant.toLocalDateTime(timezone)`)

#### 4.2.3 Event Type Code → Enum

```kotlin
fun Int.toEarningsEventType(): EarningsEventType {
    return when (this) {
        1 -> EarningsEventType.CALL
        2 -> EarningsEventType.EARNINGS
        11 -> EarningsEventType.MEETING
        else -> EarningsEventType.UNKNOWN
    }
}
```

### 4.3 Nullable 처리 전략

#### 4.3.1 필수 필드

다음 필드는 반드시 존재해야 하며, 없으면 예외 발생:
- `startdatetime` (earningsDate)
- `eventtype` (eventType)

#### 4.3.2 선택 필드

다음 필드는 nullable로 처리:
- `epsestimate`: 추정치가 없을 수 있음
- `epsactual`: 미래 일정은 null (아직 발표 안 됨)
- `epssurprisepct`: 미래 일정은 null
- `timeZoneShortName`: 일부 응답에서 누락 가능

#### 4.3.3 0 vs null 처리

Yahoo API는 값이 없을 때 `0.0` 또는 `null`을 반환:
- yfinance는 `0.0`을 `NaN`으로 변환
- UFC는 `0.0`을 `null`로 변환 권장 (의미적으로 "데이터 없음")

```kotlin
val epsActual: Double? = row[3].jsonPrimitive.doubleOrNull?.takeIf { it != 0.0 }
```

### 4.4 정렬 및 필터링

#### 4.4.1 정렬 방향

API 요청 시 `sortType`으로 정렬 지정:
- `"DESC"`: 최신 일정부터 (미래 → 과거)
- `"ASC"`: 오래된 일정부터 (과거 → 미래)

**권장**: `"DESC"` (최신 일정이 우선순위 높음)

#### 4.4.2 미래/과거 필터링

`getEarningsCalendar()` 및 `getEarningsHistory()` 구현 시:

```kotlin
// 현재 시각
val now = Clock.System.now()

// 미래 일정만
val future = earningsDates.filter { it.earningsDate > now }

// 과거 일정만
val past = earningsDates.filter { it.earningsDate <= now }
```

---

## 5. 에러 처리

### 5.1 예상되는 에러 케이스

#### 5.1.1 잘못된 심볼

**시나리오**: 존재하지 않는 심볼 조회 (예: "INVALID123")

**Yahoo 응답**:
```json
{
  "finance": {
    "result": [
      {
        "documents": [
          {
            "columns": [...],
            "rows": []
          }
        ]
      }
    ],
    "error": null
  }
}
```

**UFC 처리**:
- ErrorCode: `INVALID_SYMBOL`
- 메시지: "유효하지 않은 심볼입니다: INVALID123"
- 대안: 빈 `EarningsDates` 반환 (예외 대신)

#### 5.1.2 실적 일정 없음

**시나리오**: 상장폐지 종목 또는 일정이 없는 종목

**Yahoo 응답**: 위와 동일 (빈 rows)

**UFC 처리**:
- 예외 발생하지 않음
- 빈 리스트 반환: `EarningsDates(symbol, emptyList())`
- 로그 경고: "No earnings dates found for symbol: {symbol}"

#### 5.1.3 limit 범위 초과

**시나리오**: `limit = 150` (최대 100 초과)

**UFC 처리**:
- ErrorCode: `INVALID_PARAMETER`
- 메시지: "limit은 1-100 범위여야 합니다: 150"
- 사전 검증 (API 호출 전)

#### 5.1.4 파싱 오류

**시나리오**: Yahoo API 응답 형식 변경 또는 손상된 JSON

**Kotlin 예외**: `SerializationException`, `JsonDecodingException`

**UFC 처리**:
- ErrorCode: `DATA_PARSING_ERROR`
- 메시지: "실적 일정 응답 파싱 실패: {원본 메시지}"
- 원본 응답 로그 기록

#### 5.1.5 HTTP 에러

**시나리오**: 400 Bad Request, 500 Internal Server Error

**UFC 처리**:
- ErrorCode: `EXTERNAL_API_ERROR`
- 메시지: "Visualization API 요청 실패: HTTP {status}"
- metadata: `{"symbol": "AAPL", "limit": 12, "httpStatus": 400}`

### 5.2 에러 응답 구조

#### 5.2.1 VisualizationError (Internal Response)

```kotlin
@Serializable
internal data class VisualizationError(
    val code: String?,
    val description: String?
)
```

#### 5.2.2 UFC Exception 구조

```kotlin
throw ApiException(
    errorCode = ErrorCode.INVALID_SYMBOL,
    message = "유효하지 않은 심볼입니다: $symbol",
    metadata = mapOf("symbol" to symbol, "limit" to limit)
)
```

### 5.3 재시도 전략

| 에러 타입 | 재시도 | 최대 횟수 | 대기 시간 | 비고 |
|----------|-------|----------|----------|------|
| Rate Limit (429) | Yes | 무제한 | Rate Limiter 자동 | - |
| Network Error | Yes | 3회 | Exponential backoff | - |
| HTTP 5xx | Yes | 3회 | 1s, 2s, 4s | - |
| HTTP 4xx | No | - | - | 클라이언트 오류 |
| 파싱 오류 | No | - | - | API 변경 가능성 |

---

## 6. 테스트 전략

### 6.1 단위 테스트 시나리오

#### 6.1.1 YahooClient.visualization() 테스트

**위치**: `/src/test/kotlin/com/ulalax/ufc/yahoo/YahooClientTest.kt`

| 테스트 케이스 | 목적 | Mock 응답 | 검증 항목 |
|-------------|------|-----------|----------|
| `정상 응답 파싱` | 기본 기능 검증 | 유효한 VisualizationResponse | columns, rows 정확히 파싱 |
| `빈 rows 처리` | 데이터 없는 경우 | rows = [] | 예외 없이 빈 document 반환 |
| `null 필드 처리` | Nullable 검증 | epsactual = null | null 필드가 예외 발생 안 함 |
| `Event Type 변환` | 코드 → enum | eventtype = 1, 2, 11 | 올바른 enum 매핑 |
| `HTTP 에러 처리` | 네트워크 에러 | HTTP 500 | ApiException (EXTERNAL_API_ERROR) |
| `파싱 에러 처리` | 잘못된 JSON | 손상된 JSON | ApiException (DATA_PARSING_ERROR) |

#### 6.1.2 EarningsService.getEarningsDates() 테스트

**위치**: `/src/test/kotlin/com/ulalax/ufc/domain/earnings/EarningsServiceTest.kt`

| 테스트 케이스 | 목적 | Mock 응답 | 검증 항목 |
|-------------|------|-----------|----------|
| `VisualizationDocument → EarningsDates 변환` | 타입 변환 검증 | 유효한 document | 모든 필드 정확히 매핑 |
| `ISO 8601 → Instant` | 시간 변환 | "2024-02-01T16:30:00" | Instant 정확성 |
| `Event Type 변환` | enum 변환 | 1, 2, 11, 99 | CALL, EARNINGS, MEETING, UNKNOWN |
| `0.0 → null 변환` | null 처리 | epsactual = 0.0 | null로 변환 |
| `limit 검증` | 파라미터 검증 | limit = 150 | INVALID_PARAMETER 예외 |
| `캐싱 동작` | 캐시 히트 | - | 2번째 호출 시 HTTP 미호출 |

#### 6.1.3 StockApi.getEarningsDates() 테스트

**위치**: `/src/test/kotlin/com/ulalax/ufc/api/StockApiTest.kt`

| 테스트 케이스 | 목적 | Mock 응답 | 검증 항목 |
|-------------|------|-----------|----------|
| `기본 조회 (limit=12)` | 기본 동작 | EarningsService 호출 | EarningsDates 반환 |
| `limit 지정 조회` | 파라미터 전달 | limit=50 | 50개 조회 |
| `미래 일정 필터링` | getEarningsCalendar() | 과거/미래 혼합 | 미래만 반환 |
| `과거 이력 필터링` | getEarningsHistory() | 과거/미래 혼합 | 과거만 반환 |

### 6.2 통합 테스트 시나리오

#### 6.2.1 실제 Yahoo API 호출 테스트

**위치**: `/src/integrationTest/kotlin/com/ulalax/ufc/yahoo/YahooClientIntegrationTest.kt`

| 테스트 케이스 | 심볼 | 검증 항목 |
|-------------|------|----------|
| `주식 실적 일정 조회` | AAPL | earningsDates 크기 > 0, 미래/과거 데이터 혼합 |
| `ETF 실적 일정 조회` | SPY | earningsDates 크기 = 0 (ETF는 실적 없음) |
| `잘못된 심볼 조회` | INVALID123 | 빈 earningsDates 또는 예외 |
| `limit 변경 조회` | AAPL, limit=50 | earningsDates 크기 ≤ 50 |

#### 6.2.2 End-to-End 테스트

**위치**: `/src/integrationTest/kotlin/com/ulalax/ufc/UfcE2ETest.kt`

| 테스트 케이스 | 시나리오 | 검증 항목 |
|-------------|---------|----------|
| `UFC를 통한 실적 일정 조회` | ufc.stock.getEarningsDates("AAPL") | EarningsDates 반환 |
| `미래 일정만 조회` | ufc.stock.getEarningsCalendar("AAPL") | 모든 earningsDate > now |
| `과거 이력만 조회` | ufc.stock.getEarningsHistory("AAPL") | 모든 earningsDate <= now |
| `캐싱 동작 확인` | 연속 2회 호출 | 2번째 호출이 더 빠름 |

### 6.3 성능 테스트 시나리오

| 테스트 케이스 | 목표 | 측정 항목 |
|-------------|------|----------|
| `단일 조회 응답 시간` | < 1000ms | P50, P95, P99 |
| `캐시 히트 응답 시간` | < 10ms | P50, P95, P99 |
| `대량 데이터 조회 (limit=100)` | < 1500ms | P50, P95, P99 |

### 6.4 테스트 데이터

#### 6.4.1 Mock VisualizationResponse

```json
{
  "finance": {
    "result": [
      {
        "documents": [
          {
            "columns": [
              {"label": "Event Start Date", "id": "startdatetime"},
              {"label": "Timezone short name", "id": "timeZoneShortName"},
              {"label": "EPS Estimate", "id": "epsestimate"},
              {"label": "Reported EPS", "id": "epsactual"},
              {"label": "Surprise (%)", "id": "epssurprisepct"},
              {"label": "Event Type", "id": "eventtype"}
            ],
            "rows": [
              ["2024-05-02T16:30:00", "EDT", 1.50, null, null, 2],
              ["2024-02-01T16:30:00", "EST", 2.10, 2.18, 3.81, 2],
              ["2023-11-02T16:30:00", "EDT", 1.39, 1.46, 5.04, 2]
            ]
          }
        ]
      }
    ],
    "error": null
  }
}
```

#### 6.4.2 Mock 빈 응답

```json
{
  "finance": {
    "result": [
      {
        "documents": [
          {
            "columns": [...],
            "rows": []
          }
        ]
      }
    ],
    "error": null
  }
}
```

---

## 7. 구현 우선순위

### 7.1 Phase 1: 기본 기능 (필수)

**목표**: 실적 발표 일정 조회 기본 기능 구현

**작업 항목**:
1. Internal Response Models 정의 (`VisualizationResponse`, `VisualizationDocument`)
2. Internal Request Models 정의 (`VisualizationRequest`, `VisualizationQuery`)
3. YahooClient.visualization() 구현 (POST 요청)
4. Domain Models 정의 (`EarningsDates`, `EarningsDate`, `EarningsEventType`)
5. EarningsService 구현 (변환 로직)
6. YahooApiUrls에 VISUALIZATION 엔드포인트 추가
7. 단위 테스트 작성 (YahooClientTest, EarningsServiceTest)

**검증 기준**:
- `YahooClient.visualization("AAPL", 12)` 정상 동작
- VisualizationDocument → EarningsDates 변환 성공
- 단위 테스트 커버리지 > 80%

### 7.2 Phase 2: API 통합 (중요)

**목표**: StockApi에 메서드 추가 및 캐싱 구현

**작업 항목**:
1. StockApi.getEarningsDates() 구현
2. 캐싱 로직 추가 (CacheHelper 활용, TTL: 1시간)
3. 에러 처리 강화 (빈 rows, 파싱 오류)
4. 통합 테스트 작성 (실제 API 호출)
5. EarningsHttpClient 인터페이스 정의 (Domain Layer)

**검증 기준**:
- `ufc.stock.getEarningsDates("AAPL")` 정상 동작
- 캐싱 동작 확인
- 통합 테스트 통과

### 7.3 Phase 3: 편의 기능 (선택)

**목표**: 미래/과거 필터링 및 고급 기능

**작업 항목**:
1. StockApi.getEarningsCalendar() 구현 (미래 일정만)
2. StockApi.getEarningsHistory() 구현 (과거 이력만)
3. 정렬 옵션 지원 (sortType: ASC/DESC)
4. limit 기본값 조정 (12 → 20)
5. 문서화 (KDoc 및 사용 가이드)

**검증 기준**:
- 미래/과거 필터링 정확도 100%
- 사용자 가이드 작성 완료

### 7.4 Phase 4: 최적화 (선택)

**목표**: 성능 최적화 및 사용성 개선

**작업 항목**:
1. 캐시 TTL 최적화 (과거 이력은 6시간)
2. 배치 조회 지원 (다중 심볼)
3. 성능 벤치마크 테스트
4. QuoteSummary earningsDates 모듈과 통합 검토
5. 실적 발표 알림 기능 (미래 일정 기반)

**검증 기준**:
- 응답 시간 < 1000ms (P95)
- 캐시 히트율 > 60%

---

## 부록

### A. 참고 링크

- yfinance Visualization 구현: `/home/ulalax/project/kairos/yfinance/yfinance/base.py` (Line 864-914)
- UFC Architecture: `/home/ulalax/project/kairos/ufc/doc/ARCHITECTURE.md`
- UFC YahooClient: `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/yahoo/YahooClient.kt`

### B. 용어 정리

| 용어 | 설명 |
|-----|------|
| **Visualization API** | Yahoo Finance의 실적 발표 일정 조회 API |
| **EPS** | Earnings Per Share (주당순이익) |
| **Surprise (%)** | (실제 EPS - 추정 EPS) / 추정 EPS × 100 |
| **Earnings Call** | 실적 발표 전화 회의 |
| **Earnings Report** | 공식 실적 발표 |
| **Stockholders Meeting** | 주주총회 |

### C. Yahoo API 제약사항

1. **비공식 API**: Yahoo Finance는 공식 문서를 제공하지 않으며, 언제든 변경 가능
2. **최대 100개 제한**: size 파라미터는 100을 초과할 수 없음
3. **POST 전용**: GET 요청 불가, 반드시 POST Body 사용
4. **테이블 형식**: rows가 2차원 배열이므로 파싱 복잡도 증가
5. **타입 혼합**: rows의 값이 String, Double, Int 등 혼합

### D. 변경 이력

| 버전 | 날짜 | 변경 내용 |
|-----|------|----------|
| 1.0 | 2025-12-05 | 초안 작성 |

---

**작성자**: Claude (Anthropic AI)
**검토자**: -
**승인자**: -
