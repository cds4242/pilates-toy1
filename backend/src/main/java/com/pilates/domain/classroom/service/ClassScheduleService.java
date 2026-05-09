package com.pilates.domain.classroom.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.domain.classroom.dto.ClassScheduleCreateRequest;
import com.pilates.domain.classroom.dto.ClassScheduleResponse;
import com.pilates.domain.classroom.entity.ClassSchedule;
import com.pilates.domain.classroom.entity.ClassScheduleStatus;
import com.pilates.domain.classroom.entity.LessonType;
import com.pilates.domain.classroom.repository.ClassScheduleRepository;
import com.pilates.domain.classroom.repository.LessonTypeRepository;
import com.pilates.domain.instructor.entity.Instructor;
import com.pilates.domain.instructor.repository.InstructorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 수업 시간표 도메인 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassScheduleService {

    private final ClassScheduleRepository classScheduleRepository;
    private final InstructorRepository instructorRepository;
    private final LessonTypeRepository lessonTypeRepository;

    /**
     * 수업 단건 생성 (ad-hoc).
     */
    @Transactional
    public ClassScheduleResponse createSingleClass(ClassScheduleCreateRequest request) {
        Instructor instructor = instructorRepository.findById(request.instructorId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INSTRUCTOR_NOT_FOUND));
        LessonType lessonType = lessonTypeRepository.findById(request.lessonTypeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LESSON_TYPE_NOT_FOUND));

        ClassSchedule schedule = ClassSchedule.builder()
                .instructor(instructor)
                .lessonType(lessonType)
                .classDate(request.classDate())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .maxCapacity(request.maxCapacity())
                .status(ClassScheduleStatus.SCHEDULED)
                .build();

        classScheduleRepository.save(schedule);
        log.info("수업 단건 생성: id={}, date={}, instructor={}", schedule.getId(),
                request.classDate(), instructor.getName());
        return toResponse(schedule);
    }

    /**
     * 수업 취소 (휴강).
     */
    @Transactional
    public void cancelClass(Long id) {
        ClassSchedule schedule = findById(id);

        if (schedule.getStatus() == ClassScheduleStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.CLASS_ALREADY_CANCELLED);
        }
        if (schedule.getStatus() == ClassScheduleStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.CLASS_ALREADY_COMPLETED);
        }

        schedule.cancel();
        // TODO: 해당 수업의 예약 건들 일괄 취소 + 정기권 복구 (reservation 도메인 구현 후 연결)
        log.info("수업 취소: id={}, date={}", id, schedule.getClassDate());
    }

    /**
     * 수업 완료 처리.
     */
    @Transactional
    public void completeClass(Long id) {
        ClassSchedule schedule = findById(id);

        if (schedule.getStatus() == ClassScheduleStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.CLASS_ALREADY_COMPLETED);
        }
        if (schedule.getStatus() == ClassScheduleStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.CLASS_ALREADY_CANCELLED);
        }

        schedule.complete();
        // TODO: 노쇼 처리 (attendance 도메인 구현 후 연결)
        log.info("수업 완료: id={}, date={}", id, schedule.getClassDate());
    }

    /**
     * 날짜 범위별 수업 목록 조회 (취소 제외).
     */
    @Transactional(readOnly = true)
    public List<ClassScheduleResponse> listByDateRange(LocalDate from, LocalDate to) {
        return classScheduleRepository
                .findAllByClassDateBetweenAndStatusNot(from, to, ClassScheduleStatus.CANCELLED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 강사 + 날짜 범위별 수업 목록 조회 (전체 상태).
     */
    @Transactional(readOnly = true)
    public List<ClassScheduleResponse> listByInstructorAndDateRange(Long instructorId,
                                                                     LocalDate from, LocalDate to) {
        return classScheduleRepository
                .findAllByInstructorIdAndClassDateBetween(instructorId, from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 수업 상세 조회.
     */
    @Transactional(readOnly = true)
    public ClassScheduleResponse getClassDetail(Long id) {
        return toResponse(findById(id));
    }

    // ── private ──

    private ClassSchedule findById(Long id) {
        return classScheduleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLASS_NOT_FOUND));
    }

    private ClassScheduleResponse toResponse(ClassSchedule cs) {
        return new ClassScheduleResponse(
                cs.getId(),
                cs.getClassDate(),
                cs.getStartTime(),
                cs.getEndTime(),
                cs.getInstructor().getId(),
                cs.getInstructor().getName(),
                cs.getLessonType().getId(),
                cs.getLessonType().getName(),
                cs.getMaxCapacity(),
                cs.getCurrentCount(),
                cs.getStatus().name(),
                cs.isReservable()
        );
    }
}
