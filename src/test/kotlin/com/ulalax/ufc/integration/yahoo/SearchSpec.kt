package com.ulalax.ufc.integration.yahoo

import com.ulalax.ufc.integration.utils.IntegrationTestBase
import com.ulalax.ufc.integration.utils.RecordingConfig
import com.ulalax.ufc.integration.utils.ResponseRecorder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * YahooClient.search() API Integration 테스트
 *
 * 이 테스트는 실제 Yahoo Finance API를 호출하여 Search 기능을 검증합니다.
 * API 가이드처럼 읽힐 수 있도록 @Nested 그룹핑 패턴을 사용합니다.
 *
 * ## 테스트 실행 방법
 * ```bash
 * # 특정 클래스 실행
 * ./gradlew test --tests 'SearchSpec'
 *
 * # 특정 그룹 실행
 * ./gradlew test --tests 'SearchSpec$BasicBehavior'
 * ```
 */
@DisplayName("YahooClient.search() - 종목 및 뉴스 검색")
class SearchSpec : IntegrationTestBase() {

    @Nested
    @DisplayName("기본 동작")
    inner class BasicBehavior {

        @Test
        @DisplayName("Apple 검색 시 AAPL 종목을 찾을 수 있다")
        fun `returns AAPL when searching for Apple`() = integrationTest {
            // Given
            val query = "Apple"

            // When
            val result = ufc.yahoo.search(query)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.query).isEqualTo(query)
            assertThat(result.quotes).isNotEmpty()
            assertThat(result.quotes).anyMatch { it.symbol == "AAPL" }

            // Record
            if (RecordingConfig.isRecordingEnabled) {
                ResponseRecorder.record(
                    result,
                    RecordingConfig.Paths.Yahoo.SEARCH,
                    "apple_search"
                )
            }
        }

        @Test
        @DisplayName("정확한 심볼 AAPL 검색 시 첫 번째 결과로 반환된다")
        fun `returns AAPL as first result when searching exact symbol`() = integrationTest {
            // Given
            val query = "AAPL"

            // When
            val result = ufc.yahoo.search(query)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.quotes).isNotEmpty()
            assertThat(result.quotes.first().symbol).isEqualTo("AAPL")
            assertThat(result.quotes.first().score).isGreaterThan(0.0)

            // Record
            if (RecordingConfig.isRecordingEnabled) {
                ResponseRecorder.record(
                    result,
                    RecordingConfig.Paths.Yahoo.SEARCH,
                    "aapl_exact_symbol"
                )
            }
        }
    }

    @Nested
    @DisplayName("검색 결과 데이터 스펙")
    inner class ResponseSpec {

        @Test
        @DisplayName("종목 검색 결과에 필수 필드가 포함된다")
        fun `quote results contain required fields`() = integrationTest {
            // Given
            val query = "AAPL"

            // When
            val result = ufc.yahoo.search(query)

            // Then
            assertThat(result.quotes).isNotEmpty()
            val firstQuote = result.quotes.first()

            assertThat(firstQuote.symbol).isNotBlank()
            assertThat(firstQuote.quoteType).isNotBlank()
            assertThat(firstQuote.score).isGreaterThanOrEqualTo(0.0)
        }

        @Test
        @DisplayName("뉴스 검색 결과에 필수 필드가 포함된다")
        fun `news results contain required fields`() = integrationTest {
            // Given
            val query = "Apple"

            // When
            val result = ufc.yahoo.search(query, quotesCount = 1, newsCount = 10)

            // Then
            if (result.news.isNotEmpty()) {
                val firstNews = result.news.first()

                assertThat(firstNews.uuid).isNotBlank()
                assertThat(firstNews.title).isNotBlank()
                assertThat(firstNews.link).isNotBlank()
                assertThat(firstNews.publishTime).isGreaterThan(0L)
            }
        }
    }

    @Nested
    @DisplayName("검색 옵션")
    inner class SearchOptions {

        @Test
        @DisplayName("quotesCount 파라미터로 종목 결과 개수를 제한할 수 있다")
        fun `can limit quotes count`() = integrationTest {
            // Given
            val query = "tech"
            val quotesCount = 3

            // When
            val result = ufc.yahoo.search(query, quotesCount = quotesCount, newsCount = 0)

            // Then
            assertThat(result.quotes.size).isLessThanOrEqualTo(quotesCount)
        }

        @Test
        @DisplayName("newsCount 파라미터로 뉴스 결과 개수를 제한할 수 있다")
        fun `can limit news count`() = integrationTest {
            // Given
            val query = "Apple"
            val newsCount = 5

            // When
            val result = ufc.yahoo.search(query, quotesCount = 0, newsCount = newsCount)

            // Then
            assertThat(result.news.size).isLessThanOrEqualTo(newsCount)
        }

        @Test
        @DisplayName("enableFuzzyQuery로 퍼지 검색을 활성화할 수 있다")
        fun `can enable fuzzy query for typo correction`() = integrationTest {
            // Given
            val query = "Appel" // 오타

            // When
            val result = ufc.yahoo.search(query, enableFuzzyQuery = true)

            // Then
            // 퍼지 검색이 활성화되면 오타에도 Apple 관련 결과가 나올 수 있음
            assertThat(result).isNotNull()

            // Record
            if (RecordingConfig.isRecordingEnabled) {
                ResponseRecorder.record(
                    result,
                    RecordingConfig.Paths.Yahoo.SEARCH,
                    "fuzzy_query_typo"
                )
            }
        }
    }

    @Nested
    @DisplayName("엣지 케이스")
    inner class EdgeCases {

        @Test
        @DisplayName("존재하지 않는 검색어는 빈 결과를 반환한다")
        fun `returns empty results for non-existent query`() = integrationTest {
            // Given
            val query = "XYZNONEXISTENT999"

            // When
            val result = ufc.yahoo.search(query)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.quotes).isEmpty()
        }

        @Test
        @DisplayName("0개 결과 요청 시 빈 리스트를 반환한다")
        fun `returns empty lists when count is zero`() = integrationTest {
            // Given
            val query = "Apple"

            // When
            val result = ufc.yahoo.search(query, quotesCount = 0, newsCount = 0)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.quotes).isEmpty()
            assertThat(result.news).isEmpty()
        }
    }

    @Nested
    @DisplayName("실제 사용 사례")
    inner class RealWorldUseCases {

        @Test
        @DisplayName("다국가 종목 검색 - Samsung")
        fun `can search multi-country stocks like Samsung`() = integrationTest {
            // Given
            val query = "Samsung"

            // When
            val result = ufc.yahoo.search(query, quotesCount = 10)

            // Then
            assertThat(result.quotes).isNotEmpty()
            // 한국 삼성전자 (005930.KS) 또는 미국 상장 Samsung 종목이 포함될 수 있음
            assertThat(result.quotes).anyMatch {
                it.symbol.contains("Samsung", ignoreCase = true) ||
                it.longName?.contains("Samsung", ignoreCase = true) == true
            }

            // Record
            if (RecordingConfig.isRecordingEnabled) {
                ResponseRecorder.record(
                    result,
                    RecordingConfig.Paths.Yahoo.SEARCH,
                    "samsung_multi_country"
                )
            }
        }

        @Test
        @DisplayName("ETF 검색 - SPY")
        fun `can search ETFs like SPY`() = integrationTest {
            // Given
            val query = "SPY"

            // When
            val result = ufc.yahoo.search(query)

            // Then
            assertThat(result.quotes).isNotEmpty()
            assertThat(result.quotes).anyMatch {
                it.symbol == "SPY" && it.quoteType == "ETF"
            }

            // Record
            if (RecordingConfig.isRecordingEnabled) {
                ResponseRecorder.record(
                    result,
                    RecordingConfig.Paths.Yahoo.SEARCH,
                    "spy_etf"
                )
            }
        }
    }
}
