package com.ulalax.ufc.domain.price

/**
 * 시계열 OHLCV(Open, High, Low, Close, Volume) 데이터
 *
 * 특정 시점(일, 시간, 분 등)의 가격 정보를 나타냅니다.
 *
 * @property timestamp 데이터 타임스탬프 (Unix timestamp, seconds)
 * @property open 시가 (Open)
 * @property high 고가 (High)
 * @property low 저가 (Low)
 * @property close 종가 (Close)
 * @property adjClose 조정종가 (Adjusted Close, 배당/분할 조정됨)
 * @property volume 거래량 (주)
 */
data class OHLCV(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val adjClose: Double?,
    val volume: Long
) {
    /**
     * 일중 가격 변동폭 (절대값)
     * High - Low
     *
     * @return 변동폭
     */
    fun range(): Double = high - low

    /**
     * 일중 변동률 (%)
     * (High - Low) / Low * 100
     *
     * @return 변동률 (%)
     */
    fun rangePercent(): Double {
        if (low == 0.0) return 0.0
        return ((high - low) / low) * 100.0
    }

    /**
     * 종가 기준 변동 (절대값)
     * Close - Open
     *
     * @return 변동액
     */
    fun change(): Double = close - open

    /**
     * 종가 기준 변동률 (%)
     * (Close - Open) / Open * 100
     *
     * @return 변동률 (%)
     */
    fun changePercent(): Double {
        if (open == 0.0) return 0.0
        return ((close - open) / open) * 100.0
    }

    /**
     * 양봉(상승) 여부
     * Close > Open이면 양봉
     *
     * @return true면 양봉, false면 음봉 또는 보합
     */
    fun isBullish(): Boolean = close > open

    /**
     * 음봉(하락) 여부
     * Close < Open이면 음봉
     *
     * @return true면 음봉, false면 양봉 또는 보합
     */
    fun isBearish(): Boolean = close < open
}
