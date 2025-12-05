# Earnings Calendar API 기술 명세서

> **Version**: 1.0
> **작성일**: 2025-12-05

---

## 1. 개요

### 1.1 API 목적

Yahoo Finance Earnings Calendar을 통해 특정 심볼의 **실적 발표 일정(Earnings Calendar)**을 조회한다.

| 항목 | 값 |
|-----|---|
| 엔드포인트 | `GET https://finance.yahoo.com/calendar/earnings` |
| HTTP 메서드 | GET |
| 인증 | 불필요 |
| 응답 형식 | HTML (웹 스크래핑) |

### 1.2 제공 데이터

| 데이터 | 설명 |
|-------|------|
| 실적 발표 일시 | 과거 및 미래 실적 발표 일정 (타임존 포함) |
| EPS 추정치 | 애널리스트 컨센서스 |
| EPS 실제값 | 발표된 실제 EPS (과거만) |
| 서프라이즈 비율 | (실제 - 추정) / 추정 × 100 |

### 1.3 QuoteSummary EARNINGS_DATES 모듈과의 차이

| 구분 | Earnings Calendar | QuoteSummary.EARNINGS_DATES |
|------|------------------|----------------------------|
| 데이터 소스 | HTML 스크래핑 | JSON API |
| 최대 레코드 수 | 100개 | 제한적 (4-12개) |
| 과거 데이터 | 수년 전까지 | 제한적 |
| Offset 지원 | 지원 | 미지원 |
| 안정성 | 낮음 (HTML 변경에 취약) | 높음 |
| 사용 목적 | 상세 역사 분석 | 간단한 다음 실적일 조회 |

---

## 2. 데이터 소스 분석

### 2.1 요청 파라미터

| 파라미터 | 타입 | 필수 | 기본값 | 설명 | 제약사항 |
|---------|------|-----|-------|------|---------|
| symbol | String | Yes | - | 티커 심볼 | 공백 불가 |
| offset | Int | No | 0 | 페이지네이션 오프셋 | 0 이상 |
| size | Int | No | 25 | 조회할 레코드 수 | 25, 50, 100 |

### 2.2 Size 계산 규칙

| limit 범위 | Yahoo size 파라미터 |
|-----------|-------------------|
| 1 ≤ limit ≤ 25 | 25 |
| 26 ≤ limit ≤ 50 | 50 |
| 51 ≤ limit ≤ 100 | 100 |
| limit > 100 | 에러 |

### 2.3 Offset 동작

| offset 값 | 조회 데이터 |
|-----------|-----------|
| 0 | 가장 최근 미래 실적부터 |
| 1 | 가장 최근 과거 실적부터 |
| n | n번째 과거 실적부터 |

### 2.4 HTML 응답 구조

HTML `<table>` 태그 내 데이터:

| 컬럼명 | HTML 필드명 | 타입 | Nullable |
|-------|-----------|------|----------|
| Symbol | Symbol | String | No |
| Company | Company | String | No |
| Earnings Date | Earnings Date | String | No |
| EPS Estimate | EPS Estimate | String | Yes |
| Reported EPS | Reported EPS | String | Yes |
| Surprise (%) | Surprise (%) | String | Yes |

### 2.5 Earnings Date 형식

| 형식 | 예시 |
|-----|------|
| 패턴 | `"Month DD, YYYY at H[H] AM/PM TZ"` |
| 예시 1 | `"October 30, 2025 at 4 PM EDT"` |
| 예시 2 | `"July 22, 2025 at 10 AM PST"` |

### 2.6 타임존 매핑

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

### 2.7 빈 값 처리

| HTML 값 | 의미 |
|--------|-----|
| `-` | 데이터 없음 (null) |
| 빈 문자열 | 데이터 없음 (null) |

---

## 3. 데이터/모델 설계

### 3.1 Domain 모델

#### EarningsCalendar

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| symbol | String | No | 티커 심볼 |
| events | List&lt;EarningsEvent&gt; | No | 실적 이벤트 목록 |
| requestedLimit | Int | No | 요청한 limit |
| requestedOffset | Int | No | 요청한 offset |
| actualCount | Int | No | 실제 반환된 이벤트 수 |

#### EarningsEvent

| 필드 | 타입 | Nullable | 설명 |
|-----|------|----------|------|
| earningsDate | Instant | No | 실적 발표 일시 (UTC) |
| timeZone | String | Yes | 타임존 (IANA 형식) |
| epsEstimate | Double | Yes | EPS 추정치 |
| reportedEps | Double | Yes | 실제 발표된 EPS |
| surprisePercent | Double | Yes | 서프라이즈 (%) |

### 3.2 Internal Response 모델

#### EarningsCalendarHtmlResponse

| 필드 | 타입 |
|-----|------|
| hasTable | Boolean |
| rows | List&lt;EarningsTableRow&gt; |

#### EarningsTableRow

| 필드 | 타입 |
|-----|------|
| symbol | String |
| company | String |
| earningsDateRaw | String |
| epsEstimateRaw | String |
| reportedEpsRaw | String |
| surprisePercentRaw | String |

### 3.3 API 메서드 시그니처

```kotlin
suspend fun earningsCalendar(
    symbol: String,
    limit: Int = 12,
    offset: Int = 0
): EarningsCalendar
```

| 파라미터 | 타입 | 기본값 | 제약 |
|---------|------|-------|------|
| symbol | String | - | 필수, 공백 불가 |
| limit | Int | 12 | 1-100 |
| offset | Int | 0 | 0 이상 |

| 반환 | 설명 |
|-----|------|
| EarningsCalendar | 실적 발표 일정 목록 |

### 3.4 필드 매핑

| Yahoo HTML | Domain 필드 | 변환 |
|-----------|------------|------|
| Earnings Date | earningsDate | String → Instant (UTC) |
| Earnings Date | timeZone | 타임존 추출 및 IANA 변환 |
| EPS Estimate | epsEstimate | "-" → null, String → Double |
| Reported EPS | reportedEps | "-" → null, String → Double |
| Surprise (%) | surprisePercent | "-" → null, String → Double |

### 3.5 데이터 변환 규칙

#### 날짜 변환

| 단계 | 설명 |
|-----|------|
| 1 | 타임존 문자열 추출 (예: "EDT") |
| 2 | IANA 타임존으로 변환 (예: "America/New_York") |
| 3 | LocalDateTime 파싱 (`MMMM d, yyyy 'at' h a`) |
| 4 | ZonedDateTime 생성 (타임존 적용) |
| 5 | Instant 변환 (UTC) |

#### 숫자 변환

| 입력 | 출력 |
|-----|------|
| "-" | null |
| 빈 문자열 | null |
| 유효한 숫자 | Double |
| 파싱 실패 | DataParsingException |

---

## 4. 예외 처리

### 4.1 ErrorCode 매핑

| 에러 케이스 | ErrorCode | 설명 |
|-----------|-----------|------|
| symbol 빈 문자열 | INVALID_PARAMETER | 파라미터 검증 실패 |
| limit 범위 초과 | INVALID_PARAMETER | 1-100 범위 위반 |
| offset 음수 | INVALID_PARAMETER | 0 이상 필요 |
| HTTP 4xx/5xx | EXTERNAL_API_ERROR | API 오류 |
| Rate Limit (429) | RATE_LIMITED | 요청 제한 |
| HTML 파싱 실패 | DATA_PARSING_ERROR | 역직렬화 실패 |
| 날짜 파싱 실패 | DATA_PARSING_ERROR | 날짜 형식 오류 |
| 50% 이상 행 파싱 실패 | DATA_PARSING_ERROR | 대량 파싱 오류 |

### 4.2 빈 결과 처리

| 상황 | 처리 |
|-----|------|
| 빈 테이블 | 빈 EarningsCalendar 반환 (예외 아님) |
| 존재하지 않는 심볼 | 빈 EarningsCalendar 반환 |
| offset 범위 초과 | 빈 EarningsCalendar 반환 |

### 4.3 부분 파싱 실패 처리

| 실패율 | 처리 |
|-------|------|
| < 50% | 성공한 데이터만 반환 + 로그 경고 |
| ≥ 50% | DataParsingException 발생 |

### 4.4 재시도 정책

| 에러 | 재시도 | 횟수 |
|-----|-------|-----|
| Rate Limit (429) | Yes | Rate Limiter 처리 |
| Network Error | Yes | 3회 |
| HTTP 5xx | Yes | 3회 |
| HTTP 4xx | No | - |
| 파싱 실패 | No | - |

---

## 5. 참고 자료

### 5.1 API 제약사항

| 제약 | 설명 |
|-----|------|
| 비공식 API | HTML 스크래핑, 변경 가능 |
| 최대 100개 | limit 상한 |
| HTML 파싱 오버헤드 | JSON API 대비 느림 |
| 구조 변경에 취약 | Yahoo Finance HTML 변경 시 파싱 실패 가능 |

### 5.2 용어

| 용어 | 설명 |
|-----|------|
| EPS | Earnings Per Share (주당순이익) |
| Surprise | (실제 - 추정) / 추정 × 100 |
| Offset | 페이지네이션 시작점 |

### 5.3 yfinance 참조

| 항목 | 값 |
|-----|---|
| 파일 | `/home/ulalax/project/kairos/yfinance/yfinance/base.py` |
| 메서드 | `_get_earnings_dates_using_scrape` (Line 743-835) |
| 참조 사항 | URL 생성, BeautifulSoup 파싱, 날짜/타임존 처리 |
