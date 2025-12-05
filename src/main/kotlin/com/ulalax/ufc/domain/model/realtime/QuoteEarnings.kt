package com.ulalax.ufc.domain.model.realtime

import java.time.Instant

/**
 * 수익 정보
 *
 * @property epsTrailingTwelveMonths 과거 12개월 EPS (Earnings Per Share)
 * @property epsForward 예상 EPS
 * @property epsCurrentYear 올해 EPS
 * @property earningsTimestamp 실적 발표 시간
 * @property earningsTimestampStart 실적 발표 시작 시간
 * @property earningsTimestampEnd 실적 발표 종료 시간
 */
data class QuoteEarnings(
    val epsTrailingTwelveMonths: Double? = null,
    val epsForward: Double? = null,
    val epsCurrentYear: Double? = null,
    val earningsTimestamp: Instant? = null,
    val earningsTimestampStart: Instant? = null,
    val earningsTimestampEnd: Instant? = null
)
