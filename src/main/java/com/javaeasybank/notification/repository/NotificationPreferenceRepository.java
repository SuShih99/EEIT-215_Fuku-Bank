package com.javaeasybank.notification.repository;

import com.javaeasybank.notification.entity.NotificationPreference;
import com.javaeasybank.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    List<NotificationPreference> findByCustomerId(String customerId);

    Optional<NotificationPreference> findByCustomerIdAndType(String customerId, NotificationType type);

    boolean existsByCustomerIdAndType(String customerId, NotificationType type);
}
