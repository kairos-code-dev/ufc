package com.ulalax.ufc.client

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
 * 향후 다음 기능들을 추가할 예정입니다:
 * - FRED API 함수들 (경제 지표 조회)
 * - 캐싱 레이어
 * - 고급 필터링 및 정렬 옵션
 */
object UFCClient {
    private val logger = LoggerFactory.getLogger(UFCClient::class.java)

    /**
     * UFCClient를 생성합니다.
     *
     * @param config UFC 클라이언트 설정
     * @return 생성된 UFCClientImpl 인스턴스
     * @throws UfcException 인증 또는 초기화 실패 시
     *
     * 예시:
     * ```
     * val client = UFCClient.create(UFCClientConfig())
     * ```
     */
    suspend fun create(config: UFCClientConfig): UFCClientImpl {
        logger.info("Creating UFC client with config")
        return UFCClientImpl.create(config).also {
            logger.info("UFC client created successfully")
        }
    }
}
