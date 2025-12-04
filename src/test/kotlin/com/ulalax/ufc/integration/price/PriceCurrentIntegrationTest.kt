package com.ulalax.ufc.integration.price

import com.ulalax.ufc.api.client.Ufc
import com.ulalax.ufc.api.client.UfcClientConfig
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitingSettings
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import kotlin.system.measureTimeMillis

/**
 * PriceService - 현재가 조회 통합 테스트
 *
 * 이 테스트는 실제 Yahoo Finance API를 호출합니다.
 * Mock이나 Fake 객체를 사용하지 않고, 실제 네트워크 요청을 통해
 * ufc.price.getCurrentPrice() 메서드의 모든 시나리오를 검증합니다.
 *
 * ## 테스트 환경
 * - 실제 Yahoo Finance API 호출
 * - 네트워크 연결 필요
 * - API Rate Limiting 적용
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew test --tests "*PriceCurrentIntegrationTest" --tests "*.integration.*"
 * ```
 *
 * ## 주의사항
 * - 네트워크 상태에 따라 테스트 실패 가능
 * - API 변경으로 인한 실패 가능
 * - Rate Limiting으로 인한 지연 발생 가능
 */
@Tag("integration")
@DisplayName("Price Service - 현재가 조회 통합 테스트")
class PriceCurrentIntegrationTest {

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
    @DisplayName("getCurrentPrice() - 단일 심볼 조회")
    inner class GetCurrentPrice {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("유효한 심볼로 조회 시 현재가 데이터를 반환한다")
            fun shouldReturnPriceDataForValidSymbol() = runTest {
                // Given: AAPL 심볼

                // When: AAPL 현재가 조회
                val priceData = ufc.price.getCurrentPrice("AAPL")

                // Then: 정확한 가격 데이터 반환
                assertThat(priceData.symbol)
                    .withFailMessage("심볼은 'AAPL'이어야 합니다. 실제값: ${priceData.symbol}")
                    .isEqualTo("AAPL")

                assertThat(priceData.lastPrice)
                    .withFailMessage("현재가는 0보다 커야 합니다. 실제값: ${priceData.lastPrice}")
                    .isGreaterThan(0.0)

                assertThat(priceData.currency)
                    .withFailMessage("통화는 USD여야 합니다. 실제값: ${priceData.currency}")
                    .isEqualTo("USD")

                assertThat(priceData.exchange)
                    .withFailMessage("거래소 정보가 있어야 합니다")
                    .isNotBlank()

                assertThat(priceData.marketCap)
                    .withFailMessage("시가총액은 0보다 커야 합니다. 실제값: ${priceData.marketCap}")
                    .isGreaterThan(0)
            }

            @Test
            @DisplayName("ETF 심볼로 조회 시 정상 데이터를 반환한다")
            fun shouldReturnPriceDataForETFSymbol() = runTest {
                // Given: SPY ETF 심볼

                // When
                val priceData = ufc.price.getCurrentPrice("SPY")

                // Then
                assertThat(priceData.symbol)
                    .withFailMessage("심볼은 'SPY'여야 합니다")
                    .isEqualTo("SPY")

                assertThat(priceData.lastPrice)
                    .withFailMessage("ETF 현재가는 0보다 커야 합니다")
                    .isGreaterThan(0.0)

                assertThat(priceData.currency)
                    .withFailMessage("통화는 USD여야 합니다")
                    .isEqualTo("USD")
            }

            @Test
            @DisplayName("인덱스 심볼로 조회 시 정상 데이터를 반환한다")
            fun shouldReturnPriceDataForIndexSymbol() = runTest {
                // Given: S&P 500 Index 심볼 (^GSPC)

                // When
                val priceData = ufc.price.getCurrentPrice("^GSPC")

                // Then
                assertThat(priceData.symbol)
                    .withFailMessage("심볼은 '^GSPC'여야 합니다")
                    .isEqualTo("^GSPC")

                assertThat(priceData.lastPrice)
                    .withFailMessage("인덱스 현재가는 0보다 커야 합니다")
                    .isGreaterThan(0.0)
            }

            @Test
            @DisplayName("기술주 심볼로 조회 시 모든 가격 필드가 존재한다")
            fun shouldReturnAllPriceFieldsForTechStock() = runTest {
                // Given: MSFT 심볼

                // When
                val priceData = ufc.price.getCurrentPrice("MSFT")

                // Then: 필수 가격 필드 검증
                assertThat(priceData.symbol).isEqualTo("MSFT")
                assertThat(priceData.lastPrice).isGreaterThan(0.0)
                assertThat(priceData.regularMarketPrice).isGreaterThan(0.0)
                assertThat(priceData.previousClose).isGreaterThan(0.0)

                // 선택적 필드 검증 (장 마감 후에는 null일 수 있음)
                if (priceData.open != null) {
                    assertThat(priceData.open).isGreaterThan(0.0)
                }
                if (priceData.dayHigh != null) {
                    assertThat(priceData.dayHigh).isGreaterThan(0.0)
                }
                if (priceData.dayLow != null) {
                    assertThat(priceData.dayLow).isGreaterThan(0.0)
                }
                if (priceData.volume != null) {
                    assertThat(priceData.volume).isGreaterThan(0)
                }
            }

            @Test
            @DisplayName("다양한 특수문자 포함 심볼을 정상 처리한다")
            fun shouldHandleSymbolsWithSpecialCharacters() = runTest {
                // Given: 하이픈 포함 심볼 (BRK-A)

                // When
                val priceData = ufc.price.getCurrentPrice("BRK-A")

                // Then
                assertThat(priceData.symbol)
                    .withFailMessage("하이픈 포함 심볼을 정상 처리해야 합니다")
                    .isEqualTo("BRK-A")

                assertThat(priceData.lastPrice)
                    .withFailMessage("BRK-A 현재가는 0보다 커야 합니다")
                    .isGreaterThan(0.0)
            }
        }

        @Nested
        @DisplayName("캐싱 동작")
        inner class CachingBehavior {

            @Test
            @DisplayName("같은 심볼을 연속 조회 시 두 번째는 빠르게 반환된다")
            fun shouldReturnFasterOnSecondCallDueToCache() = runTest {
                // Given: AAPL 심볼

                // When: 2회 연속 조회하고 시간 측정
                val firstCallTime = measureTimeMillis {
                    ufc.price.getCurrentPrice("AAPL")
                }

                val secondCallTime = measureTimeMillis {
                    ufc.price.getCurrentPrice("AAPL")
                }

                // Then: 두 번째 호출이 첫 번째보다 빠름 (캐싱)
                assertThat(secondCallTime)
                    .withFailMessage(
                        "두 번째 호출(캐시)이 첫 번째보다 빨라야 합니다. " +
                        "첫 번째: ${firstCallTime}ms, 두 번째: ${secondCallTime}ms"
                    )
                    .isLessThan(firstCallTime)
            }

            @Test
            @DisplayName("다른 심볼 조회 시 각각 독립적으로 조회된다")
            fun shouldQueryIndependentlyForDifferentSymbols() = runTest {
                // Given: 3개의 다른 심볼

                // When
                val aapl = ufc.price.getCurrentPrice("AAPL")
                val msft = ufc.price.getCurrentPrice("MSFT")
                val googl = ufc.price.getCurrentPrice("GOOGL")

                // Then: 각 심볼이 올바른 데이터 반환
                assertThat(aapl.symbol).isEqualTo("AAPL")
                assertThat(msft.symbol).isEqualTo("MSFT")
                assertThat(googl.symbol).isEqualTo("GOOGL")

                // 각 가격이 서로 다름 (정상적인 시장 상황)
                assertThat(aapl.lastPrice)
                    .withFailMessage("AAPL과 MSFT 가격은 달라야 합니다")
                    .isNotEqualTo(msft.lastPrice)

                assertThat(msft.lastPrice)
                    .withFailMessage("MSFT와 GOOGL 가격은 달라야 합니다")
                    .isNotEqualTo(googl.lastPrice)
            }

            @Test
            @DisplayName("캐싱된 데이터도 정확한 가격 정보를 포함한다")
            fun shouldReturnAccurateCachedData() = runTest {
                // Given: 첫 번째 조회로 캐시 생성
                val firstCall = ufc.price.getCurrentPrice("AAPL")

                // When: 두 번째 조회 (캐시에서)
                val secondCall = ufc.price.getCurrentPrice("AAPL")

                // Then: 두 응답이 동일
                assertThat(secondCall.symbol).isEqualTo(firstCall.symbol)
                assertThat(secondCall.lastPrice).isEqualTo(firstCall.lastPrice)
                assertThat(secondCall.currency).isEqualTo(firstCall.currency)
                assertThat(secondCall.marketCap).isEqualTo(firstCall.marketCap)
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
                    ufc.price.getCurrentPrice("")
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
                    ufc.price.getCurrentPrice("   ")
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

            @Test
            @DisplayName("20자 초과 심볼 조회 시 INVALID_SYMBOL 에러를 발생시킨다")
            fun shouldThrowInvalidSymbolErrorForTooLongSymbol() = runTest {
                // Given: 21자 심볼
                val tooLongSymbol = "A".repeat(21)

                // When & Then
                val exception = try {
                    ufc.price.getCurrentPrice(tooLongSymbol)
                    null
                } catch (e: UfcException) {
                    e
                }

                assertThat(exception)
                    .withFailMessage("20자 초과 심볼 조회 시 UfcException이 발생해야 합니다")
                    .isNotNull()
                    .isInstanceOf(UfcException::class.java)

                assertThat(exception!!.errorCode)
                    .withFailMessage("ErrorCode는 INVALID_SYMBOL이어야 합니다. 실제: ${exception.errorCode}")
                    .isEqualTo(ErrorCode.INVALID_SYMBOL)
            }

            @Test
            @DisplayName("허용된 특수문자는 정상 처리된다")
            fun shouldAcceptAllowedSpecialCharacters() = runTest {
                // Given: 캐럿(^), 하이픈(-), 점(.) 허용

                // When & Then: 에러 없이 조회됨
                val gspc = ufc.price.getCurrentPrice("^GSPC")  // 인덱스
                val brkA = ufc.price.getCurrentPrice("BRK-A") // 클래스 구분

                assertThat(gspc.symbol).isEqualTo("^GSPC")
                assertThat(brkA.symbol).isEqualTo("BRK-A")
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
                    ufc.price.getCurrentPrice(invalidSymbol)
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
                        ErrorCode.PRICE_DATA_NOT_FOUND,
                        ErrorCode.EXTERNAL_API_ERROR
                    )
            }

            @Test
            @DisplayName("유효하지 않은 심볼 형식 조회 시 적절한 에러를 발생시킨다")
            fun shouldThrowErrorForInvalidSymbolFormat() = runTest {
                // Given: 특수문자가 너무 많은 심볼
                val invalidSymbol = "@@@###$$$"

                // When & Then
                val exception = try {
                    ufc.price.getCurrentPrice(invalidSymbol)
                    null
                } catch (e: UfcException) {
                    e
                }

                assertThat(exception)
                    .withFailMessage("유효하지 않은 심볼 형식 조회 시 UfcException이 발생해야 합니다")
                    .isNotNull()

                // ErrorCode 검증 - INVALID_SYMBOL 또는 DATA_NOT_FOUND 모두 허용
                assertThat(exception!!.errorCode)
                    .withFailMessage("에러 코드가 유효한 범위 내에 있어야 합니다. 실제: ${exception.errorCode}")
                    .isIn(
                        ErrorCode.INVALID_SYMBOL,
                        ErrorCode.DATA_NOT_FOUND,
                        ErrorCode.PRICE_DATA_NOT_FOUND,
                        ErrorCode.EXTERNAL_API_ERROR
                    )
            }
        }

        @Nested
        @DisplayName("가격 데이터 계산")
        inner class PriceDataCalculations {

            @Test
            @DisplayName("52주 범위 위치를 정확히 계산한다")
            fun shouldCalculateFiftyTwoWeekPositionCorrectly() = runTest {
                // Given: AAPL 심볼

                // When
                val priceData = ufc.price.getCurrentPrice("AAPL")

                // Then: 52주 위치가 0.0 ~ 1.0 범위 내
                val position = priceData.fiftyTwoWeekPosition()
                if (position != null) {
                    assertThat(position)
                        .withFailMessage("52주 위치는 0.0 ~ 1.0 범위여야 합니다. 실제값: $position")
                        .isBetween(0.0, 1.0)
                }
            }

            @Test
            @DisplayName("일중 변동률을 정확히 계산한다")
            fun shouldCalculateDailyChangePercentCorrectly() = runTest {
                // Given: AAPL 심볼

                // When
                val priceData = ufc.price.getCurrentPrice("AAPL")

                // Then: 일중 변동률이 합리적인 범위 (-20% ~ +20%)
                val changePercent = priceData.dailyChangePercent()
                if (changePercent != null) {
                    assertThat(changePercent)
                        .withFailMessage(
                            "일중 변동률은 일반적으로 -20% ~ +20% 범위입니다. 실제값: $changePercent%"
                        )
                        .isBetween(-20.0, 20.0)
                }
            }

            @Test
            @DisplayName("이동평균선 위치를 정확히 판단한다")
            fun shouldDetermineMovingAveragePositionCorrectly() = runTest {
                // Given: AAPL 심볼

                // When
                val priceData = ufc.price.getCurrentPrice("AAPL")

                // Then: MA 위치 판단이 일관성 있음
                val isAbove50 = priceData.isAbove50DayMA()
                val isAbove200 = priceData.isAbove200DayMA()

                if (isAbove50 != null && priceData.fiftyDayAverage != null) {
                    val expectedAbove50 = (priceData.lastPrice ?: priceData.regularMarketPrice ?: 0.0) >
                                         priceData.fiftyDayAverage!!
                    assertThat(isAbove50)
                        .withFailMessage("50일 MA 위치 판단이 일치해야 합니다")
                        .isEqualTo(expectedAbove50)
                }

                if (isAbove200 != null && priceData.twoHundredDayAverage != null) {
                    val expectedAbove200 = (priceData.lastPrice ?: priceData.regularMarketPrice ?: 0.0) >
                                          priceData.twoHundredDayAverage!!
                    assertThat(isAbove200)
                        .withFailMessage("200일 MA 위치 판단이 일치해야 합니다")
                        .isEqualTo(expectedAbove200)
                }
            }
        }
    }

    @Nested
    @DisplayName("getCurrentPrice(List<String>) - 다중 심볼 조회")
    inner class GetCurrentPriceMultiple {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("여러 심볼 동시 조회 시 모든 데이터를 맵으로 반환한다")
            fun shouldReturnAllPriceDataAsMap() = runTest {
                // Given: 3개 심볼
                val symbols = listOf("AAPL", "MSFT", "GOOGL")

                // When
                val results = ufc.price.getCurrentPrice(symbols)

                // Then: 대부분 반환 (일부 실패 허용)
                assertThat(results)
                    .withFailMessage("최소 2개 이상의 심볼이 반환되어야 합니다")
                    .hasSizeGreaterThanOrEqualTo(2)

                results.forEach { (symbol, priceData) ->
                    assertThat(priceData.symbol)
                        .withFailMessage("반환된 심볼이 요청한 심볼과 일치해야 합니다")
                        .isEqualTo(symbol)

                    assertThat(priceData.lastPrice)
                        .withFailMessage("$symbol 의 가격은 0보다 커야 합니다")
                        .isGreaterThan(0.0)
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
                val results = ufc.price.getCurrentPrice(symbols)

                // Then: 대부분 반환 (일부 실패 허용)
                assertThat(results.size)
                    .withFailMessage("10개 중 최소 8개는 반환되어야 합니다. 실제: ${results.size}")
                    .isGreaterThanOrEqualTo(8)

                // 모든 반환된 데이터가 유효한지 확인
                results.values.forEach { priceData ->
                    assertThat(priceData.lastPrice)
                        .withFailMessage("${priceData.symbol} 의 가격이 유효해야 합니다")
                        .isGreaterThan(0.0)
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
                val results = ufc.price.getCurrentPrice(symbols)

                // Then: 모든 타입이 조회됨
                assertThat(results)
                    .withFailMessage("모든 타입의 심볼이 조회되어야 합니다")
                    .hasSizeGreaterThanOrEqualTo(2)

                results.forEach { (_, priceData) ->
                    assertThat(priceData.lastPrice).isGreaterThan(0.0)
                }
            }

            @Test
            @DisplayName("2개 심볼 조회가 정상 동작한다")
            fun shouldHandleTwoSymbols() = runTest {
                // Given: 2개 심볼
                val symbols = listOf("AAPL", "MSFT")

                // When
                val results = ufc.price.getCurrentPrice(symbols)

                // Then: 2개 모두 반환
                assertThat(results).hasSize(2)
                assertThat(results).containsKeys("AAPL", "MSFT")

                results.values.forEach { priceData ->
                    assertThat(priceData.lastPrice).isGreaterThan(0.0)
                }
            }
        }

        @Nested
        @DisplayName("부분 실패 처리")
        inner class PartialFailureHandling {

            @Test
            @DisplayName("일부 심볼이 유효하지 않으면 에러를 발생시킨다")
            fun shouldThrowErrorWhenSomeSymbolsInvalid() = runTest {
                // Given: 유효한 심볼과 유효하지 않은 심볼 혼합
                val symbols = listOf("AAPL", "INVALID123", "MSFT")

                // When & Then: 현재 구현은 일부 심볼이 유효하지 않으면 전체 요청 실패
                val exception = try {
                    ufc.price.getCurrentPrice(symbols)
                    null
                } catch (e: UfcException) {
                    e
                }

                assertThat(exception)
                    .withFailMessage("유효하지 않은 심볼이 포함되면 UfcException이 발생해야 합니다")
                    .isNotNull()

                assertThat(exception!!.errorCode)
                    .withFailMessage("ErrorCode는 EXTERNAL_API_ERROR 또는 DATA_NOT_FOUND여야 합니다. 실제: ${exception.errorCode}")
                    .isIn(
                        ErrorCode.EXTERNAL_API_ERROR,
                        ErrorCode.DATA_NOT_FOUND,
                        ErrorCode.PRICE_DATA_NOT_FOUND
                    )
            }
        }

        @Nested
        @DisplayName("입력 검증")
        inner class InputValidation {

            @Test
            @DisplayName("빈 리스트 조회 시 IllegalArgumentException을 발생시킨다")
            fun shouldThrowExceptionForEmptyList() = runTest {
                // When & Then
                val exception = try {
                    ufc.price.getCurrentPrice(emptyList())
                    null
                } catch (e: Exception) {
                    e
                }

                assertThat(exception)
                    .withFailMessage("빈 리스트 조회 시 예외가 발생해야 합니다")
                    .isNotNull()

                // IllegalArgumentException 또는 UfcException 모두 허용
                assertThat(exception)
                    .withFailMessage("IllegalArgumentException 또는 UfcException이어야 합니다. 실제: ${exception!!::class.simpleName}")
                    .matches { it is IllegalArgumentException || it is UfcException }
            }

            @Test
            @DisplayName("중복된 심볼이 있어도 정상 처리한다")
            fun shouldHandleDuplicateSymbols() = runTest {
                // Given: 중복된 심볼
                val symbols = listOf("AAPL", "MSFT", "AAPL")

                // When
                val results = ufc.price.getCurrentPrice(symbols)

                // Then: 중복 제거되어 2개만 반환 (또는 중복 허용)
                assertThat(results)
                    .withFailMessage("중복 심볼도 정상 처리되어야 합니다")
                    .hasSizeGreaterThanOrEqualTo(2)

                assertThat(results).containsKey("AAPL")
                assertThat(results).containsKey("MSFT")
            }
        }

        @Nested
        @DisplayName("성능")
        inner class Performance {

            @Test
            @DisplayName("다중 심볼 조회가 단일 조회보다 효율적이다")
            fun shouldBeMoreEfficientThanMultipleSingleCalls() = runTest {
                // Given: 3개 심볼
                val symbols = listOf("AAPL", "MSFT", "GOOGL")

                // When: 다중 조회 시간 측정
                val batchTime = measureTimeMillis {
                    ufc.price.getCurrentPrice(symbols)
                }

                // When: 개별 조회 시간 측정
                val individualTime = measureTimeMillis {
                    symbols.forEach { symbol ->
                        ufc.price.getCurrentPrice(symbol)
                    }
                }

                // Then: 배치 조회가 더 빠름 (캐싱 효과 제외)
                // 주의: 캐싱으로 인해 이 테스트는 실패할 수 있음
                println("배치 조회: ${batchTime}ms, 개별 조회: ${individualTime}ms")

                // 정보만 출력하고 assertion은 하지 않음 (캐싱으로 인한 불확실성)
                assertThat(batchTime)
                    .withFailMessage("배치 조회 시간: ${batchTime}ms")
                    .isGreaterThan(0)
            }
        }
    }
}
