package com.pratik.finpay.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {

    public static final String DEFAULT_CURRENCY = "INR";
    public static final int MONEY_SCALE = 2;

    private MoneyUtils() {
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public static BigDecimal requirePositive(BigDecimal amount, String fieldName) {
        if (!isPositive(amount)) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
        return normalize(amount);
    }

    public static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("amount must not be null");
        }
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public static boolean isSupportedCurrency(String currency) {
        return DEFAULT_CURRENCY.equalsIgnoreCase(currency);
    }

    public static String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
        return currency.trim().toUpperCase();
    }
}