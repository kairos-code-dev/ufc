package com.ulalax.ufc.yahoo.internal.auth

import com.ulalax.ufc.yahoo.internal.YahooApiUrls
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import org.slf4j.LoggerFactory

/**
 * Yahoo Finance API의 기본 인증 전략을 구현합니다.
 *
 * 이 구현은 다음 단계를 거쳐 CRUMB 토큰을 획득합니다:
 * 1. fc.yahoo.com에 접속하여 초기 쿠키 획득 (Ktor HttpClient가 자동 관리)
 * 2. /getcrumb API 호출하여 CRUMB 토큰 획득
 * 3. 응답 검증 및 AuthResult 생성
 *
 * Ktor의 HttpClient는 기본적으로 쿠키를 자동으로 관리하므로,
 * 명시적으로 쿠키 설정이 필요하지 않습니다.
 *
 * @property httpClient Ktor의 HttpClient 인스턴스
 *                       CIO 엔진을 사용하여 코루틴 기반 논블로킹 I/O 제공
 *
 * @throws Exception CRUMB 토큰 획득 실패 시
 */
class BasicAuthStrategy(
    private val httpClient: HttpClient
) : AuthStrategy {

    private companion object {
        private val logger = LoggerFactory.getLogger(BasicAuthStrategy::class.java)

        // CRUMB 유효성 검사
        private const val MIN_CRUMB_LENGTH = 10
        private val INVALID_RESPONSE_PATTERNS = listOf("<!DOCTYPE", "<html", "{\"error\"")
    }

    /**
     * Yahoo Finance CRUMB 토큰을 획득하여 인증을 수행합니다.
     *
     * 프로세스:
     * 1. fc.yahoo.com 방문 (HttpClient가 자동으로 쿠키 저장)
     * 2. /v1/test/getcrumb 엔드포인트에 GET 요청
     * 3. 응답에서 CRUMB 값 추출 및 검증
     * 4. AuthResult 생성 후 반환
     *
     * Ktor HttpClient의 CIO 엔진은 쿠키를 자동으로 관리하므로,
     * 별도의 쿠키 설정이 필요하지 않습니다.
     *
     * @return 유효한 CRUMB 토큰을 포함하는 AuthResult
     * @throws Exception CRUMB_ACQUISITION_FAILED - CRUMB 획득 실패
     * @throws Exception AUTHENTICATION_FAILED - 기타 인증 실패
     */
    override suspend fun authenticate(): AuthResult {
        return try {
            logger.debug("Yahoo Finance 인증 시작: CRUMB 토큰 획득 중...")

            // Step 1: fc.yahoo.com 방문 (쿠키 획득)
            visitFcYahoo()

            // Step 2: CRUMB 토큰 획득
            val crumb = fetchCrumb()

            // Step 3: 응답 검증
            validateCrumb(crumb)

            // Step 4: AuthResult 생성 및 반환
            val authResult = AuthResult(
                crumb = crumb,
                strategy = "basic"
            )

            logger.info("Yahoo Finance 인증 성공: strategy=${authResult.strategy}, crumb_length=${crumb.length}")
            authResult

        } catch (e: Exception) {
            logger.error("Yahoo Finance 인증 실패: ${e.message}", e)
            throw Exception("Yahoo Finance 인증 중 오류 발생: ${e.message}", e)
        }
    }

    /**
     * fc.yahoo.com에 방문하여 초기 쿠키를 획득합니다.
     *
     * Ktor의 HttpClient는 자동으로 쿠키를 저장합니다.
     * 이 요청은 후속 /getcrumb 요청에서 필요한 세션 쿠키를 설정합니다.
     *
     * @throws Exception 네트워크 오류 또는 HTTP 오류 발생 시
     */
    private suspend fun visitFcYahoo() {
        try {
            // fc.yahoo.com 사용: finance.yahoo.com은 매우 긴 CSP/link 헤더를 반환하여
            // Ktor CIO의 헤더 라인 길이 제한을 초과함 (Header line length limit exceeded)
            // fc.yahoo.com은 짧은 헤더를 반환하면서도 필요한 쿠키를 설정함
            logger.debug("Yahoo Finance 쿠키 획득을 위해 fc.yahoo.com 방문 중...")
            val response = httpClient.get(YahooApiUrls.FC)

            // fc.yahoo.com은 404를 반환하지만 Set-Cookie 헤더는 정상적으로 설정됨
            // 따라서 404도 성공으로 처리 (쿠키 획득이 목적)
            if (response.status.value !in 200..499) {
                logger.warn("fc.yahoo.com 방문 실패: status=${response.status}")
            }
            logger.debug("fc.yahoo.com 방문 완료 (status=${response.status.value})")
        } catch (e: Exception) {
            logger.warn("Yahoo Finance 메인 페이지 방문 중 예외 발생 (계속 진행): ${e.message}")
            // 초기 방문이 실패해도 계속 진행한다.
            // CRUMB 획득이 실패할 때 정확한 예외를 발생시킨다.
        }
    }

    /**
     * /v1/test/getcrumb 엔드포인트에서 CRUMB 토큰을 획득합니다.
     *
     * 요청 헤더:
     * - User-Agent: 필수 (일부 요청은 User-Agent 검증 가능)
     *
     * @return 획득한 CRUMB 문자열
     * @throws Exception CRUMB 획득 실패 시
     */
    private suspend fun fetchCrumb(): String {
        try {
            logger.debug("CRUMB 토큰 획득 중: endpoint=${YahooApiUrls.CRUMB}")

            val response = httpClient.get(YahooApiUrls.CRUMB) {
                // Ktor는 자동으로 기본 User-Agent를 설정합니다
                // 필요시 사용자 정의 User-Agent 추가 가능
            }

            // HTTP 상태 코드 검증
            if (response.status.value !in 200..299) {
                logger.error("CRUMB 요청 실패: status=${response.status}")
                throw Exception("CRUMB 토큰 획득 실패: HTTP ${response.status.value}")
            }

            // 응답 본문 파싱
            val responseBody = response.body<String>()
            logger.debug("CRUMB 응답 수신: length=${responseBody.length}")

            return responseBody

        } catch (e: Exception) {
            logger.error("CRUMB 요청 중 네트워크 오류 발생", e)
            throw Exception("CRUMB 토큰 획득 중 네트워크 오류: ${e.message}", e)
        }
    }

    /**
     * 획득한 CRUMB의 유효성을 검증합니다.
     *
     * 검증 기준:
     * 1. CRUMB이 비어있지 않음
     * 2. CRUMB이 최소 길이 이상
     * 3. HTML 또는 에러 응답이 아님
     * 4. JSON 형식의 에러 응답이 아님
     *
     * @param crumb 검증할 CRUMB 값
     * @throws Exception 검증 실패 시 CRUMB_ACQUISITION_FAILED 예외 발생
     */
    private fun validateCrumb(crumb: String) {
        // 1. null 또는 공백 확인
        if (crumb.isBlank()) {
            logger.error("CRUMB 검증 실패: 빈 응답")
            throw Exception("Yahoo Finance에서 빈 CRUMB 응답을 반환했습니다.")
        }

        // 2. 최소 길이 확인
        if (crumb.length < MIN_CRUMB_LENGTH) {
            logger.error("CRUMB 검증 실패: 길이 부족 (length=${crumb.length}, min=$MIN_CRUMB_LENGTH)")
            throw Exception("CRUMB 토큰의 길이가 너무 짧습니다 (길이: ${crumb.length})")
        }

        // 3. HTML 응답 확인
        val trimmedCrumb = crumb.trim()
        if (INVALID_RESPONSE_PATTERNS.any { trimmedCrumb.startsWith(it, ignoreCase = true) }) {
            logger.error("CRUMB 검증 실패: HTML/JSON 에러 응답")
            val preview = trimmedCrumb.take(100)
            throw Exception("Yahoo Finance가 HTML/에러 응답을 반환했습니다 (응답: $preview)")
        }

        logger.debug("CRUMB 검증 성공: length=${crumb.length}")
    }
}
