package com.pilates.domain.classroom.repository;

import com.pilates.domain.classroom.entity.FixedSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

/**
 * 고정 스케줄 리포지토리.
 */
public interface FixedScheduleRepository extends JpaRepository<FixedSchedule, Long> {

    List<FixedSchedule> findAllByActiveTrue();

    List<FixedSchedule> findAllByInstructorIdAndActiveTrue(Long instructorId);

    List<FixedSchedule> findAllByInstructorIdAndDayOfWeekAndActiveTrue(Long instructorId, DayOfWeek dayOfWeek);
}
