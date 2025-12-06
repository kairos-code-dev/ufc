package com.ulalax.ufc.domain.model.search

/**
 * 검색된 종목 정보
 *
 * @property symbol 티커 심볼 (예: "AAPL")
 * @property shortName 짧은 이름
 * @property longName 전체 이름
 * @property quoteType 자산 유형 (EQUITY, ETF, MUTUALFUND, CRYPTOCURRENCY 등)
 * @property exchange 거래소 코드 (NMS, NYQ 등)
 * @property exchangeDisplay 거래소 표시명
 * @property sector 섹터 (주식만 해당)
 * @property industry 산업 (주식만 해당)
 * @property score 검색 관련도 점수 (높을수록 관련도가 높음)
 */
data class SearchQuote(
    val symbol: String,
    val shortName: String?,
    val longName: String?,
    val quoteType: String,
    val exchange: String?,
    val exchangeDisplay: String?,
    val sector: String?,
    val industry: String?,
    val score: Double,
)
