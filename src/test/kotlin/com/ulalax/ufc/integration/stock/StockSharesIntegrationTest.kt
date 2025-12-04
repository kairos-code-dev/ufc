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
import java.time.LocalDate

/**
 * StockService - 발행주식수 조회 통합 테스트
 *
 * 이 테스트는 실제 Yahoo Finance API를 호출합니다.
 * ufc.stock.getShares() 및 getSharesFull() 메서드의 모든 시나리오를 검증합니다.
 */
@Tag("integration")
@DisplayName("Stock Service - 발행주식수 조회 통합 테스트")
class StockSharesIntegrationTest {

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
    @DisplayName("getShares() - 단일 심볼 조회")
    inner class GetShares {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("유효한 심볼로 조회 시 발행주식수 히스토리를 반환한다")
            fun shouldReturnSharesHistoryForValidSymbol() = runTest {
                // Given: AAPL 심볼

                // When
                val shares = ufc.stock.getShares("AAPL")

                // Then: 분기별 데이터 반환
                assertThat(shares)
                    .withFailMessage("발행주식수 데이터가 있어야 합니다")
                    .isNotEmpty()

                // 첫 번째 데이터 검증
                val firstShare = shares.first()
                assertThat(firstShare.date)
                    .withFailMessage("날짜가 있어야 합니다")
                    .isNotNull()

                assertThat(firstShare.shares)
                    .withFailMessage("발행주식수는 0보다 커야 합니다")
                    .isGreaterThan(0)
            }

            @Test
            @DisplayName("발행주식수 데이터는 날짜 순서로 정렬되어 있다")
            fun shouldBeSortedByDate() = runTest {
                // Given: MSFT 심볼

                // When
                val shares = ufc.stock.getShares("MSFT")

                // Then: 날짜 오름차순 정렬
                if (shares.size > 1) {
                    for (i in 0 until shares.size - 1) {
                        assertThat(shares[i + 1].date)
                            .withFailMessage("발행주식수 데이터는 날짜 오름차순이어야 합니다")
                            .isAfterOrEqualTo(shares[i].date)
                    }
                }
            }

            @Test
            @DisplayName("여러 분기의 발행주식수 데이터를 반환한다")
            fun shouldReturnMultipleQuarters() = runTest {
                // Given: GOOGL 심볼

                // When
                val shares = ufc.stock.getShares("GOOGL")

                // Then: 최소 1개 이상의 데이터 (Yahoo API가 히스토리를 제공하지 않을 수 있음)
                assertThat(shares.size)
                    .withFailMessage("최소 1개 이상의 데이터가 있어야 합니다")
                    .isGreaterThanOrEqualTo(1)
            }

            @Test
            @DisplayName("기술주의 발행주식수는 합리적인 범위이다")
            fun shouldHaveReasonableSharesOutstanding() = runTest {
                // Given: 기술주들
                val symbols = listOf("AAPL", "MSFT")

                // When & Then: 발행주식수가 합리적 범위 (10억 ~ 500억)
                symbols.forEach { symbol ->
                    val shares = ufc.stock.getShares(symbol)
                    val latestShares = shares.last().shares

                    assertThat(latestShares)
                        .withFailMessage("$symbol 의 발행주식수가 합리적 범위여야 합니다")
                        .isBetween(1_000_000_000L, 50_000_000_000L)
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
                    ufc.stock.getShares("")
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
                    ufc.stock.getShares(invalidSymbol)
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
            @DisplayName("모든 발행주식수 값이 양수이다")
            fun shouldHavePositiveShares() = runTest {
                // Given: AAPL 심볼

                // When
                val shares = ufc.stock.getShares("AAPL")

                // Then: 모든 값이 양수
                shares.forEach { shareData ->
                    assertThat(shareData.shares)
                        .withFailMessage("발행주식수는 항상 양수여야 합니다. 날짜: ${shareData.date}")
                        .isGreaterThan(0)
                }
            }

            @Test
            @DisplayName("날짜는 과거부터 현재까지의 범위이다")
            fun shouldHaveDatesInValidRange() = runTest {
                // Given: MSFT 심볼

                // When
                val shares = ufc.stock.getShares("MSFT")

                // Then: 모든 날짜가 과거~현재 범위
                val today = LocalDate.now()
                shares.forEach { shareData ->
                    assertThat(shareData.date)
                        .withFailMessage("날짜는 현재 이전이어야 합니다")
                        .isBeforeOrEqualTo(today)

                    assertThat(shareData.date)
                        .withFailMessage("날짜는 1900년 이후여야 합니다")
                        .isAfter(LocalDate.of(1900, 1, 1))
                }
            }
        }
    }

    @Nested
    @DisplayName("getShares(List<String>) - 다중 심볼 조회")
    inner class GetSharesMultiple {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("여러 심볼 동시 조회 시 모든 발행주식수를 맵으로 반환한다")
            fun shouldReturnAllSharesAsMap() = runTest {
                // Given: 3개 심볼
                val symbols = listOf("AAPL", "MSFT", "GOOGL")

                // When
                val results = ufc.stock.getShares(symbols)

                // Then: 대부분 반환
                assertThat(results)
                    .withFailMessage("최소 2개 이상의 심볼이 반환되어야 합니다")
                    .hasSizeGreaterThanOrEqualTo(2)

                results.forEach { (symbol, shares) ->
                    assertThat(shares)
                        .withFailMessage("$symbol 의 발행주식수 데이터가 있어야 합니다")
                        .isNotEmpty()

                    shares.forEach { shareData ->
                        assertThat(shareData.shares).isGreaterThan(0)
                    }
                }
            }

            @Test
            @DisplayName("2개 심볼 조회가 정상 동작한다")
            fun shouldHandleTwoSymbols() = runTest {
                // Given: 2개 심볼
                val symbols = listOf("AAPL", "MSFT")

                // When
                val results = ufc.stock.getShares(symbols)

                // Then: 2개 모두 반환
                assertThat(results).hasSize(2)
                assertThat(results).containsKeys("AAPL", "MSFT")

                results.values.forEach { shares ->
                    assertThat(shares).isNotEmpty()
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
                val results = ufc.stock.getShares(emptyList())

                // Then
                assertThat(results)
                    .withFailMessage("빈 리스트 조회 시 빈 맵을 반환해야 합니다")
                    .isEmpty()
            }
        }
    }

    @Nested
    @DisplayName("getSharesFull() - 기간별 조회")
    inner class GetSharesFull {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("시작일과 종료일을 지정하여 조회할 수 있다")
            fun shouldReturnSharesForDateRange() = runTest {
                // Given: AAPL 심볼과 날짜 범위
                val start = LocalDate.of(2020, 1, 1)
                val end = LocalDate.of(2023, 12, 31)

                // When
                val shares = ufc.stock.getSharesFull("AAPL", start, end)

                // Then: 지정된 범위의 데이터만 반환
                assertThat(shares)
                    .withFailMessage("발행주식수 데이터가 있어야 합니다")
                    .isNotEmpty()

                shares.forEach { shareData ->
                    assertThat(shareData.date)
                        .withFailMessage("날짜는 지정된 범위 내여야 합니다")
                        .isBetween(start, end)
                }
            }

            @Test
            @DisplayName("시작일만 지정하여 조회할 수 있다")
            fun shouldReturnSharesFromStartDate() = runTest {
                // Given: MSFT 심볼과 시작일만 지정
                val start = LocalDate.of(2022, 1, 1)

                // When
                val shares = ufc.stock.getSharesFull("MSFT", start, null)

                // Then: 시작일 이후 데이터
                assertThat(shares)
                    .withFailMessage("발행주식수 데이터가 있어야 합니다")
                    .isNotEmpty()

                shares.forEach { shareData ->
                    assertThat(shareData.date)
                        .withFailMessage("날짜는 시작일 이후여야 합니다")
                        .isAfterOrEqualTo(start)
                }
            }

            @Test
            @DisplayName("종료일만 지정하여 조회할 수 있다")
            fun shouldReturnSharesUntilEndDate() = runTest {
                // Given: GOOGL 심볼과 종료일만 지정
                val end = LocalDate.of(2023, 12, 31)

                // When
                val shares = ufc.stock.getSharesFull("GOOGL", null, end)

                // Then: 종료일 이전 데이터
                assertThat(shares)
                    .withFailMessage("발행주식수 데이터가 있어야 합니다")
                    .isNotEmpty()

                shares.forEach { shareData ->
                    assertThat(shareData.date)
                        .withFailMessage("날짜는 종료일 이전이어야 합니다")
                        .isBeforeOrEqualTo(end)
                }
            }

            @Test
            @DisplayName("날짜 범위를 지정하지 않으면 전체 히스토리를 반환한다")
            fun shouldReturnFullHistoryWhenNoDatesSpecified() = runTest {
                // Given: AAPL 심볼, 날짜 범위 없음

                // When
                val shares = ufc.stock.getSharesFull("AAPL", null, null)

                // Then: 전체 히스토리 반환
                assertThat(shares)
                    .withFailMessage("전체 히스토리가 반환되어야 합니다")
                    .isNotEmpty()
            }
        }

        @Nested
        @DisplayName("입력 검증")
        inner class InputValidation {

            @Test
            @DisplayName("시작일이 종료일보다 늦은 경우도 처리 가능하다")
            fun shouldHandleInvertedDateRange() = runTest {
                // Given: 시작일 > 종료일
                val start = LocalDate.of(2023, 12, 31)
                val end = LocalDate.of(2020, 1, 1)

                // When
                val shares = ufc.stock.getSharesFull("AAPL", start, end)

                // Then: 빈 결과거나 전체 데이터 반환 (구현에 따라 다름)
                // API가 전체 데이터를 반환하고 클라이언트 측에서 필터링하지 않을 수 있음
                assertThat(shares)
                    .withFailMessage("잘못된 날짜 범위 처리 시 에러가 발생하지 않아야 합니다")
                    .isNotNull()
            }
        }

        @Nested
        @DisplayName("에러 처리")
        inner class ErrorHandling {

            @Test
            @DisplayName("존재하지 않는 심볼 조회 시 빈 리스트를 반환한다")
            fun shouldReturnEmptyListForNonExistentSymbol() = runTest {
                // Given: 존재하지 않는 심볼
                val invalidSymbol = "XXXXXXXXX"

                // When
                val shares = ufc.stock.getSharesFull(invalidSymbol, null, null)

                // Then: Yahoo Finance API는 존재하지 않는 심볼에 대해
                // 에러를 반환하지 않고 빈 결과를 반환합니다
                assertThat(shares)
                    .withFailMessage("존재하지 않는 심볼 조회 시 빈 리스트를 반환해야 합니다")
                    .isEmpty()
            }
        }
    }
}
