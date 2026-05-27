package com.v_payment.pay.payment.entity;

import com.v_payment.pay.payment.infra.PaymentError;
import jakarta.persistence.*;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_outbox")
public class PaymentOutbox {
    private static final int MAX_ATTEMPT_COUNT = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_outbox_id")
    private Long id;

    private String orderId;

    private String paymentKey;

    private Long amount;

    @Enumerated(EnumType.STRING)
    private PaymentOutboxStatus status;

    private Integer attemptCount;

    private String lastErrorCode;

    private String lastErrorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime nextAttemptTime;

    protected PaymentOutbox() {
    }

    @Builder
    private PaymentOutbox(String orderId,
                          String paymentKey,
                          Long amount,
                          PaymentOutboxStatus status,
                          Integer attemptCount,
                          String lastErrorCode,
                          String lastErrorMessage,
                          LocalDateTime createdAt,
                          LocalDateTime updatedAt,
                          LocalDateTime nextAttemptTime) {
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.amount = amount;
        this.status = status;
        this.attemptCount = attemptCount;
        this.lastErrorCode = lastErrorCode;
        this.lastErrorMessage = lastErrorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.nextAttemptTime = nextAttemptTime;
    }

    public static PaymentOutbox create(Payment payment) {
        LocalDateTime now = LocalDateTime.now();

        return PaymentOutbox.builder()
                .orderId(payment.getOrderId())
                .paymentKey(payment.getPaymentKey())
                .amount(payment.getRequestedAmount())
                .status(PaymentOutboxStatus.READY)
                .attemptCount(0)
                .lastErrorCode(null)
                .lastErrorMessage(null)
                .createdAt(now)
                .updatedAt(now)
                .nextAttemptTime(now)
                .build();
    }

    public void publish() {
        validateReady();
        this.status = PaymentOutboxStatus.PUBLISHED;
        this.lastErrorCode = null;
        this.lastErrorMessage = null;
        this.nextAttemptTime = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void retry(PaymentError paymentError, String errorMessage, LocalDateTime nextAttemptTime) {
        validateReady();
        int nextAttemptCount = currentAttemptCount() + 1;
        if (nextAttemptCount >= MAX_ATTEMPT_COUNT) {
            fail(paymentError, errorMessage);
            return;
        }

        this.attemptCount = nextAttemptCount;
        this.lastErrorCode = paymentError.name();
        this.lastErrorMessage = errorMessage;
        this.nextAttemptTime = nextAttemptTime;
        this.updatedAt = LocalDateTime.now();
    }

    public void fail(PaymentError paymentError, String errorMessage) {
        validateReady();
        this.status = PaymentOutboxStatus.FAILED;
        this.attemptCount = Math.max(currentAttemptCount() + 1, MAX_ATTEMPT_COUNT);
        this.lastErrorCode = paymentError.name();
        this.lastErrorMessage = errorMessage;
        this.nextAttemptTime = null;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isFailed() {
        return this.status == PaymentOutboxStatus.FAILED;
    }

    private int currentAttemptCount() {
        return this.attemptCount == null ? 0 : this.attemptCount;
    }

    private void validateReady() {
        if (this.status != PaymentOutboxStatus.READY) {
            throw new IllegalStateException("PaymentOutbox status must be READY.");
        }
    }
}
