# UFC Implementation Steps - Phase 9-15 (API 구현 및 완성)

## 문서 정보
- **버전**: 1.0.0
- **최종 작성일**: 2025-12-02
- **이전 문서**: step-by-step-guide-02-phase-04-08-auth-and-testing.md
- **대상**: Claude Haiku Model

---

## Phase 9: Yahoo Finance Chart API 구현

### Step 9.1: Chart API 응답 모델 작성

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/model/yahoo/ChartResponse.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.model.yahoo

import kotlinx.serialization.Serializable

/**
 * Yahoo Finance Chart API 응답
 */
@Serializable
data class ChartResponse(
    val chart: Chart
) {
    @Serializable
    data class Chart(
        val result: List<Result>? = null,
        val error: Error? = null
    )

    @Serializable
    data class Result(
        val meta: Meta,
        val timestamp: List<Long>,
        val indicators: Indicators,
        val events: Events? = null
    )

    @Serializable
    data class Meta(
        val currency: String,
        val symbol: String,
        val exchangeName: String,
        val instrumentType: String,
        val firstTradeDate: Long,
        val regularMarketTime: Long,
        val gmtoffset: Int,
        val timezone: String,
        val exchangeTimezoneName: String,
        val regularMarketPrice: Double? = null,
        val chartPreviousClose: Double? = null,
        val priceHint: Int,
        val currentTradingPeriod: TradingPeriod? = null,
        val dataGranularity: String,
        val range: String,
        val validRanges: List<String>
    )

    @Serializable
    data class TradingPeriod(
        val pre: Period? = null,
        val regular: Period,
        val post: Period? = null
    )

    @Serializable
    data class Period(
        val timezone: String,
        val start: Long,
        val end: Long,
        val gmtoffset: Int
    )

    @Serializable
    data class Indicators(
        val quote: List<Quote>,
        val adjclose: List<AdjClose>? = null
    )

    @Serializable
    data class Quote(
        val open: List<Double?>,
        val high: List<Double?>,
        val low: List<Double?>,
        val close: List<Double?>,
        val volume: List<Long?>
    )

    @Serializable
    data class AdjClose(
        val adjclose: List<Double?>
    )

    @Serializable
    data class Events(
        val dividends: Map<String, Dividend>? = null,
        val splits: Map<String, Split>? = null
    )

    @Serializable
    data class Dividend(
        val amount: Double,
        val date: Long
    )

    @Serializable
    data class Split(
        val date: Long,
        val numerator: Int,
        val denominator: Int,
        val splitRatio: String
    )

    @Serializable
    data class Error(
        val code: String,
        val description: String
    )
}
```

✅ **완료 조건:**
- ChartResponse.kt 파일이 작성됨
- 모든 nested 클래스가 정의됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/model/yahoo/ChartResponse.kt`

---

### Step 9.2: PriceBar 모델 작성

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/model/stock/PriceBar.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.model.stock

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 가격 데이터 (OHLCV)
 */
data class PriceBar(
    val date: LocalDate,
    val timestamp: Long,
    val open: Double?,
    val high: Double?,
    val low: Double?,
    val close: Double?,
    val adjClose: Double?,
    val volume: Long?
) {
    companion object {
        fun fromTimestamp(
            timestamp: Long,
            open: Double?,
            high: Double?,
            low: Double?,
            close: Double?,
            adjClose: Double?,
            volume: Long?
        ): PriceBar {
            val date = Instant.ofEpochSecond(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            return PriceBar(
                date = date,
                timestamp = timestamp,
                open = open,
                high = high,
                low = low,
                close = close,
                adjClose = adjClose,
                volume = volume
            )
        }
    }
}
```

✅ **완료 조건:**
- PriceBar.kt 파일이 작성됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/model/stock/PriceBar.kt`

---

### Step 9.3: ChartParams 작성

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/internal/yahoo/ChartParams.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.internal.yahoo

import com.ulalax.ufc.model.common.Interval
import io.ktor.client.request.*
import java.time.LocalDate
import java.time.ZoneId

/**
 * Chart API 요청 파라미터
 */
internal data class ChartParams(
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

    companion object {
        fun fromDates(
            start: LocalDate,
            end: LocalDate,
            interval: Interval = Interval.OneDay
        ): ChartParams {
            val period1 = start.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
            val period2 = end.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()

            return ChartParams(
                period1 = period1,
                period2 = period2,
                interval = interval
            )
        }
    }
}
```

✅ **완료 조건:**
- ChartParams.kt 파일이 작성됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/internal/yahoo/ChartParams.kt`

---

### Step 9.4: YahooFinanceClient - Chart API 구현

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/internal/yahoo/YahooFinanceClient.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.internal.yahoo

import com.ulalax.ufc.exception.ErrorCode
import com.ulalax.ufc.exception.UFCException
import com.ulalax.ufc.internal.yahoo.auth.YahooAuthenticator
import com.ulalax.ufc.model.stock.PriceBar
import com.ulalax.ufc.model.yahoo.ChartResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

/**
 * Yahoo Finance HTTP 클라이언트
 */
internal class YahooFinanceClient(
    private val httpClient: HttpClient,
    private val authenticator: YahooAuthenticator
) {

    /**
     * Chart API 호출
     */
    suspend fun fetchChart(
        symbol: String,
        params: ChartParams
    ): ChartResponse {
        val url = "${YahooApiUrls.CHART}/$symbol"

        val response = httpClient.get(url) {
            authenticator.applyAuth(this)
            params.applyTo(this)
        }.body<ChartResponse>()

        // 에러 체크
        response.chart.error?.let { error ->
            throw UFCException(
                errorCode = ErrorCode.NO_DATA_AVAILABLE,
                message = error.description,
                metadata = mapOf(
                    "symbol" to symbol,
                    "errorCode" to error.code
                )
            )
        }

        return response
    }

    /**
     * Chart 응답을 PriceBar 리스트로 변환
     */
    fun chartToPriceBars(response: ChartResponse): List<PriceBar> {
        val result = response.chart.result?.firstOrNull()
            ?: throw UFCException(
                errorCode = ErrorCode.EMPTY_RESPONSE,
                message = "No result in chart response"
            )

        val timestamps = result.timestamp
        val quote = result.indicators.quote.firstOrNull()
            ?: throw UFCException(
                errorCode = ErrorCode.EMPTY_RESPONSE,
                message = "No quote data in indicators"
            )

        val adjClose = result.indicators.adjclose?.firstOrNull()?.adjclose

        return timestamps.indices.map { i ->
            PriceBar.fromTimestamp(
                timestamp = timestamps[i],
                open = quote.open.getOrNull(i),
                high = quote.high.getOrNull(i),
                low = quote.low.getOrNull(i),
                close = quote.close.getOrNull(i),
                adjClose = adjClose?.getOrNull(i),
                volume = quote.volume.getOrNull(i)
            )
        }
    }
}
```

✅ **완료 조건:**
- YahooFinanceClient.kt 파일이 작성됨
- fetchChart 메서드 구현됨
- chartToPriceBars 변환 메서드 구현됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/internal/yahoo/YahooFinanceClient.kt`

---

### Step 9.5: Chart API Live Test 작성

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/liveTest/kotlin/com/ulalax/ufc/live/yahoo/chart/ChartDailyLiveTest.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.live.yahoo.chart

import com.ulalax.ufc.internal.yahoo.ChartParams
import com.ulalax.ufc.internal.yahoo.YahooFinanceClient
import com.ulalax.ufc.internal.yahoo.YahooHttpClientFactory
import com.ulalax.ufc.internal.yahoo.auth.YahooAuthenticator
import com.ulalax.ufc.model.common.Interval
import com.ulalax.ufc.utils.RecordingConfig
import com.ulalax.ufc.utils.ResponseRecorder
import com.ulalax.ufc.utils.TestSymbols
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

/**
 * Chart API Daily Live Test
 *
 * ## 목적
 * - Yahoo Finance Chart API 일간 데이터 조회 검증
 * - OHLCV 데이터 파싱 확인
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew liveTest --tests "ChartDailyLiveTest"
 * ```
 */
@Tag("live")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChartDailyLiveTest {

    private val httpClient = YahooHttpClientFactory.create()
    private val authenticator = YahooAuthenticator(httpClient)
    private val client = YahooFinanceClient(httpClient, authenticator)

    @AfterAll
    fun tearDown() {
        httpClient.close()
    }

    @Test
    @DisplayName("AAPL 일간 가격 데이터를 조회할 수 있다")
    fun testFetchDailyChart_AAPL() = runTest {
        // Given: AAPL 심볼과 날짜 범위
        val symbol = TestSymbols.AAPL
        val start = LocalDate.of(2024, 1, 1)
        val end = LocalDate.of(2024, 12, 31)

        // When: Chart API 호출
        val params = ChartParams.fromDates(start, end, Interval.OneDay)
        val response = client.fetchChart(symbol, params)

        // Then: 응답 검증
        assertNotNull(response.chart.result, "Chart result should not be null")
        assertTrue(response.chart.result!!.isNotEmpty(), "Chart result should not be empty")

        val result = response.chart.result!!.first()
        assertNotNull(result.timestamp, "Timestamps should not be null")
        assertTrue(result.timestamp.isNotEmpty(), "Timestamps should not be empty")

        // Then: PriceBar 변환 검증
        val priceBars = client.chartToPriceBars(response)
        assertTrue(priceBars.isNotEmpty(), "PriceBars should not be empty")

        val firstBar = priceBars.first()
        assertNotNull(firstBar.open, "Open price should not be null")
        assertNotNull(firstBar.close, "Close price should not be null")
        assertNotNull(firstBar.volume, "Volume should not be null")

        println("✅ AAPL Daily Chart:")
        println("   - Data points: ${priceBars.size}")
        println("   - First date: ${priceBars.first().date}")
        println("   - Last date: ${priceBars.last().date}")
        println("   - First close: ${firstBar.close}")
        println("   - Last close: ${priceBars.last().close}")

        // 응답 레코딩
        ResponseRecorder.record(
            data = response,
            category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
            fileName = "aapl_daily_2024"
        )
    }

    @Test
    @DisplayName("SPY ETF 일간 가격 데이터를 조회할 수 있다")
    fun testFetchDailyChart_SPY() = runTest {
        // Given: SPY 심볼과 최근 3개월
        val symbol = TestSymbols.SPY
        val end = LocalDate.now()
        val start = end.minusMonths(3)

        // When: Chart API 호출
        val params = ChartParams.fromDates(start, end, Interval.OneDay)
        val response = client.fetchChart(symbol, params)

        // Then: PriceBar 변환
        val priceBars = client.chartToPriceBars(response)

        assertTrue(priceBars.isNotEmpty())
        assertTrue(priceBars.size >= 50, "Should have at least 50 trading days in 3 months")

        println("✅ SPY Daily Chart:")
        println("   - Data points: ${priceBars.size}")
        println("   - Date range: ${priceBars.first().date} to ${priceBars.last().date}")

        // 응답 레코딩
        ResponseRecorder.record(
            data = response,
            category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
            fileName = "spy_daily_3months"
        )
    }

    @Test
    @DisplayName("Adjusted Close 데이터가 포함되어야 한다")
    fun testAdjustedClose() = runTest {
        // Given: AAPL 심볼
        val symbol = TestSymbols.AAPL
        val start = LocalDate.of(2024, 1, 1)
        val end = LocalDate.of(2024, 12, 31)

        // When: Chart API 호출 (includeAdjustedClose = true)
        val params = ChartParams.fromDates(start, end, Interval.OneDay)
        val response = client.fetchChart(symbol, params)

        // Then: Adjusted Close 확인
        val result = response.chart.result!!.first()
        assertNotNull(result.indicators.adjclose, "Adjusted close should be present")

        val priceBars = client.chartToPriceBars(response)
        val barsWithAdjClose = priceBars.filter { it.adjClose != null }

        assertTrue(barsWithAdjClose.isNotEmpty(), "Some bars should have adjusted close")

        println("✅ Adjusted Close Data:")
        println("   - Total bars: ${priceBars.size}")
        println("   - Bars with adj close: ${barsWithAdjClose.size}")
    }
}
```

✅ **완료 조건:**
- ChartDailyLiveTest.kt 파일이 작성됨
- 3개의 테스트가 구현됨

🧪 **테스트 실행:**
```bash
./gradlew liveTest --tests "ChartDailyLiveTest"
```

**예상 결과:**
- 3개 테스트 모두 통과
- AAPL, SPY 가격 데이터 출력
- 레코딩 파일 생성

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/liveTest/kotlin/com/ulalax/ufc/live/yahoo/chart/ChartDailyLiveTest.kt`

---

## Phase 10: StockApi 구현

### Step 10.1: StockApi 인터페이스 작성

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/api/StockApi.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.api

import com.ulalax.ufc.model.common.Interval
import com.ulalax.ufc.model.common.Period
import com.ulalax.ufc.model.stock.PriceBar
import java.time.LocalDate

/**
 * 주식 도메인 API
 *
 * 개별 주식의 가격, 정보, 재무제표 등을 조회합니다.
 */
interface StockApi {

    /**
     * 주가 이력 조회 (Period 방식)
     */
    suspend fun history(
        symbol: String,
        period: Period = Period.OneYear,
        interval: Interval = Interval.OneDay
    ): List<PriceBar>

    /**
     * 주가 이력 조회 (날짜 범위 지정)
     */
    suspend fun history(
        symbol: String,
        start: LocalDate,
        end: LocalDate,
        interval: Interval = Interval.OneDay
    ): List<PriceBar>
}
```

✅ **완료 조건:**
- StockApi.kt 파일이 작성됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/api/StockApi.kt`

---

### Step 10.2: StockApiImpl 구현

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/internal/stock/StockApiImpl.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.internal.stock

import com.ulalax.ufc.api.StockApi
import com.ulalax.ufc.internal.yahoo.ChartParams
import com.ulalax.ufc.internal.yahoo.YahooFinanceClient
import com.ulalax.ufc.model.common.Interval
import com.ulalax.ufc.model.common.Period
import com.ulalax.ufc.model.stock.PriceBar
import java.time.LocalDate

/**
 * StockApi 구현
 */
internal class StockApiImpl(
    private val yahooClient: YahooFinanceClient
) : StockApi {

    override suspend fun history(
        symbol: String,
        period: Period,
        interval: Interval
    ): List<PriceBar> {
        val params = ChartParams(
            period1 = null, // Period 사용 시 null
            period2 = null,
            interval = interval,
            includeAdjustedClose = true,
            events = "div,splits"
        )

        val response = yahooClient.fetchChart(symbol, params)
        return yahooClient.chartToPriceBars(response)
    }

    override suspend fun history(
        symbol: String,
        start: LocalDate,
        end: LocalDate,
        interval: Interval
    ): List<PriceBar> {
        val params = ChartParams.fromDates(start, end, interval)
        val response = yahooClient.fetchChart(symbol, params)
        return yahooClient.chartToPriceBars(response)
    }
}
```

✅ **완료 조건:**
- StockApiImpl.kt 파일이 작성됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/internal/stock/StockApiImpl.kt`

---

## Phase 11: UFCClient 완성

### Step 11.1: UFCClient 전체 구현

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/client/UFCClient.kt` (덮어쓰기)

**파일 내용:**
```kotlin
package com.ulalax.ufc.client

import com.ulalax.ufc.api.StockApi
import com.ulalax.ufc.exception.ErrorCode
import com.ulalax.ufc.exception.UFCException
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitingSettings
import com.ulalax.ufc.infrastructure.ratelimit.TokenBucketRateLimiter
import com.ulalax.ufc.internal.stock.StockApiImpl
import com.ulalax.ufc.internal.yahoo.YahooFinanceClient
import com.ulalax.ufc.internal.yahoo.YahooHttpClientFactory
import com.ulalax.ufc.internal.yahoo.auth.YahooAuthenticator
import io.ktor.client.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

/**
 * UFC 클라이언트 설정
 */
data class UFCClientConfig(
    val fredApiKey: String? = null,
    val rateLimitingSettings: RateLimitingSettings = RateLimitingSettings()
)

/**
 * UFC (US Free Financial Data Collector) 통합 클라이언트
 *
 * Domain-Based 아키텍처를 사용하여 주식, ETF, 매크로 지표 기능을 제공합니다.
 */
class UFCClient private constructor(
    val stock: StockApi,
    private val httpClient: HttpClient,
    private val config: UFCClientConfig
) : AutoCloseable {

    private val logger = LoggerFactory.getLogger(UFCClient::class.java)

    companion object {
        /**
         * UFCClient 인스턴스 생성
         */
        suspend fun create(
            config: UFCClientConfig = UFCClientConfig()
        ): UFCClient {
            val logger = LoggerFactory.getLogger(UFCClient::class.java)

            try {
                // 1. HTTP 클라이언트 생성
                val httpClient = YahooHttpClientFactory.create()

                // 2. Rate Limiter 생성
                val yahooRateLimiter = TokenBucketRateLimiter(
                    config.rateLimitingSettings.yahoo
                )

                // 3. Yahoo Finance 인증
                val authenticator = YahooAuthenticator(httpClient)
                authenticator.authenticate()

                // 4. Yahoo Finance Client 생성
                val yahooClient = YahooFinanceClient(httpClient, authenticator)

                // 5. Domain API 구현체 생성
                val stockApi = StockApiImpl(yahooClient)

                logger.info("UFCClient initialized successfully")

                return UFCClient(
                    stock = stockApi,
                    httpClient = httpClient,
                    config = config
                )
            } catch (e: Exception) {
                throw UFCException(
                    errorCode = ErrorCode.AUTH_FAILED,
                    message = "Failed to initialize UFCClient",
                    cause = e
                )
            }
        }

        /**
         * 동기 생성 헬퍼 (Java 호환)
         */
        @JvmStatic
        fun createBlocking(config: UFCClientConfig = UFCClientConfig()): UFCClient {
            return runBlocking { create(config) }
        }
    }

    override fun close() {
        try {
            httpClient.close()
            logger.info("UFCClient closed successfully")
        } catch (e: Exception) {
            logger.error("Error while closing UFCClient", e)
        }
    }
}
```

✅ **완료 조건:**
- UFCClient.kt 파일이 완전히 재작성됨
- StockApi가 통합됨
- 인증 및 초기화 로직 완성

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/client/UFCClient.kt` (완성본)

---

### Step 11.2: UFCClient 통합 Live Test

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/liveTest/kotlin/com/ulalax/ufc/live/UFCClientLiveTest.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.live

import com.ulalax.ufc.client.UFCClient
import com.ulalax.ufc.client.UFCClientConfig
import com.ulalax.ufc.model.common.Interval
import com.ulalax.ufc.utils.TestSymbols
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

/**
 * UFCClient 통합 Live Test
 *
 * ## 목적
 * - UFCClient 전체 기능 통합 테스트
 * - StockApi 정상 작동 확인
 */
@Tag("live")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UFCClientLiveTest {

    private lateinit var client: UFCClient

    @org.junit.jupiter.api.BeforeAll
    fun setUp() = runTest {
        client = UFCClient.create(UFCClientConfig())
        println("🚀 UFCClient 초기화 완료")
    }

    @AfterAll
    fun tearDown() {
        client.close()
        println("🏁 UFCClient 종료")
    }

    @Test
    @DisplayName("UFCClient를 생성할 수 있다")
    fun testCreateClient() = runTest {
        // Given/When: UFCClient 생성 (setUp에서 완료)

        // Then: StockApi가 사용 가능해야 함
        assertNotNull(client.stock, "StockApi should not be null")

        println("✅ UFCClient 생성 성공")
    }

    @Test
    @DisplayName("StockApi로 AAPL 가격 이력을 조회할 수 있다")
    fun testStockApiHistory() = runTest {
        // Given: AAPL 심볼과 날짜 범위
        val symbol = TestSymbols.AAPL
        val start = LocalDate.of(2024, 1, 1)
        val end = LocalDate.of(2024, 3, 31)

        // When: 가격 이력 조회
        val history = client.stock.history(
            symbol = symbol,
            start = start,
            end = end,
            interval = Interval.OneDay
        )

        // Then: 데이터 검증
        assertNotNull(history, "History should not be null")
        assertTrue(history.isNotEmpty(), "History should not be empty")
        assertTrue(history.size >= 50, "Should have at least 50 trading days in Q1")

        val firstBar = history.first()
        assertNotNull(firstBar.open, "Open price should not be null")
        assertNotNull(firstBar.close, "Close price should not be null")

        println("✅ AAPL History:")
        println("   - Data points: ${history.size}")
        println("   - First: ${firstBar.date} - Close: ${firstBar.close}")
        println("   - Last: ${history.last().date} - Close: ${history.last().close}")
    }

    @Test
    @DisplayName("여러 심볼의 데이터를 순차적으로 조회할 수 있다")
    fun testMultipleSymbols() = runTest {
        // Given: 여러 심볼
        val symbols = listOf(TestSymbols.AAPL, TestSymbols.MSFT, TestSymbols.GOOGL)
        val start = LocalDate.of(2024, 11, 1)
        val end = LocalDate.of(2024, 11, 30)

        // When: 순차적으로 조회
        val results = symbols.map { symbol ->
            val history = client.stock.history(symbol, start, end)
            symbol to history
        }

        // Then: 모든 심볼의 데이터가 조회되어야 함
        assertEquals(3, results.size, "Should have 3 results")
        results.forEach { (symbol, history) ->
            assertTrue(history.isNotEmpty(), "$symbol should have data")
            println("✅ $symbol: ${history.size} data points")
        }
    }
}
```

✅ **완료 조건:**
- UFCClientLiveTest.kt 파일이 작성됨
- 3개의 통합 테스트 구현

🧪 **테스트 실행:**
```bash
./gradlew liveTest --tests "UFCClientLiveTest"
```

**예상 결과:**
- 3개 테스트 모두 통과
- 여러 심볼의 데이터 조회 성공

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/liveTest/kotlin/com/ulalax/ufc/live/UFCClientLiveTest.kt`

---

## Phase 12: Unit Test 작성

### Step 12.1: JsonResponseLoader 작성

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/test/kotlin/com/ulalax/ufc/utils/JsonResponseLoader.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 레코딩된 JSON 응답을 로드하는 유틸리티
 */
object JsonResponseLoader {

    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateDeserializer())
        .registerTypeAdapter(BigDecimal::class.java, BigDecimalDeserializer())
        .create()

    /**
     * 레코딩된 JSON 파일을 로드
     */
    inline fun <reified T> load(category: String, fileName: String): T {
        val resourcePath = "responses/$category/$fileName.json"
        val jsonString = loadResourceAsString(resourcePath)
        return gson.fromJson(jsonString, T::class.java)
    }

    /**
     * Resource 파일을 문자열로 로드
     */
    private fun loadResourceAsString(path: String): String {
        val resource = javaClass.classLoader.getResourceAsStream(path)
            ?: throw IllegalArgumentException("Resource not found: $path")
        return resource.bufferedReader().use { it.readText() }
    }
}

// Gson Deserializers
class LocalDateDeserializer : com.google.gson.JsonDeserializer<LocalDate> {
    override fun deserialize(
        json: com.google.gson.JsonElement,
        typeOfT: java.lang.reflect.Type,
        context: com.google.gson.JsonDeserializationContext
    ): LocalDate {
        return LocalDate.parse(json.asString)
    }
}

class BigDecimalDeserializer : com.google.gson.JsonDeserializer<BigDecimal> {
    override fun deserialize(
        json: com.google.gson.JsonElement,
        typeOfT: java.lang.reflect.Type,
        context: com.google.gson.JsonDeserializationContext
    ): BigDecimal {
        return BigDecimal(json.asString)
    }
}
```

✅ **완료 조건:**
- JsonResponseLoader.kt 파일이 작성됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/test/kotlin/com/ulalax/ufc/utils/JsonResponseLoader.kt`

---

### Step 12.2: ChartResponse Unit Test

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/test/kotlin/com/ulalax/ufc/source/yahoo/ChartResponseTest.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.source.yahoo

import com.ulalax.ufc.model.yahoo.ChartResponse
import com.ulalax.ufc.utils.JsonResponseLoader
import com.ulalax.ufc.utils.RecordingConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * ChartResponse Unit Test
 *
 * ## 목적
 * - 레코딩된 Chart 응답 데이터 파싱 검증
 * - 데이터 변환 로직 검증
 */
class ChartResponseTest {

    @Test
    @DisplayName("레코딩된 AAPL Chart 응답을 파싱할 수 있다")
    fun testParseChartResponse_AAPL() {
        // Given: 레코딩된 AAPL Chart JSON
        val response = JsonResponseLoader.load<ChartResponse>(
            category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
            fileName = "aapl_daily_2024"
        )

        // Then: 응답 구조 검증
        assertNotNull(response.chart.result, "Result should not be null")
        assertTrue(response.chart.result!!.isNotEmpty(), "Result should not be empty")

        val result = response.chart.result!!.first()
        assertNotNull(result.meta, "Meta should not be null")
        assertEquals("AAPL", result.meta.symbol, "Symbol should be AAPL")
        assertNotNull(result.timestamp, "Timestamps should not be null")
        assertTrue(result.timestamp.isNotEmpty(), "Timestamps should not be empty")

        // Then: Quote 데이터 검증
        val quote = result.indicators.quote.first()
        assertNotNull(quote.open, "Open prices should not be null")
        assertNotNull(quote.close, "Close prices should not be null")
        assertEquals(result.timestamp.size, quote.open.size, "Open prices count should match timestamps")
    }

    @Test
    @DisplayName("Adjusted Close 데이터를 파싱할 수 있다")
    fun testParseAdjustedClose() {
        // Given: 레코딩된 Chart JSON
        val response = JsonResponseLoader.load<ChartResponse>(
            category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
            fileName = "aapl_daily_2024"
        )

        // Then: AdjClose 검증
        val result = response.chart.result!!.first()
        assertNotNull(result.indicators.adjclose, "Adjusted close should be present")

        val adjClose = result.indicators.adjclose!!.first()
        assertNotNull(adjClose.adjclose, "Adjusted close values should not be null")
        assertTrue(adjClose.adjclose.isNotEmpty(), "Adjusted close values should not be empty")
    }

    @Test
    @DisplayName("타임스탬프가 시간 순으로 정렬되어 있다")
    fun testTimestampsSorted() {
        // Given: 레코딩된 Chart JSON
        val response = JsonResponseLoader.load<ChartResponse>(
            category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
            fileName = "aapl_daily_2024"
        )

        // When: 타임스탬프 추출
        val timestamps = response.chart.result!!.first().timestamp

        // Then: 정렬 확인
        for (i in 0 until timestamps.size - 1) {
            assertTrue(
                timestamps[i] <= timestamps[i + 1],
                "Timestamps should be in ascending order"
            )
        }
    }
}
```

✅ **완료 조건:**
- ChartResponseTest.kt 파일이 작성됨
- 3개의 Unit Test 구현

🧪 **테스트 실행:**
먼저 Live Test를 실행하여 응답을 레코딩해야 합니다:
```bash
# 1. Live Test 실행하여 레코딩
./gradlew liveTest --tests "ChartDailyLiveTest"

# 2. 레코딩된 파일을 test resources로 복사
cp -r src/liveTest/resources/responses/* src/test/resources/responses/

# 3. Unit Test 실행
./gradlew test --tests "ChartResponseTest"
```

**예상 결과:**
- 3개 테스트 모두 통과

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/test/kotlin/com/ulalax/ufc/source/yahoo/ChartResponseTest.kt`

---

## Phase 13: 최종 검증

### Step 13.1: 전체 빌드 및 테스트

**실행 명령:**
```bash
# 1. Clean build
./gradlew clean build

# 2. Unit Test 실행
./gradlew test

# 3. Live Test 실행
./gradlew liveTest

# 4. 전체 Check
./gradlew check
```

✅ **완료 조건:**
- 모든 빌드 성공
- 모든 Unit Test 통과
- 모든 Live Test 통과

📝 **예상 결과:**
```
BUILD SUCCESSFUL
Total time: X mins

Unit Tests: X passed, 0 failed
Live Tests: X passed, 0 failed
```

---

### Step 13.2: 사용 예제 작성

**파일 경로:** `/home/ulalax/project/kairos/ufc/README.md`

**파일 내용:**
```markdown
# UFC (US Free Financial Data Collector)

Kotlin/JVM 기반 미국 금융 데이터 수집 라이브러리

## 특징

- ✅ **Multi-Source**: Yahoo Finance + FRED (추후 추가)
- ✅ **타입 안전성**: Kotlin의 강력한 타입 시스템
- ✅ **비동기 처리**: Kotlin Coroutines
- ✅ **ErrorCode 기반**: 통합 에러 처리
- ✅ **Rate Limiting**: 자동 속도 제한

## 설치

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.ulalax:ufc:1.0.0")
}
```

## 사용 예제

### 기본 사용

```kotlin
import com.ulalax.ufc.client.UFCClient
import com.ulalax.ufc.model.common.Interval
import java.time.LocalDate

suspend fun main() {
    // 1. Client 생성
    val ufc = UFCClient.create()

    try {
        // 2. 주가 이력 조회
        val history = ufc.stock.history(
            symbol = "AAPL",
            start = LocalDate.of(2024, 1, 1),
            end = LocalDate.of(2024, 12, 31),
            interval = Interval.OneDay
        )

        // 3. 데이터 사용
        history.forEach { bar ->
            println("${bar.date}: Close=${bar.close}, Volume=${bar.volume}")
        }
    } finally {
        // 4. Client 종료
        ufc.close()
    }
}
```

### 에러 처리

```kotlin
import com.ulalax.ufc.exception.UFCException
import com.ulalax.ufc.exception.ErrorCode

try {
    val history = ufc.stock.history("INVALID_SYMBOL")
} catch (e: UFCException) {
    when (e.errorCode) {
        ErrorCode.INVALID_SYMBOL -> {
            println("심볼을 찾을 수 없습니다: ${e.metadata["symbol"]}")
        }
        ErrorCode.RATE_LIMITED -> {
            val retryAfter = e.metadata["retryAfter"] as Long
            println("$retryAfter 초 후 재시도하세요")
        }
        else -> {
            println("에러 발생: ${e.message}")
        }
    }
}
```

## 라이센스

Apache License 2.0

## 기여

Issue 및 Pull Request를 환영합니다!
```

✅ **완료 조건:**
- README.md 파일이 작성됨

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/README.md`

---

## Phase 14: 최종 체크리스트

### ✅ 프로젝트 셋업
- [x] build.gradle.kts 작성
- [x] 디렉토리 구조 생성
- [x] .gitignore 작성

### ✅ 공통 모델
- [x] ErrorCode enum
- [x] UFCException
- [x] Period, Interval, DataFrequency

### ✅ Infrastructure
- [x] RateLimiter 인터페이스
- [x] TokenBucketRateLimiter 구현
- [x] UserAgents 관리

### ✅ Yahoo Finance
- [x] YahooAuthenticator 구현
- [x] YahooHttpClientFactory
- [x] YahooFinanceClient
- [x] Chart API 구현
- [x] ChartResponse 모델

### ✅ Domain API
- [x] StockApi 인터페이스
- [x] StockApiImpl 구현

### ✅ UFCClient
- [x] UFCClient 완전 구현
- [x] 초기화 로직
- [x] 리소스 관리

### ✅ 테스트
- [x] LiveTestBase
- [x] RecordingConfig
- [x] ResponseRecorder
- [x] YahooAuthLiveTest
- [x] ChartDailyLiveTest
- [x] UFCClientLiveTest
- [x] JsonResponseLoader
- [x] ChartResponseTest

### ✅ 문서
- [x] README.md

---

## Phase 15: 축하합니다! 🎉

모든 Phase를 완료했습니다!

### 📊 완성된 기능

1. **프로젝트 인프라** ✅
   - Gradle 빌드 시스템
   - 디렉토리 구조
   - 의존성 관리

2. **에러 처리 시스템** ✅
   - ErrorCode 기반
   - UFCException
   - 재시도 로직

3. **Infrastructure** ✅
   - Rate Limiting (Token Bucket)
   - HTTP Client (Ktor + OkHttp)
   - User-Agent 관리

4. **Yahoo Finance** ✅
   - Cookie/Crumb 인증
   - Chart API
   - 가격 데이터 조회

5. **Domain API** ✅
   - StockApi
   - 날짜 범위/Period 방식 지원

6. **테스트** ✅
   - Live Test 인프라
   - Unit Test 인프라
   - 응답 레코딩 시스템

### 🚀 다음 단계

현재 구현된 것:
- ✅ Yahoo Finance Chart API
- ✅ StockApi (가격 조회)

향후 추가할 수 있는 기능:
- ETFApi (ETF 데이터)
- MacroApi (FRED 경제 지표)
- SearchApi (검색 기능)
- 재무제표 API
- 뉴스 API

### 📝 사용 방법

```bash
# 프로젝트 빌드
./gradlew build

# Unit Test 실행
./gradlew test

# Live Test 실행 (인터넷 필요)
./gradlew liveTest

# 전체 검증
./gradlew check
```

---

**축하합니다! UFC 프로젝트의 핵심 기능이 완성되었습니다!** 🎊

---

**이전 문서**: step-by-step-guide-02-phase-04-08-auth-and-testing.md
**다음 단계**: 추가 기능 구현 (ETF, FRED, Search 등)
**완성도**: 기본 기능 100% 완료
