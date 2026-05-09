package com.pilates.domain.admin.dto;

import java.math.BigDecimal;
import java.util.List;

public final class StatisticsResponse {

    private StatisticsResponse() {}

    public record RevenueStatistics(
            BigDecimal total,
            List<PeriodRevenue> breakdown
    ) {
        public record PeriodRevenue(
                String period,
                BigDecimal amount,
                long count
        ) {}
    }

    public record MemberStatistics(
            List<PeriodCount> signups,
            List<PeriodCount> withdrawals,
            long activeCount
    ) {
        public record PeriodCount(
                String period,
                long count
        ) {}
    }

    public record AttendanceStatistics(
            double overallRate,
            List<InstructorAttendance> byInstructor,
            List<LessonTypeAttendance> byLessonType
    ) {
        public record InstructorAttendance(
                String instructor,
                double rate,
                long totalClasses
        ) {}

        public record LessonTypeAttendance(
                String lessonType,
                double rate,
                long totalClasses
        ) {}
    }

    public record PopularTimesStatistics(
            List<HourCount> byHour,
            List<DayOfWeekCount> byDayOfWeek
    ) {
        public record HourCount(
                int hour,
                long count
        ) {}

        public record DayOfWeekCount(
                String dayOfWeek,
                long count
        ) {}
    }
}
