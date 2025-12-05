package com.ulalax.ufc.domain.model.visualization

/**
 * 특정 심볼의 실적 발표 일정 목록 (Visualization API)
 *
 * Visualization API를 통해 조회한 실적 발표 일정입니다.
 * earningsCalendar() API와 달리 POST 요청을 통해 조회하며, 더 간단한 응답을 제공합니다.
 *
 * @property symbol 티커 심볼 (예: "AAPL")
 * @property earningsDates 실적 발표 일정 목록 (최신순 정렬)
 */
data class VisualizationEarningsCalendar(
    val symbol: String,
    val earningsDates: List<EarningsDate>
)
