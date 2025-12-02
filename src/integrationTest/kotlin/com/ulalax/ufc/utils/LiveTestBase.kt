package com.ulalax.ufc.utils

import com.ulalax.ufc.client.UFCClientImpl
import com.ulalax.ufc.client.UFCClientConfig
import com.ulalax.ufc.client.UFCClient
import com.ulalax.ufc.internal.ResponseRecordingContext
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 라이브 테스트의 기본 클래스
 *
 * 라이브 테스트 클래스는 이 클래스를 상속하여 다음 기능을 사용할 수 있습니다:
 * - UFC 클라이언트 초기화 및 정리
 * - FRED API 키 자동 로드
 * - 응답 기록 설정 확인
 * - 코루틴 기반 테스트 지원 (runTest)
 *
 * 테스트 실행 흐름:
 * 1. @BeforeAll setUp() 호출 - 클라이언트 초기화
 * 2. 각 테스트 메서드 실행 - liveTest() 래퍼 사용
 * 3. @AfterAll tearDown() 호출 - 클라이언트 정리
 *
 * 테스트 태그:
 * - "live": 라이브 테스트임을 표시
 * - JUnit5의 @Tag를 사용하여 필터링 가능
 *
 * 사용 예시:
 * ```
 * @Tag("live")
 * class YahooFinanceLiveTest : LiveTestBase() {
 *
 *     @Test
 *     fun testFetchQuote() = liveTest {
 *         // UFC 클라이언트는 자동으로 초기화됨 (this.client)
 *         // 테스트 코드 작성
 *         val result = fetchSomeData()
 *         ResponseRecorder.record(result, "yahoo/ticker", "test_result")
 *     }
 *
 *     @Test
 *     fun testWithTimeout() = liveTest(timeout = 60.seconds) {
 *         // 더 긴 타임아웃이 필요한 테스트
 *     }
 * }
 * ```
 *
 * 주의사항:
 * - FRED_API_KEY는 local.properties 파일에서 자동으로 로드됩니다
 * - API 키가 없으면 경고 메시지만 출력되고 계속 진행됩니다
 * - 각 테스트는 30초의 기본 타임아웃을 가집니다
 * - 레이트 리미팅을 고려하여 테스트를 설계하세요
 */
@Tag("live")
@TestInstance(PER_CLASS)
abstract class LiveTestBase {
    companion object {
        private val logger = LoggerFactory.getLogger(LiveTestBase::class.java)
    }

    /**
     * UFC 클라이언트 인스턴스
     *
     * setUp()에서 초기화되며, tearDown()에서 정리됩니다.
     * 모든 라이브 테스트에서 사용할 수 있습니다.
     */
    protected lateinit var client: UFCClientImpl

    /**
     * 라이브 테스트 초기화
     *
     * 다음 작업을 수행합니다:
     * 1. local.properties에서 FRED_API_KEY 로드
     * 2. UFCClient 생성 및 초기화
     * 3. 응답 기록 설정 확인 메시지 출력
     *
     * runTest를 사용하여 suspend 함수로 실행됩니다.
     */
    @BeforeAll
    fun setUp() = runTest {
        logger.info("🚀 Live Test 시작")

        // FRED API 키 로드
        val fredApiKey = loadFredApiKey()
        if (fredApiKey == null) {
            logger.warn(
                "⚠️ FRED_API_KEY not configured. " +
                    "Some tests may fail or be skipped. " +
                    "Set FRED_API_KEY in local.properties file."
            )
        } else {
            logger.info("✅ FRED_API_KEY loaded successfully")
        }

        // UFC 클라이언트 생성
        client = UFCClient.create(
            UFCClientConfig(fredApiKey = fredApiKey)
        )

        // 응답 기록 설정 확인
        val recordingStatus = if (RecordingConfig.isRecordingEnabled) {
            "활성화 (Enabled)"
        } else {
            "비활성화 (Disabled)"
        }
        logger.info("📝 Response Recording: $recordingStatus")
        logger.info("📂 Base Output Path: ${RecordingConfig.baseOutputPath}")
    }

    /**
     * 라이브 테스트 정리
     *
     * UFC 클라이언트를 안전하게 종료합니다.
     * 클라이언트가 초기화되었는지 확인 후 close()를 호출합니다.
     *
     * 서브클래스에서 추가 정리가 필요한 경우 onBeforeCleanup()을 override합니다.
     */
    @AfterAll
    fun tearDown() {
        try {
            // 서브클래스의 추가 정리 실행
            onBeforeCleanup()

            if (::client.isInitialized) {
                client.close()
                logger.info("✅ UFC Client closed successfully")
            }
        } catch (e: Exception) {
            logger.warn("⚠️ Error while closing UFC client", e)
        }
        logger.info("🏁 Live Test 종료")
    }

    /**
     * 라이브 테스트 종료 전 추가 정리가 필요한 경우 override합니다.
     *
     * 이 메서드는 tearDown()에서 UFC 클라이언트 정리 전에 호출됩니다.
     * 서브클래스에서 HTTP 클라이언트 등 추가 리소스를 정리해야 하는 경우 사용합니다.
     *
     * 사용 예시:
     * ```
     * class MyLiveTest : LiveTestBase() {
     *     private val httpClient = ...
     *
     *     override fun onBeforeCleanup() {
     *         httpClient.close()
     *     }
     * }
     * ```
     */
    protected open fun onBeforeCleanup() {
        // 기본 구현: 아무것도 하지 않음
        // 서브클래스에서 필요시 override
    }

    /**
     * local.properties 파일에서 FRED_API_KEY를 로드합니다.
     *
     * 로드 순서:
     * 1. local.properties 파일 존재 여부 확인
     * 2. FRED_API_KEY 프로퍼티 읽기
     * 3. null이거나 "your_fred_api_key_here" 같은 플레이스홀더면 null 반환
     *
     * @return 로드된 FRED API 키 또는 null
     */
    private fun loadFredApiKey(): String? {
        val localPropertiesFile = File("local.properties")

        if (!localPropertiesFile.exists()) {
            logger.debug("local.properties file not found")
            return null
        }

        return try {
            val properties = java.util.Properties()
            localPropertiesFile.inputStream().use { properties.load(it) }

            val apiKey = properties.getProperty("FRED_API_KEY")?.trim()

            // 플레이스홀더 값 제외
            when {
                apiKey.isNullOrEmpty() -> null
                apiKey.contains("your_fred_api_key_here", ignoreCase = true) -> null
                else -> apiKey
            }
        } catch (e: Exception) {
            logger.debug("Error reading local.properties", e)
            null
        }
    }

    /**
     * 라이브 테스트용 코루틴 블록을 실행합니다.
     *
     * 주의: runTest는 특정 테스트 디스패처를 사용하므로, 실제 네트워크 호출이 필요한 경우
     * 적절한 타임아웃을 설정해야 합니다.
     *
     * @param timeout 테스트 실행 타임아웃 (기본값: 30초)
     * @param block 실행할 suspend 함수
     *
     * 사용 예시:
     * ```
     * @Test
     * fun testNetworkCall() = liveTest {
     *     // suspend 함수 호출
     *     val result = client.doSomething()
     *     // 검증
     *     assertThat(result).isNotNull()
     * }
     *
     * @Test
     * fun testSlowNetworkCall() = liveTest(timeout = 60.seconds) {
     *     // 더 긴 타임아웃이 필요한 네트워크 호출
     * }
     * ```
     */
    protected suspend fun liveTest(timeout: Duration = 30.seconds, block: suspend () -> Unit) {
        runTest(timeout = timeout) {
            block()
        }
    }

    /**
     * 라이브 테스트용 코루틴 블록을 실행하면서 응답을 자동으로 레코딩합니다.
     *
     * ResponseRecordingContext를 사용하여 API 응답을 자동으로 캡처하고 파일로 저장합니다.
     * 레코딩이 비활성화된 경우 일반 테스트로 실행됩니다.
     *
     * @param category 응답 파일을 저장할 카테고리 경로 (예: "yahoo/chart/daily")
     * @param fileName 저장할 파일 이름 (확장자 제외)
     * @param timeout 테스트 실행 타임아웃 (기본값: 30초)
     * @param block 실행할 suspend 함수
     *
     * 사용 예시:
     * ```
     * @Test
     * fun testChartData() = liveTestWithRecording(
     *     category = RecordingConfig.Paths.Yahoo.Chart.DAILY,
     *     fileName = "aapl_daily_1y"
     * ) {
     *     val data = service.getChartData("AAPL", Interval.OneDay, Period.OneYear)
     *     assertThat(data).isNotEmpty()
     *     // 응답이 자동으로 src/liveTest/resources/responses/yahoo/chart/daily/aapl_daily_1y.json에 저장됨
     * }
     * ```
     */
    protected fun liveTestWithRecording(
        category: String,
        fileName: String,
        timeout: Duration = 30.seconds,
        block: suspend () -> Unit
    ) = runTest(timeout = timeout) {
        if (!RecordingConfig.isRecordingEnabled) {
            // 레코딩 비활성화 → 일반 테스트로 실행
            block()
            return@runTest
        }

        // ResponseRecordingContext 생성
        val recordingContext = ResponseRecordingContext()

        // Context와 함께 테스트 실행
        withContext(recordingContext) {
            try {
                // 테스트 블록 실행 (API 호출 시 자동 저장)
                block()

                // 마지막 응답 가져오기
                val responseBody = recordingContext.getResponseBody()

                if (responseBody != null) {
                    // ResponseRecorder로 파일 저장
                    ResponseRecorder.recordRaw(responseBody, category, fileName)
                } else {
                    logger.warn("No response recorded for $category/$fileName")
                }
            } finally {
                // Context 정리
                recordingContext.clear()
            }
        }
    }
}
