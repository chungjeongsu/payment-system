package com.v_payment.pay.payment.service.outbox;

import com.v_payment.pay.payment.entity.outbox.PaymentPayload;
import com.v_payment.pay.payment.infra.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxScheduler {
    private final PaymentOutboxService paymentOutboxService;
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    @Scheduled(fixedDelay = 500)
    public void schedulePaymentOutbox() {
        List<Long> ids = paymentOutboxService.loadApproves(200);

        for (Long id : ids) {
            executorService.submit(() -> approvePipeline(id));
        }
    }

    private void approvePipeline(Long id) {
        try{
            PaymentPayload paymentPayload = paymentOutboxService.preApprove(id);

            Result result = paymentOutboxService.approve(paymentPayload);

            paymentOutboxService.postApprove(result, id);
        } catch (Exception e){
            log.error(e.getMessage());
        }
    }
}
