package com.ulalax.ufc.domain.model.realtime

/**
 * 성장률 정보
 *
 * @property revenueGrowth 매출 성장률
 * @property earningsGrowth 실적 성장률
 */
data class QuoteGrowthRates(
    val revenueGrowth: Double? = null,
    val earningsGrowth: Double? = null,
)
