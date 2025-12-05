package com.ulalax.ufc.domain.model.market

/**
 * 시장 타임존 정보
 *
 * 시장의 타임존 관련 정보를 나타냅니다.
 *
 * @property shortName 타임존 약어 (예: EST, KST)
 * @property ianaName 타임존 IANA 이름 (예: America/New_York, Asia/Seoul)
 * @property gmtOffsetMillis GMT 오프셋 (밀리초)
 */
data class MarketTimezone(
    val shortName: String,
    val ianaName: String,
    val gmtOffsetMillis: Long
)
