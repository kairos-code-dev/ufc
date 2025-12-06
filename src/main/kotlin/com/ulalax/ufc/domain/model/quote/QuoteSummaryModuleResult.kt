package com.ulalax.ufc.domain.model.quote

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
    val modules: Map<QuoteSummaryModule, Any?>,
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
    fun hasModule(module: QuoteSummaryModule): Boolean = modules.containsKey(module) && modules[module] != null

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
    fun hasAllRequestedModules(): Boolean = requestedModules.all { hasModule(it) }

    /**
     * 응답에 포함된 모듈 목록을 반환합니다.
     *
     * @return 실제로 데이터가 존재하는 모듈의 Set
     */
    fun getAvailableModules(): Set<QuoteSummaryModule> = modules.filterValues { it != null }.keys

    /**
     * 요청했지만 응답에 포함되지 않은 모듈 목록을 반환합니다.
     *
     * 이 함수는 디버깅이나 로깅 목적으로 유용합니다.
     *
     * @return 요청했으나 데이터가 없는 모듈의 Set
     */
    fun getMissingModules(): Set<QuoteSummaryModule> = requestedModules.filterNot { hasModule(it) }.toSet()
}
