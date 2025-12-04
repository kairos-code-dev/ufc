# Ufc (Unified Finance Client)

**무료 미국 금융 데이터 수집 Kotlin 라이브러리**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

Ufc는 Yahoo Finance와 FRED(Federal Reserve Economic Data)로부터 금융 데이터를 수집하는 Kotlin 라이브러리입니다. Python의 [yfinance](https://github.com/ranaroussi/yfinance)와 [fredapi](https://github.com/mortada/fredapi)에서 영감을 받아, Kotlin/JVM 환경에서 동일한 기능을 제공합니다.

## 주요 특징

- **네임스페이스 기반 API**: `ufc.price`, `ufc.stock`, `ufc.funds`, `ufc.corp`, `ufc.macro`
- **Multi-Source 통합**: Yahoo Finance + FRED API
- **자동 Rate Limiting**: API별 최적화된 요청 제한
- **자동 캐싱**: API 호출 60% 감소
- **Coroutines 지원**: 비동기 고성능 처리
- **타입 안정성**: Kotlin 강타입 시스템

## 기술 스택

- Kotlin 2.1.0
- JDK 21
- Ktor 3.0.1 (HTTP Client)
- kotlinx.serialization 1.7.3
- kotlinx.coroutines 1.9.0

---

## 빠른 시작

### 설치

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.ulalax:ufc:1.0.0")
}
```

### 기본 사용법

```kotlin
import com.ulalax.ufc.api.client.Ufc
import com.ulalax.ufc.api.client.UfcClientConfig

suspend fun main() {
    Ufc.create(UfcClientConfig()).use { ufc ->

        // 현재 주가 조회
        val price = ufc.price.getCurrentPrice("AAPL")
        println("AAPL: $${price.lastPrice}")

        // 회사 정보 조회
        val info = ufc.stock.getCompanyInfo("AAPL")
        println("${info.longName} - ${info.sector}")

        // ETF 정보 조회
        val spy = ufc.funds.getFundData("SPY")
        println("SPY Top Holdings: ${spy.topHoldings.take(3).map { it.symbol }}")

    }
}
```

### FRED API 사용 (거시경제 지표)

FRED API를 사용하려면 API Key가 필요합니다.

1. https://fred.stlouisfed.org/docs/api/api_key.html 에서 무료 API Key 발급
2. 설정에 추가:

```kotlin
val ufc = Ufc.create(
    UfcClientConfig(fredApiKey = "your_fred_api_key")
)

// GDP 조회
val gdp = ufc.macro?.getGDP(GDPType.REAL)
println("Real GDP: ${gdp?.observations?.last()?.value}")

// 실업률 조회
val unemployment = ufc.macro?.getUnemployment(UnemploymentType.RATE)
println("Unemployment: ${unemployment?.observations?.last()?.value}%")
```

---

## 데이터 소스별 기능

### Yahoo Finance (API Key 불필요)

| 네임스페이스 | 기능 | 메서드 |
|-------------|------|--------|
| `ufc.price` | 현재가 조회 | `getCurrentPrice(symbol)` |
| | 다중 심볼 현재가 | `getCurrentPrice(symbols)` |
| | 가격 히스토리 (기간) | `getPriceHistory(symbol, period, interval)` |
| | 가격 히스토리 (날짜) | `getPriceHistory(symbol, start, end, interval)` |
| | 메타데이터 포함 히스토리 | `getPriceHistoryWithMetadata(symbol, period)` |
| `ufc.stock` | 회사 정보 | `getCompanyInfo(symbol)` |
| | 빠른 정보 | `getFastInfo(symbol)` |
| | ISIN 코드 | `getIsin(symbol)` |
| | 발행주식수 | `getShares(symbol)` |
| | 발행주식수 (전체 기간) | `getSharesFull(symbol, start, end)` |
| `ufc.funds` | ETF/뮤추얼펀드 정보 | `getFundData(symbol)` |
| | 다중 펀드 조회 | `getFundData(symbols)` |
| | 펀드 여부 확인 | `isFund(symbol)` |
| `ufc.corp` | 배당금 히스토리 | `getDividends(symbol, period)` |
| | 주식 분할 히스토리 | `getSplits(symbol, period)` |
| | 자본이득 히스토리 | `getCapitalGains(symbol, period)` |

### FRED API (API Key 필요)

| 네임스페이스 | 기능 | 메서드 |
|-------------|------|--------|
| `ufc.macro` | 범용 시계열 조회 | `getSeries(seriesId)` |
| | GDP 지표 | `getGDP(type)` |
| | 인플레이션 지표 | `getInflation(type)` |
| | 실업률/고용 지표 | `getUnemployment(type)` |
| | 금리 지표 | `getInterestRate(type)` |

**GDP 타입**: `REAL`, `NOMINAL`, `GROWTH`, `POTENTIAL`

**인플레이션 타입**: `CPI_ALL`, `CPI_CORE`, `PCE_ALL`, `PCE_CORE`, `PPI`

**실업률 타입**: `RATE`, `INITIAL_CLAIMS`, `CONTINUING_CLAIMS`, `NONFARM_PAYROLLS`

**금리 타입**: `FEDERAL_FUNDS`, `TREASURY_10Y`, `TREASURY_2Y`, `TREASURY_5Y`, `TREASURY_30Y`, `TREASURY_3M`, `MORTGAGE_30Y`

---

## API 상세 예제

### 가격 데이터

```kotlin
// 현재가
val price = ufc.price.getCurrentPrice("AAPL")
println("Last: ${price.lastPrice}, Change: ${price.regularMarketChange}")

// 다중 심볼
val prices = ufc.price.getCurrentPrice(listOf("AAPL", "GOOGL", "MSFT"))
prices.forEach { (symbol, data) ->
    println("$symbol: ${data.lastPrice}")
}

// 1년 일봉 히스토리
val history = ufc.price.getPriceHistory("AAPL", Period.OneYear, Interval.OneDay)
history.forEach { ohlcv ->
    println("${ohlcv.timestamp}: O=${ohlcv.open} H=${ohlcv.high} L=${ohlcv.low} C=${ohlcv.close} V=${ohlcv.volume}")
}

// 날짜 범위 지정
val rangeHistory = ufc.price.getPriceHistory(
    symbol = "AAPL",
    start = LocalDate.of(2024, 1, 1),
    end = LocalDate.of(2024, 12, 31),
    interval = Interval.OneWeek
)
```

### 주식 정보

```kotlin
// 회사 상세 정보
val info = ufc.stock.getCompanyInfo("AAPL")
println("""
    회사명: ${info.longName}
    섹터: ${info.sector}
    산업: ${info.industry}
    국가: ${info.country}
    직원수: ${info.fullTimeEmployees}
    웹사이트: ${info.website}
""".trimIndent())

// ISIN 코드
val isin = ufc.stock.getIsin("AAPL")  // US0378331005

// 빠른 정보 (최소 API 호출)
val fast = ufc.stock.getFastInfo("AAPL")
println("${fast.symbol} (${fast.exchange}) - ${fast.quoteType}")

// 발행주식수 히스토리
val shares = ufc.stock.getShares("AAPL")
shares.forEach { println("${it.date}: ${it.shares}") }
```

### ETF/펀드 정보

```kotlin
val spy = ufc.funds.getFundData("SPY")
println("""
    펀드: ${spy.symbol}
    타입: ${spy.quoteType}
    카테고리: ${spy.category}
    총 자산: ${spy.totalAssets}

    Top 10 보유종목:
""".trimIndent())

spy.topHoldings.take(10).forEach { holding ->
    println("  ${holding.symbol}: ${holding.holdingPercent}%")
}

// 섹터별 비중
spy.sectorWeights.forEach { (sector, weight) ->
    println("  $sector: $weight%")
}
```

### 배당금/분할

```kotlin
// 최근 5년 배당금
val dividends = ufc.corp.getDividends("AAPL", Period.FiveYears)
dividends.dividends.forEach { div ->
    println("${div.date}: $${div.amount}")
}

// 전체 주식분할 히스토리
val splits = ufc.corp.getSplits("AAPL", Period.Max)
splits.splits.forEach { split ->
    println("${split.date}: ${split.numerator}:${split.denominator}")
}
```

### 거시경제 지표 (FRED)

```kotlin
// FRED API Key 필요
val ufc = Ufc.create(UfcClientConfig(fredApiKey = "your_key"))

// 실질 GDP
val gdp = ufc.macro?.getGDP(GDPType.REAL, startDate = "2020-01-01")
gdp?.observations?.forEach { obs ->
    println("${obs.date}: ${obs.value}")
}

// 연방기금금리
val fedRate = ufc.macro?.getInterestRate(InterestRateType.FEDERAL_FUNDS)
println("현재 기준금리: ${fedRate?.observations?.last()?.value}%")

// CPI (소비자물가지수)
val cpi = ufc.macro?.getInflation(InflationType.CPI_ALL)

// 범용 시리즈 조회 (FRED 시리즈 ID 직접 사용)
val customSeries = ufc.macro?.getSeries(
    seriesId = "T10Y2Y",  // 10년-2년 금리 스프레드
    startDate = "2023-01-01",
    frequency = "m"  // 월간
)
```

---

## Rate Limiting

Ufc는 각 데이터 소스별로 자동 Rate Limiting을 적용합니다.

| 데이터 소스 | Rate Limit | 설명 |
|------------|------------|------|
| Yahoo Finance | 50 req/sec | 비공식 API, 보수적 설정 권장 |
| FRED | 2 req/sec | 공식 제한: 120 req/min |

### Rate Limit 커스터마이징

```kotlin
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitConfig
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitingSettings

val config = UfcClientConfig(
    rateLimitingSettings = RateLimitingSettings(
        yahoo = RateLimitConfig(
            capacity = 30,      // 버스트 허용량
            refillRate = 30,    // 초당 리필 토큰
            enabled = true
        ),
        fred = RateLimitConfig(
            capacity = 2,
            refillRate = 2,
            enabled = true
        )
    )
)
```

---

## 캐싱

Ufc는 자동 캐싱으로 중복 API 호출을 방지합니다.

| 데이터 타입 | TTL | 설명 |
|------------|-----|------|
| 현재가 | 60초 | 실시간 데이터 |
| 히스토리 | 5분 | 과거 데이터 |
| 회사 정보 | 1시간 | 정적 데이터 |
| 펀드 데이터 | 1시간 | 정적 데이터 |

---

## 에러 처리

Ufc는 `UfcException` 단일 예외 시스템을 사용합니다.

```kotlin
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.api.exception.ErrorCode

try {
    val price = ufc.price.getCurrentPrice("INVALID_SYMBOL")
} catch (e: UfcException) {
    when (e.errorCode) {
        ErrorCode.INVALID_SYMBOL -> println("잘못된 심볼: ${e.message}")
        ErrorCode.PRICE_DATA_NOT_FOUND -> println("가격 데이터 없음")
        ErrorCode.RATE_LIMITED -> println("요청 제한 초과, 잠시 후 재시도")
        ErrorCode.AUTH_FAILED -> println("인증 실패")
        ErrorCode.NETWORK_ERROR -> println("네트워크 오류: ${e.message}")
        else -> println("오류 발생: ${e.message}")
    }
}
```

### 주요 에러 코드

| ErrorCode | 설명 |
|-----------|------|
| `INVALID_SYMBOL` | 유효하지 않은 심볼 |
| `PRICE_DATA_NOT_FOUND` | 가격 데이터 없음 |
| `FUND_DATA_NOT_FOUND` | 펀드 데이터 없음 |
| `ISIN_NOT_FOUND` | ISIN 코드 없음 |
| `RATE_LIMITED` | Rate limit 초과 |
| `AUTH_FAILED` | 인증 실패 |
| `NETWORK_ERROR` | 네트워크 오류 |
| `PARSING_ERROR` | 응답 파싱 실패 |

---

## 지원 자산 유형

| 자산 유형 | 예시 | 지원 기능 |
|----------|------|----------|
| 주식 (Stock) | AAPL, GOOGL, MSFT | 모든 기능 |
| ETF | SPY, QQQ, VTI | 모든 기능 + 펀드 데이터 |
| 뮤추얼펀드 | VFIAX, FXAIX | 가격, 펀드 데이터 |
| 인덱스 | ^GSPC, ^DJI, ^IXIC | 가격만 |
| 암호화폐 | BTC-USD, ETH-USD | 가격만 |
| 환율 | EURUSD=X, JPYKRW=X | 가격만 |

---

## Period & Interval

### Period (조회 기간)

```kotlin
enum class Period {
    OneDay,      // 1일
    FiveDays,    // 5일
    OneMonth,    // 1개월
    ThreeMonths, // 3개월
    SixMonths,   // 6개월
    OneYear,     // 1년
    TwoYears,    // 2년
    FiveYears,   // 5년
    TenYears,    // 10년
    Ytd,         // 연초부터
    Max          // 전체
}
```

### Interval (데이터 간격)

```kotlin
enum class Interval {
    OneMinute,      // 1분 (최대 7일)
    TwoMinutes,     // 2분
    FiveMinutes,    // 5분
    FifteenMinutes, // 15분
    ThirtyMinutes,  // 30분
    SixtyMinutes,   // 60분
    NinetyMinutes,  // 90분
    OneHour,        // 1시간
    OneDay,         // 1일
    FiveDays,       // 5일
    OneWeek,        // 1주
    OneMonth,       // 1개월
    ThreeMonths     // 3개월
}
```

---

## 프로젝트 구조

```
ufc/
├── src/main/kotlin/com/ulalax/ufc/
│   ├── api/                    # 공개 API
│   │   ├── client/             # Ufc 클라이언트
│   │   ├── PriceApi.kt         # 가격 API
│   │   ├── StockApi.kt         # 주식 API
│   │   ├── FundsApi.kt         # 펀드 API
│   │   ├── CorpApi.kt          # 기업행동 API
│   │   ├── MacroApi.kt         # 거시경제 API
│   │   └── exception/          # 예외 클래스
│   │
│   ├── domain/                 # 도메인 로직
│   │   ├── price/              # 가격 도메인
│   │   ├── stock/              # 주식 도메인
│   │   ├── funds/              # 펀드 도메인
│   │   ├── corp/               # 기업행동 도메인
│   │   ├── macro/              # 거시경제 도메인
│   │   └── common/             # 공통 모델
│   │
│   └── infrastructure/         # 인프라스트럭처
│       ├── yahoo/              # Yahoo Finance 클라이언트
│       ├── fred/               # FRED 클라이언트
│       ├── ratelimit/          # Rate Limiter
│       └── util/               # 캐싱, 유틸리티
│
└── src/test/kotlin/            # 테스트
```

---

## 빌드 및 테스트

```bash
# 빌드
./gradlew build

# 단위 테스트
./gradlew test

# 통합 테스트 (실제 API 호출)
./gradlew integrationTest
```

---

## 제한사항

### Yahoo Finance

- **비공식 API**: Yahoo Finance는 공식 API를 제공하지 않습니다. 웹 스크래핑 기반으로 동작하며, Yahoo의 정책 변경에 따라 동작이 달라질 수 있습니다.
- **Rate Limiting**: 과도한 요청 시 일시적으로 차단될 수 있습니다.
- **상업적 사용**: Yahoo Finance 이용약관을 확인하세요.

### FRED

- **API Key 필요**: https://fred.stlouisfed.org 에서 무료 발급
- **Rate Limit**: 120 requests/minute

---

## 라이선스

Apache License 2.0

---

## 기여

이슈 및 Pull Request를 환영합니다!

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 참고 자료

- [yfinance (Python)](https://github.com/ranaroussi/yfinance)
- [fredapi (Python)](https://github.com/mortada/fredapi)
- [FRED API Documentation](https://fred.stlouisfed.org/docs/api/fred/)
- [Ktor Documentation](https://ktor.io/docs/)

---

**Made with Kotlin**
