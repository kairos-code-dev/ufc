package com.ulalax.ufc.domain.model.screener

/**
 * Screener 쿼리에서 사용되는 연산자
 *
 * Yahoo Finance Screener API는 다양한 논리 및 비교 연산자를 지원합니다.
 */
enum class ScreenerOperator(
    val apiValue: String,
) {
    /** 논리 AND - 모든 조건을 만족 */
    AND("and"),

    /** 논리 OR - 하나 이상의 조건을 만족 */
    OR("or"),

    /** 같음 */
    EQ("eq"),

    /** 초과 (greater than) */
    GT("gt"),

    /** 미만 (less than) */
    LT("lt"),

    /** 이상 (greater than or equal) */
    GTE("gte"),

    /** 이하 (less than or equal) */
    LTE("lte"),

    /** 범위 (between) */
    BTWN("btwn"),

    /**
     * 포함 (is in)
     * 클라이언트에서 OR + EQ 조합으로 변환됩니다.
     * 예: IS_IN("exchange", "NMS", "NYQ") → OR(EQ("exchange", "NMS"), EQ("exchange", "NYQ"))
     */
    IS_IN("is_in"),
}
