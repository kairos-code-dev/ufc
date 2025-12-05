package com.ulalax.ufc.domain.model.options

/**
 * 옵션 체인
 *
 * 특정 만기일의 콜/풋 옵션 목록을 포함합니다.
 *
 * @property expirationDate 만기일 (Unix timestamp, seconds)
 * @property hasMiniOptions 미니 옵션 존재 여부
 * @property calls 콜 옵션 목록
 * @property puts 풋 옵션 목록
 */
data class OptionsChain(
    val expirationDate: Long,
    val hasMiniOptions: Boolean,
    val calls: List<OptionContract>,
    val puts: List<OptionContract>
) {
    /**
     * 특정 행사가의 콜 옵션 찾기
     *
     * @param strike 행사가
     * @return 해당 행사가의 콜 옵션, 없으면 null
     */
    fun findCall(strike: Double): OptionContract? {
        return calls.find { it.strike == strike }
    }

    /**
     * 특정 행사가의 풋 옵션 찾기
     *
     * @param strike 행사가
     * @return 해당 행사가의 풋 옵션, 없으면 null
     */
    fun findPut(strike: Double): OptionContract? {
        return puts.find { it.strike == strike }
    }

    /**
     * ITM(내가격) 콜 옵션 목록
     *
     * @return ITM 콜 옵션 목록
     */
    fun getInTheMoneyCall(): List<OptionContract> {
        return calls.filter { it.inTheMoney }
    }

    /**
     * ITM(내가격) 풋 옵션 목록
     *
     * @return ITM 풋 옵션 목록
     */
    fun getInTheMoneyPut(): List<OptionContract> {
        return puts.filter { it.inTheMoney }
    }

    /**
     * OTM(외가격) 콜 옵션 목록
     *
     * @return OTM 콜 옵션 목록
     */
    fun getOutOfTheMoneyCall(): List<OptionContract> {
        return calls.filter { !it.inTheMoney }
    }

    /**
     * OTM(외가격) 풋 옵션 목록
     *
     * @return OTM 풋 옵션 목록
     */
    fun getOutOfTheMoneyPut(): List<OptionContract> {
        return puts.filter { !it.inTheMoney }
    }
}
