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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminMemberE2ETest {

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
    @DisplayName("시나리오1: 회원 검색 (이름)")
    void scenario1_searchByName() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        authHelper.loginAsMember("01099002001");

        MvcResult result = mockMvc.perform(get("/api/admin/members")
                        .param("search", "TestUser")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data.get("content")).isNotNull();
        assertThat(data.get("totalElements").asLong()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(2)
    @DisplayName("시나리오2: 회원 검색 (휴대폰 - phone_hash)")
    void scenario2_searchByPhone() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        authHelper.loginAsMember("01099002002");

        MvcResult result = mockMvc.perform(get("/api/admin/members")
                        .param("search", "01099002002")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data.get("content").size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(3)
    @DisplayName("시나리오3: 회원 상세 - 8개 도메인 통합 정보")
    void scenario3_memberDetail() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] memberResult = authHelper.loginAsMember("01099002003");
        String memberId = memberResult[1];

        MvcResult result = mockMvc.perform(get("/api/admin/members/" + memberId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data.get("name").asText()).isEqualTo("TestUser");
        assertThat(data.get("phone").asText()).contains("****");
        assertThat(data.get("memberships")).isNotNull();
        assertThat(data.get("recentReservations")).isNotNull();
        assertThat(data.get("attendanceRate")).isNotNull();
        assertThat(data.get("payments")).isNotNull();
        assertThat(data.get("memos")).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("시나리오4: 회원 메모 CRUD")
    void scenario4_memoCrud() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] memberResult = authHelper.loginAsMember("01099002004");
        String memberId = memberResult[1];

        // CREATE
        MvcResult createResult = mockMvc.perform(post("/api/admin/members/" + memberId + "/memos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"허리 주의 필요\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode memoData = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data");
        Long memoId = memoData.get("id").asLong();
        assertThat(memoData.get("content").asText()).isEqualTo("허리 주의 필요");

        // READ
        MvcResult readResult = mockMvc.perform(get("/api/admin/members/" + memberId + "/memos")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode memos = objectMapper.readTree(readResult.getResponse().getContentAsString()).get("data");
        assertThat(memos.size()).isGreaterThanOrEqualTo(1);

        // UPDATE
        mockMvc.perform(patch("/api/admin/members/" + memberId + "/memos/" + memoId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"허리 주의 + 무릎\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("허리 주의 + 무릎"));

        // DELETE
        mockMvc.perform(delete("/api/admin/members/" + memberId + "/memos/" + memoId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 삭제 후 목록에서 사라짐
        MvcResult afterDelete = mockMvc.perform(get("/api/admin/members/" + memberId + "/memos")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode memosAfter = objectMapper.readTree(afterDelete.getResponse().getContentAsString()).get("data");
        boolean found = false;
        for (JsonNode m : memosAfter) {
            if (m.get("id").asLong() == memoId) found = true;
        }
        assertThat(found).isFalse();
    }

    @Test
    @Order(5)
    @DisplayName("시나리오5: 강제 탈퇴 처리")
    void scenario5_forceWithdraw() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] memberResult = authHelper.loginAsMember("01099002005");
        String memberId = memberResult[1];

        mockMvc.perform(delete("/api/admin/members/" + memberId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 상세에서 WITHDRAWN 확인
        MvcResult result = mockMvc.perform(get("/api/admin/members/" + memberId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data.get("status").asText()).isEqualTo("WITHDRAWN");
    }

    @Test
    @Order(6)
    @DisplayName("시나리오6: 권한 분리 (회원 토큰 → 403)")
    void scenario6_memberAccessDenied() throws Exception {
        String[] memberResult = authHelper.loginAsMember("01099002006");
        String memberToken = memberResult[0];

        mockMvc.perform(get("/api/admin/members")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }
}
