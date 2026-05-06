package com.v_payment.pay.payment.service;

import com.v_payment.pay.global.ConnMonitor;
import com.v_payment.pay.global.ExecutorWithRetry;
import com.v_payment.pay.global.LTimer;
import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.res.ApprovalRes;
import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.infra.PaymentError;
import com.v_payment.pay.payment.infra.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j(topic = "API_LOGGER")
@Component
@RequiredArgsConstructor
public class PaymentServiceFacade {
    private static final long DB_CONNECTION_TIMEOUT = 2000;
    private static final long PAYMENT_API_TIMEOUT = 1500;

    private final PaymentService paymentService;

    public ApprovalRes approvePipeline(ApprovalReq approvalReq) {
        long vStartTime = LTimer.getCurrTime();
        PaymentPayload paymentPayload = paymentService.validateApprovalReq(approvalReq);
        ConnMonitor.logConnectionStatus("validateApprovalReq() 트랜잭션 종료 후");
        log.debug("승인 요청 검증 [{}] latency = {}", approvalReq.orderId(), LTimer.getDiff(vStartTime));
        if(LTimer.getDiff(vStartTime) >= DB_CONNECTION_TIMEOUT) {
            log.warn("validateApprovalReq()가 느립니다. orderId = {}", approvalReq.orderId());
        }

        long cStartTime = LTimer.getCurrTime();
        Result approveResult = getApproveResult(paymentPayload);
        ConnMonitor.logConnectionStatus("getApproveResult() 이후");
        log.debug("승인 호출 [{}] latency = {}", approvalReq.orderId(), LTimer.getDiff(cStartTime));
        if(LTimer.getDiff(cStartTime) >= PAYMENT_API_TIMEOUT) {
            log.warn("결제 API 호출이 느립니다. orderId = {}", approvalReq.orderId());
        }

        long fStartTime = LTimer.getCurrTime();
        Payment finishedPayment = paymentService.finalizePaymentPayload(approveResult);
        ConnMonitor.logConnectionStatus("finishedPayment() 트랜잭션 종료 후");
        log.debug("승인 결과 처리 [{}] latency = {}", approvalReq.orderId(), LTimer.getDiff(fStartTime));
        if(LTimer.getDiff(fStartTime) >= DB_CONNECTION_TIMEOUT) {
            log.warn("finalizePaymentPayload()가 느립니다. orderId = {}", approvalReq.orderId());
        }

        return ApprovalRes.from(finishedPayment);
    }

    private Result getApproveResult(PaymentPayload paymentPayload) {
        return ExecutorWithRetry
                .task(() -> paymentService.approve(paymentPayload))
                .continueCondition(this::getContinueCondition)
                .delayMillis(1000)
                .maxAttempts(3)
                .recovery(() -> paymentService.recoverApproveFailed(paymentPayload))
                .execute();
    }

    private boolean getContinueCondition(Result result) {
        if(result instanceof FailedResult failedResult) {
            return failedResult.paymentError() == PaymentError.NETWORK_TIMEOUT ||
                    failedResult.paymentError() == PaymentError.UPSTREAM_429 ||
                    failedResult.paymentError() == PaymentError.UPSTREAM_5XX;
        }
        return false;
    }
}
