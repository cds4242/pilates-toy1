# 운영 초기 세팅 가이드

## 실행 시점
- 운영 서버 **최초 배포 시 1회만** 실행

## 실행 전 준비

### 1. 환경변수 설정

| 변수 | 설명 | 예시 |
|------|------|------|
| `INITIAL_ADMIN_LOGIN_ID` | 초기 슈퍼 관리자 로그인 ID | `super_admin` |
| `INITIAL_ADMIN_PASSWORD_HASH` | BCrypt 해시 (평문 X) | `$2a$12$...` |

### 2. BCrypt 해시 생성

```bash
# Python (bcrypt 패키지 필요)
python3 -c "import bcrypt; print(bcrypt.hashpw(b'여기에비밀번호', bcrypt.gensalt(12)).decode())"

# 또는 Spring Boot CLI
spring encodepassword 여기에비밀번호

# 또는 온라인: https://bcrypt-generator.com/ (운영 비밀번호는 오프라인 도구 권장)
```

## 실행 방법

```bash
# 환경변수 설정
export INITIAL_ADMIN_LOGIN_ID=super_admin
export INITIAL_ADMIN_PASSWORD_HASH='$2a$12$생성된해시값'

# envsubst로 환경변수 치환 후 실행
envsubst < prod_init.sql | mysql -h <DB_HOST> -u <DB_USER> -p <DB_NAME>
```

## 첫 로그인 후 필수 작업
1. `POST /api/admin/auth/login` 으로 로그인
2. 강사 등록: `POST /api/admin/instructors`
3. 강사 admin 계정 등록: admins 테이블에 role=INSTRUCTOR, instructor_id 연결
4. 초기 비밀번호 변경 (비밀번호 변경 API 구현 후)

## 주의사항
- Flyway가 관리하지 않는 수동 스크립트
- `envsubst` 없이 실행하면 `${변수명}` 문자열이 그대로 INSERT → 로그인 불가 (의도된 안전장치)
- 2회 실행 시 DUPLICATE KEY 에러 발생 (멱등성 없음)
- 실행 후 `prod_init.sql`은 서버에서 삭제할 것
