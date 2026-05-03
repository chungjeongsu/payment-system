package com.v_payment.pay.payment.service;

import com.v_payment.pay.global.BusinessException;
import com.v_payment.pay.global.ConnMonitor;
import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.req.PaymentCreateReq;
import com.v_payment.pay.payment.controller.dto.res.PaymentCreateRes;
import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.entity.PaymentStatus;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.infra.Result;
import com.v_payment.pay.payment.infra.SuccessResult;
import com.v_payment.pay.payment.infra.TossPayment;
import com.v_payment.pay.payment.repository.PaymentRepository;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

import static com.v_payment.pay.payment.exception.PaymentException.*;

@Slf4j(topic = "API_LOGGER")
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final Clock clock;
    private final TossPayment tossPayment;
    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentCreateRes create(PaymentCreateReq paymentCreateReq) {
        Payment newPayment = Payment.create(paymentCreateReq, clock);
        Payment savedPayment = paymentRepository.save(newPayment);
        return PaymentCreateRes.from(savedPayment);
    }

    @Transactional
    public PaymentPayload validateApprovalReq(ApprovalReq approvalReq) {
        ConnMonitor.logConnectionStatus("validateApprovalReq() 트랜잭션 시작");
        Payment payment = paymentRepository.findByOrderIdAndPaymentStatus(approvalReq.orderId(), PaymentStatus.PENDING)
                .orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));

        if(!payment.isSameRequestedAmount(approvalReq.requestedAmount())) throw new BusinessException(PAYMENT_INVALID);
        if(!payment.isSameMethod(approvalReq.method())) throw new BusinessException(PAYMENT_INVALID);
        if(!payment.isSameProvider(approvalReq.provider())) throw new  BusinessException(PAYMENT_INVALID);

        payment.completeValidate(approvalReq);

        return payment.getPaymentPayload();
    }

    public Result approve(PaymentPayload paymentPayload) {
        return tossPayment.call(paymentPayload);
    }

    @Transactional
    public Payment finalizePaymentPayload(Result approveResult) {
        ConnMonitor.logConnectionStatus("finishedPayment() 트랜잭션 시작");
        if(approveResult instanceof SuccessResult successResult) {
            return applySuccessResult(successResult);
        } else if (approveResult instanceof FailedResult failedResult) {
            return applyFailedResult(failedResult);
        }
        throw new BusinessException(UNKNOWN_ERROR);
    }

    @Transactional
    public void recoverApproveFailed(PaymentPayload paymentPayload) {
        Payment retryFailedPayment = paymentRepository.findByOrderIdAndPaymentStatus(paymentPayload.getOrderId(),
                PaymentStatus.APPROVING).orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));
        retryFailedPayment.retryFailed();
    }

    private Payment applySuccessResult(SuccessResult successResult) {
        Payment successedPayment = paymentRepository.findByOrderIdAndPaymentStatus(successResult.orderId(),
                PaymentStatus.APPROVING).orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));
        successedPayment.success(successResult);
        return successedPayment;
    }

    private Payment applyFailedResult(FailedResult failedResult) {
        Payment failedPayment = paymentRepository.findByOrderIdAndPaymentStatus(failedResult.orderId(),
                PaymentStatus.APPROVING).orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));
        failedPayment.failed(failedResult);
        return failedPayment;
    }
}