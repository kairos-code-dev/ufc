# Data Models Reference - 전체 데이터 모델 참조

## 문서 정보
- **버전**: 2.0.0
- **최종 수정일**: 2025-12-01
- **작성자**: Claude Code (Software Architect)
- **문서 상태**: Active

---

## 1. 개요

이 문서는 UFC 프로젝트에서 사용되는 모든 데이터 모델을 정의합니다.

### 1.1 모델 분류

- **공통 모델**: 여러 소스에서 공유되는 모델
- **Yahoo Finance 모델**: Yahoo Finance API 전용 모델
- **FRED 모델**: FRED API 전용 모델

### 1.2 설계 원칙

- **Null Safety**: 모든 nullable 필드는 명시적으로 `?` 표시
- **kotlinx.serialization**: `@Serializable` 어노테이션 사용
- **문서화**: 모든 필드에 주석으로 단위와 의미 설명
- **기본값**: 선택적 필드에는 기본값 제공

---

## 2. 공통 모델

### 2.1 ValueFormat

Yahoo Finance API에서 값을 여러 형식으로 제공할 때 사용하는 공통 모델입니다.

```kotlin
/**
 * 값의 여러 표현 형식을 담는 공통 모델
 *
 * Yahoo Finance는 같은 값을 여러 형식으로 제공합니다:
 * - raw: 원시 숫자 값
 * - fmt: 포맷팅된 문자열 (예: "1,234.56")
 * - longFmt: 긴 형식 문자열 (예: "1,234.56M")
 */
@Serializable
data class ValueFormat(
    @SerialName("raw") val raw: Double? = null,           // 원시 숫자 값
    @SerialName("fmt") val fmt: String? = null,           // 포맷팅된 문자열
    @SerialName("longFmt") val longFmt: String? = null    // 긴 형식 문자열
)
```

**사용 예시:**
```kotlin
val price = ValueFormat(
    raw = 450.25,
    fmt = "450.25",
    longFmt = "450.25 USD"
)
```

### 2.2 Error

API 응답의 에러 정보를 담는 모델입니다.

```kotlin
/**
 * API 에러 정보
 *
 * Yahoo Finance와 FRED API 모두에서 에러 발생 시 사용됩니다.
 */
@Serializable
data class Error(
    @SerialName("code") val code: String? = null,             // 에러 코드
    @SerialName("description") val description: String? = null // 에러 설명
)
```

### 2.3 Meta

Chart API 응답의 메타데이터입니다.

```kotlin
/**
 * Chart 메타데이터
 *
 * 차트 데이터의 메타 정보를 포함합니다.
 */
@Serializable
data class Meta(
    @SerialName("currency") val currency: String? = null,                      // 통화 (USD, EUR 등)
    @SerialName("symbol") val symbol: String? = null,                          // 심볼
    @SerialName("exchangeName") val exchangeName: String? = null,              // 거래소 이름
    @SerialName("instrumentType") val instrumentType: String? = null,          // 상품 타입 (EQUITY, ETF 등)
    @SerialName("firstTradeDate") val firstTradeDate: Long? = null,           // 최초 거래일 (Unix timestamp)
    @SerialName("regularMarketTime") val regularMarketTime: Long? = null,     // 정규 시장 시간 (Unix timestamp)
    @SerialName("gmtoffset") val gmtoffset: Long? = null,                     // GMT 오프셋 (초)
    @SerialName("timezone") val timezone: String? = null,                      // 타임존 (EST, PST 등)
    @SerialName("exchangeTimezoneName") val exchangeTimezoneName: String? = null, // 거래소 타임존 이름
    @SerialName("regularMarketPrice") val regularMarketPrice: Double? = null, // 정규 시장 가격
    @SerialName("chartPreviousClose") val chartPreviousClose: Double? = null, // 차트 이전 종가
    @SerialName("previousClose") val previousClose: Double? = null,           // 이전 종가
    @SerialName("scale") val scale: Int? = null,                              // 스케일
    @SerialName("priceHint") val priceHint: Int? = null,                      // 가격 힌트 (소수점 자릿수)
    @SerialName("currentTradingPeriod") val currentTradingPeriod: TradingPeriod? = null, // 현재 거래 기간
    @SerialName("tradingPeriods") val tradingPeriods: List<List<TradingPeriodInfo>>? = null, // 거래 기간 목록
    @SerialName("dataGranularity") val dataGranularity: String? = null,       // 데이터 세분성 (1d, 1h 등)
    @SerialName("range") val range: String? = null,                           // 범위
    @SerialName("validRanges") val validRanges: List<String>? = null          // 유효한 범위 목록
)

/**
 * 거래 기간
 */
@Serializable
data class TradingPeriod(
    @SerialName("timezone") val timezone: String? = null,                     // 타임존
    @SerialName("start") val start: Long? = null,                            // 시작 시간 (Unix timestamp)
    @SerialName("end") val end: Long? = null,                                // 종료 시간 (Unix timestamp)
    @SerialName("gmtoffset") val gmtoffset: Long? = null                     // GMT 오프셋 (초)
)

/**
 * 거래 기간 상세 정보
 */
@Serializable
data class TradingPeriodInfo(
    @SerialName("timezone") val timezone: String? = null,                     // 타임존
    @SerialName("start") val start: Long? = null,                            // 시작 시간 (Unix timestamp)
    @SerialName("end") val end: Long? = null,                                // 종료 시간 (Unix timestamp)
    @SerialName("gmtoffset") val gmtoffset: Long? = null                     // GMT 오프셋 (초)
)
```

---

## 3. Yahoo Finance - Events

### 3.1 Events

차트 데이터의 이벤트(배당, 분할, 자본 이득) 정보입니다.

```kotlin
/**
 * 차트 이벤트 (배당, 분할, 자본 이득)
 */
@Serializable
data class Events(
    @SerialName("dividends") val dividends: Map<String, Dividend>? = null,        // 배당 (timestamp -> Dividend)
    @SerialName("splits") val splits: Map<String, Split>? = null,                 // 분할 (timestamp -> Split)
    @SerialName("capitalGains") val capitalGains: Map<String, CapitalGain>? = null // 자본 이득 (timestamp -> CapitalGain)
)
```

### 3.2 Dividend

배당 정보입니다.

```kotlin
/**
 * 배당 정보
 */
@Serializable
data class Dividend(
    @SerialName("amount") val amount: Double,           // 배당금 (주당, USD)
    @SerialName("date") val date: Long                  // 배당 지급일 (Unix timestamp)
)
```

**예시:**
```kotlin
val dividend = Dividend(
    amount = 0.57,  // $0.57 per share
    date = 1620000000L
)
```

### 3.3 Split

주식 분할 정보입니다.

```kotlin
/**
 * 주식 분할 정보
 */
@Serializable
data class Split(
    @SerialName("date") val date: Long,                  // 분할 날짜 (Unix timestamp)
    @SerialName("numerator") val numerator: Long,        // 분자 (예: 2-for-1 분할의 2)
    @SerialName("denominator") val denominator: Long,    // 분모 (예: 2-for-1 분할의 1)
    @SerialName("splitRatio") val splitRatio: String     // 분할 비율 문자열 (예: "2:1")
) {
    /**
     * 분할 비율 (Double)
     * 예: 2-for-1 분할 = 2.0
     */
    val ratio: Double
        get() = numerator.toDouble() / denominator.toDouble()
}
```

**예시:**
```kotlin
val split = Split(
    date = 1598832000L,
    numerator = 4,
    denominator = 1,
    splitRatio = "4:1"
)
// split.ratio = 4.0 (4-for-1 stock split)
```

### 3.4 CapitalGain

뮤추얼 펀드의 자본 이득 분배 정보입니다.

```kotlin
/**
 * 자본 이득 분배 (뮤추얼 펀드)
 */
@Serializable
data class CapitalGain(
    @SerialName("amount") val amount: Double,           // 자본 이득 금액 (주당, USD)
    @SerialName("date") val date: Long                  // 분배 날짜 (Unix timestamp)
)
```

### 3.5 AdjClose

조정 종가 데이터입니다.

```kotlin
/**
 * 조정 종가 (Adjusted Close)
 *
 * 배당, 분할 등을 고려한 조정 종가입니다.
 */
@Serializable
data class AdjClose(
    @SerialName("adjclose") val adjclose: List<Double?>  // 조정 종가 목록
)
```

---

## 4. Yahoo Finance - ETF Holdings

### 4.1 EquityHoldings

ETF의 주식 보유 지표입니다.

```kotlin
/**
 * ETF 주식 보유 지표
 */
@Serializable
data class EquityHoldings(
    @SerialName("priceToEarnings") val priceToEarnings: ValueFormat? = null,           // P/E Ratio (가격/수익 비율)
    @SerialName("priceToBook") val priceToBook: ValueFormat? = null,                   // P/B Ratio (가격/장부가 비율)
    @SerialName("priceToSales") val priceToSales: ValueFormat? = null,                 // P/S Ratio (가격/매출 비율)
    @SerialName("priceToCashflow") val priceToCashflow: ValueFormat? = null,           // P/CF Ratio (가격/현금흐름 비율)
    @SerialName("medianMarketCap") val medianMarketCap: ValueFormat? = null,           // 중간 시가총액 (USD)
    @SerialName("threeYearEarningsGrowth") val threeYearEarningsGrowth: ValueFormat? = null, // 3년 수익 성장률 (%)
    @SerialName("priceToEarningsCat") val priceToEarningsCat: ValueFormat? = null,     // 카테고리 평균 P/E Ratio
    @SerialName("priceToBookCat") val priceToBookCat: ValueFormat? = null,             // 카테고리 평균 P/B Ratio
    @SerialName("priceToSalesCat") val priceToSalesCat: ValueFormat? = null,           // 카테고리 평균 P/S Ratio
    @SerialName("priceToCashflowCat") val priceToCashflowCat: ValueFormat? = null,     // 카테고리 평균 P/CF Ratio
    @SerialName("medianMarketCapCat") val medianMarketCapCat: ValueFormat? = null,     // 카테고리 평균 중간 시가총액
    @SerialName("threeYearEarningsGrowthCat") val threeYearEarningsGrowthCat: ValueFormat? = null // 카테고리 평균 3년 수익 성장률
)
```

### 4.2 BondHoldings

ETF의 채권 보유 지표입니다.

```kotlin
/**
 * ETF 채권 보유 지표
 */
@Serializable
data class BondHoldings(
    @SerialName("maturity") val maturity: ValueFormat? = null,                         // 평균 만기 (년)
    @SerialName("duration") val duration: ValueFormat? = null,                         // 듀레이션 (년)
    @SerialName("creditQuality") val creditQuality: ValueFormat? = null,               // 신용 등급
    @SerialName("maturityCat") val maturityCat: ValueFormat? = null,                   // 카테고리 평균 만기
    @SerialName("durationCat") val durationCat: ValueFormat? = null,                   // 카테고리 평균 듀레이션
    @SerialName("creditQualityCat") val creditQualityCat: ValueFormat? = null          // 카테고리 평균 신용 등급
)
```

### 4.3 BondRating

채권 신용 등급 분포입니다.

```kotlin
/**
 * 채권 신용 등급 분포
 */
@Serializable
data class BondRating(
    @SerialName("bb") val bb: ValueFormat? = null,        // BB 등급 비중 (%)
    @SerialName("aa") val aa: ValueFormat? = null,        // AA 등급 비중 (%)
    @SerialName("aaa") val aaa: ValueFormat? = null,      // AAA 등급 비중 (%)
    @SerialName("a") val a: ValueFormat? = null,          // A 등급 비중 (%)
    @SerialName("other") val other: ValueFormat? = null,  // 기타 등급 비중 (%)
    @SerialName("b") val b: ValueFormat? = null,          // B 등급 비중 (%)
    @SerialName("bbb") val bbb: ValueFormat? = null,      // BBB 등급 비중 (%)
    @SerialName("below_b") val belowB: ValueFormat? = null, // B 미만 등급 비중 (%)
    @SerialName("us_government") val usGovernment: ValueFormat? = null // 미국 국채 비중 (%)
)
```

### 4.4 ManagementInfo

펀드 운용 정보입니다.

```kotlin
/**
 * 펀드 운용 정보
 */
@Serializable
data class ManagementInfo(
    @SerialName("managerName") val managerName: String? = null,           // 운용 매니저 이름
    @SerialName("managerBio") val managerBio: String? = null,             // 매니저 약력
    @SerialName("startdate") val startDate: ValueFormat? = null,          // 운용 시작일 (Unix timestamp)
    @SerialName("tenure") val tenure: ValueFormat? = null                 // 재임 기간 (년)
)
```

### 4.5 FeesExpensesInvestmentCat

펀드 수수료 (카테고리 평균) 정보입니다.

```kotlin
/**
 * 펀드 수수료 (카테고리 평균)
 */
@Serializable
data class FeesExpensesInvestmentCat(
    @SerialName("annualReportExpenseRatio") val annualReportExpenseRatio: ValueFormat? = null, // 연간 보고 비용 비율 (%)
    @SerialName("frontEndSalesLoad") val frontEndSalesLoad: ValueFormat? = null,               // 프론트 엔드 판매 수수료 (%)
    @SerialName("deferredSalesLoad") val deferredSalesLoad: ValueFormat? = null,               // 이연 판매 수수료 (%)
    @SerialName("twelveBOne") val twelveBOne: ValueFormat? = null                              // 12b-1 수수료 (%)
)
```

---

## 5. Yahoo Finance - 통합 모델

### 5.1 ETFData

ETF의 모든 데이터를 통합한 모델입니다.

```kotlin
/**
 * ETF 전체 데이터
 *
 * ETF의 모든 정보를 통합한 모델입니다.
 */
data class ETFData(
    val topHoldings: TopHoldings? = null,                       // 상위 보유 종목
    val fundProfile: FundProfile? = null,                       // 펀드 프로필
    val summaryDetail: SummaryDetail? = null,                   // 요약 상세
    val price: Price? = null,                                   // 가격 정보
    val quoteType: QuoteType? = null,                          // Quote 타입
    val defaultKeyStatistics: DefaultKeyStatistics? = null     // 기본 통계
)
```

---

## 6. Yahoo Finance - Search & Screener

### 6.1 SearchResult

종목 검색 결과입니다.

```kotlin
/**
 * 종목 검색 결과
 */
@Serializable
data class SearchResult(
    @SerialName("symbol") val symbol: String,                          // 심볼
    @SerialName("name") val name: String? = null,                      // 종목명
    @SerialName("exch") val exchange: String? = null,                  // 거래소
    @SerialName("type") val type: String? = null,                      // 타입 (EQUITY, ETF, MUTUALFUND 등)
    @SerialName("exchDisp") val exchangeDisplay: String? = null,       // 거래소 표시명
    @SerialName("typeDisp") val typeDisplay: String? = null,           // 타입 표시명
    @SerialName("score") val score: Double? = null,                    // 검색 스코어
    @SerialName("isYahooFinance") val isYahooFinance: Boolean? = null  // Yahoo Finance 지원 여부
)
```

### 6.2 ScreenerQuery

스크리너 조회 조건입니다.

```kotlin
/**
 * 스크리너 조회 조건
 */
data class ScreenerQuery(
    val offset: Int = 0,                          // 오프셋
    val size: Int = 25,                           // 페이지 크기
    val sortField: String? = null,                // 정렬 필드
    val sortType: SortType = SortType.DESC,       // 정렬 방향
    val quoteType: QuoteTypeFilter? = null,       // Quote 타입 필터 (EQUITY, ETF 등)
    val query: ScreenerFilter? = null             // 필터 조건
)

/**
 * 정렬 방향
 */
enum class SortType {
    ASC,   // 오름차순
    DESC   // 내림차순
}

/**
 * Quote 타입 필터
 */
enum class QuoteTypeFilter {
    EQUITY,        // 주식
    ETF,           // ETF
    MUTUALFUND,    // 뮤추얼 펀드
    INDEX,         // 지수
    CRYPTOCURRENCY // 암호화폐
}

/**
 * 스크리너 필터 조건
 */
data class ScreenerFilter(
    val operator: FilterOperator,               // 연산자 (AND, OR)
    val operands: List<FilterCondition>         // 조건 목록
)

/**
 * 필터 연산자
 */
enum class FilterOperator {
    AND,   // 그리고
    OR,    // 또는
    NOT    // 부정
}

/**
 * 필터 조건
 */
data class FilterCondition(
    val operator: ComparisonOperator,           // 비교 연산자
    val field: String,                          // 필드명 (예: "marketcap", "pe" 등)
    val values: List<Any>                       // 값 목록
)

/**
 * 비교 연산자
 */
enum class ComparisonOperator {
    EQ,    // 같음 (=)
    GT,    // 크다 (>)
    LT,    // 작다 (<)
    GTE,   // 크거나 같다 (>=)
    LTE,   // 작거나 같다 (<=)
    BTWN   // 사이 (BETWEEN)
}
```

### 6.3 ScreenerResult

스크리너 결과입니다.

```kotlin
/**
 * 스크리너 결과
 */
@Serializable
data class ScreenerResult(
    @SerialName("symbol") val symbol: String,                      // 심볼
    @SerialName("shortName") val shortName: String? = null,        // 종목명
    @SerialName("regularMarketPrice") val regularMarketPrice: ValueFormat? = null,     // 현재가
    @SerialName("regularMarketChange") val regularMarketChange: ValueFormat? = null,   // 가격 변동
    @SerialName("regularMarketChangePercent") val regularMarketChangePercent: ValueFormat? = null, // 변동률 (%)
    @SerialName("regularMarketVolume") val regularMarketVolume: ValueFormat? = null,   // 거래량
    @SerialName("marketCap") val marketCap: ValueFormat? = null,                       // 시가총액
    @SerialName("fiftyTwoWeekHigh") val fiftyTwoWeekHigh: ValueFormat? = null,         // 52주 최고가
    @SerialName("fiftyTwoWeekLow") val fiftyTwoWeekLow: ValueFormat? = null,           // 52주 최저가
    @SerialName("trailingPE") val trailingPE: ValueFormat? = null,                     // Trailing P/E
    @SerialName("dividendYield") val dividendYield: ValueFormat? = null,               // 배당 수익률 (%)
    @SerialName("averageDailyVolume3Month") val averageDailyVolume3Month: ValueFormat? = null // 3개월 평균 거래량
)
```

---

## 7. FRED - 매크로 데이터 모델

### 7.1 MacroData

범용 매크로 경제 데이터 모델입니다.

```kotlin
/**
 * 범용 매크로 경제 데이터
 *
 * FRED의 모든 경제 지표에 사용할 수 있는 범용 모델입니다.
 */
data class MacroData(
    val date: LocalDate,                    // 날짜
    val value: Double?,                     // 값
    val unit: String? = null,               // 단위 (Billions of Dollars, Percent 등)
    val seasonallyAdjusted: Boolean = false // 계절 조정 여부
)
```

### 7.2 Category

FRED 카테고리 정보입니다.

```kotlin
/**
 * FRED 카테고리 정보
 */
@Serializable
data class Category(
    @SerialName("id") val id: Int,                                    // 카테고리 ID
    @SerialName("name") val name: String,                             // 카테고리명
    @SerialName("parent_id") val parentId: Int? = null,               // 부모 카테고리 ID
    @SerialName("notes") val notes: String? = null                    // 설명
)
```

---

## 8. 통계 모델

### 8.1 MetricStats

지표 통계 정보입니다.

```kotlin
/**
 * 지표 통계 정보
 */
data class MetricStats(
    val mean: Double,                       // 평균
    val median: Double,                     // 중간값
    val min: Double,                        // 최솟값
    val max: Double,                        // 최댓값
    val stdDev: Double,                     // 표준편차
    val count: Long                         // 데이터 수
)
```

---

## 9. Enum 타입

### 9.1 Chart API Interval

```kotlin
/**
 * Chart API 간격
 */
enum class ChartInterval(val value: String) {
    ONE_MINUTE("1m"),           // 1분
    TWO_MINUTES("2m"),          // 2분
    FIVE_MINUTES("5m"),         // 5분
    FIFTEEN_MINUTES("15m"),     // 15분
    THIRTY_MINUTES("30m"),      // 30분
    SIXTY_MINUTES("60m"),       // 60분 (1시간)
    NINETY_MINUTES("90m"),      // 90분
    ONE_HOUR("1h"),             // 1시간
    ONE_DAY("1d"),              // 1일
    FIVE_DAYS("5d"),            // 5일
    ONE_WEEK("1wk"),            // 1주
    ONE_MONTH("1mo"),           // 1개월
    THREE_MONTHS("3mo")         // 3개월
}
```

### 9.2 Chart API Range

```kotlin
/**
 * Chart API 범위
 */
enum class ChartRange(val value: String) {
    ONE_DAY("1d"),              // 1일
    FIVE_DAYS("5d"),            // 5일
    ONE_MONTH("1mo"),           // 1개월
    THREE_MONTHS("3mo"),        // 3개월
    SIX_MONTHS("6mo"),          // 6개월
    ONE_YEAR("1y"),             // 1년
    TWO_YEARS("2y"),            // 2년
    FIVE_YEARS("5y"),           // 5년
    TEN_YEARS("10y"),           // 10년
    YEAR_TO_DATE("ytd"),        // 연초 대비
    MAX("max")                  // 전체 기간
}
```

### 9.3 Chart API Events

```kotlin
/**
 * Chart API 이벤트 타입
 */
enum class ChartEvents(val value: String) {
    DIVIDENDS("div"),           // 배당
    SPLITS("splits"),           // 분할
    CAPITAL_GAINS("capitalGains"), // 자본 이득
    ALL("div,splits,capitalGains") // 전체
}
```

### 9.4 QuoteSummary Modules

```kotlin
/**
 * QuoteSummary API 모듈
 */
enum class QuoteSummaryModule(val value: String) {
    ASSET_PROFILE("assetProfile"),                                       // 기업 프로필
    SUMMARY_DETAIL("summaryDetail"),                                     // 요약 상세
    SUMMARY_PROFILE("summaryProfile"),                                   // 요약 프로필
    FINANCIAL_DATA("financialData"),                                     // 재무 데이터
    QUOTE_TYPE("quoteType"),                                            // Quote 타입
    DEFAULT_KEY_STATISTICS("defaultKeyStatistics"),                     // 기본 통계
    CALENDAR_EVENTS("calendarEvents"),                                  // 이벤트 일정
    INCOME_STATEMENT_HISTORY("incomeStatementHistory"),                 // 손익계산서 (연간)
    INCOME_STATEMENT_HISTORY_QUARTERLY("incomeStatementHistoryQuarterly"), // 손익계산서 (분기)
    BALANCE_SHEET_HISTORY("balanceSheetHistory"),                       // 재무상태표 (연간)
    BALANCE_SHEET_HISTORY_QUARTERLY("balanceSheetHistoryQuarterly"),    // 재무상태표 (분기)
    CASHFLOW_STATEMENT_HISTORY("cashflowStatementHistory"),             // 현금흐름표 (연간)
    CASHFLOW_STATEMENT_HISTORY_QUARTERLY("cashflowStatementHistoryQuarterly"), // 현금흐름표 (분기)
    RECOMMENDATION_TREND("recommendationTrend"),                        // 애널리스트 추천
    INSTITUTION_OWNERSHIP("institutionOwnership"),                      // 기관 보유
    FUND_OWNERSHIP("fundOwnership"),                                    // 펀드 보유
    MAJOR_HOLDERS_BREAKDOWN("majorHoldersBreakdown"),                   // 주요 주주
    INSIDER_HOLDERS("insiderHolders"),                                  // 내부자 보유
    TOP_HOLDINGS("topHoldings"),                                        // 상위 보유 종목 (ETF)
    FUND_PROFILE("fundProfile"),                                        // 펀드 프로필 (ETF)
    PRICE("price"),                                                     // 가격 정보
    EARNINGS("earnings"),                                               // 실적 정보
    EARNINGS_HISTORY("earningsHistory"),                                // 실적 이력
    EARNINGS_TREND("earningsTrend"),                                    // 실적 추세
    UPGRADE_DOWNGRADE_HISTORY("upgradeDowngradeHistory"),               // 등급 변경 이력
    ESG_SCORES("esgScores")                                            // ESG 점수
}
```

### 9.5 FRED Units

```kotlin
/**
 * FRED 단위 변환
 */
enum class FREDUnits(val value: String, val description: String) {
    LIN("lin", "Levels (No transformation)"),                           // 수준 (변환 없음)
    CHG("chg", "Change"),                                               // 변화량
    CH1("ch1", "Change from Year Ago"),                                 // 1년 전 대비 변화량
    PCH("pch", "Percent Change"),                                       // 변화율 (%)
    PC1("pc1", "Percent Change from Year Ago"),                         // 1년 전 대비 변화율 (%)
    PCA("pca", "Compounded Annual Rate of Change"),                     // 연간 복리 성장률
    CCH("cch", "Continuously Compounded Rate of Change"),               // 연속 복리 성장률
    CCA("cca", "Continuously Compounded Annual Rate of Change"),        // 연간 연속 복리 성장률
    LOG("log", "Natural Log")                                           // 자연 로그
}
```

### 9.6 FRED Output Type

```kotlin
/**
 * FRED 출력 타입
 */
enum class FREDOutputType(val value: Int, val description: String) {
    OBSERVATIONS_BY_REAL_TIME_PERIOD(1, "Observations by Real-Time Period"),        // 실시간 기간별
    OBSERVATIONS_BY_VINTAGE_DATE(2, "Observations by Vintage Date (Single Vintage)"), // Vintage 날짜별 (단일)
    OBSERVATIONS_BY_VINTAGE_DATE_ALL(3, "Observations by Vintage Date (All Vintages)"), // Vintage 날짜별 (전체)
    OBSERVATIONS_INITIAL_RELEASE(4, "Observations, Initial Release Only")            // 최초 발표만
}
```

### 9.7 FRED Sort Order

```kotlin
/**
 * FRED 정렬 순서
 */
enum class FREDSortOrder(val value: String) {
    ASC("asc"),     // 오름차순
    DESC("desc")    // 내림차순
}
```

---

## 10. 사용 예시

### 10.1 ValueFormat 활용

```kotlin
val expenseRatio = ValueFormat(
    raw = 0.0945,
    fmt = "0.09%",
    longFmt = "0.09%"
)

println("Raw: ${expenseRatio.raw}")      // 0.0945
println("Formatted: ${expenseRatio.fmt}") // "0.09%"
```

### 10.2 Events 처리

```kotlin
val events = Events(
    dividends = mapOf(
        "1620000000" to Dividend(amount = 0.57, date = 1620000000L),
        "1628000000" to Dividend(amount = 0.60, date = 1628000000L)
    ),
    splits = mapOf(
        "1598832000" to Split(
            date = 1598832000L,
            numerator = 4,
            denominator = 1,
            splitRatio = "4:1"
        )
    )
)

events.dividends?.forEach { (timestamp, dividend) ->
    println("Dividend: $${dividend.amount} on ${Instant.ofEpochSecond(dividend.date)}")
}

events.splits?.forEach { (timestamp, split) ->
    println("Split: ${split.splitRatio} (${split.ratio}x) on ${Instant.ofEpochSecond(split.date)}")
}
```

### 10.3 ScreenerQuery 구성

```kotlin
val query = ScreenerQuery(
    offset = 0,
    size = 50,
    sortField = "marketcap",
    sortType = SortType.DESC,
    quoteType = QuoteTypeFilter.ETF,
    query = ScreenerFilter(
        operator = FilterOperator.AND,
        operands = listOf(
            FilterCondition(
                operator = ComparisonOperator.GT,
                field = "marketcap",
                values = listOf(1_000_000_000) // 시가총액 > $1B
            ),
            FilterCondition(
                operator = ComparisonOperator.LT,
                field = "expenseratio",
                values = listOf(0.20) // 운용 비용 < 0.20%
            )
        )
    )
)
```

---

## 11. 참고 사항

### 11.1 Null Safety

모든 모델은 Kotlin의 Null Safety 기능을 활용합니다:

- **Non-null 필드**: 반드시 값이 존재하는 필드
- **Nullable 필드 (`?`)**: 값이 없을 수 있는 필드
- **기본값**: 선택적 필드에 기본값 제공

### 11.2 Serialization

모든 직렬화 가능한 모델은 `@Serializable` 어노테이션을 사용합니다:

```kotlin
@Serializable
data class Example(
    @SerialName("field_name") val fieldName: String
)
```

### 11.3 ValueFormat 사용 이유

Yahoo Finance API는 같은 값을 여러 형식으로 제공합니다:

- **raw**: 계산에 사용 (Double)
- **fmt**: 사용자에게 표시 (String)
- **longFmt**: 긴 형식 표시 (String)

이를 통해 데이터의 정확성과 사용자 친화성을 모두 확보합니다.

---

**다음 문서**: [07-advanced-topics.md](./07-advanced-topics.md)
