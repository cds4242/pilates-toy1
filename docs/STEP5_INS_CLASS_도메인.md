# STEP 5: 강사 + 수업 도메인 구현

## 작업 일시
2026-05-12 ~

## 작업 요약
강사(Instructor) 관리 + 수업 유형(LessonType) + 시간표(FixedSchedule → ClassSchedule) 도메인을 구현한다.
엔티티는 이미 STEP 3에서 생성됨. Repository → Service → Controller → 테스트 순서로 진행.

## 현재 상태 (STEP 3 완료 기준)

### 이미 존재하는 것
- **Entity**: Instructor, InstructorAvailableTime, InstructorStatus
- **Entity**: LessonType, FixedSchedule, ClassSchedule, ClassScheduleStatus
- **DB 테이블**: V1 마이그레이션으로 생성 완료
- **인덱스**: V2 마이그레이션으로 추가 완료
- **ClassSchedule.@Version**: 낙관적 락 적용 완료

### 아직 없는 것
- Repository, Service, Controller, DTO
- 도메인 메서드 구현 (placeholder만 있음)
- 고정 스케줄 → 수업 자동 생성 배치
- 강사 근무 시간 외 수업 배정 방지 검증

## Phase 구성

| Phase | 내용 | 상태 |
|-------|------|------|
| 1 | 강사 CRUD + 근무 가능 시간 관리 | 대기 |
| 2 | 수업 유형 CRUD (LessonType) | 대기 |
| 3 | 고정 스케줄 CRUD + 수업 자동 생성 | 대기 |
| 4 | 수업 시간표 조회 + 수업 취소/휴강 | 대기 |
| 5 | 통합 테스트 + Swagger | 대기 |

---

## Phase 1: 강사 CRUD + 근무 가능 시간

### 구현 범위
- InstructorRepository, InstructorAvailableTimeRepository
- InstructorService: CRUD + 상태 변경 + 근무 시간 관리
- InstructorController: 관리자 전용 API

### API 설계
| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| POST | /api/admin/instructors | 강사 등록 | ADMIN |
| GET | /api/admin/instructors | 강사 목록 조회 | ADMIN |
| GET | /api/admin/instructors/{id} | 강사 상세 조회 | ADMIN |
| PATCH | /api/admin/instructors/{id} | 강사 정보 수정 | ADMIN |
| PATCH | /api/admin/instructors/{id}/status | 강사 상태 변경 | ADMIN |
| PUT | /api/admin/instructors/{id}/available-times | 근무 시간 설정 | ADMIN |
| GET | /api/admin/instructors/{id}/available-times | 근무 시간 조회 | ADMIN |

### 비즈니스 규칙
- 강사 삭제는 soft delete (수업 이력 보존)
- 비활성(INACTIVE) 강사에게 새 수업 배정 불가
- 근무 시간 변경 시 기존 수업과 충돌 확인 (경고만, 차단 X)

### Entity 도메인 메서드 구현
```java
// Instructor
updateInfo(name, phone, profileImageUrl) → 필드 업데이트
changeStatus(ACTIVE/INACTIVE) → 상태 전이 (INACTIVE 시 미래 수업 경고)

// InstructorAvailableTime
// 별도 메서드 불필요 (CRUD로 관리)
```

---

## Phase 2: 수업 유형 CRUD

### 구현 범위
- LessonTypeRepository
- LessonTypeService: CRUD + 활성/비활성
- LessonTypeController: 관리자 전용 API

### API 설계
| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| POST | /api/admin/lesson-types | 수업 유형 등록 | ADMIN |
| GET | /api/admin/lesson-types | 수업 유형 목록 | ADMIN |
| PATCH | /api/admin/lesson-types/{id} | 수업 유형 수정 | ADMIN |

### 비즈니스 규칙
- 사용 중인 수업 유형은 삭제 불가 (비활성만 가능)
- deduction_count: 개인=2, 나머지=1 (관리자 설정 가능)
- 시드 데이터: 개인(정원1,50분,2회차감), 듀엣(2,50분,1), 그룹(8,50분,1), 체험(1,50분,1)

---

## Phase 3: 고정 스케줄 + 수업 자동 생성

### 구현 범위
- FixedScheduleRepository, ClassScheduleRepository
- FixedScheduleService: 고정 스케줄 CRUD
- ClassScheduleGenerationService: 고정 스케줄 → 수업 자동 생성 (배치)
- 스케줄러: 매주 일요일 자정, 다음 주 수업 자동 생성

### API 설계
| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| POST | /api/admin/fixed-schedules | 고정 스케줄 등록 | ADMIN |
| GET | /api/admin/fixed-schedules | 고정 스케줄 목록 | ADMIN |
| PATCH | /api/admin/fixed-schedules/{id} | 고정 스케줄 수정 | ADMIN |
| DELETE | /api/admin/fixed-schedules/{id} | 고정 스케줄 삭제 (비활성) | ADMIN |
| POST | /api/admin/class-schedules/generate | 다음 N주 수업 수동 생성 | ADMIN |

### 비즈니스 규칙
- 강사 근무 시간 외 스케줄 등록 시 검증 에러
- 같은 강사의 시간 겹침 방지
- 고정 스케줄 비활성화 시 미래 미예약 수업 자동 취소
- 수업 자동 생성: 중복 방지 (같은 날짜+시간+강사 이미 존재 시 skip)

### 자동 생성 로직
```
1. 활성 FixedSchedule 전체 조회
2. 대상 주의 각 요일에 대해
3. FixedSchedule.dayOfWeek와 매칭되는 날짜 계산
4. 해당 날짜가 holidays 테이블에 있으면 skip
5. 이미 같은 (instructor_id, class_date, start_time) 존재하면 skip
6. ClassSchedule INSERT (status=SCHEDULED, currentCount=0)
```

---

## Phase 4: 수업 시간표 조회 + 취소/휴강

### 구현 범위
- ClassScheduleService: 조회 + 상태 변경
- ClassScheduleController: 관리자 + 강사용 API

### API 설계
| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| GET | /api/admin/class-schedules | 주간/일별 수업 조회 | ADMIN |
| GET | /api/admin/class-schedules/{id} | 수업 상세 (예약자 목록 포함) | ADMIN |
| POST | /api/admin/class-schedules | 단건 수업 등록 | ADMIN |
| PATCH | /api/admin/class-schedules/{id}/cancel | 수업 취소 (휴강) | ADMIN |
| PATCH | /api/admin/class-schedules/{id}/complete | 수업 완료 처리 | ADMIN |
| GET | /api/instructor/schedules | 강사 본인 수업 조회 | INSTRUCTOR |
| GET | /api/instructor/schedules/{id} | 강사 본인 수업 상세 | INSTRUCTOR |

### 비즈니스 규칙
- 수업 취소 시 해당 예약 전체 취소 + 정기권 복구 (reservation 도메인 연동)
  - STEP 5에서는 TODO placeholder, reservation 도메인 완성 후 연결
- 수업 완료: 미출석 예약을 노쇼로 자동 전환 (attendance 도메인 연동)
  - STEP 5에서는 TODO placeholder
- currentCount: 예약 시 increment, 취소 시 decrement (낙관적 락)
- 과거 수업 수정 불가

### Entity 도메인 메서드 구현
```java
// ClassSchedule
incrementCount() → currentCount < maxCapacity 검증 후 +1
decrementCount() → currentCount > 0 검증 후 -1
cancel() → SCHEDULED → CANCELLED 전이
complete() → SCHEDULED → COMPLETED 전이
```

---

## Phase 5: 통합 테스트 + Swagger

### E2E 시나리오
1. 강사 등록 → 근무 시간 설정 → 정보 수정 → 비활성화
2. 수업 유형 등록 → 수정 → 비활성화
3. 고정 스케줄 등록 → 수업 자동 생성 → 주간 조회
4. 단건 수업 등록 → 취소 (휴강) → 완료
5. 강사 근무 시간 외 스케줄 등록 → 검증 에러

### Swagger 문서화
- @Tag: Instructor, LessonType, Schedule
- 모든 엔드포인트 @Operation, @ApiResponses
- DTO @Schema

---

## 기술 제약
- Service, Repository, Controller 코드 작성 (Entity는 도메인 메서드만 추가)
- 관리자 권한 체크: @PreAuthorize("hasRole('ADMIN')") 또는 서비스에서 검증
- 강사 로그인: Admin 엔티티와 연결 (admin.instructor_id FK)
  - STEP 5에서는 관리자 API만 구현, 강사 전용 API는 Admin 인증 후 instructor_id로 필터
- 예약/출석 도메인 의존: TODO placeholder로 처리

## 패키지 구조 (예상)
```
domain/instructor/
├── controller/  InstructorController
├── dto/         InstructorRequest, InstructorResponse, AvailableTimeRequest 등
├── repository/  InstructorRepository, InstructorAvailableTimeRepository
└── service/     InstructorService

domain/classroom/
├── controller/  LessonTypeController, FixedScheduleController, ClassScheduleController
├── dto/         LessonTypeRequest, ClassScheduleResponse, WeeklyScheduleResponse 등
├── repository/  LessonTypeRepository, FixedScheduleRepository, ClassScheduleRepository
└── service/     LessonTypeService, FixedScheduleService, ClassScheduleService, ClassScheduleGenerationService
```
