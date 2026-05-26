package com.javaeasybank.notification.dto;

import com.javaeasybank.notification.entity.Notification;
import com.javaeasybank.notification.enums.NotificationType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NotificationResponseDTO {

    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private String linkUrl;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponseDTO fromEntity(Notification notification) {
        NotificationResponseDTO dto = new NotificationResponseDTO();
        dto.setId(notification.getId());
        dto.setType(notification.getType());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setLinkUrl(notification.getLinkUrl());
        dto.setIsRead(notification.getIsRead());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }
}
