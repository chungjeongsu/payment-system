package com.v_payment.pay.payment.entity.outbox;

import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.infra.PaymentError;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
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

    @Enumerated(EnumType.STRING)
    private PaymentError lastErrorCode;   //nullable

    private String lastErrorMessage;    //nullable

    private LocalDateTime createdAt;

    private LocalDateTime nextAttemptTime;

    protected PaymentOutbox() {
    }

    @Builder
    private PaymentOutbox(String orderId,
                          String paymentKey,
                          Long amount,
                          PaymentOutboxStatus status,
                          Integer attemptCount,
                          PaymentError lastErrorCode,
                          String lastErrorMessage,
                          LocalDateTime createdAt,
                          LocalDateTime nextAttemptTime) {
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.amount = amount;
        this.status = status;
        this.attemptCount = attemptCount;
        this.lastErrorCode = lastErrorCode;
        this.lastErrorMessage = lastErrorMessage;
        this.createdAt = createdAt;
        this.nextAttemptTime = nextAttemptTime;
    }

    public PaymentPayload getPaymentPayload() {
        return PaymentPayload.create(orderId, paymentKey, amount);
    }

    public void process() {
        if(status != PaymentOutboxStatus.READY) throw new IllegalStateException("Ready 상태가 아닙니다.");
        this.status = PaymentOutboxStatus.PROCESSING;
        this.attemptCount++;
    }

    public void success() {
        if(status != PaymentOutboxStatus.PROCESSING) throw new IllegalStateException("Processing 상태가 아닙니다.");
        this.status = PaymentOutboxStatus.PUBLISHED;
    }

    public void failed(FailedResult failedResult, LocalDateTime nextAttemptTime) {
        if(status != PaymentOutboxStatus.PROCESSING) throw new IllegalStateException("Processing 상태가 아닙니다.");
        this.status = PaymentOutboxStatus.READY;
        this.lastErrorCode = failedResult.paymentError();
        this.lastErrorMessage = failedResult.message();
        this.nextAttemptTime = nextAttemptTime;
    }

    public void dead(FailedResult failedResult) {
        if(status != PaymentOutboxStatus.PROCESSING) throw new IllegalStateException("Processing 상태가 아닙니다.");
        this.status = PaymentOutboxStatus.DEAD;
        this.lastErrorCode = failedResult.paymentError();
        this.lastErrorMessage = failedResult.message();
        this.nextAttemptTime = null;
    }

    public static PaymentOutbox create(Payment payment, LocalDateTime createdAt) {
        return PaymentOutbox.builder()
                .orderId(payment.getOrderId())
                .paymentKey(payment.getPaymentKey())
                .amount(payment.getRequestedAmount())
                .status(PaymentOutboxStatus.READY)
                .attemptCount(0)
                .lastErrorCode(null)
                .lastErrorMessage(null)
                .createdAt(createdAt)
                .nextAttemptTime(createdAt)
                .build();
    }
}
