package com.ulalax.ufc.integration

import com.ulalax.ufc.api.client.Ufc
import com.ulalax.ufc.api.client.UfcClientConfig
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.domain.quote.*
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitingSettings
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*

/**
 * QuoteSummary API - 통합 테스트
 *
 * 이 테스트는 실제 Yahoo Finance QuoteSummary API를 호출합니다.
 * Mock이나 Fake 객체를 사용하지 않고, 실제 네트워크 요청을 통해
 * ufc.quoteSummary() 메서드의 모든 시나리오를 검증합니다.
 *
 * ## 테스트 환경
 * - 실제 Yahoo Finance QuoteSummary API 호출
 * - 네트워크 연결 필요
 * - API Rate Limiting 적용
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew test --tests "*QuoteSummaryIntegrationSpec" --tests "*.integration.*"
 * ```
 *
 * ## 주의사항
 * - 네트워크 상태에 따라 테스트 실패 가능
 * - API 변경으로 인한 실패 가능
 * - Rate Limiting으로 인한 지연 발생 가능
 * - 모듈별로 데이터가 제공되지 않을 수 있음
 */
@Tag("integration")
@DisplayName("QuoteSummary API - 통합 테스트")
class QuoteSummaryIntegrationSpec {

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
    @DisplayName("단일 모듈 조회")
    inner class SingleModuleQuery {

        @Test
        @DisplayName("PRICE 모듈만 조회 시 가격 정보를 반환한다")
        fun shouldReturnPriceModuleOnly() = runTest {
            // Given: AAPL 심볼, PRICE 모듈

            // When
            val result = ufc.quoteSummary("AAPL", QuoteSummaryModule.PRICE)

            // Then: 요청 모듈 확인
            assertThat(result.requestedModules)
                .withFailMessage("요청한 모듈은 PRICE만 있어야 합니다")
                .containsExactly(QuoteSummaryModule.PRICE)

            // PRICE 모듈 존재 확인
            assertThat(result.hasModule(QuoteSummaryModule.PRICE))
                .withFailMessage("PRICE 모듈이 존재해야 합니다")
                .isTrue()

            // PRICE 데이터 검증
            val price: Price? = result.getModule(QuoteSummaryModule.PRICE)
            assertThat(price)
                .withFailMessage("PRICE 모듈 데이터가 null이 아니어야 합니다")
                .isNotNull

            assertThat(price!!.symbol)
                .withFailMessage("심볼은 AAPL이어야 합니다")
                .isEqualTo("AAPL")

            assertThat(price.regularMarketPrice?.doubleValue)
                .withFailMessage("정규 시장 가격이 0보다 커야 합니다")
                .isNotNull()
                .isGreaterThan(0.0)
        }

        @Test
        @DisplayName("SUMMARY_DETAIL 모듈만 조회 시 상세 요약 정보를 반환한다")
        fun shouldReturnSummaryDetailModuleOnly() = runTest {
            // Given: AAPL 심볼, SUMMARY_DETAIL 모듈

            // When
            val result = ufc.quoteSummary("AAPL", QuoteSummaryModule.SUMMARY_DETAIL)

            // Then
            assertThat(result.requestedModules)
                .containsExactly(QuoteSummaryModule.SUMMARY_DETAIL)

            assertThat(result.hasModule(QuoteSummaryModule.SUMMARY_DETAIL))
                .withFailMessage("SUMMARY_DETAIL 모듈이 존재해야 합니다")
                .isTrue()

            val summaryDetail: SummaryDetail? = result.getModule(QuoteSummaryModule.SUMMARY_DETAIL)
            assertThat(summaryDetail)
                .withFailMessage("SUMMARY_DETAIL 모듈 데이터가 null이 아니어야 합니다")
                .isNotNull
        }

        @Test
        @DisplayName("FINANCIAL_DATA 모듈만 조회 시 재무 정보를 반환한다")
        fun shouldReturnFinancialDataModuleOnly() = runTest {
            // Given: AAPL 심볼, FINANCIAL_DATA 모듈

            // When
            val result = ufc.quoteSummary("AAPL", QuoteSummaryModule.FINANCIAL_DATA)

            // Then
            assertThat(result.requestedModules)
                .containsExactly(QuoteSummaryModule.FINANCIAL_DATA)

            assertThat(result.hasModule(QuoteSummaryModule.FINANCIAL_DATA))
                .withFailMessage("FINANCIAL_DATA 모듈이 존재해야 합니다")
                .isTrue()

            val financialData: FinancialData? = result.getModule(QuoteSummaryModule.FINANCIAL_DATA)
            assertThat(financialData)
                .withFailMessage("FINANCIAL_DATA 모듈 데이터가 null이 아니어야 합니다")
                .isNotNull
        }
    }

    @Nested
    @DisplayName("여러 모듈 조회")
    inner class MultipleModulesQuery {

        @Test
        @DisplayName("2개 모듈 동시 조회 시 모든 모듈 데이터를 반환한다")
        fun shouldReturnTwoModulesTogether() = runTest {
            // Given: PRICE, SUMMARY_DETAIL 모듈

            // When
            val result = ufc.quoteSummary(
                "AAPL",
                QuoteSummaryModule.PRICE,
                QuoteSummaryModule.SUMMARY_DETAIL
            )

            // Then: 요청 모듈 확인
            assertThat(result.requestedModules)
                .withFailMessage("요청한 모듈은 PRICE, SUMMARY_DETAIL이어야 합니다")
                .containsExactlyInAnyOrder(
                    QuoteSummaryModule.PRICE,
                    QuoteSummaryModule.SUMMARY_DETAIL
                )

            // 두 모듈 모두 존재 확인
            assertThat(result.hasModule(QuoteSummaryModule.PRICE))
                .withFailMessage("PRICE 모듈이 존재해야 합니다")
                .isTrue()

            assertThat(result.hasModule(QuoteSummaryModule.SUMMARY_DETAIL))
                .withFailMessage("SUMMARY_DETAIL 모듈이 존재해야 합니다")
                .isTrue()

            // 데이터 검증
            val price: Price? = result.getModule(QuoteSummaryModule.PRICE)
            val summaryDetail: SummaryDetail? = result.getModule(QuoteSummaryModule.SUMMARY_DETAIL)

            assertThat(price).isNotNull
            assertThat(summaryDetail).isNotNull
        }

        @Test
        @DisplayName("3개 이상 모듈 동시 조회가 정상 동작한다")
        fun shouldReturnThreeOrMoreModulesTogether() = runTest {
            // Given: PRICE, SUMMARY_DETAIL, FINANCIAL_DATA 모듈

            // When
            val result = ufc.quoteSummary(
                "AAPL",
                QuoteSummaryModule.PRICE,
                QuoteSummaryModule.SUMMARY_DETAIL,
                QuoteSummaryModule.FINANCIAL_DATA
            )

            // Then
            assertThat(result.requestedModules)
                .hasSize(3)
                .containsExactlyInAnyOrder(
                    QuoteSummaryModule.PRICE,
                    QuoteSummaryModule.SUMMARY_DETAIL,
                    QuoteSummaryModule.FINANCIAL_DATA
                )

            // 사용 가능한 모듈 확인
            val availableModules = result.getAvailableModules()
            assertThat(availableModules)
                .withFailMessage("최소 2개 이상의 모듈이 제공되어야 합니다")
                .hasSizeGreaterThanOrEqualTo(2)
        }

        @Test
        @DisplayName("주식 관련 주요 모듈들을 조회한다")
        fun shouldReturnStockRelatedModules() = runTest {
            // Given: 주식 관련 주요 모듈들
            val modules = arrayOf(
                QuoteSummaryModule.PRICE,
                QuoteSummaryModule.SUMMARY_DETAIL,
                QuoteSummaryModule.ASSET_PROFILE,
                QuoteSummaryModule.FINANCIAL_DATA,
                QuoteSummaryModule.DEFAULT_KEY_STATISTICS
            )

            // When
            val result = ufc.quoteSummary("AAPL", *modules)

            // Then: 요청 모듈 확인
            assertThat(result.requestedModules)
                .containsExactlyInAnyOrder(*modules)

            // 대부분의 모듈이 제공됨 (일부는 null일 수 있음)
            val availableCount = modules.count { result.hasModule(it) }
            assertThat(availableCount)
                .withFailMessage("최소 3개 이상의 모듈이 제공되어야 합니다. 실제: $availableCount")
                .isGreaterThanOrEqualTo(3)
        }

        @Test
        @DisplayName("ETF의 펀드 관련 모듈들을 조회한다")
        fun shouldReturnFundRelatedModulesForETF() = runTest {
            // Given: SPY ETF, 펀드 관련 모듈
            val modules = arrayOf(
                QuoteSummaryModule.PRICE,
                QuoteSummaryModule.TOP_HOLDINGS,
                QuoteSummaryModule.FUND_PROFILE
            )

            // When
            val result = ufc.quoteSummary("SPY", *modules)

            // Then
            assertThat(result.requestedModules)
                .containsExactlyInAnyOrder(*modules)

            // PRICE는 반드시 존재
            assertThat(result.hasModule(QuoteSummaryModule.PRICE))
                .withFailMessage("ETF의 PRICE 모듈은 반드시 존재해야 합니다")
                .isTrue()

            // TOP_HOLDINGS 또는 FUND_PROFILE 중 하나는 존재 (API 응답에 따라 다름)
            val hasFundData = result.hasModule(QuoteSummaryModule.TOP_HOLDINGS) ||
                              result.hasModule(QuoteSummaryModule.FUND_PROFILE)
            assertThat(hasFundData)
                .withFailMessage("ETF는 펀드 관련 모듈이 최소 하나는 제공되어야 합니다")
                .isTrue()
        }
    }

    @Nested
    @DisplayName("모듈 미지정 시 기본값 사용")
    inner class DefaultModules {

        @Test
        @DisplayName("모듈 미지정 시 기본 모듈 세트를 조회한다")
        fun shouldUseDefaultModulesWhenNotSpecified() = runTest {
            // Given: 모듈 미지정

            // When
            val result = ufc.quoteSummary("AAPL")

            // Then: 기본 모듈이 요청됨 (PRICE, SUMMARY_DETAIL, QUOTE_TYPE)
            assertThat(result.requestedModules)
                .withFailMessage("기본 모듈은 PRICE, SUMMARY_DETAIL, QUOTE_TYPE이어야 합니다")
                .containsExactlyInAnyOrder(
                    QuoteSummaryModule.PRICE,
                    QuoteSummaryModule.SUMMARY_DETAIL,
                    QuoteSummaryModule.QUOTE_TYPE
                )

            // 최소 하나 이상의 모듈이 제공됨
            assertThat(result.getAvailableModules())
                .withFailMessage("기본 모듈 중 최소 하나는 제공되어야 합니다")
                .isNotEmpty
        }

        @Test
        @DisplayName("기본 모듈에는 가격 정보가 포함된다")
        fun shouldIncludePriceInDefaultModules() = runTest {
            // When
            val result = ufc.quoteSummary("MSFT")

            // Then: PRICE 모듈 존재
            assertThat(result.hasModule(QuoteSummaryModule.PRICE))
                .withFailMessage("기본 모듈에는 PRICE가 포함되어야 합니다")
                .isTrue()

            val price: Price? = result.getModule(QuoteSummaryModule.PRICE)
            assertThat(price?.symbol)
                .isEqualTo("MSFT")
        }
    }

    @Nested
    @DisplayName("requestedModules 확인")
    inner class RequestedModulesVerification {

        @Test
        @DisplayName("requestedModules는 사용자가 요청한 모듈 목록을 정확히 반환한다")
        fun shouldReturnExactRequestedModules() = runTest {
            // Given: 특정 모듈 세트
            val requested = arrayOf(
                QuoteSummaryModule.PRICE,
                QuoteSummaryModule.FINANCIAL_DATA
            )

            // When
            val result = ufc.quoteSummary("AAPL", *requested)

            // Then: 요청한 모듈 목록과 정확히 일치
            assertThat(result.requestedModules)
                .containsExactlyInAnyOrder(*requested)
        }

        @Test
        @DisplayName("getAvailableModules()는 실제로 데이터가 있는 모듈만 반환한다")
        fun shouldReturnOnlyModulesWithData() = runTest {
            // When
            val result = ufc.quoteSummary(
                "AAPL",
                QuoteSummaryModule.PRICE,
                QuoteSummaryModule.SUMMARY_DETAIL
            )

            // Then: 사용 가능한 모듈은 요청한 모듈의 부분집합
            val availableModules = result.getAvailableModules()
            assertThat(availableModules)
                .isSubsetOf(result.requestedModules)

            // 각 사용 가능 모듈의 데이터가 null이 아님
            availableModules.forEach { module ->
                assertThat(result.modules[module])
                    .withFailMessage("사용 가능한 모듈 $module 의 데이터는 null이 아니어야 합니다")
                    .isNotNull
            }
        }

        @Test
        @DisplayName("hasAllRequestedModules()는 모든 요청 모듈의 존재 여부를 정확히 반환한다")
        fun shouldCorrectlyCheckAllRequestedModules() = runTest {
            // When: 일반적으로 제공되는 모듈들 요청
            val result = ufc.quoteSummary(
                "AAPL",
                QuoteSummaryModule.PRICE,
                QuoteSummaryModule.SUMMARY_DETAIL
            )

            // Then: 두 모듈 모두 제공될 가능성이 높음
            val hasAll = result.hasAllRequestedModules()

            if (hasAll) {
                // 모든 모듈이 존재하면 각각 확인 가능
                assertThat(result.hasModule(QuoteSummaryModule.PRICE)).isTrue()
                assertThat(result.hasModule(QuoteSummaryModule.SUMMARY_DETAIL)).isTrue()
            } else {
                // 일부만 존재하면 사용 가능 모듈 < 요청 모듈
                assertThat(result.getAvailableModules().size)
                    .isLessThan(result.requestedModules.size)
            }
        }
    }

    @Nested
    @DisplayName("hasModule, getModule 동작 확인")
    inner class ModuleAccessMethods {

        @Test
        @DisplayName("hasModule()은 모듈 존재 여부를 정확히 반환한다")
        fun shouldCorrectlyCheckModuleExistence() = runTest {
            // When
            val result = ufc.quoteSummary(
                "AAPL",
                QuoteSummaryModule.PRICE,
                QuoteSummaryModule.SUMMARY_DETAIL
            )

            // Then: 요청한 모듈은 true, 요청하지 않은 모듈은 false
            assertThat(result.hasModule(QuoteSummaryModule.PRICE))
                .withFailMessage("요청한 PRICE 모듈은 존재해야 합니다")
                .isTrue()

            assertThat(result.hasModule(QuoteSummaryModule.EARNINGS_TREND))
                .withFailMessage("요청하지 않은 EARNINGS_TREND 모듈은 존재하지 않아야 합니다")
                .isFalse()
        }

        @Test
        @DisplayName("getModule()은 타입 안전하게 모듈 데이터를 반환한다")
        fun shouldReturnTypeSafeModuleData() = runTest {
            // When
            val result = ufc.quoteSummary("AAPL", QuoteSummaryModule.PRICE)

            // Then: 타입 캐스팅 없이 사용 가능
            val price: Price? = result.getModule(QuoteSummaryModule.PRICE)
            assertThat(price).isNotNull

            // Price 타입의 속성 직접 접근 가능
            assertThat(price!!.symbol).isEqualTo("AAPL")
            assertThat(price.regularMarketPrice).isNotNull
        }

        @Test
        @DisplayName("getModule()은 존재하지 않는 모듈에 대해 null을 반환한다")
        fun shouldReturnNullForNonExistentModule() = runTest {
            // When: PRICE만 요청
            val result = ufc.quoteSummary("AAPL", QuoteSummaryModule.PRICE)

            // Then: 요청하지 않은 모듈은 null 반환
            val earningsTrend: EarningsTrend? = result.getModule(QuoteSummaryModule.EARNINGS_TREND)
            assertThat(earningsTrend)
                .withFailMessage("요청하지 않은 모듈은 null이어야 합니다")
                .isNull()
        }

        @Test
        @DisplayName("getModule()은 잘못된 타입으로 캐스팅 시 null을 반환한다")
        fun shouldReturnNullForWrongTypeCast() = runTest {
            // When: PRICE 모듈 요청
            val result = ufc.quoteSummary("AAPL", QuoteSummaryModule.PRICE)

            // Then: 잘못된 타입으로 캐스팅하면 null
            val wrongType: SummaryDetail? = result.getModule(QuoteSummaryModule.PRICE)
            assertThat(wrongType)
                .withFailMessage("잘못된 타입으로 캐스팅하면 null이어야 합니다")
                .isNull()
        }
    }

    @Nested
    @DisplayName("요청했지만 데이터 없는 모듈 처리")
    inner class MissingModuleHandling {

        @Test
        @DisplayName("getMissingModules()는 요청했지만 제공되지 않은 모듈을 반환한다")
        fun shouldReturnMissingModules() = runTest {
            // Given: 일부는 제공되고 일부는 제공되지 않을 수 있는 모듈들
            // 주의: 실제 API 응답에 따라 결과가 다를 수 있음
            val modules = arrayOf(
                QuoteSummaryModule.PRICE,
                QuoteSummaryModule.SUMMARY_DETAIL,
                QuoteSummaryModule.INSIDER_TRANSACTIONS,
                QuoteSummaryModule.EARNINGS_TREND
            )

            // When
            val result = ufc.quoteSummary("AAPL", *modules)

            // Then: 누락된 모듈 확인
            val missingModules = result.getMissingModules()

            // 누락된 모듈은 요청한 모듈의 부분집합
            assertThat(missingModules)
                .isSubsetOf(result.requestedModules.toList())

            // 누락된 모듈은 사용 가능한 모듈과 겹치지 않음
            val availableModules = result.getAvailableModules()
            assertThat(missingModules)
                .doesNotContainAnyElementsOf(availableModules)
        }

        @Test
        @DisplayName("모든 모듈이 제공되면 getMissingModules()는 빈 Set을 반환한다")
        fun shouldReturnEmptySetWhenAllModulesProvided() = runTest {
            // Given: 일반적으로 모두 제공되는 모듈들
            val modules = arrayOf(
                QuoteSummaryModule.PRICE,
                QuoteSummaryModule.SUMMARY_DETAIL
            )

            // When
            val result = ufc.quoteSummary("AAPL", *modules)

            // Then: 두 모듈 모두 제공될 가능성이 높음
            if (result.hasAllRequestedModules()) {
                assertThat(result.getMissingModules())
                    .withFailMessage("모든 모듈이 제공되면 누락 모듈은 비어있어야 합니다")
                    .isEmpty()
            }
        }
    }

    @Nested
    @DisplayName("다양한 심볼 타입")
    inner class VariousSymbolTypes {

        @Test
        @DisplayName("주식 심볼의 QuoteSummary를 조회한다")
        fun shouldReturnQuoteSummaryForStock() = runTest {
            // When
            val result = ufc.quoteSummary("MSFT", QuoteSummaryModule.PRICE)

            // Then
            assertThat(result.hasModule(QuoteSummaryModule.PRICE)).isTrue()

            val price: Price? = result.getModule(QuoteSummaryModule.PRICE)
            assertThat(price?.symbol).isEqualTo("MSFT")
        }

        @Test
        @DisplayName("ETF 심볼의 QuoteSummary를 조회한다")
        fun shouldReturnQuoteSummaryForETF() = runTest {
            // When
            val result = ufc.quoteSummary("SPY", QuoteSummaryModule.PRICE)

            // Then
            assertThat(result.hasModule(QuoteSummaryModule.PRICE)).isTrue()

            val price: Price? = result.getModule(QuoteSummaryModule.PRICE)
            assertThat(price?.symbol).isEqualTo("SPY")
        }

        @Test
        @DisplayName("인덱스 심볼의 QuoteSummary를 조회한다")
        fun shouldReturnQuoteSummaryForIndex() = runTest {
            // When: S&P 500 인덱스
            val result = ufc.quoteSummary("^GSPC", QuoteSummaryModule.PRICE)

            // Then
            assertThat(result.hasModule(QuoteSummaryModule.PRICE)).isTrue()

            val price: Price? = result.getModule(QuoteSummaryModule.PRICE)
            assertThat(price?.symbol).isEqualTo("^GSPC")
        }
    }

    @Nested
    @DisplayName("입력 검증")
    inner class InputValidation {

        @Test
        @DisplayName("빈 문자열 심볼 조회 시 에러를 발생시킨다")
        fun shouldThrowErrorForBlankSymbol() = runTest {
            // When & Then
            assertThatThrownBy {
                runBlocking {
                    ufc.quoteSummary("", QuoteSummaryModule.PRICE)
                }
            }
                .isInstanceOf(UfcException::class.java)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SYMBOL)
        }

        @Test
        @DisplayName("존재하지 않는 심볼 조회 시 적절한 에러를 발생시킨다")
        fun shouldThrowErrorForNonExistentSymbol() = runTest {
            // Given: 존재하지 않는 심볼
            val invalidSymbol = "INVALIDXXX"

            // When & Then
            val exception = try {
                ufc.quoteSummary(invalidSymbol, QuoteSummaryModule.PRICE)
                null
            } catch (e: UfcException) {
                e
            }

            assertThat(exception)
                .withFailMessage("존재하지 않는 심볼 조회 시 UfcException이 발생해야 합니다")
                .isNotNull

            assertThat(exception!!.errorCode)
                .withFailMessage("에러 코드가 적절해야 합니다")
                .isIn(
                    ErrorCode.DATA_NOT_FOUND,
                    ErrorCode.EXTERNAL_API_ERROR
                )
        }
    }
}
