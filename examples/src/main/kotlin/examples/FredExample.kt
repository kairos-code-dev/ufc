package examples

import com.ulalax.ufc.api.Ufc
import com.ulalax.ufc.api.UfcConfig
import com.ulalax.ufc.domain.model.series.DataFrequency
import com.ulalax.ufc.domain.exception.UfcException
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * FRED Economic Data Example
 *
 * This example demonstrates how to retrieve economic data from FRED (Federal Reserve Economic Data).
 * It covers:
 * 1. Retrieving popular economic indicators (GDP, unemployment, inflation)
 * 2. Custom date ranges
 * 3. Different data frequencies
 * 4. Analyzing trends in economic data
 *
 * IMPORTANT: This example requires a FRED API key.
 * Get your free API key at: https://fred.stlouisfed.org/docs/api/api_key.html
 *
 * Set your API key via:
 * - Environment variable: FRED_API_KEY=your_key_here
 * - Or in local.properties: fred.api.key=your_key_here
 */
fun main() = runBlocking {
    println("=".repeat(80))
    println("UFC FRED Example - Economic Data from Federal Reserve")
    println("=".repeat(80))
    println()

    // Get FRED API key from environment variable
    val fredApiKey = System.getenv("FRED_API_KEY")

    if (fredApiKey == null || fredApiKey.isBlank()) {
        println("ERROR: FRED API key not found!")
        println()
        println("To run this example, you need a FRED API key.")
        println("Get your free API key at: https://fred.stlouisfed.org/docs/api/api_key.html")
        println()
        println("Then set it via environment variable:")
        println("  export FRED_API_KEY=your_api_key_here")
        println()
        println("Or add it to local.properties:")
        println("  fred.api.key=your_api_key_here")
        return@runBlocking
    }

    // Create UFC client with FRED API key
    val config = UfcConfig(fredApiKey = fredApiKey)

    Ufc.create(config).use { ufc ->
        try {
            // Example 1: GDP data
            gdpData(ufc)
            println()

            // Example 2: Unemployment rate
            unemploymentData(ufc)
            println()

            // Example 3: Inflation (CPI)
            inflationData(ufc)
            println()

            // Example 4: Interest rates
            interestRatesData(ufc)

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
 * Example 1: Retrieve GDP (Gross Domestic Product) data
 */
suspend fun gdpData(ufc: Ufc) {
    println("-".repeat(80))
    println("Example 1: GDP (Gross Domestic Product)")
    println("-".repeat(80))

    val seriesId = "GDP"
    val startDate = LocalDate.of(2020, 1, 1)

    println("Fetching GDP data from FRED...")
    println("Series ID: $seriesId")
    println("Start Date: $startDate")
    println()

    val series = ufc.series(seriesId, startDate = startDate)

    println("=== Series Information ===")
    println("Title: ${series.title}")
    println("Frequency: ${series.frequency}")
    println("Units: ${series.units}")
    println("Total Observations: ${series.observations.size}")
    println()

    println("=== Recent GDP Data ===")
    println("%-15s %20s".format("Date", "GDP (Billions)"))
    println("-".repeat(40))

    series.observations.takeLast(10).forEach { obs ->
        val dateStr = obs.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val valueStr = obs.value?.let { "$%.2fB".format(it) } ?: "N/A"
        println("%-15s %20s".format(dateStr, valueStr))
    }

    println()

    // Calculate growth rate
    if (series.observations.size >= 2) {
        val latest = series.observations.last()
        val previous = series.observations[series.observations.size - 2]

        val latestValue = latest.value
        val previousValue = previous.value
        if (latestValue != null && previousValue != null) {
            val growthRate = ((latestValue - previousValue) / previousValue) * 100
            println("Quarter-over-Quarter Growth: %.2f%%".format(growthRate))
            println()
        }
    }

    println("Note: GDP is reported quarterly and represents the total value of goods and")
    println("      services produced in the United States.")
}

/**
 * Example 2: Retrieve unemployment rate data
 */
suspend fun unemploymentData(ufc: Ufc) {
    println("-".repeat(80))
    println("Example 2: Unemployment Rate")
    println("-".repeat(80))

    val seriesId = "UNRATE"
    val startDate = LocalDate.of(2022, 1, 1)

    println("Fetching unemployment rate from FRED...")
    println("Series ID: $seriesId")
    println("Start Date: $startDate")
    println()

    val series = ufc.series(seriesId, startDate = startDate)

    println("=== Series Information ===")
    println("Title: ${series.title}")
    println("Frequency: ${series.frequency}")
    println("Units: ${series.units}")
    println()

    println("=== Monthly Unemployment Rate ===")
    println("%-15s %15s".format("Month", "Rate"))
    println("-".repeat(35))

    series.observations.takeLast(12).forEach { obs ->
        val dateStr = obs.date.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val valueStr = obs.value?.let { "%.1f%%".format(it) } ?: "N/A"
        println("%-15s %15s".format(dateStr, valueStr))
    }

    println()

    // Calculate average and trend
    val recentValues = series.observations.takeLast(12).mapNotNull { it.value }
    if (recentValues.isNotEmpty()) {
        val avgRate = recentValues.average()
        val minRate = recentValues.minOrNull()
        val maxRate = recentValues.maxOrNull()

        println("=== 12-Month Statistics ===")
        println("Average Rate: %.2f%%".format(avgRate))
        println("Minimum Rate: %.2f%%".format(minRate))
        println("Maximum Rate: %.2f%%".format(maxRate))
        println()
    }

    println("Note: The unemployment rate represents the percentage of the labor force")
    println("      that is unemployed and actively seeking employment.")
}

/**
 * Example 3: Retrieve inflation data (Consumer Price Index)
 */
suspend fun inflationData(ufc: Ufc) {
    println("-".repeat(80))
    println("Example 3: Inflation Rate (CPI)")
    println("-".repeat(80))

    val seriesId = "CPIAUCSL"  // Consumer Price Index for All Urban Consumers
    val startDate = LocalDate.of(2021, 1, 1)

    println("Fetching CPI data from FRED...")
    println("Series ID: $seriesId (Consumer Price Index)")
    println("Start Date: $startDate")
    println()

    val series = ufc.series(seriesId, startDate = startDate)

    println("=== Series Information ===")
    println("Title: ${series.title}")
    println("Frequency: ${series.frequency}")
    println("Units: ${series.units}")
    println()

    // Calculate year-over-year inflation rate
    println("=== Year-over-Year Inflation Rate ===")
    println("%-15s %12s %20s".format("Month", "CPI", "YoY Inflation"))
    println("-".repeat(50))

    series.observations.takeLast(12).forEach { obs ->
        // Find observation from 12 months ago
        val yearAgoDate = obs.date.minusYears(1)
        val yearAgoObs = series.observations.find { it.date == yearAgoDate }

        val dateStr = obs.date.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val cpiStr = obs.value?.let { "%.2f".format(it) } ?: "N/A"

        val obsValue = obs.value
        val yearAgoValue = yearAgoObs?.value
        val inflationStr = if (obsValue != null && yearAgoValue != null) {
            val inflation = ((obsValue - yearAgoValue) / yearAgoValue) * 100
            "%.2f%%".format(inflation)
        } else {
            "N/A"
        }

        println("%-15s %12s %20s".format(dateStr, cpiStr, inflationStr))
    }

    println()

    // Calculate recent inflation trend
    val recentObs = series.observations.takeLast(13)
    if (recentObs.size >= 13) {
        val latest = recentObs.last()
        val yearAgo = recentObs.first()

        val latestValue = latest.value
        val yearAgoValue = yearAgo.value
        if (latestValue != null && yearAgoValue != null) {
            val annualInflation = ((latestValue - yearAgoValue) / yearAgoValue) * 100
            println("Latest Annual Inflation Rate: %.2f%%".format(annualInflation))
            println()
        }
    }

    println("Note: CPI measures the average change in prices paid by urban consumers")
    println("      for a basket of goods and services. Higher CPI indicates inflation.")
}

/**
 * Example 4: Retrieve interest rates data
 */
suspend fun interestRatesData(ufc: Ufc) {
    println("-".repeat(80))
    println("Example 4: Federal Funds Rate")
    println("-".repeat(80))

    val seriesId = "DFF"  // Daily Federal Funds Rate
    val startDate = LocalDate.of(2023, 1, 1)

    println("Fetching Federal Funds Rate from FRED...")
    println("Series ID: $seriesId")
    println("Start Date: $startDate")
    println()

    val series = ufc.series(seriesId, startDate = startDate, frequency = DataFrequency.Monthly)

    println("=== Series Information ===")
    println("Title: ${series.title}")
    println("Frequency: ${series.frequency}")
    println("Units: ${series.units}")
    println()

    println("=== Federal Funds Rate History ===")
    println("%-15s %15s".format("Month", "Rate"))
    println("-".repeat(35))

    series.observations.takeLast(12).forEach { obs ->
        val dateStr = obs.date.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val valueStr = obs.value?.let { "%.2f%%".format(it) } ?: "N/A"
        println("%-15s %15s".format(dateStr, valueStr))
    }

    println()

    // Analyze rate changes
    val recentObs = series.observations.takeLast(6)
    if (recentObs.size >= 2) {
        val latest = recentObs.last()
        val oldest = recentObs.first()

        val latestValue = latest.value
        val oldestValue = oldest.value
        if (latestValue != null && oldestValue != null) {
            val rateChange = latestValue - oldestValue
            val trend = when {
                rateChange > 0.25 -> "Rising (tightening monetary policy)"
                rateChange < -0.25 -> "Falling (loosening monetary policy)"
                else -> "Relatively stable"
            }

            println("=== 6-Month Analysis ===")
            println("Rate Change: %.2f percentage points".format(rateChange))
            println("Trend: $trend")
            println()
        }
    }

    println("Note: The Federal Funds Rate is the interest rate at which banks lend to")
    println("      each other overnight. It's a key monetary policy tool used by the")
    println("      Federal Reserve to influence economic conditions.")
    println()
    println("Popular FRED Series IDs:")
    println("  - GDP: Gross Domestic Product")
    println("  - UNRATE: Unemployment Rate")
    println("  - CPIAUCSL: Consumer Price Index (Inflation)")
    println("  - DFF: Federal Funds Rate")
    println("  - GS10: 10-Year Treasury Rate")
    println("  - M2SL: M2 Money Supply")
    println("  - DEXUSEU: USD/EUR Exchange Rate")
}
