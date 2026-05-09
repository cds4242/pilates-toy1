# STEP 12: 프론트엔드 (Next.js) 풀스택 통합

## 작업 일시
2026-05-09

## 작업 요약
Next.js 16 + TypeScript + TailwindCSS + shadcn/ui 프로젝트 초기화.
핵심 5페이지 구현 + Playwright E2E + 백엔드 풀스택 연동 검증.

## 완료 상태

### 신규 생성
- **Next.js 16 프로젝트**: frontend/ 디렉토리, App Router, TypeScript strict
- **의존성**: axios, react-query, zustand, react-hook-form, zod, date-fns, lucide-react, shadcn/ui, @playwright/test
- **API 클라이언트**: Axios 인스턴스 + JWT 인터셉터 + 401 자동 토큰 갱신 + ApiResponse 언래핑
- **Zustand 인증 스토어**: accessToken/refreshToken/user localStorage persist
- **레이아웃**: Header, BottomNav (모바일), AdminSidebar (데스크톱)
- **핵심 5페이지**: 로그인, 회원가입, 회원 홈, 시간표, 관리자 대시보드
- **Placeholder 8페이지**: reservations, membership, profile, members, classes, statistics, instructor/schedule, instructor/attendance
- **Playwright E2E**: 7시나리오 x 2프로젝트 (mobile+desktop) = 14 테스트

### 백엔드 수정
- **SecurityConfig**: `.cors(cors -> {})` 추가 (Spring Security CORS preflight 허용)
- **시드 데이터**: admin 비밀번호 BCrypt 해시 strength 12로 재생성

## 기술 스택
- Next.js 16.2.6, React 19.2.4, TypeScript 5
- TailwindCSS 4, shadcn/ui (oklch 색상 토큰)
- Zustand 5, TanStack React Query 5
- Playwright 1.59 (Chromium)

## 검증 결과
- npm run build: **0 TypeScript 에러**, 15 routes
- Playwright: **14 passed**, 0 failed
- 풀스택 연동: admin 로그인 → 대시보드 4영역 API 호출 성공 (스크린샷 확인)

## 커밋 이력 (8개)
1. `chore(frontend): Next.js 16 프로젝트 초기 셋업`
2. `chore(frontend): shadcn/ui 컴포넌트 + 디자인 토큰`
3. `feat(frontend): API 클라이언트 + Zustand 인증 스토어 + 타입`
4. `feat(frontend): 라우팅 구조 + 레이아웃 + 인증 미들웨어`
5. `feat(frontend): 핵심 3페이지 (회원 홈 + 시간표 + 관리자 대시보드)`
6. `feat(frontend): placeholder 페이지 8개`
7. `test(frontend): Playwright E2E 7시나리오 (14 테스트)`
8. `fix(backend): CORS preflight 허용 + 시드 admin 비밀번호 해시 수정`
