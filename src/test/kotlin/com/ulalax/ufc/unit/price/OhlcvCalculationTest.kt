package com.ulalax.ufc.unit.domain.price

import com.ulalax.ufc.fixtures.OhlcvFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * OHLCV - 시계열 가격 데이터 계산 테스트
 *
 * OHLCV 클래스의 순수 계산 함수들에 대한 단위 테스트입니다.
 * Given-When-Then 패턴을 따르며, OhlcvFixtures를 활용합니다.
 */
@DisplayName("OHLCV - 시계열 가격 데이터 계산")
class OhlcvCalculationTest {

    @Nested
    @DisplayName("range() - 일중 가격 변동폭 계산")
    inner class Range {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("고가와 저가의 차이를 정확히 계산한다")
            fun shouldCalculateRangeCorrectly() {
                // Given: 고가 110, 저가 95인 양봉 캔들
                val ohlcv = OhlcvFixtures.builder()
                    .withHigh(110.0)
                    .withLow(95.0)
                    .build()

                // When: 변동폭을 계산
                val range = ohlcv.range()

                // Then: 고가 - 저가 = 15.0
                assertThat(range)
                    .withFailMessage(
                        "변동폭 계산 실패: 기대값=15.0, 실제값=$range, " +
                                "고가=${ohlcv.high}, 저가=${ohlcv.low}"
                    )
                    .isEqualTo(15.0)
            }

            @Test
            @DisplayName("고가와 저가가 같을 때 변동폭은 0이다")
            fun shouldReturnZeroWhenHighEqualsLow() {
                // Given: 고가와 저가가 동일한 캔들
                val ohlcv = OhlcvFixtures.builder()
                    .withHigh(100.0)
                    .withLow(100.0)
                    .withOpen(100.0)
                    .withClose(100.0)
                    .build()

                // When: 변동폭을 계산
                val range = ohlcv.range()

                // Then: 변동폭은 0
                assertThat(range)
                    .withFailMessage(
                        "고가와 저가가 같을 때 변동폭은 0이어야 함: " +
                                "실제값=$range, 고가=${ohlcv.high}, 저가=${ohlcv.low}"
                    )
                    .isEqualTo(0.0)
            }

            @Test
            @DisplayName("소수점 가격의 변동폭을 정확히 계산한다")
            fun shouldCalculateRangeWithDecimalPrices() {
                // Given: 소수점 가격을 가진 캔들
                val ohlcv = OhlcvFixtures.builder()
                    .withOpen(121.0)
                    .withHigh(123.45)
                    .withLow(120.67)
                    .withClose(122.0)
                    .build()

                // When: 변동폭을 계산
                val range = ohlcv.range()

                // Then: 정확한 소수점 계산 (부동소수점 오차 허용)
                assertThat(range)
                    .withFailMessage(
                        "소수점 변동폭 계산 실패: 기대값=2.78, 실제값=$range, " +
                                "고가=${ohlcv.high}, 저가=${ohlcv.low}"
                    )
                    .isCloseTo(2.78, org.assertj.core.data.Offset.offset(0.0001))
            }
        }

        @Nested
        @DisplayName("경계 조건")
        inner class EdgeCases {

            @Test
            @DisplayName("저가가 0일 때도 변동폭을 계산한다")
            fun shouldCalculateRangeWhenLowIsZero() {
                // Given: 저가가 0인 극단적인 경우
                val ohlcv = OhlcvFixtures.builder()
                    .withHigh(10.0)
                    .withLow(0.0)
                    .withOpen(5.0)
                    .withClose(5.0)
                    .build()

                // When: 변동폭을 계산
                val range = ohlcv.range()

                // Then: 변동폭은 고가와 동일
                assertThat(range)
                    .withFailMessage(
                        "저가가 0일 때 변동폭 계산 실패: 기대값=10.0, 실제값=$range"
                    )
                    .isEqualTo(10.0)
            }

            @Test
            @DisplayName("매우 큰 가격 범위도 정확히 계산한다")
            fun shouldCalculateRangeWithLargePrices() {
                // Given: 매우 큰 가격의 캔들
                val ohlcv = OhlcvFixtures.builder()
                    .withHigh(1_000_000.0)
                    .withLow(999_000.0)
                    .withOpen(999_500.0)
                    .withClose(999_800.0)
                    .build()

                // When: 변동폭을 계산
                val range = ohlcv.range()

                // Then: 정확한 변동폭 계산
                assertThat(range)
                    .withFailMessage(
                        "큰 가격 범위 계산 실패: 기대값=1000.0, 실제값=$range"
                    )
                    .isEqualTo(1_000.0)
            }
        }
    }

    @Nested
    @DisplayName("rangePercent() - 일중 변동률 계산")
    inner class RangePercent {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("저가 대비 변동률을 백분율로 계산한다")
            fun shouldCalculateRangePercentCorrectly() {
                // Given: 저가 100, 고가 110인 캔들 (10% 변동)
                val ohlcv = OhlcvFixtures.builder()
                    .withLow(100.0)
                    .withHigh(110.0)
                    .build()

                // When: 변동률을 계산
                val rangePercent = ohlcv.rangePercent()

                // Then: (110 - 100) / 100 * 100 = 10.0%
                assertThat(rangePercent)
                    .withFailMessage(
                        "변동률 계산 실패: 기대값=10.0%, 실제값=$rangePercent%, " +
                                "고가=${ohlcv.high}, 저가=${ohlcv.low}"
                    )
                    .isEqualTo(10.0)
            }

            @Test
            @DisplayName("고가와 저가가 같을 때 변동률은 0이다")
            fun shouldReturnZeroPercentWhenHighEqualsLow() {
                // Given: 고가와 저가가 동일한 캔들
                val ohlcv = OhlcvFixtures.builder()
                    .withHigh(100.0)
                    .withLow(100.0)
                    .withOpen(100.0)
                    .withClose(100.0)
                    .build()

                // When: 변동률을 계산
                val rangePercent = ohlcv.rangePercent()

                // Then: 변동률은 0%
                assertThat(rangePercent)
                    .withFailMessage(
                        "고가와 저가가 같을 때 변동률은 0%이어야 함: 실제값=$rangePercent%"
                    )
                    .isEqualTo(0.0)
            }

            @Test
            @DisplayName("소수점 변동률을 정확히 계산한다")
            fun shouldCalculateDecimalRangePercent() {
                // Given: 저가 200, 고가 205인 캔들 (2.5% 변동)
                val ohlcv = OhlcvFixtures.builder()
                    .withOpen(202.0)
                    .withLow(200.0)
                    .withHigh(205.0)
                    .withClose(203.0)
                    .build()

                // When: 변동률을 계산
                val rangePercent = ohlcv.rangePercent()

                // Then: 2.5%
                assertThat(rangePercent)
                    .withFailMessage(
                        "소수점 변동률 계산 실패: 기대값=2.5%, 실제값=$rangePercent%"
                    )
                    .isEqualTo(2.5)
            }
        }

        @Nested
        @DisplayName("경계 조건")
        inner class EdgeCases {

            @Test
            @DisplayName("저가가 0일 때 변동률은 0을 반환한다")
            fun shouldReturnZeroWhenLowIsZero() {
                // Given: 저가가 0인 극단적인 경우
                val ohlcv = OhlcvFixtures.builder()
                    .withLow(0.0)
                    .withHigh(10.0)
                    .withOpen(5.0)
                    .withClose(5.0)
                    .build()

                // When: 변동률을 계산
                val rangePercent = ohlcv.rangePercent()

                // Then: 0으로 나누기 방지, 0% 반환
                assertThat(rangePercent)
                    .withFailMessage(
                        "저가가 0일 때 변동률은 0%이어야 함: 실제값=$rangePercent%"
                    )
                    .isEqualTo(0.0)
            }

            @Test
            @DisplayName("매우 작은 저가에서도 변동률을 계산한다")
            fun shouldCalculateRangePercentWithSmallLow() {
                // Given: 매우 작은 저가
                val ohlcv = OhlcvFixtures.builder()
                    .withLow(0.01)
                    .withHigh(0.02)
                    .withOpen(0.015)
                    .withClose(0.018)
                    .build()

                // When: 변동률을 계산
                val rangePercent = ohlcv.rangePercent()

                // Then: (0.02 - 0.01) / 0.01 * 100 = 100%
                assertThat(rangePercent)
                    .withFailMessage(
                        "작은 저가에서 변동률 계산 실패: 기대값=100.0%, 실제값=$rangePercent%"
                    )
                    .isEqualTo(100.0)
            }

            @Test
            @DisplayName("100% 이상의 큰 변동률도 계산한다")
            fun shouldCalculateLargeRangePercent() {
                // Given: 저가 대비 3배 이상의 고가 (200% 변동)
                val ohlcv = OhlcvFixtures.builder()
                    .withLow(100.0)
                    .withHigh(300.0)
                    .withOpen(150.0)
                    .withClose(250.0)
                    .build()

                // When: 변동률을 계산
                val rangePercent = ohlcv.rangePercent()

                // Then: 200%
                assertThat(rangePercent)
                    .withFailMessage(
                        "큰 변동률 계산 실패: 기대값=200.0%, 실제값=$rangePercent%"
                    )
                    .isEqualTo(200.0)
            }
        }
    }

    @Nested
    @DisplayName("change() - 종가 기준 변동액 계산")
    inner class Change {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("종가와 시가의 차이를 정확히 계산한다")
            fun shouldCalculateChangeCorrectly() {
                // Given: 시가 100, 종가 105인 양봉
                val ohlcv = OhlcvFixtures.bullish()

                // When: 변동액을 계산
                val change = ohlcv.change()

                // Then: 105 - 100 = 5.0
                assertThat(change)
                    .withFailMessage(
                        "변동액 계산 실패: 기대값=5.0, 실제값=$change, " +
                                "시가=${ohlcv.open}, 종가=${ohlcv.close}"
                    )
                    .isEqualTo(5.0)
            }

            @Test
            @DisplayName("음봉의 경우 음수 변동액을 반환한다")
            fun shouldReturnNegativeChangeForBearish() {
                // Given: 시가 100, 종가 95인 음봉
                val ohlcv = OhlcvFixtures.bearish()

                // When: 변동액을 계산
                val change = ohlcv.change()

                // Then: 95 - 100 = -5.0
                assertThat(change)
                    .withFailMessage(
                        "음봉 변동액 계산 실패: 기대값=-5.0, 실제값=$change, " +
                                "시가=${ohlcv.open}, 종가=${ohlcv.close}"
                    )
                    .isEqualTo(-5.0)
            }

            @Test
            @DisplayName("보합의 경우 변동액은 0이다")
            fun shouldReturnZeroForFlat() {
                // Given: 시가와 종가가 같은 보합
                val ohlcv = OhlcvFixtures.flat()

                // When: 변동액을 계산
                val change = ohlcv.change()

                // Then: 변동 없음
                assertThat(change)
                    .withFailMessage(
                        "보합 변동액 계산 실패: 0이어야 하지만 실제값=$change, " +
                                "시가=${ohlcv.open}, 종가=${ohlcv.close}"
                    )
                    .isEqualTo(0.0)
            }

            @Test
            @DisplayName("소수점 가격의 변동액을 정확히 계산한다")
            fun shouldCalculateChangeWithDecimalPrices() {
                // Given: 소수점 가격
                val ohlcv = OhlcvFixtures.builder()
                    .withOpen(123.45)
                    .withClose(125.67)
                    .withLow(123.0)
                    .withHigh(126.0)
                    .build()

                // When: 변동액을 계산
                val change = ohlcv.change()

                // Then: 2.22 (부동소수점 오차 허용)
                assertThat(change)
                    .withFailMessage(
                        "소수점 변동액 계산 실패: 기대값=2.22, 실제값=$change"
                    )
                    .isCloseTo(2.22, org.assertj.core.data.Offset.offset(0.0001))
            }
        }

        @Nested
        @DisplayName("경계 조건")
        inner class EdgeCases {

            @Test
            @DisplayName("시가가 0일 때도 변동액을 계산한다")
            fun shouldCalculateChangeWhenOpenIsZero() {
                // Given: 시가가 0
                val ohlcv = OhlcvFixtures.builder()
                    .withOpen(0.0)
                    .withClose(10.0)
                    .withLow(0.0)
                    .withHigh(10.0)
                    .build()

                // When: 변동액을 계산
                val change = ohlcv.change()

                // Then: 10.0
                assertThat(change)
                    .withFailMessage(
                        "시가가 0일 때 변동액 계산 실패: 기대값=10.0, 실제값=$change"
                    )
                    .isEqualTo(10.0)
            }

            @Test
            @DisplayName("매우 큰 가격 변동도 계산한다")
            fun shouldCalculateLargeChange() {
                // Given: 큰 가격 변동
                val ohlcv = OhlcvFixtures.builder()
                    .withOpen(1_000_000.0)
                    .withClose(1_500_000.0)
                    .withLow(1_000_000.0)
                    .withHigh(1_500_000.0)
                    .build()

                // When: 변동액을 계산
                val change = ohlcv.change()

                // Then: 500,000
                assertThat(change)
                    .withFailMessage(
                        "큰 변동액 계산 실패: 기대값=500000.0, 실제값=$change"
                    )
                    .isEqualTo(500_000.0)
            }
        }
    }

    @Nested
    @DisplayName("changePercent() - 종가 기준 변동률 계산")
    inner class ChangePercent {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("시가 대비 종가 변동률을 백분율로 계산한다")
            fun shouldCalculateChangePercentCorrectly() {
                // Given: 시가 100, 종가 105인 양봉 (5% 상승)
                val ohlcv = OhlcvFixtures.bullish()

                // When: 변동률을 계산
                val changePercent = ohlcv.changePercent()

                // Then: (105 - 100) / 100 * 100 = 5.0%
                assertThat(changePercent)
                    .withFailMessage(
                        "변동률 계산 실패: 기대값=5.0%, 실제값=$changePercent%, " +
                                "시가=${ohlcv.open}, 종가=${ohlcv.close}"
                    )
                    .isEqualTo(5.0)
            }

            @Test
            @DisplayName("음봉의 경우 음수 변동률을 반환한다")
            fun shouldReturnNegativePercentForBearish() {
                // Given: 시가 100, 종가 95인 음봉 (-5% 하락)
                val ohlcv = OhlcvFixtures.bearish()

                // When: 변동률을 계산
                val changePercent = ohlcv.changePercent()

                // Then: -5.0%
                assertThat(changePercent)
                    .withFailMessage(
                        "음봉 변동률 계산 실패: 기대값=-5.0%, 실제값=$changePercent%"
                    )
                    .isEqualTo(-5.0)
            }

            @Test
            @DisplayName("보합의 경우 변동률은 0이다")
            fun shouldReturnZeroPercentForFlat() {
                // Given: 시가와 종가가 같은 보합
                val ohlcv = OhlcvFixtures.flat()

                // When: 변동률을 계산
                val changePercent = ohlcv.changePercent()

                // Then: 0%
                assertThat(changePercent)
                    .withFailMessage(
                        "보합 변동률 계산 실패: 0%이어야 하지만 실제값=$changePercent%"
                    )
                    .isEqualTo(0.0)
            }

            @Test
            @DisplayName("소수점 변동률을 정확히 계산한다")
            fun shouldCalculateDecimalChangePercent() {
                // Given: 시가 200, 종가 203인 캔들 (1.5% 상승)
                val ohlcv = OhlcvFixtures.builder()
                    .withOpen(200.0)
                    .withClose(203.0)
                    .withLow(199.0)
                    .withHigh(204.0)
                    .build()

                // When: 변동률을 계산
                val changePercent = ohlcv.changePercent()

                // Then: 1.5%
                assertThat(changePercent)
                    .withFailMessage(
                        "소수점 변동률 계산 실패: 기대값=1.5%, 실제값=$changePercent%"
                    )
                    .isEqualTo(1.5)
            }
        }

        @Nested
        @DisplayName("경계 조건")
        inner class EdgeCases {

            @Test
            @DisplayName("시가가 0일 때 변동률은 0을 반환한다")
            fun shouldReturnZeroWhenOpenIsZero() {
                // Given: 시가가 0인 극단적인 경우
                val ohlcv = OhlcvFixtures.builder()
                    .withOpen(0.0)
                    .withClose(10.0)
                    .withLow(0.0)
                    .withHigh(10.0)
                    .build()

                // When: 변동률을 계산
                val changePercent = ohlcv.changePercent()

                // Then: 0으로 나누기 방지, 0% 반환
                assertThat(changePercent)
                    .withFailMessage(
                        "시가가 0일 때 변동률은 0%이어야 함: 실제값=$changePercent%"
                    )
                    .isEqualTo(0.0)
            }

            @Test
            @DisplayName("100% 이상의 큰 변동률도 계산한다")
            fun shouldCalculateLargeChangePercent() {
                // Given: 시가 100, 종가 300 (200% 상승)
                val ohlcv = OhlcvFixtures.builder()
                    .withOpen(100.0)
                    .withClose(300.0)
                    .withLow(100.0)
                    .withHigh(300.0)
                    .build()

                // When: 변동률을 계산
                val changePercent = ohlcv.changePercent()

                // Then: 200%
                assertThat(changePercent)
                    .withFailMessage(
                        "큰 변동률 계산 실패: 기대값=200.0%, 실제값=$changePercent%"
                    )
                    .isEqualTo(200.0)
            }

            @Test
            @DisplayName("-100% 이하의 큰 하락률도 계산한다")
            fun shouldCalculateLargeNegativePercent() {
                // Given: 시가 300, 종가 100 (-66.67% 하락)
                val ohlcv = OhlcvFixtures.builder()
                    .withOpen(300.0)
                    .withClose(100.0)
                    .withLow(100.0)
                    .withHigh(300.0)
                    .build()

                // When: 변동률을 계산
                val changePercent = ohlcv.changePercent()

                // Then: 약 -66.67%
                assertThat(changePercent)
                    .withFailMessage(
                        "큰 하락률 계산 실패: 기대값=-66.67%, 실제값=$changePercent%"
                    )
                    .isCloseTo(-66.67, org.assertj.core.data.Offset.offset(0.01))
            }

            @Test
            @DisplayName("매우 작은 시가에서도 변동률을 계산한다")
            fun shouldCalculateChangePercentWithSmallOpen() {
                // Given: 매우 작은 시가
                val ohlcv = OhlcvFixtures.builder()
                    .withOpen(0.01)
                    .withClose(0.02)
                    .withLow(0.01)
                    .withHigh(0.02)
                    .build()

                // When: 변동률을 계산
                val changePercent = ohlcv.changePercent()

                // Then: 100%
                assertThat(changePercent)
                    .withFailMessage(
                        "작은 시가에서 변동률 계산 실패: 기대값=100.0%, 실제값=$changePercent%"
                    )
                    .isEqualTo(100.0)
            }
        }
    }

    @Nested
    @DisplayName("isBullish() - 양봉 여부 판단")
    inner class IsBullish {

        @Test
        @DisplayName("종가가 시가보다 높으면 true를 반환한다")
        fun shouldReturnTrueWhenCloseIsGreaterThanOpen() {
            // Given: 양봉 캔들 (시가 < 종가)
            val ohlcv = OhlcvFixtures.bullish()

            // When: 양봉 여부 확인
            val isBullish = ohlcv.isBullish()

            // Then: true
            assertThat(isBullish)
                .withFailMessage(
                    "양봉 판단 실패: true이어야 하지만 false 반환, " +
                            "시가=${ohlcv.open}, 종가=${ohlcv.close}"
                )
                .isTrue()
        }

        @Test
        @DisplayName("종가가 시가와 같으면 false를 반환한다")
        fun shouldReturnFalseWhenCloseEqualsOpen() {
            // Given: 보합 캔들 (시가 = 종가)
            val ohlcv = OhlcvFixtures.flat()

            // When: 양봉 여부 확인
            val isBullish = ohlcv.isBullish()

            // Then: false (보합은 양봉이 아님)
            assertThat(isBullish)
                .withFailMessage(
                    "보합 판단 실패: false이어야 하지만 true 반환, " +
                            "시가=${ohlcv.open}, 종가=${ohlcv.close}"
                )
                .isFalse()
        }

        @Test
        @DisplayName("종가가 시가보다 낮으면 false를 반환한다")
        fun shouldReturnFalseWhenCloseIsLessThanOpen() {
            // Given: 음봉 캔들 (시가 > 종가)
            val ohlcv = OhlcvFixtures.bearish()

            // When: 양봉 여부 확인
            val isBullish = ohlcv.isBullish()

            // Then: false
            assertThat(isBullish)
                .withFailMessage(
                    "음봉 판단 실패: false이어야 하지만 true 반환, " +
                            "시가=${ohlcv.open}, 종가=${ohlcv.close}"
                )
                .isFalse()
        }

        @Test
        @DisplayName("아주 작은 차이라도 종가가 높으면 true를 반환한다")
        fun shouldReturnTrueForSmallDifference() {
            // Given: 아주 작은 상승
            val ohlcv = OhlcvFixtures.builder()
                .withOpen(100.0)
                .withClose(100.01)
                .build()

            // When: 양봉 여부 확인
            val isBullish = ohlcv.isBullish()

            // Then: true (작은 차이라도 양봉)
            assertThat(isBullish)
                .withFailMessage(
                    "미세한 상승도 양봉으로 판단해야 함: " +
                            "시가=${ohlcv.open}, 종가=${ohlcv.close}"
                )
                .isTrue()
        }

        @Test
        @DisplayName("상승 추세의 모든 캔들이 양봉으로 판단된다")
        fun shouldIdentifyAllBullishCandlesInUptrend() {
            // Given: 상승 추세 데이터
            val uptrend = OhlcvFixtures.uptrend()

            // When: 모든 캔들의 양봉 여부 확인
            val allBullish = uptrend.all { it.isBullish() }

            // Then: 모두 양봉
            assertThat(allBullish)
                .withFailMessage(
                    "상승 추세의 모든 캔들은 양봉이어야 함: " +
                            "양봉 개수=${uptrend.count { it.isBullish() }}, " +
                            "전체=${uptrend.size}"
                )
                .isTrue()
        }
    }

    @Nested
    @DisplayName("isBearish() - 음봉 여부 판단")
    inner class IsBearish {

        @Test
        @DisplayName("종가가 시가보다 낮으면 true를 반환한다")
        fun shouldReturnTrueWhenCloseIsLessThanOpen() {
            // Given: 음봉 캔들 (시가 > 종가)
            val ohlcv = OhlcvFixtures.bearish()

            // When: 음봉 여부 확인
            val isBearish = ohlcv.isBearish()

            // Then: true
            assertThat(isBearish)
                .withFailMessage(
                    "음봉 판단 실패: true이어야 하지만 false 반환, " +
                            "시가=${ohlcv.open}, 종가=${ohlcv.close}"
                )
                .isTrue()
        }

        @Test
        @DisplayName("종가가 시가와 같으면 false를 반환한다")
        fun shouldReturnFalseWhenCloseEqualsOpen() {
            // Given: 보합 캔들 (시가 = 종가)
            val ohlcv = OhlcvFixtures.flat()

            // When: 음봉 여부 확인
            val isBearish = ohlcv.isBearish()

            // Then: false (보합은 음봉이 아님)
            assertThat(isBearish)
                .withFailMessage(
                    "보합 판단 실패: false이어야 하지만 true 반환, " +
                            "시가=${ohlcv.open}, 종가=${ohlcv.close}"
                )
                .isFalse()
        }

        @Test
        @DisplayName("종가가 시가보다 높으면 false를 반환한다")
        fun shouldReturnFalseWhenCloseIsGreaterThanOpen() {
            // Given: 양봉 캔들 (시가 < 종가)
            val ohlcv = OhlcvFixtures.bullish()

            // When: 음봉 여부 확인
            val isBearish = ohlcv.isBearish()

            // Then: false
            assertThat(isBearish)
                .withFailMessage(
                    "양봉 판단 실패: false이어야 하지만 true 반환, " +
                            "시가=${ohlcv.open}, 종가=${ohlcv.close}"
                )
                .isFalse()
        }

        @Test
        @DisplayName("아주 작은 차이라도 종가가 낮으면 true를 반환한다")
        fun shouldReturnTrueForSmallDifference() {
            // Given: 아주 작은 하락
            val ohlcv = OhlcvFixtures.builder()
                .withOpen(100.0)
                .withClose(99.99)
                .withLow(99.99)
                .build()

            // When: 음봉 여부 확인
            val isBearish = ohlcv.isBearish()

            // Then: true (작은 차이라도 음봉)
            assertThat(isBearish)
                .withFailMessage(
                    "미세한 하락도 음봉으로 판단해야 함: " +
                            "시가=${ohlcv.open}, 종가=${ohlcv.close}"
                )
                .isTrue()
        }

        @Test
        @DisplayName("하락 추세의 모든 캔들이 음봉으로 판단된다")
        fun shouldIdentifyAllBearishCandlesInDowntrend() {
            // Given: 하락 추세 데이터
            val downtrend = OhlcvFixtures.downtrend()

            // When: 모든 캔들의 음봉 여부 확인
            val allBearish = downtrend.all { it.isBearish() }

            // Then: 모두 음봉
            assertThat(allBearish)
                .withFailMessage(
                    "하락 추세의 모든 캔들은 음봉이어야 함: " +
                            "음봉 개수=${downtrend.count { it.isBearish() }}, " +
                            "전체=${downtrend.size}"
                )
                .isTrue()
        }

        @Test
        @DisplayName("양봉과 음봉은 상호 배타적이다")
        fun shouldBeMutuallyExclusiveWithBullish() {
            // Given: 여러 타입의 캔들
            val bullish = OhlcvFixtures.bullish()
            val bearish = OhlcvFixtures.bearish()
            val flat = OhlcvFixtures.flat()

            // When & Then: 양봉과 음봉은 동시에 true일 수 없음
            assertThat(bullish.isBullish() && bullish.isBearish())
                .withFailMessage("양봉은 동시에 음봉일 수 없음")
                .isFalse()

            assertThat(bearish.isBullish() && bearish.isBearish())
                .withFailMessage("음봉은 동시에 양봉일 수 없음")
                .isFalse()

            assertThat(flat.isBullish() || flat.isBearish())
                .withFailMessage("보합은 양봉도 음봉도 아님")
                .isFalse()
        }
    }
}
