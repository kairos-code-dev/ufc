package com.ulalax.ufc.domain.stock

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
}
