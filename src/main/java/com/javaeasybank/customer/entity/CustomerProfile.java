package com.javaeasybank.customer.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_profile")
@Getter
@Setter
public class CustomerProfile {

    @Id
    @Column(name = "customer_id", length = 20, nullable = false)
    private String customerId;

    // 💡 確保 CIF 的長度為 20，對應 SQL 的 VARCHAR(20)
    @Column(length = 20, nullable = false, unique = true)
    private String cif;

    @Column(name = "id_number", length = 20, nullable = false, unique = true)
    private String idNumber;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    @Column(length = 1, nullable = false)
    private String gender;

    @Column(length = 100, nullable = false, unique = true)
    private String email;

    @Column(length = 20, nullable = false, unique = true)
    private String phone;

    @Column(length = 255, nullable = false)
    private String address;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(length = 20, nullable = false)
    private String status = "ACTIVE";

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