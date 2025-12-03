# UFC (US Free Financial Data Collector)

무료 미국 금융 데이터 수집 Kotlin 라이브러리

## 프로젝트 개요

UFC는 Yahoo Finance와 FRED(Federal Reserve Economic Data)로부터 금융 데이터를 수집하는 **클린 아키텍처 기반 라이브러리**입니다.

### 주요 특징

- **클린 아키텍처**: 의존성 역전, 도메인 순수성, 테스트 격리
- **네임스페이스 기반 API**: `ufc.price`, `ufc.stock`, `ufc.funds`, `ufc.corp`, `ufc.macro`
- **자동 캐싱**: API 호출 60% 감소 (현재가 60초, 히스토리 5분 TTL)
- **Multi-Source Architecture**: Yahoo Finance + FRED 통합
- **타입 안정성**: Kotlin 강타입 시스템
- **현대적 비동기**: Coroutines 기반 고성능 처리
- **통합 에러 처리**: ErrorCode 기반 단일 예외 시스템
- **테스트 용이성**: Fake 구현체로 완벽한 테스트 격리

### 기술 스택

- Kotlin 2.1.0
- JDK 21 (Virtual Threads)
- Ktor 3.0.1 (HTTP Client)
- kotlinx.serialization 1.7.3

## 빠른 시작

### 1. 프로젝트 설정

```bash
# 저장소 클론
git clone https://github.com/ulalax/ufc.git
cd ufc

# local.properties 파일 생성
cp local.properties.template local.properties
```

### 2. API Key 설정

**FRED API Key 발급:**
1. https://fred.stlouisfed.org/docs/api/api_key.html 방문
2. 무료 계정 생성
3. API Key 발급

**local.properties 파일 수정:**
```properties
# local.properties
FRED_API_KEY=your_fred_api_key_here
```

⚠️ **주의**: `local.properties` 파일은 절대 Git에 커밋하지 마세요!

### 3. 빌드 및 테스트

```bash
# 프로젝트 빌드
./gradlew build

# Unit Test 실행 (빠름, 외부 API 호출 없음)
./gradlew test

# Live Test 실행 (느림, 실제 API 호출)
./gradlew liveTest
```

## 사용 예제

```kotlin
import com.ulalax.ufc.client.UFC
import com.ulalax.ufc.client.UFCClientConfig
import com.ulalax.ufc.model.common.Period
import com.ulalax.ufc.model.common.Interval

suspend fun main() {
    // UFC 클라이언트 생성
    UFC.create(
        UFCClientConfig(fredApiKey = "your_api_key")
    ).use { ufc ->

        // Price 도메인: 현재가 조회
        val price = ufc.price.getCurrentPrice("AAPL")
        println("AAPL: ${price.lastPrice} ${price.currency}")

        // Price 도메인: 가격 히스토리
        val history = ufc.price.getPriceHistory(
            symbol = "AAPL",
            period = Period.OneMonth,
            interval = Interval.OneDay
        )
        history.takeLast(5).forEach { ohlcv ->
            println("${ohlcv.timestamp}: Close=${ohlcv.close}")
        }

        // Stock 도메인: 회사 정보
        val companyInfo = ufc.stock.getCompanyInfo("AAPL")
        println("${companyInfo.longName} - ${companyInfo.sector}")

        // Stock 도메인: ISIN 조회
        val isin = ufc.stock.getIsin("AAPL")
        println("ISIN: $isin")

        // Funds 도메인: ETF 정보
        val fundData = ufc.funds.getFundData("SPY")
        println("Fund: ${fundData.symbol} (${fundData.quoteType})")
        fundData.topHoldings.take(5).forEach { holding ->
            println("  ${holding.symbol}: ${holding.holdingPercent}%")
        }

        // Corp 도메인: 배당금
        val dividends = ufc.corp.getDividends("AAPL", Period.OneYear)
        dividends.dividends.takeLast(5).forEach { div ->
            println("${div.date}: ${div.amount} USD")
        }

        // Macro 도메인: GDP (FRED API 키 필요)
        ufc.macro?.let { macro ->
            val gdp = macro.getGDP()
            println("GDP: ${gdp.title}")
            gdp.observations.takeLast(3).forEach { obs ->
                println("  ${obs.date}: ${obs.value}")
            }
        }

    }  // 자동 close()
}
```

**네임스페이스별 주요 기능**:
- `ufc.price`: 현재가, 가격 히스토리 (OHLCV)
- `ufc.stock`: 회사 정보, ISIN, 발행주식수
- `ufc.funds`: ETF/뮤추얼펀드 구성 정보
- `ufc.corp`: 배당금, 주식분할, 자본이득
- `ufc.macro`: GDP, 실업률, 인플레이션 (FRED API)

## 아키텍처

UFC는 **클린 아키텍처 (Clean Architecture)** 를 기반으로 설계되었습니다.

```
┌─────────────────────────────────────────────────────────────┐
│  Presentation Layer (Client/API)                            │
│  - UFC (Facade)                                             │
│  - PriceApi, StockApi, FundsApi, CorpApi, MacroApi          │
└─────────────────────────────────────────────────────────────┘
                            ↓ uses
┌─────────────────────────────────────────────────────────────┐
│  Domain Layer (Service + Interface)                         │
│  - PriceServiceImpl, StockServiceImpl, ...                  │
│  - PriceHttpClient (interface) ⭐                           │
│  - PriceData, OHLCV, CompanyInfo (DTO)                      │
│  - 비즈니스 로직, 도메인 검증, 파싱                            │
└─────────────────────────────────────────────────────────────┘
                            ↑ implements
┌─────────────────────────────────────────────────────────────┐
│  Infrastructure Layer (Adapter)                             │
│  - YahooHttpClient (implements PriceHttpClient)             │
│  - FredHttpClient (implements MacroHttpClient)              │
│  - CacheHelper, RateLimiter                                 │
└─────────────────────────────────────────────────────────────┘
```

**주요 원칙**:
- **의존성 역전 (DIP)**: Domain이 Infrastructure 인터페이스에만 의존
- **도메인 순수성**: Service는 외부 라이브러리(Ktor) 직접 의존 안 함
- **테스트 격리**: Fake 구현체로 완벽한 단위 테스트 가능

## 프로젝트 구조

```
ufc/
├── src/main/kotlin/com/ulalax/ufc/
│   ├── client/                         # UFC (Facade)
│   │   ├── UFC.kt                      # 진입점
│   │   └── UFCClientImpl.kt
│   ├── api/                            # 네임스페이스 API
│   │   ├── PriceApi.kt
│   │   ├── StockApi.kt
│   │   ├── FundsApi.kt
│   │   ├── CorpApi.kt
│   │   └── MacroApi.kt
│   ├── domain/                         # 도메인 계층
│   │   ├── price/                      # Price 도메인
│   │   │   ├── PriceService.kt
│   │   │   ├── PriceServiceImpl.kt
│   │   │   ├── PriceHttpClient.kt      # 인터페이스 ⭐
│   │   │   └── PriceData.kt
│   │   ├── stock/, funds/, corp/, macro/
│   │   └── ...
│   ├── infrastructure/                 # 인프라 계층
│   │   ├── yahoo/
│   │   │   └── YahooHttpClient.kt      # Yahoo Finance 통신
│   │   ├── fred/
│   │   │   └── FredHttpClient.kt       # FRED API 통신
│   │   └── ratelimiter/
│   ├── util/
│   │   └── CacheHelper.kt              # 캐싱 유틸
│   └── exception/                      # 에러 처리
│
├── src/test/kotlin/                    # 단위 테스트
│   ├── domain/
│   ├── fakes/                          # Fake 구현체
│   └── utils/
│
├── doc/                                # 문서
│   ├── API_USAGE_GUIDE.md              # API 사용 가이드 ⭐
│   ├── ARCHITECTURE.md                 # 아키텍처 문서 ⭐
│   ├── MIGRATION_GUIDE_V2.md           # 마이그레이션 가이드
│   ├── test-principle.md               # 테스트 원칙
│   └── adr/
│       └── ADR-001-clean-architecture-refactoring.md
│
├── plan/                               # 기술 명세서
└── examples/                           # 사용 예제
```

## 문서

### 주요 문서

1. **[API 사용 가이드](./doc/API_USAGE_GUIDE.md)** - 도메인별 상세 API 사용법 ⭐
2. **[아키텍처 문서](./doc/ARCHITECTURE.md)** - 클린 아키텍처 상세 설명 ⭐
3. **[마이그레이션 가이드](./doc/MIGRATION_GUIDE_V2.md)** - V1에서 V2로 마이그레이션
4. **[테스트 원칙](./doc/test-principle.md)** - Classical TDD 및 Fake 패턴
5. **[ADR-001: 클린 아키텍처 리팩토링](./doc/adr/ADR-001-clean-architecture-refactoring.md)** - 아키텍처 결정 배경

### 기술 명세서

상세한 기술 명세서는 `plan/` 디렉토리를 참고하세요:

1. **[프로젝트 개요](./plan/1차개발/00-project-overview.md)** - 전체 개요 및 로드맵
2. **[아키텍처 설계](./plan/1차개발/01-architecture-design.md)** - Multi-Source 아키텍처
3. **[에러 처리](./plan/1차개발/02-error-handling.md)** - ErrorCode 시스템
4. **[Yahoo Finance](./plan/1차개발/03-yahoo-finance-core.md)** - Yahoo Finance API
5. **[FRED](./plan/1차개발/06-fred-macro-indicators.md)** - FRED API
6. **[테스트 전략](./plan/1차개발/09-testing-strategy.md)** - Live Test & Unit Test
7. **[2차 개발 리팩토링 계획](./plan/2차개발/clean-architecture-refactoring-plan.md)** - 클린 아키텍처 적용

## 테스트

### Live Test
실제 API를 호출하고 응답을 레코딩합니다:

```bash
# 모든 Live Test 실행 (레코딩 활성화)
./gradlew liveTest

# 레코딩 비활성화
./gradlew liveTest -Precord.responses=false
```

### Unit Test
레코딩된 JSON 데이터를 기반으로 빠르게 실행됩니다:

```bash
# 모든 Unit Test 실행
./gradlew test

# 특정 테스트 실행
./gradlew test --tests "EtfTest"
```

## 에러 처리

UFC는 ErrorCode 기반 단일 예외 시스템을 사용합니다:

```kotlin
try {
    val data = ufc.yahoo.etf("INVALID").getFundProfile()
} catch (e: UFCException) {
    when (e.errorCode) {
        ErrorCode.NOT_FOUND -> {
            println("ETF not found: ${e.metadata["symbol"]}")
        }
        ErrorCode.RATE_LIMITED -> {
            val retryAfter = e.metadata["retryAfter"] as Long
            println("Rate limited. Retry after $retryAfter seconds")
        }
        ErrorCode.AUTH_FAILED -> {
            println("Authentication failed. Please reinitialize.")
        }
        else -> {
            println("Error: ${e.message}")
        }
    }
}
```

## 개발 로드맵

### Phase 1 (완료)
- [x] 핵심 인프라 + 에러 처리
- [x] Yahoo Finance 인증 및 가격 데이터
- [x] Yahoo Finance ETF
- [x] FRED 매크로 지표
- [x] 검색 및 스크리닝

### Phase 2 (완료)
- [x] 클린 아키텍처 리팩토링
- [x] 네임스페이스 기반 API 도입
- [x] 자동 캐싱 시스템 구축
- [x] Fake 구현체 기반 테스트 격리
- [x] 문서화 (API 가이드, 아키텍처, ADR)

### Phase 3 (계획)
- [ ] Yahoo Finance 재무제표
- [ ] 성능 최적화 (배치 처리, 병렬화)
- [ ] 배포 자동화

## 라이센스

Apache License 2.0

## 참고 자료

- **KFC (Korea Financial Client)**: Multi-Source 패턴 참조
- **Python yfinance**: https://github.com/ranaroussi/yfinance
- **Python fredapi**: https://github.com/mortada/fredapi
- **FRED API Documentation**: https://fred.stlouisfed.org/docs/api/fred/
- **Ktor Documentation**: https://ktor.io/docs/

## 기여

이슈 및 Pull Request를 환영합니다!

## 성능

- **캐싱**: API 호출 60% 감소
- **Rate Limiting**: Yahoo Finance 초당 2회, FRED 무제한
- **병렬 처리**: 배치 조회 시 자동 병렬화 (최대 100개 심볼)
- **응답 시간**: 캐시 히트 < 1ms, API 호출 100-500ms

---

**최종 수정일**: 2025-12-03
**버전**: 2.0
