package com.ulalax.ufc.domain.model.earnings

/**
 * 실적 발표 캘린더
 *
 * Yahoo Finance Earnings Calendar API를 통해 조회한 실적 발표 일정을 나타냅니다.
 * 특정 심볼에 대한 과거 및 미래 실적 발표 일정이 포함됩니다.
 *
 * 사용 예시:
 * ```kotlin
 * val calendar = ufc.earningsCalendar("AAPL", limit = 12, offset = 0)
 *
 * // 이벤트 조회
 * calendar.events.forEach { event ->
 *     println("${event.earningsDate}: EPS Estimate = ${event.epsEstimate}")
 * }
 *
 * // 과거 실적 필터링
 * val historical = calendar.getHistoricalEvents()
 * ```
 *
 * @property symbol 티커 심볼 (예: "AAPL")
 * @property events 실적 이벤트 목록 (시간순 정렬)
 * @property requestedLimit 요청한 limit 파라미터
 * @property requestedOffset 요청한 offset 파라미터
 * @property actualCount 실제 반환된 이벤트 수
 */
data class EarningsCalendar(
    val symbol: String,
    val events: List<EarningsEvent>,
    val requestedLimit: Int,
    val requestedOffset: Int,
    val actualCount: Int,
) {
    /**
     * 이벤트가 비어있는지 확인
     *
     * @return 이벤트가 없으면 true
     */
    fun isEmpty(): Boolean = events.isEmpty()

    /**
     * 이벤트가 존재하는지 확인
     *
     * @return 이벤트가 있으면 true
     */
    fun isNotEmpty(): Boolean = events.isNotEmpty()

    /**
     * 과거 실적 이벤트만 필터링
     *
     * reportedEps가 존재하는 이벤트만 반환
     *
     * @return 과거 실적 이벤트 목록
     */
    fun getHistoricalEvents(): List<EarningsEvent> = events.filter { it.isHistorical() }

    /**
     * 미래 예정 실적 이벤트만 필터링
     *
     * reportedEps가 null인 이벤트만 반환
     *
     * @return 미래 실적 이벤트 목록
     */
    fun getFutureEvents(): List<EarningsEvent> = events.filter { it.isFuture() }

    /**
     * 다음 예정된 실적 발표 일정 조회
     *
     * 미래 실적 중 가장 가까운 이벤트를 반환
     *
     * @return 다음 실적 이벤트, 없으면 null
     */
    fun getNextEarnings(): EarningsEvent? = getFutureEvents().firstOrNull()

    /**
     * 가장 최근 과거 실적 조회
     *
     * 과거 실적 중 가장 최근 이벤트를 반환
     *
     * @return 최근 과거 실적 이벤트, 없으면 null
     */
    fun getLatestHistoricalEarnings(): EarningsEvent? = getHistoricalEvents().firstOrNull()
}
