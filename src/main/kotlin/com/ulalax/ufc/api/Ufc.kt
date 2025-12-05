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
import java.time.LocalDate

class Ufc private constructor(
    val yahoo: YahooClient,
    val fred: FredClient?,
    val businessInsider: BusinessInsiderClient
) : AutoCloseable {

    // 직접 접근 - Yahoo
    suspend fun quote(symbol: String): QuoteData = yahoo.quote(symbol)

    suspend fun quote(symbols: List<String>): List<QuoteData> = yahoo.quote(symbols)

    suspend fun quoteSummary(symbol: String, vararg modules: QuoteSummaryModule): QuoteSummaryModuleResult =
        yahoo.quoteSummary(symbol, *modules)

    suspend fun chart(symbol: String, interval: Interval = Interval.OneDay,
                      period: Period = Period.OneYear, vararg events: ChartEventType) =
        yahoo.chart(symbol, interval, period, *events)

    suspend fun earningsCalendar(symbol: String, limit: Int = 12, offset: Int = 0) =
        yahoo.earningsCalendar(symbol, limit, offset)

    suspend fun fundamentalsTimeseries(
        symbol: String,
        types: List<FundamentalsType>,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): FundamentalsTimeseriesResult =
        yahoo.fundamentalsTimeseries(symbol, types, startDate, endDate)

    suspend fun lookup(query: String, type: LookupType = LookupType.ALL, count: Int = 25): LookupResult =
        yahoo.lookup(query, type, count)

    suspend fun marketSummary(market: MarketCode): MarketSummaryResult =
        yahoo.marketSummary(market)

    suspend fun marketTime(market: MarketCode): MarketTimeResult =
        yahoo.marketTime(market)

    suspend fun options(symbol: String, expirationDate: Long? = null): OptionsData =
        yahoo.options(symbol, expirationDate)

    suspend fun screener(query: ScreenerQuery, sortField: ScreenerSortField = ScreenerSortField.TICKER,
                         sortAsc: Boolean = false, size: Int = 100, offset: Int = 0) =
        yahoo.screener(query, sortField, sortAsc, size, offset)

    suspend fun screener(predefinedId: String, count: Int = 25,
                         sortField: ScreenerSortField? = null, sortAsc: Boolean? = null) =
        yahoo.screener(predefinedId, count, sortField, sortAsc)

    suspend fun screener(predefined: PredefinedScreener, count: Int = 25,
                         sortField: ScreenerSortField? = null, sortAsc: Boolean? = null) =
        yahoo.screener(predefined, count, sortField, sortAsc)

    suspend fun search(query: String, quotesCount: Int = 8, newsCount: Int = 8, enableFuzzyQuery: Boolean = false): SearchResponse =
        yahoo.search(query, quotesCount, newsCount, enableFuzzyQuery)

    suspend fun visualization(symbol: String, limit: Int = 12): VisualizationEarningsCalendar =
        yahoo.visualization(symbol, limit)

    // 직접 접근 - FRED
    suspend fun series(seriesId: String, startDate: LocalDate? = null,
                       endDate: LocalDate? = null, frequency: DataFrequency? = null) =
        fred?.series(seriesId, startDate, endDate, frequency)
            ?: throw UfcException(ErrorCode.CONFIGURATION_ERROR, "FRED API key not configured")

    // 직접 접근 - Business Insider
    suspend fun searchIsin(isin: String): IsinSearchResult = businessInsider.searchIsin(isin)

    override fun close() {
        yahoo.close()
        fred?.close()
        businessInsider.close()
    }

    companion object {
        suspend fun create(): Ufc {
            val yahoo = YahooClient.create()
            val bi = BusinessInsiderClient.create()
            return Ufc(yahoo, null, bi)
        }

        suspend fun create(config: UfcConfig): Ufc {
            val yahoo = YahooClient.create(config.yahooConfig)
            val fred = config.fredApiKey?.let { FredClient.create(it, config.fredConfig) }
            val bi = BusinessInsiderClient.create(config.businessInsiderConfig)
            return Ufc(yahoo, fred, bi)
        }
    }
}
