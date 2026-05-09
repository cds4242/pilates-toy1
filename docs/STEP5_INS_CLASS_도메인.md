# STEP 5: 강사 + 수업 도메인 구현

## 작업 일시
2026-05-12

## 작업 요약
강사(Instructor) 관리 + 수업 유형(LessonType) + 시간표(FixedSchedule → ClassSchedule) + 자동 생성 스케줄러.
엔티티 비즈니스 메서드 구현 + Repository + Service + Controller + E2E 테스트 + Swagger.

## Phase 구성

| Phase | 내용 | 상태 |
|-------|------|------|
| 1 | 강사 CRUD + 근무 가능 시간 관리 | 완료 |
| 2 | 수업 유형 CRUD (LessonType) | 완료 |
| 3 | 고정 스케줄 CRUD + 수업 자동 생성 | 완료 |
| 4 | 수업 시간표 조회 + 수업 취소/휴강 | 완료 |
| 5 | 강사 본인 수업 조회 + 시간 충돌 검증 | 완료 |
| 6 | 통합 테스트 + Swagger + 문서 | 완료 |

## 구현 API 목록 (28개)

### 강사 관리 — Admin (8 API)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | /api/admin/instructors | 강사 등록 |
| GET | /api/admin/instructors | 강사 목록 (전체) |
| GET | /api/admin/instructors/{id} | 강사 상세 |
| PATCH | /api/admin/instructors/{id} | 강사 정보 수정 |
| DELETE | /api/admin/instructors/{id} | 강사 비활성화 |
| POST | /api/admin/instructors/{id}/activate | 강사 활성화 |
| PUT | /api/admin/instructors/{id}/available-times | 근무 시간 설정 |
| GET | /api/admin/instructors/{id}/available-times | 근무 시간 조회 |

### 강사 — Public (2 API)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | /api/instructors | 강사 목록 (활성만) |
| GET | /api/instructors/{publicId} | 강사 상세 (공개) |

### 수업 유형 — Admin (4 API)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | /api/admin/lesson-types | 수업 유형 등록 |
| GET | /api/admin/lesson-types | 수업 유형 목록 (전체) |
| PATCH | /api/admin/lesson-types/{id} | 수업 유형 수정 |
| DELETE | /api/admin/lesson-types/{id} | 수업 유형 비활성화 |

### 수업 유형 — Public (1 API)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | /api/lesson-types | 수업 유형 목록 (활성만) |

### 고정 스케줄 — Admin (4 API)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | /api/admin/fixed-schedules | 고정 스케줄 등록 |
| GET | /api/admin/fixed-schedules | 고정 스케줄 목록 |
| PATCH | /api/admin/fixed-schedules/{id} | 고정 스케줄 수정 |
| DELETE | /api/admin/fixed-schedules/{id} | 고정 스케줄 비활성화 |

### 수업 시간표 — Admin (5 API)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | /api/admin/class-schedules | 단건 수업 등록 |
| GET | /api/admin/class-schedules | 날짜 범위 수업 조회 |
| GET | /api/admin/class-schedules/{id} | 수업 상세 |
| POST | /api/admin/class-schedules/{id}/cancel | 휴강 처리 |
| POST | /api/admin/class-schedules/generate | 자동 생성 (N주치) |

### 수업 시간표 — Instructor (2 API)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | /api/instructor/class-schedules | 강사 본인 수업 조회 |
| GET | /api/instructor/class-schedules/{id} | 강사 본인 수업 상세 |

### 수업 시간표 — Member (2 API)
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | /api/class-schedules | 수업 목록 (예약용) |
| GET | /api/class-schedules/{id} | 수업 상세 |

## 검증 항목

| # | 항목 | 상태 |
|---|------|------|
| 1 | 강사 전용 API (본인 수업 조회) | 정상 |
| 2 | 자동 생성 공휴일 제외 | 정상 |
| 3 | 강사 가능 시간 검증 (FIXED_003) | 정상 |
| 4a | 고정 스케줄 시간 충돌 (FIXED_002) | 정상 |
| 4b | 단건 수업 시간 충돌 (CLASS_005) | 정상 |
| 5 | 비활성 fixed_schedule 제외 | 정상 |
| 6 | 휴강 TODO [STEP 8] 라벨 | 정상 |
| 7 | 회원 예약 상태 TODO [STEP 8] | 정상 |
| 8 | @EnableScheduling | 정상 |

## E2E 테스트 (ClassroomE2ETest)

| 시나리오 | 내용 | 상태 |
|----------|------|------|
| 1 | 강사 등록→시간대→수업유형→고정스케줄→자동생성→시간외거부 | 통과 |
| 2 | 단건 수업→휴강→CANCELLED 확인 | 통과 |
| 3 | 회원 공개 조회 (강사/수업유형/시간표) | 통과 |
| 4 | 자동 생성 멱등성 (2회 호출→중복 없음) | 통과 |

## 생성/수정 파일 (40+개)

### 신규 파일
- Repository 6개 (Instructor, AvailableTime, LessonType, FixedSchedule, ClassSchedule, Holiday)
- DTO 13개 (요청/응답 record, 전부 @Schema)
- Service 4개 (Instructor, LessonType, FixedSchedule, ClassSchedule)
- Scheduler 1개 (ClassScheduleGenerator)
- Controller 8개 (Admin 5 + Public 2 + Instructor 1)
- E2E 테스트 1개 (ClassroomE2ETest, 4시나리오)

### 수정 파일
- Entity 4개 (Instructor, LessonType, FixedSchedule, ClassSchedule 도메인 메서드)
- ErrorCode 15개 추가
- SecurityConfig 공개 경로 추가
- 시드 데이터 (강사 3명, 시간대, 고정 스케줄, 공휴일)
- ARCHITECTURE.md 갱신 (classroom 섹션)

## 커밋 이력

| 해시 | 메시지 |
|------|--------|
| b1feb02 | feat(instructor,classroom): Entity 비즈니스 메서드 + Repository + ErrorCode |
| ff6b15d | feat(instructor,classroom): Service + Controller + DTO + 자동 생성 스케줄러 |
| faa7fc9 | feat(classroom): SecurityConfig + 시드 + E2E 4시나리오 + 문서 |
| bd325bc | feat(instructor): 강사 본인 수업 조회 API |
| 5018f92 | fix(classroom): 단건 수업 시간 충돌 검증 추가 |
| fc114f5 | docs: STEP 8 연결 TODO 주석 보강 |

## STEP 8 연결 대기 (TODO)

| 위치 | 내용 |
|------|------|
| ClassScheduleService.cancelClass | 휴강 시 예약 자동 취소 + 정기권 복구 |
| ClassScheduleService.completeClass | 수업 완료 시 노쇼 자동 전환 |
| ClassScheduleService.getDetailForInstructor | 예약자 리스트 응답 포함 |
| MemberClassScheduleController.listClasses | 회원 본인 예약 상태 표시 (myReservationStatus) |
| MemberClassScheduleController.getDetail | 수업 상세 본인 예약 여부 |
