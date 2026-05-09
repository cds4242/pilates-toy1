package com.pilates.domain.membership.repository;

import com.pilates.domain.membership.entity.MembershipLessonType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 정기권-수업 유형 매핑 Repository.
 */
public interface MembershipLessonTypeRepository extends JpaRepository<MembershipLessonType, Long> {

    List<MembershipLessonType> findAllByMembershipId(Long membershipId);

    void deleteAllByMembershipId(Long membershipId);
}
