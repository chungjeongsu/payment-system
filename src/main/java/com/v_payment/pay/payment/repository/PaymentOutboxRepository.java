package com.v_payment.pay.payment.repository;

import com.v_payment.pay.payment.entity.PaymentOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Long> {
}
