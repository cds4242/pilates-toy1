package com.pilates.domain.membership.repository;

import com.pilates.domain.membership.entity.MembershipHolding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 정기권 홀딩 이력 Repository.
 */
public interface MembershipHoldingRepository extends JpaRepository<MembershipHolding, Long> {

    List<MembershipHolding> findAllByMembershipIdOrderByCreatedAtDesc(Long membershipId);
}
