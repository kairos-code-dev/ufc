# UFC (US Free Financial Data Collector) - 프로젝트 개요

## 문서 정보
- **버전**: 2.0.0
- **최종 수정일**: 2025-12-01
- **작성자**: Claude Code (Software Architect)
- **문서 상태**: Active

---

## 1. 프로젝트 비전

### 1.1 프로젝트 정의

UFC(US Free Financial Data Collector)는 **무료 미국 금융 데이터 수집 라이브러리**입니다.

**핵심 가치:**
- **Multi-Source Architecture**: Yahoo Finance + FRED (Federal Reserve Economic Data)
- **타입 안정성**: Kotlin의 강력한 타입 시스템
- **현대적 비동기**: Coroutines 기반 고성능 데이터 수집
- **통합 에러 처리**: ErrorCode 기반 단일 예외 시스템
- **JVM 생태계**: JDK 21, Kotlin 2.1.0

### 1.2 핵심 목표

1. **다중 데이터 소스 통합**
   - Yahoo Finance: 주가, ETF, 재무제표, 뉴스
   - FRED: GDP, 실업률, 인플레이션, 금리 등 매크로 경제 지표

2. **완전한 기능성**
   - Yahoo Finance: Python yfinance 주요 기능 구현
   - FRED: Python fredapi 주요 기능 구현

3. **성능 최적화**
   - Kotlin Coroutines 기반 비동기 처리
   - JDK 21 Virtual Threads 활용

4. **사용 편의성**
   - 통합 클라이언트 (UFCClient)
   - 직관적인 API
   - 표준 Kotlin 컬렉션 반환

5. **안정성**
   - ErrorCode 기반 예외 시스템
   - 자동 재시도 및 Rate Limiting
   - 포괄적인 에러 처리

---

## 2. 데이터 소스

### 2.1 Yahoo Finance

**제공 데이터:**
- **주가 데이터**: OHLCV, 조정 종가, Intraday/Daily
- **ETF 데이터**: Top Holdings, Sector Weightings, Asset Allocation
- **재무 정보**: 손익계산서, 재무상태표, 현금흐름표
- **기업 행동**: 배당, 주식 분할
- **분석 데이터**: 애널리스트 추천, 주주 구성, ESG 점수
- **뉴스**: 종목별 뉴스, 실적 발표 일정

**커버리지:**
- 주식, ETF, 뮤추얼 펀드, 암호화폐, 선물, 통화 등
- 전 세계 시장 지수

### 2.2 FRED (Federal Reserve Economic Data)

**제공 데이터:**
- **국민 경제**: GDP, GNP, 개인 소득
- **노동 시장**: 실업률, 비농업 고용, 노동 참가율
- **물가 지표**: CPI, PCE, PPI
- **금리**: Federal Funds Rate, Treasury Yields (10Y, 30Y)
- **통화 정책**: M1, M2, 연준 자산
- **주택 시장**: 주택 가격 지수, 주택 착공
- **소비자 신뢰**: Consumer Sentiment, Retail Sales
- **국제 무역**: 무역 수지, 환율

**커버리지:**
- 800,000+ 시계열 데이터
- 미국 경제 지표 중심
- 일부 국제 지표 포함

---

## 3. 아키텍처 개요

### 3.1 Multi-Source 아키텍처

```
┌──────────────────────────────────────────────────────────┐
│                    UFCClient (Facade)                    │
│  val yahoo: YahooFinanceSource                           │
│  val fred: FREDSource                                    │
└──────────────────────────────────────────────────────────┘
                          ↓
┌────────────────────────────┬─────────────────────────────┐
│   YahooFinanceSource       │      FREDSource             │
├────────────────────────────┼─────────────────────────────┤
│ - Ticker                   │ - Series (시계열 조회)      │
│ - ETF                      │ - Search (지표 검색)        │
│ - Search/Screener          │ - Categories (카테고리)     │
│ - Chart API                │ - Releases (발표 일정)      │
│ - QuoteSummary API         │ - Vintage Data (개정 이력)  │
└────────────────────────────┴─────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────┐
│              Shared Infrastructure                       │
│  - ErrorCode + UFCException                              │
│  - HTTP Clients (Ktor)                                   │
│  - Serialization (kotlinx.serialization)                 │
│  - Caching (LRU)                                         │
│  - Retry Logic                                           │
└──────────────────────────────────────────────────────────┘
```

### 3.2 DataSource 인터페이스

모든 데이터 소스는 공통 인터페이스를 구현합니다:

```kotlin
interface DataSource {
    val name: String
    suspend fun initialize()
    fun close()
}

interface YahooFinanceSource : DataSource {
    fun ticker(symbol: String): Ticker
    fun etf(symbol: String): ETF
    fun search(query: String): Search
}

interface FREDSource : DataSource {
    suspend fun getSeries(seriesId: String, ...): Series
    suspend fun search(text: String, ...): List<SeriesInfo>
    suspend fun getCategory(categoryId: Int): Category
}
```

---

## 4. 에러 처리 전략

### 4.1 ErrorCode 기반 시스템

**변경 사항:**
- **기존**: 여러 개의 예외 클래스 (YFException.Auth, YFException.RateLimit 등)
- **신규**: 단일 예외 + ErrorCode enum

**구조:**
```kotlin
class UFCException(
    val errorCode: ErrorCode,
    message: String,
    cause: Throwable? = null,
    val metadata: Map<String, Any> = emptyMap()
) : Exception(message, cause)

enum class ErrorCode {
    // Network & Connection
    NETWORK_ERROR,
    CONNECTION_TIMEOUT,
    SSL_ERROR,

    // Authentication
    AUTH_FAILED,
    INVALID_API_KEY,
    CRUMB_EXPIRED,

    // Rate Limiting
    RATE_LIMITED,
    TOO_MANY_REQUESTS,

    // Data Errors
    NOT_FOUND,
    INVALID_SYMBOL,
    INVALID_SERIES_ID,
    NO_DATA_AVAILABLE,

    // Parsing Errors
    PARSE_ERROR,
    SERIALIZATION_ERROR,

    // Parameter Errors
    INVALID_PARAMETER,
    INVALID_DATE_RANGE,
    INVALID_INTERVAL,

    // Server Errors
    SERVER_ERROR,
    SERVICE_UNAVAILABLE,

    // Unknown
    UNKNOWN_ERROR
}
```

**장점:**
- 단일 catch 블록으로 모든 예외 처리
- ErrorCode로 세밀한 분기 가능
- metadata로 컨텍스트 정보 전달
- 로깅 및 모니터링 용이

### 4.2 사용 예시

```kotlin
try {
    val etf = ufc.yahoo.etf("SPY")
    val data = etf.fetchFundsData()
} catch (e: UFCException) {
    when (e.errorCode) {
        ErrorCode.NOT_FOUND -> println("ETF not found: ${e.metadata["symbol"]}")
        ErrorCode.RATE_LIMITED -> {
            val retryAfter = e.metadata["retryAfter"] as? Long ?: 60
            println("Rate limited. Retry after $retryAfter seconds")
        }
        ErrorCode.AUTH_FAILED -> println("Authentication failed. Please reinitialize.")
        else -> println("Error: ${e.message}")
    }
}
```

---

## 5. 주요 기능

### 5.1 Yahoo Finance 기능

#### 5.1.1 주가 데이터 (Ticker)
- Historical Price (과거 가격)
- Real-time Quote (실시간 가격)
- Corporate Actions (배당, 분할)
- 재무제표 (손익계산서, 재무상태표, 현금흐름표)
- 애널리스트 추천
- 주주 정보
- 뉴스

#### 5.1.2 ETF 데이터 (ETF)
- Top Holdings (상위 보유 종목)
- Sector Weightings (섹터별 비중)
- Asset Allocation (자산 배분)
- Fund Profile (펀드 프로필)
- Equity/Bond Holdings (주식/채권 지표)
- NAV vs 시장가격 비교
- 배당 정보

#### 5.1.3 검색 및 스크리닝
- 종목 검색 (Ticker Search)
- 커스텀 스크리너 (Query DSL)
- 사전 정의된 스크리너

### 5.2 FRED 기능

#### 5.2.1 시계열 데이터 조회
- `getSeries(seriesId)`: 시계열 데이터 조회
- `getSeriesInfo(seriesId)`: 시리즈 메타데이터
- `getSeriesFirstRelease(seriesId)`: 첫 발표 데이터 (개정 제외)
- `getSeriesAllReleases(seriesId)`: 모든 개정 이력
- `getSeriesAsOfDate(seriesId, date)`: 특정 시점 데이터
- `getSeriesVintageDates(seriesId)`: 개정 날짜 목록

#### 5.2.2 검색 및 탐색
- `search(text)`: 전문 검색
- `searchByCategory(categoryId)`: 카테고리별 검색
- `searchByRelease(releaseId)`: 발표별 검색
- 필터링: frequency, units, seasonal_adjustment

#### 5.2.3 주요 경제 지표

| 카테고리 | Series ID | 지표명 |
|---------|-----------|--------|
| **GDP** | GDPC1 | Real GDP (실질 GDP) |
| | GDPPOT | Potential GDP (잠재 GDP) |
| **실업** | UNRATE | Unemployment Rate (실업률) |
| | PAYEMS | Nonfarm Payrolls (비농업 고용) |
| **인플레이션** | CPIAUCSL | CPI (소비자 물가 지수) |
| | PCE | Personal Consumption Expenditures (개인 소비 지출) |
| **금리** | DFF | Federal Funds Rate (연방 기금 금리) |
| | DGS10 | 10-Year Treasury Yield (10년물 국채 수익률) |
| **소비자 신뢰** | UMCSENT | Consumer Sentiment (소비자 신뢰 지수) |
| **주택** | CSUSHPISA | S&P/Case-Shiller Home Price Index |

---

## 6. 기술 스택

### 6.1 핵심 기술

| 카테고리 | 기술 | 버전 | 사유 |
|---------|------|------|------|
| **언어** | Kotlin | 2.1.0 | 타입 안정성, 코루틴 |
| **JDK** | OpenJDK | 21 LTS | Virtual Threads |
| **HTTP 클라이언트** | Ktor Client | 3.0.1 | Kotlin Native, 멀티 플랫폼 |
| **직렬화** | kotlinx.serialization | 1.7.3 | 성능, 타입 안전성 |
| **HTML 파싱** | Jsoup | 1.18.1 | Yahoo Finance HTML 파싱 |
| **XML 파싱** | kotlinx-serialization-xml | 내장 | FRED XML 응답 |
| **로깅** | SLF4J + Logback | 2.0.x | 표준 로깅 |

### 6.2 프로젝트 정보

- **프로젝트명**: UFC (US Free Financial Data Collector)
- **패키지명**: `com.ulalax.ufc`
- **라이센스**: Apache License 2.0
- **Git**: https://github.com/ulalax/ufc

---

## 7. 프로젝트 구조

```
ufc/
├── src/
│   ├── main/kotlin/com/ulalax/ufc/
│   │   ├── client/                 # UFCClient (Facade)
│   │   │   ├── UFCClient.kt
│   │   │   └── UFCClientConfig.kt
│   │   │
│   │   ├── source/                 # DataSource 구현
│   │   │   ├── DataSource.kt       # 공통 인터페이스
│   │   │   ├── yahoo/
│   │   │   │   ├── YahooFinanceSource.kt
│   │   │   │   ├── YahooFinanceClient.kt (HTTP)
│   │   │   │   ├── Authenticator.kt
│   │   │   │   ├── Ticker.kt
│   │   │   │   ├── ETF.kt
│   │   │   │   └── Search.kt
│   │   │   │
│   │   │   └── fred/
│   │   │       ├── FREDSource.kt
│   │   │       ├── FREDClient.kt (HTTP)
│   │   │       └── FREDApi.kt
│   │   │
│   │   ├── model/                  # 데이터 모델
│   │   │   ├── common/             # 공통 모델
│   │   │   ├── yahoo/
│   │   │   │   ├── chart/
│   │   │   │   ├── etf/
│   │   │   │   ├── quote/
│   │   │   │   └── financials/
│   │   │   │
│   │   │   └── fred/
│   │   │       ├── Series.kt
│   │   │       ├── SeriesInfo.kt
│   │   │       └── Category.kt
│   │   │
│   │   ├── exception/              # 에러 처리
│   │   │   ├── UFCException.kt
│   │   │   └── ErrorCode.kt
│   │   │
│   │   ├── serialization/          # 시리얼라이저
│   │   ├── cache/                  # 캐싱
│   │   └── utils/                  # 유틸리티
│   │
│   └── test/kotlin/com/ulalax/ufc/
│       ├── source/yahoo/
│       ├── source/fred/
│       └── integration/
│
├── plan/                           # 기술 명세서
│   ├── 00-project-overview.md      # 본 문서
│   ├── 01-architecture-design.md
│   ├── 02-error-handling.md
│   ├── 03-yahoo-finance-core.md
│   ├── 04-yahoo-finance-etf.md
│   ├── 05-yahoo-finance-price.md
│   ├── 06-fred-macro-indicators.md
│   ├── 07-advanced-topics.md
│   └── 08-data-models-reference.md
│
└── build.gradle.kts
```

---

## 8. 개발 로드맵

### Phase 1: 핵심 인프라 + 에러 처리 (Week 1-2)

**목표**: Multi-source 아키텍처 기반 구축

- [ ] UFCClient Facade 설계
- [ ] DataSource 인터페이스
- [ ] ErrorCode enum + UFCException 구현
- [ ] Ktor HTTP 클라이언트 (공통)
- [ ] LRU 캐싱 전략
- [ ] 단위 테스트

**산출물**:
- `UFCClient` 완성
- `ErrorCode` 시스템 완성
- 공통 인프라 완성

### Phase 2: Yahoo Finance 인증 및 가격 데이터 (Week 3-4)

**목표**: Yahoo Finance 기본 기능

- [ ] YahooFinanceSource 구현
- [ ] Cookie/Crumb 인증
- [ ] Chart API (Historical Price)
- [ ] Ticker 클래스 (가격 히스토리, 배당, 분할)
- [ ] 데이터 모델 + 시리얼라이저

**산출물**:
- `YahooFinanceSource` 완성
- `Ticker` 클래스 완성
- Chart API 완성

### Phase 3: Yahoo Finance ETF (Week 5)

**목표**: ETF 전용 기능

- [ ] QuoteSummary API (전체 모듈)
- [ ] ETF 클래스
- [ ] Top Holdings, Sector Weightings, Asset Allocation
- [ ] NAV vs 시장가격 비교
- [ ] 배당 정보

**산출물**:
- `ETF` 클래스 완성
- QuoteSummary API 전체 모듈 구현
- ETF 분석 기능 완성

### Phase 4: FRED 매크로 지표 (Week 6-7)

**목표**: FRED API 통합

- [ ] FREDSource 구현
- [ ] FRED API Key 인증
- [ ] Series 조회 (getSeries, getSeriesInfo)
- [ ] Vintage Data (첫 발표, 전체 개정, 특정 시점)
- [ ] Search API
- [ ] 주요 경제 지표 Enum
- [ ] XML 파싱

**산출물**:
- `FREDSource` 완성
- 주요 경제 지표 조회 가능
- Vintage Data 지원

### Phase 5: Yahoo Finance 재무제표 및 분석 (Week 8-9)

**목표**: 재무 데이터

- [ ] 손익계산서, 재무상태표, 현금흐름표
- [ ] 애널리스트 추천
- [ ] 주주 정보
- [ ] ESG 점수

**산출물**:
- 재무제표 API 완성
- 분석 데이터 API 완성

### Phase 6: 검색 및 스크리닝 (Week 10)

**목표**: 검색 기능

- [ ] Yahoo Finance Search API
- [ ] Yahoo Finance Screener
- [ ] FRED Search (full-text, category, release)
- [ ] 필터링 및 정렬

**산출물**:
- `Search` 클래스 완성
- `Screener` 클래스 완성
- FRED 검색 완성

### Phase 7: 성능 최적화 (Week 11)

**목표**: 성능 향상

- [ ] Virtual Threads 활용
- [ ] 병렬 요청 배치 처리
- [ ] 캐싱 최적화
- [ ] 연결 풀링
- [ ] 메모리 프로파일링

**산출물**:
- 성능 벤치마크
- 최적화 가이드

### Phase 8: 문서화 및 배포 (Week 12)

**목표**: 문서 및 배포

- [ ] KDoc 작성
- [ ] 사용자 가이드
- [ ] API 레퍼런스
- [ ] 예제 코드
- [ ] Maven Central 배포

**산출물**:
- 완전한 문서 세트
- 1.0.0 릴리스

---

## 9. 사용 예시

### 9.1 기본 사용

```kotlin
import com.ulalax.ufc.UFCClient

suspend fun main() {
    val ufc = UFCClient()

    // Yahoo Finance: ETF 데이터
    val spy = ufc.yahoo.etf("SPY")
    val profile = spy.getFundProfile()
    println("Expense Ratio: ${profile.feesExpensesInvestment?.annualReportExpenseRatio?.fmt}")

    val holdings = spy.getTopHoldings()
    holdings.holdings?.forEach { holding ->
        println("${holding.symbol}: ${holding.holdingPercent?.fmt}")
    }

    // Yahoo Finance: 주가 데이터
    val aapl = ufc.yahoo.ticker("AAPL")
    val history = aapl.history(period = Period.OneYear)
    history.forEach { bar ->
        println("${bar.date}: Open=${bar.open}, Close=${bar.close}, Volume=${bar.volume}")
    }

    // FRED: GDP 조회
    val gdp = ufc.fred.getSeries("GDPC1",
        observationStart = "2020-01-01",
        observationEnd = "2024-01-01"
    )
    gdp.observations.forEach { obs ->
        println("${obs.date}: ${obs.value}")
    }

    // FRED: 검색
    val unemployment = ufc.fred.search("unemployment rate")
    unemployment.forEach {
        println("${it.id}: ${it.title}")
    }
}
```

### 9.2 에러 처리

```kotlin
try {
    val data = ufc.yahoo.etf("INVALID").fetchFundsData()
} catch (e: UFCException) {
    when (e.errorCode) {
        ErrorCode.NOT_FOUND -> {
            val symbol = e.metadata["symbol"]
            println("Symbol $symbol not found")
        }
        ErrorCode.RATE_LIMITED -> {
            val retryAfter = e.metadata["retryAfter"] as Long
            delay(retryAfter * 1000)
            // retry
        }
        else -> logger.error("UFC Error: ${e.errorCode}", e)
    }
}
```

### 9.3 멀티 소스 통합

```kotlin
// 주식 + 경제 지표 통합 분석
suspend fun analyzeMacroImpact(symbol: String) {
    val ticker = ufc.yahoo.ticker(symbol)
    val history = ticker.history(period = Period.FiveYears)

    // 동시에 여러 경제 지표 조회
    val (gdp, unemployment, inflation) = coroutineScope {
        listOf(
            async { ufc.fred.getSeries("GDPC1") },
            async { ufc.fred.getSeries("UNRATE") },
            async { ufc.fred.getSeries("CPIAUCSL") }
        ).awaitAll()
    }

    // 데이터 분석이 필요한 경우:
    // 1. JSON으로 직렬화하여 Python pandas로 전달
    // 2. Kotlin DataFrame 라이브러리 추가
    // 3. Apache Commons Math로 통계 계산
}
```

### 9.4 데이터 분석

UFC는 **데이터 수집 전용 라이브러리**입니다. 데이터 분석이 필요한 경우 다음 라이브러리를 사용하세요:

**Kotlin 분석 라이브러리:**
- **Kotlin DataFrame**: `implementation("org.jetbrains.kotlinx:dataframe:0.14.1")`
- **Apache Commons Math**: 통계 계산
- **Lets-Plot**: 데이터 시각화

**Python 연동:**
- kotlinx.serialization으로 JSON 직렬화
- Python pandas로 불러오기

**사용 예시:**
```kotlin
// 1. UFC로 데이터 수집
val ufc = UFCClient()
val holdings = ufc.yahoo.etf("SPY").getTopHoldings()

// 2. JSON 저장
val json = Json.encodeToString(holdings)
File("holdings.json").writeText(json)

// 3. Python에서 분석
// import pandas as pd
// df = pd.read_json("holdings.json")
// df.groupby("sector").sum()
```

**Kotlin DataFrame 사용 예시:**
```kotlin
// build.gradle.kts에 추가
// implementation("org.jetbrains.kotlinx:dataframe:0.14.1")

val holdings = ufc.yahoo.etf("SPY").getTopHoldings()
val df = holdings.holdings?.let {
    dataFrameOf(
        "symbol" to it.map { h -> h.symbol },
        "name" to it.map { h -> h.holdingName },
        "weight" to it.map { h -> h.holdingPercent?.raw }
    )
}
df?.filter { weight > 0.01 }
    ?.sortByDesc { weight }
    ?.print()
```

---

## 10. 문서 구조

### 10.1 핵심 명세 문서

1. **[00-project-overview.md](./00-project-overview.md)** (본 문서)
   - 프로젝트 전체 개요
   - Multi-source 아키텍처
   - 로드맵

2. **[01-architecture-design.md](./01-architecture-design.md)**
   - Multi-source 아키텍처 설계
   - DataSource 인터페이스
   - UFCClient Facade
   - 레이어 구조

3. **[02-error-handling.md](./02-error-handling.md)**
   - ErrorCode enum 정의
   - UFCException 클래스
   - 에러 코드별 처리 방법
   - 재시도 정책

4. **[03-yahoo-finance-core.md](./03-yahoo-finance-core.md)**
   - YahooFinanceSource 구현
   - HTTP 클라이언트 (Ktor)
   - Cookie/Crumb 인증
   - 에러 처리 및 재시도

5. **[04-yahoo-finance-etf.md](./04-yahoo-finance-etf.md)**
   - QuoteSummary API (전체 모듈)
   - ETF 클래스 설계
   - Top Holdings, Sector Weightings, Asset Allocation
   - NAV vs 시장가격 비교
   - 배당 정보

6. **[05-yahoo-finance-price.md](./05-yahoo-finance-price.md)**
   - Chart API (Historical Price)
   - Chart API 파라미터 명세 (Interval, Range, Events)
   - Corporate Actions (배당, 분할)
   - Real-time Quote
   - Ticker Info

7. **[06-fred-macro-indicators.md](./06-fred-macro-indicators.md)**
   - FRED API 개요
   - API Key 인증
   - Series 조회 (getSeries, getSeriesInfo)
   - Vintage Data (개정 이력)
   - Search API
   - 주요 경제 지표별 강타입 클래스 (GDP, CPI, Unemployment, Interest Rates)
   - 경제 지표 Enum
   - 데이터 모델

8. **[07-advanced-topics.md](./07-advanced-topics.md)**
   - TLS Fingerprinting 회피
   - Virtual Threads 활용
   - 캐싱 전략
   - 병렬 처리
   - 디버깅 가이드

9. **[08-data-models-reference.md](./08-data-models-reference.md)**
   - 전체 데이터 모델 참조
   - 공통 모델 (ValueFormat, Error, Meta)
   - Yahoo Finance 모델 (Events, ETF Holdings, Search, Screener)
   - FRED 모델 (MacroData, Category)
   - Enum 타입 (Interval, Range, Events, QuoteSummaryModule, FREDUnits 등)
   - DataFrame 스키마

---

## 11. 성공 지표

### 11.1 기능 완성도
- [ ] Yahoo Finance 주요 기능 100% 커버
- [ ] FRED 주요 기능 100% 커버
- [ ] Multi-source 통합 API 제공
- [ ] ErrorCode 시스템 완성

### 11.2 성능
- [ ] 단일 종목 데이터 수집: < 500ms
- [ ] 100개 종목 병렬 수집: < 5초
- [ ] FRED 시계열 조회: < 300ms

### 11.3 안정성
- [ ] 단위 테스트 커버리지: > 80%
- [ ] 통합 테스트 통과율: 100%
- [ ] ErrorCode 커버리지: 100%

---

## 12. 참고 자료

- **Python yfinance**: https://github.com/ranaroussi/yfinance
- **Python fredapi**: https://github.com/mortada/fredapi
- **FRED API Documentation**: https://fred.stlouisfed.org/docs/api/fred/
- **Ktor Documentation**: https://ktor.io/docs/

---

**마지막 업데이트**: 2025-12-01
**다음 문서**: [01-architecture-design.md](./01-architecture-design.md)
