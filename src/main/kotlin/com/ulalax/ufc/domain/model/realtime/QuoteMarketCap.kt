package com.ulalax.ufc.domain.model.realtime

/**
 * 시가총액 및 발행주식수 정보
 *
 * @property marketCap 시가총액
 * @property sharesOutstanding 발행주식수
 */
data class QuoteMarketCap(
    val marketCap: Long? = null,
    val sharesOutstanding: Long? = null
)
