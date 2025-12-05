package com.ulalax.ufc.integration.utils

import org.slf4j.LoggerFactory

/**
 * 데이터 크기에 따라 자동으로 적절한 레코딩 방식을 선택하는 스마트 레코더
 *
 * 대용량 API 응답을 효율적으로 레코딩하기 위해 세 가지 전략을 사용합니다:
 *
 * ## 레코딩 전략
 *
 * | Tier | 데이터 크기 | 전략 | 파일명 suffix |
 * |------|------------|------|---------------|
 * | 1 | <= 10,000 | 전체 레코딩 | (없음) |
 * | 2 | 10,001 ~ 100,000 | 처음 10,000개만 | _limited |
 * | 3 | > 100,000 | 랜덤 1,000개 샘플 | _sample |
 *
 * ## 사용 예제
 * ```kotlin
 * // 대용량 데이터도 안전하게 레코딩
 * val chartData = client.chart("AAPL", Interval.OneDay, Period.TenYears)
 * SmartRecorder.recordSmartly(
 *     data = chartData.quotes,
 *     category = RecordingConfig.Paths.Yahoo.CHART,
 *     fileName = "aapl_10y_chart"
 * )
 * ```
 *
 * ## ResponseRecorder와의 차이점
 * - ResponseRecorder: 단순 레코딩, 대용량 데이터는 잘림 경고만 출력
 * - SmartRecorder: 데이터 크기에 따라 최적의 전략 자동 선택, 파일명에 전략 반영
 *
 * @see ResponseRecorder 기본 레코딩 유틸리티
 * @see RecordingConfig 레코딩 설정 및 경로
 */
object SmartRecorder {

    @PublishedApi
    internal val logger = LoggerFactory.getLogger(SmartRecorder::class.java)

    // ========================================
    // 전략 임계값 상수
    // ========================================

    /**
     * Tier 1과 Tier 2의 경계값
     * 이 값 이하의 데이터는 전체 레코딩됩니다.
     */
    const val SMALL_THRESHOLD = 10_000

    /**
     * Tier 2와 Tier 3의 경계값
     * 이 값 초과 시 샘플링 전략이 적용됩니다.
     */
    const val MEDIUM_THRESHOLD = 100_000

    /**
     * Tier 3에서 추출하는 샘플 크기
     * 대용량 데이터에서 랜덤으로 추출할 아이템 수입니다.
     */
    const val LARGE_SAMPLE_SIZE = 1_000

    // ========================================
    // 레코딩 전략 sealed class
    // ========================================

    /**
     * 데이터 크기에 따른 레코딩 전략을 나타내는 sealed class
     *
     * 각 전략은 자신만의 특성(파일명 suffix, 설명 메시지)을 가집니다.
     */
    sealed class RecordingStrategy {
        /** 파일명에 추가될 suffix (전체 레코딩은 빈 문자열) */
        abstract val fileNameSuffix: String

        /** 로깅에 사용될 전략 설명 */
        abstract val description: String

        /** 원본 데이터를 전략에 맞게 변환 */
        abstract fun <T> transform(data: List<T>): List<T>

        /**
         * Tier 1: 전체 레코딩 전략
         *
         * 데이터 크기가 10,000개 이하일 때 사용됩니다.
         * 모든 데이터를 그대로 레코딩합니다.
         */
        data object Full : RecordingStrategy() {
            override val fileNameSuffix: String = ""
            override val description: String = "전체 레코딩"

            override fun <T> transform(data: List<T>): List<T> = data
        }

        /**
         * Tier 2: 제한된 레코딩 전략
         *
         * 데이터 크기가 10,001 ~ 100,000개일 때 사용됩니다.
         * 처음 10,000개만 레코딩하여 파일 크기를 제한합니다.
         *
         * @property originalSize 원본 데이터의 크기 (로깅용)
         */
        data class Limited(val originalSize: Int) : RecordingStrategy() {
            override val fileNameSuffix: String = "_limited"
            override val description: String =
                "제한된 레코딩 (${originalSize}개 중 ${SMALL_THRESHOLD}개)"

            override fun <T> transform(data: List<T>): List<T> =
                data.take(SMALL_THRESHOLD)
        }

        /**
         * Tier 3: 샘플링 레코딩 전략
         *
         * 데이터 크기가 100,000개를 초과할 때 사용됩니다.
         * 랜덤으로 1,000개를 샘플링하여 대표성 있는 데이터를 저장합니다.
         *
         * @property originalSize 원본 데이터의 크기 (로깅용)
         */
        data class Sampled(val originalSize: Int) : RecordingStrategy() {
            override val fileNameSuffix: String = "_sample"
            override val description: String =
                "샘플링 레코딩 (${originalSize}개 중 ${LARGE_SAMPLE_SIZE}개 랜덤 추출)"

            override fun <T> transform(data: List<T>): List<T> =
                data.shuffled().take(LARGE_SAMPLE_SIZE)
        }
    }

    // ========================================
    // 전략 선택 로직
    // ========================================

    /**
     * 데이터 크기에 따라 적절한 레코딩 전략을 선택합니다.
     *
     * @param dataSize 레코딩할 데이터의 크기
     * @return 선택된 레코딩 전략
     */
    fun selectStrategy(dataSize: Int): RecordingStrategy {
        return when {
            dataSize <= SMALL_THRESHOLD -> RecordingStrategy.Full
            dataSize <= MEDIUM_THRESHOLD -> RecordingStrategy.Limited(dataSize)
            else -> RecordingStrategy.Sampled(dataSize)
        }
    }

    // ========================================
    // 메인 레코딩 함수
    // ========================================

    /**
     * 데이터 크기에 따라 자동으로 적절한 레코딩 방식을 선택하여 저장합니다.
     *
     * 이 함수는 데이터 크기를 분석하여 최적의 레코딩 전략을 자동으로 선택합니다:
     * - **Tier 1** (<=10,000): 전체 데이터 그대로 저장
     * - **Tier 2** (10,001~100,000): 처음 10,000개만 저장, 파일명에 `_limited` 추가
     * - **Tier 3** (>100,000): 1,000개 랜덤 샘플링, 파일명에 `_sample` 추가
     *
     * @param T 저장할 데이터의 타입 (Serializable)
     * @param data 저장할 리스트 데이터
     * @param category API 카테고리 경로 (RecordingConfig.Paths 사용)
     * @param fileName 파일명 (확장자 제외, 전략에 따라 suffix 자동 추가)
     *
     * @sample recordSmartlyExample
     */
    inline fun <reified T> recordSmartly(
        data: List<T>,
        category: String,
        fileName: String
    ) {
        // 레코딩이 비활성화된 경우 조기 반환
        if (!RecordingConfig.isRecordingEnabled) {
            logger.debug("레코딩이 비활성화되어 있습니다. 스킵합니다.")
            return
        }

        // 빈 데이터 체크
        if (data.isEmpty()) {
            logger.warn("$category/$fileName 에 레코딩할 데이터가 없습니다.")
            return
        }

        // 전략 선택
        val strategy = selectStrategy(data.size)

        // 전략에 따른 데이터 변환
        val transformedData = strategy.transform(data)

        // 파일명에 전략 suffix 추가
        val finalFileName = "$fileName${strategy.fileNameSuffix}"

        // 전략별 로깅
        logStrategy(strategy, data.size, category, finalFileName)

        // ResponseRecorder를 통해 실제 레코딩 수행
        ResponseRecorder.recordList(transformedData, category, finalFileName)
    }

    /**
     * 단일 객체를 레코딩합니다.
     *
     * List가 아닌 단일 객체(예: QuoteSummaryModuleResult)를 레코딩할 때 사용합니다.
     *
     * @param T 저장할 데이터의 타입 (Serializable)
     * @param data 저장할 단일 객체
     * @param category API 카테고리 경로 (RecordingConfig.Paths 사용)
     * @param fileName 파일명 (확장자 제외)
     */
    inline fun <reified T> recordSmartly(
        data: T,
        category: String,
        fileName: String
    ) {
        // 레코딩이 비활성화된 경우 조기 반환
        if (!RecordingConfig.isRecordingEnabled) {
            logger.debug("레코딩이 비활성화되어 있습니다. 스킵합니다.")
            return
        }

        // ResponseRecorder를 통해 실제 레코딩 수행
        ResponseRecorder.record(data, category, fileName)

        logger.info(
            "[SmartRecorder] 단일 객체 레코딩: {}/{}",
            category, fileName
        )
    }

    /**
     * 전략별 로깅 메시지를 출력합니다.
     *
     * @param strategy 선택된 레코딩 전략
     * @param originalSize 원본 데이터 크기
     * @param category 저장 카테고리
     * @param fileName 최종 파일명
     */
    @PublishedApi
    internal fun logStrategy(
        strategy: RecordingStrategy,
        originalSize: Int,
        category: String,
        fileName: String
    ) {
        when (strategy) {
            is RecordingStrategy.Full -> {
                logger.info(
                    "[SmartRecorder] Tier 1 - 전체 레코딩: {}개 -> {}/{}",
                    originalSize, category, fileName
                )
            }

            is RecordingStrategy.Limited -> {
                logger.warn(
                    "[SmartRecorder] Tier 2 - 제한된 레코딩: {}개 중 {}개만 레코딩 -> {}/{}",
                    originalSize, SMALL_THRESHOLD, category, fileName
                )
                println("! [SmartRecorder] 대용량 데이터 감지: ${originalSize}개 중 ${SMALL_THRESHOLD}개만 레코딩됨 (${fileName})")
            }

            is RecordingStrategy.Sampled -> {
                logger.warn(
                    "[SmartRecorder] Tier 3 - 샘플링 레코딩: {}개 중 {}개 랜덤 샘플링 -> {}/{}",
                    originalSize, LARGE_SAMPLE_SIZE, category, fileName
                )
                println("!! [SmartRecorder] 초대용량 데이터 감지: ${originalSize}개 중 ${LARGE_SAMPLE_SIZE}개 랜덤 샘플링됨 (${fileName})")
                println("   - 샘플링 비율: ${String.format("%.2f", LARGE_SAMPLE_SIZE.toDouble() / originalSize * 100)}%")
            }
        }
    }

    // ========================================
    // 유틸리티 함수
    // ========================================

    /**
     * 현재 데이터 크기에 어떤 전략이 적용될지 미리 확인합니다.
     *
     * 실제 레코딩 전에 어떤 전략이 선택될지 확인하고 싶을 때 사용합니다.
     *
     * @param dataSize 확인할 데이터 크기
     * @return 적용될 전략의 설명 문자열
     *
     * @sample previewStrategyExample
     */
    fun previewStrategy(dataSize: Int): String {
        val strategy = selectStrategy(dataSize)
        return buildString {
            append("데이터 크기: $dataSize -> ")
            append("적용 전략: ${strategy::class.simpleName} ")
            append("(${strategy.description})")
            if (strategy.fileNameSuffix.isNotEmpty()) {
                append(", 파일명 suffix: '${strategy.fileNameSuffix}'")
            }
        }
    }

    /**
     * 전략 선택 기준을 설명하는 도움말을 반환합니다.
     */
    fun getStrategyGuide(): String = buildString {
        appendLine("=== SmartRecorder 전략 가이드 ===")
        appendLine()
        appendLine("Tier 1 - Full (전체 레코딩)")
        appendLine("  - 조건: 데이터 <= ${SMALL_THRESHOLD}개")
        appendLine("  - 동작: 모든 데이터 저장")
        appendLine("  - 파일명: 원본 그대로")
        appendLine()
        appendLine("Tier 2 - Limited (제한된 레코딩)")
        appendLine("  - 조건: ${SMALL_THRESHOLD}개 < 데이터 <= ${MEDIUM_THRESHOLD}개")
        appendLine("  - 동작: 처음 ${SMALL_THRESHOLD}개만 저장")
        appendLine("  - 파일명: *_limited.json")
        appendLine()
        appendLine("Tier 3 - Sampled (샘플링 레코딩)")
        appendLine("  - 조건: 데이터 > ${MEDIUM_THRESHOLD}개")
        appendLine("  - 동작: 랜덤 ${LARGE_SAMPLE_SIZE}개 샘플링")
        appendLine("  - 파일명: *_sample.json")
    }
}

// ========================================
// 사용 예제 (KDoc @sample용)
// ========================================

private fun recordSmartlyExample() {
    // 예시: 차트 데이터 레코딩
    // val chartData = client.chart("AAPL", Interval.OneDay, Period.TenYears)
    // SmartRecorder.recordSmartly(
    //     data = chartData.quotes,
    //     category = RecordingConfig.Paths.Yahoo.CHART,
    //     fileName = "aapl_10y_chart"
    // )
    // 결과:
    // - 500개인 경우 -> aapl_10y_chart.json (전체)
    // - 50,000개인 경우 -> aapl_10y_chart_limited.json (처음 10,000개)
    // - 200,000개인 경우 -> aapl_10y_chart_sample.json (랜덤 1,000개)
}

private fun previewStrategyExample() {
    // 전략 미리보기
    println(SmartRecorder.previewStrategy(5_000))
    // 출력: 데이터 크기: 5000 -> 적용 전략: Full (전체 레코딩)

    println(SmartRecorder.previewStrategy(50_000))
    // 출력: 데이터 크기: 50000 -> 적용 전략: Limited (제한된 레코딩 (50000개 중 10000개)), 파일명 suffix: '_limited'
}
