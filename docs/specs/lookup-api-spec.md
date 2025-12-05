# Lookup API 기능 명세서

**작성일**: 2025-12-05
**버전**: 1.0.0
**대상 프로젝트**: UFC (Unified Finance Client)

---

## 1. API 개요

### 1.1 목적

Lookup API는 Yahoo Finance의 `/v1/finance/lookup` 엔드포인트를 통해 금융상품 검색 기능을 제공합니다. 이 API는 사용자가 입력한 검색어(쿼리)를 기반으로 관련 금융상품(주식, ETF, 뮤추얼펀드, 인덱스, 선물, 통화, 암호화폐)을 조회하고 필터링할 수 있는 기능을 제공합니다.

### 1.2 Search API와의 차이점

| 구분 | Lookup API | Search API |
|------|-----------|-----------|
| **엔드포인트** | `/v1/finance/lookup` | `/v1/finance/search` |
| **용도** | 금융상품 타입별 정밀 검색 | 범용 키워드 검색 |
| **필터링** | 타입별 필터링 지원 (equity, etf, mutualfund 등) | 제한적 필터링 |
| **결과 구조** | 단일 타입에 집중된 결과 | 다양한 타입 혼합 결과 |
| **정렬** | 관련도 기반 정렬 | 관련도 + 인기도 기반 정렬 |
| **응답 속도** | 빠름 (단일 타입 조회 시) | 보통 (전체 검색) |

### 1.3 주요 사용 사례

1. **타입별 금융상품 검색**: 사용자가 "Apple"을 검색할 때 주식만, ETF만, 또는 모든 타입을 선택적으로 조회
2. **자동완성 기능**: 사용자 입력에 따라 실시간으로 금융상품 제안
3. **종목 발견**: 특정 섹터나 산업군의 금융상품 탐색
4. **심볼 검증**: 입력된 심볼이 유효한지 확인하고 정식 심볼 획득
5. **크로스 마켓 검색**: 다양한 국가/거래소의 금융상품 동시 검색

---

## 2. Yahoo Finance Lookup API 분석

### 2.1 API 엔드포인트

```
GET https://query1.finance.yahoo.com/v1/finance/lookup
```

### 2.2 요청 파라미터

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| `query` | String | 필수 | - | 검색 키워드 (예: "Apple", "AAPL", "SPY") |
| `type` | String | 선택 | "all" | 필터링할 금융상품 타입 |
| `count` | Int | 선택 | 25 | 반환할 최대 결과 개수 (1-100) |
| `start` | Int | 선택 | 0 | 결과 페이징 시작 인덱스 |
| `formatted` | Boolean | 선택 | false | 포맷된 값 포함 여부 |
| `fetchPricingData` | Boolean | 선택 | true | 가격 데이터 포함 여부 |
| `lang` | String | 선택 | "en-US" | 언어 설정 |
| `region` | String | 선택 | "US" | 지역 설정 |

### 2.3 지원하는 타입 값

| 타입 값 | 설명 | 예시 |
|--------|------|------|
| `all` | 모든 금융상품 타입 | 주식, ETF, 펀드 모두 포함 |
| `equity` | 주식 (Stock) | AAPL, GOOGL, MSFT |
| `mutualfund` | 뮤추얼펀드 | VFIAX, FXAIX |
| `etf` | 상장지수펀드 | SPY, QQQ, VTI |
| `index` | 인덱스 | ^GSPC, ^DJI, ^IXIC |
| `future` | 선물 | ES=F, GC=F |
| `currency` | 통화 | EURUSD=X, JPYKRW=X |
| `cryptocurrency` | 암호화폐 | BTC-USD, ETH-USD |

### 2.4 응답 JSON 구조

```
{
  "finance": {
    "result": [
      {
        "count": 25,
        "start": 0,
        "total": 156,
        "documents": [
          {
            "symbol": "AAPL",
            "name": "Apple Inc.",
            "exch": "NMS",
            "type": "S",
            "exchDisp": "NASDAQ",
            "typeDisp": "Equity"
          },
          ...
        ]
      }
    ],
    "error": null
  }
}
```

#### 2.4.1 응답 필드 상세

**최상위 구조**:
- `finance.result`: 검색 결과 배열 (일반적으로 단일 요소)
- `finance.error`: 에러 정보 (성공 시 null)

**result 필드**:
- `count`: 현재 응답에 포함된 결과 개수
- `start`: 페이징 시작 인덱스
- `total`: 전체 검색 결과 개수
- `documents`: 검색된 금융상품 목록

**documents 필드** (각 금융상품):
- `symbol`: 티커 심볼 (필수)
- `name`: 금융상품 명칭 (필수)
- `exch`: 거래소 코드 (선택)
- `type`: 상품 타입 코드 (선택)
  - "S": Stock (주식)
  - "E": ETF
  - "M": Mutual Fund
  - "I": Index
  - "F": Future
  - "C": Currency
- `exchDisp`: 거래소 표시명 (선택)
- `typeDisp`: 타입 표시명 (선택)
- `industry`: 산업 분류 (선택)
- `industryDisp`: 산업 표시명 (선택)
- `sector`: 섹터 분류 (선택)
- `sectorDisp`: 섹터 표시명 (선택)
- `score`: 검색 관련도 점수 (선택)
- `prevName`: 이전 명칭 (선택)
- `nameMatch`: 이름 매칭 여부 (선택)
- `isYahooFinance`: Yahoo Finance 공식 지원 여부 (선택)

**에러 응답 구조**:
```
{
  "finance": {
    "result": null,
    "error": {
      "code": "Bad Request",
      "description": "Invalid query parameter"
    }
  }
}
```

---

## 3. UFC 통합 설계

### 3.1 아키텍처 통합 방안

#### 3.1.1 기존 아키텍처와의 정합성

UFC는 현재 3계층 아키텍처를 사용합니다:
1. **API Layer** (`YahooClient`): 공개 API 인터페이스
2. **Domain Layer**: 도메인 모델 및 비즈니스 로직
3. **Infrastructure Layer**: Yahoo Finance HTTP 클라이언트, 인증, Rate Limiting

Lookup API도 이 구조를 따라 통합됩니다:
- `YahooClient.lookup()` 메서드로 공개 API 제공
- `com.ulalax.ufc.yahoo.model` 패키지에 도메인 모델 배치
- `com.ulalax.ufc.yahoo.internal.response` 패키지에 내부 응답 모델 배치
- 기존 `YahooAuthenticator`, `RateLimiter`, `HttpClient` 재사용

#### 3.1.2 공통 인프라 재사용

다음 인프라는 기존 구현을 그대로 재사용합니다:
- **인증**: `YahooAuthenticator`를 통한 CRUMB 토큰 획득
- **Rate Limiting**: `GlobalRateLimiters.getYahooLimiter()` 사용
- **HTTP 클라이언트**: Ktor HttpClient 재사용
- **에러 처리**: `ErrorCode`, `ApiException`, `DataParsingException` 재사용
- **JSON 파싱**: kotlinx.serialization 사용

### 3.2 네임스페이스 배치

#### 3.2.1 배치 전략

Lookup API는 `YahooClient`의 직접 메서드로 노출됩니다.

**선택 이유**:
1. Search API와 유사한 성격이지만 별도 네임스페이스를 만들 만큼 복잡하지 않음
2. `chart()`, `quoteSummary()`와 동일한 레벨로 배치하여 일관성 유지
3. 향후 Search API 추가 시 함께 고려 가능

**사용 예시**:
```kotlin
val yahoo = YahooClient.create()

// Lookup API 호출
val results = yahoo.lookup(
    query = "Apple",
    type = LookupType.EQUITY,
    count = 10
)
```

#### 3.2.2 향후 확장 가능성

만약 Search/Lookup 관련 API가 복잡해지면 별도 네임스페이스 분리 가능:
```kotlin
// 미래 확장 가능성 (현재는 구현하지 않음)
val searchResult = ufc.search.lookup(...)
val quickSearch = ufc.search.quick(...)
```

### 3.3 필요한 모델 클래스

#### 3.3.1 공개 도메인 모델 (com.ulalax.ufc.yahoo.model)

**LookupType**
- 역할: 검색할 금융상품 타입 Enum
- 필드: apiValue (Yahoo API 전송용 문자열)

**LookupResult**
- 역할: 검색 결과 전체를 담는 컨테이너
- 필드:
  - query: String (검색 키워드)
  - type: LookupType (검색 타입)
  - count: Int (반환된 결과 개수)
  - start: Int (페이징 시작 인덱스)
  - total: Int (전체 결과 개수)
  - documents: List&lt;LookupDocument&gt; (검색된 금융상품 목록)

**LookupDocument**
- 역할: 개별 금융상품 검색 결과
- 필드:
  - symbol: String (티커 심볼, 필수)
  - name: String (금융상품 명칭, 필수)
  - exchange: String? (거래소 코드, 선택)
  - exchangeDisplay: String? (거래소 표시명, 선택)
  - typeCode: String? (타입 코드: S, E, M, I, F, C, 선택)
  - typeDisplay: String? (타입 표시명, 선택)
  - industry: String? (산업 분류, 선택)
  - industryDisplay: String? (산업 표시명, 선택)
  - sector: String? (섹터 분류, 선택)
  - sectorDisplay: String? (섹터 표시명, 선택)
  - score: Double? (검색 관련도 점수, 선택)
  - isYahooFinance: Boolean? (Yahoo Finance 공식 지원 여부, 선택)

#### 3.3.2 내부 응답 모델 (com.ulalax.ufc.yahoo.internal.response)

**LookupResponse**
- 역할: Yahoo API 원본 응답 직렬화
- 필드:
  - finance: LookupFinance

**LookupFinance**
- 역할: finance 최상위 객체
- 필드:
  - result: List&lt;LookupResultResponse&gt;?
  - error: LookupError?

**LookupResultResponse**
- 역할: result 배열의 개별 요소
- 필드:
  - count: Int?
  - start: Int?
  - total: Int?
  - documents: List&lt;LookupDocumentResponse&gt;?

**LookupDocumentResponse**
- 역할: documents 배열의 개별 금융상품 (Yahoo 원본 필드명 유지)
- 필드:
  - symbol: String?
  - name: String?
  - exch: String?
  - type: String?
  - exchDisp: String?
  - typeDisp: String?
  - industry: String?
  - industryDisp: String?
  - sector: String?
  - sectorDisp: String?
  - score: Double?
  - prevName: String?
  - nameMatch: Boolean?
  - isYahooFinance: Boolean?

**LookupError**
- 역할: 에러 응답
- 필드:
  - code: String?
  - description: String?

### 3.4 API 메서드 시그니처

#### 3.4.1 YahooClient 메서드

```kotlin
/**
 * Lookup API를 호출하여 금융상품을 검색합니다.
 *
 * @param query 검색 키워드 (예: "Apple", "AAPL")
 * @param type 검색할 금융상품 타입 (기본값: LookupType.ALL)
 * @param count 반환할 최대 결과 개수 (기본값: 25, 범위: 1-100)
 * @return LookupResult
 * @throws ApiException API 호출 실패 시
 * @throws ValidationException 파라미터 검증 실패 시
 */
suspend fun lookup(
    query: String,
    type: LookupType = LookupType.ALL,
    count: Int = 25
): LookupResult
```

#### 3.4.2 파라미터 검증 규칙

- `query`:
  - 빈 문자열 불가
  - 공백만 있는 문자열 불가
  - trim 처리 후 검증

- `count`:
  - 범위: 1 이상 100 이하
  - 범위 초과 시 `ValidationException` 발생

- `type`:
  - LookupType Enum 값만 허용

---

## 4. 데이터 매핑

### 4.1 Yahoo 응답 필드 → UFC 도메인 모델 매핑

| Yahoo 필드 | UFC 도메인 필드 | 타입 변환 | 비고 |
|-----------|---------------|---------|------|
| `symbol` | `symbol` | String → String | 필수 필드 |
| `name` | `name` | String → String | 필수 필드 |
| `exch` | `exchange` | String? → String? | 선택 필드 |
| `exchDisp` | `exchangeDisplay` | String? → String? | 선택 필드 |
| `type` | `typeCode` | String? → String? | 선택 필드, 코드 값 |
| `typeDisp` | `typeDisplay` | String? → String? | 선택 필드, 표시명 |
| `industry` | `industry` | String? → String? | 선택 필드 |
| `industryDisp` | `industryDisplay` | String? → String? | 선택 필드 |
| `sector` | `sector` | String? → String? | 선택 필드 |
| `sectorDisp` | `sectorDisplay` | String? → String? | 선택 필드 |
| `score` | `score` | Double? → Double? | 선택 필드 |
| `isYahooFinance` | `isYahooFinance` | Boolean? → Boolean? | 선택 필드 |

### 4.2 타입 변환 규칙

#### 4.2.1 LookupType Enum 변환

| Kotlin Enum | Yahoo API 값 | 설명 |
|-------------|-------------|------|
| `LookupType.ALL` | "all" | 모든 타입 |
| `LookupType.EQUITY` | "equity" | 주식 |
| `LookupType.MUTUAL_FUND` | "mutualfund" | 뮤추얼펀드 |
| `LookupType.ETF` | "etf" | ETF |
| `LookupType.INDEX` | "index" | 인덱스 |
| `LookupType.FUTURE` | "future" | 선물 |
| `LookupType.CURRENCY` | "currency" | 통화 |
| `LookupType.CRYPTOCURRENCY` | "cryptocurrency" | 암호화폐 |

#### 4.2.2 TypeCode 변환

Yahoo API가 반환하는 `type` 필드는 단일 문자 코드입니다:

| Yahoo 코드 | 의미 | LookupType 대응 |
|-----------|------|---------------|
| "S" | Stock | EQUITY |
| "E" | ETF | ETF |
| "M" | Mutual Fund | MUTUAL_FUND |
| "I" | Index | INDEX |
| "F" | Future | FUTURE |
| "C" | Currency | CURRENCY |

**변환 규칙**:
- `typeCode`에는 Yahoo 원본 값("S", "E" 등)을 그대로 저장
- `typeDisplay`에는 사람이 읽을 수 있는 형태("Equity", "ETF" 등) 저장
- 추가 변환 로직 없이 원본 유지

### 4.3 Nullable 처리 전략

#### 4.3.1 필수 필드 (Non-null)

다음 필드는 반드시 존재해야 하며, 없을 경우 `DataParsingException` 발생:
- `symbol`: 티커 심볼 (금융상품 식별자)
- `name`: 금융상품 명칭

**처리 방식**:
```
LookupDocumentResponse.symbol이 null 또는 blank인 경우:
→ DataParsingException(ErrorCode.DATA_PARSING_ERROR, "symbol 필드가 필수입니다")
```

#### 4.3.2 선택 필드 (Nullable)

다음 필드는 선택 사항이며 null을 허용:
- exchange, exchangeDisplay
- typeCode, typeDisplay
- industry, industryDisplay
- sector, sectorDisplay
- score
- isYahooFinance

**처리 방식**:
- Yahoo 응답에 없으면 null로 설정
- 별도 검증 없이 그대로 전달

#### 4.3.3 빈 리스트 처리

`documents` 배열이 비어있는 경우:
- 에러가 아닌 정상 응답으로 처리
- `LookupResult.documents = emptyList()`로 반환
- `LookupResult.count = 0`, `total = 0` 설정

---

## 5. 에러 처리

### 5.1 예상되는 에러 케이스

#### 5.1.1 파라미터 검증 에러

| 케이스 | ErrorCode | HTTP 상태 | 메시지 |
|--------|-----------|----------|--------|
| 빈 검색어 | `INVALID_PARAMETER` | - | "검색어가 비어있습니다" |
| count 범위 초과 (< 1) | `INVALID_PARAMETER` | - | "count는 1 이상이어야 합니다" |
| count 범위 초과 (> 100) | `INVALID_PARAMETER` | - | "count는 100 이하여야 합니다" |

**예외 타입**: `ValidationException`

#### 5.1.2 네트워크 에러

| 케이스 | ErrorCode | HTTP 상태 | 재시도 가능 |
|--------|-----------|----------|-----------|
| 타임아웃 | `NETWORK_TIMEOUT` | - | Yes |
| 연결 실패 | `NETWORK_CONNECTION_ERROR` | - | Yes |
| DNS 오류 | `NETWORK_DNS_ERROR` | - | Yes |

**예외 타입**: `ApiException`

#### 5.1.3 Yahoo API 에러

| 케이스 | ErrorCode | HTTP 상태 | 재시도 가능 |
|--------|-----------|----------|-----------|
| 잘못된 파라미터 | `EXTERNAL_API_ERROR` | 400 | No |
| 인증 실패 | `AUTHENTICATION_FAILED` | 401 | Yes (CRUMB 재획득) |
| Rate Limit 초과 | `RATE_LIMIT_EXCEEDED` | 429 | Yes |
| 서버 오류 | `EXTERNAL_API_ERROR` | 500 | Yes |
| 서비스 불가 | `SERVICE_UNAVAILABLE` | 503 | Yes |

**예외 타입**: `ApiException`

#### 5.1.4 데이터 파싱 에러

| 케이스 | ErrorCode | 메시지 |
|--------|-----------|--------|
| JSON 파싱 실패 | `JSON_PARSING_ERROR` | "응답 JSON 파싱에 실패했습니다" |
| 필수 필드 누락 (symbol) | `DATA_PARSING_ERROR` | "symbol 필드가 필수입니다" |
| 필수 필드 누락 (name) | `DATA_PARSING_ERROR` | "name 필드가 필수입니다" |
| 빈 result 배열 | `DATA_NOT_FOUND` | "검색 결과를 찾을 수 없습니다" |

**예외 타입**: `DataParsingException` 또는 `ApiException`

### 5.2 에러 응답 구조

#### 5.2.1 Yahoo API 에러 응답

```json
{
  "finance": {
    "result": null,
    "error": {
      "code": "Bad Request",
      "description": "Missing required parameter: query"
    }
  }
}
```

**처리 방식**:
```
if (response.finance.error != null) {
    throw ApiException(
        errorCode = ErrorCode.EXTERNAL_API_ERROR,
        message = "Lookup API 에러: ${response.finance.error.description}",
        metadata = mapOf("errorCode" to response.finance.error.code)
    )
}
```

#### 5.2.2 UFC 에러 응답 구조

모든 에러는 `UfcException`의 서브클래스로 던져집니다:

```kotlin
try {
    val results = yahoo.lookup("Apple", LookupType.EQUITY)
} catch (e: ValidationException) {
    // 파라미터 검증 실패
    println("검증 오류: ${e.message}")
    println("오류 코드: ${e.errorCode}")
} catch (e: ApiException) {
    // API 호출 또는 외부 에러
    println("API 오류: ${e.message}")
    println("HTTP 상태: ${e.statusCode}")
    println("재시도 가능: ${e.errorCode.isRetryable()}")
} catch (e: DataParsingException) {
    // 데이터 파싱 실패
    println("파싱 오류: ${e.message}")
}
```

### 5.3 에러 로깅 전략

#### 5.3.1 로그 레벨

| 에러 타입 | 로그 레벨 | 비고 |
|----------|---------|------|
| 파라미터 검증 실패 | WARN | 클라이언트 코드 수정 필요 |
| 네트워크 타임아웃 | WARN | 일시적 오류, 재시도 가능 |
| Rate Limit 초과 | WARN | 예상 가능한 오류 |
| Yahoo API 에러 (4xx) | ERROR | Yahoo API 문제 |
| Yahoo API 에러 (5xx) | ERROR | Yahoo 서버 문제 |
| JSON 파싱 실패 | ERROR | 예상치 못한 응답 구조 |
| 필수 필드 누락 | ERROR | 데이터 무결성 문제 |

#### 5.3.2 로그 메시지 포맷

```
[YahooClient] Lookup API 요청 실패: query=Apple, type=equity, count=10
[ErrorCode] EXTERNAL_API_ERROR (7004)
[Details] Yahoo API returned 400 Bad Request: Missing required parameter
[Metadata] {errorCode=Bad Request, query=Apple}
```

---

## 6. 테스트 전략

### 6.1 단위 테스트 시나리오

단위 테스트는 모킹을 사용하여 외부 API 호출 없이 수행됩니다.

#### 6.1.1 파라미터 검증 테스트

**테스트 케이스**:
1. 빈 검색어 입력 시 `ValidationException` 발생
2. 공백만 있는 검색어 입력 시 `ValidationException` 발생
3. count < 1 입력 시 `ValidationException` 발생
4. count > 100 입력 시 `ValidationException` 발생
5. 정상 파라미터 입력 시 검증 통과

**테스트 파일 위치**: `/src/test/kotlin/com/ulalax/ufc/unit/yahoo/LookupValidationTest.kt`

#### 6.1.2 응답 파싱 테스트

**테스트 케이스**:
1. 정상 응답 JSON → LookupResult 변환 성공
2. 빈 documents 배열 → 빈 리스트 반환
3. symbol 누락 시 `DataParsingException` 발생
4. name 누락 시 `DataParsingException` 발생
5. 선택 필드 누락 시 null로 처리
6. 잘못된 JSON 형식 시 `JsonParsingException` 발생

**테스트 데이터**: `/src/test/resources/yahoo/lookup/`
- `lookup_apple_success.json`: 정상 응답 샘플
- `lookup_empty_results.json`: 빈 결과 샘플
- `lookup_missing_symbol.json`: symbol 누락 샘플
- `lookup_invalid_json.json`: 잘못된 JSON 샘플

**테스트 파일 위치**: `/src/test/kotlin/com/ulalax/ufc/unit/yahoo/LookupParsingTest.kt`

#### 6.1.3 에러 응답 처리 테스트

**테스트 케이스**:
1. Yahoo API 에러 응답 → `ApiException` 발생
2. HTTP 400 응답 → `ApiException` with `EXTERNAL_API_ERROR`
3. HTTP 401 응답 → `ApiException` with `AUTHENTICATION_FAILED`
4. HTTP 429 응답 → `ApiException` with `RATE_LIMIT_EXCEEDED`
5. HTTP 500 응답 → `ApiException` with `EXTERNAL_API_ERROR`
6. HTTP 503 응답 → `ApiException` with `SERVICE_UNAVAILABLE`

**테스트 파일 위치**: `/src/test/kotlin/com/ulalax/ufc/unit/yahoo/LookupErrorHandlingTest.kt`

#### 6.1.4 타입 변환 테스트

**테스트 케이스**:
1. LookupType.ALL → "all" 변환
2. LookupType.EQUITY → "equity" 변환
3. LookupType.ETF → "etf" 변환
4. LookupType.MUTUAL_FUND → "mutualfund" 변환
5. 모든 LookupType Enum 값 → API 값 변환 검증

**테스트 파일 위치**: `/src/test/kotlin/com/ulalax/ufc/unit/yahoo/LookupTypeTest.kt`

### 6.2 통합 테스트 시나리오

통합 테스트는 실제 Yahoo Finance API를 호출합니다.

#### 6.2.1 기본 동작 테스트

**테스트 그룹**: `BasicBehavior`

**테스트 케이스**:
1. "Apple" 검색 → AAPL 심볼 포함 확인
2. "Microsoft" 검색 → MSFT 심볼 포함 확인
3. "SPY" 검색 → SPY ETF 결과 확인
4. 한글 검색어 ("삼성전자") → 결과 확인

**예상 결과**:
- 각 검색어에 대해 최소 1개 이상의 결과 반환
- symbol, name 필드 필수로 존재
- 결과가 관련도 순으로 정렬됨

**테스트 파일 위치**: `/src/test/kotlin/com/ulalax/ufc/integration/yahoo/LookupSpec.kt`

#### 6.2.2 타입 필터링 테스트

**테스트 그룹**: `TypeFiltering`

**테스트 케이스**:
1. type=EQUITY로 "Apple" 검색 → 주식만 반환
2. type=ETF로 "S&P" 검색 → ETF만 반환
3. type=MUTUAL_FUND로 "Vanguard" 검색 → 뮤추얼펀드만 반환
4. type=INDEX로 "Dow Jones" 검색 → 인덱스만 반환
5. type=ALL로 "Apple" 검색 → 모든 타입 반환

**검증**:
- 각 결과의 typeCode 또는 typeDisplay가 요청 타입과 일치
- type=ALL일 때 다양한 타입 혼합 가능

#### 6.2.3 페이징 테스트

**테스트 그룹**: `Pagination`

**테스트 케이스**:
1. count=5로 검색 → 최대 5개 결과 반환
2. count=50로 검색 → 최대 50개 결과 반환
3. count=100로 검색 → 최대 100개 결과 반환
4. total > count인 경우 → 추가 결과 존재 확인

**검증**:
- `LookupResult.count` <= 요청한 count
- `LookupResult.documents.size` == `LookupResult.count`
- `LookupResult.total`이 전체 결과 개수를 나타냄

#### 6.2.4 응답 데이터 스펙 테스트

**테스트 그룹**: `ResponseSpec`

**테스트 케이스**:
1. 검색 결과는 symbol, name을 필수로 포함
2. exchange, exchangeDisplay는 선택 필드
3. sector, industry는 주식에만 존재
4. score 필드는 관련도를 나타내며 높은 순으로 정렬
5. isYahooFinance 필드 확인

#### 6.2.5 에러 케이스 테스트

**테스트 그룹**: `ErrorCases`

**테스트 케이스**:
1. 존재하지 않는 검색어 → 빈 결과 반환 (에러 아님)
2. 특수문자만 검색 → 빈 결과 또는 에러
3. 매우 긴 검색어 (1000자) → 적절한 에러 또는 결과
4. Rate Limit 초과 시나리오 → `RATE_LIMIT_EXCEEDED` 에러

#### 6.2.6 실전 활용 예제 테스트

**테스트 그룹**: `UsageExamples`

**테스트 케이스**:
1. Lookup으로 심볼 찾기 → QuoteSummary API 연계 조회
2. 자동완성 구현 예제 → "App" 입력 시 Apple 관련 종목 제안
3. 크로스 마켓 검색 → 동일 회사의 다양한 거래소 상장 종목 조회
4. 섹터별 종목 탐색 → 특정 산업군 검색

**예제**:
```kotlin
// 1. Lookup으로 심볼 찾기
val lookupResult = yahoo.lookup("Apple", LookupType.EQUITY, count = 5)
val firstSymbol = lookupResult.documents.firstOrNull()?.symbol

if (firstSymbol != null) {
    // 2. 찾은 심볼로 상세 정보 조회
    val quote = yahoo.quoteSummary(firstSymbol, QuoteSummaryModule.PRICE)
    // ...
}
```

### 6.3 테스트 데이터 관리

#### 6.3.1 Fixture 데이터

**위치**: `/src/test/kotlin/com/ulalax/ufc/fixture/TestFixtures.kt`

**추가할 데이터**:
```kotlin
object Lookup {
    const val QUERY_APPLE = "Apple"
    const val QUERY_MICROSOFT = "Microsoft"
    const val QUERY_SPY = "SPY"
    const val QUERY_VANGUARD = "Vanguard"
    const val QUERY_NONEXISTENT = "ZZZNONEXISTENT9999"
}
```

#### 6.3.2 Response Recording

통합 테스트 실행 시 실제 API 응답을 JSON 파일로 저장:

**저장 위치**: `/src/test/resources/recorded/yahoo/lookup/`
- `apple_equity.json`
- `microsoft_all.json`
- `spy_etf.json`
- `vanguard_mutualfund.json`

**목적**:
1. 단위 테스트용 샘플 데이터 생성
2. API 응답 구조 변경 감지
3. 오프라인 테스트 지원

---

## 7. 구현 우선순위

### 7.1 Phase 1: 핵심 기능 구현

1. **모델 클래스 정의**
   - LookupType Enum
   - LookupResult, LookupDocument (공개 모델)
   - LookupResponse, LookupFinance, LookupResultResponse, LookupDocumentResponse (내부 모델)

2. **YahooClient.lookup() 메서드 구현**
   - 파라미터 검증
   - API 호출
   - 응답 파싱
   - 에러 처리

3. **단위 테스트 작성**
   - 파라미터 검증 테스트
   - 응답 파싱 테스트
   - 에러 핸들링 테스트

### 7.2 Phase 2: 통합 테스트 및 검증

1. **통합 테스트 작성**
   - 기본 동작 테스트
   - 타입 필터링 테스트
   - 페이징 테스트

2. **실전 시나리오 테스트**
   - QuoteSummary와 연계 테스트
   - 자동완성 구현 예제

### 7.3 Phase 3: 문서화 및 최적화

1. **KDoc 문서화**
   - 모든 공개 API에 KDoc 주석 추가
   - 사용 예제 포함

2. **성능 최적화**
   - 응답 캐싱 검토 (필요 시)
   - Rate Limiting 튜닝

---

## 8. 참고 사항

### 8.1 yfinance 구현과의 차이점

| 항목 | yfinance | UFC |
|------|----------|-----|
| 반환 타입 | pandas.DataFrame | LookupResult (도메인 모델) |
| 메서드 구조 | get_all(), get_stock() 등 개별 메서드 | lookup(type) 단일 메서드 |
| 프로퍼티 접근 | .all, .stock 등 프로퍼티 | 지원 안 함 (명시적 메서드 호출만) |
| 캐싱 | 인스턴스 레벨 캐싱 | 전역 캐싱 (필요 시) |

**UFC 선택 이유**:
- Kotlin 타입 시스템 활용 (타입 안정성)
- 명시적 메서드 호출로 의도 명확화
- 도메인 모델 사용으로 비즈니스 로직 분리

### 8.2 제약 사항

1. **Yahoo Finance 비공식 API**
   - 공식 지원 없음, 구조 변경 가능성 있음
   - Rate Limiting 정책 명확하지 않음
   - 상업적 사용 제한 가능

2. **검색 품질**
   - 검색 알고리즘이 Yahoo 서버에 의존
   - 일부 검색어는 예상과 다른 결과 반환 가능
   - 국가별, 언어별 결과 차이 존재

3. **데이터 완전성**
   - sector, industry 등 선택 필드는 일부 종목에만 존재
   - 거래소 정보가 없는 경우 존재
   - ISIN 등 국제 식별자는 Lookup API에서 제공되지 않음 (별도 API 사용 필요)

### 8.3 향후 확장 가능성

1. **검색 옵션 추가**
   - region, lang 파라미터 노출
   - formatted 옵션 지원
   - fetchPricingData 옵션 지원

2. **결과 정렬 및 필터링**
   - 클라이언트 측 정렬 기능 (score, name 등)
   - 거래소별 필터링
   - 국가별 필터링

3. **캐싱 전략**
   - 동일 검색어에 대한 응답 캐싱 (TTL: 1시간)
   - LRU 캐시 적용

4. **Search API 통합**
   - `/v1/finance/search` 추가 구현
   - Lookup과 Search 결과 통합 인터페이스 제공

---

## 9. 체크리스트

### 9.1 구현 완료 기준

- [ ] LookupType Enum 정의 완료
- [ ] LookupResult, LookupDocument 모델 정의 완료
- [ ] 내부 응답 모델 (LookupResponse 등) 정의 완료
- [ ] YahooApiUrls에 LOOKUP 상수 추가
- [ ] YahooClient.lookup() 메서드 구현 완료
- [ ] 파라미터 검증 로직 구현 완료
- [ ] 응답 파싱 로직 구현 완료
- [ ] 에러 처리 로직 구현 완료
- [ ] 단위 테스트 작성 완료 (파라미터, 파싱, 에러)
- [ ] 통합 테스트 작성 완료 (기본 동작, 타입 필터링, 페이징)
- [ ] KDoc 문서화 완료
- [ ] README 업데이트 (사용 예제 추가)

### 9.2 테스트 통과 기준

- [ ] 모든 단위 테스트 통과
- [ ] 모든 통합 테스트 통과 (실제 API 호출)
- [ ] 에러 케이스 테스트 통과
- [ ] yfinance 참조 구현과 결과 일치 확인

### 9.3 문서화 완료 기준

- [ ] 모든 공개 API에 KDoc 주석 추가
- [ ] 사용 예제 코드 작성
- [ ] README에 Lookup API 섹션 추가
- [ ] 이 명세서를 기반으로 한 구현 가이드 작성 (옵션)

---

## 부록 A: API 호출 예시

### A.1 기본 검색

**요청**:
```
GET https://query1.finance.yahoo.com/v1/finance/lookup?query=Apple&type=all&count=10&crumb=xxx
```

**응답**:
```json
{
  "finance": {
    "result": [
      {
        "count": 10,
        "start": 0,
        "total": 156,
        "documents": [
          {
            "symbol": "AAPL",
            "name": "Apple Inc.",
            "exch": "NMS",
            "type": "S",
            "exchDisp": "NASDAQ",
            "typeDisp": "Equity",
            "sector": "Technology",
            "sectorDisp": "Technology",
            "industry": "Consumer Electronics",
            "industryDisp": "Consumer Electronics"
          },
          {
            "symbol": "AAPL.MX",
            "name": "Apple Inc.",
            "exch": "MEX",
            "type": "S",
            "exchDisp": "Mexico",
            "typeDisp": "Equity"
          }
        ]
      }
    ],
    "error": null
  }
}
```

### A.2 타입 필터링 (ETF만)

**요청**:
```
GET https://query1.finance.yahoo.com/v1/finance/lookup?query=S%26P&type=etf&count=5&crumb=xxx
```

**응답**:
```json
{
  "finance": {
    "result": [
      {
        "count": 5,
        "start": 0,
        "total": 47,
        "documents": [
          {
            "symbol": "SPY",
            "name": "SPDR S&P 500 ETF Trust",
            "exch": "PCX",
            "type": "E",
            "exchDisp": "NYSE Arca",
            "typeDisp": "ETF"
          },
          {
            "symbol": "VOO",
            "name": "Vanguard S&P 500 ETF",
            "exch": "PCX",
            "type": "E",
            "exchDisp": "NYSE Arca",
            "typeDisp": "ETF"
          }
        ]
      }
    ],
    "error": null
  }
}
```

### A.3 검색 결과 없음

**요청**:
```
GET https://query1.finance.yahoo.com/v1/finance/lookup?query=ZZZNONEXISTENT9999&type=all&count=10&crumb=xxx
```

**응답**:
```json
{
  "finance": {
    "result": [
      {
        "count": 0,
        "start": 0,
        "total": 0,
        "documents": []
      }
    ],
    "error": null
  }
}
```

### A.4 에러 응답

**요청**:
```
GET https://query1.finance.yahoo.com/v1/finance/lookup?type=all&count=10&crumb=xxx
(query 파라미터 누락)
```

**응답**:
```json
{
  "finance": {
    "result": null,
    "error": {
      "code": "Bad Request",
      "description": "Missing required parameter: query"
    }
  }
}
```

---

**문서 끝**
