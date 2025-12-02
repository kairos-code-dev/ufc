package com.ulalax.ufc.client

import com.ulalax.ufc.model.common.Interval
import com.ulalax.ufc.model.common.Period
import com.ulalax.ufc.utils.LiveTestBase
import com.ulalax.ufc.utils.RecordingConfig
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * UFCClient Live Test
 *
 * 실제 Yahoo Finance API와의 통신을 테스트합니다.
 */
class UFCClientLiveTest : LiveTestBase() {

    @Test
    fun testClientCreationAndClosure() = runTest {
        val localClient = UFCClient.create(UFCClientConfig())

        assertThat(localClient).isNotNull

        localClient.close()
    }

    @Test
    fun testClientStatus() = runTest {
        val localClient = UFCClient.create(UFCClientConfig())

        val status = localClient.getStatus()
        assertThat(status).isNotBlank()

        localClient.close()
    }

    @Test
    fun testRateLimiterStatus() = runTest {
        val localClient = UFCClient.create(UFCClientConfig())

        val rateLimiterStatus = localClient.getRateLimiterStatus()
        assertThat(rateLimiterStatus.availableTokens).isNotEqualTo(0)
        assertThat(rateLimiterStatus.capacity).isNotEqualTo(0)

        localClient.close()
    }

    @Test
    fun testCanFetchAaplDailyChartData() = liveTestWithRecording(
        category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
        fileName = "aapl_daily_1m_client"
    ) {
        val localClient = UFCClient.create(UFCClientConfig())

        try {
            val chartData = localClient.getChartData(
                symbol = "AAPL",
                interval = Interval.OneDay,
                period = Period.OneMonth
            )

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
    fun testCanFetchHourlyChartData() = liveTestWithRecording(
        category = RecordingConfig.Paths.Yahoo.Chart.INTRADAY,
        fileName = "googl_hourly_5d_client"
    ) {
        val localClient = UFCClient.create(UFCClientConfig())

        try {
            val chartData = localClient.getChartData(
                symbol = "GOOGL",
                interval = Interval.OneHour,
                period = Period.FiveDays
            )

            assertThat(chartData).isNotEmpty

            chartData
        } finally {
            localClient.close()
        }
    }

    @Test
    fun testCanFetchMultipleSymbolsChartData() = liveTestWithRecording(
        category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
        fileName = "multi_symbols_daily_1m"
    ) {
        val localClient = UFCClient.create(UFCClientConfig())

        try {
            val chartData = localClient.getChartData(
                symbols = listOf("AAPL", "GOOGL", "TSLA"),
                interval = Interval.OneDay,
                period = Period.OneMonth
            )

            assertThat(chartData.size).isEqualTo(3)
            chartData.values.forEach { assertThat(it).isNotEmpty }

            chartData.mapValues { (_, data) -> data }
        } finally {
            localClient.close()
        }
    }

    @Test
    fun testCanFetchQuoteSummary() = liveTestWithRecording(
        category = RecordingConfig.Paths.Yahoo.Quote.SUMMARY,
        fileName = "aapl_summary_client"
    ) {
        val localClient = UFCClient.create(UFCClientConfig())

        try {
            val summary = localClient.getQuoteSummary("AAPL")

            assertThat(summary).isNotNull
            assertThat(summary.price).isNotNull

            summary
        } finally {
            localClient.close()
        }
    }

    @Test
    fun testCanFetchStockSummary() = liveTestWithRecording(
        category = RecordingConfig.Paths.Yahoo.Quote.SUMMARY,
        fileName = "aapl_stock_summary_client"
    ) {
        val localClient = UFCClient.create(UFCClientConfig())

        try {
            val stockSummary = localClient.getStockSummary("AAPL")

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
    fun testCanFetchQuoteSummaryWithSpecificModules() = liveTestWithRecording(
        category = RecordingConfig.Paths.Yahoo.Quote.SUMMARY,
        fileName = "aapl_summary_with_modules"
    ) {
        val localClient = UFCClient.create(UFCClientConfig())

        try {
            val summary = localClient.getQuoteSummary(
                symbol = "AAPL",
                modules = listOf("price", "summaryDetail")
            )

            assertThat(summary).isNotNull
            assertThat(summary.price).isNotNull

            summary
        } finally {
            localClient.close()
        }
    }

    @Test
    fun testCanFetchMultipleStockSummaries() = liveTestWithRecording(
        category = RecordingConfig.Paths.Yahoo.Quote.SUMMARY,
        fileName = "multi_stock_summaries"
    ) {
        val localClient = UFCClient.create(UFCClientConfig())

        try {
            val summaries = localClient.getStockSummary(
                symbols = listOf("AAPL", "GOOGL")
            )

            assertThat(summaries.size).isEqualTo(2)
            summaries.values.forEach { assertThat(it.symbol).isNotNull }

            summaries
        } finally {
            localClient.close()
        }
    }

    @Test
    fun testCanFetchIndexData() = liveTestWithRecording(
        category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
        fileName = "gspc_daily_3m_client"
    ) {
        val localClient = UFCClient.create(UFCClientConfig())

        try {
            val chartData = localClient.getChartData(
                symbol = "^GSPC",
                interval = Interval.OneDay,
                period = Period.ThreeMonths
            )

            assertThat(chartData).isNotEmpty

            chartData
        } finally {
            localClient.close()
        }
    }

    @Test
    fun testCanFetchTslaData() = liveTestWithRecording(
        category = RecordingConfig.Paths.Yahoo.Quote.SUMMARY,
        fileName = "tsla_summary_client"
    ) {
        val localClient = UFCClient.create(UFCClientConfig())

        try {
            val stockSummary = localClient.getStockSummary("TSLA")

            assertThat(stockSummary).isNotNull
            assertThat(stockSummary.symbol).isEqualTo("TSLA")

            stockSummary
        } finally {
            localClient.close()
        }
    }

    @Test
    fun testCanFetchOneDayChartData() = liveTestWithRecording(
        category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
        fileName = "aapl_daily_1d"
    ) {
        val localClient = UFCClient.create(UFCClientConfig())

        try {
            val chartData = localClient.getChartData(
                symbol = "AAPL",
                interval = Interval.OneDay,
                period = Period.OneDay
            )

            assertThat(chartData).isNotEmpty

            chartData
        } finally {
            localClient.close()
        }
    }

    @Test
    fun testCanFetchOneYearChartData() = liveTestWithRecording(
        category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
        fileName = "aapl_daily_1y_client"
    ) {
        val localClient = UFCClient.create(UFCClientConfig())

        try {
            val chartData = localClient.getChartData(
                symbol = "AAPL",
                interval = Interval.OneDay,
                period = Period.OneYear
            )

            assertThat(chartData).isNotEmpty
            assertThat(chartData.size).isNotEqualTo(0)

            chartData
        } finally {
            localClient.close()
        }
    }

    @Test
    fun testCanFetchMaxPeriodChartData() = liveTestWithRecording(
        category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
        fileName = "aapl_daily_max_client"
    ) {
        val localClient = UFCClient.create(UFCClientConfig())

        try {
            val chartData = localClient.getChartData(
                symbol = "AAPL",
                interval = Interval.OneDay,
                period = Period.Max
            )

            assertThat(chartData).isNotEmpty
            assertThat(chartData.size).isNotEqualTo(0)

            chartData
        } finally {
            localClient.close()
        }
    }

    @Test
    fun testClientConfig() = runTest {
        val config = UFCClientConfig(
            fredApiKey = "test-key"
        )
        val localClient = UFCClient.create(config)

        try {
            val retrievedConfig = localClient.getConfig()
            assertThat(retrievedConfig.fredApiKey).isEqualTo("test-key")
        } finally {
            localClient.close()
        }
    }

    @Test
    fun testSequentialRequests() = runTest {
        val localClient = UFCClient.create(UFCClientConfig())

        try {
            // 첫 번째 요청
            val chartData1 = localClient.getChartData(
                symbol = "AAPL",
                period = Period.OneMonth
            )
            assertThat(chartData1).isNotEmpty

            // 두 번째 요청
            val chartData2 = localClient.getChartData(
                symbol = "GOOGL",
                period = Period.OneMonth
            )
            assertThat(chartData2).isNotEmpty

            // 세 번째 요청
            val summary = localClient.getStockSummary("TSLA")
            assertThat(summary).isNotNull
        } finally {
            localClient.close()
        }
    }
}
