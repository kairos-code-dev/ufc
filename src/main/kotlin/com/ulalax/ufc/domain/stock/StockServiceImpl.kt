package com.ulalax.ufc.domain.stock

import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.domain.quote.QuoteSummaryResponse
import com.ulalax.ufc.infrastructure.util.CacheHelper
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.time.LocalDate
import kotlin.time.Duration.Companion.hours

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

    /**
     * ISIN 코드 조회
     *
     * yfinance와 동일한 방식으로 Business Insider Search API를 사용하여 ISIN을 조회합니다.
     *
     * @param symbol 주식 심볼
     * @return ISIN 코드 (12자리)
     * @throws UfcException ISIN 데이터를 찾을 수 없음
     */
    override suspend fun getIsin(symbol: String): String {
        validateSymbol(symbol)

        logger.debug("Fetching ISIN: symbol={}", symbol)

        return cache.getOrPut("stock:isin:$symbol", ttl = ISIN_TTL) {
            parseIsinFromBusinessInsider(symbol)
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
        validateSymbol(symbol)

        logger.debug("Fetching shares full history: symbol={}, start={}, end={}", symbol, start, end)

        // LocalDate를 Unix timestamp로 변환
        val period1 = start?.atStartOfDay(java.time.ZoneId.of("America/New_York"))?.toEpochSecond()
        val period2 = end?.atTime(23, 59, 59)?.atZone(java.time.ZoneId.of("America/New_York"))?.toEpochSecond()

        // Fundamentals Timeseries API 호출
        val response = httpClient.fetchFundamentalsTimeseries(symbol, period1, period2)
        return parseSharesFull(response, start, end)
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
     * Business Insider Search API를 사용하여 ISIN 파싱
     *
     * yfinance의 get_isin() 메서드와 동일한 구현입니다.
     *
     * 알고리즘:
     * 1. 심볼에 "-" 또는 "^"가 포함되어 있으면 ISIN을 찾을 수 없음
     * 2. 회사의 shortName을 조회하여 검색 쿼리로 사용 (없으면 심볼 사용)
     * 3. Business Insider Search API 호출
     * 4. 응답에서 "{SYMBOL}|" 패턴 검색하여 ISIN 추출
     *
     * 참고: https://github.com/ranaroussi/yfinance/blob/main/yfinance/base.py#L1062
     */
    private suspend fun parseIsinFromBusinessInsider(symbol: String): String {
        val ticker = symbol.uppercase()

        // 1. 특수 문자가 포함된 심볼은 ISIN을 찾을 수 없음
        if (ticker.contains("-") || ticker.contains("^")) {
            throw UfcException(
                errorCode = ErrorCode.ISIN_NOT_FOUND,
                message = "ISIN을 찾을 수 없습니다: $symbol (특수 문자가 포함된 심볼)"
            )
        }

        // 2. 검색 쿼리 결정 (shortName 또는 심볼)
        var query = ticker
        try {
            // QuoteSummary에서 shortName 조회 시도
            val quoteSummary = httpClient.fetchQuoteSummary(ticker, listOf("price", "quoteType"))
            val shortName = quoteSummary.quoteSummary.result?.firstOrNull()?.price?.shortName
                ?: quoteSummary.quoteSummary.result?.firstOrNull()?.quoteType?.shortName

            if (shortName != null) {
                query = shortName
            }
        } catch (e: Exception) {
            logger.debug("Failed to fetch shortName for ISIN search, using ticker: {}", e.message)
            // shortName 조회 실패 시 ticker 사용
        }

        // 3. Business Insider Search API 호출 및 ISIN 추출
        // 먼저 query(shortName)로 시도, 실패하면 ticker로 재시도
        val searchResponse = httpClient.fetchBusinessInsiderSearch(query)
        var searchStr = "\"$ticker|"

        val finalSearchResponse = if (searchResponse.contains(searchStr)) {
            searchResponse
        } else if (query != ticker) {
            // shortName으로 검색 실패 시 ticker로 재검색
            logger.debug("ISIN search with shortName failed, retrying with ticker: {}", ticker)
            val tickerResponse = httpClient.fetchBusinessInsiderSearch(ticker)
            if (tickerResponse.contains(searchStr)) {
                tickerResponse
            } else {
                searchResponse // 원래 응답 유지
            }
        } else {
            searchResponse
        }

        // 4. ISIN 추출
        if (!finalSearchResponse.contains(searchStr)) {
            // ticker로 찾지 못한 경우, 일반 패턴으로 재검색
            if (query.lowercase() in finalSearchResponse.lowercase()) {
                searchStr = "\"|"
                if (!finalSearchResponse.contains(searchStr)) {
                    throw UfcException(
                        errorCode = ErrorCode.ISIN_NOT_FOUND,
                        message = "ISIN을 찾을 수 없습니다: $symbol (Business Insider 응답에서 ISIN 추출 실패)"
                    )
                }
            } else {
                throw UfcException(
                    errorCode = ErrorCode.ISIN_NOT_FOUND,
                    message = "ISIN을 찾을 수 없습니다: $symbol (Business Insider 검색 결과 없음)"
                )
            }
        }

        // 문자열 파싱: "TICKER|ISIN|..." 형태에서 ISIN 추출
        return try {
            finalSearchResponse.split(searchStr)[1]
                .split("\"")[0]
                .split("|")[0]
        } catch (e: Exception) {
            logger.error("Failed to parse ISIN from Business Insider response: {}", e.message)
            throw UfcException(
                errorCode = ErrorCode.ISIN_NOT_FOUND,
                message = "ISIN 파싱 실패: $symbol (Business Insider 응답 형식 오류)",
                cause = e
            )
        }
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
     * Shares Full 파싱 (Fundamentals Timeseries API 응답)
     *
     * yfinance의 get_shares_full(start, end) 메서드와 동일한 방식으로 파싱합니다.
     */
    private fun parseSharesFull(
        response: com.ulalax.ufc.domain.fundamentals.FundamentalsTimeseriesResponse,
        start: LocalDate?,
        end: LocalDate?
    ): List<SharesData> {
        val result = response.timeseries.result?.firstOrNull()
            ?: return emptyList()

        val timestamps = result.timestamp ?: return emptyList()
        val sharesOut = result.sharesOut ?: return emptyList()

        // 두 배열의 길이가 다르면 에러
        if (timestamps.size != sharesOut.size) {
            logger.warn(
                "Timestamp and shares_out array length mismatch: timestamps={}, shares={}",
                timestamps.size, sharesOut.size
            )
            return emptyList()
        }

        // timestamp와 shares_out을 pair로 결합하여 파싱
        val sharesList = timestamps.zip(sharesOut).mapNotNull { (timestamp, shares) ->
            try {
                // Unix timestamp를 LocalDate로 변환 (America/New_York 기준)
                val instant = java.time.Instant.ofEpochSecond(timestamp)
                val date = instant.atZone(java.time.ZoneId.of("America/New_York")).toLocalDate()
                SharesData(date = date, shares = shares)
            } catch (e: Exception) {
                logger.warn("Failed to parse share data: timestamp={}, error={}", timestamp, e.message)
                null
            }
        }

        // 날짜 필터링 (클라이언트 측)
        val filtered = sharesList.filter { shareData ->
            val afterStart = start == null || !shareData.date.isBefore(start)
            val beforeEnd = end == null || !shareData.date.isAfter(end)
            afterStart && beforeEnd
        }

        // 중복 날짜 제거 (같은 날짜는 가장 최근 값 유지)
        val deduplicated = filtered
            .groupBy { it.date }
            .mapValues { (_, values) -> values.last() }
            .values
            .toList()

        // 날짜 오름차순 정렬
        return deduplicated.sortedBy { it.date }
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
