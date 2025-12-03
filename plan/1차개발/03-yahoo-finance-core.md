# Yahoo Finance Core - HTTP 클라이언트 및 인증

## 문서 정보
- **버전**: 2.0.0
- **최종 수정일**: 2025-12-01
- **작성자**: Claude Code (Software Architect)
- **문서 상태**: Active

---

## 1. YahooFinanceSource 인터페이스

```kotlin
/**
 * Yahoo Finance 데이터 소스
 */
interface YahooFinanceSource : DataSource {
    override val name: String get() = "YahooFinance"

    fun ticker(symbol: String): Ticker
    fun etf(symbol: String): ETF
    suspend fun search(query: String): List<SearchResult>
    suspend fun screener(screener: ScreenerQuery): List<ScreenerResult>
}
```

---

## 2. Cookie/Crumb 인증

### 2.1 인증 전략

Yahoo Finance는 두 가지 인증 방식을 지원합니다:

1. **Basic Strategy**: fc.yahoo.com → getcrumb
2. **CSRF Strategy**: consent.yahoo.com → CSRF 토큰 → getcrumb

### 2.2 Authenticator 인터페이스

```kotlin
/**
 * Yahoo Finance 인증 처리
 */
internal interface Authenticator {
    suspend fun authenticate()
    fun getCrumb(): String
    fun applyCookies(request: HttpRequestBuilder)
    fun isAuthenticated(): Boolean
    fun reset()
}
```

### 2.3 BasicAuthStrategy

```kotlin
internal class BasicAuthStrategy(
    private val httpClient: HttpClient
) {
    suspend fun authenticate(): AuthResult {
        // 1. Cookie 획득
        val cookieResponse = httpClient.get("https://fc.yahoo.com")

        // 2. Crumb 획득
        val crumbResponse = httpClient.get("https://query1.finance.yahoo.com/v1/test/getcrumb")
        val crumb = crumbResponse.bodyAsText()

        if (crumb.isEmpty() || crumb.startsWith("<html>")) {
            throw UFCException(ErrorCode.CRUMB_ACQUISITION_FAILED)
        }

        return AuthResult(crumb = crumb)
    }
}
```

---

## 3. HTTP 클라이언트

### 3.1 YahooFinanceClient

```kotlin
/**
 * Yahoo Finance HTTP 클라이언트
 */
internal class YahooFinanceClient(
    private val httpClient: HttpClient,
    private val authenticator: Authenticator
) {

    companion object {
        private const val CHART_BASE_URL = "https://query2.finance.yahoo.com/v8/finance/chart"
        private const val QUOTE_SUMMARY_URL = "https://query2.finance.yahoo.com/v10/finance/quoteSummary"
    }

    suspend fun fetchChart(
        symbol: String,
        params: ChartParams
    ): ChartResponse {
        return withRetry {
            httpClient.get("$CHART_BASE_URL/$symbol") {
                authenticator.applyCookies(this)
                params.applyTo(this)
            }.body()
        }
    }

    suspend fun fetchQuoteSummary(
        symbol: String,
        modules: List<String>
    ): QuoteSummaryResponse {
        return withRetry {
            httpClient.get("$QUOTE_SUMMARY_URL/$symbol") {
                authenticator.applyCookies(this)
                parameter("modules", modules.joinToString(","))
                parameter("crumb", authenticator.getCrumb())
            }.body()
        }
    }
}
```

---

## 4. Chart API

### 4.1 ChartParams

```kotlin
data class ChartParams(
    val period1: Long? = null,
    val period2: Long? = null,
    val interval: Interval = Interval.OneDay,
    val includeAdjustedClose: Boolean = true,
    val events: String = "div,splits"
) {
    fun applyTo(builder: HttpRequestBuilder) {
        period1?.let { builder.parameter("period1", it) }
        period2?.let { builder.parameter("period2", it) }
        builder.parameter("interval", interval.value)
        builder.parameter("events", events)
        if (includeAdjustedClose) {
            builder.parameter("includeAdjustedClose", "true")
        }
    }
}
```

### 4.2 ChartResponse

```kotlin
@Serializable
data class ChartResponse(
    val chart: Chart
) {
    @Serializable
    data class Chart(
        val result: List<Result>?,
        val error: Error?
    ) {
        fun getResultOrThrow(): Result {
            error?.let {
                throw UFCException(
                    errorCode = ErrorCode.NO_DATA_AVAILABLE,
                    metadata = mapOf("error" to it.description)
                )
            }
            return result?.firstOrNull() ?: throw UFCException(ErrorCode.EMPTY_RESPONSE)
        }
    }

    @Serializable
    data class Result(
        val meta: Meta,
        val timestamp: List<Long>,
        val indicators: Indicators,
        val events: Events? = null
    )
}
```

---

## 5. QuoteSummary API

### 5.1 사용 가능한 모듈

| 모듈 | 설명 |
|------|------|
| assetProfile | 기업 프로필 |
| summaryDetail | 요약 상세 |
| summaryProfile | 요약 프로필 |
| financialData | 재무 데이터 |
| quoteType | Quote 타입 |
| defaultKeyStatistics | 기본 통계 |
| calendarEvents | 이벤트 일정 |
| incomeStatementHistory | 손익계산서 (연간) |
| incomeStatementHistoryQuarterly | 손익계산서 (분기) |
| balanceSheetHistory | 재무상태표 (연간) |
| balanceSheetHistoryQuarterly | 재무상태표 (분기) |
| cashflowStatementHistory | 현금흐름표 (연간) |
| cashflowStatementHistoryQuarterly | 현금흐름표 (분기) |
| recommendationTrend | 애널리스트 추천 |
| institutionOwnership | 기관 보유 |
| fundOwnership | 펀드 보유 |
| majorHoldersBreakdown | 주요 주주 |
| insiderHolders | 내부자 보유 |
| topHoldings | 상위 보유 종목 (ETF) |
| fundProfile | 펀드 프로필 (ETF) |
| price | 가격 정보 |
| earnings | 실적 정보 |
| earningsHistory | 실적 이력 |
| earningsTrend | 실적 추세 |
| upgradeDowngradeHistory | 등급 변경 이력 |
| esgScores | ESG 점수 |

---

## 6. 재시도 및 에러 처리

### 6.1 재시도 로직

```kotlin
suspend fun <T> withRetry(
    maxRetries: Int = 3,
    initialDelay: Long = 1000,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: UFCException) {
            when {
                e.errorCode == ErrorCode.CRUMB_EXPIRED && attempt < maxRetries - 1 -> {
                    authenticator.reset()
                    authenticator.authenticate()
                    delay(currentDelay)
                }
                e.errorCode.isRetryable() && attempt < maxRetries - 1 -> {
                    delay(currentDelay)
                }
                else -> throw e
            }
            currentDelay *= 2
        }
    }
    throw UFCException(ErrorCode.UNKNOWN_ERROR)
}
```

---

**다음 문서**: [04-yahoo-finance-etf.md](./04-yahoo-finance-etf.md)
