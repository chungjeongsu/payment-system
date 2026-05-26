package com.v_payment.pay.payment.repository;

import com.v_payment.pay.payment.entity.PaymentLedger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentLedgerRepository extends JpaRepository<PaymentLedger, Long> {
}
