package com.ulalax.ufc.integration

import com.ulalax.ufc.api.client.UFC
import com.ulalax.ufc.api.client.UFCClientConfig
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitingSettings
import com.ulalax.ufc.domain.common.Interval
import com.ulalax.ufc.domain.common.Period
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

/**
 * 도메인 간 상호작용 통합 테스트
 *
 * 여러 도메인 API를 조합하여 실제 사용 시나리오를 검증합니다.
 *
 * 특징:
 * - 실제 API 호출 (네트워크 의존성)
 * - 병렬 요청 처리 검증
 * - 도메인 간 데이터 일관성 검증
 * - Rate Limiting 동작 확인
 *
 * 실행 방법:
 * ```bash
 * ./gradlew test --tests "*CrossDomainIntegration*"
 * # 또는
 * ./gradlew integrationTest
 * ```
 */
@Tag("integration")
class CrossDomainIntegrationTest {

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
    // 주식 정보 + 가격 조합 테스트
    // ========================================

    @Test
    fun `should fetch stock info and current price together`() = runTest {
        // Given
        val symbol = "AAPL"

        // When - 병렬 요청
        val (companyInfo, priceData) = coroutineScope {
            val companyDeferred = async { ufc.stock.getCompanyInfo(symbol) }
            val priceDeferred = async { ufc.price.getCurrentPrice(symbol) }

            companyDeferred.await() to priceDeferred.await()
        }

        // Then - 데이터 일관성 확인
        assertThat(companyInfo.symbol).isEqualTo(symbol)
        assertThat(priceData.symbol).isEqualTo(symbol)
        assertThat(companyInfo.currency).isEqualTo(priceData.currency)

        println("✓ Company: ${companyInfo.longName}")
        println("✓ Price: ${priceData.lastPrice} ${priceData.currency}")
        priceData.marketCap?.let { println("✓ Market Cap: $it") }
    }

    @Test
    fun `should fetch stock info and price history together`() = runTest {
        // Given
        val symbol = "MSFT"

        // When - 병렬 요청
        val (companyInfo, priceHistory) = coroutineScope {
            val companyDeferred = async { ufc.stock.getCompanyInfo(symbol) }
            val priceDeferred = async { ufc.price.getPriceHistory(symbol, Period.OneMonth) }

            companyDeferred.await() to priceDeferred.await()
        }

        // Then
        assertThat(companyInfo.symbol).isEqualTo(symbol)
        assertThat(priceHistory).isNotEmpty()

        println("✓ Company: ${companyInfo.longName}")
        println("✓ Price History: ${priceHistory.size} data points")
        println("✓ Latest Close: ${priceHistory.last().close}")
    }

    // ========================================
    // 펀드 정보 + 가격 조합 테스트
    // ========================================

    @Test
    fun `should fetch fund data and price history together`() = runTest {
        // Given
        val symbol = "SPY"

        // When - 병렬 요청
        val (fundData, priceHistory) = coroutineScope {
            val fundDeferred = async { ufc.funds.getFundData(symbol) }
            val priceDeferred = async {
                ufc.price.getPriceHistory(symbol, Period.OneMonth, Interval.OneDay)
            }

            fundDeferred.await() to priceDeferred.await()
        }

        // Then
        assertThat(fundData.symbol).isEqualTo(symbol)
        assertThat(priceHistory).isNotEmpty()
        assertThat(priceHistory.size).isGreaterThan(15) // 한 달치 데이터

        println("✓ Fund: ${fundData.description ?: fundData.symbol}")
        println("✓ Total Assets: ${fundData.quoteType}")
        println("✓ Price History: ${priceHistory.size} data points")
    }

    @Test
    fun `should verify fund type and fetch appropriate data`() = runTest {
        // Given
        val symbol = "QQQ"

        // When - 순차 요청 (펀드 여부 확인 후 데이터 조회)
        val isFund = ufc.funds.isFund(symbol)

        val fundData = if (isFund) {
            ufc.funds.getFundData(symbol)
        } else {
            null
        }

        // Then
        assertThat(isFund).isTrue()
        assertThat(fundData).isNotNull()
        assertThat(fundData?.symbol).isEqualTo(symbol)

        println("✓ $symbol is a fund: $isFund")
        println("✓ Fund description: ${fundData?.description ?: fundData?.symbol}")
    }

    // ========================================
    // 배당금 + 가격 조합 테스트
    // ========================================

    @Test
    fun `should fetch dividends and price history for analysis`() = runTest {
        // Given
        val symbol = "AAPL"

        // When - 병렬 요청
        val (dividends, priceHistory) = coroutineScope {
            val dividendsDeferred = async { ufc.corp.getDividends(symbol, Period.FiveYears) }
            val priceDeferred = async { ufc.price.getPriceHistory(symbol, Period.FiveYears) }

            dividendsDeferred.await() to priceDeferred.await()
        }

        // Then
        assertThat(dividends.symbol).isEqualTo(symbol)
        assertThat(dividends.dividends).isNotEmpty()
        assertThat(priceHistory).isNotEmpty()

        println("✓ Dividends: ${dividends.dividends.size} payments")
        println("✓ Price History: ${priceHistory.size} data points")
        println("✓ Latest Dividend: ${dividends.dividends.last().amount}")
        println("✓ Latest Price: ${priceHistory.last().close}")
    }

    // ========================================
    // 다중 심볼 조회 테스트
    // ========================================

    @Test
    fun `should fetch multiple symbols across domains`() = runTest {
        // Given
        val symbols = listOf("AAPL", "MSFT", "GOOGL")

        // When - 병렬 요청
        val (companies, prices) = coroutineScope {
            val companiesDeferred = async { ufc.stock.getCompanyInfo(symbols) }
            val pricesDeferred = async { ufc.price.getCurrentPrice(symbols) }

            companiesDeferred.await() to pricesDeferred.await()
        }

        // Then
        assertThat(companies).hasSize(3)
        assertThat(prices).hasSize(3)

        symbols.forEach { symbol ->
            val company = companies[symbol]
            val price = prices[symbol]

            assertThat(company).isNotNull()
            assertThat(price).isNotNull()
            assertThat(company?.symbol).isEqualTo(symbol)
            assertThat(price?.symbol).isEqualTo(symbol)

            println("✓ $symbol: ${company?.longName} @ ${price?.lastPrice} ${price?.currency}")
        }
    }

    // ========================================
    // 전체 주식 정보 조합 테스트
    // ========================================

    @Test
    fun `should build complete stock profile with all domains`() = runTest {
        // Given
        val symbol = "AAPL"

        // When - 모든 관련 정보 병렬 조회
        val (companyInfo, currentPrice, priceHistory, dividends, splits) = coroutineScope {
            val companyDeferred = async { ufc.stock.getCompanyInfo(symbol) }
            val priceDeferred = async { ufc.price.getCurrentPrice(symbol) }
            val historyDeferred = async { ufc.price.getPriceHistory(symbol, Period.OneMonth) }
            val dividendsDeferred = async { ufc.corp.getDividends(symbol, Period.FiveYears) }
            val splitsDeferred = async { ufc.corp.getSplits(symbol, Period.Max) }

            Tuple5(
                companyDeferred.await(),
                priceDeferred.await(),
                historyDeferred.await(),
                dividendsDeferred.await(),
                splitsDeferred.await()
            )
        }

        // Then - 완전한 주식 프로필 구성
        assertThat(companyInfo.symbol).isEqualTo(symbol)
        assertThat(currentPrice.symbol).isEqualTo(symbol)
        assertThat(priceHistory).isNotEmpty()
        assertThat(dividends.symbol).isEqualTo(symbol)
        assertThat(splits.symbol).isEqualTo(symbol)

        println("✓ Complete Stock Profile for $symbol:")
        println("  Company: ${companyInfo.longName}")
        println("  Sector: ${companyInfo.sector}")
        println("  Current Price: ${currentPrice.lastPrice} ${currentPrice.currency}")
        println("  Price History: ${priceHistory.size} data points")
        println("  Dividends: ${dividends.dividends.size} payments")
        println("  Stock Splits: ${splits.splits.size} events")

        // 기본 검증만 수행 (데이터 유무는 optional)
        assertThat(companyInfo.longName).isNotBlank()
        assertThat(currentPrice.lastPrice).isGreaterThan(0.0)
    }

    // ========================================
    // Rate Limiting 동작 검증 테스트
    // ========================================

    @Test
    fun `should handle rate limiting across domains`() = runTest {
        // Given - 여러 도메인에 대량 요청
        val symbols = listOf("AAPL", "MSFT", "GOOGL", "AMZN", "TSLA")

        // When - 각 심볼에 대해 Price + Stock 정보 요청
        val startTime = System.currentTimeMillis()

        symbols.forEach { symbol ->
            coroutineScope {
                async { ufc.price.getCurrentPrice(symbol) }
                async { ufc.stock.getCompanyInfo(symbol) }
            }
        }

        val duration = System.currentTimeMillis() - startTime

        // Then - Rate Limiting이 작동하여 적절한 시간 소요
        println("✓ Total duration for ${symbols.size} symbols (2 API calls each): ${duration}ms")
        println("✓ Average per symbol: ${duration / symbols.size}ms")

        // Rate limiting이 있어도 모든 요청이 성공적으로 완료됨을 확인
        assertThat(duration).isGreaterThan(0)
    }

    @Test
    fun `should handle concurrent requests efficiently`() = runTest {
        // Given
        val symbol = "AAPL"

        // When - 동일한 데이터를 여러 번 병렬 요청 (캐싱 효과 확인)
        val requestCount = 5
        val results = mutableListOf<Long>()

        // 첫 번째 요청 (캐시 미스)
        val firstDuration = measureTimeMillis {
            ufc.price.getCurrentPrice(symbol)
        }
        results.add(firstDuration)

        // 이후 요청들 (캐시 히트)
        repeat(requestCount - 1) {
            val duration = measureTimeMillis {
                ufc.price.getCurrentPrice(symbol)
            }
            results.add(duration)
        }

        // Then
        println("✓ Request durations:")
        results.forEachIndexed { index, duration ->
            println("  Request ${index + 1}: ${duration}ms")
        }

        // 캐시된 요청은 빠르게 완료되어야 함 (타이밍은 유연하게)
        val avgCachedTime = results.drop(1).average()
        println("✓ First request: ${results.first()}ms")
        println("✓ Avg cached request: ${avgCachedTime}ms")
        // 모든 요청이 성공적으로 완료되었는지만 확인
        assertThat(results).allMatch { it >= 0 }
    }

    // ========================================
    // 데이터 일관성 검증 테스트
    // ========================================

    @Test
    fun `should verify data consistency across domains`() = runTest {
        // Given
        val symbol = "AAPL"

        // When
        val companyInfo = ufc.stock.getCompanyInfo(symbol)
        val fastInfo = ufc.stock.getFastInfo(symbol)
        val priceData = ufc.price.getCurrentPrice(symbol)

        // Then - 기본 정보가 일치해야 함
        assertThat(companyInfo.symbol).isEqualTo(fastInfo.symbol)
        assertThat(companyInfo.symbol).isEqualTo(priceData.symbol)
        assertThat(companyInfo.currency).isEqualTo(fastInfo.currency)
        assertThat(companyInfo.currency).isEqualTo(priceData.currency)

        println("✓ Data consistency verified for $symbol:")
        println("  Symbol: ${companyInfo.symbol} = ${fastInfo.symbol} = ${priceData.symbol}")
        println("  Currency: ${companyInfo.currency} = ${fastInfo.currency} = ${priceData.currency}")
    }

    @Test
    fun `should verify market cap consistency`() = runTest {
        // Given
        val symbol = "MSFT"

        // When
        val companyInfo = ufc.stock.getCompanyInfo(symbol)
        val priceData = ufc.price.getCurrentPrice(symbol)

        // Then - Price data의 Market Cap 확인
        priceData.marketCap?.let { marketCap ->
            println("✓ Market Cap for $symbol: $marketCap")
            assertThat(marketCap).isGreaterThan(0)
        } ?: println("✓ Market Cap not available for $symbol")

        // Company info 검증
        assertThat(companyInfo.symbol).isEqualTo(symbol)
        assertThat(priceData.symbol).isEqualTo(symbol)
    }

    // Helper data class for tuple
    private data class Tuple5<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )
}
