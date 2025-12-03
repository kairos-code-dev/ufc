package com.ulalax.ufc.api.impl

import com.ulalax.ufc.api.FundsApi
import com.ulalax.ufc.domain.funds.FundData
import com.ulalax.ufc.domain.funds.FundsService
import com.ulalax.ufc.infrastructure.yahoo.response.FundDataResponse

/**
 * FundsApi의 내부 구현체
 *
 * FundsService에 모든 작업을 위임하는 어댑터 역할을 합니다.
 */
internal class FundsApiImpl(
    private val fundsService: FundsService
) : FundsApi {

    override suspend fun getFundData(symbol: String): FundData {
        return fundsService.getFundData(symbol)
    }

    override suspend fun getFundData(symbols: List<String>): Map<String, FundData> {
        return fundsService.getFundData(symbols)
    }

    override suspend fun getRawFundData(symbol: String): FundDataResponse {
        return fundsService.getRawFundData(symbol)
    }

    override suspend fun isFund(symbol: String): Boolean {
        return fundsService.isFund(symbol)
    }
}
