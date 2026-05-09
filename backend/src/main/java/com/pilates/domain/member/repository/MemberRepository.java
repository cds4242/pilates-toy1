package com.pilates.domain.member.repository;

import com.pilates.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 회원 Repository.
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    /** phone_hash로 회원 조회 (중복 확인 + 로그인). soft delete 제외. */
    Optional<Member> findByPhoneHashAndDeletedAtIsNull(String phoneHash);

    /** public_id로 회원 조회. */
    Optional<Member> findByPublicIdAndDeletedAtIsNull(String publicId);

    /** phone_hash 존재 여부 (중복 가입 방지). */
    boolean existsByPhoneHashAndDeletedAtIsNull(String phoneHash);
}
