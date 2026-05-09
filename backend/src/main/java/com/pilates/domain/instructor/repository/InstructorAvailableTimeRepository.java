package com.pilates.domain.instructor.repository;

import com.pilates.domain.instructor.entity.InstructorAvailableTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

/**
 * 강사 근무 가능 시간 리포지토리.
 */
public interface InstructorAvailableTimeRepository extends JpaRepository<InstructorAvailableTime, Long> {

    List<InstructorAvailableTime> findAllByInstructorId(Long instructorId);

    List<InstructorAvailableTime> findAllByInstructorIdAndDayOfWeek(Long instructorId, DayOfWeek dayOfWeek);

    void deleteAllByInstructorId(Long instructorId);
}
