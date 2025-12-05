package com.ulalax.ufc.domain.model.options

import kotlin.math.max

/**
 * 옵션 계약 정보
 *
 * 개별 옵션 계약(콜 또는 풋)의 상세 정보를 나타냅니다.
 *
 * @property contractSymbol 계약 심볼 (예: "AAPL250103C00150000")
 * @property strike 행사가
 * @property currency 통화 (예: "USD")
 * @property lastPrice 최종 거래 가격
 * @property change 가격 변동
 * @property percentChange 변동률 (%)
 * @property volume 거래량
 * @property openInterest 미결제 약정 수
 * @property bid 매수 호가
 * @property ask 매도 호가
 * @property contractSize 계약 크기 (예: "REGULAR")
 * @property expiration 만기일 (Unix timestamp, seconds)
 * @property lastTradeDate 최종 거래 일시 (Unix timestamp, seconds)
 * @property impliedVolatility 내재 변동성 (0~1 범위)
 * @property inTheMoney ITM(내가격) 여부
 */
data class OptionContract(
    val contractSymbol: String,
    val strike: Double,
    val currency: String,
    val lastPrice: Double? = null,
    val change: Double? = null,
    val percentChange: Double? = null,
    val volume: Long? = null,
    val openInterest: Long? = null,
    val bid: Double? = null,
    val ask: Double? = null,
    val contractSize: String,
    val expiration: Long,
    val lastTradeDate: Long? = null,
    val impliedVolatility: Double? = null,
    val inTheMoney: Boolean
) {
    /**
     * 매수-매도 호가 스프레드 (절대값)
     *
     * 매도호가 - 매수호가를 계산합니다.
     * bid 또는 ask가 없으면 null을 반환합니다.
     *
     * @return 호가 스프레드, 데이터가 없으면 null
     */
    fun getBidAskSpread(): Double? {
        return if (bid != null && ask != null) {
            ask - bid
        } else {
            null
        }
    }

    /**
     * 매수-매도 호가 스프레드 비율 (%)
     *
     * (Ask - Bid) / ((Ask + Bid) / 2) * 100 으로 계산합니다.
     * bid 또는 ask가 없으면 null을 반환합니다.
     *
     * @return 호가 스프레드 비율 (%), 데이터가 없으면 null
     */
    fun getBidAskSpreadPercent(): Double? {
        return if (bid != null && ask != null) {
            val mid = (ask + bid) / 2.0
            if (mid == 0.0) return null
            ((ask - bid) / mid) * 100.0
        } else {
            null
        }
    }

    /**
     * 중간 가격 (Mid Price)
     *
     * (Bid + Ask) / 2 로 계산합니다.
     * bid 또는 ask가 없으면 null을 반환합니다.
     *
     * @return 중간 가격, 데이터가 없으면 null
     */
    fun getMidPrice(): Double? {
        return if (bid != null && ask != null) {
            (bid + ask) / 2.0
        } else {
            null
        }
    }

    /**
     * 내재 가치 (Intrinsic Value)
     *
     * 콜옵션: max(0, 현재가 - 행사가)
     * 풋옵션: max(0, 행사가 - 현재가)
     *
     * @param underlyingPrice 기초 자산 현재가
     * @param isCall 콜옵션이면 true, 풋옵션이면 false
     * @return 내재 가치
     */
    fun getIntrinsicValue(underlyingPrice: Double, isCall: Boolean): Double {
        return if (isCall) {
            max(0.0, underlyingPrice - strike)
        } else {
            max(0.0, strike - underlyingPrice)
        }
    }

    /**
     * 시간 가치 (Time Value)
     *
     * 시간 가치 = 옵션 가격 - 내재 가치
     * lastPrice가 없으면 null을 반환합니다.
     *
     * @param underlyingPrice 기초 자산 현재가
     * @param isCall 콜옵션이면 true, 풋옵션이면 false
     * @return 시간 가치, lastPrice가 없으면 null
     */
    fun getTimeValue(underlyingPrice: Double, isCall: Boolean): Double? {
        return lastPrice?.let {
            it - getIntrinsicValue(underlyingPrice, isCall)
        }
    }
}
