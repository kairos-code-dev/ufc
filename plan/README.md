# UFC Technical Specification Documents

## 문서 개요

이 디렉토리는 UFC (US Free Financial Data Collector) 프로젝트의 기술 명세서를 포함합니다.

### 버전 정보
- **버전**: 2.0.0
- **최종 수정일**: 2025-12-01
- **아키텍처**: Multi-Source (Yahoo Finance + FRED)

---

## 문서 목록

### 1. 프로젝트 개요
- **[00-project-overview.md](./00-project-overview.md)** - 프로젝트 전체 개요
  - Multi-Source 아키텍처 소개
  - Yahoo Finance + FRED 통합
  - 개발 로드맵
  - 주요 기능

### 2. 아키텍처 설계
- **[01-architecture-design.md](./01-architecture-design.md)** - Multi-Source 아키텍처 설계
  - 레이어 구조 (Client, Source, Infrastructure, Model)
  - DataSource 인터페이스
  - UFCClient Facade
  - HTTP 클라이언트 전략
  - 의존성 주입

### 3. 에러 처리
- **[02-error-handling.md](./02-error-handling.md)** - ErrorCode 기반 예외 시스템
  - KFC ErrorCode 시스템 참조
  - 단일 예외 + ErrorCode enum
  - Metadata 활용
  - 재시도 정책
  - 로깅 전략

### 4. Yahoo Finance Core
- **[03-yahoo-finance-core.md](./03-yahoo-finance-core.md)** - HTTP 클라이언트 및 인증
  - YahooFinanceSource 인터페이스
  - Cookie/Crumb 인증 (Basic + CSRF)
  - Chart API
  - QuoteSummary API
  - 재시도 및 에러 처리

### 5. Yahoo Finance ETF
- **[04-yahoo-finance-etf.md](./04-yahoo-finance-etf.md)** - 전체 ETF 엔드포인트 명세
  - QuoteSummary 모듈 (topHoldings, fundProfile, summaryDetail, price 등)
  - Chart API (가격 히스토리)
  - Events API (배당, 분할)
  - ETF 클래스 구현
  - DataFrame 변환

### 6. Yahoo Finance Price
- **[05-yahoo-finance-price.md](./05-yahoo-finance-price.md)** - 가격 데이터 명세
  - Ticker 클래스
  - OHLCV 모델
  - Corporate Actions (배당, 분할)
  - Period/Interval enum

### 7. FRED Macro Indicators
- **[06-fred-macro-indicators.md](./06-fred-macro-indicators.md)** - 매크로 경제 지표
  - Python fredapi 참조
  - FREDSource 인터페이스
  - Vintage Data (개정 이력)
  - Search API
  - 주요 경제 지표 Enum
  - Series/Observation 모델

### 8. Data Models Reference
- **[08-data-models-reference.md](./08-data-models-reference.md)** - 데이터 모델 참조
  - 공통 모델 (ValueFormat, Error, Meta)
  - Yahoo Finance 모델
  - FRED 모델
  - Enum 타입

### 9. Testing Strategy
- **[09-testing-strategy.md](./09-testing-strategy.md)** - 테스트 구현 전략
  - KFC 프로젝트 패턴 준수
  - Live Test vs Unit Test 분리
  - 응답 레코딩 시스템
  - Given-When-Then 패턴
  - 테스트 작성 가이드

### 10. Advanced Topics
- **[07-advanced-topics.md](./07-advanced-topics.md)** - 고급 주제
  - Virtual Threads 활용
  - 캐싱 전략
  - Rate Limiting
  - TLS Fingerprinting 회피
  - 병렬 처리 패턴
  - 에러 복구 (Circuit Breaker)
  - Kotlin DataFrame 통합
  - 로깅 및 모니터링
  - 성능 최적화

---

## 문서 읽는 순서

### 처음 시작하는 경우
1. **00-project-overview.md** - 프로젝트 전체 이해
2. **01-architecture-design.md** - 아키텍처 구조 파악
3. **02-error-handling.md** - 에러 처리 방식 이해

### Yahoo Finance 구현
4. **03-yahoo-finance-core.md** - 기본 인프라
5. **04-yahoo-finance-etf.md** - ETF 기능
6. **05-yahoo-finance-price.md** - 가격 데이터

### FRED 구현
7. **06-fred-macro-indicators.md** - FRED API 전체

### 데이터 모델 및 테스트
8. **08-data-models-reference.md** - 전체 데이터 모델 참조
9. **09-testing-strategy.md** - 테스트 구현 전략

### 최적화
10. **07-advanced-topics.md** - 고급 기능 및 최적화

---

## 주요 특징

### Multi-Source 아키텍처
- **Yahoo Finance**: 주가, ETF, 재무제표, 뉴스
- **FRED**: GDP, 실업률, 인플레이션, 금리 등 매크로 경제 지표

### ErrorCode 기반 예외 시스템
- KFC 참조
- 단일 UFCException + ErrorCode enum
- Metadata로 컨텍스트 정보 전달

### 기술 스택
- Kotlin 2.1.0
- JDK 21 (Virtual Threads)
- Ktor 3.0.1 (HTTP Client)
- kotlinx.serialization

---

## 참고 자료

- **KFC (Korea Financial Client)**: Multi-Source 패턴 참조
- **Python yfinance**: https://github.com/ranaroussi/yfinance
- **Python fredapi**: https://github.com/mortada/fredapi
- **FRED API Documentation**: https://fred.stlouisfed.org/docs/api/fred/

---

**마지막 업데이트**: 2025-12-02
