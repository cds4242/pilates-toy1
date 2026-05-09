package com.pilates.domain.admin.repository;

import com.pilates.domain.admin.entity.StudioSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudioSettingRepository extends JpaRepository<StudioSetting, Long> {

    Optional<StudioSetting> findBySettingKey(String settingKey);
}
