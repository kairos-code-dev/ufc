package com.ulalax.ufc.internal.fakes

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * 테스트용 JSON 레코딩 응답을 관리하는 저장소입니다.
 *
 * 실제 API 호출 대신 사전에 기록된 JSON 파일을 로드하여 제공하므로:
 * - 네트워크 연결이 필요 없음
 * - 테스트가 빠르게 실행됨
 * - 일관된 테스트 응답 보장
 * - 실제 API 응답 형식을 정확히 테스트 가능
 *
 * 사용 예시:
 * ```
 * val repository = RecordedResponseRepository()
 *
 * // JSON 파일 로드
 * val crumbResponse = repository.loadResponse(
 *     path = "src/test/resources/yahoo",
 *     fileName = "crumb_response.json"
 * )
 *
 * // 응답 캐싱
 * repository.cacheResponse("yahoo:crumb", crumbResponse)
 *
 * // 캐시된 응답 조회
 * val cached = repository.getResponse("yahoo:crumb")
 *
 * // 캐시 초기화
 * repository.clearCache()
 * ```
 */
class RecordedResponseRepository {

    // JSON 응답 캐시 (key -> response)
    private val responseCache = mutableMapOf<String, String>()

    // 로드된 파일 경로 추적 (디버깅/로깅용)
    private val loadedFiles = mutableSetOf<String>()

    /**
     * 지정된 경로의 JSON 파일을 로드합니다.
     *
     * 파일이 존재하지 않으면 IOException을 발생시킵니다.
     * 로드된 파일은 loadedFiles에 추가되어 추적됩니다.
     *
     * @param path 파일 경로 (예: "src/test/resources/yahoo")
     * @param fileName 파일명 (예: "crumb_response.json")
     * @return 로드된 파일의 문자열 내용
     * @throws IllegalArgumentException 경로가 비어있거나 null인 경우
     * @throws IllegalArgumentException 파일명이 비어있거나 null인 경우
     * @throws java.io.FileNotFoundException 파일이 존재하지 않는 경우
     * @throws java.io.IOException 파일 읽기 실패 시
     */
    fun loadResponse(path: String, fileName: String): String {
        require(path.isNotBlank()) { "경로는 비어있을 수 없습니다" }
        require(fileName.isNotBlank()) { "파일명은 비어있을 수 없습니다" }

        val filePath = Paths.get(path, fileName)
        val file = filePath.toFile()

        if (!file.exists()) {
            throw IllegalArgumentException(
                "응답 파일이 존재하지 않습니다: ${filePath.toAbsolutePath()}"
            )
        }

        if (!file.isFile) {
            throw IllegalArgumentException(
                "경로가 파일이 아닙니다: ${filePath.toAbsolutePath()}"
            )
        }

        return try {
            val content = Files.readString(filePath)
            loadedFiles.add(filePath.toAbsolutePath().toString())
            content
        } catch (e: Exception) {
            throw RuntimeException("파일 읽기 실패: ${filePath.toAbsolutePath()}", e)
        }
    }

    /**
     * 지정된 경로의 텍스트 파일을 로드합니다.
     *
     * loadResponse()와 동일하지만 이름이 더 명확합니다.
     *
     * @param path 파일 경로
     * @param fileName 파일명
     * @return 로드된 파일의 문자열 내용
     * @throws IllegalArgumentException 입력이 유효하지 않은 경우
     * @throws java.io.IOException 파일 읽기 실패 시
     */
    fun loadFile(path: String, fileName: String): String {
        return loadResponse(path, fileName)
    }

    /**
     * 응답을 캐시에 저장합니다.
     *
     * 캐시 키와 응답을 저장하여 같은 키로 여러 번 조회할 때 파일 I/O를 피할 수 있습니다.
     *
     * @param key 캐시 키
     * @param response 저장할 응답 문자열
     * @throws IllegalArgumentException 키가 비어있는 경우
     */
    fun cacheResponse(key: String, response: String) {
        require(key.isNotBlank()) { "캐시 키는 비어있을 수 없습니다" }
        responseCache[key] = response
    }

    /**
     * 캐시에서 응답을 조회합니다.
     *
     * @param key 캐시 키
     * @return 캐시된 응답 또는 null (없는 경우)
     */
    fun getResponse(key: String): String? {
        return responseCache[key]
    }

    /**
     * 특정 키의 응답이 캐시되어 있는지 확인합니다.
     *
     * @param key 조회할 키
     * @return 캐시되어 있으면 true
     */
    fun isCached(key: String): Boolean {
        return key in responseCache
    }

    /**
     * 현재 캐시에 저장된 모든 키를 반환합니다.
     *
     * @return 캐시 키 목록
     */
    fun getCachedKeys(): List<String> {
        return responseCache.keys.toList()
    }

    /**
     * 현재 캐시된 응답의 개수를 반환합니다.
     *
     * @return 캐시 크기
     */
    fun getCacheSize(): Int {
        return responseCache.size
    }

    /**
     * 모든 캐시를 초기화합니다.
     *
     * 테스트 격리(Test Isolation)를 위해 각 테스트 후 호출하는 것을 권장합니다.
     */
    fun clearCache() {
        responseCache.clear()
    }

    /**
     * 로드된 모든 파일 경로를 반환합니다.
     *
     * 디버깅 및 로깅 목적으로 사용합니다.
     *
     * @return 로드된 파일 경로 목록
     */
    fun getLoadedFiles(): List<String> {
        return loadedFiles.toList()
    }

    /**
     * 로드된 파일 목록을 초기화합니다.
     */
    fun clearLoadedFilesTracking() {
        loadedFiles.clear()
    }

    /**
     * 특정 디렉토리의 모든 JSON 파일을 로드합니다.
     *
     * 디렉토리의 모든 .json 파일을 로드하고 파일명(확장자 제외)을 키로 캐싱합니다.
     *
     * @param path 디렉토리 경로
     * @return 로드된 파일 개수
     * @throws IllegalArgumentException 경로가 비어있는 경우
     * @throws IllegalArgumentException 경로가 디렉토리가 아닌 경우
     */
    fun loadDirectory(path: String): Int {
        require(path.isNotBlank()) { "경로는 비어있을 수 없습니다" }

        val directory = File(path)
        if (!directory.exists() || !directory.isDirectory) {
            throw IllegalArgumentException(
                "유효한 디렉토리가 아닙니다: ${directory.absolutePath}"
            )
        }

        var loadedCount = 0
        directory.listFiles { file -> file.extension == "json" }?.forEach { file ->
            try {
                val content = file.readText()
                val keyName = file.nameWithoutExtension
                cacheResponse(keyName, content)
                loadedFiles.add(file.absolutePath)
                loadedCount++
            } catch (e: Exception) {
                // 파일 로드 실패해도 계속 진행
                System.err.println("경고: 파일 로드 실패 - ${file.absolutePath}: ${e.message}")
            }
        }

        return loadedCount
    }

    /**
     * URL 패턴에 기반하여 응답을 매핑합니다.
     *
     * 예를 들어, "https://fc.yahoo.com/getcrumb" URL에 대해
     * 특정 응답을 매핑할 수 있습니다.
     *
     * @param urlPattern URL 패턴 (예: "yahoo:crumb")
     * @param response 매핑할 응답
     */
    fun mapUrlPattern(urlPattern: String, response: String) {
        cacheResponse("url:$urlPattern", response)
    }

    /**
     * 매핑된 응답을 조회합니다.
     *
     * @param urlPattern URL 패턴
     * @return 매핑된 응답 또는 null
     */
    fun getUrlPattern(urlPattern: String): String? {
        return getResponse("url:$urlPattern")
    }

    /**
     * 저장소의 상태를 상세히 문자열로 반환합니다.
     *
     * @return 상태 정보 문자열
     */
    override fun toString(): String {
        return "RecordedResponseRepository(" +
                "cacheSize=${responseCache.size}, " +
                "loadedFilesCount=${loadedFiles.size}, " +
                "cachedKeys=${ responseCache.keys.take(5).joinToString(", ") }${if (responseCache.size > 5) ", ..." else ""})"
    }
}
