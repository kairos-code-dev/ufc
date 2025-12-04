package com.ulalax.ufc.domain.funds

/**
 * ETF 및 뮤추얼펀드 정보 조회 서비스 인터페이스.
 *
 * Yahoo Finance를 통해 ETF 및 뮤추얼펀드의 상세 정보를 제공합니다.
 * 펀드의 보유 종목, 자산 배분, 섹터별 비중 등을 조회할 수 있습니다.
 *
 * yfinance의 Ticker.funds_data에 대응됩니다.
 *
 * 사용 예시:
 * ```
 * val fundsService: FundsService = ...
 *
 * // 펀드 데이터 조회
 * val spy = fundsService.getFundData("SPY")
 * println(spy.symbol)              // "SPY"
 * println(spy.quoteType)           // "ETF"
 * println(spy.topHoldings)         // 상위 보유 종목
 *
 * // 펀드 여부 확인
 * val isFund = fundsService.isFund("SPY")
 * println(isFund)                  // true
 *
 * val isStock = fundsService.isFund("AAPL")
 * println(isStock)                 // false
 *
 * // 다중 조회
 * val funds = fundsService.getFundData(listOf("SPY", "AGG", "VTI"))
 * funds.forEach { (symbol, data) ->
 *     println("$symbol: ${data.description}")
 * }
 * ```
 */
interface FundsService {

    /**
     * 단일 펀드의 데이터를 조회합니다.
     *
     * @param symbol 펀드 심볼 (예: "SPY", "AGG", "VTSAX")
     * @return FundData 펀드의 통합 정보
     * @throws com.ulalax.ufc.exception.UfcException
     *   - ErrorCode.INVALID_SYMBOL: 유효하지 않은 심볼
     *   - ErrorCode.FUND_DATA_NOT_FOUND: 펀드 데이터 없음
     *   - ErrorCode.INVALID_FUND_TYPE: ETF/MUTUALFUND가 아님
     *   - ErrorCode.INCOMPLETE_FUND_DATA: 불완전한 데이터
     */
    suspend fun getFundData(symbol: String): FundData

    /**
     * 다중 펀드의 데이터를 조회합니다.
     *
     * 여러 펀드를 한 번에 조회하며, 실패한 심볼은 제외됩니다.
     *
     * @param symbols 펀드 심볼 목록
     * @return Map<String, FundData> 심볼을 키로 하는 펀드 데이터 맵
     * @throws com.ulalax.ufc.exception.UfcException 모든 조회가 실패한 경우
     */
    suspend fun getFundData(symbols: List<String>): Map<String, FundData>

    /**
     * 주어진 심볼이 펀드(ETF 또는 MUTUALFUND)인지 확인합니다.
     *
     * @param symbol 조회할 심볼
     * @return Boolean true이면 펀드, false이면 다른 자산
     * @throws com.ulalax.ufc.exception.UfcException 데이터 조회 실패
     */
    suspend fun isFund(symbol: String): Boolean
}
