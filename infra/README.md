# 인프라 설정 가이드

## Docker Compose (로컬 개발)

```bash
cd infra
docker-compose up -d
```

- MySQL 8.0: `localhost:3306` (pilates/pilates1234)
- Redis 7: `localhost:6379`

## Cloudflare R2 (파일 저장소)

### 버킷 정보
- **버킷명**: `pilates-member-profile-images`
- **용도**: 회원 프로필 사진 저장
- **파일 규격**: JPG/PNG/WebP, 최대 5MB
- **리사이즈**: 200x200 (목록), 500x500 (상세)

### 환경변수

```env
R2_ACCESS_KEY=your-access-key
R2_SECRET_KEY=your-secret-key
R2_BUCKET=pilates-member-profile-images
R2_ENDPOINT=https://your-account-id.r2.cloudflarestorage.com
```

### CORS 정책

```json
[
  {
    "AllowedOrigins": ["http://localhost:3000", "https://your-domain.com"],
    "AllowedMethods": ["GET", "PUT"],
    "AllowedHeaders": ["*"],
    "MaxAgeSeconds": 3600
  }
]
```

### 파일 경로 규칙

```
{bucket}/{member_public_id}/{timestamp}_{size}.{ext}
```

예시:
```
pilates-member-profile-images/550e8400e29b41d4a716446655440000/1715500800_200.webp
pilates-member-profile-images/550e8400e29b41d4a716446655440000/1715500800_500.webp
```
