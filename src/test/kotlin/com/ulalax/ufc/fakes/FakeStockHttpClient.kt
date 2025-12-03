package com.ulalax.ufc.fakes

import com.ulalax.ufc.domain.quote.QuoteSummaryResponse
import com.ulalax.ufc.domain.stock.StockHttpClient
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import java.util.concurrent.ConcurrentHashMap

/**
 * StockHttpClient의 Fake 구현체
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
class FakeStockHttpClient : StockHttpClient {

    // 심볼별 QuoteSummary 응답 저장
    private val quoteSummaryResponses = ConcurrentHashMap<String, QuoteSummaryResponse>()

    // 예외 시뮬레이션
    private var exceptionToThrow: Exception? = null

    // 호출 횟수 추적
    var quoteSummaryCallCount = 0
        private set

    /**
     * QuoteSummary 응답 설정
     */
    fun setQuoteSummaryResponse(symbol: String, response: QuoteSummaryResponse) {
        quoteSummaryResponses[symbol] = response
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
        quoteSummaryResponses.clear()
        exceptionToThrow = null
        quoteSummaryCallCount = 0
    }

    // ============================================================================
    // StockHttpClient 인터페이스 구현
    // ============================================================================

    override suspend fun fetchQuoteSummary(
        symbol: String,
        modules: List<String>
    ): QuoteSummaryResponse {
        quoteSummaryCallCount++

        // 예외 시뮬레이션
        exceptionToThrow?.let { throw it }

        // 응답 반환
        return quoteSummaryResponses[symbol]
            ?: throw UfcException(
                errorCode = ErrorCode.DATA_NOT_FOUND,
                message = "Fake: QuoteSummary 데이터가 설정되지 않았습니다: $symbol"
            )
    }
}
