package com.algaworks.algashop.billing.infrastructure.payment.fastpay;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange(value = "/api/v1/payments", accept = "application/json")
public interface FastPaymentAPIClient {

    @PostExchange(contentType = "application/json")
    FastpayPaymentModel capture(@RequestBody FastpayPaymentInput paymentInput);

    @GetExchange("/{paymentId}")
    FastpayPaymentModel findById(@PathVariable String paymentId);

    @PutExchange("/{paymentId}/refund")
    FastpayPaymentModel refund(@PathVariable String paymentId);

    @PutExchange("/{paymentId}/cancel")
    FastpayPaymentModel cancel(@PathVariable String paymentId);

}
