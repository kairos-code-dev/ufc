package com.ulalax.ufc.infrastructure.yahoo

import com.ulalax.ufc.domain.fundamentals.FundamentalsTimeseriesResponse
import com.ulalax.ufc.domain.quote.QuoteSummaryResponse
import com.ulalax.ufc.domain.stock.StockHttpClient
import com.ulalax.ufc.api.exception.ApiException
import com.ulalax.ufc.api.exception.DataParsingException
import com.ulalax.ufc.api.exception.ErrorCode
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.infrastructure.ratelimit.RateLimiter
import com.ulalax.ufc.infrastructure.http.bodyAsTextWithRecording
import com.ulalax.ufc.infrastructure.yahoo.YahooApiUrls
import com.ulalax.ufc.infrastructure.yahoo.auth.AuthResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Yahoo Finance Stock HTTP 클라이언트 구현체
 *
 * 책임:
 * - HTTP 요청/응답 처리만 담당
 * - JSON 파싱은 StockService에서 수행 (문맥의 지역성)
 * - Rate Limiting 적용
 * - 인증 정보 (CRUMB) 관리
 *
 * Infrastructure Layer에 위치:
 * - 외부 API 의존성 격리
 * - Domain은 StockHttpClient 인터페이스만 의존
 * - 테스트에서 Fake 구현체로 교체 가능
 *
 * @property httpClient Ktor HTTP 클라이언트
 * @property rateLimiter Rate Limiting 제어
 * @property authResult Yahoo Finance 인증 정보 (CRUMB 토큰)
 */
internal class YahooStockHttpClient(
    private val httpClient: HttpClient,
    private val rateLimiter: RateLimiter,
    private val authResult: AuthResult
) : StockHttpClient {

    companion object {
        private val logger = LoggerFactory.getLogger(YahooStockHttpClient::class.java)
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
     * @return QuoteSummaryResponse (원본 JSON 응답)
     * @throws UfcException HTTP 요청 실패 시
     */
    override suspend fun fetchQuoteSummary(
        symbol: String,
        modules: List<String>
    ): QuoteSummaryResponse {
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
     * Yahoo Finance Fundamentals Timeseries API 호출
     *
     * GET /ws/fundamentals-timeseries/v1/finance/timeseries/{symbol}
     * - 파라미터: symbol, period1, period2
     *
     * yfinance의 get_shares_full(start, end) 메서드에 대응됩니다.
     *
     * @param symbol 조회할 심볼
     * @param period1 시작 Unix timestamp (초 단위)
     * @param period2 종료 Unix timestamp (초 단위)
     * @return FundamentalsTimeseriesResponse
     * @throws UfcException HTTP 요청 실패 시
     */
    override suspend fun fetchFundamentalsTimeseries(
        symbol: String,
        period1: Long?,
        period2: Long?
    ): FundamentalsTimeseriesResponse {
        // Rate Limiting 토큰 획득
        rateLimiter.acquire()

        return try {
            // 기본값 설정
            val now = Instant.now().epochSecond
            val actualPeriod2 = period2 ?: now

            // period1 기본값: period2로부터 18개월 전
            // (종료일만 지정된 경우에도 유효한 범위가 되도록)
            val defaultPeriod1 = actualPeriod2 - (18 * 30 * 24 * 60 * 60L)
            val actualPeriod1 = period1 ?: defaultPeriod1

            logger.debug(
                "Calling Yahoo Finance Fundamentals Timeseries API: symbol={}, period1={}, period2={}",
                symbol, actualPeriod1, actualPeriod2
            )

            // API 요청 수행
            val response = httpClient.get("${YahooApiUrls.FUNDAMENTALS_TIMESERIES}/$symbol") {
                parameter("symbol", symbol)
                parameter("period1", actualPeriod1)
                parameter("period2", actualPeriod2)
            }

            // HTTP 상태 코드 확인
            if (!response.status.isSuccess()) {
                throw ApiException(
                    message = "Fundamentals Timeseries API 요청 실패: HTTP ${response.status.value}",
                    statusCode = response.status.value,
                    responseBody = try { response.body<String>() } catch (e: Exception) { null }
                )
            }

            // 응답 파싱 (자동 레코딩 지원)
            val responseBody = response.bodyAsTextWithRecording()
            logger.debug("Fundamentals Timeseries API response received: length={}", responseBody.length)

            try {
                val timeseriesResponse = json.decodeFromString<FundamentalsTimeseriesResponse>(responseBody)

                // 에러 응답 확인
                if (timeseriesResponse.timeseries.error != null) {
                    throw ApiException(
                        message = "Fundamentals Timeseries API 에러: ${timeseriesResponse.timeseries.error?.description ?: "Unknown error"}",
                        statusCode = 200
                    )
                }

                // 결과 확인
                if (timeseriesResponse.timeseries.result.isNullOrEmpty()) {
                    throw UfcException(
                        errorCode = ErrorCode.DATA_NOT_FOUND,
                        message = "Fundamentals Timeseries 데이터를 찾을 수 없습니다: $symbol"
                    )
                }

                timeseriesResponse

            } catch (e: Exception) {
                when (e) {
                    is UfcException, is ApiException -> throw e
                    else -> {
                        logger.error("JSON 파싱 실패. 응답 내용: {}", responseBody.take(500), e)
                        throw DataParsingException(
                            message = "Fundamentals Timeseries 응답 JSON 파싱 실패",
                            sourceData = responseBody.take(500),
                            cause = e
                        )
                    }
                }
            }

        } catch (e: UfcException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error while calling Fundamentals Timeseries API", e)
            throw UfcException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Fundamentals Timeseries API 호출 중 오류 발생: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Business Insider 검색 API 호출
     *
     * GET https://markets.businessinsider.com/ajax/SearchController_Suggest
     * - 파라미터: max_results, query
     *
     * yfinance의 get_isin() 메서드에서 사용하는 방법입니다.
     *
     * @param query 검색 쿼리 (심볼 또는 회사명)
     * @return HTML/텍스트 응답
     * @throws UfcException HTTP 요청 실패 시
     */
    override suspend fun fetchBusinessInsiderSearch(query: String): String {
        // Rate Limiting 토큰 획득
        rateLimiter.acquire()

        return try {
            val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())

            logger.debug("Calling Business Insider Search API: query={}", query)

            // API 요청 수행
            val url = "https://markets.businessinsider.com/ajax/SearchController_Suggest?max_results=25&query=$encodedQuery"
            val response = httpClient.get(url)

            // HTTP 상태 코드 확인
            if (!response.status.isSuccess()) {
                throw ApiException(
                    message = "Business Insider Search API 요청 실패: HTTP ${response.status.value}",
                    statusCode = response.status.value,
                    responseBody = try { response.body<String>() } catch (e: Exception) { null }
                )
            }

            // 응답 텍스트 반환
            val responseBody = response.body<String>()
            logger.debug("Business Insider Search API response received: length={}", responseBody.length)

            responseBody

        } catch (e: UfcException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error while calling Business Insider Search API", e)
            throw UfcException(
                errorCode = ErrorCode.EXTERNAL_API_ERROR,
                message = "Business Insider Search API 호출 중 오류 발생: ${e.message}",
                cause = e
            )
        }
    }
}
