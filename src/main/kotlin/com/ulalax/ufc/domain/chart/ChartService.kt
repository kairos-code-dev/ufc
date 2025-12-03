package com.ulalax.ufc.domain.chart

import com.ulalax.ufc.domain.common.Interval
import com.ulalax.ufc.domain.common.Period

/**
 * Yahoo Finance Chart API를 통한 차트 데이터 조회 서비스 인터페이스
 *
 * 이 서비스는 다음 기능을 제공합니다:
 * - 단일 심볼의 OHLCV 데이터 조회
 * - 다중 심볼의 차트 데이터 조회
 * - 다양한 시간 간격(interval)과 기간(period)을 지원
 *
 * 사용 예시:
 * ```
 * val chartService = yahooChartService
 *
 * // AAPL의 1일 간격, 1년 기간 데이터 조회
 * val data = chartService.getChartData("AAPL", Interval.OneDay, Period.OneYear)
 * println("${data.size} 개의 데이터 포인트 조회")
 *
 * // 다중 심볼 조회
 * val multiData = chartService.getChartData(
 *     symbols = listOf("AAPL", "GOOGL"),
 *     interval = Interval.OneHour,
 *     period = Period.FiveDays
 * )
 * ```
 */
interface ChartService {

    /**
     * 단일 심볼의 차트 데이터를 조회합니다.
     *
     * @param symbol 조회할 심볼 (예: "AAPL", "^GSPC")
     * @param interval 데이터 간격 (기본값: OneDay)
     * @param period 조회 기간 (기본값: OneYear)
     * @return OHLCV 데이터의 리스트
     * @throws UfcException 데이터 조회 실패 시
     */
    suspend fun getChartData(
        symbol: String,
        interval: Interval = Interval.OneDay,
        period: Period = Period.OneYear
    ): List<OHLCVData>

    /**
     * 다중 심볼의 차트 데이터를 조회합니다.
     *
     * @param symbols 조회할 심볼 목록
     * @param interval 데이터 간격 (기본값: OneDay)
     * @param period 조회 기간 (기본값: OneYear)
     * @return 심볼별 OHLCV 데이터 맵 (심볼 -> 데이터 리스트)
     * @throws UfcException 데이터 조회 실패 시
     */
    suspend fun getChartData(
        symbols: List<String>,
        interval: Interval = Interval.OneDay,
        period: Period = Period.OneYear
    ): Map<String, List<OHLCVData>>

    /**
     * 차트 데이터 응답의 원본 형식을 반환합니다.
     *
     * 이 메서드는 사용자가 원본 API 응답 구조에 접근해야 할 경우 사용합니다.
     *
     * @param symbol 조회할 심볼
     * @param interval 데이터 간격
     * @param period 조회 기간
     * @param includeEvents 이벤트 데이터(배당금, 분할, 자본이득) 포함 여부 (기본값: false)
     * @return 원본 ChartDataResponse 객체
     * @throws UfcException 데이터 조회 실패 시
     */
    suspend fun getRawChartData(
        symbol: String,
        interval: Interval = Interval.OneDay,
        period: Period = Period.OneYear,
        includeEvents: Boolean = false
    ): ChartDataResponse
}
