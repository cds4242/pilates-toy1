package com.pilates.domain.membership.repository;

import com.pilates.domain.membership.entity.Membership;
import com.pilates.domain.membership.entity.MembershipStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 정기권 Repository.
 */
public interface MembershipRepository extends JpaRepository<Membership, Long> {

    List<Membership> findAllByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long memberId);

    List<Membership> findAllByMemberIdAndStatusAndDeletedAtIsNull(Long memberId, MembershipStatus status);

    List<Membership> findAllByEndDateBeforeAndStatusNotInAndDeletedAtIsNull(LocalDate date, List<MembershipStatus> excludeStatuses);

    Optional<Membership> findByIdAndDeletedAtIsNull(Long id);

    Optional<Membership> findByPublicIdAndDeletedAtIsNull(String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Membership m WHERE m.id = :id AND m.deletedAt IS NULL")
    Optional<Membership> findByIdForUpdate(@Param("id") Long id);

    List<Membership> findAllByEndDateAndStatusAndDeletedAtIsNull(LocalDate endDate, MembershipStatus status);
}
