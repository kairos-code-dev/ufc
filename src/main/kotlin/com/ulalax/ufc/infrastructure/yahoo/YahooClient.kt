package com.ulalax.ufc.infrastructure.yahoo

import com.ulalax.ufc.domain.exception.ApiException
import com.ulalax.ufc.domain.exception.DataParsingException
import com.ulalax.ufc.domain.exception.ErrorCode
import com.ulalax.ufc.infrastructure.common.ratelimit.GlobalRateLimiters
import com.ulalax.ufc.infrastructure.common.ratelimit.RateLimiter
import com.ulalax.ufc.infrastructure.yahoo.internal.YahooApiUrls
import com.ulalax.ufc.infrastructure.yahoo.internal.auth.YahooAuthenticator
import com.ulalax.ufc.infrastructure.yahoo.internal.response.ChartDataResponse
import com.ulalax.ufc.infrastructure.yahoo.internal.response.QuoteSummaryResponse
import com.ulalax.ufc.domain.model.chart.*
import com.ulalax.ufc.domain.model.quote.*
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
     * 리소스를 정리합니다.
     */
    override fun close() {
        httpClient.close()
        logger.info("YahooClient closed")
    }
}
