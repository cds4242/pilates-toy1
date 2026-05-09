package com.pilates.domain.admin.service;

import com.pilates.common.security.encryption.EncryptionService;
import com.pilates.domain.admin.dto.DashboardResponse;
import com.pilates.domain.admin.dto.DashboardResponse.*;
import com.pilates.domain.attendance.repository.AttendanceRepository;
import com.pilates.domain.classroom.entity.ClassSchedule;
import com.pilates.domain.classroom.entity.ClassScheduleStatus;
import com.pilates.domain.classroom.repository.ClassScheduleRepository;
import com.pilates.domain.member.entity.Member;
import com.pilates.domain.member.repository.MemberRepository;
import com.pilates.domain.membership.entity.Membership;
import com.pilates.domain.membership.entity.MembershipStatus;
import com.pilates.domain.membership.repository.MembershipRepository;
import com.pilates.domain.payment.entity.Payment;
import com.pilates.domain.payment.entity.PaymentStatus;
import com.pilates.domain.payment.repository.PaymentRepository;
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
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final ClassScheduleRepository classScheduleRepository;
    private final PaymentRepository paymentRepository;
    private final MembershipRepository membershipRepository;
    private final MemberRepository memberRepository;
    private final AttendanceRepository attendanceRepository;
    private final ReservationRepository reservationRepository;
    private final EncryptionService encryptionService;

    public DashboardResponse getDashboard() {
        return new DashboardResponse(
                getTodayClasses(),
                getThisWeekRevenue(),
                getExpiringMemberships(),
                getAlerts()
        );
    }

    private TodayClasses getTodayClasses() {
        LocalDate today = LocalDate.now();
        List<ClassSchedule> schedules = classScheduleRepository
                .findAllByClassDateBetweenAndStatusNot(today, today, ClassScheduleStatus.CANCELLED);

        List<ScheduleItem> items = schedules.stream()
                .map(cs -> new ScheduleItem(
                        cs.getStartTime(),
                        cs.getInstructor().getName(),
                        cs.getLessonType().getName(),
                        cs.getCurrentCount(),
                        cs.getMaxCapacity()
                ))
                .sorted(Comparator.comparing(ScheduleItem::time))
                .toList();

        return new TodayClasses(items.size(), items);
    }

    private ThisWeekRevenue getThisWeekRevenue() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<Payment> payments = paymentRepository.findAllByPaidAtBetween(
                weekStart.atStartOfDay(),
                weekEnd.atTime(LocalTime.MAX)
        );

        // COMPLETED 상태만
        List<Payment> completed = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED
                        || p.getStatus() == PaymentStatus.PARTIAL_REFUND
                        || p.getStatus() == PaymentStatus.REFUNDED)
                .toList();

        Map<LocalDate, BigDecimal> dailyMap = new LinkedHashMap<>();
        for (LocalDate d = weekStart; !d.isAfter(weekEnd); d = d.plusDays(1)) {
            dailyMap.put(d, BigDecimal.ZERO);
        }

        for (Payment p : completed) {
            if (p.getPaidAt() != null) {
                LocalDate day = p.getPaidAt().toLocalDate();
                BigDecimal netAmount = p.getAmount().subtract(
                        p.getRefundAmount() != null ? p.getRefundAmount() : BigDecimal.ZERO);
                dailyMap.merge(day, netAmount, BigDecimal::add);
            }
        }

        BigDecimal total = dailyMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<DailyRevenue> breakdown = dailyMap.entrySet().stream()
                .map(e -> new DailyRevenue(e.getKey(), e.getValue()))
                .toList();

        return new ThisWeekRevenue(total, breakdown);
    }

    private List<ExpiringMembership> getExpiringMemberships() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysLater = today.plusDays(7);

        // end_date가 오늘~7일 이내인 ACTIVE 정기권
        List<Membership> expiring = new ArrayList<>();
        for (LocalDate d = today; !d.isAfter(sevenDaysLater); d = d.plusDays(1)) {
            expiring.addAll(membershipRepository.findAllByEndDateAndStatusAndDeletedAtIsNull(
                    d, MembershipStatus.ACTIVE));
        }

        // member 일괄 조회 (N+1 방지)
        Set<Long> memberIds = expiring.stream()
                .map(m -> m.getMember().getId())
                .collect(Collectors.toSet());
        Map<Long, Member> memberMap = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        return expiring.stream()
                .map(ms -> {
                    Member member = memberMap.get(ms.getMember().getId());
                    String memberName = member != null ? encryptionService.decrypt(member.getName()) : "알 수 없음";
                    String passName = ms.getMembershipPass() != null ? ms.getMembershipPass().getName() : "직접 발급";
                    int daysLeft = (int) (ms.getEndDate().toEpochDay() - today.toEpochDay());
                    return new ExpiringMembership(
                            ms.getMember().getId(), memberName, passName, daysLeft, ms.getEndDate());
                })
                .sorted(Comparator.comparingInt(ExpiringMembership::daysLeft))
                .toList();
    }

    private Alerts getAlerts() {
        return new Alerts(getNoShowAlerts(), getLowMembershipAlerts());
    }

    private List<NoShowMember> getNoShowAlerts() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime now = LocalDateTime.now();

        List<Object[]> noShows = attendanceRepository.countNoShowByMemberAndPeriod(thirtyDaysAgo, now);

        // 3회 이상만
        List<Object[]> filtered = noShows.stream()
                .filter(row -> ((Long) row[1]) >= 3)
                .toList();

        if (filtered.isEmpty()) return List.of();

        Set<Long> memberIds = filtered.stream()
                .map(row -> (Long) row[0])
                .collect(Collectors.toSet());
        Map<Long, Member> memberMap = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        return filtered.stream()
                .map(row -> {
                    Long memberId = (Long) row[0];
                    int count = ((Long) row[1]).intValue();
                    Member member = memberMap.get(memberId);
                    String name = member != null ? encryptionService.decrypt(member.getName()) : "알 수 없음";
                    return new NoShowMember(memberId, name, count);
                })
                .toList();
    }

    private List<LowMembershipMember> getLowMembershipAlerts() {
        // 잔여 1회 이하 활성 정기권 (무제한 제외)
        // MembershipRepository에 메서드가 없으므로 findAll + filter
        // 운영 규모가 작으므로 허용. 대규모시 커스텀 쿼리 필요.
        List<Membership> all = membershipRepository.findAll();
        List<Membership> low = all.stream()
                .filter(m -> m.getDeletedAt() == null)
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .filter(m -> !m.isUnlimited())
                .filter(m -> m.getRemainingCount() <= 1)
                .toList();

        if (low.isEmpty()) return List.of();

        Set<Long> memberIds = low.stream()
                .map(m -> m.getMember().getId())
                .collect(Collectors.toSet());
        Map<Long, Member> memberMap = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, m -> m));

        return low.stream()
                .map(ms -> {
                    Member member = memberMap.get(ms.getMember().getId());
                    String name = member != null ? encryptionService.decrypt(member.getName()) : "알 수 없음";
                    String passName = ms.getMembershipPass() != null ? ms.getMembershipPass().getName() : "직접 발급";
                    return new LowMembershipMember(
                            ms.getMember().getId(), name, passName, ms.getRemainingCount());
                })
                .toList();
    }
}
