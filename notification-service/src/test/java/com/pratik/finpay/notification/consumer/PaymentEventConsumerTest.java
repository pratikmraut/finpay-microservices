package com.pratik.finpay.notification.consumer;

import com.pratik.finpay.common.enums.PaymentEventType;
import com.pratik.finpay.common.enums.PaymentStatus;
import com.pratik.finpay.common.event.PaymentEvent;
import com.pratik.finpay.notification.dto.response.NotificationResponse;
import com.pratik.finpay.notification.entity.NotificationStatus;
import com.pratik.finpay.notification.service.NotificationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentEventConsumerTest {

    @Test
    void handlePaymentEventShouldCreateNotification() {
        FakeNotificationService notificationService = new FakeNotificationService();
        notificationService.response = new NotificationResponse(
                1L,
                "event-1",
                "PAY-123",
                null,
                2L,
                "Payment PAY-123 completed successfully for amount INR 500.00.",
                NotificationStatus.CREATED,
                null
        );
        PaymentEventConsumer consumer = new PaymentEventConsumer(notificationService);

        NotificationResponse response = consumer.handlePaymentEvent(event());

        assertEquals("PAY-123", response.paymentReference());
        assertEquals("event-1", notificationService.receivedEvent.eventId());
    }

    private PaymentEvent event() {
        return new PaymentEvent(
                "event-1",
                "PAY-123",
                1L,
                2L,
                new BigDecimal("500.00"),
                "INR",
                PaymentStatus.SUCCESS,
                PaymentEventType.PAYMENT_COMPLETED,
                "test event",
                Instant.parse("2026-06-21T00:00:00Z")
        );
    }

    private static class FakeNotificationService extends NotificationService {

        private NotificationResponse response;
        private PaymentEvent receivedEvent;

        FakeNotificationService() {
            super(null);
        }

        @Override
        public NotificationResponse createFromPaymentEvent(PaymentEvent event) {
            this.receivedEvent = event;
            return response;
        }
    }
}
