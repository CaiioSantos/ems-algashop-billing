package com.algaworks.algashop.billing.infrastructure.listener;

import com.algaworks.algashop.billing.domain.model.invoice.InvoiceCanceledEvent;
import com.algaworks.algashop.billing.domain.model.invoice.InvoiceIssuedEvent;
import com.algaworks.algashop.billing.domain.model.invoice.InvoicePaidEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class InvoiceEventListener {

    @EventListener
    public void listen(InvoiceIssuedEvent invoiceIssuedEvent){

    }

    @EventListener
    public void listen(InvoicePaidEvent invoicePaidEvent){

    }

    @EventListener
    public void listen(InvoiceCanceledEvent invoiceCanceledEvent){

    }
}
