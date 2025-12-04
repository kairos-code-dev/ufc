package com.ulalax.ufc.fixtures

import com.ulalax.ufc.domain.corp.Split
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Split 테스트 데이터 Fixture (Mother Pattern)
 *
 * Split 도메인 모델의 다양한 시나리오를 위한 테스트 데이터를 제공합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val split2For1 = SplitFixtures.twoForOne()
 * val split4For1 = SplitFixtures.fourForOne()
 * val customSplit = SplitFixtures.builder()
 *     .withNumerator(3)
 *     .withDenominator(1)
 *     .build()
 * ```
 */
object SplitFixtures {

    /**
     * 기본 타임스탬프 (2024-01-01 00:00:00 UTC)
     */
    private val DEFAULT_TIMESTAMP = LocalDate.of(2024, 1, 1)
        .atStartOfDay()
        .toEpochSecond(ZoneOffset.UTC)

    /**
     * 2:1 주식 분할 생성
     *
     * 가장 일반적인 주식 분할 비율
     * 1주가 2주로 분할됨
     *
     * @return 2:1 Split
     */
    fun twoForOne(): Split = Split(
        date = DEFAULT_TIMESTAMP,
        numerator = 2,
        denominator = 1,
        ratio = 2.0
    )

    /**
     * 3:1 주식 분할 생성
     *
     * 1주가 3주로 분할됨
     *
     * @return 3:1 Split
     */
    fun threeForOne(): Split = Split(
        date = DEFAULT_TIMESTAMP,
        numerator = 3,
        denominator = 1,
        ratio = 3.0
    )

    /**
     * 4:1 주식 분할 생성
     *
     * 대규모 주식 분할
     * 1주가 4주로 분할됨
     *
     * @return 4:1 Split
     */
    fun fourForOne(): Split = Split(
        date = DEFAULT_TIMESTAMP,
        numerator = 4,
        denominator = 1,
        ratio = 4.0
    )

    /**
     * 1:2 역분할 생성
     *
     * 주가가 너무 낮을 때 사용
     * 2주가 1주로 합쳐짐
     *
     * @return 1:2 Reverse Split
     */
    fun oneForTwo(): Split = Split(
        date = DEFAULT_TIMESTAMP,
        numerator = 1,
        denominator = 2,
        ratio = 0.5
    )

    /**
     * 1:5 역분할 생성
     *
     * 대규모 역분할
     * 5주가 1주로 합쳐짐
     *
     * @return 1:5 Reverse Split
     */
    fun oneForFive(): Split = Split(
        date = DEFAULT_TIMESTAMP,
        numerator = 1,
        denominator = 5,
        ratio = 0.2
    )

    /**
     * 7:1 주식 분할 생성
     *
     * 매우 대규모 주식 분할 (예: TSLA 2020년)
     * 1주가 7주로 분할됨
     *
     * @return 7:1 Split
     */
    fun sevenForOne(): Split = Split(
        date = DEFAULT_TIMESTAMP,
        numerator = 7,
        denominator = 1,
        ratio = 7.0
    )

    /**
     * 3:2 주식 분할 생성
     *
     * 비표준 분할 비율
     * 2주가 3주로 분할됨
     *
     * @return 3:2 Split
     */
    fun threeForTwo(): Split = Split(
        date = DEFAULT_TIMESTAMP,
        numerator = 3,
        denominator = 2,
        ratio = 1.5
    )

    /**
     * 커스텀 Split 데이터를 생성하기 위한 빌더 반환
     *
     * @return SplitBuilder 인스턴스
     */
    fun builder(): SplitBuilder = SplitBuilder()

    /**
     * Split 빌더 클래스
     *
     * 테스트에 필요한 특정 필드만 변경하여 Split을 생성할 수 있습니다.
     *
     * ## 사용 예시
     * ```kotlin
     * val split = SplitFixtures.builder()
     *     .withNumerator(5)
     *     .withDenominator(1)
     *     .withDate(2023, 6, 15)
     *     .build()
     * ```
     */
    class SplitBuilder {
        private var date: Long = DEFAULT_TIMESTAMP
        private var numerator: Int = 2
        private var denominator: Int = 1

        fun withDate(date: Long) = apply { this.date = date }

        fun withDate(year: Int, month: Int, day: Int) = apply {
            this.date = LocalDate.of(year, month, day)
                .atStartOfDay()
                .toEpochSecond(ZoneOffset.UTC)
        }

        fun withNumerator(numerator: Int) = apply { this.numerator = numerator }

        fun withDenominator(denominator: Int) = apply { this.denominator = denominator }

        /**
         * 설정된 값으로 Split 인스턴스 생성
         *
         * @return Split 인스턴스
         * @throws IllegalArgumentException numerator 또는 denominator가 0 이하일 때
         */
        fun build(): Split {
            require(numerator > 0) {
                "분자(numerator)는 0보다 커야 합니다. 실제값: $numerator"
            }
            require(denominator > 0) {
                "분모(denominator)는 0보다 커야 합니다. 실제값: $denominator"
            }

            val ratio = numerator.toDouble() / denominator.toDouble()

            return Split(
                date = date,
                numerator = numerator,
                denominator = denominator,
                ratio = ratio
            )
        }
    }
}
