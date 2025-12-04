package com.ulalax.ufc.domain.chart

import com.ulalax.ufc.domain.price.OHLCV

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
    val events: ChartEvents?
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
    fun hasEvent(eventType: ChartEventType): Boolean {
        return eventType in requestedEvents
    }

    /**
     * 배당금 이벤트 데이터 가져오기
     *
     * 배당금 이벤트를 요청한 경우에만 데이터가 반환됩니다.
     *
     * @return 배당금 이벤트 맵 (timestamp → event), 없으면 null
     */
    fun getDividends(): Map<String, DividendEvent>? {
        return if (hasEvent(ChartEventType.DIVIDEND)) {
            events?.dividends
        } else {
            null
        }
    }

    /**
     * 주식 분할 이벤트 데이터 가져오기
     *
     * 주식 분할 이벤트를 요청한 경우에만 데이터가 반환됩니다.
     *
     * @return 주식 분할 이벤트 맵 (timestamp → event), 없으면 null
     */
    fun getSplits(): Map<String, SplitEvent>? {
        return if (hasEvent(ChartEventType.SPLIT)) {
            events?.splits
        } else {
            null
        }
    }

    /**
     * 자본 이득 이벤트 데이터 가져오기
     *
     * 자본 이득 이벤트를 요청한 경우에만 데이터가 반환됩니다.
     *
     * @return 자본 이득 이벤트 맵 (timestamp → event), 없으면 null
     */
    fun getCapitalGains(): Map<String, CapitalGainEvent>? {
        return if (hasEvent(ChartEventType.CAPITAL_GAIN)) {
            events?.capitalGains
        } else {
            null
        }
    }
}
