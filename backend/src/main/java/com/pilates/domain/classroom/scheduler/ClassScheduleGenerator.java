package com.pilates.domain.classroom.scheduler;

import com.pilates.domain.admin.entity.Holiday;
import com.pilates.domain.admin.repository.HolidayRepository;
import com.pilates.domain.classroom.entity.ClassSchedule;
import com.pilates.domain.classroom.entity.FixedSchedule;
import com.pilates.domain.classroom.repository.ClassScheduleRepository;
import com.pilates.domain.classroom.repository.FixedScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 고정 스케줄 기반 수업 자동 생성기.
 * 매주 일요일 자정에 실행되며, 관리자 API를 통해 수동 실행도 가능하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClassScheduleGenerator {

    private final FixedScheduleRepository fixedScheduleRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final HolidayRepository holidayRepository;

    /**
     * 매주 일요일 자정에 향후 4주간 수업을 자동 생성한다.
     */
    @Scheduled(cron = "0 0 0 * * SUN")
    public void generateWeekly() {
        generateUpcomingClasses(4);
    }

    /**
     * 향후 N주간 수업을 고정 스케줄 기반으로 생성한다.
     * 이미 존재하는 수업, 공휴일은 건너뛴다.
     *
     * @param weeks 생성할 주 수 (기본 4)
     * @return 생성된 수업 수
     */
    @Transactional
    public int generateUpcomingClasses(int weeks) {
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = startDate.plusWeeks(weeks);

        // 활성 고정 스케줄 조회
        List<FixedSchedule> activeSchedules = fixedScheduleRepository.findAllByActiveTrue();
        if (activeSchedules.isEmpty()) {
            log.info("활성 고정 스케줄이 없습니다. 수업 생성 생략.");
            return 0;
        }

        // 기간 내 공휴일 조회
        Set<LocalDate> holidays = holidayRepository.findAllByHolidayDateBetween(startDate, endDate)
                .stream()
                .map(Holiday::getHolidayDate)
                .collect(Collectors.toSet());

        int createdCount = 0;
        int skippedCount = 0;

        for (FixedSchedule fixed : activeSchedules) {
            DayOfWeek targetDay = fixed.getDayOfWeek();

            // 시작일부터 종료일까지 해당 요일의 날짜들을 구한다
            LocalDate date = startDate.with(TemporalAdjusters.nextOrSame(targetDay));

            while (!date.isAfter(endDate)) {
                // 공휴일 체크
                if (holidays.contains(date)) {
                    log.debug("공휴일 건너뜀: date={}, schedule={}", date, fixed.getId());
                    skippedCount++;
                    date = date.plusWeeks(1);
                    continue;
                }

                // 이미 존재하는 수업 체크
                if (classScheduleRepository.existsByFixedScheduleIdAndClassDate(fixed.getId(), date)) {
                    log.debug("이미 존재하는 수업 건너뜀: date={}, schedule={}", date, fixed.getId());
                    skippedCount++;
                    date = date.plusWeeks(1);
                    continue;
                }

                // 수업 생성
                ClassSchedule newClass = ClassSchedule.createFromFixed(fixed, date);
                classScheduleRepository.save(newClass);
                createdCount++;

                date = date.plusWeeks(1);
            }
        }

        log.info("수업 자동 생성 완료: created={}, skipped={}, period={} ~ {}",
                createdCount, skippedCount, startDate, endDate);
        return createdCount;
    }
}
