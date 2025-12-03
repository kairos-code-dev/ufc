package com.ulalax.ufc.integration

import com.ulalax.ufc.api.client.UFC
import com.ulalax.ufc.api.client.UFCClientConfig
import com.ulalax.ufc.domain.macro.GDPType
import com.ulalax.ufc.domain.macro.InflationType
import com.ulalax.ufc.domain.macro.UnemploymentType
import com.ulalax.ufc.domain.macro.InterestRateType
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitingSettings
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

/**
 * Macro 도메인 통합 테스트
 *
 * 실제 FRED API를 호출하여 거시경제 지표 조회를 검증합니다.
 *
 * 특징:
 * - 실제 FRED API 호출 (네트워크 의존성)
 * - FRED API 키 필요 (환경 변수: FRED_API_KEY)
 * - GDP, 실업률, 인플레이션, 금리 등 조회
 *
 * 환경 변수 설정:
 * ```bash
 * export FRED_API_KEY=your_fred_api_key_here
 * ```
 *
 * 실행 방법:
 * ```bash
 * export FRED_API_KEY=your_key
 * ./gradlew test --tests "*MacroIntegration*"
 * # 또는
 * ./gradlew integrationTest
 * ```
 */
@Tag("integration")
class MacroIntegrationTest {

    private lateinit var ufc: UFC
    private var fredApiKey: String? = null

    @BeforeEach
    fun setUp() = runTest {
        fredApiKey = System.getenv("FRED_API_KEY")

        ufc = UFC.create(
            UFCClientConfig(
                fredApiKey = fredApiKey,
                rateLimitingSettings = RateLimitingSettings()
            )
        )
    }

    @AfterEach
    fun tearDown() {
        ufc.close()
    }

    // ========================================
    // FRED API 키 없이 실행 시 예외 테스트
    // ========================================

    @Test
    fun `macro should be null when FRED API key is not provided`() = runTest {
        // Given - API 키 없이 클라이언트 생성
        val ufcWithoutKey = UFC.create(
            UFCClientConfig(
                fredApiKey = null,
                rateLimitingSettings = RateLimitingSettings()
            )
        )

        try {
            // Then - macro가 null이어야 함
            assertThat(ufcWithoutKey.macro).isNull()
            println("✓ Macro API is null when FRED API key is not provided (as expected)")
        } finally {
            ufcWithoutKey.close()
        }
    }

    // ========================================
    // GDP 조회 테스트
    // ========================================

    @Test
    @EnabledIfEnvironmentVariable(named = "FRED_API_KEY", matches = ".+")
    fun `getGDP should return real GDP data`() = runTest {
        // Given
        val macro = ufc.macro ?: run {
            println("⚠ FRED API key not set, skipping test")
            return@runTest
        }

        // When
        val gdp = macro.getGDP(GDPType.REAL)

        // Then
        assertThat(gdp.seriesId).isEqualTo("GDPC1")
        assertThat(gdp.data).isNotEmpty()

        gdp.data.forEach { obs ->
            assertThat(obs.date).isNotBlank()
            assertThat(obs.value).isGreaterThan(0.0)
        }

        println("✓ Real GDP data points: ${gdp.data.size}")
        println("✓ Latest GDP: ${gdp.data.last().value} (${gdp.data.last().date})")
        println("✓ Units: ${gdp.units}")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "FRED_API_KEY", matches = ".+")
    fun `getGDP should return GDP growth rate`() = runTest {
        // Given
        val macro = ufc.macro ?: run {
            println("⚠ FRED API key not set, skipping test")
            return@runTest
        }

        // When
        val gdpGrowth = macro.getGDP(GDPType.GROWTH)

        // Then
        assertThat(gdpGrowth.seriesId).isEqualTo("A191RL1Q225SBEA")
        assertThat(gdpGrowth.data).isNotEmpty()

        println("✓ GDP Growth data points: ${gdpGrowth.data.size}")
        println("✓ Latest GDP Growth: ${gdpGrowth.data.last().value}% (${gdpGrowth.data.last().date})")
    }

    // ========================================
    // 실업률 조회 테스트
    // ========================================

    @Test
    @EnabledIfEnvironmentVariable(named = "FRED_API_KEY", matches = ".+")
    fun `getUnemployment should return unemployment rate data`() = runTest {
        // Given
        val macro = ufc.macro ?: run {
            println("⚠ FRED API key not set, skipping test")
            return@runTest
        }

        // When
        val unemployment = macro.getUnemployment(UnemploymentType.RATE)

        // Then
        assertThat(unemployment.seriesId).isEqualTo("UNRATE")
        assertThat(unemployment.data).isNotEmpty()

        unemployment.data.forEach { obs ->
            assertThat(obs.value).isGreaterThan(0.0)
            assertThat(obs.value).isLessThan(50.0) // 현실적인 실업률 범위
        }

        println("✓ Unemployment Rate data points: ${unemployment.data.size}")
        println("✓ Latest Unemployment Rate: ${unemployment.data.last().value}% (${unemployment.data.last().date})")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "FRED_API_KEY", matches = ".+")
    fun `getUnemployment should return initial jobless claims`() = runTest {
        // Given
        val macro = ufc.macro ?: run {
            println("⚠ FRED API key not set, skipping test")
            return@runTest
        }

        // When
        val claims = macro.getUnemployment(UnemploymentType.INITIAL_CLAIMS)

        // Then
        assertThat(claims.seriesId).isEqualTo("ICSA")
        assertThat(claims.data).isNotEmpty()

        println("✓ Initial Jobless Claims data points: ${claims.data.size}")
        println("✓ Latest Claims: ${claims.data.last().value} (${claims.data.last().date})")
    }

    // ========================================
    // 인플레이션 조회 테스트
    // ========================================

    @Test
    @EnabledIfEnvironmentVariable(named = "FRED_API_KEY", matches = ".+")
    fun `getInflation should return CPI data`() = runTest {
        // Given
        val macro = ufc.macro ?: run {
            println("⚠ FRED API key not set, skipping test")
            return@runTest
        }

        // When
        val cpi = macro.getInflation(InflationType.CPI_ALL)

        // Then
        assertThat(cpi.seriesId).isEqualTo("CPIAUCSL")
        assertThat(cpi.data).isNotEmpty()

        println("✓ CPI data points: ${cpi.data.size}")
        println("✓ Latest CPI: ${cpi.data.last().value} (${cpi.data.last().date})")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "FRED_API_KEY", matches = ".+")
    fun `getInflation should return Core CPI data`() = runTest {
        // Given
        val macro = ufc.macro ?: run {
            println("⚠ FRED API key not set, skipping test")
            return@runTest
        }

        // When
        val coreCpi = macro.getInflation(InflationType.CPI_CORE)

        // Then
        assertThat(coreCpi.seriesId).isEqualTo("CPILFESL")
        assertThat(coreCpi.data).isNotEmpty()

        println("✓ Core CPI data points: ${coreCpi.data.size}")
        println("✓ Latest Core CPI: ${coreCpi.data.last().value} (${coreCpi.data.last().date})")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "FRED_API_KEY", matches = ".+")
    fun `getInflation should return PCE data`() = runTest {
        // Given
        val macro = ufc.macro ?: run {
            println("⚠ FRED API key not set, skipping test")
            return@runTest
        }

        // When
        val pce = macro.getInflation(InflationType.PCE_ALL)

        // Then
        assertThat(pce.seriesId).isEqualTo("PCEPI")
        assertThat(pce.data).isNotEmpty()

        println("✓ PCE data points: ${pce.data.size}")
        println("✓ Latest PCE: ${pce.data.last().value} (${pce.data.last().date})")
    }

    // ========================================
    // 금리 조회 테스트
    // ========================================

    @Test
    @EnabledIfEnvironmentVariable(named = "FRED_API_KEY", matches = ".+")
    fun `getInterestRate should return Federal Funds Rate`() = runTest {
        // Given
        val macro = ufc.macro ?: run {
            println("⚠ FRED API key not set, skipping test")
            return@runTest
        }

        // When
        val fedFunds = macro.getInterestRate(InterestRateType.FEDERAL_FUNDS)

        // Then
        assertThat(fedFunds.seriesId).isEqualTo("FEDFUNDS")
        assertThat(fedFunds.data).isNotEmpty()

        println("✓ Federal Funds Rate data points: ${fedFunds.data.size}")
        println("✓ Latest Fed Funds Rate: ${fedFunds.data.last().value}% (${fedFunds.data.last().date})")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "FRED_API_KEY", matches = ".+")
    fun `getInterestRate should return 10-Year Treasury Yield`() = runTest {
        // Given
        val macro = ufc.macro ?: run {
            println("⚠ FRED API key not set, skipping test")
            return@runTest
        }

        // When
        val treasury10y = macro.getInterestRate(InterestRateType.TREASURY_10Y)

        // Then
        assertThat(treasury10y.seriesId).isEqualTo("DGS10")
        assertThat(treasury10y.data).isNotEmpty()

        println("✓ 10-Year Treasury Yield data points: ${treasury10y.data.size}")
        println("✓ Latest 10Y Yield: ${treasury10y.data.last().value}% (${treasury10y.data.last().date})")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "FRED_API_KEY", matches = ".+")
    fun `getInterestRate should return 2-Year Treasury Yield`() = runTest {
        // Given
        val macro = ufc.macro ?: run {
            println("⚠ FRED API key not set, skipping test")
            return@runTest
        }

        // When
        val treasury2y = macro.getInterestRate(InterestRateType.TREASURY_2Y)

        // Then
        assertThat(treasury2y.seriesId).isEqualTo("DGS2")
        assertThat(treasury2y.data).isNotEmpty()

        println("✓ 2-Year Treasury Yield data points: ${treasury2y.data.size}")
        println("✓ Latest 2Y Yield: ${treasury2y.data.last().value}% (${treasury2y.data.last().date})")
    }

    // ========================================
    // 기간 필터링 테스트
    // ========================================

    @Test
    @EnabledIfEnvironmentVariable(named = "FRED_API_KEY", matches = ".+")
    fun `getSeries should support date range filtering`() = runTest {
        // Given
        val macro = ufc.macro ?: run {
            println("⚠ FRED API key not set, skipping test")
            return@runTest
        }

        // When - 2020년부터 2023년까지 GDP 데이터
        val gdp = macro.getSeries(
            seriesId = "GDPC1",
            startDate = "2020-01-01",
            endDate = "2023-12-31"
        )

        // Then
        assertThat(gdp.data).isNotEmpty()

        // 날짜가 범위 내에 있는지 확인
        gdp.data.forEach { obs ->
            assertThat(obs.date).isGreaterThanOrEqualTo("2020-01-01")
            assertThat(obs.date).isLessThanOrEqualTo("2023-12-31")
        }

        println("✓ GDP data (2020-2023): ${gdp.data.size} data points")
        println("✓ First: ${gdp.data.first().date}")
        println("✓ Last: ${gdp.data.last().date}")
    }

    // ========================================
    // 데이터 검증 테스트
    // ========================================

    @Test
    @EnabledIfEnvironmentVariable(named = "FRED_API_KEY", matches = ".+")
    fun `macro data should be sorted by date`() = runTest {
        // Given
        val macro = ufc.macro ?: run {
            println("⚠ FRED API key not set, skipping test")
            return@runTest
        }

        // When
        val gdp = macro.getGDP(GDPType.REAL)

        // Then - 날짜가 오름차순으로 정렬되어 있는지 확인
        val dates = gdp.data.map { it.date }
        assertThat(dates).isSorted()
        println("✓ GDP data is sorted by date")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "FRED_API_KEY", matches = ".+")
    fun `macro values should be within reasonable ranges`() = runTest {
        // Given
        val macro = ufc.macro ?: run {
            println("⚠ FRED API key not set, skipping test")
            return@runTest
        }

        // When
        val unemployment = macro.getUnemployment(UnemploymentType.RATE)

        // Then - 실업률은 0%에서 50% 사이
        unemployment.data.forEach { obs ->
            assertThat(obs.value).isGreaterThan(0.0)
            assertThat(obs.value).isLessThan(50.0)
        }
        println("✓ All unemployment rate values are within reasonable range (0-50%)")
    }
}
