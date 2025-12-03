package com.ulalax.ufc.domain.price

import com.ulalax.ufc.infrastructure.yahoo.response.ChartResponse
import com.ulalax.ufc.infrastructure.yahoo.response.PriceResponse
import com.ulalax.ufc.domain.common.Interval
import com.ulalax.ufc.domain.common.Period
import java.time.LocalDate

/**
 * Price 데이터 HTTP 통신 인터페이스
 *
 * 인터페이스가 필요한 이유:
 * - 외부 의존성(HTTP 호출)을 테스트에서 Fake로 교체하기 위함
 * - 도메인 순수성 유지 (PriceService가 Ktor에 직접 의존 방지)
 *
 * 교체 가능성은 없지만, 테스트 격리를 위해 추상화 필요
 *
 * 구현체:
 * - YahooHttpClient: Yahoo Finance API 실제 구현
 * - FakePriceHttpClient: 테스트용 Fake 구현
 *
 * 책임:
 * - HTTP 요청/응답 처리만 담당
 * - JSON 파싱은 PriceService에서 수행 (문맥의 지역성)
 */
interface PriceHttpClient {
    /**
     * Yahoo Finance QuoteSummary API 호출
     *
     * quoteSummary API를 통해 현재 가격 및 요약 정보를 조회합니다.
     *
     * @param symbol 조회할 심볼 (예: "AAPL", "SPY")
     * @param modules 조회할 모듈 목록 (예: ["price", "summaryDetail"])
     * @return 원본 JSON 응답 객체 (PriceResponse)
     * @throws UfcException HTTP 요청 실패 시
     */
    suspend fun fetchQuoteSummary(
        symbol: String,
        modules: List<String>
    ): PriceResponse

    /**
     * Yahoo Finance Chart API 호출 (기간 기반)
     *
     * Chart API를 통해 OHLCV 히스토리 데이터를 조회합니다.
     *
     * @param symbol 조회할 심볼
     * @param interval 데이터 간격 (예: Interval.OneDay)
     * @param period 조회 기간 (예: Period.OneYear)
     * @return 원본 JSON 응답 객체 (ChartResponse)
     * @throws UfcException HTTP 요청 실패 시
     */
    suspend fun fetchChart(
        symbol: String,
        interval: Interval,
        period: Period
    ): ChartResponse

    /**
     * Yahoo Finance Chart API 호출 (날짜 범위 기반)
     *
     * Chart API를 통해 특정 날짜 범위의 OHLCV 데이터를 조회합니다.
     *
     * @param symbol 조회할 심볼
     * @param start 시작 날짜
     * @param end 종료 날짜
     * @param interval 데이터 간격
     * @return 원본 JSON 응답 객체 (ChartResponse)
     * @throws UfcException HTTP 요청 실패 시
     */
    suspend fun fetchChartByDateRange(
        symbol: String,
        start: LocalDate,
        end: LocalDate,
        interval: Interval
    ): ChartResponse

    /**
     * Yahoo Finance Chart API 호출 (이벤트 포함)
     *
     * Chart API를 통해 배당금, 주식 분할 등 이벤트 데이터를 포함한 차트 데이터를 조회합니다.
     *
     * @param symbol 조회할 심볼
     * @param interval 데이터 간격
     * @param period 조회 기간
     * @param includeEvents 이벤트 데이터 포함 여부
     * @return 원본 JSON 응답 객체 (ChartResponse)
     * @throws UfcException HTTP 요청 실패 시
     */
    suspend fun fetchChartWithEvents(
        symbol: String,
        interval: Interval,
        period: Period,
        includeEvents: Boolean
    ): ChartResponse
}
