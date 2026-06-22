package com.pratik.finpay.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pratik.finpay.auth.dto.request.LoginRequest;
import com.pratik.finpay.auth.dto.request.RegisterRequest;
import com.pratik.finpay.auth.dto.response.AuthResponse;
import com.pratik.finpay.auth.dto.response.UserProfileResponse;
import com.pratik.finpay.auth.entity.Role;
import com.pratik.finpay.auth.exception.DuplicateResourceException;
import com.pratik.finpay.auth.exception.GlobalExceptionHandler;
import com.pratik.finpay.auth.exception.InvalidCredentialsException;
import com.pratik.finpay.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private FakeAuthService authService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        authService = new FakeAuthService();
        objectMapper = Jackson2ObjectMapperBuilder.json().build();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void registerShouldReturnCreated() throws Exception {
        RegisterRequest request = new RegisterRequest("Pratik Raut", "pratik@example.com", "9999999999", "Pass@123");
        authService.registerResponse = new UserProfileResponse(
                1L,
                "Pratik Raut",
                "pratik@example.com",
                "9999999999",
                Role.USER,
                null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("pratik@example.com"));
    }

    @Test
    void registerDuplicateEmailShouldReturnConflict() throws Exception {
        RegisterRequest request = new RegisterRequest("Pratik Raut", "pratik@example.com", "9999999999", "Pass@123");
        authService.registerException = new DuplicateResourceException("User with email already exists");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void loginShouldReturnToken() throws Exception {
        LoginRequest request = new LoginRequest("pratik@example.com", "Pass@123");
        authService.loginResponse = new AuthResponse("jwt-token", "Bearer", 3600, 1L, "pratik@example.com");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"));
    }

    @Test
    void loginInvalidPasswordShouldReturnUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest("pratik@example.com", "wrong-password");
        authService.loginException = new InvalidCredentialsException("Invalid email or password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void meShouldReturnCurrentUserProfile() throws Exception {
        authService.profileResponse = new UserProfileResponse(
                1L,
                "Pratik Raut",
                "pratik@example.com",
                "9999999999",
                Role.USER,
                null
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("pratik@example.com", null, List.of());

        mockMvc.perform(get("/api/v1/auth/me").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("pratik@example.com"));
    }

    private static class FakeAuthService extends AuthService {

        private UserProfileResponse registerResponse;
        private RuntimeException registerException;
        private AuthResponse loginResponse;
        private RuntimeException loginException;
        private UserProfileResponse profileResponse;

        FakeAuthService() {
            super(null, null, null);
        }

        @Override
        public UserProfileResponse register(RegisterRequest request) {
            if (registerException != null) {
                throw registerException;
            }
            return registerResponse;
        }

        @Override
        public AuthResponse login(LoginRequest request) {
            if (loginException != null) {
                throw loginException;
            }
            return loginResponse;
        }

        @Override
        public UserProfileResponse getProfileByEmail(String email) {
            return profileResponse;
        }
    }
}
