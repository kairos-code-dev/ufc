# Business Insider ISIN Search API 기술 명세서

> **Version**: 1.0
> **작성일**: 2025-12-05

---

## 1. 개요

### 1.1 API 목적

Business Insider Markets API를 통해 **ISIN(International Securities Identification Number)으로 금융 상품을 검색**한다.

| 항목 | 값 |
|-----|---|
| 엔드포인트 | `GET https://markets.businessinsider.com/ajax/SearchController_Suggest` |
| HTTP 메서드 | GET |
| 인증 | 불필요 |

### 1.2 제공 데이터

| 데이터 | 설명 |
|-------|------|
| ISIN | 국제 증권 식별 번호 |
| 심볼 | 티커 심볼 (AAPL, MSFT 등) |
| 종목명 | 금융상품 정식 명칭 |
| 유형 정보 | 상품 타입 (Stocks, ETF 등) |

### 1.3 Yahoo Finance Lookup API와의 차이

| 구분 | Business Insider | Yahoo Lookup |
|-----|-----------------|--------------|
| 엔드포인트 | `/ajax/SearchController_Suggest` | `/v1/finance/lookup` |
| 검색 방식 | ISIN 기반 | 키워드 기반 |
| 용도 | ISIN → Symbol 변환 | 종목 검색 |
| 글로벌 커버리지 | 넓음 (전세계 증권) | Yahoo 중심 |
| ISIN 지원 | 직접 지원 | 미지원 |
| 인증 | 불필요 | CRUMB 토큰 필요 |

### 1.4 주요 사용 사례

| 사용 사례 | 설명 |
|---------|------|
| ISIN → 심볼 변환 | 유럽/아시아 증권을 Yahoo Finance 심볼로 변환 |
| 크로스 마켓 검색 | 동일 기업의 다중 상장 정보 조회 |
| 글로벌 포트폴리오 | 국제 포트폴리오 관리 시 심볼 통일 |
| Yahoo 보완 | Yahoo Finance에서 ISIN 검색 불가 시 대안 |

---

## 2. 데이터 소스 분석

### 2.1 요청 파라미터

| 파라미터 | 타입 | 필수 | 기본값 | 제약 | 설명 |
|---------|------|------|--------|-----|------|
| query | String | Yes | - | 12자리 영숫자 | ISIN 검색어 |
| max_results | Int | No | 10 | 1 이상 | 최대 결과 개수 |

### 2.2 ISIN 형식

| 구성 요소 | 길이 | 설명 | 예시 |
|---------|------|------|------|
| 국가 코드 | 2자리 | ISO 3166-1 alpha-2 | US, KR, GB, DE, JP |
| 기본 식별자 | 9자리 | 각 국가별 식별 번호 | 037833100 |
| 체크 디지트 | 1자리 | Luhn 알고리즘 기반 검증 숫자 | 5 |
| **전체** | **12자리** | **정규식: `^[A-Z]{2}[A-Z0-9]{9}[0-9]$`** | **US0378331005** |

### 2.3 ISIN 예시

| ISIN | 종목명 | 심볼 | 국가 |
|------|-------|------|------|
| US0378331005 | Apple Inc | AAPL | 미국 |
| US5949181045 | Microsoft Corp | MSFT | 미국 |
| US88160R1014 | Tesla Inc | TSLA | 미국 |
| KR7005930003 | Samsung Electronics | - | 한국 |
| KR7000660001 | SK Hynix | - | 한국 |
| GB0005405286 | HSBC Holdings | - | 영국 |
| DE0007164600 | SAP SE | - | 독일 |
| JP3633400001 | Toyota Motor Corp | - | 일본 |
| FR0000120271 | TotalEnergies SE | - | 프랑스 |

### 2.4 응답 구조

응답은 JavaScript callback 형식으로 반환됩니다:

```javascript
mmSuggestDeliver(0,
    new Array("Name", "Category", "Keywords", ...),
    new Array(
        new Array("Apple Inc.", "Stocks", "AAPL|US0378331005|AAPL||AAPL", ...),
        ...
    ),
    count, 0
);
```

### 2.5 응답 필드 파싱

| 배열 인덱스 | 필드명 | 설명 | 예시 |
|-----------|-------|------|------|
| 0 | Name | 종목명 | "Apple Inc." |
| 1 | Category | 상품 유형 | "Stocks" |
| 2 | Keywords | 심볼과 ISIN (파이프 구분) | "AAPL\|US0378331005\|AAPL\|\|AAPL" |

### 2.6 Keywords 필드 구조

Keywords 필드는 파이프(`|`)로 구분된 문자열입니다:

| 위치 | 값 | 설명 |
|-----|---|------|
| 0 | Symbol | 티커 심볼 (예: AAPL) |
| 1 | ISIN | 국제 증권 식별 번호 (예: US0378331005) |
| 2 | Symbol (중복) | 티커 심볼 반복 |
| 3 | 빈 값 | - |
| 4 | Symbol (중복) | 티커 심볼 반복 |

예시: `"AAPL|US0378331005|AAPL||AAPL"`

---

## 3. 데이터/모델 설계

### 3.1 Domain 모델

#### IsinSearchResult

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| isin | String | No | 국제증권식별번호 (12자리 영숫자) |
| symbol | String | No | 거래소 심볼 (예: AAPL, MSFT) |
| name | String | No | 종목명 |
| exchange | String | Yes | 거래소 코드 (예: NASDAQ, NYSE) |
| currency | String | Yes | 통화 코드 (예: USD, KRW) |
| type | String | Yes | 증권 유형 (예: Stocks, ETF, Bond) |

```kotlin
data class IsinSearchResult(
    val isin: String,
    val symbol: String,
    val name: String,
    val exchange: String? = null,
    val currency: String? = null,
    val type: String? = null
)
```

### 3.2 Internal Response 모델

Business Insider API는 구조화된 JSON이 아닌 JavaScript callback을 반환하므로, Internal Response 모델은 파싱 중간 단계로만 사용됩니다.

#### ParsedResult (Internal)

| 필드 | 타입 | 설명 |
|-----|------|------|
| name | String | 종목명 |
| category | String | 상품 유형 |
| symbol | String | 티커 심볼 |
| isin | String | ISIN 코드 |

```kotlin
private data class ParsedResult(
    val name: String,
    val category: String,
    val symbol: String,
    val isin: String
)
```

### 3.3 API 메서드 시그니처

```kotlin
suspend fun searchIsin(isin: String): IsinSearchResult
```

| 파라미터 | 타입 | 제약 | 설명 |
|---------|------|------|------|
| isin | String | 필수, 12자리 영숫자 | 국제증권식별번호 |

| 반환 | 설명 |
|-----|------|
| IsinSearchResult | ISIN 검색 결과 (심볼, 이름, 유형 등) |

### 3.4 필드 매핑

| Business Insider 필드 | Domain 필드 | 변환 |
|---------------------|------------|------|
| Keywords[1] (파싱) | isin | 대문자 정규화 |
| Keywords[0] (파싱) | symbol | 필수 검증, 빈 문자열 시 예외 |
| Name | name | 그대로 |
| - | exchange | null (Business Insider 미제공) |
| - | currency | null (Business Insider 미제공) |
| Category | type | 그대로 |

### 3.5 필수 필드 검증

| 필드 | 누락 시 처리 |
|-----|-----------|
| symbol | DATA_PARSING_ERROR 발생 |
| name | 항목 제외 (필터링) |
| isin | 요청 ISIN으로 대체 |

### 3.6 응답 파싱 로직

| 단계 | 처리 내용 |
|-----|---------|
| 1 | JavaScript callback 문자열 수신 |
| 2 | 정규식으로 `new Array("Name", "Category", "Keywords")` 패턴 추출 |
| 3 | 헤더 행 건너뛰기 (Name == "Name" && Category == "Category") |
| 4 | Keywords 필드를 `\|`로 분리하여 Symbol, ISIN 추출 |
| 5 | 요청한 ISIN과 정확히 일치하는 결과 찾기 |
| 6 | 일치하는 결과 없으면 첫 번째 결과 사용 |
| 7 | IsinSearchResult로 변환 |

---

## 4. 예외 처리

### 4.1 ErrorCode 매핑

| 에러 케이스 | ErrorCode | 설명 |
|-----------|-----------|------|
| ISIN 길이 != 12 | INVALID_PARAMETER | ISIN은 정확히 12자 |
| ISIN 형식 오류 | INVALID_PARAMETER | 정규식 검증 실패 |
| Symbol 누락 | DATA_PARSING_ERROR | 필수 필드 검증 실패 |
| 검색 결과 없음 | DATA_NOT_FOUND | 해당 ISIN 없음 |
| HTTP 4xx/5xx | EXTERNAL_API_ERROR | API 오류 |
| Rate Limit (429) | RATE_LIMITED | 요청 제한 |
| 파싱 실패 | DATA_PARSING_ERROR | JavaScript callback 파싱 실패 |
| 네트워크 오류 | EXTERNAL_API_ERROR | 연결 실패, 타임아웃 등 |

### 4.2 ISIN 검증 규칙

| 검증 항목 | 규칙 | 예외 |
|---------|-----|------|
| 길이 | 정확히 12자 | INVALID_PARAMETER |
| 형식 | `^[A-Z]{2}[A-Z0-9]{9}[0-9]$` | INVALID_PARAMETER |
| 국가 코드 | 대문자 2자리 | INVALID_PARAMETER |
| 체크 디지트 | 마지막 1자리는 숫자 | INVALID_PARAMETER |

### 4.3 빈 결과 처리

| 상황 | 처리 |
|-----|------|
| 검색 결과 없음 | DATA_NOT_FOUND 예외 발생 |
| 잘못된 ISIN 형식 | INVALID_PARAMETER 예외 발생 |
| Symbol 필드 빈 문자열 | DATA_PARSING_ERROR 예외 발생 |
| Name 필드 빈 문자열 | 항목 제외 (다음 결과 사용) |

### 4.4 재시도 정책

| 에러 | 재시도 | 횟수 |
|-----|-------|-----|
| Rate Limit (429) | Yes | Rate Limiter 처리 |
| Network Error | Yes | 3회 |
| HTTP 5xx | Yes | 3회 |
| HTTP 4xx | No | - |
| ISIN 검증 실패 | No | - |
| 파싱 실패 | No | - |

### 4.5 예외 메시지

| ErrorCode | 메시지 형식 |
|-----------|-----------|
| INVALID_PARAMETER | "ISIN must be 12 characters: {isin}" |
| INVALID_PARAMETER | "Invalid ISIN format: {isin}" |
| DATA_NOT_FOUND | "No results found for ISIN: {isin}" |
| DATA_PARSING_ERROR | "Symbol not found for ISIN: {isin}" |
| EXTERNAL_API_ERROR | "Failed to search ISIN {isin}: {message}" |

---

## 5. 참고 자료

### 5.1 API 제약사항

| 제약 | 설명 |
|-----|------|
| 비공식 API | Business Insider 내부 자동완성 API 활용 |
| 문서 없음 | 공식 문서화되지 않음, 변경 가능 |
| Rate Limiting | 존재 (GlobalRateLimiters에서 관리) |
| 결과 정확도 | 모든 결과가 정확하지 않을 수 있음 (검증 필요) |
| JavaScript 응답 | JSON이 아닌 JavaScript callback 형식 |

### 5.2 ISIN 체계

| 국가 코드 | 국가명 | 예시 ISIN |
|---------|-------|----------|
| US | 미국 | US0378331005 (Apple) |
| KR | 한국 | KR7005930003 (삼성전자) |
| GB | 영국 | GB0005405286 (HSBC) |
| DE | 독일 | DE0007164600 (SAP) |
| JP | 일본 | JP3633400001 (Toyota) |
| FR | 프랑스 | FR0000120271 (TotalEnergies) |
| CH | 스위스 | CH0012032048 (Roche) |
| CN | 중국 | CNE1000001Z5 (Alibaba) |

### 5.3 ISIN vs 기타 식별자

| 식별자 | 범위 | 예시 | 특징 |
|-------|-----|------|------|
| ISIN | 글로벌 표준 | US0378331005 | 12자리, 국가별 코드 포함 |
| CUSIP | 미국/캐나다 | 037833100 | 9자리, ISIN의 일부 |
| SEDOL | 영국 | 2046251 | 7자리, 런던증권거래소 |
| Ticker Symbol | 거래소별 | AAPL | 가변 길이, 거래소마다 다를 수 있음 |

### 5.4 사용 시 주의사항

| 주의사항 | 설명 |
|---------|------|
| ISIN 정규화 | 소문자 입력 시 대문자로 자동 변환 |
| Symbol 검증 | 반환된 심볼을 Yahoo Finance API에서 재검증 권장 |
| 다중 상장 | 동일 기업이 여러 거래소에 상장된 경우 주의 |
| 한국 종목 | KR로 시작하는 ISIN의 경우 심볼이 없을 수 있음 |
| 캐싱 권장 | 동일 ISIN 반복 조회 시 결과 캐싱 고려 |

### 5.5 Rate Limiting 설정

| 설정 항목 | 기본값 | 설명 |
|---------|-------|------|
| 초당 요청 수 | 10 RPS | GlobalRateLimiters 관리 |
| 버스트 허용 | 10 | Token Bucket 용량 |
| 타임아웃 | 60초 | 요청 타임아웃 |
| 재시도 대기 | 지수 백오프 | Rate Limit 시 |

### 5.6 Yahoo Finance API 연동

Business Insider에서 조회한 심볼은 Yahoo Finance API에서 바로 사용 가능합니다:

| 단계 | API | 목적 |
|-----|-----|------|
| 1 | BusinessInsider.searchIsin() | ISIN → Symbol 변환 |
| 2 | Yahoo.quoteSummary() | 심볼로 시세 조회 |
| 3 | Yahoo.chart() | 심볼로 차트 데이터 조회 |

### 5.7 제한 사항

| 제한 | 설명 |
|-----|------|
| Exchange 미제공 | Business Insider는 거래소 정보 미제공 |
| Currency 미제공 | Business Insider는 통화 정보 미제공 |
| 정확도 보장 불가 | 자동완성 API라 100% 정확성 보장 안 됨 |
| API 변경 가능 | 비공식 API로 예고 없이 변경될 수 있음 |
| ISIN 외 검색 불가 | Symbol 검색은 지원하지 않음 |

### 5.8 GlobalRateLimiters 설정

Business Insider API는 `GlobalRateLimiters.getBusinessInsiderLimiter()`를 통해 전역 Rate Limiter를 사용합니다:

```kotlin
val rateLimiter = GlobalRateLimiters.getBusinessInsiderLimiter(config.rateLimitConfig)
```

| 특징 | 설명 |
|-----|------|
| 싱글톤 패턴 | 모든 클라이언트 인스턴스가 동일한 Rate Limiter 공유 |
| Token Bucket | 버스트 트래픽 허용하면서 평균 속도 제한 |
| 자동 대기 | Rate Limit 도달 시 자동으로 대기 |

### 5.9 활용 예제

#### 예제 1: ISIN으로 Yahoo Finance 심볼 검색

```kotlin
val ufc = Ufc.create()

// 1. ISIN으로 검색
val result = ufc.searchIsin("US0378331005")
println("Symbol: ${result.symbol}")  // AAPL

// 2. Yahoo Finance API에서 사용
val quote = ufc.quoteSummary(result.symbol, QuoteSummaryModule.PRICE)
```

#### 예제 2: 글로벌 포트폴리오 관리

```kotlin
val isins = listOf(
    "US0378331005",  // Apple (US)
    "KR7005930003",  // Samsung (KR)
    "JP3633400001",  // Toyota (JP)
    "GB0005405286"   // HSBC (GB)
)

val portfolio = isins.map { isin ->
    ufc.searchIsin(isin)
}
```

#### 예제 3: 에러 처리

```kotlin
try {
    val result = ufc.searchIsin("INVALID")
} catch (e: ValidationException) {
    println("Invalid ISIN format: ${e.message}")
} catch (e: ApiException) {
    when (e.errorCode) {
        ErrorCode.DATA_NOT_FOUND -> println("ISIN not found")
        ErrorCode.RATE_LIMITED -> println("Rate limit exceeded")
        else -> println("API error: ${e.message}")
    }
}
```

---

## 부록

### A. ISIN 체크 디지트 알고리즘

ISIN의 마지막 자리는 Luhn 알고리즘을 사용한 체크 디지트입니다:

1. ISIN의 앞 11자리를 숫자로 변환 (A=10, B=11, ..., Z=35)
2. 변환된 숫자를 우측에서 좌측으로 홀수/짝수 위치별로 처리
3. 짝수 위치는 2를 곱하고, 10 이상이면 각 자리수를 합산
4. 모든 숫자를 합산
5. 합계를 10으로 나눈 나머지를 10에서 뺀 값이 체크 디지트

### B. Business Insider API 응답 예시

```javascript
mmSuggestDeliver(0,
    new Array("Name", "Category", "Keywords", "Country", "URL"),
    new Array(
        new Array("Apple Inc.", "Stocks", "AAPL|US0378331005|AAPL||AAPL", "United States", "/stocks/aapl-stock"),
        new Array("Microsoft Corporation", "Stocks", "MSFT|US5949181045|MSFT||MSFT", "United States", "/stocks/msft-stock")
    ),
    2, 0
);
```

### C. 정규식 패턴

| 용도 | 정규식 | 설명 |
|-----|-------|------|
| ISIN 검증 | `^[A-Z]{2}[A-Z0-9]{9}[0-9]$` | 국가코드(2) + 식별자(9) + 체크섬(1) |
| 배열 파싱 | `new Array\("([^"]*)"\s*,\s*"([^"]*)"\s*,\s*"([^"]*)` | JavaScript callback 파싱 |

### D. 관련 링크

| 리소스 | URL |
|-------|-----|
| ISIN 공식 표준 | ISO 6166 |
| Business Insider Markets | https://markets.businessinsider.com |
| ISIN 검증 도구 | https://www.isin.org/isin/ |
