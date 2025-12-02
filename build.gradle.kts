import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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

// 소스셋 설정
sourceSets {
    create("liveTest") {
        kotlin {
            srcDir("src/liveTest/kotlin")
        }
        resources {
            srcDir("src/liveTest/resources")
        }
        compileClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].output
    }
}

// LiveTest 테스트 작업 설정
val liveTestImplementation by configurations.getting {
    extendsFrom(configurations["testImplementation"])
}

// liveTest에 GSON 의존성 추가
dependencies {
    liveTestImplementation("com.google.code.gson:gson:$gsonVersion")
}

val liveTest = tasks.register<Test>("liveTest") {
    useJUnitPlatform()
    testClassesDirs = sourceSets["liveTest"].output.classesDirs
    classpath = sourceSets["liveTest"].runtimeClasspath
    shouldRunAfter("test")

    // 라이브 테스트 필터 설정
    filter {
        setFailOnNoMatchingTests(false)
    }

    // 환경 변수 설정
    environment("TEST_ENV", "live")
}

// 빌드 작업 설정
tasks {
    // LiveTest 리소스 중복 처리 전략
    named<Copy>("processLiveTestResources") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    test {
        useJUnitPlatform {
            includeTags("unit", "integration")
            excludeTags("liveTest")
        }

        // 테스트 출력 설정
        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStandardStreams = false
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
