package com.ulalax.ufc.infrastructure.fred

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * FRED API Series 응답 모델
 *
 * GET /fred/series
 */
@Serializable
data class FredSeriesResponse(
    @SerialName("seriess")
    val seriess: List<FredSeries>
)

/**
 * FRED Series 메타데이터
 */
@Serializable
data class FredSeries(
    val id: String,
    val title: String,
    @SerialName("observation_start")
    val observationStart: String,
    @SerialName("observation_end")
    val observationEnd: String,
    val frequency: String,
    @SerialName("frequency_short")
    val frequencyShort: String,
    val units: String,
    @SerialName("units_short")
    val unitsShort: String,
    @SerialName("seasonal_adjustment")
    val seasonalAdjustment: String? = null,
    @SerialName("seasonal_adjustment_short")
    val seasonalAdjustmentShort: String? = null,
    @SerialName("last_updated")
    val lastUpdated: String,
    val popularity: Int,
    val notes: String? = null
)

/**
 * FRED API Series Observations 응답 모델
 *
 * GET /fred/series/observations
 */
@Serializable
data class FredObservationsResponse(
    @SerialName("observations")
    val observations: List<FredObservation>
)

/**
 * FRED Observation 데이터 포인트
 */
@Serializable
data class FredObservation(
    @SerialName("realtime_start")
    val realtimeStart: String,
    @SerialName("realtime_end")
    val realtimeEnd: String,
    val date: String,
    val value: String  // "." indicates missing value
)
