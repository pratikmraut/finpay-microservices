package com.pratik.finpay.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pratik.finpay.common.enums.WalletStatus;
import com.pratik.finpay.wallet.dto.request.CreateWalletRequest;
import com.pratik.finpay.wallet.dto.request.UpdateWalletStatusRequest;
import com.pratik.finpay.wallet.dto.request.WalletOperationRequest;
import com.pratik.finpay.wallet.dto.response.WalletBalanceResponse;
import com.pratik.finpay.wallet.dto.response.WalletResponse;
import com.pratik.finpay.wallet.exception.BusinessException;
import com.pratik.finpay.wallet.exception.GlobalExceptionHandler;
import com.pratik.finpay.wallet.exception.ResourceNotFoundException;
import com.pratik.finpay.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WalletControllerTest {

    private FakeWalletService walletService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        walletService = new FakeWalletService();
        objectMapper = Jackson2ObjectMapperBuilder.json().build();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WalletController(walletService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void createWalletShouldReturnCreated() throws Exception {
        walletService.walletResponse = walletResponse(WalletStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateWalletRequest(
                                1L,
                                new BigDecimal("5000.00"),
                                "INR"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.walletId").value(1));
    }

    @Test
    void getWalletShouldReturnWallet() throws Exception {
        walletService.walletResponse = walletResponse(WalletStatus.ACTIVE);

        mockMvc.perform(get("/api/v1/wallets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.walletNumber").value("WALLET-TEST"));
    }

    @Test
    void getUnknownWalletShouldReturnNotFound() throws Exception {
        walletService.runtimeException = new ResourceNotFoundException("Wallet not found");

        mockMvc.perform(get("/api/v1/wallets/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void getBalanceShouldReturnBalance() throws Exception {
        walletService.balanceResponse = balanceResponse(new BigDecimal("5000.00"), WalletStatus.ACTIVE);

        mockMvc.perform(get("/api/v1/wallets/1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(5000.00));
    }

    @Test
    void updateStatusShouldReturnUpdatedWallet() throws Exception {
        walletService.walletResponse = walletResponse(WalletStatus.INACTIVE);

        mockMvc.perform(patch("/api/v1/wallets/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateWalletStatusRequest(WalletStatus.INACTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    void debitInvalidAmountShouldReturnBadRequest() throws Exception {
        WalletOperationRequest request = new WalletOperationRequest(BigDecimal.ZERO, "INR", "PAY-TEST");

        mockMvc.perform(post("/internal/v1/wallets/1/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void debitInsufficientBalanceShouldReturnBadRequest() throws Exception {
        walletService.runtimeException = new BusinessException("INSUFFICIENT_BALANCE", "Wallet does not have sufficient balance");

        mockMvc.perform(post("/internal/v1/wallets/1/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operation("250.00"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_BALANCE"));
    }

    @Test
    void creditShouldReturnUpdatedBalance() throws Exception {
        walletService.balanceResponse = balanceResponse(new BigDecimal("1250.00"), WalletStatus.ACTIVE);

        mockMvc.perform(post("/internal/v1/wallets/1/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operation("250.00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(1250.00));
    }

    private WalletResponse walletResponse(WalletStatus status) {
        return new WalletResponse(
                1L,
                "WALLET-TEST",
                1L,
                new BigDecimal("5000.00"),
                "INR",
                status,
                null,
                null
        );
    }

    private WalletBalanceResponse balanceResponse(BigDecimal balance, WalletStatus status) {
        return new WalletBalanceResponse(1L, "WALLET-TEST", balance, "INR", status);
    }

    private WalletOperationRequest operation(String amount) {
        return new WalletOperationRequest(new BigDecimal(amount), "INR", "PAY-TEST");
    }

    private static class FakeWalletService extends WalletService {

        private WalletResponse walletResponse;
        private WalletBalanceResponse balanceResponse;
        private RuntimeException runtimeException;

        FakeWalletService() {
            super(null);
        }

        @Override
        public WalletResponse createWallet(CreateWalletRequest request) {
            throwIfNeeded();
            return walletResponse;
        }

        @Override
        public WalletResponse getWallet(Long walletId) {
            throwIfNeeded();
            return walletResponse;
        }

        @Override
        public WalletBalanceResponse getBalance(Long walletId) {
            throwIfNeeded();
            return balanceResponse;
        }

        @Override
        public WalletResponse updateStatus(Long walletId, UpdateWalletStatusRequest request) {
            throwIfNeeded();
            return walletResponse;
        }

        @Override
        public WalletBalanceResponse debitWallet(Long walletId, WalletOperationRequest request) {
            throwIfNeeded();
            return balanceResponse;
        }

        @Override
        public WalletBalanceResponse creditWallet(Long walletId, WalletOperationRequest request) {
            throwIfNeeded();
            return balanceResponse;
        }

        private void throwIfNeeded() {
            if (runtimeException != null) {
                throw runtimeException;
            }
        }
    }
}
