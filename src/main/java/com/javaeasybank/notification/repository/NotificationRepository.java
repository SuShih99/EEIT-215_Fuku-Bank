package com.javaeasybank.notification.repository;

import com.javaeasybank.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<Notification> findByCustomerIdAndIsReadFalseOrderByCreatedAtDesc(String customerId);

    Long countByCustomerIdAndIsReadFalse(String customerId);

    Optional<Notification> findByIdAndCustomerId(Long id, String customerId);
}
