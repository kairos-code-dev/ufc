package com.ulalax.ufc.domain.stock

/**
 * 금융 자산의 유형을 정의하는 열거형.
 *
 * Yahoo Finance API의 quoteType 필드를 매핑합니다.
 */
enum class AssetType(val value: String) {
    EQUITY("EQUITY"),
    ETF("ETF"),
    MUTUALFUND("MUTUALFUND"),
    INDEX("INDEX"),
    CRYPTOCURRENCY("CRYPTOCURRENCY"),
    CURRENCY("CURRENCY"),
    FUTURE("FUTURE"),
    OPTION("OPTION"),
    UNKNOWN("UNKNOWN");

    companion object {
        fun fromString(value: String?): AssetType =
            values().firstOrNull { it.value == value } ?: UNKNOWN
    }
}
