package com.pratik.finpay.payment.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Profile("!docker")
public class LocalIdempotencyStore implements IdempotencyStore {

    private final ConcurrentMap<String, String> paymentReferences = new ConcurrentHashMap<>();

    @Override
    public Optional<String> getPaymentReference(String idempotencyKey) {
        return Optional.ofNullable(paymentReferences.get(idempotencyKey));
    }

    @Override
    public void savePaymentReference(String idempotencyKey, String paymentReference) {
        paymentReferences.put(idempotencyKey, paymentReference);
    }
}
