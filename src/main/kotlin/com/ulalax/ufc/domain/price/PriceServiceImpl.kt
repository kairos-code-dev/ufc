package com.ulalax.ufc.domain.price

import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.infrastructure.yahoo.response.ChartResponse
import com.ulalax.ufc.infrastructure.yahoo.response.PriceResponse
import com.ulalax.ufc.domain.common.Interval
import com.ulalax.ufc.domain.common.Period
import com.ulalax.ufc.infrastructure.util.CacheHelper
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.min
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Price 도메인 서비스 구현체
 *
 * 책임:
 * - 오케스트레이션 (캐시 → HTTP → 파싱)
 * - 도메인 검증
 * - JSON 파싱 (지역성 원칙: 관련 로직을 가까이 배치)
 *
 * 의존성:
 * - PriceHttpClient (인터페이스): 테스트 격리 가능
 * - CacheHelper (구체 클래스): 인메모리로 충분히 빠름
 *
 * 도메인 순수성:
 * - Ktor HttpClient에 직접 의존하지 않음
 * - PriceHttpClient 인터페이스만 의존 (의존성 역전)
 *
 * 문맥의 지역성:
 * - 파싱 로직이 Service 내부에 위치
 * - 별도 Parser 클래스를 만들지 않음 (구현체 하나, YAGNI)
 *
 * @property httpClient HTTP 통신 인터페이스 (테스트 시 Fake로 교체 가능)
 * @property cache 캐싱 유틸리티
 */
class PriceServiceImpl(
    private val httpClient: PriceHttpClient,
    private val cache: CacheHelper
) : PriceService {

    companion object {
        private val logger = LoggerFactory.getLogger(PriceServiceImpl::class.java)

        // 캐시 TTL 설정
        private val CURRENT_PRICE_TTL = 60.seconds  // 현재가: 1분
        private val PRICE_HISTORY_TTL = 5.minutes   // 히스토리: 5분
    }

    // ============================================================================
    // Public API Methods
    // ============================================================================

    override suspend fun getCurrentPrice(symbol: String): PriceData {
        validateSymbol(symbol)

        logger.debug("Fetching current price: symbol={}", symbol)

        return cache.getOrPut("price:current:$symbol", ttl = CURRENT_PRICE_TTL) {
            val response = httpClient.fetchQuoteSummary(symbol, listOf("price", "summaryDetail"))
            parsePriceData(symbol, response)
        }
    }

    override suspend fun getCurrentPrice(symbols: List<String>): Map<String, PriceData> {
        require(symbols.isNotEmpty()) { "심볼 목록이 비어있습니다" }
        require(symbols.size <= 100) { "최대 100개의 심볼만 조회 가능합니다" }

        symbols.forEach { validateSymbol(it) }

        logger.debug("Fetching current price for {} symbols", symbols.size)

        // 병렬 처리
        return coroutineScope {
            symbols.map { symbol ->
                async { symbol to getCurrentPrice(symbol) }
            }
        }.map { it.await() }.toMap()
    }

    override suspend fun getPriceHistory(
        symbol: String,
        period: Period,
        interval: Interval
    ): List<OHLCV> {
        validateSymbol(symbol)
        validatePeriodInterval(period, interval)

        logger.debug(
            "Fetching price history: symbol={}, period={}, interval={}",
            symbol, period.value, interval.value
        )

        return cache.getOrPut("price:history:$symbol:$period:$interval", ttl = PRICE_HISTORY_TTL) {
            val response = httpClient.fetchChart(symbol, interval, period)
            parseChartData(response)
        }
    }

    override suspend fun getPriceHistory(
        symbol: String,
        start: LocalDate,
        end: LocalDate,
        interval: Interval
    ): List<OHLCV> {
        validateSymbol(symbol)

        if (start >= end) {
            throw UfcException(
                ErrorCode.INVALID_DATE_RANGE,
                "시작 날짜는 종료 날짜보다 이전이어야 합니다: start=$start, end=$end"
            )
        }

        logger.debug(
            "Fetching price history (date range): symbol={}, start={}, end={}, interval={}",
            symbol, start, end, interval.value
        )

        return cache.getOrPut("price:history:$symbol:$start:$end:$interval", ttl = PRICE_HISTORY_TTL) {
            val response = httpClient.fetchChartByDateRange(symbol, start, end, interval)
            parseChartData(response)
        }
    }

    override suspend fun getPriceHistoryWithMetadata(
        symbol: String,
        period: Period,
        interval: Interval
    ): Pair<ChartMetadata, List<OHLCV>> {
        validateSymbol(symbol)
        validatePeriodInterval(period, interval)

        logger.debug(
            "Fetching price history with metadata: symbol={}, period={}, interval={}",
            symbol, period.value, interval.value
        )

        return cache.getOrPut("price:history_meta:$symbol:$period:$interval", ttl = PRICE_HISTORY_TTL) {
            val response = httpClient.fetchChart(symbol, interval, period)
            val metadata = parseChartMetadata(response)
            val ohlcv = parseChartData(response)
            metadata to ohlcv
        }
    }

    override suspend fun getHistoryMetadata(symbol: String): ChartMetadata {
        validateSymbol(symbol)

        logger.debug("Fetching history metadata: symbol={}", symbol)

        // 최소 기간으로 조회하여 메타데이터만 추출
        val response = httpClient.fetchChart(symbol, Interval.OneDay, Period.OneDay)
        return parseChartMetadata(response)
    }

    override suspend fun getRawPrice(symbol: String): PriceResponse {
        validateSymbol(symbol)

        logger.debug("Fetching raw price: symbol={}", symbol)

        return httpClient.fetchQuoteSummary(symbol, listOf("price", "summaryDetail"))
    }

    override suspend fun getRawPriceHistory(
        symbol: String,
        period: Period,
        interval: Interval
    ): ChartResponse {
        validateSymbol(symbol)
        validatePeriodInterval(period, interval)

        logger.debug(
            "Fetching raw price history: symbol={}, period={}, interval={}",
            symbol, period.value, interval.value
        )

        return httpClient.fetchChart(symbol, interval, period)
    }

    // ============================================================================
    // Private: 도메인 검증
    // ============================================================================

    /**
     * 심볼 검증
     */
    private fun validateSymbol(symbol: String) {
        if (symbol.isBlank()) {
            throw UfcException(ErrorCode.INVALID_SYMBOL, "심볼이 비어있습니다")
        }

        if (symbol.length > 20) {
            throw UfcException(ErrorCode.INVALID_SYMBOL, "심볼이 너무 깁니다: $symbol (최대 20자)")
        }

        // 유효한 문자: 영문, 숫자, ^, ., -, _
        val validPattern = Regex("^[A-Za-z0-9^.\\-_]+$")
        if (!validPattern.matches(symbol)) {
            throw UfcException(ErrorCode.INVALID_SYMBOL, "유효하지 않은 심볼: $symbol")
        }
    }

    /**
     * Period와 Interval의 조합이 유효한지 검증
     */
    private fun validatePeriodInterval(period: Period, interval: Interval) {
        val daysInPeriod = when (period) {
            Period.OneDay -> 1
            Period.FiveDays -> 5
            Period.OneMonth -> 30
            Period.ThreeMonths -> 90
            Period.SixMonths -> 180
            Period.OneYear -> 365
            Period.TwoYears -> 730
            Period.FiveYears -> 1825
            Period.TenYears -> 3650
            Period.YearToDate -> 365
            Period.Max -> Int.MAX_VALUE
        }

        // Intraday intervals (< 60분) - 최대 7일
        if (interval.minutes < 60 && daysInPeriod > 7) {
            throw UfcException(
                ErrorCode.INVALID_PERIOD_INTERVAL,
                "Intraday 간격(< 1시간)은 최대 7일까지만 조회 가능합니다. " +
                        "요청: period=$period (${daysInPeriod}일), interval=${interval.toKoreanString()}"
            )
        }

        // Hourly interval (60분) - 최대 60일
        if (interval.minutes == 60 && daysInPeriod > 60) {
            throw UfcException(
                ErrorCode.INVALID_PERIOD_INTERVAL,
                "시간 간격(1시간)은 최대 60일까지만 조회 가능합니다. " +
                        "요청: period=$period (${daysInPeriod}일), interval=${interval.toKoreanString()}"
            )
        }
    }

    // ============================================================================
    // Private: JSON 파싱 (지역성 원칙)
    //
    // 별도 Parser 클래스를 만들지 않는 이유:
    // - 구현체가 하나뿐 (Yahoo만)
    // - 파싱 로직이 Service와 강하게 결합
    // - 문맥의 지역성: 관련 로직을 가까이 배치
    // ============================================================================

    /**
     * PriceData 파싱
     */
    private fun parsePriceData(symbol: String, response: PriceResponse): PriceData {
        val result = response.quoteSummary.result?.firstOrNull()
            ?: throw UfcException(
                ErrorCode.PRICE_DATA_NOT_FOUND,
                "심볼 '$symbol'의 가격 데이터를 찾을 수 없습니다"
            )

        val priceModule = result.price
        val summaryModule = result.summaryDetail

        // 필수 필드 검증
        if (priceModule == null && summaryModule == null) {
            throw UfcException(
                ErrorCode.INCOMPLETE_PRICE_DATA,
                "심볼 '$symbol'의 가격 모듈 데이터가 없습니다"
            )
        }

        // 기본 정보 추출
        val currency = priceModule?.currency
        val exchange = priceModule?.exchange
        val baseSymbol = priceModule?.symbol

        // 현재 가격 정보 추출
        val lastPrice = priceModule?.regularMarketPrice?.doubleValue
        val regularMarketPrice = priceModule?.regularMarketPrice?.doubleValue
        val regularMarketTime: Long? = null  // Yahoo API에서 제공하지 않음
        val open: Double? = null  // SummaryDetail에서 추출
        val dayHigh = summaryModule?.regularMarketDayHigh?.doubleValue
        val dayLow = summaryModule?.regularMarketDayLow?.doubleValue
        val volume = summaryModule?.regularMarketVolume?.longValue
        val regularMarketVolume = summaryModule?.regularMarketVolume?.longValue
        val marketCap = summaryModule?.marketCap?.longValue

        // 이동평균선 추출 (Yahoo QuoteSummary API에는 없음)
        val fiftyDayAverage: Double? = null
        val twoHundredDayAverage: Double? = null

        // 배당 정보 추출
        val dividendYield = summaryModule?.dividendYield?.doubleValue
        val dividendRate = summaryModule?.dividendRate?.doubleValue
        val exDividendDate = summaryModule?.exDividendDate?.longValue

        // 52주 범위, PER 등 추출
        val fiftyTwoWeekHigh = summaryModule?.fiftyTwoWeekHigh?.doubleValue
            ?: priceModule?.fiftyTwoWeekHigh?.doubleValue
        val fiftyTwoWeekLow = summaryModule?.fiftyTwoWeekLow?.doubleValue
            ?: priceModule?.fiftyTwoWeekLow?.doubleValue
        val fiftyTwoWeekChange: Double? = null  // API에서 제공하지 않음
        val fiftyTwoWeekChangePercent = priceModule?.fiftyTwoWeekChangePercent?.doubleValue
        val previousClose = priceModule?.regularMarketPreviousClose?.doubleValue
        val averageVolume = summaryModule?.averageVolume?.longValue
        val averageVolume10days = summaryModule?.averageVolume10days?.longValue
        val beta = summaryModule?.beta?.doubleValue
        val trailingPE = summaryModule?.trailingPE?.doubleValue
        val forwardPE = summaryModule?.forwardPE?.doubleValue

        return PriceData(
            symbol = baseSymbol ?: symbol,
            currency = currency,
            exchange = exchange,
            lastPrice = lastPrice,
            regularMarketPrice = regularMarketPrice,
            regularMarketTime = regularMarketTime,
            open = open,
            dayHigh = dayHigh,
            dayLow = dayLow,
            previousClose = previousClose,
            volume = volume,
            regularMarketVolume = regularMarketVolume,
            averageVolume = averageVolume,
            averageVolume10days = averageVolume10days,
            fiftyTwoWeekHigh = fiftyTwoWeekHigh,
            fiftyTwoWeekLow = fiftyTwoWeekLow,
            fiftyTwoWeekChange = fiftyTwoWeekChange,
            fiftyTwoWeekChangePercent = fiftyTwoWeekChangePercent,
            fiftyDayAverage = fiftyDayAverage,
            twoHundredDayAverage = twoHundredDayAverage,
            marketCap = marketCap,
            dividendYield = dividendYield,
            dividendRate = dividendRate,
            exDividendDate = exDividendDate,
            beta = beta,
            trailingPE = trailingPE,
            forwardPE = forwardPE
        )
    }

    /**
     * Chart 응답을 OHLCV 리스트로 변환
     */
    private fun parseChartData(response: ChartResponse): List<OHLCV> {
        val result = response.chart.result?.firstOrNull()
            ?: return emptyList()

        val timestamps = result.timestamp ?: return emptyList()
        val indicators = result.indicators ?: return emptyList()
        val quotes = indicators.quote?.firstOrNull() ?: return emptyList()

        val opens = quotes.open ?: return emptyList()
        val highs = quotes.high ?: return emptyList()
        val lows = quotes.low ?: return emptyList()
        val closes = quotes.close ?: return emptyList()
        val volumes = quotes.volume ?: return emptyList()

        val adjCloses = indicators.adjclose?.firstOrNull()?.adjclose

        val ohlcvList = mutableListOf<OHLCV>()

        val minSize = min(
            min(min(min(timestamps.size, opens.size), min(highs.size, lows.size)), closes.size),
            volumes.size
        )

        for (i in 0 until minSize) {
            // null 값이 있으면 스킵
            val open = opens.getOrNull(i) ?: continue
            val high = highs.getOrNull(i) ?: continue
            val low = lows.getOrNull(i) ?: continue
            val close = closes.getOrNull(i) ?: continue
            val volume = volumes.getOrNull(i) ?: continue
            val timestamp = timestamps.getOrNull(i) ?: continue

            val adjClose = adjCloses?.getOrNull(i)

            ohlcvList.add(
                OHLCV(
                    timestamp = timestamp,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    adjClose = adjClose,
                    volume = volume.toLong()
                )
            )
        }

        return ohlcvList
    }

    /**
     * Chart 메타데이터 추출
     */
    private fun parseChartMetadata(response: ChartResponse): ChartMetadata {
        val result = response.chart.result?.firstOrNull()
            ?: return ChartMetadata(
                symbol = "",
                currency = null,
                exchangeName = null,
                timezone = null,
                regularMarketPrice = null,
                regularMarketTime = null,
                dataGranularity = null,
                range = null,
                validRanges = null
            )

        val meta = result.meta
            ?: return ChartMetadata(
                symbol = "",
                currency = null,
                exchangeName = null,
                timezone = null,
                regularMarketPrice = null,
                regularMarketTime = null,
                dataGranularity = null,
                range = null,
                validRanges = null
            )

        return ChartMetadata(
            symbol = meta.symbol ?: "",
            currency = meta.currency,
            exchangeName = meta.exchange,
            timezone = null,  // ChartMeta에서 제공하지 않음
            regularMarketPrice = meta.regularMarketPrice,
            regularMarketTime = null,  // ChartMeta에서 제공하지 않음
            dataGranularity = meta.dataGranularity,
            range = meta.range,
            validRanges = meta.validRanges
        )
    }
}

// Extension function for toKoreanString
private fun Interval.toKoreanString(): String = when (this) {
    Interval.OneMinute -> "1분"
    Interval.TwoMinutes -> "2분"
    Interval.FiveMinutes -> "5분"
    Interval.FifteenMinutes -> "15분"
    Interval.ThirtyMinutes -> "30분"
    Interval.OneHour -> "1시간"
    Interval.OneDay -> "1일"
    Interval.FiveDays -> "5일"
    Interval.OneWeek -> "1주"
    Interval.OneMonth -> "1개월"
    Interval.ThreeMonths -> "3개월"
}
