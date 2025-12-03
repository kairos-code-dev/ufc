package com.ulalax.ufc.infrastructure.fred

/**
 * FRED (Federal Reserve Economic Data) API URL 상수
 *
 * FRED API 문서: https://fred.stlouisfed.org/docs/api/fred/
 */
internal object FredApiUrls {
    /**
     * FRED API 기본 URL
     */
    private const val BASE_URL = "https://api.stlouisfed.org/fred"

    /**
     * Series Info API
     *
     * GET /fred/series
     * - 파라미터: series_id, api_key, file_type
     */
    const val SERIES = "$BASE_URL/series"

    /**
     * Series Observations API
     *
     * GET /fred/series/observations
     * - 파라미터: series_id, api_key, file_type, observation_start, observation_end,
     *             frequency, units, aggregation_method, sort_order, limit, offset
     */
    const val SERIES_OBSERVATIONS = "$BASE_URL/series/observations"
}
