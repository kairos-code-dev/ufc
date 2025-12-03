package com.ulalax.ufc.domain.funds

/**
 * 펀드의 운영 정보.
 *
 * 비용률, 회전율, 총순자산 등 펀드 운영과 관련된 메트릭을 담습니다.
 *
 * @property annualReportExpenseRatio 연간 비용률 (%)
 * @property annualHoldingsTurnover 연간 보유 회전율 (%)
 * @property totalNetAssets 총순자산 (TNA)
 */
data class FundOperations(
    val annualReportExpenseRatio: OperationMetric?,
    val annualHoldingsTurnover: OperationMetric?,
    val totalNetAssets: OperationMetric?
)
