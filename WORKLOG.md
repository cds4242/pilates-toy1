# WORKLOG — 필라테스 프로젝트

> 세션 간 동기화 로그. 모든 작업 시작·종료 시 기록.
>
> **사용법**:
> - 세션 시작: 마지막 5~10개 항목 읽기
> - 세션 종료: 한 항목 추가 (1~3줄)
> - 결정 사항은 DECISIONS.md로 분리

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
