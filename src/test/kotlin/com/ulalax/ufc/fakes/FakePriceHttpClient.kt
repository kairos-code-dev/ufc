package com.ulalax.ufc.fakes

import com.ulalax.ufc.domain.chart.*
import com.ulalax.ufc.domain.price.PriceHttpClient
import com.ulalax.ufc.domain.quote.*
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.infrastructure.yahoo.response.ChartResponse
import com.ulalax.ufc.infrastructure.yahoo.response.PriceResponse
import com.ulalax.ufc.domain.common.Interval
import com.ulalax.ufc.domain.common.Period
import kotlinx.serialization.json.JsonPrimitive
import java.time.LocalDate

/**
 * 테스트용 PriceHttpClient Fake 구현체
 *
 * 인터페이스 덕분에 간단하게 구현 가능
 *
 * 특징:
 * - HTTP 호출 없이 미리 설정된 응답 반환
 * - 호출 횟수 추적 (캐싱 테스트용)
 * - 예외 시뮬레이션 가능
 *
 * 사용 예시:
 * ```kotlin
 * val fakeHttpClient = FakePriceHttpClient()
 *
 * // 응답 설정
 * fakeHttpClient.setQuoteSummaryResponse("AAPL", validPriceResponse)
 *
 * // Service에 주입
 * val service = RefactoredPriceService(fakeHttpClient, cache)
 *
 * // 호출 횟수 확인 (캐싱 테스트)
 * assertEquals(1, fakeHttpClient.quoteSummaryCallCount)
 * ```
 */
class FakePriceHttpClient : PriceHttpClient {

    private val quoteSummaryResponses = mutableMapOf<String, PriceResponse>()
    private val chartResponses = mutableMapOf<String, ChartResponse>()

    // 호출 횟수 추적
    var quoteSummaryCallCount = 0
        private set
    var chartCallCount = 0
        private set

    // 예외 시뮬레이션
    private var shouldThrowException: Exception? = null

    /**
     * QuoteSummary 응답 설정
     *
     * @param symbol 심볼
     * @param response 반환할 응답
     */
    fun setQuoteSummaryResponse(symbol: String, response: PriceResponse) {
        quoteSummaryResponses[symbol] = response
    }

    /**
     * Chart 응답 설정
     *
     * @param symbol 심볼
     * @param response 반환할 응답
     */
    fun setChartResponse(symbol: String, response: ChartResponse) {
        chartResponses[symbol] = response
    }

    /**
     * 예외 발생 설정 (모든 메서드에서 예외 발생)
     *
     * @param exception 발생시킬 예외
     */
    fun setException(exception: Exception) {
        shouldThrowException = exception
    }

    /**
     * 호출 횟수 초기화
     */
    fun resetCallCount() {
        quoteSummaryCallCount = 0
        chartCallCount = 0
    }

    /**
     * 모든 응답 초기화
     */
    fun clear() {
        quoteSummaryResponses.clear()
        chartResponses.clear()
        shouldThrowException = null
        resetCallCount()
    }

    // ============================================================================
    // PriceHttpClient Implementation
    // ============================================================================

    override suspend fun fetchQuoteSummary(
        symbol: String,
        modules: List<String>
    ): PriceResponse {
        quoteSummaryCallCount++

        shouldThrowException?.let { throw it }

        return quoteSummaryResponses[symbol]
            ?: throw UfcException(
                ErrorCode.DATA_NOT_FOUND,
                "Fake: QuoteSummary 응답이 설정되지 않았습니다: $symbol"
            )
    }

    override suspend fun fetchChart(
        symbol: String,
        interval: Interval,
        period: Period
    ): ChartResponse {
        chartCallCount++

        shouldThrowException?.let { throw it }

        return chartResponses[symbol]
            ?: throw UfcException(
                ErrorCode.DATA_NOT_FOUND,
                "Fake: Chart 응답이 설정되지 않았습니다: $symbol"
            )
    }

    override suspend fun fetchChartByDateRange(
        symbol: String,
        start: LocalDate,
        end: LocalDate,
        interval: Interval
    ): ChartResponse {
        chartCallCount++

        shouldThrowException?.let { throw it }

        return chartResponses[symbol]
            ?: throw UfcException(
                ErrorCode.DATA_NOT_FOUND,
                "Fake: Chart 응답이 설정되지 않았습니다: $symbol"
            )
    }

    override suspend fun fetchChartWithEvents(
        symbol: String,
        interval: Interval,
        period: Period,
        includeEvents: Boolean
    ): ChartResponse {
        chartCallCount++

        shouldThrowException?.let { throw it }

        return chartResponses[symbol]
            ?: throw UfcException(
                ErrorCode.DATA_NOT_FOUND,
                "Fake: Chart 응답이 설정되지 않았습니다: $symbol"
            )
    }
}

/**
 * 테스트용 QuoteSummary 응답 빌더
 *
 * 간단하게 유효한 QuoteSummaryResponse를 생성합니다.
 */
object TestQuoteSummaryResponseBuilder {

    /**
     * 기본 Price 응답 생성
     *
     * @param symbol 심볼
     * @param price 현재 가격
     * @param previousClose 전일 종가
     * @return QuoteSummaryResponse
     */
    fun createPriceResponse(
        symbol: String,
        price: Double,
        previousClose: Double? = null,
        currency: String = "USD",
        exchange: String = "NMS"
    ): QuoteSummaryResponse {
        return QuoteSummaryResponse(
            quoteSummary = QuoteSummary(
                result = listOf(
                    QuoteSummaryResult(
                        price = Price(
                            symbol = symbol,
                            currency = currency,
                            exchange = exchange,
                            regularMarketPrice = RawFormatted(
                                raw = kotlinx.serialization.json.JsonPrimitive(price),
                                fmt = price.toString()
                            ),
                            regularMarketPreviousClose = previousClose?.let {
                                RawFormatted(
                                    raw = kotlinx.serialization.json.JsonPrimitive(it),
                                    fmt = it.toString()
                                )
                            },
                            fiftyTwoWeekHigh = RawFormatted(
                                raw = kotlinx.serialization.json.JsonPrimitive(price * 1.2),
                                fmt = null
                            ),
                            fiftyTwoWeekLow = RawFormatted(
                                raw = kotlinx.serialization.json.JsonPrimitive(price * 0.8),
                                fmt = null
                            ),
                            fiftyTwoWeekChangePercent = RawFormatted(
                                raw = kotlinx.serialization.json.JsonPrimitive(0.15),
                                fmt = null
                            )
                        ),
                        summaryDetail = SummaryDetail(
                            regularMarketDayHigh = RawFormatted(
                                raw = kotlinx.serialization.json.JsonPrimitive(price * 1.01),
                                fmt = null
                            ),
                            regularMarketDayLow = RawFormatted(
                                raw = kotlinx.serialization.json.JsonPrimitive(price * 0.99),
                                fmt = null
                            ),
                            regularMarketVolume = RawFormatted(
                                raw = kotlinx.serialization.json.JsonPrimitive(1000000),
                                fmt = null
                            ),
                            marketCap = RawFormatted(
                                raw = kotlinx.serialization.json.JsonPrimitive(1000000000),
                                fmt = null
                            ),
                            averageVolume = RawFormatted(
                                raw = kotlinx.serialization.json.JsonPrimitive(900000),
                                fmt = null
                            ),
                            averageVolume10days = RawFormatted(
                                raw = kotlinx.serialization.json.JsonPrimitive(950000),
                                fmt = null
                            ),
                            beta = RawFormatted(
                                raw = kotlinx.serialization.json.JsonPrimitive(1.05),
                                fmt = null
                            ),
                            trailingPE = RawFormatted(
                                raw = kotlinx.serialization.json.JsonPrimitive(25.0),
                                fmt = null
                            ),
                            forwardPE = RawFormatted(
                                raw = kotlinx.serialization.json.JsonPrimitive(22.0),
                                fmt = null
                            ),
                            fiftyTwoWeekHigh = RawFormatted(
                                raw = kotlinx.serialization.json.JsonPrimitive(price * 1.2),
                                fmt = null
                            ),
                            fiftyTwoWeekLow = RawFormatted(
                                raw = kotlinx.serialization.json.JsonPrimitive(price * 0.8),
                                fmt = null
                            ),
                            dividendYield = RawFormatted(
                                raw = kotlinx.serialization.json.JsonPrimitive(0.015),
                                fmt = null
                            ),
                            dividendRate = RawFormatted(
                                raw = kotlinx.serialization.json.JsonPrimitive(0.88),
                                fmt = null
                            ),
                            exDividendDate = RawFormatted(
                                raw = kotlinx.serialization.json.JsonPrimitive(1704067200),
                                fmt = null
                            )
                        )
                    )
                ),
                error = null
            )
        )
    }

    /**
     * Stock 회사 정보 응답 생성
     *
     * @param symbol 심볼
     * @param longName 회사명
     * @return QuoteSummaryResponse
     */
    fun createCompanyInfoResponse(
        symbol: String,
        longName: String = "Apple Inc.",
        currency: String = "USD",
        exchange: String = "NASDAQ"
    ): QuoteSummaryResponse {
        return QuoteSummaryResponse(
            quoteSummary = QuoteSummary(
                result = listOf(
                    QuoteSummaryResult(
                        price = Price(
                            symbol = symbol,
                            longName = longName,
                            shortName = longName.split(" ").first(),
                            currency = currency
                        ),
                        quoteType = QuoteType(
                            symbol = symbol,
                            longName = longName,
                            shortName = longName.split(" ").first(),
                            exchange = exchange,
                            quoteType = "EQUITY",
                            market = currency
                        ),
                        assetProfile = AssetProfile(
                            address1 = "One Apple Park Way",
                            city = "Cupertino",
                            state = "CA",
                            zip = "95014",
                            country = "United States",
                            phone = "408-996-1010",
                            website = "https://www.apple.com",
                            industry = "Consumer Electronics",
                            sector = "Technology",
                            longBusinessSummary = "Apple Inc. designs, manufactures, and markets smartphones, personal computers, tablets, wearables, and accessories worldwide.",
                            fullTimeEmployees = 154000
                        ),
                        defaultKeyStatistics = DefaultKeyStatistics(
                            sharesOutstanding = RawFormatted(
                                raw = JsonPrimitive(15550061000L),
                                fmt = "15.55B"
                            ),
                            isin = "US0378331005"
                        )
                    )
                ),
                error = null
            )
        )
    }

    /**
     * Stock FastInfo 응답 생성
     *
     * @param symbol 심볼
     * @return QuoteSummaryResponse
     */
    fun createFastInfoResponse(
        symbol: String,
        currency: String = "USD",
        exchange: String = "NASDAQ"
    ): QuoteSummaryResponse {
        return QuoteSummaryResponse(
            quoteSummary = QuoteSummary(
                result = listOf(
                    QuoteSummaryResult(
                        price = Price(
                            symbol = symbol,
                            currency = currency
                        ),
                        quoteType = QuoteType(
                            symbol = symbol,
                            exchange = exchange,
                            quoteType = "EQUITY",
                            market = currency
                        )
                    )
                ),
                error = null
            )
        )
    }

    /**
     * Stock ISIN 응답 생성
     *
     * @param symbol 심볼
     * @param isin ISIN 코드
     * @return QuoteSummaryResponse
     */
    fun createIsinResponse(
        symbol: String,
        isin: String = "US0378331005"
    ): QuoteSummaryResponse {
        return QuoteSummaryResponse(
            quoteSummary = QuoteSummary(
                result = listOf(
                    QuoteSummaryResult(
                        defaultKeyStatistics = DefaultKeyStatistics(
                            isin = isin
                        )
                    )
                ),
                error = null
            )
        )
    }

    /**
     * Stock Shares 응답 생성
     *
     * @param symbol 심볼
     * @param shares 발행주식수
     * @return QuoteSummaryResponse
     */
    fun createSharesResponse(
        symbol: String,
        shares: Long = 15550061000L
    ): QuoteSummaryResponse {
        return QuoteSummaryResponse(
            quoteSummary = QuoteSummary(
                result = listOf(
                    QuoteSummaryResult(
                        defaultKeyStatistics = DefaultKeyStatistics(
                            sharesOutstanding = RawFormatted(
                                raw = JsonPrimitive(shares),
                                fmt = "${shares / 1_000_000_000.0}B"
                            )
                        )
                    )
                ),
                error = null
            )
        )
    }

    /**
     * Fund 데이터 응답 생성 (Funds 도메인 테스트용)
     *
     * @param symbol 심볼
     * @param quoteType 자산 타입 (ETF, MUTUALFUND)
     * @return QuoteSummaryResponse
     */
    fun createFundDataResponse(
        symbol: String,
        quoteType: String = "ETF"
    ): QuoteSummaryResponse {
        return QuoteSummaryResponse(
            quoteSummary = QuoteSummary(
                result = listOf(
                    QuoteSummaryResult(
                        price = Price(
                            symbol = symbol,
                            longName = "Test Fund $symbol",
                            currency = "USD"
                        ),
                        quoteType = QuoteType(
                            symbol = symbol,
                            quoteType = quoteType,
                            longName = "Test Fund $symbol"
                        ),
                        fundProfile = FundProfile(
                            categoryName = "Large Blend",
                            family = "Test Family"
                        ),
                        topHoldings = TopHoldings(
                            holdings = listOf(
                                Holding(
                                    symbol = "AAPL",
                                    name = "Apple Inc.",
                                    holdingPercent = RawFormatted(
                                        raw = JsonPrimitive(0.05),
                                        fmt = "5.00%"
                                    )
                                )
                            )
                        ),
                        summaryDetail = SummaryDetail()
                    )
                ),
                error = null
            )
        )
    }
}

/**
 * 테스트용 Chart 응답 빌더
 *
 * 간단하게 유효한 ChartDataResponse를 생성합니다.
 */
object TestChartResponseBuilder {

    /**
     * 기본 Chart 응답 생성
     *
     * @param symbol 심볼
     * @param dataPoints 데이터 포인트 수
     * @param basePrice 기준 가격
     * @return ChartDataResponse
     */
    fun createChartResponse(
        symbol: String,
        dataPoints: Int = 10,
        basePrice: Double = 100.0
    ): ChartDataResponse {
        val timestamps = (0 until dataPoints).map { 1609459200L + it * 86400 }  // 2021-01-01부터 하루씩

        val opens = (0 until dataPoints).map { basePrice + it }
        val highs = (0 until dataPoints).map { basePrice + it + 2 }
        val lows = (0 until dataPoints).map { basePrice + it - 1 }
        val closes = (0 until dataPoints).map { basePrice + it + 1 }
        val volumes = (0 until dataPoints).map { 1000000.0 + it * 10000 }
        val adjCloses = (0 until dataPoints).map { basePrice + it + 1 }

        return ChartDataResponse(
            chart = Chart(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(
                            symbol = symbol,
                            currency = "USD",
                            exchange = "NMS",
                            regularMarketPrice = closes.last(),
                            dataGranularity = "1d",
                            range = "1y",
                            validRanges = listOf("1d", "5d", "1mo", "3mo", "6mo", "1y", "2y", "5y", "10y", "ytd", "max")
                        ),
                        timestamp = timestamps,
                        indicators = ChartIndicators(
                            quote = listOf(
                                ChartQuote(
                                    open = opens,
                                    high = highs,
                                    low = lows,
                                    close = closes,
                                    volume = volumes.map { it.toLong() }
                                )
                            ),
                            adjclose = listOf(
                                ChartAdjClose(
                                    adjclose = adjCloses
                                )
                            )
                        )
                    )
                ),
                error = null
            )
        )
    }

    /**
     * 배당 이벤트가 포함된 Chart 응답 생성 (Corp 도메인 테스트용)
     */
    fun createChartDataWithDividends(symbol: String, dividendAmount: Double = 0.88): ChartDataResponse {
        val baseResponse = createChartResponse(symbol)
        val result = baseResponse.chart.result?.first() ?: throw IllegalStateException("No chart result")
        return ChartDataResponse(
            chart = Chart(
                result = listOf(
                    result.copy(
                        events = ChartEvents(
                            dividends = mapOf("1609459200" to DividendEvent(amount = dividendAmount, date = 1609459200L))
                        )
                    )
                ),
                error = null
            )
        )
    }

    /**
     * 주식분할 이벤트가 포함된 Chart 응답 생성 (Corp 도메인 테스트용)
     */
    fun createChartDataWithSplits(symbol: String, splitRatio: String = "4:1"): ChartDataResponse {
        val baseResponse = createChartResponse(symbol)
        val result = baseResponse.chart.result?.first() ?: throw IllegalStateException("No chart result")
        val parts = splitRatio.split(":")
        return ChartDataResponse(
            chart = Chart(
                result = listOf(
                    result.copy(
                        events = ChartEvents(
                            splits = mapOf(
                                "1609459200" to SplitEvent(
                                    date = 1609459200L,
                                    numerator = parts[0].toDouble(),
                                    denominator = parts[1].toDouble(),
                                    splitRatio = splitRatio
                                )
                            )
                        )
                    )
                ),
                error = null
            )
        )
    }

    /**
     * 자본이득 이벤트가 포함된 Chart 응답 생성 (Corp 도메인 테스트용)
     */
    fun createChartDataWithCapitalGains(symbol: String, capitalGain: Double = 1.50): ChartDataResponse {
        val baseResponse = createChartResponse(symbol)
        val result = baseResponse.chart.result?.first() ?: throw IllegalStateException("No chart result")
        return ChartDataResponse(
            chart = Chart(
                result = listOf(
                    result.copy(
                        events = ChartEvents(
                            capitalGains = mapOf("1609459200" to CapitalGainEvent(amount = capitalGain, date = 1609459200L))
                        )
                    )
                ),
                error = null
            )
        )
    }

    /**
     * 모든 Corporate Actions가 포함된 Chart 응답 생성 (Corp 도메인 테스트용)
     */
    fun createChartDataWithAll(symbol: String): ChartDataResponse {
        val baseResponse = createChartResponse(symbol)
        val result = baseResponse.chart.result?.first() ?: throw IllegalStateException("No chart result")
        return ChartDataResponse(
            chart = Chart(
                result = listOf(
                    result.copy(
                        events = ChartEvents(
                            dividends = mapOf("1609459200" to DividendEvent(amount = 0.88, date = 1609459200L)),
                            splits = mapOf("1609545600" to SplitEvent(date = 1609545600L, numerator = 4.0, denominator = 1.0, splitRatio = "4:1")),
                            capitalGains = mapOf("1609632000" to CapitalGainEvent(amount = 1.50, date = 1609632000L))
                        )
                    )
                ),
                error = null
            )
        )
    }

    /**
     * 빈 Chart 응답 생성 (이벤트 없음)
     */
    fun createEmptyChartData(symbol: String): ChartDataResponse = createChartResponse(symbol)
}
