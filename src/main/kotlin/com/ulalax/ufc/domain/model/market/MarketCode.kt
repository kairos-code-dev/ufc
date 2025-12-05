package com.ulalax.ufc.domain.model.market

/**
 * 지원하는 시장 코드
 *
 * Yahoo Finance Market API에서 지원하는 시장들을 나타냅니다.
 *
 * @property code Yahoo API에 전달되는 시장 코드
 * @property description 시장 설명
 */
enum class MarketCode(val code: String, val description: String) {
    US("us", "United States"),
    KR("kr", "South Korea"),
    JP("jp", "Japan"),
    GB("gb", "United Kingdom"),
    DE("de", "Germany"),
    HK("hk", "Hong Kong"),
    CN("cn", "China"),
    FR("fr", "France");

    companion object {
        /**
         * 문자열 코드로부터 MarketCode를 찾습니다.
         *
         * @param code 시장 코드 문자열
         * @return 해당하는 MarketCode, 찾지 못하면 null
         */
        fun fromCode(code: String): MarketCode? =
            entries.find { it.code.equals(code, ignoreCase = true) }
    }
}
