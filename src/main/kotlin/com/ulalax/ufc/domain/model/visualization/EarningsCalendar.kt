package com.ulalax.ufc.domain.model.visualization

/**
 * 특정 심볼의 실적 발표 일정 목록
 *
 * @property symbol 티커 심볼 (예: "AAPL")
 * @property earningsDates 실적 발표 일정 목록 (최신순 정렬)
 */
data class EarningsCalendar(
    val symbol: String,
    val earningsDates: List<EarningsDate>
)
