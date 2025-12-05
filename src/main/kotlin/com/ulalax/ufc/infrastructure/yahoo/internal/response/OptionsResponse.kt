package com.ulalax.ufc.infrastructure.yahoo.internal.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Yahoo Finance Options API 응답
 *
 * Options API 엔드포인트의 최상위 응답 구조입니다.
 */
@Serializable
internal data class OptionsResponse(
    @SerialName("optionChain")
    val optionChain: OptionChainWrapper
)

/**
 * 옵션 체인 래퍼
 *
 * result 배열과 error 정보를 포함합니다.
 */
@Serializable
internal data class OptionChainWrapper(
    @SerialName("result")
    val result: List<OptionsResult>? = null,

    @SerialName("error")
    val error: OptionsError? = null
)

/**
 * 옵션 조회 결과
 *
 * 단일 심볼의 옵션 체인 데이터를 나타냅니다.
 */
@Serializable
internal data class OptionsResult(
    @SerialName("underlyingSymbol")
    val underlyingSymbol: String,

    @SerialName("expirationDates")
    val expirationDates: List<Long> = emptyList(),

    @SerialName("strikes")
    val strikes: List<Double> = emptyList(),

    @SerialName("hasMiniOptions")
    val hasMiniOptions: Boolean = false,

    @SerialName("quote")
    val quote: UnderlyingQuoteResponse? = null,

    @SerialName("options")
    val options: List<OptionsChainResponse> = emptyList()
)

/**
 * 만기일별 옵션 체인
 *
 * 특정 만기일의 콜/풋 옵션 목록을 포함합니다.
 */
@Serializable
internal data class OptionsChainResponse(
    @SerialName("expirationDate")
    val expirationDate: Long,

    @SerialName("hasMiniOptions")
    val hasMiniOptions: Boolean = false,

    @SerialName("calls")
    val calls: List<OptionContractResponse> = emptyList(),

    @SerialName("puts")
    val puts: List<OptionContractResponse> = emptyList()
)

/**
 * 옵션 계약 정보
 *
 * 개별 옵션 계약(콜 또는 풋)의 상세 정보입니다.
 */
@Serializable
internal data class OptionContractResponse(
    @SerialName("contractSymbol")
    val contractSymbol: String,

    @SerialName("strike")
    val strike: Double,

    @SerialName("currency")
    val currency: String,

    @SerialName("lastPrice")
    val lastPrice: Double? = null,

    @SerialName("change")
    val change: Double? = null,

    @SerialName("percentChange")
    val percentChange: Double? = null,

    @SerialName("volume")
    val volume: Long? = null,

    @SerialName("openInterest")
    val openInterest: Long? = null,

    @SerialName("bid")
    val bid: Double? = null,

    @SerialName("ask")
    val ask: Double? = null,

    @SerialName("contractSize")
    val contractSize: String,

    @SerialName("expiration")
    val expiration: Long,

    @SerialName("lastTradeDate")
    val lastTradeDate: Long? = null,

    @SerialName("impliedVolatility")
    val impliedVolatility: Double? = null,

    @SerialName("inTheMoney")
    val inTheMoney: Boolean
)

/**
 * 기초 자산 가격 정보
 *
 * 옵션의 기초 자산(주식/지수)의 현재 가격 정보입니다.
 */
@Serializable
internal data class UnderlyingQuoteResponse(
    @SerialName("symbol")
    val symbol: String,

    @SerialName("shortName")
    val shortName: String? = null,

    @SerialName("regularMarketPrice")
    val regularMarketPrice: Double? = null,

    @SerialName("regularMarketChange")
    val regularMarketChange: Double? = null,

    @SerialName("regularMarketChangePercent")
    val regularMarketChangePercent: Double? = null,

    @SerialName("regularMarketVolume")
    val regularMarketVolume: Long? = null,

    @SerialName("regularMarketTime")
    val regularMarketTime: Long? = null
)

/**
 * 옵션 API 에러 응답
 */
@Serializable
internal data class OptionsError(
    @SerialName("code")
    val code: String? = null,

    @SerialName("description")
    val description: String? = null
)
