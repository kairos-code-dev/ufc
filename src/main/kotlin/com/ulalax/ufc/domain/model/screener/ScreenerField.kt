package com.ulalax.ufc.domain.model.screener

/**
 * Screener 필터링에 사용할 수 있는 필드를 나타내는 인터페이스
 */
sealed interface ScreenerField {
    /** API에서 사용하는 필드명 */
    val apiValue: String

    /** 필드 카테고리 (예: price, trading, valuation) */
    val category: String

    /** 필드 값 타입 */
    val valueType: FieldValueType
}

/**
 * 주식(Equity) 필터링 필드
 */
enum class EquityField(
    override val apiValue: String,
    override val category: String,
    override val valueType: FieldValueType
) : ScreenerField {
    // 공통
    REGION("region", "common", FieldValueType.STRING),
    SECTOR("sector", "common", FieldValueType.STRING),
    INDUSTRY("industry", "common", FieldValueType.STRING),
    EXCHANGE("exchange", "common", FieldValueType.STRING),

    // 가격
    INTRADAY_MARKET_CAP("intradaymarketcap", "price", FieldValueType.NUMBER),
    INTRADAY_PRICE("intradayprice", "price", FieldValueType.NUMBER),
    PERCENT_CHANGE("percentchange", "price", FieldValueType.NUMBER),
    EOD_PRICE("eodprice", "price", FieldValueType.NUMBER),

    // 밸류에이션
    PE_RATIO("peratio.lasttwelvemonths", "valuation", FieldValueType.NUMBER),
    PEG_RATIO_5Y("pegratio_5y", "valuation", FieldValueType.NUMBER),
    PRICE_BOOK_RATIO("pricebookratio.quarterly", "valuation", FieldValueType.NUMBER),

    // 수익성
    RETURN_ON_ASSETS("returnonassets.lasttwelvemonths", "profitability", FieldValueType.NUMBER),
    RETURN_ON_EQUITY("returnonequity.lasttwelvemonths", "profitability", FieldValueType.NUMBER),
    FORWARD_DIVIDEND_YIELD("forward_dividend_yield", "profitability", FieldValueType.NUMBER),

    // 거래
    BETA("beta", "trading", FieldValueType.NUMBER),
    AVG_DAILY_VOL_3M("avgdailyvol3m", "trading", FieldValueType.NUMBER),
    DAY_VOLUME("dayvolume", "trading", FieldValueType.NUMBER),

    // 손익계산서
    TOTAL_REVENUES("totalrevenues.lasttwelvemonths", "income_statement", FieldValueType.NUMBER),
    NET_INCOME("netincomeis.lasttwelvemonths", "income_statement", FieldValueType.NUMBER),
    EBITDA("ebitda.lasttwelvemonths", "income_statement", FieldValueType.NUMBER)
}

/**
 * 펀드(Fund) 필터링 필드
 */
enum class FundField(
    override val apiValue: String,
    override val category: String,
    override val valueType: FieldValueType
) : ScreenerField {
    // 공통
    REGION("region", "common", FieldValueType.STRING),
    CATEGORY("category", "common", FieldValueType.STRING),

    // 가격
    INTRADAY_PRICE("intradayprice", "price", FieldValueType.NUMBER),
    PERCENT_CHANGE("percentchange", "price", FieldValueType.NUMBER),

    // 펀드 특화
    FUND_NET_ASSETS("fundnetassets", "fund", FieldValueType.NUMBER),
    PERFORMANCE_RATING("performancerating", "fund", FieldValueType.NUMBER),
    YIELD("yield", "fund", FieldValueType.NUMBER),
    EXPENSE_RATIO("expenseratio", "fund", FieldValueType.NUMBER)
}
