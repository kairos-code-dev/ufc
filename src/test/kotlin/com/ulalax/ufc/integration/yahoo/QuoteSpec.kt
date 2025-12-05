package com.ulalax.ufc.integration.yahoo

import com.ulalax.ufc.integration.utils.IntegrationTestBase
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Quote API Integration Test
 *
 * 실제 Yahoo Finance Quote API를 호출하여 실시간 시장 데이터를 조회하는 통합 테스트입니다.
 *
 * ## 거래일/비거래일 동작
 * - **거래일 (장중)**: price, volume, change, changePercent가 실시간으로 변동
 * - **거래일 (장외)**: 종가 기준으로 고정, postMarket/preMarket 데이터 제공
 * - **휴장일**: 전일 종가 기준 데이터, change/changePercent는 0 또는 전일 대비
 */
@DisplayName("[I] Yahoo.quote() - 실시간 시장 데이터 조회")
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QuoteSpec : IntegrationTestBase() {

    @Test
    fun `returns quote data for AAPL`() = integrationTest {
        // given
        val symbol = "AAPL"

        // when
        val quote = ufc.quote(symbol)

        // then - Identification
        quote.identification.shouldNotBeNull()
        quote.identification!!.symbol shouldBe "AAPL"
        quote.identification!!.longName.shouldNotBeNull()
        quote.identification!!.exchange.shouldNotBeNull()
        quote.identification!!.quoteType shouldBe "EQUITY"
        quote.identification!!.currency shouldBe "USD"

        // then - Pricing
        quote.pricing.shouldNotBeNull()
        quote.pricing!!.price shouldNotBe 0.0
        quote.pricing!!.open.shouldNotBeNull()
        quote.pricing!!.dayHigh.shouldNotBeNull()
        quote.pricing!!.dayLow.shouldNotBeNull()
        quote.pricing!!.volume.shouldNotBeNull()
        quote.pricing!!.previousClose.shouldNotBeNull()
        quote.pricing!!.marketTime.shouldNotBeNull()

        // then - 52-Week
        quote.fiftyTwoWeek.shouldNotBeNull()
        quote.fiftyTwoWeek!!.high.shouldNotBeNull()
        quote.fiftyTwoWeek!!.low.shouldNotBeNull()
        quote.fiftyTwoWeek!!.range.shouldNotBeNull()

        // then - Moving Averages
        quote.movingAverages.shouldNotBeNull()
        quote.movingAverages!!.fiftyDayAverage.shouldNotBeNull()
        quote.movingAverages!!.twoHundredDayAverage.shouldNotBeNull()

        // then - Volumes
        quote.volumes.shouldNotBeNull()
        quote.volumes!!.averageDailyVolume3Month.shouldNotBeNull()

        // then - Market Cap
        quote.marketCap.shouldNotBeNull()
        quote.marketCap!!.marketCap.shouldNotBeNull()
        quote.marketCap!!.sharesOutstanding.shouldNotBeNull()

        // then - Dividends (AAPL은 배당주)
        quote.dividends.shouldNotBeNull()
        quote.dividends!!.annualRate.shouldNotBeNull()
        quote.dividends!!.yield.shouldNotBeNull()

        // then - Financial Ratios
        quote.financialRatios.shouldNotBeNull()
        quote.financialRatios!!.trailingPE.shouldNotBeNull()
        quote.financialRatios!!.forwardPE.shouldNotBeNull()
        quote.financialRatios!!.priceToBook.shouldNotBeNull()

        // then - Earnings
        quote.earnings.shouldNotBeNull()
        quote.earnings!!.epsTrailingTwelveMonths.shouldNotBeNull()

        // Note: Quote API (/v7/finance/quote)는 기본 시장 데이터만 반환합니다.
        // 다음 필드들(revenue, financialHealth, growthRates, analystRatings)은
        // 반환되지 않을 수 있습니다. 상세 재무 데이터가 필요하면 quoteSummary API를 사용하세요.
    }

    @Test
    fun `returns quote data for multiple symbols`() = integrationTest {
        // given
        val symbols = listOf("AAPL", "MSFT", "GOOGL")

        // when
        val quotes = ufc.quote(symbols)

        // then
        quotes shouldHaveSize 3

        // then - 각 심볼 확인
        val appleQuote = quotes.find { it.identification?.symbol == "AAPL" }
        appleQuote.shouldNotBeNull()
        appleQuote.pricing.shouldNotBeNull()
        appleQuote.pricing!!.price shouldNotBe 0.0

        val msftQuote = quotes.find { it.identification?.symbol == "MSFT" }
        msftQuote.shouldNotBeNull()
        msftQuote.pricing.shouldNotBeNull()
        msftQuote.pricing!!.price shouldNotBe 0.0

        val googlQuote = quotes.find { it.identification?.symbol == "GOOGL" }
        googlQuote.shouldNotBeNull()
        googlQuote.pricing.shouldNotBeNull()
        googlQuote.pricing!!.price shouldNotBe 0.0
    }

    @Test
    fun `returns quote data with core market fields for AAPL`() = integrationTest {
        // given
        val symbol = "AAPL"

        // when
        val quote = ufc.quote(symbol)

        // then - Quote API에서 제공하는 핵심 카테고리 검증
        // Note: Quote API (/v7/finance/quote)는 기본 시장 데이터만 반환합니다.
        // revenue, financialHealth, growthRates, analystRatings 등은 반환되지 않습니다.
        // 이러한 상세 재무 데이터가 필요하면 quoteSummary API를 사용하세요.
        quote.identification.shouldNotBeNull()
        quote.pricing.shouldNotBeNull()
        quote.fiftyTwoWeek.shouldNotBeNull()
        quote.movingAverages.shouldNotBeNull()
        quote.volumes.shouldNotBeNull()
        quote.marketCap.shouldNotBeNull()
        quote.dividends.shouldNotBeNull()
        quote.financialRatios.shouldNotBeNull()
        quote.earnings.shouldNotBeNull()

        // then - Identification 상세 검증
        quote.identification!!.symbol shouldBe "AAPL"
        quote.identification!!.longName.shouldNotBeNull()
        quote.identification!!.shortName.shouldNotBeNull()
        quote.identification!!.exchange.shouldNotBeNull()
        quote.identification!!.timezoneName.shouldNotBeNull()
        quote.identification!!.quoteType shouldBe "EQUITY"
        quote.identification!!.currency shouldBe "USD"

        // then - Pricing 상세 검증
        quote.pricing!!.price shouldNotBe 0.0
        quote.pricing!!.open.shouldNotBeNull()
        quote.pricing!!.dayHigh.shouldNotBeNull()
        quote.pricing!!.dayLow.shouldNotBeNull()
        quote.pricing!!.volume.shouldNotBeNull()
        quote.pricing!!.volume!! shouldNotBe 0L
        quote.pricing!!.previousClose.shouldNotBeNull()
        quote.pricing!!.change.shouldNotBeNull()
        quote.pricing!!.changePercent.shouldNotBeNull()
        quote.pricing!!.marketTime.shouldNotBeNull()

        // then - 52-Week 상세 검증
        quote.fiftyTwoWeek!!.high.shouldNotBeNull()
        quote.fiftyTwoWeek!!.low.shouldNotBeNull()
        quote.fiftyTwoWeek!!.highChange.shouldNotBeNull()
        quote.fiftyTwoWeek!!.lowChange.shouldNotBeNull()
        quote.fiftyTwoWeek!!.highChangePercent.shouldNotBeNull()
        quote.fiftyTwoWeek!!.lowChangePercent.shouldNotBeNull()
        quote.fiftyTwoWeek!!.range.shouldNotBeNull()

        // then - Moving Averages 상세 검증
        quote.movingAverages!!.fiftyDayAverage.shouldNotBeNull()
        quote.movingAverages!!.fiftyDayChange.shouldNotBeNull()
        quote.movingAverages!!.fiftyDayChangePercent.shouldNotBeNull()
        quote.movingAverages!!.twoHundredDayAverage.shouldNotBeNull()
        quote.movingAverages!!.twoHundredDayChange.shouldNotBeNull()
        quote.movingAverages!!.twoHundredDayChangePercent.shouldNotBeNull()

        // then - Volumes 상세 검증
        quote.volumes!!.averageDailyVolume3Month.shouldNotBeNull()
        quote.volumes!!.averageDailyVolume10Day.shouldNotBeNull()

        // then - Market Cap 상세 검증
        quote.marketCap!!.marketCap.shouldNotBeNull()
        quote.marketCap!!.marketCap!! shouldNotBe 0L
        quote.marketCap!!.sharesOutstanding.shouldNotBeNull()
        quote.marketCap!!.sharesOutstanding!! shouldNotBe 0L

        // then - Dividends 상세 검증 (AAPL은 배당주)
        quote.dividends!!.annualRate.shouldNotBeNull()
        quote.dividends!!.yield.shouldNotBeNull()
        quote.dividends!!.trailingRate.shouldNotBeNull()
        quote.dividends!!.trailingYield.shouldNotBeNull()

        // then - Financial Ratios 상세 검증
        quote.financialRatios!!.trailingPE.shouldNotBeNull()
        quote.financialRatios!!.forwardPE.shouldNotBeNull()
        quote.financialRatios!!.priceToBook.shouldNotBeNull()
        quote.financialRatios!!.bookValue.shouldNotBeNull()

        // then - Earnings 상세 검증
        quote.earnings!!.epsTrailingTwelveMonths.shouldNotBeNull()
        quote.earnings!!.epsForward.shouldNotBeNull()
        quote.earnings!!.epsCurrentYear.shouldNotBeNull()
    }

    @Test
    fun `returns ETF quote data for SPY`() = integrationTest {
        // given
        val symbol = "SPY"

        // when
        val quote = ufc.quote(symbol)

        // then - Identification
        quote.identification.shouldNotBeNull()
        quote.identification!!.symbol shouldBe "SPY"
        quote.identification!!.quoteType shouldBe "ETF"

        // then - Pricing
        quote.pricing.shouldNotBeNull()
        quote.pricing!!.price shouldNotBe 0.0

        // then - Market Cap (ETF도 시가총액 정보 제공)
        quote.marketCap.shouldNotBeNull()

        // then - Dividends (SPY는 배당 ETF)
        quote.dividends.shouldNotBeNull()
    }
}
