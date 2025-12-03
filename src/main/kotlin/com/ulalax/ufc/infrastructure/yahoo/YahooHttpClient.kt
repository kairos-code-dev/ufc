package com.ulalax.ufc.infrastructure.yahoo

import com.ulalax.ufc.domain.chart.ChartDataResponse
import com.ulalax.ufc.domain.price.PriceHttpClient
import com.ulalax.ufc.domain.quote.QuoteSummaryResponse
import com.ulalax.ufc.api.exception.ApiException
import com.ulalax.ufc.api.exception.DataParsingException
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.infrastructure.ratelimit.RateLimiter
import com.ulalax.ufc.infrastructure.http.bodyAsTextWithRecording
import com.ulalax.ufc.infrastructure.yahoo.YahooApiUrls
import com.ulalax.ufc.infrastructure.yahoo.auth.AuthResult
import com.ulalax.ufc.infrastructure.yahoo.response.ChartResponse
import com.ulalax.ufc.infrastructure.yahoo.response.PriceResponse
import com.ulalax.ufc.domain.common.Interval
import com.ulalax.ufc.domain.common.Period
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Yahoo Finance HTTP 클라이언트 구현체
 *
 * 책임:
 * - HTTP 요청/응답 처리만 담당
 * - JSON 파싱은 Service에서 수행 (문맥의 지역성)
 * - Rate Limiting 적용
 * - 인증 정보 (CRUMB) 관리
 *
 * Infrastructure Layer에 위치:
 * - 외부 API 의존성 격리
 * - Domain은 PriceHttpClient 인터페이스만 의존
 * - 테스트에서 Fake 구현체로 교체 가능
 *
 * @property httpClient Ktor HTTP 클라이언트
 * @property rateLimiter Rate Limiting 제어
 * @property authResult Yahoo Finance 인증 정보 (CRUMB 토큰)
 */
internal class YahooHttpClient(
    private val httpClient: HttpClient,
    private val rateLimiter: RateLimiter,
    private val authResult: AuthResult
) : PriceHttpClient {

    companion object {
        private val logger = LoggerFactory.getLogger(YahooHttpClient::class.java)
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    /**
     * Yahoo Finance QuoteSummary API 호출
     *
     * GET /v10/finance/quoteSummary/{symbol}
     * - 파라미터: modules, crumb
     *
     * @param symbol 조회할 심볼
     * @param modules 조회할 모듈 목록
     * @return PriceResponse (원본 JSON 응답)
     * @throws UfcException HTTP 요청 실패 시
     */
    override suspend fun fetchQuoteSummary(
        symbol: String,
        modules: List<String>
    ): PriceResponse {
        // Rate Limiting 토큰 획득
        rateLimiter.acquire()

        return try {
            logger.debug(
                "Calling Yahoo Finance QuoteSummary API: symbol={}, modules={}",
                symbol, modules
            )

            // API 요청 수행
            val response = httpClient.get("${YahooApiUrls.QUOTE_SUMMARY}/$symbol") {
                parameter("modules", modules.joinToString(","))
                parameter("crumb", authResult.crumb)
            }

            // HTTP 상태 코드 확인
            if (!response.status.isSuccess()) {
                throw ApiException(
                    message = "QuoteSummary API 요청 실패: HTTP ${response.status.value}",
                    statusCode = response.status.value,
                    responseBody = try { response.body<String>() } catch (e: Exception) { null }
                )
            }

            // 응답 파싱 (자동 레코딩 지원)
            val responseBody = response.bodyAsTextWithRecording()
            logger.debug("QuoteSummary API response received: length={}", responseBody.length)

            try {
                val quoteSummaryResponse = json.decodeFromString<QuoteSummaryResponse>(responseBody)

                // 에러 응답 확인
                if (quoteSummaryResponse.quoteSummary.error != null) {
                    throw ApiException(
                        message = "QuoteSummary API 에러: ${quoteSummaryResponse.quoteSummary.error?.description ?: "Unknown error"}",
                        statusCode = 200
                    )
                }

                // 결과 확인
                if (quoteSummaryResponse.quoteSummary.result.isNullOrEmpty()) {
                    throw UfcException(
                        errorCode = ErrorCode.DATA_NOT_FOUND,
                        message = "QuoteSummary 데이터를 찾을 수 없습니다: $symbol"
                    )
                }

                quoteSummaryResponse

            } catch (e: Exception) {
                when (e) {
                    is UfcException, is ApiException -> throw e
                    else -> {
                        logger.error("JSON 파싱 실패. 응답 내용: {}", responseBody.take(500), e)
                        throw DataParsingException(
                            message = "QuoteSummary 응답 JSON 파싱 실패",
                            sourceData = responseBody.take(500),
                            cause = e
                        )
                    }
                }
            }

        } catch (e: UfcException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error while calling QuoteSummary API", e)
            throw UfcException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "QuoteSummary API 호출 중 오류 발생: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Yahoo Finance Chart API 호출 (기간 기반)
     *
     * GET /v8/finance/chart/{symbol}
     * - 파라미터: interval, range, crumb
     *
     * @param symbol 조회할 심볼
     * @param interval 데이터 간격
     * @param period 조회 기간
     * @return ChartResponse (원본 JSON 응답)
     * @throws UfcException HTTP 요청 실패 시
     */
    override suspend fun fetchChart(
        symbol: String,
        interval: Interval,
        period: Period
    ): ChartResponse {
        // Rate Limiting 토큰 획득
        rateLimiter.acquire()

        return try {
            logger.debug(
                "Calling Yahoo Finance Chart API: symbol={}, interval={}, range={}",
                symbol, interval.value, period.value
            )

            // API 요청 수행
            val response = httpClient.get("${YahooApiUrls.CHART}/$symbol") {
                parameter("interval", interval.value)
                parameter("range", period.value)
                parameter("crumb", authResult.crumb)
            }

            // HTTP 상태 코드 확인
            if (!response.status.isSuccess()) {
                throw ApiException(
                    message = "Chart API 요청 실패: HTTP ${response.status.value}",
                    statusCode = response.status.value,
                    responseBody = try { response.body<String>() } catch (e: Exception) { null }
                )
            }

            // 응답 파싱 (자동 레코딩 지원)
            val responseBody = response.bodyAsTextWithRecording()
            logger.debug("Chart API response received: length={}", responseBody.length)

            parseChartResponse(responseBody, symbol)

        } catch (e: UfcException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error while calling Chart API", e)
            throw UfcException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Chart API 호출 중 오류 발생: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Yahoo Finance Chart API 호출 (날짜 범위 기반)
     *
     * GET /v8/finance/chart/{symbol}
     * - 파라미터: interval, period1, period2, crumb
     *
     * @param symbol 조회할 심볼
     * @param start 시작 날짜
     * @param end 종료 날짜
     * @param interval 데이터 간격
     * @return ChartResponse (원본 JSON 응답)
     * @throws UfcException HTTP 요청 실패 시
     */
    override suspend fun fetchChartByDateRange(
        symbol: String,
        start: LocalDate,
        end: LocalDate,
        interval: Interval
    ): ChartResponse {
        // Rate Limiting 토큰 획득
        rateLimiter.acquire()

        return try {
            logger.debug(
                "Calling Yahoo Finance Chart API (date range): symbol={}, start={}, end={}, interval={}",
                symbol, start, end, interval.value
            )

            // LocalDate를 Unix timestamp로 변환
            val period1 = start.atStartOfDay(ZoneOffset.UTC).toEpochSecond()
            val period2 = end.atTime(23, 59, 59).toInstant(ZoneOffset.UTC).epochSecond

            // API 요청 수행
            val response = httpClient.get("${YahooApiUrls.CHART}/$symbol") {
                parameter("interval", interval.value)
                parameter("period1", period1)
                parameter("period2", period2)
                parameter("crumb", authResult.crumb)
            }

            // HTTP 상태 코드 확인
            if (!response.status.isSuccess()) {
                throw ApiException(
                    message = "Chart API 요청 실패: HTTP ${response.status.value}",
                    statusCode = response.status.value,
                    responseBody = try { response.body<String>() } catch (e: Exception) { null }
                )
            }

            // 응답 파싱 (자동 레코딩 지원)
            val responseBody = response.bodyAsTextWithRecording()
            logger.debug("Chart API response received: length={}", responseBody.length)

            parseChartResponse(responseBody, symbol)

        } catch (e: UfcException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error while calling Chart API", e)
            throw UfcException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Chart API 호출 중 오류 발생: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Yahoo Finance Chart API 호출 (이벤트 포함)
     *
     * GET /v8/finance/chart/{symbol}
     * - 파라미터: interval, range, events, crumb
     *
     * @param symbol 조회할 심볼
     * @param interval 데이터 간격
     * @param period 조회 기간
     * @param includeEvents 이벤트 데이터 포함 여부
     * @return ChartResponse (원본 JSON 응답)
     * @throws UfcException HTTP 요청 실패 시
     */
    override suspend fun fetchChartWithEvents(
        symbol: String,
        interval: Interval,
        period: Period,
        includeEvents: Boolean
    ): ChartResponse {
        // Rate Limiting 토큰 획득
        rateLimiter.acquire()

        return try {
            logger.debug(
                "Calling Yahoo Finance Chart API (with events): symbol={}, interval={}, range={}, includeEvents={}",
                symbol, interval.value, period.value, includeEvents
            )

            // API 요청 수행
            val response = httpClient.get("${YahooApiUrls.CHART}/$symbol") {
                parameter("interval", interval.value)
                parameter("range", period.value)
                parameter("crumb", authResult.crumb)
                if (includeEvents) {
                    parameter("events", "div,splits,capitalGains")
                }
            }

            // HTTP 상태 코드 확인
            if (!response.status.isSuccess()) {
                throw ApiException(
                    message = "Chart API 요청 실패: HTTP ${response.status.value}",
                    statusCode = response.status.value,
                    responseBody = try { response.body<String>() } catch (e: Exception) { null }
                )
            }

            // 응답 파싱 (자동 레코딩 지원)
            val responseBody = response.bodyAsTextWithRecording()
            logger.debug("Chart API response received: length={}", responseBody.length)

            parseChartResponse(responseBody, symbol)

        } catch (e: UfcException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error while calling Chart API", e)
            throw UfcException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Chart API 호출 중 오류 발생: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Chart API 응답 파싱 (공통 로직)
     *
     * @param responseBody JSON 응답 문자열
     * @param symbol 심볼 (에러 메시지용)
     * @return ChartResponse
     * @throws UfcException 파싱 실패 시
     */
    private fun parseChartResponse(responseBody: String, symbol: String): ChartResponse {
        return try {
            val chartResponse = json.decodeFromString<ChartDataResponse>(responseBody)

            // 에러 응답 확인
            if (chartResponse.chart.error != null) {
                throw ApiException(
                    message = "Chart API 에러: ${chartResponse.chart.error?.description ?: "Unknown error"}",
                    statusCode = 200
                )
            }

            // 결과 확인
            if (chartResponse.chart.result.isNullOrEmpty()) {
                throw UfcException(
                    errorCode = ErrorCode.DATA_NOT_FOUND,
                    message = "차트 데이터를 찾을 수 없습니다: $symbol"
                )
            }

            chartResponse

        } catch (e: Exception) {
            when (e) {
                is UfcException, is ApiException -> throw e
                else -> {
                    logger.error("JSON 파싱 실패. 응답 내용: {}", responseBody.take(500), e)
                    throw DataParsingException(
                        message = "Chart 응답 JSON 파싱 실패",
                        sourceData = responseBody.take(500),
                        cause = e
                    )
                }
            }
        }
    }
}
