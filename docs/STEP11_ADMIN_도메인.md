# STEP 11: 관리자(Admin) 도메인 풀스택 구현

## 작업 일시
2026-05-09

## 작업 요약
대시보드 + 회원 관리 + 통계 + 엑셀 일괄 + 학원 설정.
의뢰인이 매일 사용하는 핵심 화면. 데이터 정확성과 응답 속도가 핵심.

## 완료 상태

### 신규 생성
- **V12**: member_memos admin_id 추가, instructor_id nullable, deleted_at, 통계 인덱스 6개
- **AdminDashboardService**: 4영역 통합 쿼리 (수업·매출·만료·알림)
- **AdminMemberService**: 8도메인 통합 조회, 메모 CRUD, 강제 탈퇴
- **AdminStatisticsService**: 매출·회원·출석·인기시간 4종 통계
- **AdminBulkImportService**: 엑셀 회원 등록 (부분 성공), 정기권 발급, 매출 다운로드
- **AdminSettingsService**: 학원 설정 Key-Value CRUD
- **MemberMemoRepository**: 신규 (기존 엔티티 있었으나 레포 없었음)
- **StudioSettingRepository**: 신규

### 수정
- **MemberMemo Entity**: admin_id 추가, instructor_id nullable, deleted_at + softDelete() 추가
- **StudioSetting Entity**: updateValue() 메서드 추가
- **ErrorCode**: ADMIN_010~017 추가 (8개)
- **SecurityConfig**: `/api/admin/settings/**` → SUPER_ADMIN 전용 분리
- **build.gradle.kts**: Apache POI 5.2.5 의존성 추가

## API 엔드포인트

### 대시보드
| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| GET | /api/admin/dashboard | 대시보드 통합 조회 | ADMIN/SUPER_ADMIN |

### 회원 관리
| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| GET | /api/admin/members | 회원 검색 (이름/phone, 상태, 페이징) | ADMIN/SUPER_ADMIN |
| GET | /api/admin/members/{id} | 회원 상세 (8도메인 통합) | ADMIN/SUPER_ADMIN |
| GET | /api/admin/members/{id}/memos | 회원 메모 목록 | ADMIN/SUPER_ADMIN |
| POST | /api/admin/members/{id}/memos | 회원 메모 작성 | ADMIN/SUPER_ADMIN |
| PATCH | /api/admin/members/{id}/memos/{memoId} | 회원 메모 수정 (작성자만) | ADMIN/SUPER_ADMIN |
| DELETE | /api/admin/members/{id}/memos/{memoId} | 회원 메모 삭제 (작성자만) | ADMIN/SUPER_ADMIN |
| DELETE | /api/admin/members/{id} | 강제 탈퇴 | ADMIN/SUPER_ADMIN |

### 통계
| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| GET | /api/admin/statistics/revenue | 매출 통계 (일/주/월별) | ADMIN/SUPER_ADMIN |
| GET | /api/admin/statistics/members | 회원 추이 (가입/탈퇴) | ADMIN/SUPER_ADMIN |
| GET | /api/admin/statistics/attendance | 출석률 (전체/강사별/유형별) | ADMIN/SUPER_ADMIN |
| GET | /api/admin/statistics/popular-times | 인기 시간대 (시간/요일별) | ADMIN/SUPER_ADMIN |

### 엑셀 일괄
| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| POST | /api/admin/members/bulk | 회원 일괄 등록 (엑셀) | ADMIN/SUPER_ADMIN |
| GET | /api/admin/members/bulk/template | 회원 등록 템플릿 다운로드 | ADMIN/SUPER_ADMIN |
| POST | /api/admin/memberships/bulk | 정기권 일괄 발급 (엑셀) | ADMIN/SUPER_ADMIN |
| GET | /api/admin/statistics/revenue/excel | 매출 엑셀 다운로드 | ADMIN/SUPER_ADMIN |

### 학원 설정
| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| GET | /api/admin/settings | 학원 설정 조회 | **SUPER_ADMIN만** |
| PATCH | /api/admin/settings | 학원 설정 수정 | **SUPER_ADMIN만** |

## 대시보드 통합 쿼리 전략

| 영역 | 데이터 소스 | 쿼리 | N+1 방지 |
|------|------------|------|---------|
| 오늘 수업 | class_schedules | classDate=today, ≠CANCELLED | instructor/lessonType LAZY |
| 이번 주 매출 | payments | paid_at BETWEEN 월~일 | COMPLETED/REFUNDED 필터 |
| 만료 임박 | memberships | end_date ≤ today+7, ACTIVE | member ID 일괄 조회 |
| 노쇼 알림 | attendances | 30일 NO_SHOW ≥ 3 GROUP BY | member ID 일괄 조회 |
| 잔여 부족 | memberships | remaining_count ≤ 1, !unlimited | member ID 일괄 조회 |

## 엑셀 처리 설계

- **부분 성공**: 행별 독립 트랜잭션 (`TransactionTemplate`). 50행째 실패해도 1~49행 유지.
- **검증**: 휴대폰 형식(01X-XXXX-XXXX), phone_hash 중복, 필수 필드
- **제한**: 5MB / 1000행
- **자원 관리**: Workbook try-with-resources (메모리 누수 방지)

## 테스트 결과

### 시나리오 카운트
| 클래스 | 시나리오 |
|--------|---------|
| AdminDashboardE2ETest | 3 |
| AdminMemberE2ETest | 6 |
| AdminStatisticsE2ETest | 5 |
| AdminBulkImportE2ETest | 5 |
| AdminSettingsE2ETest | 4 |
| **합계** | **23** |

### 빌드
- **109 tests, 0 failures, 1 skipped** (skip = STEP 8 기존)
- `./gradlew clean test` BUILD SUCCESSFUL

## 커밋 이력 (8개)
1. `feat(admin): V12 마이그레이션 + ErrorCode + POI 의존성 + SecurityConfig 갱신`
2. `feat(admin): 대시보드 API + Service (실시간 통합 쿼리)`
3. `feat(admin): 회원 검색·상세 + 메모 CRUD + 강제 탈퇴`
4. `feat(admin): 통계 API (매출·회원·출석·인기시간)`
5. `feat(admin): 엑셀 일괄 등록 + 정기권 발급 + 매출 다운로드`
6. `feat(admin): 학원 설정 API (SUPER_ADMIN 전용)`
7. `test(integration): admin 도메인 E2E 5개 클래스 23 시나리오`
8. `docs(architecture): admin 도메인 통합 쿼리 + 엑셀 흐름 + 권한 매트릭스`

## 알려진 제한

- **회원 검색**: 소규모 운영 가정. 전체 조회 + 메모리 필터링. 대규모 시 QueryDSL 전환 필요.
- **V13 미생성**: studio_settings는 V1에 이미 Key-Value 구조로 존재. 스펙의 단일 row 구조 대신 기존 활용.
- **정기권 일괄 발급**: 기본 10회/30일로 발급. 실제 운영에서는 passCode로 MembershipPass 매핑 필요.
