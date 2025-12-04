package com.ulalax.ufc.fixtures

import com.ulalax.ufc.domain.price.OHLCV
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * OHLCV 테스트 데이터 Fixture (Mother Pattern)
 *
 * OHLCV 도메인 모델의 다양한 시나리오를 위한 테스트 데이터를 제공합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val bullishCandle = OhlcvFixtures.bullish()
 * val bearishCandle = OhlcvFixtures.bearish()
 * val customCandle = OhlcvFixtures.builder()
 *     .withOpen(100.0)
 *     .withClose(110.0)
 *     .build()
 * ```
 */
object OhlcvFixtures {

    /**
     * 기본 타임스탬프 (2024-01-01 00:00:00 UTC)
     */
    private val DEFAULT_TIMESTAMP = LocalDate.of(2024, 1, 1)
        .atStartOfDay()
        .toEpochSecond(ZoneOffset.UTC)

    /**
     * 양봉 데이터 생성
     *
     * 시가보다 종가가 높은 상승 캔들
     *
     * @return 양봉 OHLCV
     */
    fun bullish(): OHLCV = OHLCV(
        timestamp = DEFAULT_TIMESTAMP,
        open = 100.0,
        high = 110.0,
        low = 95.0,
        close = 105.0,
        adjClose = 105.0,
        volume = 1_000_000L
    )

    /**
     * 음봉 데이터 생성
     *
     * 시가보다 종가가 낮은 하락 캔들
     *
     * @return 음봉 OHLCV
     */
    fun bearish(): OHLCV = OHLCV(
        timestamp = DEFAULT_TIMESTAMP,
        open = 100.0,
        high = 105.0,
        low = 90.0,
        close = 95.0,
        adjClose = 95.0,
        volume = 1_200_000L
    )

    /**
     * 보합 데이터 생성
     *
     * 시가와 종가가 동일한 횡보 캔들
     *
     * @return 보합 OHLCV
     */
    fun flat(): OHLCV = OHLCV(
        timestamp = DEFAULT_TIMESTAMP,
        open = 100.0,
        high = 102.0,
        low = 98.0,
        close = 100.0,
        adjClose = 100.0,
        volume = 800_000L
    )

    /**
     * 1주일 상승 추세 데이터 생성
     *
     * 7일간 연속 상승하는 패턴의 OHLCV 리스트
     * 매일 약 2% 상승
     *
     * @return 상승 추세 OHLCV 리스트 (7개)
     */
    fun uptrend(): List<OHLCV> {
        val basePrice = 100.0
        val dailyGrowth = 1.02 // 2% 상승

        return (0..6).map { day ->
            val dayPrice = basePrice * Math.pow(dailyGrowth, day.toDouble())
            val open = dayPrice
            val close = dayPrice * dailyGrowth
            val high = close * 1.01 // 종가보다 1% 높은 고가
            val low = open * 0.99 // 시가보다 1% 낮은 저가

            OHLCV(
                timestamp = DEFAULT_TIMESTAMP + (day * 86400L), // 하루씩 증가
                open = open,
                high = high,
                low = low,
                close = close,
                adjClose = close,
                volume = 1_000_000L + (day * 50_000L)
            )
        }
    }

    /**
     * 1주일 하락 추세 데이터 생성
     *
     * 7일간 연속 하락하는 패턴의 OHLCV 리스트
     * 매일 약 2% 하락
     *
     * @return 하락 추세 OHLCV 리스트 (7개)
     */
    fun downtrend(): List<OHLCV> {
        val basePrice = 100.0
        val dailyDecline = 0.98 // 2% 하락

        return (0..6).map { day ->
            val dayPrice = basePrice * Math.pow(dailyDecline, day.toDouble())
            val open = dayPrice
            val close = dayPrice * dailyDecline
            val high = open * 1.01 // 시가보다 1% 높은 고가
            val low = close * 0.99 // 종가보다 1% 낮은 저가

            OHLCV(
                timestamp = DEFAULT_TIMESTAMP + (day * 86400L), // 하루씩 증가
                open = open,
                high = high,
                low = low,
                close = close,
                adjClose = close,
                volume = 1_200_000L + (day * 100_000L)
            )
        }
    }

    /**
     * 1주일 횡보 데이터 생성
     *
     * 7일간 특정 가격 범위 내에서 움직이는 패턴의 OHLCV 리스트
     * 약 ±1% 내외의 변동
     *
     * @return 횡보 OHLCV 리스트 (7개)
     */
    fun sideways(): List<OHLCV> {
        val basePrice = 100.0
        val variations = listOf(0.0, 0.5, -0.3, 0.2, -0.5, 0.3, 0.0) // % 변동

        return variations.mapIndexed { index, variation ->
            val open = basePrice * (1 + variation / 100.0)
            val close = basePrice * (1 + (if (index % 2 == 0) 0.2 else -0.2) / 100.0)
            val high = maxOf(open, close) * 1.01
            val low = minOf(open, close) * 0.99

            OHLCV(
                timestamp = DEFAULT_TIMESTAMP + (index * 86400L), // 하루씩 증가
                open = open,
                high = high,
                low = low,
                close = close,
                adjClose = close,
                volume = 900_000L + (index * 10_000L)
            )
        }
    }

    /**
     * 커스텀 OHLCV 데이터를 생성하기 위한 빌더 반환
     *
     * @return OhlcvBuilder 인스턴스
     */
    fun builder(): OhlcvBuilder = OhlcvBuilder()

    /**
     * OHLCV 빌더 클래스
     *
     * 테스트에 필요한 특정 필드만 변경하여 OHLCV를 생성할 수 있습니다.
     *
     * ## 사용 예시
     * ```kotlin
     * val ohlcv = OhlcvFixtures.builder()
     *     .withOpen(100.0)
     *     .withClose(110.0)
     *     .withHigh(115.0)
     *     .withLow(98.0)
     *     .build()
     * ```
     */
    class OhlcvBuilder {
        private var timestamp: Long = DEFAULT_TIMESTAMP
        private var open: Double = 100.0
        private var high: Double = 110.0
        private var low: Double = 90.0
        private var close: Double = 100.0
        private var adjClose: Double? = 100.0
        private var volume: Long = 1_000_000L

        fun withTimestamp(timestamp: Long) = apply { this.timestamp = timestamp }

        fun withTimestamp(year: Int, month: Int, day: Int) = apply {
            this.timestamp = LocalDate.of(year, month, day)
                .atStartOfDay()
                .toEpochSecond(ZoneOffset.UTC)
        }

        fun withOpen(open: Double) = apply { this.open = open }

        fun withHigh(high: Double) = apply { this.high = high }

        fun withLow(low: Double) = apply { this.low = low }

        fun withClose(close: Double) = apply { this.close = close }

        fun withAdjClose(adjClose: Double?) = apply { this.adjClose = adjClose }

        fun withVolume(volume: Long) = apply { this.volume = volume }

        /**
         * 설정된 값으로 OHLCV 인스턴스 생성
         *
         * @return OHLCV 인스턴스
         * @throws IllegalStateException high < low 또는 close가 범위 밖일 때
         */
        fun build(): OHLCV {
            require(high >= low) {
                "고가($high)는 저가($low)보다 크거나 같아야 합니다."
            }
            require(open in low..high) {
                "시가($open)는 저가($low)와 고가($high) 사이에 있어야 합니다."
            }
            require(close in low..high) {
                "종가($close)는 저가($low)와 고가($high) 사이에 있어야 합니다."
            }

            return OHLCV(
                timestamp = timestamp,
                open = open,
                high = high,
                low = low,
                close = close,
                adjClose = adjClose,
                volume = volume
            )
        }
    }
}
