package com.ulalax.ufc.integration.yahoo

import com.ulalax.ufc.domain.exception.ApiException
import com.ulalax.ufc.fixture.TestFixtures
import com.ulalax.ufc.integration.utils.IntegrationTestBase
import com.ulalax.ufc.integration.utils.RecordingConfig
import com.ulalax.ufc.domain.model.chart.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Yahoo.chart() API Integration 테스트
 *
 * 이 테스트는 실제 Yahoo Finance API를 호출하여 Chart 기능을 검증합니다.
 * API 가이드처럼 읽힐 수 있도록 @Nested 그룹핑 패턴을 사용합니다.
 *
 * ## 거래일/비거래일 동작
 * - **거래일 (장중)**: 당일 봉의 OHLCV가 실시간으로 업데이트, 마지막 봉은 미완성
 * - **거래일 (장외)**: 당일 봉 완성, 과거 데이터는 동일
 * - **휴장일**: 당일 데이터 없음, 마지막 거래일까지의 데이터만 반환
 *
 * ## 테스트 실행 방법
 * ```bash
 * # 특정 클래스 실행
 * ./gradlew test --tests 'ChartSpec'
 *
 * # 특정 그룹 실행
 * ./gradlew test --tests 'ChartSpec$BasicBehavior'
 * ```
 */
@DisplayName("[I] Yahoo.chart() - 차트 데이터 조회")
class ChartSpec : IntegrationTestBase() {

    @Nested
    @DisplayName("기본 동작")
    inner class BasicBehavior {

        @Test
        @DisplayName("AAPL의 1년 일봉 차트를 조회할 수 있다")
        fun `returns chart data for AAPL with 1 year period`() = integrationTest(
            RecordingConfig.Paths.Yahoo.CHART,
            "aapl_1y_daily"
        ) {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val interval = Interval.OneDay
            val period = Period.OneYear

            // When
            val result = ufc.yahoo.chart(symbol, interval, period)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.meta.symbol).isEqualTo(symbol)
            assertThat(result.prices).isNotEmpty()
        }

        @Test
        @DisplayName("MSFT의 1개월 시간봉 차트를 조회할 수 있다")
        fun `returns hourly chart data for MSFT with 1 month period`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.MSFT
            val interval = Interval.OneHour
            val period = Period.OneMonth

            // When
            val result = ufc.yahoo.chart(symbol, interval, period)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.meta.symbol).isEqualTo(symbol)
            assertThat(result.prices).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("응답 데이터 스펙")
    inner class ResponseSpec {

        @Test
        @DisplayName("차트 데이터는 OHLCV 정보를 포함한다")
        fun `chart data contains OHLCV information`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val interval = Interval.OneDay
            val period = Period.OneMonth

            // When
            val result = ufc.yahoo.chart(symbol, interval, period)

            // Then
            assertThat(result.prices).isNotEmpty()

            val firstPrice = result.prices.first()
            assertThat(firstPrice.open).isGreaterThan(0.0)
            assertThat(firstPrice.high).isGreaterThan(0.0)
            assertThat(firstPrice.low).isGreaterThan(0.0)
            assertThat(firstPrice.close).isGreaterThan(0.0)
            assertThat(firstPrice.volume).isGreaterThan(0)
        }

        @Test
        @DisplayName("차트 메타데이터는 심볼과 통화 정보를 포함한다")
        fun `chart metadata contains symbol and currency`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val interval = Interval.OneDay
            val period = Period.OneYear

            // When
            val result = ufc.yahoo.chart(symbol, interval, period)

            // Then
            assertThat(result.meta.symbol).isEqualTo(symbol)
            assertThat(result.meta.currency).isNotNull()
        }
    }

    @Nested
    @DisplayName("이벤트 조회")
    inner class EventsSpec {

        @Test
        @DisplayName("배당금 이벤트를 조회할 수 있다")
        fun `can fetch dividend events`() = integrationTest(
            RecordingConfig.Paths.Yahoo.CHART,
            "aapl_dividends"
        ) {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val interval = Interval.OneDay
            val period = Period.OneYear

            // When
            val result = ufc.yahoo.chart(
                symbol = symbol,
                interval = interval,
                period = period,
                ChartEventType.DIVIDEND
            )

            // Then
            assertThat(result).isNotNull()
            assertThat(result.hasEvent(ChartEventType.DIVIDEND)).isTrue()
        }

        @Test
        @DisplayName("배당금과 주식 분할을 동시에 조회할 수 있다")
        fun `can fetch both dividends and splits at once`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val interval = Interval.OneDay
            val period = Period.FiveYears

            // When
            val result = ufc.yahoo.chart(
                symbol = symbol,
                interval = interval,
                period = period,
                ChartEventType.DIVIDEND,
                ChartEventType.SPLIT
            )

            // Then
            assertThat(result).isNotNull()
            assertThat(result.hasEvent(ChartEventType.DIVIDEND)).isTrue()
            assertThat(result.hasEvent(ChartEventType.SPLIT)).isTrue()
        }
    }

    @Nested
    @DisplayName("사용 가능한 옵션")
    inner class AvailableOptions {

        @Test
        @DisplayName("Interval enum은 다양한 봉 간격을 지원한다")
        fun `Interval enum supports various candle intervals`() {
            // Given - 분봉
            val minuteIntervals = listOf(
                Interval.OneMinute,
                Interval.TwoMinutes,
                Interval.FiveMinutes,
                Interval.FifteenMinutes,
                Interval.ThirtyMinutes,
                Interval.OneHour
            )

            // Given - 일봉 이상
            val dailyIntervals = listOf(
                Interval.OneDay,
                Interval.FiveDays,
                Interval.OneWeek,
                Interval.OneMonth,
                Interval.ThreeMonths
            )

            // Then
            assertThat(Interval.entries).hasSize(11)
            assertThat(Interval.entries).containsAll(minuteIntervals)
            assertThat(Interval.entries).containsAll(dailyIntervals)
        }

        @Test
        @DisplayName("Period enum은 다양한 조회 기간을 지원한다")
        fun `Period enum supports various time periods`() {
            // Given - 단기
            val shortTermPeriods = listOf(
                Period.OneDay,
                Period.FiveDays,
                Period.OneMonth,
                Period.ThreeMonths,
                Period.SixMonths
            )

            // Given - 장기
            val longTermPeriods = listOf(
                Period.OneYear,
                Period.TwoYears,
                Period.FiveYears,
                Period.TenYears,
                Period.YearToDate,
                Period.Max
            )

            // Then
            assertThat(Period.entries).hasSize(11)
            assertThat(Period.entries).containsAll(shortTermPeriods)
            assertThat(Period.entries).containsAll(longTermPeriods)
        }

        @Test
        @DisplayName("ChartEventType enum은 배당금과 주식분할 이벤트를 지원한다")
        fun `ChartEventType enum supports dividend and split events`() {
            // Given
            val supportedEvents = listOf(
                ChartEventType.DIVIDEND,
                ChartEventType.SPLIT,
                ChartEventType.CAPITAL_GAIN
            )

            // Then
            assertThat(ChartEventType.entries).hasSize(3)
            assertThat(ChartEventType.entries).containsAll(supportedEvents)

            // API 값 확인
            assertThat(ChartEventType.DIVIDEND.apiValue).isEqualTo("div")
            assertThat(ChartEventType.SPLIT.apiValue).isEqualTo("split")
            assertThat(ChartEventType.CAPITAL_GAIN.apiValue).isEqualTo("capitalGain")
        }
    }

    @Nested
    @DisplayName("데이터 접근 방법")
    inner class DataAccessExamples {

        @Test
        @DisplayName("차트에서 OHLCV 데이터를 추출할 수 있다")
        fun `can extract OHLCV data from chart`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val interval = Interval.OneDay
            val period = Period.OneMonth

            // When
            val result = ufc.yahoo.chart(symbol, interval, period)

            // Then
            assertThat(result.prices).isNotEmpty()

            val firstCandle = result.prices.first()

            // OHLCV 데이터 접근
            val open = firstCandle.open
            val high = firstCandle.high
            val low = firstCandle.low
            val close = firstCandle.close
            val volume = firstCandle.volume
            val timestamp = firstCandle.timestamp

            // 검증
            assertThat(open).isGreaterThan(0.0)
            assertThat(high).isGreaterThanOrEqualTo(open)
            assertThat(low).isLessThanOrEqualTo(open)
            assertThat(close).isGreaterThan(0.0)
            assertThat(volume).isGreaterThan(0)
            assertThat(timestamp).isGreaterThan(0)
        }

        @Test
        @DisplayName("차트 메타데이터에서 심볼, 통화, 거래소 정보를 얻을 수 있다")
        fun `can get symbol currency and exchange from metadata`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val interval = Interval.OneDay
            val period = Period.OneMonth

            // When
            val result = ufc.yahoo.chart(symbol, interval, period)

            // Then
            val meta = result.meta
            assertThat(meta.symbol).isEqualTo(symbol)
            assertThat(meta.currency).isNotNull()
            assertThat(meta.regularMarketPrice).isNotNull().isGreaterThan(0.0)
        }

        @Test
        @DisplayName("배당금 이벤트 데이터를 추출할 수 있다")
        fun `can extract dividend events`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val interval = Interval.OneDay
            val period = Period.OneYear

            // When
            val result = ufc.yahoo.chart(
                symbol = symbol,
                interval = interval,
                period = period,
                ChartEventType.DIVIDEND
            )

            // Then
            assertThat(result.hasEvent(ChartEventType.DIVIDEND)).isTrue()

            val dividends = result.getDividends()
            assertThat(dividends).isNotNull().isNotEmpty()

            val firstEntry = dividends!!.entries.first()
            assertThat(firstEntry.key).isNotBlank()
            assertThat(firstEntry.value.amount).isNotNull().isGreaterThan(0.0)
            assertThat(firstEntry.value.date).isNotNull().isGreaterThan(0)
        }

        @Test
        @DisplayName("주식분할 이벤트 데이터를 추출할 수 있다")
        fun `can extract split events`() = integrationTest {
            // Given - AAPL은 2020년 8/31에 4:1 분할이 있었음, 최대 기간으로 조회
            val symbol = TestFixtures.Symbols.AAPL
            val interval = Interval.OneDay
            val period = Period.Max

            // When
            val result = ufc.yahoo.chart(
                symbol = symbol,
                interval = interval,
                period = period,
                ChartEventType.SPLIT
            )

            // Then
            assertThat(result.hasEvent(ChartEventType.SPLIT)).isTrue()

            val splits = result.getSplits()
            assertThat(splits).isNotNull().isNotEmpty()

            val firstEntry = splits!!.entries.first()
            assertThat(firstEntry.key).isNotBlank()
            assertThat(firstEntry.value.date).isNotNull().isGreaterThan(0)
            assertThat(firstEntry.value.numerator).isNotNull()
            assertThat(firstEntry.value.denominator).isNotNull()
        }

        @Test
        @DisplayName("OHLCV 데이터의 헬퍼 메서드를 활용할 수 있다")
        fun `can use OHLCV helper methods`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val interval = Interval.OneDay
            val period = Period.OneMonth

            // When
            val result = ufc.yahoo.chart(symbol, interval, period)

            // Then
            val firstCandle = result.prices.first()

            // 가격 변동폭
            val range = firstCandle.range()
            assertThat(range).isGreaterThanOrEqualTo(0.0)

            // 가격 변동률
            val rangePercent = firstCandle.rangePercent()
            assertThat(rangePercent).isGreaterThanOrEqualTo(0.0)

            // 종가 기준 변동액
            val change = firstCandle.change()

            // 종가 기준 변동률
            val changePercent = firstCandle.changePercent()

            // 양봉/음봉 여부
            val isBullish = firstCandle.isBullish()
            val isBearish = firstCandle.isBearish()

            // 양봉과 음봉은 동시에 true일 수 없음
            assertThat(isBullish && isBearish).isFalse()
        }
    }

    @Nested
    @DisplayName("활용 예제")
    inner class UsageExamples {

        @Test
        @DisplayName("암호화폐(BTC-USD) 차트 데이터를 조회할 수 있다")
        fun `can fetch cryptocurrency chart data`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.BTC_USD
            val interval = Interval.OneDay
            val period = Period.ThreeMonths

            // When
            val result = ufc.yahoo.chart(symbol, interval, period)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.meta.symbol).isEqualTo(symbol)
            assertThat(result.prices).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("에러 케이스")
    inner class ErrorCases {

        @Test
        @DisplayName("존재하지 않는 심볼 조회 시 ApiException을 던진다")
        fun `throws ApiException for invalid symbol`() = integrationTest {
            // Given
            val invalidSymbol = TestFixtures.Symbols.INVALID
            val interval = Interval.OneDay
            val period = Period.OneMonth

            // When & Then
            val result = runCatching {
                ufc.yahoo.chart(invalidSymbol, interval, period)
            }
            // Then
            assertThat(result.isFailure).isTrue()
        }
    }
}
