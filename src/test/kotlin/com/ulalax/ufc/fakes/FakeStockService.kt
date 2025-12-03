package com.ulalax.ufc.fakes

import com.ulalax.ufc.domain.quote.QuoteSummaryResponse
import com.ulalax.ufc.domain.stock.*
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

/**
 * Stock 서비스의 Fake 구현체.
 *
 * 테스트를 위해 메모리 기반으로 동작합니다.
 * 테스트 코드에서 응답을 설정하고 동작을 제어할 수 있습니다.
 */
class FakeStockService : StockService {

    private val companyInfoResponses = ConcurrentHashMap<String, CompanyInfo>()
    private val fastInfoResponses = ConcurrentHashMap<String, FastInfo>()
    private val isinResponses = ConcurrentHashMap<String, String>()
    private val sharesResponses = ConcurrentHashMap<String, List<SharesData>>()
    private val rawResponses = ConcurrentHashMap<String, QuoteSummaryResponse>()

    private var failingSymbols = setOf<String>()

    /**
     * CompanyInfo 응답을 설정합니다.
     */
    fun setCompanyInfo(symbol: String, info: CompanyInfo) {
        companyInfoResponses[symbol] = info
    }

    /**
     * FastInfo 응답을 설정합니다.
     */
    fun setFastInfo(symbol: String, info: FastInfo) {
        fastInfoResponses[symbol] = info
    }

    /**
     * ISIN 응답을 설정합니다.
     */
    fun setIsin(symbol: String, isin: String) {
        isinResponses[symbol] = isin
    }

    /**
     * Shares 응답을 설정합니다.
     */
    fun setShares(symbol: String, shares: List<SharesData>) {
        sharesResponses[symbol] = shares
    }

    /**
     * Raw QuoteSummary 응답을 설정합니다.
     */
    fun setRawResponse(symbol: String, response: QuoteSummaryResponse) {
        rawResponses[symbol] = response
    }

    /**
     * 실패할 심볼 목록을 설정합니다.
     */
    fun setFailingSymbols(symbols: Set<String>) {
        failingSymbols = symbols
    }

    /**
     * 모든 응답을 초기화합니다.
     */
    fun reset() {
        companyInfoResponses.clear()
        fastInfoResponses.clear()
        isinResponses.clear()
        sharesResponses.clear()
        rawResponses.clear()
        failingSymbols = emptySet()
    }

    override suspend fun getCompanyInfo(symbol: String): CompanyInfo {
        if (symbol in failingSymbols) {
            throw UfcException(
                errorCode = ErrorCode.STOCK_DATA_NOT_FOUND,
                message = "주식 정보를 찾을 수 없습니다: $symbol"
            )
        }

        return companyInfoResponses[symbol]
            ?: throw UfcException(
                errorCode = ErrorCode.STOCK_DATA_NOT_FOUND,
                message = "주식 정보를 찾을 수 없습니다: $symbol"
            )
    }

    override suspend fun getCompanyInfo(symbols: List<String>): Map<String, CompanyInfo> {
        return symbols.mapNotNull { symbol ->
            try {
                symbol to getCompanyInfo(symbol)
            } catch (e: Exception) {
                null
            }
        }.toMap()
    }

    override suspend fun getFastInfo(symbol: String): FastInfo {
        if (symbol in failingSymbols) {
            throw UfcException(
                errorCode = ErrorCode.STOCK_DATA_NOT_FOUND,
                message = "빠른 정보를 찾을 수 없습니다: $symbol"
            )
        }

        return fastInfoResponses[symbol]
            ?: throw UfcException(
                errorCode = ErrorCode.STOCK_DATA_NOT_FOUND,
                message = "빠른 정보를 찾을 수 없습니다: $symbol"
            )
    }

    override suspend fun getFastInfo(symbols: List<String>): Map<String, FastInfo> {
        return symbols.mapNotNull { symbol ->
            try {
                symbol to getFastInfo(symbol)
            } catch (e: Exception) {
                null
            }
        }.toMap()
    }

    override suspend fun getIsin(symbol: String): String {
        if (symbol in failingSymbols) {
            throw UfcException(
                errorCode = ErrorCode.ISIN_NOT_FOUND,
                message = "ISIN 정보를 찾을 수 없습니다: $symbol"
            )
        }

        return isinResponses[symbol]
            ?: throw UfcException(
                errorCode = ErrorCode.ISIN_NOT_FOUND,
                message = "ISIN 정보를 찾을 수 없습니다: $symbol"
            )
    }

    override suspend fun getIsin(symbols: List<String>): Map<String, String> {
        return symbols.mapNotNull { symbol ->
            try {
                symbol to getIsin(symbol)
            } catch (e: Exception) {
                null
            }
        }.toMap()
    }

    override suspend fun getShares(symbol: String): List<SharesData> {
        if (symbol in failingSymbols) {
            throw UfcException(
                errorCode = ErrorCode.SHARES_DATA_NOT_FOUND,
                message = "발행주식수 정보를 찾을 수 없습니다: $symbol"
            )
        }

        return sharesResponses[symbol]
            ?: throw UfcException(
                errorCode = ErrorCode.SHARES_DATA_NOT_FOUND,
                message = "발행주식수 정보를 찾을 수 없습니다: $symbol"
            )
    }

    override suspend fun getShares(symbols: List<String>): Map<String, List<SharesData>> {
        return symbols.mapNotNull { symbol ->
            try {
                val shares = getShares(symbol)
                symbol to shares
            } catch (e: Exception) {
                null
            }
        }.toMap()
    }

    override suspend fun getSharesFull(
        symbol: String,
        start: LocalDate?,
        end: LocalDate?
    ): List<SharesData> {
        // getShares와 동일하게 동작
        return getShares(symbol)
    }

    override suspend fun getRawQuoteSummary(
        symbol: String,
        modules: List<String>
    ): QuoteSummaryResponse {
        if (symbol in failingSymbols) {
            throw UfcException(
                errorCode = ErrorCode.STOCK_DATA_NOT_FOUND,
                message = "주식 정보를 찾을 수 없습니다: $symbol"
            )
        }

        return rawResponses[symbol]
            ?: throw UfcException(
                errorCode = ErrorCode.STOCK_DATA_NOT_FOUND,
                message = "주식 정보를 찾을 수 없습니다: $symbol"
            )
    }
}
