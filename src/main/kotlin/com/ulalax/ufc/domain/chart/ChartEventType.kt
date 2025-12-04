package com.ulalax.ufc.domain.chart

/**
 * Chart API에서 지원하는 이벤트 타입
 *
 * Yahoo Finance Chart API의 events 파라미터에 사용되는 이벤트 종류입니다.
 * 각 이벤트는 API에서 요구하는 값으로 매핑됩니다.
 *
 * 사용 예시:
 * ```kotlin
 * val events = setOf(ChartEventType.DIVIDEND, ChartEventType.SPLIT)
 * val result = ufc.chart(symbol = "AAPL", events = events)
 * ```
 *
 * @property apiValue API 요청시 사용되는 실제 값
 */
enum class ChartEventType(val apiValue: String) {
    /**
     * 배당금 이벤트
     *
     * 주식의 배당금 지급 내역을 조회합니다.
     */
    DIVIDEND("div"),

    /**
     * 주식 분할 이벤트
     *
     * 주식 분할(Stock Split) 내역을 조회합니다.
     */
    SPLIT("split"),

    /**
     * 자본 이득 이벤트
     *
     * ETF 등에서 발생하는 자본 이득(Capital Gain) 내역을 조회합니다.
     */
    CAPITAL_GAIN("capitalGain")
}
