package com.ulalax.ufc.domain.model.market

import java.time.Instant

/**
 * 거래 시간
 *
 * 특정 거래 세션의 시작과 종료 시각을 나타냅니다.
 * 프리마켓, 정규장, 애프터마켓 등에 사용됩니다.
 *
 * @property start 시작 시각
 * @property end 종료 시각
 */
data class TradingHours(
    val start: Instant,
    val end: Instant,
)
