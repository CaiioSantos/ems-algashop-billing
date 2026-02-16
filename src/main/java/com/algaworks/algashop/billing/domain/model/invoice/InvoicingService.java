package com.algaworks.algashop.billing.domain.model.invoice;

import com.algaworks.algashop.billing.domain.model.DomainException;
import com.algaworks.algashop.billing.domain.model.invoice.payment.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoicingService {

    private final InvoiceRepository repository;

    public Invoice issue(String orderId, UUID customerId, Payer payer, Set<LineItem> items) {
        if (repository.existsByOrderId(orderId)) {
            throw new DomainException(String.format("Invoice already exists for order %s", orderId));
        }
        return Invoice.issue(orderId,customerId,payer,items);
    }

    public void assignPayment(Invoice invoice, Payment payment) {
        invoice.assignPaymentGatewayCode(payment.getGatewayCode());
        switch (payment.getStatus()) {
            case FAILED -> invoice.cancel("payment failed");
            case REFUNDED -> invoice.cancel("payment refunded");
            case PAID -> invoice.markAsPaid();
        }

    }

}
