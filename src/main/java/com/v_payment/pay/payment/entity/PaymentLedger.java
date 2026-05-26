package com.v_payment.pay.payment.entity;

import jakarta.persistence.*;

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
}
