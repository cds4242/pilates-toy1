package com.pilates.domain.membership.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.domain.classroom.entity.LessonType;
import com.pilates.domain.classroom.repository.LessonTypeRepository;
import com.pilates.domain.membership.dto.*;
import com.pilates.domain.membership.entity.MembershipPass;
import com.pilates.domain.membership.entity.MembershipPassLessonType;
import com.pilates.domain.membership.repository.MembershipPassLessonTypeRepository;
import com.pilates.domain.membership.repository.MembershipPassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 정기권 종류(상품 카탈로그) 도메인 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipPassService {

    private final MembershipPassRepository membershipPassRepository;
    private final MembershipPassLessonTypeRepository membershipPassLessonTypeRepository;
    private final LessonTypeRepository lessonTypeRepository;

    /**
     * 정기권 종류 생성.
     */
    @Transactional
    public MembershipPassResponse createMembershipPass(MembershipPassCreateRequest request) {
        // 중복 이름 검사
        if (membershipPassRepository.existsByNameAndDeletedAtIsNull(request.name())) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_PASS_DUPLICATE_NAME);
        }

        // 수업 유형 조회
        List<LessonType> lessonTypes = findLessonTypes(request.lessonTypeIds());

        String publicId = UUID.randomUUID().toString().replace("-", "");

        MembershipPass pass = MembershipPass.builder()
                .publicId(publicId)
                .name(request.name())
                .price(request.price())
                .totalCount(request.totalCount())
                .validityDays(request.validityDays())
                .unlimited(request.unlimited())
                .monthlyLimit(request.monthlyLimit())
                .displayOrder(request.displayOrder())
                .build();

        // 설정 유효성 검증
        pass.validate();

        // 확장 필드 설정
        java.time.LocalDate startDate = request.saleStartDate() != null ? java.time.LocalDate.parse(request.saleStartDate()) : null;
        java.time.LocalDate endDate = request.saleEndDate() != null ? java.time.LocalDate.parse(request.saleEndDate()) : null;
        pass.updateExtendedInfo(
                request.visible() != null ? request.visible() : true,
                true, startDate, endDate,
                request.category(), request.description());

        membershipPassRepository.save(pass);

        // 수업 유형 매핑 생성
        saveMappings(pass, lessonTypes);

        log.info("정기권 종류 생성: id={}, name={}", pass.getId(), pass.getName());

        return toResponse(pass, lessonTypes);
    }

    /**
     * 정기권 종류 정보 수정.
     */
    @Transactional
    public MembershipPassResponse updateMembershipPass(Long id, MembershipPassUpdateRequest request) {
        MembershipPass pass = findById(id);

        pass.updateInfo(request.name(), request.price(), request.totalCount(),
                request.validityDays(), request.displayOrder());

        // 확장 필드 업데이트
        java.time.LocalDate startDate = request.saleStartDate() != null ? java.time.LocalDate.parse(request.saleStartDate()) : null;
        java.time.LocalDate endDate = request.saleEndDate() != null ? java.time.LocalDate.parse(request.saleEndDate()) : null;
        pass.updateExtendedInfo(request.visible(), request.active(), startDate, endDate,
                request.category(), request.description());

        log.info("정기권 종류 수정: id={}", id);

        List<MembershipPassLessonType> mappings = membershipPassLessonTypeRepository.findAllByMembershipPassId(id);
        List<LessonType> lessonTypes = mappings.stream()
                .map(MembershipPassLessonType::getLessonType)
                .toList();

        return toResponse(pass, lessonTypes);
    }

    /**
     * 정기권 종류-수업 유형 매핑 변경.
     */
    @Transactional
    public void updateLessonTypeMappings(Long id, LessonTypeMappingRequest request) {
        MembershipPass pass = findById(id);

        List<LessonType> lessonTypes = findLessonTypes(request.lessonTypeIds());

        // 기존 매핑 삭제
        membershipPassLessonTypeRepository.deleteAllByMembershipPassId(id);

        // 새 매핑 생성
        saveMappings(pass, lessonTypes);

        log.info("정기권 종류 수업 유형 매핑 변경: passId={}, lessonTypeIds={}", id, request.lessonTypeIds());
    }

    /**
     * 정기권 종류 비활성화 (논리 삭제).
     */
    @Transactional
    public void deactivate(Long id) {
        MembershipPass pass = findById(id);
        pass.softDelete();
        log.info("정기권 종류 비활성화: id={}", id);
    }

    /**
     * 활성 정기권 종류 목록 조회.
     */
    @Transactional(readOnly = true)
    public List<MembershipPassResponse> listActive() {
        List<MembershipPass> passes = membershipPassRepository.findAllByDeletedAtIsNullOrderByDisplayOrderAsc();

        return passes.stream()
                .map(pass -> {
                    List<MembershipPassLessonType> mappings =
                            membershipPassLessonTypeRepository.findAllByMembershipPassId(pass.getId());
                    List<LessonType> lessonTypes = mappings.stream()
                            .map(MembershipPassLessonType::getLessonType)
                            .toList();
                    return toResponse(pass, lessonTypes);
                })
                .toList();
    }

    /**
     * 정기권 종류 상세 조회.
     */
    @Transactional(readOnly = true)
    public MembershipPassResponse getDetail(Long id) {
        MembershipPass pass = findById(id);

        List<MembershipPassLessonType> mappings =
                membershipPassLessonTypeRepository.findAllByMembershipPassId(id);
        List<LessonType> lessonTypes = mappings.stream()
                .map(MembershipPassLessonType::getLessonType)
                .toList();

        return toResponse(pass, lessonTypes);
    }

    /**
     * 공개 목록 조회 (PublicMembershipPassResponse).
     */
    @Transactional(readOnly = true)
    public List<PublicMembershipPassResponse> listPublic() {
        List<MembershipPass> passes = membershipPassRepository.findAllByDeletedAtIsNullOrderByDisplayOrderAsc();
        java.time.LocalDate today = java.time.LocalDate.now();

        return passes.stream()
                .filter(pass -> pass.isSellable(today))
                .map(pass -> {
                    List<MembershipPassLessonType> mappings =
                            membershipPassLessonTypeRepository.findAllByMembershipPassId(pass.getId());
                    List<String> lessonTypeNames = mappings.stream()
                            .map(m -> m.getLessonType().getName())
                            .toList();
                    return toPublicResponse(pass, lessonTypeNames);
                })
                .toList();
    }

    /**
     * 공개 상세 조회.
     */
    @Transactional(readOnly = true)
    public PublicMembershipPassResponse getPublicDetail(Long id) {
        MembershipPass pass = findById(id);

        List<MembershipPassLessonType> mappings =
                membershipPassLessonTypeRepository.findAllByMembershipPassId(id);
        List<String> lessonTypeNames = mappings.stream()
                .map(m -> m.getLessonType().getName())
                .toList();

        return toPublicResponse(pass, lessonTypeNames);
    }

    // ── private ──

    private MembershipPass findById(Long id) {
        return membershipPassRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_PASS_NOT_FOUND));
    }

    private List<LessonType> findLessonTypes(List<Long> lessonTypeIds) {
        return lessonTypeIds.stream()
                .map(ltId -> lessonTypeRepository.findById(ltId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.LESSON_TYPE_NOT_FOUND)))
                .toList();
    }

    private void saveMappings(MembershipPass pass, List<LessonType> lessonTypes) {
        List<MembershipPassLessonType> mappings = lessonTypes.stream()
                .map(lt -> MembershipPassLessonType.builder()
                        .membershipPass(pass)
                        .lessonType(lt)
                        .build())
                .toList();
        membershipPassLessonTypeRepository.saveAll(mappings);
    }

    private MembershipPassResponse toResponse(MembershipPass pass, List<LessonType> lessonTypes) {
        List<MembershipPassResponse.LessonTypeInfo> lessonTypeInfos = lessonTypes.stream()
                .map(lt -> new MembershipPassResponse.LessonTypeInfo(lt.getId(), lt.getName()))
                .toList();

        return new MembershipPassResponse(
                pass.getId(),
                pass.getPublicId(),
                pass.getName(),
                pass.getPrice(),
                pass.getTotalCount(),
                pass.getValidityDays(),
                pass.isUnlimited(),
                pass.getMonthlyLimit(),
                pass.getDisplayOrder(),
                pass.isVisible(),
                pass.isActive(),
                pass.getSaleStartDate() != null ? pass.getSaleStartDate().toString() : null,
                pass.getSaleEndDate() != null ? pass.getSaleEndDate().toString() : null,
                pass.getCategory(),
                pass.getDescription(),
                lessonTypeInfos,
                pass.getCreatedAt() != null ? pass.getCreatedAt().toString() : null
        );
    }

    private PublicMembershipPassResponse toPublicResponse(MembershipPass pass, List<String> lessonTypeNames) {
        return new PublicMembershipPassResponse(
                pass.getPublicId(),
                pass.getName(),
                pass.getPrice(),
                pass.getTotalCount(),
                pass.getValidityDays(),
                pass.isUnlimited(),
                pass.getCategory(),
                pass.getDescription(),
                lessonTypeNames
        );
    }
}
