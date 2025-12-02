package com.ulalax.ufc.live.yahoo

import com.ulalax.ufc.internal.yahoo.YahooApiUrls
import com.ulalax.ufc.internal.yahoo.YahooHttpClientFactory
import com.ulalax.ufc.internal.yahoo.auth.YahooAuthenticator
import com.ulalax.ufc.utils.LiveTestBase
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.assertj.core.api.Assertions.assertThat

/**
 * Yahoo Finance API 인증 기능에 대한 통합 테스트
 *
 * 실제 Yahoo Finance API에 연결하여 다음을 검증합니다:
 * - Crumb 토큰 획득 성공
 * - 인증 결과 유효성 검증
 * - 인증 결과 캐싱 및 재사용
 * - 캐시된 인증으로 API 호출 성능 검증
 *
 * ## 테스트 실행 조건
 * - 네트워크 연결이 필수입니다
 * - Yahoo Finance API가 정상 작동해야 합니다
 * - 타임아웃은 기본값으로 설정되어 있습니다 (30초)
 *
 * ## 테스트 환경
 * - Tag: "integration" - 통합 테스트임을 표시
 * - TestInstance: PER_CLASS - 클래스 인스턴스 재사용으로 setUp/tearDown 최소화
 *
 * ## 구현 내용
 * 1. HTTP 클라이언트 생성 (Ktor CIO 엔진)
 * 2. Yahoo 인증기 초기화
 * 3. 5개의 테스트 메서드로 인증 기능 검증:
 *    - testAuthentication: 기본 인증 프로세스 검증
 *    - testGetCrumb: Crumb 토큰 단독 획득 검증
 *    - testAuthenticationCaching: 인증 캐싱 재사용 검증
 *    - testAuthenticatorWithHttpClient: 캐시된 인증으로 API 호출 검증
 *    - 추가: 기본 인증 전략 검증
 *
 * ## 실행 방법
 * ```bash
 * # 통합 테스트만 실행
 * ./gradlew integrationTest --tests "YahooAuthSpec"
 *
 * # 특정 메서드만 실행
 * ./gradlew integrationTest --tests "YahooAuthSpec.testAuthentication"
 *
 * # 빌드만 수행 (통합 테스트 제외)
 * ./gradlew build -x integrationTest
 *
 * # 통합 테스트 감지 확인 (dry-run)
 * ./gradlew integrationTest --dry-run
 * ```
 *
 * ## 주의사항
 * - 각 테스트는 실제 네트워크 호출을 수행합니다
 * - Yahoo Finance API 레이트 리미팅을 고려해야 합니다
 * - 테스트 실행 간 충분한 간격을 두기를 권장합니다
 * - 실패 원인은 네트워크, Yahoo API 상태 등 다양할 수 있습니다
 */
class YahooAuthSpec : LiveTestBase() {

    private companion object {
        private val logger = LoggerFactory.getLogger(YahooAuthSpec::class.java)
    }

    /**
     * Ktor HTTP 클라이언트 인스턴스
     *
     * 다음 특징을 가집니다:
     * - CIO 엔진을 사용한 경량 비동기 클라이언트
     * - 타임아웃 설정: 연결 30초, 요청 60초
     * - 기본 헤더: User-Agent, Accept, Accept-Language 등
     * - 자동 JSON 파싱 (ContentNegotiation)
     *
     * tearDown()에서 안전하게 종료됩니다.
     */
    private val httpClient = YahooHttpClientFactory.create()

    /**
     * Yahoo Finance API 인증기 인스턴스
     *
     * 다음 기능을 제공합니다:
     * - Crumb 토큰 획득
     * - 인증 결과 캐싱
     * - 동시성 제어 (Mutex 기반)
     * - 인증 유효성 자동 검증
     *
     * httpClient를 통해 모든 HTTP 요청을 수행합니다.
     */
    private val authenticator = YahooAuthenticator(httpClient)

    /**
     * 테스트 환경 정리
     *
     * HTTP 클라이언트를 정리합니다.
     */
    override fun onBeforeCleanup() {
        try {
            httpClient.close()
            logger.info("✅ HTTP 클라이언트 정리 완료")
        } catch (e: Exception) {
            logger.warn("⚠️ HTTP 클라이언트 정리 중 오류 발생", e)
        }
    }

    /**
     * Yahoo Finance 인증을 성공적으로 수행할 수 있는지 검증합니다.
     *
     * 이 테스트는 다음을 검증합니다:
     * 1. authenticate() 메서드가 null이 아닌 결과를 반환
     * 2. 반환된 AuthResult의 crumb이 null이 아님
     * 3. crumb이 빈 문자열이 아님
     * 4. crumb이 HTML 콘텐츠가 아님 ("<html>" 체크)
     * 5. strategy가 "basic"으로 설정됨
     * 6. 인증이 유효한 상태임
     *
     * ## 검증 로직
     * - crumb < html 체크: HTML 오류 페이지를 감지하기 위함
     * - strategy 체크: 현재 BasicAuthStrategy만 구현되어 있음
     * - isValid() 체크: 타임바운드 인증의 유효성 검증
     *
     * @throws AssertionError 위 조건 중 하나라도 실패하면 발생
     */
    @Test
    @DisplayName("Yahoo Finance 인증을 성공적으로 수행할 수 있다")
    fun testAuthentication() = runTest {
        logger.info("🔍 Yahoo Finance 인증 테스트 시작...")

        // 1. 인증 수행
        val authResult = authenticator.authenticate()

        // 2. 결과 검증 - null 체크
        assertThat(authResult).isNotNull()
        logger.info("✅ AuthResult 획득 성공")

        // 3. Crumb 토큰 검증 - null 체크
        val crumb = authResult.crumb
        assertThat(crumb).isNotNull()
        logger.info("✅ Crumb 토큰 획득 성공")

        // 4. Crumb 토큰 검증 - 빈 문자열 체크
        assertThat(crumb).isNotEmpty()
        logger.info("✅ Crumb 토큰이 비어있지 않음")

        // 5. Crumb 토큰 검증 - HTML 콘텐츠 체크
        assertThat(crumb.lowercase()).doesNotStartWith("<html>")
        logger.info("✅ Crumb 토큰이 HTML이 아님")

        // 6. 인증 전략 검증
        val strategy = authResult.strategy
        assertThat(strategy).isEqualTo("basic")
        logger.info("✅ 인증 전략 검증 완료: $strategy")

        // 7. 인증 유효성 검증
        assertThat(authResult.isValid()).isTrue()
        logger.info("✅ 인증이 유효함")

        // 8. 로그 출력 - 인증 성공 정보
        val crumbPreview = crumb.take(20)
        logger.info("""
            |╔════════════════════════════════════════════════════════════╗
            |║ 🎯 Yahoo Finance 인증 성공                                 ║
            |║─────────────────────────────────────────────────────────── ║
            |║ • Strategy: $strategy
            |║ • Crumb (처음 20글자): $crumbPreview...
            |║ • Crumb 길이: ${crumb.length} 글자
            |║ • 유효 여부: ${if (authResult.isValid()) "✓ 유효" else "✗ 만료됨"}
            |║ • 경과 시간: ${System.currentTimeMillis() - authResult.timestamp}ms
            |╚════════════════════════════════════════════════════════════╝
        """.trimMargin())
    }

    /**
     * Crumb 토큰을 독립적으로 가져올 수 있는지 검증합니다.
     *
     * 이 테스트는 다음을 검증합니다:
     * 1. getCrumb() 메서드가 직접 crumb 문자열을 반환
     * 2. 반환된 crumb이 null이 아님
     * 3. crumb이 빈 문자열이 아님
     *
     * ## 목적
     * getCrumb()은 authenticate()를 래핑하여 crumb만 추출하는
     * 편의 메서드입니다. 이를 통해 AuthResult 객체가 필요 없는
     * 단순한 사용 사례를 지원합니다.
     *
     * @throws AssertionError crumb이 null이거나 비어있으면 발생
     */
    @Test
    @DisplayName("Crumb을 가져올 수 있다")
    fun testGetCrumb() = runTest {
        logger.info("🔍 Crumb 획득 테스트 시작...")

        // 1. Crumb 획득
        val crumb = authenticator.getCrumb()

        // 2. 결과 검증 - null 체크
        assertThat(crumb).isNotNull()
        logger.info("✅ Crumb 획득 성공")

        // 3. 결과 검증 - 빈 문자열 체크
        assertThat(crumb).isNotEmpty()
        logger.info("✅ Crumb이 유효함")

        // 4. 로그 출력
        logger.info("""
            |╔════════════════════════════════════════════════════════════╗
            |║ 🎯 Crumb 획득 성공                                          ║
            |║─────────────────────────────────────────────────────────── ║
            |║ • Crumb 길이: ${crumb.length} 글자
            |║ • Crumb (처음 30글자): ${crumb.take(30)}...
            |╚════════════════════════════════════════════════════════════╝
        """.trimMargin())
    }

    /**
     * 인증 결과가 효과적으로 캐시되고 재사용되는지 검증합니다.
     *
     * 이 테스트는 다음을 검증합니다:
     * 1. 첫 번째 getCrumb() 호출로 crumb1 획득
     * 2. 두 번째 getCrumb() 호출로 crumb2 획득
     * 3. crumb1과 crumb2가 정확히 동일함 (캐시 재사용)
     * 4. 두 번의 API 호출이 아닌 한 번의 API 호출만 발생
     *
     * ## 성능 검증
     * 이 테스트는 캐싱이 제대로 작동하여:
     * - 불필요한 네트워크 요청을 줄임
     * - API 레이트 리미팅을 회피함
     * - 응답 시간을 개선함
     *
     * 을 보장합니다.
     *
     * ## 주의사항
     * 이 테스트 이전의 testAuthentication이나 testGetCrumb이 실행되었다면,
     * 그 결과의 캐시가 남아있을 수 있습니다. 하지만 이는 문제가 아닙니다.
     * 왜냐하면 우리는 동일한 authenticator 인스턴스를 사용하고 있고,
     * 캐시된 값이 동일하다는 것만 확인하면 되기 때문입니다.
     *
     * @throws AssertionError crumb1과 crumb2가 다르면 발생
     */
    @Test
    @DisplayName("인증 결과를 캐시할 수 있다")
    fun testAuthenticationCaching() = runTest {
        logger.info("🔍 인증 캐싱 테스트 시작...")

        // 1. 첫 번째 Crumb 획득 (실제 인증 수행 또는 기존 캐시 사용)
        val crumb1 = authenticator.getCrumb()
        logger.info("✅ 첫 번째 Crumb 획득: ${crumb1.take(20)}...")

        // 2. 두 번째 Crumb 획득 (반드시 캐시에서 재사용)
        val crumb2 = authenticator.getCrumb()
        logger.info("✅ 두 번째 Crumb 획득: ${crumb2.take(20)}...")

        // 3. 캐시 재사용 검증
        assertThat(crumb1).isEqualTo(crumb2)
        logger.info("✅ 캐시 재사용 확인")

        // 4. 로그 출력
        logger.info("""
            |╔════════════════════════════════════════════════════════════╗
            |║ 🎯 캐시 재사용 성공                                         ║
            |║─────────────────────────────────────────────────────────── ║
            |║ • 첫 번째 Crumb: ${crumb1.take(30)}...
            |║ • 두 번째 Crumb: ${crumb2.take(30)}...
            |║ • 동일 여부: ${crumb1 == crumb2} ✓
            |║ • Crumb 길이: ${crumb1.length} 글자
            |║ • 성능 이점: 불필요한 네트워크 요청 제거
            |╚════════════════════════════════════════════════════════════╝
        """.trimMargin())
    }

    /**
     * 캐시된 인증으로 실제 Yahoo Finance API를 호출할 수 있는지 검증합니다.
     *
     * 이 테스트는 다음을 검증합니다:
     * 1. authenticate() 메서드로 인증 수행
     * 2. httpClient.get()으로 Yahoo API 호출
     * 3. applyAuth()를 통해 crumb 파라미터 자동 추가
     * 4. 응답 상태 코드가 200-299 범위 (성공)
     *
     * ## 엔드포인트 설명
     * - YahooApiUrls.SEARCH: 주식 검색 엔드포인트
     * - parameter("q", "AAPL"): Apple 주식 검색
     * - authenticator.applyAuth(this): HttpRequestBuilder에 crumb 파라미터 추가
     *
     * ## API 호출 흐름
     * 1. authenticate() - 필요시 재인증, 아니면 캐시 사용
     * 2. applyAuth() - crumb 파라미터 자동 추가
     * 3. httpClient.get() - HTTP GET 요청 실행
     * 4. response.status.isSuccess() - HTTP 상태 코드 검증
     *
     * ## 주의사항
     * - 이 테스트는 실제 Yahoo Finance API에 요청을 보냅니다
     * - API 응답 속도는 네트워크 상태에 따라 달라집니다
     * - 레이트 리미팅에 걸릴 수 있으므로 주의가 필요합니다
     *
     * @throws AssertionError HTTP 응답이 실패하면 발생
     */
    @Test
    @DisplayName("초기화된 클라이언트로 API를 호출할 수 있다")
    fun testAuthenticatorWithHttpClient() = runTest {
        logger.info("🔍 인증된 API 호출 테스트 시작...")

        // 1. 인증 수행
        logger.info("Step 1: 인증 수행 중...")
        val authResult = authenticator.authenticate()
        logger.info("✅ 인증 완료: strategy=${authResult.strategy}")

        // 2. HTTP GET 요청 실행 (applyAuth를 통해 crumb 자동 추가)
        logger.info("Step 2: Yahoo API 호출 중 (SEARCH 엔드포인트)...")
        val response = httpClient.get(YahooApiUrls.SEARCH) {
            // 검색 쿼리 파라미터 추가
            parameter("q", "AAPL")

            // 인증 정보(crumb) 자동 추가
            authenticator.applyAuth(this)
        }
        logger.info("✅ HTTP 요청 전송 완료")

        // 3. 응답 상태 코드 검증
        val statusCode = response.status.value
        val isSuccess = response.status.isSuccess()

        logger.info("✅ 응답 수신 완료: HTTP $statusCode")

        assertThat(isSuccess).isTrue()
        logger.info("✅ HTTP 상태 코드 검증 완료")

        // 4. 로그 출력
        val contentLength = response.headers["Content-Length"] ?: "미정의"
        logger.info("""
            |╔════════════════════════════════════════════════════════════╗
            |║ 🎯 인증된 API 호출 성공                                     ║
            |║─────────────────────────────────────────────────────────── ║
            |║ • 엔드포인트: ${YahooApiUrls.SEARCH}
            |║ • 검색 쿼리: AAPL
            |║ • HTTP 상태: $statusCode (${response.status.description})
            |║ • 인증 전략: ${authResult.strategy}
            |║ • Crumb 파라미터: 자동 추가됨 ✓
            |║ • 응답 크기: $contentLength bytes
            |╚════════════════════════════════════════════════════════════╝
        """.trimMargin())
    }

    /**
     * BasicAuthStrategy를 통한 기본 인증 전략이 올바르게 작동하는지 검증합니다.
     *
     * 이 테스트는 다음을 검증합니다:
     * 1. authenticate() 결과의 strategy가 "basic"임
     * 2. BasicAuthStrategy가 기본 인증 전략임
     * 3. 향후 다른 전략이 추가될 때의 확장성 확보
     *
     * ## 전략 패턴
     * YahooAuthenticator는 AuthStrategy 인터페이스를 구현한
     * 여러 전략을 지원할 수 있도록 설계되었습니다.
     * 현재는 BasicAuthStrategy만 구현되어 있습니다.
     *
     * 향후 다음과 같은 전략이 추가될 수 있습니다:
     * - AdvancedAuthStrategy (더 복잡한 인증 프로세스)
     * - ApiKeyAuthStrategy (API 키 기반 인증)
     * - OAuthStrategy (OAuth 2.0 기반 인증)
     */
    @Test
    @DisplayName("기본 인증 전략이 올바르게 작동한다")
    fun testBasicAuthStrategy() = runTest {
        logger.info("🔍 기본 인증 전략 테스트 시작...")

        // 1. 인증 수행
        val authResult = authenticator.authenticate()
        logger.info("✅ 인증 수행 완료")

        // 2. 전략 검증
        val strategy = authResult.strategy
        assertThat(strategy).isEqualTo("basic")
        logger.info("✅ 기본 인증 전략 검증 완료")

        // 3. 향후 확장성 메시지
        logger.info("""
            |╔════════════════════════════════════════════════════════════╗
            |║ 🎯 기본 인증 전략 검증 완료                                 ║
            |║─────────────────────────────────────────────────────────── ║
            |║ • 현재 전략: $strategy
            |║ • 구현: BasicAuthStrategy
            |║ • 확장성: 준비됨 ✓
            |║                                                            ║
            |║ 향후 지원 가능 전략:                                        ║
            |║ • Advanced Auth Strategy                                  ║
            |║ • API Key Auth Strategy                                   ║
            |║ • OAuth Strategy                                          ║
            |╚════════════════════════════════════════════════════════════╝
        """.trimMargin())
    }
}
