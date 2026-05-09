# STEP 0: 디자인 시안 작성

## 작업 일시
2026-05-09 ~ 2026-05-12

## 작업 요약
의뢰인 확인용 HTML 디자인 시안 3종(A/B/C)을 작성.
회원(모바일) + 강사(모바일) + 관리자(PC) 총 11개 화면, 반응형 적용.
더블클릭으로 브라우저에서 바로 확인 가능한 순수 HTML/CSS 시안.

## 시안 구성

### 시안 A: 미니멀 모던 (**채택**)
- 메인 컬러: #F8C8DC → **#F0A0B5** (채도 상향 확정)
- 폰트: Pretendard (400/600/700)
- 버튼: border-radius 8px, 핑크 채움
- 톤: 클래스101 / 자비스 미니멀
- 관리자 사이드바: 화이트

### 시안 B: 따뜻한 부티크
- 메인 컬러: #E8A4A4 (코랄) + #F5E6D3 (베이지)
- 폰트: Pretendard + Playfair Display (영문 헤더)
- 버튼: border-radius 24px, 아웃라인 → hover 채움
- 톤: 호텔 라운지, 고급스러움
- 관리자 사이드바: 크림 베이지

### 시안 C: 프레시 액티브
- 메인 컬러: #A8E6CF (민트) → #FF6B9D (핑크) 그라데이션
- 폰트: Pretendard 헤비 (700/800/900)
- 버튼: border-radius 12px, 그라데이션
- 톤: 클래스패스, 에너제틱
- 관리자 사이드바: 다크 (#2C3E50)

## 포함 화면 (시안당 11개)

### 회원 (모바일 우선, 375px 기준)
1. `member/login.html` — 휴대폰번호 + 비밀번호 로그인
2. `member/home.html` — 정기권 현황, 다음 예약 3건, 빠른 예약
3. `member/reservation.html` — 날짜 선택, 시간표, 예약 확인 모달
4. `member/mypage.html` — 프로필, 출석 이력, 설정 메뉴

### 강사 (모바일 우선, 375px 기준)
5. `instructor/login.html` — 이메일 + 비밀번호 (강사 전용 구분)
6. `instructor/schedule.html` — 오늘/내일/이번 주 수업 리스트
7. `instructor/attendance.html` — 회원별 출석/지각/결석 체크

### 관리자 (PC 우선, 1280px 기준, 반응형)
8. `admin/login.html` — 이메일 + 비밀번호 + 2FA 코드
9. `admin/dashboard.html` — KPI 카드, 수업 리스트, 알림, 매출 차트
10. `admin/members.html` — 회원 테이블(PC) / 카드(모바일), 검색, 등록
11. `admin/schedule.html` — 주간 캘린더(PC) / 리스트(모바일), 수업 등록

## 기술 제약
- 순수 HTML + CSS `<style>` 태그 (외부 파일 없음)
- JS 최소 (탭 전환, 모달, 사이드바 토글)
- 폰트: Pretendard CDN
- 아이콘: Heroicons SVG 인라인
- CSS 변수로 디자인 토큰 관리 (색상 일괄 변경 용이)
- `<meta name="color-scheme" content="light">` 다크모드 차단
- 관리자 반응형: 768px 기준 사이드바 → 햄버거, 테이블 → 카드

## 더미 데이터 (3종 통일)
- 회원: 김민지, 12회권 잔여 8/12, 만료 2026.07.30
- 강사: 박지영, 이수진, 최재훈
- 예약: 5/10 19:00 그룹, 5/13 19:00 그룹, 5/15 10:00 듀엣
- 관리자: 김OO 원장님
- 회원 리스트: 10명 (다양한 정기권 상태)

## 폴더 구조
```
design-samples/
├── README.md
├── A-minimal-modern/
│   ├── index.html
│   ├── member/    (4개)
│   ├── instructor/ (3개)
│   └── admin/     (4개)
├── B-warm-boutique/  (동일 구조)
└── C-fresh-active/   (동일 구조)
```

## 의뢰인 피드백 반영 (2026-05-12)

### 시안 A 채택 + 미세 조정 5건
1. 메인 핑크 채도 상향 (#F8C8DC → #F0A0B5)
2. 카드 모서리 12px → 18px
3. 본문 글씨 크기 14px → 15px
4. 관리자 대시보드 캘린더 뷰 추가 (리스트 ↔ 캘린더 토글)
5. 로그인 화면 학원 사진 영역 추가

### 추가 기능: 회원 프로필 사진 (v1 포함)
- member/signup.html 신규 페이지 (프로필 사진 등록)
- member/mypage.html 프로필 사진 업로드/변경 영역
- instructor/attendance.html 회원 아바타 표시 (사진 or 이니셜)
- admin/members.html 프로필 컬럼 추가 (PC 테이블 + 모바일 카드)
- 변경 합의서: docs/contracts/CHANGE_ORDER_001_profile_image.md

### 견적 변경
- 480만원 → 510만원 (+30만원)
- 일정 +3일 (6/30 오픈 유지)

### 명세·DB 업데이트
- SPEC.md: 회원 프로필 사진 + 파일 저장(3.8) + 의뢰인 책임 + 변경 관리 섹션 추가
- ERD.md: members 테이블에 profile_image_url, profile_image_uploaded_at 추가
- DB_DESIGN_DECISIONS.md: 프로필 사진 저장 정책 (R2, 리사이즈, fallback) 기록
- Member.java: profileImageUrl, profileImageUploadedAt 필드 추가
- V3__add_member_profile_image.sql: ALTER TABLE members 마이그레이션
- infra/README.md: Cloudflare R2 설정 가이드 추가

### Flyway 검증 결과
- V3 마이그레이션 MySQL 적용 성공 확인 (`now at version v3`)

## 학원 사진 적용 (2026-05-12)

의뢰인으로부터 학원 사진 3장 수령, 로그인 화면에 적용 확정.

| 로그인 페이지 | 사진 파일 | 비고 |
|--------------|----------|------|
| 회원 로그인 | studio.jpg | 기본 |
| 강사 로그인 | studio2.jpg | object-position 30% (위쪽 포커스) |
| 관리자 로그인 | studio3.jpg | object-position 30% (위쪽 포커스) |

- placeholder 구조 유지 (사진 교체 시 src만 변경)
- 160px 높이, object-fit: cover, border-radius: 18px

## 커밋 이력

| 커밋 | 내용 |
|------|------|
| `5279402` | feat(design): 회원/강사/관리자 디자인 시안 3종 추가 (A/B/C) |
| `69862b2` | feat(design): 시안 A 의뢰인 피드백 반영 (디자인 토큰, 학원 사진 영역) |
| `601c45c` | feat(design): 관리자 대시보드 캘린더 뷰 추가 |
| `a94d260` | feat(member): 회원 프로필 사진 기능 추가 (시안 + Entity + 마이그레이션) |
| `709805b` | docs: 변경 합의서 #1 추가 및 SPEC·인프라 문서 업데이트 |
| `5087e46` | feat(design): 로그인 3종에 학원 사진 적용 |

## 다음 단계
- 확정된 시안 A 기준으로 프론트엔드 컴포넌트 개발 (STEP 4)
