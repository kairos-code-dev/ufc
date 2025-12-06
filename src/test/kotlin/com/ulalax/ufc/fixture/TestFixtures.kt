package com.ulalax.ufc.fixture

import java.time.LocalDate

/**
 * 중앙 집중식 테스트 데이터 관리
 *
 * 모든 테스트에서 사용할 상수와 픽스처를 제공합니다.
 * 테스트 데이터를 한 곳에서 관리하여 일관성과 유지보수성을 향상시킵니다.
 *
 * ## 사용 예제
 * ```kotlin
 * @Test
 * fun `should fetch quote summary for Apple`() = integrationTest {
 *     val result = ufc.yahoo.quoteSummary(TestFixtures.Symbols.AAPL, QuoteSummaryModule.PRICE)
 *     assertThat(result.hasModule(QuoteSummaryModule.PRICE)).isTrue()
 * }
 * ```
 */
object TestFixtures {
    /**
     * Yahoo Finance 티커 심볼
     */
    object Symbols {
        /** Apple Inc. */
        const val AAPL = "AAPL"

        /** Microsoft Corporation */
        const val MSFT = "MSFT"

        /** Alphabet Inc. (Google) Class A */
        const val GOOGL = "GOOGL"

        /** Amazon.com Inc. */
        const val AMZN = "AMZN"

        /** Tesla Inc. */
        const val TSLA = "TSLA"

        /** NVIDIA Corporation */
        const val NVDA = "NVDA"

        /** Meta Platforms Inc. (Facebook) */
        const val META = "META"

        /** Bitcoin USD */
        const val BTC_USD = "BTC-USD"

        /** S&P 500 Index */
        const val SPY = "SPY"

        /** 존재하지 않는 심볼 (에러 테스트용) */
        const val INVALID = "INVALID_SYMBOL_12345"
    }

    /**
     * ISIN (International Securities Identification Number)
     */
    object Isin {
        /** Apple Inc. */
        const val APPLE = "US0378331005"

        /** Microsoft Corporation */
        const val MICROSOFT = "US5949181045"

        /** Samsung Electronics */
        const val SAMSUNG = "KR7005930003"

        /** Toyota Motor Corporation */
        const val TOYOTA = "JP3633400001"

        /** 존재하지 않는 ISIN (에러 테스트용) */
        const val INVALID = "XX0000000000"
    }

    /**
     * FRED 경제 데이터 시리즈 ID
     */
    object FredSeries {
        /** Gross Domestic Product (GDP) */
        const val GDP = "GDP"

        /** Unemployment Rate */
        const val UNEMPLOYMENT = "UNRATE"

        /** Consumer Price Index for All Urban Consumers */
        const val CPI = "CPIAUCSL"

        /** Federal Funds Effective Rate */
        const val FED_FUNDS_RATE = "FEDFUNDS"

        /** 10-Year Treasury Constant Maturity Rate */
        const val TREASURY_10Y = "DGS10"

        /** S&P 500 Index */
        const val SP500 = "SP500"

        /** 존재하지 않는 시리즈 (에러 테스트용) */
        const val INVALID = "INVALID_SERIES_12345"
    }

    /**
     * 테스트용 날짜 상수
     */
    object Dates {
        /** 특정 거래일 (2024-11-25) */
        val TRADING_DAY: LocalDate = LocalDate.of(2024, 11, 25)

        /** 1년 전 */
        val ONE_YEAR_AGO: LocalDate = LocalDate.now().minusYears(1)

        /** 1개월 전 */
        val ONE_MONTH_AGO: LocalDate = LocalDate.now().minusMonths(1)

        /** 1주일 전 */
        val ONE_WEEK_AGO: LocalDate = LocalDate.now().minusWeeks(1)

        /** 어제 */
        val YESTERDAY: LocalDate = LocalDate.now().minusDays(1)

        /** 오늘 */
        val TODAY: LocalDate = LocalDate.now()

        /** 미래 날짜 (에러 테스트용) */
        val FUTURE_DATE: LocalDate = LocalDate.now().plusYears(1)
    }

    /**
     * HTTP 상태 코드 및 에러 케이스
     */
    object ErrorCases {
        /** Rate Limit 초과 시뮬레이션용 반복 횟수 */
        const val RATE_LIMIT_TEST_ITERATIONS = 100

        /** Timeout 테스트용 긴 대기 시간 (밀리초) */
        const val LONG_TIMEOUT_MS = 60_000L

        /** Retry 테스트용 최대 재시도 횟수 */
        const val MAX_RETRY_COUNT = 3
    }

    /**
     * 일반적인 테스트 설정
     */
    object Config {
        /** 기본 테스트 타임아웃 (초) */
        const val DEFAULT_TEST_TIMEOUT_SECONDS = 30L

        /** Integration 테스트 타임아웃 (초) */
        const val INTEGRATION_TEST_TIMEOUT_SECONDS = 60L

        /** 빠른 테스트 타임아웃 (초) */
        const val FAST_TEST_TIMEOUT_SECONDS = 5L
    }
}
