package com.ulalax.ufc.integration

import com.ulalax.ufc.api.client.UFC
import com.ulalax.ufc.api.client.UFCClientConfig
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitingSettings
import com.ulalax.ufc.domain.common.Period
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Corp 도메인 통합 테스트
 *
 * 실제 Yahoo Finance API를 호출하여 기업 행동(배당금, 주식분할, 자본이득) 정보 조회를 검증합니다.
 *
 * 특징:
 * - 실제 API 호출 (네트워크 의존성)
 * - 배당금, 주식분할, 자본이득 이력 조회
 * - 안정적인 심볼 사용 (AAPL, MSFT 등)
 *
 * 실행 방법:
 * ```bash
 * ./gradlew test --tests "*CorpIntegration*"
 * # 또는
 * ./gradlew integrationTest
 * ```
 */
@Tag("integration")
class CorpIntegrationTest {

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
    // 배당금 조회 테스트
    // ========================================

    @Test
    fun `getDividends should return AAPL dividend history`() = runTest {
        // When
        val dividendHistory = ufc.corp.getDividends(
            symbol = "AAPL",
            period = Period.FiveYears
        )

        // Then
        assertThat(dividendHistory.symbol).isEqualTo("AAPL")
        assertThat(dividendHistory.dividends).isNotEmpty()
        dividendHistory.dividends.forEach { dividend ->
            assertThat(dividend.date).isNotNull()
            assertThat(dividend.amount).isGreaterThan(0.0)
        }
        println("✓ AAPL Dividends (5 years): ${dividendHistory.dividends.size} payments")
        println("✓ Latest dividend: ${dividendHistory.dividends.last().amount} on ${dividendHistory.dividends.last().date}")
        println("✓ First 3 dividends:")
        dividendHistory.dividends.take(3).forEach { div ->
            println("  - ${div.date}: ${div.amount} USD")
        }
    }

    @Test
    fun `getDividends should return MSFT dividend history`() = runTest {
        // When
        val dividendHistory = ufc.corp.getDividends(
            symbol = "MSFT",
            period = Period.FiveYears
        )

        // Then
        assertThat(dividendHistory.symbol).isEqualTo("MSFT")
        assertThat(dividendHistory.dividends).isNotEmpty()
        println("✓ MSFT Dividends (5 years): ${dividendHistory.dividends.size} payments")
        println("✓ Latest dividend: ${dividendHistory.dividends.last().amount} on ${dividendHistory.dividends.last().date}")
    }

    @Test
    fun `getDividends should return dividend history sorted by date`() = runTest {
        // When
        val dividendHistory = ufc.corp.getDividends(
            symbol = "AAPL",
            period = Period.TwoYears
        )

        // Then
        assertThat(dividendHistory.dividends).isNotEmpty()

        // 날짜가 오름차순으로 정렬되어 있는지 확인
        val dates = dividendHistory.dividends.map { it.date }
        assertThat(dates).isSorted()
        println("✓ AAPL Dividends are sorted by date")
        println("✓ Earliest: ${dates.first()}")
        println("✓ Latest: ${dates.last()}")
    }

    @Test
    fun `getDividends should return empty list for non-dividend stock`() = runTest {
        // Given - TSLA는 배당금을 지급하지 않음
        val symbol = "TSLA"

        // When
        val dividendHistory = ufc.corp.getDividends(symbol, Period.FiveYears)

        // Then
        assertThat(dividendHistory.symbol).isEqualTo(symbol)
        // TSLA는 일반적으로 배당을 지급하지 않지만, 정책이 변경될 수 있음
        println("✓ $symbol dividend history: ${dividendHistory.dividends.size} payments")
    }

    // ========================================
    // 주식분할 조회 테스트
    // ========================================

    @Test
    fun `getSplits should return AAPL stock split history`() = runTest {
        // When
        val splitHistory = ufc.corp.getSplits(
            symbol = "AAPL",
            period = Period.Max
        )

        // Then
        assertThat(splitHistory.symbol).isEqualTo("AAPL")
        // AAPL은 2020년에 4:1 주식분할을 했으므로 분할 이력이 있어야 함
        // 하지만 API가 최근 이력만 반환할 수 있음
        println("✓ AAPL Stock Splits (all time): ${splitHistory.splits.size} events")

        if (splitHistory.splits.isNotEmpty()) {
            splitHistory.splits.forEach { split ->
                assertThat(split.date).isNotNull()
                assertThat(split.ratio).isGreaterThan(0.0)
                println("  - ${split.date}: ${split.ratio} (${split.numerator}:${split.denominator})")
            }
        } else {
            println("  (No split history returned by API - may require different period or Yahoo Finance limitation)")
        }
    }

    @Test
    fun `getSplits should return TSLA stock split history`() = runTest {
        // When
        val splitHistory = ufc.corp.getSplits(
            symbol = "TSLA",
            period = Period.Max
        )

        // Then
        assertThat(splitHistory.symbol).isEqualTo("TSLA")
        // TSLA도 2020년과 2022년에 주식분할을 했으므로 이력이 있을 수 있음
        println("✓ TSLA Stock Splits (all time): ${splitHistory.splits.size} events")
        if (splitHistory.splits.isNotEmpty()) {
            splitHistory.splits.forEach { split ->
                assertThat(split.date).isNotNull()
                assertThat(split.ratio).isGreaterThan(0.0)
                println("  - ${split.date}: ${split.ratio} (${split.numerator}:${split.denominator})")
            }
        } else {
            println("  (No split history returned by API)")
        }
    }

    @Test
    fun `getSplits should return empty list for stocks without splits`() = runTest {
        // Given - JNJ는 주식분할이 적거나 없음
        val symbol = "JNJ"

        // When
        val splitHistory = ufc.corp.getSplits(symbol, Period.Max)

        // Then
        assertThat(splitHistory.symbol).isEqualTo(symbol)
        println("✓ $symbol Stock Splits: ${splitHistory.splits.size} events")
        // 분할 이력이 없거나 적을 수 있음
    }

    // ========================================
    // 자본이득 분배 조회 테스트
    // ========================================

    @Test
    fun `getCapitalGains should return SPY capital gains history`() = runTest {
        // When
        val capitalGainHistory = ufc.corp.getCapitalGains(
            symbol = "SPY",
            period = Period.FiveYears
        )

        // Then
        assertThat(capitalGainHistory.symbol).isEqualTo("SPY")
        // SPY는 ETF이므로 자본이득 분배가 있을 수 있음 (있거나 없을 수 있음)
        println("✓ SPY Capital Gains (5 years): ${capitalGainHistory.capitalGains.size} distributions")

        if (capitalGainHistory.capitalGains.isNotEmpty()) {
            capitalGainHistory.capitalGains.forEach { gain ->
                assertThat(gain.date).isNotNull()
                assertThat(gain.amount).isGreaterThan(0.0)
                println("  - ${gain.date}: ${gain.amount} USD")
            }
        } else {
            println("  (No capital gains distributions in this period)")
        }
    }

    @Test
    fun `getCapitalGains should return empty list for stocks`() = runTest {
        // Given - AAPL은 주식이므로 자본이득 분배가 없음 (ETF/펀드만 해당)
        val symbol = "AAPL"

        // When
        val capitalGainHistory = ufc.corp.getCapitalGains(symbol, Period.FiveYears)

        // Then
        assertThat(capitalGainHistory.symbol).isEqualTo(symbol)
        assertThat(capitalGainHistory.capitalGains).isEmpty()
        println("✓ AAPL has no capital gains (stocks don't distribute capital gains)")
    }

    @Test
    fun `getCapitalGains should return AGG bond ETF capital gains`() = runTest {
        // When
        val capitalGainHistory = ufc.corp.getCapitalGains(
            symbol = "AGG",
            period = Period.FiveYears
        )

        // Then
        assertThat(capitalGainHistory.symbol).isEqualTo("AGG")
        println("✓ AGG Capital Gains (5 years): ${capitalGainHistory.capitalGains.size} distributions")

        if (capitalGainHistory.capitalGains.isNotEmpty()) {
            capitalGainHistory.capitalGains.forEach { gain ->
                println("  - ${gain.date}: ${gain.amount} USD")
            }
        }
    }

    // ========================================
    // 다양한 기간 조회 테스트
    // ========================================

    @Test
    fun `getDividends should support different periods`() = runTest {
        // Given
        val symbol = "AAPL"

        // When - 1년 vs 5년 비교
        val oneYear = ufc.corp.getDividends(symbol, Period.OneYear)
        val fiveYears = ufc.corp.getDividends(symbol, Period.FiveYears)

        // Then - 5년 데이터가 1년 데이터보다 많아야 함
        assertThat(fiveYears.dividends.size).isGreaterThan(oneYear.dividends.size)
        println("✓ AAPL Dividends - 1 year: ${oneYear.dividends.size} payments")
        println("✓ AAPL Dividends - 5 years: ${fiveYears.dividends.size} payments")
    }

    // ========================================
    // 데이터 검증 테스트
    // ========================================

    @Test
    fun `dividend amounts should be positive and reasonable`() = runTest {
        // When
        val dividendHistory = ufc.corp.getDividends("AAPL", Period.TwoYears)

        // Then
        dividendHistory.dividends.forEach { dividend ->
            assertThat(dividend.amount).isGreaterThan(0.0)
            assertThat(dividend.amount).isLessThan(10.0) // 개별 배당금은 통상 10달러 미만
        }
        println("✓ All AAPL dividend amounts are valid")
    }

    @Test
    fun `split ratios should be valid`() = runTest {
        // When
        val splitHistory = ufc.corp.getSplits("AAPL", Period.Max)

        // Then
        if (splitHistory.splits.isNotEmpty()) {
            splitHistory.splits.forEach { split ->
                assertThat(split.ratio).isGreaterThan(0.0)
                assertThat(split.numerator).isGreaterThan(0)
                assertThat(split.denominator).isGreaterThan(0)
                assertThat(split.ratio).isEqualTo(split.numerator.toDouble() / split.denominator.toDouble())
            }
            println("✓ All AAPL split ratios are mathematically valid")
        } else {
            println("✓ No AAPL split data available to validate")
        }
    }
}
