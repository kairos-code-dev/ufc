package com.ulalax.ufc.unit.corp

import com.ulalax.ufc.fixtures.SplitFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Split - 주식 분할 계산 테스트
 *
 * Split 클래스의 순수 계산 함수들에 대한 단위 테스트입니다.
 * Given-When-Then 패턴을 따르며, SplitFixtures를 활용합니다.
 */
@DisplayName("Split - 주식 분할 계산")
class SplitCalculationTest {

    @Nested
    @DisplayName("ratioString() - 분할 비율 문자열 반환")
    inner class RatioString {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("2:1 분할은 '2:1' 문자열을 반환한다")
            fun shouldReturnTwoForOneString() {
                // Given: 2:1 주식 분할
                val split = SplitFixtures.twoForOne()

                // When: 비율 문자열 생성
                val ratioString = split.ratioString()

                // Then: "2:1"
                assertThat(ratioString)
                    .withFailMessage(
                        "2:1 분할은 '2:1'을 반환해야 합니다. 실제값: $ratioString"
                    )
                    .isEqualTo("2:1")
            }

            @Test
            @DisplayName("4:1 분할은 '4:1' 문자열을 반환한다")
            fun shouldReturnFourForOneString() {
                // Given: 4:1 주식 분할
                val split = SplitFixtures.fourForOne()

                // When: 비율 문자열 생성
                val ratioString = split.ratioString()

                // Then: "4:1"
                assertThat(ratioString)
                    .withFailMessage(
                        "4:1 분할은 '4:1'을 반환해야 합니다. 실제값: $ratioString"
                    )
                    .isEqualTo("4:1")
            }

            @Test
            @DisplayName("1:2 역분할은 '1:2' 문자열을 반환한다")
            fun shouldReturnOneForTwoString() {
                // Given: 1:2 역분할
                val split = SplitFixtures.oneForTwo()

                // When: 비율 문자열 생성
                val ratioString = split.ratioString()

                // Then: "1:2"
                assertThat(ratioString)
                    .withFailMessage(
                        "1:2 역분할은 '1:2'를 반환해야 합니다. 실제값: $ratioString"
                    )
                    .isEqualTo("1:2")
            }

            @Test
            @DisplayName("3:2 분할은 '3:2' 문자열을 반환한다")
            fun shouldReturnThreeForTwoString() {
                // Given: 3:2 주식 분할
                val split = SplitFixtures.threeForTwo()

                // When: 비율 문자열 생성
                val ratioString = split.ratioString()

                // Then: "3:2"
                assertThat(ratioString)
                    .withFailMessage(
                        "3:2 분할은 '3:2'를 반환해야 합니다. 실제값: $ratioString"
                    )
                    .isEqualTo("3:2")
            }

            @Test
            @DisplayName("7:1 분할은 '7:1' 문자열을 반환한다")
            fun shouldReturnSevenForOneString() {
                // Given: 7:1 주식 분할 (TSLA 2020)
                val split = SplitFixtures.sevenForOne()

                // When: 비율 문자열 생성
                val ratioString = split.ratioString()

                // Then: "7:1"
                assertThat(ratioString)
                    .withFailMessage(
                        "7:1 분할은 '7:1'을 반환해야 합니다. 실제값: $ratioString"
                    )
                    .isEqualTo("7:1")
            }
        }

        @Nested
        @DisplayName("경계 조건")
        inner class EdgeCases {

            @Test
            @DisplayName("10:1 대규모 분할도 정확히 처리한다")
            fun shouldHandleLargeSplit() {
                // Given: 10:1 대규모 분할
                val split = SplitFixtures.builder()
                    .withNumerator(10)
                    .withDenominator(1)
                    .build()

                // When: 비율 문자열 생성
                val ratioString = split.ratioString()

                // Then: "10:1"
                assertThat(ratioString)
                    .withFailMessage(
                        "10:1 분할은 '10:1'을 반환해야 합니다. 실제값: $ratioString"
                    )
                    .isEqualTo("10:1")
            }

            @Test
            @DisplayName("1:10 대규모 역분할도 정확히 처리한다")
            fun shouldHandleLargeReverseSplit() {
                // Given: 1:10 대규모 역분할
                val split = SplitFixtures.builder()
                    .withNumerator(1)
                    .withDenominator(10)
                    .build()

                // When: 비율 문자열 생성
                val ratioString = split.ratioString()

                // Then: "1:10"
                assertThat(ratioString)
                    .withFailMessage(
                        "1:10 역분할은 '1:10'을 반환해야 합니다. 실제값: $ratioString"
                    )
                    .isEqualTo("1:10")
            }
        }
    }

    @Nested
    @DisplayName("description() - 분할 설명 반환")
    inner class Description {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("2:1 분할은 '2-for-1 split' 설명을 반환한다")
            fun shouldReturnTwoForOneDescription() {
                // Given: 2:1 주식 분할
                val split = SplitFixtures.twoForOne()

                // When: 설명 문자열 생성
                val description = split.description()

                // Then: "2-for-1 split"
                assertThat(description)
                    .withFailMessage(
                        "2:1 분할은 '2-for-1 split'을 반환해야 합니다. 실제값: $description"
                    )
                    .isEqualTo("2-for-1 split")
            }

            @Test
            @DisplayName("4:1 분할은 '4-for-1 split' 설명을 반환한다")
            fun shouldReturnFourForOneDescription() {
                // Given: 4:1 주식 분할
                val split = SplitFixtures.fourForOne()

                // When: 설명 문자열 생성
                val description = split.description()

                // Then: "4-for-1 split"
                assertThat(description)
                    .withFailMessage(
                        "4:1 분할은 '4-for-1 split'을 반환해야 합니다. 실제값: $description"
                    )
                    .isEqualTo("4-for-1 split")
            }

            @Test
            @DisplayName("1:2 역분할은 '1-for-2 split' 설명을 반환한다")
            fun shouldReturnOneForTwoDescription() {
                // Given: 1:2 역분할
                val split = SplitFixtures.oneForTwo()

                // When: 설명 문자열 생성
                val description = split.description()

                // Then: "1-for-2 split"
                assertThat(description)
                    .withFailMessage(
                        "1:2 역분할은 '1-for-2 split'을 반환해야 합니다. 실제값: $description"
                    )
                    .isEqualTo("1-for-2 split")
            }

            @Test
            @DisplayName("3:2 분할은 '3-for-2 split' 설명을 반환한다")
            fun shouldReturnThreeForTwoDescription() {
                // Given: 3:2 주식 분할
                val split = SplitFixtures.threeForTwo()

                // When: 설명 문자열 생성
                val description = split.description()

                // Then: "3-for-2 split"
                assertThat(description)
                    .withFailMessage(
                        "3:2 분할은 '3-for-2 split'을 반환해야 합니다. 실제값: $description"
                    )
                    .isEqualTo("3-for-2 split")
            }

            @Test
            @DisplayName("7:1 분할은 '7-for-1 split' 설명을 반환한다")
            fun shouldReturnSevenForOneDescription() {
                // Given: 7:1 주식 분할
                val split = SplitFixtures.sevenForOne()

                // When: 설명 문자열 생성
                val description = split.description()

                // Then: "7-for-1 split"
                assertThat(description)
                    .withFailMessage(
                        "7:1 분할은 '7-for-1 split'을 반환해야 합니다. 실제값: $description"
                    )
                    .isEqualTo("7-for-1 split")
            }
        }

        @Nested
        @DisplayName("경계 조건")
        inner class EdgeCases {

            @Test
            @DisplayName("10:1 대규모 분할 설명을 정확히 생성한다")
            fun shouldHandleLargeSplitDescription() {
                // Given: 10:1 대규모 분할
                val split = SplitFixtures.builder()
                    .withNumerator(10)
                    .withDenominator(1)
                    .build()

                // When: 설명 문자열 생성
                val description = split.description()

                // Then: "10-for-1 split"
                assertThat(description)
                    .withFailMessage(
                        "10:1 분할은 '10-for-1 split'을 반환해야 합니다. 실제값: $description"
                    )
                    .isEqualTo("10-for-1 split")
            }

            @Test
            @DisplayName("1:10 대규모 역분할 설명을 정확히 생성한다")
            fun shouldHandleLargeReverseSplitDescription() {
                // Given: 1:10 대규모 역분할
                val split = SplitFixtures.builder()
                    .withNumerator(1)
                    .withDenominator(10)
                    .build()

                // When: 설명 문자열 생성
                val description = split.description()

                // Then: "1-for-10 split"
                assertThat(description)
                    .withFailMessage(
                        "1:10 역분할은 '1-for-10 split'을 반환해야 합니다. 실제값: $description"
                    )
                    .isEqualTo("1-for-10 split")
            }
        }
    }

    @Nested
    @DisplayName("ratio - 분할 비율 계산")
    inner class Ratio {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("2:1 분할의 비율은 2.0이다")
            fun shouldCalculateTwoForOneRatio() {
                // Given: 2:1 주식 분할
                val split = SplitFixtures.twoForOne()

                // When: 비율 확인
                val ratio = split.ratio

                // Then: 2.0
                assertThat(ratio)
                    .withFailMessage(
                        "2:1 분할의 비율은 2.0이어야 합니다. 실제값: $ratio"
                    )
                    .isEqualTo(2.0)
            }

            @Test
            @DisplayName("4:1 분할의 비율은 4.0이다")
            fun shouldCalculateFourForOneRatio() {
                // Given: 4:1 주식 분할
                val split = SplitFixtures.fourForOne()

                // When: 비율 확인
                val ratio = split.ratio

                // Then: 4.0
                assertThat(ratio)
                    .withFailMessage(
                        "4:1 분할의 비율은 4.0이어야 합니다. 실제값: $ratio"
                    )
                    .isEqualTo(4.0)
            }

            @Test
            @DisplayName("1:2 역분할의 비율은 0.5이다")
            fun shouldCalculateOneForTwoRatio() {
                // Given: 1:2 역분할
                val split = SplitFixtures.oneForTwo()

                // When: 비율 확인
                val ratio = split.ratio

                // Then: 0.5
                assertThat(ratio)
                    .withFailMessage(
                        "1:2 역분할의 비율은 0.5여야 합니다. 실제값: $ratio"
                    )
                    .isEqualTo(0.5)
            }

            @Test
            @DisplayName("1:5 역분할의 비율은 0.2이다")
            fun shouldCalculateOneForFiveRatio() {
                // Given: 1:5 역분할
                val split = SplitFixtures.oneForFive()

                // When: 비율 확인
                val ratio = split.ratio

                // Then: 0.2
                assertThat(ratio)
                    .withFailMessage(
                        "1:5 역분할의 비율은 0.2여야 합니다. 실제값: $ratio"
                    )
                    .isEqualTo(0.2)
            }

            @Test
            @DisplayName("3:2 분할의 비율은 1.5이다")
            fun shouldCalculateThreeForTwoRatio() {
                // Given: 3:2 주식 분할
                val split = SplitFixtures.threeForTwo()

                // When: 비율 확인
                val ratio = split.ratio

                // Then: 1.5
                assertThat(ratio)
                    .withFailMessage(
                        "3:2 분할의 비율은 1.5여야 합니다. 실제값: $ratio"
                    )
                    .isEqualTo(1.5)
            }

            @Test
            @DisplayName("7:1 분할의 비율은 7.0이다")
            fun shouldCalculateSevenForOneRatio() {
                // Given: 7:1 주식 분할
                val split = SplitFixtures.sevenForOne()

                // When: 비율 확인
                val ratio = split.ratio

                // Then: 7.0
                assertThat(ratio)
                    .withFailMessage(
                        "7:1 분할의 비율은 7.0이어야 합니다. 실제값: $ratio"
                    )
                    .isEqualTo(7.0)
            }
        }

        @Nested
        @DisplayName("경계 조건")
        inner class EdgeCases {

            @Test
            @DisplayName("비율이 1보다 크면 정분할(forward split)이다")
            fun shouldBeForwardSplitWhenRatioGreaterThanOne() {
                // Given: 정분할들
                val splits = listOf(
                    SplitFixtures.twoForOne(),      // 2.0
                    SplitFixtures.threeForOne(),    // 3.0
                    SplitFixtures.fourForOne(),     // 4.0
                    SplitFixtures.sevenForOne()     // 7.0
                )

                // When & Then: 모든 비율이 1보다 큼
                splits.forEach { split ->
                    assertThat(split.ratio)
                        .withFailMessage(
                            "정분할의 비율은 1보다 커야 합니다. " +
                                    "분할: ${split.ratioString()}, 비율: ${split.ratio}"
                        )
                        .isGreaterThan(1.0)
                }
            }

            @Test
            @DisplayName("비율이 1보다 작으면 역분할(reverse split)이다")
            fun shouldBeReverseSplitWhenRatioLessThanOne() {
                // Given: 역분할들
                val splits = listOf(
                    SplitFixtures.oneForTwo(),      // 0.5
                    SplitFixtures.oneForFive()      // 0.2
                )

                // When & Then: 모든 비율이 1보다 작음
                splits.forEach { split ->
                    assertThat(split.ratio)
                        .withFailMessage(
                            "역분할의 비율은 1보다 작아야 합니다. " +
                                    "분할: ${split.ratioString()}, 비율: ${split.ratio}"
                        )
                        .isLessThan(1.0)
                }
            }

            @Test
            @DisplayName("비율이 1이면 실질적인 분할이 아니다")
            fun shouldBeNoSplitWhenRatioEqualsOne() {
                // Given: 1:1 분할 (실질적으로 변화 없음)
                val split = SplitFixtures.builder()
                    .withNumerator(1)
                    .withDenominator(1)
                    .build()

                // When: 비율 확인
                val ratio = split.ratio

                // Then: 1.0
                assertThat(ratio)
                    .withFailMessage(
                        "1:1 분할의 비율은 1.0이어야 합니다. 실제값: $ratio"
                    )
                    .isEqualTo(1.0)
            }

            @Test
            @DisplayName("매우 큰 분할 비율도 정확히 계산한다")
            fun shouldCalculateVeryLargeRatio() {
                // Given: 100:1 대규모 분할
                val split = SplitFixtures.builder()
                    .withNumerator(100)
                    .withDenominator(1)
                    .build()

                // When: 비율 확인
                val ratio = split.ratio

                // Then: 100.0
                assertThat(ratio)
                    .withFailMessage(
                        "100:1 분할의 비율은 100.0이어야 합니다. 실제값: $ratio"
                    )
                    .isEqualTo(100.0)
            }

            @Test
            @DisplayName("매우 작은 역분할 비율도 정확히 계산한다")
            fun shouldCalculateVerySmallRatio() {
                // Given: 1:100 대규모 역분할
                val split = SplitFixtures.builder()
                    .withNumerator(1)
                    .withDenominator(100)
                    .build()

                // When: 비율 확인
                val ratio = split.ratio

                // Then: 0.01
                assertThat(ratio)
                    .withFailMessage(
                        "1:100 역분할의 비율은 0.01이어야 합니다. 실제값: $ratio"
                    )
                    .isEqualTo(0.01)
            }
        }
    }
}
