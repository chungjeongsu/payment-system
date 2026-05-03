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
    private final PaymentService paymentService;

    public ApprovalRes approvePipeline(ApprovalReq approvalReq) {
        long vStartTime = LTimer.getCurrTime();
        PaymentPayload paymentPayload = paymentService.validateApprovalReq(approvalReq);
        ConnMonitor.logConnectionStatus("validateApprovalReq() 트랜잭션 종료 후");
        log.debug("승인 요청 검증 [{}] latency = {}", approvalReq.orderId(), LTimer.getDiff(vStartTime));

        long cStartTime = LTimer.getCurrTime();
        Result approveResult = getApproveResult(paymentPayload);
        ConnMonitor.logConnectionStatus("getApproveResult() 이후");
        log.debug("승인 호출 [{}] latency = {}", approvalReq.orderId(), LTimer.getDiff(cStartTime));

        long fStartTime = LTimer.getCurrTime();
        Payment finishedPayment = paymentService.finalizePaymentPayload(approveResult);
        ConnMonitor.logConnectionStatus("finishedPayment() 트랜잭션 종료 후");
        log.debug("승인 결과 처리 [{}] latency = {}", approvalReq.orderId(), LTimer.getDiff(fStartTime));

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
