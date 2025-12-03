package com.ulalax.ufc.domain.corp

import com.ulalax.ufc.domain.chart.ChartDataResponse
import com.ulalax.ufc.domain.common.Interval
import com.ulalax.ufc.domain.common.Period

/**
 * Corp (기업 행동) 데이터 HTTP 통신 인터페이스
 *
 * 인터페이스가 필요한 이유:
 * - 외부 의존성(HTTP 호출)을 테스트에서 Fake로 교체하기 위함
 * - 도메인 순수성 유지 (CorpService가 Ktor에 직접 의존 방지)
 * - 의존성 역전 원칙 (DIP) 적용
 *
 * 구현체:
 * - YahooCorpHttpClient: Yahoo Finance Chart API 실제 구현
 * - FakeCorpHttpClient: 테스트용 Fake 구현
 *
 * 책임:
 * - HTTP 요청/응답 처리만 담당
 * - JSON 파싱은 CorpService에서 수행 (문맥의 지역성)
 */
interface CorpHttpClient {
    /**
     * Yahoo Finance Chart API 호출 (events 포함)
     *
     * Chart API를 통해 배당금, 주식 분할, 자본이득 이벤트를 조회합니다.
     *
     * @param symbol 조회할 심볼 (예: "AAPL", "SPY")
     * @param interval 데이터 간격
     * @param period 조회 기간
     * @param includeEvents 이벤트 데이터(배당금, 분할, 자본이득) 포함 여부
     * @return 원본 Chart 응답 객체
     * @throws com.ulalax.ufc.exception.UfcException HTTP 요청 실패 시
     */
    suspend fun fetchChartData(
        symbol: String,
        interval: Interval,
        period: Period,
        includeEvents: Boolean
    ): ChartDataResponse
}
