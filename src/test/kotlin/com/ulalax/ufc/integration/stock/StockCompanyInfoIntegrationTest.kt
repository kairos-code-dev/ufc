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
 * StockService - 회사 기본 정보 조회 통합 테스트
 *
 * 이 테스트는 실제 Yahoo Finance API를 호출합니다.
 * Mock이나 Fake 객체를 사용하지 않고, 실제 네트워크 요청을 통해
 * ufc.stock.getCompanyInfo() 메서드의 모든 시나리오를 검증합니다.
 *
 * ## 테스트 환경
 * - 실제 Yahoo Finance API 호출
 * - 네트워크 연결 필요
 * - API Rate Limiting 적용
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew test --tests "*StockCompanyInfoIntegrationTest" --tests "*.integration.*"
 * ```
 *
 * ## 주의사항
 * - 네트워크 상태에 따라 테스트 실패 가능
 * - API 변경으로 인한 실패 가능
 * - Rate Limiting으로 인한 지연 발생 가능
 */
@Tag("integration")
@DisplayName("Stock Service - 회사 기본 정보 조회 통합 테스트")
class StockCompanyInfoIntegrationTest {

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
    @DisplayName("getCompanyInfo() - 단일 심볼 조회")
    inner class GetCompanyInfo {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("유효한 심볼로 조회 시 회사 정보를 반환한다")
            fun shouldReturnCompanyInfoForValidSymbol() = runTest {
                // Given: AAPL 심볼

                // When: AAPL 회사 정보 조회
                val companyInfo = ufc.stock.getCompanyInfo("AAPL")

                // Then: 정확한 회사 데이터 반환
                assertThat(companyInfo.symbol)
                    .withFailMessage("심볼은 'AAPL'이어야 합니다. 실제값: ${companyInfo.symbol}")
                    .isEqualTo("AAPL")

                assertThat(companyInfo.longName)
                    .withFailMessage("회사명이 있어야 합니다")
                    .isNotBlank()

                assertThat(companyInfo.sector)
                    .withFailMessage("섹터 정보가 있어야 합니다")
                    .isNotNull()

                assertThat(companyInfo.industry)
                    .withFailMessage("업종 정보가 있어야 합니다")
                    .isNotNull()

                assertThat(companyInfo.currency)
                    .withFailMessage("통화는 USD여야 합니다. 실제값: ${companyInfo.currency}")
                    .isEqualTo("USD")
            }

            @Test
            @DisplayName("기술주 심볼로 조회 시 상세 정보를 포함한다")
            fun shouldReturnDetailedInfoForTechStock() = runTest {
                // Given: MSFT 심볼

                // When
                val companyInfo = ufc.stock.getCompanyInfo("MSFT")

                // Then: 상세 정보 검증
                assertThat(companyInfo.symbol).isEqualTo("MSFT")
                assertThat(companyInfo.longName).isNotBlank()

                // 연락처 정보
                if (companyInfo.website != null) {
                    assertThat(companyInfo.website).startsWith("http")
                }

                // 기업 정보
                if (companyInfo.employees != null) {
                    assertThat(companyInfo.employees).isGreaterThan(0)
                }

                if (companyInfo.description != null) {
                    assertThat(companyInfo.description).isNotBlank()
                }
            }

            @Test
            @DisplayName("금융주 심볼로 조회 시 정상 데이터를 반환한다")
            fun shouldReturnInfoForFinancialStock() = runTest {
                // Given: JPM 심볼

                // When
                val companyInfo = ufc.stock.getCompanyInfo("JPM")

                // Then
                assertThat(companyInfo.symbol).isEqualTo("JPM")
                assertThat(companyInfo.longName).isNotBlank()
                assertThat(companyInfo.sector)
                    .withFailMessage("JPM은 금융 섹터여야 합니다")
                    .containsIgnoringCase("Financial")
            }

            @Test
            @DisplayName("다양한 거래소의 심볼을 정상 처리한다")
            fun shouldHandleVariousExchanges() = runTest {
                // Given: 나스닥 상장 주식
                val nasdaqStock = ufc.stock.getCompanyInfo("GOOGL")

                // Then: 거래소 정보 확인
                assertThat(nasdaqStock.exchange)
                    .withFailMessage("GOOGL의 거래소 정보가 있어야 합니다")
                    .isNotBlank()
            }

            @Test
            @DisplayName("하이픈 포함 심볼을 정상 처리한다")
            fun shouldHandleSymbolsWithHyphen() = runTest {
                // Given: 하이픈 포함 심볼 (BRK-A)

                // When
                val companyInfo = ufc.stock.getCompanyInfo("BRK-A")

                // Then
                assertThat(companyInfo.symbol)
                    .withFailMessage("하이픈 포함 심볼을 정상 처리해야 합니다")
                    .isEqualTo("BRK-A")

                assertThat(companyInfo.longName)
                    .withFailMessage("BRK-A 회사명이 있어야 합니다")
                    .isNotBlank()
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
                    ufc.stock.getCompanyInfo("")
                    null
                } catch (e: UfcException) {
                    e
                }

                assertThat(exception)
                    .withFailMessage("빈 문자열 심볼 조회 시 UfcException이 발생해야 합니다")
                    .isNotNull()
                    .isInstanceOf(UfcException::class.java)

                assertThat(exception!!.errorCode)
                    .withFailMessage("ErrorCode는 INVALID_SYMBOL이어야 합니다. 실제: ${exception.errorCode}")
                    .isEqualTo(ErrorCode.INVALID_SYMBOL)
            }

            @Test
            @DisplayName("공백만 있는 심볼 조회 시 INVALID_SYMBOL 에러를 발생시킨다")
            fun shouldThrowInvalidSymbolErrorForWhitespaceSymbol() = runTest {
                // When & Then
                val exception = try {
                    ufc.stock.getCompanyInfo("   ")
                    null
                } catch (e: UfcException) {
                    e
                }

                assertThat(exception)
                    .withFailMessage("공백만 있는 심볼 조회 시 UfcException이 발생해야 합니다")
                    .isNotNull()
                    .isInstanceOf(UfcException::class.java)

                assertThat(exception!!.errorCode)
                    .withFailMessage("ErrorCode는 INVALID_SYMBOL이어야 합니다. 실제: ${exception.errorCode}")
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
                    ufc.stock.getCompanyInfo(invalidSymbol)
                    null
                } catch (e: UfcException) {
                    e
                }

                assertThat(exception)
                    .withFailMessage("존재하지 않는 심볼 조회 시 UfcException이 발생해야 합니다")
                    .isNotNull()

                assertThat(exception!!.errorCode)
                    .withFailMessage(
                        "존재하지 않는 심볼은 DATA_NOT_FOUND 또는 EXTERNAL_API_ERROR를 발생시켜야 합니다. 실제: ${exception.errorCode}"
                    )
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
            @DisplayName("longName은 항상 non-null이다")
            fun shouldAlwaysHaveLongName() = runTest {
                // Given: 여러 심볼
                val symbols = listOf("AAPL", "MSFT", "GOOGL")

                // When & Then: 모든 심볼이 longName을 가짐
                symbols.forEach { symbol ->
                    val companyInfo = ufc.stock.getCompanyInfo(symbol)
                    assertThat(companyInfo.longName)
                        .withFailMessage("$symbol 의 longName이 있어야 합니다")
                        .isNotBlank()
                }
            }

            @Test
            @DisplayName("메타데이터에 심볼 정보가 포함된다")
            fun shouldIncludeMetadata() = runTest {
                // Given: AAPL 심볼

                // When
                val companyInfo = ufc.stock.getCompanyInfo("AAPL")

                // Then: 메타데이터 검증
                assertThat(companyInfo.metadata.symbol)
                    .withFailMessage("메타데이터에 심볼이 있어야 합니다")
                    .isEqualTo("AAPL")

                assertThat(companyInfo.metadata.fetchedAt)
                    .withFailMessage("조회 시각이 기록되어야 합니다")
                    .isGreaterThan(0)
            }
        }
    }

    @Nested
    @DisplayName("getCompanyInfo(List<String>) - 다중 심볼 조회")
    inner class GetCompanyInfoMultiple {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("여러 심볼 동시 조회 시 모든 데이터를 맵으로 반환한다")
            fun shouldReturnAllCompanyInfoAsMap() = runTest {
                // Given: 3개 심볼
                val symbols = listOf("AAPL", "MSFT", "GOOGL")

                // When
                val results = ufc.stock.getCompanyInfo(symbols)

                // Then: 대부분 반환 (일부 실패 허용)
                assertThat(results)
                    .withFailMessage("최소 2개 이상의 심볼이 반환되어야 합니다")
                    .hasSizeGreaterThanOrEqualTo(2)

                results.forEach { (symbol, companyInfo) ->
                    assertThat(companyInfo.symbol)
                        .withFailMessage("반환된 심볼이 요청한 심볼과 일치해야 합니다")
                        .isEqualTo(symbol)

                    assertThat(companyInfo.longName)
                        .withFailMessage("$symbol 의 회사명이 있어야 합니다")
                        .isNotBlank()
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
                val results = ufc.stock.getCompanyInfo(symbols)

                // Then: 대부분 반환 (일부 실패 허용)
                assertThat(results.size)
                    .withFailMessage("10개 중 최소 8개는 반환되어야 합니다. 실제: ${results.size}")
                    .isGreaterThanOrEqualTo(8)

                // 모든 반환된 데이터가 유효한지 확인
                results.values.forEach { companyInfo ->
                    assertThat(companyInfo.longName)
                        .withFailMessage("${companyInfo.symbol} 의 회사명이 유효해야 합니다")
                        .isNotBlank()
                }
            }

            @Test
            @DisplayName("2개 심볼 조회가 정상 동작한다")
            fun shouldHandleTwoSymbols() = runTest {
                // Given: 2개 심볼
                val symbols = listOf("AAPL", "MSFT")

                // When
                val results = ufc.stock.getCompanyInfo(symbols)

                // Then: 2개 모두 반환
                assertThat(results).hasSize(2)
                assertThat(results).containsKeys("AAPL", "MSFT")

                results.values.forEach { companyInfo ->
                    assertThat(companyInfo.longName).isNotBlank()
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
                val results = ufc.stock.getCompanyInfo(emptyList())

                // Then
                assertThat(results)
                    .withFailMessage("빈 리스트 조회 시 빈 맵을 반환해야 합니다")
                    .isEmpty()
            }

            @Test
            @DisplayName("중복된 심볼이 있어도 정상 처리한다")
            fun shouldHandleDuplicateSymbols() = runTest {
                // Given: 중복된 심볼
                val symbols = listOf("AAPL", "MSFT", "AAPL")

                // When
                val results = ufc.stock.getCompanyInfo(symbols)

                // Then: 중복 제거되어 2개만 반환 (또는 중복 허용)
                assertThat(results)
                    .withFailMessage("중복 심볼도 정상 처리되어야 합니다")
                    .hasSizeGreaterThanOrEqualTo(2)

                assertThat(results).containsKey("AAPL")
                assertThat(results).containsKey("MSFT")
            }
        }
    }
}
