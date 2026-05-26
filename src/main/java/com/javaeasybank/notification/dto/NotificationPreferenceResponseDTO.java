package com.javaeasybank.notification.dto;

import com.javaeasybank.notification.entity.NotificationPreference;
import com.javaeasybank.notification.enums.NotificationType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NotificationPreferenceResponseDTO {

    private NotificationType type;
    private Boolean enabled;
    private Boolean locked;
    private String label;
    private LocalDateTime updatedAt;

    public static NotificationPreferenceResponseDTO fromType(NotificationType type,
                                                            boolean enabled,
                                                            boolean locked,
                                                            LocalDateTime updatedAt) {
        NotificationPreferenceResponseDTO dto = new NotificationPreferenceResponseDTO();
        dto.setType(type);
        dto.setEnabled(enabled);
        dto.setLocked(locked);
        dto.setLabel(labelOf(type));
        dto.setUpdatedAt(updatedAt);
        return dto;
    }

    public static NotificationPreferenceResponseDTO fromEntity(NotificationPreference preference) {
        boolean locked = preference.getType() == NotificationType.SECURITY;
        boolean enabled = locked || Boolean.TRUE.equals(preference.getEnabled());
        return fromType(preference.getType(), enabled, locked, preference.getUpdatedAt());
    }

    public static String labelOf(NotificationType type) {
        return switch (type) {
            case ACCOUNT_APPLICATION -> "開戶申請通知";
            case ACCOUNT_SUPPLEMENT_REQUIRED -> "開戶補件通知";
            case TRANSFER -> "轉帳通知";
            case LOAN -> "貸款通知";
            case CREDIT_CARD -> "信用卡通知";
            case PASSBOOK -> "電子存摺通知";
            case SECURITY -> "安全通知";
            case SYSTEM -> "系統通知";
        };
    }
}
