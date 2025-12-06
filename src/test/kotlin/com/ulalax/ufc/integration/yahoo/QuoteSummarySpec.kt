package com.ulalax.ufc.integration.yahoo

import com.ulalax.ufc.domain.model.chart.*
import com.ulalax.ufc.domain.model.quote.*
import com.ulalax.ufc.fixture.TestFixtures
import com.ulalax.ufc.integration.utils.IntegrationTestBase
import com.ulalax.ufc.integration.utils.RecordingConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * YahooClient.quoteSummary() API Integration 테스트
 *
 * 이 테스트는 실제 Yahoo Finance API를 호출하여 QuoteSummary 기능을 검증합니다.
 * API 가이드처럼 읽힐 수 있도록 @Nested 그룹핑 패턴을 사용합니다.
 *
 * ## 테스트 실행 방법
 * ```bash
 * # 특정 클래스 실행
 * ./gradlew test --tests 'QuoteSummarySpec'
 *
 * # 특정 그룹 실행
 * ./gradlew test --tests 'QuoteSummarySpec$BasicBehavior'
 * ```
 */
@DisplayName("[I] Yahoo.quoteSummary() - 주식 요약 정보 조회")
class QuoteSummarySpec : IntegrationTestBase() {
    @Nested
    @DisplayName("기본 동작")
    inner class BasicBehavior {
        @Test
        @DisplayName("AAPL의 PRICE 모듈을 조회할 수 있다")
        fun `returns price module for AAPL`() =
            integrationTest(
                RecordingConfig.Paths.Yahoo.QUOTE_SUMMARY,
                "aapl_price",
            ) {
                // Given
                val symbol = TestFixtures.Symbols.AAPL

                // When
                val result = ufc.yahoo.quoteSummary(symbol, QuoteSummaryModule.PRICE)

                // Then
                assertThat(result).isNotNull()
                assertThat(result.hasModule(QuoteSummaryModule.PRICE)).isTrue()
            }

        @Test
        @DisplayName("MSFT의 SUMMARY_DETAIL 모듈을 조회할 수 있다")
        fun `returns summary detail module for MSFT`() =
            integrationTest(
                RecordingConfig.Paths.Yahoo.QUOTE_SUMMARY,
                "msft_summary_detail",
            ) {
                // Given
                val symbol = TestFixtures.Symbols.MSFT

                // When
                val result = ufc.yahoo.quoteSummary(symbol, QuoteSummaryModule.SUMMARY_DETAIL)

                // Then
                assertThat(result).isNotNull()
                assertThat(result.hasModule(QuoteSummaryModule.SUMMARY_DETAIL)).isTrue()
            }
    }

    @Nested
    @DisplayName("응답 데이터 스펙")
    inner class ResponseSpec {
        @Test
        @DisplayName("요청한 모듈이 응답에 포함된다")
        fun `requested modules are included in response`() =
            integrationTest {
                // Given
                val symbol = TestFixtures.Symbols.AAPL

                // When
                val result = ufc.yahoo.quoteSummary(symbol, QuoteSummaryModule.PRICE)

                // Then
                assertThat(result.hasModule(QuoteSummaryModule.PRICE)).isTrue()
            }

        @Test
        @DisplayName("여러 모듈을 요청하면 모두 응답에 포함된다")
        fun `multiple requested modules are all included`() =
            integrationTest {
                // Given
                val symbol = TestFixtures.Symbols.AAPL

                // When
                val result =
                    ufc.yahoo.quoteSummary(
                        symbol,
                        QuoteSummaryModule.PRICE,
                        QuoteSummaryModule.SUMMARY_DETAIL,
                    )

                // Then
                assertThat(result.hasModule(QuoteSummaryModule.PRICE)).isTrue()
                assertThat(result.hasModule(QuoteSummaryModule.SUMMARY_DETAIL)).isTrue()
            }
    }

    @Nested
    @DisplayName("활용 예제")
    inner class UsageExamples {
        @Test
        @DisplayName("여러 모듈을 한번에 조회할 수 있다")
        fun `can request multiple modules at once`() =
            integrationTest {
                // Given
                val symbol = TestFixtures.Symbols.AAPL
                val modules =
                    setOf(
                        QuoteSummaryModule.PRICE,
                        QuoteSummaryModule.SUMMARY_DETAIL,
                        QuoteSummaryModule.FINANCIAL_DATA,
                    )

                // When
                val result = ufc.yahoo.quoteSummary(symbol, modules)

                // Then
                assertThat(result).isNotNull()
                assertThat(result.hasModule(QuoteSummaryModule.PRICE)).isTrue()
                assertThat(result.hasModule(QuoteSummaryModule.SUMMARY_DETAIL)).isTrue()
                assertThat(result.hasModule(QuoteSummaryModule.FINANCIAL_DATA)).isTrue()
            }

        @Test
        @DisplayName("주식의 주요 정보를 한번에 조회할 수 있다")
        fun `can fetch stock key information at once`() =
            integrationTest {
                // Given
                val symbol = TestFixtures.Symbols.TSLA
                val stockModules = QuoteSummaryModule.stockModules()

                // When
                val result = ufc.yahoo.quoteSummary(symbol, stockModules)

                // Then
                assertThat(result).isNotNull()
                assertThat(result.hasModule(QuoteSummaryModule.PRICE)).isTrue()
            }
    }

    @Nested
    @DisplayName("사용 가능한 모듈")
    inner class AvailableModules {
        @Test
        @DisplayName("QuoteSummaryModule enum에는 36개의 모듈이 정의되어 있다")
        fun `has 36 modules defined`() =
            integrationTest {
                // Given
                val allModules = QuoteSummaryModule.entries

                // Then
                assertThat(allModules).hasSize(36)
            }

        @Test
        @DisplayName("주요 모듈 카테고리를 확인할 수 있다")
        fun `can identify major module categories`() =
            integrationTest {
                // 가격/시세 모듈
                val priceModules =
                    listOf(
                        QuoteSummaryModule.PRICE,
                        QuoteSummaryModule.SUMMARY_DETAIL,
                    )

                // 기업 정보 모듈
                val companyInfoModules =
                    listOf(
                        QuoteSummaryModule.ASSET_PROFILE,
                        QuoteSummaryModule.QUOTE_TYPE,
                    )

                // 재무 모듈
                val financialModules =
                    listOf(
                        QuoteSummaryModule.FINANCIAL_DATA,
                        QuoteSummaryModule.DEFAULT_KEY_STATISTICS,
                    )

                // 실적 모듈
                val earningsModules =
                    listOf(
                        QuoteSummaryModule.EARNINGS,
                        QuoteSummaryModule.EARNINGS_TREND,
                        QuoteSummaryModule.EARNINGS_HISTORY,
                    )

                // 주주/내부자 모듈
                val holderModules =
                    listOf(
                        QuoteSummaryModule.MAJOR_HOLDERS,
                        QuoteSummaryModule.INSIDER_TRANSACTIONS,
                    )

                // 펀드 모듈
                val fundModules =
                    listOf(
                        QuoteSummaryModule.TOP_HOLDINGS,
                        QuoteSummaryModule.FUND_PROFILE,
                        QuoteSummaryModule.FUND_PERFORMANCE,
                    )

                // 재무제표 모듈
                val statementModules =
                    listOf(
                        QuoteSummaryModule.INCOME_STATEMENT_HISTORY,
                        QuoteSummaryModule.BALANCE_SHEET_HISTORY,
                        QuoteSummaryModule.CASHFLOW_STATEMENT_HISTORY,
                    )

                // Then - 모든 모듈이 존재하는지 확인
                val allModules = QuoteSummaryModule.entries
                assertThat(allModules).containsAll(priceModules)
                assertThat(allModules).containsAll(companyInfoModules)
                assertThat(allModules).containsAll(financialModules)
                assertThat(allModules).containsAll(earningsModules)
                assertThat(allModules).containsAll(holderModules)
                assertThat(allModules).containsAll(fundModules)
                assertThat(allModules).containsAll(statementModules)
            }
    }

    @Nested
    @DisplayName("모듈 조합 방법")
    inner class ModuleCombinations {
        @Test
        @DisplayName("단일 모듈만 조회할 수 있다")
        fun `can request single module`() =
            integrationTest {
                // Given
                val symbol = TestFixtures.Symbols.AAPL

                // When
                val result = ufc.yahoo.quoteSummary(symbol, QuoteSummaryModule.PRICE)

                // Then
                assertThat(result.requestedModules).hasSize(1)
                assertThat(result.hasModule(QuoteSummaryModule.PRICE)).isTrue()
            }

        @Test
        @DisplayName("여러 모듈을 동시에 조회할 수 있다")
        fun `can request multiple modules at once`() =
            integrationTest {
                // Given
                val symbol = TestFixtures.Symbols.AAPL
                val modules =
                    setOf(
                        QuoteSummaryModule.PRICE,
                        QuoteSummaryModule.SUMMARY_DETAIL,
                        QuoteSummaryModule.FINANCIAL_DATA,
                    )

                // When
                val result = ufc.yahoo.quoteSummary(symbol, modules)

                // Then
                assertThat(result.requestedModules).hasSize(3)
                assertThat(result.hasModule(QuoteSummaryModule.PRICE)).isTrue()
                assertThat(result.hasModule(QuoteSummaryModule.SUMMARY_DETAIL)).isTrue()
                assertThat(result.hasModule(QuoteSummaryModule.FINANCIAL_DATA)).isTrue()
            }

        @Test
        @DisplayName("stockModules() 프리셋을 사용할 수 있다")
        fun `can use stockModules preset`() =
            integrationTest {
                // Given
                val symbol = TestFixtures.Symbols.AAPL
                val stockModules = QuoteSummaryModule.stockModules()

                // When
                val result = ufc.yahoo.quoteSummary(symbol, stockModules)

                // Then
                assertThat(result.requestedModules).isEqualTo(stockModules)
                assertThat(result.hasModule(QuoteSummaryModule.PRICE)).isTrue()
                assertThat(result.hasModule(QuoteSummaryModule.SUMMARY_DETAIL)).isTrue()
                assertThat(result.hasModule(QuoteSummaryModule.ASSET_PROFILE)).isTrue()
                assertThat(result.hasModule(QuoteSummaryModule.FINANCIAL_DATA)).isTrue()
            }

        @Test
        @DisplayName("fundModules() 프리셋을 사용할 수 있다 (SPY ETF)")
        fun `can use fundModules preset for ETF`() =
            integrationTest(
                RecordingConfig.Paths.Yahoo.QUOTE_SUMMARY,
                "spy_fund_modules",
            ) {
                // Given
                val symbol = "SPY" // S&P 500 ETF
                val fundModules = QuoteSummaryModule.fundModules()

                // When
                val result = ufc.yahoo.quoteSummary(symbol, fundModules)

                // Then
                assertThat(result.requestedModules).isEqualTo(fundModules)
                assertThat(result.hasModule(QuoteSummaryModule.PRICE)).isTrue()
                assertThat(result.hasModule(QuoteSummaryModule.SUMMARY_DETAIL)).isTrue()
                assertThat(result.hasModule(QuoteSummaryModule.TOP_HOLDINGS)).isTrue()
            }
    }

    @Nested
    @DisplayName("모듈별 데이터 스펙")
    inner class ModuleDataSpec {
        @Nested
        @DisplayName("PRICE 모듈")
        inner class PriceModuleSpec {
            @Test
            @DisplayName("현재 가격 정보를 조회할 수 있다")
            fun `can get current price information`() =
                integrationTest {
                    // Given
                    val symbol = TestFixtures.Symbols.AAPL

                    // When - 여러 모듈을 함께 조회하여 PRICE 데이터 확인
                    val result =
                        ufc.yahoo.quoteSummary(
                            symbol,
                            QuoteSummaryModule.PRICE,
                            QuoteSummaryModule.SUMMARY_DETAIL,
                        )
                    val price: Price? = result.getModule(QuoteSummaryModule.PRICE)

                    // Then
                    assertThat(price).isNotNull
                    price!!

                    // 현재가
                    assertThat(price.regularMarketPrice?.doubleValue)
                        .isNotNull()
                        .isGreaterThan(0.0)

                    // 변동률 (선택적 필드)
                    // regularMarketChangePercent는 항상 제공되지 않을 수 있음

                    // 종목명
                    assertThat(price.longName).isNotNull().isNotBlank()
                    assertThat(price.shortName).isNotNull().isNotBlank()

                    // 통화
                    assertThat(price.currency).isNotNull().isEqualTo("USD")

                    // 심볼
                    assertThat(price.symbol).isNotNull().isEqualTo(symbol)

                    // 거래소
                    assertThat(price.exchange).isNotNull().isNotBlank()
                }
        }

        @Nested
        @DisplayName("SUMMARY_DETAIL 모듈")
        inner class SummaryDetailModuleSpec {
            @Test
            @DisplayName("주식의 상세 정보를 조회할 수 있다")
            fun `can get stock summary details`() =
                integrationTest {
                    // Given
                    val symbol = TestFixtures.Symbols.AAPL

                    // When
                    val result = ufc.yahoo.quoteSummary(symbol, QuoteSummaryModule.SUMMARY_DETAIL)
                    val detail: SummaryDetail? = result.getModule(QuoteSummaryModule.SUMMARY_DETAIL)

                    // Then
                    assertThat(detail).isNotNull
                    detail!!

                    // 시가총액
                    assertThat(detail.marketCap?.doubleValue)
                        .isNotNull()
                        .isGreaterThan(0.0)

                    // PER (주가수익비율)
                    assertThat(detail.trailingPE?.doubleValue).isNotNull()

                    // 거래량
                    assertThat(detail.averageVolume?.longValue)
                        .isNotNull()
                        .isGreaterThan(0)

                    // 52주 고가/저가
                    assertThat(detail.fiftyTwoWeekHigh?.doubleValue).isNotNull()
                    assertThat(detail.fiftyTwoWeekLow?.doubleValue).isNotNull()

                    // 베타
                    assertThat(detail.beta?.doubleValue).isNotNull()
                }
        }

        @Nested
        @DisplayName("FINANCIAL_DATA 모듈")
        inner class FinancialDataModuleSpec {
            @Test
            @DisplayName("재무 정보를 조회할 수 있다")
            fun `can get financial data`() =
                integrationTest {
                    // Given
                    val symbol = TestFixtures.Symbols.AAPL

                    // When
                    val result = ufc.yahoo.quoteSummary(symbol, QuoteSummaryModule.FINANCIAL_DATA)
                    val financial: FinancialData? = result.getModule(QuoteSummaryModule.FINANCIAL_DATA)

                    // Then
                    assertThat(financial).isNotNull
                    financial!!

                    // ROE (자기자본이익률)
                    assertThat(financial.returnOnEquity?.doubleValue).isNotNull()

                    // ROA (자산이익률)
                    assertThat(financial.returnOnAssets?.doubleValue).isNotNull()

                    // 현금흐름
                    assertThat(financial.operatingCashflow?.doubleValue).isNotNull()

                    // 잉여 현금흐름
                    assertThat(financial.freeCashflow?.doubleValue).isNotNull()

                    // 총 부채
                    assertThat(financial.totalDebt?.doubleValue).isNotNull()

                    // 총 현금
                    assertThat(financial.totalCash?.doubleValue).isNotNull()

                    // 애널리스트 추천 (항상 제공되는 것은 아님)
                    // targetPriceMean, numberOfAnalysts는 선택적 필드
                }
        }

        @Nested
        @DisplayName("ASSET_PROFILE 모듈")
        inner class AssetProfileModuleSpec {
            @Test
            @DisplayName("기업 프로필 정보를 조회할 수 있다")
            fun `can get asset profile information`() =
                integrationTest {
                    // Given
                    val symbol = TestFixtures.Symbols.AAPL

                    // When
                    val result = ufc.yahoo.quoteSummary(symbol, QuoteSummaryModule.ASSET_PROFILE)
                    val profile: AssetProfile? = result.getModule(QuoteSummaryModule.ASSET_PROFILE)

                    // Then
                    assertThat(profile).isNotNull
                    profile!!

                    // 섹터와 산업
                    assertThat(profile.sector).isNotNull().isNotBlank()
                    assertThat(profile.industry).isNotNull().isNotBlank()

                    // 회사 소개
                    assertThat(profile.longBusinessSummary).isNotNull().isNotBlank()

                    // 웹사이트
                    assertThat(profile.website).isNotNull().isNotBlank()

                    // 국가와 도시
                    assertThat(profile.country).isNotNull().isNotBlank()
                    assertThat(profile.city).isNotNull().isNotBlank()

                    // 직원 수
                    assertThat(profile.fullTimeEmployees)
                        .isNotNull()
                        .isGreaterThan(0)
                }
        }

        @Nested
        @DisplayName("QUOTE_TYPE 모듈")
        inner class QuoteTypeModuleSpec {
            @Test
            @DisplayName("자산 타입 정보를 조회할 수 있다")
            fun `can get quote type information`() =
                integrationTest {
                    // Given
                    val symbol = TestFixtures.Symbols.AAPL

                    // When - 여러 모듈을 함께 조회하여 QUOTE_TYPE 데이터 확인
                    val result =
                        ufc.yahoo.quoteSummary(
                            symbol,
                            QuoteSummaryModule.QUOTE_TYPE,
                            QuoteSummaryModule.PRICE,
                        )
                    val quoteType: QuoteType? = result.getModule(QuoteSummaryModule.QUOTE_TYPE)

                    // Then
                    assertThat(quoteType).isNotNull
                    quoteType!!

                    // 자산 타입 (EQUITY, ETF, MUTUALFUND 등)
                    assertThat(quoteType.quoteType).isNotNull().isEqualTo("EQUITY")

                    // 거래소
                    assertThat(quoteType.exchange).isNotNull().isNotBlank()

                    // 심볼과 이름
                    assertThat(quoteType.symbol).isNotNull().isEqualTo(symbol)
                    assertThat(quoteType.shortName).isNotNull().isNotBlank()
                    assertThat(quoteType.longName).isNotNull().isNotBlank()

                    // 주의: market, sector, industry는 항상 제공되지 않을 수 있음
                    // sector와 industry는 주로 ASSET_PROFILE 모듈에서 제공됨
                }
        }

        @Nested
        @DisplayName("EARNINGS_TREND 모듈")
        inner class EarningsTrendModuleSpec {
            @Test
            @DisplayName("실적 추이 정보를 조회할 수 있다")
            fun `can get earnings trend information`() =
                integrationTest {
                    // Given
                    val symbol = TestFixtures.Symbols.AAPL

                    // When
                    val result = ufc.yahoo.quoteSummary(symbol, QuoteSummaryModule.EARNINGS_TREND)
                    val earningsTrend: EarningsTrend? = result.getModule(QuoteSummaryModule.EARNINGS_TREND)

                    // Then
                    assertThat(earningsTrend).isNotNull
                    earningsTrend!!

                    // 추세 데이터가 있는지 확인
                    assertThat(earningsTrend.trend).isNotNull().isNotEmpty()

                    // 첫 번째 추세 데이터 검증
                    val firstTrend = earningsTrend.trend!!.first()
                    assertThat(firstTrend.period).isNotNull().isNotBlank()
                }
        }

        @Nested
        @DisplayName("MAJOR_HOLDERS 모듈")
        inner class MajorHoldersModuleSpec {
            @Test
            @DisplayName("주요 주주 정보는 Yahoo API에서 지원하지 않을 수 있다")
            fun `major holders module may not be supported`() =
                integrationTest {
                    // Given
                    val symbol = TestFixtures.Symbols.AAPL

                    // When & Then - MAJOR_HOLDERS 모듈은 현재 Yahoo Finance API에서 HTTP 404를 반환함
                    // 이는 API가 더 이상 이 모듈을 지원하지 않거나 접근 방법이 변경되었음을 의미
                    val result =
                        runCatching {
                            ufc.yahoo.quoteSummary(symbol, QuoteSummaryModule.MAJOR_HOLDERS)
                        }

                    // 실패하거나, 성공하더라도 데이터가 없을 수 있음
                    assertThat(
                        result.isFailure || result.getOrNull()?.hasModule(QuoteSummaryModule.MAJOR_HOLDERS) == false,
                    ).isTrue()
                }
        }

        @Nested
        @DisplayName("TOP_HOLDINGS 모듈 (ETF)")
        inner class TopHoldingsModuleSpec {
            @Test
            @DisplayName("ETF의 상위 보유 종목을 조회할 수 있다")
            fun `can get top holdings for ETF`() =
                integrationTest {
                    // Given
                    val symbol = "SPY" // S&P 500 ETF

                    // When
                    val result = ufc.yahoo.quoteSummary(symbol, QuoteSummaryModule.TOP_HOLDINGS)
                    val topHoldings: TopHoldings? = result.getModule(QuoteSummaryModule.TOP_HOLDINGS)

                    // Then
                    assertThat(topHoldings).isNotNull
                    topHoldings!!

                    // 보유 종목 리스트가 있는지 확인
                    assertThat(topHoldings.holdings).isNotNull()

                    // 섹터 비중이 있는지 확인
                    assertThat(topHoldings.sectorWeightings).isNotNull()
                }
        }

        @Nested
        @DisplayName("여러 모듈 동시 조회")
        inner class MultipleModulesSpec {
            @Test
            @DisplayName("PRICE, SUMMARY_DETAIL, FINANCIAL_DATA를 한번에 조회할 수 있다")
            fun `can get multiple modules at once`() =
                integrationTest {
                    // Given
                    val symbol = TestFixtures.Symbols.AAPL
                    val modules =
                        setOf(
                            QuoteSummaryModule.PRICE,
                            QuoteSummaryModule.SUMMARY_DETAIL,
                            QuoteSummaryModule.FINANCIAL_DATA,
                        )

                    // When
                    val result = ufc.yahoo.quoteSummary(symbol, modules)

                    // Then
                    assertThat(result.hasModule(QuoteSummaryModule.PRICE)).isTrue()
                    assertThat(result.hasModule(QuoteSummaryModule.SUMMARY_DETAIL)).isTrue()
                    assertThat(result.hasModule(QuoteSummaryModule.FINANCIAL_DATA)).isTrue()

                    // 각 모듈의 데이터를 가져올 수 있다
                    val price: Price? = result.getModule(QuoteSummaryModule.PRICE)
                    val detail: SummaryDetail? = result.getModule(QuoteSummaryModule.SUMMARY_DETAIL)
                    val financial: FinancialData? = result.getModule(QuoteSummaryModule.FINANCIAL_DATA)

                    assertThat(price).isNotNull
                    assertThat(detail).isNotNull
                    assertThat(financial).isNotNull

                    // 각 모듈의 주요 데이터가 있는지 확인
                    assertThat(price!!.regularMarketPrice?.doubleValue).isNotNull()
                    assertThat(detail!!.marketCap?.doubleValue).isNotNull()
                    assertThat(financial!!.returnOnEquity?.doubleValue).isNotNull()
                }
        }
    }

    @Nested
    @DisplayName("에러 케이스")
    inner class ErrorCases {
        @Test
        @DisplayName("존재하지 않는 심볼 조회 시 ApiException을 던진다")
        fun `throws ApiException for invalid symbol`() =
            integrationTest {
                // Given
                val invalidSymbol = TestFixtures.Symbols.INVALID

                // When & Then
                val result =
                    runCatching {
                        ufc.yahoo.quoteSummary(invalidSymbol, QuoteSummaryModule.PRICE)
                    }
                // Then
                assertThat(result.isFailure).isTrue()
            }
    }
}
