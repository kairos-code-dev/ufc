package com.ulalax.ufc.api

import com.ulalax.ufc.domain.corp.DividendHistory
import com.ulalax.ufc.domain.corp.SplitHistory
import com.ulalax.ufc.domain.corp.CapitalGainHistory
import com.ulalax.ufc.domain.common.Period

/**
 * 기업 행동(Corporate Actions) 정보 조회 API
 *
 * 배당금, 주식 분할, 자본이득 등의 기업 행동 히스토리를 제공합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val ufc = UFC.create(config)
 *
 * // 배당금 조회
 * val dividends = ufc.corp.getDividends("AAPL", Period.FiveYears)
 * dividends.dividends.forEach { div ->
 *     println("${div.date}: ${div.amount} USD")
 * }
 *
 * // 주식 분할 조회
 * val splits = ufc.corp.getSplits("AAPL", Period.Max)
 * splits.splits.forEach { split ->
 *     println("${split.date}: ${split.numerator}:${split.denominator}")
 * }
 *
 * // 자본이득 조회 (주로 ETF/뮤추얼펀드)
 * val gains = ufc.corp.getCapitalGains("SPY", Period.FiveYears)
 * gains.capitalGains.forEach { gain ->
 *     println("${gain.date}: ${gain.amount} USD")
 * }
 * ```
 */
interface CorpApi {

    /**
     * 배당금 히스토리를 조회합니다.
     *
     * @param symbol 조회할 심볼
     * @param period 조회 기간 (기본값: FiveYears)
     * @return 배당금 히스토리
     * @throws UfcException 조회 실패 시
     */
    suspend fun getDividends(
        symbol: String,
        period: Period = Period.FiveYears
    ): DividendHistory

    /**
     * 주식 분할 히스토리를 조회합니다.
     *
     * @param symbol 조회할 심볼 (MUTUALFUND 제외)
     * @param period 조회 기간 (기본값: Max)
     * @return 주식 분할 히스토리
     * @throws UnsupportedAssetTypeException MUTUALFUND에 대한 조회 시
     * @throws UfcException 조회 실패 시
     */
    suspend fun getSplits(
        symbol: String,
        period: Period = Period.Max
    ): SplitHistory

    /**
     * 자본이득 분배 히스토리를 조회합니다.
     *
     * @param symbol 조회할 심볼
     * @param period 조회 기간 (기본값: FiveYears)
     * @return 자본이득 분배 히스토리
     * @throws UfcException 조회 실패 시
     */
    suspend fun getCapitalGains(
        symbol: String,
        period: Period = Period.FiveYears
    ): CapitalGainHistory
}
