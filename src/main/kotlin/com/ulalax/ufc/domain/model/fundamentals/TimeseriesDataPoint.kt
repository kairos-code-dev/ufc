package com.ulalax.ufc.domain.model.fundamentals

import java.time.LocalDate

/**
 * 재무제표 시계열 데이터의 단일 데이터 포인트
 *
 * Yahoo Finance Fundamentals Timeseries API에서 반환되는 각 시점의 재무 데이터를 나타냅니다.
 *
 * ## 특징
 * - **asOfDate 기준 정렬 가능**: `Comparable` 인터페이스를 구현하여 날짜순 정렬을 지원합니다.
 * - **Nullable 값 지원**: 재무 데이터가 없는 경우 value가 null일 수 있습니다.
 * - **다양한 기간 타입**: 연간(12M), 분기(3M), Trailing(TTM) 등을 구분합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val dataPoint = TimeseriesDataPoint(
 *     asOfDate = LocalDate.of(2023, 12, 31),
 *     periodType = "12M",
 *     value = 394328000000.0,
 *     currencyCode = "USD"
 * )
 *
 * // 날짜순 정렬
 * val sorted = dataPoints.sorted()
 * ```
 *
 * @property asOfDate 재무제표 기준일 (ISO 8601 날짜 형식에서 변환)
 * @property periodType 기간 타입 (12M=연간, 3M=분기, TTM=Trailing, UNKNOWN=알 수 없음)
 * @property value 재무 항목 값 (데이터가 없는 경우 null)
 * @property currencyCode 통화 코드 (USD, KRW 등)
 */
data class TimeseriesDataPoint(
    val asOfDate: LocalDate,
    val periodType: String,
    val value: Double?,
    val currencyCode: String
) : Comparable<TimeseriesDataPoint> {

    /**
     * asOfDate 기준으로 정렬합니다.
     *
     * @param other 비교 대상 데이터 포인트
     * @return 음수(이전), 0(동일), 양수(이후)
     */
    override fun compareTo(other: TimeseriesDataPoint): Int {
        return asOfDate.compareTo(other.asOfDate)
    }
}
