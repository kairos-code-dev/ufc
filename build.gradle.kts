import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.Duration

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("java-library")
}

group = "com.ulalax"
version = "1.0.0"

// Java 호환성 설정
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// Kotlin 컴파일러 설정
kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-opt-in=kotlin.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
        )
    }
}

// 의존성 버전 정의
val kotlinVersion = "2.1.0"
val ktorVersion = "3.0.1"
val kotlinxSerializationVersion = "1.7.3"
val jsoupVersion = "1.18.1"
val junitVersion = "5.10.0"
val kotestVersion = "5.8.1"
val assertjVersion = "3.24.1"
val slf4jVersion = "2.0.11"
val logbackVersion = "1.4.14"
val mockkkVersion = "1.13.8"
val gsonVersion = "2.10.1"

dependencies {
    // Kotlin 표준 라이브러리
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // HTTP 클라이언트 (Ktor만 사용)
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-encoding:$ktorVersion")

    // 직렬화
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")

    // HTML 파싱
    implementation("org.jsoup:jsoup:$jsoupVersion")

    // 로깅
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("ch.qos.logback:logback-core:$logbackVersion")

    // JSON 직렬화
    implementation("com.google.code.gson:gson:$gsonVersion")

    // 테스트 - 유닛 테스트
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")

    // 테스트 - Kotest (BDD 스타일 테스트)
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")

    // 테스트 - Assertion & Mock
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("io.mockk:mockk:$mockkkVersion")

    // 테스트 - Ktor 클라이언트 테스트
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
}

// 유닛 테스트 태스크 설정
val unitTest = tasks.register<Test>("unitTest") {
    description = "Runs unit tests only (pure domain logic, excludes integration tests)"
    group = "verification"

    useJUnitPlatform {
        excludeTags("integration")
    }

    // 유닛 테스트는 순차 실행
    maxParallelForks = 1

    // 테스트 출력 설정
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

// 통합 테스트 태스크 설정
val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests only (tagged with @Tag(\"integration\"))"
    group = "verification"

    useJUnitPlatform {
        includeTags("integration")
    }

    // 통합 테스트 순차 실행 (Rate Limiting 방지)
    maxParallelForks = 1

    // 타임아웃 증가 (실제 API 호출이 느릴 수 있음)
    timeout.set(Duration.ofMinutes(5))

    // 테스트 출력 설정
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true // 통합 테스트는 출력 표시
    }

    // 환경 변수 설정
    environment("TEST_ENV", "integration")
}

// 빌드 작업 설정
tasks {
    test {
        description = "Runs all tests (unit + integration) in parallel"

        useJUnitPlatform {
            // liveTest만 제외하고 모든 테스트 실행 (unit + integration)
            excludeTags("liveTest")
        }

        // 모든 테스트 순차 실행 (Rate Limiting 방지)
        maxParallelForks = 1

        // 타임아웃 증가 (통합 테스트 포함)
        timeout.set(Duration.ofMinutes(15))

        // 테스트 출력 설정
        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStandardStreams = true // 모든 테스트 출력 표시
        }
    }

    // 전체 빌드 작업 설정
    build {
        dependsOn(test)
    }

    // 빌드 정보 출력
    wrapper {
        gradleVersion = "8.6"
        distributionType = Wrapper.DistributionType.BIN
    }
}

// 프로젝트 레이아웃 설정
layout.buildDirectory = file("build")
