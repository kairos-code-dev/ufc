package com.ulalax.ufc.domain.model.market

/**
 * 시장 상태
 *
 * 시장의 현재 거래 상태를 나타냅니다.
 *
 * @property value Yahoo API에서 반환되는 값
 */
enum class MarketState(val value: String) {
    /**
     * 프리마켓 (개장 전)
     */
    PRE("PRE"),

    /**
     * 정규 거래 시간
     */
    REGULAR("REGULAR"),

    /**
     * 애프터마켓 (폐장 후)
     */
    POST("POST"),

    /**
     * 휴장
     */
    CLOSED("CLOSED"),

    /**
     * 알 수 없는 상태
     */
    UNKNOWN("UNKNOWN");

    companion object {
        /**
         * 문자열 값으로부터 MarketState를 찾습니다.
         *
         * @param value 상태 문자열
         * @return 해당하는 MarketState, 찾지 못하면 UNKNOWN
         */
        fun fromValue(value: String?): MarketState =
            entries.find { it.value.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}
