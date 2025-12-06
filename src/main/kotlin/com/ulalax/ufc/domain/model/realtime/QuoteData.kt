package com.ulalax.ufc.domain.model.realtime

/**
 * Quote API의 실시간 시장 데이터 및 주식 기본 정보
 *
 * Quote API는 QuoteSummary API와 달리 실시간 시장 데이터와 다양한 기본 정보를
 * 한 번의 호출로 제공합니다. 다중 심볼 조회가 가능하며 응답 속도가 빠릅니다.
 *
 * @property identification 기본 식별 정보 (심볼, 이름, 거래소 등)
 * @property pricing 가격 정보 (현재가, 시가, 고가, 저가, 거래량 등)
 * @property extendedHours 장전/장후 거래 정보
 * @property fiftyTwoWeek 52주 고저가 정보
 * @property movingAverages 이동평균 정보
 * @property volumes 거래량 평균 정보
 * @property marketCap 시가총액 및 발행주식수 정보
 * @property dividends 배당 정보
 * @property financialRatios 재무 비율 정보 (PER, PBR 등)
 * @property earnings 수익 정보 (EPS 등)
 * @property revenue 매출 및 수익성 정보
 * @property financialHealth 재무 건전성 정보
 * @property growthRates 성장률 정보
 * @property analystRatings 애널리스트 의견 정보
 */
data class QuoteData(
    val identification: QuoteIdentification? = null,
    val pricing: QuotePricing? = null,
    val extendedHours: QuoteExtendedHours? = null,
    val fiftyTwoWeek: QuoteFiftyTwoWeek? = null,
    val movingAverages: QuoteMovingAverages? = null,
    val volumes: QuoteVolumes? = null,
    val marketCap: QuoteMarketCap? = null,
    val dividends: QuoteDividends? = null,
    val financialRatios: QuoteFinancialRatios? = null,
    val earnings: QuoteEarnings? = null,
    val revenue: QuoteRevenue? = null,
    val financialHealth: QuoteFinancialHealth? = null,
    val growthRates: QuoteGrowthRates? = null,
    val analystRatings: QuoteAnalystRatings? = null,
)
