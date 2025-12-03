package com.ulalax.ufc.domain.price

import com.ulalax.ufc.infrastructure.yahoo.response.ChartResponse
import com.ulalax.ufc.infrastructure.yahoo.response.PriceResponse
import com.ulalax.ufc.domain.common.Interval
import com.ulalax.ufc.domain.common.Period
import java.time.LocalDate

/**
 * 금융 상품의 가격 정보를 조회하는 서비스 인터페이스
 *
 * Yahoo Finance API를 통해 현재 가격 및 히스토리 데이터를 제공합니다.
 */
interface PriceService {
    /**
     * 현재 가격 정보 조회
     *
     * @param symbol 심볼 (예: AAPL, SPY, ^GSPC)
     * @return 현재 가격 정보
     * @throws UfcException PRICE_DATA_NOT_FOUND, AUTH_FAILED 등
     */
    suspend fun getCurrentPrice(symbol: String): PriceData

    /**
     * 다중 심볼의 현재 가격 조회
     *
     * @param symbols 심볼 목록
     * @return 심볼별 가격 정보 Map
     * @throws UfcException
     */
    suspend fun getCurrentPrice(symbols: List<String>): Map<String, PriceData>

    /**
     * 가격 히스토리 조회 (기간 기반)
     *
     * @param symbol 심볼
     * @param period 조회 기간 (예: Period.OneYear)
     * @param interval 데이터 간격 (기본값: Interval.OneDay)
     * @return OHLCV 시계열 데이터
     * @throws UfcException INVALID_PERIOD_INTERVAL 등
     */
    suspend fun getPriceHistory(
        symbol: String,
        period: Period,
        interval: Interval = Interval.OneDay
    ): List<OHLCV>

    /**
     * 가격 히스토리 조회 (날짜 범위 기반)
     *
     * @param symbol 심볼
     * @param start 시작 날짜
     * @param end 종료 날짜
     * @param interval 데이터 간격 (기본값: Interval.OneDay)
     * @return OHLCV 시계열 데이터
     * @throws UfcException INVALID_DATE_RANGE 등
     */
    suspend fun getPriceHistory(
        symbol: String,
        start: LocalDate,
        end: LocalDate,
        interval: Interval = Interval.OneDay
    ): List<OHLCV>

    /**
     * 가격 히스토리 조회 (메타데이터 포함)
     *
     * @param symbol 심볼
     * @param period 조회 기간
     * @param interval 데이터 간격
     * @return (메타데이터, OHLCV 데이터) Pair
     * @throws UfcException
     */
    suspend fun getPriceHistoryWithMetadata(
        symbol: String,
        period: Period,
        interval: Interval = Interval.OneDay
    ): Pair<ChartMetadata, List<OHLCV>>

    /**
     * 히스토리 메타데이터 조회
     *
     * yfinance의 get_history_metadata()에 해당합니다.
     * 가격 데이터 없이 메타정보만 반환합니다.
     *
     * @param symbol 심볼
     * @return 히스토리 메타데이터
     * @throws UfcException
     */
    suspend fun getHistoryMetadata(symbol: String): ChartMetadata

    /**
     * 원본 가격 응답 조회 (quoteSummary API)
     *
     * @param symbol 심볼
     * @return quoteSummary API의 원본 응답
     * @throws UfcException
     */
    suspend fun getRawPrice(symbol: String): PriceResponse

    /**
     * 원본 차트 응답 조회 (chart API)
     *
     * @param symbol 심볼
     * @param period 조회 기간
     * @param interval 데이터 간격
     * @return chart API의 원본 응답
     * @throws UfcException
     */
    suspend fun getRawPriceHistory(
        symbol: String,
        period: Period,
        interval: Interval = Interval.OneDay
    ): ChartResponse
}
