package com.ulalax.ufc.domain.exception

/**
 * Base exception class for the UFC application.
 *
 * All UFC application exceptions inherit from this class.
 * Manages structured error information through ErrorCode.
 *
 * @property errorCode The error code
 * @property message The error message
 * @property cause The cause exception
 * @property metadata Additional metadata (key-value pairs)
 */
open class UfcException(
    val errorCode: ErrorCode,
    message: String? = null,
    cause: Throwable? = null,
    val metadata: Map<String, Any> = emptyMap()
) : RuntimeException(message ?: errorCode.message, cause) {

    /**
     * Retrieves a value from metadata for a specific key.
     *
     * @param key The metadata key to look up
     * @return The value for the key, or null if not found
     */
    inline fun <reified T> getMeta(key: String): T? {
        return metadata[key] as? T
    }

    /**
     * Creates a new UfcException with an additional metadata entry.
     *
     * @param key The metadata key
     * @param value The metadata value
     * @return A new UfcException instance
     */
    fun withMeta(key: String, value: Any): UfcException {
        val newMetadata = metadata.toMutableMap().apply { put(key, value) }
        return UfcException(errorCode, message, cause, newMetadata)
    }

    /**
     * Creates a new UfcException with multiple additional metadata entries.
     *
     * @param newMetadata The metadata to add
     * @return A new UfcException instance
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
 * Exception for API call-related errors.
 *
 * @property statusCode HTTP status code
 * @property responseBody API response body
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
 * Exception for data parsing-related errors.
 *
 * @property sourceData The original data that failed to parse
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
 * Exception for configuration-related errors.
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
 * Exception for validation failures.
 *
 * @property field The field name that failed validation
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
 * Exception for network-related errors.
 */
class NetworkException(
    errorCode: ErrorCode,
    message: String? = null,
    cause: Throwable? = null,
    metadata: Map<String, Any> = emptyMap()
) : UfcException(errorCode, message, cause, metadata)

/**
 * Exception for rate limiting-related errors.
 *
 * @property retryAfterSeconds Recommended retry time in seconds
 */
class RateLimitException(
    errorCode: ErrorCode = ErrorCode.RATE_LIMIT_EXCEEDED,
    message: String? = null,
    val retryAfterSeconds: Long? = null,
    cause: Throwable? = null,
    metadata: Map<String, Any> = emptyMap()
) : UfcException(errorCode, message, cause, metadata)
