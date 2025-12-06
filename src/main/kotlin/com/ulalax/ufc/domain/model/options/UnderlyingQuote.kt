package com.ulalax.ufc.domain.model.options

/**
 * 기초 자산 가격 정보
 *
 * 옵션의 기초 자산(주식/지수)의 현재 가격 정보를 나타냅니다.
 *
 * @property symbol 심볼 (예: "AAPL")
 * @property shortName 짧은 이름 (예: "Apple Inc.")
 * @property regularMarketPrice 현재가
 * @property regularMarketChange 가격 변동
 * @property regularMarketChangePercent 변동률 (%)
 * @property regularMarketVolume 거래량
 * @property regularMarketTime 시장 시간 (Unix timestamp, seconds)
 */
data class UnderlyingQuote(
    val symbol: String,
    val shortName: String? = null,
    val regularMarketPrice: Double? = null,
    val regularMarketChange: Double? = null,
    val regularMarketChangePercent: Double? = null,
    val regularMarketVolume: Long? = null,
    val regularMarketTime: Long? = null,
)
