package com.javaeasybank.notification.service;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.notification.dto.NotificationPreferenceResponseDTO;
import com.javaeasybank.notification.entity.NotificationPreference;
import com.javaeasybank.notification.enums.NotificationType;
import com.javaeasybank.notification.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponseDTO> getPreferences(String customerId) {
        Map<NotificationType, NotificationPreference> preferenceMap = new LinkedHashMap<>();
        notificationPreferenceRepository.findByCustomerId(customerId)
                .forEach(pref -> preferenceMap.put(pref.getType(), pref));

        return Arrays.stream(NotificationType.values())
                .map(type -> {
                    NotificationPreference preference = preferenceMap.get(type);
                    boolean locked = type == NotificationType.SECURITY;
                    boolean enabled = locked || preference == null || Boolean.TRUE.equals(preference.getEnabled());
                    return NotificationPreferenceResponseDTO.fromType(type, enabled, locked,
                            preference != null ? preference.getUpdatedAt() : null);
                })
                .toList();
    }

    @Override
    @Transactional
    public NotificationPreferenceResponseDTO updatePreference(String customerId,
                                                              NotificationType type,
                                                              boolean enabled) {
        if (type == null) {
            throw new BusinessException("通知類型不可為空");
        }
        if (type == NotificationType.SECURITY && !enabled) {
            throw new BusinessException("安全通知不可關閉");
        }

        NotificationPreference preference = notificationPreferenceRepository
                .findByCustomerIdAndType(customerId, type)
                .orElseGet(NotificationPreference::new);

        preference.setCustomerId(customerId);
        preference.setType(type);
        preference.setEnabled(true);
        if (type != NotificationType.SECURITY) {
            preference.setEnabled(enabled);
        }

        NotificationPreference saved = notificationPreferenceRepository.save(preference);
        return NotificationPreferenceResponseDTO.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isNotificationEnabled(String customerId, NotificationType type) {
        if (type == NotificationType.SECURITY) {
            return true;
        }

        return notificationPreferenceRepository.findByCustomerIdAndType(customerId, type)
                .map(preference -> Boolean.TRUE.equals(preference.getEnabled()))
                .orElse(true);
    }
}
