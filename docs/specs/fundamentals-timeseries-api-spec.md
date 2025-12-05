# Fundamentals Timeseries API 기술 명세서

> **Version**: 2.0
> **작성일**: 2025-12-05

---

## 1. 개요

### 1.1 API 목적

Yahoo Finance Fundamentals Timeseries API를 통해 **재무제표의 시계열 데이터**를 조회한다. 손익계산서, 대차대조표, 현금흐름표의 각 항목에 대한 연간(Annual), 분기별(Quarterly), Trailing 12개월(TTM) 데이터를 제공한다.

| 항목 | 값 |
|-----|---|
| 엔드포인트 | `GET https://query2.finance.yahoo.com/ws/fundamentals-timeseries/v1/finance/timeseries/{symbol}` |
| HTTP 메서드 | GET |
| 인증 | 불필요 |

### 1.2 제공 데이터

| 데이터 | 설명 |
|-------|------|
| 손익계산서 시계열 | 매출, 순이익, EPS 등의 시간별 변화 |
| 대차대조표 시계열 | 자산, 부채, 자본, 발행주식수 등의 시간별 변화 |
| 현금흐름표 시계열 | 영업/투자/재무 현금흐름의 시간별 변화 |
| Trailing 데이터 | 최근 12개월 누적 재무 지표 (손익계산서, 현금흐름표만) |

### 1.3 QuoteSummary 재무 데이터와의 차이

| 구분 | Fundamentals Timeseries | QuoteSummary (재무제표 모듈) |
|------|------------------------|---------------------------|
| 목적 | 개별 재무 항목의 시계열 데이터 | 완전한 재무제표 스냅샷 |
| 데이터 범위 | 최대 4-5년 | 최근 4개 기간 |
| 데이터 구조 | 항목별 시계열 배열 | 기간별 전체 재무제표 |
| 필드 선택 | 필요한 항목만 선택 가능 | 전체 재무제표 반환 |
| Trailing 지원 | TTM 지원 | 미지원 |
| 응답 크기 | 작음 | 큼 |
| 유스케이스 | 특정 항목 추세 분석, 차트 생성 | 재무제표 전체 조회, 비율 계산 |

---

## 2. 데이터 소스 분석

### 2.1 요청 파라미터

| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---------|------|------|------|------|
| symbol (경로) | String | Yes | 조회할 종목 심볼 | `AAPL` |
| symbol (쿼리) | String | Yes | 심볼 재확인 (경로와 동일) | `symbol=AAPL` |
| type | String | Yes | 조회할 재무 항목 (쉼표 구분) | `annualTotalRevenue,quarterlyNetIncome` |
| period1 | Long | No | 조회 시작 시각 (Unix timestamp 초) | `1577836800` |
| period2 | Long | No | 조회 종료 시각 (Unix timestamp 초) | `1733356800` |

### 2.2 Type 파라미터 구조

| 구성 | 설명 | 예시 |
|-----|------|------|
| 빈도 (Frequency) | annual, quarterly, trailing | `annual`, `quarterly`, `trailing` |
| 항목명 (Field Name) | 재무 항목명 (camelCase) | `TotalRevenue`, `NetIncome`, `Eps` |
| 조합 형식 | {빈도}{항목명} | `annualTotalRevenue`, `quarterlyNetIncome` |

### 2.3 주요 재무 항목

#### 손익계산서 (Income Statement)

| 항목명 | 설명 |
|-------|------|
| TotalRevenue | 총 매출 |
| CostOfRevenue | 매출원가 |
| GrossProfit | 매출총이익 |
| OperatingIncome | 영업이익 |
| EBIT | 이자·세전이익 |
| EBITDA | 이자·세금·감가상각 전 이익 |
| NetIncome | 순이익 |
| NetIncomeCommonStockholders | 보통주 귀속 순이익 |
| BasicEPS | 기본주당순이익 |
| DilutedEPS | 희석주당순이익 |
| BasicAverageShares | 기본주식수 |
| DilutedAverageShares | 희석주식수 |
| ResearchAndDevelopment | 연구개발비 |
| SellingGeneralAndAdministration | 판매관리비 |

#### 대차대조표 (Balance Sheet)

| 항목명 | 설명 |
|-------|------|
| TotalAssets | 총자산 |
| CurrentAssets | 유동자산 |
| CashAndCashEquivalents | 현금및현금성자산 |
| AccountsReceivable | 매출채권 |
| Inventory | 재고자산 |
| NetPPE | 순유형자산 |
| TotalLiabilitiesNetMinorityInterest | 총부채 |
| CurrentLiabilities | 유동부채 |
| LongTermDebt | 장기부채 |
| StockholdersEquity | 주주지분 |
| CommonStockEquity | 보통주자본 |
| RetainedEarnings | 이익잉여금 |
| OrdinarySharesNumber | 보통주식수 |
| ShareIssued | 발행주식수 |

#### 현금흐름표 (Cash Flow)

| 항목명 | 설명 |
|-------|------|
| OperatingCashFlow | 영업활동현금흐름 |
| InvestingCashFlow | 투자활동현금흐름 |
| FinancingCashFlow | 재무활동현금흐름 |
| FreeCashFlow | 잉여현금흐름 |
| CapitalExpenditure | 자본적지출 |
| RepurchaseOfCapitalStock | 자사주 매입 |
| CashDividendsPaid | 배당금 지급 |
| EndCashPosition | 기말현금 |

### 2.4 응답 구조

#### 최상위 구조

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| timeseries | Object | No | 최상위 컨테이너 |
| timeseries.result | Array | No | 각 type별 결과 배열 |
| timeseries.error | Object | Yes | 에러 정보 (정상 시 null) |

#### Result 객체

| 필드 | 타입 | 설명 |
|-----|------|------|
| meta | Object | 메타데이터 |
| meta.type | Array&lt;String&gt; | type명 배열 |
| meta.symbol | Array&lt;String&gt; | 심볼 배열 |
| timestamp | Array&lt;Long&gt; | Unix timestamp 배열 |
| {typeName} | Array&lt;DataPoint&gt; | 실제 데이터 배열 (동적 필드) |

#### DataPoint 객체

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| asOfDate | String | No | 재무제표 기준일 (YYYY-MM-DD) |
| periodType | String | No | 기간 타입 (12M, 3M, TTM) |
| currencyCode | String | No | 통화 코드 (USD, KRW 등) |
| reportedValue | Object | No | 값 객체 |
| reportedValue.raw | Long/Double | Yes | 원본 숫자 값 |
| reportedValue.fmt | String | No | 포맷팅된 문자열 |

#### Error 객체

| 필드 | 타입 | 설명 |
|-----|------|------|
| code | String | 에러 코드 |
| description | String | 에러 설명 |

### 2.5 응답 데이터 특성

| 특성 | 설명 |
|-----|------|
| 타임스탬프 불일치 | timestamp는 공시 시각, asOfDate가 실제 기준일 (asOfDate 사용 권장) |
| 데이터 순서 | 일반적으로 과거→최신 순이나 보장되지 않음 (재정렬 필요) |
| Null 처리 | 데이터 없는 기간은 배열에서 누락 (null이 아님) |
| 통화 혼재 | 글로벌 기업은 시기별로 currencyCode가 다를 수 있음 |
| Trailing 제약 | 대차대조표는 Trailing 미지원 (시점 데이터) |

---

## 3. 데이터/모델 설계

### 3.1 Domain 모델

#### FundamentalsTimeseriesResult

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| symbol | String | No | 조회한 종목 심볼 |
| data | Map&lt;FundamentalsType, List&lt;TimeseriesDataPoint&gt;&gt; | No | 타입별 시계열 데이터 |

**헬퍼 메서드**:

| 메서드 | 반환 타입 | 설명 |
|-------|----------|------|
| hasData(type) | Boolean | 특정 타입 데이터 존재 여부 |
| get(type) | List&lt;TimeseriesDataPoint&gt;? | 특정 타입 데이터 조회 |

#### TimeseriesDataPoint

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| asOfDate | LocalDate | No | 재무제표 기준일 |
| periodType | String | No | 기간 타입 (12M, 3M, TTM, UNKNOWN) |
| value | Double | Yes | 재무 항목 값 |
| currencyCode | String | No | 통화 코드 |

**구현 요구사항**:
- `Comparable<TimeseriesDataPoint>` 구현 (asOfDate 기준 정렬)

#### FundamentalsType (Enum)

```kotlin
enum class FundamentalsType(val apiValue: String) {
    // Income Statement - Annual
    ANNUAL_TOTAL_REVENUE("annualTotalRevenue"),
    ANNUAL_NET_INCOME("annualNetIncome"),
    ANNUAL_BASIC_EPS("annualBasicEPS"),
    // ... (생략)

    // Income Statement - Quarterly
    QUARTERLY_TOTAL_REVENUE("quarterlyTotalRevenue"),
    QUARTERLY_NET_INCOME("quarterlyNetIncome"),
    // ... (생략)

    // Income Statement - Trailing
    TRAILING_TOTAL_REVENUE("trailingTotalRevenue"),
    TRAILING_EPS("trailingEps"),
    // ... (생략)

    // Balance Sheet - Annual
    ANNUAL_TOTAL_ASSETS("annualTotalAssets"),
    ANNUAL_ORDINARY_SHARES_NUMBER("annualOrdinarySharesNumber"),
    // ... (생략)

    // Cash Flow - Annual
    ANNUAL_OPERATING_CASH_FLOW("annualOperatingCashFlow"),
    ANNUAL_FREE_CASH_FLOW("annualFreeCashFlow"),
    // ... (생략)
}
```

**헬퍼 메서드**:

| 메서드 | 반환 타입 | 설명 |
|-------|----------|------|
| fromApiValue(value) | FundamentalsType? | API 값으로부터 enum 찾기 |
| incomeStatementTypes() | List&lt;FundamentalsType&gt; | 손익계산서 항목만 필터링 |
| balanceSheetTypes() | List&lt;FundamentalsType&gt; | 대차대조표 항목만 필터링 |
| cashFlowTypes() | List&lt;FundamentalsType&gt; | 현금흐름표 항목만 필터링 |

### 3.2 Internal Response 모델

#### 구조

| 클래스 | 필드 | 타입 |
|-------|-----|------|
| FundamentalsTimeseriesResponse | timeseries | Timeseries |
| Timeseries | result | List&lt;TimeseriesResult&gt;? |
| | error | ApiError? |
| TimeseriesResult | meta | Meta |
| | timestamp | List&lt;Long&gt; |
| | (동적 필드) | 각 type명에 해당하는 데이터 배열 |
| Meta | type | List&lt;String&gt; |
| | symbol | List&lt;String&gt; |
| ApiError | code | String? |
| | description | String? |

### 3.3 API 메서드 시그니처

```kotlin
suspend fun fundamentalsTimeseries(
    symbol: String,
    types: List<FundamentalsType>,
    startDate: LocalDate? = null,
    endDate: LocalDate? = null
): FundamentalsTimeseriesResult
```

| 파라미터 | 타입 | 기본값 | 제약 |
|---------|------|-------|------|
| symbol | String | - | 필수, 공백 불가 |
| types | List&lt;FundamentalsType&gt; | - | 필수, 빈 리스트 불가 |
| startDate | LocalDate | 5년 전 | 선택 |
| endDate | LocalDate | 오늘 | 선택, startDate보다 이후여야 함 |

| 반환 | 설명 |
|-----|------|
| FundamentalsTimeseriesResult | 타입별 시계열 데이터 |

### 3.4 필드 매핑

| Yahoo 필드 | Domain 필드 | 변환 |
|-----------|------------|------|
| meta.type[0] | FundamentalsType (Map key) | FundamentalsType.fromApiValue() |
| meta.symbol[0] | symbol | 첫 번째 result의 symbol 사용 |
| asOfDate | asOfDate | String (ISO 8601) → LocalDate |
| periodType | periodType | String → String (null → "UNKNOWN") |
| reportedValue.raw | value | Long/Double → Double? (null 허용) |
| currencyCode | currencyCode | String → String (null → "USD") |

---

## 4. 예외 처리

### 4.1 ErrorCode 매핑

| 에러 케이스 | HTTP 상태 | ErrorCode | 설명 |
|-----------|-----------|-----------|------|
| 잘못된 심볼 | 200 (에러 플래그) | INVALID_SYMBOL | Yahoo error.code = "Bad Request" |
| 빈 types | - | INVALID_PARAMETER | types.isEmpty() |
| 빈 symbol | - | INVALID_PARAMETER | symbol.isBlank() |
| startDate > endDate | - | INVALID_PARAMETER | 날짜 순서 오류 |
| Rate Limit 초과 | 429 | RATE_LIMITED | Too Many Requests |
| 인증 실패 | 401 | AUTH_FAILED | Unauthorized |
| 서버 오류 | 500-599 | SERVER_ERROR | Internal Server Error |
| 네트워크 타임아웃 | - | NETWORK_ERROR | Request timeout |
| JSON 파싱 실패 | - | DATA_PARSING_ERROR | 역직렬화 실패 |
| asOfDate 파싱 실패 | - | DATA_PARSING_ERROR | 날짜 형식 불일치 |

### 4.2 빈 결과 처리

| 상황 | 처리 |
|-----|------|
| result = [] | 빈 FundamentalsTimeseriesResult 반환 (예외 아님) |
| 존재하지 않는 심볼 | error 객체 존재 시 예외, 없으면 빈 결과 |
| ETF/암호화폐 | 빈 결과 반환 (재무 데이터 없음) |
| 알 수 없는 type | 경고 로그 후 해당 type 제외 (예외 아님) |

### 4.3 재시도 정책

| 에러 | 재시도 | 횟수 |
|-----|-------|-----|
| Rate Limit (429) | Yes | Rate Limiter 처리 |
| Network Error | Yes | 3회 |
| HTTP 5xx | Yes | 3회 |
| HTTP 4xx | No | - |
| JSON 파싱 실패 | No | - |

### 4.4 입력 검증

| 검증 항목 | 규칙 | 예외 |
|---------|------|------|
| symbol | 공백 불가 | IllegalArgumentException |
| types | 빈 리스트 불가 | IllegalArgumentException |
| startDate/endDate | startDate ≤ endDate | IllegalArgumentException |

### 4.5 Nullable 처리 전략

#### 필수 필드 (Non-null)

| 필드 | 기본값 | 근거 |
|------|--------|------|
| asOfDate | 예외 발생 | 날짜 없는 데이터는 무의미 |
| symbol | 예외 발생 | 요청한 심볼이 응답에 없으면 API 오류 |
| periodType | "UNKNOWN" | 기간 타입 누락 시에도 데이터는 유효 |
| currencyCode | "USD" | 대부분 미국 기업이므로 USD 가정 |

#### 선택 필드 (Nullable)

| 필드 | Nullable 이유 |
|------|--------------|
| value | Yahoo가 데이터를 제공하지 않는 경우 존재 (재무제표 미공시 기업) |

---

## 5. 참고 자료

### 5.1 API 제약사항

| 제약 | 설명 |
|-----|------|
| 비공식 API | 문서화되지 않음, 변경 가능 |
| 최대 데이터 범위 | period1/period2와 관계없이 최대 4-5년 |
| 동적 필드 | result[].{typeName} 필드가 동적 생성 (커스텀 deserializer 필요) |
| Trailing 제약 | 대차대조표는 Trailing 미지원 |

### 5.2 용어

| 용어 | 설명 |
|-----|------|
| TTM | Trailing Twelve Months (최근 12개월 누적) |
| Annual | 연간 데이터 (회계연도 기준) |
| Quarterly | 분기별 데이터 |
| asOfDate | 재무제표 기준일 (timestamp와 다름) |
| EPS | Earnings Per Share (주당순이익) |
| EBITDA | Earnings Before Interest, Taxes, Depreciation, and Amortization |

### 5.3 yfinance 참조

| 파일 | 라인 | 내용 |
|-----|-----|------|
| fundamentals.py | 122-174 | _get_financials_time_series() 메서드 참조 |
| | | URL 구성, 응답 파싱, DataFrame reshape 로직 |
