package com.ulalax.ufc.domain.macro

/**
 * GDP 지표 타입
 *
 * GDP 관련 다양한 지표를 enum으로 정의합니다.
 * 각 타입은 해당하는 FRED Series ID를 포함합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val gdp = ufc.macro.getGDP(type = GDPType.REAL)
 * val growth = ufc.macro.getGDP(type = GDPType.GROWTH)
 * ```
 */
enum class GDPType(
    val seriesId: String,
    val description: String
) {
    /**
     * Real GDP (실질 GDP)
     * - 주기: Quarterly
     * - 단위: Billions of Chained 2017 Dollars
     */
    REAL(
        seriesId = FredSeriesIds.GDP.REAL_GDP,
        description = "Real GDP"
    ),

    /**
     * Nominal GDP (명목 GDP)
     * - 주기: Quarterly
     * - 단위: Billions of Dollars
     */
    NOMINAL(
        seriesId = FredSeriesIds.GDP.NOMINAL_GDP,
        description = "Nominal GDP"
    ),

    /**
     * GDP Growth Rate (GDP 성장률)
     * - 주기: Quarterly
     * - 단위: Percent Change, Seasonally Adjusted Annual Rate
     */
    GROWTH(
        seriesId = FredSeriesIds.GDP.GDP_GROWTH,
        description = "GDP Growth Rate"
    ),

    /**
     * Potential GDP (잠재 GDP)
     * - 주기: Quarterly
     * - 단위: Billions of Chained 2017 Dollars
     */
    POTENTIAL(
        seriesId = FredSeriesIds.GDP.POTENTIAL_GDP,
        description = "Potential GDP"
    );
}

/**
 * 인플레이션 지표 타입
 *
 * 다양한 인플레이션 측정 지표를 enum으로 정의합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val cpi = ufc.macro.getInflation(type = InflationType.CPI_ALL)
 * val coreCpi = ufc.macro.getInflation(type = InflationType.CPI_CORE)
 * ```
 */
enum class InflationType(
    val seriesId: String,
    val description: String
) {
    /**
     * CPI All Items (소비자물가지수 - 전체)
     * - 주기: Monthly
     * - 단위: Index 1982-84=100, Seasonally Adjusted
     */
    CPI_ALL(
        seriesId = FredSeriesIds.Inflation.CPI,
        description = "CPI All Items"
    ),

    /**
     * CPI Core (소비자물가지수 - 핵심)
     * - 주기: Monthly
     * - 단위: Index 1982-84=100, Seasonally Adjusted
     * - 식품 및 에너지 제외
     */
    CPI_CORE(
        seriesId = FredSeriesIds.Inflation.CORE_CPI,
        description = "CPI Core (excluding Food & Energy)"
    ),

    /**
     * PCE (개인소비지출 물가지수)
     * - 주기: Monthly
     * - 단위: Index 2017=100, Seasonally Adjusted
     */
    PCE_ALL(
        seriesId = FredSeriesIds.Inflation.PCE,
        description = "PCE Price Index"
    ),

    /**
     * PCE Core (개인소비지출 물가지수 - 핵심)
     * - 주기: Monthly
     * - 단위: Index 2017=100, Seasonally Adjusted
     * - 식품 및 에너지 제외
     */
    PCE_CORE(
        seriesId = FredSeriesIds.Inflation.CORE_PCE,
        description = "PCE Core (excluding Food & Energy)"
    ),

    /**
     * PPI (생산자물가지수)
     * - 주기: Monthly
     * - 단위: Index 1982=100, Seasonally Adjusted
     */
    PPI(
        seriesId = FredSeriesIds.Inflation.PPI,
        description = "Producer Price Index"
    );
}

/**
 * 실업률 및 고용 지표 타입
 *
 * 고용 관련 다양한 지표를 enum으로 정의합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val rate = ufc.macro.getUnemployment(type = UnemploymentType.RATE)
 * val claims = ufc.macro.getUnemployment(type = UnemploymentType.INITIAL_CLAIMS)
 * ```
 */
enum class UnemploymentType(
    val seriesId: String,
    val description: String
) {
    /**
     * Unemployment Rate (실업률)
     * - 주기: Monthly
     * - 단위: Percent, Seasonally Adjusted
     */
    RATE(
        seriesId = FredSeriesIds.Unemployment.RATE,
        description = "Unemployment Rate"
    ),

    /**
     * Initial Jobless Claims (신규 실업수당 청구 건수)
     * - 주기: Weekly
     * - 단위: Thousands of Persons, Seasonally Adjusted
     */
    INITIAL_CLAIMS(
        seriesId = FredSeriesIds.Unemployment.INITIAL_CLAIMS,
        description = "Initial Jobless Claims"
    ),

    /**
     * Continuing Jobless Claims (계속 실업수당 청구 건수)
     * - 주기: Weekly
     * - 단위: Thousands of Persons, Seasonally Adjusted
     */
    CONTINUING_CLAIMS(
        seriesId = FredSeriesIds.Unemployment.CONTINUING_CLAIMS,
        description = "Continuing Jobless Claims"
    ),

    /**
     * Nonfarm Payrolls (비농업 고용)
     * - 주기: Monthly
     * - 단위: Thousands of Persons, Seasonally Adjusted
     */
    NONFARM_PAYROLLS(
        seriesId = FredSeriesIds.Unemployment.NONFARM_PAYROLL,
        description = "Nonfarm Payrolls"
    );
}

/**
 * 금리 지표 타입
 *
 * 다양한 금리 지표를 enum으로 정의합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val fedRate = ufc.macro.getInterestRate(type = InterestRateType.FEDERAL_FUNDS)
 * val treasury = ufc.macro.getInterestRate(type = InterestRateType.TREASURY_10Y)
 * ```
 */
enum class InterestRateType(
    val seriesId: String,
    val description: String
) {
    /**
     * Federal Funds Rate (연방기금금리)
     * - 주기: Daily
     * - 단위: Percent
     */
    FEDERAL_FUNDS(
        seriesId = FredSeriesIds.InterestRates.FED_FUNDS_RATE,
        description = "Federal Funds Rate"
    ),

    /**
     * 10-Year Treasury Constant Maturity Rate (10년물 국채 수익률)
     * - 주기: Daily
     * - 단위: Percent
     */
    TREASURY_10Y(
        seriesId = FredSeriesIds.InterestRates.TREASURY_10Y,
        description = "10-Year Treasury Yield"
    ),

    /**
     * 2-Year Treasury Constant Maturity Rate (2년물 국채 수익률)
     * - 주기: Daily
     * - 단위: Percent
     */
    TREASURY_2Y(
        seriesId = FredSeriesIds.InterestRates.TREASURY_2Y,
        description = "2-Year Treasury Yield"
    ),

    /**
     * 5-Year Treasury Constant Maturity Rate (5년물 국채 수익률)
     * - 주기: Daily
     * - 단위: Percent
     */
    TREASURY_5Y(
        seriesId = FredSeriesIds.InterestRates.TREASURY_5Y,
        description = "5-Year Treasury Yield"
    ),

    /**
     * 30-Year Treasury Constant Maturity Rate (30년물 국채 수익률)
     * - 주기: Daily
     * - 단위: Percent
     */
    TREASURY_30Y(
        seriesId = FredSeriesIds.InterestRates.TREASURY_30Y,
        description = "30-Year Treasury Yield"
    ),

    /**
     * 3-Month Treasury Bill Rate (3개월물 국채 수익률)
     * - 주기: Daily
     * - 단위: Percent
     */
    TREASURY_3M(
        seriesId = FredSeriesIds.InterestRates.TREASURY_3M,
        description = "3-Month Treasury Bill Rate"
    ),

    /**
     * 30-Year Fixed Rate Mortgage Average (30년 고정 모기지 금리)
     * - 주기: Weekly
     * - 단위: Percent
     */
    MORTGAGE_30Y(
        seriesId = FredSeriesIds.InterestRates.MORTGAGE_30Y,
        description = "30-Year Fixed Rate Mortgage"
    );
}
