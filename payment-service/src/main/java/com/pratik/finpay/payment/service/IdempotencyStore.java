package com.pratik.finpay.payment.service;

import java.util.Optional;

public interface IdempotencyStore {

    Optional<String> getPaymentReference(String idempotencyKey);

    void savePaymentReference(String idempotencyKey, String paymentReference);
}
