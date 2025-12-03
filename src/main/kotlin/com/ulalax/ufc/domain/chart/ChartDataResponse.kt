package com.ulalax.ufc.domain.chart

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Yahoo Finance Chart API의 응답을 나타내는 모델
 *
 * 차트 데이터는 다음 구조로 반환됩니다:
 * - result: 차트 데이터의 배열 (심볼별로 하나)
 * - error: 에러 정보 (있을 경우)
 */
@Serializable
data class ChartDataResponse(
    @SerialName("chart")
    val chart: Chart
)

/**
 * 차트 데이터의 컨테이너
 */
@Serializable
data class Chart(
    @SerialName("result")
    val result: List<ChartResult>? = null,

    @SerialName("error")
    val error: ChartError? = null
)

/**
 * 단일 심볼의 차트 데이터
 */
@Serializable
data class ChartResult(
    // 메타데이터
    @SerialName("meta")
    val meta: ChartMeta? = null,

    // 시계열 데이터 배열
    @SerialName("timestamp")
    val timestamp: List<Long>? = null,

    @SerialName("indicators")
    val indicators: ChartIndicators? = null,

    // 이벤트 데이터 (배당금, 주식분할, 자본이득 등)
    @SerialName("events")
    val events: ChartEvents? = null
)

/**
 * 차트 메타데이터
 *
 * 주식/지수의 기본 정보를 포함합니다:
 * - 심볼, 통화, 현재가, 범위 등
 */
@Serializable
data class ChartMeta(
    // 심볼 (예: "AAPL", "^GSPC")
    @SerialName("symbol")
    val symbol: String? = null,

    // 통화 (예: "USD")
    @SerialName("currency")
    val currency: String? = null,

    // 통화 기호 (예: "$")
    @SerialName("currencySymbol")
    val currencySymbol: String? = null,

    // 현재 가격
    @SerialName("regularMarketPrice")
    val regularMarketPrice: Double? = null,

    // 교환 시장 (예: "NASDAQ", "NYSE")
    @SerialName("exchange")
    val exchange: String? = null,

    // 최고가 (조회 기간 내)
    @SerialName("regularMarketDayHigh")
    val regularMarketDayHigh: Double? = null,

    // 최저가 (조회 기간 내)
    @SerialName("regularMarketDayLow")
    val regularMarketDayLow: Double? = null,

    // 데이터 간격 (예: "1d", "1h")
    @SerialName("dataGranularity")
    val dataGranularity: String? = null,

    // 범위 (예: "1y", "1d")
    @SerialName("range")
    val range: String? = null,

    // 52주 최고가
    @SerialName("fiftyTwoWeekHigh")
    val fiftyTwoWeekHigh: Double? = null,

    // 52주 최저가
    @SerialName("fiftyTwoWeekLow")
    val fiftyTwoWeekLow: Double? = null,

    // 발행 주식수
    @SerialName("sharesOutstanding")
    val sharesOutstanding: Long? = null,

    // 시가총액
    @SerialName("marketCap")
    val marketCap: Long? = null,

    // 거래량
    @SerialName("regularMarketVolume")
    val regularMarketVolume: Long? = null,

    // 유효한 범위
    @SerialName("validRanges")
    val validRanges: List<String>? = null
)

/**
 * 차트 지표 (OHLCV 데이터)
 *
 * Open, High, Low, Close, Volume 데이터를 포함합니다.
 */
@Serializable
data class ChartIndicators(
    // OHLCV 데이터
    @SerialName("quote")
    val quote: List<ChartQuote>? = null,

    // 보조 지표 (마실 연결선 등)
    @SerialName("adjclose")
    val adjclose: List<ChartAdjClose>? = null
)

/**
 * 시간별 OHLCV 데이터
 */
@Serializable
data class ChartQuote(
    // 시가 (Open Price)
    @SerialName("open")
    val open: List<Double?>? = null,

    // 고가 (High Price)
    @SerialName("high")
    val high: List<Double?>? = null,

    // 저가 (Low Price)
    @SerialName("low")
    val low: List<Double?>? = null,

    // 종가 (Close Price)
    @SerialName("close")
    val close: List<Double?>? = null,

    // 거래량 (Volume)
    @SerialName("volume")
    val volume: List<Long?>? = null
)

/**
 * 조정 종가 데이터
 *
 * 배당금, 주식분할 등으로 조정된 종가를 포함합니다.
 */
@Serializable
data class ChartAdjClose(
    // 조정 종가
    @SerialName("adjclose")
    val adjclose: List<Double?>? = null
)

/**
 * 차트 데이터 에러
 */
@Serializable
data class ChartError(
    @SerialName("code")
    val code: String? = null,

    @SerialName("description")
    val description: String? = null
)

/**
 * 차트 이벤트 데이터
 *
 * 배당금, 주식분할, 자본이득 등의 이벤트 정보를 포함합니다.
 */
@Serializable
data class ChartEvents(
    @SerialName("dividends")
    val dividends: Map<String, DividendEvent>? = null,

    @SerialName("splits")
    val splits: Map<String, SplitEvent>? = null,

    @SerialName("capitalGains")
    val capitalGains: Map<String, CapitalGainEvent>? = null
)

/**
 * 배당금 이벤트
 */
@Serializable
data class DividendEvent(
    val amount: Double? = null,
    val date: Long? = null
)

/**
 * 주식 분할 이벤트
 */
@Serializable
data class SplitEvent(
    val date: Long? = null,
    val numerator: Int? = null,
    val denominator: Int? = null,
    val splitRatio: String? = null
)

/**
 * 자본이득 이벤트
 */
@Serializable
data class CapitalGainEvent(
    val amount: Double? = null,
    val date: Long? = null
)

/**
 * 정규화된 OHLCV 데이터 포인트
 *
 * null 값을 제거한 깔끔한 데이터 구조입니다.
 */
data class OHLCVData(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val adjClose: Double?,
    val volume: Long
)
