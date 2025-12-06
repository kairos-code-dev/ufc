package com.ulalax.ufc.domain.model.quote

/**
 * Modules supported by the Yahoo Finance QuoteSummary API
 *
 * The QuoteSummary API provides detailed information about stocks, ETFs, mutual funds, etc.
 * through various modules. Each module represents data for a specific category.
 *
 * Usage example:
 * ```kotlin
 * val modules = setOf(
 *     QuoteSummaryModule.PRICE,
 *     QuoteSummaryModule.SUMMARY_DETAIL,
 *     QuoteSummaryModule.FINANCIAL_DATA
 * )
 * val result = quoteSummaryService.getQuoteSummary("AAPL", modules)
 * ```
 *
 * @property apiValue The module identifier used by the Yahoo Finance API
 */
enum class QuoteSummaryModule(val apiValue: String) {
    /**
     * Basic price information
     *
     * Includes basic price data such as current price, exchange, symbol, 52-week high/low, etc.
     */
    PRICE("price"),

    /**
     * Detailed summary information
     *
     * Includes key statistics data such as dividends, P/E ratio, beta, volume, market cap, etc.
     */
    SUMMARY_DETAIL("summaryDetail"),

    /**
     * Asset profile information
     *
     * Includes company profile information such as description, sector, industry, website, address, employee count, etc.
     */
    ASSET_PROFILE("assetProfile"),

    /**
     * Summary profile information
     *
     * Similar to assetProfile but in a more condensed format.
     */
    SUMMARY_PROFILE("summaryProfile"),

    /**
     * Asset type information
     *
     * Includes asset type information such as EQUITY, ETF, MUTUALFUND, INDEX, CRYPTOCURRENCY, etc.
     */
    QUOTE_TYPE("quoteType"),

    /**
     * Default key statistics
     *
     * Includes key statistics data such as shares outstanding, ISIN, CUSIP, etc.
     */
    DEFAULT_KEY_STATISTICS("defaultKeyStatistics"),

    /**
     * Financial data
     *
     * Includes financial information such as cash flow, debt, ROE, ROA, PEG ratio, target price, etc.
     */
    FINANCIAL_DATA("financialData"),

    /**
     * Calendar events
     *
     * Includes information about key events such as dividend dates, earnings announcement dates, etc.
     */
    CALENDAR_EVENTS("calendarEvents"),

    /**
     * Earnings trend
     *
     * Includes quarterly earnings estimates and actual earnings comparison data.
     */
    EARNINGS_TREND("earningsTrend"),

    /**
     * Earnings history
     *
     * Includes historical earnings announcement data.
     */
    EARNINGS_HISTORY("earningsHistory"),

    /**
     * Earnings dates
     *
     * Includes information about upcoming earnings announcement dates.
     */
    EARNINGS_DATES("earningsDates"),

    /**
     * Major holders
     *
     * Includes information about institutional investors and major shareholders.
     */
    MAJOR_HOLDERS("majorHolders"),

    /**
     * Insider transactions
     *
     * Includes buy/sell transaction history by executives and insiders.
     */
    INSIDER_TRANSACTIONS("insiderTransactions"),

    /**
     * Insider holders
     *
     * Includes detailed ownership information by insiders and institutions.
     */
    INSIDER_HOLDERS("insiderHolders"),

    /**
     * Institutional ownership
     *
     * Includes ownership details by institutional investors such as mutual funds.
     */
    INSTITUTION_OWNERSHIP("institutionOwnership"),

    /**
     * Fund ownership
     *
     * Includes ownership information by mutual funds.
     */
    FUND_OWNERSHIP("fundOwnership"),

    /**
     * Recommendation trend
     *
     * Includes analyst buy/sell/hold recommendation information.
     */
    RECOMMENDATION_TREND("recommendationTrend"),

    /**
     * Upgrade/downgrade history
     *
     * Includes analyst rating change history.
     */
    UPGRADE_DOWNGRADE_HISTORY("upgradeDowngradeHistory"),

    /**
     * Financial statements - Income statement
     *
     * Includes annual and quarterly income statement data.
     */
    INCOME_STATEMENT_HISTORY("incomeStatementHistory"),

    /**
     * Financial statements - Quarterly income statement
     *
     * Includes quarterly income statement data.
     */
    INCOME_STATEMENT_HISTORY_QUARTERLY("incomeStatementHistoryQuarterly"),

    /**
     * Financial statements - Balance sheet
     *
     * Includes annual balance sheet data.
     */
    BALANCE_SHEET_HISTORY("balanceSheetHistory"),

    /**
     * Financial statements - Quarterly balance sheet
     *
     * Includes quarterly balance sheet data.
     */
    BALANCE_SHEET_HISTORY_QUARTERLY("balanceSheetHistoryQuarterly"),

    /**
     * Financial statements - Cash flow statement
     *
     * Includes annual cash flow statement data.
     */
    CASHFLOW_STATEMENT_HISTORY("cashflowStatementHistory"),

    /**
     * Financial statements - Quarterly cash flow statement
     *
     * Includes quarterly cash flow statement data.
     */
    CASHFLOW_STATEMENT_HISTORY_QUARTERLY("cashflowStatementHistoryQuarterly"),

    /**
     * Top holdings of fund
     *
     * Includes information about major holdings of ETFs and mutual funds.
     */
    TOP_HOLDINGS("topHoldings"),

    /**
     * Fund profile
     *
     * Includes fund-related information such as category, family, fees, etc.
     */
    FUND_PROFILE("fundProfile"),

    /**
     * Fund performance
     *
     * Includes fund returns and performance metrics.
     */
    FUND_PERFORMANCE("fundPerformance"),

    /**
     * Stock splits and dividend history
     *
     * Includes stock split and dividend payment history.
     */
    SEC_FILINGS("secFilings"),

    /**
     * Price history
     *
     * Includes historical price data.
     */
    PRICE_HISTORY("priceHistory"),

    /**
     * Index trend
     *
     * Includes market index-related trend information.
     */
    INDEX_TREND("indexTrend"),

    /**
     * Industry trend
     *
     * Includes industry-specific trend information.
     */
    INDUSTRY_TREND("industryTrend"),

    /**
     * Sector trend
     *
     * Includes sector-specific trend information.
     */
    SECTOR_TREND("sectorTrend"),

    /**
     * Earnings history
     *
     * Includes historical earnings announcements and EPS data.
     */
    EARNINGS("earnings"),

    /**
     * Page views information
     *
     * Includes page view statistics for the symbol.
     */
    PAGE_VIEWS("pageViews"),

    /**
     * ESG scores
     *
     * Includes Environmental (E), Social (S), and Governance (G) rating scores.
     */
    ESG_SCORES("esgScores"),

    /**
     * Net asset value
     *
     * Includes NAV information for ETFs and mutual funds.
     */
    NET_SHARE_PURCHASE_ACTIVITY("netSharePurchaseActivity");

    companion object {
        /**
         * Finds QuoteSummaryModule from API value.
         *
         * @param apiValue Yahoo Finance API module identifier
         * @return The corresponding QuoteSummaryModule or null
         */
        fun fromApiValue(apiValue: String): QuoteSummaryModule? {
            return entries.find { it.apiValue == apiValue }
        }

        /**
         * Returns a Set of all modules.
         *
         * @return A Set of all QuoteSummaryModules
         */
        fun allModules(): Set<QuoteSummaryModule> {
            return entries.toSet()
        }

        /**
         * Returns a Set of modules commonly used for stocks (EQUITY).
         *
         * @return A Set of key modules for stocks
         */
        fun stockModules(): Set<QuoteSummaryModule> {
            return setOf(
                PRICE,
                SUMMARY_DETAIL,
                ASSET_PROFILE,
                QUOTE_TYPE,
                DEFAULT_KEY_STATISTICS,
                FINANCIAL_DATA,
                EARNINGS_TREND,
                EARNINGS_HISTORY,
                RECOMMENDATION_TREND,
                MAJOR_HOLDERS,
                INSIDER_TRANSACTIONS
            )
        }

        /**
         * Returns a Set of modules commonly used for ETFs and funds.
         *
         * @return A Set of key modules for funds
         */
        fun fundModules(): Set<QuoteSummaryModule> {
            return setOf(
                PRICE,
                SUMMARY_DETAIL,
                QUOTE_TYPE,
                TOP_HOLDINGS,
                FUND_PROFILE,
                FUND_PERFORMANCE
            )
        }
    }
}

/**
 * Converts QuoteSummaryModule to API parameter string.
 *
 * @return API parameter value
 */
fun QuoteSummaryModule.toApiValue(): String = apiValue

/**
 * Converts a Set of QuoteSummaryModules to a comma-separated API parameter string.
 *
 * @return Comma-separated module string
 */
fun Set<QuoteSummaryModule>.toApiValue(): String {
    return joinToString(",") { it.apiValue }
}
