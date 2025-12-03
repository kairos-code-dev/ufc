# UFC API 사용 가이드

> **Version**: 2.0
> **Last Updated**: 2025-12-03
> **Architecture**: Clean Architecture with Namespace-based API

## 목차

- [소개](#소개)
- [시작하기](#시작하기)
- [도메인별 사용 예제](#도메인별-사용-예제)
  - [Price 도메인 - 가격 정보](#price-도메인---가격-정보)
  - [Stock 도메인 - 주식 기본 정보](#stock-도메인---주식-기본-정보)
  - [Funds 도메인 - 펀드 정보](#funds-도메인---펀드-정보)
  - [Corp 도메인 - 기업 행동](#corp-도메인---기업-행동)
  - [Macro 도메인 - 거시경제 지표](#macro-도메인---거시경제-지표)
- [고급 사용법](#고급-사용법)
- [모범 사례](#모범-사례)

---

## 소개

**UFC (Unified Finance Client)** 는 Yahoo Finance와 FRED API를 통합하여 금융 데이터를 제공하는 Kotlin 라이브러리입니다.

### 클린 아키텍처 원칙

UFC는 다음의 클린 아키텍처 원칙을 따릅니다:

- **의존성 역전 (DIP)**: 도메인 계층이 인프라 계층에 직접 의존하지 않고, 인터페이스를 통해 통신합니다.
- **도메인 순수성**: 비즈니스 로직은 외부 라이브러리(Ktor, HTTP 등)에 직접 의존하지 않습니다.
- **테스트 격리**: 모든 외부 의존성은 Fake 객체로 교체 가능하여 단위 테스트가 용이합니다.
- **문맥의 지역성**: 관련 로직은 물리적으로 가까이 배치되어 인지 부하를 줄입니다.

### 주요 도메인

UFC는 5개의 명확히 분리된 도메인 네임스페이스를 제공합니다:

| 네임스페이스 | 담당 기능 | 데이터 소스 |
|------------|----------|----------|
| **`ufc.price`** | 현재가, 가격 히스토리 (OHLCV) | Yahoo Finance |
| **`ufc.stock`** | 회사 정보, ISIN, 발행주식수 | Yahoo Finance |
| **`ufc.funds`** | ETF/뮤추얼펀드 구성 정보 | Yahoo Finance |
| **`ufc.corp`** | 배당금, 주식분할, 자본이득 | Yahoo Finance |
| **`ufc.macro`** | GDP, 실업률, 인플레이션 등 | FRED API |

---

## 시작하기

### 1. UFC 클라이언트 생성

```kotlin
import com.ulalax.ufc.client.UFC
import com.ulalax.ufc.client.UFCClientConfig

suspend fun main() {
    // 기본 설정으로 생성
    val ufc = UFC.create(
        UFCClientConfig(
            fredApiKey = "your-fred-api-key"  // Optional: Macro API 사용 시 필수
        )
    )

    try {
        // API 사용
        val price = ufc.price.getCurrentPrice("AAPL")
        println("AAPL: ${price.lastPrice} ${price.currency}")
    } finally {
        ufc.close()  // 리소스 정리
    }
}
```

### 2. Try-with-resources 패턴 (권장)

```kotlin
suspend fun main() {
    UFC.create(UFCClientConfig()).use { ufc ->
        val price = ufc.price.getCurrentPrice("AAPL")
        println("AAPL: ${price.lastPrice}")
    }  // 자동으로 close() 호출
}
```

### 3. Rate Limiting 설정

UFC는 기본적으로 Yahoo Finance API에 대해 Rate Limiting을 적용합니다:

```kotlin
import com.ulalax.ufc.infrastructure.ratelimiter.RateLimitingSettings

val ufc = UFC.create(
    UFCClientConfig(
        rateLimitingSettings = RateLimitingSettings(
            yahooRequestsPerSecond = 2.0,  // 초당 최대 2회 요청 (기본값)
            yahooMaxBurst = 5               // 최대 버스트 5회 (기본값)
        )
    )
)
```

**기본값**:
- Yahoo Finance: 초당 2회, 최대 버스트 5회
- FRED API: 제한 없음 (FRED 자체 Rate Limit 적용)

### 4. 캐싱 동작

UFC는 자동으로 응답을 캐싱하여 성능을 향상시킵니다:

| API 타입 | TTL (Time To Live) | 캐시 키 예시 |
|---------|-------------------|-----------|
| 현재가 | 60초 | `price:current:AAPL` |
| 가격 히스토리 | 5분 | `price:history:AAPL:OneYear:OneDay` |
| 회사 정보 | 1시간 | `stock:info:AAPL` |
| 펀드 정보 | 1시간 | `funds:data:SPY` |
| 배당 히스토리 | 1시간 | `corp:dividends:AAPL:FiveYears` |
| 매크로 지표 | 1시간 | `macro:GDP` |

**캐시 무효화**: 현재는 자동 캐시 무효화만 지원합니다 (TTL 만료 시).

---

## 도메인별 사용 예제

### Price 도메인 - 가격 정보

`ufc.price` 네임스페이스는 현재 가격 및 가격 히스토리를 제공합니다.

#### 현재가 조회

```kotlin
import com.ulalax.ufc.domain.price.PriceData

// 단일 심볼 조회
val price: PriceData = ufc.price.getCurrentPrice("AAPL")

println("Symbol: ${price.symbol}")
println("Last Price: ${price.lastPrice} ${price.currency}")
println("Market Cap: ${price.marketCap}")
println("Day High: ${price.dayHigh}")
println("Day Low: ${price.dayLow}")
println("Volume: ${price.volume}")
println("52-Week High: ${price.fiftyTwoWeekHigh}")
println("52-Week Low: ${price.fiftyTwoWeekLow}")
println("Dividend Yield: ${price.dividendYield}")
```

**응답 데이터 구조**:
```kotlin
data class PriceData(
    val symbol: String,
    val currency: String?,
    val exchange: String?,
    val lastPrice: Double?,              // 현재가
    val regularMarketPrice: Double?,
    val open: Double?,                   // 시가
    val dayHigh: Double?,                // 당일 고가
    val dayLow: Double?,                 // 당일 저가
    val previousClose: Double?,          // 전일 종가
    val volume: Long?,                   // 거래량
    val marketCap: Long?,                // 시가총액
    val fiftyTwoWeekHigh: Double?,       // 52주 고가
    val fiftyTwoWeekLow: Double?,        // 52주 저가
    val dividendYield: Double?,          // 배당 수익률
    val beta: Double?,                   // 베타 (시장 대비 변동성)
    val trailingPE: Double?,             // PER (주가수익비율)
    // ... 기타 필드
)
```

**에러 처리**:
```kotlin
import com.ulalax.ufc.exception.UfcException
import com.ulalax.ufc.exception.ErrorCode

try {
    val price = ufc.price.getCurrentPrice("INVALID_SYMBOL")
} catch (e: UfcException) {
    when (e.errorCode) {
        ErrorCode.PRICE_DATA_NOT_FOUND -> {
            println("심볼을 찾을 수 없습니다: ${e.message}")
        }
        ErrorCode.RATE_LIMITED -> {
            println("요청 제한 초과. 잠시 후 다시 시도하세요.")
        }
        else -> {
            println("알 수 없는 오류: ${e.message}")
        }
    }
}
```

**캐싱**: 60초 TTL

---

#### 가격 히스토리 조회

```kotlin
import com.ulalax.ufc.model.common.Period
import com.ulalax.ufc.model.common.Interval
import com.ulalax.ufc.domain.price.OHLCV

// Period와 Interval로 조회
val ohlcv: List<OHLCV> = ufc.price.getPriceHistory(
    symbol = "AAPL",
    period = Period.OneMonth,
    interval = Interval.OneDay
)

ohlcv.forEach { bar ->
    println("${bar.timestamp}: Open=${bar.open}, High=${bar.high}, Low=${bar.low}, Close=${bar.close}, Volume=${bar.volume}")
}
```

**Period 옵션**:
- `Period.OneDay`, `Period.FiveDays`, `Period.OneMonth`
- `Period.ThreeMonths`, `Period.SixMonths`, `Period.OneYear`
- `Period.TwoYears`, `Period.FiveYears`, `Period.TenYears`
- `Period.YearToDate`, `Period.Max`

**Interval 옵션**:
- Intraday: `Interval.OneMinute`, `TwoMinutes`, `FiveMinutes`, `FifteenMinutes`, `ThirtyMinutes`, `OneHour`
- Daily+: `Interval.OneDay`, `FiveDays`, `OneWeek`, `OneMonth`, `ThreeMonths`

**제약사항**:
- Intraday 간격 (< 1시간): 최대 7일
- 시간 간격 (1시간): 최대 60일
- 일 간격 이상: 제한 없음

**날짜 범위로 조회**:
```kotlin
import java.time.LocalDate

val ohlcv = ufc.price.getPriceHistory(
    symbol = "AAPL",
    start = LocalDate.of(2024, 1, 1),
    end = LocalDate.of(2024, 12, 31),
    interval = Interval.OneDay
)
```

**메타데이터 포함 조회**:
```kotlin
import com.ulalax.ufc.domain.price.ChartMetadata

val (metadata, ohlcv) = ufc.price.getPriceHistoryWithMetadata(
    symbol = "AAPL",
    period = Period.OneYear,
    interval = Interval.OneDay
)

println("Symbol: ${metadata.symbol}")
println("Currency: ${metadata.currency}")
println("Exchange: ${metadata.exchangeName}")
println("Data Granularity: ${metadata.dataGranularity}")
println("Valid Ranges: ${metadata.validRanges}")
```

**캐싱**: 5분 TTL

---

#### 배치 조회 (다중 심볼)

```kotlin
val symbols = listOf("AAPL", "GOOGL", "MSFT")

// 병렬로 조회 (내부적으로 coroutines 사용)
val prices: Map<String, PriceData> = ufc.price.getCurrentPrice(symbols)

prices.forEach { (symbol, price) ->
    println("$symbol: ${price.lastPrice} ${price.currency}")
}
```

**제약사항**: 최대 100개 심볼까지 한 번에 조회 가능

---

### Stock 도메인 - 주식 기본 정보

`ufc.stock` 네임스페이스는 회사 정보, ISIN 코드, 발행주식수 등을 제공합니다.

#### 회사 정보 조회

```kotlin
import com.ulalax.ufc.domain.stock.CompanyInfo

val companyInfo: CompanyInfo = ufc.stock.getCompanyInfo("AAPL")

println("Long Name: ${companyInfo.longName}")
println("Sector: ${companyInfo.sector}")
println("Industry: ${companyInfo.industry}")
println("Country: ${companyInfo.country}")
println("Website: ${companyInfo.website}")
println("Description: ${companyInfo.longBusinessSummary}")
println("Full-Time Employees: ${companyInfo.fullTimeEmployees}")
```

**응답 데이터 구조**:
```kotlin
data class CompanyInfo(
    val symbol: String,
    val longName: String?,          // 회사명 (전체)
    val shortName: String?,         // 회사명 (약칭)
    val sector: String?,            // 섹터 (예: Technology)
    val industry: String?,          // 산업 (예: Consumer Electronics)
    val country: String?,           // 국가
    val city: String?,              // 도시
    val address1: String?,          // 주소
    val website: String?,           // 웹사이트
    val longBusinessSummary: String?,  // 사업 설명
    val fullTimeEmployees: Int?     // 정규직 직원 수
)
```

**캐싱**: 1시간 TTL

---

#### ISIN 코드 조회

```kotlin
// 단일 심볼
val isin: String = ufc.stock.getIsin("AAPL")
println("ISIN: $isin")  // 예: US0378331005

// 다중 심볼
val isins: Map<String, String> = ufc.stock.getIsin(listOf("AAPL", "GOOGL", "MSFT"))
isins.forEach { (symbol, isin) ->
    println("$symbol: $isin")
}
```

**캐싱**: 1시간 TTL

---

#### FastInfo 조회

```kotlin
import com.ulalax.ufc.domain.stock.FastInfo

val fastInfo: FastInfo = ufc.stock.getFastInfo("AAPL")

println("Symbol: ${fastInfo.symbol}")
println("Exchange: ${fastInfo.exchange}")
println("Quote Type: ${fastInfo.quoteType}")  // EQUITY, ETF, MUTUALFUND 등
println("Currency: ${fastInfo.currency}")
println("Market State: ${fastInfo.marketState}")  // REGULAR, PRE, POST, CLOSED
```

**캐싱**: 60초 TTL

---

#### 발행주식수 조회

```kotlin
val shares: Long = ufc.stock.getShares("AAPL")
println("Shares Outstanding: ${shares}")
```

**캐싱**: 1시간 TTL

---

### Funds 도메인 - 펀드 정보

`ufc.funds` 네임스페이스는 ETF 및 뮤추얼펀드의 구성 정보를 제공합니다.

#### 펀드 데이터 조회

```kotlin
import com.ulalax.ufc.domain.funds.FundData

val fundData: FundData = ufc.funds.getFundData("SPY")

println("Symbol: ${fundData.symbol}")
println("Quote Type: ${fundData.quoteType}")  // ETF, MUTUALFUND
println("Description: ${fundData.description}")
println("Category: ${fundData.categoryName}")
println("Fund Family: ${fundData.fundFamily}")
```

**Top Holdings (보유 종목)**:
```kotlin
fundData.topHoldings.forEach { holding ->
    println("${holding.symbol}: ${holding.holdingPercent}% (${holding.holdingName})")
}
```

**자산 배분 (Asset Classes)**:
```kotlin
fundData.assetClasses?.let { assets ->
    println("Cash: ${assets.cash}%")
    println("Stocks: ${assets.stocks}%")
    println("Bonds: ${assets.bonds}%")
    println("Other: ${assets.other}%")
}
```

**섹터별 배분**:
```kotlin
fundData.sectorWeightings?.forEach { sector ->
    println("${sector.key}: ${sector.value}%")
}
```

**캐싱**: 1시간 TTL

---

#### 펀드 여부 확인

```kotlin
val isFund: Boolean = ufc.funds.isFund("SPY")
println("Is Fund: $isFund")  // true

val isNotFund: Boolean = ufc.funds.isFund("AAPL")
println("Is Fund: $isNotFund")  // false
```

**캐싱**: 1시간 TTL

---

### Corp 도메인 - 기업 행동

`ufc.corp` 네임스페이스는 배당금, 주식분할, 자본이득 히스토리를 제공합니다.

#### 배당금 조회

```kotlin
import com.ulalax.ufc.domain.corp.DividendData

val dividendData: DividendData = ufc.corp.getDividends("AAPL", Period.FiveYears)

println("Symbol: ${dividendData.symbol}")
println("Total Dividends: ${dividendData.dividends.size}")

dividendData.dividends.forEach { dividend ->
    println("${dividend.date}: ${dividend.amount} USD")
}
```

**응답 데이터 구조**:
```kotlin
data class Dividend(
    val date: Long,      // Unix timestamp (seconds)
    val amount: Double   // 배당금 (USD)
)

data class DividendData(
    val symbol: String,
    val dividends: List<Dividend>
)
```

**캐싱**: 1시간 TTL

---

#### 주식분할 조회

```kotlin
import com.ulalax.ufc.domain.corp.SplitData

val splitData: SplitData = ufc.corp.getSplits("AAPL", Period.Max)

if (splitData.splits.isNotEmpty()) {
    splitData.splits.forEach { split ->
        println("${split.date}: ${split.numerator}:${split.denominator}")
        // 예: 2020-08-31: 4:1 (1주가 4주로 분할)
    }
} else {
    println("No stock splits found")
}
```

**응답 데이터 구조**:
```kotlin
data class Split(
    val date: Long,         // Unix timestamp
    val numerator: Long,    // 분자 (예: 4)
    val denominator: Long   // 분모 (예: 1)
)

data class SplitData(
    val symbol: String,
    val splits: List<Split>
)
```

**캐싱**: 1시간 TTL

---

#### 자본이득 조회 (ETF/뮤추얼펀드)

```kotlin
import com.ulalax.ufc.domain.corp.CapitalGainData

val capitalGains: CapitalGainData = ufc.corp.getCapitalGains("SPY", Period.FiveYears)

capitalGains.capitalGains.forEach { gain ->
    println("${gain.date}: ${gain.amount} USD")
}
```

**참고**: 자본이득은 주로 ETF/뮤추얼펀드에만 해당됩니다.

**캐싱**: 1시간 TTL

---

### Macro 도메인 - 거시경제 지표

`ufc.macro` 네임스페이스는 FRED API를 통해 거시경제 지표를 제공합니다.

**중요**: FRED API 키가 필요하며, 키가 없는 경우 `ufc.macro`는 `null`입니다.

#### Null 체크

```kotlin
val macro = ufc.macro ?: throw IllegalStateException("FRED API key required")

// 또는
if (ufc.macro == null) {
    println("Macro API is not available")
    return
}
```

---

#### GDP 조회

```kotlin
import com.ulalax.ufc.domain.macro.MacroSeries

val gdp: MacroSeries = macro.getGDP()

println("Series ID: ${gdp.seriesId}")
println("Title: ${gdp.title}")
println("Units: ${gdp.units}")

gdp.observations.takeLast(5).forEach { obs ->
    println("${obs.date}: ${obs.value}")
}
```

**응답 데이터 구조**:
```kotlin
data class MacroObservation(
    val date: String,      // YYYY-MM-DD
    val value: String      // 값 (문자열, "." = 데이터 없음)
)

data class MacroSeries(
    val seriesId: String,
    val title: String?,
    val units: String?,
    val observations: List<MacroObservation>
)
```

**캐싱**: 1시간 TTL

---

#### 실업률 조회

```kotlin
val unemployment: MacroSeries = macro.getUnemploymentRate()

println("Unemployment Rate (latest):")
unemployment.observations.lastOrNull()?.let { obs ->
    println("${obs.date}: ${obs.value}%")
}
```

**캐싱**: 1시간 TTL

---

#### 인플레이션 (CPI) 조회

```kotlin
val cpi: MacroSeries = macro.getCPI()

println("CPI (Consumer Price Index):")
cpi.observations.takeLast(12).forEach { obs ->
    println("${obs.date}: ${obs.value}")
}
```

**캐싱**: 1시간 TTL

---

#### 커스텀 FRED Series 조회

```kotlin
import java.time.LocalDate

val customSeries: MacroSeries = macro.getSeries(
    seriesId = "DFF",  // Federal Funds Rate
    observationStart = LocalDate.of(2020, 1, 1),
    observationEnd = LocalDate.of(2024, 12, 31)
)

println("Federal Funds Rate:")
customSeries.observations.forEach { obs ->
    println("${obs.date}: ${obs.value}%")
}
```

**FRED Series ID 찾기**: https://fred.stlouisfed.org/

**캐싱**: 1시간 TTL

---

## 고급 사용법

### 1. 병렬 요청 처리

```kotlin
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

suspend fun fetchMultipleData(ufc: UFC) = coroutineScope {
    // 여러 API를 병렬로 호출
    val priceDeferred = async { ufc.price.getCurrentPrice("AAPL") }
    val companyDeferred = async { ufc.stock.getCompanyInfo("AAPL") }
    val dividendsDeferred = async { ufc.corp.getDividends("AAPL", Period.OneYear) }

    // 모든 결과를 동시에 기다림
    val price = priceDeferred.await()
    val company = companyDeferred.await()
    val dividends = dividendsDeferred.await()

    println("Price: ${price.lastPrice}, Company: ${company.longName}, Dividends: ${dividends.dividends.size}")
}
```

---

### 2. 에러 처리 전략

```kotlin
import com.ulalax.ufc.exception.UfcException
import com.ulalax.ufc.exception.ErrorCode

suspend fun fetchWithRetry(ufc: UFC, symbol: String, maxRetries: Int = 3): PriceData? {
    var attempt = 0

    while (attempt < maxRetries) {
        try {
            return ufc.price.getCurrentPrice(symbol)
        } catch (e: UfcException) {
            when (e.errorCode) {
                ErrorCode.RATE_LIMITED -> {
                    // Rate limit 시 대기 후 재시도
                    attempt++
                    println("Rate limited. Waiting 5 seconds... (Attempt $attempt/$maxRetries)")
                    kotlinx.coroutines.delay(5000)
                }
                ErrorCode.NETWORK_ERROR -> {
                    // 네트워크 오류 시 재시도
                    attempt++
                    println("Network error. Retrying... (Attempt $attempt/$maxRetries)")
                    kotlinx.coroutines.delay(1000)
                }
                else -> {
                    // 다른 오류는 재시도 안 함
                    println("Error: ${e.message}")
                    return null
                }
            }
        }
    }

    println("Max retries exceeded")
    return null
}
```

**주요 ErrorCode**:
- `INVALID_SYMBOL`: 유효하지 않은 심볼
- `PRICE_DATA_NOT_FOUND`: 가격 데이터를 찾을 수 없음
- `INCOMPLETE_PRICE_DATA`: 불완전한 가격 데이터
- `RATE_LIMITED`: 요청 제한 초과
- `NETWORK_ERROR`: 네트워크 오류
- `INVALID_DATE_RANGE`: 유효하지 않은 날짜 범위
- `INVALID_PERIOD_INTERVAL`: 유효하지 않은 Period/Interval 조합

---

### 3. Raw 응답 조회 (고급)

UFC는 파싱되지 않은 Raw 응답도 제공합니다:

```kotlin
import com.ulalax.ufc.internal.yahoo.response.PriceResponse
import com.ulalax.ufc.internal.yahoo.response.ChartResponse

// Raw Price 응답
val rawPrice: PriceResponse = ufc.price.getRawPrice("AAPL")

// Raw Chart 응답
val rawChart: ChartResponse = ufc.price.getRawPriceHistory(
    symbol = "AAPL",
    period = Period.OneMonth,
    interval = Interval.OneDay
)

// Raw 응답을 직접 파싱할 수 있습니다
println(rawPrice.quoteSummary.result?.first()?.price?.regularMarketPrice?.doubleValue)
```

**사용 사례**:
- 커스텀 파싱 로직
- 디버깅
- UFC가 아직 지원하지 않는 필드 접근

---

## 모범 사례

### 1. API 호출 최적화

**DO**:
```kotlin
// ✅ 배치 조회 사용 (병렬 처리)
val symbols = listOf("AAPL", "GOOGL", "MSFT")
val prices = ufc.price.getCurrentPrice(symbols)
```

**DON'T**:
```kotlin
// ❌ 순차적으로 여러 번 호출 (느림)
val symbols = listOf("AAPL", "GOOGL", "MSFT")
val prices = symbols.map { symbol ->
    ufc.price.getCurrentPrice(symbol)  // 각각 별도 API 호출
}
```

---

### 2. 리소스 관리

**DO**:
```kotlin
// ✅ Try-with-resources 사용
UFC.create(config).use { ufc ->
    val data = ufc.price.getCurrentPrice("AAPL")
}  // 자동 close()
```

**DON'T**:
```kotlin
// ❌ close() 누락 (리소스 누수)
val ufc = UFC.create(config)
val data = ufc.price.getCurrentPrice("AAPL")
// close() 호출 안 함
```

---

### 3. 에러 처리 패턴

**DO**:
```kotlin
// ✅ 구체적인 ErrorCode로 처리
try {
    val price = ufc.price.getCurrentPrice(symbol)
} catch (e: UfcException) {
    when (e.errorCode) {
        ErrorCode.PRICE_DATA_NOT_FOUND -> handleNotFound()
        ErrorCode.RATE_LIMITED -> handleRateLimit()
        else -> handleGenericError(e)
    }
}
```

**DON'T**:
```kotlin
// ❌ 모든 예외를 동일하게 처리
try {
    val price = ufc.price.getCurrentPrice(symbol)
} catch (e: Exception) {
    println("Error: ${e.message}")
}
```

---

### 4. 캐시 활용

```kotlin
// ✅ 같은 데이터를 여러 번 조회해도 캐시에서 반환 (빠름)
val price1 = ufc.price.getCurrentPrice("AAPL")  // API 호출
val price2 = ufc.price.getCurrentPrice("AAPL")  // 캐시에서 반환 (60초 이내)

// ✅ TTL 이후에는 자동으로 갱신
kotlinx.coroutines.delay(61_000)  // 61초 대기
val price3 = ufc.price.getCurrentPrice("AAPL")  // 다시 API 호출
```

---

### 5. Null 안전성

```kotlin
// ✅ Nullable 필드 안전하게 처리
val price = ufc.price.getCurrentPrice("AAPL")

// Safe call 사용
println("Market Cap: ${price.marketCap ?: "N/A"}")

// Elvis operator 사용
val marketCap = price.marketCap ?: 0L

// let 사용
price.dividendYield?.let { yield ->
    println("Dividend Yield: $yield%")
}
```

---

### 6. 테스트 작성 가이드

UFC는 테스트가 용이하도록 설계되었습니다:

```kotlin
// src/test/kotlin/MyServiceTest.kt

import com.ulalax.ufc.fakes.FakePriceService

class MyServiceTest {
    @Test
    fun `가격 조회 성공`() = runTest {
        // Given: Fake 서비스 사용
        val fakePriceService = FakePriceService()
        fakePriceService.setPrice("AAPL", 150.0)

        val myService = MyService(fakePriceService)

        // When
        val result = myService.fetchPrice("AAPL")

        // Then
        assertThat(result).isEqualTo(150.0)
    }
}
```

**Fake 구현체 위치**: `src/test/kotlin/com/ulalax/ufc/fakes/`

자세한 테스트 가이드는 [doc/test-principle.md](./test-principle.md)를 참고하세요.

---

## 관련 문서

- [마이그레이션 가이드](./MIGRATION_GUIDE_V2.md) - V1에서 V2로 마이그레이션
- [아키텍처 문서](./ARCHITECTURE.md) - 클린 아키텍처 상세 설명
- [ADR-001: 클린 아키텍처 리팩토링](./adr/ADR-001-clean-architecture-refactoring.md) - 아키텍처 결정 기록
- [테스트 원칙](./test-principle.md) - 테스트 작성 가이드
- [네임스페이스 표준](./금융데이터-네임스페이스-표준.md) - 도메인 분류 기준

---

## 문의 및 피드백

문제가 발생하거나 개선 제안이 있으시면 GitHub Issues를 통해 알려주세요.

**GitHub**: https://github.com/ulalax/ufc

---

**최종 수정일**: 2025-12-03
**버전**: 2.0
