# STEP 12.5: 시안 A 디자인 정밀 적용

## 작업 일시
2026-05-10

## 작업 요약
시안 A (Minimal Modern) 디자인 토큰 + 12페이지 정밀 매칭 + Playwright 시각 회귀 baseline.

## 완료 상태

### 디자인 토큰 매핑
- Primary: #F0A0B5, Dark: #D88A9E, Light: #FAD4DE
- Text: title #2D2D2D, body #6B6B6B, sub #9B9B9B
- Border: #EEEEEE, Input: #DDDDDD
- Radius: sm 8px, md 18px
- Pretendard CDN 폰트

### 공통 컴포넌트 6종
- PassCard, ClassCard, StatusBadge, SectionTitle, KpiCard, MobileTabBar

### 12페이지 시안 매칭

| 페이지 | 매칭 점수 | 비고 |
|--------|----------|------|
| member/login | 9/10 | 스튜디오 그라디언트, 폼, 링크 |
| member/signup | 8/10 | 3단계 + 프로필 사진 + 성별 라디오 |
| member/home | 9/10 | PassCard + 예약 리스트 + 알림벨 |
| member/reservation (schedule) | 8/10 | 날짜 선택 + 수업 카드 |
| member/mypage (profile) | 8/10 | 아바타 + PassCard + 메뉴 |
| admin/login | — | 별도 로그인 페이지 미구현 (관리자 탭으로 통합) |
| admin/dashboard | 8/10 | KPI + 수업 + 알림 + 매출 차트 |
| admin/members | 5/10 | placeholder (테이블 미구현) |
| admin/schedule | 5/10 | placeholder (캘린더 미구현) |
| instructor/login | — | 관리자 탭으로 통합 |
| instructor/schedule | 7/10 | 탭 + 빈 상태 |
| instructor/attendance | 6/10 | 헤더 + 하단 버튼 (멤버 리스트 미구현) |

### Playwright 시각 회귀
- 12페이지 x 2 viewport (mobile 390x844 + desktop 1280x800) = **24 baseline 생성**

## 커밋 이력 (6개)
1. `chore(design): Tailwind 시안 A 토큰 + Pretendard 폰트`
2. `feat(design): 공통 컴포넌트 6종 (시안 A 패턴 추출)`
3. `feat(design): 회원 7페이지 시안 A 정밀 매칭`
4. `feat(design): 관리자 4페이지 시안 A 매칭`
5. `feat(design): 강사 2페이지 시안 A 매칭`
6. `test(visual): Playwright 시각 회귀 baseline 24개`
