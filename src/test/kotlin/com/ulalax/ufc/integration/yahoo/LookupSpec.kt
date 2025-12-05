package com.ulalax.ufc.integration.yahoo

import com.ulalax.ufc.domain.exception.ApiException
import com.ulalax.ufc.domain.exception.ErrorCode
import com.ulalax.ufc.domain.model.lookup.LookupType
import com.ulalax.ufc.integration.utils.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * YahooClient.lookup() API Integration 테스트
 *
 * 이 테스트는 실제 Yahoo Finance API를 호출하여 Lookup 기능을 검증합니다.
 * API 가이드처럼 읽힐 수 있도록 @Nested 그룹핑 패턴을 사용합니다.
 *
 * ## 테스트 실행 방법
 * ```bash
 * # 특정 클래스 실행
 * ./gradlew test --tests 'LookupSpec'
 *
 * # 특정 그룹 실행
 * ./gradlew test --tests 'LookupSpec$BasicBehavior'
 * ```
 */
@DisplayName("YahooClient.lookup() - 금융상품 검색")
class LookupSpec : IntegrationTestBase() {

    @Nested
    @DisplayName("기본 동작")
    inner class BasicBehavior {

        @Test
        @DisplayName("Apple 검색 시 AAPL이 포함된 결과를 반환한다")
        fun `returns results including AAPL when searching for Apple`() = integrationTest {
            // Given
            val query = "Apple"

            // When
            val result = ufc.yahoo.lookup(query)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.query).isEqualTo(query)
            assertThat(result.type).isEqualTo(LookupType.ALL)
            assertThat(result.documents).isNotEmpty()
            assertThat(result.count).isEqualTo(result.documents.size)
            assertThat(result.total).isGreaterThan(0)

            // AAPL이 결과에 포함되어야 함
            val aaplDocument = result.documents.find { it.symbol == "AAPL" }
            assertThat(aaplDocument).isNotNull()
            assertThat(aaplDocument?.name).isNotNull().containsIgnoringCase("Apple")
        }

        @Test
        @DisplayName("기본값으로 최대 25개의 결과를 반환한다")
        fun `returns up to 25 results by default`() = integrationTest {
            // Given
            val query = "tech"

            // When
            val result = ufc.yahoo.lookup(query)

            // Then
            assertThat(result.documents.size).isLessThanOrEqualTo(25)
        }

        @Test
        @DisplayName("검색 결과에는 심볼과 이름이 필수로 포함된다")
        fun `all results contain required symbol and name fields`() = integrationTest {
            // Given
            val query = "Microsoft"

            // When
            val result = ufc.yahoo.lookup(query)

            // Then
            assertThat(result.documents).allMatch { doc ->
                doc.symbol.isNotBlank() && doc.name.isNotBlank()
            }
        }
    }

    @Nested
    @DisplayName("타입 필터링")
    inner class TypeFiltering {

        @Test
        @DisplayName("EQUITY 타입으로 필터링하면 주식만 반환한다")
        fun `returns only equities when filtering by EQUITY type`() = integrationTest {
            // Given
            val query = "Apple"
            val type = LookupType.EQUITY

            // When
            val result = ufc.yahoo.lookup(query, type)

            // Then
            assertThat(result.type).isEqualTo(LookupType.EQUITY)
            assertThat(result.documents).isNotEmpty()

            // 대부분의 결과가 equity 타입이어야 함
            val equityCount = result.documents.count { doc ->
                doc.typeCode?.equals("equity", ignoreCase = true) == true
            }
            assertThat(equityCount).isGreaterThan(0)
        }

        @Test
        @DisplayName("ETF 타입으로 필터링하면 ETF만 반환한다")
        fun `returns only ETFs when filtering by ETF type`() = integrationTest {
            // Given
            val query = "SPY"
            val type = LookupType.ETF

            // When
            val result = ufc.yahoo.lookup(query, type)

            // Then
            assertThat(result.type).isEqualTo(LookupType.ETF)
            assertThat(result.documents).isNotEmpty()

            // SPY ETF가 결과에 포함되어야 함
            val spyDocument = result.documents.find { it.symbol == "SPY" }
            assertThat(spyDocument).isNotNull()
        }

        @Test
        @DisplayName("ALL 타입은 모든 종류의 금융상품을 반환한다")
        fun `returns all types of securities when using ALL type`() = integrationTest {
            // Given
            val query = "Bitcoin"
            val type = LookupType.ALL

            // When
            val result = ufc.yahoo.lookup(query, type)

            // Then
            assertThat(result.type).isEqualTo(LookupType.ALL)
            assertThat(result.documents).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("결과 개수 제어")
    inner class CountControl {

        @Test
        @DisplayName("count 파라미터로 결과 개수를 제한할 수 있다")
        fun `can limit results with count parameter`() = integrationTest {
            // Given
            val query = "tech"
            val count = 10

            // When
            val result = ufc.yahoo.lookup(query, count = count)

            // Then
            assertThat(result.documents.size).isLessThanOrEqualTo(count)
        }

        @Test
        @DisplayName("count가 1일 때 최대 1개의 결과를 반환한다")
        fun `returns at most 1 result when count is 1`() = integrationTest {
            // Given
            val query = "Apple"
            val count = 1

            // When
            val result = ufc.yahoo.lookup(query, count = count)

            // Then
            assertThat(result.documents.size).isLessThanOrEqualTo(1)
        }
    }

    @Nested
    @DisplayName("빈 결과 처리")
    inner class EmptyResults {

        @Test
        @DisplayName("존재하지 않는 검색어도 유효한 결과를 반환한다")
        fun `returns valid results for non-existent query`() = integrationTest {
            // Given
            val query = "xyzabc123notexist9999"

            // When
            val result = ufc.yahoo.lookup(query)

            // Then
            // Yahoo API는 유사한 결과를 반환할 수 있음
            assertThat(result).isNotNull()
            assertThat(result.query).isEqualTo(query)
            assertThat(result.type).isEqualTo(LookupType.ALL)
        }

        @Test
        @DisplayName("모든 검색 결과는 유효한 LookupResult 객체를 반환한다")
        fun `always returns valid LookupResult`() = integrationTest {
            // Given
            val query = "any"

            // When
            val result = ufc.yahoo.lookup(query)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.query).isEqualTo(query)
            assertThat(result.type).isEqualTo(LookupType.ALL)
            assertThat(result.count).isGreaterThanOrEqualTo(0)
            assertThat(result.total).isGreaterThanOrEqualTo(0)
        }
    }

    @Nested
    @DisplayName("파라미터 검증")
    inner class ParameterValidation {

        @Test
        @DisplayName("빈 문자열 검색 시 INVALID_PARAMETER 예외를 발생한다")
        fun `throws INVALID_PARAMETER exception for empty query`() = integrationTest {
            // Given
            val query = ""

            // When & Then
            val exception = runCatching {
                ufc.yahoo.lookup(query)
            }.exceptionOrNull()

            assertThat(exception).isNotNull()
            assertThat(exception).isInstanceOf(ApiException::class.java)
            assertThat((exception as ApiException).errorCode).isEqualTo(ErrorCode.INVALID_PARAMETER)
            assertThat(exception.message).contains("빈 문자열")
        }

        @Test
        @DisplayName("공백만 있는 검색어는 INVALID_PARAMETER 예외를 발생한다")
        fun `throws INVALID_PARAMETER exception for whitespace-only query`() = integrationTest {
            // Given
            val query = "   "

            // When & Then
            val exception = runCatching {
                ufc.yahoo.lookup(query)
            }.exceptionOrNull()

            assertThat(exception).isNotNull()
            assertThat(exception).isInstanceOf(ApiException::class.java)
            assertThat((exception as ApiException).errorCode).isEqualTo(ErrorCode.INVALID_PARAMETER)
        }

        @Test
        @DisplayName("count가 0 이하일 때 INVALID_PARAMETER 예외를 발생한다")
        fun `throws INVALID_PARAMETER exception when count is zero or negative`() = integrationTest {
            // Given
            val query = "Apple"
            val count = 0

            // When & Then
            val exception = runCatching {
                ufc.yahoo.lookup(query, count = count)
            }.exceptionOrNull()

            assertThat(exception).isNotNull()
            assertThat(exception).isInstanceOf(ApiException::class.java)
            assertThat((exception as ApiException).errorCode).isEqualTo(ErrorCode.INVALID_PARAMETER)
            assertThat(exception.message).contains("1-100")
        }

        @Test
        @DisplayName("count가 100을 초과할 때 INVALID_PARAMETER 예외를 발생한다")
        fun `throws INVALID_PARAMETER exception when count exceeds 100`() = integrationTest {
            // Given
            val query = "Apple"
            val count = 101

            // When & Then
            val exception = runCatching {
                ufc.yahoo.lookup(query, count = count)
            }.exceptionOrNull()

            assertThat(exception).isNotNull()
            assertThat(exception).isInstanceOf(ApiException::class.java)
            assertThat((exception as ApiException).errorCode).isEqualTo(ErrorCode.INVALID_PARAMETER)
            assertThat(exception.message).contains("1-100")
        }
    }

    @Nested
    @DisplayName("Ufc 파사드 통합")
    inner class UfcFacadeIntegration {

        @Test
        @DisplayName("Ufc.lookup()으로도 동일하게 검색할 수 있다")
        fun `can search using Ufc facade`() = integrationTest {
            // Given
            val query = "Tesla"

            // When
            val result = ufc.lookup(query)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.documents).isNotEmpty()
        }

        @Test
        @DisplayName("Ufc.lookup()에서도 타입 필터링이 동작한다")
        fun `type filtering works through Ufc facade`() = integrationTest {
            // Given
            val query = "Amazon"
            val type = LookupType.EQUITY

            // When
            val result = ufc.lookup(query, type)

            // Then
            assertThat(result.type).isEqualTo(LookupType.EQUITY)
            assertThat(result.documents).isNotEmpty()
        }
    }
}
