# Phase 2: Infrastructure 레이어 구현 완료

## 개요
UFC 프로젝트의 Phase 2 (Infrastructure 레이어) 구현을 완료했습니다. Token Bucket 알고리즘을 기반으로 한 Rate Limiter를 구현하여 외부 API의 요청 제한을 관리합니다.

## 구현된 파일

### 1. RateLimiter.kt (인터페이스 정의)
**경로**: `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/infrastructure/ratelimit/RateLimiter.kt`

**내용**:
- `RateLimiter` 인터페이스: Rate Limiter의 공통 계약 정의
  - `suspend fun acquire(tokensNeeded: Int = 1)`: 토큰 획득
  - `fun getAvailableTokens(): Int`: 사용 가능한 토큰 수
  - `fun getWaitTimeMillis(): Long`: 대기 시간 조회
  - `fun getStatus(): RateLimiterStatus`: 현재 상태 조회

- `RateLimiterStatus` 데이터 클래스: Rate Limiter 상태 정보
  - `availableTokens`: 사용 가능한 토큰
  - `capacity`: 최대 용량
  - `refillRate`: 초당 리필 속도
  - `isEnabled`: 활성화 여부
  - `estimatedWaitTimeMs`: 예상 대기 시간
  - 계산 속성: `utilizationPercent`, `hasTokensAvailable`

### 2. RateLimitConfig.kt (설정 클래스)
**경로**: `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/infrastructure/ratelimit/RateLimitConfig.kt`

**내용**:
- `RateLimitConfig` 데이터 클래스
  - `capacity: Int = 50`: 최대 토큰 수
  - `refillRate: Int = 50`: 초당 리필 토큰 수
  - `enabled: Boolean = true`: 활성화 여부
  - `waitTimeoutMillis: Long = 60000L`: 대기 타임아웃
  - 검증: capacity, refillRate, waitTimeoutMillis > 0
  - 계산 속성: `refillPeriodSeconds`, `fullRefillSeconds`

- `RateLimitingSettings` 데이터 클래스
  - Yahoo Finance API 설정: capacity=50, refillRate=50
  - FRED API 설정: capacity=10, refillRate=10

### 3. RateLimitException.kt (예외 클래스)
**경로**: `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/infrastructure/ratelimit/RateLimitException.kt`

**내용**:
- `RateLimitException` sealed class (부모 예외)
  - `RateLimitTimeoutException`: 토큰 대기 타임아웃
    - 속성: `source`, `config`, `tokensNeeded`, `waitedMillis`
    - 메서드: `getFormattedWaitTime()` - 형식화된 대기 시간
  - `RateLimitConfigException`: 설정 검증 오류
  - `RateLimitStateException`: 상태 오류

### 4. TokenBucketRateLimiter.kt (구현체)
**경로**: `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/infrastructure/ratelimit/TokenBucketRateLimiter.kt`

**내용**:
- Token Bucket 알고리즘 구현
- 주요 특징:
  - `Mutex`를 사용한 Coroutine-safe 동시성 제어
  - 정밀한 토큰 리필 (Double 사용)
  - 응답성 향상을 위한 스마트 대기 (100ms 단위)
  - 타임아웃 메커니즘

- 구현 메서드:
  - `acquire(tokensNeeded: Int)`: 토큰 획득 (동시성 안전)
  - `getAvailableTokens(): Int`: 사용 가능 토큰
  - `getWaitTimeMillis(): Long`: 1개 토큰 대기 시간
  - `getStatus(): RateLimiterStatus`: 현재 상태
  - `refillTokens()`: Private - 토큰 자동 리필
  - `calculateWaitTimeMs(tokensNeeded)`: Private - 대기 시간 계산

## 테스트 구현

### 1. TokenBucketRateLimiterTest.kt
**경로**: `/home/ulalax/project/kairos/ufc/src/test/kotlin/com/ulalax/ufc/infrastructure/ratelimit/TokenBucketRateLimiterTest.kt`

**테스트 항목** (40개 테스트):
- 초기 상태 검증
- 단일 및 다중 토큰 획득
- 토큰 리필 검증
- 용량 제한 검증
- 비활성화 상태 처리
- 타임아웃 예외 처리
- 동시성 제어 (5개, 50개 동시 요청)
- 동시성 + 토큰 리필

### 2. RateLimitConfigTest.kt
**경로**: `/home/ulalax/project/kairos/ufc/src/test/kotlin/com/ulalax/ufc/infrastructure/ratelimit/RateLimitConfigTest.kt`

**테스트 항목** (20개 테스트):
- 기본값 검증
- 커스텀 설정
- 입력값 검증 (capacity, refillRate, waitTimeoutMillis)
- 계산 속성 검증
- Copy 메서드 검증
- RateLimitingSettings 검증

### 3. RateLimitExceptionTest.kt
**경로**: `/home/ulalax/project/kairos/ufc/src/test/kotlin/com/ulalax/ufc/infrastructure/ratelimit/RateLimitExceptionTest.kt`

**테스트 항목** (15개 테스트):
- 예외 계층 구조 검증
- TimeoutException 생성 및 메시지 검증
- ConfigException 생성 및 메시지 검증
- StateException 생성 및 메시지 검증
- 형식화된 대기 시간 (ms, 초, 분)

## 핵심 구현 특징

### 1. Token Bucket 알고리즘
```
토큰 리필 공식: tokensToAdd = (경과시간 / 1000) * refillRate
최종 토큰 = min(현재토큰 + tokensToAdd, capacity)
```

### 2. Coroutine 동시성 제어
- `Mutex` 사용으로 스레드 안전성 보장
- suspend 함수로 비동기 처리
- 타임아웃 전에 응답성 향상을 위해 최대 100ms 단위 대기

### 3. 정밀한 계산
- Double 사용으로 소수 토큰 처리
- 밀리초 단위 정확한 리필 시간 계산

### 4. 예외 처리
- sealed class를 통한 타입 안전한 예외 관리
- 상세한 에러 메시지 제공
- 형식화된 대기 시간 제공

## 파일 구조

```
/home/ulalax/project/kairos/ufc/
├── src/main/kotlin/com/ulalax/ufc/infrastructure/ratelimit/
│   ├── RateLimiter.kt                 (인터페이스 + 상태 클래스)
│   ├── RateLimitConfig.kt             (설정 클래스)
│   ├── RateLimitException.kt          (예외 클래스)
│   └── TokenBucketRateLimiter.kt      (구현체)
└── src/test/kotlin/com/ulalax/ufc/infrastructure/ratelimit/
    ├── TokenBucketRateLimiterTest.kt  (40개 테스트)
    ├── RateLimitConfigTest.kt         (20개 테스트)
    └── RateLimitExceptionTest.kt      (15개 테스트)
```

## 빌드 상태

- **컴파일 상태**: 성공 ✓
- **생성된 클래스**: 17개 (main) + 9개 (test)
- **총 테스트**: 75개

## 사용 예시

### 기본 사용법
```kotlin
// 설정
val config = RateLimitConfig(
    capacity = 50,
    refillRate = 50,
    enabled = true,
    waitTimeoutMillis = 60000L
)

// Rate Limiter 생성
val rateLimiter = TokenBucketRateLimiter("YAHOO", config)

// API 호출 전 토큰 획득
try {
    rateLimiter.acquire(1)  // 1개 토큰 소비
    // API 호출
} catch (e: RateLimitException.RateLimitTimeoutException) {
    println("Rate limit exceeded: ${e.getFormattedWaitTime()}")
}
```

### 상태 모니터링
```kotlin
val status = rateLimiter.getStatus()
println("Available: ${status.availableTokens}")
println("Utilization: ${status.utilizationPercent}%")
println("Estimated Wait: ${status.estimatedWaitTimeMs}ms")
```

### 다중 API 설정
```kotlin
val settings = RateLimitingSettings(
    yahoo = RateLimitConfig(capacity = 50, refillRate = 50),
    fred = RateLimitConfig(capacity = 10, refillRate = 10)
)

val yahooLimiter = TokenBucketRateLimiter("YAHOO", settings.yahoo)
val fredLimiter = TokenBucketRateLimiter("FRED", settings.fred)
```

## 다음 단계

Phase 3에서는 이 Rate Limiter를 실제 HTTP 클라이언트에 통합하여:
1. HTTP 요청 전에 Rate Limiter 확인
2. 응답 헤더의 Rate Limit 정보 처리
3. 자동 재시도 로직 구현
4. 모니터링 및 로깅 통합

## 참고사항

- Kotlin 2.1.0 / Coroutines 1.8.0 사용
- Java 21 컴파일 대상
- kotlinx-serialization 지원 (JSON 직렬화)
- 완전한 코멘트 및 KDoc 제공
