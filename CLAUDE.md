# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Ufc (Unified Finance Client) is a Kotlin library for collecting financial data from Yahoo Finance and FRED (Federal Reserve Economic Data). It's inspired by Python's yfinance and fredapi libraries.

**Tech Stack**: Kotlin 2.1.0, JDK 21, Ktor 3.0.1 (HTTP client), kotlinx.serialization 1.7.3, kotlinx.coroutines

## Build & Test Commands

```bash
# Build the project
./gradlew build

# Run all tests (unit + integration, excludes liveTest)
./gradlew test

# Run unit tests only (fast, uses mock data)
./gradlew unitTest

# Run integration tests only (real API calls)
./gradlew integrationTest

# Run a single test class
./gradlew test --tests "com.ulalax.ufc.integration.yahoo.QuoteSummarySpec"

# Run a single test method
./gradlew test --tests "com.ulalax.ufc.integration.yahoo.QuoteSummarySpec.returns price module for AAPL"
```

## Architecture

### Package Structure

```
com.ulalax.ufc/
├── Ufc.kt              # Main entry point - unified client facade
├── UfcConfig.kt        # Configuration for all data sources
├── yahoo/              # Yahoo Finance client
│   ├── YahooClient.kt  # Public API (quoteSummary, chart)
│   ├── model/          # Public domain models
│   └── internal/       # Internal response types, auth, URLs
├── fred/               # FRED API client
│   ├── FredClient.kt   # Public API (series, seriesInfo)
│   ├── model/          # Public domain models
│   └── internal/       # Internal HTTP client, response types
├── businessinsider/    # Business Insider client (ISIN search)
└── common/             # Shared infrastructure
    ├── exception/      # UfcException, ErrorCode
    └── ratelimit/      # Token bucket rate limiting
```

### Key Design Patterns

1. **Facade Pattern**: `Ufc` class is the main entry point, delegating to `YahooClient`, `FredClient`, and `BusinessInsiderClient`

2. **Internal vs Public Types**: Each client has `internal/response/` types for API deserialization and `model/` types for public domain objects. YahooClient converts between these in private `convert*` methods.

3. **Global Rate Limiters**: `GlobalRateLimiters` singleton manages shared rate limiters across client instances to prevent API abuse

4. **AutoCloseable**: All clients implement `AutoCloseable` for proper resource cleanup

### Main APIs

```kotlin
// Create client
val ufc = Ufc.create()  // or Ufc.create(UfcConfig(fredApiKey = "..."))

// Yahoo Finance
ufc.quoteSummary("AAPL", QuoteSummaryModule.PRICE, QuoteSummaryModule.SUMMARY_DETAIL)
ufc.chart("AAPL", Interval.OneDay, Period.OneYear, ChartEventType.DIV, ChartEventType.SPLIT)

// FRED (requires API key)
ufc.series("GDP", startDate = LocalDate.of(2020, 1, 1))

// Business Insider
ufc.searchIsin("US0378331005")
```

## Testing Conventions

- **Unit tests**: Extend `UnitTestBase`, use `@Tag("unit")`, load mock JSON from `src/test/resources/responses/`
- **Integration tests**: Extend `IntegrationTestBase`, use `@Tag("integration")`, call real APIs
- **Test helper**: Use `integrationTest { }` or `unitTest { }` blocks for coroutine test execution
- **FRED tests**: Set `FRED_API_KEY` env var or in `local.properties`

## Response Recording

Integration tests can record API responses to `src/test/resources/responses/` for use in unit tests. See `SmartRecorder` and `RecordingConfig` for recording utilities.
