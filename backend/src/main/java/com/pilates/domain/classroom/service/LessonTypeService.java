package com.pilates.domain.classroom.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.domain.classroom.dto.LessonTypeRequest;
import com.pilates.domain.classroom.dto.LessonTypeResponse;
import com.pilates.domain.classroom.entity.LessonType;
import com.pilates.domain.classroom.repository.LessonTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 수업 유형 도메인 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LessonTypeService {

    private final LessonTypeRepository lessonTypeRepository;

    /**
     * 수업 유형 생성.
     * 같은 이름의 활성 수업 유형이 있으면 중복 에러.
     */
    @Transactional
    public LessonTypeResponse createLessonType(LessonTypeRequest request) {
        if (lessonTypeRepository.existsByNameAndActiveTrue(request.name())) {
            throw new BusinessException(ErrorCode.LESSON_TYPE_DUPLICATE_NAME);
        }

        LessonType lessonType = LessonType.builder()
                .name(request.name())
                .maxCapacity(request.maxCapacity())
                .durationMinutes(request.durationMinutes())
                .deductionCount(request.deductionCount())
                .active(true)
                .build();

        lessonTypeRepository.save(lessonType);
        log.info("수업 유형 생성: id={}, name={}", lessonType.getId(), lessonType.getName());
        return toResponse(lessonType);
    }

    /**
     * 수업 유형 수정.
     */
    @Transactional
    public LessonTypeResponse updateLessonType(Long id, LessonTypeRequest request) {
        LessonType lessonType = findById(id);
        lessonType.updateInfo(request.name(), request.maxCapacity(),
                request.durationMinutes(), request.deductionCount());
        log.info("수업 유형 수정: id={}", id);
        return toResponse(lessonType);
    }

    /**
     * 수업 유형 비활성화.
     */
    @Transactional
    public void deactivateLessonType(Long id) {
        LessonType lessonType = findById(id);
        lessonType.deactivate();
        log.info("수업 유형 비활성화: id={}", id);
    }

    /**
     * 전체 수업 유형 목록 조회 (관리자용).
     */
    @Transactional(readOnly = true)
    public List<LessonTypeResponse> listAllLessonTypes() {
        return lessonTypeRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 활성 수업 유형 목록 조회 (공개용).
     */
    @Transactional(readOnly = true)
    public List<LessonTypeResponse> listActiveLessonTypes() {
        return lessonTypeRepository.findAllByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── private ──

    private LessonType findById(Long id) {
        return lessonTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.LESSON_TYPE_NOT_FOUND));
    }

    private LessonTypeResponse toResponse(LessonType lt) {
        return new LessonTypeResponse(
                lt.getId(),
                lt.getName(),
                lt.getMaxCapacity(),
                lt.getDurationMinutes(),
                lt.getDeductionCount(),
                lt.isActive()
        );
    }
}
