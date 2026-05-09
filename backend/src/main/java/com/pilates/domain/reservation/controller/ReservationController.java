package com.pilates.domain.reservation.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.common.security.auth.LoginMember;
import com.pilates.common.security.auth.LoginMemberAnnotation;
import com.pilates.domain.reservation.dto.ReservationCreateRequest;
import com.pilates.domain.reservation.dto.ReservationResponse;
import com.pilates.domain.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 예약 API.
 * 회원 인증 필요 (JWT Access Token).
 */
@Tag(name = "Reservation", description = "예약 API")
@RestController
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 예약 생성.
     */
    @Operation(summary = "예약 생성", description = "수업을 예약한다. 정기권 자동 차감.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "예약 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "예약 불가 (수업 상태/정기권 없음/월 한도 초과)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "중복 예약/정원 초과")
    })
    @PostMapping("/api/reservations")
    public ApiResponse<ReservationResponse> createReservation(
            @LoginMemberAnnotation LoginMember loginMember,
            @Valid @RequestBody ReservationCreateRequest request) {
        return ApiResponse.success(
                reservationService.createReservation(loginMember.memberId(), request));
    }

    /**
     * 예약 취소.
     */
    @Operation(summary = "예약 취소", description = "본인 예약을 취소한다. 수업 시작 2시간 전까지 가능. 정기권 자동 복구.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "취소 불가 (시간 초과/이미 취소)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 예약 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "예약 없음")
    })
    @DeleteMapping("/api/reservations/{id}")
    public ApiResponse<Void> cancelReservation(
            @LoginMemberAnnotation LoginMember loginMember,
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        reservationService.cancelReservation(loginMember.memberId(), id, reason);
        return ApiResponse.success();
    }

    /**
     * 내 예약 목록 조회.
     */
    @Operation(summary = "내 예약 목록 조회", description = "로그인한 회원의 예약 목록을 최신순으로 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/api/members/me/reservations")
    public ApiResponse<List<ReservationResponse>> getMyReservations(
            @LoginMemberAnnotation LoginMember loginMember) {
        return ApiResponse.success(
                reservationService.getMyReservations(loginMember.memberId()));
    }

    /**
     * 내 예약 상세 조회.
     */
    @Operation(summary = "내 예약 상세 조회", description = "예약 ID로 상세 정보를 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "예약 없음")
    })
    @GetMapping("/api/members/me/reservations/{id}")
    public ApiResponse<ReservationResponse> getReservationDetail(
            @LoginMemberAnnotation LoginMember loginMember,
            @PathVariable Long id) {
        return ApiResponse.success(reservationService.getReservationDetail(id));
    }
}
