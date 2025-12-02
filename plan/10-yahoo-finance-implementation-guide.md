# Yahoo Finance Implementation Guide - 실제 구현 세부사항

## 문서 정보
- **버전**: 1.0.0
- **최종 수정일**: 2025-12-02
- **작성자**: Claude Code (Software Architect)
- **문서 상태**: Active
- **참조**: yfinance 소스코드 분석 (/home/ulalax/project/kairos/yfinance)

---

## 목차
1. [중요 전제사항](#1-중요-전제사항)
2. [HTTP 클라이언트 설정](#2-http-클라이언트-설정)
3. [Cookie/Crumb 인증 메커니즘](#3-cookiecrumb-인증-메커니즘)
4. [API 엔드포인트](#4-api-엔드포인트)
5. [에러 처리](#5-에러-처리)
6. [Rate Limiting](#6-rate-limiting)
7. [구현 체크리스트](#7-구현-체크리스트)

---

## 1. 중요 전제사항

### 1.1 Yahoo Finance는 공식 API가 아닙니다

**핵심 사실:**
- Yahoo Finance는 공식 API를 제공하지 않습니다
- 실제로는 **웹 스크래핑**입니다
- 언제든지 변경될 수 있으며, 차단될 수 있습니다
- TLS Fingerprinting을 통해 봇을 감지합니다

**yfinance의 접근 방식:**
- **curl_cffi** 라이브러리 사용 (TLS fingerprinting 우회)
- Chrome 브라우저를 **impersonate** (흉내 냄)
- User-Agent와 HTTP 헤더를 실제 브라우저처럼 설정

### 1.2 Kotlin/JVM 구현 전략

Python의 `curl_cffi`는 libcurl의 cffi 바인딩이므로, Kotlin/JVM에서는 다음 전략을 사용합니다:

**Option 1: Ktor Client with TLS Configuration (권장)**
- Ktor Client + OkHttp 엔진
- 실제 브라우저와 유사한 TLS 설정
- User-Agent 및 헤더 커스터마이징

**Option 2: OkHttp with Custom TLS**
- OkHttp 직접 사용
- TLS 1.3, Cipher Suites 커스터마이징
- Connection Pooling

**Option 3: Apache HttpClient 5**
- HTTP/2 지원
- TLS 설정 가능

---

## 2. HTTP 클라이언트 설정

### 2.1 필수 설정

yfinance 소스 분석 결과:

```python
# yfinance/base.py:83
self.session = session or requests.Session(impersonate="chrome")
```

**Kotlin 구현:**

```kotlin
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import okhttp3.ConnectionSpec
import okhttp3.TlsVersion
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * Yahoo Finance HTTP 클라이언트 팩토리
 *
 * TLS Fingerprinting 우회를 위한 브라우저 유사 설정
 */
object YahooHttpClientFactory {

    fun create(config: YahooHttpConfig = YahooHttpConfig()): HttpClient {
        return HttpClient(OkHttp) {
            // OkHttp 엔진 설정
            engine {
                // TLS 설정 - Chrome 브라우저 흉내
                config {
                    // Connection specs
                    connectionSpecs(
                        listOf(
                            ConnectionSpec.MODERN_TLS,
                            ConnectionSpec.COMPATIBLE_TLS
                        )
                    )

                    // TLS 버전
                    sslSocketFactory(
                        createSSLSocketFactory(),
                        createTrustManager()
                    )

                    // 호스트 검증 비활성화 (선택적)
                    if (config.disableHostnameVerification) {
                        hostnameVerifier { _, _ -> true }
                    }

                    // Connection pool
                    connectionPool(
                        okhttp3.ConnectionPool(
                            maxIdleConnections = 5,
                            keepAliveDuration = 5,
                            java.util.concurrent.TimeUnit.MINUTES
                        )
                    )

                    // Timeouts
                    connectTimeout(config.connectTimeout, java.util.concurrent.TimeUnit.MILLISECONDS)
                    readTimeout(config.readTimeout, java.util.concurrent.TimeUnit.MILLISECONDS)
                    writeTimeout(config.writeTimeout, java.util.concurrent.TimeUnit.MILLISECONDS)

                    // Follow redirects
                    followRedirects(true)
                    followSslRedirects(true)
                }
            }

            // HTTP/2 지원
            install(HttpTimeout) {
                requestTimeoutMillis = config.requestTimeout
                connectTimeoutMillis = config.connectTimeout
                socketTimeoutMillis = config.readTimeout
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
                level = if (config.debug) LogLevel.ALL else LogLevel.INFO
            }

            // 기본 헤더 설정
            install(DefaultRequest) {
                // User-Agent - Chrome 브라우저 흉내
                header("User-Agent", selectRandomUserAgent())

                // 브라우저 헤더들
                header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                header("Accept-Language", "en-US,en;q=0.9")
                header("Accept-Encoding", "gzip, deflate, br")
                header("Connection", "keep-alive")
                header("Upgrade-Insecure-Requests", "1")
                header("Sec-Fetch-Dest", "document")
                header("Sec-Fetch-Mode", "navigate")
                header("Sec-Fetch-Site", "none")
                header("Sec-Fetch-User", "?1")
                header("Cache-Control", "max-age=0")
            }

            // 재시도 로직
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = config.maxRetries)
                exponentialDelay()
                retryOnException(maxRetries = config.maxRetries, retryOnTimeout = true)

                modifyRequest { request ->
                    // 재시도 시 User-Agent 변경
                    request.headers["User-Agent"] = selectRandomUserAgent()
                }
            }
        }
    }

    /**
     * SSLSocketFactory 생성 (TLS 1.3 지원)
     */
    private fun createSSLSocketFactory(): SSLSocketFactory {
        val trustManager = createTrustManager()
        val sslContext = SSLContext.getInstance("TLSv1.3")
        sslContext.init(null, arrayOf(trustManager), SecureRandom())
        return sslContext.socketFactory
    }

    /**
     * TrustManager 생성 (모든 인증서 신뢰 - 개발용)
     * 프로덕션에서는 적절한 인증서 검증 필요
     */
    private fun createTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }

    /**
     * 랜덤 User-Agent 선택
     * yfinance/const.py:703-719의 USER_AGENTS 참조
     */
    private fun selectRandomUserAgent(): String {
        val userAgents = listOf(
            // Chrome
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",

            // Firefox
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14.7; rv:135.0) Gecko/20100101 Firefox/135.0",
            "Mozilla/5.0 (X11; Linux i686; rv:135.0) Gecko/20100101 Firefox/135.0",

            // Safari
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_7_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.3 Safari/605.1.15",

            // Edge
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36 Edg/131.0.2903.86"
        )
        return userAgents.random()
    }
}

/**
 * Yahoo HTTP 설정
 */
data class YahooHttpConfig(
    val connectTimeout: Long = 30_000,
    val readTimeout: Long = 30_000,
    val writeTimeout: Long = 30_000,
    val requestTimeout: Long = 60_000,
    val maxRetries: Int = 3,
    val disableHostnameVerification: Boolean = false,
    val debug: Boolean = false
)
```

### 2.2 User-Agent 목록

yfinance `const.py:703-719`에서 추출한 User-Agent 리스트:

```kotlin
object UserAgents {
    val CHROME = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"
    )

    val FIREFOX = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14.7; rv:135.0) Gecko/20100101 Firefox/135.0",
        "Mozilla/5.0 (X11; Linux i686; rv:135.0) Gecko/20100101 Firefox/135.0"
    )

    val SAFARI = listOf(
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_7_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.3 Safari/605.1.15"
    )

    val EDGE = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36 Edg/131.0.2903.86"
    )

    val ALL = CHROME + FIREFOX + SAFARI + EDGE

    fun random(): String = ALL.random()
}
```

---

## 3. Cookie/Crumb 인증 메커니즘

### 3.1 인증 개요

Yahoo Finance는 두 가지 값이 필요합니다:
1. **Cookie**: 세션 쿠키
2. **Crumb**: CSRF 토큰

yfinance 소스 분석:

```python
# yfinance/data.py:69-75
def __init__(self, session=None, proxy=None):
    self._crumb = None
    self._cookie = None

    # Default to using 'basic' strategy
    self._cookie_strategy = 'basic'
    # If it fails, then fallback method is 'csrf'
```

### 3.2 Strategy 1: Basic (권장)

**절차:**
1. `https://fc.yahoo.com` 방문 → Cookie 획득
2. `https://query1.finance.yahoo.com/v1/test/getcrumb` 호출 → Crumb 획득

**Kotlin 구현:**

```kotlin
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
        private const val COOKIE_URL = "https://fc.yahoo.com"
        private const val CRUMB_URL = "https://query1.finance.yahoo.com/v1/test/getcrumb"
    }

    override suspend fun authenticate(): AuthResult {
        // Step 1: Cookie 획득
        val cookieResponse = httpClient.get(COOKIE_URL) {
            // 브라우저처럼 요청
            header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        }

        // Cookie는 HttpClient가 자동으로 저장

        // Step 2: Crumb 획득
        val crumbResponse = httpClient.get(CRUMB_URL)
        val crumb = crumbResponse.bodyAsText().trim()

        // 검증
        if (crumb.isEmpty() || crumb.startsWith("<html>")) {
            throw UFCException(
                errorCode = ErrorCode.AUTH_FAILED,
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

### 3.3 Strategy 2: CSRF (Fallback)

Basic 전략이 실패하면 CSRF 전략 사용:

**절차:**
1. `https://guce.yahoo.com` 방문
2. Consent 폼 파싱
3. CSRF 토큰 추출
4. Consent 폼 제출
5. Crumb 획득

**Kotlin 구현:**

```kotlin
/**
 * CSRF 인증 전략 (Fallback)
 *
 * GDPR consent 페이지를 통한 인증
 */
internal class CsrfAuthStrategy(
    private val httpClient: HttpClient
) : AuthStrategy {

    companion object {
        private const val CONSENT_URL = "https://guce.yahoo.com/consent"
        private const val CRUMB_URL = "https://query1.finance.yahoo.com/v1/test/getcrumb"
    }

    override suspend fun authenticate(): AuthResult {
        // Step 1: Consent 페이지 방문
        val consentResponse = httpClient.get(CONSENT_URL) {
            parameter("done", "https://finance.yahoo.com")
            parameter("lang", "en-US")
        }

        val consentHtml = consentResponse.bodyAsText()

        // Step 2: CSRF 토큰 및 폼 데이터 파싱
        val (csrfToken, formData) = parseConsentForm(consentHtml)

        // Step 3: Consent 폼 제출
        val submitResponse = httpClient.post(consentResponse.request.url.toString()) {
            header("Referer", consentResponse.request.url.toString())
            setBody(formData)
        }

        // Step 4: Crumb 획득
        val crumbResponse = httpClient.get(CRUMB_URL)
        val crumb = crumbResponse.bodyAsText().trim()

        if (crumb.isEmpty() || crumb.startsWith("<html>")) {
            throw UFCException(
                errorCode = ErrorCode.AUTH_FAILED,
                message = "Failed to obtain crumb via CSRF",
                metadata = mapOf("strategy" to "csrf")
            )
        }

        return AuthResult(
            crumb = crumb,
            strategy = "csrf"
        )
    }

    private fun parseConsentForm(html: String): Pair<String, Map<String, String>> {
        val soup = Jsoup.parse(html)

        // CSRF 토큰 추출
        val csrfToken = soup.select("input[name=csrfToken]").attr("value")

        // 모든 hidden input 추출
        val formData = soup.select("form input[type=hidden]")
            .associate { it.attr("name") to it.attr("value") }
            .toMutableMap()

        // Agree 버튼 추가
        formData["agree"] = "agree"

        return csrfToken to formData
    }
}
```

### 3.4 Authenticator 구현

```kotlin
/**
 * Yahoo Finance 인증 관리자
 *
 * Singleton으로 구현하여 Cookie/Crumb 재사용
 */
internal class YahooAuthenticator(
    private val httpClient: HttpClient
) {
    private val lock = Mutex()

    @Volatile
    private var authResult: AuthResult? = null

    @Volatile
    private var currentStrategy: AuthStrategy = BasicAuthStrategy(httpClient)

    private val basicStrategy = BasicAuthStrategy(httpClient)
    private val csrfStrategy = CsrfAuthStrategy(httpClient)

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
                currentStrategy = basicStrategy
                return authResult!!
            } catch (e: UFCException) {
                logger.warn("Basic auth failed, trying CSRF strategy", e)
            }

            // CSRF 전략 시도 (Fallback)
            try {
                authResult = csrfStrategy.authenticate()
                currentStrategy = csrfStrategy
                return authResult!!
            } catch (e: UFCException) {
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

        // Crumb은 쿼리 파라미터로 추가
        builder.parameter("crumb", result.crumb)

        // Cookie는 HttpClient가 자동으로 관리
    }

    /**
     * 인증 리셋 (에러 발생 시)
     */
    suspend fun reset() {
        lock.withLock {
            authResult = null
            // 쿠키 클리어
            // HttpClient의 쿠키 저장소를 초기화해야 함
        }
    }
}

/**
 * 인증 결과
 */
data class AuthResult(
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

---

## 4. API 엔드포인트

### 4.1 Base URLs

yfinance `const.py:1-3`:

```kotlin
object YahooApiUrls {
    const val QUERY1 = "https://query1.finance.yahoo.com"
    const val QUERY2 = "https://query2.finance.yahoo.com"
    const val ROOT = "https://finance.yahoo.com"
    const val FC = "https://fc.yahoo.com"

    // API Endpoints
    const val CHART = "$QUERY2/v8/finance/chart"
    const val QUOTE_SUMMARY = "$QUERY2/v10/finance/quoteSummary"
    const val CRUMB = "$QUERY1/v1/test/getcrumb"
    const val SEARCH = "$QUERY1/v1/finance/search"
    const val SCREENER = "$QUERY1/v1/finance/screener"
}
```

### 4.2 Chart API

**URL:**
```
GET https://query2.finance.yahoo.com/v8/finance/chart/{symbol}
```

**필수 파라미터:**
- `period1`: Unix timestamp (시작일)
- `period2`: Unix timestamp (종료일)
- `interval`: `1m`, `2m`, `5m`, `15m`, `30m`, `1h`, `1d`, `5d`, `1wk`, `1mo`, `3mo`
- `events`: `div,splits` (배당 + 분할)
- `includeAdjustedClose`: `true`
- `crumb`: 인증 토큰

**예시:**
```kotlin
suspend fun fetchChart(symbol: String, params: ChartParams): ChartResponse {
    val url = "${YahooApiUrls.CHART}/$symbol"

    return httpClient.get(url) {
        parameter("period1", params.period1)
        parameter("period2", params.period2)
        parameter("interval", params.interval.value)
        parameter("events", "div,splits")
        parameter("includeAdjustedClose", "true")
        authenticator.applyAuth(this)
    }.body()
}
```

### 4.3 QuoteSummary API

**URL:**
```
GET https://query2.finance.yahoo.com/v10/finance/quoteSummary/{symbol}
```

**필수 파라미터:**
- `modules`: 콤마로 구분된 모듈 리스트
- `crumb`: 인증 토큰

**사용 가능한 모듈** (yfinance `const.py:126-160`):

```kotlin
object QuoteSummaryModules {
    const val SUMMARY_PROFILE = "summaryProfile"
    const val SUMMARY_DETAIL = "summaryDetail"
    const val ASSET_PROFILE = "assetProfile"
    const val FUND_PROFILE = "fundProfile"
    const val PRICE = "price"
    const val QUOTE_TYPE = "quoteType"
    const val ESG_SCORES = "esgScores"

    // Financials
    const val INCOME_STATEMENT_HISTORY = "incomeStatementHistory"
    const val INCOME_STATEMENT_HISTORY_QUARTERLY = "incomeStatementHistoryQuarterly"
    const val BALANCE_SHEET_HISTORY = "balanceSheetHistory"
    const val BALANCE_SHEET_HISTORY_QUARTERLY = "balanceSheetHistoryQuarterly"
    const val CASH_FLOW_STATEMENT_HISTORY = "cashFlowStatementHistory"
    const val CASH_FLOW_STATEMENT_HISTORY_QUARTERLY = "cashFlowStatementHistoryQuarterly"

    // Key Stats
    const val DEFAULT_KEY_STATISTICS = "defaultKeyStatistics"
    const val FINANCIAL_DATA = "financialData"
    const val CALENDAR_EVENTS = "calendarEvents"

    // ETF specific
    const val TOP_HOLDINGS = "topHoldings"
    const val FUND_OWNERSHIP = "fundOwnership"

    // Holders
    const val INSTITUTION_OWNERSHIP = "institutionOwnership"
    const val MAJOR_HOLDERS_BREAKDOWN = "majorHoldersBreakdown"
    const val INSIDER_HOLDERS = "insiderHolders"
    const val INSIDER_TRANSACTIONS = "insiderTransactions"

    // Analysis
    const val RECOMMENDATION_TREND = "recommendationTrend"
    const val EARNINGS = "earnings"
    const val EARNINGS_HISTORY = "earningsHistory"
    const val EARNINGS_TREND = "earningsTrend"
    const val UPGRADE_DOWNGRADE_HISTORY = "upgradeDowngradeHistory"

    val ALL = listOf(
        SUMMARY_PROFILE, SUMMARY_DETAIL, ASSET_PROFILE, FUND_PROFILE,
        PRICE, QUOTE_TYPE, ESG_SCORES,
        INCOME_STATEMENT_HISTORY, INCOME_STATEMENT_HISTORY_QUARTERLY,
        BALANCE_SHEET_HISTORY, BALANCE_SHEET_HISTORY_QUARTERLY,
        CASH_FLOW_STATEMENT_HISTORY, CASH_FLOW_STATEMENT_HISTORY_QUARTERLY,
        DEFAULT_KEY_STATISTICS, FINANCIAL_DATA, CALENDAR_EVENTS,
        TOP_HOLDINGS, FUND_OWNERSHIP,
        INSTITUTION_OWNERSHIP, MAJOR_HOLDERS_BREAKDOWN,
        INSIDER_HOLDERS, INSIDER_TRANSACTIONS,
        RECOMMENDATION_TREND, EARNINGS, EARNINGS_HISTORY,
        EARNINGS_TREND, UPGRADE_DOWNGRADE_HISTORY
    )
}
```

**예시:**
```kotlin
suspend fun fetchQuoteSummary(
    symbol: String,
    modules: List<String>
): QuoteSummaryResponse {
    val url = "${YahooApiUrls.QUOTE_SUMMARY}/$symbol"

    return httpClient.get(url) {
        parameter("modules", modules.joinToString(","))
        authenticator.applyAuth(this)
    }.body()
}
```

---

## 5. 에러 처리

### 5.1 Rate Limiting

Yahoo Finance는 Rate Limiting을 적용합니다:

```kotlin
/**
 * Rate Limit 에러 감지 및 처리
 */
fun handleRateLimitError(response: HttpResponse) {
    if (response.status.value == 429) {
        val retryAfter = response.headers["Retry-After"]?.toLongOrNull() ?: 60

        throw UFCException(
            errorCode = ErrorCode.RATE_LIMITED,
            message = "Rate limit exceeded",
            metadata = mapOf(
                "retryAfter" to retryAfter,
                "url" to response.request.url.toString()
            )
        )
    }
}
```

### 5.2 Crumb 만료

Crumb이 만료되면 401 Unauthorized:

```kotlin
/**
 * Crumb 만료 처리
 */
suspend fun <T> withCrumbRetry(
    maxRetries: Int = 2,
    block: suspend () -> T
): T {
    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: UFCException) {
            if (e.errorCode == ErrorCode.AUTH_FAILED && attempt < maxRetries - 1) {
                // Crumb 갱신
                authenticator.reset()
                authenticator.authenticate()
            } else {
                throw e
            }
        }
    }
    throw UFCException(ErrorCode.UNKNOWN_ERROR)
}
```

---

## 6. Rate Limiting

### 6.1 Rate Limiter 구현

```kotlin
/**
 * Simple Token Bucket Rate Limiter
 */
class TokenBucketRateLimiter(
    private val capacity: Int,
    private val refillRate: Long, // tokens per second
) {
    private val tokens = AtomicInteger(capacity)
    private val lastRefillTime = AtomicLong(System.currentTimeMillis())

    suspend fun acquire() {
        while (true) {
            refill()

            if (tokens.get() > 0 && tokens.decrementAndGet() >= 0) {
                return
            }

            // Wait before retry
            delay(100)
        }
    }

    private fun refill() {
        val now = System.currentTimeMillis()
        val last = lastRefillTime.get()
        val elapsed = (now - last) / 1000.0
        val tokensToAdd = (elapsed * refillRate).toInt()

        if (tokensToAdd > 0 && lastRefillTime.compareAndSet(last, now)) {
            val current = tokens.get()
            val newTokens = minOf(current + tokensToAdd, capacity)
            tokens.set(newTokens)
        }
    }
}

// Usage
val rateLimiter = TokenBucketRateLimiter(
    capacity = 100,
    refillRate = 50 // 50 requests per second
)

suspend fun fetchData() {
    rateLimiter.acquire()
    // Make API call
}
```

---

## 7. 구현 체크리스트

### 7.1 HTTP 클라이언트

- [ ] Ktor Client + OkHttp 엔진 설정
- [ ] TLS 1.3 지원
- [ ] User-Agent 랜덤 선택
- [ ] 브라우저 헤더 설정 (Accept, Accept-Language, etc.)
- [ ] Connection Pooling
- [ ] Cookie 자동 관리
- [ ] Redirect 자동 처리

### 7.2 인증

- [ ] Basic Auth Strategy 구현
- [ ] CSRF Auth Strategy 구현 (Fallback)
- [ ] Cookie 저장 및 재사용
- [ ] Crumb 캐싱 (1시간 TTL)
- [ ] Crumb 만료 시 자동 갱신
- [ ] Thread-safe 구현 (Mutex)

### 7.3 API 호출

- [ ] Chart API 구현
- [ ] QuoteSummary API 구현
- [ ] 모든 모듈 지원
- [ ] Crumb 파라미터 자동 추가
- [ ] 에러 응답 파싱

### 7.4 에러 처리

- [ ] Rate Limit 감지 (429)
- [ ] Auth 실패 감지 (401)
- [ ] Retry 로직 (Exponential Backoff)
- [ ] ErrorCode 매핑
- [ ] Metadata 수집

### 7.5 Rate Limiting

- [ ] Token Bucket 구현
- [ ] 요청 전 토큰 획득
- [ ] 재시도 시 Delay
- [ ] 동시성 제어

---

## 8. 참고 자료

- **yfinance 소스코드**: `/home/ulalax/project/kairos/yfinance`
- **const.py**: User-Agent, 모듈 리스트, URL
- **data.py**: 인증 로직, Cookie/Crumb 관리
- **base.py**: Session 설정, curl_cffi 사용

---

**다음 문서**: [03-yahoo-finance-core.md](./03-yahoo-finance-core.md)
**최종 수정일**: 2025-12-02
