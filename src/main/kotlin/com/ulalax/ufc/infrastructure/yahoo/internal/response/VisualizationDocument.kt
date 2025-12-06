package com.ulalax.ufc.infrastructure.yahoo.internal.response

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Visualization API 응답의 문서 (테이블 형식 데이터)
 *
 * Internal 타입 - API 역직렬화 전용
 *
 * @property columns 컬럼 정의 목록
 * @property rows 데이터 행 목록 (각 행은 JsonElement 리스트)
 */
@Serializable
internal data class VisualizationDocument(
    val columns: List<VisualizationColumn>,
    val rows: List<List<JsonElement>>,
)
