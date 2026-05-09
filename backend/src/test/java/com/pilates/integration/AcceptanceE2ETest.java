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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 의뢰인 인수 시나리오.
 * 실제 운영 시나리오를 그대로 재현하여 전체 플로우를 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AcceptanceE2ETest {

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

    // ── 헬퍼 ──

    private Long createInstructor(String adminToken, String name, String phone) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/admin/instructors")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"phone\":\"" + phone + "\"}")).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("data").get("id").asLong();
    }

    private Long createLessonType(String adminToken, String name, int capacity) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/admin/lesson-types")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"maxCapacity\":" + capacity + ",\"durationMinutes\":50,\"deductionCount\":1}")).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("data").get("id").asLong();
    }

    private Long createMembershipPass(String adminToken, String name, int price, int count, Long ltId) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/admin/membership-passes")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"price\":" + price + ",\"totalCount\":" + count + ",\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "]}")).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("data").get("id").asLong();
    }

    private Long createClassSchedule(String adminToken, Long instrId, Long ltId, LocalDate date, String start, String end, int capacity) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/admin/class-schedules")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"instructorId\":" + instrId + ",\"lessonTypeId\":" + ltId + ",\"classDate\":\"" + date + "\",\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\",\"maxCapacity\":" + capacity + "}")).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("data").get("id").asLong();
    }

    @Test
    @Order(1)
    @DisplayName("시나리오1: 신규 회원 가입 → 결제 → 정기권 → 예약 풀 플로우")
    void scenario1_memberFullFlow() throws Exception {
        String adminToken = authHelper.loginAsAdmin();

        // ── 1. 회원가입 ──
        String[] member = authHelper.loginAsMember("01070001001");
        String memberToken = member[0];
        String memberId = member[1];

        // ── 2. 관리자가 강사·수업유형·정기권종류 생성 ──
        Long instrId = createInstructor(adminToken, "인수강사A", "010-7700-0001");
        Long ltId = createLessonType(adminToken, "인수그룹A", 8);
        Long passId = createMembershipPass(adminToken, "인수12회권", 250000, 12, ltId);

        // ── 3. 정기권 종류 조회 (회원) ──
        mockMvc.perform(get("/api/membership-passes")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());

        // ── 4. 결제 (Mock 토스) ──
        MvcResult prepResult = mockMvc.perform(post("/api/payments/prepare")
                        .header("Authorization", "Bearer " + memberToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"membershipPassId\":" + passId + "}"))
                .andExpect(status().isOk())
                .andReturn();
        String orderId = objectMapper.readTree(prepResult.getResponse().getContentAsString()).get("data").get("orderId").asText();
        int amount = objectMapper.readTree(prepResult.getResponse().getContentAsString()).get("data").get("amount").asInt();
        assertThat(amount).isEqualTo(250000);

        // ── 5. 결제 승인 (Mock) ──
        mockMvc.perform(post("/api/payments/confirm").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentKey\":\"mock_pk_" + orderId + "\",\"orderId\":\"" + orderId + "\",\"amount\":" + amount + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // ── 6. 정기권 발급 확인 (잔여 12) ──
        MvcResult msResult = mockMvc.perform(get("/api/members/me/memberships")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode memberships = objectMapper.readTree(msResult.getResponse().getContentAsString()).get("data");
        boolean found12 = false;
        for (JsonNode ms : memberships) {
            if (ms.get("remainingCount").asInt() == 12) {
                found12 = true;
                break;
            }
        }
        assertThat(found12).as("12회권 발급 확인").isTrue();

        // ── 7. 수업 생성 + 시간표 조회 ──
        LocalDate futureDate = LocalDate.now().plusDays(3);
        Long classId = createClassSchedule(adminToken, instrId, ltId, futureDate, "14:00", "14:50", 8);

        MvcResult scheduleResult = mockMvc.perform(get("/api/class-schedules")
                        .param("from", futureDate.toString()).param("to", futureDate.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode schedules = objectMapper.readTree(scheduleResult.getResponse().getContentAsString()).get("data");
        assertThat(schedules.size()).isGreaterThanOrEqualTo(1);

        // ── 8. 예약 ──
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + memberToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classScheduleId\":" + classId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        // ── 9. 정기권 잔여 -1 (11) ──
        MvcResult afterReserve = mockMvc.perform(get("/api/members/me/memberships")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode afterMs = objectMapper.readTree(afterReserve.getResponse().getContentAsString()).get("data");
        boolean found11 = false;
        for (JsonNode ms : afterMs) {
            if (ms.get("remainingCount").asInt() == 11) {
                found11 = true;
                break;
            }
        }
        assertThat(found11).as("예약 후 잔여 11").isTrue();

        // ── 10. 본인 예약 이력 ──
        mockMvc.perform(get("/api/members/me/reservations")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    @Order(2)
    @DisplayName("시나리오2: 강사 일과 (수업 조회 → 출석 체크)")
    void scenario2_instructorDailyWork() throws Exception {
        String adminToken = authHelper.loginAsAdmin();

        // 강사 생성
        Long instrId = createInstructor(adminToken, "인수강사B", "010-7700-0002");
        Long ltId = createLessonType(adminToken, "인수그룹B", 4);
        Long passId = createMembershipPass(adminToken, "인수B패스", 100000, 10, ltId);

        // 회원 3명 가입 + 정기권 + 예약
        // 수업 시간: 이미 종료된 수업 (종료+30분 이내 → 출석 체크 가능)
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        LocalTime classStart = now.minusHours(1);
        LocalTime classEnd = now.minusMinutes(10);
        // 자정 부근 edge case 방어
        if (classStart.isAfter(classEnd) || classStart.getHour() >= 23) {
            classStart = LocalTime.of(9, 0);
            classEnd = now.minusMinutes(5);
            if (classStart.isAfter(classEnd)) classEnd = LocalTime.of(10, 0);
        }
        String startTime = classStart.format(DateTimeFormatter.ofPattern("HH:mm"));
        String endTime = classEnd.format(DateTimeFormatter.ofPattern("HH:mm"));
        Long classId = createClassSchedule(adminToken, instrId, ltId, today, startTime, endTime, 4);

        Long[] resIds = new Long[3];
        for (int i = 0; i < 3; i++) {
            String phone = "0107000200" + i;
            String[] m = authHelper.loginAsMember(phone);
            // 정기권 발급
            mockMvc.perform(post("/api/admin/memberships")
                    .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"memberId\":" + m[1] + ",\"totalCount\":10,\"price\":100000,\"validityDays\":90,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "],\"membershipPassId\":" + passId + "}")).andExpect(status().isOk());
            // 예약
            MvcResult rr = mockMvc.perform(post("/api/reservations")
                    .header("Authorization", "Bearer " + m[0]).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"classScheduleId\":" + classId + "}")).andExpect(status().isOk()).andReturn();
            resIds[i] = objectMapper.readTree(rr.getResponse().getContentAsString()).get("data").get("id").asLong();
        }

        // 강사 로그인
        String instructorToken = authHelper.loginAsInstructor(instrId);

        // 수업 상세 (예약자 리스트)
        MvcResult attList = mockMvc.perform(get("/api/instructor/class-schedules/" + classId + "/attendances")
                        .header("Authorization", "Bearer " + instructorToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode attendances = objectMapper.readTree(attList.getResponse().getContentAsString()).get("data");
        assertThat(attendances.size()).isEqualTo(3);

        // 출석 체크: 2명 ATTENDED, 1명 LATE
        mockMvc.perform(post("/api/instructor/attendances/" + resIds[0])
                .header("Authorization", "Bearer " + instructorToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ATTENDED\"}")).andExpect(status().isOk());
        mockMvc.perform(post("/api/instructor/attendances/" + resIds[1])
                .header("Authorization", "Bearer " + instructorToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ATTENDED\"}")).andExpect(status().isOk());
        mockMvc.perform(post("/api/instructor/attendances/" + resIds[2])
                .header("Authorization", "Bearer " + instructorToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"LATE\"}")).andExpect(status().isOk());

        // 출석 이력 재확인
        MvcResult afterMark = mockMvc.perform(get("/api/instructor/class-schedules/" + classId + "/attendances")
                        .header("Authorization", "Bearer " + instructorToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode afterAtt = objectMapper.readTree(afterMark.getResponse().getContentAsString()).get("data");
        long attendedCount = 0;
        long lateCount = 0;
        for (JsonNode a : afterAtt) {
            if ("ATTENDED".equals(a.get("status").asText())) attendedCount++;
            if ("LATE".equals(a.get("status").asText())) lateCount++;
        }
        assertThat(attendedCount).isEqualTo(2);
        assertThat(lateCount).isEqualTo(1);
    }

    @Test
    @Order(3)
    @DisplayName("시나리오3: 관리자 일과 (대시보드 → 회원 관리 → 통계)")
    void scenario3_adminDailyWork() throws Exception {
        String adminToken = authHelper.loginAsAdmin();

        // 시드: 회원 + 정기권 + 수업 생성
        String[] member = authHelper.loginAsMember("01070003001");
        Long instrId = createInstructor(adminToken, "인수강사C", "010-7700-0003");
        Long ltId = createLessonType(adminToken, "인수그룹C", 4);
        Long passId = createMembershipPass(adminToken, "인수C패스", 180000, 8, ltId);

        mockMvc.perform(post("/api/admin/memberships")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":" + member[1] + ",\"totalCount\":8,\"price\":180000,\"validityDays\":60,\"unlimited\":false,\"lessonTypeIds\":[" + ltId + "],\"membershipPassId\":" + passId + "}")).andExpect(status().isOk());

        // ── 1. 대시보드 4영역 ──
        MvcResult dashResult = mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode dash = objectMapper.readTree(dashResult.getResponse().getContentAsString()).get("data");
        assertThat(dash.get("todayClasses")).isNotNull();
        assertThat(dash.get("thisWeekRevenue")).isNotNull();
        assertThat(dash.get("expiringMemberships")).isNotNull();
        assertThat(dash.get("alerts")).isNotNull();

        // ── 2. 회원 검색 (전화번호) ──
        MvcResult searchResult = mockMvc.perform(get("/api/admin/members")
                        .param("search", "01070003001")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode searchData = objectMapper.readTree(searchResult.getResponse().getContentAsString()).get("data");
        assertThat(searchData.get("totalElements").asLong()).isGreaterThanOrEqualTo(1);

        // ── 3. 회원 상세 (8개 도메인 통합) ──
        MvcResult detailResult = mockMvc.perform(get("/api/admin/members/" + member[1])
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode detail = objectMapper.readTree(detailResult.getResponse().getContentAsString()).get("data");
        assertThat(detail.get("name").asText()).isNotEmpty();
        assertThat(detail.get("memberships")).isNotNull();
        assertThat(detail.get("recentReservations")).isNotNull();
        assertThat(detail.get("attendanceRate")).isNotNull();
        assertThat(detail.get("payments")).isNotNull();

        // ── 4. 매출 통계 ──
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();
        mockMvc.perform(get("/api/admin/statistics/revenue")
                        .param("from", from.toString()).param("to", to.toString()).param("groupBy", "daily")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").exists());

        // ── 5. 매출 엑셀 다운로드 ──
        MvcResult excelResult = mockMvc.perform(get("/api/admin/statistics/revenue/excel")
                        .param("from", from.toString()).param("to", to.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(excelResult.getResponse().getContentType()).contains("spreadsheetml");
        assertThat(excelResult.getResponse().getContentAsByteArray().length).isGreaterThan(0);
    }
}
