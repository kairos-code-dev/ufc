package com.ulalax.ufc.domain.macro

/**
 * 거시경제 지표 서비스 인터페이스
 *
 * FRED (Federal Reserve Economic Data) API를 통해 미국 및 글로벌 거시경제 지표를 조회합니다.
 *
 * 주요 기능:
 * - 범용 시계열 조회 (getSeries)
 * - 카테고리별 통합 메서드 (getGDP, getInflation, getUnemployment, getInterestRate)
 *
 * 사용 예시:
 * ```kotlin
 * val macroService: MacroService = ...
 *
 * // 범용 조회
 * val gdpSeries = macroService.getSeries("GDPC1", startDate = "2020-01-01")
 *
 * // 카테고리별 통합 메서드
 * val realGDP = macroService.getGDP(type = GDPType.REAL)
 * val cpi = macroService.getInflation(type = InflationType.CPI_ALL)
 * ```
 */
interface MacroService {

    /**
     * 시리즈 데이터 조회
     *
     * 특정 FRED 시리즈 ID에 해당하는 시계열 데이터를 조회합니다.
     *
     * @param seriesId FRED 시리즈 ID (예: "GDPC1", "UNRATE")
     * @param startDate 시작일 (YYYY-MM-DD, 선택적)
     * @param endDate 종료일 (YYYY-MM-DD, 선택적)
     * @param frequency 주기 변환 (d, w, m, q, a, 선택적)
     * @param units 단위 변환 (lin, chg, pch, pc1, log 등, 선택적)
     * @return 시계열 데이터
     */
    suspend fun getSeries(
        seriesId: String,
        startDate: String? = null,
        endDate: String? = null,
        frequency: String? = null,
        units: String? = null
    ): MacroSeries

    /**
     * GDP 지표 조회 (통합)
     *
     * GDP 관련 다양한 지표를 타입별로 조회합니다.
     *
     * @param type GDP 지표 타입
     * @param startDate 시작일 (YYYY-MM-DD, 선택적)
     * @param endDate 종료일 (YYYY-MM-DD, 선택적)
     * @return GDP 시계열 데이터
     */
    suspend fun getGDP(
        type: GDPType,
        startDate: String? = null,
        endDate: String? = null
    ): MacroSeries

    /**
     * 인플레이션 지표 조회 (통합)
     *
     * 다양한 인플레이션 측정 지표를 타입별로 조회합니다.
     *
     * @param type 인플레이션 지표 타입
     * @param startDate 시작일 (선택적)
     * @param endDate 종료일 (선택적)
     * @return 인플레이션 시계열 데이터
     */
    suspend fun getInflation(
        type: InflationType,
        startDate: String? = null,
        endDate: String? = null
    ): MacroSeries

    /**
     * 실업률 및 고용 지표 조회 (통합)
     *
     * 고용 관련 다양한 지표를 타입별로 조회합니다.
     *
     * @param type 실업률/고용 지표 타입
     * @param startDate 시작일 (선택적)
     * @param endDate 종료일 (선택적)
     * @return 실업률/고용 시계열 데이터
     */
    suspend fun getUnemployment(
        type: UnemploymentType,
        startDate: String? = null,
        endDate: String? = null
    ): MacroSeries

    /**
     * 금리 지표 조회 (통합)
     *
     * 다양한 금리 지표를 타입별로 조회합니다.
     *
     * @param type 금리 지표 타입
     * @param startDate 시작일 (선택적)
     * @param endDate 종료일 (선택적)
     * @return 금리 시계열 데이터
     */
    suspend fun getInterestRate(
        type: InterestRateType,
        startDate: String? = null,
        endDate: String? = null
    ): MacroSeries
}
