package examples

import com.ulalax.ufc.api.Ufc
import com.ulalax.ufc.domain.exception.UfcException
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Options Chain Example
 *
 * This example demonstrates how to retrieve and analyze options chain data using UFC.
 * It covers:
 * 1. Retrieving available expiration dates
 * 2. Fetching options chain for specific expiration
 * 3. Finding at-the-money (ATM) options
 * 4. Analyzing calls and puts by strike price
 * 5. Calculating option Greeks and metrics
 */
fun main() = runBlocking {
    println("=".repeat(80))
    println("UFC Options Example - Options Chain Data")
    println("=".repeat(80))
    println()

    // Create UFC client instance
    Ufc.create().use { ufc ->
        try {
            // Example 1: Overview of available expiration dates
            expirationDatesOverview(ufc)
            println()

            // Example 2: Options chain for nearest expiration
            nearestExpirationChain(ufc)
            println()

            // Example 3: At-the-money (ATM) options analysis
            atmOptionsAnalysis(ufc)
            println()

            // Example 4: Compare calls vs puts at different strikes
            callsPutsComparison(ufc)

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
 * Example 1: Overview of available expiration dates
 */
suspend fun expirationDatesOverview(ufc: Ufc) {
    println("-".repeat(80))
    println("Example 1: Available Expiration Dates")
    println("-".repeat(80))

    val symbol = "AAPL"
    println("Fetching options data for $symbol...")
    println()

    // Fetch options data (default: nearest expiration)
    val optionsData = ufc.options(symbol)

    println("=== Options Chain Overview ===")
    println("Underlying Symbol: ${optionsData.underlyingSymbol}")
    println("Current Price: ${optionsData.underlyingQuote?.regularMarketPrice?.let { "$%.2f".format(it) } ?: "N/A"}")
    println("Has Mini Options: ${if (optionsData.hasMiniOptions) "Yes" else "No"}")
    println()

    println("=== Available Expiration Dates ===")
    println("Total: ${optionsData.expirationDates.size}")
    println()

    val dateFormatter = SimpleDateFormat("yyyy-MM-dd (EEE)")
    val now = Instant.now()

    println("%-4s %-20s %15s %10s".format("#", "Date", "Days to Exp", "Type"))
    println("-".repeat(55))

    optionsData.expirationDates.take(12).forEachIndexed { index, timestamp ->
        val date = Instant.ofEpochSecond(timestamp)
        val dateStr = dateFormatter.format(Date.from(date))
        val daysToExpiration = ChronoUnit.DAYS.between(now, date)

        val expType = when {
            daysToExpiration <= 7 -> "Weekly"
            daysToExpiration <= 45 -> "Monthly"
            else -> "LEAPS"
        }

        println("%-4d %-20s %15d %10s".format(index + 1, dateStr, daysToExpiration, expType))
    }

    if (optionsData.expirationDates.size > 12) {
        println("... (${optionsData.expirationDates.size - 12} more expiration dates)")
    }
    println()

    println("Tip: Use different expiration dates based on your strategy:")
    println("     - Weekly (0-7 days): Day trading, short-term speculation")
    println("     - Monthly (1-6 weeks): Swing trading, covered calls")
    println("     - LEAPS (> 1 year): Long-term strategies, stock replacement")
}

/**
 * Example 2: Detailed options chain for nearest expiration
 */
suspend fun nearestExpirationChain(ufc: Ufc) {
    println("-".repeat(80))
    println("Example 2: Options Chain - Nearest Expiration")
    println("-".repeat(80))

    val symbol = "TSLA"
    println("Fetching nearest expiration options for $symbol...")
    println()

    val optionsData = ufc.options(symbol)
    val underlyingPrice = optionsData.underlyingQuote?.regularMarketPrice ?: 0.0

    // Calculate expiration info
    val expirationDate = Instant.ofEpochSecond(optionsData.optionsChain.expirationDate)
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd")
    val daysToExpiration = ChronoUnit.DAYS.between(Instant.now(), expirationDate)

    println("=== Chain Information ===")
    println("Underlying: ${optionsData.underlyingSymbol}")
    println("Current Price: $%.2f".format(underlyingPrice))
    println("Expiration: ${dateFormatter.format(Date.from(expirationDate))} ($daysToExpiration days)")
    println("Total Strikes: ${optionsData.strikes.size}")
    println()

    // Find strikes near the current price
    val nearbyStrikes = optionsData.strikes
        .filter { Math.abs(it - underlyingPrice) / underlyingPrice <= 0.10 } // Within 10%
        .sorted()

    println("=== CALLS (Strikes near current price) ===")
    println("%-10s %10s %10s %10s %12s %10s %8s".format("Strike", "Last", "Bid", "Ask", "Volume", "OI", "IV"))
    println("-".repeat(75))

    nearbyStrikes.forEach { strike ->
        val call = optionsData.optionsChain.findCall(strike)
        if (call != null) {
            val strikeStr = "$%.2f".format(strike)
            val lastStr = call.lastPrice?.let { "$%.2f".format(it) } ?: "N/A"
            val bidStr = call.bid?.let { "$%.2f".format(it) } ?: "N/A"
            val askStr = call.ask?.let { "$%.2f".format(it) } ?: "N/A"
            val volumeStr = call.volume?.toString() ?: "0"
            val oiStr = call.openInterest?.toString() ?: "0"
            val ivStr = call.impliedVolatility?.let { "%.1f%%".format(it * 100) } ?: "N/A"

            val marker = if (Math.abs(strike - underlyingPrice) < 1.0) " *" else ""
            println("%-10s %10s %10s %10s %12s %10s %8s%s".format(
                strikeStr, lastStr, bidStr, askStr, volumeStr, oiStr, ivStr, marker
            ))
        }
    }

    println()
    println("=== PUTS (Strikes near current price) ===")
    println("%-10s %10s %10s %10s %12s %10s %8s".format("Strike", "Last", "Bid", "Ask", "Volume", "OI", "IV"))
    println("-".repeat(75))

    nearbyStrikes.forEach { strike ->
        val put = optionsData.optionsChain.findPut(strike)
        if (put != null) {
            val strikeStr = "$%.2f".format(strike)
            val lastStr = put.lastPrice?.let { "$%.2f".format(it) } ?: "N/A"
            val bidStr = put.bid?.let { "$%.2f".format(it) } ?: "N/A"
            val askStr = put.ask?.let { "$%.2f".format(it) } ?: "N/A"
            val volumeStr = put.volume?.toString() ?: "0"
            val oiStr = put.openInterest?.toString() ?: "0"
            val ivStr = put.impliedVolatility?.let { "%.1f%%".format(it * 100) } ?: "N/A"

            val marker = if (Math.abs(strike - underlyingPrice) < 1.0) " *" else ""
            println("%-10s %10s %10s %10s %12s %10s %8s%s".format(
                strikeStr, lastStr, bidStr, askStr, volumeStr, oiStr, ivStr, marker
            ))
        }
    }

    println()
    println("* = At-the-money (ATM) or near ATM strike")
    println("IV = Implied Volatility, OI = Open Interest")
}

/**
 * Example 3: Analyze at-the-money (ATM) options
 */
suspend fun atmOptionsAnalysis(ufc: Ufc) {
    println("-".repeat(80))
    println("Example 3: At-The-Money (ATM) Options Analysis")
    println("-".repeat(80))

    val symbol = "MSFT"
    println("Analyzing ATM options for $symbol...")
    println()

    val optionsData = ufc.options(symbol)
    val underlyingPrice = optionsData.underlyingQuote?.regularMarketPrice

    if (underlyingPrice == null) {
        println("Unable to determine underlying price")
        return
    }

    val (atmCall, atmPut) = optionsData.findAtTheMoneyOptions()

    if (atmCall == null || atmPut == null) {
        println("Unable to find ATM options")
        return
    }

    println("=== Underlying Asset ===")
    println("Symbol: ${optionsData.underlyingSymbol}")
    println("Current Price: $%.2f".format(underlyingPrice))
    println()

    // Calculate expiration
    val expirationDate = Instant.ofEpochSecond(atmCall.expiration)
    val daysToExpiration = ChronoUnit.DAYS.between(Instant.now(), expirationDate)

    println("=== ATM Call Option ===")
    println("Contract: ${atmCall.contractSymbol}")
    println("Strike: $%.2f".format(atmCall.strike))
    println("Expiration: ${SimpleDateFormat("yyyy-MM-dd").format(Date.from(expirationDate))} ($daysToExpiration days)")
    println()
    println("Last Price: ${atmCall.lastPrice?.let { "$%.2f".format(it) } ?: "N/A"}")
    println("Bid: ${atmCall.bid?.let { "$%.2f".format(it) } ?: "N/A"}")
    println("Ask: ${atmCall.ask?.let { "$%.2f".format(it) } ?: "N/A"}")
    atmCall.getMidPrice()?.let { println("Mid Price: $%.2f".format(it)) }
    atmCall.getBidAskSpread()?.let { println("Bid-Ask Spread: $%.2f".format(it)) }
    println()
    println("Volume: ${atmCall.volume ?: 0}")
    println("Open Interest: ${atmCall.openInterest ?: 0}")
    println("Implied Volatility: ${atmCall.impliedVolatility?.let { "%.2f%%".format(it * 100) } ?: "N/A"}")
    println()

    // Calculate intrinsic and time value
    val callIntrinsic = atmCall.getIntrinsicValue(underlyingPrice, isCall = true)
    val callTimeValue = atmCall.getTimeValue(underlyingPrice, isCall = true)
    println("Intrinsic Value: $%.2f".format(callIntrinsic))
    callTimeValue?.let { println("Time Value: $%.2f".format(it)) }
    println()

    println("=== ATM Put Option ===")
    println("Contract: ${atmPut.contractSymbol}")
    println("Strike: $%.2f".format(atmPut.strike))
    println()
    println("Last Price: ${atmPut.lastPrice?.let { "$%.2f".format(it) } ?: "N/A"}")
    println("Bid: ${atmPut.bid?.let { "$%.2f".format(it) } ?: "N/A"}")
    println("Ask: ${atmPut.ask?.let { "$%.2f".format(it) } ?: "N/A"}")
    atmPut.getMidPrice()?.let { println("Mid Price: $%.2f".format(it)) }
    atmPut.getBidAskSpread()?.let { println("Bid-Ask Spread: $%.2f".format(it)) }
    println()
    println("Volume: ${atmPut.volume ?: 0}")
    println("Open Interest: ${atmPut.openInterest ?: 0}")
    println("Implied Volatility: ${atmPut.impliedVolatility?.let { "%.2f%%".format(it * 100) } ?: "N/A"}")
    println()

    // Calculate intrinsic and time value
    val putIntrinsic = atmPut.getIntrinsicValue(underlyingPrice, isCall = false)
    val putTimeValue = atmPut.getTimeValue(underlyingPrice, isCall = false)
    println("Intrinsic Value: $%.2f".format(putIntrinsic))
    putTimeValue?.let { println("Time Value: $%.2f".format(it)) }
    println()

    // Compare call and put
    println("=== ATM Options Comparison ===")
    val callIV = atmCall.impliedVolatility ?: 0.0
    val putIV = atmPut.impliedVolatility ?: 0.0
    val ivSkew = (putIV - callIV) / callIV * 100

    println("Call IV: %.2f%%".format(callIV * 100))
    println("Put IV: %.2f%%".format(putIV * 100))
    println("IV Skew: %.2f%%".format(ivSkew))
    println()

    if (ivSkew > 5) {
        println("Interpretation: Puts are more expensive (bearish sentiment)")
    } else if (ivSkew < -5) {
        println("Interpretation: Calls are more expensive (bullish sentiment)")
    } else {
        println("Interpretation: Balanced sentiment")
    }
}

/**
 * Example 4: Compare calls and puts across different strike prices
 */
suspend fun callsPutsComparison(ufc: Ufc) {
    println("-".repeat(80))
    println("Example 4: Calls vs Puts Comparison by Strike")
    println("-".repeat(80))

    val symbol = "GOOGL"
    println("Comparing options across strikes for $symbol...")
    println()

    val optionsData = ufc.options(symbol)
    val underlyingPrice = optionsData.underlyingQuote?.regularMarketPrice ?: 0.0

    println("=== Options Comparison ===")
    println("Underlying: ${optionsData.underlyingSymbol}")
    println("Current Price: $%.2f".format(underlyingPrice))
    println()

    // Select strikes around the current price
    val targetStrikes = optionsData.strikes
        .filter { it >= underlyingPrice * 0.95 && it <= underlyingPrice * 1.05 }
        .sorted()
        .take(7)

    println("%-10s %10s %10s %15s %15s".format("Strike", "Call IV", "Put IV", "Call Volume", "Put Volume"))
    println("-".repeat(70))

    targetStrikes.forEach { strike ->
        val call = optionsData.optionsChain.findCall(strike)
        val put = optionsData.optionsChain.findPut(strike)

        val strikeStr = "$%.2f".format(strike)
        val callIVStr = call?.impliedVolatility?.let { "%.1f%%".format(it * 100) } ?: "N/A"
        val putIVStr = put?.impliedVolatility?.let { "%.1f%%".format(it * 100) } ?: "N/A"
        val callVolStr = call?.volume?.toString() ?: "0"
        val putVolStr = put?.volume?.toString() ?: "0"

        val marker = if (Math.abs(strike - underlyingPrice) / underlyingPrice < 0.01) " *" else ""

        println("%-10s %10s %10s %15s %15s%s".format(
            strikeStr, callIVStr, putIVStr, callVolStr, putVolStr, marker
        ))
    }

    println()
    println("* = Closest to current price (ATM)")
    println()

    // Calculate total volume and open interest
    val totalCallVolume = optionsData.optionsChain.calls.sumOf { it.volume ?: 0 }
    val totalPutVolume = optionsData.optionsChain.puts.sumOf { it.volume ?: 0 }
    val totalCallOI = optionsData.optionsChain.calls.sumOf { it.openInterest ?: 0 }
    val totalPutOI = optionsData.optionsChain.puts.sumOf { it.openInterest ?: 0 }

    println("=== Overall Statistics ===")
    println("Total Call Volume: $totalCallVolume")
    println("Total Put Volume: $totalPutVolume")
    println("Call/Put Volume Ratio: %.2f".format(totalCallVolume.toDouble() / maxOf(totalPutVolume, 1)))
    println()
    println("Total Call Open Interest: $totalCallOI")
    println("Total Put Open Interest: $totalPutOI")
    println("Call/Put OI Ratio: %.2f".format(totalCallOI.toDouble() / maxOf(totalPutOI, 1)))
    println()

    println("Interpretation:")
    val volumeRatio = totalCallVolume.toDouble() / maxOf(totalPutVolume, 1)
    when {
        volumeRatio > 1.5 -> println("  - High call volume suggests bullish sentiment")
        volumeRatio < 0.67 -> println("  - High put volume suggests bearish sentiment")
        else -> println("  - Balanced call/put volume suggests neutral sentiment")
    }
}
