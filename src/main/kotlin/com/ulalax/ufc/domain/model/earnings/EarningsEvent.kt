package com.ulalax.ufc.domain.model.earnings

import java.time.Instant

/**
 * 실적 발표 이벤트
 *
 * Yahoo Finance Earnings Calendar에서 제공하는 개별 실적 발표 정보를 나타냅니다.
 *
 * @property earningsDate 실적 발표 일시 (UTC)
 * @property timeZone 타임존 (IANA 형식, 예: "America/New_York")
 * @property epsEstimate EPS 추정치 (애널리스트 컨센서스)
 * @property reportedEps 실제 발표된 EPS (과거 실적만 존재)
 * @property surprisePercent 서프라이즈 비율 (%) - (실제 - 추정) / 추정 × 100
 */
data class EarningsEvent(
    val earningsDate: Instant,
    val timeZone: String?,
    val epsEstimate: Double?,
    val reportedEps: Double?,
    val surprisePercent: Double?
) {
    /**
     * 과거 실적인지 확인
     *
     * reportedEps가 존재하면 과거 실적으로 판단
     *
     * @return 과거 실적이면 true
     */
    fun isHistorical(): Boolean = reportedEps != null

    /**
     * 미래 예정 실적인지 확인
     *
     * reportedEps가 null이면 미래 실적으로 판단
     *
     * @return 미래 실적이면 true
     */
    fun isFuture(): Boolean = reportedEps == null

    /**
     * 긍정적 서프라이즈인지 확인
     *
     * surprisePercent가 0보다 크면 긍정적 서프라이즈
     *
     * @return 긍정적 서프라이즈면 true
     */
    fun hasPositiveSurprise(): Boolean = (surprisePercent ?: 0.0) > 0.0

    /**
     * 부정적 서프라이즈인지 확인
     *
     * surprisePercent가 0보다 작으면 부정적 서프라이즈
     *
     * @return 부정적 서프라이즈면 true
     */
    fun hasNegativeSurprise(): Boolean = (surprisePercent ?: 0.0) < 0.0
}
