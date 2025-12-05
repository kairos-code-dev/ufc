package com.ulalax.ufc.domain.model.realtime

/**
 * 애널리스트 의견 정보
 *
 * @property targetHighPrice 목표가 상한
 * @property targetLowPrice 목표가 하한
 * @property targetMeanPrice 목표가 평균
 * @property targetMedianPrice 목표가 중앙값
 * @property recommendationMean 추천 평균 (1-5 척도: 1=강력매수, 5=강력매도)
 * @property recommendationKey 추천 등급 (buy/hold/sell 등)
 * @property numberOfAnalystOpinions 애널리스트 수
 */
data class QuoteAnalystRatings(
    val targetHighPrice: Double? = null,
    val targetLowPrice: Double? = null,
    val targetMeanPrice: Double? = null,
    val targetMedianPrice: Double? = null,
    val recommendationMean: Double? = null,
    val recommendationKey: String? = null,
    val numberOfAnalystOpinions: Int? = null
)
