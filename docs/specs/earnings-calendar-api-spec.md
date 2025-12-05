# Earnings Calendar API 기능 명세서

**작성일**: 2025-12-05
**대상 프로젝트**: UFC (Unified Finance Client)
**참조**: yfinance v0.2.x Earnings Calendar 구현

---

## 1. API 개요

### 1.1 목적

Earnings Calendar API는 Yahoo Finance의 실적 발표 일정 데이터를 제공하는 기능입니다. 특정 종목(symbol)에 대한 과거 및 미래 실적 발표 일정, EPS 추정치, 실제 발표 수치, 서프라이즈 퍼센트 등을 조회할 수 있습니다.

### 1.2 주요 사용 사례

1. **실적 발표 일정 조회**: 특정 종목의 다음 실적 발표 예정일 확인
2. **어닝 서프라이즈 분석**: 과거 실적에서 예상치 대비 실제 수치 비교 분석
3. **역사적 실적 추적**: 여러 분기/연도에 걸친 EPS 추이 분석
4. **투자 의사결정 지원**: 실적 발표 전후 포지션 조정을 위한 일정 파악
5. **백테스팅**: 과거 실적 발표 데이터를 활용한 전략 검증

### 1.3 QuoteSummary의 earningsDates 모듈과의 차이점

| 구분 | Earnings Calendar API | QuoteSummary.EARNINGS_DATES |
|------|----------------------|----------------------------|
| **데이터 소스** | 웹 스크래핑 (HTML 파싱) | JSON API 엔드포인트 |
| **제공 데이터 양** | 최대 100개 레코드 (설정 가능) | 제한적 (보통 4~12개) |
| **과거 데이터** | 수년 전까지 조회 가능 | 제한적 |
| **Offset 지원** | 지원 (pagination) | 미지원 |
| **응답 형식** | 타임스탬프 기반 상세 데이터 | 단순화된 날짜 정보 |
| **사용 목적** | 상세한 역사적 분석 | 간단한 다음 실적일 조회 |
| **안정성** | 상대적으로 낮음 (HTML 구조 변경에 취약) | 상대적으로 높음 (API 엔드포인트) |

**권장 사용 시나리오**:
- QuoteSummary.EARNINGS_DATES: 다음 실적 발표일만 빠르게 확인
- Earnings Calendar API: 상세한 과거 실적 히스토리 분석 필요 시

---

## 2. Yahoo Finance Earnings Calendar API 분석

### 2.1 Endpoint 정보

**URL**: `https://finance.yahoo.com/calendar/earnings`

**HTTP Method**: `GET`

**인증**: Yahoo Finance 웹 세션 (Crumb 토큰 불필요)

### 2.2 요청 파라미터

| 파라미터 | 타입 | 필수 | 설명 | 제약사항 |
|---------|------|-----|------|---------|
| `symbol` | String | 필수 | 조회할 종목 심볼 (예: AAPL, MSFT) | Yahoo Finance 지원 심볼 |
| `offset` | Integer | 선택 | 페이지네이션 오프셋 | 0 이상, 기본값 0 |
| `size` | Integer | 선택 | 조회할 레코드 수 | 25, 50, 100 중 선택 |

**파라미터 상세**:

1. **symbol**:
   - 대소문자 구분 없음 (자동으로 대문자 변환)
   - 유효하지 않은 심볼 입력 시 빈 테이블 반환

2. **offset**:
   - `offset=0`: 가장 최근 미래 실적 발표부터 시작
   - `offset=1`: 가장 최근 과거 실적 발표부터 시작
   - `offset=n`: n번째 과거 실적부터 시작
   - offset 증가 시 더 오래된 과거 데이터 조회

3. **size**:
   - `limit <= 25`: size=25 사용
   - `25 < limit <= 50`: size=50 사용
   - `50 < limit <= 100`: size=100 사용
   - `limit > 100`: 에러 처리 (Yahoo Finance 제한)

**요청 예시**:
```
GET https://finance.yahoo.com/calendar/earnings?symbol=AAPL&offset=0&size=25
GET https://finance.yahoo.com/calendar/earnings?symbol=MSFT&offset=10&size=50
GET https://finance.yahoo.com/calendar/earnings?symbol=TSLA&offset=0&size=100
```

### 2.3 응답 구조 분석

#### 2.3.1 응답 형식

Yahoo Finance는 HTML 페이지를 반환하며, 실적 데이터는 `<table>` 태그 내에 포함되어 있습니다.

**HTML 구조**:
```html
<table>
  <thead>
    <tr>
      <th>Symbol</th>
      <th>Company</th>
      <th>Earnings Date</th>
      <th>EPS Estimate</th>
      <th>Reported EPS</th>
      <th>Surprise (%)</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>AAPL</td>
      <td>Apple Inc.</td>
      <td>October 30, 2025 at 4 PM EDT</td>
      <td>2.97</td>
      <td>-</td>
      <td>-</td>
    </tr>
    <!-- 추가 행들... -->
  </tbody>
</table>
```

#### 2.3.2 응답 필드 상세

| 필드명 | 데이터 타입 | Nullable | 설명 |
|--------|-----------|----------|------|
| Symbol | String | No | 종목 심볼 (요청한 symbol과 동일) |
| Company | String | No | 회사명 |
| Earnings Date | String | No | 실적 발표 날짜 및 시간 (타임존 포함) |
| EPS Estimate | Double | Yes | 애널리스트 EPS 추정치 |
| Reported EPS | Double | Yes | 실제 발표된 EPS |
| Surprise (%) | Double | Yes | 서프라이즈 퍼센트 (실제-추정)/추정 * 100 |

**필드별 특성**:

1. **Earnings Date**:
   - 형식: `"Month DD, YYYY at H[H] AM/PM TZ"`
   - 예시: `"October 30, 2025 at 4 PM EDT"`, `"July 22, 2025 at 10 AM EST"`
   - 타임존: EDT, EST, PST, PDT 등 (실제로는 "America/New_York" 등으로 변환 필요)

2. **EPS Estimate**:
   - 미래 실적: 애널리스트 컨센서스 추정치
   - 과거 실적: 발표 전 추정치 (역사적 데이터)
   - 값이 없을 경우: "-" (HTML 상에서) → null로 파싱

3. **Reported EPS**:
   - 미래 실적: "-" (아직 발표되지 않음) → null
   - 과거 실적: 실제 발표된 EPS 수치

4. **Surprise (%)**:
   - 공식: `((Reported EPS - EPS Estimate) / EPS Estimate) * 100`
   - 미래 실적: "-" → null
   - 양수: 추정치 초과 (긍정적 서프라이즈)
   - 음수: 추정치 미달 (부정적 서프라이즈)

#### 2.3.3 빈 응답 처리

- 유효하지 않은 symbol 입력 시: `<table>` 태그는 존재하나 `<tbody>`가 비어있음
- 상장폐지된 종목: 일반적으로 빈 테이블 반환
- offset이 데이터 범위를 초과한 경우: 빈 테이블 반환

### 2.4 웹 스크래핑 방식 vs API 방식 비교

| 구분 | 웹 스크래핑 (현재 방식) | JSON API (대안) |
|------|---------------------|----------------|
| **URL** | `finance.yahoo.com/calendar/earnings` | `query1.finance.yahoo.com/v1/finance/visualization` |
| **응답 형식** | HTML | JSON |
| **파싱 난이도** | 높음 (BeautifulSoup 필요) | 낮음 (kotlinx.serialization) |
| **안정성** | 낮음 (HTML 구조 변경에 취약) | 중간 (API 변경 가능성) |
| **성능** | 낮음 (HTML 파싱 오버헤드) | 높음 (직접 JSON 디코딩) |
| **데이터 품질** | 동일 | 동일 |
| **yfinance 사용** | v0.2.x 이후 주요 방식 | v0.2.x 이전 방식 (현재 deprecated) |

**UFC 권장 방식**: 웹 스크래핑 (HTML 파싱)

**근거**:
1. yfinance의 최신 구현(_get_earnings_dates_using_scrape)이 이 방식 채택
2. JSON API 엔드포인트는 2025년 여름 이후 Yahoo가 업데이트 중단
3. 이미 UFC에는 HTTP 클라이언트 및 파싱 인프라 존재
4. Kotlin의 kotlinx.html 라이브러리로 안전한 HTML 파싱 가능

---

## 3. UFC 통합 설계

### 3.1 기존 아키텍처와의 통합 방안

UFC는 현재 다음과 같은 구조를 가지고 있습니다:

```
ufc/
├── Ufc.kt (메인 클라이언트)
├── yahoo/
│   ├── YahooClient.kt
│   ├── model/ (public 도메인 모델)
│   └── internal/ (내부 응답 모델)
├── fred/
├── businessinsider/
└── common/ (공통 인프라)
```

**통합 전략**:

1. **YahooClient 확장**: 기존 YahooClient 클래스에 새 메서드 추가
   - `suspend fun earningsCalendar(symbol: String, limit: Int, offset: Int): EarningsCalendar`

2. **일관된 API 스타일 유지**: 기존 `quoteSummary()`, `chart()` 메서드와 동일한 패턴

3. **공통 인프라 활용**:
   - YahooAuthenticator (Crumb는 불필요하지만 세션 관리 활용)
   - GlobalRateLimiters (기존 Yahoo Rate Limiter 재사용)
   - 기존 HttpClient (Ktor CIO 엔진)

4. **에러 처리 통일**: 기존 ApiException, DataParsingException 활용

### 3.2 네임스페이스 배치

**제안**: 기존 `YahooClient` 클래스에 직접 메서드 추가 (별도 네임스페이스 불필요)

**근거**:
1. Earnings Calendar는 Yahoo Finance 고유 기능
2. 현재 UFC는 `ufc.yahoo`, `ufc.fred`, `ufc.businessInsider` 3개의 클라이언트만 존재
3. calendar 전용 네임스페이스 생성은 과도한 분리
4. yfinance도 `Ticker.get_earnings_dates()` 형태로 통합

**API 사용 예시**:
```kotlin
val ufc = Ufc.create()
val calendar = ufc.yahoo.earningsCalendar("AAPL", limit = 20, offset = 0)
```

**대안** (미래 확장 고려):
만약 향후 다른 종류의 캘린더 데이터(배당금 캘린더, IPO 캘린더 등)가 추가될 경우:
```kotlin
// 미래 확장 시나리오
val earnings = ufc.calendar.earnings("AAPL")
val dividends = ufc.calendar.dividends("AAPL")
val ipos = ufc.calendar.ipos(date = LocalDate.now())
```
현재는 이런 확장 계획이 없으므로 **YahooClient 직접 추가를 권장**합니다.

### 3.3 필요한 모델 클래스 목록

#### 3.3.1 Public 도메인 모델 (yahoo/model/)

**EarningsCalendar** (응답 루트)
- 역할: Earnings Calendar API의 최종 응답 객체
- 주요 필드:
  - symbol: String (조회한 종목)
  - events: List<EarningsEvent> (실적 이벤트 목록)
  - requestedLimit: Int (요청한 limit)
  - requestedOffset: Int (요청한 offset)
  - actualCount: Int (실제 반환된 이벤트 수)

**EarningsEvent** (개별 실적 이벤트)
- 역할: 하나의 실적 발표 일정/결과 표현
- 주요 필드:
  - earningsDate: Instant (실적 발표 일시, UTC 기준)
  - timeZone: String (원본 타임존, 예: "America/New_York")
  - epsEstimate: Double? (EPS 추정치)
  - reportedEps: Double? (실제 발표된 EPS)
  - surprisePercent: Double? (서프라이즈 퍼센트)

**EarningsCalendarParams** (요청 파라미터)
- 역할: API 요청 파라미터 검증 및 캡슐화
- 주요 필드:
  - symbol: String
  - limit: Int (1~100, 기본값 12)
  - offset: Int (0 이상, 기본값 0)
- 검증 로직:
  - limit 범위 체크
  - offset 음수 체크
  - symbol 빈 문자열 체크

#### 3.3.2 Internal 응답 모델 (yahoo/internal/response/)

**EarningsCalendarHtmlResponse** (HTML 파싱 중간 결과)
- 역할: BeautifulSoup(또는 Kotlin 파서)로 파싱한 테이블 데이터 표현
- 주요 필드:
  - hasTable: Boolean (테이블 존재 여부)
  - rows: List<EarningsTableRow> (테이블 행 목록)

**EarningsTableRow** (HTML 테이블 행)
- 역할: HTML <tr> 하나를 나타내는 원시 데이터
- 주요 필드:
  - symbol: String
  - company: String
  - earningsDateRaw: String (파싱 전 원본 문자열)
  - epsEstimateRaw: String
  - reportedEpsRaw: String
  - surprisePercentRaw: String

### 3.4 API 메서드 시그니처 정의

#### 3.4.1 YahooClient 확장 메서드

```kotlin
/**
 * Earnings Calendar API를 호출하여 실적 발표 일정을 조회합니다.
 *
 * @param symbol 조회할 종목 심볼 (예: "AAPL")
 * @param limit 조회할 레코드 수 (1~100, 기본값 12)
 * @param offset 페이지네이션 오프셋 (0 이상, 기본값 0)
 * @return EarningsCalendar
 * @throws ValidationException 유효하지 않은 파라미터
 * @throws ApiException API 호출 실패
 * @throws DataParsingException HTML 파싱 실패
 */
suspend fun earningsCalendar(
    symbol: String,
    limit: Int = 12,
    offset: Int = 0
): EarningsCalendar
```

#### 3.4.2 Ufc 클라이언트 직접 접근 메서드 (선택적)

현재 UFC는 `ufc.quoteSummary()`, `ufc.chart()` 같은 직접 접근 메서드를 제공합니다.
일관성을 위해 다음 메서드도 추가 고려:

```kotlin
// Ufc.kt에 추가
/**
 * Yahoo Finance Earnings Calendar에 직접 접근합니다.
 *
 * @param symbol 조회할 종목 심볼
 * @param limit 조회할 레코드 수 (기본값 12)
 * @param offset 페이지네이션 오프셋 (기본값 0)
 * @return EarningsCalendar
 */
suspend fun earningsCalendar(
    symbol: String,
    limit: Int = 12,
    offset: Int = 0
): EarningsCalendar = yahoo.earningsCalendar(symbol, limit, offset)
```

---

## 4. 데이터 매핑

### 4.1 Yahoo 응답 필드 → UFC 도메인 모델 매핑

| Yahoo HTML 필드 | 원시 타입 | UFC 도메인 필드 | 변환 타입 | 매핑 규칙 |
|----------------|---------|---------------|----------|----------|
| Earnings Date | String | earningsDate | Instant | 날짜 파싱 + 타임존 변환 |
| Earnings Date | String | timeZone | String | 타임존 추출 (예: "EDT" → "America/New_York") |
| EPS Estimate | String | epsEstimate | Double? | "-" → null, 숫자 → Double |
| Reported EPS | String | reportedEps | Double? | "-" → null, 숫자 → Double |
| Surprise (%) | String | surprisePercent | Double? | "-" → null, 숫자 → Double |

### 4.2 타입 변환 규칙

#### 4.2.1 날짜/시간 변환

**입력 형식**: `"October 30, 2025 at 4 PM EDT"`

**변환 단계**:
1. 타임존 문자열 추출: "EDT"
2. 타임존 매핑: "EDT" → "America/New_York"
3. 날짜/시간 파싱: "October 30, 2025 at 4 PM"
4. LocalDateTime 생성
5. ZonedDateTime 변환 (타임존 적용)
6. Instant로 변환 (UTC 기준)

**타임존 매핑 테이블**:
| Yahoo 타임존 | IANA 타임존 |
|-------------|-----------|
| EDT | America/New_York |
| EST | America/New_York |
| PDT | America/Los_Angeles |
| PST | America/Los_Angeles |
| CDT | America/Chicago |
| CST | America/Chicago |
| MDT | America/Denver |
| MST | America/Denver |

**날짜 파싱 포맷**: `"MMMM d, yyyy 'at' h a"` (예: "October 30, 2025 at 4 PM")

**구현 고려사항**:
- Kotlin의 `java.time.format.DateTimeFormatter` 활용
- Locale.US 사용 (월 이름이 영어)
- 파싱 실패 시 DataParsingException 발생

#### 4.2.2 숫자 변환

**입력**: String (예: "2.97", "-", "10.88")

**변환 규칙**:
1. "-" 또는 빈 문자열: null
2. 유효한 숫자: `toDoubleOrNull()` 사용
3. 파싱 실패: DataParsingException

**예외 처리**:
```kotlin
fun parseEps(raw: String): Double? {
    return when {
        raw == "-" || raw.isBlank() -> null
        else -> raw.toDoubleOrNull()
            ?: throw DataParsingException("Invalid EPS value: $raw")
    }
}
```

### 4.3 Nullable 처리 전략

#### 4.3.1 필수 필드 (Non-null)

- **earningsDate**: 항상 존재해야 함
  - 없을 경우: DataParsingException 발생
  - 해당 행을 건너뛰기 (로그 경고)

- **symbol**: 항상 존재 (요청 파라미터와 동일)

#### 4.3.2 선택적 필드 (Nullable)

- **epsEstimate**:
  - 미래 실적: 일반적으로 존재
  - 과거 실적: 존재 (역사적 추정치)
  - 일부 종목: 애널리스트 커버리지 없으면 null

- **reportedEps**:
  - 미래 실적: 항상 null
  - 과거 실적: 일반적으로 존재
  - 특수 상황(실적 발표 취소 등): null 가능

- **surprisePercent**:
  - 미래 실적: 항상 null
  - 과거 실적: reportedEps와 epsEstimate 둘 다 존재 시 계산됨
  - 하나라도 null이면: null

#### 4.3.3 Null 처리 정책

**원칙**: "Be liberal in what you accept, be conservative in what you send"

1. **파싱 시**:
   - 필수 필드 누락: 해당 행 스킵 + 로그 경고
   - 선택적 필드 누락: null로 설정
   - 전체 행 파싱 실패: 에러 누적 후 최종 판단

2. **검증**:
   - 전체 행의 50% 이상 파싱 실패: DataParsingException
   - 50% 미만: 성공한 데이터만 반환 + 로그 경고

3. **응답 생성**:
   - 빈 이벤트 리스트: 정상 응답 (actualCount = 0)
   - 부분 실패: 성공한 데이터 반환 + 메타데이터에 실패 카운트 기록

---

## 5. 에러 처리

### 5.1 예상되는 에러 케이스

#### 5.1.1 입력 검증 에러

| 에러 시나리오 | ErrorCode | HTTP Status | 처리 방법 |
|-------------|-----------|-------------|----------|
| symbol이 빈 문자열 | INVALID_PARAMETER | N/A | ValidationException 발생 |
| limit < 1 또는 > 100 | INVALID_PARAMETER | N/A | ValidationException 발생 |
| offset < 0 | INVALID_PARAMETER | N/A | ValidationException 발생 |

**검증 로직**:
```kotlin
fun validateParams(symbol: String, limit: Int, offset: Int) {
    require(symbol.isNotBlank()) { "Symbol must not be blank" }
    require(limit in 1..100) { "Limit must be between 1 and 100" }
    require(offset >= 0) { "Offset must be non-negative" }
}
```

#### 5.1.2 네트워크 에러

| 에러 시나리오 | ErrorCode | HTTP Status | 처리 방법 |
|-------------|-----------|-------------|----------|
| 타임아웃 | NETWORK_ERROR | N/A | ApiException + 재시도 제안 |
| 연결 실패 | NETWORK_ERROR | N/A | ApiException |
| DNS 실패 | NETWORK_ERROR | N/A | ApiException |

#### 5.1.3 HTTP 에러

| 에러 시나리오 | ErrorCode | HTTP Status | 처리 방법 |
|-------------|-----------|-------------|----------|
| 404 Not Found | DATA_NOT_FOUND | 404 | 심볼 존재하지 않음 |
| 429 Too Many Requests | RATE_LIMIT_EXCEEDED | 429 | RateLimitException |
| 500 Internal Server Error | EXTERNAL_API_ERROR | 500 | ApiException |
| 503 Service Unavailable | EXTERNAL_API_ERROR | 503 | ApiException |

#### 5.1.4 파싱 에러

| 에러 시나리오 | ErrorCode | 처리 방법 |
|-------------|-----------|----------|
| HTML에 `<table>` 태그 없음 | DATA_PARSING_ERROR | DataParsingException |
| 날짜 파싱 실패 (형식 변경) | DATA_PARSING_ERROR | DataParsingException |
| 숫자 파싱 실패 (예상 외 형식) | DATA_PARSING_ERROR | 해당 행 스킵 + 로그 |
| 전체 행의 50% 이상 실패 | DATA_PARSING_ERROR | DataParsingException |

#### 5.1.5 데이터 에러

| 에러 시나리오 | ErrorCode | 처리 방법 |
|-------------|-----------|----------|
| 유효하지 않은 심볼 | DATA_NOT_FOUND | 빈 리스트 반환 (에러 아님) |
| 상장폐지 종목 | DATA_NOT_FOUND | 빈 리스트 반환 |
| offset 범위 초과 | N/A | 빈 리스트 반환 (정상) |

### 5.2 에러 응답 구조

#### 5.2.1 ValidationException 예시

```kotlin
throw ValidationException(
    errorCode = ErrorCode.INVALID_PARAMETER,
    message = "Limit must be between 1 and 100, got: $limit",
    field = "limit",
    metadata = mapOf(
        "symbol" to symbol,
        "limit" to limit,
        "offset" to offset
    )
)
```

#### 5.2.2 ApiException 예시 (HTTP 에러)

```kotlin
throw ApiException(
    errorCode = ErrorCode.EXTERNAL_API_ERROR,
    message = "Yahoo Finance Earnings Calendar API returned HTTP ${response.status.value}",
    statusCode = response.status.value,
    metadata = mapOf(
        "symbol" to symbol,
        "limit" to limit,
        "offset" to offset,
        "url" to requestUrl
    )
)
```

#### 5.2.3 DataParsingException 예시

```kotlin
throw DataParsingException(
    errorCode = ErrorCode.DATA_PARSING_ERROR,
    message = "Failed to parse earnings date: $earningsDateRaw",
    sourceData = earningsDateRaw,
    metadata = mapOf(
        "symbol" to symbol,
        "row" to rowIndex
    )
)
```

#### 5.2.4 RateLimitException 예시

```kotlin
throw RateLimitException(
    errorCode = ErrorCode.RATE_LIMIT_EXCEEDED,
    message = "Yahoo Finance rate limit exceeded",
    retryAfterSeconds = 60,
    metadata = mapOf(
        "symbol" to symbol,
        "statusCode" to 429
    )
)
```

### 5.3 에러 핸들링 가이드 (사용자 관점)

```kotlin
try {
    val calendar = ufc.yahoo.earningsCalendar("AAPL", limit = 20)
    // 정상 처리
} catch (e: ValidationException) {
    // 입력 파라미터 오류 - 파라미터 수정 필요
    logger.error("Invalid parameter: ${e.field} - ${e.message}")
} catch (e: RateLimitException) {
    // Rate Limit 초과 - 재시도 대기
    val retryAfter = e.retryAfterSeconds ?: 60
    logger.warn("Rate limited, retry after $retryAfter seconds")
} catch (e: DataParsingException) {
    // 파싱 실패 - Yahoo Finance HTML 구조 변경 가능성
    logger.error("Failed to parse response: ${e.message}")
    // 개발팀에 리포트 필요
} catch (e: ApiException) {
    // API 호출 실패 - 네트워크/서버 문제
    when (e.errorCode) {
        ErrorCode.DATA_NOT_FOUND -> {
            logger.info("No earnings data found for symbol")
        }
        ErrorCode.NETWORK_ERROR -> {
            logger.error("Network error, retry later")
        }
        else -> {
            logger.error("API error: ${e.message}")
        }
    }
} catch (e: UfcException) {
    // 기타 UFC 에러
    logger.error("UFC error: ${e.errorCode} - ${e.message}")
}
```

---

## 6. 테스트 전략

### 6.1 단위 테스트 시나리오

#### 6.1.1 파라미터 검증 테스트

**테스트 클래스**: `EarningsCalendarParamsTest`

| 테스트 케이스 | 입력 | 예상 결과 |
|-------------|-----|----------|
| 정상 파라미터 | symbol="AAPL", limit=12, offset=0 | 검증 통과 |
| 빈 심볼 | symbol="", limit=12, offset=0 | ValidationException |
| limit 범위 초과 (하한) | symbol="AAPL", limit=0, offset=0 | ValidationException |
| limit 범위 초과 (상한) | symbol="AAPL", limit=101, offset=0 | ValidationException |
| 음수 offset | symbol="AAPL", limit=12, offset=-1 | ValidationException |
| 경계값 (limit=1) | symbol="AAPL", limit=1, offset=0 | 검증 통과 |
| 경계값 (limit=100) | symbol="AAPL", limit=100, offset=0 | 검증 통과 |

#### 6.1.2 날짜 파싱 테스트

**테스트 클래스**: `EarningsDateParserTest`

| 테스트 케이스 | 입력 | 예상 결과 |
|-------------|-----|----------|
| 표준 형식 (EDT) | "October 30, 2025 at 4 PM EDT" | 올바른 Instant |
| 표준 형식 (EST) | "January 15, 2025 at 10 AM EST" | 올바른 Instant |
| 오전 시간 | "July 22, 2025 at 8 AM PDT" | 올바른 Instant |
| 12시 처리 | "May 1, 2025 at 12 PM CST" | 올바른 Instant (정오) |
| 타임존 변환 확인 | "December 5, 2025 at 5 PM EST" | UTC 22:00 (EST = UTC-5) |
| 잘못된 형식 | "Invalid Date String" | DataParsingException |
| 빈 문자열 | "" | DataParsingException |

#### 6.1.3 숫자 파싱 테스트

**테스트 클래스**: `EarningsDataParserTest`

| 테스트 케이스 | 입력 | 예상 결과 |
|-------------|-----|----------|
| 정상 EPS | "2.97" | 2.97 |
| 음수 EPS | "-0.45" | -0.45 |
| "-" (데이터 없음) | "-" | null |
| 빈 문자열 | "" | null |
| 잘못된 형식 | "N/A" | DataParsingException |
| 소수점 10자리 | "1.2345678901" | 올바른 Double |

#### 6.1.4 HTML 파싱 테스트

**테스트 클래스**: `EarningsCalendarHtmlParserTest`

| 테스트 케이스 | 입력 | 예상 결과 |
|-------------|-----|----------|
| 정상 HTML (2개 행) | 샘플 HTML | 2개 EarningsEvent |
| 빈 테이블 | `<table></table>` | 0개 EarningsEvent |
| `<table>` 태그 없음 | `<div>No Data</div>` | DataParsingException |
| 불완전한 행 (날짜만) | 날짜만 있는 `<tr>` | 해당 행 스킵 |
| 혼합 데이터 (정상+불완전) | 5행 중 2행 파싱 실패 | 3개 EarningsEvent + 로그 경고 |

#### 6.1.5 Size 계산 로직 테스트

**테스트 클래스**: `EarningsCalendarSizeCalculatorTest`

| 테스트 케이스 | limit 입력 | 예상 size 파라미터 |
|-------------|----------|----------------|
| limit = 1 | 1 | 25 |
| limit = 12 (기본값) | 12 | 25 |
| limit = 25 | 25 | 25 |
| limit = 26 | 26 | 50 |
| limit = 50 | 50 | 50 |
| limit = 51 | 51 | 100 |
| limit = 100 | 100 | 100 |

### 6.2 통합 테스트 시나리오

#### 6.2.1 실제 API 호출 테스트

**테스트 클래스**: `EarningsCalendarIntegrationTest`

**테스트 케이스**:

1. **기본 동작 테스트**:
   ```kotlin
   @Test
   fun `AAPL의 기본 실적 일정을 조회할 수 있다`() = integrationTest {
       // Given
       val symbol = "AAPL"

       // When
       val calendar = ufc.yahoo.earningsCalendar(symbol)

       // Then
       assertThat(calendar.symbol).isEqualTo(symbol)
       assertThat(calendar.events).isNotEmpty()
       assertThat(calendar.actualCount).isGreaterThan(0)
   }
   ```

2. **Limit 파라미터 테스트**:
   ```kotlin
   @Test
   fun `limit 파라미터로 결과 수를 제한할 수 있다`() = integrationTest {
       // Given
       val symbol = "MSFT"
       val limit = 5

       // When
       val calendar = ufc.yahoo.earningsCalendar(symbol, limit = limit)

       // Then
       assertThat(calendar.events.size).isLessThanOrEqualTo(limit)
       assertThat(calendar.requestedLimit).isEqualTo(limit)
   }
   ```

3. **Offset 파라미터 테스트**:
   ```kotlin
   @Test
   fun `offset 파라미터로 과거 데이터를 조회할 수 있다`() = integrationTest {
       // Given
       val symbol = "TSLA"
       val offset = 10

       // When
       val calendar = ufc.yahoo.earningsCalendar(symbol, offset = offset)

       // Then
       assertThat(calendar.requestedOffset).isEqualTo(offset)
       // offset > 0이면 과거 데이터 위주
   }
   ```

4. **데이터 필드 검증**:
   ```kotlin
   @Test
   fun `미래 실적 이벤트는 epsEstimate를 가지고 있다`() = integrationTest {
       // Given
       val symbol = "GOOGL"

       // When
       val calendar = ufc.yahoo.earningsCalendar(symbol, limit = 20)
       val futureEvents = calendar.events.filter { it.reportedEps == null }

       // Then
       assertThat(futureEvents).isNotEmpty()
       futureEvents.forEach { event ->
           assertThat(event.epsEstimate).isNotNull() // 미래 실적은 추정치 존재
       }
   }
   ```

5. **과거 실적 검증**:
   ```kotlin
   @Test
   fun `과거 실적 이벤트는 reportedEps와 surprisePercent를 가지고 있다`() = integrationTest {
       // Given
       val symbol = "AAPL"

       // When
       val calendar = ufc.yahoo.earningsCalendar(symbol, offset = 1, limit = 10)
       val pastEvents = calendar.events.filter { it.reportedEps != null }

       // Then
       assertThat(pastEvents).isNotEmpty()
       pastEvents.forEach { event ->
           assertThat(event.reportedEps).isNotNull()
           // surprisePercent는 epsEstimate가 있을 때만 계산됨
       }
   }
   ```

6. **타임존 변환 검증**:
   ```kotlin
   @Test
   fun `earningsDate는 UTC Instant로 변환된다`() = integrationTest {
       // Given
       val symbol = "AAPL"

       // When
       val calendar = ufc.yahoo.earningsCalendar(symbol, limit = 1)
       val event = calendar.events.first()

       // Then
       assertThat(event.earningsDate).isNotNull()
       assertThat(event.timeZone).isNotNull()
       // Instant는 항상 UTC 기준
   }
   ```

#### 6.2.2 에러 케이스 테스트

1. **유효하지 않은 심볼**:
   ```kotlin
   @Test
   fun `존재하지 않는 심볼 조회 시 빈 리스트를 반환한다`() = integrationTest {
       // Given
       val invalidSymbol = "INVALID_SYMBOL_XYZ"

       // When
       val calendar = ufc.yahoo.earningsCalendar(invalidSymbol)

       // Then
       assertThat(calendar.events).isEmpty()
       assertThat(calendar.actualCount).isEqualTo(0)
   }
   ```

2. **Range 초과 Offset**:
   ```kotlin
   @Test
   fun `offset이 데이터 범위를 초과하면 빈 리스트를 반환한다`() = integrationTest {
       // Given
       val symbol = "AAPL"
       val largeOffset = 1000

       // When
       val calendar = ufc.yahoo.earningsCalendar(symbol, offset = largeOffset)

       // Then
       assertThat(calendar.events).isEmpty()
   }
   ```

3. **Rate Limiting 테스트** (선택적):
   ```kotlin
   @Test
   @Tag("slow")
   fun `짧은 시간 내 많은 요청 시 Rate Limit이 적용된다`() = integrationTest {
       // Given
       val symbols = listOf("AAPL", "MSFT", "GOOGL", "TSLA", "AMZN")

       // When & Then
       // Rate Limiter가 자동으로 요청 속도 조절
       symbols.forEach { symbol ->
           val calendar = ufc.yahoo.earningsCalendar(symbol)
           assertThat(calendar.events).isNotNull()
       }
       // 에러 없이 모든 요청 완료 확인
   }
   ```

#### 6.2.3 응답 레코딩 테스트

UFC의 기존 ResponseRecorder 패턴 활용:

```kotlin
@Test
fun `AAPL 실적 캘린더 응답을 기록한다`() = integrationTest {
    // Given
    val symbol = "AAPL"

    // When
    val calendar = ufc.yahoo.earningsCalendar(symbol, limit = 20)

    // Then
    assertThat(calendar.events).isNotEmpty()

    // Record
    if (RecordingConfig.isRecordingEnabled) {
        ResponseRecorder.record(
            calendar,
            RecordingConfig.Paths.Yahoo.EARNINGS_CALENDAR,
            "aapl_earnings_calendar"
        )
    }
}
```

#### 6.2.4 성능 테스트

```kotlin
@Test
@Tag("performance")
fun `100개 레코드 조회 시 5초 이내에 완료된다`() = integrationTest {
    // Given
    val symbol = "AAPL"
    val limit = 100

    // When
    val startTime = System.currentTimeMillis()
    val calendar = ufc.yahoo.earningsCalendar(symbol, limit = limit)
    val duration = System.currentTimeMillis() - startTime

    // Then
    assertThat(duration).isLessThan(5000) // 5초 이내
    assertThat(calendar.events.size).isLessThanOrEqualTo(limit)
}
```

---

## 7. 구현 우선순위

### Phase 1: 핵심 기능 (MVP)

1. **도메인 모델 정의**:
   - EarningsCalendar
   - EarningsEvent
   - EarningsCalendarParams

2. **HTML 파싱 로직**:
   - HTML에서 `<table>` 추출
   - 각 행을 EarningsTableRow로 파싱
   - 날짜 문자열 → Instant 변환
   - 숫자 문자열 → Double? 변환

3. **YahooClient 메서드 구현**:
   - `earningsCalendar(symbol, limit, offset)` 메서드
   - 파라미터 검증
   - HTTP 요청 생성
   - 응답 파싱
   - 에러 처리

4. **기본 통합 테스트**:
   - AAPL, MSFT 등 주요 종목 조회 성공
   - 파라미터 검증 테스트
   - 유효하지 않은 심볼 처리

### Phase 2: 안정성 강화

1. **포괄적인 에러 처리**:
   - 부분 파싱 실패 처리
   - 로깅 강화
   - 메타데이터 추가

2. **단위 테스트 확장**:
   - 날짜 파싱 엣지 케이스
   - 숫자 파싱 엣지 케이스
   - HTML 파싱 실패 시나리오

3. **성능 최적화**:
   - HTML 파싱 성능 측정
   - 불필요한 객체 생성 제거
   - 메모리 프로파일링

### Phase 3: 사용성 향상 (선택적)

1. **Ufc 직접 접근 메서드 추가**:
   - `ufc.earningsCalendar()` shortcut

2. **편의 기능**:
   - `getNextEarnings(symbol)`: 다음 실적 발표일만 조회
   - `getPastEarnings(symbol, count)`: 과거 N개 실적만 조회

3. **문서화**:
   - KDoc 주석 완성
   - 사용 예시 추가
   - README 업데이트

---

## 8. 참고 사항

### 8.1 yfinance 구현 참조 포인트

- **파일**: `/home/ulalax/project/kairos/yfinance/yfinance/base.py`
- **메서드**: `_get_earnings_dates_using_scrape(limit, offset)` (Line 743-835)
- **주요 참조 사항**:
  1. URL 생성 방식 (Line 784-786)
  2. BeautifulSoup 파싱 로직 (Line 796-808)
  3. 날짜 파싱 및 타임존 처리 (Line 818-827)
  4. "Surprise (%)" 컬럼명 호환성 처리 (Line 814)

### 8.2 제약 사항

1. **Yahoo Finance 의존성**:
   - 비공식 API이므로 언제든 변경 가능
   - HTML 구조 변경 시 파싱 실패 가능
   - 대응 방안: 버전별 파서 전략 또는 fallback 메커니즘

2. **데이터 품질**:
   - 일부 종목은 애널리스트 커버리지 부족으로 epsEstimate 없음
   - 과거 데이터가 제한적인 종목 존재
   - 상장폐지 종목은 조회 불가

3. **성능 제약**:
   - HTML 파싱은 JSON 파싱보다 느림
   - 대량 조회 시 Rate Limiting 고려 필요
   - 캐싱 전략 검토 권장

### 8.3 향후 확장 가능성

1. **다중 종목 조회**:
   ```kotlin
   suspend fun earningsCalendar(symbols: List<String>): Map<String, EarningsCalendar>
   ```

2. **날짜 범위 필터링**:
   ```kotlin
   suspend fun earningsCalendar(
       symbol: String,
       startDate: LocalDate? = null,
       endDate: LocalDate? = null
   ): EarningsCalendar
   ```

3. **캐싱 지원**:
   - 미래 실적: 짧은 TTL (5분)
   - 과거 실적: 긴 TTL (1시간)

4. **추가 필드 지원** (Yahoo가 제공 시):
   - Market Cap
   - Revenue Estimate/Actual
   - Conference Call Time

---

## 9. 체크리스트

### 9.1 구현 전 확인사항

- [ ] UFC 프로젝트 구조 이해 완료
- [ ] yfinance 구현 분석 완료
- [ ] Yahoo Finance Earnings Calendar 페이지 수동 테스트
- [ ] HTML 파싱 라이브러리 선정 (kotlinx.html 또는 JSoup)
- [ ] 기존 YahooClient 코드 리뷰

### 9.2 구현 중 확인사항

- [ ] 파라미터 검증 구현
- [ ] HTML 파싱 로직 구현
- [ ] 날짜/타임존 변환 구현
- [ ] 숫자 파싱 구현
- [ ] 도메인 모델 생성
- [ ] 에러 처리 구현
- [ ] 로깅 추가

### 9.3 구현 후 확인사항

- [ ] 단위 테스트 작성 및 통과
- [ ] 통합 테스트 작성 및 통과
- [ ] 최소 3개 이상 종목 실제 테스트 (AAPL, MSFT, GOOGL)
- [ ] 에러 케이스 테스트 (유효하지 않은 심볼, offset 초과 등)
- [ ] 성능 테스트 (100개 레코드 조회)
- [ ] KDoc 문서 작성
- [ ] README 업데이트
- [ ] 코드 리뷰 요청

---

## 10. 마무리

이 명세서는 UFC 프로젝트에 Yahoo Finance Earnings Calendar API를 통합하기 위한 상세한 가이드입니다.

**핵심 설계 원칙**:
1. **기존 아키텍처 존중**: YahooClient 확장 방식 사용
2. **일관된 API 스타일**: quoteSummary, chart와 동일한 패턴
3. **견고한 에러 처리**: 부분 파싱 실패에도 최대한 데이터 제공
4. **테스트 가능성**: 단위 테스트와 통합 테스트 모두 작성
5. **확장 가능성**: 향후 기능 추가를 고려한 설계

**주의사항**:
- 웹 스크래핑 방식이므로 Yahoo Finance HTML 구조 변경에 취약
- 프로덕션 환경에서는 모니터링 및 알림 설정 권장
- 정기적인 테스트로 API 변경 감지 필요

구현 과정에서 질문이나 이슈 발생 시 yfinance 구현(`/home/ulalax/project/kairos/yfinance/yfinance/base.py` Line 743-835)을 참조하시기 바랍니다.
