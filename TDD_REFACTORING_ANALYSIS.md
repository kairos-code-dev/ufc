# UFC 라이브 테스트 리팩토링 계획
## 클래식 TDD (상태 기반 테스트) 원칙에 따른 종합 분석

작성일: 2025-12-02
대상 프로젝트: UFC (Universal Financial Client)

---

## 1. 현재 상태 분석

### 1.1 YahooChartServiceLiveTest 분석

#### 테스트 메서드 요약
| 메서드명 | 테스트 범위 | 특징 |
|---------|-----------|------|
| testBasicAuthStrategyCanObtainCrumb | 인증 | 실제 API 호출, 직접 실행 (runTest) |
| testCanFetchDailyChartDataForAapl | 차트 데이터 | 레코딩 사용, 여러 시간대 데이터 |
| testCanFetchIndexChartData | 인덱스 데이터 | 레코딩 사용, ^GSPC 조회 |
| testCanFetchHourlyChartData | 시간별 데이터 | 레코딩 사용, 정렬 검증 포함 |
| testCanFetchFiveMinuteChartData | 5분 간격 데이터 | 레코딩 사용 |
| testCanFetchGooglChartData | GOOGL 데이터 | 레코딩 사용 |
| testCanFetchTslaChartData | TSLA 데이터 | 레코딩 사용 |
| testCanFetchRawChartData | 원본 응답 | 레코딩 사용, 메타데이터 검증 |
| testCanFetchOneYearChartData | 1년 데이터 | 레코딩 사용, 거래일 수 검증 |
| testCanFetchMaxPeriodChartData | 최대 기간 데이터 | 레코딩 사용 |
| testRateLimiterWorksWithContinuousRequests | 레이트 리미팅 | 연속 요청, 직접 실행 |
| testErrorHandlingForInvalidSymbol | 에러 처리 | 음수 케이스, 예외 검증 |

**총 테스트 메서드 수: 12개**

#### 주요 의존성
- `YahooHttpClientFactory`: Ktor HttpClient 생성
- `BasicAuthStrategy`: Yahoo Finance CRUMB 토큰 획득
- `TokenBucketRateLimiter`: Rate Limiting 제어
- `YahooChartService`: 차트 데이터 조회 로직
- `HttpClient`: 네트워크 통신
- `ResponseRecordingContext`: 응답 자동 레코딩 (레코딩 활성화 시)

#### 현재 테스트 패턴
1. **라이브 테스트 방식**: `LiveTestBase`를 상속받아 `liveTestWithRecording()` 사용
2. **응답 레코딩**: 실제 API 응답을 JSON 파일로 저장
3. **테스트 격리 부재**: 테스트 간 상태 공유 (한 번의 httpClient 초기화로 모든 테스트 진행)
4. **외부 의존성 강한 결합**: 실제 Yahoo Finance API 호출 필수
5. **테스트 실행 시간**: 개별 요청이 느림 + Rate Limiting 대기 = 전체 실행 시간 길음

### 1.2 UFCClientLiveTest 분석

#### 테스트 메서드 요약
| 메서드명 | 테스트 범위 | 특징 |
|---------|-----------|------|
| testClientCreationAndClosure | 클라이언트 생명주기 | 생성/종료 검증 |
| testClientStatus | 클라이언트 상태 | getStatus() 호출 |
| testRateLimiterStatus | 레이트 리미팅 상태 | 토큰 상태 검증 |
| testCanFetchAaplDailyChartData | 차트 데이터 | 레코딩 사용 |
| testCanFetchHourlyChartData | 시간별 데이터 | 레코딩 사용 |
| testCanFetchMultipleSymbolsChartData | 다중 심볼 조회 | 레코딩 사용 |
| testCanFetchQuoteSummary | 요약 정보 | 레코딩 사용 |
| testCanFetchStockSummary | 주식 요약 | 레코딩 사용, 메타데이터 검증 |
| testCanFetchQuoteSummaryWithSpecificModules | 특정 모듈 조회 | 레코딩 사용 |
| testCanFetchMultipleStockSummaries | 다중 주식 요약 | 레코딩 사용 |
| testCanFetchIndexData | 인덱스 데이터 | 레코딩 사용 |
| testCanFetchTslaData | TSLA 데이터 | 레코딩 사용 |
| testCanFetchOneDayChartData | 1일 데이터 | 레코딩 사용 |
| testCanFetchOneYearChartData | 1년 데이터 | 레코딩 사용 |
| testCanFetchMaxPeriodChartData | 최대 기간 데이터 | 레코딩 사용 |
| testClientConfig | 클라이언트 설정 | 설정 검증 |
| testSequentialRequests | 순차 요청 | 여러 메서드 호출 |

**총 테스트 메서드 수: 17개**

#### 주요 의존성
- `UFCClient`: 통합 클라이언트 생성 및 관리
- `UFCClientImpl`: 실제 구현체
- `YahooChartService`: 차트 데이터 조회
- `YahooQuoteSummaryService`: 요약 정보 조회
- `HttpClient`: 네트워크 통신
- `RateLimiter`: Rate Limiting 제어
- `AuthStrategy`: Yahoo Finance 인증

#### 현재 테스트 패턴
1. **통합 테스트 방식**: UFCClient 전체 스택을 테스트
2. **레코딩 사용**: 실제 API 응답을 저장하여 재사용 가능
3. **클라이언트 생명주기 관리**: 각 테스트에서 독립적으로 클라이언트 생성/종료
4. **테스트 독립성**: 각 테스트가 자체 클라이언트 인스턴스 사용
5. **높은 실행 시간**: 네트워크 지연 + Rate Limiting으로 인한 대기

---

## 2. 필요한 Fake 객체 설계

### 2.1 아키텍처 계층별 Fake 객체 전략

클래식 TDD의 상태 기반 테스트 원칙에 따라, 다음 계층의 Fake 객체를 설계합니다:

```
Layer 1: HTTP 통신 계층 (하위 레벨)
  ├─ FakeHttpClient (HttpClient 모의)
  └─ RecordedResponseRepository (레코딩된 응답 관리)

Layer 2: 인증 계층 (중간 레벨)
  ├─ FakeAuthStrategy (AuthStrategy 모의)
  └─ StaticAuthResult (정적 인증 결과)

Layer 3: Rate Limiting 계층 (중간 레벨)
  ├─ FakeRateLimiter (RateLimiter 모의)
  └─ NoOpRateLimiter (No-Operation 버전)

Layer 4: 서비스 계층 (상위 레벨)
  ├─ FakeYahooChartService (YahooChartService 모의)
  └─ FakeYahooQuoteSummaryService (YahooQuoteSummaryService 모의)
```

### 2.2 상태 기반 테스트 설계 원칙

- **상태 검증**: 메서드 호출 후 객체의 상태 변화 검증
- **결정론적 동작**: 동일 입력 → 동일 출력 보장
- **고립성**: 외부 의존성 완전히 제거
- **테스트 속도**: 밀리초 단위의 빠른 실행

### 2.3 Fake 객체 세부 설계

#### 2.3.1 FakeAuthStrategy

**목적**: AuthStrategy 인터페이스 모의 구현
**상태**: 인증 성공/실패 상태, 호출 횟수 추적

```kotlin
// 구현할 인터페이스
interface AuthStrategy {
    suspend fun authenticate(): AuthResult
}

// Fake 구현의 책임
- authenticate() 호출 추적
- 미리 정의된 AuthResult 반환
- 설정 가능한 인증 실패 시나리오
- 호출 횟수 및 파라미터 기록
```

**상태 변수**:
- `callCount: Int`: authenticate() 호출 횟수
- `shouldFail: Boolean`: 인증 실패 여부
- `predefinedResult: AuthResult`: 반환할 사전정의된 결과
- `callHistory: List<Instant>`: 호출 시간 기록

**메서드**:
- `suspend fun authenticate(): AuthResult`
- `fun getCallCount(): Int`
- `fun resetCallHistory()`
- `fun setFailureMode(shouldFail: Boolean)`
- `fun getLastCallTime(): Instant?`

#### 2.3.2 FakeRateLimiter

**목적**: RateLimiter 인터페이스 모의 구현
**상태**: 토큰 상태, acquire() 호출 추적, 대기 시뮬레이션 선택적

```kotlin
// 구현할 인터페이스
interface RateLimiter {
    suspend fun acquire(tokensNeeded: Int = 1)
    fun getAvailableTokens(): Int
    fun getWaitTimeMillis(): Long
    fun getStatus(): RateLimiterStatus
}

// Fake 구현의 책임
- 토큰 버킷 상태 모의 (실제 시간 경과 시뮬레이션 안 함)
- acquire() 호출 추적
- 실제 대기 없음 (테스트 속도 우선)
- 호출 시퀀스 검증
```

**상태 변수**:
- `availableTokens: Int`: 현재 토큰 수
- `capacity: Int`: 최대 용량
- `acquireCalls: List<AcquireCall>`: acquire() 호출 기록
- `simulateDelay: Boolean`: 지연 시뮬레이션 여부

**메서드**:
- `suspend fun acquire(tokensNeeded: Int = 1)`
- `fun getAvailableTokens(): Int`
- `fun getWaitTimeMillis(): Long`
- `fun getStatus(): RateLimiterStatus`
- `fun getAcquireCallCount(): Int`
- `fun resetHistory()`
- `fun setSimulateDelay(enabled: Boolean)`

#### 2.3.3 FakeHttpClient (Recorded Responses)

**목적**: HttpClient의 HTTP 요청 모의
**상태**: 레코딩된 응답 관리, 요청 추적

```kotlin
// Ktor HttpClient 인터페이스 확장
// 실제로는 HttpClient를 직접 모의하기보다
// RecordedResponseRepository를 통해 응답 주입

// Fake 구현의 책임
- 사전 레코딩된 응답 반환
- HTTP 상태 코드 모의
- 요청 URL 및 파라미터 검증
- 오류 응답 시뮬레이션 (선택적)
```

**상태 변수**:
- `responses: Map<String, String>`: URL → 응답 매핑
- `requestLog: List<HttpRequest>`: 수행된 요청 기록
- `shouldFail: Map<String, Boolean>`: URL별 실패 여부

**메서드**:
- `fun <T> get(urlString: String, block: HttpRequestBuilder.() -> Unit): HttpResponse`
- `fun loadResponsesFromDirectory(path: String)`
- `fun addResponse(url: String, response: String)`
- `fun getRequestLog(): List<HttpRequest>`
- `fun resetRequestLog()`

#### 2.3.4 RecordedResponseRepository

**목적**: 레코딩된 응답의 중앙 관리
**상태**: 응답 파일 캐싱, 경로 매핑

```kotlin
// 책임
- src/liveTest/resources/responses에서 JSON 로드
- 메모리 캐싱으로 성능 최적화
- URL → 파일 경로 매핑
- 응답 반환 시 Fake 객체 주입
```

**상태 변수**:
- `responseCache: Map<String, String>`: 파일명 → 응답 캐시
- `basePath: Path`: 응답 파일 기본 경로
- `pathMappings: Map<String, String>`: URL 패턴 → 파일 경로

**메서드**:
- `fun loadResponse(category: String, fileName: String): String`
- `fun getResponse(url: String): String?`
- `fun preloadCategory(category: String)`
- `fun clearCache()`
- `fun getLoadedResponseCount(): Int`

#### 2.3.5 NoOpRateLimiter

**목적**: Rate Limiting을 완전히 무시하는 구현
**상태**: 별도의 상태 없음

```kotlin
// 책임
- 모든 acquire() 호출을 즉시 반환
- 토큰 제한 없음
- 실제 대기 없음
- 로깅만 선택적으로 수행
```

**메서드**:
- `suspend fun acquire(tokensNeeded: Int = 1)` → 즉시 반환
- `fun getAvailableTokens(): Int` → 무한대 반환
- `fun getWaitTimeMillis(): Long` → 0 반환
- `fun getStatus(): RateLimiterStatus` → 무제한 상태 반환

### 2.4 Fake 객체 초기화 전략

#### 테스트 Fixture 패턴

```kotlin
@DisplayName("YahooChartService - Unit Tests")
class YahooChartServiceTest {

    // Fake 객체들
    private lateinit var fakeAuthStrategy: FakeAuthStrategy
    private lateinit var fakeRateLimiter: FakeRateLimiter
    private lateinit var recordedResponseRepository: RecordedResponseRepository

    @BeforeEach
    fun setUp() {
        // 매 테스트마다 새로운 Fake 객체 생성
        fakeAuthStrategy = FakeAuthStrategy()
        fakeRateLimiter = NoOpRateLimiter()
        recordedResponseRepository = RecordedResponseRepository()

        // 필요한 응답 미리 로드
        recordedResponseRepository.preloadCategory(
            RecordingConfig.Paths.Yahoo.Chart.DAILY
        )
    }

    @AfterEach
    fun tearDown() {
        // 상태 초기화 (선택적)
        fakeAuthStrategy.reset()
        fakeRateLimiter.reset()
        recordedResponseRepository.clearCache()
    }
}
```

#### 상태 초기화 전략

1. **테스트 격리**: 각 테스트마다 새 Fake 객체 인스턴스 생성
2. **상태 리셋**: @AfterEach에서 호출 기록 초기화
3. **응답 캐싱**: RecordedResponseRepository의 응답은 유지 (성능)
4. **실패 시나리오**: setFailureMode()로 선택적으로 구성

---

## 3. 테스트 리팩토링 전략

### 3.1 디렉토리 구조 설계

```
src/
├── main/kotlin/
│   └── com/ulalax/ufc/
│       ├── domain/chart/
│       │   ├── ChartService.kt
│       │   ├── YahooChartService.kt
│       │   └── ...
│       ├── domain/quote/
│       │   ├── QuoteService.kt
│       │   └── ...
│       └── ...
│
├── test/kotlin/                    # Unit Tests (TDD 기반)
│   └── com/ulalax/ufc/
│       ├── domain/chart/
│       │   ├── YahooChartServiceTest.kt         # 단위 테스트
│       │   └── YahooChartServiceTestHelper.kt   # 헬퍼 함수
│       ├── domain/quote/
│       │   └── YahooQuoteSummaryServiceTest.kt
│       ├── client/
│       │   └── UFCClientTest.kt                 # 통합 테스트 (단위 테스트 레벨)
│       ├── fake/                                # Fake 객체들
│       │   ├── FakeAuthStrategy.kt
│       │   ├── FakeRateLimiter.kt
│       │   ├── NoOpRateLimiter.kt
│       │   ├── RecordedResponseRepository.kt
│       │   └── ...
│       └── fixture/                             # 테스트 픽스처
│           ├── OHLCVDataFixture.kt
│           ├── AuthResultFixture.kt
│           └── ...
│
└── liveTest/kotlin/                # Integration Tests (실제 API)
    └── com/ulalax/ufc/
        ├── domain/chart/
        │   └── YahooChartServiceLiveSpec.kt    # 라이브 스펙 테스트
        ├── domain/quote/
        │   └── YahooQuoteSummaryServiceLiveSpec.kt
        ├── client/
        │   └── UFCClientLiveSpec.kt
        └── resources/responses/                 # 레코딩된 응답
            ├── yahoo/chart/daily/
            │   ├── aapl_daily_1m.json
            │   └── ...
            ├── yahoo/chart/intraday/
            │   └── ...
            └── yahoo/quote/summary/
                └── ...
```

### 3.2 테스트 분류 전략

#### Unit Tests (src/test)
- **특징**: Fake 객체 사용, 외부 의존성 없음, 빠른 실행
- **목적**: 개별 클래스의 비즈니스 로직 검증
- **명명 규칙**: `*Test.kt`
- **테스트 시간**: 밀리초 단위
- **활용**: CI/CD 파이프라인에서 매번 실행

```kotlin
class YahooChartServiceTest {
    // Fake 의존성 주입
    // 상태 기반 테스트 (Output Assertion)
    // 메모리 기반 데이터 처리
}
```

#### Integration Tests (src/liveTest)
- **특징**: 실제 API 호출, 레코딩된 응답 재사용
- **목적**: 서비스 통합 검증, API 계약 검증
- **명명 규칙**: `*Spec.kt` 또는 `*LiveSpec.kt`
- **테스트 시간**: 초 단위
- **활용**: 배포 전 수행, 정기적 검증

```kotlin
class YahooChartServiceLiveSpec : LiveTestBase() {
    // 실제 HTTPClient 사용
    // 레코딩된 응답으로 테스트
    // 실제 데이터 검증
}
```

### 3.3 테스트 계층 아키텍처

#### 테스트 피라미드

```
       /\
      /  \        E2E Tests (매우 적음)
     /____\       - 전체 워크플로우 검증

    /      \     Integration Tests (중간)
   / Fake   \    - 서비스 통합
  /  HTTP   \    - 라이브 API (레코딩)
 /___________\

 /           \  Unit Tests (많음)
/  Fake All   \ - 개별 메서드
/ Dependencies \ - 비즈니스 로직
/_____________\
```

**적용 비율**:
- Unit Tests: 70% (YahooChartServiceTest, RateLimiterTest 등)
- Integration Tests: 25% (YahooChartServiceLiveSpec, UFCClientLiveSpec)
- E2E Tests: 5% (실제 사용자 시나리오)

---

## 4. 스펙 스타일 적용 계획

### 4.1 BDD 스타일 (Behavior Driven Development) 통합

클래식 TDD + BDD의 장점을 결합한 스펙 스타일:

```kotlin
@DisplayName("YahooChartService")
class YahooChartServiceTest {

    private lateinit var fakeAuthStrategy: FakeAuthStrategy
    private lateinit var fakeRateLimiter: FakeRateLimiter
    private lateinit var service: YahooChartService

    @BeforeEach
    fun setUp() {
        fakeAuthStrategy = FakeAuthStrategy()
        fakeRateLimiter = NoOpRateLimiter()
        // service 초기화...
    }

    @Nested
    @DisplayName("getChartData()")
    inner class GetChartData {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            @DisplayName("AAPL의 일일 차트 데이터를 조회할 수 있다")
            fun shouldFetchAaplDailyChartData() {
                // Given: AAPL 심볼로 요청
                val symbol = "AAPL"
                val interval = Interval.OneDay
                val period = Period.OneMonth

                // When: 차트 데이터 조회
                val result = service.getChartData(symbol, interval, period)

                // Then: 데이터가 반환되어야 함
                assertThat(result).isNotEmpty()
                assertThat(result).allMatch { ohlcv ->
                    ohlcv.timestamp > 0L &&
                    ohlcv.open > 0.0 &&
                    ohlcv.close > 0.0
                }
            }

            @Test
            @DisplayName("다중 심볼의 차트 데이터를 조회할 수 있다")
            fun shouldFetchMultipleSymbolsChartData() {
                // Given
                val symbols = listOf("AAPL", "GOOGL", "TSLA")

                // When
                val result = service.getChartData(symbols)

                // Then
                assertThat(result).hasSize(3)
                assertThat(result.keys).containsExactlyInAnyOrder("AAPL", "GOOGL", "TSLA")
                result.values.forEach { data ->
                    assertThat(data).isNotEmpty()
                }
            }
        }

        @Nested
        @DisplayName("에러 케이스")
        inner class ErrorCases {

            @Test
            @DisplayName("비어있는 심볼로 요청하면 예외를 발생시킨다")
            fun shouldThrowExceptionForBlankSymbol() {
                // When & Then
                assertThatThrownBy {
                    runBlocking { service.getChartData("") }
                }
                    .isInstanceOf(UfcException::class.java)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SYMBOL)
            }

            @Test
            @DisplayName("인증 실패 시 예외를 발생시킨다")
            fun shouldThrowExceptionWhenAuthenticationFails() {
                // Given
                fakeAuthStrategy.setFailureMode(true)

                // When & Then
                assertThatThrownBy {
                    runBlocking { service.getChartData("AAPL") }
                }
                    .isInstanceOf(UfcException::class.java)
            }
        }

        @Nested
        @DisplayName("상태 검증")
        inner class StateValidation {

            @Test
            @DisplayName("레이트 리미터의 acquire()가 호출되어야 한다")
            fun shouldCallRateLimiterAcquire() {
                // Given & When
                runBlocking { service.getChartData("AAPL") }

                // Then: 레이트 리미터 호출 검증
                assertThat(fakeRateLimiter.getAcquireCallCount()).isGreaterThan(0)
            }

            @Test
            @DisplayName("인증 결과를 사용하여 요청해야 한다")
            fun shouldUseAuthResultInRequest() {
                // Given & When
                runBlocking { service.getChartData("AAPL") }

                // Then: 인증 호출 검증
                assertThat(fakeAuthStrategy.getCallCount()).isGreaterThan(0)
            }
        }
    }

    @Nested
    @DisplayName("getRawChartData()")
    inner class GetRawChartData {

        @Test
        @DisplayName("원본 응답을 반환할 수 있다")
        fun shouldReturnRawResponse() {
            // Given, When, Then...
        }
    }
}
```

### 4.2 테스트 그룹화 전략

#### 테스트 메서드 분류

1. **긍정 케이스 (Happy Path)**
   - 정상 입력에 대한 예상되는 동작
   - 예: `shouldFetchAaplDailyChartData()`

2. **부정 케이스 (Negative Cases)**
   - 잘못된 입력, 경계값
   - 예: `shouldThrowExceptionForBlankSymbol()`

3. **엣지 케이스 (Edge Cases)**
   - 경계 조건, 특수 시나리오
   - 예: `shouldHandleMaxPeriodData()`

4. **상태 검증 (State Verification)**
   - 메서드 호출 후 객체 상태 변화
   - 예: `shouldCallRateLimiterAcquire()`

5. **상호작용 검증 (Interaction Verification)**
   - 협력 객체와의 상호작용
   - 예: `shouldUseAuthResultInRequest()`

#### @Nested 계층 구조

```
YahooChartServiceTest
├── GetChartData (@Nested)
│   ├── SuccessCases (@Nested)
│   │   ├── shouldFetchAaplDailyChartData
│   │   └── shouldFetchMultipleSymbolsChartData
│   ├── ErrorCases (@Nested)
│   │   ├── shouldThrowExceptionForBlankSymbol
│   │   └── shouldThrowExceptionWhenAuthenticationFails
│   └── StateValidation (@Nested)
│       ├── shouldCallRateLimiterAcquire
│       └── shouldUseAuthResultInRequest
│
├── GetRawChartData (@Nested)
│   ├── SuccessCases (@Nested)
│   └── ErrorCases (@Nested)
│
└── Integration (@Nested)
    ├── shouldHandleSequentialRequests
    └── shouldMaintainStateAcrossMultipleCalls
```

### 4.3 테스트 명명 규칙

#### 메서드명 규칙: `should[Expected Behavior][When Condition]`

| 패턴 | 예시 | 설명 |
|------|------|------|
| `should[Action]` | `shouldFetchAaplDailyChartData` | 성공 케이스 |
| `should[Action]When[Condition]` | `shouldThrowExceptionWhenSymbolIsBlank` | 조건부 동작 |
| `shouldNot[Action]` | `shouldNotReturnNullData` | 부정 케이스 |
| `should[State]After[Action]` | `shouldIncreaseAcquireCountAfterFetch` | 상태 변화 |

#### @DisplayName 사용

```kotlin
@Test
@DisplayName("AAPL의 일일 차트 데이터를 조회할 수 있다")
fun shouldFetchAaplDailyChartData() { ... }

// 테스트 리포트: "AAPL의 일일 차트 데이터를 조회할 수 있다"
// 메서드명만으로도 테스트 목적이 명확함
```

---

## 5. 단계별 실행 계획

### Phase 2: Fake 객체 및 Fixture 생성 (1주)

#### 2-1: Fake 객체 구조 생성
**파일**: `/src/test/kotlin/com/ulalax/ufc/fake/`

```
src/test/kotlin/com/ulalax/ufc/fake/
├── FakeAuthStrategy.kt
├── FakeRateLimiter.kt
├── NoOpRateLimiter.kt
├── RecordedResponseRepository.kt
├── FakeHttpClientBuilder.kt
└── ResponseFixtures.kt
```

**산출물**:
- FakeAuthStrategy: authenticate() 호출 추적, 설정 가능한 실패 시나리오
- FakeRateLimiter: acquire() 호출 로깅, 상태 검증
- RecordedResponseRepository: JSON 응답 로드 및 캐싱
- FakeHttpClientBuilder: 테스트별 맞춤형 HttpClient 구성

**체크리스트**:
- [ ] FakeAuthStrategy 구현 및 테스트
- [ ] FakeRateLimiter 구현 및 테스트
- [ ] RecordedResponseRepository 구현 및 테스트
- [ ] Fake 객체 통합 테스트

#### 2-2: 테스트 Fixture 생성
**파일**: `/src/test/kotlin/com/ulalax/ufc/fixture/`

```
src/test/kotlin/com/ulalax/ufc/fixture/
├── OHLCVDataFixture.kt
├── AuthResultFixture.kt
├── ChartDataResponseFixture.kt
├── RateLimiterStatusFixture.kt
└── TestDataBuilders.kt
```

**산출물**:
- 테스트 데이터 생성 함수
- Builder 패턴을 사용한 유연한 객체 구성

**예시**:
```kotlin
fun ohlcvData(
    timestamp: Long = 1700000000L,
    open: Double = 100.0,
    close: Double = 105.0
): OHLCVData = OHLCVData(
    timestamp = timestamp,
    open = open,
    high = close * 1.05,
    low = open * 0.95,
    close = close,
    volume = 1000000L,
    adjClose = close
)
```

**체크리스트**:
- [ ] OHLCVDataFixture 구현
- [ ] AuthResultFixture 구현
- [ ] ChartDataResponseFixture 구현
- [ ] Fixture 유효성 검증

**결과물**:
- Fake 객체 5개 이상
- Fixture 3개 이상
- 전체 테스트 통과: 100%

---

### Phase 3: Unit Tests 생성 (2주)

#### 3-1: YahooChartServiceTest 작성
**파일**: `/src/test/kotlin/com/ulalax/ufc/domain/chart/YahooChartServiceTest.kt`

**테스트 범위**:
- getChartData(single symbol): 5 테스트 (정상, 에러, 상태)
- getChartData(multiple symbols): 3 테스트
- getRawChartData(): 3 테스트
- 통합 시나리오: 2 테스트

**총 테스트 메서드**: 13개

**구조**:
```kotlin
class YahooChartServiceTest {
    @Nested
    inner class GetChartData { ... }

    @Nested
    inner class GetChartDataMultiple { ... }

    @Nested
    inner class GetRawChartData { ... }

    @Nested
    inner class IntegrationScenarios { ... }
}
```

**체크리스트**:
- [ ] 긍정 케이스 테스트 5개
- [ ] 부정 케이스 테스트 4개
- [ ] 상태 검증 테스트 3개
- [ ] 상호작용 검증 테스트 1개

#### 3-2: YahooQuoteSummaryServiceTest 작성
**파일**: `/src/test/kotlin/com/ulalax/ufc/domain/quote/YahooQuoteSummaryServiceTest.kt`

**테스트 범위**:
- getQuoteSummary(single symbol): 5 테스트
- getQuoteSummary(multiple symbols): 2 테스트
- 모듈 필터링: 2 테스트
- 에러 처리: 3 테스트

**총 테스트 메서드**: 12개

**체크리스트**:
- [ ] QuoteSummary 조회 테스트
- [ ] 모듈별 데이터 필터링 테스트
- [ ] 에러 처리 테스트

#### 3-3: UFCClientTest 작성
**파일**: `/src/test/kotlin/com/ulalax/ufc/client/UFCClientTest.kt`

**테스트 범위**:
- 클라이언트 생명주기: 2 테스트
- Chart API 통합: 3 테스트
- Quote API 통합: 3 테스트
- Rate Limiting 통합: 2 테스트
- 복합 시나리오: 2 테스트

**총 테스트 메서드**: 12개

**체크리스트**:
- [ ] 생성/종료 테스트
- [ ] API 메서드 통합 테스트
- [ ] 구성 관리 테스트

#### 3-4: 추가 Unit Tests
- `RateLimiterIntegrationTest` (Rate Limiter + Chart Service)
- `AuthenticationIntegrationTest` (Auth + Chart Service)

**결과물**:
- Unit Tests 4개 파일
- 총 테스트 메서드: 37개 이상
- 코드 커버리지: 70% 이상

---

### Phase 4: Live Tests 리팩토링 (1주)

#### 4-1: YahooChartServiceLiveSpec 작성
**파일**: `/src/liveTest/kotlin/com/ulalax/ufc/domain/chart/YahooChartServiceLiveSpec.kt`

**목적**: 실제 API와의 통신 검증
**의존성**: 실제 HttpClient, 실제 인증, 레코딩된 응답

**테스트 범위**:
- 실제 AAPL 데이터 조회
- 인덱스 데이터 조회
- 다양한 interval/period 조합
- 실제 에러 처리 검증

**테스트 메서드 수**: 8-10개 (YahooChartServiceLiveTest와 유사)

**체크리스트**:
- [ ] 기존 라이브 테스트와 비교하여 동일한 결과 확인
- [ ] 레코딩된 응답 사용 확인
- [ ] 실제 API 호출 시 응답 검증

#### 4-2: UFCClientLiveSpec 작성
**파일**: `/src/liveTest/kotlin/com/ulalax/ufc/client/UFCClientLiveSpec.kt`

**목적**: 통합 클라이언트의 라이브 검증
**테스트 범위**: UFCClientLiveTest와 동일

**테스트 메서드 수**: 15-17개

**체크리스트**:
- [ ] 모든 차트 데이터 조회 시나리오
- [ ] 모든 쿼트 요약 조회 시나리오
- [ ] 다중 심볼 조회 시나리오

#### 4-3: 기존 라이브 테스트 마이그레이션
**조치**:
1. 기존 `YahooChartServiceLiveTest.kt` → `YahooChartServiceLiveTest.deprecated.kt` (또는 삭제)
2. 기존 `UFCClientLiveTest.kt` → `UFCClientLiveTest.deprecated.kt` (또는 삭제)
3. 새 Spec 파일이 모든 테스트 케이스 커버 확인

**결과물**:
- YahooChartServiceLiveSpec 1개
- UFCClientLiveSpec 1개
- 레코딩된 응답 파일: 50개 이상 (이미 존재)

---

### Phase 5: 검증 및 최적화 (3일)

#### 5-1: 테스트 실행 및 검증

**Unit Tests 실행**:
```bash
./gradlew test
```

**체크리스트**:
- [ ] 모든 Unit Tests 통과 (37개 이상)
- [ ] 테스트 실행 시간: 3초 이내
- [ ] 코드 커버리지 리포트 생성

**Live Tests 실행**:
```bash
./gradlew liveTest
```

**체크리스트**:
- [ ] 모든 Live Tests 통과
- [ ] 레코딩 모드 테스트 (새 응답 기록)
- [ ] 비레코딩 모드 테스트 (저장된 응답 사용)

#### 5-2: 성능 비교

| 메트릭 | 이전 (Live Test) | 이후 (Unit Test) | 개선율 |
|--------|-----------------|-----------------|--------|
| YahooChartService 단일 테스트 | 2-5초 | 10-50ms | 50-500배 |
| 전체 YahooChartService 테스트 | 30초 | 200ms | 150배 |
| 메모리 사용량 | 100MB 이상 | 10MB 이하 | 90% 감소 |

#### 5-3: 코드 커버리지 검증

**목표 커버리지**:
- YahooChartService: 85% 이상
- YahooQuoteSummaryService: 85% 이상
- UFCClient: 75% 이상
- Rate Limiter: 90% 이상

**도구**: Jacoco
```bash
./gradlew test jacocoTestReport
```

#### 5-4: CI/CD 통합

**GitHub Actions 워크플로우**:
```yaml
name: Test Suite
on: [push, pull_request]
jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
      - run: ./gradlew test

  live-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
      - run: ./gradlew liveTest -Drecord.responses=false
```

**체크리스트**:
- [ ] CI/CD 파이프라인에서 Unit Tests 자동 실행
- [ ] 매일 정기적으로 Live Tests 실행
- [ ] 커버리지 리포트 자동 생성

#### 5-5: 문서화

**생성할 문서**:
- `TDD_MIGRATION_GUIDE.md`: 마이그레이션 가이드
- `UNIT_TEST_PATTERNS.md`: 테스트 패턴 예제
- `FAKE_OBJECTS_USAGE.md`: Fake 객체 사용 설명서
- `TEST_STRUCTURE.md`: 테스트 계층 아키텍처

**결과물**:
- 모든 테스트 통과
- 성능 50배 이상 향상
- 코드 커버리지 80% 이상

---

## 6. 주요 설계 결정사항

### 6.1 Fake vs Mock 선택

**결정**: **Fake 객체 우선**

**근거**:
- 클래식 TDD는 상태 기반 테스트를 권장 (Mock은 상호작용 기반)
- Fake 객체가 더 결정론적 (deterministic)
- 테스트 코드가 더 읽기 쉬움
- 테스트 속도가 더 빠름

**구분**:
- Fake: 실제 작동하는 구현체 (간소화), 상태 검증
- Mock: 호출 기록 중심, 상호작용 검증

```kotlin
// Fake: 상태 기반
val fakeRateLimiter = FakeRateLimiter()
fakeRateLimiter.acquire(5)
assertThat(fakeRateLimiter.getAcquireCallCount()).isEqualTo(1)

// Mock: 상호작용 기반 (사용하지 않음)
val mockRateLimiter = mockk<RateLimiter>()
coEvery { mockRateLimiter.acquire(5) } just Runs
coVerify { mockRateLimiter.acquire(5) }
```

### 6.2 레코딩 응답 전략

**결정**: **Live Tests는 레코딩된 응답 사용**

**장점**:
- 네트워크 지연 제거
- API 변경 시에도 테스트 실행 가능
- 재현 가능한 결과
- 테스트 속도 향상

**방식**:
1. 개발 초기: 실제 API로 레코딩
2. 배포 전: 레코딩된 응답으로 검증
3. 주기적: 새 레코딩으로 응답 업데이트

```bash
# 첫 실행: 실제 API 호출, 응답 저장
./gradlew liveTest -Drecord.responses=true

# 이후 실행: 저장된 응답 사용
./gradlew liveTest -Drecord.responses=false
```

### 6.3 테스트 계층 분리

**결정**: **명확한 3계층 분리**

| 계층 | 파일 위치 | 의존성 | 실행 시간 | 빈도 |
|-----|---------|--------|---------|------|
| Unit | src/test | Fake | ms | 매 커밋 |
| Integration | src/liveTest | 실제 (레코딩) | 초 | 배포 전 |
| E2E | (제외) | 실제 API | 분 | 수동 |

**장점**:
- 빠른 피드백 (Unit 테스트)
- 신뢰성 (Integration 테스트)
- 비용 효율 (레코딩 재사용)

### 6.4 Spring Test 미사용

**결정**: **Spring Test 미사용, 순수 Kotlin 테스트**

**근거**:
- 비즈니스 로직 테스트가 주 목표
- Spring 의존성 제거로 테스트 속도 향상
- Spring을 통한 의존성 주입이 불필요 (생성자 주입)

```kotlin
// 추천: Spring 미사용
class YahooChartServiceTest {
    private val fakeRateLimiter = FakeRateLimiter()
    private val service = YahooChartService(
        fakeHttpClient, fakeRateLimiter, fakeAuthResult
    )
}

// 미추천: Spring Test 사용
@SpringBootTest
class YahooChartServiceTest {
    @MockBean lateinit var rateLimiter: RateLimiter
    @InjectMocks lateinit var service: YahooChartService
}
```

### 6.5 @Nested 계층 제한

**결정**: **최대 3 수준의 @Nested 사용**

**구조**:
```
ClassName
├── MethodUnderTest (@Nested)        # 1 수준
│   ├── ScenarioGroup (@Nested)      # 2 수준
│   │   ├── Test case               # 3 수준 (최대)
```

**이유**:
- 가독성 유지
- IDE 네비게이션 편의
- 들여쓰기 과다 방지

### 6.6 테스트 데이터 관리

**결정**: **Fixture 패턴 + Builder 패턴**

```kotlin
// Fixture 함수 (빠른 생성)
fun defaultOHLCVData() = OHLCVData(...)

// Builder 패턴 (유연한 커스터마이징)
OHLCVDataBuilder()
    .withTimestamp(1700000000L)
    .withClose(105.0)
    .build()

// 권장: Fixture 사용 (테스트가 간단한 경우)
val data = defaultOHLCVData()

// 권장: Builder 사용 (커스터마이징이 필요한 경우)
val data = OHLCVDataBuilder()
    .withClose(110.0)
    .build()
```

### 6.7 에러 처리 테스트

**결정**: **AssertJ 사용, assertThatThrownBy() 권장**

```kotlin
// 권장
assertThatThrownBy { runBlocking { service.getChartData("") } }
    .isInstanceOf(UfcException::class.java)
    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SYMBOL)
    .hasMessageContaining("심볼이 비어있습니다")

// 미추천
try {
    runBlocking { service.getChartData("") }
    fail("Expected UfcException")
} catch (e: UfcException) {
    assertEquals(ErrorCode.INVALID_SYMBOL, e.errorCode)
}
```

---

## 7. 마이그레이션 체크리스트

### 사전 작업
- [ ] 현재 Live Tests 실행하여 모든 응답 레코딩
- [ ] Fake 객체 개발 환경 설정
- [ ] Gradle 테스트 설정 검증

### Phase 2 완료 기준
- [ ] 5개 이상의 Fake 객체 구현
- [ ] 3개 이상의 Fixture 구현
- [ ] Fake 객체 단위 테스트 100% 통과

### Phase 3 완료 기준
- [ ] 37개 이상의 Unit Tests 작성
- [ ] 모든 Unit Tests 통과
- [ ] 코드 커버리지 70% 이상
- [ ] 테스트 실행 시간 < 3초

### Phase 4 완료 기준
- [ ] YahooChartServiceLiveSpec 작성 및 통과
- [ ] UFCClientLiveSpec 작성 및 통과
- [ ] 기존 Live Tests와 동일한 커버리지 확인

### Phase 5 완료 기준
- [ ] 모든 테스트 (Unit + Live) 통과
- [ ] 성능 개선 50배 이상
- [ ] 코드 커버리지 80% 이상
- [ ] 문서화 완료

---

## 8. 위험 요소 및 완화 전략

### 위험 1: 레코딩된 응답의 부실화
**위험도**: 중간
**완화 전략**:
- 월 1회 Live Tests 실행하여 응답 업데이트
- API 변경 모니터링 (Yahoo Finance 공지사항)
- 응답 버전 관리 (git tracking)

### 위험 2: Fake 객체의 부정확함
**위험도**: 높음
**완화 전략**:
- Live Tests로 정기적 검증
- Fake 객체와 실제 구현 차이 최소화
- Fake 객체 변경 시 관련 Live Tests 즉시 수행

### 위험 3: 테스트 메서드 수의 증가
**위험도**: 낮음
**완화 전략**:
- Unit Tests는 빠르므로 실행 시간 증가 미미
- CI/CD에서 Live Tests는 선택적 실행
- 병렬 실행으로 성능 향상

### 위험 4: 새로운 기능 추가 시 테스트 갭
**위험도**: 중간
**완화 전략**:
- TDD 원칙 준수: 테스트 먼저 작성
- 코드 리뷰에서 테스트 커버리지 검증
- Jacoco 최소 커버리지 설정 (80%)

---

## 9. 참고 자료 및 표준

### 클래식 TDD 원칙
- Kent Beck의 "Test Driven Development: By Example"
- Growing Object-Oriented Software, Guided by Tests (Steve Freeman, Nat Pryce)

### 상태 기반 테스트 vs 상호작용 기반 테스트
- "London School vs Classicist Testing" (Martin Fowler)
- Fake 객체 우선 권장

### Kotlin 테스트 프레임워크
- JUnit 5 (Jupiter)
- AssertJ (fluent assertions)
- MockK (Kotlin mocking, 필요시에만)

### 테스트 네이밍 컨벤션
- BDD 스타일: Given-When-Then
- Describe-It 스타일: @Nested, @DisplayName

---

## 10. 예상 결과

### 정량적 개선
- **테스트 속도**: 30초 → 200ms (150배 향상)
- **테스트 메서드**: 29개 (기존) → 66개 (127% 증가)
- **코드 커버리지**: 45% → 80% (35% 포인트 증가)
- **메모리 사용량**: 100MB → 10MB (90% 감소)

### 정성적 개선
- **테스트 신뢰성**: 높아짐 (결정론적 동작)
- **개발 피드백**: 매우 빨라짐 (ms 단위)
- **유지보수성**: 향상됨 (명확한 구조)
- **문서화**: 개선됨 (테스트가 사양)

### 개발 경험
- **로컬 개발**: TDD 원칙 준수 가능 (빠른 실행)
- **코드 리뷰**: 더 명확한 의도 파악
- **온보딩**: 테스트로 학습 가능
- **버그 수정**: 더 빠른 발견 및 재현

---

## 부록 A: Fake 객체 구현 예제

### FakeAuthStrategy 스켈레톤

```kotlin
class FakeAuthStrategy : AuthStrategy {
    private var callCount = 0
    private var shouldFail = false
    private val callHistory = mutableListOf<Instant>()
    private var predefinedResult = AuthResult(
        crumb = "TEST_CRUMB_TOKEN_1234567890",
        strategy = "fake"
    )

    override suspend fun authenticate(): AuthResult {
        callCount++
        callHistory.add(Instant.now())

        if (shouldFail) {
            throw UfcException(
                errorCode = ErrorCode.AUTHENTICATION_FAILED,
                message = "Simulated authentication failure"
            )
        }

        return predefinedResult
    }

    fun getCallCount(): Int = callCount
    fun getCallHistory(): List<Instant> = callHistory.toList()
    fun setFailureMode(shouldFail: Boolean) { this.shouldFail = shouldFail }
    fun setPredefinedResult(result: AuthResult) { this.predefinedResult = result }
    fun reset() {
        callCount = 0
        callHistory.clear()
        shouldFail = false
    }
}
```

### RecordedResponseRepository 스켈레톤

```kotlin
class RecordedResponseRepository {
    private val responseCache = mutableMapOf<String, String>()
    private val basePath = Paths.get("src/liveTest/resources/responses")

    fun loadResponse(category: String, fileName: String): String {
        val key = "$category/$fileName"

        return responseCache.getOrPut(key) {
            val filePath = basePath.resolve(category).resolve("$fileName.json")
            filePath.toFile().readText()
        }
    }

    fun preloadCategory(category: String) {
        val categoryPath = basePath.resolve(category)
        if (categoryPath.toFile().exists()) {
            categoryPath.toFile().walkBottomUp().forEach { file ->
                if (file.isFile && file.extension == "json") {
                    val key = "$category/${file.nameWithoutExtension}"
                    responseCache[key] = file.readText()
                }
            }
        }
    }

    fun clearCache() = responseCache.clear()
    fun getCachedResponseCount(): Int = responseCache.size
}
```

---

**작성자**: UFC Project Analysis Team
**버전**: 1.0
**마지막 업데이트**: 2025-12-02
