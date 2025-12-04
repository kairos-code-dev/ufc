package com.ulalax.ufc.api.client

import com.ulalax.ufc.domain.stock.StockService
import com.ulalax.ufc.domain.stock.StockHttpClient
import com.ulalax.ufc.domain.stock.StockServiceImpl
import com.ulalax.ufc.domain.stock.CompanyInfo
import com.ulalax.ufc.domain.stock.FastInfo
import com.ulalax.ufc.domain.stock.SharesData
import com.ulalax.ufc.domain.funds.FundsService
import com.ulalax.ufc.domain.funds.FundsHttpClient
import com.ulalax.ufc.domain.funds.FundsServiceImpl
import com.ulalax.ufc.domain.funds.FundData
import com.ulalax.ufc.domain.price.PriceService
import com.ulalax.ufc.domain.price.PriceHttpClient
import com.ulalax.ufc.domain.price.PriceServiceImpl
import com.ulalax.ufc.domain.price.PriceData
import com.ulalax.ufc.domain.price.OHLCV
import com.ulalax.ufc.domain.price.ChartMetadata
import com.ulalax.ufc.infrastructure.yahoo.YahooHttpClient
import com.ulalax.ufc.infrastructure.yahoo.YahooStockHttpClient
import com.ulalax.ufc.infrastructure.yahoo.YahooFundsHttpClient
import com.ulalax.ufc.infrastructure.yahoo.YahooCorpHttpClient
import com.ulalax.ufc.infrastructure.util.CacheHelper
import com.ulalax.ufc.domain.corp.CorpService
import com.ulalax.ufc.domain.corp.CorpHttpClient
import com.ulalax.ufc.domain.corp.CorpServiceImpl
import com.ulalax.ufc.domain.corp.DividendHistory
import com.ulalax.ufc.domain.corp.SplitHistory
import com.ulalax.ufc.domain.corp.CapitalGainHistory
import com.ulalax.ufc.domain.macro.MacroService
import com.ulalax.ufc.domain.macro.MacroHttpClient
import com.ulalax.ufc.domain.macro.MacroServiceImpl
import com.ulalax.ufc.domain.macro.MacroSeries
import com.ulalax.ufc.domain.macro.GDPType
import com.ulalax.ufc.domain.macro.InflationType
import com.ulalax.ufc.domain.macro.UnemploymentType
import com.ulalax.ufc.domain.macro.InterestRateType
import com.ulalax.ufc.infrastructure.fred.FredMacroHttpClient
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.infrastructure.ratelimit.TokenBucketRateLimiter
import com.ulalax.ufc.infrastructure.yahoo.auth.BasicAuthStrategy
import com.ulalax.ufc.domain.common.Interval
import com.ulalax.ufc.domain.common.Period
import com.ulalax.ufc.domain.chart.ChartData
import com.ulalax.ufc.domain.chart.ChartEventType
import com.ulalax.ufc.domain.quote.QuoteSummaryModule
import com.ulalax.ufc.domain.quote.QuoteSummaryModuleResult
import com.ulalax.ufc.domain.quote.toModuleResult
import io.ktor.client.HttpClient
import org.slf4j.LoggerFactory
import java.time.LocalDate

/**
 * UFC (Universal Financial Client) 클라이언트의 완전한 구현
 *
 * 이 클래스는 Yahoo Finance, FRED 등 여러 외부 금융 API와의 통신을 통합합니다.
 * 각 API별 서비스 레이어를 내부적으로 관리하고, 사용자에게 통일된 인터페이스를 제공합니다.
 *
 * 주요 기능:
 * - Yahoo Finance Chart API 지원 (OHLCV 데이터)
 * - Yahoo Finance QuoteSummary API 지원 (주식 요약 정보)
 * - Stock API (기업 정보, ISIN, 발행주식수)
 * - Funds API (ETF/뮤추얼펀드 정보)
 * - Price API (현재가, 가격 히스토리)
 * - Corp API (배당금, 주식분할, 자본이득)
 * - Macro API (거시경제 지표 - FRED)
 * - 자동 인증 (CRUMB 토큰 관리)
 * - Rate Limiting (API 속도 제어)
 * - 에러 처리 및 재시도 로직
 *
 * 사용 예시:
 * ```
 * // 클라이언트 생성
 * val client = UFCClient.create(
 *     UFCClientConfig(
 *         fredApiKey = "your-fred-api-key",
 *         rateLimitingSettings = RateLimitingSettings()
 *     )
 * )
 *
 * // Chart 데이터 조회
 * val chartData = client.getChartData("AAPL", Interval.OneDay, Period.OneYear)
 *
 * // 주식 정보 조회
 * val companyInfo = client.getCompanyInfo("AAPL")
 * val fastInfo = client.getFastInfo("AAPL")
 *
 * // 펀드 정보 조회
 * val fundData = client.getFundData("SPY")
 *
 * // 가격 정보 조회
 * val currentPrice = client.getCurrentPrice("AAPL")
 * val priceHistory = client.getPriceHistory("AAPL", Period.OneYear)
 *
 * // 기업 행동 조회
 * val dividends = client.getDividends("AAPL")
 *
 * // 거시경제 지표 조회 (FRED API 키 필요)
 * val gdp = client.getGDP()
 *
 * // 사용 후 닫기
 * client.close()
 * ```
 *
 * @property httpClient Ktor HttpClient 인스턴스
 * @property config UFC 클라이언트 설정
 * @property yahooRateLimiter Yahoo Finance API용 Rate Limiter
 * @property stockService Stock 정보 조회 서비스
 * @property fundsService Funds 정보 조회 서비스
 * @property priceService Price 정보 조회 서비스
 * @property corpService Corp 정보 조회 서비스
 * @property macroService Macro 정보 조회 서비스 (nullable - FRED API 키 필요)
 *
 * Note: Chart and Quote services are deprecated and will be refactored to clean architecture pattern.
 * Use price, stock, funds, corp services instead.
 */
class UfcClientImpl(
    private val httpClient: HttpClient,
    private val config: UfcClientConfig,
    private val yahooRateLimiter: TokenBucketRateLimiter,
    internal val stockService: StockService,
    internal val fundsService: FundsService,
    internal val priceService: PriceService,
    internal val corpService: CorpService,
    internal val macroService: MacroService?,
    private val priceHttpClient: PriceHttpClient
) : AutoCloseable {

    companion object {
        private val logger = LoggerFactory.getLogger(UfcClientImpl::class.java)

        /**
         * UFCClient를 생성합니다.
         *
         * 이 메서드는 다음을 수행합니다:
         * 1. Ktor HTTP 클라이언트 생성
         * 2. Yahoo Finance 인증 (CRUMB 토큰 획득)
         * 3. Rate Limiter 초기화
         * 4. 각 API 서비스 인스턴스 생성
         *
         * @param config UFC 클라이언트 설정
         * @return 생성된 UFCClient 인스턴스
         * @throws UfcException 인증 실패 시
         */
        suspend fun create(config: UfcClientConfig): UfcClientImpl {
            logger.info("Creating UFC client with config: fredApiKey provided={}", config.fredApiKey != null)

            // Ktor HTTP 클라이언트 생성
            val httpClient = com.ulalax.ufc.infrastructure.yahoo.YahooHttpClientFactory.create()

            return try {
                // Yahoo Finance 인증
                val authStrategy = BasicAuthStrategy(httpClient)
                val authResult = authStrategy.authenticate()
                logger.info("Yahoo Finance authentication successful")

                // Rate Limiter 생성
                val yahooRateLimiter = TokenBucketRateLimiter(
                    "YAHOO",
                    config.rateLimitingSettings.yahoo
                )
                logger.info("Yahoo Rate limiter initialized")

                // 공유 인프라 생성 (캐싱)
                val cache = CacheHelper()
                logger.info("Cache helper initialized")

                // Stock 도메인: StockHttpClient 인터페이스를 통한 의존성 주입
                val stockHttpClient: StockHttpClient = YahooStockHttpClient(httpClient, yahooRateLimiter, authResult)
                val stockService: StockService = StockServiceImpl(stockHttpClient, cache)
                logger.info("Stock service initialized")

                // Funds 도메인: FundsHttpClient 인터페이스를 통한 의존성 주입
                val fundsHttpClient: FundsHttpClient = YahooFundsHttpClient(httpClient, yahooRateLimiter, authResult)
                val fundsService: FundsService = FundsServiceImpl(fundsHttpClient, cache)
                logger.info("Funds service initialized")

                // Price 도메인: PriceHttpClient 인터페이스를 통한 의존성 주입
                val priceHttpClient: PriceHttpClient = YahooHttpClient(httpClient, yahooRateLimiter, authResult)
                val priceService: PriceService = PriceServiceImpl(priceHttpClient, cache)
                logger.info("Price service initialized")

                // Corp 도메인: CorpHttpClient 인터페이스를 통한 의존성 주입
                val corpHttpClient: CorpHttpClient = YahooCorpHttpClient(httpClient, yahooRateLimiter, authResult)
                val corpService: CorpService = CorpServiceImpl(corpHttpClient, cache)
                logger.info("Corp service initialized")

                // FRED 서비스 생성 (API 키가 있을 때만): MacroHttpClient 인터페이스를 통한 의존성 주입
                val macroService = config.fredApiKey?.let { apiKey ->
                    logger.info("Initializing FRED Macro service")
                    val fredRateLimiter = TokenBucketRateLimiter(
                        "FRED",
                        config.rateLimitingSettings.fred
                    )
                    val macroHttpClient: MacroHttpClient = FredMacroHttpClient(apiKey, httpClient, fredRateLimiter)
                    MacroServiceImpl(macroHttpClient, cache).also {
                        logger.info("FRED Macro service initialized")
                    }
                }

                val client = UfcClientImpl(
                    httpClient = httpClient,
                    config = config,
                    yahooRateLimiter = yahooRateLimiter,
                    stockService = stockService,
                    fundsService = fundsService,
                    priceService = priceService,
                    corpService = corpService,
                    macroService = macroService,
                    priceHttpClient = priceHttpClient
                )

                logger.info("UFC client created successfully")
                client

            } catch (e: Exception) {
                logger.error("Failed to create UFC client", e)
                httpClient.close()
                throw e
            }
        }
    }

    /**
     * UFCClient 리소스를 정리합니다.
     *
     * 내부의 HttpClient를 종료하고 관련 리소스를 해제합니다.
     */
    override fun close() {
        logger.info("Closing UFC client")
        try {
            httpClient.close()
            logger.info("UFC client closed successfully")
        } catch (e: Exception) {
            logger.warn("Error while closing UFC client", e)
            throw e
        }
    }

    // ========================================
    // Chart API Methods (DEPRECATED - will be refactored)
    // ========================================
    // TODO: Refactor Chart API to clean architecture pattern
    // These methods are temporarily removed. Use priceService.getPriceHistory() instead.

    // ========================================
    // Quote Summary API Methods (DEPRECATED - will be refactored)
    // ========================================
    // TODO: Refactor Quote API to clean architecture pattern
    // These methods are temporarily removed. Use stockService methods instead.

    // ========================================
    // Status and Configuration Methods
    // ========================================

    /**
     * Rate Limiter의 현재 상태를 반환합니다.
     *
     * @return Rate Limiter 상태 정보
     */
    fun getRateLimiterStatus() = yahooRateLimiter.getStatus()

    /**
     * 클라이언트의 현재 설정을 반환합니다.
     *
     * @return UFC 클라이언트 설정
     */
    fun getConfig() = config

    /**
     * 클라이언트의 상태 정보를 반환합니다.
     *
     * @return 상태 정보 문자열
     */
    fun getStatus(): String {
        val rateLimiterStatus = getRateLimiterStatus()
        return """
            UFC Client Status:
            - Rate Limiter: ${rateLimiterStatus.availableTokens}/${rateLimiterStatus.capacity} tokens
            - Refill Rate: ${rateLimiterStatus.refillRate}/sec
            - Enabled: ${rateLimiterStatus.isEnabled}
            - Estimated Wait Time: ${rateLimiterStatus.estimatedWaitTimeMs}ms
            - Macro Service: ${if (macroService != null) "Enabled" else "Disabled (No FRED API key)"}
        """.trimIndent()
    }

    // ========================================
    // Stock API Methods (ufc.stock)
    // ========================================

    /**
     * 회사 기본 정보를 조회합니다.
     *
     * @param symbol 조회할 심볼 (예: "AAPL")
     * @return 회사 기본 정보
     */
    suspend fun getCompanyInfo(symbol: String): CompanyInfo {
        logger.debug("Fetching company info: symbol={}", symbol)
        return stockService.getCompanyInfo(symbol)
    }

    /**
     * 다중 심볼의 회사 기본 정보를 조회합니다.
     *
     * @param symbols 심볼 목록
     * @return 심볼별 회사 정보 맵
     */
    suspend fun getCompanyInfo(symbols: List<String>): Map<String, CompanyInfo> {
        logger.debug("Fetching company info for {} symbols", symbols.size)
        return stockService.getCompanyInfo(symbols)
    }

    /**
     * 빠른 정보를 조회합니다 (최소 필드만 포함).
     *
     * @param symbol 조회할 심볼
     * @return 빠른 조회용 정보
     */
    suspend fun getFastInfo(symbol: String): FastInfo {
        logger.debug("Fetching fast info: symbol={}", symbol)
        return stockService.getFastInfo(symbol)
    }

    /**
     * 다중 심볼의 빠른 정보를 조회합니다.
     *
     * @param symbols 심볼 목록
     * @return 심볼별 빠른 정보 맵
     */
    suspend fun getFastInfo(symbols: List<String>): Map<String, FastInfo> {
        logger.debug("Fetching fast info for {} symbols", symbols.size)
        return stockService.getFastInfo(symbols)
    }

    /**
     * ISIN 코드를 조회합니다.
     *
     * @param symbol 조회할 심볼
     * @return ISIN 코드
     */
    suspend fun getIsin(symbol: String): String {
        logger.debug("Fetching ISIN: symbol={}", symbol)
        return stockService.getIsin(symbol)
    }

    /**
     * 다중 심볼의 ISIN 코드를 조회합니다.
     *
     * @param symbols 심볼 목록
     * @return 심볼별 ISIN 코드 맵
     */
    suspend fun getIsin(symbols: List<String>): Map<String, String> {
        logger.debug("Fetching ISIN for {} symbols", symbols.size)
        return stockService.getIsin(symbols)
    }

    /**
     * 발행주식수 히스토리를 조회합니다.
     *
     * @param symbol 조회할 심볼
     * @return 발행주식수 리스트
     */
    suspend fun getShares(symbol: String): List<SharesData> {
        logger.debug("Fetching shares: symbol={}", symbol)
        return stockService.getShares(symbol)
    }

    /**
     * 다중 심볼의 발행주식수 히스토리를 조회합니다.
     *
     * @param symbols 심볼 목록
     * @return 심볼별 발행주식수 리스트 맵
     */
    suspend fun getShares(symbols: List<String>): Map<String, List<SharesData>> {
        logger.debug("Fetching shares for {} symbols", symbols.size)
        return stockService.getShares(symbols)
    }

    /**
     * 지정된 기간의 발행주식수 상세 히스토리를 조회합니다.
     *
     * @param symbol 조회할 심볼
     * @param start 시작일 (null이면 최초 데이터부터)
     * @param end 종료일 (null이면 최신 데이터까지)
     * @return 기간별 발행주식수 리스트
     */
    suspend fun getSharesFull(
        symbol: String,
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<SharesData> {
        logger.debug("Fetching shares full: symbol={}, start={}, end={}", symbol, start, end)
        return stockService.getSharesFull(symbol, start, end)
    }

    // ========================================
    // Funds API Methods (ufc.funds)
    // ========================================

    /**
     * 펀드 데이터를 조회합니다.
     *
     * @param symbol 펀드 심볼 (예: "SPY", "AGG")
     * @return 펀드 데이터
     */
    suspend fun getFundData(symbol: String): FundData {
        logger.debug("Fetching fund data: symbol={}", symbol)
        return fundsService.getFundData(symbol)
    }

    /**
     * 다중 펀드의 데이터를 조회합니다.
     *
     * @param symbols 펀드 심볼 목록
     * @return 심볼별 펀드 데이터 맵
     */
    suspend fun getFundData(symbols: List<String>): Map<String, FundData> {
        logger.debug("Fetching fund data for {} symbols", symbols.size)
        return fundsService.getFundData(symbols)
    }

    /**
     * 주어진 심볼이 펀드(ETF 또는 MUTUALFUND)인지 확인합니다.
     *
     * @param symbol 조회할 심볼
     * @return true이면 펀드, false이면 다른 자산
     */
    suspend fun isFund(symbol: String): Boolean {
        logger.debug("Checking if fund: symbol={}", symbol)
        return fundsService.isFund(symbol)
    }

    // ========================================
    // Price API Methods (ufc.price)
    // ========================================

    /**
     * 현재 가격 정보를 조회합니다.
     *
     * @param symbol 조회할 심볼
     * @return 현재 가격 정보
     */
    suspend fun getCurrentPrice(symbol: String): PriceData {
        logger.debug("Fetching current price: symbol={}", symbol)
        return priceService.getCurrentPrice(symbol)
    }

    /**
     * 다중 심볼의 현재 가격 정보를 조회합니다.
     *
     * @param symbols 심볼 목록
     * @return 심볼별 가격 정보 맵
     */
    suspend fun getCurrentPrice(symbols: List<String>): Map<String, PriceData> {
        logger.debug("Fetching current price for {} symbols", symbols.size)
        return priceService.getCurrentPrice(symbols)
    }

    /**
     * 가격 히스토리를 조회합니다 (기간 기반).
     *
     * @param symbol 조회할 심볼
     * @param period 조회 기간
     * @param interval 데이터 간격 (기본값: OneDay)
     * @return OHLCV 시계열 데이터
     */
    suspend fun getPriceHistory(
        symbol: String,
        period: Period,
        interval: Interval = Interval.OneDay
    ): List<OHLCV> {
        logger.debug("Fetching price history: symbol={}, period={}, interval={}",
            symbol, period.value, interval.value)
        return priceService.getPriceHistory(symbol, period, interval)
    }

    /**
     * 가격 히스토리를 조회합니다 (날짜 범위 기반).
     *
     * @param symbol 조회할 심볼
     * @param start 시작 날짜
     * @param end 종료 날짜
     * @param interval 데이터 간격 (기본값: OneDay)
     * @return OHLCV 시계열 데이터
     */
    suspend fun getPriceHistory(
        symbol: String,
        start: LocalDate,
        end: LocalDate,
        interval: Interval = Interval.OneDay
    ): List<OHLCV> {
        logger.debug("Fetching price history: symbol={}, start={}, end={}, interval={}",
            symbol, start, end, interval.value)
        return priceService.getPriceHistory(symbol, start, end, interval)
    }

    /**
     * 가격 히스토리 및 메타데이터를 조회합니다.
     *
     * @param symbol 조회할 심볼
     * @param period 조회 기간
     * @param interval 데이터 간격 (기본값: OneDay)
     * @return (메타데이터, OHLCV 데이터) Pair
     */
    suspend fun getPriceHistoryWithMetadata(
        symbol: String,
        period: Period,
        interval: Interval = Interval.OneDay
    ): Pair<ChartMetadata, List<OHLCV>> {
        logger.debug("Fetching price history with metadata: symbol={}, period={}, interval={}",
            symbol, period.value, interval.value)
        return priceService.getPriceHistoryWithMetadata(symbol, period, interval)
    }

    /**
     * 히스토리 메타데이터만 조회합니다.
     *
     * @param symbol 조회할 심볼
     * @return 히스토리 메타데이터
     */
    suspend fun getHistoryMetadata(symbol: String): ChartMetadata {
        logger.debug("Fetching history metadata: symbol={}", symbol)
        return priceService.getHistoryMetadata(symbol)
    }

    // ========================================
    // Corp API Methods (ufc.corp)
    // ========================================

    /**
     * 배당금 히스토리를 조회합니다.
     *
     * @param symbol 조회할 심볼
     * @param period 조회 기간 (기본값: FiveYears)
     * @return 배당금 히스토리
     */
    suspend fun getDividends(
        symbol: String,
        period: Period = Period.FiveYears
    ): DividendHistory {
        logger.debug("Fetching dividends: symbol={}, period={}", symbol, period.value)
        return corpService.getDividends(symbol, period)
    }

    /**
     * 주식 분할 히스토리를 조회합니다.
     *
     * @param symbol 조회할 심볼 (MUTUALFUND 제외)
     * @param period 조회 기간 (기본값: Max)
     * @return 주식 분할 히스토리
     */
    suspend fun getSplits(
        symbol: String,
        period: Period = Period.Max
    ): SplitHistory {
        logger.debug("Fetching splits: symbol={}, period={}", symbol, period.value)
        return corpService.getSplits(symbol, period)
    }

    /**
     * 자본이득 분배 히스토리를 조회합니다.
     *
     * @param symbol 조회할 심볼
     * @param period 조회 기간 (기본값: FiveYears)
     * @return 자본이득 분배 히스토리
     */
    suspend fun getCapitalGains(
        symbol: String,
        period: Period = Period.FiveYears
    ): CapitalGainHistory {
        logger.debug("Fetching capital gains: symbol={}, period={}", symbol, period.value)
        return corpService.getCapitalGains(symbol, period)
    }

    // ========================================
    // Macro API Methods (ufc.macro)
    // ========================================

    /**
     * 거시경제 지표 시계열 데이터를 조회합니다.
     *
     * @param seriesId FRED 시리즈 ID
     * @param startDate 시작일 (YYYY-MM-DD, 선택적)
     * @param endDate 종료일 (YYYY-MM-DD, 선택적)
     * @param frequency 주기 변환 (선택적)
     * @param units 단위 변환 (선택적)
     * @return 거시경제 지표 시계열 데이터
     * @throws UfcException FRED API 키가 설정되지 않은 경우
     */
    suspend fun getMacroSeries(
        seriesId: String,
        startDate: String? = null,
        endDate: String? = null,
        frequency: String? = null,
        units: String? = null
    ): MacroSeries {
        logger.debug("Fetching macro series: seriesId={}, startDate={}, endDate={}",
            seriesId, startDate, endDate)
        return macroService?.getSeries(seriesId, startDate, endDate, frequency, units)
            ?: throw UfcException(
                ErrorCode.CONFIGURATION_ERROR,
                "FRED API key is not configured. Please provide fredApiKey in UFCClientConfig."
            )
    }

    /**
     * Real GDP (실질 GDP)를 조회합니다.
     *
     * @param startDate 시작일 (선택적)
     * @param endDate 종료일 (선택적)
     * @return GDP 시계열 데이터
     */
    suspend fun getGDP(
        startDate: String? = null,
        endDate: String? = null
    ): MacroSeries {
        logger.debug("Fetching GDP: startDate={}, endDate={}", startDate, endDate)
        return macroService?.getGDP(GDPType.REAL, startDate, endDate)
            ?: throw UfcException(
                ErrorCode.CONFIGURATION_ERROR,
                "FRED API key is not configured"
            )
    }

    /**
     * GDP Growth Rate (GDP 성장률)을 조회합니다.
     *
     * @param startDate 시작일 (선택적)
     * @param endDate 종료일 (선택적)
     * @return GDP 성장률 시계열 데이터
     */
    suspend fun getGDPGrowth(
        startDate: String? = null,
        endDate: String? = null
    ): MacroSeries {
        logger.debug("Fetching GDP Growth: startDate={}, endDate={}", startDate, endDate)
        return macroService?.getGDP(GDPType.GROWTH, startDate, endDate)
            ?: throw UfcException(
                ErrorCode.CONFIGURATION_ERROR,
                "FRED API key is not configured"
            )
    }

    /**
     * Unemployment Rate (실업률)을 조회합니다.
     *
     * @param startDate 시작일 (선택적)
     * @param endDate 종료일 (선택적)
     * @return 실업률 시계열 데이터
     */
    suspend fun getUnemploymentRate(
        startDate: String? = null,
        endDate: String? = null
    ): MacroSeries {
        logger.debug("Fetching Unemployment Rate: startDate={}, endDate={}", startDate, endDate)
        return macroService?.getUnemployment(UnemploymentType.RATE, startDate, endDate)
            ?: throw UfcException(
                ErrorCode.CONFIGURATION_ERROR,
                "FRED API key is not configured"
            )
    }

    /**
     * Initial Jobless Claims (신규 실업수당 청구 건수)를 조회합니다.
     *
     * @param startDate 시작일 (선택적)
     * @param endDate 종료일 (선택적)
     * @return 신규 실업수당 청구 건수 시계열 데이터
     */
    suspend fun getInitialClaims(
        startDate: String? = null,
        endDate: String? = null
    ): MacroSeries {
        logger.debug("Fetching Initial Claims: startDate={}, endDate={}", startDate, endDate)
        return macroService?.getUnemployment(UnemploymentType.INITIAL_CLAIMS, startDate, endDate)
            ?: throw UfcException(
                ErrorCode.CONFIGURATION_ERROR,
                "FRED API key is not configured"
            )
    }

    /**
     * CPI (소비자물가지수)를 조회합니다.
     *
     * @param startDate 시작일 (선택적)
     * @param endDate 종료일 (선택적)
     * @param core true인 경우 Core CPI (식품/에너지 제외) 조회
     * @return CPI 시계열 데이터
     */
    suspend fun getCPI(
        startDate: String? = null,
        endDate: String? = null,
        core: Boolean = false
    ): MacroSeries {
        logger.debug("Fetching CPI: startDate={}, endDate={}, core={}", startDate, endDate, core)
        val type = if (core) InflationType.CPI_CORE else InflationType.CPI_ALL
        return macroService?.getInflation(type, startDate, endDate)
            ?: throw UfcException(
                ErrorCode.CONFIGURATION_ERROR,
                "FRED API key is not configured"
            )
    }

    /**
     * PCE (개인소비지출 물가지수)를 조회합니다.
     *
     * @param startDate 시작일 (선택적)
     * @param endDate 종료일 (선택적)
     * @param core true인 경우 Core PCE (식품/에너지 제외) 조회
     * @return PCE 시계열 데이터
     */
    suspend fun getPCE(
        startDate: String? = null,
        endDate: String? = null,
        core: Boolean = false
    ): MacroSeries {
        logger.debug("Fetching PCE: startDate={}, endDate={}, core={}", startDate, endDate, core)
        val type = if (core) InflationType.PCE_CORE else InflationType.PCE_ALL
        return macroService?.getInflation(type, startDate, endDate)
            ?: throw UfcException(
                ErrorCode.CONFIGURATION_ERROR,
                "FRED API key is not configured"
            )
    }

    /**
     * Federal Funds Rate (연방기금금리)를 조회합니다.
     *
     * @param startDate 시작일 (선택적)
     * @param endDate 종료일 (선택적)
     * @return 연방기금금리 시계열 데이터
     */
    suspend fun getFedFundsRate(
        startDate: String? = null,
        endDate: String? = null
    ): MacroSeries {
        logger.debug("Fetching Fed Funds Rate: startDate={}, endDate={}", startDate, endDate)
        return macroService?.getInterestRate(InterestRateType.FEDERAL_FUNDS, startDate, endDate)
            ?: throw UfcException(
                ErrorCode.CONFIGURATION_ERROR,
                "FRED API key is not configured"
            )
    }

    /**
     * Treasury Yield (국채 수익률)를 조회합니다.
     *
     * @param term 만기 (10Y, 2Y, 5Y, 30Y, 3M)
     * @param startDate 시작일 (선택적)
     * @param endDate 종료일 (선택적)
     * @return 국채 수익률 시계열 데이터
     */
    suspend fun getTreasuryYield(
        term: String,
        startDate: String? = null,
        endDate: String? = null
    ): MacroSeries {
        logger.debug("Fetching Treasury Yield: term={}, startDate={}, endDate={}", term, startDate, endDate)
        val type = when (term.uppercase()) {
            "10Y" -> InterestRateType.TREASURY_10Y
            "2Y" -> InterestRateType.TREASURY_2Y
            "5Y" -> InterestRateType.TREASURY_5Y
            "30Y" -> InterestRateType.TREASURY_30Y
            "3M" -> InterestRateType.TREASURY_3M
            else -> throw UfcException(
                ErrorCode.INVALID_PARAMETER,
                "지원하지 않는 만기입니다. term: $term (지원: 10Y, 2Y, 5Y, 30Y, 3M)"
            )
        }
        return macroService?.getInterestRate(type, startDate, endDate)
            ?: throw UfcException(
                ErrorCode.CONFIGURATION_ERROR,
                "FRED API key is not configured"
            )
    }

    // ========================================
    // QuoteSummary API Methods (통합 API)
    // ========================================

    /**
     * QuoteSummary API - 지정한 모듈들의 데이터를 한 번의 API 호출로 가져옵니다.
     *
     * Yahoo Finance QuoteSummary API를 통해 주식, ETF, 뮤추얼펀드 등의 상세 정보를 조회합니다.
     * 여러 모듈을 지정하여 필요한 정보만 선택적으로 가져올 수 있습니다.
     *
     * 모듈을 지정하지 않으면 기본 모듈 세트(PRICE, SUMMARY_DETAIL, QUOTE_TYPE)를 사용합니다.
     *
     * @param symbol 조회할 심볼 (예: "AAPL", "SPY")
     * @param modules 조회할 모듈 목록 (미지정시 기본 모듈 사용)
     * @return 요청한 모듈별 데이터를 포함한 QuoteSummaryModuleResult
     * @throws UfcException 데이터 조회 실패 시
     */
    suspend fun quoteSummary(
        symbol: String,
        vararg modules: QuoteSummaryModule
    ): QuoteSummaryModuleResult {
        logger.debug("Fetching quote summary: symbol={}, modules={}", symbol, modules.contentToString())

        // 모듈이 지정되지 않은 경우 기본 모듈 사용
        val requestedModules = if (modules.isEmpty()) {
            setOf(
                QuoteSummaryModule.PRICE,
                QuoteSummaryModule.SUMMARY_DETAIL,
                QuoteSummaryModule.QUOTE_TYPE
            )
        } else {
            modules.toSet()
        }

        // API 호출
        val moduleStrings = requestedModules.map { it.apiValue }
        val response = priceHttpClient.fetchQuoteSummary(symbol, moduleStrings)

        // 응답 변환
        val quoteSummaryResult = response.quoteSummary.result?.firstOrNull()
            ?: throw UfcException(
                ErrorCode.DATA_NOT_FOUND,
                "QuoteSummary 데이터를 찾을 수 없습니다: $symbol"
            )

        return quoteSummaryResult.toModuleResult(requestedModules)
    }

    // ========================================
    // Chart API Methods (통합 API)
    // ========================================

    /**
     * Chart API - OHLCV 데이터와 이벤트를 한 번의 API 호출로 가져옵니다.
     *
     * Yahoo Finance Chart API를 통해 가격 히스토리(OHLCV)와
     * 이벤트 데이터(배당금, 주식분할, 자본이득)를 조회합니다.
     *
     * 이벤트를 지정하지 않으면 OHLCV 데이터만 조회합니다.
     *
     * @param symbol 조회할 심볼 (예: "AAPL", "^GSPC")
     * @param interval 데이터 간격 (기본값: OneDay)
     * @param period 조회 기간 (기본값: OneYear)
     * @param events 조회할 이벤트 타입 목록 (미지정시 이벤트 미포함)
     * @return OHLCV 데이터와 요청한 이벤트를 포함한 ChartData
     * @throws UfcException 데이터 조회 실패 시
     */
    suspend fun chart(
        symbol: String,
        interval: Interval = Interval.OneDay,
        period: Period = Period.OneYear,
        vararg events: ChartEventType
    ): ChartData {
        logger.debug(
            "Fetching chart data: symbol={}, interval={}, period={}, events={}",
            symbol, interval.value, period.value, events.contentToString()
        )

        val requestedEvents = events.toSet()

        // API 호출 (이벤트 포함 여부에 따라 다른 메서드 사용)
        val response = if (requestedEvents.isNotEmpty()) {
            // 이벤트 파라미터 구성
            val eventParams = requestedEvents.joinToString(",") { it.apiValue }
            priceHttpClient.fetchChartWithEvents(symbol, interval, period, includeEvents = true)
        } else {
            priceHttpClient.fetchChart(symbol, interval, period)
        }

        // 응답 변환
        val chartResult = response.chart.result?.firstOrNull()
            ?: throw UfcException(
                ErrorCode.DATA_NOT_FOUND,
                "차트 데이터를 찾을 수 없습니다: $symbol"
            )

        // ChartMeta 구성
        val meta = chartResult.meta?.let { m ->
            com.ulalax.ufc.domain.chart.ChartMeta(
                symbol = m.symbol,
                currency = m.currency,
                currencySymbol = m.currencySymbol,
                regularMarketPrice = m.regularMarketPrice,
                exchange = m.exchange,
                regularMarketDayHigh = m.regularMarketDayHigh,
                regularMarketDayLow = m.regularMarketDayLow,
                dataGranularity = m.dataGranularity,
                range = m.range,
                fiftyTwoWeekHigh = m.fiftyTwoWeekHigh,
                fiftyTwoWeekLow = m.fiftyTwoWeekLow,
                sharesOutstanding = m.sharesOutstanding,
                marketCap = m.marketCap,
                regularMarketVolume = m.regularMarketVolume,
                validRanges = m.validRanges
            )
        } ?: throw UfcException(
            ErrorCode.DATA_PARSING_ERROR,
            "차트 메타데이터를 찾을 수 없습니다"
        )

        // OHLCV 데이터 파싱
        val timestamps = chartResult.timestamp ?: emptyList()
        val quote = chartResult.indicators?.quote?.firstOrNull()
        val adjClose = chartResult.indicators?.adjclose?.firstOrNull()

        val prices = timestamps.mapIndexedNotNull { index, timestamp ->
            val open = quote?.open?.getOrNull(index)
            val high = quote?.high?.getOrNull(index)
            val low = quote?.low?.getOrNull(index)
            val close = quote?.close?.getOrNull(index)
            val volume = quote?.volume?.getOrNull(index)
            val adj = adjClose?.adjclose?.getOrNull(index)

            // null 값이 있으면 해당 데이터 포인트 제외
            if (open != null && high != null && low != null && close != null && volume != null) {
                com.ulalax.ufc.domain.price.OHLCV(
                    timestamp = timestamp,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    adjClose = adj,
                    volume = volume
                )
            } else {
                null
            }
        }

        // 이벤트 데이터 (요청한 경우에만 포함)
        val chartEvents = chartResult.events

        return ChartData(
            requestedEvents = requestedEvents,
            meta = meta,
            prices = prices,
            events = chartEvents
        )
    }
}
