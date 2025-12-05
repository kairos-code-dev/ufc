package com.ulalax.ufc.infrastructure.yahoo.internal.response

import kotlinx.serialization.Serializable

/**
 * Visualization API 에러 응답
 *
 * Internal 타입 - API 역직렬화 전용
 *
 * @property code 에러 코드
 * @property description 에러 설명
 */
@Serializable
internal data class VisualizationError(
    val code: String? = null,
    val description: String? = null
)
