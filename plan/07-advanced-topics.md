# Advanced Topics - 고급 주제

## 문서 정보
- **버전**: 2.0.0
- **최종 수정일**: 2025-12-01
- **작성자**: Claude Code (Software Architect)
- **문서 상태**: Active

---

## 1. Virtual Threads 활용

### 1.1 JDK 21 Virtual Threads

**개요:**
- JDK 21의 Virtual Threads를 활용한 고성능 병렬 처리
- 수천 개의 동시 요청 처리 가능

**구현:**
```kotlin
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher

val virtualThreadDispatcher = Executors.newVirtualThreadPerTaskExecutor()
    .asCoroutineDispatcher()

suspend fun fetchMultipleSymbols(symbols: List<String>): List<OHLCV> {
    return withContext(virtualThreadDispatcher) {
        symbols.map { symbol ->
            async {
                ufc.yahoo.ticker(symbol).history(Period.OneYear)
            }
        }.awaitAll()
    }
}
```

---

## 2. 캐싱 전략

### 2.1 LRU Cache

```kotlin
class LRUCache<K, V>(
    private val maxSize: Int,
    private val ttl: Long? = null
) {
    private val cache = object : LinkedHashMap<K, CacheEntry<V>>(
        maxSize + 1,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, CacheEntry<V>>?): Boolean {
            return size > maxSize
        }
    }

    private val mutex = Mutex()

    suspend fun get(key: K): V? = mutex.withLock {
        val entry = cache[key] ?: return null
        
        if (ttl != null && entry.isExpired(ttl)) {
            cache.remove(key)
            return null
        }
        
        entry.value
    }

    suspend fun put(key: K, value: V) = mutex.withLock {
        cache[key] = CacheEntry(value, System.currentTimeMillis())
    }
}

data class CacheEntry<V>(
    val value: V,
    val timestamp: Long
) {
    fun isExpired(ttl: Long): Boolean {
        return System.currentTimeMillis() - timestamp > ttl
    }
}
```

### 2.2 캐싱 레벨

| 레벨 | 대상 | TTL | 설명 |
|------|------|-----|------|
| L1 | HTTP Response | 없음 | Ktor Content-Encoding |
| L2 | DataFrame | 5분-1일 | LRU Cache |
| L3 | User | 사용자 정의 | Application Cache |

---

## 3. Rate Limiting

### 3.1 Token Bucket 알고리즘

```kotlin
class TokenBucketRateLimiter(
    private val capacity: Int,
    private val refillRate: Int,
    private val refillPeriodMs: Long = 1000
) {
    private var tokens = capacity
    private var lastRefillTime = System.currentTimeMillis()
    private val mutex = Mutex()

    suspend fun acquire() = mutex.withLock {
        refill()
        
        while (tokens <= 0) {
            delay(refillPeriodMs / refillRate)
            refill()
        }
        
        tokens--
    }

    private fun refill() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRefillTime
        val tokensToAdd = (elapsed / refillPeriodMs * refillRate).toInt()
        
        if (tokensToAdd > 0) {
            tokens = (tokens + tokensToAdd).coerceAtMost(capacity)
            lastRefillTime = now
        }
    }
}
```

---

## 4. TLS Fingerprinting 회피

### 4.1 User-Agent 로테이션

```kotlin
object UserAgentPool {
    private val userAgents = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0"
    )
    
    fun random(): String = userAgents.random()
}
```

### 4.2 TLS 1.3 설정

```kotlin
HttpClient(CIO) {
    engine {
        https {
            trustManager = TrustAllX509TrustManager()
            serverName = "query2.finance.yahoo.com"
        }
    }
}
```

---

## 5. 병렬 처리 패턴

### 5.1 Fan-Out / Fan-In

```kotlin
suspend fun fetchMultipleEconomicIndicators(): MacroData {
    return coroutineScope {
        val gdp = async { ufc.fred.getSeries("GDPC1") }
        val unemployment = async { ufc.fred.getSeries("UNRATE") }
        val cpi = async { ufc.fred.getSeries("CPIAUCSL") }
        val fedRate = async { ufc.fred.getSeries("DFF") }
        
        MacroData(
            gdp = gdp.await(),
            unemployment = unemployment.await(),
            cpi = cpi.await(),
            fedRate = fedRate.await()
        )
    }
}
```

### 5.2 Chunked 처리

```kotlin
suspend fun fetchLargeSymbolList(
    symbols: List<String>,
    chunkSize: Int = 50
): List<OHLCV> {
    return symbols.chunked(chunkSize).flatMap { chunk ->
        chunk.map { symbol ->
            async {
                delay(100) // Rate limiting
                ufc.yahoo.ticker(symbol).history(Period.OneYear)
            }
        }.awaitAll()
    }
}
```

---

## 6. 에러 복구 전략

### 6.1 Circuit Breaker 패턴

Circuit Breaker는 외부 서비스 장애 시 빠른 실패(fail-fast)를 통해 시스템 전체의 안정성을 보장하는 패턴입니다.

**상태 전이:**
```
CLOSED (정상) --[실패 임계치 초과]--> OPEN (차단)
OPEN (차단) --[타임아웃 경과]--> HALF_OPEN (테스트)
HALF_OPEN (테스트) --[성공]--> CLOSED (정상)
HALF_OPEN (테스트) --[실패]--> OPEN (차단)
```

### 6.2 Circuit Breaker 구현

```kotlin
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Circuit Breaker 구현
 *
 * 외부 서비스 호출 실패를 모니터링하고, 실패율이 임계치를 초과하면
 * 자동으로 서비스 호출을 차단하여 시스템을 보호합니다.
 *
 * @param failureThreshold 실패 임계치 (이 횟수만큼 연속 실패 시 OPEN)
 * @param resetTimeoutMs 리셋 타임아웃 (OPEN 상태에서 이 시간 후 HALF_OPEN으로 전환)
 * @param halfOpenMaxAttempts HALF_OPEN 상태에서 허용할 테스트 시도 횟수
 * @param name Circuit Breaker 이름 (로깅용)
 */
class CircuitBreaker(
    private val failureThreshold: Int = 5,
    private val resetTimeoutMs: Long = 60000,
    private val halfOpenMaxAttempts: Int = 3,
    private val name: String = "default"
) {
    private val logger = LoggerFactory.getLogger(CircuitBreaker::class.java)

    // Thread-safe 상태 관리
    private val failureCount = AtomicInteger(0)
    private val successCount = AtomicInteger(0)
    private val lastFailureTime = AtomicLong(0L)
    private val lastStateChangeTime = AtomicLong(System.currentTimeMillis())

    @Volatile
    private var state = State.CLOSED

    private val mutex = Mutex()

    /**
     * Circuit Breaker 상태
     */
    enum class State {
        /**
         * CLOSED: 정상 상태. 모든 요청을 처리합니다.
         */
        CLOSED,

        /**
         * OPEN: 차단 상태. 모든 요청을 즉시 거부합니다.
         * 일정 시간 후 HALF_OPEN으로 전환됩니다.
         */
        OPEN,

        /**
         * HALF_OPEN: 테스트 상태. 제한된 수의 요청만 허용하여
         * 서비스 복구 여부를 테스트합니다.
         */
        HALF_OPEN
    }

    /**
     * Circuit Breaker를 통해 작업 실행
     *
     * @param block 실행할 작업
     * @return 작업 결과
     * @throws CircuitBreakerOpenException Circuit이 OPEN 상태일 때
     * @throws Exception block에서 발생한 예외
     */
    suspend fun <T> execute(block: suspend () -> T): T {
        // 1. 현재 상태 확인 및 전이
        checkAndTransitionState()

        // 2. 상태에 따른 처리
        when (state) {
            State.OPEN -> {
                logger.warn("[$name] Circuit breaker is OPEN, rejecting request")
                throw CircuitBreakerOpenException(
                    "Circuit breaker [$name] is OPEN. Service is currently unavailable."
                )
            }

            State.HALF_OPEN -> {
                // HALF_OPEN 상태에서는 제한된 수의 요청만 허용
                if (successCount.get() >= halfOpenMaxAttempts) {
                    logger.warn("[$name] Circuit breaker HALF_OPEN limit reached, rejecting request")
                    throw CircuitBreakerOpenException(
                        "Circuit breaker [$name] is in HALF_OPEN state with max attempts reached."
                    )
                }
            }

            State.CLOSED -> {
                // CLOSED 상태에서는 모든 요청 허용
            }
        }

        // 3. 작업 실행 및 결과 처리
        return try {
            val result = block()
            onSuccess()
            result
        } catch (e: Exception) {
            onFailure(e)
            throw e
        }
    }

    /**
     * 상태 확인 및 전이
     */
    private suspend fun checkAndTransitionState() = mutex.withLock {
        when (state) {
            State.OPEN -> {
                // OPEN 상태에서 타임아웃이 지났는지 확인
                val elapsedTime = System.currentTimeMillis() - lastFailureTime.get()
                if (elapsedTime >= resetTimeoutMs) {
                    transitionTo(State.HALF_OPEN)
                    successCount.set(0)
                    failureCount.set(0)
                    logger.info("[$name] Transitioned to HALF_OPEN after ${elapsedTime}ms")
                }
            }

            State.HALF_OPEN -> {
                // HALF_OPEN은 별도의 자동 전이 없음
            }

            State.CLOSED -> {
                // CLOSED는 별도의 자동 전이 없음
            }
        }
    }

    /**
     * 성공 처리
     */
    private suspend fun onSuccess() = mutex.withLock {
        when (state) {
            State.HALF_OPEN -> {
                successCount.incrementAndGet()
                logger.debug("[$name] Success in HALF_OPEN (${successCount.get()}/$halfOpenMaxAttempts)")

                // HALF_OPEN에서 충분한 성공이 있으면 CLOSED로 전환
                if (successCount.get() >= halfOpenMaxAttempts) {
                    transitionTo(State.CLOSED)
                    failureCount.set(0)
                    logger.info("[$name] Transitioned to CLOSED after successful test")
                }
            }

            State.CLOSED -> {
                // 성공 시 실패 카운트 초기화
                if (failureCount.get() > 0) {
                    failureCount.set(0)
                    logger.debug("[$name] Reset failure count to 0")
                }
            }

            State.OPEN -> {
                // OPEN 상태에서는 성공이 발생하지 않아야 함 (예외적 상황)
                logger.warn("[$name] Unexpected success in OPEN state")
            }
        }
    }

    /**
     * 실패 처리
     */
    private suspend fun onFailure(exception: Exception) = mutex.withLock {
        lastFailureTime.set(System.currentTimeMillis())

        when (state) {
            State.HALF_OPEN -> {
                // HALF_OPEN에서 실패하면 즉시 OPEN으로 전환
                transitionTo(State.OPEN)
                logger.warn("[$name] Failed in HALF_OPEN, transitioned back to OPEN", exception)
            }

            State.CLOSED -> {
                // CLOSED에서 실패 카운트 증가
                val currentFailures = failureCount.incrementAndGet()
                logger.debug("[$name] Failure count: $currentFailures/$failureThreshold")

                // 임계치 초과 시 OPEN으로 전환
                if (currentFailures >= failureThreshold) {
                    transitionTo(State.OPEN)
                    logger.error("[$name] Failure threshold reached, transitioned to OPEN", exception)
                }
            }

            State.OPEN -> {
                // OPEN 상태에서는 실패가 발생하지 않아야 함 (요청 자체가 차단됨)
                logger.warn("[$name] Unexpected failure in OPEN state", exception)
            }
        }
    }

    /**
     * 상태 전이
     */
    private fun transitionTo(newState: State) {
        val oldState = state
        state = newState
        lastStateChangeTime.set(System.currentTimeMillis())
        logger.info("[$name] State transition: $oldState -> $newState")
    }

    /**
     * 현재 상태 조회
     */
    fun getState(): State = state

    /**
     * 통계 정보 조회
     */
    fun getStats(): CircuitBreakerStats {
        return CircuitBreakerStats(
            state = state,
            failureCount = failureCount.get(),
            successCount = successCount.get(),
            lastFailureTime = lastFailureTime.get(),
            lastStateChangeTime = lastStateChangeTime.get(),
            timeSinceLastStateChange = System.currentTimeMillis() - lastStateChangeTime.get()
        )
    }

    /**
     * 상태 강제 초기화 (테스트용)
     */
    suspend fun reset() = mutex.withLock {
        failureCount.set(0)
        successCount.set(0)
        lastFailureTime.set(0L)
        transitionTo(State.CLOSED)
        logger.info("[$name] Circuit breaker reset to CLOSED")
    }
}

/**
 * Circuit Breaker 통계 정보
 */
data class CircuitBreakerStats(
    val state: CircuitBreaker.State,
    val failureCount: Int,
    val successCount: Int,
    val lastFailureTime: Long,
    val lastStateChangeTime: Long,
    val timeSinceLastStateChange: Long
)

/**
 * Circuit Breaker가 OPEN 상태일 때 발생하는 예외
 */
class CircuitBreakerOpenException(message: String) : Exception(message)
```

### 6.3 사용 예시

```kotlin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

suspend fun main() {
    val circuitBreaker = CircuitBreaker(
        failureThreshold = 3,
        resetTimeoutMs = 5000,
        halfOpenMaxAttempts = 2,
        name = "YahooFinanceAPI"
    )

    // 정상 호출
    repeat(5) { i ->
        try {
            val result = circuitBreaker.execute {
                fetchDataFromApi()
            }
            println("[$i] Success: $result")
        } catch (e: Exception) {
            println("[$i] Failed: ${e.message}")
        }
    }

    // 실패하는 호출 (Circuit이 OPEN으로 전환됨)
    repeat(5) { i ->
        try {
            val result = circuitBreaker.execute {
                throw RuntimeException("API Error")
            }
            println("[$i] Success: $result")
        } catch (e: CircuitBreakerOpenException) {
            println("[$i] Circuit OPEN: ${e.message}")
        } catch (e: Exception) {
            println("[$i] Failed: ${e.message}")
        }
    }

    // 상태 확인
    val stats = circuitBreaker.getStats()
    println("State: ${stats.state}")
    println("Failure Count: ${stats.failureCount}")

    // 5초 대기 (resetTimeout)
    println("Waiting for reset timeout...")
    delay(5100)

    // HALF_OPEN 상태에서 복구 테스트
    repeat(3) { i ->
        try {
            val result = circuitBreaker.execute {
                fetchDataFromApi()
            }
            println("[$i] Recovery Success: $result")
        } catch (e: Exception) {
            println("[$i] Recovery Failed: ${e.message}")
        }
    }

    println("Final State: ${circuitBreaker.getState()}")
}

suspend fun fetchDataFromApi(): String {
    // 실제 API 호출 시뮬레이션
    delay(100)
    return "Data from API"
}
```

### 6.4 YahooFinanceSource에 Circuit Breaker 통합

```kotlin
internal class YahooFinanceSourceImpl(
    private val httpClient: HttpClient,
    private val cache: LRUCache<String, Any>,
    private val config: YahooConfig
) : YahooFinanceSource {

    private val circuitBreaker = CircuitBreaker(
        failureThreshold = 5,
        resetTimeoutMs = 60000,
        halfOpenMaxAttempts = 3,
        name = "YahooFinanceAPI"
    )

    override suspend fun fetchChart(
        symbol: String,
        params: ChartParams
    ): ChartResponse {
        return circuitBreaker.execute {
            val url = buildChartUrl(symbol, params)
            httpClient.get(url).body()
        }
    }

    override suspend fun fetchQuoteSummary(
        symbol: String,
        modules: List<String>
    ): QuoteSummaryResponse {
        return circuitBreaker.execute {
            val url = buildQuoteSummaryUrl(symbol, modules)
            httpClient.get(url).body()
        }
    }
}
```

---

## 7. 데이터 분석 통합

### 7.1 Kotlin DataFrame (선택 사항)

UFC는 데이터 수집 전용 라이브러리이며, 분석 기능은 포함하지 않습니다.
데이터 분석이 필요한 경우 다음 라이브러리를 추가하세요:

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.jetbrains.kotlinx:dataframe:0.14.1")
}
```

**사용 예시:**
```kotlin
// PriceBar 리스트를 DataFrame으로 변환
val history = ticker.history(Period.ONE_YEAR)
val df = dataFrameOf(
    "date" to history.map { it.date },
    "open" to history.map { it.open },
    "high" to history.map { it.high },
    "low" to history.map { it.low },
    "close" to history.map { it.close },
    "volume" to history.map { it.volume }
)

// 이동 평균 계산
val ma20 = df.add("ma20") {
    close.rolling(20).mean()
}
```

### 7.2 Python 연동

```kotlin
// UFC로 데이터 수집
val history = ticker.history(Period.ONE_YEAR)

// JSON 직렬화
val json = Json.encodeToString(history)
File("price_data.json").writeText(json)

// Python에서 분석
// import pandas as pd
// df = pd.read_json("price_data.json")
// df['ma20'] = df['close'].rolling(20).mean()
```

### 7.3 표준 Kotlin 컬렉션

```kotlin
val history = ticker.history(Period.ONE_YEAR)

// 평균 계산
val avgClose = history.mapNotNull { it.close }.average()

// 최댓값/최솟값
val highestPrice = history.mapNotNull { it.high }.maxOrNull()
val lowestPrice = history.mapNotNull { it.low }.minOrNull()

// 필터링
val highVolumeDays = history.filter { (it.volume ?: 0) > 1_000_000 }

// 변화율 계산
val returns = history.zipWithNext { prev, curr ->
    val prevClose = prev.close ?: return@zipWithNext null
    val currClose = curr.close ?: return@zipWithNext null
    (currClose - prevClose) / prevClose * 100
}.filterNotNull()
```

---

## 8. 로깅 및 모니터링

### 8.1 SLF4J 로깅

```kotlin
private val logger = LoggerFactory.getLogger(YahooFinanceSource::class.java)

suspend fun fetchChart(symbol: String): ChartResponse {
    logger.info("Fetching chart data for symbol: $symbol")
    
    return try {
        val response = httpClient.get(url)
        logger.debug("Response status: ${response.status}")
        response.body()
    } catch (e: UFCException) {
        logger.error("Failed to fetch chart data", e)
        throw e
    }
}
```

### 8.2 메트릭 수집

```kotlin
object Metrics {
    private val requestCount = AtomicLong()
    private val errorCount = AtomicLong()
    private val requestDurations = ConcurrentHashMap<String, Long>()
    
    fun recordRequest(endpoint: String, duration: Long) {
        requestCount.incrementAndGet()
        requestDurations[endpoint] = duration
    }
    
    fun recordError(errorCode: ErrorCode) {
        errorCount.incrementAndGet()
    }
    
    fun getStats(): MetricStats {
        return MetricStats(
            totalRequests = requestCount.get(),
            totalErrors = errorCount.get(),
            avgDuration = requestDurations.values.average()
        )
    }
}
```

---

## 9. 테스트 전략

### 9.1 Mock Server

```kotlin
@Test
fun testChartAPI() = runTest {
    val mockEngine = MockEngine { request ->
        respond(
            content = """{"chart":{"result":[...]}}""",
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }
    
    val client = YahooFinanceClient(
        httpClient = HttpClient(mockEngine),
        authenticator = mockAuthenticator
    )
    
    val response = client.fetchChart("AAPL", ChartParams())
    assertNotNull(response)
}
```

---

## 10. 성능 최적화

### 10.1 연결 풀링

```kotlin
HttpClient(CIO) {
    engine {
        maxConnectionsCount = 100
        endpoint {
            maxConnectionsPerRoute = 10
            pipelineMaxSize = 20
            keepAliveTime = 5000
            connectTimeout = 5000
            connectAttempts = 3
        }
    }
}
```

### 10.2 압축

```kotlin
install(ContentEncoding) {
    gzip()
    deflate()
}
```

---

## 11. 디버깅 가이드

### 11.1 HTTP 요청 로깅

```kotlin
install(Logging) {
    logger = Logger.DEFAULT
    level = LogLevel.ALL
    filter { request ->
        request.url.host.contains("yahoo.com")
    }
}
```

### 11.2 에러 디버깅

```kotlin
try {
    val data = fetchData()
} catch (e: UFCException) {
    logger.error("""
        ErrorCode: ${e.errorCode}
        Message: ${e.message}
        Metadata: ${e.metadata}
        Cause: ${e.cause}
    """.trimIndent(), e)
}
```

---

## 12. 참고 자료

- **Ktor Documentation**: https://ktor.io/docs/
- **Kotlin Coroutines**: https://kotlinlang.org/docs/coroutines-overview.html
- **Kotlin DataFrame**: https://kotlin.github.io/dataframe/
- **JDK 21 Virtual Threads**: https://openjdk.org/jeps/444

---

**문서 완료**
