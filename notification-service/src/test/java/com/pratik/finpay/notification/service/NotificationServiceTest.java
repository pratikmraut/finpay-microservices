package com.pratik.finpay.notification.service;

import com.pratik.finpay.common.enums.PaymentEventType;
import com.pratik.finpay.common.enums.PaymentStatus;
import com.pratik.finpay.common.event.PaymentEvent;
import com.pratik.finpay.notification.dto.response.NotificationResponse;
import com.pratik.finpay.notification.entity.Notification;
import com.pratik.finpay.notification.entity.NotificationStatus;
import com.pratik.finpay.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private WalletLookupClient walletLookupClient;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, walletLookupClient);
    }

    @Test
    void consumeCompletedEventShouldCreateNotification() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> withIdentity(invocation.getArgument(0)));

        NotificationResponse response = notificationService.createFromPaymentEvent(event(PaymentEventType.PAYMENT_COMPLETED));

        assertEquals("PAY-123", response.paymentReference());
        assertEquals(1L, response.walletId());
        assertEquals(NotificationStatus.CREATED, response.status());
        assertTrue(response.message().contains("completed successfully"));
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void consumeFailedEventShouldCreateFailedNotificationMessage() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> withIdentity(invocation.getArgument(0)));

        NotificationResponse response = notificationService.createFromPaymentEvent(event(PaymentEventType.PAYMENT_FAILED));

        assertTrue(response.message().contains("failed"));
    }

    @Test
    void consumeCompensatedEventShouldCreateCompensatedNotificationMessage() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> withIdentity(invocation.getArgument(0)));

        NotificationResponse response = notificationService.createFromPaymentEvent(event(PaymentEventType.PAYMENT_COMPENSATED));

        assertTrue(response.message().contains("compensated"));
    }

    @Test
    void duplicateEventShouldReturnExistingNotification() {
        Notification existing = withIdentity(new Notification(
                "event-1",
                "PAY-123",
                null,
                2L,
                "Existing notification",
                NotificationStatus.CREATED
        ));
        when(notificationRepository.findByEventId("event-1")).thenReturn(Optional.of(existing));

        NotificationResponse response = notificationService.createFromPaymentEvent(event(PaymentEventType.PAYMENT_COMPLETED));

        assertEquals("Existing notification", response.message());
    }

    @Test
    void getNotificationsByPaymentReferenceShouldReturnResults() {
        Notification notification = withIdentity(new Notification(
                "event-1",
                "PAY-123",
                null,
                2L,
                "Payment PAY-123 completed successfully for amount INR 500.00.",
                NotificationStatus.CREATED
        ));
        when(walletLookupClient.findWalletIdForUser(7L)).thenReturn(Optional.of(2L));
        when(notificationRepository.findByPaymentReferenceAndWalletIdOrderByCreatedAtDesc("PAY-123", 2L))
                .thenReturn(List.of(notification));

        List<NotificationResponse> responses = notificationService.getByPaymentReferenceForUser("PAY-123", 7L);

        assertEquals(1, responses.size());
        assertEquals("PAY-123", responses.get(0).paymentReference());
    }

    @Test
    void getNotificationsShouldOnlyQueryCurrentUsersWallet() {
        Notification notification = withIdentity(new Notification(
                "event-1:sender",
                "PAY-123",
                null,
                2L,
                "Payment completed",
                NotificationStatus.CREATED
        ));
        when(walletLookupClient.findWalletIdForUser(7L)).thenReturn(Optional.of(2L));
        when(notificationRepository.findByWalletIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(notification));

        List<NotificationResponse> responses = notificationService.getNotificationsForUser(7L);

        assertEquals(1, responses.size());
        assertEquals(2L, responses.get(0).walletId());
        verify(notificationRepository).findByWalletIdOrderByCreatedAtDesc(2L);
    }

    @Test
    void userWithoutWalletShouldReceiveEmptyNotificationList() {
        when(walletLookupClient.findWalletIdForUser(7L)).thenReturn(Optional.empty());

        List<NotificationResponse> responses = notificationService.getNotificationsForUser(7L);

        assertTrue(responses.isEmpty());
    }

    private PaymentEvent event(PaymentEventType eventType) {
        PaymentStatus status = switch (eventType) {
            case PAYMENT_COMPLETED -> PaymentStatus.SUCCESS;
            case PAYMENT_COMPENSATED -> PaymentStatus.COMPENSATED;
            case PAYMENT_FAILED -> PaymentStatus.FAILED;
            case PAYMENT_INITIATED -> PaymentStatus.PENDING;
        };
        return new PaymentEvent(
                "event-1",
                "PAY-123",
                1L,
                2L,
                new BigDecimal("500.00"),
                "INR",
                status,
                eventType,
                "test event",
                Instant.parse("2026-06-21T00:00:00Z")
        );
    }

    private Notification withIdentity(Notification notification) {
        ReflectionTestUtils.setField(notification, "id", 1L);
        ReflectionTestUtils.setField(notification, "createdAt", Instant.parse("2026-06-21T00:00:00Z"));
        return notification;
    }
}
