import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.Duration

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("java-library")
    id("maven-publish")
    id("org.jetbrains.dokka") version "1.9.20"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
}

// Ktlint 설정
ktlint {
    version.set("1.5.0")
    android.set(false)
    ignoreFailures.set(false)

    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

group = "com.github.kairos-code-dev"
version = "0.5.0"

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
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
        )
    }
}

// Maven 퍼블리싱 설정 (JitPack)
java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.kairos-code-dev"
            artifactId = "ufc"
            version = project.version.toString()

            from(components["java"])
        }
    }
}

// Dokka 설정 (API 문서 생성)
tasks.dokkaHtml {
    outputDirectory.set(layout.buildDirectory.dir("dokka/html"))

    dokkaSourceSets {
        configureEach {
            moduleName.set("UFC - Unified Finance Client")

            // 소스 링크 설정 (GitHub)
            sourceLink {
                localDirectory.set(projectDir.resolve("src"))
                remoteUrl.set(uri("https://github.com/kairos-code-dev/ufc/tree/main/src").toURL())
                remoteLineSuffix.set("#L")
            }

            // 외부 문서 링크
            externalDocumentationLink {
                url.set(uri("https://kotlinlang.org/api/kotlinx.coroutines/").toURL())
            }
            externalDocumentationLink {
                url.set(uri("https://kotlinlang.org/api/kotlinx.serialization/").toURL())
            }
            externalDocumentationLink {
                url.set(uri("https://api.ktor.io/").toURL())
            }

            // 문서화 설정
            includeNonPublic.set(false)
            skipEmptyPackages.set(true)
            skipDeprecated.set(false)
            reportUndocumented.set(false)
            suppressInheritedMembers.set(false)

            // JDK 문서 링크
            jdkVersion.set(21)
        }
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
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")

    // 직렬화
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion")

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
val unitTest =
    tasks.register<Test>("unitTest") {
        description = "Runs unit tests only (pure domain logic, excludes integration tests)"
        group = "verification"

        useJUnitPlatform {
            excludeTags("integration")
        }

        // 유닛 테스트 병렬 실행 (CPU 스레드 수)
        maxParallelForks = Runtime.getRuntime().availableProcessors()

        // 테스트 출력 설정
        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStandardStreams = false
        }
    }

// 통합 테스트 태스크 설정
val integrationTest =
    tasks.register<Test>("integrationTest") {
        description = "Runs integration tests only (tagged with @Tag(\"integration\"))"
        group = "verification"

        useJUnitPlatform {
            includeTags("integration")
        }

        // 통합 테스트 병렬 실행 (CPU 스레드 수)
        maxParallelForks = Runtime.getRuntime().availableProcessors()

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
        }

        // 모든 테스트 병렬 실행 (CPU 스레드 수)
        maxParallelForks = Runtime.getRuntime().availableProcessors()

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
