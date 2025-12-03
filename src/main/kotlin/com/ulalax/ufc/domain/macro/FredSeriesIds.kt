package com.ulalax.ufc.domain.macro

/**
 * FRED 주요 경제 지표 Series ID 카탈로그
 *
 * FRED (Federal Reserve Economic Data)에서 제공하는 주요 거시경제 지표의 Series ID를 정의합니다.
 * 총 7개 카테고리, 50개 이상의 주요 지표를 포함합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val gdpSeriesId = FredSeriesIds.GDP.REAL_GDP  // "GDPC1"
 * val unemploymentSeriesId = FredSeriesIds.Unemployment.RATE  // "UNRATE"
 * ```
 *
 * FRED 공식 사이트: https://fred.stlouisfed.org/
 */
object FredSeriesIds {

    /**
     * GDP 및 경제 성장 지표
     */
    object GDP {
        /**
         * Nominal GDP (명목 GDP)
         * - 주기: Quarterly
         * - 단위: Billions of Dollars
         */
        const val NOMINAL_GDP = "GDP"

        /**
         * Real GDP (실질 GDP)
         * - 주기: Quarterly
         * - 단위: Billions of Chained 2017 Dollars
         */
        const val REAL_GDP = "GDPC1"

        /**
         * GDP Growth Rate (GDP 성장률)
         * - 주기: Quarterly
         * - 단위: Percent Change from Preceding Period, Seasonally Adjusted Annual Rate
         */
        const val GDP_GROWTH = "A191RL1Q225SBEA"

        /**
         * Potential GDP (잠재 GDP)
         * - 주기: Quarterly
         * - 단위: Billions of Chained 2017 Dollars
         */
        const val POTENTIAL_GDP = "GDPPOT"

        /**
         * GDP Deflator (GDP 디플레이터)
         * - 주기: Quarterly
         * - 단위: Index 2017=100
         */
        const val GDP_DEFLATOR = "GDPDEF"

        /**
         * Industrial Production Index (산업생산지수)
         * - 주기: Monthly
         * - 단위: Index 2017=100
         */
        const val INDUSTRIAL_PRODUCTION = "INDPRO"
    }

    /**
     * 실업률 및 고용 지표
     */
    object Unemployment {
        /**
         * Unemployment Rate (실업률)
         * - 주기: Monthly
         * - 단위: Percent, Seasonally Adjusted
         */
        const val RATE = "UNRATE"

        /**
         * Initial Jobless Claims (신규 실업수당 청구 건수)
         * - 주기: Weekly
         * - 단위: Thousands of Persons, Seasonally Adjusted
         */
        const val INITIAL_CLAIMS = "ICSA"

        /**
         * Continuing Jobless Claims (계속 실업수당 청구 건수)
         * - 주기: Weekly
         * - 단위: Thousands of Persons, Seasonally Adjusted
         */
        const val CONTINUING_CLAIMS = "CCSA"

        /**
         * Nonfarm Payrolls (비농업 고용)
         * - 주기: Monthly
         * - 단위: Thousands of Persons, Seasonally Adjusted
         */
        const val NONFARM_PAYROLL = "PAYEMS"

        /**
         * Labor Force Participation Rate (경제활동참가율)
         * - 주기: Monthly
         * - 단위: Percent, Seasonally Adjusted
         */
        const val LABOR_FORCE_PARTICIPATION = "CIVPART"

        /**
         * Average Hourly Earnings (평균 시간당 임금)
         * - 주기: Monthly
         * - 단위: Dollars per Hour, Seasonally Adjusted
         */
        const val AVERAGE_HOURLY_EARNINGS = "CES0500000003"
    }

    /**
     * 인플레이션 지표
     */
    object Inflation {
        /**
         * CPI All Items (소비자물가지수 - 전체)
         * - 주기: Monthly
         * - 단위: Index 1982-84=100, Seasonally Adjusted
         */
        const val CPI = "CPIAUCSL"

        /**
         * CPI Core (소비자물가지수 - 핵심)
         * - 주기: Monthly
         * - 단위: Index 1982-84=100, Seasonally Adjusted
         * - 식품 및 에너지 제외
         */
        const val CORE_CPI = "CPILFESL"

        /**
         * PCE (개인소비지출 물가지수)
         * - 주기: Monthly
         * - 단위: Index 2017=100, Seasonally Adjusted
         */
        const val PCE = "PCEPI"

        /**
         * PCE Core (개인소비지출 물가지수 - 핵심)
         * - 주기: Monthly
         * - 단위: Index 2017=100, Seasonally Adjusted
         * - 식품 및 에너지 제외
         */
        const val CORE_PCE = "PCEPILFE"

        /**
         * PPI (생산자물가지수)
         * - 주기: Monthly
         * - 단위: Index 1982=100, Seasonally Adjusted
         */
        const val PPI = "PPIACO"

        /**
         * CPI YoY (소비자물가지수 전년 대비 변화율)
         * - 주기: Monthly
         * - 단위: Percent Change from Year Ago
         */
        const val CPI_YOY = "CPIAUCSL"  // units=pc1로 조회
    }

    /**
     * 금리 지표
     */
    object InterestRates {
        /**
         * Federal Funds Rate (연방기금금리)
         * - 주기: Daily
         * - 단위: Percent
         */
        const val FED_FUNDS_RATE = "FEDFUNDS"

        /**
         * 10-Year Treasury Constant Maturity Rate (10년물 국채 수익률)
         * - 주기: Daily
         * - 단위: Percent
         */
        const val TREASURY_10Y = "DGS10"

        /**
         * 2-Year Treasury Constant Maturity Rate (2년물 국채 수익률)
         * - 주기: Daily
         * - 단위: Percent
         */
        const val TREASURY_2Y = "DGS2"

        /**
         * 5-Year Treasury Constant Maturity Rate (5년물 국채 수익률)
         * - 주기: Daily
         * - 단위: Percent
         */
        const val TREASURY_5Y = "DGS5"

        /**
         * 30-Year Treasury Constant Maturity Rate (30년물 국채 수익률)
         * - 주기: Daily
         * - 단위: Percent
         */
        const val TREASURY_30Y = "DGS30"

        /**
         * 3-Month Treasury Bill Rate (3개월물 국채 수익률)
         * - 주기: Daily
         * - 단위: Percent
         */
        const val TREASURY_3M = "DGS3MO"

        /**
         * Prime Loan Rate (우대 대출 금리)
         * - 주기: Daily
         * - 단위: Percent
         */
        const val PRIME_RATE = "DPRIME"

        /**
         * 30-Year Fixed Rate Mortgage Average (30년 고정 모기지 금리)
         * - 주기: Weekly
         * - 단위: Percent
         */
        const val MORTGAGE_30Y = "MORTGAGE30US"
    }

    /**
     * 주택 시장 지표
     */
    object Housing {
        /**
         * S&P/Case-Shiller U.S. National Home Price Index (케이스-쉴러 주택가격지수)
         * - 주기: Monthly
         * - 단위: Index Jan 2000=100, Not Seasonally Adjusted
         */
        const val CASE_SHILLER = "CSUSHPINSA"

        /**
         * Housing Starts (주택 착공)
         * - 주기: Monthly
         * - 단위: Thousands of Units, Seasonally Adjusted Annual Rate
         */
        const val HOUSING_STARTS = "HOUST"

        /**
         * New One Family Houses Sold (신규 단독주택 판매)
         * - 주기: Monthly
         * - 단위: Thousands of Units, Seasonally Adjusted Annual Rate
         */
        const val NEW_HOME_SALES = "HSN1F"

        /**
         * Existing Home Sales (기존 주택 판매)
         * - 주기: Monthly
         * - 단위: Millions of Units, Seasonally Adjusted Annual Rate
         */
        const val EXISTING_HOME_SALES = "EXHOSLUSM495S"

        /**
         * Homeownership Rate (자가 주택 보유율)
         * - 주기: Quarterly
         * - 단위: Percent, Seasonally Adjusted
         */
        const val HOMEOWNERSHIP_RATE = "RHORUSQ156N"
    }

    /**
     * 소비자 신뢰도 및 경기지표
     */
    object ConsumerSentiment {
        /**
         * University of Michigan Consumer Sentiment Index (미시간 소비자신뢰지수)
         * - 주기: Monthly
         * - 단위: Index 1966:Q1=100, Not Seasonally Adjusted
         */
        const val MICHIGAN_SENTIMENT = "UMCSENT"

        /**
         * Consumer Confidence Index (소비자신뢰지수)
         * - 주기: Monthly
         * - 단위: Index 1985=100, Seasonally Adjusted
         */
        const val CONSUMER_CONFIDENCE = "CSCICP03USM665S"

        /**
         * ISM Manufacturing PMI (제조업 구매관리자지수)
         * - 주기: Monthly
         * - 단위: Index, Seasonally Adjusted
         */
        const val ISM_MANUFACTURING = "MANEMP"

        /**
         * Retail Sales (소매 판매)
         * - 주기: Monthly
         * - 단위: Millions of Dollars, Seasonally Adjusted
         */
        const val RETAIL_SALES = "RSXFS"

        /**
         * Personal Saving Rate (개인저축률)
         * - 주기: Monthly
         * - 단위: Percent, Seasonally Adjusted Annual Rate
         */
        const val PERSONAL_SAVING_RATE = "PSAVERT"
    }

    /**
     * 통화 및 금융 지표
     */
    object MoneyAndFinance {
        /**
         * M1 Money Stock (M1 통화량)
         * - 주기: Monthly
         * - 단위: Billions of Dollars, Seasonally Adjusted
         */
        const val M1 = "M1SL"

        /**
         * M2 Money Stock (M2 통화량)
         * - 주기: Monthly
         * - 단위: Billions of Dollars, Seasonally Adjusted
         */
        const val M2 = "M2SL"

        /**
         * CBOE Volatility Index: VIX (변동성 지수)
         * - 주기: Daily
         * - 단위: Index
         */
        const val VIX = "VIXCLS"

        /**
         * Trade Weighted U.S. Dollar Index (무역가중 달러지수)
         * - 주기: Daily
         * - 단위: Index Jan 2006=100
         */
        const val DOLLAR_INDEX = "DTWEXBGS"

        /**
         * Total Public Debt (총 공공 부채)
         * - 주기: Quarterly
         * - 단위: Millions of Dollars
         */
        const val PUBLIC_DEBT = "GFDEBTN"

        /**
         * 10-Year Breakeven Inflation Rate (10년 손익분기 인플레이션율)
         * - 주기: Daily
         * - 단위: Percent
         */
        const val BREAKEVEN_INFLATION_10Y = "T10YIE"
    }
}
