package com.javaeasybank.notification.service;

import com.javaeasybank.notification.dto.NotificationPreferenceResponseDTO;
import com.javaeasybank.notification.enums.NotificationType;

import java.util.List;

public interface NotificationPreferenceService {

    List<NotificationPreferenceResponseDTO> getPreferences(String customerId);

    NotificationPreferenceResponseDTO updatePreference(String customerId, NotificationType type, boolean enabled);

    boolean isNotificationEnabled(String customerId, NotificationType type);
}
