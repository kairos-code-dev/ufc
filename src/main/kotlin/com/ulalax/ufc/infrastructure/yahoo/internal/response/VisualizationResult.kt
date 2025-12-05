package com.ulalax.ufc.infrastructure.yahoo.internal.response

import kotlinx.serialization.Serializable

/**
 * Visualization API 응답의 결과
 *
 * Internal 타입 - API 역직렬화 전용
 *
 * @property documents 문서 목록 (테이블 형식 데이터)
 */
@Serializable
internal data class VisualizationResult(
    val documents: List<VisualizationDocument>? = null
)
