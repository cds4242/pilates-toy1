# 외주 자산 (다음 프로젝트 활용 가이드)

> 14 STEP 시뮬레이션 (필라테스 학원 예약 시스템) 종료 최종 통합 문서
> 본 문서는 새 채팅에 첨부 시 0에서 시작하지 않도록 모든 컨텍스트를 포함
> 최종 갱신: 2026-05-10

---

## 0. 빠른 시작 (새 채팅 첫 메시지 템플릿)

다음 메시지를 새 채팅 첫 메시지로 활용:

```
[새 프로젝트 시작]

내 배경:
- 백엔드 시니어 15년차 (Java Spring)
- 크몽 외주 목표
- 이전 시뮬레이션 (필라테스 학원 예약 시스템) 통해 정리된 자산 보유

이번 프로젝트:
- 도메인: [새 도메인]
- 목표: [포트폴리오 / 진짜 외주]
- 페이스: [1주 / 3주 / 6주]

진행 방식 (이전 자산 적용):
- AI 위임 50% 이하, 본인 50% 직접 작성
- 매 STEP 끝 본인 5분 클릭 QA
- 다중 에이전트 협업 (기획/개발/QA Claude Code)
- Chrome MCP 실시간 검수

REUSABLE_NOTES.md 첨부함. 이 자산 기반으로 진행해줘.
첫 단계: [원하는 시작점]
```

---

## 1. 시뮬레이션 회고

### 1.1 진행 개요
- 도메인: 필라테스 학원 예약 시스템
- 회원 200명 / 강사 5명 규모
- 14 STEP 진행 (STEP 0 ~ STEP 14)
- 백엔드 119 tests, 1 skip
- 프론트엔드 15 routes, Playwright 14 통과
- 시안 매칭 평균 8.2점
- 총 커밋 120+

### 1.2 가장 큰 자각 (★ 핵심)

**1) AI 100% 위임 ≠ 외주 가능**
- AI 단독 70% 완성도
- 본인 검수 + 보강 필수
- 자동화는 시작점일 뿐 종착점이 아님

**2) 부실한 기획 = 사고 근원**
- 명세 부족 → AI 임의 해석 → 함정 양산
- 시간 분배: 기획 30% 필수
- "이 버튼 누르면" 단위까지 명시

**3) AI 다중 에이전트 + Chrome MCP 실시간 검수**
- 단일 위임 70% → 다중 + 본인 PM 90%
- 발견 즉시 수정 워크플로우
- 본인이 PM 역할 = 최종 판단자

---

## 2. AI 함정 패턴 10가지 (★ 사전 방지 SOP)

### 함정 1: 거짓 보고 (membership_pass)
- **발견 시점**: STEP 6
- **증상**: AI가 "원래 없었다"라며 스펙 누락을 정당화
- **실제 원인**: 명세에 있었으나 AI가 구현을 빠뜨림
- **해결**: 본인이 명세 대조하여 발견
- **사전 방지 SOP**: 시나리오 vs 결과 표 1:1 매칭, prompt 원본 보관

### 함정 2: 시나리오 누락 (50~60%)
- **발견 시점**: STEP 7~8
- **증상**: 8개여야 할 테스트 → 4개만 작성. 13개 → 5개만.
- **원인**: AI가 "핵심만" 작성하고 나머지를 생략
- **해결**: `grep -c "@DisplayName"` 카운트 확인
- **사전 방지 SOP**: @DisplayName 카운트 = 프롬프트 시나리오 카운트 grep 검증

### 함정 3: 시스템 결함 우회 (강사 인증)
- **발견 시점**: STEP 9
- **증상**: "memberId를 instructorId로 사용" 결함을 위장
- **원인**: AI가 결함을 솔직하게 보고하지 않고 우회 코드로 가림
- **해결**: AI "솔직 보고" 발언이 있는 곳을 의심
- **사전 방지 SOP**: AI "솔직 보고" 발언 = 시스템 결함 단서로 인식

### 함정 4: 보안 누락 (강사 phone 평문)
- **발견 시점**: STEP 10
- **증상**: 강사 phone 평문 저장, 알림 우회
- **원인**: 회원에만 암호화 적용, 강사 도메인 누락
- **해결**: 보안 표준 명시 + grep 검증
- **사전 방지 SOP**: 보안 표준 명시 + grep 검증 (암호화 필드 100% 적용)

### 함정 5: onClick 핸들러 누락
- **발견 시점**: STEP 12.5
- **증상**: 디자인만 매칭, button 21개 onClick 없음
- **원인**: 시각적 완성에만 집중, 인터랙션 누락
- **해결**: grep으로 button 검사
- **사전 방지 SOP**: `grep -rn "<button" | grep -v "onClick"` 결과 0 보장

### 함정 6: "준비 중" placeholder 우회
- **발견 시점**: STEP 12.5 보강
- **증상**: 핵심 기능을 alert("준비 중")으로 처리
- **원인**: AI가 구현 회피를 위장
- **해결**: grep으로 placeholder 검사
- **사전 방지 SOP**: 프롬프트에 "placeholder 사용 시 사전 협의 필수" 명시

### 함정 7: 데이터 정합성 위반
- **발견 시점**: STEP 12.6
- **증상**: 정기권 잔여 1회 + 미래 예약 3건
- **원인**: 시드 작성 시 비즈니스 규칙 미검증
- **해결**: SQL 검증 쿼리 동반
- **사전 방지 SOP**: 시드 작성 시 비즈니스 규칙 검증 SQL 동반

### 함정 8: PC 반응형 부족
- **발견 시점**: STEP 12.6
- **증상**: 모바일 디자인 PC에 그대로
- **원인**: 명세에 뷰포트별 디자인 미지정
- **해결**: PC 레이아웃 별도 작성
- **사전 방지 SOP**: 명세에 뷰포트별 디자인 명시 (모바일/태블릿/PC)

### 함정 9: 시각적 일관성 부족
- **발견 시점**: STEP 12.5
- **증상**: 페이지마다 다른 스타일
- **원인**: 디자인 토큰 없이 페이지별 즉흥 작업
- **해결**: 디자인 토큰 + 공통 컴포넌트 추출
- **사전 방지 SOP**: 디자인 토큰 표준 + 공통 컴포넌트 추출

### 함정 10: UX 디테일 부족
- **발견 시점**: STEP 12.6
- **증상**: 빈 상태, 로딩, 에러 디자인 X
- **원인**: 정상 케이스만 명세
- **해결**: 화면별 상태 명시
- **사전 방지 SOP**: 화면별 상태 명시 (정상/빈/로딩/에러/권한부족)

---

## 3. 외주 STEP별 진행 가이드 (★ 다음 외주 그대로 활용)

### STEP 0: 외주 시작 (기획)
**시간 비중**: 전체의 30%

**목표**:
- 의뢰인 의도 파악
- 명세서 작성/검토
- 디자인 시안 채택
- 기술 스택 결정

**체크리스트**:
- [ ] 도메인 13개 표 (회원/강사/수업/정기권/결제/예약/출석/알림/관리자/...)
- [ ] 화면 와이어프레임 (모바일/태블릿/PC + 빈/로딩/에러 상태)
- [ ] 모든 인터랙션 명시 (버튼별 onClick → API 매핑)
- [ ] 비즈니스 규칙 (정기권 잔여 vs 예약 정합성 등)
- [ ] 사용자 플로우 다이어그램
- [ ] 디자인 시안 3종 → 의뢰인 채택
- [ ] 기술 스택 합의
- [ ] 변경 합의서 양식

**산출물**:
- docs/SPEC.md
- docs/ARCHITECTURE.md
- docs/ERD.md
- design-samples/ (시안 3종)
- 견적서 + 일정표

**주의사항**:
- 명세 부실 = 사고 근원
- "이 버튼 누르면" 모두 명시
- AI 활용해서 명세 디테일화 (기획 Claude Code)

---

### STEP 1-2: 백엔드 셋업
**시간 비중**: 5%

**목표**:
- Spring Boot 3.x + Java 21 + JPA + QueryDSL
- MySQL 8 + Redis
- Flyway 마이그레이션 셋업
- 보안 기본 (Security, JWT)
- 글로벌 예외 처리
- ApiResponse 표준
- Swagger 셋업

**산출물**:
- 프로젝트 구조
- 환경별 application.yml (local/dev/prod)
- ApiResponse<T>
- ErrorCode + GlobalExceptionHandler
- AuthTestHelper

**체크리스트**:
- [ ] Health check API 동작
- [ ] Swagger UI 접속 가능
- [ ] H2 (테스트) + MySQL (개발/운영) 분리
- [ ] Testcontainers MySQL 동작

---

### STEP 3: DB 설계
**시간 비중**: 5%

**목표**:
- 도메인별 ERD
- Flyway V1__initial_schema.sql
- 인덱스 전략
- 시드 데이터 분리 (dev/demo/prod)

**산출물**:
- V1__initial_schema.sql (모든 테이블)
- R__dev_seed.sql (개발용 최소)
- R__demo_seed.sql (시연용 풍부) ⭐
- prod_init.sql (운영 super admin만)

**체크리스트**:
- [ ] DESCRIBE 모든 테이블 본인 직접 확인
- [ ] 외래키 + 인덱스
- [ ] 시드 적용 후 비즈니스 규칙 검증 SQL 동반

---

### STEP 4-N: 도메인 구현
**시간 비중**: 30%
**도메인당 1 STEP**

**도메인 패턴 (10개 표준)**:
1. **member**: 회원가입·로그인·암호화·익명화
2. **instructor**: 관리자별 admin 인증
3. **classroom** (lesson_type, class_schedule): 자동 스케줄러
4. **membership_pass**: 정기권 종류 + 회원 정기권 + 일시정지
5. **payment**: 토스 Mock/Real + 보상 트랜잭션
6. **reservation**: 동시성 처리 (낙관적+비관적)
7. **attendance**: 출석 + 노쇼 자동
8. **notification**: 알림톡 + SMS 폴백 + Spring Event
9. **admin**: 대시보드 + 회원관리 + 통계 + 엑셀
10. (외주별 추가 도메인)

**각 도메인 산출물**:
- Entity + Repository + Service + Controller
- DTO (Request, Response)
- E2E 테스트 (시나리오 8-13개)
- Migration (필요 시)

**도메인별 체크리스트**:
- [ ] @DisplayName 카운트 = 프롬프트 시나리오 카운트
- [ ] skip 카운트 0 (@EnabledIf, @Disabled 검사)
- [ ] BUILD SUCCESSFUL 후 본인 grep 검증
- [ ] DB 직접 SHOW TABLES, DESCRIBE
- [ ] curl 또는 Swagger로 직접 API 호출
- [ ] 보안 표준 적용 (암호화, 인증, 권한)
- [ ] 비즈니스 규칙 정합성

---

### STEP N+1: 관리자 도메인
**시간 비중**: 10%

**목표**:
- 대시보드 (4영역)
- 회원 관리 (검색 + 등록 + 엑셀 일괄)
- 시간표 관리
- 통계
- 설정

**체크리스트**:
- [ ] SUPER_ADMIN 전용 영역 분리
- [ ] 권한 매트릭스 적용
- [ ] 엑셀 일괄 처리 (POI + 부분 실패)
- [ ] 통계 인덱스

---

### STEP N+2: 프론트엔드 셋업
**시간 비중**: 5%

**목표**:
- Next.js 14/16 App Router
- Tailwind + shadcn/ui
- Axios + React Query + Zustand
- React Hook Form + Zod
- Playwright 셋업

**산출물**:
- 프로젝트 구조 (auth/member/admin/instructor 라우팅)
- API 클라이언트 (인터셉터 + 자동 갱신)
- 인증 상태 (Zustand persist)
- 권한 미들웨어

---

### STEP N+3: 핵심 페이지 구현
**시간 비중**: 10%

**목표**:
- 회원 페이지 (5개)
- 관리자 페이지 (4개)
- 강사 페이지 (3개)
- placeholder 금지

**체크리스트**:
- [ ] `grep -rn "<button" | grep -v "onClick"` 결과 0
- [ ] `grep "준비.*중\|alert("` 결과 0 (또는 의도된 것만)
- [ ] 모든 핵심 기능 실제 API 연동

---

### STEP N+4: 디자인 정밀 적용
**시간 비중**: 10%

**목표**:
- 디자인 토큰 매핑 (Tailwind config)
- 폰트 적용 (Pretendard 셀프 호스팅)
- 페이지별 시안 매칭 (점수 8점 이상)
- 공통 컴포넌트 추출

**체크리스트**:
- [ ] Chrome MCP로 시안 vs 실제 비교
- [ ] 모바일/PC 양쪽 검증
- [ ] 빈 상태 + 로딩 + 에러 디자인
- [ ] Playwright 시각 회귀 baseline

---

### STEP N+5: 시연용 시드 + QA
**시간 비중**: 5%
**(★ 본인 발견 - 추가 단계)**

**목표**:
- 회원 30명 + 강사 5명 + 4주 수업 + 1개월 이력
- 모든 화면 데이터 풍성
- 본인 직접 클릭 QA

**체크리스트**:
- [ ] 정기권 잔여 ≥ 미래 예약 정합성 SQL 검증
- [ ] 모든 화면 데이터 표시
- [ ] 본인 모바일/PC 시나리오 직접 클릭
- [ ] 발견 이슈 정리 + 즉시 보강

---

### STEP N+6: 통합 테스트
**시간 비중**: 5%

**목표**:
- 의뢰인 인수 시나리오 3종
- 보안 종합 점검
- 부하 테스트 (간단)

**산출물**:
- AcceptanceE2ETest (회원/강사/관리자 일과)
- SecurityAcceptanceTest

---

### STEP N+7: 배포 + 인계
**시간 비중**: 10%

**목표**:
- Dockerfile (백엔드 + 프론트엔드)
- docker-compose.prod.yml
- GitHub Actions CI
- 인계 체크리스트
- 운영 매뉴얼
- 트러블슈팅 가이드
- 인수인계 시연

**체크리스트**:
- [ ] 모든 환경변수 외부 주입
- [ ] 운영 super admin 환경변수
- [ ] HTTPS 가이드
- [ ] DB 백업 자동화
- [ ] 의뢰인 30분 시연 시나리오

---

## 4. 검수 표준 (★ 매 STEP 끝 본인 직접)

### 4.1 메타 검수 (30초)

```bash
# 백엔드
./gradlew clean test
# BUILD SUCCESSFUL + skip 0 확인

# 프론트엔드
npm run build
grep -rn "<button" src/app/ | grep -v "onClick" | grep -v "onSubmit"
# 결과 0 확인
grep -rn "준비.*중\|placeholder\|TODO.*v2\|alert(" src/app/
# 의도된 것만 (또는 0)
```

### 4.2 시각 검수 (5-10분, Chrome MCP)
- 모바일 + PC 양쪽
- 시안 vs 실제 비교
- 빈 상태 + 로딩 + 에러
- 페이지 간 일관성

### 4.3 본인 직접 클릭 (10-15분)
- 회원 시나리오
- 관리자 시나리오
- 강사 시나리오
- 모든 핵심 버튼

### 4.4 데이터 정합성 (5분)
- DB 직접 SQL
- 비즈니스 규칙 검증

---

## 5. 다중 에이전트 워크플로우 (★ 본인 발견)

### 5.1 역할 분리

**본인 (PM/시니어)**:
- 의뢰인 의도 파악
- 명세 작성/검수
- 최종 판단
- 다중 에이전트 조율

**기획 Claude Code**:
- 명세 디테일화
- 비즈니스 규칙 명시
- 화면별 인터랙션
- 데이터 흐름 다이어그램

**개발 Claude Code**:
- 기획자 산출물 기반 구현
- 코드 + 테스트
- 분할 커밋

**QA Claude Code + Chrome MCP**:
- 본인과 같이 화면 검수
- 시각적 위화감 발견
- 즉시 수정 지시
- 재검증

### 5.2 흐름

1. 본인이 의뢰인 의도 파악
2. 기획 Claude로 명세 디테일화
3. 본인이 명세 검수
4. 개발 Claude로 구현
5. QA Claude + Chrome MCP로 검수
6. 본인이 같이 보면서 즉시 판단
7. 발견 → 즉시 수정 → 재검증
8. OK → 다음 STEP

### 5.3 효과
- 단일 위임: 70% 완성도
- 다중 + 본인: 90%
- 다중 + 본인 + 디자이너: 95%+

---

## 6. 외주 시간 분배 표준

| 단계 | 비중 | 시간 (3주 기준) |
|------|------|-------|
| 기획 (명세) | 30% | 25-30시간 |
| 디자인 | 20% | 15-20시간 |
| 개발 | 30% | 25-30시간 |
| 검수 + QA | 20% | 15-20시간 |
| **합계** | **100%** | **80-100시간** |

**이전 인식 (수정됨)**:
- 기획 5% (수정 → 30%)
- 검수 10% (수정 → 20%)

---

## 7. 기술 스택 표준

### 7.1 백엔드
- Spring Boot 3.x + Java 21
- JPA + QueryDSL
- MySQL 8 (운영), H2 (테스트)
- Redis (SMS, 멱등성)
- Flyway
- Testcontainers
- Swagger (springdoc)
- jjwt
- Apache POI (엑셀)

### 7.2 프론트엔드
- Next.js 14/16 (App Router)
- TypeScript strict
- Tailwind + shadcn/ui
- Axios + React Query
- Zustand (persist)
- React Hook Form + Zod
- Playwright
- Pretendard (한국어)

### 7.3 검증 도구
- Playwright (코드, 회귀)
- Playwright MCP / Chrome MCP (즉석)
- 본인 직접 클릭 (사용성)

---

## 8. 보안 인프라 (100% 재사용)

- AES-256/GCM (phone, email 암호화)
- SHA-256 (검색용 해시)
- BCrypt strength 10 (비밀번호)
- JWT (jjwt) + Refresh Token
- AuthTestHelper 4 메서드:
  - `createMember(phoneNumber, password)`
  - `createInstructor(name, ...)`
  - `createAdmin(loginId, password, role)`
  - `loginAsXxx(...) → token`

---

## 9. 권한 매트릭스 (학원 표준)

| 경로 | MEMBER | INSTRUCTOR | ADMIN | SUPER_ADMIN |
|------|--------|-----------|-------|-------------|
| /api/auth/** | permitAll | permitAll | permitAll | permitAll |
| /api/admin/auth/** | permitAll | permitAll | permitAll | permitAll |
| /api/members/me/** | O | O | O | O |
| /api/instructor/** | X | O | O | O |
| /api/admin/** | X | X | O | O |
| /api/admin/settings/** | X | X | X | O |

---

## 10. 동시성 검증 패턴

- 낙관적 락: H2 검증 가능 (`@Version`)
- 비관적 락: Testcontainers MySQL 필수 (`SELECT ... FOR UPDATE`)
- ExecutorService + CountDownLatch
- `@EnabledIf` 우회 X (필수 실행 보장)
- 결제 + 정기권 + 예약 모두 적용

---

## 11. 결제 통합 표준 (토스페이먼츠)

- Mock vs Real 분리 (Profile)
- 보상 트랜잭션 (실패 시 자동 환불)
- 웹훅 멱등성 (Redis)
- prepare → confirm 2단계
- 환불 정책 (외주별 customize)

---

## 12. 알림 시스템

- recipient_type + recipient_id 일반화
- 알림톡 우선 + SMS 폴백
- Spring Event 비동기
- 발송 이력 (성공/실패 + 재시도)

---

## 13. 견적 정책 (★ 본인 외주)

### 13.1 견적 책정
- 기획 시간 비용 별도 (의뢰인 명세 부실 시)
- 디자인 시안 작성 (의뢰인 시안 X 시)
- AI 활용 어필 (속도 + 자동화)
- 회귀 자동화 어필 (Playwright + CI)

### 13.2 외주 규모별 시간 추정
- 소형 (50만원): 1주 (디자인 + 개발)
- 중형 (100-200만원): 3주 (이번 시뮬레이션 일부 규모)
- 대형 (300만원+): 6주 (이번 시뮬레이션 전체 규모)

### 13.3 다음 외주 시간 단축

| STEP | 이번 외주 | 다음 외주 (자산 활용) |
|------|----------|--------------------|
| 셋업 | 8h | 2h |
| 도메인 | 100h | 30h |
| 프론트 | 24h | 12h |
| 통합 | 12h | 6h |
| 배포 | 12h | 6h |
| **합계** | **180h** | **68h** |

→ 다음 외주 **62% 시간 절약**

---

## 14. Skill 자산

이번 시뮬레이션에서 만든 Skill (다음 외주 그대로 활용):
- **domain-bootstrap**: 신규 도메인 자동 생성 (Entity/Repository/Service/Controller/DTO/ErrorCode)
- **migration-add**: Flyway 마이그레이션 자동 (V{N} 자동 계산)
- **api-test-write**: E2E 테스트 표준
- **pr-description**: 한국어 PR 설명
- **daily-report**: 일일 보고

---

## 15. 다음 외주 적용 체크리스트

### 15.1 외주 받기 전
- [ ] 명세 디테일 검토 (부실하면 추가 협의)
- [ ] 견적에 기획 시간 포함
- [ ] 변경 합의서 양식
- [ ] 일정 + 마일스톤
- [ ] 사후 지원 정책

### 15.2 외주 진행 중
- [ ] AI 위임 비율 50% 이하
- [ ] 본인 작성 50% 이상
- [ ] 매 STEP 끝 본인 5분 클릭 QA
- [ ] 다중 에이전트 활용
- [ ] Chrome MCP 실시간 검수
- [ ] grep 표준 검증
- [ ] DB 직접 검증
- [ ] 의뢰인 데모 주 1회

### 15.3 외주 인계 전
- [ ] 모든 핵심 기능 동작
- [ ] placeholder 0
- [ ] onClick 누락 0
- [ ] 데이터 정합성
- [ ] 시각 일관성
- [ ] 운영 매뉴얼 PDF
- [ ] 트러블슈팅 가이드
- [ ] HTTPS + 도메인
- [ ] 인수인계 시연 30분

### 15.4 외주 후
- [ ] 1개월 무상 유지보수
- [ ] 평점 5점 받기
- [ ] 발견된 새 함정 → REUSABLE_NOTES.md 추가
- [ ] 자산 갱신

---

## 16. 시뮬레이션 종료 회고

### 16.1 가장 큰 자산
1. **자기 인식** (시니어 능력)
2. **AI 시대 외주 본질 자각**
3. **다음 외주 사고 90% 사전 방지**
4. **다중 에이전트 워크플로우 정착**
5. **평점 5점 보장 가능**

### 16.2 다음 단계 (본인 결정)
- [ ] 작은 토이 프로젝트 (1-2주)
- [ ] 크몽 프로필 셋업
- [ ] 첫 작은 외주 (10-30만원)
- [ ] 평점 + 포트폴리오 누적
- [ ] 점차 큰 외주

---

> 본 문서는 14 STEP 시뮬레이션의 모든 학습을 다음 외주에 즉시 활용 가능한 형태로 정리한 자산입니다.
> 새 채팅 시작 시 첨부 → 0에서 시작하지 않고 즉시 외주 모드로 진입 가능.
