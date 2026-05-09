package com.pilates.config;

import com.pilates.config.security.CustomAccessDeniedHandler;
import com.pilates.config.security.JwtAuthenticationEntryPoint;
import com.pilates.config.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정.
 * JWT 기반 인증, 세션 비활성화, CSRF 비활성화.
 *
 * ┌─────────────────────────────────┬────────┬────────────┬───────┬─────────────┐
 * │ 경로                             │ MEMBER │ INSTRUCTOR │ ADMIN │ SUPER_ADMIN │
 * ├─────────────────────────────────┼────────┼────────────┼───────┼─────────────┤
 * │ /api/auth/**                    │        permitAll                          │
 * │ /api/admin/auth/**              │        permitAll                          │
 * │ /api/health, /api/test/**       │        permitAll                          │
 * │ /api/instructors/** (공개)       │        permitAll                          │
 * │ /api/class-schedules/** (공개)   │        permitAll                          │
 * │ /api/admin/**                   │   X    │     O      │   O   │      O      │
 * │ /api/instructor/**              │   X    │     O      │   O   │      O      │
 * │ /api/members/me/**              │   O    │     O      │   O   │      O      │
 * │ 기타 (인증 필요)                  │   O    │     O      │   O   │      O      │
 * └─────────────────────────────────┴────────┴────────────┴───────┴─────────────┘
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    /** 인증 없이 접근 가능한 경로 */
    private static final String[] PUBLIC_URLS = {
            "/api/health",
            "/api/auth/**",
            "/api/admin/auth/**",
            "/api/test/**",
            "/api/instructors/**",
            "/api/lesson-types/**",
            "/api/class-schedules/**",
            "/api/membership-passes/**",
            "/api/payments/confirm",
            "/api/webhooks/**",
            "/actuator/health",
            "/actuator/info",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/h2-console/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers("/api/instructor/**").hasAnyRole("INSTRUCTOR", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/admin/settings/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/admin/**").hasAnyRole("INSTRUCTOR", "ADMIN", "SUPER_ADMIN")
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** BCrypt strength 12 (보안 강화) */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
