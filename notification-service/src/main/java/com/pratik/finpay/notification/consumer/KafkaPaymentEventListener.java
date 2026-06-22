package com.pratik.finpay.notification.consumer;

import com.pratik.finpay.common.event.PaymentEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Profile("docker")
public class KafkaPaymentEventListener {

    private final PaymentEventConsumer paymentEventConsumer;

    public KafkaPaymentEventListener(PaymentEventConsumer paymentEventConsumer) {
        this.paymentEventConsumer = paymentEventConsumer;
    }

    @KafkaListener(topics = "${finpay.kafka.payment-events-topic:payment-events}")
    public void onPaymentEvent(PaymentEvent event) {
        paymentEventConsumer.handlePaymentEvent(event);
    }
}
