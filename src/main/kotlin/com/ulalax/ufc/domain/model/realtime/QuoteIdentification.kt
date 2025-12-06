package com.ulalax.ufc.domain.model.realtime

/**
 * 주식의 기본 식별 정보
 *
 * @property symbol 티커 심볼
 * @property longName 정식 명칭
 * @property shortName 약식 명칭
 * @property exchange 거래소 코드
 * @property timezoneName 거래소 시간대
 * @property timezoneShortName 시간대 약어
 * @property quoteType 자산 유형 (EQUITY, ETF, INDEX 등)
 * @property currency 통화 코드
 * @property market 시장 분류
 */
data class QuoteIdentification(
    val symbol: String,
    val longName: String? = null,
    val shortName: String? = null,
    val exchange: String? = null,
    val timezoneName: String? = null,
    val timezoneShortName: String? = null,
    val quoteType: String? = null,
    val currency: String? = null,
    val market: String? = null,
)
