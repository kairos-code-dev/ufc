package com.ulalax.ufc.unit.domain.model

import com.ulalax.ufc.domain.model.options.OptionContract
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
@DisplayName("[U] OptionContract 유틸리티 메서드")
class OptionContractTest {

    @Nested
    @DisplayName("getBidAskSpread")
    inner class GetBidAskSpread {

        @Test
        fun `bid와 ask가 있으면 스프레드를 계산한다`() {
            val contract = createContract(bid = 1.0, ask = 1.5)

            val spread = contract.getBidAskSpread()

            assertThat(spread).isEqualTo(0.5)
        }

        @Test
        fun `bid가 null이면 null을 반환한다`() {
            val contract = createContract(bid = null, ask = 1.5)

            val spread = contract.getBidAskSpread()

            assertThat(spread).isNull()
        }

        @Test
        fun `ask가 null이면 null을 반환한다`() {
            val contract = createContract(bid = 1.0, ask = null)

            val spread = contract.getBidAskSpread()

            assertThat(spread).isNull()
        }
    }

    @Nested
    @DisplayName("getBidAskSpreadPercent")
    inner class GetBidAskSpreadPercent {

        @Test
        fun `bid와 ask가 있으면 스프레드 비율을 계산한다`() {
            val contract = createContract(bid = 1.0, ask = 1.5)

            val spreadPercent = contract.getBidAskSpreadPercent()

            // (1.5 - 1.0) / ((1.5 + 1.0) / 2) * 100 = 0.5 / 1.25 * 100 = 40%
            assertThat(spreadPercent).isEqualTo(40.0)
        }

        @Test
        fun `bid와 ask가 모두 0이면 null을 반환한다`() {
            val contract = createContract(bid = 0.0, ask = 0.0)

            val spreadPercent = contract.getBidAskSpreadPercent()

            assertThat(spreadPercent).isNull()
        }

        @Test
        fun `bid가 null이면 null을 반환한다`() {
            val contract = createContract(bid = null, ask = 1.5)

            val spreadPercent = contract.getBidAskSpreadPercent()

            assertThat(spreadPercent).isNull()
        }
    }

    @Nested
    @DisplayName("getMidPrice")
    inner class GetMidPrice {

        @Test
        fun `bid와 ask가 있으면 중간 가격을 계산한다`() {
            val contract = createContract(bid = 1.0, ask = 1.5)

            val midPrice = contract.getMidPrice()

            assertThat(midPrice).isEqualTo(1.25)
        }

        @Test
        fun `bid가 null이면 null을 반환한다`() {
            val contract = createContract(bid = null, ask = 1.5)

            val midPrice = contract.getMidPrice()

            assertThat(midPrice).isNull()
        }
    }

    @Nested
    @DisplayName("getIntrinsicValue")
    inner class GetIntrinsicValue {

        @Test
        fun `ITM 콜옵션의 내재가치를 계산한다`() {
            val contract = createContract(strike = 100.0)

            val intrinsicValue = contract.getIntrinsicValue(underlyingPrice = 110.0, isCall = true)

            assertThat(intrinsicValue).isEqualTo(10.0)
        }

        @Test
        fun `OTM 콜옵션의 내재가치는 0이다`() {
            val contract = createContract(strike = 100.0)

            val intrinsicValue = contract.getIntrinsicValue(underlyingPrice = 90.0, isCall = true)

            assertThat(intrinsicValue).isEqualTo(0.0)
        }

        @Test
        fun `ITM 풋옵션의 내재가치를 계산한다`() {
            val contract = createContract(strike = 100.0)

            val intrinsicValue = contract.getIntrinsicValue(underlyingPrice = 90.0, isCall = false)

            assertThat(intrinsicValue).isEqualTo(10.0)
        }

        @Test
        fun `OTM 풋옵션의 내재가치는 0이다`() {
            val contract = createContract(strike = 100.0)

            val intrinsicValue = contract.getIntrinsicValue(underlyingPrice = 110.0, isCall = false)

            assertThat(intrinsicValue).isEqualTo(0.0)
        }
    }

    @Nested
    @DisplayName("getTimeValue")
    inner class GetTimeValue {

        @Test
        fun `시간가치를 계산한다`() {
            val contract = createContract(strike = 100.0, lastPrice = 15.0)

            val timeValue = contract.getTimeValue(underlyingPrice = 110.0, isCall = true)

            // lastPrice(15) - intrinsicValue(10) = 5
            assertThat(timeValue).isEqualTo(5.0)
        }

        @Test
        fun `lastPrice가 null이면 null을 반환한다`() {
            val contract = createContract(strike = 100.0, lastPrice = null)

            val timeValue = contract.getTimeValue(underlyingPrice = 110.0, isCall = true)

            assertThat(timeValue).isNull()
        }
    }

    private fun createContract(
        strike: Double = 100.0,
        bid: Double? = 1.0,
        ask: Double? = 1.5,
        lastPrice: Double? = 1.25
    ) = OptionContract(
        contractSymbol = "AAPL250117C00100000",
        strike = strike,
        currency = "USD",
        lastPrice = lastPrice,
        change = 0.0,
        percentChange = 0.0,
        volume = 100,
        openInterest = 1000,
        bid = bid,
        ask = ask,
        contractSize = "REGULAR",
        expiration = 1737158400,
        lastTradeDate = 1704326400,
        impliedVolatility = 0.25,
        inTheMoney = false
    )
}
