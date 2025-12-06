package com.ulalax.ufc.integration.utils

import com.ulalax.ufc.api.Ufc
import com.ulalax.ufc.api.UfcConfig
import com.ulalax.ufc.infrastructure.common.ratelimit.GlobalRateLimiters
import com.ulalax.ufc.infrastructure.common.recording.ResponseRecordingContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.util.Properties
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Integration 테스트 베이스 클래스
 *
 * 실제 API를 호출하는 Integration 테스트를 위한 공통 설정을 제공합니다.
 *
 * ## 특징
 * - **실제 API 호출**: Yahoo Finance, FRED, Business Insider API를 실제로 호출합니다.
 * - **Ufc 클라이언트 자동 설정**: `@BeforeAll`에서 Ufc 클라이언트를 초기화합니다.
 * - **Rate Limiter 자동 리셋**: `@AfterAll`에서 GlobalRateLimiters를 초기화합니다.
 * - **FRED API Key 지원**: 환경변수에서 FRED_API_KEY를 읽어 자동 설정합니다.
 *
 * ## 사용 예제
 * ```kotlin
 * @DisplayName("Yahoo.quoteSummary() - 주식 요약 정보 조회")
 * class QuoteSummaryIntegrationTest : IntegrationTestBase() {
 *
 *     @Test
 *     @DisplayName("AAPL의 PRICE 모듈을 조회할 수 있다")
 *     fun `returns price module for AAPL`() = integrationTest {
 *         // Given
 *         val symbol = TestFixtures.Symbols.AAPL
 *
 *         // When
 *         val result = ufc.yahoo.quoteSummary(symbol, QuoteSummaryModule.PRICE)
 *
 *         // Then
 *         assertThat(result.hasModule(QuoteSummaryModule.PRICE)).isTrue()
 *     }
 * }
 * ```
 *
 * ## 환경 설정
 * - FRED API 사용 시 환경변수에 `FRED_API_KEY`를 설정하세요.
 * - Rate Limiting을 비활성화하려면 `UfcConfig`를 override하여 설정하세요.
 *
 * @see Ufc
 * @see UfcConfig
 * @see GlobalRateLimiters
 */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class IntegrationTestBase {
    /**
     * UFC 통합 클라이언트
     *
     * 모든 Integration 테스트에서 이 클라이언트를 사용하여 API를 호출합니다.
     */
    protected lateinit var ufc: Ufc

    /**
     * 테스트 클래스 시작 시 Ufc 클라이언트 초기화
     *
     * FRED_API_KEY를 다음 순서로 찾습니다:
     * 1. 환경변수
     * 2. local.properties 파일
     */
    @BeforeAll
    fun setUp() =
        runBlocking {
            val fredApiKey = System.getenv("FRED_API_KEY") ?: loadFromLocalProperties("FRED_API_KEY")
            ufc =
                if (fredApiKey != null) {
                    Ufc.create(UfcConfig(fredApiKey = fredApiKey))
                } else {
                    Ufc.create()
                }
        }

    /**
     * local.properties 파일에서 값을 읽습니다.
     */
    private fun loadFromLocalProperties(key: String): String? {
        val localPropertiesFile = File("local.properties")
        if (!localPropertiesFile.exists()) return null

        return try {
            val properties = Properties()
            localPropertiesFile.inputStream().use { properties.load(it) }
            properties.getProperty(key)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 테스트 클래스 종료 시 리소스 정리
     *
     * - Ufc 클라이언트 종료
     * - GlobalRateLimiters 초기화 (테스트 간 독립성 보장)
     */
    @AfterAll
    fun tearDown() {
        ufc.close()
        GlobalRateLimiters.resetForTesting()
    }

    /**
     * Integration 테스트 실행 헬퍼 함수 (레코딩 없음)
     *
     * 코루틴 환경에서 테스트를 실행하며, 기본 타임아웃을 30초로 설정합니다.
     * API 응답을 레코딩하지 않습니다.
     *
     * @param timeout 테스트 타임아웃 (기본값: 30초)
     * @param block 실행할 테스트 코드 블록
     *
     * ## 사용 예제
     * ```kotlin
     * @Test
     * fun `test API call`() = integrationTest {
     *     val result = ufc.yahoo.quoteSummary("AAPL", QuoteSummaryModule.PRICE)
     *     assertThat(result).isNotNull()
     * }
     *
     * @Test
     * fun `test with custom timeout`() = integrationTest(timeout = 60.seconds) {
     *     val result = ufc.fred.series("GDP")
     *     assertThat(result).isNotNull()
     * }
     * ```
     */
    protected fun integrationTest(
        timeout: Duration = 30.seconds,
        block: suspend () -> Unit,
    ) = runTest(timeout = timeout) { block() }

    /**
     * Integration 테스트 실행 헬퍼 함수 (레코딩 활성화)
     *
     * 코루틴 환경에서 테스트를 실행하며, API 응답을 파일로 레코딩합니다.
     * 레코딩 목적: API가 반환하는 raw 데이터의 형식을 이해하기 위함
     * 정상 동작하는 케이스만 레코딩해야 합니다.
     *
     * @param category 레코딩 카테고리 (예: "yahoo/quote_summary", "fred/series")
     * @param fileName 저장할 파일 이름 (예: "aapl_price")
     * @param timeout 테스트 타임아웃 (기본값: 30초)
     * @param block 실행할 테스트 코드 블록
     *
     * ## 사용 예제
     * ```kotlin
     * @Test
     * fun `returns price module for AAPL`() = integrationTest("yahoo/quote_summary", "aapl_price") {
     *     val result = ufc.yahoo.quoteSummary("AAPL", QuoteSummaryModule.PRICE)
     *     assertThat(result).isNotNull()
     * }
     * ```
     */
    protected inline fun integrationTest(
        category: String,
        fileName: String,
        timeout: Duration = 30.seconds,
        crossinline block: suspend () -> Unit,
    ): Unit =
        runTest(timeout = timeout) {
            val recordingContext = ResponseRecordingContext()
            withContext(recordingContext) {
                block()

                // 캡처된 응답 body가 있으면 레코딩
                val responseBody = recordingContext.getResponseBody()
                if (responseBody != null) {
                    ResponseRecorder.recordRaw(
                        jsonString = responseBody,
                        category = category,
                        fileName = fileName,
                    )
                } else {
                    println("[IntegrationTest] Warning: 캡처된 응답이 없습니다. ($category/$fileName)")
                }
            }
        }
}
