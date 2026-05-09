package com.pilates.domain.admin.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.common.security.encryption.EncryptionService;
import com.pilates.common.security.hash.HashingService;
import com.pilates.common.util.MaskingUtil;
import com.pilates.domain.admin.dto.AdminMemberResponse;
import com.pilates.domain.admin.dto.AdminMemberResponse.*;
import com.pilates.domain.admin.entity.Admin;
import com.pilates.domain.admin.repository.AdminRepository;
import com.pilates.domain.attendance.entity.AttendanceStatus;
import com.pilates.domain.attendance.repository.AttendanceRepository;
import com.pilates.domain.member.entity.Member;
import com.pilates.domain.member.entity.MemberMemo;
import com.pilates.domain.member.entity.MemberStatus;
import com.pilates.domain.member.repository.MemberMemoRepository;
import com.pilates.domain.member.repository.MemberRepository;
import com.pilates.domain.membership.entity.Membership;
import com.pilates.domain.membership.repository.MembershipRepository;
import com.pilates.domain.payment.entity.Payment;
import com.pilates.domain.payment.repository.PaymentRepository;
import com.pilates.domain.reservation.entity.Reservation;
import com.pilates.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {

    private final MemberRepository memberRepository;
    private final MembershipRepository membershipRepository;
    private final ReservationRepository reservationRepository;
    private final AttendanceRepository attendanceRepository;
    private final PaymentRepository paymentRepository;
    private final MemberMemoRepository memberMemoRepository;
    private final AdminRepository adminRepository;
    private final EncryptionService encryptionService;
    private final HashingService hashingService;

    public Page<AdminMemberResponse> searchMembers(String search, String status, int page, int size, String sort) {
        // Sort 파싱
        String[] sortParts = sort != null ? sort.split(",") : new String[]{"createdAt", "desc"};
        String sortField = sortParts.length > 0 ? sortParts[0] : "createdAt";
        Sort.Direction direction = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortField));

        // 전체 조회 후 필터링 (운영 규모 소규모 가정)
        List<Member> allMembers = memberRepository.findAll();

        List<Member> filtered = allMembers.stream()
                .filter(m -> m.getDeletedAt() == null || "WITHDRAWN".equals(status))
                .filter(m -> {
                    if (status != null && !status.isBlank()) {
                        if ("WITHDRAWN".equals(status)) {
                            return m.getStatus() == MemberStatus.WITHDRAWN;
                        }
                        return m.getStatus().name().equals(status) && m.getDeletedAt() == null;
                    }
                    return m.getDeletedAt() == null;
                })
                .filter(m -> {
                    if (search == null || search.isBlank()) return true;
                    // 이름 검색 (복호화 후 비교)
                    String decryptedName = encryptionService.decrypt(m.getName());
                    if (decryptedName != null && decryptedName.contains(search)) return true;
                    // phone hash 검색
                    String searchHash = hashingService.hash(search);
                    return searchHash.equals(m.getPhoneHash());
                })
                .toList();

        // 정렬
        List<Member> sorted = filtered.stream()
                .sorted((a, b) -> {
                    if (direction == Sort.Direction.DESC) {
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    }
                    return a.getCreatedAt().compareTo(b.getCreatedAt());
                })
                .toList();

        // 페이징
        int start = (int) pageRequest.getOffset();
        int end = Math.min(start + pageRequest.getPageSize(), sorted.size());
        List<Member> pageContent = start < sorted.size() ? sorted.subList(start, end) : List.of();

        // 활성 정기권 일괄 조회
        List<AdminMemberResponse> responses = pageContent.stream()
                .map(m -> {
                    List<Membership> memberships = membershipRepository
                            .findAllByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(m.getId());
                    var activeMsOpt = memberships.stream()
                            .filter(ms -> ms.getStatus() == com.pilates.domain.membership.entity.MembershipStatus.ACTIVE)
                            .findFirst();
                    String activeMembership = activeMsOpt
                            .map(ms -> ms.getMembershipPass() != null ? ms.getMembershipPass().getName() : "직접 발급")
                            .orElse(null);
                    String remainingInfo = activeMsOpt
                            .map(ms -> ms.isUnlimited() ? "무제한" : ms.getRemainingCount() + "/" + ms.getTotalCount())
                            .orElse("-");
                    java.time.LocalDate expiryDate = activeMsOpt
                            .map(ms -> ms.getEndDate())
                            .orElse(null);
                    // 출석률
                    long totalResolved = attendanceRepository.countResolvedByMemberId(m.getId());
                    long attended = attendanceRepository.countByMemberIdAndStatusIn(m.getId(),
                            java.util.List.of(com.pilates.domain.attendance.entity.AttendanceStatus.ATTENDED,
                                    com.pilates.domain.attendance.entity.AttendanceStatus.LATE));
                    String attendanceRate = totalResolved > 0
                            ? Math.round((double) attended / totalResolved * 100) + "%"
                            : "-";

                    return new AdminMemberResponse(
                            m.getId(),
                            encryptionService.decrypt(m.getName()),
                            MaskingUtil.maskPhone(encryptionService.decrypt(m.getPhoneEncrypted())),
                            m.getGender().name(),
                            m.getStatus().name(),
                            activeMembership,
                            remainingInfo,
                            expiryDate,
                            attendanceRate,
                            m.getCreatedAt()
                    );
                })
                .toList();

        return new PageImpl<>(responses, pageRequest, sorted.size());
    }

    public MemberDetailResponse getMemberDetail(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        String name = encryptionService.decrypt(member.getName());
        String phone = MaskingUtil.maskPhone(encryptionService.decrypt(member.getPhoneEncrypted()));
        String birth = encryptionService.decrypt(member.getBirthEncrypted());

        // 정기권
        List<Membership> memberships = membershipRepository
                .findAllByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(memberId);
        List<MembershipInfo> membershipInfos = memberships.stream()
                .map(ms -> new MembershipInfo(
                        ms.getId(),
                        ms.getMembershipPass() != null ? ms.getMembershipPass().getName() : "직접 발급",
                        ms.getStatus().name(),
                        ms.getTotalCount(),
                        ms.getRemainingCount(),
                        ms.isUnlimited(),
                        ms.getStartDate(),
                        ms.getEndDate()
                ))
                .toList();

        // 최근 예약 30건
        List<Reservation> reservations = reservationRepository
                .findAllByMemberIdOrderByCreatedAtDesc(memberId);
        List<ReservationInfo> recentReservations = reservations.stream()
                .limit(30)
                .map(r -> new ReservationInfo(
                        r.getId(),
                        r.getClassSchedule().getClassDate(),
                        r.getClassSchedule().getStartTime() + "~" + r.getClassSchedule().getEndTime(),
                        r.getClassSchedule().getInstructor().getName(),
                        r.getClassSchedule().getLessonType().getName(),
                        r.getStatus().name()
                ))
                .toList();

        // 출석률
        long totalResolved = attendanceRepository.countResolvedByMemberId(memberId);
        long attended = attendanceRepository.countByMemberIdAndStatusIn(memberId,
                List.of(AttendanceStatus.ATTENDED, AttendanceStatus.LATE));
        double overallRate = totalResolved > 0 ? (double) attended / totalResolved * 100 : 0;

        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        long resolved90 = attendanceRepository.countResolvedByMemberIdAndPeriod(memberId, ninetyDaysAgo, LocalDateTime.now());
        long attended90 = attendanceRepository.countByMemberIdAndStatusInAndPeriod(memberId,
                List.of(AttendanceStatus.ATTENDED, AttendanceStatus.LATE), ninetyDaysAgo, LocalDateTime.now());
        double recent90DayRate = resolved90 > 0 ? (double) attended90 / resolved90 * 100 : 0;

        AttendanceRate attendanceRate = new AttendanceRate(
                Math.round(overallRate * 10) / 10.0,
                Math.round(recent90DayRate * 10) / 10.0
        );

        // 결제 이력
        List<Payment> payments = paymentRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId);
        List<PaymentInfo> paymentInfos = payments.stream()
                .map(p -> new PaymentInfo(
                        p.getId(),
                        p.getOrderId(),
                        p.getAmount().toPlainString(),
                        p.getMethod().name(),
                        p.getStatus().name(),
                        p.getPaidAt()
                ))
                .toList();

        // 노쇼 카운트
        long noShowCount = attendanceRepository.countByMemberIdAndStatusIn(memberId,
                List.of(AttendanceStatus.NO_SHOW));

        // 메모
        List<MemberMemo> memos = memberMemoRepository
                .findAllByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(memberId);
        List<MemoInfo> memoInfos = memos.stream()
                .map(memo -> {
                    String writerName;
                    if (memo.getAdmin() != null) {
                        writerName = memo.getAdmin().getName();
                    } else if (memo.getInstructor() != null) {
                        writerName = memo.getInstructor().getName();
                    } else {
                        writerName = "알 수 없음";
                    }
                    return new MemoInfo(
                            memo.getId(), memo.getContent(), writerName,
                            memo.getCreatedAt(), memo.getUpdatedAt());
                })
                .toList();

        return new MemberDetailResponse(
                member.getId(), name, phone, birth,
                member.getGender().name(), member.getStatus().name(),
                member.getProfileImageUrl(), member.getCreatedAt(),
                membershipInfos, recentReservations, attendanceRate,
                paymentInfos, (int) noShowCount, memoInfos
        );
    }

    @Transactional
    public MemoInfo saveMemberMemo(Long memberId, Long adminId, String content) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        MemberMemo memo = MemberMemo.builder()
                .member(member)
                .admin(admin)
                .content(content)
                .build();
        memberMemoRepository.save(memo);

        return new MemoInfo(memo.getId(), memo.getContent(), admin.getName(),
                memo.getCreatedAt(), memo.getUpdatedAt());
    }

    @Transactional
    public MemoInfo updateMemberMemo(Long memoId, Long adminId, String content) {
        MemberMemo memo = memberMemoRepository.findByIdAndDeletedAtIsNull(memoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_MEMO_NOT_FOUND));

        if (memo.getWriterAdminId() == null || !memo.getWriterAdminId().equals(adminId)) {
            throw new BusinessException(ErrorCode.ADMIN_MEMO_NOT_OWNER);
        }

        memo.updateContent(content);
        return new MemoInfo(memo.getId(), memo.getContent(),
                memo.getAdmin() != null ? memo.getAdmin().getName() : "알 수 없음",
                memo.getCreatedAt(), memo.getUpdatedAt());
    }

    @Transactional
    public void deleteMemberMemo(Long memoId, Long adminId) {
        MemberMemo memo = memberMemoRepository.findByIdAndDeletedAtIsNull(memoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_MEMO_NOT_FOUND));

        if (memo.getWriterAdminId() == null || !memo.getWriterAdminId().equals(adminId)) {
            throw new BusinessException(ErrorCode.ADMIN_MEMO_NOT_OWNER);
        }

        memo.softDelete();
    }

    @Transactional
    public void forceWithdrawMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.ADMIN_MEMBER_FORCE_WITHDRAW_FAILED);
        }

        member.withdraw();
    }
}
