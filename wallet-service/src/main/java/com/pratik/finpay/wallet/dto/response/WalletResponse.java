package com.pratik.finpay.wallet.dto.response;

import com.pratik.finpay.common.enums.WalletStatus;
import com.pratik.finpay.wallet.entity.Wallet;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public record WalletResponse(
        Long walletId,
        String walletNumber,
        Long userId,
        BigDecimal balance,
        String currency,
        WalletStatus status,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getWalletNumber(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getStatus(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }
}
