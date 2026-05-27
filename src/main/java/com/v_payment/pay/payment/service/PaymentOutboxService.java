package com.v_payment.pay.payment.service;

import com.v_payment.pay.global.BusinessException;
import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.entity.PaymentLedger;
import com.v_payment.pay.payment.entity.PaymentOutbox;
import com.v_payment.pay.payment.entity.PaymentOutboxStatus;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.entity.PaymentStatus;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.infra.PaymentError;
import com.v_payment.pay.payment.infra.Result;
import com.v_payment.pay.payment.infra.SuccessResult;
import com.v_payment.pay.payment.infra.TossPayment;
import com.v_payment.pay.payment.repository.PaymentLedgerRepository;
import com.v_payment.pay.payment.repository.PaymentOutboxRepository;
import com.v_payment.pay.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static com.v_payment.pay.payment.exception.PaymentException.PAYMENT_INVALID;
import static com.v_payment.pay.payment.exception.PaymentException.PAYMENT_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOutboxService {
    private static final long RETRY_DELAY_SECONDS = 1;

    private final Clock clock;
    private final TossPayment tossPayment;
    private final PaymentRepository paymentRepository;
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final PaymentLedgerRepository paymentLedgerRepository;

    public List<Long> findIds(int count) {
        Pageable pageable = PageRequest.of(0, count);
        return paymentOutboxRepository.findForPublish(PaymentOutboxStatus.READY, LocalDateTime.now(clock), pageable);
    }

    public Result approve(Long id) {
        PaymentPayload paymentPayload = paymentOutboxRepository.findPaymentPayloadById(id)
                .orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));

        return tossPayment.call(paymentPayload);
    }

    @Transactional
    public void finalizePaymentPayload(Long outboxId, Result approveResult) {
        if (approveResult instanceof SuccessResult successResult) {
            applySuccessResult(outboxId, successResult); return;
        }
        if (approveResult instanceof FailedResult failedResult) {
            applyFailedResult(outboxId, failedResult);
        }
    }

    private void applySuccessResult(Long outboxId, SuccessResult successResult) {
        PaymentOutbox paymentOutbox = findReadyOutbox(outboxId);
        Payment successedPayment = paymentRepository.findByOrderIdAndPaymentStatus(
                successResult.orderId(),
                PaymentStatus.APPROVING
        ).orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));

        try {
            paymentOutbox.publish();
            successedPayment.success(successResult);
            paymentLedgerRepository.save(PaymentLedger.createApproveSuccessPaymentLedger(successResult, successedPayment));
            paymentRepository.flush();
        } catch (OptimisticLockingFailureException e) {
            throw new BusinessException(PAYMENT_INVALID);
        }
    }

    private void applyFailedResult(Long outboxId, FailedResult failedResult) {
        PaymentOutbox paymentOutbox = findReadyOutbox(outboxId);
        Payment failedPayment = paymentRepository.findByOrderIdAndPaymentStatus(
                failedResult.orderId(),
                PaymentStatus.APPROVING
        ).orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));

        try {
            if (isRetryable(failedResult)) {
                paymentOutbox.retry(
                        failedResult.paymentError(),
                        failedResult.message(),
                        LocalDateTime.now(clock).plusSeconds(RETRY_DELAY_SECONDS)
                );
            } else {
                paymentOutbox.fail(failedResult.paymentError(), failedResult.message());
            }

            if (paymentOutbox.isFailed()) {
                failedPayment.failed(failedResult);
                paymentLedgerRepository.save(PaymentLedger.createApproveFailedPaymentLedger(failedResult, failedPayment));
            }

            paymentRepository.flush();
        } catch (OptimisticLockingFailureException e) {
            throw new BusinessException(PAYMENT_INVALID);
        }
    }

    private PaymentOutbox findReadyOutbox(Long outboxId) {
        return paymentOutboxRepository.findByIdAndStatus(outboxId, PaymentOutboxStatus.READY)
                .orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));
    }

    private boolean isRetryable(FailedResult failedResult) {
        return failedResult.paymentError() == PaymentError.NETWORK_TIMEOUT ||
                failedResult.paymentError() == PaymentError.UPSTREAM_429 ||
                failedResult.paymentError() == PaymentError.UPSTREAM_5XX;
    }
}
