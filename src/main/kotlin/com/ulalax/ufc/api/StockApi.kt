package com.ulalax.ufc.api

import com.ulalax.ufc.domain.stock.CompanyInfo
import com.ulalax.ufc.domain.stock.FastInfo
import com.ulalax.ufc.domain.stock.SharesData
import java.time.LocalDate

/**
 * 주식 기본 정보 조회 API
 *
 * 회사의 기본 정보, 빠른 정보, ISIN 코드, 발행주식수 등을 제공합니다.
 * yfinance의 Ticker.info, fast_info, isin, get_shares 등에 대응됩니다.
 *
 * 사용 예시:
 * ```kotlin
 * val ufc = UFC.create(config)
 *
 * // 회사 기본 정보 조회
 * val info = ufc.stock.getCompanyInfo("AAPL")
 * println("${info.longName} - ${info.sector}")
 *
 * // 빠른 정보 조회
 * val fast = ufc.stock.getFastInfo("GOOGL")
 * println("${fast.symbol} (${fast.exchange})")
 *
 * // ISIN 조회
 * val isin = ufc.stock.getIsin("MSFT")
 * println("ISIN: $isin")
 * ```
 */
interface StockApi {

    /**
     * 회사 기본 정보를 조회합니다.
     *
     * 회사명, 섹터, 업종, 연락처 정보 등 상세한 정보를 반환합니다.
     *
     * @param symbol 심볼 (예: "AAPL")
     * @return 회사 기본 정보
     * @throws UfcException 데이터 조회 실패 시
     */
    suspend fun getCompanyInfo(symbol: String): CompanyInfo

    /**
     * 다중 심볼의 회사 기본 정보를 조회합니다.
     *
     * @param symbols 심볼 목록 (최대 50개 권장)
     * @return 심볼별 회사 정보 맵. 실패한 심볼은 제외됨
     */
    suspend fun getCompanyInfo(symbols: List<String>): Map<String, CompanyInfo>

    /**
     * 빠른 정보를 조회합니다.
     *
     * 최소한의 필드(currency, exchange, quoteType, symbol)만 반환합니다.
     * yfinance의 fast_info에 대응됩니다.
     *
     * @param symbol 심볼
     * @return 빠른 조회용 정보
     * @throws UfcException 데이터 조회 실패 시
     */
    suspend fun getFastInfo(symbol: String): FastInfo

    /**
     * 다중 심볼의 빠른 정보를 조회합니다.
     *
     * @param symbols 심볼 목록 (최대 50개 권장)
     * @return 심볼별 빠른 정보 맵. 실패한 심볼은 제외됨
     */
    suspend fun getFastInfo(symbols: List<String>): Map<String, FastInfo>

    /**
     * ISIN 코드를 조회합니다.
     *
     * ISIN (International Securities Identification Number)은 각 증권을 고유하게 식별합니다.
     * yfinance의 isin 속성에 대응됩니다.
     *
     * @param symbol 심볼
     * @return ISIN 코드 (예: "US0378331005")
     * @throws UfcException ISIN 데이터 없음
     */
    suspend fun getIsin(symbol: String): String

    /**
     * 다중 심볼의 ISIN 코드를 조회합니다.
     *
     * @param symbols 심볼 목록 (최대 50개 권장)
     * @return 심볼별 ISIN 코드 맵. 실패한 심볼은 제외됨
     */
    suspend fun getIsin(symbols: List<String>): Map<String, String>

    /**
     * 발행주식수 히스토리를 조회합니다.
     *
     * 최근 분기별 발행주식수를 반환합니다.
     * yfinance의 get_shares()에 대응됩니다.
     *
     * @param symbol 심볼
     * @return 분기별 발행주식수 리스트 (날짜 순서)
     * @throws UfcException 데이터 없음
     */
    suspend fun getShares(symbol: String): List<SharesData>

    /**
     * 다중 심볼의 발행주식수 히스토리를 조회합니다.
     *
     * @param symbols 심볼 목록 (최대 50개 권장)
     * @return 심볼별 발행주식수 리스트 맵. 실패한 심볼은 제외됨
     */
    suspend fun getShares(symbols: List<String>): Map<String, List<SharesData>>

    /**
     * 지정된 기간의 발행주식수 상세 히스토리를 조회합니다.
     *
     * yfinance의 get_shares_full(start, end)에 대응됩니다.
     *
     * @param symbol 심볼
     * @param start 시작일 (null이면 최초 데이터부터)
     * @param end 종료일 (null이면 최신 데이터까지)
     * @return 기간별 발행주식수 리스트
     * @throws UfcException 데이터 없음
     */
    suspend fun getSharesFull(
        symbol: String,
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<SharesData>
}
