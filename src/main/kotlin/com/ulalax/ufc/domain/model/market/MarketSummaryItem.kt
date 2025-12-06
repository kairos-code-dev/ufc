package com.ulalax.ufc.domain.model.market

import java.time.Instant

/**
 * 시장 요약 항목
 *
 * 특정 시장의 개별 지수 또는 주요 종목 정보를 나타냅니다.
 *
 * @property exchange 거래소 코드 (예: NMS, NYQ, KSC)
 * @property symbol 지수 심볼 (예: ^GSPC, ^KS11)
 * @property shortName 짧은 이름 (예: S&P 500, KOSPI)
 * @property regularMarketPrice 현재가
 * @property regularMarketChange 전일 대비 변동폭
 * @property regularMarketChangePercent 전일 대비 변동률 (%)
 * @property regularMarketTime 마지막 업데이트 시각
 * @property regularMarketDayHigh 당일 최고가
 * @property regularMarketDayLow 당일 최저가
 * @property regularMarketVolume 거래량
 * @property regularMarketPreviousClose 전일 종가
 * @property currency 통화 코드 (예: USD, KRW, JPY)
 * @property marketState 시장 상태
 * @property quoteType 자산 타입 (예: INDEX, EQUITY)
 * @property timezoneName 타임존 IANA 이름
 * @property timezoneShortName 타임존 약어 (예: EST, KST)
 * @property gmtOffsetMillis GMT 오프셋 (밀리초)
 */
data class MarketSummaryItem(
    val exchange: String,
    val symbol: String,
    val shortName: String,
    val regularMarketPrice: Double? = null,
    val regularMarketChange: Double? = null,
    val regularMarketChangePercent: Double? = null,
    val regularMarketTime: Instant? = null,
    val regularMarketDayHigh: Double? = null,
    val regularMarketDayLow: Double? = null,
    val regularMarketVolume: Long? = null,
    val regularMarketPreviousClose: Double? = null,
    val currency: String? = null,
    val marketState: MarketState? = null,
    val quoteType: String? = null,
    val timezoneName: String? = null,
    val timezoneShortName: String? = null,
    val gmtOffsetMillis: Long? = null,
)
