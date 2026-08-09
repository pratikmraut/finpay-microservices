package com.pratik.finpay.payment.service;

import com.pratik.finpay.common.enums.PaymentEventType;
import com.pratik.finpay.common.enums.PaymentStatus;
import com.pratik.finpay.common.event.PaymentEvent;
import com.pratik.finpay.payment.event.KafkaPaymentEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaPaymentEventPublisherTest {

    @Mock
    private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @Test
    void shouldWarmKafkaMetadataBeforeServingPayments() {
        KafkaPaymentEventPublisher publisher = new KafkaPaymentEventPublisher(kafkaTemplate, "payment-events");

        publisher.afterSingletonsInstantiated();

        verify(kafkaTemplate).partitionsFor("payment-events");
    }

    @Test
    void shouldPublishPaymentEventUsingPaymentReferenceAsKey() {
        KafkaPaymentEventPublisher publisher = new KafkaPaymentEventPublisher(kafkaTemplate, "payment-events");
        PaymentEvent event = new PaymentEvent(
                "event-1",
                "PAY-1",
                1L,
                2L,
                new BigDecimal("10.00"),
                "INR",
                PaymentStatus.SUCCESS,
                PaymentEventType.PAYMENT_COMPLETED,
                "Payment completed successfully",
                Instant.parse("2026-08-09T00:00:00Z")
        );

        publisher.publish(event);

        verify(kafkaTemplate).send("payment-events", "PAY-1", event);
    }
}
