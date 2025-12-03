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
│  │  - val stock: StockApi                              │     │
│  │  - val etf: EtfApi                                  │     │
│  │  - val macro: MacroApi                              │     │
│  │  - val search: SearchApi                            │     │
│  │  - fun close()                                      │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    Domain API Layer                         │
│                                                              │
│  ┌──────────────────────┐  ┌─────────────────────────┐      │
│  │     StockApi         │  │       EtfApi            │      │
│  ├──────────────────────┤  ├─────────────────────────┤      │
│  │ - history()          │  │ - getHoldings()         │      │
│  │ - info()             │  │ - getFundProfile()      │      │
│  │ - financials()       │  │ - getFundPerformance()  │      │
│  │                      │  │ - history()             │      │
│  └──────────────────────┘  └─────────────────────────┘      │
│                                                              │
│  ┌──────────────────────┐  ┌─────────────────────────┐      │
│  │     MacroApi         │  │     SearchApi           │      │
│  ├──────────────────────┤  ├─────────────────────────┤      │
│  │ - getSeries()        │  │ - stocks()              │      │
│  │ - getSeriesInfo()    │  │ - economicData()        │      │
│  │ - search()           │  │                         │      │
│  │ - getCategory()      │  │                         │      │
│  └──────────────────────┘  └─────────────────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    Source Layer                             │
│                                                              │
│  ┌──────────────────────┐  ┌─────────────────────────┐      │
│  │ YahooFinanceSource   │  │    FREDSource           │      │
│  ├──────────────────────┤  ├─────────────────────────┤      │
│  │ Internal:            │  │ Internal:               │      │
│  │ - Authenticator      │  │ - FREDClient (HTTP)     │      │
│  │ - ChartAPI           │  │                         │      │
│  │ - QuoteSummaryAPI    │  │                         │      │
│  │ - SearchAPI          │  │                         │      │
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

KFC(Korea Financial Client)의 Domain-Based 패턴을 참고하여 설계합니다:

**KFC 구조 (도메인별 분리):**
```kotlin
class KfcClient(
    val etf: EtfApi,      // ETF 도메인
    val corp: CorpApi?    // 기업 공시 도메인
)
```

**UFC 구조 (도메인별 분리):**
```kotlin
class UFCClient(
    val stock: StockApi,    // 주식 도메인 (Yahoo Finance)
    val etf: EtfApi,        // ETF 도메인 (Yahoo Finance)
    val macro: MacroApi,    // 매크로 지표 도메인 (FRED)
    val search: SearchApi   // 검색 도메인 (Yahoo + FRED)
)
```

### 3.2 장점

1. **도메인 중심 설계**
   - 데이터 소스가 아닌 비즈니스 도메인으로 분류
   - 사용자 관점에서 직관적인 API
   - 도메인 컨텍스트에 따른 명확한 책임 분리

2. **확장 용이성**
   - 새로운 도메인 추가 쉬움
   - 기존 도메인에 영향 없이 독립적 확장

3. **사용 편의성**
   - `ufc.stock.history("AAPL")` - 주식 가격 이력
   - `ufc.etf.getHoldings("SPY")` - ETF 보유 종목
   - `ufc.macro.getSeries("GDPC1")` - GDP 데이터
   - `ufc.search.stocks("Apple")` - 주식 검색

4. **독립적인 라이프사이클**
   - 각 도메인은 필요한 Source만 사용
   - 선택적 도메인 사용 가능 (예: FRED API Key 없을 때 macro 비활성화)

### 3.3 패키지 구조

```
com.ulalax.ufc/
├── client/
│   ├── UFCClient.kt
│   └── UFCClientConfig.kt
│
├── api/                              # 도메인 API (Public)
│   ├── StockApi.kt                   # 주식 도메인
│   ├── EtfApi.kt                     # ETF 도메인
│   ├── MacroApi.kt                   # 매크로 지표 도메인
│   └── SearchApi.kt                  # 검색 도메인
│
├── internal/                         # 내부 구현 (Internal)
│   ├── stock/
│   │   └── StockApiImpl.kt
│   ├── etf/
│   │   └── EtfApiImpl.kt
│   ├── macro/
│   │   └── MacroApiImpl.kt
│   ├── search/
│   │   └── SearchApiImpl.kt
│   │
│   ├── yahoo/                        # Yahoo Finance Source
│   │   ├── YahooFinanceSource.kt
│   │   ├── YahooHttpClient.kt
│   │   ├── Authenticator.kt
│   │   ├── ChartAPI.kt
│   │   ├── QuoteSummaryAPI.kt
│   │   └── SearchAPI.kt
│   │
│   └── fred/                         # FRED Source
│       ├── FREDSource.kt
│       └── FREDClient.kt
│
├── model/
│   ├── common/
│   │   ├── Period.kt
│   │   ├── Interval.kt
│   │   └── DataFrequency.kt
│   ├── stock/
│   │   ├── PriceBar.kt
│   │   ├── StockInfo.kt
│   │   └── Financials.kt
│   ├── etf/
│   │   ├── Holdings.kt
│   │   ├── FundProfile.kt
│   │   └── FundPerformance.kt
│   ├── macro/
│   │   ├── Series.kt
│   │   ├── SeriesInfo.kt
│   │   └── Observation.kt
│   └── search/
│       └── SearchResult.kt
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

## 4. Domain API 인터페이스

### 4.1 StockApi (주식 도메인)

```kotlin
/**
 * 주식 도메인 API
 *
 * 개별 주식의 가격, 정보, 재무제표 등을 조회합니다.
 *
 * @source Yahoo Finance
 */
interface StockApi {

    /**
     * 주가 이력 조회
     *
     * @param symbol 주식 심볼 (예: "AAPL", "MSFT")
     * @param period 조회 기간 (기본값: 1년)
     * @param interval 데이터 간격 (기본값: 1일)
     * @return 가격 이력 데이터
     */
    suspend fun history(
        symbol: String,
        period: Period = Period.OneYear,
        interval: Interval = Interval.OneDay
    ): List<PriceBar>

    /**
     * 주가 이력 조회 (날짜 범위 지정)
     *
     * @param symbol 주식 심볼
     * @param start 시작일
     * @param end 종료일
     * @param interval 데이터 간격
     * @return 가격 이력 데이터
     */
    suspend fun history(
        symbol: String,
        start: LocalDate,
        end: LocalDate,
        interval: Interval = Interval.OneDay
    ): List<PriceBar>

    /**
     * 주식 기본 정보 조회
     *
     * @param symbol 주식 심볼
     * @return 주식 정보
     */
    suspend fun info(symbol: String): StockInfo

    /**
     * 재무제표 조회
     *
     * @param symbol 주식 심볼
     * @return 재무제표 데이터
     */
    suspend fun financials(symbol: String): Financials
}
```

### 4.2 EtfApi (ETF 도메인)

```kotlin
/**
 * ETF 도메인 API
 *
 * ETF의 보유 종목, 펀드 정보, 성과 등을 조회합니다.
 *
 * @source Yahoo Finance
 */
interface EtfApi {

    /**
     * ETF 보유 종목 조회
     *
     * @param symbol ETF 심볼 (예: "SPY", "QQQ")
     * @return 상위 보유 종목
     */
    suspend fun getHoldings(symbol: String): Holdings

    /**
     * ETF 펀드 프로필 조회
     *
     * @param symbol ETF 심볼
     * @return 펀드 프로필
     */
    suspend fun getFundProfile(symbol: String): FundProfile

    /**
     * ETF 성과 조회
     *
     * @param symbol ETF 심볼
     * @return 펀드 성과
     */
    suspend fun getFundPerformance(symbol: String): FundPerformance

    /**
     * ETF 가격 이력 조회
     *
     * @param symbol ETF 심볼
     * @param period 조회 기간
     * @param interval 데이터 간격
     * @return 가격 이력 데이터
     */
    suspend fun history(
        symbol: String,
        period: Period = Period.OneYear,
        interval: Interval = Interval.OneDay
    ): List<PriceBar>

    /**
     * ETF 가격 이력 조회 (날짜 범위 지정)
     *
     * @param symbol ETF 심볼
     * @param start 시작일
     * @param end 종료일
     * @param interval 데이터 간격
     * @return 가격 이력 데이터
     */
    suspend fun history(
        symbol: String,
        start: LocalDate,
        end: LocalDate,
        interval: Interval = Interval.OneDay
    ): List<PriceBar>
}
```

### 4.3 MacroApi (매크로 지표 도메인)

```kotlin
/**
 * 매크로 경제 지표 도메인 API
 *
 * FRED의 경제 지표 데이터를 조회합니다.
 *
 * @source FRED (Federal Reserve Economic Data)
 */
interface MacroApi {

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
     * 경제 지표 검색
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

### 4.4 SearchApi (검색 도메인)

```kotlin
/**
 * 검색 도메인 API
 *
 * 주식, ETF, 경제 지표를 통합 검색합니다.
 *
 * @source Yahoo Finance + FRED
 */
interface SearchApi {

    /**
     * 주식/ETF 검색
     *
     * @param query 검색어
     * @param limit 결과 수 제한
     * @return 검색 결과
     */
    suspend fun stocks(
        query: String,
        limit: Int = 10
    ): List<SearchResult>

    /**
     * 경제 지표 검색
     *
     * @param query 검색어
     * @param limit 결과 수 제한
     * @return 검색 결과
     */
    suspend fun economicData(
        query: String,
        limit: Int = 10
    ): List<SearchResult>
}
```

---

## 5. UFCClient Facade

### 5.1 UFCClient 설계

```kotlin
/**
 * UFC (US Free Financial Data Collector) 통합 클라이언트
 *
 * Domain-Based 아키텍처를 사용하여 주식, ETF, 매크로 지표, 검색 기능을 제공합니다.
 *
 * ## 사용 예시
 *
 * ```kotlin
 * // 기본 생성 (FRED API Key 없이)
 * val ufc = UFCClient.create()
 *
 * // FRED API Key 포함
 * val ufc = UFCClient.create(
 *     config = UFCClientConfig(
 *         fredApiKey = "your_api_key"
 *     )
 * )
 *
 * // 주식 도메인 사용
 * val aaplHistory = ufc.stock.history("AAPL", period = Period.OneYear)
 * val aaplInfo = ufc.stock.info("AAPL")
 *
 * // ETF 도메인 사용
 * val spyHoldings = ufc.etf.getHoldings("SPY")
 * val spyProfile = ufc.etf.getFundProfile("SPY")
 *
 * // 매크로 지표 도메인 사용 (FRED API Key 필요)
 * val gdp = ufc.macro.getSeries("GDPC1")
 * val unemployment = ufc.macro.search("unemployment rate")
 *
 * // 검색 도메인 사용
 * val stockResults = ufc.search.stocks("Apple")
 * val macroResults = ufc.search.economicData("GDP")
 *
 * // 종료
 * ufc.close()
 * ```
 *
 * @property stock 주식 도메인 API
 * @property etf ETF 도메인 API
 * @property macro 매크로 지표 도메인 API (FRED API Key 필요)
 * @property search 검색 도메인 API
 */
class UFCClient private constructor(
    val stock: StockApi,
    val etf: EtfApi,
    val macro: MacroApi?,
    val search: SearchApi,
    private val config: UFCClientConfig
) : AutoCloseable {

    companion object {
        /**
         * UFCClient 인스턴스 생성
         *
         * 이 메서드는 다음 순서로 클라이언트를 초기화합니다:
         * 1. HTTP 클라이언트 생성 (공유 리소스)
         * 2. 캐시 초기화 (LRU Cache)
         * 3. Yahoo Finance Source 초기화 (인증 포함)
         * 4. FRED Source 초기화 (API Key가 있을 때만)
         * 5. Domain API 구현체 생성
         *
         * @param config 클라이언트 설정
         * @return UFCClient 인스턴스
         * @throws UFCException 초기화 실패 시
         */
        suspend fun create(
            config: UFCClientConfig = UFCClientConfig()
        ): UFCClient {
            // 1. 공통 HTTP 클라이언트 생성
            val httpClient = HttpClientFactory.create(config.httpConfig)

            // 2. 캐시 초기화
            val cache = LRUCache<String, Any>(
                maxSize = config.cacheConfig.maxSize,
                ttl = config.cacheConfig.ttl
            )

            // 3. Yahoo Finance Source 생성 및 초기화
            val yahooSource = YahooFinanceSourceImpl(
                httpClient = httpClient,
                cache = cache,
                config = config.yahooConfig
            )

            // Yahoo Finance 인증 초기화 (Cookie/Crumb 획득)
            try {
                yahooSource.initialize()
            } catch (e: Exception) {
                httpClient.close()
                throw UFCException(
                    errorCode = ErrorCode.INITIALIZATION_FAILED,
                    message = "Failed to initialize Yahoo Finance source",
                    cause = e
                )
            }

            // 4. FRED Source 생성 및 초기화 (API Key가 있을 때만)
            val fredSource = config.fredApiKey?.let { apiKey ->
                val source = FREDSourceImpl(
                    httpClient = httpClient,
                    apiKey = apiKey,
                    cache = cache,
                    config = config.fredConfig
                )

                try {
                    source.initialize()
                    source
                } catch (e: Exception) {
                    // FRED 초기화 실패 시 경고만 로그하고 null 반환
                    logger.warn("Failed to initialize FRED source, macro API will be unavailable", e)
                    null
                }
            }

            // 5. 도메인 API 구현체 생성
            val stockApi = StockApiImpl(yahooSource)
            val etfApi = EtfApiImpl(yahooSource)
            val macroApi = fredSource?.let { MacroApiImpl(it) }
            val searchApi = SearchApiImpl(yahooSource, fredSource)

            return UFCClient(
                stock = stockApi,
                etf = etfApi,
                macro = macroApi,
                search = searchApi,
                yahooSource = yahooSource,
                fredSource = fredSource,
                httpClient = httpClient,
                cache = cache,
                config = config
            )
        }
    }

    // Internal references for cleanup
    private val yahooSource: YahooFinanceSource
    private val fredSource: FREDSource?
    private val httpClient: HttpClient
    private val cache: LRUCache<String, Any>

    /**
     * 모든 리소스 정리
     *
     * 리소스 정리는 역순으로 진행됩니다:
     * 1. Domain API (참조만 제거)
     * 2. FRED Source 종료
     * 3. Yahoo Finance Source 종료
     * 4. 캐시 정리
     * 5. HTTP 클라이언트 종료
     *
     * 이 메서드는 idempotent하며, 여러 번 호출해도 안전합니다.
     */
    override fun close() {
        try {
            // 1. Domain API는 Source를 참조만 하므로 별도 정리 불필요

            // 2. FRED Source 종료 (있을 경우)
            fredSource?.close()

            // 3. Yahoo Finance Source 종료
            yahooSource.close()

            // 4. 캐시 정리
            cache.clear()

            // 5. HTTP 클라이언트 종료 (모든 연결 종료)
            httpClient.close()

            logger.info("UFCClient closed successfully")
        } catch (e: Exception) {
            logger.error("Error while closing UFCClient", e)
            // 예외를 던지지 않고 로그만 남김 (close는 best-effort)
        }
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
├── StockApi (StockApiImpl)
│   └── YahooFinanceSource
│       ├── HttpClient
│       ├── Authenticator
│       ├── Cache
│       └── YahooConfig
│
├── EtfApi (EtfApiImpl)
│   └── YahooFinanceSource (shared)
│
├── MacroApi (MacroApiImpl) [optional]
│   └── FREDSource
│       ├── HttpClient
│       ├── Cache
│       └── FREDConfig
│
└── SearchApi (SearchApiImpl)
    ├── YahooFinanceSource (shared)
    └── FREDSource (shared, optional)
```

---

## 9. 참고 자료

- **KFC (Korea Financial Client)**: Multi-Source 패턴 참조
- **Ktor Documentation**: https://ktor.io/docs/
- **kotlinx.serialization**: https://github.com/Kotlin/kotlinx.serialization
- **Clean Architecture**: Robert C. Martin

---

**다음 문서**: [02-error-handling.md](./02-error-handling.md)
