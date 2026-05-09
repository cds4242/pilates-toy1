package com.pilates.domain.membership.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.common.security.encryption.EncryptionService;
import com.pilates.domain.classroom.entity.LessonType;
import com.pilates.domain.classroom.repository.LessonTypeRepository;
import com.pilates.domain.member.entity.Member;
import com.pilates.domain.member.repository.MemberRepository;
import com.pilates.domain.membership.dto.*;
import com.pilates.domain.membership.entity.*;
import com.pilates.domain.membership.repository.MembershipHoldingRepository;
import com.pilates.domain.membership.repository.MembershipLessonTypeRepository;
import com.pilates.domain.membership.repository.MembershipPassLessonTypeRepository;
import com.pilates.domain.membership.repository.MembershipPassRepository;
import com.pilates.domain.membership.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * 정기권 도메인 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final MembershipLessonTypeRepository membershipLessonTypeRepository;
    private final MembershipHoldingRepository membershipHoldingRepository;
    private final MembershipPassRepository membershipPassRepository;
    private final MembershipPassLessonTypeRepository membershipPassLessonTypeRepository;
    private final MemberRepository memberRepository;
    private final LessonTypeRepository lessonTypeRepository;
    private final EncryptionService encryptionService;

    /**
     * 정기권 발급.
     * 회원과 수업 유형 존재를 검증한 뒤 정기권을 생성한다.
     * membershipPassId가 제공되면 정기권 종류 기반으로 발급한다.
     */
    @Transactional
    public MembershipResponse issueMembership(MembershipIssueRequest request) {
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        LocalDate today = LocalDate.now();
        String publicId = UUID.randomUUID().toString().replace("-", "");

        // 정기권 종류 기반 발급
        if (request.membershipPassId() != null) {
            MembershipPass pass = membershipPassRepository.findByIdAndDeletedAtIsNull(request.membershipPassId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_PASS_NOT_FOUND));

            Membership membership = Membership.builder()
                    .publicId(publicId)
                    .member(member)
                    .totalCount(pass.isUnlimited() ? 0 : pass.getTotalCount())
                    .remainingCount(pass.isUnlimited() ? 0 : pass.getTotalCount())
                    .unlimited(pass.isUnlimited())
                    .startDate(today)
                    .endDate(today.plusDays(pass.getValidityDays()))
                    .price(pass.getPrice())
                    .status(MembershipStatus.ACTIVE)
                    .membershipPass(pass)
                    .build();

            membershipRepository.save(membership);

            // 정기권 종류의 수업 유형 매핑을 복사
            List<MembershipPassLessonType> passMappings =
                    membershipPassLessonTypeRepository.findAllByMembershipPassId(pass.getId());
            List<LessonType> lessonTypes = passMappings.stream()
                    .map(MembershipPassLessonType::getLessonType)
                    .toList();

            List<MembershipLessonType> mappings = lessonTypes.stream()
                    .map(lt -> MembershipLessonType.builder()
                            .membership(membership)
                            .lessonType(lt)
                            .build())
                    .toList();
            membershipLessonTypeRepository.saveAll(mappings);

            log.info("정기권 발급 (상품 기반): id={}, memberId={}, passId={}, totalCount={}, validityDays={}",
                    membership.getId(), request.memberId(), pass.getId(), pass.getTotalCount(), pass.getValidityDays());

            List<String> lessonTypeNames = lessonTypes.stream()
                    .map(LessonType::getName)
                    .toList();

            return toResponse(membership, member, lessonTypeNames);
        }

        // 직접 입력 발급 (기존 방식)
        List<LessonType> lessonTypes = request.lessonTypeIds().stream()
                .map(id -> lessonTypeRepository.findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.LESSON_TYPE_NOT_FOUND)))
                .toList();

        Membership membership = Membership.builder()
                .publicId(publicId)
                .member(member)
                .totalCount(request.totalCount())
                .remainingCount(request.totalCount())
                .unlimited(request.unlimited())
                .startDate(today)
                .endDate(today.plusDays(request.validityDays()))
                .price(request.price())
                .status(MembershipStatus.ACTIVE)
                .build();

        membershipRepository.save(membership);

        // 수업 유형 매핑 생성
        List<MembershipLessonType> mappings = lessonTypes.stream()
                .map(lt -> MembershipLessonType.builder()
                        .membership(membership)
                        .lessonType(lt)
                        .build())
                .toList();
        membershipLessonTypeRepository.saveAll(mappings);

        log.info("정기권 발급: id={}, memberId={}, totalCount={}, validityDays={}",
                membership.getId(), request.memberId(), request.totalCount(), request.validityDays());

        // TODO [STEP 7 payment]: 환불 처리 연결

        List<String> lessonTypeNames = lessonTypes.stream()
                .map(LessonType::getName)
                .toList();

        return toResponse(membership, member, lessonTypeNames);
    }

    /**
     * 회원별 정기권 목록 조회.
     */
    @Transactional(readOnly = true)
    public List<MembershipResponse> listByMember(Long memberId) {
        List<Membership> memberships = membershipRepository
                .findAllByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(memberId);

        return memberships.stream()
                .map(m -> {
                    List<String> lessonTypeNames = getLessonTypeNames(m.getId());
                    return toResponse(m, m.getMember(), lessonTypeNames);
                })
                .toList();
    }

    /**
     * 정기권 상세 조회.
     */
    @Transactional(readOnly = true)
    public MembershipResponse getDetail(Long id) {
        Membership membership = findById(id);
        List<String> lessonTypeNames = getLessonTypeNames(id);
        return toResponse(membership, membership.getMember(), lessonTypeNames);
    }

    /**
     * 정기권 일시정지 시작.
     * ACTIVE 상태에서만 가능. 시작일은 오늘 이후, 종료일은 시작일 이후여야 한다.
     */
    @Transactional
    public MembershipHoldingResponse holdMembership(Long id, MembershipHoldRequest request) {
        Membership membership = findById(id);

        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_NOT_ACTIVE);
        }

        LocalDate today = LocalDate.now();
        if (request.fromDate().isBefore(today)) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_HOLDING_INVALID_PERIOD);
        }
        if (!request.toDate().isAfter(request.fromDate())) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_HOLDING_INVALID_PERIOD);
        }

        membership.startHolding();

        MembershipHolding holding = MembershipHolding.builder()
                .membership(membership)
                .holdStartDate(request.fromDate())
                .holdEndDate(request.toDate())
                .reason(request.reason())
                .build();
        membershipHoldingRepository.save(holding);

        log.info("정기권 일시정지: membershipId={}, from={}, to={}", id, request.fromDate(), request.toDate());

        return toHoldingResponse(holding);
    }

    /**
     * 정기권 일시정지 해제.
     * HOLDING 상태에서만 가능. 홀딩 시작일부터 오늘까지의 일수만큼 종료일을 연장한다.
     */
    @Transactional
    public void releaseHold(Long id) {
        Membership membership = findById(id);

        if (membership.getStatus() != MembershipStatus.HOLDING) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_NOT_HOLDING);
        }

        // 가장 최근 홀딩 이력 조회
        List<MembershipHolding> holdings = membershipHoldingRepository
                .findAllByMembershipIdOrderByCreatedAtDesc(id);

        if (holdings.isEmpty()) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_NOT_HOLDING);
        }

        MembershipHolding latestHolding = holdings.get(0);
        LocalDate today = LocalDate.now();
        int extendedDays = (int) ChronoUnit.DAYS.between(latestHolding.getHoldStartDate(), today);
        if (extendedDays < 0) {
            extendedDays = 0;
        }

        membership.endHolding(extendedDays);

        // 홀딩 이력 업데이트는 엔티티에 setter가 없으므로 새 레코드의 빌더 값을 활용
        // MembershipHolding에는 holdEndDate, extendedDays를 업데이트할 수 있는 메서드가 필요
        // 현재 엔티티에 없으므로 JPQL 또는 직접 필드 접근 — 여기서는 별도 업데이트 쿼리 대신
        // 새 holding으로 종료 기록을 남기는 방식 사용
        // → 실제로는 latestHolding의 holdEndDate와 extendedDays를 갱신해야 하므로
        //   MembershipHolding 엔티티에 updateOnRelease 메서드 추가 필요 (아래에서 직접 호출)
        latestHolding.updateOnRelease(today, extendedDays);

        log.info("정기권 일시정지 해제: membershipId={}, extendedDays={}", id, extendedDays);

        // TODO [STEP 8 reservation]: 예약 시 차감/복구 호출
    }

    /**
     * 정기권 홀딩 이력 조회.
     */
    @Transactional(readOnly = true)
    public List<MembershipHoldingResponse> getHoldings(Long id) {
        findById(id); // 존재 확인
        return membershipHoldingRepository.findAllByMembershipIdOrderByCreatedAtDesc(id)
                .stream()
                .map(this::toHoldingResponse)
                .toList();
    }

    // ── private ──

    private Membership findById(Long id) {
        return membershipRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));
    }

    private List<String> getLessonTypeNames(Long membershipId) {
        return membershipLessonTypeRepository.findAllByMembershipId(membershipId)
                .stream()
                .map(mlt -> mlt.getLessonType().getName())
                .toList();
    }

    private MembershipResponse toResponse(Membership m, Member member, List<String> lessonTypeNames) {
        String memberName = null;
        try {
            memberName = encryptionService.decrypt(member.getName());
        } catch (Exception e) {
            log.warn("회원 이름 복호화 실패: memberId={}", member.getId());
        }

        return new MembershipResponse(
                m.getId(),
                m.getPublicId(),
                member.getId(),
                memberName,
                m.getTotalCount(),
                m.getRemainingCount(),
                m.isUnlimited(),
                m.getStartDate(),
                m.getEndDate(),
                m.getPrice(),
                m.getStatus().name(),
                lessonTypeNames,
                m.isUsable(LocalDate.now())
        );
    }

    private MembershipHoldingResponse toHoldingResponse(MembershipHolding h) {
        return new MembershipHoldingResponse(
                h.getId(),
                h.getHoldStartDate(),
                h.getHoldEndDate(),
                h.getReason(),
                h.getExtendedDays(),
                h.getCreatedAt() != null ? h.getCreatedAt().toString() : null
        );
    }
}
