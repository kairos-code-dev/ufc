package com.ulalax.ufc.api.client

import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitingSettings

/**
 * UFC 클라이언트 설정 데이터 클래스
 *
 * 외부 API와의 통신을 위한 설정을 정의합니다.
 *
 * @property fredApiKey FRED (Federal Reserve Economic Data) API 키 (선택사항)
 * @property rateLimitingSettings Rate Limiting 설정
 *
 * 사용 예시:
 * ```
 * val config = UFCClientConfig(
 *     fredApiKey = "your-fred-api-key",
 *     rateLimitingSettings = RateLimitingSettings()
 * )
 * ```
 */
@Serializable
data class UFCClientConfig(
    val fredApiKey: String? = null,
    val rateLimitingSettings: RateLimitingSettings = RateLimitingSettings()
)

/**
 * UFC (Universal Financial Client) 클라이언트
 *
 * @deprecated 이 클래스는 deprecated 되었습니다. 대신 `UFC` 클래스를 사용하세요.
 *             UFCClient는 버전 2.0에서 제거될 예정입니다.
 *
 * 기존 코드:
 * ```
 * val client = UFCClient.create(UFCClientConfig())
 * val price = client.getCurrentPrice("AAPL")
 * ```
 *
 * 새로운 코드 (네임스페이스 기반):
 * ```
 * val ufc = UFC.create(UFCClientConfig())
 * val price = ufc.price.getCurrentPrice("AAPL")
 * val companyInfo = ufc.stock.getCompanyInfo("AAPL")
 * val fundData = ufc.funds.getFundData("SPY")
 * ```
 *
 * 네임스페이스 구조:
 * - `ufc.price`: 가격 정보 (현재가, 가격 히스토리)
 * - `ufc.stock`: 주식 기본 정보 (회사 정보, ISIN, 발행주식수)
 * - `ufc.funds`: 펀드 정보 (ETF/뮤추얼펀드 구성)
 * - `ufc.corp`: 기업 행동 (배당금, 주식분할, 자본이득)
 * - `ufc.macro`: 거시경제 지표 (GDP, 실업률, 인플레이션 등)
 *
 * Yahoo Finance, FRED API 등 여러 외부 금융 API와 통신하기 위한 통합 클라이언트입니다.
 * UFCClientImpl을 통해 실제 API 호출을 수행합니다.
 *
 * 주요 기능:
 * - Yahoo Finance Chart API (OHLCV 데이터 조회)
 * - Yahoo Finance QuoteSummary API (주식 요약 정보 조회)
 * - 자동 인증 및 CRUMB 토큰 관리
 * - Rate Limiting (API 속도 제어)
 * - 에러 처리 및 예외 관리
 *
 * AutoCloseable을 구현하므로 try-with-resources를 사용하여 자동으로 리소스를 정리할 수 있습니다.
 *
 * 사용 예시:
 * ```
 * val client = UFCClient.create(UFCClientConfig())
 * try {
 *     // Chart 데이터 조회
 *     val chartData = client.getChartData("AAPL")
 *
 *     // 요약 정보 조회
 *     val summary = client.getStockSummary("AAPL")
 * } finally {
 *     client.close()
 * }
 *
 * // 또는 try-with-resources 사용
 * UFCClient.create(UFCClientConfig()).use { client ->
 *     val data = client.getChartData("AAPL")
 * }
 * ```
 *
 * Phase 13에서 UFCClientImpl과 통합되었습니다.
 * Phase 2차 개발에서 네임스페이스 아키텍처가 추가되었습니다.
 */
@Deprecated(
    message = "Use UFC class instead. UFCClient will be removed in version 2.0. " +
            "Migration example: UFC.create(config).use { ufc -> ufc.price.getCurrentPrice(\"AAPL\") }",
    replaceWith = ReplaceWith(
        expression = "UFC.create(config)",
        imports = ["com.ulalax.ufc.client.UFC"]
    ),
    level = DeprecationLevel.WARNING
)
object UFCClient {
    private val logger = LoggerFactory.getLogger(UFCClient::class.java)

    /**
     * UFCClient를 생성합니다.
     *
     * @deprecated 이 메서드는 deprecated 되었습니다. 대신 `UFC.create()`를 사용하세요.
     *
     * @param config UFC 클라이언트 설정
     * @return 생성된 UFCClientImpl 인스턴스
     * @throws UfcException 인증 또는 초기화 실패 시
     *
     * 예시:
     * ```
     * // 기존 (deprecated)
     * val client = UFCClient.create(UFCClientConfig())
     *
     * // 새로운 방식 (권장)
     * val ufc = UFC.create(UFCClientConfig())
     * ```
     */
    @Deprecated(
        message = "Use UFC.create() instead",
        replaceWith = ReplaceWith(
            expression = "UFC.create(config)",
            imports = ["com.ulalax.ufc.client.UFC"]
        ),
        level = DeprecationLevel.WARNING
    )
    suspend fun create(config: UFCClientConfig): UFCClientImpl {
        logger.warn("UFCClient is deprecated. Please migrate to UFC class for namespace-based API access.")
        logger.info("Creating UFC client with config")
        return UFCClientImpl.create(config).also {
            logger.info("UFC client created successfully")
        }
    }
}
