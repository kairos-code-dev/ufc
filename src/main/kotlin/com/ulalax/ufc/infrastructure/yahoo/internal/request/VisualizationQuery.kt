package com.ulalax.ufc.infrastructure.yahoo.internal.request

import kotlinx.serialization.Serializable

/**
 * Visualization API 요청의 쿼리 조건
 *
 * Internal 타입 - API 직렬화 전용
 *
 * @property operator 연산자 ("eq" 고정)
 * @property operands 피연산자 배열 ["ticker", "{symbol}"]
 */
@Serializable
internal data class VisualizationQuery(
    val operator: String,
    val operands: List<String>,
)
