package com.v_payment.pay.payment.entity;

import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.req.PaymentCreateReq;
import com.v_payment.pay.payment.entity.outbox.PaymentPayload;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.infra.SuccessResult;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "payment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_order_id", columnNames = "orderId")
        })
public class Payment {
    @Id
    @Column(name = "payment_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Provider provider;                      //생성 시

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;            //생성 시

    private String orderId;                         //생성 시

    private String paymentKey;                      //검증 시

    private Long requestedAmount;                   //생성 시

    private Long approvedAmount;                    //성공 시

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;            //전체

    private LocalDateTime requestedAt;              //생성 시

    private LocalDateTime approvedAt;               //성공 시

    private String receiptUrl;                      //성공 시

    private String failedMessage;                   //실패 시

    @Version
    private Integer version;

    @Builder
    public Payment(Provider provider,
                   PaymentMethod paymentMethod,
                   String orderId,
                   String paymentKey,
                   Long requestedAmount,
                   Long approvedAmount,
                   PaymentStatus paymentStatus,
                   LocalDateTime requestedAt,
                   LocalDateTime approvedAt,
                   String receiptUrl) {
        this.provider = provider;
        this.paymentMethod = paymentMethod;
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.requestedAmount = requestedAmount;
        this.approvedAmount = approvedAmount;
        this.paymentStatus = paymentStatus;
        this.requestedAt = requestedAt;
        this.approvedAt = approvedAt;
        this.receiptUrl = receiptUrl;
    }

    public PaymentPayload getPaymentPayload() {
        return PaymentPayload.create(orderId, paymentKey, requestedAmount);
    }

    public boolean isSameRequestedAmount(Long requestedAmount) {
        return this.requestedAmount.equals(requestedAmount);
    }

    public boolean isSameMethod(PaymentMethod paymentMethod) {
        return this.paymentMethod == paymentMethod;
    }

    public boolean isSameProvider(Provider provider) {
        return this.provider.equals(provider);
    }

    public void completeValidate(ApprovalReq approvalReq) {
        this.paymentStatus = PaymentStatus.APPROVING;
        this.paymentKey = approvalReq.paymentKey();
    }

    public void success(SuccessResult successResult) {
        this.paymentStatus = PaymentStatus.APPROVED;
        this.approvedAmount = successResult.totalAmount();
        this.approvedAt = successResult.approvedAt();
        this.receiptUrl = successResult.receipt().url();
    }

    public void failed(FailedResult failedResult) {
        this.failedMessage = failedResult.message();
        this.paymentStatus = PaymentStatus.REJECTED;
    }

    public void retryFailed() {
        this.failedMessage = "retryFailed : 알 수 없는 에러";
        this.paymentStatus = PaymentStatus.REJECTED;
    }

    public static Payment create(PaymentCreateReq paymentCreateReq, Clock clock) {
        return Payment.builder()
                .provider(Provider.TOSS)
                .paymentMethod(paymentCreateReq.paymentMethod())
                .orderId(UUID.randomUUID().toString())
                .paymentKey(null)
                .requestedAmount(paymentCreateReq.requestedAmount())
                .approvedAmount(null)
                .paymentStatus(PaymentStatus.PENDING)
                .requestedAt(LocalDateTime.now(clock))
                .approvedAt(null)
                .receiptUrl(null)
                .build();
    }
}
