# Test Writing Principles

> **목적**: AI 에이전트를 위한 백엔드 테스트 작성 가이드
> **철학**: Classical TDD (State-based Testing) - **Fake 객체 우선, Mock은 최후의 수단**
> **대상**: Kotlin + JUnit 5 기반 프로젝트
> **작성일**: 2025-12-02
>
> **핵심 원칙**: 테스트는 시스템의 **스펙 문서**이며, 구현 세부 사항에 의존하지 않도록 **상태 검증(assert state)**을 통해 **리팩토링 내성**을 높입니다.

---

## 🔍 빠른 참조: 테스트 유형별 분류

| 테스트 유형 | 파일명 | 범위 | 외부 의존성 | Fake 객체 생성 방식 | 디렉토리 |
|-----------|-------|------|-----------|-----------------|---------|
| **Unit Test** | `*Test.kt` | 단일 클래스/함수 | ❌ **없음** (Fake 사용) | 직접 생성 (`FakeXXX()`) | `src/test/kotlin/` |
| **Integration Test** | `*Spec.kt` | 시스템 연동 | ✅ **있음** (DB, HTTP, 파일 등) | 프레임워크에 따라 다름 | `src/integrationTest/kotlin/` |

**핵심 차이**:
- **Unit Test**: 외부 의존성 없이 순수 Kotlin/JUnit 5로 테스트. Fake 객체를 테스트 코드에서 직접 생성.
- **Integration Test**: 실제 외부 시스템(DB, HTTP, 메시지큐 등)과 연동. 실제 인프라 또는 Testcontainers 사용.

---

## 1. 핵심 원칙

| 원칙 | 설명 |
|-----|------|
| **Test as Specification** | 테스트 = 실행 가능한 스펙. 테스트 이름과 구조만으로 동작 이해 가능 |
| **Given-When-Then** | 모든 테스트는 명확한 전제-행동-결과 구조 |
| **Isolation** | 각 테스트는 독립적. 순서 의존성 없음 |
| **Fake > Mock** | Mock은 최후의 수단. 가능하면 Fake 구현 사용 |
| **Single Concern** | 하나의 동작만 검증. 50줄 이하 |

---

## 2. 스펙 스타일 테스트 (BDD)

권장 방식은 **Specification-Based Testing** 접근법입니다.

### 2.1 구조 요소

```
테스트 클래스
├── @DisplayName("API명 - 한국어 설명")
└── @Nested
    ├── @DisplayName("메서드명() - 무엇을 하는가")
    └── @Nested
        ├── @DisplayName("정상 케이스 / 에러 케이스")
        └── @Test
            fun `동작_조건_결과`() = unitTest {
                // Given: 전제 조건
                // When: 실행
                // Then: 검증
            }
```

### 2.2 예제

```kotlin
@DisplayName("User Service - 사용자 관리")
class UserServiceTest : UnitTestBase() {  // Unit Test: *Test.kt

    @Nested
    @DisplayName("getUser() - 사용자 조회")
    inner class GetUser {

        @Nested
        @DisplayName("정상 케이스")
        inner class SuccessCases {

            @Test
            fun `사용자 ID로 조회 성공`() = unitTest {
                // Given: 테스트 데이터 준비
                val fakeRepository = FakeUserRepository()
                val service = UserService(fakeRepository)
                val user = User(id = "user-123", name = "홍길동")
                fakeRepository.save(user)

                // When: 메서드 호출
                val result = service.getUser("user-123")

                // Then: 결과 검증
                assertThat(result).isNotNull()
                assertThat(result?.name).isEqualTo("홍길동")
            }
        }

        @Nested
        @DisplayName("에러 케이스")
        inner class ErrorCases {

            @Test
            fun `존재하지 않는 사용자 조회 시 null 반환`() = unitTest {
                // Given
                val fakeRepository = FakeUserRepository()
                val service = UserService(fakeRepository)

                // When
                val result = service.getUser("invalid-id")

                // Then
                assertThat(result).isNull()
            }
        }
    }
}
```

### 2.3 스펙 스타일의 장점

- ✅ **문서화**: 테스트 자체가 스펙 문서
- ✅ **가독성**: 계층적 구조로 한눈에 이해 가능
- ✅ **유지보수**: API 변경 시 테스트 = 요구사항 변경
- ✅ **커뮤니케이션**: 비개발자도 테스트 읽기 가능

---

## 3. Test Doubles 전략

### 3.1 선택 기준

| 더블 | 사용 시점 | 특징 |
|-----|---------|------|
| **Fake** | 비즈니스 로직 검증 | 완전한 구현체, 상태 관리 가능 |
| **Mock** | 호출 검증 시 | 호출 여부만 확인 (최후의 수단) |
| **Stub** | 단순 반환값 필요 | 고정 값만 반환 |

### 3.2 Fake 우선 원칙 - Classical TDD (State-based Testing)

**Fake 객체란**: 프로덕션 인터페이스를 **완전히 구현**한 테스트용 구현체. 실제 동작을 시뮬레이션합니다.

**핵심 차이**:
- **Fake**: 상태를 변경하고 관리 → **상태 검증(assert state)** 가능 ✅
- **Mock**: 호출을 기록하기만 함 → **호출 검증(verify call)** 만 가능 ❌

#### 3.2.1 Fake 구현 예제

```kotlin
// 프로덕션 인터페이스
interface UserRepository {
    suspend fun save(user: User): User
    suspend fun findById(id: String): User?
    suspend fun findAll(): List<User>
}

// 테스트용 Fake 구현 (HashMap 기반 In-Memory 저장소)
class FakeUserRepository : UserRepository {
    private val users = ConcurrentHashMap<String, User>()

    override suspend fun save(user: User): User {
        this.users[user.id] = user
        return user
    }

    override suspend fun findById(id: String): User? {
        return users[id]
    }

    override suspend fun findAll(): List<User> {
        return users.values.toList()
    }
}
```

#### 3.2.2 상태 검증 (Fake 사용)

```kotlin
// ✅ 좋은 예: Fake + 상태 검증
// → 리팩토링 내성 높음 (구현 변경 영향 적음)
@Test
fun `사용자 정보를 저장소에 저장한다`() = unitTest {
    val fakeRepository = FakeUserRepository()
    val service = UserService(fakeRepository)

    // When
    service.saveUser(User(id = "user-123", name = "홍길동"))

    // Then: 상태 검증 (행위가 아니라 결과를 확인)
    val saved = fakeRepository.findById("user-123")
    assertThat(saved).isNotNull()
    assertThat(saved?.name).isEqualTo("홍길동")
}
```

#### 3.2.3 행위 검증 (Mock 사용)

```kotlin
// ❌ 나쁜 예: Mock + 행위 검증
// → 리팩토링 내성 낮음 (구현 변경 시 테스트 깨짐)
@Test
fun `사용자 정보를 저장한다`() = unitTest {
    val mockRepository = mockk<UserRepository>()
    val service = UserService(mockRepository)

    service.saveUser(testUser)

    // 호출 여부만 확인 (실제 결과는 검증 안 함)
    verify { mockRepository.save(any()) }
}
```

**왜 Fake가 더 좋을까?**
- 🎯 **리팩토링 내성**: 구현을 변경해도 테스트가 깨지지 않음
- 📖 **문서화**: 실제 데이터 흐름이 명확함
- 🔍 **디버깅**: 저장된 상태를 직접 검증 가능
- ♻️ **재사용**: 같은 Fake를 여러 테스트에서 사용 가능

### 3.3 Mock 사용 허용 (예외)

**원칙**: Mock은 **Fake 구현이 불가능하거나 비용이 과도할 때만** 예외적으로 허용하며, **반드시 주석으로 사유를 명시**해야 합니다.

**허용되는 경우**:

| 상황 | Fake 구현이 어려운 이유 | Mock 사용 이유 | 해결 방법 |
|-----|-------------|------------|---------|
| **복잡한 HTTP 클라이언트** | URL routing, 직렬화, 에러 핸들링 복잡 | Recorded JSON 응답 사용 | `MockHttpClient` + JSON 파일 |
| **외부 서비스 API** | 비자체 제어, 네트워크 의존 | 호출 검증만 필요 | Mock은 호출 여부만 확인 |
| **시간/난수 제어** | 시스템 시간 제어 필요 | TimeProvider Mock | 시간 기반 로직 분리 |
| **결제/외부 결제 게이트웨이** | 실제 결제 불가능 | Mock 결제 API | 단위 테스트는 Mock, 통합 테스트만 실제 |

#### 3.3.1 Mock 정당화 예시

```kotlin
// ✅ Mock 정당화: Fake HttpClient는 과도함
@Test
fun `HTTP 응답 파싱 성공`() = unitTest {
    // Mock 사용 이유:
    // - HttpClient 전체 구현은 매우 복잡함
    // - Recorded JSON으로 충분
    // - HTTP 파싱 로직만 검증 필요
    val mockHttpClient = MockHttpClient.withResponse(jsonResponse)
    val apiClient = ApiClient(httpClient = mockHttpClient)

    val result = apiClient.getUser("user-123")

    assertThat(result).isNotNull()
}

// ✅ Mock 정당화: 외부 서비스 호출 검증만 필요
@Test
fun `이벤트 발행 검증`() = unitTest {
    // Mock 사용 이유:
    // - EventPublisher의 호출 여부만 확인 필요
    // - 상태 검증 불필요 (구현 관심 없음)
    val mockEventPublisher = mockk<EventPublisher>()
    val service = UserService(mockEventPublisher)

    service.updateUser(user)

    verify { mockEventPublisher.publish(any<UserUpdatedEvent>()) }
}
```

#### 3.3.2 Mock 선택 가이드

```
상황 분석
├─ Fake 구현 가능? (Repository, Service, 간단한 의존성)
│  └─ YES → Fake 사용 (권장) ✅
├─ Fake 구현 비용 vs Mock 사용 비용
│  ├─ Fake 비용 < Mock 비용 → Fake 사용
│  └─ Fake 비용 >> Mock 비용 → Mock 사용 (주석 필수) ⚠️
└─ 호출 검증만 필요?
   └─ YES → Mock 사용 가능 (호출 여부 확인)
```

---

## 4. 테스트 명명 규칙

### 4.1 패턴

```
{메서드}_{상황}_{결과}
```

### 4.2 예제

| 상황 | 예시 |
|-----|------|
| 정상 동작 | `getUser should return user when id is valid` |
| 예외 처리 | `getUser should throw exception when id is invalid` |
| 경계 조건 | `getUser should handle empty result` |
| 비즈니스 규칙 | `calculateTotalPrice should apply discount before tax` |

---

## 5. 테스트 데이터 관리

### 5.1 Mock 응답 (Recorded Responses)

**Response Recording 시스템** 사용 예시:

```
src/test/resources/responses/
├── users/
│   ├── user_valid.json
│   └── user_not_found.json
├── orders/
│   └── order_list.json
└── errors/
    ├── 401_unauthorized.json
    ├── 404_not_found.json
    └── 429_rate_limited.json
```

사용:
```kotlin
@Test
fun `테스트`() = unitTest {
    loadMockResponse("user_valid")  // 자동으로 로드
    // ...
}
```

### 5.2 고유 데이터 생성

테스트 격리를 위해 고유 데이터 사용:

```kotlin
@Test
fun `데이터 저장`() = unitTest {
    val uniqueId = "user-${System.currentTimeMillis()}"
    val testUser = User(id = uniqueId, name = "테스트유저")

    repository.save(testUser)

    assertThat(repository.findById(uniqueId)).isNotNull()
}
```

---

## 6. 피해야 할 것 (Anti-patterns)

| Anti-pattern | 문제 | 대안 |
|-------------|------|------|
| 하드코딩된 ID | 테스트 간 충돌 | UUID, Timestamp 사용 |
| 과도한 Mock | 구현에만 의존 | Fake 구현 사용 |
| 긴 테스트 메서드 | 가독성 저하 | 50줄 이하로 분리 |
| 테스트 순서 의존 | 불안정성 | 각 테스트 독립 실행 |
| 복잡한 Setup | 유지보수 어려움 | Mother 패턴 사용 |

---

## 7. Fixture 패턴 (Mother Pattern)

테스트 데이터 반복 생성 제거:

```kotlin
object UserMother {
    fun simple(): User = User(
        id = "user-123",
        name = "홍길동",
        email = "hong@example.com",
        createdAt = LocalDateTime.now()
    )

    fun withName(name: String): User =
        simple().copy(name = name)

    fun withEmail(email: String): User =
        simple().copy(email = email)
}

// 사용
@Test
fun `테스트`() = unitTest {
    val user = UserMother.simple()
        .withName("김철수")
        .withEmail("kim@example.com")
    // ...
}
```

---

## 8. 테스트 분류

### 8.1 단위 테스트 (Unit Test - `*Test.kt`)

| 항목 | 설명 |
|-----|------|
| **목적** | 도메인 로직 및 비즈니스 규칙 검증 (스펙 = 실행 가능한 요구사항) |
| **외부 의존성** | ❌ **없음** (모든 의존성은 Fake로 대체) |
| **의존성 생성** | **Fake 객체** 직접 생성 (Repository, Service 등) |
| **검증 방식** | **상태 검증** (assert state) - Mock verify 금지 |
| **특징** | 빠름 (< 100ms), 결정적, 독립적 |
| **위치** | `src/test/kotlin/` |
| **예시** | `UserServiceTest.kt`, `OrderCalculationTest.kt` |

**Unit Test의 핵심**:
- ✅ **외부 의존성 없이 순수하게 테스트** (DB, HTTP, 파일시스템 등 사용 안 함)
- ✅ Fake 객체를 직접 `new` 또는 `FakeXXX()`로 생성
- ✅ 리팩토링 내성 향상 (구현 변경 영향 최소화)
- ✅ 상태 변화 직접 검증 가능
- ✅ 테스트가 스펙 문서가 됨

**Unit Test 예시**:
```kotlin
// ✅ 외부 의존성 없이 순수 Kotlin으로 테스트
@DisplayName("User Service - 사용자 관리")
class UserServiceTest : UnitTestBase() {

    @Test
    fun `사용자 ID로 조회 성공`() = unitTest {
        // Fake 객체 직접 생성
        val fakeRepository = FakeUserRepository()
        val service = UserService(fakeRepository)

        // Given
        val user = User(id = "user-123", name = "홍길동")
        fakeRepository.save(user)

        // When
        val result = service.getUser("user-123")

        // Then: 상태 검증
        assertThat(result).isNotNull()
        assertThat(result?.name).isEqualTo("홍길동")
    }
}
```

### 8.2 통합 테스트 (Integration Test - `*Spec.kt`)

| 항목 | 설명 |
|-----|------|
| **목적** | 시스템 흐름 및 실제 인프라 연동 검증 (DB, HTTP, 파일시스템, 메시지큐 등) |
| **외부 의존성** | ✅ **있음** (실제 DB, HTTP 서버, 파일시스템 등) |
| **인프라** | 실제 환경 또는 Testcontainers |
| **Mock 사용** | 외부 API 호출 시에만 Mock/Stub 허용 |
| **특징** | 느림 (> 1s), 실제 환경 시뮬레이션, 데이터 영속성 검증 |
| **위치** | `src/integrationTest/kotlin/` |
| **실행** | `gradle integrationTest` |
| **예시** | `UserApiSpec.kt`, `DatabaseTransactionSpec.kt` |

**Integration Test의 핵심**:
- ✅ **실제 외부 시스템과 연동** (DB, HTTP, 파일, 메시지큐 등)
- ✅ Testcontainers를 활용한 격리된 테스트 환경
- ✅ 주요 성공 시나리오 (Happy Path)
- ✅ 데이터베이스 제약조건 검증 (FK, Unique, Check)
- ✅ 트랜잭션 동작 검증
- ✅ 복잡한 쿼리 성능 검증

**Integration Test 예시**:
```kotlin
// ✅ 실제 DB와 연동하는 통합 테스트
class OrderPaymentSpec : BaseIntegrationTest() {

    private lateinit var orderService: OrderService
    private lateinit var paymentClient: FakePaymentClient

    @BeforeEach
    fun setup() {
        // 실제 DB 연결 또는 Testcontainers
        val dataSource = createTestDataSource()
        val orderRepository = OrderRepositoryImpl(dataSource)

        // 외부 서비스는 Fake로 대체
        paymentClient = FakePaymentClient()

        orderService = OrderService(orderRepository, paymentClient)
        paymentClient.reset()
    }

    @Test
    fun `결제 성공 시 주문이 완료된다`() {
        // Given: Fake의 동작 변경
        paymentClient.setResponse(PaymentResponse("SUCCESS"))

        // When
        val result = orderService.order()

        // Then: DB에 실제로 저장되었는지 확인
        assertThat(result.status).isEqualTo("COMPLETED")
    }
}
```

---

## 9. Spring Boot 사용 시 고려사항

> ⚠️ **적용 대상**: 이 섹션은 **Spring Boot 프레임워크를 사용하는 프로젝트에만** 해당됩니다.
>
> Spring Boot를 사용하지 않는 프로젝트는 이 섹션을 건너뛰어도 됩니다.

Spring Boot 프레임워크를 사용할 경우, 의존성 주입(DI)과 컨텍스트 관리로 인한 추가 고려사항이 있습니다.

### 9.1 Spring Boot에서의 테스트 분류

| 테스트 유형 | Spring 사용 | Fake 생성 방식 | 특징 |
|-----------|-----------|---------------|------|
| **Unit Test** | ❌ **Spring 미사용** | 직접 생성 (`FakeXXX()`) | 순수 Kotlin/JUnit 5 |
| **Integration Test** | ✅ **Spring Boot 사용** | Spring Bean (`@Component` + DI) | `@SpringBootTest` |

**Spring Boot 사용 시 Integration Test 예시**:
```kotlin
// ✅ Spring Boot Context 사용
@SpringBootTest
@TestConstructor(autowireMode = AutowireMode.ALL)
class OrderPaymentSpec : BaseIntegrationTest() {

    @Autowired
    private lateinit var orderService: OrderService

    @Autowired  // Fake도 Spring Bean으로 주입
    private lateinit var paymentClient: FakePaymentClient

    @BeforeEach
    fun setup() {
        paymentClient.reset()
    }

    @Test
    fun `결제 성공 시 주문이 완료된다`() {
        // Given: Fake Bean의 동작 변경
        paymentClient.setResponse(PaymentResponse("SUCCESS"))

        // When: Spring Bean 사용
        val result = orderService.order()

        // Then
        assertThat(result.status).isEqualTo("COMPLETED")
    }
}
```

### 9.2 Context 캐싱 문제와 해결법

**문제**: 통합 테스트에서 Spring 컨텍스트를 매번 새로 띄우면 느려짐 (수십 초 소요)

**해결**: 컨텍스트를 재사용하면서 **Fake 동작만 바꾸는 패턴** 사용

#### 9.2.1 Programmable Fake 패턴

**상황**: 같은 Fake 빈이지만, 테스트마다 **반환값이 달라져야** 할 때

**핵심**: Fake 객체를 **외부에서 조종 가능하게(Programmable)** 만듭니다.

**구현 예제**:

```kotlin
// src/integrationTest/kotlin/fakes/FakePaymentClient.kt
@Component
class FakePaymentClient : PaymentClient {
    // 1. 동작 제어를 위한 상태 변수
    private var shouldFail: Boolean = false
    private var fixedResponse: PaymentResponse? = null

    // 2. 테스트에서 호출할 설정 메서드 (Test-Only API)
    fun setFailureMode(fail: Boolean) {
        this.shouldFail = fail
    }

    fun setResponse(response: PaymentResponse) {
        this.fixedResponse = response
    }

    fun reset() {
        shouldFail = false
        fixedResponse = null
    }

    // 3. 실제 인터페이스 구현
    override fun pay(amount: Long): PaymentResponse {
        if (shouldFail) throw PaymentException("결제 실패")
        return fixedResponse ?: PaymentResponse("SUCCESS_DEFAULT")
    }
}
```

**테스트에서 사용**:

```kotlin
@SpringBootTest
class OrderPaymentSpec {

    @Autowired
    private lateinit var paymentClient: FakePaymentClient  // 구체 클래스로 주입

    @BeforeEach
    fun setup() {
        paymentClient.reset()  // ⚠️ 항상 상태 초기화 필수!
    }

    @Test
    fun `결제 시스템 장애 시 주문이 거절된다`() {
        // Given: Fake의 동작을 변경
        paymentClient.setFailureMode(true)

        // When & Then
        assertThrows<PaymentException> {
            orderService.order()
        }
    }

    @Test
    fun `결제 성공 시 주문이 완료된다`() {
        // Given: 성공 응답 설정
        paymentClient.setResponse(PaymentResponse("SUCCESS"))

        // When
        val result = orderService.order()

        // Then
        assertThat(result.status).isEqualTo("COMPLETED")
    }
}
```

**장점**:
- ✅ Spring 컨텍스트 재사용 → 빠름
- ✅ 테스트마다 다른 시나리오 검증 가능
- ✅ 상태 초기화만 잘하면 격리 유지

**주의사항**:
- ⚠️ **`@BeforeEach`에서 반드시 `reset()` 호출** (테스트 격리)
- ⚠️ 병렬 테스트 실행 시 `ThreadLocal` 사용 필요

#### 9.2.2 Switchable Proxy 패턴

**상황**: **Fake와 Real을 전환**해야 할 때 (대부분 Fake, 특정 테스트만 Real)

**핵심**: 두 구현체를 모두 가진 **Proxy 빈**을 등록하고, **스위치로 전환**합니다.

**구현 예제**:

```kotlin
// src/integrationTest/kotlin/config/SwitchableMailSender.kt
@Component
class SwitchableMailSender(
    private val realSender: SmtpMailSender,  // 실제 객체
    private val fakeSender: FakeMailSender   // 가짜 객체
) : MailSender {

    private var useReal: Boolean = false

    // 모드 변경 메서드
    fun switchToReal() { useReal = true }
    fun switchToFake() { useReal = false }

    // 인터페이스 구현: 스위치 상태에 따라 위임(Delegate)
    override fun send(message: String) {
        if (useReal) {
            realSender.send(message)
        } else {
            fakeSender.send(message)
        }
    }
}
```

**Config 설정**:

```kotlin
@TestConfiguration
class IntegrationTestConfig {

    @Bean
    @Primary  // 실제 주입되는 빈은 이 Proxy
    fun mailSender(
        @Qualifier("realMailSender") real: SmtpMailSender,
        @Qualifier("fakeMailSender") fake: FakeMailSender
    ): MailSender {
        return SwitchableMailSender(real, fake)
    }
}
```

**테스트에서 사용**:

```kotlin
@SpringBootTest
class UserRegistrationSpec {

    @Autowired
    private lateinit var mailSender: SwitchableMailSender

    @BeforeEach
    fun init() {
        mailSender.switchToFake()  // 기본은 Fake (안전)
    }

    @Test
    fun `일반 회원가입은 Fake 메일 발송`() {
        // Fake 모드 (기본)
        userService.register("user@test.com")

        // Fake에서 발송 내역 확인 가능
    }

    @Test
    fun `실제 메일 서버 연동 테스트`() {
        // 이 테스트만 Real 사용
        mailSender.switchToReal()

        userService.register("user@real.com")

        // 실제 메일 서버에서 확인
    }
}
```

**장점**:
- ✅ 컨텍스트 재로딩 없이 Real/Fake 전환
- ✅ 대부분 Fake로 빠르게, 필요할 때만 Real
- ✅ 빈 교체(`@MockBean`) 대비 훨씬 빠름

**주의사항**:
- ⚠️ **병렬 테스트 실행 시 `ThreadLocal<Boolean>` 사용**:
  ```kotlin
  private val useRealThreadLocal = ThreadLocal.withInitial { false }
  ```

#### 9.2.3 패턴 선택 가이드

| 상황 | 패턴 | 이유 |
|-----|------|------|
| Fake의 반환값만 바뀜 | **Programmable Fake** | 상태 변수로 제어 |
| Fake ↔ Real 전환 필요 | **Switchable Proxy** | 델리게이트 패턴 |
| 모든 테스트가 Fake만 사용 | 일반 Fake 빈 | 패턴 불필요 |
| 병렬 실행 필수 | ThreadLocal 추가 | 스레드별 격리 |

### 9.3 @MockBean 사용 시 주의사항

**문제**: `@MockBean`을 사용하면 **Spring Context가 매번 재로딩**됩니다.

**❌ DON'T (금지)**:
```kotlin
@SpringBootTest
@MockBean(PaymentClient::class)  // ← 컨텍스트 재로딩 발생!
class OrderSpec {
    // ...
}
```

**✅ DO (권장)**:
```kotlin
@SpringBootTest
class OrderSpec : BaseIntegrationTest() {

    @Autowired
    private lateinit var paymentClient: FakePaymentClient  // Programmable Fake 사용

    @BeforeEach
    fun setup() {
        paymentClient.reset()  // 상태만 초기화
    }
}
```

**원칙**:
- 개별 테스트 클래스에서 `@MockBean` 사용 금지 → **캐시 깨짐**
- 개별 테스트 클래스에서 `@TestConfiguration` 사용 금지 → **새 컨텍스트 로딩**
- 테스트마다 다른 `@ActiveProfiles` 금지 → **컨텍스트 분리됨**

### 9.4 통합 테스트 실행 전략

**기본 원칙**: Spring Boot 통합 테스트는 **순차 실행(Serial Execution)**을 기본으로 합니다.

| 항목 | 전략 | 이유 |
|-----|------|------|
| **실행 모드** | 순차 실행 (Serial) | 공유 DB에서 데드락/데이터 간섭 방지 |
| **속도 확보** | Spring Context 재사용 | 스레드 병렬화 대신 컨텍스트 캐싱 |
| **확장 전략** | Test Sharding / Gradle Fork | CI 환경에서 속도 임계치 초과 시 |

**설정 예시** (build.gradle.kts):
```kotlin
tasks.named<Test>("integrationTest") {
    // 순차 실행 강제
    maxParallelForks = 1

    // Context 캐싱 최적화
    systemProperty("spring.test.context.cache.maxSize", "1")
}
```

**컨텍스트 캐싱 유지**:
- 모든 통합 테스트는 `BaseIntegrationTest`를 상속받아 **단일 설정** 공유
- 동작 제어는 **Programmable Fake** (setter로 변경)
- Real/Fake 전환은 **Switchable Proxy** 패턴
- 상태 있는 Fake는 `@BeforeEach`에서 `reset()` 호출

**데이터 격리 전략**:
```kotlin
@Test
fun `주문 생성 시 재고가 감소한다`() {
    // ✅ 고유 ID 생성으로 데이터 격리
    val shopId = "SHOP-${UUID.randomUUID()}"
    val productId = "PROD-${UUID.randomUUID()}"

    val shop = shopRepository.save(Shop(id = shopId, name = "테스트 상점"))
    val product = productRepository.save(
        Product(id = productId, shopId = shopId, stock = 10)
    )

    // When
    orderService.createOrder(productId, quantity = 3)

    // Then: 내가 만든 데이터만 검증
    val updatedProduct = productRepository.findById(productId)
    assertThat(updatedProduct.stock).isEqualTo(7)
}
```

---

## 10. 체크리스트

### Unit Test (`*Test.kt`) 작성 시

- [ ] **Fake 객체**를 사용했는가? (Mock은 예외만)
- [ ] **상태 검증**(assert state)을 사용했는가? (verify call 금지)
- [ ] 테스트 이름이 명확한가? (Given-When-Then 이해 가능)
- [ ] 하드코딩된 ID가 없는가? (UUID, Timestamp 사용)
- [ ] 50줄 이하인가?
- [ ] 독립적으로 실행되는가? (순서 의존성 없음)
- [ ] 외부 의존성(DB, HTTP, 파일 등)을 사용하지 않았는가?

### Integration Test (`*Spec.kt`) 작성 시

- [ ] 실제 외부 시스템(DB, HTTP 등)과 연동했는가?
- [ ] 주요 시나리오(Happy Path)를 테스트했는가?
- [ ] DB 제약조건/트랜잭션을 검증했는가?
- [ ] 외부 API는 Mock/Stub을 사용했는가?
- [ ] 데이터 격리를 위해 고유 ID를 사용했는가?

### Spring Boot 사용 시 추가 체크리스트

- [ ] `@MockBean`을 사용하지 않았는가? (Context 재로딩 방지)
- [ ] Programmable Fake 또는 Switchable Proxy를 사용했는가?
- [ ] `@BeforeEach`에서 Fake 상태를 `reset()`했는가?
- [ ] 모든 테스트가 `BaseIntegrationTest`를 상속받는가?

### 테스트 리뷰 시

- [ ] 테스트가 실제로 검증하려는 것을 검증하는가?
- [ ] 실패할 때 원인을 명확히 알 수 있는가?
- [ ] 빠르게 실행되는가? (Unit Test: < 100ms, Integration Test: < 5s)
- [ ] Flaky하지 않은가? (10번 실행 시 10번 성공)

---

## 11. 프로젝트 구조 및 파일 명명 규칙

| 구분 | 디렉토리 (Source Set) | 파일 접미어 | 외부 의존성 | 주요 목적 |
|-----|-------------|----------|----------|---------|
| **Unit Test** | `src/test/kotlin` | `**Test.kt` | ❌ **없음** | 도메인 로직 검증 (Fake 직접 생성) |
| **Integration Test** | `src/integrationTest/kotlin` | `**Spec.kt` | ✅ **있음** | 시스템 흐름 및 인프라 연동 검증 |

### 11.1 디렉토리 구조

```
src/
├── test/kotlin/                       # 단위 테스트 (*Test.kt) - 외부 의존성 없음
│   ├── api/
│   │   ├── UserApiTest.kt            # Unit Test
│   │   ├── OrderApiTest.kt
│   │   └── ...
│   ├── service/                      # 비즈니스 로직 테스트
│   │   ├── OrderCalculationTest.kt
│   │   └── ...
│   ├── fakes/                        # Fake 객체 구현체 (Unit Test용)
│   │   ├── FakeUserRepository.kt     # 직접 생성
│   │   ├── FakeOrderService.kt
│   │   └── ...
│   ├── utils/                        # 테스트 유틸
│   │   ├── UnitTestBase.kt
│   │   ├── MockHttpClient.kt
│   │   ├── JsonResponseLoader.kt
│   │   └── AssertionHelpers.kt
│   └── resources/responses/          # Mock HTTP 응답 파일
│       ├── users/
│       ├── orders/
│       └── errors/
│
├── integrationTest/kotlin/           # 통합 테스트 (*Spec.kt) - 외부 의존성 있음
│   ├── api/
│   │   ├── UserApiSpec.kt           # Integration Test
│   │   ├── OrderApiSpec.kt
│   │   └── ...
│   ├── flows/                        # 시스템 흐름 테스트
│   │   └── ...
│   ├── fakes/                        # Integration Test용 Fake
│   │   ├── FakePaymentClient.kt     # (Spring Boot 사용 시 @Component)
│   │   └── ...
│   └── utils/
│       └── IntegrationTestBase.kt   # BaseIntegrationTest
│
└── docs/
    └── test-writing-principles.md (이 문서)
```

**디렉토리별 특징**:
- `src/test/kotlin/`: 외부 의존성 없음. Fake 객체를 직접 생성
- `src/integrationTest/kotlin/`: 실제 DB, HTTP 등 외부 시스템 연동

---

## 12. 주요 유틸 클래스

### UnitTestBase

모든 단위 테스트의 베이스 클래스:

```kotlin
abstract class UnitTestBase {
    // Mock HTTP 클라이언트 설정
    protected fun mockHttpClient(response: String): HttpClient

    // Mock 응답 로딩
    protected fun loadMockResponse(fileName: String): String
    protected fun mockErrorResponse(statusCode: HttpStatusCode): String

    // 테스트 실행 헬퍼
    protected fun unitTest(
        timeout: Duration = 10.seconds,
        block: suspend () -> Unit
    ) = runTest(timeout) { block() }
}
```

### Assertion Helpers

```kotlin
// 커스텀 assertion 예제
fun User.assertUser(
    name: String? = null,
    email: String? = null,
    // ...
) {
    if (name != null) assertThat(this.name).isEqualTo(name)
    if (email != null) assertThat(this.email).isEqualTo(email)
    // ...
}
```

---

## 요약

### 🎯 Classical TDD (State-based Testing) 핵심 원칙

1. **Fake 객체 우선**: Repository, Service 등 내부 의존성은 반드시 Fake 구현
    - Mock 사용은 **Fake 구현 비용 >> Mock 사용 비용**인 경우에만 예외 허용
    - 반드시 주석으로 Mock 사용 이유 명시

2. **상태 검증**: 메서드 호출 여부(verify)가 아니라 **실행 후 상태 변화를 assert**
    - ✅ `assertThat(fakeRepository.findAll()).hasSize(1)`
    - ❌ `verify { mockRepository.save(any()) }`

3. **리팩토링 내성**: 구현 세부 사항 변경 시 테스트가 깨지지 않아야 함
    - Fake: 상태만 검증 → 변경 영향 최소
    - Mock: 호출 검증 → 구현 변경 시 테스트 깨짐

4. **테스트 = 스펙 문서**
    - 테스트 이름만으로 요구사항 이해 가능
    - Spec 스타일(`@Nested`, `@DisplayName`) 사용

### 파일 명명 규칙 및 외부 의존성

| 구분 | 파일 이름 | 외부 의존성 | 의존성 생성 | 검증 방식 |
|-----|---------|-----------|---------|---------|
| **Unit Test** | `*Test.kt` | ❌ **없음** | **Fake 객체** (직접 생성) | **상태 검증** |
| **Integration Test** | `*Spec.kt` | ✅ **있음** | 실제 환경 또는 Testcontainers | 실제 데이터 검증 |

**핵심 차이**:
- **Unit Test**: 외부 의존성 없이 순수 Kotlin/JUnit 5로 테스트. Fake 객체를 직접 생성.
- **Integration Test**: 실제 외부 시스템(DB, HTTP, 파일, 메시지큐 등)과 연동.

### Spring Boot 사용 시 추가 고려사항

- **Programmable Fake**: 같은 Bean을 테스트마다 다르게 동작시킬 때
- **Switchable Proxy**: Fake/Real을 전환해야 할 때
- **@MockBean 금지**: Context 재로딩 방지
- **순차 실행**: 공유 DB 문제 해결

### 권장 도구/라이브러리

- **프레임워크**: JUnit 5 (Jupiter)
- **Assertion**: AssertJ, Kotest (상태 검증에 최적화)
- **Mock (예외)**: MockK (외부 API, 호출 검증만)
- **Async**: kotlinx-coroutines-test
- **Fixture**: Mother 패턴
- **DB Testing**: Testcontainers

### 권장 프로젝트 구조

- Fake 구현 예제: `src/test/kotlin/{package}/fakes/`
- Unit Test 예제: `src/test/kotlin/{package}/api/UserServiceTest.kt`
- Integration Test 예제: `src/integrationTest/kotlin/{package}/api/UserApiSpec.kt`
- Mock 응답 파일: `src/test/resources/responses/`

---

**마지막 업데이트**: 2025-12-02
**버전**: 2.0 (범용화)
**대상**: AI 에이전트 기반 테스트 작성 가이드 (Kotlin + JUnit 5)
