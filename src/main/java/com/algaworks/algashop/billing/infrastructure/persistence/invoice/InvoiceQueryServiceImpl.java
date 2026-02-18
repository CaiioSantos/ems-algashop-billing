package com.algaworks.algashop.billing.infrastructure.persistence.invoice;

import com.algaworks.algashop.billing.application.invoice.query.InvoiceOutput;
import com.algaworks.algashop.billing.application.invoice.query.InvoiceQueryService;
import com.algaworks.algashop.billing.application.utility.Mapper;
import com.algaworks.algashop.billing.domain.model.invoice.Invoice;
import com.algaworks.algashop.billing.domain.model.invoice.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InvoiceQueryServiceImpl implements InvoiceQueryService {

    private final InvoiceRepository invoiceRepository;
    private final Mapper mapper;

    @Override
    public InvoiceOutput findByOrderId(String orderId) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findByOrderId(orderId);
        return mapper.convert(optionalInvoice.get(),InvoiceOutput.class);
    }
}
