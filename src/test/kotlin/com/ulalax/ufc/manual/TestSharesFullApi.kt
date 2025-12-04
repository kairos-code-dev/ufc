package com.ulalax.ufc.manual

import com.ulalax.ufc.api.client.Ufc
import com.ulalax.ufc.api.client.UfcClientConfig
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitingSettings
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

/**
 * Manual test to verify Shares Full API response format
 */
fun main() = runBlocking {
    val ufc = Ufc.create(
        UfcClientConfig(
            rateLimitingSettings = RateLimitingSettings()
        )
    )
    try {
        println("Testing Shares Full API response format...")
        println("=".repeat(60))

        // Test with date range
        val start = LocalDate.of(2023, 1, 1)
        val end = LocalDate.of(2024, 1, 1)

        println("\nTest 1: With date range (2023-01-01 to 2024-01-01)")
        println("-".repeat(60))
        try {
            val result = ufc.stock.getSharesFull("AAPL", start, end)
            println("SUCCESS: Got ${result.size} data points")
            if (result.isNotEmpty()) {
                println("First entry: ${result.first()}")
                println("Last entry: ${result.last()}")
            }
        } catch (e: Exception) {
            println("ERROR: ${e.message}")
            e.printStackTrace()
        }

        println("\n" + "=".repeat(60))
        println("\nTest 2: With start date only (from 2023-01-01)")
        println("-".repeat(60))
        try {
            val result = ufc.stock.getSharesFull("AAPL", start, null)
            println("SUCCESS: Got ${result.size} data points")
            if (result.isNotEmpty()) {
                println("First entry: ${result.first()}")
                println("Last entry: ${result.last()}")
            }
        } catch (e: Exception) {
            println("ERROR: ${e.message}")
            e.printStackTrace()
        }

        println("\n" + "=".repeat(60))
        println("\nTest 3: With no dates (default range)")
        println("-".repeat(60))
        try {
            val result = ufc.stock.getSharesFull("AAPL")
            println("SUCCESS: Got ${result.size} data points")
            if (result.isNotEmpty()) {
                println("First entry: ${result.first()}")
                println("Last entry: ${result.last()}")
            }
        } catch (e: Exception) {
            println("ERROR: ${e.message}")
            e.printStackTrace()
        }

    } finally {
        ufc.close()
    }
}
