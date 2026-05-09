package com.pilates.domain.member.repository;

import com.pilates.domain.member.entity.WithdrawnMemberLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 탈퇴 회원 로그 Repository.
 */
public interface WithdrawnMemberLogRepository extends JpaRepository<WithdrawnMemberLog, Long> {
}
