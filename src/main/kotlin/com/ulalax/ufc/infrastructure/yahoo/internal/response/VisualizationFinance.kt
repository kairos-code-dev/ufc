package com.ulalax.ufc.infrastructure.yahoo.internal.response

import kotlinx.serialization.Serializable

/**
 * Visualization API 응답의 finance 객체
 *
 * Internal 타입 - API 역직렬화 전용
 *
 * @property result 결과 목록
 * @property error 에러 정보
 */
@Serializable
internal data class VisualizationFinance(
    val result: List<VisualizationResult>? = null,
    val error: VisualizationError? = null,
)
