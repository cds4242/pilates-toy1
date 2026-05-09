package com.pilates.domain.admin.repository;

import com.pilates.domain.admin.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByLoginIdAndDeletedAtIsNull(String loginId);
}
