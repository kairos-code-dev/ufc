# UFC Examples

This directory contains comprehensive examples demonstrating how to use the UFC (Unified Finance Client) library to collect financial data from various sources.

## Overview

The examples demonstrate:
- **QuoteExample.kt**: Real-time quote retrieval for single and multiple symbols
- **ChartExample.kt**: Historical OHLCV data with different intervals and periods
- **ScreenerExample.kt**: Stock screening using predefined and custom queries
- **OptionsExample.kt**: Options chain data retrieval
- **FredExample.kt**: FRED economic data (requires API key)

## Prerequisites

- JDK 21 or higher
- Gradle 8.6 or higher (included via wrapper)
- Internet connection (for API calls)
- FRED API key (for FredExample only - get one free at https://fred.stlouisfed.org/docs/api/api_key.html)

## Setup

### 1. Build the Parent Project

First, ensure the UFC library is built:

```bash
# From the project root directory
cd /home/ulalax/project/kairos/ufc
./gradlew build
```

### 2. Configure FRED API Key (Optional)

If you want to run the FRED example, you need to set up your API key:

**Option A: Environment Variable**
```bash
export FRED_API_KEY="your_api_key_here"
```

**Option B: local.properties File**
Create or edit `/home/ulalax/project/kairos/ufc/examples/local.properties`:
```properties
fred.api.key=your_api_key_here
```

## Running Examples

You can run examples in two ways:

### Method 1: Using Gradle Tasks

Each example has a dedicated Gradle task:

```bash
# From the examples directory
cd /home/ulalax/project/kairos/ufc/examples

# Run specific examples
./gradlew runQuoteExample
./gradlew runChartExample
./gradlew runScreenerExample
./gradlew runOptionsExample
./gradlew runFredExample
```

### Method 2: Using the Generic Run Task

```bash
# From the examples directory
cd /home/ulalax/project/kairos/ufc/examples

# Run specific example by class name
./gradlew run --args="QuoteExample"
./gradlew run --args="ChartExample"
./gradlew run --args="ScreenerExample"
./gradlew run --args="OptionsExample"
./gradlew run --args="FredExample"
```

### Method 3: From IntelliJ IDEA

1. Open the project in IntelliJ IDEA
2. Navigate to `examples/src/main/kotlin/examples/`
3. Right-click any example file (e.g., `QuoteExample.kt`)
4. Select "Run 'QuoteExampleKt'"

## Example Descriptions

### QuoteExample.kt
Demonstrates how to retrieve real-time quotes:
- Single symbol quote retrieval
- Multiple symbols batch retrieval
- Accessing various quote fields (price, volume, market cap, etc.)
- Error handling for invalid symbols

**Sample Output:**
```
=== Single Symbol Quote ===
Symbol: AAPL
Price: $175.43
Change: +2.15 (+1.24%)
Volume: 52,341,234
Market Cap: $2.75T

=== Multiple Symbols Quote ===
GOOGL: $142.65 (+0.82%)
MSFT: $378.91 (-0.34%)
...
```

### ChartExample.kt
Shows how to fetch historical OHLCV data:
- Different time intervals (1m, 5m, 1d, 1wk, 1mo)
- Various periods (1d, 5d, 1mo, 3mo, 6mo, 1y, 2y, 5y, 10y, ytd, max)
- Dividend and split events
- Data validation and formatting

**Sample Output:**
```
=== Daily Chart Data for AAPL (1 Year) ===
Date: 2024-01-03, Open: $184.22, High: $185.88, Low: $183.43, Close: $184.25, Volume: 58,414,480
Date: 2024-01-04, Open: $182.15, High: $183.09, Low: $180.88, Close: $181.91, Volume: 71,379,163
...

=== Dividends ===
2024-02-09: $0.24
2024-05-10: $0.24
...
```

### ScreenerExample.kt
Demonstrates stock screening capabilities:
- Using predefined screeners (most active, day gainers, day losers, etc.)
- Building custom queries with filters
- Sorting and pagination
- Field selection and data extraction

**Sample Output:**
```
=== Predefined Screener: Day Gainers ===
1. TSLA - $248.42 (+8.32%) | Volume: 125.4M
2. NVDA - $495.22 (+5.67%) | Volume: 89.2M
...

=== Custom Screener: Large Cap Tech Stocks ===
Found 47 stocks matching criteria:
- Market Cap > $10B
- PE Ratio < 30
- Region: United States

AAPL: P/E 28.4, Market Cap $2.75T
GOOGL: P/E 24.1, Market Cap $1.82T
...
```

### OptionsExample.kt
Shows how to retrieve options chain data:
- All available expiration dates
- Calls and puts for specific expiration
- Strike prices and Greeks
- Volume and open interest

**Sample Output:**
```
=== Options Chain for AAPL ===
Underlying Price: $175.43
Available Expirations: 12

=== Expiration: 2024-01-19 (14 days) ===

CALLS:
Strike $170.00 | Last: $7.85 | Bid: $7.75 | Ask: $7.95 | Vol: 1,234 | OI: 5,678 | IV: 28.4%
Strike $175.00 | Last: $4.20 | Bid: $4.10 | Ask: $4.30 | Vol: 2,456 | OI: 8,901 | IV: 26.2%
...

PUTS:
Strike $175.00 | Last: $3.85 | Bid: $3.75 | Ask: $3.95 | Vol: 1,890 | OI: 6,543 | IV: 25.8%
Strike $170.00 | Last: $1.45 | Bid: $1.40 | Ask: $1.50 | Vol: 987 | OI: 3,210 | IV: 24.1%
...
```

### FredExample.kt
Demonstrates FRED economic data retrieval:
- Popular economic indicators (GDP, unemployment, inflation)
- Custom date ranges
- Data frequency options
- Series metadata

**Sample Output:**
```
=== FRED Economic Data ===
Note: Requires FRED API key

=== GDP (Gross Domestic Product) ===
Series ID: GDP
Title: Gross Domestic Product
Units: Billions of Dollars
Frequency: Quarterly

2023-Q1: $26,469.50B
2023-Q2: $26,840.70B
2023-Q3: $27,610.10B
2023-Q4: $27,939.00B

=== Unemployment Rate ===
Series ID: UNRATE
2024-01: 3.7%
2024-02: 3.9%
...
```

## Common Issues and Solutions

### Issue: "Connection refused" or "Timeout"
**Solution**: Check your internet connection and ensure Yahoo Finance / FRED APIs are accessible.

### Issue: "Rate limit exceeded"
**Solution**: The library includes built-in rate limiting. If you still encounter this, wait a few minutes and try again.

### Issue: "Invalid API key" (FRED)
**Solution**: Verify your FRED API key is correct and properly set in environment variables or local.properties.

### Issue: "Symbol not found"
**Solution**: Ensure you're using valid Yahoo Finance ticker symbols (e.g., "AAPL" not "Apple Inc.").

## Best Practices

1. **Resource Management**: Always use `use { }` blocks or call `close()` on UFC client instances
2. **Error Handling**: Wrap API calls in try-catch blocks to handle network issues gracefully
3. **Rate Limiting**: Respect the built-in rate limits to avoid API throttling
4. **Batch Operations**: Use batch methods (e.g., `quote(listOf(...))`) instead of individual calls when fetching multiple symbols
5. **Caching**: Consider implementing caching for frequently accessed data

## API Documentation

For complete API documentation, see:
- Main README: `/home/ulalax/project/kairos/ufc/README.md`
- API Documentation: `/home/ulalax/project/kairos/ufc/docs/`

## Support

If you encounter issues:
1. Check the main project README for troubleshooting tips
2. Review the test cases in `src/test/kotlin/` for additional usage examples
3. Open an issue on the project repository

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.
