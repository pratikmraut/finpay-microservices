package com.pratik.finpay.payment.event;

import com.pratik.finpay.common.event.PaymentEvent;
import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Profile("!docker")
public class LoggingPaymentEventPublisher implements PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingPaymentEventPublisher.class);

    @Override
    public void publish(PaymentEvent event) {
        log.info("Payment event published locally: {}", event);
    }
}
