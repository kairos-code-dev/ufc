# Options API 기능 명세서

## 문서 정보
- **작성일**: 2025-12-05
- **버전**: 1.0.0
- **대상 프로젝트**: UFC (Unified Finance Client)
- **참조 구현**: yfinance (Python)

---

## 1. API 개요

### 1.1 목적
Options API는 Yahoo Finance의 `/v7/finance/options` 엔드포인트를 통해 주식 옵션 데이터를 조회하는 기능을 제공합니다. 이 API는 콜 옵션(Call Options)과 풋 옵션(Put Options)의 상세 정보를 제공하며, 옵션 만기일(Expiration Dates) 목록을 통해 특정 만기일의 옵션 체인을 조회할 수 있습니다.

### 1.2 주요 사용 사례
- **옵션 트레이딩**: 특정 종목의 콜/풋 옵션 가격, 행사가, 변동성 정보 조회
- **변동성 분석**: 내재 변동성(Implied Volatility)을 통한 시장 심리 분석
- **리스크 관리**: Greeks(Delta, Gamma, Theta 등) 데이터를 활용한 포트폴리오 리스크 관리
- **옵션 체인 비교**: 다양한 만기일과 행사가의 옵션 체인 비교 분석
- **프리미엄 추적**: 옵션 프리미엄 변화 추이 모니터링
- **ITM/OTM 분석**: In-The-Money, Out-Of-The-Money 옵션 분포 분석

### 1.3 데이터 제공 범위
- 미국 시장 주식 옵션 (EQUITY)
- ETF 옵션
- 일부 지수 옵션
- 주의: 모든 종목이 옵션을 제공하는 것은 아니며, 옵션이 없는 종목의 경우 빈 응답 또는 에러를 반환할 수 있습니다.

---

## 2. Yahoo Finance Options API 분석

### 2.1 엔드포인트 정보

#### 기본 엔드포인트
```
GET https://query2.finance.yahoo.com/v7/finance/options/{symbol}
```

#### 특정 만기일 조회
```
GET https://query2.finance.yahoo.com/v7/finance/options/{symbol}?date={timestamp}
```

### 2.2 요청 파라미터

| 파라미터 | 타입 | 필수 여부 | 설명 |
|---------|------|----------|------|
| `symbol` | String | 필수 | 종목 심볼 (예: "AAPL", "TSLA") - Path Parameter |
| `date` | Long | 선택 | 만기일 Unix Timestamp (초 단위) - Query Parameter |
| `crumb` | String | 필수 | Yahoo Finance 인증 토큰 - Query Parameter |

#### 파라미터 상세 설명

**symbol (Path Parameter)**
- 주식 또는 ETF의 심볼
- 대소문자 구분 없음 (일반적으로 대문자 사용)
- 예시: "AAPL", "GOOGL", "SPY"

**date (Query Parameter)**
- 선택적 파라미터
- 생략 시: 가장 가까운 만기일의 옵션 체인을 반환
- 지정 시: 해당 만기일의 옵션 체인을 반환
- 형식: Unix Timestamp (초 단위)
- 예시: `1735948800` (2025-01-03 00:00:00 UTC)
- 주의: 정확한 타임스탬프 값은 첫 번째 호출에서 반환된 `expirationDates` 배열에서 가져와야 함

**crumb (Query Parameter)**
- Yahoo Finance API 인증에 필요한 토큰
- 기존 UFC 인프라의 `YahooAuthenticator`를 통해 자동 획득
- 각 요청마다 자동으로 추가됨

### 2.3 HTTP 메서드 및 헤더

**HTTP 메서드**: `GET`

**필수 HTTP 헤더**:
```
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
Accept: application/json
Accept-Language: en-US,en;q=0.9
```

이 헤더들은 이미 `YahooClient`의 기본 설정에 포함되어 있습니다.

### 2.4 응답 구조

#### 전체 응답 구조
```
{
  "optionChain": {
    "result": [
      {
        "underlyingSymbol": String,
        "expirationDates": [Long, Long, ...],
        "strikes": [Double, Double, ...],
        "hasMiniOptions": Boolean,
        "quote": {
          // 기초 자산 가격 정보 (QuoteSummary PRICE 모듈과 유사)
        },
        "options": [
          {
            "expirationDate": Long,
            "hasMiniOptions": Boolean,
            "calls": [
              {
                "contractSymbol": String,
                "strike": Double,
                "currency": String,
                "lastPrice": Double,
                "change": Double,
                "percentChange": Double,
                "volume": Long,
                "openInterest": Long,
                "bid": Double,
                "ask": Double,
                "contractSize": String,
                "expiration": Long,
                "lastTradeDate": Long,
                "impliedVolatility": Double,
                "inTheMoney": Boolean
              },
              ...
            ],
            "puts": [
              {
                // calls와 동일한 구조
              },
              ...
            ]
          }
        ]
      }
    ],
    "error": null
  }
}
```

#### 응답 필드 상세 설명

**최상위 레벨 (optionChain)**:
- `result`: 결과 배열 (일반적으로 단일 요소)
- `error`: 에러 정보 (정상 응답 시 `null`)

**result[0] 레벨 (기본 정보)**:
- `underlyingSymbol`: 기초 자산 심볼 (예: "AAPL")
- `expirationDates`: 사용 가능한 모든 만기일 배열 (Unix Timestamp, 초 단위)
- `strikes`: 사용 가능한 모든 행사가 배열 (Double)
- `hasMiniOptions`: 미니 옵션 존재 여부 (Boolean)
- `quote`: 기초 자산의 현재 가격 정보 (Object)

**quote 객체 (기초 자산 정보)**:
QuoteSummary API의 PRICE 모듈과 유사한 구조:
- `symbol`: 심볼
- `shortName`: 짧은 이름
- `regularMarketPrice`: 현재가
- `regularMarketChange`: 가격 변동
- `regularMarketChangePercent`: 변동률
- `regularMarketVolume`: 거래량
- `regularMarketTime`: 시장 시간
- 기타 가격 관련 필드

**options[0] 레벨 (옵션 체인)**:
- `expirationDate`: 이 옵션 체인의 만기일 (Unix Timestamp, 초 단위)
- `hasMiniOptions`: 미니 옵션 여부
- `calls`: 콜 옵션 배열
- `puts`: 풋 옵션 배열

**calls / puts 배열 요소 (개별 옵션 계약)**:

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `contractSymbol` | String | No | 옵션 계약 심볼 (예: "AAPL250103C00150000") |
| `strike` | Double | No | 행사가 (Strike Price) |
| `currency` | String | No | 통화 (예: "USD") |
| `lastPrice` | Double | Yes | 최종 거래 가격 (프리미엄) |
| `change` | Double | Yes | 가격 변동 (전일 대비) |
| `percentChange` | Double | Yes | 변동률 (%) |
| `volume` | Long | Yes | 거래량 (당일) |
| `openInterest` | Long | Yes | 미결제 약정 (Open Interest) |
| `bid` | Double | Yes | 매수 호가 |
| `ask` | Double | Yes | 매도 호가 |
| `contractSize` | String | No | 계약 크기 (일반적으로 "REGULAR") |
| `expiration` | Long | No | 만기일 (Unix Timestamp, 초 단위) |
| `lastTradeDate` | Long | Yes | 최종 거래 일시 (Unix Timestamp, 초 단위) |
| `impliedVolatility` | Double | Yes | 내재 변동성 (Implied Volatility, 소수 형태) |
| `inTheMoney` | Boolean | No | In-The-Money 여부 |

#### 옵션 계약 심볼 형식
```
{SYMBOL}{YY}{MM}{DD}{C/P}{STRIKE_PRICE_PADDED}

예시:
- AAPL250103C00150000
  - AAPL: 기초 자산 심볼
  - 25: 연도 (2025)
  - 01: 월
  - 03: 일
  - C: 콜 옵션 (P는 풋 옵션)
  - 00150000: 행사가 $150.00 (8자리, 소수점 3자리 포함)
```

### 2.5 에러 응답

#### 일반 에러 응답
```json
{
  "optionChain": {
    "result": null,
    "error": {
      "code": "Not Found",
      "description": "No data found for symbol"
    }
  }
}
```

#### 옵션이 없는 종목
```json
{
  "optionChain": {
    "result": [
      {
        "underlyingSymbol": "XYZ",
        "expirationDates": [],
        "strikes": [],
        "hasMiniOptions": false,
        "quote": { ... },
        "options": []
      }
    ],
    "error": null
  }
}
```

#### 잘못된 만기일 지정
특정 만기일을 요청했으나 존재하지 않는 경우, 빈 옵션 배열을 반환하거나 에러를 반환할 수 있습니다.

---

## 3. UFC 통합 설계

### 3.1 아키텍처 통합 방안

UFC는 현재 `chart()`와 `quoteSummary()` 두 개의 직접 접근 API를 제공하고 있습니다. Options API도 동일한 패턴을 따릅니다.

#### 통합 레이어 구조
```
Ufc (최상위 클라이언트)
  └─ yahoo: YahooClient
       ├─ quoteSummary()  // 기존
       ├─ chart()         // 기존
       └─ options()       // 신규 추가
```

#### 호출 패턴
```kotlin
// 직접 접근 (간단한 사용)
ufc.options(symbol = "AAPL")
ufc.options(symbol = "AAPL", expirationDate = 1735948800L)

// 또는 YahooClient를 통한 접근
ufc.yahoo.options(symbol = "AAPL")
```

### 3.2 네임스페이스 배치

Options API는 Yahoo Finance 전용 기능이므로 `yahoo` 패키지 하위에 배치합니다.

#### 패키지 구조
```
com.ulalax.ufc.yahoo
  ├─ YahooClient.kt                        // options() 메서드 추가
  ├─ model/
  │    ├─ OptionsData.kt                   // 공개 도메인 모델 (신규)
  │    ├─ OptionContract.kt                // 개별 옵션 계약 모델 (신규)
  │    ├─ OptionsChain.kt                  // 콜/풋 옵션 체인 (신규)
  │    └─ UnderlyingQuote.kt               // 기초 자산 정보 (신규)
  └─ internal/
       ├─ response/
       │    └─ OptionsResponse.kt          // 내부 API 응답 모델 (신규)
       └─ YahooApiUrls.kt                  // OPTIONS 엔드포인트 추가
```

### 3.3 필요한 클래스 목록

#### 3.3.1 공개 API 모델 (yahoo/model/)

**OptionsData**
- 역할: Options API 조회 결과의 최상위 컨테이너
- 책임:
  - 기초 자산 정보 보유
  - 사용 가능한 만기일 목록 제공
  - 사용 가능한 행사가 목록 제공
  - 콜/풋 옵션 체인 제공
  - 미니 옵션 여부 정보 제공

**OptionsChain**
- 역할: 특정 만기일의 콜/풋 옵션 체인
- 책임:
  - 만기일 정보 보유
  - 콜 옵션 목록 제공
  - 풋 옵션 목록 제공
  - 특정 행사가의 옵션 필터링 기능

**OptionContract**
- 역할: 개별 옵션 계약 정보
- 책임:
  - 계약 심볼, 행사가, 만기일 등 기본 정보
  - 가격 정보 (lastPrice, bid, ask, change 등)
  - 거래 정보 (volume, openInterest)
  - Greeks 및 변동성 정보 (impliedVolatility)
  - ITM/OTM 상태 정보
  - 유틸리티 메서드 (예: bid-ask spread 계산, 프리미엄 계산)

**UnderlyingQuote**
- 역할: 기초 자산의 현재 가격 정보
- 책임:
  - 심볼, 이름
  - 현재가, 변동, 변동률
  - 거래량, 거래 시간
- 참고: Price 모델과 유사하나, Options API 응답의 quote 필드에 특화된 간소화 버전

#### 3.3.2 내부 응답 모델 (yahoo/internal/response/)

**OptionsResponse**
- 역할: Yahoo Finance Options API의 원시 JSON 응답 매핑
- 책임:
  - JSON 역직렬화
  - optionChain.result 구조 매핑
  - 에러 응답 처리

**OptionsResult**
- 역할: optionChain.result[0] 객체 매핑
- 책임:
  - underlyingSymbol, expirationDates, strikes 매핑
  - quote 객체 매핑
  - options 배열 매핑

**OptionsChainResponse**
- 역할: options[0] 객체 매핑
- 책임:
  - expirationDate, hasMiniOptions 매핑
  - calls, puts 배열 매핑

**OptionContractResponse**
- 역할: 개별 옵션 계약 원시 응답 매핑
- 책임:
  - 모든 옵션 필드의 원시 JSON 매핑
  - Nullable 필드 처리

### 3.4 API 메서드 시그니처

#### YahooClient 클래스에 추가할 메서드

```kotlin
/**
 * Options API를 호출하여 옵션 체인 데이터를 조회합니다.
 *
 * @param symbol 조회할 심볼 (예: "AAPL")
 * @param expirationDate 만기일 Unix Timestamp (선택, null인 경우 가장 가까운 만기일)
 * @return OptionsData
 * @throws ApiException API 호출 실패 시
 */
suspend fun options(
    symbol: String,
    expirationDate: Long? = null
): OptionsData
```

#### Ufc 클래스에 추가할 직접 접근 메서드

```kotlin
/**
 * Options API 직접 접근 메서드
 *
 * @param symbol 조회할 심볼
 * @param expirationDate 만기일 (선택)
 * @return OptionsData
 */
suspend fun options(
    symbol: String,
    expirationDate: Long? = null
): OptionsData = yahoo.options(symbol, expirationDate)
```

#### 유틸리티 메서드 (OptionsData 클래스 내부)

```kotlin
/**
 * 사용 가능한 모든 만기일 목록을 LocalDate 형식으로 변환
 */
fun getExpirationDatesAsLocalDate(): List<LocalDate>

/**
 * 특정 행사가에 가장 가까운 옵션 찾기
 */
fun findNearestStrike(targetStrike: Double): Double?

/**
 * ATM (At-The-Money) 옵션 찾기
 */
fun findAtTheMoneyOptions(): Pair<OptionContract?, OptionContract?>  // (call, put)
```

#### 유틸리티 메서드 (OptionContract 클래스 내부)

```kotlin
/**
 * Bid-Ask Spread 계산
 */
fun getBidAskSpread(): Double?

/**
 * Bid-Ask Spread 비율 (%)
 */
fun getBidAskSpreadPercent(): Double?

/**
 * 중간 가격 (Mid Price) 계산
 */
fun getMidPrice(): Double?

/**
 * 내재 가치 (Intrinsic Value) 계산
 *
 * @param underlyingPrice 기초 자산 현재가
 * @param isCall 콜 옵션 여부
 */
fun getIntrinsicValue(underlyingPrice: Double, isCall: Boolean): Double

/**
 * 시간 가치 (Time Value) 계산
 */
fun getTimeValue(underlyingPrice: Double, isCall: Boolean): Double?
```

---

## 4. 데이터 매핑

### 4.1 Yahoo 응답 → UFC 도메인 모델 매핑

#### OptionsResponse → OptionsData

| Yahoo 필드 경로 | UFC 모델 필드 | 타입 변환 | 비고 |
|----------------|--------------|----------|------|
| `optionChain.result[0].underlyingSymbol` | `underlyingSymbol` | String → String | 직접 매핑 |
| `optionChain.result[0].expirationDates` | `expirationDates` | List<Long> → List<Long> | Unix timestamp 유지 |
| `optionChain.result[0].strikes` | `strikes` | List<Double> → List<Double> | 직접 매핑 |
| `optionChain.result[0].hasMiniOptions` | `hasMiniOptions` | Boolean → Boolean | 직접 매핑 |
| `optionChain.result[0].quote` | `underlyingQuote` | Object → UnderlyingQuote | 변환 함수 사용 |
| `optionChain.result[0].options[0]` | `optionsChain` | Object → OptionsChain | 변환 함수 사용 |

#### OptionsChainResponse → OptionsChain

| Yahoo 필드 경로 | UFC 모델 필드 | 타입 변환 | 비고 |
|----------------|--------------|----------|------|
| `expirationDate` | `expirationDate` | Long → Long | Unix timestamp |
| `hasMiniOptions` | `hasMiniOptions` | Boolean → Boolean | 직접 매핑 |
| `calls` | `calls` | List<Object> → List<OptionContract> | 변환 함수 사용 |
| `puts` | `puts` | List<Object> → List<OptionContract> | 변환 함수 사용 |

#### OptionContractResponse → OptionContract

| Yahoo 필드 | UFC 모델 필드 | 타입 변환 | Nullable 처리 |
|-----------|--------------|----------|---------------|
| `contractSymbol` | `contractSymbol` | String → String | Non-null |
| `strike` | `strike` | Double → Double | Non-null |
| `currency` | `currency` | String → String | Non-null |
| `lastPrice` | `lastPrice` | Double? → Double? | Nullable 유지 |
| `change` | `change` | Double? → Double? | Nullable 유지 |
| `percentChange` | `percentChange` | Double? → Double? | Nullable 유지 |
| `volume` | `volume` | Long? → Long? | Nullable 유지, 거래 없으면 null |
| `openInterest` | `openInterest` | Long? → Long? | Nullable 유지 |
| `bid` | `bid` | Double? → Double? | Nullable 유지 |
| `ask` | `ask` | Double? → Double? | Nullable 유지 |
| `contractSize` | `contractSize` | String → String | Non-null, 기본값 "REGULAR" |
| `expiration` | `expiration` | Long → Long | Non-null, Unix timestamp |
| `lastTradeDate` | `lastTradeDate` | Long? → Long? | Nullable 유지 |
| `impliedVolatility` | `impliedVolatility` | Double? → Double? | Nullable 유지, 0~1 범위 (예: 0.25 = 25%) |
| `inTheMoney` | `inTheMoney` | Boolean → Boolean | Non-null |

#### UnderlyingQuote 변환

기초 자산 정보는 QuoteSummary API의 Price 모듈과 유사하나, 간소화된 형태입니다.

| Yahoo quote 필드 | UFC UnderlyingQuote 필드 | 타입 변환 |
|-----------------|-------------------------|----------|
| `symbol` | `symbol` | String → String |
| `shortName` | `shortName` | String? → String? |
| `regularMarketPrice` | `regularMarketPrice` | Double? → Double? |
| `regularMarketChange` | `regularMarketChange` | Double? → Double? |
| `regularMarketChangePercent` | `regularMarketChangePercent` | Double? → Double? |
| `regularMarketVolume` | `regularMarketVolume` | Long? → Long? |
| `regularMarketTime` | `regularMarketTime` | Long? → Long? |

### 4.2 타입 변환 규칙

#### Unix Timestamp 처리
- Yahoo API는 초 단위 Unix Timestamp를 사용 (예: 1735948800)
- UFC 모델에서도 Long 타입으로 유지
- 필요 시 LocalDate/LocalDateTime으로 변환하는 유틸리티 메서드 제공
- 변환 예시:
  ```kotlin
  fun Long.toLocalDate(): LocalDate =
      Instant.ofEpochSecond(this).atZone(ZoneId.of("UTC")).toLocalDate()
  ```

#### Double 값 정밀도
- 가격 및 행사가: Double 타입 유지 (소수점 2자리가 일반적)
- 내재 변동성: Double 타입 유지 (0~1 범위의 소수, 예: 0.2545 = 25.45%)
- 필요 시 BigDecimal로 변환 가능

#### Boolean 값
- `inTheMoney`, `hasMiniOptions`: 직접 매핑
- Null이 아닌 Boolean 값으로 보장

### 4.3 Nullable 처리 전략

#### Non-null 보장 필드
다음 필드들은 항상 존재해야 하며, 없을 경우 API 응답 자체가 유효하지 않은 것으로 간주:
- `underlyingSymbol`
- `expirationDates` (빈 배열 가능)
- `strikes` (빈 배열 가능)
- `contractSymbol`
- `strike`
- `currency`
- `expiration`
- `inTheMoney`

#### Nullable 허용 필드
다음 필드들은 옵션 거래가 없거나 데이터가 부족한 경우 null일 수 있음:
- `lastPrice`: 거래가 없는 경우
- `change`, `percentChange`: 전일 거래가 없는 경우
- `volume`: 당일 거래가 없는 경우 (0 또는 null)
- `openInterest`: 미결제 약정이 없는 경우
- `bid`, `ask`: 호가가 없는 경우
- `lastTradeDate`: 거래 이력이 없는 경우
- `impliedVolatility`: 계산 불가능한 경우

#### Null 처리 원칙
1. **원본 유지**: Yahoo API 응답의 Nullable 특성을 그대로 유지
2. **명시적 처리**: Nullable 필드를 사용하는 곳에서 명시적으로 null 체크
3. **기본값 제공 금지**: 임의의 기본값(예: 0.0)을 제공하지 않음 (데이터 왜곡 방지)
4. **유틸리티 메서드**: Null-safe한 계산 메서드 제공 (예: `getBidAskSpread()` → null 반환)

#### 빈 배열 vs Null
- `expirationDates`, `strikes`, `calls`, `puts`: 빈 배열로 반환 (null 아님)
- 옵션이 없는 종목의 경우 `expirationDates = []`, `options = []`로 반환
- 이는 "데이터가 없음"을 명시적으로 표현

---

## 5. 에러 처리

### 5.1 예상 에러 케이스

#### 5.1.1 HTTP 상태 코드 기반 에러

| HTTP 상태 | Yahoo 응답 | UFC ErrorCode | 재시도 가능 | 설명 |
|-----------|-----------|---------------|------------|------|
| 404 | Not Found | `DATA_NOT_FOUND` | No | 심볼이 존재하지 않거나 옵션 데이터가 없음 |
| 401 | Unauthorized | `AUTHENTICATION_FAILED` | No | CRUMB 토큰 만료 또는 유효하지 않음 |
| 429 | Too Many Requests | `RATE_LIMIT_EXCEEDED` | Yes | Rate Limit 초과 |
| 500 | Internal Server Error | `EXTERNAL_API_ERROR` | Yes | Yahoo Finance 서버 오류 |
| 503 | Service Unavailable | `SERVICE_UNAVAILABLE` | Yes | Yahoo Finance 서비스 일시 중단 |

#### 5.1.2 응답 구조 기반 에러

**에러 응답이 포함된 경우**:
```json
{
  "optionChain": {
    "result": null,
    "error": {
      "code": "Not Found",
      "description": "No data found for symbol XYZ"
    }
  }
}
```
- UFC ErrorCode: `DATA_NOT_FOUND`
- 재시도 불가능
- 메시지: "옵션 데이터를 찾을 수 없습니다: {symbol}"

**빈 결과 반환**:
```json
{
  "optionChain": {
    "result": [],
    "error": null
  }
}
```
- UFC ErrorCode: `DATA_NOT_FOUND`
- 재시도 불가능
- 메시지: "옵션 데이터가 비어 있습니다: {symbol}"

**옵션이 없는 정상 응답**:
```json
{
  "optionChain": {
    "result": [
      {
        "underlyingSymbol": "XYZ",
        "expirationDates": [],
        "strikes": [],
        "options": []
      }
    ],
    "error": null
  }
}
```
- 에러 아님 (정상 응답)
- `OptionsData` 반환, 단 `expirationDates`와 `strikes`가 빈 배열
- 사용자가 `expirationDates.isEmpty()` 확인 가능

#### 5.1.3 파라미터 검증 에러

**잘못된 심볼 형식**:
- 검증 시점: API 호출 전
- ErrorCode: `INVALID_SYMBOL`
- 조건: 심볼이 빈 문자열이거나 공백만 포함
- 메시지: "유효하지 않은 심볼입니다: {symbol}"

**잘못된 만기일 타임스탬프**:
- 검증 시점: API 호출 후 (사용 가능한 만기일 목록과 비교)
- ErrorCode: `INVALID_PARAMETER`
- 조건: 지정한 `expirationDate`가 `expirationDates` 배열에 없음
- 메시지: "유효하지 않은 만기일입니다. 사용 가능한 만기일: [{날짜 목록}]"
- 참고: 이 검증은 클라이언트 측에서 선택적으로 수행 (Yahoo API는 잘못된 날짜 시 빈 응답 반환)

#### 5.1.4 네트워크 및 타임아웃 에러

| 에러 타입 | UFC ErrorCode | 재시도 가능 | 설명 |
|----------|---------------|------------|------|
| 연결 타임아웃 | `NETWORK_TIMEOUT` | Yes | 기본 타임아웃: 10초 |
| 읽기 타임아웃 | `NETWORK_TIMEOUT` | Yes | 기본 타임아웃: 30초 |
| DNS 해석 실패 | `NETWORK_DNS_ERROR` | Yes | Yahoo Finance 도메인 해석 실패 |
| 연결 거부 | `NETWORK_CONNECTION_ERROR` | Yes | Yahoo Finance 서버 연결 거부 |

#### 5.1.5 파싱 에러

**JSON 파싱 실패**:
- ErrorCode: `JSON_PARSING_ERROR`
- 재시도 가능: No
- 원인: Yahoo API 응답 형식 변경 또는 손상된 응답
- 메시지: "옵션 데이터 파싱 중 오류가 발생했습니다"

**필수 필드 누락**:
- ErrorCode: `DATA_PARSING_ERROR`
- 재시도 가능: No
- 원인: 응답에 필수 필드가 없음 (예: underlyingSymbol, strike 등)
- 메시지: "옵션 데이터에 필수 필드가 없습니다: {필드명}"

### 5.2 에러 응답 구조

#### UfcException 사용
기존 UFC의 단일 예외 시스템을 따릅니다:

```kotlin
throw ApiException(
    errorCode = ErrorCode.DATA_NOT_FOUND,
    message = "옵션 데이터를 찾을 수 없습니다: $symbol",
    metadata = mapOf(
        "symbol" to symbol,
        "expirationDate" to expirationDate
    )
)
```

#### 에러 메타데이터 포함 정보
Options API 에러 시 다음 메타데이터를 포함:
- `symbol`: 조회한 심볼
- `expirationDate`: 요청한 만기일 (있는 경우)
- `availableExpirations`: 사용 가능한 만기일 목록 (파라미터 검증 에러 시)
- `statusCode`: HTTP 상태 코드 (네트워크 에러 시)

### 5.3 에러 처리 흐름

```
1. HTTP 요청 전 검증
   ├─ 심볼 형식 검증 → INVALID_SYMBOL
   └─ 통과

2. HTTP 요청 및 응답
   ├─ 타임아웃 → NETWORK_TIMEOUT
   ├─ 연결 실패 → NETWORK_CONNECTION_ERROR
   ├─ HTTP 4xx/5xx → 상태 코드별 ErrorCode 매핑
   └─ HTTP 200 성공

3. 응답 파싱
   ├─ JSON 파싱 실패 → JSON_PARSING_ERROR
   ├─ 필수 필드 누락 → DATA_PARSING_ERROR
   └─ 파싱 성공

4. 비즈니스 로직 검증
   ├─ error 필드 존재 → EXTERNAL_API_ERROR 또는 DATA_NOT_FOUND
   ├─ result 배열 비어있음 → DATA_NOT_FOUND
   ├─ expirationDates 비어있음 → 정상 (옵션 없음)
   └─ 정상 응답 반환
```

---

## 6. 테스트 전략

### 6.1 단위 테스트 시나리오

#### 6.1.1 응답 파싱 테스트
위치: `src/test/kotlin/com/ulalax/ufc/yahoo/internal/response/OptionsResponseTest.kt`

**시나리오**:
1. **정상 응답 파싱**: 완전한 옵션 체인 JSON을 파싱하여 모든 필드가 올바르게 매핑되는지 검증
2. **Nullable 필드 처리**: lastPrice, bid, ask 등이 null인 경우 올바르게 처리되는지 검증
3. **빈 옵션 체인**: expirationDates와 options가 빈 배열인 경우 정상 처리 확인
4. **에러 응답 파싱**: error 객체가 있는 응답을 올바르게 파싱하는지 검증
5. **필수 필드 누락**: contractSymbol, strike 등이 없는 경우 파싱 실패 검증

#### 6.1.2 도메인 모델 변환 테스트
위치: `src/test/kotlin/com/ulalax/ufc/yahoo/model/OptionsDataTest.kt`

**시나리오**:
1. **내부 응답 → 공개 모델 변환**: OptionsResponse → OptionsData 변환 검증
2. **타임스탬프 변환**: Unix timestamp → LocalDate 변환 유틸리티 검증
3. **행사가 검색**: findNearestStrike() 메서드 검증
4. **ATM 옵션 찾기**: findAtTheMoneyOptions() 로직 검증
5. **Bid-Ask Spread 계산**: getBidAskSpread() 정확성 검증

#### 6.1.3 유틸리티 메서드 테스트
위치: `src/test/kotlin/com/ulalax/ufc/yahoo/model/OptionContractTest.kt`

**시나리오**:
1. **Bid-Ask Spread**: 정상 케이스 및 null 처리
2. **Mid Price 계산**: (bid + ask) / 2 검증
3. **내재 가치 계산**: 콜/풋 옵션의 내재 가치 계산 로직 검증
4. **시간 가치 계산**: lastPrice - intrinsicValue 검증

### 6.2 통합 테스트 시나리오

#### 6.2.1 실제 API 호출 테스트
위치: `src/test/kotlin/com/ulalax/ufc/integration/yahoo/OptionsSpec.kt`

테스트 구조는 기존 `QuoteSummarySpec.kt`와 `ChartSpec.kt`를 참고하여 작성합니다.

**@Nested 그룹 구조**:

```kotlin
@DisplayName("YahooClient.options() - 옵션 체인 조회")
class OptionsSpec : IntegrationTestBase() {

    @Nested
    @DisplayName("기본 동작")
    inner class BasicBehavior {
        // 기본 만기일 조회
        // 특정 만기일 조회
        // 옵션이 없는 종목 처리
    }

    @Nested
    @DisplayName("응답 데이터 스펙")
    inner class ResponseSpec {
        // 만기일 목록 검증
        // 행사가 목록 검증
        // 콜/풋 옵션 체인 검증
        // 기초 자산 정보 검증
    }

    @Nested
    @DisplayName("옵션 계약 필드 검증")
    inner class OptionContractFieldsSpec {
        // 필수 필드 존재 확인
        // Nullable 필드 처리
        // 데이터 타입 검증
    }

    @Nested
    @DisplayName("유틸리티 메서드 활용")
    inner class UtilityMethodsUsage {
        // ATM 옵션 찾기
        // Bid-Ask Spread 계산
        // 내재 가치 계산
    }

    @Nested
    @DisplayName("다양한 종목 테스트")
    inner class MultipleSymbols {
        // AAPL - 활발한 옵션 시장
        // SPY - ETF 옵션
        // 소형주 - 비활성 옵션 시장
    }

    @Nested
    @DisplayName("에러 케이스")
    inner class ErrorCases {
        // 존재하지 않는 심볼
        // 잘못된 만기일
        // 옵션이 없는 종목
    }
}
```

**주요 테스트 케이스**:

1. **기본 만기일 조회 (AAPL)**:
   ```kotlin
   @Test
   @DisplayName("AAPL의 기본 만기일 옵션 체인을 조회할 수 있다")
   fun `can get default expiration options for AAPL`() = integrationTest {
       val result = ufc.yahoo.options("AAPL")

       assertThat(result.underlyingSymbol).isEqualTo("AAPL")
       assertThat(result.expirationDates).isNotEmpty()
       assertThat(result.strikes).isNotEmpty()
       assertThat(result.optionsChain).isNotNull()
       assertThat(result.optionsChain.calls).isNotEmpty()
       assertThat(result.optionsChain.puts).isNotEmpty()
   }
   ```

2. **특정 만기일 조회**:
   ```kotlin
   @Test
   @DisplayName("특정 만기일의 옵션 체인을 조회할 수 있다")
   fun `can get specific expiration date options`() = integrationTest {
       val firstCall = ufc.yahoo.options("AAPL")
       val expirationDate = firstCall.expirationDates.first()

       val result = ufc.yahoo.options("AAPL", expirationDate)

       assertThat(result.optionsChain.expirationDate).isEqualTo(expirationDate)
   }
   ```

3. **옵션 필드 검증**:
   ```kotlin
   @Test
   @DisplayName("옵션 계약의 모든 필수 필드가 존재한다")
   fun `option contract has all required fields`() = integrationTest {
       val result = ufc.yahoo.options("AAPL")
       val firstCall = result.optionsChain.calls.first()

       assertThat(firstCall.contractSymbol).isNotBlank()
       assertThat(firstCall.strike).isGreaterThan(0.0)
       assertThat(firstCall.currency).isEqualTo("USD")
       assertThat(firstCall.expiration).isGreaterThan(0L)
       assertThat(firstCall.inTheMoney).isIn(true, false)
   }
   ```

4. **ATM 옵션 찾기**:
   ```kotlin
   @Test
   @DisplayName("At-The-Money 옵션을 찾을 수 있다")
   fun `can find at-the-money options`() = integrationTest {
       val result = ufc.yahoo.options("AAPL")
       val (atmCall, atmPut) = result.findAtTheMoneyOptions()

       assertThat(atmCall).isNotNull()
       assertThat(atmPut).isNotNull()

       // ATM 옵션의 행사가는 현재가와 가장 가까워야 함
       val currentPrice = result.underlyingQuote.regularMarketPrice!!
       val callStrike = atmCall!!.strike
       val putStrike = atmPut!!.strike

       assertThat(callStrike).isCloseTo(currentPrice, within(50.0))
       assertThat(putStrike).isCloseTo(currentPrice, within(50.0))
   }
   ```

5. **Bid-Ask Spread 계산**:
   ```kotlin
   @Test
   @DisplayName("Bid-Ask Spread를 계산할 수 있다")
   fun `can calculate bid-ask spread`() = integrationTest {
       val result = ufc.yahoo.options("AAPL")
       val option = result.optionsChain.calls.first {
           it.bid != null && it.ask != null
       }

       val spread = option.getBidAskSpread()
       val spreadPercent = option.getBidAskSpreadPercent()

       assertThat(spread).isNotNull()
       assertThat(spreadPercent).isNotNull()
       assertThat(spread!!).isGreaterThan(0.0)
   }
   ```

6. **에러 케이스 - 잘못된 심볼**:
   ```kotlin
   @Test
   @DisplayName("존재하지 않는 심볼 조회 시 예외를 던진다")
   fun `throws exception for invalid symbol`() = integrationTest {
       assertThatThrownBy {
           ufc.yahoo.options("INVALID_SYMBOL_12345")
       }.isInstanceOf(ApiException::class.java)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_NOT_FOUND)
   }
   ```

7. **옵션이 없는 종목**:
   ```kotlin
   @Test
   @DisplayName("옵션이 없는 종목은 빈 배열을 반환한다")
   fun `returns empty arrays for symbols without options`() = integrationTest {
       // 소형주나 외국 종목 중 옵션이 없는 경우
       val result = runCatching {
           ufc.yahoo.options("SOME_SYMBOL_WITHOUT_OPTIONS")
       }

       // 정상 응답이지만 빈 배열이거나, DATA_NOT_FOUND 에러
       result.onSuccess { data ->
           assertThat(data.expirationDates).isEmpty()
           assertThat(data.optionsChain.calls).isEmpty()
           assertThat(data.optionsChain.puts).isEmpty()
       }.onFailure { exception ->
           assertThat(exception)
               .isInstanceOf(ApiException::class.java)
               .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DATA_NOT_FOUND)
       }
   }
   ```

#### 6.2.2 Rate Limiting 검증
위치: `src/test/kotlin/com/ulalax/ufc/integration/yahoo/OptionsRateLimitSpec.kt`

**시나리오**:
1. **순차 요청**: 여러 번의 순차 요청이 Rate Limiter를 통과하는지 확인
2. **병렬 요청**: 동시 다발적 요청 시 Rate Limit 적용 확인
3. **타임아웃**: Rate Limit 초과 시 적절한 대기 시간 확인

#### 6.2.3 캐싱 동작 검증
위치: `src/test/kotlin/com/ulalax/ufc/integration/yahoo/OptionsCacheSpec.kt`

**시나리오**:
1. **캐시 히트**: 동일한 요청 시 캐시된 응답 반환 확인
2. **캐시 만료**: TTL 경과 후 새로운 API 호출 확인
3. **캐시 무효화**: 다른 만기일 요청 시 별도 캐시 확인

### 6.3 테스트 데이터 전략

#### 테스트용 심볼 선정
1. **AAPL**: 활발한 옵션 시장, 모든 기능 테스트 가능
2. **SPY**: ETF 옵션, 유동성 높음
3. **TSLA**: 높은 변동성, IV 테스트에 적합
4. **소형주**: 비활성 옵션 시장, 엣지 케이스 테스트

#### Response Recording
기존 UFC 패턴을 따라 실제 API 응답을 JSON 파일로 저장:
```kotlin
if (RecordingConfig.isRecordingEnabled) {
    ResponseRecorder.record(
        result,
        RecordingConfig.Paths.Yahoo.OPTIONS,
        "aapl_default_expiration"
    )
}
```

저장 위치: `src/test/resources/recordings/yahoo/options/`

#### Mock 데이터 사용
단위 테스트에서는 실제 API를 호출하지 않고 저장된 JSON 파일을 사용하여 파싱 로직만 검증합니다.

---

## 7. 구현 순서 및 우선순위

### 7.1 Phase 1: 기본 구조 구축
1. 내부 응답 모델 정의 (`OptionsResponse`, `OptionsResult`, etc.)
2. 공개 도메인 모델 정의 (`OptionsData`, `OptionsChain`, `OptionContract`, etc.)
3. `YahooApiUrls`에 OPTIONS 엔드포인트 추가
4. `YahooClient.options()` 메서드 기본 구현 (만기일 파라미터 없는 버전)
5. 내부 응답 → 공개 모델 변환 로직 구현

### 7.2 Phase 2: 기능 확장
1. 특정 만기일 조회 기능 추가 (`expirationDate` 파라미터)
2. `Ufc` 클래스에 직접 접근 메서드 추가
3. 유틸리티 메서드 구현 (`findAtTheMoneyOptions`, `getBidAskSpread`, etc.)
4. 타임스탬프 변환 유틸리티 추가

### 7.3 Phase 3: 에러 처리 및 검증
1. 모든 에러 케이스 처리 로직 구현
2. 파라미터 검증 로직 추가
3. Nullable 필드 안전 처리 검증
4. 에러 메타데이터 포함 로직 구현

### 7.4 Phase 4: 테스트 작성
1. 단위 테스트 작성 (응답 파싱, 모델 변환, 유틸리티)
2. 통합 테스트 작성 (`OptionsSpec.kt`)
3. 에러 케이스 테스트 작성
4. Response Recording 구현

### 7.5 Phase 5: 문서화 및 최적화
1. KDoc 주석 작성
2. README 업데이트
3. 사용 예제 추가
4. 성능 최적화 (필요 시)

---

## 8. 제약사항 및 주의사항

### 8.1 Yahoo Finance API 제약
1. **비공식 API**: Yahoo Finance Options API는 공식 문서가 없으며, 예고 없이 변경될 수 있습니다.
2. **Rate Limiting**: 과도한 요청 시 일시적으로 차단될 수 있으므로, 기존 Rate Limiter 설정을 준수해야 합니다.
3. **데이터 지연**: 실시간 데이터가 아니며, 15~20분 지연될 수 있습니다 (무료 사용자 기준).
4. **옵션 제공 여부**: 모든 종목이 옵션을 제공하는 것은 아니므로, 빈 응답 처리가 필요합니다.

### 8.2 데이터 정확성
1. **Greeks 미제공**: Yahoo Finance API는 Delta, Gamma, Theta 등의 Greeks 값을 직접 제공하지 않습니다. 내재 변동성(IV)만 제공됩니다.
2. **계산 기반 값**: 내재 가치, 시간 가치 등은 클라이언트 측에서 계산하며, 시장가와 다를 수 있습니다.
3. **미결제 약정**: `openInterest` 값은 전일 기준이며, 당일 거래를 반영하지 않습니다.

### 8.3 성능 고려사항
1. **대량 데이터**: 인기 종목(예: AAPL, SPY)은 수백 개의 옵션 계약을 반환할 수 있으므로, 대용량 JSON 파싱 처리가 필요합니다.
2. **캐싱 전략**: 옵션 데이터는 실시간 변동이 크므로, 캐시 TTL을 짧게 설정 (권장: 1분)해야 합니다.
3. **메모리 사용**: 모든 만기일의 옵션을 한번에 조회하지 않고, 필요한 만기일만 개별 조회하는 것을 권장합니다.

### 8.4 구현 시 주의사항
1. **Null Safety**: Kotlin의 Nullable 타입을 적극 활용하여 런타임 NPE를 방지해야 합니다.
2. **타임존 처리**: Unix Timestamp는 UTC 기준이므로, 표시 시 사용자 타임존으로 변환이 필요할 수 있습니다.
3. **통화 단위**: 모든 가격은 `currency` 필드에 명시된 통화 단위 (주로 USD)로 표시됩니다.
4. **계약 심볼 파싱**: `contractSymbol` 필드를 파싱하여 만기일, 타입(콜/풋), 행사가를 추출할 수 있으나, 이는 선택적 기능입니다.

---

## 9. 참조 및 추가 자료

### 9.1 yfinance 구현 참조
- 파일: `/home/ulalax/project/kairos/yfinance/yfinance/ticker.py`
- 메서드: `option_chain()`, `_download_options()`, `_options2df()`
- 주요 로직:
  - 첫 호출 시 만기일 목록 캐싱
  - 만기일 문자열 → Unix Timestamp 변환
  - Pandas DataFrame 변환

### 9.2 기존 UFC 구현 참조
- `YahooClient.quoteSummary()`: 모델 변환 패턴
- `YahooClient.chart()`: 파라미터 처리 패턴
- `QuoteSummarySpec.kt`: 통합 테스트 작성 패턴
- `ChartSpec.kt`: 이벤트 타입 처리 패턴

### 9.3 Yahoo Finance 비공식 문서
- Reddit r/algotrading: Yahoo Finance API 사용 경험 공유
- GitHub yfinance Issues: API 변경 사항 추적
- Stack Overflow: 옵션 데이터 파싱 관련 질문

### 9.4 옵션 거래 기본 개념
- **ITM (In-The-Money)**: 콜 옵션의 경우 행사가 < 현재가, 풋 옵션의 경우 행사가 > 현재가
- **OTM (Out-Of-The-Money)**: ITM의 반대
- **ATM (At-The-Money)**: 행사가 ≈ 현재가
- **내재 변동성 (IV)**: 옵션 가격에 반영된 시장의 변동성 기대치
- **미결제 약정 (Open Interest)**: 청산되지 않은 옵션 계약 수
- **Bid-Ask Spread**: 매수 호가와 매도 호가의 차이 (유동성 지표)

---

## 10. 승인 및 검토

### 10.1 명세서 검토 체크리스트
- [ ] API 엔드포인트 및 파라미터 정의 완료
- [ ] 응답 구조 상세 분석 완료
- [ ] 도메인 모델 설계 완료
- [ ] 에러 처리 전략 정의 완료
- [ ] 테스트 시나리오 작성 완료
- [ ] 기존 UFC 아키텍처와의 일관성 확인
- [ ] yfinance 참조 구현 비교 완료

### 10.2 다음 단계
명세서 승인 후 다음 단계를 진행합니다:
1. 명세서 리뷰 및 피드백 반영
2. 구현 착수 (Phase 1부터)
3. 단위 테스트 및 통합 테스트 작성
4. 코드 리뷰 및 머지

---

**문서 종료**
