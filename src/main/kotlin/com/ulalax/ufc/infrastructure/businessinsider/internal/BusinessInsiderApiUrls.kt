package com.ulalax.ufc.infrastructure.businessinsider.internal

/**
 * Business Insider Markets API URL 관리
 *
 * Business Insider는 ISIN 기반 검색을 지원하는 금융 정보 사이트입니다.
 */
internal object BusinessInsiderApiUrls {
    const val BASE_URL = "https://markets.businessinsider.com"

    /**
     * 검색 자동완성 API (작동 확인됨)
     * 응답 형식: mmSuggestDeliver(...) JavaScript callback
     */
    const val SUGGEST_API = "$BASE_URL/ajax/SearchController_Suggest"

    /**
     * ISIN 또는 심볼 검색 URL 생성
     *
     * @param query ISIN (예: US0378331005) 또는 심볼 (예: AAPL)
     * @param maxResults 최대 결과 수 (기본값: 10)
     * @return 검색 API URL
     */
    fun searchUrl(
        query: String,
        maxResults: Int = 10,
    ): String = "$SUGGEST_API?max_results=$maxResults&query=$query"
}
