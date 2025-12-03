# UFC Testing Strategy - 테스트 구현 전략

## 문서 정보
- **버전**: 1.0.0
- **최종 수정일**: 2025-12-02
- **작성자**: Claude Code (Software Architect)
- **문서 상태**: Active
- **참조 프로젝트**: KFC (Korea Financial Client)

---

## 목차
1. [테스트 전략 개요](#1-테스트-전략-개요)
2. [테스트 분리 구조](#2-테스트-분리-구조)
3. [Live Test 설계](#3-live-test-설계)
4. [Unit Test 설계](#4-unit-test-설계)
5. [응답 레코딩 시스템](#5-응답-레코딩-시스템)
6. [테스트 데이터 관리](#6-테스트-데이터-관리)
7. [Gradle 설정](#7-gradle-설정)
8. [테스트 작성 가이드](#8-테스트-작성-가이드)

---

## 1. 테스트 전략 개요

### 1.1 핵심 원칙

UFC의 테스트는 다음 원칙을 따릅니다:

1. **테스트는 API 사용법 가이드이자 스펙 문서**
   - 모든 테스트는 Given-When-Then 패턴 사용
   - DisplayName으로 명확한 테스트 의도 표현
   - 테스트 코드가 곧 사용 예제

2. **Live Test와 Unit Test 완전 분리**
   - Live Test: 실제 API 호출, 응답 레코딩
   - Unit Test: 레코딩된 데이터 기반 테스트
   - 별도 sourceSets로 완전 격리

3. **응답 레코딩 시스템**
   - Live Test 실행 시 자동으로 응답 레코딩
   - JSON 형식으로 저장
   - Unit Test에서 재사용

4. **KFC 프로젝트 패턴 준수**
   - 동일한 네임스페이스 구조 (`com.ulalax.ufc`)
   - 동일한 테스트 구조 및 유틸리티
   - 동일한 레코딩 메커니즘

### 1.2 테스트 목표

1. **기능 검증**
   - Yahoo Finance API 모든 기능 커버리지 100%
   - FRED API 모든 기능 커버리지 100%
   - 에러 처리 시나리오 검증

2. **API 문서화**
   - 각 API의 사용법을 테스트로 표현
   - 예상 입출력 명시
   - 엣지 케이스 처리 방법 설명

3. **회귀 방지**
   - 레코딩된 응답으로 회귀 테스트
   - API 변경 사항 조기 감지
   - 안정적인 CI/CD 파이프라인

---

## 2. 테스트 분리 구조

### 2.1 디렉토리 구조

```
ufc/
├── src/
│   ├── main/kotlin/com/ulalax/ufc/
│   │   └── (프로덕션 코드)
│   │
│   ├── liveTest/kotlin/com/ulalax/ufc/
│   │   ├── live/
│   │   │   ├── yahoo/
│   │   │   │   ├── etf/
│   │   │   │   │   ├── EtfTopHoldingsLiveTest.kt
│   │   │   │   │   ├── EtfSectorWeightingsLiveTest.kt
│   │   │   │   │   ├── EtfAssetAllocationLiveTest.kt
│   │   │   │   │   ├── EtfFundProfileLiveTest.kt
│   │   │   │   │   └── EtfComprehensiveLiveTest.kt
│   │   │   │   │
│   │   │   │   ├── ticker/
│   │   │   │   │   ├── TickerHistoryLiveTest.kt
│   │   │   │   │   ├── TickerDividendsLiveTest.kt
│   │   │   │   │   ├── TickerSplitsLiveTest.kt
│   │   │   │   │   ├── TickerFinancialsLiveTest.kt
│   │   │   │   │   └── TickerInfoLiveTest.kt
│   │   │   │   │
│   │   │   │   ├── search/
│   │   │   │   │   ├── SearchLiveTest.kt
│   │   │   │   │   └── ScreenerLiveTest.kt
│   │   │   │   │
│   │   │   │   └── chart/
│   │   │   │       ├── ChartIntradayLiveTest.kt
│   │   │   │       ├── ChartDailyLiveTest.kt
│   │   │   │       └── ChartAdjustedLiveTest.kt
│   │   │   │
│   │   │   └── fred/
│   │   │       ├── series/
│   │   │       │   ├── SeriesBasicLiveTest.kt
│   │   │       │   ├── SeriesVintageLiveTest.kt
│   │   │       │   └── SeriesInfoLiveTest.kt
│   │   │       │
│   │   │       ├── search/
│   │   │       │   ├── SearchBasicLiveTest.kt
│   │   │       │   ├── SearchByCategoryLiveTest.kt
│   │   │       │   └── SearchByReleaseLiveTest.kt
│   │   │       │
│   │   │       └── indicators/
│   │   │           ├── GDPIndicatorsLiveTest.kt
│   │   │           ├── UnemploymentIndicatorsLiveTest.kt
│   │   │           ├── InflationIndicatorsLiveTest.kt
│   │   │           └── InterestRatesLiveTest.kt
│   │   │
│   │   └── utils/
│   │       ├── LiveTestBase.kt
│   │       ├── ResponseRecorder.kt
│   │       ├── RecordingConfig.kt
│   │       └── TestSymbols.kt
│   │
│   ├── liveTest/resources/
│   │   └── responses/
│   │       ├── yahoo/
│   │       │   ├── etf/
│   │       │   │   ├── top_holdings/
│   │       │   │   ├── sector_weightings/
│   │       │   │   └── fund_profile/
│   │       │   ├── ticker/
│   │       │   │   ├── history/
│   │       │   │   ├── dividends/
│   │       │   │   └── financials/
│   │       │   └── chart/
│   │       │
│   │       └── fred/
│   │           ├── series/
│   │           ├── search/
│   │           └── indicators/
│   │
│   ├── test/kotlin/com/ulalax/ufc/
│   │   ├── source/
│   │   │   ├── yahoo/
│   │   │   │   ├── EtfTest.kt
│   │   │   │   ├── TickerTest.kt
│   │   │   │   ├── SearchTest.kt
│   │   │   │   └── ChartTest.kt
│   │   │   │
│   │   │   └── fred/
│   │   │       ├── SeriesTest.kt
│   │   │       ├── SearchTest.kt
│   │   │       └── IndicatorsTest.kt
│   │   │
│   │   ├── integration/
│   │   │   └── UFCClientTest.kt
│   │   │
│   │   └── utils/
│   │       └── JsonResponseLoader.kt
│   │
│   └── test/resources/
│       └── responses/
│           ├── yahoo/
│           │   ├── etf/
│           │   ├── ticker/
│           │   └── chart/
│           └── fred/
│               ├── series/
│               └── search/
│
└── build.gradle.kts
```

### 2.2 SourceSets 분리 이유

**완전한 격리:**
- Live Test와 Unit Test는 서로 독립적
- Live Test는 실제 API 호출 (느림, 외부 의존성)
- Unit Test는 레코딩된 데이터 사용 (빠름, 격리됨)

**별도 실행:**
```bash
# Live Test만 실행
./gradlew liveTest

# Unit Test만 실행
./gradlew test

# 모두 실행
./gradlew check
```

---

## 3. Live Test 설계

### 3.1 LiveTestBase

```kotlin
package com.ulalax.ufc.utils

import com.ulalax.ufc.UFCClient
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.util.Properties
import kotlin.time.Duration.Companion.seconds

/**
 * Live Test의 공통 베이스 클래스
 *
 * 실제 API 호출을 수행하며, 선택적으로 응답을 레코딩합니다.
 * - @Tag("live"): JUnit 5 태그로 Live Test 식별
 * - @TestInstance(PER_CLASS): 클래스당 하나의 인스턴스로 UFCClient 재사용
 * - local.properties에서 FRED_API_KEY 로드
 * - RecordingConfig.isRecordingEnabled로 레코딩 모드 확인
 */
@Tag("live")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class LiveTestBase {

    protected lateinit var client: UFCClient

    @BeforeAll
    fun setUp() = runTest {
        val fredApiKey = loadFredApiKey()

        client = if (fredApiKey != null) {
            UFCClient.create(
                config = UFCClientConfig(fredApiKey = fredApiKey)
            )
        } else {
            println("ℹ️  FRED_API_KEY가 설정되지 않았습니다. FRED API 테스트는 skip됩니다.")
            UFCClient.create() // Yahoo Finance는 키 없이도 동작
        }

        println("🚀 Live Test 시작 - Recording: ${RecordingConfig.isRecordingEnabled}")
    }

    @AfterAll
    fun tearDown() {
        if (::client.isInitialized) {
            client.close()
            println("🏁 Live Test 종료")
        }
    }

    /**
     * FRED API 키를 local.properties에서 로드
     */
    private fun loadFredApiKey(): String? {
        val localPropertiesFile = File("local.properties")
        if (localPropertiesFile.exists()) {
            val properties = Properties()
            localPropertiesFile.inputStream().use { properties.load(it) }
            return properties.getProperty("FRED_API_KEY")
        }
        return null
    }

    /**
     * 테스트 실행 헬퍼 (타임아웃 설정)
     */
    protected fun liveTest(
        timeout: kotlin.time.Duration = 30.seconds,
        block: suspend () -> Unit
    ) = runTest(timeout = timeout) {
        block()
    }
}
```

### 3.2 Live Test 예제 - Yahoo Finance ETF

```kotlin
package com.ulalax.ufc.live.yahoo.etf

import com.ulalax.ufc.utils.LiveTestBase
import com.ulalax.ufc.utils.RecordingConfig
import com.ulalax.ufc.utils.ResponseRecorder
import com.ulalax.ufc.utils.TestSymbols
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * ETF Top Holdings Live Test
 *
 * ## 목적
 * - ETF의 상위 보유 종목 데이터를 실제 API로부터 조회
 * - 응답 데이터를 JSON 파일로 레코딩하여 Unit Test에서 재사용
 *
 * ## API 사용법
 * ```kotlin
 * val ufc = UFCClient.create()
 * val spy = ufc.yahoo.etf("SPY")
 * val holdings = spy.getTopHoldings()
 * ```
 *
 * ## 레코딩 파일
 * - `src/liveTest/resources/responses/yahoo/etf/top_holdings/spy_top_holdings.json`
 * - `src/liveTest/resources/responses/yahoo/etf/top_holdings/qqq_top_holdings.json`
 */
class EtfTopHoldingsLiveTest : LiveTestBase() {

    @Test
    @DisplayName("SPY ETF의 상위 보유 종목을 조회할 수 있다")
    fun testGetTopHoldings_SPY() = liveTest {
        // Given: SPY ETF
        val spy = client.yahoo.etf(TestSymbols.SPY)

        // When: Top Holdings 조회
        val holdings = spy.getTopHoldings()

        // Then: 상위 보유 종목이 존재해야 함
        assertNotNull(holdings, "Top Holdings should not be null")
        assertNotNull(holdings.holdings, "Holdings list should not be null")
        assertTrue(holdings.holdings!!.size >= 10, "Should have at least 10 holdings")

        // Then: 각 보유 종목은 심볼, 이름, 비중을 포함해야 함
        holdings.holdings!!.forEach { holding ->
            assertNotNull(holding.symbol, "Symbol should not be null")
            assertNotNull(holding.holdingName, "Holding name should not be null")
            assertNotNull(holding.holdingPercent, "Holding percent should not be null")
        }

        // Then: 총 비중 확인
        val totalWeight = holdings.holdings!!.sumOf {
            it.holdingPercent?.raw ?: 0.0
        }
        println("✅ SPY Top Holdings: ${holdings.holdings!!.size}개")
        println("✅ 총 비중: ${String.format("%.2f%%", totalWeight * 100)}")
        println("✅ 상위 3개:")
        holdings.holdings!!.take(3).forEach { holding ->
            println("   - ${holding.symbol}: ${holding.holdingName} (${holding.holdingPercent?.fmt})")
        }

        // 응답 레코딩
        ResponseRecorder.record(
            data = holdings,
            category = RecordingConfig.Paths.Yahoo.Etf.TOP_HOLDINGS,
            fileName = "spy_top_holdings"
        )
    }

    @Test
    @DisplayName("QQQ ETF의 상위 보유 종목을 조회할 수 있다")
    fun testGetTopHoldings_QQQ() = liveTest {
        // Given: QQQ ETF
        val qqq = client.yahoo.etf(TestSymbols.QQQ)

        // When: Top Holdings 조회
        val holdings = qqq.getTopHoldings()

        // Then: 기술주 중심의 보유 종목 확인
        assertNotNull(holdings.holdings)

        // Then: 주요 기술주 포함 여부 확인
        val symbols = holdings.holdings!!.map { it.symbol }
        val hasTechGiants = listOf("AAPL", "MSFT", "NVDA", "GOOGL", "AMZN")
            .any { techStock -> symbols.contains(techStock) }
        assertTrue(hasTechGiants, "QQQ should contain major tech stocks")

        println("✅ QQQ Top Holdings: ${holdings.holdings!!.size}개")
        println("✅ 상위 5개:")
        holdings.holdings!!.take(5).forEach { holding ->
            println("   - ${holding.symbol}: ${holding.holdingPercent?.fmt}")
        }

        // 응답 레코딩
        ResponseRecorder.record(
            data = holdings,
            category = RecordingConfig.Paths.Yahoo.Etf.TOP_HOLDINGS,
            fileName = "qqq_top_holdings"
        )
    }

    @Test
    @DisplayName("ETF Top Holdings의 섹터별 분포를 확인할 수 있다")
    fun testTopHoldings_SectorDistribution() = liveTest {
        // Given: SPY ETF
        val spy = client.yahoo.etf(TestSymbols.SPY)

        // When: Sector Weightings 조회
        val sectorWeightings = spy.getSectorWeightings()

        // Then: 다양한 섹터가 존재해야 함
        assertNotNull(sectorWeightings)
        assertNotNull(sectorWeightings.sectorWeightings)
        assertTrue(sectorWeightings.sectorWeightings!!.size >= 5,
            "Should have at least 5 sectors")

        // 콘솔 출력: 섹터별 비중
        println("\n=== SPY 섹터별 비중 ===")
        sectorWeightings.sectorWeightings!!
            .sortedByDescending { it.weight?.raw ?: 0.0 }
            .forEach { sector ->
                println("${sector.category}: ${sector.weight?.fmt}")
            }

        // 응답 레코딩
        ResponseRecorder.record(
            data = sectorWeightings,
            category = RecordingConfig.Paths.Yahoo.Etf.SECTOR_WEIGHTINGS,
            fileName = "spy_sector_weightings"
        )
    }
}
```

### 3.3 Live Test 예제 - FRED

```kotlin
package com.ulalax.ufc.live.fred.series

import com.ulalax.ufc.utils.LiveTestBase
import com.ulalax.ufc.utils.RecordingConfig
import com.ulalax.ufc.utils.ResponseRecorder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate

/**
 * FRED Series Basic Live Test
 *
 * ## 목적
 * - FRED API로부터 시계열 데이터를 실제로 조회
 * - 응답 데이터를 JSON 파일로 레코딩하여 Unit Test에서 재사용
 *
 * ## API 사용법
 * ```kotlin
 * val ufc = UFCClient.create(
 *     config = UFCClientConfig(fredApiKey = "your_api_key")
 * )
 * val gdp = ufc.fred.getSeries(
 *     seriesId = "GDPC1",
 *     observationStart = LocalDate.of(2020, 1, 1),
 *     observationEnd = LocalDate.of(2024, 1, 1)
 * )
 * ```
 *
 * ## 레코딩 파일
 * - `src/liveTest/resources/responses/fred/series/gdp_2020_2024.json`
 * - `src/liveTest/resources/responses/fred/series/unemployment_2020_2024.json`
 */
class SeriesBasicLiveTest : LiveTestBase() {

    @Test
    @DisplayName("실질 GDP(GDPC1) 시계열 데이터를 조회할 수 있다")
    fun testGetSeries_GDP() = liveTest {
        // Given: GDP 시리즈 ID와 날짜 범위
        val seriesId = "GDPC1"
        val start = LocalDate.of(2020, 1, 1)
        val end = LocalDate.of(2024, 1, 1)

        // When: Series 조회
        val series = client.fred.getSeries(
            seriesId = seriesId,
            observationStart = start,
            observationEnd = end
        )

        // Then: 시계열 데이터가 존재해야 함
        assertNotNull(series, "Series should not be null")
        assertEquals(seriesId, series.id, "Series ID should match")
        assertNotNull(series.observations, "Observations should not be null")
        assertTrue(series.observations.isNotEmpty(), "Observations should not be empty")

        // Then: 데이터가 날짜 범위 내에 있어야 함
        series.observations.forEach { obs ->
            assertTrue(
                obs.date >= start && obs.date <= end,
                "Observation date should be within range: ${obs.date}"
            )
        }

        // Then: 값이 null이 아닌 관측값이 존재해야 함
        val validObservations = series.observations.filter { it.value != null }
        assertTrue(validObservations.isNotEmpty(), "Should have valid observations")

        println("✅ GDP Series: ${series.title}")
        println("✅ 관측값 개수: ${series.observations.size}")
        println("✅ 유효 관측값: ${validObservations.size}")
        println("✅ 최신 값: ${validObservations.last().date} = ${validObservations.last().value}")

        // 응답 레코딩
        ResponseRecorder.record(
            data = series,
            category = RecordingConfig.Paths.Fred.SERIES,
            fileName = "gdp_2020_2024"
        )
    }

    @Test
    @DisplayName("실업률(UNRATE) 시계열 데이터를 조회할 수 있다")
    fun testGetSeries_UnemploymentRate() = liveTest {
        // Given: 실업률 시리즈 ID
        val seriesId = "UNRATE"
        val start = LocalDate.of(2020, 1, 1)
        val end = LocalDate.of(2024, 1, 1)

        // When: Series 조회
        val series = client.fred.getSeries(
            seriesId = seriesId,
            observationStart = start,
            observationEnd = end
        )

        // Then: 월간 데이터 확인
        assertNotNull(series.observations)
        assertTrue(series.observations.size >= 40,
            "Should have at least 40 months of data")

        // Then: 실업률 범위 검증 (0% ~ 100%)
        val validValues = series.observations.mapNotNull { it.value }
        validValues.forEach { value ->
            assertTrue(value >= 0.0 && value <= 100.0,
                "Unemployment rate should be between 0% and 100%: $value")
        }

        // Then: COVID-19 영향 확인 (2020년 급증)
        val early2020 = series.observations.filter {
            it.date.year == 2020 && it.date.monthValue <= 6
        }
        val covidPeak = early2020.mapNotNull { it.value }.maxOrNull()
        assertNotNull(covidPeak, "Should have COVID-19 peak unemployment")
        assertTrue(covidPeak!! > 10.0, "COVID peak unemployment should exceed 10%")

        println("✅ Unemployment Rate Series: ${series.title}")
        println("✅ COVID-19 Peak: ${String.format("%.1f%%", covidPeak)}")

        // 응답 레코딩
        ResponseRecorder.record(
            data = series,
            category = RecordingConfig.Paths.Fred.SERIES,
            fileName = "unemployment_2020_2024"
        )
    }

    @Test
    @DisplayName("Series 메타데이터를 조회할 수 있다")
    fun testGetSeriesInfo() = liveTest {
        // Given: GDP 시리즈 ID
        val seriesId = "GDPC1"

        // When: Series 정보 조회
        val info = client.fred.getSeriesInfo(seriesId)

        // Then: 메타데이터 검증
        assertEquals(seriesId, info.id)
        assertNotNull(info.title)
        assertNotNull(info.frequency)
        assertNotNull(info.units)
        assertNotNull(info.seasonalAdjustment)

        println("✅ Series Info:")
        println("   - Title: ${info.title}")
        println("   - Frequency: ${info.frequency}")
        println("   - Units: ${info.units}")
        println("   - Seasonal Adjustment: ${info.seasonalAdjustment}")
        println("   - Last Updated: ${info.lastUpdated}")
        println("   - Popularity: ${info.popularity}")

        // 응답 레코딩
        ResponseRecorder.record(
            data = info,
            category = RecordingConfig.Paths.Fred.SERIES_INFO,
            fileName = "gdp_info"
        )
    }
}
```

---

## 4. Unit Test 설계

### 4.1 JsonResponseLoader

```kotlin
package com.ulalax.ufc.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 레코딩된 JSON 응답을 로드하는 유틸리티
 *
 * Unit Test에서 Live Test가 레코딩한 JSON 파일을 읽어옵니다.
 *
 * ## 사용 예제
 * ```kotlin
 * val holdings = JsonResponseLoader.load<TopHoldingsResponse>(
 *     category = RecordingConfig.Paths.Yahoo.Etf.TOP_HOLDINGS,
 *     fileName = "spy_top_holdings"
 * )
 * ```
 */
object JsonResponseLoader {

    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateDeserializer())
        .registerTypeAdapter(BigDecimal::class.java, BigDecimalDeserializer())
        .create()

    /**
     * 레코딩된 JSON 파일을 로드
     *
     * @param category API 카테고리 (RecordingConfig.Paths 사용)
     * @param fileName 파일명 (확장자 제외)
     * @return 역직렬화된 객체
     */
    inline fun <reified T> load(category: String, fileName: String): T {
        val resourcePath = "responses/$category/$fileName.json"
        val jsonString = loadResourceAsString(resourcePath)
        return gson.fromJson(jsonString, T::class.java)
    }

    /**
     * Resource 파일을 문자열로 로드
     */
    private fun loadResourceAsString(path: String): String {
        val resource = javaClass.classLoader.getResourceAsStream(path)
            ?: throw IllegalArgumentException("Resource not found: $path")
        return resource.bufferedReader().use { it.readText() }
    }
}
```

### 4.2 Unit Test 예제 - Yahoo Finance ETF

```kotlin
package com.ulalax.ufc.source.yahoo

import com.ulalax.ufc.utils.JsonResponseLoader
import com.ulalax.ufc.utils.RecordingConfig
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * ETF Unit Test
 *
 * ## 목적
 * - 레코딩된 JSON 데이터를 기반으로 ETF 로직 검증
 * - 외부 API 호출 없이 빠른 테스트 실행
 * - 데이터 파싱 및 변환 로직 검증
 *
 * ## 테스트 데이터
 * - `src/test/resources/responses/yahoo/etf/top_holdings/spy_top_holdings.json`
 * - Live Test에서 레코딩한 실제 응답 데이터 사용
 */
class EtfTest {

    @Test
    @DisplayName("레코딩된 SPY Top Holdings 데이터를 파싱할 수 있다")
    fun testParseTopHoldings_SPY() {
        // Given: 레코딩된 SPY Top Holdings JSON
        val holdings = JsonResponseLoader.load<TopHoldingsResponse>(
            category = RecordingConfig.Paths.Yahoo.Etf.TOP_HOLDINGS,
            fileName = "spy_top_holdings"
        )

        // Then: 데이터 구조 검증
        assertNotNull(holdings)
        assertNotNull(holdings.holdings)
        assertTrue(holdings.holdings!!.size >= 10)

        // Then: 첫 번째 보유 종목 검증
        val firstHolding = holdings.holdings!!.first()
        assertNotNull(firstHolding.symbol)
        assertNotNull(firstHolding.holdingName)
        assertNotNull(firstHolding.holdingPercent)
        assertTrue(firstHolding.holdingPercent!!.raw!! > 0.0)

        // Then: 총 비중 계산
        val totalWeight = holdings.holdings!!.sumOf {
            it.holdingPercent?.raw ?: 0.0
        }
        assertTrue(totalWeight > 0.0 && totalWeight <= 1.0,
            "Total weight should be between 0% and 100%")
    }

    @Test
    @DisplayName("레코딩된 SPY Sector Weightings 데이터를 파싱할 수 있다")
    fun testParseSectorWeightings_SPY() {
        // Given: 레코딩된 SPY Sector Weightings JSON
        val sectorWeightings = JsonResponseLoader.load<SectorWeightingsResponse>(
            category = RecordingConfig.Paths.Yahoo.Etf.SECTOR_WEIGHTINGS,
            fileName = "spy_sector_weightings"
        )

        // Then: 섹터 데이터 검증
        assertNotNull(sectorWeightings.sectorWeightings)
        assertTrue(sectorWeightings.sectorWeightings!!.size >= 5)

        // Then: 각 섹터는 카테고리와 비중을 포함
        sectorWeightings.sectorWeightings!!.forEach { sector ->
            assertNotNull(sector.category, "Sector category should not be null")
            assertNotNull(sector.weight, "Sector weight should not be null")
            assertTrue(sector.weight!!.raw!! >= 0.0)
        }

        // Then: 총 비중이 ~100%에 가까운지 검증
        val totalWeight = sectorWeightings.sectorWeightings!!.sumOf {
            it.weight?.raw ?: 0.0
        }
        assertTrue(totalWeight > 0.9 && totalWeight <= 1.0,
            "Total sector weight should be close to 100%")
    }

    @Test
    @DisplayName("Top Holdings를 비중 순으로 정렬할 수 있다")
    fun testSortHoldingsByWeight() {
        // Given: 레코딩된 Top Holdings
        val holdings = JsonResponseLoader.load<TopHoldingsResponse>(
            category = RecordingConfig.Paths.Yahoo.Etf.TOP_HOLDINGS,
            fileName = "spy_top_holdings"
        )

        // When: 비중 순으로 정렬
        val sortedHoldings = holdings.holdings!!
            .sortedByDescending { it.holdingPercent?.raw ?: 0.0 }

        // Then: 첫 번째 항목이 가장 큰 비중을 가져야 함
        val firstWeight = sortedHoldings.first().holdingPercent?.raw!!
        val secondWeight = sortedHoldings[1].holdingPercent?.raw!!
        assertTrue(firstWeight >= secondWeight,
            "Holdings should be sorted by weight descending")

        // Then: 비중이 감소하는지 확인
        for (i in 0 until sortedHoldings.size - 1) {
            val currentWeight = sortedHoldings[i].holdingPercent?.raw ?: 0.0
            val nextWeight = sortedHoldings[i + 1].holdingPercent?.raw ?: 0.0
            assertTrue(currentWeight >= nextWeight,
                "Weight at index $i ($currentWeight) should be >= weight at ${i + 1} ($nextWeight)")
        }
    }
}
```

### 4.3 Unit Test 예제 - FRED

```kotlin
package com.ulalax.ufc.source.fred

import com.ulalax.ufc.utils.JsonResponseLoader
import com.ulalax.ufc.utils.RecordingConfig
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate

/**
 * FRED Series Unit Test
 *
 * ## 목적
 * - 레코딩된 FRED 시계열 데이터를 기반으로 파싱 검증
 * - 데이터 변환 로직 검증
 * - 통계 계산 로직 검증
 */
class SeriesTest {

    @Test
    @DisplayName("레코딩된 GDP 시계열 데이터를 파싱할 수 있다")
    fun testParseSeries_GDP() {
        // Given: 레코딩된 GDP JSON
        val series = JsonResponseLoader.load<Series>(
            category = RecordingConfig.Paths.Fred.SERIES,
            fileName = "gdp_2020_2024"
        )

        // Then: Series 메타데이터 검증
        assertEquals("GDPC1", series.id)
        assertNotNull(series.title)
        assertTrue(series.title!!.contains("GDP", ignoreCase = true))

        // Then: Observations 검증
        assertNotNull(series.observations)
        assertTrue(series.observations.isNotEmpty())

        // Then: 유효한 값이 존재하는지 확인
        val validObservations = series.observations.filter { it.value != null }
        assertTrue(validObservations.isNotEmpty())

        // Then: 날짜 순으로 정렬되어 있는지 확인
        for (i in 0 until series.observations.size - 1) {
            val current = series.observations[i].date
            val next = series.observations[i + 1].date
            assertTrue(current <= next,
                "Observations should be sorted by date")
        }
    }

    @Test
    @DisplayName("시계열 데이터의 기본 통계를 계산할 수 있다")
    fun testCalculateBasicStatistics() {
        // Given: 레코딩된 실업률 데이터
        val series = JsonResponseLoader.load<Series>(
            category = RecordingConfig.Paths.Fred.SERIES,
            fileName = "unemployment_2020_2024"
        )

        // When: 유효한 값만 추출
        val values = series.observations.mapNotNull { it.value }

        // Then: 평균 계산
        val average = values.average()
        assertTrue(average > 0.0, "Average unemployment should be positive")

        // Then: 최댓값/최솟값
        val max = values.maxOrNull()!!
        val min = values.minOrNull()!!
        assertTrue(max > min, "Max should be greater than min")
        assertTrue(max > 10.0, "COVID-19 peak should exceed 10%")

        // Then: 표준편차 계산
        val variance = values.map { (it - average).let { diff -> diff * diff } }.average()
        val stdDev = kotlin.math.sqrt(variance)
        assertTrue(stdDev > 0.0, "Standard deviation should be positive")

        println("✅ Statistics:")
        println("   - Average: ${String.format("%.2f%%", average)}")
        println("   - Min: ${String.format("%.2f%%", min)}")
        println("   - Max: ${String.format("%.2f%%", max)}")
        println("   - StdDev: ${String.format("%.2f%%", stdDev)}")
    }

    @Test
    @DisplayName("시계열 데이터를 특정 날짜 범위로 필터링할 수 있다")
    fun testFilterByDateRange() {
        // Given: 레코딩된 GDP 데이터
        val series = JsonResponseLoader.load<Series>(
            category = RecordingConfig.Paths.Fred.SERIES,
            fileName = "gdp_2020_2024"
        )

        // When: 2022년 데이터만 필터링
        val start = LocalDate.of(2022, 1, 1)
        val end = LocalDate.of(2022, 12, 31)
        val filtered = series.observations.filter { obs ->
            obs.date >= start && obs.date <= end
        }

        // Then: 필터링된 데이터 검증
        assertTrue(filtered.isNotEmpty(), "Should have 2022 data")
        filtered.forEach { obs ->
            assertTrue(obs.date.year == 2022,
                "All observations should be from 2022")
        }

        // Then: 분기별 데이터 확인 (GDP는 분기별)
        assertTrue(filtered.size >= 4, "Should have at least 4 quarters")
    }
}
```

---

## 5. 응답 레코딩 시스템

### 5.1 ResponseRecorder

```kotlin
package com.ulalax.ufc.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSerializer
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate

/**
 * API 응답을 JSON 파일로 저장하는 유틸리티
 *
 * Live Test 실행 중 실제 API 응답을 레코딩하여
 * Unit Test에서 Mock 데이터로 사용할 수 있도록 합니다.
 *
 * ## 사용 예제
 * ```kotlin
 * val holdings = client.yahoo.etf("SPY").getTopHoldings()
 * ResponseRecorder.record(
 *     data = holdings,
 *     category = RecordingConfig.Paths.Yahoo.Etf.TOP_HOLDINGS,
 *     fileName = "spy_top_holdings"
 * )
 * ```
 */
object ResponseRecorder {
    const val MAX_RECORD_SIZE = 10_000  // 최대 10,000개만 레코딩

    @PublishedApi
    internal val gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(LocalDate::class.java, JsonSerializer<LocalDate> { src, _, _ ->
            com.google.gson.JsonPrimitive(src.toString())
        })
        .registerTypeAdapter(BigDecimal::class.java, JsonSerializer<BigDecimal> { src, _, _ ->
            com.google.gson.JsonPrimitive(src.toPlainString())
        })
        .create()

    /**
     * 객체를 JSON 파일로 저장
     * @param data 저장할 데이터
     * @param category API 카테고리 (RecordingConfig.Paths 사용)
     * @param fileName 파일명 (확장자 제외)
     */
    inline fun <reified T> record(data: T, category: String, fileName: String) {
        if (!RecordingConfig.isRecordingEnabled) return

        val outputDir = RecordingConfig.baseOutputPath.resolve(category)
        Files.createDirectories(outputDir)

        val outputFile = outputDir.resolve("$fileName.json")
        val jsonString = gson.toJson(data)
        Files.writeString(outputFile, jsonString)

        println("✅ Recorded: $outputFile")
    }

    /**
     * 리스트 데이터를 JSON 파일로 저장
     * 데이터가 MAX_RECORD_SIZE를 초과하면 처음 MAX_RECORD_SIZE개만 레코딩
     */
    inline fun <reified T> recordList(data: List<T>, category: String, fileName: String) {
        if (!RecordingConfig.isRecordingEnabled) return

        if (data.isEmpty()) {
            println("⚠️ 경고: $category/$fileName 에 레코딩할 데이터가 없습니다.")
            return
        }

        val recordData = if (data.size > MAX_RECORD_SIZE) {
            println("⚠️ 데이터가 너무 큽니다 (${data.size}개). 처음 $MAX_RECORD_SIZE 개만 레코딩합니다.")
            data.take(MAX_RECORD_SIZE)
        } else {
            data
        }

        record(recordData, category, fileName)
    }
}
```

### 5.2 RecordingConfig

```kotlin
package com.ulalax.ufc.utils

import java.nio.file.Path
import kotlin.io.path.Path

/**
 * 레코딩 설정 및 경로 관리
 *
 * ResponseRecorder가 API 응답을 저장할 때 사용하는 설정입니다.
 * - 레코딩 활성화 여부: -Precord.responses=true로 설정
 * - 저장 경로: src/liveTest/resources/responses/
 */
object RecordingConfig {
    /**
     * 레코딩 활성화 여부
     *
     * 기본값: true (Live Test 실행 시 자동으로 응답 레코딩)
     * 레코딩 비활성화: ./gradlew liveTest -Precord.responses=false
     *
     * 사용 예시:
     * ```bash
     * # 레코딩 활성화 (기본)
     * ./gradlew liveTest
     *
     * # 레코딩 비활성화
     * ./gradlew liveTest -Precord.responses=false
     * ```
     */
    val isRecordingEnabled: Boolean
        get() = System.getProperty("record.responses", "true").toBoolean()

    /**
     * 레코딩 파일 저장 경로
     * Live Test의 리소스 경로로 저장됩니다.
     */
    val baseOutputPath: Path = Path("src/liveTest/resources/responses")

    /**
     * API별 레코딩 경로 상수
     *
     * ResponseRecorder 및 JsonResponseLoader에서
     * 일관된 경로를 사용하도록 합니다.
     */
    object Paths {
        /**
         * Yahoo Finance API 레코딩 경로
         */
        object Yahoo {
            object Etf {
                const val TOP_HOLDINGS = "yahoo/etf/top_holdings"
                const val SECTOR_WEIGHTINGS = "yahoo/etf/sector_weightings"
                const val ASSET_ALLOCATION = "yahoo/etf/asset_allocation"
                const val FUND_PROFILE = "yahoo/etf/fund_profile"
                const val EQUITY_HOLDINGS = "yahoo/etf/equity_holdings"
                const val BOND_HOLDINGS = "yahoo/etf/bond_holdings"
            }

            object Ticker {
                const val HISTORY = "yahoo/ticker/history"
                const val DIVIDENDS = "yahoo/ticker/dividends"
                const val SPLITS = "yahoo/ticker/splits"
                const val FINANCIALS = "yahoo/ticker/financials"
                const val INFO = "yahoo/ticker/info"
                const val RECOMMENDATIONS = "yahoo/ticker/recommendations"
            }

            object Chart {
                const val INTRADAY = "yahoo/chart/intraday"
                const val DAILY = "yahoo/chart/daily"
                const val ADJUSTED = "yahoo/chart/adjusted"
            }

            object Search {
                const val BASIC = "yahoo/search/basic"
                const val SCREENER = "yahoo/search/screener"
            }
        }

        /**
         * FRED API 레코딩 경로
         */
        object Fred {
            const val SERIES = "fred/series"
            const val SERIES_INFO = "fred/series_info"
            const val VINTAGE = "fred/vintage"
            const val SEARCH = "fred/search"
            const val SEARCH_BY_CATEGORY = "fred/search/category"
            const val SEARCH_BY_RELEASE = "fred/search/release"
            const val CATEGORY = "fred/category"
            const val INDICATORS = "fred/indicators"
        }
    }
}
```

### 5.3 TestSymbols

```kotlin
package com.ulalax.ufc.utils

/**
 * 테스트에서 사용하는 심볼 상수
 *
 * 일관된 테스트 데이터를 위해 사전 정의된 심볼을 사용합니다.
 */
object TestSymbols {
    // ETFs
    const val SPY = "SPY"           // SPDR S&P 500 ETF Trust
    const val QQQ = "QQQ"           // Invesco QQQ Trust
    const val IWM = "IWM"           // iShares Russell 2000 ETF
    const val VTI = "VTI"           // Vanguard Total Stock Market ETF
    const val AGG = "AGG"           // iShares Core U.S. Aggregate Bond ETF

    // Stocks
    const val AAPL = "AAPL"         // Apple Inc.
    const val MSFT = "MSFT"         // Microsoft Corporation
    const val GOOGL = "GOOGL"       // Alphabet Inc.
    const val AMZN = "AMZN"         // Amazon.com Inc.
    const val NVDA = "NVDA"         // NVIDIA Corporation

    // FRED Series IDs
    const val GDP = "GDPC1"                 // Real GDP
    const val UNEMPLOYMENT = "UNRATE"       // Unemployment Rate
    const val CPI = "CPIAUCSL"              // Consumer Price Index
    const val FEDERAL_FUNDS_RATE = "DFF"    // Federal Funds Rate
    const val TREASURY_10Y = "DGS10"        // 10-Year Treasury Rate
}
```

---

## 6. 테스트 데이터 관리

### 6.1 데이터 복사 전략

**Live Test → Unit Test 데이터 복사:**

```bash
# Live Test 실행 후 레코딩된 데이터를 Unit Test resources로 복사
./gradlew liveTest

# 데이터 복사 (선택적)
cp -r src/liveTest/resources/responses/* src/test/resources/responses/
```

**자동화 옵션:**

```kotlin
// build.gradle.kts에 추가
tasks.register<Copy>("copyTestResponses") {
    description = "Copy recorded responses from liveTest to test resources"
    from("src/liveTest/resources/responses")
    into("src/test/resources/responses")
}

// liveTest 후 자동 실행
tasks.named("liveTest") {
    finalizedBy("copyTestResponses")
}
```

### 6.2 데이터 버전 관리

**Git 전략:**

```gitignore
# .gitignore

# Live Test 레코딩 결과는 무시 (옵션)
# src/liveTest/resources/responses/

# Unit Test 데이터는 커밋
# src/test/resources/responses/
```

**권장 사항:**
- Unit Test 데이터는 Git에 커밋
- Live Test 레코딩 결과는 필요시만 커밋
- 큰 파일은 Git LFS 사용 고려

---

## 7. Gradle 설정

### 7.1 SourceSets 설정

```kotlin
// build.gradle.kts

sourceSets {
    // Live Test용 소스 디렉토리 (recorded API responses)
    create("liveTest") {
        kotlin {
            srcDir("src/liveTest/kotlin")
        }
        resources {
            srcDir("src/liveTest/resources")
        }
        // Main 출력만 포함 - test와 완전 분리
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

// Live Test용 Configuration
val liveTestImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

dependencies {
    // Live Test 의존성
    liveTestImplementation(libs.junit.jupiter)
    liveTestImplementation(libs.assertj.core)
    liveTestImplementation(libs.kotlinx.coroutines.test)
    liveTestImplementation("com.google.code.gson:gson:2.11.0")

    // Unit Test 의존성
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("com.google.code.gson:gson:2.11.0")
}
```

### 7.2 Test Tasks 설정

```kotlin
// build.gradle.kts

// Live Test Task
val liveTest = tasks.register<Test>("liveTest") {
    description = "Runs live tests that make actual API calls"
    group = "verification"

    testClassesDirs = sourceSets["liveTest"].output.classesDirs
    classpath = sourceSets["liveTest"].runtimeClasspath

    useJUnitPlatform {
        includeTags("live")
    }

    // 레코딩 활성화 플래그 전달 (기본값: true)
    systemProperty("record.responses",
        if (project.hasProperty("record.responses")) {
            project.property("record.responses").toString()
        } else {
            "true"
        }
    )

    // FRED API key 전달
    val localProperties = Properties()
    val localPropertiesFile = file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { localProperties.load(it) }
    }

    localProperties.getProperty("FRED_API_KEY")?.let { apiKey ->
        environment("FRED_API_KEY", apiKey)
    }

    // 타임아웃 설정 (Live Test는 오래 걸림)
    timeout.set(Duration.ofMinutes(30))

    // 병렬 실행 비활성화 (Rate Limiting 준수)
    maxParallelForks = 1

    shouldRunAfter(tasks.test)

    // 항상 테스트 실행 (캐시 무시)
    outputs.upToDateWhen { false }
}

// Unit Test Task
tasks.test {
    useJUnitPlatform {
        excludeTags("live")
    }
}

// Check Task에 liveTest 포함
tasks.check {
    dependsOn(liveTest)
}
```

### 7.3 Resource Processing

```kotlin
// build.gradle.kts

tasks.named("processLiveTestResources", Copy::class) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named("processTestResources", Copy::class) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
```

---

## 8. 테스트 작성 가이드

### 8.1 Live Test 작성 체크리스트

- [ ] `LiveTestBase` 상속
- [ ] `@Tag("live")` 어노테이션
- [ ] `@DisplayName`으로 명확한 테스트 의도 표현
- [ ] Given-When-Then 패턴 사용
- [ ] `liveTest { }` 헬퍼 함수 사용
- [ ] 테스트 데이터는 `TestSymbols` 사용
- [ ] 응답 데이터 `ResponseRecorder`로 레코딩
- [ ] 콘솔 출력으로 주요 정보 표시
- [ ] 충분한 assertion으로 응답 검증

**템플릿:**

```kotlin
package com.ulalax.ufc.live.yahoo.etf

import com.ulalax.ufc.utils.LiveTestBase
import com.ulalax.ufc.utils.RecordingConfig
import com.ulalax.ufc.utils.ResponseRecorder
import com.ulalax.ufc.utils.TestSymbols
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * [기능] Live Test
 *
 * ## 목적
 * - [기능 설명]
 * - [레코딩 목적]
 *
 * ## API 사용법
 * ```kotlin
 * val ufc = UFCClient.create()
 * val result = ufc.yahoo.[method]()
 * ```
 *
 * ## 레코딩 파일
 * - `src/liveTest/resources/responses/[category]/[filename].json`
 */
class [Feature]LiveTest : LiveTestBase() {

    @Test
    @DisplayName("[무엇을 할 수 있는지 명시]")
    fun test[Feature]_[Scenario]() = liveTest {
        // Given: [테스트 전제 조건]

        // When: [실행할 작업]

        // Then: [검증 내용]

        // 콘솔 출력

        // 응답 레코딩
        ResponseRecorder.record(
            data = result,
            category = RecordingConfig.Paths.Yahoo.[Category],
            fileName = "[filename]"
        )
    }
}
```

### 8.2 Unit Test 작성 체크리스트

- [ ] `JsonResponseLoader`로 레코딩된 데이터 로드
- [ ] `@DisplayName`으로 명확한 테스트 의도 표현
- [ ] Given-When-Then 패턴 사용 (선택)
- [ ] 데이터 파싱 검증
- [ ] 데이터 변환 로직 검증
- [ ] 엣지 케이스 처리 검증
- [ ] 충분한 assertion

**템플릿:**

```kotlin
package com.ulalax.ufc.source.yahoo

import com.ulalax.ufc.utils.JsonResponseLoader
import com.ulalax.ufc.utils.RecordingConfig
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * [Feature] Unit Test
 *
 * ## 목적
 * - [테스트 목적]
 * - [검증할 로직]
 *
 * ## 테스트 데이터
 * - `src/test/resources/responses/[category]/[filename].json`
 */
class [Feature]Test {

    @Test
    @DisplayName("[무엇을 할 수 있는지 명시]")
    fun test[Feature]_[Scenario]() {
        // Given: 레코딩된 데이터
        val data = JsonResponseLoader.load<[Type]>(
            category = RecordingConfig.Paths.Yahoo.[Category],
            fileName = "[filename]"
        )

        // When: [실행할 작업 (선택)]

        // Then: [검증 내용]
    }
}
```

### 8.3 테스트 네이밍 규칙

**클래스명:**
- Live Test: `[Feature]LiveTest`
- Unit Test: `[Feature]Test`

**메서드명:**
- Live Test: `test[Feature]_[Scenario]`
- Unit Test: `test[Feature]_[Scenario]`

**예시:**
```kotlin
// Live Test
class EtfTopHoldingsLiveTest {
    fun testGetTopHoldings_SPY()
    fun testGetTopHoldings_QQQ()
}

// Unit Test
class EtfTest {
    fun testParseTopHoldings_SPY()
    fun testSortHoldingsByWeight()
}
```

---

## 9. 실행 예제

### 9.1 Live Test 실행

```bash
# 모든 Live Test 실행 (레코딩 활성화)
./gradlew liveTest

# 레코딩 비활성화
./gradlew liveTest -Precord.responses=false

# 특정 테스트만 실행
./gradlew liveTest --tests "EtfTopHoldingsLiveTest"

# 병렬 실행 (주의: Rate Limiting)
./gradlew liveTest --parallel
```

### 9.2 Unit Test 실행

```bash
# 모든 Unit Test 실행
./gradlew test

# 특정 테스트만 실행
./gradlew test --tests "EtfTest"

# 빠른 실행 (병렬)
./gradlew test --parallel
```

### 9.3 전체 테스트 실행

```bash
# Live Test + Unit Test
./gradlew check

# CI/CD에서는 Unit Test만 실행 권장
./gradlew test
```

---

## 10. CI/CD 통합

### 10.1 GitHub Actions 예제

```yaml
name: Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run Unit Tests
        run: ./gradlew test

      - name: Upload Test Report
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-report
          path: build/reports/tests/test/

  live-tests:
    runs-on: ubuntu-latest
    # Live Test는 수동 트리거 또는 스케줄링
    if: github.event_name == 'workflow_dispatch'
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Create local.properties
        run: |
          echo "FRED_API_KEY=${{ secrets.FRED_API_KEY }}" > local.properties

      - name: Run Live Tests
        run: ./gradlew liveTest

      - name: Clean up secrets
        if: always()
        run: rm -f local.properties

      - name: Upload Recorded Responses
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: recorded-responses
          path: src/liveTest/resources/responses/
```

---

## 11. 참고 자료

- **KFC (Korea Financial Client)**: https://github.com/kairoscode/kfc
- **JUnit 5 Documentation**: https://junit.org/junit5/docs/current/user-guide/
- **kotlinx.coroutines.test**: https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-test/

---

**다음 문서**: [README.md](./README.md)
