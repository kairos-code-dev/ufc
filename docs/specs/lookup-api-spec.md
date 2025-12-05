# Lookup API 기술 명세서

> **Version**: 2.1
> **작성일**: 2025-12-05
> **최종 수정일**: 2025-12-05
> **변경 내역**: Yahoo Finance API 필드 구조 변경 반영 (새 필드 추가, 폴백 로직 문서화)

---

## 1. 개요

### 1.1 API 목적

Yahoo Finance Lookup API를 통해 검색어 기반으로 **금융상품(주식, ETF, 펀드 등)을 검색**한다.

| 항목 | 값 |
|-----|---|
| 엔드포인트 | `GET https://query1.finance.yahoo.com/v1/finance/lookup` |
| HTTP 메서드 | GET |
| 인증 | CRUMB 토큰 필요 |

### 1.2 제공 데이터

| 데이터 | 설명 |
|-------|------|
| 심볼 | 티커 심볼 (AAPL, MSFT 등) |
| 종목명 | 금융상품 정식 명칭 |
| 거래소 정보 | 거래소 코드 및 표시명 |
| 타입 정보 | 상품 타입 (주식, ETF, 펀드 등) |
| 섹터/산업 | 주식의 경우 섹터 및 산업 분류 |
| 관련도 점수 | 검색어와의 관련도 |

### 1.3 Search API와의 차이

| 구분 | Lookup API | Search API |
|-----|-----------|-----------|
| 엔드포인트 | `/v1/finance/lookup` | `/v1/finance/search` |
| 용도 | 타입별 정밀 검색 | 범용 키워드 검색 |
| 필터링 | 타입별 필터 지원 | 제한적 |
| 결과 구조 | 단일 타입 집중 | 다양한 타입 혼합 |
| 정렬 | 관련도 기반 | 관련도 + 인기도 |

---

## 2. 데이터 소스 분석

### 2.1 요청 파라미터

| 파라미터 | 타입 | 필수 | 기본값 | 제약 | 설명 |
|---------|------|------|--------|-----|------|
| query | String | Yes | - | 빈 문자열 불가 | 검색 키워드 |
| type | String | No | "all" | 유효한 타입만 | 금융상품 타입 필터 |
| count | Int | No | 25 | 1-100 | 최대 결과 개수 |
| start | Int | No | 0 | 0 이상 | 페이징 시작 인덱스 |
| crumb | String | Yes | - | - | CRUMB 인증 토큰 |

### 2.2 type 파라미터 값

| 타입 값 | 설명 | 예시 |
|--------|------|------|
| all | 모든 타입 | - |
| equity | 주식 | AAPL, GOOGL |
| mutualfund | 뮤추얼펀드 | VFIAX, FXAIX |
| etf | ETF | SPY, QQQ |
| index | 인덱스 | ^GSPC, ^DJI |
| future | 선물 | ES=F, GC=F |
| currency | 통화 | EURUSD=X |
| cryptocurrency | 암호화폐 | BTC-USD |

### 2.3 응답 구조

| 클래스 | 필드 | 타입 | Nullable |
|-------|-----|------|----------|
| LookupResponse | finance | LookupFinance | No |
| LookupFinance | result | List&lt;LookupResultResponse&gt; | Yes |
| | error | LookupError | Yes |
| LookupResultResponse | count | Int | Yes |
| | start | Int | Yes |
| | total | Int | Yes |
| | documents | List&lt;LookupDocumentResponse&gt; | Yes |
| LookupDocumentResponse | symbol | String | Yes |
| | shortName | String | Yes |
| | exchange | String | Yes |
| | quoteType | String | Yes |
| | industryName | String | Yes |
| | rank | Int | Yes |
| | industryLink | String | Yes |
| | name | String | Yes |
| | exch | String | Yes |
| | type | String | Yes |
| | exchDisp | String | Yes |
| | typeDisp | String | Yes |
| | industry | String | Yes |
| | industryDisp | String | Yes |
| | sector | String | Yes |
| | sectorDisp | String | Yes |
| | score | Double | Yes |
| | isYahooFinance | Boolean | Yes |
| LookupError | code | String | Yes |
| | description | String | Yes |

### 2.4 documents 필드 상세

| 필드 | 설명 | 예시 | 비고 |
|-----|------|------|------|
| symbol | 티커 심볼 | "AAPL" | |
| shortName | 금융상품 명칭 | "Apple Inc." | 주요 필드 |
| exchange | 거래소 코드 | "NMS" | 주요 필드 |
| quoteType | 타입 코드 | "EQUITY" | 주요 필드 |
| industryName | 산업 표시명 | "Consumer Electronics" | 주요 필드 |
| rank | 관련도 점수 (정수) | 1234 | 주요 필드 |
| industryLink | 산업 링크 | "/sector/ms_technology" | |
| name | 금융상품 명칭 | "Apple Inc." | 하위호환성 유지 |
| exch | 거래소 코드 | "NMS" | 하위호환성 유지 |
| type | 타입 코드 | "S" (Stock) | 하위호환성 유지 |
| exchDisp | 거래소 표시명 | "NASDAQ" | |
| typeDisp | 타입 표시명 | "Equity" | |
| industry | 산업 코드 | - | |
| industryDisp | 산업 표시명 | "Consumer Electronics" | 하위호환성 유지 |
| sector | 섹터 코드 | - | |
| sectorDisp | 섹터 표시명 | "Technology" | |
| score | 관련도 점수 | 1234.56 | 하위호환성 유지 |
| isYahooFinance | Yahoo Finance 지원 여부 | true | |

**참고**: Yahoo Finance API는 필드 구조를 변경했습니다. 새로운 필드(`shortName`, `exchange`, `quoteType`, `industryName`, `rank`)가 주요 필드이며, 기존 필드(`name`, `exch`, `type`, `industryDisp`, `score`)는 하위 호환성을 위해 유지됩니다. 구현은 새 필드를 우선 사용하고, 없으면 기존 필드로 폴백합니다.

### 2.5 TypeCode 매핑

| 코드 | 의미 | LookupType 대응 |
|-----|------|---------------|
| S | Stock | EQUITY |
| E | ETF | ETF |
| M | Mutual Fund | MUTUAL_FUND |
| I | Index | INDEX |
| F | Future | FUTURE |
| C | Currency | CURRENCY |

---

## 3. 데이터/모델 설계

### 3.1 Domain 모델

#### LookupResult

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| query | String | No | 검색 키워드 |
| type | LookupType | No | 검색 타입 |
| count | Int | No | 반환된 결과 개수 |
| start | Int | No | 페이징 시작 인덱스 |
| total | Int | No | 전체 결과 개수 |
| documents | List&lt;LookupDocument&gt; | No | 검색 결과 목록 |

#### LookupDocument

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| symbol | String | No | 티커 심볼 |
| name | String | No | 금융상품 명칭 |
| exchange | String | Yes | 거래소 코드 |
| exchangeDisplay | String | Yes | 거래소 표시명 |
| typeCode | String | Yes | 타입 코드 (S, E, M 등) |
| typeDisplay | String | Yes | 타입 표시명 |
| industry | String | Yes | 산업 분류 코드 |
| industryDisplay | String | Yes | 산업 표시명 |
| sector | String | Yes | 섹터 분류 코드 |
| sectorDisplay | String | Yes | 섹터 표시명 |
| score | Double | Yes | 검색 관련도 점수 |
| isYahooFinance | Boolean | Yes | Yahoo Finance 지원 여부 |

#### LookupType

```kotlin
enum class LookupType(val apiValue: String) {
    ALL("all"),
    EQUITY("equity"),
    MUTUAL_FUND("mutualfund"),
    ETF("etf"),
    INDEX("index"),
    FUTURE("future"),
    CURRENCY("currency"),
    CRYPTOCURRENCY("cryptocurrency")
}
```

### 3.2 Internal Response 모델

#### 구조

| 클래스 | 필드 | 타입 |
|-------|-----|------|
| LookupResponse | finance | LookupFinance |
| LookupFinance | result | List&lt;LookupResultResponse&gt;? |
| | error | LookupError? |
| LookupResultResponse | count | Int? |
| | start | Int? |
| | total | Int? |
| | documents | List&lt;LookupDocumentResponse&gt;? |
| LookupDocumentResponse | symbol | String? |
| | shortName | String? |
| | exchange | String? |
| | quoteType | String? |
| | industryName | String? |
| | rank | Int? |
| | industryLink | String? |
| | name | String? |
| | exch | String? |
| | type | String? |
| | exchDisp | String? |
| | typeDisp | String? |
| | industry | String? |
| | industryDisp | String? |
| | sector | String? |
| | sectorDisp | String? |
| | score | Double? |
| | isYahooFinance | Boolean? |
| LookupError | code | String? |
| | description | String? |

### 3.3 API 메서드 시그니처

```kotlin
suspend fun lookup(
    query: String,
    type: LookupType = LookupType.ALL,
    count: Int = 25
): LookupResult
```

| 파라미터 | 타입 | 기본값 | 제약 |
|---------|------|-------|------|
| query | String | - | 필수, 빈 문자열 불가 |
| type | LookupType | ALL | - |
| count | Int | 25 | 1-100 |

| 반환 | 설명 |
|-----|------|
| LookupResult | 검색 결과 목록 |

### 3.4 필드 매핑

| Yahoo 필드 | Domain 필드 | 변환 | 비고 |
|-----------|------------|------|------|
| symbol | symbol | 필수 검증 | |
| shortName (또는 name) | name | 필수 검증 | shortName 우선, 없으면 name 사용 |
| exchange (또는 exch) | exchange | 그대로 | exchange 우선, 없으면 exch 사용 |
| exchDisp | exchangeDisplay | 그대로 | |
| quoteType (또는 type) | typeCode | 그대로 | quoteType 우선, 없으면 type 사용 |
| typeDisp | typeDisplay | 그대로 | |
| industry | industry | 그대로 | |
| industryName (또는 industryDisp) | industryDisplay | 그대로 | industryName 우선, 없으면 industryDisp 사용 |
| sector | sector | 그대로 | |
| sectorDisp | sectorDisplay | 그대로 | |
| rank (또는 score) | score | Int를 Double로 변환 | rank 우선 (Int→Double), 없으면 score (Double) 사용 |
| isYahooFinance | isYahooFinance | Boolean? | |

**매핑 로직**:
- 구현은 새 API 필드를 우선적으로 사용하고, 없는 경우 기존 필드로 폴백합니다
- `symbol`과 `name` (또는 `shortName`)은 필수 필드이며, 둘 중 하나라도 누락되면 해당 문서는 결과에서 제외됩니다

---

## 4. 예외 처리

### 4.1 ErrorCode 매핑

| 에러 케이스 | ErrorCode | 설명 |
|-----------|-----------|------|
| query 빈 문자열 | INVALID_PARAMETER | 검색어 검증 실패 |
| count 범위 초과 | INVALID_PARAMETER | 1-100 검증 실패 |
| symbol/name 누락 | DATA_PARSING_ERROR | 필수 필드 검증 실패 |
| HTTP 4xx/5xx | EXTERNAL_API_ERROR | API 오류 |
| Rate Limit (429) | RATE_LIMITED | 요청 제한 |
| JSON 파싱 실패 | DATA_PARSING_ERROR | 역직렬화 실패 |
| CRUMB 인증 실패 | AUTHENTICATION_FAILED | 인증 오류 |

### 4.2 빈 결과 처리

| 상황 | 처리 |
|-----|------|
| documents = [] | 빈 LookupResult 반환 (예외 아님) |
| 존재하지 않는 검색어 | count=0, total=0 반환 |
| 특수문자만 검색 | 빈 결과 또는 에러 |

### 4.3 필수 필드 검증

| 필드 | 누락 시 처리 |
|-----|-----------|
| symbol | DATA_PARSING_ERROR 발생 |
| name | DATA_PARSING_ERROR 발생 |
| 기타 모든 필드 | null 허용 |

### 4.4 재시도 정책

| 에러 | 재시도 | 횟수 |
|-----|-------|-----|
| Rate Limit (429) | Yes | Rate Limiter 처리 |
| Network Error | Yes | 3회 |
| HTTP 5xx | Yes | 3회 |
| HTTP 4xx | No | - |
| CRUMB 인증 실패 | Yes | 1회 (재획득) |

---

## 5. 참고 자료

### 5.1 API 제약사항

| 제약 | 설명 |
|-----|------|
| 비공식 API | 문서화되지 않음, 변경 가능 |
| 최대 100개 | count 상한 |
| CRUMB 필수 | 인증 토큰 필요 |
| 페이징 제한 | start 파라미터 지원하나 미구현 |

### 5.2 파라미터 검증 규칙

| 파라미터 | 검증 규칙 |
|---------|---------|
| query | trim 후 빈 문자열 불가 |
| count | 1 ≤ count ≤ 100 |
| type | LookupType Enum 값만 허용 |

### 5.3 사용 사례

| 사례 | 설명 |
|-----|------|
| 자동완성 | 사용자 입력에 따라 실시간 제안 |
| 심볼 검증 | 입력 심볼의 유효성 확인 |
| 타입별 검색 | 주식만, ETF만 선택 조회 |
| 크로스 마켓 | 다양한 거래소 동시 검색 |

### 5.4 제한 사항

| 제한 | 설명 |
|-----|------|
| sector/industry | 주식에만 존재, 기타 타입은 null |
| 검색 품질 | Yahoo 서버 알고리즘에 의존 |
| 국가별 차이 | 언어/지역별 결과 상이 가능 |
| ISIN 미제공 | 별도 API 사용 필요 |
