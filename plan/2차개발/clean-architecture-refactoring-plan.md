# UFC 프로젝트 아키텍처 개선 플랜

## 📌 설계 목표

**사용자 요구사항**:
- ✅ 코드가 시스템의 스펙 문서처럼 읽혀야 한다 (Self-documenting)
- ✅ 새로운 기능 추가 시 기존 코드 수정 최소화
- ✅ 외부 의존성을 Fake로 교체해서 단위 테스트 가능
- ✅ AI 에이전트나 새 개발자가 한 번의 컨텍스트 로딩으로 구조 파악 가능

**설계 원칙**:
1. ✅ **단방향 의존성**: 외부 → 내부, 구체 → 추상
2. ✅ **의존성 주입**: 외부 의존성(HTTP, DB)은 인터페이스를 통해 주입
3. ✅ **도메인 순수성**: 비즈니스 로직은 외부 라이브러리 직접 import 금지
4. ✅ **문맥의 지역성**: 관련 로직은 물리적으로 가깝게 배치 (인지 부하 감소)
5. ✅ **YAGNI**: 교체 가능성 없으면 인터페이스 만들지 않음

**현재 문제점**:
- 캐싱 없음 (같은 데이터 반복 API 호출)
- 비대한 서비스 (YahooPriceService 557줄 - HTTP + Parsing 혼재)
- 도메인 순수성 위반 (Service가 Ktor HttpClient 직접 의존)
- 테스트 격리 어려움 (HTTP 호출을 Mock/Fake로 교체 불가)

---

## 🏗️ 목표 아키텍처

```
┌─────────────────────────────────────┐
│  Presentation (Client/API)          │
│  - UFC (Facade)                     │
│  - ufc.price.xxx()                  │
└─────────────────────────────────────┘
               ↓
┌─────────────────────────────────────┐
│  Domain (Service + Interface)       │
│  - PriceService (orchestration)     │
│  - PriceHttpClient (interface) ★    │
│  - PriceData (DTO)                  │
└─────────────────────────────────────┘
               ↑ implements
┌─────────────────────────────────────┐
│  Infrastructure (Adapter)           │
│  - YahooHttpClient (implements)     │
│  - CacheHelper (utility)            │
└─────────────────────────────────────┘
```

**핵심 원칙**:
- ✅ **의존성 역전**: Domain은 Infrastructure 인터페이스만 의존
- ✅ **테스트 격리**: HTTP 호출을 인터페이스로 추상화 (Fake 교체 가능)
- ✅ **문맥의 지역성**: 파싱 로직은 Service 내부에 (별도 Parser 클래스 불필요)
- ✅ **YAGNI**: Cache는 인터페이스 불필요 (구현체 하나, 인메모리로 충분히 빠름)

---

## 📅 리팩토링 단계

### Phase 1: 도메인 순수성 확보 + 캐싱 (3-5일, 위험도: ★★☆☆☆)

**목표**: 의존성 역전 + 테스트 격리 + 성능 향상

#### 1.1 CacheHelper 추가 (인터페이스 불필요)

**신규 파일**: `util/CacheHelper.kt`
```kotlin
/**
 * 인메모리 캐싱 유틸리티
 *
 * 인터페이스 불필요한 이유:
 * - 구현체 하나뿐 (교체 가능성 없음)
 * - 인메모리로 충분히 빠름 (테스트 격리 불필요)
 * - YAGNI 원칙 준수
 */
class CacheHelper {
    private val cache = ConcurrentHashMap<String, CachedValue<Any>>()

    suspend fun <T> getOrPut(
        key: String,
        ttl: Duration,
        producer: suspend () -> T
    ): T {
        cache[key]?.let { cached ->
            if (!cached.isExpired()) return cached.value as T
        }

        val value = producer()
        cache[key] = CachedValue(value, Clock.System.now() + ttl)
        return value
    }

    fun clear() {
        cache.clear()
    }
}

private data class CachedValue<T>(val value: T, val expiresAt: Instant) {
    fun isExpired() = Clock.System.now() > expiresAt
}
```

#### 1.2 PriceHttpClient 인터페이스 생성 (테스트 격리 위해 필요)

**신규 파일**: `domain/price/PriceHttpClient.kt`
```kotlin
/**
 * Price 데이터 HTTP 통신 인터페이스
 *
 * 인터페이스가 필요한 이유:
 * - 외부 의존성(HTTP 호출)을 테스트에서 Fake로 교체하기 위함
 * - 도메인 순수성 유지 (PriceService가 Ktor에 직접 의존 방지)
 *
 * 교체 가능성은 없지만, 테스트 격리를 위해 추상화 필요
 */
interface PriceHttpClient {
    /**
     * Yahoo Finance Quote Summary API 호출
     * @return 원본 JSON 객체
     */
    suspend fun fetchQuoteSummary(symbol: String): JsonObject

    /**
     * Yahoo Finance Chart API 호출
     * @return 원본 JSON 객체
     */
    suspend fun fetchChart(
        symbol: String,
        period: Period,
        interval: Interval
    ): JsonObject

    /**
     * 날짜 범위로 차트 데이터 조회
     */
    suspend fun fetchChartByDateRange(
        symbol: String,
        start: LocalDate,
        end: LocalDate,
        interval: Interval
    ): JsonObject
}
```

#### 1.3 YahooHttpClient 구현 (Infrastructure Layer)

**신규 파일**: `infrastructure/yahoo/YahooHttpClient.kt`
```kotlin
/**
 * Yahoo Finance HTTP 클라이언트 구현체
 *
 * 책임: HTTP 요청/응답 처리만 (JSON 파싱은 Service에서)
 */
internal class YahooHttpClient(
    private val httpClient: HttpClient,
    private val rateLimiter: RateLimiter
) : PriceHttpClient {

    override suspend fun fetchQuoteSummary(symbol: String): JsonObject {
        rateLimiter.acquire()

        val response: HttpResponse = httpClient.get {
            url("https://query2.finance.yahoo.com/v10/finance/quoteSummary/$symbol")
            parameter("modules", "price,summaryDetail")
        }

        if (!response.status.isSuccess()) {
            throw HttpException(response.status, "Failed to fetch quote summary for $symbol")
        }

        return response.body()
    }

    override suspend fun fetchChart(
        symbol: String,
        period: Period,
        interval: Interval
    ): JsonObject {
        rateLimiter.acquire()

        val response: HttpResponse = httpClient.get {
            url("https://query2.finance.yahoo.com/v8/finance/chart/$symbol")
            parameter("range", period.toYahooFormat())
            parameter("interval", interval.toYahooFormat())
        }

        if (!response.status.isSuccess()) {
            throw HttpException(response.status, "Failed to fetch chart for $symbol")
        }

        return response.body()
    }

    // ... fetchChartByDateRange 구현
}
```

#### 1.4 PriceService 리팩토링 (문맥의 지역성)

**수정 파일**: `domain/price/PriceService.kt`
```kotlin
/**
 * Price 도메인 서비스
 *
 * 책임:
 * - 오케스트레이션 (캐시 → HTTP → 파싱)
 * - 도메인 검증
 * - JSON 파싱 (지역성 원칙: 관련 로직을 가까이 배치)
 *
 * 의존성:
 * - PriceHttpClient (인터페이스): 테스트 격리 가능
 * - CacheHelper (구체 클래스): 인메모리로 충분히 빠름
 */
class PriceService(
    private val httpClient: PriceHttpClient,  // ✅ 인터페이스에 의존 (도메인 순수성)
    private val cache: CacheHelper            // ✅ 구체 클래스 (YAGNI)
) {
    suspend fun getCurrentPrice(symbol: String): PriceData {
        validateSymbol(symbol)

        return cache.getOrPut("price:current:$symbol", ttl = 60.seconds) {
            val json = httpClient.fetchQuoteSummary(symbol)
            parsePriceData(symbol, json)  // ✅ Service 내부에서 파싱 (지역성)
        }
    }

    suspend fun getCurrentPrice(symbols: List<String>): Map<String, PriceData> {
        require(symbols.isNotEmpty()) { "Symbol list cannot be empty" }
        require(symbols.size <= 100) { "Maximum 100 symbols per request" }

        // 병렬 처리
        return coroutineScope {
            symbols.map { symbol ->
                async { symbol to getCurrentPrice(symbol) }
            }.awaitAll().toMap()
        }
    }

    suspend fun getPriceHistory(
        symbol: String,
        period: Period,
        interval: Interval
    ): List<OHLCV> {
        validateSymbol(symbol)

        return cache.getOrPut("price:history:$symbol:$period:$interval", ttl = 5.minutes) {
            val json = httpClient.fetchChart(symbol, period, interval)
            parseChartData(json)  // ✅ Service 내부에서 파싱 (지역성)
        }
    }

    suspend fun getPriceHistoryWithMetadata(
        symbol: String,
        period: Period,
        interval: Interval
    ): Pair<ChartMetadata, List<OHLCV>> {
        validateSymbol(symbol)

        return cache.getOrPut("price:history_meta:$symbol:$period:$interval", ttl = 5.minutes) {
            val json = httpClient.fetchChart(symbol, period, interval)
            val metadata = parseChartMetadata(json)
            val ohlcv = parseChartData(json)
            metadata to ohlcv
        }
    }

    // ========================================
    // Private: 도메인 검증
    // ========================================

    private fun validateSymbol(symbol: String) {
        require(symbol.isNotBlank()) { "Symbol cannot be blank" }
        require(symbol.length <= 20) { "Symbol too long: $symbol" }
    }

    // ========================================
    // Private: JSON 파싱 (지역성 원칙)
    //
    // 별도 Parser 클래스를 만들지 않는 이유:
    // - 구현체가 하나뿐 (Yahoo만)
    // - 파싱 로직이 Service와 강하게 결합
    // - 문맥의 지역성: 관련 로직을 가까이 배치
    // ========================================

    private fun parsePriceData(symbol: String, json: JsonObject): PriceData {
        try {
            val result = json["quoteSummary"]["result"][0]
            val price = result["price"]
            val summaryDetail = result["summaryDetail"]

            return PriceData(
                symbol = symbol,
                lastPrice = price["regularMarketPrice"]["raw"].asDouble,
                previousClose = price["regularMarketPreviousClose"]?.get("raw")?.asDoubleOrNull(),
                open = price["regularMarketOpen"]?.get("raw")?.asDoubleOrNull(),
                dayHigh = price["regularMarketDayHigh"]?.get("raw")?.asDoubleOrNull(),
                dayLow = price["regularMarketDayLow"]?.get("raw")?.asDoubleOrNull(),
                volume = price["regularMarketVolume"]?.get("raw")?.asLongOrNull(),
                marketCap = price["marketCap"]?.get("raw")?.asLongOrNull(),
                averageVolume = summaryDetail["averageVolume"]?.get("raw")?.asLongOrNull(),
                // ... 나머지 필드
            )
        } catch (e: Exception) {
            // 에이전트가 원인을 추론할 수 있도록 충분한 문맥 제공
            throw PriceParsingException(
                symbol = symbol,
                message = "Failed to parse price data for $symbol",
                jsonSnippet = json.toString().take(200),
                cause = e
            )
        }
    }

    private fun parseChartData(json: JsonObject): List<OHLCV> {
        try {
            val chart = json["chart"]["result"][0]
            val timestamps = chart["timestamp"].jsonArray.map { it.asLong }
            val indicators = chart["indicators"]["quote"][0]

            val open = indicators["open"]?.jsonArray?.map { it.asDoubleOrNull() } ?: emptyList()
            val high = indicators["high"]?.jsonArray?.map { it.asDoubleOrNull() } ?: emptyList()
            val low = indicators["low"]?.jsonArray?.map { it.asDoubleOrNull() } ?: emptyList()
            val close = indicators["close"]?.jsonArray?.map { it.asDoubleOrNull() } ?: emptyList()
            val volume = indicators["volume"]?.jsonArray?.map { it.asLongOrNull() } ?: emptyList()

            return timestamps.indices.mapNotNull { i ->
                if (close.getOrNull(i) == null) return@mapNotNull null

                OHLCV(
                    timestamp = Instant.fromEpochSeconds(timestamps[i]),
                    open = open.getOrNull(i),
                    high = high.getOrNull(i),
                    low = low.getOrNull(i),
                    close = close[i]!!,
                    volume = volume.getOrNull(i)
                )
            }
        } catch (e: Exception) {
            throw ChartParsingException(
                message = "Failed to parse chart data",
                jsonSnippet = json.toString().take(200),
                cause = e
            )
        }
    }

    private fun parseChartMetadata(json: JsonObject): ChartMetadata {
        // ... 메타데이터 파싱
    }
}
```

#### 1.5 UFCClientImpl 수정

**수정 파일**: `client/UFCClientImpl.kt`
```kotlin
internal class UFCClientImpl(config: UFCClientConfig) : UFCClient {

    // 공유 인프라
    private val httpClient = YahooHttpClientFactory.create(config)
    private val rateLimiter = TokenBucketRateLimiter("YAHOO", config.rateLimitConfig)
    private val cache = CacheHelper()

    // Price 도메인
    private val priceHttpClient: PriceHttpClient = YahooHttpClient(httpClient, rateLimiter)
    private val priceService = PriceService(priceHttpClient, cache)
    private val _price = PriceApiImpl(priceService)

    // 다른 도메인들도 동일 패턴...

    override val price: PriceApi = _price

    override fun close() {
        httpClient.close()
        cache.clear()
    }
}
```

**효과**:
- ✅ 도메인 순수성 확보 (PriceService가 Ktor에 직접 의존 안 함)
- ✅ 테스트 격리 가능 (PriceHttpClient를 Fake로 교체)
- ✅ 문맥의 지역성 (파싱 로직이 Service 내부에)
- ✅ YAGNI 준수 (Cache는 인터페이스 불필요)
- ✅ 캐싱 추가 (API 호출 60% 감소)

---

### Phase 2: 패턴 확산 (5-7일, 위험도: ★★☆☆☆)

**목표**: 전체 도메인에 동일 패턴 적용

#### 동일 패턴을 다른 도메인에 적용:

**Stock 도메인**:
- `domain/stock/StockHttpClient.kt` (interface)
- `infrastructure/yahoo/YahooStockHttpClient.kt` (implementation)
- `domain/stock/StockService.kt` (파싱 로직 내부에)

**Funds 도메인**:
- `domain/funds/FundsHttpClient.kt` (interface)
- `infrastructure/yahoo/YahooFundsHttpClient.kt` (implementation)
- `domain/funds/FundsService.kt` (파싱 로직 내부에)

**Corp 도메인**:
- `domain/corp/CorpHttpClient.kt` (interface)
- `infrastructure/yahoo/YahooCorpHttpClient.kt` (implementation)
- `domain/corp/CorpService.kt` (파싱 로직 내부에)

**Macro 도메인**:
- `domain/macro/MacroHttpClient.kt` (interface)
- `infrastructure/fred/FredMacroHttpClient.kt` (implementation)
- `domain/macro/MacroService.kt` (파싱 로직 내부에)

각 Service에서 CacheHelper 활용

**효과**:
- ✅ 전체 코드베이스 일관성
- ✅ AI 에이전트가 패턴을 학습하면 모든 도메인 이해 가능
- ✅ 새 개발자 온보딩 용이

---

### Phase 3 (선택적): ApiImpl 제거 (1-2일, 위험도: ★☆☆☆☆)

**배경**: PriceApiImpl은 순수 위임만 (72줄 boilerplate)

**사용자 요구사항**:
- ✅ 네임스페이스 패턴 유지: `ufc.price.getCurrentPrice()`

**Option 1**: ApiImpl 제거 + Service 직접 노출
```kotlin
interface UFCClient {
    val price: PriceService  // Service 직접 노출
    val stock: StockService
}

// 사용: ufc.price.getCurrentPrice() ✅
```

**Option 2**: Api 인터페이스 유지 (타입 안전성)
```kotlin
interface UFCClient {
    val price: PriceApi
    val stock: StockApi
}
```

**권장**: Phase 2 완료 후 사용자 피드백으로 결정

---

## 🎯 최종 추천 전략

```
Phase 1 (도메인 순수성 + 캐싱)
    ↓ 3-5일
Phase 2 (패턴 확산)
    ↓ 5-7일
(Phase 3은 선택적)

총 기간: 2주
```

**핵심 개선**:
- ✅ Phase 1: 도메인 순수성 확보 + 테스트 격리 + 성능 향상
- ✅ Phase 2: 일관성 확보 (전체 도메인 통일)

**아키텍처 원칙 준수**:
- ✅ **단방향 의존성**: Domain → Infrastructure (인터페이스)
- ✅ **의존성 주입**: PriceHttpClient 인터페이스로 주입
- ✅ **도메인 순수성**: PriceService는 Ktor 직접 의존 안 함
- ✅ **문맥의 지역성**: 파싱 로직이 Service 내부에
- ✅ **YAGNI**: Cache는 인터페이스 불필요

**스킵 항목**:
- ❌ Repository 패턴: HttpClient 인터페이스로 충분
- ❌ Parser 별도 클래스: 구현체 하나, 지역성 우선
- ❌ Cache 인터페이스: 구현체 하나, 테스트 격리 불필요
- ❌ DI 프레임워크: 현재 수동 DI로 충분

---

## 📂 Critical Files

### Phase 1 핵심 파일 (4개 신규, 2개 수정)

**신규**:
1. `util/CacheHelper.kt` - 캐싱 유틸리티 (인터페이스 없음)
2. `domain/price/PriceHttpClient.kt` - HTTP 인터페이스 ★
3. `infrastructure/yahoo/YahooHttpClient.kt` - Yahoo 구현체
4. `domain/price/PriceParsingException.kt` - 도메인 예외

**수정**:
1. `domain/price/PriceService.kt` - 리팩토링 (파싱 로직 내부에)
2. `client/UFCClientImpl.kt` - 새 클래스들 주입

**삭제** (deprecated 후):
- `domain/price/YahooPriceService.kt` (557줄 → PriceService + YahooHttpClient로 대체)

### Phase 2: 다른 도메인 동일 패턴

**신규** (각 도메인별):
- Stock: `StockHttpClient.kt` (interface), `YahooStockHttpClient.kt` (impl)
- Funds: `FundsHttpClient.kt` (interface), `YahooFundsHttpClient.kt` (impl)
- Corp: `CorpHttpClient.kt` (interface), `YahooCorpHttpClient.kt` (impl)
- Macro: `MacroHttpClient.kt` (interface), `FredMacroHttpClient.kt` (impl)

---

## 📊 예상 효과

| 측면 | Before | After | 개선 |
|------|--------|-------|------|
| **도메인 순수성** | Service가 Ktor 직접 의존 | 인터페이스만 의존 | ✅✅ |
| **테스트 격리** | HTTP Mock 어려움 | Fake 구현체로 간단 | ✅✅ |
| **파일 수** | YahooPriceService 1개 | Service + HttpClient + Impl 3개 | 명확성 ↑ |
| **코드 지역성** | Parser 별도 파일 | Service 내부에 | 인지 부하 ↓ |
| **캐싱** | 없음 | 전체 API 적용 | API 호출 60% ↓ |
| **가독성** | HTTP+Parsing 혼재 | 각 책임 명확 | ✅✅ |

---

## 🧪 테스트 전략

### 1. Unit Test (Service Layer)

```kotlin
class PriceServiceTest {
    private val fakeHttpClient = FakePriceHttpClient()  // ✅ 인터페이스로 Fake 주입
    private val cache = CacheHelper()
    private val service = PriceService(fakeHttpClient, cache)

    @Test
    fun `getCurrentPrice - 정상 응답`() = runTest {
        // Given
        fakeHttpClient.setResponse("AAPL", validJsonResponse)

        // When
        val result = service.getCurrentPrice("AAPL")

        // Then
        assertThat(result.symbol).isEqualTo("AAPL")
        assertThat(result.lastPrice).isEqualTo(150.0)
    }

    @Test
    fun `getCurrentPrice - 캐싱 동작 확인`() = runTest {
        // Given
        fakeHttpClient.setResponse("AAPL", validJsonResponse)

        // When
        service.getCurrentPrice("AAPL")  // 첫 호출
        service.getCurrentPrice("AAPL")  // 두 번째 호출 (캐시)

        // Then
        assertThat(fakeHttpClient.callCount).isEqualTo(1)  // ✅ HTTP 1회만 호출
    }
}
```

### 2. Fake Implementation (Test Double)

```kotlin
/**
 * 테스트용 PriceHttpClient Fake 구현체
 *
 * 인터페이스 덕분에 간단하게 구현 가능
 */
class FakePriceHttpClient : PriceHttpClient {
    private val responses = mutableMapOf<String, JsonObject>()
    var callCount = 0
        private set

    fun setResponse(symbol: String, json: JsonObject) {
        responses[symbol] = json
    }

    override suspend fun fetchQuoteSummary(symbol: String): JsonObject {
        callCount++
        return responses[symbol] ?: throw NoSuchElementException("No response for $symbol")
    }

    override suspend fun fetchChart(
        symbol: String,
        period: Period,
        interval: Interval
    ): JsonObject {
        callCount++
        return responses["$symbol:chart"] ?: throw NoSuchElementException()
    }
}
```

---

## 🔍 새 기능 추가 예시

**시나리오**: ETF 도메인 추가

### After (개선된 구조)
```kotlin
// 1. ETF HTTP 인터페이스 (domain/etf/ETFHttpClient.kt)
interface ETFHttpClient {
    suspend fun fetchETFInfo(symbol: String): JsonObject
}

// 2. Yahoo 구현체 (infrastructure/yahoo/YahooETFHttpClient.kt)
class YahooETFHttpClient(
    private val httpClient: HttpClient,
    private val rateLimiter: RateLimiter
) : ETFHttpClient {
    override suspend fun fetchETFInfo(symbol: String): JsonObject {
        // Yahoo API 호출
    }
}

// 3. ETF 서비스 (domain/etf/ETFService.kt)
class ETFService(
    private val httpClient: ETFHttpClient,  // ✅ 인터페이스에 의존
    private val cache: CacheHelper
) {
    suspend fun getETFInfo(symbol: String): ETFData =
        cache.getOrPut("etf:$symbol", 5.minutes) {
            val json = httpClient.fetchETFInfo(symbol)
            parseETFData(json)  // ✅ 파싱 로직 내부에 (지역성)
        }

    private fun parseETFData(json: JsonObject): ETFData {
        // 파싱 로직 (Service 내부에)
    }
}

// 4. UFCClientImpl 추가 (10 lines)
private val etfHttpClient: ETFHttpClient = YahooETFHttpClient(httpClient, rateLimiter)
private val etfService = ETFService(etfHttpClient, cache)
override val etf: ETFApi = ETFApiImpl(etfService)
```

**효과**:
- ✅ 명확한 패턴 (다른 도메인과 동일)
- ✅ 도메인 순수성 유지
- ✅ 테스트 격리 가능 (Fake 구현체)

---

## ✅ 검증 기준

각 Phase 완료 시:
1. ✅ 모든 기존 테스트 통과
2. ✅ 빌드 성공
3. ✅ 새로운 단위 테스트 작성 (Fake 구현체 활용)
4. ✅ 성능 테스트 (캐싱 효과 확인)
5. ✅ 도메인 순수성 확인 (Service가 외부 라이브러리 직접 import 안 함)

---

## 🚀 시작 방법

```bash
# 1. Phase 1 브랜치 생성
git checkout -b refactor/domain-purity

# 2. CacheHelper 작성 (TDD)
# util/CacheHelper.kt + test

# 3. PriceHttpClient 인터페이스 정의
# domain/price/PriceHttpClient.kt

# 4. YahooHttpClient 구현
# infrastructure/yahoo/YahooHttpClient.kt

# 5. PriceService 리팩토링 (파싱 로직 내부에)
# domain/price/PriceService.kt

# 6. Fake 구현체 작성 (테스트용)
# test/fakes/FakePriceHttpClient.kt

# 7. 단위 테스트 작성
# test/domain/price/PriceServiceTest.kt

# 8. 테스트 실행 및 검증
./gradlew test
```

---

## 📝 참고 사항

**설계 원칙**:
- ✅ **단방향 의존성**: 외부 → 내부, 구체 → 추상
- ✅ **의존성 주입**: HTTP 인터페이스로 주입 (테스트 격리)
- ✅ **도메인 순수성**: Service는 외부 라이브러리 직접 import 금지
- ✅ **문맥의 지역성**: 파싱 로직은 Service 내부에 (인지 부하 감소)
- ✅ **YAGNI**: 교체 가능성 없으면 인터페이스 만들지 않음

**인터페이스 판단 기준**:
- ✅ HTTP 호출 → **인터페이스 필요** (테스트 격리)
- ❌ Cache → 인터페이스 불필요 (인메모리로 충분히 빠름)
- ❌ Parser → 별도 클래스 불필요 (구현체 하나, 지역성 우선)

**트레이드오프**:
- 파일 수 증가 (1개 → 3개): 하지만 각 파일의 책임이 명확해짐
- 인터페이스 추가: 하지만 테스트 격리 가능 (Fake 구현체)
- Breaking changes 가능: 하위 호환성보다 구조 개선 우선

**구조 개선 우선**:
- 하위 호환성은 부차적 (필요 시 API 변경)
- 올바른 아키텍처 확립이 최우선
- 잘못된 구조를 유지하는 것보다 과감한 리팩토링

---

**최종 추천**: Phase 1 → Phase 2 순차 진행
**총 기간**: 2주
**위험도**: 낮음 (단계별 검증)
**예상 효과**: 도메인 순수성 ✅, 테스트 격리 ✅, 성능 ↑↑, 가독성 ↑↑
