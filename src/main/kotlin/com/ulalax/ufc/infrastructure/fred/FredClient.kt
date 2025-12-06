package com.ulalax.ufc.infrastructure.fred

import com.ulalax.ufc.domain.model.series.DataFrequency
import com.ulalax.ufc.domain.model.series.FredSeries
import com.ulalax.ufc.domain.model.series.FredSeriesInfo
import com.ulalax.ufc.infrastructure.common.ratelimit.GlobalRateLimiters
import com.ulalax.ufc.infrastructure.fred.internal.FredHttpClient
import java.time.LocalDate

/**
 * FRED (Federal Reserve Economic Data) API client.
 *
 * A client for querying economic data from the Federal Reserve Economic Data (FRED) system.
 * FRED is a database of hundreds of thousands of economic time series maintained by the
 * Federal Reserve Bank of St. Louis.
 *
 * **Note:** A FRED API key is required to use this client. You can obtain a free API key at
 * https://fred.stlouisfed.org/docs/api/api_key.html
 *
 * Usage example:
 * ```kotlin
 * val fred = FredClient.create(apiKey = "your-api-key")
 *
 * // Fetch GDP data
 * val gdp = fred.series("GDP")
 *
 * // Fetch unemployment rate data with date range
 * val unrate = fred.series(
 *     "UNRATE",
 *     startDate = LocalDate.of(2020, 1, 1),
 *     endDate = LocalDate.of(2023, 12, 31)
 * )
 *
 * // Fetch only series metadata
 * val info = fred.seriesInfo("GDP")
 * println("Title: ${info.title}, Frequency: ${info.frequency}")
 *
 * fred.close()
 * ```
 *
 * Alternatively, use a `use` block for automatic resource cleanup:
 * ```kotlin
 * FredClient.create("your-api-key").use { fred ->
 *     val gdp = fred.series("GDP")
 *     println(gdp.observations.size)
 * }
 * ```
 *
 * @property httpClient The FRED HTTP client for making API requests
 * @property config Client configuration settings
 */
class FredClient private constructor(
    private val httpClient: FredHttpClient,
    private val config: FredClientConfig,
) : AutoCloseable {
    /**
     * Fetches FRED time series data.
     *
     * Retrieves both series metadata and observation values for the specified series.
     *
     * @param seriesId The series ID (e.g., "GDP", "UNRATE", "DFF")
     * @param startDate The start date for observations (null to fetch from the beginning of the series)
     * @param endDate The end date for observations (null to fetch to the end of the series)
     * @param frequency The data frequency (null to use the original frequency)
     * @return FRED series data containing metadata and observations
     * @throws IllegalArgumentException if seriesId is blank
     * @throws NoSuchElementException if the series cannot be found
     * @throws IllegalStateException if API authentication fails or rate limit is exceeded
     */
    suspend fun series(
        seriesId: String,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        frequency: DataFrequency? = null,
    ): FredSeries = httpClient.fetchSeries(seriesId, startDate, endDate, frequency)

    /**
     * Fetches FRED series metadata only.
     *
     * Retrieves only the series basic information (title, frequency, units, etc.)
     * without observation values.
     *
     * @param seriesId The series ID (e.g., "GDP", "UNRATE")
     * @return Series metadata
     * @throws IllegalArgumentException if seriesId is blank
     * @throws NoSuchElementException if the series cannot be found
     * @throws IllegalStateException if API authentication fails or rate limit is exceeded
     */
    suspend fun seriesInfo(seriesId: String): FredSeriesInfo = httpClient.fetchSeriesInfo(seriesId)

    /**
     * Closes the client and releases resources.
     *
     * Closes the HTTP client and terminates all connections.
     */
    override fun close() {
        httpClient.close()
    }

    companion object {
        /**
         * Creates a FredClient instance with default configuration.
         *
         * **Note:** A FRED API key is required. You can obtain a free API key at
         * https://fred.stlouisfed.org/docs/api/api_key.html
         *
         * @param apiKey The FRED API key
         * @return A new FredClient instance
         * @throws IllegalArgumentException if apiKey is blank
         */
        fun create(apiKey: String): FredClient = create(apiKey, FredClientConfig())

        /**
         * Creates a FredClient instance with custom configuration.
         *
         * **Note:** A FRED API key is required. You can obtain a free API key at
         * https://fred.stlouisfed.org/docs/api/api_key.html
         *
         * @param apiKey The FRED API key
         * @param config Client configuration settings
         * @return A new FredClient instance
         * @throws IllegalArgumentException if apiKey is blank
         */
        fun create(
            apiKey: String,
            config: FredClientConfig,
        ): FredClient {
            require(apiKey.isNotBlank()) { "FRED API Key는 빈 문자열이 될 수 없습니다" }

            // GlobalRateLimiters에서 공유 Rate Limiter 획득
            val rateLimiter = GlobalRateLimiters.getFredLimiter(config.rateLimitConfig)

            val httpClient = FredHttpClient(apiKey, config, rateLimiter)
            return FredClient(httpClient, config)
        }
    }
}
