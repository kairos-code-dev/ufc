package com.ulalax.ufc.infrastructure.yahoo.internal.response

import kotlinx.serialization.Serializable

/**
 * Visualization API의 최상위 응답 구조
 *
 * Internal 타입 - API 역직렬화 전용
 *
 * @property finance 금융 데이터 래퍼
 */
@Serializable
internal data class VisualizationResponse(
    val finance: VisualizationFinance
)
