package com.ulalax.ufc.domain.model.fundamentals

/**
 * Yahoo Finance Fundamentals Timeseries API 조회 결과
 *
 * 요청한 재무 항목들에 대한 시계열 데이터를 타입별로 그룹화하여 제공합니다.
 *
 * ## 특징
 * - **타입별 그룹화**: 각 FundamentalsType별로 시계열 데이터를 별도로 관리합니다.
 * - **빈 결과 지원**: 데이터가 없는 경우 빈 맵을 반환합니다 (예외 발생 안 함).
 * - **헬퍼 메서드**: 특정 타입 데이터 존재 여부 확인 및 안전한 조회를 지원합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val result = yahooClient.fundamentalsTimeseries(
 *     "AAPL",
 *     listOf(FundamentalsType.ANNUAL_TOTAL_REVENUE, FundamentalsType.QUARTERLY_NET_INCOME)
 * )
 *
 * // 특정 타입 데이터 존재 여부 확인
 * if (result.hasData(FundamentalsType.ANNUAL_TOTAL_REVENUE)) {
 *     val revenues = result.get(FundamentalsType.ANNUAL_TOTAL_REVENUE)
 *     revenues?.forEach { dataPoint ->
 *         println("${dataPoint.asOfDate}: ${dataPoint.value}")
 *     }
 * }
 *
 * // 직접 접근
 * val netIncomes = result.data[FundamentalsType.QUARTERLY_NET_INCOME] ?: emptyList()
 * ```
 *
 * @property symbol 조회한 종목 심볼
 * @property data 타입별 시계열 데이터 맵 (FundamentalsType -> 시계열 데이터 리스트)
 */
data class FundamentalsTimeseriesResult(
    val symbol: String,
    val data: Map<FundamentalsType, List<TimeseriesDataPoint>>
) {
    /**
     * 특정 타입의 데이터가 존재하는지 확인합니다.
     *
     * @param type 확인할 FundamentalsType
     * @return 데이터가 존재하면 true, 없으면 false
     */
    fun hasData(type: FundamentalsType): Boolean {
        return data.containsKey(type) && data[type]?.isNotEmpty() == true
    }

    /**
     * 특정 타입의 시계열 데이터를 조회합니다.
     *
     * @param type 조회할 FundamentalsType
     * @return 시계열 데이터 리스트 (데이터가 없으면 null)
     */
    fun get(type: FundamentalsType): List<TimeseriesDataPoint>? {
        return data[type]
    }

    companion object {
        /**
         * 빈 결과를 생성합니다.
         *
         * 데이터가 없는 경우 예외 대신 빈 결과를 반환할 때 사용합니다.
         *
         * @param symbol 조회한 종목 심볼
         * @return 빈 FundamentalsTimeseriesResult
         */
        fun empty(symbol: String): FundamentalsTimeseriesResult {
            return FundamentalsTimeseriesResult(symbol = symbol, data = emptyMap())
        }
    }
}
