package com.ulalax.ufc.domain.funds

/**
 * 펀드 메트릭 값 (펀드값 + 카테고리 평균).
 *
 * 펀드의 특정 메트릭과 같은 카테고리의 평균값을 함께 담습니다.
 * 이를 통해 펀드의 상대적 위치를 파악할 수 있습니다.
 *
 * @property fundValue 펀드의 메트릭 값
 * @property categoryAverage 같은 카테고리의 평균값
 */
data class MetricValue(
    val fundValue: Double?,
    val categoryAverage: Double?
)
