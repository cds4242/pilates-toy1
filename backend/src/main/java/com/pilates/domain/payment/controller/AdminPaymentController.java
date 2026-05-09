package com.pilates.domain.payment.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.domain.payment.dto.PaymentResponse;
import com.pilates.domain.payment.dto.PaymentStatisticsResponse;
import com.pilates.domain.payment.dto.RefundRequest;
import com.pilates.domain.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 결제 관리 API (관리자용).
 */
@Tag(name = "Payment (Admin)", description = "결제 관리")
@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "결제 목록 조회", description = "전체 결제 목록을 조회한다. memberId로 필터링 가능.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ApiResponse<List<PaymentResponse>> getAllPayments(
            @RequestParam(required = false) Long memberId) {
        return ApiResponse.success(paymentService.getAllPayments(memberId));
    }

    @Operation(summary = "결제 상세 조회", description = "결제 ID로 상세 정보를 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "결제 정보 없음")
    })
    @GetMapping("/{id}")
    public ApiResponse<PaymentResponse> getPaymentDetail(@PathVariable Long id) {
        return ApiResponse.success(paymentService.getPaymentDetail(id));
    }

    @Operation(summary = "환불 처리", description = "결제를 환불 처리한다. 전액/부분 환불 모두 가능.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "환불 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "환불 불가 상태 또는 금액 초과"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "결제 정보 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "토스 환불 실패")
    })
    @PostMapping("/{id}/refund")
    public ApiResponse<Void> refundPayment(@PathVariable Long id,
                                           @Valid @RequestBody RefundRequest request) {
        paymentService.refundPayment(id, request);
        return ApiResponse.success();
    }

    @Operation(summary = "매출 통계", description = "날짜 범위의 일별 매출 통계를 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/statistics")
    public ApiResponse<List<PaymentStatisticsResponse>> getStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(paymentService.getStatistics(from, to));
    }
}
