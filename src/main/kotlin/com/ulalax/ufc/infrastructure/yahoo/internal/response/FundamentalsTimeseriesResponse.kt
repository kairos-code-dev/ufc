package com.ulalax.ufc.infrastructure.yahoo.internal.response

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * Yahoo Finance Fundamentals Timeseries API 최상위 응답
 *
 * 내부 API 응답 형식을 직렬화/역직렬화하기 위한 internal 타입입니다.
 */
@Serializable
internal data class FundamentalsTimeseriesResponse(
    @SerialName("timeseries")
    val timeseries: Timeseries,
)

/**
 * Timeseries 컨테이너
 */
@Serializable
internal data class Timeseries(
    @SerialName("result")
    val result: List<TimeseriesResult>? = null,
    @SerialName("error")
    val error: ApiError? = null,
)

/**
 * API 에러 정보
 */
@Serializable
internal data class ApiError(
    @SerialName("code")
    val code: String? = null,
    @SerialName("description")
    val description: String? = null,
)

/**
 * Result 메타데이터
 */
@Serializable
internal data class Meta(
    @SerialName("type")
    val type: List<String>? = null,
    @SerialName("symbol")
    val symbol: List<String>? = null,
)

/**
 * 개별 데이터 포인트
 *
 * Yahoo Finance API는 reportedValue 내부에 raw, fmt를 포함합니다.
 */
@Serializable
internal data class DataPoint(
    @SerialName("asOfDate")
    val asOfDate: String? = null,
    @SerialName("periodType")
    val periodType: String? = null,
    @SerialName("currencyCode")
    val currencyCode: String? = null,
    @SerialName("reportedValue")
    val reportedValue: ReportedValue? = null,
)

/**
 * 보고된 값 (raw, fmt 구조)
 */
@Serializable
internal data class ReportedValue(
    @SerialName("raw")
    val raw: JsonElement? = null,
    @SerialName("fmt")
    val fmt: String? = null,
) {
    /**
     * raw 값을 Double로 추출합니다.
     * Long이나 Double 모두 처리 가능합니다.
     */
    val doubleValue: Double? get() {
        return when {
            raw == null -> null
            raw is JsonNull -> null
            else -> {
                val primitive = raw.jsonPrimitive
                primitive.doubleOrNull ?: primitive.longOrNull?.toDouble()
            }
        }
    }
}

/**
 * TimeseriesResult - 동적 필드를 처리하는 커스텀 Serializer 사용
 *
 * Yahoo Finance API는 요청한 type명을 동적으로 필드명으로 사용합니다.
 * 예: annualTotalRevenue, quarterlyNetIncome 등
 *
 * 이를 처리하기 위해 커스텀 Serializer를 구현합니다.
 */
@Serializable(with = TimeseriesResultSerializer::class)
internal data class TimeseriesResult(
    val meta: Meta,
    val timestamp: List<Long>,
    val dataFields: Map<String, List<DataPoint>>,
)

/**
 * TimeseriesResult를 위한 커스텀 Serializer
 *
 * meta, timestamp는 고정 필드이고, 나머지는 모두 동적 필드로 처리합니다.
 */
@OptIn(ExperimentalSerializationApi::class)
internal object TimeseriesResultSerializer : KSerializer<TimeseriesResult> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TimeseriesResult")

    override fun serialize(
        encoder: Encoder,
        value: TimeseriesResult,
    ): Unit = throw NotImplementedError("Serialization is not supported for TimeseriesResult")

    override fun deserialize(decoder: Decoder): TimeseriesResult {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw IllegalStateException("TimeseriesResultSerializer requires JsonDecoder")

        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject

        // 고정 필드 파싱
        val meta =
            jsonDecoder.json.decodeFromJsonElement<Meta>(
                jsonObject["meta"] ?: JsonObject(emptyMap()),
            )
        val timestamp =
            jsonDecoder.json.decodeFromJsonElement<List<Long>>(
                jsonObject["timestamp"] ?: JsonArray(emptyList()),
            )

        // 동적 필드 파싱 (meta, timestamp, error 제외한 모든 필드)
        val dataFields = mutableMapOf<String, List<DataPoint>>()
        val excludedKeys = setOf("meta", "timestamp", "error")

        for ((key, value) in jsonObject) {
            if (key !in excludedKeys && value is JsonArray) {
                try {
                    val dataPoints = jsonDecoder.json.decodeFromJsonElement<List<DataPoint>>(value)
                    dataFields[key] = dataPoints
                } catch (e: Exception) {
                    // 파싱 실패 시 무시 (알 수 없는 필드 형식)
                }
            }
        }

        return TimeseriesResult(
            meta = meta,
            timestamp = timestamp,
            dataFields = dataFields,
        )
    }
}
