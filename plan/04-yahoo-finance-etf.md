# Yahoo Finance ETF - 전체 엔드포인트 명세

## 문서 정보
- **버전**: 2.0.0
- **최종 수정일**: 2025-12-01
- **작성자**: Claude Code (Software Architect)
- **문서 상태**: Active

---

## 1. ETF 클래스 개요

### 1.1 설계 목표

ETF 클래스는 Yahoo Finance의 모든 ETF 관련 데이터를 통합 제공합니다.

```kotlin
class ETF(
    val symbol: String,
    private val client: YahooFinanceClient
) {
    // QuoteSummary 모듈 기반 데이터
    suspend fun getTopHoldings(): TopHoldings
    suspend fun getFundProfile(): FundProfile
    suspend fun getSummaryDetail(): SummaryDetail
    suspend fun getPrice(): Price

    // Chart API 기반 데이터
    suspend fun history(...): List<PriceBar>

    // Events API 기반 데이터
    suspend fun getDividends(): List<Dividend>
    suspend fun getSplits(): List<Split>
}
```

---

## 2. QuoteSummary API - ETF 모듈

### 2.1 topHoldings (상위 보유 종목)

**API 엔드포인트:**
```
GET /v10/finance/quoteSummary/{symbol}?modules=topHoldings
```

**응답 구조:**

상세한 데이터 모델은 `08-data-models-reference.md`를 참조하세요.

```kotlin
@Serializable
data class TopHoldings(
    @SerialName("cashPosition") val cashPosition: ValueFormat? = null,           // 현금 비중 (%)
    @SerialName("bondPosition") val bondPosition: ValueFormat? = null,           // 채권 비중 (%)
    @SerialName("stockPosition") val stockPosition: ValueFormat? = null,         // 주식 비중 (%)
    @SerialName("otherPosition") val otherPosition: ValueFormat? = null,         // 기타 자산 비중 (%)
    @SerialName("preferredPosition") val preferredPosition: ValueFormat? = null, // 우선주 비중 (%)
    @SerialName("convertiblePosition") val convertiblePosition: ValueFormat? = null, // 전환사채 비중 (%)

    @SerialName("holdings") val holdings: List<Holding>? = null,                 // 보유 종목 목록
    @SerialName("equityHoldings") val equityHoldings: EquityHoldings? = null,    // 주식 보유 지표 (상세는 08번 문서 참조)
    @SerialName("bondHoldings") val bondHoldings: BondHoldings? = null,          // 채권 보유 지표 (상세는 08번 문서 참조)
    @SerialName("bondRatings") val bondRatings: List<BondRating>? = null,        // 채권 등급 분포 (상세는 08번 문서 참조)
    @SerialName("sectorWeightings") val sectorWeightings: List<SectorWeighting>? = null  // 섹터별 비중
)

@Serializable
data class Holding(
    @SerialName("symbol") val symbol: String,                                    // 종목 심볼
    @SerialName("holdingName") val holdingName: String,                          // 종목명
    @SerialName("holdingPercent") val holdingPercent: ValueFormat? = null        // 보유 비중 (%)
)

@Serializable
data class SectorWeighting(
    @SerialName("realestate") val realEstate: ValueFormat? = null,                       // 부동산 (%)
    @SerialName("consumer_cyclical") val consumerCyclical: ValueFormat? = null,          // 경기 소비재 (%)
    @SerialName("basic_materials") val basicMaterials: ValueFormat? = null,              // 기초 소재 (%)
    @SerialName("consumer_defensive") val consumerDefensive: ValueFormat? = null,        // 필수 소비재 (%)
    @SerialName("technology") val technology: ValueFormat? = null,                       // 기술 (%)
    @SerialName("communication_services") val communicationServices: ValueFormat? = null, // 통신 서비스 (%)
    @SerialName("financial_services") val financialServices: ValueFormat? = null,        // 금융 서비스 (%)
    @SerialName("utilities") val utilities: ValueFormat? = null,                         // 유틸리티 (%)
    @SerialName("industrials") val industrials: ValueFormat? = null,                     // 산업재 (%)
    @SerialName("energy") val energy: ValueFormat? = null,                               // 에너지 (%)
    @SerialName("healthcare") val healthcare: ValueFormat? = null                        // 헬스케어 (%)
)
```

**참고:** `ValueFormat`, `EquityHoldings`, `BondHoldings`, `BondRating` 등의 상세 정의는 `08-data-models-reference.md`를 참조하세요.

### 2.2 fundProfile (펀드 프로필)

**응답 구조:**
```kotlin
@Serializable
data class FundProfile(
    @SerialName("feesExpensesInvestment") val feesExpensesInvestment: FeesExpensesInvestment? = null,
    @SerialName("feesExpensesInvestmentCat") val feesExpensesInvestmentCat: FeesExpensesInvestmentCat? = null,
    @SerialName("brokerages") val brokerages: List<String>? = null,
    @SerialName("categoryName") val categoryName: String? = null,
    @SerialName("family") val family: String? = null,
    @SerialName("legalType") val legalType: String? = null,
    @SerialName("managementInfo") val managementInfo: ManagementInfo? = null,
    @SerialName("initInvestment") val initInvestment: ValueFormat? = null,
    @SerialName("initIraInvestment") val initIraInvestment: ValueFormat? = null,
    @SerialName("subseqInvestment") val subseqInvestment: ValueFormat? = null,
    @SerialName("subseqIraInvestment") val subseqIraInvestment: ValueFormat? = null,
    @SerialName("maxAge") val maxAge: Long? = null
)

@Serializable
data class FeesExpensesInvestment(
    @SerialName("annualReportExpenseRatio") val annualReportExpenseRatio: ValueFormat? = null,
    @SerialName("frontEndSalesLoad") val frontEndSalesLoad: ValueFormat? = null,
    @SerialName("deferredSalesLoad") val deferredSalesLoad: ValueFormat? = null,
    @SerialName("twelveBOne") val twelveBOne: ValueFormat? = null
)
```

### 2.3 summaryDetail (요약 상세)

**응답 구조:**
```kotlin
@Serializable
data class SummaryDetail(
    @SerialName("previousClose") val previousClose: ValueFormat? = null,
    @SerialName("regularMarketOpen") val regularMarketOpen: ValueFormat? = null,
    @SerialName("twoHundredDayAverage") val twoHundredDayAverage: ValueFormat? = null,
    @SerialName("trailingAnnualDividendRate") val trailingAnnualDividendRate: ValueFormat? = null,
    @SerialName("trailingAnnualDividendYield") val trailingAnnualDividendYield: ValueFormat? = null,
    @SerialName("navPrice") val navPrice: ValueFormat? = null,
    @SerialName("totalAssets") val totalAssets: ValueFormat? = null,
    @SerialName("yield") val yield: ValueFormat? = null,
    @SerialName("ytdReturn") val ytdReturn: ValueFormat? = null,
    @SerialName("beta3Year") val beta3Year: ValueFormat? = null,
    @SerialName("fundFamily") val fundFamily: String? = null,
    @SerialName("fundInceptionDate") val fundInceptionDate: ValueFormat? = null,
    @SerialName("legalType") val legalType: String? = null,
    @SerialName("threeYearAverageReturn") val threeYearAverageReturn: ValueFormat? = null,
    @SerialName("fiveYearAverageReturn") val fiveYearAverageReturn: ValueFormat? = null
)
```

### 2.4 defaultKeyStatistics (기본 통계)

**응답 구조:**
```kotlin
@Serializable
data class DefaultKeyStatistics(
    @SerialName("annualHoldingsTurnover") val annualHoldingsTurnover: ValueFormat? = null,
    @SerialName("enterpriseToRevenue") val enterpriseToRevenue: ValueFormat? = null,
    @SerialName("beta3Year") val beta3Year: ValueFormat? = null,
    @SerialName("profitMargins") val profitMargins: ValueFormat? = null,
    @SerialName("enterpriseToEbitda") val enterpriseToEbitda: ValueFormat? = null,
    @SerialName("52WeekChange") val fiftyTwoWeekChange: ValueFormat? = null,
    @SerialName("morningStarOverallRating") val morningStarOverallRating: ValueFormat? = null,
    @SerialName("morningStarRiskRating") val morningStarRiskRating: ValueFormat? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("ytdReturn") val ytdReturn: ValueFormat? = null,
    @SerialName("beta") val beta: ValueFormat? = null,
    @SerialName("fundFamily") val fundFamily: String? = null
)
```

### 2.5 price (가격 정보)

**응답 구조:**
```kotlin
@Serializable
data class Price(
    @SerialName("maxAge") val maxAge: Long? = null,
    @SerialName("preMarketChangePercent") val preMarketChangePercent: ValueFormat? = null,
    @SerialName("preMarketChange") val preMarketChange: ValueFormat? = null,
    @SerialName("preMarketTime") val preMarketTime: Long? = null,
    @SerialName("preMarketPrice") val preMarketPrice: ValueFormat? = null,
    @SerialName("preMarketSource") val preMarketSource: String? = null,
    @SerialName("postMarketChangePercent") val postMarketChangePercent: ValueFormat? = null,
    @SerialName("postMarketChange") val postMarketChange: ValueFormat? = null,
    @SerialName("postMarketTime") val postMarketTime: Long? = null,
    @SerialName("postMarketPrice") val postMarketPrice: ValueFormat? = null,
    @SerialName("postMarketSource") val postMarketSource: String? = null,
    @SerialName("regularMarketChangePercent") val regularMarketChangePercent: ValueFormat? = null,
    @SerialName("regularMarketChange") val regularMarketChange: ValueFormat? = null,
    @SerialName("regularMarketTime") val regularMarketTime: Long? = null,
    @SerialName("priceHint") val priceHint: ValueFormat? = null,
    @SerialName("regularMarketPrice") val regularMarketPrice: ValueFormat? = null,
    @SerialName("regularMarketDayHigh") val regularMarketDayHigh: ValueFormat? = null,
    @SerialName("regularMarketDayLow") val regularMarketDayLow: ValueFormat? = null,
    @SerialName("regularMarketVolume") val regularMarketVolume: ValueFormat? = null,
    @SerialName("averageDailyVolume10Day") val averageDailyVolume10Day: ValueFormat? = null,
    @SerialName("averageDailyVolume3Month") val averageDailyVolume3Month: ValueFormat? = null,
    @SerialName("regularMarketPreviousClose") val regularMarketPreviousClose: ValueFormat? = null,
    @SerialName("regularMarketSource") val regularMarketSource: String? = null,
    @SerialName("regularMarketOpen") val regularMarketOpen: ValueFormat? = null,
    @SerialName("strikePrice") val strikePrice: ValueFormat? = null,
    @SerialName("openInterest") val openInterest: ValueFormat? = null,
    @SerialName("exchange") val exchange: String? = null,
    @SerialName("exchangeName") val exchangeName: String? = null,
    @SerialName("exchangeDataDelayedBy") val exchangeDataDelayedBy: Long? = null,
    @SerialName("marketState") val marketState: String? = null,
    @SerialName("quoteType") val quoteType: String? = null,
    @SerialName("symbol") val symbol: String? = null,
    @SerialName("underlyingSymbol") val underlyingSymbol: String? = null,
    @SerialName("shortName") val shortName: String? = null,
    @SerialName("longName") val longName: String? = null,
    @SerialName("currency") val currency: String? = null,
    @SerialName("quoteSourceName") val quoteSourceName: String? = null,
    @SerialName("currencySymbol") val currencySymbol: String? = null,
    @SerialName("fromCurrency") val fromCurrency: String? = null,
    @SerialName("toCurrency") val toCurrency: String? = null,
    @SerialName("lastMarket") val lastMarket: String? = null,
    @SerialName("volume24Hr") val volume24Hr: ValueFormat? = null,
    @SerialName("volumeAllCurrencies") val volumeAllCurrencies: ValueFormat? = null,
    @SerialName("circulatingSupply") val circulatingSupply: ValueFormat? = null,
    @SerialName("marketCap") val marketCap: ValueFormat? = null
)
```

### 2.6 quoteType (Quote 타입)

**응답 구조:**
```kotlin
@Serializable
data class QuoteType(
    @SerialName("exchange") val exchange: String? = null,
    @SerialName("shortName") val shortName: String? = null,
    @SerialName("longName") val longName: String? = null,
    @SerialName("exchangeTimezoneName") val exchangeTimezoneName: String? = null,
    @SerialName("exchangeTimezoneShortName") val exchangeTimezoneShortName: String? = null,
    @SerialName("isEsgPopulated") val isEsgPopulated: Boolean? = null,
    @SerialName("gmtOffSetMilliseconds") val gmtOffSetMilliseconds: String? = null,
    @SerialName("quoteType") val quoteType: String? = null,
    @SerialName("symbol") val symbol: String? = null,
    @SerialName("messageBoardId") val messageBoardId: String? = null,
    @SerialName("market") val market: String? = null
)
```

---

## 3. Chart API - 가격 히스토리

### 3.1 엔드포인트

```
GET /v8/finance/chart/{symbol}?period1={start}&period2={end}&interval={interval}
```

### 3.2 파라미터

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| period1 | Long | 시작 시간 (Unix timestamp) |
| period2 | Long | 종료 시간 (Unix timestamp) |
| interval | String | 간격 (1m, 2m, 5m, 15m, 30m, 1h, 1d, 5d, 1wk, 1mo, 3mo) |
| events | String | 이벤트 포함 (div,splits) |
| includeAdjustedClose | Boolean | 조정 종가 포함 여부 |

### 3.3 응답 데이터

```kotlin
@Serializable
data class ChartResult(
    val meta: Meta,
    val timestamp: List<Long>,
    val indicators: Indicators,
    val events: Events? = null
)

@Serializable
data class Indicators(
    val quote: List<Quote>,
    val adjclose: List<AdjClose>?
)

@Serializable
data class Quote(
    val open: List<Double?>,
    val high: List<Double?>,
    val low: List<Double?>,
    val close: List<Double?>,
    val volume: List<Long?>
)
```

---

## 4. Events API - 배당 및 분할

### 4.1 Dividends (배당)

**응답 구조:**
```kotlin
@Serializable
data class Dividends(
    val dividends: Map<String, Dividend>
)

@Serializable
data class Dividend(
    val amount: Double,
    val date: Long
)
```

### 4.2 Splits (주식 분할)

**응답 구조:**
```kotlin
@Serializable
data class Splits(
    val splits: Map<String, Split>
)

@Serializable
data class Split(
    val date: Long,
    val numerator: Long,
    val denominator: Long,
    val splitRatio: String
)
```

---

## 5. ETF 클래스 구현

### 5.1 기본 구조

```kotlin
class ETF(
    val symbol: String,
    private val client: YahooFinanceClient
) {

    private suspend fun fetchModule(module: String): QuoteSummaryResponse {
        return client.fetchQuoteSummary(symbol, listOf(module))
    }

    private suspend fun fetchModules(modules: List<String>): QuoteSummaryResponse {
        return client.fetchQuoteSummary(symbol, modules)
    }

    /**
     * 모든 ETF 데이터 한 번에 조회
     */
    suspend fun fetchAll(): ETFData {
        val modules = listOf(
            "topHoldings",
            "fundProfile",
            "summaryDetail",
            "price",
            "quoteType",
            "defaultKeyStatistics",
            "calendarEvents",
            "institutionOwnership",
            "financialData"
        )
        
        val response = fetchModules(modules)
        val result = response.quoteSummary.getResultOrThrow()
        
        return ETFData(
            topHoldings = result.topHoldings,
            fundProfile = result.fundProfile,
            summaryDetail = result.summaryDetail,
            price = result.price,
            quoteType = result.quoteType,
            defaultKeyStatistics = result.defaultKeyStatistics
        )
    }

    /**
     * 상위 보유 종목 조회
     */
    suspend fun getTopHoldings(): TopHoldings {
        val response = fetchModule("topHoldings")
        return response.quoteSummary.getResultOrThrow().topHoldings
            ?: throw UFCException(ErrorCode.NO_DATA_AVAILABLE)
    }

    /**
     * 가격 히스토리 조회
     */
    suspend fun history(
        period: Period? = null,
        interval: Interval = Interval.OneDay,
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<PriceBar> {
        val params = ChartParams.from(period, interval, start, end)
        val response = client.fetchChart(symbol, params)
        return response.chart.getResultOrThrow().toPriceBars()
    }
}
```

### 5.2 데이터 접근 예시

```kotlin
/**
 * 상위 보유 종목 리스트 접근
 */
val holdings = etf.getTopHoldings()
holdings.holdings?.forEach { holding ->
    println("${holding.symbol}: ${holding.holdingName} (${holding.holdingPercent?.fmt})")
}

/**
 * 섹터 비중 접근
 */
val sectorWeightings = holdings.sectorWeightings?.firstOrNull()
sectorWeightings?.let { sw ->
    println("Technology: ${sw.technology?.fmt}")
    println("Healthcare: ${sw.healthcare?.fmt}")
    println("Financials: ${sw.financialServices?.fmt}")
    // ... 기타 섹터
}

/**
 * 데이터 분석이 필요한 경우:
 * 1. Kotlin DataFrame 라이브러리 추가
 * 2. JSON으로 직렬화하여 Python pandas 사용
 * 3. 표준 Kotlin 컬렉션 메서드 활용 (filter, map, sortedBy 등)
 */
```

---

## 6. 사용 예시

```kotlin
suspend fun main() {
    val ufc = UFCClient.create()

    val spy = ufc.yahoo.etf("SPY")

    // 상위 보유 종목
    val holdings = spy.getTopHoldings()
    holdings.holdings?.forEach { holding ->
        println("${holding.symbol}: ${holding.holdingName} (${holding.holdingPercent?.fmt})")
    }

    // 섹터 비중
    val sectors = holdings.sectorWeightings?.firstOrNull()
    sectors?.let {
        println("Technology: ${it.technology?.fmt}")
        println("Healthcare: ${it.healthcare?.fmt}")
        println("Financial Services: ${it.financialServices?.fmt}")
    }

    // 펀드 프로필
    val profile = spy.getFundProfile()
    println("Expense Ratio: ${profile.feesExpensesInvestment?.annualReportExpenseRatio?.fmt}")

    // 가격 히스토리
    val history = spy.history(period = Period.OneYear)
    history.take(10).forEach { bar ->
        println("${bar.date}: Open=${bar.open}, Close=${bar.close}, Volume=${bar.volume}")
    }

    // NAV vs 시장가격
    val detail = spy.getSummaryDetail()
    println("NAV: ${detail.navPrice?.fmt}")
    println("Market Price: ${spy.getPrice().regularMarketPrice?.fmt}")
}
```

---

**다음 문서**: [05-yahoo-finance-price.md](./05-yahoo-finance-price.md)
