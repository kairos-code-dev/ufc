package com.ulalax.ufc.api

import com.ulalax.ufc.domain.macro.*

/**
 * 거시경제 지표 조회 API
 *
 * FRED (Federal Reserve Economic Data) API를 통해 미국 및 글로벌 거시경제 지표를 조회합니다.
 *
 * 주요 기능:
 * - 범용 시계열 조회 (getSeries)
 * - 카테고리별 통합 메서드 (getGDP, getInflation, getUnemployment, getInterestRate)
 *
 * 사용 예시:
 * ```kotlin
 * val ufc = UFC.create(UFCClientConfig(fredApiKey = "your-api-key"))
 *
 * // 범용 조회
 * val gdpSeries = ufc.macro.getSeries("GDPC1", startDate = "2020-01-01")
 *
 * // 카테고리별 통합 메서드
 * val realGDP = ufc.macro.getGDP(type = GDPType.REAL)
 * val growthRate = ufc.macro.getGDP(type = GDPType.GROWTH)
 *
 * val cpi = ufc.macro.getInflation(type = InflationType.CPI_ALL)
 * val coreCpi = ufc.macro.getInflation(type = InflationType.CPI_CORE)
 *
 * val unemploymentRate = ufc.macro.getUnemployment(type = UnemploymentType.RATE)
 * val claims = ufc.macro.getUnemployment(type = UnemploymentType.INITIAL_CLAIMS)
 *
 * val fedRate = ufc.macro.getInterestRate(type = InterestRateType.FEDERAL_FUNDS)
 * val treasury = ufc.macro.getInterestRate(type = InterestRateType.TREASURY_10Y)
 * ```
 */
interface MacroApi {

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

    // ========================================
    // 카테고리별 통합 메서드
    // ========================================

    /**
     * GDP 지표 조회 (통합)
     *
     * GDP 관련 다양한 지표를 타입별로 조회합니다.
     *
     * 지원되는 타입:
     * - REAL: Real GDP (실질 GDP)
     * - NOMINAL: Nominal GDP (명목 GDP)
     * - GROWTH: GDP Growth Rate (GDP 성장률)
     * - POTENTIAL: Potential GDP (잠재 GDP)
     *
     * @param type GDP 지표 타입
     * @param startDate 시작일 (YYYY-MM-DD, 선택적)
     * @param endDate 종료일 (YYYY-MM-DD, 선택적)
     * @return GDP 시계열 데이터
     *
     * 사용 예시:
     * ```kotlin
     * val realGDP = ufc.macro.getGDP(type = GDPType.REAL)
     * val growth = ufc.macro.getGDP(type = GDPType.GROWTH, startDate = "2020-01-01")
     * ```
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
     * 지원되는 타입:
     * - CPI_ALL: CPI All Items (소비자물가지수 - 전체)
     * - CPI_CORE: CPI Core (소비자물가지수 - 핵심, 식품/에너지 제외)
     * - PCE_ALL: PCE (개인소비지출 물가지수)
     * - PCE_CORE: PCE Core (개인소비지출 물가지수 - 핵심)
     * - PPI: Producer Price Index (생산자물가지수)
     *
     * @param type 인플레이션 지표 타입
     * @param startDate 시작일 (선택적)
     * @param endDate 종료일 (선택적)
     * @return 인플레이션 시계열 데이터
     *
     * 사용 예시:
     * ```kotlin
     * val cpi = ufc.macro.getInflation(type = InflationType.CPI_ALL)
     * val coreCpi = ufc.macro.getInflation(type = InflationType.CPI_CORE)
     * val pce = ufc.macro.getInflation(type = InflationType.PCE_CORE)
     * ```
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
     * 지원되는 타입:
     * - RATE: Unemployment Rate (실업률)
     * - INITIAL_CLAIMS: Initial Jobless Claims (신규 실업수당 청구 건수)
     * - CONTINUING_CLAIMS: Continuing Jobless Claims (계속 실업수당 청구 건수)
     * - NONFARM_PAYROLLS: Nonfarm Payrolls (비농업 고용)
     *
     * @param type 실업률/고용 지표 타입
     * @param startDate 시작일 (선택적)
     * @param endDate 종료일 (선택적)
     * @return 실업률/고용 시계열 데이터
     *
     * 사용 예시:
     * ```kotlin
     * val rate = ufc.macro.getUnemployment(type = UnemploymentType.RATE)
     * val claims = ufc.macro.getUnemployment(type = UnemploymentType.INITIAL_CLAIMS)
     * ```
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
     * 지원되는 타입:
     * - FEDERAL_FUNDS: Federal Funds Rate (연방기금금리)
     * - TREASURY_10Y: 10-Year Treasury Yield (10년물 국채 수익률)
     * - TREASURY_2Y: 2-Year Treasury Yield (2년물 국채 수익률)
     * - TREASURY_5Y: 5-Year Treasury Yield (5년물 국채 수익률)
     * - TREASURY_30Y: 30-Year Treasury Yield (30년물 국채 수익률)
     * - TREASURY_3M: 3-Month Treasury Bill Rate (3개월물 국채 수익률)
     * - MORTGAGE_30Y: 30-Year Fixed Rate Mortgage (30년 고정 모기지 금리)
     *
     * @param type 금리 지표 타입
     * @param startDate 시작일 (선택적)
     * @param endDate 종료일 (선택적)
     * @return 금리 시계열 데이터
     *
     * 사용 예시:
     * ```kotlin
     * val fedRate = ufc.macro.getInterestRate(type = InterestRateType.FEDERAL_FUNDS)
     * val treasury10y = ufc.macro.getInterestRate(type = InterestRateType.TREASURY_10Y)
     * val mortgage = ufc.macro.getInterestRate(type = InterestRateType.MORTGAGE_30Y)
     * ```
     */
    suspend fun getInterestRate(
        type: InterestRateType,
        startDate: String? = null,
        endDate: String? = null
    ): MacroSeries
}
