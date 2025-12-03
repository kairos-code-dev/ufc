package com.ulalax.ufc.infrastructure.util

import com.ulalax.ufc.domain.chart.OHLCVData

/**
 * 기술적 지표 계산 유틸리티
 *
 * 주가 데이터를 기반으로 다양한 기술적 지표를 계산하는 유틸리티 객체입니다.
 * 현재 SMA(단순 이동평균)와 EMA(지수 이동평균)를 지원합니다.
 *
 * ## 사용 예시:
 * ```kotlin
 * val prices = listOf(10.0, 11.0, 12.0, 11.0, 13.0, 14.0)
 * val sma5 = TechnicalIndicators.calculateSMA(prices, 5)
 * val ema5 = TechnicalIndicators.calculateEMA(prices, 5)
 * ```
 *
 * @since 1.0.0
 */
object TechnicalIndicators {

    /**
     * 단순 이동평균 (Simple Moving Average) 계산
     *
     * SMA는 지정된 기간 동안의 가격 데이터의 산술 평균을 계산합니다.
     *
     * ## 알고리즘:
     * ```
     * SMA = (P1 + P2 + ... + Pn) / n
     * ```
     * 여기서 P는 가격, n은 기간(period)입니다.
     *
     * ## 동작 방식:
     * - period 미만의 데이터 포인트에 대해서는 null을 반환합니다.
     * - period 이상의 데이터가 있을 때부터 이동평균을 계산합니다.
     * - 결과 리스트는 입력 리스트와 동일한 크기를 가집니다.
     *
     * @param data 가격 데이터 리스트 (시간순 정렬, 오래된 순서 → 최신 순서)
     * @param period 이동평균 기간 (예: 20일, 50일, 200일)
     * @return 이동평균 값 리스트 (앞부분 period-1개는 null)
     * @throws IllegalArgumentException period가 1보다 작은 경우
     *
     * @example
     * ```kotlin
     * val prices = listOf(10.0, 11.0, 12.0, 11.0, 13.0)
     * val sma3 = calculateSMA(prices, 3)
     * // 결과: [null, null, 11.0, 11.33, 12.0]
     * ```
     */
    fun calculateSMA(data: List<Double>, period: Int): List<Double?> {
        require(period > 0) { "Period must be greater than 0, but was $period" }

        if (data.isEmpty()) return emptyList()
        if (data.size < period) return List(data.size) { null }

        return data.indices.map { index ->
            if (index < period - 1) {
                null
            } else {
                val sum = data.subList(index - period + 1, index + 1).sum()
                sum / period
            }
        }
    }

    /**
     * 지수 이동평균 (Exponential Moving Average) 계산
     *
     * EMA는 최근 데이터에 더 큰 가중치를 부여하는 이동평균입니다.
     * SMA보다 최근 가격 변화에 더 민감하게 반응합니다.
     *
     * ## 알고리즘:
     * ```
     * Multiplier = 2 / (period + 1)
     * EMA_today = (Price_today × Multiplier) + (EMA_yesterday × (1 - Multiplier))
     * ```
     *
     * ## 초기값 처리:
     * - 첫 번째 EMA 값은 같은 기간의 SMA를 사용합니다.
     * - 이후 EMA는 이전 EMA 값을 기반으로 지수 가중 계산을 수행합니다.
     *
     * @param data 가격 데이터 리스트 (시간순 정렬, 오래된 순서 → 최신 순서)
     * @param period 이동평균 기간
     * @return EMA 값 리스트 (앞부분 period-1개는 null)
     * @throws IllegalArgumentException period가 1보다 작은 경우
     *
     * @example
     * ```kotlin
     * val prices = listOf(10.0, 11.0, 12.0, 11.0, 13.0)
     * val ema3 = calculateEMA(prices, 3)
     * // 결과: [null, null, 11.0, 11.0, 12.0]
     * ```
     */
    fun calculateEMA(data: List<Double>, period: Int): List<Double?> {
        require(period > 0) { "Period must be greater than 0, but was $period" }

        if (data.isEmpty()) return emptyList()
        if (data.size < period) return List(data.size) { null }

        val multiplier = 2.0 / (period + 1)
        val result = MutableList<Double?>(data.size) { null }

        // 첫 번째 EMA는 SMA로 시작
        val firstSMA = data.subList(0, period).average()
        result[period - 1] = firstSMA

        // 이후 EMA 계산
        for (i in period until data.size) {
            val previousEMA = result[i - 1]!!
            result[i] = (data[i] * multiplier) + (previousEMA * (1 - multiplier))
        }

        return result
    }

    /**
     * OHLCV 리스트에서 종가 기반 SMA 계산
     *
     * OHLCV 데이터 구조에서 종가(close)를 추출하여 SMA를 계산합니다.
     * 이는 실제 차트 데이터를 처리할 때 유용합니다.
     *
     * @param ohlcv OHLCV 데이터 리스트 (시간순 정렬)
     * @param period 이동평균 기간
     * @return 종가 기반 SMA 값 리스트
     * @throws IllegalArgumentException period가 1보다 작은 경우
     *
     * @see calculateSMA
     * @see OHLCVData
     */
    fun calculateSMAFromOHLCV(ohlcv: List<OHLCVData>, period: Int): List<Double?> {
        val closePrices = ohlcv.map { it.close }
        return calculateSMA(closePrices, period)
    }

    /**
     * OHLCV 리스트에서 종가 기반 EMA 계산
     *
     * OHLCV 데이터 구조에서 종가(close)를 추출하여 EMA를 계산합니다.
     * 이는 실제 차트 데이터를 처리할 때 유용합니다.
     *
     * @param ohlcv OHLCV 데이터 리스트 (시간순 정렬)
     * @param period 이동평균 기간
     * @return 종가 기반 EMA 값 리스트
     * @throws IllegalArgumentException period가 1보다 작은 경우
     *
     * @see calculateEMA
     * @see OHLCVData
     */
    fun calculateEMAFromOHLCV(ohlcv: List<OHLCVData>, period: Int): List<Double?> {
        val closePrices = ohlcv.map { it.close }
        return calculateEMA(closePrices, period)
    }
}
