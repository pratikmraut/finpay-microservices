package com.pratik.finpay.common.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyUtilsTest {

    @Test
    void isPositiveReturnsTrueForAmountGreaterThanZero() {
        assertTrue(MoneyUtils.isPositive(new BigDecimal("10.00")));
    }

    @Test
    void isPositiveReturnsFalseForZeroNegativeOrNull() {
        assertFalse(MoneyUtils.isPositive(BigDecimal.ZERO));
        assertFalse(MoneyUtils.isPositive(new BigDecimal("-1.00")));
        assertFalse(MoneyUtils.isPositive(null));
    }

    @Test
    void normalizeRoundsToTwoDecimalPlaces() {
        assertEquals(new BigDecimal("10.13"), MoneyUtils.normalize(new BigDecimal("10.125")));
    }

    @Test
    void requirePositiveRejectsInvalidAmount() {
        assertThrows(IllegalArgumentException.class, () -> MoneyUtils.requirePositive(BigDecimal.ZERO, "amount"));
    }

    @Test
    void normalizeCurrencyTrimsAndUppercases() {
        assertEquals("INR", MoneyUtils.normalizeCurrency(" inr "));
    }
}