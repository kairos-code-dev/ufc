package com.ulalax.ufc.domain.model.series

/**
 * FRED (Federal Reserve Economic Data) API의 데이터 주기를 나타내는 열거형입니다.
 *
 * 경제 데이터 조회 시 사용되는 주기 정의입니다.
 *
 * @property value API 요청 시 사용되는 값
 */
enum class DataFrequency(val value: String) {
    /**
     * 일일 데이터
     */
    Daily("d"),

    /**
     * 주간 데이터 (매주)
     */
    Weekly("w"),

    /**
     * 격주 데이터 (2주마다)
     */
    Biweekly("bw"),

    /**
     * 월간 데이터 (매월)
     */
    Monthly("m"),

    /**
     * 분기 데이터 (3개월마다)
     */
    Quarterly("q"),

    /**
     * 반기 데이터 (6개월마다)
     */
    Semiannual("sa"),

    /**
     * 연간 데이터 (매년)
     */
    Annual("a"),
}

/**
 * 문자열 값으로부터 DataFrequency 열거형을 찾습니다.
 *
 * @param value 조회할 값
 * @return 해당하는 DataFrequency, 없으면 null
 */
fun dataFrequencyFromValue(value: String): DataFrequency? {
    return DataFrequency.values().find { it.value == value }
}

/**
 * DataFrequency를 읽기 쉬운 한글 문자열로 변환합니다.
 *
 * @return 한글 표현
 */
fun DataFrequency.toKoreanString(): String = when (this) {
    DataFrequency.Daily -> "일일"
    DataFrequency.Weekly -> "주간"
    DataFrequency.Biweekly -> "격주"
    DataFrequency.Monthly -> "월간"
    DataFrequency.Quarterly -> "분기"
    DataFrequency.Semiannual -> "반기"
    DataFrequency.Annual -> "연간"
}

/**
 * DataFrequency의 상대적 크기를 비교합니다.
 *
 * @param other 비교할 DataFrequency
 * @return this가 other보다 크면 양수, 같으면 0, 작으면 음수
 */
fun DataFrequency.compareSizeWith(other: DataFrequency): Int {
    val sizeMap = mapOf(
        DataFrequency.Daily to 1,
        DataFrequency.Weekly to 5,
        DataFrequency.Biweekly to 10,
        DataFrequency.Monthly to 20,
        DataFrequency.Quarterly to 60,
        DataFrequency.Semiannual to 120,
        DataFrequency.Annual to 365,
    )
    return (sizeMap[this] ?: 0).compareTo(sizeMap[other] ?: 0)
}

/**
 * DataFrequency가 고주기 데이터(월간 이상)인지 확인합니다.
 *
 * @return 월간 이상 주기이면 true
 */
fun DataFrequency.isLongTerm(): Boolean {
    return this in listOf(
        DataFrequency.Monthly,
        DataFrequency.Quarterly,
        DataFrequency.Semiannual,
        DataFrequency.Annual
    )
}

/**
 * DataFrequency가 단기 데이터(주간 이하)인지 확인합니다.
 *
 * @return 주간 이하 주기이면 true
 */
fun DataFrequency.isShortTerm(): Boolean {
    return this in listOf(
        DataFrequency.Daily,
        DataFrequency.Weekly,
        DataFrequency.Biweekly
    )
}
