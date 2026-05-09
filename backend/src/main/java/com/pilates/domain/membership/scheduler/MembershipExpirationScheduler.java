package com.pilates.domain.membership.scheduler;

import com.pilates.domain.membership.entity.Membership;
import com.pilates.domain.membership.entity.MembershipStatus;
import com.pilates.domain.membership.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 정기권 만료 스케줄러.
 * 매일 새벽 4시 실행, 종료일이 지난 정기권을 EXPIRED로 전환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipExpirationScheduler {

    private final MembershipRepository membershipRepository;

    /**
     * 종료일이 지난 정기권 만료 처리.
     * EXPIRED, EXHAUSTED 상태는 제외한다.
     * TODO: HOLDING 중 만료 정책 (현재는 HOLDING 우선 유지)
     */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void expireOverdueMemberships() {
        LocalDate today = LocalDate.now();
        List<MembershipStatus> excludeStatuses = List.of(
                MembershipStatus.EXPIRED,
                MembershipStatus.EXHAUSTED,
                MembershipStatus.HOLDING
        );

        List<Membership> targets = membershipRepository
                .findAllByEndDateBeforeAndStatusNotInAndDeletedAtIsNull(today, excludeStatuses);

        if (targets.isEmpty()) {
            return;
        }

        for (Membership membership : targets) {
            membership.expire();
        }

        log.info("정기권 만료 처리 완료: {}건", targets.size());
    }
}
