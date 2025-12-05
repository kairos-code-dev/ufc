package com.ulalax.ufc.domain.model.fundamentals

/**
 * Yahoo Finance Fundamentals Timeseries API에서 조회 가능한 재무 항목 타입
 *
 * 각 enum 값은 Yahoo Finance API의 type 파라미터 값과 매핑됩니다.
 * {빈도}{항목명} 형식으로 구성됩니다 (예: annualTotalRevenue, quarterlyNetIncome)
 *
 * ## 사용 예시
 * ```kotlin
 * val types = listOf(
 *     FundamentalsType.ANNUAL_TOTAL_REVENUE,
 *     FundamentalsType.QUARTERLY_NET_INCOME,
 *     FundamentalsType.TRAILING_EPS
 * )
 * val result = yahooClient.fundamentalsTimeseries("AAPL", types)
 * ```
 *
 * @property apiValue Yahoo Finance API에서 사용하는 실제 파라미터 값
 */
enum class FundamentalsType(val apiValue: String) {
    // ===== Income Statement - Annual =====
    ANNUAL_TOTAL_REVENUE("annualTotalRevenue"),
    ANNUAL_COST_OF_REVENUE("annualCostOfRevenue"),
    ANNUAL_GROSS_PROFIT("annualGrossProfit"),
    ANNUAL_OPERATING_INCOME("annualOperatingIncome"),
    ANNUAL_EBIT("annualEBIT"),
    ANNUAL_EBITDA("annualEBITDA"),
    ANNUAL_NET_INCOME("annualNetIncome"),
    ANNUAL_NET_INCOME_COMMON_STOCKHOLDERS("annualNetIncomeCommonStockholders"),
    ANNUAL_BASIC_EPS("annualBasicEPS"),
    ANNUAL_DILUTED_EPS("annualDilutedEPS"),
    ANNUAL_BASIC_AVERAGE_SHARES("annualBasicAverageShares"),
    ANNUAL_DILUTED_AVERAGE_SHARES("annualDilutedAverageShares"),
    ANNUAL_RESEARCH_AND_DEVELOPMENT("annualResearchAndDevelopment"),
    ANNUAL_SELLING_GENERAL_AND_ADMINISTRATION("annualSellingGeneralAndAdministration"),

    // ===== Income Statement - Quarterly =====
    QUARTERLY_TOTAL_REVENUE("quarterlyTotalRevenue"),
    QUARTERLY_COST_OF_REVENUE("quarterlyCostOfRevenue"),
    QUARTERLY_GROSS_PROFIT("quarterlyGrossProfit"),
    QUARTERLY_OPERATING_INCOME("quarterlyOperatingIncome"),
    QUARTERLY_EBIT("quarterlyEBIT"),
    QUARTERLY_EBITDA("quarterlyEBITDA"),
    QUARTERLY_NET_INCOME("quarterlyNetIncome"),
    QUARTERLY_NET_INCOME_COMMON_STOCKHOLDERS("quarterlyNetIncomeCommonStockholders"),
    QUARTERLY_BASIC_EPS("quarterlyBasicEPS"),
    QUARTERLY_DILUTED_EPS("quarterlyDilutedEPS"),
    QUARTERLY_BASIC_AVERAGE_SHARES("quarterlyBasicAverageShares"),
    QUARTERLY_DILUTED_AVERAGE_SHARES("quarterlyDilutedAverageShares"),
    QUARTERLY_RESEARCH_AND_DEVELOPMENT("quarterlyResearchAndDevelopment"),
    QUARTERLY_SELLING_GENERAL_AND_ADMINISTRATION("quarterlySellingGeneralAndAdministration"),

    // ===== Income Statement - Trailing =====
    TRAILING_TOTAL_REVENUE("trailingTotalRevenue"),
    TRAILING_COST_OF_REVENUE("trailingCostOfRevenue"),
    TRAILING_GROSS_PROFIT("trailingGrossProfit"),
    TRAILING_OPERATING_INCOME("trailingOperatingIncome"),
    TRAILING_EBIT("trailingEBIT"),
    TRAILING_EBITDA("trailingEBITDA"),
    TRAILING_NET_INCOME("trailingNetIncome"),
    TRAILING_NET_INCOME_COMMON_STOCKHOLDERS("trailingNetIncomeCommonStockholders"),
    TRAILING_EPS("trailingEPS"),
    TRAILING_DILUTED_EPS("trailingDilutedEPS"),

    // ===== Balance Sheet - Annual =====
    ANNUAL_TOTAL_ASSETS("annualTotalAssets"),
    ANNUAL_CURRENT_ASSETS("annualCurrentAssets"),
    ANNUAL_CASH_AND_CASH_EQUIVALENTS("annualCashAndCashEquivalents"),
    ANNUAL_ACCOUNTS_RECEIVABLE("annualAccountsReceivable"),
    ANNUAL_INVENTORY("annualInventory"),
    ANNUAL_NET_PPE("annualNetPPE"),
    ANNUAL_TOTAL_LIABILITIES_NET_MINORITY_INTEREST("annualTotalLiabilitiesNetMinorityInterest"),
    ANNUAL_CURRENT_LIABILITIES("annualCurrentLiabilities"),
    ANNUAL_LONG_TERM_DEBT("annualLongTermDebt"),
    ANNUAL_STOCKHOLDERS_EQUITY("annualStockholdersEquity"),
    ANNUAL_COMMON_STOCK_EQUITY("annualCommonStockEquity"),
    ANNUAL_RETAINED_EARNINGS("annualRetainedEarnings"),
    ANNUAL_ORDINARY_SHARES_NUMBER("annualOrdinarySharesNumber"),
    ANNUAL_SHARE_ISSUED("annualShareIssued"),

    // ===== Balance Sheet - Quarterly =====
    QUARTERLY_TOTAL_ASSETS("quarterlyTotalAssets"),
    QUARTERLY_CURRENT_ASSETS("quarterlyCurrentAssets"),
    QUARTERLY_CASH_AND_CASH_EQUIVALENTS("quarterlyCashAndCashEquivalents"),
    QUARTERLY_ACCOUNTS_RECEIVABLE("quarterlyAccountsReceivable"),
    QUARTERLY_INVENTORY("quarterlyInventory"),
    QUARTERLY_NET_PPE("quarterlyNetPPE"),
    QUARTERLY_TOTAL_LIABILITIES_NET_MINORITY_INTEREST("quarterlyTotalLiabilitiesNetMinorityInterest"),
    QUARTERLY_CURRENT_LIABILITIES("quarterlyCurrentLiabilities"),
    QUARTERLY_LONG_TERM_DEBT("quarterlyLongTermDebt"),
    QUARTERLY_STOCKHOLDERS_EQUITY("quarterlyStockholdersEquity"),
    QUARTERLY_COMMON_STOCK_EQUITY("quarterlyCommonStockEquity"),
    QUARTERLY_RETAINED_EARNINGS("quarterlyRetainedEarnings"),
    QUARTERLY_ORDINARY_SHARES_NUMBER("quarterlyOrdinarySharesNumber"),
    QUARTERLY_SHARE_ISSUED("quarterlyShareIssued"),

    // ===== Cash Flow - Annual =====
    ANNUAL_OPERATING_CASH_FLOW("annualOperatingCashFlow"),
    ANNUAL_INVESTING_CASH_FLOW("annualInvestingCashFlow"),
    ANNUAL_FINANCING_CASH_FLOW("annualFinancingCashFlow"),
    ANNUAL_FREE_CASH_FLOW("annualFreeCashFlow"),
    ANNUAL_CAPITAL_EXPENDITURE("annualCapitalExpenditure"),
    ANNUAL_REPURCHASE_OF_CAPITAL_STOCK("annualRepurchaseOfCapitalStock"),
    ANNUAL_CASH_DIVIDENDS_PAID("annualCashDividendsPaid"),
    ANNUAL_END_CASH_POSITION("annualEndCashPosition"),

    // ===== Cash Flow - Quarterly =====
    QUARTERLY_OPERATING_CASH_FLOW("quarterlyOperatingCashFlow"),
    QUARTERLY_INVESTING_CASH_FLOW("quarterlyInvestingCashFlow"),
    QUARTERLY_FINANCING_CASH_FLOW("quarterlyFinancingCashFlow"),
    QUARTERLY_FREE_CASH_FLOW("quarterlyFreeCashFlow"),
    QUARTERLY_CAPITAL_EXPENDITURE("quarterlyCapitalExpenditure"),
    QUARTERLY_REPURCHASE_OF_CAPITAL_STOCK("quarterlyRepurchaseOfCapitalStock"),
    QUARTERLY_CASH_DIVIDENDS_PAID("quarterlyCashDividendsPaid"),
    QUARTERLY_END_CASH_POSITION("quarterlyEndCashPosition"),

    // ===== Cash Flow - Trailing =====
    TRAILING_OPERATING_CASH_FLOW("trailingOperatingCashFlow"),
    TRAILING_INVESTING_CASH_FLOW("trailingInvestingCashFlow"),
    TRAILING_FINANCING_CASH_FLOW("trailingFinancingCashFlow"),
    TRAILING_FREE_CASH_FLOW("trailingFreeCashFlow"),
    TRAILING_CAPITAL_EXPENDITURE("trailingCapitalExpenditure");

    companion object {
        /**
         * API 값으로부터 FundamentalsType을 찾습니다.
         *
         * @param value Yahoo Finance API 파라미터 값
         * @return 매칭되는 FundamentalsType 또는 null
         */
        fun fromApiValue(value: String): FundamentalsType? {
            return entries.find { it.apiValue == value }
        }

        /**
         * 손익계산서 항목만 필터링합니다.
         *
         * @return 손익계산서 관련 FundamentalsType 목록
         */
        fun incomeStatementTypes(): List<FundamentalsType> {
            return entries.filter {
                it.apiValue.contains("Revenue") ||
                it.apiValue.contains("Income") ||
                it.apiValue.contains("Profit") ||
                it.apiValue.contains("EPS") ||
                it.apiValue.contains("Eps") ||
                it.apiValue.contains("EBIT") ||
                it.apiValue.contains("Shares") ||
                it.apiValue.contains("Research") ||
                it.apiValue.contains("Selling")
            }
        }

        /**
         * 대차대조표 항목만 필터링합니다.
         *
         * @return 대차대조표 관련 FundamentalsType 목록
         */
        fun balanceSheetTypes(): List<FundamentalsType> {
            return entries.filter {
                it.apiValue.contains("Assets") ||
                it.apiValue.contains("Liabilities") ||
                it.apiValue.contains("Equity") ||
                it.apiValue.contains("Receivable") ||
                it.apiValue.contains("Inventory") ||
                it.apiValue.contains("PPE") ||
                it.apiValue.contains("Debt") ||
                it.apiValue.contains("Earnings") && it.apiValue.contains("Retained") ||
                it.apiValue.contains("Shares") && (it.apiValue.contains("Ordinary") || it.apiValue.contains("Issued"))
            }
        }

        /**
         * 현금흐름표 항목만 필터링합니다.
         *
         * @return 현금흐름표 관련 FundamentalsType 목록
         */
        fun cashFlowTypes(): List<FundamentalsType> {
            return entries.filter {
                it.apiValue.contains("CashFlow") ||
                it.apiValue.contains("Expenditure") ||
                it.apiValue.contains("Repurchase") ||
                it.apiValue.contains("Dividends") && it.apiValue.contains("Paid") ||
                it.apiValue.contains("CashPosition")
            }
        }
    }
}
