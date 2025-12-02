package com.ulalax.ufc.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonSerializer
import org.slf4j.LoggerFactory
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.min

/**
 * API 응답 기록을 위한 유틸리티
 *
 * 라이브 테스트에서 수신한 API 응답을 JSON 파일로 기록합니다.
 * RecordingConfig.isRecordingEnabled에 따라 기록 여부가 결정됩니다.
 *
 * 특징:
 * - Pretty printing을 사용한 가독성 높은 JSON 형식
 * - LocalDate, BigDecimal 커스텀 직렬화 지원
 * - 데이터 크기 검증 및 경고
 * - 자동 디렉토리 생성
 *
 * 사용 예시:
 * ```
 * data class StockQuote(val symbol: String, val price: BigDecimal)
 *
 * val quote = StockQuote("AAPL", BigDecimal("150.50"))
 * ResponseRecorder.record(quote, "yahoo/ticker", "AAPL_quote")
 *
 * val quotes = listOf(
 *     StockQuote("AAPL", BigDecimal("150.50")),
 *     StockQuote("MSFT", BigDecimal("320.25"))
 * )
 * ResponseRecorder.recordList(quotes, "yahoo/ticker", "all_quotes")
 * ```
 */
object ResponseRecorder {
    @PublishedApi
    internal val logger = LoggerFactory.getLogger(ResponseRecorder::class.java)

    /**
     * 기록할 수 있는 최대 항목 수
     *
     * 이 크기를 초과하는 리스트는 경고를 표시한 후 잘라냅니다.
     * 과도한 데이터 기록을 방지하기 위한 안전 장치입니다.
     */
    @PublishedApi
    internal const val MAX_RECORD_SIZE = 10_000

    /**
     * Pretty printing이 활성화된 Gson 인스턴스
     *
     * LocalDate와 BigDecimal에 대한 커스텀 직렬화를 지원합니다.
     * ISO 8601 형식으로 날짜를 저장합니다.
     */
    @PublishedApi
    internal val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        // LocalDate 커스텀 직렬화 (ISO 8601 형식)
        .registerTypeAdapter(LocalDate::class.java, JsonSerializer<LocalDate> { src, _, _ ->
            com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE))
        })
        // BigDecimal 커스텀 직렬화 (정확한 숫자 표현)
        .registerTypeAdapter(BigDecimal::class.java, JsonSerializer<BigDecimal> { src, _, _ ->
            com.google.gson.JsonPrimitive(src)
        })
        .create()

    /**
     * 단일 객체를 JSON 파일로 기록합니다.
     *
     * RecordingConfig.isRecordingEnabled가 false인 경우 아무 작업도 수행하지 않습니다.
     *
     * @param T 기록할 데이터의 타입
     * @param data 기록할 데이터
     * @param category 카테고리 경로 (예: "yahoo/ticker")
     * @param fileName 파일명 확장자 제외 (예: "AAPL_quote")
     *
     * 예시:
     * ```
     * data class EconomicData(val seriesId: String, val value: Double)
     * val data = EconomicData("GDPC1", 27360.50)
     * ResponseRecorder.record(data, "fred/series", "GDPC1_latest")
     * // 결과: src/liveTest/resources/responses/fred/series/GDPC1_latest.json
     * ```
     */
    inline fun <reified T> record(data: T, category: String, fileName: String) {
        // 기록이 비활성화된 경우 반환
        if (!RecordingConfig.isRecordingEnabled) {
            return
        }

        try {
            // 출력 디렉토리 생성
            val outputDir = RecordingConfig.baseOutputPath.resolve(category)
            Files.createDirectories(outputDir)

            // 출력 파일 경로
            val outputFile = outputDir.resolve("$fileName.json")

            // JSON으로 직렬화 후 파일에 저장
            val json = gson.toJson(data)
            Files.writeString(outputFile, json)

            logger.info("✅ Recorded: $outputFile")
        } catch (e: Exception) {
            logger.warn("Failed to record response: category=$category, fileName=$fileName", e)
        }
    }

    /**
     * 객체 리스트를 JSON 파일로 기록합니다.
     *
     * 다음의 추가 검증을 수행합니다:
     * - 빈 리스트 경고 출력
     * - MAX_RECORD_SIZE 초과 시 경고 후 자르기
     *
     * @param T 기록할 데이터의 타입
     * @param data 기록할 데이터 리스트
     * @param category 카테고리 경로 (예: "yahoo/search")
     * @param fileName 파일명 확장자 제외 (예: "search_results")
     *
     * 예시:
     * ```
     * data class SearchResult(val symbol: String, val name: String)
     * val results = listOf(
     *     SearchResult("AAPL", "Apple Inc."),
     *     SearchResult("MSFT", "Microsoft Corporation")
     * )
     * ResponseRecorder.recordList(results, "yahoo/search", "aapl_search")
     * // 결과: src/liveTest/resources/responses/yahoo/search/aapl_search.json
     * ```
     */
    inline fun <reified T> recordList(data: List<T>, category: String, fileName: String) {
        // 기록이 비활성화된 경우 반환
        if (!RecordingConfig.isRecordingEnabled) {
            return
        }

        try {
            // 빈 리스트 경고
            if (data.isEmpty()) {
                logger.warn("⚠️ Recording empty list: category=$category, fileName=$fileName")
                // 빈 리스트도 기록
            }

            // 데이터 크기 검증
            if (data.size > MAX_RECORD_SIZE) {
                logger.warn(
                    "⚠️ Data size ({}) exceeds MAX_RECORD_SIZE ({}), truncating: category={}, fileName={}",
                    data.size, MAX_RECORD_SIZE, category, fileName
                )
                // 데이터를 MAX_RECORD_SIZE까지만 자르기
                val truncatedData = data.subList(0, MAX_RECORD_SIZE)
                record(truncatedData, category, fileName)
            } else {
                // 데이터 크기가 정상 범위 내인 경우
                record(data, category, fileName)
            }
        } catch (e: Exception) {
            logger.warn("Failed to record list: category=$category, fileName=$fileName", e)
        }
    }

    /**
     * Raw JSON 문자열을 파일로 기록합니다.
     *
     * bodyAsTextWithRecording()으로 캡처된 JSON 응답을 저장하는 데 사용됩니다.
     * JSON pretty-printing을 시도하고, 실패 시 원본 그대로 저장합니다.
     *
     * RecordingConfig.isRecordingEnabled가 false인 경우 아무 작업도 수행하지 않습니다.
     *
     * @param jsonString 기록할 JSON 문자열
     * @param category 카테고리 경로 (예: "yahoo/chart/daily")
     * @param fileName 파일명 확장자 제외 (예: "aapl_1y")
     *
     * 예시:
     * ```
     * val rawJson = """{"symbol":"AAPL","price":150.50}"""
     * ResponseRecorder.recordRaw(rawJson, "yahoo/quote", "aapl_quote")
     * // 결과: src/liveTest/resources/responses/yahoo/quote/aapl_quote.json (pretty-printed)
     * ```
     */
    fun recordRaw(jsonString: String, category: String, fileName: String) {
        // 기록이 비활성화된 경우 반환
        if (!RecordingConfig.isRecordingEnabled) {
            logger.debug("Recording disabled, skipping: $category/$fileName")
            return
        }

        try {
            // 출력 디렉토리 생성
            val outputDir = RecordingConfig.baseOutputPath.resolve(category)
            Files.createDirectories(outputDir)

            // 출력 파일 경로
            val outputFile = outputDir.resolve("$fileName.json")

            // JSON pretty-print 시도
            val formatted = try {
                val jsonElement = gson.fromJson(jsonString, JsonElement::class.java)
                gson.toJson(jsonElement)
            } catch (e: Exception) {
                logger.warn("Failed to pretty-print JSON, saving raw: ${e.message}")
                jsonString
            }

            Files.writeString(outputFile, formatted)
            logger.info("✅ Recorded: $outputFile")
        } catch (e: Exception) {
            logger.warn("Failed to record raw JSON: category=$category, fileName=$fileName", e)
        }
    }
}
