package com.ulalax.ufc.businessinsider.model

/**
 * ISIN(International Securities Identification Number) 검색 결과
 *
 * @property isin 국제증권식별번호 (12자리 영숫자)
 * @property symbol 거래소 심볼 (예: AAPL, MSFT)
 * @property name 종목명
 * @property exchange 거래소 코드 (예: NASDAQ, NYSE)
 * @property currency 통화 코드 (예: USD, KRW)
 * @property type 증권 유형 (예: Stock, ETF, Bond)
 */
data class IsinSearchResult(
    val isin: String,
    val symbol: String,
    val name: String,
    val exchange: String? = null,
    val currency: String? = null,
    val type: String? = null
)
