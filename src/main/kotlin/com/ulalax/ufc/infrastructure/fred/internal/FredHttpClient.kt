package com.ulalax.ufc.infrastructure.fred.internal

import com.ulalax.ufc.domain.exception.ApiException
import com.ulalax.ufc.domain.exception.ErrorCode
import com.ulalax.ufc.domain.exception.RateLimitException
import com.ulalax.ufc.domain.exception.ValidationException
import com.ulalax.ufc.domain.model.series.DataFrequency
import com.ulalax.ufc.domain.model.series.FredObservation
import com.ulalax.ufc.domain.model.series.FredSeries
import com.ulalax.ufc.domain.model.series.FredSeriesInfo
import com.ulalax.ufc.infrastructure.common.ratelimit.RateLimiter
import com.ulalax.ufc.infrastructure.fred.FredClientConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * FRED HTTP 클라이언트 내부 구현체
 *
 * FRED API와의 HTTP 통신을 담당합니다.
 *
 * @property apiKey FRED API Key
 * @property config 클라이언트 설정
 * @property rateLimiter Rate Limiter
 */
internal class FredHttpClient(
    private val apiKey: String,
    private val config: FredClientConfig,
    private val rateLimiter: RateLimiter,
) : AutoCloseable {
    private val logger = LoggerFactory.getLogger(FredHttpClient::class.java)

    private val httpClient: HttpClient =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        coerceInputValues = true
                    },
                )
            }

            install(HttpTimeout) {
                connectTimeoutMillis = config.connectTimeoutMs
                requestTimeoutMillis = config.requestTimeoutMs
            }

            if (config.enableLogging) {
                install(Logging) {
                    logger =
                        object : Logger {
                            private val log = LoggerFactory.getLogger("FredHttpClient")

                            override fun log(message: String) {
                                log.debug(message)
                            }
                        }
                    level = LogLevel.INFO
                }
            }
        }

    init {
        require(apiKey.isNotBlank()) { "FRED API Key는 빈 문자열이 될 수 없습니다" }
    }

    /**
     * FRED 시계열 데이터를 조회합니다.
     *
     * @param seriesId 시계열 ID (예: "GDP", "UNRATE")
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param frequency 데이터 주기
     * @return FRED 시계열 데이터
     * @throws ValidationException seriesId가 비어있을 때
     * @throws ApiException HTTP 요청 실패 시
     */
    suspend fun fetchSeries(
        seriesId: String,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        frequency: DataFrequency? = null,
    ): FredSeries {
        if (seriesId.isBlank()) {
            throw ValidationException(
                errorCode = ErrorCode.INVALID_PARAMETER,
                message = "seriesId는 빈 문자열이 될 수 없습니다",
                field = "seriesId",
            )
        }

        // 1. 시계열 메타데이터 조회
        val seriesInfo = fetchSeriesInfo(seriesId)

        // 2. 시계열 관측값 조회
        val observations = fetchObservations(seriesId, startDate, endDate, frequency)

        return FredSeries(
            id = seriesInfo.id,
            title = seriesInfo.title,
            frequency = seriesInfo.frequency,
            units = seriesInfo.units,
            observations = observations,
        )
    }

    /**
     * FRED 시계열 메타데이터를 조회합니다.
     *
     * @param seriesId 시계열 ID
     * @return 시계열 정보
     */
    suspend fun fetchSeriesInfo(seriesId: String): FredSeriesInfo {
        if (seriesId.isBlank()) {
            throw ValidationException(
                errorCode = ErrorCode.INVALID_PARAMETER,
                message = "seriesId는 빈 문자열이 될 수 없습니다",
                field = "seriesId",
            )
        }

        logger.debug("Fetching FRED series info: seriesId={}", seriesId)

        // Rate Limit 적용
        rateLimiter.acquire()

        val response =
            httpClient.get(FredApiUrls.SERIES) {
                parameter("series_id", seriesId)
                parameter("api_key", apiKey)
                parameter("file_type", "json")
            }

        return when (response.status) {
            HttpStatusCode.OK -> {
                val fredResponse = response.body<FredSeriesResponse>()
                val series =
                    fredResponse.seriess.firstOrNull()
                        ?: throw ApiException(
                            errorCode = ErrorCode.DATA_NOT_FOUND,
                            message = "시계열 정보를 찾을 수 없습니다: $seriesId",
                            statusCode = response.status.value,
                            metadata = mapOf("seriesId" to seriesId),
                        )

                FredSeriesInfo(
                    id = series.id,
                    title = series.title,
                    frequency = series.frequency,
                    units = series.units,
                    seasonalAdjustment = series.seasonalAdjustment,
                    lastUpdated = series.lastUpdated,
                )
            }
            HttpStatusCode.BadRequest -> throw ValidationException(
                errorCode = ErrorCode.INVALID_PARAMETER,
                message = "잘못된 파라미터입니다. seriesId: $seriesId",
                field = "seriesId",
            )
            HttpStatusCode.Unauthorized -> throw ApiException(
                errorCode = ErrorCode.AUTHENTICATION_FAILED,
                message = "FRED API Key 인증 실패. API Key를 확인하세요.",
                statusCode = response.status.value,
            )
            HttpStatusCode.NotFound -> throw ApiException(
                errorCode = ErrorCode.DATA_NOT_FOUND,
                message = "시계열을 찾을 수 없습니다. seriesId: $seriesId",
                statusCode = response.status.value,
                metadata = mapOf("seriesId" to seriesId),
            )
            HttpStatusCode.TooManyRequests -> throw RateLimitException(
                errorCode = ErrorCode.RATE_LIMIT_EXCEEDED,
                message = "FRED API Rate Limit 초과 (120 calls/minute)",
                metadata = mapOf("seriesId" to seriesId, "rateLimit" to "120 calls/minute"),
            )
            else -> throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "FRED API 요청 실패. status: ${response.status}",
                statusCode = response.status.value,
                metadata = mapOf("seriesId" to seriesId),
            )
        }
    }

    /**
     * FRED 시계열 관측값을 조회합니다.
     *
     * @param seriesId 시계열 ID
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param frequency 데이터 주기
     * @return 관측값 목록
     */
    private suspend fun fetchObservations(
        seriesId: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
        frequency: DataFrequency?,
    ): List<FredObservation> {
        logger.debug(
            "Fetching FRED series observations: seriesId={}, startDate={}, endDate={}",
            seriesId,
            startDate,
            endDate,
        )

        // Rate Limit 적용
        rateLimiter.acquire()

        val response =
            httpClient.get(FredApiUrls.SERIES_OBSERVATIONS) {
                parameter("series_id", seriesId)
                parameter("api_key", apiKey)
                parameter("file_type", "json")

                startDate?.let { parameter("observation_start", it.format(DateTimeFormatter.ISO_DATE)) }
                endDate?.let { parameter("observation_end", it.format(DateTimeFormatter.ISO_DATE)) }
                frequency?.let { parameter("frequency", it.value) }
            }

        return when (response.status) {
            HttpStatusCode.OK -> {
                val fredResponse = response.body<FredObservationsResponse>()
                fredResponse.observations.map { obs ->
                    FredObservation(
                        date = LocalDate.parse(obs.date, DateTimeFormatter.ISO_DATE),
                        value = if (obs.value == ".") null else obs.value.toDoubleOrNull(),
                    )
                }
            }
            HttpStatusCode.BadRequest -> throw ValidationException(
                errorCode = ErrorCode.INVALID_PARAMETER,
                message = "잘못된 파라미터입니다. seriesId: $seriesId",
                field = "seriesId",
            )
            HttpStatusCode.Unauthorized -> throw ApiException(
                errorCode = ErrorCode.AUTHENTICATION_FAILED,
                message = "FRED API Key 인증 실패. API Key를 확인하세요.",
                statusCode = response.status.value,
            )
            HttpStatusCode.NotFound -> throw ApiException(
                errorCode = ErrorCode.DATA_NOT_FOUND,
                message = "시계열을 찾을 수 없습니다. seriesId: $seriesId",
                statusCode = response.status.value,
                metadata = mapOf("seriesId" to seriesId),
            )
            HttpStatusCode.TooManyRequests -> throw RateLimitException(
                errorCode = ErrorCode.RATE_LIMIT_EXCEEDED,
                message = "FRED API Rate Limit 초과 (120 calls/minute)",
                metadata = mapOf("seriesId" to seriesId, "rateLimit" to "120 calls/minute"),
            )
            else -> throw ApiException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "FRED API 요청 실패. status: ${response.status}",
                statusCode = response.status.value,
                metadata = mapOf("seriesId" to seriesId),
            )
        }
    }

    override fun close() {
        httpClient.close()
    }
}
