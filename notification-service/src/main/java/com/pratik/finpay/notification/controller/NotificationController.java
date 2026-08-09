package com.pratik.finpay.notification.controller;

import com.pratik.finpay.common.dto.ApiResponse;
import com.pratik.finpay.notification.dto.response.NotificationResponse;
import com.pratik.finpay.notification.security.AuthenticatedUser;
import com.pratik.finpay.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getAllNotifications(Authentication authentication) {
        AuthenticatedUser currentUser = (AuthenticatedUser) authentication.getDetails();
        return ResponseEntity.ok(ApiResponse.success(
                "Notifications fetched successfully",
                notificationService.getNotificationsForUser(currentUser.userId())
        ));
    }

    @GetMapping("/payment/{paymentReference}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getByPaymentReference(
            @PathVariable String paymentReference,
            Authentication authentication
    ) {
        AuthenticatedUser currentUser = (AuthenticatedUser) authentication.getDetails();
        return ResponseEntity.ok(ApiResponse.success(
                "Payment notifications fetched successfully",
                notificationService.getByPaymentReferenceForUser(paymentReference, currentUser.userId())
        ));
    }
}
