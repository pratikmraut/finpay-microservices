package com.pratik.finpay.payment.dto.response;

import com.pratik.finpay.common.enums.PaymentStatus;
import com.pratik.finpay.payment.entity.PaymentTransaction;

import java.math.BigDecimal;

public record TransferResponse(
        String paymentReference,
        Long senderWalletId,
        Long receiverWalletId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String message
) {

    public static TransferResponse from(PaymentTransaction transaction, String message) {
        return new TransferResponse(
                transaction.getPaymentReference(),
                transaction.getSenderWalletId(),
                transaction.getReceiverWalletId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                message
        );
    }
}
