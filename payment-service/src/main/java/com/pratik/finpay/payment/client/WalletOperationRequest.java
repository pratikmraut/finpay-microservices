package com.pratik.finpay.payment.client;

import java.math.BigDecimal;

public record WalletOperationRequest(
        BigDecimal amount,
        String currency,
        String reference
) {
}
