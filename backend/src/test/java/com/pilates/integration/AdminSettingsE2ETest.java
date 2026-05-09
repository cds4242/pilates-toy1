package com.pilates.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pilates.domain.admin.entity.StudioSetting;
import com.pilates.domain.admin.repository.AdminRepository;
import com.pilates.domain.admin.repository.StudioSettingRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminSettingsE2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AdminRepository adminRepository;
    @Autowired private InstructorRepository instructorRepository;
    @Autowired private StudioSettingRepository studioSettingRepository;

    private AuthTestHelper authHelper;

    @BeforeEach
    void setUp() {
        authHelper = new AuthTestHelper(mockMvc, objectMapper, redisTemplate, passwordEncoder, adminRepository, instructorRepository);
        authHelper.clearRedis();
    }

    @Test
    @Order(1)
    @DisplayName("시나리오1: 학원 설정 조회 (SUPER_ADMIN)")
    void scenario1_getSettings() throws Exception {
        // 설정 시드 데이터
        if (studioSettingRepository.findBySettingKey("CANCEL_DEADLINE_HOURS").isEmpty()) {
            studioSettingRepository.save(StudioSetting.builder()
                    .settingKey("CANCEL_DEADLINE_HOURS")
                    .settingValue("2")
                    .description("예약 취소 마감 시간 (시간)")
                    .build());
        }

        String superAdminToken = authHelper.loginAsSuperAdmin();

        MvcResult result = mockMvc.perform(get("/api/admin/settings")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data.get("settings")).isNotNull();
        assertThat(data.get("settings").size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(2)
    @DisplayName("시나리오2: 학원 설정 수정 (SUPER_ADMIN)")
    void scenario2_updateSettings() throws Exception {
        if (studioSettingRepository.findBySettingKey("CANCEL_DEADLINE_HOURS").isEmpty()) {
            studioSettingRepository.save(StudioSetting.builder()
                    .settingKey("CANCEL_DEADLINE_HOURS")
                    .settingValue("2")
                    .description("예약 취소 마감 시간 (시간)")
                    .build());
        }

        String superAdminToken = authHelper.loginAsSuperAdmin();

        mockMvc.perform(patch("/api/admin/settings")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"settings\":{\"CANCEL_DEADLINE_HOURS\":\"3\"}}"))
                .andExpect(status().isOk());

        // 확인
        MvcResult result = mockMvc.perform(get("/api/admin/settings")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode settings = objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("settings");
        boolean found = false;
        for (JsonNode s : settings) {
            if ("CANCEL_DEADLINE_HOURS".equals(s.get("key").asText())) {
                assertThat(s.get("value").asText()).isEqualTo("3");
                found = true;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("시나리오3: ADMIN 역할 → 학원 설정 접근 403")
    void scenario3_adminAccessDenied() throws Exception {
        String adminToken = authHelper.loginAsAdmin();

        mockMvc.perform(get("/api/admin/settings")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    @DisplayName("시나리오4: 회원 토큰 → 학원 설정 접근 403")
    void scenario4_memberAccessDenied() throws Exception {
        String[] memberResult = authHelper.loginAsMember("01099009001");
        String memberToken = memberResult[0];

        mockMvc.perform(get("/api/admin/settings")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }
}
