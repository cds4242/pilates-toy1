package com.pilates.domain.member.service;

import com.pilates.domain.member.entity.WithdrawnMemberLog;
import com.pilates.domain.member.repository.WithdrawnMemberLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 탈퇴 회원 익명화 스케줄러 단위 테스트.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WithdrawnMemberAnonymizationSchedulerTest {

    @Autowired
    private WithdrawnMemberAnonymizationScheduler scheduler;

    @Autowired
    private WithdrawnMemberLogRepository repository;

    @Test
    @DisplayName("30일 이상 지난 탈퇴 회원은 익명화된다")
    void anonymize_expiredRecords() {
        // given: 31일 전 탈퇴 회원
        WithdrawnMemberLog log = WithdrawnMemberLog.builder()
                .memberId(999L)
                .phoneHashOriginal("abc123hash")
                .nameOriginal("v1::encryptedName")
                .birthEncryptedOriginal("v1::encryptedBirth")
                .withdrawnAt(LocalDateTime.now().minusDays(31))
                .withdrawalReason("테스트 탈퇴")
                .build();
        repository.save(log);

        // when
        scheduler.anonymizeExpiredRecords();

        // then
        WithdrawnMemberLog result = repository.findById(log.getId()).orElseThrow();
        assertThat(result.isAnonymized()).isTrue();
        assertThat(result.getPhoneHashOriginal()).isNull();
        assertThat(result.getNameOriginal()).isNull();
        assertThat(result.getBirthEncryptedOriginal()).isNull();
        assertThat(result.getAnonymizedAt()).isNotNull();
    }

    @Test
    @DisplayName("30일 미만 탈퇴 회원은 익명화되지 않는다")
    void doNotAnonymize_recentRecords() {
        // given: 29일 전 탈퇴 회원
        WithdrawnMemberLog log = WithdrawnMemberLog.builder()
                .memberId(998L)
                .phoneHashOriginal("def456hash")
                .nameOriginal("v1::encryptedName2")
                .birthEncryptedOriginal("v1::encryptedBirth2")
                .withdrawnAt(LocalDateTime.now().minusDays(29))
                .withdrawalReason("테스트 탈퇴")
                .build();
        repository.save(log);

        // when
        scheduler.anonymizeExpiredRecords();

        // then
        WithdrawnMemberLog result = repository.findById(log.getId()).orElseThrow();
        assertThat(result.isAnonymized()).isFalse();
        assertThat(result.getPhoneHashOriginal()).isEqualTo("def456hash");
        assertThat(result.getNameOriginal()).isEqualTo("v1::encryptedName2");
    }

    @Test
    @DisplayName("이미 익명화된 회원은 변경되지 않는다")
    void skipAlreadyAnonymized() {
        // given: 이미 익명화된 기록
        WithdrawnMemberLog log = WithdrawnMemberLog.builder()
                .memberId(997L)
                .phoneHashOriginal(null)
                .nameOriginal(null)
                .birthEncryptedOriginal(null)
                .withdrawnAt(LocalDateTime.now().minusDays(60))
                .withdrawalReason("테스트")
                .build();
        repository.save(log);
        // 수동 익명화 상태 설정
        log.anonymize();
        repository.save(log);

        LocalDateTime originalAnonymizedAt = log.getAnonymizedAt();

        // when
        scheduler.anonymizeExpiredRecords();

        // then: anonymizedAt이 변경되지 않음
        WithdrawnMemberLog result = repository.findById(log.getId()).orElseThrow();
        assertThat(result.isAnonymized()).isTrue();
        assertThat(result.getAnonymizedAt()).isEqualTo(originalAnonymizedAt);
    }
}
