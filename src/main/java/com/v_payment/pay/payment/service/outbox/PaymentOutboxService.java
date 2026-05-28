package com.v_payment.pay.payment.service.outbox;

import com.v_payment.pay.payment.entity.*;
import com.v_payment.pay.payment.entity.outbox.PaymentOutbox;
import com.v_payment.pay.payment.entity.outbox.PaymentOutboxStatus;
import com.v_payment.pay.payment.entity.outbox.PaymentPayload;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.infra.PaymentError;
import com.v_payment.pay.payment.infra.Result;
import com.v_payment.pay.payment.infra.SuccessResult;
import com.v_payment.pay.payment.infra.TossPayment;
import com.v_payment.pay.payment.repository.PaymentOutboxRepository;
import com.v_payment.pay.payment.repository.PaymentRepository;
import com.v_payment.pay.payment.service.ledger.PaymentLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOutboxService {
    private static final int MAX_ATTEMPT_COUNT = 3;
    private static final long RETRY_DELAY_SECONDS = 1;

    private final Clock clock;
    private final TossPayment tossPayment;
    private final PaymentRepository paymentRepository;
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final PaymentLedgerService paymentLedgerService;

    //1. Batch로 처리해야할 이벤트들을 찾아온다.
    @Transactional
    public List<Long> loadApproves(int count) {
        List<Long> ids = paymentOutboxRepository.findForPublish(PaymentOutboxStatus.READY.name(), LocalDateTime.now(clock), count);
        if(ids.isEmpty()) return ids;
        paymentOutboxRepository.markProcessing(ids);
        return ids;
    }

    //2. 가상쓰레드를 사용하여, 각각의 엔티티 로딩
    public PaymentPayload preApprove(Long id) {
        PaymentOutbox paymentOutbox = paymentOutboxRepository.findById(id).get();
        return paymentOutbox.getPaymentPayload();
    }

    //3. 외부 API 호출
    public Result approve(PaymentPayload paymentPayload) {
        return tossPayment.call(paymentPayload);
    }

    //4. 외부 API 결과 처리
    @Transactional
    public void postApprove(Result result, Long id) {
        if(result instanceof SuccessResult successResult) {
            applySuccessResult(successResult, id);
        }
        if(result instanceof FailedResult failedResult) {
            applyFailedResult(failedResult, id);
        }
    }

    //4-1. 성공 처리
    private void applySuccessResult(SuccessResult successResult, Long id) {
        PaymentOutbox paymentOutbox = paymentOutboxRepository.findById(id).get();
        paymentOutbox.success();

        Payment payment = paymentRepository.findByOrderIdAndPaymentStatus(paymentOutbox.getOrderId(),
                PaymentStatus.APPROVING).get();
        payment.success(successResult);

        paymentLedgerService.insertPaymentLedgerAPPROVED(payment, successResult);
    }

    //4-2. 실패 처리
    private void applyFailedResult(FailedResult failedResult, Long id) {
        PaymentOutbox paymentOutbox = paymentOutboxRepository.findById(id).get();

        if (!isRetryable(failedResult) || paymentOutbox.getAttemptCount() >= MAX_ATTEMPT_COUNT) {
            applyDeadResult(failedResult, paymentOutbox); return;
        }

        paymentOutbox.failed(failedResult, LocalDateTime.now(clock).plusSeconds(RETRY_DELAY_SECONDS));
    }

    //4-2-1. 실패 중 재시도 불가 시 Dead 처리
    private void applyDeadResult(FailedResult failedResult, PaymentOutbox paymentOutbox) {
        paymentOutbox.dead(failedResult);

        Payment payment = paymentRepository.findByOrderIdAndPaymentStatus(failedResult.orderId(),
                PaymentStatus.APPROVING).get();
        payment.failed(failedResult);

        paymentLedgerService.insertPaymentLedgerREJECTED(payment, failedResult);
    }

    private boolean isRetryable(FailedResult failedResult) {
        return failedResult.paymentError() == PaymentError.NETWORK_TIMEOUT ||
                failedResult.paymentError() == PaymentError.UPSTREAM_429 ||
                failedResult.paymentError() == PaymentError.UPSTREAM_5XX;
    }
}
