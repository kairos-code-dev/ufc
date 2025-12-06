package com.ulalax.ufc.infrastructure.yahoo.streaming.internal

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Yahoo Finance WebSocket API에서 수신되는 Protobuf 메시지.
 *
 * Base64 디코딩된 바이너리 데이터를 Protobuf 형식으로 파싱합니다.
 * 모든 필드는 optional이며, 실제 데이터에 따라 일부 필드만 채워질 수 있습니다.
 */
@Serializable
internal data class PricingData(
    /**
     * 심볼 (예: "AAPL")
     */
    @ProtoNumber(1)
    val id: String = "",
    /**
     * 현재가
     */
    @ProtoNumber(2)
    val price: Float = 0f,
    /**
     * Unix timestamp (초 단위)
     */
    @ProtoNumber(3)
    val time: Long = 0L,
    /**
     * 통화 (예: "USD")
     */
    @ProtoNumber(4)
    val currency: String = "",
    /**
     * 거래소 (예: "NMS")
     */
    @ProtoNumber(5)
    val exchange: String = "",
    /**
     * 자산 유형 코드
     * 1: EQUITY, 2: ETF, 5: OPTION, 6: MUTUAL_FUND, 8: INDEX,
     * 11: CRYPTOCURRENCY, 12: CURRENCY, 13: FUTURE
     */
    @ProtoNumber(6)
    val quoteType: Int = 0,
    /**
     * 시장 시간 상태
     * 0: CLOSED, 1: REGULAR, 2: PRE_MARKET, 3: POST_MARKET
     */
    @ProtoNumber(7)
    val marketHours: Int = 0,
    /**
     * 등락률 (%)
     */
    @ProtoNumber(8)
    val changePercent: Float = 0f,
    /**
     * 당일 거래량
     */
    @ProtoNumber(9)
    val dayVolume: Long = 0L,
    /**
     * 당일 고가
     */
    @ProtoNumber(10)
    val dayHigh: Float = 0f,
    /**
     * 당일 저가
     */
    @ProtoNumber(11)
    val dayLow: Float = 0f,
    /**
     * 등락폭 (절대값)
     */
    @ProtoNumber(12)
    val change: Float = 0f,
    /**
     * 자산명 (예: "Apple Inc.")
     */
    @ProtoNumber(13)
    val shortName: String = "",
    /**
     * 만료일 (옵션 전용, Unix timestamp)
     */
    @ProtoNumber(14)
    val expireDate: Long = 0L,
    /**
     * 시가
     */
    @ProtoNumber(15)
    val openPrice: Float = 0f,
    /**
     * 전일 종가
     */
    @ProtoNumber(16)
    val previousClose: Float = 0f,
    /**
     * 행사가 (옵션 전용)
     */
    @ProtoNumber(17)
    val strikePrice: Float = 0f,
    /**
     * 기초자산 심볼 (옵션 전용)
     */
    @ProtoNumber(18)
    val underlyingSymbol: String = "",
    /**
     * 미결제약정 (옵션 전용)
     */
    @ProtoNumber(19)
    val openInterest: Long = 0L,
    /**
     * 옵션 유형 (옵션 전용)
     * Call/Put 구분
     */
    @ProtoNumber(20)
    val optionsType: Long = 0L,
    /**
     * 미니 옵션 여부 (옵션 전용)
     */
    @ProtoNumber(21)
    val miniOption: Long = 0L,
    /**
     * 최근 체결 수량
     */
    @ProtoNumber(22)
    val lastSize: Long = 0L,
    /**
     * 매수 호가
     */
    @ProtoNumber(23)
    val bid: Float = 0f,
    /**
     * 매수 호가 수량
     */
    @ProtoNumber(24)
    val bidSize: Long = 0L,
    /**
     * 매도 호가
     */
    @ProtoNumber(25)
    val ask: Float = 0f,
    /**
     * 매도 호가 수량
     */
    @ProtoNumber(26)
    val askSize: Long = 0L,
    /**
     * 소수점 자릿수 힌트
     */
    @ProtoNumber(27)
    val priceHint: Long = 0L,
    /**
     * 24시간 거래량 (암호화폐 전용)
     */
    @ProtoNumber(28)
    val vol24hr: Long = 0L,
    /**
     * 전체 통화 거래량 (암호화폐 전용)
     */
    @ProtoNumber(29)
    val volAllCurrencies: Long = 0L,
    /**
     * 기준 통화 (환율 전용)
     */
    @ProtoNumber(30)
    val fromCurrency: String = "",
    /**
     * 최근 체결 시장
     */
    @ProtoNumber(31)
    val lastMarket: String = "",
    /**
     * 유통량 (암호화폐 전용)
     */
    @ProtoNumber(32)
    val circulatingSupply: Double = 0.0,
    /**
     * 시가총액
     */
    @ProtoNumber(33)
    val marketCap: Double = 0.0,
)
