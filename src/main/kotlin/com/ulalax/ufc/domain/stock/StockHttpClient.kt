package com.ulalax.ufc.domain.stock

import com.ulalax.ufc.domain.fundamentals.FundamentalsTimeseriesResponse
import com.ulalax.ufc.domain.quote.QuoteSummaryResponse

/**
 * Stock 데이터 HTTP 통신 인터페이스
 *
 * 인터페이스가 필요한 이유:
 * - 외부 의존성(HTTP 호출)을 테스트에서 Fake로 교체하기 위함
 * - 도메인 순수성 유지 (StockService가 Ktor에 직접 의존 방지)
 * - 의존성 역전 원칙 (DIP) 적용
 *
 * 구현체:
 * - YahooStockHttpClient: Yahoo Finance API 실제 구현
 * - FakeStockHttpClient: 테스트용 Fake 구현
 *
 * 책임:
 * - HTTP 요청/응답 처리만 담당
 * - JSON 파싱은 StockService에서 수행 (문맥의 지역성)
 */
interface StockHttpClient {
    /**
     * Yahoo Finance QuoteSummary API 호출
     *
     * QuoteSummary API를 통해 회사 정보, ISIN, 발행주식수 등을 조회합니다.
     *
     * @param symbol 조회할 심볼 (예: "AAPL", "GOOGL")
     * @param modules 조회할 모듈 목록 (예: ["assetProfile", "defaultKeyStatistics"])
     * @return 원본 QuoteSummary 응답 객체
     * @throws com.ulalax.ufc.exception.UfcException HTTP 요청 실패 시
     */
    suspend fun fetchQuoteSummary(
        symbol: String,
        modules: List<String>
    ): QuoteSummaryResponse

    /**
     * Yahoo Finance Fundamentals Timeseries API 호출
     *
     * 발행주식수 히스토리 등의 시계열 데이터를 조회합니다.
     * yfinance의 get_shares_full(start, end) 메서드에 대응됩니다.
     *
     * @param symbol 조회할 심볼 (예: "AAPL", "GOOGL")
     * @param period1 시작 Unix timestamp (초 단위). null이면 기본값 사용
     * @param period2 종료 Unix timestamp (초 단위). null이면 현재 시간
     * @return 원본 Fundamentals Timeseries 응답 객체
     * @throws com.ulalax.ufc.exception.UfcException HTTP 요청 실패 시
     */
    suspend fun fetchFundamentalsTimeseries(
        symbol: String,
        period1: Long? = null,
        period2: Long? = null
    ): FundamentalsTimeseriesResponse

    /**
     * Business Insider 검색 API 호출
     *
     * ISIN 코드를 조회하기 위해 사용합니다.
     * yfinance의 get_isin() 메서드에 대응됩니다.
     *
     * @param query 검색 쿼리 (심볼 또는 회사명)
     * @return HTML/텍스트 응답
     * @throws com.ulalax.ufc.exception.UfcException HTTP 요청 실패 시
     */
    suspend fun fetchBusinessInsiderSearch(query: String): String
}
