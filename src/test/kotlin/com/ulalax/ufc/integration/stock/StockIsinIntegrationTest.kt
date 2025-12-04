package com.ulalax.ufc.integration.stock

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
 * StockService - ISIN 코드 조회 통합 테스트
 *
 * 이 테스트는 실제 Yahoo Finance API를 호출합니다.
 * ufc.stock.getIsin() 메서드의 모든 시나리오를 검증합니다.
 *
 * ISIN (International Securities Identification Number)은
 * 국제적으로 증권을 고유하게 식별하는 12자리 코드입니다.
 */
@Tag("integration")
@DisplayName("Stock Service - ISIN 코드 조회 통합 테스트")
class StockIsinIntegrationTest {

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
    @DisplayName("getIsin() - 단일 심볼 조회")
    inner class GetIsin {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("유효한 심볼로 조회 시 ISIN 코드를 반환한다")
            fun shouldReturnIsinForValidSymbol() = runTest {
                // Given: AAPL 심볼

                // When
                val isin = ufc.stock.getIsin("AAPL")

                // Then: ISIN 형식 검증 (12자리 영숫자)
                assertThat(isin)
                    .withFailMessage("ISIN 코드가 반환되어야 합니다")
                    .isNotBlank()
                    .hasSize(12)
                    .matches("[A-Z]{2}[A-Z0-9]{9}[0-9]")

                // AAPL의 ISIN은 US로 시작 (미국 주식)
                assertThat(isin)
                    .withFailMessage("AAPL의 ISIN은 US로 시작해야 합니다")
                    .startsWith("US")
            }

            @Test
            @DisplayName("다양한 주식의 ISIN 코드를 정상 조회한다")
            fun shouldReturnIsinForVariousStocks() = runTest {
                // Given: 여러 주식
                val symbols = listOf("MSFT", "GOOGL", "JPM")
                var successCount = 0
                val isinPattern = Regex("[A-Z]{2}[A-Z0-9]{9}[0-9]")

                // When & Then: ISIN 조회 시도 (Business Insider rate limiting으로 일부 실패 허용)
                symbols.forEach { symbol ->
                    try {
                        val isin = ufc.stock.getIsin(symbol)
                        // ISIN 형식 검증 (실패해도 예외 던지지 않고 카운트만 안함)
                        if (isin.length == 12 && isin.matches(isinPattern) && isin.startsWith("US")) {
                            successCount++
                        }
                    } catch (e: UfcException) {
                        // Business Insider rate limiting 또는 검색 실패 허용
                    } catch (e: Exception) {
                        // 기타 예외도 허용
                    }
                }

                // 최소 1개는 성공해야 함
                assertThat(successCount)
                    .withFailMessage("최소 1개 이상의 ISIN 조회가 성공해야 합니다. 성공: $successCount/${symbols.size}")
                    .isGreaterThanOrEqualTo(1)
            }

            @Test
            @DisplayName("ETF의 ISIN 코드를 조회할 수 있다")
            fun shouldReturnIsinForEtf() = runTest {
                // Given: SPY ETF

                // When
                val isin = ufc.stock.getIsin("SPY")

                // Then: 유효한 ISIN 형식
                assertThat(isin)
                    .withFailMessage("SPY ETF의 ISIN이 유효해야 합니다")
                    .hasSize(12)
                    .matches("[A-Z]{2}[A-Z0-9]{9}[0-9]")
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
                    ufc.stock.getIsin("")
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
                    ufc.stock.getIsin(invalidSymbol)
                    null
                } catch (e: UfcException) {
                    e
                }

                assertThat(exception)
                    .withFailMessage("존재하지 않는 심볼 조회 시 UfcException이 발생해야 합니다")
                    .isNotNull()

                assertThat(exception!!.errorCode)
                    .withFailMessage("에러 코드가 유효한 범위 내에 있어야 합니다: ${exception.errorCode}")
                    .isIn(
                        ErrorCode.DATA_NOT_FOUND,
                        ErrorCode.EXTERNAL_API_ERROR,
                        ErrorCode.ISIN_NOT_FOUND
                    )
            }

            @Test
            @DisplayName("ISIN이 없는 심볼 조회 시 에러를 발생시킨다")
            fun shouldThrowErrorWhenIsinNotAvailable() = runTest {
                // Given: 인덱스는 ISIN이 없을 수 있음
                val indexSymbol = "^GSPC"

                // When & Then: 에러 또는 빈 문자열
                try {
                    val isin = ufc.stock.getIsin(indexSymbol)
                    // ISIN이 반환되면 유효한 형식이어야 함
                    if (isin.isNotBlank()) {
                        assertThat(isin).hasSize(12)
                    }
                } catch (e: UfcException) {
                    // 에러 발생 허용
                    assertThat(e.errorCode).isIn(
                        ErrorCode.DATA_NOT_FOUND,
                        ErrorCode.EXTERNAL_API_ERROR,
                        ErrorCode.ISIN_NOT_FOUND
                    )
                }
            }
        }

        @Nested
        @DisplayName("데이터 무결성")
        inner class DataIntegrity {

            @Test
            @DisplayName("동일 심볼의 ISIN은 항상 같다")
            fun shouldReturnConsistentIsin() = runTest {
                // Given: AAPL 심볼

                // When: 2회 조회
                val isin1 = ufc.stock.getIsin("AAPL")
                val isin2 = ufc.stock.getIsin("AAPL")

                // Then: 동일한 ISIN
                assertThat(isin2)
                    .withFailMessage("동일 심볼의 ISIN은 항상 같아야 합니다")
                    .isEqualTo(isin1)
            }

            @Test
            @DisplayName("ISIN 코드는 체크섬을 포함한다")
            fun shouldIncludeCheckDigit() = runTest {
                // Given: AAPL 심볼

                // When
                val isin = ufc.stock.getIsin("AAPL")

                // Then: 마지막 자리는 숫자 (체크섬)
                assertThat(isin.last())
                    .withFailMessage("ISIN의 마지막 자리는 체크섬 숫자여야 합니다")
                    .isIn('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
            }
        }
    }

    @Nested
    @DisplayName("getIsin(List<String>) - 다중 심볼 조회")
    inner class GetIsinMultiple {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("여러 심볼 동시 조회 시 모든 ISIN을 맵으로 반환한다")
            fun shouldReturnAllIsinAsMap() = runTest {
                // Given: 3개 심볼
                val symbols = listOf("AAPL", "MSFT", "GOOGL")

                // When
                val results = ufc.stock.getIsin(symbols)

                // Then: 대부분 반환
                assertThat(results)
                    .withFailMessage("최소 2개 이상의 ISIN이 반환되어야 합니다")
                    .hasSizeGreaterThanOrEqualTo(2)

                results.forEach { (symbol, isin) ->
                    assertThat(isin)
                        .withFailMessage("$symbol 의 ISIN이 유효해야 합니다")
                        .hasSize(12)
                        .matches("[A-Z]{2}[A-Z0-9]{9}[0-9]")
                }
            }

            @Test
            @DisplayName("10개 심볼 동시 조회가 가능하다")
            fun shouldHandleTenSymbolsSimultaneously() = runTest {
                // Given: 주요 10개 심볼
                val symbols = listOf(
                    "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA",
                    "META", "NVDA", "JPM", "V", "WMT"
                )

                // When
                val results = ufc.stock.getIsin(symbols)

                // Then: 대부분 반환 (일부 실패 허용 - Business Insider rate limiting)
                assertThat(results.size)
                    .withFailMessage("10개 중 최소 5개는 반환되어야 합니다. 실제: ${results.size}")
                    .isGreaterThanOrEqualTo(5)

                // 모든 ISIN이 유효한 형식
                results.values.forEach { isin ->
                    assertThat(isin)
                        .withFailMessage("ISIN이 유효한 형식이어야 합니다")
                        .hasSize(12)
                        .matches("[A-Z]{2}[A-Z0-9]{9}[0-9]")
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
                val results = ufc.stock.getIsin(emptyList())

                // Then
                assertThat(results)
                    .withFailMessage("빈 리스트 조회 시 빈 맵을 반환해야 합니다")
                    .isEmpty()
            }
        }
    }
}
