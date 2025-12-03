package com.ulalax.ufc.domain.macro

import com.ulalax.ufc.infrastructure.util.CacheHelper
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.hours

/**
 * Macro 도메인 서비스 구현체
 *
 * 책임:
 * - 오케스트레이션 (캐시 → HTTP → 파싱)
 * - 도메인 검증
 * - JSON 파싱 (지역성 원칙: 관련 로직을 가까이 배치)
 *
 * 의존성:
 * - MacroHttpClient (인터페이스): 테스트 격리 가능
 * - CacheHelper (구체 클래스): 인메모리로 충분히 빠름
 *
 * 도메인 순수성:
 * - Ktor HttpClient에 직접 의존하지 않음
 * - MacroHttpClient 인터페이스만 의존 (의존성 역전)
 *
 * 문맥의 지역성:
 * - 파싱 로직이 Service 내부에 위치
 * - 별도 Parser 클래스를 만들지 않음 (구현체 하나, YAGNI)
 *
 * @property httpClient HTTP 통신 인터페이스 (테스트 시 Fake로 교체 가능)
 * @property cache 캐싱 유틸리티
 */
class MacroServiceImpl(
    private val httpClient: MacroHttpClient,
    private val cache: CacheHelper
) : MacroService {

    companion object {
        private val logger = LoggerFactory.getLogger(MacroServiceImpl::class.java)

        // 캐시 TTL 설정: 거시경제 데이터는 일단위/월단위로 업데이트되므로 24시간
        private val MACRO_SERIES_TTL = 24.hours
    }

    // ============================================================================
    // Public API Methods
    // ============================================================================

    override suspend fun getSeries(
        seriesId: String,
        startDate: String?,
        endDate: String?,
        frequency: String?,
        units: String?
    ): MacroSeries {
        logger.debug("Fetching macro series: seriesId={}", seriesId)

        // 캐시 키 생성 (파라미터 포함)
        val cacheKey = buildCacheKey(seriesId, startDate, endDate, frequency, units)

        return cache.getOrPut(cacheKey, ttl = MACRO_SERIES_TTL) {
            // Series Info 조회 (메타데이터)
            val seriesInfoResponse = httpClient.fetchSeriesInfo(seriesId)
            val seriesInfo = seriesInfoResponse.seriess.firstOrNull()
                ?: throw IllegalStateException("시리즈 정보를 찾을 수 없습니다. seriesId: $seriesId")

            // Series Observations 조회 (시계열 데이터)
            val observationsResponse = httpClient.fetchSeriesObservations(
                seriesId,
                startDate,
                endDate,
                frequency,
                units,
                null, // aggregationMethod
                null, // sortOrder
                null, // limit
                null  // offset
            )

            // 도메인 모델로 변환
            val dataPoints = observationsResponse.observations.map { observation ->
                MacroDataPoint(
                    date = observation.date,
                    value = parseValue(observation.value)
                )
            }

            MacroSeries(
                seriesId = seriesId,
                title = seriesInfo.title,
                frequency = seriesInfo.frequency,
                units = seriesInfo.units,
                data = dataPoints
            )
        }
    }

    override suspend fun getGDP(
        type: GDPType,
        startDate: String?,
        endDate: String?
    ): MacroSeries {
        return getSeries(
            seriesId = type.seriesId,
            startDate = startDate,
            endDate = endDate
        )
    }

    override suspend fun getInflation(
        type: InflationType,
        startDate: String?,
        endDate: String?
    ): MacroSeries {
        return getSeries(
            seriesId = type.seriesId,
            startDate = startDate,
            endDate = endDate
        )
    }

    override suspend fun getUnemployment(
        type: UnemploymentType,
        startDate: String?,
        endDate: String?
    ): MacroSeries {
        return getSeries(
            seriesId = type.seriesId,
            startDate = startDate,
            endDate = endDate
        )
    }

    override suspend fun getInterestRate(
        type: InterestRateType,
        startDate: String?,
        endDate: String?
    ): MacroSeries {
        return getSeries(
            seriesId = type.seriesId,
            startDate = startDate,
            endDate = endDate
        )
    }

    // ============================================================================
    // Private: 캐시 키 생성
    // ============================================================================

    /**
     * 캐시 키 생성
     *
     * 파라미터가 다르면 다른 캐시 키를 생성하여 별도로 캐싱
     */
    private fun buildCacheKey(
        seriesId: String,
        startDate: String?,
        endDate: String?,
        frequency: String?,
        units: String?
    ): String {
        return buildString {
            append("macro:series:")
            append(seriesId)
            if (startDate != null) append(":start=$startDate")
            if (endDate != null) append(":end=$endDate")
            if (frequency != null) append(":freq=$frequency")
            if (units != null) append(":units=$units")
        }
    }

    // ============================================================================
    // Private: JSON 파싱 (지역성 원칙)
    // ============================================================================

    /**
     * FRED API 응답의 value를 Double로 파싱합니다.
     *
     * FRED API는 결측치를 "."로 표현하므로 null로 변환합니다.
     *
     * @param value FRED API 응답의 value 문자열
     * @return 파싱된 Double 값 (결측치인 경우 null)
     */
    private fun parseValue(value: String): Double? {
        return when {
            value == "." -> null
            value.isBlank() -> null
            else -> value.toDoubleOrNull()
        }
    }
}
