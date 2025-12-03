# UFC Implementation Steps - Phase 4-8 (Yahoo Finance 구현 및 테스트)

## 문서 정보
- **버전**: 1.0.0
- **최종 작성일**: 2025-12-02
- **이전 문서**: 11-haiku-implementation-steps.md (Phase 0-3)
- **대상**: Claude Haiku Model

---

## Phase 4: Yahoo Finance 인증 완성

### Step 4.1: AuthResult 및 AuthStrategy 인터페이스

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/internal/yahoo/auth/AuthResult.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.internal.yahoo.auth

/**
 * 인증 결과
 */
internal data class AuthResult(
    val crumb: String,
    val strategy: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 인증 유효성 확인 (1시간)
     */
    fun isValid(): Boolean {
        val elapsedMs = System.currentTimeMillis() - timestamp
        return elapsedMs < 3600_000 // 1 hour
    }
}

/**
 * 인증 전략 인터페이스
 */
internal interface AuthStrategy {
    suspend fun authenticate(): AuthResult
}
```

✅ **완료 조건:**
- AuthResult.kt 파일이 작성됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/internal/yahoo/auth/AuthResult.kt`

---

### Step 4.2: BasicAuthStrategy 구현

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/internal/yahoo/auth/BasicAuthStrategy.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.internal.yahoo.auth

import com.ulalax.ufc.exception.ErrorCode
import com.ulalax.ufc.exception.UFCException
import com.ulalax.ufc.internal.yahoo.YahooApiUrls
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

/**
 * Basic 인증 전략
 *
 * 1. fc.yahoo.com 방문하여 쿠키 획득
 * 2. getcrumb API로 crumb 획득
 */
internal class BasicAuthStrategy(
    private val httpClient: HttpClient
) : AuthStrategy {

    companion object {
        private const val COOKIE_URL = YahooApiUrls.FC
        private const val CRUMB_URL = YahooApiUrls.CRUMB
    }

    override suspend fun authenticate(): AuthResult {
        // Step 1: Cookie 획득
        httpClient.get(COOKIE_URL) {
            header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        }

        // Step 2: Crumb 획득
        val crumbResponse = httpClient.get(CRUMB_URL)
        val crumb = crumbResponse.bodyAsText().trim()

        // 검증
        if (crumb.isEmpty() || crumb.startsWith("<html>")) {
            throw UFCException(
                errorCode = ErrorCode.CRUMB_ACQUISITION_FAILED,
                message = "Failed to obtain crumb",
                metadata = mapOf("strategy" to "basic", "crumb" to crumb)
            )
        }

        return AuthResult(
            crumb = crumb,
            strategy = "basic"
        )
    }
}
```

✅ **완료 조건:**
- BasicAuthStrategy.kt 파일이 작성됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/internal/yahoo/auth/BasicAuthStrategy.kt`

---

### Step 4.3: YahooAuthenticator 구현

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/internal/yahoo/auth/YahooAuthenticator.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.internal.yahoo.auth

import com.ulalax.ufc.exception.ErrorCode
import com.ulalax.ufc.exception.UFCException
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory

/**
 * Yahoo Finance 인증 관리자
 */
internal class YahooAuthenticator(
    private val httpClient: HttpClient
) {
    private val logger = LoggerFactory.getLogger(YahooAuthenticator::class.java)
    private val lock = Mutex()

    @Volatile
    private var authResult: AuthResult? = null

    private val basicStrategy = BasicAuthStrategy(httpClient)

    /**
     * 인증 수행
     */
    suspend fun authenticate(): AuthResult {
        lock.withLock {
            // 이미 인증된 경우 재사용
            authResult?.let {
                if (it.isValid()) {
                    return it
                }
            }

            // Basic 전략 시도
            try {
                authResult = basicStrategy.authenticate()
                logger.info("Yahoo Finance authenticated successfully")
                return authResult!!
            } catch (e: UFCException) {
                logger.error("Authentication failed", e)
                throw UFCException(
                    errorCode = ErrorCode.AUTH_FAILED,
                    message = "All authentication strategies failed",
                    cause = e
                )
            }
        }
    }

    /**
     * Crumb 가져오기
     */
    suspend fun getCrumb(): String {
        val result = authResult ?: authenticate()
        return result.crumb
    }

    /**
     * HTTP 요청에 인증 적용
     */
    suspend fun applyAuth(builder: HttpRequestBuilder) {
        val result = authResult ?: authenticate()
        builder.parameter("crumb", result.crumb)
    }

    /**
     * 인증 리셋 (에러 발생 시)
     */
    suspend fun reset() {
        lock.withLock {
            authResult = null
        }
    }
}
```

✅ **완료 조건:**
- YahooAuthenticator.kt 파일이 작성됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/internal/yahoo/auth/YahooAuthenticator.kt`

---

## Phase 5: Yahoo Finance HTTP Client

### Step 5.1: YahooHttpClientFactory

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/internal/yahoo/YahooHttpClientFactory.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.internal.yahoo

import com.ulalax.ufc.infrastructure.http.UserAgents
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import okhttp3.ConnectionSpec
import java.util.concurrent.TimeUnit

/**
 * Yahoo Finance HTTP 클라이언트 팩토리
 */
internal object YahooHttpClientFactory {

    fun create(): HttpClient {
        return HttpClient(OkHttp) {
            // OkHttp 엔진 설정
            engine {
                config {
                    // Connection specs
                    connectionSpecs(
                        listOf(
                            ConnectionSpec.MODERN_TLS,
                            ConnectionSpec.COMPATIBLE_TLS
                        )
                    )

                    // Connection pool
                    connectionPool(
                        okhttp3.ConnectionPool(
                            maxIdleConnections = 5,
                            keepAliveDuration = 5,
                            TimeUnit.MINUTES
                        )
                    )

                    // Timeouts
                    connectTimeout(30, TimeUnit.SECONDS)
                    readTimeout(30, TimeUnit.SECONDS)
                    writeTimeout(30, TimeUnit.SECONDS)

                    // Follow redirects
                    followRedirects(true)
                    followSslRedirects(true)
                }
            }

            // Cookie 관리
            install(HttpCookies)

            // HTTP Timeout
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }

            // JSON 직렬화
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                    prettyPrint = false
                })
            }

            // 로깅
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }

            // 기본 헤더 설정
            install(DefaultRequest) {
                // User-Agent - Chrome 브라우저 흉내
                header("User-Agent", UserAgents.random())

                // 브라우저 헤더들
                header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                header("Accept-Language", "en-US,en;q=0.9")
                header("Accept-Encoding", "gzip, deflate, br")
                header("Connection", "keep-alive")
            }

            // 재시도 로직
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 3)
                exponentialDelay()
                retryOnException(maxRetries = 3, retryOnTimeout = true)
            }
        }
    }
}
```

✅ **완료 조건:**
- YahooHttpClientFactory.kt 파일이 작성됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/internal/yahoo/YahooHttpClientFactory.kt`

---

### Step 5.2: 간단한 UFCClient 빌더 (임시)

**작업 내용:**
- 테스트용 간단한 UFCClient 빌더 작성
- 나중에 완전한 구현으로 대체 예정

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/client/UFCClient.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.client

import com.ulalax.ufc.infrastructure.ratelimit.RateLimitingSettings
import io.ktor.client.*

/**
 * UFC 클라이언트 설정
 */
data class UFCClientConfig(
    val fredApiKey: String? = null,
    val rateLimitingSettings: RateLimitingSettings = RateLimitingSettings()
)

/**
 * UFC 통합 클라이언트 (임시 구현)
 *
 * TODO: Phase 7에서 완전한 구현으로 대체
 */
class UFCClient private constructor(
    private val httpClient: HttpClient,
    private val config: UFCClientConfig
) : AutoCloseable {

    companion object {
        suspend fun create(
            config: UFCClientConfig = UFCClientConfig()
        ): UFCClient {
            // TODO: 완전한 초기화 로직 구현
            val httpClient = HttpClient()
            return UFCClient(httpClient, config)
        }
    }

    override fun close() {
        httpClient.close()
    }
}
```

✅ **완료 조건:**
- UFCClient.kt 임시 파일이 작성됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/client/UFCClient.kt`

---

## Phase 6: 테스트 인프라 구성

### Step 6.1: TestSymbols 작성

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/liveTest/kotlin/com/ulalax/ufc/utils/TestSymbols.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.utils

/**
 * 테스트에서 사용하는 심볼 상수
 */
object TestSymbols {
    // ETFs
    const val SPY = "SPY"           // SPDR S&P 500 ETF Trust
    const val QQQ = "QQQ"           // Invesco QQQ Trust
    const val IWM = "IWM"           // iShares Russell 2000 ETF
    const val VTI = "VTI"           // Vanguard Total Stock Market ETF
    const val AGG = "AGG"           // iShares Core U.S. Aggregate Bond ETF

    // Stocks
    const val AAPL = "AAPL"         // Apple Inc.
    const val MSFT = "MSFT"         // Microsoft Corporation
    const val GOOGL = "GOOGL"       // Alphabet Inc.
    const val AMZN = "AMZN"         // Amazon.com Inc.
    const val NVDA = "NVDA"         // NVIDIA Corporation

    // FRED Series IDs
    const val GDP = "GDPC1"                 // Real GDP
    const val UNEMPLOYMENT = "UNRATE"       // Unemployment Rate
    const val CPI = "CPIAUCSL"              // Consumer Price Index
    const val FEDERAL_FUNDS_RATE = "DFF"    // Federal Funds Rate
    const val TREASURY_10Y = "DGS10"        // 10-Year Treasury Rate
}
```

✅ **완료 조건:**
- TestSymbols.kt 파일이 작성됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/liveTest/kotlin/com/ulalax/ufc/utils/TestSymbols.kt`

---

### Step 6.2: RecordingConfig 작성

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/liveTest/kotlin/com/ulalax/ufc/utils/RecordingConfig.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.utils

import java.nio.file.Path
import kotlin.io.path.Path

/**
 * 레코딩 설정 및 경로 관리
 */
object RecordingConfig {
    /**
     * 레코딩 활성화 여부
     */
    val isRecordingEnabled: Boolean
        get() = System.getProperty("record.responses", "true").toBoolean()

    /**
     * 레코딩 파일 저장 경로
     */
    val baseOutputPath: Path = Path("src/liveTest/resources/responses")

    /**
     * API별 레코딩 경로 상수
     */
    object Paths {
        object Yahoo {
            object Etf {
                const val TOP_HOLDINGS = "yahoo/etf/top_holdings"
                const val SECTOR_WEIGHTINGS = "yahoo/etf/sector_weightings"
                const val ASSET_ALLOCATION = "yahoo/etf/asset_allocation"
                const val FUND_PROFILE = "yahoo/etf/fund_profile"
                const val EQUITY_HOLDINGS = "yahoo/etf/equity_holdings"
                const val BOND_HOLDINGS = "yahoo/etf/bond_holdings"
            }

            object Ticker {
                const val HISTORY = "yahoo/ticker/history"
                const val DIVIDENDS = "yahoo/ticker/dividends"
                const val SPLITS = "yahoo/ticker/splits"
                const val FINANCIALS = "yahoo/ticker/financials"
                const val INFO = "yahoo/ticker/info"
                const val RECOMMENDATIONS = "yahoo/ticker/recommendations"
            }

            object Chart {
                const val INTRADAY = "yahoo/chart/intraday"
                const val DAILY = "yahoo/chart/daily"
                const val ADJUSTED = "yahoo/chart/adjusted"
            }

            object Search {
                const val BASIC = "yahoo/search/basic"
                const val SCREENER = "yahoo/search/screener"
            }
        }

        object Fred {
            const val SERIES = "fred/series"
            const val SERIES_INFO = "fred/series_info"
            const val VINTAGE = "fred/vintage"
            const val SEARCH = "fred/search"
            const val SEARCH_BY_CATEGORY = "fred/search/category"
            const val SEARCH_BY_RELEASE = "fred/search/release"
            const val CATEGORY = "fred/category"
            const val INDICATORS = "fred/indicators"
        }
    }
}
```

✅ **완료 조건:**
- RecordingConfig.kt 파일이 작성됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/liveTest/kotlin/com/ulalax/ufc/utils/RecordingConfig.kt`

---

### Step 6.3: ResponseRecorder 작성

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/liveTest/kotlin/com/ulalax/ufc/utils/ResponseRecorder.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSerializer
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate

/**
 * API 응답을 JSON 파일로 저장하는 유틸리티
 */
object ResponseRecorder {
    const val MAX_RECORD_SIZE = 10_000

    @PublishedApi
    internal val gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(LocalDate::class.java, JsonSerializer<LocalDate> { src, _, _ ->
            com.google.gson.JsonPrimitive(src.toString())
        })
        .registerTypeAdapter(BigDecimal::class.java, JsonSerializer<BigDecimal> { src, _, _ ->
            com.google.gson.JsonPrimitive(src.toPlainString())
        })
        .create()

    /**
     * 객체를 JSON 파일로 저장
     */
    inline fun <reified T> record(data: T, category: String, fileName: String) {
        if (!RecordingConfig.isRecordingEnabled) return

        val outputDir = RecordingConfig.baseOutputPath.resolve(category)
        Files.createDirectories(outputDir)

        val outputFile = outputDir.resolve("$fileName.json")
        val jsonString = gson.toJson(data)
        Files.writeString(outputFile, jsonString)

        println("✅ Recorded: $outputFile")
    }

    /**
     * 리스트 데이터를 JSON 파일로 저장
     */
    inline fun <reified T> recordList(data: List<T>, category: String, fileName: String) {
        if (!RecordingConfig.isRecordingEnabled) return

        if (data.isEmpty()) {
            println("⚠️ 경고: $category/$fileName 에 레코딩할 데이터가 없습니다.")
            return
        }

        val recordData = if (data.size > MAX_RECORD_SIZE) {
            println("⚠️ 데이터가 너무 큽니다 (${data.size}개). 처음 $MAX_RECORD_SIZE 개만 레코딩합니다.")
            data.take(MAX_RECORD_SIZE)
        } else {
            data
        }

        record(recordData, category, fileName)
    }
}
```

✅ **완료 조건:**
- ResponseRecorder.kt 파일이 작성됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/liveTest/kotlin/com/ulalax/ufc/utils/ResponseRecorder.kt`

---

### Step 6.4: LiveTestBase 작성

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/liveTest/kotlin/com/ulalax/ufc/utils/LiveTestBase.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.utils

import com.ulalax.ufc.client.UFCClient
import com.ulalax.ufc.client.UFCClientConfig
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.util.Properties
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Live Test의 공통 베이스 클래스
 */
@Tag("live")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class LiveTestBase {

    protected lateinit var client: UFCClient

    @BeforeAll
    fun setUp() = runTest {
        val fredApiKey = loadFredApiKey()

        client = if (fredApiKey != null) {
            UFCClient.create(
                config = UFCClientConfig(fredApiKey = fredApiKey)
            )
        } else {
            println("ℹ️  FRED_API_KEY가 설정되지 않았습니다. FRED API 테스트는 skip됩니다.")
            UFCClient.create()
        }

        println("🚀 Live Test 시작 - Recording: ${RecordingConfig.isRecordingEnabled}")
    }

    @AfterAll
    fun tearDown() {
        if (::client.isInitialized) {
            client.close()
            println("🏁 Live Test 종료")
        }
    }

    /**
     * FRED API 키를 local.properties에서 로드
     */
    private fun loadFredApiKey(): String? {
        val localPropertiesFile = File("local.properties")
        if (localPropertiesFile.exists()) {
            val properties = Properties()
            localPropertiesFile.inputStream().use { properties.load(it) }
            return properties.getProperty("FRED_API_KEY")
        }
        return null
    }

    /**
     * 테스트 실행 헬퍼 (타임아웃 설정)
     */
    protected fun liveTest(
        timeout: Duration = 30.seconds,
        block: suspend () -> Unit
    ) = runTest(timeout = timeout) {
        block()
    }
}
```

✅ **완료 조건:**
- LiveTestBase.kt 파일이 작성됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/liveTest/kotlin/com/ulalax/ufc/utils/LiveTestBase.kt`

---

## Phase 7: 첫 번째 Live Test 작성

### Step 7.1: Yahoo Finance 인증 Live Test

**작업 내용:**
- Yahoo Finance 인증이 정상 작동하는지 확인하는 Live Test 작성
- 실제 API 호출을 수행하여 Cookie/Crumb 획득 확인

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/liveTest/kotlin/com/ulalax/ufc/live/yahoo/YahooAuthLiveTest.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.live.yahoo

import com.ulalax.ufc.internal.yahoo.YahooHttpClientFactory
import com.ulalax.ufc.internal.yahoo.auth.YahooAuthenticator
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Yahoo Finance 인증 Live Test
 *
 * ## 목적
 * - Yahoo Finance Cookie/Crumb 인증 메커니즘 검증
 * - Basic Auth Strategy 동작 확인
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew liveTest --tests "YahooAuthLiveTest"
 * ```
 */
@Tag("live")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class YahooAuthLiveTest {

    private val httpClient = YahooHttpClientFactory.create()
    private val authenticator = YahooAuthenticator(httpClient)

    @AfterAll
    fun tearDown() {
        httpClient.close()
    }

    @Test
    @DisplayName("Yahoo Finance 인증을 성공적으로 수행할 수 있다")
    fun testAuthentication() = runTest {
        // When: 인증 수행
        val authResult = authenticator.authenticate()

        // Then: Crumb이 정상적으로 발급되어야 함
        assertNotNull(authResult, "AuthResult should not be null")
        assertNotNull(authResult.crumb, "Crumb should not be null")
        assertFalse(authResult.crumb.isEmpty(), "Crumb should not be empty")
        assertFalse(authResult.crumb.startsWith("<html>"), "Crumb should not be HTML")
        assertEquals("basic", authResult.strategy, "Strategy should be 'basic'")

        println("✅ Yahoo Finance 인증 성공")
        println("   - Strategy: ${authResult.strategy}")
        println("   - Crumb: ${authResult.crumb.take(20)}...")
        println("   - Valid: ${authResult.isValid()}")
    }

    @Test
    @DisplayName("Crumb을 가져올 수 있다")
    fun testGetCrumb() = runTest {
        // When: Crumb 가져오기
        val crumb = authenticator.getCrumb()

        // Then: Crumb이 정상적으로 반환되어야 함
        assertNotNull(crumb)
        assertFalse(crumb.isEmpty())

        println("✅ Crumb 획득 성공: ${crumb.take(20)}...")
    }

    @Test
    @DisplayName("인증 결과가 1시간 동안 유효하다")
    fun testAuthValidityDuration() = runTest {
        // Given: 인증 수행
        val authResult = authenticator.authenticate()

        // Then: 방금 발급받은 인증은 유효해야 함
        assertTrue(authResult.isValid(), "Newly acquired auth should be valid")

        println("✅ 인증 유효성 확인: ${authResult.isValid()}")
    }
}
```

✅ **완료 조건:**
- YahooAuthLiveTest.kt 파일이 작성됨

🧪 **테스트 실행:**
```bash
# Live Test 실행
./gradlew liveTest --tests "YahooAuthLiveTest"
```

**예상 결과:**
- 3개의 테스트가 모두 통과해야 함
- Console에 Crumb 값이 출력되어야 함

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/liveTest/kotlin/com/ulalax/ufc/live/yahoo/YahooAuthLiveTest.kt`

---

## Phase 8: 첫 번째 체크포인트

### Step 8.1: 전체 빌드 및 테스트 실행

**작업 내용:**
- 지금까지 작성한 모든 코드를 빌드
- Live Test 실행하여 인증 정상 동작 확인

**실행 명령:**
```bash
# 1. Clean build
./gradlew clean build

# 2. Live Test 실행
./gradlew liveTest

# 3. 결과 확인
echo "✅ Phase 0-7 완료!"
echo "✅ 다음 Phase: Yahoo Finance Chart API 구현"
```

✅ **완료 조건:**
- Build 성공
- Live Test 모두 통과
- 에러 메시지 없음

🧪 **검증 항목:**
1. ErrorCode, UFCException이 정상 작동
2. RateLimiter가 정상 작동
3. Yahoo Finance 인증이 성공
4. Crumb이 정상적으로 발급됨

📝 **산출물:**
- Phase 0-7까지의 모든 파일
- Live Test 실행 결과

---

## 다음 단계

Phase 8까지 완료하면 다음 문서로 진행:
- **13-haiku-implementation-steps-phase9-15.md** (Yahoo Finance Chart API, ETF, FRED 구현)

---

## 트러블슈팅

### 빌드 실패 시
```bash
# Gradle 캐시 클리어
./gradlew clean

# 의존성 재다운로드
./gradlew build --refresh-dependencies
```

### Live Test 실패 시
1. 인터넷 연결 확인
2. Yahoo Finance 서비스 상태 확인 (https://finance.yahoo.com)
3. Crumb 응답이 HTML인 경우: Rate Limiting 가능성 → 잠시 대기 후 재시도

---

**다음 문서**: 13-haiku-implementation-steps-phase9-15.md
**현재 진행률**: Phase 0-8 완료 (약 35%)
