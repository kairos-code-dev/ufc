package com.ulalax.ufc.infrastructure.yahoo.internal.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Screener Custom Query 요청 바디
 *
 * POST /v1/finance/screener에 사용됩니다.
 */
@Serializable
internal data class ScreenerRequest(
    @SerialName("query")
    val query: JsonElement,

    @SerialName("quoteType")
    val quoteType: String,

    @SerialName("sortField")
    val sortField: String,

    @SerialName("sortType")
    val sortType: String,

    @SerialName("size")
    val size: Int,

    @SerialName("offset")
    val offset: Int,

    @SerialName("userId")
    val userId: String = "",

    @SerialName("userIdType")
    val userIdType: String = "guid"
)
