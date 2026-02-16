package com.algaworks.algashop.billing.domain.model.invoice.payment;

import com.algaworks.algashop.billing.domain.model.invoice.PaymentSettings;

public interface PaymentGatewayService {
    Payment capture(PaymentRequest request);
    Payment findByCode(String gateWayCode);
}
