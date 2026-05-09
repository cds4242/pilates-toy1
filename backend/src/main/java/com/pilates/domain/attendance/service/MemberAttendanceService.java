package com.pilates.domain.attendance.service;

import com.pilates.domain.attendance.dto.AttendanceRateResponse;
import com.pilates.domain.attendance.dto.AttendanceResponse;
import com.pilates.domain.attendance.entity.Attendance;
import com.pilates.domain.attendance.entity.AttendanceStatus;
import com.pilates.domain.attendance.repository.AttendanceRepository;
import com.pilates.domain.classroom.entity.ClassSchedule;
import com.pilates.common.security.encryption.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberAttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EncryptionService encryptionService;

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> listMyAttendance(Long memberId, int page, int size) {
        return attendanceRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AttendanceRateResponse getMyAttendanceRate(Long memberId, String period) {
        LocalDateTime from;
        LocalDateTime to = LocalDateTime.now();

        switch (period) {
            case "30d" -> from = to.minusDays(30);
            case "90d" -> from = to.minusDays(90);
            default -> from = null; // all
        }

        long attended, absent, noShow, late, total;

        if (from != null) {
            attended = attendanceRepository.countByMemberIdAndStatusInAndPeriod(
                    memberId, List.of(AttendanceStatus.ATTENDED), from, to);
            absent = attendanceRepository.countByMemberIdAndStatusInAndPeriod(
                    memberId, List.of(AttendanceStatus.ABSENT), from, to);
            noShow = attendanceRepository.countByMemberIdAndStatusInAndPeriod(
                    memberId, List.of(AttendanceStatus.NO_SHOW), from, to);
            late = attendanceRepository.countByMemberIdAndStatusInAndPeriod(
                    memberId, List.of(AttendanceStatus.LATE), from, to);
            total = attendanceRepository.countResolvedByMemberIdAndPeriod(memberId, from, to);
        } else {
            attended = attendanceRepository.countByMemberIdAndStatusIn(
                    memberId, List.of(AttendanceStatus.ATTENDED));
            absent = attendanceRepository.countByMemberIdAndStatusIn(
                    memberId, List.of(AttendanceStatus.ABSENT));
            noShow = attendanceRepository.countByMemberIdAndStatusIn(
                    memberId, List.of(AttendanceStatus.NO_SHOW));
            late = attendanceRepository.countByMemberIdAndStatusIn(
                    memberId, List.of(AttendanceStatus.LATE));
            total = attendanceRepository.countResolvedByMemberId(memberId);
        }

        double rate = total > 0 ? Math.round((double) attended / total * 1000.0) / 10.0 : 0.0;

        return new AttendanceRateResponse(attended, absent, noShow, late, total, rate, period);
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
