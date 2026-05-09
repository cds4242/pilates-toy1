package com.pilates.domain.classroom.repository;

import com.pilates.domain.classroom.entity.LessonType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 수업 유형 리포지토리.
 */
public interface LessonTypeRepository extends JpaRepository<LessonType, Long> {

    List<LessonType> findAllByActiveTrue();

    boolean existsByNameAndActiveTrue(String name);
}
