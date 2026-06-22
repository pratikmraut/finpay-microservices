package com.pratik.finpay.common.event;

import com.pratik.finpay.common.enums.PaymentEventType;
import com.pratik.finpay.common.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentEvent(
        String eventId,
        String paymentReference,
        Long senderWalletId,
        Long receiverWalletId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        PaymentEventType eventType,
        String message,
        Instant createdAt
) {
}