package com.pratik.finpay.auth.security;

import com.pratik.finpay.auth.entity.Role;

public record AuthenticatedUser(
        Long userId,
        String email,
        Role role
) {
}