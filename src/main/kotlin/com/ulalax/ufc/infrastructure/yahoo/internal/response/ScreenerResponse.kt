package com.ulalax.ufc.infrastructure.yahoo.internal.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Yahoo Finance Screener API 응답 모델
 */
@Serializable
internal data class ScreenerResponse(
    @SerialName("finance")
    val finance: FinanceContainer,
)

/**
 * Finance 컨테이너
 *
 * result와 error 중 하나만 존재합니다.
 */
@Serializable
internal data class FinanceContainer(
    @SerialName("result")
    val result: List<ScreenerApiResult>? = null,
    @SerialName("error")
    val error: ScreenerError? = null,
)

/**
 * Screener API 결과
 */
@Serializable
internal data class ScreenerApiResult(
    @SerialName("id")
    val id: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("count")
    val count: Int = 0,
    @SerialName("total")
    val total: Int = 0,
    @SerialName("start")
    val start: Int = 0,
    @SerialName("quotes")
    val quotes: List<Map<String, JsonElement>> = emptyList(),
)

/**
 * Screener API 에러
 */
@Serializable
internal data class ScreenerError(
    @SerialName("code")
    val code: String,
    @SerialName("description")
    val description: String,
)
