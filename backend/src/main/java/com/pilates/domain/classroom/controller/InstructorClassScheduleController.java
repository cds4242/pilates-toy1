package com.pilates.domain.classroom.controller;

import com.pilates.common.response.ApiResponse;
import com.pilates.common.security.auth.LoginMember;
import com.pilates.common.security.auth.LoginMemberAnnotation;
import com.pilates.domain.classroom.dto.ClassScheduleDetailResponse;
import com.pilates.domain.classroom.dto.ClassScheduleResponse;
import com.pilates.domain.classroom.service.ClassScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 강사 본인 수업 시간표 조회 API.
 * 로그인한 강사의 담당 수업만 조회 가능.
 */
@Tag(name = "Class Schedule (Instructor)", description = "강사 본인 수업 시간표 조회")
@RestController
@RequestMapping("/api/instructor/class-schedules")
@RequiredArgsConstructor
public class InstructorClassScheduleController {

    private final ClassScheduleService classScheduleService;

    /**
     * 강사 본인 수업 목록 조회.
     * 날짜 범위 필수, 최대 60일.
     * CANCELLED 포함 (강사가 휴강 내역 확인 필요).
     */
    @Operation(summary = "강사 본인 수업 목록", description = "로그인한 강사의 담당 수업을 날짜 범위로 조회한다. CANCELLED 포함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping
    public ApiResponse<List<ClassScheduleResponse>> listMyClasses(
            @LoginMemberAnnotation LoginMember loginMember,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        // TODO: loginMember에서 instructor_id를 매핑하는 로직 필요
        //       현재 v1에서는 memberId를 instructorId로 사용 (admin auth 구현 후 개선)
        return ApiResponse.success(
                classScheduleService.listByInstructorAndDateRange(loginMember.memberId(), from, to));
    }

    /**
     * 강사 본인 수업 상세 조회.
     * 본인 담당이 아닌 수업은 CLASS_NOT_FOUND 반환.
     */
    @Operation(summary = "강사 본인 수업 상세", description = "본인 담당 수업의 상세 정보를 조회한다. 본인 수업이 아니면 404.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "수업 없음 또는 권한 없음")
    })
    @GetMapping("/{id}")
    public ApiResponse<ClassScheduleDetailResponse> getMyClassDetail(
            @LoginMemberAnnotation LoginMember loginMember,
            @PathVariable Long id) {
        return ApiResponse.success(
                classScheduleService.getDetailForInstructor(loginMember.memberId(), id));
    }
}
