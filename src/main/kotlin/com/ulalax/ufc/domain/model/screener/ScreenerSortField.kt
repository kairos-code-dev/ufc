package com.ulalax.ufc.domain.model.screener

/**
 * Screener 결과 정렬 필드
 */
enum class ScreenerSortField(
    val apiValue: String,
) {
    TICKER("ticker"),
    PERCENT_CHANGE("percentchange"),
    DAY_VOLUME("dayvolume"),
    EOD_VOLUME("eodvolume"),
    MARKET_CAP("intradaymarketcap"),
    PE_RATIO("peratio.lasttwelvemonths"),
    FUND_NET_ASSETS("fundnetassets"),
}
