package com.ulalax.ufc.infrastructure.yahoo.internal.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Yahoo Finance Market Summary API의 최상위 응답
 *
 * 응답 구조:
 * ```json
 * {
 *   "marketSummaryResponse": {
 *     "result": [...],
 *     "error": null
 *   }
 * }
 * ```
 */
@Serializable
internal data class MarketSummaryResponse(
    @SerialName("marketSummaryResponse")
    val marketSummaryResponse: MarketSummaryWrapper
)

/**
 * Market Summary 응답의 래퍼
 */
@Serializable
internal data class MarketSummaryWrapper(
    @SerialName("result")
    val result: List<MarketSummaryItemResponse>? = null,

    @SerialName("error")
    val error: ErrorResponse? = null
)

/**
 * 시장 요약 항목 응답
 *
 * 개별 지수 또는 주요 종목의 상세 정보를 담고 있습니다.
 */
@Serializable
internal data class MarketSummaryItemResponse(
    @SerialName("exchange")
    val exchange: String,

    @SerialName("symbol")
    val symbol: String,

    @SerialName("shortName")
    val shortName: String,

    @SerialName("regularMarketPrice")
    val regularMarketPrice: JsonElement? = null,

    @SerialName("regularMarketChange")
    val regularMarketChange: JsonElement? = null,

    @SerialName("regularMarketChangePercent")
    val regularMarketChangePercent: JsonElement? = null,

    @SerialName("regularMarketTime")
    val regularMarketTime: JsonElement? = null,

    @SerialName("regularMarketDayHigh")
    val regularMarketDayHigh: JsonElement? = null,

    @SerialName("regularMarketDayLow")
    val regularMarketDayLow: JsonElement? = null,

    @SerialName("regularMarketVolume")
    val regularMarketVolume: JsonElement? = null,

    @SerialName("regularMarketPreviousClose")
    val regularMarketPreviousClose: JsonElement? = null,

    @SerialName("currency")
    val currency: String? = null,

    @SerialName("marketState")
    val marketState: String? = null,

    @SerialName("quoteType")
    val quoteType: String? = null,

    @SerialName("exchangeTimezoneName")
    val exchangeTimezoneName: String? = null,

    @SerialName("exchangeTimezoneShortName")
    val exchangeTimezoneShortName: String? = null,

    @SerialName("gmtOffSetMilliseconds")
    val gmtOffSetMilliseconds: JsonElement? = null
) {
    /** JsonElement에서 Double 값 추출 (raw 우선, 없으면 primitive) */
    private fun JsonElement.extractDouble(): Double? {
        return try {
            val obj = this.jsonPrimitive.content
            obj.toDoubleOrNull() ?: this.jsonPrimitive.doubleOrNull
        } catch (e: Exception) {
            try {
                this.jsonObject["raw"]?.jsonPrimitive?.content?.toDoubleOrNull()
            } catch (e2: Exception) {
                null
            }
        }
    }

    /** JsonElement에서 Long 값 추출 (raw 우선, 없으면 primitive) */
    private fun JsonElement.extractLong(): Long? {
        return try {
            val obj = this.jsonPrimitive.content
            obj.toLongOrNull() ?: this.jsonPrimitive.longOrNull
        } catch (e: Exception) {
            try {
                this.jsonObject["raw"]?.jsonPrimitive?.content?.toLongOrNull()
            } catch (e2: Exception) {
                null
            }
        }
    }

    val regularMarketPriceValue: Double?
        get() = regularMarketPrice?.extractDouble()

    val regularMarketChangeValue: Double?
        get() = regularMarketChange?.extractDouble()

    val regularMarketChangePercentValue: Double?
        get() = regularMarketChangePercent?.extractDouble()

    val regularMarketTimeLong: Long?
        get() = regularMarketTime?.extractLong()

    val regularMarketDayHighValue: Double?
        get() = regularMarketDayHigh?.extractDouble()

    val regularMarketDayLowValue: Double?
        get() = regularMarketDayLow?.extractDouble()

    val regularMarketVolumeValue: Long?
        get() = regularMarketVolume?.extractLong()

    val regularMarketPreviousCloseValue: Double?
        get() = regularMarketPreviousClose?.extractDouble()

    val gmtOffSetMillisecondsValue: Long?
        get() = gmtOffSetMilliseconds?.extractLong()
}

/**
 * Yahoo API 에러 응답
 */
@Serializable
internal data class ErrorResponse(
    @SerialName("code")
    val code: String? = null,

    @SerialName("description")
    val description: String? = null
)
