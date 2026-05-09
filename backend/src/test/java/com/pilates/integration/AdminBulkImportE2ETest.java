package com.pilates.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pilates.domain.admin.repository.AdminRepository;
import com.pilates.domain.instructor.repository.InstructorRepository;
import com.pilates.integration.support.AuthTestHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminBulkImportE2ETest {

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

    private byte[] createMemberExcel(String[][] data) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("회원 등록");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("이름");
            header.createCell(1).setCellValue("휴대폰");
            header.createCell(2).setCellValue("메모");

            for (int i = 0; i < data.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int j = 0; j < data[i].length; j++) {
                    row.createCell(j).setCellValue(data[i][j]);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    @Test
    @Order(1)
    @DisplayName("시나리오1: 엑셀 회원 일괄 등록 (정상)")
    void scenario1_bulkImportSuccess() throws Exception {
        String adminToken = authHelper.loginAsAdmin();

        byte[] excelBytes = createMemberExcel(new String[][]{
                {"김철수", "01088001001", "신규"},
                {"이영희", "01088001002", "체험"},
                {"박지수", "01088001003", ""}
        });

        MockMultipartFile file = new MockMultipartFile("file", "members.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelBytes);

        MvcResult result = mockMvc.perform(multipart("/api/admin/members/bulk")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data.get("successCount").asInt()).isEqualTo(3);
        assertThat(data.get("failureCount").asInt()).isEqualTo(0);
    }

    @Test
    @Order(2)
    @DisplayName("시나리오2: 엑셀 부분 실패 (중복 휴대폰)")
    void scenario2_partialFailure() throws Exception {
        String adminToken = authHelper.loginAsAdmin();

        // 먼저 한 명 등록
        authHelper.loginAsMember("01088002001");

        byte[] excelBytes = createMemberExcel(new String[][]{
                {"홍길동", "01088002001", "중복 번호"},  // 중복
                {"장보고", "01088002002", "신규"}         // 정상
        });

        MockMultipartFile file = new MockMultipartFile("file", "members.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelBytes);

        MvcResult result = mockMvc.perform(multipart("/api/admin/members/bulk")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(data.get("successCount").asInt()).isEqualTo(1);
        assertThat(data.get("failureCount").asInt()).isEqualTo(1);
        assertThat(data.get("failures").get(0).get("reason").asText()).contains("이미 가입");
    }

    @Test
    @Order(3)
    @DisplayName("시나리오3: 엑셀 형식 오류 (헤더 누락)")
    void scenario3_invalidFormat() throws Exception {
        String adminToken = authHelper.loginAsAdmin();

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("잘못된");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("이름없음");  // 잘못된 헤더

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);

            MockMultipartFile file = new MockMultipartFile("file", "bad.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());

            mockMvc.perform(multipart("/api/admin/members/bulk")
                            .file(file)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("ADMIN_011"));
        }
    }

    @Test
    @Order(4)
    @DisplayName("시나리오4: 매출 엑셀 다운로드")
    void scenario4_revenueExcelDownload() throws Exception {
        String adminToken = authHelper.loginAsAdmin();
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();

        MvcResult result = mockMvc.perform(get("/api/admin/statistics/revenue/excel")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentType())
                .contains("spreadsheetml");
        assertThat(result.getResponse().getContentAsByteArray().length).isGreaterThan(0);
    }

    @Test
    @Order(5)
    @DisplayName("시나리오5: 권한 분리 (회원 → 403)")
    void scenario5_memberAccessDenied() throws Exception {
        String[] memberResult = authHelper.loginAsMember("01088005001");
        String memberToken = memberResult[0];

        byte[] excelBytes = createMemberExcel(new String[][]{{"테스트", "01011111111", ""}});
        MockMultipartFile file = new MockMultipartFile("file", "m.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelBytes);

        mockMvc.perform(multipart("/api/admin/members/bulk")
                        .file(file)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }
}
