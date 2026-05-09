package com.pilates.domain.instructor.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.domain.instructor.dto.*;
import com.pilates.domain.instructor.entity.Instructor;
import com.pilates.domain.instructor.entity.InstructorAvailableTime;
import com.pilates.domain.instructor.entity.InstructorStatus;
import com.pilates.domain.instructor.repository.InstructorAvailableTimeRepository;
import com.pilates.domain.instructor.repository.InstructorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 강사 도메인 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstructorService {

    private final InstructorRepository instructorRepository;
    private final InstructorAvailableTimeRepository availableTimeRepository;

    /**
     * 강사 등록.
     * UUID 기반 publicId를 자동 생성한다.
     */
    @Transactional
    public InstructorResponse registerInstructor(InstructorRegisterRequest request) {
        Instructor instructor = Instructor.builder()
                .publicId(UUID.randomUUID().toString().replace("-", "").substring(0, 32))
                .name(request.name())
                .phone(request.phone())
                .status(InstructorStatus.ACTIVE)
                .profileImageUrl(request.profileImageUrl())
                .build();

        instructorRepository.save(instructor);
        log.info("강사 등록: id={}, name={}", instructor.getId(), instructor.getName());
        return toResponse(instructor);
    }

    /**
     * 강사 정보 수정.
     * null이 아닌 필드만 업데이트한다.
     */
    @Transactional
    public InstructorResponse updateInstructor(Long id, InstructorUpdateRequest request) {
        Instructor instructor = findById(id);
        instructor.updateInfo(request.name(), request.phone(), request.profileImageUrl());
        log.info("강사 정보 수정: id={}", id);
        return toResponse(instructor);
    }

    /**
     * 강사 비활성화 (soft delete 포함).
     */
    @Transactional
    public void deactivateInstructor(Long id) {
        Instructor instructor = findById(id);
        if (!instructor.isActive()) {
            throw new BusinessException(ErrorCode.INSTRUCTOR_ALREADY_INACTIVE);
        }
        instructor.deactivate();
        instructor.softDelete();
        log.info("강사 비활성화: id={}", id);
    }

    /**
     * 강사 활성화 (soft delete 복원).
     */
    @Transactional
    public InstructorResponse activateInstructor(Long id) {
        Instructor instructor = instructorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INSTRUCTOR_NOT_FOUND));
        if (instructor.isActive()) {
            throw new BusinessException(ErrorCode.INSTRUCTOR_ALREADY_ACTIVE);
        }
        instructor.activate();
        // BaseEntity의 deletedAt을 null로 복원하기 위해 별도 처리 필요
        // 현재 BaseEntity에 restore 메서드가 없으므로 activate만 수행
        log.info("강사 활성화: id={}", id);
        return toResponse(instructor);
    }

    /**
     * 강사 상세 조회 (관리자용, ID 기반).
     */
    @Transactional(readOnly = true)
    public InstructorResponse getInstructor(Long id) {
        return toResponse(findById(id));
    }

    /**
     * 강사 상세 조회 (공개용, publicId 기반).
     */
    @Transactional(readOnly = true)
    public PublicInstructorResponse getInstructorByPublicId(String publicId) {
        Instructor instructor = instructorRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INSTRUCTOR_NOT_FOUND));
        return toPublicResponse(instructor);
    }

    /**
     * 전체 강사 목록 조회 (관리자용, 삭제되지 않은 전체).
     */
    @Transactional(readOnly = true)
    public List<InstructorResponse> listAllInstructors() {
        return instructorRepository.findAllByDeletedAtIsNull()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 활성 강사 목록 조회 (공개용).
     */
    @Transactional(readOnly = true)
    public List<PublicInstructorResponse> listActiveInstructors() {
        return instructorRepository.findAllByStatusAndDeletedAtIsNull(InstructorStatus.ACTIVE)
                .stream()
                .map(this::toPublicResponse)
                .toList();
    }

    /**
     * 강사 근무 가능 시간 설정 (기존 시간 전체 교체).
     * 시간 겹침 검증 후 저장한다.
     */
    @Transactional
    public List<AvailableTimeResponse> setAvailableTimes(Long instructorId, List<AvailableTimeRequest> requests) {
        Instructor instructor = findById(instructorId);

        // 같은 요일 내 시간 겹침 검증
        validateTimeOverlap(requests);

        // 기존 시간 삭제 후 새로 저장
        availableTimeRepository.deleteAllByInstructorId(instructorId);
        availableTimeRepository.flush();

        List<InstructorAvailableTime> times = requests.stream()
                .map(req -> InstructorAvailableTime.builder()
                        .instructor(instructor)
                        .dayOfWeek(req.dayOfWeek())
                        .startTime(req.startTime())
                        .endTime(req.endTime())
                        .build())
                .toList();

        List<InstructorAvailableTime> saved = availableTimeRepository.saveAll(times);
        log.info("강사 근무 가능 시간 설정: instructorId={}, count={}", instructorId, saved.size());
        return saved.stream().map(this::toAvailableTimeResponse).toList();
    }

    /**
     * 강사 근무 가능 시간 조회.
     */
    @Transactional(readOnly = true)
    public List<AvailableTimeResponse> getAvailableTimes(Long instructorId) {
        findById(instructorId); // 존재 검증
        return availableTimeRepository.findAllByInstructorId(instructorId)
                .stream()
                .map(this::toAvailableTimeResponse)
                .toList();
    }

    // ── private ──

    private Instructor findById(Long id) {
        Instructor instructor = instructorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INSTRUCTOR_NOT_FOUND));
        if (instructor.isDeleted()) {
            throw new BusinessException(ErrorCode.INSTRUCTOR_NOT_FOUND);
        }
        return instructor;
    }

    private void validateTimeOverlap(List<AvailableTimeRequest> requests) {
        for (int i = 0; i < requests.size(); i++) {
            for (int j = i + 1; j < requests.size(); j++) {
                AvailableTimeRequest a = requests.get(i);
                AvailableTimeRequest b = requests.get(j);
                if (a.dayOfWeek() == b.dayOfWeek()
                        && a.startTime().isBefore(b.endTime())
                        && b.startTime().isBefore(a.endTime())) {
                    throw new BusinessException(ErrorCode.INSTRUCTOR_TIME_OVERLAP);
                }
            }
        }
    }

    private InstructorResponse toResponse(Instructor instructor) {
        return new InstructorResponse(
                instructor.getId(),
                instructor.getPublicId(),
                instructor.getName(),
                instructor.getPhone(),
                instructor.getStatus().name(),
                instructor.getProfileImageUrl(),
                instructor.getCreatedAt() != null
                        ? instructor.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null
        );
    }

    private PublicInstructorResponse toPublicResponse(Instructor instructor) {
        return new PublicInstructorResponse(
                instructor.getPublicId(),
                instructor.getName(),
                instructor.getProfileImageUrl()
        );
    }

    private AvailableTimeResponse toAvailableTimeResponse(InstructorAvailableTime time) {
        return new AvailableTimeResponse(
                time.getId(),
                time.getDayOfWeek(),
                time.getStartTime(),
                time.getEndTime()
        );
    }
}
