package com.javaeasybank.notification.dto;

import com.javaeasybank.notification.enums.NotificationType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationPreferenceUpdateRequest {

    private NotificationType type;
    private Boolean enabled;
}
