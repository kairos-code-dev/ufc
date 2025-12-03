package com.ulalax.ufc.fakes

import com.ulalax.ufc.domain.corp.CapitalGain
import com.ulalax.ufc.domain.corp.CapitalGainHistory
import com.ulalax.ufc.domain.corp.CorpService
import com.ulalax.ufc.domain.corp.Dividend
import com.ulalax.ufc.domain.corp.DividendHistory
import com.ulalax.ufc.domain.corp.Split
import com.ulalax.ufc.domain.corp.SplitHistory
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.domain.common.Period

/**
 * 테스트용 CorpService Fake 구현체
 *
 * 미리 정의된 응답 데이터를 반환합니다.
 */
class FakeCorpService : CorpService {

    // 배당금 응답 저장소
    private val dividendResponses = mutableMapOf<String, DividendHistory>()

    // 분할 응답 저장소
    private val splitResponses = mutableMapOf<String, SplitHistory>()

    // 자본이득 응답 저장소
    private val capitalGainResponses = mutableMapOf<String, CapitalGainHistory>()

    // 에러를 발생시킬 심볼
    private val errorSymbols = mutableSetOf<String>()

    /**
     * 배당금 응답을 추가합니다.
     */
    fun addDividendResponse(symbol: String, response: DividendHistory) {
        dividendResponses["${symbol.uppercase()}:dividends"] = response
    }

    /**
     * 분할 응답을 추가합니다.
     */
    fun addSplitResponse(symbol: String, response: SplitHistory) {
        splitResponses["${symbol.uppercase()}:splits"] = response
    }

    /**
     * 자본이득 응답을 추가합니다.
     */
    fun addCapitalGainResponse(symbol: String, response: CapitalGainHistory) {
        capitalGainResponses["${symbol.uppercase()}:gains"] = response
    }

    /**
     * 특정 심볼에 대해 에러를 발생시키도록 설정합니다.
     */
    fun addErrorSymbol(symbol: String) {
        errorSymbols.add(symbol.uppercase())
    }

    /**
     * 에러 심볼 설정을 제거합니다.
     */
    fun removeErrorSymbol(symbol: String) {
        errorSymbols.remove(symbol.uppercase())
    }

    /**
     * 모든 설정을 초기화합니다.
     */
    fun reset() {
        dividendResponses.clear()
        splitResponses.clear()
        capitalGainResponses.clear()
        errorSymbols.clear()
    }

    override suspend fun getDividends(
        symbol: String,
        period: Period
    ): DividendHistory {
        val normalizedSymbol = symbol.uppercase()

        if (normalizedSymbol in errorSymbols) {
            throw UfcException(
                errorCode = ErrorCode.DATA_NOT_FOUND,
                message = "No dividend data found for symbol: $symbol"
            )
        }

        return dividendResponses["$normalizedSymbol:dividends"]
            ?: DividendHistory(
                symbol = symbol,
                dividends = emptyList()
            )
    }

    override suspend fun getSplits(
        symbol: String,
        period: Period
    ): SplitHistory {
        val normalizedSymbol = symbol.uppercase()

        if (normalizedSymbol in errorSymbols) {
            throw UfcException(
                errorCode = ErrorCode.DATA_NOT_FOUND,
                message = "No split data found for symbol: $symbol"
            )
        }

        return splitResponses["$normalizedSymbol:splits"]
            ?: SplitHistory(
                symbol = symbol,
                splits = emptyList()
            )
    }

    override suspend fun getCapitalGains(
        symbol: String,
        period: Period
    ): CapitalGainHistory {
        val normalizedSymbol = symbol.uppercase()

        if (normalizedSymbol in errorSymbols) {
            throw UfcException(
                errorCode = ErrorCode.DATA_NOT_FOUND,
                message = "No capital gains data found for symbol: $symbol"
            )
        }

        return capitalGainResponses["$normalizedSymbol:gains"]
            ?: CapitalGainHistory(
                symbol = symbol,
                capitalGains = emptyList()
            )
    }
}

/**
 * FakeCorpService 빌더 클래스
 *
 * 테스트 데이터를 쉽게 구성하기 위한 헬퍼 클래스입니다.
 */
class FakeCorpServiceBuilder {
    private val service = FakeCorpService()

    fun withDividends(
        symbol: String,
        dividends: List<Dividend>
    ) = apply {
        service.addDividendResponse(
            symbol,
            DividendHistory(symbol, dividends.sortedBy { it.date })
        )
    }

    fun withSplits(
        symbol: String,
        splits: List<Split>
    ) = apply {
        service.addSplitResponse(
            symbol,
            SplitHistory(symbol, splits.sortedBy { it.date })
        )
    }

    fun withCapitalGains(
        symbol: String,
        gains: List<CapitalGain>
    ) = apply {
        service.addCapitalGainResponse(
            symbol,
            CapitalGainHistory(symbol, gains.sortedBy { it.date })
        )
    }

    fun withError(symbol: String) = apply {
        service.addErrorSymbol(symbol)
    }

    fun build(): FakeCorpService = service
}
