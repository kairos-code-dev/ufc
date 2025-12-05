package com.ulalax.ufc.domain.model.realtime

/**
 * 52주 고저가 정보
 *
 * @property high 52주 최고가
 * @property low 52주 최저가
 * @property highChange 최고가 대비 변화
 * @property lowChange 최저가 대비 변화
 * @property highChangePercent 최고가 대비 변화율
 * @property lowChangePercent 최저가 대비 변화율
 * @property range 52주 범위 (예: "150.00 - 200.00")
 */
data class QuoteFiftyTwoWeek(
    val high: Double? = null,
    val low: Double? = null,
    val highChange: Double? = null,
    val lowChange: Double? = null,
    val highChangePercent: Double? = null,
    val lowChangePercent: Double? = null,
    val range: String? = null
)
