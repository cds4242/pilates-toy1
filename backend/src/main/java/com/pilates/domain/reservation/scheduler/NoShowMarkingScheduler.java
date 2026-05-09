package com.pilates.domain.reservation.scheduler;

import com.pilates.domain.reservation.entity.Reservation;
import com.pilates.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 노쇼 자동 처리 스케줄러.
 * 수업 종료 30분 후에도 CONFIRMED 상태인 예약을 NO_SHOW로 전환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoShowMarkingScheduler {

    private final ReservationRepository reservationRepository;

    /**
     * 10분 간격으로 수업 종료 후 30분 이상 경과한 CONFIRMED 예약을 NO_SHOW 처리.
     */
    @Scheduled(fixedRate = 600_000)
    @Transactional
    public void markOverdueReservations() {
        LocalDate today = LocalDate.now();
        LocalTime cutoffTime = LocalTime.now().minusMinutes(30);

        // 자정 직후에는 cutoff가 음수가 될 수 있으므로 스킵
        if (cutoffTime.isAfter(LocalTime.now())) {
            return;
        }

        List<Reservation> overdueReservations =
                reservationRepository.findOverdueConfirmedReservations(today, cutoffTime);

        if (overdueReservations.isEmpty()) {
            return;
        }

        for (Reservation reservation : overdueReservations) {
            reservation.markNoShow();
        }

        log.info("노쇼 자동 처리: {}건", overdueReservations.size());
    }
}
