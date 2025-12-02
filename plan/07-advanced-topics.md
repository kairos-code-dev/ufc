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

### 6.1 Circuit Breaker

```kotlin
class CircuitBreaker(
    private val failureThreshold: Int = 5,
    private val resetTimeout: Long = 60000
) {
    private var failureCount = 0
    private var lastFailureTime = 0L
    private var state = State.CLOSED
    
    enum class State { CLOSED, OPEN, HALF_OPEN }
    
    suspend fun <T> execute(block: suspend () -> T): T {
        when (state) {
            State.OPEN -> {
                if (System.currentTimeMillis() - lastFailureTime > resetTimeout) {
                    state = State.HALF_OPEN
                } else {
                    throw UFCException(ErrorCode.SERVICE_UNAVAILABLE)
                }
            }
            State.HALF_OPEN -> {}
            State.CLOSED -> {}
        }
        
        return try {
            val result = block()
            onSuccess()
            result
        } catch (e: Exception) {
            onFailure()
            throw e
        }
    }
    
    private fun onSuccess() {
        failureCount = 0
        state = State.CLOSED
    }
    
    private fun onFailure() {
        failureCount++
        lastFailureTime = System.currentTimeMillis()
        
        if (failureCount >= failureThreshold) {
            state = State.OPEN
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
