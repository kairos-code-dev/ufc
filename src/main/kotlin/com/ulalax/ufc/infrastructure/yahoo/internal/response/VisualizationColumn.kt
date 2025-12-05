package com.ulalax.ufc.infrastructure.yahoo.internal.response

import kotlinx.serialization.Serializable

/**
 * Visualization API 응답의 컬럼 정의
 *
 * Internal 타입 - API 역직렬화 전용
 *
 * @property label 컬럼 레이블
 * @property id 컬럼 ID
 */
@Serializable
internal data class VisualizationColumn(
    val label: String,
    val id: String
)
