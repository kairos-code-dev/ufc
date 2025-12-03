package com.ulalax.ufc.api.impl

import com.ulalax.ufc.api.StockApi
import com.ulalax.ufc.domain.quote.QuoteSummaryResponse
import com.ulalax.ufc.domain.stock.CompanyInfo
import com.ulalax.ufc.domain.stock.FastInfo
import com.ulalax.ufc.domain.stock.SharesData
import com.ulalax.ufc.domain.stock.StockService
import java.time.LocalDate

/**
 * StockApi의 내부 구현체
 *
 * StockService에 모든 작업을 위임하는 어댑터 역할을 합니다.
 */
internal class StockApiImpl(
    private val stockService: StockService
) : StockApi {

    override suspend fun getCompanyInfo(symbol: String): CompanyInfo {
        return stockService.getCompanyInfo(symbol)
    }

    override suspend fun getCompanyInfo(symbols: List<String>): Map<String, CompanyInfo> {
        return stockService.getCompanyInfo(symbols)
    }

    override suspend fun getFastInfo(symbol: String): FastInfo {
        return stockService.getFastInfo(symbol)
    }

    override suspend fun getFastInfo(symbols: List<String>): Map<String, FastInfo> {
        return stockService.getFastInfo(symbols)
    }

    override suspend fun getIsin(symbol: String): String {
        return stockService.getIsin(symbol)
    }

    override suspend fun getIsin(symbols: List<String>): Map<String, String> {
        return stockService.getIsin(symbols)
    }

    override suspend fun getShares(symbol: String): List<SharesData> {
        return stockService.getShares(symbol)
    }

    override suspend fun getShares(symbols: List<String>): Map<String, List<SharesData>> {
        return stockService.getShares(symbols)
    }

    override suspend fun getSharesFull(
        symbol: String,
        start: LocalDate?,
        end: LocalDate?
    ): List<SharesData> {
        return stockService.getSharesFull(symbol, start, end)
    }

    override suspend fun getRawQuoteSummary(
        symbol: String,
        modules: List<String>
    ): QuoteSummaryResponse {
        return stockService.getRawQuoteSummary(symbol, modules)
    }
}
