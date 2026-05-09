package com.pilates.domain.attendance.repository;

import com.pilates.domain.attendance.entity.Attendance;
import com.pilates.domain.attendance.entity.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByReservationId(Long reservationId);

    List<Attendance> findAllByClassScheduleId(Long classScheduleId);

    @Query("SELECT a FROM Attendance a JOIN FETCH a.member JOIN FETCH a.reservation " +
            "WHERE a.classSchedule.id = :classScheduleId ORDER BY a.createdAt")
    List<Attendance> findAllByClassScheduleIdWithMember(@Param("classScheduleId") Long classScheduleId);

    Page<Attendance> findAllByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    long countByMemberIdAndStatusIn(Long memberId, List<AttendanceStatus> statuses);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.member.id = :memberId " +
            "AND a.status IN :statuses AND a.createdAt >= :from AND a.createdAt < :to")
    long countByMemberIdAndStatusInAndPeriod(@Param("memberId") Long memberId,
                                              @Param("statuses") List<AttendanceStatus> statuses,
                                              @Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.member.id = :memberId " +
            "AND a.status <> 'PENDING' AND a.createdAt >= :from AND a.createdAt < :to")
    long countResolvedByMemberIdAndPeriod(@Param("memberId") Long memberId,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.member.id = :memberId " +
            "AND a.status <> 'PENDING'")
    long countResolvedByMemberId(@Param("memberId") Long memberId);

    void deleteByReservationId(Long reservationId);

    /** 노쇼 카운트: 회원별, 기간별 */
    @Query("SELECT a.member.id, COUNT(a) FROM Attendance a " +
            "WHERE a.status = 'NO_SHOW' AND a.createdAt >= :from AND a.createdAt < :to " +
            "GROUP BY a.member.id HAVING COUNT(a) > 0 ORDER BY COUNT(a) DESC")
    List<Object[]> countNoShowByMemberAndPeriod(@Param("from") LocalDateTime from,
                                                 @Param("to") LocalDateTime to);
}
