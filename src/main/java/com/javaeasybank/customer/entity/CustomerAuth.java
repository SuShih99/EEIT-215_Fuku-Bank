package com.javaeasybank.customer.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

/**
 * 客戶認證表：儲存客戶的登入帳號、密碼與角色資訊。
 *
 * 設計決策：
 * - 與 customer_profile 為一對一關聯（共用 customer_id）
 * - 密碼使用 BCrypt 加密
 * - role 預設為 CUSTOMER，未來可擴展
 * - resetToken / resetTokenExpiry 用於密碼重設郵件連結
 */
@Entity
@Table(name = "CUSTOMER_AUTH")
@Getter
@Setter
public class CustomerAuth implements Persistable<String> {

    /** 讓 Spring Data JPA 的 save() 正確走 persist() 而非 merge() */
    @Transient
    private boolean isNew = true;

    @Override
    public String getId() { return authId; }

    @Override
    public boolean isNew() { return isNew; }

    @PostPersist
    @PostLoad
    void markNotNew() { this.isNew = false; }

    @Id
    @Column(name = "auth_id", length = 20, nullable = false)
    private String authId;

    @Column(name = "customer_id", length = 20, nullable = false, unique = true)
    private String customerId;

    @Column(name = "username", length = 50, nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(name = "role", length = 20, nullable = false)
    private String role = "CUSTOMER";

    @Column(name = "status", length = 20, nullable = false)
    private String status = "ACTIVE";

    @Column(name = "reset_token", length = 255)
    private String resetToken;

    @Column(name = "reset_token_expiry")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resetTokenExpiry;

    @Column(name = "last_login_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginDate;

    @Column(name = "verification_token", length = 255)
    private String verificationToken;

    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;

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
