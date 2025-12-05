# Fundamentals Timeseries API 기능 명세서

> **Version**: 1.0
> **작성일**: 2025-12-05
> **대상**: UFC 프로젝트에 Yahoo Finance Fundamentals Timeseries API 통합

---

## 목차

- [1. API 개요](#1-api-개요)
- [2. Yahoo Finance Fundamentals Timeseries API 분석](#2-yahoo-finance-fundamentals-timeseries-api-분석)
- [3. UFC 통합 설계](#3-ufc-통합-설계)
- [4. 데이터 매핑](#4-데이터-매핑)
- [5. 에러 처리](#5-에러-처리)
- [6. 테스트 전략](#6-테스트-전략)
- [7. 구현 우선순위](#7-구현-우선순위)

---

## 1. API 개요

### 1.1 Fundamentals Timeseries API의 목적

Fundamentals Timeseries API는 Yahoo Finance의 `/ws/fundamentals-timeseries/v1/finance/timeseries` 엔드포인트를 통해 **재무제표의 시계열 데이터**를 조회하기 위한 API입니다. 손익계산서, 대차대조표, 현금흐름표의 각 항목에 대한 연간(Annual), 분기별(Quarterly), Trailing 12개월(TTM) 데이터를 제공합니다.

### 1.2 주요 사용 사례

1. **재무 추세 분석**: 매출, 순이익, 현금흐름 등의 시간에 따른 변화 추이 분석
2. **발행주식수 히스토리**: 자사주 매입, 증자 등으로 인한 발행주식수 변동 추적
3. **Trailing 데이터 조회**: 최근 12개월 누적 재무 지표 (TTM) 조회
4. **재무제표 비교**: 연간/분기별 재무제표 항목 간 비교 분석
5. **커스텀 재무 비율 계산**: 시계열 데이터를 활용한 자체 재무 비율 산출

### 1.3 QuoteSummary 재무 데이터와의 차이점

| 구분 | Fundamentals Timeseries API | QuoteSummary API (재무제표 모듈) |
|------|---------------------------|--------------------------------|
| **엔드포인트** | `/ws/fundamentals-timeseries/v1/finance/timeseries/{symbol}` | `/v10/finance/quoteSummary/{symbol}` |
| **목적** | 개별 재무 항목의 시계열 데이터 | 완전한 재무제표 스냅샷 |
| **데이터 범위** | 최대 4-5년 (연간 기준) | 최근 4개 기간 (연간 또는 분기) |
| **데이터 구조** | 항목별 시계열 배열 | 기간별 전체 재무제표 |
| **필드 선택** | 필요한 항목만 선택 가능 | 전체 재무제표 반환 |
| **Trailing 지원** | TTM (Trailing Twelve Months) 지원 | 미지원 |
| **응답 크기** | 작음 (선택한 항목만) | 큼 (전체 재무제표) |
| **파싱 복잡도** | 중간 (시계열 reshape 필요) | 낮음 (플랫 구조) |
| **유스케이스** | 특정 항목 추세 분석, 차트 생성 | 재무제표 전체 조회, 비율 계산 |

**선택 가이드**:
- **Fundamentals Timeseries 사용**: 특정 재무 항목(예: 매출, EPS)의 장기 추세가 필요한 경우
- **QuoteSummary 사용**: 특정 시점의 완전한 재무제표가 필요한 경우

### 1.4 Fundamentals Timeseries의 장점

- **세밀한 데이터 선택**: 필요한 재무 항목만 선택하여 API 호출 최적화
- **긴 히스토리**: QuoteSummary보다 더 긴 기간의 데이터 제공 (최대 4-5년)
- **Trailing 지표**: TTM(Trailing Twelve Months) 데이터로 최신 실적 분석
- **효율적 응답**: 선택한 항목만 반환하므로 불필요한 데이터 전송 감소
- **차트 친화적**: 시계열 형태로 반환되어 차트 생성에 즉시 활용 가능

---

## 2. Yahoo Finance Fundamentals Timeseries API 분석

### 2.1 API 엔드포인트

```
GET https://query2.finance.yahoo.com/ws/fundamentals-timeseries/v1/finance/timeseries/{symbol}
```

**베이스 URL**: `https://query2.finance.yahoo.com`
**경로 파라미터**: `{symbol}` - 조회할 종목 심볼 (예: AAPL, MSFT)

### 2.2 요청 파라미터 상세

| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---------|------|------|------|------|
| `symbol` | String (경로) | 필수 | 조회할 종목 심볼 | `AAPL` |
| `symbol` | String (쿼리) | 필수 | 심볼 재확인 (경로와 동일) | `symbol=AAPL` |
| `type` | String | 필수 | 조회할 재무 항목 (쉼표 구분) | `annualTotalRevenue,quarterlyNetIncome` |
| `period1` | Long | 선택 | 조회 시작 시각 (Unix timestamp 초 단위) | `1577836800` (2020-01-01) |
| `period2` | Long | 선택 | 조회 종료 시각 (Unix timestamp 초 단위) | `1733356800` (2024-12-05) |

**참고사항**:
- `type` 파라미터에는 빈도(annual/quarterly/trailing) + 항목명을 결합한 값 사용
- Yahoo는 `period1`, `period2`와 관계없이 최대 4-5년 데이터만 반환
- 다수의 `type`을 한 번에 요청 가능 (쉼표로 구분)
- 각 `type`은 독립적으로 처리되며, 일부만 유효한 경우 해당 항목만 반환

### 2.3 지원하는 Type 목록

`type` 파라미터는 **빈도(frequency) + 항목명(field name)** 형식으로 구성됩니다.

#### 2.3.1 빈도 (Frequency)

| 빈도 | 설명 | 예시 |
|------|------|------|
| `annual` | 연간 데이터 (회계연도 기준) | `annualTotalRevenue` |
| `quarterly` | 분기별 데이터 | `quarterlyNetIncome` |
| `trailing` | Trailing 12개월 데이터 (최근 4분기 합산) | `trailingEps` |

**Trailing 지원 범위**:
- 손익계산서 (Income Statement): 전체 지원
- 현금흐름표 (Cash Flow): 전체 지원
- 대차대조표 (Balance Sheet): **미지원** (시점 데이터이므로 누적 의미 없음)

#### 2.3.2 항목명 (Field Names)

Yahoo Finance는 약 200개 이상의 재무 항목을 지원합니다. 주요 항목은 다음과 같습니다.

##### 손익계산서 (Income Statement)

**매출 관련**:
- `TotalRevenue`: 총 매출
- `OperatingRevenue`: 영업 매출
- `CostOfRevenue`: 매출원가
- `GrossProfit`: 매출총이익

**이익 관련**:
- `OperatingIncome`: 영업이익
- `EBIT`: 이자·세전이익
- `EBITDA`: 이자·세금·감가상각 전 이익
- `PretaxIncome`: 세전이익
- `NetIncome`: 순이익
- `NetIncomeCommonStockholders`: 보통주 귀속 순이익

**EPS 관련**:
- `BasicEPS`: 기본주당순이익
- `DilutedEPS`: 희석주당순이익
- `BasicAverageShares`: 기본주식수
- `DilutedAverageShares`: 희석주식수

**비용 관련**:
- `ResearchAndDevelopment`: 연구개발비
- `SellingGeneralAndAdministration`: 판매관리비
- `OperatingExpense`: 영업비용
- `InterestExpense`: 이자비용
- `TaxProvision`: 법인세비용

##### 대차대조표 (Balance Sheet)

**자산**:
- `TotalAssets`: 총자산
- `CurrentAssets`: 유동자산
- `CashAndCashEquivalents`: 현금및현금성자산
- `CashFinancial`: 현금
- `AccountsReceivable`: 매출채권
- `Inventory`: 재고자산
- `NetPPE`: 순유형자산
- `Goodwill`: 영업권

**부채**:
- `TotalLiabilitiesNetMinorityInterest`: 총부채
- `CurrentLiabilities`: 유동부채
- `AccountsPayable`: 매입채무
- `LongTermDebt`: 장기부채
- `CurrentDebt`: 단기부채

**자본**:
- `StockholdersEquity`: 주주지분
- `CommonStockEquity`: 보통주자본
- `RetainedEarnings`: 이익잉여금
- `TotalCapitalization`: 총자본

**주식수**:
- `OrdinarySharesNumber`: 보통주식수
- `ShareIssued`: 발행주식수
- `TreasurySharesNumber`: 자기주식수

##### 현금흐름표 (Cash Flow)

**영업활동**:
- `OperatingCashFlow`: 영업활동현금흐름
- `CashFlowFromContinuingOperatingActivities`: 계속영업활동현금흐름
- `DepreciationAndAmortization`: 감가상각비

**투자활동**:
- `InvestingCashFlow`: 투자활동현금흐름
- `CapitalExpenditure`: 자본적지출
- `PurchaseOfPPE`: 유형자산 취득

**재무활동**:
- `FinancingCashFlow`: 재무활동현금흐름
- `RepurchaseOfCapitalStock`: 자사주 매입
- `CashDividendsPaid`: 배당금 지급
- `NetIssuancePaymentsOfDebt`: 차입금 순증감

**기타**:
- `FreeCashFlow`: 잉여현금흐름
- `EndCashPosition`: 기말현금
- `BeginningCashPosition`: 기초현금

#### 2.3.3 Type 조합 예시

```
# 연간 총매출, 분기별 순이익
type=annualTotalRevenue,quarterlyNetIncome

# Trailing EPS와 연간 발행주식수
type=trailingEps,annualOrdinarySharesNumber

# 분기별 영업현금흐름, 잉여현금흐름
type=quarterlyOperatingCashFlow,quarterlyFreeCashFlow

# 연간 재무제표 3대 이익
type=annualGrossProfit,annualOperatingIncome,annualNetIncome
```

### 2.4 응답 JSON 구조 분석

#### 2.4.1 전체 응답 구조

```json
{
  "timeseries": {
    "result": [
      {
        "meta": {
          "type": ["annualTotalRevenue"],
          "symbol": ["AAPL"]
        },
        "timestamp": [1569888000, 1601510400, 1632960000, 1664582400],
        "annualTotalRevenue": [
          {
            "asOfDate": "2019-09-28",
            "periodType": "12M",
            "currencyCode": "USD",
            "reportedValue": {
              "raw": 260174000000,
              "fmt": "260.17B"
            }
          },
          {
            "asOfDate": "2020-09-26",
            "periodType": "12M",
            "currencyCode": "USD",
            "reportedValue": {
              "raw": 274515000000,
              "fmt": "274.52B"
            }
          }
        ]
      },
      {
        "meta": {
          "type": ["quarterlyNetIncome"],
          "symbol": ["AAPL"]
        },
        "timestamp": [1577491200, 1585353600, 1593216000],
        "quarterlyNetIncome": [
          {
            "asOfDate": "2019-12-28",
            "periodType": "3M",
            "currencyCode": "USD",
            "reportedValue": {
              "raw": 22236000000,
              "fmt": "22.24B"
            }
          }
        ]
      }
    ],
    "error": null
  }
}
```

#### 2.4.2 응답 필드 설명

**최상위 구조**:
- `timeseries.result`: 요청한 각 `type`별로 별도 객체 배열 반환
- `timeseries.error`: 에러 정보 (정상 응답 시 `null`)

**각 Result 객체**:
- `meta.type`: 해당 결과의 type (배열 형태)
- `meta.symbol`: 심볼 (배열 형태)
- `timestamp`: Unix timestamp 배열 (각 데이터 포인트의 시각)
- `{typeName}`: 실제 데이터 배열 (type명과 동일한 키)

**데이터 포인트 객체**:
- `asOfDate`: 재무제표 기준일 (YYYY-MM-DD 형식)
- `periodType`: 기간 타입 (`12M`, `3M`, `TTM` 등)
- `currencyCode`: 통화 코드 (예: `USD`, `KRW`)
- `reportedValue.raw`: 원본 숫자 값 (Long 또는 Double)
- `reportedValue.fmt`: 포맷팅된 문자열 (예: "260.17B")

#### 2.4.3 데이터 특성

1. **타임스탬프와 asOfDate 불일치**:
   - `timestamp`: 데이터가 공시된 시각 또는 처리된 시각
   - `asOfDate`: 실제 재무제표 기준일
   - **파싱 시 `asOfDate` 사용 권장**

2. **데이터 순서**:
   - 일반적으로 오래된 데이터부터 최신 데이터 순으로 정렬
   - 단, 보장되지 않으므로 `asOfDate` 기준으로 재정렬 필요

3. **Null 처리**:
   - 일부 기간에 데이터가 없는 경우 해당 항목이 배열에서 누락됨 (null이 아님)
   - `reportedValue.raw`가 null인 경우도 존재

4. **통화 혼재**:
   - 글로벌 기업의 경우 시기별로 `currencyCode`가 다를 수 있음
   - 통화 변환 로직 필요 시 `currencyCode` 체크 필수

#### 2.4.4 에러 응답 구조

**전체 에러**:
```json
{
  "timeseries": {
    "result": [],
    "error": {
      "code": "Bad Request",
      "description": "Invalid symbol"
    }
  }
}
```

**부분 에러** (일부 type만 실패):
```json
{
  "timeseries": {
    "result": [
      {
        "meta": { "type": ["annualTotalRevenue"] },
        "timestamp": [],
        "annualTotalRevenue": []
      }
    ],
    "error": null
  }
}
```

- 부분 에러 시 `result`는 반환되지만 데이터 배열이 비어있음
- 전체 에러 시 `result`가 빈 배열이고 `error` 객체 존재

---

## 3. UFC 통합 설계

### 3.1 기존 아키텍처와의 통합 방안

UFC는 현재 Yahoo Finance의 두 가지 API를 직접 노출하고 있습니다:
- `Ufc.quoteSummary()`: QuoteSummary API
- `Ufc.chart()`: Chart API

Fundamentals Timeseries API도 동일한 패턴으로 통합합니다.

**통합 위치**:
1. **YahooClient에 메서드 추가**: `YahooClient.fundamentalsTimeseries()`
2. **Ufc 클래스에서 직접 노출**: `Ufc.fundamentalsTimeseries()`

### 3.2 네임스페이스 배치

Fundamentals Timeseries API는 **재무 데이터**에 속하므로, 향후 고수준 API 설계 시 다음과 같은 네임스페이스에 배치될 수 있습니다:

**현재 단계 (Phase 1 - 로우레벨 API)**:
```kotlin
ufc.quoteSummary(...)
ufc.chart(...)
ufc.fundamentalsTimeseries(...)  // 신규 추가
```

**향후 고수준 API (Phase 2 - 도메인 API, 참고용)**:
```kotlin
// 예시: 재무 데이터 도메인 API
ufc.financials.getIncomeStatement(symbol, frequency = Frequency.ANNUAL)
ufc.financials.getBalanceSheet(symbol, frequency = Frequency.QUARTERLY)
ufc.financials.getCashFlow(symbol, frequency = Frequency.TRAILING)
ufc.financials.getMetric(symbol, metric = FinancialMetric.TOTAL_REVENUE, frequency = Frequency.ANNUAL)
```

**현재 명세서 범위**: Phase 1의 로우레벨 API인 `fundamentalsTimeseries()` 메서드만 다룹니다.

### 3.3 필요한 클래스 및 역할

#### 3.3.1 YahooClient (기존 클래스 확장)

**역할**: Fundamentals Timeseries API 호출 메서드 추가

**추가 메서드**:
```kotlin
// 기본 메서드
suspend fun fundamentalsTimeseries(
    symbol: String,
    types: List<FundamentalsType>,
    startDate: LocalDate? = null,
    endDate: LocalDate? = null
): FundamentalsTimeseriesResult
```

**메서드 설명**:
- `symbol`: 조회할 종목 심볼
- `types`: 조회할 재무 항목 목록 (빈도 포함)
- `startDate`: 조회 시작일 (선택, 기본값: 5년 전)
- `endDate`: 조회 종료일 (선택, 기본값: 오늘)
- 반환값: 파싱된 시계열 데이터

#### 3.3.2 FundamentalsType (신규 enum)

**패키지**: `com.ulalax.ufc.yahoo.model`

**역할**: 재무 항목과 빈도를 표현하는 Enum 클래스

**구조**:
- 각 enum 값은 빈도(Frequency) + 항목명(Field)을 나타냄
- API 파라미터 값으로 변환 가능한 `apiValue` 속성 포함
- 재무제표 타입별 그룹핑 (손익계산서, 대차대조표, 현금흐름표)

**주요 항목**:
- `ANNUAL_TOTAL_REVENUE`: 연간 총매출
- `QUARTERLY_NET_INCOME`: 분기별 순이익
- `TRAILING_EPS`: Trailing EPS
- `ANNUAL_ORDINARY_SHARES_NUMBER`: 연간 발행주식수
- 등 주요 재무 항목 약 50-100개 정의

**헬퍼 메서드**:
- `fromApiValue(value: String): FundamentalsType?`: API 값으로부터 enum 찾기
- `incomeStatementTypes(): List<FundamentalsType>`: 손익계산서 항목만 필터링
- `balanceSheetTypes(): List<FundamentalsType>`: 대차대조표 항목만 필터링
- `cashFlowTypes(): List<FundamentalsType>`: 현금흐름표 항목만 필터링

#### 3.3.3 FundamentalsTimeseriesResult (신규 data class)

**패키지**: `com.ulalax.ufc.yahoo.model`

**역할**: Fundamentals Timeseries API 응답의 도메인 모델

**필드**:
- `symbol: String`: 조회한 종목 심볼
- `data: Map<FundamentalsType, List<TimeseriesDataPoint>>`: 타입별 시계열 데이터
- `hasData(type: FundamentalsType): Boolean`: 특정 타입 데이터 존재 여부
- `get(type: FundamentalsType): List<TimeseriesDataPoint>?`: 특정 타입 데이터 조회

**사용 예시**:
```kotlin
val result = ufc.fundamentalsTimeseries(
    "AAPL",
    listOf(FundamentalsType.ANNUAL_TOTAL_REVENUE)
)

val revenues = result.get(FundamentalsType.ANNUAL_TOTAL_REVENUE)
revenues?.forEach { point ->
    println("${point.asOfDate}: ${point.value}")
}
```

#### 3.3.4 TimeseriesDataPoint (신규 data class)

**패키지**: `com.ulalax.ufc.yahoo.model`

**역할**: 단일 시계열 데이터 포인트 표현

**필드**:
- `asOfDate: LocalDate`: 재무제표 기준일
- `periodType: String`: 기간 타입 (12M, 3M, TTM 등)
- `value: Double?`: 재무 항목 값 (null 가능)
- `currencyCode: String`: 통화 코드 (USD, KRW 등)

**특징**:
- `value`가 null인 경우 데이터 미제공 의미
- `asOfDate` 기준으로 정렬 가능하도록 `Comparable` 구현

#### 3.3.5 FundamentalsTimeseriesResponse (신규 internal class)

**패키지**: `com.ulalax.ufc.yahoo.internal.response`

**역할**: Yahoo API의 JSON 응답을 직접 매핑하는 내부 DTO

**필드**:
- `timeseries: Timeseries`
  - `result: List<TimeseriesResult>`
    - `meta: Meta`
      - `type: List<String>`
      - `symbol: List<String>`
    - `timestamp: List<Long>`
    - 동적 필드: 각 type명에 해당하는 데이터 배열
  - `error: ApiError?`
    - `code: String`
    - `description: String`

**특징**:
- kotlinx.serialization 사용
- 동적 필드 파싱을 위해 `JsonObject` 또는 커스텀 deserializer 필요

#### 3.3.6 FundamentalsTimeseriesMapper (신규 internal class)

**패키지**: `com.ulalax.ufc.yahoo.internal.mapper`

**역할**: API 응답 DTO를 도메인 모델로 변환

**주요 메서드**:
- `toResult(response: FundamentalsTimeseriesResponse, requestedTypes: List<FundamentalsType>): FundamentalsTimeseriesResult`
- `parseDataPoint(jsonElement: JsonElement): TimeseriesDataPoint?`
- `reshapeTimeseries(...)`: yfinance의 DataFrame reshape 로직을 Kotlin으로 이식

**변환 로직**:
1. 각 `result` 객체에서 `meta.type`을 파싱하여 FundamentalsType 매칭
2. 해당 type의 데이터 배열을 `TimeseriesDataPoint` 리스트로 변환
3. `asOfDate` 기준으로 정렬
4. Map<FundamentalsType, List<TimeseriesDataPoint>> 형태로 구성

#### 3.3.7 YahooApiUrls (기존 클래스 확장)

**역할**: Fundamentals Timeseries API URL 상수 추가

**추가 상수**:
```kotlin
const val FUNDAMENTALS_TIMESERIES_BASE = "/ws/fundamentals-timeseries/v1/finance/timeseries"
```

### 3.4 API 메서드 시그니처 정의

#### 3.4.1 YahooClient.fundamentalsTimeseries()

```kotlin
/**
 * Yahoo Finance Fundamentals Timeseries API 호출
 *
 * 재무제표의 시계열 데이터를 조회합니다. 손익계산서, 대차대조표, 현금흐름표의
 * 각 항목에 대한 연간, 분기별, Trailing 데이터를 제공합니다.
 *
 * @param symbol 조회할 종목 심볼 (예: "AAPL", "MSFT")
 * @param types 조회할 재무 항목 목록 (FundamentalsType enum 리스트)
 * @param startDate 조회 시작일 (선택, 기본값: 5년 전)
 * @param endDate 조회 종료일 (선택, 기본값: 오늘)
 * @return FundamentalsTimeseriesResult (파싱된 시계열 데이터)
 * @throws ApiException API 호출 실패 또는 잘못된 응답
 * @throws DataParsingException 응답 파싱 실패
 *
 * 사용 예시:
 * ```kotlin
 * val result = yahooClient.fundamentalsTimeseries(
 *     symbol = "AAPL",
 *     types = listOf(
 *         FundamentalsType.ANNUAL_TOTAL_REVENUE,
 *         FundamentalsType.QUARTERLY_NET_INCOME
 *     )
 * )
 * ```
 */
suspend fun fundamentalsTimeseries(
    symbol: String,
    types: List<FundamentalsType>,
    startDate: LocalDate? = null,
    endDate: LocalDate? = null
): FundamentalsTimeseriesResult
```

#### 3.4.2 Ufc.fundamentalsTimeseries()

```kotlin
/**
 * Yahoo Finance Fundamentals Timeseries API 직접 호출
 *
 * YahooClient.fundamentalsTimeseries()의 편의 메서드입니다.
 *
 * @see YahooClient.fundamentalsTimeseries
 */
suspend fun fundamentalsTimeseries(
    symbol: String,
    types: List<FundamentalsType>,
    startDate: LocalDate? = null,
    endDate: LocalDate? = null
): FundamentalsTimeseriesResult =
    yahoo.fundamentalsTimeseries(symbol, types, startDate, endDate)
```

### 3.5 클래스 다이어그램 (개념적)

```
┌─────────────────────────────────────────────────────────────┐
│                           Ufc                                │
│  + fundamentalsTimeseries(...)                              │
└─────────────────────┬───────────────────────────────────────┘
                      │ 위임
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                      YahooClient                             │
│  + fundamentalsTimeseries(...)                              │
│  - httpClient: HttpClient                                   │
│  - authenticator: YahooAuthenticator                        │
│  - rateLimiter: RateLimiter                                 │
└─────────────────┬───────────────────────────┬───────────────┘
                  │ HTTP 호출                 │ 매핑
                  ▼                           ▼
┌──────────────────────────────┐  ┌────────────────────────────┐
│ FundamentalsTimeseriesResponse│  │FundamentalsTimeseriesMapper│
│ (internal DTO)                │  │  + toResult(...)           │
└──────────────────────────────┘  └────────────────────────────┘
                                              │ 생성
                                              ▼
                  ┌──────────────────────────────────────────┐
                  │    FundamentalsTimeseriesResult          │
                  │  + symbol: String                        │
                  │  + data: Map<Type, List<DataPoint>>      │
                  └──────────────────────────────────────────┘
                                              │ 포함
                                              ▼
                  ┌──────────────────────────────────────────┐
                  │       TimeseriesDataPoint                │
                  │  + asOfDate: LocalDate                   │
                  │  + periodType: String                    │
                  │  + value: Double?                        │
                  │  + currencyCode: String                  │
                  └──────────────────────────────────────────┘

                  ┌──────────────────────────────────────────┐
                  │       FundamentalsType (enum)            │
                  │  ANNUAL_TOTAL_REVENUE                    │
                  │  QUARTERLY_NET_INCOME                    │
                  │  TRAILING_EPS                            │
                  │  ...                                     │
                  └──────────────────────────────────────────┘
```

---

## 4. 데이터 매핑

### 4.1 Yahoo 응답 → UFC 도메인 모델 매핑

#### 4.1.1 FundamentalsTimeseriesResponse → FundamentalsTimeseriesResult

| Yahoo 응답 필드 | UFC 도메인 모델 | 변환 로직 |
|----------------|----------------|----------|
| `timeseries.result[].meta.type[0]` | `FundamentalsType` (Map의 key) | `FundamentalsType.fromApiValue()` 사용 |
| `timeseries.result[].meta.symbol[0]` | `FundamentalsTimeseriesResult.symbol` | 첫 번째 result의 symbol 사용 |
| `timeseries.result[].{typeName}[]` | `List<TimeseriesDataPoint>` (Map의 value) | 아래 DataPoint 변환 적용 |

#### 4.1.2 Yahoo DataPoint → TimeseriesDataPoint

| Yahoo 필드 | UFC 필드 | 타입 변환 | Nullable 처리 |
|-----------|---------|----------|--------------|
| `asOfDate` | `asOfDate` | `String → LocalDate` (ISO 포맷 파싱) | Non-null (필수 필드) |
| `periodType` | `periodType` | `String → String` (그대로 사용) | Non-null (기본값: "UNKNOWN") |
| `reportedValue.raw` | `value` | `JsonElement → Double?` | Nullable (데이터 없으면 null) |
| `currencyCode` | `currencyCode` | `String → String` | Non-null (기본값: "USD") |

### 4.2 타입 변환 규칙

#### 4.2.1 날짜 변환

```
Yahoo: "2024-09-30" (String, YYYY-MM-DD)
  ↓
UFC: LocalDate.of(2024, 9, 30)

변환 메서드: LocalDate.parse(asOfDate)
에러 처리: 파싱 실패 시 DataParsingException 발생
```

#### 4.2.2 숫자 값 변환

```
Yahoo: {"raw": 260174000000, "fmt": "260.17B"}
  ↓
UFC: 260174000000.0 (Double)

변환 메서드:
- JsonElement.jsonPrimitive.doubleOrNull
- Long 값인 경우 Double로 캐스팅

특수 케이스:
- raw가 null → UFC value = null
- raw가 문자열 "N/A" → UFC value = null
```

#### 4.2.3 통화 코드 변환

```
Yahoo: "USD", "KRW", null
  ↓
UFC: "USD", "KRW", "USD" (기본값)

규칙: null이면 "USD"로 간주
```

#### 4.2.4 Period Type 변환

```
Yahoo: "12M", "3M", "TTM", null
  ↓
UFC: "12M", "3M", "TTM", "UNKNOWN"

규칙: null이면 "UNKNOWN"으로 설정
```

### 4.3 Nullable 처리 전략

#### 4.3.1 필수 필드 (Non-null)

| 필드 | 기본값 | 근거 |
|------|--------|------|
| `asOfDate` | 예외 발생 | 날짜 없는 데이터는 무의미하므로 파싱 실패 처리 |
| `symbol` | 예외 발생 | 요청한 심볼이 응답에 없으면 API 오류 |
| `periodType` | "UNKNOWN" | 기간 타입 누락 시에도 데이터는 유효할 수 있음 |
| `currencyCode` | "USD" | 대부분 미국 기업이므로 USD 가정 |

#### 4.3.2 선택 필드 (Nullable)

| 필드 | Nullable 이유 |
|------|--------------|
| `value` | Yahoo가 데이터를 제공하지 않는 경우 존재 (예: 재무제표 미공시 기업) |

#### 4.3.3 Null 처리 흐름

1. **API 레벨 (YahooClient)**:
   - `reportedValue.raw`가 null이면 `TimeseriesDataPoint.value = null`로 설정
   - 데이터 포인트 자체가 누락된 경우 해당 기간은 리스트에서 제외

2. **도메인 레벨 (FundamentalsTimeseriesResult)**:
   - `value`가 null인 데이터 포인트도 리스트에 포함 (사용자가 판단)
   - `hasData(type)` 메서드는 리스트가 비어있지 않으면 `true` 반환

3. **사용자 코드**:
   - `value`가 null인지 체크 후 처리
   - 예: `dataPoint.value ?: 0.0` 또는 필터링

### 4.4 데이터 정렬 규칙

#### 4.4.1 시계열 정렬

- **기준**: `asOfDate` 오름차순 (오래된 데이터 → 최신 데이터)
- **근거**: 차트 및 추세 분석 시 자연스러운 순서
- **구현**: `TimeseriesDataPoint`를 `Comparable<TimeseriesDataPoint>`로 구현

#### 4.4.2 Type 순서

- **Map 타입**: `LinkedHashMap` 사용 (요청 순서 유지)
- **근거**: 사용자가 요청한 순서대로 데이터 반환

### 4.5 데이터 검증 규칙

#### 4.5.1 응답 검증

1. **전체 에러 체크**: `timeseries.error != null` → `ApiException` 발생
2. **빈 결과 체크**: `timeseries.result.isEmpty()` → 경고 로그 후 빈 Result 반환
3. **부분 에러 체크**: 요청한 type이 result에 없으면 해당 type은 Map에서 제외

#### 4.5.2 데이터 포인트 검증

1. **asOfDate 검증**: 파싱 불가 시 `DataParsingException`
2. **value 범위 검증**: 없음 (음수, 0 모두 유효)
3. **통화 일관성 검증**: 동일 type 내 통화 혼재 시 경고 로그 (예외 미발생)

---

## 5. 에러 처리

### 5.1 예상되는 에러 케이스

#### 5.1.1 API 호출 에러

| 에러 시나리오 | HTTP 상태 | Yahoo 에러 응답 | UFC 예외 |
|-------------|-----------|----------------|---------|
| 잘못된 심볼 | 200 (에러 플래그) | `error.code = "Bad Request"` | `ApiException(INVALID_SYMBOL)` |
| 빈 type 파라미터 | 200 | `result = []` | `ApiException(INVALID_PARAMETER)` |
| 네트워크 타임아웃 | - | - | `ApiException(NETWORK_ERROR)` |
| Rate Limit 초과 | 429 | "Too Many Requests" | `ApiException(RATE_LIMITED)` |
| 인증 실패 | 401 | Unauthorized | `ApiException(AUTH_FAILED)` |
| 서버 오류 | 500-599 | Internal Server Error | `ApiException(SERVER_ERROR)` |

#### 5.1.2 데이터 파싱 에러

| 에러 시나리오 | 원인 | UFC 예외 |
|-------------|------|---------|
| JSON 파싱 실패 | 잘못된 JSON 형식 | `DataParsingException(PARSING_ERROR)` |
| asOfDate 파싱 실패 | 날짜 형식 불일치 | `DataParsingException(PARSING_ERROR)` |
| 알 수 없는 type | API가 새로운 type 반환 | 경고 로그 (예외 미발생) |
| 필수 필드 누락 | API 응답 스키마 변경 | `DataParsingException(PARSING_ERROR)` |

#### 5.1.3 입력 검증 에러

| 에러 시나리오 | 검증 규칙 | UFC 예외 |
|-------------|----------|---------|
| 빈 심볼 | `symbol.isBlank()` | `IllegalArgumentException` |
| 빈 types 리스트 | `types.isEmpty()` | `IllegalArgumentException` |
| startDate > endDate | 날짜 순서 검증 | `IllegalArgumentException` |

### 5.2 에러 응답 구조

#### 5.2.1 Yahoo API 에러 응답

**전체 에러 (심볼 자체 오류)**:
```json
{
  "timeseries": {
    "result": [],
    "error": {
      "code": "Bad Request",
      "description": "Invalid symbol: INVALID_SYMBOL"
    }
  }
}
```

**부분 에러 (일부 type만 유효하지 않음)**:
```json
{
  "timeseries": {
    "result": [
      {
        "meta": { "type": ["annualInvalidField"] },
        "timestamp": [],
        "annualInvalidField": []
      }
    ],
    "error": null
  }
}
```

#### 5.2.2 UFC 예외 매핑

| Yahoo 에러 | ErrorCode | 메시지 예시 |
|-----------|-----------|-----------|
| `error.code = "Bad Request"` | `INVALID_SYMBOL` | "Invalid symbol: AAPL" |
| HTTP 429 | `RATE_LIMITED` | "Rate limit exceeded. Retry after X seconds" |
| HTTP 401 | `AUTH_FAILED` | "Authentication failed" |
| HTTP 500 | `SERVER_ERROR` | "Yahoo Finance server error" |
| 네트워크 타임아웃 | `NETWORK_ERROR` | "Request timeout" |
| JSON 파싱 실패 | `PARSING_ERROR` | "Failed to parse API response" |

### 5.3 에러 처리 전략

#### 5.3.1 YahooClient 레벨

```kotlin
// 의사 코드
try {
    // 1. Rate Limiting
    rateLimiter.acquire()

    // 2. HTTP 요청
    val response = httpClient.get(url) {
        parameter("symbol", symbol)
        parameter("type", types.joinToString(",") { it.apiValue })
        // ...
    }

    // 3. HTTP 상태 검증
    if (!response.status.isSuccess()) {
        when (response.status.value) {
            401 -> throw ApiException(ErrorCode.AUTH_FAILED, ...)
            429 -> throw ApiException(ErrorCode.RATE_LIMITED, ...)
            in 500..599 -> throw ApiException(ErrorCode.SERVER_ERROR, ...)
            else -> throw ApiException(ErrorCode.UNKNOWN_ERROR, ...)
        }
    }

    // 4. JSON 파싱
    val body = response.body<FundamentalsTimeseriesResponse>()

    // 5. API 에러 체크
    body.timeseries.error?.let { error ->
        throw ApiException(
            ErrorCode.INVALID_SYMBOL,
            "Yahoo API error: ${error.description}"
        )
    }

    // 6. 도메인 모델 변환
    return mapper.toResult(body, types)

} catch (e: JsonDecodingException) {
    throw DataParsingException(ErrorCode.PARSING_ERROR, "JSON parsing failed", e)
} catch (e: ApiException) {
    throw e  // 재전파
} catch (e: Exception) {
    throw ApiException(ErrorCode.NETWORK_ERROR, "Network error", e)
}
```

#### 5.3.2 Mapper 레벨

```kotlin
// 의사 코드
fun toResult(response: FundamentalsTimeseriesResponse, requestedTypes: List<FundamentalsType>): FundamentalsTimeseriesResult {
    val dataMap = mutableMapOf<FundamentalsType, List<TimeseriesDataPoint>>()

    for (resultItem in response.timeseries.result) {
        try {
            val typeName = resultItem.meta.type.firstOrNull() ?: continue
            val type = FundamentalsType.fromApiValue(typeName)

            if (type == null) {
                logger.warn("Unknown fundamentals type: $typeName")
                continue  // 무시하고 계속 진행
            }

            val dataPoints = parseDataPoints(resultItem, typeName)
            dataMap[type] = dataPoints.sortedBy { it.asOfDate }

        } catch (e: Exception) {
            logger.error("Failed to parse data for type ${resultItem.meta.type}", e)
            // 일부 type 실패 시에도 계속 진행
        }
    }

    return FundamentalsTimeseriesResult(
        symbol = response.timeseries.result.firstOrNull()?.meta?.symbol?.firstOrNull() ?: "",
        data = dataMap
    )
}
```

#### 5.3.3 에러 로깅 전략

1. **API 호출 실패**: ERROR 레벨 로그 + 예외 발생
2. **부분 파싱 실패**: WARN 레벨 로그 + 계속 진행
3. **알 수 없는 type**: WARN 레벨 로그 + 무시
4. **데이터 검증 실패**: INFO 레벨 로그 + 해당 데이터 포인트 제외

#### 5.3.4 재시도 정책

- **Rate Limit 에러 (429)**: RateLimiter에서 자동 처리 (대기 후 재시도)
- **네트워크 타임아웃**: 재시도하지 않음 (사용자에게 예외 전달)
- **서버 오류 (500-599)**: 재시도하지 않음

---

## 6. 테스트 전략

### 6.1 단위 테스트 시나리오

#### 6.1.1 FundamentalsType 테스트

**테스트 클래스**: `FundamentalsTypeTest`

| 테스트 케이스 | 검증 내용 |
|-------------|----------|
| `fromApiValue_validType_returnsEnum` | 유효한 API 값으로 enum 찾기 성공 |
| `fromApiValue_invalidType_returnsNull` | 잘못된 API 값은 null 반환 |
| `apiValue_returnsCorrectString` | enum의 apiValue가 올바른 문자열 반환 |
| `incomeStatementTypes_returnsOnlyIncomeTypes` | 손익계산서 항목만 필터링 |
| `balanceSheetTypes_returnsOnlyBalanceSheetTypes` | 대차대조표 항목만 필터링 |
| `cashFlowTypes_returnsOnlyCashFlowTypes` | 현금흐름표 항목만 필터링 |

#### 6.1.2 TimeseriesDataPoint 테스트

**테스트 클래스**: `TimeseriesDataPointTest`

| 테스트 케이스 | 검증 내용 |
|-------------|----------|
| `compareTo_ordersByAsOfDate` | asOfDate 기준으로 정렬 가능 |
| `equals_sameData_returnsTrue` | 동일 데이터 비교 시 true |
| `hashCode_sameData_returnsSameHash` | 동일 데이터의 해시코드 일치 |

#### 6.1.3 FundamentalsTimeseriesMapper 테스트

**테스트 클래스**: `FundamentalsTimeseriesMapperTest`

| 테스트 케이스 | 검증 내용 |
|-------------|----------|
| `toResult_validResponse_returnsCorrectResult` | 정상 응답을 올바른 Result로 변환 |
| `toResult_emptyResult_returnsEmptyData` | 빈 결과는 빈 Map 반환 |
| `toResult_nullValue_createsDataPointWithNullValue` | null 값도 DataPoint로 생성 |
| `toResult_multipleTypes_mapsAllTypes` | 다수 type 모두 매핑 |
| `toResult_unknownType_logsWarningAndSkips` | 알 수 없는 type은 경고 후 무시 |
| `toResult_invalidDate_throwsException` | 날짜 파싱 실패 시 예외 발생 |
| `parseDataPoint_validData_returnsDataPoint` | 정상 데이터 포인트 파싱 |
| `parseDataPoint_missingAsOfDate_throwsException` | asOfDate 누락 시 예외 |

#### 6.1.4 FundamentalsTimeseriesResult 테스트

**테스트 클래스**: `FundamentalsTimeseriesResultTest`

| 테스트 케이스 | 검증 내용 |
|-------------|----------|
| `hasData_existingType_returnsTrue` | 존재하는 type은 true 반환 |
| `hasData_nonExistingType_returnsFalse` | 없는 type은 false 반환 |
| `get_existingType_returnsData` | 존재하는 type의 데이터 반환 |
| `get_nonExistingType_returnsNull` | 없는 type은 null 반환 |

### 6.2 통합 테스트 시나리오

#### 6.2.1 기본 동작 테스트

**테스트 클래스**: `FundamentalsTimeseriesSpec`

**그룹**: `@Nested class BasicBehavior`

| 테스트 케이스 | 검증 내용 |
|-------------|----------|
| `returns annual revenue for AAPL` | AAPL의 연간 총매출 조회 성공 |
| `returns quarterly net income for MSFT` | MSFT의 분기별 순이익 조회 성공 |
| `returns trailing EPS for GOOGL` | GOOGL의 Trailing EPS 조회 성공 |
| `returns multiple types for single symbol` | 단일 심볼에 대해 다수 type 조회 |

#### 6.2.2 응답 데이터 스펙 테스트

**그룹**: `@Nested class ResponseSpec`

| 테스트 케이스 | 검증 내용 |
|-------------|----------|
| `requested types are included in response` | 요청한 type이 응답에 포함됨 |
| `data points are sorted by asOfDate` | asOfDate 기준으로 정렬됨 |
| `data points have valid dates` | 모든 데이터 포인트의 날짜가 유효함 |
| `data points have non-null currency codes` | 통화 코드가 모두 non-null |
| `empty type returns no data for that type` | 빈 type은 해당 데이터 없음 |

#### 6.2.3 다양한 재무 항목 테스트

**그룹**: `@Nested class VariousFinancials`

| 테스트 케이스 | 검증 내용 |
|-------------|----------|
| `income statement items are fetched correctly` | 손익계산서 항목 정상 조회 |
| `balance sheet items are fetched correctly` | 대차대조표 항목 정상 조회 |
| `cash flow items are fetched correctly` | 현금흐름표 항목 정상 조회 |
| `shares outstanding returns valid data` | 발행주식수 데이터 정상 반환 |

#### 6.2.4 에러 케이스 테스트

**그룹**: `@Nested class ErrorHandling`

| 테스트 케이스 | 검증 내용 |
|-------------|----------|
| `invalid symbol throws ApiException` | 잘못된 심볼 시 ApiException 발생 |
| `empty types list throws IllegalArgumentException` | 빈 types 리스트 시 예외 발생 |
| `blank symbol throws IllegalArgumentException` | 빈 심볼 시 예외 발생 |
| `start date after end date throws exception` | 날짜 순서 오류 시 예외 발생 |

#### 6.2.5 날짜 범위 테스트

**그룹**: `@Nested class DateRanges`

| 테스트 케이스 | 검증 내용 |
|-------------|----------|
| `default date range returns recent data` | 기본 날짜 범위로 최신 데이터 반환 |
| `custom date range filters data` | 커스텀 날짜 범위로 필터링 |
| `period1 and period2 are applied correctly` | period1, period2 파라미터 정확히 적용 |

#### 6.2.6 특수 케이스 테스트

**그룹**: `@Nested class SpecialCases`

| 테스트 케이스 | 검증 내용 |
|-------------|----------|
| `handles null values in data points` | null 값을 가진 데이터 포인트 처리 |
| `handles mixed currency codes` | 통화 코드 혼재 시 정상 처리 |
| `ETF fundamentals may be empty` | ETF는 재무 데이터 없을 수 있음 |
| `crypto symbols return empty data` | 암호화폐는 빈 데이터 반환 |

### 6.3 테스트 데이터 준비

#### 6.3.1 고정 심볼 (TestFixtures)

```kotlin
object Symbols {
    const val AAPL = "AAPL"   // 풍부한 재무 데이터
    const val MSFT = "MSFT"   // 풍부한 재무 데이터
    const val GOOGL = "GOOGL" // 풍부한 재무 데이터
    const val SPY = "SPY"     // ETF (재무 데이터 없음)
    const val BTC_USD = "BTC-USD"  // 암호화폐 (재무 데이터 없음)
    const val INVALID = "INVALID_SYMBOL_12345"
}
```

#### 6.3.2 샘플 응답 JSON (Mock)

단위 테스트용 샘플 JSON 파일 준비:
- `/src/test/resources/yahoo/fundamentals-timeseries/annual_revenue_success.json`
- `/src/test/resources/yahoo/fundamentals-timeseries/quarterly_income_success.json`
- `/src/test/resources/yahoo/fundamentals-timeseries/trailing_eps_success.json`
- `/src/test/resources/yahoo/fundamentals-timeseries/invalid_symbol_error.json`
- `/src/test/resources/yahoo/fundamentals-timeseries/empty_result.json`

#### 6.3.3 ResponseRecorder 활용

통합 테스트 시 실제 API 응답을 파일로 저장하여 향후 단위 테스트에 활용:

```kotlin
if (RecordingConfig.isRecordingEnabled) {
    ResponseRecorder.record(
        result,
        RecordingConfig.Paths.Yahoo.FUNDAMENTALS_TIMESERIES,
        "aapl_annual_revenue"
    )
}
```

### 6.4 테스트 실행 가이드

#### 6.4.1 단위 테스트

```bash
# 전체 단위 테스트
./gradlew test --tests '*Test'

# 특정 클래스
./gradlew test --tests 'FundamentalsTypeTest'
./gradlew test --tests 'FundamentalsTimeseriesMapperTest'
```

#### 6.4.2 통합 테스트

```bash
# 전체 통합 테스트
./gradlew integrationTest --tests '*Spec'

# 특정 클래스
./gradlew integrationTest --tests 'FundamentalsTimeseriesSpec'

# 특정 그룹
./gradlew integrationTest --tests 'FundamentalsTimeseriesSpec$BasicBehavior'
```

### 6.5 테스트 커버리지 목표

| 항목 | 목표 커버리지 |
|------|-------------|
| YahooClient.fundamentalsTimeseries() | 90% 이상 |
| FundamentalsTimeseriesMapper | 95% 이상 |
| FundamentalsType enum | 100% |
| FundamentalsTimeseriesResult | 100% |
| TimeseriesDataPoint | 100% |

---

## 7. 구현 우선순위

### 7.1 Phase 1: 핵심 기능 구현 (필수)

**목표**: Fundamentals Timeseries API의 기본 동작 구현

**작업 항목**:
1. **모델 클래스 정의**:
   - `FundamentalsType` enum (주요 50개 항목)
   - `TimeseriesDataPoint` data class
   - `FundamentalsTimeseriesResult` data class
   - `FundamentalsTimeseriesResponse` internal data class

2. **YahooClient 확장**:
   - `fundamentalsTimeseries()` 메서드 구현
   - HTTP 요청 및 파라미터 구성
   - Rate Limiting 적용

3. **매퍼 구현**:
   - `FundamentalsTimeseriesMapper` 클래스
   - JSON 응답 → 도메인 모델 변환 로직
   - asOfDate 파싱, 정렬 로직

4. **에러 처리**:
   - API 에러 응답 처리
   - 입력 검증 (빈 심볼, 빈 types 등)
   - JSON 파싱 실패 처리

5. **기본 테스트**:
   - 단위 테스트 (모델, 매퍼)
   - 통합 테스트 (AAPL 연간 매출, MSFT 분기별 순이익)

**완료 조건**: AAPL의 연간 총매출 조회 성공

### 7.2 Phase 2: 확장 기능 (중요)

**목표**: 다양한 재무 항목 및 엣지 케이스 지원

**작업 항목**:
1. **FundamentalsType 확장**:
   - 추가 재무 항목 enum 정의 (100개 이상)
   - 재무제표별 그룹핑 헬퍼 메서드

2. **Trailing 지원**:
   - Trailing 12개월 데이터 조회 테스트
   - Trailing과 Annual/Quarterly 동시 요청 테스트

3. **날짜 범위 지원**:
   - `startDate`, `endDate` 파라미터 구현
   - 날짜 범위 검증 로직

4. **추가 테스트**:
   - 다양한 재무 항목 조합 테스트
   - ETF, 암호화폐 등 특수 케이스 테스트
   - 에러 케이스 전체 시나리오 테스트

**완료 조건**: 주요 재무 항목 100개 이상 지원, 테스트 커버리지 90% 이상

### 7.3 Phase 3: 최적화 및 고도화 (선택)

**목표**: 성능 최적화 및 사용자 편의 기능 추가

**작업 항목**:
1. **캐싱 최적화**:
   - Fundamentals Timeseries 응답 캐싱 (TTL: 1일)
   - 동일 symbol + types 조합 캐시 히트율 측정

2. **헬퍼 메서드**:
   - `getRevenueHistory(symbol, frequency)`: 매출 히스토리 조회
   - `getEpsHistory(symbol, frequency)`: EPS 히스토리 조회
   - `getSharesOutstanding(symbol)`: 발행주식수 조회

3. **데이터 분석 유틸**:
   - `TimeseriesDataPoint.growthRate()`: 성장률 계산
   - `List<TimeseriesDataPoint>.average()`: 평균값 계산
   - `List<TimeseriesDataPoint>.trend()`: 추세 분석

4. **성능 테스트**:
   - 다수 type 동시 요청 성능 측정
   - 캐시 유무에 따른 응답 시간 비교

**완료 조건**: 캐시 적용 시 응답 시간 50% 단축

### 7.4 구현 순서 요약

```
Phase 1 (필수) → Phase 2 (중요) → Phase 3 (선택)
   1주          2주               1주 (옵션)

주요 마일스톤:
- Phase 1 완료: 기본 API 동작
- Phase 2 완료: 프로덕션 준비 완료
- Phase 3 완료: 최적화 및 고급 기능
```

### 7.5 의존성 및 선행 작업

**현재 UFC 상태 (이미 완료된 것)**:
- YahooClient 인프라 (HTTP, 인증, Rate Limiting)
- 공통 예외 처리 (UfcException, ErrorCode)
- 통합 테스트 프레임워크 (IntegrationTestBase, ResponseRecorder)

**추가 의존성 없음**: 기존 인프라 그대로 사용 가능

---

## 8. 참고 자료

### 8.1 yfinance 참조 코드

**파일 경로**:
- `/home/ulalax/project/kairos/yfinance/yfinance/scrapers/fundamentals.py`
  - `Financials._get_financials_time_series()` 메서드 (122-174라인)
  - URL 구성, 응답 파싱, DataFrame reshape 로직 참조

**핵심 로직**:
1. URL 구성: `timescale` + `keys`를 `type` 파라미터로 결합
2. 응답 파싱: `timeseries.result[]`에서 각 type별 데이터 추출
3. Reshape: timestamp 기준으로 DataFrame 컬럼 구성, asOfDate 기준으로 값 매핑

### 8.2 Yahoo Finance API 문서

**비공식 API**이므로 공식 문서 없음. 다음 방법으로 스펙 확인:
- yfinance 소스코드 분석
- 브라우저 DevTools로 실제 요청/응답 관찰
- 다양한 심볼 및 파라미터 조합 테스트

### 8.3 UFC 기존 구조 참조

**YahooClient 구현 패턴**:
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/yahoo/YahooClient.kt`
  - `quoteSummary()`, `chart()` 메서드 구현 패턴 참조

**테스트 구조**:
- `/home/ulalax/project/kairos/ufc/src/test/kotlin/com/ulalax/ufc/integration/yahoo/QuoteSummarySpec.kt`
  - API 가이드 스타일 통합 테스트 작성 패턴 참조

**응답 매핑**:
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/yahoo/internal/response/QuoteSummaryResponse.kt`
  - `RawFormatted` 헬퍼 클래스 패턴 참조

---

## 9. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2025-12-05 | Claude Code | 초기 명세서 작성 |

---

## 부록 A: FundamentalsType 전체 목록 (예시)

### 손익계산서 (Income Statement)

#### 연간 (Annual)
- `ANNUAL_TOTAL_REVENUE`
- `ANNUAL_COST_OF_REVENUE`
- `ANNUAL_GROSS_PROFIT`
- `ANNUAL_OPERATING_EXPENSE`
- `ANNUAL_OPERATING_INCOME`
- `ANNUAL_EBIT`
- `ANNUAL_EBITDA`
- `ANNUAL_PRETAX_INCOME`
- `ANNUAL_TAX_PROVISION`
- `ANNUAL_NET_INCOME`
- `ANNUAL_NET_INCOME_COMMON_STOCKHOLDERS`
- `ANNUAL_BASIC_EPS`
- `ANNUAL_DILUTED_EPS`
- `ANNUAL_BASIC_AVERAGE_SHARES`
- `ANNUAL_DILUTED_AVERAGE_SHARES`
- `ANNUAL_RESEARCH_AND_DEVELOPMENT`
- `ANNUAL_SELLING_GENERAL_AND_ADMINISTRATION`

#### 분기별 (Quarterly)
- `QUARTERLY_TOTAL_REVENUE`
- `QUARTERLY_COST_OF_REVENUE`
- `QUARTERLY_GROSS_PROFIT`
- `QUARTERLY_OPERATING_INCOME`
- `QUARTERLY_NET_INCOME`
- `QUARTERLY_BASIC_EPS`
- `QUARTERLY_DILUTED_EPS`

#### Trailing
- `TRAILING_TOTAL_REVENUE`
- `TRAILING_GROSS_PROFIT`
- `TRAILING_OPERATING_INCOME`
- `TRAILING_NET_INCOME`
- `TRAILING_EPS`
- `TRAILING_EBITDA`

### 대차대조표 (Balance Sheet)

#### 연간 (Annual)
- `ANNUAL_TOTAL_ASSETS`
- `ANNUAL_CURRENT_ASSETS`
- `ANNUAL_CASH_AND_CASH_EQUIVALENTS`
- `ANNUAL_ACCOUNTS_RECEIVABLE`
- `ANNUAL_INVENTORY`
- `ANNUAL_NET_PPE`
- `ANNUAL_GOODWILL`
- `ANNUAL_TOTAL_LIABILITIES_NET_MINORITY_INTEREST`
- `ANNUAL_CURRENT_LIABILITIES`
- `ANNUAL_ACCOUNTS_PAYABLE`
- `ANNUAL_LONG_TERM_DEBT`
- `ANNUAL_STOCKHOLDERS_EQUITY`
- `ANNUAL_COMMON_STOCK_EQUITY`
- `ANNUAL_RETAINED_EARNINGS`
- `ANNUAL_ORDINARY_SHARES_NUMBER`
- `ANNUAL_SHARE_ISSUED`
- `ANNUAL_TREASURY_SHARES_NUMBER`

#### 분기별 (Quarterly)
- `QUARTERLY_TOTAL_ASSETS`
- `QUARTERLY_CASH_AND_CASH_EQUIVALENTS`
- `QUARTERLY_TOTAL_LIABILITIES_NET_MINORITY_INTEREST`
- `QUARTERLY_STOCKHOLDERS_EQUITY`
- `QUARTERLY_ORDINARY_SHARES_NUMBER`

### 현금흐름표 (Cash Flow)

#### 연간 (Annual)
- `ANNUAL_OPERATING_CASH_FLOW`
- `ANNUAL_INVESTING_CASH_FLOW`
- `ANNUAL_FINANCING_CASH_FLOW`
- `ANNUAL_FREE_CASH_FLOW`
- `ANNUAL_CAPITAL_EXPENDITURE`
- `ANNUAL_REPURCHASE_OF_CAPITAL_STOCK`
- `ANNUAL_CASH_DIVIDENDS_PAID`
- `ANNUAL_END_CASH_POSITION`

#### 분기별 (Quarterly)
- `QUARTERLY_OPERATING_CASH_FLOW`
- `QUARTERLY_FREE_CASH_FLOW`
- `QUARTERLY_CAPITAL_EXPENDITURE`

#### Trailing
- `TRAILING_OPERATING_CASH_FLOW`
- `TRAILING_FREE_CASH_FLOW`

**참고**: 위 목록은 예시이며, 실제 구현 시 yfinance의 `const.py`에 정의된 200개 이상의 항목을 참조하여 확장해야 합니다.

---

**명세서 작성 완료**
