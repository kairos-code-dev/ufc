package com.ulalax.ufc.api.impl

import com.ulalax.ufc.api.PriceApi
import com.ulalax.ufc.domain.price.PriceData
import com.ulalax.ufc.domain.price.OHLCV
import com.ulalax.ufc.domain.price.ChartMetadata
import com.ulalax.ufc.domain.price.PriceService
import com.ulalax.ufc.domain.common.Interval
import com.ulalax.ufc.domain.common.Period
import java.time.LocalDate

/**
 * PriceApi의 내부 구현체
 *
 * PriceService에 모든 작업을 위임하는 어댑터 역할을 합니다.
 */
internal class PriceApiImpl(
    private val priceService: PriceService
) : PriceApi {

    override suspend fun getCurrentPrice(symbol: String): PriceData {
        return priceService.getCurrentPrice(symbol)
    }

    override suspend fun getCurrentPrice(symbols: List<String>): Map<String, PriceData> {
        return priceService.getCurrentPrice(symbols)
    }

    override suspend fun getPriceHistory(
        symbol: String,
        period: Period,
        interval: Interval
    ): List<OHLCV> {
        return priceService.getPriceHistory(symbol, period, interval)
    }

    override suspend fun getPriceHistory(
        symbol: String,
        start: LocalDate,
        end: LocalDate,
        interval: Interval
    ): List<OHLCV> {
        return priceService.getPriceHistory(symbol, start, end, interval)
    }

    override suspend fun getPriceHistoryWithMetadata(
        symbol: String,
        period: Period,
        interval: Interval
    ): Pair<ChartMetadata, List<OHLCV>> {
        return priceService.getPriceHistoryWithMetadata(symbol, period, interval)
    }

    override suspend fun getHistoryMetadata(symbol: String): ChartMetadata {
        return priceService.getHistoryMetadata(symbol)
    }
}
