package com.pilates.domain.membership.repository;

import com.pilates.domain.membership.entity.MembershipPass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 정기권 종류 리포지토리.
 */
public interface MembershipPassRepository extends JpaRepository<MembershipPass, Long> {

    Optional<MembershipPass> findByIdAndDeletedAtIsNull(Long id);

    Optional<MembershipPass> findByPublicIdAndDeletedAtIsNull(String publicId);

    List<MembershipPass> findAllByDeletedAtIsNullOrderByDisplayOrderAsc();

    boolean existsByNameAndDeletedAtIsNull(String name);
}
