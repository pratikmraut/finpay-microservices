package com.pratik.finpay.notification.controller;

import com.pratik.finpay.common.dto.ApiResponse;
import com.pratik.finpay.common.event.PaymentEvent;
import com.pratik.finpay.notification.consumer.PaymentEventConsumer;
import com.pratik.finpay.notification.dto.response.NotificationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/notifications")
public class InternalNotificationController {

    private final PaymentEventConsumer paymentEventConsumer;

    public InternalNotificationController(PaymentEventConsumer paymentEventConsumer) {
        this.paymentEventConsumer = paymentEventConsumer;
    }

    @PostMapping("/payment-events")
    public ResponseEntity<ApiResponse<NotificationResponse>> handlePaymentEvent(@RequestBody PaymentEvent event) {
        NotificationResponse response = paymentEventConsumer.handlePaymentEvent(event);
        return ResponseEntity.ok(ApiResponse.success("Payment event consumed successfully", response));
    }
}
