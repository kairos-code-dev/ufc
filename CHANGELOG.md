# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2025-12-05

Initial release of UFC (US Free Financial Data Collector), a Kotlin library for collecting financial data from Yahoo Finance and FRED.

### Added

#### Yahoo Finance APIs

- **Quote API**: Real-time market data for single or multiple symbols
  - Current price, bid/ask, volume, and market cap
  - Day high/low, 52-week high/low ranges
  - Price changes and percentage movements
  - Extended hours trading data (pre-market/post-market)
  - Moving averages (50-day, 200-day)
  - Financial ratios (P/E, P/B, debt-to-equity)
  - Analyst ratings and revenue/earnings data
  - Support for batch quote requests

- **QuoteSummary API**: Detailed company information with 37 modules
  - Asset profile and company information
  - Financial data (margins, profitability metrics)
  - Balance sheet, income statement, cash flow statements
  - Earnings history and estimates
  - Institutional and insider holdings
  - Recommendation trends and analyst ratings
  - Price history and summary details
  - Calendar events and dividend history

- **Chart API**: Historical OHLCV (Open, High, Low, Close, Volume) data
  - Multiple time intervals (1m, 2m, 5m, 15m, 30m, 60m, 90m, 1h, 1d, 5d, 1wk, 1mo, 3mo)
  - Multiple time periods (1d, 5d, 1mo, 3mo, 6mo, 1y, 2y, 5y, 10y, ytd, max)
  - Corporate actions (dividends, stock splits)
  - Adjusted and unadjusted prices
  - Trading session metadata

- **Options API**: Options chain data and Greeks
  - Call and put option contracts
  - Strike prices and expiration dates
  - Greeks (delta, gamma, theta, vega, rho, implied volatility)
  - Open interest and trading volumes
  - Underlying asset quotes
  - Multiple expiration date support

- **Screener API**: Stock screening with custom and predefined queries
  - Custom query builder with field operators (EQ, GT, LT, GTE, LTE, BTWN)
  - 100+ screening fields (price, volume, market cap, P/E ratio, etc.)
  - Predefined screeners (day gainers/losers, most active, undervalued growth stocks, etc.)
  - Sorting and pagination support
  - Result count and offset controls

- **Search API**: Unified search for symbols and financial news
  - Symbol search with fuzzy matching support
  - News article search with thumbnails
  - Configurable result counts for quotes and news
  - Publisher and thumbnail resolution data

- **Lookup API**: Type-filtered symbol search
  - Search by symbol or company name
  - Filter by asset type (EQUITY, ETF, INDEX, MUTUALFUND, FUTURES, CURRENCY, etc.)
  - Detailed document results with exchange and type information
  - Support for multiple asset types in single query

- **Market API**: Market summary and trading hours
  - Market summary by region (US, Asia, Europe, etc.)
  - Real-time market state (PREPRE, PRE, REGULAR, POST, POSTPOST, CLOSED)
  - Trading hours and timezone information
  - Market indices and their performance

- **Earnings Calendar API**: Historical and upcoming earnings dates
  - Past and future earnings announcement dates
  - Earnings per share (EPS) data
  - Pagination support with limit and offset
  - HTML scraping-based implementation

- **Fundamentals Timeseries API**: Financial statement time series data
  - Revenue, net income, operating income metrics
  - Balance sheet items (total assets, total debt, stockholders equity)
  - Cash flow metrics (free cash flow, operating cash flow)
  - Per-share metrics (EPS, book value per share)
  - Date range filtering
  - Support for 50+ fundamental data types

- **Visualization API**: Earnings visualization data
  - Historical and future earnings dates
  - Earnings event types (earnings release, conference call)
  - Date formatting for visualization purposes

- **WebSocket Streaming API**: Real-time price streaming
  - Live price updates via WebSocket
  - Subscription management for multiple symbols
  - Automatic reconnection with exponential backoff
  - Heartbeat mechanism for connection maintenance
  - Protobuf message decoding
  - Market hours state tracking (PRE_MARKET, REGULAR, POST_MARKET, CLOSED)
  - Real-time bid/ask quotes and volumes
  - SharedFlow-based event emission
  - Symbol-specific and global data streams
  - Connection lifecycle events (Connected, Disconnected, Reconnecting, Error)

#### FRED (Federal Reserve Economic Data) API

- **Series API**: Economic time series data
  - GDP, unemployment rate, inflation, and other economic indicators
  - Date range filtering (start date, end date)
  - Data frequency options (daily, weekly, monthly, quarterly, annual)
  - Automatic data transformation
  - Support for 800,000+ economic data series

- **Series Info API**: Series metadata and information
  - Series title, units, and frequency
  - Last update timestamps
  - Seasonal adjustment information
  - Data source and release details

#### Business Insider API

- **ISIN Search API**: ISIN to ticker symbol conversion
  - Convert International Securities Identification Numbers to trading symbols
  - Support for global securities
  - Exchange and market information

#### Infrastructure

- **Unified Facade Pattern**: Single entry point (Ufc class) for all data sources
  - Integrated access to Yahoo Finance, FRED, and Business Insider APIs
  - Centralized configuration management
  - Consistent error handling across all APIs

- **Rate Limiting**: Token bucket algorithm implementation
  - Global rate limiters shared across client instances
  - Configurable rates per data source (Yahoo: 50 RPS, FRED: 2 RPS, Business Insider: 10 RPS)
  - Automatic request throttling
  - Prevent API abuse and quota exhaustion

- **Authentication**: Yahoo Finance authentication handling
  - Automatic cookie and crumb management
  - Session persistence
  - Transparent re-authentication on expiration

- **Error Handling**: Comprehensive error codes and exception hierarchy
  - Structured UfcException with ErrorCode enum
  - Network errors (connection failures, timeouts)
  - Authentication errors
  - Rate limiting errors
  - Data parsing errors (JSON, Protobuf)
  - WebSocket-specific errors (connection, protocol, subscription)
  - Configuration errors
  - Validation errors
  - Retry guidance for each error type

- **Resource Management**: Automatic cleanup via AutoCloseable
  - Support for Kotlin's use block pattern
  - Proper HTTP client and WebSocket connection disposal
  - Memory leak prevention

- **HTTP Client**: Ktor-based HTTP client with advanced features
  - Configurable timeouts (connect, request, socket)
  - Cookie management
  - Content negotiation (JSON)
  - Optional request/response logging
  - Custom User-Agent headers
  - Connection pooling

- **Testing Infrastructure**: Comprehensive test framework
  - Unit tests with mock JSON responses
  - Integration tests with real API calls
  - Smart response recording system for test data
  - Separate test tags (unit, integration, liveTest)
  - Custom test base classes (UnitTestBase, IntegrationTestBase)

### Dependencies

- Kotlin 2.1.0
- Ktor 3.0.1 (HTTP client and WebSocket)
- kotlinx.serialization 1.7.3 (JSON and Protobuf)
- kotlinx.coroutines 1.10.1 (async/concurrent operations)
- kotlinx-datetime 0.6.1 (date/time handling)
- SLF4J 2.0.16 (logging facade)
- Logback 1.5.12 (logging implementation)

### Documentation

- API specifications for all 15+ endpoints in docs/specs/
- Architecture guide with design patterns and principles
- Spec guide for documentation standards
- Comprehensive README with quick start examples
- CLAUDE.md for AI assistant guidance

[Unreleased]: https://github.com/kairos-code-dev/ufc/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/kairos-code-dev/ufc/releases/tag/v1.0.0
