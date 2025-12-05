package com.ulalax.ufc.unit.utils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 테스트 리소스에서 JSON 파일을 로드하는 유틸리티
 *
 * Integration 테스트에서 레코딩된 API 응답 JSON 파일을
 * Unit 테스트에서 Mock 데이터로 사용하기 위해 로드합니다.
 *
 * ## JSON 파일 위치
 * - 기본 경로: `src/test/resources/responses/`
 * - 카테고리별 하위 디렉토리:
 *   - Yahoo: `yahoo/quote_summary/`, `yahoo/chart/`
 *   - FRED: `fred/series/`
 *   - Business Insider: `businessinsider/isin/`
 *
 * ## 사용 예제
 * ```kotlin
 * @Test
 * fun `parse AAPL price module JSON`() = unitTest {
 *     // Given
 *     val json = JsonResponseLoader.load(
 *         category = RecordingConfig.Paths.Yahoo.QUOTE_SUMMARY,
 *         fileName = "aapl_price"
 *     )
 *
 *     // When
 *     val response = Gson().fromJson(json, QuoteSummaryResponse::class.java)
 *
 *     // Then
 *     assertThat(response).isNotNull()
 * }
 * ```
 *
 * ## 에러 처리
 * - 파일을 찾을 수 없으면 `IllegalStateException` 예외를 발생시킵니다.
 * - 파일 읽기 실패 시 상세한 에러 메시지를 제공합니다.
 *
 * @see ResponseRecorder JSON 파일을 생성하는 레코더
 * @see RecordingConfig 레코딩 경로 설정
 */
object JsonResponseLoader {

    /**
     * 테스트 리소스의 기본 경로
     */
    private val baseResourcePath: Path = Paths.get("src/test/resources/responses")

    /**
     * JSON 파일을 로드하여 문자열로 반환합니다.
     *
     * @param category API 카테고리 경로 (예: "yahoo/quote_summary")
     * @param fileName 파일명 (확장자 제외)
     * @return JSON 문자열
     * @throws IllegalStateException JSON 파일을 찾을 수 없거나 읽을 수 없는 경우
     *
     * ## 사용 예제
     * ```kotlin
     * // Yahoo QuoteSummary 응답 로드
     * val json = JsonResponseLoader.load("yahoo/quote_summary", "aapl_price")
     *
     * // FRED Series 응답 로드
     * val json = JsonResponseLoader.load("fred/series", "gdp_series")
     *
     * // Business Insider ISIN 검색 응답 로드
     * val json = JsonResponseLoader.load("businessinsider/isin", "apple_isin")
     * ```
     */
    fun load(category: String, fileName: String): String {
        val filePath = baseResourcePath.resolve(category).resolve("$fileName.json")

        if (!Files.exists(filePath)) {
            throw IllegalStateException(
                """
                Mock JSON 파일을 찾을 수 없습니다.
                경로: $filePath

                해결 방법:
                1. Integration 테스트를 먼저 실행하여 API 응답을 레코딩하세요.
                   ./gradlew test --tests *IntegrationTest -Precord.responses=true

                2. 파일명과 카테고리가 정확한지 확인하세요.
                   카테고리: $category
                   파일명: $fileName.json

                3. 레코딩된 파일 목록을 확인하세요.
                   ls -la src/test/resources/responses/$category/
                """.trimIndent()
            )
        }

        return try {
            Files.readString(filePath)
        } catch (e: Exception) {
            throw IllegalStateException(
                """
                JSON 파일을 읽을 수 없습니다.
                경로: $filePath
                에러: ${e.message}
                """.trimIndent(),
                e
            )
        }
    }

    /**
     * 지정된 카테고리의 모든 JSON 파일 목록을 반환합니다.
     *
     * @param category API 카테고리 경로
     * @return JSON 파일명 목록 (확장자 제외)
     *
     * ## 사용 예제
     * ```kotlin
     * // Yahoo QuoteSummary 카테고리의 모든 파일 목록
     * val files = JsonResponseLoader.listFiles("yahoo/quote_summary")
     * println("Available files: $files")
     * // 출력: Available files: [aapl_price, msft_price, googl_price]
     * ```
     */
    fun listFiles(category: String): List<String> {
        val categoryPath = baseResourcePath.resolve(category)

        if (!Files.exists(categoryPath)) {
            return emptyList()
        }

        return Files.list(categoryPath).use { stream ->
            stream
                .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
                .map { it.fileName.toString().removeSuffix(".json") }
                .sorted()
                .toList()
        }
    }

    /**
     * 지정된 카테고리에 JSON 파일이 존재하는지 확인합니다.
     *
     * @param category API 카테고리 경로
     * @param fileName 파일명 (확장자 제외)
     * @return 파일 존재 여부
     *
     * ## 사용 예제
     * ```kotlin
     * if (JsonResponseLoader.exists("yahoo/quote_summary", "aapl_price")) {
     *     val json = JsonResponseLoader.load("yahoo/quote_summary", "aapl_price")
     *     // JSON 사용
     * } else {
     *     println("Mock 파일이 없습니다. Integration 테스트를 먼저 실행하세요.")
     * }
     * ```
     */
    fun exists(category: String, fileName: String): Boolean {
        val filePath = baseResourcePath.resolve(category).resolve("$fileName.json")
        return Files.exists(filePath)
    }

    /**
     * 기본 리소스 경로를 반환합니다.
     *
     * 테스트 환경 검증이나 디버깅 목적으로 사용됩니다.
     *
     * @return 기본 리소스 경로
     */
    fun getBasePath(): Path = baseResourcePath
}
