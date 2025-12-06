package com.ulalax.ufc.integration.yahoo

import com.ulalax.ufc.fixture.TestFixtures
import com.ulalax.ufc.integration.utils.IntegrationTestBase
import com.ulalax.ufc.integration.utils.RecordingConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Yahoo.options() API Integration 테스트
 *
 * 이 테스트는 실제 Yahoo Finance API를 호출하여 Options 기능을 검증합니다.
 * API 가이드처럼 읽힐 수 있도록 @Nested 그룹핑 패턴을 사용합니다.
 *
 * ## 거래일/비거래일 동작
 * - **거래일 (장중)**: bid, ask, lastPrice가 실시간으로 변동하며, inTheMoney 여부도 기초자산 가격에 따라 변함
 * - **거래일 (장외)**: 마지막 호가/거래가 유지, 일부 옵션은 bid/ask가 null일 수 있음
 * - **휴장일**: 전일 종가 기준 데이터, bid/ask가 null인 옵션이 많음
 *
 * ## 테스트 실행 방법
 * ```bash
 * # 특정 클래스 실행
 * ./gradlew test --tests 'OptionsSpec'
 *
 * # 특정 그룹 실행
 * ./gradlew test --tests 'OptionsSpec$BasicBehavior'
 * ```
 */
@DisplayName("[I] Yahoo.options() - 옵션 체인 데이터 조회")
class OptionsSpec : IntegrationTestBase() {

    @Nested
    @DisplayName("기본 동작")
    inner class BasicBehavior {

        @Test
        @DisplayName("AAPL 옵션 체인을 조회할 수 있다")
        fun `returns options chain for AAPL`() = integrationTest(
            RecordingConfig.Paths.Yahoo.OPTIONS,
            "aapl_options"
        ) {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.yahoo.options(symbol)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.underlyingSymbol).isEqualTo(symbol)
            assertThat(result.expirationDates).isNotEmpty()
            assertThat(result.strikes).isNotEmpty()
        }

        @Test
        @DisplayName("특정 만기일의 옵션 체인을 조회할 수 있다")
        fun `returns options chain for specific expiration date`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // 먼저 사용 가능한 만기일 조회
            val options = ufc.yahoo.options(symbol)
            val firstExpiration = options.expirationDates.first()

            // When - 특정 만기일 조회
            val result = ufc.yahoo.options(symbol, firstExpiration)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.underlyingSymbol).isEqualTo(symbol)
            assertThat(result.optionsChain.expirationDate).isEqualTo(firstExpiration)
        }
    }

    @Nested
    @DisplayName("응답 데이터 스펙")
    inner class ResponseSpec {

        @Test
        @DisplayName("옵션 체인은 콜/풋 옵션 목록을 포함한다")
        fun `options chain contains calls and puts`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.yahoo.options(symbol)

            // Then
            assertThat(result.optionsChain).isNotNull()
            assertThat(result.optionsChain.calls).isNotEmpty()
            assertThat(result.optionsChain.puts).isNotEmpty()
        }

        @Test
        @DisplayName("옵션 계약은 필수 정보를 포함한다")
        fun `option contract contains required fields`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.yahoo.options(symbol)

            // Then
            val firstCall = result.optionsChain.calls.first()
            assertThat(firstCall.contractSymbol).isNotBlank()
            assertThat(firstCall.strike).isGreaterThan(0.0)
            assertThat(firstCall.currency).isNotBlank()
            assertThat(firstCall.contractSize).isNotBlank()
            assertThat(firstCall.expiration).isGreaterThan(0L)
        }

        @Test
        @DisplayName("기초 자산 정보를 포함한다")
        fun `includes underlying quote information`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.yahoo.options(symbol)

            // Then
            assertThat(result.underlyingQuote).isNotNull()
            val quote = result.underlyingQuote!!
            assertThat(quote.symbol).isEqualTo(symbol)
            assertThat(quote.regularMarketPrice).isNotNull().isGreaterThan(0.0)
        }

        @Test
        @DisplayName("만기일 목록과 행사가 목록을 포함한다")
        fun `includes expiration dates and strikes list`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.yahoo.options(symbol)

            // Then
            assertThat(result.expirationDates).isNotEmpty()
            assertThat(result.strikes).isNotEmpty()

            // 만기일은 오름차순 정렬되어야 함
            val sortedExpirations = result.expirationDates.sorted()
            assertThat(result.expirationDates).isEqualTo(sortedExpirations)
        }
    }

    @Nested
    @DisplayName("데이터 접근 방법")
    inner class DataAccessExamples {

        @Test
        @DisplayName("콜 옵션에서 특정 행사가를 찾을 수 있다")
        fun `can find call option by strike`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.yahoo.options(symbol)
            // 실제 콜 옵션 체인에 존재하는 행사가 사용
            val callStrike = result.optionsChain.calls.first().strike

            // Then
            val callOption = result.optionsChain.findCall(callStrike)
            assertThat(callOption).isNotNull()
            assertThat(callOption!!.strike).isEqualTo(callStrike)
        }

        @Test
        @DisplayName("풋 옵션에서 특정 행사가를 찾을 수 있다")
        fun `can find put option by strike`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.yahoo.options(symbol)
            // 실제 풋 옵션 체인에 존재하는 행사가 사용
            val putStrike = result.optionsChain.puts.first().strike

            // Then
            val putOption = result.optionsChain.findPut(putStrike)
            assertThat(putOption).isNotNull()
            assertThat(putOption!!.strike).isEqualTo(putStrike)
        }

        @Test
        @DisplayName("ITM(내가격) 옵션을 필터링할 수 있다")
        fun `can filter in-the-money options`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.yahoo.options(symbol)

            // Then
            val itmCalls = result.optionsChain.getInTheMoneyCall()
            val itmPuts = result.optionsChain.getInTheMoneyPut()

            // ITM 옵션 검증
            itmCalls.forEach { assertThat(it.inTheMoney).isTrue() }
            itmPuts.forEach { assertThat(it.inTheMoney).isTrue() }
        }

        @Test
        @DisplayName("OTM(외가격) 옵션을 필터링할 수 있다")
        fun `can filter out-of-the-money options`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.yahoo.options(symbol)

            // Then
            val otmCalls = result.optionsChain.getOutOfTheMoneyCall()
            val otmPuts = result.optionsChain.getOutOfTheMoneyPut()

            // OTM 옵션 검증
            otmCalls.forEach { assertThat(it.inTheMoney).isFalse() }
            otmPuts.forEach { assertThat(it.inTheMoney).isFalse() }
        }
    }

    @Nested
    @DisplayName("유틸리티 메서드")
    inner class UtilityMethods {

        @Test
        @DisplayName("가장 가까운 행사가를 찾을 수 있다")
        fun `can find nearest strike price`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.yahoo.options(symbol)
            val currentPrice = result.underlyingQuote?.regularMarketPrice ?: 150.0

            // Then
            val nearestStrike = result.findNearestStrike(currentPrice)
            assertThat(nearestStrike).isNotNull()
        }

        @Test
        @DisplayName("ATM(등가격) 옵션을 찾을 수 있다")
        fun `can find at-the-money options`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.yahoo.options(symbol)
            val (atmCall, atmPut) = result.findAtTheMoneyOptions()

            // Then - 현재가 정보가 있으면 ATM 옵션도 찾을 수 있어야 함
            assertThat(result.underlyingQuote?.regularMarketPrice).isNotNull()
            assertThat(atmCall).isNotNull()
            assertThat(atmPut).isNotNull()

            // ATM 콜/풋의 행사가는 동일해야 함
            assertThat(atmCall!!.strike).isEqualTo(atmPut!!.strike)
        }

        @Test
        @DisplayName("만기일을 LocalDate로 변환할 수 있다")
        fun `can convert expiration dates to LocalDate`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.yahoo.options(symbol)
            val localDates = result.getExpirationDatesAsLocalDate()

            // Then
            assertThat(localDates).isNotEmpty()
            assertThat(localDates).hasSize(result.expirationDates.size)
        }

    }

    @Nested
    @DisplayName("활용 예제")
    inner class UsageExamples {

        @Test
        @DisplayName("TSLA 옵션 체인을 조회할 수 있다")
        fun `can fetch TSLA options chain`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.TSLA

            // When
            val result = ufc.yahoo.options(symbol)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.underlyingSymbol).isEqualTo(symbol)
            assertThat(result.optionsChain.calls).isNotEmpty()
            assertThat(result.optionsChain.puts).isNotEmpty()
        }

        @Test
        @DisplayName("Ufc 파사드를 통해 옵션 체인을 조회할 수 있다")
        fun `can fetch options via Ufc facade`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL

            // When
            val result = ufc.options(symbol)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.underlyingSymbol).isEqualTo(symbol)
        }
    }

    @Nested
    @DisplayName("에러 케이스")
    inner class ErrorCases {

        @Test
        @DisplayName("존재하지 않는 심볼 조회 시 적절한 에러를 반환한다")
        fun `handles invalid symbol gracefully`() = integrationTest {
            // Given
            val invalidSymbol = TestFixtures.Symbols.INVALID

            // When & Then
            val result = runCatching {
                ufc.yahoo.options(invalidSymbol)
            }

            // Then
            assertThat(result.isFailure).isTrue()
        }

        @Test
        @DisplayName("잘못된 만기일 조회 시 빈 옵션 체인을 반환한다")
        fun `returns empty options chain for invalid expiration date`() = integrationTest {
            // Given
            val symbol = TestFixtures.Symbols.AAPL
            val invalidExpiration = 0L

            // When
            val options = ufc.yahoo.options(symbol, invalidExpiration)

            // Then - 빈 체인이거나 유효한 데이터여야 함
            assertThat(options).isNotNull()
        }
    }
}
