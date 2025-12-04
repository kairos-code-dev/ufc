package com.ulalax.ufc.domain.fundamentals

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Yahoo Finance Fundamentals Timeseries API 응답
 *
 * GET /ws/fundamentals-timeseries/v1/finance/timeseries/{symbol}
 * - 발행주식수(sharesOutstanding) 히스토리를 제공
 * - Unix timestamp 기반 시계열 데이터
 *
 * 사용 예시:
 * ```
 * GET https://query2.finance.yahoo.com/ws/fundamentals-timeseries/v1/finance/timeseries/AAPL
 *     ?period1=1609459200&period2=1640995200&symbol=AAPL
 * ```
 */
@Serializable
data class FundamentalsTimeseriesResponse(
    @SerialName("timeseries")
    val timeseries: TimeseriesWrapper
)

@Serializable
data class TimeseriesWrapper(
    @SerialName("result")
    val result: List<TimeseriesResult>? = null,

    @SerialName("error")
    val error: TimeseriesError? = null
)

@Serializable
data class TimeseriesResult(
    @SerialName("meta")
    val meta: TimeseriesMeta? = null,

    /**
     * Unix timestamp 배열 (초 단위)
     */
    @SerialName("timestamp")
    val timestamp: List<Long>? = null,

    /**
     * 발행주식수 배열
     * - timestamp 배열과 1:1 대응
     * - 각 값은 해당 시점의 발행주식수
     */
    @SerialName("shares_out")
    val sharesOut: List<Long>? = null
)

@Serializable
data class TimeseriesMeta(
    @SerialName("symbol")
    val symbol: List<String>? = null,

    @SerialName("type")
    val type: List<String>? = null
)

@Serializable
data class TimeseriesError(
    @SerialName("code")
    val code: String? = null,

    @SerialName("description")
    val description: String? = null
)
