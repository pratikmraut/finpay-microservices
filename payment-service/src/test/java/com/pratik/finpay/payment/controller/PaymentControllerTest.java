package com.pratik.finpay.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pratik.finpay.common.enums.PaymentStatus;
import com.pratik.finpay.payment.dto.request.TransferRequest;
import com.pratik.finpay.payment.dto.response.PaymentTransactionResponse;
import com.pratik.finpay.payment.dto.response.TransferResponse;
import com.pratik.finpay.payment.exception.BusinessException;
import com.pratik.finpay.payment.exception.GlobalExceptionHandler;
import com.pratik.finpay.payment.exception.ResourceNotFoundException;
import com.pratik.finpay.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerTest {

    private FakePaymentService paymentService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        paymentService = new FakePaymentService();
        objectMapper = Jackson2ObjectMapperBuilder.json().build();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PaymentController(paymentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void transferShouldReturnOk() throws Exception {
        paymentService.transferResponse = transferResponse(PaymentStatus.SUCCESS);

        mockMvc.perform(post("/api/v1/payments/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTransferRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    void invalidAmountShouldReturnBadRequest() throws Exception {
        TransferRequest request = new TransferRequest(1L, 2L, BigDecimal.ZERO, "INR", "idem-12345");

        mockMvc.perform(post("/api/v1/payments/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void missingIdempotencyKeyShouldReturnBadRequest() throws Exception {
        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("500.00"), "INR", "");

        mockMvc.perform(post("/api/v1/payments/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void sameWalletTransferShouldReturnBadRequest() throws Exception {
        paymentService.runtimeException = new BusinessException("SAME_WALLET_TRANSFER", "Sender and receiver wallet must be different");
        TransferRequest request = new TransferRequest(1L, 1L, new BigDecimal("500.00"), "INR", "idem-12345");

        mockMvc.perform(post("/api/v1/payments/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SAME_WALLET_TRANSFER"));
    }

    @Test
    void getPaymentShouldReturnOk() throws Exception {
        paymentService.transactionResponse = transactionResponse();

        mockMvc.perform(get("/api/v1/payments/PAY-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentReference").value("PAY-123"));
    }

    @Test
    void getUnknownPaymentShouldReturnNotFound() throws Exception {
        paymentService.runtimeException = new ResourceNotFoundException("Payment transaction not found");

        mockMvc.perform(get("/api/v1/payments/PAY-UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void getWalletTransactionsShouldReturnList() throws Exception {
        paymentService.transactionResponses = List.of(transactionResponse());

        mockMvc.perform(get("/api/v1/wallets/1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].paymentReference").value("PAY-123"));
    }

    private TransferRequest validTransferRequest() {
        return new TransferRequest(1L, 2L, new BigDecimal("500.00"), "INR", "idem-12345");
    }

    private TransferResponse transferResponse(PaymentStatus status) {
        return new TransferResponse("PAY-123", 1L, 2L, new BigDecimal("500.00"), "INR", status, "Payment completed successfully");
    }

    private PaymentTransactionResponse transactionResponse() {
        return new PaymentTransactionResponse(
                "PAY-123",
                1L,
                2L,
                new BigDecimal("500.00"),
                "INR",
                PaymentStatus.SUCCESS,
                null,
                "idem-12345",
                null,
                null
        );
    }

    private static class FakePaymentService extends PaymentService {

        private TransferResponse transferResponse;
        private PaymentTransactionResponse transactionResponse;
        private List<PaymentTransactionResponse> transactionResponses = List.of();
        private RuntimeException runtimeException;

        FakePaymentService() {
            super(null, null, null, null);
        }

        @Override
        public TransferResponse transfer(TransferRequest request) {
            throwIfNeeded();
            return transferResponse;
        }

        @Override
        public PaymentTransactionResponse getPayment(String paymentReference) {
            throwIfNeeded();
            return transactionResponse;
        }

        @Override
        public List<PaymentTransactionResponse> getWalletTransactions(Long walletId) {
            throwIfNeeded();
            return transactionResponses;
        }

        private void throwIfNeeded() {
            if (runtimeException != null) {
                throw runtimeException;
            }
        }
    }
}
