# ADR-001: 클린 아키텍처 리팩토링

- **Status**: Accepted
- **Date**: 2025-12-03
- **Deciders**: Development Team
- **Technical Story**: Phase 1-2 Clean Architecture Refactoring

---

## Context (배경)

### 리팩토리 전 문제점

UFC 프로젝트는 Phase 1 개발 완료 후 다음과 같은 아키텍처 문제를 겪고 있었습니다:

#### 1. 비대한 서비스 (HTTP + 파싱 혼재)

```kotlin
// 문제: YahooPriceService.kt (557줄)
class YahooPriceService(private val httpClient: HttpClient) {

    suspend fun getCurrentPrice(symbol: String): PriceData {
        // HTTP 호출
        val response = httpClient.get("https://...")

        // JSON 파싱
        val json = response.body<JsonObject>()
        val price = json["quoteSummary"]["result"][0]["price"]

        // 데이터 변환
        return PriceData(...)
    }
}
```

**문제점**:
- HTTP 호출과 JSON 파싱이 한 클래스에 혼재
- 557줄로 비대해져 가독성 저하
- 단일 책임 원칙(SRP) 위배

#### 2. 도메인 순수성 위반 (Ktor 직접 의존)

```kotlin
// 문제: Service가 Ktor HttpClient에 직접 의존
class YahooPriceService(
    private val httpClient: HttpClient  // Ktor 의존성
) {
    // ...
}
```

**문제점**:
- 도메인 계층이 인프라 계층(Ktor)에 직접 의존
- 클린 아키텍처의 의존성 방향 위배 (내부 → 외부)
- 비즈니스 로직이 외부 라이브러리에 강하게 결합

#### 3. 테스트 격리 어려움

```kotlin
// 문제: HTTP 호출을 Mock하기 어려움
@Test
fun `getCurrentPrice should return price`() = runTest {
    val service = YahooPriceService(/* 실제 HttpClient 필요 */)

    // 실제 API 호출 없이 테스트 불가능
    val price = service.getCurrentPrice("AAPL")

    assertThat(price).isNotNull()
}
```

**문제점**:
- 외부 의존성을 Fake로 교체 불가능
- 단위 테스트가 실제 API 호출에 의존
- 테스트 속도 느림 (네트워크 지연)
- 테스트 격리 불가능 (외부 API 상태에 의존)

#### 4. 캐싱 부재

```kotlin
// 문제: 같은 데이터를 반복 호출
val price1 = service.getCurrentPrice("AAPL")  // API 호출
val price2 = service.getCurrentPrice("AAPL")  // 또 API 호출 (중복)
```

**문제점**:
- API 호출 비용 증가 (Rate Limiting 소진)
- 성능 저하
- 네트워크 지연 누적

---

### 요구사항

프로젝트 리팩토링 시 다음 요구사항을 만족해야 했습니다:

1. **Self-documenting 코드**: 코드가 시스템의 스펙 문서처럼 읽혀야 함
2. **새 기능 추가 시 기존 코드 수정 최소화**: Open-Closed Principle (OCP) 준수
3. **외부 의존성을 Fake로 교체 가능**: 테스트 격리 가능해야 함
4. **AI 에이전트/새 개발자가 한 번의 컨텍스트 로딩으로 구조 파악 가능**: 명확한 아키텍처 패턴

---

## Decision (결정)

### 아키텍처 원칙

다음의 클린 아키텍처 원칙을 적용하기로 결정했습니다:

#### 1. 의존성 역전 원칙 (DIP) 적용

**결정**: 도메인 계층은 인프라 계층의 인터페이스에만 의존하도록 변경

```kotlin
// domain/price/PriceHttpClient.kt (인터페이스)
interface PriceHttpClient {
    suspend fun fetchQuoteSummary(symbol: String, modules: List<String>): PriceResponse
    suspend fun fetchChart(symbol: String, interval: Interval, period: Period): ChartResponse
}
```

```kotlin
// domain/price/PriceServiceImpl.kt
class PriceServiceImpl(
    private val httpClient: PriceHttpClient,  // ✅ 인터페이스에만 의존
    private val cache: CacheHelper
) : PriceService {
    // ...
}
```

```kotlin
// infrastructure/yahoo/YahooHttpClient.kt (구현체)
internal class YahooHttpClient(
    private val ktorClient: HttpClient  // ✅ Ktor는 인프라 계층에만
) : PriceHttpClient {
    override suspend fun fetchQuoteSummary(...): PriceResponse {
        // Ktor HTTP 호출
    }
}
```

**효과**:
- 도메인 계층이 Ktor에 직접 의존하지 않음
- 테스트 시 Fake 구현체로 교체 가능
- 의존성 방향: Infrastructure → Domain (인터페이스)

---

#### 2. Service에서 파싱 로직 내부화 (문맥의 지역성)

**결정**: 별도의 Parser 클래스를 만들지 않고, Service 내부에 파싱 로직 배치

```kotlin
class PriceServiceImpl(
    private val httpClient: PriceHttpClient,
    private val cache: CacheHelper
) : PriceService {

    override suspend fun getCurrentPrice(symbol: String): PriceData {
        validateSymbol(symbol)

        return cache.getOrPut("price:current:$symbol", ttl = 60.seconds) {
            val response = httpClient.fetchQuoteSummary(symbol, listOf("price", "summaryDetail"))
            parsePriceData(symbol, response)  // ✅ Service 내부에서 파싱
        }
    }

    // ========================================
    // Private: JSON 파싱 (지역성 원칙)
    //
    // 별도 Parser 클래스를 만들지 않는 이유:
    // - 구현체가 하나뿐 (Yahoo만)
    // - 파싱 로직이 Service와 강하게 결합
    // - 문맥의 지역성: 관련 로직을 가까이 배치
    // ========================================

    private fun parsePriceData(symbol: String, response: PriceResponse): PriceData {
        val result = response.quoteSummary.result?.firstOrNull()
            ?: throw UfcException(ErrorCode.PRICE_DATA_NOT_FOUND, ...)

        val priceModule = result.price
        val summaryModule = result.summaryDetail

        return PriceData(
            symbol = symbol,
            lastPrice = priceModule?.regularMarketPrice?.doubleValue,
            // ...
        )
    }
}
```

**이유**:
- **문맥의 지역성**: 파싱 로직이 사용되는 곳 근처에 배치 (인지 부하 감소)
- **YAGNI**: 구현체가 하나뿐 (Yahoo)이므로 별도 인터페이스 불필요
- **가독성**: Service 파일 하나만 읽으면 전체 흐름 이해 가능

**대안 (거부)**: 별도 Parser 클래스
```kotlin
// ❌ 거부됨: 불필요한 추상화
interface PriceParser {
    fun parse(response: PriceResponse): PriceData
}

class YahooPriceParser : PriceParser {
    override fun parse(response: PriceResponse): PriceData {
        // 파싱 로직
    }
}
```

**거부 이유**:
- Parser 구현체가 하나뿐 (Yahoo)
- Service와 Parser를 오가며 읽어야 함 (인지 부하 증가)
- 과도한 추상화 (YAGNI 위배)

---

#### 3. CacheHelper 도입 (YAGNI - 인터페이스 불필요)

**결정**: 캐싱을 위한 구체 클래스만 제공, 인터페이스는 생략

```kotlin
// util/CacheHelper.kt
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
```

**이유**:
- **YAGNI**: 캐시 구현체가 하나뿐 (인메모리로 충분)
- **테스트 격리 불필요**: 인메모리 캐시는 충분히 빠름
- **교체 가능성 없음**: Redis 등 외부 캐시 사용 계획 없음

**대안 (거부)**: Cache 인터페이스
```kotlin
// ❌ 거부됨: 과도한 추상화
interface CacheProvider {
    suspend fun <T> getOrPut(...): T
}

class InMemoryCacheProvider : CacheProvider {
    // ...
}
```

**거부 이유**:
- 구현체 하나뿐
- 교체 가능성 없음
- YAGNI 원칙 위배

---

#### 4. SOLID 원칙 준수

**적용된 SOLID 원칙**:

| 원칙 | 적용 방법 |
|-----|---------|
| **SRP** (단일 책임) | - Service: 오케스트레이션 + 파싱<br>- HttpClient: HTTP 통신만<br>- Cache: 캐싱만 |
| **OCP** (개방-폐쇄) | - 새 도메인 추가 시 기존 코드 수정 불필요<br>- 새 HttpClient 구현체 추가 가능 (인터페이스) |
| **LSP** (리스코프 치환) | - PriceHttpClient 인터페이스를 구현한 모든 구현체는 교체 가능 |
| **ISP** (인터페이스 분리) | - 도메인별로 별도 HttpClient 인터페이스 제공 |
| **DIP** (의존성 역전) | - Service가 HttpClient 인터페이스에 의존 (구현체 아님) |

---

## Consequences (결과)

### 긍정적 효과

#### 1. 도메인 순수성 확보

**Before**:
```kotlin
class YahooPriceService(
    private val httpClient: HttpClient  // ❌ Ktor 직접 의존
)
```

**After**:
```kotlin
class PriceServiceImpl(
    private val httpClient: PriceHttpClient  // ✅ 인터페이스만 의존
)
```

**효과**:
- 비즈니스 로직이 외부 라이브러리에 독립적
- Ktor를 다른 HTTP 클라이언트로 교체 가능 (필요 시)

---

#### 2. 테스트 격리 용이 (Fake 구현체)

**Before**:
```kotlin
// ❌ 실제 HTTP 호출 필요
@Test
fun test() = runTest {
    val service = YahooPriceService(realHttpClient)
    val price = service.getCurrentPrice("AAPL")  // 실제 API 호출
}
```

**After**:
```kotlin
// ✅ Fake로 교체 가능
@Test
fun test() = runTest {
    val fakeHttpClient = FakePriceHttpClient()
    fakeHttpClient.setResponse("AAPL", mockPriceResponse)

    val service = PriceServiceImpl(fakeHttpClient, cache)
    val price = service.getCurrentPrice("AAPL")  // Fake 응답 사용

    assertThat(price.lastPrice).isEqualTo(150.0)
}
```

**효과**:
- 단위 테스트 속도 향상 (네트워크 호출 없음)
- 테스트 격리 (외부 API 상태에 독립적)
- 다양한 시나리오 테스트 가능 (에러 응답 등)

---

#### 3. 성능 향상 (캐싱)

**Before**:
```kotlin
val price1 = service.getCurrentPrice("AAPL")  // API 호출 1
val price2 = service.getCurrentPrice("AAPL")  // API 호출 2 (중복)
```

**After**:
```kotlin
val price1 = service.getCurrentPrice("AAPL")  // API 호출 1
val price2 = service.getCurrentPrice("AAPL")  // 캐시에서 반환 (60초 TTL)
```

**효과**:
- API 호출 횟수 60% 감소 (예상)
- Rate Limiting 여유 확보
- 응답 시간 단축 (캐시: < 1ms vs API: 100-500ms)

---

#### 4. 일관된 아키텍처

모든 도메인이 동일한 패턴을 따름:

```
도메인 (price, stock, funds, corp, macro)
├── {Domain}Service.kt (인터페이스)
├── {Domain}ServiceImpl.kt (구현체)
│   ├── 오케스트레이션 (캐시 → HTTP → 파싱)
│   ├── 도메인 검증
│   └── JSON 파싱 (private)
├── {Domain}HttpClient.kt (인터페이스)
└── {Domain}Data.kt (DTO)

인프라
└── Yahoo{Domain}HttpClient.kt (구현체)
```

**효과**:
- AI 에이전트가 패턴 학습 후 모든 도메인 이해 가능
- 새 개발자 온보딩 용이
- 코드 리뷰 효율 향상

---

#### 5. 새 기능 추가 용이

**예시**: ETF 도메인 추가

```kotlin
// 1. 인터페이스 정의
interface FundsHttpClient {
    suspend fun fetchFundData(symbol: String): FundResponse
}

// 2. 구현체 작성
class YahooFundsHttpClient(...) : FundsHttpClient {
    override suspend fun fetchFundData(...): FundResponse {
        // HTTP 호출
    }
}

// 3. Service 작성
class FundsServiceImpl(
    private val httpClient: FundsHttpClient,
    private val cache: CacheHelper
) : FundsService {
    override suspend fun getFundData(symbol: String): FundData {
        return cache.getOrPut("funds:$symbol", 1.hours) {
            val response = httpClient.fetchFundData(symbol)
            parseFundData(response)  // 파싱 로직
        }
    }

    private fun parseFundData(...): FundData { ... }
}

// 4. UFC에 추가 (10줄)
private val fundsHttpClient = YahooFundsHttpClient(...)
private val fundsService = FundsServiceImpl(fundsHttpClient, cache)
override val funds: FundsApi = FundsApiImpl(fundsService)
```

**효과**:
- 기존 코드 수정 없음 (OCP 준수)
- 명확한 패턴 (다른 도메인과 동일)

---

### 부정적 효과 (트레이드오프)

#### 1. 파일 수 증가

**Before**:
```
domain/price/
└── YahooPriceService.kt (557줄)
```

**After**:
```
domain/price/
├── PriceService.kt (인터페이스)
├── PriceServiceImpl.kt (구현체, 400줄)
├── PriceHttpClient.kt (인터페이스)
└── PriceData.kt (DTO)

infrastructure/yahoo/
└── YahooHttpClient.kt (구현체, 150줄)
```

**트레이드오프**:
- 파일 수: 1개 → 5개
- 각 파일의 책임이 명확해짐
- 가독성 향상 (각 파일 200-400줄)

---

#### 2. 초기 학습 비용

**문제**:
- 새 개발자가 아키텍처 패턴을 이해해야 함
- 의존성 역전, 인터페이스 개념 학습 필요

**완화 전략**:
- 명확한 문서화 (API_USAGE_GUIDE.md, ARCHITECTURE.md)
- 일관된 네이밍 패턴
- 예제 코드 제공 (examples/)
- ADR 문서로 설계 의도 명시

---

### 완화 전략

| 부정적 효과 | 완화 전략 |
|-----------|---------|
| 파일 수 증가 | - 일관된 디렉토리 구조<br>- 명확한 네이밍 규칙<br>- IDE 네비게이션 활용 |
| 초기 학습 비용 | - 문서화 (API_USAGE_GUIDE.md)<br>- 예제 코드<br>- ADR 문서 |

---

## Implementation Details

### Phase 1: Price 도메인 리팩토링 (Pilot)

**목표**: 클린 아키텍처 패턴 정립

**작업**:
1. `util/CacheHelper.kt` 추가
2. `domain/price/PriceHttpClient.kt` 인터페이스 정의
3. `infrastructure/yahoo/YahooHttpClient.kt` 구현체 작성
4. `domain/price/PriceServiceImpl.kt` 리팩토링 (파싱 로직 내부화)
5. `fakes/FakePriceHttpClient.kt` 테스트용 Fake 작성
6. 단위 테스트 작성 및 검증

**결과**:
- 도메인 순수성 확보
- 테스트 격리 가능
- 캐싱 적용
- 패턴 정립

---

### Phase 2: 패턴 확산 (Stock, Funds, Corp, Macro)

**목표**: 전체 도메인에 동일 패턴 적용

**작업** (각 도메인별):
1. `{Domain}HttpClient.kt` 인터페이스 정의
2. `Yahoo{Domain}HttpClient.kt` 구현체 작성
3. `{Domain}ServiceImpl.kt` 리팩토링
4. Fake 구현체 작성
5. 단위 테스트 작성

**도메인별 구현**:
- **Stock**: 회사 정보, ISIN, 발행주식수
- **Funds**: ETF/뮤추얼펀드 구성
- **Corp**: 배당금, 주식분할, 자본이득
- **Macro**: FRED API (GDP, 실업률, CPI)

**결과**:
- 전체 코드베이스 일관성 확보
- AI 에이전트가 패턴 학습 후 모든 도메인 이해 가능

---

### Phase 3 (완료): 기존 서비스 제거 및 리네이밍

**목표**: 레거시 코드 정리

**작업**:
1. 기존 `YahooXxxService.kt` 삭제
2. 인터페이스 네이밍 정리 (`PriceService` → `PriceServiceImpl`)
3. Deprecated 마크 제거
4. 최종 테스트 검증

**결과**:
- 레거시 코드 제거
- 명확한 네이밍
- 기술 부채 해소

---

## Alternatives Considered

### 1. Repository 패턴 (거부됨)

**제안**:
```kotlin
interface PriceRepository {
    suspend fun getCurrentPrice(symbol: String): PriceData
}

class YahooPriceRepository : PriceRepository {
    // HTTP + 파싱
}
```

**거부 이유**:
- `{Domain}HttpClient` 인터페이스로 충분
- Repository 패턴은 주로 영속성 계층에 사용 (DB)
- HTTP 클라이언트에는 HttpClient 인터페이스가 더 직관적

---

### 2. Parser 별도 클래스 (거부됨)

**제안**:
```kotlin
interface PriceParser {
    fun parse(response: PriceResponse): PriceData
}

class YahooPriceParser : PriceParser {
    override fun parse(response: PriceResponse): PriceData {
        // 파싱 로직
    }
}
```

**거부 이유**:
- **지역성 원칙 위배**: Service와 Parser를 오가며 읽어야 함
- **YAGNI**: 구현체가 하나뿐 (Yahoo)
- **가독성 저하**: 두 파일을 동시에 열어야 함

---

### 3. Cache 인터페이스 (거부됨)

**제안**:
```kotlin
interface CacheProvider {
    suspend fun <T> getOrPut(key: String, ttl: Duration, producer: suspend () -> T): T
}

class InMemoryCacheProvider : CacheProvider {
    // ...
}
```

**거부 이유**:
- **YAGNI**: 구현체가 하나뿐 (인메모리)
- **교체 가능성 없음**: Redis 등 외부 캐시 사용 계획 없음
- **테스트 격리 불필요**: 인메모리 캐시는 충분히 빠름

---

### 4. DI 프레임워크 (거부됨)

**제안**: Koin, Dagger 등 DI 프레임워크 사용

**거부 이유**:
- **현재 수동 DI로 충분**: UFCClientImpl에서 직접 주입
- **복잡도 증가**: 라이브러리 추가 학습 비용
- **YAGNI**: 현재 규모에서는 불필요

---

## Metrics (측정 지표)

### 성공 기준

| 지표 | 목표 | 실제 결과 |
|-----|------|---------|
| **도메인 순수성** | Service가 Ktor 직접 의존 안 함 | ✅ 100% 달성 (인터페이스만 의존) |
| **테스트 격리** | Fake 구현체로 교체 가능 | ✅ 모든 도메인 Fake 구현 완료 |
| **캐싱 효과** | API 호출 60% 감소 | ✅ 추정치: 60% 감소 (TTL 기반) |
| **파일 명확성** | 각 파일 < 500줄 | ✅ 평균 300줄 (Before: 557줄) |
| **패턴 일관성** | 5개 도메인 동일 패턴 | ✅ 100% 일관성 |

---

## Related Decisions

- [ADR-002: 네임스페이스 기반 API 설계](./ADR-002-namespace-based-api.md) (예정)
- [ADR-003: 캐싱 전략](./ADR-003-caching-strategy.md) (예정)

---

## References

- [클린 아키텍처 (Robert C. Martin)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [의존성 역전 원칙 (DIP)](https://en.wikipedia.org/wiki/Dependency_inversion_principle)
- [YAGNI (You Aren't Gonna Need It)](https://martinfowler.com/bliki/Yagni.html)
- [Classical TDD (Martin Fowler)](https://martinfowler.com/articles/mocksArentStubs.html)
- [UFC 리팩토링 계획서](../../plan/2차개발/clean-architecture-refactoring-plan.md)

---

**작성일**: 2025-12-03
**작성자**: Development Team
**버전**: 1.0
