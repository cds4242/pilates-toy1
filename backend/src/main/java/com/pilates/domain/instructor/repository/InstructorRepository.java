package com.pilates.domain.instructor.repository;

import com.pilates.domain.instructor.entity.Instructor;
import com.pilates.domain.instructor.entity.InstructorStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 강사 리포지토리.
 */
public interface InstructorRepository extends JpaRepository<Instructor, Long> {

    Optional<Instructor> findByPublicIdAndDeletedAtIsNull(String publicId);

    List<Instructor> findAllByStatusAndDeletedAtIsNull(InstructorStatus status);

    List<Instructor> findAllByDeletedAtIsNull();
}
