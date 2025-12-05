# Search API 기술 명세서

> **Version**: 2.0
> **작성일**: 2025-12-05

---

## 1. 개요

### 1.1 API 목적

Yahoo Finance Search API를 통해 **심볼, 회사명, 뉴스를 검색**한다.

| 항목 | 값 |
|-----|---|
| 엔드포인트 | `GET https://query1.finance.yahoo.com/v1/finance/search` |
| HTTP 메서드 | GET |
| 인증 | CRUMB 토큰 필요 |

### 1.2 제공 데이터

| 데이터 | 설명 |
|-------|------|
| 종목 검색 결과 | 주식, ETF, 뮤추얼펀드, 암호화폐 등 |
| 뉴스 검색 결과 | 관련 금융 뉴스 기사 |
| 검색 관련도 점수 | 검색어와의 관련성 수치 |
| 섹터/산업 정보 | 주식 종목의 분류 정보 |

### 1.3 주요 사용 사례

| 사용 사례 | 설명 |
|---------|------|
| 심볼 검색 | "AAPL" 입력 시 Apple Inc. 관련 정보 조회 |
| 회사명 검색 | "Apple" 입력 시 관련 종목 목록 반환 |
| 퍼지 검색 | "Appel" (오타) 입력 시 "Apple" 관련 결과 반환 |
| 뉴스 검색 | 특정 검색어와 관련된 최신 금융 뉴스 조회 |
| 심볼 자동완성 | 사용자 입력에 따른 실시간 종목 제안 |
| 다국가 종목 검색 | "Samsung" 검색 시 한국, 미국 등 여러 국가의 삼성 관련 종목 반환 |

---

## 2. 데이터 소스 분석

### 2.1 요청 파라미터

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| q | String | Yes | - | 검색 쿼리 (심볼, 회사명, 키워드) |
| quotesCount | Int | No | 8 | 반환할 최대 종목 개수 |
| newsCount | Int | No | 8 | 반환할 최대 뉴스 개수 |
| enableFuzzyQuery | Boolean | No | false | 퍼지 검색 활성화 (오타 보정) |
| quotesQueryId | String | No | "tss_match_phrase_query" | 종목 검색 쿼리 ID |
| newsQueryId | String | No | "news_cie_vespa" | 뉴스 검색 쿼리 ID |

### 2.2 응답 구조 - quotes 섹션

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| symbol | String | No | 티커 심볼 |
| shortname | String | Yes | 짧은 이름 |
| longname | String | Yes | 전체 이름 |
| quoteType | String | No | 자산 유형 (EQUITY, ETF, MUTUALFUND 등) |
| exchange | String | Yes | 거래소 코드 (NMS, NYQ 등) |
| exchDisp | String | Yes | 거래소 표시명 |
| sector | String | Yes | 섹터 (주식만 해당) |
| industry | String | Yes | 산업 (주식만 해당) |
| score | Double | No | 검색 관련도 점수 |
| typeDisp | String | Yes | 자산 유형 표시명 |

### 2.3 응답 구조 - news 섹션

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| uuid | String | No | 뉴스 고유 ID |
| title | String | No | 뉴스 제목 |
| publisher | String | Yes | 발행사 |
| link | String | No | 뉴스 링크 |
| providerPublishTime | Long | No | Unix 타임스탬프 (초 단위) |
| type | String | Yes | 뉴스 타입 (STORY, VIDEO 등) |
| thumbnail | Object | Yes | 썸네일 이미지 |
| relatedTickers | Array | No | 관련 티커들 |

### 2.4 응답 구조 - thumbnail 섹션

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| resolutions | Array | No | 이미지 해상도 목록 |

### 2.5 응답 구조 - resolution 섹션

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| url | String | No | 이미지 URL |
| width | Int | No | 너비 |
| height | Int | No | 높이 |
| tag | String | No | 태그 (예: "140x140") |

### 2.6 정렬 규칙

| 섹션 | 정렬 기준 | 순서 |
|-----|---------|------|
| quotes | score | 내림차순 |
| news | providerPublishTime | 내림차순 (최신순) |

---

## 3. 데이터/모델 설계

### 3.1 Domain 모델

#### SearchResponse

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| query | String | No | 검색어 |
| count | Int | No | 총 결과 개수 |
| quotes | List&lt;SearchQuote&gt; | No | 종목 검색 결과 |
| news | List&lt;SearchNews&gt; | No | 뉴스 검색 결과 |

#### SearchQuote

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| symbol | String | No | 티커 심볼 |
| shortName | String | Yes | 짧은 이름 |
| longName | String | Yes | 전체 이름 |
| quoteType | String | No | 자산 유형 |
| exchange | String | Yes | 거래소 코드 |
| exchangeDisplay | String | Yes | 거래소 표시명 |
| sector | String | Yes | 섹터 |
| industry | String | Yes | 산업 |
| score | Double | No | 검색 관련도 점수 |

#### SearchNews

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| uuid | String | No | 뉴스 고유 ID |
| title | String | No | 뉴스 제목 |
| publisher | String | Yes | 발행사 |
| link | String | No | 뉴스 링크 |
| publishTime | Long | No | Unix 타임스탬프 (초 단위) |
| type | String | Yes | 뉴스 타입 |
| thumbnail | NewsThumbnail | Yes | 썸네일 이미지 |
| relatedTickers | List&lt;String&gt; | No | 관련 티커들 |

#### NewsThumbnail

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| resolutions | List&lt;ThumbnailResolution&gt; | No | 이미지 해상도별 URL |

#### ThumbnailResolution

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| url | String | No | 이미지 URL |
| width | Int | No | 너비 |
| height | Int | No | 높이 |
| tag | String | No | 태그 |

### 3.2 Internal Response 모델

#### 구조

| 클래스 | 필드 | 타입 |
|-------|-----|------|
| SearchApiResponse | count | Int? |
| | quotes | List&lt;QuoteResult&gt;? |
| | news | List&lt;NewsResult&gt;? |
| QuoteResult | symbol | String? |
| | shortname | String? |
| | longname | String? |
| | quoteType | String? |
| | exchange | String? |
| | exchDisp | String? |
| | sector | String? |
| | industry | String? |
| | score | Double? |
| NewsResult | uuid | String? |
| | title | String? |
| | publisher | String? |
| | link | String? |
| | providerPublishTime | Long? |
| | type | String? |
| | thumbnail | NewsThumbnailResult? |
| | relatedTickers | List&lt;String&gt;? |
| NewsThumbnailResult | resolutions | List&lt;ThumbnailResolutionResult&gt;? |
| ThumbnailResolutionResult | url | String? |
| | width | Int? |
| | height | Int? |
| | tag | String? |

### 3.3 API 메서드 시그니처

```kotlin
suspend fun search(
    query: String,
    quotesCount: Int = 8,
    newsCount: Int = 8,
    enableFuzzyQuery: Boolean = false
): SearchResponse
```

| 파라미터 | 타입 | 기본값 | 제약 |
|---------|------|-------|------|
| query | String | - | 필수, 1-500자 |
| quotesCount | Int | 8 | 0 이상 |
| newsCount | Int | 8 | 0 이상 |
| enableFuzzyQuery | Boolean | false | - |

| 반환 | 설명 |
|-----|------|
| SearchResponse | 검색 결과 (종목 + 뉴스) |

### 3.4 필드 매핑

#### SearchQuote 매핑

| Yahoo 필드 | Domain 필드 | 변환 |
|-----------|------------|------|
| symbol | symbol | 그대로 |
| shortname | shortName | 빈 문자열 → null |
| longname | longName | 빈 문자열 → null |
| quoteType | quoteType | 그대로 |
| exchange | exchange | 빈 문자열 → null |
| exchDisp | exchangeDisplay | 빈 문자열 → null |
| sector | sector | 빈 문자열 → null |
| industry | industry | 빈 문자열 → null |
| score | score | null → 0.0 |

#### SearchNews 매핑

| Yahoo 필드 | Domain 필드 | 변환 |
|-----------|------------|------|
| uuid | uuid | 그대로 |
| title | title | 그대로 |
| publisher | publisher | 빈 문자열 → null |
| link | link | 그대로 |
| providerPublishTime | publishTime | null → 0 |
| type | type | 빈 문자열 → null |
| thumbnail | thumbnail | 그대로 |
| relatedTickers | relatedTickers | null → emptyList() |

### 3.5 필터링 규칙

| 데이터 타입 | 필수 필드 | 처리 |
|----------|---------|------|
| SearchQuote | symbol, quoteType | 누락 시 항목 제외 |
| SearchNews | uuid, title, link | 누락 시 항목 제외 |

---

## 4. 예외 처리

### 4.1 ErrorCode 매핑

| 에러 케이스 | ErrorCode | 설명 |
|-----------|-----------|------|
| query 빈 문자열 | INVALID_PARAMETER | 검색어 필수 |
| query 길이 > 500 | INVALID_PARAMETER | 검색어 길이 제한 |
| quotesCount < 0 | INVALID_PARAMETER | 음수 불가 |
| newsCount < 0 | INVALID_PARAMETER | 음수 불가 |
| HTTP 4xx | EXTERNAL_API_ERROR | API 요청 오류 |
| HTTP 5xx | SERVICE_UNAVAILABLE | 서버 오류 |
| Rate Limit (429) | RATE_LIMITED | 요청 제한 |
| JSON 파싱 실패 | DATA_PARSING_ERROR | 역직렬화 실패 |
| CRUMB 획득 실패 | EXTERNAL_API_ERROR | 인증 실패 |

### 4.2 빈 결과 처리

| 상황 | 처리 |
|-----|------|
| quotes = [] | 빈 SearchResponse 반환 (예외 아님) |
| news = [] | 빈 SearchResponse 반환 (예외 아님) |
| 존재하지 않는 검색어 | 빈 SearchResponse 반환 |

### 4.3 재시도 정책

| 에러 | 재시도 | 횟수 |
|-----|-------|-----|
| Rate Limit (429) | Yes | Rate Limiter 처리 |
| Network Error | Yes | 3회 |
| HTTP 5xx | Yes | 3회 |
| HTTP 4xx | No | - |
| Validation Error | No | - |

---

## 5. 참고 자료

### 5.1 API 제약사항

| 제약 | 설명 |
|-----|------|
| 비공식 API | 문서화되지 않음, 변경 가능 |
| 검색어 길이 | 경험적으로 500자 제한 권장 |
| CRUMB 토큰 | 요청마다 필요 |
| 퍼지 검색 성능 | enableFuzzyQuery 활성화 시 응답 속도 저하 가능 |

### 5.2 특수 케이스

| 케이스 | 설명 |
|-------|------|
| 정확한 심볼 검색 | "AAPL" 검색 시 첫 번째 결과로 반환, score 매우 높음 |
| 회사명 검색 | "Apple" 검색 시 여러 종목 반환 (AAPL, AAPL.MX 등) |
| 다국가 종목 | "Samsung" 검색 시 005930.KS, SSNLF 등 반환 |
| 특수 문자 | "!@#$%" 등 특수 문자는 빈 결과 또는 매우 적은 결과 |

### 5.3 검색어 예시

| 유형 | 예시 |
|-----|------|
| 정확한 심볼 | AAPL, MSFT, GOOGL, TSLA, SPY, QQQ |
| 회사명 | Apple, Microsoft, Google, Tesla, Samsung |
| 다국가 종목 | Samsung (→ 005930.KS, SSNLF), Toyota (→ 7203.T, TM) |
| 오타 (퍼지 검색) | Appel (→ Apple), Microsft (→ Microsoft) |
| 산업/섹터 | Tech, Energy, Finance |
