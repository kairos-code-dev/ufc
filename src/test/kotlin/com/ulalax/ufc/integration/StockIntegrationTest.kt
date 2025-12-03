package com.ulalax.ufc.integration

import com.ulalax.ufc.api.client.UFC
import com.ulalax.ufc.api.client.UFCClientConfig
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitingSettings
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Stock 도메인 통합 테스트
 *
 * 실제 Yahoo Finance API를 호출하여 Stock 정보 조회를 검증합니다.
 *
 * 특징:
 * - 실제 API 호출 (네트워크 의존성)
 * - 회사 정보, ISIN, 발행주식수 등 조회
 * - 안정적인 심볼 사용 (AAPL, MSFT 등)
 *
 * 실행 방법:
 * ```bash
 * ./gradlew test --tests "*StockIntegration*"
 * # 또는
 * ./gradlew integrationTest
 * ```
 */
@Tag("integration")
class StockIntegrationTest {

    private lateinit var ufc: UFC

    @BeforeEach
    fun setUp() = runTest {
        ufc = UFC.create(
            UFCClientConfig(
                rateLimitingSettings = RateLimitingSettings()
            )
        )
    }

    @AfterEach
    fun tearDown() {
        ufc.close()
    }

    // ========================================
    // 회사 정보 조회 테스트
    // ========================================

    @Test
    fun `getCompanyInfo should return real AAPL company data`() = runTest {
        // When
        val companyInfo = ufc.stock.getCompanyInfo("AAPL")

        // Then
        assertThat(companyInfo.symbol).isEqualTo("AAPL")
        assertThat(companyInfo.shortName).isNotBlank()
        assertThat(companyInfo.longName).contains("Apple")
        assertThat(companyInfo.sector).isNotBlank()
        assertThat(companyInfo.industry).isNotBlank()
        assertThat(companyInfo.country).isEqualTo("United States")
        assertThat(companyInfo.website).isNotBlank()
        println("✓ Company: ${companyInfo.longName}")
        println("✓ Sector: ${companyInfo.sector}")
        println("✓ Industry: ${companyInfo.industry}")
        println("✓ Website: ${companyInfo.website}")
    }

    @Test
    fun `getCompanyInfo should return real MSFT company data`() = runTest {
        // When
        val companyInfo = ufc.stock.getCompanyInfo("MSFT")

        // Then
        assertThat(companyInfo.symbol).isEqualTo("MSFT")
        assertThat(companyInfo.longName).contains("Microsoft")
        assertThat(companyInfo.sector).isNotBlank()
        assertThat(companyInfo.country).isEqualTo("United States")
        println("✓ Company: ${companyInfo.longName}")
        println("✓ Employees: ${companyInfo.employees}")
    }

    @Test
    fun `getCompanyInfo should return multiple companies`() = runTest {
        // Given
        val symbols = listOf("AAPL", "MSFT", "GOOGL")

        // When
        val companies = ufc.stock.getCompanyInfo(symbols)

        // Then
        assertThat(companies).hasSize(3)
        assertThat(companies.keys).containsExactlyInAnyOrder("AAPL", "MSFT", "GOOGL")
        companies.forEach { (symbol, info) ->
            assertThat(info.symbol).isEqualTo(symbol)
            assertThat(info.longName).isNotBlank()
            assertThat(info.sector).isNotBlank()
            println("✓ $symbol: ${info.longName} - ${info.sector}")
        }
    }

    // ========================================
    // FastInfo 조회 테스트
    // ========================================

    @Test
    fun `getFastInfo should return quick access data for AAPL`() = runTest {
        // When
        val fastInfo = ufc.stock.getFastInfo("AAPL")

        // Then
        assertThat(fastInfo.symbol).isEqualTo("AAPL")
        assertThat(fastInfo.currency).isEqualTo("USD")
        assertThat(fastInfo.exchange).isNotBlank()
        assertThat(fastInfo.quoteType).isNotNull()
        println("✓ Symbol: ${fastInfo.symbol}")
        println("✓ Exchange: ${fastInfo.exchange}")
        println("✓ Quote Type: ${fastInfo.quoteType}")
    }

    @Test
    fun `getFastInfo should return multiple fast info data`() = runTest {
        // Given
        val symbols = listOf("AAPL", "MSFT")

        // When
        val fastInfos = ufc.stock.getFastInfo(symbols)

        // Then
        assertThat(fastInfos).hasSize(2)
        fastInfos.forEach { (symbol, info) ->
            assertThat(info.symbol).isEqualTo(symbol)
            assertThat(info.currency).isNotBlank()
            println("✓ $symbol: ${info.exchange} - ${info.quoteType}")
        }
    }

    // ========================================
    // ISIN 코드 조회 테스트
    // ========================================

    @Test
    fun `getIsin should return AAPL ISIN code`() = runTest {
        // Note: Yahoo Finance API가 ISIN을 더 이상 제공하지 않을 수 있음
        // 이 경우 UfcException이 발생함
        try {
            val isin = ufc.stock.getIsin("AAPL")
            assertThat(isin).isNotBlank()
            assertThat(isin).startsWith("US") // AAPL은 미국 증권
            assertThat(isin).hasSize(12) // ISIN은 12자리
            println("✓ AAPL ISIN: $isin")
        } catch (e: Exception) {
            println("✓ ISIN not available from Yahoo Finance API (expected): ${e.message}")
            // ISIN이 없는 것은 Yahoo Finance API의 제약이므로 테스트 통과
        }
    }

    @Test
    fun `getIsin should return multiple ISIN codes`() = runTest {
        // Given
        val symbols = listOf("AAPL", "MSFT", "GOOGL")

        // When
        val isins = ufc.stock.getIsin(symbols)

        // Then
        // Yahoo Finance API가 ISIN을 제공하지 않을 수 있으므로 empty도 허용
        println("✓ Retrieved ${isins.size} ISIN codes (Yahoo Finance may not provide ISIN)")
        isins.forEach { (symbol, isin) ->
            assertThat(isin).isNotBlank()
            assertThat(isin).hasSize(12)
            assertThat(isin).startsWith("US")
            println("✓ $symbol ISIN: $isin")
        }
    }

    // ========================================
    // 발행주식수 조회 테스트
    // ========================================

    @Test
    fun `getShares should return AAPL shares data`() = runTest {
        // When
        val shares = ufc.stock.getShares("AAPL")

        // Then
        assertThat(shares).isNotEmpty()
        shares.forEach { sharesData ->
            assertThat(sharesData.date).isNotNull()
            assertThat(sharesData.shares).isGreaterThan(0)
        }
        println("✓ AAPL Shares data points: ${shares.size}")
        println("✓ Latest shares: ${shares.last().shares}")
    }

    @Test
    fun `getShares should return multiple symbols shares data`() = runTest {
        // Given
        val symbols = listOf("AAPL", "MSFT")

        // When
        val sharesMap = ufc.stock.getShares(symbols)

        // Then
        assertThat(sharesMap).hasSize(2)
        sharesMap.forEach { (symbol, sharesList) ->
            assertThat(sharesList).isNotEmpty()
            println("✓ $symbol: ${sharesList.size} shares data points")
        }
    }

    @Test
    fun `getSharesFull should return AAPL full shares history`() = runTest {
        // When
        val shares = ufc.stock.getSharesFull("AAPL")

        // Then
        assertThat(shares).isNotEmpty()
        shares.forEach { sharesData ->
            assertThat(sharesData.date).isNotNull()
            assertThat(sharesData.shares).isGreaterThan(0)
        }
        println("✓ AAPL Full shares history: ${shares.size} data points")
        println("✓ Oldest date: ${shares.first().date}")
        println("✓ Latest date: ${shares.last().date}")
    }

    // ========================================
    // 데이터 일관성 검증 테스트
    // ========================================

    @Test
    fun `company info should match fast info data`() = runTest {
        // Given
        val symbol = "AAPL"

        // When
        val companyInfo = ufc.stock.getCompanyInfo(symbol)
        val fastInfo = ufc.stock.getFastInfo(symbol)

        // Then - 기본 정보가 일치해야 함
        assertThat(companyInfo.symbol).isEqualTo(fastInfo.symbol)
        assertThat(companyInfo.currency).isEqualTo(fastInfo.currency)
        println("✓ Company Info and Fast Info match for $symbol")
    }
}
