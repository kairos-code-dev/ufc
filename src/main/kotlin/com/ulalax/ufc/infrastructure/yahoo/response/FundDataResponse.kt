package com.ulalax.ufc.infrastructure.yahoo.response

import com.ulalax.ufc.domain.quote.QuoteSummaryResponse

/**
 * Funds API를 위한 원본 API 응답 모델.
 *
 * 이는 QuoteSummaryResponse를 펀드 도메인 용도로 재사용하는 타입 별칭입니다.
 * Internal 패키지에 위치하므로 외부 노출되지 않습니다.
 */
typealias FundDataResponse = QuoteSummaryResponse
