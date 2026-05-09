package com.pilates.domain.reservation.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.common.security.encryption.EncryptionService;
import com.pilates.domain.classroom.entity.ClassSchedule;
import com.pilates.domain.classroom.entity.ClassScheduleStatus;
import com.pilates.domain.classroom.entity.LessonType;
import com.pilates.domain.classroom.repository.ClassScheduleRepository;
import com.pilates.domain.member.entity.Member;
import com.pilates.domain.member.repository.MemberRepository;
import com.pilates.domain.membership.entity.Membership;
import com.pilates.domain.membership.entity.MembershipLessonType;
import com.pilates.domain.membership.entity.MembershipStatus;
import com.pilates.domain.membership.repository.MembershipLessonTypeRepository;
import com.pilates.domain.membership.repository.MembershipRepository;
import com.pilates.domain.reservation.dto.ReservationCreateRequest;
import com.pilates.domain.reservation.dto.ReservationResponse;
import com.pilates.domain.reservation.entity.Reservation;
import com.pilates.domain.reservation.entity.ReservationStatus;
import com.pilates.domain.reservation.repository.ReservationRepository;
import com.pilates.domain.attendance.entity.Attendance;
import com.pilates.domain.attendance.repository.AttendanceRepository;
import com.pilates.domain.notification.event.ReservationCancelledEvent;
import com.pilates.domain.notification.event.ReservationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 예약 도메인 서비스.
 * 동시성 제어: ClassSchedule의 낙관적 락(@Version)과 Membership의 비관적 락(findByIdForUpdate)을 함께 사용.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final MemberRepository memberRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipLessonTypeRepository membershipLessonTypeRepository;
    private final AttendanceRepository attendanceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EncryptionService encryptionService;
    private final com.pilates.domain.admin.service.StudioSettingService studioSettingService;

    /**
     * 예약 생성.
     * 1. 수업 유효성 검증 (SCHEDULED 상태 + 미래 일자)
     * 2. 중복 예약 검증
     * 3. 정원 검증
     * 4. 사용 가능 정기권 탐색 (만료 임박 순 우선)
     * 5. 정기권 차감 + 인원 증가 + 예약 저장
     */
    @Transactional
    public ReservationResponse createReservation(Long memberId, ReservationCreateRequest request) {
        // 1. 수업 검증
        ClassSchedule classSchedule = classScheduleRepository.findById(request.classScheduleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CLASS_NOT_FOUND));

        if (classSchedule.getStatus() != ClassScheduleStatus.SCHEDULED
                || classSchedule.getClassDate().isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.RESERVATION_CLASS_NOT_RESERVABLE);
        }

        // 2. 중복 예약 검증
        boolean duplicate = reservationRepository.existsByMemberIdAndClassScheduleIdAndStatusIn(
                memberId, request.classScheduleId(), List.of(ReservationStatus.CONFIRMED));
        if (duplicate) {
            throw new BusinessException(ErrorCode.RESERVATION_DUPLICATE);
        }

        // 2-1. 동일 시간대 다른 수업 겹침 검증
        if (!reservationRepository.findOverlappingReservations(
                memberId, classSchedule.getClassDate(),
                classSchedule.getStartTime(), classSchedule.getEndTime()).isEmpty()) {
            throw new BusinessException(ErrorCode.RESERVATION_TIME_OVERLAP);
        }

        // 3. 정원 검증
        long confirmedCount = reservationRepository.findAllByClassScheduleIdAndStatusIn(
                request.classScheduleId(), List.of(ReservationStatus.CONFIRMED)).size();
        if (confirmedCount >= classSchedule.getMaxCapacity()) {
            throw new BusinessException(ErrorCode.RESERVATION_CAPACITY_EXCEEDED);
        }

        // 4. 사용 가능 정기권 탐색
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        LessonType lessonType = classSchedule.getLessonType();
        LocalDate today = LocalDate.now();

        List<Membership> activeMemberships = membershipRepository
                .findAllByMemberIdAndStatusAndDeletedAtIsNull(memberId, MembershipStatus.ACTIVE);

        // 수업 유형이 매핑된 정기권만 필터 + 사용 가능 + 만료 임박 순 정렬
        Membership usableMembership = activeMemberships.stream()
                .filter(m -> m.isUsable(today))
                .filter(m -> hasLessonTypeMapping(m.getId(), lessonType.getId()))
                .filter(m -> {
                    if (!m.isUnlimited()) {
                        return m.getRemainingCount() >= lessonType.getDeductionCount();
                    }
                    return true;
                })
                .min(Comparator.comparing(Membership::getEndDate))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NO_MEMBERSHIP));

        // 무제한권: 월 한도 체크
        if (usableMembership.isUnlimited()) {
            YearMonth currentMonth = YearMonth.from(today);
            long monthlyUsage = countMonthlyUsage(memberId, currentMonth);
            int monthlyLimit = studioSettingService.getUnlimitedMonthlyLimit();
            if (monthlyUsage >= monthlyLimit) {
                throw new BusinessException(ErrorCode.RESERVATION_MONTHLY_LIMIT);
            }
        }

        // 5. 차감 + 인원 증가 + 저장
        usableMembership.deduct(lessonType.getDeductionCount());
        classSchedule.incrementCount();

        Reservation reservation = Reservation.builder()
                .member(member)
                .classSchedule(classSchedule)
                .membership(usableMembership)
                .status(ReservationStatus.CONFIRMED)
                .build();

        reservationRepository.save(reservation);

        // 출석 PENDING 자동 생성
        attendanceRepository.save(Attendance.createPending(reservation));

        log.info("예약 생성: reservationId={}, memberId={}, classScheduleId={}, membershipId={}",
                reservation.getId(), memberId, request.classScheduleId(), usableMembership.getId());

        // 알림 이벤트 발행
        eventPublisher.publishEvent(new ReservationCreatedEvent(
                reservation.getId(), memberId,
                classSchedule.getInstructor().getId(),
                request.classScheduleId(),
                encryptionService.decrypt(member.getName()),
                classSchedule.getLessonType().getName(),
                classSchedule.getInstructor().getName(),
                classSchedule.getClassDate().toString(),
                classSchedule.getStartTime().toString()));

        return toResponse(reservation, usableMembership);
    }

    /**
     * 예약 취소.
     * 본인 검증 → 상태 검증 → 취소 가능 시간 검증 → 취소 + 정기권 복구 + 인원 감소.
     */
    @Transactional
    public void cancelReservation(Long memberId, Long reservationId, String reason) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        // 본인 검증
        if (!reservation.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_OWNED);
        }

        // 상태 검증
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.RESERVATION_ALREADY_CANCELLED);
        }

        // 취소 가능 시간 검증 (studio_settings.CANCEL_DEADLINE_HOURS)
        int deadlineHours = studioSettingService.getCancelDeadlineHours();
        if (!reservation.canCancel(LocalDateTime.now(), deadlineHours)) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_CANCELLABLE);
        }

        // 취소 처리
        reservation.cancel(reason);

        // 출석 기록 삭제
        attendanceRepository.deleteByReservationId(reservationId);

        // 정기권 복구
        Membership membership = reservation.getMembership();
        LessonType lessonType = reservation.getClassSchedule().getLessonType();
        membership.restore(lessonType.getDeductionCount());

        // 인원 감소
        reservation.getClassSchedule().decrementCount();

        log.info("예약 취소: reservationId={}, memberId={}, reason={}", reservationId, memberId, reason);

        // 알림 이벤트 발행
        ClassSchedule cs = reservation.getClassSchedule();
        eventPublisher.publishEvent(new ReservationCancelledEvent(
                reservationId, memberId,
                encryptionService.decrypt(reservation.getMember().getName()),
                cs.getClassDate().toString(),
                cs.getStartTime().toString()));
    }

    /**
     * 수업 전체 예약 취소 (휴강 처리 시).
     * ClassScheduleService.cancelClass에서 호출.
     */
    @Transactional
    public void cancelAllByClassSchedule(Long classScheduleId, String reason) {
        List<Reservation> confirmedReservations = reservationRepository
                .findAllByClassScheduleIdAndStatusIn(classScheduleId, List.of(ReservationStatus.CONFIRMED));

        for (Reservation reservation : confirmedReservations) {
            reservation.cancel(reason);
            attendanceRepository.deleteByReservationId(reservation.getId());

            Membership membership = reservation.getMembership();
            LessonType lessonType = reservation.getClassSchedule().getLessonType();
            membership.restore(lessonType.getDeductionCount());

            reservation.getClassSchedule().decrementCount();
        }

        log.info("수업 전체 예약 취소: classScheduleId={}, cancelledCount={}", classScheduleId,
                confirmedReservations.size());
    }

    /**
     * 내 예약 목록 조회 (최신순).
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyReservations(Long memberId) {
        return reservationRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(r -> toResponse(r, r.getMembership()))
                .toList();
    }

    /**
     * 예약 상세 조회.
     */
    @Transactional(readOnly = true)
    public ReservationResponse getReservationDetail(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
        return toResponse(reservation, reservation.getMembership());
    }

    /**
     * 월별 사용 횟수 조회 (무제한권 월 한도 체크용).
     */
    public long countMonthlyUsage(Long memberId, YearMonth yearMonth) {
        LocalDateTime from = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime to = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        return reservationRepository.countByMemberIdAndStatusInAndCreatedAtBetween(
                memberId, List.of(ReservationStatus.CONFIRMED, ReservationStatus.NO_SHOW), from, to);
    }

    // ── private ──

    private boolean hasLessonTypeMapping(Long membershipId, Long lessonTypeId) {
        List<MembershipLessonType> mappings = membershipLessonTypeRepository.findAllByMembershipId(membershipId);
        return mappings.stream().anyMatch(mlt -> mlt.getLessonType().getId().equals(lessonTypeId));
    }

    private ReservationResponse toResponse(Reservation reservation, Membership membership) {
        ClassSchedule cs = reservation.getClassSchedule();
        return new ReservationResponse(
                reservation.getId(),
                cs.getId(),
                cs.getClassDate().toString(),
                cs.getStartTime().toString(),
                cs.getEndTime().toString(),
                cs.getLessonType().getName(),
                cs.getInstructor().getName(),
                reservation.getStatus().name(),
                membership.isUnlimited() ? null : membership.getRemainingCount(),
                reservation.getCreatedAt() != null ? reservation.getCreatedAt().toString() : null
        );
    }
}
