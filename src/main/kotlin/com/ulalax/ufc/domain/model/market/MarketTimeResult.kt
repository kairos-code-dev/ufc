package com.ulalax.ufc.domain.model.market

import java.time.Instant

/**
 * 시장 시간 정보 결과
 *
 * 특정 시장의 거래 시간 및 현재 상태를 담고 있습니다.
 *
 * @property market 조회한 시장
 * @property exchange 거래소 코드 (예: NMS, KRW)
 * @property marketIdentifier 시장 식별자 (예: us_market, kr_market)
 * @property marketState 현재 시장 상태
 * @property open 정규 장 개장 시각
 * @property close 정규 장 폐장 시각
 * @property preMarket 프리마켓 거래 시간 (미국 시장만 해당)
 * @property postMarket 애프터마켓 거래 시간 (미국 시장만 해당)
 * @property timezone 타임존 정보
 * @property currentTime 현재 시각
 */
data class MarketTimeResult(
    val market: MarketCode,
    val exchange: String,
    val marketIdentifier: String,
    val marketState: MarketState,
    val open: Instant,
    val close: Instant,
    val preMarket: TradingHours? = null,
    val postMarket: TradingHours? = null,
    val timezone: MarketTimezone,
    val currentTime: Instant? = null,
)
