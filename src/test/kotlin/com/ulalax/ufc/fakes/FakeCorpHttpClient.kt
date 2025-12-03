package com.ulalax.ufc.fakes

import com.ulalax.ufc.domain.chart.ChartDataResponse
import com.ulalax.ufc.domain.corp.CorpHttpClient
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.domain.common.Interval
import com.ulalax.ufc.domain.common.Period
import java.util.concurrent.ConcurrentHashMap

/**
 * CorpHttpClient의 Fake 구현체
 *
 * 테스트용 Fake 객체로, 다음 기능을 제공합니다:
 * - 응답 설정 가능
 * - 호출 횟수 추적
 * - 예외 발생 시뮬레이션
 *
 * Classical TDD (State-based Testing) 원칙:
 * - Mock이 아니라 Fake 구현
 * - 상태를 저장하고 검증 가능
 * - 리팩토링 내성 향상
 */
class FakeCorpHttpClient : CorpHttpClient {

    // 심볼별 Chart 응답 저장
    private val chartDataResponses = ConcurrentHashMap<String, ChartDataResponse>()

    // 예외 시뮬레이션
    private var exceptionToThrow: Exception? = null

    // 호출 횟수 추적
    var chartDataCallCount = 0
        private set

    /**
     * ChartData 응답 설정
     */
    fun setChartDataResponse(symbol: String, response: ChartDataResponse) {
        chartDataResponses[symbol] = response
    }

    /**
     * 예외 설정 (다음 호출 시 발생)
     */
    fun setException(exception: Exception) {
        exceptionToThrow = exception
    }

    /**
     * 모든 설정 초기화
     */
    fun clear() {
        chartDataResponses.clear()
        exceptionToThrow = null
        chartDataCallCount = 0
    }

    // ============================================================================
    // CorpHttpClient 인터페이스 구현
    // ============================================================================

    override suspend fun fetchChartData(
        symbol: String,
        interval: Interval,
        period: Period,
        includeEvents: Boolean
    ): ChartDataResponse {
        chartDataCallCount++

        // 예외 시뮬레이션
        exceptionToThrow?.let { throw it }

        // 응답 반환
        return chartDataResponses[symbol]
            ?: throw UfcException(
                errorCode = ErrorCode.DATA_NOT_FOUND,
                message = "Fake: ChartData 데이터가 설정되지 않았습니다 (Corp): $symbol"
            )
    }
}
