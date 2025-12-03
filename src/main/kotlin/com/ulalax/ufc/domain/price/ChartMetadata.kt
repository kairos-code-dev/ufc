package com.ulalax.ufc.domain.price

/**
 * 차트(가격 히스토리) 데이터의 메타정보
 *
 * @property symbol 심볼
 * @property currency 통화
 * @property exchangeName 거래소명
 * @property timezone 타임존
 * @property regularMarketPrice 정규 시장 가격
 * @property regularMarketTime 정규 시장 시간 (Unix timestamp)
 * @property dataGranularity 데이터 간격 (1d, 1h, 1m 등)
 * @property range 조회 범위 (1y, 1mo 등)
 * @property validRanges 유효한 범위 목록
 */
data class ChartMetadata(
    val symbol: String,
    val currency: String?,
    val exchangeName: String?,
    val timezone: String?,
    val regularMarketPrice: Double?,
    val regularMarketTime: Long?,
    val dataGranularity: String?,
    val range: String?,
    val validRanges: List<String>?
)
