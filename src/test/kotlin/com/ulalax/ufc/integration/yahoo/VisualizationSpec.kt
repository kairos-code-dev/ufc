package com.ulalax.ufc.integration.yahoo

import com.ulalax.ufc.domain.model.visualization.EarningsEventType
import com.ulalax.ufc.fixture.TestFixtures
import com.ulalax.ufc.integration.utils.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * YahooClient.visualization() API Integration 테스트
 *
 * 이 테스트는 실제 Yahoo Finance Visualization API를 호출하여 실적 발표 일정 조회 기능을 검증합니다.
 * API 가이드처럼 읽힐 수 있도록 @Nested 그룹핑 패턴을 사용합니다.
 *
 * ## 테스트 실행 방법
 * ```bash
 * # 특정 클래스 실행
 * ./gradlew test --tests 'VisualizationSpec'
 *
 * # 특정 그룹 실행
 * ./gradlew test --tests 'VisualizationSpec$BasicBehavior'
 * ```
 */
@DisplayName("YahooClient.visualization() - 실적 발표 일정 조회")
class VisualizationSpec : IntegrationTestBase() {

    @Nested
    @DisplayName("기본 동작")
    inner class BasicBehavior {

        @Test
        @DisplayName("AAPL의 실적 발표 일정을 조회할 수 있다")
        fun `returns earnings dates for AAPL`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.yahoo.visualization(symbol, limit = 12)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.symbol).isEqualTo(symbol)
            assertThat(result.earningsDates).isNotEmpty()

            // 첫 번째 실적 일정 확인
            val firstEarnings = result.earningsDates.first()
            assertThat(firstEarnings.earningsDate).isNotNull()
            assertThat(firstEarnings.eventType).isNotNull()

            // 로그 출력 (디버깅용)
            println("총 ${result.earningsDates.size}개의 실적 일정")
            result.earningsDates.take(3).forEach { earnings ->
                println("날짜: ${earnings.earningsDate}, 타입: ${earnings.eventType}, " +
                        "EPS 추정: ${earnings.epsEstimate}, EPS 실제: ${earnings.epsActual}")
            }
        }

        @Test
        @DisplayName("MSFT의 실적 발표 일정을 조회할 수 있다")
        fun `returns earnings dates for MSFT`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.MSFT

            // When
            val result = ufc.yahoo.visualization(symbol, limit = 8)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.symbol).isEqualTo(symbol)
            assertThat(result.earningsDates).hasSizeLessThanOrEqualTo(8)
        }

        @Test
        @DisplayName("limit 파라미터가 올바르게 적용된다")
        fun `respects limit parameter`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val limit = 5

            // When
            val result = ufc.yahoo.visualization(symbol, limit = limit)

            // Then
            assertThat(result.earningsDates).hasSizeLessThanOrEqualTo(limit)
        }
    }

    @Nested
    @DisplayName("이벤트 타입 검증")
    inner class EventTypeValidation {

        @Test
        @DisplayName("이벤트 타입이 올바르게 파싱된다")
        fun `parses event types correctly`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.yahoo.visualization(symbol, limit = 20)

            // Then
            assertThat(result.earningsDates).isNotEmpty()

            // 모든 이벤트 타입이 UNKNOWN이 아님을 확인
            val hasKnownEventTypes = result.earningsDates.any {
                it.eventType != EarningsEventType.UNKNOWN
            }
            assertThat(hasKnownEventTypes).isTrue()

            // 이벤트 타입 통계
            val eventTypeCounts = result.earningsDates.groupingBy { it.eventType }.eachCount()
            println("이벤트 타입 분포: $eventTypeCounts")
        }

        @Test
        @DisplayName("과거 실적은 epsActual과 surprisePercent를 포함한다")
        fun `past earnings include actual EPS and surprise`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.yahoo.visualization(symbol, limit = 20)

            // Then
            val pastEarningsWithActuals = result.earningsDates.filter {
                it.epsActual != null && it.surprisePercent != null
            }

            // 최소 하나의 과거 실적 데이터가 있어야 함
            assertThat(pastEarningsWithActuals).isNotEmpty()

            println("실제 EPS가 있는 과거 실적: ${pastEarningsWithActuals.size}개")
        }
    }

    @Nested
    @DisplayName("엣지 케이스")
    inner class EdgeCases {

        @Test
        @DisplayName("존재하지 않는 심볼은 빈 결과를 반환한다")
        fun `returns empty result for non-existent symbol`() = integrationTest {
            // Given
            val symbol = "NONEXISTENTSYMBOL12345"

            // When
            val result = ufc.yahoo.visualization(symbol)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.symbol).isEqualTo(symbol)
            assertThat(result.earningsDates).isEmpty()
        }

        @Test
        @DisplayName("ETF는 빈 결과를 반환한다 (실적 발표가 없음)")
        fun `returns empty result for ETF`() = integrationTest {
            // Given
            val etfSymbol = "SPY" // S&P 500 ETF

            // When
            val result = ufc.yahoo.visualization(etfSymbol)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.earningsDates).isEmpty()
        }
    }

    @Nested
    @DisplayName("Ufc 파사드 통합 테스트")
    inner class UfcFacadeIntegration {

        @Test
        @DisplayName("Ufc 파사드를 통해 실적 일정을 조회할 수 있다")
        fun `can call visualization through Ufc facade`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.visualization(symbol, limit = 10)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.symbol).isEqualTo(symbol)
            assertThat(result.earningsDates).hasSizeLessThanOrEqualTo(10)
        }
    }
}
