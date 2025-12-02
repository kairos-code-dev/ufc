package com.ulalax.ufc.client

import com.ulalax.ufc.domain.chart.ChartService
import com.ulalax.ufc.domain.chart.OHLCVData
import com.ulalax.ufc.domain.chart.YahooChartService
import com.ulalax.ufc.domain.quote.QuoteSummaryService
import com.ulalax.ufc.domain.quote.QuoteSummaryResult
import com.ulalax.ufc.domain.quote.StockSummary
import com.ulalax.ufc.domain.quote.YahooQuoteSummaryService
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitConfig
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitingSettings
import com.ulalax.ufc.infrastructure.ratelimit.TokenBucketRateLimiter
import com.ulalax.ufc.internal.yahoo.auth.BasicAuthStrategy
import com.ulalax.ufc.model.common.Interval
import com.ulalax.ufc.model.common.Period
import io.ktor.client.HttpClient
import org.slf4j.LoggerFactory

/**
 * UFC (Universal Financial Client) 클라이언트의 완전한 구현
 *
 * 이 클래스는 Yahoo Finance, FRED 등 여러 외부 금융 API와의 통신을 통합합니다.
 * 각 API별 서비스 레이어를 내부적으로 관리하고, 사용자에게 통일된 인터페이스를 제공합니다.
 *
 * 주요 기능:
 * - Yahoo Finance Chart API 지원 (OHLCV 데이터)
 * - Yahoo Finance QuoteSummary API 지원 (주식 요약 정보)
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
 * val chartData = client.getChartData(
 *     symbol = "AAPL",
 *     interval = Interval.OneDay,
 *     period = Period.OneYear
 * )
 *
 * // 요약 정보 조회
 * val summary = client.getStockSummary("AAPL")
 *
 * // 사용 후 닫기
 * client.close()
 * ```
 *
 * @property httpClient Ktor HttpClient 인스턴스
 * @property config UFC 클라이언트 설정
 * @property chartService Chart API 서비스
 * @property quoteService QuoteSummary API 서비스
 * @property yahooRateLimiter Yahoo Finance API용 Rate Limiter
 */
class UFCClientImpl(
    private val httpClient: HttpClient,
    private val config: UFCClientConfig,
    private val chartService: ChartService,
    private val quoteService: QuoteSummaryService,
    private val yahooRateLimiter: TokenBucketRateLimiter
) : AutoCloseable {

    companion object {
        private val logger = LoggerFactory.getLogger(UFCClientImpl::class.java)

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
        suspend fun create(config: UFCClientConfig): UFCClientImpl {
            logger.info("Creating UFC client with config: fredApiKey provided={}", config.fredApiKey != null)

            // Ktor HTTP 클라이언트 생성
            val httpClient = com.ulalax.ufc.internal.yahoo.YahooHttpClientFactory.create()

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
                logger.info("Rate limiter initialized")

                // 서비스 인스턴스 생성
                val chartService = YahooChartService(httpClient, yahooRateLimiter, authResult)
                val quoteService = YahooQuoteSummaryService(httpClient, yahooRateLimiter, authResult)

                val client = UFCClientImpl(
                    httpClient = httpClient,
                    config = config,
                    chartService = chartService,
                    quoteService = quoteService,
                    yahooRateLimiter = yahooRateLimiter
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
    // Chart API Methods
    // ========================================

    /**
     * 단일 심볼의 OHLCV 차트 데이터를 조회합니다.
     *
     * @param symbol 조회할 심볼 (예: "AAPL")
     * @param interval 데이터 간격 (기본값: OneDay)
     * @param period 조회 기간 (기본값: OneYear)
     * @return OHLCV 데이터 리스트
     */
    suspend fun getChartData(
        symbol: String,
        interval: Interval = Interval.OneDay,
        period: Period = Period.OneYear
    ): List<OHLCVData> {
        logger.debug("Fetching chart data: symbol={}, interval={}, period={}",
            symbol, interval.value, period.value)
        return chartService.getChartData(symbol, interval, period)
    }

    /**
     * 다중 심볼의 OHLCV 차트 데이터를 조회합니다.
     *
     * @param symbols 조회할 심볼 목록
     * @param interval 데이터 간격 (기본값: OneDay)
     * @param period 조회 기간 (기본값: OneYear)
     * @return 심볼별 OHLCV 데이터 맵
     */
    suspend fun getChartData(
        symbols: List<String>,
        interval: Interval = Interval.OneDay,
        period: Period = Period.OneYear
    ): Map<String, List<OHLCVData>> {
        logger.debug("Fetching chart data for {} symbols", symbols.size)
        return chartService.getChartData(symbols, interval, period)
    }

    // ========================================
    // Quote Summary API Methods
    // ========================================

    /**
     * 단일 심볼의 전체 요약 정보를 조회합니다.
     *
     * @param symbol 조회할 심볼
     * @return 요약 정보 객체
     */
    suspend fun getQuoteSummary(symbol: String): QuoteSummaryResult {
        logger.debug("Fetching quote summary: symbol={}", symbol)
        return quoteService.getQuoteSummary(symbol)
    }

    /**
     * 특정 모듈만 포함한 요약 정보를 조회합니다.
     *
     * @param symbol 조회할 심볼
     * @param modules 포함할 모듈 목록
     * @return 요약 정보 객체
     */
    suspend fun getQuoteSummary(symbol: String, modules: List<String>): QuoteSummaryResult {
        logger.debug("Fetching quote summary: symbol={}, modules={}", symbol, modules.size)
        return quoteService.getQuoteSummary(symbol, modules)
    }

    /**
     * 다중 심볼의 요약 정보를 조회합니다.
     *
     * @param symbols 조회할 심볼 목록
     * @return 심볼별 요약 정보 맵
     */
    suspend fun getQuoteSummary(symbols: List<String>): Map<String, QuoteSummaryResult> {
        logger.debug("Fetching quote summary for {} symbols", symbols.size)
        return quoteService.getQuoteSummary(symbols)
    }

    /**
     * 정규화된 주식 요약 정보를 조회합니다.
     *
     * @param symbol 조회할 심볼
     * @return 정규화된 StockSummary 객체
     */
    suspend fun getStockSummary(symbol: String): StockSummary {
        logger.debug("Fetching stock summary: symbol={}", symbol)
        return quoteService.getStockSummary(symbol)
    }

    /**
     * 다중 심볼의 정규화된 요약 정보를 조회합니다.
     *
     * @param symbols 조회할 심볼 목록
     * @return 심볼별 정규화된 요약 정보 맵
     */
    suspend fun getStockSummary(symbols: List<String>): Map<String, StockSummary> {
        logger.debug("Fetching stock summary for {} symbols", symbols.size)
        return quoteService.getStockSummary(symbols)
    }

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
        """.trimIndent()
    }
}
