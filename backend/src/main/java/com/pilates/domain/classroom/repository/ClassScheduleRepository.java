package com.pilates.domain.classroom.repository;

import com.pilates.domain.classroom.entity.ClassSchedule;
import com.pilates.domain.classroom.entity.ClassScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 수업 시간표 리포지토리.
 */
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {

    List<ClassSchedule> findAllByClassDateBetweenAndStatusNot(LocalDate from, LocalDate to,
                                                              ClassScheduleStatus excludeStatus);

    List<ClassSchedule> findAllByInstructorIdAndClassDateBetween(Long instructorId, LocalDate from, LocalDate to);

    List<ClassSchedule> findAllByClassDateBetween(LocalDate from, LocalDate to);

    boolean existsByFixedScheduleIdAndClassDate(Long fixedScheduleId, LocalDate classDate);

    boolean existsByInstructorIdAndClassDateAndStartTimeAndStatusNot(Long instructorId, LocalDate classDate,
                                                                     LocalTime startTime,
                                                                     ClassScheduleStatus excludeStatus);

    /** 시간 겹침 수업 조회 (같은 강사, 같은 날짜, CANCELLED 제외) */
    @org.springframework.data.jpa.repository.Query(
            "SELECT cs FROM ClassSchedule cs " +
            "WHERE cs.instructor.id = :instructorId " +
            "AND cs.classDate = :classDate " +
            "AND cs.status <> 'CANCELLED' " +
            "AND cs.startTime < :endTime " +
            "AND cs.endTime > :startTime")
    java.util.List<ClassSchedule> findOverlappingClasses(
            @org.springframework.data.repository.query.Param("instructorId") Long instructorId,
            @org.springframework.data.repository.query.Param("classDate") LocalDate classDate,
            @org.springframework.data.repository.query.Param("startTime") LocalTime startTime,
            @org.springframework.data.repository.query.Param("endTime") LocalTime endTime);
}
