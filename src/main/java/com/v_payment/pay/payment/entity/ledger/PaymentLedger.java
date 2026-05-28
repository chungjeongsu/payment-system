package com.v_payment.pay.payment.entity.ledger;

import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.entity.PaymentStatus;
import com.v_payment.pay.payment.entity.Provider;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.infra.SuccessResult;
import jakarta.persistence.*;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_ledger")
public class PaymentLedger {
    @Id
    @Column(name = "payment_ledger_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;

    private String paymentKey;

    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Enumerated(EnumType.STRING)
    private PaymentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    private PaymentStatus toStatus;

    private String failedCode;

    private String failedMessage;

    private Long requestedAmount;

    private Long approvedAmount;

    private LocalDateTime createdAt;

    protected PaymentLedger() {
    }

    @Builder
    private PaymentLedger(String orderId,
                         String paymentKey,
                         Provider provider,
                         PaymentStatus fromStatus,
                         PaymentStatus toStatus,
                         String failedCode,
                         String failedMessage,
                         Long requestedAmount,
                         Long approvedAmount,
                         LocalDateTime createdAt) {
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.provider = provider;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.failedCode = failedCode;
        this.failedMessage = failedMessage;
        this.requestedAmount = requestedAmount;
        this.approvedAmount = approvedAmount;
        this.createdAt = createdAt;
    }

    public static PaymentLedger createPaymentCreatedLedger(Payment payment) {
        return create(
                payment.getOrderId(),
                payment.getPaymentKey(),
                payment.getProvider(),
                null,
                PaymentStatus.PENDING,
                null,
                null,
                payment.getRequestedAmount(),
                null
        );
    }

    public static PaymentLedger createApprovePaymentLedger(Payment payment) {
        return create(
                payment.getOrderId(),
                payment.getPaymentKey(),
                payment.getProvider(),
                PaymentStatus.PENDING,
                PaymentStatus.APPROVING,
                null,
                null,
                payment.getRequestedAmount(),
                null
        );
    }

    public static PaymentLedger createApproveSuccessPaymentLedger(SuccessResult successResult, Payment payment) {
        return create(
                successResult.orderId(),
                successResult.paymentKey(),
                payment.getProvider(),
                PaymentStatus.APPROVING,
                PaymentStatus.APPROVED,
                null,
                null,
                payment.getRequestedAmount(),
                successResult.totalAmount()
        );
    }

    public static PaymentLedger createApproveFailedPaymentLedger(FailedResult failedResult, Payment payment) {
        return create(
                failedResult.orderId(),
                payment.getPaymentKey(),
                payment.getProvider(),
                PaymentStatus.APPROVING,
                PaymentStatus.REJECTED,
                failedResult.paymentError().name(),
                failedResult.message(),
                payment.getRequestedAmount(),
                null
        );
    }

    private static PaymentLedger create(String orderId,
                                        String paymentKey,
                                        Provider provider,
                                        PaymentStatus fromStatus,
                                        PaymentStatus toStatus,
                                        String failedCode,
                                        String failedMessage,
                                        Long requestedAmount,
                                        Long approvedAmount) {
        return PaymentLedger.builder()
                .orderId(orderId)
                .paymentKey(paymentKey)
                .provider(provider)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .failedCode(failedCode)
                .failedMessage(failedMessage)
                .requestedAmount(requestedAmount)
                .approvedAmount(approvedAmount)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
