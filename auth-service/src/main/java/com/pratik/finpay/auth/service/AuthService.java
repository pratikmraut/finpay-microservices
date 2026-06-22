package com.pratik.finpay.auth.service;

import com.pratik.finpay.auth.dto.request.LoginRequest;
import com.pratik.finpay.auth.dto.request.RegisterRequest;
import com.pratik.finpay.auth.dto.response.AuthResponse;
import com.pratik.finpay.auth.dto.response.UserProfileResponse;
import com.pratik.finpay.auth.entity.Role;
import com.pratik.finpay.auth.entity.User;
import com.pratik.finpay.auth.exception.DuplicateResourceException;
import com.pratik.finpay.auth.exception.InvalidCredentialsException;
import com.pratik.finpay.auth.exception.ResourceNotFoundException;
import com.pratik.finpay.auth.repository.UserRepository;
import com.pratik.finpay.auth.security.JwtService;
import com.pratik.finpay.common.security.JwtConstants;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User with email already exists");
        }

        User user = new User(
                request.fullName().trim(),
                email,
                request.phone().trim(),
                passwordEncoder.encode(request.password()),
                Role.USER
        );

        return UserProfileResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(
                token,
                JwtConstants.TOKEN_TYPE,
                jwtService.getExpirationSeconds(),
                user.getId(),
                user.getEmail()
        );
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .map(UserProfileResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}