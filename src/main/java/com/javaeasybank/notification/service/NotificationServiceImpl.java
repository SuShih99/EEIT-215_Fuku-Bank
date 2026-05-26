package com.javaeasybank.notification.service;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.notification.dto.NotificationResponseDTO;
import com.javaeasybank.notification.dto.NotificationUnreadCountResponseDTO;
import com.javaeasybank.notification.entity.Notification;
import com.javaeasybank.notification.enums.NotificationType;
import com.javaeasybank.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceService notificationPreferenceService;

    @Override
    @Transactional
    public NotificationResponseDTO createNotification(String customerId,
                                                      NotificationType type,
                                                      String title,
                                                      String message,
                                                      String linkUrl) {
        if (type != NotificationType.SECURITY
                && !notificationPreferenceService.isNotificationEnabled(customerId, type)) {
            return null;
        }

        Notification notification = new Notification();
        notification.setCustomerId(customerId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLinkUrl(linkUrl);
        notification.setIsRead(false);
        notification.setReadAt(null);
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);
        return NotificationResponseDTO.fromEntity(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getUserNotifications(String customerId) {
        return notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(NotificationResponseDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationUnreadCountResponseDTO getUnreadCount(String customerId) {
        return new NotificationUnreadCountResponseDTO(notificationRepository.countByCustomerIdAndIsReadFalse(customerId));
    }

    @Override
    @Transactional
    public NotificationResponseDTO markAsRead(Long notificationId, String customerId) {
        Notification notification = notificationRepository.findByIdAndCustomerId(notificationId, customerId)
                .orElseThrow(() -> new BusinessException("找不到通知或無權限存取"));

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }

        return NotificationResponseDTO.fromEntity(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead(String customerId) {
        List<Notification> notifications = notificationRepository.findByCustomerIdAndIsReadFalseOrderByCreatedAtDesc(customerId);
        int updated = 0;

        for (Notification notification : notifications) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
            updated++;
        }

        return updated;
    }
}
