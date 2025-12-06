package com.ulalax.ufc.domain.model.screener

/**
 * Screener API 조회 결과
 *
 * @property id 결과 ID (Predefined screener인 경우)
 * @property title 제목
 * @property description 설명
 * @property count 현재 결과 수
 * @property total 전체 매칭 종목 수
 * @property start 시작 오프셋
 * @property quotes 종목 리스트
 */
data class ScreenerResult(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val count: Int,
    val total: Int,
    val start: Int,
    val quotes: List<ScreenerQuote>,
)
