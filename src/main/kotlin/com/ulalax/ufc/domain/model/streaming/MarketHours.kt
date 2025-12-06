package com.ulalax.ufc.domain.model.streaming

/**
 * 시장 시간 상태를 나타내는 열거형.
 *
 * Yahoo Finance WebSocket API의 market_hours 필드 값을 표현합니다.
 */
enum class MarketHours {
    /**
     * 프리마켓 시간 (장 시작 전)
     */
    PRE_MARKET,

    /**
     * 정규 거래 시간
     */
    REGULAR,

    /**
     * 포스트마켓 시간 (장 마감 후)
     */
    POST_MARKET,

    /**
     * 장외 (거래 불가)
     */
    CLOSED,

    /**
     * 알 수 없는 상태
     */
    UNKNOWN,

    ;

    companion object {
        /**
         * Protobuf의 market_hours 코드값을 MarketHours enum으로 변환합니다.
         *
         * @param code Protobuf market_hours 필드 값
         * @return 대응되는 MarketHours enum
         */
        fun fromCode(code: Int): MarketHours =
            when (code) {
                0 -> CLOSED
                1 -> REGULAR
                2 -> PRE_MARKET
                3 -> POST_MARKET
                else -> UNKNOWN
            }
    }
}
