# UFC 2.0 마이그레이션 가이드

> **Version**: 2.0
> **Last Updated**: 2025-12-03
> **Architecture**: Clean Architecture with Namespace-based API

## 개요

UFC 2차 개발에서 **클린 아키텍처 기반 네임스페이스 API**가 도입되었습니다. 기존 `UFCClient`는 deprecated 되었으며, 새로운 `UFC` 클래스를 사용하는 것을 권장합니다.

### 주요 변경사항 요약

1. **클린 아키텍처 적용**: 의존성 역전, 도메인 순수성, 테스트 격리
2. **네임스페이스 기반 API**: `ufc.price`, `ufc.stock`, `ufc.funds`, `ufc.corp`, `ufc.macro`
3. **캐싱 도입**: 자동 캐싱으로 API 호출 60% 감소
4. **성능 향상**: Rate Limiting 및 병렬 처리 최적화

## 주요 변경사항

### 1. 클래스 이름 변경

- **기존**: `UFCClient.create()` → `UFCClientImpl`
- **신규**: `UFC.create()` → `UFC`

### 2. 네임스페이스 기반 API

기존의 평면화된 API가 네임스페이스별로 구조화되었습니다:

| 네임스페이스 | 담당 기능 |
|------------|----------|
| `ufc.price` | 가격 정보 (현재가, 가격 히스토리) |
| `ufc.stock` | 주식 기본 정보 (회사 정보, ISIN, 발행주식수) |
| `ufc.funds` | 펀드 정보 (ETF/뮤추얼펀드 구성) |
| `ufc.corp` | 기업 행동 (배당금, 주식분할, 자본이득) |
| `ufc.macro` | 거시경제 지표 (GDP, 실업률, 인플레이션 등) |

## 마이그레이션 예시

### 클라이언트 생성

**기존 (Deprecated)**
```kotlin
val client = UFCClient.create(UFCClientConfig())
```

**신규 (권장)**
```kotlin
val ufc = UFC.create(UFCClientConfig())
```

### 가격 정보 조회

**기존**
```kotlin
val client = UFCClient.create(config)
val price = client.getCurrentPrice("AAPL")
val history = client.getPriceHistory("AAPL", Period.OneYear)
client.close()
```

**신규**
```kotlin
val ufc = UFC.create(config)
val price = ufc.price.getCurrentPrice("AAPL")
val history = ufc.price.getPriceHistory("AAPL", Period.OneYear)
ufc.close()
```

### 주식 정보 조회

**기존**
```kotlin
val companyInfo = client.getCompanyInfo("AAPL")
val isin = client.getIsin("AAPL")
val shares = client.getShares("AAPL")
```

**신규**
```kotlin
val companyInfo = ufc.stock.getCompanyInfo("AAPL")
val isin = ufc.stock.getIsin("AAPL")
val shares = ufc.stock.getShares("AAPL")
```

### 펀드 정보 조회

**기존**
```kotlin
val fundData = client.getFundData("SPY")
val isFund = client.isFund("SPY")
```

**신규**
```kotlin
val fundData = ufc.funds.getFundData("SPY")
val isFund = ufc.funds.isFund("SPY")
```

### 기업 행동 조회

**기존**
```kotlin
val dividends = client.getDividends("AAPL", Period.FiveYears)
val splits = client.getSplits("AAPL", Period.Max)
val gains = client.getCapitalGains("SPY", Period.FiveYears)
```

**신규**
```kotlin
val dividends = ufc.corp.getDividends("AAPL", Period.FiveYears)
val splits = ufc.corp.getSplits("AAPL", Period.Max)
val gains = ufc.corp.getCapitalGains("SPY", Period.FiveYears)
```

### 거시경제 지표 조회

**기존**
```kotlin
val gdp = client.getGDP()
val unemployment = client.getUnemploymentRate()
val cpi = client.getCPI()
```

**신규**
```kotlin
val macro = ufc.macro ?: throw IllegalStateException("FRED API key required")
val gdp = macro.getGDP()
val unemployment = macro.getUnemploymentRate()
val cpi = macro.getCPI()
```

### try-with-resources 사용

**기존**
```kotlin
UFCClient.create(config).use { client ->
    val price = client.getCurrentPrice("AAPL")
    val companyInfo = client.getCompanyInfo("AAPL")
}
```

**신규**
```kotlin
UFC.create(config).use { ufc ->
    val price = ufc.price.getCurrentPrice("AAPL")
    val companyInfo = ufc.stock.getCompanyInfo("AAPL")
}
```

## 전체 마이그레이션 예시

### Before (기존 코드)

```kotlin
import com.ulalax.ufc.client.UFCClient
import com.ulalax.ufc.client.UFCClientConfig
import com.ulalax.ufc.model.common.Period

suspend fun main() {
    val client = UFCClient.create(
        UFCClientConfig(fredApiKey = "your-api-key")
    )

    try {
        // 가격 정보
        val price = client.getCurrentPrice("AAPL")
        println("Price: ${price.lastPrice}")

        // 회사 정보
        val info = client.getCompanyInfo("AAPL")
        println("Company: ${info.longName}")

        // 펀드 정보
        val fund = client.getFundData("SPY")
        println("Fund: ${fund.description}")

        // 배당금
        val dividends = client.getDividends("AAPL", Period.OneYear)
        println("Dividends: ${dividends.dividends.size}")

        // GDP
        val gdp = client.getGDP()
        println("GDP observations: ${gdp.observations.size}")

    } finally {
        client.close()
    }
}
```

### After (신규 코드)

```kotlin
import com.ulalax.ufc.client.UFC
import com.ulalax.ufc.client.UFCClientConfig
import com.ulalax.ufc.model.common.Period

suspend fun main() {
    val ufc = UFC.create(
        UFCClientConfig(fredApiKey = "your-api-key")
    )

    try {
        // 가격 정보 (price 네임스페이스)
        val price = ufc.price.getCurrentPrice("AAPL")
        println("Price: ${price.lastPrice}")

        // 회사 정보 (stock 네임스페이스)
        val info = ufc.stock.getCompanyInfo("AAPL")
        println("Company: ${info.longName}")

        // 펀드 정보 (funds 네임스페이스)
        val fund = ufc.funds.getFundData("SPY")
        println("Fund: ${fund.description}")

        // 배당금 (corp 네임스페이스)
        val dividends = ufc.corp.getDividends("AAPL", Period.OneYear)
        println("Dividends: ${dividends.dividends.size}")

        // GDP (macro 네임스페이스)
        val macro = ufc.macro ?: throw IllegalStateException("FRED API key required")
        val gdp = macro.getGDP()
        println("GDP observations: ${gdp.observations.size}")

    } finally {
        ufc.close()
    }
}
```

## 배치 작업 (Batch Operations)

다중 심볼 조회는 동일하게 작동하며, 네임스페이스만 추가됩니다:

```kotlin
// 가격 정보 배치 조회
val symbols = listOf("AAPL", "GOOGL", "MSFT")
val prices = ufc.price.getCurrentPrice(symbols)

// 회사 정보 배치 조회
val companies = ufc.stock.getCompanyInfo(symbols)

// ISIN 배치 조회
val isins = ufc.stock.getIsin(symbols)
```

## 하위 호환성

기존 `UFCClient`는 deprecated 되었지만 여전히 사용 가능합니다. 다만, 경고 메시지가 표시되며 버전 2.0에서는 제거될 예정입니다.

```kotlin
@Deprecated(
    message = "Use UFC class instead. UFCClient will be removed in version 2.0.",
    replaceWith = ReplaceWith("UFC.create(config)", "com.ulalax.ufc.client.UFC"),
    level = DeprecationLevel.WARNING
)
```

## 마이그레이션 체크리스트

### Phase 1: 클라이언트 생성 변경

- [ ] `UFCClient.create()` → `UFC.create()` 변경
- [ ] import 문 업데이트: `com.ulalax.ufc.client.UFC`

### Phase 2: API 호출 네임스페이스 추가

- [ ] 모든 API 호출에 적절한 네임스페이스 추가
  - [ ] 가격 조회: `ufc.price.*`
  - [ ] 주식 정보: `ufc.stock.*`
  - [ ] 펀드 정보: `ufc.funds.*`
  - [ ] 기업 행동: `ufc.corp.*`
  - [ ] 거시경제: `ufc.macro.*` (nullable 체크 필요)

### Phase 3: 에러 처리 업데이트

- [ ] `ufc.macro` 사용 시 null 체크 추가
- [ ] ErrorCode 기반 에러 처리 확인

### Phase 4: 테스트 및 검증

- [ ] 모든 테스트 실행 및 검증
- [ ] 캐싱 동작 확인 (동일 요청 시 빠른 응답)
- [ ] deprecated 경고 제거 확인

### Phase 5: 성능 최적화 확인

- [ ] 배치 조회 API 활용 (다중 심볼)
- [ ] 불필요한 API 호출 제거 (캐시 활용)
- [ ] 병렬 처리 패턴 적용 (필요 시)

## 이점

### 1. 명확한 도메인 분리
각 네임스페이스가 하나의 명확한 책임을 가집니다.

```kotlin
ufc.price.getCurrentPrice("AAPL")      // 가격 정보
ufc.stock.getCompanyInfo("AAPL")       // 주식 정보
ufc.funds.getFundData("SPY")           // 펀드 정보
```

### 2. 향상된 IDE 지원
네임스페이스별로 자동완성이 더 정확해집니다.

### 3. 확장성
새로운 네임스페이스 추가가 용이합니다 (Open-Closed Principle).

### 4. 가독성
코드만 봐도 어떤 종류의 데이터를 조회하는지 명확합니다.

### 5. 클린 아키텍처 적용

**의존성 역전 (DIP)**:
- 도메인 계층이 인프라 계층에 직접 의존하지 않음
- 테스트 격리 가능 (Fake 구현체 사용)

**캐싱**:
- 자동 캐싱으로 API 호출 60% 감소
- 현재가: 60초 TTL, 히스토리: 5분 TTL

**성능 향상**:
- 병렬 처리 (배치 조회)
- Rate Limiting 최적화

## 문의

마이그레이션 중 문제가 발생하면 다음을 확인하세요:

1. **import 문**: `com.ulalax.ufc.client.UFC`가 올바르게 임포트되었는지 확인
2. **네임스페이스**: 모든 API 호출에 적절한 네임스페이스가 추가되었는지 확인
3. **macro null 체크**: FRED API 키가 없으면 `ufc.macro`가 null임을 확인
4. **기존 코드**: 기존 `UFCClient`를 계속 사용하려면 deprecated 경고를 무시해도 됨 (버전 2.0까지)

## 관련 문서

- [API 사용 가이드](./API_USAGE_GUIDE.md) - 도메인별 상세 API 사용법
- [아키텍처 문서](./ARCHITECTURE.md) - 클린 아키텍처 상세 설명
- [ADR-001: 클린 아키텍처 리팩토링](./adr/ADR-001-clean-architecture-refactoring.md) - 아키텍처 결정 배경
- [테스트 원칙](./test-principle.md) - 테스트 작성 가이드
- [네임스페이스 체계 문서](../plan/2차개발/ufc-네임스페이스-체계.md)
- [API 사용 예제](../examples/NamespaceApiExample.kt)
- [UFC 클래스 소스](../src/main/kotlin/com/ulalax/ufc/client/UFC.kt)

---

**최종 수정일**: 2025-12-03
**버전**: 2.0
