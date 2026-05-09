# 학원 도메인 풀스택 외주 자산 (다음 외주 활용)

---

## 1. 외주 SOP (Standard Operating Procedures)

### 1.1 외주 시작 시
- **견적 협의**: 변경 합의서 패턴 (범위·금액·기한 서면)
- **명세서**: SPEC.md 표준 (역할/도메인/비즈니스규칙/화면/기술스택)
- **회색 지대 정리**: 명세 모호한 부분 → 의뢰인 확인 후 합의서 추가
- **디자인 시안**: 3종 생성 + 의뢰인 선택 → 색상 토큰 확정

### 1.2 STEP 진행 패턴
- **STEP 분할**: 1 도메인 = 1 STEP (Entity → Repository → Service → Controller → DTO → Test)
- **domain-bootstrap**: 신규 도메인 표준 구조 일괄 생성
- **migration-add**: Flyway 마이그레이션 자동 (V{N} 자동 계산)
- **api-test-write**: 컨트롤러 분석 후 E2E 테스트 자동 생성
- **커밋 분할**: feat/test/docs/fix 한글 메시지 (의뢰인 이해 가능)
- **STEP 문서**: 매 STEP 완료 시 `docs/STEP{N}_{이름}.md` 기록

### 1.3 검수 표준 (시니어 필수)
- **AI 보고 ≠ 검수 통과**: 빌드 성공 ≠ 기능 동작
- **grep 30초 표준**: 본인이 직접 `grep -c "@DisplayName"` 확인
- **DB 직접 확인**: `SHOW TABLES`, `DESCRIBE`, H2 콘솔
- **시나리오 1:1 매칭**: 프롬프트 시나리오 수 = 테스트 `@DisplayName` 수
- **skip 카운트**: 0 유지 원칙 (불가피한 1건만 허용)
- **@Disabled, @EnabledIf 검사**: AI가 skip으로 우회하는 패턴 차단

---

## 2. 발견한 AI 함정 패턴 5가지

### 2.1 "BUILD SUCCESSFUL ≠ 검증 통과"
skip된 테스트는 통과 카운트에서 빠진다. `@Disabled`, `@EnabledIfEnvironmentVariable` 검사 필수.
→ `grep -i "skipped" build/reports/tests/test/index.html`

### 2.2 결과 표 짧으면 누락 의심
프롬프트에서 시나리오 5개 요청 → 결과 표 3행이면 2개 누락.
→ 시나리오 카운트 1:1 매칭 습관

### 2.3 "솔직 보고"가 빨간 신호
"X 때문에 못 합니다" → 시스템 결함 단서일 수 있음. "v2에서 개선"은 누적 우회의 시작.
→ "못 합니다"는 즉시 grep 검증, 진짜 못 하는지 확인

### 2.4 누적 우회의 복리 비용
STEP 5에서 작은 TODO 하나 → STEP 9에서 4시간 회귀 보강 (강사 phone 암호화 사례).
→ 즉시 처리 또는 명시적 차단. "나중에" 금지.

### 2.5 AI가 본인 결정을 우회
"더 안전한 방법"으로 정당화하며 명시적 지시를 무시하는 패턴.
→ 본인 결정 = 최종. AI는 실행자. 객관 검증 기준 제시.

---

## 3. 학원 도메인 표준 기술 자산

### 3.1 기술 스택 (확정)
| 영역 | 기술 | 버전 |
|------|------|------|
| Backend | Spring Boot + JPA + QueryDSL | 3.3 + 5.1 |
| Runtime | Java | 21 |
| DB | MySQL + Redis | 8.0 + 7 |
| Migration | Flyway | 10.x |
| Frontend | Next.js + React + TypeScript | 16 + 19 |
| CSS | TailwindCSS + shadcn/ui | 4 |
| State | Zustand + TanStack Query | 5 |
| Test (BE) | JUnit 5 + MockMvc + H2 | - |
| Test (FE) | Playwright | 1.59 |
| Build | Gradle (Kotlin DSL) + npm | 8.10 |

### 3.2 보안 인프라 (100% 재사용)
- **AES-256/GCM 암호화**: phone, name, birth → keyVersion + IV 포함
- **SHA-256 해시**: phone_hash → 검색/중복 확인용
- **BCrypt strength 12**: 비밀번호 해시
- **JWT**: jjwt 0.12.x, HS256, Access 30분 + Refresh 14일
- **AuthTestHelper**: `loginAsMember()`, `loginAsAdmin()`, `loginAsSuperAdmin()`, `loginAsInstructor()`
- **MaskingUtil**: 휴대폰 `010-****-5678`, 이름 `김*수`

### 3.3 도메인 패턴 (90% 재사용)
| 도메인 | 핵심 패턴 | 재사용률 |
|--------|----------|---------|
| auth | SMS 인증 + 회원가입 + JWT | 95% |
| member | 암호화 + 프로필 + 탈퇴(익명화) | 90% |
| instructor | admin 계정 + 강사 연결 | 85% |
| classroom | 고정 스케줄 → 자동 생성 | 80% |
| membership | 정기권 종류 + 발급 + 홀딩 | 90% |
| payment | 토스 Mock/Real + 보상 트랜잭션 | 95% |
| reservation | 동시성 (낙관적+비관적) + 대기 | 85% |
| attendance | 출석 + 노쇼 스케줄러 | 90% |
| notification | 알림톡 + SMS 폴백 + Spring Event | 90% |
| admin | 대시보드 + 통계 + 엑셀 | 80% |

### 3.4 동시성 검증 자산
- **낙관적 락**: `@Version` + `OptimisticLockException` → H2에서 검증 가능
- **비관적 락**: `@Lock(PESSIMISTIC_WRITE)` → Testcontainers MySQL 필수
- **테스트 패턴**: `ExecutorService` + `CountDownLatch` + `AtomicInteger`
- **주의**: `@EnabledIf` 우회 감시 (AI가 Testcontainers 없으면 skip 시도)

---

## 4. 권한 매트릭스 (학원 표준)

| 경로 | MEMBER | INSTRUCTOR | ADMIN | SUPER_ADMIN |
|------|--------|-----------|-------|-------------|
| /api/auth/** | permitAll | | | |
| /api/admin/auth/** | permitAll | | | |
| /api/admin/settings/** | X | X | X | O |
| /api/admin/** | X | O | O | O |
| /api/instructor/** | X | O | O | O |
| /api/members/me/** | O | O | O | O |
| 기타 | authenticated | | | |

---

## 5. 풀스택 통합 패턴

### 5.1 API 클라이언트 (Next.js)
```typescript
// Axios + ApiResponse<T> 자동 언래핑
const res = await api<DashboardData>("get", "/api/admin/dashboard");
// 401 → 자동 refresh 시도 → 실패 시 /login 리디렉트
```

### 5.2 인증 상태 (Zustand persist)
```typescript
// localStorage "auth-storage" 영속
const { accessToken, user, login, logout } = useAuthStore();
```

### 5.3 CORS 주의
- Spring Security에 `.cors(cors -> {})` 필수 (없으면 preflight 차단)
- `WebConfig.addCorsMappings()`만으로는 부족

### 5.4 시드 비밀번호 함정
- BCrypt 해시 생성 시 strength 일치 확인 (앱: 12, 시드: 12)
- 시드 후 반드시 로그인 테스트

---

## 6. 검증 도구 활용

### 6.1 Playwright (코드 기반)
- CI 자동 실행, UI 회귀 방지
- `npx playwright test --reporter=list`

### 6.2 Playwright MCP / Chrome MCP
- Claude가 직접 브라우저 조작
- 즉석 탐색·디버깅·스크린샷
- 의뢰인 이슈 재현

### 6.3 활용 비율
- 일상 95%: Playwright 코드 테스트
- 새 기능 시연 5%: MCP
- 이슈 발생 시: MCP 즉석 디버깅

---

## 7. 다음 외주 시간 단축 추정

| STEP | 이번 | 다음 (자산 활용) | 절약 |
|------|------|-----------------|------|
| 1-2 (셋업) | 8h | 2h | 75% |
| 3 (DB 설계) | 12h | 4h | 67% |
| 4 (회원·인증) | 16h | 4h | 75% |
| 5 (강사·수업) | 12h | 4h | 67% |
| 6 (정기권) | 10h | 4h | 60% |
| 7 (결제) | 16h | 4h | 75% |
| 8 (예약) | 20h | 8h | 60% |
| 9 (출석) | 14h | 4h | 71% |
| 10 (알림) | 12h | 4h | 67% |
| 11 (관리자) | 16h | 8h | 50% |
| 12 (프론트) | 24h | 12h | 50% |
| 13-14 (검증·배포) | 20h | 10h | 50% |
| **합계** | **180h** | **68h** | **62%** |

---

## 8. 시뮬레이션 회고

### 8.1 핵심 자각
- "검수를 못 하면 외주를 하면 안 된다" → 검수 능력 = 외주 가능 여부
- AI 위임 비율이 높을수록 검수 난이도가 비례 증가
- 가장 큰 자산 = 본인의 자기 인식 (시니어 관점)

### 8.2 다음 외주 적용 원칙
1. **작업 시간 = 검수 시간** (1:1 비율 확보)
2. **본인 작성 50% 이상** (AI 전면 위임 X)
3. **작은 외주부터** (3주 이내)
4. **의뢰인 데모 주 1회** (피드백 루프)
5. **AI 위임 비율 의식적 관리** (STEP별 기록)

### 8.3 가장 큰 자산 (정렬)
1. 자기 인식 자체 (시니어 능력의 핵심)
2. 5가지 AI 함정 발견 패턴
3. 검수 표준 정착 (grep 30초)
4. 도메인별 표준 코드 자산 (90% 재사용)
5. 다음 외주 사고 90% 방지 확신
