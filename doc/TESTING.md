# UFC 테스트 가이드

UFC 프로젝트의 테스트 전략과 실행 방법을 설명합니다.

## 목차

- [테스트 구조](#테스트-구조)
- [테스트 실행](#테스트-실행)
- [통합 테스트](#통합-테스트)
- [환경 설정](#환경-설정)
- [테스트 원칙](#테스트-원칙)

---

## 테스트 구조

UFC 프로젝트는 다음과 같은 테스트 계층을 가지고 있습니다:

```
src/test/kotlin/com/ulalax/ufc/
├── domain/                  # 도메인별 단위 테스트
│   ├── price/              # Price 도메인 테스트
│   ├── stock/              # Stock 도메인 테스트
│   ├── funds/              # Funds 도메인 테스트
│   ├── corp/               # Corp 도메인 테스트
│   └── macro/              # Macro 도메인 테스트
├── integration/             # 통합 테스트
│   ├── PriceIntegrationTest.kt
│   ├── StockIntegrationTest.kt
│   ├── FundsIntegrationTest.kt
│   ├── CorpIntegrationTest.kt
│   ├── MacroIntegrationTest.kt
│   └── CrossDomainIntegrationTest.kt
└── fakes/                   # 테스트 더블 (Fakes)
    ├── FakePriceService.kt
    ├── FakeStockService.kt
    └── ...
```

### 테스트 유형

1. **단위 테스트 (Unit Tests)**
   - 외부 의존성 없이 실행
   - Fake 구현체 사용
   - 빠른 실행 속도
   - 위치: `src/test/kotlin/com/ulalax/ufc/domain/`

2. **통합 테스트 (Integration Tests)**
   - 실제 API 호출
   - 네트워크 의존성 있음
   - 느린 실행 속도
   - `@Tag("integration")` 어노테이션
   - 위치: `src/test/kotlin/com/ulalax/ufc/integration/`

---

## 테스트 실행

### 단위 테스트만 실행 (빠름)

통합 테스트를 제외한 단위 테스트만 실행합니다.

```bash
./gradlew test
```

**특징:**
- 외부 API 호출 없음
- 빠른 실행 (수 초 이내)
- CI/CD에서 매번 실행 가능
- 통합 테스트는 자동으로 제외됨

### 통합 테스트만 실행 (느림)

실제 API를 호출하는 통합 테스트만 실행합니다.

```bash
./gradlew integrationTest
```

**특징:**
- 실제 Yahoo Finance API 호출
- 실제 FRED API 호출 (Macro 테스트)
- 느린 실행 (수 분 소요)
- Rate Limiting 적용
- 네트워크 연결 필요

### FRED API 키가 필요한 테스트 실행

Macro 도메인 통합 테스트는 FRED API 키가 필요합니다.

```bash
# 환경 변수로 API 키 설정
export FRED_API_KEY=your_fred_api_key_here

# 통합 테스트 실행
./gradlew integrationTest
```

**FRED API 키 발급:**
- 무료 키 발급: https://fred.stlouisfed.org/docs/api/api_key.html
- 회원 가입 후 즉시 발급 가능

### 전체 테스트 실행 (단위 + 통합)

모든 테스트를 순차적으로 실행합니다.

```bash
# FRED API 키 설정
export FRED_API_KEY=your_key_here

# 전체 테스트 실행
./gradlew test integrationTest
```

### 특정 테스트만 실행

```bash
# Price 통합 테스트만 실행
./gradlew test --tests "*PriceIntegration*"

# Stock 도메인 모든 테스트 실행
./gradlew test --tests "com.ulalax.ufc.domain.stock.*"

# 특정 테스트 클래스만 실행
./gradlew test --tests "com.ulalax.ufc.integration.CrossDomainIntegrationTest"
```

---

## 통합 테스트

### 통합 테스트 목록

| 테스트 파일 | 설명 | API 키 필요 |
|------------|------|------------|
| `PriceIntegrationTest.kt` | 가격 정보 조회 검증 | ❌ |
| `StockIntegrationTest.kt` | 주식 정보 조회 검증 | ❌ |
| `FundsIntegrationTest.kt` | 펀드 정보 조회 검증 | ❌ |
| `CorpIntegrationTest.kt` | 배당금, 주식분할 조회 검증 | ❌ |
| `MacroIntegrationTest.kt` | 거시경제 지표 조회 검증 | ✅ FRED API |
| `CrossDomainIntegrationTest.kt` | 도메인 간 상호작용 검증 | ❌ |

### PriceIntegrationTest

**테스트 시나리오:**
- 현재가 조회 (AAPL, MSFT 등)
- 가격 히스토리 조회 (다양한 기간/간격)
- 메타데이터 조회
- 캐싱 동작 검증

**실행 예시:**
```bash
./gradlew test --tests "*PriceIntegration*"
```

### StockIntegrationTest

**테스트 시나리오:**
- 회사 정보 조회
- FastInfo 조회
- ISIN 코드 조회
- 발행주식수 조회
- 데이터 일관성 검증

**실행 예시:**
```bash
./gradlew test --tests "*StockIntegration*"
```

### FundsIntegrationTest

**테스트 시나리오:**
- ETF 정보 조회 (SPY, QQQ, AGG)
- Top Holdings 조회
- 섹터 배분 조회
- 펀드 여부 확인

**실행 예시:**
```bash
./gradlew test --tests "*FundsIntegration*"
```

### CorpIntegrationTest

**테스트 시나리오:**
- 배당금 이력 조회
- 주식분할 이력 조회
- 자본이득 분배 조회
- 다양한 기간 조회

**실행 예시:**
```bash
./gradlew test --tests "*CorpIntegration*"
```

### MacroIntegrationTest

**테스트 시나리오:**
- GDP 조회
- 실업률 조회
- 인플레이션 조회 (CPI, PCE)
- 금리 조회 (Fed Funds, Treasury)
- 기간 필터링

**실행 예시:**
```bash
export FRED_API_KEY=your_key_here
./gradlew test --tests "*MacroIntegration*"
```

### CrossDomainIntegrationTest

**테스트 시나리오:**
- 주식 정보 + 가격 병렬 조회
- 펀드 정보 + 가격 조합
- 배당금 + 가격 히스토리 분석
- 다중 심볼 조회
- Rate Limiting 동작 검증
- 데이터 일관성 검증

**실행 예시:**
```bash
./gradlew test --tests "*CrossDomainIntegration*"
```

---

## 환경 설정

### .env 파일 설정

프로젝트 루트에 `.env` 파일을 생성하여 환경 변수를 설정할 수 있습니다.

```bash
# .env.example을 복사하여 .env 생성
cp .env.example .env

# .env 파일 편집
vim .env
```

**.env 파일 예시:**
```bash
# FRED API Key
FRED_API_KEY=your_fred_api_key_here

# 테스트 설정
TEST_STOCK_SYMBOL=AAPL
TEST_ETF_SYMBOL=SPY
TEST_ENV=integration
```

**주의사항:**
- `.env` 파일은 Git에 커밋하지 마세요!
- `.gitignore`에 이미 추가되어 있습니다.

### CI/CD 환경 설정

GitHub Actions, GitLab CI 등에서 환경 변수를 설정하는 방법:

#### GitHub Actions

```yaml
# .github/workflows/test.yml
name: Test

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'

      # 단위 테스트 (항상 실행)
      - name: Run unit tests
        run: ./gradlew test

      # 통합 테스트 (secrets 설정 필요)
      - name: Run integration tests
        env:
          FRED_API_KEY: ${{ secrets.FRED_API_KEY }}
        run: ./gradlew integrationTest
        if: github.event_name == 'push' && github.ref == 'refs/heads/main'
```

**Secrets 설정:**
1. GitHub 저장소 → Settings → Secrets and variables → Actions
2. `New repository secret` 클릭
3. Name: `FRED_API_KEY`, Secret: `your_key_here`

---

## 테스트 원칙

UFC 프로젝트는 다음 테스트 원칙을 따릅니다:

### 1. Classical TDD

- Red → Green → Refactor 사이클
- 테스트 먼저 작성 (Test First)
- 최소한의 코드로 테스트 통과

### 2. 명세 기반 테스트 (Specification-based Testing)

테스트 메서드 명명 규칙:
```kotlin
// Given-When-Then 패턴
@Test
fun `getCurrentPrice should return AAPL price data when symbol is valid`() = runTest {
    // Given
    val symbol = "AAPL"

    // When
    val price = ufc.price.getCurrentPrice(symbol)

    // Then
    assertThat(price.symbol).isEqualTo("AAPL")
    assertThat(price.lastPrice).isGreaterThan(0.0)
}
```

### 3. Fake 사용 (Test Doubles)

외부 의존성을 제거하기 위해 Fake 구현체를 사용합니다:

```kotlin
// 단위 테스트에서 Fake 사용
val fakeService = FakePriceService()
val api = PriceApiImpl(fakeService)

// 통합 테스트에서 실제 구현 사용
val ufc = UFC.create(UFCClientConfig())
```

### 4. 테스트 격리

- 각 테스트는 독립적으로 실행
- `@BeforeEach`, `@AfterEach` 사용
- 테스트 간 상태 공유 금지

```kotlin
@BeforeEach
fun setUp() = runTest {
    ufc = UFC.create(UFCClientConfig())
}

@AfterEach
fun tearDown() {
    ufc.close()
}
```

### 5. 적절한 Assertion 사용

```kotlin
// AssertJ 사용 (가독성 높음)
assertThat(price.lastPrice).isGreaterThan(0.0)
assertThat(companies).hasSize(3)
assertThat(dates).isSorted()

// 여러 assertion 조합
assertThat(fundData)
    .isNotNull()
    .satisfies {
        assertThat(it.symbol).isEqualTo("SPY")
        assertThat(it.totalAssets).isGreaterThan(0.0)
    }
```

---

## 테스트 커버리지

### 커버리지 확인

```bash
# Jacoco 플러그인 사용 (build.gradle.kts에 설정 필요)
./gradlew test jacocoTestReport

# 리포트 확인
open build/reports/jacoco/test/html/index.html
```

### 커버리지 목표

- **단위 테스트:** 80% 이상
- **통합 테스트:** 주요 시나리오 100% 커버
- **Critical Path:** 100% 커버

---

## 문제 해결

### 통합 테스트 실패 시

1. **네트워크 연결 확인**
   ```bash
   ping query1.finance.yahoo.com
   curl https://api.stlouisfed.org/fred/series?series_id=GDP&api_key=YOUR_KEY
   ```

2. **API 키 확인**
   ```bash
   echo $FRED_API_KEY
   # 출력이 없으면 환경 변수 설정 안 됨
   ```

3. **Rate Limiting**
   - Yahoo Finance는 요청 제한이 있습니다
   - 테스트 실행 간격을 두세요
   - 병렬 테스트를 피하세요 (이미 설정됨)

4. **타임아웃**
   - 네트워크가 느린 경우 타임아웃 증가
   - `build.gradle.kts`의 `timeout` 설정 조정

### 단위 테스트 실패 시

1. **Fake 구현 확인**
   - Fake 서비스가 올바르게 동작하는지 확인
   - 테스트 데이터가 적절한지 확인

2. **테스트 격리 확인**
   - 다른 테스트의 영향을 받지 않는지 확인
   - `@BeforeEach`에서 초기화가 올바른지 확인

---

## 추가 자료

- [테스트 원칙 문서](../doc/test-principle.md)
- [UFC API 문서](../README.md)
- [클린 아키텍처 가이드](../doc/NAMESPACE_ARCHITECTURE_SUMMARY.md)

---

## 요약

```bash
# 빠른 테스트 (개발 중)
./gradlew test

# 통합 테스트 (배포 전)
export FRED_API_KEY=your_key
./gradlew integrationTest

# 전체 테스트 (CI/CD)
./gradlew test integrationTest
```

**테스트 작성 가이드:**
1. 단위 테스트 먼저 작성 (TDD)
2. Fake 구현체 사용
3. Given-When-Then 패턴
4. 명확한 테스트 이름
5. 적절한 Assertion 사용
