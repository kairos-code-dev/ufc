package com.ulalax.ufc.internal

import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Coroutine Context Element for recording HTTP response bodies.
 *
 * Thread-safe하게 마지막 응답 body를 저장합니다.
 * liveTestWithRecording()에서 withContext()와 함께 사용됩니다.
 */
class ResponseRecordingContext(
    private val responseBodyRef: AtomicReference<String?> = AtomicReference(null)
) : AbstractCoroutineContextElement(ResponseRecordingContext) {

    companion object Key : CoroutineContext.Key<ResponseRecordingContext>

    /**
     * 응답 body를 저장합니다.
     * 여러 번 호출되면 마지막 값으로 덮어씁니다.
     */
    fun setResponseBody(body: String) {
        responseBodyRef.set(body)
    }

    /**
     * 저장된 응답 body를 반환합니다.
     */
    fun getResponseBody(): String? = responseBodyRef.get()

    /**
     * 저장된 응답을 초기화합니다.
     */
    fun clear() {
        responseBodyRef.set(null)
    }
}
