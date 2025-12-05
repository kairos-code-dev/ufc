package com.ulalax.ufc.infrastructure.yahoo.internal

/**
 * Yahoo Finance API의 모든 URL 상수를 정의하는 객체
 * 리팩토링을 통해 API URL 변경 시 한 곳에서만 수정할 수 있도록 구성
 */
object YahooApiUrls {

    // ===== Base URLs =====
    /**
     * Query1 서버 - 기본 데이터 조회용 엔드포인트
     */
    const val QUERY1 = "https://query1.finance.yahoo.com"

    /**
     * Query2 서버 - 차트 및 상세 데이터 조회용 엔드포인트
     */
    const val QUERY2 = "https://query2.finance.yahoo.com"

    /**
     * Yahoo Finance 메인 사이트
     * Crumb 취득 등의 용도로 사용
     */
    const val ROOT = "https://finance.yahoo.com"

    /**
     * Yahoo Finance Connect (FC) 서버
     * 추가 금융 데이터 조회용
     */
    const val FC = "https://fc.yahoo.com"

    // ===== API Endpoints =====

    /**
     * 주식/지수 차트 데이터 엔드포인트
     * 예: $CHART?symbols=AAPL&interval=1d&range=1y
     */
    const val CHART = "$QUERY2/v8/finance/chart"

    /**
     * 주식 요약 정보 엔드포인트
     * 예: $QUOTE_SUMMARY?symbols=AAPL&modules=price,summaryDetail
     */
    const val QUOTE_SUMMARY = "$QUERY2/v10/finance/quoteSummary"

    /**
     * CRUMB 취득 엔드포인트
     * Yahoo API 호출 시 필요한 crumb 토큰을 반환
     */
    const val CRUMB = "$QUERY1/v1/test/getcrumb"

    /**
     * 주식 검색 엔드포인트
     * 예: $SEARCH?q=apple&region=US&lang=en-US
     */
    const val SEARCH = "$QUERY1/v1/finance/search"

    /**
     * 주식 스크리너 엔드포인트
     * 특정 조건에 맞는 주식 목록을 반환
     */
    const val SCREENER = "$QUERY1/v1/finance/screener"

    /**
     * Fundamentals Timeseries 엔드포인트
     * 발행주식수 히스토리 등의 시계열 데이터를 반환
     * 예: $FUNDAMENTALS_TIMESERIES/AAPL?period1=1609459200&period2=1640995200
     */
    const val FUNDAMENTALS_TIMESERIES = "$QUERY2/ws/fundamentals-timeseries/v1/finance/timeseries"
}
