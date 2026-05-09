package com.pilates.domain.classroom.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.common.security.encryption.EncryptionService;
import com.pilates.domain.classroom.dto.ClassScheduleCreateRequest;
import com.pilates.domain.classroom.dto.ClassScheduleDetailResponse;
import com.pilates.domain.classroom.dto.ClassScheduleResponse;
import com.pilates.domain.classroom.entity.ClassSchedule;
import com.pilates.domain.classroom.entity.ClassScheduleStatus;
import com.pilates.domain.classroom.entity.LessonType;
import com.pilates.domain.classroom.repository.ClassScheduleRepository;
import com.pilates.domain.classroom.repository.LessonTypeRepository;
import com.pilates.domain.instructor.entity.Instructor;
import com.pilates.domain.instructor.repository.InstructorRepository;
import com.pilates.domain.reservation.dto.ReservedMemberInfo;
import com.pilates.domain.reservation.entity.Reservation;
import com.pilates.domain.reservation.entity.ReservationStatus;
import com.pilates.domain.reservation.repository.ReservationRepository;
import com.pilates.domain.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 수업 시간표 도메인 서비스.
 */
@Slf4j
@Service
public class ClassScheduleService {

    private final ClassScheduleRepository classScheduleRepository;
    private final InstructorRepository instructorRepository;
    private final LessonTypeRepository lessonTypeRepository;
    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;
    private final EncryptionService encryptionService;

    public ClassScheduleService(ClassScheduleRepository classScheduleRepository,
                                 InstructorRepository instructorRepository,
                                 LessonTypeRepository lessonTypeRepository,
                                 @Lazy ReservationService reservationService,
                                 ReservationRepository reservationRepository,
                                 EncryptionService encryptionService) {
        this.classScheduleRepository = classScheduleRepository;
        this.instructorRepository = instructorRepository;
        this.lessonTypeRepository = lessonTypeRepository;
        this.reservationService = reservationService;
        this.reservationRepository = reservationRepository;
        this.encryptionService = encryptionService;
    }

    /**
     * 수업 단건 생성 (ad-hoc).
     */
    @Transactional
    public ClassScheduleResponse createSingleClass(ClassScheduleCreateRequest request) {
        Instructor instructor = instructorRepository.findById(request.instructorId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INSTRUCTOR_NOT_FOUND));
        LessonType lessonType = lessonTypeRepository.findById(request.lessonTypeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LESSON_TYPE_NOT_FOUND));

        // 시간 충돌 검증 (같은 강사, 같은 날짜, CANCELLED 제외)
        List<ClassSchedule> overlapping = classScheduleRepository.findOverlappingClasses(
                request.instructorId(), request.classDate(), request.startTime(), request.endTime());
        if (!overlapping.isEmpty()) {
            throw new BusinessException(ErrorCode.CLASS_TIME_CONFLICT);
        }

        ClassSchedule schedule = ClassSchedule.builder()
                .instructor(instructor)
                .lessonType(lessonType)
                .classDate(request.classDate())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .maxCapacity(request.maxCapacity())
                .status(ClassScheduleStatus.SCHEDULED)
                .build();

        classScheduleRepository.save(schedule);
        log.info("수업 단건 생성: id={}, date={}, instructor={}", schedule.getId(),
                request.classDate(), instructor.getName());
        return toResponse(schedule);
    }

    /**
     * 수업 취소 (휴강).
     */
    @Transactional
    public void cancelClass(Long id) {
        ClassSchedule schedule = findById(id);

        if (schedule.getStatus() == ClassScheduleStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.CLASS_ALREADY_CANCELLED);
        }
        if (schedule.getStatus() == ClassScheduleStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.CLASS_ALREADY_COMPLETED);
        }

        schedule.cancel();
        reservationService.cancelAllByClassSchedule(id, "강사 휴강");
        log.info("수업 취소: id={}, date={}", id, schedule.getClassDate());
    }

    /**
     * 수업 완료 처리.
     */
    @Transactional
    public void completeClass(Long id) {
        ClassSchedule schedule = findById(id);

        if (schedule.getStatus() == ClassScheduleStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.CLASS_ALREADY_COMPLETED);
        }
        if (schedule.getStatus() == ClassScheduleStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.CLASS_ALREADY_CANCELLED);
        }

        schedule.complete();
        // TODO [STEP 8 reservation]: 수업 완료 시 미출석 예약을 NO_SHOW로 자동 전환 + 정기권 차감
        log.info("수업 완료: id={}, date={}", id, schedule.getClassDate());
    }

    /**
     * 날짜 범위별 수업 목록 조회 (취소 제외).
     */
    @Transactional(readOnly = true)
    public List<ClassScheduleResponse> listByDateRange(LocalDate from, LocalDate to) {
        return classScheduleRepository
                .findAllByClassDateBetweenAndStatusNot(from, to, ClassScheduleStatus.CANCELLED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 날짜 범위별 수업 목록 조회 + 본인 예약 상태 포함 (회원용).
     */
    @Transactional(readOnly = true)
    public List<ClassScheduleResponse> listByDateRangeWithMyStatus(LocalDate from, LocalDate to, Long memberId) {
        List<ClassSchedule> schedules = classScheduleRepository
                .findAllByClassDateBetweenAndStatusNot(from, to, ClassScheduleStatus.CANCELLED);

        if (schedules.isEmpty()) {
            return List.of();
        }

        // 배치 쿼리: 해당 수업들에 대한 회원의 예약 조회
        List<Long> scheduleIds = schedules.stream().map(ClassSchedule::getId).toList();
        Set<Long> reservedScheduleIds = reservationRepository
                .findAllByClassScheduleIdInAndMemberIdAndStatusIn(
                        scheduleIds, memberId, List.of(ReservationStatus.CONFIRMED))
                .stream()
                .map(r -> r.getClassSchedule().getId())
                .collect(Collectors.toSet());

        return schedules.stream()
                .map(cs -> toResponseWithMyStatus(cs, reservedScheduleIds))
                .toList();
    }

    /**
     * 강사 + 날짜 범위별 수업 목록 조회 (전체 상태).
     */
    @Transactional(readOnly = true)
    public List<ClassScheduleResponse> listByInstructorAndDateRange(Long instructorId,
                                                                     LocalDate from, LocalDate to) {
        return classScheduleRepository
                .findAllByInstructorIdAndClassDateBetween(instructorId, from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 수업 상세 조회.
     */
    @Transactional(readOnly = true)
    public ClassScheduleResponse getClassDetail(Long id) {
        return toResponse(findById(id));
    }

    /**
     * 강사 본인 수업 상세 조회. 본인 담당이 아니면 예외.
     * 예약자 리스트(CONFIRMED, NO_SHOW)를 포함하여 반환한다.
     */
    @Transactional(readOnly = true)
    public ClassScheduleDetailResponse getDetailForInstructor(Long instructorId, Long classScheduleId) {
        ClassSchedule cs = findById(classScheduleId);
        if (!cs.getInstructor().getId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.CLASS_NOT_FOUND);
        }

        List<Reservation> reservations = reservationRepository.findAllByClassScheduleIdAndStatusIn(
                classScheduleId, List.of(ReservationStatus.CONFIRMED, ReservationStatus.NO_SHOW));

        List<ReservedMemberInfo> reservedMembers = reservations.stream()
                .map(r -> new ReservedMemberInfo(
                        r.getMember().getId(),
                        encryptionService.decrypt(r.getMember().getName()),
                        r.getMember().getProfileImageUrl(),
                        r.getStatus().name()
                ))
                .toList();

        return toDetailResponse(cs, reservedMembers);
    }

    // ── private ──

    private ClassSchedule findById(Long id) {
        return classScheduleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLASS_NOT_FOUND));
    }

    private ClassScheduleResponse toResponse(ClassSchedule cs) {
        return new ClassScheduleResponse(
                cs.getId(),
                cs.getClassDate(),
                cs.getStartTime(),
                cs.getEndTime(),
                cs.getInstructor().getId(),
                cs.getInstructor().getName(),
                cs.getLessonType().getId(),
                cs.getLessonType().getName(),
                cs.getMaxCapacity(),
                cs.getCurrentCount(),
                cs.getStatus().name(),
                cs.isReservable(),
                null
        );
    }

    private ClassScheduleDetailResponse toDetailResponse(ClassSchedule cs, List<ReservedMemberInfo> reservations) {
        return new ClassScheduleDetailResponse(
                cs.getId(),
                cs.getClassDate(),
                cs.getStartTime(),
                cs.getEndTime(),
                cs.getInstructor().getId(),
                cs.getInstructor().getName(),
                cs.getLessonType().getId(),
                cs.getLessonType().getName(),
                cs.getMaxCapacity(),
                cs.getCurrentCount(),
                cs.getStatus().name(),
                cs.isReservable(),
                reservations
        );
    }

    private ClassScheduleResponse toResponseWithMyStatus(ClassSchedule cs, Set<Long> reservedScheduleIds) {
        String myStatus;
        if (reservedScheduleIds.contains(cs.getId())) {
            myStatus = "RESERVED";
        } else if (cs.getCurrentCount() >= cs.getMaxCapacity()) {
            myStatus = "FULL";
        } else {
            myStatus = "NOT_RESERVED";
        }

        return new ClassScheduleResponse(
                cs.getId(),
                cs.getClassDate(),
                cs.getStartTime(),
                cs.getEndTime(),
                cs.getInstructor().getId(),
                cs.getInstructor().getName(),
                cs.getLessonType().getId(),
                cs.getLessonType().getName(),
                cs.getMaxCapacity(),
                cs.getCurrentCount(),
                cs.getStatus().name(),
                cs.isReservable(),
                myStatus
        );
    }
}
