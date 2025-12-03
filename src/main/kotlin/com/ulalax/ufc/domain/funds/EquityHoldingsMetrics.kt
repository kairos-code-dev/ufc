package com.ulalax.ufc.domain.funds

/**
 * 펀드가 보유한 주식의 메트릭.
 *
 * 펀드가 보유한 주식들의 평가 지표를 나타냅니다.
 * PER, PBR, PSR, Price/Cash Flow 등의 메트릭과 중간값 시가총액, 3년 순이익 성장률 등을 포함합니다.
 *
 * @property priceToEarnings PER (Price to Earnings Ratio) - 주가수익률
 * @property priceToBook PBR (Price to Book Ratio) - 주가순자산비율
 * @property priceToSales PSR (Price to Sales Ratio) - 주가매출액비율
 * @property priceToCashflow 주가현금흐름비율
 * @property medianMarketCap 중간값 시가총액
 * @property threeYearEarningsGrowth 3년 순이익 성장률 (%)
 */
data class EquityHoldingsMetrics(
    val priceToEarnings: MetricValue?,
    val priceToBook: MetricValue?,
    val priceToSales: MetricValue?,
    val priceToCashflow: MetricValue?,
    val medianMarketCap: MetricValue?,
    val threeYearEarningsGrowth: MetricValue?
)
