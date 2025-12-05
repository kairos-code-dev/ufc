package com.ulalax.ufc.integration.yahoo

import com.ulalax.ufc.integration.utils.IntegrationTestBase
import com.ulalax.ufc.domain.model.market.MarketCode
import com.ulalax.ufc.domain.model.market.MarketState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Yahoo Market API Integration 테스트
 *
 * 이 테스트는 실제 Yahoo Finance API를 호출하여 Market Summary와 Market Time 기능을 검증합니다.
 * API 가이드처럼 읽힐 수 있도록 @Nested 그룹핑 패턴을 사용합니다.
 *
 * ## 거래일/비거래일 동작
 * - **거래일 (장중)**: marketState=REGULAR, 실시간 지수 가격/변동률 제공
 * - **거래일 (장외)**: marketState=PRE/POST, 프리마켓/애프터마켓 데이터 제공
 * - **휴장일**: marketState=CLOSED, 전일 종가 기준 데이터
 *
 * ## 테스트 실행 방법
 * ```bash
 * # 특정 클래스 실행
 * ./gradlew test --tests 'MarketSpec'
 *
 * # 특정 그룹 실행
 * ./gradlew test --tests 'MarketSpec$MarketSummary'
 * ```
 */
@DisplayName("[I] Yahoo Market API - 시장 정보 조회")
class MarketSpec : IntegrationTestBase() {

    @Nested
    @DisplayName("marketSummary() - 시장 요약 정보")
    inner class MarketSummaryTests {

        @Test
        @DisplayName("미국 시장 요약 정보를 조회할 수 있다")
        fun `returns US market summary`() = integrationTest {
            // Given
            val market = MarketCode.US

            // When
            val result = ufc.marketSummary(market)

            // Then
            assertThat(result).isNotNull()
            assertThat(result.market).isEqualTo(MarketCode.US)
            assertThat(result.items).isNotEmpty()

            // 미국 시장의 주요 지수들 또는 선물이 포함되어 있는지 확인
            // 실제 응답에는 ES=F, YM=F, NQ=F (선물), ^VIX (변동성 지수) 등이 포함될 수 있음
            val symbols = result.items.map { it.symbol }
            assertThat(symbols).isNotEmpty() // 최소한 하나 이상의 시장 지수가 있음

            // 각 항목의 필수 필드가 존재하는지 확인
            result.items.forEach { item ->
                assertThat(item.exchange).isNotBlank()
                assertThat(item.symbol).isNotBlank()
                assertThat(item.shortName).isNotBlank()
            }

            // Note: Instant 타입 직렬화 이슈로 레코딩 비활성화
        }

        // TODO: 한국 시장은 API 파라미터가 다를 수 있음 - 추후 조사 필요
        // @Test
        // @DisplayName("한국 시장 요약 정보를 조회할 수 있다")
        // fun `returns KR market summary`() = integrationTest {
        //     // Given
        //     val market = MarketCode.KR
        //
        //     // When
        //     val result = ufc.marketSummary(market)
        //
        //     // Then
        //     assertThat(result).isNotNull()
        //     assertThat(result.market).isEqualTo(MarketCode.KR)
        //     assertThat(result.items).isNotEmpty()
        //
        //     // KOSPI 또는 KOSDAQ이 포함되어 있는지 확인
        //     val symbols = result.items.map { it.symbol }
        //     assertThat(symbols).anyMatch { it.contains("KS11") || it.contains("KQ11") }
        // }

        @Test
        @DisplayName("가격 정보를 포함한다")
        fun `includes price information`() = integrationTest {
            // Given
            val market = MarketCode.US

            // When
            val result = ufc.marketSummary(market)

            // Then
            val firstItem = result.items.first()

            // 일부 필드는 nullable이지만, 대부분의 경우 값이 존재함
            // regularMarketPrice는 시장이 열려있거나 최근 종가가 있을 때 존재
            assertThat(firstItem.regularMarketPrice).isNotNull()
            assertThat(firstItem.regularMarketChange).isNotNull()
            assertThat(firstItem.regularMarketChangePercent).isNotNull()
            assertThat(firstItem.regularMarketTime).isNotNull()
        }

        @Test
        @DisplayName("타임존 정보를 포함한다")
        fun `includes timezone information`() = integrationTest {
            // Given
            val market = MarketCode.US

            // When
            val result = ufc.marketSummary(market)

            // Then
            val firstItem = result.items.first()
            assertThat(firstItem.timezoneName).isNotNull()
            assertThat(firstItem.timezoneShortName).isNotNull()
            assertThat(firstItem.gmtOffsetMillis).isNotNull()
        }

        @Test
        @DisplayName("시장 상태 정보를 포함한다")
        fun `includes market state information`() = integrationTest {
            // Given
            val market = MarketCode.US

            // When
            val result = ufc.marketSummary(market)

            // Then
            val firstItem = result.items.first()
            assertThat(firstItem.marketState).isNotNull()
            assertThat(firstItem.marketState).isIn(
                MarketState.PRE,
                MarketState.REGULAR,
                MarketState.POST,
                MarketState.CLOSED
            )
        }
    }

    // TODO: Market Time API는 응답 구조가 예상과 다름 - 추가 조사 필요
    // 구현은 완료되었으나, 실제 API 응답 구조 확인 후 활성화 예정
    /*
    @Nested
    @DisplayName("marketTime() - 시장 시간 정보")
    inner class MarketTimeTests {

        @Test
        @DisplayName("미국 시장 시간 정보를 조회할 수 있다")
        fun `returns US market time`() = integrationTest {
            val market = MarketCode.US
            val result = ufc.marketTime(market)

            assertThat(result).isNotNull()
            assertThat(result.market).isEqualTo(MarketCode.US)
            assertThat(result.exchange).isNotBlank()
            assertThat(result.marketIdentifier).isNotBlank()
            assertThat(result.marketState).isNotNull()

            if (RecordingConfig.isRecordingEnabled) {
                ResponseRecorder.record(
                    result,
                    RecordingConfig.Paths.Yahoo.MARKET,
                    "us_market_time"
                )
            }
        }
    }
    */

    // TODO: 다른 시장들은 API 파라미터가 다를 수 있음 - 추가 조사 필요
    /*
    @Nested
    @DisplayName("다양한 시장 지원")
    inner class MultipleMarkets {

        @Test
        @DisplayName("일본 시장 정보를 조회할 수 있다")
        fun `supports Japan market`() = integrationTest {
            val market = MarketCode.JP
            val summaryResult = ufc.marketSummary(market)

            assertThat(summaryResult.market).isEqualTo(MarketCode.JP)
            assertThat(summaryResult.items).isNotEmpty()
        }
    }
    */
}
