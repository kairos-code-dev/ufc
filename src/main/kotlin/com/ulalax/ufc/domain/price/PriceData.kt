package com.ulalax.ufc.domain.price

/**
 * 금융 상품의 현재 가격 및 관련 통계 정보
 *
 * @property symbol 심볼 (예: AAPL, SPY, ^GSPC)
 * @property currency 통화 (USD, KRW 등)
 * @property exchange 거래소 (NMS, NYQ 등)
 * @property lastPrice 최근 거래 가격
 * @property regularMarketPrice 정규 시장 가격
 * @property regularMarketTime 정규 시장 시간 (Unix timestamp)
 * @property open 일중 시가
 * @property dayHigh 일중 최고가
 * @property dayLow 일중 최저가
 * @property previousClose 전일 종가
 * @property volume 거래량 (주)
 * @property regularMarketVolume 정규 시장 거래량
 * @property averageVolume 평균 거래량
 * @property averageVolume10days 10일 평균 거래량
 * @property fiftyTwoWeekHigh 52주 최고가
 * @property fiftyTwoWeekLow 52주 최저가
 * @property fiftyTwoWeekChange 52주 변동액
 * @property fiftyTwoWeekChangePercent 52주 변동률 (%)
 * @property fiftyDayAverage 50일 이동평균선
 * @property twoHundredDayAverage 200일 이동평균선
 * @property marketCap 시가총액
 * @property dividendYield 배당수익률 (%)
 * @property dividendRate 배당금
 * @property exDividendDate 배당락일 (Unix timestamp)
 * @property beta 베타
 * @property trailingPE 트레일링 PER
 * @property forwardPE 포워드 PER
 */
data class PriceData(
    val symbol: String,
    val currency: String?,
    val exchange: String?,

    val lastPrice: Double?,
    val regularMarketPrice: Double?,
    val regularMarketTime: Long?,

    val open: Double?,
    val dayHigh: Double?,
    val dayLow: Double?,
    val previousClose: Double?,

    val volume: Long?,
    val regularMarketVolume: Long?,
    val averageVolume: Long?,
    val averageVolume10days: Long?,

    val fiftyTwoWeekHigh: Double?,
    val fiftyTwoWeekLow: Double?,
    val fiftyTwoWeekChange: Double?,
    val fiftyTwoWeekChangePercent: Double?,

    val fiftyDayAverage: Double?,
    val twoHundredDayAverage: Double?,

    val marketCap: Long?,

    val dividendYield: Double?,
    val dividendRate: Double?,
    val exDividendDate: Long?,

    val beta: Double?,
    val trailingPE: Double?,
    val forwardPE: Double?
) {
    /**
     * 52주 범위 대비 현재 가격의 위치 (0.0 ~ 1.0)
     * 0.0 = 52주 최저가, 1.0 = 52주 최고가
     *
     * @return 위치 비율 (null일 경우 계산 불가)
     */
    fun fiftyTwoWeekPosition(): Double? {
        val price = lastPrice ?: regularMarketPrice ?: return null
        val high = fiftyTwoWeekHigh ?: return null
        val low = fiftyTwoWeekLow ?: return null

        if (high == low) return null
        return (price - low) / (high - low)
    }

    /**
     * 일중 가격 변동률 (%)
     * (현재가 - 전일종가) / 전일종가 * 100
     *
     * @return 변동률 (%)
     */
    fun dailyChangePercent(): Double? {
        val current = lastPrice ?: regularMarketPrice ?: return null
        val prev = previousClose ?: return null

        if (prev == 0.0) return null
        return ((current - prev) / prev) * 100.0
    }

    /**
     * 가격이 50일 이동평균선 위에 있는지 확인
     *
     * @return true면 50일 MA 위, false면 아래, null은 계산 불가
     */
    fun isAbove50DayMA(): Boolean? {
        val price = lastPrice ?: regularMarketPrice ?: return null
        val ma50 = fiftyDayAverage ?: return null
        return price > ma50
    }

    /**
     * 가격이 200일 이동평균선 위에 있는지 확인
     *
     * @return true면 200일 MA 위, false면 아래, null은 계산 불가
     */
    fun isAbove200DayMA(): Boolean? {
        val price = lastPrice ?: regularMarketPrice ?: return null
        val ma200 = twoHundredDayAverage ?: return null
        return price > ma200
    }
}
