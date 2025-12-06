package com.ulalax.ufc.domain.model.market

/**
 * 시장 요약 결과
 *
 * 특정 시장의 주요 지수 목록과 시장 정보를 담고 있습니다.
 *
 * @property market 조회한 시장
 * @property items 지수 목록
 */
data class MarketSummaryResult(
    val market: MarketCode,
    val items: List<MarketSummaryItem>,
)
