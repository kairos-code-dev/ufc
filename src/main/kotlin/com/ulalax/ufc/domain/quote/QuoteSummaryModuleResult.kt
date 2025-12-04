package com.ulalax.ufc.domain.quote

/**
 * QuoteSummary API 요청 결과를 모듈 기반으로 관리하는 데이터 클래스
 *
 * 이 클래스는 EODHD의 FilteredStockFundamentals 패턴을 따르며,
 * 사용자가 요청한 모듈과 실제로 반환된 데이터를 타입 안전하게 관리합니다.
 *
 * 각 모듈의 데이터는 Any 타입으로 저장되며,
 * getModule() 함수를 통해 구체적인 타입으로 변환할 수 있습니다.
 *
 * 사용 예시:
 * ```kotlin
 * val result = quoteSummaryService.getQuoteSummary(
 *     symbol = "AAPL",
 *     modules = setOf(
 *         QuoteSummaryModule.PRICE,
 *         QuoteSummaryModule.FINANCIAL_DATA
 *     )
 * )
 *
 * // 모듈 존재 여부 확인
 * if (result.hasModule(QuoteSummaryModule.PRICE)) {
 *     // 타입 안전한 모듈 데이터 가져오기
 *     val price: Price? = result.getModule(QuoteSummaryModule.PRICE)
 *     println("Current price: ${price?.regularMarketPrice?.doubleValue}")
 * }
 *
 * // 재무 데이터 가져오기
 * val financialData: FinancialData? = result.getModule(QuoteSummaryModule.FINANCIAL_DATA)
 * println("ROE: ${financialData?.returnOnEquity?.doubleValue}")
 * ```
 *
 * @property requestedModules 사용자가 요청한 모듈 목록
 * @property modules 실제로 반환된 모듈별 데이터 (모듈 → 데이터)
 */
data class QuoteSummaryModuleResult(
    val requestedModules: Set<QuoteSummaryModule>,
    val modules: Map<QuoteSummaryModule, Any?>
) {
    /**
     * 특정 모듈이 응답에 포함되어 있는지 확인합니다.
     *
     * 모듈이 요청되었더라도 Yahoo Finance API에서 데이터를 제공하지 않을 수 있습니다.
     * 이 함수는 실제로 데이터가 존재하는지 확인합니다.
     *
     * @param module 확인할 모듈
     * @return 모듈 데이터가 존재하면 true, 그렇지 않으면 false
     */
    fun hasModule(module: QuoteSummaryModule): Boolean {
        return modules.containsKey(module) && modules[module] != null
    }

    /**
     * 특정 모듈의 데이터를 타입 안전하게 가져옵니다.
     *
     * reified 타입 파라미터를 사용하여 자동으로 타입 캐스팅을 수행합니다.
     * 타입이 일치하지 않거나 모듈이 존재하지 않으면 null을 반환합니다.
     *
     * 사용 예시:
     * ```kotlin
     * val price: Price? = result.getModule(QuoteSummaryModule.PRICE)
     * val financialData: FinancialData? = result.getModule(QuoteSummaryModule.FINANCIAL_DATA)
     * ```
     *
     * @param T 반환받을 데이터의 타입
     * @param module 가져올 모듈
     * @return 해당 모듈의 데이터 또는 null (타입 불일치나 데이터 부재 시)
     */
    inline fun <reified T> getModule(module: QuoteSummaryModule): T? {
        val data = modules[module] ?: return null
        return data as? T
    }

    /**
     * 모든 요청된 모듈이 응답에 포함되어 있는지 확인합니다.
     *
     * @return 모든 요청 모듈의 데이터가 존재하면 true, 그렇지 않으면 false
     */
    fun hasAllRequestedModules(): Boolean {
        return requestedModules.all { hasModule(it) }
    }

    /**
     * 응답에 포함된 모듈 목록을 반환합니다.
     *
     * @return 실제로 데이터가 존재하는 모듈의 Set
     */
    fun getAvailableModules(): Set<QuoteSummaryModule> {
        return modules.filterValues { it != null }.keys
    }

    /**
     * 요청했지만 응답에 포함되지 않은 모듈 목록을 반환합니다.
     *
     * 이 함수는 디버깅이나 로깅 목적으로 유용합니다.
     *
     * @return 요청했으나 데이터가 없는 모듈의 Set
     */
    fun getMissingModules(): Set<QuoteSummaryModule> {
        return requestedModules.filterNot { hasModule(it) }.toSet()
    }
}

/**
 * QuoteSummaryResponse를 QuoteSummaryModuleResult로 변환하는 확장 함수
 *
 * API 응답 객체를 모듈 기반 결과 객체로 변환합니다.
 * QuoteSummaryResult의 각 필드를 해당하는 QuoteSummaryModule과 매핑합니다.
 *
 * @param requestedModules 사용자가 요청한 모듈 목록
 * @return 변환된 QuoteSummaryModuleResult
 */
fun QuoteSummaryResult.toModuleResult(
    requestedModules: Set<QuoteSummaryModule>
): QuoteSummaryModuleResult {
    val moduleMap = mutableMapOf<QuoteSummaryModule, Any?>()

    // 각 모듈별 데이터 매핑
    price?.let { moduleMap[QuoteSummaryModule.PRICE] = it }
    summaryDetail?.let { moduleMap[QuoteSummaryModule.SUMMARY_DETAIL] = it }
    assetProfile?.let { moduleMap[QuoteSummaryModule.ASSET_PROFILE] = it }
    summaryProfile?.let { moduleMap[QuoteSummaryModule.SUMMARY_PROFILE] = it }
    quoteType?.let { moduleMap[QuoteSummaryModule.QUOTE_TYPE] = it }
    defaultKeyStatistics?.let { moduleMap[QuoteSummaryModule.DEFAULT_KEY_STATISTICS] = it }
    financialData?.let { moduleMap[QuoteSummaryModule.FINANCIAL_DATA] = it }
    earningsTrend?.let { moduleMap[QuoteSummaryModule.EARNINGS_TREND] = it }
    earningsHistory?.let { moduleMap[QuoteSummaryModule.EARNINGS_HISTORY] = it }
    earningsDates?.let { moduleMap[QuoteSummaryModule.EARNINGS_DATES] = it }
    majorHolders?.let { moduleMap[QuoteSummaryModule.MAJOR_HOLDERS] = it }
    insiderTransactions?.let { moduleMap[QuoteSummaryModule.INSIDER_TRANSACTIONS] = it }
    topHoldings?.let { moduleMap[QuoteSummaryModule.TOP_HOLDINGS] = it }
    fundProfile?.let { moduleMap[QuoteSummaryModule.FUND_PROFILE] = it }

    return QuoteSummaryModuleResult(
        requestedModules = requestedModules,
        modules = moduleMap
    )
}

/**
 * QuoteSummaryModuleResult를 QuoteSummaryResult로 변환하는 확장 함수
 *
 * 모듈 기반 결과를 원본 API 응답 형식으로 변환합니다.
 * 모듈이 없는 경우 해당 필드는 null로 설정됩니다.
 *
 * @return 변환된 QuoteSummaryResult
 */
fun QuoteSummaryModuleResult.toQuoteSummaryResult(): QuoteSummaryResult {
    return QuoteSummaryResult(
        price = getModule(QuoteSummaryModule.PRICE),
        summaryDetail = getModule(QuoteSummaryModule.SUMMARY_DETAIL),
        assetProfile = getModule(QuoteSummaryModule.ASSET_PROFILE),
        summaryProfile = getModule(QuoteSummaryModule.SUMMARY_PROFILE),
        quoteType = getModule(QuoteSummaryModule.QUOTE_TYPE),
        defaultKeyStatistics = getModule(QuoteSummaryModule.DEFAULT_KEY_STATISTICS),
        financialData = getModule(QuoteSummaryModule.FINANCIAL_DATA),
        earningsTrend = getModule(QuoteSummaryModule.EARNINGS_TREND),
        earningsHistory = getModule(QuoteSummaryModule.EARNINGS_HISTORY),
        earningsDates = getModule(QuoteSummaryModule.EARNINGS_DATES),
        majorHolders = getModule(QuoteSummaryModule.MAJOR_HOLDERS),
        insiderTransactions = getModule(QuoteSummaryModule.INSIDER_TRANSACTIONS),
        topHoldings = getModule(QuoteSummaryModule.TOP_HOLDINGS),
        fundProfile = getModule(QuoteSummaryModule.FUND_PROFILE)
    )
}
