package com.pratik.finpay.notification.consumer;

import com.pratik.finpay.common.event.PaymentEvent;
import com.pratik.finpay.notification.dto.response.NotificationResponse;
import com.pratik.finpay.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final NotificationService notificationService;

    public PaymentEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public NotificationResponse handlePaymentEvent(PaymentEvent event) {
        NotificationResponse response = notificationService.createFromPaymentEvent(event);
        log.info("Notification created for payment {}: {}", event.paymentReference(), response.message());
        return response;
    }
}
