# UFC 2차 개발 구현 계획

> 이 문서는 context 초기화 후에도 독립적으로 작업 진행이 가능하도록 필요한 모든 정보를 포함합니다.

## 참조 문서

### 기술명세서 (plan/2차개발/)
- [네임스페이스 체계](./ufc-네임스페이스-체계.md) - 전체 아키텍처 및 5개 네임스페이스 정의
- [ufc.price 명세서](./ufc.price-기술명세서.md) - 현재가, 시세이력, 이동평균
- [ufc.stock 명세서](./ufc.stock-기술명세서.md) - 기업 기본정보, ISIN 조회
- [ufc.funds 명세서](./ufc.funds-기술명세서.md) - ETF/뮤추얼펀드 정보
- [ufc.corp 명세서](./ufc.corp-기술명세서.md) - 배당금, 주식분할, 자본이득
- [ufc.macro 명세서](./ufc.macro-기술명세서.md) - FRED 거시경제 지표

### 테스트 원칙
- [테스트 작성 원칙](../../doc/test-principle.md) - Classical TDD, Fake 객체 활용

---

## 현재 구현 상태 분석

### 기존 구현 파일 구조
```
src/main/kotlin/com/ulalax/ufc/
├── client/
│   ├── UFCClient.kt              # 팩토리 객체
│   └── UFCClientImpl.kt          # 메인 클라이언트 (ChartService + QuoteSummaryService)
├── domain/
│   ├── chart/
│   │   ├── ChartService.kt       # Chart 인터페이스
│   │   ├── YahooChartService.kt  # Chart API 구현체
│   │   └── ChartDataResponse.kt  # 응답 모델 (events 필드 없음)
│   └── quote/
│       ├── QuoteSummaryService.kt     # QuoteSummary 인터페이스
│       ├── YahooQuoteSummaryService.kt # QuoteSummary 구현체
│       └── QuoteSummaryResponse.kt     # 응답 모델 (일부 모듈만 지원)
├── infrastructure/
│   └── ratelimit/                # Rate Limiting (TokenBucket)
├── internal/
│   └── yahoo/
│       ├── auth/                 # 인증 (CRUMB 토큰)
│       └── YahooApiUrls.kt       # API URL 상수
├── model/common/
│   ├── Interval.kt               # 데이터 간격 enum
│   └── Period.kt                 # 조회 기간 enum
└── exception/
    └── UfcException.kt           # 예외 클래스
```

### 리팩토링 필요 사항

| 파일 | 필요한 변경 | 관련 기능 |
|------|------------|----------|
| `ChartDataResponse.kt` | `events` 필드 추가 (dividends, splits, capitalGains) | ufc.corp |
| `YahooChartService.kt` | `events` 파라미터 지원, getRawChartDataWithEvents() 추가 | ufc.corp |
| `QuoteSummaryResponse.kt` | quoteType, summaryProfile, topHoldings, fundProfile 모듈 추가 | ufc.stock, ufc.funds |
| `YahooQuoteSummaryService.kt` | 새 모듈 지원을 위한 DEFAULT_MODULES 확장 | ufc.stock, ufc.funds |

---

## 구현 순서

작업은 의존성과 복잡도를 고려하여 다음 순서로 진행합니다:

```
Phase 1: 기반 리팩토링 (기존 코드 확장)
    ↓
Phase 2: ufc.stock (QuoteSummary 모듈 확장)
    ↓
Phase 3: ufc.funds (QuoteSummary 모듈 확장 - stock과 유사)
    ↓
Phase 4: ufc.price (Chart + QuoteSummary 조합)
    ↓
Phase 5: ufc.corp (Chart API events 확장)
    ↓
Phase 6: ufc.macro (새 API - FRED)
    ↓
Phase 7: UFCClient 통합 & 최종 검증
```

### 권장 에이전트

| Phase | 권장 에이전트 | 이유 |
|-------|-------------|------|
| **Phase 1** | `dev-ko-h` (Haiku) | 기존 패턴에 필드 추가만, 단순 반복 작업 |
| **Phase 2** | `dev-ko-h` (Haiku) | QuoteSummary 래핑, 기존 서비스 패턴 복제 |
| **Phase 3** | `dev-ko-h` (Haiku) | Phase 2와 거의 동일한 패턴 |
| **Phase 4** | `dev-ko-h` + `dev-ko` | 기본 구현은 Haiku, **이동평균 계산은 Sonnet** |
| **Phase 5** | `dev-ko-h` (Haiku) | Chart API 확장, 기존 패턴 활용 |
| **Phase 6** | `dev-ko` (Sonnet) | **새로운 외부 API 통합**, 인증/에러처리 설계 필요 |
| **Phase 7** | `dev-ko` (Sonnet) | **전체 아키텍처 통합**, 복잡한 의존성 관리 |

> **팁**: Haiku로 시작 후, 결과물이 기대에 못 미치거나 에러 해결이 어려울 때 Sonnet으로 전환

---

## Phase 1: 기반 리팩토링 `dev-ko-h`

### TODO 1.1: QuoteSummary 응답 모델 확장
**파일**: `src/main/kotlin/com/ulalax/ufc/domain/quote/QuoteSummaryResponse.kt`

추가할 데이터 클래스:
```kotlin
// ufc.stock용
@Serializable
data class QuoteType(
    val exchange: String?,
    val quoteType: String?,        // EQUITY, ETF, MUTUALFUND
    val symbol: String?,
    val shortName: String?,
    val longName: String?,
    val market: String?,
    val sector: String?,
    val industry: String?
)

@Serializable
data class AssetProfile(
    val sector: String?,
    val industry: String?,
    val website: String?,
    val longBusinessSummary: String?,
    val country: String?,
    val city: String?,
    val address1: String?,
    val phone: String?,
    val fullTimeEmployees: Int?
)

// ufc.funds용
@Serializable
data class TopHoldings(
    val holdings: List<Holding>?,
    val equityHoldings: EquityHoldings?,
    val bondHoldings: BondHoldings?,
    val sectorWeightings: List<SectorWeighting>?
)

@Serializable
data class FundProfile(
    val categoryName: String?,
    val family: String?,
    val legalType: String?,
    val feesExpensesInvestment: FeesExpenses?
)
```

`QuoteSummaryResult`에 필드 추가:
```kotlin
@SerialName("quoteType") val quoteType: QuoteType? = null,
@SerialName("assetProfile") val assetProfile: AssetProfile? = null,
@SerialName("topHoldings") val topHoldings: TopHoldings? = null,
@SerialName("fundProfile") val fundProfile: FundProfile? = null
```

### TODO 1.2: Chart 응답 모델에 events 필드 추가
**파일**: `src/main/kotlin/com/ulalax/ufc/domain/chart/ChartDataResponse.kt`

`ChartResult`에 추가:
```kotlin
@SerialName("events")
val events: ChartEvents? = null
```

새 데이터 클래스:
```kotlin
@Serializable
data class ChartEvents(
    @SerialName("dividends") val dividends: Map<String, DividendEvent>? = null,
    @SerialName("splits") val splits: Map<String, SplitEvent>? = null,
    @SerialName("capitalGains") val capitalGains: Map<String, CapitalGainEvent>? = null
)

@Serializable
data class DividendEvent(
    val amount: Double?,
    val date: Long?
)

@Serializable
data class SplitEvent(
    val date: Long?,
    val numerator: Int?,
    val denominator: Int?,
    val splitRatio: String?
)

@Serializable
data class CapitalGainEvent(
    val amount: Double?,
    val date: Long?
)
```

### TODO 1.3: YahooChartService events 파라미터 지원
**파일**: `src/main/kotlin/com/ulalax/ufc/domain/chart/YahooChartService.kt`

`getRawChartData()` 메서드에 `events` 파라미터 추가:
```kotlin
override suspend fun getRawChartData(
    symbol: String,
    interval: Interval,
    period: Period,
    includeEvents: Boolean = false  // 새 파라미터
): ChartDataResponse {
    // ...
    val response = httpClient.get("${YahooApiUrls.CHART}/$symbol") {
        parameter("interval", interval.value)
        parameter("range", period.value)
        parameter("crumb", authResult.crumb)
        if (includeEvents) {
            parameter("events", "div,splits,capitalGains")
        }
    }
    // ...
}
```

`ChartService` 인터페이스도 업데이트 필요.

### TODO 1.4: ErrorCode 확장
**파일**: `src/main/kotlin/com/ulalax/ufc/exception/ErrorCode.kt`

새로운 에러 코드 추가:
```kotlin
// ufc.stock
STOCK_DATA_NOT_FOUND,
ISIN_NOT_FOUND,
SHARES_DATA_NOT_FOUND,

// ufc.funds
FUND_DATA_NOT_FOUND,
INVALID_FUND_TYPE,
INCOMPLETE_FUND_DATA,

// ufc.price
PRICE_DATA_NOT_FOUND,
INVALID_PERIOD_INTERVAL,
INVALID_DATE_RANGE,
INCOMPLETE_PRICE_DATA
```

---

## Phase 2: ufc.stock 구현 `dev-ko-h`

> 참조: [ufc.stock 기술명세서](./ufc.stock-기술명세서.md)

### TODO 2.1: Stock 도메인 모델 생성
**경로**: `src/main/kotlin/com/ulalax/ufc/domain/stock/`

#### AssetType 열거형
```kotlin
// AssetType.kt
enum class AssetType(val value: String) {
    EQUITY("EQUITY"),
    ETF("ETF"),
    MUTUALFUND("MUTUALFUND"),
    INDEX("INDEX"),
    CRYPTOCURRENCY("CRYPTOCURRENCY"),
    CURRENCY("CURRENCY"),
    FUTURE("FUTURE"),
    OPTION("OPTION"),
    UNKNOWN("UNKNOWN");

    companion object {
        fun fromString(value: String?): AssetType =
            values().firstOrNull { it.value == value } ?: UNKNOWN
    }
}
```

#### 기본 모델
```kotlin
// StockModels.kt
data class FastInfo(
    val symbol: String,
    val currency: String,            // Non-nullable
    val exchange: String,            // Non-nullable
    val quoteType: AssetType         // Non-nullable
)

data class CompanyInfo(
    val symbol: String,
    val longName: String,            // Non-nullable
    val shortName: String?,
    val sector: String?,
    val industry: String?,
    val country: String?,
    val exchange: String?,
    val currency: String?,
    val quoteType: AssetType?,
    val website: String?,
    val phone: String?,
    val address: String?,
    val city: String?,
    val state: String?,
    val zipCode: String?,
    val employees: Long?,
    val description: String?,
    val sharesOutstanding: Long?,    // 경계 케이스: stock에 포함
    val metadata: CompanyInfoMetadata
)

data class SharesData(
    val date: LocalDate,
    val shares: Long
)
```

#### 메타데이터 모델
```kotlin
// CompanyInfoMetadata.kt
data class CompanyInfoMetadata(
    val symbol: String,
    val fetchedAt: Long,                  // Unix timestamp (millis)
    val source: String,                   // "YahooFinance"
    val modulesUsed: List<String>,        // ["assetProfile", "quoteType", ...]
    val dataCompleteness: DataCompleteness
)

data class DataCompleteness(
    val totalFields: Int,
    val populatedFields: Int,
    val completenessPercent: Double  // populatedFields / totalFields * 100
)
```

### TODO 2.2: StockService 인터페이스 및 구현
**파일**: `src/main/kotlin/com/ulalax/ufc/domain/stock/StockService.kt`

```kotlin
interface StockService {
    // 기본 정보
    suspend fun getCompanyInfo(symbol: String): CompanyInfo
    suspend fun getCompanyInfo(symbols: List<String>): Map<String, CompanyInfo>

    // 빠른 정보 (최소 필드)
    suspend fun getFastInfo(symbol: String): FastInfo
    suspend fun getFastInfo(symbols: List<String>): Map<String, FastInfo>

    // ISIN 조회
    suspend fun getIsin(symbol: String): String
    suspend fun getIsin(symbols: List<String>): Map<String, String>

    // 발행주식수 히스토리
    suspend fun getShares(symbol: String): List<SharesData>
    suspend fun getShares(symbols: List<String>): Map<String, List<SharesData>>
    suspend fun getSharesFull(
        symbol: String,
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<SharesData>

    // Raw 데이터 (디버깅/커스텀 파싱용)
    suspend fun getRawQuoteSummary(
        symbol: String,
        modules: List<String>
    ): QuoteSummaryResponse
}
```

**구현체**: `YahooStockService.kt`
- QuoteSummaryService를 내부적으로 사용
- **getFastInfo**: modules `["quoteType"]` (최소 호출)
- **getCompanyInfo**: modules `["assetProfile", "summaryProfile", "quoteType", "defaultKeyStatistics"]`
- **getIsin**: `quote-equity/v2/get-profile?symbol={symbol}` 엔드포인트 사용
- **getShares/getSharesFull**: `defaultKeyStatistics` 모듈에서 sharesOutstanding 추출

### TODO 2.3: StockService 캐싱 전략
**구현체에 캐시 추가:**

```kotlin
internal class YahooStockService(...) : StockService {
    // 캐시
    private val companyInfoCache = ConcurrentHashMap<String, CachedCompanyInfo>()
    private val fastInfoCache = ConcurrentHashMap<String, CachedFastInfo>()
    private val isinCache = ConcurrentHashMap<String, String>()  // 영구 캐시

    companion object {
        const val COMPANY_INFO_TTL_MILLIS = 86_400_000L  // 24시간
        const val MAX_CACHE_SIZE = 1000                   // LRU 최대 크기
        const val MAX_BATCH_SIZE = 50
        const val MAX_CONCURRENT_REQUESTS = 10
    }
}
```

**캐시 정책:**
| 데이터 | TTL | 이유 |
|--------|-----|------|
| CompanyInfo | 24시간 | 기본 정보는 자주 변경 안 됨 |
| FastInfo | 24시간 | 거래소/통화 정보 변경 거의 없음 |
| ISIN | 영구 | ISIN은 변경 안 됨 |

---

## Phase 3: ufc.funds 구현 `dev-ko-h`

> 참조: [ufc.funds 기술명세서](./ufc.funds-기술명세서.md)

### TODO 3.1: Funds 도메인 모델 생성
**경로**: `src/main/kotlin/com/ulalax/ufc/domain/funds/`

#### 메인 모델 (FundData - 10개 속성)
```kotlin
// FundData.kt
data class FundData(
    val symbol: String,
    val quoteType: String,                          // ETF | MUTUALFUND
    val description: String?,
    val fundOverview: FundOverview?,
    val fundOperations: FundOperations?,
    val assetClasses: AssetClasses?,
    val topHoldings: List<Holding>?,
    val equityHoldings: EquityHoldingsMetrics?,
    val bondHoldings: BondHoldingsMetrics?,
    val bondRatings: Map<String, Double>?,          // 채권 등급 분포
    val sectorWeightings: Map<String, Double>?
)
```

#### 세부 모델
```kotlin
// FundOverview.kt
data class FundOverview(
    val categoryName: String?,      // "Large Blend", "High Yield Bond"
    val family: String?,            // "Vanguard", "iShares"
    val legalType: String?          // "Exchange Traded Fund"
)

// FundOperations.kt
data class FundOperations(
    val annualReportExpenseRatio: OperationMetric?,
    val annualHoldingsTurnover: OperationMetric?,
    val totalNetAssets: OperationMetric?
)

data class OperationMetric(
    val fundValue: Double?,
    val categoryAverage: Double?
)

// AssetClasses.kt
data class AssetClasses(
    val cashPosition: Double?,
    val stockPosition: Double?,
    val bondPosition: Double?,
    val preferredPosition: Double?,
    val convertiblePosition: Double?,
    val otherPosition: Double?
) {
    fun totalAllocation(): Double =
        listOfNotNull(cashPosition, stockPosition, bondPosition,
            preferredPosition, convertiblePosition, otherPosition).sum()
}

// Holding.kt
data class Holding(
    val symbol: String,
    val name: String,
    val holdingPercent: Double
)

// EquityHoldingsMetrics.kt
data class EquityHoldingsMetrics(
    val priceToEarnings: MetricValue?,
    val priceToBook: MetricValue?,
    val priceToSales: MetricValue?,
    val priceToCashflow: MetricValue?,
    val medianMarketCap: MetricValue?,
    val threeYearEarningsGrowth: MetricValue?
)

// BondHoldingsMetrics.kt
data class BondHoldingsMetrics(
    val duration: MetricValue?,
    val maturity: MetricValue?,
    val creditQuality: MetricValue?
)

// MetricValue.kt
data class MetricValue(
    val fundValue: Double?,
    val categoryAverage: Double?
)
```

### TODO 3.2: FundsService 인터페이스 및 구현
**파일**: `src/main/kotlin/com/ulalax/ufc/domain/funds/FundsService.kt`

```kotlin
interface FundsService {
    // 통합 펀드 데이터
    suspend fun getFundData(symbol: String): FundData
    suspend fun getFundData(symbols: List<String>): Map<String, FundData>

    // Raw 응답
    suspend fun getRawFundData(symbol: String): FundDataResponse

    // 펀드 여부 확인 (ETF/MUTUALFUND 구분)
    suspend fun isFund(symbol: String): Boolean
}
```

**구현체**: `YahooFundsService.kt`
- QuoteSummaryService 사용
- modules: `["quoteType", "summaryProfile", "topHoldings", "fundProfile", "defaultKeyStatistics"]`
- **isFund()**: quoteType == "ETF" or "MUTUALFUND" 확인
- **9개 파싱 함수**: parseDescription(), parseFundOverview(), parseFundOperations(), parseAssetClasses(), parseTopHoldings(), parseEquityHoldings(), parseBondHoldings(), parseBondRatings(), parseSectorWeightings()
- 캐시: ConcurrentHashMap, TTL 없음 (펀드 정보는 거의 변경 안 됨)

---

## Phase 4: ufc.price 구현 `dev-ko-h` / `dev-ko`

> 참조: [ufc.price 기술명세서](./ufc.price-기술명세서.md)

### TODO 4.1: Price 도메인 모델 생성
**경로**: `src/main/kotlin/com/ulalax/ufc/domain/price/`

#### PriceData (현재 가격 정보 - 완전 모델)
```kotlin
// PriceData.kt
data class PriceData(
    // 기본 정보
    val symbol: String,
    val currency: String?,
    val exchange: String?,

    // 현재 가격
    val lastPrice: Double?,
    val regularMarketPrice: Double?,
    val regularMarketTime: Long?,

    // 일중 범위
    val open: Double?,
    val dayHigh: Double?,
    val dayLow: Double?,
    val previousClose: Double?,

    // 거래량
    val volume: Long?,
    val regularMarketVolume: Long?,
    val averageVolume: Long?,
    val averageVolume10days: Long?,

    // 52주 범위
    val fiftyTwoWeekHigh: Double?,
    val fiftyTwoWeekLow: Double?,
    val fiftyTwoWeekChange: Double?,
    val fiftyTwoWeekChangePercent: Double?,

    // 이동평균선
    val fiftyDayAverage: Double?,
    val twoHundredDayAverage: Double?,

    // 시가총액 + 배당
    val marketCap: Long?,
    val dividendYield: Double?,
    val dividendRate: Double?,
    val exDividendDate: Long?,

    // 기타
    val beta: Double?,
    val trailingPE: Double?,
    val forwardPE: Double?
) {
    /** 52주 범위 대비 현재 가격 위치 (0.0 ~ 1.0) */
    fun fiftyTwoWeekPosition(): Double? { /* 구현 */ }

    /** 일중 가격 변동폭 (%) */
    fun dailyChangePercent(): Double? { /* 구현 */ }

    /** 가격이 50일 이동평균선 위에 있는지 */
    fun isAbove50DayMA(): Boolean? { /* 구현 */ }

    /** 가격이 200일 이동평균선 위에 있는지 */
    fun isAbove200DayMA(): Boolean? { /* 구현 */ }
}
```

#### OHLCV (시계열 데이터 포인트)
```kotlin
// OHLCV.kt
data class OHLCV(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val adjClose: Double?,
    val volume: Long
) {
    fun range(): Double = high - low
    fun rangePercent(): Double { /* (high - low) / low * 100 */ }
    fun change(): Double = close - open
    fun changePercent(): Double { /* (close - open) / open * 100 */ }
    fun isBullish(): Boolean = close > open
    fun isBearish(): Boolean = close < open
}
```

#### ChartMetadata (차트 메타데이터)
```kotlin
// ChartMetadata.kt
data class ChartMetadata(
    val symbol: String,
    val currency: String?,
    val exchangeName: String?,
    val timezone: String?,
    val regularMarketPrice: Double?,
    val regularMarketTime: Long?,
    val dataGranularity: String?,   // "1d", "1h"
    val range: String?,              // "1y", "1mo"
    val validRanges: List<String>?
)
```

### TODO 4.2: PriceService 인터페이스 및 구현
**파일**: `src/main/kotlin/com/ulalax/ufc/domain/price/PriceService.kt`

```kotlin
interface PriceService {
    // 현재 가격
    suspend fun getCurrentPrice(symbol: String): PriceData
    suspend fun getCurrentPrice(symbols: List<String>): Map<String, PriceData>

    // 가격 히스토리 (Period 기반)
    suspend fun getPriceHistory(
        symbol: String,
        period: Period,
        interval: Interval = Interval.OneDay
    ): List<OHLCV>

    // 가격 히스토리 (날짜 범위 기반)
    suspend fun getPriceHistory(
        symbol: String,
        start: LocalDate,
        end: LocalDate,
        interval: Interval = Interval.OneDay
    ): List<OHLCV>

    // 가격 히스토리 + 메타데이터
    suspend fun getPriceHistoryWithMetadata(
        symbol: String,
        period: Period,
        interval: Interval = Interval.OneDay
    ): Pair<ChartMetadata, List<OHLCV>>

    // 메타데이터만 조회 (yfinance get_history_metadata 호환)
    suspend fun getHistoryMetadata(symbol: String): ChartMetadata

    // Raw 응답
    suspend fun getRawPrice(symbol: String): PriceResponse
    suspend fun getRawPriceHistory(
        symbol: String,
        period: Period,
        interval: Interval
    ): ChartResponse
}
```

**구현체**: `YahooPriceService.kt`
- **getCurrentPrice**: QuoteSummaryService 사용 (modules: `["price", "summaryDetail"]`)
- **getPriceHistory**: ChartService 위임
- **getHistoryMetadata**: Chart API meta 필드 추출
- **ChartService 의존성 주입**: 기존 ChartService 재사용

### TODO 4.3: Period/Interval 검증 로직
**파일**: `YahooPriceService.kt` 내부 메서드

**제약 조건:**
| Interval | 최대 Period |
|----------|-------------|
| 1m, 2m, 5m, 15m, 30m | 7일 |
| 60m, 90m, 1h | 60일 |
| 1d, 5d, 1wk, 1mo, 3mo | 제한 없음 |

```kotlin
private fun validatePeriodInterval(period: Period, interval: Interval) {
    val daysInPeriod = period.estimatedDays()
    when {
        interval.isIntraday() && interval.minutes < 60 && daysInPeriod > 7 ->
            throw InvalidPeriodIntervalException(
                "Intraday intervals (< 1h) can only query up to 7 days"
            )
        interval.minutes == 60 && daysInPeriod > 60 ->
            throw InvalidPeriodIntervalException(
                "Hourly interval can only query up to 60 days"
            )
    }
}
```

### TODO 4.4: PriceService 캐싱 전략
```kotlin
internal class YahooPriceService(...) : PriceService {
    private val priceCache = ConcurrentHashMap<String, CachedPriceData>()

    companion object {
        const val CURRENT_PRICE_TTL_MILLIS = 60_000L  // 1분 (현재 가격)
        const val MAX_CACHE_SIZE = 1000               // LRU
    }
}

internal data class CachedPriceData(
    val data: PriceData,
    val cachedAt: Long,
    val ttlMillis: Long
) {
    fun isExpired(currentTimeMillis: Long): Boolean =
        (currentTimeMillis - cachedAt) > ttlMillis
}
```

**캐시 정책:**
- **현재 가격**: 60초 TTL (가격은 실시간 변동)
- **히스토리**: 캐시 안 함 (조회 조건이 다양)

### TODO 4.5: 이동평균 계산 유틸리티 `dev-ko` (Sonnet 권장)
**파일**: `src/main/kotlin/com/ulalax/ufc/util/TechnicalIndicators.kt`

> 알고리즘 구현이 필요하므로 Sonnet 모델 권장

```kotlin
object TechnicalIndicators {
    fun calculateSMA(data: List<Double>, period: Int): List<Double>
    fun calculateEMA(data: List<Double>, period: Int): List<Double>
}
```

---

## Phase 5: ufc.corp 구현 `dev-ko-h`

> 참조: [ufc.corp 기술명세서](./ufc.corp-기술명세서.md)

### TODO 5.1: Corp 도메인 모델 생성
**경로**: `src/main/kotlin/com/ulalax/ufc/domain/corp/`

```kotlin
// CorpModels.kt
data class DividendHistory(
    val symbol: String,
    val dividends: List<Dividend>
)

data class Dividend(
    val date: Long,
    val amount: Double
)

data class SplitHistory(
    val symbol: String,
    val splits: List<Split>
)

data class Split(
    val date: Long,
    val numerator: Int,
    val denominator: Int,
    val ratio: String
)

data class CapitalGainHistory(
    val symbol: String,
    val capitalGains: List<CapitalGain>
)

data class CapitalGain(
    val date: Long,
    val amount: Double
)
```

### TODO 5.2: CorpService 인터페이스 및 구현
**파일**: `src/main/kotlin/com/ulalax/ufc/domain/corp/CorpService.kt`

```kotlin
interface CorpService {
    suspend fun getDividends(
        symbol: String,
        period: Period = Period.FiveYears
    ): DividendHistory

    suspend fun getSplits(
        symbol: String,
        period: Period = Period.Max
    ): SplitHistory

    suspend fun getCapitalGains(
        symbol: String,
        period: Period = Period.FiveYears
    ): CapitalGainHistory
}
```

**구현체**: `YahooCorpService.kt`
- ChartService의 getRawChartData(includeEvents=true) 사용
- chart.result.events에서 dividends, splits, capitalGains 추출
- **주의**: MUTUALFUND는 splits 미지원 (기술명세서 참조)

---

## Phase 6: ufc.macro 구현 `dev-ko`

> 참조: [ufc.macro 기술명세서](./ufc.macro-기술명세서.md)

### TODO 6.1: FRED API 클라이언트 생성
**경로**: `src/main/kotlin/com/ulalax/ufc/internal/fred/`

```kotlin
// FredApiUrls.kt
object FredApiUrls {
    const val BASE_URL = "https://api.stlouisfed.org/fred"
    const val SERIES_OBSERVATIONS = "$BASE_URL/series/observations"
    const val SERIES = "$BASE_URL/series"
}

// FredHttpClient.kt
class FredHttpClient(
    private val apiKey: String,
    private val httpClient: HttpClient,
    private val rateLimiter: RateLimiter
) {
    suspend fun getSeriesObservations(
        seriesId: String,
        startDate: String? = null,
        endDate: String? = null,
        frequency: String? = null
    ): FredObservationsResponse

    suspend fun getSeriesInfo(seriesId: String): FredSeriesResponse
}
```

### TODO 6.2: FRED 응답 모델
**파일**: `src/main/kotlin/com/ulalax/ufc/internal/fred/FredResponse.kt`

```kotlin
@Serializable
data class FredObservationsResponse(
    val observations: List<FredObservation>
)

@Serializable
data class FredObservation(
    val date: String,
    val value: String  // "." for missing data
)

@Serializable
data class FredSeriesResponse(
    val spieces: List<FredSeriesInfo>
)

@Serializable
data class FredSeriesInfo(
    val id: String,
    val title: String,
    val frequency: String,
    val units: String
)
```

### TODO 6.3: Macro 도메인 모델
**경로**: `src/main/kotlin/com/ulalax/ufc/domain/macro/`

```kotlin
// MacroModels.kt
data class MacroSeries(
    val seriesId: String,
    val title: String,
    val frequency: String,
    val units: String,
    val data: List<MacroDataPoint>
)

data class MacroDataPoint(
    val date: String,    // YYYY-MM-DD
    val value: Double?   // null for missing
)

// 미리 정의된 시리즈 ID
object FredSeriesIds {
    // GDP
    const val GDP = "GDP"
    const val REAL_GDP = "GDPC1"
    const val GDP_GROWTH = "A191RL1Q225SBEA"

    // Unemployment
    const val UNEMPLOYMENT_RATE = "UNRATE"
    const val INITIAL_CLAIMS = "ICSA"
    const val NONFARM_PAYROLL = "PAYEMS"

    // Inflation
    const val CPI = "CPIAUCSL"
    const val CORE_CPI = "CPILFESL"
    const val PCE = "PCEPI"
    const val CORE_PCE = "PCEPILFE"

    // Interest Rates
    const val FED_FUNDS_RATE = "FEDFUNDS"
    const val TREASURY_10Y = "DGS10"
    const val TREASURY_2Y = "DGS2"

    // Housing
    const val CASE_SHILLER = "CSUSHPINSA"
    const val HOUSING_STARTS = "HOUST"

    // Consumer Sentiment
    const val MICHIGAN_SENTIMENT = "UMCSENT"
    const val CONSUMER_CONFIDENCE = "CSCICP03USM665S"

    // Money & Finance
    const val M2 = "M2SL"
    const val VIX = "VIXCLS"
}
```

### TODO 6.4: MacroService 인터페이스 및 구현
**파일**: `src/main/kotlin/com/ulalax/ufc/domain/macro/MacroService.kt`

```kotlin
interface MacroService {
    // 범용 조회
    suspend fun getSeries(
        seriesId: String,
        startDate: String? = null,
        endDate: String? = null
    ): MacroSeries

    // 편의 메서드
    suspend fun getGDP(): MacroSeries
    suspend fun getUnemploymentRate(): MacroSeries
    suspend fun getCPI(): MacroSeries
    suspend fun getFedFundsRate(): MacroSeries
    // ... 기타 주요 지표
}
```

**구현체**: `FredMacroService.kt`

### TODO 6.5: Rate Limiter 설정 추가
**파일**: `src/main/kotlin/com/ulalax/ufc/infrastructure/ratelimit/RateLimitConfig.kt`

FRED API용 Rate Limit 설정 추가:
```kotlin
data class RateLimitingSettings(
    val yahoo: RateLimitConfig = RateLimitConfig(...),
    val fred: RateLimitConfig = RateLimitConfig(
        requestsPerSecond = 10.0,  // FRED API 제한
        burstCapacity = 20
    )
)
```

---

## Phase 7: UFCClient 통합 `dev-ko`

### TODO 7.1: UFCClientImpl 확장
**파일**: `src/main/kotlin/com/ulalax/ufc/client/UFCClientImpl.kt`

새 서비스 추가:
```kotlin
class UFCClientImpl(
    // 기존
    private val chartService: ChartService,
    private val quoteService: QuoteSummaryService,
    private val yahooRateLimiter: TokenBucketRateLimiter,
    // 새로 추가
    private val stockService: StockService,
    private val fundsService: FundsService,
    private val priceService: PriceService,
    private val corpService: CorpService,
    private val macroService: MacroService?  // FRED API 키 필요
) : AutoCloseable {

    // ========================================
    // Stock API Methods (ufc.stock)
    // ========================================
    suspend fun getFastInfo(symbol: String): FastInfo
    suspend fun getCompanyInfo(symbol: String): CompanyInfo

    // ========================================
    // Funds API Methods (ufc.funds)
    // ========================================
    suspend fun getFundInfo(symbol: String): FundInfo
    suspend fun getHoldings(symbol: String): FundHoldings
    suspend fun getSectorWeightings(symbol: String): SectorWeightings

    // ========================================
    // Price API Methods (ufc.price)
    // ========================================
    suspend fun getCurrentPrice(symbol: String): CurrentPrice
    suspend fun getPriceHistory(...): PriceHistory
    suspend fun getMovingAverage(...): MovingAverage

    // ========================================
    // Corp API Methods (ufc.corp)
    // ========================================
    suspend fun getDividends(...): DividendHistory
    suspend fun getSplits(...): SplitHistory
    suspend fun getCapitalGains(...): CapitalGainHistory

    // ========================================
    // Macro API Methods (ufc.macro)
    // ========================================
    suspend fun getMacroSeries(seriesId: String, ...): MacroSeries
    suspend fun getGDP(): MacroSeries
    suspend fun getUnemploymentRate(): MacroSeries
    // ... 기타
}
```

### TODO 7.2: UFCClientImpl.create() 팩토리 업데이트

```kotlin
suspend fun create(config: UFCClientConfig): UFCClientImpl {
    // ... 기존 코드 ...

    // 새 서비스 인스턴스 생성
    val stockService = YahooStockService(quoteService)
    val fundsService = YahooFundsService(quoteService)
    val priceService = YahooPriceService(quoteService, chartService)
    val corpService = YahooCorpService(chartService)

    // FRED 서비스 (API 키 있을 때만)
    val macroService = config.fredApiKey?.let { apiKey ->
        val fredRateLimiter = TokenBucketRateLimiter("FRED", config.rateLimitingSettings.fred)
        val fredClient = FredHttpClient(apiKey, httpClient, fredRateLimiter)
        FredMacroService(fredClient)
    }

    return UFCClientImpl(
        httpClient, config,
        chartService, quoteService, yahooRateLimiter,
        stockService, fundsService, priceService, corpService, macroService
    )
}
```

---

## 테스트 전략

> 상세한 테스트 원칙은 [test-principle.md](../../doc/test-principle.md) 참조

### 테스트 유형별 파일 명명
- 단위 테스트: `*Test.kt` (예: `YahooStockServiceTest.kt`)
- 통합 테스트: `*Spec.kt` (예: `StockServiceSpec.kt`)
- 실시간 테스트: `*LiveTest.kt` (예: `MacroServiceLiveTest.kt`)

### Fake 객체 활용
각 서비스에 대해 Fake 구현체 생성:
```kotlin
// FakeQuoteSummaryService.kt
class FakeQuoteSummaryService : QuoteSummaryService {
    private val responses = mutableMapOf<String, QuoteSummaryResult>()

    fun addResponse(symbol: String, result: QuoteSummaryResult) {
        responses[symbol] = result
    }

    override suspend fun getQuoteSummary(symbol: String): QuoteSummaryResult {
        return responses[symbol] ?: throw UfcException(...)
    }
}
```

### 테스트 디렉토리 구조
```
src/test/kotlin/com/ulalax/ufc/
├── domain/
│   ├── stock/
│   │   ├── YahooStockServiceTest.kt    # 단위 테스트
│   │   └── StockServiceSpec.kt         # 통합 테스트
│   ├── funds/
│   ├── price/
│   ├── corp/
│   └── macro/
├── fake/
│   ├── FakeQuoteSummaryService.kt
│   ├── FakeChartService.kt
│   └── FakeFredHttpClient.kt
└── integration/
    └── UFCClientIntegrationSpec.kt
```

### 픽스쳐 파일 구조
```
src/test/resources/fixtures/
├── stock/
│   ├── quoteSummary/
│   │   ├── AAPL_equity_full.json       # EQUITY 완전 데이터
│   │   ├── SPY_etf.json                # ETF
│   │   ├── VTSAX_mutualfund.json       # MUTUALFUND
│   │   ├── ^GSPC_index.json            # INDEX
│   │   └── BTC-USD_crypto.json         # CRYPTOCURRENCY
│   └── error/
│       ├── invalid_symbol.json
│       └── rate_limited.json
├── funds/
│   ├── SPY_etf_full.json               # 주식형 ETF
│   ├── AGG_bond_etf.json               # 채권형 ETF
│   ├── JNK_high_yield.json             # 고수익 채권
│   └── VTSAX_mutual.json               # 뮤추얼펀드
├── price/
│   ├── quoteSummary/
│   │   ├── AAPL_price_full.json
│   │   └── SPY_price.json
│   └── chart/
│       ├── AAPL_1y_1d.json
│       ├── AAPL_5d_1m.json
│       └── AAPL_1mo_1h.json
├── corp/
│   ├── AAPL_dividends.json
│   ├── TSLA_splits.json
│   └── VTSAX_capital_gains.json
└── macro/
    ├── GDP_observations.json
    ├── UNRATE_observations.json
    └── error/
        └── invalid_series.json
```

### 자산 유형별 테스트 시나리오

**ufc.stock 테스트 픽스쳐:**
| Symbol | 타입 | 목적 | 예상 DataCompleteness |
|--------|------|------|----------------------|
| AAPL | EQUITY | 완전한 데이터 | > 90% |
| SPY | ETF | sector, industry null | 60-80% |
| VTSAX | MUTUALFUND | 제한적 정보 | 40-60% |
| ^GSPC | INDEX | 최소한의 정보 | 30-50% |
| BTC-USD | CRYPTO | 기본 정보만 | 30-40% |

**ufc.funds 테스트 픽스쳐:**
| Symbol | 타입 | 목적 |
|--------|------|------|
| SPY | ETF | 주식형 ETF (S&P 500) |
| AGG | ETF | 채권형 ETF |
| JNK | ETF | 고수익 채권 ETF (bondRatings 테스트) |
| VTI | ETF | Total Market ETF |
| VTSAX | MUTUALFUND | 뮤추얼펀드 |

**ufc.price 테스트 시나리오:**
| 테스트 | 입력 | 예상 출력 |
|--------|------|----------|
| getCurrentPrice_success | "AAPL" | PriceData (정상) |
| getCurrentPrice_cached | "AAPL" (2회) | 두 번째 호출은 캐시 |
| getCurrentPrice_cacheExpired | TTL 초과 후 | API 재호출 |
| getPriceHistory_period | 1y, 1d | List<OHLCV> |
| getPriceHistory_dateRange | start, end | List<OHLCV> |
| getPriceHistory_invalidInterval | 1y, 1m | InvalidPeriodIntervalException |
| getHistoryMetadata | "AAPL" | ChartMetadata |

### Live Test 기록 패턴
실제 API 응답 기록 활용 (기존 패턴 참조):
```kotlin
@Test
@Tag("live")
fun `record live response for stock info`() = runTest {
    ResponseRecordingContext.startRecording("stock_fast_info")
    try {
        client.getFastInfo("AAPL")
    } finally {
        ResponseRecordingContext.stopRecording()
    }
}
```

**Live Test 자동 녹화 후 픽스쳐 변환:**
```kotlin
@Test
@Tag("live")
fun `record and save fixture`() = runBlocking {
    val service = createRealPriceService()
    val priceData = service.getCurrentPrice("AAPL")

    // 응답 녹화 후 JSON으로 저장
    responseRecorder.save("AAPL_live", priceData)
}
```

---

## 체크리스트

### Phase 1: 기반 리팩토링 `dev-ko-h`
- [ ] QuoteSummaryResponse.kt에 새 모듈 데이터 클래스 추가
- [ ] QuoteSummaryResult에 새 필드 추가
- [ ] ChartDataResponse.kt에 events 필드 추가
- [ ] YahooChartService에 events 파라미터 지원
- [ ] ChartService 인터페이스 업데이트
- [ ] **ErrorCode.kt 확장** (새 에러 코드 추가)

### Phase 2: ufc.stock `dev-ko-h`
- [ ] AssetType.kt 생성 (열거형)
- [ ] StockModels.kt 생성 (FastInfo, CompanyInfo, SharesData)
- [ ] CompanyInfoMetadata.kt 생성 (DataCompleteness 포함)
- [ ] StockService 인터페이스 생성
- [ ] YahooStockService 구현
  - [ ] getCompanyInfo() 구현
  - [ ] getFastInfo() 구현
  - [ ] getIsin() 구현
  - [ ] getShares() / getSharesFull() 구현
  - [ ] 캐싱 로직 구현 (24h TTL, LRU)
- [ ] FakeStockService 생성
- [ ] YahooStockServiceTest 작성
- [ ] 자산 유형별 픽스쳐 생성 (EQUITY, ETF, MUTUALFUND, INDEX, CRYPTO)

### Phase 3: ufc.funds `dev-ko-h`
- [ ] FundData.kt 생성 (메인 모델)
- [ ] FundOverview.kt, FundOperations.kt 생성
- [ ] AssetClasses.kt, Holding.kt 생성
- [ ] EquityHoldingsMetrics.kt, BondHoldingsMetrics.kt 생성
- [ ] MetricValue.kt, OperationMetric.kt 생성
- [ ] FundsService 인터페이스 생성 (isFund 포함)
- [ ] YahooFundsService 구현
  - [ ] getFundData() 구현
  - [ ] isFund() 구현
  - [ ] 9개 파싱 함수 구현
- [ ] FakeFundsService 생성
- [ ] YahooFundsServiceTest 작성
- [ ] 펀드 유형별 픽스쳐 생성 (주식형 ETF, 채권형 ETF, 뮤추얼펀드)

### Phase 4: ufc.price `dev-ko-h` / `dev-ko`
- [ ] PriceData.kt 생성 (유틸리티 메서드 포함)
- [ ] OHLCV.kt 생성 (유틸리티 메서드 포함)
- [ ] ChartMetadata.kt 생성
- [ ] PriceService 인터페이스 생성
- [ ] YahooPriceService 구현
  - [ ] getCurrentPrice() 구현
  - [ ] getPriceHistory() - Period 기반
  - [ ] getPriceHistory() - 날짜 범위 기반
  - [ ] getPriceHistoryWithMetadata() 구현
  - [ ] getHistoryMetadata() 구현
  - [ ] Period/Interval 검증 로직
  - [ ] 캐싱 로직 구현 (60s TTL)
- [ ] TechnicalIndicators 유틸리티 생성 ← `dev-ko` (Sonnet)
- [ ] FakePriceService 생성
- [ ] YahooPriceServiceTest 작성

### Phase 5: ufc.corp `dev-ko-h`
- [ ] CorpModels.kt 생성
- [ ] CorpService 인터페이스 생성
- [ ] YahooCorpService 구현
- [ ] FakeCorpService 생성
- [ ] YahooCorpServiceTest 작성

### Phase 6: ufc.macro `dev-ko`
- [ ] FredApiUrls.kt 생성
- [ ] FredResponse.kt 생성
- [ ] FredHttpClient.kt 생성
- [ ] MacroModels.kt 생성
- [ ] FredSeriesIds.kt 생성 (미리 정의된 시리즈 ID)
- [ ] MacroService 인터페이스 생성
- [ ] FredMacroService 구현
- [ ] RateLimitConfig에 FRED 설정 추가
- [ ] FakeFredHttpClient 생성
- [ ] FredMacroServiceTest 작성

### Phase 7: 통합 `dev-ko`
- [ ] UFCClientImpl 확장 (모든 서비스 통합)
- [ ] UFCClientImpl.create() 업데이트
- [ ] UFCClient 통합 테스트 작성
- [ ] 전체 Live Test 실행 및 검증
- [ ] 최종 문서 업데이트

---

## 참고사항

### Response/Domain 모델 분리 패턴

**핵심 원칙:**
- **Response 모델 (Internal)**: API 응답 그대로 매핑, `@Serializable`, 외부 노출 금지
- **Domain 모델 (Public)**: 정규화된 비즈니스 모델, 사용자에게 노출

```
Yahoo Finance API 응답
        ↓
Response 모델 (Internal)
        ↓ 변환 로직 (private 메서드)
Domain 모델 (Public)
        ↓
사용자 코드
```

**예시 (ufc.price):**
| Response 모델 | Domain 모델 | 위치 |
|--------------|-------------|------|
| `PriceModuleRaw` | `PriceData` | `internal/yahoo/response/` → `model/price/` |
| `SummaryDetailRaw` | `PriceData` | 병합됨 |
| `ChartResult` | `List<OHLCV>` | `domain/chart/` → `model/price/` |

**변환 위치:**
- Service 구현체 내부 (`YahooPriceService`, `YahooStockService` 등)
- private 메서드로 캡슐화

### Yahoo Finance API 모듈 목록
```
price, summaryDetail, financialData, quoteType, assetProfile,
topHoldings, fundProfile, earningsTrend, earningsHistory,
earningsDates, majorHolders, insiderTransactions,
incomeStatementHistory, balanceSheetHistory, cashflowStatementHistory,
defaultKeyStatistics, summaryProfile
```

### Chart API events 파라미터
```
?events=div          # 배당금만
?events=splits       # 분할만
?events=capitalGains # 자본이득만 (뮤추얼펀드)
?events=div,splits   # 복수 가능
```

### FRED API 응답 형식
- 날짜: `YYYY-MM-DD`
- 값: 문자열 (`.`은 결측치)
- 빈도: `d` (일), `w` (주), `m` (월), `q` (분기), `a` (연)

### 캐싱 전략 요약

| 네임스페이스 | 캐시 대상 | TTL | 전략 |
|------------|----------|-----|------|
| ufc.stock | CompanyInfo, FastInfo | 24시간 | LRU (max 1000) |
| ufc.stock | ISIN | 영구 | - |
| ufc.funds | FundData | 무제한 | 서비스 재시작 시 초기화 |
| ufc.price | CurrentPrice | 60초 | LRU (max 1000) |
| ufc.price | PriceHistory | 캐시 안 함 | 조회 조건 다양 |
| ufc.corp | - | 캐시 안 함 | 이벤트 데이터 |
| ufc.macro | - | 캐시 안 함 | 실시간 필요 |
