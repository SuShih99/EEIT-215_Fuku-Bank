package com.javaeasybank.customer.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "CUSTOMER_PROFILE")
@Getter
@Setter
public class CustomerProfile {

    @Id
    @Column(name = "customer_id", length = 20, nullable = false)
    private String customerId;

    // 💡 確保 CIF 的長度為 20，對應 SQL 的 VARCHAR(20)
    @Column(length = 20, nullable = false, unique = true)
    private String cif;

    @Column(name = "id_number", length = 20, nullable = false)
    private String idNumber;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    @Column(length = 1, nullable = false)
    private String gender;

    @Column(length = 100, nullable = false)
    private String email;

    @Column(length = 20, nullable = false)
    private String phone;

    @Column(length = 255, nullable = false)
    private String address;

    @Column(length = 10)
    private String nationality;

    @Column(name = "registered_address", length = 255)
    private String registeredAddress;

    @Column(name = "current_address", length = 255)
    private String currentAddress;

    @Column(length = 50)
    private String occupation;

    @Column(name = "employer", length = 100)
    private String employer;

    @Column(name = "estimated_monthly_tx")
    private Integer estimatedMonthlyTx;

    @Column(name = "account_purpose", length = 30)
    private String accountPurpose;

    @Column(name = "fund_source", length = 30)
    private String fundSource;

    @Column(name = "tax_residency", length = 10)
    private String taxResidency;

    @Column(name = "is_pep", columnDefinition = "BIT NOT NULL DEFAULT 0")
    private Boolean isPep = false;

    @Column(name = "id_front_url", length = 255)
    private String idFrontUrl;

    @Column(name = "id_back_url", length = 255)
    private String idBackUrl;

    @Column(name = "second_id_url", length = 255)
    private String secondIdUrl;

    @Column(name = "latest_account_application_id")
    private Long latestAccountApplicationId;

    @Column(name = "latest_account_application_no", length = 30)
    private String latestAccountApplicationNo;

    @Column(name = "latest_applied_account_type", length = 20)
    private String latestAppliedAccountType;

    @Column(name = "latest_applied_currency", length = 3)
    private String latestAppliedCurrency;

    @Column(name = "latest_account_application_status", length = 20)
    private String latestAccountApplicationStatus;

    @Column(name = "latest_account_application_risk_flag", length = 30)
    private String latestAccountApplicationRiskFlag;

    @Column(name = "latest_account_application_reviewed_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime latestAccountApplicationReviewedAt;

    @Column(name = "latest_account_application_reviewed_by", length = 50)
    private String latestAccountApplicationReviewedBy;

    @Column(name = "latest_account_application_reject_reason", length = 500)
    private String latestAccountApplicationRejectReason;

    @Column(name = "created_account_number", length = 14)
    private String createdAccountNumber;

    @Column(name = "account_application_synced_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime accountApplicationSyncedAt;

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


    //風控
    @Column(name="job")
    private String job;
    @Column(name="annual_income")
    private Integer annualIncome;
    @Column(name="risk_level")
    private String riskLevel;

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
