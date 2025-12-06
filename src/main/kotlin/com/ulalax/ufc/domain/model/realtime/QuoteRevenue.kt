package com.ulalax.ufc.domain.model.realtime

/**
 * 매출 및 수익성 정보
 *
 * @property totalRevenue 총 매출
 * @property revenuePerShare 주당 매출
 * @property returnOnAssets ROA (Return on Assets)
 * @property returnOnEquity ROE (Return on Equity)
 * @property profitMargins 영업이익률
 * @property grossMargins 매출총이익률
 */
data class QuoteRevenue(
    val totalRevenue: Long? = null,
    val revenuePerShare: Double? = null,
    val returnOnAssets: Double? = null,
    val returnOnEquity: Double? = null,
    val profitMargins: Double? = null,
    val grossMargins: Double? = null,
)
