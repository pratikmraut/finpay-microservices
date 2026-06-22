package com.pratik.finpay.notification.dto.response;

import com.pratik.finpay.notification.entity.Notification;
import com.pratik.finpay.notification.entity.NotificationStatus;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String eventId,
        String paymentReference,
        Long userId,
        Long walletId,
        String message,
        NotificationStatus status,
        Instant createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getEventId(),
                notification.getPaymentReference(),
                notification.getUserId(),
                notification.getWalletId(),
                notification.getMessage(),
                notification.getStatus(),
                notification.getCreatedAt()
        );
    }
}
