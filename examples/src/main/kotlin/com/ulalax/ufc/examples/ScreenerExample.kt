package com.ulalax.ufc.examples

import com.ulalax.ufc.api.Ufc
import com.ulalax.ufc.domain.model.screener.*
import com.ulalax.ufc.domain.exception.UfcException
import kotlinx.coroutines.runBlocking
import java.text.NumberFormat
import java.util.*

/**
 * Stock Screener Example
 *
 * This example demonstrates how to use UFC's stock screening capabilities.
 * It covers:
 * 1. Using predefined screeners (day gainers, most actives, etc.)
 * 2. Creating custom queries with filters
 * 3. Sorting and pagination
 * 4. Combining multiple conditions
 */
fun main() = runBlocking {
    println("=".repeat(80))
    println("UFC Screener Example - Stock Screening")
    println("=".repeat(80))
    println()

    // Create UFC client instance
    Ufc.create().use { ufc ->
        try {
            // Example 1: Predefined screeners
            predefinedScreeners(ufc)
            println()

            // Example 2: Custom query - Large cap tech stocks
            customQueryLargeCap(ufc)
            println()

            // Example 3: Custom query - Value stocks
            customQueryValueStocks(ufc)
            println()

            // Example 4: Custom query - High dividend yields
            customQueryHighDividend(ufc)

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
 * Example 1: Use predefined screeners provided by Yahoo Finance
 */
suspend fun predefinedScreeners(ufc: Ufc) {
    println("-".repeat(80))
    println("Example 1: Predefined Screeners")
    println("-".repeat(80))
    println()

    // Day Gainers - Stocks with highest percentage gains today
    println("=== Day Gainers (Top 10) ===")
    val dayGainers = ufc.screener(
        predefined = PredefinedScreener.DAY_GAINERS,
        count = 10
    )

    println("Total matches: ${dayGainers.total}")
    println()
    println("%-8s %-40s %12s %12s %15s".format("Symbol", "Name", "Price", "Change", "Volume"))
    println("-".repeat(80))

    dayGainers.quotes.forEach { quote ->
        val symbol = quote.symbol ?: "N/A"
        val name = quote.shortName?.take(37) ?: "N/A"
        val price = quote.regularMarketPrice?.let { "$%.2f".format(it) } ?: "N/A"
        val changePercent = quote.regularMarketChangePercent?.let { "+%.2f%%".format(it) } ?: "N/A"
        val volume = quote.regularMarketVolume?.let {
            val millions = it / 1_000_000.0
            "%.2fM".format(millions)
        } ?: "N/A"

        println("%-8s %-40s %12s %12s %15s".format(symbol, name, price, changePercent, volume))
    }
    println()

    // Most Actives - Stocks with highest trading volume
    println("=== Most Active Stocks (Top 10) ===")
    val mostActives = ufc.screener(
        predefined = PredefinedScreener.MOST_ACTIVES,
        count = 10
    )

    println("Total matches: ${mostActives.total}")
    println()
    println("%-8s %-40s %12s %12s %15s".format("Symbol", "Name", "Price", "Change", "Volume"))
    println("-".repeat(80))

    mostActives.quotes.forEach { quote ->
        val symbol = quote.symbol ?: "N/A"
        val name = quote.shortName?.take(37) ?: "N/A"
        val price = quote.regularMarketPrice?.let { "$%.2f".format(it) } ?: "N/A"
        val changePercent = quote.regularMarketChangePercent?.let {
            val sign = if (it >= 0) "+" else ""
            "$sign%.2f%%".format(it)
        } ?: "N/A"
        val volume = quote.regularMarketVolume?.let {
            val millions = it / 1_000_000.0
            "%.2fM".format(millions)
        } ?: "N/A"

        println("%-8s %-40s %12s %12s %15s".format(symbol, name, price, changePercent, volume))
    }
    println()

    println("Available predefined screeners:")
    println("  - DAY_GAINERS: Stocks with highest percentage gains")
    println("  - DAY_LOSERS: Stocks with highest percentage losses")
    println("  - MOST_ACTIVES: Stocks with highest trading volume")
    println("  - AGGRESSIVE_SMALL_CAPS: Small cap stocks with high growth potential")
    println("  - GROWTH_TECHNOLOGY_STOCKS: Technology stocks with strong growth")
    println("  - UNDERVALUED_GROWTH_STOCKS: Growth stocks that appear undervalued")
}

/**
 * Example 2: Create a custom query to find large cap tech stocks
 */
suspend fun customQueryLargeCap(ufc: Ufc) {
    println("-".repeat(80))
    println("Example 2: Custom Query - Large Cap Tech Stocks")
    println("-".repeat(80))
    println()

    // Build query: Large cap tech stocks with reasonable PE ratio
    // Market Cap > $50B AND Sector = Technology AND PE Ratio < 35
    val query = EquityQuery.and(
        EquityQuery.gt(EquityField.INTRADAY_MARKET_CAP, 50_000_000_000L),
        EquityQuery.eq(EquityField.SECTOR, "Technology"),
        EquityQuery.lt(EquityField.PE_RATIO, 35)
    )

    println("Query criteria:")
    println("  - Market Cap > $50B")
    println("  - Sector = Technology")
    println("  - P/E Ratio < 35")
    println()

    val result = ufc.screener(
        query = query,
        sortField = ScreenerSortField.MARKET_CAP,
        sortAsc = false,
        size = 15
    )

    println("Total matches: ${result.total}")
    println()
    println("%-8s %-35s %12s %10s %12s".format("Symbol", "Name", "Market Cap", "P/E", "Price"))
    println("-".repeat(80))

    result.quotes.forEach { quote ->
        val symbol = quote.symbol ?: "N/A"
        val name = quote.shortName?.take(32) ?: "N/A"
        val marketCap = quote.marketCap?.let {
            val billions = it / 1_000_000_000.0
            "$%.2fB".format(billions)
        } ?: "N/A"
        val pe = (quote.additionalFields["trailingPE"] as? Double)?.let { "%.2f".format(it) } ?: "N/A"
        val price = quote.regularMarketPrice?.let { "$%.2f".format(it) } ?: "N/A"

        println("%-8s %-35s %12s %10s %12s".format(symbol, name, marketCap, pe, price))
    }
}

/**
 * Example 3: Create a custom query to find value stocks
 */
suspend fun customQueryValueStocks(ufc: Ufc) {
    println("-".repeat(80))
    println("Example 3: Custom Query - Value Stocks")
    println("-".repeat(80))
    println()

    // Build query: Value stocks with good fundamentals
    // PE Ratio between 5 and 15 AND P/B Ratio < 2 AND Market Cap > $1B
    val query = EquityQuery.and(
        EquityQuery.between(EquityField.PE_RATIO, 5, 15),
        EquityQuery.lt(EquityField.PRICE_BOOK_RATIO, 2),
        EquityQuery.gt(EquityField.INTRADAY_MARKET_CAP, 1_000_000_000L)
    )

    println("Query criteria (Value investing):")
    println("  - P/E Ratio: 5-15 (undervalued)")
    println("  - P/B Ratio < 2 (trading below 2x book value)")
    println("  - Market Cap > $1B (avoid micro-caps)")
    println()

    val result = ufc.screener(
        query = query,
        sortField = ScreenerSortField.MARKET_CAP,
        sortAsc = false,
        size = 20
    )

    println("Total matches: ${result.total}")
    println()
    println("%-8s %-30s %12s %8s %8s %12s".format("Symbol", "Name", "Market Cap", "P/E", "P/B", "Price"))
    println("-".repeat(80))

    result.quotes.forEach { quote ->
        val symbol = quote.symbol ?: "N/A"
        val name = quote.shortName?.take(27) ?: "N/A"
        val marketCap = quote.marketCap?.let {
            val billions = it / 1_000_000_000.0
            if (billions >= 1.0) {
                "$%.2fB".format(billions)
            } else {
                val millions = it / 1_000_000.0
                "$%.0fM".format(millions)
            }
        } ?: "N/A"
        val pe = (quote.additionalFields["trailingPE"] as? Double)?.let { "%.2f".format(it) } ?: "N/A"
        val pb = (quote.additionalFields["priceToBook"] as? Double)?.let { "%.2f".format(it) } ?: "N/A"
        val price = quote.regularMarketPrice?.let { "$%.2f".format(it) } ?: "N/A"

        println("%-8s %-30s %12s %8s %8s %12s".format(symbol, name, marketCap, pe, pb, price))
    }
    println()

    if (result.total > 0) {
        println("Tip: These stocks appear undervalued based on traditional metrics.")
        println("     Always perform additional due diligence before investing.")
    }
}

/**
 * Example 4: Create a custom query to find high dividend yield stocks
 */
suspend fun customQueryHighDividend(ufc: Ufc) {
    println("-".repeat(80))
    println("Example 4: Custom Query - High Dividend Yield Stocks")
    println("-".repeat(80))
    println()

    // Build query: High dividend yield stocks with good fundamentals
    // Dividend Yield > 3% AND Market Cap > $5B AND Region = US
    val query = EquityQuery.and(
        EquityQuery.gt(EquityField.FORWARD_DIVIDEND_YIELD, 0.03),
        EquityQuery.gt(EquityField.INTRADAY_MARKET_CAP, 5_000_000_000L),
        EquityQuery.eq(EquityField.REGION, "us")
    )

    println("Query criteria (Income investing):")
    println("  - Dividend Yield > 3%")
    println("  - Market Cap > $5B (established companies)")
    println("  - Region = United States")
    println()

    val result = ufc.screener(
        query = query,
        sortField = ScreenerSortField.MARKET_CAP,
        sortAsc = false,
        size = 15
    )

    println("Total matches: ${result.total}")
    println()
    println("%-8s %-32s %12s %10s %12s %10s".format("Symbol", "Name", "Market Cap", "Yield", "Price", "Sector"))
    println("-".repeat(85))

    result.quotes.forEach { quote ->
        val symbol = quote.symbol ?: "N/A"
        val name = quote.shortName?.take(29) ?: "N/A"
        val marketCap = quote.marketCap?.let {
            val billions = it / 1_000_000_000.0
            "$%.2fB".format(billions)
        } ?: "N/A"
        val yield = (quote.additionalFields["dividendYield"] as? Double)?.let { "%.2f%%".format(it * 100) } ?: "N/A"
        val price = quote.regularMarketPrice?.let { "$%.2f".format(it) } ?: "N/A"
        val sector = quote.sector?.take(10) ?: "N/A"

        println("%-8s %-32s %12s %10s %12s %10s".format(symbol, name, marketCap, yield, price, sector))
    }
    println()

    if (result.total > 0) {
        println("Tip: High dividend yields can indicate:")
        println("     1. Mature, profitable companies distributing earnings")
        println("     2. Potentially undervalued stocks")
        println("     3. Companies in financial distress (be cautious!)")
        println("     Always check dividend sustainability and company fundamentals.")
    }
}
