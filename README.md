# UFC (US Free Financial Data Collector)

[![CI](https://github.com/kairos-code-dev/ufc/actions/workflows/ci.yml/badge.svg)](https://github.com/kairos-code-dev/ufc/actions/workflows/ci.yml)
[![](https://jitpack.io/v/kairos-code-dev/ufc.svg)](https://jitpack.io/#kairos-code-dev/ufc)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

A Kotlin library for collecting financial data from Yahoo Finance and FRED (Federal Reserve Economic Data). Inspired by Python's [yfinance](https://github.com/ranaroussi/yfinance) and [fredapi](https://github.com/mortada/fredapi) libraries.

## Features

- **Yahoo Finance API**
  - Real-time quotes and market data
  - Historical OHLCV chart data
  - Company fundamentals (37 QuoteSummary modules)
  - Options chains and expirations
  - Stock screener (custom & predefined queries)
  - Earnings calendar
  - WebSocket streaming for real-time prices

- **FRED API**
  - Economic data series (GDP, unemployment, inflation, etc.)
  - Series metadata and release information

- **Business Insider**
  - ISIN to ticker symbol conversion

## Requirements

- JDK 21 or higher
- Kotlin 2.1.0+

## Installation

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.kairos-code-dev:ufc:1.0.0")
}
```

### Gradle (Groovy)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.kairos-code-dev:ufc:1.0.0'
}
```

## Quick Start

```kotlin
import com.ulalax.ufc.Ufc
import com.ulalax.ufc.UfcConfig
import com.ulalax.ufc.yahoo.model.*

// Create client
val ufc = Ufc.create()

// Or with FRED API key
val ufc = Ufc.create(UfcConfig(fredApiKey = "your-api-key"))
```

### Yahoo Finance Examples

```kotlin
// Get real-time quote
val quote = ufc.quote("AAPL")
println("Price: ${quote.pricing?.price}")

// Get multiple quotes
val quotes = ufc.quote(listOf("AAPL", "GOOGL", "MSFT"))

// Get detailed company info
val summary = ufc.quoteSummary(
    "AAPL",
    QuoteSummaryModule.PRICE,
    QuoteSummaryModule.SUMMARY_DETAIL,
    QuoteSummaryModule.FINANCIAL_DATA
)

// Get historical chart data
val chart = ufc.chart(
    symbol = "AAPL",
    interval = Interval.OneDay,
    period = Period.OneYear
)

// Get chart with dividends and splits
val chartWithEvents = ufc.chart(
    symbol = "AAPL",
    interval = Interval.OneDay,
    period = Period.FiveYears,
    events = arrayOf(ChartEventType.DIV, ChartEventType.SPLIT)
)

// Get options chain
val options = ufc.options("AAPL")
println("Expiration dates: ${options.expirationDates}")

// Stock screener - predefined
val gainers = ufc.screener(PredefinedScreener.DAY_GAINERS, count = 10)

// Stock screener - custom query
val query = EquityQuery.and(
    EquityQuery.gt(EquityField.MARKET_CAP, 10_000_000_000),
    EquityQuery.lt(EquityField.PE_RATIO, 20),
    EquityQuery.eq(EquityField.SECTOR, "Technology")
)
val results = ufc.screener(query, sortField = ScreenerSortField.MARKET_CAP)

// Search for symbols
val searchResults = ufc.search("Apple")

// Get earnings calendar
val earnings = ufc.earningsCalendar("AAPL")
```

### FRED Examples

```kotlin
// Get economic data series (requires API key)
val gdp = ufc.series("GDP")
val unemployment = ufc.series(
    seriesId = "UNRATE",
    startDate = LocalDate.of(2020, 1, 1),
    endDate = LocalDate.now()
)

// Get series metadata
val info = ufc.seriesInfo("GDP")
println("Title: ${info.title}")
```

### Business Insider Examples

```kotlin
// Convert ISIN to ticker symbol
val result = ufc.searchIsin("US0378331005")  // Apple's ISIN
println("Symbol: ${result.symbol}")  // AAPL
```

### WebSocket Streaming

```kotlin
// Real-time price streaming
ufc.stream(listOf("AAPL", "GOOGL", "MSFT")) { priceData ->
    println("${priceData.symbol}: ${priceData.price}")
}
```

### Resource Management

```kotlin
// Using use block for automatic cleanup
Ufc.create().use { ufc ->
    val quote = ufc.quote("AAPL")
    // ...
}

// Or manual close
val ufc = Ufc.create()
try {
    // use ufc
} finally {
    ufc.close()
}
```

## API Documentation

Detailed API specifications are available in the [docs/specs](docs/specs) directory:

| API | Description |
|-----|-------------|
| [Quote API](docs/specs/quote-api-spec.md) | Real-time market data |
| [QuoteSummary API](docs/specs/quote-summary-api-spec.md) | Detailed company information (37 modules) |
| [Chart API](docs/specs/chart-api-spec.md) | Historical OHLCV data |
| [Options API](docs/specs/options-api-spec.md) | Options chains and Greeks |
| [Screener API](docs/specs/screener-api-spec.md) | Stock screening |
| [Search API](docs/specs/search-api-spec.md) | Symbol search |
| [Lookup API](docs/specs/lookup-api-spec.md) | Symbol lookup |
| [Market API](docs/specs/market-api-spec.md) | Market summary |
| [Earnings Calendar API](docs/specs/earnings-calendar-api-spec.md) | Earnings dates |
| [Fundamentals Timeseries API](docs/specs/fundamentals-timeseries-api-spec.md) | Financial statement timeseries |
| [Visualization API](docs/specs/visualization-api-spec.md) | Visualization data |
| [WebSocket Streaming API](docs/specs/websocket-streaming-api-spec.md) | Real-time streaming |
| [FRED API](docs/specs/fred-api-spec.md) | Federal Reserve economic data |
| [Business Insider API](docs/specs/businessinsider-isin-api-spec.md) | ISIN lookup |
| [UFC Facade API](docs/specs/ufc-facade-api-spec.md) | Main client interface |

## Configuration

```kotlin
val config = UfcConfig(
    // FRED API key (optional, required for FRED API)
    fredApiKey = "your-fred-api-key",

    // Request timeout in milliseconds
    requestTimeoutMs = 30_000,

    // Connection timeout in milliseconds
    connectTimeoutMs = 10_000
)

val ufc = Ufc.create(config)
```

## Error Handling

```kotlin
import com.ulalax.ufc.common.exception.UfcException
import com.ulalax.ufc.common.exception.ErrorCode

try {
    val quote = ufc.quote("INVALID_SYMBOL")
} catch (e: UfcException) {
    when (e.errorCode) {
        ErrorCode.INVALID_SYMBOL -> println("Invalid symbol")
        ErrorCode.RATE_LIMITED -> println("Rate limited, try again later")
        ErrorCode.NETWORK_ERROR -> println("Network error")
        ErrorCode.DATA_PARSING_ERROR -> println("Failed to parse response")
        else -> println("Error: ${e.message}")
    }
}
```

## Building from Source

```bash
# Clone the repository
git clone https://github.com/kairos-code-dev/ufc.git
cd ufc

# Build the project
./gradlew build

# Run tests
./gradlew test

# Run unit tests only
./gradlew unitTest

# Run integration tests only
./gradlew integrationTest
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Disclaimer

This library uses unofficial Yahoo Finance APIs. These APIs are not officially supported by Yahoo and may change without notice. Use at your own risk. This library is intended for personal use and educational purposes only.

## Acknowledgments

- Inspired by [yfinance](https://github.com/ranaroussi/yfinance) (Python)
- Inspired by [fredapi](https://github.com/mortada/fredapi) (Python)
