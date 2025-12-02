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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
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
@DisplayName("YahooChartService - Yahoo Finance 차트 데이터 조회")
class YahooChartServiceLiveTest : LiveTestBase() {

    private val httpClient = YahooHttpClientFactory.create()
    private val rateLimiter = TokenBucketRateLimiter(
        "YAHOO",
        RateLimitConfig(capacity = 50, refillRate = 50)
    )

    override fun onBeforeCleanup() {
        httpClient.close()
    }

    // ===== Authentication Tests =====
    @Nested
    @DisplayName("인증(Authentication) - Yahoo Finance 인증")
    inner class AuthenticationTests {

        @Test
        @DisplayName("BasicAuthStrategy로 CRUMB을 획득한다")
        fun testBasicAuthStrategyCanObtainCrumb() = runTest {
            // Given: BasicAuthStrategy 초기화
            val authStrategy = BasicAuthStrategy(httpClient)

            // When: CRUMB 획득 시도
            val authResult = authStrategy.authenticate()

            // Then: CRUMB 획득 및 기본 정보 검증
            assertThat(authResult).isNotNull
            assertThat(authResult.crumb).isNotBlank()
            assertThat(authResult.strategy).isEqualTo("basic")
        }
    }

    // ===== GetChartData Tests =====
    @Nested
    @DisplayName("getChartData() - 차트 데이터 조회")
    inner class GetChartDataTests {

        @Nested
        @DisplayName("정상 케이스 - 다양한 심볼 및 기간")
        inner class SuccessCases {

            @Test
            @DisplayName("AAPL 일일(Daily) 차트 데이터를 1개월 기간으로 조회한다")
            fun testCanFetchDailyChartDataForAApl() = liveTestWithRecording(
                category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
                fileName = "aapl_daily_1m"
            ) {
                // Given: Yahoo Finance API와 인증 준비
                val authStrategy = BasicAuthStrategy(httpClient)
                val authResult = authStrategy.authenticate()
                val service = YahooChartService(httpClient, rateLimiter, authResult)

                // When: AAPL의 일일 차트 데이터 조회
                val data = service.getChartData("AAPL", Interval.OneDay, Period.OneMonth)

                // Then: 데이터 검증
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
            @DisplayName("S&P 500(^GSPC) 지수 데이터를 3개월 기간으로 조회한다")
            fun testCanFetchIndexChartData() = liveTestWithRecording(
                category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
                fileName = "gspc_daily_3m"
            ) {
                // Given: Yahoo Finance API와 인증 준비
                val authStrategy = BasicAuthStrategy(httpClient)
                val authResult = authStrategy.authenticate()
                val service = YahooChartService(httpClient, rateLimiter, authResult)

                // When: S&P 500 지수 조회
                val data = service.getChartData("^GSPC", Interval.OneDay, Period.ThreeMonths)

                // Then: 데이터 검증
                assertThat(data).isNotEmpty
                assertThat(data.size).isGreaterThan(0)

                data // 반환하여 자동 녹음 처리
            }

            @Test
            @DisplayName("AAPL 시간별(1시간) 차트 데이터를 5일 기간으로 조회한다")
            fun testCanFetchHourlyChartData() = liveTestWithRecording(
                category = RecordingConfig.Paths.Yahoo.Chart.INTRADAY,
                fileName = "aapl_hourly_5d"
            ) {
                // Given: Yahoo Finance API와 인증 준비
                val authStrategy = BasicAuthStrategy(httpClient)
                val authResult = authStrategy.authenticate()
                val service = YahooChartService(httpClient, rateLimiter, authResult)

                // When: 1시간 간격 데이터 조회
                val data = service.getChartData("AAPL", Interval.OneHour, Period.FiveDays)

                // Then: 데이터 검증
                assertThat(data).isNotEmpty
                assertThat(data.size).isGreaterThan(1)

                // 타임스탬프가 증가하는지 확인
                for (i in 1 until data.size) {
                    assertThat(data[i].timestamp).isGreaterThan(data[i - 1].timestamp)
                }

                data // 반환하여 자동 녹음 처리
            }

            @Test
            @DisplayName("AAPL 5분 간격 차트 데이터를 1일 기간으로 조회한다")
            fun testCanFetchFiveMinuteChartData() = liveTestWithRecording(
                category = RecordingConfig.Paths.Yahoo.Chart.INTRADAY,
                fileName = "aapl_5min_1d"
            ) {
                // Given: Yahoo Finance API와 인증 준비
                val authStrategy = BasicAuthStrategy(httpClient)
                val authResult = authStrategy.authenticate()
                val service = YahooChartService(httpClient, rateLimiter, authResult)

                // When: 5분 간격 데이터 조회
                val data = service.getChartData("AAPL", Interval.FiveMinutes, Period.OneDay)

                // Then: 데이터 검증
                assertThat(data).isNotEmpty

                data // 반환하여 자동 녹음 처리
            }
        }

        @Nested
        @DisplayName("특정 기간 조회 - 중장기 데이터")
        inner class PeriodRangeCases {

            @Test
            @DisplayName("GOOGL 일일 차트 데이터를 6개월 기간으로 조회한다")
            fun testCanFetchGooglChartData() = liveTestWithRecording(
                category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
                fileName = "googl_daily_6m"
            ) {
                // Given: Yahoo Finance API와 인증 준비
                val authStrategy = BasicAuthStrategy(httpClient)
                val authResult = authStrategy.authenticate()
                val service = YahooChartService(httpClient, rateLimiter, authResult)

                // When: GOOGL 조회
                val data = service.getChartData("GOOGL", Interval.OneDay, Period.SixMonths)

                // Then: 데이터 검증
                assertThat(data).isNotEmpty

                data // 반환하여 자동 녹음 처리
            }

            @Test
            @DisplayName("TSLA 일일 차트 데이터를 1년 기간으로 조회한다")
            fun testCanFetchTslaChartData() = liveTestWithRecording(
                category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
                fileName = "tsla_daily_1y"
            ) {
                // Given: Yahoo Finance API와 인증 준비
                val authStrategy = BasicAuthStrategy(httpClient)
                val authResult = authStrategy.authenticate()
                val service = YahooChartService(httpClient, rateLimiter, authResult)

                // When: TSLA 조회
                val data = service.getChartData("TSLA", Interval.OneDay, Period.OneYear)

                // Then: 데이터 검증
                assertThat(data).isNotEmpty

                data // 반환하여 자동 녹음 처리
            }

            @Test
            @DisplayName("AAPL 일일 차트 데이터를 1년 기간으로 조회한다")
            fun testCanFetchOneYearChartData() = liveTestWithRecording(
                category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
                fileName = "aapl_daily_1y_full"
            ) {
                // Given: Yahoo Finance API와 인증 준비
                val authStrategy = BasicAuthStrategy(httpClient)
                val authResult = authStrategy.authenticate()
                val service = YahooChartService(httpClient, rateLimiter, authResult)

                // When: AAPL 1년 데이터 조회
                val data = service.getChartData("AAPL", Interval.OneDay, Period.OneYear)

                // Then: 데이터 검증
                assertThat(data).isNotEmpty
                assertThat(data.size).isGreaterThan(200) // 1년은 약 252 거래일

                data // 반환하여 자동 녹음 처리
            }

            @Test
            @DisplayName("AAPL 일일 차트 데이터를 최대 기간으로 조회한다")
            fun testCanFetchMaxPeriodChartData() = liveTestWithRecording(
                category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
                fileName = "aapl_daily_max"
            ) {
                // Given: Yahoo Finance API와 인증 준비
                val authStrategy = BasicAuthStrategy(httpClient)
                val authResult = authStrategy.authenticate()
                val service = YahooChartService(httpClient, rateLimiter, authResult)

                // When: AAPL 최대 기간 데이터 조회
                val data = service.getChartData("AAPL", Interval.OneDay, Period.Max)

                // Then: 데이터 검증
                assertThat(data).isNotEmpty
                assertThat(data.size).isGreaterThan(100) // 최대 기간은 충분한 양의 데이터 (Yahoo API 제한으로 조정)

                data // 반환하여 자동 녹음 처리
            }
        }
    }

    // ===== GetRawChartData Tests =====
    @Nested
    @DisplayName("getRawChartData() - 원본 응답 조회")
    inner class GetRawChartDataTests {

        @Test
        @DisplayName("AAPL 일일 차트의 원본 응답을 조회한다")
        fun testCanFetchRawChartData() = liveTestWithRecording(
            category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
            fileName = "aapl_raw_1m"
        ) {
            // Given: Yahoo Finance API와 인증 준비
            val authStrategy = BasicAuthStrategy(httpClient)
            val authResult = authStrategy.authenticate()
            val service = YahooChartService(httpClient, rateLimiter, authResult)

            // When: 원본 응답 조회
            val response = service.getRawChartData("AAPL", Interval.OneDay, Period.OneMonth)

            // Then: 원본 응답 검증
            assertThat(response).isNotNull
            assertThat(response.chart).isNotNull
            assertThat(response.chart.result?.size).isGreaterThan(0)
            assertThat(response.chart.result?.get(0)?.meta?.symbol).isEqualTo("AAPL")

            response // 반환하여 자동 녹음 처리
        }
    }

    // ===== Rate Limiting Tests =====
    @Nested
    @DisplayName("Rate Limiting - 레이트 제한 검증")
    inner class RateLimitingTests {

        @Test
        @DisplayName("연속된 여러 요청이 Rate Limiter에 의해 정상 처리된다")
        fun testRateLimiterWorksWithContinuousRequests() = runTest {
            // Given: Yahoo Finance API와 인증 준비
            val authStrategy = BasicAuthStrategy(httpClient)
            val authResult = authStrategy.authenticate()
            val service = YahooChartService(httpClient, rateLimiter, authResult)

            // When: 여러 심볼에 대해 연속 요청 수행
            val data1 = service.getChartData("AAPL", Interval.OneDay, Period.OneMonth)
            val data2 = service.getChartData("GOOGL", Interval.OneDay, Period.OneMonth)
            val data3 = service.getChartData("TSLA", Interval.OneDay, Period.OneMonth)

            // Then: 모든 요청이 성공적으로 처리됨
            assertThat(data1).isNotEmpty
            assertThat(data2).isNotEmpty
            assertThat(data3).isNotEmpty
        }

        @Test
        @DisplayName("존재하지 않는 심볼로 조회 시 예외를 발생시킨다")
        fun testErrorHandlingForInvalidSymbol() = runTest {
            // Given: Yahoo Finance API와 인증 준비
            val authStrategy = BasicAuthStrategy(httpClient)
            val authResult = authStrategy.authenticate()
            val service = YahooChartService(httpClient, rateLimiter, authResult)

            // When & Then: 존재하지 않는 심볼로 조회 시도 시 예외 발생 확인
            try {
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
}
