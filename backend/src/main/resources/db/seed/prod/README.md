# 운영 초기 세팅 가이드

## 실행 시점
- 운영 서버 **최초 배포 시 1회만** 실행

## 실행 전 준비
1. BCrypt 해시 생성 (관리자 비밀번호)
   ```bash
   # Spring Boot CLI 또는 온라인 도구 사용
   # 예: https://bcrypt-generator.com/
   ```
2. `prod_init.sql`에서 관리자 INSERT 주석 해제 후 해시 값 교체

## 실행 방법
```bash
mysql -h <DB_HOST> -u <DB_USER> -p <DB_NAME> < prod_init.sql
```

## 주의사항
- Flyway가 관리하지 않는 수동 스크립트임
- 2회 실행 시 DUPLICATE KEY 에러 발생 (의도된 동작, 멱등성 없음)
- 실행 후 `prod_init.sql`은 서버에서 삭제할 것
