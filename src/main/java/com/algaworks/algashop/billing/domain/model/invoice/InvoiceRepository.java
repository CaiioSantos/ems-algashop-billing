package com.algaworks.algashop.billing.domain.model.invoice;

import com.algaworks.algashop.billing.application.invoice.query.InvoiceOutput;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    boolean existsByOrderId(String orderId);

    Optional<Invoice> findByOrderId(String orderId);
}
