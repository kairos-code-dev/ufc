package com.ulalax.ufc.integration.yahoo

import com.ulalax.ufc.domain.exception.ApiException
import com.ulalax.ufc.domain.model.screener.*
import com.ulalax.ufc.integration.utils.IntegrationTestBase
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Yahoo.screener() API Integration 테스트
 *
 * 이 테스트는 실제 Yahoo Finance Screener API를 호출하여 검증합니다.
 * API 가이드처럼 읽힐 수 있도록 @Nested 그룹핑 패턴을 사용합니다.
 *
 * ## 거래일/비거래일 동작
 * - **거래일 (장중)**: DAY_GAINERS, MOST_ACTIVES 등의 순위가 실시간으로 변동
 * - **거래일 (장외)**: 종가 기준 순위 고정
 * - **휴장일**: 전일 데이터 기준, 일부 스크리너는 빈 결과 반환 가능
 *
 * ## 테스트 실행 방법
 * ```bash
 * # 특정 클래스 실행
 * ./gradlew test --tests 'ScreenerSpec'
 *
 * # 특정 그룹 실행
 * ./gradlew test --tests 'ScreenerSpec$PredefinedScreeners'
 * ```
 */
@DisplayName("[I] Yahoo.screener() - 종목 스크리너")
class ScreenerSpec : IntegrationTestBase() {
    @Nested
    @DisplayName("Predefined Screeners")
    inner class PredefinedScreeners {
        @Test
        @DisplayName("DAY_GAINERS 조회 - 당일 상승 종목")
        fun `returns day gainers`() =
            integrationTest {
                // When
                val result = ufc.yahoo.screener(PredefinedScreener.DAY_GAINERS, count = 10)

                // Then
                assertThat(result).isNotNull()
                assertThat(result.count).isGreaterThan(0)
                assertThat(result.quotes).hasSizeLessThanOrEqualTo(10)

                // 첫 번째 종목 검증
                val firstQuote = result.quotes.first()
                assertThat(firstQuote.symbol).isNotBlank()
                assertThat(firstQuote.regularMarketChangePercent).isNotNull()
            }

        @Test
        @DisplayName("MOST_ACTIVES 조회 - 가장 활발한 종목")
        fun `returns most actives`() =
            integrationTest {
                // When
                val result = ufc.yahoo.screener(PredefinedScreener.MOST_ACTIVES, count = 15)

                // Then
                assertThat(result).isNotNull()
                assertThat(result.count).isGreaterThan(0)
                assertThat(result.quotes).hasSizeLessThanOrEqualTo(15)

                // 모든 종목이 symbol을 가지고 있는지 확인
                result.quotes.forEach { quote ->
                    assertThat(quote.symbol).isNotBlank()
                }
            }

        @Test
        @DisplayName("String ID로 Predefined Screener 조회")
        fun `returns screener by string id`() =
            integrationTest {
                // When
                val result = ufc.yahoo.screener("day_losers", count = 5)

                // Then
                assertThat(result).isNotNull()
                assertThat(result.count).isGreaterThan(0)
                assertThat(result.quotes).hasSizeLessThanOrEqualTo(5)
            }

        @Test
        @DisplayName("커스텀 정렬 적용")
        fun `supports custom sorting`() =
            integrationTest {
                // When - 시가총액 기준 오름차순 정렬
                val result =
                    ufc.yahoo.screener(
                        predefined = PredefinedScreener.MOST_ACTIVES,
                        count = 10,
                        sortField = ScreenerSortField.MARKET_CAP,
                        sortAsc = true,
                    )

                // Then
                assertThat(result).isNotNull()
                assertThat(result.count).isGreaterThan(0)
            }
    }

    @Nested
    @DisplayName("Custom Queries")
    inner class CustomQueries {
        @Test
        @DisplayName("단순 조건 - 시가총액 > 10억")
        fun `filters by market cap greater than 1 billion`() =
            integrationTest {
                // Given - 시가총액 10억 달러 이상
                val query = EquityQuery.gt(EquityField.INTRADAY_MARKET_CAP, 1_000_000_000)

                // When
                val result =
                    ufc.yahoo.screener(
                        query = query,
                        sortField = ScreenerSortField.MARKET_CAP,
                        sortAsc = false,
                        size = 10,
                    )

                // Then
                assertThat(result).isNotNull()
                assertThat(result.count).isGreaterThan(0)
                assertThat(result.quotes).hasSizeLessThanOrEqualTo(10)

                // 시가총액 검증
                result.quotes.forEach { quote ->
                    assertThat(quote.marketCap).isNotNull().isGreaterThan(1_000_000_000L)
                }
            }

        @Test
        @DisplayName("복합 조건 - Technology 섹터 AND 시가총액 > 1조")
        fun `filters by sector and market cap`() =
            integrationTest {
                // Given - Technology 섹터이면서 시가총액 1조 달러 이상
                val query =
                    EquityQuery.and(
                        EquityQuery.eq(EquityField.SECTOR, "Technology"),
                        EquityQuery.gt(EquityField.INTRADAY_MARKET_CAP, 1_000_000_000_000),
                    )

                // When
                val result =
                    ufc.yahoo.screener(
                        query = query,
                        sortField = ScreenerSortField.MARKET_CAP,
                        sortAsc = false,
                        size = 5,
                    )

                // Then
                assertThat(result).isNotNull()
                assertThat(result.quotes).isNotEmpty()

                // Note: sector 필드는 필터 조건에 포함되어도 응답에서 null일 수 있음
                // 이는 Yahoo Finance API의 특성으로, 필터는 서버에서 적용되지만
                // 응답 필드는 별도로 요청해야 함
                val quotesWithSector = result.quotes.filter { it.sector != null }
                quotesWithSector.forEach { quote ->
                    assertThat(quote.sector).isEqualTo("Technology")
                }
            }

        @Test
        @DisplayName("범위 조건 - PE Ratio 0-20 사이")
        fun `filters by pe ratio between 0 and 20`() =
            integrationTest {
                // Given - PER 0-20 사이
                val query =
                    EquityQuery.and(
                        EquityQuery.between(EquityField.PE_RATIO, 0, 20),
                        EquityQuery.gt(EquityField.INTRADAY_MARKET_CAP, 10_000_000_000), // 시총 100억 이상
                    )

                // When
                val result =
                    ufc.yahoo.screener(
                        query = query,
                        sortField = ScreenerSortField.PE_RATIO,
                        sortAsc = true,
                        size = 10,
                    )

                // Then
                assertThat(result).isNotNull()
            }

        @Test
        @DisplayName("거래소 필터 - 여러 거래소 포함")
        fun `filters by multiple exchanges`() =
            integrationTest {
                // Given - NYSE 또는 NASDAQ
                val query =
                    EquityQuery.and(
                        EquityQuery.isIn(EquityField.EXCHANGE, "NYQ", "NMS"),
                        EquityQuery.gt(EquityField.INTRADAY_MARKET_CAP, 1_000_000_000),
                    )

                // When
                val result =
                    ufc.yahoo.screener(
                        query = query,
                        sortField = ScreenerSortField.TICKER,
                        size = 20,
                    )

                // Then
                assertThat(result).isNotNull()
                assertThat(result.count).isGreaterThan(0)

                // 거래소 검증
                result.quotes.forEach { quote ->
                    assertThat(quote.exchange).isNotNull().isIn("NYQ", "NMS")
                }
            }
    }

    @Nested
    @DisplayName("Pagination and Sorting")
    inner class PaginationAndSorting {
        @Test
        @DisplayName("페이지네이션 - offset과 size 지원")
        fun `supports pagination with offset and size`() =
            integrationTest {
                // Given
                val query = EquityQuery.gt(EquityField.INTRADAY_MARKET_CAP, 10_000_000_000)

                // When - 첫 번째 페이지
                val page1 =
                    ufc.yahoo.screener(
                        query = query,
                        sortField = ScreenerSortField.MARKET_CAP,
                        sortAsc = false,
                        size = 5,
                        offset = 0,
                    )

                // When - 두 번째 페이지
                val page2 =
                    ufc.yahoo.screener(
                        query = query,
                        sortField = ScreenerSortField.MARKET_CAP,
                        sortAsc = false,
                        size = 5,
                        offset = 5,
                    )

                // Then
                assertThat(page1.count).isGreaterThan(0)
                assertThat(page2.count).isGreaterThan(0)
                assertThat(page1.start).isEqualTo(0)
                assertThat(page2.start).isEqualTo(5)

                // 다른 종목이어야 함
                val page1Symbols = page1.quotes.map { it.symbol }.toSet()
                val page2Symbols = page2.quotes.map { it.symbol }.toSet()
                assertThat(page1Symbols).isNotEqualTo(page2Symbols)
            }

        @Test
        @DisplayName("정렬 - 오름차순/내림차순")
        fun `supports ascending and descending sort`() =
            integrationTest {
                // Given
                val query = EquityQuery.gt(EquityField.INTRADAY_MARKET_CAP, 100_000_000_000)

                // When - 내림차순
                val descResult =
                    ufc.yahoo.screener(
                        query = query,
                        sortField = ScreenerSortField.MARKET_CAP,
                        sortAsc = false,
                        size = 5,
                    )

                // When - 오름차순
                val ascResult =
                    ufc.yahoo.screener(
                        query = query,
                        sortField = ScreenerSortField.MARKET_CAP,
                        sortAsc = true,
                        size = 5,
                    )

                // Then
                assertThat(descResult.count).isGreaterThan(0)
                assertThat(ascResult.count).isGreaterThan(0)
            }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {
        @Test
        @DisplayName("잘못된 size 파라미터 - 범위 초과")
        fun `throws exception for invalid size`() =
            integrationTest {
                // Given
                val query = EquityQuery.gt(EquityField.INTRADAY_MARKET_CAP, 1_000_000_000)

                // When/Then - size > 250
                assertThatThrownBy {
                    runBlocking {
                        ufc.yahoo.screener(query, size = 300)
                    }
                }.isInstanceOf(IllegalArgumentException::class.java)
                    .hasMessageContaining("size must be between 1 and 250")
            }

        @Test
        @DisplayName("잘못된 offset 파라미터 - 음수")
        fun `throws exception for negative offset`() =
            integrationTest {
                // Given
                val query = EquityQuery.gt(EquityField.INTRADAY_MARKET_CAP, 1_000_000_000)

                // When/Then
                assertThatThrownBy {
                    runBlocking {
                        ufc.yahoo.screener(query, offset = -1)
                    }
                }.isInstanceOf(IllegalArgumentException::class.java)
                    .hasMessageContaining("offset must be >= 0")
            }

        @Test
        @DisplayName("잘못된 Predefined ID")
        fun `throws exception for invalid predefined id`() =
            integrationTest {
                // When/Then
                assertThatThrownBy {
                    runBlocking {
                        ufc.yahoo.screener("invalid_screener_id_12345")
                    }
                }.isInstanceOf(ApiException::class.java)
            }
    }

    @Nested
    @DisplayName("Response Structure")
    inner class ResponseStructure {
        @Test
        @DisplayName("ScreenerResult 구조 검증")
        fun `validates screener result structure`() =
            integrationTest {
                // When
                val result = ufc.yahoo.screener(PredefinedScreener.MOST_ACTIVES, count = 5)

                // Then - 메타데이터
                assertThat(result.count).isGreaterThan(0)
                assertThat(result.total).isGreaterThanOrEqualTo(result.count)
                assertThat(result.start).isGreaterThanOrEqualTo(0)

                // Then - 종목 정보
                val quote = result.quotes.first()
                assertThat(quote.symbol).isNotBlank()
            }

        @Test
        @DisplayName("additionalFields에 동적 필드 저장")
        fun `stores dynamic fields in additional fields`() =
            integrationTest {
                // When
                val result =
                    ufc.yahoo.screener(
                        PredefinedScreener.DAY_GAINERS,
                        count = 5,
                    )

                // Then
                assertThat(result.quotes).isNotEmpty()
                val quote = result.quotes.first()
                assertThat(quote.additionalFields).isNotNull()
            }
    }

    @Nested
    @DisplayName("Query Validation")
    inner class QueryValidation {
        @Test
        @DisplayName("AND 연산자는 최소 2개 operands 필요")
        fun `and operator requires at least 2 operands`() {
            // When/Then
            assertThatThrownBy {
                EquityQuery(
                    ScreenerOperator.AND,
                    listOf(
                        EquityQuery.gt(EquityField.INTRADAY_MARKET_CAP, 1_000_000_000),
                    ),
                ).validate()
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("at least 2 operands")
        }

        @Test
        @DisplayName("GT 연산자는 숫자 값 필요")
        fun `gt operator requires number value`() {
            // When/Then
            assertThatThrownBy {
                EquityQuery(
                    ScreenerOperator.GT,
                    listOf(
                        EquityField.INTRADAY_MARKET_CAP.apiValue,
                        "not a number",
                    ),
                ).validate()
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("must be Number")
        }

        @Test
        @DisplayName("BTWN 연산자는 정확히 3개 operands 필요")
        fun `btwn operator requires exactly 3 operands`() {
            // When/Then
            assertThatThrownBy {
                EquityQuery(
                    ScreenerOperator.BTWN,
                    listOf(
                        EquityField.PE_RATIO.apiValue,
                        0,
                    ),
                ).validate()
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("exactly 3 operands")
        }
    }
}
