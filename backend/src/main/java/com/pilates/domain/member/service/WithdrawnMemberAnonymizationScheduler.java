package com.pilates.domain.member.service;

import com.pilates.domain.member.entity.WithdrawnMemberLog;
import com.pilates.domain.member.repository.WithdrawnMemberLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 탈퇴 회원 개인정보 익명화 스케줄러.
 * 매일 새벽 3시 실행, 탈퇴 후 30일 지난 기록의 개인정보를 삭제한다.
 * 개인정보보호법 준수: 탈퇴 후 30일 이내 파기.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawnMemberAnonymizationScheduler {

    private static final int RETENTION_DAYS = 30;

    private final WithdrawnMemberLogRepository withdrawnMemberLogRepository;

    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시
    @Transactional
    public void anonymizeExpiredRecords() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);

        List<WithdrawnMemberLog> targets = withdrawnMemberLogRepository
                .findByAnonymizedFalseAndWithdrawnAtBefore(threshold);

        if (targets.isEmpty()) {
            return;
        }

        for (WithdrawnMemberLog record : targets) {
            record.anonymize();
        }

        log.info("탈퇴 회원 익명화 완료: {}건", targets.size());
    }
}
