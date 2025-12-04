package com.ulalax.ufc.integration.price

import com.ulalax.ufc.api.client.Ufc
import com.ulalax.ufc.api.client.UfcClientConfig
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.domain.common.Interval
import com.ulalax.ufc.domain.common.Period
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitingSettings
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.system.measureTimeMillis

/**
 * PriceService - 가격 히스토리 조회 통합 테스트
 *
 * 이 테스트는 실제 Yahoo Finance API를 호출합니다.
 * Mock이나 Fake 객체를 사용하지 않고, 실제 네트워크 요청을 통해
 * ufc.price.getPriceHistory() 메서드의 모든 시나리오를 검증합니다.
 *
 * ## 테스트 환경
 * - 실제 Yahoo Finance API 호출
 * - 네트워크 연결 필요
 * - API Rate Limiting 적용
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew test --tests "*PriceHistoryIntegrationTest" --tests "*.integration.*"
 * ```
 *
 * ## 주의사항
 * - 네트워크 상태에 따라 테스트 실패 가능
 * - API 변경으로 인한 실패 가능
 * - Rate Limiting으로 인한 지연 발생 가능
 * - 실제 거래일 수는 주말/공휴일을 제외한 수치
 */
@Tag("integration")
@DisplayName("Price Service - 가격 히스토리 조회 통합 테스트")
class PriceHistoryIntegrationTest {

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
    @DisplayName("getPriceHistory(symbol, period, interval) - Period/Interval 조합 조회")
    inner class GetPriceHistoryByPeriod {

        @Nested
        @DisplayName("정상 케이스 - 일봉 조회")
        inner class DailyInterval {

            @Test
            @DisplayName("1개월 일봉 데이터를 조회한다")
            fun shouldReturnOneMonthDailyData() = runTest {
                // Given: AAPL 심볼, 1개월 기간, 1일 간격

                // When
                val history = ufc.price.getPriceHistory("AAPL", Period.OneMonth, Interval.OneDay)

                // Then: 데이터 검증
                assertThat(history)
                    .withFailMessage("1개월 일봉 데이터가 있어야 합니다")
                    .isNotEmpty()

                assertThat(history.size)
                    .withFailMessage(
                        "1개월 일봉 데이터는 최소 15개 이상이어야 합니다 (약 20 거래일). 실제: ${history.size}"
                    )
                    .isGreaterThanOrEqualTo(15)

                // 첫 번째 데이터 검증
                val firstOhlcv = history.first()
                assertThat(firstOhlcv.open)
                    .withFailMessage("시가는 0보다 커야 합니다. 실제: ${firstOhlcv.open}")
                    .isGreaterThan(0.0)

                assertThat(firstOhlcv.high)
                    .withFailMessage("고가는 시가보다 크거나 같아야 합니다. 시가: ${firstOhlcv.open}, 고가: ${firstOhlcv.high}")
                    .isGreaterThanOrEqualTo(firstOhlcv.open)

                assertThat(firstOhlcv.low)
                    .withFailMessage("저가는 시가보다 작거나 같아야 합니다. 시가: ${firstOhlcv.open}, 저가: ${firstOhlcv.low}")
                    .isLessThanOrEqualTo(firstOhlcv.open)

                assertThat(firstOhlcv.volume)
                    .withFailMessage("거래량은 0보다 커야 합니다. 실제: ${firstOhlcv.volume}")
                    .isGreaterThan(0)
            }

            @Test
            @DisplayName("1년 일봉 데이터를 조회한다")
            fun shouldReturnOneYearDailyData() = runTest {
                // Given: AAPL 심볼, 1년 기간, 1일 간격

                // When
                val history = ufc.price.getPriceHistory("AAPL", Period.OneYear, Interval.OneDay)

                // Then: 1년 약 250 거래일
                assertThat(history.size)
                    .withFailMessage(
                        "1년 일봉 데이터는 최소 200개 이상이어야 합니다 (약 250 거래일). 실제: ${history.size}"
                    )
                    .isGreaterThanOrEqualTo(200)
            }

            @Test
            @DisplayName("5년 일봉 데이터를 조회한다")
            fun shouldReturnFiveYearsDailyData() = runTest {
                // Given: AAPL 심볼, 5년 기간

                // When
                val history = ufc.price.getPriceHistory("AAPL", Period.FiveYears, Interval.OneDay)

                // Then: 5년 약 1250 거래일
                assertThat(history.size)
                    .withFailMessage(
                        "5년 일봉 데이터는 최소 1000개 이상이어야 합니다. 실제: ${history.size}"
                    )
                    .isGreaterThanOrEqualTo(1000)
            }
        }

        @Nested
        @DisplayName("정상 케이스 - 분봉/시간봉 조회")
        inner class IntraDayInterval {

            @Test
            @DisplayName("1일 1분봉 데이터를 조회한다")
            fun shouldReturnOneDayOneMinuteData() = runTest {
                // Given: 1분봉은 최대 7일까지, AAPL 심볼

                // When
                val history = ufc.price.getPriceHistory("AAPL", Period.OneDay, Interval.OneMinute)

                // Then: 1 거래일 약 390분 (09:30 ~ 16:00)
                assertThat(history)
                    .withFailMessage("1일 1분봉 데이터가 있어야 합니다")
                    .isNotEmpty()

                assertThat(history.size)
                    .withFailMessage(
                        "1일 1분봉 데이터는 최소 100개 이상이어야 합니다 (약 390분). 실제: ${history.size}"
                    )
                    .isGreaterThanOrEqualTo(100)
            }

            @Test
            @DisplayName("5일 5분봉 데이터를 조회한다")
            fun shouldReturnFiveDaysFiveMinuteData() = runTest {
                // Given: 5일 기간, 5분 간격

                // When
                val history = ufc.price.getPriceHistory("AAPL", Period.FiveDays, Interval.FiveMinutes)

                // Then: 5일 × 390분 ÷ 5 = 약 390개
                assertThat(history)
                    .withFailMessage("5일 5분봉 데이터가 있어야 합니다")
                    .isNotEmpty()

                assertThat(history.size)
                    .withFailMessage(
                        "5일 5분봉 데이터는 최소 200개 이상이어야 합니다. 실제: ${history.size}"
                    )
                    .isGreaterThanOrEqualTo(200)
            }

            @Test
            @DisplayName("1개월 1시간봉 데이터를 조회한다")
            fun shouldReturnOneMonthOneHourData() = runTest {
                // Given: 1개월 기간, 1시간 간격

                // When
                val history = ufc.price.getPriceHistory("AAPL", Period.OneMonth, Interval.OneHour)

                // Then: 1개월 약 20일 × 6.5시간 = 130시간
                assertThat(history)
                    .withFailMessage("1개월 1시간봉 데이터가 있어야 합니다")
                    .isNotEmpty()

                assertThat(history.size)
                    .withFailMessage(
                        "1개월 1시간봉 데이터는 최소 80개 이상이어야 합니다. 실제: ${history.size}"
                    )
                    .isGreaterThanOrEqualTo(80)
            }
        }

        @Nested
        @DisplayName("정상 케이스 - 주봉/월봉 조회")
        inner class WeeklyMonthlyInterval {

            @Test
            @DisplayName("1년 주봉 데이터를 조회한다")
            fun shouldReturnOneYearWeeklyData() = runTest {
                // Given: 1년 기간, 1주 간격

                // When
                val history = ufc.price.getPriceHistory("AAPL", Period.OneYear, Interval.OneWeek)

                // Then: 1년 약 52주
                assertThat(history)
                    .withFailMessage("1년 주봉 데이터가 있어야 합니다")
                    .isNotEmpty()

                assertThat(history.size)
                    .withFailMessage(
                        "1년 주봉 데이터는 최소 45개 이상이어야 합니다 (약 52주). 실제: ${history.size}"
                    )
                    .isGreaterThanOrEqualTo(45)
            }

            @Test
            @DisplayName("5년 월봉 데이터를 조회한다")
            fun shouldReturnFiveYearsMonthlyData() = runTest {
                // Given: 5년 기간, 1개월 간격

                // When
                val history = ufc.price.getPriceHistory("AAPL", Period.FiveYears, Interval.OneMonth)

                // Then: 5년 약 60개월
                assertThat(history)
                    .withFailMessage("5년 월봉 데이터가 있어야 합니다")
                    .isNotEmpty()

                assertThat(history.size)
                    .withFailMessage(
                        "5년 월봉 데이터는 최소 50개 이상이어야 합니다 (약 60개월). 실제: ${history.size}"
                    )
                    .isGreaterThanOrEqualTo(50)
            }
        }

        @Nested
        @DisplayName("데이터 정합성 검증")
        inner class DataIntegrity {

            @Test
            @DisplayName("OHLCV 데이터가 시간순으로 정렬되어 있다")
            fun shouldBeSortedByTimestamp() = runTest {
                // When
                val history = ufc.price.getPriceHistory("AAPL", Period.OneMonth, Interval.OneDay)

                // Then: 타임스탬프 오름차순 정렬
                val timestamps = history.map { it.timestamp }
                assertThat(timestamps)
                    .withFailMessage("타임스탬프가 오름차순으로 정렬되어야 합니다")
                    .isSorted()
            }

            @Test
            @DisplayName("모든 OHLCV 데이터의 High가 Low보다 크거나 같다")
            fun shouldHaveHighGreaterThanOrEqualToLow() = runTest {
                // When
                val history = ufc.price.getPriceHistory("AAPL", Period.OneMonth, Interval.OneDay)

                // Then: High ≥ Low 검증
                history.forEachIndexed { index, ohlcv ->
                    assertThat(ohlcv.high)
                        .withFailMessage(
                            "인덱스 $index: High(${ohlcv.high})는 Low(${ohlcv.low})보다 크거나 같아야 합니다. " +
                            "타임스탬프: ${ohlcv.timestamp}"
                        )
                        .isGreaterThanOrEqualTo(ohlcv.low)
                }
            }

            @Test
            @DisplayName("Open/Close 가격이 High/Low 범위 내에 있다")
            fun shouldHaveOpenAndCloseWithinRange() = runTest {
                // When
                val history = ufc.price.getPriceHistory("AAPL", Period.OneMonth, Interval.OneDay)

                // Then: Low ≤ Open/Close ≤ High
                history.forEachIndexed { index, ohlcv ->
                    assertThat(ohlcv.open)
                        .withFailMessage(
                            "인덱스 $index: Open(${ohlcv.open})은 Low(${ohlcv.low}) ~ High(${ohlcv.high}) 범위 내에 있어야 합니다"
                        )
                        .isBetween(ohlcv.low, ohlcv.high)

                    assertThat(ohlcv.close)
                        .withFailMessage(
                            "인덱스 $index: Close(${ohlcv.close})는 Low(${ohlcv.low}) ~ High(${ohlcv.high}) 범위 내에 있어야 합니다"
                        )
                        .isBetween(ohlcv.low, ohlcv.high)
                }
            }

            @Test
            @DisplayName("Volume은 항상 0 이상이다")
            fun shouldHaveNonNegativeVolume() = runTest {
                // When
                val history = ufc.price.getPriceHistory("AAPL", Period.OneMonth, Interval.OneDay)

                // Then: Volume ≥ 0
                history.forEachIndexed { index, ohlcv ->
                    assertThat(ohlcv.volume)
                        .withFailMessage(
                            "인덱스 $index: Volume(${ohlcv.volume})은 0 이상이어야 합니다. 타임스탬프: ${ohlcv.timestamp}"
                        )
                        .isGreaterThanOrEqualTo(0)
                }
            }

            @Test
            @DisplayName("모든 가격이 양수이다")
            fun shouldHavePositivePrices() = runTest {
                // When
                val history = ufc.price.getPriceHistory("AAPL", Period.OneMonth, Interval.OneDay)

                // Then: Open, High, Low, Close > 0
                history.forEachIndexed { index, ohlcv ->
                    assertThat(ohlcv.open)
                        .withFailMessage("인덱스 $index: Open은 0보다 커야 합니다")
                        .isGreaterThan(0.0)

                    assertThat(ohlcv.high)
                        .withFailMessage("인덱스 $index: High는 0보다 커야 합니다")
                        .isGreaterThan(0.0)

                    assertThat(ohlcv.low)
                        .withFailMessage("인덱스 $index: Low는 0보다 커야 합니다")
                        .isGreaterThan(0.0)

                    assertThat(ohlcv.close)
                        .withFailMessage("인덱스 $index: Close는 0보다 커야 합니다")
                        .isGreaterThan(0.0)
                }
            }

            @Test
            @DisplayName("OHLCV 도메인 메서드가 정상 동작한다")
            fun shouldCalculateOhlcvMetricsCorrectly() = runTest {
                // When
                val history = ufc.price.getPriceHistory("AAPL", Period.OneMonth, Interval.OneDay)
                val firstOhlcv = history.first()

                // Then: 도메인 메서드 검증
                val range = firstOhlcv.range()
                assertThat(range)
                    .withFailMessage("range()는 High - Low와 같아야 합니다")
                    .isEqualTo(firstOhlcv.high - firstOhlcv.low)

                val change = firstOhlcv.change()
                assertThat(change)
                    .withFailMessage("change()는 Close - Open과 같아야 합니다")
                    .isEqualTo(firstOhlcv.close - firstOhlcv.open)

                // 양봉/음봉 검증
                if (firstOhlcv.close > firstOhlcv.open) {
                    assertThat(firstOhlcv.isBullish())
                        .withFailMessage("Close > Open이면 양봉이어야 합니다")
                        .isTrue()
                } else if (firstOhlcv.close < firstOhlcv.open) {
                    assertThat(firstOhlcv.isBearish())
                        .withFailMessage("Close < Open이면 음봉이어야 합니다")
                        .isTrue()
                }
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
                        ufc.price.getPriceHistory("", Period.OneMonth, Interval.OneDay)
                    }
                }
                    .isInstanceOf(UfcException::class.java)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SYMBOL)
            }

            @Test
            @DisplayName("공백만 있는 심볼 조회 시 에러를 발생시킨다")
            fun shouldThrowErrorForWhitespaceSymbol() = runTest {
                // When & Then
                assertThatThrownBy {
                    runBlocking {
                        ufc.price.getPriceHistory("   ", Period.OneMonth, Interval.OneDay)
                    }
                }
                    .isInstanceOf(UfcException::class.java)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SYMBOL)
            }
        }

        @Nested
        @DisplayName("다양한 심볼 타입")
        inner class VariousSymbolTypes {

            @Test
            @DisplayName("ETF 심볼의 히스토리를 조회한다")
            fun shouldReturnHistoryForETFSymbol() = runTest {
                // Given: SPY ETF

                // When
                val history = ufc.price.getPriceHistory("SPY", Period.OneMonth, Interval.OneDay)

                // Then
                assertThat(history)
                    .withFailMessage("SPY ETF 히스토리가 있어야 합니다")
                    .isNotEmpty()

                assertThat(history.size)
                    .withFailMessage("1개월 일봉 데이터는 최소 15개 이상이어야 합니다")
                    .isGreaterThanOrEqualTo(15)
            }

            @Test
            @DisplayName("인덱스 심볼의 히스토리를 조회한다")
            fun shouldReturnHistoryForIndexSymbol() = runTest {
                // Given: S&P 500 Index (^GSPC)

                // When
                val history = ufc.price.getPriceHistory("^GSPC", Period.OneMonth, Interval.OneDay)

                // Then
                assertThat(history)
                    .withFailMessage("^GSPC 인덱스 히스토리가 있어야 합니다")
                    .isNotEmpty()
            }

            @Test
            @DisplayName("특수문자 포함 심볼의 히스토리를 조회한다")
            fun shouldReturnHistoryForSymbolWithSpecialCharacters() = runTest {
                // Given: BRK-A (하이픈 포함)

                // When
                val history = ufc.price.getPriceHistory("BRK-A", Period.OneMonth, Interval.OneDay)

                // Then
                assertThat(history)
                    .withFailMessage("BRK-A 히스토리가 있어야 합니다")
                    .isNotEmpty()
            }
        }
    }

    @Nested
    @DisplayName("getPriceHistory(symbol, start, end, interval) - 날짜 범위 조회")
    inner class GetPriceHistoryByDateRange {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("특정 날짜 범위의 일봉 데이터를 조회한다")
            fun shouldReturnDataForSpecificDateRange() = runTest {
                // Given: 2024년 1월 1일 ~ 2024년 1월 31일
                val startDate = LocalDate.of(2024, 1, 1)
                val endDate = LocalDate.of(2024, 1, 31)

                // When
                val history = ufc.price.getPriceHistory("AAPL", startDate, endDate, Interval.OneDay)

                // Then: 1월 거래일 데이터
                assertThat(history)
                    .withFailMessage("2024년 1월 일봉 데이터가 있어야 합니다")
                    .isNotEmpty()

                assertThat(history.size)
                    .withFailMessage(
                        "1월 일봉 데이터는 최소 15개 이상이어야 합니다 (약 20 거래일). 실제: ${history.size}"
                    )
                    .isGreaterThanOrEqualTo(15)

                // 날짜 범위 검증
                val firstDate = Instant.ofEpochSecond(history.first().timestamp)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                val lastDate = Instant.ofEpochSecond(history.last().timestamp)
                    .atZone(ZoneId.systemDefault()).toLocalDate()

                assertThat(firstDate)
                    .withFailMessage(
                        "첫 데이터 날짜(${firstDate})는 시작일(${startDate}) 이후여야 합니다"
                    )
                    .isAfterOrEqualTo(startDate)

                assertThat(lastDate)
                    .withFailMessage(
                        "마지막 데이터 날짜(${lastDate})는 종료일(${endDate}) 이전이어야 합니다"
                    )
                    .isBeforeOrEqualTo(endDate)
            }

            @Test
            @DisplayName("1주일 범위의 시간봉 데이터를 조회한다")
            fun shouldReturnHourlyDataForOneWeek() = runTest {
                // Given: 최근 1주일
                val endDate = LocalDate.now()
                val startDate = endDate.minusDays(7)

                // When
                val history = ufc.price.getPriceHistory("AAPL", startDate, endDate, Interval.OneHour)

                // Then
                assertThat(history)
                    .withFailMessage("최근 1주일 시간봉 데이터가 있어야 합니다")
                    .isNotEmpty()
            }

            @Test
            @DisplayName("3개월 범위의 주봉 데이터를 조회한다")
            fun shouldReturnWeeklyDataForThreeMonths() = runTest {
                // Given: 2024년 1월 ~ 3월
                val startDate = LocalDate.of(2024, 1, 1)
                val endDate = LocalDate.of(2024, 3, 31)

                // When
                val history = ufc.price.getPriceHistory("AAPL", startDate, endDate, Interval.OneWeek)

                // Then: 3개월 약 12주
                assertThat(history)
                    .withFailMessage("3개월 주봉 데이터가 있어야 합니다")
                    .isNotEmpty()

                assertThat(history.size)
                    .withFailMessage(
                        "3개월 주봉 데이터는 최소 10개 이상이어야 합니다. 실제: ${history.size}"
                    )
                    .isGreaterThanOrEqualTo(10)
            }

            @Test
            @DisplayName("1년 범위의 월봉 데이터를 조회한다")
            fun shouldReturnMonthlyDataForOneYear() = runTest {
                // Given: 2023년 전체
                val startDate = LocalDate.of(2023, 1, 1)
                val endDate = LocalDate.of(2023, 12, 31)

                // When
                val history = ufc.price.getPriceHistory("AAPL", startDate, endDate, Interval.OneMonth)

                // Then: 12개월
                assertThat(history)
                    .withFailMessage("2023년 월봉 데이터가 있어야 합니다")
                    .isNotEmpty()

                assertThat(history.size)
                    .withFailMessage(
                        "1년 월봉 데이터는 최소 10개 이상이어야 합니다. 실제: ${history.size}"
                    )
                    .isGreaterThanOrEqualTo(10)
            }

            @Test
            @DisplayName("단일 날짜(1일)의 1분봉 데이터를 조회한다")
            fun shouldReturnOneMinuteDataForSingleDay() = runTest {
                // Given: 최근 거래일 (1분봉은 최근 데이터만 지원)
                val startDate = LocalDate.now().minusDays(7)
                val endDate = startDate.plusDays(1)

                // When: 1일 범위로 1분봉 조회
                // Note: Yahoo Finance API는 1분봉을 매우 제한적으로 제공 (최근 7일 정도만)
                try {
                    val history = ufc.price.getPriceHistory("AAPL", startDate, endDate, Interval.OneMinute)

                    // Then: 데이터가 있으면 충분한 수량 확인
                    if (history.isNotEmpty()) {
                        assertThat(history.size)
                            .withFailMessage("1일 1분봉은 최소 50개 이상이어야 합니다")
                            .isGreaterThanOrEqualTo(50)
                    }
                } catch (e: UfcException) {
                    // API가 1분봉을 지원하지 않을 수 있음 (HTTP 422 등)
                    // 이 경우 테스트 통과 (API 제한사항)
                    assertThat(e.errorCode)
                        .withFailMessage("1분봉 미지원 시 EXTERNAL_API_ERROR여야 합니다")
                        .isEqualTo(ErrorCode.EXTERNAL_API_ERROR)
                }
            }
        }

        @Nested
        @DisplayName("입력 검증")
        inner class InputValidation {

            @Test
            @DisplayName("시작 날짜가 종료 날짜보다 늦으면 에러를 발생시킨다")
            fun shouldThrowErrorWhenStartIsAfterEnd() = runTest {
                // Given: 시작일 > 종료일
                val startDate = LocalDate.of(2024, 12, 31)
                val endDate = LocalDate.of(2024, 1, 1)

                // When & Then
                assertThatThrownBy {
                    runBlocking {
                        ufc.price.getPriceHistory("AAPL", startDate, endDate, Interval.OneDay)
                    }
                }
                    .isInstanceOf(UfcException::class.java)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_DATE_RANGE)
            }

            @Test
            @DisplayName("빈 문자열 심볼로 날짜 범위 조회 시 에러를 발생시킨다")
            fun shouldThrowErrorForBlankSymbolWithDateRange() = runTest {
                // Given
                val startDate = LocalDate.of(2024, 1, 1)
                val endDate = LocalDate.of(2024, 1, 31)

                // When & Then
                assertThatThrownBy {
                    runBlocking {
                        ufc.price.getPriceHistory("", startDate, endDate, Interval.OneDay)
                    }
                }
                    .isInstanceOf(UfcException::class.java)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SYMBOL)
            }
        }

        @Nested
        @DisplayName("날짜 경계 케이스")
        inner class DateBoundaryCases {

            @Test
            @DisplayName("과거 10년 전 데이터를 조회한다")
            fun shouldReturnDataFromTenYearsAgo() = runTest {
                // Given: 10년 전 1월
                val now = LocalDate.now()
                val startDate = now.minusYears(10).withMonth(1).withDayOfMonth(1)
                val endDate = startDate.plusMonths(1).minusDays(1)

                // When
                val history = ufc.price.getPriceHistory("AAPL", startDate, endDate, Interval.OneDay)

                // Then: 오래된 데이터도 조회 가능
                assertThat(history)
                    .withFailMessage("10년 전 데이터도 조회 가능해야 합니다")
                    .isNotEmpty()
            }

            @Test
            @DisplayName("미래 날짜로 조회 시 빈 데이터 또는 에러를 반환한다")
            fun shouldHandleFutureDateGracefully() = runTest {
                // Given: 미래 날짜 (1년 후)
                val now = LocalDate.now()
                val startDate = now.plusYears(1)
                val endDate = startDate.plusDays(30)

                // When & Then: 빈 데이터 또는 에러 허용
                try {
                    val history = ufc.price.getPriceHistory("AAPL", startDate, endDate, Interval.OneDay)
                    assertThat(history)
                        .withFailMessage("미래 날짜는 빈 데이터를 반환해야 합니다")
                        .isEmpty()
                } catch (e: Exception) {
                    // 에러 발생도 허용 (API에 따라 다를 수 있음)
                    assertThat(e)
                        .isInstanceOfAny(UfcException::class.java, IllegalArgumentException::class.java)
                }
            }
        }
    }

    @Nested
    @DisplayName("캐싱 동작")
    inner class CachingBehavior {

        @Test
        @DisplayName("같은 조건으로 연속 조회 시 두 번째는 빠르게 반환된다")
        fun shouldReturnFasterOnSecondCallDueToCache() = runTest {
            // Given: AAPL 심볼

            // When: 동일 조건 2회 조회
            val firstCallTime = measureTimeMillis {
                ufc.price.getPriceHistory("AAPL", Period.OneMonth, Interval.OneDay)
            }

            val secondCallTime = measureTimeMillis {
                ufc.price.getPriceHistory("AAPL", Period.OneMonth, Interval.OneDay)
            }

            // Then: 두 번째 호출이 더 빠름 (캐싱)
            assertThat(secondCallTime)
                .withFailMessage(
                    "두 번째 호출(캐시)이 첫 번째보다 빨라야 합니다. " +
                    "첫 번째: ${firstCallTime}ms, 두 번째: ${secondCallTime}ms"
                )
                .isLessThan(firstCallTime)
        }

        @Test
        @DisplayName("캐싱된 데이터도 정확한 OHLCV 데이터를 포함한다")
        fun shouldReturnAccurateCachedData() = runTest {
            // Given: 첫 번째 조회로 캐시 생성
            val firstCall = ufc.price.getPriceHistory("AAPL", Period.OneMonth, Interval.OneDay)

            // When: 두 번째 조회 (캐시에서)
            val secondCall = ufc.price.getPriceHistory("AAPL", Period.OneMonth, Interval.OneDay)

            // Then: 두 응답이 동일
            assertThat(secondCall.size).isEqualTo(firstCall.size)
            assertThat(secondCall.first().timestamp).isEqualTo(firstCall.first().timestamp)
            assertThat(secondCall.first().close).isEqualTo(firstCall.first().close)
        }

        @Test
        @DisplayName("다른 심볼 조회 시 각각 독립적으로 조회된다")
        fun shouldQueryIndependentlyForDifferentSymbols() = runTest {
            // Given: 2개의 다른 심볼

            // When
            val aaplHistory = ufc.price.getPriceHistory("AAPL", Period.OneMonth, Interval.OneDay)
            val msftHistory = ufc.price.getPriceHistory("MSFT", Period.OneMonth, Interval.OneDay)

            // Then: 각 심볼이 독립적인 데이터 반환
            assertThat(aaplHistory).isNotEmpty()
            assertThat(msftHistory).isNotEmpty()

            // 가격이 다름 (정상적인 시장 상황)
            assertThat(aaplHistory.first().close)
                .withFailMessage("AAPL과 MSFT의 종가는 달라야 합니다")
                .isNotEqualTo(msftHistory.first().close)
        }

        @Test
        @DisplayName("다른 Period/Interval 조합은 각각 독립적으로 캐싱된다")
        fun shouldCacheIndependentlyForDifferentParameters() = runTest {
            // Given: AAPL 심볼

            // When: 다른 조합으로 조회
            val oneMonth = ufc.price.getPriceHistory("AAPL", Period.OneMonth, Interval.OneDay)
            val oneYear = ufc.price.getPriceHistory("AAPL", Period.OneYear, Interval.OneDay)
            val oneHour = ufc.price.getPriceHistory("AAPL", Period.OneMonth, Interval.OneHour)

            // Then: 각각 다른 데이터 크기
            assertThat(oneMonth.size)
                .withFailMessage("1개월 일봉과 1년 일봉의 데이터 크기는 달라야 합니다")
                .isLessThan(oneYear.size)

            assertThat(oneHour.size)
                .withFailMessage("1개월 시간봉과 1개월 일봉의 데이터 크기는 달라야 합니다")
                .isGreaterThan(oneMonth.size)
        }
    }

    @Nested
    @DisplayName("성능 및 안정성")
    inner class PerformanceAndReliability {

        @Test
        @DisplayName("대용량 데이터 조회가 정상 동작한다")
        fun shouldHandleLargeDataset() = runTest {
            // Given: 10년 일봉 (약 2500개 데이터)

            // When
            val history = ufc.price.getPriceHistory("AAPL", Period.TenYears, Interval.OneDay)

            // Then
            assertThat(history.size)
                .withFailMessage("10년 일봉 데이터는 최소 2000개 이상이어야 합니다")
                .isGreaterThanOrEqualTo(2000)

            // 모든 데이터가 정합성을 만족
            history.forEach { ohlcv ->
                assertThat(ohlcv.high).isGreaterThanOrEqualTo(ohlcv.low)
                assertThat(ohlcv.open).isBetween(ohlcv.low, ohlcv.high)
                assertThat(ohlcv.close).isBetween(ohlcv.low, ohlcv.high)
            }
        }

        @Test
        @DisplayName("연속된 여러 심볼 조회가 안정적으로 동작한다")
        fun shouldHandleMultipleSequentialQueries() = runTest {
            // Given: 5개 주요 심볼
            val symbols = listOf("AAPL", "MSFT", "GOOGL", "AMZN", "TSLA")

            // When: 연속 조회
            val results = symbols.map { symbol ->
                symbol to ufc.price.getPriceHistory(symbol, Period.OneMonth, Interval.OneDay)
            }

            // Then: 모두 정상 조회
            results.forEach { (symbol, history) ->
                assertThat(history)
                    .withFailMessage("$symbol 히스토리가 있어야 합니다")
                    .isNotEmpty()
            }
        }
    }
}
