package com.pratik.finpay.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransferRequest(
        @NotNull(message = "Sender wallet id is required")
        Long senderWalletId,

        @NotNull(message = "Receiver wallet id is required")
        Long receiverWalletId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "INR", message = "Currency must be INR for v1")
        String currency,

        @NotBlank(message = "Idempotency key is required")
        @Size(min = 8, max = 120, message = "Idempotency key must be between 8 and 120 characters")
        String idempotencyKey
) {
}
