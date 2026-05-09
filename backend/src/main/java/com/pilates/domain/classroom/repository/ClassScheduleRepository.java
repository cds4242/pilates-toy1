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
}
