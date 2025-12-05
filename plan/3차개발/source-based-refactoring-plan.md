# UFC 소스 기반 API 리팩토링 계획

## 목표

기존 도메인 중심 구조에서 **소스 기반 구조**로 전환하여 다음 API 제공:

```kotlin
// 개별 클라이언트 사용
val yahoo = YahooClient.create()
yahoo.quoteSummary("AAPL", QuoteSummaryModule.PRICE)
yahoo.chart("AAPL", Interval.OneDay, Period.OneYear)

val fred = FredClient.create(apiKey)
fred.series("GDP")

val bi = BusinessInsiderClient.create()
bi.searchIsin("US0378331005")

// 통합 사용
val ufc = Ufc.create(UfcConfig(fredApiKey = "xxx"))
ufc.yahoo.quoteSummary(...)
ufc.quoteSummary(...)  // 직접 접근도 가능
```

---

## 현재 구현 상태

### 완료된 패키지

```
src/main/kotlin/com/ulalax/ufc/
├── Ufc.kt                      # 통합 파사드
├── UfcConfig.kt
├── common/                     # 공통 인프라 (7개 파일)
│   ├── ratelimit/
│   ├── http/
│   └── exception/
├── yahoo/                      # Yahoo 클라이언트 (15개 파일)
│   ├── YahooClient.kt
│   ├── model/
│   └── internal/
├── fred/                       # FRED 클라이언트 (8개 파일)
│   ├── FredClient.kt
│   ├── model/
│   └── internal/
└── businessinsider/            # BI 클라이언트 (5개 파일)
    ├── BusinessInsiderClient.kt
    ├── model/
    └── internal/
```

### 복원된 기존 코드
- `api/`, `domain/`, `infrastructure/` 패키지 (git restore로 복원)
- 기존 테스트 파일들

---

## 아키텍처 가이드 준수 현황

참조: `doc/archtecture-guide.md`

| 원칙 | 준수도 | 비고 |
|------|--------|------|
| 단방향 의존성 | 85% | internal 패키지 활용 |
| 의존성 주입 | 90% | Config 기반 주입 |
| 도메인 순수성 | 70% | 직렬화 모델 노출 문제 |
| 명시적 경계 | 80% | internal 키워드 사용 |
| 에러 처리 전략 | 50% | **개선 필요** |

---

## 개선 작업

### Phase 1: 예외 처리 통일 (Critical)

**문제점:**
```kotlin
// 현재 - 다양한 예외 타입 혼재
throw Exception("API 요청 실패")
throw IllegalArgumentException("ISIN must be 12 characters")
throw NoSuchElementException("No results found")
```

**목표:**
```kotlin
// 통일 - UfcException + ErrorCode
throw UfcException(
    ErrorCode.EXTERNAL_API_ERROR,
    "API 요청 실패",
    metadata = mapOf("status" to statusCode)
)
```

**수정 파일:**
- `yahoo/YahooClient.kt`
- `fred/internal/FredHttpClient.kt`
- `businessinsider/internal/BusinessInsiderHttpClient.kt`

### Phase 2: GlobalRateLimiters 구현 (High)

**문제점:**
- 현재 Rate Limiter가 클라이언트 인스턴스별로 생성됨
- 여러 `YahooClient.create()` 호출 시 각각 독립적인 Rate Limiter 보유
- 동일 소스에 대한 Rate Limit이 실질적으로 배가되어 API 제한 초과 위험

**목표:**
- 소스별(Yahoo, FRED, BusinessInsider) 단일 Rate Limiter 공유
- KFC 프로젝트의 `GlobalRateLimiters` 패턴 적용

---

#### 2.1 현재 구조 vs 목표 구조

**현재 (문제):**
```
YahooClient.create() → 새 RateLimiter 생성
YahooClient.create() → 새 RateLimiter 생성 (별도)
→ 두 클라이언트가 각각 50 RPS 허용 = 실제 100 RPS 가능
```

**목표:**
```
YahooClient.create() → GlobalRateLimiters.getYahooLimiter() 참조
YahooClient.create() → GlobalRateLimiters.getYahooLimiter() 참조 (동일)
→ 두 클라이언트가 동일한 50 RPS Rate Limiter 공유
```

---

#### 2.2 GlobalRateLimiters 구조

**파일 위치:** `common/ratelimit/GlobalRateLimiters.kt`

**핵심 구조:**
```kotlin
object GlobalRateLimiters {
    // Volatile 변수: JMM visibility 보장
    @Volatile private var yahooInstance: TokenBucketRateLimiter? = null
    @Volatile private var fredInstance: TokenBucketRateLimiter? = null
    @Volatile private var businessInsiderInstance: TokenBucketRateLimiter? = null

    // 소스별 독립적인 Lock 객체
    private val yahooLock = Any()
    private val fredLock = Any()
    private val businessInsiderLock = Any()

    // Double-checked locking 패턴
    fun getYahooLimiter(config: RateLimitConfig): TokenBucketRateLimiter {
        yahooInstance?.let { return it }
        return synchronized(yahooLock) {
            yahooInstance ?: TokenBucketRateLimiter(config).also {
                yahooInstance = it
                logger.info { "Initialized global Yahoo RateLimiter: ${config}" }
            }
        }
    }

    fun getFredLimiter(config: RateLimitConfig): TokenBucketRateLimiter { ... }
    fun getBusinessInsiderLimiter(config: RateLimitConfig): TokenBucketRateLimiter { ... }

    // 테스트 전용
    fun resetForTesting() { ... }
}
```

---

#### 2.3 설정 통합

**RateLimitingSettings 확장:**
```kotlin
data class RateLimitingSettings(
    val yahoo: RateLimitConfig = yahooDefault(),
    val fred: RateLimitConfig = fredDefault(),
    val businessInsider: RateLimitConfig = businessInsiderDefault()
) {
    companion object {
        // Yahoo Finance: 보수적 50 RPS (공식 제한 미공개)
        fun yahooDefault() = RateLimitConfig(capacity = 50, refillRate = 50)

        // FRED: 120 requests/minute = 2 RPS
        fun fredDefault() = RateLimitConfig(capacity = 2, refillRate = 2, waitTimeoutMillis = 120000L)

        // BusinessInsider: 보수적 10 RPS (스크래핑)
        fun businessInsiderDefault() = RateLimitConfig(capacity = 10, refillRate = 10)

        // Rate Limiting 비활성화
        fun unlimited() = RateLimitingSettings(
            yahoo = RateLimitConfig(enabled = false),
            fred = RateLimitConfig(enabled = false),
            businessInsider = RateLimitConfig(enabled = false)
        )
    }
}
```

---

#### 2.4 클라이언트 수정

**YahooClient.create() 수정:**
```kotlin
fun create(config: YahooClientConfig = YahooClientConfig()): YahooClient {
    // GlobalRateLimiters에서 공유 Rate Limiter 획득
    val rateLimiter = GlobalRateLimiters.getYahooLimiter(config.rateLimitConfig)

    val httpClient = HttpClient(CIO) { ... }
    return YahooClient(httpClient, authenticator, rateLimiter)
}
```

**API 호출 시 Rate Limiting 적용:**
```kotlin
suspend fun quoteSummary(...): QuoteSummaryResponse {
    rateLimiter.acquire()  // Rate Limit 적용
    // ... API 호출
}
```

---

#### 2.5 Thread-Safety 보장

| 기법 | 목적 |
|------|------|
| `@Volatile` | JVM 메모리 가시성 보장, 다른 스레드에서 최신 값 확인 |
| `synchronized` | 초기화 시점 동시성 제어 |
| Double-checked locking | Lock 오버헤드 최소화 (이미 초기화된 경우 Lock 없이 반환) |
| 소스별 독립 Lock | Yahoo, FRED, BI 초기화가 서로 블로킹하지 않음 |

---

#### 2.6 설정 우선순위

**동작 방식:**
1. 첫 번째 `Ufc.create()` 또는 `YahooClient.create()` 호출의 설정이 적용
2. 이후 호출은 기존 Rate Limiter 재사용 (설정 무시)
3. JVM 프로세스 단위 싱글톤

**예시:**
```kotlin
// 첫 호출: RPS 30으로 초기화
val client1 = YahooClient.create(YahooClientConfig(
    rateLimitConfig = RateLimitConfig(capacity = 30, refillRate = 30)
))

// 두 번째 호출: client1과 동일한 RPS 30 Rate Limiter 공유
val client2 = YahooClient.create()  // 설정 무시됨
```

---

#### 2.7 테스트 지원

**resetForTesting():**
```kotlin
@AfterEach
fun cleanup() {
    GlobalRateLimiters.resetForTesting()  // 테스트 간 상태 초기화
}
```

**테스트 시 Rate Limiting 비활성화:**
```kotlin
val testConfig = YahooClientConfig(
    rateLimitConfig = RateLimitConfig(enabled = false)
)
val client = YahooClient.create(testConfig)
```

---

#### 2.8 수정 파일 목록

| 파일 | 작업 |
|------|------|
| `common/ratelimit/GlobalRateLimiters.kt` | **신규 생성** |
| `common/ratelimit/RateLimitConfig.kt` | `RateLimitingSettings` 확장 |
| `yahoo/YahooClientConfig.kt` | `rateLimitConfig` 필드 추가 |
| `yahoo/YahooClient.kt` | GlobalRateLimiters 연동, acquire() 호출 추가 |
| `fred/FredClientConfig.kt` | `rateLimitConfig` 필드 추가 |
| `fred/FredClient.kt` | GlobalRateLimiters 연동 |
| `businessinsider/BusinessInsiderClientConfig.kt` | `rateLimitConfig` 필드 추가 |
| `businessinsider/BusinessInsiderClient.kt` | GlobalRateLimiters 연동 |

---

#### 2.9 참고: KFC 프로젝트 구현

**파일:** `/home/ulalax/project/kairos/kfc/src/main/kotlin/dev/kairoscode/kfc/infrastructure/common/ratelimit/GlobalRateLimiters.kt`

KFC는 KRX, Naver, OPENDART 3개 소스에 대해 동일 패턴 적용:
- `getKrxLimiter()`, `getNaverLimiter()`, `getOpendartLimiter()`
- 테스트용 `resetForTesting()`
- KDoc으로 사용법 및 제약사항 문서화

---

### Phase 3: 내부 모델 은닉 (High)

**문제점:**
- `QuoteSummaryResponse`, `ChartDataResponse` 등 직렬화용 모델이 public

**목표:**
```
yahoo/
├── model/           # 공개 도메인 모델만
│   ├── Interval.kt
│   ├── Period.kt
│   ├── ChartData.kt
│   └── QuoteSummaryModuleResult.kt
└── internal/
    └── response/    # 직렬화 모델 (internal)
        ├── QuoteSummaryResponse.kt
        └── ChartDataResponse.kt
```

**수정 파일:**
- `yahoo/model/QuoteSummaryResponse.kt` → `yahoo/internal/response/` 이동
- `yahoo/model/ChartDataResponse.kt` → `yahoo/internal/response/` 이동

### Phase 4: 테스트 작성 (KFC 프로젝트 패턴 적용)

**테스트 디렉토리 구조:**
```
src/test/kotlin/com/ulalax/ufc/
├── fixture/
│   └── TestFixtures.kt                    # 중앙 테스트 데이터
├── unit/
│   ├── utils/
│   │   ├── UnitTestBase.kt               # Unit 테스트 베이스
│   │   └── JsonResponseLoader.kt         # JSON 로더
│   └── yahoo/
│       └── fake/
│           └── FakeYahooHttpClient.kt    # Fake 구현
├── integration/
│   ├── utils/
│   │   ├── IntegrationTestBase.kt        # Integration 테스트 베이스
│   │   ├── ResponseRecorder.kt           # 응답 레코딩
│   │   ├── SmartRecorder.kt              # 대용량 레코딩
│   │   └── RecordingConfig.kt            # 레코딩 설정
│   ├── yahoo/
│   │   ├── QuoteSummarySpec.kt           # API 가이드처럼
│   │   └── ChartSpec.kt
│   ├── fred/
│   │   └── SeriesSpec.kt
│   └── businessinsider/
│       └── IsinSearchSpec.kt
└── resources/
    └── responses/                         # 레코딩된 JSON
        ├── yahoo/
        ├── fred/
        └── businessinsider/
```

---

#### 4.1 응답 레코딩 시스템

**RecordingConfig.kt**
```kotlin
object RecordingConfig {
    val isRecordingEnabled: Boolean = System.getProperty("record.responses", "true").toBoolean()

    object Paths {
        object Yahoo {
            const val QUOTE_SUMMARY = "yahoo/quote_summary"
            const val CHART = "yahoo/chart"
        }
        object Fred {
            const val SERIES = "fred/series"
        }
        object BusinessInsider {
            const val ISIN_SEARCH = "businessinsider/isin"
        }
    }
}
```

**SmartRecorder.kt** (3-Tier 전략)
```kotlin
object SmartRecorder {
    private const val TIER1_THRESHOLD = 10_000
    private const val TIER2_THRESHOLD = 100_000

    fun <T> recordSmartly(data: List<T>, category: String, fileName: String) {
        when {
            data.size <= TIER1_THRESHOLD ->
                ResponseRecorder.recordList(data, category, fileName)
            data.size <= TIER2_THRESHOLD ->
                ResponseRecorder.recordList(data.take(TIER1_THRESHOLD), category, "${fileName}_limited")
            else ->
                ResponseRecorder.recordList(data.shuffled().take(1000), category, "${fileName}_sample")
        }
    }
}
```

---

#### 4.2 테스트 베이스 클래스

**IntegrationTestBase.kt**
```kotlin
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class IntegrationTestBase {
    protected lateinit var ufc: Ufc

    @BeforeAll
    fun setUp() = runBlocking {
        val fredApiKey = System.getenv("FRED_API_KEY")
        ufc = if (fredApiKey != null) {
            Ufc.create(UfcConfig(fredApiKey = fredApiKey))
        } else {
            Ufc.create()
        }
    }

    @AfterAll
    fun tearDown() { ufc.close() }

    protected fun integrationTest(
        timeout: Duration = 30.seconds,
        block: suspend () -> Unit
    ) = runTest(timeout = timeout) { block() }
}
```

**UnitTestBase.kt**
```kotlin
@Tag("unit")
abstract class UnitTestBase {
    protected fun unitTest(block: suspend () -> Unit) = runTest { block() }

    protected fun loadMockResponse(category: String, fileName: String): String =
        JsonResponseLoader.load(category, fileName)
}
```

---

#### 4.3 테스트 그룹핑 패턴 (@Nested)

**QuoteSummarySpec.kt** - API 가이드처럼 읽히는 통합 테스트
```kotlin
@DisplayName("YahooClient.quoteSummary() - 주식 요약 정보 조회")
class QuoteSummarySpec : IntegrationTestBase() {

    @Nested
    @DisplayName("기본 동작")
    inner class BasicBehavior {
        @Test
        @DisplayName("AAPL의 PRICE 모듈을 조회할 수 있다")
        fun `returns price module for AAPL`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.yahoo.quoteSummary(symbol, QuoteSummaryModule.PRICE)

            // Then
            assertThat(result.hasModule(QuoteSummaryModule.PRICE)).isTrue()

            // Record
            ResponseRecorder.record(result, RecordingConfig.Paths.Yahoo.QUOTE_SUMMARY, "aapl_price")
        }
    }

    @Nested
    @DisplayName("응답 데이터 스펙")
    inner class ResponseSpec {
        @Test
        @DisplayName("PRICE 모듈은 regularMarketPrice를 포함한다")
        fun `price module contains regularMarketPrice`() = integrationTest { ... }
    }

    @Nested
    @DisplayName("활용 예제")
    inner class UsageExamples {
        @Test
        @DisplayName("여러 모듈을 한번에 조회할 수 있다")
        fun `can request multiple modules at once`() = integrationTest { ... }
    }

    @Nested
    @DisplayName("에러 케이스")
    inner class ErrorCases {
        @Test
        @DisplayName("존재하지 않는 심볼 조회 시 UfcException을 던진다")
        fun `throws UfcException for invalid symbol`() = integrationTest { ... }
    }
}
```

**IDE 출력 예시:**
```
YahooClient.quoteSummary() - 주식 요약 정보 조회
├─ 기본 동작
│  ├─ AAPL의 PRICE 모듈을 조회할 수 있다
│  └─ MSFT의 SUMMARY_DETAIL 모듈을 조회할 수 있다
├─ 응답 데이터 스펙
│  └─ PRICE 모듈은 regularMarketPrice를 포함한다
├─ 활용 예제
│  └─ 여러 모듈을 한번에 조회할 수 있다
└─ 에러 케이스
   └─ 존재하지 않는 심볼 조회 시 UfcException을 던진다
```

---

#### 4.4 TestFixtures (중앙 테스트 데이터)

```kotlin
object TestFixtures {
    object Symbols {
        const val AAPL = "AAPL"
        const val MSFT = "MSFT"
        const val INVALID = "INVALID_SYMBOL_12345"
    }

    object Isin {
        const val APPLE = "US0378331005"
        const val SAMSUNG = "KR7005930003"
        const val INVALID = "XX0000000000"
    }

    object FredSeries {
        const val GDP = "GDP"
        const val UNEMPLOYMENT = "UNRATE"
        const val CPI = "CPIAUCSL"
    }

    object Dates {
        val TRADING_DAY: LocalDate = LocalDate.of(2024, 11, 25)
        val ONE_YEAR_AGO: LocalDate = LocalDate.now().minusYears(1)
    }
}
```

---

#### 4.5 테스트 네이밍 컨벤션

| 유형 | 패턴 | 예시 |
|------|------|------|
| 기본 동작 | `{동작} {대상}` | `returns price module for AAPL` |
| 에러 케이스 | `throws {예외} for {조건}` | `throws UfcException for invalid symbol` |
| 스펙 검증 | `{대상} contains {필드}` | `price module contains regularMarketPrice` |
| 활용 예제 | `can {동작}` | `can request multiple modules at once` |

---

#### 4.6 Gradle 태스크

```kotlin
// build.gradle.kts
tasks.register<Test>("integrationTest") {
    useJUnitPlatform { includeTags("integration") }
    systemProperty("record.responses", project.findProperty("record.responses") ?: "true")
}

tasks.register<Test>("unitTest") {
    useJUnitPlatform { includeTags("unit") }
}
```

**실행:**
```bash
# Integration 테스트 + 레코딩
./gradlew integrationTest -Precord.responses=true

# Unit 테스트 (레코딩된 JSON 사용)
./gradlew unitTest

# 전체 테스트
./gradlew test
```

### Phase 5: 기존 코드 정리

기존 도메인 중심 코드 삭제:
- `api/` 전체
- `domain/` 전체
- `infrastructure/` 전체
- 기존 테스트

---

## 작업 순서

1. [ ] Phase 1: 예외 처리 통일 (3개 파일)
2. [ ] Phase 2: GlobalRateLimiters 구현 (1개 신규 + 7개 수정)
3. [ ] Phase 3: 내부 모델 이동 (2개 파일)
4. [ ] import 정리 및 빌드 확인
5. [ ] Phase 4: Integration 테스트 작성
6. [ ] Phase 5: 기존 코드 삭제
7. [ ] 최종 빌드 및 테스트 실행

---

## 참고 문서

- `doc/archtecture-guide.md` - 아키텍처 원칙
- `plan/2차개발/clean-architecture-refactoring-plan.md` - 이전 리팩토링
- `/home/ulalax/project/kairos/kfc` - KFC 프로젝트 (테스트 패턴, GlobalRateLimiters 참조)
