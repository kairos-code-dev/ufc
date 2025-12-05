# Screener API 기능 명세서

## 문서 정보
- 작성일: 2025-12-05
- 대상 프로젝트: UFC (Unified Finance Client)
- 버전: 1.0.0
- 참조: yfinance screener 구현

---

## 1. API 개요

### 1.1 목적
Screener API는 Yahoo Finance의 종목 스크리닝 기능을 제공하여, 사용자 정의 조건에 따라 주식 및 펀드를 검색하고 필터링할 수 있게 합니다.

### 1.2 주요 사용 사례
- **조건부 종목 검색**: 시가총액, PER, 거래량 등 재무지표 기반 검색
- **사전 정의된 스크리너**: Yahoo Finance가 제공하는 인기 스크리너 활용 (예: day_gainers, most_actives)
- **다중 조건 조합**: AND/OR 논리 연산자를 사용한 복합 쿼리
- **정렬 및 페이지네이션**: 결과 정렬, 오프셋 및 크기 제어
- **섹터/산업별 필터링**: 특정 섹터나 산업군에 속한 종목 검색
- **펀드 스크리닝**: 뮤추얼펀드 및 ETF 검색

### 1.3 지원 자산 유형
- **EQUITY**: 주식 (일반 주식, 우선주)
- **MUTUALFUND**: 뮤추얼펀드

---

## 2. Yahoo Finance Screener API 분석

### 2.1 Endpoint 정보

#### 2.1.1 Custom Query API
- **URL**: `POST https://query1.finance.yahoo.com/v1/finance/screener`
- **용도**: 사용자 정의 쿼리 실행
- **인증**: Crumb 토큰 필요

#### 2.1.2 Predefined Query API
- **URL**: `GET https://query1.finance.yahoo.com/v1/finance/screener/predefined/saved`
- **용도**: Yahoo가 미리 정의한 스크리너 실행
- **인증**: Crumb 토큰 필요

### 2.2 요청 구조

#### 2.2.1 Custom Query 요청 (POST Body)
```
{
  "query": {
    "operator": "AND" | "OR" | "EQ" | "GT" | "LT" | "GTE" | "LTE" | "BTWN",
    "operands": [...]
  },
  "quoteType": "EQUITY" | "MUTUALFUND",
  "sortField": "ticker" | "percentchange" | "dayvolume" | ...,
  "sortType": "ASC" | "DESC",
  "size": 100,           // 기본값: 100, 최대: 250
  "offset": 0,           // 기본값: 0
  "userId": "",          // 기본값: ""
  "userIdType": "guid"   // 기본값: "guid"
}
```

#### 2.2.2 Predefined Query 요청 (GET Query Parameters)
```
?scrIds=aggressive_small_caps
&count=25              // 기본값: 25, 최대: 250
&offset=0              // offset은 무시됨 (Yahoo 제한)
&sortField=ticker
&sortType=DESC
&userId=
&userIdType=guid
&corsDomain=finance.yahoo.com
&formatted=false
&lang=en-US
&region=US
```

#### 2.2.3 Query 연산자

| 연산자 | 설명 | Operand 구조 | 예시 |
|--------|------|-------------|------|
| `AND` | 논리 AND | `[Query, Query, ...]` | 여러 조건 모두 만족 |
| `OR` | 논리 OR | `[Query, Query, ...]` | 여러 조건 중 하나 만족 |
| `EQ` | 같음 | `[field, value]` | `["sector", "Technology"]` |
| `GT` | 초과 | `[field, number]` | `["intradaymarketcap", 1000000000]` |
| `LT` | 미만 | `[field, number]` | `["peratio.lasttwelvemonths", 20]` |
| `GTE` | 이상 | `[field, number]` | `["intradayprice", 5]` |
| `LTE` | 이하 | `[field, number]` | `["beta", 1.5]` |
| `BTWN` | 범위 | `[field, min, max]` | `["peratio.lasttwelvemonths", 0, 20]` |
| `IS-IN` | 포함 (내부적으로 OR/EQ로 변환) | `[field, value1, value2, ...]` | `["exchange", "NMS", "NYQ"]` |

### 2.3 응답 구조

#### 2.3.1 성공 응답
```
{
  "finance": {
    "result": [{
      "id": "...",
      "title": "...",
      "description": "...",
      "canonicalName": "...",
      "count": 150,
      "quotes": [
        {
          "symbol": "AAPL",
          "shortname": "Apple Inc.",
          "quoteType": "EQUITY",
          "sector": "Technology",
          "industry": "Consumer Electronics",
          "exchange": "NMS",
          "marketCap": 2900000000000,
          "regularMarketPrice": 182.50,
          "regularMarketChange": 2.35,
          "regularMarketChangePercent": 1.30,
          "regularMarketVolume": 55000000,
          // ... 기타 필드들 (요청한 sortField 및 query 필드에 따라 달라짐)
        },
        // ... 더 많은 종목들
      ],
      "start": 0,
      "total": 1500,
      "useRecords": false
    }],
    "error": null
  }
}
```

#### 2.3.2 에러 응답
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

### 2.4 지원하는 필터 필드

#### 2.4.1 Equity 필터 필드 (카테고리별)

##### 공통 필드 (eq_fields)
- `region`: 지역 (us, kr, jp, ...)
- `sector`: 섹터 (Technology, Healthcare, Financial Services, ...)
- `industry`: 산업 (Software, Pharmaceuticals, Banks, ...)
- `peer_group`: 동종 그룹
- `exchange`: 거래소 (NMS, NYQ, ASE, ...)

##### 가격 관련 (price)
- `intradaymarketcap`: 장중 시가총액
- `intradayprice`: 장중 가격
- `percentchange`: 등락률
- `lastclosemarketcap.lasttwelvemonths`: 직전 종가 기준 시가총액
- `lastclose52weekhigh.lasttwelvemonths`: 52주 최고가 대비 종가
- `lastclose52weeklow.lasttwelvemonths`: 52주 최저가 대비 종가
- `fiftytwowkpercentchange`: 52주 변동률
- `eodprice`: 종가
- `intradaypricechange`: 장중 가격 변동

##### 거래 관련 (trading)
- `beta`: 베타 (시장 변동성 대비 종목 변동성)
- `avgdailyvol3m`: 3개월 평균 일일 거래량
- `dayvolume`: 당일 거래량
- `eodvolume`: 종가 기준 거래량
- `pctheldinsider`: 내부자 보유 비율
- `pctheldinst`: 기관 보유 비율

##### 공매도 (short_interest)
- `short_percentage_of_shares_outstanding.value`: 발행주식 대비 공매도 비율
- `short_interest.value`: 공매도 잔고
- `short_percentage_of_float.value`: 유통주식 대비 공매도 비율
- `days_to_cover_short.value`: 공매도 커버 소요 일수
- `short_interest_percentage_change.value`: 공매도 비율 변화

##### 밸류에이션 (valuation)
- `peratio.lasttwelvemonths`: PER (주가수익비율)
- `pegratio_5y`: 5년 PEG 비율
- `pricebookratio.quarterly`: 분기 PBR (주가순자산비율)
- `bookvalueshare.lasttwelvemonths`: 주당 장부가
- `lastclosemarketcaptotalrevenue.lasttwelvemonths`: 시가총액/매출 비율
- `lastclosetevtotalrevenue.lasttwelvemonths`: EV/매출 비율
- `lastclosepricetangiblebookvalue.lasttwelvemonths`: 가격/유형자산 비율
- `lastclosepriceearnings.lasttwelvemonths`: 주가/수익 비율

##### 수익성 (profitability)
- `returnonassets.lasttwelvemonths`: ROA (자산수익률)
- `returnonequity.lasttwelvemonths`: ROE (자기자본수익률)
- `returnontotalcapital.lasttwelvemonths`: 총자본수익률
- `forward_dividend_yield`: 예상 배당수익률
- `forward_dividend_per_share`: 예상 주당 배당금
- `consecutive_years_of_dividend_growth_count`: 연속 배당 증가 연수

##### 레버리지 (leverage)
- `totaldebtequity.lasttwelvemonths`: 부채비율
- `ltdebtequity.lasttwelvemonths`: 장기부채비율
- `netdebtebitda.lasttwelvemonths`: 순부채/EBITDA
- `totaldebtebitda.lasttwelvemonths`: 총부채/EBITDA
- `ebitinterestexpense.lasttwelvemonths`: EBIT/이자비용
- `ebitdainterestexpense.lasttwelvemonths`: EBITDA/이자비용
- `lastclosetevebit.lasttwelvemonths`: TEV/EBIT
- `lastclosetevebitda.lasttwelvemonths`: TEV/EBITDA

##### 유동성 (liquidity)
- `currentratio.lasttwelvemonths`: 유동비율
- `quickratio.lasttwelvemonths`: 당좌비율
- `operatingcashflowtocurrentliabilities.lasttwelvemonths`: 영업현금흐름/유동부채
- `altmanzscoreusingtheaveragestockinformationforaperiod.lasttwelvemonths`: 알트만 Z-Score

##### 손익계산서 (income_statement)
- `totalrevenues.lasttwelvemonths`: 총매출
- `totalrevenues1yrgrowth.lasttwelvemonths`: 매출 1년 성장률
- `quarterlyrevenuegrowth.quarterly`: 분기 매출 성장률
- `netincomeis.lasttwelvemonths`: 순이익
- `netincome1yrgrowth.lasttwelvemonths`: 순이익 1년 성장률
- `netincomemargin.lasttwelvemonths`: 순이익률
- `grossprofit.lasttwelvemonths`: 매출총이익
- `grossprofitmargin.lasttwelvemonths`: 매출총이익률
- `ebitda.lasttwelvemonths`: EBITDA
- `ebitda1yrgrowth.lasttwelvemonths`: EBITDA 1년 성장률
- `ebitdamargin.lasttwelvemonths`: EBITDA 마진
- `ebit.lasttwelvemonths`: EBIT
- `operatingincome.lasttwelvemonths`: 영업이익
- `dilutedepscontinuingoperations.lasttwelvemonths`: 희석 EPS (계속사업)
- `basicepscontinuingoperations.lasttwelvemonths`: 기본 EPS (계속사업)
- `netepsbasic.lasttwelvemonths`: 기본 순EPS
- `netepsdiluted.lasttwelvemonths`: 희석 순EPS
- `epsgrowth.lasttwelvemonths`: EPS 성장률
- `dilutedeps1yrgrowth.lasttwelvemonths`: 희석 EPS 1년 성장률

##### 재무상태표 (balance_sheet)
- `totalassets.lasttwelvemonths`: 총자산
- `totalequity.lasttwelvemonths`: 총자본
- `totaldebt.lasttwelvemonths`: 총부채
- `totalcurrentassets.lasttwelvemonths`: 유동자산
- `totalcurrentliabilities.lasttwelvemonths`: 유동부채
- `totalcashandshortterminvestments.lasttwelvemonths`: 현금 및 단기투자
- `totalcommonequity.lasttwelvemonths`: 보통주자본
- `totalcommonsharesoutstanding.lasttwelvemonths`: 보통주 발행수
- `totalsharesoutstanding`: 총 발행주식수

##### 현금흐름표 (cash_flow)
- `cashfromoperations.lasttwelvemonths`: 영업활동 현금흐름
- `cashfromoperations1yrgrowth.lasttwelvemonths`: 영업 CF 1년 성장률
- `leveredfreecashflow.lasttwelvemonths`: 레버리지드 FCF
- `leveredfreecashflow1yrgrowth.lasttwelvemonths`: 레버리지드 FCF 1년 성장률
- `unleveredfreecashflow.lasttwelvemonths`: 언레버리지드 FCF
- `capitalexpenditure.lasttwelvemonths`: 자본적 지출

##### ESG
- `esg_score`: ESG 종합 점수
- `environmental_score`: 환경 점수
- `social_score`: 사회 점수
- `governance_score`: 지배구조 점수
- `highest_controversy`: 최고 논란 수준

#### 2.4.2 Fund 필터 필드

##### 공통 필드
- `exchange`: 거래소 (NAS)
- `eodprice`: 종가
- `intradaypricechange`: 장중 가격 변동
- `intradayprice`: 장중 가격

##### 펀드 전용 필드
- `categoryname`: 펀드 카테고리 (Large Growth, High Yield Bond, ...)
- `performanceratingoverall`: 전체 성과 등급 (1-5)
- `initialinvestment`: 최소 투자금액
- `annualreturnnavy1categoryrank`: 연간 수익률 카테고리 순위
- `riskratingoverall`: 전체 위험 등급 (1-5)

### 2.5 제약 조건 (Yahoo Limits)
- **최대 결과 수**: 250개 (size/count 파라미터)
- **Predefined Query Offset**: offset 파라미터가 무시됨 (Yahoo API 제한)
- **Rate Limiting**: 초당 요청 수 제한 (기존 UFC Rate Limiter 사용)
- **Crumb 토큰**: 모든 요청에 필요 (기존 YahooAuthenticator 사용)

### 2.6 Predefined Screener 목록

#### 2.6.1 Equity Screeners
| 스크리너 ID | 설명 | 정렬 필드 | 주요 조건 |
|------------|------|----------|---------|
| `aggressive_small_caps` | 공격적 소형주 | eodvolume (DESC) | US 거래소, EPS 성장률 < 15 |
| `day_gainers` | 당일 상승 종목 | percentchange (DESC) | 등락률 > 3%, 시총 > 20억, 가격 > $5 |
| `day_losers` | 당일 하락 종목 | percentchange (ASC) | 등락률 < -2.5%, 시총 > 20억 |
| `most_actives` | 가장 활발한 종목 | dayvolume (DESC) | US, 시총 > 20억, 거래량 > 500만 |
| `most_shorted_stocks` | 공매도 상위 종목 | short_percentage_of_shares_outstanding (DESC) | US, 가격 > $1, 3개월 평균 거래량 > 20만 |
| `growth_technology_stocks` | 성장 테크 주식 | eodvolume (DESC) | 분기 매출 성장 > 25%, EPS 성장 > 25%, 섹터 = Technology |
| `small_cap_gainers` | 소형주 상승 종목 | eodvolume (DESC) | 시총 < 20억, NMS/NYQ |
| `undervalued_growth_stocks` | 저평가 성장주 | eodvolume (DESC) | PER 0-20, PEG < 1, EPS 성장 > 25% |
| `undervalued_large_caps` | 저평가 대형주 | eodvolume (DESC) | PER 0-20, PEG < 1, 시총 100억-1000억 |

#### 2.6.2 Fund Screeners
| 스크리너 ID | 설명 | 정렬 필드 | 주요 조건 |
|------------|------|----------|---------|
| `conservative_foreign_funds` | 보수적 해외 펀드 | fundnetassets (DESC) | 해외 대형주 카테고리, 성과 등급 4-5 |
| `high_yield_bond` | 하이일드 채권 펀드 | fundnetassets (DESC) | 하이일드 카테고리, 성과 등급 4-5 |
| `portfolio_anchors` | 포트폴리오 기반 펀드 | fundnetassets (DESC) | Large Blend, 성과 등급 4-5 |
| `solid_large_growth_funds` | 견실한 대형 성장 펀드 | fundnetassets (DESC) | Large Growth, 성과 등급 4-5 |
| `solid_midcap_growth_funds` | 견실한 중형 성장 펀드 | fundnetassets (DESC) | Mid-Cap Growth, 성과 등급 4-5 |
| `top_mutual_funds` | 상위 뮤추얼펀드 | percentchange (DESC) | 가격 > $15, 성과 등급 4-5 |

---

## 3. UFC 통합 설계

### 3.1 아키텍처 배치

#### 3.1.1 네임스페이스 구조
```
com.ulalax.ufc
├── yahoo
│   ├── YahooClient.kt (기존)
│   ├── model (기존)
│   │   ├── QuoteSummaryModule.kt
│   │   ├── ChartData.kt
│   │   └── screener (신규)
│   │       ├── ScreenerQuery.kt
│   │       ├── ScreenerResult.kt
│   │       ├── ScreenerQuote.kt
│   │       ├── ScreenerOperator.kt
│   │       ├── ScreenerField.kt
│   │       ├── ScreenerSortField.kt
│   │       └── PredefinedScreener.kt
│   └── internal
│       └── response (기존)
│           └── ScreenerResponse.kt (신규)
```

#### 3.1.2 통합 위치
- **YahooClient 클래스**: 새로운 `screener()` 메서드 추가
- **Ufc 클래스**: `screener()` 메서드를 통한 직접 접근 제공 (quoteSummary, chart와 동일 패턴)

### 3.2 클래스 및 역할

#### 3.2.1 도메인 모델 클래스

##### ScreenerQuery (추상 클래스)
- **역할**: Screener 쿼리를 표현하는 기본 추상 클래스
- **주요 메서드**:
  - `toRequestBody(): Map<String, Any>` - Yahoo API 요청 형식으로 변환
  - `validate()` - 쿼리 유효성 검사

##### EquityQuery (ScreenerQuery 상속)
- **역할**: 주식 스크리닝 쿼리
- **필드**:
  - `operator: ScreenerOperator` - 연산자 (AND, OR, EQ, GT, ...)
  - `operands: List<Any>` - 피연산자 (쿼리 또는 값)
- **유효성 검사**: Equity 필드 및 값 검증

##### FundQuery (ScreenerQuery 상속)
- **역할**: 펀드 스크리닝 쿼리
- **필드**: EquityQuery와 동일
- **유효성 검사**: Fund 필드 및 값 검증

##### ScreenerOperator (Enum)
- **역할**: 쿼리 연산자 열거
- **값**: `AND`, `OR`, `EQ`, `GT`, `LT`, `GTE`, `LTE`, `BTWN`, `IS_IN`
- **메서드**: `toApiValue(): String` - API 문자열로 변환

##### ScreenerField (Sealed Interface)
- **역할**: 필터링 가능한 필드 정의
- **하위 클래스**:
  - `EquityField` - Equity 필드 열거
  - `FundField` - Fund 필드 열거
- **속성**:
  - `apiValue: String` - API 필드명
  - `category: String` - 필드 카테고리 (price, trading, valuation, ...)
  - `valueType: FieldValueType` - 값 타입 (NUMBER, STRING, ENUM)

##### ScreenerSortField (Enum)
- **역할**: 정렬 가능한 필드
- **주요 값**: `TICKER`, `PERCENT_CHANGE`, `DAY_VOLUME`, `MARKET_CAP`, `PE_RATIO`, ...
- **속성**: `apiValue: String`

##### PredefinedScreener (Enum)
- **역할**: Yahoo가 제공하는 사전 정의 스크리너
- **주요 값**: `AGGRESSIVE_SMALL_CAPS`, `DAY_GAINERS`, `MOST_ACTIVES`, ...
- **속성**:
  - `apiId: String` - Yahoo API ID
  - `defaultSortField: ScreenerSortField`
  - `defaultSortAsc: Boolean`

##### ScreenerResult
- **역할**: 스크리닝 결과
- **필드**:
  - `id: String?` - 결과 ID
  - `title: String?` - 제목
  - `description: String?` - 설명
  - `count: Int` - 현재 결과 수
  - `total: Int` - 전체 매칭 종목 수
  - `start: Int` - 시작 오프셋
  - `quotes: List<ScreenerQuote>` - 종목 리스트

##### ScreenerQuote
- **역할**: 스크리닝 결과의 개별 종목
- **필드**:
  - `symbol: String` - 심볼
  - `shortName: String?` - 짧은 이름
  - `longName: String?` - 긴 이름
  - `quoteType: String?` - 유형 (EQUITY, MUTUALFUND)
  - `sector: String?` - 섹터
  - `industry: String?` - 산업
  - `exchange: String?` - 거래소
  - `marketCap: Long?` - 시가총액
  - `regularMarketPrice: Double?` - 정규시장 가격
  - `regularMarketChange: Double?` - 가격 변동
  - `regularMarketChangePercent: Double?` - 등락률
  - `regularMarketVolume: Long?` - 거래량
  - `additionalFields: Map<String, Any?>` - 추가 필드 (정렬 필드 등)

#### 3.2.2 내부 응답 모델 (internal.response)

##### ScreenerResponse
- **역할**: Yahoo API Raw 응답 파싱
- **필드**:
  - `finance: FinanceContainer`

##### FinanceContainer
- **필드**:
  - `result: List<ScreenerApiResult>?`
  - `error: ScreenerError?`

##### ScreenerApiResult
- **필드**:
  - `id: String?`
  - `title: String?`
  - `description: String?`
  - `count: Int`
  - `total: Int`
  - `start: Int`
  - `quotes: List<Map<String, Any>>` - 동적 필드 지원

##### ScreenerError
- **필드**:
  - `code: String`
  - `description: String`

#### 3.2.3 YahooClient 확장

##### 새로운 메서드 시그니처

```kotlin
// 1. Custom Query 실행
suspend fun screener(
    query: ScreenerQuery,
    sortField: ScreenerSortField = ScreenerSortField.TICKER,
    sortAsc: Boolean = false,
    size: Int = 100,
    offset: Int = 0
): ScreenerResult

// 2. Predefined Query 실행 (String ID)
suspend fun screener(
    predefinedId: String,
    count: Int = 25,
    sortField: ScreenerSortField? = null,
    sortAsc: Boolean? = null
): ScreenerResult

// 3. Predefined Query 실행 (Enum)
suspend fun screener(
    predefined: PredefinedScreener,
    count: Int = 25,
    sortField: ScreenerSortField? = null,
    sortAsc: Boolean? = null
): ScreenerResult
```

##### 구현 흐름
1. 파라미터 유효성 검사 (size/count <= 250)
2. Rate Limiter 획득 (`rateLimiter.acquire()`)
3. Crumb 토큰 획득 (`authenticator.getCrumb()`)
4. 쿼리 타입에 따라 분기:
   - Custom Query: POST 요청, Body에 쿼리 포함
   - Predefined Query: GET 요청, Query Parameter로 전달
5. HTTP 요청 실행
6. 응답 파싱 (`ScreenerResponse`)
7. 에러 확인
8. 도메인 모델 변환 (`ScreenerResult`)
9. 반환

### 3.3 Query Builder 패턴 설계

#### 3.3.1 기본 설계 원칙
- **Immutable Query**: 쿼리 객체는 불변
- **Type-Safe**: Kotlin sealed class 및 enum으로 타입 안전성 보장
- **Fluent API**: 메서드 체이닝 지원 (선택 사항)
- **Validation**: 쿼리 생성 시 유효성 검사

#### 3.3.2 Query 생성 방법

##### 방법 1: 직접 생성자 사용 (yfinance 스타일)
```kotlin
EquityQuery(
    operator = ScreenerOperator.AND,
    operands = listOf(
        EquityQuery(ScreenerOperator.GT, listOf("percentchange", 3)),
        EquityQuery(ScreenerOperator.EQ, listOf("region", "us"))
    )
)
```

##### 방법 2: DSL 스타일 (권장)
```kotlin
// 향후 확장 가능성을 위해 DSL 빌더 고려
// 1차 구현에서는 방법 1로 시작
```

#### 3.3.3 Query 유효성 검사

##### 검사 항목
1. **연산자 유효성**:
   - AND/OR: operands가 List<ScreenerQuery>인지 확인, 길이 >= 2
   - EQ/IS_IN: 필드가 유효 값 목록에 존재하는지 확인
   - GT/LT/GTE/LTE: operands[1]이 숫자인지 확인
   - BTWN: operands[1], operands[2]가 숫자인지 확인

2. **필드 유효성**:
   - EquityQuery: EquityField에 정의된 필드만 허용
   - FundQuery: FundField에 정의된 필드만 허용

3. **값 유효성**:
   - sector, industry, exchange 등: 허용된 값 목록 확인
   - 숫자 필드: 숫자 타입 확인

##### 에러 처리
- 유효성 검사 실패 시 `IllegalArgumentException` 발생
- 에러 메시지에 구체적인 실패 이유 포함

---

## 4. 데이터 매핑

### 4.1 Yahoo 응답 → UFC 도메인 모델

#### 4.1.1 ScreenerApiResult → ScreenerResult
| Yahoo 필드 | UFC 필드 | 타입 | 비고 |
|-----------|---------|------|------|
| `id` | `id` | String? | 결과 ID |
| `title` | `title` | String? | 제목 |
| `description` | `description` | String? | 설명 |
| `count` | `count` | Int | 현재 결과 개수 |
| `total` | `total` | Int | 전체 개수 |
| `start` | `start` | Int | 시작 오프셋 |
| `quotes` | `quotes` | List<ScreenerQuote> | 변환 필요 |

#### 4.1.2 Quote Map → ScreenerQuote

##### 기본 필드 매핑
| Yahoo 필드 | UFC 필드 | 타입 | Nullable | 비고 |
|-----------|---------|------|----------|------|
| `symbol` | `symbol` | String | No | 필수 |
| `shortname` | `shortName` | String | Yes | |
| `longname` | `longName` | String | Yes | |
| `quoteType` | `quoteType` | String | Yes | EQUITY, MUTUALFUND |
| `sector` | `sector` | String | Yes | |
| `industry` | `industry` | String | Yes | |
| `exchange` | `exchange` | String | Yes | |
| `marketCap` | `marketCap` | Long | Yes | |
| `regularMarketPrice` | `regularMarketPrice` | Double | Yes | |
| `regularMarketChange` | `regularMarketChange` | Double | Yes | |
| `regularMarketChangePercent` | `regularMarketChangePercent` | Double | Yes | |
| `regularMarketVolume` | `regularMarketVolume` | Long | Yes | |

##### 추가 필드 처리
- Yahoo 응답의 모든 기타 필드는 `additionalFields: Map<String, Any?>`에 저장
- 정렬 필드, 사용자 요청 필드 등 동적 필드 지원

### 4.2 UFC Query → Yahoo Request Body

#### 4.2.1 EquityQuery/FundQuery → Query JSON
```kotlin
// EquityQuery 예시
EquityQuery(
    ScreenerOperator.AND,
    listOf(
        EquityQuery(ScreenerOperator.GT, listOf("percentchange", 3)),
        EquityQuery(ScreenerOperator.EQ, listOf("region", "us"))
    )
)

// 변환 결과
{
  "operator": "AND",
  "operands": [
    {
      "operator": "GT",
      "operands": ["percentchange", 3]
    },
    {
      "operator": "EQ",
      "operands": ["region", "us"]
    }
  ]
}
```

#### 4.2.2 IS_IN 연산자 확장
- `IS_IN`은 Yahoo API에서 지원하지 않으므로 클라이언트에서 `OR` + `EQ`로 변환
```kotlin
// 입력
EquityQuery(ScreenerOperator.IS_IN, listOf("exchange", "NMS", "NYQ"))

// 변환 결과
{
  "operator": "OR",
  "operands": [
    {"operator": "EQ", "operands": ["exchange", "NMS"]},
    {"operator": "EQ", "operands": ["exchange", "NYQ"]}
  ]
}
```

### 4.3 타입 변환 규칙

#### 4.3.1 숫자 타입
- Yahoo 응답: `Number` (Int, Long, Double 혼재)
- UFC 모델:
  - 시가총액, 거래량 → `Long`
  - 가격, 비율, 지표 → `Double`
  - 카운트, 순위 → `Int`

#### 4.3.2 문자열 타입
- 대소문자 정규화: Yahoo는 대소문자를 구분하지 않음
- Trim 처리: 앞뒤 공백 제거

#### 4.3.3 Boolean 타입
- Yahoo 응답에 Boolean 값이 있을 경우 Kotlin Boolean으로 변환

### 4.4 Nullable 처리 전략

#### 4.4.1 필수 필드
- `ScreenerQuote.symbol`: 절대 null 불가 (파싱 실패 시 예외 발생)
- `ScreenerResult.count`, `total`, `start`: 기본값 0

#### 4.4.2 선택 필드
- 모든 재무 지표, 가격 정보: Nullable
- Yahoo API가 값을 제공하지 않으면 null 저장
- 클라이언트는 null 체크 후 사용

#### 4.4.3 컬렉션 필드
- `quotes`: 빈 리스트로 초기화 (null이 아닌 emptyList())
- `additionalFields`: 빈 맵으로 초기화 (emptyMap())

---

## 5. 에러 처리

### 5.1 예상 에러 케이스

#### 5.1.1 클라이언트 에러
| 에러 케이스 | ErrorCode | HTTP Status | 설명 |
|-----------|-----------|-------------|------|
| 쿼리 유효성 검사 실패 | `INVALID_PARAMETER` | - | 잘못된 필드, 연산자, 값 |
| size/count > 250 | `INVALID_PARAMETER` | - | Yahoo 제한 초과 |
| 지원하지 않는 필드 | `INVALID_PARAMETER` | - | EquityField/FundField에 없는 필드 |
| 지원하지 않는 값 | `INVALID_PARAMETER` | - | sector, exchange 등 허용 값 목록 외 |

#### 5.1.2 API 에러
| 에러 케이스 | ErrorCode | HTTP Status | 설명 |
|-----------|-----------|-------------|------|
| 인증 실패 | `AUTHENTICATION_FAILED` | 401 | Crumb 토큰 만료/무효 |
| Rate Limit 초과 | `RATE_LIMIT_EXCEEDED` | 429 | Yahoo Rate Limit |
| 잘못된 쿼리 | `EXTERNAL_API_ERROR` | 400 | Yahoo가 쿼리 거부 |
| 네트워크 오류 | `NETWORK_ERROR` | - | 타임아웃, 연결 실패 |
| 서버 오류 | `EXTERNAL_API_ERROR` | 500 | Yahoo 서버 오류 |

#### 5.1.3 데이터 에러
| 에러 케이스 | ErrorCode | HTTP Status | 설명 |
|-----------|-----------|-------------|------|
| 결과 없음 | (정상) | 200 | 매칭 종목 0개, `quotes: []` 반환 |
| 파싱 실패 | `DATA_PARSING_ERROR` | 200 | 응답 JSON 구조 불일치 |

### 5.2 에러 응답 구조

#### 5.2.1 Yahoo API 에러 응답
```json
{
  "finance": {
    "result": null,
    "error": {
      "code": "Bad Request",
      "description": "Invalid query structure"
    }
  }
}
```

#### 5.2.2 UFC Exception 변환
```kotlin
// Yahoo 에러 → UfcException
if (response.finance.error != null) {
    throw ApiException(
        errorCode = ErrorCode.EXTERNAL_API_ERROR,
        message = "Screener API 에러: ${response.finance.error.description}",
        metadata = mapOf(
            "yahooErrorCode" to response.finance.error.code,
            "yahooErrorDescription" to response.finance.error.description
        )
    )
}
```

### 5.3 에러 처리 전략

#### 5.3.1 Retry 전략
- **Rate Limit 초과**: Rate Limiter가 자동으로 대기
- **인증 실패**: Crumb 토큰 재발급 후 1회 재시도
- **네트워크 오류**: 재시도 없음 (클라이언트가 결정)
- **서버 오류 (5xx)**: 재시도 없음

#### 5.3.2 Fallback 전략
- **결과 없음**: 빈 리스트 반환 (`quotes = emptyList()`)
- **일부 필드 누락**: 해당 필드만 null 처리, 전체 응답은 성공

#### 5.3.3 로깅
- 에러 발생 시 Logger를 통해 상세 로그 기록
- 메타데이터 포함 (쿼리 내용, 파라미터, 응답 상태 등)

---

## 6. 테스트 전략

### 6.1 단위 테스트 시나리오

#### 6.1.1 Query 생성 및 유효성 검사
- **테스트 클래스**: `EquityQueryTest`, `FundQueryTest`
- **테스트 케이스**:
  1. 유효한 AND 쿼리 생성
  2. 유효한 OR 쿼리 생성
  3. GT/LT/GTE/LTE 쿼리 생성
  4. BTWN 쿼리 생성
  5. IS_IN 쿼리 생성 및 OR/EQ 변환 확인
  6. 중첩 쿼리 (AND 안에 OR) 생성
  7. 잘못된 필드로 쿼리 생성 시 예외 발생
  8. 잘못된 값으로 쿼리 생성 시 예외 발생
  9. AND/OR operands < 2 시 예외 발생
  10. GT/LT에 문자열 값 전달 시 예외 발생

#### 6.1.2 Query → Request Body 변환
- **테스트 클래스**: `QuerySerializationTest`
- **테스트 케이스**:
  1. 단순 EQ 쿼리 JSON 변환
  2. GT 쿼리 JSON 변환
  3. AND 쿼리 JSON 변환
  4. 중첩 쿼리 JSON 변환
  5. IS_IN → OR/EQ 변환 확인
  6. quoteType 자동 설정 확인 (EquityQuery → "EQUITY")

#### 6.1.3 응답 파싱
- **테스트 클래스**: `ScreenerResponseParsingTest`
- **테스트 케이스**:
  1. 정상 응답 파싱 (Mock JSON 사용)
  2. 에러 응답 파싱
  3. 빈 결과 응답 파싱 (`quotes: []`)
  4. 일부 필드 누락 응답 파싱 (nullable 처리 확인)
  5. 추가 필드 포함 응답 파싱 (`additionalFields` 확인)
  6. 잘못된 JSON 구조 시 DataParsingException 발생

#### 6.1.4 Enum 및 Constants
- **테스트 클래스**: `ScreenerEnumTest`
- **테스트 케이스**:
  1. ScreenerOperator.toApiValue() 확인
  2. ScreenerSortField.apiValue 확인
  3. PredefinedScreener.apiId 확인
  4. EquityField/FundField 전체 필드 개수 확인

### 6.2 통합 테스트 시나리오

#### 6.2.1 Custom Query 실행
- **테스트 클래스**: `ScreenerCustomQuerySpec`
- **테스트 케이스**:
  1. **단순 GT 쿼리**: 시가총액 > 10억인 종목 검색
     - 검증: `result.quotes.isNotEmpty()`
     - 검증: 모든 종목의 `marketCap > 1_000_000_000`

  2. **AND 쿼리**: US 지역 + 테크놀로지 섹터
     - 검증: 모든 종목의 `region = "us"`, `sector = "Technology"`

  3. **OR 쿼리**: Healthcare OR Financial Services 섹터
     - 검증: 모든 종목의 `sector in ["Healthcare", "Financial Services"]`

  4. **BTWN 쿼리**: PER 10-20 사이
     - 검증: 결과에 PER 필드 존재

  5. **복합 쿼리**: (시총 > 10억 AND 섹터 = Technology) OR (배당수익률 > 5%)
     - 검증: 복합 조건 만족 종목 반환

  6. **정렬 테스트**: percentchange 기준 내림차순
     - 검증: `quotes[0].regularMarketChangePercent >= quotes[1].regularMarketChangePercent`

  7. **페이지네이션**: size=50, offset=0 vs offset=50
     - 검증: 두 결과의 symbol이 겹치지 않음

#### 6.2.2 Predefined Query 실행
- **테스트 클래스**: `ScreenerPredefinedSpec`
- **테스트 케이스**:
  1. **day_gainers 조회**:
     - 검증: `count <= 25`
     - 검증: 모든 종목의 `regularMarketChangePercent > 0`

  2. **most_actives 조회**:
     - 검증: 거래량 순 정렬 확인

  3. **undervalued_growth_stocks 조회**:
     - 검증: 결과 반환 성공

  4. **Predefined + Custom Sort**: aggressive_small_caps를 marketCap 오름차순으로
     - 검증: 커스텀 정렬 적용 확인

  5. **count=100 설정**:
     - 검증: `result.count <= 100`

  6. **존재하지 않는 Predefined ID**:
     - 검증: ApiException 발생

#### 6.2.3 Fund Query 실행
- **테스트 클래스**: `ScreenerFundQuerySpec`
- **테스트 케이스**:
  1. **FundQuery 실행**: categoryname = "Large Growth"
     - 검증: `quoteType = "MUTUALFUND"`

  2. **Predefined Fund Screener**: high_yield_bond
     - 검증: 결과 반환 성공

#### 6.2.4 에러 처리
- **테스트 클래스**: `ScreenerErrorHandlingSpec`
- **테스트 케이스**:
  1. **size > 250 에러**:
     - 검증: IllegalArgumentException 발생

  2. **잘못된 필드 쿼리**:
     - 검증: IllegalArgumentException 발생

  3. **네트워크 타임아웃 시뮬레이션** (Mock):
     - 검증: NetworkException 발생

  4. **Yahoo API 에러 응답 처리**:
     - 검증: ApiException 발생, 메타데이터 포함

#### 6.2.5 Rate Limiting
- **테스트 클래스**: `ScreenerRateLimitSpec`
- **테스트 케이스**:
  1. **연속 요청 시 Rate Limit 적용**:
     - 10회 연속 호출, 시간 측정
     - 검증: 초당 요청 수가 설정값 이하

#### 6.2.6 실제 사용 시나리오
- **테스트 클래스**: `ScreenerRealWorldSpec`
- **테스트 케이스**:
  1. **저평가 고배당 종목 검색**:
     - 쿼리: PER < 15 AND 배당수익률 > 3% AND 시총 > 10억
     - 검증: 결과에서 무작위 종목 선택, quoteSummary로 상세 정보 조회

  2. **성장주 검색 → 차트 조회**:
     - 쿼리: EPS 성장률 > 20% AND 섹터 = Technology
     - 검증: 결과 종목의 1년 차트 조회 성공

  3. **펀드 검색 → 펀드 정보 조회**:
     - Predefined: solid_large_growth_funds
     - 검증: 첫 종목의 topHoldings 조회 성공

### 6.3 테스트 데이터 전략

#### 6.3.1 Mock 데이터
- `src/test/resources/screener/` 경로에 Mock JSON 저장
- 파일명: `screener_day_gainers.json`, `screener_equity_query.json`, ...
- 실제 Yahoo 응답 구조 기반

#### 6.3.2 Fixture 심볼
- `TestFixtures.Symbols` 확장:
  - `AAPL`, `MSFT`, `GOOGL` (기존)
  - `SPY`, `QQQ` (ETF - Predefined 테스트용)

#### 6.3.3 Response Recording
- 통합 테스트 실행 시 실제 응답 녹화 (기존 RecordingConfig 활용)
- 향후 Mock 데이터로 전환 가능

---

## 7. 구현 우선순위

### 7.1 Phase 1: 기본 구조 (필수)
1. 도메인 모델 클래스 생성
   - ScreenerOperator
   - ScreenerField (EquityField, FundField)
   - ScreenerSortField
   - ScreenerQuery (EquityQuery, FundQuery)
   - ScreenerResult, ScreenerQuote

2. 내부 응답 모델
   - ScreenerResponse
   - 관련 data class들

3. YahooClient.screener() 메서드
   - Custom Query 실행
   - 기본 에러 처리

4. 단위 테스트
   - Query 생성 및 유효성 검사
   - 응답 파싱

### 7.2 Phase 2: Predefined Screener (권장)
1. PredefinedScreener Enum 추가
2. YahooClient.screener() 오버로드 메서드
3. 통합 테스트: Predefined Query

### 7.3 Phase 3: 고급 기능 (선택)
1. IS_IN 연산자 자동 변환
2. EquityField/FundField 전체 목록 완성 (100+ 필드)
3. 필드별 유효 값 목록 검증
4. DSL 스타일 Query Builder (향후 고려)

### 7.4 Phase 4: 문서화 및 최적화
1. API 사용 예제 추가 (README)
2. KDoc 문서화
3. 성능 최적화 (필요시)

---

## 8. 제한 사항 및 고려 사항

### 8.1 Yahoo Finance API 제한
- **비공식 API**: Yahoo Finance는 공식 Screener API를 제공하지 않음
- **구조 변경 가능성**: Yahoo가 응답 구조를 예고 없이 변경할 수 있음
- **Rate Limiting**: 과도한 요청 시 일시적 차단 가능
- **Predefined Offset 무시**: Predefined Query는 offset 파라미터를 무시함

### 8.2 데이터 정확성
- **실시간 데이터 아님**: 장중 데이터는 15-20분 지연 가능
- **필드 가용성**: 모든 종목이 모든 필드를 제공하지 않음 (null 처리 필수)
- **통화 단위**: 시가총액, 가격 등은 종목의 기본 통화 기준

### 8.3 성능 고려
- **대량 결과**: 최대 250개 제한으로 전체 시장 스캔 불가
- **페이지네이션**: offset을 이용한 다중 요청 필요 (Custom Query만 가능)
- **복합 쿼리**: 중첩이 깊을수록 Yahoo API 처리 시간 증가 가능

### 8.4 보안 및 사용 정책
- **상업적 사용**: Yahoo Finance 이용약관 확인 필요
- **Crumb 토큰**: 기존 YahooAuthenticator 재사용 (보안 유지)
- **개인정보**: 사용자 정보는 요청에 포함되지 않음 (userId는 빈 문자열)

---

## 9. 참조 자료

### 9.1 내부 참조
- UFC 기존 구조: `/home/ulalax/project/kairos/ufc/src/main/kotlin/com/ulalax/ufc/`
- YahooClient 구현: `yahoo/YahooClient.kt`
- 통합 테스트 예시: `src/test/kotlin/com/ulalax/ufc/integration/yahoo/QuoteSummarySpec.kt`

### 9.2 외부 참조
- yfinance screener 구현: `/home/ulalax/project/kairos/yfinance/yfinance/screener/screener.py`
- yfinance query 구현: `/home/ulalax/project/kairos/yfinance/yfinance/screener/query.py`
- Yahoo Finance 웹사이트: https://finance.yahoo.com/screener

### 9.3 관련 문서
- UFC README: `/home/ulalax/project/kairos/ufc/README.md`
- Kotlin Serialization: https://kotlinlang.org/docs/serialization.html
- Ktor HTTP Client: https://ktor.io/docs/client.html

---

## 10. 용어 정의

| 용어 | 설명 |
|-----|------|
| Screener | 특정 조건에 맞는 종목을 검색하는 도구 |
| Query | 검색 조건을 표현하는 논리적 구조 |
| Operand | 연산자의 피연산자 (필드, 값, 또는 하위 쿼리) |
| Predefined Screener | Yahoo가 미리 정의한 인기 검색 조건 |
| Custom Query | 사용자가 직접 정의한 검색 조건 |
| Crumb | Yahoo Finance API 인증 토큰 |
| Quote | 종목 (주식, 펀드 등) |
| QuoteType | 종목 유형 (EQUITY, MUTUALFUND, ETF, ...) |
| Equity | 주식 |
| Fund | 펀드 (뮤추얼펀드, ETF 포함) |
| sortField | 결과 정렬 기준 필드 |
| sortType | 정렬 방향 (ASC: 오름차순, DESC: 내림차순) |
| offset | 결과의 시작 위치 (페이지네이션) |
| size/count | 반환할 결과 개수 |

---

## 부록: Predefined Screener 상세

### A.1 Equity Screeners

#### A.1.1 aggressive_small_caps
- **설명**: 공격적 투자 성향의 소형주
- **주요 조건**:
  - 거래소: NMS 또는 NYQ
  - EPS 성장률 < 15%
- **정렬**: eodvolume DESC

#### A.1.2 day_gainers
- **설명**: 당일 상승률 상위 종목
- **주요 조건**:
  - 등락률 > 3%
  - 지역: us
  - 시가총액 >= 20억
  - 가격 >= $5
  - 거래량 > 15,000
- **정렬**: percentchange DESC

#### A.1.3 day_losers
- **설명**: 당일 하락률 상위 종목
- **주요 조건**:
  - 등락률 < -2.5%
  - 지역: us
  - 시가총액 >= 20억
  - 가격 >= $5
  - 거래량 > 20,000
- **정렬**: percentchange ASC

#### A.1.4 most_actives
- **설명**: 거래량 상위 종목
- **주요 조건**:
  - 지역: us
  - 시가총액 >= 20억
  - 거래량 > 500만
- **정렬**: dayvolume DESC

#### A.1.5 most_shorted_stocks
- **설명**: 공매도 비율 상위 종목
- **주요 조건**:
  - 지역: us
  - 가격 > $1
  - 3개월 평균 거래량 > 20만
- **정렬**: short_percentage_of_shares_outstanding DESC

#### A.1.6 growth_technology_stocks
- **설명**: 성장하는 기술주
- **주요 조건**:
  - 분기 매출 성장률 >= 25%
  - EPS 성장률 >= 25%
  - 섹터: Technology
  - 거래소: NMS 또는 NYQ
- **정렬**: eodvolume DESC

#### A.1.7 small_cap_gainers
- **설명**: 소형주 상승 종목
- **주요 조건**:
  - 시가총액 < 20억
  - 거래소: NMS 또는 NYQ
- **정렬**: eodvolume DESC

#### A.1.8 undervalued_growth_stocks
- **설명**: 저평가된 성장주
- **주요 조건**:
  - PER: 0-20
  - PEG < 1
  - EPS 성장률 >= 25%
  - 거래소: NMS 또는 NYQ
- **정렬**: eodvolume DESC

#### A.1.9 undervalued_large_caps
- **설명**: 저평가된 대형주
- **주요 조건**:
  - PER: 0-20
  - PEG < 1
  - 시가총액: 100억-1000억
  - 거래소: NMS 또는 NYQ
- **정렬**: eodvolume DESC

### A.2 Fund Screeners

#### A.2.1 conservative_foreign_funds
- **설명**: 보수적 해외 펀드
- **주요 조건**:
  - 카테고리: Foreign Large Value/Blend/Growth, Foreign Small/Mid Growth/Blend/Value
  - 전체 성과 등급: 4 또는 5
  - 최소 투자금액 < $100,001
  - 연간 수익률 카테고리 순위 < 50
  - 위험 등급: 1, 2, 3
  - 거래소: NAS
- **정렬**: fundnetassets DESC

#### A.2.2 high_yield_bond
- **설명**: 하이일드 채권 펀드
- **주요 조건**:
  - 성과 등급: 4 또는 5
  - 최소 투자금액 < $100,001
  - 연간 수익률 순위 < 50
  - 위험 등급: 1, 2, 3
  - 카테고리: High Yield Bond
  - 거래소: NAS
- **정렬**: fundnetassets DESC

#### A.2.3 portfolio_anchors
- **설명**: 포트폴리오 기반 펀드
- **주요 조건**:
  - 카테고리: Large Blend
  - 성과 등급: 4 또는 5
  - 최소 투자금액 < $100,001
  - 연간 수익률 순위 < 50
  - 거래소: NAS
- **정렬**: fundnetassets DESC

#### A.2.4 solid_large_growth_funds
- **설명**: 견실한 대형 성장 펀드
- **주요 조건**:
  - 카테고리: Large Growth
  - 성과 등급: 4 또는 5
  - 최소 투자금액 < $100,001
  - 연간 수익률 순위 < 50
  - 거래소: NAS
- **정렬**: fundnetassets DESC

#### A.2.5 solid_midcap_growth_funds
- **설명**: 견실한 중형 성장 펀드
- **주요 조건**:
  - 카테고리: Mid-Cap Growth
  - 성과 등급: 4 또는 5
  - 최소 투자금액 < $100,001
  - 연간 수익률 순위 < 50
  - 거래소: NAS
- **정렬**: fundnetassets DESC

#### A.2.6 top_mutual_funds
- **설명**: 상위 뮤추얼펀드
- **주요 조건**:
  - 가격 > $15
  - 성과 등급: 4 또는 5
  - 최소 투자금액 > $1,000
  - 거래소: NAS
- **정렬**: percentchange DESC

---

**문서 끝**
