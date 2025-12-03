package com.ulalax.ufc.domain.common

/**
 * 데이터 조회 기간을 나타내는 열거형입니다.
 *
 * Yahoo Finance API와 주로 호환되는 기간 정의입니다.
 *
 * @property value API 요청 시 사용되는 값
 */
enum class Period(val value: String) {
    /**
     * 1일 기간
     */
    OneDay("1d"),

    /**
     * 5일 기간
     */
    FiveDays("5d"),

    /**
     * 1개월 기간
     */
    OneMonth("1mo"),

    /**
     * 3개월 기간
     */
    ThreeMonths("3mo"),

    /**
     * 6개월 기간
     */
    SixMonths("6mo"),

    /**
     * 1년 기간
     */
    OneYear("1y"),

    /**
     * 2년 기간
     */
    TwoYears("2y"),

    /**
     * 5년 기간
     */
    FiveYears("5y"),

    /**
     * 10년 기간
     */
    TenYears("10y"),

    /**
     * 연초 이후 기간 (Year-To-Date)
     */
    YearToDate("ytd"),

    /**
     * 최대 기간 (전체 이력)
     */
    Max("max"),
}

/**
 * 문자열 값으로부터 Period 열거형을 찾습니다.
 *
 * @param value 조회할 값
 * @return 해당하는 Period, 없으면 null
 */
fun periodFromValue(value: String): Period? {
    return Period.values().find { it.value == value }
}

/**
 * Period를 읽기 쉬운 한글 문자열로 변환합니다.
 *
 * @return 한글 표현
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
