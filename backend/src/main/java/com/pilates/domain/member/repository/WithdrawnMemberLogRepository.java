package com.pilates.domain.member.repository;

import com.pilates.domain.member.entity.WithdrawnMemberLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 탈퇴 회원 로그 Repository.
 */
public interface WithdrawnMemberLogRepository extends JpaRepository<WithdrawnMemberLog, Long> {

    /** 익명화 안 된 기록 중 탈퇴일이 threshold 이전인 것 조회. */
    List<WithdrawnMemberLog> findByAnonymizedFalseAndWithdrawnAtBefore(LocalDateTime threshold);
}
