package com.ulalax.ufc.domain.model.realtime

/**
 * 재무 비율 정보
 *
 * @property trailingPE 후행 PER (Price to Earnings Ratio)
 * @property forwardPE 선행 PER
 * @property priceToBook PBR (Price to Book Ratio)
 * @property priceToSales PSR (Price to Sales Ratio)
 * @property bookValue 주당 장부가치
 * @property earningsQuarterlyGrowth 분기 실적 성장률
 */
data class QuoteFinancialRatios(
    val trailingPE: Double? = null,
    val forwardPE: Double? = null,
    val priceToBook: Double? = null,
    val priceToSales: Double? = null,
    val bookValue: Double? = null,
    val earningsQuarterlyGrowth: Double? = null
)
