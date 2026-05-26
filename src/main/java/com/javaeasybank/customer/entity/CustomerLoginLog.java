package com.javaeasybank.customer.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "CUSTOMER_LOGIN_LOG")
@Getter
@Setter
public class CustomerLoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_log_id")
    private Long loginLogId;

    @Column(name = "customer_id", length = 20)
    private String customerId;

    @Column(name = "username", length = 50, nullable = false)
    private String username;

    @Column(name = "result", length = 20, nullable = false, columnDefinition = "NVARCHAR(20)")
    private String result;

    @Column(name = "fail_reason", length = 200, columnDefinition = "NVARCHAR(200)")
    private String failReason;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "device_name", length = 120, columnDefinition = "NVARCHAR(120)")
    private String deviceName;

    @Column(name = "login_time", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;

    @PrePersist
    protected void onCreate() {
        if (loginTime == null) {
            loginTime = LocalDateTime.now();
        }
    }
}
