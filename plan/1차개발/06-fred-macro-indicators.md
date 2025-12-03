# FRED Macro Indicators - 매크로 경제 지표

## 문서 정보
- **버전**: 2.0.0
- **최종 수정일**: 2025-12-02
- **작성자**: Claude Code (Software Architect)
- **문서 상태**: Active
- **참조**:
  - **FRED API 공식 문서**: https://fred.stlouisfed.org/docs/api/fred/
  - Python fredapi: https://github.com/mortada/fredapi

---

## 1. FRED API 개요

### 1.1 FRED란?

FRED (Federal Reserve Economic Data)는 세인트루이스 연방준비은행이 제공하는 경제 데이터베이스입니다.

**특징:**
- 800,000+ 시계열 데이터
- 미국 경제 지표 중심
- 무료 API 제공 (API Key 필요)
- Vintage Data (개정 이력) 지원

### 1.2 API Key 획득 및 설정

**1단계: API Key 발급**
1. https://fred.stlouisfed.org/docs/api/api_key.html 방문
2. 무료 계정 생성
3. API Key 발급

**2단계: local.properties 설정**

프로젝트 루트에 `local.properties` 파일을 생성하고 API Key를 설정합니다:

```properties
# local.properties (Git에 커밋하지 마세요!)
FRED_API_KEY=your_fred_api_key_here
```

**템플릿 파일:**
- `local.properties.template` 파일을 복사하여 `local.properties` 생성
- API Key를 입력

**보안 주의사항:**
- `.gitignore`에 `local.properties` 추가됨
- API Key를 절대 Git에 커밋하지 마세요

### 1.3 공식 API 문서

**FRED API 전체 문서**: https://fred.stlouisfed.org/docs/api/fred/

주요 엔드포인트:
- **Series Observations**: `/fred/series/observations` - 시계열 데이터 조회
- **Series Search**: `/fred/series/search` - 시리즈 검색
- **Series Info**: `/fred/series` - 시리즈 메타데이터
- **Series Vintage**: `/fred/series/vintagedates` - 개정 이력
- **Categories**: `/fred/category` - 카테고리 정보
- **Releases**: `/fred/releases` - 발표 일정

모든 API 요청은 `api_key` 파라미터가 필요합니다.

---

## 2. FREDSource 인터페이스

```kotlin
interface FREDSource : DataSource {
    override val name: String get() = "FRED"

    // Series 조회
    suspend fun getSeries(seriesId: String, ...): Series
    suspend fun getSeriesInfo(seriesId: String): SeriesInfo
    
    // Vintage Data (개정 이력)
    suspend fun getSeriesFirstRelease(seriesId: String): Series
    suspend fun getSeriesAllReleases(seriesId: String, ...): List<VintageObservation>
    suspend fun getSeriesAsOfDate(seriesId: String, asOfDate: LocalDate): Series
    suspend fun getSeriesVintageDates(seriesId: String): List<LocalDate>
    
    // 검색
    suspend fun search(text: String, ...): List<SeriesInfo>
    suspend fun searchByCategory(categoryId: Int, ...): List<SeriesInfo>
    suspend fun searchByRelease(releaseId: Int, ...): List<SeriesInfo>
    
    // 카테고리
    suspend fun getCategory(categoryId: Int): Category
}
```

---

## 3. API 메서드 명세

### 3.1 getSeries - 시계열 데이터 조회

**Python fredapi 참조:**
```python
def get_series(self, series_id, observation_start=None, observation_end=None, **kwargs):
    """Get data for a Fred series id"""
```

**Kotlin 구현:**
```kotlin
suspend fun getSeries(
    seriesId: String,
    observationStart: LocalDate? = null,
    observationEnd: LocalDate? = null,
    frequency: DataFrequency? = null,
    aggregationMethod: AggregationMethod? = null,
    units: Units? = null,
    outputType: OutputType? = null
): Series
```

**API 엔드포인트:**
```
GET https://api.stlouisfed.org/fred/series/observations
?series_id={seriesId}
&api_key={apiKey}
&file_type=json
&observation_start={start}
&observation_end={end}
&frequency={freq}
&aggregation_method={method}
&units={units}
```

**파라미터:**

| 파라미터 | 타입 | 설명 | 예시 |
|---------|------|------|------|
| series_id | String | 시리즈 ID | "GDPC1", "UNRATE" |
| observation_start | LocalDate | 관찰 시작일 | "2020-01-01" |
| observation_end | LocalDate | 관찰 종료일 | "2024-01-01" |
| frequency | DataFrequency | 데이터 주기 | "m" (월간) |
| aggregation_method | AggregationMethod | 집계 방법 | "avg", "sum", "eop" |
| units | Units | 단위 변환 | "chg" (변화량), "pch" (변화율) |

### 3.2 getSeriesInfo - 시리즈 메타데이터

**Python fredapi 참조:**
```python
def get_series_info(self, series_id):
    """Get information about a series"""
```

**Kotlin 구현:**
```kotlin
suspend fun getSeriesInfo(seriesId: String): SeriesInfo
```

**API 엔드포인트:**
```
GET https://api.stlouisfed.org/fred/series
?series_id={seriesId}
&api_key={apiKey}
&file_type=json
```

**응답 예시:**
```json
{
  "seriess": [{
    "id": "GDPC1",
    "realtime_start": "2024-01-01",
    "realtime_end": "2024-12-31",
    "title": "Real Gross Domestic Product",
    "observation_start": "1947-01-01",
    "observation_end": "2024-01-01",
    "frequency": "Quarterly",
    "frequency_short": "Q",
    "units": "Billions of Chained 2017 Dollars",
    "units_short": "Bil. of Chn. 2017 $",
    "seasonal_adjustment": "Seasonally Adjusted Annual Rate",
    "seasonal_adjustment_short": "SAAR",
    "last_updated": "2024-01-25 07:45:27-06",
    "popularity": 95,
    "notes": "Real GDP measures..."
  }]
}
```

### 3.3 Vintage Data 메서드

#### 3.3.1 getSeriesFirstRelease

**Python fredapi 참조:**
```python
def get_series_first_release(self, series_id):
    """Get first-release data (ignore revisions)"""
    df = self.get_series_all_releases(series_id)
    first_release = df.groupby('date').head(1)
    return first_release.set_index('date')['value']
```

**Kotlin 구현:**
```kotlin
suspend fun getSeriesFirstRelease(seriesId: String): Series {
    val allReleases = getSeriesAllReleases(seriesId)
    val firstRelease = allReleases
        .groupBy { it.date }
        .mapValues { it.value.first() }
    
    return Series(
        id = seriesId,
        observations = firstRelease.values.toList()
    )
}
```

#### 3.3.2 getSeriesAllReleases

**Python fredapi 참조:**
```python
def get_series_all_releases(self, series_id, realtime_start=None, realtime_end=None):
    """Get all data including revisions"""
```

**Kotlin 구현:**
```kotlin
suspend fun getSeriesAllReleases(
    seriesId: String,
    realtimeStart: LocalDate? = null,
    realtimeEnd: LocalDate? = null
): List<VintageObservation>
```

**API 엔드포인트:**
```
GET https://api.stlouisfed.org/fred/series/observations
?series_id={seriesId}
&api_key={apiKey}
&realtime_start={start}
&realtime_end={end}
&file_type=json
```

**응답 구조:**
```kotlin
data class VintageObservation(
    val date: LocalDate,          // 관찰 날짜
    val realtimeStart: LocalDate, // 실시간 시작 (발표 날짜)
    val value: Double?            // 값
)
```

**예시:**
```
GDP for Q4 2013:
- 2014-01-30 발표: 17102.5 (첫 발표)
- 2014-02-28 발표: 17080.7 (1차 개정)
- 2014-03-27 발표: 17089.6 (2차 개정)
```

#### 3.3.3 getSeriesAsOfDate

**Python fredapi 참조:**
```python
def get_series_as_of_date(self, series_id, as_of_date):
    """Get latest data known on a particular date"""
    df = self.get_series_all_releases(series_id)
    return df[df['realtime_start'] <= as_of_date]
```

**Kotlin 구현:**
```kotlin
suspend fun getSeriesAsOfDate(
    seriesId: String,
    asOfDate: LocalDate
): Series {
    val allReleases = getSeriesAllReleases(seriesId)
    val filtered = allReleases.filter { it.realtimeStart <= asOfDate }
    
    return Series(
        id = seriesId,
        observations = filtered.map { 
            Observation(it.date, it.value) 
        }
    )
}
```

#### 3.3.4 getSeriesVintageDates

**Python fredapi 참조:**
```python
def get_series_vintage_dates(self, series_id):
    """Get list of vintage dates"""
```

**Kotlin 구현:**
```kotlin
suspend fun getSeriesVintageDates(seriesId: String): List<LocalDate>
```

**API 엔드포인트:**
```
GET https://api.stlouisfed.org/fred/series/vintagedates
?series_id={seriesId}
&api_key={apiKey}
&file_type=json
```

### 3.4 search - 전문 검색

**Python fredapi 참조:**
```python
def search(self, text, limit=1000, order_by=None, sort_order=None, filter=None):
    """Fulltext search for series"""
```

**Kotlin 구현:**
```kotlin
suspend fun search(
    text: String,
    limit: Int = 1000,
    orderBy: SearchOrderBy? = null,
    sortOrder: SortOrder? = null,
    filter: SearchFilter? = null
): List<SeriesInfo>
```

**API 엔드포인트:**
```
GET https://api.stlouisfed.org/fred/series/search
?search_text={text}
&api_key={apiKey}
&file_type=json
&limit={limit}
&order_by={orderBy}
&sort_order={sortOrder}
&filter_variable={filterVar}
&filter_value={filterVal}
```

**SearchOrderBy:**
```kotlin
enum class SearchOrderBy(val value: String) {
    SEARCH_RANK("search_rank"),
    SERIES_ID("series_id"),
    TITLE("title"),
    UNITS("units"),
    FREQUENCY("frequency"),
    SEASONAL_ADJUSTMENT("seasonal_adjustment"),
    REALTIME_START("realtime_start"),
    REALTIME_END("realtime_end"),
    LAST_UPDATED("last_updated"),
    OBSERVATION_START("observation_start"),
    OBSERVATION_END("observation_end"),
    POPULARITY("popularity")
}
```

### 3.5 searchByCategory

**Python fredapi 참조:**
```python
def search_by_category(self, category_id, limit=0, order_by=None, sort_order=None, filter=None):
    """Search for series that belongs to a category"""
```

**Kotlin 구현:**
```kotlin
suspend fun searchByCategory(
    categoryId: Int,
    limit: Int = 0,
    orderBy: SearchOrderBy? = null,
    sortOrder: SortOrder? = null
): List<SeriesInfo>
```

---

## 4. 데이터 모델

### 4.1 Series

```kotlin
data class Series(
    val id: String,
    val title: String,
    val observations: List<Observation>
) {
    fun toDataFrame(): AnyFrame {
        return dataFrameOf(
            "date" to observations.map { it.date },
            "value" to observations.map { it.value }
        )
    }
}
```

### 4.2 Observation

```kotlin
data class Observation(
    val date: LocalDate,
    val value: Double?
)
```

### 4.3 SeriesInfo

```kotlin
@Serializable
data class SeriesInfo(
    val id: String,
    val title: String,
    @SerialName("observation_start") val observationStart: String,
    @SerialName("observation_end") val observationEnd: String,
    val frequency: String,
    @SerialName("frequency_short") val frequencyShort: String,
    val units: String,
    @SerialName("units_short") val unitsShort: String,
    @SerialName("seasonal_adjustment") val seasonalAdjustment: String,
    @SerialName("seasonal_adjustment_short") val seasonalAdjustmentShort: String,
    @SerialName("last_updated") val lastUpdated: String,
    val popularity: Int,
    val notes: String?
)
```

### 4.4 Enum 타입

```kotlin
enum class DataFrequency(val value: String) {
    Daily("d"),
    Weekly("w"),
    Biweekly("bw"),
    Monthly("m"),
    Quarterly("q"),
    Semiannual("sa"),
    Annual("a")
}

enum class AggregationMethod(val value: String) {
    Average("avg"),
    Sum("sum"),
    EndOfPeriod("eop")
}

enum class Units(val value: String) {
    Levels("lin"),
    Change("chg"),
    ChangeFromYearAgo("ch1"),
    PercentChange("pch"),
    PercentChangeFromYearAgo("pc1"),
    CompoundedAnnualRateOfChange("pca"),
    ContinuouslyCompoundedRateOfChange("cch"),
    ContinuouslyCompoundedAnnualRateOfChange("cca"),
    NaturalLog("log")
}
```

---

## 5. 주요 경제 지표별 강타입 클래스

각 주요 경제 지표는 특화된 데이터 클래스와 헬퍼 메서드를 제공합니다.

### 5.1 GDP (Gross Domestic Product)

```kotlin
/**
 * GDP 데이터
 *
 * 국내총생산(GDP)은 한 나라의 경제 규모를 나타내는 핵심 지표입니다.
 */
@Serializable
data class GDPData(
    val date: LocalDate,                    // 날짜
    val value: Double,                      // GDP 값 (Billions of Dollars)
    val unit: String = "Billions of Dollars", // 단위
    val seasonallyAdjusted: Boolean = true, // 계절 조정 여부
    val frequency: String = "Quarterly"     // 주기 (Quarterly, Annual)
)

/**
 * GDP 지표 클래스
 */
class GDP(private val fred: FREDSource) {

    companion object {
        const val REAL_GDP_SERIES = "GDPC1"         // 실질 GDP (2017년 기준)
        const val NOMINAL_GDP_SERIES = "GDP"         // 명목 GDP
        const val POTENTIAL_GDP_SERIES = "GDPPOT"   // 잠재 GDP
    }

    /**
     * 분기별 실질 GDP 조회
     */
    suspend fun quarterly(
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<GDPData> {
        val series = fred.getSeries(
            seriesId = REAL_GDP_SERIES,
            observationStart = start,
            observationEnd = end
        )

        return series.observations.map { obs ->
            GDPData(
                date = obs.date,
                value = obs.value ?: 0.0,
                unit = "Billions of Chained 2017 Dollars",
                seasonallyAdjusted = true,
                frequency = "Quarterly"
            )
        }
    }

    /**
     * 연간 실질 GDP 조회
     */
    suspend fun annual(
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<GDPData> {
        val series = fred.getSeries(
            seriesId = REAL_GDP_SERIES,
            observationStart = start,
            observationEnd = end,
            frequency = DataFrequency.Annual,
            aggregationMethod = AggregationMethod.Average
        )

        return series.observations.map { obs ->
            GDPData(
                date = obs.date,
                value = obs.value ?: 0.0,
                unit = "Billions of Chained 2017 Dollars",
                seasonallyAdjusted = true,
                frequency = "Annual"
            )
        }
    }

    /**
     * 최신 GDP 값
     */
    suspend fun current(): GDPData {
        return quarterly().last()
    }

    /**
     * GDP 성장률 계산 (YoY)
     */
    suspend fun growthRate(
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<GDPGrowthData> {
        val series = fred.getSeries(
            seriesId = REAL_GDP_SERIES,
            observationStart = start,
            observationEnd = end,
            units = Units.PercentChangeFromYearAgo
        )

        return series.observations.map { obs ->
            GDPGrowthData(
                date = obs.date,
                growthRate = obs.value ?: 0.0,
                unit = "Percent"
            )
        }
    }

}

/**
 * GDP 성장률 데이터
 */
data class GDPGrowthData(
    val date: LocalDate,
    val growthRate: Double,  // 성장률 (%)
    val unit: String = "Percent"
)
```

### 5.2 Unemployment Rate (실업률)

```kotlin
/**
 * 실업률 데이터
 */
@Serializable
data class UnemploymentData(
    val date: LocalDate,                // 날짜
    val rate: Double,                   // 실업률 (%)
    val laborForce: Long? = null,       // 노동력 (천 명)
    val employed: Long? = null,         // 고용자 수 (천 명)
    val unemployed: Long? = null        // 실업자 수 (천 명)
)

/**
 * 실업률 지표 클래스
 */
class UnemploymentRate(private val fred: FREDSource) {

    companion object {
        const val UNEMPLOYMENT_RATE_SERIES = "UNRATE"      // 실업률
        const val LABOR_FORCE_SERIES = "CLF16OV"           // 노동력
        const val EMPLOYED_SERIES = "CE16OV"               // 고용자 수
        const val UNEMPLOYED_SERIES = "UNEMPLOY"           // 실업자 수
        const val NONFARM_PAYROLLS_SERIES = "PAYEMS"       // 비농업 고용
    }

    /**
     * 월별 실업률 조회
     */
    suspend fun monthly(
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<UnemploymentData> {
        val series = fred.getSeries(
            seriesId = UNEMPLOYMENT_RATE_SERIES,
            observationStart = start,
            observationEnd = end
        )

        return series.observations.map { obs ->
            UnemploymentData(
                date = obs.date,
                rate = obs.value ?: 0.0
            )
        }
    }

    /**
     * 상세 실업 통계 조회 (실업률 + 노동력 + 고용/실업자 수)
     */
    suspend fun detailed(
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<UnemploymentData> {
        // 병렬로 모든 시리즈 조회
        val (rateData, laborForceData, employedData, unemployedData) = coroutineScope {
            listOf(
                async { fred.getSeries(UNEMPLOYMENT_RATE_SERIES, start, end) },
                async { fred.getSeries(LABOR_FORCE_SERIES, start, end) },
                async { fred.getSeries(EMPLOYED_SERIES, start, end) },
                async { fred.getSeries(UNEMPLOYED_SERIES, start, end) }
            ).awaitAll()
        }

        // 날짜별로 데이터 결합
        return rateData.observations.map { rateObs ->
            val laborForce = laborForceData.observations
                .find { it.date == rateObs.date }?.value?.toLong()
            val employed = employedData.observations
                .find { it.date == rateObs.date }?.value?.toLong()
            val unemployed = unemployedData.observations
                .find { it.date == rateObs.date }?.value?.toLong()

            UnemploymentData(
                date = rateObs.date,
                rate = rateObs.value ?: 0.0,
                laborForce = laborForce,
                employed = employed,
                unemployed = unemployed
            )
        }
    }

    /**
     * 최신 실업률
     */
    suspend fun current(): UnemploymentData {
        return monthly().last()
    }
}
```

### 5.3 Inflation (CPI - Consumer Price Index)

```kotlin
/**
 * CPI (소비자 물가 지수) 데이터
 */
@Serializable
data class CPIData(
    val date: LocalDate,                // 날짜
    val index: Double,                  // CPI 지수 (1982-84=100)
    val monthOverMonth: Double? = null, // 전월 대비 변화율 (%)
    val yearOverYear: Double? = null    // 전년 동월 대비 변화율 (%)
)

/**
 * CPI (소비자 물가 지수) 지표 클래스
 */
class CPI(private val fred: FREDSource) {

    companion object {
        const val CPI_ALL_ITEMS_SERIES = "CPIAUCSL"        // 전체 항목 CPI
        const val CPI_CORE_SERIES = "CPILFESL"             // 핵심 CPI (식품, 에너지 제외)
        const val PCE_SERIES = "PCEPI"                     // PCE 물가 지수
        const val PCE_CORE_SERIES = "PCEPILFE"             // 핵심 PCE
    }

    /**
     * 월별 CPI 지수 조회
     */
    suspend fun monthly(
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<CPIData> {
        val series = fred.getSeries(
            seriesId = CPI_ALL_ITEMS_SERIES,
            observationStart = start,
            observationEnd = end
        )

        return series.observations.map { obs ->
            CPIData(
                date = obs.date,
                index = obs.value ?: 0.0
            )
        }
    }

    /**
     * 인플레이션율 조회 (YoY % 변화)
     */
    suspend fun inflationRate(
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<CPIData> {
        val series = fred.getSeries(
            seriesId = CPI_ALL_ITEMS_SERIES,
            observationStart = start,
            observationEnd = end,
            units = Units.PercentChangeFromYearAgo
        )

        return series.observations.map { obs ->
            CPIData(
                date = obs.date,
                index = 0.0,
                yearOverYear = obs.value
            )
        }
    }

    /**
     * 월별 변화율 포함 CPI 조회
     */
    suspend fun withChanges(
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<CPIData> {
        // 병렬 조회
        val (indexData, momData, yoyData) = coroutineScope {
            listOf(
                async { fred.getSeries(CPI_ALL_ITEMS_SERIES, start, end) },
                async { fred.getSeries(CPI_ALL_ITEMS_SERIES, start, end, units = Units.PercentChange) },
                async { fred.getSeries(CPI_ALL_ITEMS_SERIES, start, end, units = Units.PercentChangeFromYearAgo) }
            ).awaitAll()
        }

        return indexData.observations.map { obs ->
            val mom = momData.observations.find { it.date == obs.date }?.value
            val yoy = yoyData.observations.find { it.date == obs.date }?.value

            CPIData(
                date = obs.date,
                index = obs.value ?: 0.0,
                monthOverMonth = mom,
                yearOverYear = yoy
            )
        }
    }

    /**
     * 최신 CPI
     */
    suspend fun current(): CPIData {
        return monthly().last()
    }
}
```

### 5.4 Interest Rates (금리)

```kotlin
/**
 * 금리 데이터
 */
@Serializable
data class InterestRateData(
    val date: LocalDate,                // 날짜
    val rate: Double,                   // 금리 (% per annum)
    val term: String                    // 만기 (Overnight, 10-Year 등)
)

/**
 * 금리 지표 클래스
 */
class InterestRates(private val fred: FREDSource) {

    companion object {
        const val FEDERAL_FUNDS_RATE_SERIES = "DFF"        // 연방 기금 금리
        const val TREASURY_1M_SERIES = "DGS1MO"            // 1개월물 국채
        const val TREASURY_3M_SERIES = "DGS3MO"            // 3개월물 국채
        const val TREASURY_6M_SERIES = "DGS6MO"            // 6개월물 국채
        const val TREASURY_1Y_SERIES = "DGS1"              // 1년물 국채
        const val TREASURY_2Y_SERIES = "DGS2"              // 2년물 국채
        const val TREASURY_5Y_SERIES = "DGS5"              // 5년물 국채
        const val TREASURY_10Y_SERIES = "DGS10"            // 10년물 국채
        const val TREASURY_30Y_SERIES = "DGS30"            // 30년물 국채
    }

    /**
     * 연방 기금 금리 조회
     */
    suspend fun federalFundsRate(
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<InterestRateData> {
        val series = fred.getSeries(
            seriesId = FEDERAL_FUNDS_RATE_SERIES,
            observationStart = start,
            observationEnd = end
        )

        return series.observations.map { obs ->
            InterestRateData(
                date = obs.date,
                rate = obs.value ?: 0.0,
                term = "Overnight"
            )
        }
    }

    /**
     * 국채 수익률 조회
     */
    suspend fun treasuryYield(
        term: TreasuryTerm,
        start: LocalDate? = null,
        end: LocalDate? = null
    ): List<InterestRateData> {
        val series = fred.getSeries(
            seriesId = term.seriesId,
            observationStart = start,
            observationEnd = end
        )

        return series.observations.map { obs ->
            InterestRateData(
                date = obs.date,
                rate = obs.value ?: 0.0,
                term = term.displayName
            )
        }
    }

    /**
     * 수익률 곡선 조회 (특정 날짜)
     */
    suspend fun yieldCurve(date: LocalDate): List<InterestRateData> {
        val terms = listOf(
            TreasuryTerm.ONE_MONTH,
            TreasuryTerm.THREE_MONTHS,
            TreasuryTerm.SIX_MONTHS,
            TreasuryTerm.ONE_YEAR,
            TreasuryTerm.TWO_YEARS,
            TreasuryTerm.FIVE_YEARS,
            TreasuryTerm.TEN_YEARS,
            TreasuryTerm.THIRTY_YEARS
        )

        // 병렬로 모든 만기 조회
        val results = coroutineScope {
            terms.map { term ->
                async {
                    val series = fred.getSeries(
                        seriesId = term.seriesId,
                        observationStart = date.minusDays(5),
                        observationEnd = date
                    )
                    series.observations.lastOrNull()?.let { obs ->
                        InterestRateData(
                            date = obs.date,
                            rate = obs.value ?: 0.0,
                            term = term.displayName
                        )
                    }
                }
            }.awaitAll().filterNotNull()
        }

        return results
    }
}

/**
 * 국채 만기 Enum
 */
enum class TreasuryTerm(val seriesId: String, val displayName: String) {
    ONE_MONTH("DGS1MO", "1-Month"),
    THREE_MONTHS("DGS3MO", "3-Month"),
    SIX_MONTHS("DGS6MO", "6-Month"),
    ONE_YEAR("DGS1", "1-Year"),
    TWO_YEARS("DGS2", "2-Year"),
    FIVE_YEARS("DGS5", "5-Year"),
    TEN_YEARS("DGS10", "10-Year"),
    THIRTY_YEARS("DGS30", "30-Year")
}
```

### 5.5 기타 주요 지표

```kotlin
/**
 * PCE (개인 소비 지출) 지표
 */
class PCE(private val fred: FREDSource) {
    companion object {
        const val PCE_SERIES = "PCE"                       // PCE
        const val PCE_PRICE_INDEX_SERIES = "PCEPI"         // PCE 물가 지수
        const val PCE_CORE_SERIES = "PCEPILFE"             // 핵심 PCE
    }

    suspend fun monthly(start: LocalDate? = null, end: LocalDate? = null): List<MacroData> {
        // 구현...
    }
}

/**
 * 소비자 신뢰 지수
 */
class ConsumerSentiment(private val fred: FREDSource) {
    companion object {
        const val UMICH_SENTIMENT_SERIES = "UMCSENT"       // University of Michigan 소비자 신뢰 지수
    }

    suspend fun monthly(start: LocalDate? = null, end: LocalDate? = null): List<MacroData> {
        // 구현...
    }
}

/**
 * 비농업 고용
 */
class NonfarmPayrolls(private val fred: FREDSource) {
    companion object {
        const val PAYROLLS_SERIES = "PAYEMS"               // 비농업 고용 (천 명)
    }

    suspend fun monthly(start: LocalDate? = null, end: LocalDate? = null): List<MacroData> {
        // 구현...
    }
}
```

---

## 6. 통합 경제 지표 Enum

```kotlin
/**
 * FRED 주요 경제 지표 Enum (레거시 호환)
 */
enum class EconomicIndicator(val seriesId: String, val description: String) {
    // GDP
    REAL_GDP("GDPC1", "Real GDP"),
    NOMINAL_GDP("GDP", "Nominal GDP"),
    POTENTIAL_GDP("GDPPOT", "Potential GDP"),

    // 실업
    UNEMPLOYMENT_RATE("UNRATE", "Unemployment Rate"),
    NONFARM_PAYROLLS("PAYEMS", "Nonfarm Payrolls"),
    LABOR_FORCE_PARTICIPATION("CIVPART", "Labor Force Participation Rate"),

    // 인플레이션
    CPI("CPIAUCSL", "Consumer Price Index"),
    CPI_CORE("CPILFESL", "Core CPI"),
    PCE("PCE", "Personal Consumption Expenditures"),
    PCE_CORE("PCEPILFE", "Core PCE"),
    PPI("PPIACO", "Producer Price Index"),

    // 금리
    FEDERAL_FUNDS_RATE("DFF", "Federal Funds Rate"),
    TREASURY_10Y("DGS10", "10-Year Treasury Yield"),
    TREASURY_30Y("DGS30", "30-Year Treasury Yield"),
    TREASURY_2Y("DGS2", "2-Year Treasury Yield"),

    // 통화
    M1("M1SL", "M1 Money Stock"),
    M2("M2SL", "M2 Money Stock"),

    // 소비자 신뢰
    CONSUMER_SENTIMENT("UMCSENT", "Consumer Sentiment Index"),
    RETAIL_SALES("RSXFS", "Retail Sales"),

    // 주택
    CASE_SHILLER_HOME_PRICE("CSUSHPISA", "Case-Shiller Home Price Index"),
    HOUSING_STARTS("HOUST", "Housing Starts"),

    // 무역
    TRADE_BALANCE("BOPGSTB", "Trade Balance"),
    REAL_TRADE_WEIGHTED_USD("DTWEXBGS", "Trade Weighted USD Index");

    suspend fun fetch(fred: FREDSource): Series {
        return fred.getSeries(seriesId)
    }
}
```

---

## 7. 사용 예시

### 7.1 강타입 클래스 사용

```kotlin
suspend fun main() {
    val ufc = UFCClient.create(
        config = UFCClientConfig(
            fredApiKey = "your_api_key"
        )
    )

    // GDP 클래스 사용
    val gdp = GDP(ufc.fred)

    // 분기별 GDP 조회
    val quarterlyGDP = gdp.quarterly(
        start = LocalDate.of(2020, 1, 1),
        end = LocalDate.of(2024, 1, 1)
    )
    quarterlyGDP.forEach {
        println("${it.date}: ${it.value} ${it.unit}")
    }

    // 최신 GDP
    val currentGDP = gdp.current()
    println("Current GDP: ${currentGDP.value} ${currentGDP.unit}")

    // GDP 성장률
    val growth = gdp.growthRate()
    growth.forEach {
        println("${it.date}: ${it.growthRate}%")
    }
}
```

### 7.2 실업률 상세 조회

```kotlin
suspend fun analyzeUnemployment() {
    val ufc = UFCClient.create(config = UFCClientConfig(fredApiKey = "your_api_key"))
    val unemployment = UnemploymentRate(ufc.fred)

    // 상세 실업 통계 (실업률 + 노동력 + 고용/실업자 수)
    val detailed = unemployment.detailed(
        start = LocalDate.of(2023, 1, 1),
        end = LocalDate.of(2024, 1, 1)
    )

    detailed.forEach { data ->
        println("""
            Date: ${data.date}
            Unemployment Rate: ${data.rate}%
            Labor Force: ${data.laborForce?.let { "$it thousand" } ?: "N/A"}
            Employed: ${data.employed?.let { "$it thousand" } ?: "N/A"}
            Unemployed: ${data.unemployed?.let { "$it thousand" } ?: "N/A"}
        """.trimIndent())
    }
}
```

### 7.3 인플레이션 분석

```kotlin
suspend fun analyzeInflation() {
    val ufc = UFCClient.create(config = UFCClientConfig(fredApiKey = "your_api_key"))
    val cpi = CPI(ufc.fred)

    // 변화율 포함 CPI 조회
    val data = cpi.withChanges(
        start = LocalDate.of(2023, 1, 1),
        end = LocalDate.of(2024, 1, 1)
    )

    data.forEach {
        println("""
            Date: ${it.date}
            CPI Index: ${it.index}
            Month-over-Month: ${it.monthOverMonth?.let { "$it%" } ?: "N/A"}
            Year-over-Year: ${it.yearOverYear?.let { "$it%" } ?: "N/A"}
        """.trimIndent())
    }

    // 인플레이션율만 조회
    val inflationRate = cpi.inflationRate()
    println("Latest Inflation Rate: ${inflationRate.last().yearOverYear}%")
}
```

### 7.4 금리 분석 (수익률 곡선)

```kotlin
suspend fun analyzeInterestRates() {
    val ufc = UFCClient.create(config = UFCClientConfig(fredApiKey = "your_api_key"))
    val rates = InterestRates(ufc.fred)

    // 연방 기금 금리
    val fedRate = rates.federalFundsRate()
    println("Latest Fed Funds Rate: ${fedRate.last().rate}%")

    // 10년물 국채 수익률
    val treasury10Y = rates.treasuryYield(
        term = TreasuryTerm.TEN_YEARS,
        start = LocalDate.of(2023, 1, 1)
    )
    treasury10Y.forEach {
        println("${it.date}: ${it.rate}% (${it.term})")
    }

    // 오늘의 수익률 곡선
    val yieldCurve = rates.yieldCurve(LocalDate.now())
    println("\nYield Curve (${LocalDate.now()}):")
    yieldCurve.forEach {
        println("${it.term}: ${it.rate}%")
    }
}
```

### 7.5 통합 매크로 분석

```kotlin
suspend fun comprehensiveMacroAnalysis() {
    val ufc = UFCClient.create(config = UFCClientConfig(fredApiKey = "your_api_key"))

    // 병렬로 모든 지표 조회
    val (gdpData, unemploymentData, cpiData, ratesData) = coroutineScope {
        val gdp = GDP(ufc.fred)
        val unemployment = UnemploymentRate(ufc.fred)
        val cpi = CPI(ufc.fred)
        val rates = InterestRates(ufc.fred)

        listOf(
            async { gdp.quarterly() },
            async { unemployment.monthly() },
            async { cpi.monthly() },
            async { rates.federalFundsRate() }
        ).awaitAll()
    }

    // 데이터 출력
    println("GDP Data Points: ${gdpData.size}")
    println("Unemployment Data Points: ${unemploymentData.size}")
    println("CPI Data Points: ${cpiData.size}")
    println("Fed Rate Data Points: ${ratesData.size}")

    // 데이터 분석이 필요한 경우:
    // 1. Kotlin DataFrame 라이브러리 추가하여 분석
    // 2. JSON으로 직렬화하여 Python pandas 사용
    // 3. Apache Commons Math로 상관관계 분석
}
```

### 7.6 기본 사용 (레거시)

```kotlin
suspend fun legacyUsage() {
    val ufc = UFCClient.create(
        config = UFCClientConfig(
            fredApiKey = "your_api_key"
        )
    )

    // 범용 getSeries 사용
    val gdp = ufc.fred.getSeries(
        seriesId = "GDPC1",
        observationStart = LocalDate.of(2020, 1, 1),
        observationEnd = LocalDate.of(2024, 1, 1)
    )

    gdp.observations.forEach { obs ->
        println("${obs.date}: ${obs.value}")
    }

    // 검색
    val unemployment = ufc.fred.search("unemployment rate")
    unemployment.forEach {
        println("${it.id}: ${it.title}")
    }
}
```

### 7.7 Vintage Data 활용

```kotlin
suspend fun analyzeVintageData() {
    val ufc = UFCClient.create(config = UFCClientConfig(fredApiKey = "your_api_key"))

    // 첫 발표 데이터만
    val firstRelease = ufc.fred.getSeriesFirstRelease("GDP")
    println("First Release GDP Data:")
    firstRelease.observations.forEach {
        println("${it.date}: ${it.value}")
    }

    // 모든 개정 이력
    val allReleases = ufc.fred.getSeriesAllReleases("GDP")
    allReleases.forEach {
        println("${it.date}: ${it.value} (released on ${it.realtimeStart})")
    }

    // 특정 시점 데이터 (2014-06-01 기준)
    val asOfJune2014 = ufc.fred.getSeriesAsOfDate(
        seriesId = "GDP",
        asOfDate = LocalDate.of(2014, 6, 1)
    )
    println("GDP as of June 2014:")
    asOfJune2014.observations.forEach {
        println("${it.date}: ${it.value}")
    }
}
```

### 7.8 경제 지표 Enum 활용 (레거시)

```kotlin
suspend fun useEconomicIndicatorEnum() {
    val ufc = UFCClient.create(config = UFCClientConfig(fredApiKey = "your_api_key"))

    // Enum 활용
    val gdp = EconomicIndicator.REAL_GDP.fetch(ufc.fred)
    val unemployment = EconomicIndicator.UNEMPLOYMENT_RATE.fetch(ufc.fred)
    val cpi = EconomicIndicator.CPI.fetch(ufc.fred)

    // 데이터 출력
    println("GDP: ${gdp.observations.size} observations")
    println("Unemployment: ${unemployment.observations.size} observations")
    println("CPI: ${cpi.observations.size} observations")
}
```

---

## 8. 참고 자료

- **FRED API Documentation**: https://fred.stlouisfed.org/docs/api/fred/
- **Python fredapi**: https://github.com/mortada/fredapi
- **ALFRED (Vintage Data)**: https://alfred.stlouisfed.org/

---

**다음 문서**: [07-advanced-topics.md](./07-advanced-topics.md)
