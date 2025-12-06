import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.1.0"
    application
}

group = "com.ulalax.ufc"
version = "0.5.0"

// Java compatibility settings
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// Kotlin compiler settings
kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-opt-in=kotlin.ExperimentalCoroutinesApi"
        )
    }
}

dependencies {
    // UFC Library - depends on parent project
    implementation(project(":"))

    // Kotlin standard library and coroutines
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // Logging (optional, for better output)
    implementation("org.slf4j:slf4j-api:2.0.11")
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

// Application plugin configuration
application {
    mainClass.set("com.ulalax.ufc.examples.QuoteExampleKt") // Default main class
}

// Task to run QuoteExample
tasks.register<JavaExec>("runQuoteExample") {
    group = "examples"
    description = "Run Quote Example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.ulalax.ufc.examples.QuoteExampleKt")
}

// Task to run ChartExample
tasks.register<JavaExec>("runChartExample") {
    group = "examples"
    description = "Run Chart Example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.ulalax.ufc.examples.ChartExampleKt")
}

// Task to run ScreenerExample
tasks.register<JavaExec>("runScreenerExample") {
    group = "examples"
    description = "Run Screener Example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.ulalax.ufc.examples.ScreenerExampleKt")
}

// Task to run OptionsExample
tasks.register<JavaExec>("runOptionsExample") {
    group = "examples"
    description = "Run Options Example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.ulalax.ufc.examples.OptionsExampleKt")
}

// Task to run FredExample
tasks.register<JavaExec>("runFredExample") {
    group = "examples"
    description = "Run FRED Example (requires FRED_API_KEY)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.ulalax.ufc.examples.FredExampleKt")

    // Load FRED API key from environment or local.properties
    val fredApiKey = System.getenv("FRED_API_KEY")
        ?: project.findProperty("fred.api.key")?.toString()
    if (fredApiKey != null) {
        environment("FRED_API_KEY", fredApiKey)
    }
}

// Override run task to accept command-line arguments for which example to run
tasks.named<JavaExec>("run") {
    group = "examples"
    description = "Run an example (use --args='ExampleName')"

    // Allow running with: ./gradlew run --args="QuoteExample"
    val argsList = listOfNotNull(project.findProperty("args")?.toString())
    args = argsList

    val argsListSnapshot = argsList
    if (argsListSnapshot.isNotEmpty()) {
        val exampleName = argsListSnapshot[0]
        mainClass.set("com.ulalax.ufc.examples.${exampleName}Kt")
    }
}

// Configure source sets
sourceSets {
    main {
        kotlin {
            srcDir("src/main/kotlin")
        }
    }
}
