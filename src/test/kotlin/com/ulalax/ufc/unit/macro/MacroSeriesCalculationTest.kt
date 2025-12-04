package com.ulalax.ufc.unit.macro

import com.ulalax.ufc.fixtures.MacroSeriesFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * MacroSeries - 거시경제 시계열 계산 테스트
 *
 * MacroSeries 클래스의 순수 계산 함수들에 대한 단위 테스트입니다.
 * Given-When-Then 패턴을 따르며, MacroSeriesFixtures를 활용합니다.
 */
@DisplayName("MacroSeries - 거시경제 시계열 계산")
class MacroSeriesCalculationTest {

    @Nested
    @DisplayName("size() - 데이터 포인트 개수")
    inner class Size {

        @Test
        @DisplayName("데이터 포인트 개수를 정확히 반환한다")
        fun shouldReturnCorrectSize() {
            // Given: 5개 데이터 포인트를 가진 시계열
            val series = MacroSeriesFixtures.gdpRealQuarterly()

            // When: 크기 확인
            val size = series.size()

            // Then: 5
            assertThat(size)
                .withFailMessage("데이터 포인트 개수는 5여야 합니다. 실제값: $size")
                .isEqualTo(5)
        }

        @Test
        @DisplayName("빈 시계열은 0을 반환한다")
        fun shouldReturnZeroForEmptySeries() {
            // Given: 빈 시계열
            val series = MacroSeriesFixtures.emptyDataSeries()

            // When: 크기 확인
            val size = series.size()

            // Then: 0
            assertThat(size)
                .withFailMessage("빈 시계열의 크기는 0이어야 합니다. 실제값: $size")
                .isEqualTo(0)
        }

        @Test
        @DisplayName("단일 데이터 포인트는 1을 반환한다")
        fun shouldReturnOneForSingleDataPoint() {
            // Given: 1개 데이터 포인트
            val series = MacroSeriesFixtures.singleDataPointSeries()

            // When: 크기 확인
            val size = series.size()

            // Then: 1
            assertThat(size)
                .withFailMessage("단일 데이터 포인트의 크기는 1이어야 합니다. 실제값: $size")
                .isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("first() - 첫 번째 데이터 포인트")
    inner class First {

        @Test
        @DisplayName("첫 번째 데이터 포인트를 반환한다")
        fun shouldReturnFirstDataPoint() {
            // Given: GDP 시계열
            val series = MacroSeriesFixtures.gdpRealQuarterly()

            // When: 첫 번째 데이터 포인트 조회
            val first = series.first()

            // Then: 2023-01-01 데이터
            assertThat(first)
                .withFailMessage("첫 번째 데이터 포인트가 있어야 합니다")
                .isNotNull

            assertThat(first!!.date)
                .withFailMessage("첫 번째 날짜는 2023-01-01이어야 합니다")
                .isEqualTo("2023-01-01")

            assertThat(first.value)
                .withFailMessage("첫 번째 값은 22000.0이어야 합니다")
                .isEqualTo(22000.0)
        }

        @Test
        @DisplayName("빈 시계열은 null을 반환한다")
        fun shouldReturnNullForEmptySeries() {
            // Given: 빈 시계열
            val series = MacroSeriesFixtures.emptyDataSeries()

            // When: 첫 번째 데이터 포인트 조회
            val first = series.first()

            // Then: null
            assertThat(first)
                .withFailMessage("빈 시계열의 first()는 null이어야 합니다")
                .isNull()
        }

        @Test
        @DisplayName("단일 데이터 포인트는 그 값을 반환한다")
        fun shouldReturnSingleDataPoint() {
            // Given: 1개 데이터 포인트
            val series = MacroSeriesFixtures.singleDataPointSeries()

            // When: 첫 번째 데이터 포인트 조회
            val first = series.first()

            // Then: 해당 데이터 포인트
            assertThat(first)
                .withFailMessage("데이터 포인트가 있어야 합니다")
                .isNotNull

            assertThat(first!!.date)
                .withFailMessage("날짜가 일치해야 합니다")
                .isEqualTo("2024-01-01")
        }
    }

    @Nested
    @DisplayName("last() - 마지막 데이터 포인트")
    inner class Last {

        @Test
        @DisplayName("마지막 데이터 포인트를 반환한다")
        fun shouldReturnLastDataPoint() {
            // Given: GDP 시계열
            val series = MacroSeriesFixtures.gdpRealQuarterly()

            // When: 마지막 데이터 포인트 조회
            val last = series.last()

            // Then: 2024-01-01 데이터
            assertThat(last)
                .withFailMessage("마지막 데이터 포인트가 있어야 합니다")
                .isNotNull

            assertThat(last!!.date)
                .withFailMessage("마지막 날짜는 2024-01-01이어야 합니다")
                .isEqualTo("2024-01-01")

            assertThat(last.value)
                .withFailMessage("마지막 값은 22800.0이어야 합니다")
                .isEqualTo(22800.0)
        }

        @Test
        @DisplayName("빈 시계열은 null을 반환한다")
        fun shouldReturnNullForEmptySeries() {
            // Given: 빈 시계열
            val series = MacroSeriesFixtures.emptyDataSeries()

            // When: 마지막 데이터 포인트 조회
            val last = series.last()

            // Then: null
            assertThat(last)
                .withFailMessage("빈 시계열의 last()는 null이어야 합니다")
                .isNull()
        }

        @Test
        @DisplayName("단일 데이터 포인트는 그 값을 반환한다")
        fun shouldReturnSingleDataPoint() {
            // Given: 1개 데이터 포인트
            val series = MacroSeriesFixtures.singleDataPointSeries()

            // When: 마지막 데이터 포인트 조회
            val last = series.last()

            // Then: 해당 데이터 포인트 (first()와 동일)
            assertThat(last)
                .withFailMessage("데이터 포인트가 있어야 합니다")
                .isNotNull

            assertThat(last!!.date)
                .withFailMessage("날짜가 일치해야 합니다")
                .isEqualTo("2024-01-01")
        }
    }

    @Nested
    @DisplayName("filterNonNull() - 결측치 필터링")
    inner class FilterNonNull {

        @Test
        @DisplayName("결측치가 없는 데이터는 그대로 반환한다")
        fun shouldReturnSameForNoMissingValues() {
            // Given: 결측치 없는 시계열
            val series = MacroSeriesFixtures.cpiMonthly()

            // When: 결측치 필터링
            val filtered = series.filterNonNull()

            // Then: 데이터 개수 동일
            assertThat(filtered.size())
                .withFailMessage("결측치가 없으면 크기가 동일해야 합니다")
                .isEqualTo(series.size())

            assertThat(filtered.data)
                .withFailMessage("모든 데이터가 유지되어야 합니다")
                .hasSize(5)
        }

        @Test
        @DisplayName("결측치를 제거하고 유효한 데이터만 반환한다")
        fun shouldRemoveMissingValues() {
            // Given: 결측치 포함 시계열 (5개 중 2개가 null)
            val series = MacroSeriesFixtures.seriesWithMissingValues()

            // When: 결측치 필터링
            val filtered = series.filterNonNull()

            // Then: 3개만 남음
            assertThat(filtered.size())
                .withFailMessage("결측치를 제거하면 3개가 되어야 합니다. 실제: ${filtered.size()}")
                .isEqualTo(3)

            // 모든 값이 non-null
            filtered.data.forEach { dataPoint ->
                assertThat(dataPoint.value)
                    .withFailMessage("필터링 후 모든 값은 non-null이어야 합니다")
                    .isNotNull()
            }

            // 날짜 확인
            assertThat(filtered.data[0].date).isEqualTo("2024-01-01")
            assertThat(filtered.data[1].date).isEqualTo("2024-03-01")
            assertThat(filtered.data[2].date).isEqualTo("2024-05-01")
        }

        @Test
        @DisplayName("모든 데이터가 결측치이면 빈 시계열을 반환한다")
        fun shouldReturnEmptyForAllMissingValues() {
            // Given: 모든 값이 null인 시계열
            val series = MacroSeriesFixtures.builder()
                .withData(
                    "2024-01-01" to null,
                    "2024-02-01" to null,
                    "2024-03-01" to null
                )
                .build()

            // When: 결측치 필터링
            val filtered = series.filterNonNull()

            // Then: 빈 시계열
            assertThat(filtered.size())
                .withFailMessage("모든 값이 null이면 빈 시계열이 되어야 합니다")
                .isEqualTo(0)
        }

        @Test
        @DisplayName("시계열 메타데이터는 유지된다")
        fun shouldPreserveMetadata() {
            // Given: 결측치 포함 시계열
            val series = MacroSeriesFixtures.seriesWithMissingValues()

            // When: 결측치 필터링
            val filtered = series.filterNonNull()

            // Then: 메타데이터 동일
            assertThat(filtered.seriesId).isEqualTo(series.seriesId)
            assertThat(filtered.title).isEqualTo(series.title)
            assertThat(filtered.frequency).isEqualTo(series.frequency)
            assertThat(filtered.units).isEqualTo(series.units)
        }
    }

    @Nested
    @DisplayName("filterByDateRange() - 날짜 범위 필터링")
    inner class FilterByDateRange {

        @Test
        @DisplayName("지정된 날짜 범위의 데이터만 반환한다")
        fun shouldReturnDataInDateRange() {
            // Given: GDP 시계열 (2023-01 ~ 2024-01)
            val series = MacroSeriesFixtures.gdpRealQuarterly()

            // When: 2023-04 ~ 2023-10 범위 필터링
            val filtered = series.filterByDateRange("2023-04-01", "2023-10-01")

            // Then: 3개 데이터 (2023-04, 2023-07, 2023-10)
            assertThat(filtered.size())
                .withFailMessage("날짜 범위 내 데이터는 3개여야 합니다. 실제: ${filtered.size()}")
                .isEqualTo(3)

            assertThat(filtered.first()!!.date).isEqualTo("2023-04-01")
            assertThat(filtered.last()!!.date).isEqualTo("2023-10-01")
        }

        @Test
        @DisplayName("범위 밖의 데이터는 제외된다")
        fun shouldExcludeDataOutsideRange() {
            // Given: CPI 시계열 (2024-01 ~ 2024-05)
            val series = MacroSeriesFixtures.cpiMonthly()

            // When: 2024-02 ~ 2024-04 범위 필터링
            val filtered = series.filterByDateRange("2024-02-01", "2024-04-01")

            // Then: 3개만 포함 (01월, 05월 제외)
            assertThat(filtered.size())
                .withFailMessage("범위 내 데이터는 3개여야 합니다")
                .isEqualTo(3)

            // 01월과 05월 제외 확인
            assertThat(filtered.data).noneMatch { it.date == "2024-01-01" }
            assertThat(filtered.data).noneMatch { it.date == "2024-05-01" }
        }

        @Test
        @DisplayName("범위가 데이터 전체를 포함하면 모든 데이터를 반환한다")
        fun shouldReturnAllDataWhenRangeCoversAll() {
            // Given: CPI 시계열 (2024-01 ~ 2024-05)
            val series = MacroSeriesFixtures.cpiMonthly()

            // When: 더 넓은 범위로 필터링
            val filtered = series.filterByDateRange("2023-01-01", "2025-01-01")

            // Then: 모든 데이터 포함
            assertThat(filtered.size())
                .withFailMessage("넓은 범위는 모든 데이터를 포함해야 합니다")
                .isEqualTo(series.size())
        }

        @Test
        @DisplayName("범위와 겹치는 데이터가 없으면 빈 시계열을 반환한다")
        fun shouldReturnEmptyWhenNoDataInRange() {
            // Given: CPI 시계열 (2024-01 ~ 2024-05)
            val series = MacroSeriesFixtures.cpiMonthly()

            // When: 데이터가 없는 범위로 필터링
            val filtered = series.filterByDateRange("2025-01-01", "2025-12-31")

            // Then: 빈 시계열
            assertThat(filtered.size())
                .withFailMessage("범위와 겹치는 데이터가 없으면 빈 시계열이어야 합니다")
                .isEqualTo(0)
        }

        @Test
        @DisplayName("시작일과 종료일이 같으면 해당 날짜의 데이터만 반환한다")
        fun shouldReturnSingleDateWhenStartEqualsEnd() {
            // Given: GDP 시계열
            val series = MacroSeriesFixtures.gdpRealQuarterly()

            // When: 동일한 시작일과 종료일
            val filtered = series.filterByDateRange("2023-04-01", "2023-04-01")

            // Then: 1개 데이터
            assertThat(filtered.size())
                .withFailMessage("동일 날짜는 1개 데이터만 반환해야 합니다")
                .isEqualTo(1)

            assertThat(filtered.first()!!.date).isEqualTo("2023-04-01")
        }

        @Test
        @DisplayName("시계열 메타데이터는 유지된다")
        fun shouldPreserveMetadata() {
            // Given: GDP 시계열
            val series = MacroSeriesFixtures.gdpRealQuarterly()

            // When: 날짜 범위 필터링
            val filtered = series.filterByDateRange("2023-04-01", "2023-10-01")

            // Then: 메타데이터 동일
            assertThat(filtered.seriesId).isEqualTo(series.seriesId)
            assertThat(filtered.title).isEqualTo(series.title)
            assertThat(filtered.frequency).isEqualTo(series.frequency)
            assertThat(filtered.units).isEqualTo(series.units)
        }
    }

    @Nested
    @DisplayName("MacroDataPoint - 데이터 포인트 메서드")
    inner class MacroDataPointMethods {

        @Nested
        @DisplayName("isValid() - 유효한 데이터 확인")
        inner class IsValid {

            @Test
            @DisplayName("값이 있으면 true를 반환한다")
            fun shouldReturnTrueWhenValueExists() {
                // Given: 값이 있는 데이터 포인트
                val dataPoint = MacroSeriesFixtures.cpiMonthly().first()!!

                // When: 유효성 확인
                val isValid = dataPoint.isValid()

                // Then: true
                assertThat(isValid)
                    .withFailMessage("값이 있으면 유효해야 합니다")
                    .isTrue()
            }

            @Test
            @DisplayName("값이 null이면 false를 반환한다")
            fun shouldReturnFalseWhenValueIsNull() {
                // Given: 값이 null인 데이터 포인트
                val dataPoint = MacroSeriesFixtures.seriesWithMissingValues().data[1]

                // When: 유효성 확인
                val isValid = dataPoint.isValid()

                // Then: false
                assertThat(isValid)
                    .withFailMessage("값이 null이면 유효하지 않아야 합니다")
                    .isFalse()
            }
        }

        @Nested
        @DisplayName("isMissing() - 결측치 확인")
        inner class IsMissing {

            @Test
            @DisplayName("값이 null이면 true를 반환한다")
            fun shouldReturnTrueWhenValueIsNull() {
                // Given: 값이 null인 데이터 포인트
                val dataPoint = MacroSeriesFixtures.seriesWithMissingValues().data[1]

                // When: 결측치 확인
                val isMissing = dataPoint.isMissing()

                // Then: true
                assertThat(isMissing)
                    .withFailMessage("값이 null이면 결측치여야 합니다")
                    .isTrue()
            }

            @Test
            @DisplayName("값이 있으면 false를 반환한다")
            fun shouldReturnFalseWhenValueExists() {
                // Given: 값이 있는 데이터 포인트
                val dataPoint = MacroSeriesFixtures.cpiMonthly().first()!!

                // When: 결측치 확인
                val isMissing = dataPoint.isMissing()

                // Then: false
                assertThat(isMissing)
                    .withFailMessage("값이 있으면 결측치가 아니어야 합니다")
                    .isFalse()
            }

            @Test
            @DisplayName("isValid()와 isMissing()은 상호 배타적이다")
            fun shouldBeMutuallyExclusive() {
                // Given: 여러 데이터 포인트
                val series = MacroSeriesFixtures.seriesWithMissingValues()

                // When & Then: 모든 데이터 포인트가 valid 또는 missing 중 하나
                series.data.forEach { dataPoint ->
                    val isValid = dataPoint.isValid()
                    val isMissing = dataPoint.isMissing()

                    assertThat(isValid)
                        .withFailMessage("isValid와 isMissing은 상호 배타적이어야 합니다")
                        .isNotEqualTo(isMissing)
                }
            }
        }
    }
}
