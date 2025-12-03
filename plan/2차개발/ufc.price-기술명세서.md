# UFC.PRICE 기술명세서 (Technical Specification)

## 문서 정보
- **버전**: 2.0.0
- **최종 수정일**: 2025-12-03
- **작성자**: Claude Code (Software Architect)
- **문서 상태**: Design Specification
- **문서 유형**: 설계 명세서 (코드 구현 제외)

---

## 목차
1. [개요](#1-개요)
2. [아키텍처 설계](#2-아키텍처-설계)
3. [데이터 모델 정의](#3-데이터-모델-정의)
4. [서비스 인터페이스](#4-서비스-인터페이스)
5. [API 명세](#5-api-명세)
6. [데이터 처리 흐름](#6-데이터-처리-흐름)
7. [에러 처리 전략](#7-에러-처리-전략)
8. [캐싱 전략](#8-캐싱-전략)
9. [테스트 전략](#9-테스트-전략)
10. [파일 구조](#10-파일-구조)
11. [향후 확장](#11-향후-확장)

---

## 1. 개요

### 1.1 목적

Price 도메인은 Yahoo Finance를 통해 금융 상품(주식, ETF, 펀드, 지수)의 가격 정보를 제공하는 통합 서비스입니다.

**핵심 기능:**
- 현재 가격 정보 조회 (실시간/지연)
- OHLCV 히스토리 데이터 조회
- 52주 최고/최저가, 이동평균선 정보
- 시가총액, 거래량, 배당수익률
- 다양한 기간(Period)과 간격(Interval) 지원

### 1.2 범위

**지원 자산 유형:**
- EQUITY (주식)
- ETF (상장지수펀드)
- MUTUALFUND (뮤추얼펀드)
- INDEX (지수, 예: ^GSPC)
- CRYPTOCURRENCY (암호화폐)

**제공 데이터:**

| 카테고리 | 데이터 | 설명 |
|---------|--------|------|
| **현재 가격** | lastPrice, regularMarketPrice | 최근 거래 가격 |
| **일중 범위** | open, high, low, close | 일중 OHLC |
| **거래량** | volume, regularMarketVolume | 거래량 정보 |
| **52주 범위** | fiftyTwoWeekHigh, fiftyTwoWeekLow | 연간 최고/최저가 |
| **이동평균** | fiftyDayAverage, twoHundredDayAverage | 50일/200일 이동평균선 |
| **시장 정보** | marketCap, currency, exchange | 시가총액, 통화, 거래소 |
| **배당/수익** | dividendYield, dividendRate | 배당 정보 (선택) |
| **가격 히스토리** | OHLCV 시계열 데이터 | timestamp, open, high, low, close, volume, adjClose |

### 1.3 데이터 소스

**Yahoo Finance API:**

**1) 현재 가격 정보 (quoteSummary API):**
- 엔드포인트: `https://query2.finance.yahoo.com/v10/finance/quoteSummary/{symbol}`
- 모듈: `price`, `summaryDetail`
- 인증: crumb 토큰 기반

**2) 가격 히스토리 (chart API):**
- 엔드포인트: `https://query2.finance.yahoo.com/v8/finance/chart/{symbol}`
- 파라미터: `period`, `interval`, `range`
- 인증: crumb 토큰 기반

### 1.4 yfinance 호환성

**Python yfinance의 가격 관련 기능을 Kotlin으로 이식:**

| yfinance 메서드 | UFC.PRICE 메서드 | 설명 |
|----------------|-----------------|------|
| `Ticker.info['currentPrice']` | `getCurrentPrice()` | 현재 가격 |
| `Ticker.history()` | `getPriceHistory()` | 가격 히스토리 |
| `Ticker.info['regularMarketPrice']` | `getCurrentPrice().regularMarketPrice` | 정규 시장 가격 |
| `Ticker.info['fiftyTwoWeekHigh']` | `getCurrentPrice().fiftyTwoWeekHigh` | 52주 최고가 |
| `Ticker.get_history_metadata()` | `getHistoryMetadata()` | 히스토리 메타데이터 |

**주요 차이점:**
- 타입 안전성: Kotlin의 강타입 시스템 활용
- Null 안전성: Nullable 타입 명시
- 도메인 분리: 가격 정보만 전담 (펀드 정보는 ufc.funds)

### 1.5 Chart API와의 관계

**UFC 프로젝트의 기존 ChartService와의 차이:**

| 항목 | ChartService | PriceApi |
|-----|-------------|----------|
| **목적** | OHLCV 히스토리 데이터 전문 | 현재 가격 + 히스토리 통합 |
| **엔드포인트** | chart API만 사용 | quoteSummary + chart API |
| **반환 모델** | `OHLCVData` | `PriceData`, `OHLCV` |
| **현재 가격** | 제공 안 함 | 주요 기능 |
| **52주 범위** | 메타데이터로 제공 | 주요 기능 |
| **캐싱** | 없음 | TTL 기반 차등 캐싱 |

**통합 전략:**
- PriceApi는 ChartService를 내부적으로 활용
- 가격 히스토리 요청 시 ChartService 위임
- 현재 가격은 quoteSummary API로 별도 조회

---

## 2. 아키텍처 설계

### 2.1 레이어 구조

```
┌─────────────────────────────────────────────────────────────┐
│                     Application Layer                       │
│                   (User Application)                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   Domain API Layer                          │
│                                                              │
│  ┌────────────────────────────────────────────────────┐     │
│  │              PriceApi (Interface)                   │     │
│  │  - getCurrentPrice(symbol): PriceData               │     │
│  │  - getCurrentPrice(symbols): Map<String, PriceData> │     │
│  │  - getPriceHistory(symbol, ...): List<OHLCV>        │     │
│  │  - getRawPrice(symbol): PriceResponse               │     │
│  │  - getRawPriceHistory(...): ChartResponse           │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                 Internal Implementation                     │
│                                                              │
│  ┌────────────────────────────────────────────────────┐     │
│  │         YahooPrice Service (Implementation)         │     │
│  │  - httpClient: HttpClient                           │     │
│  │  - rateLimiter: RateLimiter                         │     │
│  │  - authResult: AuthResult                           │     │
│  │  - chartService: ChartService (위임)                │     │
│  │  - priceCache: ConcurrentHashMap<String, CachedData>│     │
│  │                                                      │     │
│  │  Private Methods:                                    │     │
│  │  - parseCurrentPrice()                               │     │
│  │  - parseMarketInfo()                                 │     │
│  │  - parseMovingAverages()                             │     │
│  │  - parseDividendInfo()                               │     │
│  │  - isCacheValid()                                    │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   Source Layer                              │
│                                                              │
│  ┌────────────────────────────────────────────────────┐     │
│  │         YahooFinanceSource                          │     │
│  │  - QuoteSummaryAPI (현재 가격)                      │     │
│  │  - ChartAPI (가격 히스토리)                         │     │
│  │  - Authenticator (Cookie/Crumb)                     │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              Infrastructure Layer                           │
│                                                              │
│  ┌─────────────┐  ┌──────────┐  ┌────────┐  ┌──────────┐   │
│  │ HTTP Client │  │  Cache   │  │ Retry  │  │   Log    │   │
│  │   (Ktor)    │  │ (TTL+LRU)│  │ Logic  │  │ (SLF4J)  │   │
│  └─────────────┘  └──────────┘  └────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 컴포넌트 다이어그램

```
┌─────────────────────────────────────────────────────────────┐
│                      PriceApi                               │
│                                                              │
│  Public Methods:                                             │
│  ┌────────────────────────────────────────────────────┐     │
│  │ getCurrentPrice(symbol)                             │     │
│  │   └─> PriceData (현재 가격 + 통계)                  │     │
│  │                                                      │     │
│  │ getCurrentPrice(symbols)                             │     │
│  │   └─> Map<String, PriceData>                        │     │
│  │                                                      │     │
│  │ getPriceHistory(symbol, period, interval)            │     │
│  │   └─> List<OHLCV> (시계열 데이터)                   │     │
│  │                                                      │     │
│  │ getRawPrice(symbol)                                  │     │
│  │   └─> PriceResponse (quoteSummary 원본)             │     │
│  │                                                      │     │
│  │ getRawPriceHistory(symbol, period, interval)         │     │
│  │   └─> ChartResponse (chart API 원본)                │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            ↓ uses
┌─────────────────────────────────────────────────────────────┐
│                 YahooFinanceSource                          │
│                                                              │
│  ┌────────────────────────────────────────────────────┐     │
│  │ quoteSummary(symbol, modules)                       │     │
│  │   └─> QuoteSummaryResponse (price, summaryDetail)  │     │
│  │                                                      │     │
│  │ chart(symbol, period, interval)                     │     │
│  │   └─> ChartDataResponse (OHLCV)                     │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            ↓ delegates to
┌─────────────────────────────────────────────────────────────┐
│                     ChartService                            │
│                                                              │
│  ┌────────────────────────────────────────────────────┐     │
│  │ getChartData(symbol, interval, period)              │     │
│  │   └─> List<OHLCVData>                               │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 책임 분리 (Separation of Concerns)

| 레이어 | 책임 | 예시 |
|--------|------|------|
| **Domain API** | 비즈니스 로직, 유효성 검증 | 심볼 검증, 기간/인터벌 조합 검증 |
| **Internal** | 데이터 변환, 캐시 관리 | API 응답 → PriceData, TTL 캐시 |
| **Source** | 데이터 소스 추상화 | quoteSummary, chart API 호출 |
| **Infrastructure** | 공통 인프라 | HTTP, Cache, Retry, Logging |

---

## 3. 데이터 모델 정의

### 3.1 Domain 모델 (Public API)

#### PriceData (현재 가격 정보)

**목적:** 금융 상품의 현재 가격 및 관련 통계 정보

```kotlin
data class PriceData(
    // 기본 정보
    val symbol: String,
    val currency: String?,
    val exchange: String?,

    // 현재 가격
    val lastPrice: Double?,
    val regularMarketPrice: Double?,
    val regularMarketTime: Long?,  // Unix timestamp

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

    // 시가총액
    val marketCap: Long?,

    // 배당 정보 (선택)
    val dividendYield: Double?,
    val dividendRate: Double?,
    val exDividendDate: Long?,  // Unix timestamp

    // 기타
    val beta: Double?,
    val trailingPE: Double?,
    val forwardPE: Double?
) {
    /**
     * 52주 범위 대비 현재 가격의 위치 (0.0 ~ 1.0)
     * 0.0 = 52주 최저가, 1.0 = 52주 최고가
     */
    fun fiftyTwoWeekPosition(): Double? {
        val price = lastPrice ?: regularMarketPrice ?: return null
        val high = fiftyTwoWeekHigh ?: return null
        val low = fiftyTwoWeekLow ?: return null

        if (high == low) return null
        return (price - low) / (high - low)
    }

    /**
     * 일중 가격 변동폭 (%)
     */
    fun dailyChangePercent(): Double? {
        val current = lastPrice ?: regularMarketPrice ?: return null
        val prev = previousClose ?: return null

        if (prev == 0.0) return null
        return ((current - prev) / prev) * 100.0
    }

    /**
     * 가격이 50일 이동평균선 위에 있는지 확인
     */
    fun isAbove50DayMA(): Boolean? {
        val price = lastPrice ?: regularMarketPrice ?: return null
        val ma50 = fiftyDayAverage ?: return null
        return price > ma50
    }

    /**
     * 가격이 200일 이동평균선 위에 있는지 확인
     */
    fun isAbove200DayMA(): Boolean? {
        val price = lastPrice ?: regularMarketPrice ?: return null
        val ma200 = twoHundredDayAverage ?: return null
        return price > ma200
    }
}
```

#### OHLCV (가격 히스토리 데이터 포인트)

**목적:** 특정 시점의 OHLCV(Open, High, Low, Close, Volume) 데이터

```kotlin
data class OHLCV(
    val timestamp: Long,           // Unix timestamp (seconds)
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val adjClose: Double?,         // Adjusted Close (배당/분할 조정)
    val volume: Long
) {
    /**
     * 일중 가격 변동폭 (High - Low)
     */
    fun range(): Double = high - low

    /**
     * 일중 변동률 (%)
     */
    fun rangePercent(): Double {
        if (low == 0.0) return 0.0
        return ((high - low) / low) * 100.0
    }

    /**
     * 종가 기준 변동 (Close - Open)
     */
    fun change(): Double = close - open

    /**
     * 종가 기준 변동률 (%)
     */
    fun changePercent(): Double {
        if (open == 0.0) return 0.0
        return ((close - open) / open) * 100.0
    }

    /**
     * 양봉 여부 (Close > Open)
     */
    fun isBullish(): Boolean = close > open

    /**
     * 음봉 여부 (Close < Open)
     */
    fun isBearish(): Boolean = close < open
}
```

#### ChartMetadata (차트 메타데이터)

**목적:** 가격 히스토리 조회 결과의 메타 정보

```kotlin
data class ChartMetadata(
    val symbol: String,
    val currency: String?,
    val exchangeName: String?,
    val timezone: String?,
    val regularMarketPrice: Double?,
    val regularMarketTime: Long?,
    val dataGranularity: String?,   // "1d", "1h", etc.
    val range: String?,              // "1y", "1mo", etc.
    val validRanges: List<String>?
)
```

### 3.2 Response 모델 (Internal)

#### PriceResponse (quoteSummary API 응답)

```kotlin
@Serializable
data class PriceResponse(
    @SerialName("quoteSummary")
    val quoteSummary: QuoteSummary
)

@Serializable
data class QuoteSummary(
    @SerialName("result")
    val result: List<QuoteSummaryResult>? = null,

    @SerialName("error")
    val error: ApiError? = null
)

@Serializable
data class QuoteSummaryResult(
    @SerialName("price")
    val price: PriceModuleRaw? = null,

    @SerialName("summaryDetail")
    val summaryDetail: SummaryDetailRaw? = null
)
```

#### PriceModuleRaw (price 모듈)

```kotlin
@Serializable
data class PriceModuleRaw(
    @SerialName("symbol")
    val symbol: String? = null,

    @SerialName("currency")
    val currency: String? = null,

    @SerialName("exchange")
    val exchange: String? = null,

    @SerialName("regularMarketPrice")
    val regularMarketPrice: ValueFormat? = null,

    @SerialName("regularMarketTime")
    val regularMarketTime: Long? = null,

    @SerialName("regularMarketOpen")
    val regularMarketOpen: ValueFormat? = null,

    @SerialName("regularMarketDayHigh")
    val regularMarketDayHigh: ValueFormat? = null,

    @SerialName("regularMarketDayLow")
    val regularMarketDayLow: ValueFormat? = null,

    @SerialName("regularMarketVolume")
    val regularMarketVolume: ValueFormat? = null,

    @SerialName("regularMarketPreviousClose")
    val regularMarketPreviousClose: ValueFormat? = null,

    @SerialName("marketCap")
    val marketCap: ValueFormat? = null
)
```

#### SummaryDetailRaw (summaryDetail 모듈)

```kotlin
@Serializable
data class SummaryDetailRaw(
    @SerialName("fiftyTwoWeekHigh")
    val fiftyTwoWeekHigh: ValueFormat? = null,

    @SerialName("fiftyTwoWeekLow")
    val fiftyTwoWeekLow: ValueFormat? = null,

    @SerialName("fiftyTwoWeekChange")
    val fiftyTwoWeekChange: ValueFormat? = null,

    @SerialName("fiftyTwoWeekChangePercent")
    val fiftyTwoWeekChangePercent: ValueFormat? = null,

    @SerialName("fiftyDayAverage")
    val fiftyDayAverage: ValueFormat? = null,

    @SerialName("twoHundredDayAverage")
    val twoHundredDayAverage: ValueFormat? = null,

    @SerialName("averageVolume")
    val averageVolume: ValueFormat? = null,

    @SerialName("averageVolume10days")
    val averageVolume10days: ValueFormat? = null,

    @SerialName("dividendYield")
    val dividendYield: ValueFormat? = null,

    @SerialName("dividendRate")
    val dividendRate: ValueFormat? = null,

    @SerialName("exDividendDate")
    val exDividendDate: ValueFormat? = null,

    @SerialName("beta")
    val beta: ValueFormat? = null,

    @SerialName("trailingPE")
    val trailingPE: ValueFormat? = null,

    @SerialName("forwardPE")
    val forwardPE: ValueFormat? = null,

    @SerialName("volume")
    val volume: ValueFormat? = null,

    @SerialName("previousClose")
    val previousClose: ValueFormat? = null,

    @SerialName("open")
    val open: ValueFormat? = null,

    @SerialName("dayHigh")
    val dayHigh: ValueFormat? = null,

    @SerialName("dayLow")
    val dayLow: ValueFormat? = null,

    @SerialName("marketCap")
    val marketCap: ValueFormat? = null
)
```

#### ValueFormat (Yahoo Finance 표준 포맷)

```kotlin
@Serializable
data class ValueFormat(
    @SerialName("raw")
    val raw: Double? = null,

    @SerialName("fmt")
    val fmt: String? = null,

    @SerialName("longFmt")
    val longFmt: String? = null
)
```

#### ChartResponse (chart API 응답)

```kotlin
// 기존 ChartDataResponse 재사용
@Serializable
data class ChartResponse(
    @SerialName("chart")
    val chart: Chart
)

@Serializable
data class Chart(
    @SerialName("result")
    val result: List<ChartResult>? = null,

    @SerialName("error")
    val error: ChartError? = null
)
```

### 3.3 데이터 변환 전략

**변환 규칙:**

| Response 모델 | Domain 모델 | 변환 규칙 |
|--------------|------------|----------|
| `PriceModuleRaw` | `PriceData` | ValueFormat.raw 추출 |
| `SummaryDetailRaw` | `PriceData` | ValueFormat.raw 추출 (병합) |
| `ChartResult` | `List<OHLCV>` | timestamp + indicators 조합 |
| `ChartMeta` | `ChartMetadata` | 메타 정보 추출 |

**병합 전략 (price + summaryDetail):**
- `price` 모듈: 현재 가격, 거래량, 일중 범위
- `summaryDetail` 모듈: 52주 범위, 이동평균, 배당 정보
- 중복 필드 처리: `price` 모듈 우선 (더 최신 데이터)

**변환 위치:**
- `YahooPriceService` 내부 private 메서드
- 응답 모델은 외부 노출 금지

### 3.4 캐시 데이터 모델

```kotlin
internal data class CachedPriceData(
    val data: PriceData,
    val cachedAt: Long,           // Unix timestamp (millis)
    val ttlMillis: Long           // Time-To-Live (millis)
) {
    fun isExpired(currentTimeMillis: Long): Boolean {
        return (currentTimeMillis - cachedAt) > ttlMillis
    }
}
```

---

## 4. 서비스 인터페이스

### 4.1 PriceApi (Public Interface)

```kotlin
interface PriceApi {

    /**
     * 현재 가격 정보 조회
     *
     * @param symbol 심볼 (예: AAPL, SPY, ^GSPC)
     * @return PriceData 현재 가격 및 통계
     * @throws UFCException
     */
    suspend fun getCurrentPrice(symbol: String): PriceData

    /**
     * 다중 심볼의 현재 가격 조회
     *
     * @param symbols 심볼 목록
     * @return Map<String, PriceData> 심볼별 가격 정보
     * @throws UFCException
     */
    suspend fun getCurrentPrice(symbols: List<String>): Map<String, PriceData>

    /**
     * 가격 히스토리 조회 (기간 기반)
     *
     * @param symbol 심볼
     * @param period 조회 기간 (예: Period.OneYear)
     * @param interval 데이터 간격 (기본값: Interval.OneDay)
     * @return List<OHLCV> 시계열 OHLCV 데이터
     * @throws UFCException
     */
    suspend fun getPriceHistory(
        symbol: String,
        period: Period,
        interval: Interval = Interval.OneDay
    ): List<OHLCV>

    /**
     * 가격 히스토리 조회 (날짜 범위 기반)
     *
     * @param symbol 심볼
     * @param start 시작 날짜
     * @param end 종료 날짜
     * @param interval 데이터 간격 (기본값: Interval.OneDay)
     * @return List<OHLCV> 시계열 OHLCV 데이터
     * @throws UFCException
     */
    suspend fun getPriceHistory(
        symbol: String,
        start: LocalDate,
        end: LocalDate,
        interval: Interval = Interval.OneDay
    ): List<OHLCV>

    /**
     * 가격 히스토리 조회 (메타데이터 포함)
     *
     * @param symbol 심볼
     * @param period 조회 기간
     * @param interval 데이터 간격
     * @return Pair<ChartMetadata, List<OHLCV>>
     * @throws UFCException
     */
    suspend fun getPriceHistoryWithMetadata(
        symbol: String,
        period: Period,
        interval: Interval = Interval.OneDay
    ): Pair<ChartMetadata, List<OHLCV>>

    /**
     * 히스토리 메타데이터 조회 (chart API)
     *
     * yfinance의 get_history_metadata()에 해당
     * 가격 히스토리 없이 메타데이터만 조회
     *
     * @param symbol 심볼
     * @return ChartMetadata 히스토리 메타데이터
     * @throws UFCException
     */
    suspend fun getHistoryMetadata(symbol: String): ChartMetadata

    /**
     * 원본 가격 응답 조회 (quoteSummary API)
     *
     * @param symbol 심볼
     * @return PriceResponse 원본 API 응답
     * @throws UFCException
     */
    suspend fun getRawPrice(symbol: String): PriceResponse

    /**
     * 원본 차트 응답 조회 (chart API)
     *
     * @param symbol 심볼
     * @param period 조회 기간
     * @param interval 데이터 간격
     * @return ChartResponse 원본 API 응답
     * @throws UFCException
     */
    suspend fun getRawPriceHistory(
        symbol: String,
        period: Period,
        interval: Interval = Interval.OneDay
    ): ChartResponse
}
```

### 4.2 YahooPriceService (Internal Implementation)

```kotlin
internal class YahooPriceService(
    private val httpClient: HttpClient,
    private val rateLimiter: RateLimiter,
    private val authResult: AuthResult,
    private val chartService: ChartService
) : PriceApi {

    // 캐시: Symbol -> CachedPriceData
    private val priceCache = ConcurrentHashMap<String, CachedPriceData>()

    // 캐시 TTL 설정
    companion object {
        const val CURRENT_PRICE_TTL_MILLIS = 60_000L     // 1분 (현재 가격)
        const val HISTORY_CACHE_TTL_MILLIS = 3_600_000L  // 1시간 (히스토리)
    }

    // Public API 구현
    override suspend fun getCurrentPrice(symbol: String): PriceData
    override suspend fun getCurrentPrice(symbols: List<String>): Map<String, PriceData>
    override suspend fun getPriceHistory(
        symbol: String,
        period: Period,
        interval: Interval
    ): List<OHLCV>
    override suspend fun getPriceHistory(
        symbol: String,
        start: LocalDate,
        end: LocalDate,
        interval: Interval
    ): List<OHLCV>
    override suspend fun getPriceHistoryWithMetadata(
        symbol: String,
        period: Period,
        interval: Interval
    ): Pair<ChartMetadata, List<OHLCV>>
    override suspend fun getHistoryMetadata(symbol: String): ChartMetadata
    override suspend fun getRawPrice(symbol: String): PriceResponse
    override suspend fun getRawPriceHistory(
        symbol: String,
        period: Period,
        interval: Interval
    ): ChartResponse

    // Private 헬퍼 메서드
    private suspend fun fetchQuoteSummary(symbol: String, modules: List<String>): PriceResponse
    private fun validateSymbol(symbol: String)
    private fun validatePeriodInterval(period: Period, interval: Interval)
    private fun parsePriceData(symbol: String, response: PriceResponse): PriceData
    private fun parseCurrentPrice(price: PriceModuleRaw?, summary: SummaryDetailRaw?): PriceData
    private fun parseMarketInfo(price: PriceModuleRaw?): Triple<String?, String?, String?>
    private fun parseMovingAverages(summary: SummaryDetailRaw?): Pair<Double?, Double?>
    private fun parseDividendInfo(summary: SummaryDetailRaw?): Triple<Double?, Double?, Long?>
    private fun convertChartToOHLCV(chartResponse: ChartResponse): List<OHLCV>
    private fun extractChartMetadata(chartResponse: ChartResponse): ChartMetadata
    private fun getCachedPrice(symbol: String): PriceData?
    private fun setCachedPrice(symbol: String, data: PriceData)
}
```

---

## 5. API 명세

### 5.1 Yahoo Finance quoteSummary API

**Base URL:**
```
https://query2.finance.yahoo.com/v10/finance/quoteSummary/{symbol}
```

**Request Method:** GET

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| modules | String | Y | 쉼표로 구분된 모듈 (price,summaryDetail) |
| crumb | String | Y | 인증 토큰 |
| corsDomain | String | N | CORS 도메인 |
| formatted | String | N | 포맷 여부 (기본값: false) |

**Price API 모듈:**

| 모듈 | 설명 | 제공 데이터 |
|------|------|-----------|
| price | 현재 가격 정보 | regularMarketPrice, open, high, low, volume |
| summaryDetail | 상세 통계 | 52주 범위, 이동평균, 배당, 베타 |

**Request Example:**
```
GET /v10/finance/quoteSummary/AAPL?modules=price,summaryDetail&crumb=xxx
Host: query2.finance.yahoo.com
Cookie: A3=xxx
```

**Response Structure (Success):**

```json
{
  "quoteSummary": {
    "result": [
      {
        "price": {
          "symbol": "AAPL",
          "currency": "USD",
          "exchange": "NMS",
          "regularMarketPrice": {"raw": 178.25, "fmt": "178.25"},
          "regularMarketTime": 1701460800,
          "regularMarketOpen": {"raw": 177.50, "fmt": "177.50"},
          "regularMarketDayHigh": {"raw": 179.00, "fmt": "179.00"},
          "regularMarketDayLow": {"raw": 177.00, "fmt": "177.00"},
          "regularMarketVolume": {"raw": 52341200, "fmt": "52.34M"},
          "regularMarketPreviousClose": {"raw": 177.80, "fmt": "177.80"},
          "marketCap": {"raw": 2850000000000, "fmt": "2.85T"}
        },
        "summaryDetail": {
          "fiftyTwoWeekHigh": {"raw": 199.62, "fmt": "199.62"},
          "fiftyTwoWeekLow": {"raw": 124.17, "fmt": "124.17"},
          "fiftyTwoWeekChange": {"raw": 30.50, "fmt": "30.50"},
          "fiftyTwoWeekChangePercent": {"raw": 0.2065, "fmt": "20.65%"},
          "fiftyDayAverage": {"raw": 175.50, "fmt": "175.50"},
          "twoHundredDayAverage": {"raw": 165.30, "fmt": "165.30"},
          "averageVolume": {"raw": 58000000, "fmt": "58M"},
          "averageVolume10days": {"raw": 55000000, "fmt": "55M"},
          "dividendYield": {"raw": 0.0048, "fmt": "0.48%"},
          "dividendRate": {"raw": 0.96, "fmt": "0.96"},
          "exDividendDate": {"raw": 1699574400, "fmt": "Nov 10, 2023"},
          "beta": {"raw": 1.29, "fmt": "1.29"},
          "trailingPE": {"raw": 29.5, "fmt": "29.50"},
          "forwardPE": {"raw": 27.2, "fmt": "27.20"}
        }
      }
    ],
    "error": null
  }
}
```

**Error Response:**

```json
{
  "quoteSummary": {
    "result": null,
    "error": {
      "code": "Not Found",
      "description": "No data found for symbol INVALID"
    }
  }
}
```

### 5.2 Yahoo Finance chart API

**Base URL:**
```
https://query2.finance.yahoo.com/v8/finance/chart/{symbol}
```

**Request Method:** GET

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| period1 | Long | N | 시작 Unix timestamp (seconds) |
| period2 | Long | N | 종료 Unix timestamp (seconds) |
| range | String | N | 조회 기간 (1d, 5d, 1mo, 3mo, 6mo, 1y, 2y, 5y, 10y, ytd, max) |
| interval | String | Y | 데이터 간격 (1m, 2m, 5m, 15m, 30m, 60m, 90m, 1h, 1d, 5d, 1wk, 1mo, 3mo) |
| events | String | N | 포함할 이벤트 (div, split) |
| includeAdjustedClose | Boolean | N | 조정 종가 포함 여부 |
| crumb | String | Y | 인증 토큰 |

**Period vs Date Range:**
- `range` 파라미터 사용: 상대 기간 (예: 1y = 1년 전부터 현재까지)
- `period1`, `period2` 사용: 절대 날짜 범위

**Interval 제약사항:**

| Interval | 최대 조회 기간 |
|----------|---------------|
| 1m, 2m, 5m, 15m, 30m | 7일 |
| 60m, 90m, 1h | 60일 |
| 1d | 제한 없음 |
| 1wk, 1mo, 3mo | 제한 없음 |

**Request Example (Period):**
```
GET /v8/finance/chart/AAPL?range=1y&interval=1d&crumb=xxx
Host: query2.finance.yahoo.com
Cookie: A3=xxx
```

**Request Example (Date Range):**
```
GET /v8/finance/chart/AAPL?period1=1609459200&period2=1640995200&interval=1d&crumb=xxx
Host: query2.finance.yahoo.com
Cookie: A3=xxx
```

**Response Structure (Success):**

```json
{
  "chart": {
    "result": [
      {
        "meta": {
          "symbol": "AAPL",
          "currency": "USD",
          "exchangeName": "NMS",
          "timezone": "EST",
          "regularMarketPrice": 178.25,
          "regularMarketTime": 1701460800,
          "dataGranularity": "1d",
          "range": "1y",
          "validRanges": ["1d", "5d", "1mo", "3mo", "6mo", "1y", "2y", "5y", "10y", "ytd", "max"]
        },
        "timestamp": [1670000000, 1670086400, 1670172800],
        "indicators": {
          "quote": [
            {
              "open": [145.50, 146.20, 147.00],
              "high": [147.00, 148.50, 149.20],
              "low": [144.80, 145.50, 146.30],
              "close": [146.50, 147.80, 148.90],
              "volume": [58000000, 62000000, 55000000]
            }
          ],
          "adjclose": [
            {
              "adjclose": [146.20, 147.50, 148.60]
            }
          ]
        }
      }
    ],
    "error": null
  }
}
```

### 5.3 Rate Limiting

**제한:**
- Yahoo Finance: 비공식 제한 (추정 2000 req/hour)
- quoteSummary: 공유 Rate Limiter (FundsApi와 동일 엔드포인트)
- chart: 별도 Rate Limiter (다른 엔드포인트)

**전략:**
- TokenBucket Rate Limiter 적용
- Exponential Backoff (429 응답 시)
- Cache 우선 사용 (TTL 기반)

**Rate Limiter 설정:**

| API | Tokens/Second | Burst Size |
|-----|---------------|------------|
| quoteSummary | 5 | 10 |
| chart | 10 | 20 |

### 5.4 타임존 및 시간 처리

**Yahoo Finance 타임존:**
- timestamp: UTC 기준 Unix timestamp (seconds)
- regularMarketTime: 해당 거래소의 현지 시간 (Unix timestamp)

**처리 전략:**
- 모든 timestamp는 UTC 기준으로 저장
- 클라이언트에서 필요 시 로컬 타임존으로 변환
- exDividendDate, regularMarketTime 등도 Unix timestamp로 통일

---

## 6. 데이터 처리 흐름

### 6.1 getCurrentPrice() 흐름도

```
User Request: getCurrentPrice("AAPL")
    ↓
[1] 심볼 검증
    - 빈 문자열 체크
    - 길이 제한 (1-20자)
    - 유효 문자 체크 (영문, 숫자, ^, ., -, _)
    ↓
[2] 캐시 조회 (Key: "AAPL")
    ↓
    ├─> Cache Hit && Not Expired
    │   → PriceData 반환 (API 호출 생략)
    │
    └─> Cache Miss || Expired
         ↓
        [3] Rate Limiter 토큰 획득 (quoteSummary)
            ↓
        [4] QuoteSummary API 호출
            - modules: price,summaryDetail
            - 인증: Cookie + Crumb
            ↓
        [5] 응답 검증
            - HTTP 상태 확인 (200 OK)
            - error 필드 확인 (null)
            - result null/empty 체크
            ↓
        [6] 데이터 파싱
            - parseCurrentPrice(price, summaryDetail)
            - parseMarketInfo(price)
            - parseMovingAverages(summaryDetail)
            - parseDividendInfo(summaryDetail)
            ↓
        [7] PriceData 생성
            - 모든 필드 병합
            - null 안전 처리
            ↓
        [8] 캐시 저장
            - TTL: 60초 (CURRENT_PRICE_TTL_MILLIS)
            - Key: symbol
            ↓
        [9] PriceData 반환
```

### 6.2 getPriceHistory() 흐름도 (Period 기반)

```
User Request: getPriceHistory("AAPL", Period.OneYear, Interval.OneDay)
    ↓
[1] 입력 검증
    - 심볼 검증
    - Period/Interval 조합 검증
    ↓
[2] ChartService에 위임
    - chartService.getChartData(symbol, interval, period)
    ↓
[3] ChartService 내부
    ↓
    [3-1] Rate Limiter 토큰 획득 (chart)
    ↓
    [3-2] Chart API 호출
        - range: period.value (예: "1y")
        - interval: interval.value (예: "1d")
        - includeAdjustedClose: true
        ↓
    [3-3] 응답 검증
        - HTTP 상태 확인
        - error 필드 확인
        - result null/empty 체크
        ↓
    [3-4] OHLCV 데이터 변환
        - timestamp 배열 순회
        - quote.open[i], high[i], low[i], close[i], volume[i]
        - adjclose[i] (있을 경우)
        - null 값 필터링
        ↓
    [3-5] List<OHLCVData> 반환
    ↓
[4] OHLCV -> List<OHLCV> 변환
    - OHLCVData -> OHLCV 매핑
    ↓
[5] List<OHLCV> 반환
```

### 6.3 getPriceHistory() 흐름도 (Date Range 기반)

```
User Request: getPriceHistory("AAPL", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31), Interval.OneDay)
    ↓
[1] 입력 검증
    - 심볼 검증
    - 날짜 범위 검증 (start < end)
    - Interval 검증
    ↓
[2] LocalDate -> Unix Timestamp 변환
    - start: LocalDate.atStartOfDay(UTC).toEpochSecond()
    - end: LocalDate.atTime(23, 59, 59).toEpochSecond(UTC)
    ↓
[3] Rate Limiter 토큰 획득
    ↓
[4] Chart API 호출
    - period1: startTimestamp
    - period2: endTimestamp
    - interval: interval.value
    ↓
[5] 응답 처리 (Period 기반과 동일)
    ↓
[6] List<OHLCV> 반환
```

### 6.4 getCurrentPrice(symbols) 다중 조회 흐름도

```
User Request: getCurrentPrice(listOf("AAPL", "GOOGL", "MSFT"))
    ↓
[1] 입력 검증
    - 빈 리스트 체크
    - 심볼 개수 제한 (최대 50개)
    - 각 심볼 유효성 검증
    ↓
[2] 캐시 필터링
    - symbols.partition { getCachedPrice(it) != null }
    - cachedSymbols: 캐시에서 반환
    - uncachedSymbols: API 호출 필요
    ↓
[3] 병렬 API 호출 (coroutineScope)
    - uncachedSymbols.map { symbol ->
          async { getCurrentPrice(symbol) }
      }
    ↓
[4] 결과 병합
    - cachedResults + apiResults
    ↓
[5] Map<String, PriceData> 반환
```

### 6.5 에러 시나리오 흐름

```
API 호출 실패
    ↓
[1] HTTP Status 확인
    ├─> 401/403 → ErrorCode.AUTH_FAILED
    ├─> 404 → ErrorCode.PRICE_DATA_NOT_FOUND
    ├─> 429 → ErrorCode.RATE_LIMITED
    ├─> 500/502/503 → ErrorCode.EXTERNAL_API_ERROR
    └─> 기타 → ErrorCode.DATA_RETRIEVAL_ERROR
    ↓
[2] Retry 로직
    - 401/403/404: Retry 없음
    - 429: Exponential Backoff (최대 3회)
    - 500+: Exponential Backoff (최대 3회)
    ↓
[3] UFCException 생성
    - errorCode 설정
    - message 설정
    - metadata 추가 (symbol, api, status)
    ↓
[4] Exception throw
```

### 6.6 데이터 병합 전략 (price + summaryDetail)

```
price 모듈:
  - regularMarketPrice, open, high, low, volume
  - currency, exchange, marketCap

summaryDetail 모듈:
  - fiftyTwoWeekHigh/Low, fiftyDayAverage, twoHundredDayAverage
  - averageVolume, averageVolume10days
  - dividendYield, dividendRate, exDividendDate
  - beta, trailingPE, forwardPE
  - previousClose, open, dayHigh, dayLow (중복)

병합 규칙:
  1. price 모듈 우선 (더 최신)
  2. summaryDetail은 price에 없는 필드만 추가
  3. 중복 필드: price 모듈 값 사용
     - open: price.regularMarketOpen vs summaryDetail.open → price 우선
     - volume: price.regularMarketVolume vs summaryDetail.volume → price 우선
```

---

## 7. 에러 처리 전략

### 7.1 에러 분류

| ErrorCode | HTTP Status | 설명 | Retry 여부 |
|-----------|------------|------|-----------|
| INVALID_SYMBOL | - | 심볼 형식 오류 | No |
| PRICE_DATA_NOT_FOUND | 404 | 가격 데이터 없음 | No |
| INVALID_PERIOD_INTERVAL | - | Period/Interval 조합 오류 | No |
| INVALID_DATE_RANGE | - | 날짜 범위 오류 | No |
| AUTH_FAILED | 401, 403 | 인증 실패 | No (재인증 필요) |
| RATE_LIMITED | 429 | Rate Limit 초과 | Yes (Backoff) |
| EXTERNAL_API_ERROR | 500+ | API 서버 오류 | Yes |
| DATA_PARSING_ERROR | - | 파싱 오류 | No |
| INCOMPLETE_PRICE_DATA | - | 불완전한 가격 데이터 | No |

### 7.2 커스텀 예외

```kotlin
// InvalidPeriodIntervalException
class InvalidPeriodIntervalException(
    message: String,
    cause: Throwable? = null
) : UFCException(ErrorCode.INVALID_PERIOD_INTERVAL, message, cause)

// InvalidDateRangeException
class InvalidDateRangeException(
    message: String,
    cause: Throwable? = null
) : UFCException(ErrorCode.INVALID_DATE_RANGE, message, cause)

// IncompletePriceDataException
class IncompletePriceDataException(
    message: String,
    cause: Throwable? = null
) : UFCException(ErrorCode.INCOMPLETE_PRICE_DATA, message, cause)
```

### 7.3 Period/Interval 조합 검증

**제약 조건:**

| Interval | 최대 Period |
|----------|-------------|
| 1m, 2m, 5m, 15m, 30m | 7일 |
| 60m, 90m, 1h | 60일 |
| 1d, 5d, 1wk, 1mo, 3mo | 제한 없음 |

**검증 로직:**

```kotlin
private fun validatePeriodInterval(period: Period, interval: Interval) {
    val daysInPeriod = when (period) {
        Period.OneDay -> 1
        Period.FiveDays -> 5
        Period.OneMonth -> 30
        Period.ThreeMonths -> 90
        Period.SixMonths -> 180
        Period.OneYear -> 365
        Period.TwoYears -> 730
        Period.FiveYears -> 1825
        Period.TenYears -> 3650
        Period.YearToDate -> 365  // 근사값
        Period.Max -> Int.MAX_VALUE
    }

    when {
        interval.isIntraday() && interval.minutes < 60 && daysInPeriod > 7 -> {
            throw InvalidPeriodIntervalException(
                "Intraday intervals (< 1h) can only query up to 7 days. " +
                "Requested: period=$period, interval=$interval"
            )
        }
        interval.minutes == 60 && daysInPeriod > 60 -> {
            throw InvalidPeriodIntervalException(
                "Hourly interval can only query up to 60 days. " +
                "Requested: period=$period, interval=$interval"
            )
        }
    }
}
```

### 7.4 에러 처리 전략

**검증 에러 (Client-side):**
- 즉시 throw
- Retry 없음
- 사용자에게 명확한 에러 메시지 제공

**API 에러 (Server-side):**
- Retry 정책 적용
- Exponential Backoff
- 최대 3회 재시도

**파싱 에러:**
- 응답 로깅 (500자 제한)
- DATA_PARSING_ERROR throw
- Retry 없음

**불완전 데이터:**
- 필수 필드 누락 시 IncompletePriceDataException
- 선택 필드 누락 시 null로 처리

---

## 8. 캐싱 전략

### 8.1 캐시 정책

| 데이터 타입 | Cache Key | TTL | Storage |
|-----------|----------|-----|---------|
| 현재 가격 | `price:{symbol}` | 60초 | ConcurrentHashMap |
| 히스토리 | 캐시 안 함 | - | - |

**캐시 TTL 차등 적용:**
- **현재 가격**: 60초 (1분)
  - 이유: 가격은 실시간 변동, 짧은 TTL 필요
  - 사용 사례: 대시보드, 워치리스트

- **히스토리**: 캐시 안 함
  - 이유: 조회 조건(period, interval)이 다양하여 캐시 키 관리 복잡
  - ChartService가 별도로 캐싱 처리 가능

**캐시 무효화:**
- 자동: TTL 만료 시
- 자동: 서비스 재시작 시
- 수동: API 미제공

**캐시 전략:**
- **Cache-Aside Pattern**
  1. 캐시 조회
  2. Cache Hit && Not Expired → 반환
  3. Cache Miss || Expired → API 호출
  4. 캐시 저장
  5. 데이터 반환

### 8.2 캐시 크기 제한

**제한 있음 (LRU):**
- 최대 1000개 심볼 캐시
- LRU (Least Recently Used) 정책
- 메모리 효율성 고려

**구현:**

```kotlin
private val priceCache = object : LinkedHashMap<String, CachedPriceData>(
    100,      // 초기 용량
    0.75f,    // Load factor
    true      // Access order (LRU)
) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedPriceData>?): Boolean {
        return size > 1000
    }
}
```

### 8.3 캐시 동시성 제어

**ConcurrentHashMap 사용:**
- Thread-safe
- 동시 읽기/쓰기 지원
- Lock contention 최소화

**캐시 검증:**

```kotlin
private fun getCachedPrice(symbol: String): PriceData? {
    val cached = priceCache[symbol] ?: return null
    val currentTime = System.currentTimeMillis()

    return if (cached.isExpired(currentTime)) {
        priceCache.remove(symbol)
        null
    } else {
        cached.data
    }
}

private fun setCachedPrice(symbol: String, data: PriceData) {
    val cached = CachedPriceData(
        data = data,
        cachedAt = System.currentTimeMillis(),
        ttlMillis = CURRENT_PRICE_TTL_MILLIS
    )
    priceCache[symbol] = cached
}
```

---

## 9. 테스트 전략

### 9.1 Unit Test (Fast, Isolated)

**테스트 대상:**
- 데이터 변환 로직
- 가격 파싱 로직
- 에러 처리 로직
- Period/Interval 검증

**테스트 방법:**
- Fake 객체 사용 (FakeHttpClient, FakeRateLimiter, FakeChartService)
- JSON 픽스쳐 사용 (src/test/resources/fixtures/price/)
- 외부 API 호출 없음

**테스트 케이스:**

| 테스트 | 입력 | 예상 출력 |
|--------|------|----------|
| getCurrentPrice_success | "AAPL" | PriceData (정상) |
| getCurrentPrice_notFound | "INVALID" | PRICE_DATA_NOT_FOUND |
| getCurrentPrice_cached | "AAPL" (2회 호출) | 두 번째 호출은 캐시 사용 |
| getCurrentPrice_cacheExpired | "AAPL" (TTL 초과 후) | API 재호출 |
| getPriceHistory_success | "AAPL", 1y, 1d | List<OHLCV> (정상) |
| getPriceHistory_invalidInterval | "AAPL", 1y, 1m | InvalidPeriodIntervalException |
| parsePriceData_mergeModules | price + summaryDetail | 모든 필드 병합 |
| parsePriceData_nullHandling | price (일부 null) | null 안전 처리 |

**픽스쳐 파일:**

```
src/test/resources/fixtures/price/
├── quoteSummary/
│   ├── AAPL_success.json
│   ├── SPY_etf.json
│   ├── ^GSPC_index.json
│   └── INVALID_notfound.json
├── chart/
│   ├── AAPL_1y_1d.json
│   ├── AAPL_5d_1m.json
│   └── error_invalidSymbol.json
```

### 9.2 Live Test (Slow, Real API)

**테스트 대상:**
- 실제 API 통합
- 인증 흐름
- 응답 파싱

**테스트 방법:**
- 실제 Yahoo Finance API 호출
- 응답 자동 녹화 (ResponseRecorder)
- 녹화된 데이터 → Unit Test 픽스쳐

**테스트 픽스쳐:**

| Symbol | 타입 | 설명 |
|--------|------|------|
| AAPL | EQUITY | 주식 (Apple) |
| SPY | ETF | S&P 500 ETF |
| ^GSPC | INDEX | S&P 500 지수 |
| BTC-USD | CRYPTOCURRENCY | 비트코인 |
| VTSAX | MUTUALFUND | 뮤추얼펀드 |

**Live Test 예시:**

```kotlin
@Test
@Tag("live")
fun `getCurrentPrice should fetch real data from Yahoo Finance`() = runBlocking {
    // Given
    val service = createRealPriceService()

    // When
    val priceData = service.getCurrentPrice("AAPL")

    // Then
    assertThat(priceData.symbol).isEqualTo("AAPL")
    assertThat(priceData.regularMarketPrice).isNotNull()
    assertThat(priceData.currency).isEqualTo("USD")
    assertThat(priceData.fiftyTwoWeekHigh).isNotNull()
    assertThat(priceData.fiftyTwoWeekLow).isNotNull()

    // 응답 녹화
    responseRecorder.save("AAPL_live", priceData)
}

@Test
@Tag("live")
fun `getPriceHistory should fetch OHLCV data`() = runBlocking {
    // Given
    val service = createRealPriceService()

    // When
    val history = service.getPriceHistory("AAPL", Period.OneMonth, Interval.OneDay)

    // Then
    assertThat(history).isNotEmpty()
    assertThat(history.first().timestamp).isLessThan(history.last().timestamp)
    history.forEach { ohlcv ->
        assertThat(ohlcv.high).isGreaterThanOrEqualTo(ohlcv.low)
        assertThat(ohlcv.high).isGreaterThanOrEqualTo(ohlcv.open)
        assertThat(ohlcv.high).isGreaterThanOrEqualTo(ohlcv.close)
    }
}
```

### 9.3 Integration Test

**테스트 시나리오:**
- UFCClient → PriceApi → YahooSource → API
- 캐시 통합 테스트 (Cache Hit/Miss)
- 에러 전파 테스트
- ChartService 위임 테스트

**캐시 통합 테스트:**

```kotlin
@Test
fun `getCurrentPrice should use cache on second call`() = runBlocking {
    // Given
    val httpClient = mockHttpClient()
    val service = YahooPriceService(httpClient, rateLimiter, authResult, chartService)

    // When
    val first = service.getCurrentPrice("AAPL")
    val second = service.getCurrentPrice("AAPL")

    // Then
    assertThat(first).isEqualTo(second)
    verify(httpClient, times(1)).get(any())  // API 호출 1회만
}

@Test
fun `getCurrentPrice should refresh cache after TTL expiry`() = runBlocking {
    // Given
    val service = YahooPriceService(httpClient, rateLimiter, authResult, chartService)

    // When
    val first = service.getCurrentPrice("AAPL")
    delay(CURRENT_PRICE_TTL_MILLIS + 1000)  // TTL 초과 대기
    val second = service.getCurrentPrice("AAPL")

    // Then
    verify(httpClient, times(2)).get(any())  // API 호출 2회
}
```

### 9.4 Performance Test

**테스트 대상:**
- 다중 심볼 조회 성능
- 캐시 효율성
- Rate Limiter 동작

**Performance Test 예시:**

```kotlin
@Test
fun `getCurrentPrice for multiple symbols should complete within timeout`() = runBlocking {
    // Given
    val symbols = listOf("AAPL", "GOOGL", "MSFT", "AMZN", "TSLA")
    val service = createRealPriceService()

    // When
    val startTime = System.currentTimeMillis()
    val results = service.getCurrentPrice(symbols)
    val duration = System.currentTimeMillis() - startTime

    // Then
    assertThat(results).hasSize(5)
    assertThat(duration).isLessThan(10_000)  // 10초 이내
}
```

---

## 10. 파일 구조

### 10.1 디렉토리 레이아웃

```
src/main/kotlin/com/ulalax/ufc/
├── api/                              # Public API
│   └── PriceApi.kt                   # Price 도메인 인터페이스
│
├── internal/price/                   # Internal Implementation
│   ├── YahooPriceService.kt          # Price 서비스 구현체
│   ├── PriceValidator.kt             # 검증 로직 (선택적)
│   ├── PriceConverter.kt             # 변환 로직 (선택적)
│   └── PriceCache.kt                 # 캐시 관리 (선택적)
│
├── model/price/                      # Domain Models (Public)
│   ├── PriceData.kt                  # 현재 가격 모델
│   ├── OHLCV.kt                      # 히스토리 데이터 포인트
│   └── ChartMetadata.kt              # 차트 메타데이터
│
├── model/common/                     # Common Models
│   ├── Period.kt                     # 조회 기간 열거형
│   └── Interval.kt                   # 데이터 간격 열거형
│
├── internal/yahoo/response/          # Response Models (Internal)
│   ├── PriceResponse.kt              # quoteSummary 응답
│   ├── QuoteSummaryResult.kt
│   ├── PriceModuleRaw.kt
│   ├── SummaryDetailRaw.kt
│   ├── ChartResponse.kt              # chart 응답 (기존 재사용)
│   └── ValueFormat.kt                # Yahoo 표준 포맷
│
├── domain/chart/                     # Chart Service (기존)
│   ├── ChartService.kt               # ChartService 인터페이스
│   ├── YahooChartService.kt          # 구현체
│   ├── ChartDataResponse.kt          # 응답 모델
│   └── OHLCVData.kt                  # OHLCV 데이터
│
└── exception/
    ├── UFCException.kt
    ├── ErrorCode.kt
    └── PriceExceptions.kt            # 커스텀 예외
```

### 10.2 파일별 책임

| 파일 | 책임 | Public/Internal |
|------|------|-----------------|
| PriceApi.kt | 도메인 인터페이스 정의 | Public |
| YahooPriceService.kt | 비즈니스 로직 구현 | Internal |
| PriceData.kt | 현재 가격 Domain 모델 | Public |
| OHLCV.kt | 히스토리 Domain 모델 | Public |
| PriceResponse.kt | quoteSummary API 응답 | Internal |
| ChartResponse.kt | chart API 응답 (재사용) | Internal |

### 10.3 ChartService 통합

**기존 ChartService 재사용:**
- `domain/chart/` 디렉토리 유지
- PriceApi는 ChartService를 의존성으로 주입
- 히스토리 조회 시 ChartService 위임

**통합 방법:**

```kotlin
internal class YahooPriceService(
    private val httpClient: HttpClient,
    private val rateLimiter: RateLimiter,
    private val authResult: AuthResult,
    private val chartService: ChartService  // ChartService 주입
) : PriceApi {

    override suspend fun getPriceHistory(
        symbol: String,
        period: Period,
        interval: Interval
    ): List<OHLCV> {
        // ChartService 위임
        val ohlcvDataList = chartService.getChartData(symbol, interval, period)

        // OHLCVData -> OHLCV 변환
        return ohlcvDataList.map { data ->
            OHLCV(
                timestamp = data.timestamp,
                open = data.open,
                high = data.high,
                low = data.low,
                close = data.close,
                adjClose = data.adjClose,
                volume = data.volume
            )
        }
    }
}
```

---

## 11. 향후 확장

### 11.1 Phase 2: 실시간 가격 스트리밍

#### 11.1.1 개요

WebSocket을 통한 실시간 가격 업데이트 제공

#### 11.1.2 메서드 명세

```kotlin
interface PriceStreamApi {
    /**
     * 실시간 가격 스트림 시작
     *
     * @param symbol 심볼
     * @return Flow<PriceData> 가격 업데이트 스트림
     */
    fun streamPrice(symbol: String): Flow<PriceData>

    /**
     * 다중 심볼 실시간 스트림
     *
     * @param symbols 심볼 목록
     * @return Flow<Map<String, PriceData>>
     */
    fun streamPrices(symbols: List<String>): Flow<Map<String, PriceData>>
}
```

#### 11.1.3 구현 전략

**WebSocket 기반:**
- Yahoo Finance WebSocket API 활용
- Kotlin Flow로 스트림 제공
- 자동 재연결 로직

**백오프 전략:**
- 연결 실패 시 Exponential Backoff
- 최대 재시도 횟수 제한

### 11.2 Phase 3: 기술 지표 계산

#### 11.2.1 개요

가격 히스토리 기반 기술 지표 계산 기능

#### 11.2.2 지원 지표

| 지표 | 설명 |
|------|------|
| SMA | Simple Moving Average (단순이동평균) |
| EMA | Exponential Moving Average (지수이동평균) |
| RSI | Relative Strength Index (상대강도지수) |
| MACD | Moving Average Convergence Divergence |
| Bollinger Bands | 볼린저 밴드 |

#### 11.2.3 메서드 명세

```kotlin
interface TechnicalIndicatorApi {
    /**
     * 단순이동평균 계산
     *
     * @param symbol 심볼
     * @param period 조회 기간
     * @param window 이동평균 윈도우 (일수)
     * @return List<Pair<Long, Double>> (timestamp, sma)
     */
    suspend fun calculateSMA(
        symbol: String,
        period: Period,
        window: Int
    ): List<Pair<Long, Double>>

    /**
     * RSI 계산
     *
     * @param symbol 심볼
     * @param period 조회 기간
     * @param window RSI 윈도우 (기본값: 14)
     * @return List<Pair<Long, Double>> (timestamp, rsi)
     */
    suspend fun calculateRSI(
        symbol: String,
        period: Period,
        window: Int = 14
    ): List<Pair<Long, Double>>
}
```

### 11.3 Phase 4: 가격 알림 (Price Alerts)

#### 11.3.1 개요

특정 가격 조건 도달 시 알림 기능

#### 11.3.2 메서드 명세

```kotlin
data class PriceAlert(
    val id: String,
    val symbol: String,
    val condition: AlertCondition,
    val targetPrice: Double,
    val createdAt: Long,
    val triggeredAt: Long? = null
)

enum class AlertCondition {
    ABOVE,        // 가격이 목표가 이상
    BELOW,        // 가격이 목표가 이하
    CHANGE_UP,    // 가격 상승률 (%)
    CHANGE_DOWN   // 가격 하락률 (%)
}

interface PriceAlertApi {
    /**
     * 가격 알림 생성
     */
    suspend fun createAlert(
        symbol: String,
        condition: AlertCondition,
        targetPrice: Double
    ): PriceAlert

    /**
     * 활성 알림 조회
     */
    suspend fun getActiveAlerts(): List<PriceAlert>

    /**
     * 알림 삭제
     */
    suspend fun deleteAlert(alertId: String)
}
```

#### 11.3.3 구현 전략

**백그라운드 모니터링:**
- 주기적으로 가격 조회 (예: 1분마다)
- 조건 충족 시 콜백 실행

**이벤트 기반:**
- 가격 스트림 구독
- 조건 충족 시 즉시 알림

---

## 참고 자료

- **Python yfinance**: https://github.com/ranaroussi/yfinance
- **UFC Architecture**: `/plan/1차개발/01-architecture-design.md`
- **Error Handling**: `/plan/1차개발/02-error-handling.md`
- **Yahoo Finance Price API**: `/plan/1차개발/05-yahoo-finance-price.md`
- **UFC.FUNDS 기술명세서**: `/plan/2차개발/ufc.funds-기술명세서.md`

---

**최종 수정일**: 2025-12-03
**문서 버전**: 2.0.0
**문서 유형**: 설계 명세서 (코드 구현 제외)
