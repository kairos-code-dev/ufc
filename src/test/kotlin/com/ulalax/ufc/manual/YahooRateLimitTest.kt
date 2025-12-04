package com.ulalax.ufc.manual

import com.ulalax.ufc.api.client.Ufc
import com.ulalax.ufc.api.client.UfcClientConfig
import com.ulalax.ufc.api.exception.UfcException
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitConfig
import com.ulalax.ufc.infrastructure.ratelimit.RateLimitingSettings
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

/**
 * Yahoo Finance API Rate Limit 임계치 테스트
 *
 * 이 테스트는 실제로 Yahoo Finance API를 호출하여
 * rate limit의 실제 임계치를 측정합니다.
 *
 * ⚠️ 주의: 기본적으로 @Disabled 처리되어 있습니다.
 * 실제 API를 다량 호출하므로 필요할 때만 실행하세요.
 *
 * 실행 방법:
 * 1. @Disabled 어노테이션을 주석 처리
 * 2. 아래 명령어 실행:
 * ```bash
 * ./gradlew test --tests "com.ulalax.ufc.manual.YahooRateLimitTest"
 * ```
 * 또는 IDE에서 직접 테스트 클래스/메서드를 선택하여 실행
 */
@Disabled("수동 테스트: 실제 API 호출로 rate limit을 측정합니다. 필요시 주석 해제 후 실행")
@Tag("manual")
@Tag("rateLimit")
class YahooRateLimitTest {

    /**
     * 테스트 1: CRUMB 토큰 획득 rate limit 테스트
     *
     * CRUMB 토큰을 반복적으로 획득하면서 429 에러가 발생하는 지점을 찾습니다.
     */
    @Test
    fun `CRUMB 토큰 획득 - 연속 요청 임계치 테스트`() = runBlocking {
        println("=" .repeat(80))
        println("CRUMB 토큰 획득 Rate Limit 테스트 시작")
        println("=" .repeat(80))

        var successCount = 0
        var failureCount = 0
        var firstFailureAt = -1

        val results = mutableListOf<TestResult>()

        // 60초 동안 최대한 많은 CRUMB 토큰 획득 시도
        val totalTime = measureTimeMillis {
            for (i in 1..200) {
                val result = try {
                    val startTime = System.currentTimeMillis()

                    // Rate limiting 없이 UFC 클라이언트 생성 (CRUMB 획득)
                    val ufc = Ufc.create(
                        UfcClientConfig(
                            rateLimitingSettings = RateLimitingSettings(
                                yahoo = RateLimitConfig(
                                    capacity = 1000,
                                    refillRate = 1000,
                                    enabled = false // Rate limiter 비활성화
                                )
                            )
                        )
                    )

                    val elapsedTime = System.currentTimeMillis() - startTime
                    ufc.close()

                    successCount++
                    TestResult(i, true, elapsedTime, null)

                } catch (e: UfcException) {
                    failureCount++
                    if (firstFailureAt == -1) {
                        firstFailureAt = i
                    }

                    val is429 = e.message?.contains("429") == true ||
                                e.message?.contains("Too Many Requests") == true

                    TestResult(i, false, 0, e.message)
                }

                results.add(result)

                // 진행 상황 출력 (10개마다)
                if (i % 10 == 0) {
                    println("[$i] 성공: $successCount, 실패: $failureCount")
                }

                // 연속 5번 실패하면 중단
                if (results.takeLast(5).all { !it.success }) {
                    println("\n연속 5번 실패 감지. 테스트 중단.")
                    break
                }

                // 약간의 딜레이 (0ms - 즉시 요청)
                delay(0)
            }
        }

        // 결과 출력
        println("\n" + "=".repeat(80))
        println("테스트 결과 요약")
        println("=".repeat(80))
        println("총 시도: ${results.size}")
        println("성공: $successCount")
        println("실패: $failureCount")
        println("총 소요 시간: ${totalTime}ms (${totalTime / 1000.0}초)")
        println("초당 요청 수: ${successCount / (totalTime / 1000.0)}")

        if (firstFailureAt > 0) {
            println("\n첫 번째 실패: ${firstFailureAt}번째 요청")
            println("실패까지 성공한 요청 수: ${firstFailureAt - 1}")
        }

        // 성공한 요청의 평균 응답 시간
        val avgResponseTime = results.filter { it.success }.map { it.elapsedTime }.average()
        println("평균 응답 시간: ${avgResponseTime.toInt()}ms")

        // 실패 원인 분석
        val failures = results.filter { !it.success }
        if (failures.isNotEmpty()) {
            println("\n실패 원인:")
            failures.groupBy { it.errorMessage }.forEach { (error, list) ->
                println("  - ${error?.take(100)}: ${list.size}건")
            }
        }

        println("=".repeat(80))
    }

    /**
     * 테스트 2: 데이터 요청 rate limit 테스트
     *
     * 실제 데이터 조회를 반복하면서 rate limit을 측정합니다.
     */
    @Test
    fun `데이터 요청 - 초당 요청 수 임계치 테스트`() = runBlocking {
        println("=" .repeat(80))
        println("데이터 요청 Rate Limit 테스트 시작")
        println("=" .repeat(80))

        // 먼저 UFC 클라이언트 생성 (CRUMB 획득)
        val ufc = Ufc.create(
            UfcClientConfig(
                rateLimitingSettings = RateLimitingSettings(
                    yahoo = RateLimitConfig(
                        capacity = 1000,
                        refillRate = 1000,
                        enabled = false // Rate limiter 비활성화
                    )
                )
            )
        )

        var successCount = 0
        var failureCount = 0
        val results = mutableListOf<TestResult>()

        // 여러 요청 속도 테스트
        val requestRates = listOf(1, 2, 5, 10, 20, 50, 100) // 초당 요청 수

        for (rps in requestRates) {
            println("\n" + "-".repeat(80))
            println("초당 ${rps}개 요청 테스트 중...")
            println("-".repeat(80))

            val delayMs = 1000L / rps
            var localSuccess = 0
            var localFailure = 0

            // 각 속도로 60초 동안 테스트 (또는 실패 시까지)
            val testDuration = 60000L // 60초
            val startTime = System.currentTimeMillis()
            var requestCount = 0

            while (System.currentTimeMillis() - startTime < testDuration) {
                requestCount++

                val result = try {
                    val reqStartTime = System.currentTimeMillis()

                    // 간단한 주식 정보 조회
                    ufc.stock.getFastInfo("AAPL")

                    val elapsedTime = System.currentTimeMillis() - reqStartTime
                    localSuccess++
                    successCount++

                    TestResult(requestCount, true, elapsedTime, null)

                } catch (e: UfcException) {
                    localFailure++
                    failureCount++

                    TestResult(requestCount, false, 0, e.message)
                }

                results.add(result)

                // 10개마다 진행 상황 출력
                if (requestCount % 10 == 0) {
                    val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                    val actualRps = requestCount / elapsed
                    println("  [${requestCount}] 성공: $localSuccess, 실패: $localFailure, 실제 RPS: ${actualRps.toInt()}")
                }

                // 연속 3번 실패하면 이 속도는 중단
                if (results.takeLast(3).all { !it.success }) {
                    println("\n  연속 3번 실패 감지. 초당 ${rps}개는 너무 빠름.")
                    break
                }

                delay(delayMs)
            }

            val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
            println("\n  초당 ${rps}개 테스트 결과:")
            println("    성공: $localSuccess, 실패: $localFailure")
            println("    실제 소요 시간: ${elapsed}초")
            println("    실제 RPS: ${requestCount / elapsed}")

            // 실패가 발생하면 더 빠른 속도는 테스트하지 않음
            if (localFailure > 0) {
                println("\n  Rate limit 감지. 더 빠른 속도 테스트 중단.")
                break
            }

            // 다음 테스트 전 대기
            println("\n  다음 테스트까지 10초 대기...")
            delay(10000)
        }

        ufc.close()

        // 최종 결과 출력
        println("\n" + "=".repeat(80))
        println("최종 결과 요약")
        println("=".repeat(80))
        println("총 요청: ${results.size}")
        println("성공: $successCount")
        println("실패: $failureCount")
        println("성공률: ${(successCount.toDouble() / results.size * 100).toInt()}%")
        println("=".repeat(80))
    }

    /**
     * 테스트 3: 점진적 증가 테스트
     *
     * 요청 속도를 점진적으로 증가시키면서 정확한 임계치를 찾습니다.
     */
    @Test
    fun `점진적 증가 - 정확한 임계치 찾기`() = runBlocking {
        println("=" .repeat(80))
        println("점진적 증가 Rate Limit 테스트 시작")
        println("=" .repeat(80))

        val ufc = Ufc.create(
            UfcClientConfig(
                rateLimitingSettings = RateLimitingSettings(
                    yahoo = RateLimitConfig(
                        capacity = 1000,
                        refillRate = 1000,
                        enabled = false
                    )
                )
            )
        )

        // 1초부터 시작해서 점진적으로 딜레이 감소 (속도 증가)
        val delayIntervals = listOf(
            1000L,  // 초당 1개
            500L,   // 초당 2개
            333L,   // 초당 3개
            250L,   // 초당 4개
            200L,   // 초당 5개
            167L,   // 초당 6개
            143L,   // 초당 7개
            125L,   // 초당 8개
            111L,   // 초당 9개
            100L,   // 초당 10개
            50L,    // 초당 20개
            33L,    // 초당 30개
            20L,    // 초당 50개
            10L,    // 초당 100개
        )

        for (delayMs in delayIntervals) {
            val rps = 1000.0 / delayMs
            println("\n" + "-".repeat(80))
            println("딜레이 ${delayMs}ms (초당 ${rps.toInt()}개) 테스트 중...")
            println("-".repeat(80))

            var success = 0
            var failure = 0

            // 각 속도로 30개 요청 테스트
            for (i in 1..30) {
                try {
                    ufc.stock.getFastInfo("AAPL")
                    success++
                    print(".")
                } catch (e: UfcException) {
                    failure++
                    print("X")

                    // 429 에러인지 확인
                    if (e.message?.contains("429") == true) {
                        println("\n\n⚠️  Rate Limit 감지!")
                        println("임계치: 초당 약 ${rps.toInt()}개 요청에서 제한 발생")
                        ufc.close()
                        return@runBlocking
                    }
                }

                delay(delayMs)
            }

            println("\n성공: $success, 실패: $failure")

            if (failure > 0) {
                println("\n⚠️  이 속도에서 실패 발생. 테스트 중단.")
                break
            }

            // 다음 속도 테스트 전 5초 대기
            println("다음 테스트까지 5초 대기...")
            delay(5000)
        }

        ufc.close()
        println("\n" + "=".repeat(80))
        println("테스트 완료")
        println("=".repeat(80))
    }

    /**
     * 테스트 4: 동시 요청 임계치 찾기 (Binary Search)
     *
     * 정확히 몇 개의 동시 요청까지 허용되는지 확인합니다.
     */
    @Test
    fun `동시 요청 임계치 찾기 - Binary Search`() = runBlocking {
        println("=" .repeat(80))
        println("동시 요청 임계치 테스트 시작")
        println("=" .repeat(80))

        val ufc = Ufc.create(
            UfcClientConfig(
                rateLimitingSettings = RateLimitingSettings(
                    yahoo = RateLimitConfig(
                        capacity = 1000,
                        refillRate = 1000,
                        enabled = false
                    )
                )
            )
        )

        // 테스트할 동시 요청 수 목록
        val concurrencyLevels = listOf(2, 3, 5, 7, 10, 15, 20, 25, 30)

        for (concurrency in concurrencyLevels) {
            println("\n" + "-".repeat(80))
            println("동시 요청 ${concurrency}개 테스트 중...")
            println("-".repeat(80))

            var successCount = 0
            var failureCount = 0
            val errors = mutableListOf<String>()

            // 3회 반복 테스트로 일관성 확인
            repeat(3) { round ->
                println("\n라운드 ${round + 1}/3:")

                try {
                    // 동시에 N개 요청 실행
                    coroutineScope {
                        val results = List(concurrency) { i ->
                            async {
                                try {
                                    ufc.stock.getFastInfo("AAPL")
                                    print(".")
                                    true
                                } catch (e: UfcException) {
                                    print("X")
                                    errors.add(e.message ?: "Unknown error")
                                    false
                                }
                            }
                        }.awaitAll()

                        val roundSuccess = results.count { it }
                        val roundFailure = results.count { !it }

                        successCount += roundSuccess
                        failureCount += roundFailure

                        println("\n  성공: $roundSuccess, 실패: $roundFailure")
                    }
                } catch (e: Exception) {
                    println("\n  예외 발생: ${e.message}")
                    errors.add(e.message ?: "Unknown exception")
                }

                // 라운드 간 대기
                if (round < 2) {
                    delay(2000)
                }
            }

            println("\n동시 요청 ${concurrency}개 최종 결과:")
            println("  총 성공: $successCount / ${concurrency * 3}")
            println("  총 실패: $failureCount / ${concurrency * 3}")
            println("  성공률: ${(successCount * 100.0 / (concurrency * 3)).toInt()}%")

            // 429 에러 분석
            val has429 = errors.any { it.contains("429") || it.contains("Too Many Requests") }
            if (has429) {
                println("\n⚠️  Rate Limit 감지!")
                println("  임계치: 동시 ${concurrency}개 요청에서 429 에러 발생")
                println("  권장 maxConcurrentRequests: ${(concurrency * 0.7).toInt()} ~ ${(concurrency * 0.8).toInt()}")
                break
            }

            if (failureCount > 0) {
                println("\n⚠️  실패 발생 (429가 아닌 다른 에러):")
                errors.groupBy { it.take(100) }.forEach { (error, list) ->
                    println("    - $error: ${list.size}건")
                }
            }

            // 다음 테스트 전 대기
            println("\n다음 테스트까지 5초 대기...")
            delay(5000)
        }

        ufc.close()
        println("\n" + "=".repeat(80))
        println("테스트 완료")
        println("=".repeat(80))
    }

    /**
     * 테스트 5: CRUMB vs IP 기반 제한 확인
     *
     * 제한이 CRUMB 토큰 단위인지 IP 주소 단위인지 확인합니다.
     */
    @Test
    fun `CRUMB vs IP 기반 제한 확인`() = runBlocking {
        println("=" .repeat(80))
        println("CRUMB vs IP 기반 제한 테스트 시작")
        println("=" .repeat(80))

        // 시나리오 A: 1개 클라이언트 (1 CRUMB) → 10개 동시 요청
        println("\n" + "-".repeat(80))
        println("시나리오 A: 단일 CRUMB, 10개 동시 요청")
        println("-".repeat(80))

        val ufcA = Ufc.create(
            UfcClientConfig(
                rateLimitingSettings = RateLimitingSettings(
                    yahoo = RateLimitConfig(
                        capacity = 1000,
                        refillRate = 1000,
                        enabled = false
                    )
                )
            )
        )

        var scenarioASuccess = 0
        var scenarioAFailure = 0
        val scenarioAErrors = mutableListOf<String>()

        try {
            coroutineScope {
                val results = List(10) { i ->
                    async {
                        try {
                            ufcA.stock.getFastInfo("AAPL")
                            print(".")
                            true
                        } catch (e: UfcException) {
                            print("X")
                            scenarioAErrors.add(e.message ?: "Unknown")
                            false
                        }
                    }
                }.awaitAll()

                scenarioASuccess = results.count { it }
                scenarioAFailure = results.count { !it }
            }
        } catch (e: Exception) {
            println("\n예외: ${e.message}")
        }

        ufcA.close()

        println("\n시나리오 A 결과:")
        println("  성공: $scenarioASuccess / 10")
        println("  실패: $scenarioAFailure / 10")
        val scenarioAHas429 = scenarioAErrors.any { it.contains("429") }
        println("  429 에러: ${if (scenarioAHas429) "발생" else "없음"}")

        // 시나리오 간 대기
        println("\n시나리오 B 준비 중... 10초 대기")
        delay(10000)

        // 시나리오 B: 10개 클라이언트 (10 CRUMB) → 각 1개 요청 (동시)
        println("\n" + "-".repeat(80))
        println("시나리오 B: 10개 CRUMB, 각 1개씩 동시 요청")
        println("-".repeat(80))

        var scenarioBSuccess = 0
        var scenarioBFailure = 0
        val scenarioBErrors = mutableListOf<String>()

        try {
            coroutineScope {
                val results = List(10) { i ->
                    async {
                        var localUfc: Ufc? = null
                        try {
                            // 각 코루틴마다 새로운 UFC 클라이언트 생성 (새 CRUMB)
                            localUfc = Ufc.create(
                                UfcClientConfig(
                                    rateLimitingSettings = RateLimitingSettings(
                                        yahoo = RateLimitConfig(
                                            capacity = 1000,
                                            refillRate = 1000,
                                            enabled = false
                                        )
                                    )
                                )
                            )
                            localUfc.stock.getFastInfo("AAPL")
                            print(".")
                            true
                        } catch (e: UfcException) {
                            print("X")
                            scenarioBErrors.add(e.message ?: "Unknown")
                            false
                        } finally {
                            localUfc?.close()
                        }
                    }
                }.awaitAll()

                scenarioBSuccess = results.count { it }
                scenarioBFailure = results.count { !it }
            }
        } catch (e: Exception) {
            println("\n예외: ${e.message}")
        }

        println("\n시나리오 B 결과:")
        println("  성공: $scenarioBSuccess / 10")
        println("  실패: $scenarioBFailure / 10")
        val scenarioBHas429 = scenarioBErrors.any { it.contains("429") }
        println("  429 에러: ${if (scenarioBHas429) "발생" else "없음"}")

        // 결과 해석
        println("\n" + "=".repeat(80))
        println("결과 분석")
        println("=".repeat(80))

        when {
            !scenarioAHas429 && !scenarioBHas429 -> {
                println("✅ 두 시나리오 모두 성공 → 동시 10개 요청은 허용됨")
            }
            scenarioAHas429 && !scenarioBHas429 -> {
                println("⚠️  시나리오 A 실패, B 성공 → CRUMB 기반 제한으로 추정")
                println("   (단일 CRUMB으로는 동시 요청 제한, 여러 CRUMB은 허용)")
            }
            scenarioAHas429 && scenarioBHas429 -> {
                println("⚠️  두 시나리오 모두 실패 → IP 기반 제한으로 추정")
                println("   (CRUMB 개수와 무관하게 IP 주소 단위로 제한)")
            }
            !scenarioAHas429 && scenarioBHas429 -> {
                println("⚠️  예상 외 결과: 시나리오 A 성공, B 실패")
                println("   (추가 조사 필요)")
            }
        }
        println("=".repeat(80))
    }

    /**
     * 테스트 6: 순차 vs 동시 요청 비교
     *
     * 빠른 순차 요청과 진짜 동시 요청의 차이를 확인합니다.
     */
    @Test
    fun `순차 vs 동시 요청 비교`() = runBlocking {
        println("=" .repeat(80))
        println("순차 vs 동시 요청 비교 테스트 시작")
        println("=" .repeat(80))

        val ufc = Ufc.create(
            UfcClientConfig(
                rateLimitingSettings = RateLimitingSettings(
                    yahoo = RateLimitConfig(
                        capacity = 1000,
                        refillRate = 1000,
                        enabled = false
                    )
                )
            )
        )

        val requestCount = 20

        // 순차 요청: 20개 요청, 0ms 딜레이
        println("\n" + "-".repeat(80))
        println("순차 요청: ${requestCount}개, 0ms 딜레이")
        println("-".repeat(80))

        var sequentialSuccess = 0
        var sequentialFailure = 0
        val sequentialErrors = mutableListOf<String>()

        val sequentialTime = measureTimeMillis {
            for (i in 1..requestCount) {
                try {
                    ufc.stock.getFastInfo("AAPL")
                    sequentialSuccess++
                    print(".")
                } catch (e: UfcException) {
                    sequentialFailure++
                    print("X")
                    sequentialErrors.add(e.message ?: "Unknown")
                }
                delay(0) // Yield to other coroutines
            }
        }

        println("\n순차 요청 결과:")
        println("  성공: $sequentialSuccess / $requestCount")
        println("  실패: $sequentialFailure / $requestCount")
        println("  소요 시간: ${sequentialTime}ms")
        println("  평균 응답 시간: ${sequentialTime / requestCount}ms")
        val sequentialHas429 = sequentialErrors.any { it.contains("429") }
        println("  429 에러: ${if (sequentialHas429) "발생" else "없음"}")

        // 테스트 간 대기
        println("\n다음 테스트까지 10초 대기...")
        delay(10000)

        // 동시 요청: 20개 동시 async 요청
        println("\n" + "-".repeat(80))
        println("동시 요청: ${requestCount}개 async")
        println("-".repeat(80))

        var concurrentSuccess = 0
        var concurrentFailure = 0
        val concurrentErrors = mutableListOf<String>()

        val concurrentTime = measureTimeMillis {
            coroutineScope {
                val results = List(requestCount) { i ->
                    async {
                        try {
                            ufc.stock.getFastInfo("AAPL")
                            print(".")
                            true
                        } catch (e: UfcException) {
                            print("X")
                            concurrentErrors.add(e.message ?: "Unknown")
                            false
                        }
                    }
                }.awaitAll()

                concurrentSuccess = results.count { it }
                concurrentFailure = results.count { !it }
            }
        }

        println("\n동시 요청 결과:")
        println("  성공: $concurrentSuccess / $requestCount")
        println("  실패: $concurrentFailure / $requestCount")
        println("  소요 시간: ${concurrentTime}ms")
        val concurrentHas429 = concurrentErrors.any { it.contains("429") }
        println("  429 에러: ${if (concurrentHas429) "발생" else "없음"}")

        ufc.close()

        // 결과 비교
        println("\n" + "=".repeat(80))
        println("결과 분석")
        println("=".repeat(80))
        println("순차 요청: ${if (sequentialHas429) "실패" else "성공"}")
        println("동시 요청: ${if (concurrentHas429) "실패" else "성공"}")

        when {
            !sequentialHas429 && concurrentHas429 -> {
                println("\n✅ 가설 확인: 순차는 성공, 동시는 실패 → 동시성이 문제")
                println("   Yahoo Finance는 동시 요청 수를 제한하는 것으로 확인됨")
            }
            !sequentialHas429 && !concurrentHas429 -> {
                println("\n✅ 두 방식 모두 성공 → ${requestCount}개 동시 요청은 허용됨")
            }
            sequentialHas429 && concurrentHas429 -> {
                println("\n⚠️  두 방식 모두 실패 → 요청 속도가 너무 빠름")
            }
            sequentialHas429 && !concurrentHas429 -> {
                println("\n⚠️  예상 외 결과: 순차 실패, 동시 성공 (추가 조사 필요)")
            }
        }
        println("=".repeat(80))
    }

    /**
     * 테스트 7: 혼합 서비스 엔드포인트 동시 호출
     *
     * 엔드포인트별 제한 vs 전역 제한 확인합니다.
     */
    @Test
    fun `혼합 서비스 엔드포인트 동시 호출`() = runBlocking {
        println("=" .repeat(80))
        println("혼합 서비스 엔드포인트 동시 호출 테스트 시작")
        println("=" .repeat(80))

        val ufc = Ufc.create(
            UfcClientConfig(
                rateLimitingSettings = RateLimitingSettings(
                    yahoo = RateLimitConfig(
                        capacity = 1000,
                        refillRate = 1000,
                        enabled = false
                    )
                )
            )
        )

        // 4개 서비스 × 3개 요청 = 12개 동시 요청
        println("\n4개 서비스(Stock, Funds, Price, Corp) × 3개 = 12개 동시 요청")
        println("-".repeat(80))

        val results = mutableMapOf<String, MutableList<Boolean>>()
        val errors = mutableMapOf<String, MutableList<String>>()

        coroutineScope {
            val allRequests = mutableListOf<Pair<String, Boolean>>()

            // Stock 서비스 요청
            repeat(3) { i ->
                async {
                    val service = "Stock"
                    try {
                        ufc.stock.getFastInfo("AAPL")
                        print("S")
                        allRequests.add(service to true)
                    } catch (e: UfcException) {
                        print("X")
                        allRequests.add(service to false)
                        errors.getOrPut(service) { mutableListOf() }.add(e.message ?: "Unknown")
                    }
                }
            }

            // Funds 서비스 요청
            val fundsJobs = List(3) { i ->
                async {
                    val service = "Funds"
                    try {
                        ufc.funds.getFundData("VFIAX")
                        print("F")
                        allRequests.add(service to true)
                    } catch (e: UfcException) {
                        print("X")
                        allRequests.add(service to false)
                        errors.getOrPut(service) { mutableListOf() }.add(e.message ?: "Unknown")
                    }
                }
            }

            // Price 서비스 요청
            val priceJobs = List(3) { i ->
                async {
                    val service = "Price"
                    try {
                        ufc.price.getCurrentPrice("AAPL")
                        print("P")
                        allRequests.add(service to true)
                    } catch (e: UfcException) {
                        print("X")
                        allRequests.add(service to false)
                        errors.getOrPut(service) { mutableListOf() }.add(e.message ?: "Unknown")
                    }
                }
            }

            // Corp 서비스 요청
            val corpJobs = List(3) { i ->
                async {
                    val service = "Corp"
                    try {
                        ufc.corp.getDividends("AAPL")
                        print("C")
                        allRequests.add(service to true)
                    } catch (e: UfcException) {
                        print("X")
                        allRequests.add(service to false)
                        errors.getOrPut(service) { mutableListOf() }.add(e.message ?: "Unknown")
                    }
                }
            }

            // 모든 작업 완료 대기
            (fundsJobs + priceJobs + corpJobs).awaitAll()

            // 결과 집계
            allRequests.forEach { (service, success) ->
                results.getOrPut(service) { mutableListOf() }.add(success)
            }
        }

        // 결과 출력
        println("\n\n서비스별 결과:")
        listOf("Stock", "Funds", "Price", "Corp").forEach { service ->
            val serviceResults = results[service] ?: emptyList()
            val success = serviceResults.count { it }
            val failure = serviceResults.size - success
            val has429 = errors[service]?.any { it.contains("429") } ?: false

            println("  $service: 성공 $success / ${serviceResults.size}, 429 에러: ${if (has429) "발생" else "없음"}")
        }

        ufc.close()

        // 분석
        println("\n" + "=".repeat(80))
        println("결과 분석")
        println("=".repeat(80))

        val servicesWithError = errors.filter { (_, msgs) ->
            msgs.any { it.contains("429") }
        }.keys

        when {
            servicesWithError.isEmpty() -> {
                println("✅ 모든 서비스 성공 → 12개 동시 요청은 허용됨")
            }
            servicesWithError.size == 4 -> {
                println("⚠️  모든 서비스에서 429 발생 → 전역 제한으로 추정")
                println("   (엔드포인트별 제한이 아닌 IP 또는 CRUMB 단위 제한)")
            }
            else -> {
                println("⚠️  일부 서비스만 429 발생:")
                servicesWithError.forEach { service ->
                    println("    - $service")
                }
                println("   (엔드포인트별 제한 가능성 있음)")
            }
        }
        println("=".repeat(80))
    }

    /**
     * 테스트 8: RPS (Requests Per Second) 한계 찾기
     *
     * 동시 연결 수를 높게 유지하면서 초당 요청 수만 조절하여
     * Yahoo Finance의 실제 RPS 한계를 찾습니다.
     */
    @Test
    fun `RPS 한계 찾기 - 초당 요청 수 제한 측정`() = runBlocking {
        println("=".repeat(80))
        println("Yahoo Finance RPS 한계 테스트 시작")
        println("=".repeat(80))

        val ufc = Ufc.create(
            UfcClientConfig(
                rateLimitingSettings = RateLimitingSettings(
                    yahoo = RateLimitConfig(
                        capacity = 1000,
                        refillRate = 1000,
                        enabled = false
                    )
                )
            )
        )

        val semaphore = Semaphore(20)  // 동시 20개 허용 (충분히 높게 설정)
        val testRpsValues = listOf(5, 10, 15, 20, 25, 30, 40, 50)  // 테스트할 RPS 값들

        for (targetRps in testRpsValues) {
            println("\n" + "-".repeat(80))
            println("RPS $targetRps 테스트 중 (동시 연결 20개, ${3}초)")
            println("-".repeat(80))

            // 각 테스트 사이 쿨다운
            delay(10000)

            val durationSeconds = 3
            val totalRequests = targetRps * durationSeconds
            val delayBetweenRequests = (1000.0 / targetRps).toLong()
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)
            val errors = mutableListOf<String>()

            val startTime = System.currentTimeMillis()

            coroutineScope {
                repeat(totalRequests) { i ->
                    launch {
                        semaphore.withPermit {
                            try {
                                ufc.stock.getFastInfo("AAPL")
                                successCount.incrementAndGet()
                                print(".")
                            } catch (e: UfcException) {
                                failCount.incrementAndGet()
                                print("X")
                                errors.add(e.message ?: "Unknown")
                            }
                        }
                    }
                    delay(delayBetweenRequests)
                }
            }

            val elapsedMs = System.currentTimeMillis() - startTime
            val successRate = successCount.get() * 100.0 / totalRequests
            val actualRps = (successCount.get() + failCount.get()) * 1000.0 / elapsedMs

            println("\n\nRPS $targetRps 결과:")
            println("  총 요청: $totalRequests")
            println("  성공: ${successCount.get()}")
            println("  실패: ${failCount.get()}")
            println("  성공률: ${String.format("%.1f", successRate)}%")
            println("  실제 RPS: ${String.format("%.1f", actualRps)}")
            println("  소요 시간: ${elapsedMs}ms")

            // 429 에러 확인
            val has429 = errors.any { it.contains("429") || it.contains("Too Many Requests") }
            if (has429) {
                println("\n⚠️  Rate Limit 감지!")
                println("  RPS ${targetRps}에서 429 에러 발생")
                println("  권장 RPS 설정: ${(targetRps * 0.7).toInt()} ~ ${(targetRps * 0.8).toInt()}")

                // 실패한 에러 메시지 샘플 출력
                val errorSamples = errors.filter { it.contains("429") }.take(3)
                if (errorSamples.isNotEmpty()) {
                    println("\n  에러 샘플:")
                    errorSamples.forEach { error ->
                        println("    - ${error.take(100)}")
                    }
                }

                break
            }

            if (failCount.get() > 0) {
                println("\n⚠️  실패 발생 (429가 아닌 다른 에러):")
                errors.groupBy { it.take(100) }.forEach { (error, list) ->
                    println("    - $error: ${list.size}건")
                }
            }
        }

        ufc.close()
        println("\n" + "=".repeat(80))
        println("테스트 완료")
        println("=".repeat(80))
    }

    /**
     * 테스트 9: RPS 상세 측정 - 세밀한 단위로 임계치 찾기
     *
     * 대략적인 임계치를 찾은 후, 더 세밀한 단위로 정확한 한계를 측정합니다.
     */
    @Test
    fun `RPS 상세 측정 - 세밀한 임계치`() = runBlocking {
        println("=".repeat(80))
        println("Yahoo Finance RPS 상세 측정 테스트 시작")
        println("=".repeat(80))

        val ufc = Ufc.create(
            UfcClientConfig(
                rateLimitingSettings = RateLimitingSettings(
                    yahoo = RateLimitConfig(
                        capacity = 1000,
                        refillRate = 1000,
                        enabled = false
                    )
                )
            )
        )

        val semaphore = Semaphore(20)
        // 더 세밀한 RPS 값들: 15부터 35까지 5 단위로
        val testRpsValues = listOf(15, 20, 25, 30, 35, 40, 45, 50, 60, 70, 80, 90, 100)

        val results = mutableListOf<RpsTestResult>()

        for (targetRps in testRpsValues) {
            println("\n" + "-".repeat(80))
            println("RPS $targetRps 테스트")
            println("-".repeat(80))

            delay(8000)  // 8초 쿨다운

            val durationSeconds = 5  // 5초 동안 테스트
            val totalRequests = targetRps * durationSeconds
            val delayBetweenRequests = (1000.0 / targetRps).toLong()
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            val startTime = System.currentTimeMillis()

            coroutineScope {
                repeat(totalRequests) {
                    launch {
                        semaphore.withPermit {
                            try {
                                ufc.stock.getFastInfo("AAPL")
                                successCount.incrementAndGet()
                            } catch (e: UfcException) {
                                failCount.incrementAndGet()
                            }
                        }
                    }
                    delay(delayBetweenRequests)
                }
            }

            val elapsedMs = System.currentTimeMillis() - startTime
            val successRate = successCount.get() * 100.0 / totalRequests
            val actualRps = (successCount.get() + failCount.get()) * 1000.0 / elapsedMs

            val result = RpsTestResult(
                targetRps = targetRps,
                actualRps = actualRps,
                totalRequests = totalRequests,
                successCount = successCount.get(),
                failCount = failCount.get(),
                successRate = successRate,
                elapsedMs = elapsedMs
            )
            results.add(result)

            println("  성공=${successCount.get()}/${totalRequests} (${String.format("%.0f", successRate)}%), 실제 RPS=${String.format("%.1f", actualRps)}")

            // 성공률이 95% 미만이면 중단
            if (successRate < 95.0) {
                println("\n⚠️  성공률 95% 미만 감지. 테스트 중단.")
                break
            }
        }

        ufc.close()

        // 결과 요약
        println("\n" + "=".repeat(80))
        println("결과 요약")
        println("=".repeat(80))
        println("\n목표RPS | 실제RPS | 성공/전체 | 성공률 | 소요시간")
        println("-".repeat(60))
        results.forEach { r ->
            println(String.format(
                "%7d | %7.1f | %4d/%4d | %5.1f%% | %6dms",
                r.targetRps, r.actualRps, r.successCount, r.totalRequests, r.successRate, r.elapsedMs
            ))
        }

        // 최대 성공 RPS 찾기
        val maxSuccessRps = results.filter { it.successRate >= 99.0 }.maxByOrNull { it.targetRps }
        if (maxSuccessRps != null) {
            println("\n✅ 최대 성공 RPS: ${maxSuccessRps.targetRps} (성공률 ${String.format("%.1f", maxSuccessRps.successRate)}%)")
            println("   권장 설정: capacity=${(maxSuccessRps.targetRps * 0.8).toInt()}, refillRate=${(maxSuccessRps.targetRps * 0.8).toInt()}")
        }

        println("=".repeat(80))
    }

    data class RpsTestResult(
        val targetRps: Int,
        val actualRps: Double,
        val totalRequests: Int,
        val successCount: Int,
        val failCount: Int,
        val successRate: Double,
        val elapsedMs: Long
    )

    data class TestResult(
        val requestNumber: Int,
        val success: Boolean,
        val elapsedTime: Long,
        val errorMessage: String?
    )
}
