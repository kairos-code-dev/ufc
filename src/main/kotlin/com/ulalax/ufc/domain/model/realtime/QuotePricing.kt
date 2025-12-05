package com.ulalax.ufc.domain.model.realtime

import java.time.Instant

/**
 * 주식의 가격 정보
 *
 * @property price 현재가
 * @property open 시가
 * @property dayHigh 당일 고가
 * @property dayLow 당일 저가
 * @property volume 당일 거래량
 * @property previousClose 전일 종가
 * @property change 가격 변화
 * @property changePercent 변화율 (%)
 * @property marketTime 시장 시간
 */
data class QuotePricing(
    val price: Double,
    val open: Double? = null,
    val dayHigh: Double? = null,
    val dayLow: Double? = null,
    val volume: Long? = null,
    val previousClose: Double? = null,
    val change: Double? = null,
    val changePercent: Double? = null,
    val marketTime: Instant? = null
)
