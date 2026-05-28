package com.v_payment.pay.payment.infra;

import com.v_payment.pay.payment.config.TossPaymentProperties;
import com.v_payment.pay.payment.entity.outbox.PaymentPayload;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class TossPayment {
    private static final String AUTHORIZATION_HEADER_KEY = "Authorization";
    private static final String CONTENT_TYPE_HEADER_KEY = "Content-Type";

    private final RestClient tossPaymentClient;
    private final TossPaymentProperties tossPaymentProperties;

    @Timed(value = "pay.api")
    public Result call(PaymentPayload paymentPayload) {
        try{
            return exchangeRequestToResponse(paymentPayload);
        } catch (ResourceAccessException e) {
            return new FailedResult(paymentPayload.getOrderId(), PaymentError.NETWORK_TIMEOUT, e.getMessage());
        } catch (RestClientResponseException e) {
            int statusCode = e.getStatusCode().value();
            if(statusCode == 429) return new FailedResult(paymentPayload.getOrderId(), PaymentError.UPSTREAM_429, e.getMessage());
            if(statusCode >= 500) return new FailedResult(paymentPayload.getOrderId(), PaymentError.UPSTREAM_5XX, e.getMessage());
            return new FailedResult(paymentPayload.getOrderId(), PaymentError.UPSTREAM_4XX, e.getMessage());
        } catch (RuntimeException e) {
            return new FailedResult(paymentPayload.getOrderId(), PaymentError.UNKNOWN, e.getMessage());
        }
    }

    private Result exchangeRequestToResponse(PaymentPayload paymentPayload) {
        return tossPaymentClient.post()
                .uri(tossPaymentProperties.uri())
                .header(AUTHORIZATION_HEADER_KEY, encodeBase64(tossPaymentProperties.secret()))
                .header(CONTENT_TYPE_HEADER_KEY, tossPaymentProperties.contentType())
                .body(paymentPayload)
                .retrieve()
                .body(SuccessResult.class);
    }

    private String encodeBase64(String secretKey) {
        return "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }
}
