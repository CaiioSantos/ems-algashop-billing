package com.algaworks.algashop.billing.infrastructure.creditcard.fastpay;

import com.algaworks.algashop.billing.domain.model.creditcard.LimitedCreditCard;
import com.algaworks.algashop.billing.presentation.BadGatewayException;
import com.algaworks.algashop.billing.presentation.GatewayTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.circuitbreaker.retry.FrameworkRetryCircuitBreaker;
import org.springframework.cloud.circuitbreaker.retry.FrameworkRetryConfig;
import org.springframework.cloud.circuitbreaker.retry.FrameworkRetryConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.core.retry.RetryException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.util.UUID;

@Component
@Slf4j
public class ResilientFastPayAPIClient {

    private final FastpayCreditCardAPIClient fastpayCreditCardAPIClient;
    private final FrameworkRetryCircuitBreaker circuitBreaker;


    public ResilientFastPayAPIClient(FastpayCreditCardAPIClient fastpayCreditCardAPIClient,
                                     CircuitBreakerFactory<FrameworkRetryConfig,
                                             FrameworkRetryConfigBuilder> circuitBreakerFactory) {
        this.fastpayCreditCardAPIClient = fastpayCreditCardAPIClient;
        this.circuitBreaker = (FrameworkRetryCircuitBreaker) circuitBreakerFactory.create("fastpayCreditCardCB");
    }

    public FastpayCreditCardResponse register(UUID customerId, String tokenizedCard) {
        FastpayCreditCardInput input = FastpayCreditCardInput.builder()
                .tokenizedCard(tokenizedCard)
                .customerCode(customerId.toString())
                .build();
        try {
            FastpayCreditCardResponse response = circuitBreaker.run(
                    () -> doRegister(input),
                    ex -> doInternalFallback(input, ex)
            );
            if (response == null) {
                throw new BadGatewayException.ClientErrorException("Invalid credit card provided");
            }
            return response;
        } catch (NoFallbackAvailableException e) {
            log.error("Error registering credit card with FastPay API", e);
            throw unwrapException(e);
        }
    }

    private FastpayCreditCardResponse doRegister(FastpayCreditCardInput input) {
        try {
            return fastpayCreditCardAPIClient.create(input);

        }catch (HttpClientErrorException e) {
            if (!(e instanceof HttpClientErrorException.NotFound)) {
                log.warn("FastPay API Client Error: {}", input, e);
            }
            return null;
        } catch (RestClientException e) {
            throw translateException(e);
        }
    }

    private RuntimeException unwrapException(NoFallbackAvailableException e) {
        if (e.getCause() instanceof RetryException re) {
            if (re.getCause() instanceof GatewayTimeoutException gte) {
                return gte;
            }
            if (re.getCause() instanceof BadGatewayException bge) {
                return bge;
            }
        }
        return e;
    }

    private RuntimeException translateException(RestClientException e) {
        if (e.getCause() instanceof SocketTimeoutException
                || e instanceof ResourceAccessException) {
            return new GatewayTimeoutException("FastPay API Timeout", e);
        }

        if (e instanceof HttpClientErrorException) {
            return new BadGatewayException.ClientErrorException("FastPay API Bad Gateway", e);
        }

        if (e instanceof HttpServerErrorException) {
            return new BadGatewayException.ServerErrorException("FastPay API Bad Gateway", e);
        }

        return new BadGatewayException("FastPay API Bad Gateway", e);
    }

    private FastpayCreditCardResponse doInternalFallback(FastpayCreditCardInput input, Throwable ex) {
        log.error("Internal fallback triggered for FastPay API", ex);
        throw new RuntimeException("FastPay API is currently unavailable", ex);
    }
}
