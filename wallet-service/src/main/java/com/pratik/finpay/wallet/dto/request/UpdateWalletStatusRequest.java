package com.pratik.finpay.wallet.dto.request;

import com.pratik.finpay.common.enums.WalletStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateWalletStatusRequest(
        @NotNull(message = "Wallet status is required")
        WalletStatus status
) {
}
