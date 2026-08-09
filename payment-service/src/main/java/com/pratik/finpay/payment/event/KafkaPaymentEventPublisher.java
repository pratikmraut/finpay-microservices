package com.pratik.finpay.payment.event;

import com.pratik.finpay.common.event.PaymentEvent;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile({"docker", "ui-local"})
public class KafkaPaymentEventPublisher implements PaymentEventPublisher, SmartInitializingSingleton {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private final String paymentEventsTopic;

    public KafkaPaymentEventPublisher(
            KafkaTemplate<String, PaymentEvent> kafkaTemplate,
            @Value("${finpay.kafka.payment-events-topic:payment-events}") String paymentEventsTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.paymentEventsTopic = paymentEventsTopic;
    }

    @Override
    public void publish(PaymentEvent event) {
        kafkaTemplate.send(paymentEventsTopic, event.paymentReference(), event);
    }

    @Override
    public void afterSingletonsInstantiated() {
        kafkaTemplate.partitionsFor(paymentEventsTopic);
    }
}
