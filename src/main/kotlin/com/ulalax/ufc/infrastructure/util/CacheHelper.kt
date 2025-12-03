package com.ulalax.ufc.infrastructure.util

import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

/**
 * 인메모리 캐싱 유틸리티
 *
 * 인터페이스가 불필요한 이유:
 * - 구현체 하나뿐 (교체 가능성 없음)
 * - 인메모리로 충분히 빠름 (테스트 격리 불필요)
 * - YAGNI 원칙 준수
 *
 * 사용 예시:
 * ```kotlin
 * val cache = CacheHelper()
 *
 * // 캐시를 활용한 데이터 조회
 * val data = cache.getOrPut("key:symbol", ttl = 60.seconds) {
 *     // 캐시 미스 시 실행되는 producer 함수
 *     expensiveApiCall()
 * }
 *
 * // 캐시 초기화
 * cache.clear()
 * ```
 *
 * 스레드 안전: ConcurrentHashMap을 사용하여 멀티스레드 환경에서 안전합니다.
 */
class CacheHelper {
    private val cache = ConcurrentHashMap<String, CachedValue<Any>>()

    /**
     * 캐시에서 값을 조회하거나, 없으면 producer를 실행하여 캐시에 저장
     *
     * @param key 캐시 키
     * @param ttl Time-To-Live (캐시 유효 시간)
     * @param producer 캐시 미스 시 실행할 함수
     * @return 캐시된 값 또는 새로 생성된 값
     */
    suspend fun <T> getOrPut(
        key: String,
        ttl: Duration,
        producer: suspend () -> T
    ): T {
        // 캐시 확인
        cache[key]?.let { cached ->
            if (!cached.isExpired()) {
                @Suppress("UNCHECKED_CAST")
                return cached.value as T
            }
        }

        // 캐시 미스 또는 만료: producer 실행
        val value = producer()
        val expiresAt = System.currentTimeMillis() + ttl.inWholeMilliseconds
        cache[key] = CachedValue(value as Any, expiresAt)
        return value
    }

    /**
     * 특정 키의 캐시를 무효화
     *
     * @param key 무효화할 캐시 키
     */
    fun invalidate(key: String) {
        cache.remove(key)
    }

    /**
     * 패턴에 매칭되는 모든 캐시 무효화
     *
     * @param keyPattern 키 패턴 (예: "price:*")
     */
    fun invalidatePattern(keyPattern: String) {
        val regex = keyPattern.replace("*", ".*").toRegex()
        cache.keys.filter { regex.matches(it) }.forEach { cache.remove(it) }
    }

    /**
     * 모든 캐시 초기화
     */
    fun clear() {
        cache.clear()
    }

    /**
     * 캐시 크기 반환
     *
     * @return 현재 캐시에 저장된 항목 수
     */
    fun size(): Int = cache.size
}

/**
 * 캐시된 값과 만료 시간을 포함하는 내부 데이터 클래스
 *
 * @property value 캐시된 값
 * @property expiresAt 만료 시각 (Unix timestamp in milliseconds)
 */
private data class CachedValue<T>(
    val value: T,
    val expiresAt: Long
) {
    /**
     * 캐시가 만료되었는지 확인
     *
     * @return true면 만료됨, false면 유효함
     */
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
}
