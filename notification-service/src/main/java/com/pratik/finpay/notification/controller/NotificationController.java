package com.pratik.finpay.notification.controller;

import com.pratik.finpay.common.dto.ApiResponse;
import com.pratik.finpay.notification.dto.response.NotificationResponse;
import com.pratik.finpay.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getAllNotifications() {
        return ResponseEntity.ok(ApiResponse.success(
                "Notifications fetched successfully",
                notificationService.getAllNotifications()
        ));
    }

    @GetMapping("/payment/{paymentReference}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getByPaymentReference(
            @PathVariable String paymentReference
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Payment notifications fetched successfully",
                notificationService.getByPaymentReference(paymentReference)
        ));
    }
}
