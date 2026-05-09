package com.pilates.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pilates.domain.admin.repository.AdminRepository;
import com.pilates.domain.instructor.repository.InstructorRepository;
import com.pilates.integration.support.AuthTestHelper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 권한 매트릭스 종합 검증 E2E 테스트.
 * SecurityConfig의 URL별 role 제한이 정확히 동작하는지 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SecurityE2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AdminRepository adminRepository;
    @Autowired private InstructorRepository instructorRepository;

    private AuthTestHelper authHelper;

    @BeforeEach
    void setUp() {
        authHelper = new AuthTestHelper(mockMvc, objectMapper, redisTemplate, passwordEncoder, adminRepository, instructorRepository);
        authHelper.clearRedis();
    }

    // ═══════════════════════════════════════════
    // 시나리오 1: 인증 없음 → 401
    // ═══════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("시나리오1: 인증 없이 보호된 API → 401")
    void scenario1_noAuth401() throws Exception {
        // 회원 API (인증 필요)
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized());

        // 예약 API (인증 필요)
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classScheduleId\":1}"))
                .andExpect(status().isUnauthorized());

        // admin API (인증 필요)
        mockMvc.perform(get("/api/admin/class-schedules")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isUnauthorized());
    }

    // ═══════════════════════════════════════════
    // 시나리오 2: 만료된 토큰 → 401
    // ═══════════════════════════════════════════

    @Test
    @Order(2)
    @DisplayName("시나리오2: 잘못된 토큰 → 401")
    void scenario2_invalidToken401() throws Exception {
        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    // ═══════════════════════════════════════════
    // 시나리오 3: 회원 토큰 → admin API 403
    // ═══════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("시나리오3: 회원 토큰으로 admin API 5개 도메인 → 403")
    void scenario3_memberTokenAdminApi403() throws Exception {
        String[] memberAuth = authHelper.loginAsMember("01099880001");
        String memberToken = memberAuth[0];

        // 강사 등록
        mockMvc.perform(post("/api/admin/instructors")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"테스트\",\"phone\":\"010-0000-0000\"}"))
                .andExpect(status().isForbidden());

        // 수업 유형 등록
        mockMvc.perform(post("/api/admin/lesson-types")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"테스트\",\"maxCapacity\":8,\"durationMinutes\":50,\"deductionCount\":1}"))
                .andExpect(status().isForbidden());

        // 수업 조회
        mockMvc.perform(get("/api/admin/class-schedules")
                        .header("Authorization", "Bearer " + memberToken)
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isForbidden());

        // 정기권 종류 등록
        mockMvc.perform(post("/api/admin/membership-passes")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"테스트\",\"price\":100000,\"totalCount\":10,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[1]}"))
                .andExpect(status().isForbidden());

        // 출석 통계
        mockMvc.perform(get("/api/admin/attendances/no-show-counts")
                        .header("Authorization", "Bearer " + memberToken)
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════
    // 시나리오 4: 회원 토큰 → instructor API 403
    // ═══════════════════════════════════════════

    @Test
    @Order(4)
    @DisplayName("시나리오4: 회원 토큰으로 instructor API → 403")
    void scenario4_memberTokenInstructorApi403() throws Exception {
        String[] memberAuth = authHelper.loginAsMember("01099880002");
        String memberToken = memberAuth[0];

        // 강사 수업 조회
        mockMvc.perform(get("/api/instructor/class-schedules")
                        .header("Authorization", "Bearer " + memberToken)
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isForbidden());

        // 출석 체크
        mockMvc.perform(post("/api/instructor/attendances/1")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ATTENDED\"}"))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════
    // 시나리오 5: admin 토큰 → admin/instructor API 200
    // ═══════════════════════════════════════════

    @Test
    @Order(5)
    @DisplayName("시나리오5: admin 토큰으로 admin + instructor API → 접근 가능")
    void scenario5_adminTokenAccessGranted() throws Exception {
        String adminToken = authHelper.loginAsAdmin();

        // admin API → 200 (강사 등록)
        MvcResult ir = mockMvc.perform(post("/api/admin/instructors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"보안강사\",\"phone\":\"010-0000-0000\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Long instrId = objectMapper.readTree(ir.getResponse().getContentAsString()).get("data").get("id").asLong();

        // admin이 instructor API 접근 가능 (instructorId는 없으므로 ACCESS_DENIED at service level, but 403 from Spring Security는 아님)
        // admin 토큰에는 instructorId가 없으므로 controller에서 ACCESS_DENIED(COMMON_005) 반환
        // 이것은 Spring Security 403이 아니라 비즈니스 로직 403
        mockMvc.perform(get("/api/instructor/class-schedules")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_005"));

        // 강사 admin으로 로그인하면 instructor API 접근 가능
        String instructorToken = authHelper.loginAsInstructor(instrId);

        // instructor API → instructorId 매칭 안 되면 404 (본인 수업 없음)
        // 하지만 Spring Security 레벨에서는 통과 (200 or business error, not 403 from security)
        mockMvc.perform(get("/api/instructor/class-schedules")
                        .header("Authorization", "Bearer " + instructorToken)
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isOk());
    }

    // ═══════════════════════════════════════════
    // 시나리오 6: permitAll 경로 검증
    // ═══════════════════════════════════════════

    @Test
    @Order(6)
    @DisplayName("시나리오6: permitAll 경로 → 인증 없이 접근 가능")
    void scenario6_permitAllPaths() throws Exception {
        // 헬스 체크
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());

        // 공개 강사 목록
        mockMvc.perform(get("/api/instructors"))
                .andExpect(status().isOk());

        // 공개 수업 유형
        mockMvc.perform(get("/api/lesson-types"))
                .andExpect(status().isOk());

        // 공개 수업 시간표
        mockMvc.perform(get("/api/class-schedules")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isOk());

        // admin 로그인 (permitAll)
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"nonexistent\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized()); // 401은 인증 실패, 403이 아님
    }
}
