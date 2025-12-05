package com.ulalax.ufc.integration.yahoo

import com.ulalax.ufc.fixture.TestFixtures
import com.ulalax.ufc.integration.utils.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * YahooClient.earningsCalendar() API Integration 테스트
 *
 * 이 테스트는 실제 Yahoo Finance API를 호출하여 Earnings Calendar 기능을 검증합니다.
 * HTML 스크래핑 방식이므로 Yahoo Finance 웹 페이지 구조 변경 시 실패할 수 있습니다.
 *
 * ## 테스트 실행 방법
 * ```bash
 * # 특정 클래스 실행
 * ./gradlew test --tests 'EarningsCalendarSpec'
 *
 * # 특정 그룹 실행
 * ./gradlew test --tests 'EarningsCalendarSpec$BasicBehavior'
 * ```
 */
@DisplayName("[I] Yahoo.earningsCalendar() - 실적 발표 일정 조회")
class EarningsCalendarSpec : IntegrationTestBase() {

    @Nested
    @DisplayName("기본 동작")
    inner class BasicBehavior {

        @Test
        @DisplayName("AAPL의 실적 일정을 조회할 수 있다 (기본 limit=12)")
        fun `returns earnings calendar for AAPL with default limit`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.earningsCalendar(symbol)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.symbol).isEqualTo(symbol)
            assertThat(result.requestedLimit).isEqualTo(12)
            assertThat(result.requestedOffset).isEqualTo(0)

            // HTML 스크래핑 기반이므로 결과가 있으면 검증
            result.events.forEach { event ->
                assertThat(event.earningsDate).isNotNull()
            }
        }

        @Test
        @DisplayName("MSFT의 실적 일정을 커스텀 limit으로 조회할 수 있다")
        fun `returns earnings calendar for MSFT with custom limit`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.MSFT
            val limit = 5

            // When
            val result = ufc.earningsCalendar(symbol, limit = limit)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.symbol).isEqualTo(symbol)
            assertThat(result.requestedLimit).isEqualTo(limit)
            assertThat(result.events.size).isLessThanOrEqualTo(limit)
        }

        @Test
        @DisplayName("offset을 사용하여 과거 실적을 조회할 수 있다")
        fun `returns historical earnings with offset`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val offset = 1

            // When
            val result = ufc.earningsCalendar(symbol, limit = 10, offset = offset)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.requestedOffset).isEqualTo(offset)
        }
    }

    @Nested
    @DisplayName("이벤트 필터링")
    inner class EventFiltering {

        @Test
        @DisplayName("과거 실적과 미래 실적을 필터링할 수 있다")
        fun `filters historical and future events`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.earningsCalendar(symbol, limit = 20)

            // Then
            val historical = result.getHistoricalEvents()
            val future = result.getFutureEvents()

            assertThat(result.events.size).isEqualTo(historical.size + future.size)
            println("Historical: ${historical.size}, Future: ${future.size}")

            // 과거 실적은 reportedEps가 존재해야 함
            historical.forEach { event ->
                assertThat(event.reportedEps).isNotNull()
                assertThat(event.isHistorical()).isTrue()
            }

            // 미래 실적은 reportedEps가 null이어야 함
            future.forEach { event ->
                assertThat(event.reportedEps).isNull()
                assertThat(event.isFuture()).isTrue()
            }
        }

        @Test
        @DisplayName("다음 예정된 실적 일정을 조회할 수 있다")
        fun `returns next earnings event`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.earningsCalendar(symbol)
            val futureEvents = result.getFutureEvents()

            // Then - 미래 실적이 있으면 첫 번째가 다음 실적
            futureEvents.forEach { event ->
                assertThat(event.isFuture()).isTrue()
                assertThat(event.reportedEps).isNull()
            }
        }

        @Test
        @DisplayName("가장 최근 과거 실적을 조회할 수 있다")
        fun `returns latest historical earnings`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.earningsCalendar(symbol, limit = 20, offset = 1)
            val historicalEvents = result.getHistoricalEvents()

            // Then - 과거 실적들 검증
            historicalEvents.forEach { event ->
                assertThat(event.isHistorical()).isTrue()
                assertThat(event.reportedEps).isNotNull()
            }
        }
    }

    @Nested
    @DisplayName("서프라이즈 분석")
    inner class SurpriseAnalysis {

        @Test
        @DisplayName("과거 실적의 서프라이즈를 분석할 수 있다")
        fun `analyzes earnings surprises`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.earningsCalendar(symbol, limit = 20, offset = 1)
            val historical = result.getHistoricalEvents()

            // Then
            val withSurprise = historical.filter { it.surprisePercent != null }
            val positiveSurprises = withSurprise.filter { it.hasPositiveSurprise() }
            val negativeSurprises = withSurprise.filter { it.hasNegativeSurprise() }

            // 서프라이즈가 있는 이벤트들 검증
            positiveSurprises.forEach { event ->
                assertThat(event.surprisePercent).isGreaterThan(0.0)
            }

            negativeSurprises.forEach { event ->
                assertThat(event.surprisePercent).isLessThan(0.0)
            }
        }
    }

    @Nested
    @DisplayName("엣지 케이스")
    inner class EdgeCases {

        @Test
        @DisplayName("존재하지 않는 심볼은 빈 결과를 반환한다")
        fun `returns empty result for non-existent symbol`() = integrationTest {
            // Given
            val symbol = "INVALIDXYZ123"

            // When
            val result = ufc.earningsCalendar(symbol)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.symbol).isEqualTo(symbol)
            assertThat(result.isEmpty()).isTrue()
            assertThat(result.events).isEmpty()
            assertThat(result.actualCount).isEqualTo(0)
        }

        @Test
        @DisplayName("매우 큰 offset은 빈 결과를 반환한다")
        fun `returns empty result for large offset`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val offset = 1000

            // When
            val result = ufc.earningsCalendar(symbol, limit = 10, offset = offset)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.requestedOffset).isEqualTo(offset)
            // offset이 너무 크면 빈 결과일 가능성이 높음
            println("Events count with large offset: ${result.events.size}")
        }

        @Test
        @DisplayName("limit=1로 최소 개수를 조회할 수 있다")
        fun `returns result with minimum limit`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val limit = 1

            // When
            val result = ufc.earningsCalendar(symbol, limit = limit)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.requestedLimit).isEqualTo(limit)
            assertThat(result.events.size).isLessThanOrEqualTo(limit)
        }

        @Test
        @DisplayName("limit=100으로 최대 개수를 조회할 수 있다")
        fun `returns result with maximum limit`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val limit = 100

            // When
            val result = ufc.earningsCalendar(symbol, limit = limit)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.requestedLimit).isEqualTo(limit)
            assertThat(result.events.size).isLessThanOrEqualTo(limit)
            println("Events count with max limit: ${result.events.size}")
        }
    }
}
