package com.ulalax.ufc.infrastructure.yahoo

import com.ulalax.ufc.domain.exception.ApiException
import com.ulalax.ufc.domain.exception.DataParsingException
import com.ulalax.ufc.domain.exception.ErrorCode
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
import com.ulalax.ufc.infrastructure.yahoo.internal.response.LookupResponse
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
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Yahoo Finance API 클라이언트
 *
 * Yahoo Finance API에 접근하기 위한 간단한 클라이언트 인터페이스를 제공합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val yahoo = YahooClient.create()
 * val quote = yahoo.quoteSummary("AAPL", QuoteSummaryModule.PRICE)
 * val chart = yahoo.chart("AAPL", Interval.OneDay, Period.OneYear)
 * ```
 *
 * @property httpClient Ktor HTTP 클라이언트
 * @property authenticator Yahoo Finance 인증 관리자
 * @property rateLimiter Rate Limiter
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
         * YahooClient 인스턴스를 생성합니다.
         *
         * @param config 클라이언트 설정 (옵션)
         * @return YahooClient 인스턴스
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
     * QuoteSummary API를 호출하여 주식 정보를 조회합니다.
     *
     * @param symbol 조회할 심볼 (예: "AAPL")
     * @param modules 조회할 모듈들
     * @return QuoteSummaryModuleResult
     * @throws ApiException API 호출 실패 시
     */
    suspend fun quoteSummary(
        symbol: String,
        vararg modules: QuoteSummaryModule
    ): QuoteSummaryModuleResult {
        return quoteSummary(symbol, modules.toSet())
    }

    /**
     * QuoteSummary API를 호출하여 주식 정보를 조회합니다.
     *
     * @param symbol 조회할 심볼 (예: "AAPL")
     * @param modules 조회할 모듈 Set
     * @return QuoteSummaryModuleResult
     * @throws ApiException API 호출 실패 시
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
     * Chart API를 호출하여 차트 데이터를 조회합니다.
     *
     * @param symbol 조회할 심볼 (예: "AAPL")
     * @param interval 데이터 간격
     * @param period 조회 기간
     * @param events 조회할 이벤트 타입들 (옵션)
     * @return ChartData
     * @throws ApiException API 호출 실패 시
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
     * Earnings Calendar API를 호출하여 실적 발표 일정을 조회합니다.
     *
     * HTML 스크래핑을 통해 데이터를 수집하므로, Yahoo Finance 웹 페이지 구조 변경 시
     * 파싱이 실패할 수 있습니다.
     *
     * @param symbol 조회할 심볼 (예: "AAPL")
     * @param limit 조회할 레코드 수 (기본값: 12, 최대: 100)
     * @param offset 페이지네이션 오프셋 (기본값: 0)
     * @return EarningsCalendar
     * @throws ApiException API 호출 실패 시
     * @throws DataParsingException HTML 파싱 실패 시
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
     * Fundamentals Timeseries API를 호출하여 재무제표 시계열 데이터를 조회합니다.
     *
     * Yahoo Finance의 재무제표 항목별 시계열 데이터를 조회합니다.
     * 손익계산서, 대차대조표, 현금흐름표의 각 항목에 대한 연간, 분기별, Trailing 데이터를 제공합니다.
     *
     * @param symbol 조회할 심볼 (예: "AAPL")
     * @param types 조회할 재무 항목 타입 목록
     * @param startDate 조회 시작 날짜 (기본값: 5년 전)
     * @param endDate 조회 종료 날짜 (기본값: 오늘)
     * @return FundamentalsTimeseriesResult 타입별 시계열 데이터
     * @throws IllegalArgumentException symbol이 공백이거나 types가 빈 리스트인 경우
     * @throws ApiException API 호출 실패 시
     *
     * ## 사용 예시
     * ```kotlin
     * // 연간 매출과 분기별 순이익 조회
     * val result = yahooClient.fundamentalsTimeseries(
     *     "AAPL",
     *     listOf(
     *         FundamentalsType.ANNUAL_TOTAL_REVENUE,
     *         FundamentalsType.QUARTERLY_NET_INCOME
     *     )
     * )
     *
     * // 특정 기간 데이터 조회
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
     * Lookup API를 호출하여 금융상품을 검색합니다.
     *
     * @param query 검색 키워드 (빈 문자열 불가)
     * @param type 검색할 금융상품 타입 (기본값: ALL)
     * @param count 최대 결과 개수 (기본값: 25, 범위: 1-100)
     * @return LookupResult
     * @throws ApiException API 호출 실패 시
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
     * 리소스를 정리합니다.
     */
    override fun close() {
        httpClient.close()
        logger.info("YahooClient closed")
    }
}
