package com.pilates.domain.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminMemberResponse(
        Long id,
        String name,
        String phone,
        String gender,
        String status,
        String activeMembership,
        LocalDateTime createdAt
) {

    public record MemberDetailResponse(
            Long id,
            String name,
            String phone,
            String birthDate,
            String gender,
            String status,
            String profileImageUrl,
            LocalDateTime createdAt,
            List<MembershipInfo> memberships,
            List<ReservationInfo> recentReservations,
            AttendanceRate attendanceRate,
            List<PaymentInfo> payments,
            int noShowCount,
            List<MemoInfo> memos
    ) {}

    public record MembershipInfo(
            Long id,
            String passName,
            String status,
            int totalCount,
            int remainingCount,
            boolean unlimited,
            LocalDate startDate,
            LocalDate endDate
    ) {}

    public record ReservationInfo(
            Long id,
            LocalDate classDate,
            String time,
            String instructor,
            String className,
            String status
    ) {}

    public record AttendanceRate(
            double overallRate,
            double recent90DayRate
    ) {}

    public record PaymentInfo(
            Long id,
            String orderId,
            String amount,
            String method,
            String status,
            LocalDateTime paidAt
    ) {}

    public record MemoInfo(
            Long id,
            String content,
            String writerName,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
