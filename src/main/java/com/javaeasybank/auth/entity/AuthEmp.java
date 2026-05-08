package com.javaeasybank.auth.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_emp")
@Getter
@Setter
public class AuthEmp {

    @Id
    @Column(name = "emp_id", length = 10)
    private String empId;

    @Column(name = "emp_name", length = 50, nullable = false)
    private String empName;

    @Column(name = "dept_id", length = 10, nullable = false)
    private String deptId;

    @Column(name = "role_id", length = 10, nullable = false)
    private String roleId;

    @Column(length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(length = 10)
    private String status = "ACTIVE";

    @Column(name = "contract_end_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime contractEndDate;

    @Column(name = "permission_expire", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime permissionExpire;

    @Column(name = "failed_attempts")
    private Integer failedAttempts = 0;

    @Column(name = "pwd_updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pwdUpdatedAt;

    @Column(name = "last_login_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginDate;

    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}