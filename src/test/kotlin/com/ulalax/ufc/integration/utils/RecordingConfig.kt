package com.ulalax.ufc.integration.utils

import java.nio.file.Path
import java.nio.file.Paths

/**
 * 레코딩 설정 및 경로 관리
 *
 * ResponseRecorder가 API 응답을 저장할 때 사용하는 설정입니다.
 * - 저장 경로: src/test/resources/responses/
 * - 레코딩은 integrationTest(category, fileName) { } 함수 시그니처를 통해 제어됩니다.
 *
 * 구조:
 * - Yahoo: Yahoo Finance API
 * - Fred: FRED 경제 데이터 API
 * - BusinessInsider: Business Insider ISIN 검색 API
 *
 * ## 사용 예제
 * ```kotlin
 * // 레코딩 활성화 (category, fileName 파라미터 제공)
 * integrationTest("yahoo/quote_summary", "aapl_price") {
 *     val result = ufc.yahoo.quoteSummary("AAPL", QuoteSummaryModule.PRICE)
 *     // API raw 응답이 자동으로 레코딩됨
 * }
 *
 * // 레코딩 비활성화 (category, fileName 없이 호출)
 * integrationTest {
 *     val result = ufc.yahoo.quoteSummary("AAPL", QuoteSummaryModule.PRICE)
 *     // 레코딩 없음
 * }
 * ```
 */
object RecordingConfig {
    /**
     * 레코딩 파일 저장 경로
     * Test 리소스 경로로 저장됩니다.
     */
    val baseOutputPath: Path
        get() =
            java.nio.file.Paths
                .get("src/test/resources/responses")

    /**
     * API 소스별 레코딩 경로
     *
     * 구조:
     * - Yahoo: Yahoo Finance API
     *   - QuoteSummary: 주식 요약 정보
     *   - Chart: 차트 데이터
     * - Fred: FRED 경제 데이터 API
     *   - Series: 시계열 데이터
     * - BusinessInsider: Business Insider API
     *   - IsinSearch: ISIN 검색
     */
    object Paths {
        // ========================================
        // Yahoo Finance API 네임스페이스
        // ========================================

        /**
         * Yahoo Finance API 레코딩 경로
         */
        object Yahoo {
            const val BASE = "yahoo"

            /** QuoteSummary API 응답 (주식 요약 정보) */
            const val QUOTE_SUMMARY = "$BASE/quote_summary"

            /** Chart API 응답 (차트 데이터) */
            const val CHART = "$BASE/chart"

            /** Market API 응답 (시장 정보) */
            const val MARKET = "$BASE/market"

            /** Search API 응답 (종목 및 뉴스 검색) */
            const val SEARCH = "$BASE/search"

            /** Options API 응답 (옵션 체인 데이터) */
            const val OPTIONS = "$BASE/options"
        }

        // ========================================
        // FRED API 네임스페이스
        // ========================================

        /**
         * FRED API 레코딩 경로
         */
        object Fred {
            const val BASE = "fred"

            /** Series API 응답 (경제 시계열 데이터) */
            const val SERIES = "$BASE/series"
        }

        // ========================================
        // Business Insider API 네임스페이스
        // ========================================

        /**
         * Business Insider API 레코딩 경로
         */
        object BusinessInsider {
            const val BASE = "businessinsider"

            /** ISIN 검색 API 응답 */
            const val ISIN_SEARCH = "$BASE/isin"
        }

        // ========================================
        // 공통
        // ========================================

        /**
         * 에러 응답 저장 경로
         */
        const val ERRORS = "errors"
    }
}
