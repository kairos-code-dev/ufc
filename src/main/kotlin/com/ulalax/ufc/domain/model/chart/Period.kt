package com.ulalax.ufc.domain.model.chart

/**
 * Enum representing data query periods.
 *
 * Period definitions primarily compatible with Yahoo Finance API.
 *
 * @property value The value used in API requests
 */
enum class Period(val value: String) {
    /**
     * 1 day period
     */
    OneDay("1d"),

    /**
     * 5 days period
     */
    FiveDays("5d"),

    /**
     * 1 month period
     */
    OneMonth("1mo"),

    /**
     * 3 months period
     */
    ThreeMonths("3mo"),

    /**
     * 6 months period
     */
    SixMonths("6mo"),

    /**
     * 1 year period
     */
    OneYear("1y"),

    /**
     * 2 years period
     */
    TwoYears("2y"),

    /**
     * 5 years period
     */
    FiveYears("5y"),

    /**
     * 10 years period
     */
    TenYears("10y"),

    /**
     * Year-to-date period
     */
    YearToDate("ytd"),

    /**
     * Maximum period (entire history)
     */
    Max("max"),
}

/**
 * Finds a Period enum from a string value.
 *
 * @param value The value to look up
 * @return The corresponding Period, or null if not found
 */
fun periodFromValue(value: String): Period? {
    return Period.values().find { it.value == value }
}

/**
 * Converts Period to a human-readable Korean string.
 *
 * @return Korean representation
 */
fun Period.toKoreanString(): String = when (this) {
    Period.OneDay -> "1일"
    Period.FiveDays -> "5일"
    Period.OneMonth -> "1개월"
    Period.ThreeMonths -> "3개월"
    Period.SixMonths -> "6개월"
    Period.OneYear -> "1년"
    Period.TwoYears -> "2년"
    Period.FiveYears -> "5년"
    Period.TenYears -> "10년"
    Period.YearToDate -> "연초~현재"
    Period.Max -> "전체"
}
