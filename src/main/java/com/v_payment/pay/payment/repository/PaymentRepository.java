package com.v_payment.pay.payment.repository;

import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.entity.PaymentMethod;
import com.v_payment.pay.payment.entity.PaymentStatus;
import com.v_payment.pay.payment.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderIdAndPaymentStatus(String orderId, PaymentStatus paymentStatus);

    Optional<Payment> findByOrderIdAndPaymentStatusAndRequestedAmountAndProviderAndPaymentMethod(
            String orderId, PaymentStatus paymentStatus, Long requestedAmount, Provider provider, PaymentMethod paymentMethod);

}
