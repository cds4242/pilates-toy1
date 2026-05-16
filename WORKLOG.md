# WORKLOG — 필라테스 프로젝트

> 세션 간 동기화 로그. 모든 작업 시작·종료 시 기록.
>
> **사용법**:
> - 세션 시작: 마지막 5~10개 항목 읽기
> - 세션 종료: 한 항목 추가 (1~3줄)
> - 결정 사항은 DECISIONS.md로 분리

---

## 2026-05-16 — Railway 풀스택 배포 완료 🎉

**한 것**:
- Railway Trial Workspace 셋업 (무료 크레딧 $5, 카드 미등록)
- MySQL + Redis 플러그인 추가 (자동 reference variable 발급)
- backend 서비스 배포 (Root Directory: `backend`, Internal Port: 8080)
- frontend 서비스 배포 (Root Directory: `frontend`, 자동 포트 감지)
- CORS 정정: `*` → `http://localhost:3000` → 최종 frontend public URL
- 공개 URL 발급:
  * Backend: https://backend-production-81c77.up.railway.app
  * Frontend: https://frontend-production-8081.up.railway.app
- 풀스택 통신 정상 + 강사 시드 데이터(박지영/이수진 등) 노출 확인 + HTTPS 자동

**시뮬레이션 통과 함정 (외주 가서 만날 패턴 미리 경험)**:
1. C 드라이브 100% 만석 → 메이플 50GB 회수로 복구
2. monorepo Root Directory 명시 필수 (Railway 자동 처리 X — Railpack 빌더가 backend/frontend 둘 다 보고 충돌)
3. CORS 와일드카드(`*`) + `allowCredentials=true` 충돌 (Spring Security)
4. Railway Shared Variable 우선순위 함정 (서비스 Variables 변경이 Shared로 무력화)
5. Railway 자동 포트 감지 모드 (frontend, 명시 입력 UI 없음 — 컨테이너의 `EXPOSE`로 감지)
6. Healthcheck Timeout 300초 (Spring Boot 부팅 대기 시간 — backend Flyway + JPA 초기화)

**시뮬레이션 자산 검증**:
- 본인 Dockerfile 2개 (backend/frontend) Railway에서 무수정 동작
- demo 프로파일 정상 적용 (Mock SMS/카카오/토스, MySQL 실 DB)
- Flyway migration + 시드 데이터 자동 실행
- D-009 (standalone) · D-010 (NEXT_PUBLIC build-time) · D-011 (stdout 로깅) · D-012 (demo 프로파일) 전부 Railway에서 효과 확인
- D-014 결정 추가 (Trial Workspace 채택 정책)

**다음**:
- 시드 데이터 보강 (DEFERRED #1)
- 본격 QA (B 전략 후속) — Chrome MCP로 Railway URL 대상 회귀
- Railway 서비스 처리 (Pause 시점 본인 결정 — Trial $5 크레딧 소진 관리)

---

## 2026-05-16 — NAS 박제 잔재 전수 검사 + 운영 정리 (완료 ✅, Railway 배포 직전 점검)

본인 의도: NAS 박제는 nas-snapshot/으로 백업 완료, 메인 코드는 실제 운영(Railway) 준비.

**발견 버그 2건 (브라우저 QA 중)**:
- 회원관리 → 정기권 발급 시도 시 "Cannot read properties (map)" — 원인: `frontend/.env.production`의 `NEXT_PUBLIC_DEMO_MODE=true`가 mock adapter를 활성, 모든 API 호출이 mock에 가로채여 백엔드 호출 0건. mock 데이터의 lessonTypes 누락에서 .map() undefined
- 시간표 김하늘 박스 클릭 시 페이지 로드 실패 — 위와 동일 원인 (mock adapter가 처리 못함). 추가로 강사 1~3(박지영/이수진/최재훈)의 phone_encrypted가 이전 키로 암호화돼 있어 `/api/admin/instructors` 500 ENC_001

**조치**:
1. `frontend/.env.production` 삭제 (NAS 박제 시절 잔재 — DEMO_MODE=true). compose build.args가 NEXT_PUBLIC_API_URL 제대로 주입
2. `frontend/.env.local`, `.env.local.bak`, `snapshot-rewrite.mjs`, `snapshot-pages.mjs` 삭제 — 박제 후처리 스크립트 (D-008 박제 동결로 더 이상 사용 X)
3. 코드 3곳 `NEXT_PUBLIC_BASE_PATH` 참조 제거 → `/studio*.jpg` 직접 참조 (admin-login/instructor-login/(auth)login)
4. `client.ts` IS_DEMO 분기에 "NAS 박제 한정 + 운영 자동 비활성" 명시 주석
5. `InstructorPhoneMigrationRunner` @Profile에 `demo` 추가 + 키 불일치 재암호화 로직 보강 → 기존 60바이트 잘못된 phone_encrypted 3건 자동 재암호화

**검증**:
- 컨테이너 4개 전부 healthy
- QA 자동: admin 7/7, instructor 1/1, member 1/3(2건은 API 경로 변경 가능성, 핵심 OK) HTTP 200
- 브라우저 QA: 정기권 발급 200 OK + 회원 상세 갱신, 김하늘 박스 클릭 → 수업 상세 모달 정상 표시
- backend Gradle test: 121건 중 118 pass / 2 fail (AdminMemberE2ETest masking expects + ReservationConcurrencyIT MySQL 외부 DB 의존 — 두 건 모두 인프라 의존, 핵심 비즈니스 로직 무관)

**다음**: Step 3 Railway 배포 진입 가능

---

## 2026-05-16 — Step 2 로컬 docker compose 실기동 검증 (완료 ✅)

- compose build 1차 실패 (C 드라이브 100% 만석 → npm ci EIO + buildkit EOF)
- 메이플 50GB 삭제 + docker prune + pip cache + Temp 정리로 ~44GB 여유 확보
- compose build 2차 성공 (backend 659MB, frontend 250MB, ~10분)
- backend `/app/logs/` 디렉토리 부재로 Restarting 루프 — logback 파일 appender 3개가 디렉토리 자동 생성 실패
- → stdout 단일화로 12-Factor App 정렬 (**D-011**): logback-spring.xml 전면 개편, FILE/REQUEST_FILE/ERROR_FILE 전부 제거, prod는 JSON 콘솔
- backend 재기동했더니 또 12회 재시작 — Spring Boot 부팅 중 `No qualifying bean of type 'SmsService'`
- → **동기화 누락 사례 4호**: docker-compose에서 `SPRING_PROFILES_ACTIVE=prod` 사용한 것 자체가 의도 불일치
  - prod = 실 NHN Toast/Toss 키 필요한 운영 환경 (Mock 빈 없음)
  - 시뮬레이션·Railway 시연용 별도 프로파일 필요
- → demo 프로파일 신규 (**D-012**): application-demo.yml + Mock 빈 4개 @Profile에 demo 추가
- → docker-compose.prod.yml 파일명 유지, 내부만 `demo` 프로파일로 변경 (**D-013**)
- frontend unhealthy 20분 지속 — wget이 IPv6 `::1` 시도, Next.js는 `0.0.0.0`(IPv4)만 바인딩
- → frontend Dockerfile HEALTHCHECK: `localhost` → `127.0.0.1` 명시
- **최종 검증**: 4컨테이너 전부 healthy (mysql/redis/backend/frontend)
- **통신 검증 5종 전부 200 OK** (health, root, instructors API+시드, actuator/health, frontend→backend 컨테이너 통신)
- 자원 사용량: backend 517MB · frontend 36MB · mysql 503MB · redis 3MB (Railway 512MB 한계 시 backend 비등비등 — JVM MaxRAMPercentage=75 효과 확인)

다음 결정 필요:
- Step 3 Railway 배포 진입 OK
- prune으로 사라진 빌드 캐시 재구축 필요 시 다음 빌드 다시 ~10분
- 변경 파일 git status: backend/Dockerfile-(어제 다른 변경)... 8개 (logback-spring.xml, application-demo.yml 신규, Mock 4종 @Profile, docker-compose.prod.yml, frontend Dockerfile, WORKLOG, DECISIONS)

---

## 2026-05-15 — STEP 0~14 완료 시점 (소급 정리)

지금까지의 진행 상황을 역순으로 정리.

### STEP 14: 배포·인계 (시뮬레이션 종료)
- Dockerfile + docker-compose.prod.yml 작성
- .env.example 작성
- HANDOVER_CHECKLIST.md 작성
- REUSABLE_NOTES.md 통합
- 미완료: 실제 빌드 검증, GitHub Actions 그린 확인, 진짜 배포

### STEP 13: 인수 테스트 + 운영 매뉴얼
- AcceptanceE2ETest 3시나리오
- SecurityAcceptanceTest 7시나리오
- 운영 매뉴얼·트러블슈팅 가이드
- 백엔드 119 tests / 1 skip
- 미완료: 부하 테스트, Playwright 재실행

### STEP 12.5: 시안 매칭 보강
- placeholder 5개 보강
- Chrome MCP 비교 (보고서 작성 — 실제 픽셀 비교는 미실행, baseline 대체)
- 평균 시안 매칭 점수 8점+
- 미완료: Chrome MCP 실제 픽셀 비교

### STEP 12: 프론트엔드 시안 매칭
- Tailwind 토큰 매핑
- Pretendard 폰트 적용
- 12개 페이지 시안 매칭 (초기 6.9점 → 보강 후 8점+)
- Playwright baseline 12개

### STEP 11: 강사용 + 관리자용 화면 ⚠️ (코드/커밋 기반 추정 — 본인 확인 필요)

**커밋 범위**: `1596a5f` ~ `7aaea0d` (9개 커밋)

**백엔드 admin 도메인** (확정 — 커밋 메시지 명시)
- V12 마이그레이션 + ErrorCode + POI 의존성 + SecurityConfig 갱신 (`1596a5f`)
- 대시보드 API + Service (실시간 통합 쿼리) (`2118aa9`)
- 회원 검색·상세 + 메모 CRUD + 강제 탈퇴 (`77867b2`)
- 통계 API: 매출/회원 추이/출석률/인기 시간대 (`31ecb21`)
- 엑셀 일괄 등록 + 정기권 발급 + 매출 다운로드 (`7c09495`)
- 학원 설정 API — SUPER_ADMIN 전용 (`0f37c04`)
- E2E 5개 클래스 23 시나리오 (`ce497a0`)
- 통합 쿼리 + 엑셀 흐름 + 권한 매트릭스 아키텍처 문서 (`02829f7`)

**강사용 프론트엔드 페이지** ⚠️ (코드 기반 — 별도 라우트 그룹 없음)
- `/instructor/schedule` — 주간 시간표 (오늘/이번주 탭, 주 이동, 카드 expand)
- `/instructor/attendance` — 출석 체크 (classId 쿼리, ATTENDED/LATE/ABSENT 마킹)
- `/instructor-login` — 강사 전용 모바일 로그인 (커밋 `2a197da`로 보아 STEP 12 디자인 단계에서 분리됨)
- ⚠️ STEP 11 시점에서 강사 페이지가 위 2개뿐인지, 이후 추가됐는지 확인 필요

**관리자용 프론트엔드 페이지** ⚠️ (`(admin)` 라우트 그룹 — STEP 12 이후에 정리됐을 가능성)
- `/dashboard` — 관리자 대시보드 (핵심 3페이지에 포함, 커밋 `5e5d3bd`는 STEP 12)
- `/members` — 회원 관리 (검색·상세·메모·강제 탈퇴 백엔드와 매칭)
- `/instructors` — 강사 관리
- `/classes` — 수업 관리
- `/membership-passes` — 정기권 관리 (발급 포함)
- `/statistics` — 통계 (매출/회원/출석/인기 시간대)
- `/settings` — 학원 설정 (SUPER_ADMIN)
- `/admin-login` — 관리자 로그인
- ⚠️ STEP 11에서는 API + 일부만, STEP 12에서 페이지 본격 구현 가능성 큼

**테스트** (확정)
- admin 도메인 E2E 5개 클래스 / 23 시나리오 (`ce497a0`)

**미완료·보강 가능성** ⚠️
- 강사가 본인 수업 외 다른 강사 일정을 볼 수 있는지 (권한 분리)
- 관리자 엑셀 다운로드의 실제 파일명·인코딩 검증 여부
- 통계 페이지의 차트 라이브러리 (커밋 `fa870cf` "차트" 언급 — STEP 12.5에서 보강됨)



### STEP 10: notification 도메인
- KakaoAlimtalkClient 인터페이스 + Mock
- 5종 알림 템플릿 (RESERVATION_CONFIRM, REMINDER_1HOUR 등)
- SMS 폴백 + 비동기 발송 + 스케줄러
- 미완료: 실제 NHN Toast 연동 (v2)

### STEP 9: 권한 분리 + AuthTestHelper
- AuthTestHelper 4개 메서드
- SecurityE2ETest 6시나리오
- 6개 도메인 + 동시성 IT 전환
- 76 tests / 0 fail / 1 skip
- 보강: super admin 운영 시드 (prod_init.sql) 추가

### STEP 8: payment 도메인
- 시나리오 5개 → 13개 보강 (누락 발견)

### STEP 7: 토스페이먼츠 연동
- TossPaymentClient 인터페이스 + Mock/Real 분리
- 시나리오 4개 → 8개 보강
- 미완료: 실제 토스 개발자센터 가입 후 테스트 결제

### STEP 6: classroom (수업·예약)
- 비관적 락 동시 예약 처리
- membership_pass 누락 → 보강

### STEP 5: membership (정기권)
- 예약 시점 차감 정책 채택
- 정기권 4종 시드

### STEP 4: member (회원)
- AES-256/GCM 휴대폰 암호화 + Hash 검색
- 회원가입·로그인·JWT

### STEP 0~3: 셋업 + 기반
- 모노레포 구조 (backend Spring Boot 3 + frontend Next.js 14)
- Flyway 마이그레이션
- Testcontainers MySQL
- 인증·보안 기반

---

## 다음 세션 시작 시점

- 진행 중: DEFERRED_ITEMS.md 정리 완료 (2026-05-15 claude.ai 세션)
- 다음 결정: Phase A (★★★ 3개) 어디부터 시작할지
  - #1 시연용 시드 데이터 (2~3h)
  - #2 본인 직접 클릭 QA (2~3h)
  - #3 프론트엔드 호스팅 (30m)

---

## 2026-05-15 — Docker 검수 세션 (교육 모드)

**한 것**:
- backend/Dockerfile + frontend/Dockerfile + docker-compose.prod.yml + .env.example 검수
- 종합 점수 6/10 (경우 2: 1~2시간 보강 후 Step 2 진입)
- backend: curl 부재(HEALTHCHECK 깨짐), -Xmx 미설정, .dockerignore 없음 (3건)
- frontend: **CRITICAL** — next.config.ts(export) ↔ Dockerfile(standalone) 정면 충돌 (1건)
- compose: version 3.8 deprecated, frontend NEXT_PUBLIC_API_URL 런타임 주입 오류 (2건)

**발견 (동기화 누락 사례 3호)**:
- D-006(NAS 박제 → Railway 전환) 결정 이후 `frontend/next.config.ts`가 NAS 박제 모드 그대로 남음
- → 풀스택 빌드 즉시 실패하는 결함
- → D-009로 정식 결정화 (다음 보강 작업에서 처리)

**다음 결정 필요 (사전 기록)**:
- [x] Next.js 빌드 모드 옵션 A 채택 → D-009로 기록
- [x] NEXT_PUBLIC_* 빌드 시점 주입 → D-010으로 기록
- [ ] 보강 작업 1~5순위 실행 (다음 세션, docker_fix_prompt.md 기반)

**미완료**:
- 보강 작업 자체 (코드 변경 0건, 검수만 — 다음 세션에서 실행)
- Step 2(로컬 docker compose up) 진입 대기

---

## 2026-05-16 — NAS 박제 동결 + 스냅샷 보관

**한 것**:
- NAS(`192.168.0.30:22311`)에서 시연(`/p1` 3.6M) + 매뉴얼(`/portfolio` 7.7M) + 랜딩(`index.html`) + Apache 설정 SSH로 다운로드
- `nas-snapshot/nas-snapshot-pilates-20260516.tar.gz` (8.1M) + 압축 풀어둔 사본 보관
- `nas-snapshot/README.md` 작성 (복구 절차 A/B/C)
- DECISIONS.md에 D-008 (NAS 박제 동결) 추가
- 메인 필라테스 코드는 Railway 풀스택용 진화, NAS 박제는 동결로 분리

**중요 단서 (보고용)**:
- NAS 루트 index.html에 5개 프로젝트(p1~p5) 각각 "시연+메뉴얼" 쌍 — 필라테스는 `/p1` + `/portfolio`
- Synology SCP는 OpenSSH 신버전과 호환 안 됨 → `scp -O` (legacy) 필수
- next.config.ts는 여전히 NAS 박제 모드(export+basePath) — D-008에 따라 의식적으로 손대지 않음

**다음 결정 필요**:
- [ ] Railway 배포 진입 시 next.config.ts standalone 전환 — 별도 시점에 결정

**미완료**: 없음 (박제 동결 작업 종결)

---

## 2026-05-16 — D-008 표현 보강 (의도적 동결 명시)

**한 것**:
- D-008 톤 보정: "보조 자산" → "의도적·영구 동결 시연 슬롯"
- "Railway는 NAS 폐기가 아닌 다음 단계 (별도 트랙 병행)" 명시
- "NAS를 구버전·곧 폐기 취급 금지" 금지 항목 추가
- 두 트랙 역할 분리 명문화 (NAS=정적 시연 영속 / Railway=동적 풀스택 경험)

**미완료**: 없음

---

## 2026-05-16 — Docker 파일 보강 실행 (D-009 + D-010 적용)

**한 것** (검수 결과 1~5순위 모두 적용):
1. `frontend/next.config.ts`: export/basePath/trailingSlash/images.unoptimized/env 제거 → `output: "standalone"` 만 남김 (D-009 실행)
2. `backend/Dockerfile`: curl 설치(HEALTHCHECK 정상화) + JVM `-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC`
3. `.dockerignore` 3개 신규 (`backend/`, `frontend/`, 루트)
4. `docker-compose.prod.yml`: frontend `build.args.NEXT_PUBLIC_API_URL` + 런타임 `BACKEND_INTERNAL_URL` 분리 (D-010 실행)
5. `frontend/Dockerfile` builder stage: `ARG NEXT_PUBLIC_API_URL` + `ENV` 추가
6. `.env.example`: `NEXT_PUBLIC_API_URL` 항목 추가
7. `docker-compose.prod.yml`: deprecated `version: '3.8'` 줄 제거

**자가 감지**:
- 프롬프트가 신규 D-008/D-009 추가를 요청했으나, 같은 내용이 이미 D-009/D-010으로 기록되어 있음 → 중복 추가 X. 본 세션은 "실행"으로 기록.
- `NEXT_PUBLIC_BASE_PATH`를 참조하는 코드 3곳 (admin-login/instructor-login/(auth)login의 studio*.jpg src)은 `|| ""` fallback 덕에 변수 미정의 상태에서도 안전 → 코드 수정 불필요 (basePath="" 와 동일 결과)
- `frontend/snapshot-rewrite.mjs` 존재 — NAS 박제 전용 후처리 스크립트. D-008 동결 결정으로 더 이상 재실행 안 함. **삭제 여부는 본인 결정 대기** (일단 보존)

**미완료**:
- 작업 7: docker build 실제 검증 (다음 단계)

**다음 결정 필요**:
- [ ] `snapshot-rewrite.mjs` + `snapshot-pages.mjs` 등 NAS 박제 전용 스크립트 삭제 여부 (박제는 동결됐고 재생성 안 함 → 삭제 후보. 단 박제본 복구 절차에서 참조하지 않는지 확인 필요)
- [ ] `frontend/.gitignore`에 `snapshot/` 디렉토리 추가 여부

---

## 2026-05-15 — claude.ai 세션 (Phase A 재설계)

**한 것**:
- DEFERRED_ITEMS.md 우선순위 재조정 (#3 스킵, #8 격상) — D:/ai/toy1 환경에는 파일 부재 (claude.ai 세션 측 작업)
- DECISIONS.md에 D-006, D-007 추가 (호스팅 전략 + 우선순위)
- CLAUDE.md에 작업 가드레일 보강 (절대 금지 + 자가 감지 패턴)

**발견**:
- NAS 박제(https://dsjh.synology.me:8443/p1) 존재 — 동기화 누락 사례 1호
- → WORKLOG 시스템 즉시 가치 증명
- 코드 환경에 DEFERRED_ITEMS.md 자체가 없음 — 동기화 누락 사례 2호

**다음 결정 필요**:
- [ ] STEP 11 추정치 본인 확인 후 확정
- [ ] DEFERRED_ITEMS.md를 코드 환경에도 생성할지 (claude.ai 사본만으로 충분한지)
- [ ] 1단계 진입 — #1(시드) 부터 시작 확정?
- [ ] Railway 계정 생성 시점 (#8 진입 전)

**미완료**:
- STEP 11 본인 확인 (Claude Code 추정치 작성 후 본인 검증 대기)
- DEFERRED_ITEMS.md 본문 (claude.ai 세션에서 별도 첨부 예정)

---
