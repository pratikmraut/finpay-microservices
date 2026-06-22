package com.pratik.finpay.wallet.security;

public record AuthenticatedUser(
        Long userId,
        String email,
        String role
) {
}
