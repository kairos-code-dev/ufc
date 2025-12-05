package com.ulalax.ufc.domain.model.realtime

/**
 * 이동평균 정보
 *
 * @property fiftyDayAverage 50일 이동평균
 * @property fiftyDayChange 50일 평균 대비 변화
 * @property fiftyDayChangePercent 50일 평균 대비 변화율
 * @property twoHundredDayAverage 200일 이동평균
 * @property twoHundredDayChange 200일 평균 대비 변화
 * @property twoHundredDayChangePercent 200일 평균 대비 변화율
 */
data class QuoteMovingAverages(
    val fiftyDayAverage: Double? = null,
    val fiftyDayChange: Double? = null,
    val fiftyDayChangePercent: Double? = null,
    val twoHundredDayAverage: Double? = null,
    val twoHundredDayChange: Double? = null,
    val twoHundredDayChangePercent: Double? = null
)
