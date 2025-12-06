package com.ulalax.ufc.domain.model.screener

/**
 * Yahoo Finance에서 제공하는 사전 정의된 스크리너
 *
 * @property apiId API에서 사용하는 ID
 * @property defaultSortField 기본 정렬 필드
 * @property defaultSortAsc 기본 정렬 방향 (true: 오름차순, false: 내림차순)
 */
enum class PredefinedScreener(
    val apiId: String,
    val defaultSortField: ScreenerSortField,
    val defaultSortAsc: Boolean,
) {
    // Equity Screeners
    AGGRESSIVE_SMALL_CAPS("aggressive_small_caps", ScreenerSortField.EOD_VOLUME, false),
    DAY_GAINERS("day_gainers", ScreenerSortField.PERCENT_CHANGE, false),
    DAY_LOSERS("day_losers", ScreenerSortField.PERCENT_CHANGE, true),
    MOST_ACTIVES("most_actives", ScreenerSortField.DAY_VOLUME, false),
    GROWTH_TECHNOLOGY_STOCKS("growth_technology_stocks", ScreenerSortField.EOD_VOLUME, false),
    UNDERVALUED_GROWTH_STOCKS("undervalued_growth_stocks", ScreenerSortField.EOD_VOLUME, false),

    // Fund Screeners
    HIGH_YIELD_BOND("high_yield_bond", ScreenerSortField.FUND_NET_ASSETS, false),
    SOLID_LARGE_GROWTH_FUNDS("solid_large_growth_funds", ScreenerSortField.FUND_NET_ASSETS, false),
    TOP_MUTUAL_FUNDS("top_mutual_funds", ScreenerSortField.PERCENT_CHANGE, false),
}
