package com.v_payment.pay.payment.repository;

import com.v_payment.pay.payment.entity.ledger.PaymentLedger;
import com.v_payment.pay.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface PaymentLedgerRepository extends JpaRepository<PaymentLedger, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO payment_ledger (
                order_id,
                payment_key,
                provider,
                from_status,
                to_status,
                failed_code,
                failed_message,
                requested_amount,
                approved_amount,
                created_at
            )
            VALUES (
                :orderId,
                :paymentKey,
                :provider,
                :fromStatus,
                :toStatus,
                :failedCode,
                :failedMessage,
                :requestedAmount,
                :approvedAmount,
                :createdAt
            )
            """, nativeQuery = true)
    void insertPaymentLedger(@Param("orderId") String orderId,
                             @Param("paymentKey") String paymentKey,
                             @Param("provider") String provider,
                             @Param("fromStatus") PaymentStatus fromStatus,
                             @Param("toStatus") PaymentStatus toStatus,
                             @Param("failedCode") String failedCode,
                             @Param("failedMessage") String failedMessage,
                             @Param("requestedAmount") Long requestedAmount,
                             @Param("approvedAmount") Long approvedAmount,
                             @Param("createdAt") LocalDateTime createdAt);
}
