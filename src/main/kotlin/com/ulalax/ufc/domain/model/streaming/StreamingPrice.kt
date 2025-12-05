package com.ulalax.ufc.domain.model.streaming

/**
 * 실시간 가격 정보를 담는 경량 모델.
 *
 * WebSocket을 통해 수신된 실시간 가격 데이터를 표현합니다.
 * 최소한의 필드만 포함하여 빠른 업데이트에 최적화되어 있습니다.
 *
 * @property symbol 심볼 (예: "AAPL")
 * @property price 현재가
 * @property change 등락폭 (절대값)
 * @property changePercent 등락률 (%)
 * @property timestamp Unix timestamp (초 단위)
 * @property volume 당일 거래량
 * @property bid 매수 호가 (없을 경우 null)
 * @property ask 매도 호가 (없을 경우 null)
 * @property marketHours 시장 시간 상태
 */
data class StreamingPrice(
    val symbol: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val timestamp: Long,
    val volume: Long,
    val bid: Double?,
    val ask: Double?,
    val marketHours: MarketHours
)
