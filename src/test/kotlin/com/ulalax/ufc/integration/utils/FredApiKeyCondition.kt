package com.ulalax.ufc.integration.utils

import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File
import java.util.Properties

/**
 * FRED API Key 존재 여부를 확인하는 JUnit 5 조건
 *
 * 다음 순서로 FRED_API_KEY를 찾습니다:
 * 1. 환경변수 (System.getenv)
 * 2. local.properties 파일
 *
 * ## 사용 예제
 * ```kotlin
 * @ExtendWith(FredApiKeyCondition::class)
 * class FredSeriesSpec : IntegrationTestBase() {
 *     // ...
 * }
 * ```
 */
class FredApiKeyCondition : ExecutionCondition {
    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        val apiKey = findFredApiKey()

        return if (apiKey != null && apiKey.isNotBlank()) {
            ConditionEvaluationResult.enabled("FRED_API_KEY found")
        } else {
            ConditionEvaluationResult.disabled(
                "FRED_API_KEY not found. Set it via environment variable or local.properties",
            )
        }
    }

    private fun findFredApiKey(): String? {
        // 1. 환경변수 확인
        System.getenv("FRED_API_KEY")?.let { return it }

        // 2. local.properties 확인
        return loadFromLocalProperties("FRED_API_KEY")
    }

    private fun loadFromLocalProperties(key: String): String? {
        val localPropertiesFile = File("local.properties")
        if (!localPropertiesFile.exists()) return null

        return try {
            val properties = Properties()
            localPropertiesFile.inputStream().use { properties.load(it) }
            properties.getProperty(key)
        } catch (e: Exception) {
            null
        }
    }
}
