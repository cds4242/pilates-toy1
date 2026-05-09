package com.pilates.domain.attendance.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.domain.attendance.dto.AttendanceResponse;
import com.pilates.domain.attendance.dto.BatchAttendanceRequest;
import com.pilates.domain.attendance.entity.Attendance;
import com.pilates.domain.attendance.entity.AttendanceStatus;
import com.pilates.domain.attendance.repository.AttendanceRepository;
import com.pilates.domain.classroom.entity.ClassSchedule;
import com.pilates.domain.classroom.repository.ClassScheduleRepository;
import com.pilates.common.security.encryption.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstructorAttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final EncryptionService encryptionService;
    private final com.pilates.domain.admin.service.StudioSettingService studioSettingService;

    /**
     * 단건 출석 체크.
     */
    @Transactional
    public void markAttendance(Long instructorId, Long reservationId, String statusStr) {
        AttendanceStatus targetStatus = parseInstructorStatus(statusStr);

        Attendance attendance = attendanceRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTENDANCE_NOT_FOUND));

        validateInstructorOwnership(instructorId, attendance);
        validateCheckable(attendance);

        applyStatus(attendance, targetStatus, instructorId);

        log.info("출석 체크: reservationId={}, status={}, instructorId={}", reservationId, targetStatus, instructorId);
    }

    /**
     * 일괄 출석 체크 (트랜잭션).
     */
    @Transactional
    public void markBatchAttendance(Long instructorId, Long classScheduleId,
                                     List<BatchAttendanceRequest.AttendanceItem> items) {
        ClassSchedule classSchedule = classScheduleRepository.findById(classScheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLASS_NOT_FOUND));

        if (!classSchedule.getInstructor().getId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        for (BatchAttendanceRequest.AttendanceItem item : items) {
            AttendanceStatus targetStatus = parseInstructorStatus(item.status());

            Attendance attendance = attendanceRepository.findByReservationId(item.reservationId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ATTENDANCE_NOT_FOUND));

            if (!attendance.getClassSchedule().getId().equals(classScheduleId)) {
                throw new BusinessException(ErrorCode.ATTENDANCE_NOT_FOUND);
            }

            validateCheckable(attendance);
            applyStatus(attendance, targetStatus, instructorId);
        }

        log.info("일괄 출석 체크: classScheduleId={}, count={}, instructorId={}",
                classScheduleId, items.size(), instructorId);
    }

    /**
     * 수업별 출석 현황 조회 (강사 본인 수업만).
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> listAttendanceForClass(Long instructorId, Long classScheduleId) {
        ClassSchedule classSchedule = classScheduleRepository.findById(classScheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLASS_NOT_FOUND));

        if (!classSchedule.getInstructor().getId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        return attendanceRepository.findAllByClassScheduleIdWithMember(classScheduleId).stream()
                .map(this::toResponse)
                .toList();
    }

    // ── private ──

    private AttendanceStatus parseInstructorStatus(String statusStr) {
        try {
            AttendanceStatus status = AttendanceStatus.valueOf(statusStr);
            if (status == AttendanceStatus.PENDING || status == AttendanceStatus.NO_SHOW) {
                throw new BusinessException(ErrorCode.ATTENDANCE_INVALID_STATUS);
            }
            return status;
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.ATTENDANCE_INVALID_STATUS);
        }
    }

    private void validateInstructorOwnership(Long instructorId, Attendance attendance) {
        if (!attendance.getClassSchedule().getInstructor().getId().equals(instructorId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void validateCheckable(Attendance attendance) {
        int afterEndMinutes = studioSettingService.getNoShowAutoMarkMinutes();
        if (!attendance.isCheckable(LocalDateTime.now(), afterEndMinutes)) {
            throw new BusinessException(ErrorCode.ATTENDANCE_NOT_CHECKABLE);
        }
    }

    private void applyStatus(Attendance attendance, AttendanceStatus status, Long instructorId) {
        switch (status) {
            case ATTENDED -> attendance.markAttended(instructorId);
            case LATE -> attendance.markLate(instructorId);
            case ABSENT -> attendance.markAbsent(instructorId);
            default -> throw new BusinessException(ErrorCode.ATTENDANCE_INVALID_STATUS);
        }
    }

    private AttendanceResponse toResponse(Attendance a) {
        ClassSchedule cs = a.getClassSchedule();
        return new AttendanceResponse(
                a.getId(),
                a.getReservation().getId(),
                a.getMember().getId(),
                encryptionService.decrypt(a.getMember().getName()),
                cs.getId(),
                cs.getClassDate().toString(),
                cs.getStartTime().toString(),
                cs.getEndTime().toString(),
                cs.getLessonType().getName(),
                cs.getInstructor().getName(),
                a.getStatus().name(),
                a.getCheckedAt() != null ? a.getCheckedAt().toString() : null,
                a.getCreatedAt() != null ? a.getCreatedAt().toString() : null
        );
    }
}
