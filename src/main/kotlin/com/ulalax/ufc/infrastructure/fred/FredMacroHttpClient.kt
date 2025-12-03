package com.ulalax.ufc.infrastructure.fred

import com.ulalax.ufc.domain.macro.MacroHttpClient
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.infrastructure.ratelimit.RateLimiter
import com.ulalax.ufc.infrastructure.fred.FredApiUrls
import com.ulalax.ufc.infrastructure.fred.FredObservationsResponse
import com.ulalax.ufc.infrastructure.fred.FredSeriesResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import org.slf4j.LoggerFactory

/**
 * FRED Macro HTTP 클라이언트 구현체
 *
 * 책임:
 * - HTTP 요청/응답 처리만 담당
 * - JSON 파싱은 MacroService에서 수행 (문맥의 지역성)
 * - Rate Limiting 적용
 * - FRED API Key 관리
 *
 * Infrastructure Layer에 위치:
 * - 외부 API 의존성 격리
 * - Domain은 MacroHttpClient 인터페이스만 의존
 * - 테스트에서 Fake 구현체로 교체 가능
 *
 * @property apiKey FRED API Key
 * @property httpClient Ktor HTTP 클라이언트
 * @property rateLimiter Rate Limiting 제어
 */
internal class FredMacroHttpClient(
    private val apiKey: String,
    private val httpClient: HttpClient,
    private val rateLimiter: RateLimiter
) : MacroHttpClient {

    companion object {
        private val logger = LoggerFactory.getLogger(FredMacroHttpClient::class.java)
    }

    init {
        require(apiKey.isNotBlank()) { "FRED API Key는 빈 문자열이 될 수 없습니다" }
    }

    /**
     * FRED Series Info 조회
     *
     * GET /fred/series
     * - 파라미터: series_id, api_key, file_type
     *
     * @param seriesId 시리즈 ID
     * @return FredSeriesResponse (원본 JSON 응답)
     * @throws UfcException HTTP 요청 실패 시
     */
    override suspend fun fetchSeriesInfo(seriesId: String): FredSeriesResponse {
        require(seriesId.isNotBlank()) { "seriesId는 빈 문자열이 될 수 없습니다" }

        // Rate Limiting 토큰 획득
        rateLimiter.acquire()

        return try {
            logger.debug("Calling FRED Series Info API: seriesId={}", seriesId)

            val response = httpClient.get(FredApiUrls.SERIES) {
                parameter("series_id", seriesId)
                parameter("api_key", apiKey)
                parameter("file_type", "json")
            }

            when (response.status) {
                HttpStatusCode.OK -> response.body<FredSeriesResponse>()
                HttpStatusCode.BadRequest -> throw UfcException(
                    errorCode = ErrorCode.INVALID_PARAMETER,
                    message = "잘못된 파라미터입니다. seriesId: $seriesId"
                )
                HttpStatusCode.Unauthorized -> throw UfcException(
                    errorCode = ErrorCode.AUTH_FAILED,
                    message = "FRED API Key 인증 실패. API Key를 확인하세요."
                )
                HttpStatusCode.NotFound -> throw UfcException(
                    errorCode = ErrorCode.NOT_FOUND,
                    message = "시리즈를 찾을 수 없습니다. seriesId: $seriesId"
                )
                HttpStatusCode.TooManyRequests -> throw UfcException(
                    errorCode = ErrorCode.RATE_LIMITED,
                    message = "FRED API Rate Limit 초과 (120 calls/minute)"
                )
                HttpStatusCode.InternalServerError -> throw UfcException(
                    errorCode = ErrorCode.SOURCE_ERROR,
                    message = "FRED API 서버 오류"
                )
                HttpStatusCode.ServiceUnavailable -> throw UfcException(
                    errorCode = ErrorCode.SOURCE_UNAVAILABLE,
                    message = "FRED API 서비스 일시 중단"
                )
                else -> throw UfcException(
                    errorCode = ErrorCode.SOURCE_ERROR,
                    message = "FRED API 요청 실패. status: ${response.status}"
                )
            }
        } catch (e: UfcException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error while calling FRED Series Info API", e)
            throw UfcException(
                errorCode = ErrorCode.NETWORK_ERROR,
                message = "FRED API 네트워크 오류. seriesId: $seriesId",
                cause = e
            )
        }
    }

    /**
     * FRED Series Observations 조회
     *
     * GET /fred/series/observations
     * - 파라미터: series_id, api_key, file_type, observation_start, observation_end, 등
     *
     * @param seriesId 시리즈 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @param frequency 주기
     * @param units 단위 변환
     * @param aggregationMethod 집계 방법
     * @param sortOrder 정렬 순서
     * @param limit 페이징 제한
     * @param offset 페이징 오프셋
     * @return FredObservationsResponse (원본 JSON 응답)
     * @throws UfcException HTTP 요청 실패 시
     */
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
        require(seriesId.isNotBlank()) { "seriesId는 빈 문자열이 될 수 없습니다" }

        // Rate Limiting 토큰 획득
        rateLimiter.acquire()

        return try {
            logger.debug("Calling FRED Series Observations API: seriesId={}", seriesId)

            val response = httpClient.get(FredApiUrls.SERIES_OBSERVATIONS) {
                parameter("series_id", seriesId)
                parameter("api_key", apiKey)
                parameter("file_type", "json")

                startDate?.let { parameter("observation_start", it) }
                endDate?.let { parameter("observation_end", it) }
                frequency?.let { parameter("frequency", it) }
                units?.let { parameter("units", it) }
                aggregationMethod?.let { parameter("aggregation_method", it) }
                sortOrder?.let { parameter("sort_order", it) }
                limit?.let { parameter("limit", it) }
                offset?.let { parameter("offset", it) }
            }

            when (response.status) {
                HttpStatusCode.OK -> response.body<FredObservationsResponse>()
                HttpStatusCode.BadRequest -> throw UfcException(
                    errorCode = ErrorCode.INVALID_PARAMETER,
                    message = "잘못된 파라미터입니다. seriesId: $seriesId"
                )
                HttpStatusCode.Unauthorized -> throw UfcException(
                    errorCode = ErrorCode.AUTH_FAILED,
                    message = "FRED API Key 인증 실패. API Key를 확인하세요."
                )
                HttpStatusCode.NotFound -> throw UfcException(
                    errorCode = ErrorCode.NOT_FOUND,
                    message = "시리즈를 찾을 수 없습니다. seriesId: $seriesId"
                )
                HttpStatusCode.TooManyRequests -> throw UfcException(
                    errorCode = ErrorCode.RATE_LIMITED,
                    message = "FRED API Rate Limit 초과 (120 calls/minute)"
                )
                HttpStatusCode.InternalServerError -> throw UfcException(
                    errorCode = ErrorCode.SOURCE_ERROR,
                    message = "FRED API 서버 오류"
                )
                HttpStatusCode.ServiceUnavailable -> throw UfcException(
                    errorCode = ErrorCode.SOURCE_UNAVAILABLE,
                    message = "FRED API 서비스 일시 중단"
                )
                else -> throw UfcException(
                    errorCode = ErrorCode.SOURCE_ERROR,
                    message = "FRED API 요청 실패. status: ${response.status}"
                )
            }
        } catch (e: UfcException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error while calling FRED Series Observations API", e)
            throw UfcException(
                errorCode = ErrorCode.NETWORK_ERROR,
                message = "FRED API 네트워크 오류. seriesId: $seriesId",
                cause = e
            )
        }
    }
}
