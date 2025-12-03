package com.ulalax.ufc.domain.corp

import com.ulalax.ufc.domain.common.Period

/**
 * 기업 행동(Corporate Actions) 정보 조회 서비스 인터페이스
 *
 * 배당금, 주식 분할, 자본이득 등의 기업 행동 히스토리를 제공합니다.
 *
 * 사용 예시:
 * ```
 * val corpService = yahooCorporateActionsService
 *
 * // 배당금 조회
 * val dividends = corpService.getDividends("AAPL", Period.FiveYears)
 * println("${dividends.dividends.size} 개의 배당금 기록")
 *
 * // 주식 분할 조회 (MUTUALFUND 제외)
 * val splits = corpService.getSplits("AAPL", Period.Max)
 * println("${splits.splits.size} 개의 분할 기록")
 *
 * // 자본이득 조회
 * val gains = corpService.getCapitalGains("SPY", Period.FiveYears)
 * println("${gains.capitalGains.size} 개의 자본이득 기록")
 * ```
 */
interface CorpService {

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
