package com.ulalax.ufc.domain.model.screener

/**
 * Screener 필드의 값 타입
 */
enum class FieldValueType {
    /** 숫자 타입 (Long, Double 등) */
    NUMBER,

    /** 문자열 타입 */
    STRING,

    /** 열거형 타입 (특정 값만 허용) */
    ENUM,
}
