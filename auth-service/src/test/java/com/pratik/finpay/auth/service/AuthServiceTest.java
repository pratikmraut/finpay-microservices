package com.pratik.finpay.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pratik.finpay.auth.dto.request.LoginRequest;
import com.pratik.finpay.auth.dto.request.RegisterRequest;
import com.pratik.finpay.auth.dto.response.AuthResponse;
import com.pratik.finpay.auth.dto.response.UserProfileResponse;
import com.pratik.finpay.auth.entity.Role;
import com.pratik.finpay.auth.entity.User;
import com.pratik.finpay.auth.exception.DuplicateResourceException;
import com.pratik.finpay.auth.exception.InvalidCredentialsException;
import com.pratik.finpay.auth.repository.UserRepository;
import com.pratik.finpay.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtService jwtService = new JwtService(
                "test-secret-value-that-is-long-enough-for-local-tests",
                60,
                new ObjectMapper()
        );
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerShouldCreateUser() {
        RegisterRequest request = new RegisterRequest("Pratik Raut", "Pratik@Example.com", "9999999999", "Pass@123");
        User savedUser = user("pratik@example.com");

        when(userRepository.existsByEmail("pratik@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Pass@123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserProfileResponse response = authService.register(request);

        assertEquals(1L, response.userId());
        assertEquals("pratik@example.com", response.email());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerDuplicateEmailShouldFail() {
        RegisterRequest request = new RegisterRequest("Pratik Raut", "pratik@example.com", "9999999999", "Pass@123");
        when(userRepository.existsByEmail("pratik@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginValidCredentialsShouldReturnToken() {
        LoginRequest request = new LoginRequest("pratik@example.com", "Pass@123");
        User user = user("pratik@example.com");

        when(userRepository.findByEmail("pratik@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Pass@123", "hashed-password")).thenReturn(true);

        AuthResponse response = authService.login(request);

        assertNotNull(response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(1L, response.userId());
        assertEquals("pratik@example.com", response.email());
    }

    @Test
    void loginInvalidPasswordShouldFail() {
        LoginRequest request = new LoginRequest("pratik@example.com", "wrong-password");
        User user = user("pratik@example.com");

        when(userRepository.findByEmail("pratik@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    private User user(String email) {
        User user = new User("Pratik Raut", email, "9999999999", "hashed-password", Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "createdAt", Instant.parse("2026-06-21T00:00:00Z"));
        ReflectionTestUtils.setField(user, "updatedAt", Instant.parse("2026-06-21T00:00:00Z"));
        return user;
    }
}