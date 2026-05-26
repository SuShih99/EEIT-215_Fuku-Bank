package com.javaeasybank.notification.entity;

import com.javaeasybank.notification.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notification_preferences",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_NOTIFICATION_PREF",
                columnNames = {"customer_id", "notification_type"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, length = 20)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 40)
    private NotificationType type;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.updatedAt = LocalDateTime.now();
        if (this.enabled == null) {
            this.enabled = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.enabled == null) {
            this.enabled = true;
        }
    }
}
