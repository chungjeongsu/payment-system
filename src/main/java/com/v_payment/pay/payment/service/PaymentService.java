package com.v_payment.pay.payment.service;

import com.v_payment.pay.global.BusinessException;
import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.req.PaymentCreateReq;
import com.v_payment.pay.payment.controller.dto.res.PaymentCreateRes;
import com.v_payment.pay.payment.entity.*;
import com.v_payment.pay.payment.entity.outbox.PaymentOutbox;
import com.v_payment.pay.payment.repository.PaymentOutboxRepository;
import com.v_payment.pay.payment.repository.PaymentRepository;
import com.v_payment.pay.payment.service.ledger.PaymentLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

import static com.v_payment.pay.payment.exception.PaymentException.*;

@Slf4j(topic = "API_LOGGER")
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final Clock clock;
    private final PaymentRepository paymentRepository;
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final PaymentLedgerService paymentLedgerService;

    @Transactional
    public PaymentCreateRes create(PaymentCreateReq paymentCreateReq) {
        Payment newPayment = Payment.create(paymentCreateReq, clock);
        Payment savedPayment = paymentRepository.save(newPayment);

        paymentLedgerService.insertPaymentLedgerPENDING(savedPayment);
        return PaymentCreateRes.from(savedPayment);
    }

    @Transactional
    public void validateApprovalReq(ApprovalReq approvalReq) {
        //Payment 검증 및 상태 업뎃
        Payment payment = paymentRepository.findByOrderIdAndPaymentStatusAndRequestedAmountAndProviderAndPaymentMethod(
                approvalReq.orderId(), PaymentStatus.PENDING, approvalReq.requestedAmount(), approvalReq.provider(),
                approvalReq.method()).orElseThrow(() -> new BusinessException(PAYMENT_INVALID));
        try{
            payment.completeValidate(approvalReq);
            paymentRepository.flush();
        } catch (OptimisticLockingFailureException e) {
            throw new BusinessException(PAYMENT_INVALID);
        }

        //Payment 원장 테이블 저장
        paymentLedgerService.insertPaymentLedgerAPPROVING(payment);

        //Payment 아웃박스 테이블 저장
        PaymentOutbox paymentOutbox = PaymentOutbox.create(payment, LocalDateTime.now(clock));
        paymentOutboxRepository.save(paymentOutbox);
    }
}
