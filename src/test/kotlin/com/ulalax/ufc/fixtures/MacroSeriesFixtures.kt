package com.ulalax.ufc.fixtures

import com.ulalax.ufc.domain.macro.MacroDataPoint
import com.ulalax.ufc.domain.macro.MacroSeries

/**
 * MacroSeries 테스트 데이터 Fixture (Mother Pattern)
 *
 * MacroSeries 도메인 모델의 다양한 시나리오를 위한 테스트 데이터를 제공합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val gdpSeries = MacroSeriesFixtures.gdpRealQuarterly()
 * val cpiSeries = MacroSeriesFixtures.cpiMonthly()
 * val customSeries = MacroSeriesFixtures.builder()
 *     .withSeriesId("UNRATE")
 *     .withDataPoints(listOf(...))
 *     .build()
 * ```
 */
object MacroSeriesFixtures {

    /**
     * 실질 GDP 분기별 데이터 생성
     *
     * FRED 시리즈 ID: GDPC1
     * 주기: Quarterly
     *
     * @return 실질 GDP MacroSeries
     */
    fun gdpRealQuarterly(): MacroSeries = MacroSeries(
        seriesId = "GDPC1",
        title = "Real Gross Domestic Product",
        frequency = "Quarterly",
        units = "Billions of Chained 2017 Dollars",
        data = listOf(
            MacroDataPoint(date = "2023-01-01", value = 22000.0),
            MacroDataPoint(date = "2023-04-01", value = 22200.0),
            MacroDataPoint(date = "2023-07-01", value = 22400.0),
            MacroDataPoint(date = "2023-10-01", value = 22600.0),
            MacroDataPoint(date = "2024-01-01", value = 22800.0)
        )
    )

    /**
     * CPI 월별 데이터 생성
     *
     * FRED 시리즈 ID: CPIAUCSL
     * 주기: Monthly
     *
     * @return CPI MacroSeries
     */
    fun cpiMonthly(): MacroSeries = MacroSeries(
        seriesId = "CPIAUCSL",
        title = "Consumer Price Index for All Urban Consumers: All Items",
        frequency = "Monthly",
        units = "Index 1982-1984=100",
        data = listOf(
            MacroDataPoint(date = "2024-01-01", value = 308.4),
            MacroDataPoint(date = "2024-02-01", value = 309.1),
            MacroDataPoint(date = "2024-03-01", value = 309.7),
            MacroDataPoint(date = "2024-04-01", value = 310.3),
            MacroDataPoint(date = "2024-05-01", value = 310.9)
        )
    )

    /**
     * 실업률 월별 데이터 생성
     *
     * FRED 시리즈 ID: UNRATE
     * 주기: Monthly
     *
     * @return 실업률 MacroSeries
     */
    fun unemploymentRateMonthly(): MacroSeries = MacroSeries(
        seriesId = "UNRATE",
        title = "Unemployment Rate",
        frequency = "Monthly",
        units = "Percent",
        data = listOf(
            MacroDataPoint(date = "2024-01-01", value = 3.7),
            MacroDataPoint(date = "2024-02-01", value = 3.8),
            MacroDataPoint(date = "2024-03-01", value = 3.9),
            MacroDataPoint(date = "2024-04-01", value = 3.6),
            MacroDataPoint(date = "2024-05-01", value = 3.5)
        )
    )

    /**
     * 연방기금금리 일별 데이터 생성
     *
     * FRED 시리즈 ID: DFF
     * 주기: Daily
     *
     * @return 연방기금금리 MacroSeries
     */
    fun federalFundsRateDaily(): MacroSeries = MacroSeries(
        seriesId = "DFF",
        title = "Federal Funds Effective Rate",
        frequency = "Daily",
        units = "Percent",
        data = listOf(
            MacroDataPoint(date = "2024-01-01", value = 5.33),
            MacroDataPoint(date = "2024-01-02", value = 5.33),
            MacroDataPoint(date = "2024-01-03", value = 5.33),
            MacroDataPoint(date = "2024-01-04", value = 5.33),
            MacroDataPoint(date = "2024-01-05", value = 5.33)
        )
    )

    /**
     * 결측치가 포함된 시계열 데이터 생성
     *
     * 일부 데이터 포인트의 value가 null인 경우
     *
     * @return 결측치 포함 MacroSeries
     */
    fun seriesWithMissingValues(): MacroSeries = MacroSeries(
        seriesId = "TEST_MISSING",
        title = "Test Series with Missing Values",
        frequency = "Monthly",
        units = "Percent",
        data = listOf(
            MacroDataPoint(date = "2024-01-01", value = 100.0),
            MacroDataPoint(date = "2024-02-01", value = null),
            MacroDataPoint(date = "2024-03-01", value = 102.0),
            MacroDataPoint(date = "2024-04-01", value = null),
            MacroDataPoint(date = "2024-05-01", value = 104.0)
        )
    )

    /**
     * 빈 데이터 시계열 생성
     *
     * 데이터 포인트가 없는 경우
     *
     * @return 빈 MacroSeries
     */
    fun emptyDataSeries(): MacroSeries = MacroSeries(
        seriesId = "TEST_EMPTY",
        title = "Test Series with Empty Data",
        frequency = "Monthly",
        units = "Index",
        data = emptyList()
    )

    /**
     * 단일 데이터 포인트 시계열 생성
     *
     * 데이터 포인트가 1개만 있는 경우
     *
     * @return 단일 데이터 MacroSeries
     */
    fun singleDataPointSeries(): MacroSeries = MacroSeries(
        seriesId = "TEST_SINGLE",
        title = "Test Series with Single Data Point",
        frequency = "Annual",
        units = "Billions",
        data = listOf(
            MacroDataPoint(date = "2024-01-01", value = 1000.0)
        )
    )

    /**
     * 상승 추세 시계열 데이터 생성
     *
     * 지속적으로 증가하는 패턴
     *
     * @return 상승 추세 MacroSeries
     */
    fun uptrendSeries(): MacroSeries = MacroSeries(
        seriesId = "TEST_UPTREND",
        title = "Test Uptrend Series",
        frequency = "Monthly",
        units = "Index",
        data = listOf(
            MacroDataPoint(date = "2024-01-01", value = 100.0),
            MacroDataPoint(date = "2024-02-01", value = 105.0),
            MacroDataPoint(date = "2024-03-01", value = 110.0),
            MacroDataPoint(date = "2024-04-01", value = 115.0),
            MacroDataPoint(date = "2024-05-01", value = 120.0)
        )
    )

    /**
     * 하락 추세 시계열 데이터 생성
     *
     * 지속적으로 감소하는 패턴
     *
     * @return 하락 추세 MacroSeries
     */
    fun downtrendSeries(): MacroSeries = MacroSeries(
        seriesId = "TEST_DOWNTREND",
        title = "Test Downtrend Series",
        frequency = "Monthly",
        units = "Index",
        data = listOf(
            MacroDataPoint(date = "2024-01-01", value = 120.0),
            MacroDataPoint(date = "2024-02-01", value = 115.0),
            MacroDataPoint(date = "2024-03-01", value = 110.0),
            MacroDataPoint(date = "2024-04-01", value = 105.0),
            MacroDataPoint(date = "2024-05-01", value = 100.0)
        )
    )

    /**
     * 커스텀 MacroSeries 데이터를 생성하기 위한 빌더 반환
     *
     * @return MacroSeriesBuilder 인스턴스
     */
    fun builder(): MacroSeriesBuilder = MacroSeriesBuilder()

    /**
     * MacroSeries 빌더 클래스
     *
     * 테스트에 필요한 특정 필드만 변경하여 MacroSeries를 생성할 수 있습니다.
     *
     * ## 사용 예시
     * ```kotlin
     * val series = MacroSeriesFixtures.builder()
     *     .withSeriesId("GDPC1")
     *     .withTitle("Real GDP")
     *     .withDataPoints(listOf(...))
     *     .build()
     * ```
     */
    class MacroSeriesBuilder {
        private var seriesId: String = "TEST_SERIES"
        private var title: String = "Test Series"
        private var frequency: String = "Monthly"
        private var units: String = "Index"
        private var data: List<MacroDataPoint> = emptyList()

        fun withSeriesId(seriesId: String) = apply { this.seriesId = seriesId }

        fun withTitle(title: String) = apply { this.title = title }

        fun withFrequency(frequency: String) = apply { this.frequency = frequency }

        fun withUnits(units: String) = apply { this.units = units }

        fun withDataPoints(data: List<MacroDataPoint>) = apply { this.data = data }

        /**
         * 간단한 데이터 포인트 추가
         *
         * @param dateValuePairs 날짜와 값의 쌍 목록 ("2024-01-01" to 100.0)
         */
        fun withData(vararg dateValuePairs: Pair<String, Double?>) = apply {
            this.data = dateValuePairs.map { (date, value) ->
                MacroDataPoint(date = date, value = value)
            }
        }

        /**
         * 설정된 값으로 MacroSeries 인스턴스 생성
         *
         * @return MacroSeries 인스턴스
         */
        fun build(): MacroSeries = MacroSeries(
            seriesId = seriesId,
            title = title,
            frequency = frequency,
            units = units,
            data = data
        )
    }
}
