package com.ulalax.ufc.domain.model.screener

/**
 * Screener API에서 반환되는 종목 정보
 *
 * @property symbol 티커 심볼
 * @property shortName 짧은 이름
 * @property longName 긴 이름
 * @property quoteType 자산 유형 (EQUITY, MUTUALFUND)
 * @property sector 섹터
 * @property industry 산업
 * @property exchange 거래소
 * @property marketCap 시가총액
 * @property regularMarketPrice 정규시장 가격
 * @property regularMarketChange 가격 변동
 * @property regularMarketChangePercent 등락률 (%)
 * @property regularMarketVolume 거래량
 * @property additionalFields 추가 필드 (정렬 필드 등 동적 필드)
 */
data class ScreenerQuote(
    val symbol: String,
    val shortName: String? = null,
    val longName: String? = null,
    val quoteType: String? = null,
    val sector: String? = null,
    val industry: String? = null,
    val exchange: String? = null,
    val marketCap: Long? = null,
    val regularMarketPrice: Double? = null,
    val regularMarketChange: Double? = null,
    val regularMarketChangePercent: Double? = null,
    val regularMarketVolume: Long? = null,
    val additionalFields: Map<String, Any?> = emptyMap()
)
