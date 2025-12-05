# Quote API 기능 명세서

> **Version**: 1.0
> **작성일**: 2025-12-05
> **대상**: UFC 프로젝트에 Yahoo Finance `/v7/finance/quote` API 통합

---

## 목차

- [1. API 개요](#1-api-개요)
- [2. Yahoo Finance Quote API 분석](#2-yahoo-finance-quote-api-분석)
- [3. UFC 통합 설계](#3-ufc-통합-설계)
- [4. 데이터 매핑](#4-데이터-매핑)
- [5. 에러 처리](#5-에러-처리)
- [6. 테스트 전략](#6-테스트-전략)
- [7. 구현 우선순위](#7-구현-우선순위)

---

## 1. API 개요

### 1.1 Quote API의 목적

Quote API는 Yahoo Finance의 `/v7/finance/quote` 엔드포인트를 통해 **실시간 시장 데이터와 기본 주식 정보**를 빠르게 조회하기 위한 API입니다.

### 1.2 QuoteSummary API와의 차이점

| 구분 | Quote API (`/v7/finance/quote`) | QuoteSummary API (`/v10/finance/quoteSummary`) |
|------|--------------------------------|----------------------------------------------|
| **목적** | 실시간 시장 데이터 및 기본 정보 | 상세한 모듈별 금융 정보 |
| **응답 속도** | 빠름 (단일 요청) | 상대적으로 느림 (모듈별 집계) |
| **데이터 범위** | 넓고 얕음 (시장 데이터 중심) | 좁고 깊음 (상세 금융 정보) |
| **모듈 선택** | 불가능 (고정 응답) | 가능 (PRICE, SUMMARY_DETAIL 등) |
| **파라미터** | `symbols` (다중 심볼 지원) | `symbol` (단일 심볼), `modules` |
| **응답 구조** | `quoteResponse.result[]` | `quoteSummary.result[].modules` |
| **주요 필드** | regularMarketPrice, marketCap, volume, 52주 고저가, PE/PB 등 | 모듈별 상세 정보 (AssetProfile, FinancialData 등) |

### 1.3 주요 사용 사례

1. **빠른 현재가 조회**: 다중 심볼의 실시간 가격 정보를 한 번에 조회
2. **마켓 스크리너**: 여러 종목의 시가총액, PER, 거래량 등을 비교
3. **포트폴리오 모니터링**: 다수의 종목을 동시에 모니터링
4. **기본 정보 확인**: 심볼의 거래소, 통화, 자산 유형 등 메타데이터 확인
5. **QuoteSummary 대체**: 복잡한 모듈이 필요 없는 경우 더 빠른 대안으로 사용

### 1.4 Quote API의 장점

- **다중 심볼 지원**: 한 번의 요청으로 최대 수십 개 심볼 조회 가능
- **빠른 응답**: QuoteSummary보다 평균 30-50% 빠른 응답 시간
- **포괄적 데이터**: 가격, 시가총액, 배당률, PER, 거래량 등 대부분의 기본 정보 포함
- **간단한 구조**: 모듈 없이 플랫한 JSON 구조로 파싱이 용이

---

## 2. Yahoo Finance Quote API 분석

### 2.1 API 엔드포인트

```
GET https://query1.finance.yahoo.com/v7/finance/quote
```

**베이스 URL**: `YahooApiUrls.QUERY1` (`https://query1.finance.yahoo.com`)

### 2.2 요청 파라미터

| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---------|------|------|------|------|
| `symbols` | String | 필수 | 조회할 심볼 (쉼표로 구분) | `"AAPL"` 또는 `"AAPL,GOOGL,MSFT"` |
| `formatted` | String | 선택 | 포맷팅 여부 (`true`/`false`) | `"false"` (기본값: `true`) |
| `crumb` | String | 선택 | CRUMB 토큰 (인증용) | `"dFhd8fj..."`|

**참고사항**:
- `formatted=false`로 설정하면 `raw` 값만 반환 (파싱 용이)
- `formatted=true`로 설정하면 `raw`와 `fmt` (포맷팅된 문자열) 모두 반환
- 다중 심볼 조회 시 최대 개수는 비공식 제한 (일반적으로 50-100개까지 안전)

### 2.3 응답 JSON 구조

#### 2.3.1 전체 응답 구조

```json
{
  "quoteResponse": {
    "result": [
      {
        "symbol": "AAPL",
        "longName": "Apple Inc.",
        "regularMarketPrice": 175.43,
        "marketCap": 2800000000000,
        // ... 약 80-100개 필드
      }
    ],
    "error": null
  }
}
```

#### 2.3.2 에러 응답 구조

```json
{
  "quoteResponse": {
    "result": [],
    "error": {
      "code": "Not Found",
      "description": "No data found for symbol INVALID"
    }
  }
}
```

### 2.4 주요 응답 필드 분류

Quote API는 약 80-100개의 필드를 반환하며, 다음과 같이 분류할 수 있습니다:

#### 2.4.1 기본 정보 (Identification)

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| `symbol` | String | 심볼 | `"AAPL"` |
| `longName` | String | 정식 명칭 | `"Apple Inc."` |
| `shortName` | String | 약식 명칭 | `"Apple"` |
| `exchange` | String | 거래소 코드 | `"NMS"` (NASDAQ) |
| `exchangeTimezoneName` | String | 거래소 시간대 | `"America/New_York"` |
| `exchangeTimezoneShortName` | String | 시간대 약어 | `"EST"` |
| `quoteType` | String | 자산 유형 | `"EQUITY"`, `"ETF"`, `"INDEX"` |
| `currency` | String | 통화 코드 | `"USD"` |
| `market` | String | 시장 분류 | `"us_market"` |

#### 2.4.2 가격 정보 (Pricing)

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| `regularMarketPrice` | Double | 현재가 | `175.43` |
| `regularMarketOpen` | Double | 시가 | `174.50` |
| `regularMarketDayHigh` | Double | 당일 고가 | `176.20` |
| `regularMarketDayLow` | Double | 당일 저가 | `174.30` |
| `regularMarketVolume` | Long | 당일 거래량 | `58000000` |
| `regularMarketPreviousClose` | Double | 전일 종가 | `174.00` |
| `regularMarketChange` | Double | 가격 변화 | `1.43` |
| `regularMarketChangePercent` | Double | 변화율 (%) | `0.82` |
| `regularMarketTime` | Long | 시장 시간 (Unix timestamp) | `1701878400` |

#### 2.4.3 장전/장후 거래 (Pre/Post Market)

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| `preMarketPrice` | Double | 장전 가격 | `175.00` |
| `preMarketChange` | Double | 장전 변화 | `1.00` |
| `preMarketChangePercent` | Double | 장전 변화율 | `0.57` |
| `preMarketTime` | Long | 장전 시간 | `1701866400` |
| `postMarketPrice` | Double | 장후 가격 | `175.60` |
| `postMarketChange` | Double | 장후 변화 | `0.17` |
| `postMarketChangePercent` | Double | 장후 변화율 | `0.10` |
| `postMarketTime` | Long | 장후 시간 | `1701892800` |

#### 2.4.4 52주 고저가 (52-Week Range)

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| `fiftyTwoWeekHigh` | Double | 52주 최고가 | `198.23` |
| `fiftyTwoWeekLow` | Double | 52주 최저가 | `124.17` |
| `fiftyTwoWeekHighChange` | Double | 최고가 대비 변화 | `-22.80` |
| `fiftyTwoWeekLowChange` | Double | 최저가 대비 변화 | `51.26` |
| `fiftyTwoWeekHighChangePercent` | Double | 최고가 대비 변화율 | `-11.50` |
| `fiftyTwoWeekLowChangePercent` | Double | 최저가 대비 변화율 | `41.28` |
| `fiftyTwoWeekRange` | String | 52주 범위 | `"124.17 - 198.23"` |

#### 2.4.5 이동평균 (Moving Averages)

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| `fiftyDayAverage` | Double | 50일 이동평균 | `172.50` |
| `fiftyDayAverageChange` | Double | 50일 평균 대비 변화 | `2.93` |
| `fiftyDayAverageChangePercent` | Double | 50일 평균 대비 변화율 | `1.70` |
| `twoHundredDayAverage` | Double | 200일 이동평균 | `165.30` |
| `twoHundredDayAverageChange` | Double | 200일 평균 대비 변화 | `10.13` |
| `twoHundredDayAverageChangePercent` | Double | 200일 평균 대비 변화율 | `6.13` |

#### 2.4.6 거래량 평균 (Volume Averages)

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| `averageDailyVolume3Month` | Long | 3개월 평균 일일 거래량 | `55000000` |
| `averageDailyVolume10Day` | Long | 10일 평균 일일 거래량 | `58000000` |

#### 2.4.7 시가총액 및 발행주식수 (Market Cap & Shares)

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| `marketCap` | Long | 시가총액 | `2800000000000` |
| `sharesOutstanding` | Long | 발행주식수 | `16000000000` |

#### 2.4.8 배당 정보 (Dividends)

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| `dividendRate` | Double | 연간 배당금 | `0.96` |
| `dividendYield` | Double | 배당수익률 | `0.0055` (0.55%) |
| `dividendDate` | Long | 배당 지급일 (Unix timestamp) | `1699545600` |
| `exDividendDate` | Long | 배당락일 (Unix timestamp) | `1698940800` |
| `trailingAnnualDividendRate` | Double | 과거 12개월 배당금 | `0.94` |
| `trailingAnnualDividendYield` | Double | 과거 12개월 배당수익률 | `0.0054` |

#### 2.4.9 재무 비율 (Financial Ratios)

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| `trailingPE` | Double | 후행 PER | `28.5` |
| `forwardPE` | Double | 선행 PER | `26.3` |
| `priceToBook` | Double | PBR | `45.2` |
| `priceToSales` | Double | PSR | `7.8` |
| `bookValue` | Double | 주당 장부가치 | `3.88` |
| `earningsQuarterlyGrowth` | Double | 분기 실적 성장률 | `0.08` (8%) |

#### 2.4.10 수익 정보 (Earnings)

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| `epsTrailingTwelveMonths` | Double | 과거 12개월 EPS | `6.15` |
| `epsForward` | Double | 예상 EPS | `6.67` |
| `epsCurrentYear` | Double | 올해 EPS | `6.50` |
| `earningsTimestamp` | Long | 실적 발표 시간 | `1699545600` |
| `earningsTimestampStart` | Long | 실적 발표 시작 시간 | `1699545600` |
| `earningsTimestampEnd` | Long | 실적 발표 종료 시간 | `1699632000` |

#### 2.4.11 매출 및 수익성 (Revenue & Profitability)

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| `totalRevenue` | Long | 총 매출 | `383000000000` |
| `revenuePerShare` | Double | 주당 매출 | `23.94` |
| `returnOnAssets` | Double | ROA | `0.22` (22%) |
| `returnOnEquity` | Double | ROE | `1.47` (147%) |
| `profitMargins` | Double | 영업이익률 | `0.25` (25%) |
| `grossMargins` | Double | 매출총이익률 | `0.43` (43%) |

#### 2.4.12 재무 건전성 (Financial Health)

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| `totalCash` | Long | 총 현금 | `50000000000` |
| `totalCashPerShare` | Double | 주당 현금 | `3.12` |
| `totalDebt` | Long | 총 부채 | `110000000000` |
| `debtToEquity` | Double | 부채비율 | `1.73` (173%) |
| `currentRatio` | Double | 유동비율 | `0.98` |
| `quickRatio` | Double | 당좌비율 | `0.82` |

#### 2.4.13 성장률 (Growth Rates)

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| `revenueGrowth` | Double | 매출 성장률 | `0.05` (5%) |
| `earningsGrowth` | Double | 실적 성장률 | `0.11` (11%) |

#### 2.4.14 애널리스트 의견 (Analyst Ratings)

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| `targetHighPrice` | Double | 목표가 상한 | `220.00` |
| `targetLowPrice` | Double | 목표가 하한 | `150.00` |
| `targetMeanPrice` | Double | 목표가 평균 | `185.00` |
| `targetMedianPrice` | Double | 목표가 중앙값 | `183.00` |
| `recommendationMean` | Double | 추천 평균 (1-5 척도) | `2.1` |
| `recommendationKey` | String | 추천 등급 | `"buy"`, `"hold"`, `"sell"` |
| `numberOfAnalystOpinions` | Int | 애널리스트 수 | `38` |

#### 2.4.15 ETF 전용 필드 (ETF-Specific)

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| `ytdReturn` | Double | 연초 대비 수익률 | `0.12` (12%) |
| `threeYearAverageReturn` | Double | 3년 평균 수익률 | `0.15` (15%) |
| `fiveYearAverageReturn` | Double | 5년 평균 수익률 | `0.18` (18%) |

#### 2.4.16 뮤추얼펀드 전용 필드 (Mutual Fund-Specific)

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| `navPrice` | Double | NAV (순자산가치) | `125.43` |

#### 2.4.17 메타데이터 (Metadata)

| 필드명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| `firstTradeDateMilliseconds` | Long | 첫 거래일 (밀리초) | `345479400000` |
| `priceHint` | Int | 가격 소수점 힌트 | `2` (소수점 2자리) |
| `messageBoardId` | String | 메시지보드 ID | `"finmb_24937"` |
| `displayName` | String | 표시 이름 | `"Apple"` |
| `gmtOffSetMilliseconds` | Long | GMT 오프셋 (밀리초) | `-18000000` |
| `esgPopulated` | Boolean | ESG 데이터 유무 | `true` |
| `tradeable` | Boolean | 거래 가능 여부 | `true` |
| `cryptoTradeable` | Boolean | 암호화폐 거래 가능 여부 | `false` |

### 2.5 Nullable 필드 처리

Quote API는 자산 유형(EQUITY, ETF, INDEX 등)에 따라 일부 필드가 반환되지 않을 수 있습니다:

- **주식 (EQUITY)**: 대부분의 필드 제공
- **ETF**: `ytdReturn`, `threeYearAverageReturn` 등 ETF 전용 필드 제공
- **인덱스 (INDEX)**: `marketCap`, `sharesOutstanding` 등 미제공
- **암호화폐**: 전통적인 재무 지표 대부분 미제공

### 2.6 yfinance에서의 활용

yfinance는 Quote API를 다음과 같이 활용합니다:

1. **`_fetch_additional_info()`**: QuoteSummary와 병합하여 info 딕셔너리 보강
2. **FastInfo 클래스**: Quote API 결과를 캐싱하여 빠른 액세스 제공
3. **Retired Keys**: quoteSummary에서 제거된 필드를 Quote API로 대체

```python
# yfinance 내부 로직 (의사코드)
quoteSummary_result = fetch_quoteSummary(modules=["price", "summaryDetail"])
quote_result = fetch_quote()
merged_info = {**quoteSummary_result, **quote_result}  # Quote가 우선순위
```

---

## 3. UFC 통합 설계

### 3.1 기존 아키텍처와의 통합 방안

Quote API는 기존 UFC 아키텍처의 다음 계층에 통합됩니다:

```
YahooClient (Infrastructure)
    ↓ implements
QuoteHttpClient (Domain Interface)
    ↓ used by
QuoteService (Domain Service)
    ↓ used by
PriceApi / StockApi (Presentation)
```

### 3.2 네임스페이스 배치

Quote API는 **가격 정보**와 **기본 주식 정보** 모두를 제공하므로, 다음과 같이 배치합니다:

#### 옵션 1: 기존 네임스페이스 확장 (권장)

**장점**:
- 사용자 입장에서 일관성 있음
- 기존 API와 자연스럽게 통합

**단점**:
- 내부 로직이 복잡해질 수 있음

**배치 예시**:
```
ufc.price.getQuote(symbol)              // PriceApi에 추가
ufc.price.getQuote(symbols)             // 다중 심볼
ufc.price.getCurrentPrice(symbol)       // 기존 API (내부적으로 Quote 사용 가능)
ufc.stock.getBasicInfo(symbol)          // StockApi에 추가
```

#### 옵션 2: 새로운 네임스페이스 추가

**장점**:
- Quote API의 독립성 유지
- 명확한 API 분리

**단점**:
- 사용자가 어떤 API를 사용해야 할지 혼란

**배치 예시**:
```
ufc.quote.get(symbol)                   // 새로운 QuoteApi
ufc.quote.get(symbols)                  // 다중 심볼
```

**권장사항**: **옵션 1**을 채택하여 `PriceApi`와 `StockApi`에 Quote 기반 메서드를 추가

### 3.3 필요한 모델 클래스 목록

#### 3.3.1 Domain 계층 (Public Models)

**위치**: `/src/main/kotlin/com/ulalax/ufc/yahoo/model/`

| 클래스명 | 역할 | 주요 필드 |
|---------|------|----------|
| `QuoteData` | Quote API 전체 응답을 담는 도메인 모델 | symbol, longName, regularMarketPrice, marketCap 등 |
| `QuoteIdentification` | 기본 정보 그룹 | symbol, longName, exchange, quoteType, currency |
| `QuotePricing` | 가격 정보 그룹 | regularMarketPrice, open, high, low, volume, change |
| `QuoteExtendedHours` | 장전/장후 거래 그룹 | preMarketPrice, postMarketPrice, changes |
| `QuoteFiftyTwoWeek` | 52주 고저가 그룹 | high, low, changes, range |
| `QuoteMovingAverages` | 이동평균 그룹 | fiftyDayAverage, twoHundredDayAverage, changes |
| `QuoteVolumes` | 거래량 정보 그룹 | averageDailyVolume3Month, averageDailyVolume10Day |
| `QuoteMarketCap` | 시가총액 정보 그룹 | marketCap, sharesOutstanding |
| `QuoteDividends` | 배당 정보 그룹 | rate, yield, dividendDate, exDividendDate |
| `QuoteFinancialRatios` | 재무 비율 그룹 | trailingPE, forwardPE, priceToBook, bookValue |
| `QuoteEarnings` | 수익 정보 그룹 | epsTrailingTwelveMonths, epsForward, earningsTimestamp |
| `QuoteFinancialHealth` | 재무 건전성 그룹 | totalCash, totalDebt, debtToEquity, currentRatio |
| `QuoteGrowthRates` | 성장률 그룹 | revenueGrowth, earningsGrowth |
| `QuoteAnalystRatings` | 애널리스트 의견 그룹 | targetMeanPrice, recommendationKey, numberOfAnalysts |

**설계 원칙**:
- QuoteData는 모든 그룹을 nullable 필드로 포함 (자산 유형에 따라 일부 미제공)
- 각 그룹은 독립적인 데이터 클래스로 정의
- Serializable 구현 (캐싱 지원)

#### 3.3.2 Infrastructure 계층 (Internal Response Models)

**위치**: `/src/main/kotlin/com/ulalax/ufc/yahoo/internal/response/`

| 클래스명 | 역할 |
|---------|------|
| `QuoteResponse` | Yahoo API의 전체 응답 래퍼 |
| `QuoteResponseData` | `quoteResponse` 객체 |
| `QuoteResult` | 개별 심볼의 Quote 결과 (JSON 필드 1:1 매핑) |
| `QuoteError` | 에러 응답 |

### 3.4 API 메서드 시그니처 정의

#### 3.4.1 YahooClient (Infrastructure)

**위치**: `/src/main/kotlin/com/ulalax/ufc/yahoo/YahooClient.kt`

```kotlin
// 단일 심볼 조회
suspend fun quote(symbol: String): QuoteResult

// 다중 심볼 조회
suspend fun quote(symbols: List<String>): List<QuoteResult>
```

**동작**:
- Rate Limiting 적용
- CRUMB 토큰 획득
- HTTP 요청 수행
- 응답 파싱 및 에러 처리
- Internal `QuoteResult` 반환

#### 3.4.2 PriceApi (Presentation)

**위치**: `/src/main/kotlin/com/ulalax/ufc/yahoo/api/PriceApi.kt` (신규 또는 기존 확장)

```kotlin
// 단일 심볼의 Quote 조회
suspend fun getQuote(symbol: String): QuoteData

// 다중 심볼의 Quote 조회
suspend fun getQuote(symbols: List<String>): Map<String, QuoteData>

// 가격 정보만 추출 (간소화 버전)
suspend fun getQuotePricing(symbol: String): QuotePricing
```

**동작**:
- YahooClient 호출
- Internal `QuoteResult`를 Public `QuoteData`로 변환
- 캐싱 적용 (TTL: 60초)
- 도메인 검증 (심볼 유효성)

#### 3.4.3 StockApi (Presentation)

**위치**: `/src/main/kotlin/com/ulalax/ufc/yahoo/api/StockApi.kt` (신규 또는 기존 확장)

```kotlin
// Quote 기반 기본 정보 조회 (FastInfo 대체)
suspend fun getBasicInfo(symbol: String): QuoteIdentification

// Quote 기반 재무 비율 조회
suspend fun getFinancialRatios(symbol: String): QuoteFinancialRatios
```

**동작**:
- PriceApi.getQuote() 내부 호출
- 필요한 필드만 추출하여 반환

### 3.5 캐싱 전략

| API | 캐시 키 형식 | TTL | 이유 |
|-----|-------------|-----|------|
| `getQuote(symbol)` | `"quote:single:{symbol}"` | 60초 | 실시간 데이터 |
| `getQuote(symbols)` | `"quote:multi:{sorted_symbols}"` | 60초 | 실시간 데이터 |
| `getQuotePricing(symbol)` | `"quote:pricing:{symbol}"` | 60초 | 가격 데이터 |
| `getBasicInfo(symbol)` | `"quote:basic:{symbol}"` | 1시간 | 정적 메타데이터 |

**다중 심볼 캐싱 주의사항**:
- `["AAPL", "GOOGL"]`과 `["GOOGL", "AAPL"]`은 동일한 요청으로 간주 (정렬 필요)
- 개별 심볼 캐시와 별도로 관리

### 3.6 에러 처리 전략

Quote API는 다음 에러 상황을 처리해야 합니다:

| 에러 케이스 | ErrorCode | HTTP 상태 | 처리 방법 |
|-----------|-----------|-----------|----------|
| 잘못된 심볼 | `INVALID_SYMBOL` | 200 (빈 result) | 빈 배열 확인 후 예외 발생 |
| 심볼 미존재 | `DATA_NOT_FOUND` | 200 (빈 result) | 심볼별로 null 처리 또는 예외 |
| API 에러 | `EXTERNAL_API_ERROR` | 4xx/5xx | HTTP 상태 코드 기반 예외 |
| Rate Limit 초과 | `RATE_LIMITED` | 429 | Rate Limiter에서 자동 대기 |
| 네트워크 오류 | `NETWORK_ERROR` | - | Ktor 예외 래핑 |
| 파싱 오류 | `DATA_PARSING_ERROR` | 200 | JSON 역직렬화 실패 시 |

**다중 심볼 에러 처리**:
- 일부 심볼만 실패하는 경우: 성공한 심볼만 반환하고 실패한 심볼은 로그 기록
- 전체 실패하는 경우: 예외 발생

---

## 4. 데이터 매핑

### 4.1 Yahoo 응답 필드 → UFC 도메인 모델 매핑

#### 4.1.1 QuoteIdentification 매핑

| Yahoo 필드 | UFC 필드 | 타입 변환 | Nullable | 비고 |
|-----------|---------|----------|----------|------|
| `symbol` | `symbol` | String → String | No | 필수 필드 |
| `longName` | `longName` | String → String | Yes | 일부 심볼 미제공 |
| `shortName` | `shortName` | String → String | Yes | - |
| `exchange` | `exchange` | String → String | Yes | - |
| `exchangeTimezoneName` | `timezoneName` | String → String | Yes | - |
| `exchangeTimezoneShortName` | `timezoneShortName` | String → String | Yes | - |
| `quoteType` | `quoteType` | String → String | Yes | EQUITY, ETF, INDEX 등 |
| `currency` | `currency` | String → String | Yes | - |
| `market` | `market` | String → String | Yes | - |

#### 4.1.2 QuotePricing 매핑

| Yahoo 필드 | UFC 필드 | 타입 변환 | Nullable | 비고 |
|-----------|---------|----------|----------|------|
| `regularMarketPrice` | `price` | Double → Double | No | 현재가 (필수) |
| `regularMarketOpen` | `open` | Double → Double | Yes | - |
| `regularMarketDayHigh` | `dayHigh` | Double → Double | Yes | - |
| `regularMarketDayLow` | `dayLow` | Double → Double | Yes | - |
| `regularMarketVolume` | `volume` | Long → Long | Yes | - |
| `regularMarketPreviousClose` | `previousClose` | Double → Double | Yes | - |
| `regularMarketChange` | `change` | Double → Double | Yes | - |
| `regularMarketChangePercent` | `changePercent` | Double → Double | Yes | 백분율 (%) |
| `regularMarketTime` | `marketTime` | Long → Instant | Yes | Unix timestamp → Instant |

#### 4.1.3 QuoteExtendedHours 매핑

| Yahoo 필드 | UFC 필드 | 타입 변환 | Nullable | 비고 |
|-----------|---------|----------|----------|------|
| `preMarketPrice` | `preMarketPrice` | Double → Double | Yes | - |
| `preMarketChange` | `preMarketChange` | Double → Double | Yes | - |
| `preMarketChangePercent` | `preMarketChangePercent` | Double → Double | Yes | - |
| `preMarketTime` | `preMarketTime` | Long → Instant | Yes | - |
| `postMarketPrice` | `postMarketPrice` | Double → Double | Yes | - |
| `postMarketChange` | `postMarketChange` | Double → Double | Yes | - |
| `postMarketChangePercent` | `postMarketChangePercent` | Double → Double | Yes | - |
| `postMarketTime` | `postMarketTime` | Long → Instant | Yes | - |

#### 4.1.4 QuoteFiftyTwoWeek 매핑

| Yahoo 필드 | UFC 필드 | 타입 변환 | Nullable | 비고 |
|-----------|---------|----------|----------|------|
| `fiftyTwoWeekHigh` | `high` | Double → Double | Yes | - |
| `fiftyTwoWeekLow` | `low` | Double → Double | Yes | - |
| `fiftyTwoWeekHighChange` | `highChange` | Double → Double | Yes | - |
| `fiftyTwoWeekLowChange` | `lowChange` | Double → Double | Yes | - |
| `fiftyTwoWeekHighChangePercent` | `highChangePercent` | Double → Double | Yes | - |
| `fiftyTwoWeekLowChangePercent` | `lowChangePercent` | Double → Double | Yes | - |
| `fiftyTwoWeekRange` | `range` | String → String | Yes | "low - high" 형식 |

#### 4.1.5 QuoteMovingAverages 매핑

| Yahoo 필드 | UFC 필드 | 타입 변환 | Nullable | 비고 |
|-----------|---------|----------|----------|------|
| `fiftyDayAverage` | `fiftyDayAverage` | Double → Double | Yes | - |
| `fiftyDayAverageChange` | `fiftyDayChange` | Double → Double | Yes | - |
| `fiftyDayAverageChangePercent` | `fiftyDayChangePercent` | Double → Double | Yes | - |
| `twoHundredDayAverage` | `twoHundredDayAverage` | Double → Double | Yes | - |
| `twoHundredDayAverageChange` | `twoHundredDayChange` | Double → Double | Yes | - |
| `twoHundredDayAverageChangePercent` | `twoHundredDayChangePercent` | Double → Double | Yes | - |

#### 4.1.6 QuoteDividends 매핑

| Yahoo 필드 | UFC 필드 | 타입 변환 | Nullable | 비고 |
|-----------|---------|----------|----------|------|
| `dividendRate` | `annualRate` | Double → Double | Yes | 연간 배당금 |
| `dividendYield` | `yield` | Double → Double | Yes | 배당수익률 (소수) |
| `dividendDate` | `dividendDate` | Long → LocalDate | Yes | Unix timestamp → LocalDate |
| `exDividendDate` | `exDividendDate` | Long → LocalDate | Yes | - |
| `trailingAnnualDividendRate` | `trailingRate` | Double → Double | Yes | - |
| `trailingAnnualDividendYield` | `trailingYield` | Double → Double | Yes | - |

#### 4.1.7 QuoteFinancialRatios 매핑

| Yahoo 필드 | UFC 필드 | 타입 변환 | Nullable | 비고 |
|-----------|---------|----------|----------|------|
| `trailingPE` | `trailingPE` | Double → Double | Yes | PER (후행) |
| `forwardPE` | `forwardPE` | Double → Double | Yes | PER (선행) |
| `priceToBook` | `priceToBook` | Double → Double | Yes | PBR |
| `priceToSales` | `priceToSales` | Double → Double | Yes | PSR |
| `bookValue` | `bookValue` | Double → Double | Yes | 주당 장부가치 |

### 4.2 타입 변환 규칙

#### 4.2.1 Unix Timestamp → Instant

```kotlin
// Yahoo: Long (Unix timestamp in seconds)
// UFC: kotlinx.datetime.Instant

fun Long.toInstant(): Instant = Instant.fromEpochSeconds(this)
```

#### 4.2.2 Unix Timestamp → LocalDate

```kotlin
// Yahoo: Long (Unix timestamp in seconds)
// UFC: kotlinx.datetime.LocalDate

fun Long.toLocalDate(timezone: TimeZone): LocalDate {
    return Instant.fromEpochSeconds(this)
        .toLocalDateTime(timezone)
        .date
}
```

#### 4.2.3 백분율 변환

Yahoo API는 백분율을 다음과 같이 반환:
- `changePercent`: `0.82` (의미: 0.82%)
- `dividendYield`: `0.0055` (의미: 0.55%)

UFC에서는 일관성을 위해 **소수 형태**로 유지하고, 필요 시 사용자가 `* 100` 변환

#### 4.2.4 Nullable 처리

모든 필드는 기본적으로 nullable로 처리:
- Yahoo API가 필드를 제공하지 않는 경우 null
- 값이 없는 경우 (예: 배당금이 없는 종목) null

### 4.3 Nullable 처리 전략

#### 4.3.1 필수 필드

다음 필드는 **반드시 존재해야 하며**, 없으면 예외 발생:
- `symbol`
- `regularMarketPrice`

#### 4.3.2 선택 필드

대부분의 필드는 선택 필드로 처리:
- Kotlin의 nullable 타입 활용 (`String?`, `Double?`)
- 기본값 제공 없음 (null은 null로 유지)

#### 4.3.3 그룹 단위 Nullable

특정 자산 유형에서 제공되지 않는 그룹은 전체를 null로 처리:
- 예: INDEX 타입의 경우 `QuoteDividends` 전체가 null

---

## 5. 에러 처리

### 5.1 예상되는 에러 케이스

#### 5.1.1 잘못된 심볼

**시나리오**: 존재하지 않는 심볼 조회 (예: "INVALID123")

**Yahoo 응답**:
```json
{
  "quoteResponse": {
    "result": [],
    "error": null
  }
}
```

**UFC 처리**:
- ErrorCode: `INVALID_SYMBOL`
- 메시지: "유효하지 않은 심볼입니다: INVALID123"
- HTTP 상태: 200 (Yahoo는 에러여도 200 반환)

#### 5.1.2 일부 심볼 실패 (다중 조회)

**시나리오**: `["AAPL", "INVALID", "GOOGL"]` 조회

**Yahoo 응답**:
```json
{
  "quoteResponse": {
    "result": [
      {"symbol": "AAPL", ...},
      {"symbol": "GOOGL", ...}
    ],
    "error": null
  }
}
```

**UFC 처리**:
- 성공한 심볼만 반환: `{"AAPL": QuoteData, "GOOGL": QuoteData}`
- 실패한 심볼은 로그 경고: `"Quote data not found for symbol: INVALID"`
- 예외 발생하지 않음 (부분 성공 허용)

#### 5.1.3 Rate Limit 초과

**시나리오**: 초당 50회 이상 요청

**Yahoo 응답**:
```
HTTP 429 Too Many Requests
```

**UFC 처리**:
- ErrorCode: `RATE_LIMITED`
- Rate Limiter에서 자동 대기 (사용자에게 투명)
- 대기 실패 시 예외 발생

#### 5.1.4 네트워크 오류

**시나리오**: 인터넷 연결 끊김, DNS 실패 등

**Ktor 예외**: `IOException`, `UnresolvedAddressException`

**UFC 처리**:
- ErrorCode: `NETWORK_ERROR`
- 메시지: "네트워크 오류: {원본 메시지}"
- 원본 예외 체인 유지

#### 5.1.5 파싱 오류

**시나리오**: Yahoo API 응답 형식 변경 또는 손상된 JSON

**Ktor 예외**: `SerializationException`

**UFC 처리**:
- ErrorCode: `DATA_PARSING_ERROR`
- 메시지: "응답 파싱 실패: {원본 메시지}"
- 원본 응답 로그 기록 (디버깅용)

#### 5.1.6 CRUMB 획득 실패

**시나리오**: Yahoo 인증 시스템 변경 또는 쿠키 만료

**UFC 처리**:
- ErrorCode: `AUTH_FAILED`
- 메시지: "Yahoo Finance 인증 실패"
- CRUMB 재획득 시도 (최대 3회)

### 5.2 에러 응답 구조

Quote API의 에러 응답은 다음과 같이 정의됩니다:

#### 5.2.1 QuoteError (Internal Response)

**위치**: `/src/main/kotlin/com/ulalax/ufc/yahoo/internal/response/QuoteResponse.kt`

```kotlin
@Serializable
data class QuoteError(
    val code: String?,
    val description: String?
)
```

#### 5.2.2 UFC Exception 구조

UFC는 모든 에러를 `ApiException`으로 통합:

```kotlin
throw ApiException(
    errorCode = ErrorCode.INVALID_SYMBOL,
    message = "유효하지 않은 심볼입니다: $symbol",
    metadata = mapOf("symbol" to symbol)
)
```

### 5.3 재시도 전략

| 에러 타입 | 재시도 | 최대 횟수 | 대기 시간 | 비고 |
|----------|-------|----------|----------|------|
| Rate Limit (429) | Yes | 무제한 | Rate Limiter 자동 | - |
| Network Error | Yes | 3회 | Exponential backoff (1s, 2s, 4s) | - |
| CRUMB 실패 | Yes | 3회 | 즉시 재시도 | CRUMB 재획득 |
| 파싱 오류 | No | - | - | API 변경 가능성 |
| 잘못된 심볼 | No | - | - | 사용자 입력 오류 |

---

## 6. 테스트 전략

### 6.1 단위 테스트 시나리오

#### 6.1.1 YahooClient.quote() 테스트

**위치**: `/src/test/kotlin/com/ulalax/ufc/yahoo/YahooClientTest.kt`

| 테스트 케이스 | 목적 | Mock 응답 | 검증 항목 |
|-------------|------|-----------|----------|
| `단일 심볼 정상 조회` | 기본 기능 검증 | 유효한 QuoteResponse | symbol, regularMarketPrice 일치 |
| `다중 심볼 정상 조회` | 다중 심볼 파싱 | 2개 심볼 포함 | result 크기 = 2, 모든 심볼 포함 |
| `빈 결과 처리` | 잘못된 심볼 | result = [] | ApiException (INVALID_SYMBOL) |
| `null 필드 처리` | Nullable 필드 검증 | 일부 필드 null | null 필드가 예외 발생 안 함 |
| `HTTP 에러 처리` | 네트워크 에러 | HTTP 500 | ApiException (EXTERNAL_API_ERROR) |
| `파싱 에러 처리` | 잘못된 JSON | 손상된 JSON | ApiException (DATA_PARSING_ERROR) |
| `Rate Limit 적용` | Rate Limiter 호출 확인 | - | rateLimiter.acquire() 호출됨 |
| `CRUMB 토큰 사용` | 인증 검증 | - | crumb 파라미터 포함 |

#### 6.1.2 QuoteData 변환 테스트

**위치**: `/src/test/kotlin/com/ulalax/ufc/yahoo/model/QuoteDataTest.kt`

| 테스트 케이스 | 목적 | 검증 항목 |
|-------------|------|----------|
| `QuoteResult → QuoteData 변환` | 타입 변환 검증 | 모든 필드 정확히 매핑 |
| `Unix Timestamp → Instant` | 시간 변환 | Instant 정확성 |
| `Unix Timestamp → LocalDate` | 날짜 변환 | LocalDate 정확성 |
| `Nullable 필드 변환` | null 처리 | null은 null로 유지 |

#### 6.1.3 PriceApi.getQuote() 테스트

**위치**: `/src/test/kotlin/com/ulalax/ufc/api/PriceApiTest.kt`

| 테스트 케이스 | 목적 | Mock 응답 | 검증 항목 |
|-------------|------|-----------|----------|
| `단일 심볼 Quote 조회` | API 통합 | YahooClient.quote() | QuoteData 반환 |
| `다중 심볼 Quote 조회` | 다중 심볼 | YahooClient.quote() | Map<String, QuoteData> |
| `캐싱 동작 확인` | 캐시 히트 | - | 2번째 호출 시 HTTP 미호출 |
| `캐싱 TTL 만료` | 캐시 미스 | - | 60초 후 재호출 |

### 6.2 통합 테스트 시나리오

#### 6.2.1 실제 Yahoo API 호출 테스트

**위치**: `/src/integrationTest/kotlin/com/ulalax/ufc/yahoo/YahooClientIntegrationTest.kt`

| 테스트 케이스 | 심볼 | 검증 항목 |
|-------------|------|----------|
| `주식 Quote 조회` | AAPL | symbol, regularMarketPrice, marketCap 존재 |
| `ETF Quote 조회` | SPY | ytdReturn 존재 |
| `인덱스 Quote 조회` | ^GSPC | marketCap 미존재 (null) |
| `암호화폐 Quote 조회` | BTC-USD | quoteType = "CRYPTOCURRENCY" |
| `다중 심볼 조회` | AAPL,GOOGL,MSFT | 3개 모두 반환 |
| `잘못된 심볼 조회` | INVALID123 | ApiException (INVALID_SYMBOL) |

#### 6.2.2 End-to-End 테스트

**위치**: `/src/integrationTest/kotlin/com/ulalax/ufc/UfcE2ETest.kt`

| 테스트 케이스 | 시나리오 | 검증 항목 |
|-------------|---------|----------|
| `UFC를 통한 Quote 조회` | ufc.price.getQuote("AAPL") | QuoteData 반환 |
| `QuoteSummary와 비교` | Quote vs QuoteSummary | regularMarketPrice 일치 |
| `캐싱 동작 확인` | 연속 2회 호출 | 2번째 호출이 더 빠름 |

### 6.3 성능 테스트 시나리오

| 테스트 케이스 | 목표 | 측정 항목 |
|-------------|------|----------|
| `단일 심볼 응답 시간` | < 500ms | P50, P95, P99 |
| `다중 심볼 (10개) 응답 시간` | < 1000ms | P50, P95, P99 |
| `캐시 히트 응답 시간` | < 10ms | P50, P95, P99 |
| `Rate Limit 처리` | 초당 50회 요청 | 모든 요청 성공 |

### 6.4 테스트 데이터

#### 6.4.1 Mock QuoteResponse (단일 심볼)

```json
{
  "quoteResponse": {
    "result": [
      {
        "symbol": "AAPL",
        "longName": "Apple Inc.",
        "shortName": "Apple",
        "regularMarketPrice": 175.43,
        "regularMarketOpen": 174.50,
        "regularMarketDayHigh": 176.20,
        "regularMarketDayLow": 174.30,
        "regularMarketVolume": 58000000,
        "regularMarketPreviousClose": 174.00,
        "regularMarketChange": 1.43,
        "regularMarketChangePercent": 0.82,
        "marketCap": 2800000000000,
        "trailingPE": 28.5,
        "dividendRate": 0.96,
        "dividendYield": 0.0055
      }
    ],
    "error": null
  }
}
```

#### 6.4.2 Mock QuoteResponse (다중 심볼)

```json
{
  "quoteResponse": {
    "result": [
      {
        "symbol": "AAPL",
        "regularMarketPrice": 175.43
      },
      {
        "symbol": "GOOGL",
        "regularMarketPrice": 140.25
      }
    ],
    "error": null
  }
}
```

#### 6.4.3 Mock QuoteResponse (에러)

```json
{
  "quoteResponse": {
    "result": [],
    "error": {
      "code": "Not Found",
      "description": "No data found for symbol INVALID"
    }
  }
}
```

---

## 7. 구현 우선순위

### 7.1 Phase 1: 기본 기능 (필수)

**목표**: 단일 심볼 Quote 조회 기능 구현

**작업 항목**:
1. Internal Response Models 정의 (`QuoteResponse`, `QuoteResult`)
2. YahooClient.quote(symbol) 구현
3. 핵심 Domain Models 정의 (`QuoteData`, `QuotePricing`, `QuoteIdentification`)
4. YahooApiUrls에 QUOTE 엔드포인트 추가
5. 단위 테스트 작성 (YahooClientTest)
6. 통합 테스트 작성 (실제 API 호출)

**검증 기준**:
- `YahooClient.quote("AAPL")` 정상 동작
- 모든 핵심 필드 파싱 성공
- 단위 테스트 커버리지 > 80%

### 7.2 Phase 2: 다중 심볼 지원 (중요)

**목표**: 다중 심볼 조회 및 캐싱 구현

**작업 항목**:
1. YahooClient.quote(symbols) 구현
2. PriceApi.getQuote(symbol) 구현
3. PriceApi.getQuote(symbols) 구현
4. 캐싱 로직 추가 (CacheHelper 활용)
5. 다중 심볼 에러 처리 (일부 실패 시나리오)
6. 통합 테스트 확장

**검증 기준**:
- 다중 심볼 조회 성공
- 캐싱 동작 확인
- 일부 심볼 실패 시 나머지 반환

### 7.3 Phase 3: 전체 필드 지원 (선택)

**목표**: 모든 Quote 필드 매핑 및 그룹화

**작업 항목**:
1. 전체 Domain Models 정의 (QuoteDividends, QuoteFinancialRatios 등)
2. QuoteData에 모든 그룹 추가
3. 타입 변환 유틸리티 함수 (Instant, LocalDate)
4. 자산 유형별 테스트 (ETF, INDEX, CRYPTO)
5. 문서화 (KDoc)

**검증 기준**:
- 80개 이상 필드 매핑 완료
- 모든 자산 유형 테스트 통과

### 7.4 Phase 4: 고급 기능 (선택)

**목표**: 성능 최적화 및 편의 기능

**작업 항목**:
1. StockApi.getBasicInfo(symbol) 구현 (FastInfo 대체)
2. PriceApi.getQuotePricing(symbol) 구현 (간소화 버전)
3. 성능 벤치마크 테스트
4. QuoteSummary와의 통합 (중복 필드 제거)
5. 사용자 가이드 작성

**검증 기준**:
- getBasicInfo() 응답 시간 < 300ms
- QuoteSummary 대비 30% 이상 빠름

---

## 부록

### A. 참고 링크

- yfinance Quote 구현: `/home/ulalax/project/kairos/yfinance/yfinance/scrapers/quote.py`
- UFC Architecture: `/home/ulalax/project/kairos/ufc/doc/ARCHITECTURE.md`
- UFC YahooClient: `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/yahoo/YahooClient.kt`

### B. 용어 정리

| 용어 | 설명 |
|-----|------|
| **Quote** | Yahoo Finance의 실시간 시장 데이터 API |
| **QuoteSummary** | Yahoo Finance의 상세 모듈별 정보 API |
| **CRUMB** | Yahoo Finance 인증 토큰 |
| **Fast Info** | yfinance의 빠른 정보 조회 기능 |
| **Nullable** | Kotlin의 null 가능 타입 (`String?`, `Double?`) |

### C. 변경 이력

| 버전 | 날짜 | 변경 내용 |
|-----|------|----------|
| 1.0 | 2025-12-05 | 초안 작성 |

---

**작성자**: Claude (Anthropic AI)
**검토자**: -
**승인자**: -
