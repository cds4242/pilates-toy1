# Pilates Studio Frontend

Next.js 16 + TypeScript + TailwindCSS + shadcn/ui

## 개발 환경 셋업

```bash
npm install
cp .env.example .env.local
npm run dev
# http://localhost:3000
```

## 백엔드 의존성

프론트엔드가 정상 작동하려면 백엔드가 실행 중이어야 합니다:

```bash
cd ../backend
./gradlew bootRun
# http://localhost:8080
```

## Playwright E2E 테스트

```bash
# 브라우저 설치 (최초 1회)
npx playwright install chromium

# 테스트 실행
npx playwright test

# UI 모드
npx playwright test --ui
```

## 프로젝트 구조

```
src/
  app/              # App Router 페이지
    (auth)/          # 로그인, 회원가입
    (member)/        # 회원 전용 (홈, 시간표, 정기권, 프로필)
    (admin)/         # 관리자 (대시보드, 회원관리, 수업, 통계)
    instructor/      # 강사 (스케줄, 출석)
  components/
    ui/              # shadcn/ui 컴포넌트
    layout/          # 헤더, 하단 네비, 사이드바
  lib/
    api/             # Axios 클라이언트 + 도메인별 API
    store/           # Zustand 상태 관리
    types/           # TypeScript 타입
    hooks/           # 커스텀 훅
    utils/           # 유틸리티
```
