# Fake 객체 상세 설계 문서
## UFC 프로젝트 TDD 리팩토링

---

## 목차
1. [개요](#개요)
2. [Fake 객체 계층 구조](#fake-객체-계층-구조)
3. [각 Fake 객체의 상세 설계](#각-fake-객체의-상세-설계)
4. [통합 시나리오](#통합-시나리오)
5. [테스트 픽스처](#테스트-픽스처)

---

## 개요

### Fake 객체의 역할

TDD에서 Fake 객체는:
- 외부 의존성을 제거하여 테스트 속도 향상
- 결정론적(deterministic) 동작으로 테스트 신뢰성 확보
- 복잡한 상황을 간단하게 시뮬레이션

### 예상 Fake 객체 목록

```
1. FakeAuthStrategy          → AuthStrategy 모의
2. FakeRateLimiter           → RateLimiter 모의
3. FakeHttpClient            → HttpClient 모의 (응답 주입)
4. RecordedResponseRepository → 레코딩 응답 관리
5. NoOpRateLimiter           → Rate Limiting 제거
6. FakeHttpClientBuilder     → HttpClient 구성 헬퍼
```

---

## Fake 객체 계층 구조

### 계층 1: HTTP 통신 (가장 하위)

```
HttpClient (Ktor)
├── 실제 구현: 네트워크 I/O 수행
└── Fake 구현: 레코딩된 응답 반환

RecordedResponseRepository
└── 역할: JSON 응답 파일 관리, 메모리 캐싱
```

**특징**:
- 가장 낮은 수준의 의존성
- 네트워크 지연 제거
- 응답 재현 가능

### 계층 2: 인증 (중간)

```
AuthStrategy (Yahoo Finance)
├── 실제 구현: CRUMB 토큰 획득
└── Fake 구현: 고정 토큰 반환

AuthResult (Value Object)
└── 실제 구현과 동일 (상태 객체)
```

**특징**:
- 단순한 인터페이스 (authenticate() 하나)
- 상태 객체로 추적 가능

### 계층 3: Rate Limiting (중간)

```
RateLimiter (Token Bucket)
├── 실제 구현: 시간 기반 토큰 리필
├── Fake 구현: 무제한 토큰 반환
└── NoOp 구현: 즉시 반환 (로깅 없음)

RateLimiterStatus (Value Object)
└── 상태 조회용 데이터
```

**특징**:
- 시간 처리 제거
- 호출 기록 가능
- 다양한 구현 제공

### 계층 4: 서비스 (가장 상위)

```
ChartService (YahooChartService)
├── 실제 구현: HTTP + Auth + RateLimit 조합
└── Fake 구현: 미리 정의된 데이터 반환

QuoteService (YahooQuoteSummaryService)
├── 실제 구현: HTTP + Auth + RateLimit 조합
└── Fake 구현: 미리 정의된 데이터 반환
```

**특징**:
- 상위 계층에서는 일반적으로 Fake 필요 없음
- 하위 계층 Fake 객체 조합으로 테스트

---

## 각 Fake 객체의 상세 설계

### 1. FakeAuthStrategy

#### 파일: `src/test/kotlin/com/ulalax/ufc/fake/FakeAuthStrategy.kt`

#### 구현 이유
```
실제 BasicAuthStrategy:
  1. fc.yahoo.com 방문 (쿠키 획득)
  2. /v1/test/getcrumb API 호출
  3. CRUMB 응답 파싱 및 검증
  4. AuthResult 생성

소요 시간: 500ms-2000ms (네트워크 지연)
문제점: 네트워크 불안정성, Yahoo API 변경 가능성

Fake 버전:
  1. 고정 CRUMB 반환
  2. 호출 추적
  3. 실패 시나리오 시뮬레이션

소요 시간: < 1ms
```

#### 코드 예제

```kotlin
class FakeAuthStrategy : AuthStrategy {
    // 상태 변수
    private var callCount = 0
    private var shouldFail = false
    private val callHistory = mutableListOf<Long>() // 타임스탬프
    private var predefinedResult = defaultAuthResult()

    // 실패 타입 선택
    private var failureType: FailureType? = null

    enum class FailureType {
        CRUMB_ACQUISITION_FAILED,
        AUTHENTICATION_FAILED,
        NETWORK_ERROR
    }

    // AuthStrategy 구현
    override suspend fun authenticate(): AuthResult {
        callCount++
        callHistory.add(System.currentTimeMillis())

        if (shouldFail) {
            throw UfcException(
                errorCode = when (failureType) {
                    FailureType.CRUMB_ACQUISITION_FAILED ->
                        ErrorCode.CRUMB_ACQUISITION_FAILED
                    FailureType.AUTHENTICATION_FAILED ->
                        ErrorCode.AUTHENTICATION_FAILED
                    FailureType.NETWORK_ERROR ->
                        ErrorCode.NETWORK_REQUEST_FAILED
                    null -> ErrorCode.AUTHENTICATION_FAILED
                },
                message = "Simulated authentication failure"
            )
        }

        return predefinedResult
    }

    // 테스트 헬퍼 메서드
    fun getCallCount(): Int = callCount

    fun getCallHistory(): List<Long> = callHistory.toList()

    fun getLastCallTime(): Long? = callHistory.lastOrNull()

    fun setFailureMode(
        shouldFail: Boolean,
        type: FailureType? = null
    ) {
        this.shouldFail = shouldFail
        this.failureType = type
    }

    fun setPredefinedResult(result: AuthResult) {
        this.predefinedResult = result
    }

    fun reset() {
        callCount = 0
        callHistory.clear()
        shouldFail = false
        failureType = null
        predefinedResult = defaultAuthResult()
    }

    // 호출 패턴 검증
    fun verifyCalledOnce(): Boolean = callCount == 1

    fun verifyNeverCalled(): Boolean = callCount == 0

    fun verifyCalledAtLeast(count: Int): Boolean = callCount >= count

    companion object {
        private fun defaultAuthResult() = AuthResult(
            crumb = "FAKE_CRUMB_TOKEN_FOR_TESTING_1234567890abcdef",
            strategy = "fake"
        )
    }
}
```

#### 테스트 사용 예시

```kotlin
class YahooChartServiceTest {
    private lateinit var fakeAuthStrategy: FakeAuthStrategy

    @BeforeEach
    fun setUp() {
        fakeAuthStrategy = FakeAuthStrategy()
    }

    @Test
    @DisplayName("인증 성공 시 CRUMB을 사용하여 요청한다")
    fun shouldUseAuthResultInRequest() = runBlocking {
        // Given
        val service = YahooChartService(
            httpClient = fakeHttpClient,
            rateLimiter = fakeRateLimiter,
            authResult = fakeAuthStrategy.authenticate()
        )

        // When
        service.getChartData("AAPL")

        // Then
        assertThat(fakeAuthStrategy.verifyCalledOnce()).isTrue()
    }

    @Test
    @DisplayName("인증 실패 시 예외를 발생시킨다")
    fun shouldThrowExceptionWhenAuthFails() = runBlocking {
        // Given
        fakeAuthStrategy.setFailureMode(
            shouldFail = true,
            type = FakeAuthStrategy.FailureType.CRUMB_ACQUISITION_FAILED
        )

        // When & Then
        assertThatThrownBy {
            runBlocking { fakeAuthStrategy.authenticate() }
        }
            .isInstanceOf(UfcException::class.java)
            .hasFieldOrPropertyWithValue(
                "errorCode",
                ErrorCode.CRUMB_ACQUISITION_FAILED
            )
    }
}
```

---

### 2. FakeRateLimiter

#### 파일: `src/test/kotlin/com/ulalax/ufc/fake/FakeRateLimiter.kt`

#### 구현 이유
```
실제 TokenBucketRateLimiter:
  1. 시간 기반 토큰 리필 계산
  2. Mutex를 사용한 동시성 제어
  3. 토큰 부족 시 지연(delay) 수행
  4. 타임아웃 관리

문제점: 시간 경과 대기로 테스트 속도 저하
        타이밍 이슈로 인한 플레이키 테스트(flaky test)

Fake 버전:
  1. 설정 가능한 토큰 상태
  2. acquire() 호출 추적
  3. 지연 없이 즉시 반환
  4. 호출 시퀀스 검증
```

#### 코드 예제

```kotlin
class FakeRateLimiter(
    private val initialTokens: Int = 100,
    private val capacity: Int = 100
) : RateLimiter {

    // 상태 변수
    private var availableTokens = initialTokens
    private var totalTokensConsumed = 0
    private val acquireHistory = mutableListOf<AcquireCall>()

    // 제어 플래그
    private var shouldDelayAcquire = false
    private var delayMillis = 0L
    private var shouldFailAcquire = false

    data class AcquireCall(
        val timestamp: Long = System.currentTimeMillis(),
        val tokensRequested: Int,
        val succeeded: Boolean
    )

    // RateLimiter 구현
    override suspend fun acquire(tokensNeeded: Int) {
        if (shouldFailAcquire) {
            throw RateLimitException.RateLimitTimeoutException(
                source = "FAKE",
                config = RateLimitConfig(),
                tokensNeeded = tokensNeeded,
                waitedMillis = 0
            )
        }

        // 토큰 소비 (실제로는 상태만 변경)
        val succeeded = if (availableTokens >= tokensNeeded) {
            availableTokens -= tokensNeeded
            totalTokensConsumed += tokensNeeded
            true
        } else {
            // 실제 구현과 달리 즉시 실패 (데스트 용도)
            false
        }

        // 호출 기록
        acquireHistory.add(
            AcquireCall(
                tokensRequested = tokensNeeded,
                succeeded = succeeded
            )
        )

        // 선택적 지연
        if (shouldDelayAcquire) {
            delay(delayMillis)
        }

        if (!succeeded) {
            throw RateLimitException.RateLimitTimeoutException(
                source = "FAKE",
                config = RateLimitConfig(),
                tokensNeeded = tokensNeeded,
                waitedMillis = 0
            )
        }
    }

    override fun getAvailableTokens(): Int = availableTokens

    override fun getWaitTimeMillis(): Long {
        return if (availableTokens > 0) 0L else delayMillis
    }

    override fun getStatus(): RateLimiterStatus {
        return RateLimiterStatus(
            availableTokens = availableTokens,
            capacity = capacity,
            refillRate = 50,
            isEnabled = true,
            estimatedWaitTimeMs = getWaitTimeMillis()
        )
    }

    // 테스트 헬퍼
    fun getAcquireCallCount(): Int = acquireHistory.size

    fun getAcquireHistory(): List<AcquireCall> = acquireHistory.toList()

    fun getTotalTokensConsumed(): Int = totalTokensConsumed

    fun resetTokens(tokens: Int = initialTokens) {
        availableTokens = tokens
        totalTokensConsumed = 0
    }

    fun setSimulateDelay(enabled: Boolean, millis: Long = 100L) {
        shouldDelayAcquire = enabled
        delayMillis = millis
    }

    fun setSimulateFailure(enabled: Boolean) {
        shouldFailAcquire = enabled
    }

    fun reset() {
        availableTokens = initialTokens
        totalTokensConsumed = 0
        acquireHistory.clear()
        shouldDelayAcquire = false
        shouldFailAcquire = false
    }

    // 호출 패턴 검증
    fun verifyAcquireCalledWithTokens(tokens: Int): Boolean {
        return acquireHistory.any { it.tokensRequested == tokens }
    }

    fun verifySequentialAcquire(vararg tokenCounts: Int): Boolean {
        if (tokenCounts.size != acquireHistory.size) return false
        return tokenCounts.indices.all { i ->
            acquireHistory[i].tokensRequested == tokenCounts[i]
        }
    }
}
```

#### 테스트 사용 예시

```kotlin
@Test
@DisplayName("Rate Limiter의 acquire()가 호출되어야 한다")
fun shouldCallRateLimiterAcquire() = runBlocking {
    // Given
    val fakeRateLimiter = FakeRateLimiter()
    val service = YahooChartService(
        httpClient = fakeHttpClient,
        rateLimiter = fakeRateLimiter,
        authResult = authResult
    )

    // When
    service.getChartData("AAPL")
    service.getChartData("GOOGL")

    // Then: acquire()가 정확히 2번 호출됨
    assertThat(fakeRateLimiter.getAcquireCallCount()).isEqualTo(2)
    assertThat(fakeRateLimiter.getTotalTokensConsumed()).isEqualTo(2)
}

@Test
@DisplayName("토큰 부족 시 예외를 발생시킨다")
fun shouldThrowExceptionWhenTokensInsufficient() = runBlocking {
    // Given
    val fakeRateLimiter = FakeRateLimiter(initialTokens = 1, capacity = 1)

    // When & Then
    assertThatThrownBy {
        runBlocking {
            fakeRateLimiter.acquire(1) // OK
            fakeRateLimiter.acquire(1) // 실패
        }
    }
        .isInstanceOf(RateLimitException.RateLimitTimeoutException::class.java)
}
```

---

### 3. RecordedResponseRepository

#### 파일: `src/test/kotlin/com/ulalax/ufc/fake/RecordedResponseRepository.kt`

#### 구현 이유
```
테스트에서 필요한 HTTP 응답:
  1. 실제 API에서 레코딩한 JSON
  2. 다양한 시나리오별 응답
  3. 에러 응답
  4. 엣지 케이스 응답

문제점: 매번 응답을 생성하거나 네트워크 요청
해결: 파일에서 로드하여 캐싱

역할:
  1. src/liveTest/resources/responses/에서 JSON 로드
  2. 메모리 캐싱으로 성능 최적화
  3. URL 패턴 → 파일 경로 매핑
```

#### 코드 예제

```kotlin
class RecordedResponseRepository(
    private val basePath: Path = Paths.get("src/liveTest/resources/responses")
) {

    // 캐시: "category/fileName" -> JSON String
    private val responseCache = mutableMapOf<String, String>()

    // URL 패턴 매핑
    private val urlPatternMappings = mutableMapOf<String, String>()

    // 초기화
    init {
        setupDefaultMappings()
    }

    /**
     * 카테고리와 파일명으로 응답 로드
     * 예: loadResponse("yahoo/chart/daily", "aapl_daily_1m")
     *     -> src/liveTest/resources/responses/yahoo/chart/daily/aapl_daily_1m.json
     */
    fun loadResponse(category: String, fileName: String): String {
        val cacheKey = "$category/$fileName"

        return responseCache.getOrPut(cacheKey) {
            val filePath = basePath
                .resolve(category)
                .resolve("$fileName.json")

            if (!filePath.toFile().exists()) {
                throw FileNotFoundException(
                    "Response file not found: $filePath"
                )
            }

            filePath.toFile().readText()
        }
    }

    /**
     * URL 패턴으로 응답 조회
     * 예: getResponse("https://query1.finance.yahoo.com/v8/finance/chart/AAPL?...")
     *     -> 미리 매핑된 파일에서 로드
     */
    fun getResponse(url: String): String? {
        // URL에 매칭하는 패턴 찾기
        val pattern = urlPatternMappings.keys.find { pattern ->
            url.contains(pattern, ignoreCase = true)
        } ?: return null

        val mapping = urlPatternMappings[pattern] ?: return null
        val (category, fileName) = mapping.split("/").let {
            it[0] to it.drop(1).joinToString("/")
        }

        return try {
            loadResponse(category, fileName)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 전체 카테고리 미리 로드 (성능 최적화)
     */
    fun preloadCategory(category: String) {
        val categoryPath = basePath.resolve(category)

        if (!categoryPath.toFile().exists()) {
            return // 카테고리가 없으면 무시
        }

        categoryPath.toFile().walkBottomUp().forEach { file ->
            if (file.isFile && file.extension == "json") {
                val fileName = file.nameWithoutExtension
                val cacheKey = "$category/$fileName"

                if (!responseCache.containsKey(cacheKey)) {
                    responseCache[cacheKey] = file.readText()
                }
            }
        }
    }

    /**
     * URL 패턴 → 파일 경로 매핑 추가
     */
    fun addUrlMapping(
        pattern: String,  // "v8/finance/chart"
        category: String, // "yahoo/chart/daily"
        fileName: String  // "aapl_daily_1m"
    ) {
        urlPatternMappings[pattern] = "$category/$fileName"
    }

    /**
     * 캐시 초기화
     */
    fun clearCache() {
        responseCache.clear()
    }

    /**
     * 캐시된 응답 수 조회
     */
    fun getCachedResponseCount(): Int = responseCache.size

    /**
     * 캐시된 응답 목록 조회
     */
    fun getCachedResponseKeys(): Set<String> = responseCache.keys.toSet()

    /**
     * 특정 응답이 캐시되어 있는지 확인
     */
    fun isCached(category: String, fileName: String): Boolean {
        return responseCache.containsKey("$category/$fileName")
    }

    /**
     * 기본 URL 패턴 매핑 설정
     */
    private fun setupDefaultMappings() {
        // Chart API 패턴
        addUrlMapping(
            "v8/finance/chart",
            RecordingConfig.Paths.Yahoo.Chart.DAILY,
            "aapl_daily_1m"
        )

        // QuoteSummary API 패턴
        addUrlMapping(
            "v10/finance/quoteSummary",
            RecordingConfig.Paths.Yahoo.Quote.SUMMARY,
            "aapl_summary_client"
        )

        // 추가 패턴들...
    }
}
```

---

### 4. NoOpRateLimiter

#### 파일: `src/test/kotlin/com/ulalax/ufc/fake/NoOpRateLimiter.kt`

#### 구현 이유
```
FakeRateLimiter는 호출 추적 기능 제공
NoOpRateLimiter는 순수하게 Rate Limiting 무시

사용 시나리오:
  1. 단순 테스트 (호출 추적 불필요)
  2. 성능 벤치마크 (최소한의 오버헤드)
  3. 통합 테스트 (상태 검증 불필요)
```

#### 코드 예제

```kotlin
/**
 * Rate Limiting을 완전히 제거하는 No-Operation 구현
 * 모든 호출을 즉시 허용하며, 로깅도 없음
 */
class NoOpRateLimiter : RateLimiter {

    override suspend fun acquire(tokensNeeded: Int) {
        // 아무것도 하지 않음 (즉시 반환)
    }

    override fun getAvailableTokens(): Int = Int.MAX_VALUE

    override fun getWaitTimeMillis(): Long = 0L

    override fun getStatus(): RateLimiterStatus {
        return RateLimiterStatus(
            availableTokens = Int.MAX_VALUE,
            capacity = Int.MAX_VALUE,
            refillRate = Int.MAX_VALUE,
            isEnabled = false,
            estimatedWaitTimeMs = 0L
        )
    }
}
```

#### 테스트 사용 예시

```kotlin
// 단순한 로직 테스트 (Rate Limiting 무시)
@Test
fun shouldProcessChartDataCorrectly() = runBlocking {
    val service = YahooChartService(
        httpClient = fakeHttpClient,
        rateLimiter = NoOpRateLimiter(), // 로깅 불필요
        authResult = authResult
    )

    val result = service.getChartData("AAPL")
    assertThat(result).isNotEmpty()
}
```

---

### 5. FakeHttpClientBuilder

#### 파일: `src/test/kotlin/com/ulalax/ufc/fake/FakeHttpClientBuilder.kt`

#### 구현 이유
```
HttpClient 모의는 복잡함:
  1. 다양한 HTTP 메서드 지원 (GET, POST, ...)
  2. 요청 빌더 패턴
  3. 응답 처리 (body<T>(), status 등)

해결: RecordedResponseRepository와 함께 사용
      응답 데이터 주입하는 헬퍼
```

#### 코드 예제 (간소화)

```kotlin
/**
 * 테스트용 HttpClient를 구성하는 빌더
 *
 * 주의: 실제 Ktor HttpClient를 직접 모의하기보다
 * YahooChartService와 YahooQuoteSummaryService는
 * RecordedResponseRepository를 통한 응답 주입으로 테스트
 */
class FakeHttpClientBuilder {

    private val responseRepository = RecordedResponseRepository()
    private val requestLog = mutableListOf<String>()

    fun preloadChartResponses(): FakeHttpClientBuilder {
        responseRepository.preloadCategory(
            RecordingConfig.Paths.Yahoo.Chart.DAILY
        )
        responseRepository.preloadCategory(
            RecordingConfig.Paths.Yahoo.Chart.INTRADAY
        )
        return this
    }

    fun preloadQuoteResponses(): FakeHttpClientBuilder {
        responseRepository.preloadCategory(
            RecordingConfig.Paths.Yahoo.Quote.SUMMARY
        )
        return this
    }

    fun getResponseRepository(): RecordedResponseRepository {
        return responseRepository
    }

    fun getRequestLog(): List<String> = requestLog.toList()

    fun reset() {
        responseRepository.clearCache()
        requestLog.clear()
    }
}
```

---

## 통합 시나리오

### 시나리오 1: 정상적인 차트 데이터 조회

```kotlin
@DisplayName("YahooChartService - 정상 동작")
class YahooChartServiceIntegrationTest {

    private lateinit var fakeAuthStrategy: FakeAuthStrategy
    private lateinit var fakeRateLimiter: FakeRateLimiter
    private lateinit var responseRepository: RecordedResponseRepository

    @BeforeEach
    fun setUp() {
        fakeAuthStrategy = FakeAuthStrategy()
        fakeRateLimiter = FakeRateLimiter()
        responseRepository = RecordedResponseRepository()
        responseRepository.preloadCategory(
            RecordingConfig.Paths.Yahoo.Chart.DAILY
        )
    }

    @Test
    @DisplayName("AAPL 일일 데이터를 조회하고 검증한다")
    fun shouldFetchAndValidateAaplData() = runBlocking {
        // Given
        val authResult = fakeAuthStrategy.authenticate()
        val service = YahooChartService(
            httpClient = createMockHttpClient(), // RecordedResponseRepository 사용
            rateLimiter = fakeRateLimiter,
            authResult = authResult
        )

        // When
        val result = service.getChartData("AAPL", Interval.OneDay, Period.OneMonth)

        // Then: 데이터 검증
        assertThat(result).isNotEmpty()
        assertThat(result).allMatch { ohlcv ->
            ohlcv.timestamp > 0L &&
            ohlcv.close > 0.0 &&
            ohlcv.volume >= 0L
        }

        // Then: Fake 객체 상태 검증
        assertThat(fakeAuthStrategy.verifyCalledOnce()).isTrue()
        assertThat(fakeRateLimiter.getAcquireCallCount()).isGreaterThan(0)
    }
}
```

### 시나리오 2: 인증 실패 처리

```kotlin
@Test
@DisplayName("인증 실패 시 적절한 예외를 발생시킨다")
fun shouldHandleAuthenticationFailure() = runBlocking {
    // Given
    fakeAuthStrategy.setFailureMode(
        shouldFail = true,
        type = FakeAuthStrategy.FailureType.CRUMB_ACQUISITION_FAILED
    )

    // When & Then
    assertThatThrownBy {
        runBlocking { fakeAuthStrategy.authenticate() }
    }
        .isInstanceOf(UfcException::class.java)
        .hasMessageContaining("인증")
}
```

### 시나리오 3: Rate Limiting 시뮬레이션

```kotlin
@Test
@DisplayName("다중 요청 시 Rate Limiting이 적용된다")
fun shouldApplyRateLimitingOnMultipleRequests() = runBlocking {
    // Given
    val fakeRateLimiter = FakeRateLimiter(initialTokens = 2, capacity = 2)

    // When & Then: 3번째 요청에서 실패
    fakeRateLimiter.acquire(1) // OK
    fakeRateLimiter.acquire(1) // OK

    assertThatThrownBy {
        runBlocking { fakeRateLimiter.acquire(1) } // 실패
    }
        .isInstanceOf(RateLimitException.RateLimitTimeoutException::class.java)
}
```

---

## 테스트 픽스처

### Fixture 정의

```kotlin
// src/test/kotlin/com/ulalax/ufc/fixture/

object OHLCVDataFixture {
    fun default() = OHLCVData(
        timestamp = 1700000000L,
        open = 100.0,
        high = 105.0,
        low = 95.0,
        close = 102.0,
        volume = 1000000L,
        adjClose = 102.0
    )

    fun withTimestamp(timestamp: Long) = default().copy(timestamp = timestamp)

    fun withClose(close: Double) = default().copy(
        close = close,
        high = close * 1.05,
        low = close * 0.95
    )
}

object AuthResultFixture {
    fun default() = AuthResult(
        crumb = "FAKE_CRUMB_TOKEN_FOR_TESTING_1234567890abcdef",
        strategy = "fake"
    )

    fun withCrumb(crumb: String) = default().copy(crumb = crumb)
}

object ChartDataResponseFixture {
    fun default() = ChartDataResponse(
        chart = Chart(
            result = listOf(
                ChartResult(
                    meta = Meta(symbol = "AAPL"),
                    timestamp = listOf(1700000000L),
                    indicators = Indicators(
                        quote = listOf(
                            Quote(
                                open = listOf(100.0),
                                high = listOf(105.0),
                                low = listOf(95.0),
                                close = listOf(102.0),
                                volume = listOf(1000000L)
                            )
                        )
                    )
                )
            ),
            error = null
        )
    )
}
```

---

**작성자**: UFC Project Architecture Team
**버전**: 1.0
**마지막 업데이트**: 2025-12-02
