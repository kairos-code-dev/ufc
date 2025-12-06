package com.ulalax.ufc.unit.utils

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Tag

/**
 * Unit 테스트 베이스 클래스
 *
 * Mock 데이터를 사용하는 빠른 Unit 테스트를 위한 공통 설정을 제공합니다.
 *
 * ## 특징
 * - **Mock 데이터 사용**: 실제 API 호출 없이 레코딩된 JSON 응답 사용
 * - **빠른 실행**: 네트워크 호출이 없어 빠르게 실행됩니다.
 * - **격리된 테스트**: 외부 의존성 없이 독립적으로 실행됩니다.
 * - **JSON 응답 로더**: 테스트 리소스에서 JSON 파일을 쉽게 로드할 수 있습니다.
 *
 * ## 사용 예제
 * ```kotlin
 * @DisplayName("Yahoo Response Parsing - Unit Test")
 * class YahooResponseParsingTest : UnitTestBase() {
 *
 *     @Test
 *     @DisplayName("PRICE 모듈 JSON을 파싱할 수 있다")
 *     fun `can parse PRICE module JSON`() = unitTest {
 *         // Given
 *         val json = loadMockResponse(RecordingConfig.Paths.Yahoo.QUOTE_SUMMARY, "aapl_price")
 *         val gson = Gson()
 *
 *         // When
 *         val response = gson.fromJson(json, QuoteSummaryResponse::class.java)
 *
 *         // Then
 *         assertThat(response).isNotNull()
 *         assertThat(response.quoteSummary).isNotNull()
 *     }
 * }
 * ```
 *
 * ## Mock 데이터 준비
 * 1. Integration 테스트를 실행하여 실제 API 응답을 레코딩합니다.
 * 2. 레코딩된 JSON 파일은 `src/test/resources/responses/` 디렉토리에 저장됩니다.
 * 3. Unit 테스트에서 `loadMockResponse()`로 JSON을 로드하여 사용합니다.
 *
 * @see JsonResponseLoader
 * @see IntegrationTestBase
 */
@Tag("unit")
abstract class UnitTestBase {
    /**
     * Unit 테스트 실행 헬퍼 함수
     *
     * 코루틴 환경에서 테스트를 실행합니다.
     * Integration 테스트와 달리 기본 타임아웃이 짧습니다.
     *
     * @param block 실행할 테스트 코드 블록
     *
     * ## 사용 예제
     * ```kotlin
     * @Test
     * fun `test JSON parsing`() = unitTest {
     *     val json = loadMockResponse("yahoo/quote_summary", "aapl_price")
     *     val data = parseJson(json)
     *     assertThat(data).isNotNull()
     * }
     * ```
     */
    protected fun unitTest(block: suspend () -> Unit) = runTest { block() }

    /**
     * 테스트 리소스에서 Mock JSON 응답을 로드합니다.
     *
     * @param category API 카테고리 (RecordingConfig.Paths 사용)
     * @param fileName 파일명 (확장자 제외)
     * @return JSON 문자열
     * @throws IllegalStateException JSON 파일을 찾을 수 없는 경우
     *
     * ## 사용 예제
     * ```kotlin
     * // Yahoo QuoteSummary 응답 로드
     * val json = loadMockResponse(RecordingConfig.Paths.Yahoo.QUOTE_SUMMARY, "aapl_price")
     *
     * // FRED Series 응답 로드
     * val json = loadMockResponse(RecordingConfig.Paths.Fred.SERIES, "gdp_series")
     *
     * // Business Insider ISIN 검색 응답 로드
     * val json = loadMockResponse(RecordingConfig.Paths.BusinessInsider.ISIN_SEARCH, "apple_isin")
     * ```
     */
    protected fun loadMockResponse(
        category: String,
        fileName: String,
    ): String = JsonResponseLoader.load(category, fileName)
}
