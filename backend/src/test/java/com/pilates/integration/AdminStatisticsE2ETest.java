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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminStatisticsE2ETest {

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
    @DisplayName("시나리오1: 매출 통계 (일별)")
    void scenario1_revenueDaily() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();

        MvcResult result = mockMvc.perform(get("/api/admin/statistics/revenue")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("groupBy", "daily")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data.get("total")).isNotNull();
        assertThat(data.get("breakdown")).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("시나리오2: 매출 통계 (월별)")
    void scenario2_revenueMonthly() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        LocalDate from = LocalDate.now().minusMonths(3);
        LocalDate to = LocalDate.now();

        MvcResult result = mockMvc.perform(get("/api/admin/statistics/revenue")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("groupBy", "monthly")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data.get("total")).isNotNull();
    }

    @Test
    @Order(3)
    @DisplayName("시나리오3: 회원 추이")
    void scenario3_memberTrend() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        // 회원 가입 생성 (통계 데이터용)
        authHelper.loginAsMember("01099003001");

        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now();

        MvcResult result = mockMvc.perform(get("/api/admin/statistics/members")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data.get("signups")).isNotNull();
        assertThat(data.get("activeCount").asLong()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(4)
    @DisplayName("시나리오4: 출석률 통계")
    void scenario4_attendanceStats() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();

        MvcResult result = mockMvc.perform(get("/api/admin/statistics/attendance")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data.get("overallRate")).isNotNull();
        assertThat(data.get("byInstructor")).isNotNull();
        assertThat(data.get("byLessonType")).isNotNull();
    }

    @Test
    @Order(5)
    @DisplayName("시나리오5: 인기 시간대")
    void scenario5_popularTimes() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();

        MvcResult result = mockMvc.perform(get("/api/admin/statistics/popular-times")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data.get("byHour")).isNotNull();
        assertThat(data.get("byDayOfWeek")).isNotNull();
    }
}
