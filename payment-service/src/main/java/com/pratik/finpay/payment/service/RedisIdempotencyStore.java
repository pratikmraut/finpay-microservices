package com.pratik.finpay.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@Profile("docker")
public class RedisIdempotencyStore implements IdempotencyStore {

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;
    private final Duration ttl;

    public RedisIdempotencyStore(
            StringRedisTemplate redisTemplate,
            @Value("${finpay.redis.idempotency-key-prefix:payment:idempotency:}") String keyPrefix,
            @Value("${finpay.redis.idempotency-ttl-hours:24}") long ttlHours
    ) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
        this.ttl = Duration.ofHours(ttlHours);
    }

    @Override
    public Optional<String> getPaymentReference(String idempotencyKey) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(redisKey(idempotencyKey)));
    }

    @Override
    public void savePaymentReference(String idempotencyKey, String paymentReference) {
        redisTemplate.opsForValue().set(redisKey(idempotencyKey), paymentReference, ttl);
    }

    private String redisKey(String idempotencyKey) {
        return keyPrefix + idempotencyKey;
    }
}
