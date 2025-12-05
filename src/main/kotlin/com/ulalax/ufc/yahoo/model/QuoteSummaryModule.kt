package com.ulalax.ufc.yahoo.model

/**
 * Yahoo Finance QuoteSummary API에서 지원하는 모듈
 *
 * QuoteSummary API는 다양한 모듈을 통해 주식, ETF, 뮤추얼 펀드 등의
 * 상세 정보를 제공합니다. 각 모듈은 특정 카테고리의 데이터를 나타냅니다.
 *
 * 사용 예시:
 * ```kotlin
 * val modules = setOf(
 *     QuoteSummaryModule.PRICE,
 *     QuoteSummaryModule.SUMMARY_DETAIL,
 *     QuoteSummaryModule.FINANCIAL_DATA
 * )
 * val result = quoteSummaryService.getQuoteSummary("AAPL", modules)
 * ```
 *
 * @property apiValue Yahoo Finance API에서 사용하는 모듈 식별자
 */
enum class QuoteSummaryModule(val apiValue: String) {
    /**
     * 기본 가격 정보
     *
     * 현재가, 거래소, 심볼, 52주 최고/최저가 등 기본적인 가격 데이터를 포함합니다.
     */
    PRICE("price"),

    /**
     * 상세 요약 정보
     *
     * 배당금, PER, 베타, 거래량, 시가총액 등 주요 통계 데이터를 포함합니다.
     */
    SUMMARY_DETAIL("summaryDetail"),

    /**
     * 자산 프로필 정보
     *
     * 회사 소개, 섹터, 산업, 웹사이트, 주소, 직원 수 등 기업 프로필 정보를 포함합니다.
     */
    ASSET_PROFILE("assetProfile"),

    /**
     * 요약 프로필 정보
     *
     * assetProfile과 유사하나 더 간략한 형태의 프로필 정보를 포함합니다.
     */
    SUMMARY_PROFILE("summaryProfile"),

    /**
     * 자산 타입 정보
     *
     * EQUITY, ETF, MUTUALFUND, INDEX, CRYPTOCURRENCY 등 자산의 타입 정보를 포함합니다.
     */
    QUOTE_TYPE("quoteType"),

    /**
     * 기본 주요 통계
     *
     * 발행주식수, ISIN, CUSIP 등 주요 통계 데이터를 포함합니다.
     */
    DEFAULT_KEY_STATISTICS("defaultKeyStatistics"),

    /**
     * 재무 데이터
     *
     * 현금 흐름, 부채, ROE, ROA, PEG 비율, 목표 가격 등 재무 정보를 포함합니다.
     */
    FINANCIAL_DATA("financialData"),

    /**
     * 캘린더 이벤트
     *
     * 배당금 날짜, 실적 발표 날짜 등 주요 이벤트 정보를 포함합니다.
     */
    CALENDAR_EVENTS("calendarEvents"),

    /**
     * 수익 추이
     *
     * 분기별 실적 추정치와 실제 수익 비교 데이터를 포함합니다.
     */
    EARNINGS_TREND("earningsTrend"),

    /**
     * 수익 이력
     *
     * 과거 실적 발표 이력 데이터를 포함합니다.
     */
    EARNINGS_HISTORY("earningsHistory"),

    /**
     * 실적 발표 날짜
     *
     * 다음 실적 발표 예정일 정보를 포함합니다.
     */
    EARNINGS_DATES("earningsDates"),

    /**
     * 주요 주주
     *
     * 기관 투자자 및 주요 주주 정보를 포함합니다.
     */
    MAJOR_HOLDERS("majorHolders"),

    /**
     * 내부자 거래
     *
     * 임원 및 내부자의 주식 매수/매도 내역을 포함합니다.
     */
    INSIDER_TRANSACTIONS("insiderTransactions"),

    /**
     * 내부자 보유 정보
     *
     * 내부자 및 기관의 보유 지분 상세 정보를 포함합니다.
     */
    INSIDER_HOLDERS("insiderHolders"),

    /**
     * 기관 투자자 보유 정보
     *
     * 뮤추얼 펀드 등 기관 투자자의 보유 내역을 포함합니다.
     */
    INSTITUTION_OWNERSHIP("institutionOwnership"),

    /**
     * 펀드 보유 정보
     *
     * 뮤추얼 펀드의 보유 지분 정보를 포함합니다.
     */
    FUND_OWNERSHIP("fundOwnership"),

    /**
     * 추천 동향
     *
     * 애널리스트의 매수/매도/보유 추천 정보를 포함합니다.
     */
    RECOMMENDATION_TREND("recommendationTrend"),

    /**
     * 업그레이드/다운그레이드 이력
     *
     * 애널리스트의 등급 변경 이력을 포함합니다.
     */
    UPGRADE_DOWNGRADE_HISTORY("upgradeDowngradeHistory"),

    /**
     * 재무제표 - 손익계산서
     *
     * 연간 및 분기별 손익계산서 데이터를 포함합니다.
     */
    INCOME_STATEMENT_HISTORY("incomeStatementHistory"),

    /**
     * 재무제표 - 분기별 손익계산서
     *
     * 분기별 손익계산서 데이터를 포함합니다.
     */
    INCOME_STATEMENT_HISTORY_QUARTERLY("incomeStatementHistoryQuarterly"),

    /**
     * 재무제표 - 대차대조표
     *
     * 연간 대차대조표 데이터를 포함합니다.
     */
    BALANCE_SHEET_HISTORY("balanceSheetHistory"),

    /**
     * 재무제표 - 분기별 대차대조표
     *
     * 분기별 대차대조표 데이터를 포함합니다.
     */
    BALANCE_SHEET_HISTORY_QUARTERLY("balanceSheetHistoryQuarterly"),

    /**
     * 재무제표 - 현금흐름표
     *
     * 연간 현금흐름표 데이터를 포함합니다.
     */
    CASHFLOW_STATEMENT_HISTORY("cashflowStatementHistory"),

    /**
     * 재무제표 - 분기별 현금흐름표
     *
     * 분기별 현금흐름표 데이터를 포함합니다.
     */
    CASHFLOW_STATEMENT_HISTORY_QUARTERLY("cashflowStatementHistoryQuarterly"),

    /**
     * 펀드의 상위 보유 종목
     *
     * ETF 및 뮤추얼 펀드의 주요 보유 종목 정보를 포함합니다.
     */
    TOP_HOLDINGS("topHoldings"),

    /**
     * 펀드 프로필
     *
     * 펀드의 카테고리, 가족, 수수료 등 펀드 관련 정보를 포함합니다.
     */
    FUND_PROFILE("fundProfile"),

    /**
     * 펀드 성과
     *
     * 펀드의 수익률 및 성과 지표를 포함합니다.
     */
    FUND_PERFORMANCE("fundPerformance"),

    /**
     * 주식 분할 및 배당금 이력
     *
     * 주식 분할 및 배당금 지급 이력을 포함합니다.
     */
    SEC_FILINGS("secFilings"),

    /**
     * 가격 이력
     *
     * 과거 가격 데이터를 포함합니다.
     */
    PRICE_HISTORY("priceHistory"),

    /**
     * 색인 추세
     *
     * 시장 지수 관련 추세 정보를 포함합니다.
     */
    INDEX_TREND("indexTrend"),

    /**
     * 산업 추세
     *
     * 업종별 추세 정보를 포함합니다.
     */
    INDUSTRY_TREND("industryTrend"),

    /**
     * 섹터 추세
     *
     * 섹터별 추세 정보를 포함합니다.
     */
    SECTOR_TREND("sectorTrend"),

    /**
     * 실적 연혁
     *
     * 과거 실적 발표 및 EPS 이력을 포함합니다.
     */
    EARNINGS("earnings"),

    /**
     * 페이지 뷰 정보
     *
     * 심볼에 대한 페이지 조회 통계를 포함합니다.
     */
    PAGE_VIEWS("pageViews"),

    /**
     * ESG 점수
     *
     * 환경(E), 사회(S), 거버넌스(G) 관련 평가 점수를 포함합니다.
     */
    ESG_SCORES("esgScores"),

    /**
     * 순자산 가치
     *
     * ETF 및 뮤추얼 펀드의 NAV 정보를 포함합니다.
     */
    NET_SHARE_PURCHASE_ACTIVITY("netSharePurchaseActivity");

    companion object {
        /**
         * API 값으로부터 QuoteSummaryModule을 찾습니다.
         *
         * @param apiValue Yahoo Finance API 모듈 식별자
         * @return 해당하는 QuoteSummaryModule 또는 null
         */
        fun fromApiValue(apiValue: String): QuoteSummaryModule? {
            return entries.find { it.apiValue == apiValue }
        }

        /**
         * 모든 모듈의 Set을 반환합니다.
         *
         * @return 모든 QuoteSummaryModule의 Set
         */
        fun allModules(): Set<QuoteSummaryModule> {
            return entries.toSet()
        }

        /**
         * 주식(EQUITY)에 일반적으로 사용되는 모듈 Set을 반환합니다.
         *
         * @return 주식 관련 주요 모듈의 Set
         */
        fun stockModules(): Set<QuoteSummaryModule> {
            return setOf(
                PRICE,
                SUMMARY_DETAIL,
                ASSET_PROFILE,
                QUOTE_TYPE,
                DEFAULT_KEY_STATISTICS,
                FINANCIAL_DATA,
                EARNINGS_TREND,
                EARNINGS_HISTORY,
                RECOMMENDATION_TREND,
                MAJOR_HOLDERS,
                INSIDER_TRANSACTIONS
            )
        }

        /**
         * ETF 및 펀드에 일반적으로 사용되는 모듈 Set을 반환합니다.
         *
         * @return 펀드 관련 주요 모듈의 Set
         */
        fun fundModules(): Set<QuoteSummaryModule> {
            return setOf(
                PRICE,
                SUMMARY_DETAIL,
                QUOTE_TYPE,
                TOP_HOLDINGS,
                FUND_PROFILE,
                FUND_PERFORMANCE
            )
        }
    }
}

/**
 * QuoteSummaryModule을 API 파라미터 문자열로 변환합니다.
 *
 * @return API 파라미터 값
 */
fun QuoteSummaryModule.toApiValue(): String = apiValue

/**
 * QuoteSummaryModule Set을 쉼표로 구분된 API 파라미터 문자열로 변환합니다.
 *
 * @return 쉼표로 구분된 모듈 문자열
 */
fun Set<QuoteSummaryModule>.toApiValue(): String {
    return joinToString(",") { it.apiValue }
}
