package com.ulalax.ufc.domain.stock

import com.ulalax.ufc.domain.quote.QuoteSummaryResponse
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.infrastructure.util.CacheHelper
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.time.LocalDate
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Stock 도메인 서비스 구현체
 *
 * 책임:
 * - 오케스트레이션 (캐시 → HTTP → 파싱)
 * - 도메인 검증
 * - JSON 파싱 (지역성 원칙: 관련 로직을 가까이 배치)
 *
 * 의존성:
 * - StockHttpClient (인터페이스): 테스트 격리 가능
 * - CacheHelper (구체 클래스): 인메모리로 충분히 빠름
 *
 * 도메인 순수성:
 * - Ktor HttpClient에 직접 의존하지 않음
 * - StockHttpClient 인터페이스만 의존 (의존성 역전)
 *
 * 문맥의 지역성:
 * - 파싱 로직이 Service 내부에 위치
 * - 별도 Parser 클래스를 만들지 않음 (구현체 하나, YAGNI)
 *
 * @property httpClient HTTP 통신 인터페이스 (테스트 시 Fake로 교체 가능)
 * @property cache 캐싱 유틸리티
 */
class StockServiceImpl(
    private val httpClient: StockHttpClient,
    private val cache: CacheHelper
) : StockService {

    companion object {
        private val logger = LoggerFactory.getLogger(StockServiceImpl::class.java)

        // 캐시 TTL 설정
        private val COMPANY_INFO_TTL = 24.hours    // 회사 정보: 24시간
        private val FAST_INFO_TTL = 24.hours       // 빠른 정보: 24시간
        private val ISIN_TTL = 720.hours           // ISIN: 30일 (거의 영구)
        private val SHARES_TTL = 1.hours           // 발행주식수: 1시간

        private const val MAX_BATCH_SIZE = 50
    }

    // ============================================================================
    // Public API Methods
    // ============================================================================

    override suspend fun getCompanyInfo(symbol: String): CompanyInfo {
        validateSymbol(symbol)

        logger.debug("Fetching company info: symbol={}", symbol)

        return cache.getOrPut("stock:company:$symbol", ttl = COMPANY_INFO_TTL) {
            val response = httpClient.fetchQuoteSummary(
                symbol,
                listOf("assetProfile", "summaryProfile", "quoteType", "defaultKeyStatistics", "price")
            )
            parseCompanyInfo(response, symbol)
        }
    }

    override suspend fun getCompanyInfo(symbols: List<String>): Map<String, CompanyInfo> {
        if (symbols.isEmpty()) return emptyMap()
        require(symbols.size <= MAX_BATCH_SIZE) { "최대 $MAX_BATCH_SIZE 개의 심볼만 조회 가능합니다" }

        symbols.forEach { validateSymbol(it) }

        logger.debug("Fetching company info for {} symbols", symbols.size)

        // 병렬 처리
        val results = mutableMapOf<String, CompanyInfo>()
        val failed = mutableListOf<String>()

        coroutineScope {
            symbols.map { symbol ->
                async {
                    try {
                        symbol to getCompanyInfo(symbol)
                    } catch (e: Exception) {
                        logger.warn("Failed to fetch company info: symbol={}, error={}", symbol, e.message)
                        failed.add(symbol)
                        null
                    }
                }
            }
        }.mapNotNull { it.await() }.forEach { (symbol, info) ->
            results[symbol] = info
        }

        if (failed.isNotEmpty()) {
            logger.info("Partial success for batch company info: failed={}", failed)
        }

        return results
    }

    override suspend fun getFastInfo(symbol: String): FastInfo {
        validateSymbol(symbol)

        logger.debug("Fetching fast info: symbol={}", symbol)

        return cache.getOrPut("stock:fast:$symbol", ttl = FAST_INFO_TTL) {
            val response = httpClient.fetchQuoteSummary(symbol, listOf("quoteType", "price"))
            parseFastInfo(response, symbol)
        }
    }

    override suspend fun getFastInfo(symbols: List<String>): Map<String, FastInfo> {
        if (symbols.isEmpty()) return emptyMap()
        require(symbols.size <= MAX_BATCH_SIZE) { "최대 $MAX_BATCH_SIZE 개의 심볼만 조회 가능합니다" }

        symbols.forEach { validateSymbol(it) }

        logger.debug("Fetching fast info for {} symbols", symbols.size)

        val results = mutableMapOf<String, FastInfo>()
        val failed = mutableListOf<String>()

        coroutineScope {
            symbols.map { symbol ->
                async {
                    try {
                        symbol to getFastInfo(symbol)
                    } catch (e: Exception) {
                        logger.warn("Failed to fetch fast info: symbol={}, error={}", symbol, e.message)
                        failed.add(symbol)
                        null
                    }
                }
            }
        }.mapNotNull { it.await() }.forEach { (symbol, info) ->
            results[symbol] = info
        }

        if (failed.isNotEmpty()) {
            logger.info("Partial success for batch fast info: failed={}", failed)
        }

        return results
    }

    override suspend fun getIsin(symbol: String): String {
        validateSymbol(symbol)

        logger.debug("Fetching ISIN: symbol={}", symbol)

        return cache.getOrPut("stock:isin:$symbol", ttl = ISIN_TTL) {
            val response = httpClient.fetchQuoteSummary(symbol, listOf("defaultKeyStatistics"))
            parseIsin(response, symbol)
        }
    }

    override suspend fun getIsin(symbols: List<String>): Map<String, String> {
        if (symbols.isEmpty()) return emptyMap()
        require(symbols.size <= MAX_BATCH_SIZE) { "최대 $MAX_BATCH_SIZE 개의 심볼만 조회 가능합니다" }

        symbols.forEach { validateSymbol(it) }

        logger.debug("Fetching ISIN for {} symbols", symbols.size)

        val results = mutableMapOf<String, String>()
        val failed = mutableListOf<String>()

        coroutineScope {
            symbols.map { symbol ->
                async {
                    try {
                        symbol to getIsin(symbol)
                    } catch (e: Exception) {
                        logger.warn("Failed to fetch ISIN: symbol={}, error={}", symbol, e.message)
                        failed.add(symbol)
                        null
                    }
                }
            }
        }.mapNotNull { it.await() }.forEach { (symbol, isin) ->
            results[symbol] = isin
        }

        if (failed.isNotEmpty()) {
            logger.info("Partial success for batch ISIN: failed={}", failed)
        }

        return results
    }

    override suspend fun getShares(symbol: String): List<SharesData> {
        validateSymbol(symbol)

        logger.debug("Fetching shares history: symbol={}", symbol)

        return cache.getOrPut("stock:shares:$symbol", ttl = SHARES_TTL) {
            val response = httpClient.fetchQuoteSummary(symbol, listOf("defaultKeyStatistics"))
            parseShares(response)
        }
    }

    override suspend fun getShares(symbols: List<String>): Map<String, List<SharesData>> {
        if (symbols.isEmpty()) return emptyMap()
        require(symbols.size <= MAX_BATCH_SIZE) { "최대 $MAX_BATCH_SIZE 개의 심볼만 조회 가능합니다" }

        symbols.forEach { validateSymbol(it) }

        logger.debug("Fetching shares for {} symbols", symbols.size)

        val results = mutableMapOf<String, List<SharesData>>()
        val failed = mutableListOf<String>()

        coroutineScope {
            symbols.map { symbol ->
                async {
                    try {
                        val shares = getShares(symbol)
                        if (shares.isNotEmpty()) {
                            symbol to shares
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        logger.warn("Failed to fetch shares: symbol={}, error={}", symbol, e.message)
                        failed.add(symbol)
                        null
                    }
                }
            }
        }.mapNotNull { it.await() }.forEach { (symbol, shares) ->
            results[symbol] = shares
        }

        if (failed.isNotEmpty()) {
            logger.info("Partial success for batch shares: failed={}", failed)
        }

        return results
    }

    override suspend fun getSharesFull(
        symbol: String,
        start: LocalDate?,
        end: LocalDate?
    ): List<SharesData> {
        // 현재는 getShares와 동일하게 동작 (Yahoo API의 제약)
        // 향후 별도의 엔드포인트가 필요하면 확장 가능
        return getShares(symbol)
    }

    override suspend fun getRawQuoteSummary(
        symbol: String,
        modules: List<String>
    ): QuoteSummaryResponse {
        validateSymbol(symbol)
        return httpClient.fetchQuoteSummary(symbol, modules)
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

        if (symbol.length > 20) {
            throw UfcException(ErrorCode.INVALID_SYMBOL, "심볼이 너무 깁니다: $symbol (최대 20자)")
        }

        // 유효한 문자: 영문, 숫자, ^, ., -, _
        val validPattern = Regex("^[A-Za-z0-9^.\\-_]+$")
        if (!validPattern.matches(symbol)) {
            throw UfcException(ErrorCode.INVALID_SYMBOL, "유효하지 않은 심볼: $symbol")
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
     * CompanyInfo 파싱
     */
    private fun parseCompanyInfo(response: QuoteSummaryResponse, symbol: String): CompanyInfo {
        val result = response.quoteSummary.result?.firstOrNull()
            ?: throw UfcException(
                errorCode = ErrorCode.STOCK_DATA_NOT_FOUND,
                message = "주식 정보를 찾을 수 없습니다: $symbol"
            )

        val assetProfile = result.assetProfile
        val summaryProfile = result.summaryProfile
        val quoteType = result.quoteType
        val defaultKeyStats = result.defaultKeyStatistics
        val price = result.price

        // 필드 추출
        val longName = quoteType?.longName
            ?: price?.longName
            ?: throw UfcException(
                errorCode = ErrorCode.STOCK_DATA_NOT_FOUND,
                message = "회사명을 찾을 수 없습니다: $symbol"
            )

        // 데이터 완전성 계산
        val completeness = calculateDataCompleteness(
            assetProfile != null,
            summaryProfile != null,
            quoteType != null,
            defaultKeyStats != null
        )

        return CompanyInfo(
            symbol = symbol,
            longName = longName,
            shortName = quoteType?.shortName ?: price?.shortName,
            sector = assetProfile?.sector,
            industry = assetProfile?.industry,
            country = assetProfile?.country,
            exchange = quoteType?.exchange,
            currency = price?.currency ?: quoteType?.market,
            quoteType = quoteType?.quoteType?.let { AssetType.fromString(it) },
            website = assetProfile?.website,
            phone = assetProfile?.phone,
            address = assetProfile?.address1,
            city = assetProfile?.city,
            state = assetProfile?.state,
            zipCode = assetProfile?.zip,
            employees = assetProfile?.fullTimeEmployees?.toLong(),
            description = assetProfile?.longBusinessSummary,
            sharesOutstanding = defaultKeyStats?.sharesOutstanding?.longValue,
            metadata = CompanyInfoMetadata(
                symbol = symbol,
                fetchedAt = System.currentTimeMillis(),
                source = "YahooFinance",
                modulesUsed = listOf("assetProfile", "summaryProfile", "quoteType", "defaultKeyStatistics", "price"),
                dataCompleteness = completeness
            )
        )
    }

    /**
     * FastInfo 파싱
     */
    private fun parseFastInfo(response: QuoteSummaryResponse, symbol: String): FastInfo {
        val result = response.quoteSummary.result?.firstOrNull()
            ?: throw UfcException(
                errorCode = ErrorCode.STOCK_DATA_NOT_FOUND,
                message = "빠른 정보를 찾을 수 없습니다: $symbol"
            )

        val quoteType = result.quoteType
            ?: throw UfcException(
                errorCode = ErrorCode.STOCK_DATA_NOT_FOUND,
                message = "자산 정보를 찾을 수 없습니다: $symbol"
            )

        val price = result.price

        return FastInfo(
            symbol = symbol,
            currency = price?.currency ?: quoteType.market
                ?: throw UfcException(
                    errorCode = ErrorCode.STOCK_DATA_NOT_FOUND,
                    message = "통화 정보를 찾을 수 없습니다: $symbol"
                ),
            exchange = quoteType.exchange
                ?: throw UfcException(
                    errorCode = ErrorCode.STOCK_DATA_NOT_FOUND,
                    message = "거래소 정보를 찾을 수 없습니다: $symbol"
                ),
            quoteType = AssetType.fromString(quoteType.quoteType)
        )
    }

    /**
     * ISIN 파싱
     */
    private fun parseIsin(response: QuoteSummaryResponse, symbol: String): String {
        val result = response.quoteSummary.result?.firstOrNull()
            ?: throw UfcException(
                errorCode = ErrorCode.ISIN_NOT_FOUND,
                message = "ISIN 정보를 찾을 수 없습니다: $symbol"
            )

        val isin = result.defaultKeyStatistics?.isin
            ?: throw UfcException(
                errorCode = ErrorCode.ISIN_NOT_FOUND,
                message = "ISIN 데이터가 없습니다: $symbol"
            )

        return isin
    }

    /**
     * Shares 파싱
     */
    private fun parseShares(response: QuoteSummaryResponse): List<SharesData> {
        val result = response.quoteSummary.result?.firstOrNull()
            ?: return emptyList()

        val defaultKeyStats = result.defaultKeyStatistics
            ?: return emptyList()

        val shares = defaultKeyStats.sharesOutstanding?.longValue
            ?: return emptyList()

        // 현재는 최신 발행주식수 하나만 반환
        // 향후 Yahoo API에서 히스토리 정보가 제공되면 확장 가능
        return listOf(
            SharesData(
                date = LocalDate.now(),
                shares = shares
            )
        )
    }

    /**
     * 데이터 완전성 계산
     */
    private fun calculateDataCompleteness(
        hasAssetProfile: Boolean,
        hasSummaryProfile: Boolean,
        hasQuoteType: Boolean,
        hasDefaultKeyStats: Boolean
    ): DataCompleteness {
        val totalFields = 20  // CompanyInfo의 주요 필드 수
        val populatedModules = listOfNotNull(
            "assetProfile".takeIf { hasAssetProfile },
            "summaryProfile".takeIf { hasSummaryProfile },
            "quoteType".takeIf { hasQuoteType },
            "defaultKeyStatistics".takeIf { hasDefaultKeyStats }
        ).size

        val populatedFields = (populatedModules * 5)  // 모듈당 평균 5개 필드
        val completenessPercent = if (totalFields > 0) {
            (populatedFields.toDouble() / totalFields) * 100
        } else {
            0.0
        }

        return DataCompleteness(
            totalFields = totalFields,
            populatedFields = populatedFields,
            completenessPercent = completenessPercent
        )
    }
}
