package examples

import com.ulalax.ufc.client.UFC
import com.ulalax.ufc.client.UFCClientConfig
import com.ulalax.ufc.model.common.Period
import com.ulalax.ufc.model.common.Interval
import kotlinx.coroutines.runBlocking

/**
 * UFC 네임스페이스 API 사용 예제
 *
 * 2차 개발에서 추가된 네임스페이스 기반 API의 사용법을 보여줍니다.
 */
fun main() = runBlocking {
    // UFC 클라이언트 생성
    val ufc = UFC.create(
        UFCClientConfig(
            fredApiKey = System.getenv("FRED_API_KEY")
        )
    )

    try {
        println("=== UFC Namespace API Examples ===\n")

        // 1. Price API - 가격 정보
        println("1. Price API")
        println("-------------")
        val price = ufc.price.getCurrentPrice("AAPL")
        println("Symbol: ${price.symbol}")
        println("Last Price: ${price.lastPrice} ${price.currency}")
        println("Market Cap: ${price.marketCap}")
        println()

        // 가격 히스토리
        val history = ufc.price.getPriceHistory("AAPL", Period.OneMonth, Interval.OneDay)
        println("Price History (last 5 days):")
        history.takeLast(5).forEach { ohlcv ->
            println("  ${ohlcv.timestamp}: Open=${ohlcv.open}, Close=${ohlcv.close}, Volume=${ohlcv.volume}")
        }
        println()

        // 2. Stock API - 주식 기본 정보
        println("2. Stock API")
        println("-------------")
        val companyInfo = ufc.stock.getCompanyInfo("AAPL")
        println("Company: ${companyInfo.longName}")
        println("Sector: ${companyInfo.sector}")
        println("Industry: ${companyInfo.industry}")
        println("Country: ${companyInfo.country}")
        println()

        // ISIN 조회
        val isin = ufc.stock.getIsin("AAPL")
        println("ISIN: $isin")
        println()

        // FastInfo 조회
        val fastInfo = ufc.stock.getFastInfo("AAPL")
        println("FastInfo: ${fastInfo.symbol} (${fastInfo.exchange}) - ${fastInfo.quoteType}")
        println()

        // 3. Funds API - 펀드 정보
        println("3. Funds API")
        println("-------------")
        val fundData = ufc.funds.getFundData("SPY")
        println("Fund: ${fundData.symbol} (${fundData.quoteType})")
        println("Description: ${fundData.description}")
        println("Category: ${fundData.categoryName}")
        println()

        println("Top Holdings (first 5):")
        fundData.topHoldings.take(5).forEach { holding ->
            println("  ${holding.symbol}: ${holding.holdingPercent}%")
        }
        println()

        // Asset Classes
        fundData.assetClasses?.let { assets ->
            println("Asset Classes:")
            println("  Cash: ${assets.cash}")
            println("  Stocks: ${assets.stocks}")
            println("  Bonds: ${assets.bonds}")
            println("  Other: ${assets.other}")
        }
        println()

        // 4. Corp API - 기업 행동
        println("4. Corp API")
        println("-------------")
        val dividends = ufc.corp.getDividends("AAPL", Period.OneYear)
        println("Dividends (last 5):")
        dividends.dividends.takeLast(5).forEach { div ->
            println("  ${div.date}: ${div.amount} USD")
        }
        println()

        // 주식분할 조회
        val splits = ufc.corp.getSplits("AAPL", Period.Max)
        if (splits.splits.isNotEmpty()) {
            println("Stock Splits:")
            splits.splits.takeLast(3).forEach { split ->
                println("  ${split.date}: ${split.numerator}:${split.denominator}")
            }
        } else {
            println("No stock splits found")
        }
        println()

        // 5. Macro API - 거시경제 지표 (FRED API 키 필요)
        val macro = ufc.macro
        if (macro != null) {
            println("5. Macro API")
            println("-------------")
            val gdp = macro.getGDP()
            println("GDP Series: ${gdp.seriesId}")
            println("Latest GDP observations (last 3):")
            gdp.observations.takeLast(3).forEach { obs ->
                println("  ${obs.date}: ${obs.value}")
            }
            println()

            val unemployment = macro.getUnemploymentRate()
            println("Unemployment Rate: ${unemployment.seriesId}")
            println("Latest observation: ${unemployment.observations.lastOrNull()?.let { "${it.date}: ${it.value}%" }}")
        } else {
            println("5. Macro API")
            println("-------------")
            println("Macro API is not available (FRED API key not provided)")
        }
        println()

        // 다중 심볼 조회 예제
        println("6. Batch Operations")
        println("-------------------")
        val symbols = listOf("AAPL", "GOOGL", "MSFT")
        val prices = ufc.price.getCurrentPrice(symbols)
        println("Current Prices:")
        prices.forEach { (symbol, priceData) ->
            println("  $symbol: ${priceData.lastPrice} ${priceData.currency}")
        }
        println()

        val companies = ufc.stock.getCompanyInfo(symbols)
        println("Company Info:")
        companies.forEach { (symbol, info) ->
            println("  $symbol: ${info.longName} (${info.sector})")
        }
        println()

        println("=== All Examples Completed Successfully ===")

    } catch (e: Exception) {
        println("Error occurred: ${e.message}")
        e.printStackTrace()
    } finally {
        ufc.close()
        println("\nUFC client closed")
    }
}
