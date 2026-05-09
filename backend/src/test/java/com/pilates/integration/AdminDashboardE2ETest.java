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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminDashboardE2ETest {

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
    @DisplayName("시나리오1: 대시보드 모든 영역 정상 데이터 반환")
    void scenario1_dashboardAllAreas() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] memberResult = authHelper.loginAsMember("01099001001");
        String memberId = memberResult[1];

        // 강사 + 수업유형 + 정기권 + 수업 생성
        MvcResult ir = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"대시보드강사1\",\"phone\":\"010-0000-0001\"}")).andExpect(status().isOk()).andReturn();
        Long instrId = objectMapper.readTree(ir.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult lt = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"대시보드유형1\",\"maxCapacity\":5,\"durationMinutes\":50,\"deductionCount\":1}")).andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(lt.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult mp = mockMvc.perform(post("/api/admin/membership-passes")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"대시보드패스1\",\"price\":100000,\"totalCount\":10,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}")).andExpect(status().isOk()).andReturn();
        Long passId = objectMapper.readTree(mp.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 정기권 발급 (잔여 1회 → lowMembership 알림 트리거)
        mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId + ",\"totalCount\":1,\"price\":100000,\"validityDays\":5,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "],\"membershipPassId\":" + passId + "}")).andExpect(status().isOk());

        // 오늘 수업 생성
        LocalDate today = LocalDate.now();
        mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + today + "\",\"startTime\":\"10:00\",\"endTime\":\"10:50\",\"maxCapacity\":5}")).andExpect(status().isOk());

        // 대시보드 호출
        MvcResult result = mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data.get("todayClasses")).isNotNull();
        assertThat(data.get("todayClasses").get("count").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(data.get("thisWeekRevenue")).isNotNull();
        assertThat(data.get("expiringMemberships")).isNotNull();
        assertThat(data.get("alerts")).isNotNull();
        assertThat(data.get("alerts").get("lowMembershipMembers")).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("시나리오2: 권한 분리 (회원 토큰 → 403)")
    void scenario2_memberAccessDenied() throws Exception {
        String[] memberResult = authHelper.loginAsMember("01099001002");
        String memberToken = memberResult[0];

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    @DisplayName("시나리오3: 데이터 없는 경우 빈 응답 (오류 X)")
    void scenario3_emptyDashboard() throws Exception {
        String adminToken = authHelper.loginAsAdmin();

        MvcResult result = mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data.get("todayClasses").get("schedules")).isNotNull();
        assertThat(data.get("thisWeekRevenue").get("total")).isNotNull();
        assertThat(data.get("alerts").get("noShowMembers")).isNotNull();
    }
}
