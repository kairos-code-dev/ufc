package com.ulalax.ufc.domain.model.lookup

/**
 * Yahoo Finance Lookup API에서 지원하는 금융상품 타입
 *
 * 금융상품을 타입별로 필터링하여 검색할 수 있습니다.
 *
 * @property apiValue Yahoo Finance API에 전달되는 실제 파라미터 값
 */
enum class LookupType(
    val apiValue: String,
) {
    /**
     * 모든 타입의 금융상품
     */
    ALL("all"),

    /**
     * 주식 (Stock)
     * 예: AAPL, GOOGL
     */
    EQUITY("equity"),

    /**
     * 뮤추얼펀드 (Mutual Fund)
     * 예: VFIAX, FXAIX
     */
    MUTUAL_FUND("mutualfund"),

    /**
     * 상장지수펀드 (ETF)
     * 예: SPY, QQQ
     */
    ETF("etf"),

    /**
     * 인덱스 (Index)
     * 예: ^GSPC, ^DJI
     */
    INDEX("index"),

    /**
     * 선물 (Future)
     * 예: ES=F, GC=F
     */
    FUTURE("future"),

    /**
     * 통화 (Currency)
     * 예: EURUSD=X
     */
    CURRENCY("currency"),

    /**
     * 암호화폐 (Cryptocurrency)
     * 예: BTC-USD, ETH-USD
     */
    CRYPTOCURRENCY("cryptocurrency"),
}
