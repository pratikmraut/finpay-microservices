package com.pratik.finpay.notification.security;

public record AuthenticatedUser(
        Long userId,
        String email,
        String role
) {
}
