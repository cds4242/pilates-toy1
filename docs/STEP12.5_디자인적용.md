# STEP 12.5: 시안 A 디자인 정밀 적용

## 작업 일시
2026-05-09 ~ 2026-05-10

## 작업 요약
시안 A (Minimal Modern) 디자인 토큰 + 12페이지 정밀 매칭 + placeholder 5개 보강 + Chrome MCP 픽셀 검증.

## 완료 상태

### 디자인 토큰
- Primary: #F0A0B5, Dark: #D88A9E, Light: #FAD4DE
- Text: title #2D2D2D, body #6B6B6B, sub #9B9B9B
- Radius: sm 8px, md 18px. Pretendard CDN.

### 공통 컴포넌트 6종
PassCard, ClassCard, StatusBadge, SectionTitle, KpiCard, MobileTabBar

### 12페이지 시안 매칭 점수 (보강 후)

| 페이지 | 점수 | 변경 |
|--------|------|------|
| member/login | 9 | 시안 구조 100% |
| member/signup | 8 | 3단계 + 프로필 사진 + 성별 라디오 |
| member/home | 9 | PassCard + 예약 리스트 + 알림벨 |
| member/schedule | 8 | 날짜 선택 + 수업 카드 |
| member/profile | 8 | 아바타 + PassCard + 메뉴 |
| member/membership | 8 | **보강**: PassCard + 지난 수강권 + CTA |
| member/reservations | 8 | **보강**: 다가오는/지난 분리 + 취소 + StatusBadge |
| admin/dashboard | 9 | KPI + 수업 + 알림 + 매출 차트 |
| admin/members | 8 | **보강**: 검색 + 테이블(PC) + 카드(모바일) + 페이징 |
| admin/classes | 8 | **보강**: 수업 추가 폼 + 주간 캘린더 + 모바일 리스트 |
| instructor/schedule | 7 | 탭 + 빈 상태 |
| instructor/attendance | 8 | **보강**: 멤버 리스트 + 출석/지각/결석 3버튼 |

- **9점 이상**: 3개
- **8점**: 8개
- **7점 이하**: 1개 (instructor/schedule — 시안의 수업 카드 목록은 API 연동 필요)
- **평균**: 8.2점

### Chrome MCP 비교
| 페이지 | 비교 결과 |
|--------|----------|
| login | 구조 일치, 스튜디오 사진 → 그라디언트 대체 |
| dashboard | KPI+수업+알림+차트 구조 일치 |
| members | 사이드바+검색+테이블 구조 일치, 컬럼 수 차이(8→5) |

### Playwright 시각 회귀
- 24 baselines (12페이지 x 2 viewport) — 갱신 완료
- 24 passed, 0 failed

## 커밋 이력 (보강분 4개)
1. `feat(design): 회원 정기권 + 예약 이력 페이지 시안 매칭`
2. `feat(design): 관리자 회원 테이블 + 시간표 캘린더 시안 매칭`
3. `feat(design): 강사 출석 체크 시안 매칭`
4. (baseline 변경 없어 추가 커밋 불필요)
