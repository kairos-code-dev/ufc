package com.ulalax.ufc.infrastructure.yahoo

import com.ulalax.ufc.domain.chart.ChartDataResponse
import com.ulalax.ufc.domain.corp.CorpHttpClient
import com.ulalax.ufc.api.exception.ApiException
import com.ulalax.ufc.api.exception.DataParsingException
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.infrastructure.ratelimit.RateLimiter
import com.ulalax.ufc.infrastructure.http.bodyAsTextWithRecording
import com.ulalax.ufc.infrastructure.yahoo.YahooApiUrls
import com.ulalax.ufc.infrastructure.yahoo.auth.AuthResult
import com.ulalax.ufc.domain.common.Interval
import com.ulalax.ufc.domain.common.Period
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Yahoo Finance Corp HTTP 클라이언트 구현체
 *
 * 책임:
 * - HTTP 요청/응답 처리만 담당
 * - JSON 파싱은 CorpService에서 수행 (문맥의 지역성)
 * - Rate Limiting 적용
 * - 인증 정보 (CRUMB) 관리
 *
 * Infrastructure Layer에 위치:
 * - 외부 API 의존성 격리
 * - Domain은 CorpHttpClient 인터페이스만 의존
 * - 테스트에서 Fake 구현체로 교체 가능
 *
 * @property httpClient Ktor HTTP 클라이언트
 * @property rateLimiter Rate Limiting 제어
 * @property authResult Yahoo Finance 인증 정보 (CRUMB 토큰)
 */
internal class YahooCorpHttpClient(
    private val httpClient: HttpClient,
    private val rateLimiter: RateLimiter,
    private val authResult: AuthResult
) : CorpHttpClient {

    companion object {
        private val logger = LoggerFactory.getLogger(YahooCorpHttpClient::class.java)
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    /**
     * Yahoo Finance Chart API 호출 (events 포함)
     *
     * GET /v8/finance/chart/{symbol}
     * - 파라미터: interval, range, events, crumb
     *
     * @param symbol 조회할 심볼
     * @param interval 데이터 간격
     * @param period 조회 기간
     * @param includeEvents 이벤트 데이터 포함 여부
     * @return ChartDataResponse (원본 JSON 응답)
     * @throws UfcException HTTP 요청 실패 시
     */
    override suspend fun fetchChartData(
        symbol: String,
        interval: Interval,
        period: Period,
        includeEvents: Boolean
    ): ChartDataResponse {
        // Rate Limiting 토큰 획득
        rateLimiter.acquire()

        return try {
            logger.debug(
                "Calling Yahoo Finance Chart API for corp events: symbol={}, interval={}, period={}, includeEvents={}",
                symbol, interval.value, period.value, includeEvents
            )

            // API 요청 수행
            val response = httpClient.get("${YahooApiUrls.CHART}/$symbol") {
                parameter("interval", interval.value)
                parameter("range", period.value)
                if (includeEvents) {
                    parameter("events", "div,split,capitalGain")
                }
                parameter("crumb", authResult.crumb)
            }

            // HTTP 상태 코드 확인
            if (!response.status.isSuccess()) {
                throw ApiException(
                    message = "Chart API 요청 실패 (Corp): HTTP ${response.status.value}",
                    statusCode = response.status.value,
                    responseBody = try { response.body<String>() } catch (e: Exception) { null }
                )
            }

            // 응답 파싱 (자동 레코딩 지원)
            val responseBody = response.bodyAsTextWithRecording()
            logger.debug("Chart API response received (Corp): length={}", responseBody.length)

            try {
                val chartDataResponse = json.decodeFromString<ChartDataResponse>(responseBody)

                // 에러 응답 확인
                if (chartDataResponse.chart.error != null) {
                    throw ApiException(
                        message = "Chart API 에러 (Corp): ${chartDataResponse.chart.error?.description ?: "Unknown error"}",
                        statusCode = 200
                    )
                }

                // 결과 확인
                if (chartDataResponse.chart.result.isNullOrEmpty()) {
                    throw UfcException(
                        errorCode = ErrorCode.DATA_NOT_FOUND,
                        message = "Chart 데이터를 찾을 수 없습니다 (Corp): $symbol"
                    )
                }

                chartDataResponse

            } catch (e: Exception) {
                when (e) {
                    is UfcException, is ApiException -> throw e
                    else -> {
                        logger.error("JSON 파싱 실패 (Corp). 응답 내용: {}", responseBody.take(500), e)
                        throw DataParsingException(
                            message = "Chart 응답 JSON 파싱 실패 (Corp)",
                            sourceData = responseBody.take(500),
                            cause = e
                        )
                    }
                }
            }

        } catch (e: UfcException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error while calling Chart API (Corp)", e)
            throw UfcException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Chart API 호출 중 오류 발생 (Corp): ${e.message}",
                cause = e
            )
        }
    }
}
