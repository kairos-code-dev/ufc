package com.ulalax.ufc.infrastructure.yahoo.internal.response

/**
 * Earnings Calendar HTML 스크래핑 응답
 *
 * Yahoo Finance Earnings Calendar 페이지의 HTML 파싱 결과를 나타냅니다.
 * 내부 응답 타입으로, 외부에 노출되지 않습니다.
 *
 * @property hasTable HTML에 테이블이 존재하는지 여부
 * @property rows 파싱된 테이블 행 목록
 */
internal data class EarningsCalendarHtmlResponse(
    val hasTable: Boolean,
    val rows: List<EarningsTableRow>,
)
