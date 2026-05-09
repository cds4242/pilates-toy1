package com.pilates.domain.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record DashboardResponse(
        TodayClasses todayClasses,
        ThisWeekRevenue thisWeekRevenue,
        List<ExpiringMembership> expiringMemberships,
        Alerts alerts
) {

    public record TodayClasses(
            int count,
            List<ScheduleItem> schedules
    ) {}

    public record ScheduleItem(
            LocalTime time,
            String instructor,
            String className,
            int reservedCount,
            int capacity
    ) {}

    public record ThisWeekRevenue(
            BigDecimal total,
            List<DailyRevenue> breakdown
    ) {}

    public record DailyRevenue(
            LocalDate date,
            BigDecimal amount
    ) {}

    public record ExpiringMembership(
            Long memberId,
            String memberName,
            String passName,
            int daysLeft,
            LocalDate endDate
    ) {}

    public record Alerts(
            List<NoShowMember> noShowMembers,
            List<LowMembershipMember> lowMembershipMembers
    ) {}

    public record NoShowMember(
            Long memberId,
            String memberName,
            int noShowCount
    ) {}

    public record LowMembershipMember(
            Long memberId,
            String memberName,
            String passName,
            int remainingCount
    ) {}
}
