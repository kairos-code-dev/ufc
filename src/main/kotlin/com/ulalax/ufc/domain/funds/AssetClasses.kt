package com.ulalax.ufc.domain.funds

/**
 * 펀드의 자산 클래스 배분.
 *
 * 펀드가 투자한 자산의 유형별 비중을 나타냅니다.
 * 현금, 주식, 채권, 우선주, 전환사채, 기타 자산의 비중을 포함합니다.
 *
 * @property cashPosition 현금 비중 (%)
 * @property stockPosition 주식 비중 (%)
 * @property bondPosition 채권 비중 (%)
 * @property preferredPosition 우선주 비중 (%)
 * @property convertiblePosition 전환사채 비중 (%)
 * @property otherPosition 기타 자산 비중 (%)
 */
data class AssetClasses(
    val cashPosition: Double?,
    val stockPosition: Double?,
    val bondPosition: Double?,
    val preferredPosition: Double?,
    val convertiblePosition: Double?,
    val otherPosition: Double?
) {
    /**
     * 전체 자산 배분의 합계를 계산합니다.
     *
     * null인 필드는 0으로 취급합니다.
     * 정상적으로 데이터가 입력되면 100에 가까운 값이 반환됩니다.
     *
     * @return 전체 배분 합계 (%)
     */
    fun totalAllocation(): Double =
        listOfNotNull(
            cashPosition,
            stockPosition,
            bondPosition,
            preferredPosition,
            convertiblePosition,
            otherPosition
        ).sum()
}
