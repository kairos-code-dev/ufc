package com.ulalax.ufc.domain.funds

import com.ulalax.ufc.domain.quote.QuoteSummaryResponse
import com.ulalax.ufc.domain.quote.QuoteSummaryResult
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.infrastructure.util.CacheHelper
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.hours

/**
 * Funds 도메인 서비스 구현체
 *
 * 책임:
 * - 오케스트레이션 (캐시 → HTTP → 파싱)
 * - 도메인 검증
 * - JSON 파싱 (지역성 원칙: 관련 로직을 가까이 배치)
 *
 * 의존성:
 * - FundsHttpClient (인터페이스): 테스트 격리 가능
 * - CacheHelper (구체 클래스): 인메모리로 충분히 빠름
 *
 * 도메인 순수성:
 * - Ktor HttpClient에 직접 의존하지 않음
 * - FundsHttpClient 인터페이스만 의존 (의존성 역전)
 *
 * 문맥의 지역성:
 * - 파싱 로직이 Service 내부에 위치
 * - 별도 Parser 클래스를 만들지 않음 (구현체 하나, YAGNI)
 *
 * @property httpClient HTTP 통신 인터페이스 (테스트 시 Fake로 교체 가능)
 * @property cache 캐싱 유틸리티
 */
class FundsServiceImpl(
    private val httpClient: FundsHttpClient,
    private val cache: CacheHelper
) : FundsService {

    companion object {
        private val logger = LoggerFactory.getLogger(FundsServiceImpl::class.java)

        // 캐시 TTL 설정: 펀드 정보는 거의 변경되지 않으므로 24시간
        private val FUND_DATA_TTL = 24.hours

        private const val MAX_BATCH_SIZE = 50

        // 펀드 데이터 조회에 사용할 모듈 목록
        private val FUND_MODULES = listOf(
            "quoteType",
            "summaryProfile",
            "topHoldings",
            "fundProfile",
            "defaultKeyStatistics"
        )
    }

    // ============================================================================
    // Public API Methods
    // ============================================================================

    override suspend fun getFundData(symbol: String): FundData {
        validateSymbol(symbol)

        logger.debug("Fetching fund data: symbol={}", symbol)

        return cache.getOrPut("funds:data:$symbol", ttl = FUND_DATA_TTL) {
            val response = httpClient.fetchQuoteSummary(symbol, FUND_MODULES)
            parseFundData(symbol, response)
        }
    }

    override suspend fun getFundData(symbols: List<String>): Map<String, FundData> {
        if (symbols.isEmpty()) return emptyMap()
        require(symbols.size <= MAX_BATCH_SIZE) { "최대 $MAX_BATCH_SIZE 개의 심볼만 조회 가능합니다" }

        symbols.forEach { validateSymbol(it) }

        logger.debug("Fetching fund data for {} symbols", symbols.size)

        // 병렬 처리
        val results = mutableMapOf<String, FundData>()
        val failed = mutableListOf<String>()

        coroutineScope {
            symbols.map { symbol ->
                async {
                    try {
                        symbol to getFundData(symbol)
                    } catch (e: Exception) {
                        logger.warn("Failed to fetch fund data: symbol={}, error={}", symbol, e.message)
                        failed.add(symbol)
                        null
                    }
                }
            }
        }.mapNotNull { it.await() }.forEach { (symbol, data) ->
            results[symbol] = data
        }

        // 모든 조회가 실패한 경우 예외 발생
        if (results.isEmpty() && symbols.isNotEmpty()) {
            throw UfcException(
                ErrorCode.FUND_DATA_NOT_FOUND,
                "모든 심볼의 펀드 데이터 조회에 실패했습니다"
            )
        }

        if (failed.isNotEmpty()) {
            logger.info("Partial success for batch fund data: failed={}", failed)
        }

        return results
    }

    override suspend fun isFund(symbol: String): Boolean {
        validateSymbol(symbol)

        return try {
            logger.debug("Checking if symbol is a fund: symbol={}", symbol)
            val response = httpClient.fetchQuoteSummary(symbol, listOf("quoteType"))

            val result = response.quoteSummary.result?.firstOrNull() ?: return false
            val quoteType = result.quoteType?.quoteType ?: return false

            quoteType == "ETF" || quoteType == "MUTUALFUND"
        } catch (e: Exception) {
            logger.debug("Error checking if symbol is a fund: symbol={}", symbol, e)
            false
        }
    }

    // ============================================================================
    // Private: 도메인 검증
    // ============================================================================

    /**
     * 심볼 검증
     */
    private fun validateSymbol(symbol: String) {
        if (symbol.isBlank()) {
            throw UfcException(ErrorCode.INVALID_SYMBOL, "심볼이 비어있습니다")
        }

        if (symbol.length > 10) {
            throw UfcException(ErrorCode.INVALID_SYMBOL, "심볼이 너무 깁니다: $symbol (최대 10자)")
        }
    }

    // ============================================================================
    // Private: JSON 파싱 (지역성 원칙)
    //
    // 별도 Parser 클래스를 만들지 않는 이유:
    // - 구현체가 하나뿐 (Yahoo만)
    // - 파싱 로직이 Service와 강하게 결합
    // - 문맥의 지역성: 관련 로직을 가까이 배치
    // ============================================================================

    /**
     * FundData 파싱
     */
    private fun parseFundData(symbol: String, response: QuoteSummaryResponse): FundData {
        val result = response.quoteSummary.result?.firstOrNull()
            ?: throw UfcException(
                ErrorCode.FUND_DATA_NOT_FOUND,
                "펀드 데이터를 찾을 수 없습니다: $symbol"
            )

        // quoteType 검증
        val quoteType = result.quoteType?.quoteType
            ?: throw UfcException(
                ErrorCode.INVALID_FUND_TYPE,
                "Quote type을 찾을 수 없습니다: $symbol"
            )

        if (quoteType != "ETF" && quoteType != "MUTUALFUND") {
            throw UfcException(
                ErrorCode.INVALID_FUND_TYPE,
                "심볼 $symbol 은(는) 펀드가 아닙니다 (type: $quoteType)"
            )
        }

        return FundData(
            symbol = symbol,
            quoteType = quoteType,
            description = parseDescription(result),
            fundOverview = parseFundOverview(result),
            fundOperations = parseFundOperations(result),
            assetClasses = parseAssetClasses(result),
            topHoldings = parseTopHoldings(result),
            equityHoldings = parseEquityHoldings(result),
            bondHoldings = parseBondHoldings(result),
            bondRatings = parseBondRatings(result),
            sectorWeightings = parseSectorWeightings(result)
        )
    }

    /**
     * Description 파싱
     */
    private fun parseDescription(result: QuoteSummaryResult): String? {
        // summaryProfile에는 longBusinessSummary가 없음
        // 필요시 API 응답에서 직접 추출해야 함
        return null
    }

    /**
     * FundOverview 파싱
     */
    private fun parseFundOverview(result: QuoteSummaryResult): FundOverview? {
        val fundProfile = result.fundProfile ?: return null

        return FundOverview(
            categoryName = fundProfile.categoryName,
            family = fundProfile.family,
            legalType = fundProfile.legalType
        )
    }

    /**
     * FundOperations 파싱
     */
    private fun parseFundOperations(result: QuoteSummaryResult): FundOperations? {
        val fundProfile = result.fundProfile ?: return null
        val fees = fundProfile.feesExpensesInvestment ?: return null

        return FundOperations(
            annualReportExpenseRatio = fees.annualReportExpenseRatio?.doubleValue?.let {
                OperationMetric(fundValue = it, categoryAverage = null)
            },
            annualHoldingsTurnover = fees.annualHoldingsTurnover?.doubleValue?.let {
                OperationMetric(fundValue = it, categoryAverage = null)
            },
            totalNetAssets = fees.totalNetAssets?.longValue?.let {
                OperationMetric(fundValue = it.toDouble(), categoryAverage = null)
            }
        )
    }

    /**
     * AssetClasses 파싱
     */
    private fun parseAssetClasses(result: QuoteSummaryResult): AssetClasses? {
        val topHoldings = result.topHoldings ?: return null

        // AssetClasses는 직접 필드가 없으므로 null 반환
        // API 응답에 cashPosition, stockPosition 등이 없음
        return null
    }

    /**
     * TopHoldings 파싱
     */
    private fun parseTopHoldings(result: QuoteSummaryResult): List<Holding>? {
        val topHoldings = result.topHoldings?.holdings ?: return null

        return topHoldings.mapNotNull { holding ->
            val symbol = holding.symbol ?: return@mapNotNull null
            val name = holding.name ?: return@mapNotNull null
            val percent = holding.holdingPercent?.doubleValue ?: return@mapNotNull null

            Holding(
                symbol = symbol,
                name = name,
                holdingPercent = percent
            )
        }
    }

    /**
     * EquityHoldings 파싱
     */
    private fun parseEquityHoldings(result: QuoteSummaryResult): EquityHoldingsMetrics? {
        val equityHoldings = result.topHoldings?.equityHoldings ?: return null

        return EquityHoldingsMetrics(
            priceToEarnings = equityHoldings.priceToEarnings?.doubleValue?.let {
                MetricValue(fundValue = it, categoryAverage = null)
            },
            priceToBook = equityHoldings.priceToBook?.doubleValue?.let {
                MetricValue(fundValue = it, categoryAverage = null)
            },
            priceToSales = equityHoldings.priceToSales?.doubleValue?.let {
                MetricValue(fundValue = it, categoryAverage = null)
            },
            priceToCashflow = equityHoldings.priceToCashflow?.doubleValue?.let {
                MetricValue(fundValue = it, categoryAverage = null)
            },
            medianMarketCap = equityHoldings.medianMarketCap?.longValue?.let {
                MetricValue(fundValue = it.toDouble(), categoryAverage = null)
            },
            threeYearEarningsGrowth = equityHoldings.threeYearEarningsGrowth?.doubleValue?.let {
                MetricValue(fundValue = it, categoryAverage = null)
            }
        )
    }

    /**
     * BondHoldings 파싱
     */
    private fun parseBondHoldings(result: QuoteSummaryResult): BondHoldingsMetrics? {
        val bondHoldings = result.topHoldings?.bondHoldings ?: return null

        return BondHoldingsMetrics(
            duration = bondHoldings.duration?.doubleValue?.let {
                MetricValue(fundValue = it, categoryAverage = null)
            },
            maturity = bondHoldings.maturity?.doubleValue?.let {
                MetricValue(fundValue = it, categoryAverage = null)
            },
            creditQuality = bondHoldings.creditQuality?.doubleValue?.let {
                MetricValue(fundValue = it, categoryAverage = null)
            }
        )
    }

    /**
     * BondRatings 파싱
     */
    private fun parseBondRatings(result: QuoteSummaryResult): Map<String, Double>? {
        // API 응답에 bondRatings가 없음
        return null
    }

    /**
     * SectorWeightings 파싱
     */
    private fun parseSectorWeightings(result: QuoteSummaryResult): Map<String, Double>? {
        val sectorWeightings = result.topHoldings?.sectorWeightings ?: return null

        return sectorWeightings.mapNotNull { weighting ->
            val sectorName = weighting.sector ?: return@mapNotNull null
            val weight = weighting.weight?.doubleValue ?: return@mapNotNull null

            sectorName to weight
        }.toMap()
    }
}
