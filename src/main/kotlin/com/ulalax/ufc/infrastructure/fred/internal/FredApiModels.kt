package com.ulalax.ufc.infrastructure.fred.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * FRED API series 응답 모델
 */
@Serializable
internal data class FredSeriesResponse(
    val seriess: List<FredSeriesDto>
)

/**
 * FRED API series DTO
 */
@Serializable
internal data class FredSeriesDto(
    val id: String,
    val title: String,
    val frequency: String,
    val units: String,
    @SerialName("seasonal_adjustment")
    val seasonalAdjustment: String,
    @SerialName("last_updated")
    val lastUpdated: String
)

/**
 * FRED API observations 응답 모델
 */
@Serializable
internal data class FredObservationsResponse(
    val observations: List<FredObservationDto>
)

/**
 * FRED API observation DTO
 */
@Serializable
internal data class FredObservationDto(
    val date: String,
    val value: String
)
