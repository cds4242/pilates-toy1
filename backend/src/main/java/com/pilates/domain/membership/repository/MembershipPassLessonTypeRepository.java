package com.pilates.domain.membership.repository;

import com.pilates.domain.membership.entity.MembershipPassLessonType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 정기권 종류-수업 유형 매핑 리포지토리.
 */
public interface MembershipPassLessonTypeRepository extends JpaRepository<MembershipPassLessonType, Long> {

    List<MembershipPassLessonType> findAllByMembershipPassId(Long membershipPassId);

    void deleteAllByMembershipPassId(Long membershipPassId);
}
