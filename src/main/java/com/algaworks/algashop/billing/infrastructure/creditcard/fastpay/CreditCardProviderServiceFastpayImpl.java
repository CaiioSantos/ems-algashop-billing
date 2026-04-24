package com.algaworks.algashop.billing.infrastructure.creditcard.fastpay;

import com.algaworks.algashop.billing.domain.model.creditcard.CreditCardProviderService;
import com.algaworks.algashop.billing.domain.model.creditcard.LimitedCreditCard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "algashop.integrations.payment.provider", havingValue = "FASTPAY")
@RequiredArgsConstructor
public class CreditCardProviderServiceFastpayImpl implements CreditCardProviderService {

    private final FastpayCreditCardAPIClient fastpayCreditCardAPIClient;

    @Override
    public LimitedCreditCard register(UUID customerId, String tokenizedCard) {
        FastpayCreditCardInput input = FastpayCreditCardInput.builder()
                .tokenizedCard(tokenizedCard)
                .customerCode(customerId.toString())
                .build();

        FastpayCreditCardResponse response = fastpayCreditCardAPIClient.create(input);
        return toLimitedCreditCard(response);
    }

    @Override
    public Optional<LimitedCreditCard> findById(String gatewayCode) {
        try {
            FastpayCreditCardResponse response = fastpayCreditCardAPIClient.findById(gatewayCode);
            return Optional.of(toLimitedCreditCard(response));
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("Credit card not found with ID: {}", gatewayCode);
            return Optional.empty();
        } catch (HttpClientErrorException e) {
            log.warn("Error finding credit card with ID: {}", gatewayCode, e);
            throw e;
        }
    }

    @Override
    public void delete(String gatewayCode) {
        fastpayCreditCardAPIClient.delete(gatewayCode);
    }

    private LimitedCreditCard toLimitedCreditCard(FastpayCreditCardResponse response) {
        return LimitedCreditCard.builder()
                .brand(response.getBrand())
                .expYear(response.getExpYear())
                .expMonth(response.getExpMonth())
                .lastNumbers(response.getLastNumbers())
                .gatewayCode(response.getId())
                .build();
    }
}