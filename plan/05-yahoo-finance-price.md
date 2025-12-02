# Yahoo Finance Price - 가격 데이터 명세

## 문서 정보
- **버전**: 2.0.0
- **최종 수정일**: 2025-12-01
- **작성자**: Claude Code (Software Architect)
- **문서 상태**: Active

---

## 1. Ticker 클래스

```kotlin
class Ticker(
    val symbol: String,
    private val client: YahooFinanceClient
) {
    /**
     * 가격 히스토리 조회
     */
    suspend fun history(
        period: Period? = null,
        interval: Interval = Interval.OneDay,
        start: LocalDate? = null,
        end: LocalDate? = null,
        actions: Boolean = true
    ): List<PriceBar>

    /**
     * 배당 정보
     */
    suspend fun dividends(
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<Dividend>

    /**
     * 주식 분할 정보
     */
    suspend fun splits(
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<Split>

    /**
     * 종목 정보
     */
    suspend fun info(): TickerInfo
}
```

---

## 2. Chart API 파라미터 명세

### 2.1 엔드포인트

```
GET /v8/finance/chart/{symbol}
```

### 2.2 필수 파라미터

모든 파라미터는 선택적(optional)이며, 기본값이 설정되어 있습니다.

### 2.3 파라미터 상세

| 파라미터 | 타입 | 설명 | 기본값 | 예시 |
|---------|------|------|--------|------|
| period1 | Long | 시작 시간 (Unix timestamp) | - | 1609459200 |
| period2 | Long | 종료 시간 (Unix timestamp) | 현재 시간 | 1640995200 |
| interval | Enum | 데이터 간격 (Interval) | 1d | 1d, 1h, 5m |
| range | Enum | 기간 범위 (Range) | - | 1mo, 1y, 5y |
| events | Enum | 포함할 이벤트 (Events) | - | div, splits, div,splits |
| includePrePost | Boolean | 장전/장후 거래 포함 | false | true, false |
| includeAdjustedClose | Boolean | 조정 종가 포함 | true | true, false |

**참고:**
- `period1`/`period2`와 `range`는 상호 배타적입니다. 둘 중 하나만 사용하세요.
- `period1`/`period2`를 사용하면 정확한 날짜 범위를 지정할 수 있습니다.
- `range`를 사용하면 미리 정의된 기간을 사용할 수 있습니다.

---

## 3. 가격 데이터 모델

### 3.1 PriceBar

```kotlin
/**
 * PriceBar (가격 바 데이터)
 *
 * 주가 데이터의 기본 단위입니다. OHLCV (Open, High, Low, Close, Volume) 형식입니다.
 */
data class PriceBar(
    val date: LocalDateTime,         // 날짜/시간
    val open: Double?,               // 시가 (Opening Price)
    val high: Double?,               // 고가 (High Price)
    val low: Double?,                // 저가 (Low Price)
    val close: Double?,              // 종가 (Closing Price)
    val adjClose: Double?,           // 조정 종가 (Adjusted Close, 배당/분할 반영)
    val volume: Long?                // 거래량 (Volume)
)
```

### 3.2 Interval Enum

```kotlin
/**
 * Chart API 간격
 *
 * 데이터 포인트 간의 시간 간격을 정의합니다.
 */
enum class Interval(val value: String, val description: String) {
    ONE_MINUTE("1m", "1 Minute"),                   // 1분 (Intraday, 최근 30일)
    TWO_MINUTES("2m", "2 Minutes"),                 // 2분 (Intraday, 최근 60일)
    FIVE_MINUTES("5m", "5 Minutes"),                // 5분 (Intraday, 최근 60일)
    FIFTEEN_MINUTES("15m", "15 Minutes"),           // 15분 (Intraday, 최근 60일)
    THIRTY_MINUTES("30m", "30 Minutes"),            // 30분 (Intraday, 최근 60일)
    SIXTY_MINUTES("60m", "60 Minutes"),             // 60분 (Intraday, 최근 730일)
    NINETY_MINUTES("90m", "90 Minutes"),            // 90분 (Intraday, 최근 60일)
    ONE_HOUR("1h", "1 Hour"),                       // 1시간 (Intraday, 최근 730일)
    ONE_DAY("1d", "1 Day"),                         // 1일 (Daily, 제한 없음)
    FIVE_DAYS("5d", "5 Days"),                      // 5일 (Weekly)
    ONE_WEEK("1wk", "1 Week"),                      // 1주 (Weekly, 제한 없음)
    ONE_MONTH("1mo", "1 Month"),                    // 1개월 (Monthly, 제한 없음)
    THREE_MONTHS("3mo", "3 Months")                 // 3개월 (Quarterly, 제한 없음)
}
```

**Interval 제약사항:**
- **1분 간격**: 최근 30일 데이터만 제공
- **5분, 15분, 30분, 90분 간격**: 최근 60일 데이터만 제공
- **60분, 1시간 간격**: 최근 730일(약 2년) 데이터만 제공
- **1일, 1주, 1개월, 3개월 간격**: 전체 기간 데이터 제공

### 3.3 Range Enum

```kotlin
/**
 * Chart API 범위
 *
 * 미리 정의된 기간 범위입니다.
 */
enum class Range(val value: String, val description: String) {
    ONE_DAY("1d", "1 Day"),                         // 1일
    FIVE_DAYS("5d", "5 Days"),                      // 5일
    ONE_MONTH("1mo", "1 Month"),                    // 1개월
    THREE_MONTHS("3mo", "3 Months"),                // 3개월
    SIX_MONTHS("6mo", "6 Months"),                  // 6개월
    ONE_YEAR("1y", "1 Year"),                       // 1년
    TWO_YEARS("2y", "2 Years"),                     // 2년
    FIVE_YEARS("5y", "5 Years"),                    // 5년
    TEN_YEARS("10y", "10 Years"),                   // 10년
    YEAR_TO_DATE("ytd", "Year to Date"),            // 연초 대비
    MAX("max", "Maximum Available")                 // 전체 기간
}
```

### 3.4 Events Enum

```kotlin
/**
 * Chart API 이벤트 타입
 *
 * 차트 데이터에 포함할 기업 행동(corporate actions)을 정의합니다.
 */
enum class Events(val value: String, val description: String) {
    DIVIDENDS("div", "Dividends"),                  // 배당
    SPLITS("splits", "Stock Splits"),               // 주식 분할
    CAPITAL_GAINS("capitalGains", "Capital Gains"), // 자본 이득 (뮤추얼 펀드)
    ALL("div,splits", "All Events")                 // 모든 이벤트
}
```

**참고:**
- `DIVIDENDS`: 배당금 지급 정보
- `SPLITS`: 주식 분할 정보 (예: 2-for-1 split)
- `CAPITAL_GAINS`: 뮤추얼 펀드의 자본 이득 분배
- `ALL`: 배당 + 분할 (가장 일반적으로 사용)

### 3.5 ChartParams

```kotlin
/**
 * Chart API 파라미터
 *
 * Chart API 요청 시 사용되는 파라미터를 캡슐화합니다.
 */
data class ChartParams(
    val period1: Long? = null,                      // 시작 시간 (Unix timestamp)
    val period2: Long? = null,                      // 종료 시간 (Unix timestamp)
    val interval: Interval = Interval.ONE_DAY,      // 간격
    val range: Range? = null,                       // 범위 (period1/period2 대신 사용)
    val events: Events = Events.ALL,                // 이벤트
    val includePrePost: Boolean = false,            // 장전/장후 거래 포함 여부
    val includeAdjustedClose: Boolean = true        // 조정 종가 포함 여부
) {
    companion object {
        /**
         * Period와 LocalDate로부터 ChartParams 생성
         */
        fun from(
            period: Period? = null,
            interval: Interval = Interval.ONE_DAY,
            start: LocalDate? = null,
            end: LocalDate? = null
        ): ChartParams {
            return when {
                period != null -> ChartParams(
                    range = period.toRange(),
                    interval = interval
                )
                start != null || end != null -> ChartParams(
                    period1 = start?.atStartOfDay(ZoneId.of("America/New_York"))
                        ?.toEpochSecond(),
                    period2 = end?.atTime(23, 59, 59)
                        ?.atZone(ZoneId.of("America/New_York"))
                        ?.toEpochSecond(),
                    interval = interval
                )
                else -> ChartParams(interval = interval)
            }
        }
    }

    /**
     * HttpRequestBuilder에 파라미터 적용
     */
    fun applyTo(builder: HttpRequestBuilder) {
        period1?.let { builder.parameter("period1", it) }
        period2?.let { builder.parameter("period2", it) }
        range?.let { builder.parameter("range", it.value) }
        builder.parameter("interval", interval.value)
        builder.parameter("events", events.value)
        if (includePrePost) {
            builder.parameter("includePrePost", "true")
        }
        if (includeAdjustedClose) {
            builder.parameter("includeAdjustedClose", "true")
        }
    }
}
```

### 3.6 Period Helper Enum

```kotlin
/**
 * 기간 헬퍼 Enum
 *
 * 사용자 친화적인 기간 표현을 Range로 변환합니다.
 */
enum class Period(val value: String) {
    ONE_DAY("1d"),
    FIVE_DAYS("5d"),
    ONE_MONTH("1mo"),
    THREE_MONTHS("3mo"),
    SIX_MONTHS("6mo"),
    ONE_YEAR("1y"),
    TWO_YEARS("2y"),
    FIVE_YEARS("5y"),
    TEN_YEARS("10y"),
    YEAR_TO_DATE("ytd"),
    MAX("max");

    /**
     * Period를 Range로 변환
     */
    fun toRange(): Range {
        return when (this) {
            ONE_DAY -> Range.ONE_DAY
            FIVE_DAYS -> Range.FIVE_DAYS
            ONE_MONTH -> Range.ONE_MONTH
            THREE_MONTHS -> Range.THREE_MONTHS
            SIX_MONTHS -> Range.SIX_MONTHS
            ONE_YEAR -> Range.ONE_YEAR
            TWO_YEARS -> Range.TWO_YEARS
            FIVE_YEARS -> Range.FIVE_YEARS
            TEN_YEARS -> Range.TEN_YEARS
            YEAR_TO_DATE -> Range.YEAR_TO_DATE
            MAX -> Range.MAX
        }
    }
}
```

---

## 4. Adjusted Price 계산

### 4.1 Adjusted Close 개념

**Adjusted Close (조정 종가)**는 배당금 지급과 주식 분할을 고려하여 과거 주가를 조정한 값입니다. 이를 통해 시간에 따른 순수한 가격 변화를 정확하게 분석할 수 있습니다.

**조정이 필요한 이유:**
- **배당금 지급**: 배당 지급일에 주가가 배당금만큼 하락하므로, 과거 가격을 배당금만큼 조정
- **주식 분할**: 분할 시 주가가 비례하여 하락하므로, 과거 가격을 분할 비율로 조정

### 4.2 조정 가격 계산 공식

Yahoo Finance는 다음 공식을 사용하여 과거 주가를 조정합니다:

```kotlin
/**
 * Adjusted Close 계산 알고리즘
 *
 * 최신 날짜부터 역순으로 계산하며, 배당과 분할을 모두 고려합니다.
 */
fun calculateAdjustedPrices(
    prices: List<PriceBar>,
    dividends: Map<LocalDate, Dividend>,
    splits: Map<LocalDate, Split>
): List<PriceBar> {
    if (prices.isEmpty()) return emptyList()

    val sortedPrices = prices.sortedByDescending { it.date }
    val adjustedPrices = mutableListOf<PriceBar>()

    // 누적 조정 계수 (최신 날짜 = 1.0)
    var cumulativeAdjustmentFactor = 1.0

    sortedPrices.forEachIndexed { index, priceBar ->
        val currentDate = priceBar.date.toLocalDate()

        // 1. 배당 조정
        dividends[currentDate]?.let { dividend ->
            val close = priceBar.close ?: return@let
            // Adjustment factor = (Close - Dividend) / Close
            val dividendFactor = (close - dividend.amount) / close
            cumulativeAdjustmentFactor *= dividendFactor
        }

        // 2. 주식 분할 조정
        splits[currentDate]?.let { split ->
            // Adjustment factor = 1 / Split Ratio
            // 예: 2-for-1 분할 (ratio = 2.0) → factor = 0.5
            val splitFactor = 1.0 / split.ratio
            cumulativeAdjustmentFactor *= splitFactor
        }

        // 3. 모든 가격 필드에 누적 조정 계수 적용
        val adjustedBar = priceBar.copy(
            open = priceBar.open?.let { it * cumulativeAdjustmentFactor },
            high = priceBar.high?.let { it * cumulativeAdjustmentFactor },
            low = priceBar.low?.let { it * cumulativeAdjustmentFactor },
            close = priceBar.close?.let { it * cumulativeAdjustmentFactor },
            adjClose = priceBar.close?.let { it * cumulativeAdjustmentFactor }
        )

        adjustedPrices.add(adjustedBar)
    }

    // 원래 순서로 복원 (오래된 날짜 → 최신 날짜)
    return adjustedPrices.reversed()
}
```

### 4.3 계산 예시

**시나리오:**
- 2024-01-01: Close = $100
- 2024-06-01: 배당 $2 지급
- 2024-09-01: 2-for-1 주식 분할
- 2024-12-01: Close = $60 (최신)

**계산 과정 (역순):**

```
2024-12-01: Close = $60
  → Adjusted = $60 * 1.0 = $60.00
  → cumulative_factor = 1.0

2024-09-01: Split 2:1 발생
  → split_factor = 1 / 2.0 = 0.5
  → cumulative_factor = 1.0 * 0.5 = 0.5

2024-06-01: Dividend $2 지급, Close = $120 (분할 전)
  → dividend_factor = (120 - 2) / 120 = 0.9833
  → cumulative_factor = 0.5 * 0.9833 = 0.4917

2024-01-01: Close = $100
  → Adjusted = $100 * 0.4917 = $49.17
```

**결과:**
- 2024-01-01부터 2024-12-01까지 순수 수익률 = ($60 - $49.17) / $49.17 = 22.0%

### 4.4 auto_adjust 옵션

```kotlin
/**
 * 가격 이력 조회 시 자동 조정 옵션
 *
 * @param symbol 주식 심볼
 * @param period 조회 기간
 * @param interval 데이터 간격
 * @param autoAdjust true일 경우 모든 가격 필드를 조정, false일 경우 adjClose만 제공
 * @return 가격 이력 데이터
 */
suspend fun history(
    symbol: String,
    period: Period = Period.OneYear,
    interval: Interval = Interval.OneDay,
    autoAdjust: Boolean = false
): List<PriceBar>
```

**autoAdjust = false (기본값):**
```kotlin
PriceBar(
    date = 2024-01-01,
    open = 100.0,      // 원본 가격
    high = 105.0,      // 원본 가격
    low = 98.0,        // 원본 가격
    close = 100.0,     // 원본 가격
    adjClose = 49.17,  // 조정 종가만 제공
    volume = 1000000
)
```

**autoAdjust = true:**
```kotlin
PriceBar(
    date = 2024-01-01,
    open = 49.17,      // 조정된 시가
    high = 51.63,      // 조정된 고가
    low = 48.18,       // 조정된 저가
    close = 49.17,     // 조정된 종가
    adjClose = 49.17,  // 조정 종가
    volume = 1000000   // 거래량은 조정 안 함
)
```

### 4.5 실제 구현 예시

```kotlin
/**
 * Chart API 응답을 PriceBar 리스트로 변환
 */
fun ChartResult.toPriceBars(autoAdjust: Boolean = false): List<PriceBar> {
    val timestamps = this.timestamp
    val quotes = this.indicators.quote.firstOrNull() ?: return emptyList()
    val adjCloseList = this.indicators.adjclose?.firstOrNull()?.adjclose

    val priceBars = timestamps.indices.map { i ->
        val timestamp = Instant.ofEpochSecond(timestamps[i])
        val date = LocalDateTime.ofInstant(timestamp, ZoneId.of("America/New_York"))

        PriceBar(
            date = date,
            open = quotes.open.getOrNull(i),
            high = quotes.high.getOrNull(i),
            low = quotes.low.getOrNull(i),
            close = quotes.close.getOrNull(i),
            adjClose = adjCloseList?.getOrNull(i),
            volume = quotes.volume.getOrNull(i)
        )
    }

    if (!autoAdjust) {
        return priceBars
    }

    // autoAdjust = true일 경우, adjClose를 기준으로 모든 가격 조정
    return priceBars.map { bar ->
        if (bar.close == null || bar.adjClose == null || bar.close == 0.0) {
            bar
        } else {
            val adjustmentFactor = bar.adjClose / bar.close
            bar.copy(
                open = bar.open?.let { it * adjustmentFactor },
                high = bar.high?.let { it * adjustmentFactor },
                low = bar.low?.let { it * adjustmentFactor },
                close = bar.adjClose,
                adjClose = bar.adjClose
            )
        }
    }
}
```

### 4.6 사용 예시

```kotlin
suspend fun compareAdjustedVsRaw() {
    val ufc = UFCClient.create()
    val aapl = ufc.stock.ticker("AAPL")

    // 원본 가격 (adjClose만 조정됨)
    val rawPrices = aapl.history(
        period = Period.FiveYears,
        autoAdjust = false
    )

    // 모든 가격 필드 조정
    val adjustedPrices = aapl.history(
        period = Period.FiveYears,
        autoAdjust = true
    )

    // 수익률 계산 (조정 가격 사용)
    val firstBar = adjustedPrices.first()
    val lastBar = adjustedPrices.last()
    val totalReturn = ((lastBar.close!! - firstBar.close!!) / firstBar.close!!) * 100

    println("Total Return (5 years): ${totalReturn.format(2)}%")
}
```

---

## 5. Corporate Actions

Corporate Actions는 별도로 `08-data-models-reference.md`에 정의되어 있습니다.

### 5.1 Dividends

배당 정보는 `Dividend` 데이터 클래스로 표현됩니다 (상세는 08번 문서 참조).

```kotlin
data class Dividend(
    val date: LocalDate,                 // 배당 지급일
    val amount: Double                   // 배당금 (주당, USD)
)
```

### 5.2 Splits

주식 분할 정보는 `Split` 데이터 클래스로 표현됩니다 (상세는 08번 문서 참조).

```kotlin
data class Split(
    val date: LocalDate,                 // 분할 날짜
    val numerator: Long,                 // 분자
    val denominator: Long,               // 분모
    val splitRatio: String               // 분할 비율 문자열
) {
    val ratio: Double get() = numerator.toDouble() / denominator.toDouble()
}
```

---

## 6. Ticker Info

종목 정보 모델입니다.

```kotlin
/**
 * 종목 정보
 */
data class TickerInfo(
    val symbol: String,                  // 심볼
    val shortName: String?,              // 짧은 이름
    val longName: String?,               // 긴 이름
    val currency: String?,               // 통화 (USD, EUR 등)
    val exchange: String?,               // 거래소 (NYSE, NASDAQ 등)
    val quoteType: String?,              // Quote 타입 (EQUITY, ETF 등)
    val sector: String?,                 // 섹터 (Technology, Healthcare 등)
    val industry: String?,               // 산업 (Software, Pharmaceuticals 등)
    val marketCap: Long?,                // 시가총액 (USD)
    val sharesOutstanding: Long?,        // 발행 주식 수
    val trailingPE: Double?,             // Trailing P/E Ratio
    val forwardPE: Double?,              // Forward P/E Ratio
    val dividendYield: Double?,          // 배당 수익률 (%)
    val beta: Double?,                   // 베타 (시장 대비 변동성)
    val fiftyTwoWeekHigh: Double?,       // 52주 최고가
    val fiftyTwoWeekLow: Double?,        // 52주 최저가
    val website: String?,                // 웹사이트 URL
    val description: String?             // 회사 설명
)
```

---

## 7. 사용 예시

### 7.1 기본 사용

```kotlin
suspend fun main() {
    val ufc = UFCClient.create()
    val aapl = ufc.yahoo.ticker("AAPL")

    // 1년 일별 데이터
    val history = aapl.history(
        period = Period.ONE_YEAR,
        interval = Interval.ONE_DAY
    )
    history.forEach { bar ->
        println("${bar.date}: ${bar.open} -> ${bar.close} (Vol: ${bar.volume})")
    }

    // 최근 30일 5분 데이터 (Intraday)
    val intraday = aapl.history(
        period = Period.ONE_MONTH,
        interval = Interval.FIVE_MINUTES
    )
    println("Total ${intraday.size} data points")
}
```

### 7.2 날짜 범위 지정

```kotlin
suspend fun specificDateRange() {
    val ufc = UFCClient.create()
    val spy = ufc.yahoo.ticker("SPY")

    // 특정 날짜 범위
    val history = spy.history(
        start = LocalDate.of(2023, 1, 1),
        end = LocalDate.of(2023, 12, 31),
        interval = Interval.ONE_DAY
    )

    println("Total ${history.size} data points")

    // 가격 통계 계산
    val closePrices = history.mapNotNull { it.close }
    val avgPrice = closePrices.average()
    val maxPrice = closePrices.maxOrNull()
    val minPrice = closePrices.minOrNull()

    println("Average: $avgPrice, Max: $maxPrice, Min: $minPrice")
}
```

### 7.3 배당 및 분할 조회

```kotlin
suspend fun dividendsAndSplits() {
    val ufc = UFCClient.create()
    val aapl = ufc.yahoo.ticker("AAPL")

    // 배당 정보
    val dividends = aapl.dividends(
        start = LocalDate.of(2020, 1, 1)
    )
    dividends.forEach { div ->
        println("${div.date}: $${div.amount}")
    }

    // 분할 정보
    val splits = aapl.splits()
    splits.forEach { split ->
        println("${split.date}: ${split.splitRatio} (${split.ratio}x)")
    }
}
```

### 7.4 종목 정보 조회

```kotlin
suspend fun tickerInfo() {
    val ufc = UFCClient.create()
    val aapl = ufc.yahoo.ticker("AAPL")

    val info = aapl.info()
    println("""
        Company: ${info.longName}
        Sector: ${info.sector}
        Industry: ${info.industry}
        Market Cap: $${info.marketCap?.let { it / 1_000_000_000 }}B
        P/E Ratio: ${info.trailingPE}
        Dividend Yield: ${info.dividendYield}%
        52-Week High: $${info.fiftyTwoWeekHigh}
        52-Week Low: $${info.fiftyTwoWeekLow}
    """.trimIndent())
}
```

### 7.5 장전/장후 거래 포함

```kotlin
suspend fun includePrePost() {
    val ufc = UFCClient.create()
    val tsla = ufc.yahoo.ticker("TSLA")

    // ChartParams를 직접 구성하여 장전/장후 거래 포함
    val params = ChartParams(
        range = Range.ONE_DAY,
        interval = Interval.FIVE_MINUTES,
        includePrePost = true,           // 장전/장후 거래 포함
        includeAdjustedClose = true
    )

    val client = YahooFinanceClient(httpClient, authenticator)
    val response = client.fetchChart("TSLA", params)

    // 처리...
}
```

---

**다음 문서**: [06-fred-macro-indicators.md](./06-fred-macro-indicators.md)
**관련 문서**: [08-data-models-reference.md](./08-data-models-reference.md)
