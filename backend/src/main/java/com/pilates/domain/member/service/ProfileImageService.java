package com.pilates.domain.member.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.domain.member.entity.Member;
import com.pilates.domain.member.entity.MemberStatus;
import com.pilates.domain.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * 프로필 사진 업로드 서비스.
 * v1: 로컬 파일 시스템 저장 (Cloudflare R2는 나중에 교체).
 * 파일 규격: 최대 5MB, JPG/PNG/WebP.
 */
@Slf4j
@Service
public class ProfileImageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final MemberRepository memberRepository;
    private final Path uploadDir;

    public ProfileImageService(MemberRepository memberRepository,
                               @Value("${app.upload.profile-image-dir:uploads/profile}") String uploadDir) {
        this.memberRepository = memberRepository;
        this.uploadDir = Paths.get(uploadDir);
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            log.warn("프로필 이미지 디렉토리 생성 실패: {}", uploadDir, e);
        }
    }

    /**
     * 프로필 사진 업로드.
     * @param memberId 회원 ID
     * @param file 업로드 파일
     * @return 저장된 이미지 URL
     */
    @Transactional
    public String uploadProfileImage(Long memberId, MultipartFile file) {
        validateFile(file);

        Member member = findActiveMember(memberId);

        // 기존 파일 삭제 (있으면)
        if (member.getProfileImageUrl() != null) {
            deleteFile(member.getProfileImageUrl());
        }

        // 파일 저장
        String fileName = member.getPublicId() + "_" + System.currentTimeMillis() + getExtension(file);
        Path filePath = uploadDir.resolve(fileName);

        try {
            Files.write(filePath, file.getBytes());
        } catch (IOException e) {
            log.error("프로필 이미지 저장 실패: memberId={}", memberId, e);
            throw new BusinessException(ErrorCode.PROFILE_IMAGE_UPLOAD_FAILED);
        }

        // DB 업데이트
        String imageUrl = "/uploads/profile/" + fileName;
        member.updateProfileImage(imageUrl);

        log.info("프로필 사진 업로드: memberId={}, url={}", memberId, imageUrl);
        return imageUrl;
    }

    /**
     * 프로필 사진 삭제.
     */
    @Transactional
    public void deleteProfileImage(Long memberId) {
        Member member = findActiveMember(memberId);

        if (member.getProfileImageUrl() != null) {
            deleteFile(member.getProfileImageUrl());
            member.removeProfileImage();
            log.info("프로필 사진 삭제: memberId={}", memberId);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PROFILE_IMAGE_EMPTY);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PROFILE_IMAGE_TOO_LARGE);
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusinessException(ErrorCode.PROFILE_IMAGE_INVALID_TYPE);
        }
    }

    private String getExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            return originalName.substring(originalName.lastIndexOf("."));
        }
        return ".jpg";
    }

    private void deleteFile(String imageUrl) {
        try {
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            Files.deleteIfExists(uploadDir.resolve(fileName));
        } catch (IOException e) {
            log.warn("프로필 이미지 파일 삭제 실패: {}", imageUrl, e);
        }
    }

    private Member findActiveMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.isDeleted() || member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return member;
    }
}
