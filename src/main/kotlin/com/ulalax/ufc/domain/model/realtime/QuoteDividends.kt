package com.ulalax.ufc.domain.model.realtime

import java.time.LocalDate

/**
 * 배당 정보
 *
 * @property annualRate 연간 배당금
 * @property yield 배당수익률
 * @property dividendDate 배당 지급일
 * @property exDividendDate 배당락일
 * @property trailingRate 과거 12개월 배당금
 * @property trailingYield 과거 12개월 배당수익률
 */
data class QuoteDividends(
    val annualRate: Double? = null,
    val yield: Double? = null,
    val dividendDate: LocalDate? = null,
    val exDividendDate: LocalDate? = null,
    val trailingRate: Double? = null,
    val trailingYield: Double? = null
)
