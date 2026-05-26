package com.javaeasybank.notification.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.javaeasybank.notification.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, length = 20)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String title;

    @Column(nullable = false, length = 1000, columnDefinition = "NVARCHAR(1000)")
    private String message;

    @Column(name = "link_url", length = 255)
    private String linkUrl;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isRead == null) {
            this.isRead = false;
        }
        if (Boolean.FALSE.equals(this.isRead)) {
            this.readAt = null;
        }
    }
}
