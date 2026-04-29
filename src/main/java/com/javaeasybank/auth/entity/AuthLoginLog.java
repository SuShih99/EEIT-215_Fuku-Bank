package com.javaeasybank.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_login_log")
@Getter
@Setter
public class AuthLoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "attempt_email", length = 100, nullable = false)
    private String attemptEmail;

    @Column(name = "emp_id", length = 10)
    private String empId;

    @Column(name = "login_time")
    private LocalDateTime loginTime = LocalDateTime.now();

    @Column(name = "login_result", length = 10, nullable = false)
    private String loginResult;

    @Column(name = "fail_reason", length = 200)
    private String failReason;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;
}
