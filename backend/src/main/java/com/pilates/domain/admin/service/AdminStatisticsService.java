package com.pilates.domain.admin.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.domain.admin.dto.StatisticsResponse.*;
import com.pilates.domain.attendance.entity.Attendance;
import com.pilates.domain.attendance.entity.AttendanceStatus;
import com.pilates.domain.attendance.repository.AttendanceRepository;
import com.pilates.domain.classroom.entity.ClassSchedule;
import com.pilates.domain.classroom.entity.ClassScheduleStatus;
import com.pilates.domain.classroom.repository.ClassScheduleRepository;
import com.pilates.domain.member.entity.Member;
import com.pilates.domain.member.entity.MemberStatus;
import com.pilates.domain.member.repository.MemberRepository;
import com.pilates.domain.payment.entity.Payment;
import com.pilates.domain.payment.entity.PaymentStatus;
import com.pilates.domain.payment.repository.PaymentRepository;
import com.pilates.domain.reservation.entity.Reservation;
import com.pilates.domain.reservation.entity.ReservationStatus;
import com.pilates.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatisticsService {

    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final AttendanceRepository attendanceRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final ReservationRepository reservationRepository;

    public RevenueStatistics getRevenueStatistics(LocalDate from, LocalDate to, String groupBy) {
        validateRange(from, to);

        List<Payment> payments = paymentRepository.findAllByPaidAtBetween(
                from.atStartOfDay(), to.atTime(LocalTime.MAX));

        List<Payment> completed = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED
                        || p.getStatus() == PaymentStatus.PARTIAL_REFUND
                        || p.getStatus() == PaymentStatus.REFUNDED)
                .toList();

        Map<String, BigDecimal> amountMap = new LinkedHashMap<>();
        Map<String, Long> countMap = new LinkedHashMap<>();

        for (Payment p : completed) {
            if (p.getPaidAt() == null) continue;
            String key = groupKey(p.getPaidAt().toLocalDate(), groupBy);
            BigDecimal net = p.getAmount().subtract(
                    p.getRefundAmount() != null ? p.getRefundAmount() : BigDecimal.ZERO);
            amountMap.merge(key, net, BigDecimal::add);
            countMap.merge(key, 1L, Long::sum);
        }

        BigDecimal total = amountMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RevenueStatistics.PeriodRevenue> breakdown = amountMap.entrySet().stream()
                .map(e -> new RevenueStatistics.PeriodRevenue(e.getKey(), e.getValue(), countMap.getOrDefault(e.getKey(), 0L)))
                .toList();

        return new RevenueStatistics(total, breakdown);
    }

    public MemberStatistics getMemberStatistics(LocalDate from, LocalDate to) {
        validateRange(from, to);

        List<Member> allMembers = memberRepository.findAll();

        // 가입자 (from~to 기간 생성)
        Map<String, Long> signupMap = new LinkedHashMap<>();
        Map<String, Long> withdrawMap = new LinkedHashMap<>();

        for (Member m : allMembers) {
            if (m.getCreatedAt() != null) {
                LocalDate created = m.getCreatedAt().toLocalDate();
                if (!created.isBefore(from) && !created.isAfter(to)) {
                    String key = created.format(DateTimeFormatter.ISO_LOCAL_DATE);
                    signupMap.merge(key, 1L, Long::sum);
                }
            }
            if (m.getStatus() == MemberStatus.WITHDRAWN && m.getDeletedAt() != null) {
                LocalDate withdrawn = m.getDeletedAt().toLocalDate();
                if (!withdrawn.isBefore(from) && !withdrawn.isAfter(to)) {
                    String key = withdrawn.format(DateTimeFormatter.ISO_LOCAL_DATE);
                    withdrawMap.merge(key, 1L, Long::sum);
                }
            }
        }

        long activeCount = allMembers.stream()
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE && m.getDeletedAt() == null)
                .count();

        List<MemberStatistics.PeriodCount> signups = signupMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new MemberStatistics.PeriodCount(e.getKey(), e.getValue()))
                .toList();

        List<MemberStatistics.PeriodCount> withdrawals = withdrawMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new MemberStatistics.PeriodCount(e.getKey(), e.getValue()))
                .toList();

        return new MemberStatistics(signups, withdrawals, activeCount);
    }

    public AttendanceStatistics getAttendanceStatistics(LocalDate from, LocalDate to) {
        validateRange(from, to);

        List<Attendance> attendances = attendanceRepository.findAll().stream()
                .filter(a -> a.getCreatedAt() != null
                        && !a.getCreatedAt().toLocalDate().isBefore(from)
                        && !a.getCreatedAt().toLocalDate().isAfter(to))
                .filter(a -> a.getStatus() != AttendanceStatus.PENDING)
                .toList();

        long totalResolved = attendances.size();
        long totalAttended = attendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ATTENDED || a.getStatus() == AttendanceStatus.LATE)
                .count();
        double overallRate = totalResolved > 0 ? Math.round((double) totalAttended / totalResolved * 1000) / 10.0 : 0;

        // 강사별
        Map<String, List<Attendance>> byInstructor = attendances.stream()
                .collect(Collectors.groupingBy(a -> a.getClassSchedule().getInstructor().getName()));

        List<AttendanceStatistics.InstructorAttendance> instructorStats = byInstructor.entrySet().stream()
                .map(e -> {
                    long total = e.getValue().size();
                    long att = e.getValue().stream()
                            .filter(a -> a.getStatus() == AttendanceStatus.ATTENDED || a.getStatus() == AttendanceStatus.LATE)
                            .count();
                    double rate = total > 0 ? Math.round((double) att / total * 1000) / 10.0 : 0;
                    return new AttendanceStatistics.InstructorAttendance(e.getKey(), rate, total);
                })
                .toList();

        // 수업 유형별
        Map<String, List<Attendance>> byLessonType = attendances.stream()
                .collect(Collectors.groupingBy(a -> a.getClassSchedule().getLessonType().getName()));

        List<AttendanceStatistics.LessonTypeAttendance> lessonTypeStats = byLessonType.entrySet().stream()
                .map(e -> {
                    long total = e.getValue().size();
                    long att = e.getValue().stream()
                            .filter(a -> a.getStatus() == AttendanceStatus.ATTENDED || a.getStatus() == AttendanceStatus.LATE)
                            .count();
                    double rate = total > 0 ? Math.round((double) att / total * 1000) / 10.0 : 0;
                    return new AttendanceStatistics.LessonTypeAttendance(e.getKey(), rate, total);
                })
                .toList();

        return new AttendanceStatistics(overallRate, instructorStats, lessonTypeStats);
    }

    public PopularTimesStatistics getPopularTimesStatistics(LocalDate from, LocalDate to) {
        validateRange(from, to);

        List<ClassSchedule> schedules = classScheduleRepository
                .findAllByClassDateBetweenAndStatusNot(from, to, ClassScheduleStatus.CANCELLED);

        // 시간대별
        Map<Integer, Long> hourMap = new TreeMap<>();
        for (int h = 6; h <= 22; h++) hourMap.put(h, 0L);

        Map<DayOfWeek, Long> dowMap = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) dowMap.put(d, 0L);

        for (ClassSchedule cs : schedules) {
            int hour = cs.getStartTime().getHour();
            long reserved = cs.getCurrentCount();
            hourMap.merge(hour, reserved, Long::sum);
            dowMap.merge(cs.getClassDate().getDayOfWeek(), reserved, Long::sum);
        }

        List<PopularTimesStatistics.HourCount> byHour = hourMap.entrySet().stream()
                .map(e -> new PopularTimesStatistics.HourCount(e.getKey(), e.getValue()))
                .toList();

        List<PopularTimesStatistics.DayOfWeekCount> byDow = dowMap.entrySet().stream()
                .map(e -> new PopularTimesStatistics.DayOfWeekCount(e.getKey().name(), e.getValue()))
                .toList();

        return new PopularTimesStatistics(byHour, byDow);
    }

    private String groupKey(LocalDate date, String groupBy) {
        if ("monthly".equalsIgnoreCase(groupBy)) {
            return date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        } else if ("weekly".equalsIgnoreCase(groupBy)) {
            LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return weekStart.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessException(ErrorCode.ADMIN_STATISTICS_INVALID_RANGE);
        }
    }
}
