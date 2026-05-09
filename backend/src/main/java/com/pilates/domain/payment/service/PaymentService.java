package com.pilates.domain.payment.service;

import com.pilates.common.error.BusinessException;
import com.pilates.common.error.ErrorCode;
import com.pilates.common.security.encryption.EncryptionService;
import com.pilates.common.tosspayments.TossConfirmResponse;
import com.pilates.common.tosspayments.TossPaymentClient;
import com.pilates.domain.member.entity.Member;
import com.pilates.domain.member.repository.MemberRepository;
import com.pilates.domain.membership.dto.MembershipIssueRequest;
import com.pilates.domain.membership.dto.MembershipResponse;
import com.pilates.domain.membership.entity.Membership;
import com.pilates.domain.membership.entity.MembershipPass;
import com.pilates.domain.membership.repository.MembershipPassRepository;
import com.pilates.domain.membership.repository.MembershipRepository;
import com.pilates.domain.membership.service.MembershipService;
import com.pilates.domain.payment.dto.*;
import com.pilates.domain.payment.entity.Payment;
import com.pilates.domain.payment.entity.PaymentMethod;
import com.pilates.domain.payment.entity.PaymentStatus;
import com.pilates.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 결제 도메인 서비스.
 * 토스페이먼츠 결제 승인/취소를 중개하고, 정기권 발급과 연동한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final MembershipPassRepository membershipPassRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipService membershipService;
    private final TossPaymentClient tossPaymentClient;
    private final EncryptionService encryptionService;

    /**
     * 결제 준비.
     * 정기권 종류를 기반으로 PENDING 상태의 결제를 생성하고 orderId를 발급한다.
     *
     * @param memberId 회원 ID
     * @param request  결제 준비 요청
     * @return orderId, 금액, 주문명
     */
    @Transactional
    public PrepareResponse preparePayment(Long memberId, PrepareRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        MembershipPass pass = membershipPassRepository.findByIdAndDeletedAtIsNull(request.membershipPassId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_PASS_NOT_FOUND));

        String orderId = "ORDER_" + UUID.randomUUID();

        Payment payment = Payment.builder()
                .orderId(orderId)
                .member(member)
                .membershipPass(pass)
                .amount(pass.getPrice())
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);

        log.info("결제 준비 완료: orderId={}, memberId={}, passId={}, amount={}",
                orderId, memberId, pass.getId(), pass.getPrice());

        return new PrepareResponse(orderId, pass.getPrice(), pass.getName());
    }

    /**
     * 결제 승인 (토스 콜백).
     * 금액 일치 검증 후 토스 승인 API를 호출하고, 정기권을 발급한다.
     * 정기권 발급 실패 시 보상 트랜잭션으로 토스 취소 + 결제 환불 처리한다.
     *
     * @param request 결제 승인 요청
     * @return 결제 ID, 정기권 ID, 상태
     */
    @Transactional
    public ConfirmResponse confirmPayment(ConfirmRequest request) {
        Payment payment = paymentRepository.findByOrderId(request.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));

        // 이미 처리된 결제 체크
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        // 금액 일치 검증 (보안 핵심)
        if (payment.getAmount().compareTo(request.amount()) != 0) {
            log.warn("결제 금액 불일치! orderId={}, expected={}, actual={}",
                    request.orderId(), payment.getAmount(), request.amount());
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 토스 결제 승인
        TossConfirmResponse tossResponse;
        try {
            tossResponse = tossPaymentClient.confirmPayment(
                    request.paymentKey(), request.orderId(), request.amount());
        } catch (Exception e) {
            log.error("토스 결제 승인 실패: orderId={}, error={}", request.orderId(), e.getMessage());
            payment.fail("토스 승인 실패: " + e.getMessage());
            throw new BusinessException(ErrorCode.PAYMENT_TOSS_CONFIRM_FAILED);
        }

        // 결제 승인 완료
        payment.confirm(request.paymentKey(), tossResponse.cardCompany(), LocalDateTime.now());

        // 정기권 발급
        Membership membership;
        try {
            MembershipIssueRequest issueRequest = new MembershipIssueRequest(
                    payment.getMember().getId(),
                    null, // totalCount — pass 기반 발급이므로 null
                    null, // price — pass 기반 발급이므로 null
                    null, // validityDays — pass 기반 발급이므로 null
                    false, // unlimited — pass 기반 발급이므로 무시됨
                    List.of(), // lessonTypeIds — pass 기반 발급이므로 빈 리스트
                    payment.getMembershipPass().getId()
            );
            MembershipResponse membershipResponse = membershipService.issueMembership(issueRequest);
            membership = findMembershipById(membershipResponse.id());
            payment.linkMembership(membership);
        } catch (Exception e) {
            // 보상 트랜잭션: 토스 결제 취소
            log.error("정기권 발급 실패, 보상 트랜잭션 실행: orderId={}, error={}", request.orderId(), e.getMessage());
            try {
                tossPaymentClient.cancelPayment(request.paymentKey(), "정기권 발급 실패로 인한 자동 취소", payment.getAmount());
            } catch (Exception cancelEx) {
                log.error("보상 트랜잭션 토스 취소 실패: orderId={}, error={}", request.orderId(), cancelEx.getMessage());
            }
            payment.refund(payment.getAmount(), "정기권 발급 실패로 인한 자동 환불");
            throw new BusinessException(ErrorCode.PAYMENT_TOSS_CONFIRM_FAILED);
        }

        log.info("결제 승인 완료: paymentId={}, orderId={}, membershipId={}",
                payment.getId(), request.orderId(), membership.getId());

        return new ConfirmResponse(payment.getId(), membership.getId(), payment.getStatus().name());
    }

    /**
     * 환불 처리.
     * 토스 결제 취소 후 Payment 환불 상태를 갱신한다.
     * 전액 환불 시 연결된 정기권을 만료시킨다.
     *
     * @param paymentId 결제 ID
     * @param request   환불 요청
     */
    @Transactional
    public void refundPayment(Long paymentId, RefundRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));

        if (!payment.isRefundable()) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_REFUNDABLE);
        }

        // 사용분 차감 계산: 환불 가능 = 결제 금액 - 이미 환불 - 사용분
        BigDecimal maxRefundable = payment.getRefundableAmount();
        if (payment.getMembership() != null && !payment.getMembership().isUnlimited()) {
            int totalCount = payment.getMembership().getTotalCount();
            int remainingCount = payment.getMembership().getRemainingCount();
            int usedCount = totalCount - remainingCount;
            if (usedCount > 0 && totalCount > 0) {
                BigDecimal usedAmount = payment.getAmount()
                        .multiply(BigDecimal.valueOf(usedCount))
                        .divide(BigDecimal.valueOf(totalCount), 0, java.math.RoundingMode.CEILING);
                BigDecimal afterUsage = payment.getAmount().subtract(usedAmount)
                        .subtract(payment.getRefundAmount() != null ? payment.getRefundAmount() : BigDecimal.ZERO);
                if (afterUsage.compareTo(BigDecimal.ZERO) < 0) afterUsage = BigDecimal.ZERO;
                maxRefundable = afterUsage.min(maxRefundable);
            }
        }

        if (request.refundAmount().compareTo(maxRefundable) > 0) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_EXCEEDED);
        }

        // 토스 결제 취소
        try {
            tossPaymentClient.cancelPayment(payment.getPaymentKey(), request.reason(), request.refundAmount());
        } catch (Exception e) {
            log.error("토스 환불 실패: paymentId={}, error={}", paymentId, e.getMessage());
            throw new BusinessException(ErrorCode.PAYMENT_TOSS_REFUND_FAILED);
        }

        payment.refund(request.refundAmount(), request.reason());

        // 전액 환불 시 정기권 만료
        if (payment.getStatus() == PaymentStatus.REFUNDED && payment.getMembership() != null) {
            payment.getMembership().expire();
            log.info("전액 환불로 정기권 만료: membershipId={}", payment.getMembership().getId());
        }

        log.info("환불 처리 완료: paymentId={}, refundAmount={}, status={}",
                paymentId, request.refundAmount(), payment.getStatus());
    }

    /**
     * 내 결제 목록 조회.
     *
     * @param memberId 회원 ID
     * @return 결제 목록
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments(Long memberId) {
        return paymentRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(this::toPaymentResponse)
                .toList();
    }

    /**
     * 결제 상세 조회.
     *
     * @param id 결제 ID
     * @return 결제 상세
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentDetail(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));
        return toPaymentResponse(payment);
    }

    /**
     * 전체 결제 목록 조회 (관리자).
     *
     * @param memberId 회원 ID 필터 (nullable)
     * @return 결제 목록
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments(Long memberId) {
        List<Payment> payments;
        if (memberId != null) {
            payments = paymentRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId);
        } else {
            payments = paymentRepository.findAll();
        }
        return payments.stream()
                .map(this::toPaymentResponse)
                .toList();
    }

    /**
     * 매출 통계 조회 (관리자). 날짜별 집계.
     */
    @Transactional(readOnly = true)
    public List<PaymentStatisticsResponse> getStatistics(java.time.LocalDate from, java.time.LocalDate to) {
        List<Payment> payments = paymentRepository.findAllByPaidAtBetween(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        return payments.stream()
                .filter(p -> p.getStatus() != PaymentStatus.FAILED && p.getStatus() != PaymentStatus.PENDING)
                .collect(java.util.stream.Collectors.groupingBy(p -> p.getPaidAt().toLocalDate()))
                .entrySet().stream()
                .map(e -> {
                    java.time.LocalDate date = e.getKey();
                    List<Payment> dayPayments = e.getValue();
                    long count = dayPayments.size();
                    BigDecimal total = dayPayments.stream()
                            .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal refund = dayPayments.stream()
                            .map(p -> p.getRefundAmount() != null ? p.getRefundAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new PaymentStatisticsResponse(date, count, total, refund, total.subtract(refund));
                })
                .sorted(java.util.Comparator.comparing(PaymentStatisticsResponse::date))
                .toList();
    }

    // ── private ──

    private Membership findMembershipById(Long id) {
        return membershipRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        String passName = null;
        if (payment.getMembershipPass() != null) {
            passName = payment.getMembershipPass().getName();
        }

        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getMethod().name(),
                payment.getStatus().name(),
                payment.getRefundAmount(),
                passName,
                payment.getPaidAt() != null ? payment.getPaidAt().toString() : null,
                payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : null
        );
    }
}
