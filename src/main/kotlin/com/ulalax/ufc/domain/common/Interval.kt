package com.ulalax.ufc.domain.common

/**
 * 데이터 조회 간격(시간 단위)을 나타내는 열거형입니다.
 *
 * Yahoo Finance API와 호환되는 간격 정의입니다.
 *
 * @property value API 요청 시 사용되는 값
 * @property minutes 간격을 분 단위로 나타낸 값
 */
enum class Interval(val value: String, val minutes: Int) {
    /**
     * 1분 간격
     */
    OneMinute("1m", 1),

    /**
     * 2분 간격
     */
    TwoMinutes("2m", 2),

    /**
     * 5분 간격
     */
    FiveMinutes("5m", 5),

    /**
     * 15분 간격
     */
    FifteenMinutes("15m", 15),

    /**
     * 30분 간격
     */
    ThirtyMinutes("30m", 30),

    /**
     * 1시간 간격
     */
    OneHour("1h", 60),

    /**
     * 1일 간격
     */
    OneDay("1d", 1440),

    /**
     * 5일 간격
     */
    FiveDays("5d", 7200),

    /**
     * 1주일 간격
     */
    OneWeek("1wk", 10080),

    /**
     * 1개월 간격
     */
    OneMonth("1mo", 43200),

    /**
     * 3개월 간격
     */
    ThreeMonths("3mo", 129600),
}

/**
 * 문자열 값으로부터 Interval 열거형을 찾습니다.
 *
 * @param value 조회할 값
 * @return 해당하는 Interval, 없으면 null
 */
fun intervalFromValue(value: String): Interval? {
    return Interval.values().find { it.value == value }
}

/**
 * Interval을 읽기 쉬운 한글 문자열로 변환합니다.
 *
 * @return 한글 표현
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
 * Interval 값이 분 단위 데이터에 적합한지 확인합니다.
 *
 * @return 분 단위 간격이면 true
 */
fun Interval.isIntraday(): Boolean {
    return this.minutes < 1440  // 1440분 = 1일
}

/**
 * Interval 값이 일 단위 이상의 데이터에 적합한지 확인합니다.
 *
 * @return 일 단위 이상 간격이면 true
 */
fun Interval.isDailyOrLonger(): Boolean {
    return this.minutes >= 1440  // 1440분 = 1일
}
