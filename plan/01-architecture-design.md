# UFC Architecture Design - Multi-Source 아키텍처 설계

## 문서 정보
- **버전**: 2.0.0
- **최종 수정일**: 2025-12-01
- **작성자**: Claude Code (Software Architect)
- **문서 상태**: Active

---

## 목차
1. [아키텍처 개요](#1-아키텍처-개요)
2. [레이어 구조](#2-레이어-구조)
3. [Multi-Source 패턴](#3-multi-source-패턴)
4. [DataSource 인터페이스](#4-datasource-인터페이스)
5. [UFCClient Facade](#5-ufcclient-facade)
6. [HTTP 클라이언트 전략](#6-http-클라이언트-전략)
7. [데이터 모델 설계](#7-데이터-모델-설계)
8. [의존성 주입](#8-의존성-주입)

---

## 1. 아키텍처 개요

### 1.1 아키텍처 원칙

UFC는 다음과 같은 아키텍처 원칙을 따릅니다:

1. **Multi-Source Architecture**
   - 여러 데이터 소스를 통합하는 Facade 패턴
   - 각 소스는 독립적인 모듈로 분리
   - 공통 인프라 공유

2. **Layer Separation**
   - Client Layer (Facade)
   - Source Layer (Data Sources)
   - Infrastructure Layer (HTTP, Serialization, Cache)
   - Model Layer (Data Models)

3. **ErrorCode-Based Exception Handling**
   - 단일 예외 클래스 (UFCException)
   - ErrorCode enum으로 세밀한 분류
   - Metadata로 컨텍스트 정보 전달

4. **Kotlin Coroutines**
   - 모든 I/O 작업은 suspend 함수
   - 병렬 처리 지원
   - Structured Concurrency

5. **Type Safety**
   - Kotlin의 강력한 타입 시스템
   - Null Safety
   - Sealed Classes for ADT

### 1.2 전체 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────┐
│                       Application                           │
│                    (User's Application)                      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                  Client Layer (Facade)                      │
│                                                              │
│  ┌────────────────────────────────────────────────────┐     │
│  │              UFCClient                              │     │
│  │  - val yahoo: YahooFinanceSource                    │     │
│  │  - val fred: FREDSource                             │     │
│  │  - fun close()                                      │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    Source Layer                             │
│                                                              │
│  ┌──────────────────────┐  ┌─────────────────────────┐      │
│  │ YahooFinanceSource   │  │    FREDSource           │      │
│  ├──────────────────────┤  ├─────────────────────────┤      │
│  │ - ticker()           │  │ - getSeries()           │      │
│  │ - etf()              │  │ - getSeriesInfo()       │      │
│  │ - search()           │  │ - search()              │      │
│  │ - screener()         │  │ - searchByCategory()    │      │
│  │                      │  │ - getCategory()         │      │
│  │ Internal:            │  │                         │      │
│  │ - Authenticator      │  │ Internal:               │      │
│  │ - ChartAPI           │  │ - FREDClient (HTTP)     │      │
│  │ - QuoteSummaryAPI    │  │                         │      │
│  └──────────────────────┘  └─────────────────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              Infrastructure Layer                           │
│                                                              │
│  ┌─────────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │  HTTP Clients   │  │ Serialization│  │    Cache     │   │
│  │  (Ktor)         │  │ (kotlinx)    │  │    (LRU)     │   │
│  └─────────────────┘  └──────────────┘  └──────────────┘   │
│                                                              │
│  ┌─────────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │  Retry Logic    │  │ Rate Limiter │  │  Logging     │   │
│  └─────────────────┘  └──────────────┘  └──────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                      Model Layer                            │
│                                                              │
│  ┌──────────────────────┐  ┌─────────────────────────┐      │
│  │   Yahoo Models       │  │     FRED Models         │      │
│  ├──────────────────────┤  ├─────────────────────────┤      │
│  │ - ChartResponse      │  │ - Series                │      │
│  │ - QuoteSummary       │  │ - SeriesInfo            │      │
│  │ - ETFHoldings        │  │ - Observation           │      │
│  │ - SearchResult       │  │ - Category              │      │
│  └──────────────────────┘  └─────────────────────────┘      │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │            Common Models                            │    │
│  │  - ErrorCode (enum)                                 │    │
│  │  - UFCException                                     │    │
│  │  - Period, Interval, DataFrequency                  │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 레이어 구조

### 2.1 Client Layer (Facade)

**역할:**
- 사용자에게 통합된 API 제공
- 여러 DataSource를 조합
- 리소스 관리 (초기화, 종료)

**구성:**
```kotlin
class UFCClient(
    val yahoo: YahooFinanceSource,
    val fred: FREDSource,
    private val config: UFCClientConfig = UFCClientConfig()
) : AutoCloseable {

    companion object {
        suspend fun create(
            config: UFCClientConfig = UFCClientConfig()
        ): UFCClient
    }

    override fun close() {
        yahoo.close()
        fred.close()
    }
}
```

### 2.2 Source Layer (Data Sources)

**역할:**
- 특정 데이터 소스에 대한 추상화
- 도메인 로직 캡슐화
- 내부 구현 숨김 (Authenticator, HTTP Client 등)

**구성:**
- `YahooFinanceSource`: Yahoo Finance API 추상화
- `FREDSource`: FRED API 추상화

### 2.3 Infrastructure Layer

**역할:**
- 공통 인프라 제공
- HTTP 통신, 직렬화, 캐싱, 재시도 등
- 여러 Source에서 공유

**구성:**
- HTTP Client (Ktor)
- Serialization (kotlinx.serialization)
- Cache (LRU)
- Retry Logic
- Rate Limiter
- Logging (SLF4J)

### 2.4 Model Layer

**역할:**
- 데이터 모델 정의
- Source별 모델 분리
- 공통 모델 정의 (ErrorCode, Period 등)

**구성:**
- Yahoo Models: `model/yahoo/`
- FRED Models: `model/fred/`
- Common Models: `model/common/`

---

## 3. Multi-Source 패턴

### 3.1 설계 목표

KFC(Korea Financial Client)의 Multi-Source 패턴을 참고하여 설계합니다:

**KFC 구조:**
```kotlin
class KfcClient(
    val krx: KrxEtfApi,
    val naver: NaverEtfApi,
    val opendart: OpenDartApi?
)
```

**UFC 구조:**
```kotlin
class UFCClient(
    val yahoo: YahooFinanceSource,
    val fred: FREDSource
)
```

### 3.2 장점

1. **명확한 책임 분리**
   - 각 소스는 독립적인 모듈
   - 변경 사항이 다른 소스에 영향 없음

2. **확장 용이성**
   - 새로운 데이터 소스 추가 쉬움
   - 기존 코드 수정 최소화

3. **사용 편의성**
   - `ufc.yahoo.ticker("AAPL")`
   - `ufc.fred.getSeries("GDPC1")`

4. **독립적인 라이프사이클**
   - 각 소스는 독립적으로 초기화/종료
   - 선택적 소스 사용 가능 (예: FRED API Key 없을 때)

### 3.3 패키지 구조

```
com.ulalax.ufc/
├── client/
│   ├── UFCClient.kt
│   └── UFCClientConfig.kt
│
├── source/
│   ├── DataSource.kt                 # 공통 인터페이스
│   │
│   ├── yahoo/
│   │   ├── YahooFinanceSource.kt     # Public API
│   │   ├── internal/
│   │   │   ├── YahooFinanceClient.kt (HTTP)
│   │   │   ├── Authenticator.kt
│   │   │   ├── ChartAPI.kt
│   │   │   └── QuoteSummaryAPI.kt
│   │   ├── Ticker.kt
│   │   ├── ETF.kt
│   │   └── Search.kt
│   │
│   └── fred/
│       ├── FREDSource.kt             # Public API
│       └── internal/
│           └── FREDClient.kt (HTTP)
│
├── model/
│   ├── common/
│   │   ├── Period.kt
│   │   ├── Interval.kt
│   │   └── DataFrequency.kt
│   ├── yahoo/
│   │   ├── chart/
│   │   ├── etf/
│   │   └── quote/
│   └── fred/
│       ├── Series.kt
│       └── SeriesInfo.kt
│
├── exception/
│   ├── UFCException.kt
│   └── ErrorCode.kt
│
├── infrastructure/
│   ├── http/
│   │   ├── HttpClientFactory.kt
│   │   └── RetryPolicy.kt
│   ├── serialization/
│   ├── cache/
│   │   └── LRUCache.kt
│   └── ratelimit/
│       └── RateLimiter.kt
│
└── utils/
```

---

## 4. DataSource 인터페이스

### 4.1 공통 인터페이스

```kotlin
/**
 * 모든 데이터 소스의 기본 인터페이스
 */
interface DataSource {
    /**
     * 데이터 소스 이름
     */
    val name: String

    /**
     * 데이터 소스 초기화
     * 인증, 연결 풀 생성 등
     */
    suspend fun initialize()

    /**
     * 데이터 소스 종료
     * 리소스 정리
     */
    fun close()
}
```

### 4.2 YahooFinanceSource

```kotlin
/**
 * Yahoo Finance 데이터 소스
 */
interface YahooFinanceSource : DataSource {

    /**
     * 주식/ETF Ticker 객체 생성
     *
     * @param symbol 종목 심볼 (예: "AAPL", "SPY")
     * @return Ticker 객체
     */
    fun ticker(symbol: String): Ticker

    /**
     * ETF 전용 객체 생성
     *
     * @param symbol ETF 심볼 (예: "SPY", "QQQ")
     * @return ETF 객체
     */
    fun etf(symbol: String): ETF

    /**
     * 종목 검색
     *
     * @param query 검색어
     * @return 검색 결과
     */
    suspend fun search(query: String): List<SearchResult>

    /**
     * 스크리너 (필터링)
     *
     * @param screener 스크리너 조건
     * @return 스크리너 결과
     */
    suspend fun screener(screener: ScreenerQuery): List<ScreenerResult>
}
```

### 4.3 FREDSource

```kotlin
/**
 * FRED (Federal Reserve Economic Data) 데이터 소스
 */
interface FREDSource : DataSource {

    /**
     * 시계열 데이터 조회
     *
     * @param seriesId 시리즈 ID (예: "GDPC1", "UNRATE")
     * @param observationStart 관찰 시작일
     * @param observationEnd 관찰 종료일
     * @param frequency 데이터 주기 (선택)
     * @param aggregationMethod 집계 방법 (선택)
     * @param units 단위 변환 (선택)
     * @return 시계열 데이터
     */
    suspend fun getSeries(
        seriesId: String,
        observationStart: LocalDate? = null,
        observationEnd: LocalDate? = null,
        frequency: DataFrequency? = null,
        aggregationMethod: AggregationMethod? = null,
        units: Units? = null
    ): Series

    /**
     * 시리즈 메타데이터 조회
     *
     * @param seriesId 시리즈 ID
     * @return 시리즈 정보
     */
    suspend fun getSeriesInfo(seriesId: String): SeriesInfo

    /**
     * 첫 발표 데이터 조회 (개정 제외)
     *
     * @param seriesId 시리즈 ID
     * @return 첫 발표 데이터
     */
    suspend fun getSeriesFirstRelease(seriesId: String): Series

    /**
     * 모든 개정 데이터 조회
     *
     * @param seriesId 시리즈 ID
     * @param realtimeStart 실시간 시작일
     * @param realtimeEnd 실시간 종료일
     * @return 모든 개정 데이터
     */
    suspend fun getSeriesAllReleases(
        seriesId: String,
        realtimeStart: LocalDate? = null,
        realtimeEnd: LocalDate? = null
    ): List<VintageObservation>

    /**
     * 특정 시점 데이터 조회
     *
     * @param seriesId 시리즈 ID
     * @param asOfDate 기준 날짜
     * @return 해당 시점 데이터
     */
    suspend fun getSeriesAsOfDate(
        seriesId: String,
        asOfDate: LocalDate
    ): Series

    /**
     * Vintage 날짜 목록 조회
     *
     * @param seriesId 시리즈 ID
     * @return Vintage 날짜 리스트
     */
    suspend fun getSeriesVintageDates(seriesId: String): List<LocalDate>

    /**
     * 전문 검색
     *
     * @param text 검색 텍스트
     * @param limit 결과 수 제한 (0 = 무제한)
     * @param orderBy 정렬 기준
     * @param sortOrder 정렬 순서
     * @param filter 필터 조건
     * @return 검색 결과 리스트
     */
    suspend fun search(
        text: String,
        limit: Int = 1000,
        orderBy: SearchOrderBy? = null,
        sortOrder: SortOrder? = null,
        filter: SearchFilter? = null
    ): List<SeriesInfo>

    /**
     * 카테고리별 검색
     *
     * @param categoryId 카테고리 ID
     * @param limit 결과 수 제한
     * @return 시리즈 리스트
     */
    suspend fun searchByCategory(
        categoryId: Int,
        limit: Int = 0
    ): List<SeriesInfo>

    /**
     * Release별 검색
     *
     * @param releaseId Release ID
     * @param limit 결과 수 제한
     * @return 시리즈 리스트
     */
    suspend fun searchByRelease(
        releaseId: Int,
        limit: Int = 0
    ): List<SeriesInfo>

    /**
     * 카테고리 정보 조회
     *
     * @param categoryId 카테고리 ID
     * @return 카테고리 정보
     */
    suspend fun getCategory(categoryId: Int): Category
}
```

---

## 5. UFCClient Facade

### 5.1 UFCClient 설계

```kotlin
/**
 * UFC (US Free Financial Data Collector) 통합 클라이언트
 *
 * Multi-Source 아키텍처를 사용하여 Yahoo Finance와 FRED 데이터를 통합 제공합니다.
 *
 * ## 사용 예시
 *
 * ```kotlin
 * // 기본 생성
 * val ufc = UFCClient.create()
 *
 * // FRED API Key 포함
 * val ufc = UFCClient.create(
 *     config = UFCClientConfig(
 *         fredApiKey = "your_api_key"
 *     )
 * )
 *
 * // Yahoo Finance 사용
 * val spy = ufc.yahoo.etf("SPY")
 * val holdings = spy.getTopHoldings()
 *
 * // FRED 사용
 * val gdp = ufc.fred.getSeries("GDPC1")
 *
 * // 종료
 * ufc.close()
 * ```
 *
 * @property yahoo Yahoo Finance 데이터 소스
 * @property fred FRED 데이터 소스
 */
class UFCClient private constructor(
    val yahoo: YahooFinanceSource,
    val fred: FREDSource,
    private val config: UFCClientConfig
) : AutoCloseable {

    companion object {
        /**
         * UFCClient 인스턴스 생성
         *
         * @param config 클라이언트 설정
         * @return UFCClient 인스턴스
         */
        suspend fun create(
            config: UFCClientConfig = UFCClientConfig()
        ): UFCClient {
            // Yahoo Finance Source 생성
            val yahooSource = YahooFinanceSourceImpl(
                config = config.yahooConfig
            )

            // FRED Source 생성
            val fredSource = FREDSourceImpl(
                apiKey = config.fredApiKey,
                config = config.fredConfig
            )

            // 초기화
            yahooSource.initialize()
            fredSource.initialize()

            return UFCClient(
                yahoo = yahooSource,
                fred = fredSource,
                config = config
            )
        }
    }

    /**
     * 모든 리소스 정리
     */
    override fun close() {
        yahoo.close()
        fred.close()
    }
}
```

### 5.2 UFCClientConfig

```kotlin
/**
 * UFC 클라이언트 설정
 *
 * @property fredApiKey FRED API Key (필수)
 * @property yahooConfig Yahoo Finance 설정
 * @property fredConfig FRED 설정
 * @property httpConfig HTTP 클라이언트 설정
 * @property cacheConfig 캐시 설정
 */
data class UFCClientConfig(
    // FRED API Key (필수)
    val fredApiKey: String? = null,

    // Yahoo Finance 설정
    val yahooConfig: YahooConfig = YahooConfig(),

    // FRED 설정
    val fredConfig: FREDConfig = FREDConfig(),

    // HTTP 설정
    val httpConfig: HttpConfig = HttpConfig(),

    // 캐시 설정
    val cacheConfig: CacheConfig = CacheConfig()
)

/**
 * Yahoo Finance 설정
 */
data class YahooConfig(
    // Cookie/Crumb 자동 갱신 여부
    val autoRefreshAuth: Boolean = true,

    // 인증 캐시 TTL (초)
    val authCacheTTL: Long = 3600,

    // User-Agent
    val userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
)

/**
 * FRED 설정
 */
data class FREDConfig(
    // API Key 파일 경로 (선택)
    val apiKeyFile: String? = null,

    // 환경 변수 이름
    val apiKeyEnvVar: String = "FRED_API_KEY"
)

/**
 * HTTP 클라이언트 설정
 */
data class HttpConfig(
    // 연결 타임아웃 (밀리초)
    val connectTimeout: Long = 30_000,

    // 읽기 타임아웃 (밀리초)
    val readTimeout: Long = 30_000,

    // 재시도 횟수
    val maxRetries: Int = 3,

    // Rate Limit (requests/second)
    val rateLimit: Int = 50
)

/**
 * 캐시 설정
 */
data class CacheConfig(
    // 캐시 사용 여부
    val enabled: Boolean = true,

    // 캐시 크기
    val maxSize: Int = 1000,

    // 캐시 TTL (초)
    val ttl: Long = 300
)
```

---

## 6. HTTP 클라이언트 전략

### 6.1 Ktor 클라이언트 사용

**선택 이유:**
- Kotlin Native 지원
- Coroutines 기반
- 멀티 플랫폼 지원
- 풍부한 플러그인 생태계

### 6.2 공통 HTTP 클라이언트

```kotlin
/**
 * HTTP 클라이언트 팩토리
 */
object HttpClientFactory {

    fun create(config: HttpConfig): HttpClient {
        return HttpClient(CIO) {
            // Timeout 설정
            install(HttpTimeout) {
                connectTimeoutMillis = config.connectTimeout
                requestTimeoutMillis = config.readTimeout
            }

            // JSON 직렬화
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                })
            }

            // 로깅
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }

            // 재시도
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = config.maxRetries)
                exponentialDelay()
            }

            // User-Agent
            defaultRequest {
                header(HttpHeaders.UserAgent, config.userAgent)
            }
        }
    }
}
```

### 6.3 Yahoo Finance 전용 클라이언트

```kotlin
/**
 * Yahoo Finance HTTP 클라이언트
 */
internal class YahooFinanceClient(
    private val httpClient: HttpClient,
    private val authenticator: Authenticator
) {

    /**
     * Chart API 요청
     */
    suspend fun fetchChart(
        symbol: String,
        params: ChartParams
    ): ChartResponse {
        val url = buildChartUrl(symbol, params)

        return withRetry {
            httpClient.get(url) {
                // Cookie 추가
                authenticator.applyCookies(this)
            }.body()
        }
    }

    /**
     * QuoteSummary API 요청
     */
    suspend fun fetchQuoteSummary(
        symbol: String,
        modules: List<String>
    ): QuoteSummaryResponse {
        val url = buildQuoteSummaryUrl(symbol, modules)

        return withRetry {
            httpClient.get(url) {
                authenticator.applyCookies(this)
                // Crumb 추가
                parameter("crumb", authenticator.getCrumb())
            }.body()
        }
    }

    private suspend fun <T> withRetry(
        maxRetries: Int = 3,
        block: suspend () -> T
    ): T {
        // 재시도 로직
    }
}
```

### 6.4 FRED 전용 클라이언트

```kotlin
/**
 * FRED HTTP 클라이언트
 */
internal class FREDClient(
    private val httpClient: HttpClient,
    private val apiKey: String
) {

    companion object {
        private const val BASE_URL = "https://api.stlouisfed.org/fred"
    }

    /**
     * Series 데이터 요청
     */
    suspend fun fetchSeries(
        seriesId: String,
        params: SeriesParams
    ): SeriesResponse {
        val url = "$BASE_URL/series/observations"

        return httpClient.get(url) {
            parameter("series_id", seriesId)
            parameter("api_key", apiKey)
            parameter("file_type", "json")

            params.observationStart?.let {
                parameter("observation_start", it.toString())
            }
            params.observationEnd?.let {
                parameter("observation_end", it.toString())
            }
            params.frequency?.let {
                parameter("frequency", it.value)
            }
        }.body()
    }

    /**
     * Search 요청
     */
    suspend fun search(
        text: String,
        params: SearchParams
    ): SearchResponse {
        val url = "$BASE_URL/series/search"

        return httpClient.get(url) {
            parameter("search_text", text)
            parameter("api_key", apiKey)
            parameter("file_type", "json")
            parameter("limit", params.limit)

            params.orderBy?.let {
                parameter("order_by", it.value)
            }
            params.sortOrder?.let {
                parameter("sort_order", it.value)
            }
        }.body()
    }
}
```

---

## 7. 데이터 모델 설계

### 7.1 공통 모델

```kotlin
/**
 * 기간 (Period)
 */
enum class Period(val value: String) {
    OneDay("1d"),
    FiveDays("5d"),
    OneMonth("1mo"),
    ThreeMonths("3mo"),
    SixMonths("6mo"),
    OneYear("1y"),
    TwoYears("2y"),
    FiveYears("5y"),
    TenYears("10y"),
    YearToDate("ytd"),
    Max("max")
}

/**
 * 간격 (Interval)
 */
enum class Interval(val value: String) {
    OneMinute("1m"),
    TwoMinutes("2m"),
    FiveMinutes("5m"),
    FifteenMinutes("15m"),
    ThirtyMinutes("30m"),
    OneHour("1h"),
    OneDay("1d"),
    FiveDays("5d"),
    OneWeek("1wk"),
    OneMonth("1mo"),
    ThreeMonths("3mo")
}

/**
 * 데이터 주기 (FRED)
 */
enum class DataFrequency(val value: String) {
    Daily("d"),
    Weekly("w"),
    Biweekly("bw"),
    Monthly("m"),
    Quarterly("q"),
    Semiannual("sa"),
    Annual("a")
}
```

### 7.2 Yahoo Finance 모델

```kotlin
/**
 * Chart Response (가격 데이터)
 */
@Serializable
data class ChartResponse(
    val chart: Chart
) {
    @Serializable
    data class Chart(
        val result: List<Result>?,
        val error: Error?
    )

    @Serializable
    data class Result(
        val meta: Meta,
        val timestamp: List<Long>,
        val indicators: Indicators
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
}

/**
 * QuoteSummary Response
 */
@Serializable
data class QuoteSummaryResponse(
    val quoteSummary: QuoteSummary
) {
    @Serializable
    data class QuoteSummary(
        val result: List<Result>?,
        val error: Error?
    )

    @Serializable
    data class Result(
        val quoteType: QuoteType? = null,
        val price: Price? = null,
        val summaryDetail: SummaryDetail? = null,
        val fundProfile: FundProfile? = null,
        val topHoldings: TopHoldings? = null,
        // ... 기타 모듈
    )
}
```

### 7.3 FRED 모델

```kotlin
/**
 * Series (시계열 데이터)
 */
data class Series(
    val id: String,
    val title: String,
    val observations: List<Observation>
)

/**
 * Observation (관찰 데이터)
 */
data class Observation(
    val date: LocalDate,
    val value: Double?
)

/**
 * SeriesInfo (시리즈 메타데이터)
 */
@Serializable
data class SeriesInfo(
    val id: String,
    val title: String,
    @SerialName("observation_start")
    val observationStart: String,
    @SerialName("observation_end")
    val observationEnd: String,
    val frequency: String,
    @SerialName("frequency_short")
    val frequencyShort: String,
    val units: String,
    @SerialName("units_short")
    val unitsShort: String,
    @SerialName("seasonal_adjustment")
    val seasonalAdjustment: String,
    @SerialName("seasonal_adjustment_short")
    val seasonalAdjustmentShort: String,
    @SerialName("last_updated")
    val lastUpdated: String,
    val popularity: Int,
    val notes: String?
)

/**
 * VintageObservation (개정 이력 데이터)
 */
data class VintageObservation(
    val date: LocalDate,
    val realtimeStart: LocalDate,
    val value: Double?
)
```

---

## 8. 의존성 주입

### 8.1 Constructor Injection

UFC는 Constructor Injection을 사용합니다:

```kotlin
// Source 구현체
internal class YahooFinanceSourceImpl(
    private val httpClient: HttpClient,
    private val authenticator: Authenticator,
    private val cache: Cache,
    private val config: YahooConfig
) : YahooFinanceSource {
    // ...
}

// Facade에서 생성
companion object {
    suspend fun create(config: UFCClientConfig): UFCClient {
        val httpClient = HttpClientFactory.create(config.httpConfig)
        val cache = LRUCache(config.cacheConfig)

        val authenticator = Authenticator(httpClient)
        authenticator.initialize()

        val yahooSource = YahooFinanceSourceImpl(
            httpClient = httpClient,
            authenticator = authenticator,
            cache = cache,
            config = config.yahooConfig
        )

        // ...
    }
}
```

### 8.2 의존성 그래프

```
UFCClient
├── YahooFinanceSource
│   ├── HttpClient (shared)
│   ├── Authenticator
│   ├── Cache (shared)
│   └── YahooConfig
│
└── FREDSource
    ├── HttpClient (shared)
    ├── Cache (shared)
    └── FREDConfig
```

---

## 9. 참고 자료

- **KFC (Korea Financial Client)**: Multi-Source 패턴 참조
- **Ktor Documentation**: https://ktor.io/docs/
- **kotlinx.serialization**: https://github.com/Kotlin/kotlinx.serialization
- **Clean Architecture**: Robert C. Martin

---

**다음 문서**: [02-error-handling.md](./02-error-handling.md)
