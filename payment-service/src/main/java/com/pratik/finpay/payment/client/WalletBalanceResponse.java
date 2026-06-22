package com.pratik.finpay.payment.client;

import com.pratik.finpay.common.enums.WalletStatus;

import java.math.BigDecimal;

public record WalletBalanceResponse(
        Long walletId,
        String walletNumber,
        BigDecimal balance,
        String currency,
        WalletStatus status
) {
}
