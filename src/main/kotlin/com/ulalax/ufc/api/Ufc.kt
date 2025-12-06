package com.ulalax.ufc.api

import com.ulalax.ufc.infrastructure.yahoo.YahooClient
import com.ulalax.ufc.domain.model.chart.ChartEventType
import com.ulalax.ufc.domain.model.chart.Interval
import com.ulalax.ufc.domain.model.chart.Period
import com.ulalax.ufc.domain.model.fundamentals.FundamentalsTimeseriesResult
import com.ulalax.ufc.domain.model.fundamentals.FundamentalsType
import com.ulalax.ufc.domain.model.quote.QuoteSummaryModule
import com.ulalax.ufc.domain.model.quote.QuoteSummaryModuleResult
import com.ulalax.ufc.domain.model.lookup.LookupType
import com.ulalax.ufc.domain.model.lookup.LookupResult
import com.ulalax.ufc.domain.model.market.MarketCode
import com.ulalax.ufc.domain.model.market.MarketSummaryResult
import com.ulalax.ufc.domain.model.market.MarketTimeResult
import com.ulalax.ufc.domain.model.options.OptionsData
import com.ulalax.ufc.domain.model.realtime.QuoteData
import com.ulalax.ufc.domain.model.screener.*
import com.ulalax.ufc.domain.model.search.SearchResponse
import com.ulalax.ufc.domain.model.visualization.VisualizationEarningsCalendar
import com.ulalax.ufc.infrastructure.fred.FredClient
import com.ulalax.ufc.domain.model.series.DataFrequency
import com.ulalax.ufc.infrastructure.businessinsider.BusinessInsiderClient
import com.ulalax.ufc.domain.model.security.IsinSearchResult
import com.ulalax.ufc.domain.exception.ErrorCode
import com.ulalax.ufc.domain.exception.UfcException
import com.ulalax.ufc.infrastructure.yahoo.streaming.StreamingClient
import java.time.LocalDate

/**
 * Main entry point for the UFC (US Free Financial Data Collector) library.
 *
 * This class provides a unified facade for accessing financial data from multiple sources:
 * - **Yahoo Finance**: Real-time quotes, historical data, company fundamentals, options chains,
 *   stock screener, earnings calendar, and WebSocket streaming
 * - **FRED**: Federal Reserve Economic Data including GDP, unemployment, inflation, and other
 *   economic indicators
 * - **Business Insider**: ISIN to ticker symbol conversion
 *
 * The library is inspired by Python's yfinance and fredapi libraries, providing similar
 * functionality for Kotlin/JVM applications.
 *
 * ## Quick Start
 * ```kotlin
 * // Create client without FRED API
 * val ufc = Ufc.create()
 * val quote = ufc.quote("AAPL")
 * println("Price: ${quote.pricing?.price}")
 *
 * // Or with FRED API key
 * val config = UfcConfig(fredApiKey = "your-api-key")
 * val ufcWithFred = Ufc.create(config)
 * ```
 *
 * ## Usage Examples
 * ```kotlin
 * // Get multiple quotes
 * val quotes = ufc.quote(listOf("AAPL", "GOOGL", "MSFT"))
 *
 * // Get historical chart data
 * val chart = ufc.chart(
 *     symbol = "AAPL",
 *     interval = Interval.OneDay,
 *     period = Period.OneYear
 * )
 *
 * // Get company fundamentals
 * val summary = ufc.quoteSummary(
 *     "AAPL",
 *     QuoteSummaryModule.PRICE,
 *     QuoteSummaryModule.FINANCIAL_DATA
 * )
 *
 * // Screen stocks
 * val gainers = ufc.screener(PredefinedScreener.DAY_GAINERS, count = 10)
 *
 * // Get FRED economic data (requires API key)
 * val gdp = ufc.series("GDP")
 * ```
 *
 * ## Resource Management
 * This class implements [AutoCloseable] and should be used with try-with-resources or Kotlin's
 * use function to ensure proper cleanup of HTTP clients and WebSocket connections:
 * ```kotlin
 * Ufc.create().use { ufc ->
 *     val quote = ufc.quote("AAPL")
 *     // ...
 * }
 * ```
 *
 * @property yahoo Yahoo Finance client for market data and company information
 * @property fred FRED client for economic data (null if API key not provided)
 * @property businessInsider Business Insider client for ISIN lookups
 * @property streaming WebSocket streaming client for real-time price updates
 *
 * @see YahooClient
 * @see FredClient
 * @see BusinessInsiderClient
 * @see StreamingClient
 * @see UfcConfig
 */
class Ufc private constructor(
    val yahoo: YahooClient,
    val fred: FredClient?,
    val businessInsider: BusinessInsiderClient,
    val streaming: StreamingClient
) : AutoCloseable {

    // Yahoo Finance API methods

    /**
     * Retrieves real-time quote data for a single symbol.
     *
     * This method provides current market data including price, volume, market cap,
     * bid/ask spreads, and other real-time trading information.
     *
     * @param symbol The ticker symbol (e.g., "AAPL", "GOOGL", "MSFT")
     * @return [QuoteData] containing real-time market information
     * @throws UfcException if the symbol is invalid or the request fails
     *
     * @sample
     * ```kotlin
     * val quote = ufc.quote("AAPL")
     * println("Current price: ${quote.pricing?.price}")
     * println("Market cap: ${quote.pricing?.marketCap}")
     * ```
     */
    suspend fun quote(symbol: String): QuoteData = yahoo.quote(symbol)

    /**
     * Retrieves real-time quote data for multiple symbols in a single request.
     *
     * This is more efficient than making multiple individual quote requests when
     * you need data for several symbols.
     *
     * @param symbols List of ticker symbols (e.g., listOf("AAPL", "GOOGL", "MSFT"))
     * @return List of [QuoteData] containing real-time market information for each symbol
     * @throws UfcException if any symbol is invalid or the request fails
     *
     * @sample
     * ```kotlin
     * val quotes = ufc.quote(listOf("AAPL", "GOOGL", "MSFT"))
     * quotes.forEach { quote ->
     *     println("${quote.symbol}: ${quote.pricing?.price}")
     * }
     * ```
     */
    suspend fun quote(symbols: List<String>): List<QuoteData> = yahoo.quote(symbols)

    /**
     * Retrieves detailed company information using Yahoo's QuoteSummary modules.
     *
     * Yahoo Finance provides 37 different data modules including price, financials,
     * balance sheet, income statement, cash flow, analyst recommendations, and more.
     * You can request specific modules to get only the data you need.
     *
     * @param symbol The ticker symbol (e.g., "AAPL")
     * @param modules Variable number of [QuoteSummaryModule] enum values specifying which
     *                data modules to retrieve (e.g., PRICE, FINANCIAL_DATA, BALANCE_SHEET_HISTORY)
     * @return [QuoteSummaryModuleResult] containing the requested module data
     * @throws UfcException if the symbol is invalid or the request fails
     *
     * @sample
     * ```kotlin
     * val summary = ufc.quoteSummary(
     *     "AAPL",
     *     QuoteSummaryModule.PRICE,
     *     QuoteSummaryModule.SUMMARY_DETAIL,
     *     QuoteSummaryModule.FINANCIAL_DATA
     * )
     * println("P/E Ratio: ${summary.summaryDetail?.trailingPE}")
     * ```
     */
    suspend fun quoteSummary(symbol: String, vararg modules: QuoteSummaryModule): QuoteSummaryModuleResult =
        yahoo.quoteSummary(symbol, *modules)

    /**
     * Retrieves historical OHLCV (Open, High, Low, Close, Volume) chart data.
     *
     * This method provides historical price and volume data with configurable time intervals
     * and periods. Optionally includes corporate events like dividends and stock splits.
     *
     * @param symbol The ticker symbol (e.g., "AAPL")
     * @param interval Time interval for each data point (e.g., OneDay, OneHour, FiveMinutes)
     * @param period Historical time range (e.g., OneYear, FiveYears, Max)
     * @param events Optional corporate events to include (DIV for dividends, SPLIT for stock splits)
     * @return Chart data containing historical prices, volumes, and requested events
     * @throws UfcException if the symbol is invalid or the request fails
     *
     * @sample
     * ```kotlin
     * // Get daily data for one year
     * val chart = ufc.chart("AAPL", Interval.OneDay, Period.OneYear)
     *
     * // Get 5-minute data with dividends and splits
     * val detailedChart = ufc.chart(
     *     symbol = "AAPL",
     *     interval = Interval.FiveMinutes,
     *     period = Period.OneMonth,
     *     events = arrayOf(ChartEventType.DIV, ChartEventType.SPLIT)
     * )
     * ```
     */
    suspend fun chart(symbol: String, interval: Interval = Interval.OneDay,
                      period: Period = Period.OneYear, vararg events: ChartEventType) =
        yahoo.chart(symbol, interval, period, *events)

    /**
     * Retrieves the earnings calendar for a specific company.
     *
     * This method provides upcoming and historical earnings announcement dates,
     * earnings per share (EPS) estimates, and actual results.
     *
     * @param symbol The ticker symbol (e.g., "AAPL")
     * @param limit Maximum number of earnings records to retrieve (default: 12)
     * @param offset Number of records to skip for pagination (default: 0)
     * @return Earnings calendar data with announcement dates and EPS information
     * @throws UfcException if the symbol is invalid or the request fails
     *
     * @sample
     * ```kotlin
     * val earnings = ufc.earningsCalendar("AAPL", limit = 4)
     * earnings.forEach { earning ->
     *     println("Date: ${earning.date}, EPS: ${earning.eps}")
     * }
     * ```
     */
    suspend fun earningsCalendar(symbol: String, limit: Int = 12, offset: Int = 0) =
        yahoo.earningsCalendar(symbol, limit, offset)

    /**
     * Retrieves time-series fundamental data for financial statement line items.
     *
     * This method provides historical fundamental data such as revenue, earnings, assets,
     * liabilities, and other financial statement items over time.
     *
     * @param symbol The ticker symbol (e.g., "AAPL")
     * @param types List of fundamental data types to retrieve (e.g., revenue, net income, total assets)
     * @param startDate Optional start date for the time series (default: null, returns all available data)
     * @param endDate Optional end date for the time series (default: null, returns up to latest available)
     * @return [FundamentalsTimeseriesResult] containing historical fundamental data
     * @throws UfcException if the symbol is invalid or the request fails
     *
     * @sample
     * ```kotlin
     * val fundamentals = ufc.fundamentalsTimeseries(
     *     symbol = "AAPL",
     *     types = listOf(FundamentalsType.TOTAL_REVENUE, FundamentalsType.NET_INCOME),
     *     startDate = LocalDate.of(2020, 1, 1)
     * )
     * ```
     */
    suspend fun fundamentalsTimeseries(
        symbol: String,
        types: List<FundamentalsType>,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): FundamentalsTimeseriesResult =
        yahoo.fundamentalsTimeseries(symbol, types, startDate, endDate)

    /**
     * Looks up securities by query string with optional type filtering.
     *
     * This method searches for stocks, ETFs, mutual funds, indices, futures, currencies,
     * and other financial instruments matching the query.
     *
     * @param query Search query string (e.g., "Apple", "AAPL", "technology")
     * @param type Type of security to search for (default: ALL)
     * @param count Maximum number of results to return (default: 25)
     * @return [LookupResult] containing matching securities
     * @throws UfcException if the request fails
     *
     * @sample
     * ```kotlin
     * val result = ufc.lookup("Apple", LookupType.EQUITY, count = 10)
     * result.quotes.forEach { quote ->
     *     println("${quote.symbol}: ${quote.longName}")
     * }
     * ```
     */
    suspend fun lookup(query: String, type: LookupType = LookupType.ALL, count: Int = 25): LookupResult =
        yahoo.lookup(query, type, count)

    /**
     * Retrieves market summary data for a specific market.
     *
     * This method provides overview information about a market including major indices,
     * top gainers, top losers, and market statistics.
     *
     * @param market The market code (e.g., US, JP, GB)
     * @return [MarketSummaryResult] containing market overview data
     * @throws UfcException if the request fails
     *
     * @sample
     * ```kotlin
     * val summary = ufc.marketSummary(MarketCode.US)
     * println("Market overview: ${summary.indices}")
     * ```
     */
    suspend fun marketSummary(market: MarketCode): MarketSummaryResult =
        yahoo.marketSummary(market)

    /**
     * Retrieves market trading hours and status for a specific market.
     *
     * This method provides information about market open/close times, pre-market,
     * post-market hours, and current trading status.
     *
     * @param market The market code (e.g., US, JP, GB)
     * @return [MarketTimeResult] containing market hours and status
     * @throws UfcException if the request fails
     *
     * @sample
     * ```kotlin
     * val marketTime = ufc.marketTime(MarketCode.US)
     * println("Market is open: ${marketTime.isMarketOpen}")
     * ```
     */
    suspend fun marketTime(market: MarketCode): MarketTimeResult =
        yahoo.marketTime(market)

    /**
     * Retrieves options chain data for a specific symbol and expiration date.
     *
     * This method provides call and put option contracts including strike prices,
     * bid/ask prices, implied volatility, Greeks, and open interest.
     *
     * @param symbol The ticker symbol (e.g., "AAPL")
     * @param expirationDate Optional Unix timestamp for specific expiration date
     *                       (default: null, returns nearest expiration)
     * @return [OptionsData] containing calls, puts, and available expiration dates
     * @throws UfcException if the symbol is invalid or has no options available
     *
     * @sample
     * ```kotlin
     * val options = ufc.options("AAPL")
     * println("Available expirations: ${options.expirationDates}")
     * options.calls.forEach { call ->
     *     println("Strike: ${call.strike}, Premium: ${call.lastPrice}")
     * }
     * ```
     */
    suspend fun options(symbol: String, expirationDate: Long? = null): OptionsData =
        yahoo.options(symbol, expirationDate)

    /**
     * Screens stocks using a custom query with filtering criteria.
     *
     * This method allows you to build complex queries to filter stocks based on
     * fundamental metrics like market cap, P/E ratio, sector, dividend yield, etc.
     *
     * @param query Custom screener query built using [ScreenerQuery] operators
     * @param sortField Field to sort results by (default: TICKER)
     * @param sortAsc Sort in ascending order if true, descending if false (default: false)
     * @param size Maximum number of results to return (default: 100)
     * @param offset Number of results to skip for pagination (default: 0)
     * @return Screener results matching the query criteria
     * @throws UfcException if the query is invalid or the request fails
     *
     * @sample
     * ```kotlin
     * val query = EquityQuery.and(
     *     EquityQuery.gt(EquityField.MARKET_CAP, 10_000_000_000),
     *     EquityQuery.lt(EquityField.PE_RATIO, 20),
     *     EquityQuery.eq(EquityField.SECTOR, "Technology")
     * )
     * val results = ufc.screener(query, sortField = ScreenerSortField.MARKET_CAP)
     * ```
     */
    suspend fun screener(query: ScreenerQuery, sortField: ScreenerSortField = ScreenerSortField.TICKER,
                         sortAsc: Boolean = false, size: Int = 100, offset: Int = 0) =
        yahoo.screener(query, sortField, sortAsc, size, offset)

    /**
     * Screens stocks using a predefined screener ID.
     *
     * This method uses Yahoo Finance's predefined screener IDs for common
     * screening strategies.
     *
     * @param predefinedId The predefined screener ID string
     * @param count Maximum number of results to return (default: 25)
     * @param sortField Optional field to sort results by
     * @param sortAsc Optional sort order (true for ascending, false for descending)
     * @return Screener results for the predefined screen
     * @throws UfcException if the screener ID is invalid or the request fails
     */
    suspend fun screener(predefinedId: String, count: Int = 25,
                         sortField: ScreenerSortField? = null, sortAsc: Boolean? = null) =
        yahoo.screener(predefinedId, count, sortField, sortAsc)

    /**
     * Screens stocks using a predefined screener enum.
     *
     * This method provides convenient access to Yahoo Finance's predefined screeners
     * such as day gainers, day losers, most active, etc.
     *
     * @param predefined The predefined screener enum (e.g., DAY_GAINERS, DAY_LOSERS, MOST_ACTIVE)
     * @param count Maximum number of results to return (default: 25)
     * @param sortField Optional field to sort results by
     * @param sortAsc Optional sort order (true for ascending, false for descending)
     * @return Screener results for the predefined screen
     * @throws UfcException if the request fails
     *
     * @sample
     * ```kotlin
     * val gainers = ufc.screener(PredefinedScreener.DAY_GAINERS, count = 10)
     * gainers.quotes.forEach { quote ->
     *     println("${quote.symbol}: ${quote.regularMarketChangePercent}%")
     * }
     * ```
     */
    suspend fun screener(predefined: PredefinedScreener, count: Int = 25,
                         sortField: ScreenerSortField? = null, sortAsc: Boolean? = null) =
        yahoo.screener(predefined, count, sortField, sortAsc)

    /**
     * Searches for symbols, companies, and related news.
     *
     * This method provides a unified search across quotes (stocks, ETFs, funds, etc.)
     * and news articles related to the search query.
     *
     * @param query Search query string (e.g., "Apple", "AAPL", "iPhone")
     * @param quotesCount Maximum number of quote results to return (default: 8)
     * @param newsCount Maximum number of news results to return (default: 8)
     * @param enableFuzzyQuery Enable fuzzy matching for the query (default: false)
     * @return [SearchResponse] containing matching quotes and news articles
     * @throws UfcException if the request fails
     *
     * @sample
     * ```kotlin
     * val results = ufc.search("Apple", quotesCount = 10, newsCount = 5)
     * results.quotes.forEach { println(it.symbol) }
     * results.news.forEach { println(it.title) }
     * ```
     */
    suspend fun search(query: String, quotesCount: Int = 8, newsCount: Int = 8, enableFuzzyQuery: Boolean = false): SearchResponse =
        yahoo.search(query, quotesCount, newsCount, enableFuzzyQuery)

    /**
     * Retrieves earnings calendar visualization data for a symbol.
     *
     * This method provides earnings data formatted for visualization purposes,
     * including historical and upcoming earnings announcements.
     *
     * @param symbol The ticker symbol (e.g., "AAPL")
     * @param limit Maximum number of earnings records to retrieve (default: 12)
     * @return [VisualizationEarningsCalendar] containing visualization-ready earnings data
     * @throws UfcException if the symbol is invalid or the request fails
     *
     * @sample
     * ```kotlin
     * val visualization = ufc.visualization("AAPL", limit = 8)
     * // Use this data to create earnings calendar charts
     * ```
     */
    suspend fun visualization(symbol: String, limit: Int = 12): VisualizationEarningsCalendar =
        yahoo.visualization(symbol, limit)

    // FRED API methods

    /**
     * Retrieves economic data series from the Federal Reserve Economic Data (FRED) API.
     *
     * This method provides access to thousands of economic time series including GDP,
     * unemployment rates, inflation, interest rates, and various economic indicators.
     *
     * Requires a FRED API key to be configured during client creation. API keys are
     * free and can be obtained from https://fred.stlouisfed.org/docs/api/api_key.html
     *
     * @param seriesId The FRED series ID (e.g., "GDP", "UNRATE", "CPIAUCSL")
     * @param startDate Optional start date for the data range (default: null, returns all available data)
     * @param endDate Optional end date for the data range (default: null, returns up to latest available)
     * @param frequency Optional data frequency aggregation (e.g., DAILY, WEEKLY, MONTHLY, QUARTERLY, ANNUAL)
     * @return Economic data series observations
     * @throws UfcException with [ErrorCode.CONFIGURATION_ERROR] if FRED API key is not configured,
     *                      or if the series ID is invalid or the request fails
     *
     * @sample
     * ```kotlin
     * // Requires FRED API key in config
     * val config = UfcConfig(fredApiKey = "your-api-key")
     * val ufc = Ufc.create(config)
     *
     * // Get GDP data
     * val gdp = ufc.series("GDP")
     *
     * // Get unemployment rate for specific date range
     * val unemployment = ufc.series(
     *     seriesId = "UNRATE",
     *     startDate = LocalDate.of(2020, 1, 1),
     *     endDate = LocalDate.now()
     * )
     * ```
     */
    suspend fun series(seriesId: String, startDate: LocalDate? = null,
                       endDate: LocalDate? = null, frequency: DataFrequency? = null) =
        fred?.series(seriesId, startDate, endDate, frequency)
            ?: throw UfcException(ErrorCode.CONFIGURATION_ERROR, "FRED API key not configured")

    // Business Insider API methods

    /**
     * Searches for a ticker symbol using an ISIN (International Securities Identification Number).
     *
     * This method converts ISIN codes to their corresponding ticker symbols using
     * Business Insider's API. Useful for international securities identification.
     *
     * @param isin The 12-character ISIN code (e.g., "US0378331005" for Apple)
     * @return [IsinSearchResult] containing the ticker symbol and related information
     * @throws UfcException if the ISIN is invalid or not found
     *
     * @sample
     * ```kotlin
     * val result = ufc.searchIsin("US0378331005")  // Apple's ISIN
     * println("Symbol: ${result.symbol}")  // Outputs: AAPL
     * ```
     */
    suspend fun searchIsin(isin: String): IsinSearchResult = businessInsider.searchIsin(isin)

    /**
     * Closes all underlying HTTP clients and resources.
     *
     * This method releases all network resources including HTTP clients for Yahoo Finance,
     * FRED, and Business Insider, as well as WebSocket streaming connections. Should be
     * called when the UFC client is no longer needed.
     *
     * It is recommended to use the `use` function for automatic resource management:
     * ```kotlin
     * Ufc.create().use { ufc ->
     *     // Use the client
     * }
     * // Automatically closed
     * ```
     */
    override fun close() {
        yahoo.close()
        fred?.close()
        businessInsider.close()
        streaming.close()
    }

    companion object {
        /**
         * Creates a new UFC client instance with default configuration.
         *
         * This factory method initializes all underlying clients (Yahoo Finance,
         * Business Insider, and WebSocket streaming) with default settings.
         * FRED client will not be available without an API key.
         *
         * @return A new [Ufc] instance with default configuration
         * @throws UfcException if client initialization fails
         *
         * @sample
         * ```kotlin
         * val ufc = Ufc.create()
         * val quote = ufc.quote("AAPL")
         * // Note: FRED API will not be available
         * ```
         */
        suspend fun create(): Ufc {
            val yahoo = YahooClient.create()
            val bi = BusinessInsiderClient.create()
            val streaming = StreamingClient.create()
            return Ufc(yahoo, null, bi, streaming)
        }

        /**
         * Creates a new UFC client instance with custom configuration.
         *
         * This factory method allows you to configure all aspects of the client including:
         * - FRED API key for economic data access
         * - Request and connection timeouts
         * - Client-specific configurations
         *
         * @param config [UfcConfig] containing configuration settings for all underlying clients
         * @return A new [Ufc] instance with the specified configuration
         * @throws UfcException if client initialization fails or configuration is invalid
         *
         * @sample
         * ```kotlin
         * val config = UfcConfig(
         *     fredApiKey = "your-fred-api-key",
         *     requestTimeoutMs = 30_000,
         *     connectTimeoutMs = 10_000
         * )
         * val ufc = Ufc.create(config)
         *
         * // Now FRED API is available
         * val gdp = ufc.series("GDP")
         * ```
         */
        suspend fun create(config: UfcConfig): Ufc {
            val yahoo = YahooClient.create(config.yahooConfig)
            val fred = config.fredApiKey?.let { FredClient.create(it, config.fredConfig) }
            val bi = BusinessInsiderClient.create(config.businessInsiderConfig)
            val streaming = StreamingClient.create(config.streamingConfig)
            return Ufc(yahoo, fred, bi, streaming)
        }
    }
}
