package com.pratik.finpay.wallet.dto.response;

import com.pratik.finpay.common.enums.WalletStatus;
import com.pratik.finpay.wallet.entity.Wallet;

import java.io.Serializable;
import java.math.BigDecimal;

public record WalletBalanceResponse(
        Long walletId,
        String walletNumber,
        BigDecimal balance,
        String currency,
        WalletStatus status
) implements Serializable {

    public static WalletBalanceResponse from(Wallet wallet) {
        return new WalletBalanceResponse(
                wallet.getId(),
                wallet.getWalletNumber(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getStatus()
        );
    }
}
