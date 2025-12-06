package com.ulalax.ufc.domain.model.streaming

/**
 * 자산 유형을 나타내는 열거형.
 *
 * Yahoo Finance WebSocket API의 quote_type 필드 값을 표현합니다.
 */
enum class QuoteType {
    /**
     * 주식
     */
    EQUITY,

    /**
     * 상장지수펀드 (ETF)
     */
    ETF,

    /**
     * 인덱스
     */
    INDEX,

    /**
     * 뮤추얼펀드
     */
    MUTUAL_FUND,

    /**
     * 옵션
     */
    OPTION,

    /**
     * 암호화폐
     */
    CRYPTOCURRENCY,

    /**
     * 환율
     */
    CURRENCY,

    /**
     * 선물
     */
    FUTURE,

    /**
     * 알 수 없는 유형
     */
    UNKNOWN,

    ;

    companion object {
        /**
         * Protobuf의 quote_type 코드값을 QuoteType enum으로 변환합니다.
         *
         * @param code Protobuf quote_type 필드 값
         * @return 대응되는 QuoteType enum
         */
        fun fromCode(code: Int): QuoteType =
            when (code) {
                1 -> EQUITY
                2 -> ETF
                5 -> OPTION
                6 -> MUTUAL_FUND
                8 -> INDEX
                11 -> CRYPTOCURRENCY
                12 -> CURRENCY
                13 -> FUTURE
                else -> UNKNOWN
            }
    }
}
