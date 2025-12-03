package com.ulalax.ufc.domain.funds

/**
 * 펀드 운영 메트릭 값 (펀드값 + 카테고리 평균).
 *
 * MetricValue와 동일한 구조로, 운영 관련 메트릭(비용률, 회전율 등)을 담습니다.
 *
 * @property fundValue 펀드의 메트릭 값 (비용률, 회전율 등)
 * @property categoryAverage 같은 카테고리의 평균값
 */
data class OperationMetric(
    val fundValue: Double?,
    val categoryAverage: Double?
)
