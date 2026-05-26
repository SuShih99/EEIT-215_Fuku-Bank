package com.javaeasybank.notification.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationUnreadCountResponseDTO {
    private Long count;

    public NotificationUnreadCountResponseDTO() {
    }

    public NotificationUnreadCountResponseDTO(Long count) {
        this.count = count;
    }
}
