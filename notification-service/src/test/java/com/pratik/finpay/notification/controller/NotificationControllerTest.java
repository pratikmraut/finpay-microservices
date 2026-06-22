package com.pratik.finpay.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pratik.finpay.notification.dto.response.NotificationResponse;
import com.pratik.finpay.notification.entity.NotificationStatus;
import com.pratik.finpay.notification.exception.GlobalExceptionHandler;
import com.pratik.finpay.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerTest {

    private FakeNotificationService notificationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        notificationService = new FakeNotificationService();
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new NotificationController(notificationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void getAllNotificationsShouldReturnResults() throws Exception {
        notificationService.responses = List.of(response());

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].paymentReference").value("PAY-123"));
    }

    @Test
    void getNotificationsByPaymentReferenceShouldReturnResults() throws Exception {
        notificationService.responses = List.of(response());

        mockMvc.perform(get("/api/v1/notifications/payment/PAY-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].paymentReference").value("PAY-123"));
    }

    private NotificationResponse response() {
        return new NotificationResponse(
                1L,
                "event-1",
                "PAY-123",
                null,
                2L,
                "Payment PAY-123 completed successfully for amount INR 500.00.",
                NotificationStatus.CREATED,
                null
        );
    }

    private static class FakeNotificationService extends NotificationService {

        private List<NotificationResponse> responses = List.of();

        FakeNotificationService() {
            super(null);
        }

        @Override
        public List<NotificationResponse> getAllNotifications() {
            return responses;
        }

        @Override
        public List<NotificationResponse> getByPaymentReference(String paymentReference) {
            return responses;
        }
    }
}
