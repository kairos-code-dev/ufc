package com.ulalax.ufc.domain.model.lookup

/**
 * 검색된 금융상품 정보
 *
 * Yahoo Finance Lookup API에서 반환되는 개별 검색 결과를 나타냅니다.
 *
 * @property symbol 티커 심볼 (예: "AAPL", "MSFT")
 * @property name 금융상품의 정식 명칭 (예: "Apple Inc.")
 * @property exchange 거래소 코드 (예: "NMS")
 * @property exchangeDisplay 거래소 표시명 (예: "NASDAQ")
 * @property typeCode 타입 코드 (예: "S" for Stock, "E" for ETF)
 * @property typeDisplay 타입 표시명 (예: "Equity", "ETF")
 * @property industry 산업 분류 코드
 * @property industryDisplay 산업 표시명 (예: "Consumer Electronics")
 * @property sector 섹터 분류 코드
 * @property sectorDisplay 섹터 표시명 (예: "Technology")
 * @property score 검색어와의 관련도 점수 (높을수록 관련성이 높음)
 * @property isYahooFinance Yahoo Finance에서 지원 여부
 */
data class LookupDocument(
    val symbol: String,
    val name: String,
    val exchange: String? = null,
    val exchangeDisplay: String? = null,
    val typeCode: String? = null,
    val typeDisplay: String? = null,
    val industry: String? = null,
    val industryDisplay: String? = null,
    val sector: String? = null,
    val sectorDisplay: String? = null,
    val score: Double? = null,
    val isYahooFinance: Boolean? = null,
)
