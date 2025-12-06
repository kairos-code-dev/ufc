package com.ulalax.ufc.infrastructure.yahoo

import com.ulalax.ufc.domain.exception.ApiException
import com.ulalax.ufc.domain.exception.DataParsingException
import com.ulalax.ufc.domain.exception.ErrorCode
import com.ulalax.ufc.domain.exception.ValidationException
import com.ulalax.ufc.infrastructure.common.ratelimit.GlobalRateLimiters
import com.ulalax.ufc.infrastructure.common.ratelimit.RateLimiter
import com.ulalax.ufc.infrastructure.yahoo.internal.YahooApiUrls
import com.ulalax.ufc.infrastructure.yahoo.internal.auth.YahooAuthenticator
import com.ulalax.ufc.infrastructure.yahoo.internal.response.ChartDataResponse
import com.ulalax.ufc.infrastructure.yahoo.internal.response.FundamentalsTimeseriesResponse
import com.ulalax.ufc.infrastructure.yahoo.internal.response.QuoteSummaryResponse
import com.ulalax.ufc.domain.model.chart.*
import com.ulalax.ufc.domain.model.fundamentals.*
import com.ulalax.ufc.domain.model.quote.*
import com.ulalax.ufc.domain.model.earnings.*
import com.ulalax.ufc.domain.model.lookup.*
import com.ulalax.ufc.domain.model.market.*
import com.ulalax.ufc.domain.model.options.*
import com.ulalax.ufc.domain.model.realtime.*
import com.ulalax.ufc.domain.model.screener.*
import com.ulalax.ufc.domain.model.search.*
import com.ulalax.ufc.domain.model.visualization.*
import com.ulalax.ufc.infrastructure.yahoo.internal.response.LookupResponse
import com.ulalax.ufc.infrastructure.yahoo.internal.response.VisualizationResponse
import com.ulalax.ufc.infrastructure.yahoo.internal.request.VisualizationRequest
import com.ulalax.ufc.infrastructure.yahoo.internal.request.VisualizationQuery
import com.ulalax.ufc.infrastructure.yahoo.internal.response.SearchApiResponse
import com.ulalax.ufc.infrastructure.yahoo.internal.response.ScreenerResponse
import com.ulalax.ufc.infrastructure.yahoo.internal.request.ScreenerRequest
import com.ulalax.ufc.infrastructure.yahoo.internal.response.QuoteApiResponse
import com.ulalax.ufc.infrastructure.yahoo.internal.response.OptionsResponse
import com.ulalax.ufc.infrastructure.yahoo.internal.response.MarketSummaryResponse
import com.ulalax.ufc.infrastructure.yahoo.internal.response.MarketTimeResponse
import com.ulalax.ufc.infrastructure.yahoo.internal.response.EarningsCalendarHtmlResponse
import com.ulalax.ufc.infrastructure.yahoo.internal.response.EarningsTableRow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

/**
 * Yahoo Finance API Client
 *
 * Provides a simple client interface to access the Yahoo Finance API.
 * This client handles authentication, rate limiting, and response parsing automatically.
 *
 * Usage example:
 * ```kotlin
 * val yahoo = YahooClient.create()
 * val quote = yahoo.quoteSummary("AAPL", QuoteSummaryModule.PRICE)
 * val chart = yahoo.chart("AAPL", Interval.OneDay, Period.OneYear)
 * ```
 *
 * @property httpClient Ktor HTTP client for making API requests
 * @property authenticator Yahoo Finance authentication manager
 * @property rateLimiter Rate limiter to prevent API throttling
 */
class YahooClient internal constructor(
    private val httpClient: HttpClient,
    private val authenticator: YahooAuthenticator,
    private val rateLimiter: RateLimiter
) : AutoCloseable {

    companion object {
        private val logger = LoggerFactory.getLogger(YahooClient::class.java)

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        /**
         * Creates a new YahooClient instance with the specified configuration.
         *
         * @param config Client configuration (optional). Defaults to [YahooClientConfig] with standard settings.
         * @return A new YahooClient instance ready to make API calls
         */
        fun create(config: YahooClientConfig = YahooClientConfig()): YahooClient {
            // GlobalRateLimiters에서 공유 Rate Limiter 획득
            val rateLimiter = GlobalRateLimiters.getYahooLimiter(config.rateLimitConfig)

            val httpClient = HttpClient(CIO) {
                // 기본 헤더 설정 (Yahoo Finance API 요구사항)
                defaultRequest {
                    header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    header(HttpHeaders.Accept, "application/json, text/plain, */*")
                    header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9")
                }

                // 쿠키 자동 관리
                install(HttpCookies)

                // JSON 직렬화
                install(ContentNegotiation) {
                    json(json)
                }

                // 타임아웃 설정
                install(HttpTimeout) {
                    connectTimeoutMillis = config.connectTimeoutMs
                    requestTimeoutMillis = config.requestTimeoutMs
                }

                // 로깅 설정 (개발 시 유용)
                if (config.enableLogging) {
                    install(Logging) {
                        level = LogLevel.INFO
                    }
                }
            }

            val authenticator = YahooAuthenticator(httpClient)

            return YahooClient(httpClient, authenticator, rateLimiter)
        }
    }

    /**
     * Fetches quote summary data for a given symbol from the Yahoo Finance QuoteSummary API.
     *
     * @param symbol The stock symbol to query (e.g., "AAPL", "GOOGL")
     * @param modules Variable number of [QuoteSummaryModule] to retrieve (e.g., PRICE, FINANCIAL_DATA, EARNINGS)
     * @return [QuoteSummaryModuleResult] containing the requested module data
     * @throws ApiException If the API call fails or returns an error response
     */
    suspend fun quoteSummary(
        symbol: String,
        vararg modules: QuoteSummaryModule
    ): QuoteSummaryModuleResult {
        return quoteSummary(symbol, modules.toSet())
    }

    /**
     * Fetches quote summary data for a given symbol from the Yahoo Finance QuoteSummary API.
     *
     * @param symbol The stock symbol to query (e.g., "AAPL", "GOOGL")
     * @param modules Set of [QuoteSummaryModule] to retrieve
     * @return [QuoteSummaryModuleResult] containing the requested module data
     * @throws ApiException If the API call fails or returns an error response
     */
    suspend fun quoteSummary(
        symbol: String,
        modules: Set<QuoteSummaryModule>
    ): QuoteSummaryModuleResult {
        logger.debug("Calling Yahoo Finance QuoteSummary API: symbol={}, modules={}", symbol, modules)

        // Rate Limit 적용
        rateLimiter.acquire()

        // CRUMB 토큰 획득
        val crumb = authenticator.getCrumb()

        // API 요청
        val response = httpClient.get("${YahooApiUrls.QUOTE_SUMMARY}/$symbol") {
            parameter("modules", modules.joinToString(",") { it.apiValue })
            parameter("crumb", crumb)
        }

        // HTTP 상태 코드 확인
        if (!response.status.isSuccess()) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "QuoteSummary API 요청 실패: HTTP ${response.status.value}",
                statusCode = response.status.value,
                metadata = mapOf("symbol" to symbol, "modules" to modules.joinToString(",") { it.apiValue })
            )
        }

        // 응답 파싱
        val responseBody = response.body<String>()
        val quoteSummaryResponse = json.decodeFromString<QuoteSummaryResponse>(responseBody)

        // 에러 응답 확인
        if (quoteSummaryResponse.quoteSummary.error != null) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "QuoteSummary API 에러: ${quoteSummaryResponse.quoteSummary.error?.description ?: "Unknown error"}",
                metadata = mapOf("symbol" to symbol, "errorCode" to (quoteSummaryResponse.quoteSummary.error?.code ?: "UNKNOWN"))
            )
        }

        // 결과 확인
        if (quoteSummaryResponse.quoteSummary.result.isNullOrEmpty()) {
            throw ApiException(
                errorCode = ErrorCode.DATA_NOT_FOUND,
                message = "QuoteSummary 데이터를 찾을 수 없습니다: $symbol",
                metadata = mapOf("symbol" to symbol)
            )
        }

        // QuoteSummaryResult를 QuoteSummaryModuleResult로 변환
        return convertToQuoteSummaryModuleResult(
            quoteSummaryResponse.quoteSummary.result.first(),
            modules
        )
    }

    /**
     * Internal QuoteSummaryResult를 public QuoteSummaryModuleResult로 변환합니다.
     *
     * @param result 내부 API 응답 결과
     * @param requestedModules 요청한 모듈 목록
     * @return QuoteSummaryModuleResult
     */
    private fun convertToQuoteSummaryModuleResult(
        result: com.ulalax.ufc.infrastructure.yahoo.internal.response.QuoteSummaryResult,
        requestedModules: Set<QuoteSummaryModule>
    ): QuoteSummaryModuleResult {
        val moduleMap = mutableMapOf<QuoteSummaryModule, Any?>()

        // 각 모듈별 데이터 매핑 - internal response 타입을 public domain 타입으로 변환
        result.price?.let { moduleMap[QuoteSummaryModule.PRICE] = convertPrice(it) }
        result.summaryDetail?.let { moduleMap[QuoteSummaryModule.SUMMARY_DETAIL] = convertSummaryDetail(it) }
        result.assetProfile?.let { moduleMap[QuoteSummaryModule.ASSET_PROFILE] = convertAssetProfile(it) }
        result.summaryProfile?.let { moduleMap[QuoteSummaryModule.SUMMARY_PROFILE] = convertSummaryProfile(it) }
        result.quoteType?.let { moduleMap[QuoteSummaryModule.QUOTE_TYPE] = convertQuoteType(it) }
        result.defaultKeyStatistics?.let { moduleMap[QuoteSummaryModule.DEFAULT_KEY_STATISTICS] = convertDefaultKeyStatistics(it) }
        result.financialData?.let { moduleMap[QuoteSummaryModule.FINANCIAL_DATA] = convertFinancialData(it) }
        result.earningsTrend?.let { moduleMap[QuoteSummaryModule.EARNINGS_TREND] = convertEarningsTrend(it) }
        result.earningsHistory?.let { moduleMap[QuoteSummaryModule.EARNINGS_HISTORY] = convertEarningsHistory(it) }
        result.earningsDates?.let { moduleMap[QuoteSummaryModule.EARNINGS_DATES] = convertEarningsDates(it) }
        result.majorHolders?.let { moduleMap[QuoteSummaryModule.MAJOR_HOLDERS] = convertMajorHolders(it) }
        result.insiderTransactions?.let { moduleMap[QuoteSummaryModule.INSIDER_TRANSACTIONS] = convertInsiderTransactions(it) }
        result.topHoldings?.let { moduleMap[QuoteSummaryModule.TOP_HOLDINGS] = convertTopHoldings(it) }
        result.fundProfile?.let { moduleMap[QuoteSummaryModule.FUND_PROFILE] = convertFundProfile(it) }

        return QuoteSummaryModuleResult(
            requestedModules = requestedModules,
            modules = moduleMap
        )
    }

    // 변환 함수들 - internal response 타입을 public domain 타입으로 변환
    private fun convertRawFormatted(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.RawFormatted): RawFormatted {
        return RawFormatted(raw = internal.raw, fmt = internal.fmt)
    }

    private fun convertPrice(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.Price): Price {
        return Price(
            maxAge = internal.maxAge,
            regularMarketPrice = internal.regularMarketPrice?.let { convertRawFormatted(it) },
            currency = internal.currency,
            symbol = internal.symbol,
            longName = internal.longName,
            shortName = internal.shortName,
            exchange = internal.exchange,
            postMarketChangePercent = internal.postMarketChangePercent?.let { convertRawFormatted(it) },
            postMarketPrice = internal.postMarketPrice?.let { convertRawFormatted(it) },
            fiftyTwoWeekLow = internal.fiftyTwoWeekLow?.let { convertRawFormatted(it) },
            fiftyTwoWeekHigh = internal.fiftyTwoWeekHigh?.let { convertRawFormatted(it) },
            fiftyTwoWeekChangePercent = internal.fiftyTwoWeekChangePercent?.let { convertRawFormatted(it) },
            regularMarketDayRange = internal.regularMarketDayRange,
            regularMarketPreviousClose = internal.regularMarketPreviousClose?.let { convertRawFormatted(it) },
            regularMarketChange = internal.regularMarketChange?.let { convertRawFormatted(it) },
            regularMarketChangePercent = internal.regularMarketChangePercent?.let { convertRawFormatted(it) }
        )
    }

    private fun convertSummaryDetail(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.SummaryDetail): SummaryDetail {
        return SummaryDetail(
            maxAge = internal.maxAge,
            dividendRate = internal.dividendRate?.let { convertRawFormatted(it) },
            dividendYield = internal.dividendYield?.let { convertRawFormatted(it) },
            averageVolume = internal.averageVolume?.let { convertRawFormatted(it) },
            averageVolume10days = internal.averageVolume10days?.let { convertRawFormatted(it) },
            beta = internal.beta?.let { convertRawFormatted(it) },
            regularMarketDayHigh = internal.regularMarketDayHigh?.let { convertRawFormatted(it) },
            regularMarketDayLow = internal.regularMarketDayLow?.let { convertRawFormatted(it) },
            regularMarketVolume = internal.regularMarketVolume?.let { convertRawFormatted(it) },
            marketCap = internal.marketCap?.let { convertRawFormatted(it) },
            revenue = internal.revenue?.let { convertRawFormatted(it) },
            dividendDate = internal.dividendDate?.let { convertRawFormatted(it) },
            exDividendDate = internal.exDividendDate?.let { convertRawFormatted(it) },
            sharesOutstanding = internal.sharesOutstanding?.let { convertRawFormatted(it) },
            debtToEquity = internal.debtToEquity?.let { convertRawFormatted(it) },
            trailingPE = internal.trailingPE?.let { convertRawFormatted(it) },
            forwardPE = internal.forwardPE?.let { convertRawFormatted(it) },
            priceToBook = internal.priceToBook?.let { convertRawFormatted(it) },
            fiftyTwoWeekHigh = internal.fiftyTwoWeekHigh?.let { convertRawFormatted(it) },
            fiftyTwoWeekLow = internal.fiftyTwoWeekLow?.let { convertRawFormatted(it) }
        )
    }

    private fun convertFinancialData(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.FinancialData): FinancialData {
        return FinancialData(
            maxAge = internal.maxAge,
            operatingCashflow = internal.operatingCashflow?.let { convertRawFormatted(it) },
            freeCashflow = internal.freeCashflow?.let { convertRawFormatted(it) },
            totalDebt = internal.totalDebt?.let { convertRawFormatted(it) },
            totalCash = internal.totalCash?.let { convertRawFormatted(it) },
            longTermDebt = internal.longTermDebt?.let { convertRawFormatted(it) },
            currentRatio = internal.currentRatio?.let { convertRawFormatted(it) },
            returnOnEquity = internal.returnOnEquity?.let { convertRawFormatted(it) },
            returnOnAssets = internal.returnOnAssets?.let { convertRawFormatted(it) },
            pegRatio = internal.pegRatio?.let { convertRawFormatted(it) },
            profitMargins = internal.profitMargins?.let { convertRawFormatted(it) },
            revenueGrowth = internal.revenueGrowth?.let { convertRawFormatted(it) },
            earningsGrowth = internal.earningsGrowth?.let { convertRawFormatted(it) },
            targetPriceHigh = internal.targetPriceHigh?.let { convertRawFormatted(it) },
            targetPriceLow = internal.targetPriceLow?.let { convertRawFormatted(it) },
            targetPriceMean = internal.targetPriceMean?.let { convertRawFormatted(it) },
            recommendationKey = internal.recommendationKey,
            numberOfAnalysts = internal.numberOfAnalysts
        )
    }

    private fun convertEarningsTrendData(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.EarningsTrendData): EarningsTrendData {
        return EarningsTrendData(
            maxAge = internal.maxAge,
            period = internal.period,
            endDate = internal.endDate,
            epsEstimate = internal.epsEstimate?.let { convertRawFormatted(it) },
            epsActual = internal.epsActual?.let { convertRawFormatted(it) },
            epsDifference = internal.epsDifference?.let { convertRawFormatted(it) },
            surprisePercent = internal.surprisePercent?.let { convertRawFormatted(it) }
        )
    }

    private fun convertEarningsTrend(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.EarningsTrend): EarningsTrend {
        return EarningsTrend(
            maxAge = internal.maxAge,
            trend = internal.trend?.map { convertEarningsTrendData(it) },
            earningsHistory = internal.earningsHistory?.map { convertEarningsTrendData(it) }
        )
    }

    private fun convertEarningsHistory(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.EarningsHistory): EarningsHistory {
        return EarningsHistory(
            maxAge = internal.maxAge,
            history = internal.history?.map { convertEarningsTrendData(it) }
        )
    }

    private fun convertEarningsDates(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.EarningsDates): EarningsDates {
        return EarningsDates(
            maxAge = internal.maxAge,
            earningsDate = internal.earningsDate,
            earningsAverage = internal.earningsAverage?.let { convertRawFormatted(it) },
            earningsLow = internal.earningsLow?.let { convertRawFormatted(it) },
            earningsHigh = internal.earningsHigh?.let { convertRawFormatted(it) }
        )
    }

    private fun convertHolder(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.Holder): Holder {
        return Holder(
            maxAge = internal.maxAge,
            holder = internal.holder,
            value = internal.value?.let { convertRawFormatted(it) }
        )
    }

    private fun convertMajorHolders(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.MajorHolders): MajorHolders {
        return MajorHolders(
            maxAge = internal.maxAge,
            holders = internal.holders?.map { convertHolder(it) }
        )
    }

    private fun convertInsiderTransaction(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.InsiderTransaction): InsiderTransaction {
        return InsiderTransaction(
            maxAge = internal.maxAge,
            filerName = internal.filerName,
            relationship = internal.relationship,
            transactionDate = internal.transactionDate?.let { convertRawFormatted(it) },
            transactionShares = internal.transactionShares?.let { convertRawFormatted(it) },
            transactionPrice = internal.transactionPrice?.let { convertRawFormatted(it) },
            sharesOwned = internal.sharesOwned?.let { convertRawFormatted(it) }
        )
    }

    private fun convertInsiderTransactions(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.InsiderTransactions): InsiderTransactions {
        return InsiderTransactions(
            maxAge = internal.maxAge,
            transactions = internal.transactions?.map { convertInsiderTransaction(it) }
        )
    }

    private fun convertQuoteType(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.QuoteType): QuoteType {
        return QuoteType(
            exchange = internal.exchange,
            quoteType = internal.quoteType,
            symbol = internal.symbol,
            shortName = internal.shortName,
            longName = internal.longName,
            market = internal.market,
            sector = internal.sector,
            industry = internal.industry
        )
    }

    private fun convertAssetProfile(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.AssetProfile): AssetProfile {
        return AssetProfile(
            sector = internal.sector,
            industry = internal.industry,
            website = internal.website,
            longBusinessSummary = internal.longBusinessSummary,
            country = internal.country,
            city = internal.city,
            address1 = internal.address1,
            phone = internal.phone,
            state = internal.state,
            zip = internal.zip,
            fullTimeEmployees = internal.fullTimeEmployees
        )
    }

    private fun convertSummaryProfile(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.SummaryProfile): SummaryProfile {
        return SummaryProfile(
            sector = internal.sector,
            industry = internal.industry,
            website = internal.website,
            address1 = internal.address1,
            city = internal.city,
            state = internal.state,
            zip = internal.zip,
            country = internal.country,
            phone = internal.phone
        )
    }

    private fun convertDefaultKeyStatistics(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.DefaultKeyStatistics): DefaultKeyStatistics {
        return DefaultKeyStatistics(
            sharesOutstanding = internal.sharesOutstanding?.let { convertRawFormatted(it) },
            isin = internal.isin,
            cusip = internal.cusip,
            maxAge = internal.maxAge
        )
    }

    private fun convertHolding(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.Holding): Holding {
        return Holding(
            symbol = internal.symbol,
            name = internal.name,
            holdingPercent = internal.holdingPercent?.let { convertRawFormatted(it) }
        )
    }

    private fun convertEquityHoldings(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.EquityHoldings): EquityHoldings {
        return EquityHoldings(
            priceToEarnings = internal.priceToEarnings?.let { convertRawFormatted(it) },
            priceToBook = internal.priceToBook?.let { convertRawFormatted(it) },
            priceToSales = internal.priceToSales?.let { convertRawFormatted(it) },
            priceToCashflow = internal.priceToCashflow?.let { convertRawFormatted(it) },
            medianMarketCap = internal.medianMarketCap?.let { convertRawFormatted(it) },
            threeYearEarningsGrowth = internal.threeYearEarningsGrowth?.let { convertRawFormatted(it) }
        )
    }

    private fun convertBondHoldings(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.BondHoldings): BondHoldings {
        return BondHoldings(
            duration = internal.duration?.let { convertRawFormatted(it) },
            maturity = internal.maturity?.let { convertRawFormatted(it) },
            creditQuality = internal.creditQuality?.let { convertRawFormatted(it) }
        )
    }

    private fun convertSectorWeighting(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.SectorWeighting): SectorWeighting {
        return SectorWeighting(
            sector = internal.sector,
            weight = internal.weight?.let { convertRawFormatted(it) }
        )
    }

    private fun convertTopHoldings(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.TopHoldings): TopHoldings {
        return TopHoldings(
            holdings = internal.holdings?.map { convertHolding(it) },
            equityHoldings = internal.equityHoldings?.let { convertEquityHoldings(it) },
            bondHoldings = internal.bondHoldings?.let { convertBondHoldings(it) },
            sectorWeightings = internal.sectorWeightings?.map { convertSectorWeighting(it) }
        )
    }

    private fun convertFeesExpenses(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.FeesExpenses): FeesExpenses {
        return FeesExpenses(
            annualReportExpenseRatio = internal.annualReportExpenseRatio?.let { convertRawFormatted(it) },
            annualHoldingsTurnover = internal.annualHoldingsTurnover?.let { convertRawFormatted(it) },
            totalNetAssets = internal.totalNetAssets?.let { convertRawFormatted(it) }
        )
    }

    private fun convertFundProfile(internal: com.ulalax.ufc.infrastructure.yahoo.internal.response.FundProfile): FundProfile {
        return FundProfile(
            categoryName = internal.categoryName,
            family = internal.family,
            legalType = internal.legalType,
            feesExpensesInvestment = internal.feesExpensesInvestment?.let { convertFeesExpenses(it) }
        )
    }

    /**
     * Fetches historical chart data for a given symbol from the Yahoo Finance Chart API.
     *
     * @param symbol The stock symbol to query (e.g., "AAPL", "TSLA")
     * @param interval The data interval (e.g., [Interval.OneDay], [Interval.OneHour])
     * @param period The time period to retrieve data for (e.g., [Period.OneYear], [Period.OneMonth])
     * @param events Optional event types to include (e.g., [ChartEventType.DIVIDENDS], [ChartEventType.SPLITS])
     * @return [ChartData] containing OHLCV data and metadata
     * @throws ApiException If the API call fails or returns an error response
     */
    suspend fun chart(
        symbol: String,
        interval: Interval,
        period: Period,
        vararg events: ChartEventType = emptyArray()
    ): ChartData {
        logger.debug(
            "Calling Yahoo Finance Chart API: symbol={}, interval={}, period={}, events={}",
            symbol, interval.value, period.value, events.toList()
        )

        // Rate Limit 적용
        rateLimiter.acquire()

        // CRUMB 토큰 획득
        val crumb = authenticator.getCrumb()

        // API 요청
        val response = httpClient.get("${YahooApiUrls.CHART}/$symbol") {
            parameter("interval", interval.value)
            parameter("range", period.value)
            parameter("crumb", crumb)
            if (events.isNotEmpty()) {
                parameter("events", events.joinToString(",") { it.apiValue })
            }
        }

        // HTTP 상태 코드 확인
        if (!response.status.isSuccess()) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Chart API 요청 실패: HTTP ${response.status.value}",
                statusCode = response.status.value,
                metadata = mapOf(
                    "symbol" to symbol,
                    "interval" to interval.value,
                    "period" to period.value
                )
            )
        }

        // 응답 파싱
        val responseBody = response.body<String>()
        val chartResponse = json.decodeFromString<ChartDataResponse>(responseBody)

        // 에러 응답 확인
        if (chartResponse.chart.error != null) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Chart API 에러: ${chartResponse.chart.error?.description ?: "Unknown error"}",
                metadata = mapOf(
                    "symbol" to symbol,
                    "errorCode" to (chartResponse.chart.error?.code ?: "UNKNOWN")
                )
            )
        }

        // 결과 확인
        if (chartResponse.chart.result.isNullOrEmpty()) {
            throw ApiException(
                errorCode = ErrorCode.DATA_NOT_FOUND,
                message = "차트 데이터를 찾을 수 없습니다: $symbol",
                metadata = mapOf("symbol" to symbol)
            )
        }

        // ChartResult를 ChartData로 변환
        val chartResult = chartResponse.chart.result.first()
        return convertToChartData(chartResult, events.toSet())
    }

    /**
     * Internal ChartResult를 public ChartData로 변환합니다.
     *
     * @param chartResult 내부 API 응답 결과
     * @param requestedEvents 요청한 이벤트 타입들
     * @return ChartData
     */
    private fun convertToChartData(
        chartResult: com.ulalax.ufc.infrastructure.yahoo.internal.response.ChartResult,
        requestedEvents: Set<ChartEventType>
    ): ChartData {
        // 메타데이터 변환
        val meta = chartResult.meta?.let {
            ChartMeta(
                symbol = it.symbol,
                currency = it.currency,
                currencySymbol = it.currencySymbol,
                regularMarketPrice = it.regularMarketPrice,
                exchange = it.exchange,
                regularMarketDayHigh = it.regularMarketDayHigh,
                regularMarketDayLow = it.regularMarketDayLow,
                dataGranularity = it.dataGranularity,
                range = it.range,
                fiftyTwoWeekHigh = it.fiftyTwoWeekHigh,
                fiftyTwoWeekLow = it.fiftyTwoWeekLow,
                sharesOutstanding = it.sharesOutstanding,
                marketCap = it.marketCap,
                regularMarketVolume = it.regularMarketVolume,
                validRanges = it.validRanges
            )
        } ?: throw DataParsingException(
            errorCode = ErrorCode.DATA_PARSING_ERROR,
            message = "차트 메타데이터가 없습니다"
        )

        // OHLCV 데이터 변환
        val timestamps = chartResult.timestamp ?: emptyList()
        val quote = chartResult.indicators?.quote?.firstOrNull()
        val adjClose = chartResult.indicators?.adjclose?.firstOrNull()

        val prices = timestamps.indices.mapNotNull { i ->
            val timestamp = timestamps.getOrNull(i) ?: return@mapNotNull null
            val open = quote?.open?.getOrNull(i) ?: return@mapNotNull null
            val high = quote?.high?.getOrNull(i) ?: return@mapNotNull null
            val low = quote?.low?.getOrNull(i) ?: return@mapNotNull null
            val close = quote?.close?.getOrNull(i) ?: return@mapNotNull null
            val volume = quote?.volume?.getOrNull(i) ?: return@mapNotNull null
            val adj = adjClose?.adjclose?.getOrNull(i)

            OHLCV(
                timestamp = timestamp,
                open = open,
                high = high,
                low = low,
                close = close,
                adjClose = adj,
                volume = volume
            )
        }

        // 이벤트 데이터 변환
        val events = chartResult.events?.let { eventsResponse ->
            ChartEvents(
                dividends = eventsResponse.dividends?.mapValues { (_, v) ->
                    DividendEvent(amount = v.amount, date = v.date)
                },
                splits = eventsResponse.splits?.mapValues { (_, v) ->
                    SplitEvent(
                        date = v.date,
                        numerator = v.numerator,
                        denominator = v.denominator,
                        splitRatio = v.splitRatio
                    )
                },
                capitalGains = eventsResponse.capitalGains?.mapValues { (_, v) ->
                    CapitalGainEvent(amount = v.amount, date = v.date)
                }
            )
        }

        return ChartData(
            requestedEvents = requestedEvents,
            meta = meta,
            prices = prices,
            events = events
        )
    }

    /**
     * Fetches earnings calendar data for a given symbol from the Yahoo Finance Earnings Calendar.
     *
     * Note: This method uses HTML scraping to collect data. If Yahoo Finance changes their
     * web page structure, parsing may fail.
     *
     * @param symbol The stock symbol to query (e.g., "AAPL", "MSFT")
     * @param limit The number of records to retrieve (default: 12, max: 100)
     * @param offset Pagination offset for retrieving additional results (default: 0)
     * @return [EarningsCalendar] containing earnings event data
     * @throws ApiException If the API call fails or returns an error response
     * @throws DataParsingException If HTML parsing fails or success rate is below 50%
     */
    suspend fun earningsCalendar(
        symbol: String,
        limit: Int = 12,
        offset: Int = 0
    ): EarningsCalendar {
        // 파라미터 검증
        require(symbol.isNotBlank()) { "Symbol must not be blank" }
        require(limit in 1..100) { "Limit must be between 1 and 100, got $limit" }
        require(offset >= 0) { "Offset must be non-negative, got $offset" }

        logger.debug("Calling Yahoo Finance Earnings Calendar: symbol={}, limit={}, offset={}", symbol, limit, offset)

        // Rate Limit 적용
        rateLimiter.acquire()

        // Yahoo Finance의 size 파라미터 계산
        val size = when {
            limit <= 25 -> 25
            limit <= 50 -> 50
            else -> 100
        }

        // HTML 요청
        val response = httpClient.get(YahooApiUrls.EARNINGS_CALENDAR) {
            parameter("symbol", symbol)
            parameter("offset", offset)
            parameter("size", size)
        }

        // HTTP 상태 코드 확인
        if (!response.status.isSuccess()) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Earnings Calendar API 요청 실패: HTTP ${response.status.value}",
                statusCode = response.status.value,
                metadata = mapOf("symbol" to symbol, "offset" to offset, "size" to size)
            )
        }

        // HTML 파싱
        val html = response.body<String>()
        val htmlResponse = parseEarningsCalendarHtml(html)

        // 데이터 변환 (limit 적용)
        val events = htmlResponse.rows
            .take(limit)
            .mapNotNull { row -> convertToEarningsEvent(row) }

        // 50% 이상 파싱 실패 시 예외 발생
        val successRate = if (htmlResponse.rows.isEmpty()) 1.0
                         else events.size.toDouble() / htmlResponse.rows.take(limit).size.toDouble()

        if (successRate < 0.5) {
            logger.warn("Earnings Calendar 파싱 성공률 낮음: ${successRate * 100}%")
            throw DataParsingException(
                errorCode = ErrorCode.DATA_PARSING_ERROR,
                message = "Earnings Calendar 파싱 실패율이 50%를 초과했습니다 (성공률: ${successRate * 100}%)",
                metadata = mapOf(
                    "symbol" to symbol,
                    "totalRows" to htmlResponse.rows.size,
                    "parsedRows" to events.size
                )
            )
        }

        return EarningsCalendar(
            symbol = symbol,
            events = events,
            requestedLimit = limit,
            requestedOffset = offset,
            actualCount = events.size
        )
    }

    /**
     * HTML을 파싱하여 EarningsCalendarHtmlResponse로 변환합니다.
     *
     * @param html Yahoo Finance Earnings Calendar HTML
     * @return EarningsCalendarHtmlResponse
     */
    private fun parseEarningsCalendarHtml(html: String): EarningsCalendarHtmlResponse {
        logger.debug("HTML length: ${html.length}, contains table: ${html.contains("<table")}")

        // 테이블 존재 확인
        if (!html.contains("<table")) {
            logger.warn("No table found in HTML response. HTML preview: ${html.take(500)}")
            return EarningsCalendarHtmlResponse(hasTable = false, rows = emptyList())
        }

        val rows = mutableListOf<EarningsTableRow>()

        // 간단한 HTML 파싱: <tr>...</tr> 태그 추출
        val trPattern = Regex("<tr[^>]*>(.*?)</tr>", RegexOption.DOT_MATCHES_ALL)
        val tdPattern = Regex("<td[^>]*>(.*?)</td>", RegexOption.DOT_MATCHES_ALL)

        trPattern.findAll(html).forEach { trMatch ->
            val trContent = trMatch.groupValues[1]
            val tds = tdPattern.findAll(trContent).map { it.groupValues[1].trim() }.toList()

            // 테이블 행이 6개 컬럼을 가지는 경우만 처리
            // [Symbol, Company, Earnings Date, EPS Estimate, Reported EPS, Surprise (%)]
            if (tds.size >= 6) {
                try {
                    val symbol = cleanHtml(tds[0])
                    val company = cleanHtml(tds[1])
                    val earningsDateRaw = cleanHtml(tds[2])
                    val epsEstimateRaw = cleanHtml(tds[3])
                    val reportedEpsRaw = cleanHtml(tds[4])
                    val surprisePercentRaw = cleanHtml(tds[5])

                    // Symbol이 존재하고 Earnings Date가 존재하는 행만 추가
                    if (symbol.isNotBlank() && earningsDateRaw.isNotBlank()) {
                        rows.add(
                            EarningsTableRow(
                                symbol = symbol,
                                company = company,
                                earningsDateRaw = earningsDateRaw,
                                epsEstimateRaw = epsEstimateRaw,
                                reportedEpsRaw = reportedEpsRaw,
                                surprisePercentRaw = surprisePercentRaw
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to parse earnings table row: ${e.message}")
                }
            }
        }

        logger.debug("Parsed ${rows.size} rows from HTML table")
        return EarningsCalendarHtmlResponse(hasTable = true, rows = rows)
    }

    /**
     * HTML 태그와 엔티티를 제거하고 순수 텍스트를 추출합니다.
     *
     * @param html HTML 문자열
     * @return 정제된 텍스트
     */
    private fun cleanHtml(html: String): String {
        return html
            .replace(Regex("<[^>]*>"), "") // HTML 태그 제거
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .trim()
    }

    /**
     * EarningsTableRow를 EarningsEvent로 변환합니다.
     *
     * @param row HTML 파싱 결과
     * @return EarningsEvent, 파싱 실패 시 null
     */
    private fun convertToEarningsEvent(row: EarningsTableRow): EarningsEvent? {
        return try {
            // 날짜 파싱
            val (earningsDate, timeZone) = parseEarningsDate(row.earningsDateRaw)

            // 숫자 파싱
            val epsEstimate = parseDoubleOrNull(row.epsEstimateRaw)
            val reportedEps = parseDoubleOrNull(row.reportedEpsRaw)
            val surprisePercent = parsePercentOrNull(row.surprisePercentRaw)

            EarningsEvent(
                earningsDate = earningsDate,
                timeZone = timeZone,
                epsEstimate = epsEstimate,
                reportedEps = reportedEps,
                surprisePercent = surprisePercent
            )
        } catch (e: Exception) {
            logger.warn("Failed to convert earnings event: ${e.message}, row=$row")
            null
        }
    }

    /**
     * Earnings Date 문자열을 파싱하여 Instant와 TimeZone을 반환합니다.
     *
     * 예시: "October 30, 2025 at 4 PM EDT" → (Instant, "America/New_York")
     *
     * @param dateStr Earnings Date 원본 문자열
     * @return Pair<Instant, String?> (UTC Instant, IANA TimeZone)
     */
    private fun parseEarningsDate(dateStr: String): Pair<Instant, String?> {
        // 타임존 추출 (마지막 단어)
        val parts = dateStr.trim().split(" ")
        val timeZoneAbbr = parts.lastOrNull() ?: ""
        val ianaTimeZone = mapTimeZone(timeZoneAbbr)

        // 날짜 부분 파싱 (타임존 제거)
        val dateWithoutTz = dateStr.replace(timeZoneAbbr, "").trim()

        // 포맷: "MMMM d, yyyy 'at' h a" (예: "October 30, 2025 at 4 PM")
        val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h a", Locale.ENGLISH)
        val localDateTime = LocalDateTime.parse(dateWithoutTz, formatter)

        // ZonedDateTime으로 변환 (IANA TimeZone 적용)
        val zoneId = if (ianaTimeZone != null) {
            java.time.ZoneId.of(ianaTimeZone)
        } else {
            java.time.ZoneId.of("UTC")
        }
        val zonedDateTime = ZonedDateTime.of(localDateTime, zoneId)

        // Instant로 변환
        val instant = zonedDateTime.toInstant()

        return Pair(instant, ianaTimeZone)
    }

    /**
     * Yahoo Finance 타임존 약어를 IANA 타임존으로 매핑합니다.
     *
     * @param abbr 타임존 약어 (예: "EDT", "PST")
     * @return IANA 타임존 (예: "America/New_York"), 매핑 실패 시 null
     */
    private fun mapTimeZone(abbr: String): String? {
        return when (abbr.uppercase()) {
            "EDT", "EST" -> "America/New_York"
            "PDT", "PST" -> "America/Los_Angeles"
            "CDT", "CST" -> "America/Chicago"
            "MDT", "MST" -> "America/Denver"
            else -> null
        }
    }

    /**
     * 문자열을 Double로 파싱합니다.
     * "-" 또는 빈 문자열은 null로 반환합니다.
     *
     * @param str 원본 문자열
     * @return Double 또는 null
     */
    private fun parseDoubleOrNull(str: String): Double? {
        val trimmed = str.trim()
        return when {
            trimmed.isEmpty() || trimmed == "-" -> null
            else -> trimmed.toDoubleOrNull()
        }
    }

    /**
     * 퍼센트 문자열을 Double로 파싱합니다.
     * "-" 또는 빈 문자열은 null로 반환합니다.
     *
     * @param str 원본 문자열 (예: "6.49%")
     * @return Double 또는 null
     */
    private fun parsePercentOrNull(str: String): Double? {
        val trimmed = str.trim().replace("%", "")
        return when {
            trimmed.isEmpty() || trimmed == "-" -> null
            else -> trimmed.toDoubleOrNull()
        }
    }

    /**
     * Fetches fundamentals timeseries data from the Yahoo Finance Fundamentals Timeseries API.
     *
     * Retrieves timeseries data for financial statement items from Yahoo Finance.
     * Provides annual, quarterly, and trailing data for items from income statements,
     * balance sheets, and cash flow statements.
     *
     * @param symbol The stock symbol to query (e.g., "AAPL", "GOOGL")
     * @param types List of [FundamentalsType] to retrieve (e.g., ANNUAL_TOTAL_REVENUE, QUARTERLY_NET_INCOME)
     * @param startDate Start date for the query (default: 5 years ago)
     * @param endDate End date for the query (default: today)
     * @return [FundamentalsTimeseriesResult] containing timeseries data organized by type
     * @throws IllegalArgumentException If symbol is blank or types list is empty
     * @throws ApiException If the API call fails or returns an error response
     *
     * Usage examples:
     * ```kotlin
     * // Retrieve annual revenue and quarterly net income
     * val result = yahooClient.fundamentalsTimeseries(
     *     "AAPL",
     *     listOf(
     *         FundamentalsType.ANNUAL_TOTAL_REVENUE,
     *         FundamentalsType.QUARTERLY_NET_INCOME
     *     )
     * )
     *
     * // Retrieve data for a specific date range
     * val result = yahooClient.fundamentalsTimeseries(
     *     "AAPL",
     *     listOf(FundamentalsType.TRAILING_EPS),
     *     startDate = LocalDate.of(2020, 1, 1),
     *     endDate = LocalDate.of(2023, 12, 31)
     * )
     * ```
     */
    suspend fun fundamentalsTimeseries(
        symbol: String,
        types: List<FundamentalsType>,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): FundamentalsTimeseriesResult {
        // 입력 검증
        require(symbol.isNotBlank()) { "symbol은 공백일 수 없습니다" }
        require(types.isNotEmpty()) { "types는 빈 리스트일 수 없습니다" }
        if (startDate != null && endDate != null) {
            require(!startDate.isAfter(endDate)) { "startDate는 endDate보다 이전이어야 합니다" }
        }

        logger.debug(
            "Calling Yahoo Finance Fundamentals Timeseries API: symbol={}, types={}, startDate={}, endDate={}",
            symbol, types.map { it.apiValue }, startDate, endDate
        )

        // Rate Limit 적용
        rateLimiter.acquire()

        // CRUMB 토큰 획득
        val crumb = authenticator.getCrumb()

        // 날짜를 Unix timestamp로 변환
        val period1 = startDate?.atStartOfDay(ZoneOffset.UTC)?.toEpochSecond()
            ?: LocalDate.now().minusYears(5).atStartOfDay(ZoneOffset.UTC).toEpochSecond()
        val period2 = endDate?.atStartOfDay(ZoneOffset.UTC)?.toEpochSecond()
            ?: LocalDate.now().atStartOfDay(ZoneOffset.UTC).toEpochSecond()

        // API 요청
        val response = httpClient.get("${YahooApiUrls.FUNDAMENTALS_TIMESERIES}/$symbol") {
            parameter("symbol", symbol)
            parameter("type", types.joinToString(",") { it.apiValue })
            parameter("period1", period1)
            parameter("period2", period2)
            parameter("crumb", crumb)
        }

        // HTTP 상태 코드 확인
        if (!response.status.isSuccess()) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Fundamentals Timeseries API 요청 실패: HTTP ${response.status.value}",
                statusCode = response.status.value,
                metadata = mapOf(
                    "symbol" to symbol,
                    "types" to types.joinToString(",") { it.apiValue }
                )
            )
        }

        // 응답 파싱
        val responseBody = response.body<String>()
        val timeseriesResponse = json.decodeFromString<FundamentalsTimeseriesResponse>(responseBody)

        // 에러 응답 확인
        if (timeseriesResponse.timeseries.error != null) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Fundamentals Timeseries API 에러: ${timeseriesResponse.timeseries.error?.description ?: "Unknown error"}",
                metadata = mapOf(
                    "symbol" to symbol,
                    "errorCode" to (timeseriesResponse.timeseries.error?.code ?: "UNKNOWN")
                )
            )
        }

        // 결과가 없는 경우 빈 결과 반환
        if (timeseriesResponse.timeseries.result.isNullOrEmpty()) {
            logger.warn("No fundamentals timeseries data found for symbol: {}", symbol)
            return FundamentalsTimeseriesResult.empty(symbol)
        }

        // Internal Response를 Domain Model로 변환
        return convertToFundamentalsTimeseriesResult(symbol, timeseriesResponse.timeseries.result, types)
    }

    /**
     * Internal TimeseriesResult를 public FundamentalsTimeseriesResult로 변환합니다.
     *
     * @param symbol 조회한 심볼
     * @param results 내부 API 응답 결과 리스트
     * @param requestedTypes 요청한 타입 목록
     * @return FundamentalsTimeseriesResult
     */
    private fun convertToFundamentalsTimeseriesResult(
        symbol: String,
        results: List<com.ulalax.ufc.infrastructure.yahoo.internal.response.TimeseriesResult>,
        requestedTypes: List<FundamentalsType>
    ): FundamentalsTimeseriesResult {
        val dataMap = mutableMapOf<FundamentalsType, List<TimeseriesDataPoint>>()

        // 각 result를 순회하며 동적 필드에서 데이터 추출
        for (result in results) {
            for ((fieldName, dataPoints) in result.dataFields) {
                // fieldName을 FundamentalsType으로 변환
                val fundamentalsType = FundamentalsType.fromApiValue(fieldName)
                if (fundamentalsType == null) {
                    logger.warn("Unknown fundamentals type from API: {}", fieldName)
                    continue
                }

                // DataPoint를 TimeseriesDataPoint로 변환
                val timeseriesDataPoints = dataPoints.mapNotNull { dataPoint ->
                    val asOfDateStr = dataPoint.asOfDate
                    if (asOfDateStr == null) {
                        logger.warn("asOfDate is null for type: {}", fieldName)
                        return@mapNotNull null
                    }

                    try {
                        val asOfDate = LocalDate.parse(asOfDateStr)
                        val periodType = dataPoint.periodType ?: "UNKNOWN"
                        val value = dataPoint.reportedValue?.doubleValue
                        val currencyCode = dataPoint.currencyCode ?: "USD"

                        TimeseriesDataPoint(
                            asOfDate = asOfDate,
                            periodType = periodType,
                            value = value,
                            currencyCode = currencyCode
                        )
                    } catch (e: Exception) {
                        logger.warn("Failed to parse data point for type {}: {}", fieldName, e.message)
                        null
                    }
                }.sorted() // asOfDate 기준 정렬

                if (timeseriesDataPoints.isNotEmpty()) {
                    dataMap[fundamentalsType] = timeseriesDataPoints
                }
            }
        }

        return FundamentalsTimeseriesResult(
            symbol = symbol,
            data = dataMap
        )
    }

    /**
     * Searches for financial instruments using the Yahoo Finance Lookup API.
     *
     * @param query Search keyword (cannot be empty)
     * @param type Type of financial instrument to search for (default: [LookupType.ALL])
     * @param count Maximum number of results to return (default: 25, range: 1-100)
     * @return [LookupResult] containing matching financial instruments
     * @throws ApiException If the API call fails, returns an error response, or if parameters are invalid
     */
    suspend fun lookup(
        query: String,
        type: LookupType = LookupType.ALL,
        count: Int = 25
    ): LookupResult {
        // 파라미터 검증
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            throw ApiException(
                errorCode = ErrorCode.INVALID_PARAMETER,
                message = "검색어는 빈 문자열일 수 없습니다",
                metadata = mapOf("query" to query)
            )
        }

        if (count !in 1..100) {
            throw ApiException(
                errorCode = ErrorCode.INVALID_PARAMETER,
                message = "count는 1-100 범위여야 합니다",
                metadata = mapOf("count" to count)
            )
        }

        logger.debug(
            "Calling Yahoo Finance Lookup API: query={}, type={}, count={}",
            trimmedQuery, type.apiValue, count
        )

        // Rate Limit 적용
        rateLimiter.acquire()

        // CRUMB 토큰 획득
        val crumb = authenticator.getCrumb()

        // API 요청
        val response = httpClient.get(YahooApiUrls.LOOKUP) {
            parameter("query", trimmedQuery)
            parameter("type", type.apiValue)
            parameter("count", count)
            parameter("crumb", crumb)
        }

        // HTTP 상태 코드 확인
        if (!response.status.isSuccess()) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Lookup API 요청 실패: HTTP ${response.status.value}",
                statusCode = response.status.value,
                metadata = mapOf(
                    "query" to trimmedQuery,
                    "type" to type.apiValue,
                    "count" to count
                )
            )
        }

        // 응답 파싱
        val responseBody = response.body<String>()
        val lookupResponse = json.decodeFromString<LookupResponse>(responseBody)

        // 에러 응답 확인
        if (lookupResponse.finance.error != null) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Lookup API 에러: ${lookupResponse.finance.error?.description ?: "Unknown error"}",
                metadata = mapOf(
                    "query" to trimmedQuery,
                    "errorCode" to (lookupResponse.finance.error?.code ?: "UNKNOWN")
                )
            )
        }

        // 결과 확인 및 변환
        if (lookupResponse.finance.result.isNullOrEmpty()) {
            // 빈 결과는 에러가 아니라 빈 LookupResult 반환
            return LookupResult(
                query = trimmedQuery,
                type = type,
                count = 0,
                start = 0,
                total = 0,
                documents = emptyList()
            )
        }

        // LookupResultResponse를 LookupResult로 변환
        return convertToLookupResult(lookupResponse.finance.result.first(), trimmedQuery, type)
    }

    /**
     * Internal LookupResultResponse를 public LookupResult로 변환합니다.
     *
     * @param resultResponse 내부 API 응답 결과
     * @param query 검색 키워드
     * @param type 검색 타입
     * @return LookupResult
     */
    private fun convertToLookupResult(
        resultResponse: com.ulalax.ufc.infrastructure.yahoo.internal.response.LookupResultResponse,
        query: String,
        type: LookupType
    ): LookupResult {
        val documents = resultResponse.documents?.mapNotNull { doc ->
            // symbol은 필수 필드
            val symbol = doc.symbol
            // name은 shortName 또는 name 필드에서 가져옴
            val name = doc.shortName ?: doc.name

            if (symbol.isNullOrBlank() || name.isNullOrBlank()) {
                logger.warn("Skipping document with missing symbol or name: {}", doc)
                return@mapNotNull null
            }

            LookupDocument(
                symbol = symbol,
                name = name,
                exchange = doc.exchange ?: doc.exch,
                exchangeDisplay = doc.exchDisp,
                typeCode = doc.quoteType ?: doc.type,
                typeDisplay = doc.typeDisp,
                industry = doc.industry,
                industryDisplay = doc.industryName ?: doc.industryDisp,
                sector = doc.sector,
                sectorDisplay = doc.sectorDisp,
                score = doc.rank?.toDouble() ?: doc.score,
                isYahooFinance = doc.isYahooFinance
            )
        } ?: emptyList()

        return LookupResult(
            query = query,
            type = type,
            count = documents.size, // 실제 반환된 documents 개수
            start = resultResponse.start ?: 0,
            total = resultResponse.total ?: documents.size,
            documents = documents
        )
    }

    /**
     * Fetches market summary information from the Yahoo Finance Market Summary API.
     *
     * @param market The [MarketCode] to query (e.g., US, KR, JP)
     * @return [MarketSummaryResult] containing market summary data including indices and major stocks
     * @throws ApiException If the API call fails or returns an error response
     */
    suspend fun marketSummary(market: MarketCode): MarketSummaryResult {
        logger.debug("Calling Yahoo Finance Market Summary API: market={}", market.code)

        // Rate Limit 적용
        rateLimiter.acquire()

        // CRUMB 토큰 획득
        val crumb = authenticator.getCrumb()

        // API 요청
        val response = httpClient.get(YahooApiUrls.MARKET_SUMMARY) {
            parameter("market", market.code)
            parameter("crumb", crumb)
        }

        // HTTP 상태 코드 확인
        if (!response.status.isSuccess()) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Market Summary API 요청 실패: HTTP ${response.status.value}",
                statusCode = response.status.value,
                metadata = mapOf("market" to market.code)
            )
        }

        // 응답 파싱
        val responseBody = response.body<String>()
        val marketSummaryResponse = try {
            json.decodeFromString<MarketSummaryResponse>(responseBody)
        } catch (e: Exception) {
            throw DataParsingException(
                errorCode = ErrorCode.DATA_PARSING_ERROR,
                message = "Market Summary 응답 파싱 실패: ${e.message}",
                metadata = mapOf("market" to market.code)
            )
        }

        // 에러 응답 확인
        if (marketSummaryResponse.marketSummaryResponse.error != null) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Market Summary API 에러: ${marketSummaryResponse.marketSummaryResponse.error?.description ?: "Unknown error"}",
                metadata = mapOf(
                    "market" to market.code,
                    "errorCode" to (marketSummaryResponse.marketSummaryResponse.error?.code ?: "UNKNOWN")
                )
            )
        }

        // 결과 확인
        val result = marketSummaryResponse.marketSummaryResponse.result
        if (result.isNullOrEmpty()) {
            throw ApiException(
                errorCode = ErrorCode.DATA_NOT_FOUND,
                message = "Market Summary 데이터를 찾을 수 없습니다: ${market.code}",
                metadata = mapOf("market" to market.code)
            )
        }

        // Domain 모델로 변환
        val items = result.map { item ->
            MarketSummaryItem(
                exchange = item.exchange,
                symbol = item.symbol,
                shortName = item.shortName,
                regularMarketPrice = item.regularMarketPriceValue,
                regularMarketChange = item.regularMarketChangeValue,
                regularMarketChangePercent = item.regularMarketChangePercentValue,
                regularMarketTime = item.regularMarketTimeLong?.let {
                    if (it > 0) Instant.ofEpochSecond(it) else null
                },
                regularMarketDayHigh = item.regularMarketDayHighValue,
                regularMarketDayLow = item.regularMarketDayLowValue,
                regularMarketVolume = item.regularMarketVolumeValue,
                regularMarketPreviousClose = item.regularMarketPreviousCloseValue,
                currency = item.currency,
                marketState = MarketState.fromValue(item.marketState),
                quoteType = item.quoteType,
                timezoneName = item.exchangeTimezoneName,
                timezoneShortName = item.exchangeTimezoneShortName,
                gmtOffsetMillis = item.gmtOffSetMillisecondsValue
            )
        }

        return MarketSummaryResult(
            market = market,
            items = items
        )
    }

    /**
     * Fetches market time information from the Yahoo Finance Market Time API.
     *
     * @param market The [MarketCode] to query (e.g., US, KR, JP)
     * @return [MarketTimeResult] containing market hours, timezone, and current market state
     * @throws ApiException If the API call fails or returns an error response
     */
    suspend fun marketTime(market: MarketCode): MarketTimeResult {
        logger.debug("Calling Yahoo Finance Market Time API: market={}", market.code)

        // Rate Limit 적용
        rateLimiter.acquire()

        // CRUMB 토큰 획득
        val crumb = authenticator.getCrumb()

        // API 요청
        val response = httpClient.get(YahooApiUrls.MARKET_TIME) {
            parameter("market", market.code)
            parameter("crumb", crumb)
        }

        // HTTP 상태 코드 확인
        if (!response.status.isSuccess()) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Market Time API 요청 실패: HTTP ${response.status.value}",
                statusCode = response.status.value,
                metadata = mapOf("market" to market.code)
            )
        }

        // 응답 파싱
        val responseBody = response.body<String>()
        val marketTimeResponse = try {
            json.decodeFromString<MarketTimeResponse>(responseBody)
        } catch (e: Exception) {
            throw DataParsingException(
                errorCode = ErrorCode.DATA_PARSING_ERROR,
                message = "Market Time 응답 파싱 실패: ${e.message}",
                metadata = mapOf("market" to market.code)
            )
        }

        // 에러 응답 확인
        if (marketTimeResponse.finance.error != null) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Market Time API 에러: ${marketTimeResponse.finance.error?.description ?: "Unknown error"}",
                metadata = mapOf(
                    "market" to market.code,
                    "errorCode" to (marketTimeResponse.finance.error?.code ?: "UNKNOWN")
                )
            )
        }

        // 결과 확인
        val marketTimes = marketTimeResponse.finance.marketTimes
        if (marketTimes.isNullOrEmpty() || marketTimes.first().marketTime.isEmpty()) {
            throw ApiException(
                errorCode = ErrorCode.DATA_NOT_FOUND,
                message = "Market Time 데이터를 찾을 수 없습니다: ${market.code}",
                metadata = mapOf("market" to market.code)
            )
        }

        val item = marketTimes.first().marketTime.first()

        // 필수 필드 확인
        if (item.exchange == null || item.market == null || item.marketState == null ||
            item.open == null || item.close == null) {
            throw DataParsingException(
                errorCode = ErrorCode.DATA_PARSING_ERROR,
                message = "Market Time 필수 정보가 누락되었습니다",
                metadata = mapOf(
                    "market" to market.code,
                    "hasExchange" to (item.exchange != null).toString(),
                    "hasMarket" to (item.market != null).toString(),
                    "hasMarketState" to (item.marketState != null).toString(),
                    "hasOpen" to (item.open != null).toString(),
                    "hasClose" to (item.close != null).toString()
                )
            )
        }

        // Timezone 정보 확인
        if (item.timezone.isNullOrEmpty()) {
            throw DataParsingException(
                errorCode = ErrorCode.DATA_PARSING_ERROR,
                message = "타임존 정보가 없습니다",
                metadata = mapOf("market" to market.code)
            )
        }

        val timezoneInfo = item.timezone.first()

        // 타임존 필수 필드 확인
        if (timezoneInfo.short == null || timezoneInfo.name == null || timezoneInfo.gmtoffset == null) {
            throw DataParsingException(
                errorCode = ErrorCode.DATA_PARSING_ERROR,
                message = "타임존 필수 정보가 누락되었습니다",
                metadata = mapOf(
                    "market" to market.code,
                    "hasShort" to (timezoneInfo.short != null).toString(),
                    "hasName" to (timezoneInfo.name != null).toString(),
                    "hasGmtOffset" to (timezoneInfo.gmtoffset != null).toString()
                )
            )
        }

        // ISO 8601 문자열을 Instant로 변환
        fun parseInstant(isoString: String, fieldName: String): Instant {
            return try {
                Instant.parse(isoString)
            } catch (e: Exception) {
                throw DataParsingException(
                    errorCode = ErrorCode.DATA_PARSING_ERROR,
                    message = "시간 정보 파싱 실패: $fieldName",
                    metadata = mapOf("market" to market.code, "value" to isoString)
                )
            }
        }

        // Domain 모델로 변환
        return MarketTimeResult(
            market = market,
            exchange = item.exchange,
            marketIdentifier = item.market,
            marketState = MarketState.fromValue(item.marketState),
            open = parseInstant(item.open, "open"),
            close = parseInstant(item.close, "close"),
            preMarket = item.preMarket?.let {
                TradingHours(
                    start = parseInstant(it.start, "preMarket.start"),
                    end = parseInstant(it.end, "preMarket.end")
                )
            },
            postMarket = item.postMarket?.let {
                TradingHours(
                    start = parseInstant(it.start, "postMarket.start"),
                    end = parseInstant(it.end, "postMarket.end")
                )
            },
            timezone = MarketTimezone(
                shortName = timezoneInfo.short,
                ianaName = timezoneInfo.name,
                gmtOffsetMillis = timezoneInfo.gmtoffset
            ),
            currentTime = item.time?.let { parseInstant(it, "time") }
        )
    }

    /**
     * Fetches options chain data from the Yahoo Finance Options API.
     *
     * @param symbol The stock symbol to query (e.g., "AAPL", "TSLA")
     * @param expirationDate Optional expiration date as Unix timestamp in seconds. If null, returns the nearest expiration date.
     * @return [OptionsData] containing options chain with calls, puts, and underlying quote information
     * @throws ApiException If the API call fails or returns an error response
     */
    suspend fun options(
        symbol: String,
        expirationDate: Long? = null
    ): OptionsData {
        logger.debug(
            "Calling Yahoo Finance Options API: symbol={}, expirationDate={}",
            symbol, expirationDate
        )

        // Rate Limit 적용
        rateLimiter.acquire()

        // CRUMB 토큰 획득
        val crumb = authenticator.getCrumb()

        // API 요청
        val response = httpClient.get("${YahooApiUrls.OPTIONS}/$symbol") {
            parameter("crumb", crumb)
            expirationDate?.let { parameter("date", it) }
        }

        // HTTP 상태 코드 확인
        if (!response.status.isSuccess()) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Options API 요청 실패: HTTP ${response.status.value}",
                statusCode = response.status.value,
                metadata = mapOf(
                    "symbol" to symbol,
                    "expirationDate" to (expirationDate?.toString() ?: "null")
                )
            )
        }

        // 응답 파싱
        val responseBody = response.body<String>()
        val optionsResponse = json.decodeFromString<OptionsResponse>(responseBody)

        // 에러 응답 확인
        if (optionsResponse.optionChain.error != null) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Options API 에러: ${optionsResponse.optionChain.error?.description ?: "Unknown error"}",
                metadata = mapOf(
                    "symbol" to symbol,
                    "errorCode" to (optionsResponse.optionChain.error?.code ?: "UNKNOWN")
                )
            )
        }

        // 결과 확인
        if (optionsResponse.optionChain.result.isNullOrEmpty()) {
            throw ApiException(
                errorCode = ErrorCode.DATA_NOT_FOUND,
                message = "옵션 데이터를 찾을 수 없습니다: $symbol",
                metadata = mapOf("symbol" to symbol)
            )
        }

        // OptionsResult를 OptionsData로 변환
        val optionsResult = optionsResponse.optionChain.result.first()
        return convertToOptionsData(optionsResult)
    }

    /**
     * Internal OptionsResult를 public OptionsData로 변환합니다.
     *
     * @param result 내부 API 응답 결과
     * @return OptionsData
     */
    private fun convertToOptionsData(
        result: com.ulalax.ufc.infrastructure.yahoo.internal.response.OptionsResult
    ): OptionsData {
        // 기초 자산 정보 변환
        val underlyingQuote = result.quote?.let { convertUnderlyingQuote(it) }

        // 옵션 체인 변환 (첫 번째 만기일의 옵션)
        val optionsChain = if (result.options.isNotEmpty()) {
            convertOptionsChain(result.options.first())
        } else {
            // 옵션 데이터가 없는 경우 빈 체인 반환
            OptionsChain(
                expirationDate = result.expirationDates.firstOrNull() ?: 0L,
                hasMiniOptions = result.hasMiniOptions,
                calls = emptyList(),
                puts = emptyList()
            )
        }

        return OptionsData(
            underlyingSymbol = result.underlyingSymbol,
            expirationDates = result.expirationDates,
            strikes = result.strikes,
            hasMiniOptions = result.hasMiniOptions,
            underlyingQuote = underlyingQuote,
            optionsChain = optionsChain
        )
    }

    /**
     * Internal UnderlyingQuoteResponse를 public UnderlyingQuote로 변환합니다.
     */
    private fun convertUnderlyingQuote(
        response: com.ulalax.ufc.infrastructure.yahoo.internal.response.UnderlyingQuoteResponse
    ): UnderlyingQuote {
        return UnderlyingQuote(
            symbol = response.symbol,
            shortName = response.shortName,
            regularMarketPrice = response.regularMarketPrice,
            regularMarketChange = response.regularMarketChange,
            regularMarketChangePercent = response.regularMarketChangePercent,
            regularMarketVolume = response.regularMarketVolume,
            regularMarketTime = response.regularMarketTime
        )
    }

    /**
     * Internal OptionsChainResponse를 public OptionsChain으로 변환합니다.
     */
    private fun convertOptionsChain(
        response: com.ulalax.ufc.infrastructure.yahoo.internal.response.OptionsChainResponse
    ): OptionsChain {
        return OptionsChain(
            expirationDate = response.expirationDate,
            hasMiniOptions = response.hasMiniOptions,
            calls = response.calls.map { convertOptionContract(it) },
            puts = response.puts.map { convertOptionContract(it) }
        )
    }

    /**
     * Internal OptionContractResponse를 public OptionContract로 변환합니다.
     */
    private fun convertOptionContract(
        response: com.ulalax.ufc.infrastructure.yahoo.internal.response.OptionContractResponse
    ): OptionContract {
        return OptionContract(
            contractSymbol = response.contractSymbol,
            strike = response.strike,
            currency = response.currency,
            lastPrice = response.lastPrice,
            change = response.change,
            percentChange = response.percentChange,
            volume = response.volume,
            openInterest = response.openInterest,
            bid = response.bid,
            ask = response.ask,
            contractSize = response.contractSize,
            expiration = response.expiration,
            lastTradeDate = response.lastTradeDate,
            impliedVolatility = response.impliedVolatility,
            inTheMoney = response.inTheMoney
        )
    }

    /**
     * Fetches real-time market data for a single symbol from the Yahoo Finance Quote API.
     *
     * @param symbol The stock symbol to query (e.g., "AAPL", "GOOGL")
     * @return [QuoteData] containing real-time market data including price, volume, and market state
     * @throws ApiException If the API call fails or returns an error response
     */
    suspend fun quote(symbol: String): QuoteData {
        logger.debug("Calling Yahoo Finance Quote API: symbol={}", symbol)

        // Rate Limit 적용
        rateLimiter.acquire()

        // CRUMB 토큰 획득
        val crumb = authenticator.getCrumb()

        // API 요청
        val response = httpClient.get(YahooApiUrls.QUOTE) {
            parameter("symbols", symbol)
            parameter("formatted", "false")
            parameter("crumb", crumb)
        }

        // HTTP 상태 코드 확인
        if (!response.status.isSuccess()) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Quote API 요청 실패: HTTP ${response.status.value}",
                statusCode = response.status.value,
                metadata = mapOf("symbol" to symbol)
            )
        }

        // 응답 파싱
        val responseBody = response.body<String>()
        val quoteResponse = json.decodeFromString<QuoteApiResponse>(responseBody)

        // 에러 응답 확인
        if (quoteResponse.quoteResponse.error != null) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Quote API 에러: ${quoteResponse.quoteResponse.error?.description ?: "Unknown error"}",
                metadata = mapOf(
                    "symbol" to symbol,
                    "errorCode" to (quoteResponse.quoteResponse.error?.code ?: "UNKNOWN")
                )
            )
        }

        // 결과 확인
        if (quoteResponse.quoteResponse.result.isNullOrEmpty()) {
            throw ApiException(
                errorCode = ErrorCode.INVALID_SYMBOL,
                message = "Quote 데이터를 찾을 수 없습니다: $symbol",
                metadata = mapOf("symbol" to symbol)
            )
        }

        // QuoteResult를 QuoteData로 변환
        return convertToQuoteData(quoteResponse.quoteResponse.result.first())
    }

    /**
     * Fetches real-time market data for multiple symbols from the Yahoo Finance Quote API.
     *
     * @param symbols List of stock symbols to query (e.g., listOf("AAPL", "GOOGL", "MSFT"))
     * @return List of [QuoteData] containing real-time market data for each symbol. Symbols that fail to retrieve will be omitted.
     * @throws ApiException If the API call fails or returns an error response
     */
    suspend fun quote(symbols: List<String>): List<QuoteData> {
        if (symbols.isEmpty()) {
            return emptyList()
        }

        logger.debug("Calling Yahoo Finance Quote API: symbols={}", symbols)

        // Rate Limit 적용
        rateLimiter.acquire()

        // CRUMB 토큰 획득
        val crumb = authenticator.getCrumb()

        // API 요청 (쉼표로 구분된 심볼들)
        val response = httpClient.get(YahooApiUrls.QUOTE) {
            parameter("symbols", symbols.joinToString(","))
            parameter("formatted", "false")
            parameter("crumb", crumb)
        }

        // HTTP 상태 코드 확인
        if (!response.status.isSuccess()) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Quote API 요청 실패: HTTP ${response.status.value}",
                statusCode = response.status.value,
                metadata = mapOf("symbols" to symbols.joinToString(","))
            )
        }

        // 응답 파싱
        val responseBody = response.body<String>()
        val quoteResponse = json.decodeFromString<QuoteApiResponse>(responseBody)

        // 에러 응답 확인
        if (quoteResponse.quoteResponse.error != null) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Quote API 에러: ${quoteResponse.quoteResponse.error?.description ?: "Unknown error"}",
                metadata = mapOf(
                    "symbols" to symbols.joinToString(","),
                    "errorCode" to (quoteResponse.quoteResponse.error?.code ?: "UNKNOWN")
                )
            )
        }

        // 결과 확인 및 변환 (다중 심볼의 경우 성공한 것만 반환)
        val results = quoteResponse.quoteResponse.result ?: emptyList()

        // 일부 심볼이 실패한 경우 경고 로그
        if (results.size < symbols.size) {
            val foundSymbols = results.map { it.symbol }.toSet()
            val missingSymbols = symbols.filterNot { it in foundSymbols }
            logger.warn("일부 심볼의 데이터를 찾을 수 없습니다: {}", missingSymbols)
        }

        return results.map { convertToQuoteData(it) }
    }

    /**
     * Internal QuoteResult를 public QuoteData로 변환합니다.
     *
     * @param result 내부 API 응답 결과
     * @return QuoteData
     */
    private fun convertToQuoteData(
        result: com.ulalax.ufc.infrastructure.yahoo.internal.response.QuoteResult
    ): QuoteData {
        // Identification
        val identification = QuoteIdentification(
            symbol = result.symbol,
            longName = result.longName,
            shortName = result.shortName,
            exchange = result.exchange,
            timezoneName = result.exchangeTimezoneName,
            timezoneShortName = result.exchangeTimezoneShortName,
            quoteType = result.quoteType,
            currency = result.currency,
            market = result.market
        )

        // Pricing
        val pricing = QuotePricing(
            price = result.regularMarketPrice,
            open = result.regularMarketOpen,
            dayHigh = result.regularMarketDayHigh,
            dayLow = result.regularMarketDayLow,
            volume = result.regularMarketVolume,
            previousClose = result.regularMarketPreviousClose,
            change = result.regularMarketChange,
            changePercent = result.regularMarketChangePercent,
            marketTime = result.regularMarketTime?.let { Instant.ofEpochSecond(it) }
        )

        // Extended Hours
        val extendedHours = if (
            result.preMarketPrice != null || result.postMarketPrice != null
        ) {
            QuoteExtendedHours(
                preMarketPrice = result.preMarketPrice,
                preMarketChange = result.preMarketChange,
                preMarketChangePercent = result.preMarketChangePercent,
                preMarketTime = result.preMarketTime?.let { Instant.ofEpochSecond(it) },
                postMarketPrice = result.postMarketPrice,
                postMarketChange = result.postMarketChange,
                postMarketChangePercent = result.postMarketChangePercent,
                postMarketTime = result.postMarketTime?.let { Instant.ofEpochSecond(it) }
            )
        } else null

        // 52-Week
        val fiftyTwoWeek = if (
            result.fiftyTwoWeekHigh != null || result.fiftyTwoWeekLow != null
        ) {
            QuoteFiftyTwoWeek(
                high = result.fiftyTwoWeekHigh,
                low = result.fiftyTwoWeekLow,
                highChange = result.fiftyTwoWeekHighChange,
                lowChange = result.fiftyTwoWeekLowChange,
                highChangePercent = result.fiftyTwoWeekHighChangePercent,
                lowChangePercent = result.fiftyTwoWeekLowChangePercent,
                range = result.fiftyTwoWeekRange
            )
        } else null

        // Moving Averages
        val movingAverages = if (
            result.fiftyDayAverage != null || result.twoHundredDayAverage != null
        ) {
            QuoteMovingAverages(
                fiftyDayAverage = result.fiftyDayAverage,
                fiftyDayChange = result.fiftyDayAverageChange,
                fiftyDayChangePercent = result.fiftyDayAverageChangePercent,
                twoHundredDayAverage = result.twoHundredDayAverage,
                twoHundredDayChange = result.twoHundredDayAverageChange,
                twoHundredDayChangePercent = result.twoHundredDayAverageChangePercent
            )
        } else null

        // Volumes
        val volumes = if (
            result.averageDailyVolume3Month != null || result.averageDailyVolume10Day != null
        ) {
            QuoteVolumes(
                averageDailyVolume3Month = result.averageDailyVolume3Month,
                averageDailyVolume10Day = result.averageDailyVolume10Day
            )
        } else null

        // Market Cap
        val marketCap = if (
            result.marketCap != null || result.sharesOutstanding != null
        ) {
            QuoteMarketCap(
                marketCap = result.marketCap,
                sharesOutstanding = result.sharesOutstanding
            )
        } else null

        // Dividends
        val dividends = if (
            result.dividendRate != null || result.dividendYield != null ||
            result.dividendDate != null || result.exDividendDate != null ||
            result.trailingAnnualDividendRate != null || result.trailingAnnualDividendYield != null
        ) {
            QuoteDividends(
                annualRate = result.dividendRate,
                yield = result.dividendYield,
                dividendDate = result.dividendDate?.let {
                    Instant.ofEpochSecond(it).atZone(ZoneOffset.UTC).toLocalDate()
                },
                exDividendDate = result.exDividendDate?.let {
                    Instant.ofEpochSecond(it).atZone(ZoneOffset.UTC).toLocalDate()
                },
                trailingRate = result.trailingAnnualDividendRate,
                trailingYield = result.trailingAnnualDividendYield
            )
        } else null

        // Financial Ratios
        val financialRatios = if (
            result.trailingPE != null || result.forwardPE != null ||
            result.priceToBook != null || result.priceToSales != null ||
            result.bookValue != null || result.earningsQuarterlyGrowth != null
        ) {
            QuoteFinancialRatios(
                trailingPE = result.trailingPE,
                forwardPE = result.forwardPE,
                priceToBook = result.priceToBook,
                priceToSales = result.priceToSales,
                bookValue = result.bookValue,
                earningsQuarterlyGrowth = result.earningsQuarterlyGrowth
            )
        } else null

        // Earnings
        val earnings = if (
            result.epsTrailingTwelveMonths != null || result.epsForward != null ||
            result.epsCurrentYear != null || result.earningsTimestamp != null ||
            result.earningsTimestampStart != null || result.earningsTimestampEnd != null
        ) {
            QuoteEarnings(
                epsTrailingTwelveMonths = result.epsTrailingTwelveMonths,
                epsForward = result.epsForward,
                epsCurrentYear = result.epsCurrentYear,
                earningsTimestamp = result.earningsTimestamp?.let { Instant.ofEpochSecond(it) },
                earningsTimestampStart = result.earningsTimestampStart?.let { Instant.ofEpochSecond(it) },
                earningsTimestampEnd = result.earningsTimestampEnd?.let { Instant.ofEpochSecond(it) }
            )
        } else null

        // Revenue
        val revenue = if (
            result.totalRevenue != null || result.revenuePerShare != null ||
            result.returnOnAssets != null || result.returnOnEquity != null ||
            result.profitMargins != null || result.grossMargins != null
        ) {
            QuoteRevenue(
                totalRevenue = result.totalRevenue,
                revenuePerShare = result.revenuePerShare,
                returnOnAssets = result.returnOnAssets,
                returnOnEquity = result.returnOnEquity,
                profitMargins = result.profitMargins,
                grossMargins = result.grossMargins
            )
        } else null

        // Financial Health
        val financialHealth = if (
            result.totalCash != null || result.totalCashPerShare != null ||
            result.totalDebt != null || result.debtToEquity != null ||
            result.currentRatio != null || result.quickRatio != null
        ) {
            QuoteFinancialHealth(
                totalCash = result.totalCash,
                totalCashPerShare = result.totalCashPerShare,
                totalDebt = result.totalDebt,
                debtToEquity = result.debtToEquity,
                currentRatio = result.currentRatio,
                quickRatio = result.quickRatio
            )
        } else null

        // Growth Rates
        val growthRates = if (
            result.revenueGrowth != null || result.earningsGrowth != null
        ) {
            QuoteGrowthRates(
                revenueGrowth = result.revenueGrowth,
                earningsGrowth = result.earningsGrowth
            )
        } else null

        // Analyst Ratings
        val analystRatings = if (
            result.targetHighPrice != null || result.targetLowPrice != null ||
            result.targetMeanPrice != null || result.targetMedianPrice != null ||
            result.recommendationMean != null || result.recommendationKey != null ||
            result.numberOfAnalystOpinions != null
        ) {
            QuoteAnalystRatings(
                targetHighPrice = result.targetHighPrice,
                targetLowPrice = result.targetLowPrice,
                targetMeanPrice = result.targetMeanPrice,
                targetMedianPrice = result.targetMedianPrice,
                recommendationMean = result.recommendationMean,
                recommendationKey = result.recommendationKey,
                numberOfAnalystOpinions = result.numberOfAnalystOpinions
            )
        } else null

        return QuoteData(
            identification = identification,
            pricing = pricing,
            extendedHours = extendedHours,
            fiftyTwoWeek = fiftyTwoWeek,
            movingAverages = movingAverages,
            volumes = volumes,
            marketCap = marketCap,
            dividends = dividends,
            financialRatios = financialRatios,
            earnings = earnings,
            revenue = revenue,
            financialHealth = financialHealth,
            growthRates = growthRates,
            analystRatings = analystRatings
        )
    }

    /**
     * Searches for stocks using a custom query with the Yahoo Finance Screener API.
     *
     * @param query [ScreenerQuery] containing custom filter conditions
     * @param sortField Field to sort results by (default: [ScreenerSortField.TICKER])
     * @param sortAsc Whether to sort in ascending order (default: false)
     * @param size Number of results to return (default: 100, max: 250)
     * @param offset Pagination offset for retrieving additional results (default: 0)
     * @return [ScreenerResult] containing matching stocks with their data
     * @throws ApiException If the API call fails or returns an error response
     */
    suspend fun screener(
        query: ScreenerQuery,
        sortField: ScreenerSortField = ScreenerSortField.TICKER,
        sortAsc: Boolean = false,
        size: Int = 100,
        offset: Int = 0
    ): ScreenerResult {
        logger.debug(
            "Calling Yahoo Finance Screener API (Custom): quoteType={}, sortField={}, size={}, offset={}",
            query.quoteType, sortField.apiValue, size, offset
        )

        // 파라미터 검증
        require(size in 1..250) { "size must be between 1 and 250, but was $size" }
        require(offset >= 0) { "offset must be >= 0, but was $offset" }

        // 쿼리 유효성 검사
        query.validate()

        // Rate Limit 적용
        rateLimiter.acquire()

        // CRUMB 토큰 획득
        val crumb = authenticator.getCrumb()

        // 요청 바디 생성
        val queryMap = query.toRequestBody()
        val queryJson = convertMapToJsonElement(queryMap)

        val requestBody = ScreenerRequest(
            query = queryJson,
            quoteType = query.quoteType,
            sortField = sortField.apiValue,
            sortType = if (sortAsc) "ASC" else "DESC",
            size = size,
            offset = offset
        )

        // API 요청 (POST)
        val response = httpClient.post(YahooApiUrls.SCREENER) {
            parameter("crumb", crumb)
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        // HTTP 상태 코드 확인
        if (!response.status.isSuccess()) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Screener API 요청 실패: HTTP ${response.status.value}",
                statusCode = response.status.value,
                metadata = mapOf(
                    "quoteType" to query.quoteType,
                    "sortField" to sortField.apiValue
                )
            )
        }

        // 응답 파싱
        val responseBody = response.body<String>()
        val screenerResponse = json.decodeFromString<ScreenerResponse>(responseBody)

        // 에러 응답 확인
        if (screenerResponse.finance.error != null) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Screener API 에러: ${screenerResponse.finance.error.description}",
                metadata = mapOf("errorCode" to screenerResponse.finance.error.code)
            )
        }

        // 결과 확인
        if (screenerResponse.finance.result.isNullOrEmpty()) {
            throw ApiException(
                errorCode = ErrorCode.DATA_NOT_FOUND,
                message = "Screener 결과를 찾을 수 없습니다",
                metadata = mapOf("quoteType" to query.quoteType)
            )
        }

        // ScreenerResult로 변환
        return convertToScreenerResult(screenerResponse.finance.result.first())
    }

    /**
     * Searches for stocks using a predefined screener query with the Yahoo Finance Screener API.
     *
     * @param predefinedId Predefined screener ID (e.g., "day_gainers", "most_actives")
     * @param count Number of results to return (range: 1-250)
     * @param sortField Custom sort field (if null, uses the predefined screener's default)
     * @param sortAsc Custom sort direction (if null, uses the predefined screener's default)
     * @return [ScreenerResult] containing matching stocks with their data
     * @throws ApiException If the API call fails or returns an error response
     */
    suspend fun screener(
        predefinedId: String,
        count: Int = 25,
        sortField: ScreenerSortField? = null,
        sortAsc: Boolean? = null
    ): ScreenerResult {
        logger.debug(
            "Calling Yahoo Finance Screener API (Predefined): scrIds={}, count={}",
            predefinedId, count
        )

        // 파라미터 검증
        require(count in 1..250) { "count must be between 1 and 250, but was $count" }

        // Rate Limit 적용
        rateLimiter.acquire()

        // CRUMB 토큰 획득
        val crumb = authenticator.getCrumb()

        // API 요청 (GET)
        val response = httpClient.get(YahooApiUrls.SCREENER_PREDEFINED) {
            parameter("scrIds", predefinedId)
            parameter("count", count)
            parameter("crumb", crumb)
            if (sortField != null) {
                parameter("sortField", sortField.apiValue)
            }
            if (sortAsc != null) {
                parameter("sortType", if (sortAsc) "ASC" else "DESC")
            }
        }

        // HTTP 상태 코드 확인
        if (!response.status.isSuccess()) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Screener API 요청 실패: HTTP ${response.status.value}",
                statusCode = response.status.value,
                metadata = mapOf("predefinedId" to predefinedId)
            )
        }

        // 응답 파싱
        val responseBody = response.body<String>()
        val screenerResponse = json.decodeFromString<ScreenerResponse>(responseBody)

        // 에러 응답 확인
        if (screenerResponse.finance.error != null) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Screener API 에러: ${screenerResponse.finance.error.description}",
                metadata = mapOf("errorCode" to screenerResponse.finance.error.code)
            )
        }

        // 결과 확인
        if (screenerResponse.finance.result.isNullOrEmpty()) {
            throw ApiException(
                errorCode = ErrorCode.DATA_NOT_FOUND,
                message = "Screener 결과를 찾을 수 없습니다",
                metadata = mapOf("predefinedId" to predefinedId)
            )
        }

        // ScreenerResult로 변환
        return convertToScreenerResult(screenerResponse.finance.result.first())
    }

    /**
     * Searches for stocks using a predefined screener query with the Yahoo Finance Screener API.
     *
     * @param predefined [PredefinedScreener] enum value (e.g., DAY_GAINERS, MOST_ACTIVES)
     * @param count Number of results to return (range: 1-250)
     * @param sortField Custom sort field (if null, uses the predefined screener's default)
     * @param sortAsc Custom sort direction (if null, uses the predefined screener's default)
     * @return [ScreenerResult] containing matching stocks with their data
     * @throws ApiException If the API call fails or returns an error response
     */
    suspend fun screener(
        predefined: PredefinedScreener,
        count: Int = 25,
        sortField: ScreenerSortField? = null,
        sortAsc: Boolean? = null
    ): ScreenerResult {
        return screener(
            predefinedId = predefined.apiId,
            count = count,
            sortField = sortField ?: predefined.defaultSortField,
            sortAsc = sortAsc ?: predefined.defaultSortAsc
        )
    }

    /**
     * Internal ScreenerApiResult를 public ScreenerResult로 변환합니다.
     *
     * @param apiResult 내부 API 응답 결과
     * @return ScreenerResult
     */
    private fun convertToScreenerResult(
        apiResult: com.ulalax.ufc.infrastructure.yahoo.internal.response.ScreenerApiResult
    ): ScreenerResult {
        val quotes = apiResult.quotes.mapNotNull { quoteMap ->
            try {
                convertToScreenerQuote(quoteMap)
            } catch (e: Exception) {
                logger.warn("Failed to parse screener quote: ${e.message}")
                null
            }
        }

        return ScreenerResult(
            id = apiResult.id,
            title = apiResult.title,
            description = apiResult.description,
            count = apiResult.count,
            total = apiResult.total,
            start = apiResult.start,
            quotes = quotes
        )
    }

    /**
     * Map을 JsonElement로 변환하는 헬퍼 함수
     *
     * @param map 변환할 Map
     * @return JsonElement
     */
    private fun convertMapToJsonElement(map: Map<String, Any>): JsonElement {
        return buildJsonObject {
            map.forEach { (key, value) ->
                when (value) {
                    is String -> put(key, value)
                    is Number -> put(key, value)
                    is Boolean -> put(key, value)
                    is List<*> -> {
                        putJsonArray(key) {
                            value.forEach { item ->
                                when (item) {
                                    is Map<*, *> -> add(convertMapToJsonElement(item as Map<String, Any>))
                                    is String -> add(item)
                                    is Number -> add(item)
                                    is Boolean -> add(item)
                                    else -> add(JsonNull)
                                }
                            }
                        }
                    }
                    is Map<*, *> -> put(key, convertMapToJsonElement(value as Map<String, Any>))
                    else -> put(key, JsonNull)
                }
            }
        }
    }

    /**
     * Internal quote Map을 public ScreenerQuote로 변환합니다.
     *
     * @param quoteMap 내부 API 응답 quote
     * @return ScreenerQuote
     */
    private fun convertToScreenerQuote(quoteMap: Map<String, JsonElement>): ScreenerQuote {
        // symbol은 필수
        val symbol = quoteMap["symbol"]?.jsonPrimitive?.content
            ?: throw DataParsingException(
                errorCode = ErrorCode.DATA_PARSING_ERROR,
                message = "symbol 필드가 없습니다"
            )

        // 표준 필드 추출
        val shortName = quoteMap["shortname"]?.jsonPrimitive?.contentOrNull
        val longName = quoteMap["longname"]?.jsonPrimitive?.contentOrNull
        val quoteType = quoteMap["quoteType"]?.jsonPrimitive?.contentOrNull
        val sector = quoteMap["sector"]?.jsonPrimitive?.contentOrNull
        val industry = quoteMap["industry"]?.jsonPrimitive?.contentOrNull
        val exchange = quoteMap["exchange"]?.jsonPrimitive?.contentOrNull

        // 숫자 필드 추출
        val marketCap = quoteMap["marketCap"]?.jsonPrimitive?.longOrNull
        val regularMarketPrice = quoteMap["regularMarketPrice"]?.jsonPrimitive?.doubleOrNull
        val regularMarketChange = quoteMap["regularMarketChange"]?.jsonPrimitive?.doubleOrNull
        val regularMarketChangePercent = quoteMap["regularMarketChangePercent"]?.jsonPrimitive?.doubleOrNull
        val regularMarketVolume = quoteMap["regularMarketVolume"]?.jsonPrimitive?.longOrNull

        // 나머지는 additionalFields에 저장
        val standardFields = setOf(
            "symbol", "shortname", "longname", "quoteType", "sector", "industry", "exchange",
            "marketCap", "regularMarketPrice", "regularMarketChange", "regularMarketChangePercent", "regularMarketVolume"
        )

        val additionalFields = quoteMap
            .filterKeys { it !in standardFields }
            .mapValues { (_, value) ->
                when (value) {
                    is JsonNull -> null
                    is JsonPrimitive -> when {
                        value.isString -> value.content
                        else -> value.longOrNull ?: value.doubleOrNull ?: value.booleanOrNull
                    }
                    is JsonArray -> value.toString()
                    is JsonObject -> value.toString()
                    else -> null
                }
            }

        return ScreenerQuote(
            symbol = symbol,
            shortName = shortName,
            longName = longName,
            quoteType = quoteType,
            sector = sector,
            industry = industry,
            exchange = exchange,
            marketCap = marketCap,
            regularMarketPrice = regularMarketPrice,
            regularMarketChange = regularMarketChange,
            regularMarketChangePercent = regularMarketChangePercent,
            regularMarketVolume = regularMarketVolume,
            additionalFields = additionalFields
        )
    }

    /**
     * Searches for symbols and news using the Yahoo Finance Search API.
     *
     * @param query Search query string
     * @param quotesCount Number of quote results to return (default: 8)
     * @param newsCount Number of news results to return (default: 8)
     * @param enableFuzzyQuery Whether to enable fuzzy search matching (default: false)
     * @return [SearchResponse] containing matching quotes and news articles
     * @throws ApiException If the API call fails or returns an error response
     */
    suspend fun search(
        query: String,
        quotesCount: Int = 8,
        newsCount: Int = 8,
        enableFuzzyQuery: Boolean = false
    ): SearchResponse {
        // 파라미터 검증
        require(query.isNotBlank()) { "검색어는 비어있을 수 없습니다" }
        require(query.length <= 500) { "검색어는 500자를 초과할 수 없습니다" }
        require(quotesCount >= 0) { "quotesCount는 0 이상이어야 합니다" }
        require(newsCount >= 0) { "newsCount는 0 이상이어야 합니다" }

        logger.debug(
            "Calling Yahoo Finance Search API: query={}, quotesCount={}, newsCount={}, enableFuzzyQuery={}",
            query, quotesCount, newsCount, enableFuzzyQuery
        )

        // Rate Limit 적용
        rateLimiter.acquire()

        // CRUMB 토큰 획득
        val crumb = authenticator.getCrumb()

        // API 요청
        val response = httpClient.get(YahooApiUrls.SEARCH) {
            parameter("q", query)
            parameter("quotesCount", quotesCount)
            parameter("newsCount", newsCount)
            parameter("enableFuzzyQuery", enableFuzzyQuery)
            parameter("quotesQueryId", "tss_match_phrase_query")
            parameter("newsQueryId", "news_cie_vespa")
            parameter("crumb", crumb)
        }

        // HTTP 상태 코드 확인
        if (!response.status.isSuccess()) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Search API 요청 실패: HTTP ${response.status.value}",
                statusCode = response.status.value,
                metadata = mapOf("query" to query)
            )
        }

        // 응답 파싱
        val responseBody = response.body<String>()
        val searchApiResponse = json.decodeFromString<SearchApiResponse>(responseBody)

        // SearchApiResponse를 SearchResponse로 변환
        return convertToSearchResponse(query, searchApiResponse)
    }

    /**
     * Internal SearchApiResponse를 public SearchResponse로 변환합니다.
     *
     * @param query 검색어
     * @param response 내부 API 응답 결과
     * @return SearchResponse
     */
    private fun convertToSearchResponse(
        query: String,
        response: SearchApiResponse
    ): SearchResponse {
        // quotes 변환 (필수 필드 누락 시 제외)
        val quotes = response.quotes?.mapNotNull { quoteResult ->
            // 필수 필드 확인
            val symbol = quoteResult.symbol?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val quoteType = quoteResult.quoteType?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

            SearchQuote(
                symbol = symbol,
                shortName = quoteResult.shortname?.takeIf { it.isNotBlank() },
                longName = quoteResult.longname?.takeIf { it.isNotBlank() },
                quoteType = quoteType,
                exchange = quoteResult.exchange?.takeIf { it.isNotBlank() },
                exchangeDisplay = quoteResult.exchDisp?.takeIf { it.isNotBlank() },
                sector = quoteResult.sector?.takeIf { it.isNotBlank() },
                industry = quoteResult.industry?.takeIf { it.isNotBlank() },
                score = quoteResult.score ?: 0.0
            )
        } ?: emptyList()

        // news 변환 (필수 필드 누락 시 제외)
        val news = response.news?.mapNotNull { newsResult ->
            // 필수 필드 확인
            val uuid = newsResult.uuid?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val title = newsResult.title?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val link = newsResult.link?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

            SearchNews(
                uuid = uuid,
                title = title,
                publisher = newsResult.publisher?.takeIf { it.isNotBlank() },
                link = link,
                publishTime = newsResult.providerPublishTime ?: 0L,
                type = newsResult.type?.takeIf { it.isNotBlank() },
                thumbnail = newsResult.thumbnail?.let { convertNewsThumbnail(it) },
                relatedTickers = newsResult.relatedTickers ?: emptyList()
            )
        } ?: emptyList()

        return SearchResponse(
            query = query,
            count = response.count ?: 0,
            quotes = quotes,
            news = news
        )
    }

    /**
     * Internal NewsThumbnailResult를 public NewsThumbnail로 변환합니다.
     *
     * @param thumbnail 내부 썸네일 응답
     * @return NewsThumbnail
     */
    private fun convertNewsThumbnail(
        thumbnail: com.ulalax.ufc.infrastructure.yahoo.internal.response.NewsThumbnailResult
    ): NewsThumbnail {
        val resolutions = thumbnail.resolutions?.mapNotNull { resResult ->
            val url = resResult.url?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val width = resResult.width ?: return@mapNotNull null
            val height = resResult.height ?: return@mapNotNull null
            val tag = resResult.tag?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

            ThumbnailResolution(
                url = url,
                width = width,
                height = height,
                tag = tag
            )
        } ?: emptyList()

        return NewsThumbnail(resolutions = resolutions)
    }

    /**
     * Fetches earnings calendar data using the Yahoo Finance Visualization API.
     *
     * @param symbol The stock symbol to query (e.g., "AAPL", "TSLA")
     * @param limit Number of results to return (range: 1-100, default: 12)
     * @return [VisualizationEarningsCalendar] containing earnings dates with estimates and actuals
     * @throws ValidationException If parameter validation fails
     * @throws ApiException If the API call fails or returns an error response
     */
    suspend fun visualization(
        symbol: String,
        limit: Int = 12
    ): VisualizationEarningsCalendar {
        logger.debug("Calling Yahoo Finance Visualization API: symbol={}, limit={}", symbol, limit)

        // 파라미터 검증
        if (limit !in 1..100) {
            throw ValidationException(
                errorCode = ErrorCode.INVALID_PARAMETER,
                message = "limit는 1-100 범위여야 합니다: $limit",
                field = "limit",
                metadata = mapOf("limit" to limit.toString())
            )
        }

        // Rate Limit 적용
        rateLimiter.acquire()

        // CRUMB 토큰 획득
        val crumb = authenticator.getCrumb()

        // 요청 본문 생성
        val requestBody = VisualizationRequest(
            size = limit,
            query = VisualizationQuery(
                operator = "eq",
                operands = listOf("ticker", symbol)
            ),
            sortField = "startdatetime",
            sortType = "DESC",
            entityIdType = "earnings",
            includeFields = listOf(
                "startdatetime",
                "timeZoneShortName",
                "epsestimate",
                "epsactual",
                "epssurprisepct",
                "eventtype"
            )
        )

        // API 요청
        val response = httpClient.post(YahooApiUrls.VISUALIZATION) {
            contentType(ContentType.Application.Json)
            parameter("crumb", crumb)
            setBody(requestBody)
        }

        // HTTP 상태 코드 확인
        if (!response.status.isSuccess()) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Visualization API 요청 실패: HTTP ${response.status.value}",
                statusCode = response.status.value,
                metadata = mapOf("symbol" to symbol, "limit" to limit.toString())
            )
        }

        // 응답 파싱
        val responseBody = response.body<String>()
        val visualizationResponse = json.decodeFromString<VisualizationResponse>(responseBody)

        // 에러 응답 확인
        if (visualizationResponse.finance.error != null) {
            throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Visualization API 에러: ${visualizationResponse.finance.error?.description ?: "Unknown error"}",
                metadata = mapOf(
                    "symbol" to symbol,
                    "errorCode" to (visualizationResponse.finance.error?.code ?: "UNKNOWN")
                )
            )
        }

        // 결과 확인 및 변환
        val documents = visualizationResponse.finance.result?.firstOrNull()?.documents
        if (documents.isNullOrEmpty()) {
            // 빈 결과는 에러가 아니라 빈 목록 반환
            return VisualizationEarningsCalendar(
                symbol = symbol,
                earningsDates = emptyList()
            )
        }

        val document = documents.first()
        val earningsDates = document.rows.mapNotNull { row ->
            try {
                // 각 row는 [startdatetime, timeZoneShortName, epsestimate, epsactual, epssurprisepct, eventtype] 순서
                val startDatetimeStr = (row.getOrNull(0) as? JsonPrimitive)?.content
                val timezoneShortName = (row.getOrNull(1) as? JsonPrimitive)?.content
                val epsEstimate = (row.getOrNull(2) as? JsonPrimitive)?.double
                val epsActual = (row.getOrNull(3) as? JsonPrimitive)?.double
                val epsSurprisePct = (row.getOrNull(4) as? JsonPrimitive)?.double
                val eventTypeCode = (row.getOrNull(5) as? JsonPrimitive)?.int

                // startdatetime과 eventtype은 필수
                if (startDatetimeStr == null || eventTypeCode == null) {
                    return@mapNotNull null
                }

                // ISO 8601 파싱하여 epoch seconds로 변환
                val earningsDate = Instant.parse(startDatetimeStr).epochSecond

                // 0.0 값은 null로 처리
                val finalEpsEstimate = if (epsEstimate == 0.0) null else epsEstimate
                val finalEpsActual = if (epsActual == 0.0) null else epsActual
                val finalEpsSurprisePct = if (epsSurprisePct == 0.0) null else epsSurprisePct

                EarningsDate(
                    earningsDate = earningsDate,
                    timezoneShortName = timezoneShortName,
                    epsEstimate = finalEpsEstimate,
                    epsActual = finalEpsActual,
                    surprisePercent = finalEpsSurprisePct,
                    eventType = EarningsEventType.fromCode(eventTypeCode)
                )
            } catch (e: Exception) {
                logger.warn("Failed to parse earnings date row: $row", e)
                null
            }
        }

        return VisualizationEarningsCalendar(
            symbol = symbol,
            earningsDates = earningsDates
        )
    }

    /**
     * Closes the client and releases all resources.
     *
     * This method closes the underlying HTTP client and should be called when the client
     * is no longer needed to free up system resources.
     */
    override fun close() {
        httpClient.close()
        logger.info("YahooClient closed")
    }
}
