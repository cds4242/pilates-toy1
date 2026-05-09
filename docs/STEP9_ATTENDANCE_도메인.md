# STEP 9: 출석(Attendance) 도메인 구현

## 작업 일시
2026-05-12 ~

## 작업 요약
강사의 출석 체크 + 출석 이력 조회 + STEP 8 노쇼 스케줄러 연동.
예약 1건당 출석 기록 1건 (1:1).

## 현재 상태

### 이미 존재하는 것
- **Entity**: Attendance, AttendanceStatus (ATTENDED, ABSENT, LATE)
- **DB 테이블**: V1 마이그레이션으로 생성 (reservation_id UNIQUE)
- **NoShowMarkingScheduler**: STEP 8에서 구현 (Attendance 없는 CONFIRMED → NO_SHOW)

### 아직 없는 것
- Repository, Service, Controller, DTO
- 강사 출석 체크 API
- 출석 이력 조회 (회원/강사/관리자)

## Phase 구성

| Phase | 내용 | 상태 |
|-------|------|------|
| 1 | Attendance Entity 비즈니스 메서드 + Repository | 대기 |
| 2 | 강사 출석 체크 Service + Controller | 대기 |
| 3 | 출석 이력 조회 (회원/강사/관리자) | 대기 |
| 4 | 노쇼 스케줄러 연동 보강 | 대기 |
| 5 | E2E 테스트 + Swagger | 대기 |

## API 설계

### 강사 출석 체크
| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| POST | /api/instructor/class-schedules/{id}/attendance | 일괄 출석 체크 | INSTRUCTOR |
| GET | /api/instructor/class-schedules/{id}/attendance | 출석 현황 조회 | INSTRUCTOR |

### 관리자
| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| GET | /api/admin/class-schedules/{id}/attendance | 출석 현황 | ADMIN |
| PATCH | /api/admin/attendance/{id} | 출석 상태 수정 | ADMIN |

### 회원
| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| GET | /api/members/me/attendance | 내 출석 이력 | MEMBER |

## 비즈니스 규칙
- 출석 체크는 수업 당일에만 가능
- 강사는 본인 수업만 출석 체크 가능
- 예약 1건당 출석 1건 (UNIQUE 제약)
- 출석 체크 시 Reservation.status도 갱신 가능 (ATTENDED 연동)
- 노쇼 스케줄러: Attendance 없는 CONFIRMED → NO_SHOW (STEP 8 기존 로직 유지)

## E2E 시나리오
1. 강사가 수업 출석 체크 (출석/지각/결석)
2. 중복 출석 체크 방지
3. 본인 수업 아닌 출석 체크 → 403
4. 회원 출석 이력 조회
5. 관리자 출석 상태 수정
6. 노쇼 스케줄러 + 출석 연동
