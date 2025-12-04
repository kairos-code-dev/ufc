package com.ulalax.ufc.unit.domain.price

import com.ulalax.ufc.fixtures.PriceDataFixtures
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.withPrecision
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * PriceData 순수 계산 함수 테스트
 *
 * PriceData 도메인 모델의 순수 계산 함수들에 대한 단위 테스트입니다.
 * 외부 의존성 없이 입력값에 대한 계산 결과를 검증합니다.
 *
 * ## 테스트 대상 함수
 * - fiftyTwoWeekPosition(): 52주 범위 내 현재가 위치 계산
 * - dailyChangePercent(): 일중 변동률 계산
 * - isAbove50DayMA(): 50일 이동평균선 위 여부
 * - isAbove200DayMA(): 200일 이동평균선 위 여부
 *
 * ## 테스트 원칙
 * - Given-When-Then 구조
 * - 정상 케이스와 경계 조건 분리
 * - null 반환 케이스 명시적 테스트
 * - 의미 있는 실패 메시지
 */
@DisplayName("PriceData - 가격 데이터 계산 함수")
class PriceDataCalculationTest {

    @Nested
    @DisplayName("fiftyTwoWeekPosition() - 52주 범위 내 현재가 위치 계산")
    inner class FiftyTwoWeekPosition {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("52주 범위 중간 가격일 때 0.5를 반환한다")
            fun shouldReturnHalfWhenPriceIsInMiddle() {
                // Given: 52주 최저 100, 최고 200, 현재가 150
                val priceData = PriceDataFixtures.atFiftyTwoWeekMiddle()

                // When
                val position = priceData.fiftyTwoWeekPosition()

                // Then: (150-100)/(200-100) = 0.5
                assertThat(position)
                    .withFailMessage("52주 범위 중간(150)에서 위치는 0.5여야 합니다")
                    .isEqualTo(0.5)
            }

            @Test
            @DisplayName("52주 최고가일 때 1.0을 반환한다")
            fun shouldReturnOneWhenPriceIsAtHigh() {
                // Given: 52주 최저 100, 최고 200, 현재가 200
                val priceData = PriceDataFixtures.atFiftyTwoWeekHigh()

                // When
                val position = priceData.fiftyTwoWeekPosition()

                // Then: (200-100)/(200-100) = 1.0
                assertThat(position)
                    .withFailMessage("52주 최고가(200)에서 위치는 1.0이어야 합니다")
                    .isEqualTo(1.0)
            }

            @Test
            @DisplayName("52주 최저가일 때 0.0을 반환한다")
            fun shouldReturnZeroWhenPriceIsAtLow() {
                // Given: 52주 최저 100, 최고 200, 현재가 100
                val priceData = PriceDataFixtures.atFiftyTwoWeekLow()

                // When
                val position = priceData.fiftyTwoWeekPosition()

                // Then: (100-100)/(200-100) = 0.0
                assertThat(position)
                    .withFailMessage("52주 최저가(100)에서 위치는 0.0이어야 합니다")
                    .isEqualTo(0.0)
            }

            @Test
            @DisplayName("현재가가 52주 범위 25% 지점일 때 0.25를 반환한다")
            fun shouldReturnQuarterWhenPriceIsAtQuarter() {
                // Given: 52주 최저 100, 최고 200, 현재가 125
                val priceData = PriceDataFixtures.builder()
                    .withLastPrice(125.0)
                    .withFiftyTwoWeekHigh(200.0)
                    .withFiftyTwoWeekLow(100.0)
                    .build()

                // When
                val position = priceData.fiftyTwoWeekPosition()

                // Then: (125-100)/(200-100) = 0.25
                assertThat(position)
                    .withFailMessage("52주 범위 25% 지점(125)에서 위치는 0.25여야 합니다")
                    .isEqualTo(0.25)
            }

            @Test
            @DisplayName("현재가가 52주 범위 75% 지점일 때 0.75를 반환한다")
            fun shouldReturnThreeQuartersWhenPriceIsAtThreeQuarters() {
                // Given: 52주 최저 100, 최고 200, 현재가 175
                val priceData = PriceDataFixtures.builder()
                    .withLastPrice(175.0)
                    .withFiftyTwoWeekHigh(200.0)
                    .withFiftyTwoWeekLow(100.0)
                    .build()

                // When
                val position = priceData.fiftyTwoWeekPosition()

                // Then: (175-100)/(200-100) = 0.75
                assertThat(position)
                    .withFailMessage("52주 범위 75% 지점(175)에서 위치는 0.75여야 합니다")
                    .isEqualTo(0.75)
            }

            @Test
            @DisplayName("lastPrice가 없을 때 regularMarketPrice를 사용한다")
            fun shouldUseRegularMarketPriceWhenLastPriceIsNull() {
                // Given: lastPrice=null, regularMarketPrice=150
                val priceData = PriceDataFixtures.builder()
                    .withLastPrice(null)
                    .withRegularMarketPrice(150.0)
                    .withFiftyTwoWeekHigh(200.0)
                    .withFiftyTwoWeekLow(100.0)
                    .build()

                // When
                val position = priceData.fiftyTwoWeekPosition()

                // Then: regularMarketPrice로 계산
                assertThat(position)
                    .withFailMessage("lastPrice가 없을 때 regularMarketPrice(150)로 계산해야 합니다")
                    .isEqualTo(0.5)
            }
        }

        @Nested
        @DisplayName("경계 조건")
        inner class EdgeCases {

            @Test
            @DisplayName("52주 최고가와 최저가가 같을 때 null을 반환한다")
            fun shouldReturnNullWhenHighEqualsLow() {
                // Given: 52주 최고가 = 최저가 = 150
                val priceData = PriceDataFixtures.withSameFiftyTwoWeekRange()

                // When
                val position = priceData.fiftyTwoWeekPosition()

                // Then: 계산 불가 (division by zero 방지)
                assertThat(position)
                    .withFailMessage("52주 최고가와 최저가가 같으면 위치를 계산할 수 없어야 합니다")
                    .isNull()
            }

            @Test
            @DisplayName("52주 최고가 데이터가 없을 때 null을 반환한다")
            fun shouldReturnNullWhenHighIsMissing() {
                // Given: fiftyTwoWeekHigh = null
                val priceData = PriceDataFixtures.builder()
                    .withLastPrice(150.0)
                    .withFiftyTwoWeekHigh(null)
                    .withFiftyTwoWeekLow(100.0)
                    .build()

                // When
                val position = priceData.fiftyTwoWeekPosition()

                // Then
                assertThat(position)
                    .withFailMessage("52주 최고가 데이터가 없으면 null을 반환해야 합니다")
                    .isNull()
            }

            @Test
            @DisplayName("52주 최저가 데이터가 없을 때 null을 반환한다")
            fun shouldReturnNullWhenLowIsMissing() {
                // Given: fiftyTwoWeekLow = null
                val priceData = PriceDataFixtures.builder()
                    .withLastPrice(150.0)
                    .withFiftyTwoWeekHigh(200.0)
                    .withFiftyTwoWeekLow(null)
                    .build()

                // When
                val position = priceData.fiftyTwoWeekPosition()

                // Then
                assertThat(position)
                    .withFailMessage("52주 최저가 데이터가 없으면 null을 반환해야 합니다")
                    .isNull()
            }

            @Test
            @DisplayName("현재가 데이터가 없을 때 null을 반환한다")
            fun shouldReturnNullWhenPriceIsMissing() {
                // Given: lastPrice = null, regularMarketPrice = null
                val priceData = PriceDataFixtures.withoutPriceData()

                // When
                val position = priceData.fiftyTwoWeekPosition()

                // Then
                assertThat(position)
                    .withFailMessage("현재가 데이터가 없으면 null을 반환해야 합니다")
                    .isNull()
            }
        }
    }

    @Nested
    @DisplayName("dailyChangePercent() - 일중 변동률 계산")
    inner class DailyChangePercent {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("현재가가 전일 대비 5% 상승했을 때 5.0을 반환한다")
            fun shouldReturnFivePercentWhenPriceIncreasedByFivePercent() {
                // Given: 전일종가 100, 현재가 105
                val priceData = PriceDataFixtures.builder()
                    .withLastPrice(105.0)
                    .withPreviousClose(100.0)
                    .build()

                // When
                val changePercent = priceData.dailyChangePercent()

                // Then: ((105-100)/100) * 100 = 5.0%
                assertThat(changePercent)
                    .withFailMessage("5% 상승시 변동률은 5.0이어야 합니다")
                    .isCloseTo(5.0, withPrecision(0.0001))
            }

            @Test
            @DisplayName("현재가가 전일 대비 5% 하락했을 때 -5.0을 반환한다")
            fun shouldReturnNegativeFivePercentWhenPriceDecreasedByFivePercent() {
                // Given: 전일종가 100, 현재가 95
                val priceData = PriceDataFixtures.builder()
                    .withLastPrice(95.0)
                    .withPreviousClose(100.0)
                    .build()

                // When
                val changePercent = priceData.dailyChangePercent()

                // Then: ((95-100)/100) * 100 = -5.0%
                assertThat(changePercent)
                    .withFailMessage("5% 하락시 변동률은 -5.0이어야 합니다")
                    .isCloseTo(-5.0, withPrecision(0.0001))
            }

            @Test
            @DisplayName("현재가가 전일과 같을 때 0.0을 반환한다")
            fun shouldReturnZeroWhenPriceUnchanged() {
                // Given: 전일종가 = 현재가 = 100
                val priceData = PriceDataFixtures.withZeroDailyChange()

                // When
                val changePercent = priceData.dailyChangePercent()

                // Then: ((100-100)/100) * 100 = 0.0%
                assertThat(changePercent)
                    .withFailMessage("변동 없을 때 변동률은 0.0이어야 합니다")
                    .isEqualTo(0.0)
            }

            @Test
            @DisplayName("현재가가 전일 대비 10% 상승했을 때 10.0을 반환한다")
            fun shouldReturnTenPercentWhenPriceIncreasedByTenPercent() {
                // Given: 전일종가 100, 현재가 110
                val priceData = PriceDataFixtures.builder()
                    .withLastPrice(110.0)
                    .withPreviousClose(100.0)
                    .build()

                // When
                val changePercent = priceData.dailyChangePercent()

                // Then: ((110-100)/100) * 100 = 10.0%
                assertThat(changePercent)
                    .withFailMessage("10% 상승시 변동률은 10.0이어야 합니다")
                    .isCloseTo(10.0, withPrecision(0.0001))
            }

            @Test
            @DisplayName("현재가가 전일 대비 소수점 변동률을 정확히 계산한다")
            fun shouldCalculateDecimalChangePercent() {
                // Given: 전일종가 142.857, 현재가 150
                val priceData = PriceDataFixtures.withPositiveDailyChange()

                // When
                val changePercent = priceData.dailyChangePercent()

                // Then: ((150-142.857)/142.857) * 100 ≈ 5.0%
                assertThat(changePercent)
                    .withFailMessage("소수점 변동률을 정확히 계산해야 합니다")
                    .isCloseTo(5.0, withPrecision(0.01))
            }

            @Test
            @DisplayName("lastPrice가 없을 때 regularMarketPrice를 사용한다")
            fun shouldUseRegularMarketPriceWhenLastPriceIsNull() {
                // Given: lastPrice=null, regularMarketPrice=105, previousClose=100
                val priceData = PriceDataFixtures.builder()
                    .withLastPrice(null)
                    .withRegularMarketPrice(105.0)
                    .withPreviousClose(100.0)
                    .build()

                // When
                val changePercent = priceData.dailyChangePercent()

                // Then: regularMarketPrice로 계산
                assertThat(changePercent)
                    .withFailMessage("lastPrice가 없을 때 regularMarketPrice로 계산해야 합니다")
                    .isCloseTo(5.0, withPrecision(0.0001))
            }
        }

        @Nested
        @DisplayName("경계 조건")
        inner class EdgeCases {

            @Test
            @DisplayName("전일 종가가 0일 때 null을 반환한다")
            fun shouldReturnNullWhenPreviousCloseIsZero() {
                // Given: previousClose = 0
                val priceData = PriceDataFixtures.withZeroPreviousClose()

                // When
                val changePercent = priceData.dailyChangePercent()

                // Then: division by zero 방지
                assertThat(changePercent)
                    .withFailMessage("전일 종가가 0이면 변동률을 계산할 수 없어야 합니다")
                    .isNull()
            }

            @Test
            @DisplayName("전일 종가 데이터가 없을 때 null을 반환한다")
            fun shouldReturnNullWhenPreviousCloseIsMissing() {
                // Given: previousClose = null
                val priceData = PriceDataFixtures.withoutPreviousClose()

                // When
                val changePercent = priceData.dailyChangePercent()

                // Then
                assertThat(changePercent)
                    .withFailMessage("전일 종가 데이터가 없으면 null을 반환해야 합니다")
                    .isNull()
            }

            @Test
            @DisplayName("현재가 데이터가 없을 때 null을 반환한다")
            fun shouldReturnNullWhenCurrentPriceIsMissing() {
                // Given: lastPrice = null, regularMarketPrice = null
                val priceData = PriceDataFixtures.withoutPriceData()

                // When
                val changePercent = priceData.dailyChangePercent()

                // Then
                assertThat(changePercent)
                    .withFailMessage("현재가 데이터가 없으면 null을 반환해야 합니다")
                    .isNull()
            }

            @Test
            @DisplayName("현재가가 전일 대비 100% 상승했을 때 100.0을 반환한다")
            fun shouldReturnOneHundredPercentWhenPriceDoubled() {
                // Given: 전일종가 100, 현재가 200
                val priceData = PriceDataFixtures.builder()
                    .withLastPrice(200.0)
                    .withPreviousClose(100.0)
                    .build()

                // When
                val changePercent = priceData.dailyChangePercent()

                // Then: ((200-100)/100) * 100 = 100.0%
                assertThat(changePercent)
                    .withFailMessage("100% 상승시 변동률은 100.0이어야 합니다")
                    .isCloseTo(100.0, withPrecision(0.0001))
            }

            @Test
            @DisplayName("현재가가 전일 대비 50% 하락했을 때 -50.0을 반환한다")
            fun shouldReturnNegativeFiftyPercentWhenPriceHalved() {
                // Given: 전일종가 100, 현재가 50
                val priceData = PriceDataFixtures.builder()
                    .withLastPrice(50.0)
                    .withPreviousClose(100.0)
                    .build()

                // When
                val changePercent = priceData.dailyChangePercent()

                // Then: ((50-100)/100) * 100 = -50.0%
                assertThat(changePercent)
                    .withFailMessage("50% 하락시 변동률은 -50.0이어야 합니다")
                    .isCloseTo(-50.0, withPrecision(0.0001))
            }
        }
    }

    @Nested
    @DisplayName("isAbove50DayMA() - 50일 이동평균선 위 여부")
    inner class IsAbove50DayMA {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("현재가가 50일 이동평균보다 높으면 true를 반환한다")
            fun shouldReturnTrueWhenPriceIsAbove50DayMA() {
                // Given: 현재가 150, 50일 MA 140
                val priceData = PriceDataFixtures.above50DayMA()

                // When
                val isAbove = priceData.isAbove50DayMA()

                // Then
                assertThat(isAbove)
                    .withFailMessage("현재가(150)가 50일 MA(140)보다 높으면 true여야 합니다")
                    .isTrue()
            }

            @Test
            @DisplayName("현재가가 50일 이동평균보다 낮으면 false를 반환한다")
            fun shouldReturnFalseWhenPriceIsBelow50DayMA() {
                // Given: 현재가 130, 50일 MA 140
                val priceData = PriceDataFixtures.below50DayMA()

                // When
                val isAbove = priceData.isAbove50DayMA()

                // Then
                assertThat(isAbove)
                    .withFailMessage("현재가(130)가 50일 MA(140)보다 낮으면 false여야 합니다")
                    .isFalse()
            }

            @Test
            @DisplayName("현재가가 50일 이동평균과 같으면 false를 반환한다")
            fun shouldReturnFalseWhenPriceEquals50DayMA() {
                // Given: 현재가 = 50일 MA = 140
                val priceData = PriceDataFixtures.builder()
                    .withLastPrice(140.0)
                    .withFiftyDayAverage(140.0)
                    .build()

                // When
                val isAbove = priceData.isAbove50DayMA()

                // Then: price > ma50 조건 (같으면 false)
                assertThat(isAbove)
                    .withFailMessage("현재가가 50일 MA와 같으면 false여야 합니다")
                    .isFalse()
            }

            @Test
            @DisplayName("lastPrice가 없을 때 regularMarketPrice를 사용한다")
            fun shouldUseRegularMarketPriceWhenLastPriceIsNull() {
                // Given: lastPrice=null, regularMarketPrice=150, 50일 MA=140
                val priceData = PriceDataFixtures.builder()
                    .withLastPrice(null)
                    .withRegularMarketPrice(150.0)
                    .withFiftyDayAverage(140.0)
                    .build()

                // When
                val isAbove = priceData.isAbove50DayMA()

                // Then
                assertThat(isAbove)
                    .withFailMessage("lastPrice가 없을 때 regularMarketPrice로 비교해야 합니다")
                    .isTrue()
            }

            @Test
            @DisplayName("현재가가 50일 MA보다 아주 조금 높아도 true를 반환한다")
            fun shouldReturnTrueWhenPriceIsSlightlyAbove50DayMA() {
                // Given: 현재가 140.01, 50일 MA 140.00
                val priceData = PriceDataFixtures.builder()
                    .withLastPrice(140.01)
                    .withFiftyDayAverage(140.0)
                    .build()

                // When
                val isAbove = priceData.isAbove50DayMA()

                // Then
                assertThat(isAbove)
                    .withFailMessage("현재가가 50일 MA보다 조금이라도 높으면 true여야 합니다")
                    .isTrue()
            }
        }

        @Nested
        @DisplayName("경계 조건")
        inner class EdgeCases {

            @Test
            @DisplayName("50일 이동평균 데이터가 없으면 null을 반환한다")
            fun shouldReturnNullWhen50DayMAIsMissing() {
                // Given: fiftyDayAverage = null
                val priceData = PriceDataFixtures.without50DayMA()

                // When
                val isAbove = priceData.isAbove50DayMA()

                // Then
                assertThat(isAbove)
                    .withFailMessage("50일 MA 데이터가 없으면 null을 반환해야 합니다")
                    .isNull()
            }

            @Test
            @DisplayName("현재가 데이터가 없으면 null을 반환한다")
            fun shouldReturnNullWhenPriceIsMissing() {
                // Given: lastPrice = null, regularMarketPrice = null
                val priceData = PriceDataFixtures.withoutPriceData()

                // When
                val isAbove = priceData.isAbove50DayMA()

                // Then
                assertThat(isAbove)
                    .withFailMessage("현재가 데이터가 없으면 null을 반환해야 합니다")
                    .isNull()
            }
        }
    }

    @Nested
    @DisplayName("isAbove200DayMA() - 200일 이동평균선 위 여부")
    inner class IsAbove200DayMA {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("현재가가 200일 이동평균보다 높으면 true를 반환한다")
            fun shouldReturnTrueWhenPriceIsAbove200DayMA() {
                // Given: 현재가 150, 200일 MA 130
                val priceData = PriceDataFixtures.above200DayMA()

                // When
                val isAbove = priceData.isAbove200DayMA()

                // Then
                assertThat(isAbove)
                    .withFailMessage("현재가(150)가 200일 MA(130)보다 높으면 true여야 합니다")
                    .isTrue()
            }

            @Test
            @DisplayName("현재가가 200일 이동평균보다 낮으면 false를 반환한다")
            fun shouldReturnFalseWhenPriceIsBelow200DayMA() {
                // Given: 현재가 120, 200일 MA 130
                val priceData = PriceDataFixtures.below200DayMA()

                // When
                val isAbove = priceData.isAbove200DayMA()

                // Then
                assertThat(isAbove)
                    .withFailMessage("현재가(120)가 200일 MA(130)보다 낮으면 false여야 합니다")
                    .isFalse()
            }

            @Test
            @DisplayName("현재가가 200일 이동평균과 같으면 false를 반환한다")
            fun shouldReturnFalseWhenPriceEquals200DayMA() {
                // Given: 현재가 = 200일 MA = 130
                val priceData = PriceDataFixtures.builder()
                    .withLastPrice(130.0)
                    .withTwoHundredDayAverage(130.0)
                    .build()

                // When
                val isAbove = priceData.isAbove200DayMA()

                // Then: price > ma200 조건 (같으면 false)
                assertThat(isAbove)
                    .withFailMessage("현재가가 200일 MA와 같으면 false여야 합니다")
                    .isFalse()
            }

            @Test
            @DisplayName("lastPrice가 없을 때 regularMarketPrice를 사용한다")
            fun shouldUseRegularMarketPriceWhenLastPriceIsNull() {
                // Given: lastPrice=null, regularMarketPrice=150, 200일 MA=130
                val priceData = PriceDataFixtures.builder()
                    .withLastPrice(null)
                    .withRegularMarketPrice(150.0)
                    .withTwoHundredDayAverage(130.0)
                    .build()

                // When
                val isAbove = priceData.isAbove200DayMA()

                // Then
                assertThat(isAbove)
                    .withFailMessage("lastPrice가 없을 때 regularMarketPrice로 비교해야 합니다")
                    .isTrue()
            }

            @Test
            @DisplayName("현재가가 200일 MA보다 아주 조금 높아도 true를 반환한다")
            fun shouldReturnTrueWhenPriceIsSlightlyAbove200DayMA() {
                // Given: 현재가 130.01, 200일 MA 130.00
                val priceData = PriceDataFixtures.builder()
                    .withLastPrice(130.01)
                    .withTwoHundredDayAverage(130.0)
                    .build()

                // When
                val isAbove = priceData.isAbove200DayMA()

                // Then
                assertThat(isAbove)
                    .withFailMessage("현재가가 200일 MA보다 조금이라도 높으면 true여야 합니다")
                    .isTrue()
            }

            @Test
            @DisplayName("현재가가 50일 MA는 아래지만 200일 MA는 위에 있으면 true를 반환한다")
            fun shouldReturnTrueWhenPriceIsAbove200DayMAButBelow50DayMA() {
                // Given: 현재가 135, 50일 MA 140, 200일 MA 130
                val priceData = PriceDataFixtures.builder()
                    .withLastPrice(135.0)
                    .withFiftyDayAverage(140.0)
                    .withTwoHundredDayAverage(130.0)
                    .build()

                // When
                val isAbove = priceData.isAbove200DayMA()

                // Then
                assertThat(isAbove)
                    .withFailMessage("현재가(135)가 200일 MA(130) 위에 있으면 true여야 합니다")
                    .isTrue()
            }
        }

        @Nested
        @DisplayName("경계 조건")
        inner class EdgeCases {

            @Test
            @DisplayName("200일 이동평균 데이터가 없으면 null을 반환한다")
            fun shouldReturnNullWhen200DayMAIsMissing() {
                // Given: twoHundredDayAverage = null
                val priceData = PriceDataFixtures.without200DayMA()

                // When
                val isAbove = priceData.isAbove200DayMA()

                // Then
                assertThat(isAbove)
                    .withFailMessage("200일 MA 데이터가 없으면 null을 반환해야 합니다")
                    .isNull()
            }

            @Test
            @DisplayName("현재가 데이터가 없으면 null을 반환한다")
            fun shouldReturnNullWhenPriceIsMissing() {
                // Given: lastPrice = null, regularMarketPrice = null
                val priceData = PriceDataFixtures.withoutPriceData()

                // When
                val isAbove = priceData.isAbove200DayMA()

                // Then
                assertThat(isAbove)
                    .withFailMessage("현재가 데이터가 없으면 null을 반환해야 합니다")
                    .isNull()
            }
        }
    }
}
