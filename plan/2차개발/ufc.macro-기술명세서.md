# UFC Macro 서비스 기술명세서

## 문서 정보
- **버전**: 2.0.0
- **최종 수정일**: 2025-12-03
- **작성자**: Claude Code (Software Architect)
- **문서 성격**: 설계 중심 명세서
- **관련 문서**:
  - 1차개발 FRED 명세서: `/plan/1차개발/06-fred-macro-indicators.md`
  - 아키텍처 설계: `/plan/1차개발/01-architecture-design.md`
  - Python fredapi: https://github.com/mortada/fredapi
  - FRED API 공식 문서: https://fred.stlouisfed.org/docs/api/fred/

---

## 목차

1. [개요 및 범위](#1-개요-및-범위)
2. [FRED API 분석](#2-fred-api-분석)
3. [아키텍처 설계](#3-아키텍처-설계)
4. [데이터 모델 설계](#4-데이터-모델-설계)
5. [서비스 인터페이스](#5-서비스-인터페이스)
6. [API 명세](#6-api-명세)
7. [데이터 처리 흐름](#7-데이터-처리-흐름)
8. [캐싱 전략](#8-캐싱-전략)
9. [에러 처리](#9-에러-처리)
10. [테스트 전략](#10-테스트-전략)
11. [파일 구조](#11-파일-구조)

---

## 1. 개요 및 범위

### 1.1 목적

`ufc.macro` 서비스는 FRED (Federal Reserve Economic Data) API를 통해 미국 및 글로벌 거시경제 지표를 제공하는 도메인 서비스입니다.

**핵심 목표:**
- Python fredapi 라이브러리와 동등한 기능 제공
- 50+ 주요 경제 지표의 타입 안전한 접근
- Vintage Data (데이터 개정 이력) 지원
- 지표별 최적화된 캐싱 전략

### 1.2 FRED API 개요

**FRED (Federal Reserve Economic Data)**
- 세인트루이스 연방준비은행 운영
- 800,000+ 시계열 경제 데이터
- 무료 API 제공 (API Key 필요)
- Rate Limit: 120 calls/minute

**API 특징:**
- Realtime vs Vintage Data 구분
- 데이터 변환 기능 (증감률, 로그, 차분)
- 주기 변환 (일일→월간, 월간→분기 등)
- 검색 및 카테고리 기반 탐색

### 1.3 제공 지표 분류 (7개 카테고리, 50+ 지표)

#### A. 금리 관련 (Interest Rates)
- Federal Funds Rate (연방기금금리)
- Treasury Yields (1M, 3M, 6M, 1Y, 2Y, 5Y, 10Y, 30Y)
- Prime Lending Rate, Mortgage Rates

#### B. 인플레이션 (Inflation)
- CPI (소비자물가지수) - 전체 / 핵심
- PPI (생산자물가지수)
- PCE (개인소비지출) - 전체 / 핵심

#### C. 실업률 (Unemployment)
- Unemployment Rate (실업률)
- Initial/Continuing Jobless Claims
- Nonfarm Payrolls, Labor Force Participation

#### D. GDP 및 경제성장 (Economic Growth)
- Real/Nominal GDP
- GDP Growth Rate, Potential GDP
- Industrial Production Index

#### E. 주택 시장 (Housing Market)
- Housing Starts, New/Existing Home Sales
- Case-Shiller Home Price Index
- Homeownership Rate

#### F. 소비자 신뢰도 (Consumer Sentiment)
- University of Michigan Consumer Sentiment
- ISM Manufacturing/Services PMI
- Retail Sales

#### G. 통화 및 금융 (Money & Finance)
- M1/M2 Money Stock
- Trade Weighted Dollar Index
- Total Debt

### 1.4 주요 특징

1. **Python fredapi 호환성**
   - 동일한 메서드 시그니처 제공
   - 동일한 데이터 구조 반환

2. **타입 안정성**
   - 각 지표별 강타입 데이터 클래스
   - Enum 기반 파라미터 (Units, Frequency, AggregationMethod)

3. **Vintage Data 지원**
   - 첫 발표 데이터 조회 (getSeriesFirstRelease)
   - 전체 개정 이력 조회 (getSeriesAllReleases)
   - 특정 시점 데이터 조회 (getSeriesAsOfDate)

4. **데이터 변환**
   - 9가지 Units 변환 (원본값, 변화량, 변화율, 로그 등)
   - 7가지 Frequency 변환 (일일, 주간, 월간, 분기, 연간)
   - 3가지 Aggregation 방법 (평균, 합계, 기간 말)

5. **지표별 서비스**
   - GDPService, UnemploymentService 등 7개 도메인 서비스
   - 각 서비스는 해당 지표에 특화된 메서드 제공

---

## 2. FRED API 분석

### 2.1 API 인증

**API Key 발급:**
1. https://fred.stlouisfed.org/docs/api/api_key.html 방문
2. 무료 계정 생성 (이메일 인증)
3. API Key 발급 (32자리 문자열)

**API Key 설정:**
```properties
# local.properties (Git 제외)
FRED_API_KEY=abcd1234efgh5678ijkl9012mnop3456
```

### 2.2 주요 엔드포인트

#### A. Series Observations (시계열 데이터)
```
GET https://api.stlouisfed.org/fred/series/observations
```

**필수 파라미터:**
- series_id: 시리즈 ID (예: "GDPC1", "UNRATE")
- api_key: API Key
- file_type: 응답 형식 ("json")

**선택 파라미터:**
- observation_start/end: 관찰 기간
- realtime_start/end: 실시간 기간 (Vintage 조회)
- units: 단위 변환 (lin, chg, pch, pc1, log 등)
- frequency: 주기 변환 (d, w, m, q, a)
- aggregation_method: 집계 방법 (avg, sum, eop)
- sort_order: 정렬 순서 (asc, desc)
- limit/offset: 페이징

#### B. Series (시리즈 메타데이터)
```
GET https://api.stlouisfed.org/fred/series
```

**반환 정보:**
- 시리즈 제목, 주기, 단위
- 관찰 기간 (시작일, 종료일)
- 계절 조정 여부
- 인기도, 마지막 업데이트 시간

#### C. Series Search (전문 검색)
```
GET https://api.stlouisfed.org/fred/series/search
```

**검색 옵션:**
- search_text: 검색어
- order_by: 정렬 기준 (search_rank, popularity 등)
- filter_variable/value: 필터링

#### D. Series Vintage Dates (개정 이력 날짜)
```
GET https://api.stlouisfed.org/fred/series/vintagedates
```

### 2.3 Rate Limiting

**제한사항:**
- 분당 120 요청 (120 calls/minute)
- 초과 시 HTTP 429 Too Many Requests

**UFC 대응 전략:**
- TokenBucketRateLimiter 사용
- 분당 100 요청으로 제한 (안전 마진 20%)
- 자동 대기 및 재시도 (Exponential Backoff)

### 2.4 데이터 업데이트 주기

| 지표 유형 | 업데이트 주기 | 예시 | 캐시 TTL |
|----------|-------------|------|---------|
| 일일 | 매일 오후 | Federal Funds Rate | 1시간 |
| 주간 | 매주 목요일 | Jobless Claims | 1일 |
| 월간 | 매월 중순 | CPI, Unemployment | 1일 |
| 분기별 | 분기 말 후 1개월 | GDP | 7일 |
| 연간 | 연말 후 3개월 | Annual GDP | 30일 |

### 2.5 Units (단위 변환)

| Units 값 | 설명 | 수식 | 사용 예 |
|---------|------|------|--------|
| lin | 원본값 (Levels) | - | GDP 절대값 |
| chg | 변화량 (Change) | v(t) - v(t-1) | 실업자 수 증감 |
| ch1 | 전년 대비 변화량 | v(t) - v(t-12) | 연간 GDP 증가분 |
| pch | 변화율 (%) | 100 * chg / v(t-1) | 월간 CPI 상승률 |
| pc1 | 전년 대비 변화율 (%) | 100 * ch1 / v(t-12) | YoY 인플레이션 |
| pca | 복리 연율 (%) | - | 연환산 성장률 |
| cch | 연속 복리 변화율 | - | 금융 모델링 |
| cca | 연속 복리 연율 | - | 금융 모델링 |
| log | 자연로그 | ln(v(t)) | 로그 변환 분석 |

### 2.6 Frequency (주기 변환)

| Frequency | 설명 | 집계 방법 | 사용 예 |
|----------|------|---------|--------|
| d | Daily (일일) | - | 금리 데이터 |
| w | Weekly (주간) | avg, sum, eop | 실업수당청구 |
| bw | Biweekly (격주) | avg, sum, eop | - |
| m | Monthly (월간) | avg, sum, eop | CPI, 실업률 |
| q | Quarterly (분기) | avg, sum, eop | GDP |
| sa | Semiannual (반년) | avg, sum, eop | - |
| a | Annual (연간) | avg, sum, eop | 연간 GDP |

### 2.7 Aggregation Method (집계 방법)

| Method | 설명 | 사용 예 |
|--------|------|--------|
| avg | 평균 (Average) | 일일 금리 → 월간 평균 금리 |
| sum | 합계 (Sum) | 월간 판매량 → 분기 총판매량 |
| eop | 기간 말 값 (End of Period) | 월말 지수 값 |

---

## 3. 아키텍처 설계

### 3.1 Multi-Source 아키텍처 통합

```
┌────────────────────────────────────────────────────────┐
│                    UFCClient (Facade)                   │
│  ┌──────────┬──────────┬──────────┬──────────┐        │
│  │  stock   │   etf    │  search  │  macro   │        │
│  └──────────┴──────────┴──────────┴──────────┘        │
└────────────────────────────────────────────────────────┘
                            │
                            │ ufc.macro (이 문서)
                            ↓
┌────────────────────────────────────────────────────────┐
│                  MacroApi (Public)                      │
│  - getSeries(seriesId, ...): Series                    │
│  - getSeriesInfo(seriesId): SeriesInfo                 │
│  - search(text, ...): List<SeriesInfo>                 │
│  - getGDP(): GDPService                                │
│  - getUnemployment(): UnemploymentService              │
│  - getInflation(): InflationService                    │
│  - getInterestRates(): InterestRatesService            │
└────────────────────────────────────────────────────────┘
                            │
                            ↓
┌────────────────────────────────────────────────────────┐
│            MacroApiImpl (Internal Domain)               │
│  - fredSource: FREDSource                              │
│  - cacheManager: MacroCacheManager                     │
│  지표별 서비스 팩토리 (lazy initialization):            │
│  - gdpService, unemploymentService, ...                │
└────────────────────────────────────────────────────────┘
                            │
                            ↓
┌────────────────────────────────────────────────────────┐
│              FREDSource (Source Layer)                  │
│  - httpClient: HttpClient (Ktor)                       │
│  - rateLimiter: TokenBucketRateLimiter (100/min)       │
│  - apiKey: String                                       │
│                                                         │
│  Methods:                                               │
│  - fetchSeriesObservations(...): Response              │
│  - fetchSeriesInfo(...): Response                      │
│  - fetchSeriesSearch(...): Response                    │
└────────────────────────────────────────────────────────┘
                            │
                            ↓
┌────────────────────────────────────────────────────────┐
│                   FRED REST API                         │
│  https://api.stlouisfed.org/fred/                      │
└────────────────────────────────────────────────────────┘
```

### 3.2 계층별 책임 (Layer Responsibilities)

| 계층 | 책임 | 데이터 형식 |
|------|------|-----------|
| **Public API** | 도메인 메서드 제공, 타입 변환 | Domain Models (Series, SeriesInfo) |
| **Internal Domain** | 비즈니스 로직, 캐시 조회/저장 | Domain Models |
| **Source Layer** | HTTP 요청, 응답 파싱, Rate Limiting | Response Models (Raw JSON) |
| **Infrastructure** | HTTP Client, 직렬화, 에러 매핑 | HTTP Response |

### 3.3 지표별 서비스 아키텍처

```
                  MacroApi
                      │
        ┌─────────────┼─────────────┐
        │             │             │
    GDPService  UnemploymentService ...
        │             │
        └─────────────┴─────────────┐
                      │
                  FREDSource
                      │
              (공통 FRED API 호출)
```

**설계 원칙:**
- 각 지표별 서비스는 독립적인 인터페이스
- FREDSource를 공유하여 중복 제거
- 지표별 캐싱 정책 적용
- 지표 특화 메서드 제공 (예: GDP.growthRate(), Unemployment.current())

### 3.4 Vintage Data 처리 구조

```
┌─────────────────────────────────────────────────────┐
│              Vintage Data Timeline                   │
│                                                      │
│  Q4 2023 GDP:                                       │
│  2024-01-30 → 17102.5 (첫 발표, First Release)     │
│  2024-02-28 → 17080.7 (1차 개정, 1st Revision)     │
│  2024-03-27 → 17089.6 (2차 개정, 2nd Revision)     │
│                                                      │
│  Methods:                                            │
│  - getSeriesFirstRelease() → 17102.5                │
│  - getSeriesAllReleases() → [17102.5, 17080.7, ...] │
│  - getSeriesAsOfDate(2024-02-15) → 17102.5          │
└─────────────────────────────────────────────────────┘
```

**Vintage Data 용도:**
- 백테스팅: 과거 시점의 정보로 투자 전략 검증
- 개정 영향 분석: GDP 개정이 시장에 미친 영향 연구
- 예측 모델 훈련: 첫 발표값으로 모델 학습

---

## 4. 데이터 모델 설계

### 4.1 Domain Models (Public)

사용자에게 반환되는 도메인 모델입니다.

#### A. Series (시계열 데이터)

```kotlin
package com.ulalax.ufc.model.macro

data class Series(
    val id: String,
    val title: String,
    val observations: List<Observation>
)
```

**필드 설명:**
- id: FRED 시리즈 ID (예: "GDPC1", "UNRATE")
- title: 시리즈 제목 (예: "Real Gross Domestic Product")
- observations: 관찰값 목록

#### B. Observation (관찰값)

```kotlin
data class Observation(
    val date: LocalDate,
    val value: Double?
)
```

**필드 설명:**
- date: 관찰 날짜
- value: 값 (null인 경우 데이터 없음, FRED는 "."로 표현)

#### C. SeriesInfo (시리즈 메타데이터)

```kotlin
data class SeriesInfo(
    val id: String,
    val title: String,
    val observationStart: String,
    val observationEnd: String,
    val frequency: String,
    val frequencyShort: String,
    val units: String,
    val unitsShort: String,
    val seasonalAdjustment: String,
    val seasonalAdjustmentShort: String,
    val lastUpdated: String,
    val popularity: Int,
    val notes: String?
)
```

**필드 설명:**
- frequency: "Quarterly", "Monthly", "Daily" 등
- frequencyShort: "Q", "M", "D" 등
- seasonalAdjustment: "Seasonally Adjusted Annual Rate" 등
- popularity: 인기도 (0-100)

#### D. VintageObservation (개정 이력 포함 관찰값)

```kotlin
data class VintageObservation(
    val date: LocalDate,
    val realtimeStart: LocalDate,
    val value: Double?
)
```

**필드 설명:**
- date: 실제 관찰 날짜 (예: 2023-12-31, Q4 2023)
- realtimeStart: 발표 날짜 (예: 2024-01-30)
- value: 해당 발표일 기준 값

### 4.2 Response Models (Internal)

FRED API 원시 응답을 파싱하기 위한 내부 모델입니다.

#### A. FREDSeriesObservationsResponse

```kotlin
package com.ulalax.ufc.internal.fred.model

@Serializable
data class FREDSeriesObservationsResponse(
    @SerialName("realtime_start") val realtimeStart: String,
    @SerialName("realtime_end") val realtimeEnd: String,
    @SerialName("observation_start") val observationStart: String,
    @SerialName("observation_end") val observationEnd: String,
    @SerialName("units") val units: String,
    @SerialName("count") val count: Int,
    @SerialName("observations") val observations: List<FREDObservation>
)

@Serializable
data class FREDObservation(
    @SerialName("date") val date: String,
    @SerialName("value") val value: String,  // "." = 데이터 없음
    @SerialName("realtime_start") val realtimeStart: String? = null,
    @SerialName("realtime_end") val realtimeEnd: String? = null
)
```

#### B. FREDSeriesInfoResponse

```kotlin
@Serializable
data class FREDSeriesInfoResponse(
    @SerialName("seriess") val seriess: List<FREDSeriesInfoItem>
)

@Serializable
data class FREDSeriesInfoItem(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("observation_start") val observationStart: String,
    @SerialName("observation_end") val observationEnd: String,
    @SerialName("frequency") val frequency: String,
    @SerialName("frequency_short") val frequencyShort: String,
    @SerialName("units") val units: String,
    @SerialName("units_short") val unitsShort: String,
    @SerialName("seasonal_adjustment") val seasonalAdjustment: String,
    @SerialName("seasonal_adjustment_short") val seasonalAdjustmentShort: String,
    @SerialName("last_updated") val lastUpdated: String,
    @SerialName("popularity") val popularity: Int,
    @SerialName("notes") val notes: String? = null
)
```

### 4.3 Enum Types

#### A. Units (단위 변환)

```kotlin
package com.ulalax.ufc.model.common

enum class Units(val value: String, val description: String) {
    LEVELS("lin", "Levels (원본값)"),
    CHANGE("chg", "Change (변화량)"),
    CHANGE_FROM_YEAR_AGO("ch1", "Change from Year Ago (전년 대비 변화량)"),
    PERCENT_CHANGE("pch", "Percent Change (변화율 %)"),
    PERCENT_CHANGE_FROM_YEAR_AGO("pc1", "Percent Change from Year Ago (전년 대비 변화율 %)"),
    COMPOUNDED_ANNUAL_RATE("pca", "Compounded Annual Rate of Change (복리 연율)"),
    CONTINUOUSLY_COMPOUNDED_RATE("cch", "Continuously Compounded Rate of Change (연속 복리 변화율)"),
    CONTINUOUSLY_COMPOUNDED_ANNUAL_RATE("cca", "Continuously Compounded Annual Rate of Change (연속 복리 연율)"),
    NATURAL_LOG("log", "Natural Log (자연로그)")
}
```

#### B. DataFrequency (주기)

```kotlin
enum class DataFrequency(val value: String, val description: String) {
    DAILY("d", "Daily (일일)"),
    WEEKLY("w", "Weekly (주간)"),
    BIWEEKLY("bw", "Biweekly (격주)"),
    MONTHLY("m", "Monthly (월간)"),
    QUARTERLY("q", "Quarterly (분기별)"),
    SEMIANNUAL("sa", "Semiannual (반년)"),
    ANNUAL("a", "Annual (연간)")
}
```

#### C. AggregationMethod (집계 방법)

```kotlin
enum class AggregationMethod(val value: String, val description: String) {
    AVERAGE("avg", "Average (평균)"),
    SUM("sum", "Sum (합계)"),
    END_OF_PERIOD("eop", "End of Period (기간 말)")
}
```

#### D. SearchOrderBy (검색 정렬 기준)

```kotlin
enum class SearchOrderBy(val value: String) {
    SEARCH_RANK("search_rank"),
    SERIES_ID("series_id"),
    TITLE("title"),
    UNITS("units"),
    FREQUENCY("frequency"),
    SEASONAL_ADJUSTMENT("seasonal_adjustment"),
    REALTIME_START("realtime_start"),
    REALTIME_END("realtime_end"),
    LAST_UPDATED("last_updated"),
    OBSERVATION_START("observation_start"),
    OBSERVATION_END("observation_end"),
    POPULARITY("popularity")
}
```

#### E. SortOrder (정렬 순서)

```kotlin
enum class SortOrder(val value: String) {
    ASC("asc"),
    DESC("desc")
}
```

### 4.4 지표별 Data Models

#### A. GDP Data

```kotlin
package com.ulalax.ufc.model.macro

data class GDPData(
    val date: LocalDate,
    val value: Double,
    val unit: String = "Billions of Dollars",
    val seasonallyAdjusted: Boolean = true,
    val frequency: String = "Quarterly"
)

data class GDPGrowthData(
    val date: LocalDate,
    val growthRate: Double,
    val unit: String = "Percent"
)
```

#### B. Unemployment Data

```kotlin
data class UnemploymentData(
    val date: LocalDate,
    val rate: Double,
    val laborForce: Long? = null,
    val employed: Long? = null,
    val unemployed: Long? = null
)

data class JoblessClaimsData(
    val date: LocalDate,
    val initialClaims: Long? = null,
    val continuingClaims: Long? = null
)
```

#### C. Inflation Data

```kotlin
data class CPIData(
    val date: LocalDate,
    val index: Double,
    val monthOverMonth: Double? = null,
    val yearOverYear: Double? = null
)

data class PCEData(
    val date: LocalDate,
    val index: Double,
    val monthOverMonth: Double? = null,
    val yearOverYear: Double? = null
)
```

#### D. Interest Rate Data

```kotlin
data class InterestRateData(
    val date: LocalDate,
    val rate: Double,
    val term: String
)

data class YieldCurve(
    val date: LocalDate,
    val yields: Map<String, Double>
)
```

### 4.5 주요 지표 Series ID 카탈로그

#### GDP 관련

| 지표명 | Series ID | 주기 | 단위 |
|-------|----------|------|------|
| Real GDP | GDPC1 | Quarterly | Billions of Chained 2017 $ |
| Nominal GDP | GDP | Quarterly | Billions of $ |
| Potential GDP | GDPPOT | Quarterly | Billions of Chained 2017 $ |
| GDP Deflator | GDPDEF | Quarterly | Index 2017=100 |

#### 실업률 관련

| 지표명 | Series ID | 주기 | 단위 |
|-------|----------|------|------|
| Unemployment Rate | UNRATE | Monthly | Percent |
| Initial Jobless Claims | ICSA | Weekly | Thousands |
| Continuing Claims | CCSA | Weekly | Thousands |
| Nonfarm Payrolls | PAYEMS | Monthly | Thousands of Persons |
| Labor Force Participation | CIVPART | Monthly | Percent |

#### 인플레이션 관련

| 지표명 | Series ID | 주기 | 단위 |
|-------|----------|------|------|
| CPI All Items | CPIAUCSL | Monthly | Index 1982-84=100 |
| CPI Core | CPILFESL | Monthly | Index 1982-84=100 |
| PPI | PPIACO | Monthly | Index 1982=100 |
| PCE | PCEPI | Monthly | Index 2017=100 |
| PCE Core | PCEPILFE | Monthly | Index 2017=100 |

#### 금리 관련

| 지표명 | Series ID | 주기 | 단위 |
|-------|----------|------|------|
| Federal Funds Rate | DFF | Daily | Percent |
| Prime Rate | DPRIME | Daily | Percent |
| 30-Year Mortgage | MORTGAGE30US | Weekly | Percent |
| 1-Month Treasury | DGS1MO | Daily | Percent |
| 3-Month Treasury | DGS3MO | Daily | Percent |
| 10-Year Treasury | DGS10 | Daily | Percent |
| 30-Year Treasury | DGS30 | Daily | Percent |

---

## 5. 서비스 인터페이스

### 5.1 MacroApi (Public Interface)

```kotlin
package com.ulalax.ufc.api

interface MacroApi {

    // 범용 시계열 조회
    suspend fun getSeries(
        seriesId: String,
        observationStart: LocalDate? = null,
        observationEnd: LocalDate? = null,
        frequency: DataFrequency? = null,
        aggregationMethod: AggregationMethod? = null,
        units: Units? = null
    ): Series

    // 시리즈 메타데이터
    suspend fun getSeriesInfo(seriesId: String): SeriesInfo

    // Vintage Data
    suspend fun getSeriesFirstRelease(seriesId: String): Series
    suspend fun getSeriesAllReleases(
        seriesId: String,
        realtimeStart: LocalDate? = null,
        realtimeEnd: LocalDate? = null
    ): List<VintageObservation>
    suspend fun getSeriesAsOfDate(
        seriesId: String,
        asOfDate: LocalDate
    ): Series
    suspend fun getSeriesVintageDates(seriesId: String): List<LocalDate>

    // 검색
    suspend fun search(
        text: String,
        limit: Int = 1000,
        orderBy: SearchOrderBy? = null,
        sortOrder: SortOrder? = null
    ): List<SeriesInfo>

    // 지표별 서비스 팩토리
    fun getGDP(): GDPService
    fun getUnemployment(): UnemploymentService
    fun getInflation(): InflationService
    fun getInterestRates(): InterestRatesService
    fun getHousing(): HousingService
    fun getConsumerSentiment(): ConsumerSentimentService
    fun getMoneyAndFinance(): MoneyAndFinanceService
}
```

### 5.2 지표별 서비스 인터페이스

#### A. GDPService

```kotlin
package com.ulalax.ufc.api

interface GDPService {

    suspend fun quarterly(
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<GDPData>

    suspend fun annual(
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<GDPData>

    suspend fun growthRate(
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<GDPGrowthData>

    suspend fun current(): GDPData

    suspend fun potential(
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<GDPData>
}
```

**Series ID 매핑:**
- quarterly(): GDPC1 (Real GDP)
- annual(): GDP (Nominal GDP)
- potential(): GDPPOT (Potential GDP)
- growthRate(): GDPC1 with units=pc1

#### B. UnemploymentService

```kotlin
interface UnemploymentService {

    suspend fun rate(
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<UnemploymentData>

    suspend fun current(): UnemploymentData

    suspend fun joblessClaims(
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<JoblessClaimsData>

    suspend fun nonfarmPayrolls(
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<Observation>

    suspend fun laborForceParticipation(
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<Observation>
}
```

**Series ID 매핑:**
- rate(): UNRATE
- joblessClaims(): ICSA (Initial), CCSA (Continuing)
- nonfarmPayrolls(): PAYEMS
- laborForceParticipation(): CIVPART

#### C. InflationService

```kotlin
interface InflationService {

    suspend fun cpi(
        start: LocalDate? = null,
        end: LocalDate? = null,
        core: Boolean = false
    ): List<CPIData>

    suspend fun pce(
        start: LocalDate? = null,
        end: LocalDate? = null,
        core: Boolean = false
    ): List<PCEData>

    suspend fun ppi(
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<Observation>

    suspend fun current(): CPIData
}
```

**Series ID 매핑:**
- cpi(core=false): CPIAUCSL
- cpi(core=true): CPILFESL
- pce(core=false): PCEPI
- pce(core=true): PCEPILFE
- ppi(): PPIACO

#### D. InterestRatesService

```kotlin
interface InterestRatesService {

    suspend fun federalFundsRate(
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<InterestRateData>

    suspend fun treasuryYield(
        term: TreasuryTerm,
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<InterestRateData>

    suspend fun yieldCurve(date: LocalDate): YieldCurve

    suspend fun mortgage30Year(
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<InterestRateData>
}

enum class TreasuryTerm(val seriesId: String, val displayName: String) {
    ONE_MONTH("DGS1MO", "1-Month"),
    THREE_MONTHS("DGS3MO", "3-Month"),
    SIX_MONTHS("DGS6MO", "6-Month"),
    ONE_YEAR("DGS1", "1-Year"),
    TWO_YEARS("DGS2", "2-Year"),
    FIVE_YEARS("DGS5", "5-Year"),
    TEN_YEARS("DGS10", "10-Year"),
    THIRTY_YEARS("DGS30", "30-Year")
}
```

### 5.3 FREDSource (Source Layer Interface)

```kotlin
package com.ulalax.ufc.internal.fred

interface FREDSource {

    suspend fun fetchSeriesObservations(
        seriesId: String,
        observationStart: LocalDate? = null,
        observationEnd: LocalDate? = null,
        realtimeStart: LocalDate? = null,
        realtimeEnd: LocalDate? = null,
        limit: Int? = null,
        offset: Int? = null,
        sortOrder: SortOrder? = null,
        units: Units? = null,
        frequency: DataFrequency? = null,
        aggregationMethod: AggregationMethod? = null
    ): FREDSeriesObservationsResponse

    suspend fun fetchSeriesInfo(seriesId: String): FREDSeriesInfoResponse

    suspend fun fetchSeriesSearch(
        searchText: String,
        limit: Int? = null,
        orderBy: SearchOrderBy? = null,
        sortOrder: SortOrder? = null
    ): FREDSeriesSearchResponse

    suspend fun fetchSeriesVintageDates(seriesId: String): FREDVintageDatesResponse
}
```

---

## 6. API 명세

### 6.1 getSeries - 시계열 데이터 조회

**메서드 시그니처:**
```kotlin
suspend fun getSeries(
    seriesId: String,
    observationStart: LocalDate? = null,
    observationEnd: LocalDate? = null,
    frequency: DataFrequency? = null,
    aggregationMethod: AggregationMethod? = null,
    units: Units? = null
): Series
```

**파라미터:**
- seriesId: FRED 시리즈 ID (필수)
- observationStart: 관찰 시작일 (선택)
- observationEnd: 관찰 종료일 (선택)
- frequency: 주기 변환 (선택)
- aggregationMethod: 집계 방법 (선택, frequency 사용 시 필수)
- units: 단위 변환 (선택)

**반환값:**
- Series 객체 (id, title, observations)

**예외:**
- UFCException (ErrorCode.NOT_FOUND): 시리즈 ID가 존재하지 않음
- UFCException (ErrorCode.RATE_LIMITED): API Rate Limit 초과
- UFCException (ErrorCode.AUTH_FAILED): API Key 인증 실패
- UFCException (ErrorCode.INVALID_PARAMETER): 잘못된 파라미터

**Python fredapi 매핑:**
```python
fred.get_series('GDPC1', observation_start='2020-01-01', observation_end='2024-01-01')
```

### 6.2 getSeriesInfo - 시리즈 메타데이터

**메서드 시그니처:**
```kotlin
suspend fun getSeriesInfo(seriesId: String): SeriesInfo
```

**파라미터:**
- seriesId: FRED 시리즈 ID

**반환값:**
- SeriesInfo 객체 (id, title, frequency, units, popularity 등)

**Python fredapi 매핑:**
```python
fred.get_series_info('GDPC1')
```

### 6.3 getSeriesFirstRelease - 첫 발표 데이터

**메서드 시그니처:**
```kotlin
suspend fun getSeriesFirstRelease(seriesId: String): Series
```

**목적:**
- 각 관찰값의 첫 발표 데이터만 반환 (개정 제외)
- 백테스팅에 사용 (당시 알려진 정보만 활용)

**처리 로직:**
1. getSeriesAllReleases() 호출하여 모든 개정 이력 조회
2. 각 date별로 가장 빠른 realtimeStart를 가진 값만 선택
3. Series 객체로 변환하여 반환

**Python fredapi 매핑:**
```python
fred.get_series_first_release('GDPC1')
```

### 6.4 getSeriesAllReleases - 전체 개정 이력

**메서드 시그니처:**
```kotlin
suspend fun getSeriesAllReleases(
    seriesId: String,
    realtimeStart: LocalDate? = null,
    realtimeEnd: LocalDate? = null
): List<VintageObservation>
```

**파라미터:**
- seriesId: FRED 시리즈 ID
- realtimeStart: 실시간 시작일 (발표일 필터링)
- realtimeEnd: 실시간 종료일

**반환값:**
- VintageObservation 리스트 (date, realtimeStart, value)

**Python fredapi 매핑:**
```python
fred.get_series_all_releases('GDPC1')
```

### 6.5 getSeriesAsOfDate - 특정 시점 데이터

**메서드 시그니처:**
```kotlin
suspend fun getSeriesAsOfDate(
    seriesId: String,
    asOfDate: LocalDate
): Series
```

**목적:**
- 특정 날짜 기준으로 당시 알려진 최신 데이터 조회
- 백테스팅: 2020년 3월 1일 시점에서 알 수 있었던 GDP 데이터

**처리 로직:**
1. getSeriesAllReleases() 호출
2. realtimeStart <= asOfDate인 데이터만 필터링
3. 각 date별로 가장 최근 realtimeStart를 가진 값 선택
4. Series 객체로 변환

**Python fredapi 매핑:**
```python
fred.get_series_as_of_date('GDPC1', '2020-03-01')
```

### 6.6 search - 시리즈 검색

**메서드 시그니처:**
```kotlin
suspend fun search(
    text: String,
    limit: Int = 1000,
    orderBy: SearchOrderBy? = null,
    sortOrder: SortOrder? = null
): List<SeriesInfo>
```

**파라미터:**
- text: 검색어 (시리즈 제목, 설명에서 전문 검색)
- limit: 결과 개수 제한 (기본값: 1000)
- orderBy: 정렬 기준 (기본값: search_rank)
- sortOrder: 정렬 순서 (기본값: desc)

**반환값:**
- SeriesInfo 리스트

**Python fredapi 매핑:**
```python
fred.search('unemployment rate', limit=10)
```

---

## 7. 데이터 처리 흐름

### 7.1 기본 시계열 조회 흐름

```
User Request
    │
    ↓
MacroApi.getSeries(seriesId, ...)
    │
    ├─→ Cache 조회
    │   ├─ Hit → Domain Model 반환
    │   └─ Miss ↓
    │
    ↓
FREDSource.fetchSeriesObservations(...)
    │
    ├─→ Rate Limiter 확인 (100/min)
    │   └─ 대기 필요 시 Exponential Backoff
    │
    ├─→ HTTP Request to FRED API
    │   GET /fred/series/observations?series_id=...
    │
    ├─→ HTTP Response (JSON)
    │
    ├─→ Parse to FREDSeriesObservationsResponse
    │
    ├─→ Validate Response
    │   ├─ 성공 (200) ↓
    │   ├─ 404 → ErrorCode.NOT_FOUND
    │   ├─ 429 → ErrorCode.RATE_LIMITED
    │   └─ 401 → ErrorCode.AUTH_FAILED
    │
    ↓
Transform to Domain Model
    │
    ├─→ FREDObservation → Observation
    │   - date: String → LocalDate
    │   - value: String ("." → null, "123.45" → 123.45)
    │
    ├─→ Create Series(id, title, observations)
    │
    ├─→ Cache 저장 (지표별 TTL)
    │
    └─→ Return to User
```

### 7.2 Vintage Data 조회 흐름

```
User Request: getSeriesAsOfDate(seriesId, asOfDate)
    │
    ↓
getSeriesAllReleases(seriesId)
    │
    ├─→ FREDSource.fetchSeriesObservations(
    │       seriesId,
    │       realtimeStart = "1776-07-04",
    │       realtimeEnd = "9999-12-31"
    │   )
    │
    ├─→ Parse to List<VintageObservation>
    │
    ↓
Filter by asOfDate
    │
    ├─→ vintages.filter { it.realtimeStart <= asOfDate }
    │
    ├─→ Group by date
    │   └─ 각 date별로 가장 최근 realtimeStart 선택
    │
    ↓
Transform to Series
    │
    └─→ Return Series(id, title, observations)
```

**예시:**
```
Q4 2023 GDP 조회 (asOfDate = 2024-02-15)

All Releases:
- (date=2023-12-31, realtimeStart=2024-01-30, value=17102.5)
- (date=2023-12-31, realtimeStart=2024-02-28, value=17080.7)
- (date=2023-12-31, realtimeStart=2024-03-27, value=17089.6)

After Filter (realtimeStart <= 2024-02-15):
- (date=2023-12-31, realtimeStart=2024-01-30, value=17102.5)

Result: 17102.5 (2024년 2월 15일 시점의 최신값)
```

### 7.3 지표별 서비스 조회 흐름

```
User Request
    │
    ↓
MacroApi.getGDP()
    │
    ├─→ Lazy Initialization
    │   └─ GDPServiceImpl(fredSource)
    │
    ↓
GDPService.quarterly(start, end)
    │
    ├─→ getSeries(
    │       seriesId = "GDPC1",
    │       observationStart = start,
    │       observationEnd = end
    │   )
    │
    ├─→ Series.observations → List<GDPData>
    │   └─ Observation(date, value) → GDPData(
    │           date = date,
    │           value = value,
    │           unit = "Billions of Dollars",
    │           seasonallyAdjusted = true,
    │           frequency = "Quarterly"
    │       )
    │
    └─→ Return List<GDPData>
```

### 7.4 Units 변환 처리

```
User Request: getSeries("GDPC1", units = Units.PERCENT_CHANGE)
    │
    ↓
FREDSource.fetchSeriesObservations(
    seriesId = "GDPC1",
    units = "pch"  ← Enum.value 사용
)
    │
    ├─→ FRED API가 서버에서 계산하여 반환
    │   GET /fred/series/observations
    │   ?series_id=GDPC1&units=pch
    │
    └─→ Response에 이미 변환된 값 포함
        observations: [
            { date: "2024-01-01", value: "2.5" },  ← % Change
            { date: "2024-04-01", value: "3.1" }
        ]
```

**참고:**
- Units 변환은 클라이언트가 아닌 FRED API 서버에서 수행
- UFC는 units 파라미터만 전달

---

## 8. 캐싱 전략

### 8.1 지표별 캐시 TTL

| 지표 카테고리 | 업데이트 주기 | 캐시 TTL | 예시 |
|-------------|-------------|---------|------|
| 일일 지표 | 매일 오후 | 1시간 | Federal Funds Rate, Treasury Yields |
| 주간 지표 | 매주 목요일 | 1일 | Initial Jobless Claims |
| 월간 지표 | 매월 중순 | 1일 | CPI, Unemployment Rate, PCE |
| 분기별 지표 | 분기 말 후 1개월 | 7일 | GDP |
| 연간 지표 | 연말 후 3개월 | 30일 | Annual GDP |
| 메타데이터 | 거의 변경 없음 | 7일 | SeriesInfo |
| 검색 결과 | 자주 변경됨 | 1시간 | search() 결과 |

### 8.2 캐시 키 전략

**시계열 데이터 (getSeries):**
```
macro:series:{seriesId}:{observationStart}:{observationEnd}:{frequency}:{units}:{aggregationMethod}
```

**예시:**
```
macro:series:GDPC1:2020-01-01:2024-01-01:null:null:null
macro:series:UNRATE:2023-01-01:2024-01-01:null:pc1:null
```

**시리즈 메타데이터 (getSeriesInfo):**
```
macro:info:{seriesId}
```

**Vintage Data (getSeriesAllReleases):**
```
macro:vintage:{seriesId}:{realtimeStart}:{realtimeEnd}
```

**검색 결과 (search):**
```
macro:search:{text}:{limit}:{orderBy}:{sortOrder}
```

### 8.3 캐시 무효화 정책

**시간 기반 무효화 (TTL):**
- 모든 캐시는 TTL 기반 자동 만료

**수동 무효화 (필요 시):**
- 특정 시리즈의 캐시만 삭제하는 메서드 제공
- 전체 Macro 캐시 삭제 메서드 제공

**캐시 워밍 (선택적):**
- 주요 지표 (GDP, Unemployment, CPI 등)는 애플리케이션 시작 시 미리 로드
- 사용자 요청 시 빠른 응답 보장

### 8.4 MacroCacheManager 인터페이스

```kotlin
package com.ulalax.ufc.internal.macro

interface MacroCacheManager {

    suspend fun getSeries(key: String): Series?
    suspend fun putSeries(key: String, series: Series, ttl: Duration)

    suspend fun getSeriesInfo(key: String): SeriesInfo?
    suspend fun putSeriesInfo(key: String, info: SeriesInfo, ttl: Duration)

    suspend fun invalidate(key: String)
    suspend fun invalidateAll()
}
```

---

## 9. 에러 처리

### 9.1 FRED API 에러 코드 매핑

| HTTP Status | FRED 에러 | UFC ErrorCode | 설명 |
|------------|----------|--------------|------|
| 200 | - | - | 정상 |
| 400 | Bad Request | INVALID_PARAMETER | 잘못된 파라미터 (series_id, dates 등) |
| 401 | Unauthorized | AUTH_FAILED | API Key 인증 실패 |
| 404 | Not Found | NOT_FOUND | 시리즈 ID가 존재하지 않음 |
| 429 | Too Many Requests | RATE_LIMITED | Rate Limit 초과 (120/min) |
| 500 | Internal Server Error | SOURCE_ERROR | FRED 서버 오류 |
| 503 | Service Unavailable | SOURCE_UNAVAILABLE | FRED 서비스 일시 중단 |

### 9.2 에러 처리 전략

#### A. Rate Limiting (429)

**재시도 전략:**
```
1차 재시도: 1초 대기
2차 재시도: 2초 대기
3차 재시도: 4초 대기
4차 재시도: 8초 대기
5차 이상: 실패 (UFCException 발생)
```

**TokenBucketRateLimiter:**
- 분당 100 토큰 (안전 마진 20%)
- 토큰 고갈 시 자동 대기
- 대기 시간 계산 후 suspend

#### B. 인증 실패 (401)

**처리 방법:**
- API Key 재확인 안내 메시지
- local.properties 파일 확인 요청
- UFCException 발생 (재시도 불가)

#### C. 데이터 없음 (404)

**처리 방법:**
- 잘못된 series_id → UFCException (NOT_FOUND)
- 유효한 series_id이지만 데이터 없음 → 빈 observations 반환

#### D. 네트워크 에러

**처리 방법:**
- ConnectTimeoutException → UFCException (NETWORK_ERROR)
- SocketTimeoutException → UFCException (TIMEOUT)
- 최대 3회 재시도 (Exponential Backoff)

### 9.3 데이터 검증

**응답 검증 규칙:**
1. observations 배열 존재 확인
2. value="." 처리 (null로 변환)
3. value가 숫자가 아닌 경우 처리
4. date 형식 검증 (LocalDate 파싱 실패 시)

**검증 실패 시:**
- UFCException (PARSING_ERROR) 발생
- 원본 JSON을 metadata에 포함

---

## 10. 테스트 전략

### 10.1 테스트 분류

#### A. Live Test (src/liveTest/)
- 실제 FRED API 호출
- API Key 필요
- 응답 레코딩 (JSON 파일 저장)
- 주기: 월 1회 또는 API 변경 시

#### B. Unit Test (src/test/)
- 레코딩된 JSON 기반
- API Key 불필요
- 빠른 실행 (< 1초)
- 주기: 모든 커밋마다 실행

### 10.2 Live Test 시나리오

#### 기본 시계열 조회
```
Test: testGetGDPSeries
- getSeries("GDPC1", start=2020-01-01, end=2024-01-01)
- 검증:
  - observations.size > 0
  - 모든 value가 유효한 숫자
  - date가 start ~ end 범위 내
```

#### Vintage Data
```
Test: testGetSeriesAllReleases
- getSeriesAllReleases("GDPC1", realtimeStart=2024-01-01)
- 검증:
  - VintageObservation 리스트 반환
  - 동일 date에 여러 realtimeStart 존재
```

#### 지표별 서비스
```
Test: testUnemploymentService
- getUnemployment().current()
- 검증:
  - UnemploymentData 반환
  - rate 값이 0 ~ 100 범위
```

#### Units 변환
```
Test: testUnitsTransformation
- getSeries("GDPC1", units=Units.PERCENT_CHANGE)
- 검증:
  - 값이 % 형태 (소수)
  - 첫 번째 값은 null (변화율 계산 불가)
```

#### 검색
```
Test: testSearch
- search("unemployment rate", limit=10)
- 검증:
  - SeriesInfo 리스트 반환
  - "UNRATE" 포함
  - popularity 순 정렬
```

### 10.3 Unit Test 시나리오

#### Response Parsing
```
Test: testParseSeriesObservationsResponse
- Given: recorded JSON response
- When: parse to FREDSeriesObservationsResponse
- Then: 모든 필드 정상 파싱
```

#### Domain Model Transformation
```
Test: testTransformToSeries
- Given: FREDSeriesObservationsResponse
- When: transform to Series
- Then: observations 개수 일치, value 변환 정확
```

#### Error Handling
```
Test: testHandleNotFoundError
- Given: 404 response
- When: getSeries("INVALID_ID")
- Then: UFCException (ErrorCode.NOT_FOUND)
```

#### Cache Hit/Miss
```
Test: testCacheHit
- Given: cached series
- When: getSeries("GDPC1")
- Then: FREDSource 호출 없이 캐시에서 반환
```

### 10.4 테스트 데이터 레코딩

**레코딩 파일 경로:**
```
src/test/resources/recorded/macro/
├── getSeries_GDPC1_2020-01-01_2024-01-01.json
├── getSeriesInfo_UNRATE.json
├── search_unemployment_rate.json
└── getSeriesAllReleases_GDPC1.json
```

**레코딩 포맷:**
```json
{
  "request": {
    "method": "getSeries",
    "params": {
      "seriesId": "GDPC1",
      "observationStart": "2020-01-01",
      "observationEnd": "2024-01-01"
    }
  },
  "response": {
    "realtime_start": "2024-12-03",
    "realtime_end": "2024-12-03",
    "observations": [...]
  },
  "recordedAt": "2024-12-03T10:30:00Z"
}
```

---

## 11. 파일 구조

### 11.1 전체 디렉토리 구조

```
src/
├── main/kotlin/com/ulalax/ufc/
│   ├── api/
│   │   └── MacroApi.kt                        # Public Interface
│   │
│   ├── model/
│   │   ├── common/
│   │   │   ├── Units.kt                       # Enum
│   │   │   ├── DataFrequency.kt               # Enum
│   │   │   ├── AggregationMethod.kt           # Enum
│   │   │   ├── SearchOrderBy.kt               # Enum
│   │   │   └── SortOrder.kt                   # Enum
│   │   │
│   │   └── macro/
│   │       ├── Series.kt                      # Domain Model
│   │       ├── Observation.kt
│   │       ├── SeriesInfo.kt
│   │       ├── VintageObservation.kt
│   │       ├── GDPData.kt                     # 지표별 Models
│   │       ├── UnemploymentData.kt
│   │       ├── CPIData.kt
│   │       ├── InterestRateData.kt
│   │       └── YieldCurve.kt
│   │
│   └── internal/
│       ├── macro/
│       │   ├── MacroApiImpl.kt                # Domain 구현
│       │   ├── MacroCacheManager.kt           # Cache 관리
│       │   ├── services/
│       │   │   ├── GDPService.kt              # Interface
│       │   │   ├── GDPServiceImpl.kt          # 구현
│       │   │   ├── UnemploymentService.kt
│       │   │   ├── UnemploymentServiceImpl.kt
│       │   │   ├── InflationService.kt
│       │   │   ├── InflationServiceImpl.kt
│       │   │   ├── InterestRatesService.kt
│       │   │   ├── InterestRatesServiceImpl.kt
│       │   │   ├── HousingService.kt
│       │   │   ├── HousingServiceImpl.kt
│       │   │   ├── ConsumerSentimentService.kt
│       │   │   ├── ConsumerSentimentServiceImpl.kt
│       │   │   ├── MoneyAndFinanceService.kt
│       │   │   └── MoneyAndFinanceServiceImpl.kt
│       │   └── SeriesIdCatalog.kt             # Series ID 상수
│       │
│       └── fred/
│           ├── FREDSource.kt                  # Source Interface
│           ├── FREDSourceImpl.kt              # HTTP 구현
│           ├── FREDRateLimiter.kt             # TokenBucket
│           └── model/
│               ├── FREDSeriesObservationsResponse.kt
│               ├── FREDSeriesInfoResponse.kt
│               ├── FREDSeriesSearchResponse.kt
│               └── FREDVintageDatesResponse.kt
│
├── liveTest/kotlin/live/macro/
│   ├── FREDLiveTest.kt                        # Live 테스트
│   ├── GDPLiveTest.kt
│   ├── UnemploymentLiveTest.kt
│   ├── InflationLiveTest.kt
│   └── VintageDataLiveTest.kt
│
└── test/kotlin/api/macro/
    ├── MacroApiTest.kt                        # Unit 테스트
    ├── GDPServiceTest.kt
    ├── UnemploymentServiceTest.kt
    ├── ResponseParsingTest.kt
    └── ErrorHandlingTest.kt
```

### 11.2 파일별 책임

#### Public API Layer

| 파일 | 책임 |
|------|------|
| MacroApi.kt | 사용자 대면 인터페이스, 메서드 시그니처 정의 |

#### Domain Models

| 파일 | 책임 |
|------|------|
| Series.kt | 시계열 데이터 Domain Model |
| SeriesInfo.kt | 시리즈 메타데이터 Domain Model |
| VintageObservation.kt | Vintage Data Domain Model |
| GDPData.kt | GDP 지표 특화 Model |

#### Internal Domain Layer

| 파일 | 책임 |
|------|------|
| MacroApiImpl.kt | MacroApi 구현, FREDSource 호출, 캐시 관리 |
| MacroCacheManager.kt | 캐시 저장/조회/무효화 |
| GDPServiceImpl.kt | GDP 지표 특화 로직 |
| SeriesIdCatalog.kt | 모든 Series ID 상수 정의 |

#### Source Layer

| 파일 | 책임 |
|------|------|
| FREDSource.kt | FRED API 호출 인터페이스 |
| FREDSourceImpl.kt | HTTP 요청/응답 처리, JSON 파싱 |
| FREDRateLimiter.kt | TokenBucket Rate Limiter 구현 |

#### Response Models

| 파일 | 책임 |
|------|------|
| FREDSeriesObservationsResponse.kt | FRED API 원시 응답 매핑 |

### 11.3 SeriesIdCatalog 예시

```kotlin
package com.ulalax.ufc.internal.macro

object SeriesIdCatalog {

    object GDP {
        const val REAL_GDP = "GDPC1"
        const val NOMINAL_GDP = "GDP"
        const val POTENTIAL_GDP = "GDPPOT"
        const val GDP_DEFLATOR = "GDPDEF"
    }

    object Unemployment {
        const val RATE = "UNRATE"
        const val INITIAL_CLAIMS = "ICSA"
        const val CONTINUING_CLAIMS = "CCSA"
        const val NONFARM_PAYROLLS = "PAYEMS"
        const val LABOR_FORCE_PARTICIPATION = "CIVPART"
    }

    object Inflation {
        const val CPI_ALL = "CPIAUCSL"
        const val CPI_CORE = "CPILFESL"
        const val PPI = "PPIACO"
        const val PCE = "PCEPI"
        const val PCE_CORE = "PCEPILFE"
    }

    object InterestRates {
        const val FEDERAL_FUNDS_RATE = "DFF"
        const val PRIME_RATE = "DPRIME"
        const val MORTGAGE_30_YEAR = "MORTGAGE30US"
        const val TREASURY_1_MONTH = "DGS1MO"
        const val TREASURY_3_MONTH = "DGS3MO"
        const val TREASURY_10_YEAR = "DGS10"
        const val TREASURY_30_YEAR = "DGS30"
    }
}
```

---

## 12. 참고 자료

### 12.1 FRED API 공식 문서
- **API Overview**: https://fred.stlouisfed.org/docs/api/fred/
- **Series Observations**: https://fred.stlouisfed.org/docs/api/fred/series_observations.html
- **Series Search**: https://fred.stlouisfed.org/docs/api/fred/series_search.html
- **API Key 발급**: https://fred.stlouisfed.org/docs/api/api_key.html

### 12.2 Python fredapi
- **GitHub**: https://github.com/mortada/fredapi
- **PyPI**: https://pypi.org/project/fredapi/

### 12.3 관련 UFC 문서
- **1차개발 FRED 명세서**: `/plan/1차개발/06-fred-macro-indicators.md`
- **아키텍처 설계**: `/plan/1차개발/01-architecture-design.md`
- **에러 처리**: `/plan/1차개발/02-error-handling.md`
- **테스트 전략**: `/plan/1차개발/09-testing-strategy.md`

---

## 문서 이력

| 버전 | 날짜 | 변경 내용 | 작성자 |
|------|------|---------|--------|
| 1.0.0 | 2025-12-02 | 초안 작성 | Claude Code |
| 2.0.0 | 2025-12-03 | 설계 중심 명세서로 전면 재작성 | Claude Code |

**2.0.0 변경사항:**
- 구현 코드 제거, 설계 개념 중심으로 전환
- 아키텍처 다이어그램 ASCII 형식 추가
- Domain Models와 Response Models 명확히 분리
- 메서드 시그니처만 제공, 구현 로직 제외
- 데이터 처리 흐름 개념적으로 설명
- 캐싱 전략, 에러 처리 전략 중심 설명
- 파일 구조 및 책임 명확화

---

**최종 수정일**: 2025-12-03
**문서 버전**: 2.0.0
