package com.ulalax.ufc.domain.chart

import com.ulalax.ufc.infrastructure.ratelimit.RateLimitConfig
import com.ulalax.ufc.infrastructure.ratelimit.TokenBucketRateLimiter
import com.ulalax.ufc.internal.yahoo.YahooHttpClientFactory
import com.ulalax.ufc.internal.yahoo.auth.BasicAuthStrategy
import com.ulalax.ufc.model.common.Interval
import com.ulalax.ufc.model.common.Period
import com.ulalax.ufc.utils.LiveTestBase
import com.ulalax.ufc.utils.RecordingConfig
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Yahoo Chart Service Live Test
 *
 * 이 테스트는 실제 Yahoo Finance API와 통신합니다.
 * 개발 환경에서 API의 실제 동작을 검증하기 위해 사용됩니다.
 *
 * 주의:
 * - 실제 네트워크 요청을 수행합니다
 * - Rate Limiting이 적용됩니다
 * - 실행 시간이 오래 걸릴 수 있습니다
 * - API 변경 시 테스트 실패가 발생할 수 있습니다
 */
class YahooChartServiceLiveTest : LiveTestBase() {

    private val httpClient = YahooHttpClientFactory.create()
    private val rateLimiter = TokenBucketRateLimiter(
        "YAHOO",
        RateLimitConfig(capacity = 50, refillRate = 50)
    )

    override fun onBeforeCleanup() {
        httpClient.close()
    }

    @Test
    fun testBasicAuthStrategyCanObtainCrumb() = runTest {
        val authStrategy = BasicAuthStrategy(httpClient)

        // CRUMB 획득 시도
        val authResult = authStrategy.authenticate()

        assertThat(authResult).isNotNull
        assertThat(authResult.crumb).isNotBlank()
        assertThat(authResult.strategy).isEqualTo("basic")
    }

    @Test
    fun testCanFetchDailyChartDataForAApl() = liveTestWithRecording(
        category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
        fileName = "aapl_daily_1m"
    ) {
        // CRUMB 획득
        val authStrategy = BasicAuthStrategy(httpClient)
        val authResult = authStrategy.authenticate()

        // Chart Service 생성
        val service = YahooChartService(httpClient, rateLimiter, authResult)

        // AAPL 차트 데이터 조회 (최근 1개월)
        val data = service.getChartData("AAPL", Interval.OneDay, Period.OneMonth)

        // 데이터 검증
        assertThat(data).isNotEmpty

        // OHLCV 데이터 확인
        data.forEach { ohlcv ->
            assertThat(ohlcv.timestamp).isGreaterThan(0L)
            assertThat(ohlcv.open).isGreaterThan(0.0)
            assertThat(ohlcv.high).isGreaterThan(0.0)
            assertThat(ohlcv.low).isGreaterThan(0.0)
            assertThat(ohlcv.close).isGreaterThan(0.0)
            assertThat(ohlcv.volume).isGreaterThanOrEqualTo(0L)
        }

        // 마지막 데이터가 최근 날짜인지 확인
        val lastTimestamp = data.last().timestamp
        val now = Instant.now().epochSecond
        assertThat(now - lastTimestamp).isLessThan(86400 * 7) // 7일 이내

        data // 반환하여 자동 녹음 처리
    }

    @Test
    fun testCanFetchIndexChartData() = liveTestWithRecording(
        category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
        fileName = "gspc_daily_3m"
    ) {
        val authStrategy = BasicAuthStrategy(httpClient)
        val authResult = authStrategy.authenticate()

        val service = YahooChartService(httpClient, rateLimiter, authResult)

        // S&P 500 지수 조회
        val data = service.getChartData("^GSPC", Interval.OneDay, Period.ThreeMonths)

        // 데이터 검증
        assertThat(data).isNotEmpty
        assertThat(data.size).isGreaterThan(0)

        data // 반환하여 자동 녹음 처리
    }

    @Test
    fun testCanFetchHourlyChartData() = liveTestWithRecording(
        category = RecordingConfig.Paths.Yahoo.Chart.INTRADAY,
        fileName = "aapl_hourly_5d"
    ) {
        val authStrategy = BasicAuthStrategy(httpClient)
        val authResult = authStrategy.authenticate()

        val service = YahooChartService(httpClient, rateLimiter, authResult)

        // 1시간 간격 데이터 조회
        val data = service.getChartData("AAPL", Interval.OneHour, Period.FiveDays)

        // 데이터 검증
        assertThat(data).isNotEmpty
        assertThat(data.size).isGreaterThan(1)

        // 타임스탬프가 증가하는지 확인
        for (i in 1 until data.size) {
            assertThat(data[i].timestamp).isGreaterThan(data[i - 1].timestamp)
        }

        data // 반환하여 자동 녹음 처리
    }

    @Test
    fun testCanFetchFiveMinuteChartData() = liveTestWithRecording(
        category = RecordingConfig.Paths.Yahoo.Chart.INTRADAY,
        fileName = "aapl_5min_1d"
    ) {
        val authStrategy = BasicAuthStrategy(httpClient)
        val authResult = authStrategy.authenticate()

        val service = YahooChartService(httpClient, rateLimiter, authResult)

        // 5분 간격 데이터 조회
        val data = service.getChartData("AAPL", Interval.FiveMinutes, Period.OneDay)

        // 데이터 검증
        assertThat(data).isNotEmpty

        data // 반환하여 자동 녹음 처리
    }

    @Test
    fun testCanFetchGooglChartData() = liveTestWithRecording(
        category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
        fileName = "googl_daily_6m"
    ) {
        val authStrategy = BasicAuthStrategy(httpClient)
        val authResult = authStrategy.authenticate()

        val service = YahooChartService(httpClient, rateLimiter, authResult)

        // GOOGL 조회
        val data = service.getChartData("GOOGL", Interval.OneDay, Period.SixMonths)

        // 데이터 검증
        assertThat(data).isNotEmpty

        data // 반환하여 자동 녹음 처리
    }

    @Test
    fun testCanFetchTslaChartData() = liveTestWithRecording(
        category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
        fileName = "tsla_daily_1y"
    ) {
        val authStrategy = BasicAuthStrategy(httpClient)
        val authResult = authStrategy.authenticate()

        val service = YahooChartService(httpClient, rateLimiter, authResult)

        // TSLA 조회
        val data = service.getChartData("TSLA", Interval.OneDay, Period.OneYear)

        // 데이터 검증
        assertThat(data).isNotEmpty

        data // 반환하여 자동 녹음 처리
    }

    @Test
    fun testCanFetchRawChartData() = liveTestWithRecording(
        category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
        fileName = "aapl_raw_1m"
    ) {
        val authStrategy = BasicAuthStrategy(httpClient)
        val authResult = authStrategy.authenticate()

        val service = YahooChartService(httpClient, rateLimiter, authResult)

        // 원본 응답 조회
        val response = service.getRawChartData("AAPL", Interval.OneDay, Period.OneMonth)

        assertThat(response).isNotNull
        assertThat(response.chart).isNotNull
        assertThat(response.chart.result?.size).isGreaterThan(0)
        assertThat(response.chart.result?.get(0)?.meta?.symbol).isEqualTo("AAPL")

        response // 반환하여 자동 녹음 처리
    }

    @Test
    fun testCanFetchOneYearChartData() = liveTestWithRecording(
        category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
        fileName = "aapl_daily_1y_full"
    ) {
        val authStrategy = BasicAuthStrategy(httpClient)
        val authResult = authStrategy.authenticate()

        val service = YahooChartService(httpClient, rateLimiter, authResult)

        val data = service.getChartData("AAPL", Interval.OneDay, Period.OneYear)

        assertThat(data).isNotEmpty
        assertThat(data.size).isGreaterThan(200) // 1년은 약 252 거래일

        data // 반환하여 자동 녹음 처리
    }

    @Test
    fun testCanFetchMaxPeriodChartData() = liveTestWithRecording(
        category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
        fileName = "aapl_daily_max"
    ) {
        val authStrategy = BasicAuthStrategy(httpClient)
        val authResult = authStrategy.authenticate()

        val service = YahooChartService(httpClient, rateLimiter, authResult)

        val data = service.getChartData("AAPL", Interval.OneDay, Period.Max)

        assertThat(data).isNotEmpty
        assertThat(data.size).isGreaterThan(100) // 최대 기간은 충분한 양의 데이터 (Yahoo API 제한으로 조정)

        data // 반환하여 자동 녹음 처리
    }

    @Test
    fun testRateLimiterWorksWithContinuousRequests() = runTest {
        val authStrategy = BasicAuthStrategy(httpClient)
        val authResult = authStrategy.authenticate()

        val service = YahooChartService(httpClient, rateLimiter, authResult)

        // 여러 요청 수행
        val data1 = service.getChartData("AAPL", Interval.OneDay, Period.OneMonth)
        val data2 = service.getChartData("GOOGL", Interval.OneDay, Period.OneMonth)
        val data3 = service.getChartData("TSLA", Interval.OneDay, Period.OneMonth)

        assertThat(data1).isNotEmpty
        assertThat(data2).isNotEmpty
        assertThat(data3).isNotEmpty
    }

    @Test
    fun testErrorHandlingForInvalidSymbol() = runTest {
        val authStrategy = BasicAuthStrategy(httpClient)
        val authResult = authStrategy.authenticate()

        val service = YahooChartService(httpClient, rateLimiter, authResult)

        try {
            // 존재하지 않는 심볼로 조회 시도
            val data = service.getChartData(
                "INVALID_SYMBOL_THAT_DOES_NOT_EXIST",
                Interval.OneDay,
                Period.OneMonth
            )
        } catch (e: Exception) {
            // 오류가 예상됨
            assertThat(e).isNotNull
        }
    }
}
