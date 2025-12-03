package com.ulalax.ufc.fakes

import com.ulalax.ufc.domain.macro.MacroHttpClient
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.infrastructure.fred.FredObservationsResponse
import com.ulalax.ufc.infrastructure.fred.FredSeriesResponse
import java.util.concurrent.ConcurrentHashMap

/**
 * MacroHttpClient의 Fake 구현체
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
class FakeMacroHttpClient : MacroHttpClient {

    // 시리즈별 응답 저장
    private val seriesInfoResponses = ConcurrentHashMap<String, FredSeriesResponse>()
    private val observationsResponses = ConcurrentHashMap<String, FredObservationsResponse>()

    // 예외 시뮬레이션
    private var exceptionToThrow: Exception? = null

    // 호출 횟수 추적
    var seriesInfoCallCount = 0
        private set
    var observationsCallCount = 0
        private set

    /**
     * SeriesInfo 응답 설정
     */
    fun setSeriesInfoResponse(seriesId: String, response: FredSeriesResponse) {
        seriesInfoResponses[seriesId] = response
    }

    /**
     * Observations 응답 설정
     */
    fun setObservationsResponse(seriesId: String, response: FredObservationsResponse) {
        observationsResponses[seriesId] = response
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
        seriesInfoResponses.clear()
        observationsResponses.clear()
        exceptionToThrow = null
        seriesInfoCallCount = 0
        observationsCallCount = 0
    }

    // ============================================================================
    // MacroHttpClient 인터페이스 구현
    // ============================================================================

    override suspend fun fetchSeriesInfo(seriesId: String): FredSeriesResponse {
        seriesInfoCallCount++

        // 예외 시뮬레이션
        exceptionToThrow?.let { throw it }

        // 응답 반환
        return seriesInfoResponses[seriesId]
            ?: throw UfcException(
                errorCode = ErrorCode.DATA_NOT_FOUND,
                message = "Fake: SeriesInfo 데이터가 설정되지 않았습니다 (Macro): $seriesId"
            )
    }

    override suspend fun fetchSeriesObservations(
        seriesId: String,
        startDate: String?,
        endDate: String?,
        frequency: String?,
        units: String?,
        aggregationMethod: String?,
        sortOrder: String?,
        limit: Int?,
        offset: Int?
    ): FredObservationsResponse {
        observationsCallCount++

        // 예외 시뮬레이션
        exceptionToThrow?.let { throw it }

        // 응답 반환
        return observationsResponses[seriesId]
            ?: throw UfcException(
                errorCode = ErrorCode.DATA_NOT_FOUND,
                message = "Fake: Observations 데이터가 설정되지 않았습니다 (Macro): $seriesId"
            )
    }
}
