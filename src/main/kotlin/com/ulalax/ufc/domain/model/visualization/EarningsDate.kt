package com.ulalax.ufc.domain.model.visualization

/**
 * 개별 실적 발표 일정 정보
 *
 * @property earningsDate 실적 발표 일시 (epoch seconds)
 * @property timezoneShortName 시간대 약어 (예: "EST", "PST")
 * @property epsEstimate 애널리스트 컨센서스 EPS 추정치
 * @property epsActual 발표된 실제 EPS (과거 실적만 존재)
 * @property surprisePercent 서프라이즈 비율 (%) = (실제 - 추정) / 추정 × 100
 * @property eventType 이벤트 타입 (CALL, EARNINGS, MEETING)
 */
data class EarningsDate(
    val earningsDate: Long,
    val timezoneShortName: String? = null,
    val epsEstimate: Double? = null,
    val epsActual: Double? = null,
    val surprisePercent: Double? = null,
    val eventType: EarningsEventType,
)
