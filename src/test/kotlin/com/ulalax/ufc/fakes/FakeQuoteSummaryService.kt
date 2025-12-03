package com.ulalax.ufc.fakes

import com.ulalax.ufc.domain.quote.*
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import java.util.concurrent.ConcurrentHashMap

/**
 * QuoteSummary 서비스의 Fake 구현체.
 *
 * 테스트를 위해 메모리 기반으로 동작합니다.
 */
class FakeQuoteSummaryService : QuoteSummaryService {

    private val responses = ConcurrentHashMap<String, QuoteSummaryResult>()
    private val rawResponses = ConcurrentHashMap<String, QuoteSummaryResponse>()
    private var failingSymbols = setOf<String>()

    fun setResponse(symbol: String, response: QuoteSummaryResponse) {
        rawResponses[symbol] = response
        response.quoteSummary.result?.firstOrNull()?.let {
            responses[symbol] = it
        }
    }

    fun setFailingSymbols(symbols: Set<String>) {
        failingSymbols = symbols
    }

    fun clearResponses() {
        responses.clear()
        rawResponses.clear()
    }

    fun reset() {
        clearResponses()
        failingSymbols = emptySet()
    }

    override suspend fun getQuoteSummary(symbol: String): QuoteSummaryResult {
        if (symbol in failingSymbols) {
            throw UfcException(
                errorCode = ErrorCode.DATA_NOT_FOUND,
                message = "요약 정보를 찾을 수 없습니다: $symbol"
            )
        }
        return responses[symbol]
            ?: throw UfcException(
                errorCode = ErrorCode.DATA_NOT_FOUND,
                message = "요약 정보를 찾을 수 없습니다: $symbol"
            )
    }

    override suspend fun getQuoteSummary(
        symbol: String,
        modules: List<String>
    ): QuoteSummaryResult {
        return getQuoteSummary(symbol)
    }

    override suspend fun getQuoteSummary(symbols: List<String>): Map<String, QuoteSummaryResult> {
        return symbols.mapNotNull { symbol ->
            try {
                symbol to getQuoteSummary(symbol)
            } catch (e: Exception) {
                null
            }
        }.toMap()
    }

    override suspend fun getQuoteSummary(
        symbols: List<String>,
        modules: List<String>
    ): Map<String, QuoteSummaryResult> {
        return getQuoteSummary(symbols)
    }

    override suspend fun getStockSummary(symbol: String): StockSummary {
        val result = getQuoteSummary(symbol)
        return convertToStockSummary(symbol, result)
    }

    override suspend fun getStockSummary(symbols: List<String>): Map<String, StockSummary> {
        return symbols.mapNotNull { symbol ->
            try {
                symbol to getStockSummary(symbol)
            } catch (e: Exception) {
                null
            }
        }.toMap()
    }

    override suspend fun getRawQuoteSummary(
        symbol: String,
        modules: List<String>?
    ): QuoteSummaryResponse {
        if (symbol in failingSymbols) {
            throw UfcException(
                errorCode = ErrorCode.DATA_NOT_FOUND,
                message = "요약 정보를 찾을 수 없습니다: $symbol"
            )
        }
        return rawResponses[symbol]
            ?: throw UfcException(
                errorCode = ErrorCode.DATA_NOT_FOUND,
                message = "요약 정보를 찾을 수 없습니다: $symbol"
            )
    }

    private fun convertToStockSummary(symbol: String, result: QuoteSummaryResult): StockSummary {
        val price = result.price
        val summaryDetail = result.summaryDetail
        val financialData = result.financialData

        return StockSummary(
            symbol = symbol,
            currency = price?.currency,
            exchange = price?.exchange,
            currentPrice = price?.regularMarketPrice?.doubleValue,
            previousClose = price?.regularMarketPreviousClose?.doubleValue,
            dayHigh = summaryDetail?.regularMarketDayHigh?.doubleValue,
            dayLow = summaryDetail?.regularMarketDayLow?.doubleValue,
            fiftyTwoWeekHigh = summaryDetail?.fiftyTwoWeekHigh?.doubleValue,
            fiftyTwoWeekLow = summaryDetail?.fiftyTwoWeekLow?.doubleValue,
            fiftyTwoWeekChange = price?.fiftyTwoWeekHigh?.doubleValue?.minus(
                price.fiftyTwoWeekLow?.doubleValue ?: 0.0
            ),
            fiftyTwoWeekChangePercent = price?.fiftyTwoWeekChangePercent?.doubleValue,
            volume = summaryDetail?.regularMarketVolume?.longValue,
            averageVolume = summaryDetail?.averageVolume?.longValue,
            averageVolume10Days = summaryDetail?.averageVolume10days?.longValue,
            marketCap = summaryDetail?.marketCap?.longValue,
            sharesOutstanding = summaryDetail?.sharesOutstanding?.longValue,
            dividendRate = summaryDetail?.dividendRate?.doubleValue,
            dividendYield = summaryDetail?.dividendYield?.doubleValue,
            dividendDate = summaryDetail?.dividendDate?.longValue,
            exDividendDate = summaryDetail?.exDividendDate?.longValue,
            trailingPE = summaryDetail?.trailingPE?.doubleValue,
            forwardPE = summaryDetail?.forwardPE?.doubleValue,
            priceToBook = summaryDetail?.priceToBook?.doubleValue,
            beta = summaryDetail?.beta?.doubleValue,
            debtToEquity = financialData?.totalDebt?.doubleValue?.div(
                financialData.totalCash?.doubleValue ?: 1.0
            ),
            operatingCashflow = financialData?.operatingCashflow?.longValue,
            freeCashflow = financialData?.freeCashflow?.longValue,
            totalDebt = financialData?.totalDebt?.longValue,
            totalCash = financialData?.totalCash?.longValue,
            currentRatio = financialData?.currentRatio?.doubleValue,
            returnOnEquity = financialData?.returnOnEquity?.doubleValue,
            returnOnAssets = financialData?.returnOnAssets?.doubleValue,
            profitMargins = financialData?.profitMargins?.doubleValue,
            revenueGrowth = financialData?.revenueGrowth?.doubleValue,
            earningsGrowth = financialData?.earningsGrowth?.doubleValue,
            recommendationKey = financialData?.recommendationKey,
            numberOfAnalysts = financialData?.numberOfAnalysts,
            targetPriceMean = financialData?.targetPriceMean?.doubleValue,
            targetPriceLow = financialData?.targetPriceLow?.doubleValue,
            targetPriceHigh = financialData?.targetPriceHigh?.doubleValue
        )
    }
}
