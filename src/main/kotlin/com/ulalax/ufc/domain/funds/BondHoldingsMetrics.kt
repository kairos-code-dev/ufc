package com.ulalax.ufc.domain.funds

/**
 * 펀드가 보유한 채권의 메트릭.
 *
 * 펀드가 보유한 채권들의 특성을 나타냅니다.
 * 듀레이션(이자율 변화에 대한 민감도), 만기, 신용등급 등의 메트릭을 포함합니다.
 *
 * @property duration 듀레이션 (년, 이자율 변화에 대한 민감도)
 * @property maturity 평균 만기 (년)
 * @property creditQuality 신용등급 (숫자로 표현)
 */
data class BondHoldingsMetrics(
    val duration: MetricValue?,
    val maturity: MetricValue?,
    val creditQuality: MetricValue?
)
