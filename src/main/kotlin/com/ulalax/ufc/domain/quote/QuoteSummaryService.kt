package com.ulalax.ufc.domain.quote

/**
 * Yahoo Finance QuoteSummary API를 통한 주식 요약 정보 조회 서비스 인터페이스
 *
 * 이 서비스는 다음 기능을 제공합니다:
 * - 단일 심볼의 상세 주식 정보 조회
 * - 다중 심볼의 요약 정보 조회
 * - 다양한 모듈 선택으로 필요한 정보만 조회
 *
 * 지원하는 모듈:
 * - price: 기본 가격 정보
 * - summaryDetail: 상세 정보 (배당금, PER, 베타 등)
 * - financialData: 재무 정보 (EPS, PEG, 수익률 등)
 * - earningsTrend: 수익 추이
 * - earningsHistory: 수익 이력
 * - earningsDates: 수익 발표 날짜
 * - majorHolders: 주요 주주
 * - insiderTransactions: 내부자 거래
 *
 * 사용 예시:
 * ```
 * val quoteService = yahooQuoteSummaryService
 *
 * // 모든 정보를 포함한 AAPL 요약 조회
 * val summary = quoteService.getQuoteSummary("AAPL")
 *
 * // 특정 모듈만 포함한 조회
 * val priceSummary = quoteService.getQuoteSummary(
 *     symbol = "AAPL",
 *     modules = listOf("price", "summaryDetail")
 * )
 *
 * // 정규화된 데이터 조회
 * val normalized = quoteService.getStockSummary("AAPL")
 * ```
 */
interface QuoteSummaryService {

    /**
     * 단일 심볼의 전체 요약 정보를 조회합니다.
     *
     * 기본적으로 모든 모듈을 포함합니다.
     *
     * @param symbol 조회할 심볼 (예: "AAPL")
     * @return 요약 정보 객체
     * @throws UfcException 데이터 조회 실패 시
     */
    suspend fun getQuoteSummary(symbol: String): QuoteSummaryResult

    /**
     * 특정 모듈만 포함한 요약 정보를 조회합니다.
     *
     * @param symbol 조회할 심볼
     * @param modules 포함할 모듈 목록 (price, summaryDetail, financialData 등)
     * @return 요청된 모듈만 포함된 요약 정보 객체
     * @throws UfcException 데이터 조회 실패 시
     */
    suspend fun getQuoteSummary(
        symbol: String,
        modules: List<String>
    ): QuoteSummaryResult

    /**
     * 다중 심볼의 요약 정보를 조회합니다.
     *
     * 기본적으로 모든 모듈을 포함합니다.
     *
     * @param symbols 조회할 심볼 목록
     * @return 심볼별 요약 정보 맵
     * @throws UfcException 데이터 조회 실패 시
     */
    suspend fun getQuoteSummary(symbols: List<String>): Map<String, QuoteSummaryResult>

    /**
     * 다중 심볼의 요약 정보를 특정 모듈만 포함하여 조회합니다.
     *
     * @param symbols 조회할 심볼 목록
     * @param modules 포함할 모듈 목록
     * @return 심볼별 요약 정보 맵
     * @throws UfcException 데이터 조회 실패 시
     */
    suspend fun getQuoteSummary(
        symbols: List<String>,
        modules: List<String>
    ): Map<String, QuoteSummaryResult>

    /**
     * 정규화된 주식 요약 정보를 조회합니다.
     *
     * 원본 API 응답의 여러 필드를 하나의 깔끔한 StockSummary 구조로 통합합니다.
     *
     * @param symbol 조회할 심볼
     * @return 정규화된 StockSummary 객체
     * @throws UfcException 데이터 조회 실패 시
     */
    suspend fun getStockSummary(symbol: String): StockSummary

    /**
     * 다중 심볼의 정규화된 요약 정보를 조회합니다.
     *
     * @param symbols 조회할 심볼 목록
     * @return 심볼별 정규화된 요약 정보 맵
     * @throws UfcException 데이터 조회 실패 시
     */
    suspend fun getStockSummary(symbols: List<String>): Map<String, StockSummary>

    /**
     * QuoteSummary API의 원본 응답을 반환합니다.
     *
     * 사용자가 원본 API 응답 구조에 접근해야 할 경우 사용합니다.
     *
     * @param symbol 조회할 심볼
     * @param modules 포함할 모듈 목록 (null일 경우 모든 모듈 포함)
     * @return 원본 QuoteSummaryResponse 객체
     * @throws UfcException 데이터 조회 실패 시
     */
    suspend fun getRawQuoteSummary(
        symbol: String,
        modules: List<String>? = null
    ): QuoteSummaryResponse
}
