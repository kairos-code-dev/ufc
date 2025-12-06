package com.ulalax.ufc.infrastructure.common.recording

import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * HTTP 응답 레코딩을 위한 Coroutine Context Element
 *
 * API 응답을 자동으로 캡처하여 테스트 목적으로 저장할 수 있게 해주는 컨텍스트입니다.
 * 주로 API 호출의 실제 응답을 기록하여 테스트 fixture로 활용하거나,
 * 디버깅 시 실제 응답 데이터를 확인하는 용도로 사용됩니다.
 *
 * 단일 응답 body만 저장하며, 각 API 호출 시 덮어쓰기됩니다.
 * Thread-safe한 AtomicReference를 사용하여 동시성 문제를 방지합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val recordingContext = ResponseRecordingContext()
 * withContext(recordingContext) {
 *     kfcClient.funds.getList()
 *     val capturedResponse = recordingContext.getResponseBody()
 *     // capturedResponse를 테스트 fixture로 저장
 * }
 * ```
 */
class ResponseRecordingContext(
    private val responseBodyRef: AtomicReference<String?> = AtomicReference(null),
) : AbstractCoroutineContextElement(ResponseRecordingContext) {
    companion object Key : CoroutineContext.Key<ResponseRecordingContext>

    /**
     * 마지막 응답 body를 저장합니다.
     * 이전 응답은 자동으로 덮어씌워집니다.
     *
     * @param body 저장할 HTTP 응답 body (JSON 문자열)
     */
    fun setResponseBody(body: String) {
        responseBodyRef.set(body)
    }

    /**
     * 저장된 마지막 응답 body를 가져옵니다.
     *
     * @return 저장된 응답 body, 없으면 null
     */
    fun getResponseBody(): String? = responseBodyRef.get()

    /**
     * 저장된 응답 body를 초기화합니다.
     * 여러 API 호출을 순차적으로 기록할 때, 각 호출 전에 clear()를 호출하면
     * 이전 응답과 혼동되지 않습니다.
     */
    fun clear() {
        responseBodyRef.set(null)
    }
}
