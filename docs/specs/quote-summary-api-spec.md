# QuoteSummary API 기술 명세서

> **Version**: 1.0
> **작성일**: 2025-12-05

---

## 1. 개요

### 1.1 API 목적

Yahoo Finance QuoteSummary API를 통해 특정 심볼의 **상세 금융 정보**를 모듈별로 조회한다. 37개의 모듈을 지원하며, 필요한 모듈만 선택적으로 조회 가능하다.

| 항목 | 값 |
|-----|---|
| 엔드포인트 | `GET https://query2.finance.yahoo.com/v10/finance/quoteSummary/{symbol}` |
| HTTP 메서드 | GET |
| 인증 | CRUMB 토큰 필요 |
| 다중 심볼 | 미지원 (한 번에 하나의 심볼만) |
| 모듈 선택 | 필수 (1개 이상의 모듈 지정) |

### 1.2 Quote API와의 차이점

| 구분 | QuoteSummary API | Quote API |
|------|-----------------|-----------|
| 목적 | 상세 모듈별 금융 정보 | 실시간 기본 시장 데이터 |
| 다중 심볼 | 미지원 | 지원 (쉼표 구분) |
| 모듈 선택 | 가능 (37개 모듈) | 불가능 (고정 응답) |
| 응답 속도 | 상대적으로 느림 | 빠름 |
| 데이터 깊이 | 깊고 상세함 | 넓고 얕음 |
| 사용 사례 | 종합 분석, 상세 리포트 | 실시간 모니터링, 간단한 조회 |

### 1.3 제공 데이터

QuoteSummary API는 37개의 모듈을 통해 다양한 카테고리의 데이터를 제공한다.

| 카테고리 | 모듈 | 주요 데이터 |
|---------|------|----------|
| 기본 정보 | PRICE, QUOTE_TYPE, ASSET_PROFILE, SUMMARY_PROFILE | 가격, 심볼, 회사 정보, 섹터, 산업 |
| 핵심 통계 | SUMMARY_DETAIL, DEFAULT_KEY_STATISTICS, FINANCIAL_DATA | 시가총액, 배당, PER, ROE, 목표가 |
| 실적 관련 | CALENDAR_EVENTS, EARNINGS, EARNINGS_HISTORY, EARNINGS_TREND, EARNINGS_DATES | 실적 발표일, EPS, 추정치 대비 실적 |
| 보유자 정보 | MAJOR_HOLDERS, INSIDER_HOLDERS, INSIDER_TRANSACTIONS, INSTITUTION_OWNERSHIP, FUND_OWNERSHIP | 주요 주주, 내부자 거래, 기관 보유 |
| 애널리스트 | RECOMMENDATION_TREND, UPGRADE_DOWNGRADE_HISTORY | 매수/매도 추천, 등급 변경 이력 |
| 재무제표 (연간) | INCOME_STATEMENT_HISTORY, BALANCE_SHEET_HISTORY, CASH_FLOW_STATEMENT_HISTORY | 손익계산서, 대차대조표, 현금흐름표 |
| 재무제표 (분기) | INCOME_STATEMENT_HISTORY_QUARTERLY, BALANCE_SHEET_HISTORY_QUARTERLY, CASH_FLOW_STATEMENT_HISTORY_QUARTERLY | 분기별 재무제표 |
| 펀드 관련 | TOP_HOLDINGS, FUND_PROFILE, FUND_PERFORMANCE | ETF/펀드 보유 종목, 수수료, 수익률 |
| 기타 | SEC_FILINGS, PRICE_HISTORY, ESG_SCORES, PAGE_VIEWS | SEC 제출 서류, 가격 이력, ESG 점수 |

---

## 2. 데이터 소스 분석

### 2.1 요청 파라미터

| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---------|------|-----|------|------|
| symbol | String (Path) | Yes | 조회할 심볼 (URL 경로) | "AAPL" |
| modules | String (Query) | Yes | 쉼표로 구분된 모듈 목록 | "price,summaryDetail,financialData" |
| crumb | String (Query) | Yes | CRUMB 인증 토큰 | "dFhd8fj..." |
| formatted | String (Query) | No | 포맷팅 여부 | "false" (기본값: "true") |

### 2.2 응답 구조

#### 정상 응답

| 경로 | 타입 | 설명 |
|-----|------|------|
| quoteSummary.result | Array | QuoteSummary 결과 배열 (길이 1) |
| quoteSummary.result[0] | Object | 각 모듈별 데이터 포함 |
| quoteSummary.result[0].price | Object? | PRICE 모듈 데이터 (요청 시) |
| quoteSummary.result[0].summaryDetail | Object? | SUMMARY_DETAIL 모듈 데이터 (요청 시) |
| quoteSummary.result[0].{moduleName} | Object? | 각 모듈별 데이터 |
| quoteSummary.error | null | 정상 시 null |

#### 에러 응답

| 경로 | 타입 | 설명 |
|-----|------|------|
| quoteSummary.result | null 또는 빈 Array | 오류 시 null 또는 [] |
| quoteSummary.error.code | String | 에러 코드 |
| quoteSummary.error.description | String | 에러 설명 |

### 2.3 지원 모듈 목록 (37개)

#### 기본 정보 모듈 (5개)

| 모듈 | API 값 | 설명 | 주요 필드 |
|-----|--------|------|----------|
| PRICE | price | 기본 가격 정보 | symbol, longName, currency, exchange, regularMarketPrice, fiftyTwoWeekHigh/Low |
| SUMMARY_DETAIL | summaryDetail | 상세 요약 정보 | dividendRate, dividendYield, beta, trailingPE, forwardPE, marketCap, volume |
| SUMMARY_PROFILE | summaryProfile | 요약 프로필 | sector, industry, website, address, city, country, phone |
| ASSET_PROFILE | assetProfile | 자산 프로필 (상세) | longBusinessSummary, fullTimeEmployees, sector, industry, website |
| QUOTE_TYPE | quoteType | 자산 타입 | quoteType (EQUITY/ETF/MUTUALFUND), exchange, symbol, market |

#### 핵심 통계 모듈 (2개)

| 모듈 | API 값 | 설명 | 주요 필드 |
|-----|--------|------|----------|
| DEFAULT_KEY_STATISTICS | defaultKeyStatistics | 기본 주요 통계 | sharesOutstanding, isin, cusip |
| FINANCIAL_DATA | financialData | 재무 데이터 | currentRatio, returnOnEquity, returnOnAssets, totalCash, totalDebt, targetPriceHigh/Low/Mean, recommendationKey |

#### 실적 관련 모듈 (5개)

| 모듈 | API 값 | 설명 | 주요 필드 |
|-----|--------|------|----------|
| CALENDAR_EVENTS | calendarEvents | 캘린더 이벤트 | earnings (다음 실적 발표일), dividendDate |
| EARNINGS | earnings | 실적 연혁 | earningsChart (분기별/연도별 실적), financialsChart |
| EARNINGS_HISTORY | earningsHistory | 실적 이력 | history (과거 실적 발표 내역) |
| EARNINGS_TREND | earningsTrend | 실적 추이 | trend (추정 EPS, 실제 EPS, 차이, 서프라이즈) |
| EARNINGS_DATES | earningsDates | 실적 발표 날짜 | earningsDate (예정일 배열), earningsAverage/Low/High |

#### 보유자 정보 모듈 (6개)

| 모듈 | API 값 | 설명 | 주요 필드 |
|-----|--------|------|----------|
| MAJOR_HOLDERS | majorHolders | 주요 주주 | holders (기관명, 보유 비율) |
| INSIDER_HOLDERS | insiderHolders | 내부자 보유 정보 | holders (내부자별 보유 지분) |
| INSIDER_TRANSACTIONS | insiderTransactions | 내부자 거래 | transactions (거래자명, 관계, 거래일, 거래량, 가격) |
| INSTITUTION_OWNERSHIP | institutionOwnership | 기관 투자자 보유 | ownershipList (기관명, 보유량, 보유 비율) |
| FUND_OWNERSHIP | fundOwnership | 펀드 보유 정보 | ownershipList (펀드명, 보유량, 비율) |
| NET_SHARE_PURCHASE_ACTIVITY | netSharePurchaseActivity | 순자산 매수 활동 | 내부자 순매수/매도 데이터 |

#### 애널리스트 모듈 (2개)

| 모듈 | API 값 | 설명 | 주요 필드 |
|-----|--------|------|----------|
| RECOMMENDATION_TREND | recommendationTrend | 추천 동향 | trend (strongBuy, buy, hold, sell, strongSell 수) |
| UPGRADE_DOWNGRADE_HISTORY | upgradeDowngradeHistory | 등급 변경 이력 | history (날짜, 기관, 이전/변경 등급) |

#### 재무제표 모듈 - 연간 (3개)

| 모듈 | API 값 | 설명 | 주요 필드 |
|-----|--------|------|----------|
| INCOME_STATEMENT_HISTORY | incomeStatementHistory | 손익계산서 (연간) | incomeStatementHistory (totalRevenue, netIncome, ebitda) |
| BALANCE_SHEET_HISTORY | balanceSheetHistory | 대차대조표 (연간) | balanceSheetStatements (totalAssets, totalLiabilities, totalStockholderEquity) |
| CASHFLOW_STATEMENT_HISTORY | cashflowStatementHistory | 현금흐름표 (연간) | cashflowStatements (operatingCashflow, investingCashflow, financingCashflow) |

#### 재무제표 모듈 - 분기 (3개)

| 모듈 | API 값 | 설명 | 주요 필드 |
|-----|--------|------|----------|
| INCOME_STATEMENT_HISTORY_QUARTERLY | incomeStatementHistoryQuarterly | 손익계산서 (분기) | incomeStatementHistory (분기별 데이터) |
| BALANCE_SHEET_HISTORY_QUARTERLY | balanceSheetHistoryQuarterly | 대차대조표 (분기) | balanceSheetStatements (분기별 데이터) |
| CASHFLOW_STATEMENT_HISTORY_QUARTERLY | cashflowStatementHistoryQuarterly | 현금흐름표 (분기) | cashflowStatements (분기별 데이터) |

#### 펀드 관련 모듈 (3개)

| 모듈 | API 값 | 설명 | 주요 필드 |
|-----|--------|------|----------|
| TOP_HOLDINGS | topHoldings | 상위 보유 종목 | holdings (symbol, name, holdingPercent), equityHoldings, bondHoldings, sectorWeightings |
| FUND_PROFILE | fundProfile | 펀드 프로필 | categoryName, family, legalType, feesExpensesInvestment |
| FUND_PERFORMANCE | fundPerformance | 펀드 성과 | 수익률, 성과 지표 |

#### 기타 모듈 (8개)

| 모듈 | API 값 | 설명 | 주요 필드 |
|-----|--------|------|----------|
| SEC_FILINGS | secFilings | SEC 제출 서류 | 주식 분할 및 배당금 이력 |
| PRICE_HISTORY | priceHistory | 가격 이력 | 과거 가격 데이터 |
| INDEX_TREND | indexTrend | 지수 추세 | 시장 지수 관련 추세 정보 |
| INDUSTRY_TREND | industryTrend | 산업 추세 | 업종별 추세 정보 |
| SECTOR_TREND | sectorTrend | 섹터 추세 | 섹터별 추세 정보 |
| PAGE_VIEWS | pageViews | 페이지 뷰 | 심볼 조회 통계 |
| ESG_SCORES | esgScores | ESG 점수 | 환경(E), 사회(S), 거버넌스(G) 평가 점수 |
| NET_SHARE_PURCHASE_ACTIVITY | netSharePurchaseActivity | 순자산 가치 | ETF/펀드 NAV 정보 |

### 2.4 자산 유형별 모듈 제공 여부

| 모듈 그룹 | EQUITY | ETF | MUTUALFUND | INDEX | CRYPTOCURRENCY |
|---------|--------|-----|------------|-------|----------------|
| 기본 정보 | Yes | Yes | Yes | Yes | Yes |
| 핵심 통계 | Yes | Partial | Partial | Partial | Partial |
| 실적 관련 | Yes | No | No | No | No |
| 보유자 정보 | Yes | Partial | Partial | No | No |
| 애널리스트 | Yes | Partial | Partial | No | Partial |
| 재무제표 | Yes | No | No | No | No |
| 펀드 관련 | No | Yes | Yes | No | No |
| 기타 | Partial | Partial | Partial | Partial | Partial |

---

## 3. 데이터/모델 설계

### 3.1 Domain 모델

#### QuoteSummaryModuleResult

QuoteSummary API 요청 결과를 모듈 기반으로 관리하는 메인 데이터 클래스

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| requestedModules | Set&lt;QuoteSummaryModule&gt; | No | 사용자가 요청한 모듈 목록 |
| modules | Map&lt;QuoteSummaryModule, Any?&gt; | No | 실제로 반환된 모듈별 데이터 (모듈 → 데이터) |

**메서드**:
- `hasModule(module: QuoteSummaryModule): Boolean` - 특정 모듈 데이터 존재 여부 확인
- `getModule<T>(module: QuoteSummaryModule): T?` - 타입 안전한 모듈 데이터 가져오기 (reified)
- `hasAllRequestedModules(): Boolean` - 모든 요청 모듈이 응답에 포함되었는지 확인
- `getAvailableModules(): Set<QuoteSummaryModule>` - 실제 데이터가 존재하는 모듈 목록
- `getMissingModules(): Set<QuoteSummaryModule>` - 요청했으나 응답에 없는 모듈 목록

#### QuoteSummaryModule (Enum)

Yahoo Finance QuoteSummary API에서 지원하는 모듈 열거형

| Enum 값 | API 값 | 설명 |
|--------|--------|------|
| PRICE | "price" | 기본 가격 정보 |
| SUMMARY_DETAIL | "summaryDetail" | 상세 요약 정보 |
| SUMMARY_PROFILE | "summaryProfile" | 요약 프로필 정보 |
| ASSET_PROFILE | "assetProfile" | 자산 프로필 정보 |
| QUOTE_TYPE | "quoteType" | 자산 타입 정보 |
| DEFAULT_KEY_STATISTICS | "defaultKeyStatistics" | 기본 주요 통계 |
| FINANCIAL_DATA | "financialData" | 재무 데이터 |
| CALENDAR_EVENTS | "calendarEvents" | 캘린더 이벤트 |
| EARNINGS | "earnings" | 실적 연혁 |
| EARNINGS_HISTORY | "earningsHistory" | 실적 이력 |
| EARNINGS_TREND | "earningsTrend" | 실적 추이 |
| EARNINGS_DATES | "earningsDates" | 실적 발표 날짜 |
| MAJOR_HOLDERS | "majorHolders" | 주요 주주 |
| INSIDER_HOLDERS | "insiderHolders" | 내부자 보유 정보 |
| INSIDER_TRANSACTIONS | "insiderTransactions" | 내부자 거래 |
| INSTITUTION_OWNERSHIP | "institutionOwnership" | 기관 투자자 보유 |
| FUND_OWNERSHIP | "fundOwnership" | 펀드 보유 정보 |
| NET_SHARE_PURCHASE_ACTIVITY | "netSharePurchaseActivity" | 순자산 매수 활동 |
| RECOMMENDATION_TREND | "recommendationTrend" | 추천 동향 |
| UPGRADE_DOWNGRADE_HISTORY | "upgradeDowngradeHistory" | 등급 변경 이력 |
| INCOME_STATEMENT_HISTORY | "incomeStatementHistory" | 손익계산서 (연간) |
| BALANCE_SHEET_HISTORY | "balanceSheetHistory" | 대차대조표 (연간) |
| CASHFLOW_STATEMENT_HISTORY | "cashflowStatementHistory" | 현금흐름표 (연간) |
| INCOME_STATEMENT_HISTORY_QUARTERLY | "incomeStatementHistoryQuarterly" | 손익계산서 (분기) |
| BALANCE_SHEET_HISTORY_QUARTERLY | "balanceSheetHistoryQuarterly" | 대차대조표 (분기) |
| CASHFLOW_STATEMENT_HISTORY_QUARTERLY | "cashflowStatementHistoryQuarterly" | 현금흐름표 (분기) |
| TOP_HOLDINGS | "topHoldings" | 상위 보유 종목 |
| FUND_PROFILE | "fundProfile" | 펀드 프로필 |
| FUND_PERFORMANCE | "fundPerformance" | 펀드 성과 |
| SEC_FILINGS | "secFilings" | SEC 제출 서류 |
| PRICE_HISTORY | "priceHistory" | 가격 이력 |
| INDEX_TREND | "indexTrend" | 지수 추세 |
| INDUSTRY_TREND | "industryTrend" | 산업 추세 |
| SECTOR_TREND | "sectorTrend" | 섹터 추세 |
| PAGE_VIEWS | "pageViews" | 페이지 뷰 |
| ESG_SCORES | "esgScores" | ESG 점수 |

**Companion 메서드**:
- `fromApiValue(apiValue: String): QuoteSummaryModule?` - API 값으로 Enum 찾기
- `allModules(): Set<QuoteSummaryModule>` - 모든 모듈 반환
- `stockModules(): Set<QuoteSummaryModule>` - 주식(EQUITY)용 일반 모듈
- `fundModules(): Set<QuoteSummaryModule>` - ETF/펀드용 일반 모듈

#### 주요 모듈별 데이터 클래스

##### RawFormatted

Yahoo Finance API의 `{raw, fmt}` 형식 값을 나타내는 헬퍼 클래스

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| raw | JsonElement | Yes | 원시 값 (숫자, 문자열 등) |
| fmt | String | Yes | 포맷된 문자열 값 |
| doubleValue | Double | Yes (속성) | raw 값을 Double로 추출 |
| longValue | Long | Yes (속성) | raw 값을 Long으로 추출 |

##### Price (PRICE 모듈)

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| symbol | String | Yes | 티커 심볼 |
| longName | String | Yes | 정식 회사명 |
| shortName | String | Yes | 약식 회사명 |
| currency | String | Yes | 통화 코드 (USD, KRW 등) |
| exchange | String | Yes | 거래소 코드 |
| regularMarketPrice | RawFormatted | Yes | 현재 시장가 |
| regularMarketChange | RawFormatted | Yes | 가격 변화 |
| regularMarketChangePercent | RawFormatted | Yes | 변화율 (%) |
| regularMarketPreviousClose | RawFormatted | Yes | 전일 종가 |
| regularMarketDayRange | String | Yes | 당일 가격 범위 |
| postMarketPrice | RawFormatted | Yes | 장후 가격 |
| postMarketChangePercent | RawFormatted | Yes | 장후 변화율 |
| fiftyTwoWeekHigh | RawFormatted | Yes | 52주 최고가 |
| fiftyTwoWeekLow | RawFormatted | Yes | 52주 최저가 |
| fiftyTwoWeekChangePercent | RawFormatted | Yes | 52주 변화율 |

##### SummaryDetail (SUMMARY_DETAIL 모듈)

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| dividendRate | RawFormatted | Yes | 연간 배당금 |
| dividendYield | RawFormatted | Yes | 배당 수익률 |
| dividendDate | RawFormatted | Yes | 배당 지급일 (타임스탬프) |
| exDividendDate | RawFormatted | Yes | 배당락일 (타임스탬프) |
| beta | RawFormatted | Yes | 베타 계수 |
| trailingPE | RawFormatted | Yes | 후행 PER |
| forwardPE | RawFormatted | Yes | 선행 PER |
| priceToBook | RawFormatted | Yes | PBR (주가순자산비율) |
| marketCap | RawFormatted | Yes | 시가총액 |
| sharesOutstanding | RawFormatted | Yes | 발행주식수 |
| averageVolume | RawFormatted | Yes | 평균 거래량 |
| averageVolume10days | RawFormatted | Yes | 10일 평균 거래량 |
| regularMarketVolume | RawFormatted | Yes | 당일 거래량 |
| regularMarketDayHigh | RawFormatted | Yes | 당일 고가 |
| regularMarketDayLow | RawFormatted | Yes | 당일 저가 |
| fiftyTwoWeekHigh | RawFormatted | Yes | 52주 최고가 |
| fiftyTwoWeekLow | RawFormatted | Yes | 52주 최저가 |
| debtToEquity | RawFormatted | Yes | 부채비율 |

##### FinancialData (FINANCIAL_DATA 모듈)

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| operatingCashflow | RawFormatted | Yes | 영업 현금 흐름 |
| freeCashflow | RawFormatted | Yes | 잉여 현금 흐름 |
| totalCash | RawFormatted | Yes | 총 현금 |
| totalDebt | RawFormatted | Yes | 총 부채 |
| longTermDebt | RawFormatted | Yes | 장기 부채 |
| currentRatio | RawFormatted | Yes | 유동비율 |
| returnOnEquity | RawFormatted | Yes | ROE (자기자본이익률) |
| returnOnAssets | RawFormatted | Yes | ROA (자산이익률) |
| profitMargins | RawFormatted | Yes | 순이익률 |
| revenueGrowth | RawFormatted | Yes | 매출 성장률 |
| earningsGrowth | RawFormatted | Yes | 수익 성장률 |
| pegRatio | RawFormatted | Yes | PEG 비율 |
| targetPriceHigh | RawFormatted | Yes | 목표가 상한 |
| targetPriceLow | RawFormatted | Yes | 목표가 하한 |
| targetPriceMean | RawFormatted | Yes | 목표가 평균 |
| recommendationKey | String | Yes | 추천 등급 (buy/hold/sell) |
| numberOfAnalysts | Int | Yes | 애널리스트 수 |

##### AssetProfile (ASSET_PROFILE 모듈)

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| sector | String | Yes | 섹터 |
| industry | String | Yes | 산업 |
| website | String | Yes | 웹사이트 |
| longBusinessSummary | String | Yes | 회사 소개 (상세) |
| country | String | Yes | 국가 |
| city | String | Yes | 도시 |
| address1 | String | Yes | 주소 |
| state | String | Yes | 주/도 |
| zip | String | Yes | 우편번호 |
| phone | String | Yes | 전화번호 |
| fullTimeEmployees | Int | Yes | 정규직 직원 수 |

##### QuoteType (QUOTE_TYPE 모듈)

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| exchange | String | Yes | 거래소 |
| quoteType | String | Yes | 자산 타입 (EQUITY/ETF/MUTUALFUND/INDEX/CRYPTOCURRENCY) |
| symbol | String | Yes | 심볼 |
| shortName | String | Yes | 약식명 |
| longName | String | Yes | 정식명 |
| market | String | Yes | 시장 |
| sector | String | Yes | 섹터 |
| industry | String | Yes | 산업 |

##### DefaultKeyStatistics (DEFAULT_KEY_STATISTICS 모듈)

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| sharesOutstanding | RawFormatted | Yes | 발행주식수 |
| isin | String | Yes | ISIN 코드 |
| cusip | String | Yes | CUSIP 코드 |

##### EarningsTrend (EARNINGS_TREND 모듈)

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| trend | List&lt;EarningsTrendData&gt; | Yes | 실적 추이 데이터 목록 |
| earningsHistory | List&lt;EarningsTrendData&gt; | Yes | 실적 이력 목록 |

**EarningsTrendData**:

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| period | String | Yes | 기간 (예: "1Q2023") |
| endDate | String | Yes | 종료 날짜 |
| epsEstimate | RawFormatted | Yes | 추정 EPS |
| epsActual | RawFormatted | Yes | 실제 EPS |
| epsDifference | RawFormatted | Yes | 차이 (실제 - 추정) |
| surprisePercent | RawFormatted | Yes | 서프라이즈 비율 (%) |

##### TopHoldings (TOP_HOLDINGS 모듈 - 펀드용)

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| holdings | List&lt;Holding&gt; | Yes | 보유 종목 목록 |
| equityHoldings | EquityHoldings | Yes | 주식 보유 지표 |
| bondHoldings | BondHoldings | Yes | 채권 보유 지표 |
| sectorWeightings | List&lt;SectorWeighting&gt; | Yes | 섹터별 비중 |

**Holding**:

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| symbol | String | Yes | 보유 종목 심볼 |
| name | String | Yes | 보유 종목명 |
| holdingPercent | RawFormatted | Yes | 보유 비율 (%) |

**EquityHoldings**:

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| priceToEarnings | RawFormatted | Yes | 가중평균 PER |
| priceToBook | RawFormatted | Yes | 가중평균 PBR |
| priceToSales | RawFormatted | Yes | 가중평균 PSR |
| priceToCashflow | RawFormatted | Yes | 가중평균 PCF (주가현금흐름비율) |
| medianMarketCap | RawFormatted | Yes | 중앙값 시가총액 |
| threeYearEarningsGrowth | RawFormatted | Yes | 3년 수익 성장률 |

**BondHoldings**:

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| duration | RawFormatted | Yes | 듀레이션 |
| maturity | RawFormatted | Yes | 만기 |
| creditQuality | RawFormatted | Yes | 신용등급 |

**SectorWeighting**:

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| sector | String | Yes | 섹터명 |
| weight | RawFormatted | Yes | 비중 (%) |

##### FundProfile (FUND_PROFILE 모듈 - 펀드용)

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| categoryName | String | Yes | 펀드 카테고리 |
| family | String | Yes | 펀드 가족 (운용사) |
| legalType | String | Yes | 법적 형태 |
| feesExpensesInvestment | FeesExpenses | Yes | 수수료 및 비용 정보 |

**FeesExpenses**:

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| annualReportExpenseRatio | RawFormatted | Yes | 연간 비용 비율 |
| annualHoldingsTurnover | RawFormatted | Yes | 연간 회전율 |
| totalNetAssets | RawFormatted | Yes | 총 순자산 |

### 3.2 Internal Response 모델

#### 구조

| 클래스 | 필드 | 타입 | 설명 |
|-------|-----|------|------|
| QuoteSummaryResponse | quoteSummary | QuoteSummaryData | 최상위 응답 래퍼 |
| QuoteSummaryData | result | List&lt;QuoteSummaryResult&gt;? | 결과 배열 (길이 1 또는 null) |
| | error | QuoteSummaryError? | 에러 객체 |
| QuoteSummaryResult | (동적 필드) | JsonElement | 각 모듈명에 해당하는 필드 |
| QuoteSummaryError | code | String? | 에러 코드 |
| | description | String? | 에러 설명 |

#### 변환 로직

Internal Response → Domain 모델 변환 시:
1. `result` 배열에서 첫 번째 요소 추출
2. 요청한 각 모듈에 대해 응답에서 해당 모듈명의 JsonElement 추출
3. 모듈별로 적절한 데이터 클래스로 역직렬화
4. `QuoteSummaryModuleResult`에 `Map<QuoteSummaryModule, Any?>` 형태로 저장

### 3.3 API 메서드 시그니처

#### YahooClient (Infrastructure)

```kotlin
suspend fun quoteSummary(
    symbol: String,
    vararg modules: QuoteSummaryModule
): QuoteSummaryModuleResult
```

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|-----|------|
| symbol | String | Yes | 조회할 심볼 (예: "AAPL") |
| modules | vararg QuoteSummaryModule | Yes | 조회할 모듈 목록 (가변 인자, 1개 이상) |

**반환**: `QuoteSummaryModuleResult` - 요청한 모듈별 데이터를 포함한 결과 객체

**예외**:
- `ValidationException` - 빈 모듈 목록
- `ApiException` - API 호출 실패
- `DataParsingException` - 응답 파싱 실패

```kotlin
suspend fun quoteSummary(
    symbol: String,
    modules: Set<QuoteSummaryModule>
): QuoteSummaryModuleResult
```

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|-----|------|
| symbol | String | Yes | 조회할 심볼 |
| modules | Set&lt;QuoteSummaryModule&gt; | Yes | 조회할 모듈 Set (1개 이상) |

**반환**: `QuoteSummaryModuleResult`

### 3.4 필드 매핑

#### 타입 변환 규칙

| Yahoo 타입 | Domain 타입 | 변환 규칙 |
|-----------|------------|----------|
| Object {raw, fmt} | RawFormatted | 직접 매핑 |
| JsonElement (숫자) | Double | raw.jsonPrimitive.doubleOrNull |
| JsonElement (정수) | Long | raw.jsonPrimitive.longOrNull |
| String | String | 그대로 |
| Long (timestamp) | Instant | Instant.fromEpochSeconds() |
| Long (timestamp) | LocalDate | toLocalDateTime().date |
| Int | Int | 그대로 |
| null | null | 그대로 |

#### Nullable 처리

| 상황 | 처리 |
|-----|------|
| 필수 필드 없음 (symbol) | 예외 발생 (DATA_PARSING_ERROR) |
| 선택 필드 없음 | null로 설정 |
| 모듈 전체 없음 | Map에서 해당 모듈 제외 또는 값을 null로 설정 |
| 요청한 모듈이 응답에 없음 | `getMissingModules()`로 추적 가능 |

---

## 4. 예외 처리

### 4.1 ErrorCode 매핑

| 에러 케이스 | ErrorCode | HTTP Status | 설명 |
|-----------|-----------|-------------|------|
| 빈 모듈 목록 | INVALID_PARAMETER | - | 최소 1개 이상의 모듈 필요 |
| 잘못된 심볼 | INVALID_SYMBOL | 404 | 존재하지 않는 심볼 |
| 모듈 데이터 없음 | DATA_NOT_FOUND | - | 요청한 모듈의 데이터가 응답에 없음 (경고) |
| HTTP 4xx/5xx | EXTERNAL_API_ERROR | 4xx/5xx | API 오류 |
| Rate Limit (429) | RATE_LIMITED | 429 | 요청 제한 초과 |
| JSON 파싱 실패 | DATA_PARSING_ERROR | - | 역직렬화 실패 |
| CRUMB 획득 실패 | AUTH_FAILED | 401 | 인증 실패 |
| 네트워크 오류 | NETWORK_ERROR | - | 연결 실패 |

### 4.2 빈 결과 처리

| 상황 | 처리 |
|-----|------|
| result = null 또는 [] | INVALID_SYMBOL 예외 (심볼이 존재하지 않음) |
| result[0] 존재하지만 요청 모듈 없음 | QuoteSummaryModuleResult 반환, `getMissingModules()`로 확인 가능 |
| 일부 모듈만 존재 | 존재하는 모듈만 Map에 포함, 나머지는 제외 |
| 필수 필드 파싱 실패 | DATA_PARSING_ERROR 예외 |

### 4.3 재시도 정책

| 에러 | 재시도 | 횟수 | 대기 시간 |
|-----|-------|-----|----------|
| Rate Limit (429) | Yes | 무제한 | Rate Limiter 처리 (Token Bucket) |
| Network Error | Yes | 3회 | Exponential backoff |
| CRUMB 실패 | Yes | 3회 | 즉시 |
| HTTP 5xx | Yes | 3회 | Exponential backoff |
| HTTP 4xx (429 제외) | No | - | - |
| 파싱 오류 | No | - | - |
| 잘못된 심볼 | No | - | - |

### 4.4 로깅 전략

| 로그 레벨 | 상황 | 메시지 예시 |
|---------|-----|-----------|
| DEBUG | API 요청 시작 | "Calling Yahoo Finance QuoteSummary API: symbol=AAPL, modules=[PRICE, SUMMARY_DETAIL]" |
| INFO | 성공적인 응답 | "QuoteSummary API success: symbol=AAPL, modules=2" |
| WARN | 일부 모듈 누락 | "Requested modules missing in response: [EARNINGS_DATES]" |
| ERROR | API 오류 | "QuoteSummary API 요청 실패: HTTP 404, symbol=INVALID" |
| ERROR | 파싱 오류 | "Failed to parse QuoteSummary response for symbol=AAPL" |

---

## 5. 참고 자료

### 5.1 API 제약사항

| 제약 | 설명 |
|-----|------|
| 비공식 API | Yahoo Finance는 공식 문서가 없으며 언제든지 변경될 수 있음 |
| 단일 심볼만 지원 | 한 번에 하나의 심볼만 조회 가능 (Quote API와 다름) |
| 모듈 필수 | 최소 1개 이상의 모듈을 반드시 지정해야 함 |
| CRUMB 필수 | 인증 토큰(CRUMB) 필요 |
| 자산 유형별 차이 | 일부 모듈은 특정 자산 타입에서만 데이터 제공 |
| Rate Limiting | 과도한 요청 시 429 에러 발생 |

### 5.2 캐싱 전략

| API | TTL | 이유 |
|-----|-----|------|
| quoteSummary (PRICE, SUMMARY_DETAIL) | 60초 | 실시간성이 중요한 데이터 |
| quoteSummary (재무제표) | 24시간 | 분기/연간 단위로 갱신되는 정적 데이터 |
| quoteSummary (ASSET_PROFILE) | 7일 | 거의 변하지 않는 기업 정보 |
| quoteSummary (EARNINGS_DATES) | 1시간 | 실적 발표일은 자주 변경되지 않음 |

### 5.3 사용 패턴 가이드

#### 주식 분석용 모듈 조합

```kotlin
val stockModules = setOf(
    QuoteSummaryModule.PRICE,
    QuoteSummaryModule.SUMMARY_DETAIL,
    QuoteSummaryModule.FINANCIAL_DATA,
    QuoteSummaryModule.DEFAULT_KEY_STATISTICS,
    QuoteSummaryModule.EARNINGS_TREND,
    QuoteSummaryModule.RECOMMENDATION_TREND
)
```

#### ETF/펀드 분석용 모듈 조합

```kotlin
val fundModules = setOf(
    QuoteSummaryModule.PRICE,
    QuoteSummaryModule.SUMMARY_DETAIL,
    QuoteSummaryModule.TOP_HOLDINGS,
    QuoteSummaryModule.FUND_PROFILE,
    QuoteSummaryModule.FUND_PERFORMANCE
)
```

#### 기업 재무분석용 모듈 조합

```kotlin
val financialModules = setOf(
    QuoteSummaryModule.FINANCIAL_DATA,
    QuoteSummaryModule.INCOME_STATEMENT_HISTORY,
    QuoteSummaryModule.BALANCE_SHEET_HISTORY,
    QuoteSummaryModule.CASH_FLOW_STATEMENT_HISTORY
)
```

### 5.4 Quote API vs QuoteSummary API 선택 가이드

| 사용 사례 | 권장 API | 이유 |
|---------|---------|------|
| 실시간 가격 모니터링 | Quote API | 빠른 응답, 다중 심볼 지원 |
| 상세 기업 정보 조회 | QuoteSummary API | 깊이 있는 데이터, 모듈 선택 가능 |
| 대시보드 (다중 심볼) | Quote API | 한 번에 여러 심볼 조회 가능 |
| 종합 리포트 (단일 심볼) | QuoteSummary API | 재무제표, 실적, 보유자 정보 등 상세 데이터 |
| 간단한 가격/통계 확인 | Quote API | 오버헤드가 적음 |
| 재무제표 분석 | QuoteSummary API | 전문적인 재무 데이터 제공 |
| 펀드 보유 종목 분석 | QuoteSummary API | TOP_HOLDINGS 모듈 지원 |

### 5.5 용어

| 용어 | 설명 |
|-----|------|
| QuoteSummary | Yahoo Finance의 상세 정보 API |
| Module | QuoteSummary API에서 제공하는 데이터 카테고리 단위 |
| CRUMB | Yahoo Finance 인증 토큰 |
| RawFormatted | Yahoo API의 `{raw, fmt}` 형식 응답 구조 |
| PER | Price to Earnings Ratio (주가수익비율) |
| PBR | Price to Book Ratio (주가순자산비율) |
| ROE | Return on Equity (자기자본이익률) |
| ROA | Return on Assets (자산이익률) |
| EPS | Earnings Per Share (주당순이익) |
| ISIN | International Securities Identification Number (국제 증권 식별 번호) |
| CUSIP | Committee on Uniform Securities Identification Procedures (미국 증권 식별 번호) |
| ESG | Environmental, Social, and Governance (환경, 사회, 거버넌스) |
