package com.ulalax.ufc.domain.funds

/**
 * 펀드가 보유하고 있는 종목 정보.
 *
 * 펀드의 포트폴리오에 포함된 개별 증권(주식, ETF 등)을 나타냅니다.
 *
 * @property symbol 증권의 심볼 (예: "AAPL", "MSFT")
 * @property name 증권의 이름 (예: "Apple Inc", "Microsoft Corporation")
 * @property holdingPercent 펀드 내 보유 비중 (%)
 */
data class Holding(
    val symbol: String,
    val name: String,
    val holdingPercent: Double
)
