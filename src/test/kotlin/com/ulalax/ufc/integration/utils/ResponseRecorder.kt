package com.ulalax.ufc.integration.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.JsonSerializer
import java.math.BigDecimal
import java.nio.file.Files
import java.time.LocalDate

/**
 * API 응답을 JSON 파일로 저장하는 유틸리티
 *
 * Live Test 실행 중 실제 API 응답을 레코딩하여
 * Unit Test에서 Mock 데이터로 사용할 수 있도록 합니다.
 *
 * ## 사용 예제
 * ```kotlin
 * // 객체 레코딩
 * val quoteSummary = client.quoteSummary("AAPL", QuoteSummaryModule.PRICE)
 * ResponseRecorder.record(
 *     data = quoteSummary,
 *     category = RecordingConfig.Paths.Yahoo.QUOTE_SUMMARY,
 *     fileName = "aapl_price"
 * )
 *
 * // 리스트 레코딩
 * val seriesData = client.series("GDP")
 * ResponseRecorder.recordList(
 *     data = seriesData.observations,
 *     category = RecordingConfig.Paths.Fred.SERIES,
 *     fileName = "gdp_series"
 * )
 *
 * // Raw JSON 직접 레코딩 (HTTP 응답 본문)
 * ResponseRecorder.recordRaw(
 *     jsonString = responseBody,
 *     category = RecordingConfig.Paths.Yahoo.CHART,
 *     fileName = "aapl_chart"
 * )
 * ```
 */
object ResponseRecorder {
    const val MAX_RECORD_SIZE = 10_000 // 최대 10,000개만 레코딩
    const val DEFAULT_CHUNK_SIZE = 1_000 // 기본 청크 크기

    @PublishedApi
    internal val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(
                LocalDate::class.java,
                JsonSerializer<LocalDate> { src, _, _ ->
                    com.google.gson.JsonPrimitive(src.toString())
                },
            ).registerTypeAdapter(
                BigDecimal::class.java,
                JsonSerializer<BigDecimal> { src, _, _ ->
                    com.google.gson.JsonPrimitive(src.toPlainString())
                },
            ).create()

    // Pretty printing 없는 compact Gson (내부 파싱용)
    private val compactGson: Gson = Gson()

    /**
     * 객체를 JSON 파일로 저장
     * @param data 저장할 데이터
     * @param category API 카테고리 (RecordingConfig.Paths 사용)
     * @param fileName 파일명 (확장자 제외)
     */
    inline fun <reified T> record(
        data: T,
        category: String,
        fileName: String,
    ) {
        val outputDir = RecordingConfig.baseOutputPath.resolve(category)
        Files.createDirectories(outputDir)

        val outputFile = outputDir.resolve("$fileName.json")
        val jsonString = gson.toJson(data)
        Files.writeString(outputFile, jsonString)

        println("[Recording] Recorded: $outputFile")
    }

    /**
     * 리스트 데이터를 JSON 파일로 저장
     * 데이터가 MAX_RECORD_SIZE를 초과하면 처음 MAX_RECORD_SIZE개만 레코딩
     */
    inline fun <reified T> recordList(
        data: List<T>,
        category: String,
        fileName: String,
    ) {
        if (data.isEmpty()) {
            println("[Recording] Warning: No data to record for $category/$fileName")
            return
        }

        val recordData =
            if (data.size > MAX_RECORD_SIZE) {
                println(
                    "[Recording] Warning: Data too large (${data.size} items). Recording only first $MAX_RECORD_SIZE items.",
                )
                data.take(MAX_RECORD_SIZE)
            } else {
                data
            }

        record(recordData, category, fileName)
    }

    /**
     * Raw JSON 문자열을 직접 파일로 저장
     *
     * HTTP 응답 본문(raw JSON)을 직접 받아서 저장합니다.
     * JSON 파싱 후 pretty-printing을 진행하며, 파싱 실패 시 원본을 그대로 저장합니다.
     *
     * @param jsonString 저장할 JSON 문자열 (HTTP 응답 본문)
     * @param category API 카테고리 경로
     * @param fileName 파일명 (확장자 제외)
     * @return 저장 성공 여부
     *
     * ## 사용 예제
     * ```kotlin
     * val responseBody = httpClient.get(url).bodyAsText()
     * ResponseRecorder.recordRaw(
     *     jsonString = responseBody,
     *     category = RecordingConfig.Paths.Yahoo.CHART,
     *     fileName = "aapl_chart_raw"
     * )
     * ```
     */
    fun recordRaw(
        jsonString: String,
        category: String,
        fileName: String,
    ): Boolean {
        if (jsonString.isBlank()) {
            println("[Recording] Warning: Empty JSON string for $category/$fileName")
            return false
        }

        val outputDir = RecordingConfig.baseOutputPath.resolve(category)
        Files.createDirectories(outputDir)

        val outputFile = outputDir.resolve("$fileName.json")

        // JSON 파싱 및 pretty-printing 시도
        val formattedJson =
            try {
                val jsonElement = JsonParser.parseString(jsonString)
                gson.toJson(jsonElement)
            } catch (e: Exception) {
                // 파싱 실패 시 원본 JSON 그대로 저장
                println(
                    "[Recording] Warning: JSON parsing failed for $category/$fileName. Saving raw content. Error: ${e.message}",
                )
                jsonString
            }

        return try {
            Files.writeString(outputFile, formattedJson)
            println("[Recording] Recorded raw JSON: $outputFile")
            true
        } catch (e: Exception) {
            println("[Recording] Error: Failed to write file $outputFile. Error: ${e.message}")
            false
        }
    }

    /**
     * 대용량 리스트를 청크 단위로 처리하여 메모리 효율적으로 저장
     *
     * 대용량 데이터를 한 번에 직렬화하지 않고, 청크 단위로 나누어 처리합니다.
     * 메모리 효율성을 위해 청크별로 직렬화하고 파일에 점진적으로 기록합니다.
     *
     * @param data 저장할 대용량 리스트
     * @param category API 카테고리 경로
     * @param fileName 파일명 (확장자 제외)
     * @param chunkSize 한 번에 처리할 청크 크기 (기본값: 1000)
     * @param enableMemoryLogging 메모리 사용량 로깅 활성화 (기본값: false)
     * @return 저장된 항목 수
     *
     * ## 사용 예제
     * ```kotlin
     * val largeDataList = fetchAllHistoricalData()  // 100,000개 이상
     * val savedCount = ResponseRecorder.recordListStreaming(
     *     data = largeDataList,
     *     category = RecordingConfig.Paths.Yahoo.CHART,
     *     fileName = "historical_all",
     *     chunkSize = 2000,
     *     enableMemoryLogging = true
     * )
     * println("Saved $savedCount items")
     * ```
     */
    inline fun <reified T> recordListStreaming(
        data: List<T>,
        category: String,
        fileName: String,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        enableMemoryLogging: Boolean = false,
    ): Int {
        if (data.isEmpty()) {
            println("[Recording] Warning: No data to record for $category/$fileName")
            return 0
        }

        val totalSize = data.size
        val effectiveChunkSize = chunkSize.coerceAtLeast(100) // 최소 100개

        // 데이터가 청크 크기보다 작으면 일반 recordList 사용
        if (totalSize <= effectiveChunkSize) {
            recordList(data, category, fileName)
            return totalSize
        }

        val outputDir = RecordingConfig.baseOutputPath.resolve(category)
        Files.createDirectories(outputDir)
        val outputFile = outputDir.resolve("$fileName.json")

        // 메모리 사용량 로깅 (시작)
        if (enableMemoryLogging) {
            logMemoryUsage("Before streaming (total: $totalSize items)")
        }

        return try {
            // 파일 스트리밍 방식으로 JSON 배열 작성
            Files.newBufferedWriter(outputFile).use { writer ->
                writer.write("[\n")

                var processedCount = 0
                val chunks = data.chunked(effectiveChunkSize)
                val totalChunks = chunks.size

                chunks.forEachIndexed { chunkIndex, chunk ->
                    chunk.forEachIndexed { itemIndex, item ->
                        val json = gson.toJson(item)

                        // 들여쓰기 추가
                        val indentedJson = json.lines().joinToString("\n") { "  $it" }
                        writer.write(indentedJson)

                        // 마지막 항목이 아니면 쉼표 추가
                        val isLastItem = (chunkIndex == totalChunks - 1) && (itemIndex == chunk.size - 1)
                        if (!isLastItem) {
                            writer.write(",")
                        }
                        writer.write("\n")

                        processedCount++
                    }

                    // 청크 처리 완료 로깅
                    val progress = ((chunkIndex + 1).toDouble() / totalChunks * 100).toInt()
                    println("[Recording] Progress: $processedCount/$totalSize ($progress%)")

                    // 메모리 사용량 로깅 (청크마다)
                    if (enableMemoryLogging && (chunkIndex + 1) % 5 == 0) {
                        logMemoryUsage("After chunk ${chunkIndex + 1}/$totalChunks")
                    }

                    // GC 힌트 (대용량 처리 시)
                    if (chunkIndex % 10 == 9) {
                        System.gc()
                    }
                }

                writer.write("]")
            }

            // 메모리 사용량 로깅 (완료)
            if (enableMemoryLogging) {
                logMemoryUsage("After streaming completed")
            }

            println("[Recording] Streaming completed: $outputFile ($totalSize items)")
            totalSize
        } catch (e: Exception) {
            println("[Recording] Error during streaming for $category/$fileName: ${e.message}")
            e.printStackTrace()

            // 부분 저장된 파일이 있으면 삭제
            try {
                if (Files.exists(outputFile)) {
                    Files.delete(outputFile)
                    println("[Recording] Partial file deleted: $outputFile")
                }
            } catch (deleteError: Exception) {
                println("[Recording] Warning: Failed to delete partial file: ${deleteError.message}")
            }

            0
        }
    }

    /**
     * 메모리 사용량을 로깅합니다.
     */
    @PublishedApi
    internal fun logMemoryUsage(context: String) {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMemory = runtime.maxMemory() / (1024 * 1024)
        val freeMemory = runtime.freeMemory() / (1024 * 1024)

        println("[Memory] $context - Used: ${usedMemory}MB, Free: ${freeMemory}MB, Max: ${maxMemory}MB")
    }
}
