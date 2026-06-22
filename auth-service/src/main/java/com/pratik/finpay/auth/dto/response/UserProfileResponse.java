package com.pratik.finpay.auth.dto.response;

import com.pratik.finpay.auth.entity.Role;
import com.pratik.finpay.auth.entity.User;

import java.time.Instant;

public record UserProfileResponse(
        Long userId,
        String fullName,
        String email,
        String phone,
        Role role,
        Instant createdAt
) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}