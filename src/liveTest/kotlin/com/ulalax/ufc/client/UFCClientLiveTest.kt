package com.ulalax.ufc.client

import com.ulalax.ufc.model.common.Interval
import com.ulalax.ufc.model.common.Period
import com.ulalax.ufc.utils.LiveTestBase
import com.ulalax.ufc.utils.RecordingConfig
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * UFCClient Live Test
 *
 * 실제 Yahoo Finance API와의 통신을 테스트합니다.
 */
@DisplayName("UFCClient - UFC 통합 클라이언트 테스트")
class UFCClientLiveTest : LiveTestBase() {

    // ===== Initialization Tests =====
    @Nested
    @DisplayName("초기화 및 관리 - 클라이언트 라이프사이클")
    inner class InitializationTests {

        @Test
        @DisplayName("클라이언트를 생성하고 정상적으로 종료한다")
        fun testClientCreationAndClosure() = runTest {
            // Given: UFC Client Config 준비
            val config = UFCClientConfig()

            // When: 클라이언트 생성
            val localClient = UFCClient.create(config)

            // Then: 클라이언트 정상 생성 확인
            assertThat(localClient).isNotNull

            // Finally: 클라이언트 종료
            localClient.close()
        }

        @Test
        @DisplayName("클라이언트 상태 조회를 수행한다")
        fun testClientStatus() = runTest {
            // Given: UFC Client 생성
            val localClient = UFCClient.create(UFCClientConfig())

            try {
                // When: 클라이언트 상태 조회
                val status = localClient.getStatus()

                // Then: 상태 정보 검증
                assertThat(status).isNotBlank()
            } finally {
                // Finally: 클라이언트 종료
                localClient.close()
            }
        }

        @Test
        @DisplayName("클라이언트 설정을 조회한다")
        fun testClientConfig() = runTest {
            // Given: 특정 설정을 포함한 UFCClientConfig
            val config = UFCClientConfig(
                fredApiKey = "test-key"
            )

            // When: 클라이언트 생성 및 설정 조회
            val localClient = UFCClient.create(config)

            try {
                val retrievedConfig = localClient.getConfig()

                // Then: 설정 정보 검증
                assertThat(retrievedConfig.fredApiKey).isEqualTo("test-key")
            } finally {
                // Finally: 클라이언트 종료
                localClient.close()
            }
        }
    }

    // ===== Chart Operations Tests =====
    @Nested
    @DisplayName("Chart API - 차트 데이터 조회")
    inner class ChartOperationsTests {

        @Nested
        @DisplayName("단일 심볼 조회")
        inner class SingleSymbolTests {

            @Test
            @DisplayName("AAPL 일일 차트 데이터를 1개월 기간으로 조회한다")
            fun testCanFetchAaplDailyChartData() = liveTestWithRecording(
                category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
                fileName = "aapl_daily_1m_client"
            ) {
                // Given: UFC Client 생성
                val localClient = UFCClient.create(UFCClientConfig())

                try {
                    // When: AAPL 일일 차트 데이터 조회
                    val chartData = localClient.getChartData(
                        symbol = "AAPL",
                        interval = Interval.OneDay,
                        period = Period.OneMonth
                    )

                    // Then: 차트 데이터 검증
                    assertThat(chartData).isNotEmpty

                    // OHLCV 데이터 검증
                    chartData.forEach { ohlcv ->
                        assertThat(ohlcv.timestamp).isNotEqualTo(0L)
                        assertThat(ohlcv.open).isNotEqualTo(0.0)
                        assertThat(ohlcv.high).isNotEqualTo(0.0)
                        assertThat(ohlcv.low).isNotEqualTo(0.0)
                        assertThat(ohlcv.close).isNotEqualTo(0.0)
                        assertThat(ohlcv.volume).isNotEqualTo(0L)
                    }

                    chartData
                } finally {
                    localClient.close()
                }
            }

            @Test
            @DisplayName("S&P 500(^GSPC) 지수 데이터를 3개월 기간으로 조회한다")
            fun testCanFetchIndexData() = liveTestWithRecording(
                category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
                fileName = "gspc_daily_3m_client"
            ) {
                // Given: UFC Client 생성
                val localClient = UFCClient.create(UFCClientConfig())

                try {
                    // When: S&P 500 지수 데이터 조회
                    val chartData = localClient.getChartData(
                        symbol = "^GSPC",
                        interval = Interval.OneDay,
                        period = Period.ThreeMonths
                    )

                    // Then: 차트 데이터 검증
                    assertThat(chartData).isNotEmpty

                    chartData
                } finally {
                    localClient.close()
                }
            }

            @Test
            @DisplayName("AAPL 일일 차트 데이터를 1일 기간으로 조회한다")
            fun testCanFetchOneDayChartData() = liveTestWithRecording(
                category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
                fileName = "aapl_daily_1d"
            ) {
                // Given: UFC Client 생성
                val localClient = UFCClient.create(UFCClientConfig())

                try {
                    // When: AAPL 1일 데이터 조회
                    val chartData = localClient.getChartData(
                        symbol = "AAPL",
                        interval = Interval.OneDay,
                        period = Period.OneDay
                    )

                    // Then: 차트 데이터 검증
                    assertThat(chartData).isNotEmpty

                    chartData
                } finally {
                    localClient.close()
                }
            }

            @Test
            @DisplayName("AAPL 일일 차트 데이터를 1년 기간으로 조회한다")
            fun testCanFetchOneYearChartData() = liveTestWithRecording(
                category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
                fileName = "aapl_daily_1y_client"
            ) {
                // Given: UFC Client 생성
                val localClient = UFCClient.create(UFCClientConfig())

                try {
                    // When: AAPL 1년 데이터 조회
                    val chartData = localClient.getChartData(
                        symbol = "AAPL",
                        interval = Interval.OneDay,
                        period = Period.OneYear
                    )

                    // Then: 차트 데이터 검증
                    assertThat(chartData).isNotEmpty
                    assertThat(chartData.size).isNotEqualTo(0)

                    chartData
                } finally {
                    localClient.close()
                }
            }

            @Test
            @DisplayName("AAPL 일일 차트 데이터를 최대 기간으로 조회한다")
            fun testCanFetchMaxPeriodChartData() = liveTestWithRecording(
                category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
                fileName = "aapl_daily_max_client"
            ) {
                // Given: UFC Client 생성
                val localClient = UFCClient.create(UFCClientConfig())

                try {
                    // When: AAPL 최대 기간 데이터 조회
                    val chartData = localClient.getChartData(
                        symbol = "AAPL",
                        interval = Interval.OneDay,
                        period = Period.Max
                    )

                    // Then: 차트 데이터 검증
                    assertThat(chartData).isNotEmpty
                    assertThat(chartData.size).isNotEqualTo(0)

                    chartData
                } finally {
                    localClient.close()
                }
            }
        }

        @Nested
        @DisplayName("다중 심볼 및 다양한 주기")
        inner class MultipleSymbolsAndIntervalsTests {

            @Test
            @DisplayName("GOOGL 시간별(1시간) 차트 데이터를 5일 기간으로 조회한다")
            fun testCanFetchHourlyChartData() = liveTestWithRecording(
                category = RecordingConfig.Paths.Yahoo.Chart.INTRADAY,
                fileName = "googl_hourly_5d_client"
            ) {
                // Given: UFC Client 생성
                val localClient = UFCClient.create(UFCClientConfig())

                try {
                    // When: GOOGL 시간별 차트 데이터 조회
                    val chartData = localClient.getChartData(
                        symbol = "GOOGL",
                        interval = Interval.OneHour,
                        period = Period.FiveDays
                    )

                    // Then: 차트 데이터 검증
                    assertThat(chartData).isNotEmpty

                    chartData
                } finally {
                    localClient.close()
                }
            }

            @Test
            @DisplayName("여러 심볼(AAPL, GOOGL, TSLA)의 차트 데이터를 동시 조회한다")
            fun testCanFetchMultipleSymbolsChartData() = liveTestWithRecording(
                category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
                fileName = "multi_symbols_daily_1m"
            ) {
                // Given: UFC Client 생성
                val localClient = UFCClient.create(UFCClientConfig())

                try {
                    // When: 다중 심볼 차트 데이터 조회
                    val chartData = localClient.getChartData(
                        symbols = listOf("AAPL", "GOOGL", "TSLA"),
                        interval = Interval.OneDay,
                        period = Period.OneMonth
                    )

                    // Then: 모든 심볼의 데이터 검증
                    assertThat(chartData.size).isEqualTo(3)
                    chartData.values.forEach { assertThat(it).isNotEmpty }

                    chartData.mapValues { (_, data) -> data }
                } finally {
                    localClient.close()
                }
            }
        }
    }

    // ===== Quote Operations Tests =====
    @Nested
    @DisplayName("Quote API - 주식 정보 조회")
    inner class QuoteOperationsTests {

        @Nested
        @DisplayName("단일 심볼 조회")
        inner class SingleSymbolQuoteTests {

            @Test
            @DisplayName("AAPL의 Quote Summary를 조회한다")
            fun testCanFetchQuoteSummary() = liveTestWithRecording(
                category = RecordingConfig.Paths.Yahoo.Quote.SUMMARY,
                fileName = "aapl_summary_client"
            ) {
                // Given: UFC Client 생성
                val localClient = UFCClient.create(UFCClientConfig())

                try {
                    // When: AAPL Quote Summary 조회
                    val summary = localClient.getQuoteSummary("AAPL")

                    // Then: Quote 정보 검증
                    assertThat(summary).isNotNull
                    assertThat(summary.price).isNotNull

                    summary
                } finally {
                    localClient.close()
                }
            }

            @Test
            @DisplayName("AAPL의 Stock Summary를 조회한다")
            fun testCanFetchStockSummary() = liveTestWithRecording(
                category = RecordingConfig.Paths.Yahoo.Quote.SUMMARY,
                fileName = "aapl_stock_summary_client"
            ) {
                // Given: UFC Client 생성
                val localClient = UFCClient.create(UFCClientConfig())

                try {
                    // When: AAPL Stock Summary 조회
                    val stockSummary = localClient.getStockSummary("AAPL")

                    // Then: Stock 정보 검증
                    assertThat(stockSummary).isNotNull
                    assertThat(stockSummary.symbol).isEqualTo("AAPL")
                    assertThat(stockSummary.currency).isNotNull
                    assertThat(stockSummary.exchange).isNotNull

                    stockSummary
                } finally {
                    localClient.close()
                }
            }

            @Test
            @DisplayName("TSLA의 Stock Summary를 조회한다")
            fun testCanFetchTslaData() = liveTestWithRecording(
                category = RecordingConfig.Paths.Yahoo.Quote.SUMMARY,
                fileName = "tsla_summary_client"
            ) {
                // Given: UFC Client 생성
                val localClient = UFCClient.create(UFCClientConfig())

                try {
                    // When: TSLA Stock Summary 조회
                    val stockSummary = localClient.getStockSummary("TSLA")

                    // Then: Stock 정보 검증
                    assertThat(stockSummary).isNotNull
                    assertThat(stockSummary.symbol).isEqualTo("TSLA")

                    stockSummary
                } finally {
                    localClient.close()
                }
            }
        }

        @Nested
        @DisplayName("고급 조회 - 특정 모듈 및 다중 심볼")
        inner class AdvancedQuoteTests {

            @Test
            @DisplayName("AAPL의 Quote Summary를 특정 모듈로 조회한다")
            fun testCanFetchQuoteSummaryWithSpecificModules() = liveTestWithRecording(
                category = RecordingConfig.Paths.Yahoo.Quote.SUMMARY,
                fileName = "aapl_summary_with_modules"
            ) {
                // Given: UFC Client 생성
                val localClient = UFCClient.create(UFCClientConfig())

                try {
                    // When: AAPL Quote Summary를 특정 모듈로 조회
                    val summary = localClient.getQuoteSummary(
                        symbol = "AAPL",
                        modules = listOf("price", "summaryDetail")
                    )

                    // Then: Quote 정보 검증
                    assertThat(summary).isNotNull
                    assertThat(summary.price).isNotNull

                    summary
                } finally {
                    localClient.close()
                }
            }

            @Test
            @DisplayName("여러 심볼(AAPL, GOOGL)의 Stock Summary를 동시 조회한다")
            fun testCanFetchMultipleStockSummaries() = liveTestWithRecording(
                category = RecordingConfig.Paths.Yahoo.Quote.SUMMARY,
                fileName = "multi_stock_summaries"
            ) {
                // Given: UFC Client 생성
                val localClient = UFCClient.create(UFCClientConfig())

                try {
                    // When: 다중 심볼 Stock Summary 조회
                    val summaries = localClient.getStockSummary(
                        symbols = listOf("AAPL", "GOOGL")
                    )

                    // Then: 모든 심볼의 정보 검증
                    assertThat(summaries.size).isEqualTo(2)
                    summaries.values.forEach { assertThat(it.symbol).isNotNull }

                    summaries
                } finally {
                    localClient.close()
                }
            }
        }
    }

    // ===== Rate Limiting Tests =====
    @Nested
    @DisplayName("Rate Limiting - 레이트 제한 및 연속 요청")
    inner class RateLimitingAndContinuousTests {

        @Test
        @DisplayName("클라이언트의 Rate Limiter 상태를 조회한다")
        fun testRateLimiterStatus() = runTest {
            // Given: UFC Client 생성
            val localClient = UFCClient.create(UFCClientConfig())

            try {
                // When: Rate Limiter 상태 조회
                val rateLimiterStatus = localClient.getRateLimiterStatus()

                // Then: Rate Limiter 상태 검증
                assertThat(rateLimiterStatus.availableTokens).isNotEqualTo(0)
                assertThat(rateLimiterStatus.capacity).isNotEqualTo(0)
            } finally {
                // Finally: 클라이언트 종료
                localClient.close()
            }
        }

        @Test
        @DisplayName("연속된 서로 다른 API 호출들이 Rate Limiter에 의해 정상 처리된다")
        fun testSequentialRequests() = runTest {
            // Given: UFC Client 생성
            val localClient = UFCClient.create(UFCClientConfig())

            try {
                // When: 순차적으로 서로 다른 API 호출
                val chartData1 = localClient.getChartData(
                    symbol = "AAPL",
                    period = Period.OneMonth
                )
                assertThat(chartData1).isNotEmpty

                val chartData2 = localClient.getChartData(
                    symbol = "GOOGL",
                    period = Period.OneMonth
                )
                assertThat(chartData2).isNotEmpty

                val summary = localClient.getStockSummary("TSLA")

                // Then: 모든 요청이 성공
                assertThat(summary).isNotNull
            } finally {
                // Finally: 클라이언트 종료
                localClient.close()
            }
        }
    }
}
