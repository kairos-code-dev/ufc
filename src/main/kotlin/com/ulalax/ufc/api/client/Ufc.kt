package com.ulalax.ufc.api.client

import com.ulalax.ufc.api.*
import com.ulalax.ufc.api.impl.*
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.domain.chart.ChartData
import com.ulalax.ufc.domain.chart.ChartEventType
import com.ulalax.ufc.domain.common.Interval
import com.ulalax.ufc.domain.common.Period
import com.ulalax.ufc.domain.quote.QuoteSummaryModule
import com.ulalax.ufc.domain.quote.QuoteSummaryModuleResult
import org.slf4j.LoggerFactory

/**
 * UFC (Unified Finance Client) - 네임스페이스 기반 통합 금융 API 클라이언트
 *
 * Yahoo Finance, FRED 등 여러 외부 금융 API와의 통신을 네임스페이스 기반으로 통합합니다.
 * 각 도메인별로 명확히 분리된 네임스페이스를 제공하여 직관적인 API 사용을 지원합니다.
 *
 * ## 네임스페이스 구조
 *
 * - `ufc.price`: 가격 정보 (현재가, 가격 히스토리)
 * - `ufc.stock`: 주식 기본 정보 (회사 정보, ISIN, 발행주식수)
 * - `ufc.funds`: 펀드 정보 (ETF/뮤추얼펀드 구성)
 * - `ufc.corp`: 기업 행동 (배당금, 주식분할, 자본이득)
 * - `ufc.macro`: 거시경제 지표 (GDP, 실업률, 인플레이션 등)
 *
 * ## 사용 예시
 *
 * ```kotlin
 * // 클라이언트 생성
 * val ufc = UFC.create(
 *     UFCClientConfig(
 *         fredApiKey = "your-fred-api-key"
 *     )
 * )
 *
 * // 가격 정보 조회
 * val price = ufc.price.getCurrentPrice("AAPL")
 * println("${price.symbol}: ${price.lastPrice} ${price.currency}")
 *
 * val history = ufc.price.getPriceHistory("AAPL", Period.OneYear)
 * history.forEach { println("${it.timestamp}: ${it.close}") }
 *
 * // 주식 정보 조회
 * val companyInfo = ufc.stock.getCompanyInfo("AAPL")
 * println("${companyInfo.longName} - ${companyInfo.sector}")
 *
 * val isin = ufc.stock.getIsin("AAPL")
 * println("ISIN: $isin")
 *
 * // 펀드 정보 조회
 * val fundData = ufc.funds.getFundData("SPY")
 * println("${fundData.symbol}: ${fundData.description}")
 * fundData.topHoldings.forEach { holding ->
 *     println("${holding.symbol}: ${holding.holdingPercent}%")
 * }
 *
 * // 기업 행동 조회
 * val dividends = ufc.corp.getDividends("AAPL", Period.FiveYears)
 * dividends.dividends.forEach { div ->
 *     println("${div.date}: ${div.amount} USD")
 * }
 *
 * // 거시경제 지표 조회 (FRED API 키 필요)
 * val gdp = ufc.macro.getGDP()
 * gdp.observations.forEach { obs ->
 *     println("${obs.date}: ${obs.value}")
 * }
 *
 * // 사용 후 닫기
 * ufc.close()
 *
 * // 또는 try-with-resources 사용
 * UFC.create(config).use { ufc ->
 *     val data = ufc.price.getCurrentPrice("AAPL")
 * }
 * ```
 *
 * ## 하위 호환성
 *
 * 기존 UFCClient와 UFCClientImpl은 계속 사용 가능하지만, deprecated 되었습니다.
 * 새로운 코드에서는 UFC 클래스를 사용하는 것을 권장합니다.
 *
 * @property price 가격 정보 API
 * @property stock 주식 기본 정보 API
 * @property funds 펀드 정보 API
 * @property corp 기업 행동 API
 * @property macro 거시경제 지표 API (nullable - FRED API 키가 제공된 경우에만 사용 가능)
 */
class Ufc private constructor(
    private val impl: UfcClientImpl
) : AutoCloseable {

    /**
     * 가격 정보 API
     *
     * 현재 가격 및 가격 히스토리 조회를 제공합니다.
     */
    val price: PriceApi

    /**
     * 주식 기본 정보 API
     *
     * 회사 정보, ISIN 코드, 발행주식수 등을 제공합니다.
     */
    val stock: StockApi

    /**
     * 펀드 정보 API
     *
     * ETF 및 뮤추얼펀드의 구성 정보를 제공합니다.
     */
    val funds: FundsApi

    /**
     * 기업 행동 API
     *
     * 배당금, 주식분할, 자본이득 히스토리를 제공합니다.
     */
    val corp: CorpApi

    /**
     * 거시경제 지표 API (nullable)
     *
     * FRED API를 통해 GDP, 실업률, 인플레이션 등 거시경제 지표를 제공합니다.
     * FRED API 키가 제공되지 않은 경우 null입니다.
     *
     * 사용 전 null 체크가 필요합니다:
     * ```kotlin
     * val macro = ufc.macro ?: throw IllegalStateException("FRED API key required")
     * val gdp = macro.getGDP()
     * ```
     */
    val macro: MacroApi?

    init {
        // 네임스페이스 API 인스턴스 생성
        price = PriceApiImpl(impl.priceService)
        stock = StockApiImpl(impl.stockService)
        funds = FundsApiImpl(impl.fundsService)
        corp = CorpApiImpl(impl.corpService)
        macro = impl.macroService?.let { MacroApiImpl(it) }

        logger.info("UFC client initialized with namespaces: price, stock, funds, corp${if (macro != null) ", macro" else ""}")
    }

    /**
     * QuoteSummary API - 지정한 모듈들의 데이터를 한 번의 API 호출로 가져옵니다.
     *
     * Yahoo Finance QuoteSummary API를 통해 주식, ETF, 뮤추얼펀드 등의 상세 정보를 조회합니다.
     * 여러 모듈을 지정하여 필요한 정보만 선택적으로 가져올 수 있습니다.
     *
     * 지원하는 주요 모듈:
     * - PRICE: 기본 가격 정보
     * - SUMMARY_DETAIL: 배당금, PER, 베타 등 주요 통계
     * - FINANCIAL_DATA: 재무 정보 (EPS, PEG, ROE 등)
     * - ASSET_PROFILE: 기업 프로필 (섹터, 산업, 웹사이트 등)
     * - TOP_HOLDINGS: 펀드의 상위 보유 종목
     * - FUND_PROFILE: 펀드 프로필
     *
     * 모듈을 지정하지 않으면 기본 모듈 세트(PRICE, SUMMARY_DETAIL, QUOTE_TYPE)를 사용합니다.
     *
     * 사용 예시:
     * ```kotlin
     * // 가격과 재무 데이터만 조회
     * val result = ufc.quoteSummary(
     *     symbol = "AAPL",
     *     QuoteSummaryModule.PRICE,
     *     QuoteSummaryModule.FINANCIAL_DATA
     * )
     *
     * // 특정 모듈의 데이터 가져오기
     * val price: Price? = result.getModule(QuoteSummaryModule.PRICE)
     * println("Current price: ${price?.regularMarketPrice?.doubleValue}")
     * ```
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
        return impl.quoteSummary(symbol, *modules)
    }

    /**
     * Chart API - OHLCV 데이터와 이벤트를 한 번의 API 호출로 가져옵니다.
     *
     * Yahoo Finance Chart API를 통해 가격 히스토리(OHLCV)와
     * 이벤트 데이터(배당금, 주식분할, 자본이득)를 조회합니다.
     *
     * 지원하는 이벤트:
     * - DIVIDEND: 배당금 지급 내역
     * - SPLIT: 주식 분할 내역
     * - CAPITAL_GAIN: 자본 이득 내역 (ETF 등)
     *
     * 이벤트를 지정하지 않으면 OHLCV 데이터만 조회합니다.
     *
     * 사용 예시:
     * ```kotlin
     * // OHLCV 데이터와 배당 이벤트 조회
     * val result = ufc.chart(
     *     symbol = "AAPL",
     *     interval = Interval.OneDay,
     *     period = Period.OneYear,
     *     ChartEventType.DIVIDEND,
     *     ChartEventType.SPLIT
     * )
     *
     * // 가격 데이터 확인
     * result.prices.forEach { ohlcv ->
     *     println("${ohlcv.timestamp}: ${ohlcv.close}")
     * }
     *
     * // 배당 이벤트 확인
     * if (result.hasEvent(ChartEventType.DIVIDEND)) {
     *     val dividends = result.getDividends()
     *     dividends?.forEach { (timestamp, event) ->
     *         println("Dividend: ${event.amount} at $timestamp")
     *     }
     * }
     * ```
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
        return impl.chart(symbol, interval, period, *events)
    }

    /**
     * 클라이언트 리소스를 정리합니다.
     *
     * HTTP 클라이언트 및 기타 리소스를 닫습니다.
     */
    override fun close() {
        logger.info("Closing UFC client")
        impl.close()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Ufc::class.java)

        /**
         * UFC 클라이언트를 생성합니다.
         *
         * 이 메서드는 다음을 수행합니다:
         * 1. Ktor HTTP 클라이언트 생성
         * 2. Yahoo Finance 인증 (CRUMB 토큰 획득)
         * 3. Rate Limiter 초기화
         * 4. 각 API 서비스 인스턴스 생성
         * 5. 네임스페이스 API 초기화
         *
         * @param config UFC 클라이언트 설정
         * @return 생성된 UFC 인스턴스
         * @throws UfcException 인증 실패 또는 초기화 실패 시
         *
         * 사용 예시:
         * ```kotlin
         * val ufc = UFC.create(
         *     UFCClientConfig(
         *         fredApiKey = "your-fred-api-key",
         *         rateLimitingSettings = RateLimitingSettings()
         *     )
         * )
         * ```
         */
        suspend fun create(config: UfcClientConfig): Ufc {
            logger.info("Creating UFC client with config: fredApiKey provided={}", config.fredApiKey != null)

            return try {
                val impl = UfcClientImpl.create(config)
                Ufc(impl).also {
                    logger.info("UFC client created successfully")
                }
            } catch (e: Exception) {
                logger.error("Failed to create UFC client", e)
                throw when (e) {
                    is UfcException -> e
                    else -> UfcException(
                        errorCode = ErrorCode.UNKNOWN_ERROR,
                        message = "Failed to create UFC client: ${e.message}",
                        cause = e
                    )
                }
            }
        }
    }
}
