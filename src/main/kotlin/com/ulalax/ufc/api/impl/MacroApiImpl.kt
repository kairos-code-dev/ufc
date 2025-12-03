package com.ulalax.ufc.api.impl

import com.ulalax.ufc.api.MacroApi
import com.ulalax.ufc.domain.macro.*

/**
 * MacroApi의 내부 구현체
 *
 * MacroService에 모든 작업을 위임하는 어댑터 역할을 합니다.
 */
internal class MacroApiImpl(
    private val macroService: MacroService
) : MacroApi {

    override suspend fun getSeries(
        seriesId: String,
        startDate: String?,
        endDate: String?,
        frequency: String?,
        units: String?
    ): MacroSeries {
        return macroService.getSeries(seriesId, startDate, endDate, frequency, units)
    }

    override suspend fun getGDP(
        type: GDPType,
        startDate: String?,
        endDate: String?
    ): MacroSeries {
        return macroService.getGDP(type, startDate, endDate)
    }

    override suspend fun getInflation(
        type: InflationType,
        startDate: String?,
        endDate: String?
    ): MacroSeries {
        return macroService.getInflation(type, startDate, endDate)
    }

    override suspend fun getUnemployment(
        type: UnemploymentType,
        startDate: String?,
        endDate: String?
    ): MacroSeries {
        return macroService.getUnemployment(type, startDate, endDate)
    }

    override suspend fun getInterestRate(
        type: InterestRateType,
        startDate: String?,
        endDate: String?
    ): MacroSeries {
        return macroService.getInterestRate(type, startDate, endDate)
    }
}
