package com.ulalax.ufc.domain.chart

import com.ulalax.ufc.exception.ErrorCode
import com.ulalax.ufc.exception.UfcException
import com.ulalax.ufc.exception.ApiException
import com.ulalax.ufc.exception.DataParsingException
import com.ulalax.ufc.infrastructure.ratelimit.RateLimiter
import com.ulalax.ufc.internal.ResponseRecordingContext
import com.ulalax.ufc.internal.bodyAsTextWithRecording
import com.ulalax.ufc.internal.yahoo.YahooApiUrls
import com.ulalax.ufc.internal.yahoo.auth.AuthResult
import com.ulalax.ufc.model.common.Interval
import com.ulalax.ufc.model.common.Period
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Yahoo Finance Chart API의 구현체
 *
 * 다음 엔드포인트를 사용합니다:
 * - GET /v8/finance/chart/{symbol}
 *   - 파라미터: interval, range, crumb, .crumb
 *
 * Rate Limiting:
 * - TokenBucketRateLimiter 적용
 * - 각 요청마다 1개 토큰 소비
 *
 * 에러 처리:
 * - HTTP 오류: ApiException 발생
 * - JSON 파싱 오류: DataParsingException 발생
 * - 기타 예외: UfcException 발생
 *
 * @property httpClient Ktor HttpClient
 * @property rateLimiter Rate Limiting 제어
 * @property authResult Yahoo Finance 인증 정보 (CRUMB 토큰 포함)
 */
class YahooChartService(
    private val httpClient: HttpClient,
    private val rateLimiter: RateLimiter,
    private val authResult: AuthResult
) : ChartService {

    companion object {
        private val logger = LoggerFactory.getLogger(YahooChartService::class.java)
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    /**
     * 단일 심볼의 차트 데이터를 조회합니다.
     *
     * 프로세스:
     * 1. Rate Limiting 토큰 획득
     * 2. API 요청 수행
     * 3. 응답 파싱
     * 4. OHLCV 데이터로 변환
     *
     * @param symbol 조회할 심볼 (예: "AAPL")
     * @param interval 데이터 간격 (기본값: OneDay)
     * @param period 조회 기간 (기본값: OneYear)
     * @return OHLCV 데이터 리스트
     * @throws UfcException 조회 실패 시
     */
    override suspend fun getChartData(
        symbol: String,
        interval: Interval,
        period: Period
    ): List<OHLCVData> {
        validateSymbol(symbol)

        logger.info(
            "Fetching chart data: symbol={}, interval={}, period={}",
            symbol, interval.value, period.value
        )

        return try {
            // 원본 응답 조회
            val response = getRawChartData(symbol, interval, period)

            // OHLCV 데이터로 변환
            val ohlcvDataList = response.toOHLCVDataList()

            logger.info(
                "Chart data fetched successfully: symbol={}, dataPoints={}",
                symbol, ohlcvDataList.size
            )

            ohlcvDataList

        } catch (e: UfcException) {
            logger.error(
                "Failed to fetch chart data: symbol={}, interval={}, period={}",
                symbol, interval.value, period.value, e
            )
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error while fetching chart data", e)
            throw UfcException(
                errorCode = ErrorCode.DATA_RETRIEVAL_ERROR,
                message = "차트 데이터 조회 중 오류 발생: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * 다중 심볼의 차트 데이터를 조회합니다.
     *
     * 각 심볼별로 순차적으로 요청합니다.
     *
     * @param symbols 조회할 심볼 목록
     * @param interval 데이터 간격 (기본값: OneDay)
     * @param period 조회 기간 (기본값: OneYear)
     * @return 심볼별 OHLCV 데이터 맵
     * @throws UfcException 조회 실패 시
     */
    override suspend fun getChartData(
        symbols: List<String>,
        interval: Interval,
        period: Period
    ): Map<String, List<OHLCVData>> {
        if (symbols.isEmpty()) {
            throw UfcException(
                errorCode = ErrorCode.INVALID_PARAMETER,
                message = "조회할 심볼 목록이 비어있습니다"
            )
        }

        logger.info(
            "Fetching chart data for multiple symbols: count={}, interval={}, period={}",
            symbols.size, interval.value, period.value
        )

        val result = mutableMapOf<String, List<OHLCVData>>()

        for (symbol in symbols) {
            try {
                val data = getChartData(symbol, interval, period)
                result[symbol] = data
            } catch (e: Exception) {
                logger.warn("Failed to fetch chart data for symbol: {}", symbol, e)
                throw e
            }
        }

        logger.info("Successfully fetched chart data for {} symbols", symbols.size)
        return result
    }

    /**
     * 차트 데이터의 원본 응답을 조회합니다.
     *
     * @param symbol 조회할 심볼
     * @param interval 데이터 간격
     * @param period 조회 기간
     * @return ChartDataResponse 객체
     * @throws UfcException 조회 실패 시
     */
    override suspend fun getRawChartData(
        symbol: String,
        interval: Interval,
        period: Period
    ): ChartDataResponse {
        validateSymbol(symbol)

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

            try {
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
     * 심볼 유효성을 검증합니다.
     */
    private fun validateSymbol(symbol: String) {
        if (symbol.isBlank()) {
            throw UfcException(
                errorCode = ErrorCode.INVALID_SYMBOL,
                message = "심볼이 비어있습니다"
            )
        }

        if (symbol.length > 10) {
            throw UfcException(
                errorCode = ErrorCode.INVALID_SYMBOL,
                message = "심볼이 너무 깁니다: $symbol"
            )
        }
    }
}

/**
 * ChartDataResponse를 OHLCVData 리스트로 변환합니다.
 */
private fun ChartDataResponse.toOHLCVDataList(): List<OHLCVData> {
    val result = chart.result?.firstOrNull()
        ?: return emptyList()

    val timestamps = result.timestamp ?: return emptyList()
    val indicators = result.indicators ?: return emptyList()
    val quote = indicators.quote?.firstOrNull() ?: return emptyList()
    val adjCloseData = indicators.adjclose?.firstOrNull()?.adjclose

    val opens = quote.open ?: emptyList()
    val highs = quote.high ?: emptyList()
    val lows = quote.low ?: emptyList()
    val closes = quote.close ?: emptyList()
    val volumes = quote.volume ?: emptyList()

    // 타임스탬프와 OHLCV 데이터를 병렬로 처리하여 완전한 데이터만 포함
    return timestamps.indices.mapNotNull { index ->
        val open = opens.getOrNull(index)
        val high = highs.getOrNull(index)
        val low = lows.getOrNull(index)
        val close = closes.getOrNull(index)
        val volume = volumes.getOrNull(index)
        val adjClose = adjCloseData?.getOrNull(index)

        // 모든 필수 필드가 null이 아닐 경우에만 데이터 포함
        if (open != null && high != null && low != null && close != null && volume != null) {
            OHLCVData(
                timestamp = timestamps[index],
                open = open,
                high = high,
                low = low,
                close = close,
                adjClose = adjClose,
                volume = volume
            )
        } else {
            null
        }
    }
}
