package com.ulalax.ufc.internal

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.currentCoroutineContext
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("HttpResponseExtensions")

/**
 * HttpResponse body를 문자열로 읽으면서 ResponseRecordingContext에 저장합니다.
 *
 * Context가 없으면 저장하지 않고 그냥 반환합니다 (호환성).
 *
 * @return 응답 body 문자열
 */
suspend fun HttpResponse.bodyAsTextWithRecording(): String {
    val responseBody = this.bodyAsText()

    try {
        val recordingContext = currentCoroutineContext()[ResponseRecordingContext]
        if (recordingContext != null) {
            recordingContext.setResponseBody(responseBody)
            logger.trace("Response recorded to context (length: ${responseBody.length})")
        }
    } catch (e: Exception) {
        logger.trace("ResponseRecordingContext not found or error: ${e.message}")
        // Context가 없어도 무시 (정상 동작)
    }

    return responseBody
}
