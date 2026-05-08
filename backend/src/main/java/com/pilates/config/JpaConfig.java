package com.pilates.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * JPA Auditing 활성화 설정.
 * BaseEntity의 createdAt, updatedAt, createdBy, updatedBy 자동 관리.
 * createdBy/updatedBy는 JWT 인증 구현 후 SecurityContext에서 추출하도록 변경 예정.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        // TODO: Spring Security 인증 구현 후 SecurityContext에서 사용자 ID 추출
        return () -> Optional.of("system");
    }
}
