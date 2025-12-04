package com.ulalax.ufc.integration.stock

import com.ulalax.ufc.api.client.Ufc
import com.ulalax.ufc.api.client.UfcClientConfig
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.domain.stock.AssetType
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitingSettings
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

/**
 * StockService - 빠른 정보 조회 통합 테스트
 *
 * 이 테스트는 실제 Yahoo Finance API를 호출합니다.
 * ufc.stock.getFastInfo() 메서드의 모든 시나리오를 검증합니다.
 */
@Tag("integration")
@DisplayName("Stock Service - 빠른 정보 조회 통합 테스트")
class StockFastInfoIntegrationTest {

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
    @DisplayName("getFastInfo() - 단일 심볼 조회")
    inner class GetFastInfo {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("유효한 심볼로 조회 시 빠른 정보를 반환한다")
            fun shouldReturnFastInfoForValidSymbol() = runTest {
                // Given: AAPL 심볼

                // When
                val fastInfo = ufc.stock.getFastInfo("AAPL")

                // Then: 모든 필드가 non-null
                assertThat(fastInfo.symbol)
                    .withFailMessage("심볼은 'AAPL'이어야 합니다")
                    .isEqualTo("AAPL")

                assertThat(fastInfo.currency)
                    .withFailMessage("통화 정보가 있어야 합니다")
                    .isNotBlank()

                assertThat(fastInfo.exchange)
                    .withFailMessage("거래소 정보가 있어야 합니다")
                    .isNotBlank()

                assertThat(fastInfo.quoteType)
                    .withFailMessage("자산 타입이 있어야 합니다")
                    .isNotNull()
            }

            @Test
            @DisplayName("주식 심볼의 quoteType은 EQUITY이다")
            fun shouldReturnEquityTypeForStock() = runTest {
                // Given: MSFT 주식 심볼

                // When
                val fastInfo = ufc.stock.getFastInfo("MSFT")

                // Then
                assertThat(fastInfo.quoteType)
                    .withFailMessage("주식의 quoteType은 EQUITY여야 합니다")
                    .isEqualTo(AssetType.EQUITY)
            }

            @Test
            @DisplayName("ETF 심볼의 quoteType은 ETF이다")
            fun shouldReturnEtfTypeForEtf() = runTest {
                // Given: SPY ETF 심볼

                // When
                val fastInfo = ufc.stock.getFastInfo("SPY")

                // Then
                assertThat(fastInfo.quoteType)
                    .withFailMessage("ETF의 quoteType은 ETF여야 합니다")
                    .isEqualTo(AssetType.ETF)
            }

            @Test
            @DisplayName("인덱스 심볼의 quoteType은 INDEX이다")
            fun shouldReturnIndexTypeForIndex() = runTest {
                // Given: ^GSPC 인덱스 심볼

                // When
                val fastInfo = ufc.stock.getFastInfo("^GSPC")

                // Then
                assertThat(fastInfo.quoteType)
                    .withFailMessage("인덱스의 quoteType은 INDEX여야 합니다")
                    .isEqualTo(AssetType.INDEX)
            }

            @Test
            @DisplayName("다양한 통화의 심볼을 정상 처리한다")
            fun shouldHandleVariousCurrencies() = runTest {
                // Given: USD 통화 주식
                val usdStock = ufc.stock.getFastInfo("AAPL")

                // Then
                assertThat(usdStock.currency)
                    .withFailMessage("AAPL은 USD 통화여야 합니다")
                    .isEqualTo("USD")
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
                    ufc.stock.getFastInfo("")
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
                    ufc.stock.getFastInfo(invalidSymbol)
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
        }

        @Nested
        @DisplayName("데이터 무결성")
        inner class DataIntegrity {

            @Test
            @DisplayName("모든 필드가 non-null이다")
            fun shouldHaveAllNonNullFields() = runTest {
                // Given: 여러 심볼
                val symbols = listOf("AAPL", "MSFT", "SPY")

                // When & Then: 모든 필드가 non-null
                symbols.forEach { symbol ->
                    val fastInfo = ufc.stock.getFastInfo(symbol)

                    assertThat(fastInfo.symbol)
                        .withFailMessage("$symbol: symbol이 non-null이어야 합니다")
                        .isNotBlank()

                    assertThat(fastInfo.currency)
                        .withFailMessage("$symbol: currency가 non-null이어야 합니다")
                        .isNotBlank()

                    assertThat(fastInfo.exchange)
                        .withFailMessage("$symbol: exchange가 non-null이어야 합니다")
                        .isNotBlank()

                    assertThat(fastInfo.quoteType)
                        .withFailMessage("$symbol: quoteType이 non-null이어야 합니다")
                        .isNotNull()
                }
            }
        }
    }

    @Nested
    @DisplayName("getFastInfo(List<String>) - 다중 심볼 조회")
    inner class GetFastInfoMultiple {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("여러 심볼 동시 조회 시 모든 데이터를 맵으로 반환한다")
            fun shouldReturnAllFastInfoAsMap() = runTest {
                // Given: 3개 심볼
                val symbols = listOf("AAPL", "MSFT", "GOOGL")

                // When
                val results = ufc.stock.getFastInfo(symbols)

                // Then: 대부분 반환
                assertThat(results)
                    .withFailMessage("최소 2개 이상의 심볼이 반환되어야 합니다")
                    .hasSizeGreaterThanOrEqualTo(2)

                results.forEach { (symbol, fastInfo) ->
                    assertThat(fastInfo.symbol)
                        .withFailMessage("반환된 심볼이 요청한 심볼과 일치해야 합니다")
                        .isEqualTo(symbol)

                    assertThat(fastInfo.currency).isNotBlank()
                    assertThat(fastInfo.exchange).isNotBlank()
                    assertThat(fastInfo.quoteType).isNotNull()
                }
            }

            @Test
            @DisplayName("다양한 타입의 심볼을 동시 조회 가능하다")
            fun shouldHandleMixedSymbolTypes() = runTest {
                // Given: 주식, ETF, 인덱스 혼합
                val symbols = listOf(
                    "AAPL",  // 주식
                    "SPY",   // ETF
                    "^GSPC"  // 인덱스
                )

                // When
                val results = ufc.stock.getFastInfo(symbols)

                // Then: 모든 타입이 조회됨
                assertThat(results)
                    .withFailMessage("모든 타입의 심볼이 조회되어야 합니다")
                    .hasSizeGreaterThanOrEqualTo(2)

                results.values.forEach { fastInfo ->
                    assertThat(fastInfo.quoteType).isNotNull()
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
                val results = ufc.stock.getFastInfo(emptyList())

                // Then
                assertThat(results)
                    .withFailMessage("빈 리스트 조회 시 빈 맵을 반환해야 합니다")
                    .isEmpty()
            }
        }
    }
}
