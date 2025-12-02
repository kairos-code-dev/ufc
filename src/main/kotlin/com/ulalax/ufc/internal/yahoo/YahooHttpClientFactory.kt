package com.ulalax.ufc.internal.yahoo

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import com.ulalax.ufc.infrastructure.http.UserAgents

/**
 * Yahoo Finance API용 Ktor HTTP 클라이언트 팩토리
 *
 * Ktor CIO 엔진 기반의 프로덕션급 HTTP 클라이언트를 생성합니다.
 * 타임아웃, JSON 직렬화, 로깅을 포함하고 있습니다.
 *
 * 주요 특징:
 * - CIO 엔진: 비동기 I/O 기반의 경량 엔진
 * - HttpCookies: Yahoo Finance API 인증에 필요한 쿠키 자동 관리
 * - ContentEncoding: gzip/deflate 압축 자동 해제
 * - HttpTimeout: 연결 타임아웃 30초, 소켓/요청 타임아웃 60초
 * - ContentNegotiation: JSON 자동 파싱 (알려지지 않은 키 무시, lenient 모드)
 * - Logging: 모든 HTTP 요청/응답 로깅
 * - DefaultRequest: User-Agent, Accept, Accept-Language 등 기본 헤더 설정
 *
 * 사용 예시:
 * ```
 * val httpClient = YahooHttpClientFactory.create()
 * try {
 *     // HTTP 요청 수행
 * } finally {
 *     httpClient.close()
 * }
 * ```
 */
object YahooHttpClientFactory {
    private val logger = LoggerFactory.getLogger(YahooHttpClientFactory::class.java)

    /**
     * Ktor 로깅 어댑터 - SLF4J와 Ktor 로거를 연결합니다
     */
    private val ktorLogger = object : Logger {
        override fun log(message: String) {
            logger.info(message)
        }
    }

    /**
     * Ktor CIO 엔진 기반의 HttpClient를 생성합니다.
     *
     * 생성된 클라이언트는 다음과 같은 플러그인이 설치됩니다:
     * 1. HttpCookies - 쿠키 자동 관리 (Yahoo Finance API 인증 필수)
     * 2. ContentEncoding - gzip/deflate 압축 자동 해제
     * 3. HttpTimeout - 타임아웃 설정 (연결 30초, 요청 60초)
     * 4. ContentNegotiation + Serialization - JSON 자동 파싱
     * 5. Logging - 요청/응답 로깅 (INFO 레벨)
     * 6. DefaultRequest - 기본 헤더 설정 (User-Agent, Accept, etc)
     *
     * @return 설정된 HttpClient 인스턴스
     */
    fun create(): HttpClient {
        logger.info("Creating Yahoo Finance HTTP client with CIO engine")

        return HttpClient(CIO) {
            // 1. 쿠키 관리
            // Yahoo Finance API 인증에 필요한 쿠키를 자동으로 관리합니다
            install(HttpCookies)

            // 2. 컨텐츠 인코딩 (압축 해제)
            // gzip, deflate로 압축된 응답을 자동으로 해제합니다
            install(ContentEncoding) {
                gzip()
                deflate()
            }

            // 3. 타임아웃 설정
            // 각 단계별로 타임아웃을 설정하여 연결 장애 시 빠르게 실패합니다
            install(HttpTimeout) {
                // 서버와의 초기 연결 타임아웃
                connectTimeoutMillis = 30_000L // 30초

                // 서버로부터 데이터를 읽는 동안의 타임아웃
                socketTimeoutMillis = 60_000L // 60초

                // 전체 요청 타임아웃
                requestTimeoutMillis = 60_000L // 60초
            }

            // 4. 컨텐츠 협상 및 JSON 직렬화 설정
            // JSON 응답을 자동으로 파싱하기 위한 설정
            install(ContentNegotiation) {
                json(Json {
                    // 알려지지 않은 JSON 키를 무시합니다
                    // Yahoo Finance API 응답의 미지의 필드는 무시됩니다
                    ignoreUnknownKeys = true

                    // lenient 모드: JSON 형식 검증을 느슨하게 합니다
                    // 줄바꿈이나 트레일링 쉼표 같은 문법을 허용합니다
                    isLenient = true

                    // 숫자 타입 강제 변환
                    // "123" (문자열)을 123 (숫자)으로 변환합니다
                    coerceInputValues = true
                })
            }

            // 5. 로깅 플러그인
            // 모든 HTTP 요청과 응답을 로깅합니다
            install(Logging) {
                logger = ktorLogger
                level = LogLevel.INFO
            }

            // 6. 기본 요청 헤더 설정
            // Yahoo Finance API와의 통신을 위한 필수 헤더를 설정합니다
            defaultRequest {
                // 무작위 User-Agent를 선택하여 봇 탐지 우회
                // Yahoo가 자동화된 요청을 제한하는 것을 피합니다
                header("User-Agent", UserAgents.random())

                // 브라우저가 HTML과 XML을 받을 수 있음을 표시
                header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")

                // 선호 언어: 영어
                header("Accept-Language", "en-US,en;q=0.5")

                // gzip 압축 지원
                header("Accept-Encoding", "gzip, deflate")

                // HTTP 연결을 닫지 않고 재사용 (Keep-Alive)
                header("Connection", "keep-alive")
            }

            // CIO 엔진 설정
            // CIO 엔진은 비동기 I/O를 사용하므로 별도의 스레드 풀 설정이 필요 없습니다
        }.also {
            logger.info("Yahoo Finance HTTP client created successfully")
        }
    }
}
