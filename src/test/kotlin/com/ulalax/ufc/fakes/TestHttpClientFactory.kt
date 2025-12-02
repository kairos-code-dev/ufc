package com.ulalax.ufc.fakes

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/**
 * 테스트용 HttpClient를 생성하는 팩토리입니다.
 *
 * Ktor의 MockEngine을 사용하여 실제 네트워크 호출 없이:
 * - 사전에 기록된 응답 반환
 * - 응답 캐싱 지원
 * - 재현 가능한 테스트 환경 제공
 * - 네트워크 지연 제거로 빠른 테스트 실행
 *
 * 사용 예시:
 * ```
 * // 기본 MockClient 생성
 * val httpClient = TestHttpClientFactory.createBasicMockClient()
 *
 * // 레코딩 응답 기반 MockClient 생성
 * val repository = RecordedResponseRepository()
 * repository.loadResponse("src/test/resources", "crumb.json")
 * val httpClient = TestHttpClientFactory.createClientWithRecordedResponses(repository)
 *
 * // 다양한 응답 설정
 * val httpClient = TestHttpClientFactory.createClientWithResponses(
 *     mapOf(
 *         "https://api.example.com/data" to testJsonResponse("{\"status\": \"ok\"}")
 *     )
 * )
 * ```
 */
object TestHttpClientFactory {

    /**
     * 기본 설정의 MockClient를 생성합니다.
     *
     * 모든 요청에 대해 200 OK와 빈 응답을 반환합니다.
     *
     * @return 설정된 HttpClient
     */
    fun createBasicMockClient(): HttpClient {
        return HttpClient(MockEngine { request ->
            respond(
                content = "",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/plain")
            )
        })
    }

    /**
     * 레코딩된 응답을 기반으로 MockClient를 생성합니다.
     *
     * RecordedResponseRepository에 캐시된 응답을 URL 경로에 따라 반환합니다.
     * URL 경로와 캐시 키를 매핑하여 사용합니다.
     *
     * @param repository 레코딩된 응답을 포함하는 저장소
     * @param urlToCacheKeyMapping URL 경로를 캐시 키로 매핑하는 맵 (기본값: 자동 매핑)
     * @return 설정된 HttpClient
     */
    fun createClientWithRecordedResponses(
        repository: RecordedResponseRepository,
        urlToCacheKeyMapping: Map<String, String> = emptyMap()
    ): HttpClient {
        return HttpClient(MockEngine { request ->
            val url = request.url.toString()

            // 맵핑된 키를 통해 캐시 조회
            var cacheKey = urlToCacheKeyMapping[url]

            // 매핑이 없으면 자동으로 경로에서 파일명 추출
            if (cacheKey == null) {
                val pathSegments = request.url.pathSegments
                cacheKey = if (pathSegments.isNotEmpty()) {
                    pathSegments.last()
                } else {
                    url.substringAfterLast("/")
                }
            }

            val responseContent = repository.getResponse(cacheKey)
                ?: repository.getUrlPattern(url)
                ?: "{}"  // 기본값: 빈 JSON 객체

            respond(
                content = responseContent,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        })
    }

    /**
     * 다양한 응답을 반환하는 MockClient를 생성합니다.
     *
     * URL별로 다른 응답을 반환하도록 설정합니다.
     *
     * @param responses URL을 응답 내용으로 매핑하는 맵
     * @return 설정된 HttpClient
     */
    fun createClientWithResponses(
        responses: Map<String, Pair<HttpStatusCode, String>>
    ): HttpClient {
        return HttpClient(MockEngine { request ->
            val url = request.url.toString()
            val (statusCode, content) = responses[url]
                ?: (HttpStatusCode.NotFound to "{\"error\": \"Not found\"}")

            respond(
                content = content,
                status = statusCode,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        })
    }

    /**
     * 지정된 경로에서 JSON 응답을 로드하는 MockClient를 생성합니다.
     *
     * 편의 메서드로, RecordedResponseRepository를 자동으로 생성하고 사용합니다.
     *
     * @param resourcePath 테스트 리소스 디렉토리 경로
     * @param fileMapping 파일명을 URL 패턴으로 매핑하는 맵
     * @return 설정된 HttpClient
     */
    fun createClientWithResourceResponses(
        resourcePath: String,
        fileMapping: Map<String, String> = emptyMap()
    ): HttpClient {
        val repository = RecordedResponseRepository()

        // 매핑된 파일들을 로드
        fileMapping.forEach { (urlPattern, fileName) ->
            try {
                val content = repository.loadResponse(resourcePath, fileName)
                repository.mapUrlPattern(urlPattern, content)
            } catch (e: Exception) {
                System.err.println("경고: 리소스 로드 실패 - $fileName: ${e.message}")
            }
        }

        return createClientWithRecordedResponses(repository)
    }

}

/**
 * 테스트용 HttpClient 생성을 위한 헬퍼 함수들입니다.
 */

/**
 * 간단한 응답 헬퍼 함수입니다.
 *
 * @param content 응답 내용
 * @param statusCode HTTP 상태 코드 (기본값: 200 OK)
 * @return 상태 코드와 내용의 Pair
 */
fun testResponse(
    content: String,
    statusCode: HttpStatusCode = HttpStatusCode.OK
): Pair<HttpStatusCode, String> {
    return Pair(statusCode, content)
}

/**
 * JSON 응답 헬퍼 함수입니다.
 *
 * @param jsonString JSON 문자열
 * @param statusCode HTTP 상태 코드 (기본값: 200 OK)
 * @return 상태 코드와 JSON 내용의 Pair
 */
fun testJsonResponse(
    jsonString: String,
    statusCode: HttpStatusCode = HttpStatusCode.OK
): Pair<HttpStatusCode, String> {
    return Pair(statusCode, jsonString)
}

/**
 * 에러 응답 헬퍼 함수입니다.
 *
 * @param statusCode HTTP 상태 코드 (기본값: 500 Internal Server Error)
 * @param message 에러 메시지
 * @return 상태 코드와 에러 JSON의 Pair
 */
fun testErrorResponse(
    message: String = "Internal Server Error",
    statusCode: HttpStatusCode = HttpStatusCode.InternalServerError
): Pair<HttpStatusCode, String> {
    return Pair(statusCode, "{\"error\": \"$message\"}")
}
