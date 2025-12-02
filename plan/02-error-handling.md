# UFC Error Handling - ErrorCode 기반 예외 시스템

## 문서 정보
- **버전**: 2.0.0
- **최종 수정일**: 2025-12-01
- **작성자**: Claude Code (Software Architect)
- **문서 상태**: Active

---

## 1. ErrorCode 기반 시스템 개요

### 1.1 설계 원칙

KFC의 ErrorCode 시스템을 참조하여 UFC의 에러 처리 시스템을 설계합니다.

**기존 방식 (여러 예외 클래스):**
```kotlin
// ❌ 기존: 예외 클래스가 너무 많음
class NetworkException : UFCException()
class AuthenticationException : UFCException()
class RateLimitException : UFCException()
// ... 10+ 예외 클래스
```

**새로운 방식 (ErrorCode + 단일 예외):**
```kotlin
// ✅ 신규: 단일 예외 + ErrorCode
class UFCException(
    val errorCode: ErrorCode,
    message: String,
    cause: Throwable? = null,
    val metadata: Map<String, Any> = emptyMap()
) : Exception(message, cause)

enum class ErrorCode { ... }
```

### 1.2 장점

1. **단순성**
   - 하나의 예외 클래스만 처리
   - ErrorCode로 세밀한 분기 가능

2. **확장성**
   - 새로운 에러 코드 추가 쉬움
   - 기존 코드 수정 불필요

3. **Metadata 활용**
   - 컨텍스트 정보 전달 (symbol, retryAfter 등)
   - 로깅 및 모니터링 용이

4. **일관성**
   - 모든 예외 처리 패턴 통일
   - 코드베이스 전체 일관성 유지

---

## 2. ErrorCode 정의

### 2.1 코드 체계

ErrorCode는 카테고리별로 번호대를 구분합니다:

- **1000번대**: 네트워크 오류
- **2000번대**: 인증 오류
- **3000번대**: Rate Limiting 오류
- **4000번대**: 데이터 오류
- **5000번대**: 파싱 오류
- **6000번대**: 파라미터 오류
- **7000번대**: 서버 오류
- **9000번대**: 기타 오류

### 2.2 ErrorCode Enum

```kotlin
/**
 * UFC 에러 코드
 *
 * 모든 예외는 에러 코드와 메시지로 구성됩니다.
 */
enum class ErrorCode(val code: Int, val message: String) {

    // ================================
    // 1000번대: 네트워크 오류
    // ================================

    /**
     * 네트워크 연결 실패
     */
    NETWORK_CONNECTION_FAILED(1001, "네트워크 연결에 실패했습니다"),

    /**
     * 네트워크 타임아웃
     */
    NETWORK_TIMEOUT(1002, "네트워크 요청 시간이 초과되었습니다"),

    /**
     * HTTP 요청 실패
     */
    HTTP_REQUEST_FAILED(1003, "HTTP 요청이 실패했습니다"),

    /**
     * HTTP 오류 응답 (4xx, 5xx)
     */
    HTTP_ERROR_RESPONSE(1004, "HTTP 요청이 오류 응답을 반환했습니다"),

    /**
     * SSL/TLS 오류
     */
    SSL_ERROR(1005, "SSL/TLS 연결에 실패했습니다"),

    // ================================
    // 2000번대: 인증 오류
    // ================================

    /**
     * 인증 실패
     */
    AUTH_FAILED(2001, "인증에 실패했습니다"),

    /**
     * Cookie 획득 실패
     */
    COOKIE_ACQUISITION_FAILED(2002, "Cookie 획득에 실패했습니다"),

    /**
     * Crumb 획득 실패
     */
    CRUMB_ACQUISITION_FAILED(2003, "Crumb 획득에 실패했습니다"),

    /**
     * Crumb 만료
     */
    CRUMB_EXPIRED(2004, "Crumb이 만료되었습니다"),

    /**
     * API Key 누락
     */
    MISSING_API_KEY(2005, "API Key가 제공되지 않았습니다"),

    /**
     * API Key 무효
     */
    INVALID_API_KEY(2006, "API Key가 유효하지 않습니다"),

    // ================================
    // 3000번대: Rate Limiting 오류
    // ================================

    /**
     * Rate Limit 초과
     */
    RATE_LIMITED(3001, "API 호출 제한을 초과했습니다"),

    /**
     * Too Many Requests (429)
     */
    TOO_MANY_REQUESTS(3002, "요청이 너무 많습니다"),

    // ================================
    // 4000번대: 데이터 오류
    // ================================

    /**
     * 데이터 없음
     */
    NO_DATA_AVAILABLE(4001, "요청한 데이터가 존재하지 않습니다"),

    /**
     * 심볼 찾을 수 없음
     */
    INVALID_SYMBOL(4002, "유효하지 않은 심볼입니다"),

    /**
     * Series ID 무효
     */
    INVALID_SERIES_ID(4003, "유효하지 않은 Series ID입니다"),

    /**
     * 빈 응답
     */
    EMPTY_RESPONSE(4004, "응답 데이터가 비어있습니다"),

    // ================================
    // 5000번대: 파싱 오류
    // ================================

    /**
     * JSON 파싱 실패
     */
    JSON_PARSE_ERROR(5001, "JSON 파싱에 실패했습니다"),

    /**
     * XML 파싱 실패
     */
    XML_PARSE_ERROR(5002, "XML 파싱에 실패했습니다"),

    /**
     * 직렬화 오류
     */
    SERIALIZATION_ERROR(5003, "데이터 직렬화에 실패했습니다"),

    /**
     * 잘못된 데이터 형식
     */
    INVALID_DATA_FORMAT(5004, "데이터 형식이 올바르지 않습니다"),

    /**
     * 필수 필드 누락
     */
    REQUIRED_FIELD_MISSING(5005, "필수 필드가 누락되었습니다"),

    // ================================
    // 6000번대: 파라미터 오류
    // ================================

    /**
     * 잘못된 파라미터
     */
    INVALID_PARAMETER(6001, "파라미터가 올바르지 않습니다"),

    /**
     * 잘못된 날짜 범위
     */
    INVALID_DATE_RANGE(6002, "날짜 범위가 올바르지 않습니다"),

    /**
     * 잘못된 Interval
     */
    INVALID_INTERVAL(6003, "Interval이 올바르지 않습니다"),

    /**
     * 잘못된 Period
     */
    INVALID_PERIOD(6004, "Period가 올바르지 않습니다"),

    // ================================
    // 7000번대: 서버 오류
    // ================================

    /**
     * 서버 오류 (5xx)
     */
    SERVER_ERROR(7001, "서버에서 오류가 발생했습니다"),

    /**
     * 서비스 이용 불가
     */
    SERVICE_UNAVAILABLE(7002, "서비스를 이용할 수 없습니다"),

    // ================================
    // 9000번대: 기타 오류
    // ================================

    /**
     * 알 수 없는 오류
     */
    UNKNOWN_ERROR(9999, "알 수 없는 오류가 발생했습니다");

    override fun toString(): String = "[$code] $message"
}
```

---

## 3. UFCException 클래스

### 3.1 클래스 정의

```kotlin
/**
 * UFC 라이브러리의 통합 예외 클래스
 *
 * ErrorCode 기반 예외 처리 시스템을 제공합니다.
 *
 * @property errorCode 에러 코드
 * @property metadata 추가 컨텍스트 정보
 * @param message 에러 메시지 (기본값: ErrorCode의 메시지)
 * @param cause 원인이 되는 예외
 *
 * ## 사용 예시
 *
 * ### 예외 발생
 * ```kotlin
 * throw UFCException(ErrorCode.INVALID_SYMBOL, metadata = mapOf("symbol" to "INVALID"))
 * throw UFCException(ErrorCode.JSON_PARSE_ERROR, cause = e)
 * ```
 *
 * ### 예외 처리
 * ```kotlin
 * try {
 *     val data = ufc.yahoo.etf("SPY").fetchFundsData()
 * } catch (e: UFCException) {
 *     when (e.errorCode) {
 *         ErrorCode.NOT_FOUND -> {
 *             val symbol = e.metadata["symbol"]
 *             println("Symbol $symbol not found")
 *         }
 *         ErrorCode.RATE_LIMITED -> {
 *             val retryAfter = e.metadata["retryAfter"] as? Long ?: 60
 *             delay(retryAfter * 1000)
 *             // retry
 *         }
 *         else -> println("Error: ${e.message}")
 *     }
 * }
 * ```
 */
class UFCException(
    val errorCode: ErrorCode,
    message: String = errorCode.message,
    cause: Throwable? = null,
    val metadata: Map<String, Any> = emptyMap()
) : Exception(message, cause) {

    /**
     * 에러 코드 번호
     */
    val code: Int get() = errorCode.code

    /**
     * 메타데이터 조회 헬퍼
     */
    inline fun <reified T> getMeta(key: String): T? = metadata[key] as? T

    override fun toString(): String {
        val metaStr = if (metadata.isNotEmpty()) {
            ", metadata=$metadata"
        } else {
            ""
        }
        return "UFCException(errorCode=$errorCode$metaStr)"
    }
}
```

---

## 4. 에러 처리 패턴

### 4.1 기본 패턴

```kotlin
// 예외 발생
throw UFCException(
    errorCode = ErrorCode.INVALID_SYMBOL,
    metadata = mapOf("symbol" to symbol)
)

// 예외 처리
try {
    val data = fetchData()
} catch (e: UFCException) {
    when (e.errorCode) {
        ErrorCode.INVALID_SYMBOL -> handleInvalidSymbol(e)
        ErrorCode.RATE_LIMITED -> handleRateLimit(e)
        else -> handleUnknownError(e)
    }
}
```

### 4.2 Metadata 활용

**Network Timeout:**
```kotlin
throw UFCException(
    errorCode = ErrorCode.NETWORK_TIMEOUT,
    metadata = mapOf(
        "url" to url,
        "timeout" to timeoutMs
    )
)
```

**Rate Limit:**
```kotlin
throw UFCException(
    errorCode = ErrorCode.RATE_LIMITED,
    metadata = mapOf(
        "retryAfter" to retryAfterSeconds,
        "limit" to rateLimit
    )
)
```

**Invalid Symbol:**
```kotlin
throw UFCException(
    errorCode = ErrorCode.INVALID_SYMBOL,
    metadata = mapOf(
        "symbol" to symbol,
        "message" to responseMessage
    )
)
```

### 4.3 HTTP 에러 변환

```kotlin
/**
 * HTTP 응답을 UFCException으로 변환
 */
fun HttpResponse.toUFCException(): UFCException {
    return when (status.value) {
        401, 403 -> UFCException(
            errorCode = ErrorCode.AUTH_FAILED,
            metadata = mapOf("status" to status.value)
        )

        404 -> UFCException(
            errorCode = ErrorCode.NO_DATA_AVAILABLE,
            metadata = mapOf("url" to request.url.toString())
        )

        429 -> {
            val retryAfter = headers["Retry-After"]?.toLongOrNull() ?: 60
            UFCException(
                errorCode = ErrorCode.TOO_MANY_REQUESTS,
                metadata = mapOf("retryAfter" to retryAfter)
            )
        }

        in 500..599 -> UFCException(
            errorCode = ErrorCode.SERVER_ERROR,
            metadata = mapOf("status" to status.value)
        )

        else -> UFCException(
            errorCode = ErrorCode.HTTP_ERROR_RESPONSE,
            metadata = mapOf("status" to status.value)
        )
    }
}
```

---

## 5. 재시도 정책

### 5.1 재시도 가능 에러

다음 ErrorCode는 재시도 가능합니다:

- `NETWORK_TIMEOUT`
- `NETWORK_CONNECTION_FAILED`
- `TOO_MANY_REQUESTS`
- `SERVER_ERROR`
- `SERVICE_UNAVAILABLE`

### 5.2 재시도 로직

```kotlin
/**
 * 재시도 가능 여부
 */
fun ErrorCode.isRetryable(): Boolean {
    return this in setOf(
        ErrorCode.NETWORK_TIMEOUT,
        ErrorCode.NETWORK_CONNECTION_FAILED,
        ErrorCode.TOO_MANY_REQUESTS,
        ErrorCode.SERVER_ERROR,
        ErrorCode.SERVICE_UNAVAILABLE
    )
}

/**
 * 재시도 로직
 */
suspend fun <T> withRetry(
    maxRetries: Int = 3,
    initialDelay: Long = 1000,
    maxDelay: Long = 10000,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: UFCException) {
            if (!e.errorCode.isRetryable() || attempt == maxRetries - 1) {
                throw e
            }

            // Rate Limit의 경우 Retry-After 준수
            val delayMs = if (e.errorCode == ErrorCode.RATE_LIMITED) {
                val retryAfter = e.getMeta<Long>("retryAfter") ?: 60
                retryAfter * 1000
            } else {
                currentDelay
            }

            delay(delayMs)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }
    // Unreachable
    throw UFCException(ErrorCode.UNKNOWN_ERROR)
}
```

---

## 6. 로깅 전략

### 6.1 로깅 레벨

| ErrorCode 카테고리 | 로깅 레벨 |
|-------------------|---------|
| 네트워크 오류 (1000번대) | WARN |
| 인증 오류 (2000번대) | ERROR |
| Rate Limiting (3000번대) | WARN |
| 데이터 오류 (4000번대) | INFO |
| 파싱 오류 (5000번대) | ERROR |
| 파라미터 오류 (6000번대) | WARN |
| 서버 오류 (7000번대) | ERROR |
| 기타 (9000번대) | ERROR |

### 6.2 로깅 구현

```kotlin
/**
 * UFCException 로깅
 */
fun UFCException.log(logger: Logger) {
    val level = when (errorCode.code) {
        in 1000..1999 -> LogLevel.WARN
        in 2000..2999 -> LogLevel.ERROR
        in 3000..3999 -> LogLevel.WARN
        in 4000..4999 -> LogLevel.INFO
        in 5000..5999 -> LogLevel.ERROR
        in 6000..6999 -> LogLevel.WARN
        in 7000..7999 -> LogLevel.ERROR
        else -> LogLevel.ERROR
    }

    val message = buildString {
        append("[${errorCode.code}] ${errorCode.message}")
        if (metadata.isNotEmpty()) {
            append(" | metadata=$metadata")
        }
    }

    when (level) {
        LogLevel.ERROR -> logger.error(message, this)
        LogLevel.WARN -> logger.warn(message, this)
        LogLevel.INFO -> logger.info(message)
        else -> logger.debug(message)
    }
}
```

---

## 7. 사용 예시

### 7.1 Yahoo Finance

```kotlin
// YahooFinanceSource 구현
internal class YahooFinanceSourceImpl : YahooFinanceSource {

    suspend fun fetchChart(symbol: String): ChartResponse {
        try {
            val response = httpClient.get(url)

            if (response.status != HttpStatusCode.OK) {
                throw response.toUFCException()
            }

            return response.body()

        } catch (e: SerializationException) {
            throw UFCException(
                errorCode = ErrorCode.JSON_PARSE_ERROR,
                cause = e,
                metadata = mapOf("symbol" to symbol)
            )
        } catch (e: IOException) {
            throw UFCException(
                errorCode = ErrorCode.NETWORK_CONNECTION_FAILED,
                cause = e
            )
        } catch (e: UFCException) {
            throw e
        } catch (e: Exception) {
            throw UFCException(
                errorCode = ErrorCode.UNKNOWN_ERROR,
                cause = e
            )
        }
    }
}
```

### 7.2 FRED

```kotlin
// FREDSource 구현
internal class FREDSourceImpl(
    private val apiKey: String?
) : FREDSource {

    override suspend fun initialize() {
        if (apiKey == null) {
            throw UFCException(ErrorCode.MISSING_API_KEY)
        }
    }

    suspend fun getSeries(seriesId: String): Series {
        try {
            val response = httpClient.get(url) {
                parameter("series_id", seriesId)
                parameter("api_key", apiKey)
            }

            if (response.status != HttpStatusCode.OK) {
                throw response.toUFCException()
            }

            val data: SeriesResponse = response.body()

            if (data.observations.isEmpty()) {
                throw UFCException(
                    errorCode = ErrorCode.NO_DATA_AVAILABLE,
                    metadata = mapOf("seriesId" to seriesId)
                )
            }

            return data.toSeries()

        } catch (e: UFCException) {
            throw e
        } catch (e: Exception) {
            throw UFCException(
                errorCode = ErrorCode.UNKNOWN_ERROR,
                cause = e
            )
        }
    }
}
```

---

## 8. 참고 자료

- **KFC ErrorCode System**: /home/ulalax/project/kairos/kfc/src/main/kotlin/dev/kairoscode/kfc/exception/
- **Effective Error Handling in Kotlin**: https://kotlinlang.org/docs/exceptions.html

---

**다음 문서**: [03-yahoo-finance-core.md](./03-yahoo-finance-core.md)
