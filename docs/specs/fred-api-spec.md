# FRED API 기술 명세서

> **Version**: 1.0
> **작성일**: 2025-12-05

---

## 1. 개요

### 1.1 API 목적

FRED (Federal Reserve Economic Data) API를 통해 **미국 연방준비제도의 경제 데이터**를 조회한다. GDP, 실업률, 금리 등 수천 개의 경제 지표 시계열 데이터를 제공한다.

| API | 엔드포인트 | HTTP 메서드 | 인증 |
|-----|-----------|------------|------|
| Series Data | `GET https://api.stlouisfed.org/fred/series/observations` | GET | API Key 필수 |
| Series Info | `GET https://api.stlouisfed.org/fred/series` | GET | API Key 필수 |

### 1.2 제공 데이터

| 데이터 | 설명 |
|-------|------|
| 경제 시계열 | GDP, 실업률, 금리, 물가지수 등 경제 지표의 시간별 변화 |
| 시계열 메타데이터 | 시계열 제목, 빈도, 단위, 계절 조정 여부 등 |
| 관측값 | 특정 날짜의 경제 지표 값 (시계열 데이터 포인트) |

### 1.3 Yahoo Finance API와의 차이점

| 구분 | FRED API | Yahoo Finance API |
|------|----------|------------------|
| 목적 | 거시경제 데이터 조회 | 주식/금융 상품 시장 데이터 조회 |
| 데이터 범위 | 경제 지표 (GDP, 실업률, 금리) | 주가, 재무제표, 옵션 |
| 인증 | API Key 필수 (공식 등록) | Cookie/Crumb (비공식) |
| 신뢰성 | 공식 API, 매우 안정적 | 비공식 API, 변경 가능성 |
| Rate Limit | 120 requests/second | 제한 없음 (자체 제한 권장) |
| 데이터 출처 | 미국 연방준비제도 | Yahoo Finance |
| API 문서 | 공식 문서 완비 | 문서 없음 |

### 1.4 주요 시계열 ID

| ID | 설명 | 빈도 | 단위 |
|----|------|------|------|
| GDP | 미국 국내총생산 (명목) | Quarterly | Billions of Dollars |
| GDPC1 | 미국 국내총생산 (실질) | Quarterly | Billions of Chained 2017 Dollars |
| UNRATE | 실업률 | Monthly | Percent |
| CPIAUCSL | 소비자물가지수 (전체) | Monthly | Index 1982-1984=100 |
| FEDFUNDS | 연방기금 실효금리 | Monthly | Percent |
| DFF | 연방기금금리 (일별) | Daily | Percent |
| DGS10 | 10년물 국채 수익률 | Daily | Percent |
| DGS2 | 2년물 국채 수익률 | Daily | Percent |
| T10Y2Y | 10년-2년 국채 스프레드 | Daily | Percent |
| MORTGAGE30US | 30년 모기지 금리 | Weekly | Percent |
| M2SL | M2 통화량 | Monthly | Billions of Dollars |
| INDPRO | 산업생산지수 | Monthly | Index 2017=100 |
| PAYEMS | 비농업 고용자수 | Monthly | Thousands of Persons |
| HOUST | 주택 착공 건수 | Monthly | Thousands of Units |

---

## 2. 데이터 소스 분석

### 2.1 Series Observations API

#### 요청 파라미터

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|---------|------|-----|------|--------|
| series_id | String | Yes | FRED 시계열 ID | - |
| api_key | String | Yes | FRED API Key | - |
| file_type | String | No | 응답 형식 | json |
| observation_start | String | No | 시작 날짜 (YYYY-MM-DD) | 시계열 시작일 |
| observation_end | String | No | 종료 날짜 (YYYY-MM-DD) | 시계열 종료일 |
| frequency | String | No | 데이터 빈도 (aggregation) | 원본 빈도 |
| sort_order | String | No | 정렬 순서 (asc, desc) | asc |
| limit | Int | No | 반환 개수 제한 | 100000 |
| offset | Int | No | 시작 오프셋 | 0 |

#### 응답 구조

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| realtime_start | String | No | 실시간 데이터 시작일 (YYYY-MM-DD) |
| realtime_end | String | No | 실시간 데이터 종료일 (YYYY-MM-DD) |
| observation_start | String | No | 관측 시작일 (YYYY-MM-DD) |
| observation_end | String | No | 관측 종료일 (YYYY-MM-DD) |
| units | String | No | 단위 (Percent, Billions of Dollars 등) |
| output_type | Int | No | 출력 타입 (1=observations) |
| file_type | String | No | 파일 타입 (json) |
| order_by | String | No | 정렬 기준 (observation_date) |
| sort_order | String | No | 정렬 순서 (asc, desc) |
| count | Int | No | 반환된 관측값 개수 |
| offset | Int | No | 오프셋 |
| limit | Int | No | 제한 개수 |
| observations | Array | No | 관측값 배열 |

#### Observation 객체

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| realtime_start | String | No | 실시간 시작일 |
| realtime_end | String | No | 실시간 종료일 |
| date | String | No | 관측 날짜 (YYYY-MM-DD) |
| value | String | No | 관측값 ("." = 데이터 없음) |

#### 응답 예시

```json
{
  "realtime_start": "2024-12-05",
  "realtime_end": "2024-12-05",
  "observation_start": "2020-01-01",
  "observation_end": "2024-12-05",
  "units": "Percent",
  "output_type": 1,
  "file_type": "json",
  "order_by": "observation_date",
  "sort_order": "asc",
  "count": 60,
  "offset": 0,
  "limit": 100000,
  "observations": [
    {
      "realtime_start": "2024-12-05",
      "realtime_end": "2024-12-05",
      "date": "2020-01-01",
      "value": "3.5"
    },
    {
      "realtime_start": "2024-12-05",
      "realtime_end": "2024-12-05",
      "date": "2020-02-01",
      "value": "3.5"
    },
    {
      "realtime_start": "2024-12-05",
      "realtime_end": "2024-12-05",
      "date": "2020-03-01",
      "value": "."
    }
  ]
}
```

### 2.2 Series Info API

#### 요청 파라미터

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|---------|------|-----|------|--------|
| series_id | String | Yes | FRED 시계열 ID | - |
| api_key | String | Yes | FRED API Key | - |
| file_type | String | No | 응답 형식 | json |

#### 응답 구조

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| realtime_start | String | No | 실시간 데이터 시작일 |
| realtime_end | String | No | 실시간 데이터 종료일 |
| seriess | Array | No | 시계열 정보 배열 (단수 조회 시에도 배열) |

#### Series 객체

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| id | String | No | 시계열 ID |
| title | String | No | 시계열 제목 |
| observation_start | String | No | 첫 관측일 (YYYY-MM-DD) |
| observation_end | String | No | 마지막 관측일 (YYYY-MM-DD) |
| frequency | String | No | 데이터 빈도 (Daily, Monthly, Quarterly 등) |
| frequency_short | String | No | 빈도 약어 (D, M, Q 등) |
| units | String | No | 단위 (Percent, Billions of Dollars 등) |
| units_short | String | No | 단위 약어 (%, Bil. of $ 등) |
| seasonal_adjustment | String | No | 계절 조정 여부 (Seasonally Adjusted, Not Seasonally Adjusted) |
| seasonal_adjustment_short | String | No | 계절 조정 약어 (SA, NSA) |
| last_updated | String | No | 마지막 업데이트 시각 (YYYY-MM-DD HH:MM:SS 형식) |
| popularity | Int | No | 인기도 점수 |
| notes | String | Yes | 시계열 설명 (HTML 포함 가능) |

#### 응답 예시

```json
{
  "realtime_start": "2024-12-05",
  "realtime_end": "2024-12-05",
  "seriess": [
    {
      "id": "UNRATE",
      "title": "Unemployment Rate",
      "observation_start": "1948-01-01",
      "observation_end": "2024-11-01",
      "frequency": "Monthly",
      "frequency_short": "M",
      "units": "Percent",
      "units_short": "%",
      "seasonal_adjustment": "Seasonally Adjusted",
      "seasonal_adjustment_short": "SA",
      "last_updated": "2024-12-06 07:31:01-06",
      "popularity": 94,
      "notes": "The unemployment rate represents the number of unemployed..."
    }
  ]
}
```

### 2.3 DataFrequency (빈도 파라미터)

| Enum 값 | API 값 | 설명 |
|---------|--------|------|
| Daily | d | 일별 데이터 |
| Weekly | w | 주별 데이터 |
| Biweekly | bw | 격주 데이터 |
| Monthly | m | 월별 데이터 |
| Quarterly | q | 분기별 데이터 |
| Semiannual | sa | 반기별 데이터 |
| Annual | a | 연별 데이터 |

**Aggregation 규칙**:
- 원본 빈도보다 낮은 빈도로 집계 가능 (예: 일별 → 월별)
- 원본 빈도보다 높은 빈도로 변환 불가 (예: 월별 → 일별)
- 집계 방법: 평균값(average), 합계(sum), 기말값(end of period) 등 시계열마다 다름

---

## 3. 데이터/모델 설계

### 3.1 Domain 모델

#### FredSeries

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| id | String | No | FRED 시계열 ID |
| title | String | No | 시계열 제목 |
| frequency | String | No | 데이터 빈도 (Daily, Monthly, Quarterly 등) |
| units | String | No | 단위 (Percent, Billions of Dollars 등) |
| observations | List&lt;FredObservation&gt; | No | 관측값 목록 (빈 리스트 가능) |

**특징**:
- 시계열 메타데이터와 관측값을 모두 포함
- `series()` 메서드 호출 시 반환되는 타입
- 관측값은 날짜순으로 정렬됨

#### FredObservation

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| date | LocalDate | No | 관측 날짜 |
| value | Double | Yes | 관측값 (데이터 없으면 null) |

**Null 처리 규칙**:
- FRED API에서 `value = "."` → Domain 모델에서 `value = null`
- 데이터가 아직 발표되지 않았거나 수정 중인 경우 발생
- null 값도 관측값 목록에 포함됨 (날짜 정보 유지)

#### FredSeriesInfo

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| id | String | No | FRED 시계열 ID |
| title | String | No | 시계열 제목 |
| frequency | String | No | 데이터 빈도 |
| units | String | No | 단위 |
| seasonalAdjustment | String | Yes | 계절 조정 여부 (SA, NSA) |
| lastUpdated | String | Yes | 마지막 업데이트 시각 |

**특징**:
- 관측값 없이 메타데이터만 포함
- `seriesInfo()` 메서드 호출 시 반환되는 타입
- 시계열 존재 여부 확인이나 빠른 메타데이터 조회에 사용

#### DataFrequency (Enum)

```kotlin
enum class DataFrequency(val value: String) {
    Daily("d"),
    Weekly("w"),
    Biweekly("bw"),
    Monthly("m"),
    Quarterly("q"),
    Semiannual("sa"),
    Annual("a")
}
```

**헬퍼 함수**:
- `dataFrequencyFromValue(value: String): DataFrequency?` - API 값으로부터 enum 조회
- `DataFrequency.toKoreanString(): String` - 한글 이름 반환 (확장 함수)
- `DataFrequency.compareSizeWith(other: DataFrequency): Int` - 빈도 크기 비교 (확장 함수)
- `DataFrequency.isLongTerm(): Boolean` - 장기 빈도(월 이상) 여부 (확장 함수)
- `DataFrequency.isShortTerm(): Boolean` - 단기 빈도(주 이하) 여부 (확장 함수)

### 3.2 Internal Response 모델

#### FredSeriesResponse

| 필드 | 타입 | 설명 |
|-----|------|------|
| seriess | List&lt;FredSeriesDto&gt; | 시계열 정보 배열 |

**특징**:
- "seriess" 필드명 유지 (FRED API 원본 필드명)
- 단일 시계열 조회 시에도 배열로 반환

#### FredSeriesDto

| 필드 | 타입 | 설명 |
|-----|------|------|
| id | String | 시계열 ID |
| title | String | 시계열 제목 |
| frequency | String | 데이터 빈도 |
| units | String | 단위 |
| seasonalAdjustment | String (@SerialName("seasonal_adjustment")) | 계절 조정 |
| lastUpdated | String (@SerialName("last_updated")) | 마지막 업데이트 |

#### FredObservationsResponse

| 필드 | 타입 | 설명 |
|-----|------|------|
| observations | List&lt;FredObservationDto&gt; | 관측값 배열 |

#### FredObservationDto

| 필드 | 타입 | 설명 |
|-----|------|------|
| date | String | 관측 날짜 (YYYY-MM-DD) |
| value | String | 관측값 ("." = 데이터 없음) |

### 3.3 API 메서드 시그니처

#### FredClient

```kotlin
suspend fun series(
    seriesId: String,
    startDate: LocalDate? = null,
    endDate: LocalDate? = null,
    frequency: DataFrequency? = null
): FredSeries

suspend fun seriesInfo(seriesId: String): FredSeriesInfo
```

| 메서드 | 파라미터 | 반환 | 설명 |
|-------|---------|-----|------|
| series | seriesId: String | FredSeries | 시계열 데이터 조회 (메타데이터 + 관측값) |
| | startDate: LocalDate? | | 시작 날짜 (null이면 시계열 시작일) |
| | endDate: LocalDate? | | 종료 날짜 (null이면 시계열 종료일) |
| | frequency: DataFrequency? | | 데이터 빈도 (null이면 원본 빈도) |
| seriesInfo | seriesId: String | FredSeriesInfo | 시계열 메타데이터만 조회 |

### 3.4 필드 매핑

#### Series Observations API → FredSeries

| FRED API 필드 | Domain 필드 | 변환 규칙 |
|--------------|------------|----------|
| seriess[0].id | id | String → String |
| seriess[0].title | title | String → String |
| seriess[0].frequency | frequency | String → String |
| seriess[0].units | units | String → String |
| observations[].date | observations[].date | String (YYYY-MM-DD) → LocalDate |
| observations[].value | observations[].value | String → Double? ("." → null) |

#### Series API → FredSeriesInfo

| FRED API 필드 | Domain 필드 | 변환 규칙 |
|--------------|------------|----------|
| seriess[0].id | id | String → String |
| seriess[0].title | title | String → String |
| seriess[0].frequency | frequency | String → String |
| seriess[0].units | units | String → String |
| seriess[0].seasonal_adjustment | seasonalAdjustment | String → String? |
| seriess[0].last_updated | lastUpdated | String → String? |

#### 타입 변환 상세

| 변환 | 구현 |
|-----|------|
| String (날짜) → LocalDate | `LocalDate.parse(dateString)` |
| String (관측값) → Double? | `if (value == ".") null else value.toDouble()` |
| String (시각) → String | 그대로 유지 (파싱 불필요) |

---

## 4. 예외 처리

### 4.1 ErrorCode 매핑

| 에러 케이스 | HTTP 상태 | ErrorCode | 설명 |
|-----------|-----------|-----------|------|
| 잘못된 seriesId | 400 | INVALID_PARAMETER | 시계열 ID가 존재하지 않음 |
| 빈 seriesId | - | INVALID_PARAMETER | seriesId.isBlank() |
| API Key 누락 | 400 | API_KEY_MISSING | api_key 파라미터 없음 |
| 잘못된 API Key | 400 | AUTH_FAILED | 유효하지 않은 API Key |
| Rate Limit 초과 | 429 | RATE_LIMITED | 120 requests/second 초과 |
| 날짜 형식 오류 | 400 | INVALID_PARAMETER | startDate/endDate 형식 오류 |
| startDate > endDate | - | INVALID_PARAMETER | 날짜 순서 오류 |
| 잘못된 frequency | 400 | INVALID_PARAMETER | 유효하지 않은 빈도 값 |
| 서버 오류 | 500-599 | EXTERNAL_API_ERROR | FRED 서버 오류 |
| 네트워크 오류 | - | NETWORK_ERROR | 연결 실패/타임아웃 |
| JSON 파싱 실패 | - | DATA_PARSING_ERROR | 응답 역직렬화 실패 |
| 날짜 파싱 실패 | - | DATA_PARSING_ERROR | 관측 날짜 형식 오류 |

### 4.2 FRED API 에러 응답

#### Bad Request (400)

```json
{
  "error_code": 400,
  "error_message": "Bad Request. The value for variable api_key is not registered."
}
```

| FRED 에러 메시지 | ErrorCode | 설명 |
|----------------|-----------|------|
| "api_key is not registered" | AUTH_FAILED | API Key 미등록 |
| "series does not exist" | INVALID_PARAMETER | 시계열 존재하지 않음 |
| "Invalid date format" | INVALID_PARAMETER | 날짜 형식 오류 |
| "The value for variable frequency is not valid" | INVALID_PARAMETER | 빈도 값 오류 |

### 4.3 빈 결과 처리

| 상황 | 처리 |
|-----|------|
| observations = [] | FredSeries 반환 (빈 관측값 목록) |
| seriess = [] | INVALID_PARAMETER 예외 (시계열 없음) |
| 존재하지 않는 시계열 | INVALID_PARAMETER 예외 |
| 날짜 범위에 데이터 없음 | FredSeries 반환 (빈 관측값 목록) |
| 모든 value = "." | FredSeries 반환 (모든 observation.value = null) |

### 4.4 재시도 정책

| 에러 | 재시도 | 횟수 | 대기 시간 |
|-----|-------|-----|----------|
| Rate Limit (429) | Yes | 무제한 | Token Bucket Rate Limiter |
| Network Error | Yes | 3회 | Exponential backoff (1s, 2s, 4s) |
| HTTP 5xx | Yes | 3회 | Exponential backoff (1s, 2s, 4s) |
| HTTP 400 | No | - | - |
| HTTP 401 | No | - | - |
| JSON 파싱 실패 | No | - | - |
| 날짜 파싱 실패 | No | - | - |

### 4.5 입력 검증

| 검증 항목 | 규칙 | 예외 |
|---------|------|------|
| seriesId | 공백 불가 | IllegalArgumentException |
| apiKey | 공백 불가 | IllegalArgumentException (FredClient.create 시) |
| startDate/endDate | startDate ≤ endDate | IllegalArgumentException |
| frequency | null 또는 유효한 DataFrequency | IllegalArgumentException |

---

## 5. 참고 자료

### 5.1 API 제약사항

| 제약 | 설명 |
|-----|------|
| 공식 API | FRED 공식 API, 안정적이고 변경 가능성 낮음 |
| Rate Limit | 일반 사용자: 120 requests/second |
| API Key 필수 | https://fred.stlouisfed.org/docs/api/api_key.html 에서 무료 발급 |
| 데이터 범위 | 시계열마다 다름 (일부는 1940년대부터 제공) |
| 업데이트 주기 | 시계열마다 다름 (일별, 월별, 분기별 등) |
| Aggregation 제약 | 원본 빈도보다 낮은 빈도로만 변환 가능 |

### 5.2 Rate Limiting

| 항목 | 값 |
|-----|-----|
| 기본 제한 | 120 requests/second |
| 구현 방식 | Token Bucket Algorithm |
| 공유 Limiter | GlobalRateLimiters.getFredLimiter() |
| 429 응답 처리 | 자동 재시도 (Rate Limiter 대기) |

**Rate Limiter 설정**:
```kotlin
val config = FredClientConfig(
    rateLimitConfig = RateLimitConfig(
        requestsPerSecond = 100.0,  // 기본값: 120.0
        burstCapacity = 100          // 기본값: 120
    )
)
val fred = FredClient.create(apiKey, config)
```

### 5.3 캐싱 전략

| API | 권장 TTL | 이유 |
|-----|---------|------|
| series() | 1-24시간 | 경제 데이터는 일별/월별/분기별 업데이트 |
| seriesInfo() | 24시간 | 메타데이터는 거의 변경되지 않음 |

**주기별 권장 TTL**:
- Daily 시계열: 1시간
- Weekly 시계열: 6시간
- Monthly 시계열: 24시간
- Quarterly 시계열: 48시간

### 5.4 주요 시계열 카테고리

#### 거시경제 지표

| ID | 설명 | 빈도 |
|----|------|------|
| GDP | 국내총생산 (명목) | Quarterly |
| GDPC1 | 국내총생산 (실질) | Quarterly |
| GDPPOT | 잠재 GDP | Quarterly |
| A191RL1Q225SBEA | GDP 성장률 (전년 대비) | Quarterly |

#### 노동시장

| ID | 설명 | 빈도 |
|----|------|------|
| UNRATE | 실업률 | Monthly |
| PAYEMS | 비농업 고용자수 | Monthly |
| CIVPART | 경제활동참가율 | Monthly |
| U6RATE | 확대 실업률 (U-6) | Monthly |

#### 물가/인플레이션

| ID | 설명 | 빈도 |
|----|------|------|
| CPIAUCSL | 소비자물가지수 (전체) | Monthly |
| CPILFESL | 근원 소비자물가지수 (식품/에너지 제외) | Monthly |
| PCEPI | 개인소비지출 물가지수 | Monthly |
| PCEPILFE | 근원 PCE 물가지수 | Monthly |

#### 금리/통화

| ID | 설명 | 빈도 |
|----|------|------|
| FEDFUNDS | 연방기금 실효금리 | Monthly |
| DFF | 연방기금금리 (일별) | Daily |
| DGS10 | 10년물 국채 수익률 | Daily |
| DGS2 | 2년물 국채 수익률 | Daily |
| DGS5 | 5년물 국채 수익률 | Daily |
| T10Y2Y | 10년-2년 국채 스프레드 | Daily |
| T10Y3M | 10년-3개월 국채 스프레드 | Daily |

#### 주택시장

| ID | 설명 | 빈도 |
|----|------|------|
| MORTGAGE30US | 30년 모기지 금리 | Weekly |
| MORTGAGE15US | 15년 모기지 금리 | Weekly |
| HOUST | 주택 착공 건수 | Monthly |
| CSUSHPISA | Case-Shiller 주택가격지수 | Monthly |

#### 통화량

| ID | 설명 | 빈도 |
|----|------|------|
| M1SL | M1 통화량 | Monthly |
| M2SL | M2 통화량 | Monthly |
| WM2NS | M2 통화량 (주별) | Weekly |

#### 산업생산

| ID | 설명 | 빈도 |
|----|------|------|
| INDPRO | 산업생산지수 | Monthly |
| CAPUTLB50001SQ | 설비가동률 | Quarterly |
| TOTALSA | 소매 판매 | Monthly |

### 5.5 용어

| 용어 | 설명 |
|-----|------|
| FRED | Federal Reserve Economic Data (연방준비제도 경제 데이터) |
| Series | 시계열 (특정 경제 지표의 시간에 따른 데이터) |
| Observation | 관측값 (특정 날짜의 시계열 데이터 포인트) |
| Realtime | 실시간 (데이터 조회 시점 기준) |
| SA | Seasonally Adjusted (계절 조정됨) |
| NSA | Not Seasonally Adjusted (계절 미조정) |
| SAAR | Seasonally Adjusted Annual Rate (계절조정 연율) |
| GDP | Gross Domestic Product (국내총생산) |
| CPI | Consumer Price Index (소비자물가지수) |
| PCE | Personal Consumption Expenditures (개인소비지출) |
| UNRATE | Unemployment Rate (실업률) |
| TTM | Trailing Twelve Months (최근 12개월) |

### 5.6 API Key 발급

1. https://fred.stlouisfed.org/docs/api/api_key.html 방문
2. "Request API Key" 클릭
3. 계정 생성 또는 로그인
4. 개인정보 입력 및 이용약관 동의
5. API Key 즉시 발급 (무료)

**사용 방법**:
```kotlin
val fred = FredClient.create(apiKey = "your-api-key-here")
```

또는 환경변수:
```bash
export FRED_API_KEY="your-api-key-here"
```

```kotlin
val apiKey = System.getenv("FRED_API_KEY")
val fred = FredClient.create(apiKey)
```

### 5.7 참고 링크

| 링크 | 설명 |
|-----|------|
| https://fred.stlouisfed.org/ | FRED 메인 페이지 |
| https://fred.stlouisfed.org/docs/api/fred/ | FRED API 공식 문서 |
| https://fred.stlouisfed.org/docs/api/api_key.html | API Key 발급 가이드 |
| https://fred.stlouisfed.org/categories | 시계열 카테고리 탐색 |
| https://fred.stlouisfed.org/tags/series | 인기 시계열 목록 |

---

## 부록

### A. 사용 예시

#### 기본 사용

```kotlin
// FredClient 생성
val fred = FredClient.create(apiKey = "your-api-key")

// GDP 데이터 조회 (전체 기간)
val gdp = fred.series("GDP")
println("${gdp.title}: ${gdp.observations.size} observations")

// 최신 관측값
val latest = gdp.observations.last()
println("${latest.date}: ${latest.value}")

// 리소스 정리
fred.close()
```

#### 날짜 범위 지정

```kotlin
val startDate = LocalDate.of(2020, 1, 1)
val endDate = LocalDate.of(2023, 12, 31)

val unrate = fred.series(
    seriesId = "UNRATE",
    startDate = startDate,
    endDate = endDate
)

// 범위 내 평균 실업률 계산
val avgUnemployment = unrate.observations
    .mapNotNull { it.value }
    .average()
println("Average unemployment rate: $avgUnemployment%")
```

#### 빈도 변환 (Aggregation)

```kotlin
// 일별 데이터를 월별로 집계
val dff = fred.series(
    seriesId = "DFF",  // 일별 연방기금금리
    frequency = DataFrequency.Monthly
)

println("Frequency: ${dff.frequency}")  // "Monthly"
```

#### 메타데이터만 조회

```kotlin
// 관측값 없이 메타데이터만 빠르게 조회
val info = fred.seriesInfo("GDP")

println("Title: ${info.title}")
println("Frequency: ${info.frequency}")
println("Units: ${info.units}")
println("Seasonal Adjustment: ${info.seasonalAdjustment}")
println("Last Updated: ${info.lastUpdated}")
```

#### use 블록 사용

```kotlin
FredClient.create(apiKey).use { fred ->
    val gdp = fred.series("GDP")
    val unrate = fred.series("UNRATE")

    println("GDP: ${gdp.observations.size} points")
    println("UNRATE: ${unrate.observations.size} points")
} // 자동으로 close() 호출
```

#### 다중 시계열 조회

```kotlin
val fred = FredClient.create(apiKey)

val seriesIds = listOf("GDP", "UNRATE", "FEDFUNDS", "CPIAUCSL")
val results = seriesIds.map { id ->
    fred.series(id)
}

results.forEach { series ->
    println("${series.id}: ${series.title}")
    println("  Latest: ${series.observations.last().value}")
}

fred.close()
```

#### Null 값 처리

```kotlin
val series = fred.series("UNRATE")

// Null 값 필터링
val validObservations = series.observations.filter { it.value != null }

// Null 값 개수 확인
val nullCount = series.observations.count { it.value == null }
println("Missing data points: $nullCount")

// Null-safe 평균 계산
val average = series.observations
    .mapNotNull { it.value }
    .average()
```

### B. 일반적인 사용 패턴

#### 패턴 1: 최신 경제 지표 대시보드

```kotlin
data class EconomicIndicator(
    val name: String,
    val value: Double?,
    val date: LocalDate,
    val unit: String
)

suspend fun fetchLatestIndicators(fred: FredClient): List<EconomicIndicator> {
    val indicators = listOf(
        "GDP" to "GDP",
        "UNRATE" to "Unemployment Rate",
        "CPIAUCSL" to "Inflation (CPI)",
        "FEDFUNDS" to "Fed Funds Rate"
    )

    return indicators.map { (id, name) ->
        val series = fred.series(id)
        val latest = series.observations.last()

        EconomicIndicator(
            name = name,
            value = latest.value,
            date = latest.date,
            unit = series.units
        )
    }
}
```

#### 패턴 2: 시계열 추세 분석

```kotlin
suspend fun analyzeUnemploymentTrend(fred: FredClient) {
    val oneYearAgo = LocalDate.now().minusYears(1)

    val unrate = fred.series(
        seriesId = "UNRATE",
        startDate = oneYearAgo
    )

    val values = unrate.observations.mapNotNull { it.value }

    val start = values.first()
    val end = values.last()
    val change = end - start

    println("Unemployment Rate (1 year)")
    println("  Start: $start%")
    println("  End: $end%")
    println("  Change: ${if (change > 0) "+" else ""}$change%")
}
```

#### 패턴 3: 여러 지표 비교

```kotlin
suspend fun compareGrowthRates(fred: FredClient) {
    val gdp = fred.series("GDP")
    val m2 = fred.series("M2SL")

    // YoY 성장률 계산
    fun calculateYoYGrowth(series: FredSeries): Double? {
        val observations = series.observations.filter { it.value != null }
        if (observations.size < 2) return null

        val latest = observations.last().value!!
        val yearAgo = observations[observations.size - 5].value!!  // 4 quarters ago

        return ((latest - yearAgo) / yearAgo) * 100.0
    }

    val gdpGrowth = calculateYoYGrowth(gdp)
    val m2Growth = calculateYoYGrowth(m2)

    println("YoY Growth Rates:")
    println("  GDP: ${gdpGrowth?.let { "%.2f%%".format(it) } ?: "N/A"}")
    println("  M2 Money Supply: ${m2Growth?.let { "%.2f%%".format(it) } ?: "N/A"}")
}
```
