package com.ulalax.ufc.domain.model.realtime

/**
 * 재무 건전성 정보
 *
 * @property totalCash 총 현금
 * @property totalCashPerShare 주당 현금
 * @property totalDebt 총 부채
 * @property debtToEquity 부채비율
 * @property currentRatio 유동비율
 * @property quickRatio 당좌비율
 */
data class QuoteFinancialHealth(
    val totalCash: Long? = null,
    val totalCashPerShare: Double? = null,
    val totalDebt: Long? = null,
    val debtToEquity: Double? = null,
    val currentRatio: Double? = null,
    val quickRatio: Double? = null,
)
