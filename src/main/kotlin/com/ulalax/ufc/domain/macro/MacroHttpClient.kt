package com.ulalax.ufc.domain.macro

import com.ulalax.ufc.infrastructure.fred.FredObservationsResponse
import com.ulalax.ufc.infrastructure.fred.FredSeriesResponse

/**
 * Macro (거시경제) 데이터 HTTP 통신 인터페이스
 *
 * 인터페이스가 필요한 이유:
 * - 외부 의존성(HTTP 호출)을 테스트에서 Fake로 교체하기 위함
 * - 도메인 순수성 유지 (MacroService가 Ktor에 직접 의존 방지)
 * - 의존성 역전 원칙 (DIP) 적용
 *
 * 구현체:
 * - FredMacroHttpClient: FRED API 실제 구현
 * - FakeMacroHttpClient: 테스트용 Fake 구현
 *
 * 책임:
 * - HTTP 요청/응답 처리만 담당
 * - JSON 파싱은 MacroService에서 수행 (문맥의 지역성)
 */
interface MacroHttpClient {
    /**
     * FRED Series Info 조회
     *
     * 시리즈의 메타데이터를 조회합니다.
     *
     * @param seriesId 시리즈 ID (예: "GDPC1", "UNRATE")
     * @return FRED Series 응답
     * @throws com.ulalax.ufc.exception.UfcException HTTP 요청 실패 시
     */
    suspend fun fetchSeriesInfo(seriesId: String): FredSeriesResponse

    /**
     * FRED Series Observations 조회
     *
     * 특정 시리즈의 시계열 데이터를 조회합니다.
     *
     * @param seriesId 시리즈 ID (예: "GDPC1", "UNRATE")
     * @param startDate 시작일 (YYYY-MM-DD, 선택적)
     * @param endDate 종료일 (YYYY-MM-DD, 선택적)
     * @param frequency 주기 (d, w, m, q, a, 선택적)
     * @param units 단위 변환 (lin, chg, pch, pc1, log 등, 선택적)
     * @param aggregationMethod 집계 방법 (avg, sum, eop, 선택적)
     * @param sortOrder 정렬 순서 (asc, desc, 선택적)
     * @param limit 페이징 제한 (선택적)
     * @param offset 페이징 오프셋 (선택적)
     * @return FRED Observations 응답
     * @throws com.ulalax.ufc.exception.UfcException HTTP 요청 실패 시
     */
    suspend fun fetchSeriesObservations(
        seriesId: String,
        startDate: String? = null,
        endDate: String? = null,
        frequency: String? = null,
        units: String? = null,
        aggregationMethod: String? = null,
        sortOrder: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): FredObservationsResponse
}
