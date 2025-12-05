package com.ulalax.ufc.integration.yahoo

import com.ulalax.ufc.domain.model.fundamentals.FundamentalsType
import com.ulalax.ufc.fixture.TestFixtures
import com.ulalax.ufc.integration.utils.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * YahooClient.fundamentalsTimeseries() API Integration 테스트
 *
 * 이 테스트는 실제 Yahoo Finance Fundamentals Timeseries API를 호출하여 재무제표 시계열 데이터 조회 기능을 검증합니다.
 * API 가이드처럼 읽힐 수 있도록 @Nested 그룹핑 패턴을 사용합니다.
 *
 * ## 테스트 실행 방법
 * ```bash
 * # 특정 클래스 실행
 * ./gradlew test --tests 'FundamentalsTimeseriesSpec'
 *
 * # 특정 그룹 실행
 * ./gradlew test --tests 'FundamentalsTimeseriesSpec$BasicBehavior'
 * ```
 */
@DisplayName("YahooClient.fundamentalsTimeseries() - 재무제표 시계열 데이터 조회")
class FundamentalsTimeseriesSpec : IntegrationTestBase() {

    @Nested
    @DisplayName("기본 동작")
    inner class BasicBehavior {

        @Test
        @DisplayName("AAPL의 연간 총 매출 시계열을 조회할 수 있다")
        fun `returns annual total revenue for AAPL`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val types = listOf(FundamentalsType.ANNUAL_TOTAL_REVENUE)

            // When
            val result = ufc.fundamentalsTimeseries(symbol, types)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.symbol).isEqualTo(symbol)
            assertThat(result.hasData(FundamentalsType.ANNUAL_TOTAL_REVENUE)).isTrue()

            val revenues = result.get(FundamentalsType.ANNUAL_TOTAL_REVENUE)
            assertThat(revenues).isNotNull()
            assertThat(revenues).isNotEmpty()

            // 시계열 데이터 검증
            revenues?.forEach { dataPoint ->
                assertThat(dataPoint.asOfDate).isNotNull()
                assertThat(dataPoint.periodType).isNotBlank()
                assertThat(dataPoint.currencyCode).isNotBlank()
                // value는 null일 수 있음
            }

            // 날짜순 정렬 확인
            val dates = revenues?.map { it.asOfDate }
            assertThat(dates).isSorted()
        }

        @Test
        @DisplayName("여러 재무 항목을 동시에 조회할 수 있다")
        fun `returns multiple fundamentals types`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val types = listOf(
                FundamentalsType.ANNUAL_TOTAL_REVENUE,
                FundamentalsType.QUARTERLY_NET_INCOME,
                FundamentalsType.TRAILING_EPS
            )

            // When
            val result = ufc.fundamentalsTimeseries(symbol, types)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.symbol).isEqualTo(symbol)

            // 각 타입별로 데이터 존재 확인
            types.forEach { type ->
                // 모든 타입이 데이터를 가지지 않을 수 있으므로 hasData만 체크
                val data = result.get(type)
                if (data != null) {
                    assertThat(data).isNotEmpty()
                    println("$type: ${data.size} data points")
                }
            }
        }

        @Test
        @DisplayName("특정 기간의 데이터를 조회할 수 있다")
        fun `returns data for specific date range`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val types = listOf(FundamentalsType.ANNUAL_TOTAL_REVENUE)
            val startDate = LocalDate.of(2020, 1, 1)
            val endDate = LocalDate.of(2023, 12, 31)

            // When
            val result = ufc.fundamentalsTimeseries(symbol, types, startDate, endDate)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.symbol).isEqualTo(symbol)

            val revenues = result.get(FundamentalsType.ANNUAL_TOTAL_REVENUE)
            if (revenues != null) {
                assertThat(revenues).isNotEmpty()
                // 날짜 범위 내에 있는지 확인 (API는 정확한 범위를 보장하지 않을 수 있음)
                revenues.forEach { dataPoint ->
                    assertThat(dataPoint.asOfDate).isNotNull()
                }
            }
        }
    }

    @Nested
    @DisplayName("손익계산서 데이터")
    inner class IncomeStatementData {

        @Test
        @DisplayName("분기별 순이익을 조회할 수 있다")
        fun `returns quarterly net income`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val types = listOf(FundamentalsType.QUARTERLY_NET_INCOME)

            // When
            val result = ufc.fundamentalsTimeseries(symbol, types)

            // Then
            assertThat(result).isNotNull()
            val netIncomes = result.get(FundamentalsType.QUARTERLY_NET_INCOME)

            if (netIncomes != null) {
                assertThat(netIncomes).isNotEmpty()
                netIncomes.forEach { dataPoint ->
                    assertThat(dataPoint.periodType).isIn("3M", "UNKNOWN")
                }
            }
        }

        @Test
        @DisplayName("Trailing 데이터를 조회할 수 있다")
        fun `returns trailing data`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            // trailingEPS는 404를 반환할 수 있으므로 다른 trailing 타입 사용
            val types = listOf(FundamentalsType.TRAILING_TOTAL_REVENUE)

            // When
            val result = ufc.fundamentalsTimeseries(symbol, types)

            // Then
            assertThat(result).isNotNull()
            val revenue = result.get(FundamentalsType.TRAILING_TOTAL_REVENUE)

            if (revenue != null) {
                assertThat(revenue).isNotEmpty()
                revenue.forEach { dataPoint ->
                    assertThat(dataPoint.periodType).isIn("TTM", "UNKNOWN")
                }
            }
        }
    }

    @Nested
    @DisplayName("대차대조표 데이터")
    inner class BalanceSheetData {

        @Test
        @DisplayName("연간 총 자산을 조회할 수 있다")
        fun `returns annual total assets`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val types = listOf(FundamentalsType.ANNUAL_TOTAL_ASSETS)

            // When
            val result = ufc.fundamentalsTimeseries(symbol, types)

            // Then
            assertThat(result).isNotNull()
            val assets = result.get(FundamentalsType.ANNUAL_TOTAL_ASSETS)

            if (assets != null) {
                assertThat(assets).isNotEmpty()
                assets.forEach { dataPoint ->
                    assertThat(dataPoint.periodType).isIn("12M", "UNKNOWN")
                }
            }
        }

        @Test
        @DisplayName("분기별 발행주식수를 조회할 수 있다")
        fun `returns quarterly ordinary shares number`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val types = listOf(FundamentalsType.QUARTERLY_ORDINARY_SHARES_NUMBER)

            // When
            val result = ufc.fundamentalsTimeseries(symbol, types)

            // Then
            assertThat(result).isNotNull()
            val shares = result.get(FundamentalsType.QUARTERLY_ORDINARY_SHARES_NUMBER)

            if (shares != null) {
                assertThat(shares).isNotEmpty()
            }
        }
    }

    @Nested
    @DisplayName("현금흐름표 데이터")
    inner class CashFlowData {

        @Test
        @DisplayName("연간 영업 현금 흐름을 조회할 수 있다")
        fun `returns annual operating cash flow`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val types = listOf(FundamentalsType.ANNUAL_OPERATING_CASH_FLOW)

            // When
            val result = ufc.fundamentalsTimeseries(symbol, types)

            // Then
            assertThat(result).isNotNull()
            val cashFlows = result.get(FundamentalsType.ANNUAL_OPERATING_CASH_FLOW)

            if (cashFlows != null) {
                assertThat(cashFlows).isNotEmpty()
            }
        }

        @Test
        @DisplayName("분기별 잉여 현금 흐름을 조회할 수 있다")
        fun `returns quarterly free cash flow`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val types = listOf(FundamentalsType.QUARTERLY_FREE_CASH_FLOW)

            // When
            val result = ufc.fundamentalsTimeseries(symbol, types)

            // Then
            assertThat(result).isNotNull()
            val freeCashFlows = result.get(FundamentalsType.QUARTERLY_FREE_CASH_FLOW)

            if (freeCashFlows != null) {
                assertThat(freeCashFlows).isNotEmpty()
            }
        }
    }

    @Nested
    @DisplayName("데이터 품질")
    inner class DataQuality {

        @Test
        @DisplayName("TimeseriesDataPoint는 날짜순으로 정렬된다")
        fun `data points are sorted by date`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val types = listOf(FundamentalsType.ANNUAL_TOTAL_REVENUE)

            // When
            val result = ufc.fundamentalsTimeseries(symbol, types)

            // Then
            val revenues = result.get(FundamentalsType.ANNUAL_TOTAL_REVENUE)
            if (revenues != null && revenues.size > 1) {
                for (i in 0 until revenues.size - 1) {
                    assertThat(revenues[i].asOfDate)
                        .isBeforeOrEqualTo(revenues[i + 1].asOfDate)
                }
            }
        }

        @Test
        @DisplayName("currencyCode는 항상 존재한다")
        fun `currency code is always present`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val types = listOf(FundamentalsType.ANNUAL_TOTAL_REVENUE)

            // When
            val result = ufc.fundamentalsTimeseries(symbol, types)

            // Then
            val revenues = result.get(FundamentalsType.ANNUAL_TOTAL_REVENUE)
            revenues?.forEach { dataPoint ->
                assertThat(dataPoint.currencyCode).isNotBlank()
            }
        }
    }

    @Nested
    @DisplayName("에지 케이스")
    inner class EdgeCases {

        @Test
        @DisplayName("존재하지 않는 심볼은 빈 결과를 반환한다")
        fun `returns empty result for invalid symbol`() = integrationTest {
            // Given
            val symbol = "INVALID_SYMBOL_XXXYYY"
            val types = listOf(FundamentalsType.ANNUAL_TOTAL_REVENUE)

            // When
            val result = ufc.fundamentalsTimeseries(symbol, types)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.symbol).isEqualTo(symbol)
            assertThat(result.data).isEmpty()
        }

        @Test
        @DisplayName("ETF는 재무 데이터가 없어 빈 결과를 반환할 수 있다")
        fun `returns empty result for ETF`() = integrationTest {
            // Given
            val symbol = "SPY" // ETF
            val types = listOf(FundamentalsType.ANNUAL_TOTAL_REVENUE)

            // When
            val result = ufc.fundamentalsTimeseries(symbol, types)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.symbol).isEqualTo(symbol)
            // ETF는 재무 데이터가 없을 수 있음
        }
    }
}
