package com.ulalax.ufc.api

import com.ulalax.ufc.infrastructure.businessinsider.BusinessInsiderClientConfig
import com.ulalax.ufc.infrastructure.fred.FredClientConfig
import com.ulalax.ufc.infrastructure.yahoo.YahooClientConfig
import com.ulalax.ufc.infrastructure.yahoo.streaming.StreamingClientConfig

/**
 * Configuration for the UFC (US Free Financial Data Collector) client.
 *
 * This class centralizes all configuration settings for the UFC library, including:
 * - Yahoo Finance API client settings
 * - FRED (Federal Reserve Economic Data) API settings and authentication
 * - Business Insider API client settings
 * - WebSocket streaming configuration for real-time data
 *
 * ## Usage Examples
 *
 * ### Basic usage with defaults
 * ```kotlin
 * val config = UfcConfig()
 * val ufc = Ufc.create(config)
 * ```
 *
 * ### With FRED API key
 * ```kotlin
 * val config = UfcConfig(fredApiKey = "your-fred-api-key")
 * val ufc = Ufc.create(config)
 * ```
 *
 * ### Custom timeout and logging settings
 * ```kotlin
 * val config = UfcConfig(
 *     yahooConfig = YahooClientConfig(
 *         connectTimeoutMs = 60_000,
 *         requestTimeoutMs = 120_000,
 *         enableLogging = true
 *     ),
 *     fredConfig = FredClientConfig(
 *         enableLogging = true
 *     )
 * )
 * val ufc = Ufc.create(config)
 * ```
 *
 * ### Custom rate limiting
 * ```kotlin
 * val config = UfcConfig(
 *     yahooConfig = YahooClientConfig(
 *         rateLimitConfig = RateLimitConfig(
 *             maxRequestsPerSecond = 30,
 *             maxBurstSize = 10
 *         )
 *     )
 * )
 * val ufc = Ufc.create(config)
 * ```
 *
 * ### Custom WebSocket streaming configuration
 * ```kotlin
 * val config = UfcConfig(
 *     streamingConfig = StreamingClientConfig(
 *         connectTimeoutMs = 15_000,
 *         heartbeatIntervalMs = 20_000,
 *         enableLogging = true
 *     )
 * )
 * val ufc = Ufc.create(config)
 * ```
 *
 * @property fredApiKey Optional API key for FRED (Federal Reserve Economic Data) API.
 *                      Required for accessing FRED economic data endpoints.
 *                      You can obtain a free API key from https://fred.stlouisfed.org/docs/api/api_key.html
 *                      Default: null (FRED functionality will be unavailable without a key)
 *
 * @property yahooConfig Configuration for Yahoo Finance API client.
 *                       Controls HTTP timeouts, logging, and rate limiting for Yahoo Finance requests.
 *                       Default: YahooClientConfig() with 30s connect timeout, 60s request timeout,
 *                       logging disabled, and 50 requests per second rate limit
 *
 * @property fredConfig Configuration for FRED API client.
 *                      Controls HTTP timeouts, logging, and rate limiting for FRED requests.
 *                      Default: FredClientConfig() with 30s connect timeout, 60s request timeout,
 *                      logging disabled, and 2 requests per second rate limit (as per FRED guidelines)
 *
 * @property businessInsiderConfig Configuration for Business Insider API client.
 *                                 Controls HTTP timeouts, logging, and rate limiting for Business Insider requests.
 *                                 Primarily used for ISIN to ticker symbol conversion.
 *                                 Default: BusinessInsiderClientConfig() with 30s connect timeout, 60s request timeout,
 *                                 logging disabled, and 10 requests per second rate limit
 *
 * @property streamingConfig Configuration for WebSocket streaming client.
 *                           Controls WebSocket connection settings, heartbeat intervals, reconnection behavior,
 *                           and event buffering for real-time Yahoo Finance data streaming.
 *                           Default: StreamingClientConfig() with 10s connect timeout, 15s heartbeat interval,
 *                           30s ping timeout, automatic reconnection enabled, 64-event buffer, and logging disabled
 *
 * @see com.ulalax.ufc.Ufc.create
 * @see YahooClientConfig
 * @see FredClientConfig
 * @see BusinessInsiderClientConfig
 * @see StreamingClientConfig
 */
data class UfcConfig(
    val fredApiKey: String? = null,
    val yahooConfig: YahooClientConfig = YahooClientConfig(),
    val fredConfig: FredClientConfig = FredClientConfig(),
    val businessInsiderConfig: BusinessInsiderClientConfig = BusinessInsiderClientConfig(),
    val streamingConfig: StreamingClientConfig = StreamingClientConfig(),
)
