package com.ulalax.ufc.infrastructure.businessinsider

import com.ulalax.ufc.domain.model.security.IsinSearchResult
import com.ulalax.ufc.infrastructure.businessinsider.internal.BusinessInsiderHttpClient
import com.ulalax.ufc.infrastructure.common.ratelimit.GlobalRateLimiters

/**
 * Business Insider Markets 클라이언트
 *
 * ISIN(International Securities Identification Number)을 기반으로
 * 종목 심볼과 정보를 조회하는 클라이언트입니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val bi = BusinessInsiderClient.create()
 * val result = bi.searchIsin("US0378331005")  // Apple Inc
 * println(result.symbol)  // "AAPL"
 * println(result.name)    // "Apple Inc"
 * bi.close()
 * ```
 *
 * ## ISIN 형식
 * - 12자리 영숫자: 국가코드(2) + 식별자(9) + 체크섬(1)
 * - 예시:
 *   - US0378331005: Apple Inc (미국)
 *   - KR7005930003: 삼성전자 (한국)
 *   - GB0005405286: HSBC Holdings (영국)
 *
 * @property httpClient Business Insider HTTP 클라이언트
 * @property config 클라이언트 설정
 */
class BusinessInsiderClient private constructor(
    private val httpClient: BusinessInsiderHttpClient,
    private val config: BusinessInsiderClientConfig,
) : AutoCloseable {
    /**
     * ISIN으로 종목 정보 검색
     *
     * Business Insider Markets API를 통해 ISIN에 해당하는
     * 거래소 심볼과 종목 정보를 조회합니다.
     *
     * @param isin 국제증권식별번호 (12자리 영숫자)
     * @return ISIN 검색 결과 (심볼, 이름, 거래소 등)
     * @throws IllegalArgumentException ISIN 형식이 잘못된 경우
     * @throws NoSuchElementException 검색 결과가 없는 경우
     * @throws Exception 네트워크 오류 또는 API 오류
     */
    suspend fun searchIsin(isin: String): IsinSearchResult = httpClient.searchByIsin(isin)

    /**
     * 클라이언트 리소스 해제
     *
     * HTTP 클라이언트를 닫고 관련 리소스를 정리합니다.
     */
    override fun close() {
        httpClient.close()
    }

    companion object {
        /**
         * 기본 설정으로 Business Insider 클라이언트 생성
         *
         * @return Business Insider 클라이언트 인스턴스
         */
        fun create(): BusinessInsiderClient = create(BusinessInsiderClientConfig())

        /**
         * 사용자 정의 설정으로 Business Insider 클라이언트 생성
         *
         * @param config 클라이언트 설정 (타임아웃, 로깅 등)
         * @return Business Insider 클라이언트 인스턴스
         */
        fun create(config: BusinessInsiderClientConfig): BusinessInsiderClient {
            // GlobalRateLimiters에서 공유 Rate Limiter 획득
            val rateLimiter = GlobalRateLimiters.getBusinessInsiderLimiter(config.rateLimitConfig)

            val httpClient = BusinessInsiderHttpClient(config, rateLimiter)
            return BusinessInsiderClient(httpClient, config)
        }
    }
}
