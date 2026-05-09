package com.pilates.domain.reservation.repository;

import com.pilates.domain.reservation.entity.Reservation;
import com.pilates.domain.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 예약 Repository.
 */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByMemberIdAndClassScheduleIdAndStatusIn(Long memberId, Long classScheduleId,
                                                          List<ReservationStatus> statuses);

    List<Reservation> findAllByClassScheduleIdAndStatusIn(Long classScheduleId,
                                                           List<ReservationStatus> statuses);

    List<Reservation> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);

    List<Reservation> findAllByClassScheduleIdInAndMemberIdAndStatusIn(List<Long> classScheduleIds,
                                                                        Long memberId,
                                                                        List<ReservationStatus> statuses);

    long countByMemberIdAndStatusInAndCreatedAtBetween(Long memberId,
                                                        List<ReservationStatus> statuses,
                                                        LocalDateTime from,
                                                        LocalDateTime to);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.classSchedule cs " +
            "WHERE cs.classDate = :date AND cs.endTime < :time " +
            "AND r.status = 'CONFIRMED' AND r.deletedAt IS NULL")
    List<Reservation> findOverdueConfirmedReservations(@Param("date") LocalDate date,
                                                        @Param("time") LocalTime time);

    /** 시간 겹침 예약 조회 (같은 회원, 같은 날짜, CONFIRMED, 시간 겹침) */
    @Query("SELECT r FROM Reservation r JOIN FETCH r.classSchedule cs " +
            "WHERE r.member.id = :memberId " +
            "AND cs.classDate = :classDate " +
            "AND r.status = 'CONFIRMED' " +
            "AND r.deletedAt IS NULL " +
            "AND cs.startTime < :endTime AND cs.endTime > :startTime")
    List<Reservation> findOverlappingReservations(@Param("memberId") Long memberId,
                                                    @Param("classDate") LocalDate classDate,
                                                    @Param("startTime") LocalTime startTime,
                                                    @Param("endTime") LocalTime endTime);
}
