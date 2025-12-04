package com.ulalax.ufc.integration

import com.ulalax.ufc.api.client.Ufc
import com.ulalax.ufc.api.client.UfcClientConfig
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.domain.chart.ChartEventType
import com.ulalax.ufc.domain.common.Interval
import com.ulalax.ufc.domain.common.Period
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitingSettings
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*

/**
 * Chart API - 통합 테스트
 *
 * 이 테스트는 실제 Yahoo Finance Chart API를 호출합니다.
 * Mock이나 Fake 객체를 사용하지 않고, 실제 네트워크 요청을 통해
 * ufc.chart() 메서드의 모든 시나리오를 검증합니다.
 *
 * ## 테스트 환경
 * - 실제 Yahoo Finance Chart API 호출
 * - 네트워크 연결 필요
 * - API Rate Limiting 적용
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew test --tests "*ChartIntegrationSpec" --tests "*.integration.*"
 * ```
 *
 * ## 주의사항
 * - 네트워크 상태에 따라 테스트 실패 가능
 * - API 변경으로 인한 실패 가능
 * - Rate Limiting으로 인한 지연 발생 가능
 * - 이벤트 데이터는 심볼에 따라 존재하지 않을 수 있음
 */
@Tag("integration")
@DisplayName("Chart API - 통합 테스트")
class ChartIntegrationSpec {

    private lateinit var ufc: Ufc

    @BeforeEach
    fun setUp() = runBlocking {
        ufc = Ufc.create(
            UfcClientConfig(
                rateLimitingSettings = RateLimitingSettings()
            )
        )
    }

    @AfterEach
    fun tearDown() {
        ufc.close()
    }

    @Nested
    @DisplayName("기본 OHLCV 데이터 조회")
    inner class BasicOHLCVData {

        @Test
        @DisplayName("이벤트 없이 OHLCV 데이터만 조회한다")
        fun shouldReturnOHLCVDataWithoutEvents() = runTest {
            // Given: AAPL 심볼, 이벤트 미지정

            // When
            val result = ufc.chart(
                symbol = "AAPL",
                interval = Interval.OneDay,
                period = Period.OneMonth
            )

            // Then: OHLCV 데이터 존재
            assertThat(result.prices)
                .withFailMessage("OHLCV 데이터는 항상 존재해야 합니다")
                .isNotEmpty

            assertThat(result.prices.size)
                .withFailMessage("1개월 일봉 데이터는 최소 15개 이상이어야 합니다")
                .isGreaterThanOrEqualTo(15)

            // 요청한 이벤트 없음
            assertThat(result.requestedEvents)
                .withFailMessage("이벤트를 요청하지 않았으므로 빈 Set이어야 합니다")
                .isEmpty()

            // 이벤트 데이터는 null 또는 비어있음
            assertThat(result.events?.dividends).isNullOrEmpty()
            assertThat(result.events?.splits).isNullOrEmpty()
            assertThat(result.events?.capitalGains).isNullOrEmpty()
        }

        @Test
        @DisplayName("OHLCV 데이터의 메타정보를 포함한다")
        fun shouldIncludeMetadata() = runTest {
            // When
            val result = ufc.chart(
                symbol = "AAPL",
                interval = Interval.OneDay,
                period = Period.OneMonth
            )

            // Then: 메타데이터 검증
            assertThat(result.meta.symbol)
                .withFailMessage("메타데이터에 심볼이 있어야 합니다")
                .isEqualTo("AAPL")

            assertThat(result.meta.currency)
                .withFailMessage("메타데이터에 통화 정보가 있어야 합니다")
                .isNotBlank()

            assertThat(result.meta.regularMarketPrice)
                .withFailMessage("메타데이터에 현재가가 있어야 합니다")
                .isNotNull()
                .isGreaterThan(0.0)
        }

        @Test
        @DisplayName("OHLCV 데이터가 시간순으로 정렬되어 있다")
        fun shouldBeSortedByTimestamp() = runTest {
            // When
            val result = ufc.chart(
                symbol = "AAPL",
                interval = Interval.OneDay,
                period = Period.OneMonth
            )

            // Then: 타임스탬프 오름차순 정렬
            val timestamps = result.prices.map { it.timestamp }
            assertThat(timestamps)
                .withFailMessage("타임스탬프가 오름차순으로 정렬되어야 합니다")
                .isSorted()
        }

        @Test
        @DisplayName("OHLCV 데이터의 가격 정합성이 유지된다")
        fun shouldMaintainPriceIntegrity() = runTest {
            // When
            val result = ufc.chart(
                symbol = "AAPL",
                interval = Interval.OneDay,
                period = Period.OneMonth
            )

            // Then: 모든 데이터 포인트의 정합성 검증
            result.prices.forEachIndexed { index, ohlcv ->
                assertThat(ohlcv.high)
                    .withFailMessage("인덱스 $index: High는 Low보다 크거나 같아야 합니다")
                    .isGreaterThanOrEqualTo(ohlcv.low)

                assertThat(ohlcv.open)
                    .withFailMessage("인덱스 $index: Open은 Low ~ High 범위 내에 있어야 합니다")
                    .isBetween(ohlcv.low, ohlcv.high)

                assertThat(ohlcv.close)
                    .withFailMessage("인덱스 $index: Close는 Low ~ High 범위 내에 있어야 합니다")
                    .isBetween(ohlcv.low, ohlcv.high)

                assertThat(ohlcv.volume)
                    .withFailMessage("인덱스 $index: Volume은 0 이상이어야 합니다")
                    .isGreaterThanOrEqualTo(0)
            }
        }
    }

    @Nested
    @DisplayName("배당 이벤트 포함 조회")
    inner class DividendEventQuery {

        @Test
        @DisplayName("배당 이벤트를 포함하여 조회한다")
        fun shouldReturnWithDividendEvents() = runTest {
            // Given: AAPL 심볼 (배당 지급 주식), 배당 이벤트 요청

            // When: 1년 데이터 + 배당 이벤트
            val result = ufc.chart(
                symbol = "AAPL",
                interval = Interval.OneDay,
                period = Period.OneYear,
                ChartEventType.DIVIDEND
            )

            // Then: 요청한 이벤트 확인
            assertThat(result.requestedEvents)
                .withFailMessage("요청한 이벤트는 DIVIDEND만 있어야 합니다")
                .containsExactly(ChartEventType.DIVIDEND)

            // OHLCV 데이터 존재
            assertThat(result.prices)
                .withFailMessage("OHLCV 데이터는 항상 존재해야 합니다")
                .isNotEmpty

            // hasEvent 확인
            assertThat(result.hasEvent(ChartEventType.DIVIDEND))
                .withFailMessage("배당 이벤트를 요청했으므로 true여야 합니다")
                .isTrue()
        }

        @Test
        @DisplayName("getDividends()로 배당 이벤트 데이터를 가져온다")
        fun shouldGetDividendEventsData() = runTest {
            // When: 5년 데이터 + 배당 이벤트 (충분한 배당 이력)
            val result = ufc.chart(
                symbol = "AAPL",
                interval = Interval.OneDay,
                period = Period.FiveYears,
                ChartEventType.DIVIDEND
            )

            // Then: getDividends() 호출
            val dividends = result.getDividends()

            // AAPL은 분기 배당을 지급하므로 데이터가 있을 가능성이 높음
            if (dividends != null && dividends.isNotEmpty()) {
                assertThat(dividends)
                    .withFailMessage("5년간 AAPL의 배당 이벤트가 최소 10개는 있어야 합니다")
                    .hasSizeGreaterThanOrEqualTo(10)

                // 각 배당 이벤트 검증
                dividends.forEach { (timestamp, event) ->
                    assertThat(timestamp)
                        .withFailMessage("타임스탬프가 비어있지 않아야 합니다")
                        .isNotBlank()

                    assertThat(event.amount)
                        .withFailMessage("배당금액은 0보다 커야 합니다")
                        .isNotNull()
                        .isGreaterThan(0.0)
                }
            }
        }

        @Test
        @DisplayName("배당을 지급하지 않는 주식은 배당 이벤트가 없다")
        fun shouldHaveNoDividendsForNonDividendStock() = runTest {
            // Given: TSLA (역사적으로 배당 미지급 주식)

            // When
            val result = ufc.chart(
                symbol = "TSLA",
                interval = Interval.OneDay,
                period = Period.OneYear,
                ChartEventType.DIVIDEND
            )

            // Then: 배당 이벤트는 요청했지만 데이터는 없거나 비어있음
            assertThat(result.hasEvent(ChartEventType.DIVIDEND))
                .withFailMessage("배당 이벤트를 요청했으므로 true여야 합니다")
                .isTrue()

            val dividends = result.getDividends()
            // TSLA는 배당을 지급하지 않으므로 null이거나 비어있음
            if (dividends != null) {
                assertThat(dividends)
                    .withFailMessage("TSLA는 배당을 지급하지 않으므로 배당 이벤트가 없거나 적어야 합니다")
                    .hasSizeLessThan(3)
            }
        }
    }

    @Nested
    @DisplayName("여러 이벤트 포함 조회")
    inner class MultipleEventsQuery {

        @Test
        @DisplayName("배당과 분할 이벤트를 동시에 조회한다")
        fun shouldReturnDividendAndSplitEvents() = runTest {
            // Given: AAPL 심볼, 배당 + 분할 이벤트

            // When: 10년 데이터 (주식분할 이력 포함 가능성)
            val result = ufc.chart(
                symbol = "AAPL",
                interval = Interval.OneDay,
                period = Period.TenYears,
                ChartEventType.DIVIDEND,
                ChartEventType.SPLIT
            )

            // Then: 요청한 이벤트 확인
            assertThat(result.requestedEvents)
                .withFailMessage("요청한 이벤트는 DIVIDEND, SPLIT이어야 합니다")
                .containsExactlyInAnyOrder(
                    ChartEventType.DIVIDEND,
                    ChartEventType.SPLIT
                )

            // hasEvent 확인
            assertThat(result.hasEvent(ChartEventType.DIVIDEND))
                .withFailMessage("배당 이벤트를 요청했으므로 true여야 합니다")
                .isTrue()

            assertThat(result.hasEvent(ChartEventType.SPLIT))
                .withFailMessage("분할 이벤트를 요청했으므로 true여야 합니다")
                .isTrue()

            // OHLCV 데이터 존재
            assertThat(result.prices)
                .isNotEmpty
        }

        @Test
        @DisplayName("3개 모든 이벤트를 동시에 조회한다")
        fun shouldReturnAllThreeEvents() = runTest {
            // Given: 모든 이벤트 타입 요청

            // When
            val result = ufc.chart(
                symbol = "AAPL",
                interval = Interval.OneDay,
                period = Period.FiveYears,
                ChartEventType.DIVIDEND,
                ChartEventType.SPLIT,
                ChartEventType.CAPITAL_GAIN
            )

            // Then: 요청한 이벤트 확인
            assertThat(result.requestedEvents)
                .hasSize(3)
                .containsExactlyInAnyOrder(
                    ChartEventType.DIVIDEND,
                    ChartEventType.SPLIT,
                    ChartEventType.CAPITAL_GAIN
                )

            // 각 이벤트 hasEvent true
            assertThat(result.hasEvent(ChartEventType.DIVIDEND)).isTrue()
            assertThat(result.hasEvent(ChartEventType.SPLIT)).isTrue()
            assertThat(result.hasEvent(ChartEventType.CAPITAL_GAIN)).isTrue()
        }

        @Test
        @DisplayName("getSplits()로 주식 분할 이벤트를 가져온다")
        fun shouldGetSplitEventsData() = runTest {
            // Given: AAPL (2020년 8월 4:1 분할 이력)

            // When: 충분한 기간으로 분할 이력 포함
            val result = ufc.chart(
                symbol = "AAPL",
                interval = Interval.OneDay,
                period = Period.TenYears,
                ChartEventType.SPLIT
            )

            // Then: getSplits() 호출
            val splits = result.getSplits()

            // AAPL은 2020년에 주식분할을 했으므로 데이터가 있을 수 있음
            if (splits != null && splits.isNotEmpty()) {
                // 각 분할 이벤트 검증
                splits.forEach { (timestamp, event) ->
                    assertThat(timestamp)
                        .withFailMessage("타임스탬프가 비어있지 않아야 합니다")
                        .isNotBlank()

                    assertThat(event.numerator)
                        .withFailMessage("분자(numerator)가 있어야 합니다")
                        .isNotNull()
                        .isGreaterThan(0.0)

                    assertThat(event.denominator)
                        .withFailMessage("분모(denominator)가 있어야 합니다")
                        .isNotNull()
                        .isGreaterThan(0.0)
                }
            }
        }

        @Test
        @DisplayName("ETF의 자본이득 이벤트를 조회한다")
        fun shouldGetCapitalGainEventsForETF() = runTest {
            // Given: SPY ETF, 자본이득 이벤트
            // 주의: 모든 ETF가 자본이득을 분배하는 것은 아님

            // When
            val result = ufc.chart(
                symbol = "SPY",
                interval = Interval.OneDay,
                period = Period.FiveYears,
                ChartEventType.CAPITAL_GAIN
            )

            // Then: 자본이득 이벤트 요청됨
            assertThat(result.hasEvent(ChartEventType.CAPITAL_GAIN))
                .withFailMessage("자본이득 이벤트를 요청했으므로 true여야 합니다")
                .isTrue()

            // getCapitalGains() 호출
            val capitalGains = result.getCapitalGains()

            // SPY는 자본이득 분배가 적거나 없을 수 있음 (null 허용)
            if (capitalGains != null && capitalGains.isNotEmpty()) {
                capitalGains.forEach { (timestamp, event) ->
                    assertThat(timestamp).isNotBlank()
                    assertThat(event.amount).isNotNull()
                }
            }
        }
    }

    @Nested
    @DisplayName("requestedEvents 확인")
    inner class RequestedEventsVerification {

        @Test
        @DisplayName("requestedEvents는 요청한 이벤트 목록을 정확히 반환한다")
        fun shouldReturnExactRequestedEvents() = runTest {
            // Given: 특정 이벤트 세트
            val events = arrayOf(
                ChartEventType.DIVIDEND,
                ChartEventType.SPLIT
            )

            // When
            val result = ufc.chart(
                symbol = "AAPL",
                interval = Interval.OneDay,
                period = Period.OneYear,
                *events
            )

            // Then: 요청한 이벤트 목록과 정확히 일치
            assertThat(result.requestedEvents)
                .containsExactlyInAnyOrder(*events)
        }

        @Test
        @DisplayName("이벤트를 요청하지 않으면 requestedEvents는 빈 Set이다")
        fun shouldReturnEmptySetWhenNoEventsRequested() = runTest {
            // When: 이벤트 미요청
            val result = ufc.chart(
                symbol = "AAPL",
                interval = Interval.OneDay,
                period = Period.OneMonth
            )

            // Then
            assertThat(result.requestedEvents)
                .withFailMessage("이벤트를 요청하지 않았으므로 빈 Set이어야 합니다")
                .isEmpty()
        }
    }

    @Nested
    @DisplayName("hasEvent 동작 확인")
    inner class HasEventBehavior {

        @Test
        @DisplayName("hasEvent()는 요청한 이벤트에 대해 true를 반환한다")
        fun shouldReturnTrueForRequestedEvents() = runTest {
            // When: 배당 이벤트만 요청
            val result = ufc.chart(
                symbol = "AAPL",
                interval = Interval.OneDay,
                period = Period.OneYear,
                ChartEventType.DIVIDEND
            )

            // Then: 요청한 이벤트는 true
            assertThat(result.hasEvent(ChartEventType.DIVIDEND))
                .withFailMessage("요청한 배당 이벤트는 true여야 합니다")
                .isTrue()
        }

        @Test
        @DisplayName("hasEvent()는 요청하지 않은 이벤트에 대해 false를 반환한다")
        fun shouldReturnFalseForNonRequestedEvents() = runTest {
            // When: 배당 이벤트만 요청
            val result = ufc.chart(
                symbol = "AAPL",
                interval = Interval.OneDay,
                period = Period.OneYear,
                ChartEventType.DIVIDEND
            )

            // Then: 요청하지 않은 이벤트는 false
            assertThat(result.hasEvent(ChartEventType.SPLIT))
                .withFailMessage("요청하지 않은 분할 이벤트는 false여야 합니다")
                .isFalse()

            assertThat(result.hasEvent(ChartEventType.CAPITAL_GAIN))
                .withFailMessage("요청하지 않은 자본이득 이벤트는 false여야 합니다")
                .isFalse()
        }

        @Test
        @DisplayName("이벤트를 요청했지만 데이터가 없어도 hasEvent()는 true를 반환한다")
        fun shouldReturnTrueEvenIfNoEventData() = runTest {
            // Given: 배당 미지급 주식 (TSLA)

            // When: 배당 이벤트 요청
            val result = ufc.chart(
                symbol = "TSLA",
                interval = Interval.OneDay,
                period = Period.OneYear,
                ChartEventType.DIVIDEND
            )

            // Then: 요청은 했으므로 hasEvent는 true
            assertThat(result.hasEvent(ChartEventType.DIVIDEND))
                .withFailMessage("요청한 이벤트는 데이터 유무와 관계없이 true여야 합니다")
                .isTrue()

            // 하지만 실제 데이터는 없거나 적음
            val dividends = result.getDividends()
            if (dividends != null) {
                assertThat(dividends).hasSizeLessThan(3)
            }
        }
    }

    @Nested
    @DisplayName("다양한 interval/period 조합 테스트")
    inner class IntervalPeriodCombinations {

        @Test
        @DisplayName("1일 1분봉 + 배당 이벤트 조합을 조회한다")
        fun shouldReturnOneDayOneMinuteWithDividend() = runTest {
            // When
            val result = ufc.chart(
                symbol = "AAPL",
                interval = Interval.OneMinute,
                period = Period.OneDay,
                ChartEventType.DIVIDEND
            )

            // Then: OHLCV 데이터 존재
            assertThat(result.prices)
                .withFailMessage("1분봉 데이터가 있어야 합니다")
                .isNotEmpty

            // 이벤트 요청 확인
            assertThat(result.hasEvent(ChartEventType.DIVIDEND)).isTrue()
        }

        @Test
        @DisplayName("1개월 1시간봉 + 분할 이벤트 조합을 조회한다")
        fun shouldReturnOneMonthOneHourWithSplit() = runTest {
            // When
            val result = ufc.chart(
                symbol = "AAPL",
                interval = Interval.OneHour,
                period = Period.OneMonth,
                ChartEventType.SPLIT
            )

            // Then
            assertThat(result.prices)
                .withFailMessage("1시간봉 데이터가 있어야 합니다")
                .isNotEmpty

            assertThat(result.hasEvent(ChartEventType.SPLIT)).isTrue()
        }

        @Test
        @DisplayName("1년 주봉 + 여러 이벤트 조합을 조회한다")
        fun shouldReturnOneYearWeeklyWithMultipleEvents() = runTest {
            // When
            val result = ufc.chart(
                symbol = "AAPL",
                interval = Interval.OneWeek,
                period = Period.OneYear,
                ChartEventType.DIVIDEND,
                ChartEventType.SPLIT
            )

            // Then: 주봉 데이터 (약 52개)
            assertThat(result.prices.size)
                .withFailMessage("1년 주봉 데이터는 최소 45개 이상이어야 합니다")
                .isGreaterThanOrEqualTo(45)

            // 여러 이벤트 요청 확인
            assertThat(result.requestedEvents).hasSize(2)
        }

        @Test
        @DisplayName("5년 월봉 + 모든 이벤트 조합을 조회한다")
        fun shouldReturnFiveYearsMonthlyWithAllEvents() = runTest {
            // When
            val result = ufc.chart(
                symbol = "AAPL",
                interval = Interval.OneMonth,
                period = Period.FiveYears,
                ChartEventType.DIVIDEND,
                ChartEventType.SPLIT,
                ChartEventType.CAPITAL_GAIN
            )

            // Then: 월봉 데이터 (약 60개)
            assertThat(result.prices.size)
                .withFailMessage("5년 월봉 데이터는 최소 50개 이상이어야 합니다")
                .isGreaterThanOrEqualTo(50)

            // 모든 이벤트 요청 확인
            assertThat(result.requestedEvents).hasSize(3)
        }

        @Test
        @DisplayName("10년 일봉 + 배당 이벤트로 대용량 데이터를 조회한다")
        fun shouldReturnLargeDatasetWithEvents() = runTest {
            // When: 10년 일봉 (약 2500개) + 배당 이벤트
            val result = ufc.chart(
                symbol = "AAPL",
                interval = Interval.OneDay,
                period = Period.TenYears,
                ChartEventType.DIVIDEND
            )

            // Then: 대용량 데이터 검증
            assertThat(result.prices.size)
                .withFailMessage("10년 일봉 데이터는 최소 2000개 이상이어야 합니다")
                .isGreaterThanOrEqualTo(2000)

            // 배당 이벤트 확인
            val dividends = result.getDividends()
            if (dividends != null) {
                assertThat(dividends)
                    .withFailMessage("10년간 AAPL의 배당 이벤트가 최소 30개는 있어야 합니다")
                    .hasSizeGreaterThanOrEqualTo(30)
            }

            // 모든 데이터의 정합성 유지
            result.prices.forEach { ohlcv ->
                assertThat(ohlcv.high).isGreaterThanOrEqualTo(ohlcv.low)
            }
        }
    }

    @Nested
    @DisplayName("다양한 심볼 타입")
    inner class VariousSymbolTypes {

        @Test
        @DisplayName("주식 심볼의 차트 데이터를 조회한다")
        fun shouldReturnChartDataForStock() = runTest {
            // When
            val result = ufc.chart(
                symbol = "MSFT",
                interval = Interval.OneDay,
                period = Period.OneMonth,
                ChartEventType.DIVIDEND
            )

            // Then
            assertThat(result.prices).isNotEmpty
            assertThat(result.meta.symbol).isEqualTo("MSFT")
        }

        @Test
        @DisplayName("ETF 심볼의 차트 데이터를 조회한다")
        fun shouldReturnChartDataForETF() = runTest {
            // When
            val result = ufc.chart(
                symbol = "SPY",
                interval = Interval.OneDay,
                period = Period.OneMonth,
                ChartEventType.DIVIDEND
            )

            // Then
            assertThat(result.prices).isNotEmpty
            assertThat(result.meta.symbol).isEqualTo("SPY")
        }

        @Test
        @DisplayName("인덱스 심볼의 차트 데이터를 조회한다")
        fun shouldReturnChartDataForIndex() = runTest {
            // When: S&P 500 인덱스
            val result = ufc.chart(
                symbol = "^GSPC",
                interval = Interval.OneDay,
                period = Period.OneMonth
            )

            // Then
            assertThat(result.prices).isNotEmpty
            assertThat(result.meta.symbol).isEqualTo("^GSPC")

            // 인덱스는 배당이 없음
            assertThat(result.requestedEvents).isEmpty()
        }
    }

    @Nested
    @DisplayName("입력 검증")
    inner class InputValidation {

        @Test
        @DisplayName("빈 문자열 심볼 조회 시 에러를 발생시킨다")
        fun shouldThrowErrorForBlankSymbol() = runTest {
            // When & Then
            assertThatThrownBy {
                runBlocking {
                    ufc.chart(
                        symbol = "",
                        interval = Interval.OneDay,
                        period = Period.OneMonth
                    )
                }
            }
                .isInstanceOf(UfcException::class.java)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SYMBOL)
        }

        @Test
        @DisplayName("존재하지 않는 심볼 조회 시 적절한 에러를 발생시킨다")
        fun shouldThrowErrorForNonExistentSymbol() = runTest {
            // Given: 존재하지 않는 심볼
            val invalidSymbol = "INVALIDXXX"

            // When & Then
            val exception = try {
                ufc.chart(
                    symbol = invalidSymbol,
                    interval = Interval.OneDay,
                    period = Period.OneMonth
                )
                null
            } catch (e: UfcException) {
                e
            }

            assertThat(exception)
                .withFailMessage("존재하지 않는 심볼 조회 시 UfcException이 발생해야 합니다")
                .isNotNull

            assertThat(exception!!.errorCode)
                .withFailMessage("에러 코드가 적절해야 합니다")
                .isIn(
                    ErrorCode.DATA_NOT_FOUND,
                    ErrorCode.EXTERNAL_API_ERROR
                )
        }
    }

    @Nested
    @DisplayName("이벤트 데이터 접근 메서드")
    inner class EventDataAccessMethods {

        @Test
        @DisplayName("요청하지 않은 이벤트의 getter는 null을 반환한다")
        fun shouldReturnNullForNonRequestedEventGetter() = runTest {
            // When: 배당 이벤트만 요청
            val result = ufc.chart(
                symbol = "AAPL",
                interval = Interval.OneDay,
                period = Period.OneYear,
                ChartEventType.DIVIDEND
            )

            // Then: 요청하지 않은 이벤트의 getter는 null
            assertThat(result.getSplits())
                .withFailMessage("요청하지 않은 분할 이벤트는 null이어야 합니다")
                .isNull()

            assertThat(result.getCapitalGains())
                .withFailMessage("요청하지 않은 자본이득 이벤트는 null이어야 합니다")
                .isNull()

            // 요청한 이벤트는 non-null (데이터가 없어도 빈 맵일 수 있음)
            val dividends = result.getDividends()
            // AAPL은 배당을 지급하므로 null이 아니거나, null이면 hasEvent는 여전히 true
            if (dividends != null) {
                assertThat(dividends).isInstanceOf(Map::class.java)
            }
        }

        @Test
        @DisplayName("모든 이벤트를 요청하면 모든 getter가 사용 가능하다")
        fun shouldEnableAllGettersWhenAllEventsRequested() = runTest {
            // When: 모든 이벤트 요청
            val result = ufc.chart(
                symbol = "AAPL",
                interval = Interval.OneDay,
                period = Period.FiveYears,
                ChartEventType.DIVIDEND,
                ChartEventType.SPLIT,
                ChartEventType.CAPITAL_GAIN
            )

            // Then: hasEvent로 확인 후 getter 사용 가능
            if (result.hasEvent(ChartEventType.DIVIDEND)) {
                result.getDividends() // null 또는 Map
            }

            if (result.hasEvent(ChartEventType.SPLIT)) {
                result.getSplits() // null 또는 Map
            }

            if (result.hasEvent(ChartEventType.CAPITAL_GAIN)) {
                result.getCapitalGains() // null 또는 Map
            }

            // 검증: 모든 hasEvent는 true
            assertThat(result.hasEvent(ChartEventType.DIVIDEND)).isTrue()
            assertThat(result.hasEvent(ChartEventType.SPLIT)).isTrue()
            assertThat(result.hasEvent(ChartEventType.CAPITAL_GAIN)).isTrue()
        }
    }
}
