package examples

import com.ulalax.ufc.api.Ufc
import com.ulalax.ufc.domain.exception.UfcException
import kotlinx.coroutines.runBlocking
import java.text.NumberFormat
import java.util.*

/**
 * Real-time Quote Retrieval Example
 *
 * This example demonstrates how to retrieve real-time market quotes using UFC.
 * It covers:
 * 1. Single symbol quote retrieval
 * 2. Multiple symbols batch retrieval
 * 3. Accessing various quote fields (price, volume, market cap, etc.)
 * 4. Error handling for invalid symbols
 */
fun main() = runBlocking {
    println("=".repeat(80))
    println("UFC Quote Example - Real-time Market Data")
    println("=".repeat(80))
    println()

    // Create UFC client instance
    // Always use 'use' block to ensure proper resource cleanup
    Ufc.create().use { ufc ->
        try {
            // Example 1: Single Symbol Quote
            singleSymbolQuote(ufc)
            println()

            // Example 2: Multiple Symbols Quote
            multipleSymbolsQuote(ufc)
            println()

            // Example 3: Detailed Quote Information
            detailedQuoteInfo(ufc)
            println()

            // Example 4: Error Handling
            errorHandlingExample(ufc)

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
 * Example 1: Retrieve quote for a single symbol
 */
suspend fun singleSymbolQuote(ufc: Ufc) {
    println("-".repeat(80))
    println("Example 1: Single Symbol Quote")
    println("-".repeat(80))

    val symbol = "AAPL"
    println("Fetching quote for $symbol...")
    println()

    val quote = ufc.quote(symbol)

    // Display basic information
    quote.identification?.let { id ->
        println("Company: ${id.longName ?: id.shortName ?: "N/A"}")
        println("Symbol: ${id.symbol}")
        println("Exchange: ${id.exchange}")
        println("Market: ${id.market}")
    }

    println()

    // Display pricing information
    quote.pricing?.let { price ->
        val formatter = NumberFormat.getCurrencyInstance(Locale.US)

        println("Current Price: ${formatter.format(price.price)}")
        println("Open: ${price.open?.let { formatter.format(it) } ?: "N/A"}")
        println("High: ${price.dayHigh?.let { formatter.format(it) } ?: "N/A"}")
        println("Low: ${price.dayLow?.let { formatter.format(it) } ?: "N/A"}")
        println("Previous Close: ${price.previousClose?.let { formatter.format(it) } ?: "N/A"}")

        // Display change
        val change = price.change
        val changePercent = price.changePercent
        if (change != null && changePercent != null) {
            val changeStr = if (change >= 0) "+%.2f".format(change) else "%.2f".format(change)
            val percentStr = if (changePercent >= 0) "+%.2f%%".format(changePercent) else "%.2f%%".format(changePercent)
            println("Change: $changeStr ($percentStr)")
        }

        // Display volume
        price.volume?.let { vol ->
            println("Volume: ${NumberFormat.getNumberInstance(Locale.US).format(vol)}")
        }
    }

    println()

    // Display market cap
    quote.marketCap?.let { mc ->
        mc.marketCap?.let { cap ->
            val formatter = NumberFormat.getNumberInstance(Locale.US)
            val billion = cap / 1_000_000_000.0
            println("Market Cap: $${String.format("%.2fB", billion)}")
        }
    }

    // Display 52-week range
    quote.fiftyTwoWeek?.let { fw ->
        if (fw.low != null && fw.high != null) {
            val formatter = NumberFormat.getCurrencyInstance(Locale.US)
            println("52-Week Range: ${formatter.format(fw.low)} - ${formatter.format(fw.high)}")
        }
    }

    // Display PE ratio
    quote.financialRatios?.let { fr ->
        fr.trailingPE?.let { pe ->
            println("P/E Ratio (TTM): %.2f".format(pe))
        }
    }
}

/**
 * Example 2: Retrieve quotes for multiple symbols at once
 */
suspend fun multipleSymbolsQuote(ufc: Ufc) {
    println("-".repeat(80))
    println("Example 2: Multiple Symbols Quote (Batch Retrieval)")
    println("-".repeat(80))

    val symbols = listOf("AAPL", "GOOGL", "MSFT", "TSLA", "AMZN")
    println("Fetching quotes for: ${symbols.joinToString(", ")}")
    println()

    val quotes = ufc.quote(symbols)

    println("%-10s %-50s %12s %12s %10s".format("Symbol", "Name", "Price", "Change", "Volume"))
    println("-".repeat(80))

    quotes.forEach { quote ->
        val symbol = quote.identification?.symbol ?: "N/A"
        val name = quote.identification?.shortName?.take(47) ?: "N/A"
        val price = quote.pricing?.let { "$%.2f".format(it.price) } ?: "N/A"
        val changePercent = quote.pricing?.changePercent?.let {
            val sign = if (it >= 0) "+" else ""
            "$sign%.2f%%".format(it)
        } ?: "N/A"
        val volume = quote.pricing?.volume?.let {
            val millions = it / 1_000_000.0
            "%.2fM".format(millions)
        } ?: "N/A"

        println("%-10s %-50s %12s %12s %10s".format(symbol, name, price, changePercent, volume))
    }
}

/**
 * Example 3: Display detailed quote information including financial metrics
 */
suspend fun detailedQuoteInfo(ufc: Ufc) {
    println("-".repeat(80))
    println("Example 3: Detailed Quote Information")
    println("-".repeat(80))

    val symbol = "MSFT"
    println("Fetching detailed quote for $symbol...")
    println()

    val quote = ufc.quote(symbol)

    // Company identification
    println("=== Company Information ===")
    quote.identification?.let { id ->
        println("Name: ${id.longName}")
        println("Symbol: ${id.symbol}")
        println("Exchange: ${id.exchange}")
        println("Type: ${id.quoteType}")
    }
    println()

    // Financial ratios
    println("=== Financial Ratios ===")
    quote.financialRatios?.let { fr ->
        fr.trailingPE?.let { println("P/E Ratio (TTM): %.2f".format(it)) }
        fr.forwardPE?.let { println("P/E Ratio (Forward): %.2f".format(it)) }
        fr.priceToBook?.let { println("P/B Ratio: %.2f".format(it)) }
    }
    println()

    // Earnings information
    println("=== Earnings ===")
    quote.earnings?.let { earn ->
        earn.epsTrailingTwelveMonths?.let { println("EPS (TTM): $%.2f".format(it)) }
        earn.epsForward?.let { println("EPS (Forward): $%.2f".format(it)) }
    }
    println()

    // Dividends
    println("=== Dividends ===")
    quote.dividends?.let { div ->
        div.annualRate?.let { println("Annual Dividend: $%.2f".format(it)) }
        div.yield?.let { println("Dividend Yield: %.2f%%".format(it * 100)) }
        div.trailingRate?.let { println("Trailing Annual Dividend: $%.2f".format(it)) }
    }
    println()

    // Moving averages
    println("=== Moving Averages ===")
    quote.movingAverages?.let { ma ->
        ma.fiftyDayAverage?.let { println("50-Day MA: $%.2f".format(it)) }
        ma.twoHundredDayAverage?.let { println("200-Day MA: $%.2f".format(it)) }
    }
    println()

    // Analyst ratings
    println("=== Analyst Ratings ===")
    quote.analystRatings?.let { ar ->
        ar.recommendationKey?.let { println("Recommendation: $it") }
        ar.numberOfAnalystOpinions?.let { println("Number of Analysts: $it") }
    }
}

/**
 * Example 4: Demonstrate error handling for invalid symbols
 */
suspend fun errorHandlingExample(ufc: Ufc) {
    println("-".repeat(80))
    println("Example 4: Error Handling")
    println("-".repeat(80))

    val invalidSymbol = "INVALID_SYMBOL_XYZ"
    println("Attempting to fetch quote for invalid symbol: $invalidSymbol")
    println()

    try {
        val quote = ufc.quote(invalidSymbol)

        // Check if the quote has valid data
        val identification = quote.identification
        if (identification?.symbol == null) {
            println("Warning: Symbol '$invalidSymbol' returned empty data")
            println("This symbol may not exist or may not be available on Yahoo Finance")
        } else {
            println("Quote retrieved successfully for: ${identification.symbol}")
        }
    } catch (e: UfcException) {
        println("Error retrieving quote: ${e.message}")
        println("Error code: ${e.errorCode}")
    } catch (e: Exception) {
        println("Unexpected error: ${e.message}")
    }

    println()
    println("Tip: Always verify that the symbol exists on Yahoo Finance before querying")
    println("     Valid examples: AAPL, GOOGL, MSFT, TSLA, AMZN, ^GSPC, ^DJI")
}
