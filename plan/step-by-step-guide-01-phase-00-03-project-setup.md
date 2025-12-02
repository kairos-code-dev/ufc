# UFC Implementation Steps - Haiku 모델 실행 가이드

## 문서 정보
- **버전**: 1.0.0
- **최종 작성일**: 2025-12-02
- **목적**: Haiku 모델이 실행할 수 있는 세분화된 단계별 구현 가이드
- **대상**: Claude Haiku Model

---

## 이 문서의 사용법

이 문서는 UFC 프로젝트를 처음부터 구현하는 데 필요한 모든 단계를 작은 작업 단위로 나누어 제공합니다.

**실행 방법:**
1. 각 Phase를 순서대로 진행
2. 각 Step은 독립적으로 실행 가능
3. 각 Step 완료 후 반드시 체크포인트 확인
4. 테스트가 있는 경우 반드시 실행하여 검증

**체크포인트 표기:**
- ✅ **완료 조건**: 이 단계가 완료되었다고 판단할 수 있는 기준
- 🧪 **테스트**: 구현이 정상 동작하는지 확인하는 방법
- 📝 **산출물**: 생성되어야 하는 파일 목록

---

## Phase 0: 프로젝트 초기 셋업

### Step 0.1: build.gradle.kts 작성

**작업 내용:**
- Gradle 프로젝트 설정 파일 작성
- 필요한 의존성 추가
- Kotlin 2.1.0, JDK 21 설정

**파일 경로:** `/home/ulalax/project/kairos/ufc/build.gradle.kts`

**파일 내용:**
```kotlin
plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    `java-library`
    `maven-publish`
}

group = "com.ulalax"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // Ktor Client
    implementation("io.ktor:ktor-client-core:3.0.1")
    implementation("io.ktor:ktor-client-okhttp:3.0.1")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.1")
    implementation("io.ktor:ktor-client-logging:3.0.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // HTML Parsing
    implementation("org.jsoup:jsoup:1.18.1")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("ch.qos.logback:logback-classic:1.5.12")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.ktor:ktor-client-mock:3.0.1")
    testImplementation("org.assertj:assertj-core:3.27.0")
}

// SourceSets for LiveTest
sourceSets {
    create("liveTest") {
        kotlin {
            srcDir("src/liveTest/kotlin")
        }
        resources {
            srcDir("src/liveTest/resources")
        }
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

// Live Test Configuration
val liveTestImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

dependencies {
    liveTestImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    liveTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    liveTestImplementation("com.google.code.gson:gson:2.11.0")
}

// Test Tasks
tasks.test {
    useJUnitPlatform()
}

val liveTest = tasks.register<Test>("liveTest") {
    description = "Runs live tests that make actual API calls"
    group = "verification"

    testClassesDirs = sourceSets["liveTest"].output.classesDirs
    classpath = sourceSets["liveTest"].runtimeClasspath

    useJUnitPlatform {
        includeTags("live")
    }

    systemProperty("record.responses",
        project.findProperty("record.responses")?.toString() ?: "true"
    )

    timeout.set(java.time.Duration.ofMinutes(30))
    maxParallelForks = 1
    outputs.upToDateWhen { false }
}

tasks.check {
    dependsOn(liveTest)
}

kotlin {
    jvmToolchain(21)
}
```

✅ **완료 조건:**
- build.gradle.kts 파일이 작성됨
- 모든 의존성이 올바르게 추가됨

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/build.gradle.kts`

---

### Step 0.2: 디렉토리 구조 생성

**작업 내용:**
- 프로젝트 디렉토리 구조 생성

**실행 명령:**
```bash
cd /home/ulalax/project/kairos/ufc

# Main source
mkdir -p src/main/kotlin/com/ulalax/ufc/{client,api,internal/{stock,etf,macro,search,yahoo,fred},model/{common,stock,etf,macro,search},exception,infrastructure/{http,serialization,cache,ratelimit},utils}
mkdir -p src/main/resources

# Test source
mkdir -p src/test/kotlin/com/ulalax/ufc/{source/{yahoo,fred},integration,utils}
mkdir -p src/test/resources/responses/{yahoo/{etf,ticker,chart},fred/{series,search}}

# Live Test source
mkdir -p src/liveTest/kotlin/com/ulalax/ufc/{live/{yahoo/{etf,ticker,search,chart},fred/{series,search,indicators}},utils}
mkdir -p src/liveTest/resources/responses/{yahoo/{etf/{top_holdings,sector_weightings,asset_allocation,fund_profile,equity_holdings,bond_holdings},ticker/{history,dividends,splits,financials,info,recommendations},chart/{intraday,daily,adjusted},search/{basic,screener}},fred/{series,series_info,vintage,search/{category,release},indicators}}

# Plan documents
mkdir -p plan
```

✅ **완료 조건:**
- 모든 디렉토리가 생성됨
- `ls -la src/` 명령으로 디렉토리 구조 확인 가능

📝 **산출물:**
- 완전한 디렉토리 구조

---

### Step 0.3: .gitignore 작성

**파일 경로:** `/home/ulalax/project/kairos/ufc/.gitignore`

**파일 내용:**
```gitignore
# Gradle
.gradle/
build/
gradle-app.setting
!gradle-wrapper.jar
.gradletasknamecache

# Kotlin
*.class
*.jar
*.war
*.ear
*.kt.swp

# IDE
.idea/
*.iml
*.ipr
*.iws
.vscode/
.DS_Store

# Local properties
local.properties

# Log files
*.log

# Test recordings (optional)
# src/liveTest/resources/responses/

# Secrets
*.key
*.pem
```

✅ **완료 조건:**
- .gitignore 파일이 작성됨

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/.gitignore`

---

### Step 0.4: local.properties 템플릿 작성

**파일 경로:** `/home/ulalax/project/kairos/ufc/local.properties.template`

**파일 내용:**
```properties
# FRED API Key (필수)
# https://fred.stlouisfed.org/docs/api/api_key.html 에서 발급
FRED_API_KEY=your_fred_api_key_here
```

✅ **완료 조건:**
- local.properties.template 파일이 작성됨

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/local.properties.template`

---

## Phase 1: 공통 모델 및 예외 시스템

### Step 1.1: ErrorCode enum 작성

**작업 내용:**
- ErrorCode enum 정의
- 에러 코드 체계 구현

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/exception/ErrorCode.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.exception

/**
 * UFC 에러 코드
 *
 * 모든 예외는 에러 코드와 메시지로 구성됩니다.
 */
enum class ErrorCode(val code: Int, val message: String) {

    // ================================
    // 1000번대: 네트워크 오류
    // ================================

    /** 네트워크 연결 실패 */
    NETWORK_CONNECTION_FAILED(1001, "네트워크 연결에 실패했습니다"),

    /** 네트워크 타임아웃 */
    NETWORK_TIMEOUT(1002, "네트워크 요청 시간이 초과되었습니다"),

    /** HTTP 요청 실패 */
    HTTP_REQUEST_FAILED(1003, "HTTP 요청이 실패했습니다"),

    /** HTTP 오류 응답 (4xx, 5xx) */
    HTTP_ERROR_RESPONSE(1004, "HTTP 요청이 오류 응답을 반환했습니다"),

    /** SSL/TLS 오류 */
    SSL_ERROR(1005, "SSL/TLS 연결에 실패했습니다"),

    // ================================
    // 2000번대: 인증 오류
    // ================================

    /** 인증 실패 */
    AUTH_FAILED(2001, "인증에 실패했습니다"),

    /** Cookie 획득 실패 */
    COOKIE_ACQUISITION_FAILED(2002, "Cookie 획득에 실패했습니다"),

    /** Crumb 획득 실패 */
    CRUMB_ACQUISITION_FAILED(2003, "Crumb 획득에 실패했습니다"),

    /** Crumb 만료 */
    CRUMB_EXPIRED(2004, "Crumb이 만료되었습니다"),

    /** API Key 누락 */
    MISSING_API_KEY(2005, "API Key가 제공되지 않았습니다"),

    /** API Key 무효 */
    INVALID_API_KEY(2006, "API Key가 유효하지 않습니다"),

    // ================================
    // 3000번대: Rate Limiting 오류
    // ================================

    /** Rate Limit 초과 */
    RATE_LIMITED(3001, "API 호출 제한을 초과했습니다"),

    /** Too Many Requests (429) */
    TOO_MANY_REQUESTS(3002, "요청이 너무 많습니다"),

    // ================================
    // 4000번대: 데이터 오류
    // ================================

    /** 데이터 없음 */
    NO_DATA_AVAILABLE(4001, "요청한 데이터가 존재하지 않습니다"),

    /** 심볼 찾을 수 없음 */
    INVALID_SYMBOL(4002, "유효하지 않은 심볼입니다"),

    /** Series ID 무효 */
    INVALID_SERIES_ID(4003, "유효하지 않은 Series ID입니다"),

    /** 빈 응답 */
    EMPTY_RESPONSE(4004, "응답 데이터가 비어있습니다"),

    // ================================
    // 5000번대: 파싱 오류
    // ================================

    /** JSON 파싱 실패 */
    JSON_PARSE_ERROR(5001, "JSON 파싱에 실패했습니다"),

    /** XML 파싱 실패 */
    XML_PARSE_ERROR(5002, "XML 파싱에 실패했습니다"),

    /** 직렬화 오류 */
    SERIALIZATION_ERROR(5003, "데이터 직렬화에 실패했습니다"),

    /** 잘못된 데이터 형식 */
    INVALID_DATA_FORMAT(5004, "데이터 형식이 올바르지 않습니다"),

    /** 필수 필드 누락 */
    REQUIRED_FIELD_MISSING(5005, "필수 필드가 누락되었습니다"),

    // ================================
    // 6000번대: 파라미터 오류
    // ================================

    /** 잘못된 파라미터 */
    INVALID_PARAMETER(6001, "파라미터가 올바르지 않습니다"),

    /** 잘못된 날짜 범위 */
    INVALID_DATE_RANGE(6002, "날짜 범위가 올바르지 않습니다"),

    /** 잘못된 Interval */
    INVALID_INTERVAL(6003, "Interval이 올바르지 않습니다"),

    /** 잘못된 Period */
    INVALID_PERIOD(6004, "Period가 올바르지 않습니다"),

    // ================================
    // 7000번대: 서버 오류
    // ================================

    /** 서버 오류 (5xx) */
    SERVER_ERROR(7001, "서버에서 오류가 발생했습니다"),

    /** 서비스 이용 불가 */
    SERVICE_UNAVAILABLE(7002, "서비스를 이용할 수 없습니다"),

    // ================================
    // 9000번대: 기타 오류
    // ================================

    /** 알 수 없는 오류 */
    UNKNOWN_ERROR(9999, "알 수 없는 오류가 발생했습니다");

    override fun toString(): String = "[$code] $message"
}

/**
 * 재시도 가능 여부
 */
fun ErrorCode.isRetryable(): Boolean {
    return this in setOf(
        ErrorCode.NETWORK_TIMEOUT,
        ErrorCode.NETWORK_CONNECTION_FAILED,
        ErrorCode.TOO_MANY_REQUESTS,
        ErrorCode.SERVER_ERROR,
        ErrorCode.SERVICE_UNAVAILABLE
    )
}
```

✅ **완료 조건:**
- ErrorCode.kt 파일이 작성됨
- 모든 에러 코드가 정의됨
- isRetryable() 확장 함수가 작성됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/exception/ErrorCode.kt`

---

### Step 1.2: UFCException 클래스 작성

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/exception/UFCException.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.exception

/**
 * UFC 라이브러리의 통합 예외 클래스
 *
 * ErrorCode 기반 예외 처리 시스템을 제공합니다.
 */
class UFCException(
    val errorCode: ErrorCode,
    message: String = errorCode.message,
    cause: Throwable? = null,
    val metadata: Map<String, Any> = emptyMap()
) : Exception(message, cause) {

    /** 에러 코드 번호 */
    val code: Int get() = errorCode.code

    /** 메타데이터 조회 헬퍼 */
    inline fun <reified T> getMeta(key: String): T? = metadata[key] as? T

    override fun toString(): String {
        val metaStr = if (metadata.isNotEmpty()) {
            ", metadata=$metadata"
        } else {
            ""
        }
        return "UFCException(errorCode=$errorCode$metaStr)"
    }
}
```

✅ **완료 조건:**
- UFCException.kt 파일이 작성됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/exception/UFCException.kt`

---

### Step 1.3: 공통 모델 - Period, Interval, DataFrequency

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/model/common/Period.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.model.common

/**
 * 기간 (Period)
 */
enum class Period(val value: String) {
    OneDay("1d"),
    FiveDays("5d"),
    OneMonth("1mo"),
    ThreeMonths("3mo"),
    SixMonths("6mo"),
    OneYear("1y"),
    TwoYears("2y"),
    FiveYears("5y"),
    TenYears("10y"),
    YearToDate("ytd"),
    Max("max")
}
```

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/model/common/Interval.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.model.common

/**
 * 간격 (Interval)
 */
enum class Interval(val value: String) {
    OneMinute("1m"),
    TwoMinutes("2m"),
    FiveMinutes("5m"),
    FifteenMinutes("15m"),
    ThirtyMinutes("30m"),
    OneHour("1h"),
    OneDay("1d"),
    FiveDays("5d"),
    OneWeek("1wk"),
    OneMonth("1mo"),
    ThreeMonths("3mo")
}
```

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/model/common/DataFrequency.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.model.common

/**
 * 데이터 주기 (FRED)
 */
enum class DataFrequency(val value: String) {
    Daily("d"),
    Weekly("w"),
    Biweekly("bw"),
    Monthly("m"),
    Quarterly("q"),
    Semiannual("sa"),
    Annual("a")
}
```

✅ **완료 조건:**
- Period.kt, Interval.kt, DataFrequency.kt 파일이 작성됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/model/common/Period.kt`
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/model/common/Interval.kt`
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/model/common/DataFrequency.kt`

---

## Phase 2: Infrastructure 레이어

### Step 2.1: RateLimiter 인터페이스

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/infrastructure/ratelimit/RateLimiter.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.infrastructure.ratelimit

/**
 * Rate Limiter 인터페이스
 */
interface RateLimiter {
    /**
     * 주어진 개수의 토큰을 소비합니다.
     */
    suspend fun acquire(tokensNeeded: Int = 1)

    /**
     * 현재 사용 가능한 토큰 개수를 반환합니다.
     */
    fun getAvailableTokens(): Int

    /**
     * 1개의 토큰을 획득하는 데 필요한 대기 시간(밀리초)을 반환합니다.
     */
    fun getWaitTimeMillis(): Long

    /**
     * Rate Limiter의 현재 상태를 반환합니다.
     */
    fun getStatus(): RateLimiterStatus
}

/**
 * Rate Limiter의 현재 상태
 */
data class RateLimiterStatus(
    val availableTokens: Int,
    val capacity: Int,
    val refillRate: Int,
    val isEnabled: Boolean,
    val estimatedWaitTimeMs: Long
)
```

✅ **완료 조건:**
- RateLimiter.kt 파일이 작성됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/infrastructure/ratelimit/RateLimiter.kt`

---

### Step 2.2: RateLimitConfig 및 RateLimitException

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/infrastructure/ratelimit/RateLimitConfig.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.infrastructure.ratelimit

/**
 * Rate Limiting 설정 데이터 클래스
 */
data class RateLimitConfig(
    val capacity: Int = 50,
    val refillRate: Int = 50,
    val enabled: Boolean = true,
    val waitTimeoutMillis: Long = 60000L
) {
    init {
        require(capacity > 0) { "capacity must be greater than 0" }
        require(refillRate > 0) { "refillRate must be greater than 0" }
        require(waitTimeoutMillis > 0) { "waitTimeoutMillis must be greater than 0" }
    }
}

/**
 * 모든 데이터 소스의 Rate Limiting 설정
 */
data class RateLimitingSettings(
    val yahoo: RateLimitConfig = RateLimitConfig(
        capacity = 50,
        refillRate = 50
    ),
    val fred: RateLimitConfig = RateLimitConfig(
        capacity = 10,
        refillRate = 10
    )
)
```

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/infrastructure/ratelimit/RateLimitException.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.infrastructure.ratelimit

/**
 * Rate Limiting 관련 기본 예외 클래스
 */
sealed class RateLimitException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/**
 * Rate limiting 대기 시간이 초과되었을 때 발생하는 예외
 */
class RateLimitTimeoutException(
    val source: String,
    val config: RateLimitConfig,
    message: String = "Rate limit timeout exceeded for $source"
) : RateLimitException(message)
```

✅ **완료 조건:**
- RateLimitConfig.kt, RateLimitException.kt 파일이 작성됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- 2개 파일

---

### Step 2.3: TokenBucketRateLimiter 구현

**작업 내용:**
- Token Bucket 알고리즘 기반 Rate Limiter 구현

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/infrastructure/ratelimit/TokenBucketRateLimiter.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.infrastructure.ratelimit

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Token Bucket 알고리즘 기반 Rate Limiter 구현
 */
class TokenBucketRateLimiter(private val config: RateLimitConfig) : RateLimiter {
    private val lock = Mutex()
    private var tokens: Double = config.capacity.toDouble()
    private var lastRefillTime: Long = System.currentTimeMillis()

    override suspend fun acquire(tokensNeeded: Int) {
        if (!config.enabled) {
            return
        }

        require(tokensNeeded > 0) { "tokensNeeded must be greater than 0" }

        val startTime = System.currentTimeMillis()

        while (true) {
            lock.withLock {
                refillTokens()

                if (tokens >= tokensNeeded) {
                    tokens -= tokensNeeded
                    return
                }

                val waitTimeMs = calculateWaitTimeMs(tokensNeeded)
                val elapsedTime = System.currentTimeMillis() - startTime
                if (elapsedTime + waitTimeMs > config.waitTimeoutMillis) {
                    throw RateLimitTimeoutException(
                        source = "Unknown",
                        config = config,
                        message = "Rate limit timeout exceeded after ${elapsedTime}ms"
                    )
                }
            }

            delay(10)
        }
    }

    override fun getAvailableTokens(): Int = tokens.toInt()

    override fun getWaitTimeMillis(): Long = calculateWaitTimeMs(1)

    override fun getStatus(): RateLimiterStatus {
        return RateLimiterStatus(
            availableTokens = getAvailableTokens(),
            capacity = config.capacity,
            refillRate = config.refillRate,
            isEnabled = config.enabled,
            estimatedWaitTimeMs = getWaitTimeMillis()
        )
    }

    private fun refillTokens() {
        val now = System.currentTimeMillis()
        val elapsedSeconds = (now - lastRefillTime) / 1000.0
        val tokensToAdd = elapsedSeconds * config.refillRate
        tokens = minOf(tokens + tokensToAdd, config.capacity.toDouble())
        lastRefillTime = now
    }

    private fun calculateWaitTimeMs(tokensNeeded: Int): Long {
        if (config.refillRate == 0) {
            return Long.MAX_VALUE
        }
        val tokensShortage = (tokensNeeded - tokens).coerceAtLeast(0.0)
        val secondsNeeded = tokensShortage / config.refillRate
        return (secondsNeeded * 1000).toLong().coerceAtLeast(1L)
    }
}
```

✅ **완료 조건:**
- TokenBucketRateLimiter.kt 파일이 작성됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/infrastructure/ratelimit/TokenBucketRateLimiter.kt`

---

## Phase 3: Yahoo Finance 인증 구현

### Step 3.1: User-Agent 관리

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/infrastructure/http/UserAgents.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.infrastructure.http

/**
 * User-Agent 관리
 */
object UserAgents {
    val CHROME = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"
    )

    val FIREFOX = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:135.0) Gecko/20100101 Firefox/135.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14.7; rv:135.0) Gecko/20100101 Firefox/135.0"
    )

    val SAFARI = listOf(
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_7_4) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.3 Safari/605.1.15"
    )

    val EDGE = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36 Edg/131.0.2903.86"
    )

    val ALL = CHROME + FIREFOX + SAFARI + EDGE

    fun random(): String = ALL.random()
}
```

✅ **완료 조건:**
- UserAgents.kt 파일이 작성됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/infrastructure/http/UserAgents.kt`

---

### Step 3.2: Yahoo API URLs

**파일 경로:** `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/internal/yahoo/YahooApiUrls.kt`

**파일 내용:**
```kotlin
package com.ulalax.ufc.internal.yahoo

/**
 * Yahoo Finance API URLs
 */
internal object YahooApiUrls {
    const val QUERY1 = "https://query1.finance.yahoo.com"
    const val QUERY2 = "https://query2.finance.yahoo.com"
    const val ROOT = "https://finance.yahoo.com"
    const val FC = "https://fc.yahoo.com"

    // API Endpoints
    const val CHART = "$QUERY2/v8/finance/chart"
    const val QUOTE_SUMMARY = "$QUERY2/v10/finance/quoteSummary"
    const val CRUMB = "$QUERY1/v1/test/getcrumb"
    const val SEARCH = "$QUERY1/v1/finance/search"
    const val SCREENER = "$QUERY1/v1/finance/screener"
}
```

✅ **완료 조건:**
- YahooApiUrls.kt 파일이 작성됨

🧪 **테스트:**
```bash
./gradlew build
```

📝 **산출물:**
- `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/internal/yahoo/YahooApiUrls.kt`

---

다음 Step들은 파일이 너무 커지므로 별도의 Phase로 구분합니다.

## 실행 순서 요약

1. **Phase 0**: 프로젝트 초기 셋업 (4 steps)
2. **Phase 1**: 공통 모델 및 예외 시스템 (3 steps)
3. **Phase 2**: Infrastructure 레이어 (3 steps)
4. **Phase 3**: Yahoo Finance 인증 (2 steps)
5. **Phase 4**: Yahoo Finance Chart API (계속...)
6. **Phase 5**: Yahoo Finance QuoteSummary API
7. **Phase 6**: FRED API
8. **Phase 7**: UFCClient Facade
9. **Phase 8**: 테스트 구현

---

**다음 문서**: Phase 4부터는 다음 파일에 계속됩니다.

각 Phase를 순서대로 진행하며, 각 Step 완료 후 반드시 빌드를 실행하여 에러가 없는지 확인하세요.
