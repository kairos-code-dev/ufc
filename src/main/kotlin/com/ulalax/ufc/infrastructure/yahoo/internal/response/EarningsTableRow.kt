package com.ulalax.ufc.infrastructure.yahoo.internal.response

/**
 * Earnings Calendar 테이블 행
 *
 * Yahoo Finance Earnings Calendar HTML 테이블의 개별 행 데이터를 나타냅니다.
 * 내부 응답 타입으로, 외부에 노출되지 않습니다.
 *
 * 모든 필드는 HTML에서 추출한 원본 문자열 형태로 저장되며,
 * YahooClient에서 Domain 타입으로 변환됩니다.
 *
 * @property symbol 티커 심볼 (예: "AAPL")
 * @property company 회사명 (예: "Apple Inc.")
 * @property earningsDateRaw 실적 발표 일시 원본 (예: "October 30, 2025 at 4 PM EDT")
 * @property epsEstimateRaw EPS 추정치 원본 (예: "1.54", "-")
 * @property reportedEpsRaw 실제 발표된 EPS 원본 (예: "1.64", "-")
 * @property surprisePercentRaw 서프라이즈 비율 원본 (예: "6.49%", "-")
 */
internal data class EarningsTableRow(
    val symbol: String,
    val company: String,
    val earningsDateRaw: String,
    val epsEstimateRaw: String,
    val reportedEpsRaw: String,
    val surprisePercentRaw: String
)
