package com.ulalax.ufc.infrastructure.yahoo.internal.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory

/**
 * Yahoo Finance API 인증을 관리하는 주요 클래스입니다.
 *
 * 이 클래스는 다음 책임을 가집니다:
 * 1. 인증 전략 관리 및 실행
 * 2. 인증 결과 캐싱 및 재사용
 * 3. 동시성 제어 (Mutex를 사용한 스레드 안전성)
 * 4. 인증 유효성 검증 및 자동 갱신
 *
 * ## 동시성 제어:
 * Mutex를 사용하여 다중 코루틴이 동시에 인증을 수행하는 것을 방지합니다.
 * 이를 통해 불필요한 네트워크 요청을 줄이고 안정성을 향상시킵니다.
 *
 * ## 캐싱 전략:
 * - authResult는 @Volatile로 선언되어 메모리 가시성을 보장합니다
 * - 캐시된 인증이 유효하면 재사용
 * - 캐시된 인증이 만료되었으면 재인증
 *
 * @property httpClient Ktor의 HttpClient 인스턴스 (CIO 엔진 사용)
 * @property authResult 캐시된 인증 결과 (@Volatile로 메모리 가시성 보장)
 * @property lock Mutex를 사용한 인증 프로세스의 동시성 제어
 * @property basicStrategy BasicAuthStrategy 인스턴스
 */
class YahooAuthenticator(
    private val httpClient: HttpClient
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(YahooAuthenticator::class.java)
    }

    /**
     * 캐시된 인증 결과.
     *
     * @Volatile 어노테이션으로 선언되어 메모리 가시성을 보장합니다.
     * 이를 통해 다른 스레드/코루틴이 최신 값을 읽을 수 있습니다.
     *
     * 일반 변수로 충분한 이유:
     * - 실제 값 변경은 Mutex 내부에서만 발생
     * - @Volatile은 단순히 가시성을 보장하기 위함
     * - 복잡한 동기화는 Mutex가 담당
     */
    @Volatile
    private var authResult: AuthResult? = null

    /**
     * Mutex는 인증 프로세스의 동시성 제어를 담당합니다.
     *
     * 사용 사례:
     * - 여러 코루틴이 동시에 authenticate() 호출 시 첫 번째만 실행되고 나머지는 대기
     * - 인증 중에 reset()이 호출되어도 안전함
     * - 캐시 확인과 갱신이 원자적으로 수행됨
     *
     * Mutex는 StdlibLock 기반으로 Kotlin 표준 라이브러리의 동시성 프리미티브를 사용합니다.
     */
    private val lock = Mutex()

    /**
     * BasicAuthStrategy 인스턴스.
     *
     * Lazy 초기화가 아닌 즉시 초기화로 하여 간단하게 구성했습니다.
     * 향후 전략을 주입받을 수 있도록 구조를 개선할 수 있습니다.
     */
    private val basicStrategy = BasicAuthStrategy(httpClient)

    /**
     * Yahoo Finance API 인증을 수행합니다.
     *
     * 프로세스:
     * 1. Mutex 획득 (다른 코루틴이 동시에 인증하지 않도록)
     * 2. 기존 인증 결과가 유효한지 확인
     *    - 유효하면 재사용 (네트워크 요청 회피)
     *    - 만료되었으면 재인증
     * 3. BasicAuthStrategy를 사용하여 인증 수행
     * 4. 결과 캐싱
     *
     * 이 메서드는 idempotent합니다. 연속으로 호출해도 캐시된 유효한 인증을 재사용합니다.
     *
     * @return 유효한 AuthResult 객체
     * @throws Exception 인증 실패 시
     */
    suspend fun authenticate(): AuthResult {
        return lock.withLock {
            // 1단계: 기존 인증 확인
            val cachedAuth = authResult
            if (cachedAuth != null && cachedAuth.isValid()) {
                logger.debug("캐시된 인증 재사용: elapsed=${System.currentTimeMillis() - cachedAuth.timestamp}ms")
                return@withLock cachedAuth
            }

            // 2단계: 만료된 인증이 있으면 로그
            if (cachedAuth != null && !cachedAuth.isValid()) {
                logger.info("캐시된 인증이 만료되어 재인증 수행: strategy=${cachedAuth.strategy}")
            }

            // 3단계: 새로운 인증 수행
            logger.info("새로운 인증 수행 중...")
            val newAuthResult = try {
                basicStrategy.authenticate()
            } catch (e: Exception) {
                logger.error("인증 전략 실행 실패", e)
                // 인증 실패 시 캐시 초기화
                authResult = null
                throw Exception("인증 수행 중 예외 발생: ${e.message}", e)
            }

            // 4단계: 결과 캐싱
            authResult = newAuthResult
            logger.info("인증 성공 및 캐싱 완료: strategy=${newAuthResult.strategy}")
            newAuthResult
        }
    }

    /**
     * 캐시된 CRUMB 토큰을 획득합니다.
     *
     * 필요한 경우 먼저 인증을 수행하고, 성공한 인증 결과에서 CRUMB을 추출합니다.
     *
     * @return 유효한 CRUMB 토큰 문자열
     * @throws Exception 인증 실패 시
     */
    suspend fun getCrumb(): String {
        val authResult = authenticate()
        return authResult.crumb
    }

    /**
     * HttpRequestBuilder에 CRUMB을 쿼리 파라미터로 추가합니다.
     *
     * Yahoo Finance API 호출 시 모든 요청에는 crumb 파라미터가 필요합니다.
     * 이 메서드는 그 과정을 간소화합니다.
     *
     * 사용 예시:
     * ```kotlin
     * val request = HttpRequestBuilder().apply {
     *     url("https://query1.finance.yahoo.com/v1/finance/search")
     *     authenticator.applyAuth(this)
     * }
     * ```
     *
     * @param builder 수정할 HttpRequestBuilder
     * @throws Exception 인증 실패 시
     */
    suspend fun applyAuth(builder: HttpRequestBuilder) {
        val crumb = getCrumb()
        builder.url.parameters.append("crumb", crumb)
        logger.debug("CRUMB 파라미터 추가 완료")
    }

    /**
     * 캐시된 인증 결과를 초기화합니다.
     *
     * 인증 오류가 발생했거나 강제로 재인증이 필요한 경우에 사용됩니다.
     * 이 메서드는 비용이 적으므로 안전하게 호출할 수 있습니다.
     *
     * 호출 예시:
     * ```kotlin
     * try {
     *     // API 호출
     *     client.get(url)
     * } catch (e: AuthenticationException) {
     *     authenticator.reset() // 재인증을 위해 캐시 초기화
     *     throw e
     * }
     * ```
     */
    suspend fun reset() {
        lock.withLock {
            val oldAuth = authResult
            authResult = null
            if (oldAuth != null) {
                logger.info("인증 캐시 리셋: strategy=${oldAuth.strategy}")
            } else {
                logger.debug("인증 캐시가 이미 비어있음")
            }
        }
    }

    /**
     * 현재 캐시된 인증 상태를 반환합니다.
     *
     * 주로 디버깅 및 모니터링용으로 사용됩니다.
     *
     * @return 캐시된 인증 결과, 없으면 null
     */
    suspend fun getCachedAuth(): AuthResult? {
        return lock.withLock {
            authResult
        }
    }
}
