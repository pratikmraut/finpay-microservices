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
import java.util.Objects;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final WalletLookupClient walletLookupClient;

    public NotificationService(
            NotificationRepository notificationRepository,
            WalletLookupClient walletLookupClient
    ) {
        this.notificationRepository = notificationRepository;
        this.walletLookupClient = walletLookupClient;
    }

    @Transactional
    public NotificationResponse createFromPaymentEvent(PaymentEvent event) {
        var legacyNotification = notificationRepository.findByEventId(event.eventId());
        if (legacyNotification.isPresent()) {
            return NotificationResponse.from(legacyNotification.get());
        }

        NotificationResponse senderNotification = findOrCreateNotification(
                event,
                "sender",
                event.senderWalletId(),
                buildMessage(event, true)
        );

        if (shouldNotifyReceiver(event)
                && !Objects.equals(event.senderWalletId(), event.receiverWalletId())) {
            findOrCreateNotification(
                    event,
                    "receiver",
                    event.receiverWalletId(),
                    buildMessage(event, false)
            );
        }

        return senderNotification;
    }

    private NotificationResponse findOrCreateNotification(
            PaymentEvent event,
            String audience,
            Long walletId,
            String message
    ) {
        String audienceEventId = event.eventId() + ":" + audience;
        return notificationRepository.findByEventId(audienceEventId)
                .map(NotificationResponse::from)
                .orElseGet(() -> NotificationResponse.from(notificationRepository.save(new Notification(
                audienceEventId,
                event.paymentReference(),
                null,
                walletId,
                message,
                NotificationStatus.CREATED
        ))));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForUser(Long userId) {
        return walletLookupClient.findWalletIdForUser(userId)
                .map(this::getNotificationsForWallet)
                .orElseGet(List::of);
    }

    private List<NotificationResponse> getNotificationsForWallet(Long walletId) {
        return notificationRepository.findByWalletIdOrderByCreatedAtDesc(walletId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getByPaymentReferenceForUser(String paymentReference, Long userId) {
        return walletLookupClient.findWalletIdForUser(userId)
                .map(walletId -> getByPaymentReferenceForWallet(paymentReference, walletId))
                .orElseGet(List::of);
    }

    private List<NotificationResponse> getByPaymentReferenceForWallet(String paymentReference, Long walletId) {
        return notificationRepository.findByPaymentReferenceAndWalletIdOrderByCreatedAtDesc(paymentReference, walletId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    private boolean shouldNotifyReceiver(PaymentEvent event) {
        return event.eventType() == PaymentEventType.PAYMENT_COMPLETED
                || event.eventType() == PaymentEventType.PAYMENT_COMPENSATED;
    }

    private String buildMessage(PaymentEvent event, boolean sender) {
        String amount = event.currency() + " " + event.amount();
        if (event.eventType() == PaymentEventType.PAYMENT_COMPLETED) {
            String movement = sender
                    ? "You sent " + amount + " to wallet #" + event.receiverWalletId() + "."
                    : "You received " + amount + " from wallet #" + event.senderWalletId() + ".";
            return "Payment " + event.paymentReference() + " completed successfully. " + movement;
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
