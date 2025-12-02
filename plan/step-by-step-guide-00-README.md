# UFC 구현 가이드 - 실행 순서

## 문서 정보
- **버전**: 1.0.0
- **최종 작성일**: 2025-12-02
- **목적**: Haiku 모델을 위한 단계별 구현 가이드 인덱스

---

## 📚 문서 구조

UFC 프로젝트는 haiku 모델이 실행할 수 있도록 다음과 같이 세분화된 문서로 구성되어 있습니다:

### 1. 계획 문서 (참조용)
- `00-project-overview.md` - 프로젝트 전체 개요
- `01-architecture-design.md` - 아키텍처 설계
- `02-error-handling.md` - 에러 처리 전략
- `03-yahoo-finance-core.md` - Yahoo Finance 핵심
- `04-yahoo-finance-etf.md` - ETF 기능
- `05-yahoo-finance-price.md` - 가격 데이터
- `06-fred-macro-indicators.md` - FRED API
- `07-advanced-topics.md` - 고급 주제
- `08-data-models-reference.md` - 데이터 모델 참조
- `09-testing-strategy.md` - 테스트 전략
- `10-yahoo-finance-implementation-guide.md` - 구현 상세

### 2. 실행 가이드 (Haiku용)
- **`11-haiku-implementation-steps.md`** ← START HERE
  - Phase 0: 프로젝트 초기 셋업 (4 steps)
  - Phase 1: 공통 모델 및 예외 시스템 (3 steps)
  - Phase 2: Infrastructure 레이어 (3 steps)
  - Phase 3: Yahoo Finance 인증 (2 steps)

- **`12-haiku-implementation-steps-phase4-8.md`**
  - Phase 4: Yahoo Finance 인증 완성 (3 steps)
  - Phase 5: Yahoo Finance HTTP Client (2 steps)
  - Phase 6: 테스트 인프라 구성 (4 steps)
  - Phase 7: 첫 번째 Live Test (1 step)
  - Phase 8: 첫 번째 체크포인트 (1 step)

- **`13-haiku-implementation-steps-phase9-15.md`** (다음 작성 예정)
  - Phase 9: Yahoo Finance Chart API
  - Phase 10: Yahoo Finance QuoteSummary API
  - Phase 11: ETF 기능 구현
  - Phase 12: FRED API 구현
  - Phase 13: UFCClient Facade 완성
  - Phase 14: 전체 테스트 작성
  - Phase 15: 최종 검증

---

## 🚀 시작하기

### 전제 조건
- JDK 21 설치
- Gradle 8.x 설치
- Git 설치
- 인터넷 연결 (Live Test용)

### 실행 순서

**Step 1: 프로젝트 디렉토리 준비**
```bash
cd /home/ulalax/project/kairos/ufc
```

**Step 2: 문서 순서대로 실행**
1. **11-haiku-implementation-steps.md** 열기
2. Phase 0부터 순서대로 진행
3. 각 Step 완료 후 체크포인트 확인
4. 12-haiku-implementation-steps-phase4-8.md로 이동
5. 계속 진행...

---

## ✅ 체크포인트

### Phase 0-3 완료 후
```bash
./gradlew build
# ✅ Build 성공 확인
```

### Phase 4-8 완료 후
```bash
./gradlew liveTest --tests "YahooAuthLiveTest"
# ✅ Yahoo Finance 인증 테스트 통과 확인
```

### Phase 9-15 완료 후
```bash
./gradlew test
./gradlew liveTest
# ✅ 모든 테스트 통과 확인
```

---

## 📊 진행률 추적

| Phase | 내용 | 문서 | 진행률 |
|-------|------|------|--------|
| 0-3 | 프로젝트 셋업 + 기본 인프라 | 11 | 0% |
| 4-8 | Yahoo 인증 + 테스트 인프라 | 12 | 0% |
| 9-15 | API 구현 + 테스트 완성 | 13 | 0% |

---

## 🛠️ 트러블슈팅

### 빌드 실패
```bash
./gradlew clean build --refresh-dependencies
```

### Live Test 실패
- 인터넷 연결 확인
- Yahoo Finance 서비스 상태 확인
- 잠시 대기 후 재시도 (Rate Limiting)

### 의존성 문제
```bash
./gradlew dependencies
```

---

## 📝 중요 참고사항

### 각 Step의 완료 조건
모든 Step은 다음을 포함합니다:
- ✅ **완료 조건**: 무엇이 완료되어야 하는지
- 🧪 **테스트**: 어떻게 검증하는지
- 📝 **산출물**: 어떤 파일이 생성되는지

### 순서 준수
- 반드시 문서 순서대로 진행
- 각 Step은 이전 Step에 의존
- 건너뛰지 말 것

### 테스트 우선
- 각 Phase 완료 후 반드시 빌드 실행
- Live Test가 있는 경우 반드시 실행
- 에러 발생 시 즉시 해결

---

## 📧 문의

문제가 발생하면 다음 정보를 포함하여 문의:
1. 현재 진행 중인 Phase와 Step
2. 에러 메시지 전문
3. `./gradlew build` 결과

---

**시작 문서**: 11-haiku-implementation-steps.md
**프로젝트 루트**: /home/ulalax/project/kairos/ufc
**마지막 업데이트**: 2025-12-02
