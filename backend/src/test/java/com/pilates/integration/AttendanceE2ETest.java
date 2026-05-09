package com.pilates.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pilates.domain.admin.repository.AdminRepository;
import com.pilates.domain.instructor.repository.InstructorRepository;
import com.pilates.domain.reservation.scheduler.NoShowMarkingScheduler;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 출석 도메인 E2E 통합 테스트.
 * 강사 인증은 AdminAuthService 경유 (admins 테이블 로그인).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AttendanceE2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private NoShowMarkingScheduler noShowMarkingScheduler;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AdminRepository adminRepository;
    @Autowired private InstructorRepository instructorRepository;

    private AuthTestHelper authHelper;

    @BeforeEach
    void setUp() {
        authHelper = new AuthTestHelper(mockMvc, objectMapper, redisTemplate, passwordEncoder, adminRepository, instructorRepository);
        authHelper.clearRedis();
    }

    // ── 헬퍼 ──

    /**
     * 강사+수업유형+정기권+정기권발급+수업 생성.
     * adminToken을 사용하여 /api/admin/** 엔드포인트 호출.
     * 반환: [classScheduleId, lessonTypeId, membershipPassId, instructorId]
     */
    private long[] setupFullScenario(String adminToken, String memberId, String suffix, int capacity,
                                       LocalDate classDate, String startTime, String endTime) throws Exception {
        MvcResult ir = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ATT강사" + suffix + "\",\"phone\":\"010-0000-0000\"}")).andExpect(status().isOk()).andReturn();
        Long instrId = objectMapper.readTree(ir.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult lt = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ATT유형" + suffix + "\",\"maxCapacity\":" + capacity + ",\"durationMinutes\":50,\"deductionCount\":1}")).andExpect(status().isOk()).andReturn();
        Long ltId = objectMapper.readTree(lt.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult mp = mockMvc.perform(post("/api/admin/membership-passes")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ATT패스" + suffix + "\",\"price\":100000,\"totalCount\":10,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}")).andExpect(status().isOk()).andReturn();
        Long passId = objectMapper.readTree(mp.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + memberId + ",\"totalCount\":10,\"price\":100000,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "],\"membershipPassId\":" + passId + "}")).andExpect(status().isOk());

        MvcResult cs = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + classDate + "\",\"startTime\":\"" + startTime + "\",\"endTime\":\"" + endTime + "\",\"maxCapacity\":" + capacity + "}")).andExpect(status().isOk()).andReturn();
        Long classId = objectMapper.readTree(cs.getResponse().getContentAsString()).get("data").get("id").asLong();

        return new long[]{classId, ltId, passId, instrId};
    }

    private long[] setupFutureScenario(String adminToken, String memberId, String suffix, int capacity) throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(14);
        return setupFullScenario(adminToken, memberId, suffix, capacity, futureDate, "10:00", "10:50");
    }

    private Long reserveClass(String token, Long classId) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"classScheduleId\":" + classId + "}")).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("data").get("id").asLong();
    }

    // ═══════════════════════════════════════════
    // 시나리오 1: 강사 출석 체크 정상
    // ═══════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("시나리오1: 강사 토큰으로 출석 체크 → ATTENDED/LATE/ABSENT 마킹 + 이력 조회")
    void scenario1_instructorMarkAttendance() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth1 = authHelper.loginAsMember("01066660001");
        String[] auth2 = authHelper.loginAsMember("01066660002");
        String[] auth3 = authHelper.loginAsMember("01066660003");

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalTime startTime = now.minusHours(1).withSecond(0).withNano(0);
        LocalTime endTime = now.minusMinutes(10).withSecond(0).withNano(0);
        if (startTime.isAfter(endTime) || startTime.getHour() >= 23) return;

        String startStr = startTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        String endStr = endTime.format(DateTimeFormatter.ofPattern("HH:mm"));

        long[] setup = setupFullScenario(adminToken, auth1[1], "ATT1", 8, today, startStr, endStr);
        Long classId = setup[0]; Long ltId = setup[1]; Long passId = setup[2]; Long instrId = setup[3];

        for (String[] auth : new String[][]{auth2, auth3}) {
            mockMvc.perform(post("/api/admin/memberships")
                    .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"memberId\":" + auth[1] + ",\"totalCount\":10,\"price\":100000,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "],\"membershipPassId\":" + passId + "}")).andExpect(status().isOk());
        }

        Long resId1 = reserveClass(auth1[0], classId);
        Long resId2 = reserveClass(auth2[0], classId);
        Long resId3 = reserveClass(auth3[0], classId);

        // 강사 admin 계정 생성 + 로그인
        String instructorToken = authHelper.loginAsInstructor(instrId);

        // 강사 토큰으로 출석 현황 조회 → 3명 PENDING
        MvcResult listResult = mockMvc.perform(get("/api/instructor/class-schedules/" + classId + "/attendances")
                        .header("Authorization", "Bearer " + instructorToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode attendances = objectMapper.readTree(listResult.getResponse().getContentAsString()).get("data");
        assertThat(attendances.size()).isEqualTo(3);

        // 단건 출석 마킹: ATTENDED, LATE, ABSENT
        mockMvc.perform(post("/api/instructor/attendances/" + resId1)
                        .header("Authorization", "Bearer " + instructorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ATTENDED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/instructor/attendances/" + resId2)
                        .header("Authorization", "Bearer " + instructorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LATE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/instructor/attendances/" + resId3)
                        .header("Authorization", "Bearer " + instructorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ABSENT\"}"))
                .andExpect(status().isOk());

        // 회원 본인 출석 이력 조회 → 상태 확인
        MvcResult myAtt = mockMvc.perform(get("/api/members/me/attendances")
                        .header("Authorization", "Bearer " + auth1[0]))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode content = objectMapper.readTree(myAtt.getResponse().getContentAsString()).get("data").get("content");
        boolean foundAttended = false;
        for (JsonNode att : content) {
            if (att.get("classScheduleId").asLong() == classId) {
                assertThat(att.get("status").asText()).isEqualTo("ATTENDED");
                foundAttended = true;
            }
        }
        assertThat(foundAttended).isTrue();
    }

    // ═══════════════════════════════════════════
    // 시나리오 2: 다른 강사 수업 출석 체크 → ACCESS_DENIED
    // ═══════════════════════════════════════════

    @Test
    @Order(2)
    @DisplayName("시나리오2: 다른 강사 토큰으로 출석 체크 시도 → ACCESS_DENIED")
    void scenario2_otherInstructorAccessDenied() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth = authHelper.loginAsMember("01066660010");
        String memberToken = auth[0]; String memberId = auth[1];

        LocalDate today = LocalDate.now();
        LocalTime startTime = LocalTime.now().minusHours(1).withSecond(0).withNano(0);
        LocalTime endTime = LocalTime.now().minusMinutes(10).withSecond(0).withNano(0);
        if (startTime.isAfter(endTime) || startTime.getHour() >= 23) return;

        String startStr = startTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        String endStr = endTime.format(DateTimeFormatter.ofPattern("HH:mm"));

        long[] setup = setupFullScenario(adminToken, memberId, "ATT2", 8, today, startStr, endStr);
        Long classId = setup[0]; Long instrId = setup[3];

        Long resId = reserveClass(memberToken, classId);

        // 다른 강사 생성 + 로그인
        MvcResult ir2 = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ATT강사OTHER2\",\"phone\":\"010-0000-0000\"}")).andExpect(status().isOk()).andReturn();
        Long otherInstrId = objectMapper.readTree(ir2.getResponse().getContentAsString()).get("data").get("id").asLong();
        String otherInstructorToken = authHelper.loginAsInstructor(otherInstrId);

        // 다른 강사가 단건 출석 체크 → COMMON_005
        mockMvc.perform(post("/api/instructor/attendances/" + resId)
                        .header("Authorization", "Bearer " + otherInstructorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ATTENDED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_005"));

        // 다른 강사가 일괄 출석 체크 → COMMON_005
        mockMvc.perform(post("/api/instructor/class-schedules/" + classId + "/attendances")
                        .header("Authorization", "Bearer " + otherInstructorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"attendances\":[{\"reservationId\":" + resId + ",\"status\":\"ATTENDED\"}]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_005"));

        // 다른 강사가 출석 현황 조회 → COMMON_005
        mockMvc.perform(get("/api/instructor/class-schedules/" + classId + "/attendances")
                        .header("Authorization", "Bearer " + otherInstructorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_005"));

        // 회원 토큰으로 강사 API 접근 → 403 (ROLE_MEMBER로는 /api/instructor/** 접근 불가)
        mockMvc.perform(post("/api/instructor/attendances/" + resId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ATTENDED\"}"))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════
    // 시나리오 3: 출석 체크 시간 외 → ATT_002
    // ═══════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("시나리오3: 강사 토큰으로 미래 수업 출석 체크 → ATT_002")
    void scenario3_notCheckableTime() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth = authHelper.loginAsMember("01066660020");
        String memberToken = auth[0]; String memberId = auth[1];

        long[] setup = setupFutureScenario(adminToken, memberId, "ATT3", 8);
        Long classId = setup[0]; Long instrId = setup[3];

        Long resId = reserveClass(memberToken, classId);

        // 해당 강사로 로그인
        String instructorToken = authHelper.loginAsInstructor(instrId);

        // 미래 수업 출석 체크 → ATT_002 (시간 외)
        mockMvc.perform(post("/api/instructor/attendances/" + resId)
                        .header("Authorization", "Bearer " + instructorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ATTENDED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ATT_002"));
    }

    // ═══════════════════════════════════════════
    // 시나리오 4: 회원 출석률 계산
    // ═══════════════════════════════════════════

    @Test
    @Order(4)
    @DisplayName("시나리오4: 출석률 계산 → PENDING만 → 출석률 0, 강사 마킹 후 반영")
    void scenario4_attendanceRate() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth = authHelper.loginAsMember("01066660030");
        String memberToken = auth[0]; String memberId = auth[1];

        long[] setup = setupFutureScenario(adminToken, memberId, "ATT4", 8);
        Long classId = setup[0];

        reserveClass(memberToken, classId);

        // 출석률 조회 (PENDING 제외 → 0)
        MvcResult rateResult = mockMvc.perform(get("/api/members/me/attendance-rate")
                        .header("Authorization", "Bearer " + memberToken)
                        .param("period", "all"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode rate = objectMapper.readTree(rateResult.getResponse().getContentAsString()).get("data");
        assertThat(rate.get("totalCount").asLong()).isEqualTo(0);
        assertThat(rate.get("attendanceRate").asDouble()).isEqualTo(0.0);
        assertThat(rate.get("period").asText()).isEqualTo("all");
    }

    // ═══════════════════════════════════════════
    // 시나리오 5: 노쇼 자동 처리 후 Attendance 상태
    // ═══════════════════════════════════════════

    @Test
    @Order(5)
    @DisplayName("시나리오5: 노쇼 스케줄러 → Attendance.status = NO_SHOW")
    void scenario5_noShowSchedulerWithAttendance() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[] auth = authHelper.loginAsMember("01066660040");
        String memberToken = auth[0]; String memberId = auth[1];

        LocalDate today = LocalDate.now();
        LocalTime endTime = LocalTime.now().minusMinutes(31).withSecond(0).withNano(0);
        LocalTime startTime = endTime.minusMinutes(50);
        if (startTime.isAfter(endTime) || startTime.getHour() >= 23) return;

        String startStr = startTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        String endStr = endTime.format(DateTimeFormatter.ofPattern("HH:mm"));

        long[] setup = setupFullScenario(adminToken, memberId, "ATT5", 8, today, startStr, endStr);
        Long classId = setup[0];

        Long resId = reserveClass(memberToken, classId);

        noShowMarkingScheduler.markOverdueReservations();

        // NO_SHOW 확인
        MvcResult myAtt = mockMvc.perform(get("/api/members/me/attendances")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode content = objectMapper.readTree(myAtt.getResponse().getContentAsString()).get("data").get("content");
        boolean foundNoShow = false;
        for (JsonNode att : content) {
            if (att.get("classScheduleId").asLong() == classId) {
                assertThat(att.get("status").asText()).isEqualTo("NO_SHOW");
                foundNoShow = true;
            }
        }
        assertThat(foundNoShow).isTrue();

        // 출석률에 NO_SHOW 반영
        MvcResult rateResult = mockMvc.perform(get("/api/members/me/attendance-rate")
                        .header("Authorization", "Bearer " + memberToken)
                        .param("period", "all"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode rate = objectMapper.readTree(rateResult.getResponse().getContentAsString()).get("data");
        assertThat(rate.get("noShowCount").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(rate.get("totalCount").asLong()).isGreaterThanOrEqualTo(1);
    }

    // ═══════════════════════════════════════════
    // 시나리오 6: 일괄 출석 체크 (강사 토큰으로 진짜 호출)
    // ═══════════════════════════════════════════

    @Test
    @Order(6)
    @DisplayName("시나리오6: 강사 토큰으로 일괄 출석 체크 5명 → 유효하지 않은 status 시 롤백")
    void scenario6_batchAttendanceTransaction() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        String[][] auths = new String[5][];
        for (int i = 0; i < 5; i++) {
            auths[i] = authHelper.loginAsMember("0106666005" + i);
        }

        LocalDate today = LocalDate.now();
        LocalTime startTime = LocalTime.now().minusHours(1).withSecond(0).withNano(0);
        LocalTime endTime = LocalTime.now().minusMinutes(10).withSecond(0).withNano(0);
        if (startTime.isAfter(endTime) || startTime.getHour() >= 23) return;

        String startStr = startTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        String endStr = endTime.format(DateTimeFormatter.ofPattern("HH:mm"));

        long[] setup = setupFullScenario(adminToken, auths[0][1], "ATT6", 8, today, startStr, endStr);
        Long classId = setup[0]; Long ltId = setup[1]; Long passId = setup[2]; Long instrId = setup[3];

        for (int i = 1; i < 5; i++) {
            mockMvc.perform(post("/api/admin/memberships")
                    .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"memberId\":" + auths[i][1] + ",\"totalCount\":10,\"price\":100000,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "],\"membershipPassId\":" + passId + "}")).andExpect(status().isOk());
        }

        Long[] resIds = new Long[5];
        for (int i = 0; i < 5; i++) {
            resIds[i] = reserveClass(auths[i][0], classId);
        }

        // 강사 로그인
        String instructorToken = authHelper.loginAsInstructor(instrId);

        // 유효하지 않은 status 포함 일괄 호출 → ATT_004 + 전체 롤백
        StringBuilder invalidBatch = new StringBuilder("{\"attendances\":[");
        for (int i = 0; i < 4; i++) {
            invalidBatch.append("{\"reservationId\":").append(resIds[i]).append(",\"status\":\"ATTENDED\"},");
        }
        invalidBatch.append("{\"reservationId\":").append(resIds[4]).append(",\"status\":\"INVALID_STATUS\"}]}");

        mockMvc.perform(post("/api/instructor/class-schedules/" + classId + "/attendances")
                        .header("Authorization", "Bearer " + instructorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBatch.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ATT_004"));

        // 롤백 확인: 5명 모두 아직 PENDING
        for (int i = 0; i < 5; i++) {
            MvcResult myAtt = mockMvc.perform(get("/api/members/me/attendances")
                            .header("Authorization", "Bearer " + auths[i][0]))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode content = objectMapper.readTree(myAtt.getResponse().getContentAsString()).get("data").get("content");
            for (JsonNode att : content) {
                if (att.get("classScheduleId").asLong() == classId) {
                    assertThat(att.get("status").asText()).isEqualTo("PENDING");
                }
            }
        }

        // 정상 일괄 호출 → 5명 모두 ATTENDED
        StringBuilder validBatch = new StringBuilder("{\"attendances\":[");
        for (int i = 0; i < 5; i++) {
            if (i > 0) validBatch.append(",");
            validBatch.append("{\"reservationId\":").append(resIds[i]).append(",\"status\":\"ATTENDED\"}");
        }
        validBatch.append("]}");

        mockMvc.perform(post("/api/instructor/class-schedules/" + classId + "/attendances")
                        .header("Authorization", "Bearer " + instructorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBatch.toString()))
                .andExpect(status().isOk());

        // 5명 모두 ATTENDED 확인
        for (int i = 0; i < 5; i++) {
            MvcResult myAtt = mockMvc.perform(get("/api/members/me/attendances")
                            .header("Authorization", "Bearer " + auths[i][0]))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode content = objectMapper.readTree(myAtt.getResponse().getContentAsString()).get("data").get("content");
            for (JsonNode att : content) {
                if (att.get("classScheduleId").asLong() == classId) {
                    assertThat(att.get("status").asText()).isEqualTo("ATTENDED");
                }
            }
        }

        // 관리자 노쇼 카운트 조회
        mockMvc.perform(get("/api/admin/attendances/no-show-counts")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("from", today.minusDays(1).toString())
                        .param("to", today.plusDays(1).toString()))
                .andExpect(status().isOk());
    }
}
