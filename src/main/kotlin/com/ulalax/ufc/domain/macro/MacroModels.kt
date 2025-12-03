package com.ulalax.ufc.domain.macro

/**
 * 거시경제 시계열 데이터
 *
 * FRED API로부터 조회한 거시경제 지표 시계열 데이터를 나타냅니다.
 *
 * @property seriesId FRED 시리즈 ID (예: "GDPC1", "UNRATE")
 * @property title 시리즈 제목 (예: "Real Gross Domestic Product")
 * @property frequency 주기 (Quarterly, Monthly, Daily 등)
 * @property units 단위 (Billions of Dollars, Percent 등)
 * @property data 데이터 포인트 목록
 *
 * 사용 예시:
 * ```kotlin
 * val gdpSeries = MacroSeries(
 *     seriesId = "GDPC1",
 *     title = "Real Gross Domestic Product",
 *     frequency = "Quarterly",
 *     units = "Billions of Chained 2017 Dollars",
 *     data = listOf(
 *         MacroDataPoint(date = "2024-01-01", value = 27610.1),
 *         MacroDataPoint(date = "2024-04-01", value = 27750.3)
 *     )
 * )
 * ```
 */
data class MacroSeries(
    val seriesId: String,
    val title: String,
    val frequency: String,
    val units: String,
    val data: List<MacroDataPoint>
) {
    /**
     * 데이터 포인트 개수
     */
    fun size(): Int = data.size

    /**
     * 첫 번째 데이터 포인트 (가장 오래된 데이터)
     */
    fun first(): MacroDataPoint? = data.firstOrNull()

    /**
     * 마지막 데이터 포인트 (가장 최근 데이터)
     */
    fun last(): MacroDataPoint? = data.lastOrNull()

    /**
     * 결측치가 아닌 데이터만 필터링
     */
    fun filterNonNull(): MacroSeries = copy(
        data = data.filter { it.value != null }
    )

    /**
     * 특정 날짜 범위의 데이터만 필터링
     *
     * @param startDate 시작일 (YYYY-MM-DD, 포함)
     * @param endDate 종료일 (YYYY-MM-DD, 포함)
     */
    fun filterByDateRange(startDate: String, endDate: String): MacroSeries = copy(
        data = data.filter { it.date >= startDate && it.date <= endDate }
    )
}

/**
 * 거시경제 데이터 포인트
 *
 * 특정 날짜의 관찰값을 나타냅니다.
 *
 * @property date 관찰 날짜 (YYYY-MM-DD)
 * @property value 관찰값 (null인 경우 결측치)
 *
 * 예시:
 * ```kotlin
 * val dataPoint = MacroDataPoint(
 *     date = "2024-01-01",
 *     value = 27610.1
 * )
 * ```
 *
 * FRED API에서 value가 "."인 경우 null로 변환됩니다.
 */
data class MacroDataPoint(
    val date: String,
    val value: Double?
) {
    /**
     * 유효한 데이터인지 확인 (결측치가 아님)
     */
    fun isValid(): Boolean = value != null

    /**
     * 결측치인지 확인
     */
    fun isMissing(): Boolean = value == null
}
