package com.ulalax.ufc.domain.funds

/**
 * ETF 또는 뮤추얼펀드의 통합 정보를 담는 데이터 모델.
 *
 * yfinance의 FundsData 클래스를 Kotlin으로 이식한 모델입니다.
 * 펀드의 기본 정보, 운영 정보, 자산 배분, 보유 종목 등을 포함합니다.
 *
 * @property symbol 펀드 심볼 (예: "SPY", "VTI", "VTSAX")
 * @property quoteType 자산 타입 ("ETF" 또는 "MUTUALFUND")
 * @property description 펀드 설명
 * @property fundOverview 펀드 개요 정보
 * @property fundOperations 펀드 운영 정보
 * @property assetClasses 자산 클래스 배분
 * @property topHoldings 상위 보유 종목 목록
 * @property equityHoldings 주식 보유 메트릭
 * @property bondHoldings 채권 보유 메트릭
 * @property bondRatings 채권 등급 분포 (예: "A": 30.5, "BBB": 20.0)
 * @property sectorWeightings 섹터별 비중 (예: "Technology": 28.5, "Finance": 15.3)
 */
data class FundData(
    val symbol: String,
    val quoteType: String,                          // ETF | MUTUALFUND
    val description: String?,
    val fundOverview: FundOverview?,
    val fundOperations: FundOperations?,
    val assetClasses: AssetClasses?,
    val topHoldings: List<Holding>?,
    val equityHoldings: EquityHoldingsMetrics?,
    val bondHoldings: BondHoldingsMetrics?,
    val bondRatings: Map<String, Double>?,
    val sectorWeightings: Map<String, Double>?
)
