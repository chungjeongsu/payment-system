package com.v_payment.pay.payment.service;

import com.v_payment.pay.payment.entity.PaymentOutboxStatus;
import com.v_payment.pay.payment.repository.PaymentOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentOutboxProcessor {
    private final Clock clock;
    private final PaymentOutboxRepository paymentOutboxRepository;

    public List<Long> findApprovableIds(int count) {
        return paymentOutboxRepository.findForPublish(
                PaymentOutboxStatus.READY,
                LocalDateTime.now(clock),
                PageRequest.of(0, count)
        );
    }
}
