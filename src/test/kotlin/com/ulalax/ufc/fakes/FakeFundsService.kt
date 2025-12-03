package com.ulalax.ufc.fakes

import com.ulalax.ufc.domain.funds.AssetClasses
import com.ulalax.ufc.domain.funds.BondHoldingsMetrics
import com.ulalax.ufc.domain.funds.EquityHoldingsMetrics
import com.ulalax.ufc.domain.funds.FundData
import com.ulalax.ufc.domain.funds.FundOperations
import com.ulalax.ufc.domain.funds.FundOverview
import com.ulalax.ufc.domain.funds.FundsService
import com.ulalax.ufc.domain.funds.Holding
import com.ulalax.ufc.domain.funds.MetricValue
import com.ulalax.ufc.domain.funds.OperationMetric
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.infrastructure.yahoo.response.FundDataResponse

/**
 * 테스트용 Fake FundsService 구현체.
 *
 * 사전에 정의된 응답을 반환하여 실제 API 호출 없이 테스트를 수행합니다.
 */
class FakeFundsService : FundsService {

    private val responses = mutableMapOf<String, FundData>()
    private val isFundMap = mutableMapOf<String, Boolean>()
    private val rawResponses = mutableMapOf<String, FundDataResponse>()

    /**
     * 테스트 데이터 추가.
     *
     * @param symbol 펀드 심볼
     * @param data 펀드 데이터
     */
    fun addResponse(symbol: String, data: FundData) {
        responses[symbol] = data
        isFundMap[symbol] = true
    }

    /**
     * 펀드 여부 응답 설정.
     *
     * @param symbol 심볼
     * @param isFund 펀드 여부
     */
    fun setIsFund(symbol: String, isFund: Boolean) {
        isFundMap[symbol] = isFund
    }

    /**
     * Raw 응답 추가.
     *
     * @param symbol 펀드 심볼
     * @param response 원본 API 응답
     */
    fun addRawResponse(symbol: String, response: FundDataResponse) {
        rawResponses[symbol] = response
    }

    /**
     * 기본 테스트 데이터 추가.
     */
    fun addDefaultResponses() {
        // SPY ETF
        addResponse(
            "SPY",
            FundData(
                symbol = "SPY",
                quoteType = "ETF",
                description = "The SPDR S&P 500 ETF Trust aims to provide investment results that correspond to the price and yield performance of the S&P 500 Index.",
                fundOverview = FundOverview(
                    categoryName = "Large Blend",
                    family = "SPDR State Street Global Advisors",
                    legalType = "Exchange Traded Fund"
                ),
                fundOperations = FundOperations(
                    annualReportExpenseRatio = OperationMetric(fundValue = 0.03, categoryAverage = null),
                    annualHoldingsTurnover = OperationMetric(fundValue = 5.0, categoryAverage = null),
                    totalNetAssets = OperationMetric(fundValue = 400_000_000_000.0, categoryAverage = null)
                ),
                assetClasses = null,
                topHoldings = listOf(
                    Holding(symbol = "AAPL", name = "Apple Inc", holdingPercent = 7.2),
                    Holding(symbol = "MSFT", name = "Microsoft Corporation", holdingPercent = 6.5),
                    Holding(symbol = "NVDA", name = "NVIDIA Corporation", holdingPercent = 5.3)
                ),
                equityHoldings = EquityHoldingsMetrics(
                    priceToEarnings = MetricValue(fundValue = 22.5, categoryAverage = null),
                    priceToBook = MetricValue(fundValue = 4.5, categoryAverage = null),
                    priceToSales = MetricValue(fundValue = 3.2, categoryAverage = null),
                    priceToCashflow = MetricValue(fundValue = 20.0, categoryAverage = null),
                    medianMarketCap = MetricValue(fundValue = 850_000_000_000.0, categoryAverage = null),
                    threeYearEarningsGrowth = MetricValue(fundValue = 12.5, categoryAverage = null)
                ),
                bondHoldings = null,
                bondRatings = null,
                sectorWeightings = mapOf(
                    "Technology" to 28.5,
                    "Healthcare" to 12.0,
                    "Finance" to 11.5,
                    "Industrials" to 9.0
                )
            )
        )

        // AGG ETF (Bond)
        addResponse(
            "AGG",
            FundData(
                symbol = "AGG",
                quoteType = "ETF",
                description = "The iShares Core U.S. Aggregate Bond ETF seeks to track the investment results of an index comprised of the total U.S. investment-grade bond market.",
                fundOverview = FundOverview(
                    categoryName = "Intermediate Core Bond",
                    family = "iShares",
                    legalType = "Exchange Traded Fund"
                ),
                fundOperations = FundOperations(
                    annualReportExpenseRatio = OperationMetric(fundValue = 0.03, categoryAverage = null),
                    annualHoldingsTurnover = OperationMetric(fundValue = 25.0, categoryAverage = null),
                    totalNetAssets = OperationMetric(fundValue = 100_000_000_000.0, categoryAverage = null)
                ),
                assetClasses = null,
                topHoldings = listOf(
                    Holding(symbol = "US0378331005", name = "US Treasury Bond", holdingPercent = 15.0),
                    Holding(symbol = "US4581401001", name = "Intel Corp Bond", holdingPercent = 2.5)
                ),
                equityHoldings = null,
                bondHoldings = BondHoldingsMetrics(
                    duration = MetricValue(fundValue = 5.5, categoryAverage = null),
                    maturity = MetricValue(fundValue = 8.5, categoryAverage = null),
                    creditQuality = MetricValue(fundValue = 4.5, categoryAverage = null)
                ),
                bondRatings = null,
                sectorWeightings = null
            )
        )
    }

    // ========================================
    // Public API Implementation
    // ========================================

    override suspend fun getFundData(symbol: String): FundData {
        return responses[symbol]
            ?: throw UfcException(
                ErrorCode.FUND_DATA_NOT_FOUND,
                "Test data not found for symbol: $symbol"
            )
    }

    override suspend fun getFundData(symbols: List<String>): Map<String, FundData> {
        val result = mutableMapOf<String, FundData>()
        symbols.forEach { symbol ->
            responses[symbol]?.let { result[symbol] = it }
        }

        if (result.isEmpty() && symbols.isNotEmpty()) {
            throw UfcException(
                ErrorCode.FUND_DATA_NOT_FOUND,
                "No fund data found for any of the symbols"
            )
        }

        return result
    }

    override suspend fun getRawFundData(symbol: String): FundDataResponse {
        return rawResponses[symbol]
            ?: throw UfcException(
                ErrorCode.FUND_DATA_NOT_FOUND,
                "Test raw data not found for symbol: $symbol"
            )
    }

    override suspend fun isFund(symbol: String): Boolean {
        return isFundMap[symbol]
            ?: throw UfcException(
                ErrorCode.FUND_DATA_NOT_FOUND,
                "Test data not found for symbol: $symbol"
            )
    }
}
