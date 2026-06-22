package com.pratik.finpay.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pratik.finpay.common.enums.PaymentEventType;
import com.pratik.finpay.common.enums.PaymentStatus;
import com.pratik.finpay.common.event.PaymentEvent;
import com.pratik.finpay.notification.consumer.PaymentEventConsumer;
import com.pratik.finpay.notification.dto.response.NotificationResponse;
import com.pratik.finpay.notification.entity.NotificationStatus;
import com.pratik.finpay.notification.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalNotificationControllerTest {

    private FakePaymentEventConsumer paymentEventConsumer;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        paymentEventConsumer = new FakePaymentEventConsumer();
        paymentEventConsumer.response = new NotificationResponse(
                1L,
                "event-1",
                "PAY-123",
                null,
                2L,
                "Payment PAY-123 completed successfully for amount INR 500.00.",
                NotificationStatus.CREATED,
                null
        );
        objectMapper = Jackson2ObjectMapperBuilder.json().build();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new InternalNotificationController(paymentEventConsumer))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void handlePaymentEventShouldReturnNotification() throws Exception {
        mockMvc.perform(post("/internal/v1/notifications/payment-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentReference").value("PAY-123"));
    }

    private PaymentEvent event() {
        return new PaymentEvent(
                "event-1",
                "PAY-123",
                1L,
                2L,
                new BigDecimal("500.00"),
                "INR",
                PaymentStatus.SUCCESS,
                PaymentEventType.PAYMENT_COMPLETED,
                "Payment completed successfully",
                Instant.parse("2026-06-21T00:00:00Z")
        );
    }

    private static class FakePaymentEventConsumer extends PaymentEventConsumer {

        private NotificationResponse response;

        FakePaymentEventConsumer() {
            super(null);
        }

        @Override
        public NotificationResponse handlePaymentEvent(PaymentEvent event) {
            return response;
        }
    }
}
