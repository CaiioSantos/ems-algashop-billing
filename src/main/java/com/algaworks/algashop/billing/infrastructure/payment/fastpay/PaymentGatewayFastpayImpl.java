package com.algaworks.algashop.billing.infrastructure.payment.fastpay;

import com.algaworks.algashop.billing.domain.model.creditcard.CreditCard;
import com.algaworks.algashop.billing.domain.model.creditcard.CreditCardNotFoundException;
import com.algaworks.algashop.billing.domain.model.creditcard.CreditCardRepository;
import com.algaworks.algashop.billing.domain.model.invoice.Address;
import com.algaworks.algashop.billing.domain.model.invoice.Payer;
import com.algaworks.algashop.billing.domain.model.invoice.payment.Payment;
import com.algaworks.algashop.billing.domain.model.invoice.payment.PaymentGatewayService;
import com.algaworks.algashop.billing.domain.model.invoice.payment.PaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@ConditionalOnProperty(name = "algashop.integrations.payment.provider", havingValue = "FASTPAY")
@RequiredArgsConstructor
public class PaymentGatewayFastpayImpl implements PaymentGatewayService {

    private final FastPaymentAPIClient fastpayPaymentAPIClient;
    private final CreditCardRepository creditCardRepository;

    @Override
    public Payment capture(PaymentRequest request) {
        FastpayPaymentInput input = convertToInput(request);
        FastpayPaymentModel paymentModel = fastpayPaymentAPIClient.capture(input);
        return convertToPayment(paymentModel);
    }

    @Override
    public Payment findByCode(String gateWayCode) {
        return this.convertToPayment(fastpayPaymentAPIClient.findById(gateWayCode));
    }

    private Payment convertToPayment(FastpayPaymentModel paymentModel) {
        Payment.PaymentBuilder builder = Payment.builder()
                .gatewayCode(paymentModel.getId())
                .invoiceId(UUID.fromString(paymentModel.getReferenceCode()));


        FastpayPaymentMethod fastpayPaymentMethod;

        try {
            fastpayPaymentMethod = FastpayPaymentMethod.valueOf(paymentModel.getMethod());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown payment method: " + paymentModel.getMethod());
        }

        FastpayPaymentStatus fastpayPaymentStatus;
        try {
            fastpayPaymentStatus = FastpayPaymentStatus.valueOf(paymentModel.getStatus());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown payment status: " + paymentModel.getStatus());
        }
        builder.method(FastpayEnumConverter.convert(fastpayPaymentMethod));
        builder.status(FastpayEnumConverter.convert(fastpayPaymentStatus));
        return builder.build();
    }

    private FastpayPaymentInput convertToInput(PaymentRequest request) {
        Payer payer = request.getPayer();
        Address address = payer.getAddress();

        var paymentInput = FastpayPaymentInput.builder()
                .totalAmount(request.getAmount())
                .referenceCode(request.getInvoiceId().toString())
                .fullName(payer.getFullName())
                .document(payer.getDocument())
                .phone(payer.getPhone())
                .addressLine1(address.getStreet() + ", " + address.getNumber())
                .addressLine2(address.getComplement())
                .zipCode(address.getZipCode())
                .replyToUrl("http://example.com/webhook/fastpay");

        switch (request.getMethod()) {
            case CREDIT_CARD -> {
                paymentInput.method(FastpayPaymentMethod.CREDIT.name());
                CreditCard creditCard = creditCardRepository.findById(request.getCreditCardId())
                        .orElseThrow(() -> new CreditCardNotFoundException());
                paymentInput.creditCardId(creditCard.getGatewayCode());
            }
            case GATEWAY_BALANCE -> paymentInput.method(FastpayPaymentMethod.GATEWAY_BALANCE.name());
        }
        return paymentInput.build();
    }
}
