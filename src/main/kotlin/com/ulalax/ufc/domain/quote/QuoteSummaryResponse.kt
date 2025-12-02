package com.ulalax.ufc.domain.quote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Yahoo Finance API의 {raw, fmt} 형식 값을 나타내는 헬퍼 클래스
 */
@Serializable
data class RawFormatted(
    @SerialName("raw")
    val raw: JsonElement? = null,

    @SerialName("fmt")
    val fmt: String? = null
) {
    /** Double 값 추출 */
    val doubleValue: Double? get() = raw?.jsonPrimitive?.doubleOrNull

    /** Long 값 추출 */
    val longValue: Long? get() = raw?.jsonPrimitive?.longOrNull
}

/**
 * Yahoo Finance QuoteSummary API의 응답을 나타내는 모델
 *
 * quoteSummary 엔드포인트는 다양한 모듈의 상세 주식 정보를 반환합니다:
 * - price: 기본 가격 정보
 * - summaryDetail: 주식의 상세 정보
 * - financialData: 재무 정보
 * - earningsTrend: 수익 추이
 * - earningsHistory: 수익 이력
 * - earningsDates: 수익 발표 날짜
 * - majorHolders: 주요 주주
 * - insiderTransactions: 내부자 거래
 */
@Serializable
data class QuoteSummaryResponse(
    @SerialName("quoteSummary")
    val quoteSummary: QuoteSummary
)

/**
 * QuoteSummary의 컨테이너
 */
@Serializable
data class QuoteSummary(
    @SerialName("result")
    val result: List<QuoteSummaryResult>? = null,

    @SerialName("error")
    val error: QuoteError? = null
)

/**
 * 단일 심볼의 QuoteSummary 데이터
 *
 * 요청된 모듈별로 다양한 정보를 포함합니다.
 */
@Serializable
data class QuoteSummaryResult(
    // 기본 가격 정보
    @SerialName("price")
    val price: Price? = null,

    // 상세 정보
    @SerialName("summaryDetail")
    val summaryDetail: SummaryDetail? = null,

    // 재무 정보
    @SerialName("financialData")
    val financialData: FinancialData? = null,

    // 수익 추이
    @SerialName("earningsTrend")
    val earningsTrend: EarningsTrend? = null,

    // 수익 이력
    @SerialName("earningsHistory")
    val earningsHistory: EarningsHistory? = null,

    // 수익 발표 날짜
    @SerialName("earningsDates")
    val earningsDates: EarningsDates? = null,

    // 주요 주주
    @SerialName("majorHolders")
    val majorHolders: MajorHolders? = null,

    // 내부자 거래
    @SerialName("insiderTransactions")
    val insiderTransactions: InsiderTransactions? = null
)

/**
 * 기본 가격 정보
 */
@Serializable
data class Price(
    @SerialName("maxAge")
    val maxAge: Int? = null,

    // 현재 가격
    @SerialName("regularMarketPrice")
    val regularMarketPrice: RawFormatted? = null,

    // 통화
    @SerialName("currency")
    val currency: String? = null,

    // 심볼
    @SerialName("symbol")
    val symbol: String? = null,

    // 거래소
    @SerialName("exchange")
    val exchange: String? = null,

    // 장 시간 후 변화
    @SerialName("postMarketChangePercent")
    val postMarketChangePercent: RawFormatted? = null,

    // 장 시간 후 가격
    @SerialName("postMarketPrice")
    val postMarketPrice: RawFormatted? = null,

    // 53주 최저가
    @SerialName("fiftyTwoWeekLow")
    val fiftyTwoWeekLow: RawFormatted? = null,

    // 53주 최고가
    @SerialName("fiftyTwoWeekHigh")
    val fiftyTwoWeekHigh: RawFormatted? = null,

    // 52주 변화
    @SerialName("fiftyTwoWeekChangePercent")
    val fiftyTwoWeekChangePercent: RawFormatted? = null,

    // 공시 당시 가격
    @SerialName("regularMarketDayRange")
    val regularMarketDayRange: String? = null,

    // 이전 거래일 종가
    @SerialName("regularMarketPreviousClose")
    val regularMarketPreviousClose: RawFormatted? = null,

    // 변화
    @SerialName("regularMarketChange")
    val regularMarketChange: RawFormatted? = null,

    // 변화 비율
    @SerialName("regularMarketChangePercent")
    val regularMarketChangePercent: RawFormatted? = null
)

/**
 * 상세 주식 정보
 */
@Serializable
data class SummaryDetail(
    @SerialName("maxAge")
    val maxAge: Int? = null,

    // 배당 수익률
    @SerialName("dividendRate")
    val dividendRate: RawFormatted? = null,

    // 배당 수익률 (퍼센트)
    @SerialName("dividendYield")
    val dividendYield: RawFormatted? = null,

    // 거래량
    @SerialName("averageVolume")
    val averageVolume: RawFormatted? = null,

    // 거래 규모 (50일 평균)
    @SerialName("averageVolume10days")
    val averageVolume10days: RawFormatted? = null,

    // 베타
    @SerialName("beta")
    val beta: RawFormatted? = null,

    // 일일 고가
    @SerialName("regularMarketDayHigh")
    val regularMarketDayHigh: RawFormatted? = null,

    // 일일 저가
    @SerialName("regularMarketDayLow")
    val regularMarketDayLow: RawFormatted? = null,

    // 일일 거래량
    @SerialName("regularMarketVolume")
    val regularMarketVolume: RawFormatted? = null,

    // 시가총액
    @SerialName("marketCap")
    val marketCap: RawFormatted? = null,

    // 매출
    @SerialName("revenue")
    val revenue: RawFormatted? = null,

    // 배당 지급 날짜
    @SerialName("dividendDate")
    val dividendDate: RawFormatted? = null,

    // 배당 지급 날짜 (타임스탬프)
    @SerialName("exDividendDate")
    val exDividendDate: RawFormatted? = null,

    // 발행주식수
    @SerialName("sharesOutstanding")
    val sharesOutstanding: RawFormatted? = null,

    // 공개 부채
    @SerialName("debtToEquity")
    val debtToEquity: RawFormatted? = null,

    // PER (주가수익비율)
    @SerialName("trailingPE")
    val trailingPE: RawFormatted? = null,

    // 포워드 PER
    @SerialName("forwardPE")
    val forwardPE: RawFormatted? = null,

    // PBR (주가순자산비율)
    @SerialName("priceToBook")
    val priceToBook: RawFormatted? = null,

    // 52주 고가
    @SerialName("fiftyTwoWeekHigh")
    val fiftyTwoWeekHigh: RawFormatted? = null,

    // 52주 저가
    @SerialName("fiftyTwoWeekLow")
    val fiftyTwoWeekLow: RawFormatted? = null
)

/**
 * 재무 정보
 */
@Serializable
data class FinancialData(
    @SerialName("maxAge")
    val maxAge: Int? = null,

    // 현금 흐름
    @SerialName("operatingCashflow")
    val operatingCashflow: RawFormatted? = null,

    // 잉여 현금 흐름
    @SerialName("freeCashflow")
    val freeCashflow: RawFormatted? = null,

    // 총 부채
    @SerialName("totalDebt")
    val totalDebt: RawFormatted? = null,

    // 총 현금
    @SerialName("totalCash")
    val totalCash: RawFormatted? = null,

    // 장기 부채
    @SerialName("longTermDebt")
    val longTermDebt: RawFormatted? = null,

    // 현재 비율
    @SerialName("currentRatio")
    val currentRatio: RawFormatted? = null,

    // ROE (자기자본이익률)
    @SerialName("returnOnEquity")
    val returnOnEquity: RawFormatted? = null,

    // ROA (자산이익률)
    @SerialName("returnOnAssets")
    val returnOnAssets: RawFormatted? = null,

    // PEG 비율
    @SerialName("pegRatio")
    val pegRatio: RawFormatted? = null,

    // 순이익률
    @SerialName("profitMargins")
    val profitMargins: RawFormatted? = null,

    // 매출 성장률
    @SerialName("revenueGrowth")
    val revenueGrowth: RawFormatted? = null,

    // 수익 성장률
    @SerialName("earningsGrowth")
    val earningsGrowth: RawFormatted? = null,

    // 올해 목표 가격
    @SerialName("targetPriceHigh")
    val targetPriceHigh: RawFormatted? = null,

    // 목표 가격 (낮음)
    @SerialName("targetPriceLow")
    val targetPriceLow: RawFormatted? = null,

    // 목표 가격 (평균)
    @SerialName("targetPriceMean")
    val targetPriceMean: RawFormatted? = null,

    // 추천
    @SerialName("recommendationKey")
    val recommendationKey: String? = null,

    // 추천 수
    @SerialName("numberOfAnalysts")
    val numberOfAnalysts: Int? = null
)

/**
 * 수익 추이
 */
@Serializable
data class EarningsTrend(
    @SerialName("maxAge")
    val maxAge: Int? = null,

    @SerialName("trend")
    val trend: List<EarningsTrendData>? = null,

    @SerialName("earningsHistory")
    val earningsHistory: List<EarningsTrendData>? = null
)

/**
 * 수익 추이 데이터
 */
@Serializable
data class EarningsTrendData(
    @SerialName("maxAge")
    val maxAge: Int? = null,

    // 기간 (예: "1Q2023")
    @SerialName("period")
    val period: String? = null,

    // 종료 날짜
    @SerialName("endDate")
    val endDate: String? = null,

    // 추정 수익
    @SerialName("epsEstimate")
    val epsEstimate: RawFormatted? = null,

    // 실제 수익
    @SerialName("epsActual")
    val epsActual: RawFormatted? = null,

    // 차이
    @SerialName("epsDifference")
    val epsDifference: RawFormatted? = null,

    // 놀람 비율
    @SerialName("surprisePercent")
    val surprisePercent: RawFormatted? = null
)

/**
 * 수익 이력
 */
@Serializable
data class EarningsHistory(
    @SerialName("maxAge")
    val maxAge: Int? = null,

    @SerialName("history")
    val history: List<EarningsTrendData>? = null
)

/**
 * 수익 발표 날짜
 */
@Serializable
data class EarningsDates(
    @SerialName("maxAge")
    val maxAge: Int? = null,

    // 다음 발표 날짜
    @SerialName("earningsDate")
    val earningsDate: List<Long>? = null,

    // 평균 수익 발표 날짜
    @SerialName("earningsAverage")
    val earningsAverage: RawFormatted? = null,

    // 예상 수익
    @SerialName("earningsLow")
    val earningsLow: RawFormatted? = null,

    // 예상 수익 (높음)
    @SerialName("earningsHigh")
    val earningsHigh: RawFormatted? = null
)

/**
 * 주요 주주
 */
@Serializable
data class MajorHolders(
    @SerialName("maxAge")
    val maxAge: Int? = null,

    @SerialName("holders")
    val holders: List<Holder>? = null
)

/**
 * 주주 정보
 */
@Serializable
data class Holder(
    @SerialName("maxAge")
    val maxAge: Int? = null,

    // 기관명
    @SerialName("holder")
    val holder: String? = null,

    // 보유 비율
    @SerialName("value")
    val value: RawFormatted? = null
)

/**
 * 내부자 거래
 */
@Serializable
data class InsiderTransactions(
    @SerialName("maxAge")
    val maxAge: Int? = null,

    @SerialName("transactions")
    val transactions: List<InsiderTransaction>? = null
)

/**
 * 내부자 거래 정보
 */
@Serializable
data class InsiderTransaction(
    @SerialName("maxAge")
    val maxAge: Int? = null,

    // 거래자명
    @SerialName("filerName")
    val filerName: String? = null,

    // 관계
    @SerialName("relationship")
    val relationship: String? = null,

    // 거래 날짜
    @SerialName("transactionDate")
    val transactionDate: RawFormatted? = null,

    // 거래량
    @SerialName("transactionShares")
    val transactionShares: RawFormatted? = null,

    // 거래 가격
    @SerialName("transactionPrice")
    val transactionPrice: RawFormatted? = null,

    // 거래 후 주식수
    @SerialName("sharesOwned")
    val sharesOwned: RawFormatted? = null
)

/**
 * 에러 응답
 */
@Serializable
data class QuoteError(
    @SerialName("code")
    val code: String? = null,

    @SerialName("description")
    val description: String? = null
)

/**
 * 정규화된 주식 요약 정보
 *
 * API 응답의 여러 필드를 하나의 깔끔한 데이터 구조로 통합합니다.
 */
data class StockSummary(
    val symbol: String,
    val currency: String?,
    val exchange: String?,
    // 가격 정보
    val currentPrice: Double?,
    val previousClose: Double?,
    val dayHigh: Double?,
    val dayLow: Double?,
    val fiftyTwoWeekHigh: Double?,
    val fiftyTwoWeekLow: Double?,
    val fiftyTwoWeekChange: Double?,
    val fiftyTwoWeekChangePercent: Double?,
    // 거래 정보
    val volume: Long?,
    val averageVolume: Long?,
    val averageVolume10Days: Long?,
    val marketCap: Long?,
    val sharesOutstanding: Long?,
    // 배당금 정보
    val dividendRate: Double?,
    val dividendYield: Double?,
    val dividendDate: Long?,
    val exDividendDate: Long?,
    // 평가 지표
    val trailingPE: Double?,
    val forwardPE: Double?,
    val priceToBook: Double?,
    val beta: Double?,
    val debtToEquity: Double?,
    // 재무 정보
    val operatingCashflow: Long?,
    val freeCashflow: Long?,
    val totalDebt: Long?,
    val totalCash: Long?,
    val currentRatio: Double?,
    val returnOnEquity: Double?,
    val returnOnAssets: Double?,
    val profitMargins: Double?,
    val revenueGrowth: Double?,
    val earningsGrowth: Double?,
    // 분석가 정보
    val recommendationKey: String?,
    val numberOfAnalysts: Int?,
    val targetPriceMean: Double?,
    val targetPriceLow: Double?,
    val targetPriceHigh: Double?
)
