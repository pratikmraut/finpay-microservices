package com.pratik.finpay.payment.client;

import java.math.BigDecimal;

public interface WalletClient {

    WalletBalanceResponse debitWallet(Long walletId, BigDecimal amount, String currency, String reference);

    WalletBalanceResponse creditWallet(Long walletId, BigDecimal amount, String currency, String reference);
}
