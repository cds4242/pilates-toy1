package com.pilates.domain.classroom.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.domain.classroom.dto.FixedScheduleRequest;
import com.pilates.domain.classroom.dto.FixedScheduleResponse;
import com.pilates.domain.classroom.entity.FixedSchedule;
import com.pilates.domain.classroom.entity.LessonType;
import com.pilates.domain.classroom.repository.FixedScheduleRepository;
import com.pilates.domain.classroom.repository.LessonTypeRepository;
import com.pilates.domain.instructor.entity.Instructor;
import com.pilates.domain.instructor.entity.InstructorAvailableTime;
import com.pilates.domain.instructor.repository.InstructorAvailableTimeRepository;
import com.pilates.domain.instructor.repository.InstructorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 고정 스케줄 도메인 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FixedScheduleService {

    private final FixedScheduleRepository fixedScheduleRepository;
    private final InstructorRepository instructorRepository;
    private final InstructorAvailableTimeRepository availableTimeRepository;
    private final LessonTypeRepository lessonTypeRepository;

    /**
     * 고정 스케줄 생성.
     * 검증: 강사 활성, 수업 유형 활성, 근무 가능 시간 내, 시간 충돌 없음.
     */
    @Transactional
    public FixedScheduleResponse createFixedSchedule(FixedScheduleRequest request) {
        Instructor instructor = findActiveInstructor(request.instructorId());
        LessonType lessonType = findActiveLessonType(request.lessonTypeId());

        // 강사 근무 가능 시간 내 검증
        validateWithinAvailableTime(request);

        // 같은 강사, 같은 요일 시간 충돌 검증
        validateNoTimeConflict(request);

        FixedSchedule schedule = FixedSchedule.builder()
                .instructor(instructor)
                .lessonType(lessonType)
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .active(true)
                .build();

        fixedScheduleRepository.save(schedule);
        log.info("고정 스케줄 생성: id={}, instructor={}, dayOfWeek={}, time={}-{}",
                schedule.getId(), instructor.getName(), request.dayOfWeek(),
                request.startTime(), request.endTime());
        return toResponse(schedule);
    }

    /**
     * 고정 스케줄 수정.
     */
    @Transactional
    public FixedScheduleResponse updateFixedSchedule(Long id, FixedScheduleRequest request) {
        FixedSchedule schedule = findById(id);
        Instructor instructor = findActiveInstructor(request.instructorId());
        LessonType lessonType = findActiveLessonType(request.lessonTypeId());

        schedule.updateInfo(instructor, lessonType, request.dayOfWeek(),
                request.startTime(), request.endTime());
        log.info("고정 스케줄 수정: id={}", id);
        return toResponse(schedule);
    }

    /**
     * 고정 스케줄 비활성화.
     */
    @Transactional
    public void deactivateFixedSchedule(Long id) {
        FixedSchedule schedule = findById(id);
        schedule.deactivate();
        log.info("고정 스케줄 비활성화: id={}", id);
    }

    /**
     * 강사별 고정 스케줄 목록 조회.
     */
    @Transactional(readOnly = true)
    public List<FixedScheduleResponse> listByInstructor(Long instructorId) {
        return fixedScheduleRepository.findAllByInstructorIdAndActiveTrue(instructorId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 전체 고정 스케줄 목록 조회.
     */
    @Transactional(readOnly = true)
    public List<FixedScheduleResponse> listAll() {
        return fixedScheduleRepository.findAllByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── private ──

    private FixedSchedule findById(Long id) {
        return fixedScheduleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.FIXED_SCHEDULE_NOT_FOUND));
    }

    private Instructor findActiveInstructor(Long instructorId) {
        Instructor instructor = instructorRepository.findById(instructorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INSTRUCTOR_NOT_FOUND));
        if (!instructor.isActive()) {
            throw new BusinessException(ErrorCode.INSTRUCTOR_ALREADY_INACTIVE);
        }
        return instructor;
    }

    private LessonType findActiveLessonType(Long lessonTypeId) {
        LessonType lessonType = lessonTypeRepository.findById(lessonTypeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LESSON_TYPE_NOT_FOUND));
        if (!lessonType.isActive()) {
            throw new BusinessException(ErrorCode.LESSON_TYPE_NOT_FOUND);
        }
        return lessonType;
    }

    private void validateWithinAvailableTime(FixedScheduleRequest request) {
        List<InstructorAvailableTime> availableTimes =
                availableTimeRepository.findAllByInstructorIdAndDayOfWeek(
                        request.instructorId(), request.dayOfWeek());

        boolean withinAvailable = availableTimes.stream().anyMatch(at ->
                !request.startTime().isBefore(at.getStartTime())
                        && !request.endTime().isAfter(at.getEndTime()));

        if (!withinAvailable) {
            throw new BusinessException(ErrorCode.FIXED_SCHEDULE_OUT_OF_AVAILABLE);
        }
    }

    private void validateNoTimeConflict(FixedScheduleRequest request) {
        List<FixedSchedule> existing = fixedScheduleRepository
                .findAllByInstructorIdAndDayOfWeekAndActiveTrue(
                        request.instructorId(), request.dayOfWeek());

        boolean conflict = existing.stream().anyMatch(s ->
                request.startTime().isBefore(s.getEndTime())
                        && s.getStartTime().isBefore(request.endTime()));

        if (conflict) {
            throw new BusinessException(ErrorCode.FIXED_SCHEDULE_TIME_CONFLICT);
        }
    }

    private FixedScheduleResponse toResponse(FixedSchedule s) {
        return new FixedScheduleResponse(
                s.getId(),
                s.getInstructor().getId(),
                s.getInstructor().getName(),
                s.getLessonType().getId(),
                s.getLessonType().getName(),
                s.getDayOfWeek(),
                s.getStartTime(),
                s.getEndTime(),
                s.isActive()
        );
    }
}
