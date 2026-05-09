package com.pilates.domain.payment.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.common.security.auth.LoginMember;
import com.pilates.common.security.auth.LoginMemberAnnotation;
import com.pilates.domain.payment.dto.*;
import com.pilates.domain.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 결제 API.
 */
@Tag(name = "Payment", description = "결제 API")
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "결제 준비", description = "정기권 구매를 위한 결제를 준비하고 orderId를 발급한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "결제 준비 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "정기권 종류 없음")
    })
    @PostMapping("/api/payments/prepare")
    public ApiResponse<PrepareResponse> preparePayment(
            @LoginMemberAnnotation LoginMember loginMember,
            @Valid @RequestBody PrepareRequest request) {
        return ApiResponse.success(paymentService.preparePayment(loginMember.memberId(), request));
    }

    @Operation(summary = "결제 승인", description = "토스 결제 승인 콜백을 처리하고 정기권을 발급한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "결제 승인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "금액 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 처리된 결제"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "토스 승인 실패")
    })
    @PostMapping("/api/payments/confirm")
    public ApiResponse<ConfirmResponse> confirmPayment(@Valid @RequestBody ConfirmRequest request) {
        return ApiResponse.success(paymentService.confirmPayment(request));
    }

    @Operation(summary = "내 결제 목록 조회", description = "로그인 회원의 결제 내역을 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/api/members/me/payments")
    public ApiResponse<List<PaymentResponse>> getMyPayments(@LoginMemberAnnotation LoginMember loginMember) {
        return ApiResponse.success(paymentService.getMyPayments(loginMember.memberId()));
    }

    @Operation(summary = "내 결제 상세 조회", description = "결제 ID로 상세 정보를 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "결제 정보 없음")
    })
    @GetMapping("/api/members/me/payments/{id}")
    public ApiResponse<PaymentResponse> getMyPaymentDetail(
            @LoginMemberAnnotation LoginMember loginMember,
            @PathVariable Long id) {
        return ApiResponse.success(paymentService.getPaymentDetail(id));
    }
}
