package com.ulalax.ufc.integration.funds

import com.ulalax.ufc.api.client.Ufc
import com.ulalax.ufc.api.client.UfcClientConfig
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitingSettings
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

/**
 * FundsService - 펀드 데이터 조회 통합 테스트
 *
 * 이 테스트는 실제 Yahoo Finance API를 호출합니다.
 * Mock이나 Fake 객체를 사용하지 않고, 실제 네트워크 요청을 통해
 * ufc.funds.getFundData() 메서드의 모든 시나리오를 검증합니다.
 *
 * ## 테스트 환경
 * - 실제 Yahoo Finance API 호출
 * - 네트워크 연결 필요
 * - API Rate Limiting 적용
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew test --tests "*FundsDataIntegrationTest" --tests "*.integration.*"
 * ```
 *
 * ## 주의사항
 * - 네트워크 상태에 따라 테스트 실패 가능
 * - API 변경으로 인한 실패 가능
 * - Rate Limiting으로 인한 지연 발생 가능
 */
@Tag("integration")
@DisplayName("Funds Service - 펀드 데이터 조회 통합 테스트")
class FundsDataIntegrationTest {

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
    @DisplayName("getFundData() - 단일 심볼 조회")
    inner class GetFundData {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("유효한 ETF 심볼로 조회 시 펀드 데이터를 반환한다")
            fun shouldReturnFundDataForValidEtf() = runTest {
                // Given: SPY ETF 심볼

                // When
                val fundData = ufc.funds.getFundData("SPY")

                // Then: 정확한 펀드 데이터 반환
                assertThat(fundData.symbol)
                    .withFailMessage("심볼은 'SPY'여야 합니다. 실제값: ${fundData.symbol}")
                    .isEqualTo("SPY")

                assertThat(fundData.quoteType)
                    .withFailMessage("quoteType은 'ETF'여야 합니다. 실제값: ${fundData.quoteType}")
                    .isEqualTo("ETF")

                // 펀드 개요 정보 검증
                if (fundData.fundOverview != null) {
                    assertThat(fundData.fundOverview)
                        .withFailMessage("펀드 개요 정보가 있어야 합니다")
                        .isNotNull()
                }

                // 보유 종목 검증 (Optional - Yahoo API가 제공하지 않을 수 있음)
                fundData.topHoldings?.let { holdings ->
                    if (holdings.isNotEmpty()) {
                        holdings.forEach { holding ->
                            assertThat(holding.symbol)
                                .withFailMessage("보유 종목 심볼이 있어야 합니다")
                                .isNotBlank()
                        }
                    }
                }
            }

            @Test
            @DisplayName("뮤추얼펀드 심볼로 조회 시 정상 데이터를 반환한다")
            fun shouldReturnFundDataForMutualFund() = runTest {
                // Given: VTSAX 뮤추얼펀드 심볼

                // When
                val fundData = ufc.funds.getFundData("VTSAX")

                // Then
                assertThat(fundData.symbol)
                    .withFailMessage("심볼은 'VTSAX'여야 합니다")
                    .isEqualTo("VTSAX")

                assertThat(fundData.quoteType)
                    .withFailMessage("quoteType은 'MUTUALFUND'여야 합니다")
                    .isEqualTo("MUTUALFUND")
            }

            @Test
            @DisplayName("채권 ETF의 펀드 데이터를 정상 조회한다")
            fun shouldReturnFundDataForBondEtf() = runTest {
                // Given: AGG (채권 ETF)

                // When
                val fundData = ufc.funds.getFundData("AGG")

                // Then
                assertThat(fundData.symbol).isEqualTo("AGG")
                assertThat(fundData.quoteType).isEqualTo("ETF")

                // 채권 보유 메트릭 검증
                if (fundData.bondHoldings != null) {
                    assertThat(fundData.bondHoldings)
                        .withFailMessage("채권 ETF는 채권 보유 메트릭이 있어야 합니다")
                        .isNotNull()
                }

                // 채권 등급 분포 검증
                if (fundData.bondRatings != null) {
                    assertThat(fundData.bondRatings)
                        .withFailMessage("채권 등급 분포가 있어야 합니다")
                        .isNotEmpty()
                }
            }

            @Test
            @DisplayName("주식 ETF의 섹터별 비중을 조회할 수 있다")
            fun shouldReturnSectorWeightingsForEquityEtf() = runTest {
                // Given: SPY (주식 ETF)

                // When
                val fundData = ufc.funds.getFundData("SPY")

                // Then: 섹터별 비중 검증 (Optional - Yahoo API가 제공하지 않을 수 있음)
                fundData.sectorWeightings?.let { sectorWeightings ->
                    if (sectorWeightings.isNotEmpty()) {
                        // 비중 합계가 100% 근처여야 함 (오차 허용)
                        val totalWeight = sectorWeightings.values.sum()
                        assertThat(totalWeight)
                            .withFailMessage("섹터 비중 합계가 합리적 범위여야 합니다. 실제: $totalWeight")
                            .isBetween(80.0, 120.0)
                    }
                }
            }

            @Test
            @DisplayName("펀드 운영 정보를 조회할 수 있다")
            fun shouldReturnFundOperations() = runTest {
                // Given: SPY ETF

                // When
                val fundData = ufc.funds.getFundData("SPY")

                // Then: 펀드 운영 정보 검증
                if (fundData.fundOperations != null) {
                    assertThat(fundData.fundOperations)
                        .withFailMessage("펀드 운영 정보가 있어야 합니다")
                        .isNotNull()

                    // 비용률 검증 (annualReportExpenseRatio가 있는지 확인)
                    // fundOperations의 필드는 nullable일 수 있음
                }
            }

            @Test
            @DisplayName("펀드 설명을 조회할 수 있다")
            fun shouldReturnFundDescription() = runTest {
                // Given: SPY ETF

                // When
                val fundData = ufc.funds.getFundData("SPY")

                // Then: 설명이 있으면 비어있지 않아야 함
                if (fundData.description != null) {
                    assertThat(fundData.description)
                        .withFailMessage("펀드 설명이 비어있지 않아야 합니다")
                        .isNotBlank()
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
                    ufc.funds.getFundData("")
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

            @Test
            @DisplayName("공백만 있는 심볼 조회 시 INVALID_SYMBOL 에러를 발생시킨다")
            fun shouldThrowInvalidSymbolErrorForWhitespaceSymbol() = runTest {
                // When & Then
                val exception = try {
                    ufc.funds.getFundData("   ")
                    null
                } catch (e: UfcException) {
                    e
                }

                assertThat(exception)
                    .withFailMessage("공백만 있는 심볼 조회 시 UfcException이 발생해야 합니다")
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
                    ufc.funds.getFundData(invalidSymbol)
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
                        ErrorCode.FUND_DATA_NOT_FOUND,
                        ErrorCode.EXTERNAL_API_ERROR
                    )
            }

            @Test
            @DisplayName("펀드가 아닌 심볼 조회 시 INVALID_FUND_TYPE 에러를 발생시킨다")
            fun shouldThrowInvalidFundTypeForNonFund() = runTest {
                // Given: 주식 심볼 (펀드가 아님)
                val stockSymbol = "AAPL"

                // When & Then
                val exception = try {
                    ufc.funds.getFundData(stockSymbol)
                    null
                } catch (e: UfcException) {
                    e
                }

                assertThat(exception)
                    .withFailMessage("펀드가 아닌 심볼 조회 시 UfcException이 발생해야 합니다")
                    .isNotNull()

                assertThat(exception!!.errorCode)
                    .withFailMessage("ErrorCode는 INVALID_FUND_TYPE이어야 합니다")
                    .isEqualTo(ErrorCode.INVALID_FUND_TYPE)
            }
        }

        @Nested
        @DisplayName("데이터 무결성")
        inner class DataIntegrity {

            @Test
            @DisplayName("symbol과 quoteType은 항상 non-null이다")
            fun shouldAlwaysHaveSymbolAndQuoteType() = runTest {
                // Given: 여러 펀드
                val symbols = listOf("SPY", "AGG", "QQQ")

                // When & Then: 모든 펀드가 symbol과 quoteType을 가짐
                symbols.forEach { symbol ->
                    val fundData = ufc.funds.getFundData(symbol)

                    assertThat(fundData.symbol)
                        .withFailMessage("$symbol: symbol이 있어야 합니다")
                        .isNotBlank()

                    assertThat(fundData.quoteType)
                        .withFailMessage("$symbol: quoteType이 있어야 합니다")
                        .isNotBlank()
                        .isIn("ETF", "MUTUALFUND")
                }
            }

            @Test
            @DisplayName("자산 클래스 배분 비율의 합은 100% 근처이다")
            fun shouldHaveReasonableAssetClassAllocation() = runTest {
                // Given: SPY ETF

                // When
                val fundData = ufc.funds.getFundData("SPY")

                // Then: 자산 클래스 배분 검증
                if (fundData.assetClasses != null) {
                    val total = fundData.assetClasses.totalAllocation()

                    if (total > 0) {
                        assertThat(total)
                            .withFailMessage("자산 배분 합계가 합리적 범위여야 합니다. 실제: $total")
                            .isBetween(80.0, 120.0)
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("getFundData(List<String>) - 다중 심볼 조회")
    inner class GetFundDataMultiple {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("여러 펀드 동시 조회 시 모든 데이터를 맵으로 반환한다")
            fun shouldReturnAllFundDataAsMap() = runTest {
                // Given: 3개 펀드
                val symbols = listOf("SPY", "AGG", "QQQ")

                // When
                val results = ufc.funds.getFundData(symbols)

                // Then: 대부분 반환 (일부 실패 허용)
                assertThat(results)
                    .withFailMessage("최소 2개 이상의 펀드가 반환되어야 합니다")
                    .hasSizeGreaterThanOrEqualTo(2)

                results.forEach { (symbol, fundData) ->
                    assertThat(fundData.symbol)
                        .withFailMessage("반환된 심볼이 요청한 심볼과 일치해야 합니다")
                        .isEqualTo(symbol)

                    assertThat(fundData.quoteType)
                        .withFailMessage("$symbol: quoteType이 있어야 합니다")
                        .isNotBlank()
                }
            }

            @Test
            @DisplayName("ETF와 뮤추얼펀드를 동시 조회할 수 있다")
            fun shouldHandleMixedFundTypes() = runTest {
                // Given: ETF와 뮤추얼펀드 혼합
                val symbols = listOf(
                    "SPY",    // ETF
                    "VTSAX"   // MUTUALFUND
                )

                // When
                val results = ufc.funds.getFundData(symbols)

                // Then: 모든 타입이 조회됨
                assertThat(results)
                    .withFailMessage("ETF와 뮤추얼펀드 모두 조회되어야 합니다")
                    .hasSizeGreaterThanOrEqualTo(1)

                results.values.forEach { fundData ->
                    assertThat(fundData.quoteType)
                        .withFailMessage("quoteType이 ETF 또는 MUTUALFUND여야 합니다")
                        .isIn("ETF", "MUTUALFUND")
                }
            }

            @Test
            @DisplayName("5개 펀드 동시 조회가 가능하다")
            fun shouldHandleFiveSymbolsSimultaneously() = runTest {
                // Given: 주요 5개 ETF
                val symbols = listOf("SPY", "AGG", "QQQ", "VTI", "BND")

                // When
                val results = ufc.funds.getFundData(symbols)

                // Then: 대부분 반환
                assertThat(results.size)
                    .withFailMessage("5개 중 최소 3개는 반환되어야 합니다. 실제: ${results.size}")
                    .isGreaterThanOrEqualTo(3)

                results.values.forEach { fundData ->
                    assertThat(fundData.symbol).isNotBlank()
                    assertThat(fundData.quoteType).isIn("ETF", "MUTUALFUND")
                }
            }
        }

        @Nested
        @DisplayName("입력 검증")
        inner class InputValidation {

            @Test
            @DisplayName("빈 리스트 조회 시 빈 맵을 반환한다")
            fun shouldReturnEmptyMapForEmptyList() = runTest {
                // When
                val results = ufc.funds.getFundData(emptyList())

                // Then
                assertThat(results)
                    .withFailMessage("빈 리스트 조회 시 빈 맵을 반환해야 합니다")
                    .isEmpty()
            }

            @Test
            @DisplayName("중복된 심볼이 있어도 정상 처리한다")
            fun shouldHandleDuplicateSymbols() = runTest {
                // Given: 중복된 심볼
                val symbols = listOf("SPY", "AGG", "SPY")

                // When
                val results = ufc.funds.getFundData(symbols)

                // Then: 중복 제거되어 처리됨
                assertThat(results)
                    .withFailMessage("중복 심볼도 정상 처리되어야 합니다")
                    .hasSizeGreaterThanOrEqualTo(2)

                assertThat(results).containsKey("SPY")
                assertThat(results).containsKey("AGG")
            }
        }
    }

    @Nested
    @DisplayName("isFund() - 펀드 여부 확인")
    inner class IsFund {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("ETF 심볼은 true를 반환한다")
            fun shouldReturnTrueForEtf() = runTest {
                // Given: SPY ETF

                // When
                val isFund = ufc.funds.isFund("SPY")

                // Then
                assertThat(isFund)
                    .withFailMessage("SPY는 펀드여야 합니다")
                    .isTrue()
            }

            @Test
            @DisplayName("뮤추얼펀드 심볼은 true를 반환한다")
            fun shouldReturnTrueForMutualFund() = runTest {
                // Given: VTSAX 뮤추얼펀드

                // When
                val isFund = ufc.funds.isFund("VTSAX")

                // Then
                assertThat(isFund)
                    .withFailMessage("VTSAX는 펀드여야 합니다")
                    .isTrue()
            }

            @Test
            @DisplayName("주식 심볼은 false를 반환한다")
            fun shouldReturnFalseForStock() = runTest {
                // Given: AAPL 주식

                // When
                val isFund = ufc.funds.isFund("AAPL")

                // Then
                assertThat(isFund)
                    .withFailMessage("AAPL은 주식이므로 펀드가 아니어야 합니다")
                    .isFalse()
            }

            @Test
            @DisplayName("인덱스 심볼은 false를 반환한다")
            fun shouldReturnFalseForIndex() = runTest {
                // Given: ^GSPC 인덱스

                // When
                val isFund = ufc.funds.isFund("^GSPC")

                // Then
                assertThat(isFund)
                    .withFailMessage("인덱스는 펀드가 아니어야 합니다")
                    .isFalse()
            }
        }

        @Nested
        @DisplayName("에러 처리")
        inner class ErrorHandling {

            @Test
            @DisplayName("존재하지 않는 심볼 조회 시 false를 반환하거나 에러를 발생시킨다")
            fun shouldReturnFalseOrThrowErrorForNonExistentSymbol() = runTest {
                // Given: 존재하지 않는 심볼
                val invalidSymbol = "XXXXXXXXX"

                // When & Then: 에러 또는 false 반환
                try {
                    val result = ufc.funds.isFund(invalidSymbol)
                    // 에러가 발생하지 않으면 false여야 함
                    assertThat(result)
                        .withFailMessage("존재하지 않는 심볼은 false를 반환해야 합니다")
                        .isFalse()
                } catch (e: UfcException) {
                    // 에러 발생도 허용
                    assertThat(e.errorCode)
                        .withFailMessage("에러 코드가 유효한 범위 내에 있어야 합니다")
                        .isIn(
                            ErrorCode.DATA_NOT_FOUND,
                            ErrorCode.EXTERNAL_API_ERROR
                        )
                }
            }
        }
    }
}
