package com.javaeasybank.customer.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "CUSTOMER_DEVICE",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_CustomerDevice_Fingerprint",
                columnNames = {"customer_id", "device_fingerprint"}
        )
)
@Getter
@Setter
public class CustomerDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "customer_id", length = 20, nullable = false)
    private String customerId;

    @Column(name = "device_fingerprint", length = 64, nullable = false)
    private String deviceFingerprint;

    @Column(name = "device_name", length = 120, nullable = false)
    private String deviceName;

    @Column(name = "browser_name", length = 60)
    private String browserName;

    @Column(name = "operating_system", length = 60)
    private String operatingSystem;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "ACTIVE";

    @Column(name = "trusted", nullable = false)
    private Boolean trusted = true;

    @Column(name = "first_seen_at", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSeenAt;

    @Column(name = "revoked_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime revokedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (firstSeenAt == null) {
            firstSeenAt = now;
        }
        if (lastSeenAt == null) {
            lastSeenAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (lastSeenAt == null) {
            lastSeenAt = LocalDateTime.now();
        }
    }
}
