package com.ulalax.ufc.domain.quote

import com.ulalax.ufc.exception.ApiException
import com.ulalax.ufc.exception.DataParsingException
import com.ulalax.ufc.exception.ErrorCode
import com.ulalax.ufc.exception.UfcException
import com.ulalax.ufc.infrastructure.ratelimit.RateLimiter
import com.ulalax.ufc.internal.ResponseRecordingContext
import com.ulalax.ufc.internal.bodyAsTextWithRecording
import com.ulalax.ufc.internal.yahoo.YahooApiUrls
import com.ulalax.ufc.internal.yahoo.auth.AuthResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Yahoo Finance QuoteSummary API의 구현체
 *
 * 다음 엔드포인트를 사용합니다:
 * - GET /v10/finance/quoteSummary/{symbol}
 *   - 파라미터: modules, crumb
 *
 * Rate Limiting:
 * - TokenBucketRateLimiter 적용
 * - 각 요청마다 1개 토큰 소비
 *
 * 에러 처리:
 * - HTTP 오류: ApiException 발생
 * - JSON 파싱 오류: DataParsingException 발생
 * - 기타 예외: UfcException 발생
 *
 * @property httpClient Ktor HttpClient
 * @property rateLimiter Rate Limiting 제어
 * @property authResult Yahoo Finance 인증 정보 (CRUMB 토큰 포함)
 */
class YahooQuoteSummaryService(
    private val httpClient: HttpClient,
    private val rateLimiter: RateLimiter,
    private val authResult: AuthResult
) : QuoteSummaryService {

    companion object {
        private val logger = LoggerFactory.getLogger(YahooQuoteSummaryService::class.java)
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        // 기본 모듈 목록
        private val DEFAULT_MODULES = listOf(
            "price",
            "summaryDetail",
            "financialData",
            "earningsTrend",
            "earningsHistory",
            "earningsDates",
            "majorHolders",
            "insiderTransactions"
        )
    }

    /**
     * 단일 심볼의 전체 요약 정보를 조회합니다.
     */
    override suspend fun getQuoteSummary(symbol: String): QuoteSummaryResult {
        validateSymbol(symbol)

        logger.info(
            "Fetching quote summary: symbol={}",
            symbol
        )

        return try {
            val response = getRawQuoteSummary(symbol)
            val result = response.quoteSummary.result?.firstOrNull()
                ?: throw UfcException(
                    errorCode = ErrorCode.DATA_NOT_FOUND,
                    message = "요약 정보를 찾을 수 없습니다: $symbol"
                )

            logger.info("Quote summary fetched successfully: symbol={}", symbol)
            result

        } catch (e: UfcException) {
            logger.error("Failed to fetch quote summary: symbol={}", symbol, e)
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error while fetching quote summary", e)
            throw UfcException(
                errorCode = ErrorCode.DATA_RETRIEVAL_ERROR,
                message = "요약 정보 조회 중 오류 발생: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * 특정 모듈만 포함한 요약 정보를 조회합니다.
     */
    override suspend fun getQuoteSummary(
        symbol: String,
        modules: List<String>
    ): QuoteSummaryResult {
        validateSymbol(symbol)

        if (modules.isEmpty()) {
            throw UfcException(
                errorCode = ErrorCode.INVALID_PARAMETER,
                message = "포함할 모듈이 없습니다"
            )
        }

        return try {
            val response = getRawQuoteSummary(symbol, modules)
            val result = response.quoteSummary.result?.firstOrNull()
                ?: throw UfcException(
                    errorCode = ErrorCode.DATA_NOT_FOUND,
                    message = "요약 정보를 찾을 수 없습니다: $symbol"
                )

            logger.info(
                "Quote summary fetched successfully: symbol={}, modules={}",
                symbol, modules.size
            )
            result

        } catch (e: UfcException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error while fetching quote summary", e)
            throw UfcException(
                errorCode = ErrorCode.DATA_RETRIEVAL_ERROR,
                message = "요약 정보 조회 중 오류 발생: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * 다중 심볼의 요약 정보를 조회합니다.
     */
    override suspend fun getQuoteSummary(symbols: List<String>): Map<String, QuoteSummaryResult> {
        if (symbols.isEmpty()) {
            throw UfcException(
                errorCode = ErrorCode.INVALID_PARAMETER,
                message = "조회할 심볼 목록이 비어있습니다"
            )
        }

        logger.info("Fetching quote summary for multiple symbols: count={}", symbols.size)

        val result = mutableMapOf<String, QuoteSummaryResult>()

        for (symbol in symbols) {
            try {
                val data = getQuoteSummary(symbol)
                result[symbol] = data
            } catch (e: Exception) {
                logger.warn("Failed to fetch quote summary for symbol: {}", symbol, e)
                throw e
            }
        }

        logger.info("Successfully fetched quote summary for {} symbols", symbols.size)
        return result
    }

    /**
     * 다중 심볼의 요약 정보를 특정 모듈만 포함하여 조회합니다.
     */
    override suspend fun getQuoteSummary(
        symbols: List<String>,
        modules: List<String>
    ): Map<String, QuoteSummaryResult> {
        if (symbols.isEmpty()) {
            throw UfcException(
                errorCode = ErrorCode.INVALID_PARAMETER,
                message = "조회할 심볼 목록이 비어있습니다"
            )
        }

        if (modules.isEmpty()) {
            throw UfcException(
                errorCode = ErrorCode.INVALID_PARAMETER,
                message = "포함할 모듈이 없습니다"
            )
        }

        logger.info(
            "Fetching quote summary for multiple symbols: count={}, modules={}",
            symbols.size, modules.size
        )

        val result = mutableMapOf<String, QuoteSummaryResult>()

        for (symbol in symbols) {
            try {
                val data = getQuoteSummary(symbol, modules)
                result[symbol] = data
            } catch (e: Exception) {
                logger.warn("Failed to fetch quote summary for symbol: {}", symbol, e)
                throw e
            }
        }

        logger.info("Successfully fetched quote summary for {} symbols", symbols.size)
        return result
    }

    /**
     * 정규화된 주식 요약 정보를 조회합니다.
     */
    override suspend fun getStockSummary(symbol: String): StockSummary {
        validateSymbol(symbol)

        logger.info("Fetching stock summary (normalized): symbol={}", symbol)

        return try {
            val result = getQuoteSummary(symbol)
            val normalized = result.toStockSummary(symbol)

            logger.info("Stock summary fetched successfully: symbol={}", symbol)
            normalized

        } catch (e: UfcException) {
            logger.error("Failed to fetch stock summary: symbol={}", symbol, e)
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error while fetching stock summary", e)
            throw UfcException(
                errorCode = ErrorCode.DATA_RETRIEVAL_ERROR,
                message = "주식 요약 정보 조회 중 오류 발생: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * 다중 심볼의 정규화된 요약 정보를 조회합니다.
     */
    override suspend fun getStockSummary(symbols: List<String>): Map<String, StockSummary> {
        if (symbols.isEmpty()) {
            throw UfcException(
                errorCode = ErrorCode.INVALID_PARAMETER,
                message = "조회할 심볼 목록이 비어있습니다"
            )
        }

        logger.info("Fetching stock summary for multiple symbols: count={}", symbols.size)

        val result = mutableMapOf<String, StockSummary>()

        for (symbol in symbols) {
            try {
                val data = getStockSummary(symbol)
                result[symbol] = data
            } catch (e: Exception) {
                logger.warn("Failed to fetch stock summary for symbol: {}", symbol, e)
                throw e
            }
        }

        logger.info("Successfully fetched stock summary for {} symbols", symbols.size)
        return result
    }

    /**
     * QuoteSummary API의 원본 응답을 반환합니다.
     */
    override suspend fun getRawQuoteSummary(
        symbol: String,
        modules: List<String>?
    ): QuoteSummaryResponse {
        validateSymbol(symbol)

        // Rate Limiting 토큰 획득
        rateLimiter.acquire()

        val modulesToUse = modules ?: DEFAULT_MODULES

        return try {
            logger.debug(
                "Calling Yahoo Finance QuoteSummary API: symbol={}, modules={}",
                symbol, modulesToUse.size
            )

            // 모듈을 쉼표로 구분된 문자열로 변환
            val modulesParam = modulesToUse.joinToString(",")

            // API 요청 수행
            val response = httpClient.get("${YahooApiUrls.QUOTE_SUMMARY}/$symbol") {
                parameter("modules", modulesParam)
                parameter("crumb", authResult.crumb)
            }

            // HTTP 상태 코드 확인
            if (!response.status.isSuccess()) {
                throw ApiException(
                    message = "QuoteSummary API 요청 실패: HTTP ${response.status.value}",
                    statusCode = response.status.value,
                    responseBody = try { response.body<String>() } catch (e: Exception) { null }
                )
            }

            // 응답 파싱 (자동 레코딩 지원)
            val responseBody = response.bodyAsTextWithRecording()
            logger.debug("QuoteSummary API response received: length={}", responseBody.length)

            try {
                val quoteResponse = json.decodeFromString<QuoteSummaryResponse>(responseBody)

                // 에러 응답 확인
                if (quoteResponse.quoteSummary.error != null) {
                    throw ApiException(
                        message = "QuoteSummary API 에러: ${quoteResponse.quoteSummary.error?.description ?: "Unknown error"}",
                        statusCode = 200
                    )
                }

                // 결과 확인
                if (quoteResponse.quoteSummary.result.isNullOrEmpty()) {
                    throw UfcException(
                        errorCode = ErrorCode.DATA_NOT_FOUND,
                        message = "요약 정보를 찾을 수 없습니다: $symbol"
                    )
                }

                quoteResponse
            } catch (e: Exception) {
                when (e) {
                    is UfcException, is ApiException -> throw e
                    else -> {
                        logger.error("JSON 파싱 실패. 응답 내용: {}", responseBody.take(500), e)
                        throw DataParsingException(
                            message = "QuoteSummary 응답 JSON 파싱 실패",
                            sourceData = responseBody.take(500),
                            cause = e
                        )
                    }
                }
            }

        } catch (e: UfcException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error while calling QuoteSummary API", e)
            throw UfcException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "QuoteSummary API 호출 중 오류 발생: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * 심볼 유효성을 검증합니다.
     */
    private fun validateSymbol(symbol: String) {
        if (symbol.isBlank()) {
            throw UfcException(
                errorCode = ErrorCode.INVALID_SYMBOL,
                message = "심볼이 비어있습니다"
            )
        }

        if (symbol.length > 10) {
            throw UfcException(
                errorCode = ErrorCode.INVALID_SYMBOL,
                message = "심볼이 너무 깁니다: $symbol"
            )
        }
    }
}

/**
 * QuoteSummaryResult를 StockSummary로 변환합니다.
 */
private fun QuoteSummaryResult.toStockSummary(symbol: String): StockSummary {
    return StockSummary(
        symbol = symbol,
        currency = price?.currency,
        exchange = price?.exchange,
        // 가격 정보
        currentPrice = price?.regularMarketPrice?.doubleValue ?: summaryDetail?.fiftyTwoWeekHigh?.doubleValue,
        previousClose = price?.regularMarketPreviousClose?.doubleValue,
        dayHigh = price?.regularMarketPrice?.doubleValue,
        dayLow = summaryDetail?.regularMarketDayLow?.doubleValue,
        fiftyTwoWeekHigh = summaryDetail?.fiftyTwoWeekHigh?.doubleValue ?: price?.fiftyTwoWeekHigh?.doubleValue,
        fiftyTwoWeekLow = summaryDetail?.fiftyTwoWeekLow?.doubleValue ?: price?.fiftyTwoWeekLow?.doubleValue,
        fiftyTwoWeekChange = null,
        fiftyTwoWeekChangePercent = price?.fiftyTwoWeekChangePercent?.doubleValue,
        // 거래 정보
        volume = summaryDetail?.regularMarketVolume?.longValue,
        averageVolume = summaryDetail?.averageVolume?.longValue,
        averageVolume10Days = summaryDetail?.averageVolume10days?.longValue,
        marketCap = summaryDetail?.marketCap?.longValue,
        sharesOutstanding = summaryDetail?.sharesOutstanding?.longValue,
        // 배당금 정보
        dividendRate = summaryDetail?.dividendRate?.doubleValue,
        dividendYield = summaryDetail?.dividendYield?.doubleValue,
        dividendDate = summaryDetail?.dividendDate?.longValue,
        exDividendDate = summaryDetail?.exDividendDate?.longValue,
        // 평가 지표
        trailingPE = summaryDetail?.trailingPE?.doubleValue,
        forwardPE = summaryDetail?.forwardPE?.doubleValue,
        priceToBook = summaryDetail?.priceToBook?.doubleValue,
        beta = summaryDetail?.beta?.doubleValue,
        debtToEquity = summaryDetail?.debtToEquity?.doubleValue,
        // 재무 정보
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
        // 분석가 정보
        recommendationKey = financialData?.recommendationKey,
        numberOfAnalysts = financialData?.numberOfAnalysts,
        targetPriceMean = financialData?.targetPriceMean?.doubleValue,
        targetPriceLow = financialData?.targetPriceLow?.doubleValue,
        targetPriceHigh = financialData?.targetPriceHigh?.doubleValue
    )
}
