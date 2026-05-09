package com.pilates.domain.member.repository;

import com.pilates.domain.member.entity.MemberMemo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberMemoRepository extends JpaRepository<MemberMemo, Long> {

    List<MemberMemo> findAllByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long memberId);

    Optional<MemberMemo> findByIdAndDeletedAtIsNull(Long id);
}
