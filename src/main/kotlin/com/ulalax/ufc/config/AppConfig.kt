package com.ulalax.ufc.config

import org.slf4j.LoggerFactory
import java.io.File
import java.util.Properties

/**
 * 애플리케이션 설정을 관리하는 클래스입니다.
 *
 * 로컬 설정 파일(local.properties)과 애플리케이션 설정(application.properties)을 통합 관리합니다.
 */
object AppConfig {
    private val logger = LoggerFactory.getLogger(AppConfig::class.java)
    private val appProperties = Properties()
    private val localProperties = Properties()

    init {
        loadApplicationProperties()
        loadLocalProperties()
    }

    /**
     * 애플리케이션 설정 파일을 로드합니다.
     */
    private fun loadApplicationProperties() {
        try {
            val resource = AppConfig::class.java.classLoader.getResourceAsStream("application.properties")
            if (resource != null) {
                appProperties.load(resource)
                logger.info("애플리케이션 설정 파일 로드 완료")
            } else {
                logger.warn("애플리케이션 설정 파일을 찾을 수 없습니다")
            }
        } catch (e: Exception) {
            logger.error("애플리케이션 설정 파일 로드 실패", e)
        }
    }

    /**
     * 로컬 설정 파일을 로드합니다.
     */
    private fun loadLocalProperties() {
        try {
            val localFile = File("local.properties")
            if (localFile.exists()) {
                localProperties.load(localFile.inputStream())
                logger.info("로컬 설정 파일 로드 완료")
            } else {
                logger.warn("로컬 설정 파일(local.properties)을 찾을 수 없습니다")
                logger.warn("local.properties.template을 참조하여 local.properties를 생성해주세요")
            }
        } catch (e: Exception) {
            logger.error("로컬 설정 파일 로드 실패", e)
        }
    }

    /**
     * 설정 값을 가져옵니다. (로컬 설정 우선)
     */
    fun get(key: String): String? {
        return localProperties.getProperty(key) ?: appProperties.getProperty(key)
    }

    /**
     * 설정 값을 가져옵니다. (기본값 포함)
     */
    fun get(key: String, default: String): String {
        return get(key) ?: default
    }

    /**
     * 정수형 설정 값을 가져옵니다.
     */
    fun getInt(key: String, default: Int = 0): Int {
        return get(key)?.toIntOrNull() ?: default
    }

    /**
     * 긴 정수형 설정 값을 가져옵니다.
     */
    fun getLong(key: String, default: Long = 0L): Long {
        return get(key)?.toLongOrNull() ?: default
    }

    /**
     * 불린형 설정 값을 가져옵니다.
     */
    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return get(key)?.toBoolean() ?: default
    }
}
