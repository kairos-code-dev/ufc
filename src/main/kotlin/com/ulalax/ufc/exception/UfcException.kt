package com.ulalax.ufc.exception

/**
 * UFC 애플리케이션의 기본 예외 클래스입니다.
 *
 * 모든 UFC 애플리케이션 예외는 이 클래스를 상속합니다.
 * ErrorCode를 통해 구조화된 에러 정보를 관리합니다.
 *
 * @property errorCode 에러 코드
 * @property message 에러 메시지
 * @property cause 원인 예외
 * @property metadata 추가 메타데이터 (Key-Value)
 */
open class UfcException(
    val errorCode: ErrorCode,
    message: String? = null,
    cause: Throwable? = null,
    val metadata: Map<String, Any> = emptyMap()
) : RuntimeException(message ?: errorCode.message, cause) {

    /**
     * 메타데이터에서 특정 키의 값을 조회합니다.
     *
     * @param key 조회할 메타데이터 키
     * @return 해당 키의 값, 없으면 null
     */
    inline fun <reified T> getMeta(key: String): T? {
        return metadata[key] as? T
    }

    /**
     * 메타데이터 항목을 추가한 새로운 UfcException을 생성합니다.
     *
     * @param key 메타데이터 키
     * @param value 메타데이터 값
     * @return 새로운 UfcException 인스턴스
     */
    fun withMeta(key: String, value: Any): UfcException {
        val newMetadata = metadata.toMutableMap().apply { put(key, value) }
        return UfcException(errorCode, message, cause, newMetadata)
    }

    /**
     * 여러 메타데이터 항목을 추가한 새로운 UfcException을 생성합니다.
     *
     * @param newMetadata 추가할 메타데이터
     * @return 새로운 UfcException 인스턴스
     */
    fun withMeta(newMetadata: Map<String, Any>): UfcException {
        val mergedMetadata = metadata.toMutableMap().apply { putAll(newMetadata) }
        return UfcException(errorCode, message, cause, mergedMetadata)
    }

    override fun toString(): String {
        return "UfcException(code=${errorCode.code}, message=${errorCode.message}, " +
                "customMessage=$message, metadata=$metadata)"
    }
}

/**
 * API 호출 관련 예외입니다.
 *
 * @property statusCode HTTP 상태 코드
 * @property responseBody API 응답 본문
 */
class ApiException(
    errorCode: ErrorCode = ErrorCode.EXTERNAL_API_ERROR,
    message: String? = null,
    val statusCode: Int? = null,
    val responseBody: String? = null,
    cause: Throwable? = null,
    metadata: Map<String, Any> = emptyMap()
) : UfcException(errorCode, message, cause, metadata) {
    constructor(
        message: String,
        statusCode: Int? = null,
        responseBody: String? = null,
        cause: Throwable? = null
    ) : this(
        errorCode = ErrorCode.EXTERNAL_API_ERROR,
        message = message,
        statusCode = statusCode,
        responseBody = responseBody,
        cause = cause
    )
}

/**
 * 데이터 파싱 관련 예외입니다.
 *
 * @property sourceData 파싱 실패한 원본 데이터
 */
class DataParsingException(
    errorCode: ErrorCode = ErrorCode.JSON_PARSING_ERROR,
    message: String? = null,
    val sourceData: String? = null,
    cause: Throwable? = null,
    metadata: Map<String, Any> = emptyMap()
) : UfcException(errorCode, message, cause, metadata) {
    constructor(
        message: String,
        sourceData: String? = null,
        cause: Throwable? = null
    ) : this(
        errorCode = ErrorCode.JSON_PARSING_ERROR,
        message = message,
        sourceData = sourceData,
        cause = cause
    )
}

/**
 * 설정 관련 예외입니다.
 */
class ConfigException(
    errorCode: ErrorCode = ErrorCode.CONFIGURATION_ERROR,
    message: String? = null,
    cause: Throwable? = null,
    metadata: Map<String, Any> = emptyMap()
) : UfcException(errorCode, message, cause, metadata) {
    constructor(
        message: String,
        cause: Throwable? = null
    ) : this(
        errorCode = ErrorCode.CONFIGURATION_ERROR,
        message = message,
        cause = cause
    )
}

/**
 * 유효성 검사 실패 예외입니다.
 *
 * @property field 유효성 검사 실패한 필드명
 */
class ValidationException(
    errorCode: ErrorCode = ErrorCode.INVALID_PARAMETER,
    message: String? = null,
    val field: String? = null,
    cause: Throwable? = null,
    metadata: Map<String, Any> = emptyMap()
) : UfcException(errorCode, message, cause, metadata) {
    constructor(
        message: String,
        field: String? = null,
        cause: Throwable? = null
    ) : this(
        errorCode = ErrorCode.INVALID_PARAMETER,
        message = message,
        field = field,
        cause = cause
    )
}

/**
 * 네트워크 관련 예외입니다.
 */
class NetworkException(
    errorCode: ErrorCode,
    message: String? = null,
    cause: Throwable? = null,
    metadata: Map<String, Any> = emptyMap()
) : UfcException(errorCode, message, cause, metadata)

/**
 * Rate Limiting 관련 예외입니다.
 *
 * @property retryAfterSeconds 재시도 권장 시간 (초)
 */
class RateLimitException(
    errorCode: ErrorCode = ErrorCode.RATE_LIMIT_EXCEEDED,
    message: String? = null,
    val retryAfterSeconds: Long? = null,
    cause: Throwable? = null,
    metadata: Map<String, Any> = emptyMap()
) : UfcException(errorCode, message, cause, metadata)
