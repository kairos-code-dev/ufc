package com.ulalax.ufc.domain.model.visualization

/**
 * 실적 발표 이벤트 타입
 *
 * Yahoo Finance Visualization API에서 반환하는 이벤트 타입을 나타냅니다.
 *
 * @property code Yahoo API에서 사용하는 이벤트 코드
 */
enum class EarningsEventType(val code: Int) {
    /**
     * 실적 전화 회의 (Earnings Call)
     */
    CALL(1),

    /**
     * 실적 발표 (Earnings Report)
     */
    EARNINGS(2),

    /**
     * 주주총회 (Shareholder Meeting)
     */
    MEETING(11),

    /**
     * 알 수 없는 이벤트 타입
     */
    UNKNOWN(-1);

    companion object {
        /**
         * 코드 값으로부터 EarningsEventType을 찾습니다.
         *
         * @param code 이벤트 코드
         * @return 해당하는 EarningsEventType, 찾을 수 없으면 UNKNOWN
         */
        fun fromCode(code: Int): EarningsEventType {
            return entries.find { it.code == code } ?: UNKNOWN
        }
    }
}
