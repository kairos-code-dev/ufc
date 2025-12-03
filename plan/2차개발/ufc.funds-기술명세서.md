# UFC.FUNDS 기술명세서 (Technical Specification)

## 문서 정보
- **버전**: 2.0.0
- **최종 수정일**: 2025-12-03
- **작성자**: Claude Code (Software Architect)
- **문서 상태**: Design Specification
- **문서 유형**: 설계 명세서 (코드 구현 제외)

---

## 목차
1. [개요](#1-개요)
2. [아키텍처 설계](#2-아키텍처-설계)
3. [데이터 모델 정의](#3-데이터-모델-정의)
4. [서비스 인터페이스](#4-서비스-인터페이스)
5. [API 명세](#5-api-명세)
6. [데이터 처리 흐름](#6-데이터-처리-흐름)
7. [에러 처리 전략](#7-에러-처리-전략)
8. [캐싱 전략](#8-캐싱-전략)
9. [테스트 전략](#9-테스트-전략)
10. [파일 구조](#10-파일-구조)

---

## 1. 개요

### 1.1 목적

Funds 도메인은 Yahoo Finance를 통해 ETF(Exchange-Traded Fund) 및 뮤추얼펀드의 상세 정보를 제공하는 통합 서비스입니다.

**핵심 기능:**
- ETF/뮤추얼펀드 보유 종목 조회
- 자산 클래스 배분 (현금, 주식, 채권 등)
- 섹터별 비중 분석
- 펀드 운영 정보 (비용률, 회전율, 총순자산)
- 주식/채권 보유 메트릭

### 1.2 범위

**지원 자산 유형:**
- ETF (Exchange-Traded Fund)
- MUTUALFUND (Mutual Fund)

**통합 처리:**
- ETF와 뮤추얼펀드는 동일한 데이터 모델로 처리
- `quoteType` 필드로만 구분 ("ETF" vs "MUTUALFUND")
- 동일한 API 모듈 활용

**제공 데이터 (10개 주요 속성):**

| 속성 | 설명 |
|------|------|
| quoteType | 자산 유형 (ETF, MUTUALFUND) |
| description | 펀드 설명 |
| fundOverview | 펀드 개요 (카테고리, 패밀리, 법적형태) |
| fundOperations | 운영 정보 (비용률, 회전율, 총순자산) |
| assetClasses | 자산 배분 (현금, 주식, 채권 등) |
| topHoldings | 상위 보유 종목 |
| equityHoldings | 주식 보유 메트릭 (PER, PBR, PSR 등) |
| bondHoldings | 채권 보유 메트릭 (듀레이션, 만기, 신용등급) |
| bondRatings | 채권 등급 분포 |
| sectorWeightings | 섹터별 비중 |

### 1.3 데이터 소스

**Yahoo Finance API:**
- 엔드포인트: `https://query2.finance.yahoo.com/v10/finance/quoteSummary/{symbol}`
- 요청 모듈: `quoteType`, `summaryProfile`, `topHoldings`, `fundProfile`, `defaultKeyStatistics`
- 인증: crumb 토큰 기반

### 1.4 yfinance 호환성

**Python yfinance의 `FundsData` 클래스를 Kotlin으로 이식:**
- 동일한 데이터 구조 제공
- 타입 안전성 강화
- null 안전성 보장

---

## 2. 아키텍처 설계

### 2.1 레이어 구조

```
┌─────────────────────────────────────────────────────────────┐
│                     Application Layer                       │
│                   (User Application)                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   Domain API Layer                          │
│                                                              │
│  ┌────────────────────────────────────────────────────┐     │
│  │              FundsApi (Interface)                   │     │
│  │  - getFundData(symbol): FundData                    │     │
│  │  - getFundData(symbols): Map<String, FundData>      │     │
│  │  - getRawFundData(symbol): FundDataResponse         │     │
│  │  - isFund(symbol): Boolean                          │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                 Internal Implementation                     │
│                                                              │
│  ┌────────────────────────────────────────────────────┐     │
│  │         YahooFundsService (Implementation)          │     │
│  │  - httpClient: HttpClient                           │     │
│  │  - rateLimiter: RateLimiter                         │     │
│  │  - authResult: AuthResult                           │     │
│  │  - cache: ConcurrentHashMap                         │     │
│  │                                                      │     │
│  │  Private Methods:                                    │     │
│  │  - parseDescription()                                │     │
│  │  - parseFundOverview()                               │     │
│  │  - parseFundOperations()                             │     │
│  │  - parseAssetClasses()                               │     │
│  │  - parseTopHoldings()                                │     │
│  │  - parseEquityHoldings()                             │     │
│  │  - parseBondHoldings()                               │     │
│  │  - parseBondRatings()                                │     │
│  │  - parseSectorWeightings()                           │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   Source Layer                              │
│                                                              │
│  ┌────────────────────────────────────────────────────┐     │
│  │         YahooFinanceSource                          │     │
│  │  - QuoteSummaryAPI                                  │     │
│  │  - Authenticator (Cookie/Crumb)                     │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              Infrastructure Layer                           │
│                                                              │
│  ┌─────────────┐  ┌──────────┐  ┌────────┐  ┌──────────┐   │
│  │ HTTP Client │  │  Cache   │  │ Retry  │  │   Log    │   │
│  │   (Ktor)    │  │  (LRU)   │  │ Logic  │  │ (SLF4J)  │   │
│  └─────────────┘  └──────────┘  └────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 컴포넌트 다이어그램

```
┌─────────────────────────────────────────────────────────────┐
│                      FundsApi                               │
│                                                              │
│  Public Methods:                                             │
│  ┌────────────────────────────────────────────────────┐     │
│  │ getFundData(symbol)                                 │     │
│  │   └─> FundData (정규화 모델)                        │     │
│  │                                                      │     │
│  │ getFundData(symbols)                                 │     │
│  │   └─> Map<String, FundData>                         │     │
│  │                                                      │     │
│  │ getRawFundData(symbol)                               │     │
│  │   └─> FundDataResponse (API 응답)                   │     │
│  │                                                      │     │
│  │ isFund(symbol)                                       │     │
│  │   └─> Boolean (ETF/MUTUALFUND 여부)                 │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            ↓ uses
┌─────────────────────────────────────────────────────────────┐
│                 YahooFinanceSource                          │
│                                                              │
│  ┌────────────────────────────────────────────────────┐     │
│  │ quoteSummary(symbol, modules)                       │     │
│  │   └─> QuoteSummaryResponse                          │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 책임 분리 (Separation of Concerns)

| 레이어 | 책임 | 예시 |
|--------|------|------|
| **Domain API** | 비즈니스 로직, 유효성 검증 | 펀드 여부 확인, 심볼 검증 |
| **Internal** | 데이터 변환, 파싱 | API 응답 → FundData |
| **Source** | 데이터 소스 추상화 | Yahoo Finance API 호출 |
| **Infrastructure** | 공통 인프라 | HTTP, Cache, Retry |

---

## 3. 데이터 모델 정의

### 3.1 Domain 모델 (Public API)

#### FundData (정규화 모델)

**목적:** ETF/뮤추얼펀드의 통합 정보

```
FundData
├── symbol: String
├── quoteType: String (ETF | MUTUALFUND)
├── description: String?
├── fundOverview: FundOverview?
├── fundOperations: FundOperations?
├── assetClasses: AssetClasses?
├── topHoldings: List<Holding>?
├── equityHoldings: EquityHoldingsMetrics?
├── bondHoldings: BondHoldingsMetrics?
├── bondRatings: Map<String, Double>?
└── sectorWeightings: Map<String, Double>?
```

#### FundOverview (펀드 개요)

```
FundOverview
├── categoryName: String? (예: "Large Blend", "High Yield Bond")
├── family: String? (예: "Vanguard", "iShares")
└── legalType: String? (예: "Exchange Traded Fund")
```

#### FundOperations (운영 정보)

```
FundOperations
├── annualReportExpenseRatio: OperationMetric?
├── annualHoldingsTurnover: OperationMetric?
└── totalNetAssets: OperationMetric?

OperationMetric
├── fundValue: Double?
└── categoryAverage: Double?
```

#### AssetClasses (자산 배분)

```
AssetClasses
├── cashPosition: Double? (%)
├── stockPosition: Double? (%)
├── bondPosition: Double? (%)
├── preferredPosition: Double? (%)
├── convertiblePosition: Double? (%)
└── otherPosition: Double? (%)

Methods:
- totalAllocation(): Double  // 전체 배분 합계 (검증용)
```

#### Holding (보유 종목)

```
Holding
├── symbol: String
├── name: String
└── holdingPercent: Double
```

#### EquityHoldingsMetrics (주식 보유 메트릭)

```
EquityHoldingsMetrics
├── priceToEarnings: MetricValue?
├── priceToBook: MetricValue?
├── priceToSales: MetricValue?
├── priceToCashflow: MetricValue?
├── medianMarketCap: MetricValue?
└── threeYearEarningsGrowth: MetricValue?

MetricValue
├── fundValue: Double?
└── categoryAverage: Double?
```

#### BondHoldingsMetrics (채권 보유 메트릭)

```
BondHoldingsMetrics
├── duration: MetricValue? (년)
├── maturity: MetricValue? (년)
└── creditQuality: MetricValue? (숫자)
```

### 3.2 Response 모델 (Internal)

#### FundDataResponse (API 응답)

```
FundDataResponse
└── quoteSummary: QuoteSummary
    ├── result: List<QuoteSummaryResult>?
    └── error: ApiError?

QuoteSummaryResult
├── quoteType: QuoteTypeRaw?
├── summaryProfile: SummaryProfileRaw?
├── topHoldings: TopHoldingsRaw?
├── fundProfile: FundProfileRaw?
└── defaultKeyStatistics: DefaultKeyStatisticsRaw?
```

#### TopHoldingsRaw (topHoldings 모듈)

```
TopHoldingsRaw
├── cashPosition: ValueFormat?
├── stockPosition: ValueFormat?
├── bondPosition: ValueFormat?
├── holdings: List<HoldingRaw>?
├── equityHoldings: EquityHoldingsRaw?
├── bondHoldings: BondHoldingsRaw?
├── bondRatings: List<BondRatingRaw>?
└── sectorWeightings: List<SectorWeightingRaw>?
```

#### ValueFormat (Yahoo Finance 표준 포맷)

```
ValueFormat
├── raw: Double?
├── fmt: String?
└── longFmt: String?
```

### 3.3 데이터 변환 전략

**변환 규칙:**

| Response 모델 | Domain 모델 | 변환 규칙 |
|--------------|------------|----------|
| `TopHoldingsRaw` | `AssetClasses` | ValueFormat.raw 추출 |
| `List<HoldingRaw>` | `List<Holding>` | 필터링 (null 제거) |
| `EquityHoldingsRaw` | `EquityHoldingsMetrics` | 펀드값 + 카테고리 평균값 매핑 |
| `List<SectorWeightingRaw>` | `Map<String, Double>` | 키-값 쌍으로 변환 |

**변환 위치:**
- `YahooFundsService` 내부 private 메서드
- 응답 모델은 외부 노출 금지

---

## 4. 서비스 인터페이스

### 4.1 FundsApi (Public Interface)

```kotlin
interface FundsApi {

    /**
     * 펀드 데이터 조회
     *
     * @param symbol 펀드 심볼 (예: SPY, QQQ, VTI, VTSAX)
     * @return FundData 정규화된 펀드 데이터
     * @throws UFCException
     */
    suspend fun getFundData(symbol: String): FundData

    /**
     * 다중 펀드 데이터 조회
     *
     * @param symbols 심볼 목록
     * @return Map<String, FundData>
     * @throws UFCException
     */
    suspend fun getFundData(symbols: List<String>): Map<String, FundData>

    /**
     * 원본 API 응답 조회
     *
     * @param symbol 펀드 심볼
     * @return FundDataResponse 원본 응답
     * @throws UFCException
     */
    suspend fun getRawFundData(symbol: String): FundDataResponse

    /**
     * 펀드 여부 확인
     *
     * @param symbol 심볼
     * @return Boolean (ETF 또는 MUTUALFUND일 경우 true)
     * @throws UFCException
     */
    suspend fun isFund(symbol: String): Boolean
}
```

### 4.2 YahooFundsService (Internal Implementation)

```kotlin
internal class YahooFundsService(
    private val httpClient: HttpClient,
    private val rateLimiter: RateLimiter,
    private val authResult: AuthResult
) : FundsApi {

    // Public API 구현
    override suspend fun getFundData(symbol: String): FundData
    override suspend fun getFundData(symbols: List<String>): Map<String, FundData>
    override suspend fun getRawFundData(symbol: String): FundDataResponse
    override suspend fun isFund(symbol: String): Boolean

    // Private 헬퍼 메서드 (구현 제외)
    private suspend fun fetchQuoteSummary(symbol: String, modules: List<String>)
    private fun validateSymbol(symbol: String)
    private fun parseFundData(symbol: String, response: FundDataResponse): FundData
    private fun parseDescription(result: QuoteSummaryResult): String?
    private fun parseFundOverview(result: QuoteSummaryResult): FundOverview?
    private fun parseFundOperations(result: QuoteSummaryResult): FundOperations?
    private fun parseAssetClasses(result: QuoteSummaryResult): AssetClasses?
    private fun parseTopHoldings(result: QuoteSummaryResult): List<Holding>?
    private fun parseEquityHoldings(result: QuoteSummaryResult): EquityHoldingsMetrics?
    private fun parseBondHoldings(result: QuoteSummaryResult): BondHoldingsMetrics?
    private fun parseBondRatings(result: QuoteSummaryResult): Map<String, Double>?
    private fun parseSectorWeightings(result: QuoteSummaryResult): Map<String, Double>?
}
```

---

## 5. API 명세

### 5.1 Yahoo Finance QuoteSummary API

**Base URL:**
```
https://query2.finance.yahoo.com/v10/finance/quoteSummary/{symbol}
```

**Request Method:** GET

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| modules | String | Y | 쉼표로 구분된 모듈 리스트 |
| crumb | String | Y | 인증 토큰 |
| corsDomain | String | N | CORS 도메인 (기본값: finance.yahoo.com) |
| formatted | String | N | 포맷 여부 (기본값: false) |

**Funds API 모듈:**

| 모듈 | 설명 | 사용 도메인 메서드 |
|------|------|-------------------|
| quoteType | 자산 타입 (ETF, MUTUALFUND) | isFund() |
| summaryProfile | 펀드 설명 | getFundData() |
| topHoldings | 보유 종목, 자산 배분 | getFundData() |
| fundProfile | 펀드 프로필 | getFundData() |
| defaultKeyStatistics | 기본 통계 | getFundData() |

### 5.2 Response 구조

**Success Response:**

```json
{
  "quoteSummary": {
    "result": [
      {
        "quoteType": {"quoteType": "ETF"},
        "summaryProfile": {
          "longBusinessSummary": "The SPDR S&P 500 ETF Trust..."
        },
        "topHoldings": {
          "cashPosition": {"raw": 0.12, "fmt": "0.12%"},
          "stockPosition": {"raw": 99.88, "fmt": "99.88%"},
          "holdings": [
            {
              "symbol": "AAPL",
              "holdingName": "Apple Inc",
              "holdingPercent": {"raw": 7.2, "fmt": "7.20%"}
            }
          ],
          "sectorWeightings": [
            {
              "technology": {"raw": 28.5, "fmt": "28.50%"}
            }
          ]
        },
        "fundProfile": {
          "categoryName": "Large Blend",
          "family": "SPDR State Street Global Advisors",
          "legalType": "Exchange Traded Fund"
        }
      }
    ],
    "error": null
  }
}
```

**Error Response:**

```json
{
  "quoteSummary": {
    "result": null,
    "error": {
      "code": "Not Found",
      "description": "No data found for symbol"
    }
  }
}
```

### 5.3 Rate Limiting

**제한:**
- Yahoo Finance: 비공식 제한 (추정 2000 req/hour)
- 권장: 50 req/second 이하

**전략:**
- TokenBucket Rate Limiter 적용
- Exponential Backoff
- Cache 우선 사용

---

## 6. 데이터 처리 흐름

### 6.1 getFundData() 흐름도

```
User Request: getFundData("SPY")
    ↓
[1] 심볼 검증
    - 빈 문자열 체크
    - 길이 제한 (10자 이하)
    ↓
[2] 캐시 조회 (Key: "SPY")
    ↓
    ├─> Cache Hit → FundData 반환
    │
    └─> Cache Miss
         ↓
        [3] Rate Limiter 토큰 획득
            ↓
        [4] QuoteSummary API 호출
            - modules: quoteType,summaryProfile,topHoldings,fundProfile,defaultKeyStatistics
            - 인증: Cookie + Crumb
            ↓
        [5] 응답 검증
            - HTTP 상태 확인
            - error 필드 확인
            - result null 체크
            ↓
        [6] quoteType 검증
            - quoteType == "ETF" or "MUTUALFUND"
            ↓
        [7] 데이터 변환 (9개 파싱 함수 실행)
            - parseDescription()
            - parseFundOverview()
            - parseFundOperations()
            - parseAssetClasses()
            - parseTopHoldings()
            - parseEquityHoldings()
            - parseBondHoldings()
            - parseBondRatings()
            - parseSectorWeightings()
            ↓
        [8] FundData 생성
            ↓
        [9] 캐시 저장 (TTL: 무제한)
            ↓
        [10] FundData 반환
```

### 6.2 에러 시나리오 흐름

```
API 호출 실패
    ↓
[1] HTTP Status 확인
    ├─> 401/403 → ErrorCode.AUTH_FAILED
    ├─> 404 → ErrorCode.FUND_DATA_NOT_FOUND
    ├─> 429 → ErrorCode.RATE_LIMITED
    ├─> 500/502/503 → ErrorCode.EXTERNAL_API_ERROR
    └─> 기타 → ErrorCode.DATA_RETRIEVAL_ERROR
    ↓
[2] Retry 로직
    - 401/403/404: Retry 없음
    - 429: Exponential Backoff (최대 3회)
    - 500+: Exponential Backoff (최대 3회)
    ↓
[3] UFCException 생성
    - errorCode 설정
    - message 설정
    - metadata 추가
    ↓
[4] Exception throw
```

### 6.3 데이터 변환 흐름

```
TopHoldingsRaw
    ↓
[1] AssetClasses 변환
    cashPosition.raw → AssetClasses.cashPosition
    stockPosition.raw → AssetClasses.stockPosition
    ...
    ↓
[2] Holdings 변환
    holdings[] → List<Holding>
    mapNotNull { symbol, holdingName, holdingPercent.raw }
    ↓
[3] SectorWeightings 변환
    sectorWeightings[0] → Map<String, Double>
    technology.raw → "technology": 28.5
    ...
    ↓
[4] EquityMetrics 변환
    equityHoldings → EquityHoldingsMetrics
    priceToEarnings.raw → MetricValue(fundValue, categoryAverage)
    ...
    ↓
[5] BondMetrics 변환
    bondHoldings → BondHoldingsMetrics
    duration.raw → MetricValue(fundValue, categoryAverage)
    ...
    ↓
[6] FundData 생성
```

---

## 7. 에러 처리 전략

### 7.1 에러 분류

| ErrorCode | HTTP Status | 설명 | Retry 여부 |
|-----------|------------|------|-----------|
| INVALID_SYMBOL | - | 심볼 형식 오류 | No |
| FUND_DATA_NOT_FOUND | 404 | 펀드 데이터 없음 | No |
| INVALID_FUND_TYPE | - | ETF/MUTUALFUND 아님 | No |
| AUTH_FAILED | 401, 403 | 인증 실패 | No (재인증 필요) |
| RATE_LIMITED | 429 | Rate Limit 초과 | Yes (Backoff) |
| EXTERNAL_API_ERROR | 500+ | API 서버 오류 | Yes |
| DATA_PARSING_ERROR | - | 파싱 오류 | No |
| INCOMPLETE_FUND_DATA | - | 불완전한 데이터 | No |

### 7.2 커스텀 예외

```kotlin
// InvalidFundTypeException
class InvalidFundTypeException(message: String, cause: Throwable? = null)
    : UFCException(ErrorCode.INVALID_FUND_TYPE, message, cause)

// IncompleteFundDataException
class IncompleteFundDataException(message: String, cause: Throwable? = null)
    : UFCException(ErrorCode.INCOMPLETE_FUND_DATA, message, cause)
```

### 7.3 에러 처리 전략

**검증 에러 (Client-side):**
- 즉시 throw
- Retry 없음

**API 에러 (Server-side):**
- Retry 정책 적용
- Exponential Backoff
- 최대 3회 재시도

**파싱 에러:**
- 응답 로깅 (500자 제한)
- DATA_PARSING_ERROR throw
- Retry 없음

---

## 8. 캐싱 전략

### 8.1 캐시 정책

| 데이터 타입 | Cache Key | TTL | Storage |
|-----------|----------|-----|---------|
| FundData | `{symbol}` | 무제한 | ConcurrentHashMap |

**캐시 무효화:**
- 자동: 서비스 재시작 시
- 수동: API 미제공

**캐시 전략:**
- **Cache-Aside Pattern**
  1. 캐시 조회
  2. Cache Miss → API 호출
  3. 캐시 저장
  4. 데이터 반환

**이유:**
- 펀드 정보는 거의 변경되지 않음
- 보유 종목은 일일 단위로 변경 (실시간 반영 불필요)

### 8.2 캐시 크기 제한

**제한 없음 (ConcurrentHashMap):**
- 메모리 효율성보다 성능 우선
- 대부분의 사용 시나리오에서 심볼 수는 제한적

---

## 9. 테스트 전략

### 9.1 Unit Test (Fast, Isolated)

**테스트 대상:**
- 데이터 변환 로직
- 펀드 검증 로직
- 에러 처리 로직

**테스트 방법:**
- Fake 객체 사용 (FakeHttpClient, FakeRateLimiter, FakeAuthStrategy)
- JSON 픽스쳐 사용 (src/test/resources/fixtures/funds/)
- 외부 API 호출 없음

**테스트 케이스:**

| 테스트 | 입력 | 예상 출력 |
|--------|------|----------|
| getFundData_success | "SPY" | FundData (정상) |
| getFundData_notAFund | "AAPL" | InvalidFundTypeException |
| getFundData_notFound | "INVALID" | FUND_DATA_NOT_FOUND |
| parseAssetClasses_total100 | TopHoldingsRaw | totalAllocation() ≈ 100% |
| convertToHoldings_filterNull | HoldingRaw (일부 null) | null 제거된 List<Holding> |

### 9.2 Live Test (Slow, Real API)

**테스트 대상:**
- 실제 API 통합
- 인증 흐름
- 응답 파싱

**테스트 방법:**
- 실제 Yahoo Finance API 호출
- 응답 자동 녹화 (ResponseRecorder)
- 녹화된 데이터 → Unit Test 픽스쳐

**테스트 픽스쳐:**

| Symbol | 타입 | 설명 |
|--------|------|------|
| SPY | ETF | 주식형 ETF (S&P 500) |
| AGG | ETF | 채권형 ETF |
| JNK | ETF | 고수익 채권 ETF |
| VTI | ETF | Total Market ETF |
| VTSAX | MUTUALFUND | 뮤추얼펀드 |

### 9.3 Integration Test

**테스트 시나리오:**
- UFCClient → FundsApi → YahooSource → API
- 캐시 통합 테스트 (Cache Hit/Miss)
- 에러 전파 테스트

---

## 10. 파일 구조

### 10.1 디렉토리 레이아웃

```
src/main/kotlin/com/ulalax/ufc/
├── api/                              # Public API
│   └── FundsApi.kt                   # Funds 도메인 인터페이스
│
├── internal/funds/                   # Internal Implementation
│   ├── YahooFundsService.kt          # Funds 서비스 구현체
│   ├── FundsValidator.kt             # 검증 로직 (선택적)
│   └── FundsConverter.kt             # 변환 로직 (선택적)
│
├── model/funds/                      # Domain Models (Public)
│   ├── FundData.kt                   # 정규화 모델
│   ├── FundOverview.kt
│   ├── FundOperations.kt
│   ├── AssetClasses.kt
│   ├── Holding.kt
│   ├── EquityHoldingsMetrics.kt
│   ├── BondHoldingsMetrics.kt
│   └── MetricValue.kt
│
├── internal/yahoo/response/          # Response Models (Internal)
│   ├── FundDataResponse.kt
│   ├── QuoteSummaryResult.kt
│   ├── TopHoldingsRaw.kt
│   ├── FundProfileRaw.kt
│   ├── EquityHoldingsRaw.kt
│   └── ValueFormat.kt
│
└── exception/
    ├── UFCException.kt
    ├── ErrorCode.kt
    └── FundDataExceptions.kt         # 커스텀 예외
```

### 10.2 파일별 책임

| 파일 | 책임 | Public/Internal |
|------|------|-----------------|
| FundsApi.kt | 도메인 인터페이스 정의 | Public |
| YahooFundsService.kt | 비즈니스 로직 구현 | Internal |
| FundData.kt | 정규화된 Domain 모델 | Public |
| FundDataResponse.kt | API 응답 모델 | Internal |

---

## 11. QuoteSummaryService와의 관계

### 11.1 공유 컴포넌트

**같은 API 사용:**
- Yahoo Finance quoteSummary endpoint
- 동일한 인증 메커니즘 (Cookie/Crumb)
- 동일한 Response 구조 (QuoteSummaryResponse)

**다른 모듈 요청:**

| 서비스 | 모듈 |
|--------|------|
| FundsApi | quoteType, summaryProfile, topHoldings, fundProfile, defaultKeyStatistics |
| StockApi | price, summaryDetail, financialData, earningsTrend |

### 11.2 차이점

| 항목 | QuoteSummaryService | FundsService |
|-----|---------------------|-------------|
| 대상 자산 | 주식 (EQUITY) | ETF, MUTUALFUND |
| 반환 데이터 | StockSummary | FundData |
| 캐싱 전략 | 없음 | ConcurrentHashMap |
| yfinance 대응 | Ticker.info | Ticker.funds_data |

---

## 12. 향후 확장

### 12.1 Phase 2: Batch 조회 (다중 펀드 데이터)

#### 12.1.1 개요

여러 펀드 심볼에 대해 한 번에 데이터를 조회하는 기능입니다. 단일 조회보다 효율적으로 처리할 수 있도록 설계됩니다.

#### 12.1.2 메서드 명세

```kotlin
/**
 * 다중 펀드 데이터 조회
 *
 * @param symbols 펀드 심볼 리스트 (예: listOf("SPY", "AGG", "VTI"))
 * @return Map<String, FundData> 심볼을 키로, 펀드 데이터를 값으로 하는 맵
 * @throws UFCException
 *   - INVALID_SYMBOL: 유효하지 않은 심볼이 포함된 경우
 *   - FUND_DATA_NOT_FOUND: 데이터를 찾을 수 없는 심볼이 있는 경우
 *   - RATE_LIMITED: Rate Limit 초과
 *   - EXTERNAL_API_ERROR: API 서버 오류
 */
suspend fun getFundData(symbols: List<String>): Map<String, FundData>
```

#### 12.1.3 동시성 전략

**병렬 처리:**
- 여러 심볼을 동시에 호출하여 성능 최적화
- `coroutineScope`를 사용한 구조적 동시성
- Rate Limiter와 협력하여 API 제한 준수

**구현 예시 (개념):**
```kotlin
suspend fun getFundData(symbols: List<String>): Map<String, FundData> {
    return coroutineScope {
        symbols.associate { symbol ->
            symbol to async { getFundData(symbol) }
        }.mapValues { (_, deferred) ->
            deferred.await()
        }
    }
}
```

**Rate Limiting 고려사항:**
- 동시 요청 수를 RateLimiter로 제어
- 토큰 소비: 심볼 개수만큼 토큰 필요
- Exponential Backoff 적용

#### 12.1.4 에러 처리

**부분 실패 시나리오:**

| 시나리오 | 처리 방식 |
|--------|---------|
| 일부 심볼 실패 | 성공한 것만 반환, 실패한 것은 생략 |
| 모든 심볼 실패 | Exception throw |
| 유효하지 않은 심볼 | 즉시 INVALID_SYMBOL 예외 |

**에러 응답 예시:**
```json
{
  "SPY": {"symbol": "SPY", "quoteType": "ETF", ...},
  "AGG": {"symbol": "AGG", "quoteType": "ETF", ...},
  // VTI는 실패하여 생략
}
```

#### 12.1.5 캐시 활용

**최적화 전략:**
1. 입력된 심볼 목록 중 캐시에 있는 것은 즉시 반환
2. 캐시에 없는 심볼만 API 호출
3. 호출 결과를 캐시에 저장
4. 캐시된 것과 신규 조회 결과를 병합하여 반환

**캐시 히트율 향상:**
- 자주 조회되는 심볼들은 캐시되어 있음
- 네트워크 왕복 감소

#### 12.1.6 파라미터 검증

```kotlin
private fun validateSymbols(symbols: List<String>) {
    require(symbols.isNotEmpty()) { "심볼 리스트가 비어있습니다" }
    require(symbols.size <= 50) { "최대 50개의 심볼만 조회 가능합니다" }
    symbols.forEach { validateSymbol(it) }
}
```

**제약 사항:**
- 최소 1개, 최대 50개 심볼
- 각 심볼은 1-10자 영문/숫자

#### 12.1.7 테스트 케이스

```kotlin
// 성공 케이스
test("getFundData with multiple symbols") {
    val symbols = listOf("SPY", "AGG", "VTI")
    val result = api.getFundData(symbols)

    assertThat(result).hasSize(3)
    assertThat(result["SPY"]).isNotNull()
    assertThat(result["AGG"]).isNotNull()
    assertThat(result["VTI"]).isNotNull()
}

// 부분 실패 케이스
test("getFundData with partial failure") {
    val symbols = listOf("SPY", "INVALID", "AGG")
    val result = api.getFundData(symbols)

    // INVALID는 제외되고 나머지만 반환
    assertThat(result).hasSize(2)
    assertThat(result).containsKeys("SPY", "AGG")
    assertThat(result).doesNotContainKey("INVALID")
}

// 캐시 활용 테스트
test("getFundData uses cache for known symbols") {
    // 첫 번째 호출
    val result1 = api.getFundData(listOf("SPY"))

    // 두 번째 호출 (캐시 히트)
    val result2 = api.getFundData(listOf("SPY"))

    assertThat(result1["SPY"]).isEqualTo(result2["SPY"])
    // API 호출 횟수는 1회만
}

// 동시성 테스트
test("getFundData handles concurrent requests") {
    val symbols = listOf("SPY", "AGG", "VTI", "QQQ", "IWM")

    val results = (1..10).map {
        async { api.getFundData(symbols) }
    }.awaitAll()

    results.forEach { result ->
        assertThat(result).hasSize(5)
    }
}
```

#### 12.1.8 성능 고려사항

**시간 복잡도:**
- 캐시 히트: O(n) - n은 심볼 개수
- 캐시 미스: O(n * api_latency) - 병렬 처리 시 최소화

**메모리 사용:**
- 결과 맵: O(n * data_size)
- 내부 캐시: ConcurrentHashMap으로 관리

**네트워크 효율:**
- 병렬 호출로 총 대기 시간 단축
- 단일 호출보다 대역폭 활용 최대화

---

## 참고 자료

- **Python yfinance**: https://github.com/ranaroussi/yfinance
- **UFC Architecture**: `/plan/1차개발/01-architecture-design.md`
- **Error Handling**: `/plan/1차개발/02-error-handling.md`
- **Yahoo Finance Funds**: `/plan/1차개발/04-yahoo-finance-etf.md`

---

**최종 수정일**: 2025-12-03
**문서 버전**: 2.0.0
**문서 유형**: 설계 명세서 (코드 구현 제외)
