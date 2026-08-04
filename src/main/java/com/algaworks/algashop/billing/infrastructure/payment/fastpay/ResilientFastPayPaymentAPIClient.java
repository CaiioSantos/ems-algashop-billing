package com.algaworks.algashop.billing.infrastructure.payment.fastpay;

import com.algaworks.algashop.billing.domain.model.invoice.payment.PaymentRequest;
import com.algaworks.algashop.billing.infrastructure.creditcard.fastpay.FastpayCreditCardAPIClient;
import com.algaworks.algashop.billing.infrastructure.creditcard.fastpay.FastpayCreditCardInput;
import com.algaworks.algashop.billing.infrastructure.creditcard.fastpay.FastpayCreditCardResponse;
import com.algaworks.algashop.billing.presentation.BadGatewayException;
import com.algaworks.algashop.billing.presentation.GatewayTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.circuitbreaker.retry.FrameworkRetryCircuitBreaker;
import org.springframework.cloud.circuitbreaker.retry.FrameworkRetryConfig;
import org.springframework.cloud.circuitbreaker.retry.FrameworkRetryConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.core.retry.RetryException;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.util.UUID;

@Component
@Slf4j
public class ResilientFastPayPaymentAPIClient {

    private final FastPaymentAPIClient fastpayPaymentAPIClient;
    private final FrameworkRetryCircuitBreaker circuitBreaker;


    public ResilientFastPayPaymentAPIClient(FastPaymentAPIClient fastpayPaymentAPIClient,
                                            CircuitBreakerFactory<FrameworkRetryConfig,
                                             FrameworkRetryConfigBuilder> circuitBreakerFactory) {
        this.fastpayPaymentAPIClient = fastpayPaymentAPIClient;
        this.circuitBreaker = (FrameworkRetryCircuitBreaker) circuitBreakerFactory.create("fastpayPaymentCB");
    }

    public FastpayPaymentModel capture(FastpayPaymentInput input) {

        try {
            return circuitBreaker.run(() -> {
                try {
                    return doCapture(input);
                } catch (RestClientException e) {
                    throw new FastpayPaymentCaptureFailed(
                            "Fail to capture payment of reference code %s"
                                    .formatted(input.getReferenceCode()), e);
                }
            });
        } catch (NoFallbackAvailableException e) {
            throw unwrapException(e);
        }
    }

    @ConcurrencyLimit(10)
    public FastpayPaymentModel findById(String paymentId) {
        log.info("Trying to find payment {} on Fastpay", paymentId);

        try {
            return circuitBreaker.run(() -> doFindById(paymentId));
        } catch (NoFallbackAvailableException e) {
            throw unwrapException(e);
        }
    }

    @ConcurrencyLimit(10)
    public void refund(String paymentId) {
        log.info("Trying to refund payment {} on Fastpay", paymentId);

        try {
            circuitBreaker.run(() -> {
                doRefund(paymentId);
                return Void.TYPE;
            });
        } catch (NoFallbackAvailableException e) {
            throw unwrapException(e);
        }
    }

    @ConcurrencyLimit(10)
    public void cancel(String paymentId) {
        log.info("Trying to cancel payment {} on Fastpay", paymentId);

        try {
            circuitBreaker.run(() -> {
                doCancel(paymentId);
                return Void.TYPE;
            });
        } catch (NoFallbackAvailableException e) {
            throw unwrapException(e);
        }
    }

    private FastpayPaymentModel doCapture(FastpayPaymentInput input) {
        try {
            return fastpayPaymentAPIClient.capture(input);
        } catch (RestClientException e) {
            throw translateException(e);
        }
    }

    private FastpayPaymentModel doFindById(String paymentId) {
        try {
            return fastpayPaymentAPIClient.findById(paymentId);
        } catch (RestClientException e) {
            throw translateException(e);
        }
    }

    private void doRefund(String paymentId) {
        try {
            fastpayPaymentAPIClient.refund(paymentId);
        } catch (RestClientException e) {
            throw translateException(e);
        }
    }

    private void doCancel(String paymentId) {
        try {
            fastpayPaymentAPIClient.cancel(paymentId);
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

    private FastpayPaymentModel doInternalFallback(FastpayPaymentInput input, Throwable ex) {
        log.error("Internal fallback triggered for FastPay API");
        throw new RuntimeException("FastPay API is currently unavailable", ex);
    }
}
