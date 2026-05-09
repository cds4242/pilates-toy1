package com.pilates.domain.member.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.common.security.encryption.EncryptionService;
import com.pilates.common.security.hash.HashingService;
import com.pilates.common.security.password.PasswordPolicy;
import com.pilates.domain.auth.service.SmsVerificationService;
import com.pilates.domain.member.dto.MemberResponse;
import com.pilates.domain.member.dto.MemberUpdateRequest;
import com.pilates.domain.member.dto.PasswordResetRequest;
import com.pilates.domain.member.entity.Member;
import com.pilates.domain.member.entity.MemberStatus;
import com.pilates.domain.member.entity.WithdrawnMemberLog;
import com.pilates.domain.member.repository.MemberRepository;
import com.pilates.domain.member.repository.WithdrawnMemberLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 회원 도메인 서비스.
 * Phase 4: 내 정보 조회/수정, 비밀번호 재설정
 * Phase 5: 회원 탈퇴
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final WithdrawnMemberLogRepository withdrawnMemberLogRepository;
    private final EncryptionService encryptionService;
    private final HashingService hashingService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final SmsVerificationService smsVerificationService;

    /**
     * 내 정보 조회.
     * 암호화된 필드를 복호화하여 반환한다.
     */
    @Transactional(readOnly = true)
    public MemberResponse getMyInfo(Long memberId) {
        Member member = findActiveMember(memberId);

        return new MemberResponse(
                member.getPublicId(),
                encryptionService.decrypt(member.getName()),
                encryptionService.decrypt(member.getPhoneEncrypted()),
                member.getGender().name(),
                member.getBirthEncrypted() != null
                        ? encryptionService.decrypt(member.getBirthEncrypted()) : null,
                member.getStatus().name(),
                member.getProfileImageUrl(),
                member.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }

    /**
     * 내 정보 수정.
     * null이 아닌 필드만 업데이트한다.
     */
    @Transactional
    public MemberResponse updateMyInfo(Long memberId, MemberUpdateRequest request) {
        Member member = findActiveMember(memberId);

        if (request.name() != null && !request.name().isBlank()) {
            member.updateName(encryptionService.encrypt(request.name()));
        }
        if (request.birthDate() != null && !request.birthDate().isBlank()) {
            member.updateBirthEncrypted(encryptionService.encrypt(request.birthDate()));
        }

        return getMyInfo(memberId);
    }

    /**
     * 비밀번호 재설정.
     * SMS 인증 완료 후, verifiedToken으로 전화번호 확인 → 해당 회원 비밀번호 변경.
     */
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        // 1. SMS 인증 토큰에서 전화번호 추출
        String phoneNumber = smsVerificationService.getVerifiedPhoneNumber(request.verifiedToken());

        // 2. 비밀번호 정책 검증
        passwordPolicy.validate(request.newPassword());

        // 3. 전화번호로 회원 조회
        String phoneHash = hashingService.hash(phoneNumber);
        Member member = memberRepository.findByPhoneHashAndDeletedAtIsNull(phoneHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 4. 비밀번호 변경
        member.changePassword(passwordEncoder.encode(request.newPassword()));

        log.info("비밀번호 재설정: memberId={}", member.getId());
    }

    /**
     * 회원 탈퇴 (Phase 5).
     * soft delete + phone_hash NULL + WithdrawnMemberLog 기록
     */
    @Transactional
    public void withdraw(Long memberId, String reason) {
        Member member = findActiveMember(memberId);

        // 1. 탈퇴 이력 기록 (개인정보 원본 보관, 30일 후 익명화)
        WithdrawnMemberLog withdrawnLog = WithdrawnMemberLog.builder()
                .memberId(member.getId())
                .phoneHashOriginal(member.getPhoneHash())
                .nameOriginal(member.getName())
                .birthEncryptedOriginal(member.getBirthEncrypted())
                .withdrawnAt(LocalDateTime.now())
                .withdrawalReason(reason)
                .build();
        withdrawnMemberLogRepository.save(withdrawnLog);

        // 2. 회원 탈퇴 처리 (status=WITHDRAWN, phone_hash=NULL, soft delete)
        member.withdraw();

        log.info("회원 탈퇴: memberId={}", memberId);
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
