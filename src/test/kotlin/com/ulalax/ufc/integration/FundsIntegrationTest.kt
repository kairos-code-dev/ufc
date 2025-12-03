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
 * Funds 도메인 통합 테스트
 *
 * 실제 Yahoo Finance API를 호출하여 펀드(ETF/뮤추얼펀드) 정보 조회를 검증합니다.
 *
 * 특징:
 * - 실제 API 호출 (네트워크 의존성)
 * - ETF 및 뮤추얼펀드 정보 조회
 * - 안정적인 심볼 사용 (SPY, QQQ, AGG 등)
 *
 * 실행 방법:
 * ```bash
 * ./gradlew test --tests "*FundsIntegration*"
 * # 또는
 * ./gradlew integrationTest
 * ```
 */
@Tag("integration")
class FundsIntegrationTest {

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
    // 펀드 데이터 조회 테스트
    // ========================================

    @Test
    fun `getFundData should return real SPY ETF data`() = runTest {
        // When
        val fundData = ufc.funds.getFundData("SPY")

        // Then
        assertThat(fundData.symbol).isEqualTo("SPY")
        assertThat(fundData.quoteType).isIn("ETF", "MUTUALFUND")
        fundData.description?.let {
            assertThat(it).isNotBlank()
            println("✓ Description: $it")
        }
        fundData.fundOverview?.let {
            it.categoryName?.let { category -> println("✓ Category: $category") }
            it.family?.let { family -> println("✓ Family: $family") }
        }
        println("✓ Quote Type: ${fundData.quoteType}")
    }

    @Test
    fun `getFundData should return real QQQ ETF data`() = runTest {
        // When
        val fundData = ufc.funds.getFundData("QQQ")

        // Then
        assertThat(fundData.symbol).isEqualTo("QQQ")
        assertThat(fundData.quoteType).isEqualTo("ETF")
        println("✓ QQQ Quote Type: ${fundData.quoteType}")
        fundData.description?.let { println("✓ Description: $it") }
    }

    @Test
    fun `getFundData should return real AGG bond ETF data`() = runTest {
        // When
        val fundData = ufc.funds.getFundData("AGG")

        // Then
        assertThat(fundData.symbol).isEqualTo("AGG")
        assertThat(fundData.quoteType).isEqualTo("ETF")
        println("✓ AGG Quote Type: ${fundData.quoteType}")
        fundData.fundOverview?.categoryName?.let { println("✓ Category: $it") }
    }

    @Test
    fun `getFundData should return multiple funds data`() = runTest {
        // Given
        val symbols = listOf("SPY", "QQQ", "AGG")

        // When
        val fundsData = ufc.funds.getFundData(symbols)

        // Then
        assertThat(fundsData).hasSize(3)
        assertThat(fundsData.keys).containsExactlyInAnyOrder("SPY", "QQQ", "AGG")
        fundsData.forEach { (symbol, data) ->
            assertThat(data.symbol).isEqualTo(symbol)
            assertThat(data.quoteType).isNotBlank()
            println("✓ $symbol: ${data.quoteType}")
        }
    }

    // ========================================
    // Top Holdings 테스트
    // ========================================

    @Test
    fun `getFundData should return SPY top holdings if available`() = runTest {
        // When
        val fundData = ufc.funds.getFundData("SPY")

        // Then
        fundData.topHoldings?.let { holdings ->
            if (holdings.isNotEmpty()) {
                println("✓ SPY Top Holdings (${holdings.size}):")
                holdings.take(5).forEach { holding ->
                    println("  - ${holding.symbol}: ${holding.holdingPercent}% (${holding.name})")
                }
                // Basic validation - at least symbol should be present
                holdings.forEach { holding ->
                    assertThat(holding.symbol).isNotBlank()
                }
            } else {
                println("✓ SPY top holdings list is empty")
            }
        } ?: println("✓ SPY top holdings not available in current response")
    }

    @Test
    fun `getFundData should return QQQ top holdings if available`() = runTest {
        // When
        val fundData = ufc.funds.getFundData("QQQ")

        // Then
        fundData.topHoldings?.let { holdings ->
            if (holdings.isNotEmpty()) {
                println("✓ QQQ Top Holdings (${holdings.size}):")
                holdings.take(10).forEach { holding ->
                    println("  - ${holding.symbol}: ${holding.holdingPercent}% (${holding.name})")
                }
            } else {
                println("✓ QQQ top holdings list is empty")
            }
        } ?: println("✓ QQQ top holdings not available in current response")
    }

    // ========================================
    // 섹터 및 자산 배분 테스트
    // ========================================

    @Test
    fun `getFundData should return SPY sector weightings if available`() = runTest {
        // When
        val fundData = ufc.funds.getFundData("SPY")

        // Then
        fundData.sectorWeightings?.let { sectorWeightings ->
            if (sectorWeightings.isNotEmpty()) {
                var totalWeight = 0.0
                println("✓ SPY Sector Weightings:")
                sectorWeightings.forEach { (sector, weight) ->
                    assertThat(sector).isNotBlank()
                    assertThat(weight).isGreaterThan(0.0)
                    totalWeight += weight
                    println("  - $sector: $weight%")
                }
                println("✓ Total weight: $totalWeight%")
            } else {
                println("✓ SPY sector weightings list is empty")
            }
        } ?: println("✓ SPY sector weightings not available in current response")
    }

    @Test
    fun `getFundData should return bond ratings for AGG if available`() = runTest {
        // When
        val fundData = ufc.funds.getFundData("AGG")

        // Then
        fundData.bondRatings?.let { bondRatings ->
            if (bondRatings.isNotEmpty()) {
                println("✓ AGG Bond Ratings:")
                bondRatings.forEach { (rating, weight) ->
                    println("  - $rating: $weight%")
                }
            } else {
                println("✓ AGG bond ratings list is empty")
            }
        } ?: println("✓ AGG bond ratings not available in current response")
    }

    // ========================================
    // 펀드 여부 확인 테스트
    // ========================================

    @Test
    fun `isFund should return true for SPY ETF`() = runTest {
        // When
        val result = ufc.funds.isFund("SPY")

        // Then
        assertThat(result).isTrue()
        println("✓ SPY is a fund: $result")
    }

    @Test
    fun `isFund should return true for QQQ ETF`() = runTest {
        // When
        val result = ufc.funds.isFund("QQQ")

        // Then
        assertThat(result).isTrue()
        println("✓ QQQ is a fund: $result")
    }

    @Test
    fun `isFund should return false for AAPL stock`() = runTest {
        // When
        val result = ufc.funds.isFund("AAPL")

        // Then
        assertThat(result).isFalse()
        println("✓ AAPL is a fund: $result")
    }

    // ========================================
    // 펀드 메타데이터 테스트
    // ========================================

    @Test
    fun `getFundData should return fund overview if available`() = runTest {
        // When
        val fundData = ufc.funds.getFundData("SPY")

        // Then
        if (fundData.fundOverview != null) {
            println("✓ SPY Fund Overview:")
            fundData.fundOverview.categoryName?.let { println("  - Category: $it") }
            fundData.fundOverview.family?.let { println("  - Family: $it") }
            fundData.fundOverview.legalType?.let { println("  - Legal Type: $it") }
        } else {
            println("✓ SPY fund overview not available in current response")
        }
    }

    @Test
    fun `getFundData should return asset classes if available`() = runTest {
        // When
        val fundData = ufc.funds.getFundData("SPY")

        // Then
        if (fundData.assetClasses != null) {
            println("✓ SPY Asset Classes:")
            println("  - Stock: ${fundData.assetClasses.stockPosition}")
            println("  - Bond: ${fundData.assetClasses.bondPosition}")
            println("  - Cash: ${fundData.assetClasses.cashPosition}")
            println("  - Other: ${fundData.assetClasses.otherPosition}")
        } else {
            println("✓ SPY asset classes not available in current response")
        }
    }

    @Test
    fun `getFundData should return equity holdings metrics if available`() = runTest {
        // When
        val fundData = ufc.funds.getFundData("QQQ")

        // Then
        if (fundData.equityHoldings != null) {
            println("✓ QQQ Equity Holdings Metrics:")
            fundData.equityHoldings.priceToEarnings?.let { println("  - P/E Ratio: ${it.fundValue}") }
            fundData.equityHoldings.priceToBook?.let { println("  - P/B Ratio: ${it.fundValue}") }
            fundData.equityHoldings.priceToSales?.let { println("  - P/S Ratio: ${it.fundValue}") }
            fundData.equityHoldings.priceToCashflow?.let { println("  - P/CF Ratio: ${it.fundValue}") }
        } else {
            println("✓ QQQ equity holdings metrics not available in current response")
        }
    }
}
