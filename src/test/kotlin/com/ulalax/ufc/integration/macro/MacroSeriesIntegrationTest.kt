package com.ulalax.ufc.integration.macro

import com.ulalax.ufc.api.client.Ufc
import com.ulalax.ufc.api.client.UfcClientConfig
import com.ulalax.ufc.api.config.AppConfig
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.domain.macro.*
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitingSettings
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

/**
 * MacroService - 거시경제 지표 조회 통합 테스트
 *
 * 이 테스트는 실제 FRED API를 호출합니다.
 * Mock이나 Fake 객체를 사용하지 않고, 실제 네트워크 요청을 통해
 * ufc.macro 메서드의 모든 시나리오를 검증합니다.
 *
 * ## 테스트 환경
 * - 실제 FRED API 호출
 * - FRED API 키 필요 (local.properties 또는 환경 변수 FRED_API_KEY)
 * - 네트워크 연결 필요
 * - API Rate Limiting 적용
 *
 * ## 실행 방법
 * 1. local.properties 파일에 FRED_API_KEY 설정 (권장):
 *    ```
 *    FRED_API_KEY=your_api_key
 *    ```
 * 2. 또는 환경 변수로 설정:
 *    ```bash
 *    export FRED_API_KEY=your_api_key
 *    ./gradlew test --tests "*MacroSeriesIntegrationTest"
 *    ```
 *
 * ## 주의사항
 * - FRED API 키가 없으면 테스트 자동 스킵
 * - 네트워크 상태에 따라 테스트 실패 가능
 * - API 변경으로 인한 실패 가능
 * - Rate Limiting으로 인한 지연 발생 가능
 */
@Tag("integration")
@DisplayName("Macro Service - 거시경제 지표 조회 통합 테스트")
class MacroSeriesIntegrationTest {

    private lateinit var ufc: Ufc
    // 환경 변수 우선, 없으면 local.properties에서 조회
    private val fredApiKey: String? = System.getenv("FRED_API_KEY") ?: AppConfig.get("FRED_API_KEY")

    @BeforeEach
    fun setUp() = runBlocking {
        // FRED API 키가 없으면 테스트 스킵
        Assumptions.assumeTrue(fredApiKey != null, "FRED_API_KEY not found in local.properties or environment variable")

        ufc = Ufc.create(
            UfcClientConfig(
                fredApiKey = fredApiKey,
                rateLimitingSettings = RateLimitingSettings()
            )
        )
    }

    @AfterEach
    fun tearDown() {
        if (::ufc.isInitialized) {
            ufc.close()
        }
    }

    @Nested
    @DisplayName("getSeries() - 범용 시계열 조회")
    inner class GetSeries {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("유효한 시리즈 ID로 조회 시 데이터를 반환한다")
            fun shouldReturnSeriesDataForValidId() = runTest {
                // Given: 실질 GDP 시리즈 ID (GDPC1)

                // When
                val series = ufc.macro!!.getSeries("GDPC1")

                // Then: 시계열 데이터 반환
                assertThat(series.seriesId)
                    .withFailMessage("시리즈 ID는 'GDPC1'이어야 합니다")
                    .isEqualTo("GDPC1")

                assertThat(series.title)
                    .withFailMessage("제목이 있어야 합니다")
                    .isNotBlank()

                assertThat(series.frequency)
                    .withFailMessage("주기 정보가 있어야 합니다")
                    .isNotBlank()

                assertThat(series.units)
                    .withFailMessage("단위 정보가 있어야 합니다")
                    .isNotBlank()

                assertThat(series.data)
                    .withFailMessage("데이터 포인트가 있어야 합니다")
                    .isNotEmpty()
            }

            @Test
            @DisplayName("실업률 시계열을 조회할 수 있다")
            fun shouldReturnUnemploymentRateSeries() = runTest {
                // Given: 실업률 시리즈 ID (UNRATE)

                // When
                val series = ufc.macro!!.getSeries("UNRATE")

                // Then
                assertThat(series.seriesId).isEqualTo("UNRATE")
                assertThat(series.frequency).isEqualToIgnoringCase("Monthly")
                assertThat(series.data).isNotEmpty()

                // 실업률은 퍼센트 단위
                assertThat(series.units).containsIgnoringCase("Percent")
            }

            @Test
            @DisplayName("시작일을 지정하여 조회할 수 있다")
            fun shouldReturnSeriesFromStartDate() = runTest {
                // Given: GDP, 시작일 지정

                // When
                val series = ufc.macro!!.getSeries("GDPC1", startDate = "2020-01-01")

                // Then: 2020년 이후 데이터만
                assertThat(series.data)
                    .withFailMessage("2020년 이후 데이터가 있어야 합니다")
                    .isNotEmpty()

                series.data.forEach { dataPoint ->
                    assertThat(dataPoint.date)
                        .withFailMessage("날짜는 2020-01-01 이후여야 합니다")
                        .isGreaterThanOrEqualTo("2020-01-01")
                }
            }

            @Test
            @DisplayName("종료일을 지정하여 조회할 수 있다")
            fun shouldReturnSeriesUntilEndDate() = runTest {
                // Given: GDP, 종료일 지정

                // When
                val series = ufc.macro!!.getSeries("GDPC1", endDate = "2023-12-31")

                // Then: 2023년 이전 데이터만
                assertThat(series.data).isNotEmpty()

                series.data.forEach { dataPoint ->
                    assertThat(dataPoint.date)
                        .withFailMessage("날짜는 2023-12-31 이전이어야 합니다")
                        .isLessThanOrEqualTo("2023-12-31")
                }
            }

            @Test
            @DisplayName("시작일과 종료일을 모두 지정하여 조회할 수 있다")
            fun shouldReturnSeriesInDateRange() = runTest {
                // Given: GDP, 날짜 범위 지정

                // When
                val series = ufc.macro!!.getSeries(
                    "GDPC1",
                    startDate = "2020-01-01",
                    endDate = "2023-12-31"
                )

                // Then: 지정된 범위의 데이터만
                assertThat(series.data).isNotEmpty()

                series.data.forEach { dataPoint ->
                    assertThat(dataPoint.date)
                        .withFailMessage("날짜는 지정된 범위 내여야 합니다")
                        .isBetween("2020-01-01", "2023-12-31")
                }
            }
        }

        @Nested
        @DisplayName("입력 검증")
        inner class InputValidation {

            @Test
            @DisplayName("빈 문자열 시리즈 ID 조회 시 에러를 발생시킨다")
            fun shouldThrowErrorForBlankSeriesId() = runTest {
                // When & Then
                val exception = try {
                    ufc.macro!!.getSeries("")
                    null
                } catch (e: UfcException) {
                    e
                } catch (e: IllegalArgumentException) {
                    // 입력 검증에서 바로 예외 발생할 수 있음
                    UfcException(ErrorCode.INVALID_SYMBOL, e.message ?: "Invalid series ID", cause = e)
                }

                assertThat(exception)
                    .withFailMessage("빈 문자열 시리즈 ID 조회 시 예외가 발생해야 합니다")
                    .isNotNull()
            }
        }

        @Nested
        @DisplayName("에러 처리")
        inner class ErrorHandling {

            @Test
            @DisplayName("존재하지 않는 시리즈 ID 조회 시 에러를 발생시킨다")
            fun shouldThrowErrorForNonExistentSeriesId() = runTest {
                // Given: 존재하지 않는 시리즈 ID
                val invalidSeriesId = "INVALID_SERIES_ID_12345"

                // When & Then
                val exception = try {
                    ufc.macro!!.getSeries(invalidSeriesId)
                    null
                } catch (e: UfcException) {
                    e
                }

                assertThat(exception)
                    .withFailMessage("존재하지 않는 시리즈 ID 조회 시 UfcException이 발생해야 합니다")
                    .isNotNull()

                // FRED API는 다양한 에러 코드를 반환할 수 있음
                assertThat(exception!!.errorCode)
                    .withFailMessage("에러 코드가 유효한 범위 내에 있어야 합니다: ${exception.errorCode}")
                    .isIn(
                        ErrorCode.DATA_NOT_FOUND,
                        ErrorCode.EXTERNAL_API_ERROR,
                        ErrorCode.NETWORK_ERROR,
                        ErrorCode.DATA_RETRIEVAL_ERROR,
                        ErrorCode.INVALID_PARAMETER
                    )
            }
        }

        @Nested
        @DisplayName("데이터 무결성")
        inner class DataIntegrity {

            @Test
            @DisplayName("모든 데이터 포인트에 날짜가 있다")
            fun shouldHaveDateForAllDataPoints() = runTest {
                // Given: GDP 시계열

                // When
                val series = ufc.macro!!.getSeries("GDPC1")

                // Then: 모든 데이터 포인트가 날짜를 가짐
                series.data.forEach { dataPoint ->
                    assertThat(dataPoint.date)
                        .withFailMessage("모든 데이터 포인트는 날짜가 있어야 합니다")
                        .isNotBlank()
                        .matches("\\d{4}-\\d{2}-\\d{2}")
                }
            }

            @Test
            @DisplayName("데이터 포인트는 날짜 순서로 정렬되어 있다")
            fun shouldBeSortedByDate() = runTest {
                // Given: CPI 시계열

                // When
                val series = ufc.macro!!.getSeries("CPIAUCSL")

                // Then: 날짜 오름차순 정렬
                if (series.data.size > 1) {
                    for (i in 0 until series.data.size - 1) {
                        assertThat(series.data[i + 1].date)
                            .withFailMessage("데이터는 날짜 오름차순이어야 합니다")
                            .isGreaterThanOrEqualTo(series.data[i].date)
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("getGDP() - GDP 지표 조회")
    inner class GetGDP {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("실질 GDP를 조회할 수 있다")
            fun shouldReturnRealGDP() = runTest {
                // Given: REAL 타입

                // When
                val series = ufc.macro!!.getGDP(GDPType.REAL)

                // Then: 실질 GDP 데이터
                assertThat(series.seriesId)
                    .withFailMessage("실질 GDP 시리즈 ID여야 합니다")
                    .isEqualTo("GDPC1")

                assertThat(series.data).isNotEmpty()
            }

            @Test
            @DisplayName("명목 GDP를 조회할 수 있다")
            fun shouldReturnNominalGDP() = runTest {
                // Given: NOMINAL 타입

                // When
                val series = ufc.macro!!.getGDP(GDPType.NOMINAL)

                // Then: 명목 GDP 데이터
                assertThat(series.seriesId)
                    .withFailMessage("명목 GDP 시리즈 ID여야 합니다")
                    .isEqualTo("GDP")

                assertThat(series.data).isNotEmpty()
            }

            @Test
            @DisplayName("GDP 성장률을 조회할 수 있다")
            fun shouldReturnGDPGrowth() = runTest {
                // Given: GROWTH 타입

                // When
                val series = ufc.macro!!.getGDP(GDPType.GROWTH)

                // Then: GDP 성장률 데이터
                assertThat(series.data).isNotEmpty()

                // 성장률은 퍼센트 단위
                assertThat(series.units).containsIgnoringCase("Percent")
            }

            @Test
            @DisplayName("잠재 GDP를 조회할 수 있다")
            fun shouldReturnPotentialGDP() = runTest {
                // Given: POTENTIAL 타입

                // When
                val series = ufc.macro!!.getGDP(GDPType.POTENTIAL)

                // Then: 잠재 GDP 데이터
                assertThat(series.data).isNotEmpty()
            }
        }
    }

    @Nested
    @DisplayName("getInflation() - 인플레이션 지표 조회")
    inner class GetInflation {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("CPI All Items를 조회할 수 있다")
            fun shouldReturnCPIAll() = runTest {
                // Given: CPI_ALL 타입

                // When
                val series = ufc.macro!!.getInflation(InflationType.CPI_ALL)

                // Then: CPI 전체 데이터
                assertThat(series.seriesId)
                    .withFailMessage("CPI All 시리즈 ID여야 합니다")
                    .isEqualTo("CPIAUCSL")

                assertThat(series.data).isNotEmpty()
            }

            @Test
            @DisplayName("CPI Core를 조회할 수 있다")
            fun shouldReturnCPICore() = runTest {
                // Given: CPI_CORE 타입

                // When
                val series = ufc.macro!!.getInflation(InflationType.CPI_CORE)

                // Then: CPI 핵심 데이터
                assertThat(series.data).isNotEmpty()
            }

            @Test
            @DisplayName("PCE를 조회할 수 있다")
            fun shouldReturnPCE() = runTest {
                // Given: PCE_ALL 타입

                // When
                val series = ufc.macro!!.getInflation(InflationType.PCE_ALL)

                // Then: PCE 데이터
                assertThat(series.data).isNotEmpty()
            }

            @Test
            @DisplayName("PPI를 조회할 수 있다")
            fun shouldReturnPPI() = runTest {
                // Given: PPI 타입

                // When
                val series = ufc.macro!!.getInflation(InflationType.PPI)

                // Then: PPI 데이터
                assertThat(series.data).isNotEmpty()
            }
        }
    }

    @Nested
    @DisplayName("getUnemployment() - 실업률 및 고용 지표 조회")
    inner class GetUnemployment {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("실업률을 조회할 수 있다")
            fun shouldReturnUnemploymentRate() = runTest {
                // Given: RATE 타입

                // When
                val series = ufc.macro!!.getUnemployment(UnemploymentType.RATE)

                // Then: 실업률 데이터
                assertThat(series.seriesId)
                    .withFailMessage("실업률 시리즈 ID여야 합니다")
                    .isEqualTo("UNRATE")

                assertThat(series.data).isNotEmpty()

                // 실업률은 퍼센트 단위
                assertThat(series.units).containsIgnoringCase("Percent")
            }

            @Test
            @DisplayName("신규 실업수당 청구 건수를 조회할 수 있다")
            fun shouldReturnInitialClaims() = runTest {
                // Given: INITIAL_CLAIMS 타입

                // When
                val series = ufc.macro!!.getUnemployment(UnemploymentType.INITIAL_CLAIMS)

                // Then: 신규 청구 건수 데이터
                assertThat(series.data).isNotEmpty()
            }

            @Test
            @DisplayName("계속 실업수당 청구 건수를 조회할 수 있다")
            fun shouldReturnContinuingClaims() = runTest {
                // Given: CONTINUING_CLAIMS 타입

                // When
                val series = ufc.macro!!.getUnemployment(UnemploymentType.CONTINUING_CLAIMS)

                // Then: 계속 청구 건수 데이터
                assertThat(series.data).isNotEmpty()
            }

            @Test
            @DisplayName("비농업 고용을 조회할 수 있다")
            fun shouldReturnNonfarmPayrolls() = runTest {
                // Given: NONFARM_PAYROLLS 타입

                // When
                val series = ufc.macro!!.getUnemployment(UnemploymentType.NONFARM_PAYROLLS)

                // Then: 비농업 고용 데이터
                assertThat(series.data).isNotEmpty()
            }
        }
    }

    @Nested
    @DisplayName("getInterestRate() - 금리 지표 조회")
    inner class GetInterestRate {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("연방기금금리를 조회할 수 있다")
            fun shouldReturnFederalFundsRate() = runTest {
                // Given: FEDERAL_FUNDS 타입

                // When
                val series = ufc.macro!!.getInterestRate(InterestRateType.FEDERAL_FUNDS)

                // Then: 연방기금금리 데이터
                assertThat(series.seriesId)
                    .withFailMessage("연방기금금리 시리즈 ID여야 합니다")
                    .isEqualTo("FEDFUNDS")

                assertThat(series.data).isNotEmpty()

                // 금리는 퍼센트 단위
                assertThat(series.units).containsIgnoringCase("Percent")
            }

            @Test
            @DisplayName("10년물 국채 수익률을 조회할 수 있다")
            fun shouldReturnTreasury10Y() = runTest {
                // Given: TREASURY_10Y 타입

                // When
                val series = ufc.macro!!.getInterestRate(InterestRateType.TREASURY_10Y)

                // Then: 10년물 국채 데이터
                assertThat(series.data).isNotEmpty()
            }

            @Test
            @DisplayName("2년물 국채 수익률을 조회할 수 있다")
            fun shouldReturnTreasury2Y() = runTest {
                // Given: TREASURY_2Y 타입

                // When
                val series = ufc.macro!!.getInterestRate(InterestRateType.TREASURY_2Y)

                // Then: 2년물 국채 데이터
                assertThat(series.data).isNotEmpty()
            }

            @Test
            @DisplayName("30년 고정 모기지 금리를 조회할 수 있다")
            fun shouldReturnMortgage30Y() = runTest {
                // Given: MORTGAGE_30Y 타입

                // When
                val series = ufc.macro!!.getInterestRate(InterestRateType.MORTGAGE_30Y)

                // Then: 30년 모기지 금리 데이터
                assertThat(series.data).isNotEmpty()
            }
        }
    }
}
