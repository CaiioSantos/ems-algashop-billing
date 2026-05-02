package com.algaworks.algashop.billing.application.invoice.management;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GenerateInvoiceInput{

    private String orderId;

    @NotNull
    private UUID customerId;

    @NotNull
    private PaymentSettingsInput paymentSettings;

    @NotNull
    private PayerData payer;

    @NotEmpty
    private List<LineItemInput> items;
}