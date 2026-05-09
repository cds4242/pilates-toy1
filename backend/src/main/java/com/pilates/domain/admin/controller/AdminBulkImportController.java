package com.pilates.domain.admin.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.domain.admin.dto.BulkImportResponse;
import com.pilates.domain.admin.service.AdminBulkImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Tag(name = "Admin Bulk Import", description = "관리자 엑셀 일괄 처리 API")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminBulkImportController {

    private final AdminBulkImportService bulkImportService;

    @Operation(summary = "회원 일괄 등록", description = "엑셀 파일로 회원 일괄 등록 (부분 성공)")
    @PostMapping(value = "/members/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<BulkImportResponse> bulkImportMembers(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(bulkImportService.bulkImportMembers(file));
    }

    @Operation(summary = "회원 등록 엑셀 템플릿 다운로드")
    @GetMapping("/members/bulk/template")
    public ResponseEntity<byte[]> downloadMemberTemplate() {
        byte[] content = bulkImportService.generateMemberTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=member_template.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @Operation(summary = "정기권 일괄 발급", description = "엑셀 파일로 정기권 일괄 발급")
    @PostMapping(value = "/memberships/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<BulkImportResponse> bulkIssueMemberships(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(bulkImportService.bulkIssueMemberships(file));
    }

    @Operation(summary = "매출 엑셀 다운로드")
    @GetMapping("/statistics/revenue/excel")
    public ResponseEntity<byte[]> downloadRevenueExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] content = bulkImportService.generateRevenueExcel(from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=revenue_" + from + "_" + to + ".xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }
}
