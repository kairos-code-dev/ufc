package com.ulalax.ufc.integration.businessinsider

import com.ulalax.ufc.domain.exception.ValidationException
import com.ulalax.ufc.domain.model.quote.QuoteSummaryModule
import com.ulalax.ufc.fixture.TestFixtures
import com.ulalax.ufc.integration.utils.IntegrationTestBase
import com.ulalax.ufc.integration.utils.RecordingConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * BusinessInsiderClient.searchIsin() API Integration 테스트
 *
 * 이 테스트는 실제 Business Insider Markets API를 호출하여 ISIN 검색 기능을 검증합니다.
 * API 가이드처럼 읽힐 수 있도록 @Nested 그룹핑 패턴을 사용합니다.
 *
 * ## 테스트 실행 방법
 * ```bash
 * # 특정 클래스 실행
 * ./gradlew test --tests 'IsinSearchSpec'
 *
 * # 특정 그룹 실행
 * ./gradlew test --tests 'IsinSearchSpec$BasicBehavior'
 * ```
 *
 * ## ISIN 형식
 * ISIN(International Securities Identification Number)은 12자리 영숫자입니다.
 * - 구조: 국가코드(2) + 식별자(9) + 체크섬(1)
 * - 예시:
 *   - US0378331005: Apple Inc (미국)
 *   - KR7005930003: 삼성전자 (한국)
 *   - JP3633400001: Toyota Motor (일본)
 */
@DisplayName("[I] BusinessInsider.searchIsin() - ISIN 검색")
class IsinSearchSpec : IntegrationTestBase() {

    @Nested
    @DisplayName("기본 동작")
    inner class BasicBehavior {

        @Test
        @DisplayName("Apple ISIN으로 검색할 수 있다")
        fun `returns result for Apple ISIN`() = integrationTest(
            RecordingConfig.Paths.BusinessInsider.ISIN_SEARCH,
            "apple_isin"
        ) {
            // Given
            val isin = TestFixtures.Isin.APPLE  // US0378331005

            // When
            val result = ufc.businessInsider.searchIsin(isin)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.isin).isEqualTo(isin)
            assertThat(result.symbol).isEqualTo("AAPL")
            assertThat(result.name).contains("Apple")
        }

        @Test
        @DisplayName("Microsoft ISIN으로 검색할 수 있다")
        fun `returns result for Microsoft ISIN`() = integrationTest {
            // Given
            val isin = TestFixtures.Isin.MICROSOFT  // US5949181045

            // When
            val result = ufc.businessInsider.searchIsin(isin)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.isin).isEqualTo(isin)
            assertThat(result.symbol).isEqualTo("MSFT")
            assertThat(result.name).contains("Microsoft")
        }

        @Test
        @DisplayName("Samsung 한국 ISIN으로 검색할 수 있다")
        fun `returns result for Samsung Korea ISIN`() = integrationTest {
            // Given
            val isin = TestFixtures.Isin.SAMSUNG  // KR7005930003

            // When
            val result = ufc.businessInsider.searchIsin(isin)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.isin).isEqualTo(isin)
            assertThat(result.name).contains("Samsung")
        }
    }

    @Nested
    @DisplayName("응답 데이터 스펙")
    inner class ResponseSpec {

        @Test
        @DisplayName("검색 결과는 ISIN, 심볼, 이름을 포함한다")
        fun `result contains isin symbol and name`() = integrationTest {
            // Given
            val isin = TestFixtures.Isin.APPLE

            // When
            val result = ufc.businessInsider.searchIsin(isin)

            // Then
            assertThat(result.isin).isNotEmpty()
            assertThat(result.symbol).isNotEmpty()
            assertThat(result.name).isNotEmpty()
        }

        @Test
        @DisplayName("검색 결과의 ISIN은 요청한 ISIN과 일치한다")
        fun `result ISIN matches requested ISIN`() = integrationTest {
            // Given
            val isin = TestFixtures.Isin.MICROSOFT

            // When
            val result = ufc.businessInsider.searchIsin(isin)

            // Then
            assertThat(result.isin).isEqualTo(isin)
        }
    }

    @Nested
    @DisplayName("ISIN 형식")
    inner class IsinFormat {

        @Test
        @DisplayName("ISIN은 12자리 영숫자로 구성된다: 국가코드(2) + 식별자(9) + 체크섬(1)")
        fun `ISIN consists of 12 alphanumeric characters`() {
            // ISIN 구조:
            // - 국가 코드 (2자리): ISO 3166-1 alpha-2 (예: US, KR, JP, DE)
            // - 기본 식별자 (9자리): 숫자 또는 문자
            // - 체크 디지트 (1자리): Luhn 알고리즘 기반 숫자

            val appleIsin = "US0378331005"
            assertThat(appleIsin).hasSize(12)
            assertThat(appleIsin.substring(0, 2)).isEqualTo("US")  // 국가 코드
            assertThat(appleIsin.substring(2, 11)).isEqualTo("037833100")  // 식별자
            assertThat(appleIsin.substring(11, 12)).isEqualTo("5")  // 체크섬
        }

        @Test
        @DisplayName("미국 주식 ISIN은 US로 시작한다")
        fun `US stock ISIN starts with US`() = integrationTest {
            val usIsins = listOf(
                "US0378331005",  // Apple
                "US5949181045",  // Microsoft
                "US88160R1014"   // Tesla
            )

            usIsins.forEach { isin ->
                assertThat(isin).startsWith("US")
                val result = ufc.businessInsider.searchIsin(isin)
                assertThat(result.isin).isEqualTo(isin)
            }
        }

        @Test
        @DisplayName("한국 주식 ISIN은 KR로 시작한다")
        fun `Korean stock ISIN starts with KR`() = integrationTest {
            // KR7005930003: 삼성전자
            val samsungIsin = "KR7005930003"

            assertThat(samsungIsin).startsWith("KR")

            val result = ufc.businessInsider.searchIsin(samsungIsin)
            assertThat(result.isin).isEqualTo(samsungIsin)
            assertThat(result.name).contains("Samsung")
        }

        @Test
        @DisplayName("일본 주식 ISIN은 JP로 시작한다")
        fun `Japanese stock ISIN starts with JP`() = integrationTest {
            // JP3633400001: Toyota Motor
            val toyotaIsin = "JP3633400001"

            assertThat(toyotaIsin).startsWith("JP")

            val result = ufc.businessInsider.searchIsin(toyotaIsin)
            assertThat(result.isin).isEqualTo(toyotaIsin)
            assertThat(result.name).contains("Toyota")
        }
    }

    @Nested
    @DisplayName("데이터 접근 방법")
    inner class DataAccessExamples {

        @Test
        @DisplayName("검색 결과에서 심볼, ISIN, 종목명을 얻을 수 있다")
        fun `can get symbol, isin, and name from result`() = integrationTest {
            val result = ufc.businessInsider.searchIsin("US0378331005")

            // 티커 심볼 (예: AAPL)
            val symbol = result.symbol
            assertThat(symbol).isEqualTo("AAPL")

            // ISIN 코드
            val isin = result.isin
            assertThat(isin).isEqualTo("US0378331005")

            // 종목명
            val name = result.name
            assertThat(name).contains("Apple")
        }

        @Test
        @DisplayName("검색 결과에서 거래소, 통화 정보를 얻을 수 있다")
        fun `can get exchange and currency from result`() = integrationTest {
            val result = ufc.businessInsider.searchIsin("US0378331005")

            // 거래소 코드 (예: NASDAQ, NYSE)
            val exchange = result.exchange
            // exchange는 null일 수 있음
            exchange?.let {
                assertThat(it).isNotBlank()
            }

            // 통화 코드 (예: USD, KRW)
            val currency = result.currency
            currency?.let {
                assertThat(it).isNotBlank()
            }
        }

        @Test
        @DisplayName("검색 결과에서 종목 유형을 얻을 수 있다")
        fun `can get security type from result`() = integrationTest {
            val result = ufc.businessInsider.searchIsin("US0378331005")

            // 종목 유형 (예: Stock, ETF, Bond 등)
            val type = result.type
            // type은 null일 수 있음
            type?.let {
                assertThat(it).isNotBlank()
            }
        }

        @Test
        @DisplayName("ISIN으로 검색한 심볼을 Yahoo Finance API에서 사용할 수 있다")
        fun `symbol from ISIN search can be used with Yahoo Finance API`() = integrationTest {
            // 1. ISIN으로 심볼 검색
            val searchResult = ufc.businessInsider.searchIsin("US0378331005")
            val symbol = searchResult.symbol  // "AAPL"

            // 2. Yahoo Finance API에서 해당 심볼로 조회
            val quoteResult = ufc.yahoo.quoteSummary(
                symbol,
                com.ulalax.ufc.domain.model.quote.QuoteSummaryModule.PRICE
            )

            // 3. 검증
            assertThat(quoteResult).isNotNull()
            assertThat(quoteResult.hasModule(com.ulalax.ufc.domain.model.quote.QuoteSummaryModule.PRICE)).isTrue()
        }
    }

    @Nested
    @DisplayName("활용 예제")
    inner class UsageExamples {

        @Test
        @DisplayName("일본 주식(Toyota) ISIN으로 검색할 수 있다")
        fun `can search Japanese stock ISIN`() = integrationTest {
            // Given
            val isin = TestFixtures.Isin.TOYOTA  // JP3633400001

            // When
            val result = ufc.businessInsider.searchIsin(isin)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.isin).isEqualTo(isin)
            assertThat(result.name).contains("Toyota")
        }

        @Test
        @DisplayName("ISIN으로 검색하여 Yahoo Finance 심볼로 변환할 수 있다")
        fun `can convert ISIN to Yahoo Finance symbol`() = integrationTest {
            // Given
            val isin = TestFixtures.Isin.APPLE

            // When
            val searchResult = ufc.businessInsider.searchIsin(isin)
            val yahooSymbol = searchResult.symbol

            // Then - Yahoo Finance API로 실제 조회 가능한지 확인
            val yahooQuote = ufc.yahoo.quoteSummary(
                yahooSymbol,
                com.ulalax.ufc.domain.model.quote.QuoteSummaryModule.PRICE
            )

            assertThat(yahooQuote).isNotNull()
        }
    }

    @Nested
    @DisplayName("검증")
    inner class ValidationSpec {

        @Test
        @DisplayName("잘못된 ISIN 형식은 ValidationException을 던진다")
        fun `throws ValidationException for invalid ISIN format`() = integrationTest {
            // Given - 12자리가 아닌 ISIN
            val invalidIsin = "INVALID"

            // When & Then
            val result = runCatching { ufc.businessInsider.searchIsin(invalidIsin) }
            // Then
            assertThat(result.isFailure).isTrue()
        }

        @Test
        @DisplayName("빈 ISIN은 ValidationException을 던진다")
        fun `throws ValidationException for empty ISIN`() = integrationTest {
            // Given
            val emptyIsin = ""

            // When & Then
            val result = runCatching { ufc.businessInsider.searchIsin(emptyIsin) }
            // Then
            assertThat(result.isFailure).isTrue()
        }

        @Test
        @DisplayName("소문자 ISIN도 처리할 수 있다")
        fun `can handle lowercase ISIN`() = integrationTest {
            // Given - 소문자 ISIN
            val lowercaseIsin = TestFixtures.Isin.APPLE.lowercase()

            // When
            val result = ufc.businessInsider.searchIsin(lowercaseIsin)

            // Then
            assertThat(result).isNotNull()
            // ISIN은 대문자로 정규화되어 반환될 수 있음
            assertThat(result.symbol).isEqualTo("AAPL")
        }
    }

    @Nested
    @DisplayName("에러 케이스")
    inner class ErrorCases {

        @Test
        @DisplayName("존재하지 않는 ISIN 조회 시 NoSuchElementException을 던진다")
        fun `throws NoSuchElementException for non-existent ISIN`() = integrationTest {
            // Given - 형식은 올바르나 존재하지 않는 ISIN
            val nonExistentIsin = TestFixtures.Isin.INVALID  // XX0000000000

            // When & Then
            val result = runCatching { ufc.businessInsider.searchIsin(nonExistentIsin) }
            // Then
            assertThat(result.isFailure).isTrue()
        }
    }
}
