package com.ulalax.ufc.domain.model.streaming

/**
 * 실시간 상세 시세 정보를 담는 모델.
 *
 * WebSocket을 통해 수신된 상세한 실시간 시세 데이터를 표현합니다.
 * StreamingPrice의 모든 필드와 추가적인 세부 정보를 포함합니다.
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
 * @property dayHigh 당일 고가
 * @property dayLow 당일 저가
 * @property openPrice 시가
 * @property previousClose 전일 종가
 * @property bidSize 매수 호가 수량 (없을 경우 null)
 * @property askSize 매도 호가 수량 (없을 경우 null)
 * @property currency 통화 (예: "USD")
 * @property exchange 거래소 (예: "NMS")
 * @property shortName 자산명 (예: "Apple Inc.")
 * @property quoteType 자산 유형
 */
data class StreamingQuote(
    val symbol: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val timestamp: Long,
    val volume: Long,
    val bid: Double?,
    val ask: Double?,
    val marketHours: MarketHours,
    val dayHigh: Double,
    val dayLow: Double,
    val openPrice: Double,
    val previousClose: Double,
    val bidSize: Long?,
    val askSize: Long?,
    val currency: String,
    val exchange: String,
    val shortName: String,
    val quoteType: QuoteType
)
