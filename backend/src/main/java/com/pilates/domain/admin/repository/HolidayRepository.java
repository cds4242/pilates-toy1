package com.pilates.domain.admin.repository;

import com.pilates.domain.admin.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * 휴무일/공휴일 리포지토리.
 */
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    boolean existsByHolidayDate(LocalDate date);

    List<Holiday> findAllByHolidayDateBetween(LocalDate from, LocalDate to);
}
