# Screener API 기술 명세서

> **Version**: 2.0
> **작성일**: 2025-12-05

---

## 1. 개요

### 1.1 API 목적

Yahoo Finance Screener API를 통해 사용자 정의 조건으로 주식 및 펀드를 검색하고 필터링한다.

| 항목 | 값 |
|-----|---|
| Custom Query 엔드포인트 | `POST https://query1.finance.yahoo.com/v1/finance/screener` |
| Predefined Query 엔드포인트 | `GET https://query1.finance.yahoo.com/v1/finance/screener/predefined/saved` |
| HTTP 메서드 | POST (Custom) / GET (Predefined) |
| 인증 | Crumb 토큰 필요 |

### 1.2 주요 사용 사례

| 사용 사례 | 설명 |
|----------|------|
| 조건부 종목 검색 | 시가총액, PER, 거래량 등 재무지표 기반 검색 |
| 사전 정의된 스크리너 | Yahoo Finance 제공 인기 스크리너 활용 (day_gainers, most_actives) |
| 다중 조건 조합 | AND/OR 논리 연산자를 사용한 복합 쿼리 |
| 정렬 및 페이지네이션 | 결과 정렬, 오프셋 및 크기 제어 |
| 섹터/산업별 필터링 | 특정 섹터나 산업군에 속한 종목 검색 |

### 1.3 지원 자산 유형

| 타입 | 설명 |
|-----|------|
| EQUITY | 주식 (일반 주식, 우선주) |
| MUTUALFUND | 뮤추얼펀드 |

---

## 2. 데이터 소스 분석

### 2.1 Custom Query 요청 파라미터 (POST Body)

| 필드 | 타입 | 필수 | 기본값 | 제약 | 설명 |
|-----|------|-----|-------|------|------|
| query | Object | Yes | - | - | 검색 쿼리 객체 |
| query.operator | String | Yes | - | AND/OR/EQ/GT/LT/GTE/LTE/BTWN | 연산자 |
| query.operands | Array | Yes | - | - | 피연산자 (쿼리 또는 값) |
| quoteType | String | Yes | - | EQUITY/MUTUALFUND | 자산 유형 |
| sortField | String | Yes | - | - | 정렬 필드 |
| sortType | String | Yes | - | ASC/DESC | 정렬 방향 |
| size | Int | Yes | 100 | 1-250 | 결과 개수 |
| offset | Int | No | 0 | - | 시작 위치 |
| userId | String | No | "" | - | 사용자 ID (빈 문자열) |
| userIdType | String | No | "guid" | - | 사용자 ID 타입 |

### 2.2 Predefined Query 요청 파라미터 (Query String)

| 파라미터 | 타입 | 필수 | 기본값 | 제약 | 설명 |
|---------|------|-----|-------|------|------|
| scrIds | String | Yes | - | - | Predefined Screener ID |
| count | Int | No | 25 | 1-250 | 결과 개수 |
| offset | Int | No | 0 | - | 무시됨 (Yahoo 제한) |
| sortField | String | No | - | - | 정렬 필드 (커스텀) |
| sortType | String | No | - | ASC/DESC | 정렬 방향 (커스텀) |
| corsDomain | String | No | finance.yahoo.com | - | CORS 도메인 |
| formatted | Boolean | No | false | - | 포맷 여부 |
| lang | String | No | en-US | - | 언어 |
| region | String | No | US | - | 지역 |

### 2.3 Query 연산자

| 연산자 | Operands 구조 | 설명 | 예시 |
|--------|--------------|------|------|
| AND | `[Query, Query, ...]` | 논리 AND (모든 조건 만족) | - |
| OR | `[Query, Query, ...]` | 논리 OR (하나 이상 만족) | - |
| EQ | `[field, value]` | 같음 | `["sector", "Technology"]` |
| GT | `[field, number]` | 초과 | `["intradaymarketcap", 1000000000]` |
| LT | `[field, number]` | 미만 | `["peratio.lasttwelvemonths", 20]` |
| GTE | `[field, number]` | 이상 | `["intradayprice", 5]` |
| LTE | `[field, number]` | 이하 | `["beta", 1.5]` |
| BTWN | `[field, min, max]` | 범위 | `["peratio.lasttwelvemonths", 0, 20]` |

### 2.4 응답 구조

| 경로 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| finance.result | Array | Yes | 결과 배열 (에러 시 null) |
| finance.result[0].id | String | Yes | 결과 ID |
| finance.result[0].title | String | Yes | 제목 |
| finance.result[0].description | String | Yes | 설명 |
| finance.result[0].count | Int | No | 현재 결과 수 |
| finance.result[0].total | Int | No | 전체 매칭 수 |
| finance.result[0].start | Int | No | 시작 오프셋 |
| finance.result[0].quotes | Array | No | 종목 배열 |
| finance.error | Object | Yes | 에러 객체 (성공 시 null) |
| finance.error.code | String | No | 에러 코드 |
| finance.error.description | String | No | 에러 설명 |

### 2.5 Quote 응답 필드

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| symbol | String | No | 티커 심볼 |
| shortname | String | Yes | 짧은 이름 |
| longname | String | Yes | 긴 이름 |
| quoteType | String | Yes | EQUITY/MUTUALFUND |
| sector | String | Yes | 섹터 |
| industry | String | Yes | 산업 |
| exchange | String | Yes | 거래소 |
| marketCap | Long | Yes | 시가총액 |
| regularMarketPrice | Double | Yes | 정규시장 가격 |
| regularMarketChange | Double | Yes | 가격 변동 |
| regularMarketChangePercent | Double | Yes | 등락률 |
| regularMarketVolume | Long | Yes | 거래량 |

### 2.6 주요 Equity 필터 필드 (카테고리별)

#### 공통

| 필드 | 설명 |
|-----|------|
| region | 지역 (us, kr, jp) |
| sector | 섹터 (Technology, Healthcare, Financial Services) |
| industry | 산업 |
| exchange | 거래소 (NMS, NYQ, ASE) |

#### 가격

| 필드 | 설명 |
|-----|------|
| intradaymarketcap | 장중 시가총액 |
| intradayprice | 장중 가격 |
| percentchange | 등락률 |
| eodprice | 종가 |

#### 밸류에이션

| 필드 | 설명 |
|-----|------|
| peratio.lasttwelvemonths | PER (주가수익비율) |
| pegratio_5y | 5년 PEG 비율 |
| pricebookratio.quarterly | 분기 PBR |

#### 수익성

| 필드 | 설명 |
|-----|------|
| returnonassets.lasttwelvemonths | ROA (자산수익률) |
| returnonequity.lasttwelvemonths | ROE (자기자본수익률) |
| forward_dividend_yield | 예상 배당수익률 |

#### 거래

| 필드 | 설명 |
|-----|------|
| beta | 베타 |
| avgdailyvol3m | 3개월 평균 일일 거래량 |
| dayvolume | 당일 거래량 |

#### 손익계산서

| 필드 | 설명 |
|-----|------|
| totalrevenues.lasttwelvemonths | 총매출 |
| netincomeis.lasttwelvemonths | 순이익 |
| ebitda.lasttwelvemonths | EBITDA |

### 2.7 주요 Predefined Screener

#### Equity Screeners

| ID | 설명 | 정렬 필드 | 주요 조건 |
|----|------|----------|---------|
| aggressive_small_caps | 공격적 소형주 | eodvolume DESC | EPS 성장률 < 15% |
| day_gainers | 당일 상승 종목 | percentchange DESC | 등락률 > 3%, 시총 > 20억 |
| day_losers | 당일 하락 종목 | percentchange ASC | 등락률 < -2.5%, 시총 > 20억 |
| most_actives | 가장 활발한 종목 | dayvolume DESC | 거래량 > 500만 |
| growth_technology_stocks | 성장 테크 주식 | eodvolume DESC | 분기 매출 성장 > 25%, 섹터 = Technology |
| undervalued_growth_stocks | 저평가 성장주 | eodvolume DESC | PER 0-20, PEG < 1 |

#### Fund Screeners

| ID | 설명 | 정렬 필드 | 주요 조건 |
|----|------|----------|---------|
| high_yield_bond | 하이일드 채권 펀드 | fundnetassets DESC | 성과 등급 4-5 |
| solid_large_growth_funds | 견실한 대형 성장 펀드 | fundnetassets DESC | Large Growth, 성과 등급 4-5 |
| top_mutual_funds | 상위 뮤추얼펀드 | percentchange DESC | 가격 > $15, 성과 등급 4-5 |

---

## 3. 데이터/모델 설계

### 3.1 Domain 모델

#### ScreenerResult

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| id | String | Yes | 결과 ID |
| title | String | Yes | 제목 |
| description | String | Yes | 설명 |
| count | Int | No | 현재 결과 수 |
| total | Int | No | 전체 매칭 종목 수 |
| start | Int | No | 시작 오프셋 |
| quotes | List&lt;ScreenerQuote&gt; | No | 종목 리스트 |

#### ScreenerQuote

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| symbol | String | No | 티커 심볼 |
| shortName | String | Yes | 짧은 이름 |
| longName | String | Yes | 긴 이름 |
| quoteType | String | Yes | EQUITY/MUTUALFUND |
| sector | String | Yes | 섹터 |
| industry | String | Yes | 산업 |
| exchange | String | Yes | 거래소 |
| marketCap | Long | Yes | 시가총액 |
| regularMarketPrice | Double | Yes | 정규시장 가격 |
| regularMarketChange | Double | Yes | 가격 변동 |
| regularMarketChangePercent | Double | Yes | 등락률 |
| regularMarketVolume | Long | Yes | 거래량 |
| additionalFields | Map&lt;String, Any?&gt; | No | 추가 필드 (정렬 필드 등) |

#### ScreenerQuery (추상 클래스)

| 필드 | 타입 | 설명 |
|-----|------|------|
| operator | ScreenerOperator | 연산자 |
| operands | List&lt;Any&gt; | 피연산자 (쿼리 또는 값) |

- **메서드**: `toRequestBody(): Map<String, Any>` - Yahoo API 요청 형식으로 변환
- **메서드**: `validate()` - 쿼리 유효성 검사
- **하위 클래스**: `EquityQuery`, `FundQuery`

#### ScreenerOperator (Enum)

```kotlin
enum class ScreenerOperator {
    AND,    // 논리 AND
    OR,     // 논리 OR
    EQ,     // 같음
    GT,     // 초과
    LT,     // 미만
    GTE,    // 이상
    LTE,    // 이하
    BTWN,   // 범위
    IS_IN   // 포함 (클라이언트에서 OR/EQ로 변환)
}
```

#### ScreenerField (Sealed Interface)

| 속성 | 타입 | 설명 |
|-----|------|------|
| apiValue | String | API 필드명 |
| category | String | 필드 카테고리 (price, trading, valuation) |
| valueType | FieldValueType | 값 타입 (NUMBER, STRING, ENUM) |

- **하위 클래스**: `EquityField` (Enum), `FundField` (Enum)

#### FieldValueType (Enum)

```kotlin
enum class FieldValueType {
    NUMBER,  // 숫자 타입 (Long, Double 등)
    STRING,  // 문자열 타입
    ENUM     // 열거형 타입 (특정 값만 허용)
}
```

#### ScreenerSortField (Enum)

```kotlin
enum class ScreenerSortField(val apiValue: String) {
    TICKER("ticker"),
    PERCENT_CHANGE("percentchange"),
    DAY_VOLUME("dayvolume"),
    MARKET_CAP("intradaymarketcap"),
    PE_RATIO("peratio.lasttwelvemonths"),
    // ...
}
```

#### PredefinedScreener (Enum)

```kotlin
enum class PredefinedScreener(
    val apiId: String,
    val defaultSortField: ScreenerSortField,
    val defaultSortAsc: Boolean
) {
    AGGRESSIVE_SMALL_CAPS("aggressive_small_caps", ScreenerSortField.EOD_VOLUME, false),
    DAY_GAINERS("day_gainers", ScreenerSortField.PERCENT_CHANGE, false),
    MOST_ACTIVES("most_actives", ScreenerSortField.DAY_VOLUME, false),
    // ...
}
```

### 3.2 Internal Request 모델

#### ScreenerRequest (Custom Query용)

| 필드 | 타입 |
|-----|------|
| query | Map&lt;String, Any&gt; |
| quoteType | String |
| sortField | String |
| sortType | String |
| size | Int |
| offset | Int |
| userId | String |
| userIdType | String |

### 3.3 Internal Response 모델

#### 구조

| 클래스 | 필드 | 타입 |
|-------|-----|------|
| ScreenerResponse | finance | FinanceContainer |
| FinanceContainer | result | List&lt;ScreenerApiResult&gt;? |
| | error | ScreenerError? |
| ScreenerApiResult | id | String? |
| | title | String? |
| | description | String? |
| | count | Int |
| | total | Int |
| | start | Int |
| | quotes | List&lt;Map&lt;String, Any&gt;&gt; |
| ScreenerError | code | String |
| | description | String |

### 3.4 API 메서드 시그니처

#### Custom Query 실행

```kotlin
suspend fun screener(
    query: ScreenerQuery,
    sortField: ScreenerSortField = ScreenerSortField.TICKER,
    sortAsc: Boolean = false,
    size: Int = 100,
    offset: Int = 0
): ScreenerResult
```

| 파라미터 | 타입 | 기본값 | 제약 | 설명 |
|---------|------|-------|------|------|
| query | ScreenerQuery | - | - | 검색 쿼리 객체 |
| sortField | ScreenerSortField | TICKER | - | 정렬 필드 |
| sortAsc | Boolean | false | - | 오름차순 여부 |
| size | Int | 100 | 1-250 | 결과 개수 |
| offset | Int | 0 | >= 0 | 시작 위치 |

#### Predefined Query 실행 (String ID)

```kotlin
suspend fun screener(
    predefinedId: String,
    count: Int = 25,
    sortField: ScreenerSortField? = null,
    sortAsc: Boolean? = null
): ScreenerResult
```

| 파라미터 | 타입 | 기본값 | 제약 | 설명 |
|---------|------|-------|------|------|
| predefinedId | String | - | - | Predefined Screener ID |
| count | Int | 25 | 1-250 | 결과 개수 |
| sortField | ScreenerSortField? | null | - | 커스텀 정렬 (null이면 기본값) |
| sortAsc | Boolean? | null | - | 커스텀 정렬 방향 |

#### Predefined Query 실행 (Enum)

```kotlin
suspend fun screener(
    predefined: PredefinedScreener,
    count: Int = 25,
    sortField: ScreenerSortField? = null,
    sortAsc: Boolean? = null
): ScreenerResult
```

| 파라미터 | 타입 | 기본값 | 제약 | 설명 |
|---------|------|-------|------|------|
| predefined | PredefinedScreener | - | - | Predefined Screener Enum |
| count | Int | 25 | 1-250 | 결과 개수 |
| sortField | ScreenerSortField? | null | - | 커스텀 정렬 |
| sortAsc | Boolean? | null | - | 커스텀 정렬 방향 |

### 3.5 필드 매핑

#### Yahoo 응답 → UFC Domain

| Yahoo 필드 | UFC 필드 | 타입 변환 | 비고 |
|-----------|---------|----------|------|
| symbol | symbol | String | 필수 |
| shortname | shortName | String? | - |
| longname | longName | String? | - |
| quoteType | quoteType | String? | - |
| sector | sector | String? | - |
| industry | industry | String? | - |
| exchange | exchange | String? | - |
| marketCap | marketCap | Number → Long? | - |
| regularMarketPrice | regularMarketPrice | Number → Double? | - |
| regularMarketChange | regularMarketChange | Number → Double? | - |
| regularMarketChangePercent | regularMarketChangePercent | Number → Double? | - |
| regularMarketVolume | regularMarketVolume | Number → Long? | - |
| (기타) | additionalFields | Map&lt;String, Any?&gt; | 동적 필드 |

#### UFC Query → Yahoo Request

| 변환 | 설명 |
|-----|------|
| EquityQuery → quoteType: "EQUITY" | 자동 설정 |
| FundQuery → quoteType: "MUTUALFUND" | 자동 설정 |
| IS_IN 연산자 → OR + EQ | `IS_IN("exchange", "NMS", "NYQ")` → `OR(EQ("exchange", "NMS"), EQ("exchange", "NYQ"))` |
| ScreenerQuery.validate() | 필드 유효성, 연산자 유효성, 값 타입 검사 |

### 3.6 Query 유효성 검사

| 검사 항목 | 조건 | 예외 |
|----------|-----|------|
| AND/OR operands | operands.size >= 2 | IllegalArgumentException |
| AND/OR operands 타입 | 모두 ScreenerQuery | IllegalArgumentException |
| EQ/GT/LT/GTE/LTE operands | operands.size == 2 | IllegalArgumentException |
| GT/LT/GTE/LTE 값 타입 | operands[1] is Number | IllegalArgumentException |
| BTWN operands | operands.size == 3 | IllegalArgumentException |
| BTWN 값 타입 | operands[1], operands[2] is Number | IllegalArgumentException |
| 필드 유효성 | EquityField/FundField에 존재 | IllegalArgumentException |

---

## 4. 예외 처리

### 4.1 ErrorCode 매핑

| 에러 케이스 | ErrorCode | HTTP Status | 설명 |
|-----------|-----------|-------------|------|
| 쿼리 유효성 검사 실패 | INVALID_PARAMETER | - | 잘못된 필드, 연산자, 값 |
| size/count > 250 | INVALID_PARAMETER | - | Yahoo 제한 초과 |
| size/count < 1 | INVALID_PARAMETER | - | 최소값 미만 |
| offset < 0 | INVALID_PARAMETER | - | 음수 불가 |
| 지원하지 않는 필드 | INVALID_PARAMETER | - | EquityField/FundField에 없음 |
| Crumb 토큰 만료/무효 | AUTHENTICATION_FAILED | 401 | 인증 실패 |
| Rate Limit 초과 | RATE_LIMIT_EXCEEDED | 429 | Yahoo Rate Limit |
| 잘못된 쿼리 | EXTERNAL_API_ERROR | 400 | Yahoo가 쿼리 거부 |
| HTTP 5xx | EXTERNAL_API_ERROR | 5xx | Yahoo 서버 오류 |
| 네트워크 오류 | NETWORK_ERROR | - | 타임아웃, 연결 실패 |
| JSON 파싱 실패 | DATA_PARSING_ERROR | 200 | 응답 구조 불일치 |
| symbol 필드 누락 | DATA_PARSING_ERROR | 200 | 필수 필드 없음 |

### 4.2 빈 결과 처리

| 상황 | 처리 |
|-----|------|
| quotes = [] | 빈 ScreenerResult 반환 (예외 아님) |
| 매칭 종목 0개 | count = 0, total = 0, quotes = [] |
| 존재하지 않는 Predefined ID | ApiException (EXTERNAL_API_ERROR) |

### 4.3 재시도 정책

| 에러 | 재시도 | 횟수 | 비고 |
|-----|-------|-----|------|
| Rate Limit (429) | Yes | Rate Limiter 자동 처리 | - |
| Crumb 토큰 만료 (401) | Yes | 1회 | 토큰 재발급 후 재시도 |
| Network Error | No | - | 클라이언트가 결정 |
| HTTP 5xx | No | - | - |
| HTTP 4xx (401 제외) | No | - | - |

### 4.4 Nullable 처리

| 상황 | 처리 |
|-----|------|
| symbol 필드 누락 | DATA_PARSING_ERROR 발생 |
| 재무 지표 누락 | null 저장 |
| count/total/start 누락 | 기본값 0 |
| quotes 누락 | 빈 리스트 |
| additionalFields 누락 | 빈 맵 |

---

## 5. 참고 자료

### 5.1 API 제약사항

| 제약 | 설명 |
|-----|------|
| 비공식 API | 문서화되지 않음, 변경 가능 |
| 최대 결과 250개 | size/count 상한 |
| Predefined offset 무시 | Predefined Query는 offset 파라미터 무시 |
| 실시간 데이터 아님 | 15-20분 지연 가능 |
| 필드 가용성 | 모든 종목이 모든 필드를 제공하지 않음 |

### 5.2 통합 위치

| 컴포넌트 | 위치 |
|---------|------|
| YahooClient | `com.ulalax.ufc.infrastructure.yahoo.YahooClient` |
| Domain 모델 | `com.ulalax.ufc.domain.model.screener` |
| Internal 모델 | `com.ulalax.ufc.yahoo.internal.response` |
| Ufc 파사드 | `com.ulalax.ufc.Ufc` |

### 5.3 용어

| 용어 | 설명 |
|-----|------|
| Screener | 특정 조건에 맞는 종목을 검색하는 도구 |
| Query | 검색 조건을 표현하는 논리적 구조 |
| Operand | 연산자의 피연산자 (필드, 값, 하위 쿼리) |
| Predefined Screener | Yahoo가 미리 정의한 인기 검색 조건 |
| Custom Query | 사용자가 직접 정의한 검색 조건 |
| Crumb | Yahoo Finance API 인증 토큰 |
| Quote | 종목 (주식, 펀드) |
