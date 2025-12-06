package com.ulalax.ufc.domain.model.chart

/**
 * OHLCV (Open, High, Low, Close, Volume) 가격 데이터
 *
 * Chart API에서 사용되는 OHLCV 데이터를 나타냅니다.
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
    val volume: Long,
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

/**
 * Chart API 조회 결과
 *
 * Yahoo Finance Chart API를 통해 조회한 차트 데이터를 나타냅니다.
 * 가격 데이터(OHLCV)는 항상 포함되며, 이벤트 데이터는 요청시에만 포함됩니다.
 *
 * 사용 예시:
 * ```kotlin
 * val result = ufc.chart(
 *     symbol = "AAPL",
 *     events = setOf(ChartEventType.DIVIDEND, ChartEventType.SPLIT)
 * )
 *
 * // 요청한 이벤트 확인
 * if (result.hasEvent(ChartEventType.DIVIDEND)) {
 *     val dividends = result.getDividends()
 * }
 *
 * // 가격 데이터는 항상 존재
 * result.prices.forEach { ohlcv ->
 *     println("${ohlcv.timestamp}: ${ohlcv.close}")
 * }
 * ```
 *
 * @property requestedEvents 요청한 이벤트 종류 목록
 * @property meta 차트 메타데이터 (심볼, 통화, 현재가 등)
 * @property prices OHLCV 가격 데이터 목록 (항상 포함)
 * @property events 이벤트 데이터 (배당, 분할, 자본이득 등, 요청시에만 포함)
 */
data class ChartData(
    val requestedEvents: Set<ChartEventType>,
    val meta: ChartMeta,
    val prices: List<OHLCV>,
    val events: ChartEvents?,
) {
    /**
     * 특정 이벤트 타입을 요청했는지 확인
     *
     * 요청하지 않은 이벤트는 events에 포함되지 않을 수 있으므로,
     * events 데이터를 사용하기 전에 요청 여부를 확인하는 것이 좋습니다.
     *
     * @param eventType 확인할 이벤트 타입
     * @return 요청한 이벤트면 true, 아니면 false
     */
    fun hasEvent(eventType: ChartEventType): Boolean = eventType in requestedEvents

    /**
     * 배당금 이벤트 데이터 가져오기
     *
     * 배당금 이벤트를 요청한 경우에만 데이터가 반환됩니다.
     *
     * @return 배당금 이벤트 맵 (timestamp → event), 없으면 null
     */
    fun getDividends(): Map<String, DividendEvent>? =
        if (hasEvent(ChartEventType.DIVIDEND)) {
            events?.dividends
        } else {
            null
        }

    /**
     * 주식 분할 이벤트 데이터 가져오기
     *
     * 주식 분할 이벤트를 요청한 경우에만 데이터가 반환됩니다.
     *
     * @return 주식 분할 이벤트 맵 (timestamp → event), 없으면 null
     */
    fun getSplits(): Map<String, SplitEvent>? =
        if (hasEvent(ChartEventType.SPLIT)) {
            events?.splits
        } else {
            null
        }

    /**
     * 자본 이득 이벤트 데이터 가져오기
     *
     * 자본 이득 이벤트를 요청한 경우에만 데이터가 반환됩니다.
     *
     * @return 자본 이득 이벤트 맵 (timestamp → event), 없으면 null
     */
    fun getCapitalGains(): Map<String, CapitalGainEvent>? =
        if (hasEvent(ChartEventType.CAPITAL_GAIN)) {
            events?.capitalGains
        } else {
            null
        }
}

/**
 * 차트 메타데이터
 *
 * 주식/지수의 기본 정보를 포함합니다:
 * - 심볼, 통화, 현재가, 범위 등
 */
data class ChartMeta(
    // 심볼 (예: "AAPL", "^GSPC")
    val symbol: String? = null,
    // 통화 (예: "USD")
    val currency: String? = null,
    // 통화 기호 (예: "$")
    val currencySymbol: String? = null,
    // 현재 가격
    val regularMarketPrice: Double? = null,
    // 교환 시장 (예: "NASDAQ", "NYSE")
    val exchange: String? = null,
    // 최고가 (조회 기간 내)
    val regularMarketDayHigh: Double? = null,
    // 최저가 (조회 기간 내)
    val regularMarketDayLow: Double? = null,
    // 데이터 간격 (예: "1d", "1h")
    val dataGranularity: String? = null,
    // 범위 (예: "1y", "1d")
    val range: String? = null,
    // 52주 최고가
    val fiftyTwoWeekHigh: Double? = null,
    // 52주 최저가
    val fiftyTwoWeekLow: Double? = null,
    // 발행 주식수
    val sharesOutstanding: Long? = null,
    // 시가총액
    val marketCap: Long? = null,
    // 거래량
    val regularMarketVolume: Long? = null,
    // 유효한 범위
    val validRanges: List<String>? = null,
)

/**
 * 차트 이벤트 데이터
 *
 * 배당금, 주식분할, 자본이득 등의 이벤트 정보를 포함합니다.
 */
data class ChartEvents(
    val dividends: Map<String, DividendEvent>? = null,
    val splits: Map<String, SplitEvent>? = null,
    val capitalGains: Map<String, CapitalGainEvent>? = null,
)

/**
 * 배당금 이벤트
 */
data class DividendEvent(
    val amount: Double? = null,
    val date: Long? = null,
)

/**
 * 주식 분할 이벤트
 */
data class SplitEvent(
    val date: Long? = null,
    val numerator: Double? = null,
    val denominator: Double? = null,
    val splitRatio: String? = null,
)

/**
 * 자본이득 이벤트
 */
data class CapitalGainEvent(
    val amount: Double? = null,
    val date: Long? = null,
)
