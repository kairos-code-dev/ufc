package com.ulalax.ufc.infrastructure.yahoo.internal.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Yahoo Finance Market Time API의 최상위 응답
 *
 * 응답 구조:
 * ```json
 * {
 *   "finance": {
 *     "marketTimes": [
 *       {
 *         "marketTime": [...]
 *       }
 *     ],
 *     "error": null
 *   }
 * }
 * ```
 */
@Serializable
internal data class MarketTimeResponse(
    @SerialName("finance")
    val finance: FinanceWrapper
)

/**
 * Finance 응답의 래퍼
 */
@Serializable
internal data class FinanceWrapper(
    @SerialName("marketTimes")
    val marketTimes: List<MarketTimeWrapper>? = null,

    @SerialName("error")
    val error: ErrorResponse? = null
)

/**
 * Market Time 응답의 래퍼
 */
@Serializable
internal data class MarketTimeWrapper(
    @SerialName("marketTime")
    val marketTime: List<MarketTimeItemResponse>
)

/**
 * 시장 시간 정보 응답
 *
 * 특정 시장의 거래 시간 및 현재 상태를 담고 있습니다.
 */
@Serializable
internal data class MarketTimeItemResponse(
    @SerialName("exchange")
    val exchange: String? = null,

    @SerialName("market")
    val market: String? = null,

    @SerialName("marketState")
    val marketState: String? = null,

    @SerialName("open")
    val open: String? = null,

    @SerialName("close")
    val close: String? = null,

    @SerialName("preMarket")
    val preMarket: PrePostMarketResponse? = null,

    @SerialName("postMarket")
    val postMarket: PrePostMarketResponse? = null,

    @SerialName("timezone")
    val timezone: List<TimezoneResponse>? = null,

    @SerialName("time")
    val time: String? = null
)

/**
 * Pre-Market 또는 Post-Market 거래 시간 응답
 */
@Serializable
internal data class PrePostMarketResponse(
    @SerialName("start")
    val start: String,

    @SerialName("end")
    val end: String
)

/**
 * 타임존 정보 응답
 */
@Serializable
internal data class TimezoneResponse(
    @SerialName("short")
    val short: String? = null,

    @SerialName("name")
    val name: String? = null,

    @SerialName("gmtoffset")
    val gmtoffset: Long? = null
)
