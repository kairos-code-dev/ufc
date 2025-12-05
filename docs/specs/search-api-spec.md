# Search API 기능 명세서

> **Version**: 1.0
> **작성일**: 2025-12-05
> **대상**: UFC 프로젝트에 Yahoo Finance `/v1/finance/search` API 통합

---

## 목차

- [1. API 개요](#1-api-개요)
- [2. Yahoo Finance Search API 분석](#2-yahoo-finance-search-api-분석)
- [3. UFC 통합 설계](#3-ufc-통합-설계)
- [4. 데이터 매핑](#4-데이터-매핑)
- [5. 에러 처리](#5-에러-처리)
- [6. 테스트 전략](#6-테스트-전략)
- [7. 구현 우선순위](#7-구현-우선순위)

---

## 1. API 개요

### 1.1 Search API의 목적

Search API는 Yahoo Finance의 `/v1/finance/search` 엔드포인트를 통해 **심볼, 회사명, 뉴스를 검색**하는 기능을 제공합니다. 사용자가 입력한 검색어를 기반으로 관련 금융 상품(주식, ETF, 뮤추얼펀드 등)과 뉴스 기사를 찾을 수 있습니다.

### 1.2 주요 사용 사례

1. **심볼 검색**: "AAPL" 입력 시 Apple Inc. 관련 정보 조회
2. **회사명 검색**: "Apple" 입력 시 Apple Inc.를 포함한 관련 종목 목록 반환
3. **퍼지 검색**: "Appel" (오타) 입력 시 "Apple" 관련 결과 반환 (enableFuzzyQuery 활성화)
4. **뉴스 검색**: 특정 검색어와 관련된 최신 금융 뉴스 조회
5. **심볼 자동완성**: 사용자 입력에 따른 실시간 종목 제안
6. **다국가 종목 검색**: "Samsung" 검색 시 한국, 미국 등 여러 국가의 삼성 관련 종목 반환

### 1.3 기존 API와의 차이점

| API | 목적 | 입력 | 출력 |
|-----|------|------|------|
| **Search API** | 심볼/회사명 검색 | 검색어 (자유 텍스트) | 관련 종목 목록 + 뉴스 |
| **QuoteSummary API** | 상세 정보 조회 | 심볼 (정확한 티커) | 특정 종목의 상세 모듈 |
| **Chart API** | 가격 히스토리 | 심볼 (정확한 티커) | OHLCV 데이터 |

**Search API의 역할**: 사용자가 정확한 심볼을 모를 때, 검색어를 통해 심볼을 찾고, 이를 QuoteSummary나 Chart API에 전달하는 **중간 단계** 역할을 수행합니다.

### 1.4 Search API의 장점

- **유연한 검색**: 심볼, 회사명, ISIN 등 다양한 검색 키워드 지원
- **통합 검색**: 주식, ETF, 뮤추얼펀드, 인덱스, 암호화폐 등 모든 자산 유형 검색 가능
- **뉴스 통합**: 종목 정보와 함께 관련 뉴스를 한 번에 조회
- **퍼지 검색**: 오타가 있어도 유사한 결과 반환 (옵션)
- **다국어/다국가**: 전 세계 금융 상품 검색 가능

---

## 2. Yahoo Finance Search API 분석

### 2.1 API 엔드포인트

```
GET https://query1.finance.yahoo.com/v1/finance/search
```

**베이스 URL**: `YahooApiUrls.QUERY1` (`https://query1.finance.yahoo.com`)
**전체 URL**: `YahooApiUrls.SEARCH` (`https://query1.finance.yahoo.com/v1/finance/search`)

### 2.2 요청 파라미터

| 파라미터 | 타입 | 필수 | 기본값 | 설명 | 예시 |
|---------|------|------|--------|------|------|
| `q` | String | 필수 | - | 검색 쿼리 (심볼, 회사명, 키워드) | `"AAPL"`, `"Apple"`, `"Samsung"` |
| `quotesCount` | Int | 선택 | 8 | 반환할 최대 종목 개수 | `10`, `20` |
| `newsCount` | Int | 선택 | 8 | 반환할 최대 뉴스 개수 | `5`, `10` |
| `enableFuzzyQuery` | Boolean | 선택 | false | 퍼지 검색 활성화 (오타 보정) | `true`, `false` |
| `quotesQueryId` | String | 선택 | `"tss_match_phrase_query"` | 종목 검색 쿼리 ID | 고정값 사용 권장 |
| `newsQueryId` | String | 선택 | `"news_cie_vespa"` | 뉴스 검색 쿼리 ID | 고정값 사용 권장 |
| `listsCount` | Int | 선택 | 8 | 반환할 최대 리스트 개수 | `5`, `10` |
| `enableCb` | Boolean | 선택 | true | Company Breakdown 포함 여부 | `true`, `false` |
| `enableNavLinks` | Boolean | 선택 | false | Navigation Links 포함 여부 | `true`, `false` |
| `enableResearchReports` | Boolean | 선택 | false | 리서치 리포트 포함 여부 | `true`, `false` |
| `enableCulturalAssets` | Boolean | 선택 | false | 문화 자산 포함 여부 | `true`, `false` |
| `recommendedCount` | Int | 선택 | 8 | 추천 항목 개수 | `5`, `10` |

**파라미터 설명**:
- `q`: 필수 파라미터. 검색어가 비어있으면 에러 발생
- `quotesCount`: 0으로 설정하면 종목 정보를 반환하지 않음 (뉴스만 조회 시 유용)
- `newsCount`: 0으로 설정하면 뉴스를 반환하지 않음 (종목만 조회 시 유용)
- `enableFuzzyQuery`: 오타 보정이 필요한 경우 활성화 (응답 속도가 약간 느려질 수 있음)
- `listsCount`, `enableCb`, `enableNavLinks` 등: yfinance는 기본값 사용, UFC도 동일하게 처리 권장

### 2.3 응답 JSON 구조

#### 2.3.1 전체 응답 구조

```json
{
  "explains": [],
  "count": 15,
  "quotes": [
    {
      "exchange": "NMS",
      "shortname": "Apple Inc.",
      "quoteType": "EQUITY",
      "symbol": "AAPL",
      "index": "quotes",
      "score": 3876671.0,
      "typeDisp": "Equity",
      "longname": "Apple Inc.",
      "exchDisp": "NASDAQ",
      "sector": "Technology",
      "industry": "Consumer Electronics",
      "isYahooFinance": true
    }
  ],
  "news": [
    {
      "uuid": "c4d5a8b1-...",
      "title": "Apple Reports Strong Q4 Earnings",
      "publisher": "CNBC",
      "link": "https://...",
      "providerPublishTime": 1701234567,
      "type": "STORY",
      "thumbnail": {
        "resolutions": [
          {
            "url": "https://...",
            "width": 140,
            "height": 140,
            "tag": "140x140"
          }
        ]
      },
      "relatedTickers": ["AAPL"]
    }
  ],
  "nav": [],
  "lists": [],
  "researchReports": [],
  "screenerFieldResults": [],
  "totalTime": 45,
  "timeTakenForQuotes": 23,
  "timeTakenForNews": 19,
  "timeTakenForAlgowatchlist": 0,
  "timeTakenForPredefinedScreener": 0,
  "timeTakenForCrunchbase": 0,
  "timeTakenForNav": 0,
  "timeTakenForResearchReports": 0,
  "timeTakenForScreenerField": 0,
  "timeTakenForCulturalAssets": 0
}
```

#### 2.3.2 주요 섹션별 구조

**quotes 섹션** (종목 정보):
```json
{
  "exchange": "NMS",           // 거래소 코드 (예: NMS=NASDAQ, NYQ=NYSE)
  "shortname": "Apple Inc.",   // 짧은 이름
  "quoteType": "EQUITY",       // 자산 유형 (EQUITY, ETF, MUTUALFUND, INDEX, CRYPTOCURRENCY, CURRENCY 등)
  "symbol": "AAPL",            // 티커 심볼
  "index": "quotes",           // 고정값
  "score": 3876671.0,          // 검색 관련도 점수 (높을수록 관련성 높음)
  "typeDisp": "Equity",        // 자산 유형 표시명
  "longname": "Apple Inc.",    // 전체 이름
  "exchDisp": "NASDAQ",        // 거래소 표시명
  "sector": "Technology",      // 섹터 (주식만 해당, nullable)
  "industry": "Consumer Electronics",  // 산업 (주식만 해당, nullable)
  "isYahooFinance": true       // Yahoo Finance에서 제공 여부
}
```

**news 섹션** (뉴스 정보):
```json
{
  "uuid": "c4d5a8b1-...",                  // 뉴스 고유 ID
  "title": "Apple Reports Strong Q4...",  // 뉴스 제목
  "publisher": "CNBC",                     // 발행사
  "link": "https://...",                   // 뉴스 링크
  "providerPublishTime": 1701234567,       // Unix 타임스탬프 (초 단위)
  "type": "STORY",                         // 뉴스 타입 (STORY, VIDEO 등)
  "thumbnail": {                           // 썸네일 이미지 (nullable)
    "resolutions": [
      {
        "url": "https://...",
        "width": 140,
        "height": 140,
        "tag": "140x140"
      }
    ]
  },
  "relatedTickers": ["AAPL"]               // 관련 티커들
}
```

**lists 섹션**: 특정 종목 리스트 (예: "Top Gainers", "Most Active")
- yfinance 참조: 대부분 비어있음, UFC에서는 지원하지 않아도 무방

**nav 섹션**: 네비게이션 링크
- yfinance 참조: `enableNavLinks=true`일 때만 반환, 일반적으로 비어있음

**researchReports 섹션**: 리서치 리포트
- yfinance 참조: `enableResearchReports=true`일 때만 반환, 일반적으로 비어있음

#### 2.3.3 에러 응답 구조

**에러 응답 예시**:
```json
{
  "finance": {
    "result": null,
    "error": {
      "code": "Bad Request",
      "description": "Missing query parameter 'q'"
    }
  }
}
```

또는:

```json
{
  "explains": [],
  "count": 0,
  "quotes": [],
  "news": [],
  "nav": [],
  "lists": []
}
```

**참고**: Yahoo Finance Search API는 검색 결과가 없어도 HTTP 200을 반환하며, `quotes`와 `news`가 빈 배열로 반환됩니다.

### 2.4 검색 결과 정렬 규칙

- **score 필드**: 검색 관련도 점수. 높을수록 검색어와 관련성이 높음
- **quotes 배열**: score 내림차순으로 자동 정렬됨
- **news 배열**: providerPublishTime 내림차순 (최신순)

### 2.5 특수 케이스

#### 2.5.1 심볼이 정확히 일치하는 경우
검색어가 정확한 심볼(예: "AAPL")인 경우, 해당 심볼이 첫 번째 결과로 반환되며 score가 매우 높음.

#### 2.5.2 회사명으로 검색하는 경우
검색어가 회사명(예: "Apple")인 경우, 관련 종목 여러 개가 반환될 수 있음:
- AAPL (Apple Inc.)
- AAPL.MX (Apple Inc. - Mexico)
- AAPL34.SA (Apple Inc. - Brazil)

#### 2.5.3 다국가 종목
"Samsung" 검색 시:
- 005930.KS (삼성전자 - 한국)
- SSNLF (Samsung Electronics - OTC US)
- 005930.KQ (삼성전자우 - 한국)

#### 2.5.4 빈 검색 결과
검색어가 존재하지 않는 경우:
```json
{
  "count": 0,
  "quotes": [],
  "news": []
}
```

---

## 3. UFC 통합 설계

### 3.1 네임스페이스 배치

Search API는 **새로운 도메인**이므로 `ufc.yahoo` 패키지 내에 search 관련 모델과 기능을 추가합니다.

**패키지 구조**:
```
com.ulalax.ufc.yahoo/
├── YahooClient.kt                          (기존)
├── YahooClientConfig.kt                    (기존)
├── model/
│   ├── SearchRequest.kt                    (신규)
│   ├── SearchResponse.kt                   (신규)
│   └── ...기존 모델들
└── internal/
    ├── YahooApiUrls.kt                     (기존 - SEARCH 상수 이미 존재)
    └── response/
        └── SearchApiResponse.kt            (신규 - internal API 응답)
```

**설계 원칙**:
1. Search API는 Yahoo Finance의 하위 기능이므로 `YahooClient`에 메서드 추가
2. Public API 모델은 `com.ulalax.ufc.yahoo.model` 패키지에 배치
3. Internal API 응답 모델은 `com.ulalax.ufc.yahoo.internal.response` 패키지에 배치
4. 기존 QuoteSummary, Chart API와 동일한 설계 패턴 유지

### 3.2 YahooClient에 추가할 메서드

**메서드 시그니처**:

```kotlin
suspend fun YahooClient.search(
    query: String,
    quotesCount: Int = 8,
    newsCount: Int = 8,
    enableFuzzyQuery: Boolean = false
): SearchResponse
```

**설명**:
- `query`: 검색어 (필수)
- `quotesCount`: 반환할 종목 최대 개수 (기본값: 8)
- `newsCount`: 반환할 뉴스 최대 개수 (기본값: 8)
- `enableFuzzyQuery`: 퍼지 검색 활성화 여부 (기본값: false)
- 반환값: `SearchResponse` (quotes, news 포함)

**참고**: yfinance의 모든 파라미터를 노출하지 않고, 주요 파라미터만 노출합니다. 나머지 파라미터(listsCount, enableCb 등)는 내부적으로 기본값 사용.

### 3.3 Ufc 클래스에 추가할 편의 메서드

`Ufc.kt`에 다음 편의 메서드 추가:

```kotlin
suspend fun Ufc.search(
    query: String,
    quotesCount: Int = 8,
    newsCount: Int = 8,
    enableFuzzyQuery: Boolean = false
): SearchResponse = yahoo.search(query, quotesCount, newsCount, enableFuzzyQuery)
```

**사용 예시**:
```kotlin
val ufc = Ufc.create()
val result = ufc.search("Apple", quotesCount = 10, newsCount = 5)
```

### 3.4 필요한 모델 클래스 목록

#### 3.4.1 Public API 모델 (com.ulalax.ufc.yahoo.model)

**1. SearchResponse**
- 역할: Search API의 최종 응답 객체
- 주요 필드:
  - `query: String` - 검색어
  - `count: Int` - 총 결과 개수
  - `quotes: List<SearchQuote>` - 종목 검색 결과
  - `news: List<SearchNews>` - 뉴스 검색 결과

**2. SearchQuote**
- 역할: 종목 검색 결과 개별 항목
- 주요 필드:
  - `symbol: String` - 티커 심볼
  - `shortName: String?` - 짧은 이름
  - `longName: String?` - 전체 이름
  - `quoteType: String` - 자산 유형 (EQUITY, ETF 등)
  - `exchange: String?` - 거래소 코드
  - `exchangeDisplay: String?` - 거래소 표시명
  - `sector: String?` - 섹터 (주식만 해당)
  - `industry: String?` - 산업 (주식만 해당)
  - `score: Double` - 검색 관련도 점수

**3. SearchNews**
- 역할: 뉴스 검색 결과 개별 항목
- 주요 필드:
  - `uuid: String` - 뉴스 고유 ID
  - `title: String` - 뉴스 제목
  - `publisher: String?` - 발행사
  - `link: String` - 뉴스 링크
  - `publishTime: Long` - Unix 타임스탬프 (초 단위)
  - `type: String?` - 뉴스 타입 (STORY, VIDEO 등)
  - `thumbnail: NewsThumbnail?` - 썸네일 이미지
  - `relatedTickers: List<String>` - 관련 티커들

**4. NewsThumbnail**
- 역할: 뉴스 썸네일 이미지 정보
- 주요 필드:
  - `resolutions: List<ThumbnailResolution>` - 이미지 해상도별 URL

**5. ThumbnailResolution**
- 역할: 썸네일 이미지 해상도별 정보
- 주요 필드:
  - `url: String` - 이미지 URL
  - `width: Int` - 너비
  - `height: Int` - 높이
  - `tag: String` - 태그 (예: "140x140")

#### 3.4.2 Internal API 응답 모델 (com.ulalax.ufc.yahoo.internal.response)

**1. SearchApiResponse**
- 역할: Yahoo Finance API 원본 응답 (JSON 직렬화용)
- 주요 필드:
  - `explains: List<Any>?`
  - `count: Int?`
  - `quotes: List<QuoteResult>?`
  - `news: List<NewsResult>?`
  - `nav: List<Any>?`
  - `lists: List<Any>?`
  - `researchReports: List<Any>?`

**2. QuoteResult**
- 역할: Yahoo Finance API 원본 종목 응답
- 주요 필드: SearchQuote와 동일하나 JSON 필드명 매핑용 (예: `exchDisp`, `typeDisp`)

**3. NewsResult**
- 역할: Yahoo Finance API 원본 뉴스 응답
- 주요 필드: SearchNews와 동일하나 JSON 필드명 매핑용 (예: `providerPublishTime`)

**4. NewsThumbnailResult**
- 역할: Yahoo Finance API 원본 썸네일 응답

**5. ThumbnailResolutionResult**
- 역할: Yahoo Finance API 원본 썸네일 해상도 응답

### 3.5 변환 함수

YahooClient 내부에 다음 private 함수 추가:

```kotlin
private fun convertToSearchResponse(
    query: String,
    apiResponse: SearchApiResponse
): SearchResponse
```

역할: Internal API 응답을 Public Domain 모델로 변환

### 3.6 Rate Limiting 및 인증

- **Rate Limiter**: 기존 `YahooClient`의 `rateLimiter` 재사용
- **CRUMB 토큰**: 기존 `authenticator.getCrumb()` 사용
- **HTTP 클라이언트**: 기존 `httpClient` 재사용

### 3.7 캐싱 전략

**캐싱 미적용 권장**:
- Search API는 사용자 입력에 따라 결과가 매번 다를 수 있음
- 검색어가 동일하더라도 뉴스는 실시간으로 변경됨
- 캐싱하지 않고 항상 실시간 조회하는 것을 권장

**대안 (선택적)**:
- 동일한 검색어에 대해 짧은 TTL(예: 10초) 캐싱 적용 가능
- 자동완성 등 반복적인 검색어에 유용

---

## 4. 데이터 매핑

### 4.1 Yahoo API 응답 → UFC 도메인 모델 매핑

#### 4.1.1 SearchQuote 매핑

| Yahoo API 필드 | UFC 모델 필드 | 타입 | Nullable | 설명 |
|---------------|--------------|------|----------|------|
| `symbol` | `symbol` | String | No | 티커 심볼 |
| `shortname` | `shortName` | String? | Yes | 짧은 이름 |
| `longname` | `longName` | String? | Yes | 전체 이름 |
| `quoteType` | `quoteType` | String | No | 자산 유형 |
| `exchange` | `exchange` | String? | Yes | 거래소 코드 |
| `exchDisp` | `exchangeDisplay` | String? | Yes | 거래소 표시명 |
| `sector` | `sector` | String? | Yes | 섹터 |
| `industry` | `industry` | String? | Yes | 산업 |
| `score` | `score` | Double | No | 검색 관련도 점수 |
| `typeDisp` | (무시) | - | - | 표시용 타입 (quoteType으로 충분) |
| `index` | (무시) | - | - | 항상 "quotes" 고정값 |
| `isYahooFinance` | (무시) | - | - | 내부 플래그 |

**참고**:
- `symbol`과 `quoteType`은 필수 필드로 간주 (없으면 해당 항목 스킵)
- `score` 기본값: 0.0 (없을 경우)

#### 4.1.2 SearchNews 매핑

| Yahoo API 필드 | UFC 모델 필드 | 타입 | Nullable | 설명 |
|---------------|--------------|------|----------|------|
| `uuid` | `uuid` | String | No | 뉴스 고유 ID |
| `title` | `title` | String | No | 뉴스 제목 |
| `publisher` | `publisher` | String? | Yes | 발행사 |
| `link` | `link` | String | No | 뉴스 링크 |
| `providerPublishTime` | `publishTime` | Long | No | Unix 타임스탬프 (초) |
| `type` | `type` | String? | Yes | 뉴스 타입 |
| `thumbnail` | `thumbnail` | NewsThumbnail? | Yes | 썸네일 이미지 |
| `relatedTickers` | `relatedTickers` | List<String> | No | 관련 티커들 (빈 리스트 가능) |

**참고**:
- `uuid`, `title`, `link`는 필수 필드로 간주 (없으면 해당 항목 스킵)
- `relatedTickers`가 null이면 빈 리스트로 변환
- `publishTime` 기본값: 0 (없을 경우)

#### 4.1.3 NewsThumbnail 매핑

| Yahoo API 필드 | UFC 모델 필드 | 타입 | Nullable | 설명 |
|---------------|--------------|------|----------|------|
| `resolutions` | `resolutions` | List<ThumbnailResolution> | No | 이미지 해상도 목록 |

#### 4.1.4 ThumbnailResolution 매핑

| Yahoo API 필드 | UFC 모델 필드 | 타입 | Nullable | 설명 |
|---------------|--------------|------|----------|------|
| `url` | `url` | String | No | 이미지 URL |
| `width` | `width` | Int | No | 너비 |
| `height` | `height` | Int | No | 높이 |
| `tag` | `tag` | String | No | 태그 |

### 4.2 타입 변환 규칙

#### 4.2.1 문자열 타입
- Yahoo API: 문자열 필드가 null 또는 빈 문자열일 수 있음
- UFC 모델: nullable String으로 처리, 빈 문자열은 null로 변환

#### 4.2.2 숫자 타입
- `score`: Double 타입, 기본값 0.0
- `publishTime`: Long 타입 (Unix 타임스탬프 - 초 단위), 기본값 0

#### 4.2.3 배열 타입
- `quotes`, `news`: null일 경우 빈 리스트로 변환
- `relatedTickers`: null일 경우 빈 리스트로 변환

#### 4.2.4 중첩 객체
- `thumbnail`: nullable로 처리
- `resolutions`: null일 경우 빈 리스트로 변환

### 4.3 Nullable 처리 전략

**원칙**:
1. **필수 필드**: 없으면 해당 항목을 결과에서 제외
   - 예: `symbol`, `quoteType` 없는 종목은 스킵
   - 예: `uuid`, `title`, `link` 없는 뉴스는 스킵

2. **선택 필드**: nullable로 선언하고 null 허용
   - 예: `sector`, `industry`, `publisher`

3. **배열 필드**: null일 경우 빈 리스트로 변환
   - 예: `quotes`, `news`, `relatedTickers`

**변환 로직**:
```
// 유사 코드 (실제 구현 아님)
val quotes = apiResponse.quotes
    ?.filter { it.symbol != null && it.quoteType != null }
    ?.map { convertToSearchQuote(it) }
    ?: emptyList()

val news = apiResponse.news
    ?.filter { it.uuid != null && it.title != null && it.link != null }
    ?.map { convertToSearchNews(it) }
    ?: emptyList()
```

### 4.4 데이터 정규화

#### 4.4.1 문자열 트리밍
- 모든 문자열 필드는 `.trim()` 처리
- 트리밍 후 빈 문자열이면 null로 변환

#### 4.4.2 URL 검증
- `link` 필드: 유효한 URL 형식인지 검증 (선택적)
- 유효하지 않은 경우 해당 뉴스 항목 스킵

#### 4.4.3 타임스탬프 검증
- `publishTime`: 음수이거나 비정상적으로 큰 값인 경우 0으로 변환

---

## 5. 에러 처리

### 5.1 예상되는 에러 케이스

#### 5.1.1 클라이언트 에러

**1. 검색어 누락**
- **상황**: `query` 파라미터가 빈 문자열이거나 공백만 포함
- **처리**: `ValidationException` 발생
- **ErrorCode**: `MISSING_REQUIRED_PARAMETER`
- **메시지**: "검색어가 필요합니다"

**2. 잘못된 파라미터**
- **상황**: `quotesCount`, `newsCount`가 음수
- **처리**: `ValidationException` 발생
- **ErrorCode**: `INVALID_PARAMETER`
- **메시지**: "quotesCount와 newsCount는 0 이상이어야 합니다"

**3. 너무 긴 검색어**
- **상황**: `query` 길이가 500자 초과 (경험적 제한)
- **처리**: `ValidationException` 발생
- **ErrorCode**: `INVALID_PARAMETER`
- **메시지**: "검색어는 500자를 초과할 수 없습니다"

#### 5.1.2 네트워크 에러

**1. 네트워크 타임아웃**
- **상황**: 요청 타임아웃 발생
- **처리**: `ApiException` 발생
- **ErrorCode**: `NETWORK_TIMEOUT`
- **메시지**: "Search API 요청 타임아웃"

**2. 연결 실패**
- **상황**: Yahoo Finance 서버 연결 불가
- **처리**: `ApiException` 발생
- **ErrorCode**: `NETWORK_CONNECTION_ERROR`
- **메시지**: "Search API 연결 실패"

#### 5.1.3 API 응답 에러

**1. HTTP 4xx 에러**
- **상황**: Bad Request (400), Unauthorized (401) 등
- **처리**: `ApiException` 발생
- **ErrorCode**: `EXTERNAL_API_ERROR`
- **메시지**: "Search API 요청 실패: HTTP {status}"

**2. HTTP 5xx 에러**
- **상황**: Internal Server Error (500), Service Unavailable (503) 등
- **처리**: `ApiException` 발생
- **ErrorCode**: `SERVICE_UNAVAILABLE`
- **메시지**: "Search API 서버 오류: HTTP {status}"

**3. Rate Limit 초과**
- **상황**: Yahoo Finance에서 429 Too Many Requests 반환
- **처리**: `ApiException` 발생
- **ErrorCode**: `RATE_LIMITED`
- **메시지**: "Search API Rate Limit 초과"

#### 5.1.4 데이터 파싱 에러

**1. JSON 파싱 실패**
- **상황**: 응답이 유효한 JSON 형식이 아님
- **처리**: `DataParsingException` 발생
- **ErrorCode**: `JSON_PARSING_ERROR`
- **메시지**: "Search API 응답 파싱 실패"

**2. 예상치 못한 응답 구조**
- **상황**: 응답에 `quotes`, `news` 필드가 없음
- **처리**: 빈 리스트로 처리 (에러 발생하지 않음)

#### 5.1.5 검색 결과 없음

**1. 빈 검색 결과**
- **상황**: `quotes`와 `news` 모두 빈 배열
- **처리**: 에러 발생하지 않음, `SearchResponse` 반환 (빈 리스트)
- **참고**: 사용자에게 "검색 결과가 없습니다" 메시지 표시는 클라이언트 책임

#### 5.1.6 CRUMB 획득 실패

**1. CRUMB 토큰 획득 실패**
- **상황**: `authenticator.getCrumb()` 호출 실패
- **처리**: `ApiException` 발생
- **ErrorCode**: `CRUMB_ACQUISITION_FAILED`
- **메시지**: "CRUMB 토큰 획득 실패"

### 5.2 에러 응답 구조

**Yahoo Finance API 에러 응답 예시**:
```json
{
  "finance": {
    "result": null,
    "error": {
      "code": "Bad Request",
      "description": "Missing query parameter 'q'"
    }
  }
}
```

**처리 방법**:
- 응답에 `finance.error` 또는 `error` 필드가 있는지 확인
- 있으면 `ApiException` 발생
- ErrorCode: `EXTERNAL_API_ERROR`
- 메시지: `error.description` 또는 `error.code` 사용

### 5.3 에러 처리 플로우

```
1. 입력 검증
   ├─ query 빈 문자열 체크 → ValidationException
   ├─ quotesCount, newsCount 음수 체크 → ValidationException
   └─ query 길이 체크 → ValidationException

2. Rate Limiting
   └─ rateLimiter.acquire() → RateLimitException (내부 처리)

3. CRUMB 획득
   └─ authenticator.getCrumb() 실패 → ApiException (CRUMB_ACQUISITION_FAILED)

4. HTTP 요청
   ├─ 네트워크 타임아웃 → ApiException (NETWORK_TIMEOUT)
   ├─ 연결 실패 → ApiException (NETWORK_CONNECTION_ERROR)
   └─ HTTP 4xx/5xx → ApiException (EXTERNAL_API_ERROR / SERVICE_UNAVAILABLE)

5. 응답 파싱
   ├─ JSON 파싱 실패 → DataParsingException (JSON_PARSING_ERROR)
   ├─ error 필드 존재 → ApiException (EXTERNAL_API_ERROR)
   └─ 빈 결과 → 정상 처리 (빈 리스트 반환)

6. 데이터 변환
   └─ 변환 중 예외 → DataParsingException (DATA_PARSING_ERROR)
```

### 5.4 재시도 전략

**재시도 가능한 에러**:
- `NETWORK_TIMEOUT`
- `NETWORK_CONNECTION_ERROR`
- `SERVICE_UNAVAILABLE`
- `RATE_LIMITED`

**재시도 불가능한 에러**:
- `MISSING_REQUIRED_PARAMETER`
- `INVALID_PARAMETER`
- `JSON_PARSING_ERROR`
- `EXTERNAL_API_ERROR` (4xx 에러)

**참고**: 재시도 로직은 UFC 클라이언트 레벨에서 구현하지 않고, 사용자 애플리케이션에서 처리하도록 권장.

---

## 6. 테스트 전략

### 6.1 단위 테스트 시나리오

**목표**: 데이터 변환 로직 검증 (Mock 응답 사용)

#### 6.1.1 데이터 변환 테스트

**테스트 케이스**:
1. **정상 응답 변환**
   - Given: 유효한 SearchApiResponse (quotes, news 포함)
   - When: convertToSearchResponse 호출
   - Then: SearchResponse가 올바르게 변환됨

2. **빈 응답 변환**
   - Given: 빈 quotes, 빈 news
   - When: convertToSearchResponse 호출
   - Then: 빈 리스트가 포함된 SearchResponse 반환

3. **null 필드 처리**
   - Given: sector, industry가 null인 quote
   - When: convertToSearchQuote 호출
   - Then: nullable 필드가 null로 설정됨

4. **필수 필드 누락 처리**
   - Given: symbol이 null인 quote
   - When: quotes 필터링
   - Then: 해당 항목이 제외됨

#### 6.1.2 Nullable 처리 테스트

**테스트 케이스**:
1. **선택 필드가 null인 경우**
   - Given: publisher가 null인 news
   - When: convertToSearchNews 호출
   - Then: publisher가 null인 SearchNews 반환

2. **빈 문자열 처리**
   - Given: shortname이 빈 문자열인 quote
   - When: convertToSearchQuote 호출
   - Then: shortName이 null로 변환됨

3. **배열이 null인 경우**
   - Given: relatedTickers가 null인 news
   - When: convertToSearchNews 호출
   - Then: relatedTickers가 빈 리스트로 변환됨

#### 6.1.3 입력 검증 테스트

**테스트 케이스**:
1. **빈 검색어**
   - Given: query = ""
   - When: search 호출
   - Then: ValidationException 발생

2. **음수 quotesCount**
   - Given: quotesCount = -1
   - When: search 호출
   - Then: ValidationException 발생

3. **음수 newsCount**
   - Given: newsCount = -1
   - When: search 호출
   - Then: ValidationException 발생

### 6.2 통합 테스트 시나리오

**목표**: 실제 Yahoo Finance API 호출 검증

**테스트 파일**: `src/test/kotlin/com/ulalax/ufc/integration/yahoo/SearchSpec.kt`

**테스트 구조**: @Nested 그룹핑 패턴 사용 (IsinSearchSpec 참조)

#### 6.2.1 기본 동작 (BasicBehavior)

**테스트 케이스**:
1. **정확한 심볼로 검색**
   - Given: query = "AAPL"
   - When: ufc.search("AAPL") 호출
   - Then:
     - 첫 번째 결과의 symbol이 "AAPL"
     - quotes 리스트가 비어있지 않음
     - 검색 결과 기록 (ResponseRecorder)

2. **회사명으로 검색**
   - Given: query = "Apple"
   - When: ufc.search("Apple") 호출
   - Then:
     - quotes에 "AAPL" 포함
     - quotes 리스트가 비어있지 않음
     - 검색 결과 기록

3. **ETF 검색**
   - Given: query = "SPY"
   - When: ufc.search("SPY") 호출
   - Then:
     - 첫 번째 결과의 symbol이 "SPY"
     - quoteType이 "ETF"
     - 검색 결과 기록

4. **다국가 종목 검색**
   - Given: query = "Samsung"
   - When: ufc.search("Samsung") 호출
   - Then:
     - quotes에 한국 종목(005930.KS) 또는 미국 OTC 종목 포함
     - 검색 결과 기록

#### 6.2.2 파라미터 동작 (ParameterBehavior)

**테스트 케이스**:
1. **quotesCount 제한**
   - Given: query = "Apple", quotesCount = 3
   - When: ufc.search("Apple", quotesCount = 3) 호출
   - Then: quotes 개수가 3개 이하

2. **newsCount 제한**
   - Given: query = "Apple", newsCount = 2
   - When: ufc.search("Apple", newsCount = 2) 호출
   - Then: news 개수가 2개 이하

3. **종목만 조회 (뉴스 제외)**
   - Given: query = "AAPL", newsCount = 0
   - When: ufc.search("AAPL", newsCount = 0) 호출
   - Then:
     - quotes가 비어있지 않음
     - news가 비어있음

4. **뉴스만 조회 (종목 제외)**
   - Given: query = "Apple", quotesCount = 0
   - When: ufc.search("Apple", quotesCount = 0) 호출
   - Then:
     - quotes가 비어있음
     - news가 비어있지 않음

5. **퍼지 검색 활성화**
   - Given: query = "Appel" (오타), enableFuzzyQuery = true
   - When: ufc.search("Appel", enableFuzzyQuery = true) 호출
   - Then:
     - quotes에 "AAPL" 포함 (오타 보정)
     - 검색 결과 기록

#### 6.2.3 응답 데이터 스펙 (ResponseSpec)

**테스트 케이스**:
1. **SearchQuote 필드 검증**
   - Given: query = "AAPL"
   - When: ufc.search("AAPL") 호출
   - Then: 첫 번째 quote가 다음 필드 포함:
     - symbol (not null)
     - quoteType (not null)
     - shortName 또는 longName (적어도 하나는 not null)

2. **SearchNews 필드 검증**
   - Given: query = "Apple"
   - When: ufc.search("Apple", newsCount = 5) 호출
   - Then: 첫 번째 news가 다음 필드 포함:
     - uuid (not null)
     - title (not null)
     - link (not null)
     - publishTime > 0

3. **섹터/산업 정보 확인**
   - Given: query = "AAPL"
   - When: ufc.search("AAPL") 호출
   - Then: 첫 번째 quote의 sector, industry가 null이 아님 (주식인 경우)

4. **뉴스 썸네일 확인**
   - Given: query = "Apple"
   - When: ufc.search("Apple", newsCount = 5) 호출
   - Then: 일부 뉴스가 thumbnail을 포함할 수 있음 (optional)

#### 6.2.4 빈 결과 처리 (EmptyResults)

**테스트 케이스**:
1. **존재하지 않는 검색어**
   - Given: query = "XYZNONEXISTENT123"
   - When: ufc.search("XYZNONEXISTENT123") 호출
   - Then:
     - 에러 발생하지 않음
     - quotes가 빈 리스트
     - news가 빈 리스트

2. **특수 문자 검색**
   - Given: query = "!@#$%"
   - When: ufc.search("!@#$%") 호출
   - Then:
     - 에러 발생하지 않음
     - quotes가 빈 리스트 (또는 매우 적은 결과)

#### 6.2.5 에러 케이스 (ErrorCases)

**테스트 케이스**:
1. **빈 검색어**
   - Given: query = ""
   - When: ufc.search("") 호출
   - Then: ValidationException 발생

2. **공백만 있는 검색어**
   - Given: query = "   "
   - When: ufc.search("   ") 호출
   - Then: ValidationException 발생

3. **음수 quotesCount**
   - Given: quotesCount = -1
   - When: ufc.search("AAPL", quotesCount = -1) 호출
   - Then: ValidationException 발생

#### 6.2.6 활용 예제 (UsageExamples)

**테스트 케이스**:
1. **심볼 검색 후 QuoteSummary 조회**
   - Given:
     - query = "Apple"
     - searchResult = ufc.search("Apple")
     - symbol = searchResult.quotes.first().symbol
   - When: ufc.quoteSummary(symbol, QuoteSummaryModule.PRICE) 호출
   - Then:
     - 정상적으로 가격 정보 조회됨
     - symbol이 일치

2. **심볼 검색 후 Chart 조회**
   - Given:
     - query = "Microsoft"
     - searchResult = ufc.search("Microsoft")
     - symbol = searchResult.quotes.first().symbol
   - When: ufc.chart(symbol, Interval.OneDay, Period.OneMonth) 호출
   - Then:
     - 정상적으로 차트 데이터 조회됨

3. **뉴스 링크 접근**
   - Given:
     - query = "Tesla"
     - searchResult = ufc.search("Tesla", newsCount = 5)
     - newsLink = searchResult.news.first().link
   - When: newsLink 확인
   - Then:
     - 유효한 URL 형식
     - link가 비어있지 않음

#### 6.2.7 데이터 접근 방법 (DataAccessExamples)

**테스트 케이스**:
1. **검색 결과에서 심볼 목록 추출**
   - Given: query = "Tech"
   - When:
     - searchResult = ufc.search("Tech", quotesCount = 10)
     - symbols = searchResult.quotes.map { it.symbol }
   - Then: symbols가 비어있지 않음

2. **검색 결과 정렬 확인**
   - Given: query = "Apple"
   - When: searchResult = ufc.search("Apple", quotesCount = 5)
   - Then: quotes가 score 내림차순으로 정렬됨

3. **뉴스 발행 시간 확인**
   - Given: query = "AAPL"
   - When: searchResult = ufc.search("AAPL", newsCount = 5)
   - Then:
     - news가 publishTime 내림차순으로 정렬됨
     - 모든 publishTime > 0

### 6.3 테스트 데이터

**고정 검색어 (Fixtures)**:
```kotlin
object TestFixtures {
    object SearchQuery {
        const val APPLE_SYMBOL = "AAPL"
        const val APPLE_NAME = "Apple"
        const val MICROSOFT_SYMBOL = "MSFT"
        const val SPY_ETF = "SPY"
        const val SAMSUNG = "Samsung"
        const val TYPO_APPLE = "Appel"  // 오타
        const val NONEXISTENT = "XYZNONEXISTENT123"
    }
}
```

### 6.4 Response Recording

**기록 경로**: `src/test/resources/responses/yahoo/search/`

**기록 파일명**:
- `apple_symbol_search.json`
- `apple_name_search.json`
- `microsoft_search.json`
- `spy_etf_search.json`
- `samsung_search.json`
- `fuzzy_search.json`
- `empty_result.json`

**기록 조건**: `RecordingConfig.isRecordingEnabled = true`

---

## 7. 구현 우선순위

### 7.1 Phase 1: 핵심 기능 구현 (우선순위: 높음)

**목표**: 기본적인 검색 기능 구현

**구현 항목**:
1. Internal API 응답 모델 작성
   - `SearchApiResponse.kt`
   - `QuoteResult.kt`
   - `NewsResult.kt`

2. Public API 도메인 모델 작성
   - `SearchResponse.kt`
   - `SearchQuote.kt`
   - `SearchNews.kt`
   - `NewsThumbnail.kt`
   - `ThumbnailResolution.kt`

3. YahooClient에 search 메서드 추가
   - 기본 파라미터: query, quotesCount, newsCount, enableFuzzyQuery
   - HTTP 요청 및 응답 처리
   - 변환 함수 구현

4. Ufc 클래스에 편의 메서드 추가
   - `Ufc.search()` 메서드

5. 입력 검증 로직
   - 빈 검색어 체크
   - 음수 파라미터 체크

6. 에러 처리
   - ValidationException
   - ApiException
   - DataParsingException

### 7.2 Phase 2: 테스트 작성 (우선순위: 높음)

**목표**: 통합 테스트 및 단위 테스트 작성

**구현 항목**:
1. 통합 테스트 파일 작성
   - `SearchSpec.kt`
   - @Nested 그룹핑 적용

2. 주요 시나리오 테스트
   - 기본 동작 (심볼, 회사명 검색)
   - 파라미터 동작 (quotesCount, newsCount, enableFuzzyQuery)
   - 응답 데이터 스펙 검증

3. Response Recording
   - 주요 검색 결과 JSON 기록

### 7.3 Phase 3: 고급 기능 및 최적화 (우선순위: 중간)

**목표**: 사용성 개선 및 최적화

**구현 항목**:
1. 캐싱 지원 (선택적)
   - 짧은 TTL(10초) 캐싱
   - 동일 검색어 반복 조회 최적화

2. 추가 편의 메서드
   - `searchSymbols()`: 종목만 검색 (newsCount = 0)
   - `searchNews()`: 뉴스만 검색 (quotesCount = 0)

3. 검색 결과 정렬 유틸리티
   - score 기준 정렬 (이미 API에서 정렬되어 옴)
   - quoteType별 필터링 유틸리티

### 7.4 Phase 4: 문서화 및 예제 (우선순위: 낮음)

**목표**: 사용자 가이드 작성

**구현 항목**:
1. README 업데이트
   - Search API 사용법 추가
   - 예제 코드 추가

2. API 문서 생성
   - KDoc 주석 작성
   - Dokka 문서 생성

3. 활용 예제 작성
   - 심볼 자동완성 예제
   - 검색 → QuoteSummary 연계 예제

---

## 부록

### A. yfinance Search 구현 참조

**yfinance의 Search 클래스 주요 특징**:
1. 생성자에서 자동으로 search() 호출
2. 프로퍼티로 결과 접근: `quotes`, `news`, `lists`, `research`, `nav`
3. `all` 프로퍼티: 모든 결과를 dict로 반환
4. `response` 프로퍼티: 원본 API 응답 반환

**UFC 설계와의 차이점**:
- **yfinance**: 클래스 인스턴스 생성 시 자동 검색
- **UFC**: `YahooClient.search()` 메서드 호출로 검색, `SearchResponse` 반환

**이유**:
- Kotlin의 suspend 함수와 코루틴 지원
- 일관된 API 설계 (QuoteSummary, Chart와 동일 패턴)

### B. 검색어 예시

**정확한 심볼**:
- "AAPL", "MSFT", "GOOGL", "TSLA", "SPY", "QQQ"

**회사명**:
- "Apple", "Microsoft", "Google", "Tesla", "Samsung"

**다국가 종목**:
- "Samsung" → 005930.KS (한국), SSNLF (미국 OTC)
- "Toyota" → 7203.T (일본), TM (미국 ADR)

**오타 (퍼지 검색)**:
- "Appel" → "Apple"
- "Microsft" → "Microsoft"

**산업/섹터 키워드**:
- "Tech", "Energy", "Finance"

### C. API 제한사항

**Yahoo Finance Search API 제한**:
1. **비공식 API**: 공식 문서 없음, 변경 가능성 있음
2. **Rate Limiting**: 과도한 요청 시 차단 가능
3. **검색어 길이**: 경험적으로 500자 제한 권장
4. **결과 개수**: quotesCount, newsCount 최대값 제한 없으나 100개 이상은 비효율적
5. **퍼지 검색**: enableFuzzyQuery 활성화 시 응답 속도 느려질 수 있음

**UFC 설계 시 고려사항**:
- 기본값 유지 (quotesCount=8, newsCount=8)
- 사용자가 필요 시 파라미터 조정 가능
- Rate Limiter를 통한 자동 제한

### D. 참고 자료

- yfinance search.py: `/home/ulalax/project/kairos/yfinance/yfinance/search.py`
- UFC YahooClient: `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/yahoo/YahooClient.kt`
- UFC ErrorCode: `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/common/exception/ErrorCode.kt`
- 통합 테스트 예제: `/home/ulalax/project/kairos/ufc/src/test/kotlin/com/ulalax/ufc/integration/businessinsider/IsinSearchSpec.kt`

---

**문서 끝**
