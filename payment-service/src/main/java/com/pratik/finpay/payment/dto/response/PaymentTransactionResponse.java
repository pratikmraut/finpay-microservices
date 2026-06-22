package com.pratik.finpay.payment.dto.response;

import com.pratik.finpay.common.enums.PaymentStatus;
import com.pratik.finpay.payment.entity.PaymentTransaction;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentTransactionResponse(
        String paymentReference,
        Long senderWalletId,
        Long receiverWalletId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String failureReason,
        String idempotencyKey,
        Instant createdAt,
        Instant updatedAt
) {

    public static PaymentTransactionResponse from(PaymentTransaction transaction) {
        return new PaymentTransactionResponse(
                transaction.getPaymentReference(),
                transaction.getSenderWalletId(),
                transaction.getReceiverWalletId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getFailureReason(),
                transaction.getIdempotencyKey(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}
