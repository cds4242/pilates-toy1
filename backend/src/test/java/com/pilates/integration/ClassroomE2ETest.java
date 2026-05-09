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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 강사 + 수업 도메인 E2E 통합 테스트.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClassroomE2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AdminRepository adminRepository;
    @Autowired private InstructorRepository instructorRepository;

    private AuthTestHelper authHelper;

    @BeforeEach
    void setUp() {
        Set<String> smsKeys = redisTemplate.keys("sms:*");
        if (smsKeys != null) redisTemplate.delete(smsKeys);
        Set<String> authKeys = redisTemplate.keys("auth:*");
        if (authKeys != null) redisTemplate.delete(authKeys);
        authHelper = new AuthTestHelper(mockMvc, objectMapper, redisTemplate, passwordEncoder, adminRepository, instructorRepository);
    }

    // ═══════════════════════════════════════════
    // 시나리오 1: 관리자 시간표 등록 풀 플로우
    // ═══════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("시나리오1: 강사 등록 → 시간대 설정 → 수업 유형 → 고정 스케줄 → 자동 생성")
    void scenario1_fullScheduleFlow() throws Exception {
        String token = authHelper.loginAsAdmin();

        // 1. 강사 등록
        MvcResult instrResult = mockMvc.perform(post("/api/admin/instructors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"테스트강사\",\"phone\":\"010-0000-1111\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("테스트강사"))
                .andReturn();
        Long instrId = objectMapper.readTree(instrResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // 2. 근무 시간 설정 (월~금 09:00~21:00)
        String timesJson = "[" +
                "{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"09:00\",\"endTime\":\"21:00\"}," +
                "{\"dayOfWeek\":\"TUESDAY\",\"startTime\":\"09:00\",\"endTime\":\"21:00\"}," +
                "{\"dayOfWeek\":\"WEDNESDAY\",\"startTime\":\"09:00\",\"endTime\":\"21:00\"}," +
                "{\"dayOfWeek\":\"THURSDAY\",\"startTime\":\"09:00\",\"endTime\":\"21:00\"}," +
                "{\"dayOfWeek\":\"FRIDAY\",\"startTime\":\"09:00\",\"endTime\":\"21:00\"}" +
                "]";
        mockMvc.perform(put("/api/admin/instructors/" + instrId + "/available-times")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(timesJson))
                .andExpect(status().isOk());

        // 3. 수업 유형 등록
        MvcResult ltResult = mockMvc.perform(post("/api/admin/lesson-types")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"E2E그룹\",\"maxCapacity\":8,\"durationMinutes\":50,\"deductionCount\":1}"))
                .andExpect(status().isOk())
                .andReturn();
        Long lessonTypeId = objectMapper.readTree(ltResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // 4. 고정 스케줄 등록 (월요일 10:00)
        mockMvc.perform(post("/api/admin/fixed-schedules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + lessonTypeId +
                                ",\"dayOfWeek\":\"MONDAY\",\"startTime\":\"10:00\",\"endTime\":\"10:50\"}"))
                .andExpect(status().isOk());

        // 5. 수동 4주치 생성
        MvcResult genResult = mockMvc.perform(post("/api/admin/class-schedules/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weeks\":4}"))
                .andExpect(status().isOk())
                .andReturn();
        // 생성된 수업이 있는지 확인
        String genResponse = genResult.getResponse().getContentAsString();
        assertThat(genResponse).contains("success");

        // 6. 강사 근무 시간 외 고정 스케줄 → 에러
        mockMvc.perform(post("/api/admin/fixed-schedules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + lessonTypeId +
                                ",\"dayOfWeek\":\"SATURDAY\",\"startTime\":\"10:00\",\"endTime\":\"10:50\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("FIXED_003"));

        // 7. 주간 수업 조회
        LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        LocalDate fourWeeksLater = nextMonday.plusWeeks(4);
        mockMvc.perform(get("/api/admin/class-schedules")
                        .header("Authorization", "Bearer " + token)
                        .param("from", nextMonday.toString())
                        .param("to", fourWeeksLater.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ═══════════════════════════════════════════
    // 시나리오 2: 휴강 처리
    // ═══════════════════════════════════════════

    @Test
    @Order(2)
    @DisplayName("시나리오2: 단건 수업 등록 → 취소(휴강) → 조회 시 CANCELLED")
    void scenario2_cancelClass() throws Exception {
        String token = authHelper.loginAsAdmin();

        // 강사 등록
        MvcResult instrResult = mockMvc.perform(post("/api/admin/instructors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"휴강테스트강사\",\"phone\":\"010-0000-2222\"}"))
                .andExpect(status().isOk()).andReturn();
        Long instrId = objectMapper.readTree(instrResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // 수업 유형
        MvcResult ltResult = mockMvc.perform(post("/api/admin/lesson-types")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"E2E듀엣\",\"maxCapacity\":2,\"durationMinutes\":50,\"deductionCount\":1}"))
                .andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(ltResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // 단건 수업 등록 (미래 날짜)
        LocalDate futureDate = LocalDate.now().plusDays(7);
        MvcResult classResult = mockMvc.perform(post("/api/admin/class-schedules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId +
                                ",\"classDate\":\"" + futureDate + "\",\"startTime\":\"14:00\",\"endTime\":\"14:50\",\"maxCapacity\":2}"))
                .andExpect(status().isOk()).andReturn();
        Long classId = objectMapper.readTree(classResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // 휴강 처리
        mockMvc.perform(post("/api/admin/class-schedules/" + classId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 상세 조회 → CANCELLED
        mockMvc.perform(get("/api/admin/class-schedules/" + classId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    // ═══════════════════════════════════════════
    // 시나리오 3: 회원용 수업 조회
    // ═══════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("시나리오3: 회원이 공개 수업 목록 + 강사 목록 조회")
    void scenario3_memberViewClasses() throws Exception {
        // 공개 강사 목록 (인증 없이)
        mockMvc.perform(get("/api/instructors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // 공개 수업 유형 (인증 없이)
        mockMvc.perform(get("/api/lesson-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // 공개 수업 시간표 (인증 없이)
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(30);
        mockMvc.perform(get("/api/class-schedules")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ═══════════════════════════════════════════
    // 시나리오 4: 자동 생성 멱등성
    // ═══════════════════════════════════════════

    @Test
    @Order(4)
    @DisplayName("시나리오4: 자동 생성 2회 호출 → 중복 없음 (멱등성)")
    void scenario4_generateIdempotent() throws Exception {
        String token = authHelper.loginAsAdmin();

        // 강사 + 수업 유형 + 고정 스케줄
        MvcResult instrResult = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"멱등성강사\",\"phone\":\"010-0000-4444\"}")).andExpect(status().isOk()).andReturn();
        Long instrId = objectMapper.readTree(instrResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 근무 시간 (화요일)
        mockMvc.perform(put("/api/admin/instructors/" + instrId + "/available-times")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\"dayOfWeek\":\"TUESDAY\",\"startTime\":\"09:00\",\"endTime\":\"21:00\"}]")).andExpect(status().isOk());

        MvcResult ltResult = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"E2E멱등\",\"maxCapacity\":4,\"durationMinutes\":50,\"deductionCount\":1}")).andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(ltResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(post("/api/admin/fixed-schedules")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId +
                        ",\"dayOfWeek\":\"TUESDAY\",\"startTime\":\"15:00\",\"endTime\":\"15:50\"}")).andExpect(status().isOk());

        // 1차 생성
        mockMvc.perform(post("/api/admin/class-schedules/generate")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"weeks\":2}")).andExpect(status().isOk());

        // 수업 수 확인
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusWeeks(3);
        MvcResult list1 = mockMvc.perform(get("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + token)
                .param("from", from.toString())
                .param("to", to.toString())).andReturn();
        JsonNode data1 = objectMapper.readTree(list1.getResponse().getContentAsString()).get("data");

        // 2차 생성 (멱등)
        mockMvc.perform(post("/api/admin/class-schedules/generate")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"weeks\":2}")).andExpect(status().isOk());

        // 수업 수 동일해야 함
        MvcResult list2 = mockMvc.perform(get("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + token)
                .param("from", from.toString())
                .param("to", to.toString())).andReturn();
        JsonNode data2 = objectMapper.readTree(list2.getResponse().getContentAsString()).get("data");

        assertThat(data2.size()).isEqualTo(data1.size());
    }
}
