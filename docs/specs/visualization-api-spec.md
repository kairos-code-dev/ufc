# Visualization API 기술 명세서

> **Version**: 2.1
> **작성일**: 2025-12-05

---

## 1. 개요

### 1.1 API 목적

Yahoo Finance Visualization API를 통해 특정 심볼의 **실적 발표 일정(Earnings Dates)**을 조회한다.

| 항목 | 값 |
|-----|---|
| 엔드포인트 | `POST https://query1.finance.yahoo.com/v1/finance/visualization` |
| HTTP 메서드 | POST |
| 인증 | 불필요 |

### 1.2 제공 데이터

| 데이터 | 설명 |
|-------|------|
| 실적 발표 일시 | 과거 및 미래 실적 발표 일정 |
| EPS 추정치 | 애널리스트 컨센서스 |
| EPS 실제값 | 발표된 실제 EPS (과거만) |
| 서프라이즈 비율 | (실제 - 추정) / 추정 × 100 |
| 이벤트 타입 | Call, Earnings, Meeting |

---

## 2. 데이터 소스 분석

### 2.1 요청 Body 파라미터

| 필드 | 타입 | 필수 | 설명 |
|-----|------|-----|------|
| size | Int | Yes | 조회 개수 (1-100) |
| query.operator | String | Yes | `"eq"` 고정 |
| query.operands | Array | Yes | `["ticker", "{symbol}"]` |
| sortField | String | Yes | `"startdatetime"` 고정 |
| sortType | String | Yes | `"DESC"` 또는 `"ASC"` |
| entityIdType | String | Yes | `"earnings"` 고정 |
| includeFields | Array | Yes | 응답 필드 목록 |

### 2.2 includeFields 값

| 필드 ID | 설명 |
|--------|------|
| startdatetime | 실적 발표 시간 (ISO 8601) |
| timeZoneShortName | 시간대 약어 |
| epsestimate | EPS 추정치 |
| epsactual | EPS 실제값 |
| epssurprisepct | 서프라이즈 (%) |
| eventtype | 이벤트 타입 코드 |

### 2.3 응답 rows 필드

| 인덱스 | 필드 | 타입 | Nullable |
|-------|-----|------|----------|
| 0 | startdatetime | String | No |
| 1 | timeZoneShortName | String | Yes |
| 2 | epsestimate | Double | Yes |
| 3 | epsactual | Double | Yes |
| 4 | epssurprisepct | Double | Yes |
| 5 | eventtype | Int | No |

### 2.4 Event Type 코드

| 코드 | 의미 |
|-----|-----|
| 1 | Call (실적 전화 회의) |
| 2 | Earnings (실적 발표) |
| 11 | Meeting (주주총회) |

---

## 3. 데이터/모델 설계

### 3.1 Domain 모델

#### EarningsDates

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| symbol | String | No | 티커 심볼 |
| earningsDates | List&lt;EarningsDate&gt; | No | 실적 발표 목록 |

#### EarningsDate

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| earningsDate | Instant | No | 실적 발표 일시 |
| timezoneShortName | String | Yes | 시간대 약어 |
| epsEstimate | Double | Yes | EPS 추정치 |
| epsActual | Double | Yes | EPS 실제값 |
| surprisePercent | Double | Yes | 서프라이즈 (%) |
| eventType | EarningsEventType | No | 이벤트 타입 |

#### EarningsEventType

```kotlin
enum class EarningsEventType(val code: Int) {
    CALL(1),
    EARNINGS(2),
    MEETING(11),
    UNKNOWN(-1)
}
```

### 3.2 Internal Request 모델

#### VisualizationRequest

| 필드 | 타입 |
|-----|------|
| size | Int |
| query | VisualizationQuery |
| sortField | String |
| sortType | String |
| entityIdType | String |
| includeFields | List&lt;String&gt; |

#### VisualizationQuery

| 필드 | 타입 |
|-----|------|
| operator | String |
| operands | List&lt;String&gt; |

### 3.3 Internal Response 모델

#### 구조

| 클래스 | 필드 | 타입 |
|-------|-----|------|
| VisualizationResponse | finance | VisualizationFinance |
| VisualizationFinance | result | List&lt;VisualizationResult&gt;? |
| | error | VisualizationError? |
| VisualizationResult | documents | List&lt;VisualizationDocument&gt;? |
| VisualizationDocument | columns | List&lt;VisualizationColumn&gt; |
| | rows | List&lt;List&lt;JsonElement&gt;&gt; |
| VisualizationColumn | label | String |
| | id | String |
| VisualizationError | code | String? |
| | description | String? |

### 3.4 API 메서드 시그니처

```kotlin
suspend fun visualization(symbol: String, limit: Int = 12): EarningsDates
```

| 파라미터 | 타입 | 기본값 | 제약 |
|---------|------|-------|------|
| symbol | String | - | 필수 |
| limit | Int | 12 | 1-100 |

| 반환 | 설명 |
|-----|------|
| EarningsDates | 실적 발표 일정 목록 |

### 3.5 필드 매핑

| Yahoo 필드 | Domain 필드 | 변환 |
|-----------|------------|------|
| startdatetime | earningsDate | ISO 8601 → Instant |
| timeZoneShortName | timezoneShortName | 그대로 |
| epsestimate | epsEstimate | Double? |
| epsactual | epsActual | 0.0 → null |
| epssurprisepct | surprisePercent | 0.0 → null |
| eventtype | eventType | Int → Enum |

---

## 4. 예외 처리

### 4.1 ErrorCode 매핑

| 에러 케이스 | ErrorCode | 설명 |
|-----------|-----------|------|
| limit 범위 초과 | INVALID_PARAMETER | 1-100 검증 실패 |
| HTTP 4xx/5xx | EXTERNAL_API_ERROR | API 오류 |
| Rate Limit (429) | RATE_LIMITED | 요청 제한 |
| JSON 파싱 실패 | DATA_PARSING_ERROR | 역직렬화 실패 |

### 4.2 빈 결과 처리

| 상황 | 처리 |
|-----|------|
| rows = [] | 빈 EarningsDates 반환 (예외 아님) |
| 존재하지 않는 심볼 | 빈 EarningsDates 반환 |
| ETF (실적 없음) | 빈 EarningsDates 반환 |

### 4.3 재시도 정책

| 에러 | 재시도 | 횟수 |
|-----|-------|-----|
| Rate Limit (429) | Yes | Rate Limiter 처리 |
| Network Error | Yes | 3회 |
| HTTP 5xx | Yes | 3회 |
| HTTP 4xx | No | - |

---

## 5. 참고 자료

### 5.1 API 제약사항

| 제약 | 설명 |
|-----|------|
| 비공식 API | 문서화되지 않음, 변경 가능 |
| 최대 100개 | size 상한 |
| POST 전용 | GET 불가 |
| 타입 혼합 | rows에 String, Double, Int 혼합 |

### 5.2 용어

| 용어 | 설명 |
|-----|------|
| EPS | Earnings Per Share (주당순이익) |
| Surprise | (실제 - 추정) / 추정 × 100 |
