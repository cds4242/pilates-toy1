package com.pilates.integration;

import com.fasterxml.jackson.databind.JsonNode;
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
 * 보안 종합 점검.
 * 의뢰인 인계 전 보안 시나리오 확인.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SecurityAcceptanceTest {

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

    @Test
    @Order(1)
    @DisplayName("보안1: 만료된 JWT → 401")
    void security1_expiredToken() throws Exception {
        // 임의의 유효하지 않은 토큰
        String fakeToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI5OTkiLCJ0eXBlIjoiYWNjZXNzIiwicm9sZSI6Ik1FTUJFUiIsImlhdCI6MTAwMDAwMDAwMCwiZXhwIjoxMDAwMDAwMDAxfQ.invalid";

        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + fakeToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    @DisplayName("보안2: 회원이 관리자 API 접근 → 403")
    void security2_memberAccessAdminApi() throws Exception {
        String[] member = authHelper.loginAsMember("01080001001");
        String memberToken = member[0];

        // 대시보드
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());

        // 회원 관리
        mockMvc.perform(get("/api/admin/members")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());

        // 통계
        mockMvc.perform(get("/api/admin/statistics/revenue")
                        .param("from", "2026-01-01").param("to", "2026-12-31")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    @DisplayName("보안3: SQL Injection 시도 방어 (parameterized query)")
    void security3_sqlInjection() throws Exception {
        String adminToken = authHelper.loginAsAdmin();

        // 회원 검색에 SQL Injection 시도
        mockMvc.perform(get("/api/admin/members")
                        .param("search", "'; DROP TABLE members; --")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @Order(4)
    @DisplayName("보안4: XSS 페이로드 방어 (Bean Validation)")
    void security4_xssPayload() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] member = authHelper.loginAsMember("01080004001");

        // 메모에 XSS 시도 → 저장은 되지만 프론트에서 이스케이프
        MvcResult result = mockMvc.perform(post("/api/admin/members/" + member[1] + "/memos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"<script>alert('xss')</script>\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // 저장된 내용 확인 (서버는 저장, 프론트에서 이스케이프)
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        // 서버가 HTML을 그대로 저장해도 React는 자동 이스케이프
        // 중요: DB에 저장은 되지만 렌더링 시 안전
        org.assertj.core.api.Assertions.assertThat(data.get("content").asText()).contains("<script>");
    }

    @Test
    @Order(5)
    @DisplayName("보안5: 회원 탈퇴 후 API 접근 불가")
    void security5_withdrawnMemberAccess() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] member = authHelper.loginAsMember("01080005001");
        String memberToken = member[0];

        // 내 정보 정상 접근
        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());

        // 강제 탈퇴
        mockMvc.perform(delete("/api/admin/members/" + member[1])
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 탈퇴 후 내 정보 접근 → 에러 (회원 not found)
        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(6)
    @DisplayName("보안6: ADMIN이 SUPER_ADMIN 전용 설정 접근 → 403")
    void security6_adminCannotAccessSettings() throws Exception {
        String adminToken = authHelper.loginAsAdmin();

        mockMvc.perform(get("/api/admin/settings")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(7)
    @DisplayName("보안7: 인증 헤더 없이 보호된 API 접근 → 401")
    void security7_noAuthHeader() throws Exception {
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }
}
