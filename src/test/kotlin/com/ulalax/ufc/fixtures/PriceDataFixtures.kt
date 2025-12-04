package com.ulalax.ufc.fixtures

import com.ulalax.ufc.domain.price.PriceData

/**
 * PriceData 테스트 데이터 Fixture (Mother Pattern)
 *
 * PriceData 도메인 모델의 다양한 시나리오를 위한 테스트 데이터를 제공합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val standardPrice = PriceDataFixtures.standard()
 * val priceAtHigh = PriceDataFixtures.atFiftyTwoWeekHigh()
 * val customPrice = PriceDataFixtures.builder()
 *     .withLastPrice(150.0)
 *     .withPreviousClose(140.0)
 *     .build()
 * ```
 */
object PriceDataFixtures {

    /**
     * 기본 PriceData 생성
     *
     * 모든 필드가 채워진 표준 가격 데이터
     *
     * @return 기본 PriceData
     */
    fun standard(): PriceData = PriceData(
        symbol = "AAPL",
        currency = "USD",
        exchange = "NMS",
        lastPrice = 150.0,
        regularMarketPrice = 150.0,
        regularMarketTime = 1704067200L, // 2024-01-01 00:00:00 UTC
        open = 148.0,
        dayHigh = 152.0,
        dayLow = 147.0,
        previousClose = 145.0,
        volume = 50_000_000L,
        regularMarketVolume = 50_000_000L,
        averageVolume = 45_000_000L,
        averageVolume10days = 48_000_000L,
        fiftyTwoWeekHigh = 200.0,
        fiftyTwoWeekLow = 100.0,
        fiftyTwoWeekChange = 50.0,
        fiftyTwoWeekChangePercent = 50.0,
        fiftyDayAverage = 140.0,
        twoHundredDayAverage = 130.0,
        marketCap = 2_500_000_000_000L,
        dividendYield = 0.5,
        dividendRate = 0.75,
        exDividendDate = 1704067200L,
        beta = 1.2,
        trailingPE = 28.5,
        forwardPE = 25.0
    )

    /**
     * 52주 최고가에 있는 PriceData 생성
     *
     * @return 52주 최고가 PriceData
     */
    fun atFiftyTwoWeekHigh(): PriceData = standard().copy(
        lastPrice = 200.0,
        regularMarketPrice = 200.0,
        fiftyTwoWeekHigh = 200.0,
        fiftyTwoWeekLow = 100.0
    )

    /**
     * 52주 최저가에 있는 PriceData 생성
     *
     * @return 52주 최저가 PriceData
     */
    fun atFiftyTwoWeekLow(): PriceData = standard().copy(
        lastPrice = 100.0,
        regularMarketPrice = 100.0,
        fiftyTwoWeekHigh = 200.0,
        fiftyTwoWeekLow = 100.0
    )

    /**
     * 52주 범위 중간에 있는 PriceData 생성
     *
     * @return 52주 범위 중간 PriceData
     */
    fun atFiftyTwoWeekMiddle(): PriceData = standard().copy(
        lastPrice = 150.0,
        regularMarketPrice = 150.0,
        fiftyTwoWeekHigh = 200.0,
        fiftyTwoWeekLow = 100.0
    )

    /**
     * 52주 범위가 동일한 PriceData 생성 (계산 불가 시나리오)
     *
     * @return 52주 범위 동일 PriceData
     */
    fun withSameFiftyTwoWeekRange(): PriceData = standard().copy(
        lastPrice = 150.0,
        regularMarketPrice = 150.0,
        fiftyTwoWeekHigh = 150.0,
        fiftyTwoWeekLow = 150.0
    )

    /**
     * 52주 데이터가 없는 PriceData 생성
     *
     * @return 52주 데이터 누락 PriceData
     */
    fun withoutFiftyTwoWeekData(): PriceData = standard().copy(
        fiftyTwoWeekHigh = null,
        fiftyTwoWeekLow = null
    )

    /**
     * 50일 이동평균선 위에 있는 PriceData 생성
     *
     * @return 50일 MA 위 PriceData
     */
    fun above50DayMA(): PriceData = standard().copy(
        lastPrice = 150.0,
        regularMarketPrice = 150.0,
        fiftyDayAverage = 140.0
    )

    /**
     * 50일 이동평균선 아래에 있는 PriceData 생성
     *
     * @return 50일 MA 아래 PriceData
     */
    fun below50DayMA(): PriceData = standard().copy(
        lastPrice = 130.0,
        regularMarketPrice = 130.0,
        fiftyDayAverage = 140.0
    )

    /**
     * 50일 이동평균 데이터가 없는 PriceData 생성
     *
     * @return 50일 MA 누락 PriceData
     */
    fun without50DayMA(): PriceData = standard().copy(
        fiftyDayAverage = null
    )

    /**
     * 200일 이동평균선 위에 있는 PriceData 생성
     *
     * @return 200일 MA 위 PriceData
     */
    fun above200DayMA(): PriceData = standard().copy(
        lastPrice = 150.0,
        regularMarketPrice = 150.0,
        twoHundredDayAverage = 130.0
    )

    /**
     * 200일 이동평균선 아래에 있는 PriceData 생성
     *
     * @return 200일 MA 아래 PriceData
     */
    fun below200DayMA(): PriceData = standard().copy(
        lastPrice = 120.0,
        regularMarketPrice = 120.0,
        twoHundredDayAverage = 130.0
    )

    /**
     * 200일 이동평균 데이터가 없는 PriceData 생성
     *
     * @return 200일 MA 누락 PriceData
     */
    fun without200DayMA(): PriceData = standard().copy(
        twoHundredDayAverage = null
    )

    /**
     * 상승한 PriceData 생성 (전일 대비 +5%)
     *
     * @return 상승 PriceData
     */
    fun withPositiveDailyChange(): PriceData = standard().copy(
        lastPrice = 150.0,
        regularMarketPrice = 150.0,
        previousClose = 142.857 // 약 +5% 상승
    )

    /**
     * 하락한 PriceData 생성 (전일 대비 -5%)
     *
     * @return 하락 PriceData
     */
    fun withNegativeDailyChange(): PriceData = standard().copy(
        lastPrice = 140.0,
        regularMarketPrice = 140.0,
        previousClose = 147.368 // 약 -5% 하락
    )

    /**
     * 변동이 없는 PriceData 생성 (전일 대비 0%)
     *
     * @return 보합 PriceData
     */
    fun withZeroDailyChange(): PriceData = standard().copy(
        lastPrice = 150.0,
        regularMarketPrice = 150.0,
        previousClose = 150.0
    )

    /**
     * 전일 종가가 0인 PriceData 생성 (계산 불가 시나리오)
     *
     * @return 전일 종가 0 PriceData
     */
    fun withZeroPreviousClose(): PriceData = standard().copy(
        lastPrice = 150.0,
        regularMarketPrice = 150.0,
        previousClose = 0.0
    )

    /**
     * 전일 종가 데이터가 없는 PriceData 생성
     *
     * @return 전일 종가 누락 PriceData
     */
    fun withoutPreviousClose(): PriceData = standard().copy(
        previousClose = null
    )

    /**
     * 가격 데이터가 없는 PriceData 생성
     *
     * @return 가격 데이터 누락 PriceData
     */
    fun withoutPriceData(): PriceData = standard().copy(
        lastPrice = null,
        regularMarketPrice = null
    )

    /**
     * 최소 필드만 있는 PriceData 생성 (필수 필드만)
     *
     * @return 최소 PriceData
     */
    fun minimal(): PriceData = PriceData(
        symbol = "AAPL",
        currency = null,
        exchange = null,
        lastPrice = null,
        regularMarketPrice = null,
        regularMarketTime = null,
        open = null,
        dayHigh = null,
        dayLow = null,
        previousClose = null,
        volume = null,
        regularMarketVolume = null,
        averageVolume = null,
        averageVolume10days = null,
        fiftyTwoWeekHigh = null,
        fiftyTwoWeekLow = null,
        fiftyTwoWeekChange = null,
        fiftyTwoWeekChangePercent = null,
        fiftyDayAverage = null,
        twoHundredDayAverage = null,
        marketCap = null,
        dividendYield = null,
        dividendRate = null,
        exDividendDate = null,
        beta = null,
        trailingPE = null,
        forwardPE = null
    )

    /**
     * 커스텀 PriceData를 생성하기 위한 빌더 반환
     *
     * @return PriceDataBuilder 인스턴스
     */
    fun builder(): PriceDataBuilder = PriceDataBuilder()

    /**
     * PriceData 빌더 클래스
     *
     * 테스트에 필요한 특정 필드만 변경하여 PriceData를 생성할 수 있습니다.
     *
     * ## 사용 예시
     * ```kotlin
     * val priceData = PriceDataFixtures.builder()
     *     .withSymbol("AAPL")
     *     .withLastPrice(150.0)
     *     .withPreviousClose(145.0)
     *     .build()
     * ```
     */
    class PriceDataBuilder {
        private var symbol: String = "AAPL"
        private var currency: String? = "USD"
        private var exchange: String? = "NMS"
        private var lastPrice: Double? = 150.0
        private var regularMarketPrice: Double? = 150.0
        private var regularMarketTime: Long? = 1704067200L
        private var open: Double? = 148.0
        private var dayHigh: Double? = 152.0
        private var dayLow: Double? = 147.0
        private var previousClose: Double? = 145.0
        private var volume: Long? = 50_000_000L
        private var regularMarketVolume: Long? = 50_000_000L
        private var averageVolume: Long? = 45_000_000L
        private var averageVolume10days: Long? = 48_000_000L
        private var fiftyTwoWeekHigh: Double? = 200.0
        private var fiftyTwoWeekLow: Double? = 100.0
        private var fiftyTwoWeekChange: Double? = 50.0
        private var fiftyTwoWeekChangePercent: Double? = 50.0
        private var fiftyDayAverage: Double? = 140.0
        private var twoHundredDayAverage: Double? = 130.0
        private var marketCap: Long? = 2_500_000_000_000L
        private var dividendYield: Double? = 0.5
        private var dividendRate: Double? = 0.75
        private var exDividendDate: Long? = 1704067200L
        private var beta: Double? = 1.2
        private var trailingPE: Double? = 28.5
        private var forwardPE: Double? = 25.0

        fun withSymbol(symbol: String) = apply { this.symbol = symbol }
        fun withCurrency(currency: String?) = apply { this.currency = currency }
        fun withExchange(exchange: String?) = apply { this.exchange = exchange }
        fun withLastPrice(lastPrice: Double?) = apply { this.lastPrice = lastPrice }
        fun withRegularMarketPrice(regularMarketPrice: Double?) =
            apply { this.regularMarketPrice = regularMarketPrice }
        fun withRegularMarketTime(regularMarketTime: Long?) =
            apply { this.regularMarketTime = regularMarketTime }
        fun withOpen(open: Double?) = apply { this.open = open }
        fun withDayHigh(dayHigh: Double?) = apply { this.dayHigh = dayHigh }
        fun withDayLow(dayLow: Double?) = apply { this.dayLow = dayLow }
        fun withPreviousClose(previousClose: Double?) = apply { this.previousClose = previousClose }
        fun withVolume(volume: Long?) = apply { this.volume = volume }
        fun withRegularMarketVolume(regularMarketVolume: Long?) =
            apply { this.regularMarketVolume = regularMarketVolume }
        fun withAverageVolume(averageVolume: Long?) = apply { this.averageVolume = averageVolume }
        fun withAverageVolume10days(averageVolume10days: Long?) =
            apply { this.averageVolume10days = averageVolume10days }
        fun withFiftyTwoWeekHigh(fiftyTwoWeekHigh: Double?) =
            apply { this.fiftyTwoWeekHigh = fiftyTwoWeekHigh }
        fun withFiftyTwoWeekLow(fiftyTwoWeekLow: Double?) =
            apply { this.fiftyTwoWeekLow = fiftyTwoWeekLow }
        fun withFiftyTwoWeekChange(fiftyTwoWeekChange: Double?) =
            apply { this.fiftyTwoWeekChange = fiftyTwoWeekChange }
        fun withFiftyTwoWeekChangePercent(fiftyTwoWeekChangePercent: Double?) =
            apply { this.fiftyTwoWeekChangePercent = fiftyTwoWeekChangePercent }
        fun withFiftyDayAverage(fiftyDayAverage: Double?) =
            apply { this.fiftyDayAverage = fiftyDayAverage }
        fun withTwoHundredDayAverage(twoHundredDayAverage: Double?) =
            apply { this.twoHundredDayAverage = twoHundredDayAverage }
        fun withMarketCap(marketCap: Long?) = apply { this.marketCap = marketCap }
        fun withDividendYield(dividendYield: Double?) = apply { this.dividendYield = dividendYield }
        fun withDividendRate(dividendRate: Double?) = apply { this.dividendRate = dividendRate }
        fun withExDividendDate(exDividendDate: Long?) = apply { this.exDividendDate = exDividendDate }
        fun withBeta(beta: Double?) = apply { this.beta = beta }
        fun withTrailingPE(trailingPE: Double?) = apply { this.trailingPE = trailingPE }
        fun withForwardPE(forwardPE: Double?) = apply { this.forwardPE = forwardPE }

        /**
         * 설정된 값으로 PriceData 인스턴스 생성
         *
         * @return PriceData 인스턴스
         */
        fun build(): PriceData = PriceData(
            symbol = symbol,
            currency = currency,
            exchange = exchange,
            lastPrice = lastPrice,
            regularMarketPrice = regularMarketPrice,
            regularMarketTime = regularMarketTime,
            open = open,
            dayHigh = dayHigh,
            dayLow = dayLow,
            previousClose = previousClose,
            volume = volume,
            regularMarketVolume = regularMarketVolume,
            averageVolume = averageVolume,
            averageVolume10days = averageVolume10days,
            fiftyTwoWeekHigh = fiftyTwoWeekHigh,
            fiftyTwoWeekLow = fiftyTwoWeekLow,
            fiftyTwoWeekChange = fiftyTwoWeekChange,
            fiftyTwoWeekChangePercent = fiftyTwoWeekChangePercent,
            fiftyDayAverage = fiftyDayAverage,
            twoHundredDayAverage = twoHundredDayAverage,
            marketCap = marketCap,
            dividendYield = dividendYield,
            dividendRate = dividendRate,
            exDividendDate = exDividendDate,
            beta = beta,
            trailingPE = trailingPE,
            forwardPE = forwardPE
        )
    }
}
