package com.ulalax.ufc.infrastructure.yahoo.internal.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Yahoo Finance Quote API의 최상위 응답 모델
 *
 * Quote API는 실시간 시장 데이터와 기본 주식 정보를 제공합니다.
 * QuoteSummary API와 달리 다중 심볼 조회가 가능하며 응답 속도가 빠릅니다.
 */
@Serializable
internal data class QuoteApiResponse(
    @SerialName("quoteResponse")
    val quoteResponse: QuoteResponseData
)

/**
 * Quote API 응답의 데이터 컨테이너
 */
@Serializable
internal data class QuoteResponseData(
    @SerialName("result")
    val result: List<QuoteResult>? = null,

    @SerialName("error")
    val error: QuoteApiError? = null
)

/**
 * Quote API 에러 정보
 */
@Serializable
internal data class QuoteApiError(
    @SerialName("code")
    val code: String? = null,

    @SerialName("description")
    val description: String? = null
)

/**
 * 단일 심볼의 Quote 데이터
 *
 * Yahoo Finance의 모든 원시 필드를 포함합니다.
 * 이 internal 타입은 JSON 역직렬화를 위해 사용되며,
 * public domain 타입인 QuoteData로 변환됩니다.
 */
@Serializable
internal data class QuoteResult(
    // ===== 기본 정보 (Identification) =====
    @SerialName("symbol")
    val symbol: String,

    @SerialName("longName")
    val longName: String? = null,

    @SerialName("shortName")
    val shortName: String? = null,

    @SerialName("exchange")
    val exchange: String? = null,

    @SerialName("exchangeTimezoneName")
    val exchangeTimezoneName: String? = null,

    @SerialName("exchangeTimezoneShortName")
    val exchangeTimezoneShortName: String? = null,

    @SerialName("quoteType")
    val quoteType: String? = null,

    @SerialName("currency")
    val currency: String? = null,

    @SerialName("market")
    val market: String? = null,

    // ===== 가격 정보 (Pricing) =====
    @SerialName("regularMarketPrice")
    val regularMarketPrice: Double,

    @SerialName("regularMarketOpen")
    val regularMarketOpen: Double? = null,

    @SerialName("regularMarketDayHigh")
    val regularMarketDayHigh: Double? = null,

    @SerialName("regularMarketDayLow")
    val regularMarketDayLow: Double? = null,

    @SerialName("regularMarketVolume")
    val regularMarketVolume: Long? = null,

    @SerialName("regularMarketPreviousClose")
    val regularMarketPreviousClose: Double? = null,

    @SerialName("regularMarketChange")
    val regularMarketChange: Double? = null,

    @SerialName("regularMarketChangePercent")
    val regularMarketChangePercent: Double? = null,

    @SerialName("regularMarketTime")
    val regularMarketTime: Long? = null,

    // ===== 장전/장후 거래 (Extended Hours) =====
    @SerialName("preMarketPrice")
    val preMarketPrice: Double? = null,

    @SerialName("preMarketChange")
    val preMarketChange: Double? = null,

    @SerialName("preMarketChangePercent")
    val preMarketChangePercent: Double? = null,

    @SerialName("preMarketTime")
    val preMarketTime: Long? = null,

    @SerialName("postMarketPrice")
    val postMarketPrice: Double? = null,

    @SerialName("postMarketChange")
    val postMarketChange: Double? = null,

    @SerialName("postMarketChangePercent")
    val postMarketChangePercent: Double? = null,

    @SerialName("postMarketTime")
    val postMarketTime: Long? = null,

    // ===== 52주 고저가 (52-Week Range) =====
    @SerialName("fiftyTwoWeekHigh")
    val fiftyTwoWeekHigh: Double? = null,

    @SerialName("fiftyTwoWeekLow")
    val fiftyTwoWeekLow: Double? = null,

    @SerialName("fiftyTwoWeekHighChange")
    val fiftyTwoWeekHighChange: Double? = null,

    @SerialName("fiftyTwoWeekLowChange")
    val fiftyTwoWeekLowChange: Double? = null,

    @SerialName("fiftyTwoWeekHighChangePercent")
    val fiftyTwoWeekHighChangePercent: Double? = null,

    @SerialName("fiftyTwoWeekLowChangePercent")
    val fiftyTwoWeekLowChangePercent: Double? = null,

    @SerialName("fiftyTwoWeekRange")
    val fiftyTwoWeekRange: String? = null,

    // ===== 이동평균 (Moving Averages) =====
    @SerialName("fiftyDayAverage")
    val fiftyDayAverage: Double? = null,

    @SerialName("fiftyDayAverageChange")
    val fiftyDayAverageChange: Double? = null,

    @SerialName("fiftyDayAverageChangePercent")
    val fiftyDayAverageChangePercent: Double? = null,

    @SerialName("twoHundredDayAverage")
    val twoHundredDayAverage: Double? = null,

    @SerialName("twoHundredDayAverageChange")
    val twoHundredDayAverageChange: Double? = null,

    @SerialName("twoHundredDayAverageChangePercent")
    val twoHundredDayAverageChangePercent: Double? = null,

    // ===== 거래량 평균 (Volume Averages) =====
    @SerialName("averageDailyVolume3Month")
    val averageDailyVolume3Month: Long? = null,

    @SerialName("averageDailyVolume10Day")
    val averageDailyVolume10Day: Long? = null,

    // ===== 시가총액 및 발행주식수 (Market Cap) =====
    @SerialName("marketCap")
    val marketCap: Long? = null,

    @SerialName("sharesOutstanding")
    val sharesOutstanding: Long? = null,

    // ===== 배당 정보 (Dividends) =====
    @SerialName("dividendRate")
    val dividendRate: Double? = null,

    @SerialName("dividendYield")
    val dividendYield: Double? = null,

    @SerialName("dividendDate")
    val dividendDate: Long? = null,

    @SerialName("exDividendDate")
    val exDividendDate: Long? = null,

    @SerialName("trailingAnnualDividendRate")
    val trailingAnnualDividendRate: Double? = null,

    @SerialName("trailingAnnualDividendYield")
    val trailingAnnualDividendYield: Double? = null,

    // ===== 재무 비율 (Financial Ratios) =====
    @SerialName("trailingPE")
    val trailingPE: Double? = null,

    @SerialName("forwardPE")
    val forwardPE: Double? = null,

    @SerialName("priceToBook")
    val priceToBook: Double? = null,

    @SerialName("priceToSales")
    val priceToSales: Double? = null,

    @SerialName("bookValue")
    val bookValue: Double? = null,

    @SerialName("earningsQuarterlyGrowth")
    val earningsQuarterlyGrowth: Double? = null,

    // ===== 수익 정보 (Earnings) =====
    @SerialName("epsTrailingTwelveMonths")
    val epsTrailingTwelveMonths: Double? = null,

    @SerialName("epsForward")
    val epsForward: Double? = null,

    @SerialName("epsCurrentYear")
    val epsCurrentYear: Double? = null,

    @SerialName("earningsTimestamp")
    val earningsTimestamp: Long? = null,

    @SerialName("earningsTimestampStart")
    val earningsTimestampStart: Long? = null,

    @SerialName("earningsTimestampEnd")
    val earningsTimestampEnd: Long? = null,

    // ===== 매출 및 수익성 (Revenue & Profitability) =====
    @SerialName("totalRevenue")
    val totalRevenue: Long? = null,

    @SerialName("revenuePerShare")
    val revenuePerShare: Double? = null,

    @SerialName("returnOnAssets")
    val returnOnAssets: Double? = null,

    @SerialName("returnOnEquity")
    val returnOnEquity: Double? = null,

    @SerialName("profitMargins")
    val profitMargins: Double? = null,

    @SerialName("grossMargins")
    val grossMargins: Double? = null,

    // ===== 재무 건전성 (Financial Health) =====
    @SerialName("totalCash")
    val totalCash: Long? = null,

    @SerialName("totalCashPerShare")
    val totalCashPerShare: Double? = null,

    @SerialName("totalDebt")
    val totalDebt: Long? = null,

    @SerialName("debtToEquity")
    val debtToEquity: Double? = null,

    @SerialName("currentRatio")
    val currentRatio: Double? = null,

    @SerialName("quickRatio")
    val quickRatio: Double? = null,

    // ===== 성장률 (Growth Rates) =====
    @SerialName("revenueGrowth")
    val revenueGrowth: Double? = null,

    @SerialName("earningsGrowth")
    val earningsGrowth: Double? = null,

    // ===== 애널리스트 의견 (Analyst Ratings) =====
    @SerialName("targetHighPrice")
    val targetHighPrice: Double? = null,

    @SerialName("targetLowPrice")
    val targetLowPrice: Double? = null,

    @SerialName("targetMeanPrice")
    val targetMeanPrice: Double? = null,

    @SerialName("targetMedianPrice")
    val targetMedianPrice: Double? = null,

    @SerialName("recommendationMean")
    val recommendationMean: Double? = null,

    @SerialName("recommendationKey")
    val recommendationKey: String? = null,

    @SerialName("numberOfAnalystOpinions")
    val numberOfAnalystOpinions: Int? = null
)
