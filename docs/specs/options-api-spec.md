# Options API 기술 명세서

> **Version**: 1.0
> **작성일**: 2025-12-05

---

## 1. 개요

### 1.1 API 목적

Yahoo Finance Options API를 통해 특정 심볼의 **옵션 체인(Options Chain)** 데이터를 조회한다.

| 항목 | 값 |
|-----|---|
| 엔드포인트 | `GET https://query2.finance.yahoo.com/v7/finance/options/{symbol}` |
| HTTP 메서드 | GET |
| 인증 | Crumb 토큰 필요 |

### 1.2 제공 데이터

| 데이터 | 설명 |
|-------|------|
| 만기일 목록 | 사용 가능한 모든 옵션 만기일 |
| 행사가 목록 | 사용 가능한 모든 행사가 |
| 콜 옵션 | Call Options 체인 |
| 풋 옵션 | Put Options 체인 |
| 기초 자산 정보 | 현재가, 변동률, 거래량 등 |
| 옵션 계약 상세 | 가격, 거래량, 미결제약정, 내재변동성 등 |

---

## 2. 데이터 소스 분석

### 2.1 요청 파라미터

| 파라미터 | 타입 | 위치 | 필수 | 설명 |
|---------|------|-----|------|------|
| symbol | String | Path | Yes | 티커 심볼 (예: "AAPL") |
| date | Long | Query | No | 만기일 Unix Timestamp (초 단위) |
| crumb | String | Query | Yes | 인증 토큰 (자동 추가) |

### 2.2 응답 구조

| 레벨 | 필드 | 타입 | Nullable | 설명 |
|-----|------|------|----------|------|
| Root | optionChain | Object | No | 최상위 컨테이너 |
| optionChain | result | Array | Yes | 결과 배열 (단일 요소) |
| optionChain | error | Object | Yes | 에러 정보 |
| result[0] | underlyingSymbol | String | No | 기초 자산 심볼 |
| result[0] | expirationDates | List&lt;Long&gt; | No | 만기일 목록 (빈 배열 가능) |
| result[0] | strikes | List&lt;Double&gt; | No | 행사가 목록 (빈 배열 가능) |
| result[0] | hasMiniOptions | Boolean | No | 미니 옵션 존재 여부 |
| result[0] | quote | Object | Yes | 기초 자산 가격 정보 |
| result[0] | options | Array | No | 옵션 체인 배열 |

### 2.3 옵션 계약 필드

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| contractSymbol | String | No | 계약 심볼 (예: "AAPL250103C00150000") |
| strike | Double | No | 행사가 |
| currency | String | No | 통화 (예: "USD") |
| lastPrice | Double | Yes | 최종 거래 가격 |
| change | Double | Yes | 가격 변동 |
| percentChange | Double | Yes | 변동률 (%) |
| volume | Long | Yes | 거래량 |
| openInterest | Long | Yes | 미결제 약정 |
| bid | Double | Yes | 매수 호가 |
| ask | Double | Yes | 매도 호가 |
| contractSize | String | No | 계약 크기 (기본: "REGULAR") |
| expiration | Long | No | 만기일 (Unix Timestamp) |
| lastTradeDate | Long | Yes | 최종 거래 일시 |
| impliedVolatility | Double | Yes | 내재 변동성 (0~1 범위) |
| inTheMoney | Boolean | No | ITM 여부 |

### 2.4 기초 자산 quote 필드

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| symbol | String | No | 심볼 |
| shortName | String | Yes | 짧은 이름 |
| regularMarketPrice | Double | Yes | 현재가 |
| regularMarketChange | Double | Yes | 가격 변동 |
| regularMarketChangePercent | Double | Yes | 변동률 |
| regularMarketVolume | Long | Yes | 거래량 |
| regularMarketTime | Long | Yes | 시장 시간 |

---

## 3. 데이터/모델 설계

### 3.1 Domain 모델

#### OptionsData

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| underlyingSymbol | String | No | 기초 자산 심볼 |
| expirationDates | List&lt;Long&gt; | No | 사용 가능한 만기일 목록 |
| strikes | List&lt;Double&gt; | No | 사용 가능한 행사가 목록 |
| hasMiniOptions | Boolean | No | 미니 옵션 존재 여부 |
| underlyingQuote | UnderlyingQuote | Yes | 기초 자산 정보 |
| optionsChain | OptionsChain | No | 옵션 체인 |

#### OptionsChain

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| expirationDate | Long | No | 만기일 |
| hasMiniOptions | Boolean | No | 미니 옵션 여부 |
| calls | List&lt;OptionContract&gt; | No | 콜 옵션 목록 |
| puts | List&lt;OptionContract&gt; | No | 풋 옵션 목록 |

#### OptionContract

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| contractSymbol | String | No | 계약 심볼 |
| strike | Double | No | 행사가 |
| currency | String | No | 통화 |
| lastPrice | Double | Yes | 최종 거래 가격 |
| change | Double | Yes | 가격 변동 |
| percentChange | Double | Yes | 변동률 |
| volume | Long | Yes | 거래량 |
| openInterest | Long | Yes | 미결제 약정 |
| bid | Double | Yes | 매수 호가 |
| ask | Double | Yes | 매도 호가 |
| contractSize | String | No | 계약 크기 |
| expiration | Long | No | 만기일 |
| lastTradeDate | Long | Yes | 최종 거래 일시 |
| impliedVolatility | Double | Yes | 내재 변동성 |
| inTheMoney | Boolean | No | ITM 여부 |

#### UnderlyingQuote

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| symbol | String | No | 심볼 |
| shortName | String | Yes | 짧은 이름 |
| regularMarketPrice | Double | Yes | 현재가 |
| regularMarketChange | Double | Yes | 가격 변동 |
| regularMarketChangePercent | Double | Yes | 변동률 |
| regularMarketVolume | Long | Yes | 거래량 |
| regularMarketTime | Long | Yes | 시장 시간 |

### 3.2 Internal Response 모델

#### 구조

| 클래스 | 필드 | 타입 |
|-------|-----|------|
| OptionsResponse | optionChain | OptionChainWrapper |
| OptionChainWrapper | result | List&lt;OptionsResult&gt;? |
| | error | OptionsError? |
| OptionsResult | underlyingSymbol | String |
| | expirationDates | List&lt;Long&gt; |
| | strikes | List&lt;Double&gt; |
| | hasMiniOptions | Boolean |
| | quote | UnderlyingQuoteResponse? |
| | options | List&lt;OptionsChainResponse&gt; |
| OptionsChainResponse | expirationDate | Long |
| | hasMiniOptions | Boolean |
| | calls | List&lt;OptionContractResponse&gt; |
| | puts | List&lt;OptionContractResponse&gt; |
| OptionContractResponse | (계약 필드) | (해당 타입) |
| OptionsError | code | String? |
| | description | String? |

### 3.3 API 메서드 시그니처

```kotlin
suspend fun options(symbol: String, expirationDate: Long? = null): OptionsData
```

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|-------|------|
| symbol | String | - | 티커 심볼 (필수) |
| expirationDate | Long? | null | 만기일 Unix Timestamp (선택) |

| 반환 | 설명 |
|-----|------|
| OptionsData | 옵션 체인 데이터 |

### 3.4 유틸리티 메서드

#### OptionsData

```kotlin
fun getExpirationDatesAsLocalDate(): List<LocalDate>
fun findNearestStrike(targetStrike: Double): Double?
fun findAtTheMoneyOptions(): Pair<OptionContract?, OptionContract?>
```

#### OptionContract

```kotlin
fun getBidAskSpread(): Double?
fun getBidAskSpreadPercent(): Double?
fun getMidPrice(): Double?
fun getIntrinsicValue(underlyingPrice: Double, isCall: Boolean): Double
fun getTimeValue(underlyingPrice: Double, isCall: Boolean): Double?
```

### 3.5 필드 매핑

| Yahoo 필드 | Domain 필드 | 변환 |
|-----------|------------|------|
| optionChain.result[0].underlyingSymbol | underlyingSymbol | 그대로 |
| expirationDates | expirationDates | 그대로 |
| strikes | strikes | 그대로 |
| hasMiniOptions | hasMiniOptions | 그대로 |
| quote | underlyingQuote | Object → UnderlyingQuote |
| options[0] | optionsChain | Object → OptionsChain |
| calls / puts | calls / puts | Array → List&lt;OptionContract&gt; |

---

## 4. 예외 처리

### 4.1 ErrorCode 매핑

| 에러 케이스 | ErrorCode | 설명 |
|-----------|-----------|------|
| 빈 심볼 | INVALID_PARAMETER | 심볼 검증 실패 |
| 404 Not Found | DATA_NOT_FOUND | 심볼 또는 옵션 데이터 없음 |
| 401 Unauthorized | AUTHENTICATION_FAILED | Crumb 토큰 만료 |
| 429 Rate Limit | RATE_LIMITED | 요청 제한 초과 |
| 500/503 | EXTERNAL_API_ERROR | Yahoo 서버 오류 |
| JSON 파싱 실패 | DATA_PARSING_ERROR | 역직렬화 실패 |
| 필수 필드 누락 | DATA_PARSING_ERROR | 필드 검증 실패 |

### 4.2 빈 결과 처리

| 상황 | 처리 |
|-----|------|
| result = [] | DATA_NOT_FOUND 예외 |
| expirationDates = [] | 정상 반환 (옵션 없음) |
| error 객체 존재 | EXTERNAL_API_ERROR 또는 DATA_NOT_FOUND |
| 잘못된 만기일 | 빈 calls/puts 배열 반환 |

### 4.3 재시도 정책

| 에러 | 재시도 | 횟수 |
|-----|-------|-----|
| Rate Limit (429) | Yes | Rate Limiter 처리 |
| Network Error | Yes | 3회 |
| HTTP 5xx | Yes | 3회 |
| HTTP 4xx | No | - |
| Parsing Error | No | - |

### 4.4 에러 메타데이터

| 키 | 타입 | 설명 |
|----|------|------|
| symbol | String | 조회한 심볼 |
| expirationDate | Long? | 요청한 만기일 |
| availableExpirations | List&lt;Long&gt;? | 사용 가능한 만기일 목록 |
| statusCode | Int? | HTTP 상태 코드 |

---

## 5. 참고 자료

### 5.1 API 제약사항

| 제약 | 설명 |
|-----|------|
| 비공식 API | 문서화되지 않음, 변경 가능 |
| Crumb 필요 | YahooAuthenticator로 자동 획득 |
| 데이터 지연 | 15-20분 지연 (무료 사용자) |
| 옵션 제공 | 모든 종목이 옵션을 제공하지 않음 |

### 5.2 계약 심볼 형식

```
{SYMBOL}{YY}{MM}{DD}{C/P}{STRIKE_PRICE_PADDED}

예시: AAPL250103C00150000
- AAPL: 기초 자산 심볼
- 25: 연도 (2025)
- 01: 월
- 03: 일
- C: 콜 옵션 (P는 풋)
- 00150000: 행사가 $150.00 (8자리)
```

### 5.3 용어

| 용어 | 설명 |
|-----|------|
| ITM | In-The-Money (내가격) |
| OTM | Out-Of-The-Money (외가격) |
| ATM | At-The-Money (등가격) |
| IV | Implied Volatility (내재 변동성) |
| Open Interest | 미결제 약정 수 |
| Bid-Ask Spread | 매수/매도 호가 차이 |
| Greeks | Delta, Gamma, Theta 등 (API 미제공) |

### 5.4 패키지 구조

```
com.ulalax.ufc.yahoo
  ├─ YahooClient.kt              # options() 메서드
  ├─ model/
  │    ├─ OptionsData.kt
  │    ├─ OptionsChain.kt
  │    ├─ OptionContract.kt
  │    └─ UnderlyingQuote.kt
  └─ internal/
       ├─ response/
       │    └─ OptionsResponse.kt
       └─ YahooApiUrls.kt
```
