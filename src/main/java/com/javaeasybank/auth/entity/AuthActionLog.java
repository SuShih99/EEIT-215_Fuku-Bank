package com.javaeasybank.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_action_log")
@Getter
@Setter
public class AuthActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "emp_id", length = 10)
    private String empId;

    @Column(name = "emp_name", length = 50)
    private String empName;

    @Column(name = "action", length = 50, nullable = false)
    private String action; // e.g., LOGIN, LOGOUT, CREATE_EMP, UPDATE_EMP, SUSPEND_EMP

    @Column(name = "target", length = 50)
    private String target; // Target ID, e.g., E26101

    @Column(name = "details", length = 500)
    private String details;

    @Column(name = "action_time")
    private LocalDateTime actionTime = LocalDateTime.now();

    @Column(name = "ip_address", length = 45)
    private String ipAddress;
}
