package com.ulalax.ufc.infrastructure.yahoo.internal.request

import kotlinx.serialization.Serializable

/**
 * Visualization API POST 요청 본문
 *
 * Internal 타입 - API 직렬화 전용
 *
 * @property size 조회할 실적 일정 개수 (1-100)
 * @property query 심볼 검색 조건
 * @property sortField 정렬 기준 필드 ("startdatetime")
 * @property sortType 정렬 방향 ("DESC" 또는 "ASC")
 * @property entityIdType 엔티티 타입 ("earnings" 고정)
 * @property includeFields 응답에 포함할 필드 목록
 */
@Serializable
internal data class VisualizationRequest(
    val size: Int,
    val query: VisualizationQuery,
    val sortField: String,
    val sortType: String,
    val entityIdType: String,
    val includeFields: List<String>
)
