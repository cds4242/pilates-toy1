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

### STEP 11: 강사용 + 관리자용 화면
- (기록 필요 — 다음 세션에서 보강)

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
