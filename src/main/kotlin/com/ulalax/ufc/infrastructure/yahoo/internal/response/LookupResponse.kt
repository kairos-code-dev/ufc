package com.ulalax.ufc.infrastructure.yahoo.internal.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Yahoo Finance Lookup API의 최상위 응답 모델
 *
 * Lookup API는 검색어 기반으로 금융상품을 검색합니다.
 * 타입별 필터링을 지원하여 주식, ETF, 펀드 등을 분류하여 검색할 수 있습니다.
 */
@Serializable
internal data class LookupResponse(
    @SerialName("finance")
    val finance: LookupFinance,
)

/**
 * Lookup API 응답의 finance 컨테이너
 */
@Serializable
internal data class LookupFinance(
    @SerialName("result")
    val result: List<LookupResultResponse>? = null,
    @SerialName("error")
    val error: LookupError? = null,
)

/**
 * Lookup API 검색 결과
 */
@Serializable
internal data class LookupResultResponse(
    @SerialName("count")
    val count: Int? = null,
    @SerialName("start")
    val start: Int? = null,
    @SerialName("total")
    val total: Int? = null,
    @SerialName("documents")
    val documents: List<LookupDocumentResponse>? = null,
)

/**
 * 검색된 금융상품 개별 정보
 */
@Serializable
internal data class LookupDocumentResponse(
    @SerialName("symbol")
    val symbol: String? = null,
    @SerialName("shortName")
    val shortName: String? = null,
    @SerialName("exchange")
    val exchange: String? = null,
    @SerialName("quoteType")
    val quoteType: String? = null,
    @SerialName("industryName")
    val industryName: String? = null,
    @SerialName("rank")
    val rank: Int? = null,
    @SerialName("industryLink")
    val industryLink: String? = null,
    // 하위 호환성을 위해 유지 (사용되지 않을 수 있음)
    @SerialName("name")
    val name: String? = null,
    @SerialName("exch")
    val exch: String? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("exchDisp")
    val exchDisp: String? = null,
    @SerialName("typeDisp")
    val typeDisp: String? = null,
    @SerialName("industry")
    val industry: String? = null,
    @SerialName("industryDisp")
    val industryDisp: String? = null,
    @SerialName("sector")
    val sector: String? = null,
    @SerialName("sectorDisp")
    val sectorDisp: String? = null,
    @SerialName("score")
    val score: Double? = null,
    @SerialName("isYahooFinance")
    val isYahooFinance: Boolean? = null,
)

/**
 * Lookup API 에러 응답
 */
@Serializable
internal data class LookupError(
    @SerialName("code")
    val code: String? = null,
    @SerialName("description")
    val description: String? = null,
)
