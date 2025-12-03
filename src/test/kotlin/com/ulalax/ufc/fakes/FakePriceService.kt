package com.ulalax.ufc.fakes

import com.ulalax.ufc.domain.price.ChartMetadata
import com.ulalax.ufc.domain.price.OHLCV
import com.ulalax.ufc.domain.price.PriceData
import com.ulalax.ufc.domain.price.PriceService
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.infrastructure.yahoo.response.ChartResponse
import com.ulalax.ufc.infrastructure.yahoo.response.PriceResponse
import com.ulalax.ufc.domain.common.Interval
import com.ulalax.ufc.domain.common.Period
import java.time.LocalDate

/**
 * PriceService의 테스트 Fake 구현체
 *
 * 테스트 목적으로 사용되는 Mock 객체입니다.
 */
class FakePriceService : PriceService {
    private val priceDataMap = mutableMapOf<String, PriceData>()
    private val ohlcvDataMap = mutableMapOf<Pair<String, Period>, List<OHLCV>>()
    private val metadataMap = mutableMapOf<String, ChartMetadata>()
    private val rawPriceResponses = mutableMapOf<String, PriceResponse>()
    private val rawChartResponses = mutableMapOf<Triple<String, Period, Interval>, ChartResponse>()

    private var shouldThrowException = false
    private var exceptionToThrow: UfcException? = null

    // ============================================================================
    // Setup Methods for Testing
    // ============================================================================

    /**
     * 심볼에 대한 가격 데이터 설정
     */
    fun setPriceData(symbol: String, priceData: PriceData) {
        priceDataMap[symbol] = priceData
    }

    /**
     * 심볼과 Period에 대한 OHLCV 데이터 설정
     */
    fun setOHLCVData(symbol: String, period: Period, data: List<OHLCV>) {
        ohlcvDataMap[symbol to period] = data
    }

    /**
     * 심볼에 대한 메타데이터 설정
     */
    fun setMetadata(symbol: String, metadata: ChartMetadata) {
        metadataMap[symbol] = metadata
    }

    /**
     * Raw 가격 응답 설정
     */
    fun setRawPriceResponse(symbol: String, response: PriceResponse) {
        rawPriceResponses[symbol] = response
    }

    /**
     * Raw 차트 응답 설정
     */
    fun setRawChartResponse(symbol: String, period: Period, interval: Interval, response: ChartResponse) {
        rawChartResponses[Triple(symbol, period, interval)] = response
    }

    /**
     * 예외 발생 설정
     */
    fun setShouldThrowException(exception: UfcException) {
        shouldThrowException = true
        exceptionToThrow = exception
    }

    /**
     * 예외 발생 초기화
     */
    fun clearException() {
        shouldThrowException = false
        exceptionToThrow = null
    }

    /**
     * 모든 데이터 초기화
     */
    fun clear() {
        priceDataMap.clear()
        ohlcvDataMap.clear()
        metadataMap.clear()
        rawPriceResponses.clear()
        rawChartResponses.clear()
        clearException()
    }

    // ============================================================================
    // PriceService Implementation
    // ============================================================================

    override suspend fun getCurrentPrice(symbol: String): PriceData {
        if (shouldThrowException && exceptionToThrow != null) {
            throw exceptionToThrow!!
        }

        return priceDataMap[symbol]
            ?: throw UfcException(ErrorCode.PRICE_DATA_NOT_FOUND, "심볼 '$symbol'의 가격 데이터를 찾을 수 없습니다.")
    }

    override suspend fun getCurrentPrice(symbols: List<String>): Map<String, PriceData> {
        if (shouldThrowException && exceptionToThrow != null) {
            throw exceptionToThrow!!
        }

        if (symbols.isEmpty()) {
            throw UfcException(ErrorCode.INVALID_PARAMETER, "심볼 목록이 비어있습니다.")
        }

        return symbols
            .mapNotNull { symbol ->
                priceDataMap[symbol]?.let { symbol to it }
            }
            .toMap()
    }

    override suspend fun getPriceHistory(
        symbol: String,
        period: Period,
        interval: Interval
    ): List<OHLCV> {
        if (shouldThrowException && exceptionToThrow != null) {
            throw exceptionToThrow!!
        }

        return ohlcvDataMap[symbol to period]
            ?: emptyList()
    }

    override suspend fun getPriceHistory(
        symbol: String,
        start: LocalDate,
        end: LocalDate,
        interval: Interval
    ): List<OHLCV> {
        if (shouldThrowException && exceptionToThrow != null) {
            throw exceptionToThrow!!
        }

        // 근사 Period로 데이터 조회
        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt()
        val approximatePeriod = when {
            daysDiff <= 1 -> Period.OneDay
            daysDiff <= 5 -> Period.FiveDays
            daysDiff <= 30 -> Period.OneMonth
            daysDiff <= 90 -> Period.ThreeMonths
            daysDiff <= 180 -> Period.SixMonths
            daysDiff <= 365 -> Period.OneYear
            daysDiff <= 730 -> Period.TwoYears
            daysDiff <= 1825 -> Period.FiveYears
            else -> Period.Max
        }

        val data = ohlcvDataMap[symbol to approximatePeriod] ?: return emptyList()

        // 날짜 범위로 필터링
        val startTimestamp = start.atStartOfDay(java.time.ZoneOffset.UTC).toEpochSecond()
        val endTimestamp = end.atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC).epochSecond

        return data.filter { it.timestamp in startTimestamp..endTimestamp }
    }

    override suspend fun getPriceHistoryWithMetadata(
        symbol: String,
        period: Period,
        interval: Interval
    ): Pair<ChartMetadata, List<OHLCV>> {
        if (shouldThrowException && exceptionToThrow != null) {
            throw exceptionToThrow!!
        }

        val metadata = metadataMap[symbol]
            ?: throw UfcException(ErrorCode.PRICE_DATA_NOT_FOUND, "심볼 '$symbol'의 메타데이터를 찾을 수 없습니다.")

        val ohlcv = ohlcvDataMap[symbol to period] ?: emptyList()

        return metadata to ohlcv
    }

    override suspend fun getHistoryMetadata(symbol: String): ChartMetadata {
        if (shouldThrowException && exceptionToThrow != null) {
            throw exceptionToThrow!!
        }

        return metadataMap[symbol]
            ?: throw UfcException(ErrorCode.PRICE_DATA_NOT_FOUND, "심볼 '$symbol'의 메타데이터를 찾을 수 없습니다.")
    }

    override suspend fun getRawPrice(symbol: String): PriceResponse {
        if (shouldThrowException && exceptionToThrow != null) {
            throw exceptionToThrow!!
        }

        return rawPriceResponses[symbol]
            ?: throw UfcException(ErrorCode.PRICE_DATA_NOT_FOUND, "심볼 '$symbol'의 원본 응답을 찾을 수 없습니다.")
    }

    override suspend fun getRawPriceHistory(
        symbol: String,
        period: Period,
        interval: Interval
    ): ChartResponse {
        if (shouldThrowException && exceptionToThrow != null) {
            throw exceptionToThrow!!
        }

        return rawChartResponses[Triple(symbol, period, interval)]
            ?: throw UfcException(ErrorCode.PRICE_DATA_NOT_FOUND, "심볼 '$symbol'의 차트 응답을 찾을 수 없습니다.")
    }
}
