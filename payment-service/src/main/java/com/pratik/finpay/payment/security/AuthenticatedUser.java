package com.pratik.finpay.payment.security;

public record AuthenticatedUser(
        Long userId,
        String email,
        String role
) {
}
