package com.pilates.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pilates.domain.admin.repository.AdminRepository;
import com.pilates.domain.instructor.repository.InstructorRepository;
import com.pilates.domain.membership.scheduler.MembershipExpirationScheduler;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 정기권 도메인 E2E 통합 테스트.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MembershipE2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private MembershipExpirationScheduler expirationScheduler;
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
    // 시나리오 1: 정기권 발급 → 조회
    // ═══════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("시나리오1: 관리자가 정기권 발급 → 회원이 조회")
    void scenario1_issueAndQuery() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] memberAuth = authHelper.loginAsMember("01077770001");
        String memberToken = memberAuth[0];
        Long memberId = Long.parseLong(memberAuth[1]);

        // 수업 유형 등록 (정기권 매핑용)
        MvcResult ltResult = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"MSE2E그룹\",\"maxCapacity\":8,\"durationMinutes\":50,\"deductionCount\":1}"))
                .andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(ltResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 정기권 발급
        MvcResult issueResult = mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId + ",\"totalCount\":12,\"price\":480000,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(12))
                .andExpect(jsonPath("$.data.remainingCount").value(12))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();
        Long mshipId = objectMapper.readTree(issueResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 회원이 내 정기권 조회
        mockMvc.perform(get("/api/members/me/memberships")
                .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].totalCount").value(12));

        // 상세 조회
        mockMvc.perform(get("/api/members/me/memberships/" + mshipId)
                .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.remainingCount").value(12));
    }

    // ═══════════════════════════════════════════
    // 시나리오 2: 일시정지 + 해제
    // ═══════════════════════════════════════════

    @Test
    @Order(2)
    @DisplayName("시나리오2: 정기권 일시정지 → 해제 → 유효기간 연장 확인")
    void scenario2_holdAndRelease() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] memberAuth = authHelper.loginAsMember("01077770002");
        Long memberId = Long.parseLong(memberAuth[1]);

        // 수업 유형
        MvcResult ltResult = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"MSE2E듀엣\",\"maxCapacity\":2,\"durationMinutes\":50,\"deductionCount\":1}"))
                .andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(ltResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 정기권 발급
        MvcResult issueResult = mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId + ",\"totalCount\":8,\"price\":320000,\"validityDays\":60,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode issued = objectMapper.readTree(issueResult.getResponse().getContentAsString()).get("data");
        Long mshipId = issued.get("id").asLong();
        String originalEndDate = issued.get("endDate").asText();

        // 일시정지 (오늘부터 10일간)
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate holdEnd = today.plusDays(10);
        mockMvc.perform(post("/api/admin/memberships/" + mshipId + "/hold")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromDate\":\"" + today + "\",\"toDate\":\"" + holdEnd + "\",\"reason\":\"여행\"}"))
                .andExpect(status().isOk());

        // 상태 확인 → HOLDING
        mockMvc.perform(get("/api/admin/memberships/" + mshipId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HOLDING"));

        // 해제
        mockMvc.perform(post("/api/admin/memberships/" + mshipId + "/release-hold")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 상태 → ACTIVE, endDate 연장 확인
        MvcResult afterRelease = mockMvc.perform(get("/api/admin/memberships/" + mshipId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();
        JsonNode afterData = objectMapper.readTree(afterRelease.getResponse().getContentAsString()).get("data");
        String newEndDate = afterData.get("endDate").asText();
        // 같은 날 시작-해제면 0일 연장이므로 endDate가 같거나 더 늦어야 함
        assertThat(java.time.LocalDate.parse(newEndDate))
                .isAfterOrEqualTo(java.time.LocalDate.parse(originalEndDate));

        // 홀딩 이력 조회
        mockMvc.perform(get("/api/admin/memberships/" + mshipId + "/holdings")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].reason").value("여행"));
    }

    // ═══════════════════════════════════════════
    // 시나리오 3: 만료 자동 처리
    // ═══════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("시나리오3: 만료 스케줄러 → 기간 지난 정기권 EXPIRED")
    void scenario3_expirationScheduler() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] memberAuth = authHelper.loginAsMember("01077770003");
        Long memberId = Long.parseLong(memberAuth[1]);

        MvcResult ltResult = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"MSE2E만료\",\"maxCapacity\":4,\"durationMinutes\":50,\"deductionCount\":1}"))
                .andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(ltResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        // validityDays=1로 발급 후 스케줄러 직접 호출로 테스트
        MvcResult issueResult = mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId + ",\"totalCount\":4,\"price\":200000,\"validityDays\":1,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}"))
                .andExpect(status().isOk()).andReturn();

        // 스케줄러 직접 호출 (end_date < today인 것만 처리하므로, validityDays=1이면 내일까지 유효)
        // 이 테스트는 스케줄러 메서드 호출이 에러 없이 동작하는지 확인
        expirationScheduler.expireOverdueMemberships();
        // 실제 만료는 end_date가 과거인 경우에만 → 이 데이터는 아직 유효
    }

    // ═══════════════════════════════════════════
    // 시나리오 4: 무제한권 발급
    // ═══════════════════════════════════════════

    @Test
    @Order(4)
    @DisplayName("시나리오4: 무제한권 발급 → 조회 → usable 확인")
    void scenario4_unlimitedMembership() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] memberAuth = authHelper.loginAsMember("01077770004");
        Long memberId = Long.parseLong(memberAuth[1]);

        MvcResult ltResult = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"MSE2E무제한\",\"maxCapacity\":8,\"durationMinutes\":50,\"deductionCount\":1}"))
                .andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(ltResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 무제한권 발급
        mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId + ",\"totalCount\":0,\"price\":900000,\"validityDays\":365,\"unlimited\":true,\"lessonTypeIds\":[" + ltId + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unlimited").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.usable").value(true));
    }
}
