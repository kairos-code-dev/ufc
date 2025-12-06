package com.ulalax.ufc.domain.model.realtime

import java.time.Instant

/**
 * 장전/장후 거래 정보
 *
 * @property preMarketPrice 장전 가격
 * @property preMarketChange 장전 변화
 * @property preMarketChangePercent 장전 변화율
 * @property preMarketTime 장전 시간
 * @property postMarketPrice 장후 가격
 * @property postMarketChange 장후 변화
 * @property postMarketChangePercent 장후 변화율
 * @property postMarketTime 장후 시간
 */
data class QuoteExtendedHours(
    val preMarketPrice: Double? = null,
    val preMarketChange: Double? = null,
    val preMarketChangePercent: Double? = null,
    val preMarketTime: Instant? = null,
    val postMarketPrice: Double? = null,
    val postMarketChange: Double? = null,
    val postMarketChangePercent: Double? = null,
    val postMarketTime: Instant? = null,
)
