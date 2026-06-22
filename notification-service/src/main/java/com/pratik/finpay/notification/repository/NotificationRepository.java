package com.pratik.finpay.notification.repository;

import com.pratik.finpay.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByEventId(String eventId);

    Optional<Notification> findByEventId(String eventId);

    List<Notification> findByPaymentReferenceOrderByCreatedAtDesc(String paymentReference);

    List<Notification> findAllByOrderByCreatedAtDesc();
}
