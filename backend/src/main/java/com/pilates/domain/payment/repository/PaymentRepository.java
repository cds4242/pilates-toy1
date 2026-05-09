package com.pilates.domain.payment.repository;

import com.pilates.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 결제 리포지토리.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    boolean existsByOrderId(String orderId);

    List<Payment> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);

    List<Payment> findAllByPaidAtBetween(LocalDateTime from, LocalDateTime to);
}
