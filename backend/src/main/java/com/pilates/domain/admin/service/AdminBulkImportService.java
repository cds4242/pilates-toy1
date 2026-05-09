package com.pilates.domain.admin.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.common.security.encryption.EncryptionService;
import com.pilates.common.security.hash.HashingService;
import com.pilates.common.security.hash.PhoneNumberNormalizer;
import com.pilates.domain.admin.dto.BulkImportResponse;
import com.pilates.domain.admin.dto.BulkImportResponse.FailureDetail;
import com.pilates.domain.member.entity.Gender;
import com.pilates.domain.member.entity.Member;
import com.pilates.domain.member.entity.MemberStatus;
import com.pilates.domain.member.repository.MemberRepository;
import com.pilates.domain.membership.entity.Membership;
import com.pilates.domain.membership.entity.MembershipPass;
import com.pilates.domain.membership.entity.MembershipStatus;
import com.pilates.domain.membership.repository.MembershipRepository;
import com.pilates.domain.payment.entity.Payment;
import com.pilates.domain.payment.entity.PaymentStatus;
import com.pilates.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBulkImportService {

    private static final int MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int MAX_ROWS = 1000;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^01[016789]\\d{7,8}$");

    private final MemberRepository memberRepository;
    private final MembershipRepository membershipRepository;
    private final PaymentRepository paymentRepository;
    private final EncryptionService encryptionService;
    private final HashingService hashingService;
    private final TransactionTemplate transactionTemplate;

    public BulkImportResponse bulkImportMembers(MultipartFile file) {
        validateFile(file);

        List<String[]> rows;
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            rows = parseSheet(sheet, new String[]{"이름", "휴대폰", "메모"});
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.ADMIN_BULK_IMPORT_INVALID_FORMAT);
        }

        if (rows.size() > MAX_ROWS) {
            throw new BusinessException(ErrorCode.ADMIN_BULK_IMPORT_TOO_LARGE);
        }

        List<FailureDetail> failures = new ArrayList<>();
        int successCount = 0;

        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            int rowNum = i + 2; // 헤더가 1행이므로

            try {
                String name = row[0];
                String phone = row[1];
                String memo = row.length > 2 ? row[2] : null;

                if (name == null || name.isBlank()) {
                    failures.add(new FailureDetail(rowNum, "이름이 비어있습니다."));
                    continue;
                }

                if (phone == null || phone.isBlank()) {
                    failures.add(new FailureDetail(rowNum, "휴대폰 번호가 비어있습니다."));
                    continue;
                }

                String normalized = phone.replaceAll("[^0-9]", "");
                if (!PHONE_PATTERN.matcher(normalized).matches()) {
                    failures.add(new FailureDetail(rowNum, "휴대폰 번호 형식이 올바르지 않습니다."));
                    continue;
                }

                String phoneHash = hashingService.hash(normalized);
                if (memberRepository.existsByPhoneHashAndDeletedAtIsNull(phoneHash)) {
                    failures.add(new FailureDetail(rowNum, "이미 가입된 휴대폰 번호입니다."));
                    continue;
                }

                transactionTemplate.executeWithoutResult(status -> {
                    Member member = Member.builder()
                            .publicId(UUID.randomUUID().toString().replace("-", ""))
                            .name(encryptionService.encrypt(name))
                            .phoneEncrypted(encryptionService.encrypt(normalized))
                            .phoneHash(phoneHash)
                            .gender(Gender.FEMALE) // 기본값
                            .status(MemberStatus.ACTIVE)
                            .build();
                    memberRepository.save(member);
                });
                successCount++;

            } catch (Exception e) {
                failures.add(new FailureDetail(rowNum, "등록 실패: " + e.getMessage()));
            }
        }

        return new BulkImportResponse(successCount, failures.size(), failures);
    }

    public BulkImportResponse bulkIssueMemberships(MultipartFile file) {
        validateFile(file);

        List<String[]> rows;
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            rows = parseSheet(sheet, new String[]{"휴대폰", "정기권종류코드"});
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.ADMIN_BULK_IMPORT_INVALID_FORMAT);
        }

        if (rows.size() > MAX_ROWS) {
            throw new BusinessException(ErrorCode.ADMIN_BULK_IMPORT_TOO_LARGE);
        }

        List<FailureDetail> failures = new ArrayList<>();
        int successCount = 0;

        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            int rowNum = i + 2;

            try {
                String phone = row[0];
                String passCode = row.length > 1 ? row[1] : null;

                if (phone == null || phone.isBlank()) {
                    failures.add(new FailureDetail(rowNum, "휴대폰 번호가 비어있습니다."));
                    continue;
                }

                String normalized = phone.replaceAll("[^0-9]", "");
                String phoneHash = hashingService.hash(normalized);
                var memberOpt = memberRepository.findByPhoneHashAndDeletedAtIsNull(phoneHash);
                if (memberOpt.isEmpty()) {
                    failures.add(new FailureDetail(rowNum, "해당 회원을 찾을 수 없습니다."));
                    continue;
                }

                Member member = memberOpt.get();

                transactionTemplate.executeWithoutResult(status -> {
                    LocalDate start = LocalDate.now();
                    Membership membership = Membership.builder()
                            .publicId(UUID.randomUUID().toString().replace("-", ""))
                            .member(member)
                            .totalCount(10)
                            .remainingCount(10)
                            .unlimited(false)
                            .startDate(start)
                            .endDate(start.plusDays(30))
                            .price(BigDecimal.ZERO)
                            .status(MembershipStatus.ACTIVE)
                            .build();
                    membershipRepository.save(membership);
                });
                successCount++;

            } catch (Exception e) {
                failures.add(new FailureDetail(rowNum, "발급 실패: " + e.getMessage()));
            }
        }

        return new BulkImportResponse(successCount, failures.size(), failures);
    }

    public byte[] generateMemberTemplate() {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("회원 등록");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("이름");
            header.createCell(1).setCellValue("휴대폰");
            header.createCell(2).setCellValue("메모");

            Row example = sheet.createRow(1);
            example.createCell(0).setCellValue("홍길동");
            example.createCell(1).setCellValue("01012345678");
            example.createCell(2).setCellValue("신규 회원");

            for (int i = 0; i < 3; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public byte[] generateRevenueExcel(LocalDate from, LocalDate to) {
        List<Payment> payments = paymentRepository.findAllByPaidAtBetween(
                from.atStartOfDay(), to.atTime(LocalTime.MAX));

        List<Payment> completed = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED
                        || p.getStatus() == PaymentStatus.PARTIAL_REFUND
                        || p.getStatus() == PaymentStatus.REFUNDED)
                .sorted((a, b) -> {
                    if (a.getPaidAt() == null) return 1;
                    if (b.getPaidAt() == null) return -1;
                    return a.getPaidAt().compareTo(b.getPaidAt());
                })
                .toList();

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("매출");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("결제일");
            header.createCell(1).setCellValue("주문번호");
            header.createCell(2).setCellValue("결제수단");
            header.createCell(3).setCellValue("결제금액");
            header.createCell(4).setCellValue("환불금액");
            header.createCell(5).setCellValue("실 수령액");
            header.createCell(6).setCellValue("상태");

            int rowIdx = 1;
            for (Payment p : completed) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getPaidAt() != null
                        ? p.getPaidAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
                row.createCell(1).setCellValue(p.getOrderId());
                row.createCell(2).setCellValue(p.getMethod().name());
                row.createCell(3).setCellValue(p.getAmount().doubleValue());
                row.createCell(4).setCellValue(p.getRefundAmount() != null ? p.getRefundAmount().doubleValue() : 0);
                BigDecimal net = p.getAmount().subtract(
                        p.getRefundAmount() != null ? p.getRefundAmount() : BigDecimal.ZERO);
                row.createCell(5).setCellValue(net.doubleValue());
                row.createCell(6).setCellValue(p.getStatus().name());
            }

            for (int i = 0; i < 7; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.ADMIN_BULK_IMPORT_INVALID_FORMAT);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.ADMIN_BULK_IMPORT_TOO_LARGE);
        }
    }

    private List<String[]> parseSheet(Sheet sheet, String[] expectedHeaders) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new BusinessException(ErrorCode.ADMIN_BULK_IMPORT_INVALID_FORMAT);
        }

        // 헤더 검증
        for (int i = 0; i < expectedHeaders.length; i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null || !expectedHeaders[i].equals(getCellStringValue(cell).trim())) {
                throw new BusinessException(ErrorCode.ADMIN_BULK_IMPORT_INVALID_FORMAT);
            }
        }

        List<String[]> rows = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String[] values = new String[expectedHeaders.length];
            boolean allEmpty = true;
            for (int j = 0; j < expectedHeaders.length; j++) {
                Cell cell = row.getCell(j);
                values[j] = cell != null ? getCellStringValue(cell) : null;
                if (values[j] != null && !values[j].isBlank()) allEmpty = false;
            }
            if (!allEmpty) rows.add(values);
        }

        return rows;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }
}
