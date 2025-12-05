# Quote API 기술 명세서

> **Version**: 2.0
> **작성일**: 2025-12-05

---

## 1. 개요

### 1.1 API 목적

Yahoo Finance Quote API를 통해 **실시간 시장 데이터와 기본 주식 정보**를 조회한다.

| 항목 | 값 |
|-----|---|
| 엔드포인트 | `GET https://query1.finance.yahoo.com/v7/finance/quote` |
| HTTP 메서드 | GET |
| 인증 | CRUMB 토큰 필요 |
| 다중 심볼 | 지원 (쉼표로 구분) |

### 1.2 QuoteSummary API와의 차이점

| 구분 | Quote API | QuoteSummary API |
|------|-----------|-----------------|
| 목적 | 실시간 시장 데이터 및 기본 정보 | 상세한 모듈별 금융 정보 |
| 응답 속도 | 빠름 | 상대적으로 느림 |
| 데이터 범위 | 넓고 얕음 (약 80-100개 필드) | 좁고 깊음 (모듈별) |
| 모듈 선택 | 불가능 (고정 응답) | 가능 |
| 다중 심볼 | 지원 | 미지원 |

### 1.3 제공 데이터

| 카테고리 | 주요 필드 |
|---------|----------|
| 기본 정보 | symbol, longName, exchange, quoteType, currency |
| 가격 정보 | regularMarketPrice, open, high, low, volume, change |
| 장전/장후 거래 | preMarketPrice, postMarketPrice |
| 52주 고저가 | fiftyTwoWeekHigh, fiftyTwoWeekLow |
| 이동평균 | fiftyDayAverage, twoHundredDayAverage |
| 시가총액 | marketCap, sharesOutstanding |
| 배당 정보 | dividendRate, dividendYield, dividendDate |
| 재무 비율 | trailingPE, forwardPE, priceToBook |
| 수익 정보 | epsTrailingTwelveMonths, epsForward |
| 재무 건전성 | totalCash, totalDebt, debtToEquity |
| 애널리스트 의견 | targetMeanPrice, recommendationKey |

---

## 2. 데이터 소스 분석

### 2.1 요청 파라미터

| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---------|------|-----|------|------|
| symbols | String | Yes | 조회할 심볼 (쉼표 구분) | "AAPL" 또는 "AAPL,GOOGL,MSFT" |
| formatted | String | No | 포맷팅 여부 | "false" (기본값: "true") |
| crumb | String | Yes | CRUMB 인증 토큰 | "dFhd8fj..." |

### 2.2 응답 구조

#### 정상 응답

| 경로 | 타입 | 설명 |
|-----|------|------|
| quoteResponse.result | Array | Quote 결과 배열 |
| quoteResponse.error | Object? | 에러 객체 (정상 시 null) |

#### 에러 응답

| 경로 | 타입 | 설명 |
|-----|------|------|
| quoteResponse.result | Array | 빈 배열 |
| quoteResponse.error.code | String? | 에러 코드 |
| quoteResponse.error.description | String? | 에러 설명 |

### 2.3 응답 필드 분류

#### 기본 정보 (Identification)

| Yahoo 필드 | 타입 | Nullable | 설명 |
|-----------|------|----------|------|
| symbol | String | No | 티커 심볼 |
| longName | String | Yes | 정식 명칭 |
| shortName | String | Yes | 약식 명칭 |
| exchange | String | Yes | 거래소 코드 |
| exchangeTimezoneName | String | Yes | 거래소 시간대 |
| exchangeTimezoneShortName | String | Yes | 시간대 약어 |
| quoteType | String | Yes | 자산 유형 (EQUITY, ETF, INDEX) |
| currency | String | Yes | 통화 코드 |
| market | String | Yes | 시장 분류 |

#### 가격 정보 (Pricing)

| Yahoo 필드 | 타입 | Nullable | 설명 |
|-----------|------|----------|------|
| regularMarketPrice | Double | No | 현재가 |
| regularMarketOpen | Double | Yes | 시가 |
| regularMarketDayHigh | Double | Yes | 당일 고가 |
| regularMarketDayLow | Double | Yes | 당일 저가 |
| regularMarketVolume | Long | Yes | 당일 거래량 |
| regularMarketPreviousClose | Double | Yes | 전일 종가 |
| regularMarketChange | Double | Yes | 가격 변화 |
| regularMarketChangePercent | Double | Yes | 변화율 (%) |
| regularMarketTime | Long | Yes | 시장 시간 (Unix timestamp) |

#### 장전/장후 거래 (Extended Hours)

| Yahoo 필드 | 타입 | Nullable | 설명 |
|-----------|------|----------|------|
| preMarketPrice | Double | Yes | 장전 가격 |
| preMarketChange | Double | Yes | 장전 변화 |
| preMarketChangePercent | Double | Yes | 장전 변화율 |
| preMarketTime | Long | Yes | 장전 시간 |
| postMarketPrice | Double | Yes | 장후 가격 |
| postMarketChange | Double | Yes | 장후 변화 |
| postMarketChangePercent | Double | Yes | 장후 변화율 |
| postMarketTime | Long | Yes | 장후 시간 |

#### 52주 고저가 (52-Week Range)

| Yahoo 필드 | 타입 | Nullable | 설명 |
|-----------|------|----------|------|
| fiftyTwoWeekHigh | Double | Yes | 52주 최고가 |
| fiftyTwoWeekLow | Double | Yes | 52주 최저가 |
| fiftyTwoWeekHighChange | Double | Yes | 최고가 대비 변화 |
| fiftyTwoWeekLowChange | Double | Yes | 최저가 대비 변화 |
| fiftyTwoWeekHighChangePercent | Double | Yes | 최고가 대비 변화율 |
| fiftyTwoWeekLowChangePercent | Double | Yes | 최저가 대비 변화율 |
| fiftyTwoWeekRange | String | Yes | 52주 범위 ("low - high") |

#### 이동평균 (Moving Averages)

| Yahoo 필드 | 타입 | Nullable | 설명 |
|-----------|------|----------|------|
| fiftyDayAverage | Double | Yes | 50일 이동평균 |
| fiftyDayAverageChange | Double | Yes | 50일 평균 대비 변화 |
| fiftyDayAverageChangePercent | Double | Yes | 50일 평균 대비 변화율 |
| twoHundredDayAverage | Double | Yes | 200일 이동평균 |
| twoHundredDayAverageChange | Double | Yes | 200일 평균 대비 변화 |
| twoHundredDayAverageChangePercent | Double | Yes | 200일 평균 대비 변화율 |

#### 거래량 평균 (Volume Averages)

| Yahoo 필드 | 타입 | Nullable | 설명 |
|-----------|------|----------|------|
| averageDailyVolume3Month | Long | Yes | 3개월 평균 일일 거래량 |
| averageDailyVolume10Day | Long | Yes | 10일 평균 일일 거래량 |

#### 시가총액 및 발행주식수 (Market Cap)

| Yahoo 필드 | 타입 | Nullable | 설명 |
|-----------|------|----------|------|
| marketCap | Long | Yes | 시가총액 |
| sharesOutstanding | Long | Yes | 발행주식수 |

#### 배당 정보 (Dividends)

| Yahoo 필드 | 타입 | Nullable | 설명 |
|-----------|------|----------|------|
| dividendRate | Double | Yes | 연간 배당금 |
| dividendYield | Double | Yes | 배당수익률 |
| dividendDate | Long | Yes | 배당 지급일 (Unix timestamp) |
| exDividendDate | Long | Yes | 배당락일 (Unix timestamp) |
| trailingAnnualDividendRate | Double | Yes | 과거 12개월 배당금 |
| trailingAnnualDividendYield | Double | Yes | 과거 12개월 배당수익률 |

#### 재무 비율 (Financial Ratios)

| Yahoo 필드 | 타입 | Nullable | 설명 |
|-----------|------|----------|------|
| trailingPE | Double | Yes | 후행 PER |
| forwardPE | Double | Yes | 선행 PER |
| priceToBook | Double | Yes | PBR |
| priceToSales | Double | Yes | PSR |
| bookValue | Double | Yes | 주당 장부가치 |
| earningsQuarterlyGrowth | Double | Yes | 분기 실적 성장률 |

#### 수익 정보 (Earnings)

| Yahoo 필드 | 타입 | Nullable | 설명 |
|-----------|------|----------|------|
| epsTrailingTwelveMonths | Double | Yes | 과거 12개월 EPS |
| epsForward | Double | Yes | 예상 EPS |
| epsCurrentYear | Double | Yes | 올해 EPS |
| earningsTimestamp | Long | Yes | 실적 발표 시간 |
| earningsTimestampStart | Long | Yes | 실적 발표 시작 시간 |
| earningsTimestampEnd | Long | Yes | 실적 발표 종료 시간 |

#### 매출 및 수익성 (Revenue & Profitability)

| Yahoo 필드 | 타입 | Nullable | 설명 |
|-----------|------|----------|------|
| totalRevenue | Long | Yes | 총 매출 |
| revenuePerShare | Double | Yes | 주당 매출 |
| returnOnAssets | Double | Yes | ROA |
| returnOnEquity | Double | Yes | ROE |
| profitMargins | Double | Yes | 영업이익률 |
| grossMargins | Double | Yes | 매출총이익률 |

#### 재무 건전성 (Financial Health)

| Yahoo 필드 | 타입 | Nullable | 설명 |
|-----------|------|----------|------|
| totalCash | Long | Yes | 총 현금 |
| totalCashPerShare | Double | Yes | 주당 현금 |
| totalDebt | Long | Yes | 총 부채 |
| debtToEquity | Double | Yes | 부채비율 |
| currentRatio | Double | Yes | 유동비율 |
| quickRatio | Double | Yes | 당좌비율 |

#### 성장률 (Growth Rates)

| Yahoo 필드 | 타입 | Nullable | 설명 |
|-----------|------|----------|------|
| revenueGrowth | Double | Yes | 매출 성장률 |
| earningsGrowth | Double | Yes | 실적 성장률 |

#### 애널리스트 의견 (Analyst Ratings)

| Yahoo 필드 | 타입 | Nullable | 설명 |
|-----------|------|----------|------|
| targetHighPrice | Double | Yes | 목표가 상한 |
| targetLowPrice | Double | Yes | 목표가 하한 |
| targetMeanPrice | Double | Yes | 목표가 평균 |
| targetMedianPrice | Double | Yes | 목표가 중앙값 |
| recommendationMean | Double | Yes | 추천 평균 (1-5 척도) |
| recommendationKey | String | Yes | 추천 등급 (buy/hold/sell) |
| numberOfAnalystOpinions | Int | Yes | 애널리스트 수 |

### 2.4 자산 유형별 필드 제공 여부

| 필드 그룹 | EQUITY | ETF | INDEX | CRYPTOCURRENCY |
|---------|--------|-----|-------|----------------|
| 기본 정보 | Yes | Yes | Yes | Yes |
| 가격 정보 | Yes | Yes | Yes | Yes |
| 장전/장후 거래 | Yes | Yes | Partial | Yes |
| 52주 고저가 | Yes | Yes | Yes | Yes |
| 이동평균 | Yes | Yes | Yes | Yes |
| 시가총액 | Yes | Yes | No | Yes |
| 배당 정보 | Partial | Partial | No | No |
| 재무 비율 | Yes | Partial | No | No |
| 수익 정보 | Yes | No | No | No |
| 재무 건전성 | Yes | No | No | No |
| 애널리스트 의견 | Yes | Partial | No | Partial |

---

## 3. 데이터/모델 설계

### 3.1 Domain 모델

#### QuoteData

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| identification | QuoteIdentification | Yes | 기본 정보 |
| pricing | QuotePricing | Yes | 가격 정보 |
| extendedHours | QuoteExtendedHours | Yes | 장전/장후 거래 |
| fiftyTwoWeek | QuoteFiftyTwoWeek | Yes | 52주 고저가 |
| movingAverages | QuoteMovingAverages | Yes | 이동평균 |
| volumes | QuoteVolumes | Yes | 거래량 정보 |
| marketCap | QuoteMarketCap | Yes | 시가총액 정보 |
| dividends | QuoteDividends | Yes | 배당 정보 |
| financialRatios | QuoteFinancialRatios | Yes | 재무 비율 |
| earnings | QuoteEarnings | Yes | 수익 정보 |
| revenue | QuoteRevenue | Yes | 매출 및 수익성 |
| financialHealth | QuoteFinancialHealth | Yes | 재무 건전성 |
| growthRates | QuoteGrowthRates | Yes | 성장률 |
| analystRatings | QuoteAnalystRatings | Yes | 애널리스트 의견 |

#### QuoteIdentification

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| symbol | String | No | 티커 심볼 |
| longName | String | Yes | 정식 명칭 |
| shortName | String | Yes | 약식 명칭 |
| exchange | String | Yes | 거래소 코드 |
| timezoneName | String | Yes | 거래소 시간대 |
| timezoneShortName | String | Yes | 시간대 약어 |
| quoteType | String | Yes | 자산 유형 |
| currency | String | Yes | 통화 코드 |
| market | String | Yes | 시장 분류 |

#### QuotePricing

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| price | Double | No | 현재가 |
| open | Double | Yes | 시가 |
| dayHigh | Double | Yes | 당일 고가 |
| dayLow | Double | Yes | 당일 저가 |
| volume | Long | Yes | 당일 거래량 |
| previousClose | Double | Yes | 전일 종가 |
| change | Double | Yes | 가격 변화 |
| changePercent | Double | Yes | 변화율 (%) |
| marketTime | Instant | Yes | 시장 시간 |

#### QuoteExtendedHours

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| preMarketPrice | Double | Yes | 장전 가격 |
| preMarketChange | Double | Yes | 장전 변화 |
| preMarketChangePercent | Double | Yes | 장전 변화율 |
| preMarketTime | Instant | Yes | 장전 시간 |
| postMarketPrice | Double | Yes | 장후 가격 |
| postMarketChange | Double | Yes | 장후 변화 |
| postMarketChangePercent | Double | Yes | 장후 변화율 |
| postMarketTime | Instant | Yes | 장후 시간 |

#### QuoteFiftyTwoWeek

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| high | Double | Yes | 52주 최고가 |
| low | Double | Yes | 52주 최저가 |
| highChange | Double | Yes | 최고가 대비 변화 |
| lowChange | Double | Yes | 최저가 대비 변화 |
| highChangePercent | Double | Yes | 최고가 대비 변화율 |
| lowChangePercent | Double | Yes | 최저가 대비 변화율 |
| range | String | Yes | 52주 범위 |

#### QuoteMovingAverages

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| fiftyDayAverage | Double | Yes | 50일 이동평균 |
| fiftyDayChange | Double | Yes | 50일 평균 대비 변화 |
| fiftyDayChangePercent | Double | Yes | 50일 평균 대비 변화율 |
| twoHundredDayAverage | Double | Yes | 200일 이동평균 |
| twoHundredDayChange | Double | Yes | 200일 평균 대비 변화 |
| twoHundredDayChangePercent | Double | Yes | 200일 평균 대비 변화율 |

#### QuoteVolumes

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| averageDailyVolume3Month | Long | Yes | 3개월 평균 일일 거래량 |
| averageDailyVolume10Day | Long | Yes | 10일 평균 일일 거래량 |

#### QuoteMarketCap

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| marketCap | Long | Yes | 시가총액 |
| sharesOutstanding | Long | Yes | 발행주식수 |

#### QuoteDividends

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| annualRate | Double | Yes | 연간 배당금 |
| yield | Double | Yes | 배당수익률 |
| dividendDate | LocalDate | Yes | 배당 지급일 |
| exDividendDate | LocalDate | Yes | 배당락일 |
| trailingRate | Double | Yes | 과거 12개월 배당금 |
| trailingYield | Double | Yes | 과거 12개월 배당수익률 |

#### QuoteFinancialRatios

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| trailingPE | Double | Yes | 후행 PER |
| forwardPE | Double | Yes | 선행 PER |
| priceToBook | Double | Yes | PBR |
| priceToSales | Double | Yes | PSR |
| bookValue | Double | Yes | 주당 장부가치 |
| earningsQuarterlyGrowth | Double | Yes | 분기 실적 성장률 |

#### QuoteEarnings

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| epsTrailingTwelveMonths | Double | Yes | 과거 12개월 EPS |
| epsForward | Double | Yes | 예상 EPS |
| epsCurrentYear | Double | Yes | 올해 EPS |
| earningsTimestamp | Instant | Yes | 실적 발표 시간 |
| earningsTimestampStart | Instant | Yes | 실적 발표 시작 시간 |
| earningsTimestampEnd | Instant | Yes | 실적 발표 종료 시간 |

#### QuoteRevenue

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| totalRevenue | Long | Yes | 총 매출 |
| revenuePerShare | Double | Yes | 주당 매출 |
| returnOnAssets | Double | Yes | ROA |
| returnOnEquity | Double | Yes | ROE |
| profitMargins | Double | Yes | 영업이익률 |
| grossMargins | Double | Yes | 매출총이익률 |

#### QuoteFinancialHealth

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| totalCash | Long | Yes | 총 현금 |
| totalCashPerShare | Double | Yes | 주당 현금 |
| totalDebt | Long | Yes | 총 부채 |
| debtToEquity | Double | Yes | 부채비율 |
| currentRatio | Double | Yes | 유동비율 |
| quickRatio | Double | Yes | 당좌비율 |

#### QuoteGrowthRates

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| revenueGrowth | Double | Yes | 매출 성장률 |
| earningsGrowth | Double | Yes | 실적 성장률 |

#### QuoteAnalystRatings

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| targetHighPrice | Double | Yes | 목표가 상한 |
| targetLowPrice | Double | Yes | 목표가 하한 |
| targetMeanPrice | Double | Yes | 목표가 평균 |
| targetMedianPrice | Double | Yes | 목표가 중앙값 |
| recommendationMean | Double | Yes | 추천 평균 (1-5 척도) |
| recommendationKey | String | Yes | 추천 등급 |
| numberOfAnalystOpinions | Int | Yes | 애널리스트 수 |

### 3.2 Internal Response 모델

#### 구조

| 클래스 | 필드 | 타입 |
|-------|-----|------|
| QuoteResponse | quoteResponse | QuoteResponseData |
| QuoteResponseData | result | List&lt;QuoteResult&gt; |
| | error | QuoteError? |
| QuoteResult | symbol | String |
| | (모든 Yahoo 필드) | (각 필드 타입) |
| QuoteError | code | String? |
| | description | String? |

### 3.3 API 메서드 시그니처

#### YahooClient (Infrastructure)

```kotlin
suspend fun quote(symbol: String): QuoteData
suspend fun quote(symbols: List<String>): List<QuoteData>
```

| 메서드 | 파라미터 | 반환 | 설명 |
|-------|---------|-----|------|
| quote | symbol: String | QuoteData | 단일 심볼 조회 |
| quote | symbols: List&lt;String&gt; | List&lt;QuoteData&gt; | 다중 심볼 조회 |

### 3.4 필드 매핑

#### 타입 변환 규칙

| Yahoo 타입 | Domain 타입 | 변환 규칙 |
|-----------|------------|----------|
| Long (timestamp) | Instant | Instant.fromEpochSeconds() |
| Long (timestamp) | LocalDate | toLocalDateTime().date |
| Double (percent) | Double | 그대로 (소수 형태 유지) |
| String | String | 그대로 |
| null | null | 그대로 |

#### Nullable 처리

| 상황 | 처리 |
|-----|------|
| 필수 필드 없음 (symbol, price) | 예외 발생 |
| 선택 필드 없음 | null로 설정 |
| 그룹 전체 없음 | 그룹 객체 자체를 null로 설정 |

---

## 4. 예외 처리

### 4.1 ErrorCode 매핑

| 에러 케이스 | ErrorCode | 설명 |
|-----------|-----------|------|
| 잘못된 심볼 | INVALID_SYMBOL | 존재하지 않는 심볼 |
| 심볼 미존재 (다중) | - | 성공한 심볼만 반환 (로그 경고) |
| HTTP 4xx/5xx | EXTERNAL_API_ERROR | API 오류 |
| Rate Limit (429) | RATE_LIMITED | 요청 제한 |
| JSON 파싱 실패 | DATA_PARSING_ERROR | 역직렬화 실패 |
| CRUMB 획득 실패 | AUTH_FAILED | 인증 실패 |
| 네트워크 오류 | NETWORK_ERROR | 연결 실패 |

### 4.2 빈 결과 처리

| 상황 | 처리 |
|-----|------|
| result = [] (단일 심볼) | INVALID_SYMBOL 예외 |
| result 일부 누락 (다중) | 성공한 심볼만 반환 |
| 필수 필드 없음 | DATA_PARSING_ERROR 예외 |

### 4.3 재시도 정책

| 에러 | 재시도 | 횟수 | 대기 시간 |
|-----|-------|-----|----------|
| Rate Limit (429) | Yes | 무제한 | Rate Limiter 처리 |
| Network Error | Yes | 3회 | Exponential backoff |
| CRUMB 실패 | Yes | 3회 | 즉시 |
| HTTP 5xx | Yes | 3회 | Exponential backoff |
| HTTP 4xx | No | - | - |
| 파싱 오류 | No | - | - |

---

## 5. 참고 자료

### 5.1 API 제약사항

| 제약 | 설명 |
|-----|------|
| 비공식 API | 문서화되지 않음, 변경 가능 |
| 다중 심볼 상한 | 일반적으로 50-100개 권장 |
| CRUMB 필수 | 인증 토큰 필요 |
| 자산 유형별 차이 | 일부 필드는 특정 유형만 제공 |

### 5.2 캐싱 전략

| API | TTL | 이유 |
|-----|-----|------|
| quote (단일) | 60초 | 실시간 데이터 |
| quote (다중) | 60초 | 실시간 데이터 |

### 5.3 용어

| 용어 | 설명 |
|-----|------|
| Quote | 실시간 시장 데이터 API |
| CRUMB | Yahoo Finance 인증 토큰 |
| PER | Price to Earnings Ratio (주가수익비율) |
| PBR | Price to Book Ratio (주가순자산비율) |
| EPS | Earnings Per Share (주당순이익) |
| ROA | Return on Assets (자산수익률) |
| ROE | Return on Equity (자기자본수익률) |
