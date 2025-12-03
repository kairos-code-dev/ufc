package com.ulalax.ufc.api.impl

import com.ulalax.ufc.api.CorpApi
import com.ulalax.ufc.domain.corp.CorpService
import com.ulalax.ufc.domain.corp.DividendHistory
import com.ulalax.ufc.domain.corp.SplitHistory
import com.ulalax.ufc.domain.corp.CapitalGainHistory
import com.ulalax.ufc.domain.common.Period

/**
 * CorpApi의 내부 구현체
 *
 * CorpService에 모든 작업을 위임하는 어댑터 역할을 합니다.
 */
internal class CorpApiImpl(
    private val corpService: CorpService
) : CorpApi {

    override suspend fun getDividends(symbol: String, period: Period): DividendHistory {
        return corpService.getDividends(symbol, period)
    }

    override suspend fun getSplits(symbol: String, period: Period): SplitHistory {
        return corpService.getSplits(symbol, period)
    }

    override suspend fun getCapitalGains(symbol: String, period: Period): CapitalGainHistory {
        return corpService.getCapitalGains(symbol, period)
    }
}
