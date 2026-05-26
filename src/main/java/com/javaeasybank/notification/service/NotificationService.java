package com.javaeasybank.notification.service;

import com.javaeasybank.notification.dto.NotificationResponseDTO;
import com.javaeasybank.notification.dto.NotificationUnreadCountResponseDTO;
import com.javaeasybank.notification.enums.NotificationType;

import java.util.List;

public interface NotificationService {

    NotificationResponseDTO createNotification(String customerId,
                                               NotificationType type,
                                               String title,
                                               String message,
                                               String linkUrl);

    List<NotificationResponseDTO> getUserNotifications(String customerId);

    NotificationUnreadCountResponseDTO getUnreadCount(String customerId);

    NotificationResponseDTO markAsRead(Long notificationId, String customerId);

    int markAllAsRead(String customerId);
}
