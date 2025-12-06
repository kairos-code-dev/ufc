package com.ulalax.ufc.integration.fred

import com.ulalax.ufc.fixture.TestFixtures
import com.ulalax.ufc.integration.utils.FredApiKeyCondition
import com.ulalax.ufc.integration.utils.IntegrationTestBase
import com.ulalax.ufc.integration.utils.RecordingConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

/**
 * FredClient.series() API Integration 테스트
 *
 * 이 테스트는 실제 FRED API를 호출하여 시계열 데이터 조회 기능을 검증합니다.
 * API 가이드처럼 읽힐 수 있도록 @Nested 그룹핑 패턴을 사용합니다.
 *
 * ## 환경 설정
 * FRED API Key가 필요합니다. 환경변수에 `FRED_API_KEY`를 설정하세요.
 * ```bash
 * export FRED_API_KEY="your-api-key-here"
 * ```
 *
 * ## 테스트 실행 방법
 * ```bash
 * # 특정 클래스 실행
 * ./gradlew test --tests 'FredSeriesSpec'
 *
 * # 특정 그룹 실행
 * ./gradlew test --tests 'FredSeriesSpec$BasicBehavior'
 * ```
 *
 * ## 참고
 * - FRED API Key가 없으면 테스트가 스킵됩니다.
 * - FRED API는 120 requests/minute 제한이 있습니다.
 */
@DisplayName("[I] Fred.series() - FRED 시계열 데이터 조회")
@ExtendWith(FredApiKeyCondition::class)
class FredSeriesSpec : IntegrationTestBase() {
    @Nested
    @DisplayName("기본 동작")
    inner class BasicBehavior {
        @Test
        @DisplayName("GDP 시계열을 조회할 수 있다")
        fun `returns GDP series data`() =
            integrationTest(
                RecordingConfig.Paths.Fred.SERIES,
                "gdp_series",
            ) {
                // Given
                val seriesId = TestFixtures.FredSeries.GDP

                // When
                val result = ufc.fred!!.series(seriesId)

                // Then
                assertThat(result).isNotNull()
                assertThat(result.id).isEqualTo(seriesId)
                assertThat(result.title).contains("Gross Domestic Product")
                assertThat(result.observations).isNotEmpty()
            }

        @Test
        @DisplayName("실업률(UNRATE) 시계열을 조회할 수 있다")
        fun `returns unemployment rate series data`() =
            integrationTest {
                // Given
                val seriesId = TestFixtures.FredSeries.UNEMPLOYMENT

                // When
                val result = ufc.fred!!.series(seriesId)

                // Then
                assertThat(result).isNotNull()
                assertThat(result.id).isEqualTo(seriesId)
                assertThat(result.title).contains("Unemployment Rate")
                assertThat(result.observations).isNotEmpty()
            }
    }

    @Nested
    @DisplayName("응답 데이터 스펙")
    inner class ResponseSpec {
        @Test
        @DisplayName("시계열 정보는 ID, 제목, 주기, 단위를 포함한다")
        fun `series info contains id title frequency and units`() =
            integrationTest {
                // Given
                val seriesId = TestFixtures.FredSeries.GDP

                // When
                val result = ufc.fred!!.series(seriesId)

                // Then
                assertThat(result.id).isEqualTo(seriesId)
                assertThat(result.title).isNotEmpty()
                assertThat(result.frequency).isNotEmpty()
                assertThat(result.units).isNotEmpty()
            }

        @Test
        @DisplayName("관측값은 날짜와 값을 포함한다")
        fun `observations contain date and value`() =
            integrationTest {
                // Given
                val seriesId = TestFixtures.FredSeries.GDP

                // When
                val result = ufc.fred!!.series(seriesId)

                // Then
                assertThat(result.observations).isNotEmpty()

                val firstObservation = result.observations.first()
                assertThat(firstObservation.date).isNotNull()
                // value can be null for missing data
            }

        @Test
        @DisplayName("관측값은 날짜순으로 정렬되어 있다")
        fun `observations are sorted by date`() =
            integrationTest {
                // Given
                val seriesId = TestFixtures.FredSeries.UNEMPLOYMENT

                // When
                val result = ufc.fred!!.series(seriesId)

                // Then
                val dates = result.observations.map { it.date }
                assertThat(dates).isSorted()
                assertThat(dates.size).isGreaterThan(1)
                assertThat(dates.first()).isBefore(dates.last())
            }
    }

    @Nested
    @DisplayName("날짜 범위 조회")
    inner class DateRangeSpec {
        @Test
        @DisplayName("특정 날짜 범위로 데이터를 조회할 수 있다")
        fun `can fetch data with specific date range`() =
            integrationTest {
                // Given
                val seriesId = TestFixtures.FredSeries.GDP
                val startDate = LocalDate.of(2020, 1, 1)
                val endDate = LocalDate.of(2023, 12, 31)

                // When
                val result =
                    ufc.fred!!.series(
                        seriesId = seriesId,
                        startDate = startDate,
                        endDate = endDate,
                    )

                // Then
                assertThat(result).isNotNull()
                assertThat(result.observations).isNotEmpty()

                // 날짜 범위 내의 데이터만 조회되는지 확인
                result.observations.forEach { observation ->
                    assertThat(observation.date).isAfterOrEqualTo(startDate)
                    assertThat(observation.date).isBeforeOrEqualTo(endDate)
                }
            }

        @Test
        @DisplayName("최근 1년 데이터를 조회할 수 있다")
        fun `can fetch recent one year data`() =
            integrationTest {
                // Given
                val seriesId = TestFixtures.FredSeries.UNEMPLOYMENT
                val startDate = TestFixtures.Dates.ONE_YEAR_AGO
                val endDate = TestFixtures.Dates.TODAY

                // When
                val result =
                    ufc.fred!!.series(
                        seriesId = seriesId,
                        startDate = startDate,
                        endDate = endDate,
                    )

                // Then
                assertThat(result).isNotNull()
                assertThat(result.observations).isNotEmpty()
            }
    }

    @Nested
    @DisplayName("시계열 메타데이터 조회")
    inner class SeriesInfoSpec {
        @Test
        @DisplayName("시계열 메타데이터만 조회할 수 있다")
        fun `can fetch series metadata only`() =
            integrationTest(
                RecordingConfig.Paths.Fred.SERIES,
                "gdp_info",
            ) {
                // Given
                val seriesId = TestFixtures.FredSeries.GDP

                // When
                val result = ufc.fred!!.seriesInfo(seriesId)

                // Then
                assertThat(result).isNotNull()
                assertThat(result.id).isEqualTo(seriesId)
                assertThat(result.title).contains("Gross Domestic Product")
                assertThat(result.frequency).isNotEmpty()
            }
    }

    @Nested
    @DisplayName("주요 시계열 목록")
    inner class PopularSeries {
        @Test
        @DisplayName("GDP - 미국 국내총생산")
        fun `GDP series - US Gross Domestic Product`() =
            integrationTest {
                // GDP: 분기별 미국 국내총생산 (10억 달러)
                val result = ufc.fred!!.series("GDP")

                assertThat(result.id).isEqualTo("GDP")
                assertThat(result.title).contains("Gross Domestic Product")
                assertThat(result.frequency).isEqualTo("Quarterly")
                assertThat(result.units).contains("Billions")
            }

        @Test
        @DisplayName("UNRATE - 미국 실업률")
        fun `UNRATE series - US Unemployment Rate`() =
            integrationTest {
                // UNRATE: 월별 미국 실업률 (퍼센트)
                val result = ufc.fred!!.series("UNRATE")

                assertThat(result.id).isEqualTo("UNRATE")
                assertThat(result.title).contains("Unemployment Rate")
                assertThat(result.frequency).isEqualTo("Monthly")
                assertThat(result.units).contains("Percent")
            }

        @Test
        @DisplayName("FEDFUNDS - 연방기금금리")
        fun `FEDFUNDS series - Federal Funds Rate`() =
            integrationTest {
                // FEDFUNDS: 월별 연방기금 실효금리 (퍼센트)
                val result = ufc.fred!!.series("FEDFUNDS")

                assertThat(result.id).isEqualTo("FEDFUNDS")
                assertThat(result.title).contains("Federal Funds")
            }

        @Test
        @DisplayName("DGS10 - 10년물 국채 수익률")
        fun `DGS10 series - 10 Year Treasury Rate`() =
            integrationTest {
                // DGS10: 일별 10년 만기 국채 수익률 (퍼센트)
                val result = ufc.fred!!.series("DGS10")

                assertThat(result.id).isEqualTo("DGS10")
                assertThat(result.title).contains("10-Year")
            }

        @Test
        @DisplayName("CPIAUCSL - 소비자물가지수 (인플레이션)")
        fun `CPIAUCSL series - Consumer Price Index`() =
            integrationTest {
                // CPIAUCSL: 월별 도시 소비자 물가지수
                val result = ufc.fred!!.series("CPIAUCSL")

                assertThat(result.id).isEqualTo("CPIAUCSL")
                assertThat(result.title).contains("Consumer Price Index")
            }
    }

    @Nested
    @DisplayName("데이터 접근 방법")
    inner class DataAccessExamples {
        @Test
        @DisplayName("시계열 메타데이터 접근: id, title, frequency, units")
        fun `can access series metadata`() =
            integrationTest {
                val result = ufc.fred!!.series("GDP")

                // 시계열 ID
                val seriesId = result.id
                assertThat(seriesId).isEqualTo("GDP")

                // 제목
                val title = result.title
                assertThat(title).isNotBlank()

                // 주기 (Quarterly, Monthly, Daily 등)
                val frequency = result.frequency
                assertThat(frequency).isNotBlank()

                // 단위 (Percent, Billions of Dollars 등)
                val units = result.units
                assertThat(units).isNotBlank()
            }

        @Test
        @DisplayName("관측값에서 날짜와 값 추출")
        fun `can extract date and value from observations`() =
            integrationTest {
                val result = ufc.fred!!.series("UNRATE")

                // 최신 관측값
                val latestObservation = result.observations.last()

                // 날짜 (LocalDate)
                val date = latestObservation.date
                assertThat(date).isNotNull()

                // 값 (Double, null일 수 있음 - 미발표 데이터)
                val value = latestObservation.value
                // value가 null이 아니면 검증
                value?.let {
                    assertThat(it).isGreaterThanOrEqualTo(0.0)
                }
            }

        @Test
        @DisplayName("날짜 범위로 필터링하여 조회")
        fun `can filter by date range`() =
            integrationTest {
                val startDate = LocalDate.of(2023, 1, 1)
                val endDate = LocalDate.of(2023, 12, 31)

                val result =
                    ufc.fred!!.series(
                        seriesId = "UNRATE",
                        startDate = startDate,
                        endDate = endDate,
                    )

                // 모든 관측값이 범위 내에 있는지 확인
                result.observations.forEach { obs ->
                    assertThat(obs.date).isAfterOrEqualTo(startDate)
                    assertThat(obs.date).isBeforeOrEqualTo(endDate)
                }
            }
    }

    @Nested
    @DisplayName("활용 예제")
    inner class UsageExamples {
        @Test
        @DisplayName("연방기금금리(Fed Funds Rate)를 조회할 수 있다")
        fun `can fetch federal funds rate`() =
            integrationTest {
                // Given
                val seriesId = TestFixtures.FredSeries.FED_FUNDS_RATE

                // When
                val result = ufc.fred!!.series(seriesId)

                // Then
                assertThat(result).isNotNull()
                assertThat(result.id).isEqualTo(seriesId)
                assertThat(result.observations).isNotEmpty()
            }

        @Test
        @DisplayName("10년물 국채 수익률을 조회할 수 있다")
        fun `can fetch 10 year treasury rate`() =
            integrationTest {
                // Given
                val seriesId = TestFixtures.FredSeries.TREASURY_10Y

                // When
                val result = ufc.fred!!.series(seriesId)

                // Then
                assertThat(result).isNotNull()
                assertThat(result.id).isEqualTo(seriesId)
                assertThat(result.observations).isNotEmpty()
            }
    }

    @Nested
    @DisplayName("에러 케이스")
    inner class ErrorCases {
        @Test
        @DisplayName("존재하지 않는 시계열 조회 시 예외를 던진다")
        fun `throws exception for invalid series id`() =
            integrationTest {
                // Given
                val invalidSeriesId = TestFixtures.FredSeries.INVALID

                // When & Then
                val result =
                    runCatching {
                        ufc.fred!!.series(invalidSeriesId)
                    }
                // Then
                assertThat(result.isFailure).isTrue()
            }
    }
}
