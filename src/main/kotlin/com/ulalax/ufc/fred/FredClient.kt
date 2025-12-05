package com.ulalax.ufc.fred

import com.ulalax.ufc.common.ratelimit.GlobalRateLimiters
import com.ulalax.ufc.fred.internal.FredHttpClient
import com.ulalax.ufc.fred.model.DataFrequency
import com.ulalax.ufc.fred.model.FredSeries
import com.ulalax.ufc.fred.model.FredSeriesInfo
import java.time.LocalDate

/**
 * FRED (Federal Reserve Economic Data) API 클라이언트
 *
 * 미국 연방준비제도(Federal Reserve)의 경제 데이터를 조회하는 클라이언트입니다.
 *
 * 사용 예시:
 * ```kotlin
 * val fred = FredClient.create(apiKey = "your-api-key")
 *
 * // GDP 데이터 조회
 * val gdp = fred.series("GDP")
 *
 * // 실업률 데이터 조회 (기간 지정)
 * val unrate = fred.series(
 *     "UNRATE",
 *     startDate = LocalDate.of(2020, 1, 1),
 *     endDate = LocalDate.of(2023, 12, 31)
 * )
 *
 * // 시계열 메타데이터만 조회
 * val info = fred.seriesInfo("GDP")
 * println("Title: ${info.title}, Frequency: ${info.frequency}")
 *
 * fred.close()
 * ```
 *
 * 또는 use 블록을 사용하여 자동으로 close:
 * ```kotlin
 * FredClient.create("your-api-key").use { fred ->
 *     val gdp = fred.series("GDP")
 *     println(gdp.observations.size)
 * }
 * ```
 *
 * @property httpClient FRED HTTP 클라이언트
 * @property config 클라이언트 설정
 */
class FredClient private constructor(
    private val httpClient: FredHttpClient,
    private val config: FredClientConfig
) : AutoCloseable {

    /**
     * FRED 시계열 데이터를 조회합니다.
     *
     * 시계열 메타데이터와 관측값을 함께 조회합니다.
     *
     * @param seriesId 시계열 ID (예: "GDP", "UNRATE", "DFF")
     * @param startDate 시작 날짜 (null이면 시계열의 첫 데이터부터)
     * @param endDate 종료 날짜 (null이면 시계열의 마지막 데이터까지)
     * @param frequency 데이터 주기 (null이면 원본 주기 사용)
     * @return FRED 시계열 데이터 (메타데이터 + 관측값)
     * @throws IllegalArgumentException seriesId가 비어있을 때
     * @throws NoSuchElementException 시계열을 찾을 수 없을 때
     * @throws IllegalStateException API 인증 실패 또는 Rate Limit 초과
     */
    suspend fun series(
        seriesId: String,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        frequency: DataFrequency? = null
    ): FredSeries {
        return httpClient.fetchSeries(seriesId, startDate, endDate, frequency)
    }

    /**
     * FRED 시계열 메타데이터만 조회합니다.
     *
     * 관측값 없이 시계열의 기본 정보(제목, 주기, 단위 등)만 조회합니다.
     *
     * @param seriesId 시계열 ID (예: "GDP", "UNRATE")
     * @return 시계열 메타데이터
     * @throws IllegalArgumentException seriesId가 비어있을 때
     * @throws NoSuchElementException 시계열을 찾을 수 없을 때
     * @throws IllegalStateException API 인증 실패 또는 Rate Limit 초과
     */
    suspend fun seriesInfo(seriesId: String): FredSeriesInfo {
        return httpClient.fetchSeriesInfo(seriesId)
    }

    /**
     * 클라이언트 리소스를 정리합니다.
     *
     * HTTP 클라이언트를 닫고 연결을 종료합니다.
     */
    override fun close() {
        httpClient.close()
    }

    companion object {
        /**
         * FredClient 인스턴스를 생성합니다.
         *
         * 기본 설정을 사용하여 클라이언트를 생성합니다.
         *
         * @param apiKey FRED API Key (https://fred.stlouisfed.org/docs/api/api_key.html)
         * @return FredClient 인스턴스
         * @throws IllegalArgumentException apiKey가 비어있을 때
         */
        fun create(apiKey: String): FredClient {
            return create(apiKey, FredClientConfig())
        }

        /**
         * FredClient 인스턴스를 생성합니다.
         *
         * 사용자 정의 설정을 사용하여 클라이언트를 생성합니다.
         *
         * @param apiKey FRED API Key
         * @param config 클라이언트 설정
         * @return FredClient 인스턴스
         * @throws IllegalArgumentException apiKey가 비어있을 때
         */
        fun create(apiKey: String, config: FredClientConfig): FredClient {
            require(apiKey.isNotBlank()) { "FRED API Key는 빈 문자열이 될 수 없습니다" }

            // GlobalRateLimiters에서 공유 Rate Limiter 획득
            val rateLimiter = GlobalRateLimiters.getFredLimiter(config.rateLimitConfig)

            val httpClient = FredHttpClient(apiKey, config, rateLimiter)
            return FredClient(httpClient, config)
        }
    }
}
