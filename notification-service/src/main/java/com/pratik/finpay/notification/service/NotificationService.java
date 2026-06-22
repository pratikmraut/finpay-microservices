package com.pratik.finpay.notification.service;

import com.pratik.finpay.common.enums.PaymentEventType;
import com.pratik.finpay.common.event.PaymentEvent;
import com.pratik.finpay.notification.dto.response.NotificationResponse;
import com.pratik.finpay.notification.entity.Notification;
import com.pratik.finpay.notification.entity.NotificationStatus;
import com.pratik.finpay.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public NotificationResponse createFromPaymentEvent(PaymentEvent event) {
        if (notificationRepository.existsByEventId(event.eventId())) {
            return notificationRepository.findByEventId(event.eventId())
                    .map(NotificationResponse::from)
                    .orElseThrow();
        }

        Notification notification = new Notification(
                event.eventId(),
                event.paymentReference(),
                null,
                event.receiverWalletId(),
                buildMessage(event),
                NotificationStatus.CREATED
        );

        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getByPaymentReference(String paymentReference) {
        return notificationRepository.findByPaymentReferenceOrderByCreatedAtDesc(paymentReference)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    private String buildMessage(PaymentEvent event) {
        String amount = event.currency() + " " + event.amount();
        if (event.eventType() == PaymentEventType.PAYMENT_COMPLETED) {
            return "Payment " + event.paymentReference() + " completed successfully for amount " + amount + ".";
        }
        if (event.eventType() == PaymentEventType.PAYMENT_COMPENSATED) {
            return "Payment " + event.paymentReference() + " was compensated for amount " + amount + ".";
        }
        if (event.eventType() == PaymentEventType.PAYMENT_FAILED) {
            return "Payment " + event.paymentReference() + " failed for amount " + amount + ".";
        }
        return "Payment " + event.paymentReference() + " event received for amount " + amount + ".";
    }
}
