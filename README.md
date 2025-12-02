# UFC (US Free Financial Data Collector)

무료 미국 금융 데이터 수집 Kotlin 라이브러리

## 프로젝트 개요

UFC는 Yahoo Finance와 FRED(Federal Reserve Economic Data)로부터 금융 데이터를 수집하는 Multi-Source 아키텍처 기반 라이브러리입니다.

### 주요 특징

- **Multi-Source Architecture**: Yahoo Finance + FRED 통합
- **타입 안정성**: Kotlin 강타입 시스템
- **현대적 비동기**: Coroutines 기반 고성능 처리
- **통합 에러 처리**: ErrorCode 기반 단일 예외 시스템
- **KFC 패턴 준수**: Korea Financial Client 프로젝트 패턴 참조

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
import com.ulalax.ufc.UFCClient
import com.ulalax.ufc.UFCClientConfig
import java.time.LocalDate

suspend fun main() {
    // UFCClient 생성
    val ufc = UFCClient.create(
        config = UFCClientConfig(fredApiKey = "your_api_key")
    )

    // Yahoo Finance: ETF 데이터
    val spy = ufc.yahoo.etf("SPY")
    val holdings = spy.getTopHoldings()
    holdings.holdings?.take(5)?.forEach { holding ->
        println("${holding.symbol}: ${holding.holdingPercent?.fmt}")
    }

    // Yahoo Finance: 주가 데이터
    val aapl = ufc.yahoo.ticker("AAPL")
    val history = aapl.history(period = Period.OneYear)
    println("Recent AAPL prices:")
    history.takeLast(5).forEach { bar ->
        println("${bar.date}: Close=${bar.close}")
    }

    // FRED: GDP 조회
    val gdp = ufc.fred.getSeries(
        seriesId = "GDPC1",
        observationStart = LocalDate.of(2020, 1, 1),
        observationEnd = LocalDate.of(2024, 1, 1)
    )
    println("GDP observations: ${gdp.observations.size}")

    // FRED: 검색
    val unemployment = ufc.fred.search("unemployment rate", limit = 5)
    unemployment.forEach {
        println("${it.id}: ${it.title}")
    }

    // 종료
    ufc.close()
}
```

## 프로젝트 구조

```
ufc/
├── src/
│   ├── main/kotlin/com/ulalax/ufc/         # 프로덕션 코드
│   │   ├── client/                         # UFCClient (Facade)
│   │   ├── source/                         # DataSource 구현
│   │   │   ├── yahoo/                      # Yahoo Finance
│   │   │   └── fred/                       # FRED
│   │   ├── model/                          # 데이터 모델
│   │   ├── exception/                      # 에러 처리
│   │   └── infrastructure/                 # 공통 인프라
│   │
│   ├── liveTest/kotlin/                    # Live Test (실제 API 호출)
│   │   ├── live/yahoo/                     # Yahoo Finance Live Tests
│   │   ├── live/fred/                      # FRED Live Tests
│   │   └── utils/                          # 테스트 유틸리티
│   │
│   └── test/kotlin/                        # Unit Test (레코딩된 데이터)
│       ├── source/yahoo/                   # Yahoo Finance Tests
│       └── source/fred/                    # FRED Tests
│
├── plan/                                   # 기술 명세서
│   ├── 00-project-overview.md
│   ├── 01-architecture-design.md
│   ├── 02-error-handling.md
│   ├── 03-yahoo-finance-core.md
│   ├── 04-yahoo-finance-etf.md
│   ├── 05-yahoo-finance-price.md
│   ├── 06-fred-macro-indicators.md
│   ├── 07-advanced-topics.md
│   ├── 08-data-models-reference.md
│   └── 09-testing-strategy.md
│
├── local.properties.template               # API Key 템플릿
├── build.gradle.kts
└── README.md
```

## 문서

상세한 기술 명세서는 `plan/` 디렉토리를 참고하세요:

1. **[프로젝트 개요](./plan/00-project-overview.md)** - 전체 개요 및 로드맵
2. **[아키텍처 설계](./plan/01-architecture-design.md)** - Multi-Source 아키텍처
3. **[에러 처리](./plan/02-error-handling.md)** - ErrorCode 시스템
4. **[Yahoo Finance](./plan/03-yahoo-finance-core.md)** - Yahoo Finance API
5. **[FRED](./plan/06-fred-macro-indicators.md)** - FRED API (공식 문서: https://fred.stlouisfed.org/docs/api/fred/)
6. **[테스트 전략](./plan/09-testing-strategy.md)** - Live Test & Unit Test

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

- [x] Phase 1: 핵심 인프라 + 에러 처리
- [ ] Phase 2: Yahoo Finance 인증 및 가격 데이터
- [ ] Phase 3: Yahoo Finance ETF
- [ ] Phase 4: FRED 매크로 지표
- [ ] Phase 5: Yahoo Finance 재무제표
- [ ] Phase 6: 검색 및 스크리닝
- [ ] Phase 7: 성능 최적화
- [ ] Phase 8: 문서화 및 배포

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

---

**최종 수정일**: 2025-12-02
