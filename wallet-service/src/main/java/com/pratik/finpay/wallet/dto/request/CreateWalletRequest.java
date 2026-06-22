package com.pratik.finpay.wallet.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CreateWalletRequest(
        @NotNull(message = "User id is required")
        Long userId,

        @NotNull(message = "Initial balance is required")
        @DecimalMin(value = "0.00", message = "Initial balance must not be negative")
        BigDecimal initialBalance,

        @Pattern(regexp = "INR", message = "Currency must be INR for v1")
        String currency
) {
}
