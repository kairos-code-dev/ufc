package com.ulalax.ufc.examples

import com.ulalax.ufc.api.Ufc
import com.ulalax.ufc.domain.exception.UfcException
import com.ulalax.ufc.domain.model.chart.ChartEventType
import com.ulalax.ufc.domain.model.chart.Interval
import com.ulalax.ufc.domain.model.chart.Period
import kotlinx.coroutines.runBlocking
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Historical Chart Data Example
 *
 * This example demonstrates how to retrieve historical OHLCV (Open, High, Low, Close, Volume)
 * data using UFC. It covers:
 * 1. Daily chart data with different periods
 * 2. Intraday data with different intervals
 * 3. Retrieving dividend and split events
 * 4. Basic technical analysis (moving averages, volatility)
 */
fun main() =
    runBlocking {
        println("=".repeat(80))
        println("UFC Chart Example - Historical Market Data")
        println("=".repeat(80))
        println()

        // Create UFC client instance
        Ufc.create().use { ufc ->
            try {
                // Example 1: Daily chart data for 1 year
                dailyChartData(ufc)
                println()

                // Example 2: Intraday chart data
                intradayChartData(ufc)
                println()

                // Example 3: Chart data with dividends and splits
                chartWithEvents(ufc)
                println()

                // Example 4: Basic technical analysis
                technicalAnalysis(ufc)
            } catch (e: UfcException) {
                println("Error: ${e.message}")
                println("Error Code: ${e.errorCode}")
            } catch (e: Exception) {
                println("Unexpected error: ${e.message}")
                e.printStackTrace()
            }
        }

        println()
        println("=".repeat(80))
        println("Example completed successfully")
        println("=".repeat(80))
    }

/**
 * Example 1: Retrieve daily chart data for different periods
 */
suspend fun dailyChartData(ufc: Ufc) {
    println("-".repeat(80))
    println("Example 1: Daily Chart Data")
    println("-".repeat(80))

    val symbol = "AAPL"
    println("Fetching daily chart data for $symbol (1 Year)...")
    println()

    // Fetch daily data for 1 year
    val chartData =
        ufc.chart(
            symbol = symbol,
            interval = Interval.OneDay,
            period = Period.OneYear,
        )

    // Display metadata
    chartData.meta.let { meta ->
        println("=== Chart Metadata ===")
        println("Symbol: ${meta.symbol}")
        println("Currency: ${meta.currency}")
        println("Exchange: ${meta.exchange}")
        println("Current Price: ${meta.regularMarketPrice?.let { "$%.2f".format(it) } ?: "N/A"}")
        println("52-Week High: ${meta.fiftyTwoWeekHigh?.let { "$%.2f".format(it) } ?: "N/A"}")
        println("52-Week Low: ${meta.fiftyTwoWeekLow?.let { "$%.2f".format(it) } ?: "N/A"}")
        println()
    }

    // Display sample data (first 10 and last 10 days)
    println("=== Daily Price Data (First 10 Days) ===")
    println("%-12s %10s %10s %10s %10s %15s".format("Date", "Open", "High", "Low", "Close", "Volume"))
    println("-".repeat(80))

    val dateFormatter = SimpleDateFormat("yyyy-MM-dd")
    val priceFormatter = { d: Double -> "$%.2f".format(d) }
    val volumeFormatter = { v: Long ->
        val millions = v / 1_000_000.0
        "%.2fM".format(millions)
    }

    chartData.prices.take(10).forEach { ohlcv ->
        val date = dateFormatter.format(Date(ohlcv.timestamp * 1000))
        val open = priceFormatter(ohlcv.open)
        val high = priceFormatter(ohlcv.high)
        val low = priceFormatter(ohlcv.low)
        val close = priceFormatter(ohlcv.close)
        val volume = volumeFormatter(ohlcv.volume)

        println("%-12s %10s %10s %10s %10s %15s".format(date, open, high, low, close, volume))
    }

    println()
    println("... (${chartData.prices.size - 20} more days)")
    println()

    // Display last 10 days
    println("=== Daily Price Data (Last 10 Days) ===")
    println("%-12s %10s %10s %10s %10s %15s %10s".format("Date", "Open", "High", "Low", "Close", "Volume", "Change"))
    println("-".repeat(90))

    chartData.prices.takeLast(10).forEach { ohlcv ->
        val date = dateFormatter.format(Date(ohlcv.timestamp * 1000))
        val open = priceFormatter(ohlcv.open)
        val high = priceFormatter(ohlcv.high)
        val low = priceFormatter(ohlcv.low)
        val close = priceFormatter(ohlcv.close)
        val volume = volumeFormatter(ohlcv.volume)
        val changePercent =
            if (ohlcv.changePercent() >= 0) {
                "+%.2f%%".format(ohlcv.changePercent())
            } else {
                "%.2f%%".format(ohlcv.changePercent())
            }

        println("%-12s %10s %10s %10s %10s %15s %10s".format(date, open, high, low, close, volume, changePercent))
    }

    // Display summary statistics
    println()
    println("=== Summary Statistics ===")
    val totalDays = chartData.prices.size
    val avgVolume = chartData.prices.map { it.volume }.average().toLong()
    val maxHigh = chartData.prices.maxOf { it.high }
    val minLow = chartData.prices.minOf { it.low }
    val bullishDays = chartData.prices.count { it.isBullish() }
    val bearishDays = chartData.prices.count { it.isBearish() }

    println("Total Trading Days: $totalDays")
    println("Average Volume: ${volumeFormatter(avgVolume)}")
    println("Highest Price: ${priceFormatter(maxHigh)}")
    println("Lowest Price: ${priceFormatter(minLow)}")
    println("Bullish Days: $bullishDays (${String.format("%.1f%%", bullishDays * 100.0 / totalDays)})")
    println("Bearish Days: $bearishDays (${String.format("%.1f%%", bearishDays * 100.0 / totalDays)})")
}

/**
 * Example 2: Retrieve intraday chart data with different intervals
 */
suspend fun intradayChartData(ufc: Ufc) {
    println("-".repeat(80))
    println("Example 2: Intraday Chart Data")
    println("-".repeat(80))

    val symbol = "TSLA"
    println("Fetching 5-minute interval data for $symbol (Last 5 Days)...")
    println()

    // Fetch 5-minute interval data for last 5 days
    val chartData =
        ufc.chart(
            symbol = symbol,
            interval = Interval.FiveMinutes,
            period = Period.FiveDays,
        )

    println("=== Intraday Data Summary ===")
    println("Symbol: ${chartData.meta.symbol}")
    println("Interval: 5 minutes")
    println("Period: 5 days")
    println("Total Data Points: ${chartData.prices.size}")
    println()

    // Display sample data from the most recent trading session
    println("=== Recent Trading Session (Last 20 Intervals) ===")
    println("%-20s %10s %10s %10s %10s %12s".format("Time", "Open", "High", "Low", "Close", "Volume"))
    println("-".repeat(80))

    val timeFormatter = SimpleDateFormat("MM-dd HH:mm")
    val priceFormatter = { d: Double -> "$%.2f".format(d) }
    val volumeFormatter = { v: Long -> NumberFormat.getNumberInstance(Locale.US).format(v) }

    chartData.prices.takeLast(20).forEach { ohlcv ->
        val time = timeFormatter.format(Date(ohlcv.timestamp * 1000))
        val open = priceFormatter(ohlcv.open)
        val high = priceFormatter(ohlcv.high)
        val low = priceFormatter(ohlcv.low)
        val close = priceFormatter(ohlcv.close)
        val volume = volumeFormatter(ohlcv.volume)

        println("%-20s %10s %10s %10s %10s %12s".format(time, open, high, low, close, volume))
    }

    println()
    println("Tip: Use different intervals for different analysis needs:")
    println("     - 1m, 2m, 5m: Day trading and scalping")
    println("     - 15m, 30m, 1h: Intraday swing trading")
    println("     - 1d: Daily analysis and position trading")
    println("     - 1wk, 1mo: Long-term trend analysis")
}

/**
 * Example 3: Retrieve chart data with dividend and split events
 */
suspend fun chartWithEvents(ufc: Ufc) {
    println("-".repeat(80))
    println("Example 3: Chart Data with Dividends and Splits")
    println("-".repeat(80))

    val symbol = "AAPL"
    println("Fetching chart data with events for $symbol (5 Years)...")
    println()

    // Fetch chart data with dividend and split events
    val chartData =
        ufc.chart(
            symbol = symbol,
            interval = Interval.OneDay,
            period = Period.FiveYears,
            ChartEventType.DIVIDEND,
            ChartEventType.SPLIT,
        )

    println("=== Chart Data Summary ===")
    println("Symbol: ${chartData.meta.symbol}")
    println("Period: 5 years")
    println("Total Trading Days: ${chartData.prices.size}")
    println()

    // Display dividend events
    val dividends = chartData.getDividends()
    if (dividends != null && dividends.isNotEmpty()) {
        println("=== Dividend Events ===")
        println("%-12s %15s".format("Date", "Amount"))
        println("-".repeat(30))

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd")
        dividends.entries.sortedByDescending { it.key }.take(10).forEach { (_, event) ->
            event.date?.let { timestamp ->
                val date = dateFormatter.format(Date(timestamp * 1000))
                val amount = event.amount?.let { "$%.2f".format(it) } ?: "N/A"
                println("%-12s %15s".format(date, amount))
            }
        }

        println()
        println("Total Dividends: ${dividends.size}")
        val totalDividendAmount = dividends.values.mapNotNull { it.amount }.sum()
        println("Total Dividend Amount: $%.2f".format(totalDividendAmount))
        println()
    } else {
        println("No dividend events found in this period")
        println()
    }

    // Display split events
    val splits = chartData.getSplits()
    if (splits != null && splits.isNotEmpty()) {
        println("=== Stock Split Events ===")
        println("%-12s %15s %20s".format("Date", "Split Ratio", "Details"))
        println("-".repeat(50))

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd")
        splits.entries.sortedByDescending { it.key }.forEach { (_, event) ->
            event.date?.let { timestamp ->
                val date = dateFormatter.format(Date(timestamp * 1000))
                val ratio = event.splitRatio ?: "N/A"
                val numerator = event.numerator
                val denominator = event.denominator
                val details =
                    if (numerator != null && denominator != null) {
                        "${numerator.toInt()}:${denominator.toInt()} split"
                    } else {
                        "Details N/A"
                    }
                println("%-12s %15s %20s".format(date, ratio, details))
            }
        }

        println()
        println("Total Splits: ${splits.size}")
    } else {
        println("No split events found in this period")
    }
}

/**
 * Example 4: Basic technical analysis using chart data
 */
suspend fun technicalAnalysis(ufc: Ufc) {
    println("-".repeat(80))
    println("Example 4: Basic Technical Analysis")
    println("-".repeat(80))

    val symbol = "MSFT"
    println("Fetching data for technical analysis: $symbol (6 Months)...")
    println()

    val chartData =
        ufc.chart(
            symbol = symbol,
            interval = Interval.OneDay,
            period = Period.SixMonths,
        )

    val prices = chartData.prices
    val closes = prices.map { it.close }

    println("=== Technical Indicators ===")
    println()

    // Calculate Simple Moving Averages (SMA)
    if (closes.size >= 50) {
        val sma20 = closes.takeLast(20).average()
        val sma50 = closes.takeLast(50).average()
        val currentPrice = closes.last()

        println("Simple Moving Averages:")
        println("  20-day SMA: $%.2f".format(sma20))
        println("  50-day SMA: $%.2f".format(sma50))
        println("  Current Price: $%.2f".format(currentPrice))
        println()

        // Determine trend
        val trend =
            when {
                currentPrice > sma20 && sma20 > sma50 -> "Strong Uptrend"
                currentPrice > sma20 -> "Uptrend"
                currentPrice < sma20 && sma20 < sma50 -> "Strong Downtrend"
                currentPrice < sma20 -> "Downtrend"
                else -> "Sideways"
            }
        println("  Trend Indication: $trend")
        println()
    }

    // Calculate Volatility (Standard Deviation of Returns)
    if (prices.size >= 20) {
        val returns =
            prices.zipWithNext { a, b ->
                (b.close - a.close) / a.close * 100.0
            }
        val avgReturn = returns.average()
        val variance = returns.map { (it - avgReturn) * (it - avgReturn) }.average()
        val stdDev = Math.sqrt(variance)

        println("Volatility Analysis (Last 20 Days):")
        println("  Average Daily Return: %.2f%%".format(avgReturn))
        println("  Standard Deviation: %.2f%%".format(stdDev))
        println("  Annualized Volatility: %.2f%%".format(stdDev * Math.sqrt(252.0)))
        println()
    }

    // Price Range Analysis
    val currentPrice = prices.last().close
    val highestPrice = prices.maxOf { it.high }
    val lowestPrice = prices.minOf { it.low }
    val priceRange = highestPrice - lowestPrice
    val pricePosition = (currentPrice - lowestPrice) / priceRange * 100

    println("Price Range Analysis:")
    println("  Highest: $%.2f".format(highestPrice))
    println("  Lowest: $%.2f".format(lowestPrice))
    println("  Current: $%.2f".format(currentPrice))
    println("  Position in Range: %.1f%%".format(pricePosition))
    println()

    // Volume Analysis
    val avgVolume = prices.map { it.volume }.average().toLong()
    val recentVolume = prices.takeLast(5).map { it.volume }.average().toLong()
    val volumeRatio = recentVolume.toDouble() / avgVolume.toDouble()

    println("Volume Analysis:")
    println("  Average Volume: ${NumberFormat.getNumberInstance(Locale.US).format(avgVolume)}")
    println("  Recent 5-Day Avg: ${NumberFormat.getNumberInstance(Locale.US).format(recentVolume)}")
    println("  Volume Ratio: %.2fx".format(volumeRatio))
    println(
        "  Status: ${if (volumeRatio > 1.2) {
            "Above Average"
        } else if (volumeRatio < 0.8) {
            "Below Average"
        } else {
            "Normal"
        }}",
    )
    println()

    println("Note: These are basic technical indicators for educational purposes.")
    println("      For production use, consider using specialized technical analysis libraries.")
}
