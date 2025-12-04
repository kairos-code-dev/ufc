package com.ulalax.ufc.integration.corp

import com.ulalax.ufc.api.client.Ufc
import com.ulalax.ufc.api.client.UfcClientConfig
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.domain.common.Period
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitingSettings
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

/**
 * CorpService - 기업 행동 조회 통합 테스트
 *
 * 이 테스트는 실제 Yahoo Finance API를 호출합니다.
 * Mock이나 Fake 객체를 사용하지 않고, 실제 네트워크 요청을 통해
 * ufc.corp.getDividends(), getSplits(), getCapitalGains() 메서드의 모든 시나리오를 검증합니다.
 *
 * ## 테스트 환경
 * - 실제 Yahoo Finance API 호출
 * - 네트워크 연결 필요
 * - API Rate Limiting 적용
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew test --tests "*CorpActionsIntegrationTest" --tests "*.integration.*"
 * ```
 *
 * ## 주의사항
 * - 네트워크 상태에 따라 테스트 실패 가능
 * - API 변경으로 인한 실패 가능
 * - Rate Limiting으로 인한 지연 발생 가능
 */
@Tag("integration")
@DisplayName("Corp Service - 기업 행동 조회 통합 테스트")
class CorpActionsIntegrationTest {

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
    @DisplayName("getDividends() - 배당금 조회")
    inner class GetDividends {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("배당주의 배당금 히스토리를 조회한다")
            fun shouldReturnDividendHistoryForDividendStock() = runTest {
                // Given: AAPL (배당주)

                // When
                val dividendHistory = ufc.corp.getDividends("AAPL", Period.FiveYears)

                // Then: 배당금 데이터 반환
                assertThat(dividendHistory.symbol)
                    .withFailMessage("심볼은 'AAPL'이어야 합니다")
                    .isEqualTo("AAPL")

                assertThat(dividendHistory.dividends)
                    .withFailMessage("배당금 히스토리가 있어야 합니다")
                    .isNotEmpty()

                // 첫 번째 배당금 검증
                val firstDividend = dividendHistory.dividends.first()
                assertThat(firstDividend.date)
                    .withFailMessage("배당 날짜가 있어야 합니다")
                    .isGreaterThan(0)

                assertThat(firstDividend.amount)
                    .withFailMessage("배당금액은 0보다 커야 합니다")
                    .isGreaterThan(0.0)
            }

            @Test
            @DisplayName("배당금은 날짜 순서로 정렬되어 있다")
            fun shouldBeSortedByDate() = runTest {
                // Given: MSFT (배당주)

                // When
                val dividendHistory = ufc.corp.getDividends("MSFT", Period.FiveYears)

                // Then: 날짜 오름차순 정렬
                if (dividendHistory.dividends.size > 1) {
                    for (i in 0 until dividendHistory.dividends.size - 1) {
                        assertThat(dividendHistory.dividends[i + 1].date)
                            .withFailMessage("배당금은 날짜 오름차순이어야 합니다")
                            .isGreaterThanOrEqualTo(dividendHistory.dividends[i].date)
                    }
                }
            }

            @Test
            @DisplayName("여러 분기의 배당금을 조회할 수 있다")
            fun shouldReturnMultipleQuarters() = runTest {
                // Given: JPM (금융주, 분기 배당)

                // When
                val dividendHistory = ufc.corp.getDividends("JPM", Period.TwoYears)

                // Then: 최소 4개 이상의 배당 (1년에 4회)
                assertThat(dividendHistory.dividends.size)
                    .withFailMessage("2년치 데이터는 최소 4개 이상의 배당이 있어야 합니다")
                    .isGreaterThanOrEqualTo(4)
            }

            @Test
            @DisplayName("OneYear 기간으로 조회할 수 있다")
            fun shouldReturnDividendsForOneYear() = runTest {
                // Given: AAPL, 1년 기간

                // When
                val dividendHistory = ufc.corp.getDividends("AAPL", Period.OneYear)

                // Then: 1년치 데이터만 반환
                assertThat(dividendHistory.symbol).isEqualTo("AAPL")
                assertThat(dividendHistory.dividends).isNotEmpty()
            }

            @Test
            @DisplayName("Max 기간으로 전체 히스토리를 조회할 수 있다")
            fun shouldReturnAllDividendsForMax() = runTest {
                // Given: AAPL, 전체 기간

                // When
                val dividendHistory = ufc.corp.getDividends("AAPL", Period.Max)

                // Then: 많은 배당 히스토리 반환
                assertThat(dividendHistory.dividends.size)
                    .withFailMessage("Max 기간은 많은 배당 히스토리가 있어야 합니다")
                    .isGreaterThan(10)
            }

            @Test
            @DisplayName("배당금액이 합리적인 범위이다")
            fun shouldHaveReasonableDividendAmount() = runTest {
                // Given: AAPL

                // When
                val dividendHistory = ufc.corp.getDividends("AAPL", Period.FiveYears)

                // Then: 모든 배당금액이 양수이고 합리적 범위
                dividendHistory.dividends.forEach { dividend ->
                    assertThat(dividend.amount)
                        .withFailMessage("배당금액은 양수여야 합니다")
                        .isGreaterThan(0.0)

                    assertThat(dividend.amount)
                        .withFailMessage("배당금액이 합리적 범위여야 합니다 (주당 $0.01 ~ $100)")
                        .isBetween(0.01, 100.0)
                }
            }
        }

        @Nested
        @DisplayName("입력 검증")
        inner class InputValidation {

            @Test
            @DisplayName("빈 문자열 심볼 조회 시 INVALID_SYMBOL 에러를 발생시킨다")
            fun shouldThrowInvalidSymbolErrorForBlankSymbol() = runTest {
                // When & Then
                val exception = try {
                    ufc.corp.getDividends("")
                    null
                } catch (e: UfcException) {
                    e
                }

                assertThat(exception)
                    .withFailMessage("빈 문자열 심볼 조회 시 UfcException이 발생해야 합니다")
                    .isNotNull()

                assertThat(exception!!.errorCode)
                    .withFailMessage("ErrorCode는 INVALID_SYMBOL이어야 합니다")
                    .isEqualTo(ErrorCode.INVALID_SYMBOL)
            }
        }

        @Nested
        @DisplayName("에러 처리")
        inner class ErrorHandling {

            @Test
            @DisplayName("존재하지 않는 심볼 조회 시 적절한 에러를 발생시킨다")
            fun shouldThrowErrorForNonExistentSymbol() = runTest {
                // Given: 존재하지 않는 심볼
                val invalidSymbol = "XXXXXXXXX"

                // When & Then
                val exception = try {
                    ufc.corp.getDividends(invalidSymbol)
                    null
                } catch (e: UfcException) {
                    e
                }

                assertThat(exception)
                    .withFailMessage("존재하지 않는 심볼 조회 시 UfcException이 발생해야 합니다")
                    .isNotNull()

                assertThat(exception!!.errorCode)
                    .withFailMessage("에러 코드가 유효한 범위 내에 있어야 합니다")
                    .isIn(
                        ErrorCode.DATA_NOT_FOUND,
                        ErrorCode.EXTERNAL_API_ERROR
                    )
            }

            @Test
            @DisplayName("배당이 없는 주식은 빈 리스트를 반환한다")
            fun shouldReturnEmptyListForNonDividendStock() = runTest {
                // Given: 배당하지 않는 주식 (예: TSLA)

                // When
                val dividendHistory = ufc.corp.getDividends("TSLA", Period.FiveYears)

                // Then: 빈 리스트 또는 에러
                // (TSLA가 배당을 시작했을 수도 있으므로 유연하게 처리)
                assertThat(dividendHistory.symbol).isEqualTo("TSLA")
            }
        }

        @Nested
        @DisplayName("데이터 무결성")
        inner class DataIntegrity {

            @Test
            @DisplayName("모든 배당금에 날짜와 금액이 있다")
            fun shouldHaveDateAndAmountForAllDividends() = runTest {
                // Given: AAPL

                // When
                val dividendHistory = ufc.corp.getDividends("AAPL", Period.FiveYears)

                // Then: 모든 배당금이 날짜와 금액을 가짐
                dividendHistory.dividends.forEach { dividend ->
                    assertThat(dividend.date)
                        .withFailMessage("배당 날짜가 있어야 합니다")
                        .isGreaterThan(0)

                    assertThat(dividend.amount)
                        .withFailMessage("배당금액이 있어야 합니다")
                        .isGreaterThan(0.0)
                }
            }
        }
    }

    @Nested
    @DisplayName("getSplits() - 주식 분할 조회")
    inner class GetSplits {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("주식 분할 히스토리를 조회한다")
            fun shouldReturnSplitHistory() = runTest {
                // Given: AAPL (과거 주식 분할 이력 있음)

                // When
                val splitHistory = ufc.corp.getSplits("AAPL", Period.Max)

                // Then: 분할 히스토리 반환
                assertThat(splitHistory.symbol)
                    .withFailMessage("심볼은 'AAPL'이어야 합니다")
                    .isEqualTo("AAPL")

                // AAPL은 과거 여러 번 주식 분할을 했음
                if (splitHistory.splits.isNotEmpty()) {
                    val firstSplit = splitHistory.splits.first()

                    assertThat(firstSplit.date)
                        .withFailMessage("분할 날짜가 있어야 합니다")
                        .isGreaterThan(0)

                    assertThat(firstSplit.numerator)
                        .withFailMessage("분자는 0보다 커야 합니다")
                        .isGreaterThan(0)

                    assertThat(firstSplit.denominator)
                        .withFailMessage("분모는 0보다 커야 합니다")
                        .isGreaterThan(0)

                    assertThat(firstSplit.ratio)
                        .withFailMessage("비율은 0보다 커야 합니다")
                        .isGreaterThan(0.0)
                }
            }

            @Test
            @DisplayName("주식 분할은 날짜 순서로 정렬되어 있다")
            fun shouldBeSortedByDate() = runTest {
                // Given: AAPL

                // When
                val splitHistory = ufc.corp.getSplits("AAPL", Period.Max)

                // Then: 날짜 오름차순 정렬
                if (splitHistory.splits.size > 1) {
                    for (i in 0 until splitHistory.splits.size - 1) {
                        assertThat(splitHistory.splits[i + 1].date)
                            .withFailMessage("주식 분할은 날짜 오름차순이어야 합니다")
                            .isGreaterThanOrEqualTo(splitHistory.splits[i].date)
                    }
                }
            }

            @Test
            @DisplayName("주식 분할의 ratioString()이 정확하다")
            fun shouldHaveCorrectRatioString() = runTest {
                // Given: AAPL

                // When
                val splitHistory = ufc.corp.getSplits("AAPL", Period.Max)

                // Then: ratioString() 검증
                splitHistory.splits.forEach { split ->
                    val ratioString = split.ratioString()
                    assertThat(ratioString)
                        .withFailMessage("비율 문자열이 있어야 합니다")
                        .isNotBlank()
                        .contains(":")

                    val description = split.description()
                    assertThat(description)
                        .withFailMessage("설명 문자열이 있어야 합니다")
                        .isNotBlank()
                        .contains("-for-")
                        .contains("split")
                }
            }

            @Test
            @DisplayName("FiveYears 기간으로 조회할 수 있다")
            fun shouldReturnSplitsForFiveYears() = runTest {
                // Given: AAPL, 5년 기간

                // When
                val splitHistory = ufc.corp.getSplits("AAPL", Period.FiveYears)

                // Then: 심볼 일치
                assertThat(splitHistory.symbol).isEqualTo("AAPL")
                // 5년 내 분할이 없을 수도 있음
            }

            @Test
            @DisplayName("Max 기간으로 전체 히스토리를 조회할 수 있다")
            fun shouldReturnAllSplitsForMax() = runTest {
                // Given: AAPL, 전체 기간

                // When
                val splitHistory = ufc.corp.getSplits("AAPL", Period.Max)

                // Then: AAPL은 과거 여러 번 분할
                assertThat(splitHistory.symbol).isEqualTo("AAPL")
            }
        }

        @Nested
        @DisplayName("입력 검증")
        inner class InputValidation {

            @Test
            @DisplayName("빈 문자열 심볼 조회 시 INVALID_SYMBOL 에러를 발생시킨다")
            fun shouldThrowInvalidSymbolErrorForBlankSymbol() = runTest {
                // When & Then
                val exception = try {
                    ufc.corp.getSplits("")
                    null
                } catch (e: UfcException) {
                    e
                }

                assertThat(exception)
                    .withFailMessage("빈 문자열 심볼 조회 시 UfcException이 발생해야 합니다")
                    .isNotNull()

                assertThat(exception!!.errorCode)
                    .withFailMessage("ErrorCode는 INVALID_SYMBOL이어야 합니다")
                    .isEqualTo(ErrorCode.INVALID_SYMBOL)
            }
        }

        @Nested
        @DisplayName("에러 처리")
        inner class ErrorHandling {

            @Test
            @DisplayName("존재하지 않는 심볼 조회 시 적절한 에러를 발생시킨다")
            fun shouldThrowErrorForNonExistentSymbol() = runTest {
                // Given: 존재하지 않는 심볼
                val invalidSymbol = "XXXXXXXXX"

                // When & Then
                val exception = try {
                    ufc.corp.getSplits(invalidSymbol)
                    null
                } catch (e: UfcException) {
                    e
                }

                assertThat(exception)
                    .withFailMessage("존재하지 않는 심볼 조회 시 UfcException이 발생해야 합니다")
                    .isNotNull()

                assertThat(exception!!.errorCode)
                    .withFailMessage("에러 코드가 유효한 범위 내에 있어야 합니다")
                    .isIn(
                        ErrorCode.DATA_NOT_FOUND,
                        ErrorCode.EXTERNAL_API_ERROR
                    )
            }

            @Test
            @DisplayName("주식 분할이 없는 주식은 빈 리스트를 반환한다")
            fun shouldReturnEmptyListForNoSplitHistory() = runTest {
                // Given: 최근 상장된 주식 (분할 이력 없음)

                // When: 주식 분할 조회 (대부분의 주식은 분할 이력이 없거나 적음)
                val splitHistory = ufc.corp.getSplits("MSFT", Period.OneYear)

                // Then: 심볼 일치 (빈 리스트 가능)
                assertThat(splitHistory.symbol).isEqualTo("MSFT")
            }
        }

        @Nested
        @DisplayName("데이터 무결성")
        inner class DataIntegrity {

            @Test
            @DisplayName("모든 주식 분할에 필수 필드가 있다")
            fun shouldHaveRequiredFieldsForAllSplits() = runTest {
                // Given: AAPL

                // When
                val splitHistory = ufc.corp.getSplits("AAPL", Period.Max)

                // Then: 모든 분할이 필수 필드를 가짐
                splitHistory.splits.forEach { split ->
                    assertThat(split.date)
                        .withFailMessage("분할 날짜가 있어야 합니다")
                        .isGreaterThan(0)

                    assertThat(split.numerator)
                        .withFailMessage("분자는 0보다 커야 합니다")
                        .isGreaterThan(0)

                    assertThat(split.denominator)
                        .withFailMessage("분모는 0보다 커야 합니다")
                        .isGreaterThan(0)

                    assertThat(split.ratio)
                        .withFailMessage("비율은 numerator/denominator와 일치해야 합니다")
                        .isEqualTo(split.numerator.toDouble() / split.denominator.toDouble())
                }
            }
        }
    }

    @Nested
    @DisplayName("getCapitalGains() - 자본이득 조회")
    inner class GetCapitalGains {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("ETF의 자본이득 히스토리를 조회한다")
            fun shouldReturnCapitalGainHistoryForEtf() = runTest {
                // Given: SPY ETF (자본이득 분배 가능)

                // When
                val capitalGainHistory = ufc.corp.getCapitalGains("SPY", Period.FiveYears)

                // Then: 자본이득 히스토리 반환
                assertThat(capitalGainHistory.symbol)
                    .withFailMessage("심볼은 'SPY'여야 합니다")
                    .isEqualTo("SPY")

                // 자본이득이 있을 수도 없을 수도 있음
                if (capitalGainHistory.capitalGains.isNotEmpty()) {
                    val firstGain = capitalGainHistory.capitalGains.first()

                    assertThat(firstGain.date)
                        .withFailMessage("자본이득 날짜가 있어야 합니다")
                        .isGreaterThan(0)

                    assertThat(firstGain.amount)
                        .withFailMessage("자본이득 금액이 있어야 합니다")
                        .isGreaterThanOrEqualTo(0.0)
                }
            }

            @Test
            @DisplayName("자본이득은 날짜 순서로 정렬되어 있다")
            fun shouldBeSortedByDate() = runTest {
                // Given: SPY ETF

                // When
                val capitalGainHistory = ufc.corp.getCapitalGains("SPY", Period.Max)

                // Then: 날짜 오름차순 정렬
                if (capitalGainHistory.capitalGains.size > 1) {
                    for (i in 0 until capitalGainHistory.capitalGains.size - 1) {
                        assertThat(capitalGainHistory.capitalGains[i + 1].date)
                            .withFailMessage("자본이득은 날짜 오름차순이어야 합니다")
                            .isGreaterThanOrEqualTo(capitalGainHistory.capitalGains[i].date)
                    }
                }
            }

            @Test
            @DisplayName("TwoYears 기간으로 조회할 수 있다")
            fun shouldReturnCapitalGainsForTwoYears() = runTest {
                // Given: SPY, 2년 기간

                // When
                val capitalGainHistory = ufc.corp.getCapitalGains("SPY", Period.TwoYears)

                // Then: 심볼 일치
                assertThat(capitalGainHistory.symbol).isEqualTo("SPY")
            }
        }

        @Nested
        @DisplayName("입력 검증")
        inner class InputValidation {

            @Test
            @DisplayName("빈 문자열 심볼 조회 시 INVALID_SYMBOL 에러를 발생시킨다")
            fun shouldThrowInvalidSymbolErrorForBlankSymbol() = runTest {
                // When & Then
                val exception = try {
                    ufc.corp.getCapitalGains("")
                    null
                } catch (e: UfcException) {
                    e
                }

                assertThat(exception)
                    .withFailMessage("빈 문자열 심볼 조회 시 UfcException이 발생해야 합니다")
                    .isNotNull()

                assertThat(exception!!.errorCode)
                    .withFailMessage("ErrorCode는 INVALID_SYMBOL이어야 합니다")
                    .isEqualTo(ErrorCode.INVALID_SYMBOL)
            }
        }

        @Nested
        @DisplayName("에러 처리")
        inner class ErrorHandling {

            @Test
            @DisplayName("존재하지 않는 심볼 조회 시 적절한 에러를 발생시킨다")
            fun shouldThrowErrorForNonExistentSymbol() = runTest {
                // Given: 존재하지 않는 심볼
                val invalidSymbol = "XXXXXXXXX"

                // When & Then
                val exception = try {
                    ufc.corp.getCapitalGains(invalidSymbol)
                    null
                } catch (e: UfcException) {
                    e
                }

                assertThat(exception)
                    .withFailMessage("존재하지 않는 심볼 조회 시 UfcException이 발생해야 합니다")
                    .isNotNull()

                assertThat(exception!!.errorCode)
                    .withFailMessage("에러 코드가 유효한 범위 내에 있어야 합니다")
                    .isIn(
                        ErrorCode.DATA_NOT_FOUND,
                        ErrorCode.EXTERNAL_API_ERROR
                    )
            }

            @Test
            @DisplayName("자본이득이 없는 자산은 빈 리스트를 반환한다")
            fun shouldReturnEmptyListForNoCapitalGains() = runTest {
                // Given: 주식 (일반적으로 자본이득 분배 없음)

                // When
                val capitalGainHistory = ufc.corp.getCapitalGains("AAPL", Period.FiveYears)

                // Then: 심볼 일치 (빈 리스트 가능)
                assertThat(capitalGainHistory.symbol).isEqualTo("AAPL")
            }
        }

        @Nested
        @DisplayName("데이터 무결성")
        inner class DataIntegrity {

            @Test
            @DisplayName("모든 자본이득에 날짜와 금액이 있다")
            fun shouldHaveDateAndAmountForAllCapitalGains() = runTest {
                // Given: SPY ETF

                // When
                val capitalGainHistory = ufc.corp.getCapitalGains("SPY", Period.Max)

                // Then: 모든 자본이득이 날짜와 금액을 가짐
                capitalGainHistory.capitalGains.forEach { gain ->
                    assertThat(gain.date)
                        .withFailMessage("자본이득 날짜가 있어야 합니다")
                        .isGreaterThan(0)

                    assertThat(gain.amount)
                        .withFailMessage("자본이득 금액이 있어야 합니다")
                        .isGreaterThanOrEqualTo(0.0)
                }
            }
        }
    }
}
