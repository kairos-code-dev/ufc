package com.ulalax.ufc.domain.model.chart

/**
 * Enum representing data query intervals (time units).
 *
 * Interval definitions compatible with Yahoo Finance API.
 *
 * @property value The value used in API requests
 * @property minutes The interval represented in minutes
 */
enum class Interval(val value: String, val minutes: Int) {
    /**
     * 1 minute interval
     */
    OneMinute("1m", 1),

    /**
     * 2 minutes interval
     */
    TwoMinutes("2m", 2),

    /**
     * 5 minutes interval
     */
    FiveMinutes("5m", 5),

    /**
     * 15 minutes interval
     */
    FifteenMinutes("15m", 15),

    /**
     * 30 minutes interval
     */
    ThirtyMinutes("30m", 30),

    /**
     * 1 hour interval
     */
    OneHour("1h", 60),

    /**
     * 1 day interval
     */
    OneDay("1d", 1440),

    /**
     * 5 days interval
     */
    FiveDays("5d", 7200),

    /**
     * 1 week interval
     */
    OneWeek("1wk", 10080),

    /**
     * 1 month interval
     */
    OneMonth("1mo", 43200),

    /**
     * 3 months interval
     */
    ThreeMonths("3mo", 129600),
}

/**
 * Finds an Interval enum from a string value.
 *
 * @param value The value to look up
 * @return The corresponding Interval, or null if not found
 */
fun intervalFromValue(value: String): Interval? {
    return Interval.values().find { it.value == value }
}

/**
 * Converts Interval to a human-readable Korean string.
 *
 * @return Korean representation
 */
fun Interval.toKoreanString(): String = when (this) {
    Interval.OneMinute -> "1분"
    Interval.TwoMinutes -> "2분"
    Interval.FiveMinutes -> "5분"
    Interval.FifteenMinutes -> "15분"
    Interval.ThirtyMinutes -> "30분"
    Interval.OneHour -> "1시간"
    Interval.OneDay -> "1일"
    Interval.FiveDays -> "5일"
    Interval.OneWeek -> "1주"
    Interval.OneMonth -> "1개월"
    Interval.ThreeMonths -> "3개월"
}

/**
 * Checks if the Interval value is suitable for intraday data.
 *
 * @return true if interval is less than daily
 */
fun Interval.isIntraday(): Boolean {
    return this.minutes < 1440  // 1440 minutes = 1 day
}

/**
 * Checks if the Interval value is suitable for daily or longer period data.
 *
 * @return true if interval is daily or longer
 */
fun Interval.isDailyOrLonger(): Boolean {
    return this.minutes >= 1440  // 1440 minutes = 1 day
}
