package com.ulalax.ufc.domain.model.realtime

/**
 * 거래량 평균 정보
 *
 * @property averageDailyVolume3Month 3개월 평균 일일 거래량
 * @property averageDailyVolume10Day 10일 평균 일일 거래량
 */
data class QuoteVolumes(
    val averageDailyVolume3Month: Long? = null,
    val averageDailyVolume10Day: Long? = null
)
